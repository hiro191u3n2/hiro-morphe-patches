#!/usr/bin/env python3
"""Compare shared immutable shot analysis with the frozen published 1.9.34 kernel."""
from pathlib import Path
import argparse
import hashlib
import json
import subprocess

FROZEN_SHA256 = 'c9e31be95946bef01417d46ecdae99e4dbb038f3eeda9fd665fdf63aa62aef1a'

def test(root, work):
    root, work = Path(root), Path(work)
    folder = work / 'host-s2-1935'
    classes = folder / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    frozen = root / 'tests/reference1934/FusionPixels1934Reference.java'
    original = frozen.read_text().replace('FusionPixels1934Reference', 'FusionPixels1933')
    if hashlib.sha256(original.encode()).hexdigest() != FROZEN_SHA256:
        raise RuntimeError('Frozen 1.9.34 fusion source changed')
    sources = [root / 'FusionPixels1933.java', frozen, root / 'tests/S2Analysis1935Test.java']
    if (root / 'SpeedWorkers1935.java').is_file():
        sources.append(root / 'SpeedWorkers1935.java')
    c = subprocess.run(['javac', '-source', '8', '-target', '8', '-Xlint:-options',
                        '-d', str(classes), *map(str, sources)], capture_output=True, text=True, timeout=120)
    (folder / 'compile.log').write_text(c.stdout + c.stderr)
    if c.returncode:
        raise RuntimeError('S2 compile failed: ' + c.stderr[-4000:])
    r = subprocess.run(['java', '-XX:ActiveProcessorCount=4', '-Xmx512m', '-cp', str(classes),
                        'com.hiro.ulike.S2Analysis1935Test'], capture_output=True, text=True, timeout=300)
    (folder / 'run.log').write_text(r.stdout + r.stderr)
    if r.returncode:
        raise RuntimeError('S2 test failed: ' + r.stderr[-4000:])
    result = json.loads(r.stdout)
    if result.get('status') != 'passed' or sum(result['scenarios'].values()) != result['assertions']:
        raise RuntimeError('S2 evidence inconsistent')
    result['reference_version'] = '1.9.34'
    result['reference_source_sha256'] = FROZEN_SHA256
    result['physical_device_tested'] = False
    (folder / 'result.json').write_text(json.dumps(result, indent=2, allow_nan=False) + '\n')
    return result

if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    p.add_argument('--work', type=Path, required=True)
    a = p.parse_args()
    print(json.dumps(test(a.source, a.work), indent=2, allow_nan=False))
