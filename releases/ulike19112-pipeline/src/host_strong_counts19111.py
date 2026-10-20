#!/usr/bin/env python3
"""Bounded benchmark Strong counter forwarding; exact existing dispatcher runs.

The established fixture controls transport and certificates. This is no claim
of real JNI execution, collector timing accuracy, shader quality or device speed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,shutil,subprocess

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 local=source/'tests19111/strong_counts';jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
 inputs=[source/'GpuStrong1960.java',source/'GpuQualification1961.java',source/'host_strong_timing1995.py',source/'host_strong_peers19103.py',source/'tests1982/strong/StrongDiagnosticsFixtures1982.java',source/'tests1982/strong/StrongDiagnostics1982Test.java',source/'tests1990/strongroute/DefaultPeers1990.java',source/'tests1990/strongroute/StrongDefaultRouting1990Test.java',Path(__file__).resolve(),*local.glob('*.java')]
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}
 # Reuse its whole-production compiler and controlled transport adapters. It
 # also executes the established foreground/readback/timing regression cases.
 original=module('strong_counts_original_timing',source/'host_strong_timing1995.py').test(source,work/'original',jdk,ndk)
 classes=work/'original/classes';commands=[]
 def run(label,args):
  args=list(map(str,args));commands.append(dict(label=label,argv=args));r=subprocess.run(args,capture_output=True,text=True,timeout=120);output=r.stdout+r.stderr;(work/(label+'.log')).write_text(output)
  if r.returncode:raise AssertionError(label+'\n'+output[-15000:])
  return r.stdout
 run('compile-counter-fixtures',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',classes,'-d',classes,*sorted(local.glob('*.java'))])
 base=[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp'];reports={}
 reports['counts']=json.loads(run('counts',[*base,classes,'com.hiro.ulike.StrongCounts19111Test']).strip())
 absent=work/'without-sink';shutil.copytree(classes,absent,dirs_exist_ok=True);(absent/'com/hiro/ulike/ResidentTiming19111.class').unlink()
 reports['absent']=json.loads(run('without-sink',[*base,absent,'com.hiro.ulike.StrongCountsMissing19111Test']).strip())
 wrong=work/'wrong-signature';wrong.mkdir(exist_ok=True);p=wrong/'ResidentTiming19111.java';p.write_text('package com.hiro.ulike; public final class ResidentTiming19111 {public static void dispatch(String name){throw new AssertionError("wrong overload must never be called");}}')
 run('compile-wrong-signature',[jdk/'bin/javac','--release','8','-d',wrong,p])
 reports['incompatible']=json.loads(run('wrong-signature',[*base,os.pathsep.join(map(str,[wrong,absent])),'com.hiro.ulike.StrongCountsMissing19111Test']).strip())
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}:raise AssertionError('Counter production source or fixtures changed during test')
 result=dict(status='passed',assertions=sum(r['assertions'] for r in reports.values()),tests=reports,original_regression_assertions=original['assertions'],source_sha256=pins,commands=commands,actual_jni=False,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
