#!/usr/bin/env python3
"""Execute deterministic GX25 pipeline ownership and actual quiescence tests."""
from pathlib import Path
import hashlib
import json
import os
import re
import subprocess


def test(root, work, jdk=None, ndk=None):
    root, work = Path(root).resolve(), Path(work).resolve() / 'residual-overlap1962'
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    sources = [root / 'ResidualOverlap1962.java', root / 'tests1962/ResidualOverlap1962Test.java']
    commands = [
        [jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
         '-Xlint:-options', '-d', work, *sources],
        [jdk / 'bin/java', '-cp', work, 'com.hiro.ulike.ResidualOverlap1962Test'],
    ]
    output = ''
    for label, command in zip(('compile', 'execute'), commands):
        result = subprocess.run(list(map(str, command)), text=True, capture_output=True, timeout=45)
        output = result.stdout + result.stderr
        (work / (label + '.log')).write_text(output)
        if result.returncode:
            raise RuntimeError('GX25 ' + label + ' failed: ' + output[-8000:])
    found = re.search(r'^RESULT (\{[^\n]+\})$', output, re.M)
    if not found:
        raise AssertionError('Executed GX25 report missing')
    report = json.loads(found.group(1))
    if report.get('status') != 'passed' or report.get('assertions', 0) < 50:
        raise AssertionError('GX25 ownership test coverage missing')
    if report.get('source_quiescence_verified') is not True or report.get('ordered_output_exact') is not True:
        raise AssertionError('GX25 quiescence/transactional output missing')
    report['sources'] = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    (work / 'result.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', default=str(Path(__file__).resolve().parents[1]))
    parser.add_argument('--work', required=True)
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), indent=2))
