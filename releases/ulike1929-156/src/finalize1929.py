#!/usr/bin/env python3
"""Finalize the exact original-APK-tested MPP; never infer device success from host tests."""
from pathlib import Path
import argparse,json,re
from build1929 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={
 'ULike_HQ_Texture_Online_v1.9.29.mpp':'2faae3114d5663cfb924f544b7c114f910caadc5b517eb575ea38b1d89311f6c',
 'Hiro_Morphe_Patches_v1.0.156.mpp':'40c885d205d0a087192be599263a13858ff5c48fec935835791bfc93a364de88'}
NOTES='''ULike v1.9.29 / Hiro Morphe Patches 総合版 v1.0.156

【背面カメラ再起動の黒画面：起動・終了・出力・復旧を横断修正】
前版への待ち時間の追加ではなく、元アプリと追加パッチをつないだ実際の起動・終了経路を調べ、以下のコード上の不整合を修正しました。ただし、実機ログがないため、端末での黒画面の原因がこれらに限られることや、今回すべて解消したことを確認済みとは扱いません。

1. 終了時の遅延処理は、処理の実行時にグローバルなカメラ管理先を読み直していました。終了処理を作成した時点の管理先・出力先を記録し、その出力だけを解放します。再起動後の新しい出力先や、次の世代が引き継いだSurface／SurfaceTextureは解放しません。所有情報を取得できなかった場合も、新しい出力先を代わりに解放しません。
2. 管理オブジェクトがあるだけで、すでに出力先が削除・解放されている場合も再利用していた判定を修正しました。出力先の存在・カメラ所有者・実際のSurfaceの有効性を確認します。有効な出力を単に一時停止している場合は、そのまま再利用できます。
3. 出力設定のコピーと比較に欠けていたSurface、ピクセル形式、主出力フラグを含めました。サイズは値をコピーし、同じサイズオブジェクトの中身が変更されても変更を見落としません。
4. バックグラウンド移行ですでに更新したカメラ世代を、復帰時に再度更新していました。この二重更新をやめ、復帰通知より先に準備した新しいカメラを無効化しないようにしました。復旧試行回数などの一時状態は復帰時にリセットします。
5. 背面のプレビュー開始前に、寸法だけでなく実際のSurface／SurfaceTextureも確認します。解放済みのテクスチャには元アプリの再生成・描画側通知を利用します。未準備の場合は既存の準備待ちへ返し、録画・保存中に再生成しません。任意のGL出力先を独自生成する方式ではありません。
6. 出力構成失敗の通知は、まず元アプリのYUV／通常センサーモード等への既存の復帰処理へ渡します。その前にレンズを使用不可にして復帰を妨げないようにしました。古いセッションだけを閉じ、復帰先の新しい世代・セッションを保護します。さらに最初の映像送信が失敗した場合に「プレビュー開始成功」と通知していた処理を修正しました。
7. テクスチャ式プレビューの既存の復旧監視は、カメラの状態通知だけでなく、現在の出力先から画像フレームが実際にリスナーへ届いていることも確認します。古い・停止済み出力やダミーリスナーを正常な映像と判断しません。画像通知を持たないSurfaceのみの出力や非テクスチャ方式は従来の監視を維持し、不必要な再起動を防ぎます。

【維持するもの・制限】
土台はULike v1.9.28／総合版v1.0.155です。元アプリへの既存188メソッドの変更は保持し、新規9メソッドを接続しました。追加パッチの既存コードで変更したのは起動・復旧関連4メソッドだけです。既存1,406メソッドと他アプリ用224ローダークラスを保持しています。
ノイズ低減、美顔、顔特徴点の高解像度検出、保存画像の処理・形式・解像度の選択方針、前回使ったインカメラ／アウトカメラの記憶、倍率・接写、黒帯ダブルタップ切替、設定・各パネルからの戻る操作、保存・録画中の終了保護は保持します。背面選択をインカメラへ強制変更しません。削除済みの診断項目や撤回済み10bit経路を戻しません。
新しい無限リトライや待ち時間の延長は追加していません。保存・録画中の保護と既存の試行上限を維持します。従来の構成復帰が作動した場合は、対応する通常センサーモード・出力構成を使うため、最大解像度を保証するという意味ではありません。

【検証範囲】
1,532件のPC上のアサーションを通過しました。新規172件では、今回実際に組み込むJavaヘルパーを模擬Android／カメラ環境で実行し、古い解放処理の遅延、新旧出力の共有、欠落した出力設定、再接続、解放済みテクスチャ、保存・録画中の保護、構成復帰の通知順、古い通知、実画像とメタデータの区別を検査しました。
従来の283件の開始待ち試験は旧コンポーネントの回帰検査です。今回置き換えた準備判定は新規172件で検査し、実際のDEXでの接続と分岐も別途検査しています。旧ソースの試験を新実装の実機試験として扱いません。
単体・総合版を未改造ULike 5.6.2（740）へ適用してAPKを再構築し、組み込まれたメソッドの完全一致とレジスタ解析を実施しています。新規ヘルパーの58件の直接ネイティブ参照と30件のリフレクション参照を検査しました。試験の件数・結果・対象は同梱QAに記録しています。
公開時はGitHub側で同じMPPを再ビルド・ホスト再検査し、元APKへ適用して検証したMPPとのSHA-256一致を公開条件とします。GitHubで元APKの適用やAndroid実機試験を再実行するという意味ではありません。
Galaxy実機での黒画面解消、起動時間、連続したアプリ再起動、前後切替、撮影・保存は未確認です。画像フレームがリスナーへ届くことは、画面上の表示内容が正しいことまで保証しません。元APK・APKS、ユーザー写真、個人の端末ログは公開しません。

【適用】
未改造ULike 5.6.2（740）、arm64-v8aが対象です。Managerでソースを更新後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけでは、インストール済みULikeの動作は変わりません。アプリデータの削除は不要です。
'''
LOGS=['host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','host-pipeline1924.txt','host-gesture1925.txt','host-restart1926.txt','host-start1927.txt','host-facing1928.txt','host-audit1929.txt','restart-contracts.txt','start1927-contracts.txt','audit1929-contracts.txt','emitted-audit.tsv','references.log']
def finalize(dist,evidence,build):
 qa=json.loads((dist/'QA_ULike_v1.9.29.json').read_text());v=json.loads((evidence/'validation.json').read_text());pins=json.loads((evidence/'validated-artifacts.json').read_text())
 for n,s in EXPECTED.items():
  require(sha((dist/n).read_bytes())==s==pins[n]['sha256'],'MPP differs from original-APK validated artifact '+n)
  require((dist/n).stat().st_size==pins[n]['bytes'],'Artifact length mismatch')
 require(qa['host_tests']['total']==1532 and qa['host_tests']['shipped_1929_lifecycle_helpers']==172,'Host scope mismatch')
 for name,needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=134'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=223'),('host-pipeline1924.txt','HOST_PIPELINE_ASSERTIONS=299'),('host-gesture1925.txt','HOST_GESTURE_ASSERTIONS=66'),('host-restart1926.txt','checks=132'),('host-start1927.txt','checks=283'),('host-facing1928.txt','PASS 73 facing persistence assertions'),('host-audit1929.txt','PASS1929 shipped helper assertions=172')]:
  require(needle in (build/name).read_text(),'Missing host evidence '+name)
 require(set(v['desktop_validation'])=={'single','bundle'},'Both APK tests required')
 for row in v['desktop_validation'].values():
  pr=row['patch_result'];require(pr['packageName']=='com.gorgeous.liteinternational' and pr['packageVersion']=='5.6.2' and not pr['failedPatches'] and all(x['success'] for x in pr['patchingSteps']),'APK application failure')
  require('1635 runtime/payload contracts exactly retained' in row['method_contracts'],'APK contract count mismatch')
  require(row['register_analysis']=={'all-registers':{'tested':1267,'failed':0},'stock-registers':{'tested':197,'failed':0}},'Register analysis mismatch')
 require('direct native references=58; reflected members=30' in v['audit_contracts'],'Native ABI audit missing')
 require(not v['android_device_test'] and not v['black_preview_cause_confirmed'],'Unexpected device claim')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=v['desktop_validation'],layout_contracts=v['layout_contracts'],restart_contracts=v['restart_contracts'],start_contracts=v['start_contracts'],audit_contracts=v['audit_contracts'],rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,other_app_loader_classes_unchanged=224,existing_runtime_methods_unchanged=1406,existing_runtime_methods_changed=4,existing_native_methods_unchanged=188,new_native_hooks=9,new_helper_classes=6,black_preview_fixed_on_device=False)
 sources={p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()};require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev),'Unexpected evidence artifact')
 qa['source_sha256']={n:sha(b) for n,b in sources.items()};qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.29.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in sources.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for n in ['QA_ULike_v1.9.29.json','RELEASE_NOTES.txt',*LOGS]:items[n]=(dist/n).read_bytes()
 write_zip(dist/'ULike_v1.9.29_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.29.json','RELEASE_NOTES.txt','ULike_v1.9.29_sources_and_QA.zip'];(dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;1532 host assertions;2 original APK applications;1635 method contracts per APK;1267+197 register analyses;58 direct+30 reflected native members;no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ('dist','evidence','build'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
