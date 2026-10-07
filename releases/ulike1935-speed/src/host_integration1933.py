#!/usr/bin/env python3
"""Join production capture policy/adapter and real pixel fusion on scripted Android boundaries."""
from pathlib import Path
import argparse
import json
import subprocess


def test(root, work):
    root, work = Path(root), Path(work)
    generated = work / 'host-integration1933'
    classes = generated / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    # CaptureYuv is the dedicated adversarial replay fixture and depends on that
    # suite's owner types. These direct callback/CPU-boundary cases need neither
    # it nor the scripted Fusion stand-in. Never compile the stand-in here.
    excluded = {'FusionPixels1933.java', 'CaptureYuv.java'}
    fixtures = [p for p in sorted((root / 'tests/capture-fixtures').rglob('*.java'))
                if p.name not in excluded]
    sources = [root / 'BurstCapture1933.java', root / 'CapturePolicy1933.java', root / 'YuvPlanes1934.java', root / 'NativeSpeed1935.java',
               root / 'SpeedWorkers1935.java', root / 'FusionPixels1933.java', root / 'tests/FusionPixels1933Test.java',
               root / 'tests/FusionCaptureBoundary1933Test.java', *fixtures]
    compiled = subprocess.run(
        ['javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-Xlint:-options',
         '-d', str(classes), *map(str, sources)], capture_output=True, text=True, timeout=120)
    (generated / 'compile.log').write_text(compiled.stdout + compiled.stderr)
    if compiled.returncode:
        raise RuntimeError('Real fusion/capture boundary compilation failed: ' + compiled.stderr[-4000:])
    executed = subprocess.run(
        ['java', '-XX:ActiveProcessorCount=4', '-Xmx768m', '-cp', str(classes),
         'com.hiro.ulike.FusionCaptureBoundary1933Test'],
        capture_output=True, text=True, timeout=180)
    (generated / 'run.log').write_text(executed.stdout + executed.stderr)
    if executed.returncode:
        raise RuntimeError('Real fusion/capture boundary assertions failed: ' + executed.stderr[-4000:])
    result = json.loads(executed.stdout)
    if (result.get('status') != 'passed' or type(result.get('assertions')) is not int
            or result['assertions'] <= 0 or len(result.get('scenarios', {})) != 3
            or sum(result['scenarios'].values()) != result['assertions']):
        raise RuntimeError('Missing executed integration assertions')
    (generated / 'result.json').write_text(json.dumps(result, indent=2, allow_nan=False) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work), indent=2, allow_nan=False))
