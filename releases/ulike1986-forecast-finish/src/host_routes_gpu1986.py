#!/usr/bin/env python3
"""Retain all native/plan/.85 handoff gates with one optional .86 timing peer.

This keeps the frozen .81 native/plan runner, the exact .83 upload adapter and
the .85 queue runner. Only the already additive native timing fixture gains
strongForecast1986; production source, tests and execution choices stay intact.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json


PINS = {
    'host_routes_gpu1985.py': 'd20c5acf32d02afc1f79acfb380e7911daa30c6500c2317b74dfb4cc3ec89518',
    'tests1982/strong/GpuRouteNativeFixtures1982.java': 'd58c09be164ec1999d86bd2ddb3264ff5e5ac0dda42af45c4d417dcb81f5fdaf',
}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    for name, expected in PINS.items():
        if sha(source / name) != expected:
            raise AssertionError('Frozen native-route adapter input changed: ' + name)
    prior = module('routes86_peers85', source / 'host_routes_gpu1985.py')
    forecast = module('routes86_forecast_peer', source / 'host_strong_diagnostics1986.py')
    original = source / 'host_routes_gpu1981.py'
    old_transport = source / 'tests1981/GpuPlanTransport1981.java'
    transport = work / 'transfer-fixture83/GpuPlanTransport1981.java'
    transport.parent.mkdir(exist_ok=True)
    transport.write_text(prior.adapt_memory_transport1983(old_transport.read_text()))
    old_fixture = source / 'tests1981/GpuRouteNativeFixtures1981.java'
    fixture82 = source / 'tests1982/strong/GpuRouteNativeFixtures1982.java'
    addition82 = '    static synchronized void strongGpuLockWait1982(Trace t,long nanos){if(t!=trace||nanos<0)throw new AssertionError("invalid additive lock timing");}\n'
    marker82 = '    static synchronized void strongGpuWork1973(Trace t,long cpu,long gpu,long wait,int reuse){}\n'
    old_body, body82 = old_fixture.read_text(), fixture82.read_text()
    if old_body.count(marker82) != 1 or old_body.replace(marker82, marker82 + addition82) != body82:
        raise AssertionError('Frozen .82 timing peer changed beyond its declared API')
    fixture86 = work / 'forecast-fixture86/GpuRouteNativeFixtures1982.java'
    fixture86.parent.mkdir(exist_ok=True)
    fixture86.write_text(forecast.forecast_peer1986(body82))
    diagnostic = source / 'PipelineDiagnostics1982.java'
    pins = {str(path): sha(path) for path in (original, old_transport, transport, old_fixture, fixture82,
            fixture86, diagnostic, source / 'host_routes_gpu1985.py', source / 'host_strong_diagnostics1986.py', Path(__file__))}
    retained = module('routes86_retained81', original)
    original_run = retained.run
    substitutions, transfers, additions = [], [], []

    def run(command, directory, label, timeout=240, env=None):
        command = list(command)
        for previous, replacement, required, observed in (
                (old_fixture, fixture86, 'route-compile', substitutions),
                (old_transport, transport, 'fixture-compile', transfers)):
            matches = [i for i, item in enumerate(command) if str(item) == str(previous)]
            if matches:
                if label != required or len(matches) != 1:
                    raise AssertionError('Unexpected additive route fixture substitution: ' + label)
                command[matches[0]] = replacement
                observed.append(label)
        if label == 'production-compile':
            if str(diagnostic) in map(str, command):
                raise AssertionError('Retained closure already has the extra diagnostic helper')
            command.append(diagnostic)
            additions.append(str(diagnostic))
        return original_run(command, directory, label, timeout=timeout, env=env)

    retained.queue_test = module('routes86_retained_tuning85', source / 'host_tuning1985.py').test
    retained.run = run
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if substitutions != ['route-compile'] or transfers != ['fixture-compile'] or additions != [str(diagnostic)]:
        raise AssertionError('Native, plan and production additions did not execute exactly as declared')
    if pins != {name: sha(name) for name in pins}:
        raise AssertionError('Immutable route fixture/runner changed during execution')
    result.update(gpu_routes_forecast_fixture_adapter1986_verified=True,
                  certificate_handoff_adapter1985_verified=True, qualification_progress_adapter1984_verified=True,
                  upload_budget_fixture_adapter1983_verified=True, additive_timing_fixture_adapter1982_verified=True,
                  additive_diagnostic_helper_adapter1982_verified=True,
                  diagnostic_adapter_source_sha256=pins, diagnostic_fixture_substitutions=substitutions,
                  diagnostic_production_source_additions=additions,
                  retained_runner_source_sha256=sha(original), adapter_source_sha256=sha(__file__))
    result['source_sha256'].update(pins)
    result['plan']['source_sha256'][str(diagnostic)] = pins[str(diagnostic)]
    result['plan']['diagnostic_production_source_additions'] = additions
    (work / 'plan/result.json').write_text(json.dumps(result['plan'], ensure_ascii=False, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
