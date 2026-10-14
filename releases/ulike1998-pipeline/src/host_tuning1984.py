#!/usr/bin/env python3
"""Execute production .84 tuner progress and retain the .78/.79 safety cases.

The frozen historical tests/transport are not edited. A declared adapter updates
only the two obsolete expectations that a CPU-comparison cooldown/cap must stop
all missing exact-profile snapshots. The controlled parallel-CPU peer gets a
cancellation latch because a resumed signed child now enters that comparison
without first repeating its already completed pixel trials. Native shaders and
the real parallel speed comparator are covered by the retained GPU route suite;
this runner makes no claim about physical Android execution or device speed.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import shutil
import subprocess


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def replace_once(body, old, new):
    if body.count(old) != 1:
        raise AssertionError("Frozen adapter anchor changed: " + old[:140])
    return body.replace(old, new)


def fixture1984(body):
    body = replace_once(body,
        'static class Model {long residentBytes(){return 1024;}int height=8;}',
        'static class Model {long bytes1984=1024;long residentBytes(){return bytes1984;}int height=8;}')
    body = replace_once(body,
        'static final class Protection {QualityPixels1932.Plan plan;',
        'static final class Protection {static int dataCalls1984;QualityPixels1932.Plan plan;')
    body = replace_once(body,
        'PolicyData data(int w,int r,int y){return original;}',
        'PolicyData data(int w,int r,int y){dataCalls1984++;return original;}')
    body = replace_once(body,
        'static volatile int newMode,blockProfile=-1,blockVariant=-1,policyFailureProfile=-1,policyFailureVariant=-1;',
        'static volatile int fastProfile1984=-1;static volatile int newMode,blockProfile=-1,blockVariant=-1,policyFailureProfile=-1,policyFailureVariant=-1;')
    body = replace_once(body,
        'Thread.sleep(b.profile==7?',
        'Thread.sleep(b.profile==GpuNoise1960.fastProfile1984?1:b.profile==7?')
    old = ('final class GpuStrongRouting1978 {static int calls;static boolean slowerCpu;'
           'static long[] compareCpu1978(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.PolicyData d,int f,int v,int w,GpuQualification1961.Cancellation c){return slowerCpu?new long[]{100,200}:new long[]{200,100};}')
    new = '''final class GpuStrongRouting1978 {
    static int calls;static boolean slowerCpu,slowEarly1984;
    static volatile int cpuCalls1984,cpuBlockProfile1984=-1;
    static volatile CountDownLatch cpuStarted1984,cpuRelease1984;
    static long[] compareCpu1978(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.PolicyData d,int f,int v,int w,GpuQualification1961.Cancellation c){
        cpuCalls1984++;
        try{
            if(cpuBlockProfile1984==f){cpuStarted1984.countDown();cpuRelease1984.await();}
            if(GpuNoise1960.blocked&&(GpuNoise1960.blockProfile<0||GpuNoise1960.blockProfile==f)&&GpuNoise1960.blockVariant<0){
                GpuNoise1960.blockStarted.countDown();GpuNoise1960.blockRelease.await();
            }
        }catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException();}
        if(c.cancelled())throw new CancellationException();
        return slowerCpu||slowEarly1984&&f==4?new long[]{100,200}:new long[]{200,100};
    }'''
    return replace_once(body, old, new)


def historical_tests1984(body):
    old = ('cpuBefore=StrongNoise1958.cpuRuns.get();queue(cpuFast);'
           'check(GpuQualification1961.retainedBytes()==0&&StrongNoise1958.cpuRuns.get()==cpuBefore,"cooldown stops another snapshot before clone");')
    new = '''SaveQueue1935.idle=false;int comparisonBefore1984=GpuStrongRouting1978.cpuCalls1984;
        cpuBefore=StrongNoise1958.cpuRuns.get();queue(cpuFast);
        check(GpuQualification1961.retainedBytes()>0,"cooldown permits independent missing legacy exact profiles");
        age();drain();
        check(GpuStrongRouting1978.cpuCalls1984==comparisonBefore1984&&retries(cpuName)==0,"independent exact progress does not execute or spend the cooling CPU comparison");'''
    body = replace_once(body, old, new)
    old = ('expire(cpuName);cpuBefore=StrongNoise1958.cpuRuns.get();queue(cpuFast);\n'
           '        check(!GpuQualification1961.maySchedule(cpuName)&&GpuQualification1961.retainedBytes()==0&&StrongNoise1958.cpuRuns.get()==cpuBefore,"three failed retries stay blocked after cooldown expires");')
    new = '''expire(cpuName);SaveQueue1935.idle=false;comparisonBefore1984=GpuStrongRouting1978.cpuCalls1984;
        cpuBefore=StrongNoise1958.cpuRuns.get();queue(cpuFast);
        check(!GpuQualification1961.maySchedule(cpuName)&&GpuQualification1961.retainedBytes()>0,"capped CPU comparison does not block independent missing exact profiles");
        age();drain();
        check(GpuStrongRouting1978.cpuCalls1984==comparisonBefore1984&&retries(cpuName)==3,"the CPU comparison still enforces its three-retry cap");'''
    return replace_once(body, old, new)


def telemetry_faults1984(body):
    """A controlled provider throws before its own optional catch can run.

    This tests the unchanged production tuner's outer diagnostic boundary;
    admission, ownership, comparison, persistence and rejection code are copied
    verbatim into this separate fault peer, never changed in production.
    """
    body = replace_once(body, "public final class GpuQualification1961 {",
                        "public final class GpuQualification1961 {\n    private static volatile boolean telemetryFault1984=true;")
    for signature in (
        "public static void progress1984(String phase,int completed,int total){",
        "public static void timings1984(long cpuNanos,long gpuNanos){",
        "public static void outcome1984(String reason){"):
        body = replace_once(body, signature,
                            signature + '\n        if(telemetryFault1984)throw new LinkageError("controlled diagnostic provider failure");')
    return body


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    generated, classes = work / "fixtures", work / "classes"
    classes.mkdir(exist_ok=True)
    holders = module("tuning1984_android", source / "host_qualification1967.py")
    legacy = module("tuning1984_legacy_contract", source / "host_tuning1979.py")
    legacy._legacy_coverage(source)
    production = [source / name for name in (
        "GpuQualification1961.java", "GpuStrongTuning1975.java", "GpuSnapshotBudget1981.java")]
    originals = [source / name for name in (
        "gpu79-fixtures/TuningFixtures1979.java", "gpu79-fixtures/Tuning1979Test.java",
        "gpu78-fixtures/TuningFixtures1978.java", "gpu78-fixtures/Tuning1978Test.java",
        "host_tuning1979.py", "host_tuning1978.py", "host_qualification1967.py",
        "tests1981/GpuQueue1981Test.java", "tests1984/tuning/TuningProgress1984Test.java")]
    tracked = production + originals + [Path(__file__).resolve()]
    pins = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    inputs = list(production)
    for name, body in holders.FIXTURES.items():
        if name.startswith("android/"):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            inputs.append(path)
    fixture = generated / "com/hiro/ulike/TuningFixtures1979.java"
    fixture.parent.mkdir(parents=True, exist_ok=True)
    fixture.write_text(fixture1984((source / "gpu79-fixtures/TuningFixtures1979.java").read_text()))
    inherited = fixture.with_name("Tuning1979Test.java")
    inherited.write_text(historical_tests1984((source / "gpu79-fixtures/Tuning1979Test.java").read_text()))
    inputs += [fixture, inherited, source / "tests1981/GpuQueue1981Test.java",
               source / "tests1984/tuning/TuningProgress1984Test.java"]
    java = str(Path(jdk) / "bin/java") if jdk else shutil.which("java")
    javac = str(Path(jdk) / "bin/javac") if jdk else shutil.which("javac")
    if not java or not javac:
        raise RuntimeError("A JDK is required for the production tuner regressions")

    def run(label, command):
        result = subprocess.run(list(map(str, command)), text=True, capture_output=True, timeout=180)
        (work / (label + ".log")).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise RuntimeError(label + "\n" + result.stdout + result.stderr)
        if label.startswith("compile"):
            return None
        report = json.loads(result.stdout.strip().splitlines()[-1])
        if report.get("status") != "passed" or report.get("assertions", 0) <= 0:
            raise AssertionError("Executed tuner assertions missing: " + label)
        return report

    run("compile", [javac, "--release", "8", "-encoding", "UTF-8", "-d", classes, *inputs])
    prefix = [java, "-ea", "-cp", classes]
    retained = run("historical_tuning_with_declared_progress_expectations", prefix + ["com.hiro.ulike.Tuning1979Test"])
    budget = run("retained_queue_budget", prefix + ["com.hiro.ulike.GpuQueue1981Test"])
    progress = run("tuning_progress1984", prefix + ["com.hiro.ulike.TuningProgress1984Test"])
    fault_provider = work / "fault-provider/GpuQualification1961.java"
    fault_provider.parent.mkdir(parents=True, exist_ok=True)
    fault_provider.write_text(telemetry_faults1984(production[0].read_text()))
    fault_classes = work / "fault-classes"
    fault_classes.mkdir(exist_ok=True)
    fault_inputs = [fault_provider if path == production[0] else path for path in inputs]
    run("compile_telemetry_fault", [javac, "--release", "8", "-encoding", "UTF-8", "-d", fault_classes, *fault_inputs])
    fault = run("tuning_telemetry_fault1984", [java, "-ea", "-cp", fault_classes, "com.hiro.ulike.TuningProgress1984Test", "diagnostic-fault"])
    if pins != {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:
        raise AssertionError("Tuner regression source changed during execution")
    required = (
        "cpu_companion_cooldown_isolated_and_exact_child_resumed",
        "direct_profile_checkpoint_survives_later_capture_cancellation",
        "legacy_abba_checkpoint_and_later_exact_rejection",
        "cancelled_speed_stage_resumes_signed_child_without_reproof",
        "all_exact_outputs_and_five_percent_speed_gate_preserved",
        "early_speed_miss_does_not_block_later_profile",
        "shared_model_and_real_reservation_precede_snapshot_copy")
    if any(progress.get("tests", {}).get(name, 0) <= 0 for name in required):
        raise AssertionError("A new positive/negative progress case did not execute")
    result = dict(
        status="passed", assertions=retained["assertions"] + budget["assertions"] + progress["assertions"] + fault["assertions"],
        tests=dict(inherited_tuner=retained, queue_budget=budget, progress1984=progress, telemetry_fault1984=fault),
        physical_android_tested=False, device_speedup_verified=False, actual_jni=False,
        strong_companion_cooldown_isolation1984_verified=True,
        strong_profile_checkpoint1984_verified=True,
        strong_certified_resume1984_verified=True,
        strong_exact_and_speed_gates1984_preserved=True,
        strong_shared_snapshot_admission1984_verified=True,
        strong_optional_diagnostics1984_verified=True,
        twentyfour_candidate_tuning1978_verified=True,
        idle_two_full_argb_confidence_policy1978_verified=True,
        idle_snapshot_cancel_and_memory1978_verified=True,
        idle_upload_reuse1978_verified=True,
        new_gpu_balanced_five_percent_gate1978_verified=True,
        legacy_exact_rejections1978_preserved=True,
        worker_bound_speed_retry1978_verified=True,
        legacy_gpu_failure_direct_recovery1978_verified=True,
        idle_first_legacy_exact2_progress1979_verified=True,
        later_policy_rejection1979_preserved=True,
        original_1978_tuning_cases_retained1979_verified=False,
        historical_tuning_sources_unchanged1984_verified=True,
        declared_historical_expectation_adapter1984_verified=True,
        superseded_expectations=[
            "CPU-only comparison cooldown/cap no longer denies missing independent exact-profile snapshots",
            "A certified profile resumes at its missing speed comparison instead of repeating pixel qualification"],
        adapted_fixture_source_sha256=hashlib.sha256(fixture.read_bytes()).hexdigest(),
        adapted_test_source_sha256=hashlib.sha256(inherited.read_bytes()).hexdigest(),
        fault_provider_source_sha256=hashlib.sha256(fault_provider.read_bytes()).hexdigest(),
        source_sha256=pins)
    result.update({key: value for key, value in budget.items() if key.endswith("_verified")})
    (work / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
