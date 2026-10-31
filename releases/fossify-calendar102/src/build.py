#!/usr/bin/env python3
"""Build an updated Fossify Calendar patch and optionally audit the supplied APK.

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
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.2', '1.0.323', '1.0.324'
BASE_SHA = 'df9c718225878840463a211911f9a62698df38af6291f84800b257fb79689810'
OFFICIAL_APK_SHA = '4d0aae1abca915d5509edc36f2c1724ae7856484b5d56b76a01cc360bd82b885'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
}
PREFIX = 'app/hiro/fossifycalendar/patches/'
EXT = 'extensions/fossify_calendar_colors.mpe'
MF = 'META-INF/MANIFEST.MF'
PATCH_NAME = '月表示とウィジェットの土曜を青・祝日を赤'
SINGLE = 'Fossify_Calendar_MonthColors_v1.0.2.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.324.mpp'
QA_NAME = 'QA_Fossify_Calendar_v1.0.2.json'


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
    require(sha(a.base) == BASE_SHA, 'Active bundle323 baseline hash differs')
    if a.apk is not None:
        require(sha(a.apk) == OFFICIAL_APK_SHA, 'Supplied Calendar 1.12.1 APK hash differs')
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
    for d in ('runtime', 'patch', 'test', 'host', 'runtime-dex', 'patch-dex', 'applications'):
        (a.work / d).mkdir()
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'android.jar', '-d', a.work / 'runtime',
         *sorted((ROOT / 'runtime').glob('*.java'))], 'runtime-javac.log')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'morphe.jar', '-d', a.work / 'patch',
         *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    auditcp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / 'morphe.jar')))
    run([javac, '-encoding', 'UTF-8', '-cp', auditcp, '-d', a.work / 'test',
         *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.log')
    guard_results = []
    for loader in ('morphe.jar', 'morphe-current.jar'):
        guardcp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / loader)))
        guard_results.append(run([java, '-ea', '-cp', guardcp, 'CalendarAudit', 'guard-tests', *([a.apk] if a.apk else [])], loader + '.guard-tests.txt'))
    guards = '\n'.join(guard_results)
    host_sources = sorted((ROOT / 'test-host').rglob('*.java'))
    require(bool(host_sources), 'Meaningful runtime host tests required')
    hostcp = os.pathsep.join(map(str, (a.work / 'host', a.work / 'runtime')))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', hostcp, '-d', a.work / 'host', *host_sources],
        'host-javac.log')
    host = run([java, '-ea', '-cp', hostcp, 'app.hiro.fossifycalendar.runtime.MonthColorsTest'], 'host-tests.txt')
    require(host.startswith('PASS '), 'Host tests incomplete')
    for kind, minapi in (('runtime', '23'), ('patch', '26')):
        write_zip(a.work / (kind + '.jar'), class_files(a.work / kind))
        run([java, '-cp', a.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', minapi,
             '--lib', a.tools / 'android.jar', '--classpath', a.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', a.work / (kind + '-dex'), a.work / (kind + '.jar')], kind + '-d8.log')
        require({x.name for x in (a.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    base = archive(a.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline version differs')
    require(any(k.startswith(PREFIX) for k in base) and EXT in base, 'Calendar v1.0.1 missing from reviewed baseline')
    (a.work / 'base.dex').write_bytes(base['classes.dex'])
    preservation = run([java, '-ea', '-cp', auditcp, 'CalendarAudit', 'merge', a.work / 'base.dex',
                        a.work / 'patch-dex/classes.dex', a.work / 'merged.dex'], 'loader-preservation.txt')
    require('PASS' in preservation, 'Loader preservation audit incomplete')
    additions = {**class_files(a.work / 'patch'), EXT: (a.work / 'runtime-dex/classes.dex').read_bytes()}
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-11-01T00:00:00',
                  Description='Fossify Calendar monthly app view and widget: Saturday blue, holiday dates red, app today text white, widget today text black. Preserve unrelated patches. Physical device not tested.')
    unrelated = {k: v for k, v in base.items() if not k.startswith(PREFIX) and k != EXT}
    combined = {**unrelated, **additions, 'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    sf = dict(fields)
    sf.update(Name='Fossify Calendar Month Colors', Version=VERSION)
    single = {**additions, 'classes.dex': (a.work / 'patch-dex/classes.dex').read_bytes(), MF: manifest(sf)}
    preserved = {k: v for k, v in base.items() if k not in (MF, 'classes.dex', EXT) and not k.startswith(PREFIX)}
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
                           '-f', 'org.fossify.calendar', '-p', '-v'], name + '.' + loader + '.listing.txt')
            require(listing.count('Name: ' + PATCH_NAME + '\n') == 1, 'Calendar patch not discovered exactly once')
        if a.apk is None:
            continue
        out = a.work / 'applications' / name
        out.mkdir()
        apk, report = out / 'patched.apk', out / 'morphe.json'
        run([java, '-Xmx1536m', '-jar', a.tools / 'morphe-current.jar', 'patch', a.apk, '-p', a.dist / name,
             '--exclusive', '-e', PATCH_NAME, '--unsigned', '--bytecode-mode', 'FULL',
             '-o', apk, '-t', out / 'tmp', '-r', report], name + '.apply.log', timeout=900)
        r = json.loads(report.read_text())
        require(apk.exists() and r.get('success') is True and not r.get('failedPatches')
                and r.get('appliedPatches') == [PATCH_NAME] and r.get('packageName') == 'org.fossify.calendar'
                and r.get('packageVersion') == '1.12.1' and r.get('stepResults')
                and all(s.get('success') is True for s in r['stepResults']),
                'Supplied APK patch pipeline failed')
        audit = run([java, '-ea', '-cp', auditcp, 'CalendarAudit', 'audit-app', a.apk, apk,
                     a.work / 'runtime-dex/classes.dex'], name + '.audit.txt')
        require('PASS' in audit, 'Official APK semantic audit incomplete')
        require(resource_entries(apk) == original_resources,
                'Official APK manifest/resources/assets/native libraries changed')
        with zipfile.ZipFile(apk) as z:
            entry_maps.append({n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')})
        applications.append({'bundle': name, 'package': 'org.fossify.calendar', 'version': '1.12.1',
                             'result': 'PASS', 'audit_summary': audit.splitlines()[-1],
                             'preserved_original_resources': len(original_resources)})
    if a.apk:
        require(entry_maps[0] == entry_maps[1], 'Standalone and combined APK entry contents differ')
    require(sources == {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
                        if x.is_file() and x.suffix in ('.java', '.py')}, 'Sources changed during build')
    qa = {'schema': 'fossify-calendar102-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
          'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'production_android_sdk_compile': True,
          'physical_android_tested': False, 'toolchain_sha256': TOOLS, 'sources': sources,
          'artifacts': {n: {'bytes': (a.dist / n).stat().st_size, 'sha256': sha(a.dist / n)} for n in (SINGLE, BUNDLE)},
          'loader_preservation': preservation, 'preserved_unrelated_entries': len(preserved),
          'runtime_sha256': sha(a.work / 'runtime-dex/classes.dex'), 'host_tests': host, 'guard_tests': guards,
          'apk_application': applications, 'supplied_apk_sha256': sha(a.apk) if a.apk else None,
          'supplied_apk_non_dex_resources_unchanged': bool(a.apk),
          'single_bundle_apk_entry_contents_equal': bool(a.apk)}
    (a.dist / QA_NAME).write_bytes(json_bytes(qa))
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()

