#!/usr/bin/env python3
"""Repair Japanese/English prolonged-mark lookup and Japanese exact candidate truncation."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, re, shutil, subprocess, time, urllib.request, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
OLD_SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.23.mpp'
OLD_BUNDLE='Hiro_Morphe_Patches_v1.0.312.mpp'
SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.24.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.313.mpp'
QA_NAME='QA_SwiftKey_v1.8.24.json'
SOURCE_ZIP='SwiftKeyBeta_v1.8.24_sources_and_QA.zip'
MF,EXT='META-INF/MANIFEST.MF','extensions/swiftkey_japanese.mpe'
STAMP='2026-10-28T12:00:00'
ADDED_JVM=set()
DESCRIPTION='長音「ー」を含む入力で英語・日本語の追加辞書が出ない不具合と、日本語の完全一致候補が12件で欠ける問題を修正。既存辞書・学習・候補操作を保持。実機未確認。'
EXTENSION_PIN='dea2051af47cd8e2106437dbf7b3bc4f9ac2e6d496a334462700d7d2584e405a'
APK_PIN='e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
PINS={
 OLD_SINGLE:'714d66b2cc5367346927fd8a766577640d4d1c3bc61829671f888ee13cc6bbdc',
 OLD_BUNDLE:'5381f9b8038c847f244275b3e2c90a09869c235cc5492c8b59aa832570ad5a3e',
 'apktool.jar':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
 'morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
 'morphe-current.jar':'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
 'r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'}
URLS={
 OLD_SINGLE:'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/72a7fdd39a0ce9d4f4531c3509e90fab6300fe41/downloads/'+OLD_SINGLE,
 OLD_BUNDLE:'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/845082966b599f03e66d592b5a4ed066f0a9f94f/downloads/'+OLD_BUNDLE,
 'apktool.jar':'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
 'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
 'morphe-current.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar',
 'r8.jar':'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
 'android.jar':'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar'}
WORD_TYPES={'hiro/swiftkey/WordCandidates.smali','hiro/swiftkey/WordCandidates$Index.smali','hiro/swiftkey/WordCandidates$1.smali'}
CHANGED_METHODS=[
 'Lhiro/swiftkey/KatakanaEnglishCandidates;->isRomaji(Ljava/lang/String;)Z',
 'Lhiro/swiftkey/WordCandidates;->addWords(Ljava/util/LinkedHashSet;[Ljava/lang/String;Ljava/util/Set;)V',
 'Lhiro/swiftkey/WordCandidates;->isRomaji(Ljava/lang/String;)Z']
def sha(raw): return hashlib.sha256(raw).hexdigest()
def require(ok,message):
    if not ok: raise RuntimeError(message)
def run(command,log):
    result=subprocess.run([str(a) for a in command],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    log.parent.mkdir(parents=True,exist_ok=True);log.write_text(result.stdout)
    print(result.stdout,end='',flush=True)
    require(result.returncode==0,'Command failed; see '+str(log));return result.stdout
def javac(args,log): return run(['java','com.sun.tools.javac.Main',*args],log)
def smali_files(path): return {p.relative_to(path).as_posix():p.read_bytes() for p in sorted(path.rglob('*.smali'))}
def helper(path): return any(path==f'hiro/swiftkey/{name}.smali' or path.startswith(f'hiro/swiftkey/{name}$') for name in ('EmojiRecentsSlide','EmojiRecentsBridge','EmojiToolbar'))
def manifest(raw):
    rows=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '): require(bool(rows),'Invalid manifest continuation');rows[-1]+=line[1:]
        elif line: rows.append(line)
    pairs=[line.decode().split(': ',1) for line in rows]
    require(all(len(p)==2 for p in pairs) and len(dict(pairs))==len(pairs),'Invalid manifest')
    return dict(pairs)
def manifest_bytes(values):
    lines=[]
    for key,value in values.items():
        line=b''
        for char in key+': '+value:
            raw=char.encode()
            if len(line)+len(raw)>70:lines.append(line);line=b' '
            line+=raw
        lines.append(line)
    raw=b'\r\n'.join(lines)+b'\r\n\r\n';require(manifest(raw)==values,'Manifest roundtrip');return raw
def download(inputs,tools):
    for name,pin in PINS.items():
        path=(inputs if name.endswith('.mpp') else tools)/name
        if path.exists():require(sha(path.read_bytes())==pin,'Pinned input differs: '+name);continue
        path.parent.mkdir(parents=True,exist_ok=True)
        for attempt in range(3):
            try:
                raw=urllib.request.urlopen(URLS[name],timeout=120).read()
                require(sha(raw)==pin,'Downloaded hash differs: '+name);path.write_bytes(raw);break
            except Exception:
                if attempt==2:raise
                time.sleep(2*(attempt+1))
def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist())==len(set(z.namelist())) and z.testzip() is None,'Invalid ZIP '+str(path))
        return {n:z.read(n) for n in z.namelist()}
def zip_add(z,name,raw):
    info=zipfile.ZipInfo(name,(2026,10,28,12,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
    z.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
def make_mpp(source,target,extension,old_version,version,loader_updates):
    with zipfile.ZipFile(source) as old:
        values=manifest(old.read(MF));require(values['Version']==old_version,'Baseline version differs')
        values.update(Version=version,Timestamp=STAMP,Description=DESCRIPTION)
        updates={MF:manifest_bytes(values),EXT:extension,**loader_updates}
        with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as new:
            for info in old.infolist():new.writestr(copy.copy(info),updates.get(info.filename,old.read(info)))
            for name in sorted(set(updates)-set(old.namelist())):zip_add(new,name,updates[name])
    before,after=archive(source),archive(target)
    require(set(before).issubset(after),'Archive entry removed')
    changed=sorted(n for n in before if before[n]!=after[n]);added=sorted(set(after)-set(before))
    require(set(changed)|set(added)==set(updates),'Unexpected MPP changes')
    require(set(added)==ADDED_JVM and set(after)==set(before)|ADDED_JVM,'Unexpected archive inventory change')
    return {'changed_entries':changed,'added_entries':added,'unchanged_entries':len(before)-len(changed),
            'bytes':target.stat().st_size,'sha256':sha(target.read_bytes()),'extension_sha256':sha(extension),'all_other_entries_byte_identical':True}

def source_paths():
    return sorted(p for p in ROOT.rglob('*') if p.is_file()
                  and not {'__pycache__','reproduced','classes'}.intersection(p.relative_to(ROOT).parts)
                  and p.suffix in ('.py','.java','.json','.txt','.md','.yml','.yaml','.smali','.b64'))

def normalized_report(report):
    if isinstance(report,dict):return {k:normalized_report(v) for k,v in report.items() if k not in ('elapsed_ms','elapsed_seconds')}
    if isinstance(report,list):return [normalized_report(v) for v in report]
    return report

def methods(text):
    return {m.group(1):m.group(0) for m in re.finditer(r'^\.method (.*?)\n.*?^\.end method$',text,re.M|re.S)}

def compile_word(mode,tools,work,dump):
    folder=work/mode;classes=folder/'classes';classes.mkdir(parents=True)
    source=ROOT/('baseline' if mode=='baseline' else 'src')/'hiro/swiftkey/WordCandidates.java'
    javac(['--release','8','-encoding','UTF-8','-cp',tools/'android.jar','-d',classes,
           *sorted((ROOT/'stubs').rglob('*.java')),source,ROOT/'tests/WordCandidatesCoverageTest.java'],folder/'javac.txt')
    with zipfile.ZipFile(folder/'runtime.jar','w') as runtime,zipfile.ZipFile(folder/'stubs.jar','w') as stubs:
        for p in sorted(classes.rglob('*.class')):
            name=p.relative_to(classes).as_posix()
            if 'CoverageTest' in name:continue
            zip_add(runtime if name.startswith('hiro/swiftkey/WordCandidates') else stubs,name,p.read_bytes())
    (folder/'dex').mkdir()
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--debug','--min-api','26','--lib',tools/'android.jar',
         '--classpath',folder/'stubs.jar','--output',folder/'dex',folder/'runtime.jar'],folder/'d8.txt')
    run(dump+[folder/'dex/classes.dex',folder/'smali'],folder/'dump.txt')
    census=run(['java','-Xmx512m','-cp',str(classes)+os.pathsep+str(tools/'android.jar'),
                'hiro.swiftkey.WordCandidatesCoverageTest',work/'words_ja.tsv',mode],folder/'host.txt')
    result=normalized_report(json.loads(census.strip().splitlines()[-1]))
    require(result.get('status')=='PASSED' and result.get('rows')==638044 and result.get('candidates')==847642,
            'Japanese dictionary census scope differs')
    (folder/'host.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    return result

def build(inputs,tools,work,output,compile_only=False):
    for name,pin in PINS.items():
        path=(inputs if name.endswith('.mpp') else tools)/name
        require(path.is_file() and sha(path.read_bytes())==pin,'Pinned input differs: '+name)
    if work.exists():shutil.rmtree(work)
    for folder in (work,work/'tool_classes',output):folder.mkdir(parents=True,exist_ok=True)
    before_single,before_bundle=archive(inputs/OLD_SINGLE),archive(inputs/OLD_BUNDLE)
    require(before_single[EXT]==before_bundle[EXT] and sha(before_single[EXT])==EXTENSION_PIN,'Published runtime pin differs')
    dictionary_names=sorted(n for n in before_single if n.startswith('swiftkey/'))
    require(dictionary_names and all(before_single[n]==before_bundle[n] for n in dictionary_names),'Single/bundle dictionaries differ')
    (work/'old.mpe').write_bytes(before_single[EXT]);(work/'words_ja.tsv').write_bytes(before_single['swiftkey/words_ja.tsv'])
    (work/'katakana_en.tsv').write_bytes(before_single['swiftkey/katakana_en.tsv'])
    javac(['-cp',tools/'apktool.jar','-d',work/'tool_classes',ROOT/'tools_java/Dump1814.java'],work/'javac-dump.txt')
    javac(['-cp',tools/'morphe.jar','-d',work/'tool_classes',ROOT/'tools_java/MergeDictionaries.java'],work/'javac-merge.txt')
    dump=['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'),'Dump1814']
    run(dump+[work/'old.mpe',work/'before'],work/'dump-before.txt')
    before=smali_files(work/'before');require(len(before)==91,'Published runtime class count differs')
    baseline_test=compile_word('baseline',tools,work,dump)
    fixed_test=compile_word('fixed',tools,work,dump)
    rebuilt=smali_files(work/'baseline/smali');fixed=smali_files(work/'fixed/smali')
    require(set(rebuilt)==WORD_TYPES==set(fixed),'Compiled word type inventory differs')
    require(all(rebuilt[n]==before[n] for n in WORD_TYPES),'Original Java does not exactly reconstruct published Word classes')
    expected_word={
        'private static addWords(Ljava/util/LinkedHashSet;[Ljava/lang/String;Ljava/util/Set;)V',
        'private static isRomaji(Ljava/lang/String;)Z'}
    for name in WORD_TYPES:
        old,new=rebuilt[name].decode(),fixed[name].decode();old_m,new_m=methods(old),methods(new)
        require(old_m.keys()==new_m.keys(),'Compiled word method inventory changed')
        changed={key for key in old_m if old_m[key]!=new_m[key]}
        require(changed==(expected_word if name.endswith('/WordCandidates.smali') else set()),'Unexpected compiled Word method delta')
    require(baseline_test['missing_candidates']==9411 and baseline_test['incomplete_readings']==2175,'Japanese negative control did not reproduce')
    require(fixed_test['missing_candidates']==0 and fixed_test['unexpected_candidates']==0
            and fixed_test['returned']==847642,'Japanese exact candidates still missing')
    runtime_java=os.environ.get('MORPHE_JAVA','java')
    run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'),'MergeDictionaries',
         work/'old.mpe',work/'fixed/dex/classes.dex',work/'new.mpe',work/'merge-report.json'],work/'merge.txt')
    run(dump+[work/'new.mpe',work/'after'],work/'dump-after.txt')
    after=smali_files(work/'after');require(before.keys()==after.keys(),'Runtime class inventory changed')
    changed_classes=sorted(n for n in before if before[n]!=after[n])
    require(changed_classes==['hiro/swiftkey/KatakanaEnglishCandidates.smali','hiro/swiftkey/WordCandidates.smali'],'Unrelated runtime class changed')
    changed_methods=[]
    for name in before:
        old,new=methods(before[name].decode()),methods(after[name].decode())
        require(old.keys()==new.keys(),'Runtime method inventory changed: '+name)
        for key in old:
            if old[key]!=new[key]:changed_methods.append('L'+name.removesuffix('.smali')+';->'+key.split(' ')[-1])
    require(sorted(changed_methods)==CHANGED_METHODS,'Unexpected runtime method delta')
    merge=json.loads((work/'merge-report.json').read_text())
    require(merge.get('status')=='PASSED' and merge.get('changed_methods')==CHANGED_METHODS,'Implementation transplant proof failed')
    extension=(work/'new.mpe').read_bytes();runtime_sha=sha(extension)
    for field in ('all_other_method_bodies_preserved','all_class_and_method_metadata_preserved','compiled_implementations_preserved','romaji_no_self_references'):
        require(merge.get(field) is True,'Missing preservation proof: '+field)
    if compile_only:
        report={'runtime_sha256':runtime_sha,'release_ready':False,'runtime_changed_methods':CHANGED_METHODS,
                'word_coverage':{'baseline':baseline_test,'fixed':fixed_test,'all_exact_entries_reachable':True}}
        (output/'compile-only.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
        print('COMPILE ONLY: '+str(work/'new.mpe'),flush=True);return report

    english_script=ROOT/'tests/english_regression/run_english_tests.py'
    require(english_script.is_file(),'Missing actual-DEX English regression runner')
    english_path=work/'english-host.json'
    run(['python3',english_script,'--baseline',work/'old.mpe','--fixed',work/'new.mpe',
         '--dictionary',work/'katakana_en.tsv','--tools',tools,'--work',work/'english-host','--output',english_path],work/'english-host.txt')
    english=normalized_report(json.loads(english_path.read_text()))
    require(english.get('status')=='PASSED' and english.get('whole_long_mark_examples_verified') is True and english.get('baseline_negative_examples_verified') is True
            and english.get('fixed_mpe_sha256')==runtime_sha and english.get('baseline_mpe_sha256')==EXTENSION_PIN,
            'English long-mark regression failed')
    abi_path=ROOT/'tests/runtime-abi.json';require(abi_path.is_file(),'Missing independently generated native ABI report')
    abi=json.loads(abi_path.read_text());verifier_sha=sha((ROOT/'tests/AuditDictionaryAbi.java').read_bytes())
    require(abi.get('status')=='PASSED' and abi.get('extension_sha256')==runtime_sha and abi.get('apk_sha256')==APK_PIN,
            'Native ABI report does not cover this compiled runtime')
    require(abi.get('verifier_source_sha256')==verifier_sha,'Native ABI verifier source changed')
    require(abi.get('android_jar_sha256')==PINS['android.jar'] and not abi.get('unresolved') and not abi.get('inaccessible')
            and not abi.get('incompatible_opcodes'),'Native ABI verification failed')
    require(abi.get('device_tested') is False and abi.get('original_apk_apply_tested') is False,'ABI verification scope changed')
    audited=set(abi.get('audited_classes',[]))
    require(all('L'+n.removesuffix('.smali')+';' in audited for n in changed_classes),'Changed dictionary classes omitted from ABI audit')
    single=make_mpp(inputs/OLD_SINGLE,output/SINGLE,extension,'1.8.23','1.8.24',{})
    bundle=make_mpp(inputs/OLD_BUNDLE,output/BUNDLE,extension,'1.0.312','1.0.313',{})
    new_single,new_bundle=archive(output/SINGLE),archive(output/BUNDLE)
    require(all(new_single[n]==before_single[n] and new_bundle[n]==before_bundle[n] for n in dictionary_names),'Dictionary resources changed')
    require(all(new_bundle[n]==before_bundle[n] for n in before_bundle if n not in (MF,EXT)),'Unrelated aggregate payload changed')
    require(new_single[EXT]==new_bundle[EXT],'Standalone/combined runtime differs')
    javac(['--release','8','-g:none','-d',work/'tool_classes',ROOT/'tests/LoaderParityProbe.java'],work/'javac-loader.txt')
    loaders=[]
    for name in ('morphe.jar','morphe-current.jar'):
        path=work/(name+'.loader.json')
        run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/name),'LoaderParityProbe',
             inputs/OLD_BUNDLE,output/SINGLE,output/BUNDLE,path],work/(name+'.loader.txt'))
        report=json.loads(path.read_text())
        require(report.get('status')=='PASSED' and report.get('selected_extension_sha256')==runtime_sha
                and report.get('baseline_extension_sha256')==EXTENSION_PIN,'Actual loader selected incorrect runtime')
        loaders.append(report)
    require(loaders[0]==loaders[1],'Two Morphe loader SDKs disagree')
    host={'status':'PASSED','runtime_sha256':runtime_sha,'word_coverage':{'baseline':baseline_test,'fixed':fixed_test,'all_exact_entries_reachable':True},
          'english_regression':english,'negative_control_verified':True,'compiled_word_baseline_parity':True}
    qa={
      'status':'DICTIONARY_LOOKUP_HOST_AND_ABI_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.24','bundle_version':'1.0.313',
      'baseline_swiftkey_version':'1.8.23','baseline_bundle_version':'1.0.312','target_package':'com.touchtype.swiftkey.beta',
      'target_version':'9.13.16.4','target_version_code':1236271168,'device_tested':False,'original_apk_apply_tested':False,
      'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,'loader_entries_byte_identical':True,
      'standalone_and_bundle_runtime_equal':True,'runtime_before_classes':91,'runtime_after_classes':91,
      'runtime_changed_classes':changed_classes,'runtime_changed_methods':CHANGED_METHODS,
      'host_tests_passed':True,'native_abi_verified':True,'two_loader_sdks_agree':True,'runtime_sha256':runtime_sha,
      'host_report':host,'abi_report':{'report':abi,'verifier_source_sha256':verifier_sha,'runtime_sha256':runtime_sha,'replayed_in_ci':False},
      'loader_reports':loaders,'runtime_preservation':merge,'single':single,'bundle':bundle,
      'inputs':{name:{'url':URLS[name],'sha256':pin} for name,pin in PINS.items()},
      'source_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in source_paths()}}
    (output/QA_NAME).write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    (output/'RELEASE_NOTES.txt').write_bytes((ROOT/'RELEASE_NOTES.txt').read_bytes())
    with zipfile.ZipFile(output/SOURCE_ZIP,'w') as z:
        for p in source_paths():zip_add(z,'src/'+p.relative_to(ROOT).as_posix(),p.read_bytes())
        for name in changed_classes:zip_add(z,'evidence/runtime/'+name,after[name])
        zip_add(z,QA_NAME,(output/QA_NAME).read_bytes())
    names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
    (output/'SHA256SUMS.txt').write_text(''.join(sha((output/name).read_bytes())+'  '+name+'\n' for name in names))
    print('PASS SwiftKey 1.8.24 / bundle 1.0.313 dictionary lookup repair',flush=True);return qa

def main():
    p=argparse.ArgumentParser()
    for name in ('input-dir','tools','work','output'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--download',action='store_true');p.add_argument('--compile-only',action='store_true')
    a=p.parse_args();inputs,tools,work,output=(getattr(a,n).resolve() for n in ('input_dir','tools','work','output'))
    if a.download:download(inputs,tools)
    build(inputs,tools,work,output,a.compile_only)

if __name__=='__main__':main()
