#!/usr/bin/env python3
"""Add the native emoji footer Enter key and preserve all published 1.8.21 features."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, struct, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.21.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.304.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.22.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.305.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.22.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.22_sources_and_QA.zip'
MF, EXT = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
STAMP = '2026-10-28T05:00:00'
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = tuple(JVM_PREFIX+n+'.class' for n in (
    'JapaneseSamsungEmojiPatch','MethodContract','InputLearningPatch','DictionarySupplementPatch',
    'KeySwapPatch','PasteProtectionPatch','JapaneseGlidePatch','DeleteProtectionPatch','CandidateScrollPatch','CandidateToolbarPatch','PunctuationPatch','JapaneseStartPatch'))
ADDED_JVM = {JVM_PREFIX+'EmojiFooterPatch.class'}
DESCRIPTION = '絵文字パネルの下段をabc・エンター・削除・カテゴリの順に配置。通常のEnter処理と既存の削除・カテゴリ操作を利用。上フリック感度・候補スクロール・既存改造を保持。実機未確認。'
BASE = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/ece0bf5740027de096681fd51223a1adc416bf4f/downloads/'
EXTENSION_PIN = 'e436cdc71432ed7066335d9516d5ac2d233f4a39f4014ab7ce3787ba7d3a9593'
APK_PIN = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
PINS = {
    OLD_SINGLE:'000076673c9253acb1edb3a11ebf9889396c4977d8ea7788e52275b16190d774',
    OLD_BUNDLE:'1523f42ee8597475c8d4d8b1625820fd874408090881c88f9e0107583fc00d7f',
    'apktool.jar':'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-current.jar':'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
    'r8.jar':'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'}
URLS = {
    OLD_SINGLE:BASE+OLD_SINGLE,OLD_BUNDLE:BASE+OLD_BUNDLE,
    'apktool.jar':'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
    'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
    'morphe-current.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar',
    'r8.jar':'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
    'android.jar':'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar'}
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
def helper(path): return any(path==f'hiro/swiftkey/{name}.smali' or path.startswith(f'hiro/swiftkey/{name}$') for name in ('EmojiFooter',))
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
    info=zipfile.ZipInfo(name,(2026,10,28,5,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
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
    paths=[ROOT/n for n in ('build1822.py','publish1822.py','publisher_tests.py','workflow.yml','RELEASE_NOTES.txt','README.txt','audit_runtime_abi.py') if (ROOT/n).is_file()]
    for name in ('src','stubs','tests','tools_java','patch_java'):
        paths.extend(p for p in (ROOT/name).rglob('*') if p.is_file() and p.suffix in ('.java','.py','.json','.txt','.md','.yml','.smali','.b64','.bin'))
    return sorted(set(paths))

def build(inputs,tools,work,output,compile_only=False):
    for name,pin in PINS.items():
        require(sha(((inputs if name.endswith('.mpp') else tools)/name).read_bytes())==pin,'Pinned input differs: '+name)
    if work.exists():shutil.rmtree(work)
    for d in (work,work/'tool_classes',work/'classes',work/'new_dex',output,output/'evidence'):
        d.mkdir(parents=True,exist_ok=True)
    evidence=output/'evidence'
    if (evidence/'runtime').exists():shutil.rmtree(evidence/'runtime')
    old_single,old_bundle=archive(inputs/OLD_SINGLE),archive(inputs/OLD_BUNDLE)
    require(old_single[EXT]==old_bundle[EXT] and sha(old_single[EXT])==EXTENSION_PIN,'Published baseline runtime differs')
    for package in (old_single,old_bundle):
        require({n for n in package if n.startswith(JVM_PREFIX) and n.endswith('.class')}==set(JVM_CLASSES),'Baseline JVM inventory differs')
    require(all(old_single[n]==old_bundle[n] for n in JVM_CLASSES),'Baseline JVM implementations differ')
    (work/'old.mpe').write_bytes(old_single[EXT])
    javac(['-cp',tools/'morphe.jar','-d',work/'tool_classes',ROOT/'tools_java/Merge1822.java'],work/'javac-tools.txt')
    javac(['-cp',tools/'apktool.jar','-d',work/'tool_classes',ROOT/'tools_java/Dump1814.java'],work/'javac-dump.txt')
    dump=['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'),'Dump1814']
    run(dump+[work/'old.mpe',work/'before'],work/'dump-before.txt')
    before=smali_files(work/'before')
    require(len(before)==81 and not any(helper(n) for n in before),'Unexpected baseline runtime inventory')
    production=ROOT/'src/hiro/swiftkey/EmojiFooter.java'
    stubs=sorted((ROOT/'stubs').rglob('*.java'))
    javac(['--release','8','-g:none','-encoding','UTF-8','-cp',tools/'android.jar','-d',work/'classes',*stubs,production],work/'javac-runtime.txt')
    with zipfile.ZipFile(work/'runtime.jar','w') as runtime,zipfile.ZipFile(work/'stubs.jar','w') as stubjar:
        for p in sorted((work/'classes').rglob('*.class')):
            name=p.relative_to(work/'classes').as_posix()
            (runtime if helper(name.replace('.class','.smali')) else stubjar).write(p,name)
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar','--classpath',work/'stubs.jar','--output',work/'new_dex',work/'runtime.jar'],work/'d8.txt')
    runtime_java=os.environ.get('MORPHE_JAVA','java')
    run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'),'Merge1822',work/'old.mpe',work/'new_dex/classes.dex',work/'new.mpe',evidence/'merge-review.json'],evidence/'merge.txt')
    for name,path in [('after',work/'new.mpe'),('overlay',work/'new_dex/classes.dex')]:
        run(dump+[path,work/name],work/('dump-'+name+'.txt'))
    after,overlay=(smali_files(work/n) for n in ('after','overlay'))
    require(overlay and all(helper(n) for n in overlay),'Unexpected runtime overlay')
    require(all(after.get(n)==raw for n,raw in before.items()),'An existing runtime class changed')
    require(not set(before)&set(overlay) and set(after)==set(before)|set(overlay),'Runtime addition inventory differs')
    require(all(after[n]==raw for n,raw in overlay.items()),'Merged runtime differs from compiled helper')
    added=sorted(overlay)
    integration=json.loads((evidence/'merge-review.json').read_text())
    require(integration.get('status')=='PASSED' and integration.get('all_existing_runtime_classes_byte_identical') is True
            and integration.get('unchanged_existing_classes')==81,'Runtime preservation review failed')
    for name,raw in overlay.items():
        path=evidence/'runtime'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
    extension=(work/'new.mpe').read_bytes()
    for forbidden in (b'hiro_up_flick_diagnostics','上フリック診断'.encode(),b'lastCaptureResult',b'abortedAfterCapture'):
        require(forbidden not in b'\n'.join(after.values()),'Removed diagnostics reintroduced')
    if compile_only:
        print('COMPILED ONLY for independent ABI review: '+str(work/'new.mpe'),flush=True)
        return {'extension_sha256':sha(extension),'release_ready':False,'runtime_changed':[],'runtime_added':added}

    abi_path=ROOT/'tests/native_emoji_footer_abi1822.json'
    abi=json.loads(abi_path.read_text())
    require(abi.get('status')=='PASSED' and not abi.get('unresolved') and not abi.get('inaccessible')
            and not abi.get('incompatible_opcodes'),'Emoji footer runtime ABI review failed')
    require(abi.get('extension_sha256')==sha(extension) and abi.get('apk_sha256')==APK_PIN,'ABI evidence targets different bytes')
    require(abi.get('device_tested') is False and abi.get('original_apk_apply_tested') is False,'ABI evidence scope differs')
    require(abi.get('verifier_source_sha256')==sha((ROOT/'tools_java/AuditEmojiFooterAbi.java').read_bytes())
            and abi.get('wrapper_source_sha256')==sha((ROOT/'tests/audit_emoji_footer_abi1822.py').read_bytes()),'ABI verifier sources changed')
    require(set(abi.get('audited_classes',[]))=={'L'+n.removesuffix('.smali')+';' for n in overlay},'Emoji footer ABI coverage incomplete')
    require(abi.get('interface_callbacks_checked') is True
            and all(row.get('implemented') is True for row in abi.get('interface_callback_implementations',[])),
            'Footer click or theme observer interface is incompatible')
    archive_review_path=ROOT/'tests/native_emoji_footer_archive_review.json'
    archive_review=json.loads(archive_review_path.read_text())
    require(archive_review.get('status')=='PASSED' and archive_review.get('apk_sha256')==APK_PIN,
            'Native archive hook evidence failed or targets different bytes')
    require(archive_review.get('baseline_mpp_sha256')=={'single':PINS[OLD_SINGLE],'bundle':PINS[OLD_BUNDLE]},
            'Native archive hook evidence has a different baseline')
    require(archive_review.get('device_tested') is False and archive_review.get('original_apk_apply_tested') is False
            and archive_review.get('native_archive_replayed_in_ci') is False,'Native archive evidence scope differs')
    require(set(archive_review.get('source_sha256',{}))=={'patch_java/app/hiro/swiftkey/patches/EmojiFooterPatch.java',
            'tools_java/UpdateEmojiFooterLoader.java','tests/EmojiFooterPatchProbe.java','tests/build_emoji_footer_loader.py'},
            'Native archive evidence source coverage incomplete')
    for name,pin in archive_review['source_sha256'].items():
        require(sha((ROOT/name).read_bytes())==pin,'Native archive evidence source changed: '+name)
    archive_tests=archive_review.get('native_archive_hook_tests',{})
    require(set(archive_tests)=={'archived_sdk','current_sdk'}
            and archive_tests['archived_sdk']==archive_tests['current_sdk'],'Native archive SDK evidence disagrees')
    require(archive_tests['current_sdk'].get('status')=='PASSED'
            and archive_tests['current_sdk'].get('native_fixture') is False
            and archive_tests['current_sdk'].get('native_enter_method_byte_identical') is True,
            'Actual native archive mutation evidence incomplete')
    tests_work=work/'emoji_footer_tests'
    test_output=run(['python',ROOT/'tests/run_emoji_footer_tests.py','--source-dir',ROOT,'--work',tests_work],evidence/'emoji-footer-tests.txt')
    tests=json.loads((tests_work/'report.json').read_text())
    require('PASS' in test_output and tests.get('status')=='PASSED','Emoji footer host tests failed')
    require(tests.get('production_source_sha256')==sha(production.read_bytes()),'Host tests target different production source')
    require(tests.get('device_tested') is False and tests.get('android_instrumentation_tested') is False,'Host test scope differs')
    require(tests.get('scenarios',0)>0 and tests.get('assertions',0)>0,'Missing host behavior tests')
    for behavior in ('native_view_identity_preserved','layout_order_verified','emoji_scope_verified',
                     'repeated_install_idempotent','narrow_width_verified','native_enter_dispatch_verified',
                     'failure_guards_verified','theme_following_verified'):
        require(tests.get(behavior) is True,'Missing footer behavior coverage: '+behavior)
    loader_work=work/'emoji_footer_loader'
    loader_text=run(['python',ROOT/'tests/build_emoji_footer_loader.py','--source-dir',ROOT,'--tools',tools,
                    '--work',loader_work,'--standalone',inputs/OLD_SINGLE,'--bundle',inputs/OLD_BUNDLE],evidence/'emoji-footer-loader.txt')
    loader_report=json.loads((loader_work/'report.json').read_text())
    require('PASS' in loader_text and loader_report.get('status')=='PASSED','Footer patch loader checks failed')
    require(loader_report.get('jvm_main_only_two_calls_added') is True and loader_report.get('native_hook_sdks_agree') is True,
            'Existing loader preservation or SDK parity failed')
    jvm_updates={JVM_PREFIX+n:(loader_work/'jvm'/JVM_PREFIX/n).read_bytes()
                 for n in ('JapaneseSamsungEmojiPatch.class','EmojiFooterPatch.class')}
    require(sha(jvm_updates[JVM_PREFIX+'EmojiFooterPatch.class'])==archive_review.get('loader_helper_sha256'),
            'Packaged footer patch differs from actual APK transformation evidence')
    single=make_mpp(inputs/OLD_SINGLE,output/SINGLE,extension,'1.8.21','1.8.22',
                    {**jvm_updates,'classes.dex':(loader_work/'single.dex').read_bytes()})
    bundle=make_mpp(inputs/OLD_BUNDLE,output/BUNDLE,extension,'1.0.304','1.0.305',
                    {**jvm_updates,'classes.dex':(loader_work/'bundle.dex').read_bytes()})
    new_single,new_bundle=archive(output/SINGLE),archive(output/BUNDLE)
    require(new_single[EXT]==new_bundle[EXT],'New runtime differs across packages')
    require(all(new_single[n]==new_bundle[n]==old_single[n]==old_bundle[n] for n in JVM_CLASSES if n not in jvm_updates),
            'Unrelated JVM loader changed')
    require(all(new_single[n]==new_bundle[n] for n in jvm_updates),'Added JVM hooks differ across packages')
    dex_count=struct.unpack_from('<I',new_bundle['classes.dex'],96)[0]
    require(dex_count==280,'Combined Android loader inventory differs')
    javac(['--release','8','-g:none','-encoding','UTF-8','-d',work/'tool_classes',ROOT/'tests/LoaderParityProbe.java'],work/'javac-probe.txt')
    parity=[]
    for tool,suffix in [('morphe.jar',''),('morphe-current.jar','-current')]:
        path=evidence/('loader-parity'+suffix+'.json')
        run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/tool),'LoaderParityProbe',
             inputs/OLD_BUNDLE,output/SINGLE,output/BUNDLE,path],evidence/('loader-probe'+suffix+'.txt'))
        report=json.loads(path.read_text())
        require(report.get('status')=='PASSED' and report.get('selected_extension_stream_verified') is True
                and report.get('selected_extension_sha256')==sha(extension) and report.get('baseline_extension_sha256')==EXTENSION_PIN,
                'Real loader selected extension differs')
        parity.append(report)
    require(parity[0]==parity[1],'Morphe SDKs disagree on patch selection')
    qa={'status':'EMOJI_FOOTER_HOST_AND_NATIVE_HOOK_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.22','bundle_version':'1.0.305',
        'baseline_swiftkey_version':'1.8.21','baseline_bundle_version':'1.0.304',
        'target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,
        'device_tested':False,'original_apk_apply_tested':False,'device_fix_confirmed':False,
        'runtime_unrelated_classes_unchanged':True,'all_existing_runtime_classes_byte_identical':True,
        'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,
        'standalone_and_bundle_runtime_equal':True,'loader_entries_byte_identical':False,'unrelated_loader_classes_unchanged':True,
        'jvm_swiftkey_loader_matches_standalone':True,'combined_jvm_swiftkey_discovered':True,'previous_jvm_patches_preserved':True,
        'all_static_checks_passed':True,'host_tests_passed':True,'current_manager_patcher_selection_verified':True,
        'two_loader_sdks_agree':True,'selected_extension_stream_verified':True,'loader_selected_extension_matches_packaged_runtime':True,
        'diagnostic_function_removed':True,'candidate_scroll_1821_byte_identical':True,'japanese_start_1821_byte_identical':True,
        'candidate_toolbar_1821_byte_identical':True,'emoji_toolbar_1821_byte_identical':True,'punctuation_1821_byte_identical':True,
        'flick_sensitivity_1821_byte_identical':True,'native_abi_review_verified':True,'native_abi_replayed_in_ci':False,
        'native_archive_hook_review_verified':True,'native_archive_hook_replayed_in_ci':False,
        'emoji_footer_order':['abc','enter','delete','categories'],'emoji_footer_only':True,'native_enter_pipeline_used':True,
        'native_delete_widget_preserved':True,'native_scrollable_categories_preserved':True,
        'native_abi_review':{'source':'tests/native_emoji_footer_abi1822.json','sha256':sha(abi_path.read_bytes()),
                             'apk_sha256':APK_PIN,'extension_sha256':sha(extension)},
        'native_archive_hook_review':{'source':'tests/native_emoji_footer_archive_review.json',
                                     'sha256':sha(archive_review_path.read_bytes()),'report':archive_review},
        'emoji_footer_test_report':tests,'runtime_integration':integration,'emoji_footer_loader_report':loader_report,
        'runtime':{'before_classes':len(before),'after_classes':len(after),'unchanged_existing_classes':81,
                    'changed':[],'added':added,'removed':[],'extension_sha256':sha(extension)},
        'runtime_sha256':sha(extension),'android_loader_class_count':dex_count,'loader_parity':parity[0],
        'single':single,'bundle':bundle,'inputs':{n:{'url':URLS[n],'sha256':pin} for n,pin in PINS.items()}}
    (output/QA_NAME).write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    (output/'RELEASE_NOTES.txt').write_bytes((ROOT/'RELEASE_NOTES.txt').read_bytes())
    with zipfile.ZipFile(output/SOURCE_ZIP,'w') as z:
        for p in source_paths():zip_add(z,'src/'+p.relative_to(ROOT).as_posix(),p.read_bytes())
        for p in sorted((evidence/'runtime').rglob('*.smali')):zip_add(z,'evidence/runtime/'+p.relative_to(evidence/'runtime').as_posix(),p.read_bytes())
        zip_add(z,QA_NAME,(output/QA_NAME).read_bytes())
        for suffix in ('','-current'):zip_add(z,'evidence/loader-parity'+suffix+'.json',(evidence/('loader-parity'+suffix+'.json')).read_bytes())
    names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
    (output/'SHA256SUMS.txt').write_text(''.join(sha((output/n).read_bytes())+'  '+n+'\n' for n in names))
    print('PASS SwiftKey1.8.22 /bundle305 native emoji footer Enter and ordering',flush=True)
    return qa

def main():
    p=argparse.ArgumentParser()
    for name in ('input-dir','tools','work','output'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--download',action='store_true');p.add_argument('--compile-only',action='store_true')
    a=p.parse_args();inputs=a.input_dir.resolve();tools=a.tools.resolve();work=a.work.resolve();output=a.output.resolve()
    if a.download:download(inputs,tools)
    build(inputs,tools,work,output,a.compile_only)
if __name__=='__main__':main()

