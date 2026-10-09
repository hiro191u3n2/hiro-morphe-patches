#!/usr/bin/env python3
"""Bind rebuilt ULike quality MPPs to exact desktop validation and package reproducible sources."""
from pathlib import Path
import argparse
import json
from build1941 import require, sha, write_zip, SINGLE, SELECTED, bundle_name, config, ROOT

REQUIRED_TOP_SOURCES = {
    'production1941.json', 'Transform1941.java', 'Hooks1941.java', 'FeedbackHooks1941.java',
    'PatchClass1941.java', 'Verify1941.java', 'MergePayloads.java',
    'VerifyFeedbackAbi1941.java', 'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java', 
    'VerifyFrontAbi1931.java', 'VerifyFrontAbi1938.java', 'VerifyRenderAbi1938.java', 'RenderHooks1938.java', 'VerifyBurstAbi1933.java',
    'VerifySaveAbi1935.java', 'VerifyLensAbi1936.java', 'VerifyLayoutAbi1937.java', 'VerifyGeometryAbi1937.java',
    'VerifyLayoutLifecycleAbi1937.java', 'GeometryHooks1937.java', 'LayoutHooks1937.java',
    'build1941.py', 'validate1941.py', 'finalize1941.py', 'host_regression1941.py',
}

BUILD_INPUT_KEYS = (
    'ulike_version', 'bundle_version', 'baseline_ulike_version', 'baseline_ulike_sha256',
    'baseline_ulike_bytes', 'baseline_ulike_url', 'baseline_bundle_version',
    'baseline_bundle_sha256', 'baseline_bundle_bytes', 'baseline_bundle_url', 'toolchain_sha256',
    'jdk_runtime_version', 'jdk_vendor', 'native_library_sha256', 'native_library_bytes', 'ndk_revision', 'native_seed_sha256',
)


def release_notes(qa):
    input_label = '未改造ULike5.6.2（740）APKS' if qa.get('original_split_merge_tested') else '未改造ULike5.6.2（740）の元base APK'
    split_note = '' if qa.get('original_split_merge_tested') else '元APKSの分割APK・ネイティブライブラリ結合は今回未検証です。\n'
    return f'''ULike v1.9.41 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【修正内容】
・壁などの平坦な部分を優先してノイズを低減し、文字・布目・輪郭を保護します。
・細かな粒とやや大きな粒状ノイズを別々に処理します。
・平坦部や暗部の粒をシャープ処理で再強調しないよう調整します。
・既存の最終保存経路と二重処理防止を確認し、回転・拡縮後の位置に合わせてノイズ分布の参照を修正します。
・残留ノイズ処理へ撮影ごとの処理計画と画像上の位置を引き継ぎ、各保存経路で適用します。
・撮影受付時の一瞬の白点滅を、白枠と濃い縁、薄い暗転を約320ms表示する処理へ変更します。表示は操作を遮らず、保存画像へ入りません。

【保持する処理】
ULike1.9.38／総合版1.0.171を基準にしています。
保存解像度・形式・圧縮設定・合成枚数、美顔・色処理、起動復旧・倍率表示・撮影範囲、保存高速化と他アプリの改造を保持します。

【検証範囲】
ホスト回帰テスト{qa['host_quality_result']['assertions']:,}アサーションが合格しました。
単体・総合版を{input_label}へ適用し、APK再構築、arm64-v8a出力、DEX整合性・組込メソッド一致・型解析・実APK ABIを検証しました。
{split_note}
Galaxy実機でのノイズ改善・撮影表示・速度は未確認です。
元APKS、再構築APK、写真、署名鍵は配布物に含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。
ソース更新だけではインストール済みULikeは変わりません。単体と総合はどちらか一方を使用します。アプリデータ削除は不要です。
'''


