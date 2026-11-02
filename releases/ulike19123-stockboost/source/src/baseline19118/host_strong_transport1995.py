#!/usr/bin/env python3
"""Real .95 private-output Strong transport, real CPU fallback and fault gates.

Retains the full 68-case production startup CPU/GLES replay on both banks.
The normal stage suite compiles untouched production. A separate fault suite
decorates only the existing synchronous readback call after real JNI succeeds;
all source math, shaders, admission and StrongNoise fallback remain production.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,sys
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    spec=importlib.util.spec_from_file_location('transport95_startup',source/'host_startup_math1990.py');base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
    standard=base.test(source,work/'standard',jdk,ndk)
    sys.path.insert(0,str(source));import build1990
    sources=list(dict.fromkeys(build1990.compile_inputs([]).values()))
    fixtures=[source/'tests1995/strong/StrongTransport1995Test.java',source/'tests1995/strong/GpuTransportFault1995.java',source/'tests1995/strong/StrongCollect19117Test.java'];tracked=sources+fixtures+[Path(__file__)]
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar')))
    cp=str(work/'standard/classes')+os.pathsep+str(android);env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1');commands=[]
    def run(args,label):
        args=list(map(str,args));commands.append(dict(name=label,argv=args));p=subprocess.run(args,capture_output=True,text=True,env=env,timeout=300);output=p.stdout+p.stderr;(work/(label+'.log')).write_text(output)
        if p.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise RuntimeError(label+' failed:\n'+output[-14000:])
        return output
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',work/'standard/classes',*fixtures],'normal-harness-compile')
    native_path=str(work/'standard')+os.pathsep+str(work/'standard/cpu-native')
    java=[jdk/'bin/java','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+native_path]
    normal=json.loads(run(java+['-cp',cp,'com.hiro.ulike.StrongTransport1995Test'],'actual-stage-direct-output').split('RESULT ',1)[1])
    observation=json.loads(run(java+['-cp',cp,'com.hiro.ulike.StrongCollect19117Test'],'foreground-readback-observation').split('RESULT ',1)[1])
    transport=(source/'GpuNoise1960.java').read_text()
    reads=(('return readManyIntoNative(token,ticket.nativeTicket,slots,counts,targets,offsets);',
            'readManyIntoNative(token,ticket.nativeTicket,slots,counts,targets,offsets)'),
           ('return readManyIntoObservedNative19117(token,ticket,slots,counts,targets,offsets,times);',
            'readManyIntoObservedNative19117(token,ticket,slots,counts,targets,offsets,times)'))
    for anchor,call in reads:
        if transport.count(anchor)!=1:raise AssertionError('Readback fault anchor changed: '+call)
        transport=transport.replace(anchor,'return GpuTransportFault1995.after('+call+',counts,targets,offsets);')
    for anchor in ('boolean collectManyInto(final Ticket ticket,final int[] slots,final int[] counts,',
                   'boolean collectManyIntoObserved19117(final Ticket ticket,final int[] slots,final int[] counts,'):
        pos=transport.index(anchor);start=transport.index('            try{\n',pos)
        transport=transport[:start]+transport[start:].replace('            try{\n','            try{\n                GpuTransportFault1995.before();\n',1)
    decorated=work/'test-only/GpuNoise1960.java';decorated.parent.mkdir(exist_ok=True);decorated.write_text(transport)
    fault_classes=work/'fault-classes';fault_classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',fault_classes,decorated,*fixtures],'fault-transport-compile')
    faults=json.loads(run(java+['-cp',str(fault_classes)+os.pathsep+cp,'com.hiro.ulike.StrongTransport1995Test','faults'],'actual-native-post-read-faults').split('RESULT ',1)[1])
    observed_fault=json.loads(run(java+['-cp',str(fault_classes)+os.pathsep+cp,'com.hiro.ulike.StrongCollect19117Test','fault'],'observed-post-read-fault').split('RESULT ',1)[1])
    if normal.get('direct_cases')!=136 or normal.get('compared_pixels',0)<600000 or faults.get('fault_modes')!=5:raise AssertionError('Missing direct transport/fault coverage')
    if observation.get('observed_collects')!=2 or observed_fault.get('observed_collects')!=1 or not observation.get('pixels_exact') or not observed_fault.get('pixels_exact') or not observed_fault.get('fault'):raise AssertionError('Missing real foreground observed JNI readback or rollback coverage')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Production changed during private-output proof')
    result=dict(status='passed',assertions=standard['assertions']+normal['assertions']+faults['assertions']+observation['assertions']+observed_fault['assertions'],standard=standard,direct_transport=normal,post_read_faults=faults,foreground_observation=observation,observed_post_read_fault=observed_fault,actual_jni=True,actual_native_cpu=True,physical_android_tested=False,device_speedup_verified=False,source_sha256=pins,fault_transport_sha256=sha(decorated))
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
