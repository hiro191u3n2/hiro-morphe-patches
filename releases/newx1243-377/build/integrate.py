#!/usr/bin/env python3
"""Reproducibly add NewX providers to production 376, preserving all prior entries."""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, subprocess, zipfile

BASE_SHA = '864112facfa290aa6e48a3e9a7f117af175e2c25861764a66e716af224674916'
MF = 'META-INF/MANIFEST.MF'

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def require(value, why):
    if not value:
        raise RuntimeError(why)

def read_archive(path):
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        require(len(names) == len(set(names)) and z.testzip() is None, 'Invalid archive')
        require(all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts and '\\' not in n for n in names), 'Unsafe archive name')
        return {n: z.read(n) for n in names if not n.endswith('/')}

def write_archive(path, entries):
    with zipfile.ZipFile(path, 'w') as z:
        for name, data in sorted(entries.items(), key=lambda p: (p[0] != MF, p[0])):
            info = zipfile.ZipInfo(name, (2026, 11, 17, 0, 0, 0))
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            z.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

def headers(raw):
    rows = []
    for row in raw.replace(b'\r\n', b'\n').splitlines():
        if row.startswith(b' '):
            require(bool(rows), 'Orphan continuation')
            rows[-1] += row[1:]
        elif row:
            rows.append(row)
    return dict(r.decode().split(': ', 1) for r in rows)

def manifest(fields):
    output = bytearray()
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
        row = (key + ': ' + value).encode()
        while len(row) > 70:
            end = 70
            while row[end] & 0xc0 == 0x80:
                end -= 1
            output += row[:end] + b'\r\n'
            row = b' ' + row[end:]
        output += row + b'\r\n'
    output += b'\r\n'
    require(headers(bytes(output)) == fields, 'Manifest roundtrip failed')
    return bytes(output)

def main():
    p = argparse.ArgumentParser()
    for name in ('base', 'components', 'tools', 'cli', 'out'):
        p.add_argument('--' + name, type=Path, required=True)
    a = p.parse_args()
    require(sha(a.base) == BASE_SHA, 'Production baseline differs')
    a.out.mkdir(parents=True, exist_ok=True)
    work = a.out / 'work'
    work.mkdir(exist_ok=True)
    base = read_archive(a.base)
    require(headers(base[MF])['Version'] == '1.0.376', 'Wrong production version')
    additions = {}
    component_info = {}
    for name in ('category', 'time', 'back'):
        path = a.components / name / 'component.mpp'
        component = read_archive(path)
        component_info[name] = {'sha256': sha(path), 'bytes': path.stat().st_size}
        (work / (name + '.dex')).write_bytes(component['classes.dex'])
        for entry, data in component.items():
            if entry in (MF, 'classes.dex'):
                continue
            require(entry not in base and entry not in additions, 'Archive entry collision ' + entry)
            require(entry.startswith(('app/hiro/newx/', 'extensions/newx', 'META-INF/hiro-newx/')), 'Unexpected component entry ' + entry)
            additions[entry] = data
    (work / 'baseline.dex').write_bytes(base['classes.dex'])
    cp = str(a.tools / 'classpath.jar')
    compiler = ['java', '-XX:-UsePerfData', '-jar', str(a.tools / 'ecj.jar'), '-source', '11', '-target', '11', '-cp', cp, '-d', str(work)]
    subprocess.run(compiler + [str(Path(__file__).with_name('MergeNewX.java'))], check=True)
    cmd = ['java', '-XX:-UsePerfData', '-cp', os.pathsep.join((str(work), cp)), 'MergeNewX',
           *[str(work / (n + '.dex')) for n in ('baseline', 'category', 'time', 'back')],
           str(work / 'integrated.dex'), str(work / 'standalone.dex'), str(work / 'preservation.json')]
    subprocess.run(cmd, check=True)
    fields = headers(base[MF])
    fields.update(Version='1.0.377', Timestamp='2026-11-17T00:00:00',
                  Description='Add NewX 12.43 category persistence, absolute post times and root Back exit. Existing patches retained.')
    fields['Patcher-Version'] = '2.4.0'
    combined = dict(base)
    combined.update(additions)
    combined[MF] = manifest(fields)
    combined['classes.dex'] = (work / 'integrated.dex').read_bytes()
    single = dict(additions)
    single[MF] = manifest(dict(fields, Name='NewX Hiro Addons', Version='1.0.0'))
    single['classes.dex'] = (work / 'standalone.dex').read_bytes()
    preserved = [n for n in base if n not in (MF, 'classes.dex')]
    require(all(combined[n] == base[n] for n in preserved), 'Existing entry changed')
    outputs = {}
    for name, entries in (('Hiro_Morphe_Patches_v1.0.377.mpp', combined), ('NewX_Hiro_Addons_v1.0.0.mpp', single)):
        path = a.out / name
        write_archive(path, entries)
        require(read_archive(path) == entries, 'Archive roundtrip failed')
        outputs[name] = {'bytes': path.stat().st_size, 'sha256': sha(path)}
        r = subprocess.run(['java', '-XX:-UsePerfData', '-jar', str(a.cli), 'list-patches', '--patches', str(path), '-f', 'com.twitter.android', '-p', '-v'], capture_output=True, text=True)
        (work / (name + '.providers.log')).write_text(r.stdout + r.stderr)
        require(r.returncode == 0, 'Provider loading failed: ' + r.stderr[-3000:])
        require(r.stdout.count('Name: NewX: ') == 3, 'Expected exactly three NewX providers')
    report = {'schema': 'hiro-newx-12430-build-v1', 'result': 'PASS', 'baseline_sha256': BASE_SHA,
              'component_artifacts': component_info, 'artifacts': outputs,
              'preserved_existing_zip_entries': len(preserved), 'all_existing_extensions_identical': True,
              'dex_preservation': json.loads((work / 'preservation.json').read_text()),
              'physical_device_tested': False, 'emulator_tested': False}
    (a.out / 'build-evidence.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(outputs, indent=2), flush=True)

if __name__ == '__main__':
    main()
