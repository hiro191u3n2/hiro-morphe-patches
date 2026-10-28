#!/usr/bin/env python3
"""Reproduce pinned dictionary union, verify runtime, and package existing patches."""
from __future__ import annotations
import argparse, copy, hashlib, importlib.util, json, os, shutil, subprocess, sys, urllib.request, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
MF='META-INF/MANIFEST.MF'; EXT='extensions/swiftkey_japanese.mpe'
OLD_SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.24.mpp'
OLD_BUNDLE='Hiro_Morphe_Patches_v1.0.313.mpp'
SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.25.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.314.mpp'
QA_NAME='QA_SwiftKey_v1.8.25.json'
SOURCE_ZIP='SwiftKeyBeta_v1.8.25_sources_and_QA.zip'
STAMP='2026-10-29T00:00:00'
BASE_RUNTIME='494565fcceffcdbcb6df0e5885cf537bd5e7b8ffa6d53f4b78f603b3ef38ca55'
PINS={OLD_SINGLE:'aa119501a94938f3d26b53802eed3f1a010b70a4d49dfb79531658855184e301',
 OLD_BUNDLE:'49ddb5c310f20d94bea430198e75a6a64bd9a76af5b7f0cbb2c2efec03709f22',
 'apktool.jar':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
 'morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
 'morphe-current.jar':'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
 'r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'}
URLS={OLD_SINGLE:'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/d27333305aad31473096a41cba0fc4d6a18ce131/downloads/'+OLD_SINGLE,
 OLD_BUNDLE:'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/d27333305aad31473096a41cba0fc4d6a18ce131/downloads/'+OLD_BUNDLE,
 'apktool.jar':'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
 'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
 'morphe-current.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar',
 'r8.jar':'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
 'android.jar':'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar'}
def sha(data):return hashlib.sha256(data).hexdigest()
def require(ok,message):
 if not ok:raise RuntimeError(message)
def json_bytes(data):return (json.dumps(data,ensure_ascii=False,indent=2,sort_keys=True)+'\n').encode()
def run(command,log):
 log.parent.mkdir(parents=True,exist_ok=True)
 with log.open('w') as f:
  p=subprocess.run(list(map(str,command)),stdout=f,stderr=subprocess.STDOUT)
 require(p.returncode==0,'Command failed: '+str(log))
 print('PASS '+log.name,flush=True)
def normalized(obj):
 if isinstance(obj,dict):return {k:normalized(v) for k,v in obj.items() if k not in ('elapsed_ms','elapsed_seconds','index_build_ms','lookup_ms','duration_ms')}
 if isinstance(obj,list):return [normalized(v) for v in obj]
 return obj
def archive(path):
 with zipfile.ZipFile(path) as z:
  require(len(z.namelist())==len(set(z.namelist())) and z.testzip() is None,'Invalid archive '+str(path))
  return {n:z.read(n) for n in z.namelist()}
def headers(raw):
 rows=[]
 for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
  if line.startswith(b' '):rows[-1]+=line[1:]
  elif line:rows.append(line)
 return dict(row.decode().split(': ',1) for row in rows)
def manifest_bytes(values):
 lines=[]
 for k,v in values.items():
  line=b''
  for c in k+': '+v:
   raw=c.encode()
   if len(line)+len(raw)>70:lines.append(line);line=b' '
   line+=raw
  lines.append(line)
 raw=b'\r\n'.join(lines)+b'\r\n\r\n';require(headers(raw)==values,'Manifest roundtrip');return raw
