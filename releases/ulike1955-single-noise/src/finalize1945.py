#!/usr/bin/env python3
"""Bind speed44 release assets to executed host and original-APKS validation."""
from pathlib import Path
import argparse
import json
import zipfile
from build1945 import require, sha, SINGLE, SELECTED, bundle_name, config, ROOT

BUILD_INPUT_KEYS = (
    'ulike_version', 'bundle_version', 'baseline_ulike_version', 'baseline_ulike_sha256',
    'baseline_ulike_bytes', 'baseline_ulike_url', 'baseline_bundle_version',
    'baseline_bundle_sha256', 'baseline_bundle_bytes', 'baseline_bundle_url', 'toolchain_sha256',
    'jdk_runtime_version', 'jdk_vendor', 'production_helper_roots', 'selected_candidates', 'native_library_sha256', 'native_library_bytes',
    'baseline_native_library_sha256', 'baseline_native_library_bytes',
    'ndk_revision', 'native_seed_sha256',
)


def release_notes(qa):
    return f'''ULike v1.9.45 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【採用した高速化】
10・7：残留ノイズ集計をarm64 NEONで複数画素ずつ処理し、画像中央と端の分岐を分離します。整数演算順序・丸め・端の扱いは維持します。
4：局所ノイズ・顔保護の判定結果を同じ画像の補正工程で共有します。画像・撮影設定ごとの所有権を維持します。
20：反射探索で見つけたクラス・メソッド・フィールドを保持します。画像・設定値・通知先は毎回その撮影に対応した値を読み取ります。
29：エンコーダーが空いている場合はHEIF準備を画質補正と重ねます。補正枠とは独立した所有権で、ハードウェアエンコーダーは同時1台を維持します。
32：圧縮済み出力の排出用HandlerThreadを共有します。前のコーデックの終了処理を確認してから次に引き継ぎます。
33：既存のMediaStore保存先へのFD直接書き込みを検証して保持します。この案は既に有効なため、別の一時ファイルコピー削除は追加していません。

【画質と保持する処理】
ULike1.9.44／総合版1.0.177を基準にしています。画素計算・丸め・補正強度を維持する高速化です。
保存解像度・形式・圧縮品質、最大4枚の合成、美顔・色処理、最初の撮影画像を基準にする動作は維持します。
最大3枚の受付、補正とエンコードの直列順序、メモリ上限、撮影ごとの画像・設定・通知先の所有権、既存のカメラ起動・画面・シャッター修正も維持します。他アプリの改造は保持します。

【検証範囲】
ホスト回帰テスト{qa['host_quality_result']['assertions']:,}アサーションが合格しました。
変更前との画素比較、Javaとネイティブ処理の一致、キュー・並行処理・所有権の検証を実施しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、arm64-v8a、DEX整合性・型解析・実APK ABIを検証しました。
Galaxy実機の保存秒数・連写・画質は未確認です。実機での短縮率は未測定です。
元APKS、再構築APK、写真、署名鍵は配布物に含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。
ソース更新だけではインストール済みULikeは変わりません。単体と総合はどちらか一方を使用します。アプリデータ削除は不要です。
'''


def eligible(path, relative):
    return not any(x in ('__pycache__', 'tmp', 'build', 'dist', 'classes') for x in relative.parts) and path.suffix in (
        '.java', '.py', '.json', '.dex', '.tsv', '.txt', '.c', '.h', '.md')


