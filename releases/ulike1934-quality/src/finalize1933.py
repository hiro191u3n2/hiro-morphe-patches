#!/usr/bin/env python3
"""Bind rebuilt ULike quality MPPs to exact desktop validation and package reproducible sources."""
from pathlib import Path
import argparse
import json
from build1933 import require, sha, write_zip, SINGLE, bundle_name, config, ROOT

REQUIRED_TOP_SOURCES = {
    'BurstCapture1933.java', 'CapturePolicy1933.java', 'FusionPixels1933.java',
    'FastPixels1933.java', 'FastResize1933.java',
    'Transform1933.java', 'PatchClass1933.java', 'VerifyBurst1933.java', 'MergePayloads.java',
    'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java', 'VerifyBurstAbi1933.java',
    'build1933.py', 'validate1933.py', 'finalize1933.py', 'host_burst1933.py',
    'host_capture1933.py', 'host_fusion1933.py', 'host_fast1933.py', 'host_metadata1933.py',
    'host_integration1933.py',
    'SeedBurst1933.java', 'extract_seed1933.py', 'stock-seed1933-capture.dex',
    'stock-seed1933-capture-hashes.tsv',
}
BUILD_INPUT_KEYS = (
    'ulike_version', 'bundle_version', 'baseline_ulike_version', 'baseline_ulike_sha256',
    'baseline_ulike_bytes', 'baseline_ulike_url', 'baseline_bundle_version',
    'baseline_bundle_sha256', 'baseline_bundle_bytes', 'baseline_bundle_url', 'toolchain_sha256',
    'jdk_runtime_version', 'jdk_vendor',
    'native_seed_sha256',
)


def release_notes(qa):
    return f'''ULike v1.9.33 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【採用した改造】
ご指定の候補47・50・60を採用しました。
47：同じ露出の複数フレームを利用するノイズ低減を追加します。撮影情報・画像の位置と内容を照合し、合成に適さないフレームは除外します。
50：暗所で実際のフレーム間の動きを測り、追加撮影のシャッター速度・ISO・枚数を調整します。手動露出に対応する経路では、動きがある場面は露光を短くしてISOを補い、静止が確認できる場面は露光を延ばしてISOを下げる方向へ調整します。動きを判定できない場合は長露光にしません。露出を変更するときは同じ露出条件の新しい2〜4枚の組を作り、露出の異なる元フレームを混ぜません。動きや位置ずれを考慮して合成し、適切なフレームを取得できない場合は元の1枚の撮影・保存経路へ戻します。
60：不透明な写真の高品質リサイズをRGBA4成分からRGB3成分の計算へ整理し、補間係数を共有します。90度・270度回転では列をまとめて読み取り、最大4並列で処理します。透明画素を含む画像は従来の処理へ戻し、ホストで従来処理との出力画素一致を検証します。複数フレームへの美顔処理の重複を避け、合成結果だけを既存の美顔・保存処理へ渡します。
合成する組は最大4枚です。最初の2枚で動きを判定してから露出を変更した新しい組を撮る場合、実際の取得枚数は合計で最大6枚になります。
複数枚の取得が増えるため、撮影から保存までが常に速くなるという意味ではありません。

【基準と保持する改造】
ULike1.9.32、総合版{qa['base_versions'][1]}が基準です。承認済み1.8.8系統を継承します。
前後カメラの記憶、インカメラ・背面のプレビュー復帰、黒帯ダブルタップ、インカメラ時の倍率・接写ボタン非表示など、起動・復帰の既存修正を保持します。
HEIF保存、既存の画質設定・美顔機能、今回以前の処理順・補間・撮影条件別NR・美肌連動・適応シャープ・色モアレ判定を継承します。
NR・シャープのON/OFFと強度段階、輪郭のハロー抑制設定を保持します。総合版内の他アプリの改造を保持します。
撤回済み1.9.17系の10bit処理、削除済みの診断表示や原本保管機能は復活させません。

【検証と範囲】
実装した画素合成と撮影制御のホスト回帰テスト、DEX変更範囲、保護対象のカメラ起動・復帰処理と他アプリの保持を検証しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、arm64-v8a専用出力、全DEXの整合性、組込メソッドの一致、追加コードと変更対象の型解析を実施しました。
公開ビルドは固定した入力・ツールチェーンから再構築し、元APKSへの適用検証を行ったMPPとSHA-256が一致することを条件にします。
ホストの合成画像を使った単一スレッドの画素計算部分では、7回の中央値402.47msから331.44ms（約17.65%短縮）を観測しました。Camera2・美顔SDK・画像エンコード・ファイル書込みを含む計測ではなく、端末の撮影・保存時間の改善率を示すものではありません。
Galaxy実機での複数枚撮影・合成・保存の動作、見た目の改善量、撮影・保存時間は未確認です。ホストテストの結果は、端末上の性能測定やカメラ動作確認を意味しません。
配布物に未改造APKS、再構築APK、ユーザーの写真や署名鍵は含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。単体と総合はどちらか一方を使用します。
ソース更新だけでは、インストール済みULikeの処理は変わりません。アプリデータ削除は不要です。
'''


