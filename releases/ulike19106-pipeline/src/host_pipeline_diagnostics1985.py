#!/usr/bin/env python3
"""Run the unchanged .84 backend/inverse suite with additive .85 peer APIs.

The frozen wrapper still executes all 18 backends, the .81/.83 negative
controls, 430 mathematical/source preservation checks and the .84 Resident
ticket tests. Its current Resident compilation additionally executes the .85
epoch handoff, explicit begin and exact saved-copy-decision tests.
"""
from pathlib import Path
import argparse
import importlib.util
import json
import os
import shutil
import sys


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    sys.path.insert(0, str(source))
    adapter = module('pipeline85_snapshot_adapter', source / 'host_resident_diagnostics1985.py')
    for name, expected in adapter.RETAINED84.items():
        if adapter.sha(source / name) != expected:
            raise AssertionError('Historical .84 runner changed: ' + name)
    retained = module('pipeline85_retained84', source / 'host_pipeline_diagnostics1984.py')
    original_module = retained.module
    observed = {'peer': 0, 'current_test': 0}
    test_path = source / 'tests1985/resident_snapshot/ResidentSnapshot1985Test.java'
    production = {path.name: adapter.sha(path) for path in source.glob('*.java')}

    def adapted_module(name, path):
        value = original_module(name, path)
        if Path(path).resolve() == source / 'host_resident_diagnostics1984.py':
            adapter.install_peer_adapter1985(value)
            observed['peer'] += 1
        elif Path(path).resolve() == source / 'host_pipeline_diagnostics1982.py':
            original_run = value.run

            def additional_current_test(command, log, timeout=180):
                selected = list(command)
                if Path(log).name == 'current-resident-compile.log':
                    if not any('javac' in str(argument) for argument in command) or str(source / 'GpuResident1976.java') not in map(str, command):
                        raise AssertionError('Snapshot test injection must compile the actual current Resident producer')
                    selected.append(test_path)
                    observed['current_test'] += 1
                return original_run(selected, log, timeout)

            value.run = additional_current_test
        return value

    retained.module = adapted_module
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if observed != {'peer': 1, 'current_test': 1}:
        raise AssertionError('The .85 API adapter and current Resident tests must each be compiled exactly once')
    current = adapter.run([jdk / 'bin/java', '-ea', '-cp', work / 'current-resident',
        'com.hiro.ulike.ResidentSnapshot1985Test', 'current85'], work / 'current-resident-snapshot85.log')
    for name in ('resident_snapshot_epoch_handoff1985', 'resident_snapshot_begin_gate1985',
                 'resident_snapshot_diagnostics_noninterference1985'):
        if current.get(name) is not True:
            raise AssertionError('Current .85 Resident pipeline gate did not execute: ' + name)
    for name in ('pipeline_retained_backend_suite1983', 'pipeline_reviewed_capacity_inverse1983',
                 'pipeline_reviewed_resident_diagnostic_inverse1984', 'pipeline_resident_ticket_contract1984',
                 'pipeline_published83_unreserved_copy_negative1984', 'pipeline_math_and_gates_preserved1982'):
        if result.get(name) is not True:
            raise AssertionError('A retained pipeline gate did not execute: ' + name)
    if production != {path.name: adapter.sha(path) for path in source.glob('*.java')}:
        raise AssertionError('Production sources changed during the full .85 pipeline run')
    if any(adapter.sha(source / name) != expected for name, expected in adapter.RETAINED84.items()):
        raise AssertionError('Historical .84 wrappers changed during execution')
    result['assertions'] += current['assertions']
    result['cases']['current_resident_snapshot85'] = current
    result.update(pipeline_resident_snapshot_peer_adapter1985=True,
                  pipeline_resident_epoch_handoff1985=True,
                  pipeline_resident_begin_gate1985=True,
                  pipeline_resident_copy_diagnostics1985=True,
                  pipeline_retained_full_backend_suite1985=True,
                  retained84_runner_sha256=adapter.RETAINED84,
                  snapshot_test_source_sha256=adapter.sha(test_path),
                  snapshot_adapter_source_sha256=adapter.sha(source / 'host_resident_diagnostics1985.py'),
                  runner_source_sha256=adapter.sha(__file__))
    result['fixture_adapter1985'] = (
        'Only additive reserve/begin/copy-diagnostic APIs are added to the controlled Resident peer copies. '
        'All prior tests and both exact inverse functions remain unchanged, and the current production closure executes. '
        'The separate .85 Resident runner also reproduces the published .84 stale unstarted-owner negative control.'
    )
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
