#!/usr/bin/env python3
"""Execute the frozen .82 Strong diagnostic differential with one .86 peer API.

The current dispatcher and qualification service compile unchanged. The old
transport peer gains only an optional forecast observer declaration; baseline
inputs, route/output comparisons, assertions and execution commands stay fixed.
Actual observer behavior and failures have separate .86 routing/timing tests.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json


PINS = {
    'host_strong_diagnostics1982.py': '3c8b30ac9504d4f2bccc54e69570593245ad961e30c17499a008a2a9161062d9',
    'tests1982/strong/StrongDiagnosticsFixtures1982.java': '2ce4add71d5177e5296d54a95ad10b75c119b6bb25d5c134ea4561649ffbfb79',
}
FORECAST_API1986 = '    static void strongForecast1986(Trace trace,int state,int samples,long age,long certified,long observed,long chosen){}\n'


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


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    for name, expected in PINS.items():
        if sha(source / name) != expected:
            raise AssertionError('Frozen Strong diagnostic input changed: ' + name)
    fixture = source / 'tests1982/strong/StrongDiagnosticsFixtures1982.java'
    adapted = work / 'fixture86/StrongDiagnosticsFixtures1982.java'
    adapted.parent.mkdir(exist_ok=True)
    adapted.write_text(forecast_peer1986(fixture.read_text()))
    inputs = {str(path): sha(path) for path in (source / 'host_strong_diagnostics1982.py', fixture, adapted, Path(__file__))}
    retained = module('strong86_retained82', source / 'host_strong_diagnostics1982.py')
    original_run = retained.run
    substitutions = []

    def run(command, directory, label):
        command = list(command)
        if label == 'current-compile':
            matches = [i for i, item in enumerate(command) if str(item) == str(fixture)]
            if len(matches) != 1:
                raise AssertionError('Current Strong fixture must be replaced exactly once')
            command[matches[0]] = adapted
            substitutions.append(label)
        return original_run(command, directory, label)

    retained.run = run
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if substitutions != ['current-compile'] or inputs != {name: sha(name) for name in inputs}:
        raise AssertionError('Strong diagnostic adapter did not remain a single immutable current-peer extension')
    result.update(strong_forecast_fixture_adapter1986_verified=True,
                  forecast_fixture_substitutions1986=substitutions,
                  forecast_fixture_adapter_sha256=inputs,
                  retained_runner_source_sha256=PINS['host_strong_diagnostics1982.py'],
                  adapter_source_sha256=sha(__file__))
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
