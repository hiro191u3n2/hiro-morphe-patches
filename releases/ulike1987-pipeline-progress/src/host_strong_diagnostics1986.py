#!/usr/bin/env python3
"""Execute the frozen .82 differential with an additive API and isolated cases.

The current dispatcher and qualification service compile unchanged. The old
transport peer gains an optional forecast observer and clears forecast history
at the existing independent-scenario reset boundary. All old assertions and
baseline inputs stay fixed. A seeded unisolated negative control reproduces
the CI profile-choice failure; the same scenario passes when isolated, while
two captures within one case still influence the real production selection.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil


PINS = {
    'host_strong_diagnostics1982.py': '3c8b30ac9504d4f2bccc54e69570593245ad961e30c17499a008a2a9161062d9',
    'tests1982/strong/StrongDiagnosticsFixtures1982.java': '2ce4add71d5177e5296d54a95ad10b75c119b6bb25d5c134ea4561649ffbfb79',
    'tests1982/strong/StrongDiagnostics1982Test.java': '5baf496b3622f009029ffcdd643be356884208e1681eb2533700b79194e3ae08',
}
FORECAST_API1986 = '    static void strongForecast1986(Trace trace,int state,int samples,long age,long certified,long observed,long chosen){}\n'
FORECAST_CASE_RESET1986 = '''    static void resetForecastCase1986(){
        synchronized(GpuStrong1960.Forecast1983.class){
            try {java.lang.reflect.Field recent=GpuStrong1960.Forecast1983.class.getDeclaredField("RECENT");recent.setAccessible(true);((Map<?,?>)recent.get(null)).clear();}
            catch(ReflectiveOperationException failure){throw new AssertionError("forecast scenario isolation failed",failure);}
        }
    }
'''


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def forecast_peer1986(body):
    marker = 'final class ProcessingTiming1947 {\n'
    if body.count(marker) != 1 or 'strongForecast1986(' in body:
        raise AssertionError('Pinned forecast peer declaration changed')
    result = body.replace(marker, marker + FORECAST_API1986)
    if result.replace(FORECAST_API1986, '', 1) != body:
        raise AssertionError('Forecast peer adapter changed more than its optional API')
    return result


def isolate_strong_cases1986(body):
    """Only this diagnostic matrix has independent, identically keyed cases.

    Do not add this reset to the shared API-only adapter: native-route and
    explicit .86 temporal tests must retain their intentional capture history.
    """
    marker = 'final class ProcessingTiming1947 {\n'
    old = '    static void reset(){reasons.clear();'
    new = '    static void reset(){resetForecastCase1986();reasons.clear();'
    if body.count(marker) != 1 or body.count(old) != 1 or 'resetForecastCase1986' in body:
        raise AssertionError('Independent Strong scenario reset boundary changed')
    result = body.replace(marker, marker + FORECAST_CASE_RESET1986).replace(old, new)
    if result.replace(FORECAST_CASE_RESET1986, '', 1).replace(new, old, 1) != body:
        raise AssertionError('Scenario isolation changed more than its exact fixture reset boundary')
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    for name, expected in PINS.items():
        if sha(source / name) != expected:
            raise AssertionError('Frozen Strong diagnostic input changed: ' + name)
    fixture = source / 'tests1982/strong/StrongDiagnosticsFixtures1982.java'
    harness = source / 'tests1986/strong_diagnostics/StrongCaseIsolation1986Test.java'
    adapted = work / 'fixture86/StrongDiagnosticsFixtures1982.java'
    adapted.parent.mkdir(exist_ok=True)
    api_only = forecast_peer1986(fixture.read_text())
    adapted.write_text(isolate_strong_cases1986(api_only))
    unisolated = work / 'unisolated-fixture86/StrongDiagnosticsFixtures1982.java'
    unisolated.parent.mkdir(exist_ok=True)
    unisolated.write_text(api_only)
    inputs = {str(path): sha(path) for path in (source / 'host_strong_diagnostics1982.py',
              source / 'tests1982/strong/StrongDiagnostics1982Test.java', fixture, adapted, unisolated, harness, Path(__file__))}
    retained = module('strong86_retained82', source / 'host_strong_diagnostics1982.py')
    original_run = retained.run
    substitutions = []
    current_compile = []

    def run(command, directory, label):
        command = list(command)
        if label == 'current-compile':
            matches = [i for i, item in enumerate(command) if str(item) == str(fixture)]
            if len(matches) != 1:
                raise AssertionError('Current Strong fixture must be replaced exactly once')
            command[matches[0]] = adapted
            command.append(harness)
            current_compile.append(list(command))
            substitutions.append(label)
        return original_run(command, directory, label)

    retained.run = run
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if substitutions != ['current-compile'] or len(current_compile) != 1:
        raise AssertionError('Strong diagnostic adapter must extend exactly the current compile')
    java_home = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    negative_classes = work / 'unisolated-classes'
    negative_classes.mkdir(exist_ok=True)
    negative_command = list(current_compile[0])
    negative_command[negative_command.index('-d') + 1] = negative_classes
    positions = [i for i, item in enumerate(negative_command) if str(item) == str(adapted)]
    if len(positions) != 1:
        raise AssertionError('Negative control must use the same production compile with only the reset removed')
    negative_command[positions[0]] = unisolated
    original_run(negative_command, work, 'unisolated-compile')
    isolation = {}
    for mode, classes in [('unisolated', negative_classes), ('isolated', work / 'current-classes')]:
        output = original_run([java_home / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-cp', classes,
                               'com.hiro.ulike.StrongCaseIsolation1986Test', mode], work, mode + '-scenario')
        report = json.loads(output.strip().splitlines()[-1])
        if report.get('status') != 'passed' or report.get('assertions', 0) <= 0:
            raise AssertionError('Missing executed scenario-isolation evidence: ' + mode)
        isolation[mode] = report
    if isolation['unisolated']['old_expected_assertion_failed'] is not True or isolation['isolated']['old_expected_assertion_failed'] is not False:
        raise AssertionError('Same old profile assertion must fail only without case isolation')
    if isolation['unisolated']['output_sha256'] != isolation['isolated']['output_sha256']:
        raise AssertionError('The deliberate profile-choice difference changed complete output')
    if not all(report.get('within_case_forecast_preserved') is True for report in isolation.values()):
        raise AssertionError('Scenario isolation must retain intentional history inside each case')
    if inputs != {name: sha(name) for name in inputs}:
        raise AssertionError('Strong fixture, baseline assertions or adapter changed during execution')
    result.update(strong_forecast_fixture_adapter1986_verified=True,
                  strong_diagnostic_history_leak_reproduced1986_verified=True,
                  strong_diagnostic_case_isolation1986_verified=True,
                  strong_diagnostic_within_case_forecast1986_preserved=True,
                  retained_diagnostic_assertions1986=result['assertions'],
                  case_isolation1986=isolation,
                  forecast_fixture_substitutions1986=substitutions,
                  forecast_fixture_adapter_sha256=inputs,
                  retained_runner_source_sha256=PINS['host_strong_diagnostics1982.py'],
                  adapter_source_sha256=sha(__file__))
    result['assertions'] += sum(report['assertions'] for report in isolation.values())
    result['source_sha256'].update(inputs)
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
