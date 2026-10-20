#!/usr/bin/env python3
"""Retained .97 planning/pixel cases with explicit optimized-kernel ARM proof."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess, sys
ROOT=Path(__file__).resolve().parent
REFERENCE={
 'QualityPipeline1932.java':'7ebe2bf0d0621c3d463c0b9cbcd6e2bcf697ca9f9b154db7a88f0625c77e971b',
 'SingleNoise1955.java':'e7cd9f0dc29ba75867c3160394fcd538eb67a526c70a9d4fd57149c103d157af',
 'CpuSingle1978.java':'8570fed5472452ff38dd602c611bb447531d81cac5c380a86bfa504889886bd0',
 'single_noise1955.c':'9ce27f6b990a68c32c42e936d9bcdeda9bfdbeebf7be48d2f57b64c3d39d1938'}
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']).resolve()
 choices=[Path(os.environ.get('ULIKE1990_CHAIN_CLASSES','/missing')),work.parent/'chain/host_chain1978/classes']
 base=next((p for p in choices if (p/'com/hiro/ulike/SingleNoise1955.class').is_file()),None)
 if base is None:raise AssertionError('Run current chain test first or set ULIKE1990_CHAIN_CLASSES')
 ref=source/'tests1997/front/reference1996'
 for name,pin in REFERENCE.items():
  if sha(ref/name)!=pin:raise AssertionError('Immutable .96 source changed: '+name)
 sys.path.insert(0,str(source));from host_speed1959 import _commit_fixture
 fixture=_commit_fixture(source/'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',work/'fixtures/android/graphics/Bitmap.java')
 harness=source/'tests1997/front/FrontPixels1997.java';planner=source/'tests1997/front/FrontPlan1997Test.java'
 current=[source/p for p in ['QualityPipeline1932.java','ProcessingTiming1947.java','SingleNoise1955.java','CpuSingle1978.java']]
 # The math/transport closure for direct native calls is unchanged and compiled
 # again from pinned .96 sources in a separate JVM; no candidate kernel is used.
 tracked=current+[harness,planner,source/'host_front_plan19100.py',source/'host_front_arm19100.py',source/'tests1997/front/FrontArm1997.c',source/'native1955/single_noise1955.c']+[ref/name for name in REFERENCE]
 pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
 def run(args,name):
  args=list(map(str,args));commands.append(dict(name=name,argv=args));done=subprocess.run(args,capture_output=True,text=True,timeout=300);output=done.stdout+done.stderr;(work/(name+'.log')).write_text(output)
  if done.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+'\n'+output[-12000:])
  return output
 include=jdk/'include';cc=shutil.which('cc');flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-fvisibility=hidden','-shared','-Wl,--no-undefined']
 for label,cfile,files in [('reference',ref/'single_noise1955.c',[ref/p for p in REFERENCE if p.endswith('.java')]),('candidate',source/'native1955/single_noise1955.c',current+[planner])]:
  classes=work/label/'classes';native=work/label/'native';classes.mkdir(parents=True,exist_ok=True);native.mkdir(parents=True,exist_ok=True)
  run([cc,*flags,'-I'+str(include),'-I'+str(include/'linux'),cfile,'-lm','-pthread','-o',native/'libulike_nr1955.so'],label+'-native')
  run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',base,'-d',classes,*files,fixture,harness],label+'-compile')
 java=[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m']
 data=work/'immutable96-pixels.bin'
 oracle=json.loads(run(java+['-Djava.library.path='+str(work/'reference/native'),'-cp',str(work/'reference/classes')+os.pathsep+str(base),'com.hiro.ulike.FrontPixels1997',data],'reference-pixels').split('RESULT ',1)[1])
 result=json.loads(run(java+['-Djava.library.path='+str(work/'candidate/native'),'-cp',str(work/'candidate/classes')+os.pathsep+str(base),'com.hiro.ulike.FrontPlan1997Test',data],'candidate-pixels').split('RESULT ',1)[1])
 if result.get('pixel_cases')!=128 or result.get('planner_cases')!=15100:raise AssertionError('Missing plan/strip/parallel cases')
 result['assertions']+=oracle['assertions'];result['reference']=oracle
 (work/'desktop-result.json').write_text(json.dumps(result,indent=2)+'\n')
 spec=importlib.util.spec_from_file_location('front_arm19100',source/'host_front_arm19100.py');arm=importlib.util.module_from_spec(spec);spec.loader.exec_module(arm)
 result['arm97']=arm.test(source,work/'arm97',jdk,ndk)
 if result['arm97'].get('status')!='passed' or result['arm97'].get('actual_arm_neon_executed') is not True:raise AssertionError('Actual AArch64 execution is mandatory')
 result['assertions']+=result['arm97']['assertions']
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Sources changed during front proof')
 result.update(retained97_planning_cases_on_optimized_kernel=True,actual_native_cpu=True,bitmap_commit_precision_is_surrogate=True,source_sha256=pins,oracle_pixels_sha256=sha(data),immutable_96_reference_sha256=REFERENCE)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
