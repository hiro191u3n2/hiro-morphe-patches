#!/usr/bin/env python3
"""Execute current GPU-required normalization/detail plus encoder-publication guards.

Uses actual production JNI/GLES engine, shaders and route classes. Host Bitmap
copies model Android buffers; camera, vendor beauty and HEIF hardware are absent.
The injected native readback failure happens after real shader dispatch.
"""
from pathlib import Path
import ast, importlib.util, json, os, re, shutil, subprocess


def test(root, work, jdk, ndk):
    root, work, jdk, ndk = map(lambda p: Path(p).resolve(), (root, work, jdk, ndk))
    work = work / 'strict-route1965'
    work.mkdir(parents=True, exist_ok=True)
    def run(command, name, env=None, timeout=420):
        p = subprocess.run(list(map(str, command)), capture_output=True, text=True,
                           env=env, timeout=timeout)
        output = p.stdout + p.stderr
        (work / name).write_text(output)
        if p.returncode:
            raise RuntimeError(name + ' exit=' + str(p.returncode) + ': ' + output[-9000:])
        return output
    headers = work / 'headers'
    for name in ('EGL', 'GLES3', 'KHR'):
        shutil.copytree(ndk / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include' / name,
                        headers / name, dirs_exist_ok=True)
    spec = importlib.util.spec_from_file_location('native_builder1965_route', root / 'native1960/build_native1960.py')
    builder = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(builder)
    builder.shader_header(root / 'native1960', work)
    run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math', '-ffp-contract=off',
         '-Wall', '-Wextra', '-Werror', '-Wno-misleading-indentation', '-DANDROID',
         '-I' + str(headers), '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
         '-I' + str(work), root / 'native1960/engine1960.c', root / 'tests1965/native_fault1965.c',
         '-Wl,--wrap=glClientWaitSync', '-Wl,--wrap=glUnmapBuffer', '-Wl,--wrap=glDispatchCompute',
         '-Wl,--no-undefined', '-l:libEGL.so.1', '-l:libGL.so.1', '-lm', '-o', work / 'libulike_gpu1960.so'],
        'native-compile.log')
    source_map = {}
    for folder in ['layout1937-stubs', 'geometry1937-compile', 'front1936-stubs',
                   'renderer1938-stubs', 'capture-stubs', 'fast-stubs', 'metadata-stubs',
                   'quality-stubs', 'quality-dependencies', 'timing1947-stubs', 'core1950-stubs']:
        for path in (root / folder).rglob('*.java'):
            source_map[path.relative_to(root / folder).as_posix()] = path
    module = ast.parse((root / 'build1964.py').read_text())
    retained = []
    for node in ast.walk(module):
        if isinstance(node, ast.Assign) and any(isinstance(t, ast.Name) and t.id == 'retained' for t in node.targets):
            retained = ast.literal_eval(node.value)
    production_file = next((root / name for name in ('production1966.json', 'production1965.json', 'production1964.json') if (root / name).exists()))
    production = json.loads(production_file.read_text()) + retained + [
        'GpuRequired1965', 'GpuRequiredFailure1965', 'GpuRequiredRoute1965',
        'GpuRequiredNoise1965', 'GpuRequiredPolicy1965', 'GpuRequiredChroma1965', 'GpuRequiredPlan1965']
    # The active .65 camera baseline adds these runtime signature dependencies.
    production += [name for name in ('CameraTrace1965', 'CameraSession1965', 'PreviewOutput1965',
                                     'FrontPreview1936', 'LayoutLifecycle1937') if (root / (name + '.java')).exists()]
    for name in production:
        path = root / (name + '.java')
        if not path.exists():
            path = root / 'quality-dependencies/com/hiro/ulike' / (name + '.java')
        source_map['com/hiro/ulike/' + name + '.java'] = path
    classes = work / 'production-classes'
    classes.mkdir(exist_ok=True)
    # Compile all actual helpers with their complete Android signatures first.
    candidates = [Path(os.environ.get('ULIKE_ANDROID_JAR', '/nonexistent/android.jar'))]
    candidates += [p / 'toolchain1965/tools/android.jar' for p in root.parents]
    candidates += [jdk.parent / 'tools/android.jar', jdk.parent.parent / 'tools/android.jar']
    android = next((p for p in candidates if p.exists()), None)
    if android is None:
        raise FileNotFoundError('Android SDK android.jar; provide ULIKE_ANDROID_JAR')
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-bootclasspath', android, '-d', classes, *source_map.values()], 'production-compile.log')
    fixtures = work / 'fixture-classes'
    fixtures.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-cp', str(classes) + os.pathsep + str(android), '-d', fixtures,
         *sorted((root / 'tests1965/route-fixtures').rglob('*.java')),
         root / 'tests1965/RequiredNative1965Test.java', root / 'tests1965/StrictRoute1965Test.java'],
        'fixtures-compile.log')
    env = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1', LP_DEBUG='noopt')
    reports = {}
    for name in ('format', 'capability', 'manual', 'ui', 'detail', 'mutation', 'late', 'off', 'sharp', 'chroma', 'noise', 'geometry'):
        output = run([jdk / 'bin/java', '-Xmx512m', '-Xcheck:jni', '-Djava.library.path=' + str(work),
                      '-cp', str(fixtures) + os.pathsep + str(classes) + os.pathsep + str(android),
                      'com.hiro.ulike.StrictRoute1965Test', name], name + '.log', env=env)
        assert 'WARNING in native method' not in output and 'FATAL ERROR' not in output, output
        found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
        if not found:
            raise AssertionError('Executed strict route report missing: ' + name)
        report = json.loads(found.group(1))
        assert report['status'] == 'passed' and report['assertions'] > 1
        assert report['actualProductionStrictRoute'] and report['actualJNICompute']
        reports[name] = report
    report = {'status': 'passed', 'assertions': sum(v['assertions'] for v in reports.values()),
              'strict_gpu_required_routes_verified': True,
              'cpu_image_processing_fallback_enabled': False,
              'untreated_success_on_gpu_failure_enabled': False,
              'actual_production_strict_routes_executed': True,
              'actual_production_jni_compute': True, 'physical_android_tested': False,
              'host_mesa_llvm_optimization': False, 'host_mesa_flag': 'LP_DEBUG=noopt',
              'tests': reports}
    (work / 'strict-route1965.json').write_text(json.dumps(report, indent=2) + '\n')
    return report

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--ndk', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk, args.ndk), indent=2))
