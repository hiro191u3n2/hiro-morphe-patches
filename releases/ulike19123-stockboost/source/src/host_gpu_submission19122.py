#!/usr/bin/env python3
"""Execute production Strong admission and idle GPU owner cleanup on host.

Scalar policy/generation and JNI transport are controlled fixtures. This does
not claim GLES numerical performance, preview FPS or physical-device recovery.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess,sys

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def method_body(text,marker):
    begin=text.index(marker);opening=text.index('{',begin);depth=1;end=opening+1
    while depth:
        if text[end]=='{':depth+=1
        elif text[end]=='}':depth-=1
        end+=1
    return text[begin:end]

def test(source,work,jdk,android_jar):
    source,work,jdk,android=map(lambda p:Path(p).resolve(),(source,work,jdk,android_jar))
    work.mkdir(parents=True,exist_ok=True);fixture=source/'tests19122/gpu'
    sys.path.insert(0,str(source));import build19101
    production=list(dict.fromkeys(build19101.compile_inputs([]).values()))
    tracked=[*production,*sorted(fixture.glob('*')),source/'host_gpu_submission19122.py']
    pins={str(p.relative_to(source)):sha(p) for p in tracked if p.is_file()}
    commands=[]
    def run(argv,label,expected=None):
        argv=list(map(str,argv));commands.append(dict(label=label,argv=argv))
        p=subprocess.run(argv,capture_output=True,text=True,timeout=120);out=p.stdout+p.stderr;(work/(label+'.log')).write_text(out)
        if expected:
            assert p.returncode and expected in out,label+' mutation escaped: '+out[-6000:]
        else:assert p.returncode==0 and 'WARNING in native method' not in out and 'FATAL ERROR' not in out,label+': '+out[-10000:]
        return out
    classes=work/'production';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*production],'all-production-compile')
    stage=work/'stage';stage.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-cp',os.pathsep.join(map(str,[classes,android])),'-d',stage,fixture/'CapturePolicy19122.java',fixture/'GpuSubmission19122Test.java'],'stage-fixture-compile')
    stagecmd=[jdk/'bin/java','-ea','-cp',os.pathsep.join(map(str,[stage,classes,android])),'com.hiro.ulike.GpuSubmission19122Test']
    stage_result=json.loads(run(stagecmd,'stage-admission').split('RESULT ',1)[1])
    idle=work/'idle';idle.mkdir(exist_ok=True)
    run(['cc','-std=c11','-O2','-shared','-fPIC','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),fixture/'idle_trim19122.c','-o',work/'libidletrim19122.so'],'idle-native-fixture')
    run([jdk/'bin/javac','--release','8','-d',idle,source/'GpuNoise1960.java',source/'GpuNoiseOptimized19119.java',fixture/'IdleTrimPeers19122.java',fixture/'GpuIdleTrim19122Test.java'],'idle-production-compile')
    idlecmd=[jdk/'bin/java','-ea','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',idle,'com.hiro.ulike.GpuIdleTrim19122Test']
    idle_result=json.loads(run(idlecmd,'idle-owner').split('RESULT ',1)[1])
    # Fail closed if this release accidentally edits a command graph. These
    # exact method-byte hashes were taken from public .121 source, separately
    # from the admission code under test. Native archive equality is a build gate.
    command_proof=json.loads((fixture/'baseline_commands19122.json').read_text());body=(source/'GpuStrong1960.java').read_text()
    for marker,digest in command_proof['methods'].items():
        assert hashlib.sha256(method_body(body,marker).encode()).hexdigest()==digest,'Pixel command graph changed: '+marker
    mutants=[
      ('strong-one-bank','GpuStrong1960.java','final int bankLimit19122=cpuOracle1976||benchmark1976?2:Math.max(1,Math.min(2,CapturePolicy19122.gpuBanks()));','final int bankLimit19122=2;','normal stage snapshots one GPU bank','stage'),
      ('strong-gap','GpuStrong1960.java','if(stage.submitGap19122>0)stage.submitAfter19122=System.nanoTime()+stage.submitGap19122;','if(false)stage.submitAfter19122=System.nanoTime()+stage.submitGap19122;','actual submit-side waiting recorded','stage'),
      ('trim-live-session','GpuNoise1960.java','if(activeSession||!leases1971.isEmpty()||!PostSaveCleanup19122.isCurrentIdle19122(generation))','if(!leases1971.isEmpty()||!PostSaveCleanup19122.isCurrentIdle19122(generation))','active session must not be trimmed','idle'),
      ('trim-generation','GpuNoise1960.java','||!PostSaveCleanup19122.isCurrentIdle19122(generation)','', 'new capture generation cancels cleanup','idle')]
    controls=[]
    for name,file,before,after,expected,kind in mutants:
        text=(source/file).read_text();assert text.count(before)==1,'Mutation seam missing '+name
        folder=work/name;folder.mkdir(exist_ok=True);mutant=folder/file;mutant.write_text(text.replace(before,after))
        cp=os.pathsep.join(map(str,[stage,classes,android])) if kind=='stage' else str(idle)
        run([jdk/'bin/javac','--release','8','-cp',cp,'-d',folder,mutant],name+'-compile')
        cmd=list(stagecmd if kind=='stage' else idlecmd);cmd[cmd.index('-cp')+1]=str(folder)+os.pathsep+cp
        run(cmd,name,expected);controls.append(name)
    assert pins=={str(p.relative_to(source)):sha(p) for p in tracked if p.is_file()},'Sources changed while GPU tests executed'
    result=dict(status='passed',assertions=stage_result['assertions']+idle_result['assertions']+len(command_proof['methods']),negative_controls=len(controls),negative_control_names=controls,stage=stage_result,idle=idle_result,command_graph_methods_unchanged=len(command_proof['methods']),source_sha256=pins,production_java_executed=True,host_jni_fixture=True,actual_jni_gles=False,physical_android_tested=False,device_speedup_verified=False,commands=commands)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--android-jar',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.android_jar),indent=2))
