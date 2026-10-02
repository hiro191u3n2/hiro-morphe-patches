#!/usr/bin/env python3
"""Overlay reviewed source on the pinned 1.7.6 release; finalize only exact locally applied MPP bytes."""
import sys,json,hashlib,shutil,zipfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
PINS={'ULike_HQ_Texture_Online_v1.7.6.mpp':'4d7d889b178615ad13ee271ef49076c12f80aedc19d32d4d42779946c75d5da8','Hiro_Morphe_Patches_v1.0.109.mpp':'d0476392bdf009f346874f943599fcfae4bb76fe960327e454c1c3cfc44ef31b'}
DESC='v1.7.7 黒画面対策。物理レンズの未対応出力形式を照会しただけでカメラを失敗扱いにする処理を修正。共通サイズなしは空一覧とし自動選択へ戻さない。起動復旧をレンズボタン表示から分離。メイン1倍・接写・3倍5倍・AF・画質・美顔・HEIF・保存高速化を保持。Galaxy実機未検証。'
NOTES='''【統合MPP v1.0.110：ULike v1.7.7 黒画面対策】
前版のDEXを調査し、出力形式の能力照会と実カメラの停止処理が混在している不具合を修正しました。物理レンズに非対応の形式や共通サイズがない形式を照会すると、使用していない形式でもルート全体を失敗扱いにしていました。今回は対応サイズの共通部分だけを返し、非対応形式は空の一覧とします。論理カメラだけの未確認サイズへは戻しません。実際の出力セッション構成失敗の検出は維持します。

また、復旧に必要な画面の登録がレンズボタンの描画条件に依存していました。登録を撮影画面の接続時へ移し、ボタンの位置・重なり判定が終わる前でも、表示中の撮影画面で復旧判断できるように変更しました。非表示・バックグラウンド・録画・保存中の保護は残しています。

1倍のメイン広角固定、接写・3倍・5倍の手動選択、距離自動切替なし、AF、美顔、HEIF、保存画質、保存高速化は変更していません。変更は既存ランタイムの4メソッド（出力照会2箇所、画面接続1箇所、診断バージョン1箇所）と新規ヘルパーに限定し、164個のアプリ置換メソッドとその他のアプリ用パッチは同一です。

追加130件を含む1060件のホスト検査、未改造ULike 5.6.2(740)への単体・総合パッチ適用とAPK再構築、各1431メソッドの一致照合を実施。両APKの3999エントリは内容一致です。これはAndroid/Galaxy実機検査ではありません。端末ログ未取得のため、今回の不具合が画像の黒画面の唯一の原因だったこと、実機で解消したことや起動時間の改善量は未確認です。

Managerを更新し、未改造のULike 5.6.2(740)に単体または総合のどちらか一方を適用してください。パッチ済みAPKへの重ねがけはしないでください。
'''
def sha(b):return hashlib.sha256(b).hexdigest()
def prepare(r):
 for name,dest in [('Preview177.java','src170/com/hiro/ulike/Preview177.java'),('Transform177.java','Transform177.java'),('Preview177Test.java','host170/com/hiro/ulike/Preview177Test.java')]:shutil.copyfile(HERE/name,r/dest)
 p=r/'build170.py';s=p.read_text();start=s.index('PINS=');end=s.index('\nTOOLS=');s=s[:start]+'PINS='+repr(PINS)+s[end:]
 s=s.replace('d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a','1db2419dbd45457f83b90ef8a7ef41e118174782c9a5c2185b55899df022767f').replace('v1.7.5','v1.7.6').replace('v1.0.108','v1.0.109')
 s='\n'.join(line for line in s.splitlines() if not "with zipfile.ZipFile(a.input/'ULike_HQ_Texture_Online_v1.7.4.mpp')" in line)+'\n'
 s=s.replace("{'ManualLens170','MainMacro168','InventoryCache173','StartupFocus173','Startup175'}","{'Preview177'}").replace('len(helperclasses)>=8','len(helperclasses)==1').replace('Transform175','Transform177').replace(",'CheckRebase176.java'",'')
 s=s.replace("('CheckRebase176',[b/'baseline174-runtime.dex',b/'old-runtime.dex'],'REBASE176.txt'),",'')
 s=s.replace("run('bash',ROOT/'test170.sh');","run('bash',ROOT/'test170.sh');run('java','-cp',ROOT/'host170-classes','com.hiro.ulike.Preview177Test',log=ROOT/'qa170/HOST_PREVIEW177.txt');")
 s=s.replace('Reproduce v1.7.6 from published save-speed v1.7.6','Reproduce v1.7.7 from published v1.7.6');(r/'build177.py').write_text(s)
 p=r/'package170.py';s=p.read_text();start=s.index('PINNED=');end=s.index('\nCLASS=');s=s[:start]+'PINNED='+repr({'standalone':PINS['ULike_HQ_Texture_Online_v1.7.6.mpp'],'integrated':PINS['Hiro_Morphe_Patches_v1.0.109.mpp']})+s[end:];p.write_text(s)
 p=r/'test170.sh';p.write_text(p.read_text().replace('"$R/src170/com/hiro/ulike/Startup175.java" >>','"$R/src170/com/hiro/ulike/Startup175.java" "$R/src170/com/hiro/ulike/Preview177.java" >>'))
 (r/'host170/android/hardware/camera2/params/StreamConfigurationMap.java').write_text('package android.hardware.camera2.params;import android.util.Size;import java.util.*;public class StreamConfigurationMap {public final Map<Object,Size[]> values=new HashMap<>();public boolean unsupported=false;public int queries;public Size[] getOutputSizes(int format){queries++;if(unsupported)throw new IllegalArgumentException("unsupported format");return values.get(format);}public Size[] getOutputSizes(Class<?> type){queries++;if(unsupported)throw new IllegalArgumentException("unsupported class");return values.get(type);}}')
 (r/'host170/android/util/Size.java').write_text('package android.util;public final class Size {private final int w,h;public Size(int w,int h){this.w=w;this.h=h;}public int getWidth(){return w;}public int getHeight(){return h;}public boolean equals(Object o){return o instanceof Size&&w==((Size)o).w&&h==((Size)o).h;}public int hashCode(){return w*31+h;}public String toString(){return w+"x"+h;}}')
 p=r/'host170/android/view/View.java';p.parent.mkdir(exist_ok=True);p.write_text('package android.view;public class View{public boolean shown=true,focused=true,attached=true;public int windowVisibility=0;public boolean isShown(){return shown&&attached;}public boolean hasWindowFocus(){return focused;}public int getWindowVisibility(){return windowVisibility;}}')
 p=r/'host170/com/hiro/ulike/OpticalZoom.java';s=p.read_text().replace('static boolean hostReady(){return hostOK;}','static Map<android.hardware.camera2.params.StreamConfigurationMap,Route> maps=new WeakHashMap<>();static WeakReference<android.view.View> visibleHost=new WeakReference<>(null);static boolean realHostCheck;static void host(android.view.View view){visibleHost=new WeakReference<>(view);}static boolean hostReady(){if(!realHostCheck)return hostOK;android.view.View v=visibleHost.get();return v!=null&&v.isShown()&&v.getWindowVisibility()==0&&v.hasWindowFocus();}').replace('static void reset(){','static void reset(){realHostCheck=false;visibleHost=new WeakReference<>(null);maps.clear();');p.write_text(s)
 rel=json.loads((r/'release170.json').read_text());rel['timestamp']='2026-10-02T09:43:30';rel['patch_description']=DESC;rel['manifest_description']='Nonfatal physical stream capability queries and early preview host registration. Fixed optics and image quality retained; Galaxy untested.'
 rel['standalone'].update(version='1.7.7',filename='ULike_HQ_Texture_Online_v1.7.7.mpp');rel['integrated'].update(version='1.0.110',filename='Hiro_Morphe_Patches_v1.0.110.mpp');(r/'release170.json').write_text(json.dumps(rel,ensure_ascii=False,indent=2)+'\n')
 (r/'release_pipeline/publish177.py').write_text("import publish\npublish.VERSION,publish.PREVIOUS_APP='1.7.7','1.7.6'\npublish.BUNDLE,publish.PREVIOUS='1.0.110','1.0.109'\npublish.TAG='ulike-v1.7.7'\nif __name__=='__main__':publish.main()\n")
 (r/'CHANGES177.md').write_text('# ULike v1.7.7 / Hiro Morphe v1.0.110\n\n'+NOTES)
 (r/'release_pipeline/RELEASE_NOTES.txt').write_text(NOTES)
 (r/'release_pipeline/CHANGELOG_SUMMARY.txt').write_text('能力照会による誤停止を修正し、黒画面復旧をレンズボタン表示から分離。画質・手動固定を保持。実機未検証。\n')
 shutil.copyfile(HERE/'LOCAL_QA.json',r/'release_pipeline/LOCAL_QA.json')
 shutil.copyfile(__file__,r/'reproduce177.py')
