#!/usr/bin/env python3
"""Audit completed rebuilt APKs; keep private app inputs outside published sources."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--qa', type=Path, required=True)
    parser.add_argument('--tools', type=Path, required=True)
    parser.add_argument('cases', nargs='+')
    args = parser.parse_args()
    qa = args.qa.resolve()
    tools = args.tools.resolve()
    rows = json.loads((ROOT/'translations/hardcoded-ja.json').read_text())
    targets = []
    for row in rows:
        method = row['class']+'->'+row['method']+'('+''.join(row['parameterTypes'])+')'+row['returnType']
        targets.append('\t'.join((method, row['resource'], str(row['expectedCount']),
                                  base64.b64encode(row['source'].encode()).decode())))
    (qa/'targets.tsv').write_text('\n'.join(targets)+'\n')
    compiled = qa/'audit-classes'
    compiled.mkdir(exist_ok=True)
    with (qa/'audit-compile.log').open('w') as log:
        subprocess.run(['java', '-XX:-UsePerfData', '-jar', str(tools/'ecj.jar'), '-8', '-nowarn',
                        '-encoding', 'UTF-8', '-cp', str(tools/'morphe25.jar'), '-d', str(compiled),
                        str(ROOT/'verification/AuditLocalizedDex.java')], stdout=log,
                       stderr=subprocess.STDOUT, check=True)
    for case in args.cases:
        summary = json.loads((qa/(case+'-summary.json')).read_text())
        if summary.get('result') != 'PASS':
            raise ValueError('Application has not passed: '+case)
        apk = qa/(case+'.apk')
        sha256 = hashlib.sha256(apk.read_bytes()).hexdigest()
        if summary.get('apk_sha256') != sha256:
            raise ValueError('Completed APK identity differs: '+case)
        operations = [
            ('resources', ['python3', str(ROOT/'verification/audit_resources.py'), str(apk),
                           '--report', str(qa/(case+'-resources.json')),
                           '--baseline', str(qa/('baseline-dev2.apk' if case == 'dev2' else 'piko-dev3.apk'))]),
            ('dex', ['java', '-XX:-UsePerfData', '-Xmx1g', '-cp',
                     str(compiled)+os.pathsep+str(tools/'morphe25.jar'), 'AuditLocalizedDex',
                     str(apk), str(qa/'targets.tsv'), str(qa/(case+'-localized-dex.json'))]),
        ]
        for name, command in operations:
            with (qa/(case+'-'+name+'-audit.log')).open('w') as log:
                subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, check=True)
            print((qa/(case+'-'+name+'-audit.log')).read_text(), end='', flush=True)
        report = json.loads((qa/(case+'-localized-dex.json')).read_text())
        report['apk_sha256'] = sha256
        (qa/(case+'-localized-dex.json')).write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')


if __name__ == '__main__':
    main()
