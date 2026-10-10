#!/usr/bin/env python3
"""Replay the logged 24.47MP resident admission using production Java and JNI.

The published .82 Java pair is SHA-pinned and runs in a separate JVM against
the same current native engine. Android Bitmap and a competing memory owner
are explicit host fixtures. Real native allocation, CPU uploads, readbacks,
lease rollback and cancellation execute without rewriting production methods.
The complete retained .76 JNI memory suite also checks legacy API behavior.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
PUBLISHED = {
    "GpuNoise1960.java": "6144b8b4436d9f45399a92b598090b15ae4c20651bee1dcaa6f5817d156f2a8b",
    "GpuChain1961.java": "15326a93fab9363941830859e4a6340fc92d1258bf618c854bd5ef79cfdea6a9",
}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def run(command, work, label, env=None, timeout=240):
    result = subprocess.run(list(map(str, command)), text=True, capture_output=True,
                            env=env, timeout=timeout)
    output = result.stdout + result.stderr
    (work / (label + ".log")).write_text(output)
    if result.returncode or "WARNING in native method" in output or "FATAL ERROR" in output:
        raise AssertionError(label + "\n" + output[-18000:])
    return result.stdout


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    jdk = Path(jdk or os.environ["ULIKE_JDK_HOME"]).resolve()
    ndk = Path(ndk or os.environ["ULIKE_NDK_HOME"]).resolve()
    jar = Path(os.environ.get("ULIKE_ANDROID_JAR", str(jdk.parent / "android.jar"))).resolve()
    if not jar.is_file():
        raise AssertionError("Pinned Android API jar required")
    work.mkdir(parents=True, exist_ok=True)
    baseline = source / "tests1983/memory/baseline82"
    declaration = json.loads((baseline / "pins.json").read_text())
    if declaration.get("version") != "1.9.82" or declaration.get("published_commit") != "246c498e9d60bd99946c791824cae29b262cbe1c":
        raise AssertionError("Wrong published memory baseline")
    for name, digest in PUBLISHED.items():
        if sha(baseline / name) != digest or declaration["files"][name]["sha256"] != digest:
            raise AssertionError("Published .82 memory source changed: " + name)
    own = [source / name for name in PUBLISHED] + [source / "native1960/engine1960.c",
           source / "host_memory1983.py", source / "tests1983/memory/MemoryOwners1983.java",
           source / "tests1983/memory/MemoryBudget1983Test.java", baseline / "pins.json"] + [baseline / name for name in PUBLISHED]
    before = {str(path): sha(path) for path in own}

    # Build the actual JNI engine once and run every unmodified legacy quota,
    # readback, fault and quarantine assertion before the new contract cases.
    inherited = module("memory1983_retained1976", source / "host_memory1976.py")
    legacy = inherited.test(source, work / "legacy1976", jdk=jdk, ndk=ndk)

    sys.path.insert(0, str(source))
    builder = module("memory1983_production_closure", source / "build1982.py")
    production = list(builder.compile_inputs().values()) + [builder.production_path(name) for name in builder.PRODUCTION]
    production = list(dict.fromkeys(production))
    closure_before = {str(path): sha(path) for path in production}
    classes, fixture_classes, old_classes = work / "production-classes", work / "fixture-classes", work / "published82-classes"
    for path in (classes, fixture_classes, old_classes):
        path.mkdir(exist_ok=True)
    run([jdk / "bin/javac", "-source", "8", "-target", "8", "-Xlint:-options", "-encoding", "UTF-8",
         "-bootclasspath", jar, "-d", classes, *production], work, "production-compile")

    holders = module("memory1983_android_holders", source / "host_qualification1967.py")
    fixture_sources = []
    for name, body in holders.FIXTURES.items():
        if name.startswith("android/"):
            path = work / "android-fixtures" / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            fixture_sources.append(path)
    fixture_sources += [source / name for name in (
        "tests/pipeline1942-fixtures/android/graphics/Bitmap.java",
        "tests/pipeline1942-fixtures/android/graphics/ColorSpace.java",
        "tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java",
        "tests1983/memory/MemoryOwners1983.java",
        "tests1983/memory/MemoryBudget1983Test.java")]
    cp = os.pathsep.join(map(str, (classes, jar)))
    run([jdk / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-cp", cp,
         "-d", fixture_classes, *fixture_sources], work, "fixture-compile")
    run([jdk / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-cp", cp,
         "-d", old_classes, *[baseline / name for name in PUBLISHED]], work, "published82-compile")

    environment = dict(os.environ, EGL_PLATFORM="surfaceless", LIBGL_ALWAYS_SOFTWARE="1")
    reports = {}
    for name, roots in (("baseline82", (fixture_classes, old_classes, classes, jar)),
                        ("current83", (fixture_classes, classes, jar))):
        output = run([jdk / "bin/java", "-ea", "-Xcheck:jni", "-XX:ActiveProcessorCount=4", "-Xmx1g",
                      "-Djava.library.path=" + str(work / "legacy1976"), "-cp", os.pathsep.join(map(str, roots)),
                      "com.hiro.ulike.MemoryBudget1983Test", name], work, name, env=environment)
        report = json.loads(output.strip().splitlines()[-1])
        if report.get("status") != "passed" or report.get("assertions", 0) <= 0:
            raise AssertionError("Missing executed memory assertions: " + name)
        reports[name] = report

    if before != {name: sha(name) for name in before}:
        raise AssertionError("Memory implementation, native engine or regression fixture changed during test")
    if closure_before != {name: sha(name) for name in closure_before}:
        raise AssertionError("Production dependency closure changed during memory test")
    result = {
        "status": "passed",
        "assertions": legacy["assertions"] + sum(report["assertions"] for report in reports.values()),
        "tests": reports,
        "legacy1976": legacy,
        "published82_logged_dimensions_rejected_verified": True,
        "generated_output_transfer_budget1983_verified": True,
        "forecast_and_runtime_lease_agree1983_verified": True,
        "real_upload_staging_and_temporary_preserved1983_verified": True,
        "undeclared_upload_before_jni_rejected1983_verified": True,
        "readback_consume_exception_cancel_ownership1983_verified": True,
        "physical_memory_and_512mib_limit1983_preserved": True,
        "legacy_api_memory_contract1983_preserved": True,
        "actual_production_java_and_jni_executed": True,
        "physical_android_tested": False,
        "device_speedup_verified": False,
        "source_sha256": before,
        "production_closure_sha256": closure_before,
        "fixture_sha256": {str(path): sha(path) for path in fixture_sources},
    }
    (work / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=str(ROOT))
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
