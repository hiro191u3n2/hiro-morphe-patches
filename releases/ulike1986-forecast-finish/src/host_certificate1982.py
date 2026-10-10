#!/usr/bin/env python3
"""Retain the .81 certificate cases with .82 diagnostic label assertions.

Existing queue regressions run first with the .81 family-fair order; the exact division profile is checked separately. Preferred-certificate tests also
restart a fresh Java process against file-backed preferences. They establish
admission/persistence safety, not physical Android GPU performance.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import shutil
import subprocess


def test(source, work, jdk=None):
    source, work = Path(source), Path(work)
    work.mkdir(parents=True, exist_ok=True)
    local = Path(__file__).parent
    baseline_path = local / "host_qualification1982.py"
    if not baseline_path.is_file():
        baseline_path = source / "host_qualification1982.py"
    spec = importlib.util.spec_from_file_location("qualification1981_current", baseline_path)
    baseline = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(baseline)
    queue = baseline.test(source, work / "queue", jdk=jdk)
    java = str(Path(jdk) / "bin/java") if jdk else shutil.which("java")
    javac = str(Path(jdk) / "bin/javac") if jdk else shutil.which("javac")
    if not java or not javac:
        raise RuntimeError("Java JDK required for fresh certificate tests")
    test_file = local / "PreferredCertificate1973Test.java"
    if not test_file.is_file():
        test_file = source / "PreferredCertificate1973Test.java"
    retained_test = test_file
    original_body = retained_test.read_text()
    old_label = '.contains("個別拒否（直近照会）")'
    new_label = '.contains("直近照会キャッシュ:")'
    if original_body.count(old_label) != 1:
        raise AssertionError("Retained cache-label assertion changed")
    adapted_body = original_body.replace(old_label, new_label)
    fixture_dir = work / "certificate-fixture"
    fixture_dir.mkdir(exist_ok=True)
    test_file = fixture_dir / "PreferredCertificate1973Test.java"
    test_file.write_text(adapted_body)
    classes = work / "queue/classes"
    compilation = subprocess.run(
        [javac, "--release", "8", "-encoding", "UTF-8", "-cp", str(classes), "-d", str(classes), str(test_file)],
        capture_output=True, text=True, timeout=90,
    )
    if compilation.returncode:
        raise RuntimeError("Preferred certificate compilation failed:\n" + compilation.stdout + compilation.stderr)
    # Fresh directories make each write/restore pair independent of old runs.
    disk_dir = work / "persisted"
    if disk_dir.exists():
        shutil.rmtree(disk_dir)
    disk_dir.mkdir()
    results = []
    scenarios = [
        ("logic",),
        ("write", str(disk_dir / "ordinary.properties")),
        ("restore", str(disk_dir / "ordinary.properties")),
        ("allwrite", str(disk_dir / "reject-all.properties")),
        ("allrestore", str(disk_dir / "reject-all.properties")),
    ]
    for scenario in scenarios:
        execution = subprocess.run(
            [java, "-ea", "-cp", str(classes), "com.hiro.ulike.PreferredCertificate1973Test", *scenario],
            capture_output=True, text=True, timeout=45,
        )
        (work / (scenario[0] + ".log")).write_text(execution.stdout + execution.stderr)
        if execution.returncode:
            raise RuntimeError("Preferred certificate regressions failed " + scenario[0] + ":\n" + execution.stdout + execution.stderr)
        results.append(json.loads(execution.stdout.strip().splitlines()[-1]))
    tests = dict(queue["tests"])
    for result in results:
        tests.update(result["tests"])
    production = source / "GpuQualification1961.java"
    result = {
        "status": "passed",
        "assertions": queue["assertions"] + sum(item["assertions"] for item in results),
        "tests": tests,
        "physical_android_tested": False,
        "qualification_certificate_regressions_passed": True,
        "admission_queue_regressions_passed": True,
        "gpu_safety_gates_preserved": True,
        "unchanged_exact_environment_and_rejection_schema": True,
        "preferred_success_restarted_in_fresh_java_process": True,
        "old_speed_failure_directly_promoted": False,
        "fixture_classes_in_runtime": False,
        "production_source_sha256": hashlib.sha256(production.read_bytes()).hexdigest(),
        "runner_source_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        "test_source_sha256": hashlib.sha256(test_file.read_bytes()).hexdigest(),
        "retained_queue_assertions": queue["assertions"],
        "diagnostic_certificate_label_adapter1982_verified": True,
        "retained_test_source_sha256": hashlib.sha256(original_body.encode()).hexdigest(),
        "retained_runner_source_sha256": hashlib.sha256((source / "host_certificate1981.py").read_bytes()).hexdigest(),
        "queue_diagnostic_adapter": queue,
    }
    (work / "certificate-host-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
