#!/usr/bin/env python3
"""Retain the complete backend suite while testing Resident reservation tickets.

The historical .82 runner, its .81 negative control and all 18 backend cases
still execute. Current Resident fixture copies use the reservation APIs and
replace only the superseded post-copy budget race with a commit-time proof
race. The full ticket test also runs against the current producer, with the
published .83 producer as an explicit unreserved-copy negative control.

Preservation uses the unchanged .83 exact GpuChain inverse. Only the explicit
scalar diagnostic edits in the protected Resident Probe.run are inverted; its
entire restored text must equal the pinned published .83 callback before the
unchanged .82 mathematical/source contract runs. Backend and Resident execution
compile the actual current production source, never the preservation copies.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import shutil
import sys


RESIDENT_BASELINE83_SHA256 = '1c290c4d1176d2c660c4b013df1535bf61aa10558d656ab100e56ed98f30d07c'
TICKET_FLAGS = (
    'resident_reservation_before_copy1984', 'resident_ticket_cleanup1984',
    'resident_commit_races1984', 'resident_attempt_diagnostics_noninterference1984',
    'resident_retained_proof_gates1984',
)
REQUIRED_FLAGS1984 = (
    'pipeline_resident_ticket_fixture_adapter1984',
    'pipeline_resident_ticket_contract1984',
    'pipeline_published83_unreserved_copy_negative1984',
    'pipeline_reviewed_resident_diagnostic_inverse1984',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


def inverse_resident_diagnostics1984(current, baseline):
    """Restore only a fixed diagnostic delta, requiring the whole old callback.

    This is not a replacement of a changed expected method hash. Every allowed
    span is literal, must occur exactly once in the actual callback, and the
    restored callback must equal the immutable published baseline byte for byte.
    The new reservation/commit ownership code outside this callback is retained
    in the preservation input and is exercised by the dedicated ticket cases.
    """
    if hashlib.sha256(baseline).hexdigest() != RESIDENT_BASELINE83_SHA256:
        raise AssertionError('Frozen .83 Resident source differs from its complete-file pin')
    current_text, baseline_text = current.decode('utf-8'), baseline.decode('utf-8')
    start = '                public void run(GpuQualification1961.Cancellation cancellation) {\n'
    end = '                public void close(){if(!owned.isRecycled())owned.recycle();}\n'

    def callback(body):
        if body.count(start) != 1 or body.count(end) != 1:
            raise AssertionError('Resident proof callback boundary changed')
        begin, finish = body.index(start), body.index(end)
        if finish <= begin:
            raise AssertionError('Resident proof callback boundaries are reversed')
        return body[begin:finish]

    observed = callback(current_text)
    restored = observed
    edits = [
        ('''                        if(cancellation.cancelled())return;
                        if(!GpuChain1961.opaqueResident1976(owned)){outcome1984("unsupported");return;}''',
         '                        if(cancellation.cancelled()||!GpuChain1961.opaqueResident1976(owned))return;'),
        ('if(!GpuNoise1960.workspaceFits(halfBytes)){outcome1984("memory_budget");return;}',
         'if(!GpuNoise1960.workspaceFits(halfBytes))return;'),
        ('                            progress1984("resident_baseline",0,1);\n', ''),
        ('if(baselineKey==null){GpuQualification1961.rejectSpeed(scheduledKey);outcome1984("baseline_unavailable");return;}',
         'if(baselineKey==null){GpuQualification1961.rejectSpeed(scheduledKey);return;}'),
        ('                            progress1984("resident_baseline",1,1);\n', ''),
        ('if(GpuQualification1961.exactRejected(resultKey)){outcome1984("quality_rejected");return;}',
         'if(GpuQualification1961.exactRejected(resultKey))return;'),
        ('''                            if(cancellation.cancelled())return;
                            if(!baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))){outcome1984("reference_unstable");return;}''',
         '                            if(cancellation.cancelled()||!baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity)))return;'),
        ('                            progress1984("resident_compare",trial,2);\n', ''),
        ('''                                if(cancellation.cancelled())return;
                                if(oracle==null){outcome1984("baseline_unavailable");return;}''',
         '                                if(cancellation.cancelled()||oracle==null)return;'),
        ('if(comparison==ResidentProof1978.MISMATCH){GpuQualification1961.rejectExact(resultKey);outcome1984("quality_mismatch");return;}',
         'if(comparison==ResidentProof1978.MISMATCH){GpuQualification1961.rejectExact(resultKey);return;}'),
        ('if(comparison!=ResidentProof1978.EXACT){outcome1984("comparison_incomplete");return;}',
         'if(comparison!=ResidentProof1978.EXACT)return;'),
        ('                            progress1984("resident_compare",trial+1,2);\n', ''),
        ('                            progress1984("resident_speed",trial,2);\n', ''),
        ('if(candidate==0&&ordinaryInput==null){outcome1984("baseline_unavailable");return;}',
         'if(candidate==0&&ordinaryInput==null)return;'),
        ('''                                    if(cancellation.cancelled())return;
                                    if(result==null){outcome1984(candidate==0?"baseline_unavailable":"execution_unavailable");return;}''',
         '                                    if(result==null||cancellation.cancelled())return;'),
        ('                            progress1984("resident_speed",trial+1,2);\n', ''),
        ('                        timings1984(baseline,candidateWorst);\n', ''),
        ('else {GpuQualification1961.rejectSpeed(resultKey);outcome1984("speed_condition");}',
         'else GpuQualification1961.rejectSpeed(resultKey);'),
    ]
    for new, old in edits:
        if restored.count(new) != 1:
            raise AssertionError('Reviewed Resident diagnostic inverse span changed: ' + new.splitlines()[0])
        restored = restored.replace(new, old, 1)
    if restored != callback(baseline_text):
        raise AssertionError('Resident diagnostic inverse did not reproduce the complete pinned .83 proof callback')
    return current_text.replace(observed, restored, 1).encode('utf-8'), hashlib.sha256(restored.encode()).hexdigest()


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent).resolve()
    sys.path.insert(0, str(source))
    original = source / 'host_pipeline_diagnostics1982.py'
    retained = module('pipeline84_retained82', original)
    inverse83 = module('pipeline84_exact_inverse83', source / 'host_pipeline_diagnostics1983.py')
    adapter = module('pipeline84_resident_adapter', source / 'host_resident_diagnostics1984.py')
    declaration83 = json.loads((source / 'tests1984/source-scope83.json').read_text())
    old_peer = source / 'tests1982/pipeline/ResidentPeers1982.java'
    old_bitmap = source / 'tests1982/pipeline/android/graphics/Bitmap.java'
    old_test = source / 'tests1982/pipeline/ResidentDiagnostics1982Test.java'
    reference83 = source / 'tests1984/resident/reference83/GpuResident1976.java'
    historical_names = (
        'host_pipeline_diagnostics1982.py', 'host_pipeline_diagnostics1983.py',
        'host_resident_diagnostics1983.py', 'tests1982/pipeline/ResidentPeers1982.java',
        'tests1982/pipeline/android/graphics/Bitmap.java', 'tests1982/pipeline/ResidentDiagnostics1982Test.java',
        'tests1982/pipeline/preserved81.json', 'tests1983/memory/baseline82/GpuChain1961.java',
    )
    historical_sha = {name: sha(source / name) for name in historical_names}
    for name, observed in historical_sha.items():
        if observed != declaration83[name]:
            raise AssertionError('Historical pipeline runner/contract/fixture changed: ' + name)
    if sha(reference83) != RESIDENT_BASELINE83_SHA256 or sha(reference83) != declaration83['GpuResident1976.java']:
        raise AssertionError('Published Resident negative control differs from the immutable .83 baseline')
    source_before = {path.name: sha(path) for path in source.glob('*.java')}

    copied = work / 'adapted'
    copied.mkdir(parents=True, exist_ok=True)
    copied_peer = copied / 'ResidentPeers1982.java'
    copied_peer.write_text(adapter.adapt_resident_peers1984(old_peer.read_text(), source))
    copied_bitmap = copied / 'Bitmap.java'
    copied_bitmap.write_text(adapter.adapt_bitmap1984(old_bitmap.read_text()))
    copied_test = copied / 'ResidentDiagnostics1982Test.java'
    copied_test.write_text(adapter.adapt_retained82(old_test.read_text()))
    ticket_test = source / 'tests1984/resident/ResidentTicket1984Test.java'
    execute, original_preservation = retained.run, retained.preservation
    replacements = {'peer': [], 'bitmap': [], 'current_test': [], 'ticket_test': []}
    inverse_observed = []

    def preserved_with_exact_inverse(current_source):
        if Path(current_source).resolve() != source:
            raise AssertionError('Unexpected production source during preservation')
        chain_current = (source / 'GpuChain1961.java').read_bytes()
        chain_baseline = (source / 'tests1983/memory/baseline82/GpuChain1961.java').read_bytes()
        chain_restored = inverse83.inverse_chain_capacity1983(chain_current, chain_baseline)
        resident_current = (source / 'GpuResident1976.java').read_bytes()
        resident_restored, callback_sha = inverse_resident_diagnostics1984(resident_current, reference83.read_bytes())
        declaration_name = 'tests1982/pipeline/preserved81.json'
        declaration = json.loads((source / declaration_name).read_text())
        tree = work / 'preservation-only'
        for name in set(declaration['complete_files']) | set(declaration['methods']) | {declaration_name}:
            path = tree / name
            path.parent.mkdir(parents=True, exist_ok=True)
            if name == 'GpuChain1961.java':
                path.write_bytes(chain_restored)
            elif name == 'GpuResident1976.java':
                path.write_bytes(resident_restored)
            else:
                if path.is_symlink():
                    path.unlink()
                elif path.exists():
                    raise AssertionError('Preservation adapter refuses to replace unexpected regular file: ' + str(path))
                path.symlink_to(source / name)
        inverse_observed.append(dict(chain=hashlib.sha256(chain_current).hexdigest(),
                                     resident=hashlib.sha256(resident_current).hexdigest(),
                                     restored_resident_callback=callback_sha))
        return original_preservation(tree)

    def adapted_run(command, log, timeout=180):
        resident_compile = str(old_peer) in [str(argument) for argument in command]
        current_compile = resident_compile and str(source / 'GpuResident1976.java') in [str(argument) for argument in command]
        selected = []
        for argument in command:
            kind = None
            replacement = argument
            if str(argument) == str(old_peer):
                kind, replacement = 'peer', copied_peer
            elif str(argument) == str(old_bitmap):
                kind, replacement = 'bitmap', copied_bitmap
            elif current_compile and str(argument) == str(old_test):
                kind, replacement = 'current_test', copied_test
            if kind:
                if not resident_compile or not any('javac' in str(value) for value in command):
                    raise AssertionError('Resident fixture substitution is restricted to its compilation')
                replacements[kind].append(str(log))
            selected.append(replacement)
        if current_compile:
            selected.append(ticket_test)
            replacements['ticket_test'].append(str(log))
        return execute(selected, log, timeout)

    retained.run, retained.preservation = adapted_run, preserved_with_exact_inverse
    result = retained.test(source, work, jdk=jdk, ndk=ndk)
    if {key: len(value) for key, value in replacements.items()} != {'peer': 2, 'bitmap': 2, 'current_test': 1, 'ticket_test': 1}:
        raise AssertionError('Both original Resident cases and the current ticket compilation must execute exactly once')
    if len(inverse_observed) != 1:
        raise AssertionError('The full original preservation contract must execute exactly once')
    expected = {'single-gpu', 'single-null', 'single-link', 'residual-gpu', 'residual-null', 'residual-link',
                'worker', 'worker-background', 'worker-oracle', 'worker-write-failure',
                'geometry-cpu', 'geometry-gpu', 'geometry-failure',
                'regions-cpu', 'regions-gpu', 'regions-failure', 'model', 'defaults'}
    if set(result['cases']) != expected | {'current_resident', 'baseline_resident'}:
        raise AssertionError('The inherited backend suite must retain its exact 18 backend and two Resident cases')

    current_ticket = retained.result(execute([jdk / 'bin/java', '-ea', '-cp', work / 'current-resident',
        'com.hiro.ulike.ResidentTicket1984Test', 'current84'], work / 'current-reservation84.log'))
    for flag in TICKET_FLAGS:
        if current_ticket.get(flag) is not True:
            raise AssertionError('Current Resident ticket gate did not execute: ' + flag)
    reference_classes = work / 'published83-resident'
    reference_classes.mkdir(exist_ok=True)
    execute([jdk / 'bin/javac', '--release', '8', '-encoding', 'UTF-8', '-d', reference_classes,
             reference83, source / 'ResidentProof1978.java', source / 'GpuSnapshotBudget1981.java',
             source / 'PipelineDiagnostics1982.java', copied_bitmap, copied_peer, old_test, ticket_test],
            work / 'published83-reservation-compile.log')
    published83_ticket = retained.result(execute([jdk / 'bin/java', '-ea', '-cp', reference_classes,
        'com.hiro.ulike.ResidentTicket1984Test', 'published83'], work / 'published83-reservation84.log'))
    if published83_ticket.get('published83_unreserved_copy_reproduced1984') is not True:
        raise AssertionError('Published .83 unreserved-copy negative control was not reproduced')

    if historical_sha != {name: sha(source / name) for name in historical_names}:
        raise AssertionError('Historical runner, contract or fixture changed during adapter execution')
    if source_before != {path.name: sha(path) for path in source.glob('*.java')}:
        raise AssertionError('Production sources changed during the full pipeline ticket tests')
    result['cases'].update(current_reservation84=current_ticket, published83_reservation84=published83_ticket)
    result['assertions'] += current_ticket['assertions'] + published83_ticket['assertions']
    result.update({flag: True for flag in REQUIRED_FLAGS1984})
    result.update(pipeline_retained_backend_suite1983=True,
                  pipeline_resident_fixture_adapter1983=True,
                  pipeline_reviewed_capacity_inverse1983=True,
                  reviewed_chain_baseline82_sha256=inverse83.CHAIN_BASELINE82_SHA256,
                  current_chain_preservation_input_sha256=inverse_observed[0]['chain'],
                  current_resident_preservation_input_sha256=inverse_observed[0]['resident'],
                  reviewed_resident_baseline83_sha256=RESIDENT_BASELINE83_SHA256,
                  restored_resident_callback_sha256=inverse_observed[0]['restored_resident_callback'],
                  retained_source_sha256=historical_sha,
                  fixture_adapter_sha256={path.name: sha(path) for path in (copied_peer, copied_bitmap, copied_test)},
                  fixture_compile_substitutions=replacements,
                  actual_queue_engine_executed_in_resident_cases=False,
                  resident_ticket_runner_source_sha256=sha(source / 'host_resident_diagnostics1984.py'),
                  runner_source_sha256=sha(__file__))
    result['superseded_expectations1984'] = [
        'Only the current copied .82 Resident test changes a post-copy retained-budget race into a commit-time certificate race. Ownership, cleanup, pixels, CPU proof, exact rejection and terminal checks remain.',
        'The frozen .81 Resident diagnostic negative control uses the original test without expectation changes. Published .83 runs the unreserved-copy negative control; the current producer runs the complete ticket contract.',
        'The unchanged .83 exact capacity inverse and a fixed scalar-only Resident diagnostic inverse are preservation inputs only. The complete restored Resident callback must equal the pinned published .83 callback.',
        'The original full production closure, all 18 backend cases, native residual arithmetic and the entire original mathematical/gate declaration execute. Controlled GPU transport is not an Android device or speed benchmark.',
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
