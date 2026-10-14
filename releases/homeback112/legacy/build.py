#!/usr/bin/env python3
"""Build the additive OneBack patch while retaining every existing bundle162 patch.

The first build writes two MPPs plus build evidence. Once independent review has
produced src/reviewed-qa.json and publication/manifest.json, --package-only creates
the complete release assets without rebuilding or changing the reviewed MPPs.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
RELEASE_ROOT = ROOT.parent
PUBLICATION = RELEASE_ROOT / "publication"
BASE_VERSION, VERSION, BUNDLE_VERSION = "1.0.162", "1.0.0", "1.0.163"
BASE_SHA = "17184ecbfe63b35871afff33a833a8b2b87d00d1bef8fcc22ce2997bcd297da5"
BASE_BYTES = 17322782
TOOLS = {
    "morphe.jar": "82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c",
    "android.jar": "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b",
    "d8.jar": "305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5",
}
BUNDLE = "Hiro_Morphe_Patches_v1.0.163.mpp"
SINGLE = "OneBack_Exit_NoRecents_v1.0.0.mpp"
QA = "QA_OneBack_v1.0.0.json"
SOURCE = "OneBack_v1.0.0_sources_and_QA.zip"
MF = "META-INF/MANIFEST.MF"
PATCH_PREFIX = "app/hiro/oneback/patches/"
RUNTIME_PREFIX = "app/hiro/oneback/runtime/"
EXTENSION = "extensions/oneback_exit.mpe"
PATCH_NAME = "戻る1回で終了・履歴削除"
HOST_MAIN = "app.hiro.oneback.runtime.OneBackExitHostTest"
PACKAGES = ["com.ss.android.ugc.trill", "com.zhiliaoapp.musically", "com.instagram.android",
            "com.twitter.android", "ctrip.english"]
DATE = (2026, 10, 7, 0, 0, 0)
SOURCE_SUFFIXES = {".java", ".kt", ".py", ".json", ".txt", ".md", ".tsv", ".log", ".yaml", ".yml",
                   ".js", ".cjs", ".mjs", ".template", ".patch", ".sha256", ".b64", ".html", ".xml", ".sh"}
BUILD_SOURCE_DIRECTORIES = ("runtime", "patch", "test-runtime", "test-stubs", "test-dex")
PUBLICATION_SOURCES = ("publish163.py", "manifest.json", "RELEASE_NOTES.txt", "oneback163-publish.yml")
CORE_QA = {
    "android_device_tested": False,
    "existing_resource_entries_unchanged": True,
    "existing_loader_classes_unchanged": True,
    "standalone_and_bundle_new_entries_identical": True,
    "runtime_host_tests_passed": True,
    "synthetic_apply_tests_passed": True,
    "patch_default_enabled": True,
}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def sha_file(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def digest_file(path):
    return {"bytes": Path(path).stat().st_size, "sha256": sha_file(path)}


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()


def run(command, log, timeout=300):
    print("RUN " + Path(log).name, flush=True)
    with Path(log).open("wb") as stream:
        try:
            result = subprocess.run(list(map(str, command)), stdout=stream, stderr=subprocess.STDOUT,
                                    timeout=timeout, check=False)
        except subprocess.TimeoutExpired:
            raise RuntimeError("Command timed out; log=" + str(log)) from None
    output = Path(log).read_text(errors="replace")
    if result.returncode:
        print(output[-12000:], flush=True)
        raise RuntimeError("Command failed; log=" + str(log))
    return output


def checked_path(name):
    path = PurePosixPath(name)
    require(name and not path.is_absolute() and ".." not in path.parts and "\\" not in name
            and str(path) == name and not name.endswith("/"), "Unsafe archive path: " + name)
    return path


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, "Corrupt archive")
        require(len(z.namelist()) == len(set(z.namelist())), "Duplicate archive paths")
        for name in z.namelist():
            checked_path(name.rstrip("/"))
        return {name: z.read(name) for name in z.namelist() if not name.endswith("/")}


def write_zip(path, entries):
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda value: (value != MF, value)):
            checked_path(name)
            info = zipfile.ZipInfo(name, DATE)
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def headers(raw):
    rows = []
    for line in raw.replace(b"\r\n", b"\n").split(b"\n"):
        if line.startswith(b" "):
            require(bool(rows), "Manifest continuation without a header")
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [row.decode().split(": ", 1) for row in rows]
    require(all(len(pair) == 2 for pair in pairs), "Malformed MPP manifest")
    require(len(pairs) == len(dict(pairs)), "Duplicate manifest key")
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(isinstance(value, str) and not any(c in value for c in "\r\n\0"), "Invalid manifest field")
        raw, prefix = (key + ": " + value).encode(), b""
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b" "
        lines.append(prefix + raw)
    raw = b"\r\n".join(lines) + b"\r\n\r\n"
    require(headers(raw) == fields, "Manifest roundtrip failed")
    return raw


def class_files(folder):
    return {path.relative_to(folder).as_posix(): path.read_bytes()
            for path in sorted(folder.rglob("*.class"))}


def source_inputs():
    paths = {ROOT / "build.py"}
    for folder in BUILD_SOURCE_DIRECTORIES:
        paths.update((ROOT / folder).rglob("*"))
    return {path.relative_to(ROOT).as_posix(): digest_file(path)
            for path in sorted(paths)
            if path.is_file() and (path == ROOT / "build.py" or path.suffix.lower() == ".java")
            and "__pycache__" not in path.parts}


def tool_paths(args):
    if args.java_home:
        java, javac = args.java_home / "bin/java", args.java_home / "bin/javac"
        require(java.is_file() and javac.is_file(), "--java-home must contain a JDK")
        return java, javac
    return "java", "javac"


def verify_inputs(args):
    require(args.base.stat().st_size == BASE_BYTES and sha_file(args.base) == BASE_SHA,
            "Baseline162 checksum mismatch; incorporate a newer bundle before publishing")
    for name, digest in TOOLS.items():
        require(sha_file(args.tools / name) == digest, "Tool checksum mismatch: " + name)


def build(args):
    require(not args.work.exists(), "Use a fresh build directory")
    verify_inputs(args)
    for folder in ("runtime", "patch", "test-stubs", "test-runtime", "test-dex"):
        require(any((ROOT / folder).rglob("*.java")), "Missing Java sources: " + folder)
    require((ROOT / "test-dex/DexAudit.java").is_file(), "Missing independent DEX audit")
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)
    inputs = source_inputs()
    baseline = archive(args.base)
    base_fields = headers(baseline[MF])
    require(base_fields.get("Version") == BASE_VERSION and base_fields.get("Name") == "Hiro Morphe Patches",
            "Wrong baseline MPP identity")
    require(baseline.get("classes.dex", b"").startswith(b"dex\n"), "Missing baseline patch-loader DEX")
    require(not any(name.startswith(PATCH_PREFIX) or name == EXTENSION for name in baseline),
            "Baseline already contains the proposed common patch")
    for folder in ("runtimeclasses", "patchclasses", "testclasses", "auditclasses", "runtime-dex", "patch-dex"):
        (args.work / folder).mkdir()
    java, javac = tool_paths(args)
    java_version = run([java, "-version"], args.work / "java-version.txt")
    javac_version = run([javac, "-version"], args.work / "javac-version.txt")
    require(re.search(r"(?:javac\s+)21(?:\.|\s|$)", javac_version), "Build requires JDK 21 for reviewed Morphe tools")
    common = [javac, "--release", "8", "-encoding", "UTF-8"]
    run([*common, "-cp", args.tools / "android.jar", "-d", args.work / "runtimeclasses",
         *sorted((ROOT / "runtime").rglob("*.java"))], args.work / "runtime-javac.log")
    run([*common, "-cp", args.tools / "morphe.jar", "-d", args.work / "patchclasses",
         *sorted((ROOT / "patch").rglob("*.java"))], args.work / "patch-javac.log")
    runtime = class_files(args.work / "runtimeclasses")
    patch = class_files(args.work / "patchclasses")
    require(RUNTIME_PREFIX + "OneBackExit.class" in runtime and all(name.startswith(RUNTIME_PREFIX) for name in runtime),
            "Unexpected production runtime class or packaged test stub")
    require(patch and all(name.startswith(PATCH_PREFIX) for name in patch), "Unexpected patch class")
    host_compile_cp = os.pathsep.join(map(str, (args.work / "runtimeclasses", args.tools / "android.jar")))
    run([*common, "-cp", host_compile_cp, "-d", args.work / "testclasses",
         *sorted((ROOT / "test-stubs").rglob("*.java")),
         *sorted((ROOT / "test-runtime").rglob("*.java"))], args.work / "host-javac.log")
    host_cp = os.pathsep.join(map(str, (args.work / "testclasses", args.work / "runtimeclasses", args.tools / "android.jar")))
    host_result = run([java, "-ea", "-cp", host_cp, HOST_MAIN], args.work / "host-tests.txt")
    host_summary = re.search(r"SUCCESS:\s*(\d+) cases,\s*(\d+) assertions", host_result)
    require(host_summary and int(host_summary[1]) > 0 and int(host_summary[2]) > 0,
            "Host test suite did not report completed cases and assertions")
    require(len(re.findall(r"^PASS ", host_result, re.M)) == int(host_summary[1]), "Host test case count differs")
    audit_compile_cp = os.pathsep.join(map(str, (args.work / "patchclasses", args.tools / "morphe.jar")))
    run([javac, "-encoding", "UTF-8", "-cp", audit_compile_cp, "-d", args.work / "auditclasses",
         *sorted((ROOT / "test-dex").rglob("*.java"))], args.work / "audit-javac.log")
    audit_cp = os.pathsep.join(map(str, (args.work / "auditclasses", args.work / "patchclasses", args.tools / "morphe.jar")))
    selftest_result = run([java, "-ea", "-cp", audit_cp, "DexAudit", "selftest", args.work / "synthetic-tests.json",
                          args.work / "fixture-original.dex", args.work / "fixture-patched.dex"],
                         args.work / "synthetic-tests.txt")
    selftest = json.loads((args.work / "synthetic-tests.json").read_text())
    require(selftest.get("result") == "PASS" and selftest.get("blocking_findings") == []
            and type(selftest.get("assertions")) is int and selftest["assertions"] > 0
            and selftest.get("tests"), "Synthetic DEX/metadata tests did not pass")
    write_zip(args.work / "runtime.jar", runtime)
    write_zip(args.work / "patch.jar", patch)
    for kind, min_api, classpath in (("runtime", "29", args.tools / "android.jar"),
                                     ("patch", "26", args.tools / "morphe.jar")):
        run([java, "-cp", args.tools / "d8.jar", "com.android.tools.r8.D8", "--release", "--min-api", min_api,
             "--lib", args.tools / "android.jar", "--classpath", classpath,
             "--output", args.work / (kind + "-dex"), args.work / (kind + ".jar")], args.work / (kind + "-d8.log"))
        require({path.name for path in (args.work / (kind + "-dex")).iterdir()} == {"classes.dex"}, "Unexpected multidex output")
    (args.work / "base.dex").write_bytes(baseline["classes.dex"])
    merge_result = run([java, "-ea", "-cp", audit_cp, "DexAudit", "merge", args.work / "base.dex",
                        args.work / "patch-dex/classes.dex", args.work / "merged.dex"], args.work / "loader-merge.txt")
    require("PASS" in merge_result and (args.work / "merged.dex").read_bytes().startswith(b"dex\n"),
            "Canonical loader-union audit did not pass")
    additions = {**patch, EXTENSION: (args.work / "runtime-dex/classes.dex").read_bytes()}
    require(not set(additions).intersection(baseline), "New common patch collides with existing entries")
    combined = {**baseline, **additions, "classes.dex": (args.work / "merged.dex").read_bytes()}
    fields = dict(base_fields)
    fields.update(Version=BUNDLE_VERSION, Timestamp="2026-10-07T00:00:00",
                  Description="Add OneBack exit and recents cleanup for TikTok, Instagram, X and Trip.com. Existing bundle162 patch classes and resources retained. Android device untested.")
    combined[MF] = manifest(fields)
    single_fields = dict(fields)
    single_fields.update(Name="OneBack Exit No Recents", Version=VERSION,
                         Description="One-press Android Back exits TikTok, Instagram, X or Trip.com and removes their own recent tasks. Modern and legacy Back routes. Android device untested.")
    standalone = {**additions, "classes.dex": (args.work / "patch-dex/classes.dex").read_bytes(),
                  MF: manifest(single_fields)}
    preserved = {name: raw for name, raw in baseline.items() if name not in (MF, "classes.dex")}
    require(all(combined[name] == raw for name, raw in preserved.items()), "Existing bundle resource changed")
    require({name for name in combined.keys() & baseline.keys() if combined[name] != baseline[name]} == {MF, "classes.dex"},
            "Unexpected existing-entry delta")
    require(set(combined) - set(baseline) == set(additions) and not (set(baseline) - set(combined)),
            "Unexpected additive inventory delta")
    artifacts, discovery = {}, {}
    for name, entries in ((BUNDLE, combined), (SINGLE, standalone)):
        path = args.dist / name
        write_zip(path, entries)
        require(archive(path) == entries, "Written MPP entries differ")
        discovery[name] = {}
        for package in PACKAGES:
            listing = run([java, "-jar", args.tools / "morphe.jar", "list-patches", "--patches", path,
                           "-f", package, "-p", "-v"], args.work / (name + "." + package + ".list.txt"))
            blocks = re.split(r"(?m)^(?:INFO: )?Index: \d+\s*$", listing)
            selected = [block for block in blocks
                        if re.search(r"(?m)^Name: " + re.escape(PATCH_NAME) + r"\s*$", block)]
            require(len(selected) == 1 and package in selected[0]
                    and re.search(r"(?m)^Enabled: true\s*$", selected[0]),
                    "Enabled common-patch discovery failed: " + name + " / " + package)
            discovery[name][package] = True
        artifacts[name] = digest_file(path)
    require(source_inputs() == inputs, "Build source changed during compilation or tests")
    evidence = {
        "schema": "oneback163-build-v1", "bundle_version": BUNDLE_VERSION, "patch_version": VERSION,
        "baseline_bundle_version": BASE_VERSION, "baseline_sha256": BASE_SHA, "toolchain_sha256": TOOLS,
        "java_version": java_version.strip(), "javac_version": javac_version.strip(),
        "supported_packages": PACKAGES, "patch_default_enabled": True,
        "source_inputs": inputs, "existing_resource_entries_byte_identical": len(preserved),
        "new_entries": {name: {"bytes": len(raw), "sha256": sha(raw)} for name, raw in sorted(additions.items())},
        "runtime_jvm_class_count": len(runtime), "patch_jvm_class_count": len(patch),
        "existing_resource_entries_unchanged": True, "existing_loader_classes_unchanged": True,
        "standalone_and_bundle_new_entries_identical": True,
        "runtime_host_tests_passed": True, "synthetic_dex_tests_passed": True,
        "synthetic_patch_tests_passed": True,
        "host_test_result": {"main_class": HOST_MAIN, "cases": int(host_summary[1]),
                             "assertions": int(host_summary[2]), "output": host_result.strip()},
        "synthetic_dex_test_result": selftest, "synthetic_dex_test_output": selftest_result.strip(),
        "loader_merge_result": merge_result.strip(), "morphe_patch_discovery": discovery,
        "runtime_stubs_packaged": False, "android_device_tested": False,
        "artifacts": artifacts,
    }
    (args.dist / "build-evidence.json").write_bytes(json_bytes(evidence))
    for name in ("host-tests.txt", "loader-merge.txt", "synthetic-tests.json", "synthetic-tests.txt"):
        shutil.copyfile(args.work / name, args.dist / name)
    print(json.dumps({"status": "BUILT_HOST_AND_SYNTHETIC_DEX_VERIFIED", "artifacts": artifacts,
                      "host_cases": int(host_summary[1]), "synthetic_assertions": selftest["assertions"]},
                     ensure_ascii=False, indent=2), flush=True)


def package_outputs(args):
    reviewed_path = ROOT / "reviewed-qa.json"
    if not reviewed_path.exists():
        print("MPP build complete. Independent reviewed QA is required before release packaging.", flush=True)
        return
    verify_inputs(args)
    evidence = json.loads((args.dist / "build-evidence.json").read_text())
    require(evidence.get("schema") == "oneback163-build-v1" and evidence.get("source_inputs") == source_inputs(),
            "Build evidence is missing or executable sources changed since this build")
    qa_bytes = reviewed_path.read_bytes()
    qa = json.loads(qa_bytes)
    require(qa.get("schema") == "oneback163-qa-v1" and qa.get("result") == "PASS"
            and qa.get("blocking_findings") == [], "Independent QA is incomplete or has blocking findings")
    require(qa.get("artifacts") == evidence["artifacts"], "Reviewed QA does not match built MPPs")
    require(qa.get("supported_packages") == PACKAGES and qa.get("bundle_version") == BUNDLE_VERSION
            and qa.get("patch_version") == VERSION, "Reviewed QA identifies different targets or versions")
    require(type(qa.get("original_apk_apply_tested")) is bool,
            "QA must explicitly distinguish original-APK coverage from synthetic tests")
    for key, value in CORE_QA.items():
        require(qa.get(key) is value, "Missing or failing reviewed QA assertion: " + key)
    config = json.loads((PUBLICATION / "manifest.json").read_text())
    require(config.get("schema") == "oneback163-publication-v1" and config.get("review_status") == "reviewed",
            "Publication manifest is not independently reviewed")
    require(config.get("expected_mpp") == evidence["artifacts"], "Independent expected MPP hashes differ")
    require(config.get("expected_qa_sha256") == sha(qa_bytes), "Reviewed QA checksum differs")
    require(config.get("new_entries") == evidence["new_entries"], "Reviewed additive entry hashes differ")
    for name, record in evidence["artifacts"].items():
        require(digest_file(args.dist / name) == record, "Built MPP changed after verification: " + name)
    (args.dist / QA).write_bytes(qa_bytes)
    shutil.copyfile(PUBLICATION / "RELEASE_NOTES.txt", args.dist / "RELEASE_NOTES.txt")
    files = {}
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file() or "__pycache__" in path.parts:
            continue
        require(path.suffix.lower() in SOURCE_SUFFIXES, "Unapproved source artifact: " + str(path))
        raw = path.read_bytes()
        raw.decode("utf-8")
        require(b"\x00" not in raw and re.search(rb"-----BEGIN (?:[A-Z ]+ )?PRIVATE KEY-----", raw) is None,
                "Binary or private key in source tree: " + str(path))
        files["src/" + path.relative_to(ROOT).as_posix()] = raw
    for name in PUBLICATION_SOURCES:
        files["publication/" + name] = (PUBLICATION / name).read_bytes()
    if (RELEASE_ROOT / "README.md").is_file():
        files["README.md"] = (RELEASE_ROOT / "README.md").read_bytes()
    for name in ("build-evidence.json", "host-tests.txt", "loader-merge.txt", "synthetic-tests.json", "synthetic-tests.txt", QA):
        files["evidence/" + name] = (args.dist / name).read_bytes()
    require({"src/build.py", "src/reviewed-qa.json", "src/test-dex/DexAudit.java"}.issubset(files),
            "Incomplete authored source archive")
    write_zip(args.dist / SOURCE, files)
    assets = (SINGLE, BUNDLE, QA, SOURCE, "RELEASE_NOTES.txt")
    sums = "".join(sha_file(args.dist / name) + "  " + name + "\n" for name in assets)
    (args.dist / "SHA256SUMS.txt").write_text(sums)
    print("PASS complete source/QA release assets use the independently reviewed MPPs; no application APK included", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("base", "tools", "work", "dist"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--package-only", action="store_true")
    parser.add_argument("--java-home", type=Path)
    args = parser.parse_args()
    for name in ("base", "tools", "work", "dist", "java_home"):
        value = getattr(args, name)
        if value is not None:
            setattr(args, name, value.resolve())
    if not args.package_only:
        build(args)
    package_outputs(args)


if __name__ == "__main__":
    main()
