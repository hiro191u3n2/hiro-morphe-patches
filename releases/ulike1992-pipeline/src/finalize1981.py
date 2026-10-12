#!/usr/bin/env python3
"""Finalize the .81 capture-to-save routes with fresh host and original-APKS evidence."""
from pathlib import Path, PurePosixPath
import argparse, importlib.util, json, zipfile
from build1981 import (ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    SINGLE, BUNDLE, SELECTED, PRODUCTION, CHANGED, ADDED, PUBLISHER_FILES,
    require, sha, json_bytes, source_pins, publication_pins)
from validate1981 import host_checks, SOURCE_GROUPS

QA_NAME = 'QA_ULike_v1.9.81.json'
SOURCE_ZIP = 'ULike_v1.9.81_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx',
    '.png', '.jpg', '.jpeg', '.heic', '.heif', '.pyc'}

def publisher(source):
    path = source.parent / 'publication/publish1981.py'
    spec = importlib.util.spec_from_file_location('whole_audit1981_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def release_notes(qa):
    current = qa['strong_gpu_host_evidence']
    groups = '、'.join(sorted(current['tests']))
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{BUNDLE_VERSION}
基準：公開済み ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION}。
採用範囲：案 S1〜S7 と R1〜R5。撮影から保存までの待ち、重複作業、画像の保持時間を減らす改修です。

S1 保存準備の先行
保存先の登録とファイル記述子の取得を先行し、エンコーダの生成だけを利用権で待たせます。前のエンコーダが実際に解放された通知で、次の写真の準備を一度だけ再開します。中断時は未公開の保存先と準備タスクを回収します。

S2 メモリ寿命に基づく受付
Java ヒープ、システムメモリ、実際の画像サイズ、保持中の画像とこれから必要な領域を分けて見積もります。システムの使用量へ既に反映された画像の二重計上を避けます。不明な容量やメモリ不足時には保守的な制限を残し、保存キュー上限 3 と SDK 直列実行を維持します。通常 4080×3060 と固定設定 5712×4284 を混同しません。

S3 完了時間による CPU/GPU 選択
準備・待ち・転送・読み戻しを含む完了時間で選択します。通常 Strong は画質認定に加えて 5% 以上の速度条件を要求します。利用枠の予測待ちが残りの時間予算を超える場合は CPU を使用し、同じ写真で観測した遅延も判断へ反映します。

S4 保存末尾の画像保持を短縮
writer とコールバックの終了、実際のエンコーダ解放を確認してから、ファイル検査前に不要な最終画像を解放します。元画像と検証中の画像は保持し、実際に解放できた分だけメモリ予約を減らします。

S5 予備 NR の資源再利用
同じ写真・モデルの GPU セッションとモデル転送、残差処理バッファの容量を再利用します。利用中の所有者を他のワーカーが待つことはありません。再利用経路は初回から独立した画質照合と速度条件で認定でき、従来経路の速度判定に阻まれません。従来の不一致記録は実行前と出力の確定前にも確認します。

S6 撮影中の追加検証を制限
初回 Strong の重い画質比較を待機時間へ移し、撮影中に作るコピーを制限します。GPU の全画像コピーは写真ごとに 1 件、新しい NR 再利用候補と Strong の認定が共有する帯コピーは通常合計 32 MiB までで、完全な最初の帯に対する例外も総量 96 MiB 以内に制限します。検証の種類へ順番を回し、中断後も新しい写真から認定が進むようにします。独立した 2 回の全出力一致、不一致の拒否、保存済み認定を保持します。

S7 初回準備を軽量化
プレビューの描画確認後にコンストラクタのメタデータと準備ワーカーを用意します。ダミー撮影、美肌処理の空実行、可変 SDK オブジェクトの先行生成は追加しません。

R1 CPU 退避経路を統一
予備 NR の CPU 処理へ呼び出し元の作業領域を渡し、比較処理には独立した作業領域を用意します。速くなった CPU との比較記録は、既存 GPU の画質認定・不一致記録から分離します。

R2 GPU 後段の容量を先に確認
Strong 実行前に、変形・仕上げ・保護係数で同時に必要な容量を確認して予約します。実行直前の再確認、キャンセルと CPU 退避を維持します。R6 の段階別再開は実装していません。

R3 マスク生成前に経路を固定
選択した CPU/GPU 経路に必要な保護マスクを一度だけ生成します。量子化済みの値から元マスクを近似的に復元する変更はありません。

