#!/usr/bin/env python3
"""Pin the v1.9.34 resizer and compare full outputs/ownership with the speed path."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess


def test(root, work):
    root, work = Path(root), Path(work)
    destination = work/'host-resize1935'
    classes = destination/'classes'
    classes.mkdir(parents=True, exist_ok=True)
    references = {
        'FastPixels1934Reference.java': 'c9a836e4faf03736eecccb3bc9199075a21c0f6bc770e7383a1370598a076397',
        'FastResize1934Reference.java': '63f8985f15fe5ca9a3eae6d6d49ff5e731288d5d2c30da03bcda09a3914e72cc',
        'QualityPixels1932.java': 'd67433da7946fbafb83a6e1736c1860b9cc967f4f8afe7f104f7351c2adf107d',
    }
    for name, expected in references.items():
        if hashlib.sha256((root/'tests/reference'/name).read_bytes()).hexdigest() != expected:
            raise RuntimeError('Pinned resize reference changed: '+name)
    sources = [root/name for name in ['FastPixels1933.java', 'FastResize1933.java',
                                     'SpeedWorkers1935.java', 'NativeSpeed1935.java']]
    sources += [root/'tests/reference'/name for name in references]
    sources += [root/'tests/ResizeSpeed1935Test.java', *sorted((root/'tests/fast-fixtures').rglob('*.java'))]
    compile_result = subprocess.run(['javac', '-source', '8', '-target', '8', '-Xlint:-options', '-d', str(classes),
                                     *map(str, sources)], capture_output=True, text=True, timeout=120)
    (destination/'compile.log').write_text(compile_result.stdout+compile_result.stderr)
    if compile_result.returncode:
        raise RuntimeError('Resize host compile failed: '+compile_result.stderr[-4000:])
    native_dir = os.environ.get('ULIKE_NATIVE_TEST_DIR')
    java = ['java', '-XX:ActiveProcessorCount=4', '-Xmx768m']
    if native_dir:
        java.append('-Djava.library.path='+str(Path(native_dir).resolve()))
    run = subprocess.run([*java, '-cp', str(classes), 'com.hiro.ulike.ResizeSpeed1935Test'],
                         capture_output=True, text=True, timeout=240)
    (destination/'run.log').write_text(run.stdout+run.stderr)
    if run.returncode:
        raise RuntimeError('Resize host tests failed: '+run.stderr[-4000:])
    result = json.loads(run.stdout)
    if result.get('status') != 'passed' or not result.get('bit_identical_to_1934'):
        raise RuntimeError('Missing full pixel comparison')
    if native_dir and not result.get('native_enabled'):
        raise RuntimeError('Requested native comparison did not load kernel')
    result['reference_sha256'] = references
    (destination/'result.json').write_text(json.dumps(result, indent=2)+'\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work), indent=2))
