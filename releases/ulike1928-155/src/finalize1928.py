#!/usr/bin/env python3
"""Finalize verified desktop artifacts. No Android/device execution is claimed."""
from pathlib import Path
import argparse,json
from build1928 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={
 'ULike_HQ_Texture_Online_v1.9.28.mpp':'dc3f52b63fe01476ea0a10a5389d8ad2bd3e2f122437cb0b2a2da03fe6917404',
 'Hiro_Morphe_Patches_v1.0.155.mpp':'33613c0177ad8b2909e5cb654d4a6e4d4966eb952eb79a3ddad6f7c05eddf344'}
NOTES='''ULike v1.9.28 / Hiro Morphe Patches 総合版 v1.0.155

【終了時のインカメラ／アウトカメラを次回起動へ引き継ぐ】
通常の撮影画面は、インカメラで終了したら次回もインカメラ、アウトカメラで終了したら次回もアウトカメラで起動するよう変更しました。毎回インカメラに固定する変更ではありません。
元アプリには前回のカメラを保存する last_use_front_camera がありましたが、通常起動は別の初期設定を参照していました。カメラを開く前に前回の保存値を読み込み、カメラ選択に反映します。起動後にカメラを余分に切り替える方式ではありません。有効な保存値がまだない場合は、元アプリの初期設定を保持します。別用途の明示的なカメラ指定は変更しません。
前後切替と終了前に選択を記録し、この小さな設定だけはディスク書き込み完了を待つ方式にしました。初期値と同じインカメラでも記録を作ります。保存失敗時は次の選択・終了時に再試行し、ほかの設定値を削除しません。
終了処理の途中でカメラ状態が取得できなくなった場合、それを「背面」と決めつけて保存しないよう変更しました。終了画面を閉じる直前にも、取得できる現在の選択を保存します。状態がない場合は直前の記録を維持します。

【維持するもの】
土台はULike v1.9.27／総合版v1.0.154です。元アプリへの既存185メソッドの変更と、既存追加コード1,405メソッドはそのまま保持します。既存コードで変更したのは終了直前の保存呼び出しを追加する1メソッドだけで、ほかに新規ヘルパー1クラスと元アプリ側3メソッドの接続を追加しました。
背面起動時のプレビュー再試行・初期化対策、保存解像度・形式、ノイズ低減、美顔処理、顔特徴点の高解像度検出、倍率・接写、黒帯ダブルタップによる前後切替、設定・各パネルからの戻る操作、保存・録画中の終了保護は維持しています。撮影・保存画像の処理は変更していません。削除済みの診断画面と撤回済み10bit経路は戻しません。
総合版の他アプリ用リソースと224個のローダークラスを保持します。アプリデータの削除は不要です。

【検証範囲】
1,360件のホスト側アサーションを通過しました。今回追加した73件は実際に組み込む保存ヘルパーを模擬の設定・カメラ状態で実行し、前後の記憶、保存値なし、状態消失、書き込み失敗と再試行、初期設定と保存値の違い、プロセス再生成を模した読み直しなどを検査しています。Android実機での再起動試験ではありません。
単体・総合版の両方を未改造ULike 5.6.2（740）へ適用してAPKを再構築し、各1,598件のメソッド照合、各1,239件＋188件のレジスタ解析を通過しました。新規接続先7件のネイティブ参照が元APKに存在すること、起動の元の分岐と終了処理の本体が保持されることも確認しました。
公開時にはGitHub側で再ビルド・ホスト再検査を行い、元APKへ適用して検証したMPPと完全一致したものだけを公開します。GitHubで元APKの適用や実機試験を再実行するという意味ではありません。
Galaxy実機でインカメラ選択の引き継ぎ、前後切替、撮影・保存の成功は未確認です。設定の同期保存による操作時間への影響は実機未測定です。以前の黒画面問題の実機解消も、この更新で確認したとは扱いません。
元APK・APKSやユーザーの写真は公開物に含めません。

【適用】
対象は未改造ULike 5.6.2（740）、arm64-v8aです。Managerでソースを更新した後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけでは、インストール済みULikeは変わりません。適用後、希望するカメラへ切り替えて一度終了してください。
'''
HOSTS={'total':1360,'back_and_session':71,'exit':79,'layout':134,'shadow':223,'pipeline_contract':299,'black_tap':66,'rear_restart':132,'lost_preview_start':283,'facing_memory':73,'android_device_test':False}
LOGS=['host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','host-pipeline1924.txt','host-gesture1925.txt','host-restart1926.txt','host-start1927.txt','host-facing1928.txt','restart-contracts.txt','start1927-contracts.txt','facing1928-contracts.txt','emitted-audit.tsv','references.log']
def finalize(dist,evidence,build):
 qa=json.loads((dist/'QA_ULike_v1.9.28.json').read_text());v=json.loads((evidence/'validation.json').read_text());pins=json.loads((evidence/'validated-artifacts.json').read_text())
 for n,s in EXPECTED.items():
  require(sha((dist/n).read_bytes())==s==pins[n]['sha256'],'MPP differs from original-APK validated artifact '+n)
  require((dist/n).stat().st_size==pins[n]['bytes'],'Artifact length mismatch')
 require(qa['host_tests']==HOSTS,'Host assertions scope mismatch')
 for name,needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=134'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=223'),('host-pipeline1924.txt','HOST_PIPELINE_ASSERTIONS=299'),('host-gesture1925.txt','HOST_GESTURE_ASSERTIONS=66'),('host-restart1926.txt','checks=132'),('host-start1927.txt','checks=283'),('host-facing1928.txt','PASS 73 facing persistence assertions')]:
  require(needle in (build/name).read_text(),'Missing host evidence '+name)
 require(set(v['desktop_validation'])=={'single','bundle'},'Both APK tests required')
 for row in v['desktop_validation'].values():
  r=row['patch_result'];require(r['packageName']=='com.gorgeous.liteinternational' and r['packageVersion']=='5.6.2' and not r['failedPatches'] and all(x['success'] for x in r['patchingSteps']),'APK application failure')
  require('1598 runtime/payload contracts exactly retained' in row['method_contracts'],'APK contract failure')
  require(row['register_analysis']=={'all-registers':{'tested':1239,'failed':0},'stock-registers':{'tested':188,'failed':0}},'Register analysis failure')
 require('native ABI references=7; camera startup coordinator unchanged' in v['facing_contracts'],'New native ABI gates missing')
 require('SDK/reflection ABI references=17' in v['start_contracts'],'Previous startup contract missing')
 require('104 native ABI references' in v['layout_contracts'] and '22 native ABI references' in v['layout_contracts'],'Layout/gesture gates missing')
 require('1 init ordering + 4 runtime hooks' in v['restart_contracts'],'Previous restart gates missing')
 require(not v['android_device_test'] and not v['black_preview_cause_confirmed'],'Unexpected device claim')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=v['desktop_validation'],layout_contracts=v['layout_contracts'],restart_contracts=v['restart_contracts'],start_contracts=v['start_contracts'],facing_contracts=v['facing_contracts'],rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,other_app_loader_classes_unchanged=224,existing_runtime_methods_unchanged=1405,existing_runtime_methods_changed=1,existing_native_methods_unchanged=185,new_native_hooks=3,new_helper_classes=1,front_facing_restart_device_tested=False)
 sources={p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()};require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev),'Unexpected evidence artifact')
 qa['source_sha256']={n:sha(b) for n,b in sources.items()};qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.28.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in sources.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for n in ['QA_ULike_v1.9.28.json','RELEASE_NOTES.txt',*LOGS]:items[n]=(dist/n).read_bytes()
 write_zip(dist/'ULike_v1.9.28_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.28.json','RELEASE_NOTES.txt','ULike_v1.9.28_sources_and_QA.zip'];(dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;1360 host assertions;2 original APK applications;1598 method contracts per APK;1239+188 register analyses;3 native bridges+1 pre-close snapshot;7 native ABI references;no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ('dist','evidence','build'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
