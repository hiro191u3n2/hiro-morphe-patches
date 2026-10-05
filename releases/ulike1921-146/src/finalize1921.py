#!/usr/bin/env python3
"""Bind deterministic rebuilds to this turn's desktop application test evidence."""
from pathlib import Path
import argparse,json,re
from build1921 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.21.mpp':'201dfb110a64da8962bf3a20ea6f531feb5a8ad39242c2f642a9cc872a3fe25a','Hiro_Morphe_Patches_v1.0.146.mpp':'c248e0e278868187a9f35f1f4de698382735d48047dfe97108626aa40fd7b8d6'}
NOTES='''ULike v1.9.21 / Hiro Morphe Patches v1.0.146

* **ULike:** 戻る操作時に「保存、録画の完了後に終了します」と表示され操作できなくなる不具合を修正しました。削除済みのStyleStill4.callbacksを終了判定だけが参照し、存在しないフィールドの例外を「撮影中」と扱うことがコード上の原因でした。この古い診断用参照を除去し、実在する撮影・保存・録画の状態を保護します。
* **ULike - 終了待ち:** 画面を閉じる前の終了待ちは、再度の戻る、タッチ、別キー操作で取り消し、操作に戻れます。未完了の状態が続く場合も15秒で終了待ちを取り消します。保存ジョブの強制削除、録画データの破棄、タイムアウトでの強制終了は行いません。既に要求した録画停止は取り消しません。
* **ULike - 戻る操作:** 設定、微調整、スタイル、美顔、フィルタ等を閉じて撮影画面に戻る処理を保持しています。開いているパネルがなく、撮影・保存・録画が完了していれば、撮影画面の戻る操作で従来どおり完全終了します。
* **ULike - 保持:** 顔特徴点の高解像度検出、画質・撮影処理、v1.9.20の起動待ち復帰対策を保持しています。削除済みの設定・診断機能や撤回済みの10bit経路は戻していません。他アプリ用パッチも変更していません。
* **ULike - 検証:** 150件のホスト試験、実在する終了判定用フィールドの照合、単体・統合MPPの未改造ULikeへの適用・APK再構築を実行しました。両方で各1,500件のメソッド照合、1,159件と170件のレジスタ解析が成功しています。別作業フォルダでの再ビルドもMPPのSHA-256が一致しました。

対象：未改造ULike 5.6.2（740）、arm64-v8a。Managerでソース更新後、単体版または統合版の一方で再パッチ・再インストールしてください。ソース更新だけではインストール済みアプリは変わりません。

Galaxy/Android実機での操作・撮影・保存は未確認です。ホスト試験はモックを使用した制御フロー試験で、Android/ART実行ではありません。今回のコード上の終了待ち原因は特定していますが、以前の黒画面の実機解消は引き続き未確認です。
'''

def finalize(dist,evidence,build):
 qpath=dist/'QA_ULike_v1.9.21.json';qa=json.loads(qpath.read_text())
 require(qa['status']=='BUILT_HOST_TESTED_DEVICE_UNVERIFIED_NOT_PUBLISHED','Wrong build QA status')
 identity=json.loads((evidence/'local-test-identity.json').read_text());validation=json.loads((evidence/'desktop-validation.json').read_text())
 require(identity['tested_mpp_sha256']==EXPECTED and identity['independent_build_exact'],'Wrong local validation identity')
 require(not identity['android_device_tested'] and not validation['android_device_test'],'Unexpected device claim')
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==qa['artifacts'][name]['sha256'],'Not desktop-tested exactMPP '+name)
 for kind in ('single','bundle'):
  v=validation['desktop_validation'][kind];r=v['patch_result']
  require(r['packageName']=='com.gorgeous.liteinternational' and r['packageVersion']=='5.6.2' and not r['failedPatches'] and all(s['success'] for s in r['patchingSteps']),'Desktop apply failure')
  require('1500 runtime/payload contracts exactly retained' in v['method_contracts'],'Contract count mismatch')
  require(v['register_analysis']=={'all-registers':{'tested':1159,'failed':0},'stock-registers':{'tested':170,'failed':0}},'Register verification mismatch')
 require(qa['host_tests']=={'total':150,'back':26,'session':45,'exit':79,'android_device_test':False},'Host test mismatch')
 require('exit assertions=79' in (build/'host-tests1921.txt').read_text(),'Missing this-build exit regression tests')
 require('PASS total=71' in (build/'host-tests1920.txt').read_text(),'Missing this-build regression tests')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=validation['desktop_validation'],independent_build_exact=True,rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False)
 qa['source_sha256']={p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.py','.java')}
 qa['desktop_evidence_sha256']={p.relative_to(evidence).as_posix():sha(p.read_bytes()) for p in sorted(evidence.rglob('*')) if p.is_file()}
 qpath.write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.py','.java')}
 items.update({'evidence/'+p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()})
 for name in ('QA_ULike_v1.9.21.json','RELEASE_NOTES.txt','host-tests1920.txt','host-tests1921.txt','transform1921-audit.tsv','busy-schema.tsv','helper-references.txt'):items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.21_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.21.json','RELEASE_NOTES.txt','ULike_v1.9.21_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP identity,150 host tests,2 desktop applications,each1500 contracts and1159+170 register checks; no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ('dist','evidence','build'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