def eligible_source(path, relative):
    if '__pycache__' in relative.parts:
        return False
    if len(relative.parts) == 1:
        return path.suffix in ('.java', '.py') or (path.name.endswith('1933.json') and 'hooks' in path.name) or (path.name.startswith('stock-seed') and path.suffix == '.dex') or path.name.endswith('hashes.tsv')
    folder = relative.parts[0]
    return path.suffix == '.java' and (
        folder == 'tests' or folder.endswith('-stubs') or folder.endswith('-host') or folder.startswith('host-'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    args = parser.parse_args()
    reviewed = config(args.manifest)
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1933-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APKS application required')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No hardware quality claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle differs from manifest')
    qa_path = args.dist / 'QA_ULike_v1.9.33.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1933-burst-v1' and qa.get('host_burst_passed') is True
            and qa.get('host_burst_result', {}).get('status') == 'passed',
            'Missing successful burst host validation')
    require(qa.get('camera_startup_recovery_byte_identical') is True,
            'Protected camera startup/recovery code must be preserved')
    require(qa.get('selected_candidates') == [47, 50, 60], 'Selected candidates differ')
    combined = bundle_name(qa)
    for name in (SINGLE, combined):
        path = args.dist / name
        require(evidence['artifacts'][name] == {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size},
                'Rebuilt MPP differs from original-APKS-tested bytes: ' + name)
    require(set(evidence.get('desktop_validation', {})) == {'single', 'bundle'}, 'Both APK validations are required')
    for kind in ('single', 'bundle'):
        outcome = evidence['desktop_validation'][kind]
        require(not outcome['patch_result']['failedPatches'] and all(x['success'] for x in outcome['patch_result']['patchingSteps'])
                and outcome['register_analysis']['tested'] > 0 and outcome['register_analysis']['failed'] == 0,
                'Invalid desktop validation result')
        require(outcome.get('dex_integrity_entries', 0) > 0 and outcome.get('native_abis') == ['arm64-v8a'],
                'Missing DEX integrity or architecture verification')
        require(isinstance(outcome.get('burst_capture_abi'), str) and outcome['burst_capture_abi'].startswith('PASS '),
                'Missing actual APK burst-capture ABI verification')
    require(qa['artifacts'] == evidence['artifacts'], 'QA binary mismatch')
    qa.update(status='REBUILD_MATCHES_ORIGINAL_APKS_VALIDATED_MPP_DEVICE_UNVERIFIED',
              original_apk_apply_tested=True, device_tested=False, device_quality_verified=False,
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              burst_dex_verification=evidence['burst_dex_verification'],
              stock_burst_capture_abi_verification=evidence['stock_burst_capture_abi_verification'],
              desktop_evidence_sha256=sha(args.validation.read_bytes()))
    if 'stock_lifecycle_abi_verification' in evidence:
        qa['stock_lifecycle_abi_verification'] = evidence['stock_lifecycle_abi_verification']
    sources = {}
    for path in sorted(args.source.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(args.source)
        if eligible_source(path, relative):
            sources['src/' + relative.as_posix()] = path.read_bytes()
    require(all('src/' + name in sources for name in REQUIRED_TOP_SOURCES), 'Incomplete top-level source package')
    require(any('/capture-stubs/' in name for name in sources), 'Missing compilation stubs')
    require(any(name.startswith('src/tests/') for name in sources), 'Missing kernel regression test sources')
    # The build manifest deliberately excludes release artifact fingerprints, so
    # the source ZIP does not recursively contain its own hash. The full reviewed
    # publication manifest remains committed in the release's repository folder.
    build_inputs = {'schema': 'ulike1933-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS}}
    sources['manifest.json'] = (json.dumps(build_inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    required_reports = ('QA_ULike_v1.9.33.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv',
                        'burst-dex-verification.txt', 'burst-inventory.json',
                        'helper-references.txt', 'host-burst1933-result.json')
    for name in required_reports:
        package[name] = (args.dist / name).read_bytes()
    for pattern in ('host-burst*.txt', 'host-burst*.json'):
        for path in args.dist.glob(pattern):
            package[path.name] = path.read_bytes()
    package['evidence/validation.json'] = args.validation.read_bytes()
    evidence_root = args.source.parent / 'evidence'
    for path in sorted(evidence_root.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(evidence_root)
        if any(part in ('tmp', 'classes') for part in relative.parts):
            continue
        if (path.suffix in ('.txt', '.tsv') and path.name != 'apply.log') or relative.as_posix() == 'fast-kernel-benchmark.json':
            package['evidence/' + relative.as_posix()] = path.read_bytes()
    require(not any(Path(name).suffix.casefold() in ('.apk', '.apks', '.aab', '.jks', '.keystore') for name in package),
            'Application binary or signing material in public source package')
    write_zip(args.dist / 'ULike_v1.9.33_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.33.json', 'ULike_v1.9.33_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    print('PASS release files bound to exact original-APKS-tested MPPs, full source inventory and host evidence; device untested')


if __name__ == '__main__':
    main()
