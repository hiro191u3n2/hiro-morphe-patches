#!/usr/bin/env python3
"""Build an additive Maps category-row patch and optionally audit the supplied APK.

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
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.0', '1.0.345', '1.0.346'
BASE_SHA = '35993d492f845d143ae70d61dee9d839f5151649e5615de52c3ba90c087fc954'
SUPPLIED_APK_SHA = '7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
}
PREFIX = 'app/hiro/maps/patches/'
EXT = None
MF = 'META-INF/MANIFEST.MF'
PATCH_NAME = '検索欄の下のカテゴリ行を非表示'
SINGLE = 'Maps_Hide_Category_Row_v1.0.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.346.mpp'
QA_NAME = 'QA_Maps_Category_v1.0.0.json'


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
            info = zipfile.ZipInfo(name, (2026, 11, 1, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def class_files(folder):
    return {path.relative_to(folder).as_posix(): path.read_bytes() for path in sorted(folder.rglob('*.class'))}


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def main():
    p = argparse.ArgumentParser()
    for name in ('base', 'tools', 'work', 'dist'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--java-home', type=Path)
    p.add_argument('--apk', type=Path)
    a = p.parse_args()
    for key in ('base', 'tools', 'apk', 'work', 'dist'):
        if getattr(a, key) is not None:
            setattr(a, key, getattr(a, key).resolve())
    java = a.java_home / 'bin/java' if a.java_home else 'java'
    javac = a.java_home / 'bin/javac' if a.java_home else 'javac'
    require(sha(a.base) == BASE_SHA, 'Active bundle345 baseline hash differs')
    if a.apk is not None:
        require(sha(a.apk) == SUPPLIED_APK_SHA, 'Supplied Maps 26.36.04.973607363 APK hash differs')
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
    require('21.0.13' in java_version, 'JDK21.0.13 is required for repeatable DEX output')
    require(run([javac, '-version'], 'javac.txt') == 'javac 21.0.13', 'Pinned javac required')
    sources = {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
               if x.is_file() and x.suffix in ('.java', '.py')}
    for d in ('patch', 'test', 'patch-dex', 'applications'):
        (a.work / d).mkdir()
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'morphe.jar', '-d', a.work / 'patch',
         *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    auditcp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / 'morphe.jar')))
    run([javac, '-encoding', 'UTF-8', '-cp', auditcp, '-d', a.work / 'test',
         *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.log')
    guard_results = []
    for loader in ('morphe.jar', 'morphe-current.jar'):
        guardcp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / loader)))
        guard_results.append(run([java, '-ea', '-cp', guardcp, 'MapsAudit', 'guard-tests', *([a.apk] if a.apk else [])], loader + '.guard-tests.txt'))
    guards = '\n'.join(guard_results)
    host = 'Not applicable: no runtime extension; one original method receives a null-return instruction.'
    write_zip(a.work / 'patch.jar', class_files(a.work / 'patch'))
    run([java, '-cp', a.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', a.tools / 'android.jar', '--classpath', a.tools / 'morphe.jar',
         '--output', a.work / 'patch-dex', a.work / 'patch.jar'], 'patch-d8.log')
    require({x.name for x in (a.work / 'patch-dex').iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    base = archive(a.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline version differs')
    require(not any(k.startswith(PREFIX) or k == EXT for k in base), 'Maps patch already exists in baseline')
    (a.work / 'base.dex').write_bytes(base['classes.dex'])
    preservation = run([java, '-ea', '-cp', auditcp, 'MapsAudit', 'merge', a.work / 'base.dex',
                        a.work / 'patch-dex/classes.dex', a.work / 'merged.dex'], 'loader-preservation.txt')
    require('PASS' in preservation, 'Loader preservation audit incomplete')
    additions = class_files(a.work / 'patch')
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-11-05T00:00:00',
                  Description='Maps: hide the home category shortcut row below search. Preserve unrelated patches. Physical device not tested.')
    combined = {**base, **additions, 'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    sf = dict(fields)
    sf.update(Name='Maps Hide Category Row', Version=VERSION)
    single = {**additions, 'classes.dex': (a.work / 'patch-dex/classes.dex').read_bytes(), MF: manifest(sf)}
    preserved = {k: v for k, v in base.items() if k not in (MF, 'classes.dex')}
    require(all(combined[k] == v for k, v in preserved.items()), 'Existing resource changed')
    applications = []
    entry_maps = []
    def resource_entries(path):
        with zipfile.ZipFile(path) as z:
            return {name: hashlib.sha256(z.read(name)).hexdigest() for name in z.namelist()
                    if not name.endswith('/') and not re.fullmatch(r'classes(?:[0-9]+)?\.dex', name)
                    and not (name.startswith('META-INF/') and (name == 'META-INF/MANIFEST.MF'
                             or name.upper().endswith(('.SF', '.RSA', '.DSA', '.EC'))))}
    original_resources = resource_entries(a.apk) if a.apk else None
    for name, data in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(a.dist / name, data)
        require(archive(a.dist / name) == data, 'Archive roundtrip differs')
        for loader in ('morphe.jar', 'morphe-current.jar'):
            listing = run([java, '-jar', a.tools / loader, 'list-patches', '--patches', a.dist / name,
                           '-f', 'org.ungoogled.android.apps.maps.microg', '-p', '-v'], name + '.' + loader + '.listing.txt')
            require(listing.count('Name: ' + PATCH_NAME + '\n') == 1, 'Maps patch not discovered exactly once')
        if a.apk is None:
            continue
        out = a.work / 'applications' / name
        out.mkdir()
        apk, report = out / 'patched.apk', out / 'morphe.json'
        run([java, '-Xmx4g', '-jar', a.tools / 'morphe-current.jar', 'patch', a.apk, '-p', a.dist / name,
             '--exclusive', '-e', PATCH_NAME, '--unsigned', '--bytecode-mode', 'FULL',
             '-o', apk, '-t', out / 'tmp', '-r', report], name + '.apply.log', timeout=900)
        r = json.loads(report.read_text())
        require(apk.exists() and r.get('success') is True and not r.get('failedPatches')
                and r.get('appliedPatches') == [PATCH_NAME] and r.get('packageName') == 'org.ungoogled.android.apps.maps.microg'
                and r.get('packageVersion') == '26.36.04.973607363' and r.get('stepResults')
                and all(s.get('success') is True for s in r['stepResults']),
                'Supplied APK patch pipeline failed')
        audit = run([java, '-Xmx4g', '-ea', '-cp', auditcp, 'MapsAudit', 'audit-app', a.apk, apk], name + '.audit.txt')
        require('PASS' in audit, 'Supplied APK semantic audit incomplete')
        require(resource_entries(apk) == original_resources,
                'Supplied APK manifest/resources/assets/native libraries changed')
        with zipfile.ZipFile(apk) as z:
            entry_maps.append({n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')})
        applications.append({'bundle': name, 'package': 'org.ungoogled.android.apps.maps.microg', 'version': '26.36.04.973607363',
                             'result': 'PASS', 'audit_summary': audit.splitlines()[-1],
                             'preserved_original_resources': len(original_resources)})
    if a.apk:
        require(entry_maps[0] == entry_maps[1], 'Standalone and combined APK entry contents differ')
    require(sources == {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
                        if x.is_file() and x.suffix in ('.java', '.py')}, 'Sources changed during build')
    qa = {'schema': 'maps-category100-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
          'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'production_android_sdk_compile': True,
          'physical_android_tested': False, 'toolchain_sha256': TOOLS, 'sources': sources,
          'artifacts': {n: {'bytes': (a.dist / n).stat().st_size, 'sha256': sha(a.dist / n)} for n in (SINGLE, BUNDLE)},
          'loader_preservation': preservation, 'preserved_unrelated_entries': len(preserved),
          'host_tests': host, 'guard_tests': guards,
          'apk_application': applications, 'supplied_apk_sha256': sha(a.apk) if a.apk else None,
          'supplied_apk_non_dex_resources_unchanged': bool(a.apk),
          'single_bundle_apk_entry_contents_equal': bool(a.apk)}
    (a.dist / QA_NAME).write_bytes(json_bytes(qa))
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()

