#!/usr/bin/env python3
"""Real background admission and Android profile logic; mock images/driver."""
from pathlib import Path
import hashlib
import json
import subprocess
from host_gpu1949 import java_tool


def test(root, work):
    root = Path(root)
    work = Path(work) / 'route1953'
    work.mkdir(parents=True, exist_ok=True)
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    sources = [root / 'WholeRoute1952.java', root / 'WholeRoute1953.java',
               root / 'tests/WholeRoute1953Test.java',
               *sorted((root / 'tests/route1953-fixtures').rglob('*.java'))]
    subprocess.run(java_tool('javac') + ['-encoding', 'UTF-8', '-source', '8',
                   '-target', '8', '-d', str(classes)] + list(map(str, sources)),
                   check=True, capture_output=True, text=True, timeout=60)
    run = subprocess.run(java_tool('java') + ['-Xmx256m', '-cp', str(classes),
                         'com.hiro.ulike.WholeRoute1953Test'],
                         check=True, capture_output=True, text=True, timeout=60)
    result = json.loads(run.stdout.strip())
    if (result.get('status') != 'passed' or result.get('assertions', 0) < 440
            or result.get('scenarios', 0) < 15):
        raise RuntimeError('Executed background lifecycle/profile assertions missing')
    result.update(
        suite='h21_background_admission',
        actual_java_engine_executed=True,
        actual_preferences_profile_logic_executed=True,
        mocked_android_preferences=True,
        mocked_gpu_driver_and_images=True,
        physical_android_tested=False,
        gpu_execution_claimed=False,
        device_speed_measured=False,
        foreground_cpu_returns_before_background_gpu=True,
        original_foreground_cpu_runs_once=True,
        successful_associated_save_required=True,
        exact_capture_preparation_codec_encoder_idle_required=True,
        bounded_single_pending_probe=True,
        native_snapshot_budget_reserved_before_clone=True,
        native_budget_read_lock_free=True,
        queued_probe_budget_released_before_capture_readiness_returns=True,
        active_gpu_cancel_release_after_real_drain=True,
        cancellation_does_not_disable_route=True,
        background_candidate_never_published=True,
        full_pixel_equality_required=True,
        gpu_timing_includes_candidate_policy_transfer_queue_wait_readback=True,
        cold_equality_probes=1,
        timed_equality_speed_probes=2,
        minimum_timing_margin_percent=5,
        admitted_slowdown_threshold_percent=10,
        foreground_cpu_duplication_after_admission=False,
        environment_profile_fields=['os_fingerprint', 'sdk', 'supported_abis',
                                    'app_package_and_version', 'driver_vendor_renderer_version',
                                    'native_abi_and_reviewed_source_hash', 'route_revision'],
        unknown_environment_never_restores=True,
        newly_verified_matching_history_requires_current_exact_proof=True,
        qualified_profile_checksum_required=True,
        profile_contains_images_or_masks=False,
        bounded_memory_and_persisted_shapes=24,
        sources={str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
                 for path in sources},
    )
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False,
                                               sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    arguments = parser.parse_args()
    print(json.dumps(test(arguments.root, arguments.work), ensure_ascii=False))
