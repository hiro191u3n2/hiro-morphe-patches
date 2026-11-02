#!/usr/bin/env python3
"""Execute first-frame capture regressions and preserved noise/save/feedback behavior."""
from pathlib import Path
import importlib.util
import json

SUITES = [('capture_timing', 'host_timing1943.py'),
          ('noise_preservation', 'host_noise1942.py'),
          ('save_pipeline', 'host_pipeline1942.py'),
          ('shutter_feedback', 'host_feedback1941.py')]


def test(root, work, android):
    root, work, android = Path(root), Path(work), Path(android)
    work.mkdir(parents=True, exist_ok=True)
    results = {}
    for name, filename in SUITES:
        spec = importlib.util.spec_from_file_location('ulike1943_' + name, root / filename)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        result = module.test(root, work) if name in ('capture_timing', 'noise_preservation') else module.test(root, work, android)
        if not isinstance(result, dict) or result.get('status') != 'passed' or type(result.get('assertions')) is not int or result['assertions'] <= 0:
            raise RuntimeError('Missing executed passing assertions: ' + filename)
        json.dumps(result, allow_nan=False)
        results[name] = result
        (work / ('host-regression-' + name + '-result.json')).write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    return {'status': 'passed', 'assertions': sum(x['assertions'] for x in results.values()), 'suites': results,
            'device_tested': False, 'performance_measured_on_device': False}
