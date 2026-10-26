#!/usr/bin/env python3
"""Restore SwiftKey JVM loader packaging; runtime and Android loaders remain byte-identical."""
from __future__ import annotations
import argparse, copy, hashlib, json, os, shutil, struct, subprocess, time, urllib.request, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OLD_SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.4.mpp'
OLD_BUNDLE='Hiro_Morphe_Patches_v1.0.287.mpp'
SINGLE='SwiftKeyBeta_v2_SamsungEmoji_v1.8.5.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.288.mpp'
QA_NAME='QA_SwiftKey_v1.8.5.json'
SOURCE_ZIP='SwiftKeyBeta_v1.8.5_sources_and_QA.zip'
MF='META-INF/MANIFEST.MF'
EXT='extensions/swiftkey_japanese.mpe'
STAMP='2026-10-26T05:00:00'
JVM_PREFIX='app/hiro/swiftkey/patches/'
JVM_CLASSES=tuple(JVM_PREFIX+n+'.class' for n in (
    'JapaneseSamsungEmojiPatch','MethodContract','InputLearningPatch','DictionarySupplementPatch',
    'KeySwapPatch','PasteProtectionPatch','JapaneseGlidePatch','DeleteProtectionPatch'))
DESCRIPTION='SwiftKeyの過去の改造が適用されない総合版の配布構成を修復。欠落していたJVMローダー8クラスを単体版から復元。辞書・絵文字・キー配置・グライド修正などの実装は保持。実機未確認。'
BASE='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/d851881fc048bcd28f0eb5a18eef08cca10c5aa1/downloads/'
PINS={OLD_SINGLE:'45af2d9b7a1c7878b0bceb79ad814d92bdeed48ff757f7568a8a30a39ad37def',
      OLD_BUNDLE:'566fd0ce0f736648341b566aa96eaa05bb59fd9cca5a12a47a819f29d3e14ff3',
      'morphe.jar':'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
      'morphe-current.jar':'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573'}
URLS={OLD_SINGLE:BASE+OLD_SINGLE,OLD_BUNDLE:BASE+OLD_BUNDLE,
      'morphe.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
      'morphe-current.jar':'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar'}

def sha(raw):return hashlib.sha256(raw).hexdigest()
def require(ok,message):
    if not ok:raise RuntimeError(message)
def run(command,log):
    result=subprocess.run([str(a) for a in command],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    log.parent.mkdir(parents=True,exist_ok=True);log.write_text(result.stdout)
    print(result.stdout,end='',flush=True)
    require(result.returncode==0,'Command failed: '+str(log));return result.stdout
def manifest(raw):
    lines=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '):require(bool(lines),'Invalid manifest continuation');lines[-1]+=line[1:]
        elif line:lines.append(line)
    pairs=[line.decode().split(': ',1) for line in lines]
    require(all(len(x)==2 for x in pairs) and len(dict(pairs))==len(pairs),'Invalid manifest')
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
    raw=b'\r\n'.join(lines)+b'\r\n\r\n';require(manifest(raw)==values,'Manifest roundtrip failed');return raw
def download(inputs,tools):
    for name,pin in PINS.items():
        path=(inputs if name.endswith('.mpp') else tools)/name
        if path.exists():require(sha(path.read_bytes())==pin,'Existing pinned input differs: '+name);continue
        path.parent.mkdir(parents=True,exist_ok=True)
        for attempt in range(3):
            try:
                raw=urllib.request.urlopen(URLS[name],timeout=120).read()
                require(sha(raw)==pin,'Downloaded input hash differs: '+name)
                path.write_bytes(raw);break
            except Exception:
                if attempt==2:raise
                time.sleep(2*(attempt+1))
def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist())==len(set(z.namelist())) and z.testzip() is None,'Invalid ZIP '+str(path))
        return {n:z.read(n) for n in z.namelist()}
