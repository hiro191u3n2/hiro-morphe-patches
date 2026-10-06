#!/usr/bin/env python3
"""Bind a deterministic rebuild to completed original-APK desktop verification."""
from pathlib import Path
import argparse,json,re
from build1925 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.25.mpp': 'e9ffd9c53cac07205b435590290dc00df6c8cd0c0f17a6987dc417f4150e2919', 'Hiro_Morphe_Patches_v1.0.152.mpp': '21fbbaa6f885fc8075854f12046269cfeb738d6d47452e6e10687f147bd7ad8f'}
NOTES='ULike v1.9.25 / Hiro Morphe Patches 総合版 v1.0.152\n\n【全体のノイズ低減】\n暗部だけでなく、中間調・明るい部分にも控えめな輝度・色ノイズ低減を適用します。明部・中間調は最大5×5、暗部優先が有効な暗い部分は最大7×7の輪郭保護付き処理です。暗部への追加強度と外周の重みは明るさに応じて滑らかに変化させます。前版より暗部の低減も少し強めました。\n基準は色補正後の画像です。露出・ガンマ・黒レベルの持ち上げや、補正前の明るさの戻しは追加しません。明暗境界と強い色境界を混ぜにくくし、透明な画素を平滑化しません。保存解像度・形式・既存の美顔機能は維持します。\n既存の「ノイズ除去」がオンのとき有効で、その強度設定に連動します。「暗部優先」がオフでも全体の弱い低減は働き、オンでは暗い部分を優先します。ノイズ除去オフはそのままです。強い設定では布・髪・メッシュなど低コントラストの細かな質感が弱まる可能性があります。全体への処理追加により保存時間が増える可能性があり、Galaxy実機での所要時間は未測定です。\n\n【黒い余白のダブルタップ】\n既存コードでは最初のタップの指を離した時点で単押し処理が走り、ダブルタップの処理でも単押し用の処理を呼んでいました。黒帯上のダブルタップを、この表示変更を含む単押し経路から分離しました。黒帯では単押し確定まで待ち、ダブルタップならULike本来の前後カメラ切替だけを呼びます。単押しが確定した場合は従来の単押し処理を1回だけ実行します。\n判定は写真の色ではなく、現在の黒帯と映像範囲の座標を使用します。アニメーション中は描画中の黒帯を参照し、2回のタップ間で範囲が変化しても最初の単押しを後から発火させません。タッチキャンセル・複数指操作では保留中の単押しを破棄します。\n前後カメラの連続切替は800msのガードと既存の切替処理側のガードで抑止します。録画・撮影・高品質保存の処理中、非表示・フォーカス喪失後は切替を行いません。シャッター・倍率・フィルタ等の子ボタンに先に配信されるイベントは奪わず、映像領域のタップ・ピンチ・スワイプとカメラ以外のジェスチャーリスナーは既存経路を維持します。\n今回のコード上の競合は確認しましたが、ユーザー端末の「バグ」がこの経路だけで起きたことや実機で解消したことは未確認です。\n\n【維持するもの】\n顔特徴点の高解像度検出、設定・微調整・スタイル・美顔・フィルタから撮影画面へ戻る動作、保存中の終了保護と終了待ちの取消し、v1.9.24の表示範囲修正を維持します。削除済みの診断項目や撤回済み10bit経路は戻しません。\n総合版は現行v1.0.151を土台にし、Xの絶対日時・24時間表記の修正を含め、ULike以外の同梱ファイルとローダークラスを保持します。\n\n【検証範囲】\n872件のホスト側アサーションを通過しました。内訳は戻る・初期化71、終了保護79、表示134、画像ノイズ223、模擬上流処理を使った画像ワーカー299、模擬Androidを使った黒帯ジェスチャー66です。ノイズ試験は合成画像による平均輝度・分散・輪郭・色境界・分割境界等の検査で、端末による新規撮影ではありません。\n単体・総合版の両方を未改造ULike 5.6.2（740）へ適用してAPKを再構築し、各1,563件のメソッド照合、1,210件＋182件のレジスタ解析を通過しました。4か所の元ジェスチャーの契約、3コールバックの接続、元のタッチ処理の維持、新規コードから22件のネイティブ参照、既存の表示処理から104件のネイティブ参照も検査しました。\n公開時はGitHub側でも再ビルド・ホスト再試験を行い、元APKへ適用して検証したMPPとSHA-256が一致することを条件にします。Galaxy実機での起動・カメラ切替・撮影・保存は未確認です。元APKとユーザーの写真は公開物へ含めません。\n\n【適用】\n対象は未改造ULike 5.6.2（740）、arm64-v8aです。Managerでソース更新後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけではインストール済みアプリの動作は変わりません。\n'

def finalize(dist,evidence,build):
 qa=json.loads((dist/'QA_ULike_v1.9.25.json').read_text())
 validation=json.loads((evidence/'validation.json').read_text())
 pins=json.loads((evidence/'validated-artifacts.json').read_text())
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==pins[name]['sha256'],'Rebuild differs from validated artifact '+name)
 require(qa['host_tests']=={'total':872,'back_and_session':71,'exit':79,'layout':134,'shadow':223,'pipeline_contract':299,'black_tap':66,'android_device_test':False},'Host test scope mismatch')
 for name,needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=134'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=223'),('host-pipeline1924.txt','HOST_PIPELINE_ASSERTIONS=299'),('host-gesture1925.txt','HOST_GESTURE_ASSERTIONS=66')]:require(needle in (build/name).read_text(),'New host tests missing '+name)
 require(set(validation['desktop_validation'])=={'single','bundle'},'Both APK applications required')
 for kind,row in validation['desktop_validation'].items():
  result=row['patch_result'];require(result['packageName']=='com.gorgeous.liteinternational' and result['packageVersion']=='5.6.2' and not result['failedPatches'] and all(x['success'] for x in result['patchingSteps']),'APK application failure')
  require('1563 runtime/payload contracts exactly retained' in row['method_contracts'],'Final APK contract failure')
  require(row['register_analysis']=={'all-registers':{'tested':1210,'failed':0},'stock-registers':{'tested':182,'failed':0}},'Register analysis failure')
 require('2 ratio returns and 1 onLayout return are label-safe' in validation['layout_contracts'] and '104 native ABI references' in validation['layout_contracts'],'Missing real DEX control-flow and ABI checks')
 require('4 original gesture contracts' in validation['layout_contracts'] and '22 native ABI references' in validation['layout_contracts'],'Missing gesture DEX/ABI gates')
 require(not validation['android_device_test'],'Unexpected device scope')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=validation['desktop_validation'],layout_contracts=validation['layout_contracts'],rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,other_app_loader_classes_unchanged=224,code_defects_confirmed=True,screenshot_root_cause_device_confirmed=False,processed_domain_finishing=True,raw_luminance_not_reintroduced=True,shadow_chroma_smoothing=True,all_tone_denoising=True,black_tap_native_switch=True,black_tap_device_tested=False)
 sources={p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()}
 require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev),'Unexpected evidence artifact')
 qa['source_sha256']={n:sha(b) for n,b in sources.items()};qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.25.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in sources.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for name in ('QA_ULike_v1.9.25.json','RELEASE_NOTES.txt','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','host-pipeline1924.txt','host-gesture1925.txt','emitted-audit.tsv','references.log'):items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.25_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.25.json','RELEASE_NOTES.txt','ULike_v1.9.25_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;872 host assertions;2 original APK applications;1563 method contracts per APK;1210+182 registers;label-safe returns;no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('dist','evidence','build'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
