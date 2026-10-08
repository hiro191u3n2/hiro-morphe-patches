#!/usr/bin/env python3
"""Finalize H40-H45 assets from fresh, source-pinned host/ARM/package evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import zipfile
from build1959 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, BUNDLE, SELECTED, PRODUCTION,
    CHANGED, ADDED, require, sha, json_bytes)
from validate1959 import host_checks

QA_NAME = 'QA_ULike_v1.9.59.json'
SOURCE_ZIP = 'ULike_v1.9.59_sources_and_QA.zip'
PUBLISHER_FILES = ('publish1959.py', 'ulike1959-h40-h45-publish.yml', 'README.md')
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
                    '.png', '.jpg', '.jpeg', '.heic', '.heif'}


def publisher(source):
    path = source.parent/'publication/publish1959.py'
    spec = importlib.util.spec_from_file_location('speed1959_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。H40～H45の6案を反映しました。

H40：NR9～NR13の独立した準備領域を、メモリ上限内で並列に計算します。
H41：同じ周辺画素の明暗・色差の計算値を再利用し、重複計算を減らします。
H42：新しいノイズ低減専用の4画素NEON処理を追加します。各画素の参照順・加算順・丸めを保ちます。
H43：作業配列とネイティブ作業領域を、上限と排他所有を守って再利用します。保持量をメモリ予算へ反映し、不要時は解放します。
H44：主ノイズ低減とNR13で重複していた保護判定・代表点の判定を共通化します。
H45：最初のノイズ低減が確定した画素から、次工程用の同じ2×2整数平均画像を作り、画像全体の読み直しを減らします。

NR1～NR13の計算強度・候補数・解像度・画素と丸め結果、美顔・色補正、回転・拡縮、圧縮品質と保存形式、保存公開の順序、カメラ起動修正を継承します。合成なし、撮影時のノイズ低減OFF、色補正ON／OFF、高精度・広色域・ゲインマップ画像の既存復帰経路を維持します。
合成・ノイズ・補正・圧縮・保存の区間計測を継続します。工程の時間は一部重なります。

ホスト検証：{qa['host_quality_result']['assertions']:,} アサーション成功。固定したv{BASE_VERSION}との画素一致、Java・新規JNI・実際のARM NEONと基準スカラーの一致、境界・透明画素・失敗時の復帰・メモリと排他所有を確認しました。単体と総合版のULikeペイロードが一致し、他アプリと10個の継承ネイティブのバイト一致を確認しています。既存のsmoothライブラリだけを更新し、登録数は11のままです。新しいネイティブ処理が利用できない場合はJava実装へ復帰します。
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


def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent/'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('schema') == 'ulike1959-build-declaration-v1'
            and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True,
            'Missing reviewed NR build declaration')
    qa_path = args.dist/QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size,
                       'sha256': sha((args.dist/name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1959-desktop-validation-v1'
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
             'compiled_nr_native_source_sha256', 'executed_host_source_sha256')},
            'Validated source differs from build source')
    for kind in ('standalone', 'bundle'):
        require(evidence['resource_delta'][kind+'_changed'] == sorted(CHANGED)
                and evidence['resource_delta'][kind+'_added'] == sorted(ADDED)
                and declaration['allowed_changed_'+kind+'_entries'] == sorted(CHANGED)
                and declaration['allowed_added_'+kind+'_entries'] == sorted(ADDED), 'Resource declaration differs')
    require(evidence['inherited_native_payloads'] == qa['inherited_native_payloads']
            and evidence['nr_native_payload'] == {'sha256': qa['nr_native_library_sha256'],
                                                'bytes': qa['nr_native_library_bytes']},
            'Native provenance differs')
    if 'nr_native_payload' in declaration:
        require(declaration['nr_native_payload'] == evidence['nr_native_payload'], 'Reviewed NR native pin differs')
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
    diagnostics = ('QA_ULike_v1.9.59.json', 'host-regression1959-result.json', 'native-build1958.json',
        'speed-inventory1959.json', 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log',
        'metadata.log', 'native-installer-metadata.log', 'nr-native-readelf.txt', 'RELEASE_NOTES.txt')
    for name in diagnostics:
        package[name] = (args.dist/name).read_bytes()
    require(package['helper-references.txt'].startswith(b'PASS helper references in ')
            and b'serialized_dex_verified=true' in package['emitted.log']
            and b'all ten other existing rows' in package['native-installer-metadata.log'], 'Fresh diagnostic proof absent')
    source_zip(args.dist/SOURCE_ZIP, package)
    files = [SINGLE, BUNDLE, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt']
    (args.dist/'SHA256SUMS.txt').write_text(''.join(sha((args.dist/name).read_bytes())+'  '+name+'\n' for name in files))
    contract = dict(pub.REQUIRED_QA)
    for key in ('changed_runtime_methods', 'changed_native_methods', 'new_helper_classes',
                'new_runtime_aliases', 'new_native_methods', 'new_jni_methods', 'new_runtime_methods',
                'removed_runtime_methods', 'replaced_helper_roots'):
        contract[key] = qa[key]
    contract.update({'host_quality_result.assertions': result['assertions'],
        'host_quality_result': result, 'nr_native_library_sha256': qa['nr_native_library_sha256'],
        'nr_native_library_bytes': qa['nr_native_library_bytes'],
        'nr_native_jni_exports': qa['nr_native_jni_exports'],
        'nr_native_load_segment_alignments': qa['nr_native_load_segment_alignments'],
        'nr_native_needed_libraries': qa['nr_native_needed_libraries']})
    manifest = {**declaration, 'schema': 'ulike1959-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'], 'nr_native_payload': evidence['nr_native_payload'],
        'artifacts': {name: {'bytes': (args.dist/name).stat().st_size,
                            'sha256': sha((args.dist/name).read_bytes())} for name in files+['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(manifest))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned single-image NR assets and publication manifest')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)
