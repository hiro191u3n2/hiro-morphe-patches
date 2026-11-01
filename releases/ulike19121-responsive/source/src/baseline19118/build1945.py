#!/usr/bin/env python3
"""Reproducibly preserve the quality-preserving save-speed candidates on pinned ULike1943."""
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
VERSION = '1.9.45'
BASE_VERSION = '1.9.44'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.45.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.44.mpp'
BASE_SINGLE_SHA256 = 'ee7a9810219c7851304034cf0b1fef33bba6f9a20f71501aa6330617a3138236'
TOOL_PINS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
JDK_RUNTIME_VERSION = '21.0.8+9-LTS'
JDK_VENDOR = 'Eclipse Adoptium'
ALLOWED_CHANGED = {'classes.dex', 'ulike/runtime.dex', 'META-INF/MANIFEST.MF',
                   'app/hiro/ulike/patches/UlikeHqMaxPatch.class', 'app/hiro/ulike/patches/IntegrationPayload186.class', 'ulike1935/runtime/libulike_speed1935.so', 'ulike/methods.dex', 'ulike/methods.tsv'}
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
            info = zipfile.ZipInfo(name, (2026, 10, 8, 0, 0, 0))
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


SELECTED = ['P10','P7','P4','P20','P29','P32','P33']


def bundle_name(qa):
    version = qa.get('bundle_version')
    require(isinstance(version, str) and re.fullmatch(r'\d+\.\d+\.\d+', version), 'Missing bundle version')
    return 'Hiro_Morphe_Patches_v' + version + '.mpp'


def config(path=None):
    value = json.loads(Path(path or ROOT.parent / 'manifest.json').read_text())
    require(value.get('ulike_version') == VERSION and value.get('baseline_ulike_version') == BASE_VERSION,
            'Wrong reviewed versions')
    require(value.get('baseline_ulike_sha256') == BASE_SINGLE_SHA256, 'ULike baseline mismatch')
    require(value.get('baseline_bundle_version') == '1.0.177' and value.get('bundle_version') == '1.0.178',
            'Exact reviewed integrated baseline required')
    require(value.get('baseline_bundle_sha256') == 'a4549256d59add165e0962333e8819647207ede0ee09e70ef3e3a8cd21fa4d28',
            'Integrated baseline hash mismatch')
    require(value.get('toolchain_sha256') == TOOL_PINS, 'Toolchain pins differ')
    return value


