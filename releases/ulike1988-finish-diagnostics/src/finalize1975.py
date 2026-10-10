#!/usr/bin/env python3
"""Finalize .75 GPU throughput using source-pinned host and package evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1975 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1975 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.75.json'
SOURCE_ZIP = 'ULike_v1.9.75_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1975.py'
    spec = importlib.util.spec_from_file_location('strong_gpu1975_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。STRONG_GPU_THROUGHPUT：強ノイズ処理に5つの高速化を追加しました。

1. 初回のGPU処理と独立したCPU基準の計算を並行し、GPU待ちの間にも画質照合の準備を進めます。最終出力・confidence・保護情報の完全な一致判定は保持します。
2. 画質が完全一致したGPU候補だけを比較し、測定した処理時間が短い候補を同じ条件で優先します。未確認の候補や不一致の候補は採用しません。
3. 撮影のない待機時間にGPU候補の確認を進めます。撮影開始・キャンセル・環境変更時には古い確認を採用せず、処理量とメモリを制限します。
4. 強ノイズの結果を既存のJava配列へ読み戻す専用JNIを追加し、出力配列の確保とコピーを減らします。処理中の別区間とは配列を共有せず、失敗時のCPU退避を保持します。
5. 同じ入力に対する2回の画質照合ではGPUに転送した入力を保持し、重複する転送を省きます。CPU基準の計算と完全一致の確認は2回実行します。

保全：CPUのノイズ計算と全GPUシェーダー、カメラ、保存形式、解像度、他アプリを保持します。変更するnativeはGPU転送処理の1個だけで、ほかの11個を保持します。既存のGPU登録行の指紋を更新し、読み戻し用JNIを1個追加します。カメラ診断ログの記録・共有・保存は継承します。「直近の撮影・工程別処理時間」からGPU／CPU区間数と処理時間を確認できます。並行処理の作業時間は重なるため、個別作業時間の合計と実際の経過時間は一致しない場合があります。
検証：現行ソースのホスト検証 {qa['strong_gpu_host_evidence']['assertions']:,} 件を実行しました。CPU／GPU完全一致・候補選択・待機確認・再利用・失敗退避とカメラ回帰、DEX、native登録、単体／総合版の一致を確認しました。元APKSへの今回の適用は未実施です。Galaxy実機のGPU使用、画質、表示、撮影、保存速度は未確認です。実機での高速化を保証する測定結果ではありません。元APKS、再構築APK、写真、署名鍵、利用者の診断ログは配布しません。

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
    require(declaration.get('schema') == 'ulike1975-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1975-desktop-validation-v1' and evidence.get('status') == 'passed'
        and evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION
        and evidence.get('artifacts') == qa['artifacts'] == artifacts
        and evidence.get('strong_gpu_host_evidence_sha256') == qa['strong_gpu_host_evidence_sha256']
        and evidence.get('host_assertions') == result['assertions']
        and evidence.get('host_reports') == result['tests']
        and evidence.get('source_consistency_verified') is True
        and evidence.get('serialized_dex_preservation_verified') is True
        and evidence.get('single_image_capture_admission_verified') is True
        and evidence.get('pixel_kernel_helpers_bytecode_identical') is True, 'Fresh UI/logging/source/package evidence differs')
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
    required = {'src/' + name + '.java' for name in PRODUCTION} | {
        'src/Transform1975.java', 'src/TimingCameraHooks1975.java', 'src/PatchClass1975.java', 'src/PatchLoader1975.java',
        'src/build1975.py', 'src/declare1975.py', 'src/validate1975.py', 'src/finalize1975.py',
        'src/host_strong_gpu1975.py', 'src/tests1975/host_strong1975.py', 'src/host_memory1975.py', 'src/host_native_facade1975.py', 'src/host_tuning1975.py', 'src/host_timing1975.py', 'src/Timing1975Test.java',
        'src/host_camera1973.py', 'src/host_precision1973.py',
        'src/tests1975/source-scope74.json', 'src/source_scope1975.py'}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-gpu-latency1975-result.json', 'gpu-latency-inventory1975.json',
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
    final = {**declaration, 'schema': 'ulike1975-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'],
        'gpu_native_payload': {'sha256': qa['gpu_native_library_sha256'], 'bytes': qa['gpu_native_library_bytes']},
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

