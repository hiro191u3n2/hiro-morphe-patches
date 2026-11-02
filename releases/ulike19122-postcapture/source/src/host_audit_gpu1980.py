#!/usr/bin/env python3
"""Execute real GPU-admission facades and persistent queue with transport faults.

Only GPU transport and Android host surfaces are controlled. The released Java
facades, CPU references and GpuQualification1961 compile from current source.
No fixture enters the shipped DEX; host timing is not an Android speed result.
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

ROOT = Path(__file__).resolve().parent
FACADES = ('GpuSingle1960', 'GpuResidual1961', 'GpuAnalysis1961', 'GpuPolicy1960',
           'GpuProtection1961', 'GpuGeometry1960', 'GpuChain1961')
FAMILIES = ('single', 'residual', 'spatial', 'regions', 'resident', 'protection',
            'geometry', 'chain', 'pyramid', 'evidence')
REFERENCE_PIN = '6cec80e144db4c94be335757537c3f83cae758f01b38efe75447904c84a0681f'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def run(command, log, timeout=120):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=timeout)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + (result.stdout + result.stderr)[-12000:])
    return result.stdout


def reference(source):
    directory = source / 'tests1980/gpu79-reference'
    manifest = directory / 'pins.json'
    if sha(manifest) != REFERENCE_PIN:
        raise AssertionError('Published .79 fault reference changed')
    pins = json.loads(manifest.read_text())
    if pins['baseline_version'] != '1.9.79':
        raise AssertionError('Wrong reference version')
    for name, pin in pins['files'].items():
        path = directory / name
        if sha(path) != pin['sha256'] or path.stat().st_size != pin['bytes']:
            raise AssertionError('Changed independent reference: ' + name)
    return directory, pins


def capability_test(source, work, jdk, jar, classes, old):
    """Run actual owner/facades with JNI exceptions, distinct from native false."""
    work.mkdir(parents=True, exist_ok=True)
    fixture = work / 'fixture-classes'
    fixture.mkdir(exist_ok=True)
    library = work / 'libulike_gpu1960.so'
    compiler = shutil.which('cc')
    if compiler is None:
        raise AssertionError('C compiler required for actual OWNER JNI fault test')
    run([compiler, '-std=c11', '-O2', '-shared', '-fPIC', '-Wall', '-Wextra', '-Werror',
         '-I' + str(jdk / 'include'), '-I' + str(jdk / 'include/linux'),
         source / 'tests1980/gpu_capability1980_fixture.c', '-o', library], work / 'jni-compile.log')
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', str(classes) + os.pathsep + str(jar),
         '-d', fixture, source / 'tests1980/GpuCapability1980Test.java'], work / 'fixture-compile.log')
    current, baseline = {}, {}
    for label, production, published, results in [('current80', [classes], 'false', current),
                                                 ('published79', [old, classes], 'true', baseline)]:
        cp = os.pathsep.join(map(str, [fixture, *production, jar]))
        for family in ('owner', 'single', 'residual'):
            for fault in (1, 3):
                name = family + '-' + str(fault)
                report = json.loads(run([jdk / 'bin/java', '-ea', '-Djava.library.path=' + str(work),
                                         '-cp', cp, 'com.hiro.ulike.GpuCapability1980Test', family, fault, published],
                                        work / (label + '-' + name + '.log')).strip().splitlines()[-1])
                if report.get('status') != 'passed' or report.get('assertions', 0) < 20:
                    raise AssertionError('Missing actual OWNER capability assertions: ' + name)
                results[name] = report
    result = {'status': 'passed', 'assertions': sum(r['assertions'] for r in current.values()),
              'unknown_capability_retry_verified': True, 'true_unsupported_cache_preserved': True,
              'single_residual_capability_retry_verified': True,
              'published79_unknown_capability_poisoning_reproduced': True,
              'actual_owner_executed': True, 'physical_android_tested': False,
              'gpu_transport_fixture_used': False, 'cases': current, 'published79_baseline_cases': baseline}
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    jar = os.environ.get('ULIKE_ANDROID_JAR')
    if jar is None:
        candidates = (jdk.parent.parent / 'tools/android.jar', Path.cwd() / 'tools/android.jar')
        jar = next((p for p in candidates if p.is_file()), None)
    if jar is None:
        raise AssertionError('Set ULIKE_ANDROID_JAR to the pinned Android API jar')
    jar = Path(jar).resolve()
    sys.path.insert(0, str(source))
    builder = module('gpu1980_current_compile', source / 'build1979.py')
    actual = list(builder.compile_inputs().values()) + [builder.production_path(n) for n in builder.PRODUCTION]
    actual_pins = {p.relative_to(source).as_posix(): sha(p) for p in actual}
    classes, fixtures = work / 'production-classes', work / 'fixture-classes'
    classes.mkdir(exist_ok=True)
    fixtures.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
         '-bootclasspath', jar, '-d', classes, *actual], work / 'production-compile.log')
    generated = work / 'generated'
    generated.mkdir(exist_ok=True)
    queue = module('gpu1980_android_queue', source / 'host_qualification1967.py')
    fixture_sources = []
    for name, text in queue.FIXTURES.items():
        if not name.startswith('android/'):
            continue
        path = generated / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        fixture_sources.append(path)
    fixture_sources += [source / name for name in (
        'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
        'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
        'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java',
        'tests1980/GpuTransportFaults1980.java', 'tests1980/GpuFailure1980Test.java',
        'tests1980/StrongCleanup1980Test.java')]
    cp = str(classes) + os.pathsep + str(jar)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', cp,
         '-d', fixtures, *fixture_sources], work / 'fixture-compile.log')
    cp = str(fixtures) + os.pathsep + cp
    command = [jdk / 'bin/java', '-ea', '-Xmx1g', '-XX:ActiveProcessorCount=4',
               '-Djava.library.path=' + str(work / 'unavailable'), '-cp', cp,
               'com.hiro.ulike.GpuFailure1980Test']
    ref, ref_pins = reference(source)
    old = work / 'published79-classes'
    old.mkdir(exist_ok=True)
    run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-cp', str(classes) + os.pathsep + str(jar),
         '-d', old, *[ref / name for name in ref_pins['files']]], work / 'published79-compile.log')
    capability = capability_test(source, work / 'capability', jdk, jar, classes, old)
    baseline_cp = str(fixtures) + os.pathsep + str(old) + os.pathsep + str(classes) + os.pathsep + str(jar)
    baseline_command = [*command[:command.index('-cp') + 1], baseline_cp, 'com.hiro.ulike.GpuFailure1980Test']
    before = {}
    for family in FAMILIES:
        before[family] = json.loads(run([*baseline_command, family, 'probe', '0', 'true'],
                                      work / ('published79-' + family + '.log')).strip().splitlines()[-1])
    reports = {}
    for family in FAMILIES:
        for scenario, faults in (('probe', (0, 1, 2, 3, 4, 5)), ('foreground', (0, 2, 3)), ('busy', (0,))):
            for fault in faults:
                name = family + '-' + scenario + '-' + str(fault)
                report = json.loads(run([*command, family, scenario, fault, 'false'], work / (name + '.log')).strip().splitlines()[-1])
                if report.get('status') != 'passed' or report.get('assertions', 0) < 5:
                    raise AssertionError('Missing current fault assertions: ' + name)
                reports[name] = report
                (work / 'completed.json').write_text(json.dumps(reports, indent=2) + '\n')
    cleanup = {}
    for label, path, is_old in [('published79', baseline_cp, 'true'), ('current80', cp, 'false')]:
        dump = work / (label + '-strong.bin')
        cleanup[label] = json.loads(run([jdk / 'bin/java', '-ea', '-Xmx1g', '-XX:ActiveProcessorCount=4',
                                        '-Djava.library.path=' + str(work / 'unavailable'), '-cp', path,
                                        'com.hiro.ulike.StrongCleanup1980Test', dump, is_old],
                                       work / (label + '-strong.log')).strip().splitlines()[-1])
    if (work / 'published79-strong.bin').read_bytes() != (work / 'current80-strong.bin').read_bytes():
        raise AssertionError('Strong cleanup changed current pixels or model float bits')
    cleanup.update(status='passed', differing_bytes=0, output_sha256=sha(work / 'current80-strong.bin'),
                   bytes=(work / 'current80-strong.bin').stat().st_size, physical_android_tested=False)
    if actual_pins != {p.relative_to(source).as_posix(): sha(p) for p in actual}:
        raise AssertionError('Current Java closure changed during GPU fault execution')
    reference(source)
    result = {'status': 'passed', 'assertions': sum(r['assertions'] for r in reports.values()) + cleanup['current80']['assertions'] + capability['assertions'],
              'physical_android_tested': False, 'device_speedup_verified': False,
              'gpu_transport_failure_retry_verified': True, 'real_pixel_mismatch_persistence_preserved': True,
              'gpu_busy_and_cancellation_preserve_proofs': True, 'gpu_failure_cooldown_and_three_retries_verified': True,
              'gpu_failed_outputs_never_committed': True, 'fixture_classes_in_runtime': False,
              'strong_dead_route_removal_output_exact': True, 'strong_cleanup': cleanup,
              'gpu_unknown_capability_retry_verified': True, 'gpu_true_unsupported_cache_preserved': True,
              'single_residual_capability_retry_verified': True, 'capability': capability,
              'published79_null_failure_permanent_rejection_reproduced': True,
              'published79_baseline_cases': before, 'reference_manifest_sha256': REFERENCE_PIN,
              'production_source_sha256': actual_pins, 'runner_source_sha256': sha(__file__),
              'cases': reports}
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), indent=2))
