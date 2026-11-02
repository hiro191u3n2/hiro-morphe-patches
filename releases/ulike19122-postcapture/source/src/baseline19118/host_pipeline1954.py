#!/usr/bin/env python3
"""Execute independent published .53 and updated .54 saved-pixel CPU pipelines.

The baseline uses a complete byte-pinned historical source view, including its
original Bitmap/camera fixtures and production helper families. Updated code
compiles separately with its current fixtures. GPU-specific H22-H27 execution
is reported by dedicated production JNI and ownership runners.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess

from host_common1954 import inherited1953_root, source_pins, sources1954

ROOT = Path(__file__).resolve().parent


def run(command, log, timeout=180):
    completed = subprocess.run(list(map(str, command)), capture_output=True,
                               text=True, timeout=timeout)
    Path(log).write_text(completed.stdout + completed.stderr)
    if completed.returncode:
        raise RuntimeError(completed.stdout[-3000:] + completed.stderr[-10000:])
    return completed.stdout


def checked(report, label):
    if (report.get('status') != 'passed' or type(report.get('assertions')) is not int
            or report['assertions'] <= 0):
        raise AssertionError('Actual executed pipeline assertions missing: ' + label)
    return report


def test(root, work, android=None):
    root = Path(root).resolve()
    out = Path(work).resolve() / 'host-pipeline1954'
    out.mkdir(parents=True, exist_ok=True)
    pins = source_pins(root)
    baseline = inherited1953_root(root, out)
    javac = os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    java = (os.environ.get('ULIKE_JAVA')
            or (str(Path(javac).with_name('java')) if javac else shutil.which('java')))
    compiler = [javac] if javac else [java, 'com.sun.tools.javac.Main']
    reports = {}
    compiled_sources = {}
    assertions = 0
    for name, source in [('baseline1953', baseline), ('updated1954', root)]:
        classes = out / name
        classes.mkdir(exist_ok=True)
        common = list((source / 'tests/pipeline1942-fixtures').rglob('*.java'))
        common += list((source / 'tests/timing1947-fixtures').rglob('*.java'))
        common += [source / n for n in (
            'SpeedWorkers1935.java', 'Scheduling1944.java', 'NoiseCache1944.java',
            'NativeSpeed1944.java', 'GpuInteger1949.java', 'QualityPixels1932.java',
            'NativeMoire1951.java', 'PolicyCache1945.java', 'QualityShadow1932.java',
            'SpatialNoise1934.java', 'LongMoire1934.java', 'ProcessingTiming1947.java',
            'QualityPipeline1932.java', 'tests/PipelineQuality1942Test.java',
            'tests/QualityGolden1947Test.java',
        )]
        actual_sources = sources1954(source, common)
        fingerprints = {p.relative_to(source).as_posix():
                        hashlib.sha256(p.read_bytes()).hexdigest() for p in actual_sources}
        run(compiler + ['-encoding', 'UTF-8', '-source', '8', '-target', '8',
                        '-Xlint:-options', '-d', classes, *actual_sources],
            out / (name + '-compile.log'), 120)
        golden = checked(json.loads(run([
            java, '-XX:ActiveProcessorCount=4',
            '-Djava.library.path=' + str(out / 'unavailable'), '-cp', classes,
            'com.hiro.ulike.QualityGolden1947Test', 'instrumented',
        ], out / (name + '-golden.log'))), name + ' golden')
        ownership = checked(json.loads(run([
            java, '-XX:ActiveProcessorCount=4',
            '-Djava.library.path=' + str(out / 'unavailable'), '-cp', classes,
            'com.hiro.ulike.PipelineQuality1942Test',
        ], out / (name + '-ownership.log'))), name + ' ownership')
        reports[name] = {'golden': golden, 'ownership': ownership}
        if fingerprints != {p.relative_to(source).as_posix():
                hashlib.sha256(p.read_bytes()).hexdigest() for p in actual_sources}:
            raise AssertionError('Compiled sources changed during exact pipeline execution: ' + name)
        compiled_sources[name] = fingerprints
        assertions += golden['assertions'] + ownership['assertions']
    if (reports['baseline1953']['golden']['output_sha256']
            != reports['updated1954']['golden']['output_sha256']):
        raise AssertionError('Independent published .53 and updated .54 saved pixels differ')
    workers = out / 'workers'
    workers.mkdir(exist_ok=True)
    run(compiler + ['-encoding', 'UTF-8', '-source', '8', '-target', '8',
                    '-Xlint:-options', '-d', workers, root / 'SpeedWorkers1935.java',
                    root / 'tests/SpeedWorkersHints1952Test.java'],
        out / 'workers-compile.log', 120)
    worker = checked(json.loads(run([
        java, '-XX:ActiveProcessorCount=4', '-cp', workers,
        'com.hiro.ulike.SpeedWorkersHints1952Test',
    ], out / 'workers.log')), 'worker integration')
    assertions += worker['assertions']
    result = {
        'status': 'passed', 'assertions': assertions,
        'baseline_version': '1.9.53',
        'baseline_source_sha256': pins['historical_files']['QualityPipeline1932.java'],
        'baseline_changed_family_source_sha256': pins['snapshots'],
        'compiled_sources_sha256': compiled_sources,
        'same_output_pixels': True, 'pixel_equivalence_to_baseline': True,
        'independent_published1953_source_tree_executed': True,
        'independent_published1953_bitmap_fixtures_executed': True,
        'gpu_unavailable_cpu_fallback_exact': True,
        'captured_options_and_rotation_geometry_exact': True,
        'source_and_prepared_ownership_preserved': True,
        'copy_failure_fallback_preserved': True,
        'normalization_and_detail_call_contracts_preserved': True,
        'baseline': reports['baseline1953'], 'updated': reports['updated1954'],
        'worker_hint_integration': worker,
        'physical_android_tested': False, 'device_speedup_verified': False,
        'assertion_count_basis': 'Actual independent .53 and current .54 '
            'golden/ownership tests plus current worker integration; '
            'new GPU and ownership suites are counted separately.',
        'fixture_scope': 'Independent historical .53 and current .54 production '
            'pipelines with explicit Android Bitmap/camera/primary-NR fixtures; '
            'native GPU unavailable in this CPU-preservation suite.',
    }
    (out / 'result.json').write_text(json.dumps(result, ensure_ascii=False,
                                               sort_keys=True, indent=2) + '\n')
    return result


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', required=True)
    arguments = parser.parse_args()
    print(json.dumps(test(ROOT, arguments.out), ensure_ascii=False, indent=2))
