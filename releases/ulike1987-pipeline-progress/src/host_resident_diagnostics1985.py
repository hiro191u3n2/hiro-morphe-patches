#!/usr/bin/env python3
"""Preserve .84 Resident gates and verify capture-scoped snapshot ownership.

The published .84 copy budget and Resident producer reproduce the obsolete,
unstarted-owner blockage. Current production must release only such tentative
owners, preserve started-copy exclusion and every existing byte/quality gate,
and record the exact copy-admission decision without another live lookup.
Qualification/bitmap peers are controlled; actual queue ownership is tested by
the separate qualification runner. These are not Android GPU speed claims.
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


BASELINE84 = {
    'GpuSnapshotBudget1981.java': 'aeec62fba532a7425b20113576309ff8d48f67d446b9aa64902d5a87265af045',
    'GpuResident1976.java': '4a03ea43254ebce96167cde934b91b8c85f17738d33b9f9bc85a2ac45defb7f1',
}
RETAINED84 = {
    'host_resident_diagnostics1984.py': '93108568d19164148e483ed656374627a9c60e57c67178a823d48c32b83f7cc3',
    'host_pipeline_diagnostics1984.py': 'd00d4653039c803df583168a9188f5cd678e168fedd82c66fe1242d0e0106d96',
}
REQUIRED_FLAGS1985 = (
    'snapshot_unstarted_stale_owner1985', 'snapshot_started_owner_exclusion1985',
    'snapshot_copy_caps1985_preserved', 'snapshot_whole_family_fairness1985',
    'snapshot_exact_refusal_diagnostics1985', 'snapshot_decision_immutability1985',
    'resident_snapshot_epoch_handoff1985', 'resident_snapshot_begin_gate1985',
    'resident_snapshot_diagnostics_noninterference1985',
    'published84_stale_unstarted_copy_reproduced1985',
    'published84_resident_stale_copy_reproduced1985',
    'resident_all_legacy_gates1985_preserved',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def replace_once(body, old, new):
    if body.count(old) != 1:
        raise AssertionError('Frozen Resident peer adapter span changed: ' + old)
    return body.replace(old, new, 1)


def extend_resident_peers1985(body):
    """Add .85 APIs to an already adapted .84 controlled peer, not production."""
    timing = '''
 static int copyReason1985=-1,copyPreferred1985,copyActive1985=-1,copyCalls1985,copySinkFault1985;
 static boolean copyStarted1985;static long copyEpoch1985;
 static void residentCopy1985(Trace t,int r,int p,int a,boolean s,long e){
  if(t!=null){copyCalls1985++;copyReason1985=r;copyPreferred1985=p;copyActive1985=a;copyStarted1985=s;copyEpoch1985=e;
   t.copyReason1985=r;t.copyPreferred1985=p;t.copyActive1985=a;t.copyStarted1985=s;t.copyEpoch1985=e;}
  GpuQualification1961.fault1984(copySinkFault1985);
 }
'''
    body = replace_once(body, 'final class ProcessingTiming1947 {', 'final class ProcessingTiming1947 {' + timing)
    body = replace_once(body, 'static final class Trace {}', '''static final class Trace {
 int copyReason1985=-1,copyPreferred1985,copyActive1985=-1;boolean copyStarted1985;long copyEpoch1985;
}''')
    queue = '''
 static int reserveCalls1985,beginCalls1985,beginFault1985;
 static boolean beginReject1985;static Runnable beforeBegin1985,afterBegin1985;
 static void resetSnapshot1985(){
  reserveCalls1985=beginCalls1985=beginFault1985=0;beginReject1985=false;beforeBegin1985=afterBegin1985=null;
  ProcessingTiming1947.copyReason1985=ProcessingTiming1947.copyActive1985=-1;
  ProcessingTiming1947.copyPreferred1985=ProcessingTiming1947.copyCalls1985=ProcessingTiming1947.copySinkFault1985=0;
  ProcessingTiming1947.copyStarted1985=false;ProcessingTiming1947.copyEpoch1985=0;
 }
 static void captureChanged1985(){
  Reservation1984 previous=lastReservation1984;
  if(previous!=null&&previous.epochReclaim1985&&!previous.begun1985)previous.close();
 }
 public static Reservation1984 reserve1985(String k,long b){
  reserveCalls1985++;reservationCalls1984++;events1984.add("reserve");fault1984(reserveFault1984);
  boolean allowed=canQueue(k,b);int reason=allowed?0:declineReason1983;
  if(allowed&&(b>limit1984||held>limit1984-b)){allowed=false;reason=16;}
  final Reservation1984 result=new Reservation1984(k,b,allowed,reason);result.epochReclaim1985=true;lastReservation1984=result;
  if(afterDecision1983!=null)afterDecision1983.run();if(afterReserve1984!=null)afterReserve1984.run();
  return result;
 }
'''
    body = replace_once(body, 'final class GpuQualification1961 {', 'final class GpuQualification1961 {' + queue)
    begin = '''
  boolean epochReclaim1985,begun1985;
  public boolean begin1985(){
   beginCalls1985++;if(beforeBegin1985!=null)beforeBegin1985.run();fault1984(beginFault1985);
   if(beginReject1985||begun1985||!owns||epoch!=ProcessingTiming1947.epoch||cancel||Thread.currentThread().isInterrupted())return false;
   begun1985=true;if(afterBegin1985!=null)afterBegin1985.run();return true;
  }
'''
    body = replace_once(body, 'public static final class Reservation1984 implements AutoCloseable {',
                        'public static final class Reservation1984 implements AutoCloseable {' + begin)
    return replace_once(body, 'int reason=!current()?6:',
                        'int reason=!current()?6:epochReclaim1985&&!begun1985?21:')


def install_peer_adapter1985(retained):
    original = retained.adapt_resident_peers1984
    retained.adapt_resident_peers1984 = lambda body, source: extend_resident_peers1985(original(body, source))
    return retained


def run(command, log):
    result = subprocess.run([str(value) for value in command], capture_output=True, text=True, timeout=90)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + result.stdout + result.stderr)
    for line in reversed(result.stdout.splitlines()):
        if line.startswith('{'):
            report = json.loads(line)
            if report.get('status') != 'passed' or report.get('assertions', 0) <= 0:
                raise AssertionError('Failed or empty snapshot test report')
            return report
    return None


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    sys.path.insert(0, str(source))
    tests = source / 'tests1985/resident_snapshot'
    for name, expected in BASELINE84.items():
        if sha(tests / 'baseline84' / name) != expected:
            raise AssertionError('Published .84 snapshot negative control changed: ' + name)
    for name, expected in RETAINED84.items():
        if sha(source / name) != expected:
            raise AssertionError('Historical .84 runner changed: ' + name)
    production = {name: sha(source / name) for name in BASELINE84}
    retained = install_peer_adapter1985(module('resident85_retained84', source / 'host_resident_diagnostics1984.py'))
    legacy = retained.test(source, work / 'retained84', jdk=jdk, ndk=ndk)
    reports = {}
    for mode in ('published84', 'current85'):
        classes = work / (mode + '-snapshot')
        classes.mkdir(exist_ok=True)
        budget = tests / 'baseline84/GpuSnapshotBudget1981.java' if mode == 'published84' else source / 'GpuSnapshotBudget1981.java'
        inputs = [budget, tests / 'fixtures/ProcessingTiming1947.java', tests / 'SnapshotOwnership1985Test.java']
        if mode == 'current85':
            inputs.append(tests / 'SnapshotDecision1985Test.java')
        run([jdk / 'bin/javac', '--release', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-d', classes, *inputs],
            work / (mode + '-snapshot-compile.log'))
        reports[mode + '_snapshot'] = run([jdk / 'bin/java', '-ea', '-cp', classes,
            'com.hiro.ulike.SnapshotOwnership1985Test', mode], work / (mode + '-snapshot.log'))
        if mode == 'current85':
            reports['snapshot_decisions85'] = run([jdk / 'bin/java', '-ea', '-cp', classes,
                'com.hiro.ulike.SnapshotDecision1985Test'], work / 'snapshot-decisions85.log')

    for mode in ('published84', 'current85'):
        classes = work / (mode + '-resident')
        classes.mkdir(exist_ok=True)
        baseline = tests / 'baseline84' if mode == 'published84' else source
        inputs = [baseline / 'GpuResident1976.java', baseline / 'GpuSnapshotBudget1981.java',
                  source / 'ResidentProof1978.java', source / 'PipelineDiagnostics1982.java',
                  work / 'retained84/Bitmap.java', work / 'retained84/ResidentPeers1982.java',
                  work / 'retained84/current/ResidentDiagnostics1982Test.java',
                  source / 'tests1984/resident/ResidentTicket1984Test.java', tests / 'ResidentSnapshot1985Test.java']
        run([jdk / 'bin/javac', '--release', '8', '-Xlint:-options', '-encoding', 'UTF-8', '-d', classes, *inputs],
            work / (mode + '-resident-compile.log'))
        reports[mode + '_resident'] = run([jdk / 'bin/java', '-ea', '-cp', classes,
            'com.hiro.ulike.ResidentSnapshot1985Test', mode], work / (mode + '-resident.log'))

    flags = {}
    for report in reports.values():
        for name in REQUIRED_FLAGS1985:
            if report.get(name) is True:
                flags[name] = True
    if legacy['status'] != 'passed' or not all(legacy.get(name) is True for name in (
            'resident_retained_proof_gates1984', 'resident_reservation_before_copy1984',
            'resident_ticket_cleanup1984', 'resident_commit_races1984',
            'resident_attempt_diagnostics_noninterference1984')):
        raise AssertionError('A retained .84 Resident proof/ownership gate did not execute')
    flags['resident_all_legacy_gates1985_preserved'] = True
    if any(flags.get(name) is not True for name in REQUIRED_FLAGS1985):
        raise AssertionError('Missing executed snapshot .85 flag: ' + str(set(REQUIRED_FLAGS1985) - set(flags)))
    if production != {name: sha(source / name) for name in BASELINE84}:
        raise AssertionError('Resident/snapshot production changed while its tests were running')
    if any(sha(source / name) != expected for name, expected in RETAINED84.items()):
        raise AssertionError('A historical runner was modified during snapshot testing')
    result = dict(legacy)
    result.update(status='passed', assertions=legacy['assertions'] + sum(value['assertions'] for value in reports.values()),
                  cases={'retained84': legacy, **reports}, baseline84_source_sha256=BASELINE84,
                  production_source_sha256=production, retained84_runner_sha256=RETAINED84,
                  runner_source_sha256=sha(__file__), physical_android_tested=False,
                  actual_queue_engine_executed=False, device_speedup_verified=False, **flags)
    result['fixture_adapters1985'] = [
        'Add only reserve1985, explicit begin1985 and scalar residentCopy1985 APIs to copies of the retained controlled peers. Existing .82/.83/.84 test expectations and production test inputs stay intact.',
        'The real current Copy budget and Resident producer execute. Published .84 producers/budget are immutable negative controls; no assertion treats a single ordinary round-robin deferral as a device failure.',
        'Current ticket materialization is guarded by Copy.begin then Reservation.begin1985. The controlled peer exposes cancellation barriers; the separate qualification runner executes actual shared-owner accounting.',
    ]
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
