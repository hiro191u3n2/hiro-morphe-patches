#!/usr/bin/env python3
"""Finalize preview startup repair assets using source-pinned host/package evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1972 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1972 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.72.json'
SOURCE_ZIP = 'ULike_v1.9.72_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1972.py'
    spec = importlib.util.spec_from_file_location('strong_gpu1972_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。FRONT_RESTART_RECOVERY：インカメラ再起動時の入力未到着を調べ、復旧手順と状態記録を修正しました。LAYOUT_DIAGNOSTICS_REPAIR：画面崩れを調べる配置ログを改善しました。

カメラ：プレビュー開始成功と最初の入力フレーム到着を分けて扱います。同じカメラ・所有者・設定・Surface・前景世代を照合して、入力未到着時の復旧を行い、開始・準備・中断理由・復旧結果をログへ記録します。終了、撮影中、カメラ交換、無効な表示先では古い復旧処理を続けません。
配置ログ：通常のカメラ配置計算は保持し、測定時点と対象Viewを確認した上限付きの記録を改善します。写真や画像配列を診断ZIPへ含めません。診断の共有と保存は従来どおり「直近の撮影・工程別処理時間」から利用できます。
画質・GPU・保存：v{BASE_VERSION}の強ノイズGPU優先、通常GPU資格による再照合、2回の画質照合、初回のCPU確認、未対応・失敗時のCPU退避を保持します。必要な追加量だけを予約するGPUメモリ容量判定と、容量・失敗理由を取得する2つのnative診断getterも保持します。GPU演算、ノイズ除去・美顔・補正の画素計算、解像度、保存形式、圧縮設定、保存キュー、工程別時間表示、全12個のnativeペイロード、他アプリの資源は保持します。カメラ修正によるGPU速度改善は今回の変更範囲に含みません。
ホスト検証：実際の修正ソースを使うカメラ再起動と配置診断のフィクスチャで {qa['camera_host_evidence']['assertions']:,} アサーション成功。変更対象以外の全DEXクラス、ヘルパー参照、単体と総合版の一致、native資源、ソースとビルド入力の一致を確認しました。
元APKSへの今回の適用は未実施です。Galaxy実機でインカメラ再起動、黒画面、画面崩れが改善したかは未確認です。実機のGPU速度、撮影画質、保存速度も未確認です。元APKS、再構築APK、写真、署名鍵、ユーザーの診断ログは配布しません。

Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ再適用して、生成アプリを更新インストールしてください。単体と総合版はどちらか一方を使用します。
'''

def eligible(path, relative):
    return (not path.is_symlink() and not any(part in ('__pycache__', 'tmp', 'build', 'dist', 'classes')
        or part.startswith('.') for part in relative.parts) and path.suffix in
        ('.java', '.py', '.json', '.dex', '.tsv', '.txt', '.c', '.h', '.md', '.comp', '.sh'))

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
    require(declaration.get('schema') == 'ulike1972-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1972-desktop-validation-v1' and evidence.get('status') == 'passed'
        and evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION
        and evidence.get('artifacts') == qa['artifacts'] == artifacts
        and evidence.get('camera_host_evidence_sha256') == qa['camera_host_evidence_sha256']
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
        and len(evidence['inherited_native_payloads']) == 12
        and evidence.get('native_installer_byte_identical') is True
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
        'src/Transform1972.java', 'src/TimingCameraHooks1972.java', 'src/PatchClass1972.java',
        'src/build1972.py', 'src/declare1972.py', 'src/validate1972.py', 'src/finalize1972.py', 'src/host_camera1972.py'}
    require(required <= set(sources), 'Current camera source/test graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-camera-restart1972-result.json', 'camera-restart-inventory1972.json',
                 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log', 'metadata.log', 'RELEASE_NOTES.txt'):
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
    contract.update({'camera_host_evidence': result, 'camera_host_evidence.assertions': result['assertions'],
        'camera_host_evidence_sha256': qa['camera_host_evidence_sha256']})
    final = {**declaration, 'schema': 'ulike1972-publication-v1', 'qa_required_values': contract,
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

