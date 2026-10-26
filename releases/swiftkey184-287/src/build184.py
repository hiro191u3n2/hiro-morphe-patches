#!/usr/bin/env python3
"""Reproducible SwiftKey glide runtime update, preserving every unrelated entry."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SINGLE_BASE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.3.mpp'
BUNDLE_BASE = 'Hiro_Morphe_Patches_v1.0.286.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.4.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.287.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.4.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.4_sources_and_QA.zip'
MF, EXT = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
STAMP = '2026-10-26T07:00:00'
PINS = {
    SINGLE_BASE: 'f30018c5c9e02107b0f5f48a85493a7efa819e5015c8661709b8b26fffd91dd4',
    BUNDLE_BASE: 'a8742bc3bd4bd1af27824ae0364c90dda96d32c31104f1c84109c3f7bf6aec54',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
}
REPO = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/'
URLS = {
    SINGLE_BASE: REPO+'fec446ac275d92a7b366ca83a3b63d5ad6dddf0e/downloads/'+SINGLE_BASE,
    BUNDLE_BASE: REPO+'fbd87a0265a6c37f7faa657386321ba5f7f8330f/downloads/'+BUNDLE_BASE,
    'apktool.jar': 'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
    'morphe.jar': 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
    'r8.jar': 'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
    'android.jar': 'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar',
}
DESCRIPTION = '日本語QWERTYグライドの候補選択・連続入力・配列判定・短い語を改善。既存辞書、Samsung絵文字、キー配置、LINE改行、削除保護は保持。実機未確認。'
FAMILIES = ('hiro/swiftkey/JapaneseGlide', 'hiro/swiftkey/JapaneseGlideDecoder', 'hiro/swiftkey/JapaneseGlideCandidates')
KEY_SWAP = 'hiro/swiftkey/KeySwap.smali'
OLD_SUMMARY = 'ローマ字のキーをなぞった読みを通常の日本語変換へ渡します。自動確定はしません。標準・片手配列が対象です。初期設定はオフです。'
NEW_SUMMARY = 'ローマ字のキーをなぞり、表示された読みを選ぶと通常の日本語変換へ渡します。標準・片手・PC・分割QWERTY配列に対応します。初期設定はオフです。'

def sha(raw): return hashlib.sha256(raw).hexdigest()
def require(ok, message):
    if not ok: raise RuntimeError(message)
def owned(path): return path == KEY_SWAP or any(path == f+'.smali' or path.startswith(f+'$') for f in FAMILIES)
def run(args, log, cwd=None):
    result = subprocess.run([str(a) for a in args], cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    log.parent.mkdir(parents=True, exist_ok=True); log.write_text(result.stdout)
    print(result.stdout, end='', flush=True)
    require(result.returncode == 0, 'Command failed; see '+str(log))
    return result.stdout
def javac(args, log): return run(['java', 'com.sun.tools.javac.Main', *args], log)
def files(path): return {p.relative_to(path).as_posix(): p.read_bytes() for p in sorted(path.rglob('*.smali'))}
def manifest(raw):
    rows=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '): require(bool(rows),'Invalid manifest continuation'); rows[-1]+=line[1:]
        elif line: rows.append(line)
    pairs=[line.decode().split(': ',1) for line in rows]
    require(all(len(p)==2 for p in pairs),'Malformed manifest'); require(len(dict(pairs))==len(pairs),'Duplicate manifest key')
    return dict(pairs)
def write_manifest(values):
    lines=[]
    for k,v in values.items():
        line=b''
        for c in k+': '+v:
            raw=c.encode()
            if len(line)+len(raw)>70: lines.append(line); line=b' '
            line+=raw
        lines.append(line)
    raw=b'\r\n'.join(lines)+b'\r\n\r\n'
    require(manifest(raw)==values,'Manifest roundtrip failed'); return raw
def download(input_dir, tools):
    for name,pin in PINS.items():
        path=(input_dir if name.endswith('.mpp') else tools)/name
        if path.exists(): require(sha(path.read_bytes())==pin,'Existing pinned file differs: '+name); continue
        path.parent.mkdir(parents=True,exist_ok=True)
        for attempt in range(3):
            try:
                raw=urllib.request.urlopen(URLS[name],timeout=120).read()
                require(sha(raw)==pin,'Downloaded checksum differs: '+name)
                path.write_bytes(raw); break
            except Exception:
                if attempt==2: raise
                time.sleep(2*(attempt+1))
        print('Downloaded verified '+name,flush=True)
def make_mpp(source, target, extension, old_version, version):
    with zipfile.ZipFile(source) as old:
        names=old.namelist(); require(len(names)==len(set(names)) and old.testzip() is None,'Invalid baseline ZIP')
        values=manifest(old.read(MF)); require(values['Version']==old_version,'Baseline version differs')
        values.update(Version=version,Timestamp=STAMP,Description=DESCRIPTION)
        updates={MF:write_manifest(values),EXT:extension}
        with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as new:
            for info in old.infolist(): new.writestr(copy.copy(info),updates.get(info.filename,old.read(info)))
        with zipfile.ZipFile(target) as new:
            require(new.namelist()==names and new.testzip() is None,'Output ZIP differs structurally')
            changed=[n for n in names if old.read(n)!=new.read(n)]
            require(set(changed)==set(updates),'Unexpected archive changes '+repr(changed))
            return {'changed_entries':changed,'unchanged_entries':len(names)-len(changed),'added_entries':[],
                    'bytes':target.stat().st_size,'sha256':sha(target.read_bytes()),
                    'extension_sha256':sha(new.read(EXT)),'all_other_entries_byte_identical':True}
def zip_add(z,name,raw):
    info=zipfile.ZipInfo(name,(2026,10,26,7,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
    z.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
def source_paths():
    names=('build184.py','publish184.py','publisher_tests.py','workflow.yml','RELEASE_NOTES.txt','README.txt',
           'audit_runtime_abi.py')
    paths=[ROOT/name for name in names if (ROOT/name).is_file()]
    for name in ('src','stubs','tests','tools_java'):
        if (ROOT/name).is_dir(): paths.extend(p for p in (ROOT/name).rglob('*') if p.is_file() and p.suffix in ('.java','.py','.json','.txt','.md','.yml'))
    return sorted(set(paths))
def build(input_dir,tools,work,output):
    for name,pin in PINS.items(): require(sha(((input_dir if name.endswith('.mpp') else tools)/name).read_bytes())==pin,'Pinned input differs: '+name)
    if work.exists(): shutil.rmtree(work)
    for d in (work,output,output/'evidence',work/'tool_classes',work/'classes',work/'new_dex'): d.mkdir(parents=True,exist_ok=True)
    evidence=output/'evidence'
    if (evidence/'runtime').exists(): shutil.rmtree(evidence/'runtime')
    with zipfile.ZipFile(input_dir/SINGLE_BASE) as a,zipfile.ZipFile(input_dir/BUNDLE_BASE) as b:
        require(a.read(EXT)==b.read(EXT),'Standalone/bundle runtime differs')
        for name in a.namelist():
            if name.startswith('swiftkey/'): require(a.read(name)==b.read(name),'SwiftKey resource differs: '+name)
        (work/'old.mpe').write_bytes(a.read(EXT)); (work/'glide_ja.bin').write_bytes(a.read('swiftkey/glide_ja.bin'))
    javac(['-cp',tools/'morphe.jar','-d',work/'tool_classes',ROOT/'tools_java/Merge184.java',ROOT/'tools_java/Assemble184.java'],work/'javac-merge.txt')
    javac(['-cp',tools/'apktool.jar','-d',work/'tool_classes',ROOT/'tools_java/Dump184.java'],work/'javac-dump.txt')
    dump=['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'),'Dump184']
    run(dump+[work/'old.mpe',work/'before'],work/'dump-before.txt')
    before=files(work/'before')
    old_literal=json.dumps(OLD_SUMMARY,ensure_ascii=True)[1:-1]
    new_literal=json.dumps(NEW_SUMMARY,ensure_ascii=True)[1:-1]
    key_swap=before[KEY_SWAP].decode(); require(key_swap.count(old_literal)==1,'Glide setting summary anchor differs')
    updated_key_swap=key_swap.replace(old_literal,new_literal,1)
    require(updated_key_swap.replace(new_literal,old_literal,1)==key_swap,'Unexpected KeySwap modification')
    pref=work/'preference'/KEY_SWAP;pref.parent.mkdir(parents=True,exist_ok=True);pref.write_text(updated_key_swap)
    run(['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'),'Assemble184',work/'preference',work/'preference.dex'],work/'assemble-preference.txt')
    production=sorted((ROOT/'src').rglob('*.java')); stubs=sorted((ROOT/'stubs').rglob('*.java'))
    require(production and stubs,'Missing production/stubs')
    javac(['--release','8','-g:none','-encoding','UTF-8','-cp',tools/'android.jar','-d',work/'classes',*stubs,*production],work/'javac-runtime.txt')
    with zipfile.ZipFile(work/'runtime.jar','w') as runtime,zipfile.ZipFile(work/'stubs.jar','w') as stubjar:
        for p in sorted((work/'classes').rglob('*.class')):
            name=p.relative_to(work/'classes').as_posix()
            is_runtime=name.startswith('hiro/swiftkey/JapaneseGlide')
            (runtime if is_runtime else stubjar).write(p,name)
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar','--classpath',work/'stubs.jar','--output',work/'new_dex',work/'runtime.jar'],work/'d8.txt')
    run(['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'),'Merge184',work/'old.mpe',work/'new_dex/classes.dex',work/'preference.dex',work/'new.mpe'],evidence/'merge.txt')
    for name,path in [('after',work/'new.mpe'),('overlay',work/'new_dex/classes.dex'),('preference-check',work/'preference.dex')]: run(dump+[path,work/name],work/('dump-'+name+'.txt'))
    after,overlay=(files(work/n) for n in ('after','overlay'))
    preference_check=files(work/'preference-check')
    require(preference_check=={KEY_SWAP:updated_key_swap.encode()},'Preference assembly changed unrelated instructions')
    overlay.update(preference_check)
    require(all(owned(n) for n in overlay),'Unexpected runtime overlay family')
    unrelated={n:b for n,b in before.items() if not owned(n)}
    require(unrelated=={n:b for n,b in after.items() if not owned(n)},'Unrelated runtime classes changed')
    require(overlay=={n:b for n,b in after.items() if owned(n)},'Compiled runtime differs after merge')
    for name,raw in overlay.items():
        path=evidence/'runtime'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
    extension=(work/'new.mpe').read_bytes()
    # This reviewed proof comes from the archived APK. CI checks its exact runtime
    # binding, but cannot replay the private-APK inspection without that input.
    native_proof_path=ROOT/'tests/native_abi_review.json'
    require(native_proof_path.exists(),'Missing reviewed native ABI proof; inspect '+str(work/'new.mpe'))
    native_proof=json.loads(native_proof_path.read_text())
    require(native_proof.get('status')=='PASSED' and not native_proof.get('unresolved') and not native_proof.get('inaccessible') and not native_proof.get('incompatible_opcodes'),
            'Native ABI review is not a pass')
    require(native_proof.get('extension_sha256')==sha(extension),'Native ABI review targets a different extension; review '+str(work/'new.mpe'))
    require(native_proof.get('apk_sha256')=='e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7',
            'Reviewed archived APK hash differs')
    require(native_proof.get('device_tested') is False and native_proof.get('original_apk_apply_tested') is False,
            'Native review must not claim clean-APK or device testing')
    (evidence/'native-abi-review.json').write_bytes(native_proof_path.read_bytes())
    # Tests execute the same production sources and dictionary packaged into both MPPs.
    test_runner=ROOT/'tests/run_tests.py'
    require(test_runner.exists(),'Missing host regression test runner')
    tests=run(['python',test_runner,'--source-dir',ROOT,'--tools',tools,'--work',work/'host_tests','--dictionary',work/'glide_ja.bin'],evidence/'host-tests.txt')
    require('PASS' in tests,'Host tests produced no success assertions')
    single=make_mpp(input_dir/SINGLE_BASE,output/SINGLE,extension,'1.8.3','1.8.4')
    bundle=make_mpp(input_dir/BUNDLE_BASE,output/BUNDLE,extension,'1.0.286','1.0.287')
    listing=run([os.environ.get('MORPHE_JAVA','java'),'-jar',tools/'morphe.jar','list-patches','--patches',output/SINGLE,'-v','-o'],evidence/'morphe-single-list.txt')
    require('日本語入力・予測変換を改善しSamsung絵文字に統一' in listing and listing.count('Name: ')==1,'Standalone patch listing differs')
    qa={'status':'HOST_AND_STATIC_CHECKS_PASSED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.4','bundle_version':'1.0.287',
        'target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,
        'device_tested':False,'original_apk_apply_tested':False,'device_cause_confirmed':False,'device_fix_confirmed':False,
        'runtime_unrelated_classes_unchanged':True,'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,
        'standalone_and_bundle_runtime_equal':True,'loader_entries_byte_identical':True,'all_static_checks_passed':True,
        'key_swap_change_is_glide_summary_literal_only':True,
        'native_abi_review_verified':True,'native_abi_replayed_in_ci':False,
        'native_abi_review':{'source':'tests/native_abi_review.json','sha256':sha(native_proof_path.read_bytes()),
                             'apk_sha256':native_proof['apk_sha256'],'apk_provenance':native_proof['apk_provenance'],
                             'extension_sha256':native_proof['extension_sha256'],
                             'native_and_extension_reference_count':len(native_proof['native_and_extension_references']),
                             'unresolved':[],'inaccessible':[]},
        'host_tests_passed':True,'morphe_desktop_single_list_passed':True,'morphe_manager_device_tested':False,
        'runtime':{'before_classes':len(before),'after_classes':len(after),'unchanged_unrelated_classes':len(unrelated),
                   'old_owned_classes':len(before)-len(unrelated),'new_owned_classes':len(overlay),
                   'removed':sorted(set(before)-set(after)),'added':sorted(set(after)-set(before)),
                   'changed':sorted(n for n in before.keys()&after.keys() if before[n]!=after[n]),
                   'canonical_smali_equality_verified':True,'extension_sha256':sha(extension)},
        'single':single,'bundle':bundle,'inputs':{n:{'url':URLS[n],'sha256':s} for n,s in PINS.items()}}
    (output/QA_NAME).write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    notes=ROOT/'RELEASE_NOTES.txt'; require(notes.exists(),'Missing reviewed release notes')
    (output/'RELEASE_NOTES.txt').write_bytes(notes.read_bytes())
    with zipfile.ZipFile(output/SOURCE_ZIP,'w') as z:
        for p in source_paths(): zip_add(z,'src/'+p.relative_to(ROOT).as_posix(),p.read_bytes())
        # Tool banners may contain JDK versions, paths, timing or ANSI escapes.
        # Keep raw logs available beside the artifacts; publish deterministic code and QA only.
        for p in sorted((evidence/'runtime').rglob('*.smali')):
            zip_add(z,'evidence/runtime/'+p.relative_to(evidence/'runtime').as_posix(),p.read_bytes())
        zip_add(z,QA_NAME,(output/QA_NAME).read_bytes())
    names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
    (output/'SHA256SUMS.txt').write_text(''.join(sha((output/n).read_bytes())+'  '+n+'\n' for n in names))
    print('PASS build SwiftKey 1.8.4 / bundle 1.0.287',flush=True)
    return qa
def main():
    p=argparse.ArgumentParser();p.add_argument('--input-dir',type=Path,required=True);p.add_argument('--tools',type=Path,required=True)
    p.add_argument('--work',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--download',action='store_true')
    a=p.parse_args();a.input_dir=a.input_dir.resolve();a.tools=a.tools.resolve();a.work=a.work.resolve();a.output=a.output.resolve()
    if a.download: download(a.input_dir,a.tools)
    build(a.input_dir,a.tools,a.work,a.output)
if __name__=='__main__': main()
