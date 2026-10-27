#!/usr/bin/env python3
"""Run the five concrete arrival regressions against pinned published 1.8.18 code."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path
from run_candidate_tests import SOURCES

OLD_SHA256 = 'cf7fe470701fdd932f8aec28ea1b8a668096a575ec23ee7821e608eae90614ec'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--old-source', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    assert hashlib.sha256(args.old_source.read_bytes()).hexdigest() == OLD_SHA256, 'Unpinned old source'
    source, classes = args.work / 'generated', args.work / 'classes'
    source.mkdir(parents=True, exist_ok=True); classes.mkdir(parents=True, exist_ok=True)
    for name, content in SOURCES.items():
        path = source / name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text(content + '\n')
    compile_command = ['java', 'com.sun.tools.javac.Main', '--release', '8', '-g:none', '-encoding', 'UTF-8',
                       '-d', str(classes), str(args.old_source),
                       str(args.source_dir / 'tests/CandidateArrivalHostTest.java'),
                       *map(str, sorted(source.rglob('*.java')))]
    compiled = subprocess.run(compile_command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (args.work / 'compile.log').write_text(compiled.stdout)
    assert compiled.returncode == 0, 'Negative control must compile; this is not a missing-API check: ' + compiled.stdout
    cases = {}
    for case in ('nonexpanded', 'smaller', 'survivors', 'samesize', 'collision'):
        result = subprocess.run(['java', '-ea', '-cp', str(classes), 'CandidateArrivalHostTest', case],
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        (args.work / (case + '.log')).write_text(result.stdout)
        assert result.returncode != 0 and 'AssertionError' in result.stdout, 'Old source unexpectedly passed ' + case
        assert 'NoSuchMethodError' not in result.stdout and 'NoSuchMethodException' not in result.stdout
        message = next(line for line in result.stdout.splitlines() if 'AssertionError' in line)
        cases[case] = {'behavioral_failure_detected': True, 'exit_code': result.returncode, 'failure': message}
    report = {'status': 'PASSED', 'old_production_source_sha256': OLD_SHA256,
              'old_production_compiles_with_same_fakes': True, 'negative_cases': cases,
              'new_production_source_sha256': hashlib.sha256(
                  (args.source_dir / 'src/hiro/swiftkey/CandidateScroll.java').read_bytes()).hexdigest(),
              'regression_source_sha256': hashlib.sha256(
                  (args.source_dir / 'tests/CandidateArrivalHostTest.java').read_bytes()).hexdigest(),
              'scope': 'Five behavioral host negative controls against exact published 1.8.18 runtime; no device execution',
              'device_tested': False}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print('PASS old-version negative control: all 5 concrete behavioral regressions detected; compilation succeeded')


if __name__ == '__main__':
    main()
