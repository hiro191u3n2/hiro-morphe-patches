#!/usr/bin/env python3
"""Current CPU policy producer and committed half-image concurrency tests.

Run after the current chain group. Controlled native-consumer/Bitmap boundaries
exercise production policy and strip ownership, without claiming Android speed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
 choices=[Path(os.environ.get('ULIKE1994_CHAIN_CLASSES','/missing')),Path(os.environ.get('ULIKE1990_CHAIN_CLASSES','/missing')),work.parent/'chain/host_chain1978/classes']
 base=next((p for p in choices if (p/'com/hiro/ulike/QualityPipeline1932.class').is_file()),None)
 if base is None:raise AssertionError('Run current chain group first or explicitly supply ULIKE1994_CHAIN_CLASSES')
 tracked=[source/p for p in ['SingleNoise1955.java','QualityPipeline1932.java','GpuPolicy1960.java','PairedRegions1976.java','quality-dependencies/com/hiro/ulike/FaceRegions1934.java','host_front1994.py','tests1994/front/FrontPolicy1994Test.java','tests1994/front/FrontHalf1994Test.java','tests/pipeline1942-fixtures/android/graphics/Bitmap.java','host_speed1959.py']]
 tracked+=sorted((source/'tests/h24h26/graphics').rglob('*.java'))
 pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 def run(label,args):
  completed=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=120);out=completed.stdout+completed.stderr;(work/(label+'.log')).write_text(out)
  if completed.returncode:raise AssertionError(label+'\n'+out[-12000:])
  return completed.stdout
 policy=work/'policy-classes';policy.mkdir(exist_ok=True)
 inputs=list((source/'tests/h24h26/graphics').rglob('*.java'))+[source/p for p in ['SingleNoise1955.java','GpuPolicy1960.java','PairedRegions1976.java','quality-dependencies/com/hiro/ulike/FaceRegions1934.java','tests1994/front/FrontPolicy1994Test.java']]
 run('policy-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',base,'-d',policy,*inputs])
 p=json.loads(run('policy-run',[jdk/'bin/java','-ea','-cp',str(policy)+os.pathsep+str(base),'com.hiro.ulike.FrontPolicy1994Test']).strip().splitlines()[-1])
 # The preserved fixture implements an explicit integer premultiplication
 # surrogate; it is not labelled a physical Android Bitmap implementation.
 spec=importlib.util.spec_from_file_location('front94_commit_fixture',source/'host_speed1959.py');fixture=importlib.util.module_from_spec(spec);spec.loader.exec_module(fixture)
 bitmap=fixture._commit_fixture(source/'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',work/'bitmap/android/graphics/Bitmap.java')
 half=work/'half-classes';half.mkdir(exist_ok=True)
 run('half-compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',base,'-d',half,bitmap,source/'QualityPipeline1932.java',source/'tests1994/front/FrontHalf1994Test.java'])
 h=json.loads(run('half-run',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',str(half)+os.pathsep+str(base),'com.hiro.ulike.FrontHalf1994Test']).strip().splitlines()[-1])
 if pins!={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Sources changed during frontend proof')
 result=dict(status='passed',assertions=p['assertions']+h['assertions'],tests=[p,h],source_sha256=pins,production_policy_and_pipeline=True,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--source',required=True);parser.add_argument('--work',required=True);parser.add_argument('--jdk');parser.add_argument('--ndk');a=parser.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk)))
