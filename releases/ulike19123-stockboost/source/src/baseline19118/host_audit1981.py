#!/usr/bin/env python3
"""Execute the reviewed .81 route contract against current production sources.

Each requirement names an actual evidence group. Current admission, progress and fallbacks are verified on production; unchanged numerical
evidence remains inherited and is never added to the current assertion total.
"""
from pathlib import Path
import argparse
import importlib.util
import inspect
import json

ROOT = Path(__file__).resolve().parent
CONTRACT = json.loads((ROOT / 'qa_contract1981.json').read_text())
REQUIRED_FLAGS = tuple(sorted({flag for flags in CONTRACT['requirements'].values() for flag in flags}))


def load(name):
    path = ROOT / (name + '.py')
    spec = importlib.util.spec_from_file_location(name.replace('/', '_') + '_aggregate1979', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    reports = {}
    for label, name in CONTRACT['runners']:
        runner = load(name)
        parameters = inspect.signature(runner.test).parameters
        arguments = {key: value for key, value in (('jdk', jdk), ('ndk', ndk)) if key in parameters}
        print('RUN ' + label + ': ' + name, flush=True)
        report = runner.test(source, work / label, **arguments)
        if (not isinstance(report, dict) or report.get('status') != 'passed'
                or type(report.get('assertions')) is not int or report['assertions'] <= 0):
            raise AssertionError('Fresh executed positive report absent: ' + label)
        if report.get('physical_android_tested') is not False:
            raise AssertionError('Physical Android untested disclosure absent: ' + label)
        reports[label] = report
        (work / 'completed-groups.json').write_text(json.dumps(reports, ensure_ascii=False, indent=2) + '\n')
        print('PASS ' + label + ': ' + str(report['assertions']), flush=True)
    for label, flags in CONTRACT['requirements'].items():
        parts = label.split('.')
        evidence = reports[parts[0]]
        for part in parts[1:]:
            evidence = evidence.get(part, {})
        if evidence.get('status') != 'passed' or evidence.get('physical_android_tested') is not False:
            raise AssertionError('Missing current executed evidence: ' + label)
        for flag in flags:
            if evidence.get(flag) is not True:
                raise AssertionError('Fresh regression absent: ' + label + '/' + flag)
    recovery = reports['recovery']
    schemas = {
        'qualification_proof_schema1977': 'gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1',
        'qualification_preferred_schema1977': 'gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1-strong-exact2-preferred-v1',
        'exact_schema1977': 'gx1977-exact-per-key-v1',
    }
    if any(recovery.get(key) != value for key, value in schemas.items()):
        raise AssertionError('Executed rejection/certificate schema differs from the release declaration')
    result = {
        'status': 'passed', 'assertions': sum(report['assertions'] for report in reports.values()),
        'tests': reports, 'physical_android_tested': False,
        'device_speedup_verified': False, 'gpu_execution_on_physical_android': False,
        'samsung_gpu_failure_root_cause_confirmed': False,
        'fresh_gpu_execution_in_this_release': True,
        'superseded_expectations': CONTRACT['superseded_expectations'],
        **schemas, **{flag: True for flag in REQUIRED_FLAGS},
    }
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--ndk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
