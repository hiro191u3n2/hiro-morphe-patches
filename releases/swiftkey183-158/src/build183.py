#!/usr/bin/env python3
"""Build and verify SwiftKey v1.8.3 deletion protection from pinned v1.8.2 inputs."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, re, subprocess, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
OLD_SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.2.mpp'
OLD_BUNDLE='Hiro_Morphe_Patches_v1.0.157.mpp'
SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.3.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.158.mpp'
MANIFEST='META-INF/MANIFEST.MF'
EXT='extensions/swiftkey_japanese.mpe'
BOOT='app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch'
PATCH='app/hiro/swiftkey/patches/DeleteProtectionPatch'
HELPER='hiro/swiftkey/DeleteLatinBoundary'
BUILD_TIMESTAMP='2026-10-07T02:15:32'
PINS={OLD_SINGLE:'a705113b98f1f0fa13a9c1e73adb778abe7b30392b6c575234faa5ed2abb454a',
      OLD_BUNDLE:'9b50baa737d9891a725586f567e5ee43a4a7b279a286577934fceffb1fc4a5c4',
      'apktool.jar':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
      'morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
      'r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa'}
BASE='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/44297719591f12a18476c074246bd58b623099ce/downloads/'
URLS={OLD_SINGLE:BASE+OLD_SINGLE,OLD_BUNDLE:BASE+OLD_BUNDLE,
      'apktool.jar':'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
      'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
      'r8.jar':'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar'}
DESCRIPTION='日本語入力中、1文字ずつの削除・単語削除の後で既存の英字が自動選択・再変換対象へ戻るのを抑止。PokémonなどUnicodeラテン文字に対応。新規日本語入力、手動範囲選択、既存辞書・キー配置・LINE改行を維持。実機未確認。'
NOTES='''Microsoft SwiftKey Beta v1.8.3 / Hiro Morphe Patches v1.0.158

日本語入力中に「Meta for Pokémon UNITE | Pokémon UNITE API」を一文字ずつ削除すると、
残った英単語が変換対象へ戻る経路に、削除後の既存Latin保護を追加します。
v1.8.2のカーソル移動・貼り付け対策とUnicode判定はそのまま引き継ぎます。

対象は通常のBackspace、変換文字列を短くする削除、既存の単語削除経路です。
SwiftKey本来の削除を1回実行した後、残った既存の英字だけを再変換対象から外します。
削除前は一時的な入力状態の記録だけを行い、入力欄や変換範囲を変更しません。
本文がカーソル周辺の連続した文字の削除であることを前後比較で確認し、
元の削除文字数・本文・カーソル位置・戻り値・例外処理を保持します。

日本語を入力途中のローマ字や仮名は、変換文字列が残る間はそのまま編集できます。
入力途中の変換文字列をすべて消し、以前の英字へ戻った場合は再変換を抑止します。
明示的な範囲選択、英語モード、通常の文字追加、本文置換、入力欄切替は対象外です。
削除前から既存の英単語が変換対象だった場合も、ネイティブの既存本文状態を確認して保護します。
特殊な前方削除・韓国語再合成・句読点再入力に新しい削除フックは追加していません。

元の単体v1.8.2／総合v1.0.157を土台に、既存拡張37クラスをすべて維持し、
DeleteLatinBoundaryを1クラス追加します。Android用SwiftKeyパッチローダーは
既存14クラスを完全維持し、主ローダーへ2呼び出しと新規1クラスを追加します。
元の削除メソッド3本は本文を完全に保存し、前後処理だけを持つラッパーから呼びます。
適用前に対象メソッド全体のSHA-256を確認し、対応しないAPKや二重適用は拒否します。
単体版のJVMローダーにも同じ変更を行い、Android用との追加処理の一致を確認します。
辞書、ポケモン名・スラング、学習、Samsung絵文字、キー配置、横画面、LINE長押し改行、
ULikeを含む他アプリのコード・資産は保持します。

検証: 実際に組み込む削除保護Javaのホスト試験、Unicode判定試験、DEX再展開比較、
既存全クラスの一致、対象ネイティブクラスへの新ローダー適用とDEX再出力、
JVMブートストラップの追加2呼び出し以外の完全一致、MPP全エントリー比較を実施。
ネイティブクラス検査は同版の過去v2 APK内の未変更削除処理を使っています。
テスト用スタブによるホスト試験はSwiftKey変換エンジンやAndroid入力欄の実機試験ではありません。
未改造APKへの今回のパッチ一括適用、Android/ART実行、Galaxy実機での解消は未確認です。
GitHub側の再ビルド結果と、検証済み成果物のSHA-256一致を条件に公開します。

対象: Microsoft SwiftKey Beta 9.13.16.4（versionCode 1236271168）。
Morphe Managerでソースを更新し、元の未改造APK/APKSへ
「日本語入力・予測変換を改善しSamsung絵文字に統一」を再適用してください。
既存と同じ署名で更新インストールします。追加設定やアプリデータ削除は不要です。
ソースの更新だけではインストール済みSwiftKeyの動作は変わりません。
'''

def sha(data): return hashlib.sha256(data).hexdigest()
def require(condition,message):
    if not condition: raise RuntimeError(message)
def run(command,log):
    result=subprocess.run([str(x) for x in command],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    log.parent.mkdir(parents=True,exist_ok=True); log.write_text(result.stdout)
    print(result.stdout,end='',flush=True)
    require(result.returncode==0,'Command failed: '+str(command[0]))
    return result.stdout

def parse_manifest(data):
    lines=[]
    for line in data.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' ') and lines: lines[-1]+=line[1:]
        elif line: lines.append(line)
    return {k.decode('ascii'):v.decode('utf-8') for k,v in (line.split(b': ',1) for line in lines)}
def write_manifest(values):
    lines=[]
    for key,value in values.items():
        line=b''
        for c in key+': '+value:
            raw=c.encode('utf-8')
            if len(line)+len(raw)>70: lines.append(line); line=b' '
            line+=raw
        lines.append(line)
    data=b'\r\n'.join(lines)+b'\r\n\r\n'
    require(parse_manifest(data)==values,'Manifest roundtrip differs'); return data

def files(path): return {p.relative_to(path).as_posix():p.read_bytes() for p in sorted(path.rglob('*.smali'))}
def make_mpp(original,output,extension,classes,boot,patch,old_version,new_version,is_single):
    with zipfile.ZipFile(original) as old:
        names=old.namelist()
        require(old.testzip() is None and len(names)==len(set(names)),'Invalid baseline ZIP')
        values=parse_manifest(old.read(MANIFEST)); require(values['Version']==old_version,'Wrong baseline version')
        values.update(Version=new_version,Timestamp=BUILD_TIMESTAMP,Description=DESCRIPTION)
        updates={MANIFEST:write_manifest(values),EXT:extension,'classes.dex':classes}
        additions={}
        if is_single:
            require(BOOT+'.class' in names and PATCH+'.class' not in names,'Unexpected JVM loader entries')
            updates[BOOT+'.class']=boot; additions[PATCH+'.class']=patch
        with zipfile.ZipFile(output,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as new:
            for info in old.infolist(): new.writestr(copy.copy(info),updates.get(info.filename,old.read(info)))
            for name,data in additions.items():
                info=zipfile.ZipInfo(name,(2026,10,7,2,0,0)); info.compress_type=zipfile.ZIP_DEFLATED; info.external_attr=0o100644<<16
                new.writestr(info,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
        with zipfile.ZipFile(output) as new:
            require(new.testzip() is None and new.namelist()==names+list(additions),'Unexpected final ZIP structure')
            changed=[n for n in names if old.read(n)!=new.read(n)]
            require(set(changed)==set(updates),'Unexpected ZIP changes: '+repr(changed))
            return {'changed_entries':changed,'added_entries':list(additions),'unchanged_entries':len(names)-len(changed),
                    'zip_crc_verified':True,'entries':{n:{'unchanged':n not in updates and n not in additions,
                       'bytes':len(new.read(n)),'sha256':sha(new.read(n))} for n in new.namelist()}}

def patch_bootstrap_smali(original):
    text=original.decode()
    for method,action in [('apply','apply'),('lambda$static$1','checkOriginal')]:
        match=re.search(r'^\.method[^\n]* '+re.escape(method)+r'\([^\n]*\n.*?^\.end method',text,re.M|re.S)
        require(match is not None,'Bootstrap method missing: '+method)
        block=match.group(0)
        pattern=r'(?m)^    invoke-static(?:/range)? \{p0(?: \.\. p0)?\}, Lapp/hiro/swiftkey/patches/JapaneseGlidePatch;->'+action+r'\(Lapp/morphe/patcher/patch/BytecodePatchContext;\)V$'
        anchors=list(re.finditer(pattern,block)); require(len(anchors)==1,'Bootstrap anchor differs: '+method)
        new_call='    invoke-static/range {p0 .. p0}, Lapp/hiro/swiftkey/patches/DeleteProtectionPatch;->'+action+'(Lapp/morphe/patcher/patch/BytecodePatchContext;)V'
        anchor=anchors[0]; changed=block[:anchor.end()]+'\n\n'+new_call+block[anchor.end():]
        require(changed.replace('\n\n'+new_call,'',1)==block,'Original bootstrap changed')
        text=text[:match.start()]+changed+text[match.end():]
    require(text.count('Lapp/hiro/swiftkey/patches/DeleteProtectionPatch;->')==2,'Wrong bootstrap call count')
    return text.encode()

def build(input_dir,tools,work,output):
    for name,pin in PINS.items(): require(sha(((input_dir if name.endswith('.mpp') else tools)/name).read_bytes())==pin,'Pinned input differs: '+name)
    for d in [work,output,output/'evidence',work/'tool_classes',work/'dump_classes',work/'helper_classes',work/'test_classes',work/'patcher_classes',work/'runtime_d8',work/'patcher_d8',work/'bootstrap_smali'/Path(BOOT).parent]: d.mkdir(parents=True,exist_ok=True)
    evidence=output/'evidence'; old_cp=str(tools/'morphe.jar')+os.pathsep+str(input_dir/OLD_SINGLE)
    with zipfile.ZipFile(input_dir/OLD_SINGLE) as s,zipfile.ZipFile(input_dir/OLD_BUNDLE) as b:
        require(s.read(EXT)==b.read(EXT),'Baseline extension differs')
        for n in s.namelist():
            if n.startswith('swiftkey/'): require(s.read(n)==b.read(n),'Dictionary resource differs: '+n)
        (work/'old.mpe').write_bytes(s.read(EXT)); (work/'single-old.dex').write_bytes(s.read('classes.dex'))
        (work/'bundle-old.dex').write_bytes(b.read('classes.dex')); (work/'bootstrap-old.class').write_bytes(s.read(BOOT+'.class'))
    run(['javac','-cp',old_cp,'-d',work/'tool_classes',ROOT/'Dex183.java'],work/'javac-dex.log')
    run(['javac','-cp',tools/'apktool.jar','-d',work/'dump_classes',ROOT/'Dump183.java'],work/'javac-dump.log')
    dex=['java','-cp',str(work/'tool_classes')+os.pathsep+old_cp,'Dex183']
    dump=['java','-cp',str(work/'dump_classes')+os.pathsep+str(tools/'apktool.jar'),'Dump183']
    run(dump+[work/'old.mpe',work/'runtime-before'],work/'dump-runtime-before.log')
    runtime_before=files(work/'runtime-before'); require(len(runtime_before)==37 and HELPER+'.smali' not in runtime_before,'Unexpected existing runtime')
    stubs=sorted((ROOT/'stubs').rglob('*.java'))
    production=[ROOT/'hiro/swiftkey/UnicodeLatinToken.java',ROOT/'hiro/swiftkey/DeleteLatinBoundary.java']
    run(['javac','--release','8','-g:none','-encoding','UTF-8','-d',work/'helper_classes']+stubs+production,work/'javac-runtime.log')
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--output',work/'runtime_d8',work/'helper_classes'/(HELPER+'.class')],work/'d8-runtime.log')
    run(dex+['merge',work/'old.mpe',work/'runtime_d8/classes.dex',work/'new.mpe','-','L'+HELPER+';'],work/'merge-runtime.log')
    run(dump+[work/'new.mpe',work/'runtime-after'],work/'dump-runtime-after.log'); runtime_after=files(work/'runtime-after')
    require(set(runtime_after)==set(runtime_before)|{HELPER+'.smali'},'Runtime class set differs')
    for n,raw in runtime_before.items(): require(runtime_after[n]==raw,'Existing runtime changed: '+n)
    run(dump+[work/'runtime_d8/classes.dex',work/'runtime-helper'],work/'dump-runtime-helper.log')
    require(files(work/'runtime-helper')=={HELPER+'.smali':runtime_after[HELPER+'.smali']},'New helper differs after merge')
    tests=[ROOT/'tests/UnicodeLatinTokenTest.java',ROOT/'tests/DeleteLatinBoundaryTest.java']
    run(['javac','--release','8','-g:none','-encoding','UTF-8','-cp',work/'helper_classes','-d',work/'test_classes']+tests,work/'javac-host-tests.log')
    host_cp=str(work/'test_classes')+os.pathsep+str(work/'helper_classes')
    unicode_tests=run(['java','-cp',host_cp,'UnicodeLatinTokenTest',work/'runtime-after/hiro/swiftkey/CursorLatinBoundary.smali'],evidence/'unicode-tests.txt')
    delete_tests=run(['java','-cp',host_cp,'DeleteLatinBoundaryTest'],evidence/'delete-tests.txt')
    run(['javac','--release','8','-g:none','-encoding','UTF-8','-cp',old_cp,'-d',work/'patcher_classes',ROOT/(PATCH+'.java')],work/'javac-patcher.log')
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--output',work/'patcher_d8',work/'patcher_classes'/(PATCH+'.class')],work/'d8-patcher.log')
    exports=sum((['--add-exports','java.base/jdk.internal.org.objectweb.asm'+s+'=ALL-UNNAMED'] for s in ['', '.tree','.util']),[])
    run(['javac']+exports+['-d',work/'tool_classes',ROOT/'Bootstrap183.java'],work/'javac-bootstrap.log')
    bootstrap_tests=run(['java']+exports+['-cp',work/'tool_classes','Bootstrap183',work/'bootstrap-old.class',work/'bootstrap-new.class'],evidence/'jvm-bootstrap-check.txt')
    for kind in ['single','bundle']:
        run(dump+[work/(kind+'-old.dex'),work/(kind+'-before')],work/('dump-'+kind+'-before.log'))
    single_before=files(work/'single-before'); bundle_before=files(work/'bundle-before')
    require(len(single_before)==15,'Expected 15 standalone Android patcher classes')
    require(single_before=={n:raw for n,raw in bundle_before.items() if n.startswith('app/hiro/swiftkey/')},'Baseline standalone/bundle SwiftKey loaders differ')
    patched_boot=patch_bootstrap_smali(single_before[BOOT+'.smali'])
    (work/'bootstrap_smali'/(BOOT+'.smali')).write_bytes(patched_boot)
    run(dex+['assemble',work/'bootstrap_smali',work/'bootstrap.dex'],work/'assemble-bootstrap.log')
    run(dex+['merge',work/'bootstrap.dex',work/'patcher_d8/classes.dex',work/'patcher-overlay.dex','-','L'+PATCH+';'],work/'merge-overlay.log')
    for kind in ['single','bundle']:
        run(dex+['merge',work/(kind+'-old.dex'),work/'patcher-overlay.dex',work/(kind+'-new.dex'),'L'+BOOT+';','L'+PATCH+';'],work/('merge-'+kind+'.log'))
        run(dump+[work/(kind+'-new.dex'),work/(kind+'-after')],work/('dump-'+kind+'-after.log'))
    single_after=files(work/'single-after'); bundle_after=files(work/'bundle-after')
    for label,before,after in [('single',single_before,single_after),('bundle',bundle_before,bundle_after)]:
        require(set(after)==set(before)|{PATCH+'.smali'},label+' loader class set differs')
        for n,raw in before.items(): require(after[n]==(patched_boot if n==BOOT+'.smali' else raw),'Unexpected '+label+' loader change: '+n)
    require(single_after=={n:raw for n,raw in bundle_after.items() if n.startswith('app/hiro/swiftkey/')},'Final standalone/bundle Android loaders differ')
    run(dump+[work/'patcher_d8/classes.dex',work/'new-patcher'],work/'dump-new-patcher.log')
    require(files(work/'new-patcher')=={PATCH+'.smali':single_after[PATCH+'.smali']},'New JVM-compiled patcher differs after DEX merge')
    for name,raw in {'DeleteLatinBoundary.smali':runtime_after[HELPER+'.smali'],'DeleteProtectionPatch.smali':single_after[PATCH+'.smali'],
                     'before-JapaneseSamsungEmojiPatch.smali':single_before[BOOT+'.smali'],'after-JapaneseSamsungEmojiPatch.smali':single_after[BOOT+'.smali']}.items(): (evidence/name).write_bytes(raw)
    native=json.loads((ROOT/'qa/native-validation.json').read_text()); require(native['status']=='PASSED' and native['checks']==300,'Reviewed native validation missing')
    native_abi=json.loads((ROOT/'qa/native-abi.json').read_text()); require(native_abi['status']=='PASSED' and native_abi['all_exact_descriptors_resolved'],'Reviewed ABI validation missing')
    for n in ['native-validation.json','native-hook-application.txt','native-abi.json','publication-offline.txt']: (evidence/n).write_bytes((ROOT/'qa'/n).read_bytes())
    common=((work/'new.mpe').read_bytes(),(work/'bootstrap-new.class').read_bytes(),(work/'patcher_classes'/(PATCH+'.class')).read_bytes())
    single_report=make_mpp(input_dir/OLD_SINGLE,output/SINGLE,common[0],(work/'single-new.dex').read_bytes(),common[1],common[2],'1.8.2','1.8.3',True)
    bundle_report=make_mpp(input_dir/OLD_BUNDLE,output/BUNDLE,common[0],(work/'bundle-new.dex').read_bytes(),common[1],common[2],'1.0.157','1.0.158',False)
    listing=run(['java','-jar',tools/'morphe.jar','list-patches','--patches',output/SINGLE,'-v','-o'],evidence/'morphe-single-list.txt')
    require('日本語入力・予測変換を改善しSamsung絵文字に統一' in listing and listing.count('Name: ')==1,'Standalone patch listing differs')
    qa={'status':'HOST_AND_STATIC_CHECKS_PASSED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.3','bundle_version':'1.0.158',
        'target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,
        'device_tested':False,'original_apk_apply_tested':False,'device_cause_confirmed':False,'device_fix_confirmed':False,
        'cause_confirmed_in_code':'The existing i/k/w native deletion paths rebuild token/composition metadata without a deletion boundary hook',
        'host_tests_output':{'unicode':unicode_tests,'deletion':delete_tests,'jvm_bootstrap':bootstrap_tests},
        'runtime':{'existing_class_count':37,'unchanged_existing_classes':37,'new_classes':1,
                   'canonical_dex_equality_verified':True,'japanese_mode_guard_preserved':True,'explicit_selection_guard_preserved':True,
                   'fresh_japanese_composition_guard_tested':True,'pure_deletion_and_same_buffer_guard_tested':True,
                   'no_pre_deletion_editor_or_composition_mutation':True,'deletion_hook_verified':True,
                   'hooks':['Lec0/x;->'+s for s in native['native_methods']],
                   'native_operation_executed_once_and_result_preserved':True,'document_mutation_not_added':True},
        'native_method_application_validation':native,'runtime_native_abi_validation':native_abi,'native_tests_replayed_in_ci':False,
        'single_and_bundle_swiftkey_android_loaders_equal':True,'non_swiftkey_android_loaders_unchanged':True,
        'standalone_jvm_and_android_loaders_equivalent':True,
        'loader_equivalence_evidence':'Original loader bodies are preserved except for the same two bootstrap calls in JVM and DEX; the single new loader DEX is compiled from the packaged JVM class',
        'unchanged_other_bundle_android_classes':len(bundle_before)-15,'unchanged_swiftkey_android_loader_classes':14,
        'morphe_desktop_single_list_passed':True,'morphe_manager_device_tested':False,
        'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,
        'base_and_tool_sha256':PINS,'single_preservation':single_report,'bundle_preservation':bundle_report,
        'artifacts':{n:{'bytes':(output/n).stat().st_size,'sha256':sha((output/n).read_bytes())} for n in [SINGLE,BUNDLE]}}
    (output/'QA_SwiftKey_v1.8.3.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    (output/'RELEASE_NOTES.txt').write_text(NOTES)
    finalize(output)
    print('PASS: existing 37 runtime classes and all non-target loaders/assets preserved; three checked native deletion wrappers')
    print(json.dumps(qa['artifacts'],indent=2))
    return qa

def finalize(output):
    source_zip='SwiftKeyBeta_v1.8.3_sources_and_QA.zip'; content={}
    for p in sorted(ROOT.rglob('*')):
        if p.is_file() and p.suffix in ['.java','.py','.json','.txt']:
            content['src/'+p.relative_to(ROOT).as_posix()]=p.read_bytes()
    require('src/publish183.py' in content,'Publication source missing')
    for p in sorted((output/'evidence').rglob('*')):
        if p.is_file(): content['evidence/'+p.relative_to(output/'evidence').as_posix()]=p.read_bytes()
    for n in ['QA_SwiftKey_v1.8.3.json','RELEASE_NOTES.txt']: content[n]=(output/n).read_bytes()
    content['BUILD_INPUTS.json']=(json.dumps({'sha256':PINS,'urls':URLS,'java':'21',
        'command':'python src/build183.py --input input --tools tools --work build --output dist',
        'native_validation':'Optional local re-run: compile src/tests/NativeDeleteHooksTest.java against the built patcher and target the original APK; the APK is not included.'},ensure_ascii=False,indent=2)+'\n').encode()
    with zipfile.ZipFile(output/source_zip,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n,raw in sorted(content.items()):
            info=zipfile.ZipInfo(n,(2026,10,7,2,0,0)); info.compress_type=zipfile.ZIP_DEFLATED; info.external_attr=0o100644<<16
            z.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    with zipfile.ZipFile(output/source_zip) as z: require(z.testzip() is None and {n:z.read(n) for n in z.namelist()}==content,'Source ZIP differs')
    names=[SINGLE,BUNDLE,'QA_SwiftKey_v1.8.3.json',source_zip,'RELEASE_NOTES.txt']
    (output/'SHA256SUMS.txt').write_text(''.join(sha((output/n).read_bytes())+'  '+n+'\n' for n in names))
    print('PASS deterministic source, evidence and checksums')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['input','tools','work','output']: p.add_argument('--'+name,required=True,type=Path)
    args=p.parse_args(); build(args.input.resolve(),args.tools.resolve(),args.work.resolve(),args.output.resolve())
