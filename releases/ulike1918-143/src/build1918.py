#!/usr/bin/env python3
"""Reproducible removal-only ULike update over exact188, with bundle142 non-ULike bytes retained."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, zipfile

ROOT=Path(__file__).resolve().parent
SINGLE='ULike_HQ_Texture_Online_v1.9.18.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.143.mpp'
PINS={
 'ULike_HQ_Texture_Online_v1.8.8.mpp':'dbdaab4f578604b725f6285d1312eb33f52160cbc024a9168b95202b1dde2d79',
 'Hiro_Morphe_Patches_v1.0.142.mpp':'a7ab3ee725986779c01a2a50ac6ccd7b976968e5ef7aab8b7a81ea88bc140742',
 'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
 'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
NOTES='''【統合MPP v1.0.143 / ULike v1.9.18：v1.8.8基準・設定削除と戻る操作】

指定の7項目を、表示だけでなく関連する実装と呼び出しから削除します。
顔特徴点の高解像度検出／保存専用入力の状態（1.6.3）／美顔AI実機試験（1.8.1）／美顔の形状・透明度を取得（1.8.2）／対象スタイルのAIモデルを保存／スタイルの素材・設定を端末に保存／保存専用の美顔撮影診断。

高解像度特徴点の強制設定、試験用AI推論、形状・透明度の試験取得、モデル・素材の書き出し、診断用素材API履歴、保存専用撮影の診断記録・コピー・表示を除去。不要になったONNX/試験用readbackのライブラリ配布も除去します。旧設定値が残っていても削除済み機能は再有効化しません。以前ユーザーが書き出したファイル・既存写真は削除しません。

設定画面のAndroid戻るは設定Activityだけを閉じ、背後の撮影画面へ戻ります。キー入力とOnBackInvokedの共通分岐で処理します。撮影画面ではv1.8.8の終了処理をそのまま実行し、保存待ち・カメラ解放等の終了保護を維持します。

通常の美顔処理、保存用入力の実処理、「写真の入力JPEG圧縮をなくす」、ノイズ・鮮明化・画質処理、内蔵素材準備、レンズ・プレビュー制御を継承します。削除指定の高解像度特徴点強制だけは適用しなくなります。

バージョン番号は更新判定のためv1.9.18ですが、土台はユーザー指定のv1.8.8です。不具合撤回済みv1.9.17や、その10bit改造経路を戻したものではありません。統合版の他アプリはv1.0.142と同一です。

検証の内容・限界は同梱QA参照。Android/Galaxy実機の操作・撮影試験は未実施です。パッチのソース更新だけではアプリは変わりません。対応する未改造ULike 5.6.2（740）arm64-v8aに、単体または統合版の一方で再パッチ・再インストールしてください。
'''

def sha(raw):return hashlib.sha256(raw).hexdigest()
def require(ok,message):
 if not ok:raise RuntimeError(message)
def run(args,log=None):
 print('RUN',str(args[0]),str(args[-1]),flush=True)
 p=subprocess.run(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 if log is not None:Path(log).write_text(p.stdout)
 print(p.stdout,end='',flush=True);p.check_returncode();return p.stdout

def archive(path):
 with zipfile.ZipFile(path) as z:
  require(z.testzip() is None,'Corrupt ZIP '+str(path));require(len(set(z.namelist()))==len(z.namelist()),'Duplicate ZIP names')
  for n in z.namelist():require(not Path(n).is_absolute() and '..' not in Path(n).parts,'Unsafe archive path')
  return {n:z.read(n) for n in z.namelist() if not n.endswith('/')}
def write_zip(path,items):
 with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
  for name in sorted(items,key=lambda x:(x!='META-INF/MANIFEST.MF',x)):
   i=zipfile.ZipInfo(name,(2026,10,5,12,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16
   z.writestr(i,items[name],compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
def headers(raw):
 rows=[]
 for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
  if line.startswith(b' '):rows[-1]+=line[1:]
  elif line:rows.append(line)
 pairs=[line.decode().split(': ',1) for line in rows];require(len(pairs)==len(dict(pairs)),'Duplicate manifest key');return dict(pairs)
def manifest(fields):
 lines=[]
 for key,value in fields.items():
  require(not any(c in value for c in '\r\n\0'),'Invalid manifest value')
  raw=(key+': '+value).encode();prefix=b''
  while len(prefix)+len(raw)>72:
   cut=72-len(prefix)
   while raw[cut]&192==128:cut-=1
   lines.append(prefix+raw[:cut]);raw=raw[cut:];prefix=b' '
  lines.append(prefix+raw)
 out=b'\r\n'.join(lines)+b'\r\n\r\n';require(headers(out)==fields,'Manifest roundtrip');return out

def host_test(work):
 src=work/'host-src';out=work/'host-class';src.mkdir();out.mkdir()
 stubs={
 'android/os/Looper.java':'''package android.os; public class Looper { private static final Looper MAIN=new Looper(); public static boolean onMain=true; public static Looper getMainLooper(){return MAIN;} public static Looper myLooper(){return onMain?MAIN:null;} }''',
 'android/app/Activity.java':'''package android.app; public class Activity { public boolean finishing,destroyed; public int finishes,posts; public Runnable pending; public boolean isFinishing(){return finishing;} public boolean isDestroyed(){return destroyed;} public void finish(){finishes++;finishing=true;} public void runOnUiThread(Runnable r){posts++;pending=r;} }''',
 'com/light/beauty/basisplatform/appsetting/AppSettingsActivity.java':'''package com.light.beauty.basisplatform.appsetting; public class AppSettingsActivity extends android.app.Activity {}''',
 'BackRoutingTest.java':'''import android.app.Activity; import android.os.Looper; import com.hiro.ulike.SettingsReturn1918; import com.light.beauty.basisplatform.appsetting.AppSettingsActivity;
public class BackRoutingTest { static int tests; static class SubSettings extends AppSettingsActivity{} static void check(boolean b){if(!b)throw new AssertionError("case "+tests);tests++;}
public static void main(String[] args){
 check(!SettingsReturn1918.returnToCameraIfSettings(null));
 Activity camera=new Activity();check(!SettingsReturn1918.returnToCameraIfSettings(camera)&&camera.finishes==0&&camera.posts==0);
 AppSettingsActivity settings=new AppSettingsActivity();check(SettingsReturn1918.returnToCameraIfSettings(settings)&&settings.finishes==1&&settings.posts==0);
 check(SettingsReturn1918.returnToCameraIfSettings(settings)&&settings.finishes==1);
 AppSettingsActivity dead=new AppSettingsActivity();dead.destroyed=true;check(SettingsReturn1918.returnToCameraIfSettings(dead)&&dead.finishes==0);
 SubSettings sub=new SubSettings();check(SettingsReturn1918.returnToCameraIfSettings(sub)&&sub.finishes==1);
 Looper.onMain=false;AppSettingsActivity posted=new AppSettingsActivity();check(SettingsReturn1918.returnToCameraIfSettings(posted)&&posted.finishes==0&&posted.posts==1);Looper.onMain=true;posted.pending.run();check(posted.finishes==1);
 Looper.onMain=false;AppSettingsActivity destroyedLater=new AppSettingsActivity();check(SettingsReturn1918.returnToCameraIfSettings(destroyedLater)&&destroyedLater.posts==1);destroyedLater.destroyed=true;Looper.onMain=true;destroyedLater.pending.run();check(destroyedLater.finishes==0);
 check(!SettingsReturn1918.returnToCameraIfSettings(camera)&&camera.finishes==0);
 System.out.println("PASS "+tests+" host assertions: null/camera/settings/repeat/destroyed/subclass/main-thread routing. Android device not tested."); }}'''
 }
 for name,text in stubs.items():p=src/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 run(['javac','-d',out,*sorted(src.rglob('*.java')),ROOT/'SettingsReturn1918.java'])
 return run(['java','-cp',out,'BackRoutingTest'],work/'host-tests.txt')

def build(args):
 work=args.work.resolve();require(not work.exists(),'Use a fresh build work directory');work.mkdir(parents=True)
 args.output.mkdir(parents=True,exist_ok=True)
 for name,digest in PINS.items():
  p=(args.tools if name.endswith('.jar') else args.input)/name;require(sha(p.read_bytes())==digest,'Pinned input/tool mismatch '+name)
 base=archive(args.input/'ULike_HQ_Texture_Online_v1.8.8.mpp');bundle=archive(args.input/'Hiro_Morphe_Patches_v1.0.142.mpp')
 require(headers(base['META-INF/MANIFEST.MF'])['Version']=='1.8.8','Base version');require(headers(bundle['META-INF/MANIFEST.MF'])['Version']=='1.0.142','Bundle version')
 baseline=work/'baseline';baseline.mkdir()
 for n,raw in base.items():p=baseline/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
 (work/'bundle142.dex').write_bytes(bundle['classes.dex'])
 classes=work/'classes';classes.mkdir();cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]))
 run(['javac','-cp',cp,'-d',classes,ROOT/'MergePayloads.java',ROOT/'Clean1918.java'])
 run(['javac','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-d',classes,ROOT/'CleanClass1918.java'])
 helper=work/'helper-classes';helper.mkdir();helperdex=work/'helper-dex';helperdex.mkdir()
 run(['javac','-source','8','-target','8','-Xlint:-options','-cp',args.tools/'android.jar','-d',helper,ROOT/'SettingsReturn1918.java'])
 run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--lib',args.tools/'android.jar','--min-api','26','--output',helperdex,*sorted(helper.rglob('*.class'))])
 tests=host_test(work)
 emitted=work/'emitted';emitted2=work/'independent'
 for dest in [emitted,emitted2]:run(['java','-cp',cp,'Clean1918',baseline,helperdex/'classes.dex',work/'bundle142.dex',dest,work/(dest.name+'-audit.tsv')])
 require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in emitted2.iterdir()},'Independent DEX mismatch')
 run(['java','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',classes,'CleanClass1918',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'])
 remove=lambda n:n.startswith(('ulike181/','ulike182/','app/hiro/ulike/patches/IntegrationPayload181','app/hiro/ulike/patches/IntegrationPayload182'))
 final={n:raw for n,raw in base.items() if not remove(n)}
 final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'ulike/methods.dex':(emitted/'methods.dex').read_bytes(),'ulike/methods.tsv':(emitted/'methods.tsv').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
 mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version='1.9.18',Timestamp='2026-10-05T12:00:00',Description='ULike 1.9.18 based on exact1.8.8: remove seven diagnostic/experimental features; settings Back returns to camera, camera Back retains existing exit. Device not tested.')
 final['META-INF/MANIFEST.MF']=manifest(mf)
 is_ulike=lambda n:n.startswith(('ulike/','ulike181/','ulike182/','ulike186/','ulike191/','app/hiro/ulike/patches/'))
 kept={n:raw for n,raw in bundle.items() if not is_ulike(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')}
 integrated={**kept,**{n:raw for n,raw in final.items() if is_ulike(n)},'classes.dex':(emitted/'bundle-loader.dex').read_bytes()}
 mf2=headers(bundle['META-INF/MANIFEST.MF']);mf2.update(Version='1.0.143',Timestamp='2026-10-05T12:00:00',Description='ULike1.9.18 settings cleanup over approved1.8.8; settings Back returns to camera; all non-ULike apps retained from bundle1.0.142. Device not tested.');integrated['META-INF/MANIFEST.MF']=manifest(mf2)
 artifacts={}
 for name,items in [(SINGLE,final),(BUNDLE,integrated)]:
  print('PACK',name,flush=True)
  target=args.output/name;write_zip(target,items);write_zip(work/(name+'.repeat'),items);require(target.read_bytes()==(work/(name+'.repeat')).read_bytes(),'MPP packaging not deterministic');require(archive(target)==items,'MPP content mismatch')
  print('PACK VERIFIED',name,flush=True)
  artifacts[name]={'bytes':target.stat().st_size,'sha256':sha(target.read_bytes())}
 require({n:raw for n,raw in integrated.items() if not is_ulike(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')}==kept,'Non-ULike resources changed')
 for n in ['ulike/assets/0000.bin','ulike/assets/0001.bin','ulike/assets/0002.bin','ulike/assets.tsv','ulike186/runtime/0000.bin','ulike186/resourceitems.tsv']:require(final[n]==base[n],'Image/native/asset payload changed '+n)
 for name in [SINGLE,BUNDLE]:
  listing=run(['java','-jar',args.tools/'morphe.jar','list-patches','--patches',args.output/name],work/(name+'.list.txt'))
  require('高画質撮影・質感美肌・素材通信を復旧' in listing,'Patch loader not found')
 audit=(work/'emitted-audit.tsv').read_text();require('other_app_classes_unchanged=220' in audit,'Bundle audit missing')
 qa={'schema':'ulike1918-cleanup-1','result':'PASS_BUILD_HOST_TESTS_STRUCTURAL_CHECKS', 'ulike_version':'1.9.18','bundle_version':'1.0.143','base_ulike_version':'1.8.8','base_bundle_version':'1.0.142','rejected_1917_restored':False,'android_device_tested':False,'original_apk_apply_tested':False,'original_apk_apply_report':'Separate APK_APPLY1918_QA.json when supplied; never infer from host tests.', 'host_assertions':11,'host_test_result':tests.strip(),'removed_runtime_classes':181,'added_runtime_classes':2,'remaining_runtime_classes':226,'removed_loader_classes':10,'remaining_ulike_loader_classes':13,'unchanged_other_app_loader_classes':220,'unchanged_other_app_resource_entries':len(kept),'original_stock_method_contracts_preserved':170,'deleted_resource_entries':sorted(n for n in base if remove(n)),'native_image_processor_sha256':sha(final['ulike186/runtime/0000.bin']),'native_and_bundled_beauty_assets_byte_identical':True,'camera_exit_tail_bytecode_contract_identical':True,'dangling_removed_code_references':0,'seven_setting_labels_absent':True,'diagnostic_state_writers_exporters_and_ai_tests_removed':True,'independent_dex_and_mpp_rebuild_identical':True,'artifacts':artifacts,'input_tool_pins':PINS,'source_sha256':{p.name:sha(p.read_bytes()) for p in sorted(ROOT.iterdir()) if p.suffix in ('.py','.java')},'limitations':['No Android/Galaxy physical-device navigation/camera/save execution.','Removing the high-resolution landmark override intentionally returns that setting to the underlying application defaults.','Existing exported user files and old preference storage are left intact but are not read by the removed features.']}
 (args.output/'QA_ULike_v1.9.18.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 (args.output/'RELEASE_NOTES.txt').write_text(NOTES)
 shutil.copyfile(work/'emitted-audit.tsv',args.output/'cleanup-audit1918.tsv');shutil.copyfile(work/'host-tests.txt',args.output/'host-tests1918.txt')
 write_zip(args.output/'ULike_v1.9.18_sources_and_QA.zip',{**{'src/'+p.name:p.read_bytes() for p in ROOT.iterdir() if p.suffix in ('.java','.py')},'QA.json':(args.output/'QA_ULike_v1.9.18.json').read_bytes(),'cleanup-audit.tsv':audit.encode(),'host-tests.txt':tests.encode(),'RELEASE_NOTES.txt':NOTES.encode()})
 print(json.dumps(qa,ensure_ascii=False,indent=2),flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for flag in ('input','tools','work','output'):p.add_argument('--'+flag,type=Path,required=True)
 a=p.parse_args()
 for k,v in vars(a).items():setattr(a,k,v.resolve())
 build(a)
