#!/usr/bin/env python3
"""Compile the exact runtime source against the real Android 35 public SDK.

Java 8 source mode permits ECJ's Android boot class path. This is an API check;
the released runtime remains the separately verified Java 11/D8 build.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse
import hashlib
import json
import struct
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SDK_SHA = '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def constants(raw):
    """Read public constant values without initializing Android stub classes."""
    cursor = 8
    def u2():
        nonlocal cursor
        value = struct.unpack_from('>H', raw, cursor)[0]
        cursor += 2
        return value
    def u4():
        nonlocal cursor
        value = struct.unpack_from('>I', raw, cursor)[0]
        cursor += 4
        return value
    count = u2()
    pool = [None] * count
    index = 1
    while index < count:
        tag = raw[cursor]
        cursor += 1
        if tag == 1:
            size = u2()
            pool[index] = raw[cursor:cursor + size].decode('utf-8', errors='replace')
            cursor += size
        elif tag in (3, 4):
            pool[index] = struct.unpack_from('>i' if tag == 3 else '>f', raw, cursor)[0]
            cursor += 4
        elif tag in (5, 6):
            pool[index] = struct.unpack_from('>q' if tag == 5 else '>d', raw, cursor)[0]
            cursor += 8
            index += 1
        elif tag == 8:
            pool[index] = ('String', u2())
        elif tag in (7, 16, 19, 20):
            cursor += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            cursor += 4
        elif tag == 15:
            cursor += 3
        else:
            raise RuntimeError('Unknown class constant tag ' + str(tag))
        index += 1
    cursor += 6
    interfaces = u2()
    cursor += interfaces * 2
    result = {}
    for _ in range(u2()):
        flags, name, descriptor, count = u2(), u2(), u2(), u2()
        for _ in range(count):
            attribute, size = pool[u2()], u4()
            if attribute == 'ConstantValue' and flags & 0x19 == 0x19:
                assert size == 2
                value = pool[u2()]
                if isinstance(value, tuple):
                    value = pool[value[1]]
                result[pool[name] + ':' + pool[descriptor]] = value
            else:
                cursor += size
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--android-jar', type=Path, required=True)
    parser.add_argument('--runtime-root', type=Path, default=ROOT / 'runtime')
    parser.add_argument('--runtime-report', type=Path, required=True)
    args = parser.parse_args()
    assert sha(args.android_jar) == SDK_SHA, 'SDK differs from the established official pin'
    source = {str(p.relative_to(args.runtime_root)): sha(p)
              for p in sorted((args.runtime_root / 'runtime_src').rglob('*.java'))}
    runtime = json.loads(args.runtime_report.read_text())
    assert runtime['source_sha256'] == source, 'Runtime evidence differs from current source'
    config = json.loads((ROOT / 'tool-paths.json').read_text())
    sdk_classes, stub_classes = ROOT / 'qa/sdk-classes', ROOT / 'qa/sdk-stub-classes'
    for path in (sdk_classes, stub_classes):
        path.mkdir(exist_ok=True)
    result = subprocess.run([config['java'], '-jar', config['tools']['ecj.jar']['path'],
                             '-source', '8', '-target', '8', '-encoding', 'UTF-8',
                             '-bootclasspath', str(args.android_jar), '-d', str(sdk_classes),
                             *map(str, sorted((args.runtime_root / 'runtime_src').rglob('*.java')))],
                            capture_output=True, text=True)
    (ROOT / 'qa/sdk_compile.log').write_text(result.stdout + result.stderr)
    result.check_returncode()
    subprocess.run([config['java'], '-jar', config['tools']['ecj.jar']['path'],
                    '-source', '11', '-target', '11', '-encoding', 'UTF-8',
                    '-d', str(stub_classes),
                    *map(str, sorted((args.runtime_root / 'runtime_stubs').rglob('*.java')))], check=True)
    checked = {}
    with ZipFile(args.android_jar) as sdk:
        for path in sorted(stub_classes.rglob('*.class')):
            own = constants(path.read_bytes())
            if not own:
                continue
            name = path.relative_to(stub_classes).as_posix()
            actual = constants(sdk.read(name))
            assert all(actual.get(key) == value for key, value in own.items()), name
            checked[name] = own
    report = {
        'schema': 'berry-repair339-real-sdk-compile-v1', 'result': 'PASS',
        'android_sdk_api': 35, 'android_sdk_revision': 'platform-35_r02',
        'android_jar_sha256': SDK_SHA,
        'official_archive_url': 'https://dl.google.com/android/repository/platform-35_r02.zip',
        'official_archive_sha256': '0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0',
        'source_sha256': source,
        'sdk_compile_without_custom_stubs_passed': True,
        'stub_constants_match_sdk': checked,
        'stub_constant_count': sum(len(values) for values in checked.values()),
        'all_present_stub_constants_checked': True,
        'compile_log_sha256': sha(ROOT / 'qa/sdk_compile.log'),
        'release_build_replaced': False, 'android_api_26_runtime_tested': False,
        'physical_device_tested': False,
    }
    (ROOT / 'qa/SDK_compile339.json').write_text(json.dumps(report, indent=2) + '\n')
    print('PASS runtime source compiles against actual Android 35 SDK; compile-only constants match')


if __name__ == '__main__':
    main()
