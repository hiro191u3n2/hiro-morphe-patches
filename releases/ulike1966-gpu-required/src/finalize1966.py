#!/usr/bin/env python3
"""Finalize mandatory GPU assets from source-pinned real-host-GPU/package evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import zipfile
from build1966 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, BUNDLE, SELECTED, GX_STATUS, PRODUCTION,
    CHANGED, ADDED, require, sha, json_bytes)
from validate1966 import host_checks

QA_NAME = 'QA_ULike_v1.9.66.json'
SOURCE_ZIP = 'ULike_v1.9.66_sources_and_QA.zip'
PUBLISHER_FILES = ('publish1966.py', 'ulike1966-gpu-required-publish.yml', 'README.md')
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
                    '.png', '.jpg', '.jpeg', '.heic', '.heif'}


def publisher(source):
    path = source.parent/'publication/publish1966.py'
    spec = importlib.util.spec_from_file_location('gx1962_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。

GPU_REQUIRED_NOISE：有効なノイズ解析・ノイズ除去の計算をGPUへ限定。
GPU_REQUIRED_PROTECTION：保護マスク・保護係数の画像計算をGPUへ限定。
GPU_REQUIRED_CORRECTION：追加の補正・仕上げ計算をGPUへ限定。
GPU_REQUIRED_RECOVERY：元画像を保持してGPU再初期化と制限付き再試行。回復不能時は保存失敗として伝え、CPUへの画像処理切替と未補正の成功扱いを禁止。Androidでは既知のソフトウェアGLドライバも処理開始前に拒否。
GPU_REQUIRED_DIAGNOSTICS：GPU処理・転送・待機・障害・再試行の診断を記録。

CPUは処理指示、元データのコピー、ログ、ファイルと圧縮・保存の管理に使用します。ULike既存の美顔SDK内部や顔検出の全実行部についてGPU限定を保証するものではありません。高精度演算、必要なGPU機能、メモリを確保できない場合は、画質を落とす代替保存をせず失敗します。ノイズ低減OFFはノイズ処理を行いません。解像度と保存形式、合成なし、撮影・カメラの既存修正、他アプリのペイロードを維持します。

ホスト検証：{qa['host_quality_result']['assertions']:,} アサーション成功。新しいGPU必須経路、対応するGPU計算、障害時の失敗伝播、ソースとDEXの一致、単体と総合版のULikeペイロード一致、11個の継承ネイティブのバイト一致を確認。モデルと保護・補正の画素比較範囲とGPU能力制限はQAに明記しています。完全な実機処理の画質同等性や短縮率は測定していません。
このCIでは元APKSへの適用テストを実行していません。原本適用の別途検証結果は公開後の証跡を参照してください。Galaxy実機の撮影・保存・画質・速度は未確認です。ホストのソフトウェアGPU試験はGalaxyのGPU実行や速度測定ではありません。元APKS、再構築APK、写真、署名鍵は配布しません。

Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ再適用して、生成アプリを更新インストールしてください。単体と総合版はどちらか一方を使用します。
'''


def eligible(path, relative):
    return (not path.is_symlink() and not any((part in ('__pycache__', 'tmp', 'build', 'dist', 'classes') or part.startswith('.'))
            for part in relative.parts) and path.suffix in
            ('.java', '.py', '.json', '.dex', '.tsv', '.txt', '.c', '.h', '.md', '.comp', '.glsl', '.sh'))


def source_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_STORED) as output:
        for name in sorted(entries):
            p = PurePosixPath(name)
            require(bool(name) and not p.is_absolute() and '..' not in p.parts and '\\' not in name
                    and p.suffix.casefold() not in PRIVATE_SUFFIXES, 'Unsafe/private source path')
            info = zipfile.ZipInfo(name, (2026, 10, 9, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            output.writestr(info, entries[name])


def qa_source_pins(source):
    from build1966 import source_pins
    return source_pins()

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent/'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('reviewed_source_sha256')==qa_source_pins(args.source), 'Reviewed source graph changed before finalization')
    require(declaration.get('reviewed_publication_sha256')=={n:sha((args.source.parent/'publication'/n).read_bytes()) for n in PUBLISHER_FILES}, 'Reviewed publication graph changed before finalization')
    require(declaration.get('schema') == 'ulike1966-build-declaration-v1'
            and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True,
            'Missing reviewed GPU build declaration')
    qa_path = args.dist/QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size,
                       'sha256': sha((args.dist/name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1966-desktop-validation-v1'
            and evidence.get('status') == 'passed' and evidence.get('ulike_version') == VERSION
            and evidence.get('bundle_version') == BUNDLE_VERSION
            and evidence.get('artifacts') == qa['artifacts'] == artifacts
            and evidence.get('host_quality_result_sha256') == qa['host_quality_result_sha256']
            and evidence.get('host_assertions') == result['assertions']
            and evidence.get('host_reports') == result['reports']
            and evidence.get('source_consistency_verified') is True
            and evidence.get('serialized_dex_preservation_verified') is True
            and evidence.get('single_image_capture_admission_verified') is True,
            'Fresh host/source/package evidence differs from built package')
    for key in ('original_apk_apply_tested', 'original_split_merge_tested',
                'device_tested', 'device_quality_verified', 'device_save_speed_measured'):
        require(evidence.get(key) is False, 'Unperformed Android validation claimed')
    require(evidence.get('source_groups') == {key: qa[key] for key in
            ('compiled_production_source_sha256', 'compiled_transformer_source_sha256',
             'compiled_gpu_native_source_sha256', 'executed_host_source_sha256')},
            'Validated source differs from build source')
    for kind in ('standalone', 'bundle'):
        require(evidence['resource_delta'][kind+'_changed'] == sorted(CHANGED)
                and evidence['resource_delta'][kind+'_added'] == sorted(ADDED)
                and declaration['allowed_changed_'+kind+'_entries'] == sorted(CHANGED)
                and declaration['allowed_added_'+kind+'_entries'] == sorted(ADDED), 'Resource declaration differs')
    require(evidence['inherited_native_payloads'] == qa['inherited_native_payloads']
            and evidence['gpu_native_payload'] == {'sha256': qa['gpu_native_library_sha256'],
                                                'bytes': qa['gpu_native_library_bytes']},
            'Native provenance differs')
    if 'gpu_native_payload' in declaration:
        require(declaration['gpu_native_payload'] == evidence['gpu_native_payload'], 'Reviewed GPU native pin differs')
    for key, expected in pub.REQUIRED_QA.items():
        actual = pub.lookup_qa(qa, key)
        require(type(actual) is type(expected) and actual == expected, 'Semantic QA contract failed: '+key)
    sources = {'src/'+p.relative_to(args.source).as_posix(): p.read_bytes()
               for p in sorted(args.source.rglob('*')) if p.is_file() and eligible(p, p.relative_to(args.source))}
    sources['manifest.json'] = json_bytes(declaration)
    sources['THIRD_PARTY_LICENSES.txt'] = (args.source/'THIRD_PARTY_LICENSES.txt').read_bytes()
    for name in PUBLISHER_FILES:
        sources['publication/'+name] = (args.source.parent/'publication'/name).read_bytes()
    qa.update(desktop_evidence_sha256=sha(evidence_bytes),
              desktop_validation=evidence, source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    notes = release_notes(qa)
    (args.dist/'RELEASE_NOTES.txt').write_text(notes)
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    diagnostics = ('QA_ULike_v1.9.66.json', 'host-regression1966-result.json', 'native-build1966.json',
        'gpu-inventory1966.json', 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log',
        'metadata.log', 'native-installer-metadata.log', 'gpu-native-readelf.txt', 'RELEASE_NOTES.txt')
    for name in diagnostics:
        package[name] = (args.dist/name).read_bytes()
    require(package['helper-references.txt'].startswith(b'PASS helper references in ')
            and b'serialized_dex_verified=true' in package['emitted.log']
            and b'all eleven other existing rows' in package['native-installer-metadata.log'], 'Fresh diagnostic proof absent')
    source_zip(args.dist/SOURCE_ZIP, package)
    files = [SINGLE, BUNDLE, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt']
    (args.dist/'SHA256SUMS.txt').write_text(''.join(sha((args.dist/name).read_bytes())+'  '+name+'\n' for name in files))
    contract = dict(pub.REQUIRED_QA)
    for key in ('changed_runtime_methods', 'changed_native_methods', 'new_helper_classes',
                'new_runtime_aliases', 'new_native_methods', 'new_jni_methods', 'new_runtime_methods',
                'removed_runtime_methods', 'replaced_helper_roots'):
        contract[key] = qa[key]
    contract.update({'host_quality_result.assertions': result['assertions'],
        'host_quality_result': result, 'gpu_native_library_sha256': qa['gpu_native_library_sha256'],
        'gpu_native_library_bytes': qa['gpu_native_library_bytes'],
        'gpu_native_jni_exports': qa['gpu_native_jni_exports'],
        'gpu_native_load_segment_alignments': qa['gpu_native_load_segment_alignments'],
        'gpu_native_needed_libraries': qa['gpu_native_needed_libraries']})
    manifest = {**declaration, 'schema': 'ulike1966-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'], 'gpu_native_payload': evidence['gpu_native_payload'],
        'artifacts': {name: {'bytes': (args.dist/name).stat().st_size,
                            'sha256': sha((args.dist/name).read_bytes())} for name in files+['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(manifest))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned mandatory GPU assets and publication manifest')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)
