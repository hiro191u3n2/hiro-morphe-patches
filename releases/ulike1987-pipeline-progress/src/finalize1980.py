#!/usr/bin/env python3
"""Finalize the .80 whole-app audit with fresh host and original-APKS evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1980 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1980 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.80.json'
SOURCE_ZIP = 'ULike_v1.9.80_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1980.py'
    spec = importlib.util.spec_from_file_location('whole_audit1980_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current = qa['strong_gpu_host_evidence']
    groups = '、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。撮影・GPU・ノイズ・補正・保存・カメラの待機と未使用経路を監査した修正版です。

1. GPU失敗の誤分類を修正
ノイズ除去、残差処理、解析、保護係数、画像変形、連結処理などで、転送・実行・読み戻し失敗を恒久的な画質不一致として保存していた箇所を修正しました。出力がない失敗は回数と待ち時間を制限した再試行対象にします。実際に得られた出力の不一致は従来どおり拒否します。初回のGPU利用再開に必要な画質照合、独立した2回の全出力一致、メモリ制限、キャンセル、CPU退避を保持します。
GPUの対応能力の確認に一時的に失敗した場合も、永久に未対応と記録しないよう修正しました。実際に未対応という応答が得られた場合の記録は保持し、能力が不明のまま終わった場合は後続の問い合わせで再確認します。

2. カメラの映像待機を修正・軽量化
インカメラの再開が受理された後、再試行回数を使い切ったことや期限が近いことだけを理由に、最初の映像フレームの監視まで打ち切る問題を修正しました。追加の再起動は行わず、元の期限内で到着を待ちます。新たに再起動しない区間の不要なネイティブ照会を省き、期限時に所有者を再確認してからログを記録します。無映像の診断記録を維持します。

3. 保存エラー後の解放を修正
撮影・保存の予約後に補助クラスの解決失敗などが起きた場合、保存枠や終了待ちが残らないようにしました。保存ジョブへ渡す前と渡した後の所有権を区別し、同じ画像や予約を二重に解放しません。診断自体の失敗でも後始末が進むことを確認します。致命的な例外は後始末の後で再送出します。

4. 未使用経路を削除
現行の実行エンジンが利用しているインターフェースを残し、旧エンジンの到達しない実行本体を削除しました。旧ノイズ準備や読み手のない状態値も、現用参照・反射・JNIとの関係を確認して整理します。現在使用しているノイズ・補正の処理を弱める変更は行いません。

画質・保存：解像度、ノイズ強度、質感・肌・ハロー・暗部の保護、シャープネス、保存形式・圧縮設定、単写の条件を保持します。GPU program 0〜50と全12個のnativeペイロード、native登録情報・JNIはv{BASE_VERSION}の配布物とバイト単位で同一です。今回のJava修正でnativeの識別情報を変えず、取得済みの認定と既存の画質不一致記録を維持します。

検証：{groups} の{len(current['tests'])}群、計{current['assertions']:,}件のホスト検証を今回のソースで実行しました。旧版の具体的な故障を再現し、修正後の後始末・待機・失敗分類・画質ゲートを確認します。過去の数値検証件数は今回の件数へ加算しません。変更範囲外のDEX、単体版と総合版のULike資源、他アプリ資源、全nativeの同一性、残存メソッドの参照を照合します。
元APKSへの適用を確認：ULike 5.6.2（740）の原本を使い、split結合、FULL DEX再構築、リソース再構築と整列、適用後の全runtime/payload契約と追加nativeを検査します。このローカル適用試験を通ったMPPと、公開CIで再構築したMPPの完全一致を必須にします。公開CIへ原本APKSを配布せず、CI自体で原本適用を再実行したとは表示しません。
実機での撮影・画質・保存速度は未確認です。ホスト試験と原本適用検査は、Galaxy実機での症状解消や1900msへの復帰、短縮時間の実測を意味しません。

更新：Morphe Managerのソース更新後、ULikeを再適用してください。他アプリのパッチ資源・既存の更新履歴は保持します。v{VERSION}適用後は同じ更新を未適用として表示しない判定を検証します。
診断：「直近の撮影・工程別処理時間」とログ共有を利用できます。工程時間の計算・写真ごとの記録は保持します。公開ファイルに利用者の写真、実行時ログ、元APK／APKS、署名鍵は含めません。
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
    require(declaration.get('schema') == 'ulike1980-build-declaration-v1'
        and declaration.get('change_plan_reviewed') is True and declaration.get('qa_contract_reviewed') is True
        and declaration.get('reviewed_source_sha256') == source_pins()
        and declaration.get('reviewed_publication_sha256') == publication_pins(), 'Reviewed whole-app audit graph changed before finalization')
    qa_path = args.dist / QA_NAME
    qa = json.loads(qa_path.read_text())
    result = host_checks(qa, args.source)
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    artifacts = {name: {'bytes': (args.dist / name).stat().st_size,
        'sha256': sha((args.dist / name).read_bytes())} for name in (SINGLE, BUNDLE)}
    require(evidence.get('schema') == 'ulike1980-desktop-validation-v1' and evidence.get('status') == 'passed'
        and evidence.get('ulike_version') == VERSION and evidence.get('bundle_version') == BUNDLE_VERSION
        and evidence.get('artifacts') == qa['artifacts'] == artifacts
        and evidence.get('strong_gpu_host_evidence_sha256') == qa['strong_gpu_host_evidence_sha256']
        and evidence.get('host_assertions') == result['assertions']
        and evidence.get('host_reports') == result['tests']
        and evidence.get('source_consistency_verified') is True
        and evidence.get('serialized_dex_preservation_verified') is True
        and evidence.get('single_image_capture_admission_verified') is True
        and evidence.get('unmodified_pixel_kernel_helpers_bytecode_identical') is True, 'Fresh whole-app audit/source/package evidence differs')
    require(evidence.get('source_groups') == {key: qa[key] for key in SOURCE_GROUPS}, 'Validation compiler/source graph differs')
    for key in ('device_tested',
                'device_quality_verified', 'device_save_speed_measured', 'camera_visible_preview_verified_on_device'):
        require(evidence.get(key) is False, 'Unperformed Android test claimed')
    require(evidence.get('original_apk_apply_tested') is True and evidence.get('original_split_merge_tested') is True and evidence.get('original_apk_application') == qa['original_application'], 'Tested original APKS evidence missing')
    for kind in ('standalone', 'bundle'):
        require(evidence['resource_delta'][kind + '_changed'] == sorted(CHANGED)
            and evidence['resource_delta'][kind + '_added'] == sorted(ADDED)
            and declaration['allowed_changed_' + kind + '_entries'] == sorted(CHANGED)
            and declaration['allowed_added_' + kind + '_entries'] == sorted(ADDED), 'Resource declaration differs')
    require(evidence['inherited_native_payloads'] == qa['inherited_native_payloads']
        and len(evidence['inherited_native_payloads']) == 12
        and evidence.get('native_installer_byte_identical') is True and evidence.get('native_installer_inverse_verified') is True
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
    required = {'src/' + __import__('build1980').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1980.java', 'src/TimingCameraHooks1980.java', 'src/PatchClass1980.java',
        'src/build1980.py', 'src/declare1980.py', 'src/validate1980.py', 'src/finalize1980.py',
        'src/source_scope1980.py', 'src/production1980.json', 'src/qa_contract1978.json',
        'src/application_evidence1980.py', 'src/apply_original1980.py',
        'src/VerifyAllHelperReferences1980.java', 'src/tests1980/VerifyAppliedTypes1980.java',
        'src/tests1980/original-application.json'}
    contract_sources = json.loads((args.source / 'qa_contract1980.json').read_text())
    required |= {'src/qa_contract1980.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-whole-audit1980-result.json', 'whole-audit-inventory1980.json',
                 'emitted-audit.tsv', 'all-helper-references.txt', 'helper-references.txt', 'emitted.log', 'metadata.log', 'gpu-native-readelf.txt', 'native-installer-metadata.log', 'RELEASE_NOTES.txt'):
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
    final = {**declaration, 'schema': 'ulike1980-publication-v1', 'qa_required_values': contract,
        'required_source_paths': sorted(name for name in sources if name.startswith('src/')),
        'inherited_native_payloads': qa['inherited_native_payloads'],
        'gpu_native_payload': {'sha256': qa['gpu_native_library_sha256'], 'bytes': qa['gpu_native_library_bytes']},
        'rebuilt_native_payloads': qa['rebuilt_native_payloads'],
        'artifacts': {name: {'bytes': (args.dist / name).stat().st_size,
            'sha256': sha((args.dist / name).read_bytes())} for name in files + ['SHA256SUMS.txt']}}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(final))
    pub.load_expected(args.manifest)
    print('PASS finalized source-pinned whole-app audit assets and publication manifest')
    return final

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation', 'manifest'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name, value in vars(args).items():
        setattr(args, name, value.resolve())
    finalize(args)

