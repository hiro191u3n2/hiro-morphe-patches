#!/usr/bin/env python3
"""Build .87 single-pass model snapshots, early policy dispatch and atomic Finish qualification from pinned .86/.219."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, shutil, struct, zlib
import build1945 as common
import application_evidence1987

ROOT = Path(__file__).resolve().parent
INHERITED_NUMERICAL_VALIDATION1978 = {'ulike_version': '1.9.78', 'bundle_version': '1.0.211', 'qa_url': 'https://github.com/hiro191u3n2/hiro-morphe-patches/releases/download/ulike-v1.9.78/QA_ULike_v1.9.78.json', 'qa_sha256': 'c127baa96cdc95ed0eda76d625e5e12a66b53207e699cb9af4b02c4a4b79bc98', 'assertions': 87613515, 'rerun_in_this_release': False, 'byte_identical_pixel_kernels_and_native_verified': True}
VERSION, BASE_VERSION = '1.9.87', '1.9.86'
BUNDLE_VERSION, BASE_BUNDLE_VERSION = '1.0.220', '1.0.219'
SINGLE = 'ULike_HQ_Texture_Online_v1.9.87.mpp'
BASE_SINGLE = 'ULike_HQ_Texture_Online_v1.9.86.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.220.mpp'
BASE_BUNDLE = 'Hiro_Morphe_Patches_v1.0.219.mpp'
BASE_SINGLE_SHA256 = 'e0687da4aca12fa2bad3e0380ca3289c8ffa74f7c12d327b0eb7affcb3447c7d'
BASE_BUNDLE_SHA256 = '6fd108dd1abd06602bebd85fa50ec3ed3b8ccad5d92c412e9dc07759d97b43bc'
BASE_SINGLE_BYTES, BASE_BUNDLE_BYTES = 1336294, 17983258
HISTORICAL_REGRESSION_INPUTS = {'ULike_HQ_Texture_Online_v1.9.80.mpp': {'bytes': 1280244, 'sha256': '2be00d742db6a8954a08e0dc43c27b11e1fb2d20c864d09a77b8010bb9a1ea25', 'url': 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/28f2bcc5bee767041034935185be19b987d253d2/downloads/ULike_HQ_Texture_Online_v1.9.80.mpp'}}
SELECTED = ['FIX1_SINGLE_PASS_MODEL_PROOF_SNAPSHOT', 'FIX2_SKIP_UNUSABLE_GPU_POLICY_PREPARATION', 'FIX3_ATOMIC_PRIMARY_FINISH_QUALIFICATION']
QUALIFICATION_PROOF_SCHEMA1977 = 'gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1'
QUALIFICATION_PREFERRED_SCHEMA1977 = 'gx1964-full-output-parallel-2wins5-v1-per-key-recovery-v1-strong-exact2-preferred-v1'
QUALIFICATION_EXACT_SCHEMA1977 = 'gx1977-exact-per-key-v1'
PRODUCTION = json.loads((ROOT / 'production1987.json').read_text())
GPU_ENTRY = 'ulike1960/runtime/libulike_gpu1960.so'
MOIRE_ENTRY = 'ulike1951/runtime/libulike_moire1951.so'
NR_ENTRY = 'ulike1955/runtime/libulike_nr1955.so'
NATIVE_ENTRIES = set()
NEW_JNI = []
NATIVE_JNI1978 = {}
INSTALLER = 'app/hiro/ulike/patches/IntegrationPayload186.class'
LOADER = 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'
CHANGED = {'META-INF/MANIFEST.MF', 'classes.dex', 'ulike/runtime.dex', LOADER}
ADDED = set()
REMOVED = []
PUBLISHER_FILES = ('publish1987.py', 'ulike1987-pipeline-progress-publish.yml', 'README.md')
TOOL_PINS = common.TOOL_PINS
require, sha, run, verify_jdk = common.require, common.sha, common.run, common.verify_jdk
archive, headers, manifest, own, write_zip = common.archive, common.headers, common.manifest, common.own, common.write_zip
from host_audit1987 import REQUIRED_FLAGS as HOST_FLAGS, CONTRACT as HOST_CONTRACT
ROUTE_POLICY1981 = dict(HOST_CONTRACT['route_policy1981'])
PRESERVATION_FIELDS = ('storage_codec_fence_inverse_verified', 'encoder_tail_release_positions_verified', 'face_success_completion_positions_verified', 'unchanged_pixel_kernel_methods_bytecode_identical', 'unmodified_pixel_kernel_helpers_bytecode_identical', 'quality_algorithm_and_settings_preserved', 'unrelated_runtime_classes_bytecode_identical', 'native_methods_byte_identical', 'native_methods_tsv_byte_identical', 'nonreviewed_runtime_methods_byte_identical1987', 'inherited_save_hook_contracts_preserved1982', 'save_helpers_byte_identical_to_baseline86', 'unmodified_gpu_runtime_helpers_bytecode_identical', 'camera_session_bytecode_identical', 'black_tap_disabled', 'capture_class_bytecode_identical', 'capture_begin_image_false_return_verified', 'timing_camera_trace_hooks_preserved')

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
    require(inventory['new_jni_methods'] == sorted(NEW_JNI) and inventory['new_native_methods'] == sorted(NEW_JNI) and inventory['changed_native_methods'] == [], 'Diagnostic update changed native declarations')
    require(inventory['replaced_helper_roots'] == sorted(PRODUCTION), 'Replaced camera helper inventory differs')
    require(inventory.get('retained_compiler_bridges1987') == [], 'Inherited compiler bridge inventory differs')
    require(inventory.get('diagnostic_measurement_and_logger_bodies_preserved') is True and inventory.get('save_hooks1981_reapplied') is False, 'Diagnostic edits or inherited save-hook reuse misreported')
    require(inventory.get('diagnostic_instrumentation_added1982') is True and not inventory['removed_runtime_methods'], 'Unreviewed runtime scope or removed public API')
    require(inventory['bytecode_patch_helper_roots'] == ['ProcessingTiming1947'], 'Inherited timing hook application inventory differs')
    require(type(inventory.get('preserved_runtime_class_count')) is int
        and inventory['preserved_runtime_class_count'] > 0, 'Whole-runtime preservation count absent')
    require(type(inventory.get('diagnostic_preserved_method_count')) is int
        and inventory['diagnostic_preserved_method_count'] > 0, 'Preserved diagnostic method inventory absent')

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
        'SingleResidual1961','CameraTrace1965','CameraSession1965','PreviewOutput1965','RenderStartup1938','PreviewLayout1922','LayoutLifecycle1937','FrontPreview1936','ColourCache1976','PairedRegions1976','ResidentProof1978']
    # Retained .86 helpers remain compiler-only unless explicitly instrumented.
    retained = list(dict.fromkeys(retained + json.loads((ROOT/'production1981.json').read_text())
        + json.loads((ROOT/'production1982.json').read_text())
        + json.loads((ROOT/'production1983.json').read_text())
        + json.loads((ROOT/'production1984.json').read_text())
        + json.loads((ROOT/'production1985.json').read_text())
        + json.loads((ROOT/'production1986.json').read_text())))
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
    require(declaration.get('schema') == 'ulike1987-build-declaration-v1'
        and declaration.get('reviewed_source_sha256') == current_pins
        and declaration.get('reviewed_publication_sha256') == current_publication_pins,
        'Reviewed source/publication graph changed before build')
    require(declaration.get('diagnostic_method_review_sha256') == sha((ROOT/'diagnostic_methods1987.json').read_bytes()), 'Diagnostic method declaration changed before build')
    require(declaration.get('production_helper_roots') == PRODUCTION, 'Reviewed camera production roots differ')
    pixel_reference = ROOT/'tests1987/source-scope86.json'
    require(not args.work.exists(), 'Fresh build directory required')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    source_scope = load_module('source_scope1987_build', ROOT/'source_scope1987.py').test(ROOT, args.work/'source-scope')
    pins = {BASE_SINGLE: BASE_SINGLE_SHA256, BASE_BUNDLE: BASE_BUNDLE_SHA256, **TOOL_PINS}
    for name, digest in pins.items():
        path = (args.tools if name.endswith('.jar') else args.input) / name
        require(sha(path.read_bytes()) == digest, 'Pinned input differs: ' + name)
    require((args.input / BASE_SINGLE).stat().st_size == BASE_SINGLE_BYTES
        and (args.input / BASE_BUNDLE).stat().st_size == BASE_BUNDLE_BYTES, 'Pinned baseline byte sizes differ')
    for filename,row in HISTORICAL_REGRESSION_INPUTS.items():
        historical = (args.input/filename).read_bytes()
        require(len(historical)==row['bytes'] and sha(historical)==row['sha256'], 'Pinned historical regression MPP differs: '+filename)
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
    # Java-only resource/scheduling update: preserve the exact shipped binary and its runtime
    # certificate identity. Recompiling would fold these Java-only edits into
    # the legacy global source fingerprint and make all saved proofs cold.
    native = baseline / GPU_ENTRY
    native_bytes = native.read_bytes()
    require(native_bytes[:6] == b'\x7fELF\x02\x01' and native_bytes[18:20] == b'\xb7\x00', 'Inherited ELF64 AArch64 GPU payload required')
    ndk_bin = args.ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin'
    elf = run([ndk_bin/'llvm-readelf', '-h', '-d', '-Ws', '--program-headers', '--wide', native], args.work/'gpu-native-readelf.txt')
    alignments = [int(line.split()[-1],0) for line in elf.splitlines() if line.strip().startswith('LOAD ')]
    needed = re.findall(r'\(NEEDED\).*?\[(.*?)\]', elf)
    require(alignments and all(x >= 16384 for x in alignments) and set(needed) <= {'libc.so','libm.so','libdl.so','libEGL.so','libGLESv3.so'}, 'GPU ELF alignment/dependencies differ')
    native_sources = {p.relative_to(ROOT/'native1960').as_posix():sha(p.read_bytes()) for p in sorted((ROOT/'native1960').rglob('*')) if p.is_file() and p.suffix in ('.c','.h','.comp','.py')}
    native_report = {'library':native.name, 'sha256':sha(native_bytes), 'bytes':len(native_bytes), 'ndk_revision':'27.2.12479018', 'build_action':'inherited_byte_identical_from_1.9.86', 'native_abi':19601, 'abi':'arm64-v8a', 'physical_android_tested':False, 'sources':native_sources, 'jni_exports':sorted(set(re.findall(r'\bJava_[A-Za-z0-9_]+',elf))), 'load_segment_alignments':alignments, 'needed_libraries':needed}
    native_payloads, native_reports = {}, {}

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
    transformers = ['MergePayloads', 'Transform1987', 'TimingCameraHooks1980', 'VerifyHelperReferences','VerifyAllHelperReferences1980']
    transformer_sources = [ROOT / (name + '.java') for name in transformers] + [ROOT / 'PatchClass1987.java']
    transformer_pins = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in transformer_sources}
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', classes, *[ROOT / (name + '.java') for name in transformers]], args.work / 'transform-javac.log')
    exports = ['--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED', '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']
    run(['javac', '-encoding', 'UTF-8', *exports, '-cp', cp, '-d', classes, ROOT / 'PatchClass1987.java'], args.work / 'metadata-javac.log')
    instrumented = json.loads((ROOT/'diagnostic_methods1987.json').read_text())
    require(instrumented.get('schema') == 'ulike1987-instrumented-methods-v1' and instrumented.get('review_complete') is True, 'Diagnostic method review incomplete')
    require(instrumented.get('reviewed_production_sha256') == compiled_sources,
        'Compiled production sources differ from the completed method review')
    instrumented_methods = args.work/'instrumented-methods.tsv'
    instrumented_methods.write_text(''.join(f'{owner}\t{name}\n' for owner,names in sorted(instrumented['methods'].items()) for name in sorted(names)))
    properties = ['-Dulike.production=' + ','.join(PRODUCTION), '-Dulike.newjni=', '-Dulike.instrumented=' + str(instrumented_methods)]
    for name in ('emitted', 'repeat'):
        run(['java', *properties, '-cp', cp, 'Transform1987', baseline, dex / 'classes.dex', bundle_dex, args.work / name, args.work / (name + '-audit.tsv')], args.work / (name + '.log'))
    emitted = args.work / 'emitted'
    require({p.name: p.read_bytes() for p in emitted.iterdir()} == {p.name: p.read_bytes() for p in (args.work / 'repeat').iterdir()}, 'Non-deterministic camera DEX transform')
    inventory = json.loads((emitted / 'whole-audit-inventory1987.json').read_text())
    inventory_checks(inventory)
    require(inventory.get('test_fixture_classes_absent_from_runtime') is True, 'Compile/test fixtures leaked into runtime DEX')
    run(['java', *exports, '-cp', cp, 'PatchClass1987', baseline / LOADER, emitted / 'UlikeHqMaxPatch.class'], args.work / 'metadata.log')
    refs = run(['java', '-cp', cp, 'VerifyHelperReferences', baseline / 'ulike/methods.dex', baseline / 'ulike/runtime.dex', baseline / 'ulike/methods.dex', emitted / 'runtime.dex'], args.work / 'helper-references.txt')
    all_refs = run(['java', '-cp', cp, 'VerifyAllHelperReferences1980', baseline/'ulike/methods.dex', baseline/'ulike/runtime.dex', emitted/'methods.dex', emitted/'runtime.dex'], args.work/'all-helper-references.txt')
    require(refs.startswith('PASS helper references in '), 'Fresh UI/logging helper linkage proof absent')
    final, integrated = dict(base), dict(bundle)
    for name, data in {'ulike/runtime.dex': (emitted / 'runtime.dex').read_bytes(), LOADER: (emitted / 'UlikeHqMaxPatch.class').read_bytes()}.items():
        final[name] = data
        integrated[name] = data
    final['classes.dex'] = (emitted / 'loader.dex').read_bytes()
    integrated['classes.dex'] = (emitted / 'bundle-loader.dex').read_bytes()
    fields = headers(base['META-INF/MANIFEST.MF'])
    fields.update(Version=VERSION, Description='Reduce redundant model source reads and unusable GPU policy setup, and reserve primary Finish qualification before snapshot copying; .86 pixel arithmetic, resolution, native identity and saved exact/speed proofs preserved; device speedup unmeasured.')
    final['META-INF/MANIFEST.MF'] = manifest(fields)
    fields = headers(bundle['META-INF/MANIFEST.MF'])
    fields.update(Version=BUNDLE_VERSION, Description='ULike .87 one-pass model qualification snapshots, early correction policy dispatch and primary Finish reservation; .86 image math, native identity and other apps preserved; device speedup unmeasured.')
    integrated['META-INF/MANIFEST.MF'] = manifest(fields)
    single_delta, bundle_delta = resource_delta(base, final, 'standalone'), resource_delta(bundle, integrated, 'bundle')
    require({n: b for n, b in final.items() if own(n)} == {n: b for n, b in integrated.items() if own(n)}, 'Current ULike copies differ')
    require(all(integrated[n] == b for n, b in bundle.items() if not own(n) and n not in ('classes.dex', 'META-INF/MANIFEST.MF')), 'Unrelated app resource changed')
    inherited = {n: {'sha256': sha(b), 'bytes': len(b)} for n, b in base.items() if (n.endswith('.so') or n == 'ulike186/runtime/0000.bin') and n not in NATIVE_ENTRIES}
    require(len(inherited) == 12 and all(final[n] == base[n] for n in inherited), 'All twelve native payloads must remain byte-identical')
    require(final[INSTALLER] == base[INSTALLER] and integrated[INSTALLER] == bundle[INSTALLER], 'All installer bytecode and its twelve rows must be unchanged')
    (args.work/'native-installer-metadata.log').write_text('PASS all 12 native payloads and JVM/DEX installer byte-identical to pinned .86; no native rebuild, JNI addition, or installer row update\n')
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
    os.environ['ULIKE1973_BASELINE_MPP'] = str(args.input/'ULike_HQ_Texture_Online_v1.9.80.mpp')
    os.environ['ULIKE1973_MORPHE_JAR'] = str(args.tools / 'morphe.jar')
    os.environ['ULIKE_MORPHE_JAR'] = str(args.tools / 'morphe.jar')
    os.environ['ULIKE_ANDROID_JAR'] = str(args.tools / 'android.jar')
    host = load_module('host_audit1987_build', ROOT / 'host_audit1987.py')
    result = host.test(ROOT, args.work / 'camera-host', jdk=args.jdk, ndk=args.ndk)
    require(isinstance(result, dict) and result.get('status') == 'passed'
        and type(result.get('assertions')) is int and result['assertions'] > 0, 'Fresh whole-app audit and retained quality/camera host evidence required')
    require(current_pins == source_pins() and current_publication_pins == publication_pins(), 'Reviewed source/publication graph changed during build/test')
    artifacts = {name: {'sha256': sha((args.output / name).read_bytes()), 'bytes': (args.output / name).stat().st_size} for name in (SINGLE, BUNDLE)}
    original_application = application_evidence1987.read(ROOT, artifacts)
    qa = {**inventory, 'schema': 'ulike1987-pipeline-progress-v1',
        'status': 'MPP_GPU_PIPELINE_PROGRESS87_HOST_AND_ORIGINAL_APKS_VERIFIED_DEVICE_UNVERIFIED',
        'ulike_version': VERSION, 'bundle_version': BUNDLE_VERSION, 'baseline_ulike_version': BASE_VERSION,
        'baseline_bundle_version': BASE_BUNDLE_VERSION, 'historical_regression_inputs': HISTORICAL_REGRESSION_INPUTS, 'selected_candidates': SELECTED, 'input_sha256': pins,
        'qualification_proof_schema1977': QUALIFICATION_PROOF_SCHEMA1977,
        'qualification_preferred_schema1977': QUALIFICATION_PREFERRED_SCHEMA1977,
        'exact_schema1977': QUALIFICATION_EXACT_SCHEMA1977,
        'artifacts': artifacts, 'jdk_identity': identity, 'approved_lineage': 'ULike1.8.8',
        'strong_gpu_host_evidence': result, 'strong_gpu_host_evidence_sha256': sha(json.dumps(result, sort_keys=True).encode()),
        'host_quality_passed': True, 'unmodified_pixel_kernels_identical_to_baseline': True,
        'intentional_quality_algorithm_change': False, 'capture_fusion_enabled': False,
        'production_source_consistency_verified': True, 'compiled_production_source_sha256': compiled_sources,
        'compiled_compile_only_source_sha256': compile_only_pins, 'compiled_transformer_source_sha256': transformer_pins,
        'executed_host_source_sha256': current_pins, 'inherited_native_payloads': inherited,
        'inherited_native_libraries_byte_identical': True, 'native_library_byte_identical': True,
        'native_installer_byte_identical': True, 'native_installer_inverse_verified': True,
        'native_installer_existing_rows_preserved': True, 'native_installer_existing_row_count_preserved': True,
        'native_installer_preserved_rows': 12, 'native_installer_updated_rows': 0, 'native_installer_appended_rows': 0,
        'native_installer_total_rows': 12, 'new_native_methods': sorted(NEW_JNI), 'new_jni_methods': sorted(NEW_JNI), 'changed_native_methods': [],
        'non_ulike_loader_classes_unchanged': True, 'non_ulike_resources_byte_identical': True,
        'standalone_and_bundle_ulike_resources_identical': True, 'resolution_and_save_format_preserved': True,
        'save_publication_hooks_preserved': True, 'save_publication_contract_preserved': True,
        'save_format_and_codec_configuration_preserved': True, 'inherited_host_validation_source_version': BASE_VERSION,
        'stage_timing_algorithm_preserved': True, 'diagnostic_stage_names': ['fusion', 'noise', 'correction', 'compression', 'save'],
        'unmodified_gpu_shader_and_cpu_sources_byte_identical': False, 'gpu_arithmetic_implementation_changed': False, 'gpu_pixel_source_reference_sha256': sha(pixel_reference.read_bytes()),
        'inherited_gpu_native_source_sha256': native_sources, 'rebuilt_native_payloads': native_reports, 'gpu_native_build': native_report, 'gpu_native_jni_exports': native_report['jni_exports'],
        'gpu_native_load_segment_alignments': alignments, 'gpu_native_needed_libraries': needed,
        'gpu_native_library_sha256': sha(native_bytes), 'gpu_native_library_bytes': len(native_bytes),
        'fresh_whole_app_audit_in_this_release': True, 'inherited_gpu_validation': {'fresh_gpu_execution_in_this_release': False},
        'persistent_rolling_across_restarts': True, 'anomaly_snapshots_protected': True,
        'user_incident_snapshot': True, 'logging_async_bounded': True,
        'camera_visible_preview_verified_on_device': False, 'camera_control_lifecycle_bytecode_identical': True,
        'native_memory_admission_regressions_passed': result.get('native_memory_admission_regressions_passed') is True,
        'native_failure_diagnostics_regressions_passed': result.get('native_failure_diagnostics_regressions_passed') is True,
        'strong_failure_diagnostics_regressions_passed': result.get('strong_failure_diagnostics_regressions_passed') is True,
        'qualification_certificate_regressions_passed': result.get('qualification_certificate_regressions_passed') is True,
        'backend_diagnostics_regressions_passed': result.get('backend_diagnostics_regressions_passed') is True,
        'strong_preference_telemetry_regressions_passed': result.get('strong_preference_telemetry_regressions_passed') is True,
        'unmodified_gpu_runtime_helpers_bytecode_identical': inventory.get('unmodified_gpu_runtime_helpers_bytecode_identical') is True,
        'gpu_safety_gates_preserved': result.get('gpu_safety_gates_preserved') is True, 'camera_trace_log_data_excluded': True,
        **ROUTE_POLICY1981,
        'strong_new_gpu_program_ids': [], 'legacy_gpu_program_ids': list(range(51)),
        'source_scope_evidence': source_scope,
        'inherited_numerical_validation1978': INHERITED_NUMERICAL_VALIDATION1978,
        'source_scope_evidence_sha256': sha(json.dumps(source_scope,sort_keys=True).encode()),
        **{key:source_scope[key] for key in ('camera_trace_version_only_preserved','camera_trace_logging_methods_preserved','timing_version_only_preserved','diagnostic_instrumentation_added1982','reviewed_certificate_fixture_bytes_verified','unmodified_cpu_and_other_shader_sources','unmodified_native_shader_sources_byte_identical','unmodified_java_sources_byte_identical','reviewed_native_scope_verified')},
        'inherited_logging_validation': {'version': BASE_VERSION, 'fresh_logger_regressions_in_this_release': False}, 'fresh_diagnostic_regressions_in_this_release': True,
        'original_apk_apply_tested': True, 'original_split_merge_tested': True, 'ci_android_apply_tested': False, 'original_application': original_application,
        'cpu_cache_qualification_requires_gpu_environment_identity': True, 'device_tested': False, 'device_quality_verified': False, 'device_save_speed_measured': False,
        'gpu_execution_on_physical_android': False, 'all_processing_on_gpu': False,
        'gx9_beauty_interop_supported': False, 'gx10_encoder_interop_supported': False,
        'changed_standalone_entries': single_delta[0], 'added_standalone_entries': single_delta[1],
        'changed_bundle_entries': bundle_delta[0], 'added_bundle_entries': bundle_delta[1],
        'save_encoding_helpers_byte_identical': True, 'runtime_all_helper_references_verified': True,
        'published': False, 'manager_feed_updated': False}
    for key in HOST_FLAGS:
        require(result.get(key) is True, 'Fresh current regression absent: '+key)
        qa[key] = True
    (args.output / 'QA_ULike_v1.9.87.json').write_bytes(json_bytes(qa))
    (args.output / 'host-whole-audit1987-result.json').write_bytes(json_bytes(result))
    for name in ('emitted-audit.tsv', 'all-helper-references.txt', 'helper-references.txt', 'emitted.log', 'metadata.log', 'gpu-native-readelf.txt', 'native-installer-metadata.log'):
        shutil.copyfile(args.work / name, args.output / name)
    shutil.copyfile(emitted / 'whole-audit-inventory1987.json', args.output / 'whole-audit-inventory1987.json')
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

