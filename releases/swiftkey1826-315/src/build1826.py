#!/usr/bin/env python3
"""Rebuild the reviewed sticky Shift and related emoji update from pinned MPPs."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, subprocess, sys, time, urllib.request, zipfile
from pathlib import Path
sys.dont_write_bytecode=True
ROOT=Path(__file__).resolve().parent
OLD_SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.25.mpp'
OLD_BUNDLE='Hiro_Morphe_Patches_v1.0.314.mpp'
SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.26.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.315.mpp'
QA_NAME='QA_SwiftKey_v1.8.26.json'
SOURCE_ZIP='SwiftKeyBeta_v1.8.26_sources_and_QA.zip'
MF,EXT,EMOJI='META-INF/MANIFEST.MF','extensions/swiftkey_japanese.mpe','swiftkey/emoji_ja.tsv'
JVM='app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
SHIFT_JVM='app/hiro/swiftkey/patches/ShiftLockPatch.class'
STAMP='2026-10-29T00:00:00'
DESCRIPTION='Shift一回で次に押すまで大文字を維持。挨拶・気持ち・日常語からの絵文字候補を拡充。既存の日本語辞書・入力操作を保持。実機未確認。'
RUNTIME_SHA='94e7af5056df941fc935ff8876663fe5003584ab0e38f8e71f9c8d195f907755'
PINS={OLD_SINGLE:'b6cfb2362dffe42468c8ba795dabd801153b9ed10581b10ba4fab438518165ab',OLD_BUNDLE:'cdc309a2b8a4b4f22cfec05a63d73d53089a6e0d9950ed6a835f457ca8541b5a',
'apktool.jar':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423','morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
'morphe-current.jar':'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573','r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'}
BASE='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/8a7eeea4821b7bd14f55aa32e1c8d8c36f0f3f91/downloads/'
URLS={OLD_SINGLE:BASE+OLD_SINGLE,OLD_BUNDLE:BASE+OLD_BUNDLE,
'apktool.jar':'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
'morphe-current.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar',
'r8.jar':'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar','android.jar':'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar'}
def sha(raw):return hashlib.sha256(raw).hexdigest()
def require(ok,message):
 if not ok:raise RuntimeError(message)
def run(cmd,log):
 result=subprocess.run(list(map(str,cmd)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 log.parent.mkdir(parents=True,exist_ok=True);log.write_text(result.stdout)
 print(result.stdout,end='',flush=True);require(result.returncode==0,'Command failed; see '+str(log))
 return result.stdout
def report(path):
 data=json.loads(path.read_text());require(data.get('status')=='PASSED','Component verification failed: '+str(path));return data
def normalize(data):
 if isinstance(data,dict):return {k:normalize(v) for k,v in data.items() if k not in ('elapsed_ms','elapsed_seconds')}
 if isinstance(data,list):return [normalize(v) for v in data]
 return data
def archive(path):
 with zipfile.ZipFile(path) as z:
  require(len(z.namelist())==len(set(z.namelist())) and z.testzip() is None,'Invalid ZIP '+str(path));return {n:z.read(n) for n in z.namelist()}
def manifest(raw):
 rows=[]
 for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
  if line.startswith(b' '):require(bool(rows),'Malformed manifest');rows[-1]+=line[1:]
  elif line:rows.append(line)
 pairs=[line.decode().split(': ',1) for line in rows];require(all(len(p)==2 for p in pairs) and len(dict(pairs))==len(pairs),'Manifest keys invalid');return dict(pairs)
def manifest_bytes(values):
 lines=[]
 for key,value in values.items():
  line=b''
  for c in key+': '+value:
   raw=c.encode()
   if len(line)+len(raw)>70:lines.append(line);line=b' '
   line+=raw
  lines.append(line)
 result=b'\r\n'.join(lines)+b'\r\n\r\n';require(manifest(result)==values,'Manifest roundtrip');return result
def zip_add(z,name,raw):
 i=zipfile.ZipInfo(name,(2026,10,29,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,raw,compresslevel=9)
def rewrite(source,target,updates):
 with zipfile.ZipFile(source) as old,zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as new:
  for info in old.infolist():new.writestr(copy.copy(info),updates.get(info.filename,old.read(info)))
  for name in sorted(set(updates)-set(old.namelist())):zip_add(new,name,updates[name])
def download(inputs,tools):
 for name,pin in PINS.items():
  path=(inputs if name.endswith('.mpp') else tools)/name
  if path.exists():require(sha(path.read_bytes())==pin,'Input pin differs: '+name);continue
  path.parent.mkdir(parents=True,exist_ok=True)
  for attempt in range(3):
   try:
    raw=urllib.request.urlopen(URLS[name],timeout=120).read();require(sha(raw)==pin,'Downloaded pin differs: '+name);path.write_bytes(raw);break
   except Exception:
    if attempt==2:raise
    time.sleep(2*(attempt+1))
def source_paths():
 return sorted(p for p in ROOT.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix in ('.py','.java','.json','.txt','.md','.yml','.yaml','.smali','.tsv'))
def package(source,target,updates,old_version,version):
 before=archive(source);headers=manifest(before[MF]);require(headers['Version']==old_version,'Baseline version differs')
 headers.update(Version=version,Timestamp=STAMP,Description=DESCRIPTION)
 updates={**updates,MF:manifest_bytes(headers)};rewrite(source,target,updates);after=archive(target)
 require(set(before).issubset(after),'Archive entry removed')
 changed=sorted(n for n in before if before[n]!=after[n]);added=sorted(set(after)-set(before))
 require(set(changed)|set(added)==set(updates),'Unexpected archive change')
 require(set(changed)=={MF,EMOJI,'classes.dex',JVM} and set(added)=={SHIFT_JVM},'Changes exceed approved scope')
 require(after[EXT]==before[EXT] and sha(after[EXT])==RUNTIME_SHA,'Runtime changed')
 require(all(after[n]==before[n] for n in before if n.startswith('swiftkey/') and n!=EMOJI),'Non-emoji dictionary changed')
 return {'changed_entries':changed,'added_entries':added,'unchanged_entries':len(before)-len(changed),'bytes':target.stat().st_size,'sha256':sha(target.read_bytes()),'extension_sha256':RUNTIME_SHA,'all_other_entries_byte_identical':True}
def build(inputs,tools,work,output):
 for name,pin in PINS.items():require(sha(((inputs if name.endswith('.mpp') else tools)/name).read_bytes())==pin,'Pinned input differs: '+name)
 require(not work.exists(),'Fresh build work directory required')
 work.mkdir(parents=True);output.mkdir(parents=True,exist_ok=True)
 old_s,old_b=archive(inputs/OLD_SINGLE),archive(inputs/OLD_BUNDLE)
 require(old_s[EXT]==old_b[EXT] and sha(old_s[EXT])==RUNTIME_SHA,'Runtime pin differs')
 for name in old_s:
  if name.startswith('swiftkey/') or name.startswith('app/hiro/swiftkey/'):
   require(old_s[name]==old_b[name],'Standalone/bundle differ: '+name)
 (work/'old-emoji.tsv').write_bytes(old_s[EMOJI]);(work/'baseline.mpe').write_bytes(old_s[EXT])
 # Components enforce exact source parity and native target fingerprints themselves.
 run(['python',ROOT/'emoji/build_emoji.py','--baseline',work/'old-emoji.tsv','--output',work/'emoji','--tools',tools,'--runtime',work/'baseline.mpe'],work/'emoji.log')
 emoji=normalize(report(work/'emoji/emoji-report.json'));emoji_path=work/'emoji/emoji_ja.tsv'
 from publish1826 import validate_dictionary
 dictionary_summary=validate_dictionary(old_s[EMOJI],emoji_path.read_bytes())
 require(emoji.get('host_tests_passed') is True and emoji.get('baseline_java_matches_published_dex') is True and emoji.get('runtime_sha256')==RUNTIME_SHA,'Emoji tests/source parity missing')
 run(['python',ROOT/'loader/build_loaders.py','--single',inputs/OLD_SINGLE,'--bundle',inputs/OLD_BUNDLE,'--emoji',emoji_path,'--tools',tools,'--work',work/'emoji-loader-work','--output',work/'emoji-loader'],work/'emoji-loader.log')
 loader=normalize(report(work/'emoji-loader/loader-report.json'))
 staged={}
 for kind,name in [('single',OLD_SINGLE),('bundle',OLD_BUNDLE)]:
  staged[kind]=work/(kind+'-emoji.mpp')
  rewrite(inputs/name,staged[kind],{JVM:(work/'emoji-loader/JapaneseSamsungEmojiPatch.class').read_bytes(),'classes.dex':(work/'emoji-loader'/(kind+'-classes.dex')).read_bytes(),EMOJI:emoji_path.read_bytes()})
 run(['python',ROOT/'shift/build_shift_loader.py','--single',staged['single'],'--bundle',staged['bundle'],'--tools',tools,'--work',work/'shift-work','--output',work/'shift'],work/'shift.log')
 shift=normalize(report(work/'shift/shift-report.json'))
 common={EMOJI:emoji_path.read_bytes()}
 # Shift component returns final JVM overlay tree, including the emoji digest update.
 for p in sorted((work/'shift/jvm').rglob('*.class')):common[p.relative_to(work/'shift/jvm').as_posix()]=p.read_bytes()
 require(set(common)=={EMOJI,JVM,SHIFT_JVM},'Unexpected Shift JVM overlay')
 results={}
 for kind,old,new,oldversion,version in [('single',OLD_SINGLE,SINGLE,'1.8.25','1.8.26'),('bundle',OLD_BUNDLE,BUNDLE,'1.0.314','1.0.315')]:
  results[kind]=package(inputs/old,output/new,{**common,'classes.dex':(work/'shift'/(kind+'-classes.dex')).read_bytes()},oldversion,version)
 new_s,new_b=archive(output/SINGLE),archive(output/BUNDLE)
 require(all(new_s[n]==new_b[n] for n in common),'Final standalone/bundle overlays differ')
 cls=work/'loader-test';cls.mkdir()
 run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-d',cls,ROOT/'tests/LoaderParityProbe.java'],work/'javac-loader.log')
 run(['java','com.sun.tools.javac.Main','-d',cls,ROOT/'loader/LoaderResourceProbe.java'],work/'javac-resources.log')
 final_resources=[]
 for kind,name in [('single',SINGLE),('bundle',BUNDLE)]:
  for sdk in ['morphe.jar','morphe-current.jar']:
   dest=work/(kind+'-'+sdk+'.resources.json')
   run([os.environ.get('MORPHE_JAVA','java'),'-Xmx1536m','-cp',str(cls)+os.pathsep+str(tools/sdk),'LoaderResourceProbe',output/name,dest],work/(kind+'-'+sdk+'.resources.log'))
   resource=report(dest)
   require(resource['mpp_sha256']==sha((output/name).read_bytes()) and resource['all_eight_resource_hashes_verified'] is True,'Final resource verification not bound to artifact')
   final_resources.append({'kind':kind,'sdk':sdk,**resource})
 loaders=[]
 for sdk in ['morphe.jar','morphe-current.jar']:
  dest=work/(sdk+'.loader.json')
  run([os.environ.get('MORPHE_JAVA','java'),'-Xmx1536m','-cp',str(cls)+os.pathsep+str(tools/sdk),'LoaderParityProbe',inputs/OLD_BUNDLE,output/SINGLE,output/BUNDLE,dest],work/(sdk+'.loader.log'))
  loaders.append(report(dest))
 require(loaders[0]==loaders[1],'Morphe loaders disagree')
 require(loaders[0]['selected_extension_sha256']==RUNTIME_SHA,'Loader extension differs')
 qa={'status':'SHIFT_EMOJI_HOST_AND_NATIVE_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.26','bundle_version':'1.0.315','baseline_swiftkey_version':'1.8.25','baseline_bundle_version':'1.0.314','target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,'device_tested':False,'original_apk_apply_tested':False,'runtime_byte_identical':True,'runtime_sha256':RUNTIME_SHA,'all_non_emoji_dictionary_resources_preserved':True,'other_apps_byte_identical':True,'unrelated_loader_entries_byte_identical':True,'loader_changes_verified':True,'standalone_and_bundle_runtime_equal':True,'host_tests_passed':True,'native_hook_verified':True,'two_loader_sdks_agree':True,'host_report':{'status':'PASSED','runtime_sha256':RUNTIME_SHA,'dictionary_sha256':sha(emoji_path.read_bytes()),'shift':shift,'emoji':emoji},'emoji_dictionary':dictionary_summary,'emoji_loader_report':loader,'native_hook_report':shift,'loader_reports':loaders,'final_resource_reports':final_resources,'single':results['single'],'bundle':results['bundle'],'inputs':{name:{'url':URLS[name],'sha256':pin} for name,pin in PINS.items()},'source_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in source_paths()}}
 (output/QA_NAME).write_text(json.dumps(qa,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
 (output/'RELEASE_NOTES.txt').write_bytes((ROOT/'RELEASE_NOTES.txt').read_bytes())
 with zipfile.ZipFile(output/SOURCE_ZIP,'w') as z:
  for p in source_paths():zip_add(z,'src/'+p.relative_to(ROOT).as_posix(),p.read_bytes())
  zip_add(z,QA_NAME,(output/QA_NAME).read_bytes())
 names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
 (output/'SHA256SUMS.txt').write_text(''.join(sha((output/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS SwiftKey 1.8.26 / bundle 1.0.315',flush=True)
 return qa
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('input-dir','tools','work','output'):p.add_argument('--'+name,type=Path,required=True)
 p.add_argument('--download',action='store_true');a=p.parse_args();inputs,tools,work,output=(getattr(a,n).resolve() for n in ('input_dir','tools','work','output'))
 if a.download:download(inputs,tools)
 build(inputs,tools,work,output)
