#!/usr/bin/env python3
"""Deterministic host checks of burst capture policy, pixel ownership and callbacks.

Android camera/looper objects are scripted fixtures. Pixel fusion has its own
independent production-kernel test; this runner tests the real capture adapter.
"""
from pathlib import Path
import json
import re
import subprocess


def test(root, work):
    root, work = Path(root), Path(work)
    src = root if (root / 'BurstCapture1933.java').exists() else root / 'src'
    work.mkdir(parents=True, exist_ok=True)
    classes = work / 'capture-classes'
    classes.mkdir(exist_ok=True)
    fixtures = list((src / 'tests' / 'capture-fixtures').rglob('*.java'))
    inputs = [src / 'BurstCapture1933.java', src / 'CapturePolicy1933.java', src / 'YuvPlanes1934.java', src / 'NativeSpeed1935.java',
              src / 'tests' / 'Capture1933Test.java', *fixtures]
    subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(classes),
                    *map(str, inputs)], check=True, capture_output=True, text=True)
    run = subprocess.run(['java', '-Xmx1g', '-cp', str(classes),
                          'com.hiro.ulike.Capture1933Test'],
                         check=True, capture_output=True, text=True, timeout=180)
    line = run.stdout.strip()
    match = re.fullmatch(r'CAPTURE_1933_PASS assertions=(\d+) scenarios=(\d+)', line)
    if not match:
        raise RuntimeError('unexpected capture host result: ' + line)
    result = {'status': 'passed', 'assertions': int(match[1]),
              'scenarios': int(match[2]),
              'scope': 'capture policy, YUV ownership and scripted Android callback order; no device claim'}
    (work / 'host-capture1933.json').write_text(json.dumps(result, sort_keys=True, indent=2) + '\n')
    (work / 'host-capture1933.txt').write_text(line + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    print(json.dumps(test(Path(__file__).parent, args.out), sort_keys=True))
