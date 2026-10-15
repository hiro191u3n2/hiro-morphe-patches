#!/usr/bin/env python3
"""Execute actual timing and native program-build diagnostics at their boundaries.

The GL driver is controlled in the C fixture; no physical device is claimed.
The retained real JNI transport group separately guards callback ownership and
all successful/failing readback paths after the polling allocation change.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess

def module(name, path):
    spec=importlib.util.spec_from_file_location(name,path)
    value=importlib.util.module_from_spec(spec); spec.loader.exec_module(value); return value

def test(source, work, jdk=None, ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
    fixture=source/'tests19100/diagnostics'
    tracked=[source/'ProcessingTiming1947.java',source/'GpuNoise1960.java',source/'native1960/engine1960.c',Path(__file__),*sorted(fixture.glob('*'))]
    def pins():return {str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    before=pins();commands=[]
    def run(args, name):
        args=list(map(str,args));commands.append({'name':name,'argv':args})
        done=subprocess.run(args,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180)
        (work/(name+'.log')).write_text(done.stdout)
        if done.returncode:raise RuntimeError(name+' failed:\n'+done.stdout[-10000:])
        return done.stdout
    def parsed(output):return json.loads(output.split('RESULT ',1)[1].strip())
    timing=module('diagnostics19100_timing',source/'host_timing19101.py').test(source,work/'timing',jdk,ndk)
    classes=work/'timing/classes'
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,*sorted(fixture.glob('*.java'))],'timing-diagnostic-compile')
    scalar=parsed(run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.ShaderFailureTiming19100Test'],'timing-diagnostic-run'))
    builder=module('diagnostics19100_native',source/'native1960/build_native1960.py')
    builder.shader_header(source/'native1960',work)
    executable=work/'program-failures'
    run(['cc','-std=c11','-O2','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-fno-fast-math','-ffp-contract=off',
         '-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),'-I'+str(source/'native1960'),
         fixture/'ProgramFailure19100Test.c','-l:libEGL.so.1','-l:libGL.so.1','-o',executable],'native-diagnostic-compile')
    native=parsed(run([executable],'native-diagnostic-run'))
    assert before==pins(),'Diagnostic production or test source changed during execution'
    assert scalar.get('scalar_failure_isolation') is True
    assert native.get('native_program_failure_cache') is True
    result={'status':'passed','assertions':timing['assertions']+scalar['assertions']+native['assertions'],
            'scalar_failure_isolation':True,'native_program_failure_cache':True,'scalar':scalar,'native':native,
            'retained_timing':timing,'source_sha256':before,'physical_android_tested':False,'device_speedup_verified':False}
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk')
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),ensure_ascii=False))
