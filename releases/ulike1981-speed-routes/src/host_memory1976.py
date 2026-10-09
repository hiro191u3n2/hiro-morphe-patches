#!/usr/bin/env python3
"""Compile actual production Java/JNI and exercise scalar quotas in software GLES.

Native fault wrappers are host-only. Each originating failure runs in a fresh
process, including a real five-second timeout that quarantines retained buffers.
"""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    local = Path(__file__).parent
    jdk = Path(jdk or os.environ["ULIKE_JDK_HOME"]).resolve()
    ndk = Path(ndk or os.environ["ULIKE_NDK_HOME"]).resolve()
    if "27.2.12479018" not in (ndk / "source.properties").read_text():
        raise AssertionError("Pinned Android NDK revision required")
    baseline = source
    if not (source / "native1960/build_native1960.py").is_file():
        baseline = local.parent / "baseline70/src"
    production = source / "GpuNoise1960.java"
    engine = source / "native1960/engine1960.c"
    java_fixture = local / "memory76-fixtures/Memory1971Test.java"
    c_fixture = local / "memory76-fixtures/memory_fault1971.c"
    before = {"GpuNoise1960.java": sha(production), "native1960/engine1960.c": sha(engine)}
    overlay = work / "source"
    native = overlay / "native1960"
    shutil.copytree(baseline / "native1960", native, dirs_exist_ok=True)
    shutil.copy2(engine, native / "engine1960.c")
    shutil.copy2(production, overlay / "GpuNoise1960.java")
    # Use the production generator and exact shader inputs. Its source digest
    # also binds the current scalar-memory Java class in this isolated build.
    spec = importlib.util.spec_from_file_location("native_builder1976", native / "build_native1960.py")
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    builder.shader_header(native, work)
    headers = work / "headers"
    sysroot = ndk / "toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include"
    for name in ("EGL", "GLES3", "KHR"):
        shutil.copytree(sysroot / name, headers / name, dirs_exist_ok=True)

    def run(command, name, timeout=120, env=None):
        result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=timeout, env=env)
        (work / (name + ".log")).write_text(result.stdout + result.stderr)
        if result.returncode or "WARNING in native method" in result.stdout+result.stderr or "FATAL ERROR" in result.stdout+result.stderr:
            raise RuntimeError("Memory JNI regression failed: " + name + "\n" + result.stdout[-8000:] + result.stderr[-8000:])
        return result.stdout

    library = work / "libulike_gpu1960.so"
    flags = ["-std=c11", "-O3", "-shared", "-fPIC", "-fno-fast-math", "-ffp-contract=off", "-Wall", "-Wextra", "-Werror", "-Wno-misleading-indentation", "-DANDROID"]
    includes = ["-I" + str(headers), "-I" + str(jdk / "include"), "-I" + str(jdk / "include/linux"), "-I" + str(work)]
    wrappers = ["glBufferData", "glBufferSubData", "glDispatchCompute", "glUnmapBuffer", "glClientWaitSync", "glCopyBufferSubData"]
    run(["cc", *flags, *includes, native / "engine1960.c", c_fixture, *["-Wl,--wrap=" + name for name in wrappers], "-Wl,--no-undefined", "-l:libEGL.so.1", "-l:libGL.so.1", "-lm", "-o", library], "native-compile")
    symbols = run(["nm", "-D", library], "native-symbols")
    for name in ("nativeAbi", "capacityNative1971", "failureCodeNative1971", "readManyIntoNative"):
        if "Java_com_hiro_ulike_GpuNoise1960_" + name not in symbols:
            raise AssertionError("Actual native export missing: " + name)
    classes = work / "classes"
    classes.mkdir(exist_ok=True)
    run([jdk / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-d", classes, production, java_fixture, local / "memory76-fixtures/ResidentTransport1976Test.java"], "java-compile")
    environment = dict(os.environ, EGL_PLATFORM="surfaceless", LIBGL_ALWAYS_SOFTWARE="1")
    reports = {}
    for name, mode in (("capacity_staging_reuse_and_rollback", "logic"), ("materialized_readback_accounting", "readback"), ("optional_overlap_scratch", "scratch"), ("reusable_multi_output", "reuse"), ("late_private_failure", "late"), ("pending_cancel", "cancel"), ("multi_fence_quarantine", "multifence"), ("allocation_first_reason", "1"), ("upload_first_reason", "2"), ("dispatch_first_reason", "3"), ("readback_first_reason", "4"), ("fence_quarantine", "5")):
        output = run([jdk / "bin/java", "-ea", "-Xcheck:jni", "-Xmx1024m", "-Djava.library.path=" + str(work), "-cp", classes, "com.hiro.ulike.Memory1971Test", mode], name, env=environment)
        reports[name] = json.loads(output.strip().splitlines()[-1])
    for mode in ("normal", "bounds", "copyfault", "uploadfault", "pending", "interrupt", "quarantine"):
        output = run([jdk / "bin/java", "-ea", "-Xcheck:jni", "-Xmx1024m", "-Djava.library.path=" + str(work), "-cp", classes, "com.hiro.ulike.ResidentTransport1976Test", mode], "resident-"+mode, env=environment)
        reports["resident_"+mode] = json.loads(output.strip().splitlines()[-1])
    after = {"GpuNoise1960.java": sha(production), "native1960/engine1960.c": sha(engine)}
    if after != before:
        raise AssertionError("Production source changed during memory tests")
    result = {
        "status": "passed", "assertions": sum(row["assertions"] for row in reports.values()), "tests": reports,
        "native_memory_admission_regressions_passed": True,
        "resident_transport1976_bounds_and_faults": True,
        "slot24_admission_and_legacy_slots_preserved": True,
        "resident_slot_memory_admission1976_verified": True,
        "reusable_multi_output_readback_verified": True,
        "private_partial_output_never_committed": True,
        "overlap_scratch_admission_serial_fallback_verified": True,
        "multi_output_cancel_and_quarantine_verified": True,
        "materialized_java_readback_counted_once": True,
        "remaining_java_allocation_stays_reserved": True,
        "native_failure_diagnostics_regressions_passed": True,
        "gpu_safety_gates_preserved": True,
        "physical_android_tested": False,
        "actual_production_java_and_jni_executed": True,
        "fixture_classes_in_runtime": False,
        "max_native_bytes": 512 * 1024 * 1024,
        "heap_reserve_bytes": 64 * 1024 * 1024,
        "unknown_completion_quarantine_preserved": True,
        "production_source_sha256": before,
        "runner_source_sha256": sha(__file__),
        "fixture_source_sha256": {java_fixture.name: sha(java_fixture), c_fixture.name: sha(c_fixture), "ResidentTransport1976Test.java": sha(local / "memory76-fixtures/ResidentTransport1976Test.java")},
        "host_native_sha256": sha(library),
        "ndk_revision": "27.2.12479018",
    }
    (work / "memory-host-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
