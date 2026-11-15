#!/usr/bin/env python3
"""Build bundle 373 / Berry 1.4.6 by replacing only the generic resolver family.

All original runtime extensions and every other JVM/DEX provider are retained.
This build audits packaging and provider loading; APK and device tests are separate.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import zipfile

BASE_SHA = '98316a950529a70895f568e56ed1d42df16c833509371d8aa5efe4d2c61af071'
TOOLS = {
    'classpath.jar': 'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864',
    'ecj.jar': '74c93172475fee0cd4e6eefb44233516dd40525642da35d009146d97ea598229',
    'd8.jar': '38a9ffaaebbd17d1514e3ecbff5a85b989bb13c551b9e224c7409514eea84ff5',
}
MF = 'META-INF/MANIFEST.MF'
RESOLVER = 'app/hiro/berry/patches/GenericBerryResolver'
PREFIXES = ('app/hiro/berry/', 'app/hiro/berryshare/', 'app/hiro/berryzoom/', 'app/hiro/berryqueue/', 'app/hiro/berrybfcache/')
EXTENSIONS = ('extensions/berry_foreground_tabs.mpe', 'extensions/berry_browser_visibility.mpe',
              'extensions/berry_onehand_zoom.mpe', 'extensions/berry_messagequeue.mpe', 'extensions/berry_bfcache_restore.mpe')
COMBINED, SINGLE = 'Hiro_Morphe_Patches_v1.0.373.mpp', 'Berry_Browser_v1.4.6.mpp'

def require(value, why):
    if not value:
        raise RuntimeError(why)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None, 'Invalid archive')
        require(all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts and '\\' not in n for n in z.namelist()), 'Unsafe entry')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}

def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name, raw in sorted(entries.items(), key=lambda row: (row[0] != MF, row[0])):
            info = zipfile.ZipInfo(name, (2026, 11, 15, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest headers')
    return dict(pairs)

def manifest(fields):
    out = bytearray()
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
        row = (key + ': ' + value).encode()
        while len(row) > 70:
            end = 70
            while row[end] & 0xc0 == 0x80:
                end -= 1
            out += row[:end] + b'\r\n'
            row = b' ' + row[end:]
        out += row + b'\r\n'
    raw = bytes(out) + b'\r\n'
    require(headers(raw) == fields, 'Manifest roundtrip failed')
    return raw

def replaceable(name):
    return name == RESOLVER + '.class' or (name.startswith(RESOLVER + '$') and name.endswith('.class'))

def main():
    p = argparse.ArgumentParser(description=__doc__)
    for key in ('base', 'src', 'tools', 'out'):
        p.add_argument('--' + key, type=Path, required=True)
    p.add_argument('--java', type=Path, default=Path('/usr/bin/java'))
    a = p.parse_args()
    for key in vars(a):
        setattr(a, key, getattr(a, key).resolve())
    require(sha(a.base) == BASE_SHA, 'Baseline 372 hash differs')
    for name, pin in TOOLS.items():
        require(sha(a.tools / name) == pin, 'Tool differs: ' + name)
    source = a.src / (RESOLVER + '.java') if a.src.is_dir() else a.src
    require(source.is_file() and source.name == 'GenericBerryResolver.java', 'Resolver source missing')
    source_hash = sha(source)
    require(not a.out.exists() or not any(a.out.iterdir()), 'Use an empty output directory')
    work = a.out / 'work'
    work.mkdir(parents=True)
    for name in ('classes', 'dex', 'build-tools'):
        (work / name).mkdir()
    def run(args, label):
        result = subprocess.run(list(map(str, args)), capture_output=True, text=True, timeout=300)
        (work / label).write_text(result.stdout + result.stderr)
        require(result.returncode == 0, label + ' failed:\n' + (result.stdout + result.stderr)[-12000:])
        return result.stdout
    run([a.java, '-version'], 'java.log')
    require('21.0.13' in (work / 'java.log').read_text(), 'Use reviewed Java 21.0.13')
    base = archive(a.base)
    fields = headers(base[MF])
    require(fields['Version'] == '1.0.372' and all(n in base for n in EXTENSIONS), 'Baseline inventory differs')
    write_zip(work / 'baseline-jvm.jar', {n: b for n, b in base.items() if n.endswith('.class')})
    cp = os.pathsep.join(map(str, (a.tools / 'classpath.jar', work / 'baseline-jvm.jar')))
    ecj = [a.java, '-jar', a.tools / 'ecj.jar', '-encoding', 'UTF-8']
    run([*ecj, '-source', '8', '-target', '8', '-g:source,lines', '-cp', cp, '-d', work / 'classes', source], 'compile-resolver.log')
    fresh = {q.relative_to(work / 'classes').as_posix(): q.read_bytes() for q in (work / 'classes').rglob('*.class')}
    require(fresh and all(replaceable(n) for n in fresh), 'Compiled classes exceed resolver family')
    write_zip(work / 'resolver.jar', fresh)
    run([a.java, '-cp', a.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--classpath', a.tools / 'classpath.jar', '--classpath', work / 'baseline-jvm.jar', '--output', work / 'dex', work / 'resolver.jar'], 'd8.log')
    require({q.name for q in (work / 'dex').iterdir()} == {'classes.dex'}, 'Unexpected DEX output')
    merger = Path(__file__).with_name('MergeResolver.java')
    run([*ecj, '-source', '11', '-target', '11', '-cp', a.tools / 'classpath.jar', '-d', work / 'build-tools', merger], 'compile-merger.log')
    (work / 'baseline.dex').write_bytes(base['classes.dex'])
    run([a.java, '-cp', os.pathsep.join(map(str, (work / 'build-tools', a.tools / 'classpath.jar'))), 'MergeResolver',
         work / 'baseline.dex', work / 'dex/classes.dex', work / 'combined.dex', work / 'single.dex', work / 'dex-preservation.json'], 'merge.log')
    dex_audit = json.loads((work / 'dex-preservation.json').read_text())
    combined = {n: b for n, b in base.items() if not replaceable(n)}
    combined.update(fresh)
    combined['classes.dex'] = (work / 'combined.dex').read_bytes()
    fields.update(Version='1.0.373', Timestamp='2026-11-15T00:00:00',
                  Description='Unreleased candidate: decouple one-hand zoom detection from unused foreground-tab methods. Berry 3.84.21 not yet verified.')
    combined[MF] = manifest(fields)
    single = {n: b for n, b in combined.items() if (n.endswith('.class') and n.startswith(PREFIXES)) or n in EXTENSIONS}
    single_fields = dict(fields, Name='Berry Browser', Version='1.4.6')
    single.update({MF: manifest(single_fields), 'classes.dex': (work / 'single.dex').read_bytes()})
    unchanged = [n for n in base if n not in (MF, 'classes.dex') and not replaceable(n)]
    require(all(combined[n] == base[n] for n in unchanged), 'Unrelated archive entry changed')
    require(all(combined[n] == single[n] == base[n] for n in EXTENSIONS), 'Runtime extension changed')
    require(all(replaceable(n) for n in set(base) ^ set(combined)), 'Unexpected entry addition/removal')
    outputs = {}
    for name, entries in ((COMBINED, combined), (SINGLE, single)):
        write_zip(a.out / name, entries)
        require(archive(a.out / name) == entries, 'Output roundtrip failed')
        outputs[name] = {'sha256': sha(a.out / name), 'bytes': (a.out / name).stat().st_size}
        output = run([a.java, '-jar', a.tools / 'classpath.jar', 'list-patches', '--patches', a.out / name, '-f', 'jp.ejimax.berrybrowser', '-p', '-v'], name + '.metadata.log')
        require(output.count('Name: ') == 5 and 'ダブルタップから指を離さず片手ズーム' in output, 'Berry provider inventory differs')
    require(sha(source) == source_hash, 'Source changed during build')
    report = {'schema': 'berry-resolver373-build-v1', 'result': 'PASS', 'baseline_sha256': BASE_SHA,
              'bundle_version': '1.0.373', 'berry_version': '1.4.6', 'resolver_source_sha256': source_hash,
              'artifacts': outputs, 'tool_sha256': TOOLS, 'dex_preservation': dex_audit,
              'all_runtime_extensions_identical': True, 'unchanged_archive_entries': len(unchanged),
              'all_unrelated_archive_entries_identical': True, 'physical_device_tested': False,
              'release_status': 'unreleased-candidate', 'actual_3_84_21_verified': False,
              'runtime_extensions_sha256': {n: hashlib.sha256(base[n]).hexdigest() for n in EXTENSIONS}}
    (a.out / 'QA_build373.json').write_text(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    print(json.dumps({'result': 'PASS_BUILD', 'artifacts': outputs}), flush=True)

if __name__ == '__main__':
    main()
