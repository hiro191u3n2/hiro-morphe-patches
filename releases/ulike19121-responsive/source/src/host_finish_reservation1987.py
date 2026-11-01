#!/usr/bin/env python3
"""Execute actual Finish snapshot ownership with the real qualification queue.

Published .86 is the immutable negative control. Only Android Bitmap allocation
boundaries and the ordinary queue environment are controlled. No Finish method,
queue method, copy budget, renderer, elapsed clock or safety gate is rewritten.
The queue remains save-busy; this is not an Android performance measurement.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import subprocess
import sys

BASELINE86_SHA256 = 'c3a3a7fff8ecbd164095f23152c83ceb9a1857b2431d911ddb520ecbc4c93f19'
FINISH_START = '''    static Bitmap finish(final Bitmap source,final int rotation,final int width,final int height,
            final QualityPixels1932.Plan outputPlan,final boolean refreshNoise,
            final ProcessingTiming1947.Trace trace) {'''
FINISH_END = '    /** One idle source can first establish the ordinary finish dependency and'
REQUIRED_FLAGS = (
    'finish_published86_priority_loss_reproduced1987',
    'finish_primary_reservation_handoff_verified1987',
    'finish_source_scope_inverse_verified1987',
    'finish_reservation_before_bitmap_copy_verified1987',
    'finish_atomic_capacity_race_verified1987',
    'finish_primary_queue_fairness_verified1987',
    'finish_capture_cancellation_ownership_verified1987',
    'finish_exact_and_existing_proof_arrivals_verified1987',
    'finish_allocation_failures_and_copy_limit_verified1987',
    'finish_memory_and_retry_gates_verified1987',
)


def sha(value):
    return hashlib.sha256(value if isinstance(value, bytes) else Path(value).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def inverse_chain1987(current, baseline):
    """Revert only the reviewed foreground Finish producer to exact .86."""
    if sha(baseline) != BASELINE86_SHA256:
        raise AssertionError('Published .86 whole-chain source changed')
    current, baseline = current.decode(), baseline.decode()
    for body in (current, baseline):
        if body.count(FINISH_START) != 1 or body.count(FINISH_END) != 1:
            raise AssertionError('Finish producer scope is not unique')
    first, last = current.index(FINISH_START), current.index(FINISH_END)
    old_first, old_last = baseline.index(FINISH_START), baseline.index(FINISH_END)
    restored = (current[:first] + baseline[old_first:old_last] + current[last:]).encode()
    if restored != baseline.encode():
        raise AssertionError('An unreviewed chain method or pixel boundary changed')
    return restored


def replace_once(body, old, new):
    if body.count(old) != 1:
        raise AssertionError('Historical fixture adapter boundary changed: ' + old)
    return body.replace(old, new, 1)


def bitmap_fixture(body):
    body = replace_once(body, 'public final class Bitmap {', '''public final class Bitmap {
 public static Runnable beforeCopy1987,afterCopy1987;
 public static int copyFault1987,copyCalls1987;
 public int recycleCalls1987;''')
    body = replace_once(body, ' public Bitmap copy(Config c,boolean m){', ''' public Bitmap copy(Config c,boolean m){
  copyCalls1987++;if(beforeCopy1987!=null)beforeCopy1987.run();
  if(copyFault1987==1)return null;
  if(copyFault1987==2)throw new OutOfMemoryError("controlled Finish copy allocation");
  if(copyFault1987==3)throw new IllegalStateException("controlled Finish copy failure");
  if(copyFault1987==4)throw new LinkageError("controlled Finish copy linkage");
  if(copyFault1987==5)throw new AssertionError("controlled fatal Finish copy failure");''')
    body = replace_once(body, '  HostAudit1932.event("copy:"+width+"x"+height);return b;',
                        '  HostAudit1932.event("copy:"+width+"x"+height);if(afterCopy1987!=null)afterCopy1987.run();return b;')
    return replace_once(body, ' public void recycle(){recycled=true;',
                        ' public void recycle(){recycleCalls1987++;recycled=true;')


def queue_fixture(body):
    body = replace_once(body, 'final class ProcessingTiming1947 {',
                        'final class ProcessingTiming1947 {static final class Trace {} static void detailRoute1976(Trace t,int r){}')
    return replace_once(body, 'final class GpuNoise1960 {', '''final class GpuNoise1960 {
 static boolean fits1987=true;
 static boolean supports(int program){return true;}
 static boolean workspaceFits(long bytes){return fits1987&&bytes>=0&&bytes<=512L*1024*1024;}''')


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    jar = Path(os.environ.get('ULIKE_ANDROID_JAR', str(jdk.parent / 'android.jar'))).resolve()
    sys.path.insert(0, str(source))
    local = source / 'tests1987/finish_reservation'
    baseline = local / 'baseline86/GpuChain1961.java'
    chain = source / 'GpuChain1961.java'
    restored = inverse_chain1987(chain.read_bytes(), baseline.read_bytes())
    builder = module('finish87_compile_closure', source / 'build1985.py')
    inputs = list(dict.fromkeys(list(builder.compile_inputs().values()) +
                              [builder.production_path(name) for name in builder.PRODUCTION]))
    tracked = [chain, baseline, source / 'GpuQualification1961.java', source / 'GpuSnapshotBudget1981.java',
               local / 'FinishReservation1987Test.java', source / 'host_qualification1981.py',
               source / 'tests1984/qualification/QualificationProgress1984Test.java',
               source / 'tests1984/qualification/CameraTrace1965.java',
               source / 'tests/pipeline1942-fixtures/android/graphics/Bitmap.java',
               source / 'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
               source / 'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java', Path(__file__).resolve()]
    before = {str(path): sha(path) for path in tracked}
    commands = []

    def run(label, command):
        command = list(map(str, command));commands.append(dict(label=label, argv=command))
        result = subprocess.run(command, text=True, capture_output=True, timeout=240)
        (work / (label + '.log')).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise AssertionError(label + '\n' + (result.stdout + result.stderr)[-16000:])
        return result.stdout

    production, fixtures = work / 'production', work / 'fixtures'
    production.mkdir(exist_ok=True);fixtures.mkdir(exist_ok=True)
    compile_inputs = []
    input_pins = {}
    # Snapshot this compile closure, allowing independent agents to edit other
    # stages without an inconsistent javac input graph or touching old fixtures.
    for path in inputs:
        relative = path.relative_to(source);raw = path.read_bytes();input_pins[str(relative)] = sha(raw)
        copied = work / 'production-source' / relative;copied.parent.mkdir(parents=True, exist_ok=True);copied.write_bytes(raw)
        compile_inputs.append(copied)
    run('production-compile', [jdk / 'bin/javac', '-source', '8', '-target', '8', '-Xlint:-options',
        '-encoding', 'UTF-8', '-bootclasspath', jar, '-d', production, *compile_inputs])
    fixture_inputs = []
    inherited = module('finish87_queue_fixtures', source / 'host_qualification1981.py')
    for name, body in inherited.FIXTURES.items():
        if name.endswith('/QueueHost1967.java'):
            continue
        if name.endswith('/QueueFixtures1967.java'):
            body = queue_fixture(body)
        path = work / 'fixture-source' / name;path.parent.mkdir(parents=True, exist_ok=True);path.write_text(body)
        fixture_inputs.append(path)
    bitmap = work / 'fixture-source/android/graphics/Bitmap.java';bitmap.parent.mkdir(parents=True, exist_ok=True)
    bitmap.write_text(bitmap_fixture((source / 'tests/pipeline1942-fixtures/android/graphics/Bitmap.java').read_text()))
    fixture_inputs += [bitmap, source / 'tests/pipeline1942-fixtures/android/graphics/ColorSpace.java',
                       source / 'tests/pipeline1942-fixtures/com/hiro/ulike/HostAudit1932.java',
                       source / 'tests1984/qualification/CameraTrace1965.java',
                       source / 'tests1984/qualification/QualificationProgress1984Test.java',
                       local / 'FinishReservation1987Test.java']
    cp = os.pathsep.join(map(str, (production, jar)))
    run('fixture-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
        '-cp', cp, '-d', fixtures, *fixture_inputs])
    reports = {}
    for mode, raw in (('published86', baseline.read_bytes()), ('current87', chain.read_bytes())):
        directory = work / mode;directory.mkdir(exist_ok=True)
        path = directory / 'GpuChain1961.java';path.write_bytes(raw)
        run(mode + '-compile', [jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8',
            '-cp', cp, '-d', directory, path])
        runtime = os.pathsep.join(map(str, (fixtures, directory, production, jar)))
        output = run(mode + '-run', [jdk / 'bin/java', '-ea', '-Xmx512m', '-cp', runtime,
            'com.hiro.ulike.FinishReservation1987Test', mode])
        report = json.loads(output.strip().splitlines()[-1])
        if report.get('status') != 'passed' or report.get('assertions', 0) <= 0:
            raise AssertionError('No executed Finish reservation evidence')
        reports[mode] = report
    if reports['published86'].get('finish_published86_priority_loss_reproduced1987') is not True:
        raise AssertionError('Published priority loss was not reproduced')
    if reports['current87'].get('finish_primary_reservation_handoff_verified1987') is not True:
        raise AssertionError('Current primary reservation handoff was not verified')
    required_cases = ('reserved_primary_slot', 'bytes_and_slot_before_allocation', 'capacity_race_without_reacquisition',
                      'actual_primary_fairness_and_retirement', 'capture_epoch_and_started_owner_lifetime',
                      'authoritative_proof_arrivals', 'allocation_failure_and_single_copy_cap',
                      'preallocation_capacity_workspace_and_retry_gates')
    if any(report['cases'].get(name, 0) <= 0 for report in reports.values() for name in required_cases):
        raise AssertionError('A required reservation scenario did not execute')
    if before != {str(path): sha(path) for path in tracked}:
        raise AssertionError('Finish, queue, snapshot budget or historical fixture changed during this regression')
    result = dict(status='passed', assertions=sum(report['assertions'] for report in reports.values()),
        cases=reports, **{flag: True for flag in REQUIRED_FLAGS}, source_hashes=before,
        production_compile_input_sha256=input_pins, baseline86_source_sha256=BASELINE86_SHA256,
        reviewed_chain_inverse_sha256=sha(restored), fixture_source_sha256={str(p): sha(p) for p in fixture_inputs},
        actual_finish_producer_executed1987=True, actual_qualification_queue_executed1987=True,
        actual_snapshot_budget_executed1987=True, production_method_adapters_used=False,
        physical_android_tested=False, device_speedup_verified=False, actual_gpu_transport_executed=False,
        fixture_scope='Unmodified real Finish producer, qualification queue and Copy budget. Save-busy and GPU-capability peers, Android Bitmap before/after allocation hooks, exact legacy negative control. No runtime fixture enters the release.')
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    (work / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
