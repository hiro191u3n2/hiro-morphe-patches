#!/usr/bin/env python3
"""Verify Java Android-API-compiled wrapper on the separately supplied host ORT runtime.

Original inputs, generated ONNX, tensors and vendor weights remain local. This
script writes only a compact report inside this source directory when requested.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile

if not __debug__:
    raise RuntimeError('Verification requires Python assertions; do not run with -O or -OO.')

# Official ORT 1.30.0 process opt-out: set before importing/initializing ORT.
# All Java subprocesses inherit it. disable_telemetry_events() alone is too late
# to suppress a native initialization event.
os.environ['ORT_DISABLE_TELEMETRY']='1'

ROOT=Path(__file__).resolve().parent

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def command(args,env=None):
    completed=subprocess.run([str(x) for x in args],check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,env=env)
    return completed.stdout.strip()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk',type=Path,required=True)
    parser.add_argument('--android-jar',type=Path,required=True)
    parser.add_argument('--ort-android-classes',type=Path,required=True)
    parser.add_argument('--ort-host-jar',type=Path,required=True)
    parser.add_argument('--r8',type=Path,required=True)
    parser.add_argument('--baoman',type=Path,required=True)
    parser.add_argument('--goodlike',type=Path,required=True)
    parser.add_argument('--bytenn',type=Path,required=True)
    parser.add_argument('--effect',type=Path,required=True)
    parser.add_argument('--report',type=Path)
    args=parser.parse_args()
    sys.path.insert(0,str(ROOT.parent/'model_replay'))
    import numpy as np
    import onnxruntime as ort
    ort.disable_telemetry_events()
    from baoman_replay import load_pinned,build_onnx,cpu_session
    from purity_replay import load_pinned_purity
    from verify_real_model import fixtures
    sources=sorted((ROOT/'src').rglob('*.java'))
    tests=sorted((ROOT/'test').rglob('*.java'))
    report={'status':'PASS_ANDROID_SDK36_COMPILE_AND_HOST_JAVA_TENSOR_RUNTIME',
            'source_sha256':{str(p.relative_to(ROOT)):digest(p) for p in sources+tests+[Path(__file__).resolve(),ROOT/'AndroidManifest.xml']},
            'dependencies_sha256':{p.name:digest(p) for p in [args.android_jar,args.ort_android_classes,args.ort_host_jar,args.r8]},
            'onnxruntime_python':ort.__version__,'numpy':np.__version__,
            'android_device_execution':False,'app_hook_integration':False,'complete_style_equivalence':False,
            'hdr_preservation':False,'telemetry_explicitly_disabled_python_and_java':True,
            'ort_disable_telemetry_process_flag_before_initialization':True,'fixtures':[]}
    with tempfile.TemporaryDirectory(prefix='ulike-android-model-qa-') as scratch:
        temporary=Path(scratch);classes=temporary/'classes';classes.mkdir()
        command([args.jdk/'javac','-source','8','-target','8','-bootclasspath',args.android_jar,'-cp',args.ort_android_classes,'-d',classes,*sources])
        command([args.jdk/'javac','--release','8','-cp',os.pathsep.join(map(str,[classes,args.ort_android_classes])),'-d',classes,*tests])
        source_jar=temporary/'module.jar'
        # Build dex only from production classes; host test java.nio.file APIs are excluded.
        production_classes=temporary/'production';production_classes.mkdir()
        command([args.jdk/'javac','-source','8','-target','8','-bootclasspath',args.android_jar,'-cp',args.ort_android_classes,'-d',production_classes,*sources])
        command([args.jdk/'jar','--create','--file',source_jar,'-C',production_classes,'.'])
        dex=temporary/'dex';dex.mkdir()
        command([args.jdk/'java','-cp',args.r8,'com.android.tools.r8.D8','--min-api','26','--lib',args.android_jar,'--classpath',args.ort_android_classes,'--output',dex,source_jar])
        report['d8_min_api']=26;report['d8_sdk36_compile']=True;report['dex_bytes']=(dex/'classes.dex').stat().st_size
        fixture_dir=temporary/'fixtures';fixture_dir.mkdir()
        fixtures_list=list(fixtures((1,3,256,256)))
        for name,tensor in fixtures_list:(fixture_dir/(name+'.bin')).write_bytes(tensor.astype('<f4').tobytes())
        for style,model in [('NATURAL_BLUSH',args.baoman),('PURITY2',args.goodlike)]:
            output=temporary/style
            java_command=[args.jdk/'java','-Xmx384m','-cp',os.pathsep.join(map(str,[classes,args.ort_host_jar])),
                          'hiro.ulike.model.RunTensor',style,model,args.bytenn,args.effect,fixture_dir,output]
            no_flag=dict(os.environ);no_flag.pop('ORT_DISABLE_TELEMETRY',None)
            rejection=command(java_command+['reject-missing-telemetry-flag'],env=no_flag)
            stdout=command(java_command)
            plan,_=(load_pinned(model,args.bytenn) if style=='NATURAL_BLUSH' else load_pinned_purity(model,args.effect,args.bytenn))
            session=cpu_session(build_onnx(plan))
            cases=[]
            for name,tensor in fixtures_list:
                java=np.frombuffer((output/(name+'.bin')).read_bytes(),dtype='<f4').reshape(1,4,256,256)
                expected=session.run([plan.graph.output_name],{plan.graph.input_name:tensor})[0]
                error=float(np.max(np.abs(java.astype(np.float64)-expected.astype(np.float64))))
                assert np.array_equal(java,expected),(style,name,error)
                cases.append({'fixture':name,'values':java.size,'maximum_absolute_error':error,'bit_exact':True,
                              'output_sha256':digest(output/(name+'.bin'))})
            report['fixtures'].append({'style':style,'missing_telemetry_flag_rejection':rejection,'host_wrapper_assertions':stdout,'results':cases})
    report['limitations']=[
        'Java compiler and wrapper compile against Android SDK 36 and ORT Android 1.30.0; execution uses separate host Java ORT 1.30.0 CPU binaries.',
        'No Android device, JNI loading, app integration, capture timing, face alignment, RGB/HDR conversion, output-channel interpretation or compositing is validated here.',
        'Model tensors match the frozen Python reconstruction; original vendor end-to-end numerical equivalence is not claimed.',
        'Native approximate Tanh was intentionally replaced by standard ONNX Tanh; this follows the independently documented model_replay scope.'
    ]
    encoded=json.dumps(report,indent=2)+'\n'
    if args.report:args.report.write_text(encoded)
    else:print(encoded,end='')

if __name__=='__main__':main()
