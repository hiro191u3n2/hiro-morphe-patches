#!/usr/bin/env python3
"""Execute production H16-H23 session GLES/JNI against pinned Java pixels.

The shader and native translation unit are unmodified production sources. Mesa
surfaceless EGL is an actual host GPU implementation, not a substituted Java
backend. H17's standalone policy oracle and H20's bitmap ownership integration
remain separate suites, so their assertion counts are not duplicated here.
H22/H23 have distinct dispatch/readback counters. A host-only link wrapper
exercises late unmap failure in isolated JVMs; ARM64 production is unmodified.
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

import host_moire1951
from host_common1953 import pregpu1950_root


def run(command, log, env=None, timeout=180):
    completed = subprocess.run(list(map(str, command)), text=True,
                               capture_output=True, env=env, timeout=timeout)
    Path(log).write_text(completed.stdout + completed.stderr)
    if completed.returncode:
        raise RuntimeError(completed.stdout[-3000:] + completed.stderr[-12000:])
    return completed.stdout


def test(root, work, jdk=None, ndk=None):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'host-finishgpu1953'
    work.mkdir(parents=True, exist_ok=True)
    baseline = pregpu1950_root(root)
    java = str(Path(jdk) / 'bin/java') if jdk else os.environ.get('ULIKE_JAVA') or shutil.which('java')
    if not java:
        raise RuntimeError('Java runtime required for independently compiled pixel oracles')
    old_javac = os.environ.get('ULIKE_JAVAC')
    if jdk:
        os.environ['ULIKE_JAVAC'] = str(Path(jdk) / 'bin/javac')
    try:
        baseline_classes = work / 'baseline-classes'
        host_moire1951.compile_java(baseline, baseline_classes, [
            root / 'tests/MoireOracle1951.java',
            root / 'tests/FinishSequenceOracle1952.java',
        ])
        for cls, filename in [('MoireOracle1951', 'combined.bin'),
                              ('FinishSequenceOracle1952', 'sequential.bin')]:
            run([java, '-cp', baseline_classes, 'com.hiro.ulike.' + cls, work / filename],
                work / (cls + '.log'))

        java_info = subprocess.run([java, '-XshowSettings:properties', '-version'],
                                   capture_output=True, text=True, check=True)
        match = re.search(r'^\s*java.home\s*=\s*(.+)$', java_info.stderr, re.M)
        if not match:
            raise RuntimeError('Java runtime did not report its JNI header home')
        include = Path(os.environ.get('ULIKE_JNI_INCLUDE', str(Path(jdk or match.group(1)) / 'include')))
        ndk = ndk or os.environ.get('ULIKE_NDK_HOME')
        graphics = (Path(ndk) / 'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
                    if ndk else Path(os.environ.get('ULIKE_GPU_HEADERS',
                         '/workspace/scratch/d6f5652bff71/audit_h9/prebuilt/headers')))
        if not (graphics / 'GLES3/gl31.h').is_file() or not (include / 'jni.h').is_file():
            raise RuntimeError('JNI and GLES3.1 headers required')
        # Copy graphics headers only; Android libc/sysroot headers must not
        # shadow the host libc when executing the production unit on Mesa.
        host_headers = work / 'headers'
        host_headers.mkdir(exist_ok=True)
        for folder in ['EGL', 'GLES3', 'KHR']:
            shutil.copytree(graphics / folder, host_headers / folder, dirs_exist_ok=True)

        spec = importlib.util.spec_from_file_location(
            'finish_build1953', root / 'finishgpu1953/build_finishgpu1953.py')
        builder = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(builder)
        identity = builder.shader_header(root / 'finishgpu1953', work)
        library = work / 'libulike_finish1953.so'
        run(['cc', '-std=c11', '-O3', '-shared', '-fPIC', '-fno-fast-math',
             '-ffp-contract=off', '-Wall', '-Wextra', '-Werror',
             '-I' + str(include), '-I' + str(include / 'linux'),
             '-I' + str(host_headers), '-I' + str(work),
             root / 'finishgpu1953/finish1953.c', root / 'tests/h22/FinishGpuFault1954.c',
             '-Wl,--wrap=glUnmapBuffer', '-Wl,--no-undefined',
             '-Wl,-l:libEGL.so.1', '-Wl,-l:libGL.so.1', '-lpthread', '-o', library],
            work / 'jni-build.log')
        current_classes = work / 'current-classes'
        host_moire1951.compile_java(root, current_classes, [
            root / 'GpuFinish1953.java', root / 'FinishPolicy1953.java',
            root / 'tests/h16/FinishSession1953Test.java',
            root / 'tests/h22/DispatchReadback1954Test.java',
        ])
    finally:
        if old_javac is None:
            os.environ.pop('ULIKE_JAVAC', None)
        else:
            os.environ['ULIKE_JAVAC'] = old_javac

    env = dict(os.environ, EGL_PLATFORM='surfaceless', LIBGL_ALWAYS_SOFTWARE='1')
    output = run([java, '-Xcheck:jni', '-Djava.library.path=' + str(work),
                  '-cp', current_classes, 'com.hiro.ulike.FinishSession1953Test',
                  work / 'combined.bin', work / 'sequential.bin'],
                 work / 'jni-run.log', env=env, timeout=240)
    checked_log = (work / 'jni-run.log').read_text()
    if 'WARNING' in checked_log or 'FATAL ERROR' in checked_log:
        raise AssertionError('JNI checker rejected production GPU session execution')
    matched = re.search(
        r'SESSION1953_PASS cases=(\d+) dispatched=(\d+) sequential=(\d+) pixels=(\d+) '
        r'raw=(\d+) packed=(\d+) constant=(\d+) h16=(\d+) h18=(\d+) h19=(\d+)', output)
    if not matched:
        raise AssertionError('Production session GLES/JNI endpoint did not execute: ' + output)
    (cases, dispatched, sequences, pixels, raw, packed, constant,
     h16, h18, h19) = map(int, matched.groups())
    if cases < 600 or dispatched < 600 or sequences < 200 or pixels < 4000000:
        raise AssertionError('Insufficient independent pixel-exact GPU session coverage')
    if min(raw, packed, constant, h16, h18, h19) < 1:
        raise AssertionError('Policy representation or distinct session suite not exercised')

    counters = {}
    for label in ['COUNTERS256', 'COUNTERS512']:
        found = re.search(r'^' + label + r' (\{[^\n]+\})$', output, re.M)
        if not found:
            raise AssertionError('Native session counters missing: ' + label)
        counters[label] = json.loads(found.group(1))
        if (counters[label].get('core') != (256 if label == 'COUNTERS256' else 512)
                or counters[label].get('halo') != 36
                or counters[label].get('context_binds') != 1):
            raise AssertionError('Adaptive core, dependency halo or retained context differs: ' + label)
    environment = re.search(r'^ENV (.+)$', output, re.M)
    retained = re.search(r'^NATIVE_RETAINED (\d+)$', output, re.M)
    if not environment or not retained or 'source=' + identity not in environment.group(1):
        raise AssertionError('Executed environment/source identity or memory accounting missing')

    # New H22/H23 counters are distinct from the retained H16/H18/H19 checks.
    dispatch_output = run([
        java, '-Xcheck:jni', '-Djava.library.path=' + str(work), '-cp', current_classes,
        'com.hiro.ulike.DispatchReadback1954Test', work / 'combined.bin', work / 'sequential.bin'],
        work / 'dispatch1954-run.log', env=env, timeout=300)
    dispatch_match = re.search(
        r'DISPATCH1954_PASS cases=(\d+) pixels=(\d+) h22=(\d+) h23=(\d+)', dispatch_output)
    if not dispatch_match:
        raise AssertionError('Actual H22/H23 dispatch/readback suites did not execute')
    dispatch_cases, dispatch_pixels, h22, h23 = map(int, dispatch_match.groups())
    if dispatch_cases != cases * 3 or dispatch_pixels != pixels * 3 or min(h22, h23) < dispatch_pixels:
        raise AssertionError('Each reviewed dispatch variant must compare every independent oracle pixel')
    dispatch_counters = []
    for match in re.finditer(
            r'^DISPATCH_COUNTERS core=(\d+) rows=(\d+) direct=(false|true) (\{[^\n]+\})$',
            dispatch_output, re.M):
        core, rows, direct = int(match.group(1)), int(match.group(2)), match.group(3) == 'true'
        native = json.loads(match.group(4))
        if (native.get('dispatch_rows') != rows or native.get('core') != core
                or native.get('halo') != 36 or native.get('peak_inflight') != 2
                or native.get('submit_completion_waits') != 0
                or (direct and native.get('staging_collection_memcpy_bytes') != 0)):
            raise AssertionError('Production dispatch/readback counter differs')
        dispatch_counters.append({'core_rows': core, 'dispatch_rows': rows, 'direct': direct, 'native': native})
    if len(dispatch_counters) != 12:
        raise AssertionError('Both core sizes and readback modes must execute all three dispatch sizes')
    for core in [256, 512]:
        for direct in [False, True]:
            commands = {row['dispatch_rows']: row['native']['dispatch_count'] for row in dispatch_counters
                        if row['core_rows'] == core and row['direct'] == direct}
            if set(commands) != {32, 64, 0} or not commands[32] > commands[64] > commands[0]:
                raise AssertionError('Larger dispatch sizes did not reduce actual GL command count')
    fault_reports = {}
    for direct in [False, True]:
        name = 'direct' if direct else 'staged'
        mode = 'direct-failure' if direct else 'staged-failure'
        fault_output = run([
            java, '-Xcheck:jni', '-Djava.library.path=' + str(work), '-cp', current_classes,
            'com.hiro.ulike.DispatchReadback1954Test', mode],
            work / (name + '-late-failure.log'), env=env, timeout=120)
        fault_match = re.search(r'^FAILURE1954_PASS direct=(false|true) h23=(\d+) (\{[^\n]+\})$',
                                fault_output, re.M)
        if not fault_match or (fault_match.group(1) == 'true') != direct:
            raise AssertionError('After-copy unmap failure fixture did not execute')
        count = int(fault_match.group(2)); native = json.loads(fault_match.group(3))
        if count < 1 or native.get('forced_drains') != 1 or native.get('close_drained') is not False:
            raise AssertionError('Failed slot was not quarantined after remaining ticket drain')
        h23 += count
        fault_reports[name] = {'assertions': count, 'native': native, 'jni_result': fault_output.strip()}
    for log in ['dispatch1954-run.log', 'direct-late-failure.log', 'staged-late-failure.log']:
        checked = (work / log).read_text()
        if 'WARNING' in checked or 'FATAL ERROR' in checked:
            raise AssertionError('JNI checker rejected H22/H23 execution: ' + log)

    result = {
        'status': 'passed', 'cases': cases, 'dispatched_cases': dispatched,
        'sequential_chain_cases': sequences, 'pixels_compared': pixels,
        'policy_raw_cases': raw, 'policy_packed_cases': packed, 'policy_constant_cases': constant,
        'h16_assertions': h16, 'h18_assertions': h18, 'h19_assertions': h19,
        'h22_assertions': h22, 'h23_assertions': h23,
        'legacy_session_assertions': h16 + h18 + h19,
        'h22_h23_assertions': h22 + h23,
        'assertions': h16 + h18 + h19 + h22 + h23,
        'mesa_gles_shader_executed': True, 'production_jni_executed': True,
        'gpu_chain_pixel_exact': True, 'java_float_and_mask_policy_exact': True,
        'sequential_corrected_neighborhood_pixel_exact': True,
        'source_immutable': True, 'partial_destination_rows_untouched': True,
        'private_gpu_owner_thread_executed': True,
        'one_context_bind_per_photo': True,
        'retained_context_across_photo_sessions': True,
        'actual_environment_fingerprint_checked': True,
        'foreground_environment_query_does_not_initialize_gpu': True,
        'lossless_raw4_packed2_constant4_gpu_variants_executed': True,
        'dependency_36_row_halo_pixel_exact': True,
        'source_uploaded_once_per_photo': True,
        'resident_halo_copies_counted_exactly': True,
        'photo_token_scoped_halo_no_cross_photo_reuse': True,
        'adaptive_256_512_core_rows_executed': True,
        'native_memory_reserved_before_growth': True,
        'two_bounded_slots_executed': True,
        'second_submit_before_first_collect': True,
        'submit_completion_waits_zero': True,
        'submitted_java_input_copied_before_return': True,
        'each_collected_fence_accounted': True,
        'close_drains_before_pool_reuse': True,
        'interrupted_close_preserves_interrupt_and_drains_both_slots': True,
        'closed_ticket_destination_unchanged': True,
        'concurrent_photographs_pixel_exact': True,
        'h22_dispatch_batch_pixel_exact': True,
        'h23_direct_candidate_readback_pixel_exact': True,
        'dispatch_batch_sizes_executed': [32, 64, 0],
        'dispatch_options_pixel_exact_to_pinned_java': True,
        'sequential_36_row_dependency_halo_preserved': True,
        'larger_dispatch_batches_reduce_command_count': True,
        'bounded_two_slot_fences_preserved': True,
        'direct_candidate_no_native_staging_memcpy': True,
        'direct_candidate_pixels_equal_staged_readback': True,
        'old_staged_collect_preserved': True,
        'late_unmap_failure_old_staged_destination_unchanged': True,
        'late_unmap_failure_private_candidate_written_but_rejected': True,
        'late_unmap_failure_original_source_unchanged': True,
        'late_failure_other_pending_slot_drained_and_failed_slot_quarantined': True,
        'cancelled_private_ticket_destination_unchanged': True,
        'host_fault_link_wrapper_only_android_artifact_unmodified': True,
        'dispatch_fixture_cases': dispatch_cases,
        'h22_pixels_compared': dispatch_pixels,
        'h23_pixels_compared': dispatch_pixels,
        'dispatch_counters': dispatch_counters,
        'late_failure_reports': fault_reports,
        'dispatch_jni_result': dispatch_output.strip(),
        'counters256': counters['COUNTERS256'], 'counters512': counters['COUNTERS512'],
        'environment': environment.group(1), 'native_retained_bytes': int(retained.group(1)),
        'pinned_java_oracle_version': '1.9.50',
        'physical_android_tested': False, 'device_speedup_verified': False,
        'full_float_beauty_chain_gpu_ported': False,
        'library_path': str(library), 'library_sha256': hashlib.sha256(library.read_bytes()).hexdigest(),
        'reviewed_source_identity': identity,
        'shader_sha256': hashlib.sha256((root / 'finishgpu1953/finish1953.comp').read_bytes()).hexdigest(),
        'jni_result': output.strip(),
    }
    (work / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', required=True)
    parser.add_argument('--jdk')
    parser.add_argument('--ndk')
    args = parser.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent, args.out, args.jdk, args.ndk), indent=2))
