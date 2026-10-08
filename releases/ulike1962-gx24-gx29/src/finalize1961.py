#!/usr/bin/env python3
"""Finalize conditional GPU assets from source-pinned real-host-GPU/package evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import zipfile
from build1961 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, BUNDLE, SELECTED, GX_STATUS, PRODUCTION,
    CHANGED, ADDED, require, sha, json_bytes)
from validate1961 import host_checks

QA_NAME = 'QA_ULike_v1.9.61.json'
SOURCE_ZIP = 'ULike_v1.9.61_sources_and_QA.zip'
PUBLISHER_FILES = ('publish1961.py', 'ulike1961-gx11-gx23-publish.yml', 'README.md')
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
                    '.png', '.jpg', '.jpeg', '.heic', '.heif'}


def publisher(source):
    path = source.parent/'publication/publish1961.py'
    spec = importlib.util.spec_from_file_location('gx1961_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。GX11～GX23を反映し、対応する工程へGPU経路を追加しました。GX1～GX10の既存経路を継承します。各案の対応範囲は以下のとおりです。

GX11：空間ノイズ・強いノイズの地域解析をGPUへ接続します。
GX12：画素ごとの整数補正量と4×4の滑らかさ保護をGPUへ接続します。
GX13（部分対応）：弱いノイズの残差再構成をGPUへ接続します。倍精度の変換・閾値判定は必要精度に対応しないGPUでは既存のCPU計算を維持します。
GX14（部分対応）：顔領域の画素判定・局所範囲・整数保護をGPUへ接続します。顔検出と倍精度の楕円判定は既存経路を維持します。
GX15：大きな画像の回転・拡縮を、同じ係数・丸め・必要な隣接画素を保つGPUタイル処理へ接続します。
GX16：似た部分の比較で画素・輝度・色差を共有作業領域に保持します。比較順・候補数・強度を維持します。
GX17：整数配列の全要素をGPUで比較し、一致判定だけを読み戻します。
GX18：JNI呼び出しとGPU処理をまとめ、複数の出力を1回の完了待ちで読み戻します。
GX19：強いノイズのGPU作業を2つの独立した作業領域で重ねます。
GX20（部分対応）：縮小画像・解析・準備の接続と、対応するモアレ補正から拡縮までの中間画像をGPUに保持します。全工程の連続GPU化ではありません。
GX21：GPU作業領域と転送用領域を上限内で再利用します。
GX22：同じ出力を条件に、対応する作業グループとタイルの構成を選びます。
GX23：端末・ドライバ・アプリ・ソースの識別に結び付けて、背景で2回の画素一致と転送込み5％以上の短縮を確認した構成だけを有効にします。

全工程の100％GPU化ではありません。GPUが必要な計算精度・形式に対応しない場合、同じ入力での結果が一致しない場合、障害・メモリ不足・同期不明の場合は既存のCPU処理へ復帰します。実行時間の判定は転送と読み戻しを含みます。倍精度が必要な工程はGPU側の実対応を必須とします。
ノイズ低減の計算強度・比較候補数・解像度・美顔・色補正・圧縮品質・保存形式・保存順序を継承します。合成なし、ノイズ低減OFF、高精度・広色域・ゲインマップ画像の既存経路を維持します。合成・ノイズ・補正・圧縮・保存の区間計測を継続します。

ホスト検証：{qa['host_quality_result']['assertions']:,} アサーション成功。実際のホストMesa GPU計算経路、固定基準との画素比較、対応可否と既存経路への復帰、ソース一致、単体と総合版のULikeペイロード一致、他アプリと11個の継承ネイティブのバイト一致を確認しました。既存GPUライブラリ1個の登録値を更新し、登録数12と他の11個のライブラリを維持します。ホストのソフトウェアGPU検証はGalaxy GPUの速度測定ではありません。
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
    from build1961 import source_pins
    return source_pins()

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent/'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('reviewed_source_sha256')==qa_source_pins(args.source), 'Reviewed source graph changed before finalization')
    require(declaration.get('reviewed_publication_sha256')=={n:sha((args.source.parent/'publication'/n).read_bytes()) for n in PUBLISHER_FILES}, 'Reviewed publication graph changed before finalization')
    require(declaration.get('schema') == 'ulike1961-build-declaration-v1'
            and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True,
            'Missing reviewed GPU build declaration')
    qa_path = args.dist/QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size,
                       'sha256': sha((args.dist/name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1961-desktop-validation-v1'
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
    diagnostics = ('QA_ULike_v1.9.61.json', 'host-regression1961-result.json', 'native-build1961.json',
        'gpu-inventory1961.json', 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log',
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
    manifest = {**declaration, 'schema': 'ulike1961-publication-v1', 'qa_required_values': contract,
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
