#!/usr/bin/env python3
"""Image-domain camera-moment regression, using production fusion and CPU workers."""
from pathlib import Path
import argparse,json,os,shutil,subprocess

def test(root,work,baseline_root=None):
    root=Path(root);work=Path(work);out=work/'host-shutter-moment1943';classes=out/'classes'
    classes.mkdir(parents=True,exist_ok=True)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    command=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    if baseline_root:
        fusion=Path(baseline_root)/'quality-dependencies/com/hiro/ulike/FusionPixels1933.java'
    else:
        fusion=root/'FusionPixels1933.java'
        if not fusion.is_file():fusion=root/'quality-dependencies/com/hiro/ulike/FusionPixels1933.java'
    built=subprocess.run(command+['-source','8','-target','8','-Xlint:-options','-d',str(classes),str(fusion),str(root/'SpeedWorkers1935.java'),str(root/'tests/ShutterMoment1943Test.java')],capture_output=True,text=True,timeout=120)
    (out/'compile.log').write_text(built.stdout+built.stderr)
    if built.returncode:raise RuntimeError(built.stderr[-8000:])
    arguments=['baseline'] if baseline_root else []
    run=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.ShutterMoment1943Test',*arguments],capture_output=True,text=True,timeout=120)
    (out/'test.log').write_text(run.stdout+run.stderr)
    if run.returncode:raise RuntimeError(run.stderr[-8000:])
    result=json.loads(run.stdout);(out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True);parser.add_argument('--baseline-root',type=Path)
    args=parser.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,args.out,args.baseline_root)))
