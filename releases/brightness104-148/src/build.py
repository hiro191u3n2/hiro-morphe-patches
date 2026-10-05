#!/usr/bin/env python3
"""Build a manifest-only BrightnessClick update; preserve every app-runtime payload."""
import argparse, hashlib, importlib.util, json, os, shutil, subprocess, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
BASE='Hiro_Morphe_Patches_v1.0.147.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.148.mpp'
SINGLE='BrightnessClick_S26_v1.0.4.mpp'
BASE_SHA='a2157ac3d1dd34a9681b0123090d2b863e067afcccf28054d09d9dfad91ed70b'
PINS={'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
      'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
      'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
NOTES='''【明るさタッチ v1.0.4 / Hiro Morphe Patches v1.0.148】

起動直後から「最近使ったアプリ」に表示しないように変更しました。
PermissionActivity（起動・権限案内）、BlankActivity（明るさ変更）、MainActivity（設定）を含むアプリ内Activityへ android:excludeFromRecents="true" を設定します。
終了後に削除する方式ではなく、起動時から履歴の対象外にする方式です。Android本体の「設定」アプリの履歴や通知を隠す変更ではありません。

明るさ切替・0%最小明るさ・設定保存・権限案内の実行コードと素材は前版と同一です。noHistory・強制終了・常駐処理は追加しません。
APKのversionNameはManager互換の1.5を維持し、versionCodeを9から10へ更新。改造版番号はファイル名・アプリ内表示で1.0.4とします。

Morphe Managerのソースを更新し、未改造の明るさタッチ1.5（6）へ「Galaxy S26 Ultra対応と明るさ変更権限の修正」を再適用してインストールしてください。統合版と単独版はどちらか一方を使います。インストール済み改造APKへの再パッチは既存の保護処理により中止されます。パッチソース更新だけではインストール済みアプリは変わりません。
旧版の履歴カードが残っている場合は、一度そのカードを閉じてから更新版を起動してください。

219件のホスト側Manifest試験、DEX構造・変更範囲の検証、独立2回の変換一致を確認しています。Galaxy実機での起動・履歴表示の確認、および元APKを用いた再適用試験は今回未実施です。APKの直接配布ではなくMPPの更新です。
ULike v1.9.22を含む他アプリのパッチは変更しません。
'''
def require(value,msg):
    if not value: raise RuntimeError(msg)
def sha(data):return hashlib.sha256(data).hexdigest()
def load_helpers(repo):
    path=repo/'releases/ulike-rollback188-142/rollback142.py';raw=path.read_bytes()
    require(hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()=='e7771a4868d7a798217304af6d6099bcded5569e','Publication helper drift')
    spec=importlib.util.spec_from_file_location('release_helpers',path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def run(args,log=None):
    p=subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    print(p.stdout,end='',flush=True)
    if log:Path(log).write_text(p.stdout)
    require(p.returncode==0,'Command failed: '+str(args[0]));return p.stdout

def build(repo,base,tools,work,dist):
    require(sha(base.read_bytes())==BASE_SHA,'Baseline MPP mismatch')
    for name,pin in PINS.items():require(sha((tools/name).read_bytes())==pin,'Toolchain mismatch: '+name)
    R=load_helpers(repo);old=R.archive(base);mf=R.headers(old['META-INF/MANIFEST.MF']);require(mf['Version']=='1.0.147','Wrong version')
    work.mkdir(parents=True,exist_ok=False);dist.mkdir(parents=True,exist_ok=False)
    for name in ('host','transform','dex'): (work/name).mkdir()
    (work/'base.dex').write_bytes(old['classes.dex'])
    run(['javac','--release','11','-d',work/'host',ROOT/'NoRecents.java',ROOT/'NoRecentsTest.java'])
    tests=run(['java','-cp',work/'host','NoRecentsTest'],dist/'host-tests.txt');require('PASS 219 manifest assertions' in tests,'Host test total changed')
    run(['java','-cp',tools/'d8.jar','com.android.tools.r8.D8','--min-api','26','--lib',tools/'android.jar','--output',work/'dex',work/'host/app/hiro/brightnessclick/NoRecents.class'])
    run(['javac','-cp',tools/'morphe.jar','-d',work/'transform',ROOT/'TransformBrightness.java'])
    for i in (1,2):
        run(['java','-cp',str(tools/'morphe.jar')+os.pathsep+str(work/'transform'),'TransformBrightness',work/'base.dex',work/'dex/classes.dex',work/f'bundle{i}.dex',work/f'single{i}.dex'],dist/f'transform{i}.txt')
    require((work/'bundle1.dex').read_bytes()==(work/'bundle2.dex').read_bytes(),'Independent bundle transform differs')
    require((work/'single1.dex').read_bytes()==(work/'single2.dex').read_bytes(),'Independent standalone transform differs')
    # Existing APK runtime DEX, extensions and assets remain byte-for-byte identical.
    updated={**old,'classes.dex':(work/'bundle1.dex').read_bytes()}
    mf.update(Version='1.0.148',Timestamp='2026-10-06T00:00:00',Description='BrightnessClick1.0.4: exclude all app activities from Recents at launch. Other apps unchanged from1.0.147. Device untested.')
    updated['META-INF/MANIFEST.MF']=R.manifest(mf);R.write_zip(dist/BUNDLE,updated)
    single={n:b for n,b in old.items() if n.startswith('brightnessclick/')}
    require(len(single)==4,'BrightnessClick payload changed')
    single['classes.dex']=(work/'single1.dex').read_bytes()
    single['META-INF/MANIFEST.MF']=R.manifest({**mf,'Name':'BrightnessClick S26','Version':'1.0.4','Description':'Exclude BrightnessClick from Recents from launch; existing brightness and permission behavior retained. Device untested.'})
    R.write_zip(dist/SINGLE,single)
    actual=R.archive(dist/BUNDLE);changed=sorted(n for n in old if old[n]!=actual[n])
    require(actual.keys()==old.keys() and changed==['META-INF/MANIFEST.MF','classes.dex'],'Unrelated bundle file changes')
    require(all(actual[n]==b for n,b in old.items() if n.startswith('brightnessclick/')),'App-runtime payload changed')
    qa={'status':'PASS_HOST_MANIFEST_AND_DEX_PRESERVATION_DEVICE_UNVERIFIED','bundle_version':'1.0.148','brightness_version':'1.0.4',
        'package':'jp.gr.java_conf.fimyulab.brightnessclick','input_version_name':'1.5','input_version_code':6,'output_version_name':'1.5','output_version_code':10,
        'baseline_sha256':BASE_SHA,'toolchain_sha256':PINS,'manifest_host_assertions':219,'independent_transform_identical':True,
        'unchanged_existing_loader_classes':232,'unchanged_methods_in_other_classes':1080,'unchanged_methods_in_changed_loader':13,
        'changed_existing_loader_methods':['manifest','<clinit>','about'],'added_patcher_only_helper':'app.hiro.brightnessclick.NoRecents',
        'unchanged_archive_entries':47,'changed_archive_entries':changed,'all_brightness_runtime_payloads_unchanged':True,
        'all_other_app_runtime_payloads_unchanged':True,'android_device_tested':False,'original_apk_apply_tested':False,
        'apk_produced':False,'desktop_patch_loader_tested':False,
        'artifacts':{name:{'sha256':sha((dist/name).read_bytes()),'bytes':(dist/name).stat().st_size} for name in (BUNDLE,SINGLE)}}
    (dist/'QA_BrightnessClick_v1.0.4.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
    (dist/'RELEASE_NOTES.txt').write_text(NOTES)
    R.write_zip(dist/'BrightnessClick_v1.0.4_sources_and_QA.zip',{
        **{'src/'+p.name:p.read_bytes() for p in ROOT.iterdir() if p.suffix in ('.java','.py')},
        **{p.name:p.read_bytes() for p in dist.iterdir() if p.suffix in ('.json','.txt')}})
    (dist/'SHA256SUMS.txt').write_text(''.join(sha(p.read_bytes())+'  '+p.name+'\n' for p in sorted(dist.iterdir()) if p.is_file()))
    print(json.dumps(qa,ensure_ascii=False,indent=2),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser()
    for x in ('repo','base','tools','work','dist'):p.add_argument('--'+x,required=True,type=Path)
    a=p.parse_args();build(*(getattr(a,x).resolve() for x in ('repo','base','tools','work','dist')))
