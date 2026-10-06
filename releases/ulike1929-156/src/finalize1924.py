#!/usr/bin/env python3
"""Bind a deterministic rebuild to completed original-APK desktop verification."""
from pathlib import Path
import argparse,json,re
from build1924 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.24.mpp': '239973e573882faca608f67138409a282451e9bad1083102f266864068820b75', 'Hiro_Morphe_Patches_v1.0.150.mpp': '7c361efc2bafacbb9b4c0a193dcc854a72cf2a8d441f066e6de40d5490172513'}
NOTES='ULike v1.9.24 / Hiro Morphe Patches 総合版 v1.0.150\n\n【暗部ノイズと白もやへの対策】\n前版は、シャープ・色補正後の画素を、補正前の画像から求めた絶対輝度へ近付けていました。補正済みの黒レベルや明暗差を戻してしまう経路を除去しました。今回の平滑化は補正後の画像だけを基準にし、露出の持ち上げ・ガンマ変更・一律の暗部明るさ補正は追加しません。\n暗い部分に最大7×7の輪郭保護付き平滑化を適用し、輝度ノイズと色の粒状感を抑えます。強い明暗境界・色境界は混ぜにくくし、明るい画素は今回の追加処理では変更しません。保存解像度・保存形式・撮影ビット深度は従来どおりです。\n主保存経路では既存の作業用メモリーを再利用し、補正後の周辺画素も計算してから分割処理します。未計算の余白や処理中の出力を参照せず、ワーカー間で画像を共有して上書きしません。代替経路では計算済み範囲だけを使い、内部の分割境界へ滑らかに効果を弱めます。\n既存の「ノイズ除去」と「暗部優先」がオンのとき有効で、強度はノイズ設定に連動します。オフの設定は変更しません。強い低減では暗い布・メッシュなど細かな低コントラストの質感が弱まる場合があります。追加した周辺画素の計算とフィルターによる保存時間の変化は実機未測定です。\n\n【写す範囲が時々ずれる問題】\n前版は描画中の黒帯を保存済みの配置へ合わせるだけで、その保存済み配置が現在の表示寸法と3:4比率に合っているかを検証していませんでした。今回は実際のView寸法とULike本来の上余白に照合し、3:4の表示高さと上下黒帯を再計算します。比率に合わない古いアニメーションは取り消してから補正します。\n正方形・丸型表示から戻った際に残り得る左右の余白を解除し、カメラの初回フレームや画面再接続では同じ座標でも表示範囲を再通知します。描画側には更新済みの座標スナップショットを渡し、別スレッドから変化中のView値を直接読まないようにしました。保存写真を強制的に切り抜く修正ではありません。正方形・丸型・9:16などは既存の比率選択を維持します。\n\n【維持する機能】\n顔特徴点の高解像度検出、設定・微調整・スタイル・美顔・フィルタを閉じて撮影画面へ戻る操作、保存中の終了保護と終了待ちの取消しを保持します。削除済み診断項目と撤回済み10bit経路は戻しません。総合版1.0.149の他アプリ用パッチと素材は維持します。\n\n【検証範囲】\n690件のホスト側アサーション（戻る・初期化71、終了待ち79、表示134、暗部107、模擬上流処理を使ったワーカーの範囲・受渡し・失敗保護299）を実行します。これらはAndroid実機試験ではありません。\n単体・総合MPPの両方を未改造ULikeへ適用してAPKを再構築し、それぞれ1,546件のメソッド照合、1,197件と178件のレジスタ解析を通過しています。元アプリの104件の参照、古い復旧処理を飛ばさない分岐、画面再接続時の新しい呼び出しも検査しました。公開時はGitHub側で再ビルド・再試験し、元APKへ適用して検証したMPPとSHA-256が完全一致することを条件にします。\nGalaxy実機での起動・復帰・レンズ切替・撮影・保存は未検証です。コード上の欠落は確認しましたが、添付写真の白もやや表示異常がこの経路だけで発生したこと、実機で再発しないことは未確認です。写真全体の白もやが光学的な曇り等による場合まで除去する処理ではありません。元APKや添付写真は公開物へ含めません。\n\n【適用】\n対象：未改造ULike 5.6.2（740）、arm64-v8a。Managerでソースを更新後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけではインストール済みアプリの動作は変わりません。\n'

def finalize(dist,evidence,build):
 qa=json.loads((dist/'QA_ULike_v1.9.24.json').read_text())
 validation=json.loads((evidence/'validation.json').read_text())
 pins=json.loads((evidence/'validated-artifacts.json').read_text())
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==pins[name]['sha256'],'Rebuild differs from validated artifact '+name)
 require(qa['host_tests']=={'total':690,'back_and_session':71,'exit':79,'layout':134,'shadow':107,'pipeline_contract':299,'android_device_test':False},'Host test scope mismatch')
 for name,needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=134'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=107'),('host-pipeline1924.txt','HOST_PIPELINE_ASSERTIONS=299')]:require(needle in (build/name).read_text(),'New host tests missing '+name)
 require(set(validation['desktop_validation'])=={'single','bundle'},'Both APK applications required')
 for kind,row in validation['desktop_validation'].items():
  result=row['patch_result'];require(result['packageName']=='com.gorgeous.liteinternational' and result['packageVersion']=='5.6.2' and not result['failedPatches'] and all(x['success'] for x in result['patchingSteps']),'APK application failure')
  require('1546 runtime/payload contracts exactly retained' in row['method_contracts'],'Final APK contract failure')
  require(row['register_analysis']=={'all-registers':{'tested':1197,'failed':0},'stock-registers':{'tested':178,'failed':0}},'Register analysis failure')
 require('2 ratio returns and 1 onLayout return are label-safe' in validation['layout_contracts'] and '104 native ABI references' in validation['layout_contracts'],'Missing real DEX control-flow and ABI checks')
 require(not validation['android_device_test'],'Unexpected device scope')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=validation['desktop_validation'],layout_contracts=validation['layout_contracts'],rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,other_app_loader_classes_unchanged=221,code_defects_confirmed=True,screenshot_root_cause_device_confirmed=False,processed_domain_finishing=True,raw_luminance_not_reintroduced=True,shadow_chroma_smoothing=True)
 sources={p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()}
 require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev),'Unexpected evidence artifact')
 qa['source_sha256']={n:sha(b) for n,b in sources.items()};qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.24.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in sources.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for name in ('QA_ULike_v1.9.24.json','RELEASE_NOTES.txt','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','host-pipeline1924.txt','emitted-audit.tsv','references.log'):items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.24_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.24.json','RELEASE_NOTES.txt','ULike_v1.9.24_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;690 host assertions;2 original APK applications;1546 method contracts per APK;1197+178 registers;label-safe returns;no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('dist','evidence','build'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
