#!/usr/bin/env python3
"""Bind rebuilt ULike quality MPPs to exact desktop validation and package reproducible sources."""
from pathlib import Path
import argparse
import json
from build1937 import require, sha, write_zip, SINGLE, bundle_name, config, ROOT

REQUIRED_TOP_SOURCES = {
    'PreviewLayout1922.java', 'LayoutLifecycle1937.java', 'LayoutHooks1937.java', 'LayoutGeometry1937.java', 'GeometryHooks1937.java', 'production1937.json',
    'Transform1937.java', 'Hooks1937.java', 'PatchClass1937.java', 'VerifyLayout1937.java', 'MergePayloads.java',
    'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java',
    'VerifyFrontAbi1931.java', 'VerifyFrontAbi1936.java', 'VerifyBurstAbi1933.java', 'VerifySaveAbi1935.java', 'VerifyLensAbi1936.java', 'VerifyLayoutAbi1937.java', 'VerifyGeometryAbi1937.java', 'VerifyLayoutLifecycleAbi1937.java',
    'build1937.py', 'validate1937.py', 'finalize1937.py', 'review_release1937.py', 'host_regression1937.py', 'host_layout1937.py', 'host_layout_lifecycle1937.py', 'host_geometry1937.py',
}
BUILD_INPUT_KEYS = (
    'ulike_version', 'bundle_version', 'baseline_ulike_version', 'baseline_ulike_sha256',
    'baseline_ulike_bytes', 'baseline_ulike_url', 'baseline_bundle_version',
    'baseline_bundle_sha256', 'baseline_bundle_bytes', 'baseline_bundle_url', 'toolchain_sha256',
    'jdk_runtime_version', 'jdk_vendor', 'native_library_sha256', 'native_library_bytes', 'ndk_revision', 'native_seed_sha256',
)


def release_notes(qa):
    return f'''ULike v1.9.37 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【修正内容】
・起動中にマスクのアニメーションが完了せず、プレビュー表示範囲が狭いまま残る経路を修正しました。
・古い画面や比率の遅延通知が、現在の表示範囲を上書きする経路を修正しました。
・現在の表示領域の実測サイズを使い、同じ比率でも表示矩形や描画先が変わった場合に更新を届けます。
保存写真の画質や解像度を変更する処理ではありません。

【保持する改造】
ULike1.9.36、総合版{qa['base_versions'][1]}を基準にしています。
前版のインカメラ再起動と倍率表示の修正を保持します。
画質処理、解像度、合成枚数、ノイズ除去、美顔・HEIF保存品質、S2・S3・S4・S5・S6・S7・S8・S16の高速化、T1の保存中撮影受付と他アプリの改造を保持します。
画素処理・保存処理・ネイティブ高速化ライブラリは前版と同じコードです。
撤回済み10bit系の処理、削除済み診断表示や原本保管機能は復活させません。

【検証範囲】
ホスト回帰テストは{len(qa['host_quality_result']['suites'])}群・{qa['host_quality_result']['assertions']:,}アサーションで合格しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、arm64-v8a専用出力、全DEX整合性、組込メソッド一致、型解析と反射先ABIを検証しました。
公開ビルドは固定入力・ツールチェーンで再構築し、元APKSへの適用検証済みMPPとのSHA-256一致を確認します。
Galaxy実機での起動・画面表示・撮影は未確認です。配布物に元APKS、再構築APKや署名鍵は含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。
単体と総合はどちらか一方を使用します。ソース更新だけではインストール済みULikeは変わりません。アプリデータ削除は不要です。
'''


