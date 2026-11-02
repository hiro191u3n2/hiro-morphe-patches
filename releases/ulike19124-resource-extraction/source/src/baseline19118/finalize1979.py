#!/usr/bin/env python3
"""Finalize the .79 GPU bootstrap recovery with current host and inherited numerical evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1979 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1979 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.79.json'
SOURCE_ZIP = 'ULike_v1.9.79_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1979.py'
    spec = importlib.util.spec_from_file_location('gpu_bootstrap1979_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current = qa['strong_gpu_host_evidence']
    inherited = qa['inherited_numerical_validation1978']
    groups = '、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。GPU利用再開と、保存後の検証が待機し続ける状態の修正です。

1. 初回のGPU利用再開：認定済みの経路を保持し、未認定時には従来GPU経路の照合回数を制限した起動手順を使います。独立した2回の全出力の画質照合を条件にし、不一致・メモリ不足・失敗時にはCPUへ退避します。最適化シェーダーやGPU・CPU配分候補の追加認定は、保存後の待機中に進めます。
2. 保存後の進行：GPU処理が終わり保存が落ち着いた状態を再確認し、保留中の検証が進むようにします。新しい撮影の開始を早く通知して検証を中断し、撮影処理を優先します。
3. 認定の引継ぎ：12個のnativeペイロード、native登録情報、JNI宣言をv{BASE_VERSION}とバイト単位で一致させます。GPUの識別情報を今回のJava側修正で変更せず、同じ環境で取得済みのCPU・GPU認定を保持します。認定がない経路を無条件で採用する変更ではありません。
4. 現在状態の表示：「直近の撮影・工程別処理時間」では、保存済みの撮影結果に現在の検証待ち・実行中・採用状況を添えます。写真ごとの工程時間と全体経過の計算、記録済みの診断スナップショット、カメラの操作・保存制御は維持します。カメラの問題を調べるログ共有も維持します。

画質：CPUの画素計算・モデル解析・保護係数、GPU program 0〜50のシェーダーと12個のnativeペイロードはv{BASE_VERSION}と同一です。解像度、ノイズ強度、質感・肌・ハロー・暗部の保護、シャープネス、保存形式と圧縮設定を保持します。完全一致の認定、実際の不一致の拒否記録、キャンセル、メモリ上限とCPU退避を維持します。

今回の検証：{groups} の{len(current['tests'])}群、計{current['assertions']:,}件のホスト検証を実行しました。変更したJava経路と検証制御の結果を、今回のソース指紋に結び付けています。MPPの差分、変更範囲外のDEX、全12 nativeと登録情報、単体版と総合版のULike資源、他アプリの資源も照合します。
引き継ぐ数値検証：v{inherited['ulike_version']}／総合版v{inherited['bundle_version']}で公開した{inherited['assertions']:,}件の全画素・浮動小数点・JNI等の記録を参照します。この数値検証一式は今回再実行していません。今回の同一性検査と、過去の実行記録を分けてQAに記載しています。
元APKSへの今回の適用は未実施です。実機での撮影・画質・保存速度は未確認です。今回のホスト検証を約1900msへの復帰や実機の短縮時間の証明とは扱いません。

更新：Managerのソース更新後、ULikeを再適用してください。ULikeに対する更新通知とし、他アプリのパッチ資源・履歴を保持します。v{VERSION}適用後は同じ更新を未適用として表示しない判定を検証します。
共有：不具合時には「直近の撮影・工程別処理時間」とログ共有を使用できます。公開用ソース・QAには利用者の写真、実行時ログ、元APK／APKS、署名鍵を含めません。
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
    require(declaration.get('schema') == 'ulike1979-build-declaration-v1'
        and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True
        and declaration.get('reviewed_source_sha256') == source_pins()
        and declaration.get('reviewed_publication_sha256') == publication_pins(), 'Reviewed GPU bootstrap graph changed before finalization')
    qa_path = args.dist / QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist / name).stat().st_size,
        'sha256': sha((args.dist / name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1979-desktop-validation-v1' and evidence.get('status') == 'passed'
        and evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION
        and evidence.get('artifacts') == qa['artifacts'] == artifacts
        and evidence.get('strong_gpu_host_evidence_sha256') == qa['strong_gpu_host_evidence_sha256']
        and evidence.get('host_assertions') == result['assertions']
        and evidence.get('host_reports') == result['tests']
        and evidence.get('source_consistency_verified') is True
        and evidence.get('serialized_dex_preservation_verified') is True
        and evidence.get('single_image_capture_admission_verified') is True
        and evidence.get('unmodified_pixel_kernel_helpers_bytecode_identical') is True, 'Fresh bootstrap/idle/UI/source/package evidence differs')
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
    required = {'src/' + __import__('build1979').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1979.java', 'src/TimingCameraHooks1979.java', 'src/PatchClass1979.java',
        'src/build1979.py', 'src/declare1979.py', 'src/validate1979.py', 'src/finalize1979.py',
        'src/source_scope1979.py', 'src/production1979.json', 'src/qa_contract1978.json'}
    contract_sources = json.loads((args.source / 'qa_contract1979.json').read_text())
    required |= {'src/qa_contract1979.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-gpu-bootstrap1979-result.json', 'gpu-bootstrap-inventory1979.json',
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
    final = {**declaration, 'schema': 'ulike1979-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'],
        'gpu_native_payload': {'sha256': qa['gpu_native_library_sha256'], 'bytes': qa['gpu_native_library_bytes']},
        'rebuilt_native_payloads': qa['rebuilt_native_payloads'],
        'artifacts': {name: {'bytes': (args.dist / name).stat().st_size,
            'sha256': sha((args.dist / name).read_bytes())} for name in files + ['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(final))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned GPU bootstrap assets and publication manifest')
    return final

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)

