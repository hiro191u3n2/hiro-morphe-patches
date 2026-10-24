#!/usr/bin/env python3
"""Execute the actual new pixels, final-save orchestration and feedback policies."""
from pathlib import Path
import importlib.util
import json

SUITES = [('noise_quality', 'host_noise1941.py'), ('save_pipeline', 'host_pipeline1941.py'), ('shutter_feedback', 'host_feedback1941.py')]

def test(root, work, android):
    root, work, android = Path(root), Path(work), Path(android)
    results = {}
    for name, filename in SUITES:
        path = root / filename
        spec = importlib.util.spec_from_file_location('ulike1941_' + name, path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        result = module.test(root, work) if name == 'noise_quality' else module.test(root, work, android)
        if not isinstance(result, dict) or result.get('status') != 'passed' or type(result.get('assertions')) is not int or result['assertions'] <= 0:
            raise RuntimeError('Missing executed passing assertions: ' + filename)
        json.dumps(result, allow_nan=False)
        results[name] = result
        (work / ('host-regression-' + name + '-result.json')).write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    return {'status': 'passed', 'assertions': sum(x['assertions'] for x in results.values()), 'suites': results,
            'device_tested': False, 'performance_measured_on_device': False}
