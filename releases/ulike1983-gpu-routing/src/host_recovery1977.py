#!/usr/bin/env python3
"""Compile real .76/.77 certificate services and verify recovery across JVMs.

The reference .76 service generates the old signed records and overflow marker.
The same recovery assertions must fail on that old service for the intended
reason. Each write/restart pair uses real file-backed SharedPreferences in a
fresh Java process. This tests admission and persistence, not Android GPU speed.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import shutil
import subprocess


PROOF_SCHEMA = "gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1"
PREFERRED_SCHEMA = PROOF_SCHEMA + "-strong-exact2-preferred-v1"
EXACT_SCHEMA = "gx1977-exact-per-key-v1"


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    local = Path(__file__).resolve().parent
    fixture_root = local / "recovery77-fixtures"
    if not fixture_root.is_dir():
        fixture_root = source / "recovery77-fixtures"
    java_test = fixture_root / "Recovery1977Test.java"
    legacy_source = fixture_root / "reference76/GpuQualification1961.java"
    production = source / "GpuQualification1961.java"
    baseline_path = local / "host_qualification1967.py"
    if not baseline_path.is_file():
        baseline_path = source / "host_qualification1967.py"
    for required in (java_test, legacy_source, production, baseline_path):
        if not required.is_file():
            raise RuntimeError("Missing required recovery input: " + str(required))
    spec = importlib.util.spec_from_file_location("recovery1977_android_stubs", baseline_path)
    baseline = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(baseline)
    java = str(Path(jdk) / "bin/java") if jdk else shutil.which("java")
    javac = str(Path(jdk) / "bin/javac") if jdk else shutil.which("javac")
    if not java or not javac:
        raise RuntimeError("Java JDK required for recovery persistence tests")
    production_hash = sha256(production)
    generated = work / "android-fixtures"
    generated.mkdir(exist_ok=True)
    stubs = []
    for relative, contents in baseline.FIXTURES.items():
        # Use only platform/peer stubs; there is never a substituted or mocked
        # GpuQualification1961 in either compilation.
        if relative.endswith("QueueHost1967.java"):
            continue
        path = generated / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents, encoding="utf-8")
        stubs.append(path)
    classes = {}
    for label, service in (("current", production), ("legacy76", legacy_source)):
        target = work / (label + "-classes")
        if target.exists():
            shutil.rmtree(target)
        target.mkdir()
        compiled = subprocess.run(
            [javac, "--release", "8", "-encoding", "UTF-8", "-d", str(target),
             *map(str, stubs), str(service), str(java_test)],
            capture_output=True, text=True, timeout=90,
        )
        (work / (label + "-compile.log")).write_text(compiled.stdout + compiled.stderr)
        if compiled.returncode:
            raise RuntimeError("Recovery compilation failed " + label + ":\n" + compiled.stdout + compiled.stderr)
        classes[label] = target
    disk = work / "persisted"
    if disk.exists():
        shutil.rmtree(disk)
    disk.mkdir()
    results, controls, executions = [], [], []

    def run(label, mode, preferences, expected_assertion=None):
        executed = subprocess.run(
            [java, "-ea", "-cp", str(classes[label]), "com.hiro.ulike.Recovery1977Test", mode, str(preferences)],
            capture_output=True, text=True, timeout=60,
        )
        log = work / (label + "-" + mode + ".log")
        log.write_text(executed.stdout + executed.stderr)
        executions.append({"service": label, "scenario": mode, "exit_code": executed.returncode, "log": str(log)})
        if expected_assertion is not None:
            if executed.returncode == 0 or ("java.lang.AssertionError: " + expected_assertion) not in executed.stderr:
                raise RuntimeError("Old-service negative control did not fail for the expected regression " + mode + ":\n" + executed.stdout + executed.stderr)
            controls.append({"status": "passed", "scenario": mode, "expected_assertion": expected_assertion, "observed_exit_code": executed.returncode})
            return
        if executed.returncode:
            raise RuntimeError("Recovery regressions failed " + label + "/" + mode + ":\n" + executed.stdout + executed.stderr)
        parsed = json.loads(executed.stdout.strip().splitlines()[-1])
        if parsed.get("status") != "passed" or not isinstance(parsed.get("assertions"), int) or parsed["assertions"] <= 0:
            raise RuntimeError("Recovery result did not establish passing assertions " + mode)
        results.append(parsed)

    legacy_prefs = disk / "original76.properties"
    run("legacy76", "legacy-seed", legacy_prefs)
    # Negative controls are assertions about behavior, not about implementation
    # strings or compilation. They prove the original bug remains detectable.
    run("legacy76", "legacy-control", legacy_prefs,
        "legacy blanket recovery control: no exact evidence may be invented")
    run("legacy76", "family-control", disk / "legacy76-family-control.properties",
        "CPU family overflow control: no exact evidence may be invented")
    same, changed = disk / "same-environment.properties", disk / "new-environment.properties"
    shutil.copyfile(legacy_prefs, same)
    shutil.copyfile(legacy_prefs, changed)
    run("current", "legacy-control", same)
    run("current", "family-control", disk / "current-family-control.properties")
    for mode in ("migrate-same", "restart-same"):
        run("current", mode, same)
    for mode in ("migrate-environment", "restart-environment"):
        run("current", mode, changed)
    for mode in ("journal-write", "journal-restart"):
        run("current", mode, disk / "journal.properties")
    for mode in ("speed-write", "speed-restart"):
        run("current", mode, disk / "speed.properties")
    run("current", "integrity", disk / "integrity.properties")
    if sha256(production) != production_hash:
        raise RuntimeError("Production qualification source changed during recovery tests")
    tests = {}
    for result in results:
        for name, evidence in result["tests"].items():
            if name in tests:
                raise RuntimeError("Duplicate recovery evidence section: " + name)
            tests[name] = evidence
    result = {
        "status": "passed",
        "assertions": sum(item["assertions"] for item in results),
        "tests": tests,
        "qualification_proof_schema1977": PROOF_SCHEMA,
        "qualification_preferred_schema1977": PREFERRED_SCHEMA,
        "exact_schema1977": EXACT_SCHEMA,
        "qualification_epoch_recovery_passed": True,
        "old_positive_certificates_invalidated": True,
        "old_preferred_certificates_invalidated": True,
        "legacy_blanket_not_treated_as_exact_evidence": True,
        "signed_legacy_individual_failures_preserved": True,
        "exact_journal_655_survives_process_restart": True,
        "unknown_1000_lookups_do_not_persist": True,
        "unknown_keys_require_fresh_proof": True,
        "rejection_families_and_environments_isolated": True,
        "speed_failures_distinct_from_exact": True,
        "hot_failure_cache_bounded_64": True,
        "original_1976_regression_negative_controls_passed": True,
        "new_exact_journal_signature_integrity_passed": True,
        "physical_android_tested": False,
        "gpu_performance_claimed": False,
        "fixture_classes_in_runtime": False,
        "production_source_modified_by_test": False,
        "recovery_individual_exact_failures": 655,
        "recovery_unknown_keys_read_per_scenario": 1000,
        "recovery_candidate_family_and_mode_cases": 28,
        "hot_failure_cache_limit": 64,
        "separate_jvm_invocations": len(executions),
        "negative_controls": controls,
        "executions": executions,
        "production_source_sha256": production_hash,
        "reference76_source_sha256": sha256(legacy_source),
        "runner_source_sha256": sha256(Path(__file__)),
        "test_source_sha256": sha256(java_test),
        "android_stub_provider_sha256": sha256(baseline_path),
    }
    (work / "recovery-host-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
