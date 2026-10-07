#!/usr/bin/env python3
"""Bind rebuilt ULike quality MPPs to exact desktop validation and package reproducible sources."""
from pathlib import Path
import argparse
import json
from build1934 import require, sha, write_zip, SINGLE, bundle_name, config, ROOT

REQUIRED_TOP_SOURCES = {
    'BurstCapture1933.java', 'CapturePolicy1933.java', 'FusionPixels1933.java',
    'FastPixels1933.java', 'FastResize1933.java', 'production1934.json',
    'FaceRegions1934.java', 'FaceRegions1934Pixels.java', 'SpatialNoise1934.java', 'LongMoire1934.java',
    'YuvPlanes1934.java', 'YuvHooks1934.java', 'host_q1_1934.py', 'host_spatial1934.py',
    'host_faces1934.py', 'host_yuv_geometry1934.py', 'host_scheduler1934.py',
    'Q11_Q13_AUDIT.md', 'face-sdk-audit1934.txt',
    'Transform1934.java', 'Hooks1934.java', 'PatchClass1934.java', 'VerifyQuality1934.java', 'MergePayloads.java',
    'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java', 'VerifyBurstAbi1933.java',
    'build1934.py', 'validate1934.py', 'finalize1934.py', 'host_quality1934.py',
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
)


def release_notes(qa):
    return f'''ULike v1.9.34 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【採用した改造】
今回の画質候補Q1・Q4・Q6・Q7・Q11・Q13を採用しました。
Q1：既に取得した同じ露出の画像の中から、ノイズ量を考慮した輪郭の鮮明さを複数の領域で比較し、合成基準を選びます。露出・色・動きや鮮明さの差を十分に確認できない場合は最初の画像を使います。基準を変更した場合は撮影情報も選んだ画像に合わせます。撮影枚数は増やしません。
Q4：保存する画像そのものから検出した両目の位置・間隔・顔の傾きを使い、頬や額の肌補正候補と、目・眉・口・輪郭を保護する範囲を作ります。細かな模様や肌色ではない画素をさらに除外し、回転・切り抜きに合わせて範囲を変換します。検出が不確か、顔が重なる、横向きが強い場合などは顔専用の追加補正を控えます。これは保守的な形状推定による第一段階で、唇の正確な輪郭や髪の領域を画素単位で識別する処理は未実装です。
Q6：元画像の複数箇所でノイズを測り、場所ごとにNRの強さを調整します。暗部の追加補正にも同じ位置の情報を使い、設定した強さを上限に、ノイズが少ない部分の細部を残す方向に調整します。NRオフは維持します。
Q7：従来の短周期の判定に加え、6・8・12・16画素周期の色モアレ候補を調べます。繰り返しと明暗の変化を確認し、条件を満たす低彩度の偽色候補に限定して補正します。本物の色模様と偽色を完全に区別する機能ではなく、すべての色むらの解消を保証するものではありません。
Q11：YUVの3つの実データ面と各行・画素の間隔を使い、色差をV・Uの順に正しく並べます。単枚撮影に残っていた、V面にUも連続して入っていると仮定する処理を修正し、単枚と複数枚で共通化しました。元データの値とバッファ位置を保持します。RGB変換行列や明るさの範囲を推測して変更する処理ではありません。
Q13：保存時の回転・中央切り抜き・拡縮をまとめる既存経路を維持し、90度単位の回転と整数位置の切り抜きだけで済む条件では、画素の直接移動に切り替えます。この条件では再補間によるぼけを加えません。拡縮が必要な場合は既存の一回の補間を使います。美顔SDKの顔変形を別処理に置き換えるものではありません。

【基準と保持する改造】
ULike1.9.33、総合版{qa['base_versions'][1]}が基準です。承認済み1.8.8系統を継承します。
既存の複数枚ノイズ低減と動きに応じた暗所撮影、美顔・HEIF保存、NR・シャープのON/OFFと強度設定を保持します。
前後カメラの記憶、プレビュー復帰、黒帯ダブルタップ、インカメラ時の倍率・接写ボタン非表示の既存修正を保持します。総合版内の他アプリの改造を保持します。
撤回済み1.9.17系の10bit処理、削除済みの診断表示や原本保管機能は復活させません。

【検証と範囲】
ホスト回帰テストは{len(qa['host_quality_result']['suites'])}群・{qa['host_quality_result']['assertions']:,}アサーションで合格しました。DEX変更範囲、カメラ起動・復帰処理と他アプリの保持も検証しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、arm64-v8a専用出力、全DEXの整合性、組込メソッドの一致、追加コードと変更対象の型解析を実施しました。
公開ビルドは固定した入力・ツールチェーンから再構築し、元APKSへの適用検証を行ったMPPとSHA-256が一致することを条件にします。
Galaxy実機での撮影・合成・美顔・保存動作、見た目の改善量、撮影・保存時間は未確認です。
配布物に未改造APKS、再構築APK、ユーザーの写真や署名鍵は含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。単体と総合はどちらか一方を使用します。
ソース更新だけでは、インストール済みULikeの処理は変わりません。アプリデータ削除は不要です。
'''


