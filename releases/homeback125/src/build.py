#!/usr/bin/env python3
"""SDK build, host regressions, DEX preservation and real Morphe fixture pipeline.
Physical Android behavior is not asserted. Private APK validation is a separate
local run; no user APK is uploaded to CI or included in the distribution.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,re,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parent
BASE_VERSION='1.0.258'; VERSION='1.1.15'; BUNDLE_VERSION='1.0.259'
BASE_SHA='819ac9522cbe163acbb95839915ccf503618cbc95a528ed0892ed727732eea5d'
TOOLS={'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
PREFIX='app/hiro/oneback/patches/'; EXT='extensions/oneback_exit.mpe'; MF='META-INF/MANIFEST.MF'
PATCH_NAME='戻る1回で終了・履歴削除'
PACKAGES=['com.twitter.android','com.instagram.android','com.zhiliaoapp.musically','com.ss.android.ugc.trill','ctrip.english']
BUNDLE='Hiro_Morphe_Patches_v1.0.259.mpp'; SINGLE='OneBack_Exit_NoRecents_v1.1.15.mpp'
UPGRADE_PINS={
 '120':'d614e7ba8501af6ee65b3b237cf75034d3035a46c3556ef23365363b25955fc2',
 '121':'52e1817fdaa16591cb5eb7b0383aaa07f578a4a6799dad1cf5ef6e99d9a817d5',
 '122':'e23a94da3f45accb98d52f3ed3ebbbdf7e4238b7fb6a30cb950199794c1c012b',
 '123':'cea6c6519bd9c6c70776cddd3e6a26f28709a77d0fed7a8335a7baad7be28a0e',
 '124':'559d89a6e7c3008cfb14b1e2c5e46285cf3a48365725d87524195a466c02b07f',
}
REQUIRED_HOST_TESTS={
 'Back116Regression','Back117InstagramTest','Back117TikTokTest','Back118InstagramGuardTest','Back118InstagramTest',
 'Back119InstagramTest','Back119TwitterTest','Back120InstagramTest','Back120MainDispatchTest',
 'Back121InstagramActivityLayersTest','Back121InstagramCurrentTest','Back121InstagramDispatchTest','Back121InstagramScopeTest',
 'Back122InstagramDispatcherTest','Back123InstagramTabBackTest','Back123NativeReelsTest','Back124TwitterTest','Back125TwitterKeyTest','Back125TwitterSystemTest','Back125TwitterTerminalTest','HomeBackHostTest',
}
def owned_entry(name):
 return name.startswith(PREFIX) or name==EXT or re.fullmatch(r'upgrades/oneback-runtime[0-9]+\.dex',name) is not None
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def require(ok,why):
 if not ok:raise RuntimeError(why)
def main():
 p=argparse.ArgumentParser();p.add_argument('--base',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--legacy',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--dist',type=Path,required=True);a=p.parse_args()
 a.base=a.base.resolve();a.tools=a.tools.resolve();a.legacy=a.legacy.resolve();a.work=a.work.resolve();a.dist=a.dist.resolve()
 spec=importlib.util.spec_from_file_location('base_util',a.legacy/'build.py');u=importlib.util.module_from_spec(spec);spec.loader.exec_module(u)
 require(sha(a.base)==BASE_SHA,'Active bundle258 baseline hash differs')
 for name,digest in TOOLS.items():require(sha(a.tools/name)==digest,'Pinned tool mismatch '+name)
 require(not a.work.exists(),'Use fresh work directory');a.work.mkdir(parents=True)
 require(not a.dist.exists() or not any(a.dist.iterdir()),'Use empty output directory');a.dist.mkdir(parents=True,exist_ok=True)
 def run(cmd,name,timeout=300):
  print('RUN '+name,flush=True)
  with (a.work/name).open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT,timeout=timeout)
  s=(a.work/name).read_text();require(r.returncode==0,'Command failed '+str(a.work/name)+'\n'+s[-9000:]);return s
 require('Temurin-21.0.8+9' in run(['java','--version'],'java.txt'),'Pinned JDK required')
 require(run(['javac','-version'],'javac.txt').strip()=='javac 21.0.8','Pinned javac required')
 sources={str(x.relative_to(ROOT)):sha(x) for x in sorted(ROOT.rglob('*')) if x.is_file() and x.suffix in ('.java','.py','.dex')}
 for d in ['runtime','patch','test','audit','runtime-dex','patch-dex','fixtures','synthetic']:(a.work/d).mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',a.work/'runtime',*sorted((ROOT/'runtime').glob('*.java'))],'runtime-javac.log')
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'morphe.jar','-d',a.work/'patch',*sorted((ROOT/'patch').glob('*.java'))],'patch-javac.log')
 hostcp=os.pathsep.join(map(str,[a.work/'test',a.work/'runtime',a.tools/'android.jar']))
 run(['javac','--release','8','-encoding','UTF-8','-cp',hostcp,'-d',a.work/'test',*sorted((ROOT/'test-stubs').rglob('*.java')),*sorted((ROOT/'test-runtime').glob('*.java'))],'host-javac.log')
 require(REQUIRED_HOST_TESTS.issubset({p.stem for p in (ROOT/'test-runtime').glob('*.java')}),'Required prior/new host regression source absent')
 tests={}
 for source in sorted((ROOT/'test-runtime').glob('*.java')):
  if 'public static void main' not in source.read_text():continue
  result=run(['java','-ea','-cp',hostcp,'app.hiro.oneback.runtime.'+source.stem],source.stem+'.txt')
  require('PASS' in result or 'SUCCESS:' in result or re.search(r'REGRESSION: [0-9]+ cases, 0 failed',result),'No success summary '+source.name)
  tests[source.stem]=result.strip().splitlines()[-1]
 require(REQUIRED_HOST_TESTS.issubset(tests),'Required host regressions were not run')
 auditcp=os.pathsep.join(map(str,[a.work/'audit',a.work/'patch',a.tools/'morphe.jar']))
 run(['javac','-encoding','UTF-8','-cp',auditcp,'-d',a.work/'audit',a.legacy/'test-dex/DexAudit.java',a.legacy/'test-dex/Fixtures.java',*sorted((ROOT/'test-dex').glob('*.java'))],'audit-javac.log')
 structure=run(['java','-ea','-cp',auditcp,'HomeBackAudit','selftest',a.work/'fixtures'],'structure.txt')
 for kind,minapi in [('runtime','29'),('patch','26')]:
  u.write_zip(a.work/(kind+'.jar'),u.class_files(a.work/kind))
  run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api',minapi,'--lib',a.tools/'android.jar','--classpath',a.tools/('android.jar' if kind=='runtime' else 'morphe.jar'),'--output',a.work/(kind+'-dex'),a.work/(kind+'.jar')],kind+'-d8.log')
  require({x.name for x in (a.work/(kind+'-dex')).iterdir()}=={'classes.dex'},'Unexpected multidex')
 baseline=u.archive(a.base);require(u.headers(baseline[MF])['Version']==BASE_VERSION,'Baseline version differs')
 (a.work/'base.dex').write_bytes(baseline['classes.dex'])
 merged=run(['java','-ea','-cp',auditcp,'HomeBackAudit','merge',a.work/'base.dex',a.work/'patch-dex/classes.dex',a.work/'merged.dex'],'merge.txt')
 assets={str(x.relative_to(ROOT/'assets')):x.read_bytes() for x in sorted((ROOT/'assets').rglob('*')) if x.is_file()}
 for version,digest in UPGRADE_PINS.items():
  name='upgrades/oneback-runtime'+version+'.dex'
  require(name in assets and hashlib.sha256(assets[name]).hexdigest()==digest,'Pinned upgrade reference absent or changed: '+name)
 require(assets['upgrades/oneback-runtime124.dex']==baseline[EXT],'Reference124 must be the exact active baseline runtime')
 prior_assets={k:v for k,v in baseline.items() if re.fullmatch(r'upgrades/oneback-runtime[0-9]+\.dex',k)}
 require(all(assets.get(k)==v for k,v in prior_assets.items()),'Existing owned upgrade reference removed or changed')
 additions={**u.class_files(a.work/'patch'),**assets,EXT:(a.work/'runtime-dex/classes.dex').read_bytes()}
 old={k:v for k,v in baseline.items() if owned_entry(k)}
 combined={k:v for k,v in baseline.items() if k not in old};combined.update(additions);combined['classes.dex']=(a.work/'merged.dex').read_bytes()
 fields=u.headers(baseline[MF]);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-10-19T00:00:00',Description='X key and system Back reach native four-tab exit and recent-task removal; next ordinary launch selects Home. Instagram123 behavior retained. Physical device not tested.')
 combined[MF]=u.manifest(fields);sf=dict(fields);sf.update(Name='OneBack Exit No Recents',Version=VERSION)
 single={**additions,'classes.dex':(a.work/'patch-dex/classes.dex').read_bytes(),MF:u.manifest(sf)}
 preserved={k:v for k,v in baseline.items() if k not in old and k not in [MF,'classes.dex']}
 require(all(combined[k]==v for k,v in preserved.items()),'Unrelated resource changed')
 for name,data in [(SINGLE,single),(BUNDLE,combined)]:
  u.write_zip(a.dist/name,data);require(u.archive(a.dist/name)==data,'Archive verification failed')
  listing=run(['java','-jar',a.tools/'morphe.jar','list-patches','--patches',a.dist/name,'-f','com.instagram.android','-p','-v'],name+'.listing.txt')
  require(listing.count('Name: '+PATCH_NAME+'\n')==1,'Patch not discovered exactly once')
 run(['java','-ea','-cp',auditcp,'Fixtures','create',a.work/'fixtures/fixture-original.dex',a.work/'fixtures'],'fixture-apks.txt')
 synthetic=[]
 for name in [SINGLE,BUNDLE]:
  for pkg in PACKAGES:
   out=a.work/'synthetic'/name/pkg;out.mkdir(parents=True);apk=out/'patched.apk';report=out/'morphe.json'
   run(['java','-Xmx768m','-jar',a.tools/'morphe.jar','patch',a.work/'fixtures'/(pkg+'.apk'),'-p',a.dist/name,'--exclusive','-e',PATCH_NAME,'--unsigned','--bytecode-mode','FULL','--striplibs','arm64-v8a','-o',apk,'-t',out/'tmp','-r',report],name+'.'+pkg+'.apply.log')
   result=json.loads(report.read_text());require(apk.exists() and not result.get('failedPatches') and result.get('appliedPatches')==[{'name':PATCH_NAME}] and result.get('patchingSteps') and all(v.get('success') is True for v in result['patchingSteps']),'Synthetic full pipeline failed')
   run(['java','-ea','-cp',auditcp,'Fixtures','verify',a.work/'fixtures'/(pkg+'.apk'),apk,out/'manifest.json'],name+'.'+pkg+'.manifest.txt')
   run(['java','-ea','-cp',auditcp,'HomeBackAudit','audit-app',a.work/'fixtures'/(pkg+'.apk'),apk],name+'.'+pkg+'.dex.txt')
   synthetic.append({'bundle':name,'package':pkg,'result':'PASS'})
 require(sources=={str(x.relative_to(ROOT)):sha(x) for x in sorted(ROOT.rglob('*')) if x.is_file() and x.suffix in ('.java','.py','.dex')},'Source changed during build')
 qa={'schema':'homeback125-qa-v1','patch_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'physical_android_tested':False,'production_android_sdk_compile':True,'host':tests,'dex_structure':structure.strip().splitlines()[-1],'loader_preservation':merged.strip(),'preserved_unrelated_entries':len(preserved),'synthetic':synthetic,'artifacts':{n:{'bytes':(a.dist/n).stat().st_size,'sha256':sha(a.dist/n)} for n in [SINGLE,BUNDLE]},'toolchain_sha256':TOOLS,'sources':sources,'runtime_sha256':sha(a.work/'runtime-dex/classes.dex')}
 (a.dist/'QA_HomeBack_v1.1.15.json').write_bytes(u.json_bytes(qa))
 print('PASS BUILD '+json.dumps(qa['artifacts']),flush=True)
if __name__=='__main__':main()
