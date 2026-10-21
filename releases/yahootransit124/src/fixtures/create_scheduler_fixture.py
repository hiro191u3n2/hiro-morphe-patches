#!/usr/bin/env python3
"""Build an unsigned, fully synthetic APK for Yahoo patch scheduler regression tests.

Inputs are only the adjacent authored text fixtures and pinned Android build tools.
No original APK, private resource dump, or third-party application DEX is accepted.
Publish this script and its fixture sources, not the generated APK.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import urllib.request
import zipfile


BUILD_TOOLS_URL = "https://dl.google.com/android/repository/build-tools_r35_linux.zip"
BUILD_TOOLS_SHA256 = "bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88"
AAPT2_ENTRY = "android-15/aapt2"
AAPT2_SHA256 = "d1096e11aba9c974644369ee3c50d239acac3f3428ffa928e5b9c14dfb7a57de"
ANDROID_SHA256 = "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b"
D8_SHA256 = "305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5"
PACKAGE = "jp.co.yahoo.android.apps.transit"
VERSION = "8.5.6"
APK_NAME = "synthetic-yahoo-transit-8.5.6.apk"
EXPECTED_CLASSES = {
    "Ljp/co/yahoo/android/apps/transit/TransitApplication;",
    "Landroidx/core/app/CoreComponentFactory;",
}
TEXT_INPUTS = (
    "AndroidManifest.xml",
    "res/layout/fragment_navi_search.xml",
    "res/layout/fragment_search_result_list.xml",
    "res/values/strings.xml",
    "java/jp/co/yahoo/android/apps/transit/TransitApplication.java",
    "java/androidx/core/app/CoreComponentFactory.java",
    "assets/README-synthetic-fixture.txt",
)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def check_hash(path, expected):
    require(path.is_file(), "Missing tool: " + str(path))
    actual = sha256(path)
    require(actual == expected, "Tool SHA256 mismatch: " + str(path) + ": " + actual)
    return actual


def resolve_aapt2(explicit, cache):
    if explicit:
        binary = explicit.resolve()
        check_hash(binary, AAPT2_SHA256)
        return binary
    cache.mkdir(parents=True, exist_ok=True)
    binary = cache / "aapt2"
    if binary.exists():
        check_hash(binary, AAPT2_SHA256)
        return binary.resolve()
    archive = cache / "build-tools_r35_linux.zip"
    if not archive.exists():
        temporary = archive.with_suffix(".download")
        try:
            with urllib.request.urlopen(BUILD_TOOLS_URL, timeout=60) as source:
                with temporary.open("wb") as destination:
                    shutil.copyfileobj(source, destination, 1024 * 1024)
            check_hash(temporary, BUILD_TOOLS_SHA256)
            temporary.replace(archive)
        finally:
            temporary.unlink(missing_ok=True)
    check_hash(archive, BUILD_TOOLS_SHA256)
    with zipfile.ZipFile(archive) as source:
        binary.write_bytes(source.read(AAPT2_ENTRY))
    check_hash(binary, AAPT2_SHA256)
    binary.chmod(0o755)
    return binary.resolve()


def dex_classes(data):
    """Read only class descriptors from the generated DEX, using no app tooling."""
    require(data[:4] == b"dex\n", "D8 output is not a DEX file")

    def u32(offset):
        return struct.unpack_from("<I", data, offset)[0]

    strings_count, strings_offset = u32(0x38), u32(0x3C)
    types_count, types_offset = u32(0x40), u32(0x44)
    classes_count, classes_offset = u32(0x60), u32(0x64)

    def string_at(index):
        require(index < strings_count, "Invalid DEX string index")
        offset = u32(strings_offset + 4 * index)
        while data[offset] & 0x80:
            offset += 1
        offset += 1
        return data[offset:data.index(0, offset)].decode("ascii")

    descriptors = set()
    for index in range(classes_count):
        type_index = u32(classes_offset + 32 * index)
        require(type_index < types_count, "Invalid DEX type index")
        descriptors.add(string_at(u32(types_offset + 4 * type_index)))
    require(len(descriptors) == classes_count, "Duplicate DEX class descriptor")
    return descriptors


def write_apk(path, entries):
    # STORED avoids zlib-version differences; this is a small unsigned CI fixture.
    with zipfile.ZipFile(path, "w") as archive:
        for name, content in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_STORED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, content)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tools", type=Path, required=True,
                        help="Directory containing the pinned android.jar and d8.jar")
    parser.add_argument("--work", type=Path, required=True,
                        help="New or empty build directory; no existing files are removed")
    parser.add_argument("--output", type=Path,
                        help="Output APK; default: synthetic-yahoo-transit-8.5.6.apk under --work")
    parser.add_argument("--java-home", type=Path)
    parser.add_argument("--aapt2", type=Path,
                        help="Pinned aapt2 binary; skips all network access")
    parser.add_argument("--tool-cache", type=Path,
                        help="Default: fixture-tool-cache next to --work")
    args = parser.parse_args()
    tools = args.tools.resolve()
    work = args.work.resolve()
    fixtures = Path(__file__).resolve().parent / "fixtures"
    inventory = {p.relative_to(fixtures).as_posix() for p in fixtures.rglob("*") if p.is_file()}
    require(inventory == set(TEXT_INPUTS), "Unexpected fixture input inventory")
    require(not work.exists() or not any(work.iterdir()), "--work must be new or empty")
    work.mkdir(parents=True, exist_ok=True)
    android = tools / "android.jar"
    d8 = tools / "d8.jar"
    check_hash(android, ANDROID_SHA256)
    check_hash(d8, D8_SHA256)
    cache = (args.tool_cache or work.parent / "fixture-tool-cache").resolve()
    local_aapt2 = tools / "aapt2"
    aapt2 = resolve_aapt2(args.aapt2 or (local_aapt2 if local_aapt2.is_file() else None), cache)

    def java_tool(name):
        path = (args.java_home / "bin" / name).resolve() if args.java_home else shutil.which(name)
        require(path is not None and Path(path).is_file(), "Missing JDK tool: " + name)
        return str(path)

    java, javac = java_tool("java"), java_tool("javac")
    environment = dict(os.environ, TZ="UTC", LC_ALL="C.UTF-8", SOURCE_DATE_EPOCH="315532800")
    commands = []

    def run(command):
        command = list(map(str, command))
        result = subprocess.run(command, cwd=work, env=environment, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        commands.append({"command": command, "returncode": result.returncode, "output": result.stdout})
        (work / "build-commands.log").write_text(
            "\n".join(json.dumps(entry, ensure_ascii=False) for entry in commands) + "\n",
            encoding="utf-8")
        require(result.returncode == 0, "Command failed: " + " ".join(command) + "\n" + result.stdout)
        return result.stdout.strip()

    versions = {"java": run([java, "-version"]), "javac": run([javac, "-version"]),
                "aapt2": run([aapt2, "version"])}
    for name in TEXT_INPUTS:
        destination = work / "inputs" / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes((fixtures / name).read_bytes())
    (work / "classes").mkdir()
    (work / "dex").mkdir()
    java_sources = sorted(str(p.relative_to(work)) for p in (work / "inputs/java").rglob("*.java"))
    run([javac, "--release", "8", "-g:none", "-encoding", "UTF-8", "-classpath", android,
         "-d", "classes", *java_sources])
    class_files = sorted(str(p.relative_to(work)) for p in (work / "classes").rglob("*.class"))
    require(len(class_files) == 2, "Fixture must compile exactly two stub classes")
    run([java, "-cp", d8, "com.android.tools.r8.D8", "--release", "--min-api", "28",
         "--lib", android, "--output", "dex", *class_files])
    require({p.name for p in (work / "dex").iterdir()} == {"classes.dex"}, "Unexpected D8 output")
    dex = (work / "dex/classes.dex").read_bytes()
    require(dex_classes(dex) == EXPECTED_CLASSES, "DEX must contain exactly the two synthetic stubs")
    run([aapt2, "compile", "--dir", "inputs/res", "-o", "compiled-res.zip"])
    run([aapt2, "link", "-I", android, "--manifest", "inputs/AndroidManifest.xml",
         "-o", "resource-only.apk", "compiled-res.zip"])
    with zipfile.ZipFile(work / "resource-only.apk") as resource_apk:
        entries = {name: resource_apk.read(name) for name in resource_apk.namelist()}
    require("AndroidManifest.xml" in entries and "resources.arsc" in entries, "AAPT2 output is incomplete")
    require(not any(name.endswith(".dex") for name in entries), "Unexpected DEX in resource-only APK")
    entries["classes.dex"] = dex
    entries["assets/README-synthetic-fixture.txt"] = (fixtures / "assets/README-synthetic-fixture.txt").read_bytes()
    apk = args.output.resolve() if args.output else work / APK_NAME
    apk.parent.mkdir(parents=True, exist_ok=True)
    write_apk(apk, entries)
    badging = run([aapt2, "dump", "badging", apk])
    require("name='" + PACKAGE + "'" in badging and "versionName='" + VERSION + "'" in badging,
            "Generated manifest identity differs")
    with zipfile.ZipFile(apk) as verification:
        require(verification.testzip() is None, "Generated APK ZIP integrity failed")
        require(verification.read("classes.dex") == dex, "Packaged DEX differs")
    report = {
        "schema": "synthetic-yahoo-scheduler-fixture-v1",
        "synthetic": True,
        "original_app_resources_or_code_used": False,
        "unsigned_not_for_installation_or_distribution": True,
        "package_name": PACKAGE,
        "version_name": VERSION,
        "version_code": 856,
        "tool_sha256": {"android.jar": ANDROID_SHA256, "d8.jar": D8_SHA256, "aapt2": AAPT2_SHA256},
        "build_tools_archive": {"url": BUILD_TOOLS_URL, "sha256": BUILD_TOOLS_SHA256},
        "tool_versions": versions,
        "builder_sha256": sha256(Path(__file__)),
        "text_fixture_sha256": {name: sha256(fixtures / name) for name in TEXT_INPUTS},
        "class_descriptors": sorted(dex_classes(dex)),
        "dex_sha256": hashlib.sha256(dex).hexdigest(),
        "apk_filename": apk.name,
        "apk_bytes": apk.stat().st_size,
        "apk_sha256": sha256(apk),
        "apk_entries": sorted(entries),
    }
    (work / "build-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Built synthetic regression fixture: " + str(apk))
    print("SHA256 " + report["apk_sha256"])


if __name__ == "__main__":
    main()
