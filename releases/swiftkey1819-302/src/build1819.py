#!/usr/bin/env python3
"""Build candidate-scroll corrections and Japanese session startup from pinned 1.8.18."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, struct, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.18.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.301.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.19.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.302.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.19.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.19_sources_and_QA.zip'
MF, EXT = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
STAMP = '2026-10-28T00:01:00'
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = tuple(JVM_PREFIX+n+'.class' for n in (
    'JapaneseSamsungEmojiPatch','MethodContract','InputLearningPatch','DictionarySupplementPatch',
    'KeySwapPatch','PasteProtectionPatch','JapaneseGlidePatch','DeleteProtectionPatch','CandidateScrollPatch','CandidateToolbarPatch','PunctuationPatch'))
ADDED_JVM = {JVM_PREFIX+'JapaneseStartPatch.class'}
DESCRIPTION = '展開候補の更新によるスクロール位置と並びの飛びを追加修正。通常の文字入力欄でキーボードを開くたび日本語で開始。入力中の手動言語切替、記号上フリック、左側開閉、絵文字操作、辞書を保持。実機未確認。'
BASE = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/177e441b49f43f055758d32fba909b03b9c88a4b/downloads/'
EXTENSION_PIN = '6e042bf39b88348d79feda819816590c1038048f58593e83d5609e2c8d373f2c'
APK_PIN = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
PINS = {
    OLD_SINGLE:'35251f48280adc69108d0218b3a8f464686deeb44314f59195698bc6bc8583ef',
    OLD_BUNDLE:'3fbc234e9248d0e2682ee996a31cd37d5b0b079a080ad2b8fc2b65ce6e65f163',
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
def helper(path): return any(path==f'hiro/swiftkey/{name}.smali' or path.startswith(f'hiro/swiftkey/{name}$') for name in ('CandidateScroll','JapaneseStart'))
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
    info=zipfile.ZipInfo(name,(2026,10,28,0,1,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
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
    paths=[ROOT/n for n in ('build1819.py','publish1819.py','publisher_tests.py','workflow.yml','RELEASE_NOTES.txt','README.txt','audit_runtime_abi.py') if (ROOT/n).is_file()]
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
    require(old_single[EXT]==old_bundle[EXT] and sha(old_single[EXT])==EXTENSION_PIN,'Baseline runtime differs')
    for package in (old_single,old_bundle):
        require({n for n in package if n.startswith(JVM_PREFIX) and n.endswith('.class')}==set(JVM_CLASSES),'Baseline JVM classes differ')
    require(all(old_single[n]==old_bundle[n] for n in JVM_CLASSES),'Baseline JVM classes unequal')
    (work/'old.mpe').write_bytes(old_single[EXT])
    javac(['-cp',tools/'morphe.jar','-d',work/'tool_classes',ROOT/'tools_java/Merge1819.java'],work/'javac-tools.txt')
    javac(['-cp',tools/'apktool.jar','-d',work/'tool_classes',ROOT/'tools_java/Dump1814.java'],work/'javac-dump.txt')
    dump=['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'apktool.jar'),'Dump1814']
    run(dump+[work/'old.mpe',work/'before'],work/'dump-before.txt')
    before=smali_files(work/'before')
    require(len(before)==76 and len([n for n in before if helper(n)])==5,'Unexpected baseline inventory')
    production=[ROOT/'src/hiro/swiftkey/CandidateScroll.java',ROOT/'src/hiro/swiftkey/JapaneseStart.java']
    stubs=sorted((ROOT/'stubs').rglob('*.java'))
    require(all(p.is_file() for p in production),'Missing production sources')
    javac(['--release','8','-g:none','-encoding','UTF-8','-cp',tools/'android.jar','-d',work/'classes',*stubs,*production],work/'javac-runtime.txt')
    with zipfile.ZipFile(work/'runtime.jar','w') as runtime,zipfile.ZipFile(work/'stubs.jar','w') as stubjar:
        for p in sorted((work/'classes').rglob('*.class')):
            name=p.relative_to(work/'classes').as_posix()
            (runtime if helper(name.replace('.class','.smali')) else stubjar).write(p,name)
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar','--classpath',work/'stubs.jar','--output',work/'new_dex',work/'runtime.jar'],work/'d8.txt')
    run(['java','-cp',str(work/'tool_classes')+os.pathsep+str(tools/'morphe.jar'),'Merge1819',work/'old.mpe',work/'new_dex/classes.dex',work/'new.mpe'],evidence/'merge.txt')
    for name,path in [('after',work/'new.mpe'),('overlay',work/'new_dex/classes.dex')]:
        run(dump+[path,work/name],work/('dump-'+name+'.txt'))
    after,overlay=(smali_files(work/n) for n in ('after','overlay'))
    require(overlay and all(helper(n) for n in overlay),'Unexpected runtime overlay')
    require(all(after.get(n)==v for n,v in before.items() if not helper(n)),'Unrelated runtime changed')
    require(set(before).issubset(after),'Existing runtime class removed')
    require({n for n in before if helper(n)}.issubset(overlay),'Existing candidate class removed')
    require(set(after)==set(before)|set(overlay),'Unexpected new runtime inventory')
    changed=sorted(n for n in before if before[n]!=after[n]);added=sorted(set(after)-set(before))
    require(all(helper(n) for n in changed+added),'Unexpected runtime changes')
    require(compile_only or 'hiro/swiftkey/CandidateScroll.smali' in changed,'Candidate correction missing')
    require('hiro/swiftkey/JapaneseStart.smali' in added,'Japanese startup runtime missing')
    require(all(after[n]==v for n,v in overlay.items()),'Merged overlay differs')
    for name,raw in overlay.items():
        path=evidence/'runtime'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(raw)
    extension=(work/'new.mpe').read_bytes()
    for forbidden in (b'hiro_up_flick_diagnostics','上フリック診断'.encode(),b'lastCaptureResult',b'abortedAfterCapture'):
        require(forbidden not in b'\n'.join(after.values()),'Removed diagnostics reintroduced')
    if compile_only:
        print('COMPILED ONLY for independent ABI review: '+str(work/'new.mpe'),flush=True)
        return {'extension_sha256':sha(extension),'release_ready':False,'runtime_changed':changed,'runtime_added':added}

    abi_path=ROOT/'tests/native_abi_review1819.json';require(abi_path.is_file(),'Missing runtime ABI review')
    abi=json.loads(abi_path.read_text())
    require(abi.get('status')=='PASSED' and not abi.get('unresolved') and not abi.get('inaccessible') and not abi.get('incompatible_opcodes'),'Runtime ABI review failed')
    require(abi.get('native_hook_contract_verified') is True and len(abi.get('native_hook_entrypoints',[]))==8,'Injected runtime entrypoint ABI coverage incomplete')
    require(abi.get('extension_sha256')==sha(extension) and abi.get('apk_sha256')==APK_PIN,'ABI review targets different bytes')
    require(abi.get('device_tested') is False and abi.get('original_apk_apply_tested') is False,'ABI review scope differs')
    require(abi.get('verifier_source_sha256')==sha((ROOT/'tools_java/AuditCandidateVisualAbi.java').read_bytes())
            and abi.get('wrapper_source_sha256')==sha((ROOT/'tests/audit_candidate_visual_abi.py').read_bytes()),'ABI verifier source changed')
    require(set(abi.get('audited_classes',[]))=={'L'+n.removesuffix('.smali')+';' for n in overlay if n.startswith('hiro/swiftkey/CandidateScroll')},'Candidate ABI coverage incomplete')
    required_refs={
        'Landroidx/recyclerview/widget/RecyclerView;->getItemAnimator()Lra/x1;',
        'Landroidx/recyclerview/widget/RecyclerView;->setItemAnimator(Lra/x1;)V',
        'Landroidx/recyclerview/widget/LinearLayoutManager;->l1(II)V',
        'Lfb0/u;->h(Z)V',
        'Landroidx/recyclerview/widget/RecyclerView;->w0(I)V'}
    resolved_native={r['defined_owner']+'->'+r['reference'].split('->',1)[1]
            for r in abi.get('native_and_extension_references',[])
            if r.get('resolved') is True and r.get('resolution_source')=='native_apk'}
    require(required_refs.issubset(resolved_native),'Candidate anchor/native fallback API coverage incomplete')
    review_path=ROOT/'tests/native_candidate_anchor_review.json'
    review=json.loads(review_path.read_text())
    require(review.get('status')=='STATIC_NATIVE_REVIEW_PASSED' and not review.get('blocking_findings'),'Native anchor review failed')
    require(review.get('device_tested') is False and review.get('original_apk_apply_tested') is False,'Native anchor review scope differs')
    require(review.get('apk_sha256')==APK_PIN
            and review.get('production_source_sha256')==sha(production[0].read_bytes())
            and review.get('patch_source_sha256')==sha((ROOT/'patch_java/app/hiro/swiftkey/patches/CandidateScrollPatch.java').read_bytes()),'Native anchor review targets different source or APK')
    arrival_review_path=ROOT/'tests/native_candidate_arrival_review.json'
    arrival_review=json.loads(arrival_review_path.read_text())
    require(arrival_review.get('status')=='PASSED' and arrival_review.get('source_compared_to_decoded_native') is True
            and arrival_review.get('native_methods_or_fragments')==9,'Native result-arrival review incomplete')
    require(arrival_review.get('fixture_sha256')==sha((ROOT/'tests/native_candidate_arrival_fixture.json').read_bytes())
            and arrival_review.get('device_tested') is False,'Native result-arrival evidence differs')
    for key in ('all_result_types_reach_expanded_provider','posted_result_uses_captured_type_without_current_task_check',
                'non_expanded_type_uses_native_reset_path','reattach_cached_result_precedes_active_adapter_and_scroll_to_zero'):
        require(arrival_review.get(key) is True,'Native result-arrival contract missing: '+key)
    negative_path=ROOT/'tests/candidate1819_negative_control.json'
    negative=json.loads(negative_path.read_text())
    require(negative.get('status')=='PASSED' and negative.get('old_production_compiles_with_same_fakes') is True
            and negative.get('device_tested') is False,'Candidate negative control incomplete')
    require(negative.get('old_production_source_sha256')=='cf7fe470701fdd932f8aec28ea1b8a668096a575ec23ee7821e608eae90614ec'
            and negative.get('new_production_source_sha256')==sha(production[0].read_bytes())
            and negative.get('regression_source_sha256')==sha((ROOT/'tests/CandidateArrivalHostTest.java').read_bytes()),'Candidate negative control source drift')
    require(set(negative.get('negative_cases',{}))=={'nonexpanded','smaller','survivors','samesize','collision'}
            and all(case.get('behavioral_failure_detected') is True and case.get('exit_code',0)!=0
                    for case in negative['negative_cases'].values()),'Missing behavioral failures against published 1.8.18')

    japanese_abi_path=ROOT/'tests/native_japanese_start_abi1819.json'
    japanese_abi=json.loads(japanese_abi_path.read_text())
    require(japanese_abi.get('status')=='PASSED' and not japanese_abi.get('unresolved')
            and not japanese_abi.get('inaccessible') and not japanese_abi.get('incompatible_opcodes'),'Japanese startup ABI failed')
    require(japanese_abi.get('native_hook_contract_verified') is True and len(japanese_abi.get('native_hook_entrypoints',[]))==3,'Japanese startup hook ABI incomplete')
    require(japanese_abi.get('extension_sha256')==sha(extension) and japanese_abi.get('apk_sha256')==APK_PIN,'Japanese startup ABI targets different bytes')
    require(japanese_abi.get('device_tested') is False and japanese_abi.get('original_apk_apply_tested') is False,'Japanese startup ABI scope differs')
    require(japanese_abi.get('verifier_source_sha256')==sha((ROOT/'tools_java/AuditJapaneseStartAbi.java').read_bytes())
            and japanese_abi.get('wrapper_source_sha256')==sha((ROOT/'tests/audit_japanese_start_abi.py').read_bytes()),'Japanese startup ABI verifier changed')
    require(set(japanese_abi.get('audited_classes',[]))=={'L'+n.removesuffix('.smali')+';' for n in overlay if n.startswith('hiro/swiftkey/JapaneseStart')},'Japanese startup ABI class coverage incomplete')
    japanese_review_path=ROOT/'tests/native_japanese_start_review.json'
    japanese_review=json.loads(japanese_review_path.read_text())
    require(japanese_review.get('status')=='STATIC_NATIVE_REVIEW_PASSED' and not japanese_review.get('blocking_findings'),'Japanese startup native review failed')
    require(japanese_review.get('device_tested') is False and japanese_review.get('original_apk_apply_tested') is False,'Japanese startup review scope differs')
    require(japanese_review.get('apk_sha256')==APK_PIN
            and japanese_review.get('production_source_sha256')==sha(production[1].read_bytes())
            and japanese_review.get('patch_source_sha256')==sha((ROOT/'patch_java/app/hiro/swiftkey/patches/JapaneseStartPatch.java').read_bytes()),'Japanese startup review targets different source or APK')

    japanese_work=work/'japanese_start_tests'
    japanese_text=run(['python',ROOT/'tests/run_japanese_start_tests.py','--source-dir',ROOT,'--work',japanese_work],evidence/'japanese-start-tests.txt')
    japanese_report=json.loads((japanese_work/'report.json').read_text())
    require('PASS' in japanese_text and japanese_report.get('status')=='PASSED','Japanese startup host tests failed')
    require(japanese_report.get('production_source_sha256')==sha(production[1].read_bytes()),'Japanese startup tests target different source')
    require(japanese_report.get('device_tested') is False and japanese_report.get('original_apk_apply_tested') is False,'Japanese startup host scope differs')
    require(japanese_report.get('scenarios',0)>0 and japanese_report.get('assertions',0)>0,'Missing Japanese startup assertions')
    japanese_behaviors=('japanese_start_on_normal_text_input','japanese_start_on_reopen',
        'japanese_manual_switch_preserved_within_session','japanese_configured_layout_preserved',
        'japanese_special_input_requirements_preserved','japanese_start_uses_native_language_selection','japanese_start_no_text_injection')
    for behavior in japanese_behaviors:require(japanese_report.get(behavior) is True,'Missing Japanese startup behavior: '+behavior)

    candidate_work=work/'candidate_tests'
    test_output=run(['python',ROOT/'tests/run_candidate_tests.py','--source-dir',ROOT,'--work',candidate_work],evidence/'candidate-tests.txt')
    candidate_report=json.loads((candidate_work/'report.json').read_text())
    require('PASS' in test_output and candidate_report.get('status')=='PASSED','Candidate host tests failed')
    require(candidate_report.get('production_source_sha256')==sha(production[0].read_bytes()),'Candidate tests target different source')
    require(candidate_report.get('device_tested') is False and candidate_report.get('android_instrumentation_tested') is False,'Candidate host scope differs')
    require(candidate_report.get('scenarios',0)>0 and candidate_report.get('assertions',0)>0,'Missing behavior assertions')
    behaviors=('candidate_same_query_scroll_position_preserved','candidate_reset_does_not_stop_scroll',
               'candidate_listener_orders_verified','candidate_fresh_commit_metadata_preserved',
               'candidate_layout_refresh_does_not_refetch','candidate_same_query_all_result_types_preserved',
               'candidate_smaller_request_result_does_not_reset','candidate_survivor_order_preserved',
               'candidate_same_size_reorder_refreshes_metadata','candidate_reopen_accepts_initial_results')
    for behavior in behaviors:require(candidate_report.get(behavior) is True,'Missing candidate behavior coverage: '+behavior)
    loader_work=work/'candidate_loader'
    loader_text=run(['python',ROOT/'tests/build_candidate_loader.py','--source-dir',ROOT,'--tools',tools,'--work',loader_work,'--standalone',inputs/OLD_SINGLE,'--bundle',inputs/OLD_BUNDLE],evidence/'candidate-loader.txt')
    loader_report=json.loads((loader_work/'report.json').read_text())
    require('PASS' in loader_text and loader_report.get('status')=='PASSED','Candidate loader verification failed')
    require(loader_report.get('jvm_main_unchanged') is True and loader_report.get('punctuation_patch_unchanged') is True
            and loader_report.get('native_hook_sdks_agree') is True,'Loader preservation or SDK parity failed')
    require(loader_report['native_hook_tests']['native_methods']==6 and loader_report['native_hook_tests']['native_hook_sites']==9,'Native hook coverage incomplete')
    japanese_loader_work=work/'japanese_start_loader'
    japanese_loader_text=run(['python',ROOT/'tests/build_japanese_start_loader.py','--source-dir',ROOT,'--tools',tools,
        '--work',japanese_loader_work,'--standalone',inputs/OLD_SINGLE,'--bundle',inputs/OLD_BUNDLE,
        '--candidate-loader-dir',loader_work],evidence/'japanese-start-loader.txt')
    japanese_loader_report=json.loads((japanese_loader_work/'report.json').read_text())
    require('PASS' in japanese_loader_text and japanese_loader_report.get('status')=='PASSED','Japanese startup loader failed')
    for key in ('jvm_main_only_two_calls_added','jvm_original_normalized_class_content_preserved',
                'native_hook_sdks_agree','published_prior_glide_transform_executed','shared_lifecycle_glide_hook_preserved'):
        require(japanese_loader_report.get(key) is True,'Japanese startup loader preservation failed: '+key)
    require(japanese_loader_report['native_hook_tests']['native_methods']==4
            and japanese_loader_report['native_hook_tests']['native_hook_sites']==4,'Japanese startup hook coverage incomplete')
    require(all(japanese_loader_report['android'][label].get('only_two_main_bridge_calls_added') is True
                for label in ('single','bundle')),'Unrelated main loader changes')
    helper_name=JVM_PREFIX+'CandidateScrollPatch.class'
    jvm_updates={helper_name:(loader_work/'jvm'/helper_name).read_bytes()}
    for name in (JVM_PREFIX+'JapaneseSamsungEmojiPatch.class',JVM_PREFIX+'JapaneseStartPatch.class'):
        jvm_updates[name]=(japanese_loader_work/'jvm'/name).read_bytes()
    single=make_mpp(inputs/OLD_SINGLE,output/SINGLE,extension,'1.8.18','1.8.19',{**jvm_updates,'classes.dex':(japanese_loader_work/'single.dex').read_bytes()})
    bundle=make_mpp(inputs/OLD_BUNDLE,output/BUNDLE,extension,'1.0.301','1.0.302',{**jvm_updates,'classes.dex':(japanese_loader_work/'bundle.dex').read_bytes()})
    new_single,new_bundle=archive(output/SINGLE),archive(output/BUNDLE)
    require(new_single[EXT]==new_bundle[EXT],'New runtime differs between packages')
    for old,new in ((old_single,new_single),(old_bundle,new_bundle)):
        require(all(old[n]==new[n] for n in old if n not in {MF,EXT,'classes.dex',*jvm_updates}),'Unrelated archive content changed')
        require({n for n in new if n.startswith(JVM_PREFIX) and n.endswith('.class')}==set(JVM_CLASSES)|ADDED_JVM,'Final JVM class inventory differs')
    require(all(new_single[n]==new_bundle[n]==old_single[n]==old_bundle[n] for n in JVM_CLASSES if n not in jvm_updates),'Unrelated SwiftKey JVM loader changed')
    require(all(new_single[n]==new_bundle[n] for n in jvm_updates),'New JVM hooks differ between packages')
    dex_count=struct.unpack_from('<I',new_bundle['classes.dex'],96)[0]
    require(dex_count==279 and struct.unpack_from('<I',new_single['classes.dex'],96)[0]==20,'Loader inventory differs')
    javac(['--release','8','-g:none','-encoding','UTF-8','-d',work/'tool_classes',ROOT/'tests/LoaderParityProbe.java'],work/'javac-probe.txt')
    runtime_java=os.environ.get('MORPHE_JAVA','java');parity=[]
    for tool,suffix in [('morphe.jar',''),('morphe-current.jar','-current')]:
        path=evidence/('loader-parity'+suffix+'.json')
        run([runtime_java,'-cp',str(work/'tool_classes')+os.pathsep+str(tools/tool),'LoaderParityProbe',inputs/OLD_BUNDLE,output/SINGLE,output/BUNDLE,path],evidence/('loader-probe'+suffix+'.txt'))
        report=json.loads(path.read_text())
        require(report.get('status')=='PASSED' and report.get('selected_extension_stream_verified') is True
                and report.get('selected_extension_sha256')==sha(extension) and report.get('baseline_extension_sha256')==EXTENSION_PIN,'Real loader extension stream differs')
        parity.append(report)
    require(parity[0]==parity[1],'Morphe SDKs disagree')
    qa={'status':'CANDIDATE_SCROLL_JAPANESE_START_HOST_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.19','bundle_version':'1.0.302',
        'target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,
        'device_tested':False,'original_apk_apply_tested':False,'device_fix_confirmed':False,
        'period_up_flick_question_only':True,'long_vowel_up_flick_exclamation_only':True,
        'punctuation_labels_match':True,'ordinary_punctuation_taps_preserved':True,'keyswap_combinations_verified':True,
        'candidate_open_close_on_left':True,'old_toolbar_mark_removed':True,'emoji_long_press_opens_toolbar':True,'emoji_short_press_closes_open_toolbar':True,
        'existing_non_candidate_runtime_classes_unchanged':True,'runtime_unrelated_classes_unchanged':True,
        'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,
        'standalone_and_bundle_runtime_equal':True,'loader_entries_byte_identical':False,'unrelated_loader_classes_unchanged':True,
        'jvm_swiftkey_loader_matches_standalone':True,'combined_jvm_swiftkey_discovered':True,'previous_jvm_patches_preserved':True,
        'all_static_checks_passed':True,'host_tests_passed':True,'current_manager_patcher_selection_verified':True,'two_loader_sdks_agree':True,
        'selected_extension_stream_verified':True,'loader_selected_extension_matches_packaged_runtime':True,
        'diagnostic_function_removed':True,'candidate_append_partial_updates':True,'candidate_item_animations_disabled':True,
        'upward_flick_recognizer_byte_identical':True,'candidate_toolbar_1818_byte_identical':True,
        'emoji_toolbar_1818_byte_identical':True,'punctuation_1818_byte_identical':True,
        **{key:candidate_report[key] for key in behaviors},
        **{key:japanese_report[key] for key in japanese_behaviors},
        'native_abi_review_verified':True,'native_abi_replayed_in_ci':False,
        'native_candidate_anchor_review_verified':True,'native_candidate_anchor_review_replayed_in_ci':False,
        'native_candidate_arrival_review_verified':True,'native_candidate_arrival_review_replayed_in_ci':False,
        'candidate_negative_control_verified':True,'candidate_negative_control_replayed_in_ci':False,
        'japanese_start_native_abi_verified':True,'native_japanese_start_abi_replayed_in_ci':False,
        'native_japanese_start_review_verified':True,'native_japanese_start_review_replayed_in_ci':False,
        'native_abi_review':{'source':'tests/native_abi_review1819.json','sha256':sha(abi_path.read_bytes()),'apk_sha256':APK_PIN,'extension_sha256':sha(extension)},
        'native_candidate_anchor_review':{'source':'tests/native_candidate_anchor_review.json','sha256':sha(review_path.read_bytes()),
            'runtime_source_sha256':review['production_source_sha256'],'apk_sha256':APK_PIN,'status':review['status']},
        'native_candidate_arrival_review':{'source':'tests/native_candidate_arrival_review.json','sha256':sha(arrival_review_path.read_bytes()),
            'status':arrival_review['status'],'fixture_sha256':arrival_review['fixture_sha256'],'native_methods_or_fragments':9},
        'candidate_negative_control':{'source':'tests/candidate1819_negative_control.json','sha256':sha(negative_path.read_bytes()),
            'status':negative['status'],'behavioral_failures_detected':5,'old_production_compiles_with_same_fakes':True},
        'native_japanese_start_abi_review':{'source':'tests/native_japanese_start_abi1819.json','sha256':sha(japanese_abi_path.read_bytes()),'apk_sha256':APK_PIN,'extension_sha256':sha(extension)},
        'native_japanese_start_review':{'source':'tests/native_japanese_start_review.json','sha256':sha(japanese_review_path.read_bytes()),
            'runtime_source_sha256':japanese_review['production_source_sha256'],'apk_sha256':APK_PIN,'status':japanese_review['status']},
        'candidate_test_report':candidate_report,'candidate_loader_report':loader_report,
        'japanese_start_test_report':japanese_report,'japanese_start_loader_report':japanese_loader_report,
        'runtime':{'before_classes':len(before),'after_classes':len(after),'unchanged_existing_classes':len(before)-len(changed),
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
    print('PASS SwiftKey1.8.19 /bundle302 candidate correction and Japanese startup',flush=True)
    return qa

def main():
    p=argparse.ArgumentParser()
    for name in ('input-dir','tools','work','output'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--download',action='store_true');p.add_argument('--compile-only',action='store_true')
    a=p.parse_args();inputs=a.input_dir.resolve();tools=a.tools.resolve();work=a.work.resolve();output=a.output.resolve()
    if a.download:download(inputs,tools)
    build(inputs,tools,work,output,a.compile_only)
if __name__=='__main__':main()
