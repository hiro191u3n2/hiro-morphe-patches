#!/usr/bin/env python3
"""Audit the fixed all-144 output's native inventory and privacy write log.

This is an independent, byte-level check of the completed 47.4.3 APK. It does
not run Android, establish feature behavior, or replace PrivacyApkAudit's
ELF/embedded-DEX semantic checks in the four primary cases. Run only after
the patcher has finished and closed the output APK and apply log.
"""

import argparse
import hashlib
import json
import re
import sys
import zipfile
from collections import Counter
from pathlib import Path


ORIGINAL_BYTES = 474921201
ORIGINAL_SHA256 = "af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b"
NATIVE_ENTRIES = 593
STRIP_POLICY = "Metra-0.9.0-dev.1-resource-strip"
UPSTREAM_COMMIT = "b582c984f50aeacbc812761e082354efa3fc0c28"

# Independently enumerated from the pinned upstream ResourceOptimizerPatches.kt:
# P2P (2), core (3), creation (3), LIVE (1), each in both original ABIs.
# Never infer this list from the candidate APK or from the patcher's skip log.
EMPTY_BASENAMES = (
    "libavmdlp2pv2.so", "libp2plivevdp.so",
    "libtt_c2pa_sdk.so", "libtt_c2pa_sdk_d.so", "libartlog_monitor.so",
    "libEffectCreatorJni.so", "libeffect_plugin.so", "libttvesdk_plugin.so",
    "liblink_mic_sdk.so",
)
EXPECTED_EMPTY = frozenset(
    "lib/" + abi + "/" + name
    for abi in ("arm64-v8a", "armeabi-v7a") for name in EMPTY_BASENAMES
)
CHANGED_RE = re.compile(
    r"HIRO4753 PRIVACY_NATIVE path=(\S+) format=(\S+) hosts=(\d+) "
    r"dexConstants=(\d+) dexFields=(\d+) beforeBytes=(\d+) afterBytes=(\d+) "
    r"beforeSHA256=([0-9a-f]{64}) afterSHA256=([0-9a-f]{64})\s*$"
)
EMPTY_RE = re.compile(
    r"HIRO4753 PRIVACY_NATIVE_PRESERVED_EMPTY path=(\S+) bytes=(\d+) policy=(\S+)\s*$"
)
TOTAL_RE = re.compile(
    r"HIRO4753 PRIVACY_NATIVE_TOTAL scanned=(\d+) scannedBytes=(\d+) "
    r"changedFiles=(\d+) elfHosts=(\d+) embeddedConstants=(\d+) "
    r"embeddedFields=(\d+) embeddedDex=(\d+) preservedEmpty=(\d+) formats=\{([^}]*)\}\s*$"
)
TOTAL_KEYS = (
    "scanned", "scannedBytes", "changedFiles", "elfHosts", "embeddedConstants",
    "embeddedFields", "embeddedDex", "preservedEmpty",
)


class AuditError(Exception):
    pass


def require(condition, message):
    if not condition:
        raise AuditError(message)


def snapshot(path):
    value = path.stat()
    require(path.is_file(), "Not a regular input file: " + str(path))
    return (value.st_dev, value.st_ino, value.st_size, value.st_mtime_ns, value.st_ctime_ns)


def digest_stream(handle):
    digest = hashlib.sha256()
    length = 0
    while True:
        block = handle.read(1024 * 1024)
        if not block:
            return length, digest.hexdigest()
        length += len(block)
        digest.update(block)


def digest_file(path):
    with path.open("rb") as handle:
        length, digest = digest_stream(handle)
    return {"name": path.name, "bytes": length, "sha256": digest}


def native_inventory(archive, label):
    entries = archive.infolist()
    duplicates = [name for name, count in Counter(i.filename for i in entries).items() if count != 1]
    require(not duplicates, label + " contains duplicate ZIP names: " + repr(duplicates))
    native = {i.filename: i for i in entries if i.filename.startswith("lib/") and i.filename.endswith(".so")}
    require(all(not i.is_dir() for i in native.values()), label + " has a native path that is a directory")
    return native


