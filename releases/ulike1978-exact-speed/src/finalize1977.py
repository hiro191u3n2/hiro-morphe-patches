#!/usr/bin/env python3
"""Finalize .77 GPU qualification recovery using source-pinned host and package evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1977 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1977 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.77.json'
SOURCE_ZIP = 'ULike_v1.9.77_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1977.py'
    spec = importlib.util.spec_from_file_location('strong_gpu1975_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。GPU_QUALIFICATION_EPOCH_RECOVERY：GPU画質照合の失敗記録が別の実行環境や候補へ広がり、GPU処理が全体で停止する不具合を修正しました。

修正：画質照合の拒否を実行環境と候補キーごとに分離し、件数上限による全候補の一括拒否を解消します。更新前の正の照合証明を新しい証明として引き継がず、新しいキャッシュ世代で再検証します。照合・保護条件などの個別拒否と、処理速度の条件不成立を区別します。実際の個別拒否は永続記録に保持し、64件の上限はメモリ上の高速参照用キャッシュだけに適用します。旧世代の署名付き個別拒否を保持し、一括拒否の印を個別の失敗記録として扱いません。
画質：独立したCPU基準との2回の完全一致照合、画質設定、既存の速度条件、キャンセル・メモリ不足・処理失敗時のCPU退避を保持します。画質照合を省略してGPUを強制使用する変更ではありません。初回や環境変更後は再照合の時間がかかります。
保全：既存のCPU／GPU高速化、program 42〜44、保存形式・解像度・撮影設定・既存カメラ修正・他アプリを保持します。GPU nativeペイロードの識別情報を再構築し、残り11個のnativeペイロードをバイト単位で保持します。既存12行のインストール手順はGPU行1行の指紋だけを更新し、JNIを追加しません。
確認：「直近の撮影・工程別処理時間」で、実際のGPU／CPU処理経路と「個別拒否（直近照会）」を確認できます。既存のカメラ診断ログの記録・共有・保存を継承します。
検証：現行ソースのホスト検証 {qa['strong_gpu_host_evidence']['assertions']:,} 件を実行しました。拒否記録の再起動保持、環境・候補間の分離、正の照合証明の世代分離と、既存CPU／GPU全出力一致・カメラ回帰・DEX・native登録・単体／総合版の同一性を確認しました。元APKSへの今回の適用は未実施です。Galaxy実機でのGPU復旧・画質・撮影・保存速度は未確認です。元APKS、再構築APK、写真、署名鍵、利用者の診断ログは配布しません。

Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ再適用して、生成アプリを更新インストールしてください。単体と総合版はどちらか一方を使用します。
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
            info = zipfile.ZipInfo(name, (2026, 10, 9, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            output.writestr(info, entries[name])

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent / 'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('schema') == 'ulike1977-build-declaration-v1'
        and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True
        and declaration.get('reviewed_source_sha256') == source_pins()
        and declaration.get('reviewed_publication_sha256') == publication_pins(), 'Reviewed camera graph changed before finalization')
    qa_path = args.dist / QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist / name).stat().st_size,
        'sha256': sha((args.dist / name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1977-desktop-validation-v1' and evidence.get('status') == 'passed'
        and evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION
        and evidence.get('artifacts') == qa['artifacts'] == artifacts
        and evidence.get('strong_gpu_host_evidence_sha256') == qa['strong_gpu_host_evidence_sha256']
        and evidence.get('host_assertions') == result['assertions']
        and evidence.get('host_reports') == result['tests']
        and evidence.get('source_consistency_verified') is True
        and evidence.get('serialized_dex_preservation_verified') is True
        and evidence.get('single_image_capture_admission_verified') is True
        and evidence.get('unmodified_pixel_kernel_helpers_bytecode_identical') is True, 'Fresh UI/logging/source/package evidence differs')
    require(evidence.get('source_groups') == {key: qa[key] for key in SOURCE_GROUPS}, 'Validation compiler/source graph differs')
    for key in ('original_apk_apply_tested', 'original_split_merge_tested', 'device_tested',
                'device_quality_verified', 'device_save_speed_measured', 'camera_visible_preview_verified_on_device'):
        require(evidence.get(key) is False, 'Unperformed Android test claimed')
    for kind in ('standalone', 'bundle'):
        require(evidence['resource_delta'][kind + '_changed'] == sorted(CHANGED)
            and evidence['resource_delta'][kind + '_added'] == sorted(ADDED)
            and declaration['allowed_changed_' + kind + '_entries'] == sorted(CHANGED)
            and declaration['allowed_added_' + kind + '_entries'] == sorted(ADDED), 'Resource declaration differs')
    require(evidence['inherited_native_payloads'] == qa['inherited_native_payloads']
        and len(evidence['inherited_native_payloads']) == 11
        and evidence.get('native_installer_byte_identical') is False and evidence.get('native_installer_inverse_verified') is True
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
    required = {'src/' + __import__('build1977').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1977.java', 'src/TimingCameraHooks1977.java', 'src/PatchClass1977.java', 'src/PatchLoader1977.java',
        'src/build1977.py', 'src/declare1977.py', 'src/validate1977.py', 'src/finalize1977.py',
        'src/host_strong_gpu1977.py', 'src/tests1975/host_strong1975.py', 'src/host_memory1975.py', 'src/host_native_facade1975.py', 'src/host_tuning1975.py', 'src/host_timing1975.py', 'src/Timing1975Test.java',
        'src/host_camera1973.py', 'src/host_precision1973.py', 'src/host_recovery1977.py',
        'src/host_timing1977.py', 'src/Timing1977Test.java', 'src/tests1977/CameraTrace1965.java', 'src/tests1977/ProcessingTiming1947.java',
        'src/tests1977/source-scope76.json', 'src/tests1977/reviewed-test-edits.json', 'src/source_scope1977.py'}
    required |= {'src/' + name for name in ('host_timing1976.py','Timing1976Test.java','host_tuning1976.py','host_gpu1976.py','host_memory1976.py','host_native_facade1976.py','tests1976/host_strong1976.py','host_colour_cache1976.py','host_layout1976.py','host_cpu_masks1976.py','host_resident1976.py','native_methods1976.json','host_cpu_finish_gate1976.py','host_chain1976.py','host_resident_pipeline1976.py')}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-gpu-recovery1977-result.json', 'gpu-recovery-inventory1977.json',
                 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log', 'metadata.log', 'gpu-native-readelf.txt', 'native-installer-metadata.log', 'RELEASE_NOTES.txt'):
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
    final = {**declaration, 'schema': 'ulike1977-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'],
        'gpu_native_payload': {'sha256': qa['gpu_native_library_sha256'], 'bytes': qa['gpu_native_library_bytes']},
        'rebuilt_native_payloads': qa['rebuilt_native_payloads'],
        'artifacts': {name: {'bytes': (args.dist / name).stat().st_size,
            'sha256': sha((args.dist / name).read_bytes())} for name in files + ['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(final))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned camera trace assets and publication manifest')
    return final

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)

