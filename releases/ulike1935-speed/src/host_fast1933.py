#!/usr/bin/env python3
"""Compare production fast paths to the exact 1.9.32 pixel reference on the host."""
from pathlib import Path
import argparse
import hashlib
import json
import subprocess


def test(root, work):
    root, work = Path(root), Path(work)
    generated = work / 'host-fast1933'
    classes = generated / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    reference = root / 'tests/reference/QualityPixels1932.java'
    expected = 'd67433da7946fbafb83a6e1736c1860b9cc967f4f8afe7f104f7351c2adf107d'
    if hashlib.sha256(reference.read_bytes()).hexdigest() != expected:
        raise RuntimeError('Original 1.9.32 pixel reference changed')
    sources = [root/'SpeedWorkers1935.java', root/'NativeSpeed1935.java', root/'FastPixels1933.java', root/'FastResize1933.java', reference,
               root/'tests/Fast1933Test.java', *sorted((root/'tests/fast-fixtures').rglob('*.java'))]
    compile_result = subprocess.run(['javac','-source','8','-target','8','-Xlint:-options','-d',str(classes),
                                     *map(str,sources)], capture_output=True, text=True, timeout=120)
    (generated/'compile.log').write_text(compile_result.stdout+compile_result.stderr)
    if compile_result.returncode:
        raise RuntimeError('Fast host compilation failed: '+compile_result.stderr[-4000:])
    run = subprocess.run(['java','-XX:ActiveProcessorCount=4','-Xmx768m','-cp',str(classes),
                          'com.hiro.ulike.Fast1933Test'], capture_output=True, text=True, timeout=180)
    (generated/'run.log').write_text(run.stdout+run.stderr)
    if run.returncode:
        raise RuntimeError('Fast host tests failed: '+run.stderr[-4000:])
    result=json.loads(run.stdout)
    if result.get('status')!='passed' or result.get('assertions',0)<=0:
        raise RuntimeError('Missing fast path assertions')
    result['reference_sha256']=expected
    (generated/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parent)
    parser.add_argument('--work',type=Path,required=True)
    args=parser.parse_args()
    print(json.dumps(test(args.source,args.work),indent=2))