def read_log(path):
    changed, empty, totals = [], [], []
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if "HIRO4753 PRIVACY_NATIVE_PRESERVED_EMPTY " in line:
            match = EMPTY_RE.search(line)
            require(match is not None, "Malformed preserved-empty row at line " + str(number))
            name, length, policy = match.groups()
            require(int(length) == 0 and policy == STRIP_POLICY, "Unexpected empty-row policy: " + name)
            empty.append(name)
        elif "HIRO4753 PRIVACY_NATIVE_TOTAL " in line:
            match = TOTAL_RE.search(line)
            require(match is not None, "Malformed native-total row at line " + str(number))
            values = match.groups()
            total = dict(zip(TOTAL_KEYS, map(int, values[:8])))
            formats = {}
            for item in values[8].split(", "):
                key, value = item.split("=", 1)
                require(key not in formats and value.isdecimal(), "Malformed native format count")
                formats[key] = int(value)
            total["formats"] = formats
            totals.append(total)
        elif "HIRO4753 PRIVACY_NATIVE " in line:
            match = CHANGED_RE.search(line)
            require(match is not None, "Malformed native-change row at line " + str(number))
            values = match.groups()
            row = dict(zip(("path", "format", "hosts", "dexConstants", "dexFields", "beforeBytes", "afterBytes", "beforeSHA256", "afterSHA256"), values))
            for key in ("hosts", "dexConstants", "dexFields", "beforeBytes", "afterBytes"):
                row[key] = int(row[key])
            changed.append(row)
    require(len(totals) == 1, "Expected exactly one completed PRIVACY_NATIVE_TOTAL row")
    require(len(empty) == len(EXPECTED_EMPTY) and set(empty) == EXPECTED_EMPTY,
            "Preserved-empty log must name the exact 18 known paths once each")
    require(len({row["path"] for row in changed}) == len(changed), "Duplicate native-change log path")
    total = totals[0]
    require(total["scanned"] == NATIVE_ENTRIES, "Native log did not scan all 593 original entries")
    require(total["preservedEmpty"] == 18, "Native log preservedEmpty must be 18")
    require(total["formats"].get("METRA_EMPTY_PLACEHOLDER") == 18, "Empty-placeholder format count is not 18")
    require(sum(total["formats"].values()) == NATIVE_ENTRIES, "Native format counts do not sum to 593")
    require(total["changedFiles"] == len(changed) and len(changed) > 0, "Native change count mismatch or no changes")
    for total_key, row_key in (("elfHosts", "hosts"), ("embeddedConstants", "dexConstants"), ("embeddedFields", "dexFields")):
        require(total[total_key] == sum(row[row_key] for row in changed), "Native logged total mismatch: " + total_key)
    require(total["elfHosts"] > 0 and total["embeddedConstants"] > 0, "Nonempty native privacy rewrites did not occur")
    return changed, total


