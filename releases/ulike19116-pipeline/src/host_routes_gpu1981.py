#!/usr/bin/env python3
"""Execute .81 route, snapshot and queue contracts against current production.

Android holders and injected contention are explicit test fixtures. All quality
gates, queue ownership and routing code come from the supplied release source.
Host timings are never reported as Android performance measurements.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess
import sys


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def run(command, work, label, timeout=240, env=None):
    result = subprocess.run(list(map(str, command)), text=True, capture_output=True,
                            timeout=timeout, env=env)
    output = result.stdout + result.stderr
    (work / (label + '.log')).write_text(output)
    if result.returncode:
        raise AssertionError(label + '\n' + output[-16000:])
    if 'WARNING in native method' in output or 'FATAL ERROR' in output:
        raise AssertionError('JNI checker failed: ' + label)
    return result.stdout


def report(output):
    value = json.loads(output.strip().splitlines()[-1])
    if value.get('status') != 'passed' or value.get('assertions', 0) <= 0:
        raise AssertionError('Missing executed assertions')
    return value


def queue_test(source, work, jdk):
    source, work, jdk = Path(source), Path(work), Path(jdk)
    work.mkdir(parents=True, exist_ok=True)
    generated, classes = work / 'fixtures', work / 'classes'
    classes.mkdir(exist_ok=True)
    holders = module('routes1981_android', source / 'host_qualification1967.py')
    inherited_runner = module('routes1981_tuner_contract', source / 'host_tuning1979.py')
    inherited_runner._legacy_coverage(source)
    inputs = [source / name for name in ('GpuQualification1961.java',
              'GpuStrongTuning1975.java', 'GpuSnapshotBudget1981.java',
              'gpu79-fixtures/TuningFixtures1979.java',
              'gpu79-fixtures/Tuning1979Test.java', 'tests1981/GpuQueue1981Test.java')]
    for name, body in holders.FIXTURES.items():
        if name.startswith('android/'):
            path = generated / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            inputs.append(path)
    tracked = inputs + [source / name for name in ('gpu78-fixtures/TuningFixtures1978.java',
              'gpu78-fixtures/Tuning1978Test.java', 'host_tuning1978.py', 'host_tuning1979.py',
              'host_qualification1967.py', 'host_routes_gpu1981.py')]
    before = {str(p): sha(p) for p in tracked}
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes,
         *inputs], work, 'compile')
    prefix = [jdk / 'bin/java', '-ea', '-cp', classes]
    inherited = report(run(prefix + ['com.hiro.ulike.Tuning1979Test'], work, 'tuner1979-unchanged'))
    budget = report(run(prefix + ['com.hiro.ulike.GpuQueue1981Test'], work, 'budget-fairness1981'))
    required = ('first_legacy_exact2_progress_survives_capture_cancellation',
                'later_policy_rejection_retires_early_legacy_child')
    if any(inherited.get('tests', {}).get(k, 0) <= 0 for k in required):
        raise AssertionError('Inherited early legacy proof regression cases did not execute')
    if before != {str(p): sha(p) for p in tracked}:
        raise AssertionError('Queue/tuner source changed during execution')
    result = dict(status='passed', assertions=inherited['assertions'] + budget['assertions'],
                  tests=dict(inherited_tuner=inherited, queue_budget=budget),
                  physical_android_tested=False, device_speedup_verified=False,
                  source_sha256=before)
    result.update(twentyfour_candidate_tuning1978_verified=True,
                  idle_two_full_argb_confidence_policy1978_verified=True,
                  idle_snapshot_cancel_and_memory1978_verified=True,
                  idle_upload_reuse1978_verified=True,
                  new_gpu_balanced_five_percent_gate1978_verified=True,
                  legacy_exact_rejections1978_preserved=True,
                  worker_bound_speed_retry1978_verified=True,
                  legacy_gpu_failure_direct_recovery1978_verified=True,
                  idle_first_legacy_exact2_progress1979_verified=True,
                  later_policy_rejection1979_preserved=True,
                  original_1978_tuning_cases_retained1979_verified=True)
    result.update({key: value for key, value in budget.items() if key.endswith('_verified')})
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


def native_test(source, work, jdk, ndk=None):
    source, work, jdk = Path(source).resolve(), Path(work).resolve(), Path(jdk).resolve()
    work.mkdir(parents=True, exist_ok=True)
    reference, native = source / 'tests1960/published1959-reference', source / 'native1960'
    if sha(reference / 'pins.json') != '0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575':
        raise AssertionError('Frozen independent Java pin manifest changed')
    for name, expected in json.loads((reference / 'pins.json').read_text())['files'].items():
        path = reference / name
        if sha(path) != expected['sha256'] or path.stat().st_size != expected['bytes']:
            raise AssertionError('Frozen independent Java reference changed: ' + name)
    speed = module('routes1981_frozen_cpu', source / 'host_speed1959.py')
    builder = module('routes1981_native_builder', native / 'build_native1960.py')
    holders = module('routes1981_native_android', source / 'host_qualification1967.py')
    oracle_sources = speed._sources(reference) + [reference / 'NativeSpeed1935.java',
                     reference / 'FastPixels1933.java', source / 'tests1960/Oracle1960.java']
    production = [source / name for name in ('GpuNoise1960.java', 'GpuStrong1960.java',
                  'GpuStrongTuning1975.java', 'GpuStrongRouting1978.java',
                  'GpuQualification1961.java', 'GpuSnapshotBudget1981.java',
                  'SaveQueue1935.java', 'SaveMemory1981.java', 'SpeedWorkers1935.java')]
    fixtures = [source / name for name in ('tests1981/GpuRouteNativeFixtures1981.java',
                'tests1981/GpuRouteNative1981Test.java', 'gpu79-fixtures/Bootstrap1979Test.java')]
    android_sources = []
    for name, body in holders.FIXTURES.items():
        if name.startswith('android/'):
            path = work / 'fixtures' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            android_sources.append(path)
    inputs = sorted(set(production + fixtures + oracle_sources + android_sources +
             [native / 'engine1960.c', native / 'build_native1960.py', native / 'residual_blocks1961.c',
              source / 'native1955/single_noise1955.c', source / 'gpu79-fixtures/native_fault1979.c',
              reference / 'pins.json', source / 'host_speed1959.py', source / 'host_qualification1967.py',
              source / 'host_routes_gpu1981.py'] + list(native.glob('*.comp'))))
    before = {str(p): sha(p) for p in inputs}
    refclasses, classes = work / 'reference-classes', work / 'classes'
    for path in (refclasses, classes):
        if path.exists():
            shutil.rmtree(path)
        path.mkdir()
    compiler = [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8']
    run(compiler + ['-d', refclasses, *oracle_sources], work, 'oracle-compile')
    output = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4', '-Djava.library.path=' + str(work / 'unavailable'),
                  '-cp', refclasses, 'com.hiro.ulike.Oracle1960', work / 'oracle.bin'], work, 'oracle-generate')
    oracle = json.loads(output.strip().splitlines()[-1])
    if oracle.get('referenceNativeEnabled') is not False or oracle.get('records') != 152:
        raise AssertionError('Independent frozen CPU reference absent')
    includes = []
    if ndk:
        graphics = Path(ndk).resolve() / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
        for name in ('EGL', 'GLES3', 'KHR'):
            shutil.copytree(graphics / name, work / 'headers' / name, dirs_exist_ok=True)
        includes = ['-I' + str(work / 'headers')]
    builder.shader_header(native, work)
    env = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    # The shipped NDK EGL header otherwise selects X11 types on this Linux
    # host. USE_OZONE only supplies scalar native-display typedefs; the actual
    # engine still creates its unchanged EGL_DEFAULT_DISPLAY pbuffer context.
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-DUSE_OZONE', '-fno-fast-math', '-ffp-contract=off', *includes,
         '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'), '-I' + str(work),
         native / 'engine1960.c', source / 'gpu79-fixtures/native_fault1979.c',
         '-Wl,--wrap=glDispatchCompute', '-Wl,--wrap=glMapBufferRange',
         '-l:libEGL.so.1', '-l:libGLESv2.so.2', '-o', work / 'libulike_gpu1960.so'], work, 'native-compile', env=env)
    run(compiler + ['-d', classes, *production, *fixtures, *android_sources], work, 'route-compile')
    actual = report(run([jdk / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-Xmx1g', '-Xcheck:jni',
                         '-Djava.library.path=' + str(work), '-cp', classes,
                         'com.hiro.ulike.GpuRouteNative1981Test', work / 'oracle.bin'], work, 'route-actual-jni', env=env))
    if before != {str(p): sha(p) for p in inputs}:
        raise AssertionError('Actual JNI route source changed during execution')
    result = dict(status='passed', assertions=actual['assertions'], tests=dict(actual_jni=actual),
                  frozen_java_reference=oracle, reference_payload_sha256=sha(work / 'oracle.bin'),
                  actual_jni=True, jni_check_enabled=True, actual_production_workers=True,
                  actual_qualification_and_save_ownership=True, source_sha256=before,
                  physical_android_tested=False, device_speedup_verified=False)
    result.update({key: value for key, value in actual.items()
                   if key.endswith('_verified') or key in ('cold_idle_persist_hot1981_actual_jni', 'complete_argb_confidence_exact')})
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


def plan_test(source, work, jdk):
    source, work, jdk = Path(source).resolve(), Path(work).resolve(), Path(jdk).resolve()
    work.mkdir(parents=True, exist_ok=True)
    reference = source / 'tests1981/gpu80-plan-reference'
    # This baseline is copied byte-for-byte from the published SHA-verified
    # .80 source ZIP. Run it independently; never rewrite current production.
    expected = {'GpuPolicy1960.java': '0120f25e3068526c0e66acd5d4fc48ea253861490b26e019ebecba46d5cf8c82', 'StrongNoise1958.java': '96d9a676aac098d1e866bfd0c17bde5c7299cd3a32cb148bec3b0a3fc6510e98'}
    manifest = json.loads((reference / 'pins.json').read_text())
    if manifest.get('baseline_version') != '1.9.80' or set(manifest['files']) != set(expected):
        raise AssertionError('Wrong independent policy/Strong baseline')
    for name, digest in expected.items():
        path, declared = reference / name, manifest['files'][name]
        if sha(path) != digest or declared['sha256'] != digest or path.stat().st_size != declared['bytes']:
            raise AssertionError('Published .80 policy/Strong baseline changed: ' + name)
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', str(jdk.parent / 'android.jar'))).resolve()
    if not jar.is_file():
        raise AssertionError('Pinned Android API jar required')
    sys.path.insert(0, str(source))
    builder = module('routes1981_current_plan_closure', source / 'build1981.py')
    production = list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION]
    classes, fixtures, old = work / 'production-classes', work / 'fixture-classes', work / 'published80-classes'
    for path in (classes, fixtures, old):
        if path.exists():
            shutil.rmtree(path)
        path.mkdir()
    holders = module('routes1981_plan_android', source / 'host_qualification1967.py')
    fixture_sources = []
    for name, body in holders.FIXTURES.items():
        if name.startswith('android/'):
            path = work / 'fixtures' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            fixture_sources.append(path)
    fixture_sources += [source / name for name in ('tests1981/GpuPlan1981Test.java',
                        'tests1981/GpuPlanTransport1981.java',
                        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
                        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
                        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java')]
    inputs = sorted(set(production + fixture_sources + [reference / name for name in expected] +
                        [reference / 'pins.json', source / 'build1981.py', source / 'production1981.json',
                         source / 'host_routes_gpu1981.py', source / 'host_qualification1967.py']))
    before = {str(p): sha(p) for p in inputs}
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-bootclasspath', jar, '-d', classes, *production], work, 'production-compile')
    cp = str(classes) + os.pathsep + str(jar)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', fixtures,
         *fixture_sources], work, 'fixture-compile')
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp, '-d', old,
         *[reference / name for name in expected]], work, 'published80-compile')
    results = {}
    for name, roots, baseline in [('published80', [fixtures, old, classes, jar], 'true'),
                                  ('current81', [fixtures, classes, jar], 'false')]:
        output = run([jdk / 'bin/java', '-ea', '-XX:ActiveProcessorCount=4', '-Xmx1g',
                      '-Djava.library.path=' + str(work / 'unavailable'), '-cp',
                      os.pathsep.join(map(str, roots)), 'com.hiro.ulike.GpuPlan1981Test',
                      work / (name + '.bin'), baseline], work, name)
        results[name] = report(output)
    if (work / 'published80.bin').read_bytes() != (work / 'current81.bin').read_bytes():
        raise AssertionError('R3 orchestration changed complete policy, descriptor, ARGB or confidence bytes')
    if before != {str(p): sha(p) for p in inputs}:
        raise AssertionError('Actual policy/preflight source graph changed during execution')
    result = dict(status='passed', assertions=results['current81']['assertions'], tests=results,
                  differing_bytes=0, output_sha256=sha(work / 'current81.bin'),
                  source_sha256=before, baseline_sha256=expected,
                  route_policy_single_pass1981_verified=True, resident_preflight1981_verified=True,
                  complete_policy_argb_confidence_baseline80_exact=True,
                  physical_android_tested=False, device_speedup_verified=False)
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    queue = queue_test(source, work / 'queue', jdk)
    native = native_test(source, work / 'native', jdk, ndk)
    plan = plan_test(source, work / 'plan', jdk)
    inputs = {}
    for group in (queue, native, plan):
        for path, digest in group['source_sha256'].items():
            if path in inputs and inputs[path] != digest:
                raise AssertionError('Production changed between executed evidence groups: ' + path)
            inputs[path] = digest
    if any(sha(path) != digest for path, digest in inputs.items()):
        raise AssertionError('Production changed after GPU route evidence was executed')
    result = dict(status='passed', assertions=queue['assertions'] + native['assertions'] + plan['assertions'],
                  queue=queue, native=native, plan=plan, source_sha256=inputs,
                  physical_android_tested=False, device_speedup_verified=False,
                  runner_source_sha256=sha(source / 'host_routes_gpu1981.py'),
                  current_counts_only=True)
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
