#!/usr/bin/env python3
"""Current Strong GPU qualification, exact kernels and measured cohort gates.

The original foreground-proof fixtures remain in the source archive. This
runner executes the .78 semantics and reports only checks actually performed.
Mesa timings with controlled contention validate decisions, not phone speedups.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json

RUNNERS = (
    ('tuning', 'host_tuning1978'),
    ('native_facade', 'host_native_facade1978'),
    ('programs', 'host_gpu_programs1978'),
    ('program_preservation', 'host_program_preservation1978'),
    ('precision', 'host_precision1978'),
)

FLAGS = {
    'tuning': (
        'twentyfour_candidate_tuning1978_verified',
        'idle_two_full_argb_confidence_policy1978_verified',
        'idle_snapshot_cancel_and_memory1978_verified',
        'idle_upload_reuse1978_verified',
        'new_gpu_balanced_five_percent_gate1978_verified',
        'legacy_exact_rejections1978_preserved',
        'worker_bound_speed_retry1978_verified',
        'legacy_gpu_failure_direct_recovery1978_verified',
    ),
    'native_facade': (
        'deferred_foreground_proof1978_verified',
        'certified_readback_reuse1978_verified',
        'foreground_failure_cancel_memory1978_verified',
        'private_partial_output1978_not_committed',
        'parallel_cohort1978_gate_verified',
        'actual_production_workers1978_executed',
        'direct_policy_command_graph1978_actual_jni',
        'cohort_cancel_memory_and_mismatch1978_verified',
        'mixed_route_admission1978_verified',
        'actual_parallel_cpu_fallback1978_verified',
    ),
    'programs': (
        'pow2_exact1978_actual_jni_verified',
        'retained_strong_programs1978_exact_verified',
        'direct_policy1978_full_exact_verified',
    ),
    'program_preservation': ('programs0_44_preserved1978_verified',),
    'precision': (
        'exact_division_regressions_passed',
        'legacy_program_semantics_preserved',
        'gpu_safety_gates_preserved',
        'pow2_exact1978_raw_bits_verified',
        'pow2_arm64_and_gpu_boundaries_verified',
        'pow2_normal_and_subnormal_paths_executed',
    ),
}

def test(source, work, jdk=None, ndk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    work.mkdir(parents=True, exist_ok=True)
    tracked = [source / (name + '.py') for _, name in RUNNERS]
    def pins():
        return {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    before = pins()
    tests, flags = {}, {}
    for label, name in RUNNERS:
        spec = importlib.util.spec_from_file_location('gpu_speed1978_' + label, source / (name + '.py'))
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        result = module.test(source, work / label, jdk, ndk)
        if result.get('status') != 'passed' or result.get('assertions', 0) <= 0:
            raise AssertionError('Incomplete executed GPU regression: ' + label)
        for flag in FLAGS[label]:
            if result.get(flag) is not True:
                raise AssertionError('Missing executed GPU gate: ' + label + '.' + flag)
            flags[flag] = True
        if result.get('physical_android_tested') is True or result.get('device_speedup_verified') is True:
            raise AssertionError('Host regression must not claim physical-device measurements')
        tests[label] = result
        (work / 'completed-groups.json').write_text(json.dumps(list(tests), indent=2) + '\n')
    if before != pins():
        raise AssertionError('GPU regression runner changed during execution')
    result = dict(
        status='passed', assertions=sum(x['assertions'] for x in tests.values()),
        tests=tests, **flags,
        physical_android_tested=False, device_speedup_verified=False,
        performance_contract='two AB/BA full-output representative cohorts on the actual bounded worker pool; includes native setup, transfer, GPU-bank wait, readback and join; not whole-photo timing',
        controlled_contention_and_faults='test-only JNI delay/readback controls and frozen CPU reference holders; production engine, shaders, facade and worker pool execute unchanged',
        old_foreground_proof_expectations_executed=False,
        retained_contracts='full ARGB, confidence and canonical policy; cropped halos; both native banks; exact negatives; cancellation; memory refusal; private failed output; retained program semantics',
        runner_sha256=before,
    )
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk, args.ndk), indent=2))
