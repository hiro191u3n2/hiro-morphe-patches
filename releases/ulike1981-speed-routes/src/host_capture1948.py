#!/usr/bin/env python3
"""Run production capture-overlap coordinator with isolated scripted Android ABI."""
from pathlib import Path
import argparse, importlib.util, json, os, re, shutil, subprocess

HERE=Path(__file__).resolve().parent
DEFAULT_ROOT=HERE if HERE.name=='src' else HERE.parents[1]/'src'

def test(root,work):
    root=Path(root).resolve();out=Path(work).resolve()/'host-capture1948';classes=out/'classes'
    classes.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('timing1947_fixture',root/'host_capture_save1947.py')
    timing_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(timing_module)
    timing=out/'timing/com/hiro/ulike/ProcessingTiming1947.java'
    timing.parent.mkdir(parents=True,exist_ok=True);timing.write_text(timing_module.TIMING)
    def source(name):
        for p in (root/(name+'.java'),root/'quality-dependencies/com/hiro/ulike'/(name+'.java'),root/'capture-stubs/com/hiro/ulike'/(name+'.java')):
            if p.is_file():return p
        raise RuntimeError('Missing production source '+name)
    inputs=[source(name) for name in ('BurstCapture1933','CapturePolicy1933','YuvPlanes1934','NativeSpeed1935','SpeedWorkers1935')]
    inputs += [timing,root/'tests/Capture1948Test.java',*sorted((root/'tests/capture1948-fixtures').rglob('*.java'))]
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    command=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    built=subprocess.run(command+['-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',str(classes),*map(str,inputs)],capture_output=True,text=True,timeout=120)
    (out/'compile.log').write_text(built.stdout+built.stderr)
    if built.returncode:raise RuntimeError(built.stderr[-12000:])
    run=subprocess.run(['java','-Xmx1g','-cp',str(classes),'com.hiro.ulike.Capture1948Test'],capture_output=True,text=True,timeout=180)
    (out/'test.log').write_text(run.stdout+run.stderr)
    if run.returncode:raise RuntimeError(run.stderr[-12000:])
    match=re.fullmatch(r'CAPTURE_1948_PASS assertions=(\d+) scenarios=(\d+)',run.stdout.strip())
    if not match:raise RuntimeError('Unexpected capture result: '+run.stdout)
    result={'status':'passed','assertions':int(match[1]),'scenarios':int(match[2]),
            'scope':'production BurstCapture coordinator with scripted Android ABI and preprocessing boundary; real pixel equivalence tested separately; no Android device claim',
            'frame_zero_preprocessing_overlaps_camera':True,'each_accepted_frame_and_pair_prepared':True,
            'immutable_bytes_actual_metadata_identity':True,'fuse_and_renderer_wait_all_preparation':True,
            'cancel_waits_actual_preparation_exit_before_job_end':True,'cancel_keeps_gate_until_preparation_exit':True,
            'historical_capture_callback_lifecycle_preserved':True,'cancel_plus_rejected_handler_no_deadlock':True,'pair_preprocessing_overlaps_next_camera_request':True,'reentrant_deferred_handler_failure_no_deadlock':True,'reentrant_full_deferred_queue_failure_no_deadlock':True,'device_tested':False}
    (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,default=DEFAULT_ROOT);parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args();print(json.dumps(test(args.root,args.out)))
