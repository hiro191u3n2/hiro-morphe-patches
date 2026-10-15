#!/usr/bin/env python3
"""Finalize .89 Finish failure and component diagnostics with current evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1989 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1989 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.89.json'
SOURCE_ZIP = 'ULike_v1.9.89_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1989.py'
    spec = importlib.util.spec_from_file_location('gpu_finish_memory1989_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current=qa['strong_gpu_host_evidence']
    groups='、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。

1. 全候補終了後のCPU再計算を省略
旧GPU・新32／64／128行の全候補が初回で終了した場合、次のCPU基準画像のコピー・処理・全画素digestを省きます。旧GPUを含め一つでも継続可能な候補があれば2回目を実行します。2回の独立した全出力一致と2回の実出力速度計測、取消確認、失敗分類、不一致の保存、参照画像とセッションの後処理は維持します。省略対象は保存後の背景検証であり、撮影本体の経過時間からCPU基準時間を差し引けるという意味ではありません。

2. 同じ予算上限で一度再確認
保存後のFinishで追加作業メモリを拒否された場合、回収可能な未使用native scratchを安全に解放して再判定します。使用中の画像・GPUセッション・CPU作業領域の所有権を維持し、512MiBのGPU作業上限、64MiBの余裕、96MiBの検証保持上限を引き上げません。回収の時間も実出力速度の計測範囲に含めます。要求量、使用可能量、ヒープ上限・使用量、各native所有量、予約、回収前後を記録します。背景検証の撮影ID・世代、入力と出力の寸法、CPU基準の実行・省略回数も記録し、現在の写真の時間と背景履歴を区別します。必要な予算を確保できない場合は従来どおり保留します。

3. 実際のCPU選択理由
前段のSingle／Residualと補正のモアレ・回転リサイズ・くっきりでCPUを選んだ理由を、その判定分岐からログへ渡します。GPU候補切替の原因は、最終出力のCPU／GPU採用とは別に記録します。モデル検証入力は予約要求量と取得済み画素量を分け、実際の予約判断と待ち時間・件数・保持量を記録します。診断のために認定やキュー判断を再実行しません。固定長のスカラーだけを保持し、背景作業・別の撮影・終了後の更新を混ぜません。画素ループ中にログやUI更新を行いません。

画質・保存・既存修正
NR・美肌・シャープの計算と強度、肌・輪郭・暗部の保護、解像度、色空間・ビット深度・HDR/gainmapの既存の扱い、HEIF設定、単写条件を維持します。全12 nativeペイロード、GPU program 0〜50、JNIとnative登録、認定キー、保存済みの画質不一致記録は公開.88と同じです。独立した2回の全出力一致と、転送・出力取得込みの5%の速度条件、非画質失敗の30秒待ちと最大3回の再試行を維持します。カメラ・プレビュー・保存フック、他アプリの資源、既存の検証コピー予約・モデル入力の取得・利用不可時の不要準備省略を維持します。

検証
{groups} の {len(current['tests'])} 群、計 {current['assertions']:,} assertions を今回実行しました。公開.88の不要CPU処理を対照に、現行の全候補終了・残存候補の認定、予算と回収、実CPU選択理由、取消・メモリ・旧GPU復帰、診断故障の非干渉・撮影ごとの分離・終了後不変を確認します。凍結した既存回帰はレビュー済みの互換処理を通して現行コードを実行し、source逆適用は保存確認専用です。過去の検証件数を今回の合計へ加算しません。
ULike 5.6.2（740）の原本APKSへ実際に適用し、split結合、FULL DEX再構築、リソース構築・整列、完成APKの契約・メンバー参照・型・nativeを確認しました。原本適用はローカルで実行済みで、公開CIのMPPをその検証済みMPPと完全一致させます。Android実機での撮影・画質・処理時間の測定は行っていません。回収できる容量と撮影時間への効果は実機条件によって変わり、短縮時間は未確認です。

更新
Morphe Managerのソースを更新し、ULikeを再適用してください。ログのv{VERSION}表示と、FinishのCPU基準回数・予算内訳・CPU選択理由を確認してください。単体版v{VERSION}と総合版v{BUNDLE_VERSION}のULike資源は同一です。
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
    require(declaration.get('schema') == 'ulike1989-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1989-desktop-validation-v1' and evidence.get('status') == 'passed'
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
    required = {'src/' + __import__('build1989').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1989.java', 'src/TimingCameraHooks1980.java', 'src/PatchClass1989.java',
        'src/build1989.py', 'src/declare1989.py', 'src/validate1989.py', 'src/finalize1989.py',
        'src/source_scope1989.py', 'src/diagnostic_methods1989.json', 'src/tests1989/source-scope88.json', 'src/production1989.json', 'src/qa_contract1978.json',
        'src/application_evidence1989.py', 'src/apply_original1989.py', 'src/inspect_patched1989.py',
        'src/VerifyAllHelperReferences1980.java', 'src/tests1981/VerifyAppliedTypes1981.java',
        'src/tests1989/original-application.json'}
    contract_sources = json.loads((args.source / 'qa_contract1989.json').read_text())
    required |= {'src/qa_contract1989.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-whole-audit1989-result.json', 'whole-audit-inventory1989.json',
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
    final = {**declaration, 'schema': 'ulike1989-publication-v1', 'qa_required_values': contract,
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

