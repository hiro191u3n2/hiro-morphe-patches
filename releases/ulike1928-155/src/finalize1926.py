#!/usr/bin/env python3
"""Bind a deterministic rebuild to completed original-APK desktop verification."""
from pathlib import Path
import argparse,json,re
from build1926 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.26.mpp': 'f5aeb66dead588cbe336b08fd7911fd9b4d0eca4a08521fee4078e43350ffb42', 'Hiro_Morphe_Patches_v1.0.153.mpp': '8146387a9ce3c9f760a165b08f75f437b25b24482d5a74a6adacaa509ddb85aa'}
NOTES='ULike v1.9.26 / Hiro Morphe Patches 総合版 v1.0.153\n\n【背面カメラ選択後の再起動・黒画面への対策】\nカメラの初期化メソッド先頭に置かれていた追加の監視開始を、ネイティブ側のContext・カメラ設定・内部設定の格納完了後へ移しました。未初期化の設定を使って監視や復旧が走る可能性を避けます。\n固定した背面の物理カメラを使う場合は、表示面が未接続のままセッションを先行作成するネイティブの遅延出力モードを使わず、実際の表示面が準備された後に本来のセッション作成処理へ進めます。対象は現行世代の有効な背面物理ルート、かつネイティブモード準備が成功した場合だけです。前面、古い世代、失敗したルート、背景状態、準備失敗には適用しません。\nこの変更は競合を避けるための対策であり、「Androidが物理カメラと遅延出力の併用を禁止している」という意味ではありません。ユーザー端末の黒画面がこの経路だけで発生していることは、実機ログがないため未確定です。\n復旧処理の開始条件を、映像に重なる表示用ビュー自身の表示状態ではなく、現在の撮影ウィンドウの接続・実寸・表示・フォーカスと現在のカメラ所有状態で判定します。倍率ボタンを表示する前でも、条件が揃えば既存の有限回数の復旧処理に進めます。通常の倍率ボタン操作に使う準備判定、録画・保存中の保護、世代チェックと再試行上限は変更していません。\n\n【維持するもの】\n背面／前面の選択や設定値は変更しません。背面のまま再起動した場合に前面へ強制切替する対策ではありません。固定レンズ選択、保存解像度・形式、ノイズ低減、美顔処理、顔特徴点の高解像度検出、黒帯ダブルタップの前後切替、設定・各パネルの戻る操作、終了待ちの取消し、これまでの表示範囲修正を保持します。削除済みの診断画面や撤回済みの10bit経路は戻しません。\n総合版は現行v1.0.152を土台にし、ULike以外の同梱ファイルと224個のローダークラスを保持します。今回の変更対象は元アプリ側1メソッドと追加コード側4メソッド、および新規ヘルパークラス1個です。それ以外の既存追加メソッド1,377件は正規化DEXハッシュで一致します。\n\n【検証範囲】\n1,004件のホスト側アサーションを通過しました。今回追加した132件は、実際に組み込むヘルパーを模擬Android上で実行し、前後・物理／非物理・モード・準備成功／失敗・古い世代・背景・表示寸法・ウィンドウ状態などの条件を検査したものです。残る872件は従来の戻る操作・終了保護・画面配置・ノイズ処理・黒帯ジェスチャーの回帰検査です。\n単体・総合版の双方を未改造ULike 5.6.2（740）へ適用してAPKを再構築し、各1,567件のメソッド照合と1,214件＋182件のレジスタ解析を通過しました。生成DEX上でも、初期化完了後への監視開始移動と4か所の接続を検査しています。\n公開時はGitHub側で再ビルド・ホスト再検査し、元APKに適用して検証したMPPとSHA-256が一致することを公開条件にします。GitHub側でAndroid端末の試験を行ったものではありません。\nGalaxy実機での黒画面解消、起動時間、レンズ切替、撮影・保存は未確認です。表示面の準備を待つ方式へ変更した影響で、起動時間が変わる可能性があります。元APKやユーザーの写真を公開物へ含めません。\n\n【適用】\n対象は未改造ULike 5.6.2（740）、arm64-v8aです。Managerでソースを更新後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけでは、インストール済みULikeの動作は変わりません。\n'

def finalize(dist,evidence,build):
 qa=json.loads((dist/'QA_ULike_v1.9.26.json').read_text())
 validation=json.loads((evidence/'validation.json').read_text())
 pins=json.loads((evidence/'validated-artifacts.json').read_text())
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==pins[name]['sha256'],'Rebuild differs from validated artifact '+name)
 require(qa['host_tests']=={'total':1004,'back_and_session':71,'exit':79,'layout':134,'shadow':223,'pipeline_contract':299,'black_tap':66,'rear_restart':132,'android_device_test':False},'Host test scope mismatch')
 for name,needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=134'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=223'),('host-pipeline1924.txt','HOST_PIPELINE_ASSERTIONS=299'),('host-gesture1925.txt','HOST_GESTURE_ASSERTIONS=66'),('host-restart1926.txt','checks=132')]:require(needle in (build/name).read_text(),'New host tests missing '+name)
 require(set(validation['desktop_validation'])=={'single','bundle'},'Both APK applications required')
 for kind,row in validation['desktop_validation'].items():
  result=row['patch_result'];require(result['packageName']=='com.gorgeous.liteinternational' and result['packageVersion']=='5.6.2' and not result['failedPatches'] and all(x['success'] for x in result['patchingSteps']),'APK application failure')
  require('1567 runtime/payload contracts exactly retained' in row['method_contracts'],'Final APK contract failure')
  require(row['register_analysis']=={'all-registers':{'tested':1214,'failed':0},'stock-registers':{'tested':182,'failed':0}},'Register analysis failure')
 require('2 ratio returns and 1 onLayout return are label-safe' in validation['layout_contracts'] and '104 native ABI references' in validation['layout_contracts'],'Missing real DEX control-flow and ABI checks')
 require('4 original gesture contracts' in validation['layout_contracts'] and '22 native ABI references' in validation['layout_contracts'],'Missing gesture DEX/ABI gates')
 require('1 init ordering + 4 runtime hooks' in validation['restart_contracts'],'Missing native initialization and recovery wiring checks')
 require(not validation['android_device_test'],'Unexpected device scope')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=validation['desktop_validation'],layout_contracts=validation['layout_contracts'],rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,other_app_loader_classes_unchanged=224,startup_ordering_changed=True,rear_restart_device_cause_confirmed=False,screenshot_root_cause_device_confirmed=False,restart_contracts=validation['restart_contracts'],processed_domain_finishing=True,raw_luminance_not_reintroduced=True,shadow_chroma_smoothing=True,all_tone_denoising=True,black_tap_native_switch=True,black_tap_device_tested=False)
 sources={p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()}
 require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev),'Unexpected evidence artifact')
 qa['source_sha256']={n:sha(b) for n,b in sources.items()};qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.26.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in sources.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for name in ('QA_ULike_v1.9.26.json','RELEASE_NOTES.txt','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','host-pipeline1924.txt','host-gesture1925.txt','host-restart1926.txt','restart-contracts.txt','emitted-audit.tsv','references.log'):items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.26_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.26.json','RELEASE_NOTES.txt','ULike_v1.9.26_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;1004 host assertions;2 original APK applications;1567 method contracts per APK;1214+182 registers;label-safe returns;no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('dist','evidence','build'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
