#!/usr/bin/env python3
"""Retain .84 tuner coverage and execute .85 handoff regressions.

The new suite compiles the production parallel comparator together with the
production tuner, qualification service and snapshot budget. Only CPU/GPU
transport and worker peers are controlled. The frozen .84 tuner must fail the
three reported regressions, making the newly fixed behavior reviewable without
claiming Android performance or substituting a timing constant in production.
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


def fixture1985(body, prior):
    body = prior.fixture1984(body)
    marker = "\nfinal class GpuStrongRouting1978 {"
    if body.count(marker) != 1:
        raise AssertionError("Frozen routing peer boundary changed")
    body = body.split(marker)[0] + "\n"
    replace = prior.replace_once
    body = replace(body,
        "final class SpeedWorkers1935 {static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}static void run(Runnable[] a){for(Runnable r:a)r.run();}}",
        """final class SpeedWorkers1935 {
    static volatile int workers1985=2;static final AtomicInteger runs1985=new AtomicInteger();
    static int maxWorkers(){return workers1985;}static int availableWorkers1944(){return workers1985;}
    static boolean cpuIdle1944(){return true;}static long competitionEpoch1944(){return 1;}
    static void run(final Runnable[] tasks){
        runs1985.incrementAndGet();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] threads=new Thread[tasks.length];
        for(int n=0;n<tasks.length;n++){final Runnable task=tasks[n];threads[n]=new Thread(new Runnable(){public void run(){try{task.run();}catch(Throwable bad){failure.compareAndSet(null,bad);}}});threads[n].start();}
        try{for(Thread t:threads)t.join();}catch(InterruptedException cancelled){for(Thread t:threads)t.interrupt();Thread.currentThread().interrupt();throw new CancellationException();}
        Throwable bad=failure.get();if(bad instanceof RuntimeException)throw (RuntimeException)bad;if(bad instanceof Error)throw (Error)bad;if(bad!=null)throw new AssertionError(bad);
    }
}""")
    body = replace(body, "static volatile boolean unstable;",
                   "static volatile boolean unstable;static volatile int cpuDelay1985=40;")
    body = replace(body, "static float[] gpuEvidence1960(Model m)",
                   "static long workspaceBytes(int w,int rows){return 1024;}static float[] gpuEvidence1960(Model m)")
    body = replace(body, "int turn=cpuRuns.incrementAndGet();",
                   "try{if(cpuDelay1985>0)Thread.sleep(cpuDelay1985);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException();}int turn=cpuRuns.incrementAndGet();")
    body = replace(body, "static volatile int fastProfile1984=-1;",
                   "static volatile int[] lateFailure1985=new int[8],lateAfter1985=new int[8];static volatile int fastProfile1984=-1;")
    body = replace(body, "final class GpuStrong1960 {", """final class GpuStrong1960 {
    static final AtomicInteger cohortReads1985=new AtomicInteger();
    static volatile int cohortFailureProfile1985=-1,cohortFailure1985;
    static volatile boolean cohortBlock1985;
    static volatile CountDownLatch cohortStarted1985,cohortRelease1985;""")
    body = replace(body, "    static Read1971 read1971(", """    static Read1971 readCohort1978(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank,int[][] target){
        cohortReads1985.incrementAndGet();
        if(cohortBlock1985){cohortStarted1985.countDown();try{cohortRelease1985.await();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException();}}
        Read1971 value=read1971(session,ticket,u,bank);
        if(value.result!=null&&ticket.batch.profile==cohortFailureProfile1985){
            if(cohortFailure1985==1)value.result.pixels[value.result.pixels.length-1]^=1;
            if(cohortFailure1985==2&&value.result.confidence!=null)value.result.confidence[value.result.confidence.length-1]^=1;
            if(cohortFailure1985==3)return new Read1971(null,"policy_failure");
        }
        return value;
    }
    static Read1971 read1971(""")
    body = replace(body, "        return new Read1971(new Result(out,cf),null);", """        if(GpuNoise1960.readsByProfile.get(b.profile)>GpuNoise1960.lateAfter1985[b.profile]){
            int late=GpuNoise1960.lateFailure1985[b.profile];
            if(late==1)out[out.length-1]^=1;
            if(late==2&&cf!=null)cf[cf.length-1]^=1;
            if(late==3)return new Read1971(null,"policy_failure");
        }
        return new Read1971(new Result(out,cf),null);""")
    return body


def optional_faults1985(body, prior):
    body = prior.telemetry_faults1984(body)
    for signature in (
        "public static void selection1985(String childKey,int profile,int variant,boolean priorInvalidated){",
        "public static void comparison1985(String reference,int workers,long baselineNanos,long candidateNanos){"):
        body = prior.replace_once(body, signature,
                                  signature + '\n        if(telemetryFault1984)throw new LinkageError("controlled .85 diagnostic provider failure");')
    return body


def signature_preservation1985(javap, baseline_classes, classes):
    def methods(root, owner):
        body = subprocess.check_output([str(javap), "-p", "-s", "-classpath", str(root), owner], text=True)
        result, signature = set(), ""
        for line in body.splitlines():
            if "descriptor:" in line:
                if "(" in signature:
                    result.add((signature.split("(")[0].split()[-1], line.strip().split("descriptor: ")[1]))
                signature = ""
            elif "(" in line:
                signature = line.strip()
        return result

    removed, old_count, new_count = [], 0, 0
    for path in (baseline_classes / "com/hiro/ulike").glob("GpuStrongTuning1975*.class"):
        owner = "com.hiro.ulike." + path.stem
        old, new = methods(baseline_classes, owner), methods(classes, owner)
        old_count += len(old)
        new_count += len(new)
        removed.extend((owner, *method) for method in sorted(old - new))
    if old_count == 0 or removed:
        raise AssertionError("Published tuner method descriptors were removed: " + str(removed))
    return dict(baseline_methods=old_count, current_methods=new_count, removed_methods=removed)


def test(source, work, jdk=None, ndk=None, baseline_only=False):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    prior = module("tuning85_retained84", source / "host_tuning1984.py")
    holders = module("tuning85_android", source / "host_qualification1967.py")
    java = str(Path(jdk) / "bin/java") if jdk else shutil.which("java")
    javac = str(Path(jdk) / "bin/javac") if jdk else shutil.which("javac")
    javap = str(Path(jdk) / "bin/javap") if jdk else shutil.which("javap")
    if not java or not javac or not javap:
        raise RuntimeError("A JDK is required for production handoff regressions")
    baseline = source / "tests1985/tuning/reference84/GpuStrongTuning1975.java"
    expected = "27fbbdff1559cc761ec316c4e9d256014dd40cd68506bf163a1539466e50642d"
    if hashlib.sha256(baseline.read_bytes()).hexdigest() != expected:
        raise AssertionError("The published .84 tuner reference changed")
    production = [source / name for name in (
        "GpuQualification1961.java", "GpuStrongTuning1975.java", "GpuStrongRouting1978.java", "GpuSnapshotBudget1981.java")]
    new_test = source / "tests1985/tuning/TuningHandoff1985Test.java"
    tracked = production + [baseline, new_test, Path(__file__).resolve(), source / "host_tuning1984.py",
                            source / "gpu79-fixtures/TuningFixtures1979.java", source / "host_qualification1967.py"]
    pins = {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    generated = work / "fixtures"
    inputs = list(production)
    for name, body in holders.FIXTURES.items():
        if name.startswith("android/"):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            inputs.append(path)
    fixture = generated / "com/hiro/ulike/HandoffFixtures1985.java"
    fixture.parent.mkdir(parents=True, exist_ok=True)
    fixture.write_text(fixture1985((source / "gpu79-fixtures/TuningFixtures1979.java").read_text(), prior))
    inputs += [fixture, new_test]

    def run(label, command, failure=None):
        value = subprocess.run(list(map(str, command)), text=True, capture_output=True, timeout=180)
        (work / (label + ".log")).write_text(value.stdout + value.stderr)
        if failure:
            if value.returncode == 0 or failure not in value.stderr:
                raise AssertionError("Published .84 did not reproduce the expected failure: " + label + "\n" + value.stdout + value.stderr)
            return dict(status="expected_failure", assertion=failure)
        if value.returncode:
            raise RuntimeError(label + "\n" + value.stdout + value.stderr)
        if label.startswith("compile"):
            return None
        report = json.loads(value.stdout.strip().splitlines()[-1])
        if report.get("status") != "passed" or report.get("assertions", 0) <= 0:
            raise AssertionError("Executed handoff assertions missing: " + label)
        return report

    old_classes = work / "baseline-classes"
    old_classes.mkdir(exist_ok=True)
    old_inputs = [baseline if p == production[1] else p for p in inputs]
    run("compile_published84", [javac, "--release", "8", "-encoding", "UTF-8", "-d", old_classes, *old_inputs])
    regressions = {}
    for name, expected_failure in (
        ("last-baseline", "last rejected legacy baseline must hand the exact direct child to the real parallel CPU comparison"),
        ("alternate-baseline", "another retained exact legacy baseline must complete the direct candidate's missing speed proof"),
        ("cohort-terminal", "completion must retire the aggregate whose exact child failed in the optional cohort")):
        regressions[name] = run("published84_" + name, [java, "-ea", "-cp", old_classes, "com.hiro.ulike.TuningHandoff1985Test", name], expected_failure)
    if baseline_only:
        result = dict(status="passed", published84_expected_failures=regressions)
        (work / "baseline-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
        return result

    retained = prior.test(source, work / "retained84", jdk, ndk)
    classes = work / "classes"
    classes.mkdir(exist_ok=True)
    run("compile_production85", [javac, "--release", "8", "-encoding", "UTF-8", "-d", classes, *inputs])
    signatures = signature_preservation1985(javap, old_classes, classes)
    handoff = run("handoff1985", [java, "-ea", "-cp", classes, "com.hiro.ulike.TuningHandoff1985Test"])
    fault_provider = work / "fault-provider/GpuQualification1961.java"
    fault_provider.parent.mkdir(parents=True, exist_ok=True)
    fault_provider.write_text(optional_faults1985(production[0].read_text(), prior))
    fault_classes = work / "fault-classes"
    fault_classes.mkdir(exist_ok=True)
    fault_inputs = [fault_provider if p == production[0] else p for p in inputs]
    run("compile_diagnostic_fault85", [javac, "--release", "8", "-encoding", "UTF-8", "-d", fault_classes, *fault_inputs])
    fault = run("handoff_diagnostic_fault85", [java, "-ea", "-cp", fault_classes, "com.hiro.ulike.TuningHandoff1985Test", "diagnostic-fault"])
    if pins != {str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:
        raise AssertionError("Handoff regression sources changed during execution")
    required = (
        "last_legacy_rejection_reaches_real_parallel_cpu",
        "invalid_legacy_reference_reselects_another_exact_baseline",
        "cohort_exact_rejection_rechecks_terminal_selection",
        "cohort_failure_preserves_an_independent_legacy_route",
        "optional_cohort_speed_miss_preserves_base_proof",
        "worker_condition_child_variant_handoff_is_authoritative",
        "optional_cohort_cancellation_keeps_completed_progress",
        "exact_rejections_cpu_margin_and_cooldown_are_preserved")
    if any(handoff.get("tests", {}).get(name, 0) <= 0 or fault.get("tests", {}).get(name, 0) <= 0 for name in required):
        raise AssertionError("A required positive/negative handoff case did not execute")
    result = dict(retained)
    result.update(
        status="passed", assertions=retained["assertions"] + handoff["assertions"] + fault["assertions"],
        tests=dict(retained84=retained, handoff1985=handoff, diagnostic_fault1985=fault),
        published84_expected_failures=regressions,
        strong_late_baseline_recovery1985_verified=True,
        strong_terminal_selection_revalidation1985_verified=True,
        strong_worker_variant_handoff1985_verified=True,
        strong_handoff_exact_speed_and_cancellation1985_preserved=True,
        strong_handoff_optional_diagnostics1985_verified=True,
        strong_production_parallel_comparator1985_executed=True,
        strong_private_method_signatures1985_preserved=True,
        method_signature_preservation1985=signatures,
        historical_tuning_sources_unchanged1985_verified=True,
        physical_android_tested=False, device_speedup_verified=False, actual_jni=False,
        adapted_handoff_fixture_sha256=hashlib.sha256(fixture.read_bytes()).hexdigest(),
        source_sha256=pins)
    (work / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True)
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    parser.add_argument("--ndk")
    parser.add_argument("--baseline-only", action="store_true")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk, args.baseline_only), ensure_ascii=False, indent=2))