def zip_add(z,name,data):
 info=zipfile.ZipInfo(name,(2026,10,29,0,0,0));info.external_attr=0o100644<<16;info.compress_type=zipfile.ZIP_DEFLATED
 z.writestr(info,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
def source_files():
 return sorted(p for p in ROOT.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ('.pyc',))
def package(source,target,oldver,newver,extension,words,license,loader):
 before=archive(source);metadata=headers(before[MF]);require(metadata['Version']==oldver,'Baseline version mismatch')
 metadata.update(Version=newver,Timestamp=STAMP,Description='日本語辞書を更新しUniDic・Wikipedia系2辞書を追加。読みと候補の重複を除き、既存候補順と長音の修正を保持。実機未確認。')
 updates={MF:manifest_bytes(metadata),EXT:extension,'swiftkey/words_ja.tsv':words,'swiftkey/words_LICENSE.txt':license,**loader}
 with zipfile.ZipFile(source) as old,zipfile.ZipFile(target,'w') as out:
  for info in old.infolist():out.writestr(copy.copy(info),updates.get(info.filename,old.read(info)))
 after=archive(target);require(before.keys()==after.keys(),'Archive inventory changed')
 changed=sorted(n for n in before if before[n]!=after[n]);require(set(changed)==set(updates),'Unexpected archive delta')
 return {'changed_entries':changed,'added_entries':[],'unchanged_entries':len(before)-len(changed),'bytes':target.stat().st_size,'sha256':sha(target.read_bytes()),'extension_sha256':sha(extension),'all_other_entries_byte_identical':True}
def build(args):
 for name,pin in PINS.items():
  path=(args.input_dir if name.endswith('.mpp') else args.tools)/name
  if not path.exists() and args.download:
   path.parent.mkdir(parents=True,exist_ok=True)
   raw=urllib.request.urlopen(URLS[name],timeout=180).read();require(sha(raw)==pin,'Download mismatch '+name);path.write_bytes(raw)
  require(path.exists() and sha(path.read_bytes())==pin,'Pinned file differs '+name)
 require(not args.work.exists(),'Build work must be a new directory')
 args.work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 run([sys.executable,ROOT/'dictionaries/test_dictionary_semantics.py'],args.work/'dictionary-semantics.log')
 before=archive(args.input_dir/OLD_SINGLE);bundle=archive(args.input_dir/OLD_BUNDLE)
 require(sha(before[EXT])==BASE_RUNTIME and before[EXT]==bundle[EXT],'Baseline runtime differs')
 require(all(before[n]==bundle[n] for n in before if n.startswith('swiftkey/')),'Baseline dictionary assets differ')
 for n in ('words_ja.tsv','words_LICENSE.txt','katakana_en.tsv'):(args.work/n).write_bytes(before['swiftkey/'+n])
 merge_cmd=[sys.executable,ROOT/'dictionaries/merge_dictionaries.py','--inputs',args.input_dir,'--baseline',args.work/'words_ja.tsv','--english',args.work/'katakana_en.tsv','--baseline-license',args.work/'words_LICENSE.txt','--output',args.work/'dictionary']
 if args.download:merge_cmd.append('--download')
 run(merge_cmd,args.work/'merge.log')
 words=(args.work/'dictionary/words_ja.tsv').read_bytes();license=(args.work/'dictionary/words_LICENSE.txt').read_bytes()
 dictionary=json.loads((args.work/'dictionary/merge_report.json').read_text())
 runtime_cmd=[sys.executable,ROOT/'runtime/build_runtime.py','--baseline-mpp',args.input_dir/OLD_SINGLE,'--dictionary',args.work/'dictionary/words_ja.tsv','--tools',args.tools,'--work',args.work/'runtime-work','--output',args.work/'runtime-dist']
 if args.reference_apk:runtime_cmd+=['--reference-apk',args.reference_apk]
 run(runtime_cmd,args.work/'runtime.log')
 runtime=normalized(json.loads((args.work/'runtime-dist/runtime-report.json').read_text()))
 extension=(args.work/'runtime-dist/runtime.mpe').read_bytes()
 abi=normalized(json.loads((args.work/'runtime-dist/native-abi.json').read_text()))
 require(runtime['word_coverage']['all_exact_entries_reachable'] is True,'Runtime coverage incomplete')
 require(abi['status']=='PASSED' and abi['extension_sha256']==sha(extension),'Runtime ABI mismatch')
 # The loader embeds dictionary digests and a resource-size ceiling.
 # Regenerate only that loader's reviewed constant changes for both SDK formats.
 run([sys.executable,ROOT/'loader/build_loaders.py','--single',args.input_dir/OLD_SINGLE,'--bundle',args.input_dir/OLD_BUNDLE,'--words',args.work/'dictionary/words_ja.tsv','--license',args.work/'dictionary/words_LICENSE.txt','--tools',args.tools,'--work',args.work/'loader-work','--output',args.work/'loader-dist'],args.work/'loader-update.log')
 lp=args.work/'loader-dist';loader_report=json.loads((lp/'loader-report.json').read_text())
 jvm='app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
 single=package(args.input_dir/OLD_SINGLE,args.output/SINGLE,'1.8.24','1.8.25',extension,words,license,{'classes.dex':(lp/'single-classes.dex').read_bytes(),jvm:(lp/'JapaneseSamsungEmojiPatch.class').read_bytes()})
 combined=package(args.input_dir/OLD_BUNDLE,args.output/BUNDLE,'1.0.313','1.0.314',extension,words,license,{'classes.dex':(lp/'bundle-classes.dex').read_bytes(),jvm:(lp/'JapaneseSamsungEmojiPatch.class').read_bytes()})
 classes=args.work/'loader-classes';classes.mkdir()
 run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-d',classes,ROOT/'tests/LoaderParityProbe.java'],args.work/'loader-compile.log')
 loaders=[]
 for name in ('morphe.jar','morphe-current.jar'):
  report=args.work/(name+'.loader.json')
  run([os.environ.get('MORPHE_JAVA','java'),'-cp',str(classes)+os.pathsep+str(args.tools/name),'LoaderParityProbe',args.input_dir/OLD_BUNDLE,args.output/SINGLE,args.output/BUNDLE,report],args.work/(name+'.loader.log'))
  d=json.loads(report.read_text());require(d['status']=='PASSED' and d['selected_extension_sha256']==sha(extension),'Loader failed');loaders.append(d)
 # Independent streaming baseline-order / duplicate validation, separate from merger.
 spec=importlib.util.spec_from_file_location('publication1825',ROOT/'publish1825.py');publisher=importlib.util.module_from_spec(spec);spec.loader.exec_module(publisher)
 dictionary_check=publisher.validate_dictionary(before['swiftkey/words_ja.tsv'],words)
 runtime['dictionary_sha256']=sha(words)
 runtime['word_coverage'].update({k:runtime['word_coverage']['fixed'][k] for k in ('returned','missing_candidates','unexpected_candidates')})
 abi_wrapper={'runtime_sha256':sha(extension),'verifier_source_sha256':sha((ROOT/'runtime/tests/AuditDictionaryAbi.java').read_bytes()),'replayed_in_ci':False,'report':abi}
 qa={'status':'DICTIONARY_UPDATE_HOST_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.25','bundle_version':'1.0.314','baseline_swiftkey_version':'1.8.24','baseline_bundle_version':'1.0.313','target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,'device_tested':False,'original_apk_apply_tested':False,'other_apps_byte_identical':True,'unrelated_loader_entries_byte_identical':True,'loader_changes_verified':True,'standalone_and_bundle_runtime_equal':True,'host_tests_passed':True,'native_abi_verified':True,'two_loader_sdks_agree':True,'runtime_sha256':sha(extension),'dictionary':dictionary_check,'source_dictionary_report':dictionary,'host_report':runtime,'abi_report':abi_wrapper,'loader_preservation':loader_report,'loader_reports':loaders,'single':single,'bundle':combined,'source_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in source_files()},'inputs':{n:{'url':URLS[n],'sha256':h} for n,h in PINS.items()}}
 (args.output/QA_NAME).write_bytes(json_bytes(qa))
 notes=(ROOT/'RELEASE_NOTES.txt').read_bytes();(args.output/'RELEASE_NOTES.txt').write_bytes(notes)
 with zipfile.ZipFile(args.output/SOURCE_ZIP,'w') as z:
  for p in source_files():zip_add(z,'src/'+p.relative_to(ROOT).as_posix(),p.read_bytes())
  zip_add(z,QA_NAME,json_bytes(qa));zip_add(z,'RELEASE_NOTES.txt',notes)
 names=[SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt']
 (args.output/'SHA256SUMS.txt').write_text(''.join(sha((args.output/n).read_bytes())+'  '+n+'\n' for n in names))
 print(json.dumps({'status':'PASSED','readings':dictionary_check['readings'],'pairs':dictionary_check['candidates'],'runtime':sha(extension),'single':single['sha256'],'bundle':combined['sha256']}),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--input-dir',type=Path,required=True);p.add_argument('--tools',type=Path,required=True);p.add_argument('--work',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--download',action='store_true');p.add_argument('--reference-apk',type=Path);build(p.parse_args())
