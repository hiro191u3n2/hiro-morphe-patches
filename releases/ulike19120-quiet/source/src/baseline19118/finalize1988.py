#!/usr/bin/env python3
"""Finalize .88 Finish failure and component diagnostics with current evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1988 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1988 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.88.json'
SOURCE_ZIP = 'ULike_v1.9.88_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1988.py'
    spec = importlib.util.spec_from_file_location('gpu_finish_diagnostics1988_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current=qa['strong_gpu_host_evidence']
    groups='、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。

1. 未計測を速度不足に分類しない
仕上げ候補の実行・比較が完了せず、GPUの速度計測が0回でも速度不足として扱っていた経路を修正しました。速度条件は、独立した2回の全出力一致と2回の実際の出力計測が成立した候補について判定します。利用不可、メモリ、画質不一致、比較未完了、CPU基準異常、取消はその証拠に沿って区別します。既存の非画質失敗の30秒の再試行待ち・最大3回の再試行、保持上限、正しい旧GPUへの復帰と保存済み認定の保護を維持します。

2. 仕上げ候補の停止工程
旧GPUと32／64／128行候補について、前提確認、作業メモリ、セッション、容量予約、初期実行、拡大・回転、ノイズ解析、補正ポリシー、投入、読み戻し、全画素比較などの停止工程と有限の失敗種別を記録します。比較の未完了と画質不一致を区別し、未測定のGPU時間を実測値として表示しません。保存後の検証履歴は撮影本体の時間とは別です。

3. 処理単位別の時間と採用理由
前段の解析とノイズ区間、モデルの元情報・地域解析・縮小モデル、モアレ・拡大／回転・シャープなどを分けて記録します。GPUを採用しなかった理由や検証用コピーの成立／省略は、実際に観測できた範囲だけ記録します。処理単位別時間は並行ワーカーの作業累計を含み、写真全体の経過時間や従来の工程別時間と単純加算できません。背景検証・別の撮影・終了後の更新を混ぜず、固定長のスカラーだけを保持します。画素ループ中にログやUI更新を行いません。

画質・保存・既存修正
NR・美肌・シャープの計算と強度、肌・輪郭・暗部の保護、解像度、色空間・ビット深度・HDR/gainmapの既存の扱い、HEIF設定、単写条件を維持します。全12 nativeペイロード、GPU program 0〜50、JNIとnative登録、認定キー、保存済みの画質不一致記録は公開.87と同じです。独立した2回の全出力一致と、転送・出力取得込みの5%の速度条件を維持します。カメラ・プレビュー・保存フック、他アプリの資源、.87の最初の読み取りでのモデル検証画像取得・利用不可のGPU補正準備省略・Finishコピー前予約も維持します。

検証
{groups} の {len(current['tests'])} 群、計 {current['assertions']:,} assertions を今回実行しました。公開.87での未計測の誤分類を対照に、現行のtyped failure、候補別停止工程、CPU/GPUの実採用、取消・メモリ・旧GPU復帰、診断の非干渉・撮影owner・終了後不変を確認します。凍結した既存回帰は、レビュー済みのfixture adapterとsource逆適用を使用し、今回の実装で実行する挙動検証とは証拠を区別します。過去の検証件数を今回の合計へ加算しません。
ULike 5.6.2（740）の原本APKSへ実際に適用し、split結合、FULL DEX再構築、リソース構築・整列、完成APKの契約・メンバー参照・型・nativeを確認しました。原本適用はローカルで実行済みで、公開CIのMPPをその検証済みMPPと完全一致させます。Android実機での撮影・画質・処理時間の測定は行っていません。今回の診断追加だけで処理時間が短縮したとは主張しません。

更新
Morphe Managerのソースを更新し、ULikeを再適用してください。ログのv{VERSION}表示と、仕上げ候補の停止工程・処理単位別時間を確認してください。単体版v{VERSION}と総合版v{BUNDLE_VERSION}のULike資源は同一です。
利用者の写真、生の実行時ログ、原本APKS、生成APK、署名鍵は配布しません。
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
    require(declaration.get('schema') == 'ulike1988-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1988-desktop-validation-v1' and evidence.get('status') == 'passed'
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
    required = {'src/' + __import__('build1988').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1988.java', 'src/TimingCameraHooks1980.java', 'src/PatchClass1988.java',
        'src/build1988.py', 'src/declare1988.py', 'src/validate1988.py', 'src/finalize1988.py',
        'src/source_scope1988.py', 'src/diagnostic_methods1988.json', 'src/tests1988/source-scope87.json', 'src/production1988.json', 'src/qa_contract1978.json',
        'src/application_evidence1988.py', 'src/apply_original1988.py', 'src/inspect_patched1988.py',
        'src/VerifyAllHelperReferences1980.java', 'src/tests1981/VerifyAppliedTypes1981.java',
        'src/tests1988/original-application.json'}
    contract_sources = json.loads((args.source / 'qa_contract1988.json').read_text())
    required |= {'src/qa_contract1988.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-whole-audit1988-result.json', 'whole-audit-inventory1988.json',
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
    final = {**declaration, 'schema': 'ulike1988-publication-v1', 'qa_required_values': contract,
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

