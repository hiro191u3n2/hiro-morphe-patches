#!/usr/bin/env python3
"""Reproducibly build the approved ULike multiframe and dark-scene improvements on the exact active bundle."""
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
VERSION = '1.9.35'
BASE_VERSION = '1.9.34'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.35.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.34.mpp'
BASE_SINGLE_SHA256 = '28bd1782392fb2cc4d703a1a5a32c090eb17dfb2b7de52c41e11e4e2e884ed83'
SEED_NAME = 'stock-seed1933-capture.dex'
SEED_SHA256 = '6b0c2a6af686d764a1bb3bab8ab4405b760c07f7ffdcbb0d645d9b2a72eb902a'
TOOL_PINS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
JDK_RUNTIME_VERSION = '21.0.8+9-LTS'
JDK_VENDOR = 'Eclipse Adoptium'
HELPER_PREFIXES = ('FusionPixels1933', 'BurstCapture1933', 'CapturePolicy1933', 'FastPixels1933', 'FastResize1933')
TEMPLATE_CLASSES = ('ShotContext1932.class',)
ALLOWED_CHANGED = {'classes.dex', 'ulike/runtime.dex', 'ulike/methods.dex',
                   'ulike/methods.tsv', 'META-INF/MANIFEST.MF',
                   'app/hiro/ulike/patches/UlikeHqMaxPatch.class',
                   'app/hiro/ulike/patches/IntegrationPayload186.class'}
NATIVE_ENTRY = 'ulike1935/runtime/libulike_speed1935.so'
ALLOWED_ADDED = {NATIVE_ENTRY}


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


SELECTED = ['S2', 'S3', 'S4', 'S5', 'S6', 'S7', 'S8', 'S16', 'T1']


def bundle_name(qa):
    version = qa.get('bundle_version')
    require(isinstance(version, str) and re.fullmatch(r'\d+\.\d+\.\d+', version), 'Missing bundle version')
    return 'Hiro_Morphe_Patches_v' + version + '.mpp'


def config(path=None):
    value = json.loads(Path(path or ROOT.parent / 'manifest.json').read_text())
    require(value.get('ulike_version') == VERSION and value.get('baseline_ulike_version') == BASE_VERSION,
            'Wrong reviewed versions')
    require(value.get('baseline_ulike_sha256') == BASE_SINGLE_SHA256, 'ULike baseline mismatch')
    require(value.get('baseline_bundle_version') == '1.0.167' and value.get('bundle_version') == '1.0.168',
            'Exact reviewed integrated baseline required')
    require(value.get('baseline_bundle_sha256') == 'fa0a578e0ad34e21afc1ae8a40f4d0e13b986eff88474e2025b0decbf6555b7c',
            'Integrated baseline hash mismatch')
    require(value.get('toolchain_sha256') == TOOL_PINS, 'Toolchain pins differ')
    return value


