#!/usr/bin/env python3
"""Current production route differential; Android/GPU transport is explicit.

The numerical CPU and JNI sources are the actual release sources. A separately
compiled, hash-pinned .80 dispatch reproduces loss of the worker workspace.
No result from these host fixtures claims a device speedup or GPU shader proof.
"""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

REFERENCE_PIN = '24fe5f9966ae2b3557726b1c510e73a12d68921b900b2a068aad85012e19ff65'
BASE_MPP = '2be00d742db6a8954a08e0dc43c27b11e1fb2d20c864d09a77b8010bb9a1ea25'
REQUIRED_FLAGS = (
    'pipeline81_caller_workspace_cpu_fallback_exact',
    'pipeline81_baseline_cold_gpu_workspace_loss_reproduced',
    'pipeline81_identity_all_argb_exact',
    'pipeline81_original_halos_and_rounding_preserved',
    'pipeline81_single_stage_ownership_verified',
    'pipeline81_actual_gpu_stage_all_argb_exact',
    'pipeline81_residual_reuse_rawbits_exact',
    'pipeline81_exact2_speed_gate_preserved',
    'pipeline81_original_gpu_certificates_preserved',
    'pipeline81_independent_cold_stage_admission_verified',
    'pipeline81_cpu_profile_transitions_verified',
    'pipeline81_cpu_mismatch_is_exact_rejection_verified',
    'pipeline81_cpu_oracle_native_capacity_verified',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def run(command, log, timeout=240, expected=0, env=None):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=timeout, env=env)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode != expected:
        raise AssertionError(str(log) + '\n' + (result.stdout + result.stderr)[-16000:])
    return result.stdout


def compile_inputs(source):
    sys.path.insert(0, str(source))
    # This is a compiler closure, not the set of classes selected for DEX.
    builder = module('pipeline81_current_builder', source / 'build1980.py')
    inputs = list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION]
    for path in sorted(source.glob('*1981.java')):
        if path not in inputs and 'package com.hiro.ulike;' in path.read_text():
            inputs.append(path)
    return inputs


def reference(source):
    root = source / 'tests1981/pipeline80-reference'
    if sha(root / 'pins.json') != REFERENCE_PIN:
        raise AssertionError('Changed independent .80 dispatch manifest')
    pins = json.loads((root / 'pins.json').read_text())
    if pins['baseline_version'] != '1.9.80' or pins['baseline_mpp_sha256'] != BASE_MPP:
        raise AssertionError('Wrong independent .80 release')
    for name, pin in pins['files'].items():
        if sha(root / name) != pin['sha256'] or (root / name).stat().st_size != pin['bytes']:
            raise AssertionError('Changed .80 baseline ' + name)
    for name, pin in pins['unchanged_numeric_sources'].items():
        if sha(source / name) != pin:
            raise AssertionError('Pixel arithmetic changed from .80: ' + name)
    return root, pins


