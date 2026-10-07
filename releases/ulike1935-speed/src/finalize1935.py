#!/usr/bin/env python3
"""Bind rebuilt ULike quality MPPs to exact desktop validation and package reproducible sources."""
from pathlib import Path
import argparse
import json
from build1935 import require, sha, write_zip, SINGLE, bundle_name, config, ROOT

REQUIRED_TOP_SOURCES = {
    'BurstCapture1933.java', 'CapturePolicy1933.java', 'FusionPixels1933.java',
    'FastPixels1933.java', 'FastResize1933.java', 'production1935.json',
    'FaceRegions1934.java', 'FaceRegions1934Pixels.java', 'SpatialNoise1934.java', 'LongMoire1934.java',
    'YuvPlanes1934.java', 'YuvHooks1934.java', 'PatchNativeLoader1935.java', 'NativeSpeed1935.java', 'host_q1_1934.py', 'host_spatial1934.py',
    'host_faces1934.py', 'host_yuv_geometry1934.py', 'host_scheduler1934.py',
    'Q11_Q13_AUDIT.md', 'face-sdk-audit1934.txt',
    'Transform1935.java', 'Hooks1935.java', 'SaveHooks1935.java', 'SeedSave1935.java',
    'extract_seed1935.py', 'stock-seed1935.dex', 'stock-seed1935-hashes.tsv', 'PatchClass1935.java', 'VerifySpeed1935.java', 'MergePayloads.java',
    'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java', 'VerifyBurstAbi1933.java', 'VerifySaveAbi1935.java',
    'build1935.py', 'validate1935.py', 'finalize1935.py', 'review_release1935.py', 'host_speed1935.py',
    'host_capture1933.py', 'host_fusion1933.py', 'host_fast1933.py', 'host_metadata1933.py',
    'host_integration1933.py',
    'SeedBurst1933.java', 'extract_seed1933.py', 'stock-seed1933-capture.dex',
    'stock-seed1933-capture-hashes.tsv',
}
BUILD_INPUT_KEYS = (
    'ulike_version', 'bundle_version', 'baseline_ulike_version', 'baseline_ulike_sha256',
    'baseline_ulike_bytes', 'baseline_ulike_url', 'baseline_bundle_version',
    'baseline_bundle_sha256', 'baseline_bundle_bytes', 'baseline_bundle_url', 'toolchain_sha256',
    'jdk_runtime_version', 'jdk_vendor', 'native_library_sha256', 'native_library_bytes', 'ndk_revision', 'native_seed_sha256',
)


def release_notes(qa):
    return f'''ULike v1.9.35 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【採用した改造】
S2・S3・S4・S5・S6・S7・S8・S16とT1を採用しました。
S2：同じ撮影画像・同じ条件の動き判定と合成解析を共有し、重複計算を減らします。
S3・S4・S5：画素の計算順序と丸めを維持し、画像のまとめ書き、縮小率に対応する行キャッシュ、不透明画像の重複走査省略で処理量を減らします。
S6：作業用メモリーとワーカーを再利用します。同時処理の所有権を分離し、前の写真のデータを次の写真へ混入させないようにします。
S7：YUV取り出しの連続部分を一括コピーします。面ごとの間隔、バッファ位置、V・Uの並びと元の画素値を維持します。
S8：YUV転送と一部のリサイズ演算をarm64ネイティブ処理へ移します。乗算・加算の順序と単精度の丸めを維持し、既存Java処理との画素一致を検証します。ネイティブが使用できない場合はJava処理を使用します。
S16：設定がOFFまたは強度0の場合に、最終画素へ影響しない準備処理を省きます。有効な補正を自動的に弱める処理ではありません。
T1：保存処理と次の撮影受付を分離します。処理待ちを無制限に蓄積せず、画像と設定を撮影単位で保持します。保存そのものの完了時間が短くなることを保証する変更ではありません。

【基準と保持する改造】
ULike1.9.34、総合版{qa['base_versions'][1]}が基準です。承認済み1.8.8系統を継承します。
解像度、撮影・合成枚数、ノイズ除去・シャープの設定、既存の美顔・HEIF保存品質を高速化目的で下げません。
前後カメラの記憶、プレビュー復帰、黒帯ダブルタップ、インカメラ時の倍率・接写ボタン非表示と他アプリの改造を保持します。
Q4の顔部位保護は従来の保守的な形状推定を継承します。髪・唇の精密な領域分割は未実装です。
撤回済み1.9.17系の10bit処理、削除済み診断表示や原本保管機能は復活させません。

【検証と範囲】
ホスト回帰テストは{len(qa['host_quality_result']['suites'])}群・{qa['host_quality_result']['assertions']:,}アサーションで合格しました。画素一致、状態分離、DEX変更範囲、カメラ起動・復帰処理と他アプリの保持を検証しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、arm64-v8a専用出力、ネイティブライブラリの組込み、全DEXの整合性、組込メソッド一致、追加コードと変更対象の型解析を実施しました。
公開ビルドは固定入力とツールチェーンから再構築し、元APKSへの適用検証を行ったMPPとSHA-256が一致することを条件にします。
Galaxy実機での撮影・保存、連続受付、見た目の差、撮影・保存時間は未確認です。
配布物に元APKS、再構築APK、ユーザー写真や署名鍵は含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用し、作成されたアプリを更新インストールしてください。単体と総合はどちらか一方を使用します。
ソース更新だけではインストール済みULikeの処理は変わりません。アプリデータ削除は不要です。
'''


