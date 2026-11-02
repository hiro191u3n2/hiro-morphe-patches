#!/usr/bin/env python3
"""Execute bounded .83 Strong routing and history against a pinned .82 baseline.

Production dispatcher/certificate sources are compiled without rewriting. The
unchanged .82 deterministic transport proves output/ownership, not shader
quality or physical-device speed. Late real bank release intentionally changes
CPU/GPU selection; all resulting pixels remain equal. Existing .82 diagnostics
are retained separately, without weakening their assertions.
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
    "GpuStrong1960.java": "61b77a91e3fd971e430ed41facd0b15b93d2024c2d74d2df8567b2e96c27e07a",
    "GpuQualification1961.java": "a77116519e98ae548449348b676acc47200a2e9f02ec5dc5d169e50aa47f2c2a",
}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def run(command, work, label):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=90)
    (work / (label + ".log")).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(label + "\n" + result.stdout + result.stderr)
    return result.stdout


def result(output):
    value = json.loads(output.strip().splitlines()[-1])
    if value.get("status") != "passed" or value.get("assertions", 0) <= 0:
        raise AssertionError("No executed routing assertions")
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    reference = source / "tests1983/routing/reference82"
    for name, digest in REFERENCE.items():
        if sha(reference / name) != digest:
            raise AssertionError("Pinned .82 dispatcher/certificate source changed: " + name)
    java_home = jdk or os.environ.get("ULIKE_JDK_HOME")
    if java_home:
        java_home = Path(java_home).resolve()
    elif shutil.which("javac"):
        java_home = Path(shutil.which("javac")).resolve().parent.parent
    else:
        raise RuntimeError("Set --jdk or ULIKE_JDK_HOME to the established JDK")
    holder_path = source / "host_qualification1981.py"
    spec = importlib.util.spec_from_file_location("routing1983_android_holders", holder_path)
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
    common = [source / "tests1982/strong/StrongDiagnosticsFixtures1982.java",
              source / "tests1982/strong/StrongDiagnostics1982Test.java",
              source / "tests1983/routing/Routing1983Test.java", *android]
    extra = source / "tests1983/routing/Forecast1983Test.java"
    inputs = [source / n for n in REFERENCE] + [reference / n for n in REFERENCE] + common + [extra, holder_path, source / "host_routing1983.py"]
    before = {str(p): sha(p) for p in inputs}
    runs = {}
    for mode, root in (("legacy", reference), ("current", source)):
        classes = work / (mode + "-classes")
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        run([java_home / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-d", classes,
             *[root / n for n in REFERENCE], *common, *([] if mode == "legacy" else [extra])], work, mode + "-compile")
        runs[mode] = result(run([java_home / "bin/java", "-ea", "-XX:ActiveProcessorCount=4", "-cp", classes,
                                 "com.hiro.ulike.Routing1983Test", mode], work, mode + "-routing"))
        if mode == "current":
            runs["forecast"] = result(run([java_home / "bin/java", "-ea", "-XX:ActiveProcessorCount=4", "-cp", classes,
                                          "com.hiro.ulike.Forecast1983Test"], work, "current-forecast"))
    if runs["legacy"]["output_sha256"] != runs["current"]["output_sha256"]:
        raise AssertionError("Adaptive admission changed committed full ARGB/confidence outputs")
    required = {
        "current": ("late_real_release", "logged_distribution_without_release", "quality_speed_cohort_and_capacity_gates", "deadline_cooling_cancel_and_close"),
        "forecast": ("scope_expiry_and_monotonic_floor", "slow_cooling_and_retention_bound", "real_foreground_collection", "actual_routing_with_history", "resident_deadline_separation"),
    }
    for name, groups in required.items():
        if any(runs[name]["tests"].get(group, 0) <= 0 for group in groups):
            raise AssertionError("Missing executed routing group: " + name)
    if before != {str(p): sha(p) for p in inputs}:
        raise AssertionError("Production/fixture source changed during routing verification")
    report = dict(status="passed", assertions=runs["current"]["assertions"] + runs["forecast"]["assertions"] + 3,
                  tests={**runs["current"]["tests"], **runs["forecast"]["tests"]},
                  current=runs["current"], baseline=runs["legacy"], forecast=runs["forecast"],
                  differential_assertions=3, source_sha256=before, reference_version="1.9.82",
                  physical_android_tested=False, device_speedup_verified=False, actual_jni=False,
                  fixture_classes_in_runtime=False, routing_policy_changed1983=True,
                  expired_bank_recheck1983_verified=True, late_release_gpu_adoption1983_verified=True,
                  forecast_cross_capture1983_verified=True, forecast_scope_expiry_cooling1983_verified=True,
                  routing_quality_speed_cancellation1983_verified=True, routing_output_preserved1983_verified=True,
                  resident_deadline_preserved1983_verified=True, single_waiter_bound1983_verified=True,
                  scope="Actual dispatcher/certificate sources with controlled transport. .82 immediate expired-bank CPU selection differs intentionally from .83 bounded real-release adoption; selected full ARGB/confidence output is identical. Scalar forecast tests use explicit clocks and never certify a shader or device speed.")
    (work / "result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=str(ROOT))
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
