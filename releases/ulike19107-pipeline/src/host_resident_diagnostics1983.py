#!/usr/bin/env python3
"""Retain .82 Resident assertions with additive queue/timing fixture APIs."""
from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess


def adapt_resident_peers1983(body):
    """Only controlled peers change; no historical production/test file is edited."""
    timing = '''
 static int queueReason1983=-1,queueRetries1983,queueCount1983,runningCount1983;static long retryRemaining1983,queueHeld1983,queueRequested1983;
 static void residentExit1983(Trace t,long b,int r,int q,long remaining,int retries,int queued,int running,long held,long requested){
  residentExit1982(t,b,r);if(t!=null){queueReason1983=q;retryRemaining1983=remaining;queueRetries1983=retries;queueCount1983=queued;runningCount1983=running;queueHeld1983=held;queueRequested1983=requested;}}
'''
    old = 'final class ProcessingTiming1947 {'
    if body.count(old) != 1:
        raise AssertionError('Historical timing peer changed')
    body = body.replace(old, old + timing)
    queue = '''
 static int declineReason1983=13,scheduleReason1983=12,scheduleCalls1983;static Runnable afterDecision1983,afterSchedule1983;
 static final class QueueDecision1983 {
  final boolean accepted;final int reason,retries,queuedJobs,runningJobs;final long retryRemainingNanos,retainedBytes,requestedBytes;
  QueueDecision1983(boolean a,int r,long b){accepted=a;reason=r;retries=r==9?2:0;queuedJobs=queued==null?0:1;runningJobs=background?1:0;retryRemainingNanos=r==9?1234000000L:0;retainedBytes=held;requestedBytes=b;}
 }
 static QueueDecision1983 canQueue1983(String k,long b){boolean ok=canQueue(k,b);QueueDecision1983 d=new QueueDecision1983(ok,ok?0:declineReason1983,b);if(afterDecision1983!=null)afterDecision1983.run();return d;}
 static QueueDecision1983 schedule1983(String k,long b,Probe p){scheduleCalls1983++;boolean ok=schedule(k,b,p);QueueDecision1983 d=new QueueDecision1983(ok,ok?1:scheduleReason1983,b);if(afterSchedule1983!=null)afterSchedule1983.run();return d;}
'''
    old = 'final class GpuQualification1961 {'
    if body.count(old) != 1:
        raise AssertionError('Historical queue peer changed')
    return body.replace(old, old + queue)


def run(command, log):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=90)
    Path(log).write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(str(log) + '\n' + result.stdout + result.stderr)
    value = result.stdout.strip().splitlines()
    return value[-1] if value else ''


def parsed(text):
    value = json.loads(text)
    if value.get('status') != 'passed' or value.get('assertions', 0) <= 0:
        raise AssertionError('Failed or empty Resident assertions')
    return value


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    tests = source / 'tests1982/pipeline'
    original = tests / 'ResidentPeers1982.java'
    fixture = work / 'ResidentPeers1982.java'
    fixture.write_text(adapt_resident_peers1983(original.read_text()))
    reports = {}
    pins = json.loads((tests / 'baseline81/pins.json').read_text())
    for name, pin in pins['files'].items():
        path = tests / 'baseline81' / name
        if hashlib.sha256(path.read_bytes()).hexdigest() != pin['sha256'] or path.stat().st_size != pin['bytes']:
            raise AssertionError('Historical Resident baseline changed')
    for mode in ('current', 'baseline'):
        classes = work / (mode + '-classes')
        classes.mkdir(exist_ok=True)
        resident = source / 'GpuResident1976.java' if mode == 'current' else tests / 'baseline81/GpuResident1976.java'
        files = [resident, source / 'ResidentProof1978.java', source / 'GpuSnapshotBudget1981.java', source / 'PipelineDiagnostics1982.java',
                 tests / 'android/graphics/Bitmap.java', fixture, tests / 'ResidentDiagnostics1982Test.java']
        if mode == 'current':
            files.append(source / 'tests1983/qualification/ResidentDecision1983Test.java')
        run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, *files], work / (mode + '-compile.log'))
        reports[mode + '_retained82'] = parsed(run([jdk / 'bin/java', '-ea', '-cp', classes, 'com.hiro.ulike.ResidentDiagnostics1982Test', mode], work / (mode + '-retained82.log')))
        if mode == 'current':
            reports['decision_producer1983'] = parsed(run([jdk / 'bin/java', '-ea', '-cp', classes, 'com.hiro.ulike.ResidentDecision1983Test'], work / 'decision-producer83.log'))
    result = dict(status='passed', assertions=sum(v['assertions'] for v in reports.values()), cases=reports,
                  resident_terminal_decisions1983=True, resident_no_diagnostic_readmission1983=True,
                  resident_snapshot_ownership1983=True, resident_all_terminal_reasons1982=True,
                  physical_android_tested=False, device_speedup_verified=False,
                  production_source_sha256=hashlib.sha256((source / 'GpuResident1976.java').read_bytes()).hexdigest(),
                  retained_fixture_sha256=hashlib.sha256(original.read_bytes()).hexdigest(),
                  adapter_fixture_sha256=hashlib.sha256(fixture.read_bytes()).hexdigest(),
                  runner_source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest())
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
