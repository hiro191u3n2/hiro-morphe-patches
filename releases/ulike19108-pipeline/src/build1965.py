#!/usr/bin/env python3
"""Build the camera trace update from pinned .64/.197 without changing image/native code."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, struct, zlib
import build1945 as common

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION = '1.9.65', '1.9.64'
BUNDLE_VERSION, BASE_BUNDLE_VERSION = '1.0.198', '1.0.197'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.65.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.64.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.198.mpp'
BASE_BUNDLE = 'Hiro_Morphe_Patches_v1.0.197.mpp'
BASE_SINGLE_SHA256 = 'cf6621ed1ec55c5b115516a04f897e380c785c56d9772ab48043b32c568ae48b'
BASE_BUNDLE_SHA256 = 'acf1b3381a913431819a7ccf3f94a547428e43436e6e3bb99e208809683733e5'
BASE_SINGLE_BYTES, BASE_BUNDLE_BYTES = 1163379, 17810399
SELECTED = ['CAMERA_TRACE']
PRODUCTION = ['RenderStartup1938', 'PreviewLayout1922', 'LayoutLifecycle1937',
              'FrontPreview1936', 'CameraTrace1965', 'PreviewOutput1965', 'CameraSession1965']
INSTALLER = 'app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER = 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED = {'META-INF/MANIFEST.MF', 'classes.dex', 'ulike/runtime.dex', LOADER}
ADDED = set()
# The complete renderer family is recompiled together. Its private ticket
# constructor gains ownership arguments, and javac renumbers package-private
# synthetic accessors. No retained/public camera API is removed; DEX linkage is
# independently checked against every retained class.
REMOVED = sorted([
    'Lcom/hiro/ulike/RenderStartup1938$Pending;-><init>(Ljava/lang/Object;Landroid/view/Surface;J)V',
    'Lcom/hiro/ulike/RenderStartup1938;->access$400()Ljava/lang/Object;',
    'Lcom/hiro/ulike/RenderStartup1938;->access$500()Ljava/util/Map;',
    'Lcom/hiro/ulike/RenderStartup1938;->access$600(Ljava/lang/Object;Lcom/hiro/ulike/RenderStartup1938$Pending;)V',
    'Lcom/hiro/ulike/RenderStartup1938;->access$700(Ljava/lang/Object;Ljava/lang/String;)I'])
PUBLISHER_FILES = ('publish1965.py', 'ulike1965-camera-trace-publish.yml', 'README.md')
TOOL_PINS = common.TOOL_PINS
require, sha, run, verify_jdk = common.require, common.sha, common.run, common.verify_jdk
archive, headers, manifest, own, write_zip = common.archive, common.headers, common.manifest, common.own, common.write_zip
PRESERVATION_FIELDS = ('image_pipeline_byte_identical', 'quality_pixel_math_byte_identical',
    'save_encoding_helpers_byte_identical', 'unrelated_runtime_classes_bytecode_identical',
        'native_methods_byte_identical', 'native_methods_tsv_byte_identical',
    'black_tap_disabled', 'capture_class_bytecode_identical', 'capture_begin_image_false_return_verified')

def production_path(name):
    path = ROOT / (name + '.java')
    require(path.is_file(), 'Missing camera production source ' + name)
    return path

def json_bytes(data):
    return (json.dumps(data, ensure_ascii=False, sort_keys=True, indent=2) + '\n').encode()

def source_pins():
    return {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in sorted(ROOT.rglob('*'))
        if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts
        and p.suffix in ('.java', '.py', '.c', '.h', '.comp', '.sh', '.txt', '.json', '.dex', '.tsv', '.md')}

def publication_pins():
    return {name: sha((ROOT.parent / 'publication' / name).read_bytes()) for name in PUBLISHER_FILES}

def dex_integrity(raw, name):
    require(len(raw) >= 112 and raw[:4] == b'dex\n' and struct.unpack_from('<I', raw, 32)[0] == len(raw), 'Invalid DEX header: ' + name)
    require(raw[12:32] == hashlib.sha1(raw[32:]).digest()
        and struct.unpack_from('<I', raw, 8)[0] == zlib.adler32(raw[12:]) & 0xffffffff, 'Invalid DEX integrity: ' + name)

def resource_delta(before, after, label):
    require(set(before) <= set(after), 'Removed ' + label + ' resource')
    changed = {name for name in before if before[name] != after[name]}
    added = set(after) - set(before)
    require(changed == CHANGED and added == ADDED, 'Unexpected ' + label + ' resource delta: ' + repr((sorted(changed), sorted(added))))
    return sorted(changed), sorted(added)

def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

def inventory_checks(inventory):
    require(isinstance(inventory, dict), 'Missing serialized camera inventory')
    for key in PRESERVATION_FIELDS:
        require(inventory.get(key) is True, 'Fresh DEX preservation proof absent: ' + key)
    for key in ('changed_runtime_methods', 'new_runtime_methods', 'removed_runtime_methods',
                'new_helper_classes', 'new_runtime_aliases', 'replaced_helper_roots', 'bytecode_patch_helper_roots'):
        require(isinstance(inventory.get(key), list), 'Missing serialized inventory: ' + key)
    require(inventory['removed_runtime_methods'] == REMOVED, 'Camera update removed an unreviewed runtime method')
    require(inventory['replaced_helper_roots'] == sorted(PRODUCTION), 'Replaced camera helper inventory differs')
    require(type(inventory.get('preserved_runtime_class_count')) is int
        and inventory['preserved_runtime_class_count'] > 0, 'Whole-runtime preservation count absent')

def compile_inputs():
    # These declarations are compiler inputs only. Only PRODUCTION class families
    # are passed to D8; the baseline DEX remains the authoritative retained ABI.
    stubs = {}
    for folder in ('front1936-stubs', 'renderer1938-stubs', 'layout1937-stubs', 'geometry1937-compile'):
        parent = ROOT / folder
        if parent.exists():
            for path in sorted(parent.rglob('*.java')):
                if not path.relative_to(parent).as_posix().startswith('android/'):
                    stubs[path.relative_to(parent).as_posix()] = path
    for name in PRODUCTION:
        stubs.pop('com/hiro/ulike/' + name + '.java', None)
    return stubs

def build(args):
    identity = verify_jdk()
    declaration = json.loads((ROOT.parent / 'manifest.json').read_text())
    current_pins, current_publication_pins = source_pins(), publication_pins()
    require(declaration.get('schema') == 'ulike1965-build-declaration-v1'
        and declaration.get('reviewed_source_sha256') == current_pins
        and declaration.get('reviewed_publication_sha256') == current_publication_pins,
        'Reviewed source/publication graph changed before build')
    require(declaration.get('production_helper_roots') == PRODUCTION, 'Reviewed camera production roots differ')
    require(not args.work.exists(), 'Fresh build directory required')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    pins = {BASE_SINGLE: BASE_SINGLE_SHA256, BASE_BUNDLE: BASE_BUNDLE_SHA256, **TOOL_PINS}
    for name, digest in pins.items():
        path = (args.tools if name.endswith('.jar') else args.input) / name
        require(sha(path.read_bytes()) == digest, 'Pinned input differs: ' + name)
    require((args.input / BASE_SINGLE).stat().st_size == BASE_SINGLE_BYTES
        and (args.input / BASE_BUNDLE).stat().st_size == BASE_BUNDLE_BYTES, 'Pinned baseline byte sizes differ')
    base, bundle = archive(args.input / BASE_SINGLE), archive(args.input / BASE_BUNDLE)
    require(headers(base['META-INF/MANIFEST.MF'])['Version'] == BASE_VERSION
        and headers(bundle['META-INF/MANIFEST.MF'])['Version'] == BASE_BUNDLE_VERSION, 'Wrong baseline versions')
    require({n: b for n, b in base.items() if own(n)} == {n: b for n, b in bundle.items() if own(n)}, 'Baseline ULike copies differ')
    baseline = args.work / 'baseline'
    for name, data in base.items():
        path = baseline / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    bundle_dex = baseline / 'bundle.dex'
    bundle_dex.write_bytes(bundle['classes.dex'])
    classes, helpers, dex = (args.work / name for name in ('classes', 'helper-classes', 'helper-dex'))
    for path in (classes, helpers, dex):
        path.mkdir()
    production = [production_path(name) for name in PRODUCTION]
    compiled_sources = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in production}
    stub_map = compile_inputs()
    # Scheduling is retained in the baseline. This declaration deliberately has
    # no implementation and is never included in the helper DEX.
    stub = args.work / 'compile-only/com/hiro/ulike/Scheduling1944.java'
    stub.parent.mkdir(parents=True, exist_ok=True)
    stub.write_text('package com.hiro.ulike; public final class Scheduling1944 { public static long optionalRetryDelay(boolean busy,long normal,long now,long deadline){return normal;} }\n')
    stubs = list(stub_map.values()) + [stub]
    compile_only_pins = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in stub_map.values()}
    run(['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-bootclasspath', args.tools / 'android.jar', '-d', helpers, *stubs, *production], args.work / 'helper-javac.log')
    selected = sorted(p for p in (helpers / 'com/hiro/ulike').glob('*.class')
        if any(p.name == name + '.class' or p.name.startswith(name + '$') for name in PRODUCTION))
    require(all(helpers / 'com/hiro/ulike' / (name + '.class') in selected for name in PRODUCTION), 'Missing compiled camera production root')
    require(all(any(p.name == n + '.class' or p.name.startswith(n + '$') for n in PRODUCTION) for p in selected), 'Compile-only stub selected for D8')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26', '--lib', args.tools / 'android.jar', '--classpath', helpers, '--output', dex, *selected], args.work / 'd8.log')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    transformers = ['MergePayloads', 'Transform1965', 'CameraTraceHooks1965', 'VerifyHelperReferences']
    transformer_sources = [ROOT / (name + '.java') for name in transformers] + [ROOT / 'PatchClass1965.java']
    transformer_pins = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in transformer_sources}
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *[ROOT / (name + '.java') for name in transformers]], args.work / 'transform-javac.log')
    exports = ['--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
    run(['javac', '-encoding', 'UTF-8', *exports, '-cp', cp, '-d', classes, ROOT / 'PatchClass1965.java'], args.work / 'metadata-javac.log')
    properties = ['-Dulike.production=' + ','.join(PRODUCTION), '-Dulike.newjni=']
    for name in ('emitted', 'repeat'):
        run(['java', *properties, '-cp', cp, 'Transform1965', baseline, dex / 'classes.dex', bundle_dex, args.work / name, args.work / (name + '-audit.tsv')], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} == {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()}, 'Non-deterministic camera DEX transform')
    inventory = json.loads((emitted / 'camera-inventory1965.json').read_text())
    inventory_checks(inventory)
    require(inventory.get('test_fixture_classes_absent_from_runtime') is True, 'Compile/test fixtures leaked into runtime DEX')
    run(['java', *exports, '-cp', cp, 'PatchClass1965', baseline / LOADER, emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    refs = run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex', baseline / 'ulike/methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    require(refs.startswith('PASS helper references in '), 'Fresh camera helper linkage proof absent')
    final, integrated = dict(base), dict(bundle)
    for name, data in {'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(), LOADER: (emitted / 'UlikeHqMaxPatch.class').read_bytes()}.items():
        final[name] = data
        integrated[name] = data
    final['classes.dex'] = (emitted / 'loader.dex').read_bytes()
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    fields = headers(base['META-INF/MANIFEST.MF'])
    fields.update(Version=VERSION, Description='Camera lifecycle recovery and bounded incident trace; image/native/save code preserved; physical device unverified.')
    final['META-INF/MANIFEST.MF'] = manifest(fields)
    fields = headers(bundle['META-INF/MANIFEST.MF'])
    fields.update(Version=BUNDLE_VERSION, Description='ULike camera trace update; other app and all native payloads preserved; physical device unverified.')
    integrated['META-INF/MANIFEST.MF'] = manifest(fields)
    single_delta, bundle_delta = resource_delta(base, final, 'standalone'), resource_delta(bundle, integrated, 'bundle')
    require({n: b for n, b in final.items() if own(n)} == {n: b for n, b in integrated.items() if own(n)}, 'Current ULike copies differ')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')), 'Unrelated app resource changed')
    inherited = {n: {'sha256': sha(b), 'bytes': len(b)} for n, b in base.items() if n.endswith('.so') or n == 'ulike186/runtime/0000.bin'}
    require(len(inherited) == 12 and all(final[n] == base[n] for n in inherited), 'All twelve native payloads must remain byte-identical')
    require(all(final[n] == base[n] for n in (INSTALLER, 'ulike/methods.dex', 'ulike/methods.tsv')), 'Native installer or native method resources changed')
    for name in ('ulike/runtime.dex', 'classes.dex'):
        dex_integrity(final[name], name)
    dex_integrity(integrated['classes.dex'], 'bundle classes.dex')
    write_zip(args.output / SINGLE, final)
    write_zip(args.output / BUNDLE, integrated)
    for name in (SINGLE, BUNDLE):
        run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', args.output / name], args.work / (name + '.loader.log'))
    if args.prepare_only:
        print(json.dumps({'status': 'PREPARE_ONLY_NOT_FOR_PUBLICATION', 'host_tests_run': False, 'work': str(args.work), 'output': str(args.output)}, indent=2))
        return None
    os.environ['ULIKE_MORPHE_JAR'] = str(args.tools / 'morphe.jar')
    os.environ['ULIKE1965_BASELINE_MPP'] = str(args.input / BASE_SINGLE)
    host = load_module('host_camera1965_build', ROOT / 'host_camera1965.py')
    result = host.test(ROOT, args.work / 'camera-host', jdk=args.jdk)
    require(isinstance(result, dict) and result.get('status') == 'passed'
        and type(result.get('assertions')) is int and result['assertions'] > 0, 'Fresh camera host evidence required')
    require(current_pins == source_pins() and current_publication_pins == publication_pins(), 'Reviewed source/publication graph changed during build/test')
    artifacts = {name: {'sha256': sha((args.output / name).read_bytes()), 'bytes': (args.output / name).stat().st_size} for name in (SINGLE, BUNDLE)}
    qa = {**inventory, 'schema': 'ulike1965-camera-trace-v1',
        'status': 'MPP_CAMERA_TRACE_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION, 'baseline_ulike_version': BASE_VERSION,
        'baseline_bundle_version': BASE_BUNDLE_VERSION, 'selected_candidates': SELECTED, 'input_sha256': pins,
        'artifacts': artifacts, 'jdk_identity': identity, 'approved_lineage': 'ULike1.8.8',
        'camera_host_evidence': result, 'camera_host_evidence_sha256': sha(json.dumps(result, sort_keys=True).encode()),
        'host_quality_passed': True, 'host_pixel_equivalence_to_baseline': True,
        'intentional_quality_algorithm_change': False, 'capture_fusion_enabled': False,
        'production_source_consistency_verified': True, 'compiled_production_source_sha256': compiled_sources,
        'compiled_compile_only_source_sha256': compile_only_pins, 'compiled_transformer_source_sha256': transformer_pins,
        'executed_host_source_sha256': current_pins, 'inherited_native_payloads': inherited,
        'inherited_native_libraries_byte_identical': True, 'native_library_byte_identical': True,
        'native_installer_byte_identical': True,
        'native_installer_existing_rows_preserved': True, 'native_installer_existing_row_count_preserved': True,
        'native_installer_preserved_rows': 12, 'native_installer_updated_rows': 0, 'native_installer_appended_rows': 0,
        'native_installer_total_rows': 12, 'new_native_methods': [], 'new_jni_methods': [], 'changed_native_methods': [],
        'non_ulike_loader_classes_unchanged': True, 'non_ulike_resources_byte_identical': True,
        'standalone_and_bundle_ulike_resources_identical': True, 'resolution_and_save_format_preserved': True,
        'save_publication_hooks_preserved': True, 'save_publication_contract_preserved': True,
        'save_format_and_codec_configuration_preserved': True, 'inherited_host_validation_source_version': BASE_VERSION,
        'stage_timing_algorithm_preserved': True, 'diagnostic_stage_names': ['fusion', 'noise', 'correction', 'compression', 'save'],
        'gpu_native_library_sha256': sha(base['ulike1960/runtime/libulike_gpu1960.so']),
        'gpu_native_library_bytes': len(base['ulike1960/runtime/libulike_gpu1960.so']),
        'fresh_whole_app_audit_in_this_release': False, 'inherited_gpu_validation': {'fresh_gpu_execution_in_this_release': False},
        'persistent_rolling_across_restarts': True, 'anomaly_snapshots_protected': True,
        'user_incident_snapshot': True, 'logging_async_bounded': True,
        'camera_visible_preview_verified_on_device': False, 'camera_trace_log_data_excluded': True,
        'original_apk_apply_tested': False, 'original_split_merge_tested': False, 'ci_android_apply_tested': False,
        'device_tested': False, 'device_quality_verified': False, 'device_save_speed_measured': False,
        'gpu_execution_on_physical_android': False, 'all_processing_on_gpu': False,
        'gx9_beauty_interop_supported': False, 'gx10_encoder_interop_supported': False,
        'changed_standalone_entries': single_delta[0], 'added_standalone_entries': single_delta[1],
        'changed_bundle_entries': bundle_delta[0], 'added_bundle_entries': bundle_delta[1],
        'published': False, 'manager_feed_updated': False}
    (args.output / 'QA_ULike_v1.9.65.json').write_bytes(json_bytes(qa))
    (args.output / 'host-camera1965-result.json').write_bytes(json_bytes(result))
    for name in ('emitted-audit.tsv', 'helper-references.txt', 'emitted.log', 'metadata.log'):
        shutil.copyfile(args.work / name, args.output / name)
    shutil.copyfile(emitted / 'camera-inventory1965.json', args.output / 'camera-inventory1965.json')
    print(json.dumps({'status': qa['status'], 'artifacts': artifacts, 'camera_host_assertions': result['assertions']}, ensure_ascii=False, indent=2))
    return qa

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--prepare-only', action='store_true', help='Compile provisional packages without publishable host evidence')
    args = parser.parse_args()
    for name, value in vars(args).items():
        if isinstance(value, Path):
            setattr(args, name, value.resolve())
    build(args)
