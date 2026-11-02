#!/usr/bin/env python3
"""Compile the shipped ULike source graph and exercise the speed-proof guard."""
from pathlib import Path
import hashlib
import json
import os
import subprocess


def test(source, work, jdk=None, ndk=None):
    source = Path(source).resolve()
    work = Path(work).resolve()
    if work.exists():
        raise AssertionError('Fresh resident comparison work directory required')
    work.mkdir(parents=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    android = Path(os.environ['ULIKE_ANDROID_JAR']).resolve()

    import sys
    sys.path.insert(0, str(source))
    import build19101
    if hashlib.sha256(android.read_bytes()).hexdigest() != build19101.TOOL_PINS['android.jar']:
        raise AssertionError('Pinned Android compile API required')

    roots = json.loads((source.parent / 'production-roots19115.json').read_text())
    if not {'GpuQualification1961', 'GpuResident1976'}.issubset(roots):
        raise AssertionError('Comparator and admission must enter the shipped runtime')
    fixture = Path(__file__).with_name('ResidentFairPlan19116Test.java')
    inputs = list(dict.fromkeys([
        *build19101.compile_inputs(roots).values(),
        *(build19101.production_path(root) for root in roots),
        fixture,
    ]))
    pins = {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in inputs}
    classes = work / 'classes'
    classes.mkdir()

    def run(label, arguments):
        result = subprocess.run(list(map(str, arguments)), capture_output=True, text=True, timeout=180)
        (work / (label + '.log')).write_text(result.stdout + result.stderr)
        if result.returncode:
            raise AssertionError(label + '\n' + (result.stdout + result.stderr)[-16000:])
        return result.stdout

    run('compile', [jdk / 'bin/javac', '-source', '8', '-target', '8',
                    '-encoding', 'UTF-8', '-bootclasspath', android,
                    '-d', classes, *inputs])
    output = run('compare', [jdk / 'bin/java', '-ea', '-Xmx256m',
                             '-cp', str(classes) + os.pathsep + str(android),
                             'com.hiro.ulike.ResidentFairPlan19116Test'])
    result = json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
    if result.get('status') != 'passed' or result.get('assertions') != 11 or not result.get('incomparable_plan_speed_rejection_blocked'):
        raise AssertionError('Incomplete current comparator proof')
    if pins != {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in inputs}:
        raise AssertionError('Compiled production source changed during comparison test')
    result.update(schema='ulike19116-resident-fair-plan-v1', current_source_sha256=pins,
                  actual_production_graph_compiled=True, physical_android_tested=False,
                  device_speedup_verified=False)
    (work / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True)
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))
