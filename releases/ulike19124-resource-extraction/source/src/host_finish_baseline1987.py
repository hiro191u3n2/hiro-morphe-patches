#!/usr/bin/env python3
"""Retain .86 complete Finish proofs while reviewing the .87 queue-only span.

Only the preservation input is inverted to exact published .86 before the
historical .86 inverse runs. Both old .85 and current .87 runtime tests retain
their original qualification/renderer adapters and every existing assertion.
The separate host_finish_reservation1987 suite executes the real queue handoff.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json

RETAINED86_SHA256 = '57f133d9c5edbbe1a94f4f52a67635eb7496a66d35cbff22c7e0614f00ae2dfb'
REQUIRED_FLAGS = (
    'finish_published85_missing_legacy_negative1986',
    'finish_cpu_fallback_two_complete_proofs1986',
    'finish_actual_output_timing_boundaries1986',
    'finish_existing_legacy_gate1986_preserved',
    'finish_independent_candidate_failures1986',
    'finish_cpu_reference_stability1986',
    'finish_cancellation_and_ownership1986',
    'finish_exact_history1986_preserved',
    'finish_optional_diagnostics1986_noninterference',
    'finish_resident_baseline_handoff1986',
    'finish_pixel_render_methods1986_preserved',
    'finish_reviewed_source_inverse1986',
    'finish_reviewed_scheduling_inverse1987_verified',
    'finish_current_complete_baseline_suite1987_verified',
)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    result = importlib.util.module_from_spec(spec);spec.loader.exec_module(result)
    return result


def install_inverse1987(retained, source, observed):
    source = Path(source).resolve()
    if sha(source / 'host_finish_baseline1986.py') != RETAINED86_SHA256:
        raise AssertionError('Frozen .86 Finish baseline runner changed')
    queue = module('finish87_scheduling_inverse', source / 'host_finish_reservation1987.py')
    baseline86 = source / 'tests1987/finish_reservation/baseline86/GpuChain1961.java'
    original = retained.inverse_chain1986

    def inverse(current, baseline85):
        restored86 = queue.inverse_chain1987(current, baseline86.read_bytes())
        if hashlib.sha256(restored86).hexdigest() != queue.BASELINE86_SHA256:
            raise AssertionError('Finish scheduling inverse did not restore exact .86')
        restored85 = original(restored86, baseline85)
        observed.append(dict(current_sha256=hashlib.sha256(current).hexdigest(),
                             restored86_sha256=hashlib.sha256(restored86).hexdigest(),
                             restored85_sha256=hashlib.sha256(restored85).hexdigest()))
        return restored85

    retained.inverse_chain1986 = inverse
    return retained


def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    tracked = [source / n for n in ('host_finish_baseline1986.py', 'host_finish_reservation1987.py',
        'tests1987/finish_reservation/baseline86/GpuChain1961.java', 'GpuChain1961.java')]
    tracked.append(Path(__file__).resolve());pins = {str(p): sha(p) for p in tracked}
    observed = []
    retained = install_inverse1987(module('finish87_retained86', source / 'host_finish_baseline1986.py'), source, observed)
    result = retained.test(source, work / 'retained86', jdk=jdk, ndk=ndk)
    if len(observed) != 1 or any(result.get(flag) is not True for flag in retained.REQUIRED_FLAGS):
        raise AssertionError('The retained complete Finish proof suite and exact inverse must execute')
    if pins != {str(p): sha(p) for p in tracked}:
        raise AssertionError('Finish runtime or preservation inputs changed during execution')
    result.update(finish_reviewed_scheduling_inverse1987_verified=True,
                  finish_current_complete_baseline_suite1987_verified=True,
                  scheduling_inverse_execution1987=observed,
                  retained_finish86_sha256=RETAINED86_SHA256,
                  wrapper_input_sha256=pins, runner_source_sha256=sha(__file__))
    result['preservation_adapter1987'] = ('The one reviewed foreground Finish overload is inverted only for exact source preservation. '
        'Runtime classes and all .86 proof assertions execute current production. Real .87 queue behavior has its separate reservation suite.')
    (work / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True);parser.add_argument('--work', required=True)
    parser.add_argument('--jdk');parser.add_argument('--ndk');args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), ensure_ascii=False, indent=2))
