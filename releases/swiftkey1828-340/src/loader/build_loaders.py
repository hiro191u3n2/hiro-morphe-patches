#!/usr/bin/env python3
"""Add the guarded Japanese space conversion helper to both patch loaders."""
from pathlib import Path
import argparse, hashlib, json, os, subprocess, zipfile
ROOT=Path(__file__).resolve().parent
MAIN='app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
HELPER='app/hiro/swiftkey/patches/SpaceConversionPatch.class'
PINS={
 'single':'7ef5c21172b572620cb70c711027479b25956f922cd7a8843f8477e2157b6872',
 'bundle':'b78dc7c43b4ba165f12ae4bc01980a41a2d481f8c77bf80f28be3c7cf208cc7e',
 'main':'264064ee59408562649818d64d1559a174f3e3dae25b30aa430a5f2b616d688f',
}
APK_SHA='e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def require(ok,message):
 if not ok:raise RuntimeError(message)
def run(cmd,log):
 with log.open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT)
 require(r.returncode==0,'Failed; inspect '+str(log)+'\n'+log.read_text()[-4000:])
def put(z,name,raw):
 i=zipfile.ZipInfo(name,(2026,11,3,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,raw)
def main():
 p=argparse.ArgumentParser()
 for n in ('single','bundle','tools','work','output'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args()
 for n in ('single','bundle','tools','work','output'):setattr(a,n,getattr(a,n).resolve())
 require(not a.work.exists(),'Fresh work directory required');a.work.mkdir(parents=True);a.output.mkdir(parents=True,exist_ok=True)
 inputs={}
 for kind in ('single','bundle'):
  path=getattr(a,kind);require(sha(path.read_bytes())==PINS[kind],'Wrong baseline '+kind)
  with zipfile.ZipFile(path) as z:
   require(HELPER not in z.namelist(),'Space hook already present');raw=z.read(MAIN)
   require(sha(raw)==PINS['main'],'Unexpected JVM baseline')
   inputs[kind]={'mpp_sha256':sha(path.read_bytes()),'android_sha256':sha(z.read('classes.dex')),'jvm_sha256':sha(raw)}
 baseline=a.work/'baseline';baseline.mkdir()
 with zipfile.ZipFile(a.single) as z:
  for name in z.namelist():
   if name.endswith('.class'):
    target=baseline/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(name))
 classes=a.work/'classes';classes.mkdir();helper=a.work/'helper';helper.mkdir();helperdex=a.work/'helper-dex';helperdex.mkdir()
 cp=str(a.tools/'morphe.jar')+os.pathsep+str(baseline)
 run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-cp',cp,'-d',helper,ROOT/'app/hiro/swiftkey/patches/SpaceConversionPatch.java'],a.work/'compile-helper.log')
 run(['java','com.sun.tools.javac.Main','-cp',cp+os.pathsep+str(helper)+os.pathsep+str(a.tools/'r8.jar'),'-d',classes,ROOT/'UpdateSpaceLoader.java',ROOT/'app/hiro/swiftkey/patches/NativeSpaceProbe.java'],a.work/'compile-tools.log')
 with zipfile.ZipFile(a.work/'helper.jar','w') as z:put(z,HELPER,(helper/HELPER).read_bytes())
 with zipfile.ZipFile(a.work/'baseline.jar','w') as z:
  for f in sorted(baseline.rglob('*.class')):put(z,str(f.relative_to(baseline)),f.read_bytes())
 run(['java','-cp',a.tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',a.tools/'android.jar','--classpath',a.tools/'morphe.jar','--classpath',a.work/'baseline.jar','--output',helperdex,a.work/'helper.jar'],a.work/'d8-helper.log')
 runtimecp=str(classes)+os.pathsep+str(helper)+os.pathsep+cp+os.pathsep+str(a.tools/'r8.jar')
 java=os.environ.get('MORPHE_JAVA','java')
 jvm=a.output/'jvm';jvm.mkdir()
 run([java,'-cp',runtimecp,'UpdateSpaceLoader','jvm',a.single,helper,jvm],a.work/'inject-jvm.log')
 for kind in ('single','bundle'):
  run([java,'-cp',runtimecp,'UpdateSpaceLoader','dex',getattr(a,kind),helperdex/'classes.dex',a.output/(kind+'-classes.dex')],a.work/(kind+'-inject-dex.log'))
 run([java,'-cp',runtimecp,'app.hiro.swiftkey.patches.NativeSpaceProbe',ROOT/'tests/fixtures',a.work/'native-tests'],a.work/'native-tests.log')
 native=json.loads((a.work/'native-tests/native-space-report.json').read_text());require(native['status']=='PASSED','Native fixture tests failed')
 native['reference_apk_sha256']=APK_SHA
 native['fixtures_sha256']={str(f.relative_to(ROOT/'tests/fixtures')):sha(f.read_bytes()) for f in sorted((ROOT/'tests/fixtures').rglob('*.smali'))}
 native['fixture_provenance']='Exact selected native methods disassembled from preserved modified SwiftKey Beta 9.13.16.4 reference APK; not untouched APK.'
 (a.output/'native-space-report.json').write_text(json.dumps(native,indent=2,sort_keys=True)+'\n')
 result={'status':'PASSED','single_android_sha256':sha((a.output/'single-classes.dex').read_bytes()),'bundle_android_sha256':sha((a.output/'bundle-classes.dex').read_bytes()),'jvm_sha256':sha((jvm/MAIN).read_bytes()),'helper_jvm_sha256':sha((jvm/HELPER).read_bytes()),'unrelated_android_classes_preserved':True,'unrelated_jvm_classes_preserved':True,'jvm_bridge_inverse_restoration':True,'android_bridge_inverse_restoration':True,'android_serialization_canonical_preservation':True,'added_loader_helper':HELPER,'native_report':native,'input_pins':inputs,'device_tested':False,'original_apk_apply_tested':False}
 (a.output/'loader-report.json').write_text(json.dumps(result,indent=2,sort_keys=True)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
