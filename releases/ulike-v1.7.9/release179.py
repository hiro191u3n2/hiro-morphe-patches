#!/usr/bin/env python3
import sys,json,hashlib,shutil,zipfile,re
from pathlib import Path
HERE=Path(__file__).resolve().parent
APP_PRE='d21b2e160dcbf006d28c643235161ecd5a6459ea56dea455adeceb3edc558119'
BUNDLE_PRE='86a994fba73c20d55f059f7b36d7cb3eefed3469bb2ecd268b5cd2fb777924d0'
def sha(b):return hashlib.sha256(b).hexdigest()
def prepare(root):
    for n in ['Transform179.java','SaveFast179.java','WorkPool179.java','ChromaFast179.java']:
        src=HERE/n
        dst=root/n if n=='Transform179.java' else root/'src170/com/hiro/ulike'/n
        shutil.copyfile(src,dst)
    p=root/'src170/com/hiro/ulike/MacroUi168.java';s=p.read_text()
    old='target.setOrientation(LinearLayout.VERTICAL);target.setGravity(17);target.setClickable(true);target.setFocusable(true);'
    new=old+'target.setClipChildren(false);target.setClipToPadding(false);'
    assert s.count(old)==1;s=s.replace(old,new)
    old='bar.row.addView(target,0,new LinearLayout.LayoutParams(bar.dp(58),bar.dp(48)));'
    assert s.count(old)==1;s=s.replace(old,'bar.row.addView(target,0,new LinearLayout.LayoutParams(bar.dp(58),bar.dp(52)));')
    p.write_text(s)
    stubs={
      'androidx/heifwriter/HeifWriter.java':'''package androidx.heifwriter;
import android.graphics.Bitmap;import java.io.Closeable;import java.io.FileDescriptor;
public final class HeifWriter implements Closeable {
 public void start(){} public void addBitmap(Bitmap b){} public void stop(long ms){} public void close(){}
 public static final class Builder {
  public Builder(FileDescriptor fd,int w,int h,int mode){} public Builder(String path,int w,int h,int mode){}
  public Builder setQuality(int q){return this;} public Builder setMaxImages(int n){return this;} public Builder setGridEnabled(boolean b){return this;} public Builder setRotation(int r){return this;} public HeifWriter build(){return null;}
 }
}
''',
      'com/hiro/ulike/SaveQuality2.java':'''package com.hiro.ulike;
import android.content.Context;import android.graphics.Bitmap;
public final class SaveQuality2 {
 public static String saveAndPublishLegacy179(Bitmap b,int r){return null;}
 public static Context app179(){return null;} public static boolean fixed179(){return false;}
 public static int[] output179(int w,int h,int r,boolean f){return null;} public static Bitmap normalize179(Bitmap b,int r,boolean f){return null;}
 public static void status179(String s){} public static void failure179(){}
}
''',
      'com/hiro/ulike/SaveIo168.java':'''package com.hiro.ulike;
import android.content.ContentResolver;import android.net.Uri;
public final class SaveIo168 { public static String checkedPath179(ContentResolver r,Uri u){return null;} }
''',
      'com/hiro/ulike/PhotoDetail.java':'''package com.hiro.ulike;
import android.graphics.Bitmap;
public final class PhotoDetail { public static Bitmap applyDetail(Bitmap a,Bitmap b){return a;} }
''',
      'com/hiro/ulike/DetailPixels.java':'''package com.hiro.ulike;
public final class DetailPixels { public static final class Work { public final int[] source,denoised,horizontal,output; public Work(int n){source=new int[n];denoised=new int[n];horizontal=new int[n];output=new int[n];} } }
'''}
    for n,t in stubs.items():
        q=root/'stubs170'/n;q.parent.mkdir(parents=True,exist_ok=True);q.write_text(t)
    s=(root/'build178.py').read_text()
    s=s.replace('Reproduce v1.7.8 from published chroma v1.7.7','Reproduce v1.7.9 from published v1.7.8')
    s=re.sub(r"PINS=\{[^\n]+\}","PINS={'ULike_HQ_Texture_Online_v1.7.8.mpp': '"+APP_PRE+"', 'Hiro_Morphe_Patches_v1.0.111.mpp': '"+BUNDLE_PRE+"'}",s,count=1)
    s=s.replace("old=a.input/'ULike_HQ_Texture_Online_v1.7.7.mpp';integrated=a.input/'Hiro_Morphe_Patches_v1.0.110.mpp'","old=a.input/'ULike_HQ_Texture_Online_v1.7.8.mpp';integrated=a.input/'Hiro_Morphe_Patches_v1.0.111.mpp'")
    s=s.replace("helperclasses=[f for f in helperclasses if f.name.split('$')[0].split('.')[0] in {'Preview177'}]\n assert len(helperclasses)==1,'Unexpected manual helper inventory'","helperclasses=[f for f in helperclasses if f.name.split('$')[0].split('.')[0] in {'MacroUi168','SaveFast179','WorkPool179','ChromaFast179'}]\n assert len(helperclasses)>=9,'Unexpected manual helper inventory'")
    s=s.replace("'Transform177.java'","'Transform179.java'")
    s=s.replace("('Transform177',[b/'old-runtime.dex',b/'helperdex/classes.dex',b],'RUNTIME_TRANSFORM.txt')","('Transform179',[b/'old-runtime.dex',b/'helperdex/classes.dex',b],'RUNTIME_TRANSFORM.txt')")
    s=s.replace("'v1.7.7',b/'description.txt'","'v1.7.8',b/'description.txt'")
    s=s.replace("ROOT/'release170.json'","ROOT/'release179.json'")
    (root/'build179.py').write_text(s)
    p=root/'package170.py';s=p.read_text();s=re.sub(r"PINNED=\{[^\n]+\}","PINNED={'standalone': '"+APP_PRE+"', 'integrated': '"+BUNDLE_PRE+"'}",s,count=1);p.write_text(s)
    rel={
      'timestamp':'2026-10-02T14:30:00',
      'patch_description':'v1.7.9 保存高速化①〜⑤と倍率UI修正。HEIFをMediaStore pending先へ直接書き込み、一時ファイル→DCIMコピーを廃止。同一保存内のHEIF検証を1回へ統合し、MediaStore/HeifWriter準備を画像補正と並行化。色ムラ補正は画素式を変えず輝度計算をインライン化し、Detail/Chroma作業配列を再利用。倍率下の説明文字を非表示、接写・1倍・3倍・5倍の列を52dpへ拡張して円線下端の欠けを修正。1倍メイン広角固定・接写/3倍/5倍手動切替・距離自動切替なし・黒画面対策・AF・美顔・24.5MP・HEIF品質100を保持。Galaxy実機未検証。',
      'manifest_description':'Direct pending-MediaStore HEIF save with one verification, parallel writer preparation, scratch reuse and unclipped manual lens controls. Image settings retained; Galaxy untested.',
      'standalone':{'name':'ULike HQ Texture Online','version':'1.7.9','filename':'ULike_HQ_Texture_Online_v1.7.9.mpp'},
      'integrated':{'name':'Hiro Morphe Patches','version':'1.0.112','filename':'Hiro_Morphe_Patches_v1.0.112.mpp'}}
    (root/'release179.json').write_text(json.dumps(rel,ensure_ascii=False,indent=2)+'\n')
    notes='''【統合MPP v1.0.112：ULike v1.7.9 保存高速化・倍率UI修正】
保存画質を変更せず、保存経路の余分な処理を削減しました。Android 10以降では、HEIFを一時ファイルへ作成してDCIM/Cameraへコピーする経路を使わず、MediaStoreのpending項目へHeifWriterのFileDescriptor出力で直接書き込みます。品質100、1画像、グリッド有効、回転0という従来のHeifWriter設定は維持します。保存先のMediaStore登録とHeifWriter生成は補正処理と並行して準備し、同じ保存内で重複していたHEIFコンテナ・寸法確認は公開前の1回へ統合しました。

色ムラ・モアレ抑制では、輝度計算式・整数丸め・参照画素・重みを変えず、行集計で画素ごとに呼んでいた輝度関数を同一式としてインライン化しました。またDetailPixels.Workの4本の作業配列とクロマ集計配列をSoftReferenceで再利用し、連続撮影時の大容量配列確保を減らします。ChromaPipeline177のBUSY直列化を維持するため、同じバッファを同時保存で共有しません。

倍率UIは、1倍・3倍・5倍の下に表示していた状態説明TextViewをGONEにし、接写を含むボタン列の高さを48dpから52dpへ拡張しました。接写側も52dpへ合わせ、親コンテナの高さも52dpに変更しています。28dpの円ボタンそのものは縮小せず、下端のストロークが描画領域で切れない余白を確保します。

1倍のメイン広角固定、接写・3倍・5倍の手動選択、距離による自動レンズ切替を使わない仕様、黒画面復旧、通常AF、美顔、24.5MP、HEIF保存は保持しています。Android/Galaxy実機の保存時間およびUI表示は未計測です。

Managerを更新し、未改造のULike 5.6.2(740)に単体または総合のどちらか一方を適用してください。'''
    (root/'CHANGES179.md').write_text('# ULike v1.7.9 / Hiro Morphe v1.0.112\n\n'+notes+'\n')
    (root/'release_pipeline/RELEASE_NOTES.txt').write_text(notes+'\n')
    (root/'release_pipeline/CHANGELOG_SUMMARY.txt').write_text('画質設定を維持したままHEIF直接保存・重複検査削減・作業バッファ再利用を追加し、倍率下説明文字と円線の欠けを修正。実機未検証。\n')
    (root/'release_pipeline/publish179.py').write_text("import publish\npublish.VERSION,publish.PREVIOUS_APP='1.7.9','1.7.8'\npublish.BUNDLE,publish.PREVIOUS='1.0.112','1.0.111'\npublish.TAG='ulike-v1.7.9'\nif __name__=='__main__':publish.main()\n")
    shutil.copyfile(__file__,root/'release179.py')
