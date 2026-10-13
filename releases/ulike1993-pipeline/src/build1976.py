#!/usr/bin/env python3
"""Build .76 CPU/GPU pipeline from exact .75/.208; CPU/shader math is preserved."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION = '1.9.76', '1.9.75'
BUNDLE_VERSION, BASE_BUNDLE_VERSION = '1.0.209', '1.0.208'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.76.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.75.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.209.mpp'
BASE_BUNDLE = 'Hiro_Morphe_Patches_v1.0.208.mpp'
BASE_SINGLE_SHA256 = '706a47c1110516a3e8171cb7f69151c07e6742be81d78a2aa5211705ff30eb84'
BASE_BUNDLE_SHA256 = '5bab2abea55b8e1082c69b4ad2d87de4cbd866af19a36cba410ba0cd61e14bde'
BASE_SINGLE_BYTES, BASE_BUNDLE_BYTES = 1226477, 17873460
SELECTED = ['CPU_GPU_PIPELINE_6_12']
PRODUCTION = json.loads((ROOT / 'production1976.json').read_text())
GPU_ENTRY = 'ulike1960/runtime/libulike_gpu1960.so'
MOIRE_ENTRY = 'ulike1951/runtime/libulike_moire1951.so'
NR_ENTRY = 'ulike1955/runtime/libulike_nr1955.so'
NATIVE_ENTRIES = {GPU_ENTRY, MOIRE_ENTRY, NR_ENTRY}
NEW_JNI = ['Lcom/hiro/ulike/GpuNoise1960;->copyNative1976(JIIIII)Z', 'Lcom/hiro/ulike/GpuNoise1960;->uploadRangeNative1976(JII[III)Z', 'Lcom/hiro/ulike/NativeMoire1951;->finishStripCached1976([I[I[IZIIIIZZIIIZZ[I)Z', 'Lcom/hiro/ulike/ColourCache1976;->cpuNative([I[IIIIIIIIIZIIIIII[F[IZ)Z', 'Lcom/hiro/ulike/ColourCache1976;->prepareNative([IIIIIIIIIZIIIII[FZ)[F', 'Lcom/hiro/ulike/ColourCache1976;->directNative(Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;[IIIIIIIIIZIIIII[FZ)Z']
NATIVE_JNI1976 = {name: (MOIRE_ENTRY if ';->finishStripCached1976(' in name else NR_ENTRY if ';->cpuNative(' in name else GPU_ENTRY) for name in NEW_JNI}
INSTALLER = 'app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER = 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED = {'META-INF/MANIFEST.MF', 'classes.dex', 'ulike/runtime.dex', LOADER, INSTALLER, *NATIVE_ENTRIES}
ADDED = set()
REMOVED = []
PUBLISHER_FILES = ('publish1976.py', 'ulike1976-gpu-latency-publish.yml', 'README.md')
TOOL_PINS = common.TOOL_PINS
require, sha, run, verify_jdk = common.require, common.sha, common.run, common.verify_jdk
archive, headers, manifest, own, write_zip = common.archive, common.headers, common.manifest, common.own, common.write_zip
HOST_FLAGS = ('tuning_full_exact_gate_verified','tuning_snapshot_ownership_and_cancel_verified','scalar_only_tuning_persistence_verified','strong_overlap_actual_jni1975','strong_overlap_failure_drain1975','strong_serial_fallback_actual_jni1975','strong_repeat_no_large_upload1975','strong_reusable_readback_actual_jni1975','strong_overlap1975_regressions_passed','strong_upload_reuse1975_regressions_passed','strong_buffer_reuse1975_regressions_passed','reusable_multi_output_readback_verified','private_partial_output_never_committed','overlap_scratch_admission_serial_fallback_verified','multi_output_cancel_and_quarantine_verified','strong_preflight1975_regressions_passed','materialized_java_readback_counted_once','remaining_java_allocation_stays_reserved','strong_latency_controls_regressions_passed','strong_latency_regressions_passed','cold_proof_bounds_passed','strong_singleflight_regressions_passed','strong_first_mismatch_identity_regressions_passed','latency_budget_regressions_passed','strong_gpu_preference_regressions_passed','strong_generic_fallback_regressions_passed','qualification_certificate_regressions_passed','native_memory_admission_regressions_passed','native_failure_diagnostics_regressions_passed','strong_failure_diagnostics_regressions_passed','backend_diagnostics_regressions_passed','backend_visibility_regressions_passed','strong_preference_telemetry_regressions_passed','strong_work_latency_regressions_passed','strong_full_mismatch_regressions_passed','native_facade_regressions_passed','gpu_shader_execution_on_host','exact_division_regressions_passed','legacy_program_semantics_preserved','latest_camera_layout_regressions_passed','fresh_camera_host_execution_in_this_release','gpu_safety_gates_preserved')
HOST_FLAGS += ('twelve_candidate_profile_tuning_verified','resident_slot_memory_admission1976_verified','stage_details1976_regressions_passed', 'baseline75_exact', 'actual_production_jni_executed', 'actual_arm_neon_executed', 'private_abort_rollback', 'concurrent_workers_exact', 'geometry_two_exact_trials_verified', 'geometry_queue_ownership_and_cancel_verified', 'independent_shape_certificates_required')
HOST_FLAGS += ('exact_tiled1976_actual_jni', 'legacy_program_pixels_preserved', 'full_argb_confidence_exact', 'cropped_halo_and_two_banks', 'resident_copy_range1976_actual_jni', 'virtual_slot24_budget_verified')
HOST_FLAGS += ('production_java_and_jni', 'default_original_loader', 'exact2_and_five_percent_gate', 'actual_mode_branches', 'idle_queue_fixture', 'cancellation_and_memory_refusal', 'foreground_cpu_finish_cache_and_fallback_verified', 'published75_mask_exact', 'published75_policy_exact', 'published75_native_pixels_exact', 'unknown_callback_order_unchanged', 'source_immutable_and_alpha_edges_exact', 'jni_concurrency_cancel_executed', 'cpu_finish_two_exact_and_speed_gate_verified', 'cpu_finish_snapshot_cancel_memory_verified', 'resident_two_whole_exact_trials_verified', 'resident_original_route_timing_gate_verified', 'resident_snapshot_cancel_ownership_verified', 'chain_pipeline1976_actual_jni', 'chain_resident1976_actual_jni', 'chain_source_immutable1976_verified', 'chain_fault_cancel_drain1976_verified', 'chain_unsupported_formats1976_verified', 'chain_cancel_preserves_exact_certificate1976_verified', 'actual_whole_pipeline_jni_verified', 'actual_layout_halo_equivalence_verified')
HOST_FLAGS += ('chain_legacy_sync1976_exact_verified', 'chain_measured_old_gpu_baseline1976_verified', 'chain_legacy_fallback1976_actual_jni')
PRESERVATION_FIELDS = ('unchanged_pixel_kernel_methods_bytecode_identical', 'unmodified_pixel_kernel_helpers_bytecode_identical', 'quality_algorithm_and_settings_preserved',
    'save_encoding_helpers_byte_identical', 'unrelated_runtime_classes_bytecode_identical',
        'native_methods_byte_identical', 'native_methods_tsv_byte_identical', 'camera_control_lifecycle_bytecode_identical', 'diagnostic_measurement_and_logger_bodies_preserved',
    'unmodified_gpu_runtime_helpers_bytecode_identical', 'camera_session_bytecode_identical', 'preview_output_observer_bytecode_identical', 'black_tap_disabled', 'capture_class_bytecode_identical', 'capture_begin_image_false_return_verified', 'timing_camera_trace_hooks_preserved')

def production_path(name):
    path = ROOT / (name + '.java')
    if not path.is_file(): path = ROOT / 'quality-dependencies/com/hiro/ulike' / (name + '.java')
    require(path.is_file(), 'Missing camera production source ' + name)
    return path

def json_bytes(data):
    return (json.dumps(data, ensure_ascii=False, sort_keys=True, indent=2) + '\n').encode()

def source_pins():
    return {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in sorted(ROOT.rglob('*'))
        if p.is_file() and not p.is_symlink() and '__pycache__' not in p.parts
        and p.suffix in ('.java', '.py', '.c', '.h', '.comp', '.glsl', '.sh', '.txt', '.json', '.dex', '.tsv', '.md')}

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
    require(inventory['new_jni_methods'] == sorted(NEW_JNI) and inventory['new_native_methods'] == sorted(NEW_JNI) and inventory['changed_native_methods'] == [], 'preview repair changed native declarations')
    require(inventory['replaced_helper_roots'] == sorted(PRODUCTION), 'Replaced camera helper inventory differs')
    require(type(inventory.get('preserved_runtime_class_count')) is int
        and inventory['preserved_runtime_class_count'] > 0, 'Whole-runtime preservation count absent')
    require(type(inventory.get('diagnostic_preserved_method_count')) is int
        and inventory['diagnostic_preserved_method_count'] > 0, 'Version-normalized non-UI method body proof absent')

def compile_inputs():
    # Compiler-only declarations are excluded from D8. Retained DEX is the ABI.
    stubs = {}
    folders = ['layout1937-stubs','geometry1937-compile','front1936-stubs','renderer1938-stubs',
        'capture-stubs','fast-stubs','metadata-stubs','quality-stubs','quality-dependencies',
        'timing1947-stubs','core1950-stubs']
    for folder in folders:
        parent = ROOT / folder
        if parent.exists():
            for path in sorted(parent.rglob('*.java')):
                name = path.relative_to(parent).as_posix()
                if not name.startswith('android/'):
                    stubs[name] = path
    retained = ['FastResize1933','FastPixels1933','StrongNoise1957','AsyncSave1935','ProcessingTiming1947','QualityPipeline1932',
        'ShotContext1932','TimedIo1947','QualityPixels1932','QualityShadow1932','SpeedWorkers1935',
        'SaveQueue1935','FusionPixels1933','ReflectionCache1945','CodecDrain1945','NoiseCache1944',
        'NativeSpeed1944','PolicyCache1945','Scheduling1944','GpuInteger1949','CorePixels1950',
        'NativeMoire1951','GpuFinish1952','WholeRoute1952','PerformanceHints1952','GpuFinish1953',
        'FinishPolicy1953','WholeRoute1953','SpatialNoise1934','SingleNoise1955','StrongNoise1958',
        'GpuNoise1960','GpuQualification1961','GpuStrong1960','GpuSingle1960','GpuBank1960',
        'GpuBudget1960','GpuPolicy1960','GpuAdmission1962','GpuFinish1961','GpuResidual1961','GpuProtection1961',
        'CpuGpuSchedule1964','StrongPrepared1964','ResidualTransfer1964','ResidualOverlap1962',
        'SingleResidual1961','CameraTrace1965','CameraSession1965','PreviewOutput1965','RenderStartup1938','PreviewLayout1922','LayoutLifecycle1937','FrontPreview1936']
    for name in retained:
        path = ROOT / (name + '.java')
        if path.is_file():
            stubs['com/hiro/ulike/' + name + '.java'] = path
    for path in sorted(ROOT.glob('*.java')):
        if path.name.startswith(('Gpu','Cpu','Strong','Native','Quality','Single','Residual','Face')):
            stubs['com/hiro/ulike/' + path.name] = path
    for name in PRODUCTION:
        stubs.pop('com/hiro/ulike/' + name + '.java', None)
    return stubs

def build(args):
    identity = verify_jdk()
    declaration = json.loads((ROOT.parent / 'manifest.json').read_text())
    current_pins, current_publication_pins = source_pins(), publication_pins()
    require(declaration.get('schema') == 'ulike1976-build-declaration-v1'
        and declaration.get('reviewed_source_sha256') == current_pins
        and declaration.get('reviewed_publication_sha256') == current_publication_pins,
        'Reviewed source/publication graph changed before build')
    require(declaration.get('production_helper_roots') == PRODUCTION, 'Reviewed camera production roots differ')
    pixel_reference = ROOT/'tests1976/source-scope75.json'
    require(not args.work.exists(), 'Fresh build directory required')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    source_scope = load_module('source_scope1976_build', ROOT/'source_scope1976.py').test(ROOT, args.work/'source-scope')
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
    require(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$', (args.ndk/'source.properties').read_text(), re.M), 'Pinned NDK r27c required')
    native_output = args.work/'gpu-native'
    run(['python3', ROOT/'native1960/build_native1960.py', '--ndk', args.ndk, '--out', native_output], args.work/'native1960-build.log')
    native = native_output/'libulike_gpu1960.so'; native_bytes = native.read_bytes()
    require(native_bytes[:6] == b'\x7fELF\x02\x01' and native_bytes[18:20] == b'\xb7\x00', 'ELF64 AArch64 GPU payload required')
    ndk_bin = args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
    elf = run([ndk_bin/'llvm-readelf', '-h', '-d', '-Ws', '--program-headers', '--wide', native], args.work/'gpu-native-readelf.txt')
    native_builder = load_module('native1975_builder', ROOT/'native1960/build_native1960.py')
    exports = ['Java_com_hiro_ulike_GpuNoise1960_'+name for name in native_builder.JNI_NAMES]+['Java_com_hiro_ulike_SingleResidual1961_prepareNative','Java_com_hiro_ulike_SingleResidual1961_prepareDirectNative','Java_com_hiro_ulike_ColourCache1976_prepareNative','Java_com_hiro_ulike_ColourCache1976_directNative']
    actual_exports = set(re.findall(r'\bJava_[A-Za-z0-9_]+', elf))
    require('AArch64' in elf and actual_exports == set(exports), 'Exact GPU native JNI exports differ')
    alignments = [int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
    needed = re.findall(r'\(NEEDED\).*?\[(.*?)\]', elf)
    require(alignments and all(x >= 16384 for x in alignments) and set(needed) <= {'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'}, 'GPU ELF alignment/dependencies differ')
    native_sources = {p.relative_to(ROOT/'native1960').as_posix():sha(p.read_bytes()) for p in sorted((ROOT/'native1960').rglob('*')) if p.is_file() and p.suffix in ('.c','.h','.comp','.py')}
    native_report = {'library':native.name, 'sha256':sha(native_bytes), 'bytes':len(native_bytes), 'ndk_revision':'27.2.12479018', 'build_action':'rebuilt_reviewed_gpu_cpu_pipeline', 'native_abi':19601, 'abi':'arm64-v8a', 'min_sdk':26, 'physical_android_tested':False, 'sources':native_sources, 'jni_exports':exports, 'load_segment_alignments':alignments, 'needed_libraries':needed}
    native_payloads = {GPU_ENTRY: native_bytes}
    native_reports = {GPU_ENTRY: native_report}
    for number, entry, builder_name in ((1951, MOIRE_ENTRY, 'build_native1951.py'), (1955, NR_ENTRY, 'build_native1955.py')):
        output = args.work / ('native' + str(number))
        run(['python3', ROOT / ('native' + str(number)) / builder_name, '--ndk', args.ndk, '--output', output], args.work / ('native' + str(number) + '-build.log'))
        library = output / Path(entry).name
        payload = library.read_bytes()
        require(payload[:6] == b'\x7fELF\x02\x01' and payload[18:20] == b'\xb7\x00', 'ELF64 AArch64 CPU payload required')
        listing = run([ndk_bin/'llvm-readelf', '-h', '-d', '-Ws', '--program-headers', '--wide', library], args.work / ('native' + str(number) + '-readelf.txt'))
        segment_alignment = [int(line.split()[-1], 0) for line in listing.splitlines() if line.strip().startswith('LOAD ')]
        dependencies = re.findall(r'\(NEEDED\).*?\[(.*?)\]', listing)
        require(segment_alignment and all(x >= 16384 for x in segment_alignment) and set(dependencies) <= {'libc.so','libm.so','libdl.so'}, 'CPU ELF alignment/dependencies differ')
        source_folder = ROOT / ('native' + str(number))
        source_hashes = {p.relative_to(source_folder).as_posix(): sha(p.read_bytes()) for p in sorted(source_folder.rglob('*')) if p.is_file() and p.suffix in ('.c','.h','.py')}
        report = {'library': library.name, 'sha256': sha(payload), 'bytes': len(payload), 'ndk_revision': '27.2.12479018', 'abi':'arm64-v8a', 'physical_android_tested':False, 'jni_exports': sorted(set(re.findall(r'\bJava_[A-Za-z0-9_]+', listing))), 'sources':source_hashes, 'load_segment_alignments':segment_alignment, 'needed_libraries':dependencies}
        native_reports[entry] = report
        native_payloads[entry] = payload
    require(set(native_payloads) == NATIVE_ENTRIES, 'Exact reviewed native resource graph')
    for entry, report in native_reports.items():
        prior_elf = run([ndk_bin/'llvm-readelf', '-Ws', '--wide', baseline/entry], args.work / (Path(entry).name + '-baseline-exports.txt'))
        prior_exports = set(re.findall(r'\bJava_[A-Za-z0-9_]+', prior_elf))
        require(set(NATIVE_JNI1976) == set(NEW_JNI), 'Every new native symbol has exactly one reviewed library')
        added_exports = {'Java_com_hiro_ulike_' + method.split(';->')[0].rsplit('/', 1)[1] + '_' + method.split(';->')[1].split('(')[0] for method in NEW_JNI if NATIVE_JNI1976[method] == entry}
        require(set(report['jni_exports']) == prior_exports | added_exports, 'Native export scope changed: ' + entry)

    bundle_dex = baseline / 'bundle.dex'
    bundle_dex.write_bytes(bundle['classes.dex'])
    classes, helpers, dex = (args.work / name for name in ('classes', 'helper-classes', 'helper-dex'))
    for path in (classes, helpers, dex):
        path.mkdir()
    production = [production_path(name) for name in PRODUCTION]
    compiled_sources = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in production}
    stub_map = compile_inputs()
    stubs = list(stub_map.values())
    compile_only_pins = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in stub_map.values()}
    run(['javac', '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-bootclasspath', args.tools / 'android.jar', '-d', helpers, *stubs, *production], args.work / 'helper-javac.log')
    selected = sorted(p for p in (helpers / 'com/hiro/ulike').glob('*.class')
        if any(p.name == name + '.class' or p.name.startswith(name + '$') for name in PRODUCTION))
    require(all(helpers / 'com/hiro/ulike' / (name + '.class') in selected for name in PRODUCTION), 'Missing compiled camera production root')
    require(all(any(p.name == n + '.class' or p.name.startswith(n + '$') for n in PRODUCTION) for p in selected), 'Compile-only stub selected for D8')
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26', '--lib', args.tools / 'android.jar', '--classpath', helpers, '--output', dex, *selected], args.work / 'd8.log')
    cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', baseline, classes)))
    transformers = ['MergePayloads', 'Transform1976', 'TimingCameraHooks1976', 'VerifyHelperReferences']
    transformer_sources = [ROOT / (name + '.java') for name in transformers] + [ROOT / 'PatchClass1976.java', ROOT / 'PatchLoader1976.java']
    transformer_pins = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in transformer_sources}
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *[ROOT / (name + '.java') for name in transformers]], args.work / 'transform-javac.log')
    exports = ['--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
    run(['javac', '-encoding', 'UTF-8', *exports, '-cp', cp, '-d', classes, ROOT / 'PatchClass1976.java', ROOT / 'PatchLoader1976.java'], args.work / 'metadata-javac.log')
    properties = ['-Dulike.production=' + ','.join(PRODUCTION), '-Dulike.newjni=' + ','.join(NEW_JNI), '-Dulike.gpu1960.sha=' + sha(native_bytes), '-Dulike.gpu1960.bytes=' + str(len(native_bytes))]
    for entry, prefix in ((MOIRE_ENTRY, 'moire1951'), (NR_ENTRY, 'nr1955')):
        properties += ['-Dulike.'+prefix+'.sha='+sha(native_payloads[entry]), '-Dulike.'+prefix+'.bytes='+str(len(native_payloads[entry]))]
    for name in ('emitted', 'repeat'):
        run(['java', *properties, '-cp', cp, 'Transform1976', baseline, dex / 'classes.dex', bundle_dex, args.work / name, args.work / (name + '-audit.tsv')], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} == {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()}, 'Non-deterministic camera DEX transform')
    inventory = json.loads((emitted / 'gpu-latency-inventory1976.json').read_text())
    inventory_checks(inventory)
    require(inventory.get('test_fixture_classes_absent_from_runtime') is True, 'Compile/test fixtures leaked into runtime DEX')
    run(['java', *exports, '-cp', cp, 'PatchClass1976', baseline / LOADER, emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    run(['java', *exports, '-cp', cp, 'PatchLoader1976', baseline / INSTALLER, native, args.work/'native1951/libulike_moire1951.so', args.work/'native1955/libulike_nr1955.so', emitted / 'IntegrationPayload186.class'], args.work / 'native-installer-metadata.log')
    refs = run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex', baseline / 'ulike/methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    require(refs.startswith('PASS helper references in '), 'Fresh UI/logging helper linkage proof absent')
    final, integrated = dict(base), dict(bundle)
    for name, data in {'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(), LOADER: (emitted / 'UlikeHqMaxPatch.class').read_bytes(), INSTALLER: (emitted / 'IntegrationPayload186.class').read_bytes(), **native_payloads}.items():
        final[name] = data
        integrated[name] = data
    final['classes.dex'] = (emitted / 'loader.dex').read_bytes()
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    fields = headers(base['META-INF/MANIFEST.MF'])
    fields.update(Version=VERSION, Description='CPU/GPU pipeline candidates 6-12 from .75: exact colour and mask reuse, bounded correction caches, overlapped GPU correction, measured strip selection and resident transfer; physical device unverified.')
    final['META-INF/MANIFEST.MF'] = manifest(fields)
    fields = headers(bundle['META-INF/MANIFEST.MF'])
    fields.update(Version=BUNDLE_VERSION, Description='ULike CPU/GPU pipeline candidates 6-12 with exact-output and measured-speed qualification; guarded fallback and other apps preserved; physical device unverified.')
    integrated['META-INF/MANIFEST.MF'] = manifest(fields)
    single_delta, bundle_delta = resource_delta(base, final, 'standalone'), resource_delta(bundle, integrated, 'bundle')
    require({n: b for n, b in final.items() if own(n)} == {n: b for n, b in integrated.items() if own(n)}, 'Current ULike copies differ')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')), 'Unrelated app resource changed')
    inherited = {n: {'sha256': sha(b), 'bytes': len(b)} for n, b in base.items() if (n.endswith('.so') or n == 'ulike186/runtime/0000.bin') and n not in NATIVE_ENTRIES}
    require(len(inherited) == 9 and all(final[n] == base[n] for n in inherited), 'All nine unchanged native payloads must remain byte-identical')
    require(all(final[n] == base[n] for n in ('ulike/methods.dex', 'ulike/methods.tsv')), 'Native installer or native method resources changed')
    for name in ('ulike/runtime.dex', 'classes.dex'):
        dex_integrity(final[name], name)
    dex_integrity(integrated['classes.dex'], 'bundle classes.dex')
    write_zip(args.output / SINGLE, final)
    write_zip(args.output / BUNDLE, integrated)
    for name in (SINGLE, BUNDLE):
        run(['java', '-jar', args.tools / 'morphe.jar', 'list-patches', '--patches', args.output / name], args.work / (name + '.loader.log'))
    if 'reviewed_mpp_artifacts' in declaration:
        expected = declaration['reviewed_mpp_artifacts']
        actual = {name: {'sha256': sha((args.output / name).read_bytes()), 'bytes': (args.output / name).stat().st_size} for name in (SINGLE, BUNDLE)}
        require(expected == actual, 'CI/rebuild MPP bytes differ from locally reviewed packages')
    if args.prepare_only:
        print(json.dumps({'status': 'PREPARE_ONLY_NOT_FOR_PUBLICATION', 'host_tests_run': False, 'work': str(args.work), 'output': str(args.output)}, indent=2))
        return None
    os.environ['ULIKE_MORPHE_JAR'] = str(args.tools / 'morphe.jar')
    host = load_module('host_strong_gpu1976_build', ROOT / 'host_strong_gpu1976.py')
    result = host.test(ROOT, args.work / 'camera-host', jdk=args.jdk, ndk=args.ndk)
    require(isinstance(result, dict) and result.get('status') == 'passed'
        and type(result.get('assertions')) is int and result['assertions'] > 0, 'Fresh UI/logging host evidence required')
    require(current_pins == source_pins() and current_publication_pins == publication_pins(), 'Reviewed source/publication graph changed during build/test')
    artifacts = {name: {'sha256': sha((args.output / name).read_bytes()), 'bytes': (args.output / name).stat().st_size} for name in (SINGLE, BUNDLE)}
    qa = {**inventory, 'schema': 'ulike1976-gpu-latency-v1',
        'status': 'MPP_CPU_GPU_PIPELINE_6_12_HOST_VERIFIED_ORIGINAL_APKS_AND_DEVICE_UNVERIFIED',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION, 'baseline_ulike_version': BASE_VERSION,
        'baseline_bundle_version': BASE_BUNDLE_VERSION, 'selected_candidates': SELECTED, 'input_sha256': pins,
        'artifacts': artifacts, 'jdk_identity': identity, 'approved_lineage': 'ULike1.8.8',
        'strong_gpu_host_evidence': result, 'strong_gpu_host_evidence_sha256': sha(json.dumps(result, sort_keys=True).encode()),
        'host_quality_passed': True, 'unmodified_pixel_kernels_identical_to_baseline': True,
        'intentional_quality_algorithm_change': False, 'capture_fusion_enabled': False,
        'production_source_consistency_verified': True, 'compiled_production_source_sha256': compiled_sources,
        'compiled_compile_only_source_sha256': compile_only_pins, 'compiled_transformer_source_sha256': transformer_pins,
        'executed_host_source_sha256': current_pins, 'inherited_native_payloads': inherited,
        'inherited_native_libraries_byte_identical': True, 'native_library_byte_identical': False,
        'native_installer_byte_identical': False, 'native_installer_inverse_verified': True,
        'native_installer_existing_rows_preserved': True, 'native_installer_existing_row_count_preserved': True,
        'native_installer_preserved_rows': 9, 'native_installer_updated_rows': 3, 'native_installer_appended_rows': 0,
        'native_installer_total_rows': 12, 'new_native_methods': sorted(NEW_JNI), 'new_jni_methods': sorted(NEW_JNI), 'changed_native_methods': [],
        'non_ulike_loader_classes_unchanged': True, 'non_ulike_resources_byte_identical': True,
        'standalone_and_bundle_ulike_resources_identical': True, 'resolution_and_save_format_preserved': True,
        'save_publication_hooks_preserved': True, 'save_publication_contract_preserved': True,
        'save_format_and_codec_configuration_preserved': True, 'inherited_host_validation_source_version': BASE_VERSION,
        'stage_timing_algorithm_preserved': True, 'diagnostic_stage_names': ['fusion', 'noise', 'correction', 'compression', 'save'],
        'unmodified_gpu_shader_and_cpu_sources_byte_identical': True, 'gpu_arithmetic_implementation_changed': False, 'gpu_pixel_source_reference_sha256': sha(pixel_reference.read_bytes()),
        'compiled_gpu_native_source_sha256': native_sources, 'rebuilt_native_payloads': native_reports, 'gpu_native_build': native_report, 'gpu_native_jni_exports': native_report['jni_exports'],
        'gpu_native_load_segment_alignments': alignments, 'gpu_native_needed_libraries': needed,
        'gpu_native_library_sha256': sha(native_bytes), 'gpu_native_library_bytes': len(native_bytes),
        'fresh_whole_app_audit_in_this_release': False, 'inherited_gpu_validation': {'fresh_gpu_execution_in_this_release': False},
        'persistent_rolling_across_restarts': True, 'anomaly_snapshots_protected': True,
        'user_incident_snapshot': True, 'logging_async_bounded': True,
        'camera_visible_preview_verified_on_device': False, 'camera_control_lifecycle_bytecode_identical': True,
        'strong_generic_fallback_regressions_passed': result.get('strong_generic_fallback_regressions_passed') is True,
        'native_memory_admission_regressions_passed': result.get('native_memory_admission_regressions_passed') is True,
        'native_failure_diagnostics_regressions_passed': result.get('native_failure_diagnostics_regressions_passed') is True,
        'strong_failure_diagnostics_regressions_passed': result.get('strong_failure_diagnostics_regressions_passed') is True,
        'native_facade_regressions_passed': result.get('native_facade_regressions_passed') is True,
        'gpu_shader_execution_on_host': result.get('gpu_shader_execution_on_host') is True,
        'strong_gpu_preference_regressions_passed': result.get('strong_gpu_preference_regressions_passed') is True,
        'qualification_certificate_regressions_passed': result.get('qualification_certificate_regressions_passed') is True,
        'backend_diagnostics_regressions_passed': result.get('backend_diagnostics_regressions_passed') is True,
        'strong_preference_telemetry_regressions_passed': result.get('strong_preference_telemetry_regressions_passed') is True,
        'unmodified_gpu_runtime_helpers_bytecode_identical': inventory.get('unmodified_gpu_runtime_helpers_bytecode_identical') is True,
        'gpu_safety_gates_preserved': result.get('gpu_safety_gates_preserved') is True, 'camera_trace_log_data_excluded': True,
        'strong_gpu_preference_mode': 3, 'strong_gpu_quality_trials': 2, 'strong_gpu_speed_gate_required': False,
        'strong_gpu_bank_wait_limit_ms': 15000, 'strong_gpu_worker_limit': 4,
        'strong_cold_candidate_budget': 2, 'strong_same_key_proof_singleflight': True,
        'strong_new_gpu_program_ids': [42, 43, 44], 'legacy_gpu_program_ids': list(range(42)),
        'source_scope_evidence': source_scope,
        'source_scope_evidence_sha256': sha(json.dumps(source_scope,sort_keys=True).encode()),
        **{key:source_scope[key] for key in ('camera_trace_version_only_preserved','unmodified_cpu_and_other_shader_sources','unmodified_native_shader_sources_byte_identical','unmodified_java_sources_byte_identical','reviewed_native_scope_verified')},
        'inherited_logging_validation': {'version': BASE_VERSION, 'fresh_logger_regressions_in_this_release': False},
        'original_apk_apply_tested': False, 'original_split_merge_tested': False, 'ci_android_apply_tested': False,
        'cpu_cache_qualification_requires_gpu_environment_identity': True, 'device_tested': False, 'device_quality_verified': False, 'device_save_speed_measured': False,
        'gpu_execution_on_physical_android': False, 'all_processing_on_gpu': False,
        'gx9_beauty_interop_supported': False, 'gx10_encoder_interop_supported': False,
        'changed_standalone_entries': single_delta[0], 'added_standalone_entries': single_delta[1],
        'changed_bundle_entries': bundle_delta[0], 'added_bundle_entries': bundle_delta[1],
        'published': False, 'manager_feed_updated': False}
    for key in HOST_FLAGS:
        require(result.get(key) is True, 'Fresh current regression absent: '+key)
        qa[key] = True
    (args.output / 'QA_ULike_v1.9.76.json').write_bytes(json_bytes(qa))
    (args.output / 'host-gpu-latency1976-result.json').write_bytes(json_bytes(result))
    for name in ('emitted-audit.tsv', 'helper-references.txt', 'emitted.log', 'metadata.log', 'gpu-native-readelf.txt', 'native-installer-metadata.log'):
        shutil.copyfile(args.work / name, args.output / name)
    shutil.copyfile(emitted / 'gpu-latency-inventory1976.json', args.output / 'gpu-latency-inventory1976.json')
    print(json.dumps({'status': qa['status'], 'artifacts': artifacts, 'strong_gpu_host_assertions': result['assertions']}, ensure_ascii=False, indent=2))
    return qa

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    parser.add_argument('--ndk', type=Path, required=True)
    parser.add_argument('--prepare-only', action='store_true', help='Compile provisional packages without publishable host evidence')
    args = parser.parse_args()
    for name, value in vars(args).items():
        if isinstance(value, Path):
            setattr(args, name, value.resolve())
    build(args)

