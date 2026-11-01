#!/usr/bin/env python3
"""Keep the frozen .83 routing guards and execute bounded .86 history repair.

Only the old expectation that 1.5-second cooling deletes the entire history is
superseded. A source-pinned work adapter moves that recovery expectation to the
unchanged 12-second observation TTL. Published .85 and current production are
both compiled directly for explicit-clock and actual-routing comparisons.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil

ROOT = Path(__file__).resolve().parent
PINS = {
    "host_routing1983.py": "c4420101ad322c1c60c17bdb8e7e67d04b59de00a61539feb9e479bceba97309",
    "tests1982/strong/StrongDiagnosticsFixtures1982.java": "2ce4add71d5177e5296d54a95ad10b75c119b6bb25d5c134ea4561649ffbfb79",
    "tests1983/routing/Forecast1983Test.java": "f6eb83840df04c5afc867db5b69b7c544d0a45b1b4c98e6c7407c82bb176834d",
    "tests1986/forecast/reference85/GpuStrong1960.java": "cc92980c5250ececd53686e5728ce62091a1dac9ba23c1e87b0ed77f86c6dbad",
    "tests1986/qualification/baseline85/GpuQualification1961.java": "f04426e4ded796d14d76c56a62e87ea9e873efaa2f041a9808f35f59c001782d",
}

TIMING_FIXTURE = """
    static int forecastCalls1986,forecastState1986,forecastSamples1986,forecastFailure1986;
    static long forecastAge1986,forecastCertified1986,forecastObserved1986,forecastChosen1986;
    static void strongForecast1986(Trace t,int state,int samples,long age,long certified,long observed,long chosen){
        forecastCalls1986++;trace(t);
        if(state<0||state>5||samples<0||samples>3||age< -1||certified<=0||observed<0||chosen<=0)
            throw new AssertionError("invalid scalar forecast diagnostic");
        switch(forecastFailure1986){
            case 1:throw new RuntimeException("injected optional forecast diagnostic");
            case 2:throw new LinkageError("injected optional forecast diagnostic");
            case 3:throw new AssertionError("injected optional forecast diagnostic");
            case 4:throw new OutOfMemoryError("injected optional forecast diagnostic");
        }
        forecastState1986=state;forecastSamples1986=samples;forecastAge1986=age;
        forecastCertified1986=certified;forecastObserved1986=observed;forecastChosen1986=chosen;
    }
