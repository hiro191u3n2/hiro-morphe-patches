#!/usr/bin/env python3
"""Build the Japanese-space runtime repair from pinned MPPs."""
from __future__ import annotations
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.request
import zipfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.30.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.342.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.31.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.343.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.31.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.31_sources_and_QA.zip'
MF = 'META-INF/MANIFEST.MF'
EXT = 'extensions/swiftkey_japanese.mpe'
JVM = 'app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
SPACE_JVM = 'app/hiro/swiftkey/patches/SpaceConversionPatch.class'
STAMP = '2026-11-03T00:00:00'
DESCRIPTION = '現在の生の入力と候補リクエストの完全一致を先に確認するようスペース変換を修正。候補順・辞書・キー操作を保持。実機未確認。'
BASE_RUNTIME_SHA = 'cc68b2e468136950b959a9457a0925404f8656b1a10f8611da683bac4892f8fe'
PINS = {
    OLD_SINGLE: '46ecc6db730dee36a8b2d38e597236e4bf1080cea8947dffe38736ce010b8478',
    OLD_BUNDLE: '252728d95caca44bded3635824408753dc5b29b871d4219797a9657e69f44600',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
}
URLS = {
    OLD_SINGLE: 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/630d3091a36f886aa21a24e32cc8727fd0ec3613/downloads/' + OLD_SINGLE,
    OLD_BUNDLE: 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/630d3091a36f886aa21a24e32cc8727fd0ec3613/downloads/' + OLD_BUNDLE,
    'apktool.jar': 'https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar',
    'morphe.jar': 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.17.0-dev.6/morphe-desktop-1.17.0-dev.6-all.jar',
    'morphe-current.jar': 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.19.1/morphe-desktop-1.19.1-all.jar',
    'r8.jar': 'https://storage.googleapis.com/r8-releases/raw/8.7.18/r8.jar',
    'android.jar': 'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar',
}


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def run(command, log):
    result = subprocess.run(list(map(str, command)), stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True)
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(result.stdout)
    print(result.stdout, end='', flush=True)
    require(result.returncode == 0, 'Command failed; inspect ' + str(log))
    return result.stdout


def report(path):
    data = json.loads(path.read_text())
    require(data.get('status') == 'PASSED', 'Component verification failed: ' + str(path))
    return data


def normalize(value):
    if isinstance(value, dict):
        return {k: normalize(v) for k, v in value.items()
                if k not in ('elapsed_ms', 'elapsed_seconds')}
    if isinstance(value, list):
        return [normalize(v) for v in value]
    return value


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(len(z.namelist()) == len(set(z.namelist())) and z.testzip() is None,
                'Invalid archive: ' + str(path))
        return {name: z.read(name) for name in z.namelist()}


def manifest(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Malformed manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs) and len(dict(pairs)) == len(pairs),
            'Manifest keys invalid')
    return dict(pairs)


def manifest_bytes(values):
    lines = []
    for key, value in values.items():
        line = b''
        for char in key + ': ' + value:
            raw = char.encode()
            if len(line) + len(raw) > 70:
                lines.append(line)
                line = b' '
            line += raw
        lines.append(line)
    result = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(manifest(result) == values, 'Manifest roundtrip differs')
    return result


