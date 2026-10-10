#!/usr/bin/env python3
"""Finalize .86 forecast and finish repairs with fresh host and original-APKS evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1986 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1986 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.86.json'
SOURCE_ZIP = 'ULike_v1.9.86_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1986.py'
    spec = importlib.util.spec_from_file_location('gpu_forecast_finish1986_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current = qa['strong_gpu_host_evidence']
    groups = '、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。
撮影をまたぐGPU実測履歴、旧GPU基準が使えない場合の仕上げ認定、認定と予測の診断表示を修正しました。

1. 撮影をまたぐGPU時間予測
遅いGPUの再評価までの待機状態と、予測に使う実測履歴を分離しました。1.5秒の待機が終わっても実測履歴を消さず、12秒の有効期間内で最大3撮影・64キーの上限を維持します。別々の2撮影以上の実測がそろった場合だけ履歴を使用し、認定値・有効履歴・同じ撮影のstage/bank実測の最大値で保守的に予測します。履歴が遅い間はCPUを選び、有効履歴が不足した後は認定済み経路を再評価できます。前景での新しい画質検証やGPU待ち上限の引上げは行いません。

2. 旧GPU基準が使えない場合の仕上げ認定
旧GPUの仕上げ経路で有効な比較基準を作れない場合、新しい仕上げ候補を実際のCPU出力と実測時間で認定できるようにしました。候補ごとに独立した2回の全画素一致を要求し、CPU最速に対してGPU最遅が5%以上速い場合だけ認定します。有効な旧GPU基準がある場合は、従来どおりCPUと旧GPUの両方に対する速度条件を維持します。CPU基準が2試行で変化した場合も認定しません。保存済みの画質拒否を尊重し、実行不能・メモリ不足・取消を画質不一致として記録しません。

3. 仕上げ前提と時間予測の詳細ログ
旧GPUと新GPU各候補について、全画素一致回数、速度計測回数、比較基準、CPU最速・旧GPU最速・候補GPU最遅、不成立理由を記録します。GPU連結の前提が不成立でも、旧GPUの保存済み拒否、実行不能、速度未達などを区別できます。Strongの予測は、有効撮影数、最も古い有効観測の年齢、認定値、観測最大値、採否に使った最終予測値を表示します。小さな数値だけを保持し、保存済み写真の処理時間・GPU件数や採否・取消の動作を変えません。

画質と保存
NR・美肌・シャープの計算と強度、肌・輪郭・暗部の保護、解像度、色空間・ビット深度・HDR/gainmapの既存の扱い、HEIF設定と単写条件を維持します。全12 nativeペイロード、GPU program 0〜50、JNIとnative登録は公開 .85 と同一です。既存の画質認定キー、不一致記録、独立した2回の全出力一致と速度条件、実際の転送予算と保持メモリの上限も維持します。カメラ・プレビュー・保存フックと他アプリの資源を保持します。

検証
{groups} の {len(current['tests'])} 群、計 {current['assertions']:,} assertions を今回のソースで実行。公開.85での履歴喪失と旧GPU基準依存の再現、新版の撮影間隔・履歴期限・実CPU比較・全画素不一致・速度不足・取消・CPU基準不安定・診断例外の回帰と、既存の画素・認定・GPU枠・所有権・保存の回帰を確認しました。過去の検証件数は今回の合計へ加算しません。
ULike 5.6.2（740）の原本APKSへ実際に適用し、split結合、FULL DEX再構築、リソース構築・整列、完成APKの契約・メンバー参照・型・nativeを確認。ローカルで検証したMPPと公開CIの再構築MPPは完全一致を必須にします。原本適用をCIが再実行したとは表示しません。
Android実機での撮影、最終画質、GPU配分、保存速度は未確認です。1.9秒への復帰や全工程GPU化を保証するものではありません。

更新
Morphe Managerのソースを更新し、ULikeを再適用してください。単体版v{VERSION}と総合版v{BUNDLE_VERSION}のULike資源は同一です。
「直近の撮影・工程別処理時間」でGPU採用、CPU退避、GPU連結、検証の詳細を確認できます。検証不成立の理由と、その撮影でCPUへ退避した区間を分けて確認してください。全体経過は記録開始からの時間で、必ずしもシャッター入力からの時間ではありません。標準美顔・撮影対象フレーム待ちは個別工程の計測外です。
利用者の写真、生の実行時ログ、原本APKS、生成APK、署名鍵は配布しません。
'''


def eligible(path, relative):
    return (not path.is_symlink() and not any(part in ('__pycache__', 'tmp', 'build', 'dist', 'classes')
        or part.startswith('.') for part in relative.parts) and path.suffix in
        ('.java', '.py', '.json', '.dex', '.tsv', '.txt', '.c', '.h', '.md', '.comp', '.glsl', '.sh'))

def source_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_STORED) as output:
        for name in sorted(entries):
            item = PurePosixPath(name)
            require(name and not item.is_absolute() and '..' not in item.parts and '\\' not in name
                and item.suffix.casefold() not in PRIVATE_SUFFIXES, 'Unsafe/private archive path')
            info = zipfile.ZipInfo(name, (2026, 10, 10, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            output.writestr(info, entries[name])

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent / 'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('schema') == 'ulike1986-build-declaration-v1'
        and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True
        and declaration.get('reviewed_source_sha256') == source_pins()
        and declaration.get('reviewed_publication_sha256') == publication_pins(), 'Reviewed whole-app audit graph changed before finalization')
    qa_path = args.dist / QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist / name).stat().st_size,
        'sha256': sha((args.dist / name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1986-desktop-validation-v1' and evidence.get('status') == 'passed'
        and evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION
        and evidence.get('artifacts') == qa['artifacts'] == artifacts
        and evidence.get('strong_gpu_host_evidence_sha256') == qa['strong_gpu_host_evidence_sha256']
        and evidence.get('host_assertions') == result['assertions']
        and evidence.get('host_reports') == result['tests']
        and evidence.get('source_consistency_verified') is True
        and evidence.get('serialized_dex_preservation_verified') is True
        and evidence.get('single_image_capture_admission_verified') is True
        and evidence.get('unmodified_pixel_kernel_helpers_bytecode_identical') is True, 'Fresh whole-app audit/source/package evidence differs')
    require(evidence.get('source_groups') == {key: qa[key] for key in SOURCE_GROUPS}, 'Validation compiler/source graph differs')
    for key in ('device_tested',
                'device_quality_verified', 'device_save_speed_measured', 'camera_visible_preview_verified_on_device'):
        require(evidence.get(key) is False, 'Unperformed Android test claimed')
    require(evidence.get('original_apk_apply_tested') is True and evidence.get('original_split_merge_tested') is True and evidence.get('original_apk_application') == qa['original_application'], 'Tested original APKS evidence missing')
    for kind in ('standalone', 'bundle'):
        require(evidence['resource_delta'][kind + '_changed'] == sorted(CHANGED)
            and evidence['resource_delta'][kind + '_added'] == sorted(ADDED)
            and declaration['allowed_changed_' + kind + '_entries'] == sorted(CHANGED)
            and declaration['allowed_added_' + kind + '_entries'] == sorted(ADDED), 'Resource declaration differs')
    require(evidence['inherited_native_payloads'] == qa['inherited_native_payloads']
        and len(evidence['inherited_native_payloads']) == 12
        and evidence.get('native_installer_byte_identical') is True and evidence.get('native_installer_inverse_verified') is True
        and evidence.get('native_methods_byte_identical') is True
        and evidence.get('native_methods_tsv_byte_identical') is True, 'Native preservation evidence differs')
    for key, expected in pub.REQUIRED_QA.items():
        actual = pub.lookup_qa(qa, key)
        require(type(actual) is type(expected) and actual == expected, 'Semantic QA contract failed: ' + key)
    sources = {'src/' + path.relative_to(args.source).as_posix(): path.read_bytes()
        for path in sorted(args.source.rglob('*')) if path.is_file() and eligible(path, path.relative_to(args.source))}
    sources['manifest.json'] = json_bytes(declaration)
    for name in PUBLISHER_FILES:
        sources['publication/' + name] = (args.source.parent / 'publication' / name).read_bytes()
    required = {'src/' + __import__('build1986').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1986.java', 'src/TimingCameraHooks1980.java', 'src/PatchClass1986.java',
        'src/build1986.py', 'src/declare1986.py', 'src/validate1986.py', 'src/finalize1986.py',
        'src/source_scope1986.py', 'src/diagnostic_methods1986.json', 'src/tests1986/source-scope85.json', 'src/production1986.json', 'src/qa_contract1978.json',
        'src/application_evidence1986.py', 'src/apply_original1986.py', 'src/inspect_patched1986.py',
        'src/VerifyAllHelperReferences1980.java', 'src/tests1981/VerifyAppliedTypes1981.java',
        'src/tests1986/original-application.json'}
    contract_sources = json.loads((args.source / 'qa_contract1986.json').read_text())
    required |= {'src/qa_contract1986.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-whole-audit1986-result.json', 'whole-audit-inventory1986.json',
                 'emitted-audit.tsv', 'all-helper-references.txt', 'helper-references.txt', 'emitted.log', 'metadata.log', 'gpu-native-readelf.txt', 'native-installer-metadata.log', 'RELEASE_NOTES.txt'):
        package[name] = (args.dist / name).read_bytes()
    require(package['helper-references.txt'].startswith(b'PASS helper references in ')
        and b'exact executable loader implementation preserved' in package['metadata.log'], 'Fresh linkage/metadata proof absent')
    source_zip(args.dist / SOURCE_ZIP, package)
    files = [SINGLE, BUNDLE, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    contract = dict(pub.REQUIRED_QA)
    for key in ('changed_runtime_methods', 'new_runtime_methods', 'removed_runtime_methods',
                'new_helper_classes', 'new_runtime_aliases', 'replaced_helper_roots', 'bytecode_patch_helper_roots',
                'preserved_runtime_class_count', 'changed_native_methods', 'new_native_methods', 'new_jni_methods'):
        contract[key] = qa[key]
    contract.update({'strong_gpu_host_evidence': result, 'strong_gpu_host_evidence.assertions': result['assertions'],
        'strong_gpu_host_evidence_sha256': qa['strong_gpu_host_evidence_sha256']})
    final = {**declaration, 'schema': 'ulike1986-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'],
        'gpu_native_payload': {'sha256': qa['gpu_native_library_sha256'], 'bytes': qa['gpu_native_library_bytes']},
        'rebuilt_native_payloads': qa['rebuilt_native_payloads'],
        'artifacts': {name: {'bytes': (args.dist / name).stat().st_size,
            'sha256': sha((args.dist / name).read_bytes())} for name in files + ['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(final))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned whole-app audit assets and publication manifest')
    return final

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)

