#!/usr/bin/env python3
"""Test real Home JVM closures against hash-pinned decoded Trip8.54.2 layouts."""
import argparse
import hashlib
import json
from pathlib import Path
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parent


def run(command, output):
    with output.open('wb') as stream:
        subprocess.run(list(map(str, command)), stdout=stream, stderr=subprocess.STDOUT, check=True)
    print(output.read_text(errors='replace'))


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--baseline', type=Path, required=True)
    p.add_argument('--tools', type=Path, required=True)
    p.add_argument('--work', type=Path, required=True)
    args = p.parse_args()
    args.work.mkdir(parents=True, exist_ok=False)
    manifest = json.loads((ROOT / 'fixtures/metadata.json').read_text())
    for name, expected in manifest['files'].items():
        actual = hashlib.sha256((ROOT / 'fixtures' / name).read_bytes()).hexdigest()
        if actual != expected:
            raise ValueError('Original Trip layout fixture differs: ' + name)
    classes = args.work / 'patchedclasses'
    testclasses = args.work / 'testclasses'
    classes.mkdir()
    testclasses.mkdir()
    run([sys.executable, ROOT / 'prepare_home_jvm.py', '--baseline', args.baseline,
         '--classes', classes, '--report', args.work / 'jvm.json'], args.work / 'jvm.log')
    helper = ROOT.parent / 'native/patch/app/hiro/tripcom/patches/HomeResourceDocuments.java'
    sources = sorted((ROOT / 'test-stubs').rglob('*.java')) + sorted((ROOT / 'test').rglob('*.java')) + [helper]
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar',
         '-d', testclasses, *sources], args.work / 'compile.log')
    cp = os.pathsep.join(map(str, (testclasses, args.tools / 'morphe.jar')))
    run(['java', '-cp', cp, 'HomeResourceDocumentsTest', args.baseline, classes,
         ROOT / 'fixtures', args.work / 'cases'], args.work / 'render.log')
    report = json.loads((args.work / 'render.log').read_text())
    report['fixture_provenance'] = manifest
    (args.work / 'home-resource.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
