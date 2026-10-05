#!/usr/bin/env python3
"""Reproducible face-feature restoration and diagnostic removal over approved188-lineage1918."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, zipfile

ROOT=Path(__file__).resolve().parent
SINGLE='ULike_HQ_Texture_Online_v1.9.19.mpp'
BUNDLE='Hiro_Morphe_Patches_v1.0.144.mpp'
PINS={
 'ULike_HQ_Texture_Online_v1.8.8.mpp':'dbdaab4f578604b725f6285d1312eb33f52160cbc024a9168b95202b1dde2d79',
 'ULike_HQ_Texture_Online_v1.9.18.mpp':'1d9ee52e86183badccef10c3cc3280ade31a8a921feef7c398ca1114cecb9ae5',
 'Hiro_Morphe_Patches_v1.0.143.mpp':'61cde34775a62225a1b1f13cdd96baedb3833b20a1976d6804b2c208253ff537',
 'morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
 'android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
 'd8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
NOTES='''【統合MPP v1.0.144 / ULike v1.9.19：顔特徴点検出の復元・診断整理】

「顔特徴点の高解像度検出」を設定表示・保存・実処理とも復元します。承認済みv1.8.8のFacePrecision3／FacePrecisionPolicyおよび4か所の呼び出しをそのまま戻します。Face106の高解像度設定とFace240の低解像度化抑制を復元します。初期値はオン、既存のオン／オフ設定は維持します。変更はアプリ再起動後に反映します。メッシュ密度・元SDKの適応分岐を変更する機能ではありません。

「直近の保存結果」「撮影の対応状況・診断を表示」は表示だけでなく、結果の保持・永続化・診断情報収集・整形・表示・コピーと関連する呼び出しを削除します。古い診断用設定値が端末内に残っていても読み書きしません。保存に失敗した場合の通知は、結果履歴を参照せず「保存に失敗しました。」と直接表示する形で維持します。写真や既存の書き出しファイルを削除しません。

設定から戻ると撮影画面、撮影画面から戻ると従来の完全終了処理というv1.9.18の動作を維持します。保存中の終了保護、実撮影の入力・レンズ・最大センサーモード・センサー比率、保存処理、画質・ノイズ・鮮明化、美顔・フィルター・素材通信の実処理は継承します。

土台はユーザー承認済みv1.8.8に基づくv1.9.18です。撤回済みv1.9.17や、その10bit経路は戻しません。以前削除したその他6項目も復活しません。統合版の他アプリ用パッチはv1.0.143から変更しません。

対象：未改造ULike 5.6.2（740）、arm64-v8a。Managerでソース更新後、単体版か統合版の一方で再パッチ・再インストールしてください。ソース更新のみではインストール済みアプリの処理は変わりません。Android／Galaxy実機の起動・操作・撮影・保存試験は未実施です。検証結果と限界は同梱QAに記載します。
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
 work=args.work.resolve();require(not work.exists(),'Use a fresh build work directory');work.mkdir(parents=True);args.output.mkdir(parents=True,exist_ok=True)
 for name,pin in PINS.items():require(sha(((args.tools if name.endswith('.jar') else args.input)/name).read_bytes())==pin,'Pinned input mismatch '+name)
 base=archive(args.input/'ULike_HQ_Texture_Online_v1.9.18.mpp');old=archive(args.input/'ULike_HQ_Texture_Online_v1.8.8.mpp');bundle=archive(args.input/'Hiro_Morphe_Patches_v1.0.143.mpp')
 require(headers(base['META-INF/MANIFEST.MF'])['Version']=='1.9.18' and headers(bundle['META-INF/MANIFEST.MF'])['Version']=='1.0.143','Baseline versions')
 baseline=work/'baseline';approved=work/'approved188'
 for folder,items in [(baseline,base),(approved,old)]:
  for n,raw in items.items():p=folder/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
 (work/'bundle143.dex').write_bytes(bundle['classes.dex']);classes=work/'classes';classes.mkdir();cp=os.pathsep.join(map(str,[args.tools/'morphe.jar',baseline,classes]))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,ROOT/'MergePayloads.java',ROOT/'Transform1919.java',ROOT/'FacePolicyTest1919.java'])
 run(['javac','-encoding','UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'-d',classes,ROOT/'PatchClass1919.java'])
 emitted=work/'emitted';independent=work/'independent'
 for dest in [emitted,independent]:run(['java','-Dfile.encoding=UTF-8','-cp',cp,'Transform1919',baseline,approved,work/'bundle143.dex',dest,work/(dest.name+'-audit.tsv')])
 require({p.name:p.read_bytes() for p in emitted.iterdir()}=={p.name:p.read_bytes() for p in independent.iterdir()},'Independent DEX mismatch')
 run(['java','-Dfile.encoding=UTF-8','--add-exports','java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED','-cp',cp,'PatchClass1919',baseline/'app/hiro/ulike/patches/UlikeHqMaxPatch.class',emitted/'UlikeHqMaxPatch.class'])
 backtests=host_test(work)
 policytests=run(['java','-Dfile.encoding=UTF-8','-cp',cp,'FacePolicyTest1919',emitted/'runtime.dex'],work/'policy-tests.txt')
 final=dict(base);final.update({'classes.dex':(emitted/'loader.dex').read_bytes(),'ulike/runtime.dex':(emitted/'runtime.dex').read_bytes(),'ulike/methods.dex':(emitted/'methods.dex').read_bytes(),'ulike/methods.tsv':(emitted/'methods.tsv').read_bytes(),'app/hiro/ulike/patches/UlikeHqMaxPatch.class':(emitted/'UlikeHqMaxPatch.class').read_bytes()})
 mf=headers(base['META-INF/MANIFEST.MF']);mf.update(Version='1.9.19',Timestamp='2026-10-05T13:00:00',Description='ULike 1.9.19 approved188 lineage: restore high-resolution landmarks; remove last-save history and capture diagnostics; retain error notifications and settings Back. Device not tested.');final['META-INF/MANIFEST.MF']=manifest(mf)
 is_ulike=lambda n:n.startswith(('ulike/','ulike181/','ulike182/','ulike186/','ulike191/','app/hiro/ulike/patches/'))
 kept={n:raw for n,raw in bundle.items() if not is_ulike(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')}
 integrated={**kept,**{n:raw for n,raw in final.items() if is_ulike(n)},'classes.dex':(emitted/'bundle-loader.dex').read_bytes()}
 mf=headers(bundle['META-INF/MANIFEST.MF']);mf.update(Version='1.0.144',Timestamp='2026-10-05T13:00:00',Description='ULike1.9.19: restore high-resolution landmarks, remove two reporting features, preserve navigation/save protections. All other apps unchanged from1.0.143. Device not tested.');integrated['META-INF/MANIFEST.MF']=manifest(mf)
 artifacts={}
 for name,items in [(SINGLE,final),(BUNDLE,integrated)]:
  target=args.output/name;write_zip(target,items);write_zip(work/(name+'.repeat'),items);require(target.read_bytes()==(work/(name+'.repeat')).read_bytes(),'MPP not deterministic');require(archive(target)==items,'Archive mismatch')
  artifacts[name]={'bytes':target.stat().st_size,'sha256':sha(target.read_bytes())}
  listing=run(['java','-Dfile.encoding=UTF-8','-jar',args.tools/'morphe.jar','list-patches','--patches',target],work/(name+'.list.txt'));require('高画質撮影・質感美肌・素材通信を復旧' in listing,'Loader failure')
 expected_changes={'META-INF/MANIFEST.MF','classes.dex','ulike/runtime.dex','ulike/methods.dex','ulike/methods.tsv','app/hiro/ulike/patches/UlikeHqMaxPatch.class'}
 require(set(final)==set(base),'Unexpected archive entry set change');require({n for n in base if base[n]!=final[n]}==expected_changes,'Unexpected byte changes')
 require({n:raw for n,raw in integrated.items() if not is_ulike(n) and n not in ('classes.dex','META-INF/MANIFEST.MF')}==kept,'Other application resource changed')
 for n in final:require(not n.startswith(('ulike181/','ulike182/','ulike191/')),'Withdrawn/trial payload restored')
 audit=(work/'emitted-audit.tsv').read_text();require('other_app_classes_unchanged=220' in audit and 'face_hooks_restored=4' in audit,'Incomplete code audit')
 qa={'schema':'ulike1919-face-restoration-diagnostic-cleanup-1','result':'PASS_BUILD_HOST_TESTS_STRUCTURAL_CHECKS','ulike_version':'1.9.19','bundle_version':'1.0.144','base_ulike_version':'1.9.18','approved_lineage_version':'1.8.8','base_bundle_version':'1.0.143','android_device_tested':False,'original_apk_apply_tested':False,'restored_face_classes_exact188':3,'restored_face_hook_spans_exact188':4,'restored_face_default_enabled':True,'existing_face_preference_preserved':True,'face_preference_changes_require_restart':True,'removed_diagnostic_classes':13,'remaining_runtime_classes':216,'remaining_runtime_methods':1305,'stock_method_contracts':170,'host_back_routing_assertions':11,'host_restored_policy_dex_cases':20,'host_test_result':backtests.strip(),'host_policy_test_result':policytests.strip(),'camera_and_settings_back_classes_exact1918':True,'save_exit_jobs_class_exact1918':True,'deleted_feature_references_remaining':0,'deleted_setting_labels_remaining_in_runtime':0,'other_app_loader_classes_identical':220,'other_app_resources_identical':True,'non_dex_native_and_asset_bytes_identical':True,'failure_notification_preserved_without_history':True,'changed_archive_entries':sorted(expected_changes),'independent_dex_and_mpp_rebuild_identical':True,'artifacts':artifacts,'input_tool_pins':PINS,'source_sha256':{p.name:sha(p.read_bytes()) for p in ROOT.iterdir() if p.suffix in ('.py','.java')},'limitations':['No Android/Galaxy physical-device navigation/camera/save execution.','Policy tests interpret only the restored pure policy DEX on the host, not the camera SDK or Android runtime.','Existing diagnostic preference values and existing user photos/files are not deleted; removed reporting code does not read/write them.']}
 (args.output/'QA_ULike_v1.9.19.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(args.output/'RELEASE_NOTES.txt').write_text(NOTES)
 shutil.copyfile(work/'emitted-audit.tsv',args.output/'cleanup-audit1919.tsv');shutil.copyfile(work/'host-tests.txt',args.output/'host-tests1919.txt');shutil.copyfile(work/'policy-tests.txt',args.output/'policy-tests1919.txt')
 print(json.dumps(qa,ensure_ascii=False,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for flag in ('input','tools','work','output'):p.add_argument('--'+flag,type=Path,required=True)
 a=p.parse_args()
 for k,v in vars(a).items():setattr(a,k,v.resolve())
 build(a)
