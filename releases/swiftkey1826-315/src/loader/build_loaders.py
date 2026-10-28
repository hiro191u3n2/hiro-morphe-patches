#!/usr/bin/env python3
"""Change only the pinned emoji asset digest in both real Morphe loaders."""
import argparse, copy, hashlib, json, os, struct, subprocess, zipfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent
JVM = 'app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
JVM_SHA = 'a247a1c40dd958afb338b2f74986076e0c988380da44cffe523e3b35194cd50f'
OLD_EMOJI = '24ea97801a5756f9f5d7d5244a8e6eb094de8b7c10a9986e5f363fc0fc595e12'
BASE_PINS = {
    'single': 'b6cfb2362dffe42468c8ba795dabd801153b9ed10581b10ba4fab438518165ab',
    'bundle': 'cdc309a2b8a4b4f22cfec05a63d73d53089a6e0d9950ed6a835f457ca8541b5a',
}
LIMIT = 192 * 1024 * 1024

def sha(raw): return hashlib.sha256(raw).hexdigest()
def require(ok, message):
    if not ok: raise RuntimeError(message)
def run(command, log):
    with log.open('w') as out:
        result = subprocess.run(list(map(str, command)), stdout=out, stderr=subprocess.STDOUT)
    require(result.returncode == 0, 'Failed; inspect ' + str(log))

def patch_class(raw, emoji_hash):
    require(sha(raw) == JVM_SHA, 'Baseline JVM pin differs')
    require(raw[:4] == b'\xca\xfe\xba\xbe', 'Invalid class magic')
    require(len(emoji_hash) == 64 and emoji_hash != OLD_EMOJI, 'Changed emoji hash required')
    count = struct.unpack_from('>H', raw, 8)[0]
    offset, index, occurrences = 10, 1, []
    out = bytearray(raw)
    while index < count:
        tag = raw[offset]; offset += 1
        if tag == 1:
            size = struct.unpack_from('>H', raw, offset)[0]; offset += 2
            if raw[offset:offset+size] == OLD_EMOJI.encode():
                require(size == len(emoji_hash), 'Replacement UTF8 length differs')
                out[offset:offset+size] = emoji_hash.encode(); occurrences.append((offset, size))
            offset += size
        elif tag in (3, 4): offset += 4
        elif tag in (5, 6): offset += 8; index += 1
        elif tag in (7, 8, 16, 19, 20): offset += 2
        elif tag in (9, 10, 11, 12, 17, 18): offset += 4
        elif tag == 15: offset += 3
        else: raise RuntimeError('Unknown constant tag ' + str(tag))
        index += 1
    require(len(occurrences) == 1, 'Expected exactly one emoji constant')
    restored = bytearray(out)
    for start, size in occurrences: restored[start:start+size] = raw[start:start+size]
    require(restored == raw and len(out) == len(raw), 'Unrelated JVM data changed')
    return bytes(out), {
        'status': 'PASSED', 'jvm_sha256': sha(out), 'baseline_jvm_sha256': JVM_SHA,
        'constant_pool_indices_and_lengths_preserved': True,
        'method_bytecode_and_all_other_data_byte_identical': True,
        'only_emoji_hash_string_changed': True, 'asset_capacity_unchanged': True,
        'replacements': 1, 'emoji_sha256': emoji_hash,
    }

def temporary_package(old, path, updates):
    with zipfile.ZipFile(old) as source, zipfile.ZipFile(path, 'w') as out:
        for info in source.infolist(): out.writestr(copy.copy(info), updates.get(info.filename, source.read(info)))
    with zipfile.ZipFile(old) as source, zipfile.ZipFile(path) as after:
        require(source.namelist() == after.namelist(), 'Temporary MPP inventory changed')
        changed = {n for n in source.namelist() if source.read(n) != after.read(n)}
        require(changed == set(updates), 'Temporary MPP unrelated entries changed')