def eligible_source(path, relative):
    if '__pycache__' in relative.parts:
        return False
    if len(relative.parts) == 1:
        return path.name in REQUIRED_TOP_SOURCES or ('1937' in path.name and path.suffix in ('.java', '.py', '.json', '.dex', '.tsv', '.txt'))
    return relative.parts[0].startswith(('layout1937-', 'layout-lifecycle1937-', 'geometry1937-')) and path.suffix == '.java'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    args = parser.parse_args()
    reviewed = config(args.manifest)
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1937-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APKS application required')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No hardware quality claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle differs from manifest')
    qa_path = args.dist / 'QA_ULike_v1.9.37.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1937-layout-v1' and qa.get('host_quality_passed') is True
            and qa.get('host_quality_result', {}).get('status') == 'passed',
            'Missing successful quality host validation')
    require(type(qa['host_quality_result'].get('assertions')) is int and qa['host_quality_result']['assertions'] > 0,
            'Missing executed host quality assertions')
    require(qa.get('camera_startup_recovery_byte_identical') is True and qa.get('front_lens_fixes_byte_identical') is True and qa.get('image_pipeline_byte_identical') is True
            and qa.get('speed_and_save_helpers_byte_identical') is True
            and qa.get('native_library_byte_identical') is True and qa.get('native_installer_byte_identical') is True,
            'Only reviewed preview/UI behavior may change')
    require(qa.get('selected_candidates') == ['preview_startup_layout'], 'Selected candidates differ')
    production_names = json.loads((args.source / 'production1937.json').read_text())
    require(qa.get('production_source_consistency_verified') is True
            and qa.get('compiled_production_source_sha256') == {name + '.java': sha((args.source / (name + '.java')).read_bytes()) for name in production_names},
            'Final production sources differ from the code compiled and host-verified for these MPPs')
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
        require(isinstance(outcome.get('lens_native_abi'), str) and 'PASS lens direct native/helper ABI:' in outcome['lens_native_abi'],
                'Missing actual APK lens switch ABI verification')
        require(isinstance(outcome.get('layout_native_abi'), str) and 'PASS layout native/direct ABI checks=' in outcome['layout_native_abi'],
                'Missing actual APK layout ABI verification')
        require(isinstance(outcome.get('geometry_native_abi'), str) and outcome['geometry_native_abi'].startswith('PASS geometry native ABI '),
                'Missing actual APK measured-geometry ABI verification')
        require(isinstance(outcome.get('layout_lifecycle_abi'), str) and 'PASS layout lifecycle native/helper ABI and six callback hooks checks=' in outcome['layout_lifecycle_abi'],
                'Missing actual APK delayed-layout ABI verification')
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
    require(any('/layout1937-stubs/' in name for name in sources) and any('/geometry1937-compile/' in name for name in sources), 'Missing compilation stubs')
    require(any('/layout1937-host/' in name for name in sources) and any('/layout-lifecycle1937-host/' in name for name in sources) and any('/geometry1937-host/' in name for name in sources), 'Missing regression fixtures')
    # The build manifest deliberately excludes release artifact fingerprints, so
    # the source ZIP does not recursively contain its own hash. The full reviewed
    # publication manifest remains committed in the release's repository folder.
    build_inputs = {'schema': 'ulike1937-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS if key in reviewed}}
    sources['manifest.json'] = (json.dumps(build_inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    required_reports = ('QA_ULike_v1.9.37.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv',
                        'layout-dex-verification.txt', 'layout-inventory.json',
                        'helper-references.txt', 'host-regression1937-result.json', 'native-preservation1937.json')
    for name in required_reports:
        report = (args.dist / name).read_bytes()
        if name == 'helper-references.txt':
            require(report.startswith(b'PASS helper references in ') and len(report.strip()) > 25,
                    'Missing executed helper-reference verification report')
        package[name] = report
    for pattern in ('host-layout*.txt', 'host-geometry*.txt', 'host-regression*.json'):
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
    write_zip(args.dist / 'ULike_v1.9.37_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.37.json', 'ULike_v1.9.37_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    print('PASS release files bound to exact original-APKS-tested MPPs, full source inventory and host evidence; device untested')


if __name__ == '__main__':
    main()
