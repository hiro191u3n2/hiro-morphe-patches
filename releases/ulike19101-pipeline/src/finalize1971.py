#!/usr/bin/env python3
"""Finalize preview startup repair assets using source-pinned host/package evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1971 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1971 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.71.json'
SOURCE_ZIP = 'ULike_v1.9.71_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1971.py'
    spec = importlib.util.spec_from_file_location('strong_gpu1971_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。STRONG_GPU_ROUTE_REPAIR：強ノイズGPUの候補失敗後の再照合経路、メモリ予約、失敗段階の診断を修正しました。

GPU経路：専用GPU候補を採用できない場合に継承した汎用GPU経路を改めて照合します。未知の条件は、最終出力とconfidenceをCPU基準の完全な計算結果と2回画質照合してからGPU出力を選びます。強ノイズ本処理では速度差だけを採用条件にせず、ほかの処理と準備処理の安全な採用条件は維持します。初回や条件が変わった撮影にはCPU照合の時間がかかります。
メモリ：既存GPUスロットと転送領域の容量を取得し、拡張に必要な追加分だけを予約します。同じ常駐バッファを繰り返し二重計上しないよう修正しました。総容量の制限、GPU枠の最大4ワーカー・15秒の待機、CPUへの安全な退避を保持します。
診断：「直近の撮影・工程別処理時間」に、最終出力のGPU／CPU区間、CPU照合回数、候補試行の失敗分類と最初の失敗位置を表示します。ARGB不一致、confidence不一致、保護情報不一致、保存済み拒否、メモリ不足、開始・転送・実行・取得の失敗を分け、区間数と候補試行数を区別します。「処理時間・GPU使用状況を表示」で全文を確認できます。診断ログの記録・共有・ダウンロード保存を維持し、写真や画像配列を診断ZIPに含めません。
画質・カメラ・保存：ノイズ除去の画素計算とGPUシェーダー、画像設定、解像度、保存形式、圧縮設定、カメラの起動監視・復旧を保持。変更する5つのJavaファミリー以外の継承クラス、重要な画素処理、ほかの11個のネイティブペイロード、既存ネイティブ宣言、他アプリの資源を照合しました。GPUエンジン1個と、その登録済み行のSHA・容量だけを更新します。診断用JNIを2個追加し、既存の時間計測とログ処理は版番号・診断追加を除いて保持します。
ホスト検証：実際の変更ソースによるGPU再照合、画質認定、メモリ予約、安全な退避、診断表示の検証で {qa['strong_gpu_host_evidence']['assertions']:,} アサーション成功。新しいGPUネイティブのビルド、全DEXの継承クラス、画素処理・カメラ復旧、ヘルパー参照、単体・総合版、固定ソースを確認しました。変更したJava/Cをホストの実際のJNI・ソフトウェアGLESへ接続し、GPU出力画素とconfidenceを固定CPU基準へ照合しました。ソフトウェアGLESによる以前のシェーダー診断では端末の失敗を再現できず、Galaxyで失敗する原因は確定していません。
元APKSへの今回の適用は未実施です。Galaxy実機のGPU使用、表示、撮影、保存速度は未確認です。GPUが必ず成功することや800msへの復帰を保証する実機測定結果ではありません。GPU未対応や結果不一致、失敗では継承CPU処理で保存を続けます。元APKS、再構築APK、写真、署名鍵、ユーザーの診断ログは配布しません。

Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ再適用して、生成アプリを更新インストールしてください。ソース更新のみではインストール済みアプリは変わりません。単体と総合版はどちらか一方を使用します。
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
    require(declaration.get('schema') == 'ulike1971-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1971-desktop-validation-v1' and evidence.get('status') == 'passed'
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
        'src/Transform1971.java', 'src/TimingCameraHooks1971.java', 'src/PatchClass1971.java', 'src/PatchLoader1971.java',
        'src/build1971.py', 'src/declare1971.py', 'src/validate1971.py', 'src/finalize1971.py', 'src/host_strong_gpu1971.py', 'src/host_strong_fallback1971.py', 'src/host_certificate1971.py', 'src/PreferredCertificate1971Test.java', 'src/host_timing1971.py', 'src/Timing1971Test.java', 'src/host_memory1971.py', 'src/host_native_facade1971.py', 'src/tests1971/native-pixel70-pins.json'}
    require(required <= set(sources) and any(name.startswith('src/strong71testfixtures/') for name in sources), 'Current UI source/test graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-strong-repair1971-result.json', 'strong-repair-inventory1971.json',
                 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log', 'metadata.log', 'native-installer-metadata.log', 'gpu-native-readelf.txt', 'RELEASE_NOTES.txt'):
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
    final = {**declaration, 'schema': 'ulike1971-publication-v1', 'qa_required_values': contract,
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

