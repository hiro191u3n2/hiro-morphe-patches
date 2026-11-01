#!/usr/bin/env python3
"""Finalize .82 GPU diagnostic observations with fresh host and original-APKS evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1982 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1982 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.82.json'
SOURCE_ZIP = 'ULike_v1.9.82_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1982.py'
    spec = importlib.util.spec_from_file_location('gpu_diagnostics1982_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current = qa['strong_gpu_host_evidence']
    groups = '、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。
GPU の利用状況と処理時間を判断しやすくする診断の更新です。現行の画質・処理経路・保存設定を保持します。

GPU 経路と終了理由
Strong の GPU 採用、画質認定待ち、速度条件、利用枠の待機・拒否を区別します。画質認定済みでも速度条件を満たさない場合を、画質未認定と混同しません。画像を GPU に保持する経路の成功・CPU 退避・中断・失敗も、終了した時点の状態として表示します。

採用した処理と作業累計
実際に結果を採用した CPU/GPU 処理の件数を記録します。GPU を試しただけで失敗した処理や、バックグラウンドの画質照合を、その写真で採用した GPU 処理に加算しません。未計測の作業時間は未計測と表示します。Strong の既存の作業累計は保持し、全工程の失敗試行を新たに独立計測したものとは扱いません。並行処理や採用されなかった候補の時間を含む場合があり、工程の経過時間や成功した GPU 出力の時間とは一致しません。GPU 枠内の待機・判定作業と、GPU 枠受付ロックの取得待ちを分けて確認できます。

認定状態の表示
検証の予約・実行中、直近照会キャッシュの確認済み記録・画質不一致の拒否、画質不一致以外の履歴における再試行の時間条件・間隔待ち・上限を分けて表示します。診断画面を開いたことを理由に GPU 認定や処理経路を変えません。確定済みの写真の記録は、後の状態変化で書き換えません。

画質と既存動作
画素の計算式、NR・美肌・シャープの強度、解像度、肌・輪郭・暗部の保護、色空間・ビット深度・HDR/gainmap の扱い、HEIF と保存形式、単写条件を維持します。保存先の先行準備、画像の所有権、メモリ制限、CPU 退避、キャンセル、独立した 2 回の全出力一致と速度条件、保存済みの認定・不一致記録も保持します。全 12 native ペイロード、GPU program 0〜50、native 登録と JNI は基準 .81 配布物とバイト単位で同一です。
前版の GPU利用再開と失敗分類、カメラの映像待機、初回の軽量な準備、未使用経路の整理を引き継ぎます。保存用 DEX フックは .81 の既適用結果をそのまま保持し、重ねて追加しません。

今回の検証
{groups} の {len(current['tests'])} 群、計 {current['assertions']:,} assertions を今回のソースで実行しました。既存の画質・経路・保存・所有権・取消の回帰に、診断の理由・件数・確定記録・失敗時の表示を追加しています。過去の検証件数は今回の合計へ加算しません。
元APKSへの適用を確認：ULike 5.6.2（740）の原本 APKS へ実際に適用し、split 結合、FULL DEX 再構築、リソース構築・整列、完成 APK の契約・メンバー参照・型・native を検査しました。ローカルで検査した MPP と公開 CI が再構築する MPP の完全一致を必須にします。原本適用を CI が再実行したとは表示しません。
Android 実機の撮影・保存画像・表示は未確認、体感速度は未測定です。今回の診断追加だけで実機の GPU 不使用の原因が確定した、または撮影時間が短縮したとは扱いません。

更新と診断
Morphe Manager のソース更新後、ULike を再適用してください。単体版 v{VERSION} と総合版 v{BUNDLE_VERSION} の ULike 資源は同一です。他アプリ資源と既存の更新履歴を維持します。
既存の「直近の撮影・工程別処理時間」とログ共有から診断を確認できます。全体経過は記録開始からの時間で、必ずしもシャッター入力からの時間ではありません。native の標準美顔と撮影対象フレーム待ちは個別工程の計測外です。
公開ファイルには利用者の写真、実行時ログ、原本 APKS、生成 APK、署名鍵を含めません。
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
            info = zipfile.ZipInfo(name, (2026, 10, 10, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            output.writestr(info, entries[name])

def finalize(args):
    pub = publisher(args.source)
    declaration = json.loads((args.source.parent / 'manifest.json').read_text())
    pub.configure(declaration)
    require(declaration.get('schema') == 'ulike1982-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1982-desktop-validation-v1' and evidence.get('status') == 'passed'
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
    required = {'src/' + __import__('build1982').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1982.java', 'src/TimingCameraHooks1980.java', 'src/PatchClass1982.java',
        'src/build1982.py', 'src/declare1982.py', 'src/validate1982.py', 'src/finalize1982.py',
        'src/source_scope1982.py', 'src/diagnostic_methods1982.json', 'src/tests1982/source-scope81.json', 'src/production1982.json', 'src/qa_contract1978.json',
        'src/application_evidence1982.py', 'src/apply_original1982.py', 'src/inspect_patched1982.py',
        'src/VerifyAllHelperReferences1980.java', 'src/tests1981/VerifyAppliedTypes1981.java',
        'src/tests1982/original-application.json'}
    contract_sources = json.loads((args.source / 'qa_contract1982.json').read_text())
    required |= {'src/qa_contract1982.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-whole-audit1982-result.json', 'whole-audit-inventory1982.json',
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
    final = {**declaration, 'schema': 'ulike1982-publication-v1', 'qa_required_values': contract,
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

