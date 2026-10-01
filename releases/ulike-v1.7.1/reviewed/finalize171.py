#!/usr/bin/env python3
"""Bind exact locally tested MPP bytes, re-run host gates, and package source-only QA."""
import argparse,hashlib,json,re,zipfile,shutil
from pathlib import Path
NOTES='''【統合MPP v1.0.104：ULike v1.7.1 起動不具合対策】
v1.7.0の新しいレンズ制御に、別クラスからprivateのOpticalZoom.failを直接呼ぶアクセス権限不整合が3箇所ありました。参照先の存在だけを確認する旧検査では見逃されていたため、同一パッケージの復旧処理から呼べる最小限の可視性に修正し、実際のDEXおよび再構築後APKでprivateアクセス違反が0件になることを検査します。

撮影リクエストとカメラセッションの初期化で生じるRuntimeException・LinkageError・内部参照エラーを、元の原因を保持したCameraAccessExceptionに変換し、ULikeの既存カメラエラー処理へ渡します。既存のCameraAccessExceptionはそのまま保持します。復旧通知自身の失敗がカメラのエラーコールバックを妨げないようにし、失敗・終了したセッションの構成済みフラグも解除します。非対応の固定出力を成功と表示する変更ではありません。

接写チューリップ、1倍メイン広角、3倍・5倍の手動選択、距離による自動レンズ選択を使わない仕様は保持しています。ボタン配置、レンズ識別、通常AF、撮影中・録画中の保護、美顔、HEIF、保存高速化、素材、他アプリ用パッチは変更しません。

PC上の模擬テスト762項目、privateアクセス検査、単体・総合版を未改造のULike 5.6.2（740）へ適用・再構築した各1,404メソッドの照合を通過しました。単体版・総合版から生成したAPKの各ファイル内容も一致しています。公開側では、そのローカル検証済みMPPと同じSHA-256のバイト列を再現してから配信します。

Galaxy実機での起動復旧・レンズ切替・撮影保存は未確認です。端末のクラッシュログは未取得のため、今回見つかった不整合が実機の起動失敗原因のすべてと断定はしません。端末が公開しない物理レンズの固定や、非対応セッションの成功は保証しません。

対象は未改造ULike 5.6.2（740）arm64-v8aです。Managerのソースを更新し、単体版か総合版のどちらか一方を元アプリへ適用し直してください。旧パッチ済みAPKへの重ねがけはしないでください。元APK/APKS、写真、署名鍵は公開しません。
'''
SUMMARY='起動時のレンズ復旧処理にあるprivateアクセス不整合を修正。初期化例外と失敗通知を保護し、手動レンズ・美顔・HEIF・保存高速化を維持。Galaxy実機未検証。'
def sha(raw):return hashlib.sha256(raw).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--dist',type=Path,required=True);p.add_argument('--reviewed',type=Path,required=True);a=p.parse_args();r=a.source;d=a.dist
 raw=a.reviewed.read_bytes();q=json.loads(raw);assert q['version']=='1.7.1' and q['bundle_version']=='1.0.104'
 generated=json.loads((d/'build-artifacts.json').read_text());actual=[{k:x[k] for k in ['file','bytes','sha256']} for x in generated]
 assert actual==q['artifacts'],'CI/local MPP mismatch'
 for x in actual:assert len((d/x['file']).read_bytes())==x['bytes'] and sha((d/x['file']).read_bytes())==x['sha256']
 assert '596 assertions' in (r/'qa170/HOST_MANUAL170.txt').read_text()
 assert '65 assertions' in (r/'regression170/qa/HOST_TESTS.txt').read_text()
 assert '101 new host assertions' in (r/'regression170/qa/HOST_FAST167_TESTS.txt').read_text()
 assert 'PRIVATE_ACCESS_VIOLATIONS=3' in (r/'qa170/ACCESS_BEFORE.txt').read_text()
 assert (r/'qa170/ACCESS_AFTER.txt').read_text().strip()=='PRIVATE_ACCESS_VIOLATIONS=0'
 assert 'preserved=1230, changed=7, added=3, total=1240' in (r/'qa170/RUNTIME_TRANSFORM.txt').read_text()
 for n in ['HOST_TESTS.txt','HOST_FAST167_TESTS.txt']:shutil.copyfile(r/'regression170/qa'/n,r/'qa170'/n)
 for name,text in q['local_applied_evidence'].items():(r/'qa170'/name).write_text(text)
 (r/'release_pipeline/LOCAL_QA.json').write_bytes(raw);(d/'QA_ULike_v1.7.1.json').write_bytes(raw)
 (r/'release_pipeline/RELEASE_NOTES.txt').write_text(NOTES)
 (r/'release_pipeline/CHANGELOG_SUMMARY.txt').write_text(SUMMARY+'\n')
 (r/'release_pipeline/README.md').write_text('# ULike v1.7.1 / Hiro Morphe v1.0.104\n\n'+NOTES+'\nソース内のManualLens170等の名前は既存DEXとの互換性のため保持しています。build170.pyはv1.7.1生成用に更新済みです。QAのAPK適用結果はローカル検証であり、公開ワークフローで実機テストを実施したという意味ではありません。\n')
 shutil.copyfile(Path(__file__),r/'finalize171.py')
 shutil.copyfile(Path(__file__).with_name('repair171.py'),r/'repair171.py')
 permitted={'.java','.py','.sh','.json','.txt','.md','.tsv'};files=[]
 for f in r.rglob('*'):
  n=f.relative_to(r).as_posix()
  if not f.is_file() or f.suffix not in permitted:continue
  if n.startswith(('ci-build170/','host170-classes/','regression170/tests/classes/','regression170/qa/')):continue
  if n in ['host170-sources.txt','regression170/tests/sources.txt','SOURCE_FILES.json']:continue
  files.append(n)
 (r/'SOURCE_FILES.json').write_text(json.dumps({n:sha((r/n).read_bytes()) for n in sorted(files)},indent=2)+'\n');files.append('SOURCE_FILES.json')
 out=d/'ULike_v1.7.1_sources_and_QA.zip'
 with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for n in sorted(files):
   info=zipfile.ZipInfo(n,(2026,10,2,6,45,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,(r/n).read_bytes())
 with zipfile.ZipFile(out) as z:assert z.testzip() is None
 (d/'CI_REPRODUCTION.json').write_text(json.dumps({'version':'1.7.1','result':'PASS_EXACT_REVIEWED_MPP_BYTES','artifacts':actual,'host_assertions':762,'device_tested':False,'source_zip_sha256':sha(out.read_bytes())},indent=2)+'\n')
 print('PASS: exact reviewed MPP bytes, 762 host assertions, access regression 3 -> 0; source-only ZIP generated; device unverified.')
if __name__=='__main__':main()
