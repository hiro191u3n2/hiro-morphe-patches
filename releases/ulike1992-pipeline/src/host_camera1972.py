#!/usr/bin/env python3
"""Execute the repaired camera and layout source against bounded SDK/UI fixtures."""
from pathlib import Path
import argparse
import importlib.util
import json


def test(source, work, jdk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    reports = {}
    for name in ('host_front1972', 'host_layout1972'):
        spec = importlib.util.spec_from_file_location(name + '_camera_build', source / (name + '.py'))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        report = module.test(source, work / name, jdk=jdk)
        if report.get('status') != 'passed' or type(report.get('assertions')) is not int or report['assertions'] <= 0:
            raise RuntimeError('Missing executed camera test evidence: ' + name)
        if report.get('physical_android_tested') is not False:
            raise RuntimeError('Host test must disclose that physical Android is untested: ' + name)
        reports[name] = report
    result = {
        'status': 'passed',
        'assertions': sum(report['assertions'] for report in reports.values()),
        'physical_android_tested': False,
        'tests': reports,
        'scope': 'Actual front recovery, camera scalar diagnostics and layout observation Java sources with controlled Android/SDK boundaries; physical camera output untested.',
    }
    work.mkdir(parents=True, exist_ok=True)
    (work / 'host-camera1972.json').write_text(json.dumps(result, sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent, args.work, args.jdk), indent=2))
