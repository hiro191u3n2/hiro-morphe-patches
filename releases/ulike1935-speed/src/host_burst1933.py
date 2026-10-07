#!/usr/bin/env python3
"""Run real fusion, capture-control, metadata, fast-path and integration tests."""
from pathlib import Path
import importlib.util
import json

SUITES = (('fusion', 'host_fusion1933.py'),
          ('capture', 'host_capture1933.py'),
          ('metadata', 'host_metadata1933.py'),
          ('fast', 'host_fast1933.py'),
          ('integration', 'host_integration1933.py'))


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def test(root, work):
    root, work = Path(root), Path(work)
    results = {}
    for name, filename in SUITES:
        path = root / filename
        require(path.is_file(), 'Required real host suite is absent: ' + filename)
        spec = importlib.util.spec_from_file_location('ulike1933_host_' + name, path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        require(callable(getattr(module, 'test', None)), 'Missing test(root, work): ' + filename)
        result = module.test(root, work)
        require(isinstance(result, dict) and result.get('status') == 'passed',
                'Host suite did not report a successful real run: ' + filename)
        require(type(result.get('assertions')) is int and result['assertions'] > 0,
                'Host suite contains no positive assertion count: ' + filename)
        # Fail on NaN/Infinity and non-serializable placeholders. A runner must
        # return deterministic evidence; elapsed-time measurements are separate.
        json.dumps(result, allow_nan=False)
        results[name] = result
        (work / ('host-burst-' + name + '-result.json')).write_text(
            json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    return {'status': 'passed',
            'assertions': sum(result['assertions'] for result in results.values()),
            'suites': results,
            'device_tested': False,
            'performance_measured_on_device': False}


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--work', required=True, type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work), ensure_ascii=False, indent=2))
