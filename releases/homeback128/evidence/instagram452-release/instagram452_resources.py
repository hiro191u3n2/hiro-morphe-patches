#!/usr/bin/env python3
"""Audit fresh Instagram APK resources and only the intended Back manifest flags.

Compares decompressed entries, independent of ZIP compression/alignment/signing.
The public result contains hashes and counts, never source manifests or APK data.
"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile


ANDROID = "http://schemas.android.com/apk/res/android:"
BACK = ANDROID + "enableOnBackInvokedCallback(0x0101066c)"


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def sha(path):
    result = hashlib.sha256()
    with open(path, "rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            result.update(block)
    return result.hexdigest()


def inventory(archive):
    files = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
    check(len(files) == len(set(files)), "Duplicate APK file entry")
    return set(files)


def excluded(name):
    return bool(re.fullmatch(r"classes\d*\.dex", name) or re.fullmatch(
        r"META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))", name,
        flags=re.IGNORECASE))


def manifest(aapt2, apk):
    output = subprocess.check_output(
        [aapt2, "dump", "xmltree", apk, "--file", "AndroidManifest.xml"],
        text=True)
    nodes, stack, namespaces = [], [], []
    for line in output.splitlines():
        indent = len(line) - len(line.lstrip())
        value = re.sub(r" \(line=\d+\)$", "", line.strip())
        if not value:
            continue
        if value.startswith("N: "):
            namespaces.append(value[3:])
        elif value.startswith("E: "):
            while stack and stack[-1][0] >= indent:
                stack.pop()
            parent = stack[-1][1] if stack else None
            nodes.append({"parent": parent, "name": value[3:], "attributes": {}})
            stack.append((indent, len(nodes) - 1))
        elif value.startswith("A: "):
            check(bool(stack) and indent > stack[-1][0], "Attribute without element")
            key, separator, data = value[3:].partition("=")
            check(bool(separator), "Malformed manifest attribute")
            attrs = nodes[stack[-1][1]]["attributes"]
            check(key not in attrs, "Duplicate manifest attribute")
            attrs[key] = data
        else:
            raise AssertionError("Unexpected aapt2 manifest record: " + value[:80])
    check(bool(nodes) and nodes[0]["name"] == "manifest", "Missing manifest root")
    check(sum(node["parent"] is None for node in nodes) == 1, "Multiple manifest roots")
    return sorted(namespaces), nodes


def compare_manifest(before, after):
    check(before[0] == after[0], "Manifest namespace declarations changed")
    check(len(before[1]) == len(after[1]), "Manifest element inventory changed")
    flags, kinds, previous = 0, Counter(), Counter()
    changed = 0
    for old, new in zip(before[1], after[1]):
        check((old["parent"], old["name"]) == (new["parent"], new["name"]),
              "Manifest element structure/order changed")
        old_attrs, new_attrs = dict(old["attributes"]), dict(new["attributes"])
        if old["name"] in ("application", "activity"):
            prior = old_attrs.pop(BACK, None)
            check(new_attrs.pop(BACK, None) == "true", "Back flag is not explicitly true")
            check(prior in (None, "false", "true"), "Unrecognized original Back value")
            previous["absent" if prior is None else prior] += 1
            flags += 1
            kinds[old["name"]] += 1
            changed += prior != "true"
        check(old_attrs == new_attrs, "Other manifest attribute changed on " + old["name"])
    check(kinds["application"] == 1 and kinds["activity"] > 0,
          "Expected one application and at least one activity")
    return {"explicit_true_flags": flags, "changed_flags": changed,
            "application_flags": kinds["application"], "activity_flags": kinds["activity"],
            "original_flag_values": dict(sorted(previous.items())),
            "other_semantic_changes": []}


def compare_resource_table(aapt2, before, after, proof_path):
    check(proof_path is not None, "Changed resource table requires independent typed-value proof")
    proof = json.loads(Path(proof_path).read_text())
    check(proof.get("result") == "PASS" and proof.get("input_sha256") == sha(before) and
          proof.get("output_sha256") == sha(after) and
          proof.get("raw_typed_values_references_bag_parents_preserved") is True,
          "Typed-value proof must pass and bind the actual APK hashes")
    def dump(path):
        return subprocess.check_output([aapt2, "dump", "resources", path], text=True).splitlines()
    old, new = dump(before), dump(after)
    check(len(old) == len(new), "Resource dump inventory changed")
    renamed, assigned, reference_spelling = Counter(), Counter(), 0
    entry = re.compile(r"^(\s+resource (0x[0-9a-f]+) ([^/]+)/)(.*)$")
    for left, right in zip(old, new):
        if left == right:
            continue
        x, y = entry.fullmatch(left), entry.fullmatch(right)
        if x is not None and y is not None and x.group(1) == y.group(1) and \
                y.group(4) == y.group(3) + "_" + y.group(2):
            (assigned if x.group(4) == "(name removed)" else renamed)[x.group(3)] += 1
        elif left == "        appName(0x7f040090)=@string/(name removed)" and \
                right == "        appName(0x7f040090)=@string/string_0x7f1300cb":
            # The companion raw-value audit above proves the underlying numeric
            # reference, including this exact bag attribute, was not changed.
            reference_spelling += 1
        else:
            raise AssertionError("Unexpected resource table semantic delta")
    return {"aapt2_resource_dump_lines": len(old),
            "resource_configurations": proof["resource_configurations"],
            "raw_typed_values_references_bag_parents_preserved": True,
            "named_entries_renamed": dict(sorted(renamed.items())),
            "unnamed_entries_assigned_names": dict(sorted(assigned.items())),
            "renamed_reference_display_lines": reference_spelling,
            "name_rule": "resource type + '_' + hexadecimal resource ID",
            "other_aapt2_resource_deltas": [],
            "limitation": "Resource table bytes and entry names are not identical; dynamic lookup by the old resource name is not claimed equivalent."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("aapt2", "before", "single", "bundle", "output"):
        parser.add_argument(name)
    parser.add_argument("--resource-value-proof", help="Hash-bound Instagram452ResourceValues JSON")
    args = parser.parse_args()
    with zipfile.ZipFile(args.before) as before, zipfile.ZipFile(args.single) as single, \
            zipfile.ZipFile(args.bundle) as bundle:
        before_names, single_names, bundle_names = map(inventory, (before, single, bundle))
        # Signatures may differ after signing; all other files, including DEX and
        # the manifest, must have identical bytes in single and bundle results.
        comparable_single = {n for n in single_names if not excluded(n) or n.endswith(".dex")}
        comparable_bundle = {n for n in bundle_names if not excluded(n) or n.endswith(".dex")}
        check(comparable_single == comparable_bundle, "Single/bundle file inventory differs")
        for name in sorted(comparable_single):
            check(single.read(name) == bundle.read(name), "Single/bundle entry differs: " + name)
        original_resources = {n for n in before_names if not excluded(n)}
        output_resources = {n for n in single_names if not excluded(n)}
        check(original_resources == output_resources, "Noncode file inventory changed: " +
              str(sorted(original_resources ^ output_resources)[:10]))
        preserved = original_resources - {"AndroidManifest.xml", "resources.arsc"}
        for name in sorted(preserved):
            check(before.read(name) == single.read(name), "Noncode bytes changed: " + name)
        check("resources.arsc" in original_resources, "Missing resource table")
        table_identical = before.read("resources.arsc") == single.read("resources.arsc")
        resource_hash = hashlib.sha256(single.read("resources.arsc")).hexdigest()
    manifest_result = compare_manifest(manifest(args.aapt2, args.before),
                                       manifest(args.aapt2, args.single))
    table_result = {} if table_identical else compare_resource_table(
        args.aapt2, args.before, args.single, args.resource_value_proof)
    result = {"result": "PASS", "physical_android_tested": False,
              "input_sha256": sha(args.before), "single_output_sha256": sha(args.single),
              "bundle_output_sha256": sha(args.bundle),
              "all_single_bundle_nonsignature_entry_bytes_equal": True,
              "identical_single_bundle_entries": len(comparable_single),
              "byte_exact_noncode_entries": len(preserved) + int(table_identical),
              "noncode_inventory_identical": True, "resource_table_byte_identical": table_identical,
              "resources_arsc_sha256": resource_hash, "manifest": manifest_result,
              "resource_table": table_result,
              "aapt2_sha256": sha(args.aapt2)}
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print("PASS fresh Instagram resources: " + str(len(preserved)) +
          " byte-exact noncode entries; resource table checked separately; " +
          str(manifest_result["explicit_true_flags"]) +
          " explicit Back flags; all other manifest structure and attributes unchanged")


if __name__ == "__main__":
    main()
