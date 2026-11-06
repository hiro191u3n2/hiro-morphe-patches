#!/usr/bin/env python3
"""Audit the narrowly permitted resource changes in the SwiftKey privacy patch.

Both decoded directories must be fresh apktool ``decode -s`` outputs for the
corresponding APKs.  The original APK is the monolithic merge of the previously
modified v2 base and its arm64-v8a, Japanese, and xxhdpi splits.  Its exact hash
is supplied explicitly because re-running the merge may change ZIP metadata.
This audit does not establish a successful clean-APK patch application.  The
source APKS and split-to-merged provenance are verified by a separate audit.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path


ANDROID = "{http://schemas.android.com/apk/res/android}"
REFERENCE_APKS_SHA256 = "8f3dbdcd975bad0e63355702dc371761953fd845add6f11277f5c0a2412bdd24"
FLAGS = (
    "firebase_crashlytics_collection_enabled",
    "firebase_sessions_enabled",
)
COMPONENTS = (
    ("receiver", "com.google.android.gms.analytics.AnalyticsReceiver"),
    ("service", "com.google.android.gms.analytics.AnalyticsService"),
    ("service", "com.google.android.gms.analytics.AnalyticsJobService"),
    ("provider", "com.adjust.sdk.SystemLifecycleContentProvider"),
)
BOOL_PATH = "values/bools.xml"
BOOL_NAME = "exceptions_report_enabled"
DEX_NAME = re.compile(r"classes(?:[2-9]|[1-9][0-9]+)?\.dex\Z")
SIGNATURE_NAME = re.compile(r"META-INF/[^/]+\.(?:SF|RSA|DSA|EC)\Z", re.I)


def need(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def sha_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def digest(value: object) -> str:
    return sha_bytes(json.dumps(value, ensure_ascii=False, sort_keys=True,
                               separators=(",", ":")).encode("utf-8"))


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def canonical_xml(element: ET.Element, preserve: bool = False,
                  parent_tag: str = "") -> tuple:
    """Ignore formatting indentation and attribute order, not resource values.

    Whitespace within string resources and styled/mixed string content remains
    significant.  Element order remains significant everywhere except the
    independently compared public resource ID table.
    """
    tag = local_name(element.tag)
    preserve = preserve or tag == "string" or (
        tag == "item" and (
            parent_tag in {"string-array", "plurals"}
            or element.get("type") == "string"
        )
    )

    def text(value: str | None) -> str:
        if value is None:
            return ""
        return value if preserve or value.strip() else ""

    return (
        element.tag,
        tuple(sorted(element.attrib.items())),
        text(element.text),
        tuple((canonical_xml(child, preserve, tag), text(child.tail))
              for child in element),
    )


def xml_root(path: Path) -> ET.Element:
    need(path.is_file(), f"Missing decoded XML: {path}")
    return ET.parse(path).getroot()


def one(items: list, label: str):
    need(len(items) == 1, f"Expected exactly one {label}; found {len(items)}")
    return items[0]


def manifest_audit(original: Path, patched: Path) -> dict:
    before = copy.deepcopy(xml_root(original / "AndroidManifest.xml"))
    after = copy.deepcopy(xml_root(patched / "AndroidManifest.xml"))
    need(before.tag == after.tag == "manifest", "Unexpected manifest root")
    need(before.get("package") == "com.touchtype.swiftkey.beta",
         "Reference manifest is not SwiftKey Beta")
    old_app = one(before.findall("application"), "original application")
    new_app = one(after.findall("application"), "patched application")
    changes = []

    for name in FLAGS:
        old = [e for e in old_app.findall("meta-data")
               if e.get(ANDROID + "name") == name]
        new = [e for e in new_app.findall("meta-data")
               if e.get(ANDROID + "name") == name]
        need(len(old) <= 1, f"Duplicate original manifest flag: {name}")
        target = one(new, f"patched manifest flag {name}")
        expected = copy.deepcopy(old[0]) if old else ET.Element(
            "meta-data", {ANDROID + "name": name})
        need(ANDROID + "resource" not in expected.attrib,
             f"Unexpected resource-backed collection flag: {name}")
        expected.set(ANDROID + "value", "false")
        need(canonical_xml(target) == canonical_xml(expected),
             f"Flag not disabled exactly as permitted: {name}")
        changes.append({"kind": "manifest_flag", "name": name,
                        "before": old[0].get(ANDROID + "value") if old else None,
                        "after": "false"})
        # Application-level metadata order does not affect these flags.  Remove
        # only the two fully checked entries before comparing everything else.
        if old:
            old_app.remove(old[0])
        new_app.remove(target)

    for tag, name in COMPONENTS:
        old = one([e for e in old_app.findall(tag)
                   if e.get(ANDROID + "name") == name],
                  f"original {tag} {name}")
        new = one([e for e in new_app.findall(tag)
                   if e.get(ANDROID + "name") == name],
                  f"patched {tag} {name}")
        changes.append({"kind": "component_enabled", "tag": tag, "name": name,
                        "before": old.get(ANDROID + "enabled"), "after": "false"})
        need(new.get(ANDROID + "enabled") == "false",
             f"Component remains enabled: {name}")
        old.set(ANDROID + "enabled", "false")

    expected = canonical_xml(before)
    actual = canonical_xml(after)
    need(expected == actual,
         "Manifest changed outside the two collection flags and four enabled flags")
    return {"passed": True, "allowed_changes": changes,
            "remaining_manifest_semantics_sha256": digest(actual),
            "all_other_components_permissions_and_attributes_preserved": True}


def files_under(root: Path) -> dict[str, Path]:
    need(root.is_dir(), f"Missing decoded directory: {root}")
    result = {}
    for path in sorted(root.rglob("*")):
        need(not path.is_symlink(), f"Unexpected symlink in decoded resources: {path}")
        if path.is_file():
            result[path.relative_to(root).as_posix()] = path
    return result


def compare_names(before: dict, after: dict, label: str) -> None:
    missing = sorted(before.keys() - after.keys())
    added = sorted(after.keys() - before.keys())
    need(not missing and not added,
         f"{label} inventory differs; missing={missing[:20]}, added={added[:20]}")


def public_table(path: Path) -> dict:
    root = xml_root(path)
    need(root.tag == "resources", f"Unexpected public.xml root: {path}")
    result = {}
    for element in root:
        need(element.tag == "public", f"Unexpected public.xml element: {element.tag}")
        need(set(element.attrib) == {"type", "name", "id"},
             f"Unexpected public.xml attributes: {element.attrib}")
        need(not list(element) and not (element.text or "").strip(),
             "Unexpected public.xml entry content")
        key = element.get("type") + "/" + element.get("name")
        need(key not in result, f"Duplicate public resource symbol: {key}")
        result[key] = int(element.get("id"), 0)
    need(len(set(result.values())) == len(result), "Duplicate public resource IDs")
    return {"root_attributes": sorted(root.attrib.items()), "symbols": result}


def resource_audit(original: Path, patched: Path) -> dict:
    old = files_under(original / "res")
    new = files_under(patched / "res")
    compare_names(old, new, "Decoded resources")
    need(BOOL_PATH in old, f"Missing {BOOL_PATH}")
    need("values/public.xml" in old, "Missing public resource ID table")

    old_public = public_table(old["values/public.xml"])
    new_public = public_table(new["values/public.xml"])
    need(old_public == new_public, "Public resource ID/name/type mapping changed")
    xml_count = binary_count = 0
    records = {}
    bool_before = None
    for name in sorted(old):
        if name == "values/public.xml":
            records[name] = digest(new_public)
            xml_count += 1
            continue
        if name.endswith(".xml"):
            try:
                old_root = ET.parse(old[name]).getroot()
                new_root = ET.parse(new[name]).getroot()
            except ET.ParseError:
                # Raw/binary XML payloads are not normalized.  Their bytes must
                # be identical, which is stricter than a decoded XML comparison.
                need(sha_file(old[name]) == sha_file(new[name]),
                     f"Non-text XML payload changed: {name}")
                records[name] = sha_file(new[name])
                binary_count += 1
                continue
            if name == BOOL_PATH:
                old_bool = one([e for e in old_root.findall("bool")
                                if e.get("name") == BOOL_NAME],
                               f"original bool {BOOL_NAME}")
                new_bool = one([e for e in new_root.findall("bool")
                                if e.get("name") == BOOL_NAME],
                               f"patched bool {BOOL_NAME}")
                bool_before = (old_bool.text or "").strip()
                need(bool_before in {"true", "false"},
                     "Exception-report flag is not a literal boolean")
                need((new_bool.text or "").strip() == "false",
                     "Exception reporting remains enabled")
                old_bool.text = new_bool.text
            expected = canonical_xml(old_root)
            actual = canonical_xml(new_root)
            need(expected == actual, f"Unexpected resource XML change: {name}")
            records[name] = digest(actual)
            xml_count += 1
        else:
            old_hash = sha_file(old[name])
            new_hash = sha_file(new[name])
            need(old_hash == new_hash, f"Non-XML resource bytes changed: {name}")
            records[name] = new_hash
            binary_count += 1

    return {"passed": True, "files_compared": len(records),
            "xml_files_compared": xml_count, "binary_files_compared": binary_count,
            "normalized_resource_inventory_sha256": digest(records),
            "exception_report_flag": {"name": BOOL_NAME, "before": bool_before,
                                      "after": "false"},
            "public_resource_ids": {"passed": True,
                                    "symbol_count": len(old_public["symbols"]),
                                    "mapping_sha256": digest(new_public)}}


def apk_inventory(path: Path) -> dict[str, str]:
    result = {}
    with zipfile.ZipFile(path) as apk:
        for entry in apk.infolist():
            if entry.is_dir():
                continue
            name = entry.filename
            need(name not in result, f"Duplicate APK entry: {path.name}:{name}")
            need(not name.startswith("/") and ".." not in Path(name).parts,
                 f"Unsafe APK entry name: {name}")
            result[name] = sha_bytes(apk.read(entry))
    need("AndroidManifest.xml" in result and "resources.arsc" in result,
         f"Expected an APK, not an outer APKS archive: {path}")
    return result


def selected_inventory(inventory: dict[str, str], prefix: str) -> dict[str, str]:
    return {name: value for name, value in inventory.items() if name.startswith(prefix)}


def identical_inventory(old: dict, new: dict, label: str) -> dict:
    compare_names(old, new, label)
    changed = sorted(name for name in old if old[name] != new[name])
    need(not changed, f"{label} bytes changed: {changed[:20]}")
    return {"passed": True, "file_count": len(old), "inventory_sha256": digest(new)}


def ignored_package_entry(name: str) -> bool:
    return (name in {"AndroidManifest.xml", "resources.arsc", "stamp-cert-sha256"}
            or name.startswith(("res/", "assets/", "lib/"))
            or bool(DEX_NAME.fullmatch(name))
            or name.upper() == "META-INF/MANIFEST.MF"
            or bool(SIGNATURE_NAME.fullmatch(name)))


def decoded_provenance(decoded: Path, inventory: dict[str, str], label: str) -> dict:
    binary_manifest = decoded / "original" / "AndroidManifest.xml"
    need(binary_manifest.is_file(), f"Missing {label} decoder's original binary manifest")
    need(sha_file(binary_manifest) == inventory["AndroidManifest.xml"],
         f"{label} decoded manifest does not belong to its supplied APK")
    dex_entries = {name: value for name, value in inventory.items()
                   if DEX_NAME.fullmatch(name)}
    need(dex_entries, f"No DEX entries in {label} APK")
    decoded_dex = {path.name: sha_file(path) for path in decoded.glob("classes*.dex")
                   if DEX_NAME.fullmatch(path.name)}
    identical_inventory(dex_entries, decoded_dex, f"{label} decoded-to-APK DEX")
    for prefix in ("assets/", "lib/"):
        entries = selected_inventory(inventory, prefix)
        directory = decoded / prefix.rstrip("/")
        decoded_entries = ({prefix + name: sha_file(path)
                            for name, path in files_under(directory).items()}
                           if directory.exists() else {})
        identical_inventory(entries, decoded_entries,
                            f"{label} decoded-to-APK {prefix}")
    return {"passed": True, "binary_manifest_matches_apk": True,
            "dex_entries_match_apk": len(dex_entries),
            "assets_and_native_entries_match_apk": True}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--original-apk", type=Path, required=True)
    parser.add_argument("--original-apk-sha256", required=True,
                        help="Expected SHA-256 of the independently verified merged reference APK")
    parser.add_argument("--patched-apk", type=Path, required=True)
    parser.add_argument("--original-decoded", type=Path, required=True)
    parser.add_argument("--patched-decoded", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-fA-F]{64}", args.original_apk_sha256):
        parser.error("--original-apk-sha256 must contain exactly 64 hexadecimal characters")
    expected_original_sha256 = args.original_apk_sha256.lower()
    report = {
        "schema_version": 1,
        "audit": "swiftkey_beta_privacy_resource_preservation",
        "status": "FAIL",
        "reference": {
            "kind": "previously_modified_swiftkey_beta_v2_merged_arm64_ja_xxhdpi",
            "package": "com.touchtype.swiftkey.beta",
            "version": "9.13.16.4", "version_code": 1236271168,
            "expected_original_apk_sha256": expected_original_sha256,
            "source_apks_sha256_for_separate_merge_audit": REFERENCE_APKS_SHA256,
            "merged_splits": ["base", "arm64-v8a", "ja", "xxhdpi"],
        },
        "limitations": [
            "The reference merges an already modified v2 base and its arm64-v8a, Japanese, and xxhdpi splits; this is not a clean-APK application test.",
            "Fresh apktool -s decode outputs are required; decoder fidelity is assumed.",
            "DEX behavior is audited separately; signatures and ZIP encoding are excluded.",
            "All native entries in the merged APKs are compared; the source APKS and split-to-merged provenance are verified separately.",
            "This audit does not execute the keyboard on a device or test network traffic.",
        ],
        "checks": {}, "errors": [],
    }

    def run(name, fn):
        try:
            result = fn()
            report["checks"][name] = result
            return result
        except Exception as error:
            report["checks"][name] = {"passed": False, "error": str(error)}
            report["errors"].append({"check": name, "error": str(error)})
            return None

    def input_check():
        old_hash = sha_file(args.original_apk)
        new_hash = sha_file(args.patched_apk)
        report["reference"]["actual_original_apk_sha256"] = old_hash
        report["patched_apk_sha256"] = new_hash
        need(old_hash == expected_original_sha256,
             "Original APK differs from the explicitly pinned merged v2 reference")
        return {"passed": True, "original_apk_sha256": old_hash,
                "patched_apk_sha256": new_hash}

    run("input_reference", input_check)
    old_zip = run("original_zip_inventory", lambda: apk_inventory(args.original_apk))
    new_zip = run("patched_zip_inventory", lambda: apk_inventory(args.patched_apk))
    # Keep reports compact while retaining a deterministic inventory digest.
    for label, inventory in (("original_zip_inventory", old_zip),
                             ("patched_zip_inventory", new_zip)):
        if inventory is not None:
            report["checks"][label] = {"passed": True, "file_count": len(inventory),
                                       "inventory_sha256": digest(inventory)}

    if old_zip is not None and new_zip is not None:
        run("original_decoded_provenance",
            lambda: decoded_provenance(args.original_decoded, old_zip, "original"))
        run("patched_decoded_provenance",
            lambda: decoded_provenance(args.patched_decoded, new_zip, "patched"))
        for label, prefix in (("assets", "assets/"), ("native_libraries", "lib/")):
            run(label, lambda prefix=prefix, label=label: identical_inventory(
                selected_inventory(old_zip, prefix), selected_inventory(new_zip, prefix), label))
        run("other_apk_payloads", lambda: identical_inventory(
            {n: h for n, h in old_zip.items() if not ignored_package_entry(n)},
            {n: h for n, h in new_zip.items() if not ignored_package_entry(n)},
            "Other APK payloads"))

    run("manifest", lambda: manifest_audit(args.original_decoded, args.patched_decoded))
    run("resources", lambda: resource_audit(args.original_decoded, args.patched_decoded))
    report["status"] = "PASS" if not report["errors"] else "FAIL"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
                           encoding="utf-8")
    print(json.dumps({"status": report["status"], "report": str(args.output),
                      "failed_checks": len(report["errors"])}, ensure_ascii=False))
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