def zip_add(z, name, raw):
    info = zipfile.ZipInfo(name, (2026, 11, 3, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    z.writestr(info, raw, compresslevel=9)


def download(inputs, tools):
    for name, pin in PINS.items():
        path = (inputs if name.endswith('.mpp') else tools) / name
        if path.exists():
            require(sha(path.read_bytes()) == pin, 'Input pin differs: ' + name)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        for attempt in range(3):
            try:
                raw = urllib.request.urlopen(URLS[name], timeout=120).read()
                require(sha(raw) == pin, 'Downloaded pin differs: ' + name)
                path.write_bytes(raw)
                break
            except Exception:
                if attempt == 2:
                    raise
                time.sleep(2 * (attempt + 1))


def source_paths():
    paths = sorted(p for p in ROOT.rglob('*') if p.is_file() and '__pycache__' not in p.parts)
    require(all(not p.is_symlink() for p in paths), 'Symlink source not allowed')
    require(all(p.suffix.lower() not in ('.apk', '.apks', '.apkm', '.aab', '.jks', '.keystore')
                for p in paths), 'Application or signing-key bytes in source tree')
    return paths


def package(source, target, runtime, old_version, version):
    from publish1831 import validate_archive_delta
    before = archive(source)
    headers = manifest(before[MF])
    require(headers['Version'] == old_version, 'Baseline version differs')
    headers.update(Version=version, Timestamp=STAMP, Description=DESCRIPTION)
    updates = {MF: manifest_bytes(headers), EXT: runtime}
    with zipfile.ZipFile(source) as old, zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED,
                                                       compresslevel=9) as new:
        for info in old.infolist():
            new.writestr(copy.copy(info), updates.get(info.filename, old.read(info)))
        for name in sorted(set(updates) - set(before)):
            zip_add(new, name, updates[name])
    after = archive(target)
    changed = validate_archive_delta(before, after, old_version, version, target.name)
    require(after[EXT] == runtime and runtime != before[EXT], 'Runtime update missing')
    require(all(after[n] == before[n] for n in before if n.startswith('swiftkey/')),
            'Dictionary resource changed')
    return {'changed_entries': changed, 'added_entries': sorted(set(after) - set(before)),
            'unchanged_entries': len(before) - len(changed),
            'bytes': target.stat().st_size, 'sha256': sha(target.read_bytes()),
            'extension_sha256': sha(runtime), 'all_other_entries_byte_identical': True}


def build(inputs, tools, work, output, apk=None):
    from publish1831 import REQUIRED_QA, REQUIRED_ADDED
    for name, pin in PINS.items():
        path = (inputs if name.endswith('.mpp') else tools) / name
        require(sha(path.read_bytes()) == pin, 'Pinned input differs: ' + name)
    require(not work.exists(), 'Fresh build work directory required')
    require(not output.exists() or not any(output.iterdir()), 'Fresh output directory required')
    work.mkdir(parents=True)
    output.mkdir(parents=True, exist_ok=True)
    old_single, old_bundle = archive(inputs / OLD_SINGLE), archive(inputs / OLD_BUNDLE)
    require(old_single[EXT] == old_bundle[EXT] and sha(old_single[EXT]) == BASE_RUNTIME_SHA,
            'Baseline runtime pin differs')
    for name in old_single:
        if name.startswith(('swiftkey/', 'app/hiro/swiftkey/')):
            require(old_single[name] == old_bundle[name], 'Baseline packages differ: ' + name)
    run([sys.executable, ROOT / 'tests/check_native_space_route.py', '--output', work / 'native-route.json'],
        work / 'native-route.log')
    native_route = report(work / 'native-route.json')
    baseline_runtime = work / 'baseline.mpe'
    baseline_runtime.write_bytes(old_single[EXT])
    run([sys.executable, ROOT / 'runtime/build_runtime.py', '--runtime', baseline_runtime,
         '--tools', tools, '--work', work / 'runtime-work', '--output', work / 'runtime']
         + (['--apk', apk] if apk else []),
        work / 'runtime.log')
    runtime_report = normalize(report(work / 'runtime/runtime-report.json'))
    runtime = (work / 'runtime/runtime.mpe').read_bytes()
    runtime_sha = sha(runtime)
    require(runtime_report.get('baseline_runtime_sha256') == BASE_RUNTIME_SHA
            and runtime_report.get('runtime_sha256') == runtime_sha
            and runtime_report.get('all_other_classes_preserved') is True
            and runtime_report.get('host_tests_passed') is True,
            'Runtime verification not bound to preserved baseline and output')
    results = {}
    for kind, old, new, old_version, version in (
            ('single', OLD_SINGLE, SINGLE, '1.8.30', '1.8.31'),
            ('bundle', OLD_BUNDLE, BUNDLE, '1.0.342', '1.0.343')):
        results[kind] = package(inputs / old, output / new, runtime, old_version, version)
    new_single, new_bundle = archive(output / SINGLE), archive(output / BUNDLE)
    require(new_single[EXT] == new_bundle[EXT], 'Final package runtime differs')
    cls = work / 'loader-test'
    cls.mkdir()
    run(['java', 'com.sun.tools.javac.Main', '--release', '8', '-g:none', '-d', cls,
         ROOT / 'tests/LoaderParityProbe.java'], work / 'javac-loader.log')
    run(['java', 'com.sun.tools.javac.Main', '-d', cls, ROOT / 'loader/LoaderResourceProbe.java'],
        work / 'javac-resources.log')
    resources = []
    for kind, name in [('single', SINGLE), ('bundle', BUNDLE)]:
        for sdk in ('morphe.jar', 'morphe-current.jar'):
            dest = work / (kind + '-' + sdk + '.resources.json')
            run([os.environ.get('MORPHE_JAVA', 'java'), '-Xmx1536m', '-cp', str(cls) + os.pathsep + str(tools / sdk),
                 'LoaderResourceProbe', output / name, dest], work / (kind + '-' + sdk + '.resources.log'))
            resource = report(dest)
            require(resource['mpp_sha256'] == sha((output / name).read_bytes()), 'Resource probe targets wrong MPP')
            resources.append({'kind': kind, 'sdk': sdk, **resource})
    loaders = []
    for sdk in ('morphe.jar', 'morphe-current.jar'):
        dest = work / (sdk + '.loader.json')
        run([os.environ.get('MORPHE_JAVA', 'java'), '-Xmx1536m', '-cp', str(cls) + os.pathsep + str(tools / sdk),
             'LoaderParityProbe', inputs / OLD_BUNDLE, output / SINGLE, output / BUNDLE, dest], work / (sdk + '.loader.log'))
        loaders.append(report(dest))
    require(loaders[0] == loaders[1], 'Morphe loaders disagree')
    qa = {k: v for k, v in REQUIRED_QA.items() if '.' not in k}
    qa.update(runtime_sha256=runtime_sha, runtime_report=runtime_report, native_route_report=native_route,
              host_report={'status': 'PASSED', 'runtime_sha256': runtime_sha,
                           'runtime': runtime_report},
              loader_reports=loaders,
              final_resource_reports=resources, single=results['single'], bundle=results['bundle'],
              inputs={name: {'url': URLS[name], 'sha256': pin} for name, pin in PINS.items()},
              source_sha256={p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in source_paths()})
    (output / QA_NAME).write_text(json.dumps(qa, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    (output / 'RELEASE_NOTES.txt').write_bytes((ROOT / 'RELEASE_NOTES.txt').read_bytes())
    with zipfile.ZipFile(output / SOURCE_ZIP, 'w') as z:
        for path in source_paths():
            zip_add(z, 'src/' + path.relative_to(ROOT).as_posix(), path.read_bytes())
        zip_add(z, QA_NAME, (output / QA_NAME).read_bytes())
    names = (SINGLE, BUNDLE, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt')
    (output / 'SHA256SUMS.txt').write_text(''.join(sha((output / n).read_bytes()) + '  ' + n + '\n' for n in names))
    print('PASS SwiftKey 1.8.31 / bundle 1.0.343', flush=True)
    return qa


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    for name in ('input-dir', 'tools', 'work', 'output'):
        p.add_argument('--' + name, type=Path, required=True)
    p.add_argument('--download', action='store_true')
    p.add_argument('--apk', type=Path)
    a = p.parse_args()
    inputs, tools, work, output = (getattr(a, n).resolve() for n in ('input_dir', 'tools', 'work', 'output'))
    if a.download:
        download(inputs, tools)
    build(inputs, tools, work, output, a.apk.resolve() if a.apk else None)