def eligible_source(path, relative):
    if '__pycache__' in relative.parts:
        return False
    if len(relative.parts) == 1:
        return path.suffix in ('.java', '.py') or path.name in ('production1934.json', 'Q11_Q13_AUDIT.md', 'face-sdk-audit1934.txt') or (path.name.endswith(('1933.json', '1934.json')) and 'hooks' in path.name) or (path.name.startswith('stock-seed') and path.suffix == '.dex') or path.name.endswith('hashes.tsv')
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
    require(evidence.get('schema') == 'ulike1934-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APKS application required')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No hardware quality claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle differs from manifest')
    qa_path = args.dist / 'QA_ULike_v1.9.34.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1934-quality-v1' and qa.get('host_quality_passed') is True
            and qa.get('host_quality_result', {}).get('status') == 'passed',
            'Missing successful quality host validation')
    require(type(qa['host_quality_result'].get('assertions')) is int and qa['host_quality_result']['assertions'] > 0,
            'Missing executed host quality assertions')
    require(qa.get('camera_startup_recovery_byte_identical') is True,
            'Protected camera startup/recovery code must be preserved')
    require(qa.get('selected_candidates') == ['Q1', 'Q4', 'Q6', 'Q7', 'Q11', 'Q13'], 'Selected candidates differ')
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
              quality_dex_verification=evidence['quality_dex_verification'],
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
    build_inputs = {'schema': 'ulike1934-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS}}
    sources['manifest.json'] = (json.dumps(build_inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    required_reports = ('QA_ULike_v1.9.34.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv',
                        'quality-dex-verification.txt', 'quality-inventory.json',
                        'helper-references.txt', 'host-quality1934-result.json')
    for name in required_reports:
        report = (args.dist / name).read_bytes()
        if name == 'helper-references.txt':
            require(report.startswith(b'PASS helper references in ') and len(report.strip()) > 25,
                    'Missing executed helper-reference verification report')
        package[name] = report
    for pattern in ('host-burst*.txt', 'host-burst*.json', 'host-quality*.txt', 'host-quality*.json'):
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
        if (path.suffix in ('.txt', '.tsv') and path.name != 'apply.log'):
            package['evidence/' + relative.as_posix()] = path.read_bytes()
    require(not any(Path(name).suffix.casefold() in ('.apk', '.apks', '.aab', '.jks', '.keystore') for name in package),
            'Application binary or signing material in public source package')
    write_zip(args.dist / 'ULike_v1.9.34_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.34.json', 'ULike_v1.9.34_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    print('PASS release files bound to exact original-APKS-tested MPPs, full source inventory and host evidence; device untested')


if __name__ == '__main__':
    main()
