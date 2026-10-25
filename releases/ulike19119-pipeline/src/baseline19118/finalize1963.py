#!/usr/bin/env python3
"""Finalize conditional GPU assets from source-pinned real-host-GPU/package evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import zipfile
from build1963 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, BUNDLE, SELECTED, GX_STATUS, PRODUCTION,
    CHANGED, ADDED, require, sha, json_bytes)
from validate1963 import host_checks

QA_NAME = 'QA_ULike_v1.9.63.json'
SOURCE_ZIP = 'ULike_v1.9.63_sources_and_QA.zip'
PUBLISHER_FILES = ('publish1963.py', 'ulike1963-audit-publish.yml', 'README.md')
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
                    '.png', '.jpg', '.jpeg', '.heic', '.heif'}


def publisher(source):
    path = source.parent/'publication/publish1963.py'
    spec = importlib.util.spec_from_file_location('gx1962_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。撮影・保存・ノイズ・補正・GPU・画面の全体精査で見つかった不具合と無駄な処理を修正しました。

AUDIT_GPU：GPU読み戻しのマップ後エラー、初期化途中の失敗、同期不明時の状態管理を修正。安全のため隔離した領域は解放を偽らず使用量へ計上します。状態確認の文字列生成と、資格検証が処理中のときの不要な画像コピーを減らします。
AUDIT_SAVE：撮影準備失敗後に次の撮影を受け付けにくくなる経路を修正。遅延した失敗通知が新しい撮影を解除しないよう世代を照合し、完了通知まで終了待機を維持します。保存ワーカーの異常で以降の保存が停止する問題を修正します。
AUDIT_PIPELINE：使われず再測定されるノイズ解析と、結果へ影響しない顔マスクの近傍走査を省きます。座標加算のオーバーフローを拒否し、任意のネイティブ処理を読み込めない場合は従来のCPU処理へ戻します。
AUDIT_UI：遅れて届く撮影情報が再使用された次の撮影へ結び付く不具合、測定不能時に古いプレビュー領域が残る経路、破棄した描画復旧処理を遅延通知が再登録する経路を修正します。黒い部分のダブルタップでのカメラ切替を無効化し、撮影領域の通常操作を維持します。終了待機の繰り返しの反射検索は既存キャッシュへ集約し、状態値は毎回読み取ります。

ノイズの粒・色ムラ処理、似た部分の比較候補・半径、解像度、演算順序・丸め、美顔・色補正・圧縮品質・保存形式を維持します。合成なし、ノイズ低減OFF、高精度・広色域・ゲインマップ画像の既存経路を維持します。保存と完了通知はFIFOの順序を守り、合成・ノイズ・補正・圧縮・保存の区間計測を継続します。

ホスト検証：{qa['host_quality_result']['assertions']:,} アサーション成功。公開v1.9.62の固定ソースとの独立した画像・顔マスク比較、実際のホストMesa GPU計算経路、異常時の復帰・遅延通知・保存キュー、ソースとDEXの一致、単体と総合版のULikeペイロード一致を確認しました。他アプリと11個の継承ネイティブはバイト一致。既存GPUライブラリ1個の登録値を更新し、登録数12を維持します。
元APKSへの今回の適用は未実施です。Galaxy実機の撮影・保存・画質・速度は未確認です。ホストのソフトウェアGPU検証はGalaxy GPUの速度測定ではありません。実機での短縮率は未測定です。元APKS、再構築APK、写真、署名鍵は配布しません。

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
    from build1963 import source_pins
    return source_pins()

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent/'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('reviewed_source_sha256')==qa_source_pins(args.source), 'Reviewed source graph changed before finalization')
    require(declaration.get('reviewed_publication_sha256')=={n:sha((args.source.parent/'publication'/n).read_bytes()) for n in PUBLISHER_FILES}, 'Reviewed publication graph changed before finalization')
    require(declaration.get('schema') == 'ulike1963-build-declaration-v1'
            and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True,
            'Missing reviewed GPU build declaration')
    qa_path = args.dist/QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size,
                       'sha256': sha((args.dist/name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1963-desktop-validation-v1'
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
    diagnostics = ('QA_ULike_v1.9.63.json', 'host-regression1963-result.json', 'native-build1963.json',
        'gpu-inventory1963.json', 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log',
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
    manifest = {**declaration, 'schema': 'ulike1963-publication-v1', 'qa_required_values': contract,
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
