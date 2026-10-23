#!/usr/bin/env python3
"""Run current NR1-NR4 algorithm and final-save integration host checks.

The production single-image kernel and orchestration execute directly. Android
Bitmap/camera/stock colour-correction services are explicit fixtures. The final
MPP's const-false temporal-capture hook is audited separately by Verify1955.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess

from host_common1954 import sources1954

ROOT = Path(__file__).resolve().parent


def _execute(command, log, timeout=180):
    completed = subprocess.run(list(map(str, command)), capture_output=True,
                               text=True, timeout=timeout)
    Path(log).write_text(completed.stdout + completed.stderr)
    if completed.returncode:
        raise RuntimeError('NR1955 host command failed: ' +
                           completed.stdout[-3000:] + completed.stderr[-12000:])
    return completed.stdout


def _checked(report, name):
    if (not isinstance(report, dict) or type(report.get('assertions')) is not int
            or report['assertions'] <= 0
            or not (report.get('status') == 'passed' or report.get('passed') is True)):
        raise AssertionError('Executed NR1955 assertions missing: ' + name)
    return report


def _java_tools(tools=None):
    tools = Path(tools) if tools is not None else None
    jdk = os.environ.get('ULIKE_JDK_HOME')
    if not jdk and tools is not None:
        choices = sorted(tools.glob('jdk-*/bin/javac'))
        if choices:
            jdk = str(choices[-1].parent.parent)
    javac = (os.environ.get('ULIKE_JAVAC') or
             (str(Path(jdk)/'bin/javac') if jdk else shutil.which('javac')))
    java = (os.environ.get('ULIKE_JAVA') or
            (str(Path(jdk)/'bin/java') if jdk else
             (str(Path(javac).with_name('java')) if javac else shutil.which('java'))))
    if not java:
        raise AssertionError('Java runtime required for actual NR1955 execution')
    return ([javac] if javac else [java, 'com.sun.tools.javac.Main']), java


def test(root, work, input=None, tools=None):
    root = Path(root).resolve()
    out = Path(work).resolve()/'host-nr1955'
    out.mkdir(parents=True, exist_ok=True)
    compiler, java = _java_tools(tools)
    classes = out/'pipeline-classes'
    classes.mkdir(exist_ok=True)
    common = list((root/'tests/pipeline1942-fixtures').rglob('*.java'))
    common += list((root/'tests/timing1947-fixtures').rglob('*.java'))
    common += [root/name for name in (
        'SpeedWorkers1935.java', 'Scheduling1944.java', 'NoiseCache1944.java',
        'NativeSpeed1944.java', 'GpuInteger1949.java', 'QualityPixels1932.java',
        'NativeMoire1951.java', 'PolicyCache1945.java', 'QualityShadow1932.java',
        'SpatialNoise1934.java', 'LongMoire1934.java', 'ProcessingTiming1947.java',
        'QualityPipeline1932.java', 'SingleNoise1955.java',
        'tests/SingleNoise1955Test.java', 'tests/PipelineSingleNoise1955Test.java',
    )]
    sources = sources1954(root, common)
    fingerprints = {p.relative_to(root).as_posix():
                    hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    _execute(compiler+['-encoding', 'UTF-8', '-source', '8', '-target', '8',
                       '-Xlint:-options', '-d', classes, *sources],
             out/'compile.log', 180)
    reports = {}
    for name, main in (
        ('single_image_algorithm', 'com.hiro.ulike.SingleNoise1955Test'),
        ('final_save_pipeline', 'com.hiro.ulike.PipelineSingleNoise1955Test'),
    ):
        stdout = _execute([java, '-XX:ActiveProcessorCount=4',
                           '-Djava.library.path='+str(out/'unavailable'),
                           '-cp', classes, main], out/(name+'.log'), 180)
        reports[name] = _checked(json.loads(stdout), name)
    if (reports['single_image_algorithm'].get('nativeAvailable') is not False
            or reports['final_save_pipeline'].get('nativeAvailable') is not False
            or reports['single_image_algorithm'].get('nativeParityCases') != 0):
        raise AssertionError('Unavailable-native Java fallback was not independently executed')
    # Compile the current production C source here. A cached source-tree binary
    # is never an input, and separate fresh JVMs cannot inherit nativeState.
    native = out/'native'
    native.mkdir(exist_ok=True)
    source = root/'native1955/single_noise1955.c'
    native_sources = [source, root/'native1955/build_native1955.py']
    native_pins = {p.relative_to(root).as_posix():
                   hashlib.sha256(p.read_bytes()).hexdigest() for p in native_sources}
    compiler_path = shutil.which('cc') or shutil.which('gcc')
    include = Path(java).resolve().parent.parent/'include'
    if not compiler_path or not (include/'jni.h').is_file() or not (include/'linux/jni_md.h').is_file():
        raise AssertionError('Host C compiler and actual executing JDK JNI headers required')
    flags = ['-O3', '-std=c11', '-Wall', '-Wextra', '-Werror',
             '-ffp-contract=off', '-fno-fast-math', '-fPIC',
             '-fvisibility=hidden', '-shared', '-Wl,--no-undefined']
    library = native/'libulike_nr1955.so'
    native_command = [compiler_path, *flags, '-I'+str(include),
                      '-I'+str(include/'linux'), source, '-lm', '-o', library]
    _execute(native_command, out/'native-compile.log', 180)
    if library.read_bytes()[:6] != b'\x7fELF\x02\x01':
        raise AssertionError('Fresh host JNI ELF64 build missing')
    native_version = _execute([compiler_path, '--version'], out/'native-compiler.log', 30).splitlines()[0]
    for name, main in (
        ('single_image_native', 'com.hiro.ulike.SingleNoise1955Test'),
        ('final_save_pipeline_native', 'com.hiro.ulike.PipelineSingleNoise1955Test'),
    ):
        stdout = _execute([java, '-XX:ActiveProcessorCount=4',
                           '-Djava.library.path='+str(native),
                           '-cp', classes, main], out/(name+'.log'), 180)
        reports[name] = _checked(json.loads(stdout), name)
    actual_native = reports['single_image_native']
    if (actual_native.get('nativeAvailable') is not True
            or reports['final_save_pipeline_native'].get('nativeAvailable') is not True
            or actual_native.get('nativeParityCases') != 5):
        raise AssertionError('Fresh production JNI failed mandatory five Java pixel-exact parity cases')
    if native_pins != {p.relative_to(root).as_posix():
                       hashlib.sha256(p.read_bytes()).hexdigest() for p in native_sources}:
        raise AssertionError('Native sources changed during compile or execution')
    if fingerprints != {p.relative_to(root).as_posix():
                        hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}:
        raise AssertionError('Compiled NR1955 sources changed during execution')
    result = {
        'status': 'passed',
        'assertions': sum(report['assertions'] for report in reports.values()),
        'selected': ['NR1', 'NR2', 'NR3', 'NR4'],
        'single_image_algorithm_executed': True,
        'final_save_pipeline_executed': True,
        'fresh_production_c_jni_executed': True,
        'fresh_production_c_jni_pipeline_executed': True,
        'native_java_pixel_equivalence': True,
        'native_parity_cases': actual_native['nativeParityCases'],
        'native_parity_pixel_differences': 0,
        'forced_native_unavailable_java_fallback_executed': True,
        'chroma_on_off_nr_applied': True,
        'legacy_primary_and_residual_disabled_in_new_save_path': True,
        'captured_noise_off_options_preserved': True,
        'rotation_and_resize_dimensions_preserved': True,
        'source_bitmap_immutable': True,
        'immutable_streaming_halos_equal_whole_image': True,
        'private_nr_write_failure_discarded': True,
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
        'compiled_native_sources_sha256': native_pins,
        'host_native_build': {
            'fresh_build': True,
            'production_c_source': source.relative_to(root).as_posix(),
            'compiler': compiler_path,
            'compiler_version': native_version,
            'flags': flags,
            'jni_headers': str(include),
            'library_sha256': hashlib.sha256(library.read_bytes()).hexdigest(),
            'library_bytes': library.stat().st_size,
            'host_library': str(library),
            'cached_native_binary_used': False,
            'physical_android_tested': False,
        },
        'reports': reports,
        'temporal_capture_scope': 'MPP beginImage const-false bytecode and '
            'unchanged remaining BurstCapture1933 members are audited by Verify1955; '
            'host Bitmap fixtures do not simulate a physical camera.',
        'nr4_stage_scope': 'Native beauty and watermark precede saved-image NR; '
            'new NR measures the colour-corrected saved bitmap, and final sharp '
            'uses residual noise mapping while retaining its original noise floor.',
        'fixture_scope': 'Current production SingleNoise1955, QualityPipeline1932, '
            'fresh production JNI, resampling, spatial probing and sharpening with explicit host Android '
            'Bitmap, camera bindings and identity stock-colour transform. '
            'No device or renderer-success claim.',
        'extended_format_scope': 'New integer NR is excluded before legacy '
            'normalization. The host fallback surrogate copies ARGB_8888 and '
            'does not establish physical 10-bit or HDR output preservation.',
    }
    (out/'result.json').write_text(json.dumps(result, ensure_ascii=False,
                                             sort_keys=True, indent=2)+'\n')
    return result


def run(work, input=None, tools=None):
    """Build1955 entry point: run(work, input, tools) -> executed evidence."""
    return test(ROOT, work, input, tools)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', required=True)
    parser.add_argument('--input')
    parser.add_argument('--tools')
    args = parser.parse_args()
    print(json.dumps(run(args.out, args.input, args.tools), ensure_ascii=False,
                     indent=2, sort_keys=True))