def eligible_source(path, relative):
    if '__pycache__' in relative.parts:
        return False
    if len(relative.parts) == 1:
        return path.suffix in ('.java', '.py') or path.name in ('production1935.json', 'production1934.json', 'Q11_Q13_AUDIT.md', 'face-sdk-audit1934.txt') or (path.name.endswith(('1933.json', '1934.json', '1935.json')) and 'hooks' in path.name) or (path.name.startswith('stock-seed') and path.suffix == '.dex') or path.name.endswith('hashes.tsv')
    folder = relative.parts[0]
    if folder == 'native1935':
        return path.suffix in ('.c', '.h', '.cpp', '.py', '.json', '.txt', '.md') or path.name == 'CMakeLists.txt'
    if folder == 'reference1934':
        return path.suffix in ('.java', '.json')
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
    require(evidence.get('schema') == 'ulike1935-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APKS application required')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No hardware quality claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle differs from manifest')
    qa_path = args.dist / 'QA_ULike_v1.9.35.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1935-speed-v1' and qa.get('host_quality_passed') is True
            and qa.get('host_quality_result', {}).get('status') == 'passed',
            'Missing successful quality host validation')
    require(type(qa['host_quality_result'].get('assertions')) is int and qa['host_quality_result']['assertions'] > 0,
            'Missing executed host quality assertions')
    require(qa.get('camera_startup_recovery_byte_identical') is True,
            'Protected camera startup/recovery code must be preserved')
    require(qa.get('selected_candidates') == ['S2', 'S3', 'S4', 'S5', 'S6', 'S7', 'S8', 'S16', 'T1'], 'Selected candidates differ')
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
        require(isinstance(outcome.get('save_native_abi'), str) and 'PASS save native reflection ABI:' in outcome['save_native_abi'],
                'Missing actual APK per-shot save ABI verification')
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
    build_inputs = {'schema': 'ulike1935-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS}}
    sources['manifest.json'] = (json.dumps(build_inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    required_reports = ('QA_ULike_v1.9.35.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv',
                        'speed-dex-verification.txt', 'speed-inventory.json',
                        'helper-references.txt', 'host-speed1935-result.json', 'native-loader-verification.txt', 'native-build1935.json', 'arm64-tests.json')
    for name in required_reports:
        report = (args.dist / name).read_bytes()
        if name == 'helper-references.txt':
            require(report.startswith(b'PASS helper references in ') and len(report.strip()) > 25,
                    'Missing executed helper-reference verification report')
        package[name] = report
    for pattern in ('host-burst*.txt', 'host-burst*.json', 'host-quality*.txt', 'host-quality*.json', 'host-speed*.txt', 'host-speed*.json'):
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
    write_zip(args.dist / 'ULike_v1.9.35_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.35.json', 'ULike_v1.9.35_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    print('PASS release files bound to exact original-APKS-tested MPPs, full source inventory and host evidence; device untested')


if __name__ == '__main__':
    main()