def audit(original, output, apply_log):
    inputs = {path: snapshot(path) for path in (original, output, apply_log)}
    require(len(inputs) == 3, "Original, output, and apply log must be distinct files")
    require(original.stat().st_size == ORIGINAL_BYTES, "Original APK size does not match pinned 47.4.3")
    source = digest_file(original)
    require(source["sha256"] == ORIGINAL_SHA256, "Original APK SHA256 does not match pinned 47.4.3")
    changed, total = read_log(apply_log)
    with zipfile.ZipFile(original) as source_zip, zipfile.ZipFile(output) as output_zip:
        before = native_inventory(source_zip, "Original APK")
        after = native_inventory(output_zip, "Output APK")
        require(len(before) == NATIVE_ENTRIES, "Pinned original native count is not 593")
        require(all(info.file_size > 0 for info in before.values()), "Original contains an empty native entry")
        require(set(after) == set(before), "Native inventory differs; missing=" + repr(sorted(set(before) - set(after))) + "; extra=" + repr(sorted(set(after) - set(before))))
        empty = {name for name, info in after.items() if info.file_size == 0}
        require(empty == EXPECTED_EMPTY, "Actual empty paths differ; missing=" + repr(sorted(EXPECTED_EMPTY - empty)) + "; extra=" + repr(sorted(empty - EXPECTED_EMPTY)))
        for name in sorted(EXPECTED_EMPTY):
            require(output_zip.read(name) == b"", "Empty-placeholder payload is not empty: " + name)
            require(after[name].CRC == 0, "Empty-placeholder CRC is not zero: " + name)
        verified = []
        for row in changed:
            name = row["path"]
            require(name in after and name not in empty, "Logged rewrite is missing or empty: " + name)
            require(row["format"] in ("ELF", "ZIP_DEX"), "Unexpected changed-native format: " + name)
            require(row["beforeBytes"] > 0 and row["afterBytes"] > 0, "Logged rewrite must have nonempty input/output: " + name)
            require(row["beforeSHA256"] != row["afterSHA256"], "Logged rewrite did not change bytes: " + name)
            with output_zip.open(name) as handle:
                length, digest = digest_stream(handle)
            require(length == after[name].file_size == row["afterBytes"], "Final native size mismatch: " + name)
            require(digest == row["afterSHA256"], "Final native SHA256 mismatch: " + name)
            verified.append(dict(row, verifiedFinalBytes=length, verifiedFinalSHA256=digest))
    final = digest_file(output)
    log = digest_file(apply_log)
    for path, initial in inputs.items():
        require(snapshot(path) == initial, "Input changed during audit: " + str(path))
    return {
        "schema": "hiro-tiktok-metra-empty-native-v1",
        "status": "PASS",
        "scope": "Completed 47.4.3 all-144 APK native inventory, known empty placeholders, and privacy-log output byte identity only",
        "androidRuntimeTested": False,
        "sourceOriginal": source,
        "outputApk": final,
        "applyLog": log,
        "auditScript": digest_file(Path(__file__)),
        "upstreamStripCommit": UPSTREAM_COMMIT,
        "originalNativeEntries": len(before),
        "outputNativeEntries": len(after),
        "allOriginalNativePathsPreserved": True,
        "originalEmptyNativeEntries": 0,
        "outputEmptyNativeEntries": len(empty),
        "unexpectedEmptyNativeEntries": [],
        "preservedEmptyPaths": sorted(empty),
        "nativeLogTotals": total,
        "verifiedNonemptyRewrites": sorted(verified, key=lambda row: row["path"]),
        "verifiedNonemptyRewriteCount": len(verified),
        "limitations": [
            "No Android installation or runtime behavior is established by this audit.",
            "Hash agreement proves the final APK retains the bytes logged by the privacy patch; it does not independently re-prove each rewrite's semantics.",
            "This supplementary probe does not replace the four primary real-APK cases and their format-aware privacy audits.",
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--original", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--apply-log", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    paths = [path.resolve() for path in (args.original, args.output, args.apply_log)]
    require(args.report.resolve() not in paths, "Report must not overwrite an input")
    try:
        report = audit(*paths)
    except (AuditError, OSError, ValueError, zipfile.BadZipFile, RuntimeError) as error:
        report = {"schema": "hiro-tiktok-metra-empty-native-v1", "status": "FAIL", "error": str(error)}
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print("EMPTY_NATIVE_AUDIT_FAILED " + str(error), file=sys.stderr)
        return 1
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("EMPTY_NATIVE_AUDIT_PASSED originalNative=593 outputNative=593 preservedEmpty=18 verifiedNonempty=" + str(report["verifiedNonemptyRewriteCount"]) + " outputSHA256=" + report["outputApk"]["sha256"])
    return 0


if __name__ == "__main__":
    sys.exit(main())
