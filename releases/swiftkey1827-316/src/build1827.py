#!/usr/bin/env python3
"""Build the English up-flick fix while preserving all other MPP entries."""
from __future__ import annotations
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import sys
import time
import urllib.request
import zipfile

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
OLD_SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.26.mpp'
OLD_BUNDLE = 'Hiro_Morphe_Patches_v1.0.315.mpp'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.27.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.316.mpp'
QA_NAME = 'QA_SwiftKey_v1.8.27.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.27_sources_and_QA.zip'
MF = 'META-INF/MANIFEST.MF'
EXT = 'extensions/swiftkey_japanese.mpe'
SHIFT_JVM = 'app/hiro/swiftkey/patches/ShiftLockPatch.class'
STAMP = '2026-10-30T00:00:00'
DESCRIPTION = '英語QWERTYでも上フリックで長押し文字を入力。日本語上フリック、Shift大文字維持、絵文字候補と辞書を保持。実機未確認。'
BASE_RUNTIME_SHA = '94e7af5056df941fc935ff8876663fe5003584ab0e38f8e71f9c8d195f907755'
PINS = {
    OLD_SINGLE: '733d39a89503f56991cdd18e1d7e335dd0dac2afd9380c7cb1f14cff048bea5e',
    OLD_BUNDLE: '57b0a24c3f628ad312d1fad7ea82a4678b4d5081b1af655d477c760cc5c07613',
    'apktool.jar': 'dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423',
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
}
BASE = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/4ea7ad7eba95671ed54ac9cd9408d8b0aa320a0b/downloads/'
URLS = {
    OLD_SINGLE: BASE + OLD_SINGLE, OLD_BUNDLE: BASE + OLD_BUNDLE,
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
    info = zipfile.ZipInfo(name, (2026, 10, 30, 0, 0, 0))
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
    before = archive(source)
    headers = manifest(before[MF])
    require(headers['Version'] == old_version, 'Baseline version differs')
    headers.update(Version=version, Timestamp=STAMP, Description=DESCRIPTION)
    updates = {MF: manifest_bytes(headers), EXT: runtime}
    with zipfile.ZipFile(source) as old, zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED,
                                                       compresslevel=9) as new:
        for info in old.infolist():
            new.writestr(copy.copy(info), updates.get(info.filename, old.read(info)))
    after = archive(target)
    require(before.keys() == after.keys(), 'Archive entry inventory changed')
    changed = sorted(name for name in before if before[name] != after[name])
    require(set(changed) == {MF, EXT}, 'Changes exceed manifest and runtime')
    require(after[EXT] == runtime and runtime != before[EXT], 'Runtime update missing')
    require(after[SHIFT_JVM] == before[SHIFT_JVM], 'Sticky Shift helper changed')
    require(all(after[n] == before[n] for n in before if n.startswith('swiftkey/')),
            'Dictionary resource changed')
    return {'changed_entries': changed, 'added_entries': [],
            'unchanged_entries': len(before) - len(changed),
            'bytes': target.stat().st_size, 'sha256': sha(target.read_bytes()),
            'extension_sha256': sha(runtime), 'all_other_entries_byte_identical': True}


