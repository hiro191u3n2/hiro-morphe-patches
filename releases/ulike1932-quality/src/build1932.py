#!/usr/bin/env python3
"""Reproducibly build the six approved ULike quality improvements on the exact active bundle."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION = '1.9.32'
BASE_VERSION = '1.9.31'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.32.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.31.mpp'
BASE_SINGLE_SHA256 = '646a4e78e1905c2172d50205c4058a16d0ef76d9bbfcbd8b888f7dc34cee678e'
SEED_NAME = 'stock-seed1932-watermark.dex'
SEED_SHA256 = '92cc011c4b719d1de4ed3fee74399315b3c543b3f5cfa2f136adf894022af5a9'
TOOL_PINS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
HELPER_PREFIXES = ('QualityPipeline1932', 'QualityShadow1932', 'QualityPixels1932', 'ShotContext1932')
TEMPLATE_CLASSES = ('PhotoDetail.class', 'ChromaPipeline177.class')
ALLOWED_CHANGED = {'classes.dex', 'ulike/runtime.dex', 'ulike/methods.dex',
                   'ulike/methods.tsv', 'META-INF/MANIFEST.MF',
                   'app/hiro/ulike/patches/UlikeHqMaxPatch.class'}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run(command, log):
    command = list(map(str, command))
    if command[0] == 'java':
        command.insert(1, '-XX:ActiveProcessorCount=1')
    with Path(log).open('wb') as stream:
        result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT, timeout=900)
    output = Path(log).read_text(errors='replace')
    if result.returncode:
        print(output[-12000:])
        raise RuntimeError('Command failed: ' + command[0] + '; log=' + str(log))
    return output


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt ZIP')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP entries')
        for name in z.namelist():
            require(not Path(name).is_absolute() and '..' not in Path(name).parts and '\\' not in name,
                    'Unsafe ZIP entry')
        return {name: z.read(name) for name in z.namelist() if not name.endswith('/')}


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda n: (n != 'META-INF/MANIFEST.MF', n)):
            info = zipfile.ZipInfo(name, (2026, 10, 7, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Manifest continuation without header')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs), 'Malformed manifest')
    require(len(pairs) == len(dict(pairs)), 'Duplicate manifest header')
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest')
        raw = (key + ': ' + value).encode()
        prefix = b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    data = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(data) == fields, 'Manifest roundtrip')
    return data


def own(name):
    return name.startswith('app/hiro/ulike/patches/') or re.match(r'^ulike(?:\d+)?/', name) is not None


def config(path=None):
    path = Path(path) if path is not None else ROOT.parent / 'manifest.json'
    value = json.loads(path.read_text())
    require(value.get('ulike_version') == VERSION and value.get('baseline_ulike_version') == BASE_VERSION,
            'Wrong ULike build versions')
    require(value.get('baseline_ulike_sha256') == BASE_SINGLE_SHA256, 'Approved ULike baseline differs')
    old_version, new_version = value.get('baseline_bundle_version'), value.get('bundle_version')
    require(isinstance(old_version, str) and re.fullmatch(r'\d+\.\d+\.\d+', old_version), 'Missing baseline bundle version')
    require(isinstance(new_version, str) and re.fullmatch(r'\d+\.\d+\.\d+', new_version), 'Missing new bundle version')
    old_parts, new_parts = tuple(map(int, old_version.split('.'))), tuple(map(int, new_version.split('.')))
    require(old_parts >= (1, 0, 164) and new_parts[:2] == old_parts[:2] and new_parts[2] == old_parts[2] + 1,
            'Bundle must advance once from exact reviewed baseline')
    digest = value.get('baseline_bundle_sha256')
    require(isinstance(digest, str) and re.fullmatch(r'[0-9a-f]{64}', digest), 'Missing bundle baseline checksum')
    require(value.get('toolchain_sha256') == TOOL_PINS, 'Reviewed toolchain differs')
    return value


def bundle_name(qa):
    version = qa.get('bundle_version')
    require(isinstance(version, str) and re.fullmatch(r'\d+\.\d+\.\d+', version), 'Missing bundle version')
    return 'Hiro_Morphe_Patches_v' + version + '.mpp'


def build(args):
    reviewed = config(args.manifest)
    previous_bundle = 'Hiro_Morphe_Patches_v' + reviewed['baseline_bundle_version'] + '.mpp'
    combined_name = bundle_name(reviewed)
    pins = {BASE_SINGLE: BASE_SINGLE_SHA256, previous_bundle: reviewed['baseline_bundle_sha256'], **TOOL_PINS}
    require(not args.work.exists(), 'Use a fresh work directory')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    for name, expected in pins.items():
        parent = args.tools if name.endswith('.jar') else args.input
        require(sha((parent / name).read_bytes()) == expected, 'Pinned input mismatch: ' + name)
    require(sha((ROOT / SEED_NAME).read_bytes()) == SEED_SHA256, 'Original watermark propagation seed differs')
    base = archive(args.input / BASE_SINGLE)
    bundle = archive(args.input / previous_bundle)
    require(headers(base['META-INF/MANIFEST.MF'])['Version'] == BASE_VERSION, 'ULike input version differs')
    require(headers(bundle['META-INF/MANIFEST.MF'])['Version'] == reviewed['baseline_bundle_version'], 'Bundle input version differs')
    require({n: b for n, b in base.items() if own(n)} == {n: b for n, b in bundle.items() if own(n)},
            'Current bundle no longer contains the approved ULike baseline')
    baseline = args.work / 'baseline'
    for name, data in base.items():
        target = baseline / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    bundle_dex = args.work / 'bundle-baseline.dex'
    bundle_dex.write_bytes(bundle['classes.dex'])
    (baseline / 'bundle.dex').write_bytes(bundle['classes.dex'])
    classes, helper_classes, helper_dex = [args.work / n for n in ('classes', 'helper-classes', 'helper-dex')]
    for folder in (classes, helper_classes, helper_dex):
        folder.mkdir()
    stubs = sorted((ROOT / 'quality-stubs').rglob('*.java'))
    require(stubs, 'Missing compile-time quality stubs')
    production = [ROOT / (name + '.java') for name in HELPER_PREFIXES]
    require(all(p.is_file() for p in production), 'Missing production helper source')
    run(['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-bootclasspath', args.tools / 'android.jar',
         '-d', helper_classes, *stubs, *production], args.work / 'helper-javac.log')
    package = helper_classes / 'com/hiro/ulike'
    selected = sorted(p for p in package.glob('*.class')
                      if p.name in TEMPLATE_CLASSES or any(p.name == prefix + '.class' or p.name.startswith(prefix + '$')
                                                           for prefix in HELPER_PREFIXES))
    require(all((package / (prefix + '.class')) in selected for prefix in HELPER_PREFIXES), 'Missing production helper classes')
    require(all((package / name) in selected for name in TEMPLATE_CLASSES), 'Missing reviewed alias template classes')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', helper_classes,
         '--output', helper_dex, *selected], args.work / 'd8.log')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (name + '.java') for name in ('MergePayloads', 'Transform1932', 'VerifyQuality1932', 'VerifyHelperReferences')]],
        args.work / 'transform-javac.log')
    run(['javac', '-encoding', 'UTF-8', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '-cp', cp, '-d', classes, ROOT / 'PatchClass1932.java'], args.work / 'metadata-javac.log')
    for name in ('emitted', 'repeat'):
        run(['java', '-cp', cp, 'Transform1932', baseline, helper_dex / 'classes.dex',
             bundle_dex, ROOT / SEED_NAME, args.work / name, args.work / (name + '-audit.tsv')], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} ==
            {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()}, 'DEX build not deterministic')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '-cp', cp,
         'PatchClass1932', baseline / 'app/hiro/ulike/patches/UlikeHqMaxPatch.class',
         emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    verification = run(['java', '-cp', cp, 'VerifyQuality1932', baseline, emitted], args.work / 'quality-dex-verification.txt')
    run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex',
         emitted / 'methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    host_path = ROOT / 'host_quality1932.py'
    require(host_path.is_file(), 'Quality host regression runner is required')
    spec = importlib.util.spec_from_file_location('ulike_quality_host', host_path)
    host = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(host)
    host_result = host.test(ROOT, args.work)
    require(type(host_result) is int and host_result > 0 or isinstance(host_result, dict) and bool(host_result),
            'Missing successful host regression evidence')
    (args.work / 'host-quality1932-result.json').write_text(json.dumps(host_result, ensure_ascii=False, indent=2) + '\n')
    inventory = json.loads((emitted / 'quality-inventory.json').read_text())
    for key in ('changed_runtime_methods', 'changed_native_methods', 'new_helper_classes'):
        require(isinstance(inventory.get(key), list) and all(isinstance(x, str) for x in inventory[key]),
                'Missing exact DEX inventory: ' + key)
    final = dict(base)
    final.update({'classes.dex': (emitted / 'loader.dex').read_bytes(),
                  'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(),
                  'ulike/methods.dex': (emitted / 'methods.dex').read_bytes(),
                  'ulike/methods.tsv': (emitted / 'methods.tsv').read_bytes(),
                  'app/hiro/ulike/patches/UlikeHqMaxPatch.class': (emitted / 'UlikeHqMaxPatch.class').read_bytes()})
    mf = headers(base['META-INF/MANIFEST.MF'])
    mf.update(Version=VERSION, Timestamp='2026-10-07T00:00:00',
              Description='Quality candidates1,2,8,12,15,20: processing order, resampling, condition-aware denoise, coordinated smoothing, adaptive sharp and periodic chroma detection. Camera lifecycle retained. Device untested.')
    final['META-INF/MANIFEST.MF'] = manifest(mf)
    integrated = dict(bundle)
    integrated.update({n: b for n, b in final.items() if own(n)})
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    mf = headers(bundle['META-INF/MANIFEST.MF'])
    mf.update(Version=reviewed['bundle_version'], Timestamp='2026-10-07T00:00:00',
              Description='ULike1.9.32 six requested image-quality improvements. All other patches retained from' +
                          reviewed['baseline_bundle_version'] + '. Camera lifecycle retained. Device untested.')
    integrated['META-INF/MANIFEST.MF'] = manifest(mf)
    changes = sorted(n for n in final if final[n] != base[n])
    require(set(final) == set(base), 'Unexpected standalone resource entries')
    require({'ulike/runtime.dex', 'META-INF/MANIFEST.MF'} <= set(changes) <= ALLOWED_CHANGED, 'Unexpected standalone resource changes')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')),
            'Non-ULike resource changed')
    require(set(integrated) == set(bundle), 'Unexpected integrated resource entries')
    artifacts = {}
    for name, entries in ((SINGLE, final), (combined_name, integrated)):
        path = args.output / name
        write_zip(path, entries)
        repeat = args.work / (name + '.repeat')
        write_zip(repeat, entries)
        require(path.read_bytes() == repeat.read_bytes(), 'MPP not deterministic')
        require(archive(path) == entries, 'Emitted ZIP mismatch')
        run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', path], args.work / (name + '.loader.log'))
        artifacts[name] = {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size}
    qa = {
        'schema': 'ulike1932-quality-v1', 'status': 'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED',
        'ulike_version': VERSION, 'bundle_version': reviewed['bundle_version'],
        'base_versions': [BASE_VERSION, reviewed['baseline_bundle_version']], 'approved_lineage': '1.8.8',
        'selected_candidates': [1, 2, 8, 12, 15, 20],
        'device_tested': False, 'device_quality_verified': False,
        'original_apk_apply_tested': False, 'ci_android_apply_tested': False,
        'non_ulike_resources_byte_identical': True, 'non_ulike_loader_classes_unchanged': True,
        'camera_lifecycle_byte_identical': True, 'image_pipeline_byte_identical': False,
        'standalone_and_bundle_ulike_resources_identical': True,
        'native_methods_byte_identical': final['ulike/methods.dex'] == base['ulike/methods.dex'],
        'native_methods_tsv_byte_identical': final['ulike/methods.tsv'] == base['ulike/methods.tsv'],
        'changed_runtime_methods': inventory['changed_runtime_methods'],
        'changed_native_methods': inventory['changed_native_methods'],
        'new_helper_classes': inventory['new_helper_classes'],
        'host_quality_passed': True, 'host_quality_result': host_result,
        'dex_verification': verification.strip(), 'changed_standalone_entries': changes,
        'changed_bundle_entries': sorted(n for n in integrated if integrated[n] != bundle[n]),
        'artifacts': artifacts, 'input_sha256': pins,
        'native_seed_sha256': SEED_SHA256,
        'published': False, 'manager_feed_updated': False,
        'limitations': ['No Android/Galaxy/ART or camera hardware execution.',
                        'Host tests and DEX checks do not measure device image quality or save latency.'],
    }
    (args.output / 'QA_ULike_v1.9.32.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    for name in ('emitted-audit.tsv', 'quality-dex-verification.txt', 'helper-references.txt', 'host-quality1932-result.json'):
        shutil.copyfile(args.work / name, args.output / name)
    for pattern in ('host-quality*.txt', 'host-quality*-metrics.json'):
        for path in args.work.glob(pattern):
            shutil.copyfile(path, args.output / path.name)
    shutil.copyfile(emitted / 'quality-inventory.json', args.output / 'quality-inventory.json')
    print(json.dumps({'status': qa['status'], 'bundle_version': reviewed['bundle_version'],
                      'host_quality_result': host_result, 'artifacts': artifacts}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    parser.add_argument('--jdk', type=Path)
    options = parser.parse_args()
    if options.jdk:
        os.environ['PATH'] = str(options.jdk.resolve() / 'bin') + os.pathsep + os.environ.get('PATH', '')
    for key, value in vars(options).items():
        if value is not None:
            setattr(options, key, value.resolve())
    build(options)
