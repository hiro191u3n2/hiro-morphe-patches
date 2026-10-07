#!/usr/bin/env python3
"""Bind local APK validation to reproducibly rebuilt MPPs and prepare release files."""
from pathlib import Path
import argparse
import json
from build1931 import require, sha, write_zip, SINGLE, BUNDLE

SOURCE_NAMES = {
    'FrontPreview1931.java', 'Transform1931.java', 'PatchClass1931.java', 'Seed1931.java',
    'VerifyFront1931.java', 'VerifyNativeAbi1931.java', 'VerifyFrontAbi1931.java', 'MergePayloads.java',
    'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java', 'DumpDex.java',
    'build1931.py', 'validate1931.py', 'finalize1931.py', 'host_front1931.py',
    'stock-seed1931.dex', 'stock-seed1931-hashes.tsv',
}
NOTES = '''ULike v1.9.31 / Hiro Morphe Patches 総合版 v1.0.164

【インカメラで終了後、再起動すると撮影プレビューが黒くなる問題への修正】
元ULikeではカメラを開いた通知直後のstartPreview戻り値が使われず、映像入力が遅れて届いた場合の再開処理がありません。既存パッチの開始再試行は背面だけを対象とし、インカメラでは入力準備エラーを破棄していました。
また、元の入力有効判定は画像寸法とリスナーだけを確認し、表示用SurfaceTextureが解放済みでも通ります。前回の修正はこの再生成も背面だけに限っていました。新しい前面専用処理で、実際の表示入力と所有者を確認し、解放済みなら元ULikeのnewSurfaceTexture経路で正規に作り直します。

【変更】
インカメラで映像入力・カメラ本体の準備が遅れた場合、同じ起動に限って最大2回・5秒以内で開始要求を再送します。Camera1/Camera2のクライアント、設定、実デバイス、処理スレッド、前景の世代を照合し、カメラ本来の処理スレッド上で実行します。
停止、終了、前後カメラ切替、モード切替、バックグラウンド移行では保留要求を取り消します。録画・撮影保存中は再試行を待機させ、別の起動やカメラに移った場合は破棄します。既に正常開始したプレビューを定期的に再起動する処理は追加しません。
インカメラの通常Camera2モードでは、設定により有効になる出力Surfaceの後付け構成を避け、実際の出力準備後にセッションを作ります。既存背面レンズ用の処理には委譲し、背面の開始再試行処理本体を保持します。

【保持する既存改造】
単体ULike1.9.30、総合1.0.163が基準です。前回の前後カメラ記憶、インカメラ時の倍率・接写ボタン非表示、黒帯ダブルタップ、画質・ノイズ除去・美顔・写真保存、総合版の他アプリ改造を保持します。
当初検討した同じprovider managerの再採用競合は元APKの実経路では成立しないことを確認したため、既存のprovider寿命管理には変更を加えていません。

【検証と範囲】
修正前のインカメラ開始失敗と解放済み入力の見逃しをホスト上の回帰テストで再現し、新しい実装での解消を検証しました。カメラ所有者の変更、終了・切替の競合、入力到着順、処理スレッド、待機時間と再試行回数の制限も確認しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、全DEX整合性、組込メソッド契約、追加コードと変更ネイティブメソッドの型解析を実施しました。出力はarm64-v8a専用です。
公開時は同じ固定基準とツールで再ビルドし、元APKS適用済みのMPPとSHA-256が一致することを条件にします。元APKS・再構築APK・添付スクリーンショットは配布物に含めません。
Galaxy実機での撮影画面の復帰・撮影保存は未確認です。コード上で再現した不備を修正した版であり、利用端末上の症状が完全解消したとの確認ではありません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。単体と総合はどちらか一方を使用します。
ソース更新だけでは、インストール済みULikeの処理は変わりません。アプリデータ削除は不要です。
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1931-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APKS application required')
    require(evidence.get('device_tested') is False and evidence.get('device_fix_confirmed') is False, 'No hardware claim allowed')
    for name in (SINGLE, BUNDLE):
        p = args.dist / name
        require(evidence['artifacts'][name] == {'sha256': sha(p.read_bytes()), 'bytes': p.stat().st_size},
                'Rebuilt MPP differs from original-APKS-tested bytes: ' + name)
    for kind in ('single', 'bundle'):
        outcome = evidence['desktop_validation'][kind]
        require(not outcome['patch_result']['failedPatches'] and all(x['success'] for x in outcome['patch_result']['patchingSteps'])
                and outcome['register_analysis']['failed'] == 0, 'Invalid desktop validation result')
    qa_path = args.dist / 'QA_ULike_v1.9.31.json'
    qa = json.loads(qa_path.read_text())
    require(qa['artifacts'] == evidence['artifacts'], 'QA binary mismatch')
    qa.update(status='REBUILD_MATCHES_ORIGINAL_APKS_VALIDATED_MPP_DEVICE_UNVERIFIED',
              original_apk_apply_tested=True, device_tested=False, device_fix_confirmed=False,
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              front_abi_verification=evidence['front_abi_verification'],
              stock_lifecycle_abi_verification=evidence['stock_lifecycle_abi_verification'],
              front_dex_verification=evidence['front_dex_verification'],
              desktop_evidence_sha256=sha(args.validation.read_bytes()))
    require(all(outcome.get('dex_integrity_entries', 0) > 0 for outcome in evidence['desktop_validation'].values()), 'Missing DEX integrity validation')
    sources = {}
    for p in sorted(args.source.rglob('*')):
        if not p.is_file():
            continue
        rel = p.relative_to(args.source)
        if (len(rel.parts) == 1 and p.name in SOURCE_NAMES) or (rel.parts[0] in ('front-stubs', 'front-host', 'front-baseline') and p.suffix == '.java'):
            sources['src/' + rel.as_posix()] = p.read_bytes()
    require(all('src/' + name in sources for name in SOURCE_NAMES), 'Incomplete source package')
    qa['source_sha256'] = {n: sha(b) for n, b in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(NOTES)
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    for name in ('QA_ULike_v1.9.31.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv', 'front-dex-verification.txt',
                 'helper-references.txt', 'host-front1931.txt'):
        package[name] = (args.dist / name).read_bytes()
    package['evidence/validation.json'] = args.validation.read_bytes()
    write_zip(args.dist / 'ULike_v1.9.31_sources_and_QA.zip', package)
    files = [SINGLE, BUNDLE, 'QA_ULike_v1.9.31.json', 'ULike_v1.9.31_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / n).read_bytes()) + '  ' + n + '\n' for n in files))
    print('PASS release files bound to exact original-APKS-tested MPPs; no device claim')


if __name__ == '__main__':
    main()
