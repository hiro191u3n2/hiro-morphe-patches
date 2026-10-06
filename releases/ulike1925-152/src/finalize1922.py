#!/usr/bin/env python3
"""Bind CI byte-identical rebuild to completed local application/linkage tests."""
from pathlib import Path
import argparse,json
from build1922 import sha,require,write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={'ULike_HQ_Texture_Online_v1.9.22.mpp':'5fd0d17b8e5001d77f1bb8299610349fd0c3934318db76fb735dcb51fd2bade3','Hiro_Morphe_Patches_v1.0.147.mpp':'a2157ac3d1dd34a9681b0123090d2b863e067afcccf28054d09d9dfad91ed70b'}
NOTES='''ULike v1.9.22 / Hiro Morphe Patches v1.0.147

* **ULike:** 上部の黒帯が大きくなりプレビューが狭く残る問題への修正です。表示アニメーションの開始直後に管理用参照が消され、再配置後も旧アニメーションが黒帯の高さを書き戻せる不具合をコード上で確認しました。開始から完了まで参照を保持し、次の配置計算の前に古い処理を取消します。古い更新・完了通知も無視します。
* **ULike - 遅延処理:** 最初の映像を待つ遅延処理を、対象の表示アニメーションと結び付けました。古い通知が新しい配置を開始することはなく、最初の映像通知が来ない場合の開始待ちにも上限を設けています。
* **ULike - 配置:** 再配置時は右端・下端の座標ではなく、左端・上端を引いた実際の幅・高さを使用します。上余白の変更時も計算を更新し、静止時に表示用の黒帯と本来の目標位置が不一致なら一度だけ合わせます。固定ピクセルへの強制移動ではなく、アプリ本来の比率・余白を使用します。
* **ULike - 維持:** 顔特徴点の高解像度検出、画質・撮影・保存処理、設定・各パネルの戻る操作、終了待ち取消しを保持しています。削除済みの診断機能や撤回済み10bit経路は戻していません。他アプリ用パッチも変更していません。
* **ULike - 検証:** 239件のホスト試験（新規表示制御89件・既存回帰150件）、元アプリの対象9メソッドと参照先の照合、単体・総合MPPの元アプリへの適用・APK再構築に成功しました。両方で各1,524件のメソッド照合、1,177件と176件のレジスタ解析が成功しています。独立再ビルドのMPPもSHA-256が一致しました。

対象：未改造ULike 5.6.2（740）、arm64-v8a。Managerでソース更新後、単体版または総合版の一方で再パッチ・再インストールしてください。ソース更新だけではインストール済みアプリは変わりません。

Galaxy/Android実機での操作・撮影・保存は未確認です。提供画像と整合するコード上の不具合を修正しましたが、端末で同じ発生経路だったか、再発しなくなったかは未確認です。ホスト試験はモックを使った制御試験で、Android/ART実行ではありません。画質処理のコードは変更していませんが、実機での画質比較は行っていません。
'''
def sources():
 return {p.relative_to(ROOT).as_posix():p.read_bytes() for p in sorted(ROOT.rglob('*')) if p.is_file() and p.suffix in ('.py','.java','.dex','.tsv')}
def finalize(dist,evidence,build):
 qpath=dist/'QA_ULike_v1.9.22.json';qa=json.loads(qpath.read_text())
 require(qa['status']=='BUILT_HOST_TESTED_DEVICE_UNVERIFIED_NOT_PUBLISHED','Wrong build QA status')
 identity=json.loads((evidence/'local-test-identity.json').read_text());validation=json.loads((evidence/'desktop-validation.json').read_text())
 require(identity['tested_mpp_sha256']==EXPECTED and identity['independent_build_exact'],'Missing local validation identity')
 require(not identity['android_device_tested'] and not validation['android_device_test'],'Unexpected device claim')
 for name,pin in EXPECTED.items():require(sha((dist/name).read_bytes())==pin==qa['artifacts'][name]['sha256'],'Not exact tested MPP '+name)
 for kind in ('single','bundle'):
  v=validation['desktop_validation'][kind];r=v['patch_result']
  require(r['packageName']=='com.gorgeous.liteinternational' and r['packageVersion']=='5.6.2' and not r['failedPatches'] and all(s['success'] for s in r['patchingSteps']),'Desktop apply failure')
  require('1524 runtime/payload contracts exactly retained' in v['method_contracts'],'Contract count mismatch')
  require(v['register_analysis']=={'all-registers':{'tested':1177,'failed':0},'stock-registers':{'tested':176,'failed':0}},'Register validation mismatch')
 require(validation['layout_contracts'].startswith('PASS 9 original layout contracts; 2 callback guards;'),'Missing full-stock linkage and guard test')
 require(qa['host_tests']=={'total':239,'back':26,'session':45,'exit':79,'layout':89,'android_device_test':False},'Host test mismatch')
 require('HOST_LAYOUT_ASSERTIONS=89' in (build/'host-tests1922.txt').read_text(),'Missing new build layout tests')
 require('exit assertions=79' in (build/'host-tests1921.txt').read_text(),'Missing exit regression tests')
 require('PASS total=71' in (build/'host-tests1920.txt').read_text(),'Missing prior regression tests')
 qa.update(status='REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',original_apk_apply_tested=True,desktop_validation=validation['desktop_validation'],layout_contracts=validation['layout_contracts'],independent_build_exact=True,rebuild_sha256_matches_desktop_evidence=True,ci_android_apply_tested=False,device_tested=False)
 src=sources();qa['source_sha256']={n:sha(b) for n,b in src.items()}
 ev={p.relative_to(evidence).as_posix():p.read_bytes() for p in sorted(evidence.rglob('*')) if p.is_file()}
 qa['desktop_evidence_sha256']={n:sha(b) for n,b in ev.items()}
 qpath.write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n');(dist/'RELEASE_NOTES.txt').write_text(NOTES)
 items={'src/'+n:b for n,b in src.items()};items.update({'evidence/'+n:b for n,b in ev.items()})
 for name in ('QA_ULike_v1.9.22.json','RELEASE_NOTES.txt','host-tests1920.txt','host-tests1921.txt','host-tests1922.txt','transform1922-audit.tsv','helper-references.txt'):items[name]=(dist/name).read_bytes()
 write_zip(dist/'ULike_v1.9.22_sources_and_QA.zip',items)
 names=[*EXPECTED,'QA_ULike_v1.9.22.json','RELEASE_NOTES.txt','ULike_v1.9.22_sources_and_QA.zip']
 (dist/'SHA256SUMS.txt').write_text(''.join(sha((dist/n).read_bytes())+'  '+n+'\n' for n in names))
 print('PASS exact MPP identity,239 host assertions,2 original-APK applications,each1524 method contracts and1177+176 register checks; no device claim')
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for n in ('dist','evidence','build'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
