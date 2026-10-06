#!/usr/bin/env python3
"""Replace only the QuickSearch patch in bundle160; original APKs never enter output."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_SHA = '7e3a1f531cf127f9c8c16cb8c12548ce9437b84adac82ea0d9bb2244e7333b5a'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
BUNDLE = 'Hiro_Morphe_Patches_v1.0.161.mpp'
SINGLE = 'QuickSearch_NoRecents_v1.0.1.mpp'
QA = 'QA_QuickSearch_v1.0.1.json'
SOURCE = 'QuickSearch_v1.0.1_sources_and_QA.zip'
MF = 'META-INF/MANIFEST.MF'
PATCH_PREFIX = 'app/hiro/quicksearch/patches/'
RUNTIME_PREFIX = 'app/hiro/quicksearch/runtime/'
EXTENSION = 'extensions/quicksearch_recents.mpe'
DATE = (2026, 10, 7, 0, 0, 0)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def run(command, log):
    with Path(log).open('wb') as stream:
        result = subprocess.run(list(map(str, command)), stdout=stream, stderr=subprocess.STDOUT, timeout=300)
    output = Path(log).read_text(errors='replace')
    if result.returncode:
        print(output[-10000:])
        raise RuntimeError('Command failed; log=' + str(log))
    return output


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt archive')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate archive paths')
        for name in z.namelist():
            require(not Path(name).is_absolute() and '..' not in Path(name).parts, 'Unsafe archive path')
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
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    return dict(row.decode().split(': ', 1) for row in rows)


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
    require(headers(raw) == fields, 'Manifest roundtrip failed')
    return raw


def class_files(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}


def is_quicksearch(name):
    return (name.startswith(PATCH_PREFIX) and name.endswith('.class')) or name == EXTENSION


def build(args):
    require(not args.work.exists(), 'Use a fresh build directory')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)
    require(sha(args.base.read_bytes()) == BASE_SHA, 'Baseline160 checksum mismatch; reconcile a newer bundle before publishing')
    for name, digest in TOOLS.items():
        require(sha((args.tools / name).read_bytes()) == digest, 'Tool checksum mismatch: ' + name)
    baseline = archive(args.base)
    require(headers(baseline[MF])['Version'] == '1.0.160', 'Wrong baseline version')
    for folder in ('runtimeclasses', 'patchclasses', 'testclasses', 'auditclasses', 'runtime-dex', 'patch-dex'):
        (args.work / folder).mkdir()
    common = ['javac', '--release', '8', '-encoding', 'UTF-8']
    run([*common, '-cp', args.tools / 'android.jar', '-d', args.work / 'runtimeclasses', *sorted((ROOT / 'runtime').rglob('*.java'))], args.work / 'runtime-javac.log')
    run([*common, '-cp', args.tools / 'morphe.jar', '-d', args.work / 'patchclasses', *sorted((ROOT / 'patch').rglob('*.java'))], args.work / 'patch-javac.log')
    host_cp = os.pathsep.join(map(str, (args.work / 'runtimeclasses', args.tools / 'android.jar')))
    run([*common, '-cp', host_cp, '-d', args.work / 'testclasses', *sorted((ROOT / 'test-stubs').rglob('*.java')), ROOT / 'test/SearchTaskTest.java'], args.work / 'host-javac.log')
    host_cp = os.pathsep.join(map(str, (args.work / 'testclasses', args.work / 'runtimeclasses', args.tools / 'android.jar')))
    host_result = run(['java', '-cp', host_cp, 'SearchTaskTest'], args.work / 'host-tests.txt')
    audit_cp = os.pathsep.join(map(str, (args.work / 'patchclasses', args.tools / 'morphe.jar')))
    run(['javac', '-encoding', 'UTF-8', '-cp', audit_cp, '-d', args.work / 'auditclasses', ROOT / 'test/DexAudit.java'], args.work / 'audit-javac.log')
    runtime = class_files(args.work / 'runtimeclasses')
    patch = class_files(args.work / 'patchclasses')
    require(RUNTIME_PREFIX + 'SearchTask.class' in runtime and all(n.startswith(RUNTIME_PREFIX) for n in runtime), 'Unexpected runtime class or packaged test stub')
    require(patch and all(n.startswith(PATCH_PREFIX) for n in patch), 'Unexpected patch class')
    write_zip(args.work / 'runtime.jar', runtime)
    write_zip(args.work / 'patch.jar', patch)
    for kind, min_api, classpath in (('runtime', '29', args.tools / 'android.jar'), ('patch', '26', args.tools / 'morphe.jar')):
        run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', min_api,
             '--lib', args.tools / 'android.jar', '--classpath', classpath,
             '--output', args.work / (kind + '-dex'), args.work / (kind + '.jar')], args.work / (kind + '-d8.log'))
        require({p.name for p in (args.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    (args.work / 'base.dex').write_bytes(baseline['classes.dex'])
    audit_cp = os.pathsep.join(map(str, (args.work / 'auditclasses', args.work / 'patchclasses', args.tools / 'morphe.jar')))
    merge_result = run(['java', '-cp', audit_cp, 'DexAudit', 'merge', args.work / 'base.dex', args.work / 'patch-dex/classes.dex', args.work / 'merged.dex'], args.work / 'loader-merge.txt')
    addition = dict(patch)
    addition[EXTENSION] = (args.work / 'runtime-dex/classes.dex').read_bytes()
    previous_own = {n: raw for n, raw in baseline.items() if is_quicksearch(n)}
    require(EXTENSION in previous_own and any(n.startswith(PATCH_PREFIX) for n in previous_own), 'Baseline160 QuickSearch payload missing')
    preserved_baseline = {n: raw for n, raw in baseline.items() if not is_quicksearch(n)}
    require(not set(addition).intersection(preserved_baseline), 'QuickSearch replacement collides with another patch')
    combined = {**preserved_baseline, **addition, 'classes.dex': (args.work / 'merged.dex').read_bytes()}
    fields = headers(baseline[MF])
    fields.update(Version='1.0.161', Timestamp='2026-10-07T00:00:00',
                  Description='QuickSearch0.4.5 close button and Back finish its task and remove recents. Modern Android Back supported. External results use their own task. Other bundle160 patches retained. Device untested.')
    combined[MF] = manifest(fields)
    fields.update(Name='QuickSearch No Recents', Version='1.0.1',
                  Description='QuickSearch0.4.5(36) close button, Back and configured search-exit recents cleanup. Same-task screens and clipboard timer close together. External results kept. Device untested.')
    standalone = {**addition, 'classes.dex': (args.work / 'patch-dex/classes.dex').read_bytes(), MF: manifest(fields)}
    artifacts = {}
    for name, entries in ((BUNDLE, combined), (SINGLE, standalone)):
        path = args.dist / name
        write_zip(path, entries)
        require(archive(path) == entries, 'Written MPP entries differ')
        run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', path, '-f', 'jp.ddo.sugihiro.quicksearch', '-p', '-v'], args.work / (name + '.list.txt'))
        listing = (args.work / (name + '.list.txt')).read_text()
        require('検索後の終了時に履歴へ残さない' in listing and '0.4.5' in listing, 'Patch discovery failed')
        artifacts[name] = {'bytes': path.stat().st_size, 'sha256': sha(path.read_bytes())}
    preserved = [n for n in preserved_baseline if n not in (MF, 'classes.dex')]
    require(all(combined[n] == baseline[n] for n in preserved), 'Existing bundle asset changed')
    require({n: combined[n] for n in addition} == addition, 'Standalone/integrated patch payload mismatch')
    require({n for n in combined if is_quicksearch(n)} == set(addition), 'Obsolete QuickSearch payload remains')
    evidence = {
        'schema': 'quicksearch161-build-v1', 'bundle_version': '1.0.161', 'patch_version': '1.0.1',
        'baseline_bundle_version': '1.0.160', 'baseline_sha256': BASE_SHA, 'toolchain_sha256': TOOLS,
        'supported_package': 'jp.ddo.sugihiro.quicksearch', 'supported_version': '0.4.5', 'supported_version_code': 36,
        'existing_non_quicksearch_zip_entries_preserved': len(preserved),
        'previous_quicksearch_zip_entries': sorted(previous_own),
        'replacement_quicksearch_zip_entries': sorted(addition),
        'runtime_jvm_class_count': len(runtime), 'patch_jvm_class_count': len(patch),
        'non_quicksearch_resources_unchanged': True, 'non_quicksearch_loader_classes_unchanged': True,
        'standalone_and_bundle_quicksearch_resources_identical': True,
        'host_test_result': host_result.strip(), 'loader_merge_result': merge_result.strip(),
        'runtime_stubs_packaged': False, 'artifacts': artifacts,
    }
    (args.dist / 'build-evidence.json').write_bytes(json_bytes(evidence))
    for name in ('host-tests.txt', 'loader-merge.txt'):
        shutil.copyfile(args.work / name, args.dist / name)
    print(json.dumps({'status': 'BUILT_AND_HOST_VERIFIED', 'artifacts': artifacts}, indent=2))


def package_outputs(args):
    local_path = ROOT / 'local-qa.json'
    if not local_path.exists():
        print('MPP build complete. Original-APK QA is required before release packaging.')
        return
    local = json.loads(local_path.read_text())
    evidence = json.loads((args.dist / 'build-evidence.json').read_text())
    require(local['original_apk_apply_tested'] is True and local['device_tested'] is False, 'Invalid local QA claim')
    require(local['artifacts'] == evidence['artifacts'], 'Original-APK QA does not match built MPPs')
    expected = json.loads((ROOT / 'expected-mpp.json').read_text())
    require(expected == evidence['artifacts'], 'Independent expected MPP checksums differ')
    for name, record in expected.items():
        raw = (args.dist / name).read_bytes()
        require(len(raw) == record['bytes'] and sha(raw) == record['sha256'], 'Built file checksum mismatch: ' + name)
    qa = {**evidence, 'schema': 'quicksearch-exit-recents-v2',
          'status': 'BUILT_HOST_AND_ORIGINAL_APK_VERIFIED_DEVICE_UNVERIFIED',
          'device_tested': False, 'original_apk_apply_tested': True,
          'original_apk_evidence': local,
          'search_exit_setting_preserved': True, 'manual_close_independent_of_search_exit_setting': True,
          'external_search_new_task_for_all_exit_preferences': True,
          'foreign_caller_task_preserved': True, 'other_task_ids_preserved': True,
          'same_task_application_screens_finished_together': True,
          'close_button_minimum_height_dp': 48,
          'non_weighted_close_button_minimum_width_dp': 48,
          'classic_button_layout_width_and_weight_preserved': True,
          'modern_activity_and_dialog_back_callbacks': True,
          'legacy_back_key_down_consumed_and_up_finishes': True,
          'legacy_input_back_before_ime_hook': True,
          'clipboard_timer_stopped_on_exit': True, 'manifest_changed': False,
          'published': False,
          'limitations': ['No connected Android/Galaxy device; recents UI behavior is not device-tested.',
                          'Desktop patch application and host/runtime control checks do not prove OEM task behavior.']}
    (args.dist / QA).write_bytes(json_bytes(qa))
    shutil.copyfile(ROOT / 'RELEASE_NOTES.txt', args.dist / 'RELEASE_NOTES.txt')
    allowed = {'.py', '.java', '.json', '.txt'}
    files = {p.relative_to(ROOT).as_posix(): p.read_bytes() for p in sorted(ROOT.rglob('*'))
             if p.is_file() and p.suffix in allowed and '__pycache__' not in p.parts}
    require({'build.py', 'publish161.py', 'expected-mpp.json', 'local-qa.json'}.issubset(files), 'Incomplete release sources')
    for name in ('build-evidence.json', 'host-tests.txt', 'loader-merge.txt', QA):
        files['evidence/' + name] = (args.dist / name).read_bytes()
    write_zip(args.dist / SOURCE, files)
    assets = (BUNDLE, SINGLE, QA, SOURCE, 'RELEASE_NOTES.txt')
    sums = ''.join(sha((args.dist / name).read_bytes()) + '  ' + name + '\n' for name in assets)
    (args.dist / 'SHA256SUMS.txt').write_text(sums)
    print('PASS release package uses the exact original-APK-tested MPPs; no app APK included')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base', 'tools', 'work', 'dist'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--package-only', action='store_true')
    args = parser.parse_args()
    for name in ('base', 'tools', 'work', 'dist'):
        setattr(args, name, getattr(args, name).resolve())
    if not args.package_only:
        build(args)
    package_outputs(args)