def main():
    parser = argparse.ArgumentParser()
    for name in ('single', 'bundle', 'emoji', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args.single, args.bundle, args.emoji, args.tools, args.work, args.output = (
        getattr(args, name).resolve() for name in ('single', 'bundle', 'emoji', 'tools', 'work', 'output'))
    require(not args.work.exists(), 'Fresh work directory required')
    args.work.mkdir(parents=True); args.output.mkdir(parents=True, exist_ok=True)
    emoji = args.emoji.read_bytes()
    require(0 < len(emoji) <= LIMIT, 'Emoji asset exceeds existing capacity')
    emoji_hash = sha(emoji)
    for kind in ('single', 'bundle'):
        path = getattr(args, kind)
        require(sha(path.read_bytes()) == BASE_PINS[kind], 'Baseline ' + kind + ' pin differs')
        with zipfile.ZipFile(path) as archive:
            require(sha(archive.read('swiftkey/emoji_ja.tsv')) == OLD_EMOJI, 'Baseline emoji asset differs')
            require(sha(archive.read(JVM)) == JVM_SHA, 'Baseline JVM differs')
    with zipfile.ZipFile(args.single) as archive: base_jvm = archive.read(JVM)
    jvm, jvm_report = patch_class(base_jvm, emoji_hash)
    (args.output / 'JapaneseSamsungEmojiPatch.class').write_bytes(jvm)
    classes = args.work / 'classes'; classes.mkdir()
    run(['java', 'com.sun.tools.javac.Main', '-cp', args.tools / 'morphe.jar', '-d', classes,
         ROOT / 'PatchEmojiLoader.java', ROOT / 'LoaderResourceProbe.java'], args.work / 'javac.log')
    dex_reports, resource_reports = [], []
    for kind, old in [('single', args.single), ('bundle', args.bundle)]:
        with zipfile.ZipFile(old) as archive: baseline = archive.read('classes.dex')
        baseline_path = args.work / (kind + '-baseline.dex'); baseline_path.write_bytes(baseline)
        output = args.output / (kind + '-classes.dex')
        report = args.output / (kind + '-dex-report.json')
        run(['java', '-cp', str(classes) + os.pathsep + str(args.tools / 'morphe.jar'), 'PatchEmojiLoader',
             baseline_path, output, emoji_hash, report], args.work / (kind + '-dex.log'))
        dex_report = json.loads(report.read_text())
        require(dex_report['status'] == 'PASSED', 'DEX audit failed'); dex_reports.append(dex_report)
        package = args.work / (kind + '-resources.mpp')
        temporary_package(old, package, {'classes.dex': output.read_bytes(), JVM: jvm, 'swiftkey/emoji_ja.tsv': emoji})
        for sdk in ('morphe.jar', 'morphe-current.jar'):
            result_path = args.output / (kind + '-' + sdk + '-resources.json')
            run([os.environ.get('MORPHE_JAVA', 'java'), '-Xmx1536m', '-cp', str(classes) + os.pathsep + str(args.tools / sdk),
                 'LoaderResourceProbe', package, result_path], args.work / (kind + '-' + sdk + '-resources.log'))
            resource_report = json.loads(result_path.read_text())
            require(resource_report['status'] == 'PASSED', 'Resource loader probe failed')
            resource_reports.append(resource_report)
    result = {
        'status': 'PASSED', 'unrelated_android_classes_preserved': True,
        'unrelated_jvm_classes_preserved': True, 'only_emoji_hash_string_changed': True,
        'single_android_sha256': dex_reports[0]['android_sha256'],
        'bundle_android_sha256': dex_reports[1]['android_sha256'],
        'jvm_sha256': sha(jvm), 'emoji_sha256': emoji_hash,
        'jvm_report': jvm_report, 'android_reports': dex_reports, 'resource_reports': resource_reports,
        'asset_capacity_unchanged': True, 'asset_capacity': LIMIT,
        'native_android_loader_executed': False, 'real_jvm_resource_loader_verified': True,
    }
    (args.output / 'loader-report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + '\n')
    print(json.dumps(result, ensure_ascii=False))

if __name__ == '__main__': main()
