#!/usr/bin/env python3
"""Exercise the real .84 Resident producer with explicit ticket-race peers.

The .81 diagnostic negative controls and every unmodified .82/.83 assertion
still execute against their historical implementations. Only the current
fixture copies adapt the deliberately replaced two-preflight scheduling
contract. Pixel, CPU-reference, exact-rejection and terminal-outcome checks
remain present. This runner does not claim to execute the actual queue engine
or an Android GPU; those have independent qualification/native runners.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil

from host_resident_diagnostics1983 import adapt_resident_peers1983, run, parsed


def replace_once(body, old, new):
    if body.count(old) != 1:
        raise AssertionError('Historical fixture adapter anchor changed: ' + old)
    return body.replace(old, new)


def adapt_resident_peers1984(body, source):
    body = adapt_resident_peers1983(body)
    peer = (Path(source) / 'tests1984/resident/ReservationPeer1984.txt').read_text()
    body = replace_once(body, 'final class GpuQualification1961 {',
                        'final class GpuQualification1961 {' + peer)
    return replace_once(body, 'queuedJobs=queued==null?0:1;',
                        'queuedJobs=(queued==null?0:1)+reserved1984;')


def adapt_bitmap1984(body):
    body = replace_once(body, 'public final class Bitmap {', '''public final class Bitmap {
 public static Runnable beforeCopy1984;public int recycleCalls1984;
''')
    body = replace_once(body, 'public Bitmap copy(Config c,boolean mutable){check();copies++;',
                        'public Bitmap copy(Config c,boolean mutable){check();if(beforeCopy1984!=null)beforeCopy1984.run();copies++;')
    body = replace_once(body, 'public void recycle(){if(!recycled)',
                        'public void recycle(){recycleCalls1984++;if(!recycled)')
    return body


def adapt_retained82(body):
    body = replace_once(body, 'GpuQualification1961.drop();GpuQualification1961.records.clear();',
                        'GpuQualification1961.drop();GpuQualification1961.resetTicket1984();GpuQualification1961.records.clear();')
    # Once bytes are reserved, another job cannot steal that capacity. The
    # retained post-copy rejection case now injects a real commit-time proof
    # race; clone release and the original image assertions are unchanged.
    body = replace_once(body, 'GpuQualification1961.declineAt=2;recycled=Bitmap.recycles;',
                        'GpuQualification1961.commitReject1984=11;recycled=Bitmap.recycles;')
    return replace_once(body, 'terminal(6,"queue changed after copy")',
                        'terminal(6,"certificate arrived at reserved commit")')


def adapt_retained83(body):
    # The previous retention-budget race required making the copy first. The
    # same reason must now be returned by the reservation without a clone.
    body = replace_once(body, 'GpuQualification1961.declineAt=2;GpuQualification1961.declineReason1983=16;',
                        'GpuQualification1961.declineAt=1;GpuQualification1961.declineReason1983=16;')
    body = replace_once(body, '"post-copy retention race retains exact second decision"',
                        '"retention refusal retains exact reservation decision before copy"')
    body = replace_once(body, 'GpuQualification1961.queueCalls==2&&GpuQualification1961.scheduleCalls1983==0&&Bitmap.recycles==before+1',
                        'GpuQualification1961.queueCalls==1&&GpuQualification1961.scheduleCalls1983==0&&GpuQualification1961.commitCalls1984==0&&Bitmap.recycles==before')
    body = replace_once(body, '"post-copy rejection closes sole snapshot without re-admission"',
                        '"reserved budget refusal allocates no snapshot and performs no re-admission"')
    old = 'GpuQualification1961.queueCalls==2&&GpuQualification1961.scheduleCalls1983==1'
    if body.count(old) != 2:
        raise AssertionError('Historical two-preflight assertions changed')
    body = body.replace(old,
                        'GpuQualification1961.queueCalls==1&&GpuQualification1961.scheduleCalls1983==0&&GpuQualification1961.commitCalls1984==1')
    return replace_once(body, '"original two preflights and one schedule preserved"',
                        '"one reservation and one ownership-transferring commit replace the old preflights"')


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    retained = source / 'tests1982/pipeline'
    fixtures = source / 'tests1984/resident'
    reference = json.loads((source / 'tests1984/source-scope83.json').read_text())
    original83 = fixtures / 'reference83/GpuResident1976.java'
    if sha(original83) != reference['GpuResident1976.java']:
        raise AssertionError('Resident negative control is not the immutable published .83 source')
    for name in ('host_resident_diagnostics1983.py',
                 'tests1982/pipeline/ResidentPeers1982.java',
                 'tests1982/pipeline/android/graphics/Bitmap.java',
                 'tests1982/pipeline/ResidentDiagnostics1982Test.java',
                 'tests1983/qualification/ResidentDecision1983Test.java'):
        if sha(source / name) != reference[name]:
            raise AssertionError('Historical Resident source/test changed: ' + name)
    pins = json.loads((retained / 'baseline81/pins.json').read_text())
    for name, pin in pins['files'].items():
        path = retained / 'baseline81' / name
        if sha(path) != pin['sha256'] or path.stat().st_size != pin['bytes']:
            raise AssertionError('Historical .81 Resident baseline changed: ' + name)

    peer = work / 'ResidentPeers1982.java'
    peer.write_text(adapt_resident_peers1984((retained / 'ResidentPeers1982.java').read_text(), source))
    bitmap = work / 'Bitmap.java'
    bitmap.write_text(adapt_bitmap1984((retained / 'android/graphics/Bitmap.java').read_text()))
    current82 = work / 'current/ResidentDiagnostics1982Test.java'
    current82.parent.mkdir(exist_ok=True)
    current82.write_text(adapt_retained82((retained / 'ResidentDiagnostics1982Test.java').read_text()))
    current83 = work / 'current/ResidentDecision1983Test.java'
    current83.write_text(adapt_retained83((source / 'tests1983/qualification/ResidentDecision1983Test.java').read_text()))

    reports = {}
    for mode, resident in [('baseline81', retained / 'baseline81/GpuResident1976.java'),
                           ('published83', original83), ('current84', source / 'GpuResident1976.java')]:
        classes = work / (mode + '-classes')
        classes.mkdir(exist_ok=True)
        diagnostic = current82 if mode == 'current84' else retained / 'ResidentDiagnostics1982Test.java'
        decision = current83 if mode == 'current84' else source / 'tests1983/qualification/ResidentDecision1983Test.java'
        files = [resident, source / 'ResidentProof1978.java', source / 'GpuSnapshotBudget1981.java',
                 source / 'PipelineDiagnostics1982.java', bitmap, peer, diagnostic]
        if mode != 'baseline81':
            files += [decision, fixtures / 'ResidentTicket1984Test.java']
        run([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', classes, *files],
            work / (mode + '-compile.log'))
        reports[mode + '_retained82'] = parsed(run(
            [jdk / 'bin/java', '-ea', '-cp', classes, 'com.hiro.ulike.ResidentDiagnostics1982Test',
             'baseline' if mode == 'baseline81' else 'current'], work / (mode + '-retained82.log')))
        if mode != 'baseline81':
            reports[mode + '_decisions83'] = parsed(run(
                [jdk / 'bin/java', '-ea', '-cp', classes, 'com.hiro.ulike.ResidentDecision1983Test'],
                work / (mode + '-decisions83.log')))
            reports[mode + '_reservation84'] = parsed(run(
                [jdk / 'bin/java', '-ea', '-cp', classes, 'com.hiro.ulike.ResidentTicket1984Test', mode],
                work / (mode + '-reservation84.log')))

    current = reports['current84_reservation84']
    flags = ('resident_reservation_before_copy1984', 'resident_ticket_cleanup1984',
             'resident_commit_races1984', 'resident_attempt_diagnostics_noninterference1984',
             'resident_retained_proof_gates1984')
    for name in flags:
        if current.get(name) is not True:
            raise AssertionError('Missing executed Resident ticket gate: ' + name)
    if reports['published83_reservation84'].get('published83_unreserved_copy_reproduced1984') is not True:
        raise AssertionError('Published .83 unreserved-copy negative control not reproduced')
    for name in ('resident_terminal_decisions1983', 'resident_no_diagnostic_readmission1983',
                 'resident_snapshot_ownership1983'):
        if any(reports[mode + '_decisions83'].get(name) is not True for mode in ('current84', 'published83')):
            raise AssertionError('Retained Resident gate absent: ' + name)

    result = dict(status='passed', assertions=sum(report['assertions'] for report in reports.values()), cases=reports,
                  resident_terminal_decisions1983=True, resident_no_diagnostic_readmission1983=True,
                  resident_snapshot_ownership1983=True, resident_all_terminal_reasons1982=True,
                  published83_unreserved_copy_reproduced1984=True, resident_retained_fixture_adapters1984=True,
                  physical_android_tested=False, device_speedup_verified=False,
                  actual_queue_engine_executed=False, gpu_transport='controlled Java peers; no native/GPU execution claimed',
                  production_source_sha256=sha(source / 'GpuResident1976.java'),
                  published83_source_sha256=sha(original83),
                  retained_fixture_sha256={name: sha(source / name) for name in (
                      'tests1982/pipeline/ResidentPeers1982.java', 'tests1982/pipeline/android/graphics/Bitmap.java',
                      'tests1982/pipeline/ResidentDiagnostics1982Test.java',
                      'tests1983/qualification/ResidentDecision1983Test.java')},
                  adapter_fixture_sha256={path.name: sha(path) for path in (peer, bitmap, current82, current83)},
                  runner_source_sha256=sha(__file__), **{name: current[name] for name in flags})
    result['superseded_expectations1984'] = [
        'Current scheduling is one reservation before copy plus one ownership-transferring commit; old .83 two-preflight/schedule assertions still run unchanged against published .83.',
        'A current retained-memory refusal is injected at reservation and requires zero clones. The retained post-copy refusal becomes a commit-time certificate race; clone/original-image cleanup assertions remain.',
        'Controlled peers add ticket APIs and diagnostic sinks. Current production ResidentProof1978, GpuSnapshotBudget1981 and GpuResident1976 execute without numerical substitutions.'
    ]
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
