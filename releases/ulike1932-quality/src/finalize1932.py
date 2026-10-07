#!/usr/bin/env python3
"""Bind rebuilt ULike quality MPPs to exact desktop validation and package reproducible sources."""
from pathlib import Path
import argparse
import json
from build1932 import require, sha, write_zip, SINGLE, bundle_name, config, ROOT

REQUIRED_TOP_SOURCES = {
    'QualityPipeline1932.java', 'QualityShadow1932.java', 'QualityPixels1932.java', 'ShotContext1932.java',
    'Transform1932.java', 'PatchClass1932.java', 'VerifyQuality1932.java', 'MergePayloads.java',
    'VerifyHelperReferences.java', 'VerifyApplied1920.java', 'AnalyzeAll.java',
    'build1932.py', 'validate1932.py', 'finalize1932.py', 'host_quality1932.py', 'SeedShot1932.java',
    'stock-seed1932-watermark.dex', 'stock-seed1932-watermark-hashes.tsv',
    'shot-context-hooks1932.json',
}
BUILD_INPUT_KEYS = (
    'ulike_version', 'bundle_version', 'baseline_ulike_version', 'baseline_ulike_sha256',
    'baseline_ulike_bytes', 'baseline_ulike_url', 'baseline_bundle_version',
    'baseline_bundle_sha256', 'baseline_bundle_bytes', 'baseline_bundle_url', 'toolchain_sha256',
)


def release_notes(qa):
    return f'''ULike v1.9.32 / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【採用した画質改造】
ご指定の候補1・2・8・12・15・20を採用しました。
1：拡大前のノイズ・色ムラ処理と、出力サイズに合わせた最終シャープ処理を分離します。大きな入力を縮小する場合は、折り返しを抑えた縮小を先に行い、処理量を制限します。
2：拡大・縮小に応じた画像補間を追加し、細線の偽模様や輪郭周辺の乱れを抑える構成にします。
8：取得・対応付けできた撮影条件と画像内のノイズ推定から、追加処理の強さを調整します。撮影情報が使えない場合も画像から判断する経路を保持します。
12：肌・暗部の既存処理と追加ノイズ除去の重なりを考慮し、平滑化のかけすぎを抑える処理量調整を追加します。
15：ノイズ量や出力の拡大率などに応じてシャープ量を変え、輪郭を整えながら暗部の粒を強調しすぎないようにします。
20：色模様の周期性・方向の判定を追加し、本来の暖色や滑らかな色を保護しながら色モアレ補正の対象を絞ります。

【基準と保持する改造】
ULike1.9.31、総合版{qa['base_versions'][1]}が基準です。承認済み1.8.8系統を継承します。
前後カメラの記憶、インカメラ・背面のプレビュー復帰、黒帯ダブルタップ、インカメラ時の倍率・接写ボタン非表示など、現行のカメラ寿命管理を保持します。
HEIF保存、既存の画質設定・美顔機能を継承し、総合版内の他アプリの改造を保持します。
NR・シャープのON/OFFと強度段階を引き継ぎ、指定した強度を上限に調整します。輪郭のハロー抑制設定も最終シャープへ反映します。
撤回済み1.9.17系の10bit処理、削除済みの診断表示や原本保管機能は復活させません。

【検証と範囲】
画素処理・撮影情報の扱い・処理順のホスト回帰テスト、DEXの変更範囲、カメラ寿命管理と他アプリの保持を検証しました。
単体・総合版を未改造ULike5.6.2（740）APKSへ適用し、APK再構築、arm64-v8a専用出力、全DEXの整合性、組込メソッドの一致、追加コードと変更対象の型解析を実施しました。
公開ビルドは固定した入力・ツールチェーンから再構築し、元APKSへの適用検証を行ったMPPとSHA-256が一致することを条件にします。
Galaxy実機での撮影結果、見た目の改善量、撮影・保存時間は未確認です。ホストテストの結果は、端末上の性能測定やカメラ動作確認を意味しません。
配布物に未改造APKS、再構築APK、ユーザーの写真や署名鍵は含めません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造ULike5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。単体と総合はどちらか一方を使用します。
ソース更新だけでは、インストール済みULikeの処理は変わりません。アプリデータ削除は不要です。
'''


