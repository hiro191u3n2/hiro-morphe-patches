#!/usr/bin/env python3
"""Add a continuous slide picker for native recent emoji; preserve published1.8.22 behavior."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, struct, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.22.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.305.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.23.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.306.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.23.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.23_sources_and_QA.zip'
MF, EXT = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
STAMP = '2026-10-28T05:30:00'
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = tuple(JVM_PREFIX+n+'.class' for n in (
    'JapaneseSamsungEmojiPatch','MethodContract','InputLearningPatch','DictionarySupplementPatch',
    'KeySwapPatch','PasteProtectionPatch','JapaneseGlidePatch','DeleteProtectionPatch','CandidateScrollPatch','CandidateToolbarPatch','PunctuationPatch','JapaneseStartPatch','EmojiFooterPatch'))
ADDED_JVM = set()
DESCRIPTION = '顔文字キーの上フリックから指を離さず最近使った絵文字を選択。4列3段の小窓、離して入力、枠外でキャンセル。既存の感度設定、通常タップと長押しツールバー、下段配置や候補操作を保持。実機未確認。'
BASE = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/eea97cb240c4306a3b1700c4ef4ce27cf14897f4/downloads/'
EXTENSION_PIN = 'eef461b0d8825be2c36d88ef694f62c7f8ec0d756bd8500022738206c0abcf06'
APK_PIN = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
PINS = {
    OLD_SINGLE:'322f2ba927a4c53861e23197155e46af32e6e37017a7b6518f081a01ff8a9052',
    OLD_BUNDLE:'52fe856d66e6b4b89a2280ccc237448905c1b1f2f67c2fea715b0a6d5acaf29d',
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
    info=zipfile.ZipInfo(name,(2026,10,28,5,30,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
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
    paths=[ROOT/n for n in ('build1823.py','publish1823.py','publisher_tests.py','workflow.yml','RELEASE_NOTES.txt','README.txt','audit_runtime_abi.py') if (ROOT/n).is_file()]
    for name in ('src','stubs','tests','tools_java','patch_java'):
        paths.extend(p for p in (ROOT/name).rglob('*') if p.is_file() and p.suffix in ('.java','.py','.json','.txt','.md','.yml','.smali','.b64','.bin'))
    return sorted(set(paths))

def fresh_helper(path): return path.startswith('hiro/swiftkey/EmojiRecents')

def build(inputs,tools,work,output,compile_only=False):
    for name,pin in PINS.items():
        require(sha(((inputs if name.endswith('.mpp') else tools)/name).read_bytes())==pin,'Pinned input differs: '+name)
    if work.exists():shutil.rmtree(work)
    for d in (work,work/'tool_classes',work/'classes',work/'new_dex',output,output/'evidence'):
        d.mkdir(parents=True,exist_ok=True)
    evidence=output/'evidence'
    if (evidence/'runtime').exists():shutil.rmtree(evidence/'runtime')
    old_single,old_bundle=archive(inputs/OLD_SINGLE),archive(inputs/OLD_BUNDLE)
    require(old_single[EXT]==old_bundle[EXT] and sha(old_single[EXT])==EXTENSION_PIN,'Published runtime baseline differs')
    for package in (old_single,old_bundle):
        require({n for n in package if n.startswith(JVM_PREFIX) and n.endswith('.class')}==set(JVM_CLASSES),'Baseline JVM inventory differs')
    require(all(old_single[n]==old_bundle[n] for n in JVM_CLASSES),'Baseline JVM implementations differ')
    (work/'old.mpe').write_bytes(old_single[EXT])
    javac(['-cp',tools/'morphe.jar','-d',work/'tool_classes',ROOT/'tools_java/Merge1823.java'],work/'javac-tools.txt')
    javac(['-cp',tools/'apktool.jar','-d',work/'tool_classes',ROOT/'tools_java/Dump1814.java'],work/'javac-dump.txt')
    dump=['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'),'Dump1814']
    run(dump+[work/'old.mpe',work/'before'],work/'dump-before.txt')
    before=smali_files(work/'before')
    require(len(before)==84 and not any(fresh_helper(n) for n in before),'Unexpected baseline runtime inventory')
    production=[ROOT/'src/hiro/swiftkey'/name for name in ('EmojiRecentsSlide.java','EmojiRecentsBridge.java','EmojiToolbar.java')]
    stubs=sorted((ROOT/'stubs').rglob('*.java'))
    javac(['--release','8','-g:none','-encoding','UTF-8','-cp',tools/'android.jar','-d',work/'classes',*stubs,*production],work/'javac-runtime.txt')
    with zipfile.ZipFile(work/'runtime.jar','w') as runtime,zipfile.ZipFile(work/'stubs.jar','w') as stubjar:
        for p in sorted((work/'classes').rglob('*.class')):
            name=p.relative_to(work/'classes').as_posix()
            (runtime if helper(name.replace('.class','.smali')) else stubjar).write(p,name)
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar',
         '--classpath',work/'stubs.jar','--output',work/'new_dex',work/'runtime.jar'],work/'d8.txt')
    runtime_java=os.environ.get('MORPHE_JAVA','java')
    run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'),'Merge1823',work/'old.mpe',
         work/'new_dex/classes.dex',work/'new.mpe',evidence/'merge-review.json'],evidence/'merge.txt')
    for name,path in [('after',work/'new.mpe'),('overlay',work/'new_dex/classes.dex')]:
        run(dump+[path,work/name],work/('dump-'+name+'.txt'))
    after,overlay=(smali_files(work/n) for n in ('after','overlay'))
    require(overlay and all(helper(n) for n in overlay),'Unexpected compiled runtime overlay')
    changed=sorted(n for n in before if before[n]!=after.get(n));added=sorted(set(after)-set(before))
    require(changed==['hiro/swiftkey/EmojiToolbar.smali','hiro/swiftkey/JapaneseGlide.smali'],'Unrelated runtime class changed')
    require(set(before).issubset(after) and added and all(fresh_helper(n) for n in added),'Runtime additions/removals differ')
    require(set(added)=={n for n in overlay if fresh_helper(n)},'Compiled helper inventory differs')
    require(all(after[n]==overlay[n] for n in added),'Merged new helper differs from compiled source')
    integration=json.loads((evidence/'merge-review.json').read_text())
    require(integration.get('status')=='PASSED' and integration.get('baseline_classes')==84
            and integration.get('unchanged_existing_classes')==82,'Runtime preservation report failed')
    for behavior in ('all_original_method_bodies_preserved','original_glide_restored_byte_identical',
                     'original_toolbar_restored_byte_identical','original_dispatch_called_only_when_not_consumed',
                     'init_and_reset_call_original_and_helper_once','wrapper_arguments_and_branch_verified',
                     'compiled_new_helpers_preserved','emoji_key_helper_matches_compiled_source'):
        require(integration.get(behavior) is True,'Missing integration proof: '+behavior)
    for name in sorted(set(added)|set(changed)):
        path=evidence/'runtime'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(after[name])
    extension=(work/'new.mpe').read_bytes()
    for forbidden in (b'hiro_up_flick_diagnostics','上フリック診断'.encode(),b'lastCaptureResult',b'abortedAfterCapture'):
        require(forbidden not in b'\n'.join(after.values()),'Removed diagnostics reintroduced')
    if compile_only:
        print('COMPILED ONLY for independent ABI review: '+str(work/'new.mpe'),flush=True)
        return {'extension_sha256':sha(extension),'release_ready':False,'runtime_changed':changed,'runtime_added':added}

    abi_path=ROOT/'tests/native_emoji_slide_abi1823.json'
    abi=json.loads(abi_path.read_text())
    require(abi.get('status')=='PASSED' and not abi.get('unresolved') and not abi.get('inaccessible')
            and not abi.get('incompatible_opcodes'),'Emoji slide runtime ABI review failed')
    require(abi.get('extension_sha256')==sha(extension) and abi.get('apk_sha256')==APK_PIN,'ABI evidence targets different bytes')
    require(abi.get('device_tested') is False and abi.get('original_apk_apply_tested') is False,'ABI evidence scope differs')
    require(abi.get('verifier_source_sha256')==sha((ROOT/'tools_java/AuditEmojiSlideAbi.java').read_bytes())
            and abi.get('wrapper_source_sha256')==sha((ROOT/'tests/audit_emoji_slide_abi1823.py').read_bytes()),'ABI verifier source changed')
    required_abi={'L'+n.removesuffix('.smali')+';' for n in after if helper(n)}|{'Lhiro/swiftkey/JapaneseGlide;'}
    require(set(abi.get('audited_classes',[]))==required_abi,'Gesture/bridge/changed-caller ABI coverage incomplete')
    review_path=ROOT/'tests/native_emoji_slide_review.json'
    review=json.loads(review_path.read_text())
    require(review.get('status')=='PASSED' and review.get('apk_sha256')==APK_PIN,'Native history route review failed')
    require(review.get('device_tested') is False and review.get('original_apk_apply_tested') is False,'Native review scope differs')
    for name,pin in review.get('source_sha256',{}).items():
        require(sha((ROOT/name).read_bytes())==pin,'Native review source changed: '+name)
    require(review.get('source_sha256'),'Missing native review source pins')
    tests_work=work/'emoji_slide_tests'
    test_output=run(['python',ROOT/'tests/run_emoji_slide_tests.py','--source-dir',ROOT,'--work',tests_work],evidence/'emoji-slide-tests.txt')
    tests=json.loads((tests_work/'report.json').read_text())
    require('PASS' in test_output and tests.get('status')=='PASSED','Emoji slide host tests failed')
    require(tests.get('production_source_sha256')==sha(production[0].read_bytes()),'Host tests target different gesture source')
    require(tests.get('production_source_sha256_by_file')=={p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in production},
            'Host tests target different native bridge/toolbar source')
    require(tests.get('device_tested') is False and tests.get('android_instrumentation_tested') is False,'Host test scope differs')
    require(tests.get('scenarios',0)>0 and tests.get('assertions',0)>0,'Missing host behavior assertions')
    for behavior in ('native_emoji_key_identity_verified','recent_order_limit_and_graphemes_verified',
                     'native_input_and_history_delegation_verified','private_history_guard_delegation_verified',
                     'input_once_and_failure_no_retry_verified','upward_sensitivity_verified','ordinary_tap_and_hold_preserved',
                     'continuous_slide_release_selection_verified','outside_release_and_cancel_verified',
                     'lifecycle_and_multitouch_drain_verified','popup_geometry_and_no_focus_steal_verified'):
        require(tests.get(behavior) is True,'Missing gesture/history coverage: '+behavior)
    single=make_mpp(inputs/OLD_SINGLE,output/SINGLE,extension,'1.8.22','1.8.23',{})
    bundle=make_mpp(inputs/OLD_BUNDLE,output/BUNDLE,extension,'1.0.305','1.0.306',{})
    new_single,new_bundle=archive(output/SINGLE),archive(output/BUNDLE)
    require(new_single[EXT]==new_bundle[EXT],'Runtime differs across packages')
    require(all(new_single[n]==new_bundle[n]==old_single[n]==old_bundle[n] for n in JVM_CLASSES),'Existing JVM loader changed')
    require(new_single['classes.dex']==old_single['classes.dex'] and new_bundle['classes.dex']==old_bundle['classes.dex'],
            'Existing Android patch loader changed')
    dex_count=struct.unpack_from('<I',new_bundle['classes.dex'],96)[0];require(dex_count==280,'Existing loader inventory differs')
    javac(['--release','8','-g:none','-encoding','UTF-8','-d',work/'tool_classes',ROOT/'tests/LoaderParityProbe.java'],work/'javac-probe.txt')
    parity=[]
    for tool,suffix in [('morphe.jar',''),('morphe-current.jar','-current')]:
        path=evidence/('loader-parity'+suffix+'.json')
        run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/tool),'LoaderParityProbe',inputs/OLD_BUNDLE,
             output/SINGLE,output/BUNDLE,path],evidence/('loader-probe'+suffix+'.txt'))
        report=json.loads(path.read_text())
        require(report.get('status')=='PASSED' and report.get('selected_extension_stream_verified') is True
                and report.get('selected_extension_sha256')==sha(extension) and report.get('baseline_extension_sha256')==EXTENSION_PIN,
                'Real loader selected extension differs')
        parity.append(report)
    require(parity[0]==parity[1],'Morphe SDKs disagree')
    qa={'status':'EMOJI_RECENT_SLIDE_HOST_AND_ABI_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.23','bundle_version':'1.0.306',
        'baseline_swiftkey_version':'1.8.22','baseline_bundle_version':'1.0.305',
        'target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,
        'device_tested':False,'original_apk_apply_tested':False,'device_fix_confirmed':False,
        'runtime_unrelated_classes_unchanged':True,'all_original_runtime_method_bodies_preserved':True,
        'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,
        'standalone_and_bundle_runtime_equal':True,'loader_entries_byte_identical':True,'unrelated_loader_classes_unchanged':True,
        'jvm_swiftkey_loader_matches_standalone':True,'combined_jvm_swiftkey_discovered':True,'previous_jvm_patches_preserved':True,
        'all_static_checks_passed':True,'host_tests_passed':True,'current_manager_patcher_selection_verified':True,
        'two_loader_sdks_agree':True,'selected_extension_stream_verified':True,'loader_selected_extension_matches_packaged_runtime':True,
        'diagnostic_function_removed':True,'candidate_scroll_1822_byte_identical':True,'japanese_start_1822_byte_identical':True,
        'candidate_toolbar_1822_byte_identical':True,'emoji_footer_1822_byte_identical':True,'punctuation_1822_byte_identical':True,
        'flick_sensitivity_1822_byte_identical':True,'ordinary_emoji_tap_and_hold_preserved':True,
        'native_abi_review_verified':True,'native_abi_replayed_in_ci':False,'native_history_review_verified':True,
        'recent_emoji_limit':12,'recent_emoji_columns':4,'native_history_and_input_used':True,
        'continuous_slide_selection_on_release':True,'outside_release_cancels':True,'sensitivity_setting_reused':True,
        'native_abi_review':{'source':'tests/native_emoji_slide_abi1823.json','sha256':sha(abi_path.read_bytes()),
                             'apk_sha256':APK_PIN,'extension_sha256':sha(extension)},
        'native_history_review':{'source':'tests/native_emoji_slide_review.json','sha256':sha(review_path.read_bytes()),'report':review},
        'emoji_slide_test_report':tests,'runtime_integration':integration,
        'runtime':{'before_classes':len(before),'after_classes':len(after),'unchanged_existing_classes':82,
                   'changed':changed,'added':added,'removed':[],'extension_sha256':sha(extension)},
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
    print('PASS SwiftKey1.8.23 /bundle306 recent emoji continuous slide picker',flush=True)
    return qa

def main():
    p=argparse.ArgumentParser()
    for name in ('input-dir','tools','work','output'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--download',action='store_true');p.add_argument('--compile-only',action='store_true')
    a=p.parse_args();inputs=a.input_dir.resolve();tools=a.tools.resolve();work=a.work.resolve();output=a.output.resolve()
    if a.download:download(inputs,tools)
    build(inputs,tools,work,output,a.compile_only)
if __name__=='__main__':main()