R4 CPU のモアレ処理とシャープを帯単位で連続実行
中間の整数 ARGB 丸め、隣接画素、帯の境界を維持します。変形のない所有画像・不透明 sRGB ARGB8888 を対象とし、独立した 2 回の全画素一致と、不透明確認も含めた 5% 以上の速度条件を満たした場合だけ利用します。適用条件外は元の処理を継続します。

R5 同じ写真の先行解析
補正待ちの所有画像を読み取り専用で解析します。画像・回転・設定・撮影メタデータが一致した顔と処理前ノイズの結果だけを再利用します。正常な顔ゼロと解析失敗を区別し、失敗時は元の解析を行います。縮小後やフィルタ後の推定は従来どおりです。

画質・互換性
解像度、NR・美肌・シャープの強さ、肌・輪郭・暗部の保護、色空間・ビット深度・HDR/gainmap の扱い、保存形式と HEIF 設定、単写条件を維持します。全 12 native ペイロード、GPU program 0〜50、native 登録と JNI は基準配布物とバイト単位で同一です。既存の GPU 認定と不一致記録を一括無効化しません。
前版の GPU利用再開と失敗分類、保存エラー後の後始末、カメラの映像待機、未使用経路の整理も引き継ぎます。

今回の検証
{groups} の {len(current['tests'])} 群、計 {current['assertions']:,} 件を今回のソースで実行しました。全画素・整数中間結果・実際のホスト JNI/GLES、所有権、待機、取消、メモリ、認定の永続化と旧版の具体的な故障を検査します。過去の数値試験件数は今回へ加算しません。
元APKSへの適用を確認：ULike 5.6.2（740）の原本 APKS へ実際に適用し、split 結合、FULL DEX 再構築、リソース構築・整列、完成 APK の契約・メンバー参照・型・native を検査しました。この検査対象と公開 CI が再構築する MPP の完全一致を必須にします。原本適用はローカルの検査であり、CI が同じ適用を再実行したとは表示しません。
Android 実機の撮影・保存画像は未確認、体感速度は未測定です。ホスト試験の時間を Galaxy の短縮時間として表示しません。

更新と診断
Morphe Manager のソース更新後、ULike を再適用してください。単体版 v{VERSION} と総合版 v{BUNDLE_VERSION} の ULike 資源は同一です。他アプリ資源と既存の更新履歴を維持し、新版適用後に同じ更新が未適用表示にならないことを確認します。
既存の「直近の撮影・工程別処理時間」とログ共有を利用できます。公開ファイルには利用者の写真、実行時ログ、原本 APKS、生成 APK、署名鍵を含めません。
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
    require(declaration.get('schema') == 'ulike1981-build-declaration-v1'
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
    require(evidence.get('schema') == 'ulike1981-desktop-validation-v1' and evidence.get('status') == 'passed'
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
    required = {'src/' + __import__('build1981').production_path(name).relative_to(args.source).as_posix() for name in PRODUCTION} | {
        'src/Transform1981.java', 'src/TimingCameraHooks1980.java', 'src/PatchClass1981.java',
        'src/build1981.py', 'src/declare1981.py', 'src/validate1981.py', 'src/finalize1981.py',
        'src/source_scope1981.py', 'src/production1981.json', 'src/qa_contract1978.json',
        'src/application_evidence1981.py', 'src/apply_original1981.py',
        'src/VerifyAllHelperReferences1980.java', 'src/tests1981/VerifyAppliedTypes1981.java',
        'src/tests1981/original-application.json'}
    contract_sources = json.loads((args.source / 'qa_contract1981.json').read_text())
    required |= {'src/qa_contract1981.json'} | {
        'src/' + name + '.py' for _, name in contract_sources['runners']}
    require(required <= set(sources), 'Current production/test source graph missing from archive')
    qa.update(desktop_evidence_sha256=sha(evidence_bytes), desktop_validation=evidence,
        source_sha256={name: sha(data) for name, data in sources.items()})
    qa_path.write_bytes(json_bytes(qa))
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = {**sources, 'README.md': sources['publication/README.md'], 'evidence/validation.json': evidence_bytes}
    for name in (QA_NAME, 'host-whole-audit1981-result.json', 'whole-audit-inventory1981.json',
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
    final = {**declaration, 'schema': 'ulike1981-publication-v1', 'qa_required_values': contract,
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

