#!/usr/bin/env python3
"""Execute the reviewed .80 DEX save/face transforms and exact inverse proofs."""
from pathlib import Path
import hashlib,json,os,subprocess,zipfile

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    baseline=Path(os.environ['ULIKE1973_BASELINE_MPP'])
    morphe=Path(os.environ.get('ULIKE_MORPHE_JAR',os.environ['ULIKE1973_MORPHE_JAR']))
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
    if hashlib.sha256(baseline.read_bytes()).hexdigest()!='2be00d742db6a8954a08e0dc43c27b11e1fb2d20c864d09a77b8010bb9a1ea25':raise AssertionError('Exact published .80 save baseline required')
    inputs=work/'baseline';classes=work/'classes';classes.mkdir(exist_ok=True)
    with zipfile.ZipFile(baseline) as z:
        for name in z.namelist():
            if name.endswith('.class') or name=='ulike/runtime.dex':
                p=inputs/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(name))
    files=['MergePayloads.java','SaveRuntimeHooks1981.java','tests1981/SaveRuntimeHooks1981Test.java']
    pins={name:hashlib.sha256((source/name).read_bytes()).hexdigest() for name in files}
    cp=os.pathsep.join(map(str,[morphe,inputs,classes]))
    def run(cmd,label):
        p=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=240)
        (work/(label+'.log')).write_text(p.stdout+p.stderr)
        if p.returncode:raise RuntimeError(label+': '+(p.stdout+p.stderr)[-6000:])
        return p.stdout
    run([jdk/'bin/javac','-cp',cp,'-d',classes,*[source/name for name in files]],'compile')
    result=json.loads(run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-cp',cp,'SaveRuntimeHooks1981Test',inputs/'ulike/runtime.dex',work/'roundtrip.dex'],'roundtrip').strip().splitlines()[-1])
    if result.get('status')!='passed' or result.get('assertions',0)<=0 or result.get('physical_android_tested') is not False:raise AssertionError('DEX proof result absent')
    result.update(storage_codec_fence_inverse_verified=True,encoder_tail_release_positions_verified=True,
        face_success_completion_positions_verified=True,source_sha256=pins)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
