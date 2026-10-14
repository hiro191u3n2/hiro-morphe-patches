#!/usr/bin/env python3
"""Exercise actual Java whole-route selection with owned mock photographs."""
from pathlib import Path
import hashlib
import json
import subprocess
from host_gpu1949 import java_tool


def test(root, work):
    root = Path(root)
    work = Path(work) / 'route1952'
    work.mkdir(parents=True, exist_ok=True)
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    sources = [root / 'WholeRoute1952.java', root / 'tests/WholeRoute1952Test.java']
    subprocess.run(java_tool('javac') + ['-encoding', 'UTF-8', '-source', '8',
                   '-target', '8', '-d', str(classes)] + list(map(str, sources)),
                   check=True, capture_output=True, text=True, timeout=60)
    run = subprocess.run(java_tool('java') + ['-Xmx256m', '-cp', str(classes),
                         'com.hiro.ulike.WholeRoute1952Test'],
                         check=True, capture_output=True, text=True, timeout=60)
    result = json.loads(run.stdout.strip())
    if (result.get('status') != 'passed' or result.get('assertions', 0) < 400
            or result.get('scenarios', 0) < 20):
        raise RuntimeError('Executed whole-route lifecycle assertions missing')
    result.update(
        test_adapter=True,
        physical_android_tested=False,
        gpu_execution_claimed=False,
        device_speed_measured=False,
        whole_final_stage_wall_timing=True,
        candidate_preparation_and_context_wait_included=True,
        initial_reference_output=True,
        cold_equality_probes=1,
        timed_equality_speed_probes=2,
        minimum_timing_margin_percent=5,
        admitted_slowdown_threshold_percent=10,
        slowdown_uses_last_reliable_cpu_wall_baseline=True,
        slowdown_affects_following_photograph=True,
        per_photo_cpu_duplication_after_admission=False,
        periodic_whole_photo_duplication=False,
        captured_shape_settings_keys_only=True,
        bounded_cache_shapes=24,
        in_flight_shape_eviction=False,
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
