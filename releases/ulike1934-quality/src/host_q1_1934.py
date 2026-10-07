#!/usr/bin/env python3
"""Execute Q1 sharp-reference quality, identity and fallback regressions."""
from pathlib import Path
import argparse
import json
import subprocess


def test(root, work):
    root, work = Path(root), Path(work)
    generated = work / 'host-q1-1934'
    classes = generated / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    sources = [root / 'FusionPixels1933.java', root / 'tests/Q1Reference1934Test.java']
    compiled = subprocess.run(['javac', '-source', '8', '-target', '8', '-Xlint:-options',
                               '-d', str(classes), *map(str, sources)],
                              capture_output=True, text=True, timeout=120)
    (generated / 'compile.log').write_text(compiled.stdout + compiled.stderr)
    if compiled.returncode:
        raise RuntimeError('Q1 compilation failed: ' + compiled.stderr[-4000:])
    executed = subprocess.run(['java', '-XX:ActiveProcessorCount=4', '-Xmx512m', '-cp',
                               str(classes), 'com.hiro.ulike.Q1Reference1934Test'],
                              capture_output=True, text=True, timeout=180)
    (generated / 'run.log').write_text(executed.stdout + executed.stderr)
    if executed.returncode:
        raise RuntimeError('Q1 regression failed: ' + executed.stderr[-4000:])
    result = json.loads(executed.stdout)
    if result.get('status') != 'passed' or sum(result['scenarios'].values()) != result['assertions']:
        raise RuntimeError('Q1 regression evidence inconsistent')
    (generated / 'result.json').write_text(json.dumps(result, indent=2, allow_nan=False) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work), indent=2, allow_nan=False))
