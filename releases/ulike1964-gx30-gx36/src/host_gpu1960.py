#!/usr/bin/env python3
"""Run unchanged production GX JNI and compute shaders on Mesa GLES.

The oracle is independently compiled published 1.9.59 Java, frozen before GX
edits. Software Mesa is real shader execution, not a physical Android benchmark.
Unsupported FP64 is reported explicitly and must not be treated as GPU coverage.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess

PIN_SHA = '0510bf56f58a873563cdac95168d8f1d5b223f79a98a36ac01315e9193963575'

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def run(args, log, env=None, timeout=360):
    result = subprocess.run(list(map(str, args)), capture_output=True, text=True,
                            env=env, timeout=timeout)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError('GX1960 executed test failed: ' + str(log) + '\n' +
                           result.stdout[-8000:] + result.stderr[-8000:])
    return result.stdout

def test(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve() / 'host-gpu1960'
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    ndk = Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    reference = root / 'tests1960/published1959-reference'
    if sha(reference / 'pins.json') != PIN_SHA:
        raise AssertionError('Frozen published .59 pin manifest changed')
    pins = json.loads((reference / 'pins.json').read_text())
    for name, expected in pins['files'].items():
        if sha(reference / name) != expected['sha256']:
            raise AssertionError('Frozen .59 CPU source changed: ' + name)
    from host_speed1959 import _sources
    baseline_sources = _sources(reference) + [reference / 'NativeSpeed1935.java', reference / 'FastPixels1933.java', root / 'tests1960/Oracle1960.java']
    classes = work / 'reference-classes'
    classes.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-d', classes, *baseline_sources], work / 'oracle-compile.log')
    oracle_log = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4',
                     '-Djava.library.path=' + str(work / 'unavailable'),
                     '-cp', classes, 'com.hiro.ulike.Oracle1960', work / 'published1959.bin'],
                    work / 'oracle-run.log')
    oracle = json.loads(oracle_log.strip())
    if oracle['referenceNativeEnabled'] or oracle['records'] < 80:
        raise AssertionError('Independent CPU reference generation missing')

    graphics = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    headers = work / 'headers'
    headers.mkdir(exist_ok=True)
    for name in ('EGL', 'GLES3', 'KHR'):
        shutil.copytree(graphics / name, headers / name, dirs_exist_ok=True)
    spec = importlib.util.spec_from_file_location('gx1960_builder', root / 'native1960/build_native1960.py')
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    builder.shader_header(root / 'native1960', work)
    tracked = [root / 'GpuNoise1960.java', root / 'native1960/engine1960.c',
               root / 'native1960/build_native1960.py', *sorted((root / 'native1960').glob('*.comp'))]
    before = {str(p.relative_to(root)): sha(p) for p in tracked}
    library = work / 'libulike_gpu1960.so'
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math', '-ffp-contract=off',
         '-Wall', '-Wextra', '-Werror', '-Wno-misleading-indentation', '-DANDROID',
         '-I' + str(headers), '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
         '-I' + str(work), root / 'native1960/engine1960.c', root / 'tests1960/fault1960.c',
         '-Wl,--wrap=glClientWaitSync', '-Wl,--wrap=glUnmapBuffer', '-Wl,--wrap=glDispatchCompute', '-Wl,--no-undefined',
         '-l:libEGL.so.1', '-l:libGL.so.1', '-o', library], work / 'native-compile.log')
    current = work / 'current-classes'
    current.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-cp', classes, '-d', current, root / 'GpuNoise1960.java',
         root / 'tests1960/Native1960Test.java'], work / 'current-compile.log')
    env = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    command = [jdk / 'bin/java', '-Xcheck:jni', '-Djava.library.path=' + str(work),
               '-cp', str(current) + os.pathsep + str(classes), 'com.hiro.ulike.Native1960Test']
    reports = {}
    for name, arguments in [('pixels', [work / 'published1959.bin']),
                            ('foreign_context', ['fault', 'context']),
                            ('late_unmap_failure', ['fault', 'unmap']),
                            ('unknown_fence_completion', ['fault', 'timeout'])]:
        output = run(command + arguments, work / (name + '.log'), env=env)
        if 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise AssertionError('JNI checker failed: ' + name)
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found:
            raise AssertionError('No executed result: ' + name)
        reports[name] = json.loads(found.group(1))
        if reports[name].get('status') != 'passed' or reports[name].get('assertions', 0) < 1:
            raise AssertionError('Executed assertions failed: ' + name)
    if before != {str(p.relative_to(root)): sha(p) for p in tracked}:
        raise AssertionError('Production source changed during test; rerun required')
    spec = importlib.util.spec_from_file_location('single_fp64_1960_host', root / 'tests1960/host_single_fp64_1960.py')
    desktop_single = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(desktop_single)
    reports['desktop_single_fp64'] = desktop_single.test(root, work, work / 'published1959.bin')
    facade_classes = work / 'facade-classes'
    facade_classes.mkdir(exist_ok=True)
    facade_sources = _sources(root) + [root / name for name in (
        'NativeSpeed1935.java', 'FastPixels1933.java', 'GpuNoise1960.java',
        'GpuPolicy1960.java', 'GpuStrong1960.java', 'GpuSingle1960.java',
        'GpuGeometry1960.java', 'tests1960/Native1960Test.java', 'tests1960/Facade1960Test.java')]
    facade_sources = list(dict.fromkeys(facade_sources))
    facade_pins = {str(p.relative_to(root)): sha(p) for p in facade_sources}
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-d', facade_classes, *facade_sources], work / 'facade-compile.log')
    for name, native_path, use_gpu, extra in (
            ('production_facades_gpu', work, 'true', []),
            ('production_facades_unavailable', work / 'unavailable', 'false', []),
            ('production_facade_late_failure', work, 'true', ['fault'])):
        output = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4', '-Xcheck:jni',
                      '-Djava.library.path=' + str(native_path), '-cp', facade_classes,
                      'com.hiro.ulike.Facade1960Test', work / 'published1959.bin', use_gpu, *extra],
                     work / (name + '.log'), env=env)
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found or 'WARNING in native method' in output or 'FATAL ERROR' in output:
            raise AssertionError('Facade execution/JNI report absent: ' + name)
        reports[name] = json.loads(found.group(1))
        if reports[name].get('status') != 'passed' or reports[name].get('assertions', 0) < 1:
            raise AssertionError('Facade regression failed: ' + name)
    if facade_pins != {str(p.relative_to(root)): sha(p) for p in facade_sources}:
        raise AssertionError('Production facade sources changed during execution; rerun')
    before.update(facade_pins)
    pipeline_dumps = []
    for label, source_root, sources, native_path, use_gpu in (
            ('published1959_pipeline', reference, _sources(reference), work / 'unavailable', 'false'),
            ('candidate1960_pipeline', root, facade_sources, work, 'true')):
        pipeline_classes = work / (label + '-classes')
        pipeline_classes.mkdir(exist_ok=True)
        selected = [p for p in sources if p.name != 'FastResize1933.java']
        selected += [source_root / 'FastResize1933.java', source_root / 'NativeSpeed1935.java',
                     source_root / 'FastPixels1933.java', root / 'tests1960/Pipeline1960Test.java']
        selected = list(dict.fromkeys(selected))
        run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
             '-Xlint:-options', '-d', pipeline_classes, *selected], work / (label + '-compile.log'))
        dump = work / (label + '.bin')
        output = run([jdk / 'bin/java', '-XX:ActiveProcessorCount=4', '-Xcheck:jni',
                      '-Djava.library.path=' + str(native_path), '-cp', pipeline_classes,
                      'com.hiro.ulike.Pipeline1960Test', dump, use_gpu], work / (label + '.log'), env=env)
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found:
            raise AssertionError('Complete save pipeline execution report missing')
        reports[label] = json.loads(found.group(1))
        pipeline_dumps.append(dump.read_bytes())
    if pipeline_dumps[0] != pipeline_dumps[1]:
        raise AssertionError('Saved-image pipeline differs from frozen published 1959')
    reports['candidate1960_pipeline']['exact_published1959_dump_sha256'] = hashlib.sha256(pipeline_dumps[0]).hexdigest()
    before['FastResize1933.java'] = sha(root / 'FastResize1933.java')
    report = {
        'schema': 'ulike-gx1960-executed-production-jni-v1', 'status': 'passed',
        'assertions': sum(r.get('assertions', 0) for r in reports.values()),
        'oracle': oracle, 'reports': reports,
        'reference': 'byte-frozen published 1.9.59 Java CPU in independent JVM',
        'oracle_binary_sha256': sha(work / 'published1959.bin'),
        'unchanged_production_engine_and_shader_sources': True,
        'gpu_shader_execution_on_host': True,
        'host_backend': 'Mesa software surfaceless EGL GLES',
        'host_pixel_equivalence_to_baseline': True,
        'single_fp64_shader_executed': reports['pixels']['singleFp64Supported'],
        'physical_android_tested': False, 'device_speedup_verified': False,
        'device_quality_improvement_verified': False,
        'cpu_fallback_capability_required': not reports['pixels']['singleFp64Supported'],
        'sources': before,
        'tests': {str(p.relative_to(root)): sha(p) for p in sorted((root / 'tests1960').glob('*')) if p.is_file()},
    }
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--root', default=str(Path(__file__).resolve().parent))
    p.add_argument('--work', required=True)
    p.add_argument('--jdk')
    p.add_argument('--ndk')
    a = p.parse_args()
    print(json.dumps(test(a.root, a.work, a.jdk, a.ndk), indent=2))