def write_source_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_STORED) as archive:
        for name in sorted(entries):
            info = zipfile.ZipInfo(name, (2026, 10, 8, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            archive.writestr(info, entries[name])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    args = parser.parse_args()
    reviewed = config(args.manifest)
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1945-desktop-validation-v1', 'Wrong original-APKS evidence schema')
    require(evidence.get('original_apk_apply_tested') is True and evidence.get('original_split_merge_tested') is True,
            'Executed original APKS split/native-library application required')
    require(evidence.get('original_apk_application', {}).get('status') == 'passed', 'Original APK application failed')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No physical-device claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle version differs')
    qa_path = args.dist / 'QA_ULike_v1.9.45.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1945-speed-v1' and qa.get('host_quality_passed') is True
            and qa.get('host_quality_result', {}).get('status') == 'passed', 'Missing executed host validation')
    require(type(qa['host_quality_result'].get('assertions')) is int and qa['host_quality_result']['assertions'] > 0,
            'Executed positive assertion count required')
    require(qa.get('selected_candidates') == SELECTED, 'Selected candidates differ from user instruction')
    require(qa.get('resolution_and_save_format_preserved') is True and qa.get('manual_capture_safety_checks_preserved') is True,
            'Output quality and capture safeguards must be preserved')
    require(qa.get('non_ulike_resources_byte_identical') is True and qa.get('non_ulike_loader_classes_unchanged') is True,
            'Other applications must be retained')
    roots = json.loads((args.source / 'production1945.json').read_text())
    require(qa.get('replaced_helper_roots') == sorted(roots), 'Production replacement roots differ')
    compiled = {name + '.java': sha((args.source / (name + '.java')).read_bytes()) for name in roots}
    require(qa.get('production_source_consistency_verified') is True
            and qa.get('compiled_production_source_sha256') == compiled, 'Final source differs from compiled and tested source')
    transformers = qa.get('compiled_transformer_source_sha256', {})
    require(transformers and transformers == {name: sha((args.source / name).read_bytes()) for name in transformers},
            'DEX transformer source differs from the actual compiled build')
    native_report = qa.get('native_build_report', {})
    require(qa.get('native_source_consistency_verified') is True and native_report.get('sources'),
            'Executed native-source consistency evidence required')
    require(native_report['sources'] == {name: sha((args.source / 'native1944' / name).read_bytes())
                                         for name in native_report['sources']},
            'Native source differs from the actual compiled ARM64 library')
    require(native_report.get('sha256') == qa.get('native_library_sha256')
            and native_report.get('bytes') == qa.get('native_library_bytes'), 'Native artifact report differs')
    for name, suite in qa['host_quality_result'].get('suites', {}).items():
        require(suite.get('status') == 'passed' and type(suite.get('assertions')) is int and suite['assertions'] > 0,
                'Executed host suite failed or empty: ' + name)
    combined = bundle_name(qa)
    for name in (SINGLE, combined):
        path = args.dist / name
        require(evidence['artifacts'].get(name) == {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size},
                'MPP differs from original-APKS-tested bytes: ' + name)
    require(qa['artifacts'] == evidence['artifacts'], 'QA MPP fingerprints differ')
    require(set(evidence.get('desktop_validation', {})) == {'single', 'bundle'}, 'Both original APKS validations required')
    for kind, outcome in evidence['desktop_validation'].items():
        require(not outcome['patch_result']['failedPatches'] and all(x['success'] for x in outcome['patch_result']['patchingSteps']),
                'Patch application failed: ' + kind)
        require(outcome['register_analysis']['tested'] > 0 and outcome['register_analysis']['failed'] == 0,
                'DEX register type analysis failed: ' + kind)
        require(outcome.get('dex_integrity_entries', 0) > 0 and outcome.get('native_abis') == ['arm64-v8a'],
                'DEX integrity / ARM64 output evidence missing')
        for key in ('render_lifecycle_abi', 'front_lifecycle_abi', 'burst_capture_abi', 'feedback_native_abi',
                    'save_native_abi', 'codec_lifecycle_abi', 'lens_native_abi', 'layout_native_abi', 'geometry_native_abi', 'layout_lifecycle_abi'):
            require('PASS ' in outcome.get(key, ''), 'Actual APK ABI verification missing: ' + key)
    qa.update(status='REBUILD_MATCHES_ORIGINAL_APKS_VALIDATED_MPP_DEVICE_UNVERIFIED',
              original_apk_apply_tested=True, device_tested=False, device_quality_verified=False,
              original_apk_application=evidence['original_apk_application'],
              original_input_format=evidence['original_input_format'], original_split_merge_tested=True,
              original_input_sha256=evidence['original_input_sha256'], original_base_apk_sha256=evidence['original_base_apk_sha256'],
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              desktop_evidence_sha256=sha(args.validation.read_bytes()))
    for key, value in evidence.items():
        if key.endswith('_verification'):
            qa[key] = value
    sources = {'src/' + p.relative_to(args.source).as_posix(): p.read_bytes()
               for p in sorted(args.source.rglob('*')) if p.is_file() and eligible(p, p.relative_to(args.source))}
    required = {'src/' + name + '.java' for name in roots} | {
        'src/build1945.py', 'src/validate1945.py', 'src/finalize1945.py', 'src/host_regression1945.py',
        'src/production1945.json', 'src/native1944/residual1944.c', 'src/native1944/build_native1944.py',
        'src/cache1944-reference/QualityShadowReference1943.java', 'src/host_native1945.py', 'src/host_reflection1945.py', 'src/host_policy1945.py', 'src/host_save1945.py'}
    require(required <= sources.keys(), 'Incomplete reproducible source package')
    require(any('/quality-stubs/' in name for name in sources), 'Compile-only stubs missing')
    inputs = {'schema': 'ulike1945-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS if key in reviewed}}
    sources['manifest.json'] = (json.dumps(inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    package['evidence/validation.json'] = args.validation.read_bytes()
    for path in sorted(args.dist.rglob('*')):
        relative = path.relative_to(args.dist)
        if path.is_file() and path.suffix in ('.txt', '.log', '.json', '.tsv') and path.name not in ('SHA256SUMS.txt',):
            package[relative.as_posix()] = path.read_bytes()
    require('helper-references.txt' in package and package['helper-references.txt'].startswith(b'PASS helper references in '),
            'Executed runtime helper reference check missing')
    require(not any(Path(name).suffix.lower() in ('.apk', '.apks', '.aab', '.jks', '.keystore') for name in package),
            'Application binary / signing material cannot be distributed')
    write_source_zip(args.dist / 'ULike_v1.9.45_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.45.json', 'ULike_v1.9.45_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / n).read_bytes()) + '  ' + n + '\n' for n in files))
    print('PASS speed45 assets bound to executed pixel/ownership tests and exact original-APKS applications; device untested')


if __name__ == '__main__':
    main()
