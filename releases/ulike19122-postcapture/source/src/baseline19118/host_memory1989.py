#!/usr/bin/env python3
"""Exercise actual Finish relief Java with unchanged native idle/busy pools.

The fixed competing-owner and output-only observer peers inject memory pressure
and observation faults. They never implement a fit or trim algorithm. The .88
negative control runs frozen production admission in a separate JVM. Source
inversion is exclusively for preservation contracts, not current behavior.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent
BASELINE88 = {
    'GpuNoise1960.java': '9508c6ce0b26348ed4b6df168215b8d0f4dc1ee0c15b1282d78fd4296264f9cb',
    'SpeedWorkers1935.java': '8e8f5c5d14babcff55e9f4c8180d945a156b797a4307382b6d54dada851d1c6b',
}
FLAGS = (
    'memory89_unused_native_recovery_verified',
    'memory89_original_admission_and_foreground_preserved',
    'memory89_single_retry_same_limits_verified',
    'memory89_busy_native_and_gpu_ownership_verified',
    'memory89_cancellation_preserved',
    'memory89_optional_observation_faults_noninterference',
    'memory89_native_trim_failures_isolated',
    'memory89_fixed_scalar_observation_verified',
    'memory89_native_only_java_pool_preserved',
    'memory89_published88_negative_control_reproduced',
    'memory89_reviewed_source_inverse_verified',
    'memory89_actual_production_java_native_pools_executed',
    'memory89_actual_gpu_lease_ownership_verified',
)


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def reviewed_source88_1989(raw, name):
    """Strict whole-source pins and exact reviewed added-hunk inverse only."""
    raw = raw.encode() if isinstance(raw, str) else raw
    folder = ROOT / 'tests1989/memory'
    baseline = (folder / 'baseline88' / name).read_bytes()
    if sha(baseline) != BASELINE88[name]:
        raise AssertionError('Published .88 memory source changed: ' + name)
    reviewed = json.loads((folder / 'reviewed-spans.json').read_text())[name]
    if sha(raw) != reviewed['current_sha256']:
        raise AssertionError('Reviewed .89 memory source changed: ' + name)
    lines = raw.decode().splitlines(keepends=True)
    for hunk in reversed(reviewed['hunks']):
        first, last = hunk['new_first'], hunk['new_last']
        if hunk['old'] or ''.join(lines[first:last]) != hunk['new']:
            raise AssertionError('Memory repair must only add reviewed methods: ' + name)
        lines[first:last] = []
    restored = ''.join(lines).encode()
    if restored != baseline:
        raise AssertionError('Existing foreground/budget/worker source changed: ' + name)
    return restored


def reviewed_source88(name, text):
    value = reviewed_source88_1989(text, name)
    return value.decode() if isinstance(text, str) else value


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    ndk = Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    if '27.2.12479018' not in (ndk / 'source.properties').read_text():
        raise AssertionError('Pinned Android NDK headers required')
    local = source / 'tests1989/memory'
    sources = [source / n for n in (*BASELINE88, 'FinishMemory1989.java')]
    fixtures = [local / n for n in ('Memory1989Test.java', 'MemoryOwners1989.java', 'MemoryNative1989.c')]
    native = [source / n for n in ('native1960/engine1960.c', 'native1960/build_native1960.py',
        'h8gpu/h8_gpu.c', 'h8gpu/bilateral1950.comp', 'native1958/scratch1959.h')]
    tracked = sources + fixtures + native + [Path(__file__).resolve(), local / 'reviewed-spans.json',
        local / 'baseline88/pins.json'] + [local / 'baseline88' / n for n in BASELINE88]
    before = {str(p): sha(p) for p in tracked}
    for name in BASELINE88:
        reviewed_source88_1989((source / name).read_bytes(), name)
    pins = json.loads((local / 'reviewed-spans.json').read_text())
    if sha(source / 'FinishMemory1989.java') != pins['FinishMemory1989.java']['current_sha256']:
        raise AssertionError('Reviewed Finish memory helper changed')
    commands = []

    def run(label, argv, expected_failure=None, env=None):
        argv = list(map(str, argv));commands.append({'label': label, 'argv': argv})
        result = subprocess.run(argv, text=True, capture_output=True, timeout=180, env=env)
        output = result.stdout + result.stderr
        (work / (label + '.log')).write_text(output)
        if expected_failure:
            if result.returncode == 0 or expected_failure not in output:
                raise AssertionError('Published negative control did not reproduce exact missing relief: ' + output[-8000:])
        elif result.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise AssertionError(label + '\n' + output[-16000:])
        return result.stdout

    builder = module('memory89_shader_header', source / 'native1960/build_native1960.py')
    builder.shader_header(source / 'native1960', work)
    shader = (source / 'h8gpu/bilateral1950.comp').read_text()
    (work / 'bilateral_source1951.h').write_text('/* Verbatim production shader for the unchanged native owner. */\n'
        'static const char bilateral1951_source[] =\n' + ''.join(json.dumps(line + '\n') + '\n' for line in shader.splitlines()) + ';\n')
    headers = work / 'headers'
    sysroot = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL', 'GLES3', 'KHR'):
        shutil.copytree(sysroot / name, headers / name, dirs_exist_ok=True)
    flags = ['-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math', '-ffp-contract=off',
        '-Wall', '-Wextra', '-Werror', '-Wno-misleading-indentation', '-DANDROID']
    includes = ['-I' + str(p) for p in (source, headers, jdk / 'include', jdk / 'include/linux', work)]
    link = ['-Wl,--no-undefined', '-l:libEGL.so.1', '-l:libGL.so.1', '-lm', '-pthread']
    run('actual-gpu-native-compile', ['cc', *flags, *includes, source / 'native1960/engine1960.c', *link,
        '-o', work / 'libulike_gpu1960.so'])
    run('actual-pools-native-compile', ['cc', *flags, *includes, local / 'MemoryNative1989.c', *link,
        '-o', work / 'libmemory_native1989.so'])
    current, baseline = work / 'current', work / 'published88'
    current.mkdir(exist_ok=True);baseline.mkdir(exist_ok=True)
    run('current-java-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', current,
        *sources, local / 'MemoryOwners1989.java', local / 'Memory1989Test.java'])
    run('published88-java-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', current,
        '-d', baseline, *[local / 'baseline88' / n for n in BASELINE88]])
    environment = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    base_args = [jdk / 'bin/java', '-ea', '-Xcheck:jni', '-XX:ActiveProcessorCount=4', '-Xms64m', '-Xmx768m',
        '-Djava.library.path=' + str(work)]
    output = run('current-production', [*base_args, '-cp', current, 'com.hiro.ulike.Memory1989Test'], env=environment)
    report = json.loads(output.strip().splitlines()[-1])
    if report.get('status') != 'passed' or report.get('assertions', 0) < 100:
        raise AssertionError('Executed current memory assertions missing')
    for key in ('actual_memory_java_executed', 'actual_native_pools_executed', 'actual_gpu_lease_executed'):
        if report.get(key) is not True:
            raise AssertionError('Actual production ownership evidence missing: ' + key)
    run('published88-negative', [*base_args, '-cp', os.pathsep.join(map(str, (baseline, current))),
        'com.hiro.ulike.Memory1989Test', 'published88'],
        expected_failure='UNUSED_NATIVE_CACHE_PREVENTS_FINISH', env=environment)
    if before != {str(p): sha(p) for p in tracked}:
        raise AssertionError('Memory production or immutable fixture source changed during test')
    result = dict(status='passed', assertions=report['assertions'], tests={'current_production': report},
        physical_android_tested=False, device_speedup_verified=False,
        production_source_sha256={p.name: sha(p) for p in sources},
        native_source_sha256={str(p.relative_to(source)): sha(p) for p in native},
        fixture_source_sha256={p.name: sha(p) for p in fixtures},
        runner_source_sha256=sha(__file__), baseline88_source_sha256=BASELINE88,
        fixed_native_limit_bytes=512 * 1024 * 1024, unchanged_heap_reserve_bytes=64 * 1024 * 1024,
        negative_control={'status': 'expected_failure', 'message': 'UNUSED_NATIVE_CACHE_PREVENTS_FINISH'},
        native_test_environment='host software GLES plus unchanged native pool implementations', commands=commands)
    result.update({flag: True for flag in FLAGS})
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
