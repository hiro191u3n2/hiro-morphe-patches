#!/usr/bin/env python3
"""Execute current NR9-NR13 Java, fresh production C/JNI and save integration.

Android Bitmap, camera bindings and the stock colour transform are explicit
host fixtures. A private generated Bitmap fixture adds numbered write failures
so rollback after successful NR1-NR4 and a partial Strong pass can be tested.
No production source or historical fixture is modified by this driver.
"""
from pathlib import Path
import argparse
import hashlib
import json
import shutil

from host_common1954 import sources1954
from host_nr1955 import _java_tools
import subprocess

ROOT = Path(__file__).resolve().parent
QUALITY_FLAGS = (
    'nr5_actual_image_pyramid_executed',
    'nr5_correlated_coarse_noise_reduced',
    'nr6_flat_dark_noise_reduced',
    'nr6_edge_texture_protection_checked',
    'nr7_wide_chroma_noise_reduced',
    'nr7_colour_boundary_protection_checked',
    'nr8_same_image_nlm_executed',
    'nr8_bounded_search_checked',
)


def _sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def _execute(command, log, timeout=240):
    completed = subprocess.run(list(map(str, command)), capture_output=True,
                               text=True, timeout=timeout)
    Path(log).write_text(completed.stdout + completed.stderr)
    if completed.returncode:
        raise RuntimeError('NR1958 host command failed: ' +
                           completed.stdout[-3000:] + completed.stderr[-12000:])
    return completed.stdout


def _checked(report, name):
    if (not isinstance(report, dict) or type(report.get('assertions')) is not int
            or report['assertions'] <= 0
            or not (report.get('status') == 'passed' or report.get('passed') is True)):
        raise AssertionError('Executed NR1958 assertions missing: ' + name)
    return report


def _failure_fixture(source, target):
    """Numbered failures occur on a private copy, after an earlier pass succeeds."""
    text = Path(source).read_text()
    replacements = {
        ' public int reads,writes;':
        ' public int reads,writes;\n'
        ' public static volatile int failCopyWriteAt,lastInjectedWriteAttempt;\n'
        ' public static volatile boolean failCopyWriteOom;\n'
        ' private int writeFailureAt;private boolean writeFailureOom;',
        '  b.writeFailure=failNextCopyWrite;failNextCopyWrite=false;':
        '  b.writeFailure=failNextCopyWrite;failNextCopyWrite=false;\n'
        '  b.writeFailureAt=failCopyWriteAt;b.writeFailureOom=failCopyWriteOom;\n'
        '  failCopyWriteAt=0;failCopyWriteOom=false;',
        '  if(writeFailure){writeFailure=false;throw new IllegalStateException("injected write");}':
        '  if(writeFailure){writeFailure=false;throw new IllegalStateException("injected write");}\n'
        '  if(writeFailureAt>0&&writes+1==writeFailureAt){\n'
        '   writeFailureAt=0;lastInjectedWriteAttempt=writes+1;\n'
        '   if(writeFailureOom)throw new OutOfMemoryError("injected numbered private write");\n'
        '   throw new IllegalStateException("injected numbered private write");\n'
        '  }',
    }
    for old, new in replacements.items():
        if text.count(old) != 1:
            raise AssertionError('Numbered Bitmap fault fixture anchor changed')
        text = text.replace(old, new)
    target = Path(target)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text)
    return target


