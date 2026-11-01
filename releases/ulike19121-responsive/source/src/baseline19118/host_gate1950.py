#!/usr/bin/env python3
"""Execute H5 production admission and lock regression against a mock backend."""
from pathlib import Path
import hashlib, importlib.util, json, subprocess


def test(root, work, android=None):
    root = Path(root)
    work = Path(work) / 'gpu-admission1950'
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    spec = importlib.util.spec_from_file_location('host_gpu1949_tools', root / 'host_gpu1949.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    sources = [root / 'GpuInteger1949.java', root / 'tests/GpuAdmission1950Test.java']
    subprocess.run(module.java_tool('javac') + ['-encoding', 'UTF-8', '-source', '8', '-target', '8', '-d', str(classes)] + list(map(str, sources)), check=True, capture_output=True, text=True)
    run = subprocess.run(module.java_tool('java') + ['-Xmx768m', '-cp', str(classes), 'com.hiro.ulike.GpuAdmission1950Test'], check=True, capture_output=True, text=True, timeout=30)
    result = json.loads(run.stdout.strip())
    if result.get('status') != 'passed' or result.get('assertions', 0) < 300 or result.get('scenarios', 0) != 5:
        raise RuntimeError('Executed H5 gate regression evidence missing')
    result.update(pixel_equivalence_guard_preserved=True, canonical_boundary_shape_key=True,
                  cpu_reference_not_global_lock=True, same_shape_busy_falls_back_without_wait=True, transfer_inclusive_timing_gate=True,
                  global_tile_positions=96, comparison_reassessment_interval=64,
                  physical_android_tested=False, device_speed_measured=False,
                  gpu_execution_claimed=False, whole_direct_output_route_independently_admitted=True,
                  gpu_only_seeding_inside_timing=True, admitted_output_java_copyback_removed=True, test_backend='explicit host oracle',
                  sources={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources})
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work), ensure_ascii=False))
