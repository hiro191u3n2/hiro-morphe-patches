#!/usr/bin/env python3
"""Build bundle372 with Yahoo's resource helper and its early preparation call.

The Android helper is merged into the current baseline and the existing entry
method receives exactly one leading preparation call. Both JVM counterparts
are derived from the final DEX. Unrelated archive bytes and DEX definitions are
retained, then the exact distributed bundle is tested on old and current APIs.
"""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_VERSION, BUNDLE_VERSION, VERSION = '1.0.371', '1.0.372', '1.2.6'
BASE_SHA = '996206eb4ea224a254dd49cbda83ed6b3566a0f0b322de60f05bb4aa15b1744d'
APK_SHA = '84b7abec9db2910e80fb7dffa9f14301978059d9e4c3e2445efebd9f530bff4b'
MF = 'META-INF/MANIFEST.MF'
HELPER = 'app/hiro/yahootransit/patches/NearMeLayout.class'
ENTRY = 'jp/hiro/patches/yahootransit/YahooTransitAdPatch.class'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.372.mpp'
QA = 'QA_YahooTransit_v1.2.6.json'


def require(value, why):
    if not value:
        raise RuntimeError(why)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt archive')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate entries')
        require(all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts
                    and '\\' not in n for n in z.namelist()), 'Unsafe archive entry')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [row.decode().split(': ', 1) for row in rows]
    require(all(len(p) == 2 for p in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest')
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
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
        for name in sorted(entries, key=lambda n: (n != MF, n != 'classes.dex', n)):
            info = zipfile.ZipInfo(name, (2026, 11, 12, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('base', 'apk', 'tools', 'work', 'dist'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--candidate-only', action='store_true', help='Prepare candidate and core regressions; no QA claim.')
    a = p.parse_args()
    for key in ('base', 'apk', 'tools', 'work', 'dist'):
        setattr(a, key, getattr(a, key).resolve())
    require(sha(a.base) == BASE_SHA and sha(a.apk) == APK_SHA, 'Baseline or app APK differs')
    tools = json.loads((ROOT / 'tool-sources.json').read_text())
    for name, row in tools.items():
        require(sha(a.tools / name) == row['sha256'], 'Pinned tool differs: ' + name)
    def patcher_classes(path):
        with zipfile.ZipFile(path) as jar:
            return {n: hashlib.sha256(jar.read(n)).hexdigest() for n in jar.namelist()
                    if n.startswith('app/morphe/patcher/') and n.endswith('.class')}
    desktop_patcher = patcher_classes(a.tools / 'morphe23.jar')
    manager_patcher = patcher_classes(a.tools / 'manager250-host.jar')
    require(desktop_patcher and desktop_patcher == manager_patcher,
            'Manager2.5 and tested Desktop Patcher classes differ')
    require(not a.work.exists(), 'Use a fresh build directory')
    require(not a.dist.exists() or not any(a.dist.iterdir()), 'Use an empty dist directory')
    a.work.mkdir(parents=True)
    a.dist.mkdir(parents=True, exist_ok=True)
    jdk = a.tools / 'jdk-21.0.13+9'
    java = [jdk / 'bin/java', '-XX:-UsePerfData']
    javac = [jdk / 'bin/javac', '-J-XX:-UsePerfData', '--release', '8', '-encoding', 'UTF-8']
    test_javac = [jdk / 'bin/javac', '-J-XX:-UsePerfData', '-encoding', 'UTF-8']
    def run(cmd, name, timeout=300):
        print('RUN ' + name, flush=True)
        with (a.work / name).open('w') as out:
            result = subprocess.run(list(map(str, cmd)), stdout=out, stderr=subprocess.STDOUT, timeout=timeout)
        output = (a.work / name).read_text(errors='replace').strip()
        require(result.returncode == 0, 'Failed ' + name + '\n' + output[-14000:])
        return output
    def classpath(*paths):
        return os.pathsep.join(map(str, paths))
    def source_hashes():
        return {q.relative_to(ROOT).as_posix(): sha(q) for q in sorted(ROOT.rglob('*'))
                if q.is_file() and q.suffix in ('.java', '.py', '.json') and '__pycache__' not in q.parts}
    sources = source_hashes()
    require('Temurin-21.0.13+9' in run([*java, '--version'], 'java.txt'), 'JDK differs')
    base = archive(a.base)
    fields = headers(base[MF])
    require(fields['Version'] == BASE_VERSION and HELPER in base, 'Baseline identity differs')
    for folder in ('helper-classes', 'helper-dex', 'build-tools', 'legacy-tests', 'modern-tests'):
        (a.work / folder).mkdir()
    (a.work / 'baseline.dex').write_bytes(base['classes.dex'])
    run([*javac, '-cp', a.tools / 'morphe.jar', '-d', a.work / 'helper-classes', ROOT / 'NearMeLayout.java'], 'helper-javac.log')
    compiled = {q.relative_to(a.work / 'helper-classes').as_posix(): q.read_bytes()
                for q in (a.work / 'helper-classes').rglob('*.class')}
    require(set(compiled) == {HELPER}, 'Unexpected helper classes')
    write_zip(a.work / 'helper.jar', compiled)
    run([*java, '-cp', a.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', a.tools / 'android.jar', '--classpath', a.tools / 'morphe.jar',
         '--output', a.work / 'helper-dex', a.work / 'helper.jar'], 'd8.log')
    require({q.name for q in (a.work / 'helper-dex').iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    run([*javac, '-cp', a.tools / 'morphe.jar', '-d', a.work / 'build-tools', ROOT / 'build-tools/LoaderAudit.java'], 'audit-javac.log')
    preservation = run([*java, '-ea', '-cp', classpath(a.work / 'build-tools', a.tools / 'morphe.jar'),
                        'LoaderAudit', a.work / 'baseline.dex', a.work / 'helper-dex/classes.dex',
                        a.work / 'merged.dex'], 'dex-preservation.txt')
    require(preservation.startswith('PASS LoaderAudit:'), 'DEX preservation failed')
    with zipfile.ZipFile(a.tools / 'dex-tools-v2.4.zip') as z:
        for entry in z.infolist():
            path = PurePosixPath(entry.filename)
            require(not path.is_absolute() and '..' not in path.parts and '\\' not in entry.filename
                    and path.parts[0] == 'dex-tools-v2.4' and (entry.external_attr >> 16) & 0o170000 != 0o120000,
                    'Unsafe converter archive')
        z.extractall(a.work / 'converter')
    conversion = run([*java, '-cp', a.work / 'converter/dex-tools-v2.4/lib/*',
                      'com.googlecode.dex2jar.tools.Dex2jarCmd', '-f', '-o', a.work / 'converted.jar',
                      a.work / 'merged.dex'], 'dex-conversion.log')
    require('Exception' not in conversion and 'Error' not in conversion, 'DEX conversion failed')
    converted = archive(a.work / 'converted.jar')
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-11-12T00:00:00',
                  Description='Yahoo Transit v1.2.6: decode layout and string resources explicitly before discovery on Patcher2.3. Existing ad, NearMe and reverse-search hiding retained. Other patch definitions preserved.')
    combined = {**base, HELPER: converted[HELPER], ENTRY: converted[ENTRY],
                'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    expected_changes = {MF, 'classes.dex', HELPER, ENTRY}
    require({n for n in base if combined[n] != base[n]} == expected_changes, 'Unrelated entries changed')
    artifact = a.dist / BUNDLE
    write_zip(artifact, combined)
    require(archive(artifact) == combined and set(combined) == set(base), 'Output differs')
    legacy_names = ('NearMeLayoutTest', 'AdSlotsTest', 'FullPatchExecutionTest', 'ReverseSearchButtonTest')
    run([*test_javac, '-cp', classpath(artifact, a.tools / 'morphe.jar'), '-d', a.work / 'legacy-tests',
         *[ROOT / 'tests' / (n + '.java') for n in legacy_names]], 'legacy-javac.log')
    core = {}
    for name in legacy_names:
        result = run([*java, '-ea', '-cp', classpath(a.work / 'legacy-tests', artifact, a.tools / 'morphe.jar'),
                      'app.hiro.yahootransit.patches.' + name, a.work / ('fixtures-' + name)], name + '.txt')
        require(result.splitlines()[-1].startswith('PASS ' + name + ':'), 'Regression did not complete')
        core[name] = result.splitlines()[-1]
    listings = {}
    for sdk in ('morphe.jar', 'morphe23.jar'):
        old = run([*java, '-jar', a.tools / sdk, 'list-patches', '-p', '-v', '--patches', a.base], sdk + '.old-list.txt')
        new = run([*java, '-jar', a.tools / sdk, 'list-patches', '-p', '-v', '--patches', artifact], sdk + '.new-list.txt')
        def blocks(value):
            parts = re.split(r'(?m)^(?:INFO: )?Index: [0-9]+\s*\n', value)[1:]
            require(parts and all(x.startswith('Name: ') for x in parts), 'Invalid list')
            return Counter(x.strip() for x in parts)
        require(blocks(old) == blocks(new), 'Patch metadata inventory changed')
        listings[sdk] = {'patches_preserved': sum(blocks(old).values()), 'all_metadata_equal': True}
    if a.candidate_only:
        print('PASS candidate with core regressions: ' + str(artifact))
        return
    modern_source = ROOT / 'tests/OnRequestResourceDecodeTest.java'
    require(modern_source.is_file(), 'Current API regression test missing')
    full_source = ROOT / 'tests/FullApk23Test.java'
    require(full_source.is_file(), 'Current full scheduler test missing')
    run([*test_javac, '-cp', classpath(artifact, a.tools / 'morphe23.jar'), '-d', a.work / 'modern-tests', modern_source, full_source], 'modern-javac.log')
    modern = {}
    for label, mpp, baseline in (('baseline', a.base, True), ('fixed', artifact, False)):
        cmd = [*java, '-Xmx2g', '-ea', '-cp', classpath(a.work / 'modern-tests', a.tools / 'morphe23.jar'),
               'app.hiro.yahootransit.patches.OnRequestResourceDecodeTest', mpp, a.apk, a.work / ('actual-' + label)]
        if baseline:
            cmd += ['baseline']
        result = run(cmd, 'actual-' + label + '.txt', 600)
        require(result.splitlines()[-1].startswith('PASS '), 'Actual resource regression incomplete')
        modern[label] = result.splitlines()[-1]
    full = run([*java, '-Xmx2g', '-ea', '-cp', classpath(a.work / 'modern-tests', a.tools / 'morphe23.jar'),
                'app.hiro.yahootransit.patches.FullApk23Test', artifact, a.apk, a.work / 'actual-full'],
               'actual-full.txt', 600)
    require(full.splitlines()[-1].startswith('PASS FullApk23Test:'), 'Full scheduler regression incomplete')
    require(sources == source_hashes(), 'Source changed during build')
    qa = {'schema': 'yahootransit126-qa-v1', 'result': 'PASS', 'patch_version': VERSION,
          'bundle_version': BUNDLE_VERSION, 'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA,
          'bundle_sha256': sha(artifact), 'bundle_bytes': artifact.stat().st_size,
          'app_version': '8.5.9', 'app_version_code': 482, 'app_sha256': APK_SHA,
          'android_dex_sha256': sha(a.work / 'merged.dex'), 'jvm_helper_from_final_dex': True,
          'jvm_entrypoint_from_final_dex': True,
          'changed_archive_entries': sorted(expected_changes), 'preserved_archive_entries': len(base) - len(expected_changes),
          'preserved_jvm_classes': sum(n.endswith('.class') and n not in (HELPER, ENTRY) for n in base),
          'dex_preservation': preservation, 'legacy_regressions': core, 'patch_listing': listings,
          'actual_patcher23_resource_regressions': modern,
          'actual_patcher23_full_apk_regression': full.splitlines()[-1], 'sources': sources,
          'manager250_patcher_code_parity': {
              'status': 'PASS', 'identical_patcher_classes': len(desktop_patcher),
              'manager_apk_sha256': tools['manager250.apk']['sha256'],
              'manager_host_sha256': sha(a.tools / 'manager250-host.jar'),
              'class_hashes': desktop_patcher,
              'scope': 'Patcher classes are byte-identical; Android Manager UI was not executed.'},
          'toolchain_sha256': {n: row['sha256'] for n, row in tools.items()},
          'physical_android_tested': False,
          'limitations': ['Actual binary resource transformation tested on the host; Android UI not exercised.',
                         'Original third-party APK and reconstructed test APKs are not public release artifacts.']}
    (a.dist / QA).write_text(json.dumps(qa, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    print('PASS BUILD ' + BUNDLE + ' ' + sha(artifact))


if __name__ == '__main__':
    main()
