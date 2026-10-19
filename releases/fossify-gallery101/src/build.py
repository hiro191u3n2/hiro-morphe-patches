#!/usr/bin/env python3
"""Build an additive Fossify Gallery patch and audit a public official APK.

No Android device execution is implied by SDK, host or APK structural checks.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.1', '1.0.257', '1.0.258'
BASE_SHA = '69143101c1700a144e5a447948f4e7f6a68dab0653fa77b7fae9688da4bcadd7'
OFFICIAL_APK_SHA = 'cfa321d7467b116741689f8f118b46011c882a4c2359b9320111f87c998af373'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
    'morphe2.jar': '67479a00dba2de387e5115b73960267325e544cedf44a709416cdfb70c82e7f2',
}
PREFIX = 'app/hiro/fossifygallery/patches/'
EXT = 'extensions/fossify_gallery_exit.mpe'
REFERENCE = 'fossify-gallery/original-back.dex'
MF = 'META-INF/MANIFEST.MF'
PATCH_NAME = '戻る終了時に履歴を削除'
SINGLE = 'Fossify_Gallery_NoRecents_v1.0.1.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.258.mpp'
QA_NAME = 'QA_Fossify_Gallery_v1.0.1.json'


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt archive')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate archive entries')
        return {name: z.read(name) for name in z.namelist() if not name.endswith('/')}


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Manifest continuation without header')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest')
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(isinstance(value, str) and not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    raw = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(raw) == fields, 'Manifest roundtrip differs')
    return raw


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda value: (value != MF, value)):
            info = zipfile.ZipInfo(name, (2026, 10, 19, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def class_files(folder):
    return {path.relative_to(folder).as_posix(): path.read_bytes() for path in sorted(folder.rglob('*.class'))}


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def main():
    p = argparse.ArgumentParser()
    for name in ('base', 'tools', 'apk', 'work', 'dist'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--java-home', type=Path)
    a = p.parse_args()
    for key in ('base', 'tools', 'apk', 'work', 'dist'):
        setattr(a, key, getattr(a, key).resolve())
    java = a.java_home / 'bin/java' if a.java_home else 'java'
    javac = a.java_home / 'bin/javac' if a.java_home else 'javac'
    require(sha(a.base) == BASE_SHA, 'Active bundle257 baseline hash differs')
    require(sha(a.apk) == OFFICIAL_APK_SHA, 'Official Gallery 1.14.0 APK hash differs')
    for name, digest in TOOLS.items():
        require(sha(a.tools / name) == digest, 'Pinned tool mismatch: ' + name)
    require(not a.work.exists(), 'Use a fresh build directory')
    require(not a.dist.exists() or not any(a.dist.iterdir()), 'Use an empty output directory')
    a.work.mkdir(parents=True)
    a.dist.mkdir(parents=True, exist_ok=True)

    def run(command, name, timeout=600):
        print('RUN ' + name, flush=True)
        with (a.work / name).open('w') as out:
            result = subprocess.run(list(map(str, command)), stdout=out, stderr=subprocess.STDOUT, timeout=timeout)
        output = (a.work / name).read_text()
        require(result.returncode == 0, 'Failed ' + name + '\n' + output[-12000:])
        return output.strip()

    java_version = run([java, '--version'], 'java.txt')
    require('21.0.8' in java_version, 'JDK21.0.8 is required for repeatable DEX output')
    require(run([javac, '-version'], 'javac.txt') == 'javac 21.0.8', 'Pinned javac required')
    sources = {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
               if x.is_file() and x.suffix in ('.java', '.py')}
    for d in ('runtime', 'patch', 'test', 'host', 'assets/fossify-gallery', 'runtime-dex', 'patch-dex', 'applications'):
        (a.work / d).mkdir(parents=True)
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'android.jar', '-d', a.work / 'runtime',
         *sorted((ROOT / 'runtime').glob('*.java'))], 'runtime-javac.log')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'morphe.jar', '-d', a.work / 'patch',
         *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    auditcp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.work / 'assets', a.tools / 'morphe.jar')))
    run([javac, '-encoding', 'UTF-8', '-cp', auditcp, '-d', a.work / 'test',
         *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.log')
    host_sources = sorted((ROOT / 'test-host').rglob('*.java'))
    require(bool(host_sources), 'Meaningful runtime host tests required')
    hostcp = os.pathsep.join(map(str, (a.work / 'host', a.work / 'runtime')))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', hostcp, '-d', a.work / 'host', *host_sources],
        'host-javac.log')
    host = run([java, '-ea', '-cp', hostcp, 'app.hiro.fossifygallery.runtime.GalleryExitTest'], 'host-tests.txt')
    require(host.startswith('PASS '), 'Host tests incomplete')
    reference = a.work / 'assets' / REFERENCE
    run([java, '-ea', '-cp', auditcp, 'FossifyAudit', 'export-reference', a.apk, reference], 'reference.txt')
    compatibility = {}
    for label, tool in (('stable', 'morphe.jar'), ('patcher2', 'morphe2.jar')):
        cp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.work / 'assets', a.tools / tool)))
        summary = run([java, '-ea', '-cp', cp, 'FossifyCompatibilityTest', a.apk, reference], label + '-compatibility.txt')
        require(summary.startswith('PASS '), 'Compatibility and mutation-rejection tests failed')
        compatibility[label] = summary
    for kind, minapi in (('runtime', '23'), ('patch', '26')):
        write_zip(a.work / (kind + '.jar'), class_files(a.work / kind))
        run([java, '-cp', a.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', minapi,
             '--lib', a.tools / 'android.jar', '--classpath', a.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', a.work / (kind + '-dex'), a.work / (kind + '.jar')], kind + '-d8.log')
        require({x.name for x in (a.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    base = archive(a.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline version differs')
    require(any(k.startswith(PREFIX) for k in base) and EXT in base, 'Existing Gallery patch absent from baseline')
    require(REFERENCE not in base, 'New original-method reference already exists')
    (a.work / 'base.dex').write_bytes(base['classes.dex'])
    preservation = run([java, '-ea', '-cp', auditcp, 'FossifyAudit', 'merge', a.work / 'base.dex',
                        a.work / 'patch-dex/classes.dex', a.work / 'merged.dex'], 'loader-preservation.txt')
    require('PASS' in preservation, 'Loader preservation audit incomplete')
    additions = {**class_files(a.work / 'patch'), EXT: (a.work / 'runtime-dex/classes.dex').read_bytes(),
                 REFERENCE: reference.read_bytes()}
    require(additions[EXT] == base[EXT], 'Gallery exit behavior/runtime changed during compatibility repair')
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-10-19T00:00:00',
                  Description='Fossify Gallery: compare original Back code using same DEX writer to support Patcher 2.0. Keep v1.0.0 exit runtime and other patches. Physical device not tested.')
    combined = {**{k: v for k, v in base.items() if not k.startswith(PREFIX)}, **additions,
                'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    sf = dict(fields)
    sf.update(Name='Fossify Gallery No Recents', Version=VERSION)
    single = {**additions, 'classes.dex': (a.work / 'patch-dex/classes.dex').read_bytes(), MF: manifest(sf)}
    preserved = {k: v for k, v in base.items() if k not in (MF, 'classes.dex') and not k.startswith(PREFIX)}
    require(all(combined[k] == v for k, v in preserved.items()), 'Existing resource changed')
    applications, applications2 = [], []
    entry_maps = {}
    def resource_entries(path):
        with zipfile.ZipFile(path) as z:
            return {name: hashlib.sha256(z.read(name)).hexdigest() for name in z.namelist()
                    if not name.endswith('/') and not re.fullmatch(r'classes(?:[0-9]+)?\.dex', name)
                    and not (name.startswith('META-INF/') and (name == 'META-INF/MANIFEST.MF'
                             or name.upper().endswith(('.SF', '.RSA', '.DSA', '.EC'))))}
    original_resources = resource_entries(a.apk)
    for name, data in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(a.dist / name, data)
        require(archive(a.dist / name) == data, 'Archive roundtrip differs')
        listing = run([java, '-jar', a.tools / 'morphe.jar', 'list-patches', '--patches', a.dist / name,
                       '-f', 'org.fossify.gallery', '-p', '-v'], name + '.listing.txt')
        require(listing.count('Name: ' + PATCH_NAME + '\n') == 1, 'Gallery patch not discovered exactly once')
        for label, tool, rows in (('stable', 'morphe.jar', applications), ('patcher2', 'morphe2.jar', applications2)):
            out = a.work / 'applications' / label / name
            out.mkdir(parents=True)
            apk, report = out / 'patched.apk', out / 'morphe.json'
            run([java, '-Xmx1536m', '-jar', a.tools / tool, 'patch', a.apk, '-p', a.dist / name,
                 '--exclusive', '-e', PATCH_NAME, '--unsigned', '--bytecode-mode', 'FULL',
                 '-o', apk, '-t', out / 'tmp', '-r', report], label + '.' + name + '.apply.log', timeout=900)
            r = json.loads(report.read_text())
            applied = [row if isinstance(row, str) else row.get('name') for row in r.get('appliedPatches', [])]
            steps = r.get('patchingSteps', r.get('stepResults', []))
            require(apk.exists() and not r.get('failedPatches') and applied == [PATCH_NAME]
                    and steps and all(s.get('success') is True for s in steps),
                    'Official APK patch pipeline failed: ' + label)
            audit = run([java, '-ea', '-cp', auditcp, 'FossifyAudit', 'audit-app', a.apk, apk,
                         a.work / 'runtime-dex/classes.dex'], label + '.' + name + '.audit.txt')
            require('PASS' in audit, 'Official APK semantic audit incomplete')
            require(resource_entries(apk) == original_resources,
                    'Official APK manifest/resources/assets/native libraries changed')
            with zipfile.ZipFile(apk) as z:
                entry_maps[(label, name)] = {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')}
            rows.append({'bundle': name, 'package': 'org.fossify.gallery', 'version': '1.14.0',
                         'result': 'PASS', 'audit_summary': audit.splitlines()[-1],
                         'preserved_original_resources': len(original_resources),
                         **({'patcher_version': '2.0.0-dev.12', 'desktop_version': '1.19.0-dev.4'} if label == 'patcher2' else {})})
    for label in ('stable', 'patcher2'):
        require(entry_maps[(label, SINGLE)] == entry_maps[(label, BUNDLE)],
                'Standalone and combined APK entry contents differ: ' + label)
    require(sources == {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
                        if x.is_file() and x.suffix in ('.java', '.py')}, 'Sources changed during build')
    qa = {'schema': 'fossify-gallery101-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
          'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'production_android_sdk_compile': True,
          'physical_android_tested': False, 'toolchain_sha256': TOOLS, 'sources': sources,
          'artifacts': {n: {'bytes': (a.dist / n).stat().st_size, 'sha256': sha(a.dist / n)} for n in (SINGLE, BUNDLE)},
          'loader_preservation': preservation, 'preserved_unrelated_entries': len(preserved),
          'runtime_sha256': sha(a.work / 'runtime-dex/classes.dex'), 'host_tests': host,
          'apk_application': applications, 'official_apk_sha256': sha(a.apk),
          'compatibility_patcher2_apply': applications2, 'compatibility_tests': compatibility,
          'original_back_reference_sha256': sha(reference), 'gallery_runtime_unchanged': True,
          'patcher2_writer_equivalent_to_user_dev13': True,
          'official_apk_non_dex_resources_unchanged': True,
          'single_bundle_apk_entry_contents_equal': True}
    (a.dist / QA_NAME).write_bytes(json_bytes(qa))
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()
