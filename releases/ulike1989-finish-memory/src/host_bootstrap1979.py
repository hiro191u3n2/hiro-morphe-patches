#!/usr/bin/env python3
"""Actual JNI/worker/qualification regression for restored bounded bootstrap.

The .77, published .78, and .79 Strong facades run against the same current
JNI library and qualification code. This isolates the foreground selection
regression; it does not migrate certificates between different fingerprints.
Android and model holders are fixtures. CPU pixels come from frozen .59 Java.
No physical Android latency or device speedup is asserted.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess, sys

ROOT = Path(__file__).resolve().parent
REQUIRED_FLAGS = (
    'cold_legacy_gpu_without_seed1979_verified',
    'legacy_same_environment_restore1979_verified',
    'published78_cold_regression_reproduced1979_verified',
    'bounded_foreground_actual_workers1979_verified',
    'foreground_failure_cancel_late_rejection1979_verified',
    'resident_benchmark_unknown_cpu1979_verified',
)
BASELINE_PINS = {
    'published77/GpuStrong1960.java': '9a5efe00e6f8febe864ab61ffe3cd4005820c44617d8ff08a5cf0ed432c62cbb',
    'published78/GpuStrong1960.java': '1a6a7995c1804442a1bc3322a653ed779b2e72e0457eb5f46cbf0f78c1b3c0d6',
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    sys.path.insert(0, str(source))
    fixture = source / 'gpu79-fixtures'
    baseline = fixture / 'bootstrap-reference'
    declared = json.loads((baseline / 'pins.json').read_text())
    if set(declared) != set(BASELINE_PINS):
        raise AssertionError('Unexpected bootstrap comparison baseline')
    for name, expected in BASELINE_PINS.items():
        path = baseline / name
        if sha(path) != expected or declared[name]['sha256'] != expected or path.stat().st_size != declared[name]['bytes']:
            raise AssertionError('Published foreground comparison source changed: ' + name)
    reference = source / 'tests1960/published1959-reference'
    if sha(reference / 'pins.json') != '0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575':
        raise AssertionError('Frozen independent Java pin manifest changed')
    for name, expected in json.loads((reference / 'pins.json').read_text())['files'].items():
        path = reference / name
        if sha(path) != expected['sha256'] or path.stat().st_size != expected['bytes']:
            raise AssertionError('Frozen independent Java reference changed: ' + name)

    native = source / 'native1960'
    speed = module('bootstrap79_frozen_cpu', source / 'host_speed1959.py')
    builder = module('bootstrap79_native_builder', native / 'build_native1960.py')
    android = module('bootstrap79_android_holders', source / 'host_qualification1967.py')
    oracle_sources = speed._sources(reference) + [reference / 'NativeSpeed1935.java', reference / 'FastPixels1933.java', source / 'tests1960/Oracle1960.java']
    common = [source / name for name in ('GpuNoise1960.java', 'GpuQualification1961.java', 'SaveQueue1935.java', 'SpeedWorkers1935.java')]
    inputs = set(common + [source / 'GpuStrong1960.java', native / 'engine1960.c', native / 'build_native1960.py', native / 'residual_blocks1961.c', source / 'native1955/single_noise1955.c', source / 'host_speed1959.py', source / 'host_qualification1967.py', source / 'host_bootstrap1979.py', reference / 'pins.json', baseline / 'pins.json'] + oracle_sources + sorted(native.glob('*.comp')) + sorted(source.glob('*.java')) + sorted((source / 'quality-dependencies').rglob('*.java')) + [baseline / name for name in BASELINE_PINS] + [fixture / name for name in ('BootstrapFixtures1979.java', 'Bootstrap1979Test.java', 'native_fault1979.c')])
    def pins():
        return {str(path.relative_to(source)): sha(path) for path in sorted(inputs)}
    before = pins()
    java = Path(jdk).resolve() / 'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac, jdk_root = java.parent / 'javac', java.parent.parent
    compiler = [javac] if javac.is_file() else [java, '-m', 'jdk.compiler/com.sun.tools.javac.Main']
    commands = []
    def run(command, label, timeout=180):
        command = [str(arg) for arg in command]
        commands.append(dict(label=label, argv=command))
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout,
            env=dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1'))
        output = result.stdout + result.stderr
        (work / (label + '.log')).write_text(output)
        if result.returncode:
            raise RuntimeError(label + ' failed: ' + output[-10000:])
        if '-Xcheck:jni' in command and ('WARNING in native method' in output or 'FATAL ERROR' in output):
            raise AssertionError('JNI checker reported a failure: ' + label)
        return result.stdout
    refclasses = work / 'reference-classes'
    if refclasses.exists():
        shutil.rmtree(refclasses)
    refclasses.mkdir()
    run(compiler + ['-source', '8', '-target', '8', '-Xlint:-options', '-d', refclasses, *oracle_sources], 'oracle-compile')
    out = run([java, '-XX:ActiveProcessorCount=4', '-Djava.library.path=' + str(work / 'unavailable'), '-cp', refclasses, 'com.hiro.ulike.Oracle1960', work / 'oracle.bin'], 'oracle-generate')
    oracle = json.loads(out.strip().splitlines()[-1])
    if oracle.get('referenceNativeEnabled') is not False or oracle.get('records') != 152:
        raise AssertionError('Independent frozen CPU reference absent')
    headers = []
    if ndk:
        graphics = Path(ndk).resolve() / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
        for name in ('EGL', 'GLES3', 'KHR'):
            shutil.copytree(graphics / name, work / 'headers' / name, dirs_exist_ok=True)
        headers = ['-I' + str(work / 'headers')]
    builder.shader_header(native, work)
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math', '-ffp-contract=off', *headers, '-I' + str(jdk_root / 'include'), '-I' + str(jdk_root / 'include/linux'), '-I' + str(work), native / 'engine1960.c', fixture / 'native_fault1979.c', '-Wl,--wrap=glDispatchCompute', '-Wl,--wrap=glMapBufferRange', '-l:libEGL.so.1', '-l:libGLESv2.so.2', '-o', work / 'libulike_gpu1960.so'], 'native-compile')
    android_sources = []
    for name, content in android.FIXTURES.items():
        if name.startswith('android/'):
            path = work / 'fixtures' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
            android_sources.append(path)
    results = {}
    for revision in (77, 78, 79):
        classes = work / ('classes' + str(revision))
        if classes.exists():
            shutil.rmtree(classes)
        classes.mkdir()
        strong = source / 'GpuStrong1960.java' if revision == 79 else baseline / ('published' + str(revision)) / 'GpuStrong1960.java'
        run(compiler + ['-source', '8', '-target', '8', '-Xlint:-options', '-d', classes, *common, strong, fixture / 'BootstrapFixtures1979.java', fixture / 'Bootstrap1979Test.java', *android_sources], 'compile-' + str(revision))
        modes = ('cold',)
        if revision == 77:
            modes += ('budget', 'followers')
        if revision == 78:
            modes += ('benchmark',)
        if revision == 79:
            modes += ('budget', 'followers', 'benchmark', 'memory', 'readback', 'false', 'throw', 'interrupt', 'argb', 'confidence', 'late-negative', 'legacy-rejected')
        for mode in modes:
            label = 'revision' + str(revision) + '-' + mode
            out = run([java, '-XX:ActiveProcessorCount=4', '-Xcheck:jni', '-Djava.library.path=' + str(work), '-cp', classes, 'com.hiro.ulike.Bootstrap1979Test', work / 'oracle.bin', mode, str(revision)], label)
            result = json.loads(out.strip().splitlines()[-1])
            if result.get('status') != 'passed' or result.get('revision') != revision or result.get('actual_jni') is not True or result.get('actual_workers') is not True or result.get('actual_signed_preferences') is not True or result.get('assertions', 0) <= 0:
                raise AssertionError('Actual production bootstrap evidence missing: ' + label)
            results[label] = result
    if before != pins():
        raise AssertionError('Source graph changed during production bootstrap replay')
    report = dict(status='passed', assertions=sum(item['assertions'] for item in results.values()), tests=results,
        source_sha256=before, baseline_sha256=BASELINE_PINS, frozen_java_reference=oracle,
        reference_payload_sha256=sha(work / 'oracle.bin'), actual_jni=True, jni_check_enabled=True,
        actual_production_workers=True, actual_qualification_and_save_ownership=True,
        preference_storage='Android SharedPreferences scalar holder; production signing, restore, and rejection code',
        cpu_oracle='Frozen published .59 full ARGB and confidence',
        comparison_native_library='One current JNI binary for all three Strong source revisions; no fingerprint migration',
        physical_android_tested=False, device_speedup_verified=False)
    report.update({name: True for name in REQUIRED_FLAGS})
    (work / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
