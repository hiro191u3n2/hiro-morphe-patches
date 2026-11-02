#!/usr/bin/env python3
"""Finalize reproducible H22-H27 assets from exact, fresh desktop evidence."""
from pathlib import Path, PurePosixPath
import argparse
import importlib.util
import json
import re
import zipfile

from build1954 import (
    ROOT, VERSION, BASE_VERSION, BUNDLE_VERSION, BASE_BUNDLE_VERSION,
    BASE_SINGLE_SHA256, BASE_BUNDLE_SHA256, SINGLE, SELECTED, PRODUCTION,
    GPU_ENTRY, CORE_ENTRY, MOIRE_ENTRY, H8_ENTRY, FINISH1952_ENTRY, FINISH_ENTRY, require, sha, bundle_name,
)
from validate1954 import file_pins, HOST_SUITES, CHANGED, ADDED

QA_NAME = 'QA_ULike_v1.9.54.json'
SOURCE_ZIP = 'ULike_v1.9.54_sources_and_QA.zip'
PRIVATE_SUFFIXES = {'.apk', '.apks', '.aab', '.jks', '.keystore', '.pem', '.p12', '.pfx'}
PUBLISHER_FILES = ('publish1954.py', 'ulike1954-h22-h27-publish.yml', 'README.md')


def json_bytes(value, *, sorted_keys=False):
    return (json.dumps(value, ensure_ascii=False, sort_keys=sorted_keys, indent=2) + '\n').encode()


