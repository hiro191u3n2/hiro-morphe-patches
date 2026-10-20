#!/usr/bin/env python3
"""Real model JNI overlap and failure ownership; CPU arithmetic is untouched."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess, sys
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('parallel96_base',source/'host_model_math1990.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
    retained=base.test(source,work/'retained',jdk,ndk)
    sys.path.insert(0,str(source));import build1990
    sources=list(dict.fromkeys(build1990.compile_inputs([]).values()))
    fixtures=[source/'tests1996/model/ModelParallel1996Test.java',source/'tests1996/model/ModelParallelFault1996.java']
    tracked=sources+fixtures+[source/'host_model_parallel1996.py'];pins={str(p.relative_to(source)):sha(p) for p in tracked}
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar')))
    cp=str(work/'retained/classes')+os.pathsep+str(android);env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');commands=[]
    def run(args,name):
        args=list(map(str,args));commands.append(dict(name=name,argv=args));done=subprocess.run(args,capture_output=True,text=True,env=env,timeout=300);output=done.stdout+done.stderr;(work/(name+'.log')).write_text(output)
        if done.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+'\n'+output[-14000:])
        return output
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',work/'retained/classes',*fixtures],'compile-normal')
    java=[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(work/'retained')+os.pathsep+str(work/'retained/cpu-native')]
    normal=json.loads(run(java+['-cp',cp,'com.hiro.ulike.ModelParallel1996Test'],'real-parallel').split('RESULT ',1)[1])
    body=(source/'GpuAnalysis1961.java').read_text()
    replacements={
      'GpuNoise1960.Ticket ticket=session.submit(b,0);':'GpuNoise1960.Ticket ticket=ModelParallelFault1996.submit(session.submit(b,0));',
      # Preserve .107 regional admission in the test-only fault copy. Inject
      # failure at the same actual upload result after admission succeeds.
      'if(!GpuStrong1960.regionalEvidence19107(runtime,width,height)||!session.upload(2,runtime))':'if(!GpuStrong1960.regionalEvidence19107(runtime,width,height)||!ModelParallelFault1996.upload(session.upload(2,runtime)))',
      'result=session.collect(ticket,new int[]{17,4,5,6},new int[]{64,hn,qn,en});':'result=ModelParallelFault1996.collect(session.collect(ticket,new int[]{17,4,5,6},new int[]{64,hn,qn,en}));'
    }
    for old,new in replacements.items():
        if body.count(old)!=1:raise AssertionError('Fault seam drift: '+old)
        body=body.replace(old,new)
    decorated=work/'fault-source/GpuAnalysis1961.java';decorated.parent.mkdir(exist_ok=True);decorated.write_text(body)
    faults=work/'fault-classes';faults.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',faults,decorated,*fixtures],'compile-faults')
    fault=json.loads(run(java+['-cp',str(faults)+os.pathsep+cp,'com.hiro.ulike.ModelParallel1996Test','faults'],'real-parallel-faults').split('RESULT ',1)[1])
    if normal.get('model_cases')!=108 or fault.get('fault_modes')!=7:raise AssertionError('Missing overlap source/model/fault coverage')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Source changed during model overlap proof')
    result=dict(status='passed',assertions=retained['assertions']+normal['assertions']+fault['assertions'],tests=[retained,normal,fault],actual_jni=True,actual_native_cpu=True,whole_model_bitwise_equal=True,actual_source_regions_overlapped=True,resident_runtime_upload_bitwise_equal=True,model_ticket_joined_before_handoff=True,callback_once_and_fallback_verified=True,existing_region_gpu_route_preserved=True,invalid_completed_runtime_rejected=True,physical_android_tested=False,device_speedup_verified=False,source_sha256=pins,fault_copy_sha256=sha(decorated))
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