def eligible_source(path, relative):
    if '__pycache__' in relative.parts:
        return False
    if len(relative.parts) == 1:
        return path.suffix in ('.java', '.py') or path.name == 'shot-context-hooks1932.json' or (path.name.startswith('stock-seed') and path.suffix == '.dex') or path.name.endswith('hashes.tsv')
    folder = relative.parts[0]
    return path.suffix == '.java' and (
        folder in ('quality-stubs', 'quality-host', 'tests') or folder.endswith('-host') or folder.startswith('host-'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    args = parser.parse_args()
    reviewed = config(args.manifest)
    evidence = json.loads(args.validation.read_text())
    require(evidence.get('schema') == 'ulike1932-desktop-validation-v1', 'Wrong validation schema')
    require(evidence.get('original_apk_apply_tested') is True, 'Original APKS application required')
    require(evidence.get('device_tested') is False and evidence.get('device_quality_verified') is False,
            'No hardware quality claim allowed')
    require(evidence.get('bundle_version') == reviewed['bundle_version'], 'Validation bundle differs from manifest')
    qa_path = args.dist / 'QA_ULike_v1.9.32.json'
    qa = json.loads(qa_path.read_text())
    require(qa.get('schema') == 'ulike1932-quality-v1' and qa.get('host_quality_passed') is True,
            'Missing successful quality host validation')
    require(qa.get('selected_candidates') == [1, 2, 8, 12, 15, 20], 'Selected candidates differ')
    combined = bundle_name(qa)
    for name in (SINGLE, combined):
        path = args.dist / name
        require(evidence['artifacts'][name] == {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size},
                'Rebuilt MPP differs from original-APKS-tested bytes: ' + name)
    require(set(evidence.get('desktop_validation', {})) == {'single', 'bundle'}, 'Both APK validations are required')
    for kind in ('single', 'bundle'):
        outcome = evidence['desktop_validation'][kind]
        require(not outcome['patch_result']['failedPatches'] and all(x['success'] for x in outcome['patch_result']['patchingSteps'])
                and outcome['register_analysis']['tested'] > 0 and outcome['register_analysis']['failed'] == 0,
                'Invalid desktop validation result')
        require(outcome.get('dex_integrity_entries', 0) > 0 and outcome.get('native_abis') == ['arm64-v8a'],
                'Missing DEX integrity or architecture verification')
    require(qa['artifacts'] == evidence['artifacts'], 'QA binary mismatch')
    qa.update(status='REBUILD_MATCHES_ORIGINAL_APKS_VALIDATED_MPP_DEVICE_UNVERIFIED',
              original_apk_apply_tested=True, device_tested=False, device_quality_verified=False,
              ci_android_apply_tested=False, desktop_validation=evidence['desktop_validation'],
              quality_dex_verification=evidence['quality_dex_verification'],
              desktop_evidence_sha256=sha(args.validation.read_bytes()))
    if 'stock_lifecycle_abi_verification' in evidence:
        qa['stock_lifecycle_abi_verification'] = evidence['stock_lifecycle_abi_verification']
    sources = {}
    for path in sorted(args.source.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(args.source)
        if eligible_source(path, relative):
            sources['src/' + relative.as_posix()] = path.read_bytes()
    require(all('src/' + name in sources for name in REQUIRED_TOP_SOURCES), 'Incomplete top-level source package')
    require(any(name.startswith('src/quality-stubs/') for name in sources), 'Missing compilation stubs')
    require(any(name.startswith('src/tests/') for name in sources), 'Missing kernel regression test sources')
    # The build manifest deliberately excludes release artifact fingerprints, so
    # the source ZIP does not recursively contain its own hash. The full reviewed
    # publication manifest remains committed in the release's repository folder.
    build_inputs = {'schema': 'ulike1932-build-inputs-v1', **{key: reviewed[key] for key in BUILD_INPUT_KEYS}}
    sources['manifest.json'] = (json.dumps(build_inputs, ensure_ascii=False, indent=2) + '\n').encode()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    (args.dist / 'RELEASE_NOTES.txt').write_text(release_notes(qa))
    package = dict(sources)
    package['README.md'] = (args.source.parent / 'README.md').read_bytes()
    required_reports = ('QA_ULike_v1.9.32.json', 'RELEASE_NOTES.txt', 'emitted-audit.tsv',
                        'quality-dex-verification.txt', 'quality-inventory.json',
                        'helper-references.txt', 'host-quality1932-result.json')
    for name in required_reports:
        package[name] = (args.dist / name).read_bytes()
    for pattern in ('host-quality*.txt', 'host-quality*-metrics.json'):
        for path in args.dist.glob(pattern):
            package[path.name] = path.read_bytes()
    package['evidence/validation.json'] = args.validation.read_bytes()
    evidence_root = args.source.parent / 'evidence'
    for path in sorted(evidence_root.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(evidence_root)
        if any(part in ('tmp', 'classes') for part in relative.parts):
            continue
        if path.suffix in ('.txt', '.tsv') and path.name != 'apply.log':
            package['evidence/' + relative.as_posix()] = path.read_bytes()
    require(not any(Path(name).suffix.casefold() in ('.apk', '.apks', '.aab', '.jks', '.keystore') for name in package),
            'Application binary or signing material in public source package')
    write_zip(args.dist / 'ULike_v1.9.32_sources_and_QA.zip', package)
    files = [SINGLE, combined, 'QA_ULike_v1.9.32.json', 'ULike_v1.9.32_sources_and_QA.zip', 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in files))
    print('PASS release files bound to exact original-APKS-tested MPPs, full source inventory and host evidence; device untested')


if __name__ == '__main__':
    main()