"""

EXPECTATION_UPDATES = (
    ('check(Forecast1983.cost(slow,gpu,now+Forecast1983.SLOW_COOLING)==gpu,"cooling expiry permits the original qualified path to be measured again");',
     'check(Forecast1983.cost(slow,gpu,now+Forecast1983.SLOW_COOLING)==120*MS,"cooling expiry retains both fresh capture observations");'),
    ('check(cache().isEmpty(),"speed hints do not become permanent quality rejection entries");',
     'check(cache().size()==1,"fresh scalar history remains separate from permanent quality rejection entries");'),
    ('Forecast1983.cost(single,proof.gpuNanos,now+Forecast1983.SLOW_COOLING);',
     'Forecast1983.cost(single,proof.gpuNanos,now+Forecast1983.TTL);'),
    ('"expired cooldown returns to the same certified GPU candidate"',
     '"expired observation history returns to the same certified GPU candidate"'),
)

CALL_SITE_UPDATES = (
    ('return Forecast1983.cost1986(forecast,proof.gpuNanos,System.nanoTime(),cost,stage.trace);',
     'return Math.max(cost,Forecast1983.cost(forecast,proof.gpuNanos,System.nanoTime()));'),
    ('stage.bankDuration[i]=Forecast1983.cost1986(forecast,stage.bankCertified1983[i],now,stage.bankDuration[i],stage.trace);',
     'stage.bankDuration[i]=Math.max(stage.bankDuration[i],Forecast1983.cost(forecast,stage.bankCertified1983[i],now));'),
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def once(body, old, new):
    if body.count(old) != 1:
        raise AssertionError("Source-pinned adapter location is not unique: " + old)
    return body.replace(old, new)


def load(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def forecast_span(body):
    begin = body.index("    /** .83 scalar forecasts are hints only, never pixel certificates.")
    end = body.index("    private static boolean interrupted()", begin)
    return begin, end


def production_scope(source):
    baseline = (source / "tests1986/forecast/reference85/GpuStrong1960.java").read_text()
    current = (source / "GpuStrong1960.java").read_text()
    a, b = forecast_span(current)
    x, y = forecast_span(baseline)
    restored = current[:a] + baseline[x:y] + current[b:]
    for new, old in CALL_SITE_UPDATES:
        restored = once(restored, new, old)
    if restored != baseline:
        raise AssertionError("Strong source changed outside bounded scalar history and its two optional diagnostic call sites")
    # The old forecaster entry points retain their exact source signatures.
    for signature in (
        "static synchronized long capture()",
        "static String key(String environment,String route,int variant,int workers,int parallel,long cpu,long gpu)",
        "static synchronized long cost(String key,long certified,long now)",
        "static synchronized void completed(String key,long capture,long elapsed,long cpu,long now)",
    ):
        if baseline.count(signature) != 1 or current.count(signature) != 1:
            raise AssertionError("Published forecaster signature was removed: " + signature)
    return hashlib.sha256(restored.encode()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_file():
        source = source.parent
    work.mkdir(parents=True, exist_ok=True)
    for relative, digest in PINS.items():
        if sha(source / relative) != digest:
            raise AssertionError("Frozen source or published .85 baseline changed: " + relative)
    inverse_sha = production_scope(source)
    java_home = jdk or os.environ.get("ULIKE_JDK_HOME")
    if java_home:
        java_home = Path(java_home).resolve()
    elif shutil.which("javac"):
        java_home = Path(shutil.which("javac")).resolve().parent.parent
    else:
        raise RuntimeError("Set --jdk or ULIKE_JDK_HOME to the established JDK")
    fixture_original = source / "tests1982/strong/StrongDiagnosticsFixtures1982.java"
    forecast_original = source / "tests1983/routing/Forecast1983Test.java"
    temporal = source / "tests1986/forecast/ForecastRetention1986Test.java"
    adapter_dir = work / "adapters"
    adapter_dir.mkdir(exist_ok=True)
    fixture_adapter = adapter_dir / fixture_original.name
    forecast_adapter = adapter_dir / forecast_original.name
    fixture_adapter.write_text(once(fixture_original.read_text(), "final class ProcessingTiming1947 {\n",
                                   "final class ProcessingTiming1947 {\n" + TIMING_FIXTURE))
    forecast_body = forecast_original.read_text()
    for old, new in EXPECTATION_UPDATES:
        forecast_body = once(forecast_body, old, new)
    forecast_adapter.write_text(forecast_body)
    sources = [source / p for p in PINS] + [source / "GpuStrong1960.java", source / "GpuQualification1961.java",
               source / "host_routing1986.py", temporal, fixture_adapter, forecast_adapter]
    before = {str(p): sha(p) for p in sources}
    old = load(source / "host_routing1983.py", "retained_routing1983_for_1986")
    old_run = old.run

    def adapted_run(command, destination, label):
        adapted = []
        for part in command:
            text = str(part)
            adapted.append(fixture_adapter if text == str(fixture_original) else
                           forecast_adapter if text == str(forecast_original) else part)
        if label == "current-compile":
            adapted.append(temporal)
        return old_run(adapted, destination, label)

    legacy_work = work / "retained-1983"
    old.run = adapted_run
    try:
        retained = old.test(source, legacy_work, java_home, ndk)
    finally:
        old.run = old_run
    baseline_classes = work / "baseline85-classes"
    if baseline_classes.exists():
        shutil.rmtree(baseline_classes)
    baseline_classes.mkdir()
    old_run([java_home / "bin/javac", "--release", "8", "-encoding", "UTF-8", "-d", baseline_classes,
             source / "tests1986/forecast/reference85/GpuStrong1960.java",
             source / "tests1986/qualification/baseline85/GpuQualification1961.java",
             fixture_adapter, source / "tests1982/strong/StrongDiagnostics1982Test.java", temporal,
             *sorted((legacy_work / "android-fixtures").rglob("*.java"))], work, "baseline85-compile")
    runs = {}
    for mode, classes in (("legacy", baseline_classes), ("current", legacy_work / "current-classes")):
        runs[mode] = old.result(old_run([java_home / "bin/java", "-ea", "-XX:ActiveProcessorCount=4", "-cp", classes,
                                        "com.hiro.ulike.ForecastRetention1986Test", mode], work, mode + "-temporal86"))
    baseline, current = runs["legacy"], runs["current"]
    if baseline["baseline_loss_cases"] != 4 or current["baseline_loss_cases"] != 0:
        raise AssertionError("Published .85 loss and .86 repair were not both reproduced")
    if baseline["route_output_sha256"] != current["route_output_sha256"]:
        raise AssertionError("The deliberate forecast routing difference changed committed output")
    required = ("capture_gap_sequence", "seconds_apart_actual_routing", "cooling_independent_ttl_recovery",
                "distinct_captures_and_scalar_bounds", "same_scan_scalar_diagnostics",
                "actual_current_stage_and_bank_floors", "optional_diagnostics_and_real_recovery")
    if any(current["tests"].get(group, 0) <= 0 for group in required):
        raise AssertionError("A required current-production temporal regression group did not execute")
    if before != {str(p): sha(p) for p in sources}:
        raise AssertionError("Production, baseline, test, runner or adapter changed during execution")
    report = dict(retained)
    report.update(status="passed", assertions=retained["assertions"] + current["assertions"] + 3,
                  tests={**retained["tests"], **current["tests"]}, retained1983=retained,
                  baseline85=baseline, temporal1986=current, differential_assertions1986=3,
                  source_sha256={**retained["source_sha256"], **before}, baseline85_source_sha256=PINS,
                  strong_nonforecast_inverse_sha256=inverse_sha, reference_version="1.9.85",
                  intentional_old_expectation_updates=[{"before": old, "after": new} for old, new in EXPECTATION_UPDATES],
                  forecast_history_survives_cooling1986_verified=True,
                  forecast_real_capture_gaps1986_verified=True,
                  forecast_expiry_bounded_recovery1986_verified=True,
                  forecast_no_single_capture_or_optimistic_reuse1986_verified=True,
                  forecast_diagnostics_scalar_optional1986_verified=True,
                  forecast_baseline85_loss_reproduced1986=True,
                  forecast_legacy_guards_retained1986_verified=True,
                  strong_nonforecast_source1986_preserved=True,
                  scope="Compiled published .85 and current production with controlled transport. The same lookup-before-completion sequence reproduces loss at 1.5, 3, 4.675 and 11 seconds in .85; .86 retains all still-fresh pairs. Two distinct captures, conservative maximum, original certificate floor, exact 12-second expiry, 64 keys, three captures and original bank admission limits remain authoritative. Optional scalar diagnostics are failure-isolated. Full selected ARGB/confidence output matches across the intentional CPU/GPU selection change. No physical Android execution, native shader proof or device-speed claim is made.")
    (work / "result.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default=str(ROOT))
    parser.add_argument("--work", required=True)
    parser.add_argument("--jdk")
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
