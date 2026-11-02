#!/usr/bin/env python3
"""H31: actual engine and preferences persistence; mocked Android and GPU."""
from pathlib import Path
import hashlib
import json
import subprocess
from host_gpu1949 import java_tool


def test(root, work):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'h31_dispatch1956'
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    sources = [root / 'WholeRoute1952.java', root / 'WholeRoute1953.java',
               root / 'tests/WholeRoute1953Test.java', root / 'tests/H31Dispatch1956Test.java',
               *sorted((root / 'tests/route1953-fixtures').rglob('*.java'))]
    subprocess.run(java_tool('javac') + ['-encoding', 'UTF-8', '-source', '8', '-target', '8',
                   '-d', str(classes)] + list(map(str, sources)),
                   check=True, capture_output=True, text=True, timeout=60)
    result = subprocess.run(java_tool('java') + ['-Xmx256m', '-cp', str(classes),
                            'com.hiro.ulike.H31Dispatch1956Test'],
                            check=True, capture_output=True, text=True, timeout=60)
    report = json.loads(result.stdout.strip())
    if report.get('status') != 'passed' or report.get('scenarios', 0) < 16 or report.get('assertions', 0) < 300:
        raise AssertionError('H31 persistence execution incomplete')
    report.update(suite='h31_persist_certified_dispatch',
                  actual_java_engine_and_preferences_logic_executed=True,
                  mocked_android_shared_preferences=True, mocked_gpu_and_photos=True,
                  real_gpu_executed=False, physical_android_tested=False,
                  device_speed_measured=False, qualified_modes=[32, 64, 0],
                  proof_schema='1956:1', full_fingerprint_and_route_key_required=True,
                  legacy_cpu_only_records_rejected=True, corrupt_records_rejected=True,
                  cold_start_restores_only_freshly_proved_stored_mode=True,
                  fresh_mismatch_invalidates_even_when_timing_unreliable_or_zero=True,
                  cancellation_never_persists_incomplete_proof=True,
                  runtime_rejection_deletes_record=True,
                  sources={str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources})
    (work / 'result.json').write_text(json.dumps(report, indent=2, sort_keys=True) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work)))
