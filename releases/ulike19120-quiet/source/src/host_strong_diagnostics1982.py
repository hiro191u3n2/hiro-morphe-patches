#!/usr/bin/env python3
"""Execute .82 Strong diagnostics against current and pinned published .81 code.

The production dispatcher and certificate service are compiled without edits.
Controlled transport pixels establish route/output/ownership equivalence, not
shader quality or device speed; retained actual-JNI quality tests are separate.
Only current-production assertions plus the executed differential comparison
are counted in the top-level total. Baseline assertions remain separate.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent
REFERENCE = {
    "GpuStrong1960.java": (82650, "08afcf63c4285ae2cfc5d31607577edd8c370a7284ff917524896376a1d0ab79"),
    "GpuQualification1961.java": (32359, "f4f159b4b77eede18c44d648e003f3f53bf7954176613605630ed850ff719590"),
}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def run(command, work, label):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=90)
    (work / (label + ".log")).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(label + "\n" + result.stdout + result.stderr)
    return result.stdout


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    reference = source / "tests1982/strong/reference81"
    manifest = json.loads((reference / "pins.json").read_text())
    if (manifest.get("baseline_version") != "1.9.81" or
            manifest.get("source_commit") != "9f9bf654ff572938fe083c70f2d6ac92f94b6e97" or
            set(manifest.get("files", {})) != set(REFERENCE)):
        raise AssertionError("Published .81 independent reference identity changed")
    for name, (size, digest) in REFERENCE.items():
        path = reference / name
        if path.stat().st_size != size or sha(path) != digest or manifest["files"][name] != {"bytes": size, "sha256": digest}:
            raise AssertionError("Published .81 reference changed: " + name)

    java_home = Path(jdk or os.environ.get("ULIKE_JDK_HOME") or
                     Path(shutil.which("javac")).resolve().parent.parent).resolve()
    fixtures = source / "tests1982/strong"
    holder_path = source / "host_qualification1981.py"
    spec = importlib.util.spec_from_file_location("strong1982_android_holders", holder_path)
    holders = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(holders)
    generated = work / "android-fixtures"
    android = []
    for name, body in holders.FIXTURES.items():
        if name.startswith("android/"):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            android.append(path)
    common = [fixtures / "StrongDiagnosticsFixtures1982.java", fixtures / "StrongDiagnostics1982Test.java", *android]
    inputs = [source / n for n in REFERENCE] + [reference / n for n in REFERENCE] + common + [reference / "pins.json", holder_path, source / "host_strong_diagnostics1982.py"]
    before = {str(p): sha(p) for p in inputs}
    results = {}
    for mode, root in (("legacy", reference), ("current", source)):
        classes = work / (mode + "-classes")
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        run([java_home / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-d", classes,
             *[root / n for n in REFERENCE], *common], work, mode + "-compile")
        output = run([java_home / "bin/java", "-ea", "-XX:ActiveProcessorCount=4", "-cp", classes,
                      "com.hiro.ulike.StrongDiagnostics1982Test", mode], work, mode + "-run")
        value = json.loads(output.strip().splitlines()[-1])
        if value.get("status") != "passed" or value.get("assertions", 0) <= 0:
            raise AssertionError("No executed " + mode + " regression assertions")
        results[mode] = value
    old, current = results["legacy"], results["current"]
    if old["differential_cases"] != current["differential_cases"] or current["differential_cases"] < 280:
        raise AssertionError("Independent old/new case sets differ")
    if old["decision_output_sha256"] != current["decision_output_sha256"]:
        raise AssertionError("Diagnostics changed selected routes, output, negatives, retry decisions or ownership")
    if before != {str(p): sha(p) for p in inputs}:
        raise AssertionError("Source changed during the actual diagnostic comparison")
    groups = current.get("tests", {})
    expected_groups = ("all_candidate_reason_combinations", "environment_profile_variant_and_live_speed",
                       "bank_refusal_and_public_fallback", "bank_release_and_cancellation",
                       "monitor_wait_and_legacy_timer_separation", "queue_cache_retry_and_observation")
    if any(groups.get(name, 0) <= 0 for name in expected_groups):
        raise AssertionError("Required current diagnostic regression did not execute")
    report = dict(status="passed", assertions=current["assertions"] + 3,
                  tests=groups, current=current, baseline=old,
                  differential_assertions=3, reference_version="1.9.81",
                  source_sha256=before, physical_android_tested=False,
                  device_speedup_verified=False, actual_jni=False,
                  fixture_classes_in_runtime=False,
                  scope="Actual current and pinned .81 Strong dispatcher/certificate service with controlled transport; route and output equivalence, no physical GPU or device-speed claim.")
    report.update(route_reason_combinations=True, certified_speed_not_quality_wait=True,
                  bank_refusal_classification=True, bank_wait_and_monitor_separate=True,
                  route_and_output_unchanged=True, qualification_queue_cache_separate=True,
                  qualification_retry_categories=True, status_observation_no_state_changes=True)
    (work / "result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=str(ROOT))
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