def eligible_source(path, relative):
    if any(part in ('__pycache__', 'tmp', 'build', 'dist', 'classes') for part in relative.parts):
        return False
    return path.suffix in ('.java', '.py', '.json', '.dex', '.tsv', '.txt')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    args = parser.parse_args()
    reviewed = config(args.manifest)
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1941-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APK application required')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No hardware quality claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle differs from manifest')
    qa_path = args.dist / 'QA_ULike_v1.9.41.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1941-noise-v1' and qa.get('host_quality_passed') is True
            and qa.get('host_quality_result', {}).get('status') == 'passed',
            'Missing successful quality host validation')
    require(type(qa['host_quality_result'].get('assertions')) is int and qa['host_quality_result']['assertions'] > 0,
            'Missing executed host quality assertions')
    require(qa.get('camera_startup_recovery_byte_identical') is True and qa.get('front_lens_fixes_byte_identical') is True and qa.get('image_pipeline_byte_identical') is False and qa.get('quality_pixel_math_byte_identical') is False and qa.get('manual_capture_safety_checks_preserved') is True
            and qa.get('speed_and_save_helpers_byte_identical') is True
            and qa.get('native_library_byte_identical') is True and qa.get('native_installer_byte_identical') is True,
            'Only reviewed quality helpers and accepted-shutter visual feedback may change')
    require(qa.get('viewport_layout_byte_identical') is True, 'Existing viewport geometry must remain unchanged')
    require(qa.get('black_bar_gesture_byte_identical') is True and qa.get('resolution_and_save_format_preserved') is True, 'Retain .38 gesture, output resolution and save-format policy')
    require(qa.get('selected_candidates') == SELECTED, 'Selected candidates differ')
    production_names = json.loads((args.source / 'production1941.json').read_text())
    REQUIRED_TOP_SOURCES.update(name + '.java' for name in production_names)
    require(qa.get('production_source_consistency_verified') is True
            and qa.get('compiled_production_source_sha256') == {name + '.java': sha((args.source / (name + '.java')).read_bytes()) for name in production_names},
            'Final production sources differ from the code compiled and host-verified for these MPPs')
    combined = bundle_name(qa)
    for name in (SINGLE, combined):
        path = args.dist / name
        require(evidence['artifacts'][name] == {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size},
                'Rebuilt MPP differs from original-APK-tested bytes: ' + name)
    require(set(evidence.get('desktop_validation', {})) == {'single', 'bundle'}, 'Both APK validations are required')
    for kind in ('single', 'bundle'):
        outcome = evidence['desktop_validation'][kind]
        require(not outcome['patch_result']['failedPatches'] and all(x['success'] for x in outcome['patch_result']['patchingSteps'])
                and outcome['register_analysis']['tested'] > 0 and outcome['register_analysis']['failed'] == 0,
                'Invalid desktop validation result')
        require(outcome.get('dex_integrity_entries', 0) > 0 and outcome.get('native_abis') == ['arm64-v8a'],
                'Missing DEX integrity or architecture verification')
        require(isinstance(outcome.get('render_lifecycle_abi'), str) and outcome['render_lifecycle_abi'].startswith('PASS render1938 native ABI/lifecycle checks='),
                'Missing actual APK renderer lifecycle and reflection ABI verification')
        require(isinstance(outcome.get('front_lifecycle_abi'), str) and outcome['front_lifecycle_abi'].startswith('PASS front1938 native ABI/lifecycle checks='),
                'Missing actual APK front startup native and detached-renewal guard verification')
        require(isinstance(outcome.get('burst_capture_abi'), str) and outcome['burst_capture_abi'].startswith('PASS '),
                'Missing actual APK burst-capture ABI verification')
        require(isinstance(outcome.get('feedback_native_abi'), str) and outcome['feedback_native_abi'].startswith('PASS feedback1941 native ABI '), 'Missing actual APK native feedback/gesture and drawable-overlay verification')
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
              original_input_format=evidence['original_input_format'], original_split_merge_tested=evidence['original_split_merge_tested'],
              original_input_sha256=evidence['original_input_sha256'], original_base_apk_sha256=evidence['original_base_apk_sha256'],
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              quality_dex_verification=evidence['quality_dex_verification'],
              stock_burst_capture_abi_verification=evidence['stock_burst_capture_abi_verification'],
              stock_feedback_abi_verification=evidence['stock_feedback_abi_verification'], stock_render_lifecycle_abi_verification=evidence['stock_render_lifecycle_abi_verification'],
              desktop_evidence_sha256=sha(args.validation.read_bytes()))
    if 'stock_lifecycle_abi_verification' in evidence:
        qa['stock_lifecycle_abi_verification'] = evidence['stock_lifecycle_abi_verification']
    if not evidence['original_split_merge_tested']:
        qa['limitations'].append('Only the exact original base APK was used; original split/native-library merging is not tested.')
    sources = {}
    for path in sorted(args.source.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(args.source)
        if eligible_source(path, relative):
            sources['src/' + relative.as_posix()] = path.read_bytes()
    require(all('src/' + name in sources for name in REQUIRED_TOP_SOURCES), 'Incomplete top-level source package')
    require(any('/quality-stubs/' in name for name in sources), 'Missing compilation stubs')
    # The build manifest deliberately excludes release artifact fingerprints, so
    # the source ZIP does not recursively contain its own hash. The full reviewed
    # publication manifest remains committed in the release's repository folder.
    build_inputs = {'schema': 'ulike1941-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS if key in reviewed}}
    sources['manifest.json'] = (json.dumps(build_inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    required_reports = ('QA_ULike_v1.9.41.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv',
                        'noise-dex-verification.txt', 'noise-inventory.json',
                        'helper-references.txt', 'host-regression1941-result.json', 'native-preservation1941.json')
    for name in required_reports:
        report = (args.dist / name).read_bytes()
        if name == 'helper-references.txt':
            require(report.startswith(b'PASS helper references in ') and len(report.strip()) > 25,
                    'Missing executed helper-reference verification report')
        package[name] = report
    for pattern in ('host-*.txt', 'host-regression*.json'):
        for path in args.dist.glob(pattern):
            package[path.name] = path.read_bytes()
    host_evidence = args.dist / 'host-evidence'
    if host_evidence.is_dir():
        for path in sorted(host_evidence.rglob('*')):
            if path.is_file() and path.suffix in ('.txt', '.log', '.json'):
                package['host-evidence/' + path.relative_to(host_evidence).as_posix()] = path.read_bytes()
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
    write_zip(args.dist / 'ULike_v1.9.41_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.41.json', 'ULike_v1.9.41_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    print('PASS release files bound to exact original-APKS-tested MPPs, full source inventory and host evidence; device untested')


if __name__ == '__main__':
    main()
