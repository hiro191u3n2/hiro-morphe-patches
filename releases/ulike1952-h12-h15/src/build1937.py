#!/usr/bin/env python3
"""Reproducibly build narrowly scoped startup viewport layout fixes on exact ULike1936."""
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
VERSION = '1.9.37'
BASE_VERSION = '1.9.36'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.37.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.36.mpp'
BASE_SINGLE_SHA256 = '5cfb1648269ff03448fc2ab8990e073a43af49f83ffc6ad9a64fc0f86a38aba1'
TOOL_PINS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
JDK_RUNTIME_VERSION = '21.0.8+9-LTS'
JDK_VENDOR = 'Eclipse Adoptium'
ALLOWED_CHANGED = {'classes.dex', 'ulike/runtime.dex', 'ulike/methods.dex',
                   'ulike/methods.tsv', 'META-INF/MANIFEST.MF',
                   'app/hiro/ulike/patches/UlikeHqMaxPatch.class'}
NATIVE_ENTRY = 'ulike1935/runtime/libulike_speed1935.so'
ALLOWED_ADDED = set()


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


def verify_jdk():
    result = subprocess.run(['java', '-XshowSettings:properties', '-version'],
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, timeout=30)
    require(result.returncode == 0, 'Unable to read Java toolchain identity')
    properties = dict(re.findall(r'^\s*(java\.(?:runtime\.version|vendor|version)) = (.+)$',
                                 result.stdout, re.MULTILINE))
    require(properties.get('java.runtime.version') == JDK_RUNTIME_VERSION
            and properties.get('java.vendor') == JDK_VENDOR
            and properties.get('java.version') == '21.0.8', 'Unreviewed Java runtime')
    compiler = subprocess.run(['javac', '-version'], stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, text=True, timeout=30)
    require(compiler.returncode == 0 and compiler.stdout.strip() == 'javac 21.0.8',
            'Unreviewed Java compiler')
    return {'runtime_version': JDK_RUNTIME_VERSION, 'vendor': JDK_VENDOR,
            'javac_version': '21.0.8'}


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


SELECTED = ['preview_startup_layout']


def bundle_name(qa):
    version = qa.get('bundle_version')
    require(isinstance(version, str) and re.fullmatch(r'\d+\.\d+\.\d+', version), 'Missing bundle version')
    return 'Hiro_Morphe_Patches_v' + version + '.mpp'


def config(path=None):
    value = json.loads(Path(path or ROOT.parent / 'manifest.json').read_text())
    require(value.get('ulike_version') == VERSION and value.get('baseline_ulike_version') == BASE_VERSION,
            'Wrong reviewed versions')
    require(value.get('baseline_ulike_sha256') == BASE_SINGLE_SHA256, 'ULike baseline mismatch')
    require(value.get('baseline_bundle_version') == '1.0.169' and value.get('bundle_version') == '1.0.170',
            'Exact reviewed integrated baseline required')
    require(value.get('baseline_bundle_sha256') == 'd2c8c4bef3b400ac117e6a794751a1140305bca75c510bf4368f74391ed8e8aa',
            'Integrated baseline hash mismatch')
    require(value.get('toolchain_sha256') == TOOL_PINS, 'Toolchain pins differ')
    return value


def production_sources():
    names = json.loads((ROOT / 'production1937.json').read_text())
    require(isinstance(names, list) and names and len(names) == len(set(names)), 'Production inventory')
    require(all(re.fullmatch(r'[A-Za-z][A-Za-z0-9]*', n) for n in names), 'Unsafe production name')
    paths = [ROOT / (n + '.java') for n in names]
    require(all(p.is_file() for p in paths), 'Missing production source')
    return names, paths