def finish(r,d):
 raw=(HERE/'LOCAL_QA.json').read_bytes();q=json.loads(raw);assert q['version']=='1.7.7' and q['bundle_version']=='1.0.110' and q['android_device_tested'] is False
 for item in q['artifacts']:
  data=(d/item['file']).read_bytes();assert len(data)==item['bytes'] and sha(data)==item['sha256'],'Not the locally applied bytes: '+item['file']
 for n,t in {'HOST_PREVIEW177.txt':'130 assertions','HOST_MANUAL170.txt':'596 assertions','HOST_STARTUP175.txt':'60 assertions','HOST_STARTUP173.txt':'64 assertions','HOST_LIFECYCLE172.txt':'44 assertions','HELPER_REFERENCES.txt':'1431 total payload/runtime methods','RUNTIME_TRANSFORM.txt':'preserved=1253, changed=4, added=10','ACCESS_AFTER.txt':'PRIVATE_ACCESS_VIOLATIONS=0'}.items():assert t in (r/'qa170'/n).read_text(),n
 for n,t in q['local_applied_evidence'].items():assert Path(n).name==n;(r/'qa170'/n).write_text(t)
 for n in ['HOST_TESTS.txt','HOST_FAST167_TESTS.txt']:shutil.copyfile(r/'regression170/qa'/n,r/'qa170'/n)
 (d/'QA_ULike_v1.7.7.json').write_bytes(raw)
 files=[]
 for f in r.rglob('*'):
  if not f.is_file():continue
  n=f.relative_to(r).as_posix()
  if (f.parent==r and f.suffix in {'.py','.java','.md','.sh','.json'} and f.name!='SOURCE_FILES.json') or (n.startswith(('src170/','stubs170/','host170/','regression170/')) and f.suffix=='.java') or n=='regression170/test.sh' or (n.startswith('qa170/') and f.suffix in {'.txt','.json'}) or (n.startswith('release_pipeline/') and f.suffix in {'.py','.md','.txt','.json'}):files.append(n)
 manifest={n:sha((r/n).read_bytes()) for n in sorted(files)};(r/'SOURCE_FILES.json').write_text(json.dumps(manifest,indent=2)+'\n');files.append('SOURCE_FILES.json')
 with zipfile.ZipFile(d/'ULike_v1.7.7_sources_and_QA.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n in sorted(files):
   info=zipfile.ZipInfo(n,(2026,10,2,9,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,(r/n).read_bytes())
 print('PASS exact locally applied MPP bytes; 1060 host checks; source-only archive; Android/Galaxy not tested.')
if __name__=='__main__':
 if sys.argv[1]=='prepare':prepare(Path(sys.argv[2]).resolve())
 elif sys.argv[1]=='finish':finish(Path(sys.argv[2]).resolve(),Path(sys.argv[3]).resolve())
 else:raise SystemExit('prepare SOURCE or finish SOURCE DIST')
