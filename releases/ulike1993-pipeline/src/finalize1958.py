#!/usr/bin/env python3
"""Finalize NR9-NR13 assets from fresh, source-pinned host/package evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import zipfile
from build1958 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, BUNDLE, SELECTED, PRODUCTION,
    CHANGED, ADDED, require, sha, json_bytes)
from validate1958 import host_checks

QA_NAME = 'QA_ULike_v1.9.58.json'
SOURCE_ZIP = 'ULike_v1.9.58_sources_and_QA.zip'
PUBLISHER_FILES = ('publish1958.py', 'ulike1958-smooth-single-noise-publish.yml', 'README.md')
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
                    '.png', '.jpg', '.jpeg', '.heic', '.heif'}


def publisher(source):
    path = source.parent/'publication/publish1958.py'
    spec = importlib.util.spec_from_file_location('nr1958_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。NR9～NR13を追加しました。

NR9：近傍の平均で作ったガイドの輪郭と方向のつながりを調べ、ランダムな粒を輪郭として保護し過ぎないように判定します。文字・物の輪郭と規則的な模様を保護します。
NR10：場所ごとの残留ノイズを測り、ノイズが多い平坦部だけ除去量を強めます。弱い壁紙の凹凸などは薄くなる可能性があり、細部の完全な保持は保証できません。
NR11：同じ1枚の原寸画像で似た小領域を参照する、探索範囲と処理量を制限したNLMを平坦なノイズ領域へ追加します。複数枚の合成や生成処理は使いません。
NR12：明暗の輪郭と色の境界を分け、色境界を保護しながら色のざらつきやムラを独立して抑えます。
NR13：平滑化した部分の判定を仕上げシャープへ渡し、滑らかにした粒を再強調しないようにします。Java・既存ネイティブ・GPUの共通係数へ反映し、既存ABIとライブラリを保持します。

既存NR1～NR4の後へ新処理を適用し、色補正OFFでも適用します。撮影時のノイズ低減OFFでは追加しません。NR5～NR8の旧Javaクラスとネイティブペイロードはバイト一致で保持します。合成なしを継続します。
H28～H33の速度処理、保存順序、美顔・色補正、回転・拡縮の出力寸法、圧縮品質と保存形式、カメラ起動修正を継承します。仕上げの係数をNR13に対応させ、処理追加による実機保存時間は未測定です。
sRGBのARGB8888／RGB565が新処理の対象です。高精度、広色域、ゲインマップ付き画像は既存の復帰経路を使い、新処理用に8bitへ変換しません。
合成・ノイズ・補正・圧縮・保存の区間計測を継続します。工程の一部が重なるため、その合計は全体経過と一致しない場合があります。

ホスト検証：{qa['host_quality_result']['assertions']:,} アサーションが成功。独立した合成画像の数値受入試験で、平坦部の残留ノイズ・色ムラ・輪郭・模様、Javaと新規JNIの一致を検査しました。最終保存経路、ノイズ低減OFF、色補正ON／OFF、入力画像の不変性、帯域境界、メモリ・コピー・書き込み失敗時の復帰も確認しました。
単体と総合版のULikeペイロードの一致、他アプリの保持、既存10個のネイティブペイロードのバイト一致、新しいarm64-v8aライブラリの11番目の登録と整合性を確認しました。新しいネイティブ処理が利用できない場合はJava実装へ復帰します。
ノイズ低減の計算方法を変更しているため、v{BASE_VERSION}との最終画素一致は主張しません。処理追加による保存時間と見た目の改善率は実機での測定が必要です。
元APKSへの今回の適用は未実施です。Galaxy実機の撮影・保存・画質・速度は未確認です。ホスト検証は実機での改善率や保存時間を保証しません。元APKS、再構築APK、写真、署名鍵は配布しません。

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
    require(declaration.get('schema') == 'ulike1958-build-declaration-v1'
            and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True,
            'Missing reviewed NR build declaration')
    qa_path = args.dist/QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist/name).stat().st_size,
                       'sha256': sha((args.dist/name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1958-desktop-validation-v1'
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
    diagnostics = ('QA_ULike_v1.9.58.json', 'host-regression1958-result.json', 'native-build1958.json',
        'nr-inventory1958.json', 'emitted-audit.tsv', 'helper-references.txt', 'emitted.log',
        'metadata.log', 'native-installer-metadata.log', 'nr-native-readelf.txt', 'RELEASE_NOTES.txt')
    for name in diagnostics:
        package[name] = (args.dist/name).read_bytes()
    require(package['helper-references.txt'].startswith(b'PASS helper references in ')
            and b'serialized_dex_verified=true' in package['emitted.log']
            and b'all ten existing rows' in package['native-installer-metadata.log'], 'Fresh diagnostic proof absent')
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
    manifest = {**declaration, 'schema': 'ulike1958-publication-v1', 'qa_required_values': contract,
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
