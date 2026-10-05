#!/usr/bin/env python3
"""Bind CI-rebuilt MPPs to preserved local desktop evidence, not device testing."""
import argparse, hashlib, json, shutil
from pathlib import Path
from build1920 import sha, require, write_zip
ROOT=Path(__file__).resolve().parent
EXPECTED={
 'ULike_HQ_Texture_Online_v1.9.20.mpp':'4a62ca3e345a918da881df33e2316a4bb1cdeb72bda042b7608b1018b094329a',
 'Hiro_Morphe_Patches_v1.0.145.mpp':'4a61b5ef5a11be93d79d23959a89ecbdb1ea3aac4ca970440e54428b55053477'}
NOTES="""ULike v1.9.20 / Hiro Morphe Patches v1.0.145 修正候補版

* **ULike - 戻る操作:** 微調整・スタイル・美顔・フィルタなどの標準パネルを閉じる処理を、追加した完全終了処理より先に呼びます。パネルが戻る操作を消費したときはアプリを終了しません。設定画面から撮影画面に戻る動作と、保存中の終了保護を維持します。
* **ULike - 黒画面対策の範囲:** 最大センサーモードのCamera2構成要求が戻った後、構成完了通知が10秒間届かない場合に、既存の通常センサーモードへの復帰処理を呼びます。状態トークン・前面状態・元のExecutorで競合を防ぎます。復帰が発動したセッションでは最大センサーモードを使わないため、最大解像度は保証しません。
* **ULike - 維持:** 顔特徴点の高解像度検出、前版の設定整理、承認済みv1.8.8系統を維持します。削除した診断や撤回済み10bit経路は復元しません。他アプリのパッチと素材は変更しません。
* **ULike - 検証:** GitHub側で再ビルド・71件の模擬ホスト試験を実行し、両MPPが添付候補版のSHA256と完全一致することを公開条件とします。元ULikeへのパッチ適用、各1492件のメソッド照合、1151件＋170件のレジスタ解析は、候補版作成時のローカル検証記録に基づきます。CIでは元APKを配布・再実行しません。

重要: Galaxy/Android実機では未検証です。今回報告された黒画面の原因確定・解消確認は未完了です。本対策は通常センサーモードの停止、描画エンジン、権限・プライバシー状態、同期的に停止したドライバーやExecutorの全てを直すものではありません。ホスト模擬試験やDEX静的解析は実機試験ではありません。

適用: 未改造ULike 5.6.2（740）、arm64-v8aを使用してください。Morphe Managerでソース更新後、単体版または統合版のどちらか一方で再パッチ・再インストールします。ソース更新だけではインストール済みアプリの動作は変わりません。
"""
def finalize(dist,evidence,build):
 current=json.loads((dist/'QA_ULike_v1.9.20.json').read_text())
 local=json.loads((evidence/'QA_ULike_v1.9.20.json').read_text())
 require(current['status']=='BUILT_NOT_DEVICE_VERIFIED_NOT_PUBLISHED','Missing successful build')
 require(local['status']=='DESKTOP_CHECKS_PASSED_DEVICE_UNVERIFIED_NOT_PUBLISHED','Missing local desktop evidence')
 require(not local['black_preview_cause_confirmed'] and not local['black_preview_fixed_on_device'],'Invalid device assertion')
 require(local['host_tests']=={'total':71,'back':26,'session':45,'android_device_test':False},'Wrong host test scope')
 require('PASS total=71 back=26 session=45' in (dist/'host-tests1920.txt').read_text(),'CI host test missing')
 # Verify selected evidence and every source against the original archive manifest.
 checked=[];original_hashes={}
 for line in (evidence/'SOURCE_SHA256SUMS.txt').read_text().splitlines():
  digest,name=line.split(None,1);name=name.lstrip('* ')
  p=Path(name);require(not p.is_absolute() and '..' not in p.parts,'Unsafe source hash path')
  original_hashes[name]=digest
 for full in sorted(evidence.rglob('*')):
  if not full.is_file() or full.name=='SOURCE_SHA256SUMS.txt':continue
  name=full.relative_to(evidence).as_posix();p=Path(name)
  require(name in original_hashes and sha(full.read_bytes())==original_hashes[name],'Evidence changed: '+name)
  if p.parts[0]=='src':
   require((ROOT/Path(*p.parts[1:])).read_bytes()==full.read_bytes(),'Build source differs from local candidate: '+name)
  checked.append(name)
 require(len(checked)==31,'Incomplete selected evidence')
 for name,pin in EXPECTED.items():
  raw=(dist/name).read_bytes()
  require(sha(raw)==pin==local['artifacts'][name]['sha256']==current['artifacts'][name]['sha256'],'MPP differs from desktop-tested attachment')
  require(len(raw)==local['artifacts'][name]['bytes'],'MPP size differs')
 for kind in ('single','bundle'):
  v=local['final_apk_validation'][kind];result=v['patch_result']
  require(not result['failedPatches'] and all(s['success'] for s in result['patchingSteps']),'Local patch application failed')
  require('1492 runtime/payload contracts exactly retained' in v['method_contracts'],'Missing local contracts')
  for label,count in (('all-registers',1151),('stock-registers',170)):
   require(v['register_flow_analysis'][label]=={'tested':count,'failed':0},'Missing local register checks')
  require(v['apk_crc_pass'] and v['native_architectures']==['arm64-v8a'],'Invalid local APK evidence')
 qa={**local,'status':'CI_REBUILD_MATCHES_DESKTOP_VALIDATED_ATTACHMENT_DEVICE_UNVERIFIED',
     'ci_rebuilt_and_sha256_matched':True,'ci_host_tests_passed':True,
     'original_apk_execution_location':'Earlier authoring container only; exact MPP SHA256 bound. CI does not execute or upload the original APK.',
     'ci_original_apk_execution':False,'preserved_evidence_files_verified':len(checked),
     'evidence_format':'Selected original local evidence: full build/validation sources, QA JSON and result summaries. Full original archive SHA256 9a30d5c673bdc1db46f728ac98259d6d6833d31751d495df19084ece273c50ab remains attached in the originating conversation.',
     'published':False,'manager_feed_updated':False,
     'publication_note':'Pre-publication QA snapshot. See publication_ULike_v1.9.20.json for completed release and feed verification.'}
 (dist/'QA_ULike_v1.9.20.json').write_text(json.dumps(qa,ensure_ascii=False,indent=2)+'\n')
 (dist/'RELEASE_NOTES.txt').write_text(NOTES)
 (dist/'SHA256SUMS.txt').write_text(''.join(f'{pin}  {name}\n' for name,pin in EXPECTED.items()))
 items={'README_JA.txt':NOTES.encode(),'PUBLICATION_STATE.txt':b'This archive is sealed before publication. Prior candidate evidence retains historical unpublished state. The separate publication receipt records final delivery.\n'}
 for p in sorted(ROOT.rglob('*')):
  if p.is_file() and p.suffix in ('.java','.py'):items['src/'+p.relative_to(ROOT).as_posix()]=p.read_bytes()
 for p in sorted(evidence.rglob('*')):
  if p.is_file():items['local-evidence/'+p.relative_to(evidence).as_posix()]=p.read_bytes()
 for name in ('QA_ULike_v1.9.20.json','RELEASE_NOTES.txt','SHA256SUMS.txt','host-tests1920.txt','transform1920-audit.tsv'):
  items[name]=(dist/name).read_bytes()
 for p in sorted(build.iterdir()):
  if p.is_file() and p.suffix in ('.log','.txt','.tsv'):items['ci-build/'+p.name]=p.read_bytes()
 items['SOURCE_SHA256SUMS.txt']=''.join(f'{sha(raw)}  {name}\n' for name,raw in sorted(items.items())).encode()
 write_zip(dist/'ULike_v1.9.20_sources_and_QA.zip',items)
 print('PASS: both rebuilt MPPs equal exact desktop-validated attachments; 71 CI host checks; device/black-preview unverified',flush=True)
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for k in ('dist','evidence','build'):p.add_argument('--'+k,type=Path,required=True)
 a=p.parse_args();finalize(a.dist.resolve(),a.evidence.resolve(),a.build.resolve())