def release_notes(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches 総合版 v{qa['bundle_version']}

【H22～H27の変更】
H22：GPUの処理行数を32行・64行・対象帯域全体から選び、転送と完了待ちを含む同条件の実測と画素一致を確認して採用します。
H23：写真専用の受信経路でGPU結果を専用配列へ直接読み込み、中間出力コピーを削減します。一般APIの失敗時出力契約を保持します。
H24：補助値が一定と証明できる条件では同じ値を再利用し、主ノイズ除去のx・y座標計算を正確な形状キーで再利用します。
H25：GPUに保持済みの周辺画素の範囲を確認し、新しく必要な入力行だけをJava側から送ります。証明できない条件では全入力へ復帰します。
H26：補助値の転送形式が途中で上限を超えた場合、既に丸めた値を可逆に展開して処理を続け、マスクの再計算を削減します。
H27：独立所有を確認できた撮影途中の画像を採用判定中だけ凍結・保持し、入力と比較用画像の複製を削減します。保存、変更、再利用、解放の順序を守り、共有画像には既存の複製を使います。

【維持する動作と検証】
基準は ULike v{BASE_VERSION}／総合版 v{BASE_BUNDLE_VERSION} です。解像度、撮影・合成、ノイズ除去・補正の強度、画素の丸め、圧縮と保存設定を保持します。元のCPU処理への復帰経路を備えています。
撮影・合成・ノイズ・補正・圧縮・保存の工程別時間表示を継続し、表示版を{VERSION}に更新します。工程の所要時間は重なるため、その和が全体経過に一致するとは限りません。
ホスト回帰試験 {qa['host_quality_result']['assertions']:,} アサーションが合格しました。基準版との画素一致、実際のC/JNIとMesaソフトウェアGPUシェーダー、境界・コピー・CPU復帰・保存順序を確認しました。単体と総合版のULikeペイロード、他アプリのリソース保持、arm64-v8aネイティブライブラリ、DEXチェックサム、インストーラの既存行と整合性を確認しています。
元APKSへの今回の適用は未実施です。Galaxy実機での撮影・保存・画質・速度は未確認で、体感速度の改善は測定値として保証できません。元APKS、再構築APK、写真、署名鍵は配布しません。

【適用方法】
Morphe Managerでパッチソースを更新し、未改造 ULike 5.6.2（740）へ「高画質撮影・質感美肌・素材通信を復旧」を再適用して、作成されたアプリを更新インストールしてください。ソース更新のみでは既存のインストール済みアプリは変わりません。単体と総合はどちらか一方を使用します。
'''


def readme(qa):
    return f'''ULike v{VERSION} / Hiro Morphe Patches v{qa['bundle_version']}

H22～H27：処理行数の実測選択、専用GPU受信、補助値と座標の再利用、必要な周辺画素だけの転送、可逆な転送形式展開、凍結した画像所有権による採用判定を統合します。画素結果・撮影・圧縮・保存の設定を保持するホスト検証を実行します。元APKSへの今回の適用は未実施で、実機の速度と画質は未確認です。詳しくは RELEASE_NOTES.txt と {QA_NAME} を参照してください。

再ビルド例：

    python3 src/build1954.py --input INPUT --tools TOOLS --ndk NDK_R27C --work BUILD --output DIST --jdk JDK21
    python3 src/validate1954.py --build BUILD --dist DIST --output VALIDATION.json --input INPUT
    python3 src/finalize1954.py --dist DIST --source src --validation VALIDATION.json --manifest GENERATED-MANIFEST.json

INPUTにはmanifest.jsonの固定チェックサムに対応する ULike v{BASE_VERSION} と総合版 v{BASE_BUNDLE_VERSION} のMPP、および継承試験用の ULike v1.9.52、v1.9.51 と v1.9.50 の単体MPPを置きます。固定のMorphe Desktop、Android SDK、JDK21.0.8、Android NDK r27cとMesa EGL/OpenGL ESが必要です。

Morphe Managerのソース更新後に未改造 ULike 5.6.2（740）へ再適用し、作成されたアプリを更新インストールしてください。
'''.encode()


def eligible(path, relative):
    return (not path.is_symlink()
            and not any(part in ('__pycache__', 'tmp', 'build', 'dist', 'classes')
                        for part in relative.parts)
            and path.suffix in ('.java', '.py', '.json', '.dex', '.tsv', '.txt',
                                '.c', '.h', '.md', '.comp', '.sh'))


def safe_name(name):
    path = PurePosixPath(name)
    require(bool(name) and not path.is_absolute() and '..' not in path.parts
            and '\\' not in name and path.suffix.casefold() not in PRIVATE_SUFFIXES,
            'Unsafe/private source archive path: ' + name)


def source_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_STORED) as output:
        for name in sorted(entries):
            safe_name(name)
            info = zipfile.ZipInfo(name, (2026, 10, 8, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_STORED
            output.writestr(info, entries[name])


def publisher(source):
    path = source.parent / 'publication' / 'publish1954.py'
    spec = importlib.util.spec_from_file_location('ulike1954_publication_contract', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def validate_host(qa, evidence, source, dist):
    require(qa.get('schema') == 'ulike1954-optimization-v1'
            and qa.get('ulike_version') == VERSION
            and qa.get('bundle_version') == BUNDLE_VERSION
            and qa.get('selected_candidates') == SELECTED,
            'Wrong build QA identity')
    result = qa.get('host_quality_result', {})
    suites = result.get('suites', {})
    require(qa.get('host_quality_passed') is True and result.get('status') == 'passed'
            and type(result.get('assertions')) is int and result['assertions'] > 0
            and HOST_SUITES <= suites.keys()
            and result.get('pixel_equivalence_to_baseline') is True
            and result.get('full_resolution_nv21_checked') == '4080x3060'
            and sha(json.dumps(result, sort_keys=True).encode()) == qa.get('host_quality_result_sha256')
            == evidence.get('host_quality_result_sha256')
            and evidence.get('host_assertions') == result['assertions'],
            'Executed host evidence differs from production build')
    require(all(s.get('status') == 'passed' and type(s.get('assertions')) is int
                and s['assertions'] > 0 for s in suites.values())
            and sum(s['assertions'] for s in suites.values()) == result['assertions'],
            'Incomplete host assertion evidence')
    require(json.loads((source / 'production1954.json').read_text()) == PRODUCTION
            and qa.get('replaced_helper_roots') == sorted(PRODUCTION),
            'Current production root inventory differs from compiled Dex')
    pinned_groups = {
        'compiled_production_source_sha256': '',
        'compiled_transformer_source_sha256': '',
        'compiled_gpu_native_source_sha256': 'native1949',
        'compiled_core_native_source_sha256': 'native1950',
        'compiled_finishgpu_native_source_sha256': 'finishgpu1953',
        'compiled_finishgpu1952_native_source_sha256': 'finishgpu1952',
        'executed_host_source_sha256': '',
    }
    for key, prefix in pinned_groups.items():
        require(file_pins(source, qa.get(key), prefix) == evidence.get(key),
                'Current code differs from compiled and desktop verified source: ' + key)
    for key, prefix in (('moire_native_build', 'native1951'),
                        ('h8gpu_native_build', 'h8gpu')):
        require(file_pins(source, qa.get(key, {}).get('sources'), prefix)
                == evidence.get(key + '_sources'),
                'Current native code differs from compiled and desktop verified source: ' + key)
    require(qa.get('production_source_consistency_verified') is True
            and qa.get('compiled_production_source_sha256')
            == {name + '.java': sha((source / (name + '.java')).read_bytes()) for name in PRODUCTION},
            'Production source differs from exact build')
    require(evidence.get('schema') == 'ulike1954-desktop-validation-v1'
            and evidence.get('ulike_version') == VERSION
            and evidence.get('bundle_version') == BUNDLE_VERSION
            and evidence.get('source_consistency_verified') is True,
            'Wrong desktop evidence identity')
    for key in ('original_apk_apply_tested', 'original_split_merge_tested',
                'device_tested', 'device_quality_verified'):
        require(evidence.get(key) is False, 'Unexecuted test claimed: ' + key)
    require(evidence.get('original_apk_application', {}).get('status') == 'not_tested'
            and all(outcome.get('original_apk_apply_tested') is False
                    for outcome in evidence.get('desktop_validation', {}).values())
            and set(evidence.get('desktop_validation', {})) == {'single', 'bundle'},
            'Desktop MPP-only validation must not claim APK application')
    require(evidence.get('runtime_preservation_verification', {}).get('status') == 'passed'
            and evidence.get('runtime_preservation_verification', {}).get('original_apk_apply_tested') is False,
            'Desktop resource preservation evidence absent')
    lease_bytes=(dist/'save-lease-hooks1954.json').read_bytes()
    require(json.loads(lease_bytes)==qa.get('save_lease_hook_audit')
            and sha(lease_bytes)==qa.get('save_lease_hook_audit_sha256')==evidence.get('save_lease_hook_audit_sha256')
            and all(evidence.get('runtime_preservation_verification',{}).get(key)==value for key,value in {
                'save_lease_hooks_inverse_bytecode_verified':True,'save_lease_hooks_serialized_dex_verified':True,
                'save_lease_hooks_target_count':2,'save_lease_hooks_recycle_call_count':4}.items()),
            'Final-save hook proof differs from executed desktop evidence')
    files = (SINGLE, bundle_name(qa))
    artifacts = {name: {'sha256': sha((dist / name).read_bytes()),
                        'bytes': (dist / name).stat().st_size} for name in files}
    require(qa.get('artifacts') == evidence.get('artifacts') == artifacts,
            'Evidence and MPP artifact fingerprints differ')
    for kind in ('standalone', 'bundle'):
        delta = evidence.get('resource_delta', {})
        require(set(delta.get(kind + '_changed', [])) == CHANGED
                and set(delta.get(kind + '_added', [])) == ADDED,
                'Unexpected recorded MPP resource delta: ' + kind)
        require(qa.get('changed_' + kind + '_entries') == sorted(CHANGED)
                and qa.get('added_' + kind + '_entries') == sorted(ADDED),
                'Build MPP resource inventory differs: ' + kind)
    for label in ('gpu', 'core', 'moire', 'h8gpu', 'finishgpu1952', 'finishgpu'):
        require(evidence.get('native_payloads', {}).get(label)
                == {'sha256': qa.get(label + '_native_library_sha256'),
                    'bytes': qa.get(label + '_native_library_bytes')},
                'Native binary fingerprint differs after validation: ' + label)


def finalize(args):
    evidence_bytes = args.validation.read_bytes()
    evidence = json.loads(evidence_bytes)
    qa_path = args.dist / QA_NAME
    qa = json.loads(qa_path.read_text())
    validate_host(qa, evidence, args.source, args.dist)
    pub = publisher(args.source)
    qa.update(status='MPP_DELTA_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED',
              original_apk_apply_tested=False, original_split_merge_tested=False,
              device_tested=False, device_quality_verified=False,
              ci_android_apply_tested=False,
              original_apk_application=evidence['original_apk_application'],
              desktop_validation=evidence['desktop_validation'],
              runtime_preservation_verification=evidence['runtime_preservation_verification'],
              desktop_evidence_sha256=sha(evidence_bytes))
    for key, expected in pub.REQUIRED_QA.items():
        actual = pub.lookup_qa(qa, key)
        require(type(actual) is type(expected) and actual == expected,
                'Final QA contract is not satisfied: ' + key)
    sources = {'src/' + p.relative_to(args.source).as_posix(): p.read_bytes()
               for p in sorted(args.source.rglob('*')) if p.is_file()
               and eligible(p, p.relative_to(args.source))}
    required = {'src/' + name + '.java' for name in PRODUCTION} | {
        'src/build1954.py', 'src/validate1954.py', 'src/finalize1954.py',
        'src/host_regression1954.py', 'src/host_common1954.py', 'src/host_pipeline1954.py', 'src/host_speedpolicy1954.py', 'src/host_pipelinelease1954.py', 'src/production1954.json',
        'src/Transform1954.java', 'src/Verify1954.java', 'src/PatchClass1954.java',
        'src/PatchLoader1954.java', 'src/VerifyHelperReferences.java', 'src/SaveLeaseHooks1954.java',
        'src/native1949/build_native1949.py',
        'src/native1950/build_native1950.py',
        'src/native1951/build_native1951.py',
        'src/h8gpu/build_h8gpu.py',
        'src/finishgpu1953/build_finishgpu1953.py',
        'src/finishgpu1952/build_finishgpu1952.py',
    }
    require(required <= sources.keys(), 'Incomplete H22-H27 reproducible source package')
    require(any('/quality-stubs/' in name for name in sources)
            and any('/tests/timing1947-fixtures/' in name for name in sources)
            and any(name.startswith('src/h8gpu/') and name.endswith('.comp') for name in sources),
            'Compile stubs, timing fixtures or GPU shader source missing')
    static = json.loads((args.source.parent / 'manifest.json').read_text())
    require(static.get('schema') == 'ulike1954-publication-v1'
            and static.get('ulike_version') == VERSION
            and static.get('bundle_version') == BUNDLE_VERSION
            and static.get('baseline_ulike_version') == BASE_VERSION
            and static.get('baseline_bundle_version') == BASE_BUNDLE_VERSION
            and static.get('baseline_ulike_sha256') == BASE_SINGLE_SHA256
            and static.get('baseline_bundle_sha256') == BASE_BUNDLE_SHA256
            and static.get('production_helper_roots') == PRODUCTION
            and static.get('selected_candidates') == SELECTED
            and static.get('original_apk_apply_tested') is False,
            'Reviewed static build declaration differs')
    inputs = {key: value for key, value in static.items()
              if key not in ('schema', 'change_plan_reviewed', 'qa_contract_reviewed')
              and not key.startswith('allowed_')}
    inputs['schema'] = 'ulike1954-build-inputs-v1'
    sources['manifest.json'] = json_bytes(inputs)
    for name in PUBLISHER_FILES:
        path = args.source.parent / 'publication' / name
        require(path.is_file(), 'Publication source missing: ' + name)
        sources['publication/' + name] = path.read_bytes()
    qa['source_sha256'] = {name: sha(data) for name, data in sources.items()}
    qa_path.write_bytes(json_bytes(qa, sorted_keys=True))
    notes = release_notes(qa)
    (args.dist / 'RELEASE_NOTES.txt').write_text(notes)
    package = {**sources, 'README.md': readme(qa),
               'evidence/validation.json': evidence_bytes}
    for p in sorted(args.dist.rglob('*')):
        if p.is_file() and p.suffix in ('.txt', '.log', '.json', '.tsv') and p.name not in (
                'SHA256SUMS.txt', 'publication_ULike_v1.9.54.json'):
            package[p.relative_to(args.dist).as_posix()] = p.read_bytes()
    require(package.get('helper-references.txt', b'').startswith(b'PASS helper references in ')
            and b'PASS\toptimization_roots=' in package.get('optimization-dex-verification.txt', b''),
            'Executed helper reference or incremental Dex audit missing')
    source_zip(args.dist / SOURCE_ZIP, package)
    files = [SINGLE, bundle_name(qa), QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt']
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(
        sha((args.dist / n).read_bytes()) + '  ' + n + '\n' for n in files))
    contract = dict(pub.REQUIRED_QA)
    for key in ('changed_runtime_methods', 'changed_native_methods',
                'new_helper_classes', 'new_runtime_aliases', 'new_native_methods',
                'replaced_helper_roots'):
        require(key in qa, 'Actual DEX change inventory missing: ' + key)
        contract[key] = qa[key]
    contract['host_quality_result.assertions'] = qa['host_quality_result']['assertions']
    contract['original_apk_apply_tested'] = False
    manifest = {
        **inputs, 'schema': 'ulike1954-publication-v1',
        'gpu_native_library_sha256': qa['gpu_native_library_sha256'],
        'gpu_native_library_bytes': qa['gpu_native_library_bytes'],
        'core_native_library_sha256': qa['core_native_library_sha256'],
        'core_native_library_bytes': qa['core_native_library_bytes'],
        'moire_native_library_sha256': qa['moire_native_library_sha256'],
        'moire_native_library_bytes': qa['moire_native_library_bytes'],
        'h8gpu_native_library_sha256': qa['h8gpu_native_library_sha256'],
        'h8gpu_native_library_bytes': qa['h8gpu_native_library_bytes'],
        'finishgpu1952_native_library_sha256': qa['finishgpu1952_native_library_sha256'],
        'finishgpu1952_native_library_bytes': qa['finishgpu1952_native_library_bytes'],
        'finishgpu_native_library_sha256': qa['finishgpu_native_library_sha256'],
        'finishgpu_native_library_bytes': qa['finishgpu_native_library_bytes'],
        'ndk_revision': qa['gpu_native_build']['ndk_revision'],
        'android_device_tested': False, 'original_apk_apply_tested': False,
        'change_plan_reviewed': True, 'qa_contract_reviewed': True,
        'qa_required_values': contract,
        'required_source_paths': sorted(n for n in sources if n.startswith('src/')),
        'artifacts': {name: {'bytes': (args.dist / name).stat().st_size,
                             'sha256': sha((args.dist / name).read_bytes())}
                      for name in files + ['SHA256SUMS.txt']},
    }
    for kind in ('standalone', 'bundle'):
        manifest['allowed_changed_' + kind + '_entries'] = qa['changed_' + kind + '_entries']
        manifest['allowed_added_' + kind + '_entries'] = qa['added_' + kind + '_entries']
    for key in ('allowed_changed_standalone_entries', 'allowed_added_standalone_entries',
                'allowed_changed_bundle_entries', 'allowed_added_bundle_entries'):
        require(manifest[key] == static[key], 'Generated delta differs from reviewed declaration: ' + key)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_bytes(json_bytes(manifest))
    pub.load_expected(args.manifest)
    print('PASS H22-H27 host/source consistency, exact MPP delta, deterministic package and pinned publication manifest; original APKS and device untested')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('dist', 'source', 'validation'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path)
    args = parser.parse_args()
    for name, value in vars(args).items():
        if value is not None:
            setattr(args, name, value.resolve())
    if args.manifest is None:
        args.manifest = args.dist / 'generated-manifest1954.json'
    finalize(args)


if __name__ == '__main__':
    main()
