#!/usr/bin/env python3
"""Finalize conditional GPU assets from source-pinned real-host-GPU/package evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import zipfile
from build1960 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, BUNDLE, SELECTED, GX_STATUS, PRODUCTION,
    CHANGED, ADDED, require, sha, json_bytes)
from validate1960 import host_checks

QA_NAME = 'QA_ULike_v1.9.60.json'
SOURCE_ZIP = 'ULike_v1.9.60_sources_and_QA.zip'
PUBLISHER_FILES = ('publish1960.py', 'ulike1960-gx1-gx10-publish.yml', 'README.md')
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
                    '.png', '.jpg', '.jpeg', '.heic', '.heif'}


def publisher(source):
    path = source.parent/'publication/publish1960.py'
    spec = importlib.util.spec_from_file_location('speed1960_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。GX1～GX10を検討・反映し、対応する工程へGPU経路を追加しました。各案の対応範囲は以下のとおりです。

GX1：粒ノイズ・色ムラの既存計算を、対応する精度と画素一致を条件にGPUへ接続します。
GX2：同一写真内の似た部分の比較をGPUへ接続します。比較候補数・参照画素と強度を維持します。
GX3（部分対応）：既存の保護マスクをGPUへ渡して補正量を計算します。顔検出とマスク生成は元の経路を維持します。
GX4（部分対応）：既存の2×2縮小準備画像、強いノイズ処理の差分・周波数・分布解析と補正尺度をGPUへ接続します。その他の解析・精度非対応の工程は元の経路を維持します。
GX5（部分対応）：既存のGPU対応モアレ・くっきり補正を維持します。元の色・明暗補正は取得可能な境界を維持します。
GX6：既存の回転・拡縮を同じ係数・丸め・端処理のGPU経路へ接続します。
GX7（部分対応）：強いノイズ処理の同一GPU段階内で中間画像をGPU作業領域に保持します。画像計算の全工程がGPU内で連続する構成ではありません。
GX8：GPUプログラム・コンテキストを再利用し、作業領域と同期を上限内で管理します。
GX9：美顔処理との接続可否を判定します。現行の取得境界では美顔GPU表面を取得できないため、元の美顔経路を維持します。
GX10：保存エンコーダとの接続可否を判定します。現行経路で品質・形式を維持する直接接続が使えないため、既存保存経路を維持します。

全工程の100％GPU化ではありません。GPUが必要な計算精度・形式に対応しない場合、同じ入力での結果が一致しない場合、障害・メモリ不足・同期不明の場合は既存のCPU処理へ復帰します。実行時間の判定は転送と読み戻しを含みます。倍精度が必要な工程はGPU側の実対応を必須とします。
ノイズ低減の計算強度・比較候補数・解像度・美顔・色補正・圧縮品質・保存形式・保存順序を継承します。合成なし、ノイズ低減OFF、高精度・広色域・ゲインマップ画像の既存経路を維持します。合成・ノイズ・補正・圧縮・保存の区間計測を継続します。

ホスト検証：{qa['host_quality_result']['assertions']:,} アサーション成功。実際のホストMesa GPU計算経路、固定基準との画素比較、対応可否と既存経路への復帰、ソース一致、単体と総合版のULikeペイロード一致、他アプリと11個の継承ネイティブのバイト一致を確認しました。GPUの新しいライブラリを1個追加し、登録数は12です。ホストのソフトウェアGPU検証はGalaxy GPUの速度測定ではありません。
元APKSへの今回の適用は未実施です。Galaxy実機の撮影・保存・画質・速度は未確認です。実機での短縮率は未測定です。元APKS、再構築APK、写真、署名鍵は配布しません。

Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ再適用して、生成アプリを更新インストールしてください。ソース更新のみではインストール済みアプリは変わりません。単体と総合版はどちらか一方を使用します。
'''


def eligible(path, relative):
    return (not path.is_symlink() and not any((part in ('__pycache__', 'tmp', 'build', 'dist', 'classes') or part.startswith('.'))
            for part in relative.parts) and path.suffix in
            ('.java', '.py', '.json', '.dex', '.tsv', '.txt', '.c', '.h', '.md', '.comp', '.sh'))


def source_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_STORED) as output:
        for name in sorted(entries):
            p = PurePosixPath(name)
            require(bool(name) and not p.is_absolute() and '..' not in p.parts and '\\' not in name
                    and p.suffix.casefold() not in PRIVATE_SUFFIXES, 'Unsafe/private source path')
            info = zipfile.ZipInfo(name, (2026, 10, 8, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            output.writestr(info, entries[name])


def qa_source_pins(source):
    from build1960 import source_pins
    return source_pins()

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent/'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('reviewed_source_sha256')==qa_source_pins(args.source), 'Reviewed source graph changed before finalization')
    require(declaration.get('reviewed_publication_sha256')=={n:sha((args.source.parent/'publication'/n).read_bytes()) for n in PUBLISHER_FILES}, 'Reviewed publication graph changed before finalization')
    require(declaration.get('schema') == 'ulike1960-build-declaration-v1'
            and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True,
            'Missing reviewed GPU build declaration')
    qa_path = args.dist/QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size,
                       'sha256': sha((args.dist/name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1960-desktop-validation-v1'
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
    for name in PUBLISHER_FILES:
        sources['publication/'+name] = (args.source.parent/'publication'/name).read_bytes()
    qa.update(desktop_evidence_sha256=sha(evidence_bytes),
              desktop_validation=evidence, source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    notes = release_notes(qa)
    (args.dist/'RELEASE_NOTES.txt').write_text(notes)
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    diagnostics = ('QA_ULike_v1.9.60.json', 'host-regression1960-result.json', 'native-build1960.json',
        'gpu-inventory1960.json', 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log',
        'metadata.log', 'native-installer-metadata.log', 'gpu-native-readelf.txt', 'RELEASE_NOTES.txt')
    for name in diagnostics:
        package[name] = (args.dist/name).read_bytes()
    require(package['helper-references.txt'].startswith(b'PASS helper references in ')
            and b'serialized_dex_verified=true' in package['emitted.log']
            and b'all eleven existing rows' in package['native-installer-metadata.log'], 'Fresh diagnostic proof absent')
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
    manifest = {**declaration, 'schema': 'ulike1960-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'], 'gpu_native_payload': evidence['gpu_native_payload'],
        'artifacts': {name: {'bytes': (args.dist/name).stat().st_size,
                            'sha256': sha((args.dist/name).read_bytes())} for name in files+['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(manifest))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned conditional GPU assets and publication manifest')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)