def production_sources():
    names = json.loads((ROOT / 'production1935.json').read_text())
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
    classes, helpers, dex = [args.work / n for n in ('classes', 'helper-classes', 'helper-dex')]
    for path in (classes, helpers, dex):
        path.mkdir()
    # Dedupe compiler stubs by package path, with this release's expanded quality
    # declarations preferred over earlier minimum-only fixtures. Never ship stubs.
    stub_map = {}
    folders = sorted(p for p in ROOT.iterdir() if p.is_dir() and p.name.endswith('-stubs'))
    folders.sort(key=lambda p: (p.name in ('quality-stubs', 'quality1934-stubs'), p.name))
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
    transformer_names = ['MergePayloads', 'Hooks1935', 'Transform1935', 'VerifySpeed1935', 'VerifyHelperReferences']
    transformer_names += [p.stem for p in sorted(ROOT.glob('*Hooks1935.java')) if p.stem not in transformer_names]
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (n + '.java') for n in transformer_names]], args.work / 'transform-javac.log')
    run(['javac', '-encoding', 'UTF-8', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED',
         '-cp', cp, '-d', classes, ROOT / 'PatchClass1935.java', ROOT / 'PatchNativeLoader1935.java'], args.work / 'metadata-javac.log')
    native_library = args.native / 'libulike_speed1935.so'
    native_report_path = args.native / 'native-build1935.json'
    require(native_library.is_file() and native_report_path.is_file(), 'Reviewed native build and report required')
    native_report = json.loads(native_report_path.read_text())
    arm64_tests = json.loads((args.native / 'arm64-tests.json').read_text())
    require(arm64_tests.get('schema') == 'ulike-native1935-tests-v1' and arm64_tests.get('passed') is True
            and arm64_tests.get('neon') is True and type(arm64_tests.get('assertions')) is int
            and arm64_tests['assertions'] > 0, 'Executed ARM64 kernel comparison required')
    require(native_report.get('schema') == 'ulike-native1935-v1' and native_report.get('ndk_revision') == '27.2.12479018'
            and native_report.get('target') == 'aarch64-linux-android26' and native_report.get('neon_compiled') is True
            and native_report.get('fma_instructions') is False and native_report.get('needed_libraries') == [], 'Invalid exact native build contract')
    require(native_report.get('sha256') == sha(native_library.read_bytes())
            and native_report.get('bytes') == native_library.stat().st_size, 'Native build report mismatch')
    for source, digest in native_report['sources'].items():
        require(Path(source).name == source and sha((ROOT / 'native1935' / source).read_bytes()) == digest, 'Native source provenance mismatch')
    require(reviewed.get('native_library_sha256') == sha(native_library.read_bytes()), 'Reviewed native library differs')
    require(reviewed.get('native_library_bytes') == native_library.stat().st_size, 'Reviewed native library size differs')
    native_classes, native_dex = args.work / 'native-loader-classes', args.work / 'native-loader-dex'
    package = native_classes / 'app/hiro/ulike/patches'
    package.mkdir(parents=True)
    native_dex.mkdir()
    for path in sorted((baseline / 'app/hiro/ulike/patches').glob('IntegrationPayload186*.class')):
        shutil.copyfile(path, package / path.name)
    native_installer = 'app/hiro/ulike/patches/IntegrationPayload186.class'
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED', '-cp', cp,
         'PatchNativeLoader1935', baseline / native_installer, native_library, native_classes / native_installer],
        args.work / 'native-loader-verification.txt')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', args.tools / 'morphe.jar',
         '--output', native_dex, *sorted(package.glob('*.class'))], args.work / 'native-loader-d8.log')
    seed = ROOT / 'stock-seed1935.dex'
    seed_arg = seed if seed.is_file() else '-'
    if seed.is_file():
        require(reviewed.get('native_seed_sha256') == sha(seed.read_bytes()), 'Reviewed native seed differs')
        shutil.copyfile(seed, args.work / 'baseline-stock-seed.dex')
    for name in ('emitted', 'repeat'):
        run(['java', '-cp', cp, 'Transform1935', baseline, dex / 'classes.dex', bundle_dex, seed_arg,
             args.work / name, args.work / (name + '-audit.tsv'), native_dex / 'classes.dex'], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} == {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()},
            'Non-deterministic DEX transform')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '-cp', cp, 'PatchClass1935',
         baseline / 'app/hiro/ulike/patches/UlikeHqMaxPatch.class', emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    verification = run(['java', '-cp', cp, 'VerifySpeed1935', baseline, emitted], args.work / 'speed-dex-verification.txt')
    run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex',
         emitted / 'methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    spec = importlib.util.spec_from_file_location('quality_host1934', ROOT / 'host_speed1935.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = module.test(ROOT, args.work, args.tools / 'android.jar')
    require(result.get('status') == 'passed' and type(result.get('assertions')) is int and result['assertions'] > 0,
            'Host quality verification failed')
    (args.work / 'host-speed1935-result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    inventory = json.loads((emitted / 'speed-inventory.json').read_text())
    final = dict(base)
    final.update({'classes.dex': (emitted / 'loader.dex').read_bytes(), 'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(),
                  'ulike/methods.dex': (emitted / 'methods.dex').read_bytes(), 'ulike/methods.tsv': (emitted / 'methods.tsv').read_bytes(),
                  'app/hiro/ulike/patches/UlikeHqMaxPatch.class': (emitted / 'UlikeHqMaxPatch.class').read_bytes(),
                  native_installer: (native_classes / native_installer).read_bytes(), NATIVE_ENTRY: native_library.read_bytes()})
    mf = headers(base['META-INF/MANIFEST.MF'])
    mf.update(Version=VERSION, Timestamp='2026-10-07T08:00:00', Description='S2,S3,S4,S5,S6,S7,S8,S16 exact-pixel speedups and T1 save queue. Device untested.')
    final['META-INF/MANIFEST.MF'] = manifest(mf)
    integrated = dict(bundle)
    integrated.update({n: b for n, b in final.items() if own(n)})
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    mf = headers(bundle['META-INF/MANIFEST.MF'])
    mf.update(Version=reviewed['bundle_version'], Timestamp='2026-10-07T08:00:00', Description='ULike1.9.35 speed improvements and bounded save queue. Other apps retained from1.0.167. Device untested.')
    integrated['META-INF/MANIFEST.MF'] = manifest(mf)
    changes = sorted(n for n in final.keys() & base.keys() if final[n] != base[n])
    added = sorted(final.keys() - base.keys())
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
    qa = {'schema': 'ulike1935-speed-v1', 'status': 'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED',
          'ulike_version': VERSION, 'bundle_version': reviewed['bundle_version'], 'base_versions': [BASE_VERSION, reviewed['baseline_bundle_version']],
          'approved_lineage': '1.8.8', 'selected_candidates': SELECTED,
          'device_tested': False, 'device_quality_verified': False, 'original_apk_apply_tested': False, 'ci_android_apply_tested': False,
          'non_ulike_resources_byte_identical': True, 'non_ulike_loader_classes_unchanged': True,
          'camera_lifecycle_byte_identical': False, 'camera_startup_recovery_byte_identical': True, 'image_pipeline_byte_identical': False,
          'standalone_and_bundle_ulike_resources_identical': True,
          'native_methods_byte_identical': final['ulike/methods.dex'] == base['ulike/methods.dex'],
          'native_methods_tsv_byte_identical': final['ulike/methods.tsv'] == base['ulike/methods.tsv'],
          'host_quality_passed': True, 'host_quality_result': result, 'dex_verification': verification.strip(),
          'changed_standalone_entries': changes, 'added_standalone_entries': added,
          'changed_bundle_entries': sorted(n for n in integrated.keys() & bundle.keys() if integrated[n] != bundle[n]),
          'added_bundle_entries': sorted(integrated.keys() - bundle.keys()),
          'native_library_sha256': sha(native_library.read_bytes()), 'native_library_bytes': native_library.stat().st_size,
          'native_build_report': native_report, 'arm64_kernel_tests': arm64_tests,
          'artifacts': artifacts, 'input_sha256': pins, 'jdk_identity': jdk_identity,
          'published': False, 'manager_feed_updated': False,
          'limitations': ['No Android/Galaxy/ART or camera-hardware execution.', 'No device save latency or visual quality measurement.']}
    qa.update(inventory)
    (args.output / 'QA_ULike_v1.9.35.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    for name in ('emitted-audit.tsv', 'speed-dex-verification.txt', 'helper-references.txt', 'host-speed1935-result.json', 'native-loader-verification.txt'):
        shutil.copyfile(args.work / name, args.output / name)
    shutil.copyfile(emitted / 'speed-inventory.json', args.output / 'speed-inventory.json')
    shutil.copyfile(native_report_path, args.output / 'native-build1935.json')
    shutil.copyfile(args.native / 'arm64-tests.json', args.output / 'arm64-tests.json')
    print(json.dumps({'status': qa['status'], 'bundle_version': reviewed['bundle_version'], 'host_quality_result': result, 'artifacts': artifacts}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output', 'native'):
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
