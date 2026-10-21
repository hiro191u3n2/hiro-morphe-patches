#!/usr/bin/env python3
"""Build an additive Aurora Store patch and audit a public official APK.

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
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.0', '1.0.264', '1.0.265'
BASE_SHA = '329e05444d41066a7af795672b1f672ef2954d3461e6098398f3ddb976f14fb0'
UPSTREAM_APK_SHA = '8a1ed9aa09631290da91cb793e0517b0f20dc70239ac94ae6682cd94f91a4bad'
OFFICIAL_APK_SHA = 'fd9c75d90d0f4a7c132b9b4a5a2cf1992a45e03b8d8ff988b7dcfbc0db2c4d11'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
    'morphe2.jar': '67479a00dba2de387e5115b73960267325e544cedf44a709416cdfb70c82e7f2',
}
PREFIX = 'app/hiro/aurora/patches/'
EXT = 'extensions/aurora_favourites.mpe'
MF = 'META-INF/MANIFEST.MF'
PATCH_NAME = '毎回お気に入りアプリから起動'
SINGLE = 'Aurora_Store_Favourites_v1.0.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.265.mpp'
QA_NAME = 'QA_Aurora_Store_v1.0.0.json'


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
            info = zipfile.ZipInfo(name, (2026, 10, 21, 0, 0, 0))
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
    p.add_argument('--official-apk', type=Path, required=True)
    p.add_argument('--java-home', type=Path)
    a = p.parse_args()
    for key in ('base', 'tools', 'apk', 'work', 'dist'):
        setattr(a, key, getattr(a, key).resolve())
    a.official_apk = a.official_apk.resolve()
    java = a.java_home / 'bin/java' if a.java_home else 'java'
    javac = a.java_home / 'bin/javac' if a.java_home else 'javac'
    require(sha(a.base) == BASE_SHA, 'Active bundle264 baseline hash differs')
    require(sha(a.apk) == OFFICIAL_APK_SHA, 'Official Aurora 4.8.4 APK hash differs')
    require(sha(a.official_apk) == UPSTREAM_APK_SHA, 'Upstream-signed Aurora 4.8.4 APK differs')
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
    for d in ('runtime', 'patch', 'test', 'host', 'runtime-dex', 'patch-dex', 'applications'):
        (a.work / d).mkdir()
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'android.jar', '-d', a.work / 'runtime',
         *sorted((ROOT / 'runtime').glob('*.java'))], 'runtime-javac.log')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'morphe.jar', '-d', a.work / 'patch',
         *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    auditcp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / 'morphe.jar')))
    run([javac, '-encoding', 'UTF-8', '-cp', auditcp, '-d', a.work / 'test',
         *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.log')
    # Reproduce the runtime-invalid inherited-final design before auditing the fixed APK.
    negative = subprocess.run([str(java), '-ea', '-cp', auditcp, 'AuroraAudit', 'hierarchy', str(a.apk)],
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=90)
    (a.work / 'original-final-override-rejection.txt').write_text(negative.stdout)
    require(negative.returncode != 0 and 'illegal override of final superclass method' in negative.stdout,
            'Original final-method conflict was not reproduced')
    host_sources = sorted((ROOT / 'test-host').rglob('*.java'))
    require(bool(host_sources), 'Meaningful runtime host tests required')
    hostcp = os.pathsep.join(map(str, (a.work / 'host', a.work / 'runtime')))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', hostcp, '-d', a.work / 'host', *host_sources],
        'host-javac.log')
    host = run([java, '-ea', '-cp', hostcp, 'app.hiro.aurora.runtime.FavouriteStartTest'], 'host-tests.txt')
    require(host.startswith('PASS '), 'Host tests incomplete')
    for kind, minapi in (('runtime', '23'), ('patch', '26')):
        write_zip(a.work / (kind + '.jar'), class_files(a.work / kind))
        run([java, '-cp', a.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', minapi,
             '--lib', a.tools / 'android.jar', '--classpath', a.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', a.work / (kind + '-dex'), a.work / (kind + '.jar')], kind + '-d8.log')
        require({x.name for x in (a.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    base = archive(a.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline version differs')
    require(not any(k.startswith(PREFIX) or k == EXT for k in base), 'Aurora patch already exists in baseline')
    (a.work / 'base.dex').write_bytes(base['classes.dex'])
    preservation = run([java, '-ea', '-cp', auditcp, 'AuroraAudit', 'merge', a.work / 'base.dex',
                        a.work / 'patch-dex/classes.dex', a.work / 'merged.dex'], 'loader-preservation.txt')
    require('PASS' in preservation, 'Loader preservation audit incomplete')
    additions = {**class_files(a.work / 'patch'), EXT: (a.work / 'runtime-dex/classes.dex').read_bytes()}
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-10-21T00:00:00',
                  Description='Aurora Store: open favourite apps on normal startup and reopening. Preserve authentication and all existing patches. Physical device not tested.')
    combined = {**base, **additions, 'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    sf = dict(fields)
    sf.update(Name='Aurora Store Favourites', Version=VERSION)
    single = {**additions, 'classes.dex': (a.work / 'patch-dex/classes.dex').read_bytes(), MF: manifest(sf)}
    preserved = {k: v for k, v in base.items() if k not in (MF, 'classes.dex')}
    require(all(combined[k] == v for k, v in preserved.items()), 'Existing resource changed')
    applications = []
    entry_maps = {}
    distribution_entry_maps = {}
    audit_cache = {}
    def resource_entries(path):
        with zipfile.ZipFile(path) as z:
            return {name: hashlib.sha256(z.read(name)).hexdigest() for name in z.namelist()
                    if not name.endswith('/') and not re.fullmatch(r'classes(?:[0-9]+)?\.dex', name)
                    and not (name.startswith('META-INF/') and (name == 'META-INF/MANIFEST.MF'
                             or name.upper().endswith(('.SF', '.RSA', '.DSA', '.EC'))))}
    def unsigned_entries(path):
        with zipfile.ZipFile(path) as z:
            return {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist()
                    if not n.endswith('/') and not n.startswith('META-INF/')}
    require(unsigned_entries(a.apk) == unsigned_entries(a.official_apk),
            'Official and F-Droid original code/resources differ')
    for name, data in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(a.dist / name, data)
        require(archive(a.dist / name) == data, 'Archive roundtrip differs')
        listing = run([java, '-jar', a.tools / 'morphe.jar', 'list-patches', '--patches', a.dist / name,
                       '-f', 'com.aurora.store', '-p', '-v'], name + '.listing.txt')
        require(listing.count('Name: ' + PATCH_NAME + '\n') == 1, 'Aurora patch not discovered exactly once')
        for input_name, input_apk, label, tool in ((source, source_apk, generation, jar)
                for source, source_apk in (('fdroid', a.apk), ('upstream', a.official_apk))
                for generation, jar in (('stable', 'morphe.jar'), ('patcher2', 'morphe2.jar'))):
            original_resources = resource_entries(input_apk)
            out = a.work / 'applications' / label / input_name / name
            out.mkdir(parents=True)
            apk, report = out / 'patched.apk', out / 'morphe.json'
            run([java, '-Xmx1536m', '-jar', a.tools / tool, 'patch', input_apk, '-p', a.dist / name,
                 '--exclusive', '-e', PATCH_NAME, '--unsigned', '--bytecode-mode', 'FULL',
                 '-o', apk, '-t', out / 'tmp', '-r', report], label + '-' + input_name + '-' + name + '.apply.log', timeout=900)
            r = json.loads(report.read_text())
            applied = [row if isinstance(row, str) else row.get('name') for row in r.get('appliedPatches', [])]
            steps = r.get('patchingSteps', r.get('stepResults', []))
            require(apk.exists() and not r.get('failedPatches') and applied == [PATCH_NAME]
                    and steps and all(s.get('success') is True for s in steps),
                    'Official APK patch pipeline failed')
            require(resource_entries(apk) == original_resources,
                    'Official APK manifest/resources/assets/native libraries changed')
            with zipfile.ZipFile(apk) as z:
                output_entries = {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')}
            distribution_entry_maps.setdefault((label, input_name), []).append(output_entries)
            comparable_entries = {n: digest for n, digest in output_entries.items()
                                  if not (n.startswith('META-INF/') and (n == 'META-INF/MANIFEST.MF'
                                          or n.upper().endswith(('.SF', '.RSA', '.DSA', '.EC'))))}
            group = entry_maps.setdefault(label, [])
            if group:
                require(comparable_entries == group[0], 'Output differs from the fully audited APK for ' + label)
                audit = audit_cache[label]
                audit_mode = 'exact_non_signature_entry_identity_with_fully_audited_output'
            else:
                audit = run([java, '-ea', '-cp', auditcp, 'AuroraAudit', 'audit-app', input_apk, apk,
                             a.work / 'runtime-dex/classes.dex'], label + '-' + input_name + '-' + name + '.audit.txt')
                require('PASS' in audit, 'Official APK semantic audit incomplete')
                audit_cache[label] = audit
                audit_mode = 'full_semantic_class_and_method_audit'
            group.append(comparable_entries)
            applications.append({'patcher_generation': label, 'input': input_name, 'input_sha256': sha(input_apk), 'bundle': name, 'package': 'com.aurora.store', 'version': '4.8.4',
                                 'result': 'PASS', 'audit_mode': audit_mode, 'audit_summary': audit.splitlines()[-1],
                                 'preserved_original_resources': len(original_resources)})
    require(all(all(row == group[0] for row in group[1:]) for group in entry_maps.values()), 'Standalone, combined, F-Droid and upstream APK contents differ')
    require(all(all(row == group[0] for row in group[1:]) for group in distribution_entry_maps.values()), 'Standalone and combined complete APK entry contents differ')
    require(sources == {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
                        if x.is_file() and x.suffix in ('.java', '.py')}, 'Sources changed during build')
    qa = {'schema': 'aurora-favourites100-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
          'result': 'PASS', 'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'production_android_sdk_compile': True, 'sdk_compile': True, 'bundle_sha256': sha(a.dist / BUNDLE), 'standalone_sha256': sha(a.dist / SINGLE),
          'physical_android_tested': False, 'toolchain_sha256': TOOLS, 'sources': sources,
          'artifacts': {n: {'bytes': (a.dist / n).stat().st_size, 'sha256': sha(a.dist / n)} for n in (SINGLE, BUNDLE)},
          'loader_preservation': preservation, 'preserved_unrelated_entries': len(preserved),
          'runtime_sha256': sha(a.work / 'runtime-dex/classes.dex'), 'host_tests': host, 'original_invalid_override_reproduced': True, 'patched_final_method_hierarchy_verified': True,
          'apk_application': applications, 'official_apk_sha256': sha(a.apk), 'upstream_apk_sha256': sha(a.official_apk),
          'official_apk_non_dex_resources_unchanged': True,
          'single_bundle_apk_entry_contents_equal': True, 'cross_distribution_non_signature_entry_contents_equal': True}
    (a.dist / QA_NAME).write_bytes(json_bytes(qa))
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()
