#!/usr/bin/env python3
"""Build an independently selectable SwiftKey Beta privacy patch.

The active input/learning patch is copied unchanged from bundle 1.0.361.
The APK used for verification is private and is never a release artifact.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_VERSION, VERSION, BUNDLE_VERSION = '1.0.361', '1.0.0', '1.0.362'
BASE_SHA = '94657960f0d8628c8294d67a793dfbcab94659f9554c5ecd93b34173f8fed2d8'
PREFIX = 'app/hiro/swiftkeyprivacy/patches/'
NAME = 'テレメトリ・解析・エラー報告を停止'
CORE_NAME = '日本語入力・予測変換を改善しSamsung絵文字に統一'
PACKAGE, APP_VERSION = 'com.touchtype.swiftkey.beta', '9.13.16.4'
MF = 'META-INF/MANIFEST.MF'
SINGLE = 'SwiftKeyBeta_Privacy_v1.0.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.362.mpp'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-2.2.jar': '855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


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
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [x.decode().split(': ', 1) for x in rows]
    require(all(len(x) == 2 for x in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest')
    return dict(pairs)


def manifest(fields):
    rows = []
    for k, v in fields.items():
        require(isinstance(v, str) and not any(c in v for c in '\r\n\0'), 'Invalid manifest value')
        raw, prefix = (k + ': ' + v).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            rows.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        rows.append(prefix + raw)
    out = b'\r\n'.join(rows) + b'\r\n\r\n'
    require(headers(out) == fields, 'Manifest round trip differs')
    return out


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for n, raw in sorted(entries.items(), key=lambda x: (x[0] != MF, x[0])):
            info = zipfile.ZipInfo(n, (2026, 11, 6, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, raw)


def class_files(root):
    return {p.relative_to(root).as_posix(): p.read_bytes() for p in sorted(root.rglob('*.class'))}


def main():
    p = argparse.ArgumentParser()
    for flag in ('base', 'tools', 'work', 'dist', 'java-home'):
        p.add_argument('--' + flag, type=Path, required=True)
    p.add_argument('--timestamp', required=True)
    a = p.parse_args()
    for field in ('base', 'tools', 'work', 'dist', 'java_home'):
        setattr(a, field, getattr(a, field).resolve())
    require(sha(a.base) == BASE_SHA, 'Baseline bundle changed')
    require(re.fullmatch(r'\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d', a.timestamp), 'JST timestamp required')
    for n, expected in TOOLS.items():
        require(sha(a.tools / n) == expected, 'Tool changed: ' + n)
    require(not a.work.exists(), 'Use a fresh build directory')
    require(not a.dist.exists() or not any(a.dist.iterdir()), 'Output directory must be empty')
    for path in (a.work, a.dist, a.work / 'patch', a.work / 'test', a.work / 'dex', a.work / 'tmp'):
        path.mkdir(parents=True, exist_ok=True)
    java, javac = a.java_home / 'bin/java', a.java_home / 'bin/javac'
    java_flags = ['-XX:-UsePerfData', '-Djava.io.tmpdir=' + str(a.work / 'tmp')]
    env = dict(os.environ, TMPDIR=str(a.work / 'tmp'), MORPHE_DATA_DIR=str(a.work / 'morphe-data'))

    def run(command, name, timeout=240):
        print('RUN ' + name, flush=True)
        with (a.work / name).open('w') as f:
            result = subprocess.run(list(map(str, command)), stdout=f, stderr=subprocess.STDOUT,
                                    timeout=timeout, env=env)
        output = (a.work / name).read_text()
        require(result.returncode == 0, name + ' failed:\n' + output[-12000:])
        return output.strip()

    require(run([javac, '-J-XX:-UsePerfData', '-version'], 'javac.txt') == 'javac 21.0.13', 'Pinned JDK required')
    run([javac, '-J-XX:-UsePerfData', '--release', '8', '-encoding', 'UTF-8', '-cp', a.tools / 'morphe.jar',
         '-d', a.work / 'patch', *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.txt')
    cp = os.pathsep.join(map(str, (a.work / 'test', a.work / 'patch', a.tools / 'morphe.jar')))
    run([javac, '-J-XX:-UsePerfData', '-encoding', 'UTF-8', '-cp', cp,
         '-d', a.work / 'test', *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.txt')
    patch = class_files(a.work / 'patch')
    require(patch and all(n.startswith(PREFIX) for n in patch), 'Unexpected provider classes')
    write_zip(a.work / 'patch.jar', patch)
    run([java, *java_flags, '-cp', a.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release',
         '--min-api', '26', '--lib', a.tools / 'android.jar', '--classpath', a.tools / 'morphe.jar',
         '--output', a.work / 'dex', a.work / 'patch.jar'], 'patch-d8.txt')
    require({p.name for p in (a.work / 'dex').iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    base = archive(a.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline manifest version mismatch')
    require(not any(n.startswith(PREFIX) for n in base), 'Privacy provider already exists')
    (a.work / 'base.dex').write_bytes(base['classes.dex'])
    merge = run([java, *java_flags, '-Xmx2g', '-ea', '-cp', cp, 'PrivacyAudit', 'merge',
                 a.work / 'base.dex', a.work / 'dex/classes.dex', a.work / 'merged.dex'], 'merge.txt')
    require('PASS' in merge, 'Provider preservation not verified')
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp=a.timestamp,
                  Description='Add an optional SwiftKey Beta 9.13.16.4 privacy patch. Preserve existing input, learning, sync, and unrelated patches. Device behavior unverified.')
    combined = {**base, **patch, 'classes.dex': (a.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    single = {**patch, 'classes.dex': (a.work / 'dex/classes.dex').read_bytes(),
              MF: manifest({**fields, 'Name': 'SwiftKey Beta Privacy', 'Version': VERSION})}
    preserved = {n: raw for n, raw in base.items() if n not in (MF, 'classes.dex')}
    require(all(combined[n] == raw for n, raw in preserved.items()), 'Existing bundle entry changed')
    selections = []
    for filename, entries in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(a.dist / filename, entries)
        require(archive(a.dist / filename) == entries, 'Artifact ZIP round trip differs')
        for loader in ('morphe.jar', 'morphe-2.2.jar'):
            listing = run([java, *java_flags, '-jar', a.tools / loader, 'list-patches', '-p', '-v',
                           '--patches', a.dist / filename, '-f', PACKAGE], filename + '.' + loader + '.listing.txt')
            require(listing.count('Name: ' + NAME + '\n') == 1, 'Privacy patch is not discoverable exactly once')
            if filename == BUNDLE:
                require(listing.count('Name: ' + CORE_NAME + '\n') == 1, 'Existing input patch disappeared')
            scp = os.pathsep.join(map(str, (a.work / 'test', a.tools / loader)))
            selection = run([java, *java_flags, '-ea', '-cp', scp, 'PrivacyAudit', 'selection',
                             a.dist / filename, 'integrated' if filename == BUNDLE else 'standalone',
                             *([a.base] if filename == BUNDLE else [])], filename + '.' + loader + '.selection.txt')
            require('PASS' in selection, 'Independent default-off selection not verified')
            selections.append({'artifact': filename, 'loader': loader, 'result': selection})
    report = {
        'schema': 'swiftkey-privacy100-build-v1', 'baseline_version': BASE_VERSION,
        'baseline_sha256': BASE_SHA, 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
        'patch_name': NAME, 'release_timestamp': a.timestamp, 'default': False,
        'preserved_bundle_entries': len(preserved), 'loader_preservation': merge,
        'selection': selections,
        'preserved_swiftkey_runtime_sha256': hashlib.sha256(base['extensions/swiftkey_japanese.mpe']).hexdigest(),
        'artifacts': {n: {'bytes': (a.dist / n).stat().st_size, 'sha256': sha(a.dist / n)} for n in (SINGLE, BUNDLE)},
        'source_sha256': {p.relative_to(ROOT).as_posix(): sha(p) for p in sorted(ROOT.rglob('*'))
                          if p.is_file() and p.suffix in ('.java', '.py')},
        'physical_android_tested': False, 'network_capture_tested': False,
    }
    (a.work / 'build.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(report['artifacts'], ensure_ascii=False))


if __name__ == '__main__':
    main()
