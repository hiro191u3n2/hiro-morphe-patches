#!/usr/bin/env python3
"""Run real YUV packing and exact geometry against independent host oracles."""
from pathlib import Path
import argparse
import json
import subprocess

def test(root, work):
    root, work = Path(root), Path(work)
    destination = work/'host-yuv-geometry1934'
    classes = destination/'classes'
    classes.mkdir(parents=True, exist_ok=True)
    capture = root/'tests/capture-fixtures'
    sources = [root/'YuvPlanes1934.java', root/'FastPixels1933.java', root/'FastResize1933.java',
               root/'tests/reference/QualityPixels1932.java', root/'tests/YuvGeometry1934Test.java',
               capture/'android/media/Image.java', capture/'android/graphics/Rect.java',
               capture/'android/graphics/ImageFormat.java',
               *sorted((root/'tests/fast-fixtures').rglob('*.java'))]
    compile = subprocess.run(['javac','-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=120)
    (destination/'compile.log').write_text(compile.stdout+compile.stderr)
    if compile.returncode: raise RuntimeError(compile.stderr)
    run = subprocess.run(['java','-Xmx768m','-cp',str(classes),'com.hiro.ulike.YuvGeometry1934Test'],capture_output=True,text=True,timeout=180)
    (destination/'run.log').write_text(run.stdout+run.stderr)
    if run.returncode: raise RuntimeError(run.stderr)
    result=json.loads(run.stdout)
    assert result['status']=='passed' and result['assertions']>0
    (destination/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parent)
    parser.add_argument('--work',type=Path,required=True)
    args=parser.parse_args()
    print(json.dumps(test(args.source,args.work),indent=2))
