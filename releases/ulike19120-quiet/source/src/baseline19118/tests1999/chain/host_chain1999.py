#!/usr/bin/env python3
"""Production route selection with a controlled comparator boundary; production
queue cancellation/progress. Numerical JNI oracles remain in retained chain and
finish audits. This runner supplies no fabricated pixel or device-speed proof.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, subprocess, sys

def module(name,path):
 spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 android=Path(os.environ.get('ULIKE_ANDROID_JAR',str(source.parent/'tools/android.jar'))).resolve()
 fixture=source/'tests1999/chain'
 tracked=[source/n for n in ('GpuChain1961.java','GpuResident1976.java','GpuQualification1961.java')]+list(fixture.glob('*'))
 pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked if p.is_file()}
 def run(label,args):
  done=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180);output=done.stdout+done.stderr;(work/(label+'.log')).write_text(output)
  if done.returncode:raise AssertionError(label+'\n'+output[-16000:])
  return done.stdout
 # Retain established memory/ownership/retry tests against current production.
 queue=module('queue_chain99',source/'host_queue1994.py').test(source,work/'queue',jdk,ndk)
 qclasses=work/'queue/retained93/classes'
 run('progress-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',qclasses,'-d',qclasses,fixture/'ResidentProgress1999Test.java'])
 progress=json.loads(run('progress-run',[jdk/'bin/java','-ea','-cp',qclasses,'com.hiro.ulike.ResidentProgress1999Test']).strip().splitlines()[-1])
 # Compile the actual production dependency closure. Only comparator boundary
 # results are controlled; selector/key/certificate/cancellation code is intact.
 sys.path.insert(0,str(source));build=module('build_chain99',source/'build1998.py')
 original=(source/'GpuChain1961.java').read_text()
 anchor='''        Timing1962 timing=new Timing1962(null);
        try {
            finishStage1988(6);'''
 if original.count(anchor)!=1:raise AssertionError('Unique complete comparator boundary missing')
 decorated=original.replace(anchor,'''        if(ChainRouting1999Test.controlled)return ChainRouting1999Test.comparison(tileRows);
'''+anchor)
 copied=work/'GpuChain1961.java';copied.write_text(decorated)
 sources=list(dict.fromkeys(build.compile_inputs([]).values()));sources=[copied if p.name=='GpuChain1961.java' else p for p in sources]
 classes=work/'routing-classes';classes.mkdir(exist_ok=True)
 run('routing-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources,fixture/'ChainRouting1999Test.java'])
 bitmap=work/'Bitmap.java';bitmap.write_text((source/'tests/pipeline1942-fixtures/android/graphics/Bitmap.java').read_text().replace('public final class Bitmap {','public final class Bitmap {public int getGenerationId(){return 1;}'))
 buildpeer=work/'Build.java';buildpeer.write_text('package android.os;public class Build {public static String FINGERPRINT="fixture99";public static class VERSION {public static int SDK_INT=36;}}')
 run('routing-android-peers',[jdk/'bin/javac','--release','8','-cp',str(classes)+os.pathsep+str(android),'-d',classes,bitmap,buildpeer,source/'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',source/'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java'])
 routing=json.loads(run('routing-run',[jdk/'bin/java','-ea','-cp',str(classes)+os.pathsep+str(android),'com.hiro.ulike.ChainRouting1999Test']).strip().splitlines()[-1])
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked if p.is_file()}:raise AssertionError('Chain sources changed during audit')
 result=dict(status='passed',assertions=queue['assertions']+progress['assertions']+routing['assertions'],tests=[queue,progress,routing],resident_baseline_schedule_matches=True,resident_baseline_key_binds_wide_state=True,resident_progress_cancellation_frozen=True,source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