def build(inputs, tools, work, output):
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
    baseline_runtime = work / 'baseline.mpe'
    baseline_runtime.write_bytes(old_single[EXT])
    run([sys.executable, ROOT / 'runtime/build_runtime.py', '--runtime', baseline_runtime,
         '--tools', tools, '--work', work / 'runtime-work', '--output', work / 'runtime'],
        work / 'runtime.log')
    runtime_report = normalize(report(work / 'runtime/runtime-report.json'))
    runtime = (work / 'runtime/runtime.mpe').read_bytes()
    runtime_sha = sha(runtime)
    require(runtime.startswith(b'dex\n') and struct.unpack_from('<I', runtime, 96)[0] == 91,
            'Runtime DEX class inventory differs')
    require(runtime_report.get('baseline_runtime_sha256') == BASE_RUNTIME_SHA
            and runtime_report.get('runtime_sha256') == runtime_sha
            and runtime_report.get('all_other_classes_preserved') is True,
            'Runtime report not bound to preserved baseline and output')
    changed_classes = runtime_report.get('changed_classes')
    require(isinstance(changed_classes, list) and changed_classes
            and len(changed_classes) == len(set(changed_classes))
            and all(re.fullmatch(r'Lhiro/swiftkey/FlickSymbols(?:\$[^;]+)?;', n)
                    for n in changed_classes), 'Unexpected runtime classes changed')
    host = runtime_report.get('host_report', {})
    require(host.get('status') == 'PASSED' and host.get('runtime_sha256') == runtime_sha
            and runtime_report.get('host_tests_passed') is True,
            'Host tests missing or not bound to final runtime')
    for key in ('english_upflick_enabled', 'japanese_upflick_preserved', 'gesture_safety_tests_passed'):
        require(host.get(key) is True, 'Missing host regression result: ' + key)
    route_review = json.loads((ROOT / 'runtime/tests/native_route_review.json').read_text())
    require(route_review.get('status') == 'STATIC_REVIEW_PASSED'
            and route_review.get('blockers') == []
            and route_review.get('production_source_sha256')
            == sha((ROOT / 'runtime/src/hiro/swiftkey/FlickSymbols.java').read_bytes())
            and route_review.get('reference_apk_sha256')
            == 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7',
            'Independent native route review differs from production source')
    results = {}
    for kind, old, new, old_version, version in (
            ('single', OLD_SINGLE, SINGLE, '1.8.26', '1.8.27'),
            ('bundle', OLD_BUNDLE, BUNDLE, '1.0.315', '1.0.316')):
        results[kind] = package(inputs / old, output / new, runtime, old_version, version)
    new_single, new_bundle = archive(output / SINGLE), archive(output / BUNDLE)
    require(new_single[EXT] == new_bundle[EXT], 'Final package runtime differs')
    cls = work / 'loader-test'
    cls.mkdir()
    run(['java', 'com.sun.tools.javac.Main', '--release', '8', '-g:none', '-d', cls,
         ROOT / 'tests/LoaderParityProbe.java'], work / 'javac-loader.log')
    run(['java', 'com.sun.tools.javac.Main', '-d', cls, ROOT / 'loader/LoaderResourceProbe.java'],
        work / 'javac-resources.log')
    final_resources = []
    for kind, name in (('single', SINGLE), ('bundle', BUNDLE)):
        for sdk in ('morphe.jar', 'morphe-current.jar'):
            dest = work / (kind + '-' + sdk + '.resources.json')
            run([os.environ.get('MORPHE_JAVA', 'java'), '-Xmx1536m', '-cp',
                 str(cls) + os.pathsep + str(tools / sdk), 'LoaderResourceProbe', output / name, dest],
                work / (kind + '-' + sdk + '.resources.log'))
            resource = report(dest)
            require(resource['mpp_sha256'] == sha((output / name).read_bytes())
                    and resource['all_eight_resource_hashes_verified'] is True,
                    'Resource verification not bound to final artifact')
            final_resources.append({'kind': kind, 'sdk': sdk, **resource})
    loaders = []
    for sdk in ('morphe.jar', 'morphe-current.jar'):
        dest = work / (sdk + '.loader.json')
        run([os.environ.get('MORPHE_JAVA', 'java'), '-Xmx1536m', '-cp',
             str(cls) + os.pathsep + str(tools / sdk), 'LoaderParityProbe', inputs / OLD_BUNDLE,
             output / SINGLE, output / BUNDLE, dest], work / (sdk + '.loader.log'))
        loaders.append(report(dest))
    require(loaders[0] == loaders[1], 'Morphe loaders disagree')
    require(loaders[0]['selected_extension_sha256'] == runtime_sha
            and loaders[0]['baseline_extension_sha256'] == BASE_RUNTIME_SHA, 'Loaded extension differs')
    qa = {
        'status': 'ENGLISH_UPFLICK_HOST_VERIFIED_DEVICE_UNVERIFIED',
        'swiftkey_version': '1.8.27', 'bundle_version': '1.0.316',
        'baseline_swiftkey_version': '1.8.26', 'baseline_bundle_version': '1.0.315',
        'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4',
        'target_version_code': 1236271168, 'device_tested': False, 'original_apk_apply_tested': False,
        'runtime_byte_identical': False, 'runtime_sha256': runtime_sha,
        'all_dictionary_resources_preserved': True, 'shift_helper_byte_identical': True,
        'other_apps_byte_identical': True, 'unrelated_loader_entries_byte_identical': True,
        'all_loaders_byte_identical': True, 'standalone_and_bundle_runtime_equal': True,
        'host_tests_passed': True, 'two_loader_sdks_agree': True,
        'host_report': host, 'runtime_report': runtime_report,
        'native_route_review': route_review,
        'loader_reports': loaders, 'final_resource_reports': final_resources,
        'single': results['single'], 'bundle': results['bundle'],
        'dictionary_resources': {n: {'sha256': sha(raw), 'bytes': len(raw), 'byte_identical': True}
                                 for n, raw in sorted(old_single.items()) if n.startswith('swiftkey/')},
        'inputs': {name: {'url': URLS[name], 'sha256': pin} for name, pin in PINS.items()},
        'source_sha256': {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in source_paths()},
    }
    (output / QA_NAME).write_text(json.dumps(qa, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    (output / 'RELEASE_NOTES.txt').write_bytes((ROOT / 'RELEASE_NOTES.txt').read_bytes())
    with zipfile.ZipFile(output / SOURCE_ZIP, 'w') as z:
        for path in source_paths():
            zip_add(z, 'src/' + path.relative_to(ROOT).as_posix(), path.read_bytes())
        zip_add(z, QA_NAME, (output / QA_NAME).read_bytes())
    names = (SINGLE, BUNDLE, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt')
    (output / 'SHA256SUMS.txt').write_text(''.join(sha((output / name).read_bytes())
                                                 + '  ' + name + '\n' for name in names))
    print('PASS SwiftKey 1.8.27 / bundle 1.0.316', flush=True)
    return qa


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input-dir', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--download', action='store_true')
    args = parser.parse_args()
    inputs, tools, work, output = (getattr(args, n).resolve()
                                  for n in ('input_dir', 'tools', 'work', 'output'))
    if args.download:
        download(inputs, tools)
    build(inputs, tools, work, output)
