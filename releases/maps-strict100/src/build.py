#!/usr/bin/env python3
"""Build the request-scoped Maps name filter and verify actual APK transformations."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.0', '1.0.366', '1.0.367'
BASE_SHA = 'eecd4d0360ba192a4c2552ede56f7421442acc908f813ce670204cd60cc62510'
APK_SHA = '7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063'
REPORTED_SHA = '238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-2.2.jar': '855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}
MF = 'META-INF/MANIFEST.MF'
EXT = 'extensions/maps_strict_search.mpe'
PREFIX = 'app/hiro/mapsstrict/patches/'
NAME = '店名検索を厳密にする'
EXISTING_NAMES = ['起動時に現在地を表示', '検索欄の下のカテゴリ行を非表示', '戻るで終了して履歴から削除']
SINGLE = 'Maps_Strict_Search_v1.0.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.367.mpp'
QA = 'QA_Maps_Strict_Search_v1.0.0.json'

def require(ok, why):
    if not ok:
        raise RuntimeError(why)

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None and len(z.namelist()) == len(set(z.namelist())), 'Invalid ZIP')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}

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
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest header')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    raw = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(raw) == fields, 'Manifest round trip differs')
    return raw

def write_zip(path, data):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(data, key=lambda n: (n != MF, n != 'classes.dex', n)):
            info = zipfile.ZipInfo(name, (2026, 11, 8, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            z.writestr(info, data[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)

def classes(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}

def sources():
    return {str(p.relative_to(ROOT)): sha(p) for p in sorted(ROOT.rglob('*'))
            if p.is_file() and p.suffix in ('.java', '.py') and '__pycache__' not in p.parts}

def main():
    p = argparse.ArgumentParser()
    for name in ('base', 'tools', 'work', 'dist', 'apk', 'reported-apk', 'latest-maps-apk', 'java-home'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--timestamp', required=True)
    a = p.parse_args()
    for key in ('base', 'tools', 'work', 'dist', 'apk', 'reported_apk', 'latest_maps_apk', 'java_home'):
        setattr(a, key, getattr(a, key).resolve())
    require(sha(a.base) == BASE_SHA and sha(a.apk) == APK_SHA and sha(a.reported_apk) == REPORTED_SHA,
            'A pinned input differs')
    for name, digest in TOOLS.items():
        require(sha(a.tools / name) == digest, 'Pinned tool differs: ' + name)
    require(not a.work.exists(), 'Fresh build work directory required')
    require(not a.dist.exists() or not any(a.dist.iterdir()), 'Empty dist required')
    a.work.mkdir(parents=True)
    a.dist.mkdir(parents=True, exist_ok=True)
    java, javac = a.java_home / 'bin/java', a.java_home / 'bin/javac'
    source_hashes = sources()

    def run(cmd, name, timeout=900):
        print('RUN ' + name, flush=True)
        with (a.work / name).open('w') as log:
            r = subprocess.run(list(map(str, cmd)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        text = (a.work / name).read_text()
        require(r.returncode == 0, 'Failed ' + name + '\n' + text[-18000:])
        return text.strip()

    for name in ('patch', 'runtime', 'patch-dex', 'runtime-dex', 'test', 'host', 'applications'):
        (a.work / name).mkdir()
    runtime_sources = sorted((ROOT / 'runtime').rglob('*.java'))
    require(runtime_sources, 'Production runtime missing')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'android.jar',
         '-d', a.work / 'runtime', *runtime_sources], 'runtime-javac.log')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'morphe.jar',
         '-d', a.work / 'patch', *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    compile_cp = os.pathsep.join(map(str, (a.work / 'patch', a.tools / 'morphe.jar')))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', compile_cp,
         '-d', a.work / 'test', *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.log')
    host_sources = [*runtime_sources, *sorted((ROOT / 'test-host').rglob('*.java'))]
    require(len(host_sources) > len(runtime_sources), 'Runtime behavior tests missing')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'android.jar',
         '-d', a.work / 'host', *host_sources], 'host-javac.log')
    matcher = run([java, '-ea', '-cp', a.work / 'host', 'app.hiro.mapsstrict.StrictNameMatcherTest'], 'matcher-tests.txt')
    runtime_tests = run([java, '-ea', '-cp', a.work / 'host', 'RuntimeFilterTest'], 'runtime-tests.txt')
    require('PASS' in matcher and 'PASS' in runtime_tests, 'Incomplete runtime verification')
    host = 'PASS production matcher and request-scoped response filter\n' + matcher + '\n' + runtime_tests
    guard_results = []
    for sdk in ('morphe.jar', 'morphe-2.2.jar'):
        cp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / sdk)))
        for label, apk in (('v285', a.apk), ('v287', a.reported_apk), ('latest104', a.latest_maps_apk)):
            result = run([java, '-Xmx2g', '-ea', '-cp', cp, 'MapsStrictAudit', 'guards', apk], sdk + '.' + label + '.guards.txt')
            guard_results.append({'sdk': sdk, 'input': label, 'sha256': sha(apk), 'result': result})
    for kind, api in (('runtime', '23'), ('patch', '26')):
        write_zip(a.work / (kind + '.jar'), classes(a.work / kind))
        run([java, '-cp', a.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', api,
             '--lib', a.tools / 'android.jar', '--classpath', a.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', a.work / (kind + '-dex'), a.work / (kind + '.jar')], kind + '-d8.log')
    base = archive(a.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Wrong baseline version')
    require(not any(n.startswith(PREFIX) or n == EXT for n in base), 'Search patch already in baseline')
    (a.work / 'base.dex').write_bytes(base['classes.dex'])
    audit_cp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / 'morphe-2.2.jar')))
    merge = run([java, '-Xmx2g', '-ea', '-cp', audit_cp, 'MapsStrictAudit', 'merge',
                 a.work / 'base.dex', a.work / 'patch-dex/classes.dex', a.work / 'merged.dex'], 'loader-preservation.txt')
    added = {**classes(a.work / 'patch'), EXT: (a.work / 'runtime-dex/classes.dex').read_bytes()}
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp=a.timestamp,
                  Description='Request-scoped shop-name filtering; preserve registered aliases, branch names and category searches. Existing Maps startup/back/category patches and all unrelated payloads are preserved. Android UI behavior not yet verified.')
    combined = {**base, **added, 'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    single = {**added, 'classes.dex': (a.work / 'patch-dex/classes.dex').read_bytes(),
              MF: manifest({**fields, 'Name': 'Maps Strict Search', 'Version': VERSION})}
    require(all(combined[n] == value for n, value in base.items() if n not in (MF, 'classes.dex')),
            'Existing bundle payload changed')
    for name, data in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(a.dist / name, data)
    selection = []
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe-2.2.jar'):
            for package in ('org.ungoogled.android.apps.maps.microg', 'com.google.android.apps.maps'):
                listing = run([java, '-jar', a.tools / sdk, 'list-patches', '--patches', a.dist / name,
                               '-f', package, '-p', '-v'], name + '.' + sdk + '.' + package + '.listing.txt')
                require(listing.count('Name: ' + NAME + '\n') == 1, 'Filter not selectable exactly once')
                if name == BUNDLE:
                    require(all(listing.count('Name: ' + n + '\n') == 1 for n in EXISTING_NAMES), 'Existing Maps options missing')
                selection.append({'artifact': name, 'sdk': sdk, 'package': package, 'matches': 1})

    applications = []
    def apply(label, artifact, input_apk, names, reference_apk=None):
        out = a.work / 'applications' / label
        out.mkdir()
        target, report = out / 'patched.apk', out / 'morphe.json'
        cmd = [java, '-Xmx3g', '-jar', a.tools / 'morphe-2.2.jar', 'patch', input_apk,
               '-p', a.dist / artifact, '--exclusive', '--unsigned', '--bytecode-mode', 'STRIP_FAST',
               '-o', target, '-t', out / 'tmp', '-r', report]
        for n in names:
            cmd += ['-e', n]
        run(cmd, label + '.apply.log')
        result = json.loads(report.read_text())
        applied = [x['name'] if isinstance(x, dict) else x for x in result.get('appliedPatches', [])]
        require(target.exists() and result.get('success') is True and not result.get('failedPatches')
                and set(applied) == set(names) and len(applied) == len(names), 'APK patch selection/application failed')
        steps = result.get('stepResults', [])
        require(steps and all(step.get('success') is True for step in steps)
                and {'PATCHING', 'REBUILDING'}.issubset({step.get('step') for step in steps}),
                'APK patch/rebuild steps were not successful')
        audit = run([java, '-Xmx3g', '-ea', '-cp', audit_cp, 'MapsStrictAudit', 'audit',
                     reference_apk or input_apk, target, a.work / 'runtime-dex/classes.dex'], label + '.audit.txt')
        applications.append({'label': label, 'artifact': artifact, 'input_sha256': sha(input_apk),
                             'patches': names, 'steps': steps, 'audit': audit, 'result': 'PASS'})
        return target
    one = apply('standalone', SINGLE, a.apk, [NAME])
    two = apply('integrated', BUNDLE, a.apk, [NAME])
    require(archive(one) == archive(two), 'Standalone and bundle APK contents differ')
    apply('reported-v287', BUNDLE, a.reported_apk, [NAME])
    apply('latest104-additive', BUNDLE, a.latest_maps_apk, [NAME])
    apply('all-four-maps', BUNDLE, a.apk, EXISTING_NAMES + [NAME], a.latest_maps_apk)
    require(sources() == source_hashes, 'Source changed during build')
    qa = {'schema': 'maps-strict100-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
          'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'runtime_extension': EXT,
          'release_timestamp': a.timestamp, 'release_timezone': 'Asia/Tokyo',
          'production_android_sdk_compile': True, 'runtime_android_tested': False,
          'physical_android_tested': False, 'android_emulator_tested': False,
          'runtime_limitation': 'Actual APK transformations, Java runtime tests and native contracts verified. No Android UI execution was available.',
          'source_sha256': source_hashes, 'toolchain_sha256': TOOLS, 'host_tests': host,
          'native_scope': {'owners': ['Lavds;', 'Lavdv;'], 'added_instructions': 6,
                           'marker': 'private transient synthetic Object hiroStrictSearchResponse',
                           'success_response_identity_checked_before_merge_clear': True},
          'guard_results': guard_results, 'selection_results': selection, 'apk_application': applications,
          'loader_preservation': merge, 'preserved_unrelated_entries': len(base) - 2,
          'single_bundle_apk_contents_equal': True,
          'artifacts': {n: {'bytes': (a.dist / n).stat().st_size, 'sha256': sha(a.dist / n)} for n in (SINGLE, BUNDLE)}}
    (a.dist / QA).write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)

if __name__ == '__main__':
    main()
