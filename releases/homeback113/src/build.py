#!/usr/bin/env python3
"""Reproducible home-only back MPP build; no private app/APK/device claims."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess, zipfile
ROOT=Path(__file__).resolve().parent
BASE_VERSION='1.0.237';VERSION='1.1.3';BUNDLE_VERSION='1.0.238'
BASE_SHA='7912105b6bea8a0ef0ded831071982a691514dc326dd21e665a18750d62dab5f'
TOOLS={'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
PREFIX='app/hiro/oneback/patches/';EXT='extensions/oneback_exit.mpe';MF='META-INF/MANIFEST.MF'
PATCH_NAME='戻る1回で終了・履歴削除'
PACKAGES=['com.twitter.android','com.instagram.android','com.zhiliaoapp.musically','com.ss.android.ugc.trill','ctrip.english']
BUNDLE='Hiro_Morphe_Patches_v1.0.238.mpp';SINGLE='OneBack_Exit_NoRecents_v1.1.3.mpp'
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def require(ok,why):
 if not ok:raise RuntimeError(why)
def main():
 p=argparse.ArgumentParser();p.add_argument('--base',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--legacy',type=Path,default=ROOT.parents[1]/'oneback163'/'src');p.add_argument('--work',type=Path,required=True);p.add_argument('--dist',type=Path,required=True);a=p.parse_args()
 spec=importlib.util.spec_from_file_location('base_util',a.legacy/'build.py');u=importlib.util.module_from_spec(spec);spec.loader.exec_module(u)
 require(sha(a.base)==BASE_SHA,'Current baseline must match reviewed bundle237');
 for name,digest in TOOLS.items():require(sha(a.tools/name)==digest,'Pinned tool mismatch '+name)
 require(not a.work.exists(),'Use a fresh work directory');a.work.mkdir(parents=True);a.dist.mkdir(parents=True,exist_ok=True)
 def run(cmd,name):
  log=a.work/name;print('RUN '+name,flush=True)
  with log.open('w') as out:r=subprocess.run(list(map(str,cmd)),stdout=out,stderr=subprocess.STDOUT,timeout=180,check=False)
  text=log.read_text();require(r.returncode==0,'Command failed '+str(log)+'\n'+text[-8000:]);return text
 for d in ['runtime','patch','test','audit','runtime-dex','patch-dex','fixtures','synthetic']:(a.work/d).mkdir()
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'android.jar','-d',a.work/'runtime',*sorted((ROOT/'runtime').glob('*.java'))],'runtime-javac.log')
 run(['javac','--release','8','-encoding','UTF-8','-cp',a.tools/'morphe.jar','-d',a.work/'patch',*sorted((ROOT/'patch').glob('*.java'))],'patch-javac.log')
 hostcp=os.pathsep.join(map(str,[a.work/'test',a.work/'runtime',a.tools/'android.jar']))
 run(['javac','--release','8','-encoding','UTF-8','-cp',hostcp,'-d',a.work/'test',*sorted((ROOT/'test-stubs').rglob('*.java')),*sorted((ROOT/'test-runtime').glob('*.java'))],'host-javac.log')
 host=run(['java','-ea','-cp',hostcp,'app.hiro.oneback.runtime.HomeBackHostTest'],'host-tests.txt');require('SUCCESS:' in host,'Host tests incomplete')
 auditcp=os.pathsep.join(map(str,[a.work/'audit',a.work/'patch',a.tools/'morphe.jar']))
 run(['javac','-encoding','UTF-8','-cp',auditcp,'-d',a.work/'audit',a.legacy/'test-dex/DexAudit.java',a.legacy/'test-dex/Fixtures.java',ROOT/'test-dex/HomeBackAudit.java'],'audit-javac.log')
 structure=run(['java','-ea','-cp',auditcp,'HomeBackAudit','selftest',a.work/'fixtures'],'structure-tests.txt');require('PASS selftest:' in structure,'DEX tests incomplete')
 for kind,minapi in [('runtime','29'),('patch','26')]:
  u.write_zip(a.work/(kind+'.jar'),u.class_files(a.work/kind))
  run(['java','-cp',a.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api',minapi,'--lib',a.tools/'android.jar','--classpath',a.tools/('android.jar' if kind=='runtime' else 'morphe.jar'),'--output',a.work/(kind+'-dex'),a.work/(kind+'.jar')],kind+'-d8.log')
  require({x.name for x in (a.work/(kind+'-dex')).iterdir()}=={'classes.dex'},'unexpected multidex')
 baseline=u.archive(a.base);require(u.headers(baseline[MF])['Version']==BASE_VERSION,'baseline identity');(a.work/'base.dex').write_bytes(baseline['classes.dex'])
 merge=run(['java','-ea','-cp',auditcp,'HomeBackAudit','merge',a.work/'base.dex',a.work/'patch-dex/classes.dex',a.work/'merged.dex'],'merge-audit.txt')
 require('PASS merge:' in merge,'Loader preservation not established')
 additions={**u.class_files(a.work/'patch'),EXT:(a.work/'runtime-dex/classes.dex').read_bytes()}
 old={k:v for k,v in baseline.items() if k.startswith(PREFIX) or k==EXT};require(old and EXT in old,'Prior OneBack content missing')
 combined={k:v for k,v in baseline.items() if k not in old};combined.update(additions);combined['classes.dex']=(a.work/'merged.dex').read_bytes()
 fields=u.headers(baseline[MF]);fields.update(Version=BUNDLE_VERSION,Timestamp='2026-10-15T00:00:00',Description='TikTok inbox/profile Back navigates Home; Trip delayed native Back/stop removes Recents with bounded retries. Instagram Reels preserved. Physical Android not tested.')
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
 qa={'schema':'homeback113-qa-v1','patch_version':VERSION,'bundle_version':BUNDLE_VERSION,'baseline_version':BASE_VERSION,'baseline_sha256':BASE_SHA,'physical_android_tested':False,'third_party_apk_tested':False,'home_recognition':'TikTok selected inbox/profile root tabs click Home, next distinct Back exits; Instagram Reels preserved; Trip public Back observer, successful native return bounded async token and stop cleanup; input/leave/config/reopen guards; task-ID and package-scoped retries','host':host.strip().splitlines()[-1],'dex_structure':structure.strip().splitlines()[-1],'loader_preservation':merge.strip(),'preserved_unrelated_entries':len(preserved),'discovery':discovery,'synthetic':synthetic,'artifacts':{n:{'bytes':(a.dist/n).stat().st_size,'sha256':sha(a.dist/n)} for n in [SINGLE,BUNDLE]},'toolchain_sha256':TOOLS,'sources':{str(x.relative_to(ROOT)):sha(x) for x in sorted(ROOT.rglob('*')) if x.is_file() and x.suffix in ['.java','.py']}}
 (a.dist/'QA_HomeBack_v1.1.3.json').write_bytes(u.json_bytes(qa));(a.dist/'SHA256SUMS.txt').write_text(''.join(sha(f)+'  '+f.name+'\n' for f in sorted(a.dist.iterdir()) if f.is_file() and f.name!='SHA256SUMS.txt'))
 print('PASS BUILD '+json.dumps(qa['artifacts']),flush=True)
if __name__=='__main__':main()
