#!/usr/bin/env python3
"""Run the real burst adapter with scripted Android callback/lifecycle boundaries."""
from pathlib import Path
import argparse,json,os,re,shutil,subprocess

def source(root,name):
    for path in (root/(name+'.java'),root/'quality-dependencies/com/hiro/ulike'/(name+'.java'),root/'capture-stubs/com/hiro/ulike'/(name+'.java')):
        if path.is_file():return path
    raise RuntimeError('Missing reviewed production/dependency source: '+name)

def test(root,work):
    root=Path(root);work=Path(work);out=work/'host-capture1943';classes=out/'classes'
    classes.mkdir(parents=True,exist_ok=True)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    command=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    inputs=[source(root,name) for name in ('BurstCapture1933','CapturePolicy1933','YuvPlanes1934','NativeSpeed1935')]
    inputs+=[root/'tests/Capture1943Test.java',*sorted((root/'tests/capture1943-fixtures').rglob('*.java'))]
    built=subprocess.run(command+['-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',str(classes),*map(str,inputs)],capture_output=True,text=True,timeout=120)
    (out/'compile.log').write_text(built.stdout+built.stderr)
    if built.returncode:raise RuntimeError(built.stderr[-12000:])
    run=subprocess.run(['java','-Xmx1g','-cp',str(classes),'com.hiro.ulike.Capture1943Test'],capture_output=True,text=True,timeout=180)
    (out/'test.log').write_text(run.stdout+run.stderr)
    if run.returncode:raise RuntimeError(run.stderr[-12000:])
    match=re.fullmatch(r'CAPTURE_1943_PASS assertions=(\d+) scenarios=(\d+)',run.stdout.strip())
    if not match:raise RuntimeError('Unexpected host camera result: '+run.stdout)
    result={'status':'passed','assertions':int(match[1]),'scenarios':int(match[2]),'scope':'production BurstCapture with scripted camera callbacks; production fusion tested separately; no Android/device claim','shutter_frame_metadata_preserved':True,'changed_exposure_restart_absent':True,'stationary_four_frames_preserved':True}
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,args.out)))
