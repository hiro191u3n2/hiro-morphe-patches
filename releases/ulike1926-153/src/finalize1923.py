#!/usr/bin/env python3
"""Bind a deterministic rebuild to completed original-APK desktop verification."""
from pathlib import Path
import argparse,json,re
from build1923 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.23.mpp': '3aaba8d00986ee55af332e08b17000a254910598e2b1c666039d571d5c366de8', 'Hiro_Morphe_Patches_v1.0.149.mpp': '04d34885d68b556f66175455d64e96d683339c0bc4fc29c970d10a42a900a5f4'}
NOTES='ULike v1.9.23 / Hiro Morphe Patches 総合版 v1.0.149\n\n【プレビュー表示範囲】\n前版の復旧呼び出しはRETURN命令の直前に挿入されていましたが、一部のジャンプ先が元のRETURNを指したままで、その呼び出しを飛ばしていました。今回、ジャンプ先の命令自体を復旧呼び出しへ置き換え、2本の終了経路のどちらでも復旧が働くようにします。onLayout側も同様に実際の分岐先を検査しています。\nカメラの黒帯計算では画面全体の寸法とビューの寸法が混在していました。26か所の寸法参照を現在のビューに統一し、描画へ渡すRectFも同じ座標系から生成します。破棄されたビューからの古い範囲を使わず、配置が落ち着いた際に表示領域を一度だけ通知します。固定ピクセルへの移動や、写真の強制クロップではありません。既存の比率選択を保持します。\n\n【保存写真の暗部ノイズ】\nJava／ネイティブの色ノイズ処理の後に、輝度に応じた5×5の輪郭保護付き暗部平滑化を追加します。暗い平坦部の粒状感を抑え、明るい部分はこの追加処理では変更しません。既存の色補正を戻さないよう輝度だけを調整し、カラー成分は処理済み出力を維持します。暗部にシャープ処理で再強調された粒状感も抑える構成です。\n既存の「ノイズ除去」がオン、かつ暗部優先がオンのときに働きます。強度は既存のノイズ設定に連動し、オフ設定は勝手に変更しません。現在の並列処理・分割処理・保存形式・解像度を維持し、フルサイズ画像の追加コピーを作りません。強い平滑化では、暗い場所の細かな低コントラストの質感が弱まる場合があります。\n\n【維持した機能】\n顔特徴点の高解像度検出、微調整・スタイル・美顔・フィルタ・設定から撮影画面へ戻る操作、保存中の終了保護と取消しを維持します。削除済み診断項目や撤回済み10bit経路は復元しません。総合版1.0.148に含まれる明るさタッチなど、他アプリのパッチは変更しません。\n\n【検証範囲】\n285件のホスト試験（戻る・初期化71、終了待ち79、レイアウト96、暗部処理39）、元ULikeへの単体・総合MPPの適用とAPK再構築を実施しました。各1,541件のメソッド照合、1,192件と178件のレジスタ解析を通過しています。実際のDEXで復旧呼び出しを飛ばす分岐がないこと、72件のビュー側参照が元アプリに存在しアクセス可能なことも検査しました。GitHubで再ビルドしたMPPがローカル検証済みMPPと完全一致することを公開条件とします。\nGalaxy/Android実機での起動・切り替え・撮影・保存は未検証です。コード上の不具合は確認しましたが、添付画像と同じ発生経路だったこと、実機で再発しなくなったことは未確認です。PC上の暗部処理試験と写真の比較は実機撮影試験ではありません。添付写真や元アプリのAPKは公開物に含めません。\n\n【適用】\n対象は未改造ULike 5.6.2（740）、arm64-v8aです。Morphe Managerでソース更新後、総合版または単体版のどちらか一方で再パッチ・再インストールしてください。ソース更新だけでは、インストール済みULikeの動作は変わりません。\n'

def finalize(dist,evidence,build):
 qa=json.loads((dist/'QA_ULike_v1.9.23.json').read_text())
 validation=json.loads((evidence/'validation.json').read_text())
 pins=json.loads((evidence/'validated-artifacts.json').read_text())
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==pins[name]['sha256'],'Rebuild differs from validated artifact '+name)
 require(qa['host_tests']=={'total':285,'back_and_session':71,'exit':79,'layout':96,'shadow':39,'android_device_test':False},'Host test scope mismatch')
 for name,needle in [('host-tests1920.txt','PASS total=71'),('host-tests1921.txt','exit assertions=79'),('host-tests1922.txt','HOST_LAYOUT_ASSERTIONS=96'),('host-tests1923.txt','HOST_SHADOW_ASSERTIONS=39')]:require(needle in (build/name).read_text(),'New host tests missing '+name)
 require(set(validation['desktop_validation'])=={'single','bundle'},'Both APK applications required')
 for kind,row in validation['desktop_validation'].items():
  result=row['patch_result'];require(result['packageName']=='com.gorgeous.liteinternational' and result['packageVersion']=='5.6.2' and not result['failedPatches'] and all(x['success'] for x in result['patchingSteps']),'APK application failure')
  require('1541 runtime/payload contracts exactly retained' in row['method_contracts'],'Final APK contract failure')
  require(row['register_analysis']=={'all-registers':{'tested':1192,'failed':0},'stock-registers':{'tested':178,'failed':0}},'Register analysis failure')
 require('2 ratio returns and 1 onLayout return are label-safe' in validation['layout_contracts'] and '72 native ABI references' in validation['layout_contracts'],'Missing real DEX control-flow and ABI checks')
 require(not validation['android_device_test'],'Unexpected device scope')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=validation['desktop_validation'],layout_contracts=validation['layout_contracts'],rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False,other_app_loader_classes_unchanged=221,code_defects_confirmed=True,screenshot_root_cause_device_confirmed=False,shadow_finishing_preserves_existing_chroma=True)
 sources={p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex','.tsv')}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()}
 require(all(Path(n).suffix in ('.json','.txt','.log','.tsv') for n in ev),'Unexpected evidence artifact')
 qa['source_sha256']={n:sha(b) for n,b in sources.items()};qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 (dist/'QA_ULike_v1.9.23.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in sources.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for name in ('QA_ULike_v1.9.23.json','RELEASE_NOTES.txt','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','host-tests1923.txt','emitted-audit.tsv','references.log'):items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.23_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.23.json','RELEASE_NOTES.txt','ULike_v1.9.23_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP rebuild;285 host assertions;2 original APK applications;1541 method contracts per APK;1192+178 registers;label-safe returns;no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('dist','evidence','build'):p.add_argument('--'+name,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
