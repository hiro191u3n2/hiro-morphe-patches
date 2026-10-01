#!/usr/bin/env python3
import argparse,hashlib,json,os,subprocess,tempfile,time
from pathlib import Path
ROOT=Path(__file__).resolve().parent
CORE=ROOT.parent
WORK=CORE.parents[1]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args,env=None):
 r=subprocess.run([str(x) for x in args],check=True,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=env);return r.stdout

def main():
 p=argparse.ArgumentParser();p.add_argument('--jdk-bin',type=Path,default=WORK/'models168/tools/jdk21/jdk-21.0.12.1+1/bin');p.add_argument('--natural',type=Path,default=WORK.parent/'upload/ULike_Natural_blush_1790815043265.zip');p.add_argument('--purity',type=Path,default=WORK.parent/'upload/ULike_Purity2_1790815028634.zip');a=p.parse_args()
 env=os.environ.copy();env['ORT_DISABLE_TELEMETRY']='1';env['PYTHONPATH']=str(WORK/'models168/python_deps')+os.pathsep+str(WORK/'models168/emulation_deps')
 logs={}
 effect=WORK/'models168/inputs/stock/split_config.arm64_v8a/lib/arm64-v8a/libeffect.so'
 logs['native_evidence']=run(['python3',ROOT/'verify_native.py','--effect',effect,'--output',ROOT/'EVIDENCE.json'],env)
 with tempfile.TemporaryDirectory(prefix='ulike-geometry-qa-') as t:
  tmp=Path(t);packets=tmp/'packets.txt';classes=tmp/'classes';classes.mkdir()
  logs['lua']=run(['python3',ROOT/'test/test_observer.py',a.natural,a.purity,packets],env)
  files=list((ROOT/'src').rglob('*.java'))+list((ROOT/'test').rglob('*.java'))+[CORE/'android_style_binding/src/com/hiro/ulike/binding/AuthoredMesh.java']
  logs['javac']=run([a.jdk_bin/'javac','--release','8','-d',classes,*files],env)
  logs['parser']=run([a.jdk_bin/'java','-Xmx64m','-cp',classes,'com.hiro.ulike.geometry.ObservationContracts',packets],env)
  logs['pinned_mesh_layouts']=run([a.jdk_bin/'java','-Xmx64m','-cp',classes,'com.hiro.ulike.geometry.PinnedMeshContracts',a.natural,a.purity],env)
  android=WORK/'models168/tools/android.jar';dex=tmp/'dex';dex.mkdir();androidClasses=tmp/'android';androidClasses.mkdir()
  logs['android_compile']=run([a.jdk_bin/'javac','-source','8','-target','8','-bootclasspath',android,'-d',androidClasses,*list((ROOT/'src').rglob('*.java'))],env)
  logs['d8']=run([a.jdk_bin/'java','-cp',WORK/'models168/tools/r8-8.3.37.jar','com.android.tools.r8.D8','--min-api','26','--lib',android,'--output',dex,*androidClasses.rglob('*.class')],env)
  dexbytes=(dex/'classes.dex').stat().st_size
 owned=[x for x in ROOT.rglob('*') if x.is_file() and x.name not in ['QA.json','EVIDENCE.json'] and '__pycache__' not in x.parts and 'review' not in x.relative_to(ROOT).parts and not x.name.startswith('INDEPENDENT')]
 dependencies=[CORE/'android_style_binding/instrument_style.py',CORE/'android_style_binding/SCRIPT_OBSERVER_PINS.json',CORE/'android_style_binding/src/com/hiro/ulike/binding/AuthoredMesh.java']
 result={'status':'PASS_PRIVATE_DIAGNOSTIC_ONLY','source_sha256':{str(x.relative_to(CORE)):sha(x) for x in sorted(owned+dependencies)},'logs':logs,'android_dex_bytes':dexbytes,'actual_pinned_archives_checked':True,'phone_execution':False,'production_geometry_verified':False,'complete_binding_provider':False,'device_callback_timing_proven':False}
 (ROOT/'QA.json').write_text(json.dumps(result,indent=2)+'\n');print(result['status'],dexbytes,'DEX bytes')
if __name__=='__main__':main()
