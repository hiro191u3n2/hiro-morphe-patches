#!/usr/bin/env python3
"""Freshly run every released .72 camera/layout host assertion on current sources.

The inherited runner and test fixtures are used unchanged. These SDK doubles
exercise source behavior; no physical Android camera or layout is tested.
"""
import argparse
import importlib.util
import json
from pathlib import Path


EXPECTED_GROUPS = {
    'host_front1972': {
        'FrontTest1936': 202,
        'FrontModeReady1968Test': 55,
        'FrontRetained1971Test': 102,
    },
    'host_layout1972': {
        'bounded_scalar_diagnostic': 178,
        'preserved_layout_policy': 282,
    },
}


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    spec = importlib.util.spec_from_file_location(
        'camera1973_released_runner', source / 'host_camera1972.py')
    if spec is None or spec.loader is None:
        raise RuntimeError('Released .72 camera host runner is unavailable')
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    report = runner.test(source, work / 'released-camera1972', jdk=jdk)
    if report.get('status') != 'passed' or report.get('physical_android_tested') is not False:
        raise RuntimeError('Missing fresh camera execution or incorrect device claim')
    if type(report.get('assertions')) is not int or report['assertions'] != 819:
        raise RuntimeError('Released .72 camera assertion count was not retained')
    if set(report.get('tests', {})) != set(EXPECTED_GROUPS):
        raise RuntimeError('Released .72 camera test inventory was not retained')
    for name, expected in EXPECTED_GROUPS.items():
        actual = report['tests'][name]
        if actual.get('status') != 'passed' or actual.get('physical_android_tested') is not False:
            raise RuntimeError('Incomplete camera host test: ' + name)
        if type(actual.get('assertions')) is not int or actual['assertions'] != sum(expected.values()):
            raise RuntimeError('Released camera subgroup count changed: ' + name)
        expected_groups = {
            group: {'status': 'passed', 'assertions': count}
            for group, count in expected.items()
        }
        if actual.get('groups') != expected_groups:
            raise RuntimeError('Released camera assertions or groups changed: ' + name)
    report = dict(report)
    report['latest_camera_layout_regressions_passed'] = True
    report['fresh_camera_host_execution_in_this_release'] = True
    work.mkdir(parents=True, exist_ok=True)
    (work / 'host-camera1973.json').write_text(
        json.dumps(report, sort_keys=True, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--ndk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, jdk=args.jdk, ndk=args.ndk), indent=2))
