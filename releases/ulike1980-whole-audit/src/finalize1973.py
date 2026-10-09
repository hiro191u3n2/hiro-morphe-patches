#!/usr/bin/env python3
"""Finalize bounded GPU latency and exact arithmetic using source-pinned package evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1973 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1973 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.73.json'
SOURCE_ZIP = 'ULike_v1.9.73_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1973.py'
    spec = importlib.util.spec_from_file_location('strong_gpu1973_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。STRONG_GPU_LATENCY：保存写真の強ノイズ処理で、初回GPU検証の重複と待ち時間を抑え、CPUと同じ単精度除算を作る候補を追加しました。

初回検証：撮影ごとに新しいGPU候補の画質確認を最大2候補へ制限します。同じ条件の候補検証を同時に重複実行せず、候補ごとに最初のCPU出力を保持して照合します。最初の一致後に独立した2回目のCPU／GPU完全照合を実行します。GPU枠不足や確認中の条件はCPUで進めます。未知の条件と新しいネイティブ環境は、最終ARGB・論理confidence・保護情報を完全なCPU基準と2回照合してから採用します。認定済みの条件は撮影ごとの再確認を省けますが、GPU失敗時のCPU退避は保持します。初回や条件変更時にはCPU照合の時間がかかります。
計算精度：強ノイズ本処理専用の候補39–41を追加し、単精度除算を整数による商・余りの確認で丸めます。CPU画質アルゴリズム・設定・解像度を変えず、既存の候補0–38のシェーダー分岐を保持します。新しい候補も完全な画質照合による一致が必要です。強ノイズ本処理で速度差だけを採用条件にしない方針と、準備・ほかの処理の採用条件を保持します。
診断：「直近の撮影・工程別処理時間」の「処理時間・GPU使用状況を表示」で、最終GPU／CPU区間に加え、CPU照合・GPU要求と結果取得・待機の作業時間を表示します。最初に不一致が出た候補について、ARGB不一致数と最大差、独立した論理confidence不一致数と最大差を表示し、取得していない比較は未取得とします。「処理時間・GPU使用状況を表示」で全文を確認できます。診断ログの記録・共有・Download/ULikeへのZIP保存を維持し、写真や画素配列を含めません。
保全：最新 .72 のカメラ再起動・配置修正をそのまま継承し、カメラ検証819件を今回のソースに対して再実行しました。変更する5つのJavaファミリー以外の全継承クラス、重要なCPU画素処理、ほかの11ネイティブ、既存JNI、保存形式、他アプリの資源を照合しました。GPUネイティブ1個と既存の登録行のSHA・容量のみを更新し、新JNIは追加していません。GPU境界と画質認定は変更を逆変換した .72 ソースを再コンパイルして全クラスを照合し、ログは版番号以外の処理を保持します。
検証契約：旧 .71 の重複CPU照合・6候補連続試行の期待値は、今回の最大2候補・同条件照合の集約を確認する検証に置き換えました。継承する準備処理・GPU境界・画質認定・メモリと旧シェーダー分岐は別途保全を確認しています。
ホスト検証：実際の変更ソースとJNI・ソフトウェアGLES、単精度除算のx86/ARM64照合、画質認定・メモリ・待機・診断・カメラ回帰で {qa['strong_gpu_host_evidence']['assertions']:,} 件の検証成功。単体・総合版のULike一致、11ネイティブと他アプリの保全、登録処理の逆変換、全DEXと固定ソースを確認しました。元APKSへの今回の適用は未実施です。Galaxy実機の原因特定、GPU使用、表示、撮影、保存速度は未確認です。800msへの復帰を保証する実機測定ではありません。元APKS、再構築APK、写真、署名鍵、利用者の診断ログは配布しません。

Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ再適用して生成アプリを更新インストールしてください。ソース更新のみではインストール済みアプリは変わりません。単体と総合版はどちらか一方を使用します。
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
    require(declaration.get('schema') == 'ulike1973-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1973-desktop-validation-v1' and evidence.get('status') == 'passed'
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
        'src/Transform1973.java', 'src/TimingCameraHooks1973.java', 'src/PatchClass1973.java', 'src/PatchLoader1973.java',
        'src/build1973.py', 'src/declare1973.py', 'src/validate1973.py', 'src/finalize1973.py', 'src/host_strong_gpu1973.py', 'src/host_strong_latency1973.py', 'src/host_certificate1973.py', 'src/PreferredCertificate1973Test.java', 'src/host_timing1973.py', 'src/Timing1973Test.java', 'src/host_memory1971.py', 'src/host_native_facade1973.py', 'src/tests1973/source-scope72.json','src/source_scope1973.py','src/VerifyScope1973.java','src/host_camera1973.py','src/host_precision1973.py','src/precision73-fixtures/ieee_div1973.glsl','src/latency73testfixtures/StrongLatency1973Test.java','src/latency73testfixtures/FixtureClasses.java'}
    require(required <= set(sources) and any(name.startswith('src/latency73testfixtures/') for name in sources), 'Current UI source/test graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-gpu-latency1973-result.json', 'gpu-latency-inventory1973.json',
                 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log', 'metadata.log', 'native-installer-metadata.log', 'gpu-native-readelf.txt', 'inverse-family-proof.log','RELEASE_NOTES.txt'):
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
    final = {**declaration, 'schema': 'ulike1973-publication-v1', 'qa_required_values': contract,
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