def build(args):
    reviewed = config(args.manifest)
    jdk_identity = verify_jdk()
    combined_name = bundle_name(reviewed)
    previous_bundle = 'Hiro_Morphe_Patches_v' + reviewed['baseline_bundle_version'] + '.mpp'
    pins = {BASE_SINGLE: BASE_SINGLE_SHA256, previous_bundle: reviewed['baseline_bundle_sha256'], **TOOL_PINS}
    require(not args.work.exists(), 'Use fresh build work directory')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    for name, digest in pins.items():
        parent = args.tools if name.endswith('.jar') else args.input
        require(sha((parent / name).read_bytes()) == digest, 'Pinned input differs: ' + name)
    base = archive(args.input / BASE_SINGLE)
    bundle = archive(args.input / previous_bundle)
    require(headers(base['META-INF/MANIFEST.MF'])['Version'] == BASE_VERSION, 'Wrong standalone version')
    require(headers(bundle['META-INF/MANIFEST.MF'])['Version'] == reviewed['baseline_bundle_version'], 'Wrong bundle version')
    require({n: b for n, b in base.items() if own(n)} == {n: b for n, b in bundle.items() if own(n)}, 'ULike baseline not synchronized')
    baseline = args.work / 'baseline'
    for name, data in base.items():
        target = baseline / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    bundle_dex = args.work / 'bundle-baseline.dex'
    bundle_dex.write_bytes(bundle['classes.dex'])
    (baseline / 'bundle.dex').write_bytes(bundle['classes.dex'])
    names, production = production_sources()
    compiled_source_sha256 = {path.name: sha(path.read_bytes()) for path in production}
    classes, helpers, dex = [args.work / n for n in ('classes', 'helper-classes', 'helper-dex')]
    for path in (classes, helpers, dex):
        path.mkdir()
    # Compile only the reviewed production families; native/API stubs are compile-only
    # and are excluded from the selected D8 inputs.
    stub_map = {}
    folders = [ROOT / 'layout1937-stubs', ROOT / 'geometry1937-compile']
    require(all(p.is_dir() for p in folders), 'Missing reviewed compile stubs')
    for folder in folders:
        for p in sorted(folder.rglob('*.java')):
            stub_map[str(p.relative_to(folder))] = p
    for name in names:
        stub_map.pop('com/hiro/ulike/' + name + '.java', None)
    stubs = list(stub_map.values())
    run(['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-bootclasspath', args.tools / 'android.jar',
         '-d', helpers, *stubs, *production], args.work / 'helper-javac.log')
    selected = sorted(p for p in (helpers / 'com/hiro/ulike').glob('*.class')
                      if any(p.name == n + '.class' or p.name.startswith(n + '$') for n in names))
    require(all(helpers / 'com/hiro/ulike' / (n + '.class') in selected for n in names), 'Missing compiled root')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', helpers, '--output', dex, *selected], args.work / 'd8.log')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    transformer_names = ['MergePayloads', 'Hooks1937', 'Transform1937', 'VerifyLayout1937', 'VerifyHelperReferences']
    transformer_names += [p.stem for p in sorted(ROOT.glob('*Hooks1937.java')) if p.stem not in transformer_names]
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (n + '.java') for n in transformer_names]], args.work / 'transform-javac.log')
    run(['javac', '-encoding', 'UTF-8', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED',
         '-cp', cp, '-d', classes, ROOT / 'PatchClass1937.java'], args.work / 'metadata-javac.log')
    native_library = baseline / NATIVE_ENTRY
    require(sha(native_library.read_bytes()) == reviewed['native_library_sha256']
            and native_library.stat().st_size == reviewed['native_library_bytes'], 'Pinned inherited native library differs')
    native_installer = 'app/hiro/ulike/patches/IntegrationPayload186.class'
    native_preservation = {'sha256': sha(native_library.read_bytes()), 'bytes': native_library.stat().st_size,
                           'installer_sha256': sha(base[native_installer]), 'byte_identical_to_1936': True}
    (args.work / 'native-preservation1937.json').write_text(json.dumps(native_preservation, indent=2) + '\n')
    seed = ROOT / 'stock-seed1937.dex'
    seed_arg = seed if seed.is_file() else '-'
    if seed.is_file():
        require(reviewed.get('native_seed_sha256') == sha(seed.read_bytes()), 'Reviewed native seed differs')
        shutil.copyfile(seed, args.work / 'baseline-stock-seed.dex')
    for name in ('emitted', 'repeat'):
        run(['java', '-cp', cp, 'Transform1937', baseline, dex / 'classes.dex', bundle_dex, seed_arg,
             args.work / name, args.work / (name + '-audit.tsv')], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} == {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()},
            'Non-deterministic DEX transform')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '-cp', cp, 'PatchClass1937',
         baseline / 'app/hiro/ulike/patches/UlikeHqMaxPatch.class', emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    verification = run(['java', '-cp', cp, 'VerifyLayout1937', baseline, emitted], args.work / 'layout-dex-verification.txt')
    references = run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex',
         emitted / 'methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    require(references.startswith('PASS helper references in ') and len(references.strip()) > 25,
            'Executed helper-reference verification output is missing')
    spec = importlib.util.spec_from_file_location('layout_host1937', ROOT / 'host_regression1937.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = module.test(ROOT, args.work, args.tools / 'android.jar')
    require(result.get('status') == 'passed' and type(result.get('assertions')) is int and result['assertions'] > 0,
            'Host quality verification failed')
    (args.work / 'host-regression1937-result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    require(compiled_source_sha256 == {path.name: sha(path.read_bytes()) for path in production},
            'Production source changed during compilation or host verification')
    inventory = json.loads((emitted / 'layout-inventory.json').read_text())
    final = dict(base)
    final.update({'classes.dex': (emitted / 'loader.dex').read_bytes(), 'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(),
                  'ulike/methods.dex': (emitted / 'methods.dex').read_bytes(), 'ulike/methods.tsv': (emitted / 'methods.tsv').read_bytes(),
                  'app/hiro/ulike/patches/UlikeHqMaxPatch.class': (emitted / 'UlikeHqMaxPatch.class').read_bytes()})
    mf = headers(base['META-INF/MANIFEST.MF'])
    mf.update(Version=VERSION, Timestamp='2026-10-07T12:00:00', Description='Startup viewport layout fix. Front preview, lens UI, quality and save speed retained. Device untested.')
    final['META-INF/MANIFEST.MF'] = manifest(mf)
    integrated = dict(bundle)
    integrated.update({n: b for n, b in final.items() if own(n)})
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    mf = headers(bundle['META-INF/MANIFEST.MF'])
    mf.update(Version=reviewed['bundle_version'], Timestamp='2026-10-07T12:00:00', Description='ULike1.9.37 startup viewport layout fix. Other apps retained from1.0.169. Device untested.')
    integrated['META-INF/MANIFEST.MF'] = manifest(mf)
    changes = sorted(n for n in final.keys() & base.keys() if final[n] != base[n])
    added = sorted(final.keys() - base.keys())
    require(final[native_installer] == base[native_installer] and final[NATIVE_ENTRY] == base[NATIVE_ENTRY], 'Inherited native installer or kernel changed')
    require(set(final) - set(base) == ALLOWED_ADDED and set(base) <= set(final) and set(changes) <= ALLOWED_CHANGED, 'Unexpected standalone entries')
    require(set(integrated) - set(bundle) == ALLOWED_ADDED and set(bundle) <= set(integrated), 'Unexpected integrated entries')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')),
            'Other app resources changed')
    artifacts = {}
    for name, entries in ((SINGLE, final), (combined_name, integrated)):
        path = args.output / name
        write_zip(path, entries)
        require(archive(path) == entries, 'Output archive differs')
        run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', path], args.work / (name + '.loader.log'))
        artifacts[name] = {'sha256': sha(path.read_bytes()), 'bytes': path.stat().st_size}
    qa = {'schema': 'ulike1937-layout-v1', 'status': 'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED',
          'ulike_version': VERSION, 'bundle_version': reviewed['bundle_version'], 'base_versions': [BASE_VERSION, reviewed['baseline_bundle_version']],
          'approved_lineage': '1.8.8', 'selected_candidates': SELECTED,
          'device_tested': False, 'device_quality_verified': False, 'original_apk_apply_tested': False, 'ci_android_apply_tested': False,
          'non_ulike_resources_byte_identical': True, 'non_ulike_loader_classes_unchanged': True,
          'camera_lifecycle_byte_identical': True, 'camera_startup_recovery_byte_identical': True, 'front_lens_fixes_byte_identical': True, 'viewport_layout_byte_identical': False, 'image_pipeline_byte_identical': True,
          'standalone_and_bundle_ulike_resources_identical': True,
          'native_methods_byte_identical': final['ulike/methods.dex'] == base['ulike/methods.dex'],
          'native_methods_tsv_byte_identical': final['ulike/methods.tsv'] == base['ulike/methods.tsv'],
          'host_quality_passed': True, 'host_quality_result': result,
          'production_source_consistency_verified': True, 'compiled_production_source_sha256': compiled_source_sha256, 'dex_verification': verification.strip(),
          'changed_standalone_entries': changes, 'added_standalone_entries': added,
          'changed_bundle_entries': sorted(n for n in integrated.keys() & bundle.keys() if integrated[n] != bundle[n]),
          'added_bundle_entries': sorted(integrated.keys() - bundle.keys()),
          'native_library_sha256': sha(native_library.read_bytes()), 'native_library_bytes': native_library.stat().st_size,
          'native_library_byte_identical': True, 'native_installer_byte_identical': True,
          'native_preservation': native_preservation, 'speed_and_save_helpers_byte_identical': True,
          'artifacts': artifacts, 'input_sha256': pins, 'jdk_identity': jdk_identity,
          'published': False, 'manager_feed_updated': False,
          'limitations': ['No Android/Galaxy/ART or camera-hardware execution.', 'No device save latency or visual quality measurement.']}
    qa.update(inventory)
    (args.output / 'QA_ULike_v1.9.37.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    for name in ('emitted-audit.tsv', 'layout-dex-verification.txt', 'helper-references.txt', 'host-regression1937-result.json', 'native-preservation1937.json'):
        shutil.copyfile(args.work / name, args.output / name)
    shutil.copyfile(emitted / 'layout-inventory.json', args.output / 'layout-inventory.json')
    for pattern in ('host-layout*.txt', 'host-geometry*.txt', 'host-regression*.json'):
        for path in args.work.glob(pattern):
            shutil.copyfile(path, args.output / path.name)
    print(json.dumps({'status': qa['status'], 'bundle_version': reviewed['bundle_version'], 'host_quality_result': result, 'artifacts': artifacts}, ensure_ascii=False, indent=2))


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