def native_stage(source, work, jdk, ndk, jar, classes, android_fixtures, numerical_library):
    work.mkdir(parents=True, exist_ok=True)
    native = source / 'native1960'
    builder = module('pipeline81_actual_glsl_builder', native / 'build_native1960.py')
    builder.shader_header(native, work)
    graphics = jdk.parent / 'host-graphics'
    env = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    library_folder = graphics / 'usr/lib/x86_64-linux-gnu'
    if library_folder.is_dir():
        for name in ('LIBRARY_PATH', 'LD_LIBRARY_PATH'):
            env[name] = str(library_folder) + (os.pathsep + env[name] if env.get(name) else '')
        if (library_folder / 'dri').is_dir():
            env['LIBGL_DRIVERS_PATH'] = str(library_folder / 'dri')
    includes = []
    if ndk:
        header_root = Path(ndk).resolve() / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
        for name in ('EGL', 'GLES3', 'KHR'):
            shutil.copytree(header_root / name, work / 'headers' / name, dirs_exist_ok=True)
        includes = ['-I' + str(work / 'headers')]
    elif (graphics / 'include').is_dir():
        includes = ['-I' + str(graphics / 'include')]
    native_sources = [native / 'engine1960.c', native / 'residual_blocks1961.c',
                      native / 'build_native1960.py', source / 'native1955/single_noise1955.c', *sorted(native.glob('*.comp'))]
    pins = {p.relative_to(source).as_posix(): sha(p) for p in native_sources}
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-DUSE_OZONE', '-fno-fast-math', '-ffp-contract=off',
         '-Wl,--no-undefined', *includes, '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
         '-I' + str(work), native / 'engine1960.c', native / 'residual_blocks1961.c',
         '-lm', '-pthread', '-l:libEGL.so.1', '-l:libGLESv2.so.2', '-o', work / 'libulike_gpu1960.so'],
        work / 'native-compile.log', env=env)
    shutil.copy2(numerical_library, work / numerical_library.name)
    fixture = work / 'fixtures'
    fixture.mkdir(exist_ok=True)
    files = [*android_fixtures, *[source / n for n in (
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java',
        'tests1981/SingleStageNativePeers1981.java', 'tests1981/SingleStageNative1981Test.java')]]
    fixture_pins = {str(p): sha(p) for p in files}
    cp = str(classes) + os.pathsep + str(jar)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', fixture, *files], work / 'fixture-compile.log')
    stdout = run([jdk / 'bin/java', '-ea', '-Xcheck:jni', '-Xmx1g', '-XX:ActiveProcessorCount=4',
                  '-Djava.library.path=' + str(work), '-cp', str(fixture) + os.pathsep + cp,
                  'com.hiro.ulike.SingleStageNative1981Test'], work / 'actual-gpu-stage.log', 600, env=env)
    report = json.loads(stdout.strip().splitlines()[-1])
    if report.get('status') != 'passed' or report.get('pixels', 0) < 50000:
        raise AssertionError('Incomplete actual GPU stage numerical coverage')
    if pins != {p.relative_to(source).as_posix(): sha(p) for p in native_sources} or fixture_pins != {str(p): sha(p) for p in files}:
        raise AssertionError('Native GPU stage source changed during execution')
    report.update(production_native_source_sha256=pins, fixture_sha256=fixture_pins,
                  native_library_sha256=sha(work / 'libulike_gpu1960.so'))
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', jdk.parent / 'android.jar')).resolve()
    if not jar.is_file():
        raise AssertionError('Set ULIKE_ANDROID_JAR to the pinned Android API jar')
    ref, pins = reference(source)
    inputs = compile_inputs(source)
    source_pins = {p.relative_to(source).as_posix(): sha(p) for p in inputs}
    classes, fixtures, old = (work / n for n in ('production-classes', 'fixture-classes', 'published80-classes'))
    for path in (classes, fixtures, old):
        path.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-bootclasspath', jar, '-d', classes, *inputs], work / 'production-compile.log')
    generated = work / 'generated'
    queue = module('pipeline81_android_fixtures', source / 'host_qualification1967.py')
    fixture_inputs = []
    for name, text in queue.FIXTURES.items():
        if name.startswith('android/'):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
            fixture_inputs.append(path)
    real_fixture_inputs = [source / n for n in (
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java',
        'tests1981/PipelineTransport1981.java', 'tests1981/PipelineRoutes1981Test.java')]
    fixture_pins = {p.relative_to(source).as_posix(): sha(p) for p in real_fixture_inputs}
    base_cp = str(classes) + os.pathsep + str(jar)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', base_cp,
         '-d', fixtures, *fixture_inputs, *real_fixture_inputs], work / 'fixture-compile.log')
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', base_cp,
         '-d', old, *[ref / n for n in pins['files']]], work / 'published80-compile.log')
    native = work / 'native'
    native.mkdir(exist_ok=True)
    flags = ['-O3', '-std=c11', '-Wall', '-Wextra', '-Werror', '-ffp-contract=off', '-fno-fast-math',
             '-fPIC', '-shared', '-Wl,--no-undefined', '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux')]
    libraries = {}
    for name, library in (
        ('native1955/single_noise1955.c', 'libulike_nr1955.so'),
        ('native1951/moire1951.c', 'libulike_moire1951.so'),
        ('native1960/residual_blocks1961.c', 'libresidual1981_test.so')):
        run(['cc', *flags, source / name, '-lm', '-pthread', '-o', native / library], work / (library + '.log'))
        libraries[library] = {'sha256': sha(native / library), 'source_sha256': sha(source / name)}
    cp = str(fixtures) + os.pathsep + base_cp
    old_cp = str(fixtures) + os.pathsep + str(old) + os.pathsep + base_cp

    def command(mode, backend, baseline=False):
        library_path = native if backend == 'jni' else work / 'unavailable'
        return [jdk / 'bin/java', '-ea', '-Xcheck:jni', '-Xmx1g', '-XX:ActiveProcessorCount=4',
                '-Djava.library.path=' + str(library_path), '-cp', old_cp if baseline else cp,
                'com.hiro.ulike.PipelineRoutes1981Test', mode, str(backend == 'jni').lower()]

    baseline = {}
    for mode in ('single', 'residual'):
        for backend in ('java', 'jni'):
            name = mode + '-' + backend
            result = subprocess.run(list(map(str, command(mode, backend, True))), capture_output=True, text=True, timeout=120)
            output = result.stdout + result.stderr
            (work / ('baseline-' + name + '.log')).write_text(output)
            if result.returncode == 0 or 'R1 caller workspace must survive cold GPU dispatch' not in output:
                raise AssertionError('Did not reproduce the precise .80 dispatch regression: ' + name + '\n' + output)
            baseline[name] = {'status': 'expected_baseline_failure', 'missing_worker_workspace_reproduced': True}
    reports = {}
    for mode, backends in (('identity', ('java', 'jni')), ('single', ('java', 'jni')),
                           ('residual', ('java', 'jni')), ('stage', ('java',)), ('stage-gate', ('java',)),
                           ('cpu-reference', ('java', 'jni')),
                           ('cold-stage', ('java',)), ('stage-fault', ('java',)), ('copy-denial', ('java',)), ('speed', ('java',)),
                           ('coefficients', ('jni',))):
        for backend in backends:
            name = mode + '-' + backend
            stdout = run(command(mode, backend), work / (name + '.log'), 600)
            report = json.loads(stdout.strip().splitlines()[-1])
            if report.get('status') != 'passed' or report.get('assertions', 0) < 5:
                raise AssertionError('Incomplete production route test: ' + name)
            reports[name] = report
            (work / 'completed.json').write_text(json.dumps(reports, indent=2) + '\n')
    for backend in ('java', 'jni'):
        item = reports['identity-' + backend]
        if item['pixels'] < 1000000 or item['cases'] < 300 or item['changed_pixels'] < 1000 or item['naive_differences'] < 1:
            raise AssertionError('Insufficient full-image/active two-stage coverage')
    if reports['coefficients-jni']['coefficient_words'] < 100000:
        raise AssertionError('Insufficient actual JNI residual-coefficient raw bits')
    actual_gpu = native_stage(source, work / 'actual-gpu', jdk, ndk, jar, classes, fixture_inputs, native / 'libulike_nr1955.so')
    gate_module = module('pipeline81_cpu_gate', source / 'host_speed_cpu1978.py')
    gate = gate_module.qualification(source, work / 'qualification', jdk)
    if source_pins != {p.relative_to(source).as_posix(): sha(p) for p in inputs} or fixture_pins != {p.relative_to(source).as_posix(): sha(p) for p in real_fixture_inputs}:
        raise AssertionError('Production/fixture closure changed during route verification')
    reference(source)
    report = {'status': 'passed', 'assertions': sum(v['assertions'] for v in reports.values()) + gate['assertions'] + actual_gpu['assertions'],
              **{flag: True for flag in REQUIRED_FLAGS}, 'baseline': baseline, 'runs': reports, 'qualification': gate, 'actual_gpu_stage': actual_gpu,
              'production_source_sha256': source_pins, 'fixture_source_sha256': fixture_pins,
              'unchanged_numeric_source_sha256': pins['unchanged_numeric_sources'],
              'baseline_mpp_sha256': BASE_MPP, 'baseline_manifest_sha256': REFERENCE_PIN,
              'fresh_native_libraries': libraries, 'android_jar_sha256': sha(jar), 'runner_sha256': sha(__file__),
              'fixture_classes_in_runtime': False, 'physical_android_tested': False, 'device_speedup_verified': False,
              'gpu_shader_numerical_tested_here': True, 'mesa_host_only': True}
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
