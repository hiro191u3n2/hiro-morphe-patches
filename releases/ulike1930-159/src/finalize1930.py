#!/usr/bin/env python3
"""Bind local APK validation to reproducibly rebuilt MPPs and prepare release files."""
from pathlib import Path
import argparse
import json
from build1930 import require, sha, write_zip, SINGLE, BUNDLE

SOURCE_NAMES = {
    'RearLensUi1930.java', 'Transform1930.java', 'PatchClass1930.java', 'VerifyUi1930.java',
    'MergePayloads.java', 'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java',
    'DumpDex.java', 'build1930.py', 'validate1930.py', 'finalize1930.py', 'host_ui1930.py',
    'native-ui1930-abi.txt',
}
NOTES = '''ULike v1.9.30 / Hiro Morphe Patches 総合版 v1.0.159

【インカメラで背面用の倍率・接写ボタンが出る表示不具合を修正】
黒帯をダブルタップしてインカメラへ切り替えたときも、倍率ボタン（1×・3×・5×）、接写ボタン、各ラベルをまとめて非表示にします。
表示判定が最後に使った背面レンズ情報だけを参照していました。現在の撮影画面で選択されている前後カメラを、既存の状態から読み取る判定を追加しました。前面または状態不明なら背面用UIを表示しません。
通常の表示更新の100ms間引きより先に、次の描画前に非表示へ反映します。遅れて届いた表示更新や追加時の更新も同じ条件で判定します。前面選択後に残ったクリックから背面レンズの切替を予約することも抑止します。
アウトカメラへ戻った場合は、背面レンズ情報と従来の表示条件が整い次第、倍率・接写ボタンを通常どおり表示します。

【基準と変更範囲】
ULike v1.9.29と、SwiftKey v1.8.3を含む総合版v1.0.158を基準にしています。変更は表示・操作受付の既存3メソッドと、現在の選択を読む小さなヘルパーです。
黒帯ダブルタップのカメラ切替、前回カメラの記憶、背面起動・終了の修正、倍率・接写の撮影処理、画質・ノイズ低減・美顔・保存処理、他アプリのコードと資産は保持します。
追加のカメラ再起動、常駐タイマー、毎描画ログ、通信はありません。

【検証】
実際に組み込むJavaヘルパーのホスト試験、生成DEXの分岐と接続・既存コード保持・元APK側の型と参照を検査しました。単体・総合版とも未改造ULike5.6.2（740）APKSへ適用してAPKを再構築し、組込メソッドの一致とレジスタ型解析を確認しています。
公開時は同じMPPを再ビルドし、元APKSに適用して検証したファイルとのSHA-256一致を条件にします。元APKS、再構築APK、ユーザー写真は公開しません。
Galaxy実機での表示・操作は未確認です。ホスト試験やDEX検査を実機動作確認として扱いません。

【適用】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して再インストールしてください。arm64-v8aが対象です。総合版と単体版はどちらか一方を使います。ソース更新だけではインストール済みULikeの動作は変わりません。アプリデータ削除や追加設定は不要です。
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1930-desktop-validation-v1', 'Wrong validation schema')
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
    qa_path = args.dist / 'QA_ULike_v1.9.30.json'
    qa = json.loads(qa_path.read_text())
    require(qa['artifacts'] == evidence['artifacts'], 'QA binary mismatch')
    qa.update(status='REBUILD_MATCHES_ORIGINAL_APKS_VALIDATED_MPP_DEVICE_UNVERIFIED',
              original_apk_apply_tested=True, device_tested=False, device_fix_confirmed=False,
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              ui_abi_verification=evidence['ui_abi_verification'],
              desktop_evidence_sha256=sha(args.validation.read_bytes()))
    sources = {}
    for p in sorted(args.source.rglob('*')):
        if not p.is_file():
            continue
        rel = p.relative_to(args.source)
        if (len(rel.parts) == 1 and p.name in SOURCE_NAMES) or (rel.parts[0] in ('ui-stubs', 'ui-host') and p.suffix == '.java'):
            sources['src/' + rel.as_posix()] = p.read_bytes()
    require(all('src/' + name in sources for name in SOURCE_NAMES), 'Incomplete source package')
    qa['source_sha256'] = {n: sha(b) for n, b in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(NOTES)
    package = dict(sources)
    for name in ('QA_ULike_v1.9.30.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv', 'ui-dex-verification.txt',
                 'helper-references.txt', 'host-ui1930.txt'):
        package[name] = (args.dist / name).read_bytes()
    package['evidence/validation.json'] = args.validation.read_bytes()
    write_zip(args.dist / 'ULike_v1.9.30_sources_and_QA.zip', package)
    files = [SINGLE, BUNDLE, 'QA_ULike_v1.9.30.json', 'ULike_v1.9.30_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / n).read_bytes()) + '  ' + n + '\n' for n in files))
    print('PASS release files bound to exact original-APKS-tested MPPs; no device claim')


if __name__ == '__main__':
    main()
