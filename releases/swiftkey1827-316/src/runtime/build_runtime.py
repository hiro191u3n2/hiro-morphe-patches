#!/usr/bin/env python3
"""Replace only the published FlickSymbols family; validate host and native ABI."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,zipfile
ROOT=Path(__file__).resolve().parent
BASE_SHA='94e7af5056df941fc935ff8876663fe5003584ab0e38f8e71f9c8d195f907755'
APK_SHA='e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def require(ok,message):
 if not ok:raise RuntimeError(message)
def helper(name):return name=='hiro/swiftkey/FlickSymbols.smali' or name.startswith('hiro/swiftkey/FlickSymbols$')
def files(folder):return {p.relative_to(folder).as_posix():p.read_bytes() for p in folder.rglob('*.smali')}
def main():
 p=argparse.ArgumentParser();p.add_argument('--runtime',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--apk',type=Path);p.add_argument('--compile-only',action='store_true')
 a=p.parse_args();a.runtime=a.runtime.resolve();a.tools=a.tools.resolve();a.work=a.work.resolve();a.output=a.output.resolve()
 require(sha(a.runtime)==BASE_SHA,'Wrong published v1.8.26 runtime')
 require(not a.work.exists(),'Fresh runtime build work required');a.work.mkdir(parents=True);a.output.mkdir(parents=True,exist_ok=True)
 def run(command,name):
  r=subprocess.run(list(map(str,command)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
  (a.work/(name+'.log')).write_text(r.stdout);print(r.stdout,end='',flush=True);require(r.returncode==0,'Command failed: '+name);return r.stdout
 tc=a.work/'tool-classes';tc.mkdir()
 run(['java','com.sun.tools.javac.Main','-cp',a.tools/'morphe.jar','-d',tc,ROOT/'tools_java/Merge1827.java'],'compile-merge')
 run(['java','com.sun.tools.javac.Main','-cp',a.tools/'apktool.jar','-d',tc,ROOT/'tools_java/Dump1814.java'],'compile-dump')
 dump=['java','-cp',str(tc)+os.pathsep+str(a.tools/'apktool.jar'),'Dump1814']
 run(dump+[a.runtime,a.work/'before'],'dump-before');before=files(a.work/'before')
 require(len(before)==91 and sum(helper(n) for n in before)==5,'Published runtime class inventory changed')
 for kind in ['baseline','fixed']:
  folder=a.work/kind;folder.mkdir();classes=folder/'classes';classes.mkdir();dex=folder/'dex';dex.mkdir()
  source=ROOT/('baseline' if kind=='baseline' else 'src')/'hiro/swiftkey/FlickSymbols.java'
  run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',classes,*sorted((ROOT/'stubs').rglob('*.java')),source],kind+'-javac')
  with zipfile.ZipFile(folder/'runtime.jar','w') as runtime,zipfile.ZipFile(folder/'stubs.jar','w') as stubs:
   for f in sorted(classes.rglob('*.class')):
    name=f.relative_to(classes).as_posix();(runtime if helper(name.replace('.class','.smali')) else stubs).write(f,name)
  run(['java','-cp',a.tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',a.tools/'android.jar','--classpath',folder/'stubs.jar','--output',dex,folder/'runtime.jar'],kind+'-d8')
  run(dump+[dex/'classes.dex',folder/'smali'],kind+'-dump')
 baseline=files(a.work/'baseline/smali');overlay=files(a.work/'fixed/smali')
 require(baseline and set(baseline)=={n for n in before if helper(n)},'Baseline source family inventory differs')
 require(all(before[n]==v for n,v in baseline.items()),'Restored baseline Java differs from published DEX')
 require(set(overlay)==set(baseline),'Flick family inventory changed')
 java=os.environ.get('MORPHE_JAVA','java')
 run([java,'-cp',str(tc)+os.pathsep+str(a.tools/'morphe.jar'),'Merge1827',a.runtime,a.work/'fixed/dex/classes.dex',a.output/'runtime.mpe',a.output/'merge-report.json'],'merge')
 run(dump+[a.output/'runtime.mpe',a.work/'after'],'dump-after');after=files(a.work/'after')
 require(set(after)==set(before),'Runtime inventory changed')
 changed=sorted(n for n in before if before[n]!=after[n])
 require(set(changed)=={'hiro/swiftkey/FlickSymbols.smali','hiro/swiftkey/FlickSymbols$Stroke.smali'},'Unexpected changed runtime classes: '+str(changed))
 require(all(after[n]==v for n,v in before.items() if not helper(n)),'Unrelated runtime class changed')
 require(all(after[n]==v for n,v in overlay.items()),'Merged helper differs from compiled source')
 runtime_sha=sha(a.output/'runtime.mpe')
 run(['python',ROOT/'tests/run_english_tests.py','--source-dir',ROOT,'--work',a.work/'host'],'host')
 host=json.loads((a.work/'host/report.json').read_text());host['runtime_sha256']=runtime_sha
 require(host['status']=='PASSED' and host['baseline_english_failure_reproduced'],'Missing English negative control')
 abi_path=a.output/'native-abi.json'
 if a.apk:
  require(sha(a.apk)==APK_SHA,'Unknown native reference archive')
  run(['python',ROOT/'tests/audit_native_abi.py','--apk',a.apk,'--extension',a.output/'runtime.mpe','--output',abi_path,'--tools',a.tools,'--source-dir',ROOT,'--work',a.work/'abi'],'native-abi')
 elif (ROOT/'tests/native-abi.json').is_file():abi_path.write_bytes((ROOT/'tests/native-abi.json').read_bytes())
 elif a.compile_only:
  print('COMPILED with host verification; native ABI not yet certified');return
 else:raise RuntimeError('Pinned native ABI evidence missing')
 abi=json.loads(abi_path.read_text())
 require(abi['status']=='PASSED' and abi['extension_sha256']==runtime_sha and abi['apk_sha256']==APK_SHA,'Native ABI evidence differs')
 require(not abi['unresolved'] and not abi['inaccessible'] and not abi['incompatible_opcodes'],'Native ABI unresolved')
 require(abi['verifier_source_sha256']==sha(ROOT/'tools_java/AuditSensitivityAbi.java') and abi['wrapper_source_sha256']==sha(ROOT/'tests/audit_native_abi.py'),'Native verifier source mismatch')
 refs={x['reference'] for x in abi['native_and_extension_references']}
 require('Lqj0/a;->N1:Lqj0/a;' in refs and 'Lcom/microsoft/fluency/ResultsFilter$PredictionSearchType;->NORMAL:Lcom/microsoft/fluency/ResultsFilter$PredictionSearchType;' in refs,'New English native references not audited')
 require(set(abi['audited_classes'])=={'L'+n[:-6]+';' for n in overlay},'Native audit family incomplete')
 review=json.loads((ROOT/'tests/native_route_review.json').read_text())
 require(review['status']=='STATIC_REVIEW_PASSED' and not review['blockers'],'Native route review failed')
 require(review['production_source_sha256']==sha(ROOT/'src/hiro/swiftkey/FlickSymbols.java') and review['reference_apk_sha256']==APK_SHA,'Native route review source differs')
 require(review['device_tested'] is False and review['native_flow_engine_executed'] is False,'Native route scope differs')
 report={'status':'PASSED','baseline_runtime_sha256':BASE_SHA,'runtime_sha256':runtime_sha,
  'baseline_java_matches_published_dex':True,'host_tests_passed':True,'host_report':host,
  'changed_classes':['L'+n[:-6]+';' for n in changed],'all_other_classes_preserved':True,
  'runtime':json.loads((a.output/'merge-report.json').read_text()),'native_abi_report':abi,'native_route_review':review,
  'device_tested':False,'original_apk_apply_tested':False}
 (a.output/'runtime-report.json').write_text(json.dumps(report,sort_keys=True,indent=2)+'\n')
 print('PASS English up-flick runtime '+runtime_sha)
if __name__=='__main__':main()
