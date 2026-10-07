#!/usr/bin/env python3
"""Exercise real face-region policy and geometry; no claim of Android detector accuracy."""
from pathlib import Path
import argparse,json,subprocess

def test(root,work):
    root,work=Path(root),Path(work)
    out=work/'host-faces1934';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
    sources=[root/'QualityPixels1932.java',root/'SpatialNoise1934.java',root/'LongMoire1934.java',root/'FaceRegions1934.java',root/'FaceRegions1934Pixels.java',root/'tests/FaceRegions1934Test.java']
    sources += sorted((root/'tests/face-fixtures').rglob('*.java'))
    cp=str(classes)
    built=subprocess.run(['javac','-source','8','-target','8','-Xlint:-options','-cp',cp,'-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=120)
    (out/'compile.log').write_text(built.stdout+built.stderr)
    if built.returncode:raise RuntimeError(built.stderr[-4000:])
    run=subprocess.run(['java','-cp',str(classes)+':'+cp,'com.hiro.ulike.FaceRegions1934Test'],capture_output=True,text=True,timeout=120)
    (out/'run.log').write_text(run.stdout+run.stderr)
    if run.returncode:raise RuntimeError(run.stderr[-4000:])
    result=json.loads(run.stdout);assert result['status']=='passed' and result['assertions']>0
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parent);parser.add_argument('--work',type=Path,required=True);args=parser.parse_args()
    print(json.dumps(test(args.source,args.work),indent=2))
