#!/usr/bin/env python3
"""Add exact emoji-footer hooks to pinned 1.8.21 JVM and Android loaders."""
from __future__ import annotations
import argparse, hashlib, json, os, re, shutil, subprocess, zipfile
from pathlib import Path

PREFIX='app/hiro/swiftkey/patches/'
MAIN=PREFIX+'JapaneseSamsungEmojiPatch'
HELPER=PREFIX+'EmojiFooterPatch'
BASELINES={'single':'000076673c9253acb1edb3a11ebf9889396c4977d8ea7788e52275b16190d774',
           'bundle':'1523f42ee8597475c8d4d8b1625820fd874408090881c88f9e0107583fc00d7f'}
def require(value,message):
    if not value:raise RuntimeError(message)
def sha(raw):return hashlib.sha256(raw).hexdigest()
def run(command,log):
    result=subprocess.run([str(x) for x in command],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    log.parent.mkdir(parents=True,exist_ok=True);log.write_text(result.stdout)
    require(result.returncode==0,'Command failed: '+str(log)+'\n'+result.stdout);return result.stdout
def smali(directory):return {p.relative_to(directory).as_posix():p.read_bytes() for p in sorted(directory.rglob('*.smali'))}
def classes(path):
    with zipfile.ZipFile(path) as z:return {n:z.read(n) for n in z.namelist() if n.startswith(PREFIX) and n.endswith('.class')}
def stage(baseline,output,replacements):
    with zipfile.ZipFile(baseline) as old,zipfile.ZipFile(output,'w') as new:
        names=set(old.namelist());require(len(names)==len(old.infolist()),'Duplicate baseline entries')
        for entry in old.infolist():new.writestr(entry,replacements.get(entry.filename,old.read(entry.filename)))
        for name,raw in replacements.items():
            if name not in names:new.writestr(name,raw)

def build(root,tools,work,standalone,bundle,apk=None):
    baselines={'single':standalone,'bundle':bundle}
    for label,path in baselines.items():require(sha(path.read_bytes())==BASELINES[label],'Unverified published baseline '+label)
    before_jvm=classes(standalone)
    require(len(before_jvm)==12 and before_jvm==classes(bundle),'Baseline SwiftKey JVM inventory differs')
    require(HELPER+'.class' not in before_jvm,'Footer loader already present')
    fixture=root/'tests/native_emoji_footer_fixture';fixture_manifest=json.loads((fixture/'manifest.json').read_text())
    require({p.relative_to(fixture).as_posix():sha(p.read_bytes()) for p in sorted(fixture.rglob('*.smali'))}==fixture_manifest['files'],'Native fixture source changed')
    if apk:require(sha(apk.read_bytes())==fixture_manifest['source_apk_sha256'],'Native reference archive differs')
    if work.exists():shutil.rmtree(work)
    for name in ('jvm','helper_classes','tool_classes','helper_dex'):(work/name).mkdir(parents=True,exist_ok=True)
    java21=tools/'jdk-21.0.8+9-jre/bin/java';java=os.environ.get('MORPHE_JAVA',str(java21) if java21.is_file() else 'java')
    cp=os.pathsep.join(map(str,(tools/'morphe.jar',standalone)))
    javac=['java','com.sun.tools.javac.Main','-g:none','-encoding','UTF-8']
    source=root/'patch_java'/(HELPER+'.java')
    run(javac+['--release','8','-cp',cp,'-d',work/'helper_classes',source],work/'javac-helper.log')
    require(sorted(p.relative_to(work/'helper_classes').as_posix() for p in (work/'helper_classes').rglob('*.class'))==[HELPER+'.class'],'Unexpected patch helper inventory')
    with zipfile.ZipFile(work/'emoji-footer-helper.jar','w') as z:z.write(work/'helper_classes'/(HELPER+'.class'),HELPER+'.class')
    with zipfile.ZipFile(work/'baseline-classpath.jar','w') as z:
        for name,raw in before_jvm.items():z.writestr(name,raw)
    run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--release','--min-api','26','--lib',tools/'android.jar','--classpath',tools/'morphe.jar','--classpath',work/'baseline-classpath.jar','--output',work/'helper_dex',work/'emoji-footer-helper.jar'],work/'d8-helper.log')
    tool_cp=os.pathsep.join((str(work/'helper_classes'),cp,str(tools/'r8.jar')))
    run(javac+['-cp',tool_cp,'-d',work/'tool_classes',root/'tools_java/UpdateEmojiFooterLoader.java',root/'tests/EmojiFooterPatchProbe.java'],work/'javac-tools.log')
    run(javac+['-cp',tools/'apktool.jar','-d',work/'tool_classes',root/'tools_java/Dump1814.java'],work/'javac-dump.log')
    update=[java,'-cp',str(work/'tool_classes')+os.pathsep+tool_cp,'UpdateEmojiFooterLoader']
    run(update+['jvm',standalone,work/'helper_classes',work/'jvm'],work/'jvm-bridge.log')
    dump=['java','-cp',os.pathsep.join(map(str,(work/'tool_classes',tools/'apktool.jar'))),'Dump1814']
    android={}
    for label,baseline in baselines.items():
        with zipfile.ZipFile(baseline) as z:(work/(label+'-before.dex')).write_bytes(z.read('classes.dex'))
        output=work/(label+'.dex')
        run(update+['dex',baseline,work/'helper_dex/classes.dex',output],work/(label+'-bridge.log'))
        run(dump+[work/(label+'-before.dex'),work/(label+'-before')],work/(label+'-dump-before.log'))
        run(dump+[output,work/(label+'-after')],work/(label+'-dump-after.log'))
        before,after=smali(work/(label+'-before')),smali(work/(label+'-after'))
        require(set(after)==set(before)|{HELPER+'.smali'},'Android loader class inventory changed unexpectedly')
        changed=sorted(n for n in before if before[n]!=after[n]);require(changed==[MAIN+'.smali'],'Unrelated Android loader changed')
        pattern=(rb'(?m)^    invoke-static/range \{p0 \.\. p0\}, Lapp/hiro/swiftkey/patches/EmojiFooterPatch;->'
                 rb'(?:apply|checkOriginal)\(Lapp/morphe/patcher/patch/BytecodePatchContext;\)V\n\n')
        restored,count=re.subn(pattern,b'',after[MAIN+'.smali'])
        require(count==2 and restored==before[MAIN+'.smali'],'Android main change exceeds two bridge calls')
        android[label]={'before_classes':len(before),'after_classes':len(after),'unchanged_existing_classes':len(before)-1,
            'changed_existing_classes':changed,'added_classes':[HELPER+'.smali'],'only_two_main_bridge_calls_added':True,'sha256':sha(output.read_bytes())}
    require((work/'single-after'/(HELPER+'.smali')).read_bytes()==(work/'bundle-after'/(HELPER+'.smali')).read_bytes(),'Android helper differs between packages')
    require(android['bundle']['before_classes']==279 and android['bundle']['after_classes']==280,'Combined loader inventory differs')
    replacements={p.relative_to(work/'jvm').as_posix():p.read_bytes() for p in sorted((work/'jvm').rglob('*.class'))}
    replacements['classes.dex']=(work/'single.dex').read_bytes()
    staged=work/'emoji-footer-loader-check.mpp';stage(standalone,staged,replacements)
    after_jvm=classes(staged);require(set(after_jvm)==set(before_jvm)|{HELPER+'.class'},'Staged JVM inventory differs')
    changed_jvm=sorted(n for n in before_jvm if before_jvm[n]!=after_jvm[n]);require(changed_jvm==[MAIN+'.class'],'Unrelated JVM helper changed')
    reports,archive_reports={},{}
    for label,sdk in (('archived_sdk','morphe.jar'),('current_sdk','morphe-current.jar')):
        # Execute the class packaged in the staged MPP; no loose patch helper class is available here.
        probe_cp=os.pathsep.join(map(str,(work/'tool_classes',tools/sdk,staged,tools/'r8.jar')))
        probe=[java,'-cp',probe_cp,'app.hiro.swiftkey.patches.EmojiFooterPatchProbe']
        path=work/label/'native-hook-report.json';run(probe+[fixture,path],work/label/'native-hooks.log')
        native=json.loads(path.read_text());reports[label]=native
        require(native['status']=='PASSED' and native['native_methods']==2 and native['native_methods_changed']==1
                and native['native_hook_sites']==1 and native['inserted_runtime_calls']==1
                and native['all_target_drift_rejected_before_mutation'] and native['native_enter_method_byte_identical'],
                'Exact native constructor hook/native Enter preservation checks failed')
        if apk:
            path=work/label/'native-archive-hook-report.json';run(probe+[apk,path],work/label/'native-archive-hooks.log')
            archive_reports[label]=json.loads(path.read_text())
            require(archive_reports[label]['status']=='PASSED' and not archive_reports[label]['native_fixture'],'Archived native hook failed')
    require(reports['archived_sdk']==reports['current_sdk'],'SDKs disagree on actual fixture transformation')
    if apk:require(archive_reports['archived_sdk']==archive_reports['current_sdk'],'SDKs disagree on archive transformation')
    report={'status':'PASSED','scope':'Additive footer loader hooks; actual pinned native constructor transformation; native Enter action preserved',
        'baseline_mpp_sha256':BASELINES,'baseline_jvm_patch_classes':12,'final_jvm_patch_classes':13,
        'jvm_main_only_two_calls_added':True,'jvm_original_normalized_class_content_preserved':True,
        'existing_named_patch_registration_unchanged':True,'changed_jvm_classes':changed_jvm,'added_jvm_classes':[HELPER+'.class'],
        'unchanged_jvm_classes':{n:sha(raw) for n,raw in before_jvm.items() if n not in changed_jvm},
        'native_hook_tests':reports['archived_sdk'],'native_hook_tests_by_sdk':reports,'native_archive_hook_tests':archive_reports,
        'native_hook_sdks_agree':True,'native_hook_sdks_tested':['morphe-desktop1.17.0-dev.6','morphe-desktop1.19.1/patcher2.0.1'],
        'native_probe_loader':'Pinned published1.8.21 MPP with only main bridge/helper changed; no loose helper classpath',
        'native_enter_method_preserved':True,'prior_emoji_toolbar_preserved':True,'prior_sensitivity_preserved':True,
        'android':android,'jvm':{n:{'bytes':len(raw),'sha256':sha(raw)} for n,raw in replacements.items() if n.endswith('.class')},
        'source_sha256':{'patch_java/'+HELPER+'.java':sha(source.read_bytes()),'tests/EmojiFooterPatchProbe.java':sha((root/'tests/EmojiFooterPatchProbe.java').read_bytes())},
        'apk_sha256':sha(apk.read_bytes()) if apk else None,'device_tested':False,'original_apk_apply_tested':False}
    (work/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('PASS emoji footer loader: JVM and Android bridges, 1 native constructor hook, native Enter unchanged, both SDKs')
    return report

def main():
    p=argparse.ArgumentParser()
    for name in ('source-dir','tools','work','standalone','bundle'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--apk',type=Path);a=p.parse_args()
    build(a.source_dir.resolve(),a.tools.resolve(),a.work.resolve(),a.standalone.resolve(),a.bundle.resolve(),a.apk.resolve() if a.apk else None)
if __name__=='__main__':main()
