#!/usr/bin/env python3
"""Reproducible home-only back MPP build; no private app/APK/device claims."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess, sys, zipfile
ROOT=Path(__file__).resolve().parent
sys.dont_write_bytecode=True
sys.path.insert(0,str(ROOT.parent/'publication'))
import release_checks as checks
BASE_VERSION='1.0.249';VERSION='1.1.11';BUNDLE_VERSION='1.0.250'
BASE_SHA='c10dda2839b733b54fab850b96f670bbf7cdd6ba687b120184a3103151bc2624'
TOOLS={'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
PREFIX='app/hiro/oneback/patches/';EXT='extensions/oneback_exit.mpe';MF='META-INF/MANIFEST.MF'
PATCH_NAME='戻る1回で終了・履歴削除'
PACKAGES=['com.twitter.android','com.instagram.android','com.zhiliaoapp.musically','com.ss.android.ugc.trill','ctrip.english']
BUNDLE='Hiro_Morphe_Patches_v1.0.250.mpp';SINGLE='OneBack_Exit_NoRecents_v1.1.11.mpp'
REGRESSION_TESTS={
    'twitter119':('Back119TwitterTest',35,104),
    'instagram119':('Back119InstagramTest',15,48),
    'instagram120main':('Back120MainDispatchTest',16,79),
    'instagram120':('Back120InstagramTest',25,92),
    'instagram121layers':('Back121InstagramActivityLayersTest',34,60),
    'instagram121scope':('Back121InstagramScopeTest',9,33),
    'instagram121current':('Back121InstagramCurrentTest',23,78),
    'instagram121dispatch':('Back121InstagramDispatchTest',39,204),
}
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def require(ok,why):
 if not ok:raise RuntimeError(why)
def main():
 p=argparse.ArgumentParser();p.add_argument('--base',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--legacy',type=Path,default=ROOT.parent/'legacy');p.add_argument('--work',type=Path,required=True);p.add_argument('--dist',type=Path,required=True);a=p.parse_args()
 spec=importlib.util.spec_from_file_location('base_util',a.legacy/'build.py');u=importlib.util.module_from_spec(spec);spec.loader.exec_module(u)
 require(sha(a.base)==BASE_SHA,'Current baseline must match reviewed bundle249');
 for name,digest in TOOLS.items():require(sha(a.tools/name)==digest,'Pinned tool mismatch '+name)
 require(not a.work.exists(),'Use a fresh work directory');require(not a.dist.exists() or not any(a.dist.iterdir()),'Use a fresh empty distribution directory');a.work.mkdir(parents=True);a.dist.mkdir(parents=True,exist_ok=True)
 def run(cmd,name):
  log=a.work/name;print('RUN '+name,flush=True)
  with log.open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT,timeout=180,check=False)
  text=log.read_text();require(r.returncode==0,'Command failed '+str(log)+'\n'+text[-8000:]);return text
 jdk=run(['java','--version'],'jdk-version.txt')
 require('Temurin-21.0.8+9' in jdk and '21.0.8' in jdk,'Pinned Temurin JDK 21.0.8+9.0.LTS required')
 require(run(['javac','-version'],'javac-version.txt').strip()=='javac 21.0.8','Pinned javac 21.0.8 required')
 source_snapshot={str(x.relative_to(ROOT)):sha(x) for x in sorted(ROOT.rglob('*')) if x.is_file() and x.suffix in ('.java','.py')}
 validation_sources=checks.source_digests(ROOT.parent)
 for d in ['runtime','patch','test','audit','runtime-dex','patch-dex','fixtures','synthetic']:(a.work/d).mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',a.work/'runtime',*sorted((ROOT/'runtime').glob('*.java'))],'runtime-javac.log')
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'morphe.jar','-d',a.work/'patch',*sorted((ROOT/'patch').glob('*.java'))],'patch-javac.log')
 hostcp=os.pathsep.join(map(str,[a.work/'test',a.work/'runtime',a.tools/'android.jar']))
 run(['javac','--release','8','-encoding','UTF-8','-cp',hostcp,'-d',a.work/'test',*sorted((ROOT/'test-stubs').rglob('*.java')),*sorted((ROOT/'test-runtime').glob('*.java'))],'host-javac.log')
 host=run(['java','-ea','-cp',hostcp,'app.hiro.oneback.runtime.HomeBackHostTest'],'host-tests.txt');host_match=re.search(r'SUCCESS: (\d+) cases, (\d+) assertions',host);require(host_match is not None,'Host tests incomplete');host_counts=tuple(map(int,host_match.groups()));require(host_counts[0]>=315 and host_counts[1]>=1476,'Existing host coverage lost')
 regressions={}
 for key,(test_class,min_cases,min_assertions) in REGRESSION_TESTS.items():
  output=run(['java','-ea','-cp',hostcp,'app.hiro.oneback.runtime.'+test_class],key+'-regression-tests.txt')
  match=re.search(r'(?m)^PASS '+re.escape(test_class)+r': (\d+) cases, (\d+) assertions$',output)
  require(match is not None,'Release regression tests incomplete: '+key)
  cases,assertions=map(int,match.groups());require(cases>=min_cases and assertions>=min_assertions,'Release regression coverage lost: '+key)
  regressions[key]={'test_class':test_class,'cases':cases,'assertions':assertions,'summary':match.group(0),'result':'PASS'}
 auditcp=os.pathsep.join(map(str,[a.work/'audit',a.work/'patch',a.tools/'morphe.jar']))
 run(['javac','-encoding','UTF-8','-cp',auditcp,'-d',a.work/'audit',a.legacy/'test-dex/DexAudit.java',a.legacy/'test-dex/Fixtures.java',ROOT/'test-dex/HomeBackAudit.java'],'audit-javac.log')
 structure=run(['java','-ea','-cp',auditcp,'HomeBackAudit','selftest',a.work/'fixtures'],'structure-tests.txt');require('PASS selftest:' in structure,'DEX tests incomplete')
 for kind,minapi in [('runtime','29'),('patch','26')]:
  u.write_zip(a.work/(kind+'.jar'),u.class_files(a.work/kind))
  run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api',minapi,'--lib',a.tools/'android.jar','--classpath',a.tools/('android.jar' if kind=='runtime' else 'morphe.jar'),'--output',a.work/(kind+'-dex'),a.work/(kind+'.jar')],kind+'-d8.log')
  require({x.name for x in (a.work/(kind+'-dex')).iterdir()}=={'classes.dex'},'unexpected multidex')
 runtime_raw=(a.work/'runtime-dex/classes.dex').read_bytes()
 for adapter in ('InstagramCurrent','InstagramActivityLayers'):
  require((a.work/'runtime/app/hiro/oneback/runtime'/(adapter+'.class')).is_file(),'Production Android SDK compilation omitted '+adapter)
 fresh=checks.fresh_evidence(runtime_raw,ROOT.parent)
 baseline=u.archive(a.base);require(u.headers(baseline[MF])['Version']==BASE_VERSION,'baseline identity');(a.work/'base.dex').write_bytes(baseline['classes.dex'])
 require(checks.sha(baseline[EXT])==checks.BASE_RUNTIME_SHA,'Baseline249 runtime differs')
 (a.work/'baseline120-runtime.dex').write_bytes(baseline[EXT])
 run(['javac','-encoding','UTF-8','-cp',auditcp,'-d',a.work/'audit',ROOT.parent/'evidence/ProvidedApkHookAudit.java',ROOT.parent/'publication/audit/BaselineRuntimeAbiAudit121.java'],'baseline-abi-javac.log')
 run(['java','-ea','-cp',auditcp,'BaselineRuntimeAbiAudit121',a.work/'baseline120-runtime.dex',a.work/'runtime-dex/classes.dex',a.work/'baseline249-abi.json'],'baseline249-abi.txt')
 fresh_baseline=json.loads((a.work/'baseline249-abi.json').read_text());checks.baseline_abi(fresh_baseline,checks.sha(runtime_raw))
 merge=run(['java','-ea','-cp',auditcp,'HomeBackAudit','merge',a.work/'base.dex',a.work/'patch-dex/classes.dex',a.work/'merged.dex'],'merge-audit.txt')
 require('PASS merge:' in merge,'Loader preservation not established')
 additions={**u.class_files(a.work/'patch'),EXT:(a.work/'runtime-dex/classes.dex').read_bytes()}
 old={k:v for k,v in baseline.items() if k.startswith(PREFIX) or k==EXT};require(old and EXT in old,'Prior OneBack content missing')
 combined={k:v for k,v in baseline.items() if k not in old};combined.update(additions);combined['classes.dex']=(a.work/'merged.dex').read_bytes()
 fields=u.headers(baseline[MF]);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-10-17T00:00:00',Description='Instagram current-screen and shared Back event classification update. Device behavior remains unverified.')
 combined[MF]=u.manifest(fields);sf=dict(fields);sf.update(Name='OneBack Exit No Recents',Version=VERSION)
 single={**additions,'classes.dex':(a.work/'patch-dex/classes.dex').read_bytes(),MF:u.manifest(sf)}
 preserved={k:v for k,v in baseline.items() if k not in old and k not in [MF,'classes.dex']}
 require(all(combined[k]==v for k,v in preserved.items()),'Unrelated bundle entries changed')
 discovery={}
 for name,data in [(SINGLE,single),(BUNDLE,combined)]:
  u.write_zip(a.dist/name,data);require(u.archive(a.dist/name)==data,'Written archive mismatch');discovery[name]={}
  for pkg in PACKAGES:
   listing=run(['java','-jar',a.tools/'morphe.jar','list-patches','--patches',a.dist/name,'-f',pkg,'-p','-v'],name+'.'+pkg+'.listing.txt')
   blocks=re.split(r'(?m)^(?:INFO: )?Index: \d+\s*$',listing);selected=[b for b in blocks if re.search(r'(?m)^Name: '+re.escape(PATCH_NAME)+r'\s*$',b)]
   require(len(selected)==1 and 'Enabled: true' in selected[0],'Loader discovery '+name+'/'+pkg);discovery[name][pkg]=True
 run(['java','-ea','-cp',auditcp,'Fixtures','create',a.work/'fixtures/fixture-original.dex',a.work/'fixtures'],'fixture-apks.txt')
 synthetic=[]
 for name in [SINGLE,BUNDLE]:
  for pkg in PACKAGES:
   out=a.work/'synthetic'/name/pkg;out.mkdir(parents=True)
   apk=out/'patched.apk';report=out/'morphe.json'
   run(['java','-Xmx768m','-jar',a.tools/'morphe.jar','patch',a.work/'fixtures'/(pkg+'.apk'),'-p',a.dist/name,'--exclusive','-e',PATCH_NAME,'--unsigned','--bytecode-mode','FULL','--striplibs','arm64-v8a','-o',apk,'-t',out/'tmp','-r',report],name+'.'+pkg+'.apply.log')
   result=json.loads(report.read_text());require(apk.exists() and not result.get('failedPatches') and result.get('patchingSteps') and all(v.get('success') is True for v in result['patchingSteps']),'Morphe fixture rebuild failure')
   require(result['appliedPatches']==[{'name':PATCH_NAME}],'Unexpected selected patches')
   run(['java','-ea','-cp',auditcp,'Fixtures','verify',a.work/'fixtures'/(pkg+'.apk'),apk,out/'manifest.json'],name+'.'+pkg+'.manifest.txt')
   dex=run(['java','-ea','-cp',auditcp,'HomeBackAudit','audit-app',a.work/'fixtures'/(pkg+'.apk'),apk],name+'.'+pkg+'.dex.txt')
   require('PASS real patcher fixture audit' in dex,'DEX audit not complete');synthetic.append({'bundle':name,'package':pkg,'result':'PASS'})
 fixture,projection_dex=checks.projection_fixture(ROOT.parent)
 native_root=a.work/'native-projection';native_root.mkdir();(native_root/'input.dex').write_bytes(projection_dex)
 run(['java','-ea','-cp',auditcp,'Fixtures','create',native_root/'input.dex',native_root/'fixture-apks'],'native-projection-fixture.txt')
 native_input=native_root/'fixture-apks/com.instagram.android.apk'
 projection={'schema':'oneback121-full-distributed-native-projection-v1','scope':fixture['scope'],'input_dex_sha256':checks.PROJECTION_SHA,'synthetic_namespace_sentinel':fixture['synthetic_namespace_sentinel'],'physical_android_tested':False,'original_apk_apply_tested':False,'other_patches_coapplied':False,'single_bundle_output_dex_identical':False,'outputs':{}}
 for name in [SINGLE,BUNDLE]:
  # Deliberately omit work/patch: the verifier must load the distributed MPP's
  # actual OneBackPatch.class and extension resource, exactly as the CLI does.
  distributed_cp=os.pathsep.join(map(str,[a.work/'audit',a.dist/name,a.tools/'morphe.jar']))
  run(['javac','-encoding','UTF-8','-cp',distributed_cp,'-d',a.work/'audit',ROOT.parent/'publication/audit/PipelineProbe.java'],name+'.native-projection-javac.log')
  admitted=run(['java','-ea','-cp',distributed_cp,'PipelineProbe','validate-projection',native_root/'input.dex'],name+'.native-projection-input.txt')
  require('PASS exact native structural projection:' in admitted and name in admitted,'Distributed projection admission/class origin unverified')
  out=native_root/name;out.mkdir();apk=out/'patched.apk';report=out/'morphe.json'
  run(['java','-Xmx768m','-jar',a.tools/'morphe.jar','patch',native_input,'-p',a.dist/name,'--exclusive','-e',PATCH_NAME,'--unsigned','--bytecode-mode','FULL','--striplibs','arm64-v8a','-o',apk,'-t',out/'tmp','-r',report],name+'.native-projection-apply.log')
  result=json.loads(report.read_text());require(apk.is_file() and not result.get('failedPatches') and result.get('appliedPatches')==[{'name':PATCH_NAME}] and result.get('patchingSteps') and all(v.get('success') is True for v in result['patchingSteps']),'Full distributed native projection apply failed')
  verified=run(['java','-Xmx768m','-ea','-cp',distributed_cp,'PipelineProbe','verify',native_input,apk],name+'.native-projection-output.txt')
  match=re.search(r'PASS distributedExtensionClassesByteExact=(\d+)',verified)
  require(match is not None and verified.count('exactProvidedBodyRestored=true')==2 and 'PASS unchangedSyntheticNamespaceSentinel=' in verified,'Both emitted native guards, canonical distributed runtime and unchanged synthetic sentinel were not verified')
  with zipfile.ZipFile(apk) as zipped:emitted=zipped.read('classes.dex')
  projection['outputs'][name]={'result':'PASS','mpp_sha256':sha(a.dist/name),'native_reels_hooks':1,'native_main_hooks':1,'canonical_runtime_classes':int(match[1]),'provided_native_bodies_restored_exactly':2,'synthetic_namespace_sentinel_preserved':True,'actual_distributed_loader':True,'emitted_dex_sha256':checks.sha(emitted)}
 projection['single_bundle_output_dex_identical']=projection['outputs'][SINGLE]['emitted_dex_sha256']==projection['outputs'][BUNDLE]['emitted_dex_sha256']
 require(projection['single_bundle_output_dex_identical'],'Single and bundle emitted native projection DEX differ')
 evidence=checks.historical_evidence(ROOT.parent)
 require(evidence,'Historical provided-APK audit records must be retained')
 require(validation_sources==checks.source_digests(ROOT.parent),'Validation source changed during build; repeat from frozen sources')
 require(source_snapshot=={str(x.relative_to(ROOT)):sha(x) for x in sorted(ROOT.rglob('*')) if x.is_file() and x.suffix in ('.java','.py')},'Source changed during compilation/validation; rebuild final sources')
 qa={'schema':'homeback121-qa-v1','patch_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'physical_android_tested':False,'third_party_apk_tested':False,'provided_apk_inputs_already_patched':True,'historical_provided_apk_static_audit_evidence':evidence,'fresh_provided_apk_static_audit_evidence':fresh,'fresh_baseline_runtime_abi':fresh_baseline,'distributed_native_projection':projection,'production_android_sdk_compile':{'result':'PASS','android_jar_sha256':TOOLS['android.jar'],'test_stubs_on_production_classpath':False,'required_adapters':['InstagramCurrent','InstagramActivityLayers'],'reflection_contracts_validated_by_sdk_compiler':False},'jdk_release':'21.0.8+9.0.LTS','validation_sources':validation_sources,'host_counts':{'cases':host_counts[0],'assertions':host_counts[1]},'regressions':regressions,'home_recognition':'Instagram current-screen and shared Back event classification; uploaded APK static identity, host and structural projection checks do not establish Android device behavior.','host':host.strip().splitlines()[-1],'dex_structure':structure.strip().splitlines()[-1],'loader_preservation':merge.strip(),'preserved_unrelated_entries':len(preserved),'discovery':discovery,'synthetic':synthetic,'artifacts':{n:{'bytes':(a.dist/n).stat().st_size,'sha256':sha(a.dist/n)} for n in [SINGLE,BUNDLE]},'toolchain_sha256':TOOLS,'sources':source_snapshot}
 checks.pipeline_result(qa,runtime_raw,qa['artifacts'])
 (a.dist/'QA_HomeBack_v1.1.11.json').write_bytes(u.json_bytes(qa));(a.dist/'SHA256SUMS.txt').write_text(''.join(sha(f)+'  '+f.name+'\n' for f in sorted(a.dist.iterdir()) if f.is_file() and f.name!='SHA256SUMS.txt'))
 print('PASS BUILD '+json.dumps(qa['artifacts']),flush=True)
if __name__=='__main__':main()