def production_sources():
    names = json.loads((ROOT / 'production1945.json').read_text())
    require(isinstance(names, list) and names and len(names) == len(set(names)), 'Exact production inventory')
    require(names == sorted(config()['production_helper_roots']), 'Only exact reviewed speed production families are allowed')
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
    folders = [ROOT / 'front1936-stubs', ROOT / 'renderer1938-stubs', ROOT / 'capture-stubs', ROOT / 'fast-stubs', ROOT / 'metadata-stubs', ROOT / 'quality-stubs']
    require(all(p.is_dir() for p in folders), 'Missing reviewed compile stubs')
    for folder in folders:
        for p in sorted(folder.rglob('*.java')):
            stub_map[str(p.relative_to(folder))] = p
    for p in sorted((ROOT / 'quality-dependencies').rglob('*.java')):
        stub_map[str(p.relative_to(ROOT / 'quality-dependencies'))] = p
    for retained_quality in ('QualityPipeline1932', 'QualityPixels1932', 'QualityShadow1932'):
        if retained_quality not in names:
            stub_map['com/hiro/ulike/' + retained_quality + '.java'] = ROOT / (retained_quality + '.java')
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
    transformer_names = ['MergePayloads', 'Hooks1945', 'Transform1945', 'Verify1945', 'VerifyHelperReferences']
    transformer_names += [p.stem for p in sorted(ROOT.glob('*Hooks1945.java')) if p.stem not in transformer_names]
    transformer_sources = [ROOT / (n + '.java') for n in transformer_names] + [ROOT / 'PatchClass1945.java', ROOT / 'PatchNativeLoader1945.java']
    transformer_source_sha256 = {p.name: sha(p.read_bytes()) for p in transformer_sources}
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes,
         *[ROOT / (n + '.java') for n in transformer_names]], args.work / 'transform-javac.log')
    run(['javac', '-encoding', 'UTF-8', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
         '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED',
         '-cp', cp, '-d', classes, ROOT / 'PatchClass1945.java', ROOT / 'PatchNativeLoader1945.java'], args.work / 'metadata-javac.log')
    baseline_native = baseline / NATIVE_ENTRY
    require(sha(baseline_native.read_bytes()) == reviewed['baseline_native_library_sha256']
            and baseline_native.stat().st_size == reviewed['baseline_native_library_bytes'], 'Pinned inherited native library differs')
    native_library = args.native_library
    require(native_library.is_file(), 'New arm64 kernel required')
    native_digest, native_bytes = sha(native_library.read_bytes()), native_library.stat().st_size
    require(native_library.read_bytes()[:4] == b'\x7fELF', 'New native payload is not ELF')
    if reviewed.get('native_library_sha256'):
        require(native_digest == reviewed['native_library_sha256'] and native_bytes == reviewed['native_library_bytes'], 'Reviewed new native library differs')
    native_report_path = native_library.parent / 'native-build1944.json'
    native_report = json.loads(native_report_path.read_text())
    require(native_report.get('sha256') == native_digest and native_report.get('bytes') == native_bytes and native_report.get('retained_1935_abi') is True and native_report.get('ndk_revision') == reviewed['ndk_revision'], 'Native compile evidence differs')
    native_source_sha256 = {name: sha((ROOT / 'native1944' / name).read_bytes()) for name in native_report['sources']}
    require(native_source_sha256 == native_report['sources'], 'Native sources changed after native build')
    native_installer = 'app/hiro/ulike/patches/IntegrationPayload186.class'
    native_preservation = {'baseline_sha256': sha(baseline_native.read_bytes()),
                           'baseline_bytes': baseline_native.stat().st_size,
                           'sha256': native_digest, 'bytes': native_bytes,
                           'baseline_installer_sha256': sha(base[native_installer]),
                           'byte_identical_to_1944': False,
                           'inherited_1935_jni_api_preserved': native_report['retained_1935_abi']}
    native_properties = ['-Dulike.native.sha=' + native_digest, '-Dulike.native.bytes=' + str(native_bytes)]
    (args.work / 'native-update1945.json').write_text(json.dumps(native_preservation, indent=2) + '\n')
    seed = ROOT / 'stock-seed1944.dex'
    seed_arg = seed if seed.is_file() else '-'
    if seed.is_file():
        require(reviewed.get('native_seed_sha256') == sha(seed.read_bytes()), 'Reviewed native seed differs')
        shutil.copyfile(seed, args.work / 'baseline-stock-seed.dex')
    for name in ('emitted', 'repeat'):
        run(['java', *native_properties, '-Dulike.production=' + ','.join(names), '-cp', cp, 'Transform1945', baseline, dex / 'classes.dex', bundle_dex, seed_arg,
             args.work / name, args.work / (name + '-audit.tsv')], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} == {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()},
            'Non-deterministic DEX transform')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '-cp', cp, 'PatchClass1945',
         baseline / 'app/hiro/ulike/patches/UlikeHqMaxPatch.class', emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    run(['java', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED', '-cp', cp, 'PatchNativeLoader1945',
         baseline / native_installer, native_library, emitted / 'IntegrationPayload186.class'], args.work / 'native-installer-metadata.log')
    verification = run(['java', *native_properties, '-Dulike.production=' + ','.join(names), '-cp', cp, 'Verify1945', baseline, emitted], args.work / 'speed-dex-verification.txt')
    references = run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex',
         emitted / 'methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    require(references.startswith('PASS helper references in ') and len(references.strip()) > 25,
            'Executed helper-reference verification output is missing')
    spec = importlib.util.spec_from_file_location('speed_host1944', ROOT / 'host_regression1945.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = module.test(ROOT, args.work, args.tools / 'android.jar')
    require(result.get('status') == 'passed' and type(result.get('assertions')) is int and result['assertions'] > 0,
            'Host speed/quality-preservation verification failed')
    require(set(result.get('suites', {})) >= {'capture_timing', 'noise_equivalence', 'native_equivalence', 'save_concurrency', 'scheduling', 'policy_cache', 'reflection_cache', 'arm64_simd'},
            'Host regression must execute capture, noise/native equivalence, save concurrency and scheduling suites')
    (args.work / 'host-regression1945-result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    require(compiled_source_sha256 == {path.name: sha(path.read_bytes()) for path in production},
            'Production source changed during compilation or host verification')
    require(native_source_sha256 == {name: sha((ROOT / 'native1944' / name).read_bytes()) for name in native_source_sha256}, 'Native source changed during host verification')
    require(transformer_source_sha256 == {p.name: sha(p.read_bytes()) for p in transformer_sources}, 'DEX transformer source changed during build/host verification')
    inventory = json.loads((emitted / 'speed-inventory.json').read_text())
    final = dict(base)
    final.update({'classes.dex': (emitted / 'loader.dex').read_bytes(), 'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(),
                  'ulike/methods.dex': (emitted / 'methods.dex').read_bytes(), 'ulike/methods.tsv': (emitted / 'methods.tsv').read_bytes(),
                  'app/hiro/ulike/patches/UlikeHqMaxPatch.class': (emitted / 'UlikeHqMaxPatch.class').read_bytes(),
                  native_installer: (emitted / 'IntegrationPayload186.class').read_bytes(), NATIVE_ENTRY: native_library.read_bytes()})
    mf = headers(base['META-INF/MANIFEST.MF'])
    mf.update(Version=VERSION, Timestamp='2026-10-08T01:00:00', Description='Quality-preserving noise caches, arm64 residual filtering, bounded scheduling and overlapped save processing. First capture, image policies and encoders preserved; device speed/quality unmeasured.')
    final['META-INF/MANIFEST.MF'] = manifest(mf)
    integrated = dict(bundle)
    integrated.update({n: b for n, b in final.items() if own(n)})
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    mf = headers(bundle['META-INF/MANIFEST.MF'])
    mf.update(Version=reviewed['bundle_version'], Timestamp='2026-10-08T01:00:00', Description='ULike1.9.45 exact SIMD/filter reuse and codec pipeline; other apps retained from1.0.177. Device speed/quality unmeasured.')
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
    qa = {'schema': 'ulike1945-speed-v1', 'status': 'BUILT_HOST_VERIFIED_DEVICE_UNVERIFIED',
          'ulike_version': VERSION, 'bundle_version': reviewed['bundle_version'], 'base_versions': [BASE_VERSION, reviewed['baseline_bundle_version']],
          'approved_lineage': '1.8.8', 'selected_candidates': SELECTED,
          'device_tested': False, 'device_quality_verified': False, 'original_apk_apply_tested': False, 'ci_android_apply_tested': False,
          'non_ulike_resources_byte_identical': True, 'non_ulike_loader_classes_unchanged': True,
          'camera_lifecycle_byte_identical': True, 'camera_startup_recovery_byte_identical': True, 'front_lens_fixes_preserved': True, 'viewport_layout_byte_identical': True, 'image_pipeline_byte_identical': False, 'quality_pixel_math_byte_identical': False, 'quality_algorithm_and_settings_preserved': True, 'host_pixel_equivalence_passed': True, 'manual_capture_safety_checks_preserved': True, 'black_bar_gesture_byte_identical': True, 'resolution_and_save_format_preserved': True, 'shutter_feedback_byte_identical': True, 'capture_save_policies_byte_identical': False,
          'standalone_and_bundle_ulike_resources_identical': True,
          'native_methods_byte_identical': final['ulike/methods.dex'] == base['ulike/methods.dex'],
          'native_methods_tsv_byte_identical': final['ulike/methods.tsv'] == base['ulike/methods.tsv'],
          'host_quality_passed': True, 'host_quality_result': result, 'host_capture_timing_passed': True, 'actual_sensor_capture_latency_measured': False, 'device_save_speed_measured': False,
          'production_source_consistency_verified': True, 'compiled_production_source_sha256': compiled_source_sha256, 'compiled_transformer_source_sha256': transformer_source_sha256, 'dex_verification': verification.strip(),
          'changed_standalone_entries': changes, 'added_standalone_entries': added,
          'changed_bundle_entries': sorted(n for n in integrated.keys() & bundle.keys() if integrated[n] != bundle[n]),
          'added_bundle_entries': sorted(integrated.keys() - bundle.keys()),
          'native_library_sha256': sha(native_library.read_bytes()), 'native_library_bytes': native_library.stat().st_size,
          'native_library_byte_identical': False, 'native_installer_byte_identical': False,
          'native_preservation': native_preservation, 'native_build_report': native_report, 'native_source_consistency_verified': True, 'speed_and_save_helpers_byte_identical': False, 'encoder_start_synchronization_changed': True, 'save_encoding_helpers_byte_identical': False, 'save_format_and_codec_configuration_preserved': True,
          'artifacts': artifacts, 'input_sha256': pins, 'jdk_identity': jdk_identity,
          'published': False, 'manager_feed_updated': False,
          'limitations': ['No Android/Galaxy/ART or camera-hardware execution.', 'No measured sensor capture start or shutter latency reduction.', 'Host equality and scheduling tests do not establish device timing, thermals or visual quality.']}
    qa.update(inventory)
    (args.output / 'QA_ULike_v1.9.45.json').write_text(json.dumps(qa, ensure_ascii=False, indent=2, allow_nan=False) + '\n')
    for name in ('emitted-audit.tsv', 'speed-dex-verification.txt', 'helper-references.txt', 'host-regression1945-result.json', 'native-update1945.json'):
        shutil.copyfile(args.work / name, args.output / name)
    shutil.copyfile(emitted / 'speed-inventory.json', args.output / 'speed-inventory.json')
    shutil.copyfile(native_report_path, args.output / 'native-build1945.json')
    for pattern in ('host-*.txt', 'host-regression*.json'):
        for path in args.work.glob(pattern):
            shutil.copyfile(path, args.output / path.name)
    for host_folder in sorted(args.work.iterdir()):
        if not host_folder.is_dir() or not host_folder.name.startswith(('host-', 'gesture-host')):
            continue
        for path in sorted(host_folder.rglob('*')):
            if not path.is_file() or path.suffix not in ('.txt', '.log', '.json') or path.name.endswith('.diagnostics.json'):
                continue
            relative = path.relative_to(args.work)
            if any(part in ('classes', 'src', 'fixtures') for part in relative.parts):
                continue
            target = args.output / 'host-evidence' / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(path, target)
    print(json.dumps({'status': qa['status'], 'bundle_version': reviewed['bundle_version'], 'host_quality_result': result, 'artifacts': artifacts}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=ROOT.parent / 'manifest.json')
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--native-library', type=Path, required=True)
    parser.add_argument('--ndk', type=Path, required=True)
    options = parser.parse_args()
    if options.jdk:
        os.environ['PATH'] = str(options.jdk.resolve() / 'bin') + os.pathsep + os.environ.get('PATH', '')
    for key, value in vars(options).items():
        if value is not None:
            setattr(options, key, value.resolve())
    os.environ['ULIKE_NDK'] = str(options.ndk)
    build(options)
