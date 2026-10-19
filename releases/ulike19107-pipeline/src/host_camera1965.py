#!/usr/bin/env python3
"""Fresh camera lifecycle and diagnostic tests; retained image code is checked by DEX pins.

Every result here comes from executing production Java helper bodies against
explicit Android/native-owner delivery doubles. No screen visibility, physical
Android execution, or GPU image speed is inferred from these host tests.
"""
from pathlib import Path
import argparse
import importlib.util
import json
import os
import shutil
import tempfile


RETAINED = (
    ('retained_front1936', 'host_front1936.py'),
    ('retained_renderer1938', 'host_renderer1938.py'),
    ('retained_layout1937', 'host_layout1937.py'),
    ('retained_layout_lifecycle1937', 'host_layout_lifecycle1937.py'),
    ('retained_geometry1937', 'host_geometry1937.py'),
)
NEW = (
    ('ui_camera1965', 'tests1965/ui_camera1965.py'),
    ('camera_session1965', 'tests1965/session1965.py'),
    ('camera_hooks1965', 'tests1965/hooks1965.py'),
    ('trace_storage1965', 'tests1965/trace_audit1965.py'),
    ('preview_output1965', 'tests1965/preview_output1965_test.py'),
)


def _module(root, name, relative):
    path = root / relative
    if not path.is_file():
        raise AssertionError('Required camera regression runner missing: ' + relative)
    spec = importlib.util.spec_from_file_location('host_camera1965_' + name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _checked(report, name):
    if (not isinstance(report, dict) or report.get('status') != 'passed'
            or type(report.get('assertions')) is not int
            or report['assertions'] <= 0):
        raise AssertionError('Executed camera regressions missing: ' + name)
    if report.get('physical_android_tested', False) is not False:
        raise AssertionError('Host runner cannot establish physical Android testing: ' + name)
    if report.get('device_tested', False) is not False:
        raise AssertionError('Host runner cannot establish device testing: ' + name)
    return report


def _require_fields(report, fields, name):
    for field in fields:
        if report.get(field) is not True:
            raise AssertionError('Executed ' + name + ' proof missing: ' + field)
    return True


def _jdk_path(jdk):
    if jdk is None:
        jdk = os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME')
    if jdk is None:
        javac = shutil.which('javac')
        if javac:
            jdk = Path(javac).resolve().parent.parent
    if jdk is None:
        raise AssertionError('Executing JDK required for fresh camera regressions')
    path = Path(jdk).resolve()
    if not all((path / 'bin' / name).is_file() for name in ('java', 'javac')):
        raise AssertionError('JDK java/javac missing: ' + str(path))
    return path


def test(root, work, jdk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    jdk = _jdk_path(jdk)
    folder = work / 'host-camera1965'
    folder.mkdir(parents=True, exist_ok=True)
    # Retained suites use mkdir() for their owned staging trees. Every invocation
    # gets a fresh directory rather than treating an old result as current proof.
    run = Path(tempfile.mkdtemp(prefix='run-', dir=folder))
    reports = {}
    old_path = os.environ.get('PATH')
    try:
        os.environ['PATH'] = str(jdk / 'bin') + os.pathsep + (old_path or '')
        for name, relative in RETAINED:
            child = run / name
            child.mkdir()
            module = _module(root, name, relative)
            reports[name] = _checked(module.test(root, child, androidjar=None), name)
        for name, relative in NEW:
            child = run / name
            child.mkdir()
            module = _module(root, name, relative)
            # All .65 suites accept root/work/jdk. Only the image/GPU runners
            # require NDK; this camera release intentionally invokes none.
            reports[name] = _checked(module.test(root, child, jdk=jdk), name)
    finally:
        if old_path is None:
            os.environ.pop('PATH', None)
        else:
            os.environ['PATH'] = old_path

    lifecycle = all(reports[name]['status'] == 'passed' for name, _ in RETAINED)
    lifecycle = lifecycle and all(reports[name]['status'] == 'passed'
        for name in ('ui_camera1965', 'camera_session1965', 'camera_hooks1965'))
    lifecycle = lifecycle and _require_fields(reports['camera_session1965'], (
        'five_second_sampling_tests_passed', 'bounded_weak_ownership_tests_passed',
        'scalar_privacy_tests_passed'), 'camera session lifetime')
    lifecycle = lifecycle and _require_fields(reports['camera_hooks1965'], (
        'original_control_flow_restored_by_inverse', 'modified_baseline_rejected',
        'serialized_inverse_verified'), 'serialized camera observer control flow')
    storage = _require_fields(reports['trace_storage1965'], (
        'retained_across_jvm_processes', 'same_epoch_anomalies_coalesced',
        'prior_tail_recovery_verified', 'bounded_queue_drop_report_verified',
        'protected_tail_recovery_and_live_export_verified',
        'post_trigger_cap_and_expiry_verified', 'cleanup_retry_single_writer_guard_verified'), 'persistent diagnostic storage')
    exports = _require_fields(reports['trace_storage1965'], (
        'exported_user_snapshot_immutable', 'snapshot_pending_cleanup_fault_verified',
        'pending_export_cleanup_faults_verified', 'share_read_only_grant_verified'),
        'diagnostic export')
    ownership = _require_fields(reports['preview_output1965'], (
        'output_probe_session_ownership_tests_passed',
        'worker_main_thread_separation_tests_passed',
        'bounded_native_bitmap_lifetime_tests_passed', 'initial_observation_owner_pin_tests_passed'), 'output probe session ownership')
    evidence = _require_fields(reports['preview_output1965'], (
        'sampled_input_is_not_visible_success',
        'trace_field_limit_preserves_scalar_evidence'), 'sampled output evidence')
    evidence = evidence and _require_fields(reports['camera_session1965'], (
        'sampled_input_is_not_visible_success',), 'sampled camera input evidence')
    result = {
        'status': 'passed',
        'assertions': sum(report['assertions'] for report in reports.values()),
        'camera_session_lifecycle_regressions_passed': lifecycle,
        'persistent_trace_storage_tests_passed': storage,
        'trace_export_tests_passed': exports,
        'output_probe_session_ownership_tests_passed': ownership,
        'sampled_input_is_not_visible_success': evidence,
        'physical_android_tested': False,
        'fresh_gpu_execution_in_this_release': False,
        'scope': 'Actual production camera helpers; Android/native-owner delivery doubles. Image/native retention is proved independently by fresh DEX and archive byte comparisons.',
        'executed_test_count': len(reports),
        'tests': reports,
        'logs': str(run),
    }
    if not lifecycle:
        raise AssertionError('Camera lifecycle graph did not complete')
    (folder / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), ensure_ascii=False, indent=2))