def make_mpp(source,target,baseline_version,version,additions):
    with zipfile.ZipFile(source) as old:
        values=manifest(old.read(MF));require(values['Version']==baseline_version,'Wrong baseline version')
        values.update(Version=version,Timestamp=STAMP,Description=DESCRIPTION)
        require(not(set(old.namelist())&set(additions)),'Restoration would overwrite an existing entry')
        with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as new:
            for info in old.infolist():new.writestr(copy.copy(info),manifest_bytes(values) if info.filename==MF else old.read(info.filename))
            for name,raw in additions.items():
                info=zipfile.ZipInfo(name,(2026,10,26,5,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
                new.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    before,after=archive(source),archive(target)
    changed=sorted(n for n in before if before[n]!=after[n]);added=sorted(set(after)-set(before))
    require(changed==[MF] and added==sorted(additions),'Unexpected MPP delta')
    require(set(before)<=set(after),'MPP entries removed')
    require(all(after[n]==raw for n,raw in additions.items()),'Restored JVM loader bytes differ')
    return {'changed_entries':changed,'added_entries':added,'unchanged_entries':len(before)-1,
            'bytes':target.stat().st_size,'sha256':sha(target.read_bytes()),'all_previous_payload_entries_byte_identical':True}
def source_paths():
    paths=[ROOT/n for n in ('build185.py','publish185.py','publisher_tests.py','workflow.yml','RELEASE_NOTES.txt','README.txt') if (ROOT/n).is_file()]
    for name in ('tests','tools_java'):
        paths.extend(p for p in (ROOT/name).rglob('*') if p.is_file() and p.suffix in ('.java','.py','.json','.txt','.md','.yml'))
    return sorted(set(paths))
def zip_add(z,name,raw):
    info=zipfile.ZipInfo(name,(2026,10,26,5,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
    z.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
def build(inputs,tools,work,output):
    for name,pin in PINS.items():require(sha(((inputs if name.endswith('.mpp') else tools)/name).read_bytes())==pin,'Pinned input differs: '+name)
    if work.exists():shutil.rmtree(work)
    for d in (work,work/'classes',output,output/'evidence'):d.mkdir(parents=True,exist_ok=True)
    old_single,old_bundle=archive(inputs/OLD_SINGLE),archive(inputs/OLD_BUNDLE)
    single_jvm={n:raw for n,raw in old_single.items() if n.startswith(JVM_PREFIX) and n.endswith('.class')}
    require(set(single_jvm)==set(JVM_CLASSES),'Standalone JVM loader inventory differs')
    require(not any(n in old_bundle for n in JVM_CLASSES),'Baseline no longer has the diagnosed omission')
    require(old_single[EXT]==old_bundle[EXT],'Baseline SwiftKey runtimes differ')
    require({n:raw for n,raw in old_single.items() if n.startswith('swiftkey/')}=={n:raw for n,raw in old_bundle.items() if n.startswith('swiftkey/')},'Baseline SwiftKey dictionaries differ')
    additions={n:single_jvm[n] for n in JVM_CLASSES}
    require(all(raw.startswith(b'\xca\xfe\xba\xbe') for raw in additions.values()),'Invalid JVM classfile')
    single=make_mpp(inputs/OLD_SINGLE,output/SINGLE,'1.8.4','1.8.5',{})
    bundle=make_mpp(inputs/OLD_BUNDLE,output/BUNDLE,'1.0.287','1.0.288',additions)
    new_single,new_bundle=archive(output/SINGLE),archive(output/BUNDLE)
    require(new_single[EXT]==new_bundle[EXT]==old_bundle[EXT],'Runtime changed')
    require(new_bundle['classes.dex']==old_bundle['classes.dex'] and new_single['classes.dex']==old_single['classes.dex'],'Android patch loaders changed')
    dex_count=struct.unpack_from('<I',new_bundle['classes.dex'],96)[0]
    require(dex_count==275,'Expected all 275 existing Android loader classes')
    require(all(new_bundle[n]==new_single[n]==raw for n,raw in additions.items()),'Standalone/bundle JVM loaders differ')
    probe=ROOT/'tests/LoaderParityProbe.java';require(probe.is_file(),'Missing actual Morphe loader regression test')
    run(['java','com.sun.tools.javac.Main','--release','8','-g:none','-encoding','UTF-8','-d',work/'classes',probe],work/'javac-probe.txt')
    runtime=os.environ.get('MORPHE_JAVA','java');report_path=output/'evidence/loader-parity.json'
    run([runtime,'-cp',str(work/'classes')+os.pathsep+str(tools/'morphe.jar'),'LoaderParityProbe',inputs/OLD_BUNDLE,output/SINGLE,output/BUNDLE,report_path],output/'evidence/loader-probe.txt')
    parity=json.loads(report_path.read_text())
    require(parity.get('status')=='PASSED','Morphe loader parity failed')
    current_report_path=output/'evidence/loader-parity-current.json'
    run([runtime,'-cp',str(work/'classes')+os.pathsep+str(tools/'morphe-current.jar'),'LoaderParityProbe',inputs/OLD_BUNDLE,output/SINGLE,output/BUNDLE,current_report_path],output/'evidence/loader-probe-current.txt')
    current_parity=json.loads(current_report_path.read_text())
    require(current_parity.get('status')=='PASSED','Current Morphe loader parity failed')
    require(current_parity==parity,'Supported Morphe SDKs disagree on patch selection or dependency metadata')
    notes=ROOT/'RELEASE_NOTES.txt';require(notes.is_file(),'Missing reviewed release notes')
    qa={'status':'PACKAGING_RESTORED_HOST_VERIFIED_DEVICE_UNVERIFIED','swiftkey_version':'1.8.5','bundle_version':'1.0.288',
        'device_tested':False,'original_apk_apply_tested':False,'device_cause_confirmed':False,'device_fix_confirmed':False,
        'target_package':'com.touchtype.swiftkey.beta','target_version':'9.13.16.4','target_version_code':1236271168,
        'runtime_byte_identical':True,'all_dictionary_resources_preserved':True,'other_apps_byte_identical':True,
        'standalone_and_bundle_runtime_equal':True,'android_loader_byte_identical':True,
        'jvm_swiftkey_loader_restored':True,'jvm_swiftkey_loader_matches_standalone':True,'combined_jvm_swiftkey_discovered':True,
        'previous_jvm_patches_preserved':True,'host_tests_passed':True,'all_static_checks_passed':True,
        'loader_sdk_versions_tested':['morphe-desktop1.17.0-dev.6','morphe-desktop1.19.1/patcher2.0.1'],
        'current_manager_patcher_selection_verified':True,'two_loader_sdks_agree':True,
        'android_loader_class_count':dex_count,'runtime_sha256':sha(new_bundle[EXT]),
        'root_cause':'Combined bundle omitted all eight SwiftKey JVM loader classfiles; actual Morphe JVM loader selected zero SwiftKey patches despite Android DEX metadata being present.',
        'restored_classes':{n:{'bytes':len(raw),'sha256':sha(raw)} for n,raw in additions.items()},
        'loader_parity':parity,'single':single,'bundle':bundle,
        'inputs':{n:{'url':URLS[n],'sha256':pin} for n,pin in PINS.items()}}
    (output/QA_NAME).write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    (output/'RELEASE_NOTES.txt').write_bytes(notes.read_bytes())
    with zipfile.ZipFile(output/SOURCE_ZIP,'w') as z:
        for path in source_paths():zip_add(z,'src/'+path.relative_to(ROOT).as_posix(),path.read_bytes())
        zip_add(z,QA_NAME,(output/QA_NAME).read_bytes())
        zip_add(z,'evidence/loader-parity.json',report_path.read_bytes())
        zip_add(z,'evidence/loader-parity-current.json',current_report_path.read_bytes())
    names=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt')
    (output/'SHA256SUMS.txt').write_text(''.join(sha((output/n).read_bytes())+'  '+n+'\n' for n in names))
    print('PASS packaging restoration SwiftKey 1.8.5 / bundle 1.0.288',flush=True)
    return qa
def main():
    p=argparse.ArgumentParser();p.add_argument('--input-dir',type=Path,required=True);p.add_argument('--tools',type=Path,required=True)
    p.add_argument('--work',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--download',action='store_true')
    a=p.parse_args();inputs=a.input_dir.resolve();tools=a.tools.resolve();work=a.work.resolve();output=a.output.resolve()
    if a.download:download(inputs,tools)
    build(inputs,tools,work,output)
if __name__=='__main__':main()
