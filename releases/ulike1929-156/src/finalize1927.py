#!/usr/bin/env python3
"""Bind deterministic MPPs to completed original-APK desktop validation, not device tests."""
from pathlib import Path
import argparse, json
from build1927 import sha, require, write_zip
ROOT = Path(__file__).resolve().parent
EXPECTED = {
 'ULike_HQ_Texture_Online_v1.9.27.mpp': '1f15889594f90d27ca6a097e62c6b394a372cf5354c07ae2949d9793098def22',
 'Hiro_Morphe_Patches_v1.0.154.mpp': 'd7e2d59fe0f3e6edb37fb00498e3d2c0b10456f61d6b97ef5b453a53deea3ede'
}
NOTES = '''ULike v1.9.27 / Hiro Morphe Patches 総合版 v1.0.154

【背面カメラで再起動すると黒画面になる問題への追加修正】
元アプリのコードで、カメラを開いた際のプレビュー開始が入力準備不足で失敗しても、その戻り値が処理されず、後から表示用データが設定されても開始要求を再実行しない経路を確認しました。前版の初期化順序・表示面の対策とは別の、プレビュー開始要求の取りこぼしを修正します。
元のプレビュー開始呼び出しの戻り値と例外は保持し、準備不足に限って開始要求を一時保持します。表示用データの設定完了通知と有限の準備確認を組み合わせ、有効なプレビュー入力が用意された後に、元アプリの開始処理を再実行します。新しい表示面を勝手に生成したり、背面の選択を前面へ変更したりする対策ではありません。
同じカメラ実体・背面ルート・世代で、前景の撮影ウィンドウが有効であることを再確認します。既にセッションや映像が始まっている場合、前後やレンズが切り替わった場合、古いカメラ、保存・撮影・録画中には追加開始を行いません。待機は最大5秒、追加の開始呼び出しは最大2回で、無限再試行や強制終了は行いません。保存・録画中は期限内で準備を待つだけです。
これは元コードに存在する取りこぼしの修正です。Galaxy実機のログは取得できていないため、ユーザー端末の黒画面がこの経路だけで発生していることや、今回の修正で解消したことは未確認です。

【維持するもの】
土台はULike v1.9.26／総合版v1.0.153です。既存の追加コード1,385メソッドと、元アプリへの既存182メソッドの変更をそのまま保持し、新規ヘルパー2クラスと元アプリの3か所の接続だけを追加しました。
保存解像度・保存形式、全体と暗部のノイズ低減、美顔・質感処理、顔特徴点の高解像度検出、前後選択・倍率・接写、黒帯ダブルタップ切替、設定・微調整・スタイル・美顔・フィルタからの戻る操作、終了待ちの取消し、これまでの表示範囲修正は保持しています。既存の画像処理経路は変更していません。削除済みの診断画面や撤回済みの10bit経路は戻しません。
総合版の他アプリ用リソースと224個のローダークラスも保持します。既存設定の初期化やアプリデータの削除を前提にした対策ではありません。

【検証範囲】
1,287件のホスト側アサーションを通過しました。今回追加した283件では、実際に組み込むヘルパーを模擬Android・カメラ環境で実行し、入力の遅延、通知の取りこぼし、世代交代、二重開始防止、保存・録画中の保護、準備失敗、タイムアウト、戻り値・例外の保持を検査しています。実際のAndroidカメラを使った試験ではありません。
単体・総合版の双方を未改造ULike 5.6.2（740）へ適用してAPKを再構築し、各1,591件のメソッド照合、各1,235件＋185件のレジスタ解析を通過しました。追加した3か所を逆変換すると元APKのメソッドと一致すること、追加コードが参照する17件のSDK・内部フィールドの署名が元APKに実在することも確認しています。
GitHub公開時には再ビルドとホスト再検査を行い、元APKへ適用して検証したMPPとのSHA-256一致を公開条件にします。GitHub側が元APKや実機を使って再検証したという意味ではありません。Galaxy実機での黒画面解消、起動時間、レンズ切替、撮影・保存の成功は未確認です。
元APK・APKSやユーザーの写真は公開物に含めません。

【適用】
対象は未改造ULike 5.6.2（740）、arm64-v8aです。Managerでソースを更新後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけでは、インストール済みULikeの動作は変わりません。
'''
HOSTS = {
 'total':1287, 'back_and_session':71, 'exit':79, 'layout':134, 'shadow':223,
 'pipeline_contract':299, 'black_tap':66, 'rear_restart':132,
 'lost_preview_start':283, 'android_device_test':False
}
LOGS = ['host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt',
        'host-pipeline1924.txt','host-gesture1925.txt','host-restart1926.txt','host-start1927.txt',
        'restart-contracts.txt','start1927-contracts.txt','emitted-audit.tsv','references.log']

