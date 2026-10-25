#!/usr/bin/env python3
"""Finalize reproducible diagnostics assets bound to host tests and original APKS."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import re
import zipfile
from build1947 import (
    require, sha, SINGLE, SELECTED, bundle_name, ROOT, VERSION, BASE_VERSION,
    BUNDLE_VERSION, BASE_BUNDLE_VERSION, BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256,
    TOOL_PINS, PRODUCTION,
)

BASE_COMMIT = '0c8784f707abc10f2d908281b6e12a6fc0a2cb04'
BASE_ROOT = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/' + BASE_COMMIT + '/downloads/'
BASE_SINGLE_NAME = 'ULike_HQ_Texture_Online_v1.9.45.mpp'
BASE_BUNDLE_NAME = 'Hiro_Morphe_Patches_v1.0.178.mpp'
QA_NAME = 'QA_ULike_v1.9.47.json'
SOURCE_ZIP = 'ULike_v1.9.47_sources_and_QA.zip'
NATIVE_SHA256 = '50613d3ed433de1aa3b6c0608753dc98479ac9ce8238033b972cd025b67b297a'
NATIVE_BYTES = 18560
BUILD_INPUTS = {
    'schema': 'ulike1947-build-inputs-v1', 'ulike_version': VERSION,
    'bundle_version': BUNDLE_VERSION, 'baseline_ulike_version': BASE_VERSION,
    'baseline_ulike_sha256': BASE_SINGLE_SHA256, 'baseline_ulike_bytes': 835098,
    'baseline_ulike_url': BASE_ROOT + BASE_SINGLE_NAME,
    'baseline_bundle_version': BASE_BUNDLE_VERSION, 'baseline_bundle_sha256': BASE_BUNDLE_SHA256,
    'baseline_bundle_bytes': 17482157, 'baseline_bundle_url': BASE_ROOT + BASE_BUNDLE_NAME,
    'toolchain_sha256': TOOL_PINS, 'jdk_runtime_version': '21.0.8+9-LTS',
    'jdk_vendor': 'Eclipse Adoptium', 'production_helper_roots': PRODUCTION,
    'selected_candidates': SELECTED, 'native_library_sha256': NATIVE_SHA256,
    'native_library_bytes': NATIVE_BYTES,
}
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx'}


def json_bytes(value, sort_keys=False):
    return (json.dumps(value, ensure_ascii=False, indent=2, sort_keys=sort_keys) + '\n').encode()


def release_notes(qa):
    return f'''ULike v1.9.47 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【処理時間の表示を修正】
旧「追加処理」の保存済み結果が残り続ける表示を修正し、新しい撮影ごとの計測結果を表示します。
合成・ノイズ除去・補正・圧縮・保存の経過時間を個別に表示します。撮影ごとの記録を引き継ぎ、別の撮影の結果と混ざらないようにします。
設定画面を開いた際と結果のタップ更新で現在の結果を読み直します。旧バージョンの記録は現在の計測結果として表示しません。
未実行・未完了・失敗の工程は区別して表示し、計測していない時間を実測 0 ms として扱いません。

【計測時間の読み方】
各工程は処理の前後を単調時計で測った経過時間です。処理待ちや入出力を含む区間もあります。
「補正」は保存する画像の追加処理・変形・リサイズ・ウォーターマーク等を対象にします。ULike標準のネイティブ美顔処理と撮影待機は、工程別計測の対象外です。
「圧縮」はエンコーダーの開始から終了までを測り、事前の準備・初期化を含みません。未計測の工程があるため、この5項目だけで全体の待ち時間を説明できるとは限りません。
合成、ノイズ除去、補正、圧縮、保存の一部は並行・入れ子で実行されます。工程ごとの合計は、計測開始から処理終了までの「全体経過」と一致しません。全体経過を厳密なシャッターボタンから保存完了までの時間として扱わないでください。
圧縮と書き込みが重なる保存方式では、圧縮を含む書き込み区間を独立した「純粋なディスク時間」として解釈しないでください。
この改造は遅い工程を調べるための表示・計測追加です。実機での高速化率は測定していません。

【画質と既存の改造】
ULike v1.9.45／総合版 v1.0.178 を基準にしています。
画素計算・補正強度・保存解像度・形式・圧縮品質・美顔と色処理を維持します。最大4枚の合成と最初の撮影画像を基準にする動作を保持します。
ネイティブライブラリ・その導入処理・既存のネイティブメソッドは基準版と同一です。撮影受付数、処理順序、画像と設定の所有権、カメラ起動・画面・シャッター修正、他アプリの改造も保持します。

【検証範囲】
ホスト回帰テスト {qa['host_quality_result']['assertions']:,} アサーションが合格しました。
撮影ごとの時間記録、古い記録の扱い、重複・失敗・更新動作、および既存の品質処理を検証しました。
単体・総合版を未改造 ULike 5.6.2（740）APKS に適用し、APK再構築、arm64-v8a、DEX整合性・型解析・実APK ABI・現在の処理メソッドの保持を確認しました。
Galaxy実機の時間表示・撮影・保存・画質は未確認です。スマートフォン上の実行結果を保証する検証は行っていません。
元APKS、再構築APK、写真、署名鍵は配布物に含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。
ソース更新だけではインストール済みULikeは変わりません。単体と総合はどちらか一方を使用します。アプリデータ削除は不要です。
'''


def readme(qa):
    return (f'''ULike v1.9.47／Hiro Morphe Patches v{qa['bundle_version']}

撮影ごとの合成・ノイズ除去・補正・圧縮・保存の経過時間を記録し、設定画面で最新の結果を読み直します。古い追加処理の値を現在の結果として表示しません。工程は並行・入れ子で動くため、各時間の合計は全体時間と一致しません。計測不能・未実行・失敗は実測 0 ms と区別します。「補正」は保存画像の追加処理・変形・リサイズ・ウォーターマーク等で、ULike標準のネイティブ美顔処理と撮影待機は工程別計測の対象外です。「圧縮」はエンコーダー開始から終了までで、準備・初期化を含みません。未計測工程があるため、この5項目だけで全体の待ち時間を説明できるとは限りません。「全体経過」は計測開始から処理終了までで、厳密なシャッターボタンから保存完了までの時間ではありません。

基準版は ULike v1.9.45／総合版 v1.0.178 です。画質計算・解像度・保存形式・圧縮設定とネイティブライブラリを維持し、他アプリの改造も保持します。ホスト試験と元 APKS への単体・総合版適用は確認済み、Galaxy 実機は未確認です。詳細は RELEASE_NOTES.txt と QA_ULike_v1.9.47.json を参照してください。

再ビルド例:

    python3 src/build1947.py --input INPUT --tools TOOLS --work BUILD --output DIST
    python3 src/finalize1947.py --dist DIST --source src --validation evidence/validation.json

INPUT には manifest.json の固定 SHA256 に対応する基準の単体・総合 MPP を置きます。TOOLS は固定チェックサムの morphe.jar、android.jar、d8.jar、JDK は Temurin 21.0.8+9-LTS を使います。ネイティブライブラリは基準 MPP から保持し、NDK は不要です。元 APKS の検証結果は両方の MPP の SHA256 と一致するものだけを受け付けます。

Morphe Manager のソース更新後、未改造 ULike 5.6.2（740）へパッチを再適用し、作成されたアプリを更新インストールしてください。ソース更新だけではインストール済みアプリは変わりません。
''').encode()


def eligible(path, relative):
    return (not path.is_symlink()
            and not any(x in ('__pycache__', 'tmp', 'build', 'dist', 'classes') for x in relative.parts)
            and path.suffix in ('.java', '.py', '.json', '.dex', '.tsv', '.txt', '.c', '.h', '.md'))


def safe_package_name(name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and '..' not in p.parts and '\\' not in name,
            'Unsafe source package path: ' + name)
    require(p.suffix.lower() not in PRIVATE_SUFFIXES, 'Private application/signing material: ' + name)


def write_source_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_STORED) as archive:
        for name in sorted(entries):
            safe_package_name(name)
            info = zipfile.ZipInfo(name, (2026, 10, 8, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            archive.writestr(info, entries[name])


def publisher(source):
    path = source.parent / 'publication' / 'publish1947.py'
    spec = importlib.util.spec_from_file_location('diagnostics_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def validate_host(qa, source, evidence):
    require(qa.get('schema') == 'ulike1947-diagnostics-v1'
            and qa.get('ulike_version') == VERSION and qa.get('bundle_version') == BUNDLE_VERSION,
            'Wrong diagnostics build QA identity')
    require(qa.get('host_quality_passed') is True and qa.get('host_quality_result', {}).get('status') == 'passed',
            'Executed host validation required')
    result = qa['host_quality_result']
    require(type(result.get('assertions')) is int and result['assertions'] > 0, 'Positive executed assertion count required')
    for name, suite in result.get('suites', {}).items():
        require(suite.get('status') == 'passed' and type(suite.get('assertions')) is int and suite['assertions'] > 0,
                'Executed host suite failed or empty: ' + name)
    require(qa.get('host_quality_result_sha256') == sha(json.dumps(result, sort_keys=True).encode()),
            'Host result changed after the build')
    require(qa.get('selected_candidates') == SELECTED, 'Reviewed diagnostics scope differs')
    roots = json.loads((source / 'production1947.json').read_text())
    require(roots == PRODUCTION and qa.get('replaced_helper_roots') == sorted(roots),
            'Production root inventory differs')
    compiled = {name + '.java': sha((source / (name + '.java')).read_bytes()) for name in roots}
    require(qa.get('production_source_consistency_verified') is True
            and qa.get('compiled_production_source_sha256') == compiled
            and evidence.get('compiled_production_source_sha256') == compiled,
            'Current production source differs from built/tested/original-APKS-validated source')
    transformers = qa.get('compiled_transformer_source_sha256')
    require(isinstance(transformers, dict) and transformers
            and transformers == {name: sha((source / name).read_bytes()) for name in transformers}
            and evidence.get('compiled_transformer_source_sha256') == transformers,
            'Transformer source differs from built/tested/original-APKS-validated source')
    require(qa.get('native_library_sha256') == NATIVE_SHA256 and qa.get('native_library_bytes') == NATIVE_BYTES,
            'Native45 library fingerprint must remain exact')


def validate_desktop(qa, evidence, dist):
    require(evidence.get('schema') == 'ulike1947-desktop-validation-v1', 'Wrong original-APKS evidence schema')
    require(evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION,
            'Original-APKS validation version differs')
    require(evidence.get('original_apk_apply_tested') is True and evidence.get('original_split_merge_tested') is True,
            'Executed original APKS split/native-library applications required')
    require(evidence.get('original_apk_application', {}).get('status') == 'passed', 'Original APK application failed')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'Physical-device claims are unverified')
    require(evidence.get('runtime_preservation_verification', {}).get('status') == 'passed',
            'Current rebuilt runtime contract preservation verification required')
    for key in ('original_input_sha256', 'original_base_apk_sha256'):
        require(isinstance(evidence.get(key), str) and re.fullmatch('[0-9a-f]{64}', evidence[key]),
                'Missing original application fingerprint: ' + key)
    require(evidence.get('original_input_format') == 'APKS', 'Merged original split APKS evidence required')
    artifacts = evidence.get('artifacts', {})
    require(set(artifacts) == {SINGLE, bundle_name(qa)}, 'Both exact MPP application artifacts are required')
    for name in artifacts:
        data = (dist / name).read_bytes()
        require(artifacts[name] == {'sha256': sha(data), 'bytes': len(data)},
                'MPP differs from original-APKS-tested bytes: ' + name)
    require(qa.get('artifacts') == artifacts, 'QA MPP fingerprints differ from original APKS application')
    require(set(evidence.get('desktop_validation', {})) == {'single', 'bundle'}, 'Both original APKS validations required')
    for kind, outcome in evidence['desktop_validation'].items():
        result = outcome.get('patch_result', {})
        require(not result.get('failedPatches') and result.get('patchingSteps')
                and all(step.get('success') is True for step in result['patchingSteps']),
                'Patch application failed or missing: ' + kind)
        analysis = outcome.get('register_analysis', {})
        require(analysis.get('tested', 0) > 0 and analysis.get('failed') == 0,
                'DEX register type analysis failed: ' + kind)
        require(outcome.get('dex_integrity_entries', 0) > 0 and outcome.get('native_abis') == ['arm64-v8a'],
                'DEX integrity / ARM64 output evidence missing: ' + kind)
        require('PASS ' in outcome.get('method_contracts', ''), 'Current actual APK runtime contracts missing: ' + kind)
        for key in ('render_lifecycle_abi', 'front_lifecycle_abi', 'burst_capture_abi', 'feedback_native_abi',
                    'save_native_abi', 'codec_lifecycle_abi', 'lens_native_abi', 'layout_native_abi',
                    'geometry_native_abi', 'layout_lifecycle_abi'):
            require('PASS ' in outcome.get(key, ''), 'Actual APK ABI verification missing: ' + key)


def finalize(args):
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    qa_path = args.dist / QA_NAME
    qa = json.loads(qa_path.read_text())
    validate_host(qa, args.source, evidence)
    validate_desktop(qa, evidence, args.dist)
    pub = publisher(args.source)
    qa.update(status='REBUILD_MATCHES_ORIGINAL_APKS_VALIDATED_MPP_DEVICE_UNVERIFIED',
              original_apk_apply_tested=True, device_tested=False, device_quality_verified=False,
              original_apk_application=evidence['original_apk_application'],
              original_input_format=evidence['original_input_format'], original_split_merge_tested=True,
              original_input_sha256=evidence['original_input_sha256'],
              original_base_apk_sha256=evidence['original_base_apk_sha256'],
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              desktop_evidence_sha256=sha(evidence_bytes))
    for key, value in evidence.items():
        if key.endswith('_verification'):
            qa[key] = value
    for key, expected in pub.REQUIRED_QA.items():
        actual = pub.lookup_qa(qa, key)
        require(type(actual) is type(expected) and actual == expected, 'Final QA contract is not satisfied: ' + key)
    sources = {'src/' + p.relative_to(args.source).as_posix(): p.read_bytes()
               for p in sorted(args.source.rglob('*')) if p.is_file() and eligible(p, p.relative_to(args.source))}
    required = {'src/' + name + '.java' for name in PRODUCTION} | {
        'src/build1947.py', 'src/validate1947.py', 'src/finalize1947.py',
        'src/host_regression1947.py', 'src/production1947.json',
        'src/Transform1947.java', 'src/TimingHooks1947.java', 'src/VerifyTiming1947.java',
        'src/PatchClass1947.java', 'src/VerifyHelperReferences.java',
    }
    require(required <= sources.keys(), 'Incomplete reproducible diagnostics source package')
    require(any('/quality-stubs/' in name for name in sources)
            and any('/tests/timing1947-fixtures/' in name for name in sources),
            'Actual compile-only stubs or independent timing fixtures missing')
    sources['manifest.json'] = json_bytes(BUILD_INPUTS)
    # Public publisher and the archived exact workflow make publication reviewable.
    for name in ('publish1947.py', 'ulike1947-diagnostics-publish.yml', 'README.md'):
        path = args.source.parent / 'publication' / name
        require(path.is_file(), 'Publication source missing: ' + name)
        sources['publication/' + name] = path.read_bytes()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_bytes(json_bytes(qa, sort_keys=True))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    (args.source.parent / 'README.md').write_bytes(readme(qa))
    package = {**sources, 'README.md': readme(qa), 'evidence/validation.json': evidence_bytes}
    for path in sorted(args.dist.rglob('*')):
        if path.is_file() and path.suffix in ('.txt', '.log', '.json', '.tsv') and path.name not in (
                'SHA256SUMS.txt', 'publication_ULike_v1.9.47.json'):
            package[path.relative_to(args.dist).as_posix()] = path.read_bytes()
    require(package.get('helper-references.txt', b'').startswith(b'PASS helper references in '),
            'Executed runtime helper reference check missing')
    require(package.get('timing-dex-verification.txt', b'').startswith(b'PASS_TIMING1947 '),
            'Executed diagnostics DEX verification missing')
    write_source_zip(args.dist / SOURCE_ZIP, package)
    files = [SINGLE, bundle_name(qa), QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / n).read_bytes()) + '  ' + n + '\n' for n in files))
    contract = dict(pub.REQUIRED_QA)
    inventory_keys = ('changed_runtime_methods', 'changed_native_methods', 'new_helper_classes',
                      'new_runtime_aliases', 'new_native_methods', 'replaced_helper_roots')
    for key in inventory_keys:
        require(key in qa, 'Actual DEX inventory missing: ' + key)
        contract[key] = qa[key]
    contract['host_quality_result.assertions'] = qa['host_quality_result']['assertions']
    contract['original_apk_apply_tested'] = True
    manifest = {**BUILD_INPUTS, 'schema': 'ulike1947-publication-v1',
                'android_device_tested': False, 'original_apk_apply_tested': True,
                'change_plan_reviewed': True, 'qa_contract_reviewed': True,
                'qa_required_values': contract, 'required_source_paths': sorted(n for n in sources if n.startswith('src/')),
                'artifacts': {name: {'bytes': (args.dist / name).stat().st_size,
                                     'sha256': sha((args.dist / name).read_bytes())}
                              for name in files + ['SHA256SUMS.txt']}}
    for kind in ('standalone', 'bundle'):
        manifest['allowed_changed_' + kind + '_entries'] = qa['changed_' + kind + '_entries']
        manifest['allowed_added_' + kind + '_entries'] = qa['added_' + kind + '_entries']
    args.manifest.write_bytes(json_bytes(manifest))
    entries = {name: {'bytes': len(data), 'sha256': sha(data)} for name, data in sorted(package.items())}
    (args.source.parent / 'publication' / 'source-zip-entries.json').write_bytes(json_bytes(entries))
    (args.source.parent / 'evidence').mkdir(exist_ok=True)
    saved_evidence = args.source.parent / 'evidence' / 'validation.json'
    if saved_evidence.resolve() != args.validation.resolve():
        saved_evidence.write_bytes(evidence_bytes)
    for name in (QA_NAME, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'):
        (args.source.parent / name).write_bytes((args.dist / name).read_bytes())
    pub.load_expected(args.manifest)
    print('PASS diagnostics47 host and source consistency, exact original APKS applications, deterministic package and pinned publication manifest; device untested')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path)
    args = parser.parse_args()
    for key, value in vars(args).items():
        if value is not None:
            setattr(args, key, value.resolve())
    if args.manifest is None:
        args.manifest = args.source.parent / 'manifest.json'
    finalize(args)


if __name__ == '__main__':
    main()
