#!/usr/bin/env python3
"""Execute actual timing view-refresh behavior using the retained .101 peer basis.
Controlled UI dispatch verifies visibility/coalescing/failure recovery; no FPS claim.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess

ROOT=Path(__file__).resolve().parent

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

def test(source,work,jdk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    modules={n:module('refresh121_'+n,source/('host_timing'+n+'.py')) for n in ('1982','1984','1985','1986','19101')}
    fixture=source/'tests19121/diagnostic';production=source/'ProcessingTiming1947.java'
    boost=source/'CaptureBoost19123.java'
    inputs=[production,boost,Path(__file__).resolve(),*[source/('host_timing'+n+'.py') for n in modules],*sorted(fixture.rglob('*.java'))]
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
    paths=[]
    for name,body in modules['1982'].STUBS.items():
        path=work/'fixtures'/name;path.parent.mkdir(parents=True,exist_ok=True)
        replacement=fixture/name
        if replacement.exists():body=replacement.read_text()
        else:
            body=modules['1986'].adapt_timing_peers1986(modules['1985'].adapt_timing_peers1985(modules['1984'].adapt_timing_peers1984(body)))
            if 'final class GpuQualification1961 {' in body:
                body=body.replace('final class GpuQualification1961 {',modules['19101'].QUALIFICATION_1995)
                body+=modules['19101'].FAMILIES_1995
                body+='\nfinal class SpeedWorkers1935 {static long backgroundPriorityApplied19121(){return 3;}static long backgroundPriorityFailed19121(){return 1;}}\n'
        path.write_text(body);paths.append(path)
    startup=work/'fixtures/com/hiro/ulike/GpuStartup1990.java'
    startup.write_text('''package com.hiro.ulike;
final class GpuStartup1990 {
 static Snapshot current=new Snapshot();static void initialize(android.content.Context c){}
 static Snapshot snapshot(){return current;}
 static final class Snapshot {int state,preparedPrograms,failedPrograms;long startedNanos,readyNanos,elapsedNanos;boolean domainReady;String finishWideStatus1995="";}
}''');paths.append(startup)
    paths.append(fixture/'com/hiro/ulike/DiagnosticRefresh19121Test.java')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    def run(label,args):
        done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=60)
        (work/(label+'.log')).write_text(done.stdout+done.stderr)
        if done.returncode:raise AssertionError(label+'\n'+(done.stdout+done.stderr)[-16000:])
        return done.stdout
    run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,production,boost,*paths])
    result=json.loads(run('run',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.DiagnosticRefresh19121Test']).strip().splitlines()[-1])
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}:
        raise AssertionError('Diagnostic test inputs changed during execution')
    result.update(source_sha256=pins,production_source_sha256=pins['ProcessingTiming1947.java'],
        generated_fixture_sha256={str(p.relative_to(work)):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths if p.is_relative_to(work)},
        physical_android_tested=False,device_speedup_verified=False,
        fixture_scope='Actual ProcessingTiming1947 view refresh executes against controlled Android activity/preference/UI-dispatch fixtures and retained qualification peers. No device FPS measured.')
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',default=ROOT);parser.add_argument('--work',required=True);parser.add_argument('--jdk')
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk),ensure_ascii=False))
