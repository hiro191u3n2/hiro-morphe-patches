#!/usr/bin/env python3
"""Finalize the seven .78 speed optimizations using source-pinned host and package evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1978 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1978 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.78.json'
SOURCE_ZIP = 'ULike_v1.9.78_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1978.py'
    spec = importlib.util.spec_from_file_location('strong_gpu1975_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。採用案1〜7の、画質を維持する保存高速化を実装しました。

1. GPU・CPU配分：実際の4ワーカーと2つのGPU処理枠を動かして、転送・待機・読戻し・完了待ちを含む区間群の時間を比較します。画質と速度の条件を満たした場合だけ、空いているCPUへの配分を使います。強ノイズからGPU連結する経路はCPU結果の追加転送費用が異なるため、この配分証明を流用しません。
2. 保存後の画質照合：未認定の強ノイズ区間はCPUで保存を完了し、GPU候補の独立した2回の全出力照合を保存後に回します。初回や環境変更後も、撮影中に新候補のCPU・GPU照合を重ねない方式です。次の撮影を優先し、背景検証は中断します。
3. 保護情報の直接転送：CPUで確定した肌・細部の保護情報をGPUへ直接渡す候補を追加します。同じ情報の再生成を省き、全保護情報・ARGB・信頼度の一致を確認します。
4. 前段とモデル準備：撮影ワーカー専有の作業メモリを再利用し、地域解析の輝度とスペクトル計算の同一部分を再利用します。計算精度、演算順序、標本とノイズ強度を維持します。
5. 補正係数：既知の変更されない保護マスクでは、同じ平滑化信頼度を共用します。未知のコールバックは従来の呼出し順を維持します。帯ごとの準備にも既存CPUワーカーを利用する候補を追加します。
6. GPU連結：保存後の検証用half複製を減らし、検証時に同じ入力から再生成します。全画素比較を保持したまま比較用メモリの寿命を短縮し、依存する補正経路の認定を先に進めます。等倍はノイズ分析位置を維持した専用経路で判定します。
7. 厳密除算：2の累乗による除算だけ、ビットが一致する条件で指数変換を使う候補を追加します。例外値、非正規値、境界とその他の除数には従来の厳密処理を使います。既存GPU program 0〜44を保持し、新候補45〜50を追加します。

画質：解像度、ノイズ強度、質感・肌・ハロー・暗部の保護、シャープネス、保存形式と圧縮設定を保持します。新しい候補は比較に合格した経路だけを採用し、不一致・メモリ不足・キャンセル・失敗時には従来経路へ退避します。速度条件が未成立の候補を画質条件の代わりに採用することはありません。
診断：「直近の撮影・工程別処理時間」に、実際の配分、保存後のGPU確認待ち、入力／出力寸法、等倍の別、連結検証の保持量と保留理由を追加しました。全体経過の測定範囲は変更していません。カメラ診断ログの保存・共有を継承します。
保全：他アプリの資源とカメラ・保存の制御を保持します。GPUと前段NRのnativeを再構築し、残り10個のnativeペイロードはバイト単位で保持します。既存12行のインストール手順は対象2行の指紋だけを更新し、前段作業域のJNIを4件追加します。
検証：現行ソースのホスト検証 {qa['strong_gpu_host_evidence']['assertions']:,} 件を実行しました。既存CPU／GPU回帰、新候補の全出力・モデル・保護情報、取消・メモリ・DEX・native登録、単体と総合版の同一性を確認しました。元APKSへの今回の適用は未実施です。Galaxy実機での画質・撮影・保存速度は未確認です。区間群の速度認定は写真全体の保存時間を測定した証明ではなく、約1900msへの復帰は保証しません。元APKS、再構築APK、写真、署名鍵、利用者の診断ログは配布しません。

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
    require(declaration.get('schema') == 'ulike1978-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1978-desktop-validation-v1' and evidence.get('status') == 'passed'
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
        and len(evidence['inherited_native_payloads']) == 10
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
    required = {'src/' + __import__('build1978').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1978.java', 'src/TimingCameraHooks1978.java', 'src/PatchClass1978.java', 'src/PatchLoader1978.java',
        'src/build1978.py', 'src/declare1978.py', 'src/validate1978.py', 'src/finalize1978.py',
        'src/host_strong_gpu1978.py', 'src/tests1975/host_strong1975.py', 'src/host_memory1975.py', 'src/host_native_facade1975.py', 'src/host_tuning1975.py', 'src/host_timing1975.py', 'src/Timing1975Test.java',
        'src/host_camera1973.py', 'src/host_precision1973.py', 'src/host_recovery1977.py',
        'src/host_timing1978.py', 'src/Timing1978Test.java', 'src/tests1978/CameraTrace1965.java', 'src/tests1978/ProcessingTiming1947.java',
        'src/tests1978/source-scope77.json', 'src/tests1978/reviewed-test-edits.json', 'src/source_scope1978.py'}
    required |= {'src/' + name for name in ('host_timing1976.py','Timing1976Test.java','host_tuning1976.py','host_gpu1976.py','host_memory1976.py','host_native_facade1976.py','tests1976/host_strong1976.py','host_colour_cache1976.py','host_layout1976.py','host_cpu_masks1976.py','host_resident1976.py','native_methods1976.json','host_cpu_finish_gate1976.py','host_chain1976.py','host_resident_pipeline1976.py')}
    contract_sources = json.loads((args.source / 'qa_contract1978.json').read_text())
    required |= {'src/qa_contract1978.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-exact-speed1978-result.json', 'exact-speed-inventory1978.json',
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
    final = {**declaration, 'schema': 'ulike1978-publication-v1', 'qa_required_values': contract,
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