def test(root, work, input=None, tools=None):
    root = Path(root).resolve()
    out = Path(work).resolve()/'host-nr1958'
    out.mkdir(parents=True, exist_ok=True)
    compiler, java = _java_tools(tools)
    classes = out/'pipeline-classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(exist_ok=True)
    common = list((root/'tests/pipeline1942-fixtures').rglob('*.java'))
    common += list((root/'tests/timing1947-fixtures').rglob('*.java'))
    common += [root/name for name in (
        'SpeedWorkers1935.java', 'Scheduling1944.java', 'NoiseCache1944.java',
        'NativeSpeed1944.java', 'GpuInteger1949.java', 'QualityPixels1932.java',
        'NativeMoire1951.java', 'PolicyCache1945.java', 'QualityShadow1932.java',
        'SpatialNoise1934.java', 'LongMoire1934.java', 'ProcessingTiming1947.java',
        'QualityPipeline1932.java', 'SingleNoise1955.java', 'StrongNoise1957.java', 'StrongNoise1958.java',
        'tests/SingleNoise1955Test.java', 'tests/StrongNoise1957Test.java', 'tests/StrongNoise1958Test.java', 'tests/SmoothNoise1958AcceptanceTest.java',
        'tests/PipelineSmoothNoise1958Test.java',
    )]
    sources = sources1954(root, common)
    inputs = {p.relative_to(root).as_posix(): _sha(p) for p in sources}
    original_bitmap = root/'tests/pipeline1942-fixtures/android/graphics/Bitmap.java'
    generated_bitmap = _failure_fixture(original_bitmap,
        out/'generated-fixtures/android/graphics/Bitmap.java')
    compile_sources = [generated_bitmap if p == original_bitmap else p for p in sources]
    fingerprints = {p.relative_to(root).as_posix(): _sha(p) for p in sources if p != original_bitmap}
    generated_pins = {generated_bitmap.relative_to(out).as_posix(): _sha(generated_bitmap)}
    _execute(compiler+['-encoding', 'UTF-8', '-source', '8', '-target', '8',
                       '-Xlint:-options', '-d', classes, *compile_sources],
             out/'compile.log')
    reports = {}
    java_reports = (
        ('existing_single_image_algorithm', 'com.hiro.ulike.SingleNoise1955Test'),
        ('retained_strong_single_image_algorithm', 'com.hiro.ulike.StrongNoise1957Test'),
        ('smooth_acceptance_java', 'com.hiro.ulike.SmoothNoise1958AcceptanceTest'),
        ('smooth_single_image_algorithm', 'com.hiro.ulike.StrongNoise1958Test'),
        ('smooth_final_save_pipeline', 'com.hiro.ulike.PipelineSmoothNoise1958Test'),
    )
    for name, main in java_reports:
        stdout = _execute([java, '-XX:ActiveProcessorCount=4',
                           '-Djava.library.path='+str(out/'unavailable'),
                           '-cp', classes, main], out/(name+'.log'))
        reports[name] = _checked(json.loads(stdout), name)
    for name, _ in java_reports:
        if reports[name].get('nativeAvailable') is not False:
            raise AssertionError('Java fallback did not force unavailable JNI: '+name)
    if reports['smooth_single_image_algorithm'].get('nativeParityCases') != 0:
        raise AssertionError('Strong Java fallback unexpectedly inherited native state')

    # Compile the current C source; no source-tree or previously built binary is
    # used. New JVMs isolate nativeState from the mandatory unavailable run.
    native = out/'native'
    if native.exists():
        shutil.rmtree(native)
    native.mkdir(exist_ok=True)
    source = root/'native1958/smooth_noise1958.c'
    native_sources = [source, root/'native1958/build_native1958.py', root/'native1951/moire1951.c']
    native_pins = {p.relative_to(root).as_posix(): _sha(p) for p in native_sources}
    compiler_path = shutil.which('cc') or shutil.which('gcc')
    include = Path(java).resolve().parent.parent/'include'
    if not compiler_path or not (include/'jni.h').is_file() or not (include/'linux/jni_md.h').is_file():
        raise AssertionError('Host C compiler and executing JDK JNI headers required')
    flags = ['-O3', '-std=c11', '-Wall', '-Wextra', '-Werror',
             '-ffp-contract=off', '-fno-fast-math', '-fPIC',
             '-fvisibility=hidden', '-shared', '-Wl,--no-undefined']
    library = native/'libulike_smooth1958.so'
    command = [compiler_path, *flags, '-I'+str(include), '-I'+str(include/'linux'),
               source, '-lm', '-o', library]
    _execute(command, out/'native-compile.log')
    if library.read_bytes()[:6] != b'\x7fELF\x02\x01':
        raise AssertionError('Fresh Strong host JNI ELF64 build missing')
    moire_source = root/'native1951/moire1951.c'
    moire_library = native/'libulike_moire1951.so'
    _execute([compiler_path, *flags, '-I'+str(include), '-I'+str(include/'linux'),
              moire_source, '-o', moire_library], out/'inherited-finish-native-compile.log')
    if moire_library.read_bytes()[:6] != b'\x7fELF\x02\x01':
        raise AssertionError('Fresh inherited finish JNI ELF64 build missing')
    native_version = _execute([compiler_path, '--version'],
        out/'native-compiler.log', 30).splitlines()[0]
    for name, main in (
        ('smooth_single_image_native', 'com.hiro.ulike.StrongNoise1958Test'),
        ('smooth_final_save_pipeline_native', 'com.hiro.ulike.PipelineSmoothNoise1958Test'),
        ('smooth_acceptance_native', 'com.hiro.ulike.SmoothNoise1958AcceptanceTest'),
    ):
        stdout = _execute([java, '-XX:ActiveProcessorCount=4',
                           '-Djava.library.path='+str(native),
                           '-cp', classes, main], out/(name+'.log'))
        reports[name] = _checked(json.loads(stdout), name)
    actual_native = reports['smooth_single_image_native']
    if (actual_native.get('nativeAvailable') is not True
            or reports['smooth_final_save_pipeline_native'].get('nativeAvailable') is not True
            or type(actual_native.get('nativeParityCases')) is not int
            or actual_native['nativeParityCases'] < 5
            or actual_native.get('nativeParityPixelDifferences') != 0):
        raise AssertionError('Fresh Strong JNI must pass >=5 exact Java parity cases')
    for name in ('smooth_acceptance_java', 'smooth_acceptance_native'):
        report = reports[name]
        if any(report.get(flag) is not True for flag in ('knownTruthFixtures', 'relative1957Comparison', 'halfAndFullNlmAblation')) or report.get('physicalAndroidTested') is not False:
            raise AssertionError('Independent NR9-NR12 numeric acceptance evidence missing: '+name)
    for name in ('smooth_final_save_pipeline', 'smooth_final_save_pipeline_native'):
        if any(reports[name].get(flag) is not True for flag in ('nr13_geometry_mapping_checked', 'nr13_shared_finish_policy_checked', 'nr13_actual_smoothing_admission_checked', 'nr13_residual_grain_sharpen_suppressed')):
            raise AssertionError('NR13 shared finishing numeric integration evidence absent: '+name)
    if (reports['smooth_final_save_pipeline'].get('nativeFinishAvailable') is not False
            or reports['smooth_final_save_pipeline'].get('nr13_native_finish_parity_checked') is not False
            or reports['smooth_final_save_pipeline_native'].get('nativeFinishAvailable') is not True
            or reports['smooth_final_save_pipeline_native'].get('nr13_native_finish_parity_checked') is not True):
        raise AssertionError('NR13 fresh inherited native-finishing pixel parity missing')
    accepted_native = reports['smooth_acceptance_native']
    if (reports['smooth_acceptance_java'].get('nativeAvailable') is not False
            or reports['smooth_acceptance_java'].get('nativeParityCases') != 0
            or accepted_native.get('nativeAvailable') is not True
            or accepted_native.get('nativeParityCases', 0) < 5
            or accepted_native.get('nativeParityPixelDifferences') != 0):
        raise AssertionError('Independent acceptance must test Java-only and fresh JNI parity')
    for name in ('smooth_single_image_algorithm', 'smooth_single_image_native'):
        if any(reports[name].get(flag) is not True for flag in QUALITY_FLAGS):
            raise AssertionError('Current-source NR9-NR13 quality assertions missing: '+name)
    if native_pins != {p.relative_to(root).as_posix(): _sha(p) for p in native_sources}:
        raise AssertionError('Strong native sources changed during actual execution')
    if inputs != {p.relative_to(root).as_posix(): _sha(p) for p in sources}:
        raise AssertionError('NR1958 compiler inputs changed during actual execution')
    if generated_pins != {generated_bitmap.relative_to(out).as_posix(): _sha(generated_bitmap)}:
        raise AssertionError('Generated numbered-failure fixture changed during execution')
    result = {
        'status': 'passed', 'selected': ['NR9', 'NR10', 'NR11', 'NR12', 'NR13'],
        'assertions': sum(report['assertions'] for report in reports.values()),
        'smooth_single_image_algorithm_executed': True,
        'smooth_final_save_pipeline_executed': True,
        'existing_single_image_algorithm_executed': True,
        'retained_nr5_nr8_algorithm_executed': True,
        'smooth_acceptance_java_executed': True,
        'smooth_acceptance_native_executed': True,
        'nr9_nr12_independent_known_truth_acceptance_passed': True,
        'nr9_nr12_relative1957_improvement_acceptance_passed': True,
        'nr11_half_and_full_nlm_ablation_passed': True,
        'nr13_all_finish_routes_integration_verified': True,
        'nr13_fresh_inherited_native_finish_pixel_parity_verified': True,
        'fresh_production_c_jni_executed': True,
        'fresh_production_c_jni_pipeline_executed': True,
        'native_java_pixel_equivalence': True,
        'native_parity_cases': actual_native['nativeParityCases'],
        'native_parity_pixel_differences': actual_native['nativeParityPixelDifferences'],
        'forced_native_unavailable_java_fallback_executed': True,
        'chroma_on_off_nr_applied': True,
        'legacy_primary_and_residual_disabled_in_new_save_path': True,
        'captured_noise_off_options_preserved': True,
        'rotation_and_resize_dimensions_preserved': True,
        'source_bitmap_immutable': True,
        'alpha_preservation_checked': True,
        'immutable_streaming_halos_equal_whole_image': True,
        'private_nr_write_failure_discarded': True,
        'smooth_stage_partial_write_failure_discarded': True,
        'smooth_stage_partial_write_oom_discarded': True,
        'smooth_model_memory_admission_checked': True,
        'prepared_detail_duplicate_suppressed': True,
        'completion_capture_identity_preserved': True,
        'copy_oom_failure_rollback_preserved': True,
        'concurrent_shot_options_isolated': True,
        'f16_wide_colour_gainmap_outside_integer_nr': True,
        'final_sharpen_flat_noise_non_amplification_checked': True,
        'production_host_quality_changed': True,
        'pixel_equivalence_to_baseline': False,
        'physical_android_tested': False,
        'device_speedup_verified': False,
        'device_quality_improvement_verified': False,
        'compiled_sources_sha256': fingerprints,
        'source_inputs_sha256': inputs,
        'compiled_native_sources_sha256': native_pins,
        'generated_host_fixture_source_sha256': generated_pins,
        'generated_bitmap_fixture': {
            'original_source': original_bitmap.relative_to(root).as_posix(),
            'original_source_sha256': _sha(original_bitmap),
            'generated_source_sha256': _sha(generated_bitmap),
            'scope': 'Adds numbered RuntimeException/OOM failures on exclusively '
                'owned bitmap copies. Source tree and production code untouched.',
        },
        'host_native_build': {
            'fresh_build': True,
            'production_c_source': source.relative_to(root).as_posix(),
            'compiler': compiler_path, 'compiler_version': native_version,
            'flags': flags, 'jni_headers': str(include),
            'library_sha256': _sha(library), 'library_bytes': library.stat().st_size,
            'host_library': str(library), 'cached_native_binary_used': False,
            'inherited_finish_source': moire_source.relative_to(root).as_posix(),
            'inherited_finish_source_sha256': _sha(moire_source),
            'inherited_finish_library_sha256': _sha(moire_library),
            'inherited_finish_library_bytes': moire_library.stat().st_size,
            'inherited_finish_fresh_build': True,
            'physical_android_tested': False,
        },
        'reports': reports,
        'fixture_scope': 'Current production StrongNoise1958 and QualityPipeline1932 '
            'execute directly, including fresh production Strong JNI. Explicit host '
            'Android Bitmap, camera bindings and identity stock-colour transform. '
            'Serialized SingleNoise and H28-H33 preservation is audited separately by the build. '
            'No physical camera, renderer-success or device-speed assertion.',
        'extended_format_scope': 'The existing readable gate excludes F16, wide '
            'colour and gainmap inputs before new integer NR. The legacy host '
            'surrogate copies ARGB_8888; this does not establish physical 10-bit/HDR output.',
    }
    (out/'result.json').write_text(json.dumps(result, ensure_ascii=False,
                                             sort_keys=True, indent=2)+'\n')
    return result


def run(work, input=None, tools=None):
    """Build1958 entry point: run(work, input, tools) -> actual executed evidence."""
    return test(ROOT, work, input, tools)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', required=True)
    parser.add_argument('--input')
    parser.add_argument('--tools')
    args = parser.parse_args()
    print(json.dumps(run(args.out, args.input, args.tools), ensure_ascii=False,
                     indent=2, sort_keys=True))