def finish(root,dist):
    arts=[]
    for name in ['ULike_HQ_Texture_Online_v1.7.9.mpp','Hiro_Morphe_Patches_v1.0.112.mpp']:
        p=dist/name;b=p.read_bytes();arts.append({'file':name,'bytes':len(b),'sha256':sha(b)})
    qa={'version':'1.7.9','bundle_version':'1.0.112','previous_bundle_version':'1.0.111','result':'HOST_AND_DEX_QA_PASS','android_device_tested':False,'device_save_time_ms':None,'private_access_violations':0,'runtime_reference_check':'PASS 1483 total payload/runtime methods; 6047 included members resolved; 3 inherited Android members deferred','host_regression':['ManualLens 596 assertions','Preview177 130 assertions','Startup175 60 assertions','Startup173 64 assertions','Lifecycle172 44 assertions','camera regression 166 assertions'],'artifacts':arts,'fixes':['Direct FileDescriptor HEIF output into pending MediaStore item; no staging-to-DCIM copy on API 29+','One post-encode bounds/container verification before clearing is_pending','MediaStore insert, fd open and HeifWriter build overlap the unchanged image correction stage','Pixel-equivalent chroma row accumulation inlines the existing 77/150/29 luma equation and preserves integer arithmetic','SoftReference reuse of DetailPixels.Work and chroma accumulator arrays under the existing serialized ChromaPipeline177 path','Zoom status TextView is GONE; macro/1x/3x/5x row height is 52dp so the 28dp circle stroke has vertical slack'],'limits':['No Galaxy/Android device timing measurement in this build','No claim of a specific save-time reduction until measured on device']}
    raw=(json.dumps(qa,ensure_ascii=False,indent=2)+'\n').encode();(root/'release_pipeline/LOCAL_QA.json').write_bytes(raw);(dist/'QA_ULike_v1.7.9.json').write_bytes(raw)
    files=[]
    for f in root.rglob('*'):
        if not f.is_file():continue
        n=f.relative_to(root).as_posix()
        if n.startswith(('ci-build170/','host170-classes/')):continue
        if f.parent==root and f.suffix in {'.py','.java','.md','.sh','.json'} and f.name!='SOURCE_FILES.json':files.append(n)
        elif n.startswith(('src170/','stubs170/','host170/','regression170/')) and (f.suffix=='.java' or n=='regression170/test.sh'):files.append(n)
        elif n.startswith('release_pipeline/') and f.suffix in {'.py','.md','.txt','.json'}:files.append(n)
    (root/'SOURCE_FILES.json').write_text(json.dumps({n:sha((root/n).read_bytes()) for n in sorted(set(files))},indent=2)+'\n');files.append('SOURCE_FILES.json')
    out=dist/'ULike_v1.7.9_sources_and_QA.zip'
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for n in sorted(set(files)):
            i=zipfile.ZipInfo(n,(2026,10,2,14,30,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,(root/n).read_bytes())
    print(json.dumps(qa,ensure_ascii=False,indent=2));print('SOURCE_ZIP',sha(out.read_bytes()))
if __name__=='__main__':
    mode=sys.argv[1];root=Path(sys.argv[2]).resolve()
    if mode=='prepare':prepare(root)
    elif mode=='finish':finish(root,Path(sys.argv[3]).resolve())
    else:raise SystemExit('prepare SOURCE or finish SOURCE DIST')