def finalize(dist, evidence, build):
 qa = json.loads((dist/'QA_ULike_v1.9.27.json').read_text())
 validation = json.loads((evidence/'validation.json').read_text())
 pins = json.loads((evidence/'validated-artifacts.json').read_text())
 for name, pin in EXPECTED.items():
  require(sha((dist/name).read_bytes()) == pin == pins[name]['sha256'], 'Rebuild differs from validated artifact '+name)
  require((dist/name).stat().st_size == pins[name]['bytes'], 'Artifact length differs '+name)
 require(qa['host_tests'] == HOSTS, 'Host test scope mismatch')
 for name, needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),
   ('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=134'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=223'),
   ('host-pipeline1924.txt','HOST_PIPELINE_ASSERTIONS=299'),('host-gesture1925.txt','HOST_GESTURE_ASSERTIONS=66'),
   ('host-restart1926.txt','checks=132'),('host-start1927.txt','checks=283')]:
  require(needle in (build/name).read_text(), 'New host test evidence missing '+name)
 require(set(validation['desktop_validation']) == {'single','bundle'}, 'Both APK applications required')
 for row in validation['desktop_validation'].values():
  r = row['patch_result']
  require(r['packageName']=='com.gorgeous.liteinternational' and r['packageVersion']=='5.6.2'
          and not r['failedPatches'] and all(x['success'] for x in r['patchingSteps']), 'APK application failure')
  require('1591 runtime/payload contracts exactly retained' in row['method_contracts'], 'APK method mismatch')
  require(row['register_analysis'] == {'all-registers':{'tested':1235,'failed':0},
                                      'stock-registers':{'tested':185,'failed':0}}, 'Register analysis failure')
 require('2 ratio returns and 1 onLayout return are label-safe' in validation['layout_contracts']
         and '104 native ABI references' in validation['layout_contracts'], 'Layout gates missing')
 require('4 original gesture contracts' in validation['layout_contracts']
         and '22 native ABI references' in validation['layout_contracts'], 'Gesture gates missing')
 require('1 init ordering + 4 runtime hooks' in validation['restart_contracts'], 'Previous startup gates missing')
 require('native hooks=3; inverse transform matches unmodified native methods; start body retained; SDK/reflection ABI references=17'
         in validation['start_contracts'], 'Original-APK native startup/ABI gates missing')
 require(not validation['android_device_test'] and not validation['black_preview_cause_confirmed'], 'Unexpected device scope')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,
           desktop_validation=validation['desktop_validation'],layout_contracts=validation['layout_contracts'],
           restart_contracts=validation['restart_contracts'],start_contracts=validation['start_contracts'],
           rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,
           other_app_loader_classes_unchanged=224,existing_runtime_methods_unchanged=1385,
           existing_native_methods_unchanged=182,new_native_hooks=3,new_helper_classes=2,
           rear_restart_device_cause_confirmed=False,screenshot_root_cause_device_confirmed=False)
 sources = {p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*'))
            if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev = {p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()}
 require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev), 'Unexpected evidence artifact')
 qa['source_sha256'] = {n:sha(b) for n,b in sources.items()}
 qa['desktop_evidence_sha256'] = {n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.27.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 (dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items = {'src/'+n:b for n,b in sources.items()}
 items.update({'evidence/'+n:b for n,b in ev.items()})
 for name in ['QA_ULike_v1.9.27.json','RELEASE_NOTES.txt',*LOGS]: items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.27_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.27.json','RELEASE_NOTES.txt','ULike_v1.9.27_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;1287 host assertions;2 original APK applications;1591 method contracts per APK;1235+185 registers;3 inverse-checked native startup hooks;17 ABI references;no device claim')

if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ('dist','evidence','build'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
