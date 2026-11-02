#!/usr/bin/env python3
"""H24/H26 current policy code against byte-pinned published .53 Java sources.

The oracle classes are renamed only, allowing direct comparison of arbitrary
custom-mask state and exact float policy values in the same Java runtime.
The separate counted fixture validates dispatch/storage counts; it is never
used as image-quality evidence.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import host_common1954
import host_moire1951


def run_java(classes, name, out):
    java = os.environ.get('ULIKE_JAVA') or shutil.which('java')
    if not java:
        raise RuntimeError('Java runtime unavailable for exact H24/H26 validation')
    result = subprocess.run([java, '-cp', str(classes), name], capture_output=True,
                            text=True, timeout=180)
    out.write_text(result.stdout + result.stderr)
    if result.returncode:
        raise RuntimeError(result.stdout[-2000:] + result.stderr[-10000:])
    parsed = json.loads(result.stdout)
    if parsed.get('status') != 'passed':
        raise AssertionError('Policy suite did not pass')
    return parsed


def test(root, work, android=None):
    root = Path(root).resolve()
    out = Path(work).resolve() / 'host-speedpolicy1954'
    out.mkdir(parents=True, exist_ok=True)
    pins = host_common1954.source_pins(root)
    reference = root / 'tests/published1953-reference'
    generated = out / 'oracle-source'
    generated.mkdir(exist_ok=True)
    oracles = []
    for original, renamed in [('SpatialNoise1934', 'SpatialNoiseReference1953'),
                              ('FinishPolicy1953', 'FinishPolicyReference1953')]:
        name = original + '.java'
        data = (reference / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != pins['historical_files'][name]:
            raise AssertionError('Changed published .53 policy oracle: ' + name)
        target = generated / (renamed + '.java')
        target.write_text(data.decode().replace(original, renamed))
        oracles.append(target)
    unchanged = ('NativeMoire1951.java', 'quality-dependencies/com/hiro/ulike/FaceRegions1934.java',
                 'quality-dependencies/com/hiro/ulike/FaceRegions1934Pixels.java')
    for name in unchanged:
        if hashlib.sha256((root / name).read_bytes()).hexdigest() != pins['historical_files'][name]:
            raise AssertionError('Policy math/production immutable mask source changed: ' + name)
    classes = out / 'classes'
    graphics = sorted((root / 'tests/h24h26/graphics').rglob('*.java'))
    host_moire1951.compile_java(root, classes, oracles + graphics + [
        root / 'FinishPolicy1953.java', root / unchanged[1], root / unchanged[2],
        root / 'tests/h24h26/PolicySpeed1954Test.java'])
    exact = run_java(classes, 'com.hiro.ulike.PolicySpeed1954Test', out / 'exact.log')
    counted = out / 'counted-classes'
    counted.mkdir(exist_ok=True)
    compiler = os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    command = [compiler] if compiler else ['java', 'com.sun.tools.javac.Main']
    fixtures = sorted((root / 'tests/h24h26/counts').rglob('*.java'))
    compiled = subprocess.run(command + ['-source', '8', '-target', '8', '-Xlint:-options',
        '-d', str(counted), *map(str, fixtures + [root / 'SpeedWorkers1935.java',
                                               root / 'FinishPolicy1953.java'])],
        capture_output=True, text=True, timeout=120)
    (out / 'counted-compile.log').write_text(compiled.stdout + compiled.stderr)
    if compiled.returncode:
        raise RuntimeError(compiled.stderr[-10000:])
    control = run_java(counted, 'com.hiro.ulike.PolicyCalls1954Test', out / 'counted.log')
    if exact['coordinate_values'] < 1000000 or exact['policy_cases'] < 1000:
        raise AssertionError('Insufficient independent coordinate/coefficient cases')
    if exact['unknown_mask_cases'] < 1000 or control['promotion_cases'] < 20:
        raise AssertionError('Insufficient unknown-mask/promotion cases')
    result = {
        'status': 'passed',
        'h24_assertions': exact['h24_assertions'] + control['h24_assertions'],
        'h26_assertions': exact['h26_assertions'] + control['h26_assertions'],
        'assertions': exact['h24_assertions'] + exact['h26_assertions']
                    + control['h24_assertions'] + control['h26_assertions'],
        'exact_float_coordinate_coefficients': True,
        'constant_policy_shortcut': True,
        'immutable_mask_single_pass_prefix_expansion_exact': True,
        'unknown_custom_mask_original_call_order_preserved': True,
        'policy_integer_equivalence_to_published1953': True,
        'pixel_equivalence_to_published1953': True,
        'published1953_policy_and_noise_sources_pinned': True,
        'policy_math_and_production_mask_sources_unchanged': True,
        'native_library_changes': False,
        'physical_android_speed_measured': False,
        'physical_android_tested': False,
        'exact': exact,
        'counted_control': control,
        'production_source_sha256': {name: hashlib.sha256((root / name).read_bytes()).hexdigest()
                                    for name in ('FinishPolicy1953.java', 'SpatialNoise1934.java')},
        'scope': 'Exact .53 policy integers and float map results; actual image kernels are covered '
                 'by the aggregate GPU/CPU oracle suites. Unknown custom masks retain .53 '
                 'second-pass semantics. Android detector behavior is outside this policy suite.',
    }
    (out / 'result.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent, args.out), indent=2))
