#!/usr/bin/env python3
"""Rebuild the reviewed Trip.com patch and bundle162 from immutable bundle161."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_SHA = '92d1a60ed74859eca0f9d24413167d4b792031fbd50c6a1495a608d4043b2d03'
SINGLE_BASE_SHA = 'e99d82b7d5250662c82c3a50463ac9cbd551d56db299c8837c9056cfcdaef3e4'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
BUNDLE = 'Hiro_Morphe_Patches_v1.0.162.mpp'
SINGLE = 'Tripcom_Quiet_v1.10.15.mpp'
QA = 'QA_Tripcom_v1.10.15.json'
SOURCE = 'Tripcom_v1.10.15_sources_and_QA.zip'
MF = 'META-INF/MANIFEST.MF'
PATCH_PREFIX = 'app/hiro/tripcom/patches/'
RUNTIME_PREFIX = 'app/hiro/tripcom/extension/'
EXTENSION = 'extensions/myplan_bundle_guard.mpe'
REPLACEMENT = 'replacements/rn_xtaro_ibu_schedule-431156475-30041599.7z'
DATE = (2026, 10, 7, 0, 0, 0)
HOME_NAME = 'ホームおすすめ欄・AI旅行計画欄を非表示（クラッシュ修正版）'


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def run(command, log, timeout=300):
    with Path(log).open('wb') as stream:
        result = subprocess.run(list(map(str, command)), stdout=stream, stderr=subprocess.STDOUT, timeout=timeout)
    output = Path(log).read_text(errors='replace')
    if result.returncode:
        print(output[-10000:])
        raise RuntimeError('Command failed; log=' + str(log))
    return output


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt ZIP: ' + str(path))
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP paths')
        for name in z.namelist():
            require(not Path(name).is_absolute() and '..' not in Path(name).parts and '\\' not in name,
                    'Unsafe ZIP path')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda n: (n != MF, n)):
            info = zipfile.ZipInfo(name, DATE)
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Manifest continuation without header')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    fields = dict(row.decode().split(': ', 1) for row in rows)
    require(len(fields) == len(rows), 'Duplicate manifest headers')
    return fields


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest field')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    raw = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(raw) == fields, 'Manifest round-trip failed')
    return raw


def class_files(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}


def family_member(name, families):
    return any(name == family + '.class' or name.startswith(family + '$') for family in families)


def build(args):
    require(not args.work.exists(), 'Use a fresh build directory')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)
    require(sha(args.base.read_bytes()) == BASE_SHA, 'Active bundle161 baseline differs')
    require(sha(args.single_base.read_bytes()) == SINGLE_BASE_SHA, 'Trip14 standalone baseline differs')
    for name, digest in TOOLS.items():
        require(sha((args.tools / name).read_bytes()) == digest, 'Tool checksum differs: ' + name)
    baseline, previous_single = archive(args.base), archive(args.single_base)
    require(headers(baseline[MF])['Version'] == '1.0.161', 'Wrong integrated baseline version')
    require(headers(previous_single[MF])['Version'] == '1.10.14', 'Wrong Trip baseline version')
    old_own = {n: raw for n, raw in previous_single.items() if n not in (MF, 'classes.dex')}
    require(all(baseline.get(n) == raw for n, raw in old_own.items()), 'Trip14 resources differ from bundle161')

    (args.work / 'old.7z').write_bytes(baseline[REPLACEMENT])
    run([sys.executable, ROOT / 'js/patch_myplan.py', args.work / 'old.7z', args.work / 'new.7z',
         '--bundle-output', args.work / 'rn_business.jsbundle', '--report', args.work / 'js-build.json'],
        args.work / 'js-build.log')
    # Read the actual old archive through the same strict member parser for a
    # baseline/candidate render comparison. Test subjects come from MPP bytes.
    import importlib.util
    spec = importlib.util.spec_from_file_location('trip_js_patch', ROOT / 'js/patch_myplan.py')
    js_patch = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(js_patch)
    (args.work / 'old.jsbundle').write_bytes(js_patch.read_archive(args.work / 'old.7z'))
    run(['node', ROOT / 'js/test_myplan.js', args.work / 'old.jsbundle', args.work / 'rn_business.jsbundle',
         args.work / 'js-render.json'], args.work / 'js-render.log')
    run([sys.executable, ROOT / 'native/run_tests.py', '--tools', args.tools,
         '--bundle', args.work / 'rn_business.jsbundle',
         '--work', args.work / 'native-tests'], args.work / 'native-tests.log')
    run([sys.executable, ROOT / 'resource/run_tests.py', '--baseline', args.base,
         '--tools', args.tools, '--work', args.work / 'resource-tests'],
        args.work / 'resource-tests.log')

    folders = ('runtimeclasses', 'patchclasses', 'auditclasses', 'runtime-dex', 'patch-dex')
    for name in folders:
        (args.work / name).mkdir()
    runtime_sources = sorted((ROOT / 'native/runtime').rglob('*.java'))
    patch_sources = sorted((ROOT / 'native/patch').rglob('*.java'))
    require(runtime_sources and patch_sources, 'Native implementation incomplete')
    common = ['javac', '--release', '8', '-encoding', 'UTF-8']
    run([*common, '-cp', args.tools / 'android.jar', '-d', args.work / 'runtimeclasses', *runtime_sources],
        args.work / 'runtime-javac.log')
    run([*common, '-cp', args.tools / 'morphe.jar', '-d', args.work / 'patchclasses', *patch_sources],
        args.work / 'patch-javac.log')
    run([sys.executable, ROOT / 'resource/prepare_home_jvm.py', '--baseline', args.base,
         '--classes', args.work / 'patchclasses', '--report', args.work / 'home-jvm.json'],
        args.work / 'home-jvm.log')
    run(['javac', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar', '-d', args.work / 'auditclasses',
         ROOT / 'audit/LoaderMerge.java', ROOT / 'resource/HomeClosureDex.java'], args.work / 'audit-javac.log')
    runtime, patch = class_files(args.work / 'runtimeclasses'), class_files(args.work / 'patchclasses')
    require(runtime and all(n.startswith(RUNTIME_PREFIX) for n in runtime), 'Unexpected runtime class or test stub')
    require(patch and all(n.startswith(PATCH_PREFIX) for n in patch), 'Unexpected patch loader class')
    families = [p.relative_to(ROOT / 'native/patch').with_suffix('').as_posix() for p in patch_sources]
    home_closure = PATCH_PREFIX + 'HideHomeRecommendationsPatchKt$hideHomeRecommendationsPatch$1$1'
    families.append(home_closure)
    require(all(n.startswith(PATCH_PREFIX) for n in families), 'Source family outside Trip namespace')
    require(all(family_member(n, families) for n in patch), 'Compiled loader outside source families')
    (args.work / 'families.txt').write_text(''.join('L' + family + '\n' for family in sorted(families)))
    write_zip(args.work / 'runtime.jar', runtime)
    # This existing Kotlin closure is preserved from the baseline DEX with two
    # exact invokes changed below; do not recompile its metadata through D8.
    write_zip(args.work / 'patch.jar', {n: raw for n, raw in patch.items() if n != home_closure + '.class'})
    for kind, classpath in (('runtime', args.tools / 'android.jar'), ('patch', args.tools / 'morphe.jar')):
        run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
             '--lib', args.tools / 'android.jar', '--classpath', classpath,
             '--output', args.work / (kind + '-dex'), args.work / (kind + '.jar')], args.work / (kind + '-d8.log'))
        require({p.name for p in (args.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex output')
    (args.work / 'baseline.dex').write_bytes(baseline['classes.dex'])
    (args.work / 'previous-single.dex').write_bytes(previous_single['classes.dex'])
    audit_cp = os.pathsep.join(map(str, (args.work / 'auditclasses', args.tools / 'morphe.jar')))
    run(['java', '-cp', audit_cp, 'HomeClosureDex', args.base, args.work / 'patch-dex/classes.dex',
         args.work / 'home-fixed.dex', args.work / 'home-dex.json'], args.work / 'home-dex.log')
    run(['java', '-cp', audit_cp, 'LoaderMerge', args.work / 'baseline.dex', args.work / 'previous-single.dex',
         args.work / 'home-fixed.dex', args.work / 'families.txt', args.work / 'bundle.dex',
         args.work / 'single.dex', args.work / 'loader-merge.json'], args.work / 'loader-merge.log')

    addition = {**patch, EXTENSION: (args.work / 'runtime-dex/classes.dex').read_bytes(),
                REPLACEMENT: (args.work / 'new.7z').read_bytes()}
    def update(entries, version, dex_name, description):
        out = {n: raw for n, raw in entries.items() if not family_member(n, families)}
        out.update(addition)
        fields = headers(entries[MF])
        fields.update(Version=version, Timestamp='2026-10-07T00:00:00', Description=description)
        out[MF] = manifest(fields)
        out['classes.dex'] = (args.work / dex_name).read_bytes()
        return out
    standalone = update(previous_single, '1.10.15', 'single.dex',
        'v1.10.15 My Plan: pin checked bundled screen at native load, show complete flight times and duration, remove scenic heading. Trip8.54.2. Device untested.')
    combined = update(baseline, '1.0.162', 'bundle.dex',
        'Trip.com v1.10.15: complete My Plan departure times; remove scenic heading; use checked bundled screen despite cached CRN. All other bundle161 patches retained. Device untested.')
    own = {n: raw for n, raw in standalone.items() if n not in (MF, 'classes.dex')}
    require(all(combined.get(n) == raw for n, raw in own.items()), 'Single/bundle Trip resources differ')
    unaffected = {n: raw for n, raw in baseline.items() if n not in old_own and n not in (MF, 'classes.dex')}
    require(all(combined.get(n) == raw for n, raw in unaffected.items()), 'Another application resource changed')
    require(set(combined) - set(baseline) <= set(own), 'Unexpected additional bundle resource')
    artifacts = {}
    for name, entries in ((BUNDLE, combined), (SINGLE, standalone)):
        output = args.dist / name
        write_zip(output, entries)
        require(archive(output) == entries, 'Written MPP differs')
        listing = run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', output,
                       '-f', 'ctrip.english', '-p', '-v'], args.work / (name + '.list.txt'))
        require(HOME_NAME in listing and '8.54.2' in listing, 'Morphe cannot discover expected Trip patch')
        artifacts[name] = {'bytes': output.stat().st_size, 'sha256': sha(output.read_bytes())}
    evidence = {
        'schema': 'trip162-build-v1', 'bundle_version': '1.0.162', 'patch_version': '1.10.15',
        'baseline_bundle_version': '1.0.161', 'baseline_sha256': BASE_SHA, 'toolchain_sha256': TOOLS,
        'supported_package': 'ctrip.english', 'supported_version': '8.54.2',
        'non_trip_resources_unchanged': True, 'non_trip_loader_classes_unchanged': True,
        'existing_non_trip_zip_entries_preserved': len(unaffected),
        'standalone_and_bundle_trip_resources_identical': True,
        'standalone_and_bundle_trip_loaders_identical': True,
        'replacement_patch_families': families, 'runtime_extension': EXTENSION,
        'runtime_stubs_packaged': False, 'device_tested': False,
        'js_build': json.loads((args.work / 'js-build.json').read_text()),
        'js_render': json.loads((args.work / 'js-render.json').read_text()),
        'loader_merge': json.loads((args.work / 'loader-merge.json').read_text()),
        'home_jvm_redirect': json.loads((args.work / 'home-jvm.json').read_text()),
        'home_dex_redirect': json.loads((args.work / 'home-dex.json').read_text()),
        'home_resource_regression': json.loads((args.work / 'resource-tests/home-resource.json').read_text()),
        'native_host_test_log': (args.work / 'native-tests.log').read_text(),
        'artifacts': artifacts,
    }
    (args.dist / 'build-evidence.json').write_bytes(json_bytes(evidence))
    print(json.dumps({'status': 'BUILT_AND_HOST_VERIFIED', 'artifacts': artifacts}, indent=2))


def package_outputs(args):
    local_path = ROOT / 'local-qa.json'
    if not local_path.exists():
        print('MPP build complete. Record actual original-APK validation before release packaging.')
        return
    local = json.loads(local_path.read_text())
    evidence = json.loads((args.dist / 'build-evidence.json').read_text())
    require(local['original_apk_apply_tested'] is True and local['device_tested'] is False, 'Invalid QA claims')
    require(local['artifacts'] == evidence['artifacts'], 'Actual APK validation belongs to different MPP bytes')
    expected = json.loads((ROOT / 'expected-mpp.json').read_text())
    require(expected == evidence['artifacts'], 'Independent MPP hashes differ')
    for name, record in expected.items():
        raw = (args.dist / name).read_bytes()
        require(len(raw) == record['bytes'] and sha(raw) == record['sha256'], 'Built file hash mismatch')
    reviewed_path = ROOT / 'reviewed-qa.json'
    require(reviewed_path.exists(), 'Record independently reviewed original-APK and host QA before packaging')
    reviewed_raw = reviewed_path.read_bytes()
    qa = json.loads(reviewed_raw)
    require(qa.get('schema') == 'trip162-qa-v1' and qa.get('result') == 'PASS'
            and qa.get('blocking_findings') == [], 'Reviewed QA is not a passing report')
    require(qa.get('artifacts') == expected and qa.get('original_apk_evidence') == local,
            'Reviewed QA is not bound to these artifacts/original-APK evidence')
    require(qa.get('android_device_tested') is False, 'Unsupported device test claim')
    for key in ('original_apk_apply_tested', 'non_trip_resources_unchanged',
                'non_trip_loader_classes_unchanged', 'standalone_and_bundle_trip_resources_identical',
                'js_regression_passed', 'native_hook_host_tests_passed', 'native_hook_original_apk_verified'):
        require(qa.get(key) is True, 'Missing reviewed assertion ' + key)
    require(evidence['js_render']['passed'] is True, 'Fresh JS regression failed')
    require(evidence['js_build']['patched_sha256'] == qa['js_build']['patched_sha256'], 'Reviewed JS bytes differ')
    require(evidence['loader_merge'] == qa['loader_merge'], 'Reviewed loader merge differs from fresh rebuild')
    for key in ('home_jvm_redirect', 'home_dex_redirect', 'home_resource_regression'):
        require(evidence[key] == qa[key], 'Reviewed Home resource verification differs: ' + key)
    # CI repeats source/host/build checks, then ships the exact reviewed report.
    # This retains the independently recorded original-APK evidence without
    # replacing it with a machine-specific build path or implying a CI device test.
    (args.dist / QA).write_bytes(reviewed_raw)
    publication = ROOT.parent / 'publication'
    shutil.copyfile(publication / 'RELEASE_NOTES.txt', args.dist / 'RELEASE_NOTES.txt')
    allowed = {'.py', '.java', '.js', '.json', '.txt', '.md', '.yml', '.yaml', '.xml'}
    sources = {}
    for prefix, directory in (('src', ROOT), ('publication', publication)):
        for path in sorted(directory.rglob('*')):
            if not path.is_file() or path.suffix not in allowed or '__pycache__' in path.parts:
                continue
            sources[prefix + '/' + path.relative_to(directory).as_posix()] = path.read_bytes()
    require({'src/build.py', 'src/local-qa.json', 'src/expected-mpp.json', 'src/reviewed-qa.json',
             'publication/publish162.py', 'publication/manifest.json'}.issubset(sources), 'Incomplete release sources')
    for name in ('build-evidence.json', QA):
        sources['evidence/' + name] = (args.dist / name).read_bytes()
    write_zip(args.dist / SOURCE, sources)
    assets = (BUNDLE, SINGLE, QA, SOURCE, 'RELEASE_NOTES.txt')
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in assets))
    print('PASS release package contains the exact original-APK-tested MPPs and explicit source/evidence files')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base', 'single-base', 'tools', 'work', 'dist'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--package-only', action='store_true')
    options = parser.parse_args()
    for name in ('base', 'single_base', 'tools', 'work', 'dist'):
        setattr(options, name, getattr(options, name).resolve())
    if not options.package_only:
        build(options)
    package_outputs(options)
