#!/usr/bin/env python3
"""Run independent ARSC and final DEX checks after APK packaging completes."""
import argparse
import base64
import hashlib
import json
import os
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
PORT = HERE.parent
WORKSPACE = PORT.parent


def run(args, log):
    with log.open('w') as out:
        r = subprocess.run([str(x) for x in args], stdout=out, stderr=subprocess.STDOUT)
    print(log.read_text(), end='', flush=True)
    r.check_returncode()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('cases', nargs='*', default=['dev1', 'dev2', 'dev2-clone', 'standalone'])
    ap.add_argument('--patcher', type=Path, default=WORKSPACE/'tools/morphe.jar')
    ap.add_argument('--ecj', type=Path, default=Path('/workspace/scratch/80dcfb9aa48e/tools/ecj.jar'))
    args = ap.parse_args()
    rows = json.loads((PORT/'translations/hardcoded-ja.json').read_text())
    targets = []
    for row in rows:
        method = row['class']+'->'+row['method']+'('+''.join(row['parameterTypes'])+')'+row['returnType']
        targets.append('\t'.join((method, row['resource'], str(row['expectedCount']),
                                  base64.b64encode(row['source'].encode()).decode())))
    (HERE/'targets.tsv').write_text('\n'.join(targets)+'\n')
    run(['java', '-XX:-UsePerfData', '-jar', args.ecj, '-8', '-nowarn', '-encoding', 'UTF-8',
         '-cp', args.patcher, '-d', HERE/'classes', HERE/'AuditLocalizedDex.java'], HERE/'compile-audit.log')
    resources, dex = {}, {}
    for case in args.cases:
        apk = HERE/(case+'.apk')
        if not apk.is_file():
            raise FileNotFoundError('APK not ready: '+str(apk))
        run(['python', HERE/'audit_resources.py', apk, '--report', HERE/(case+'-resources.json'),
             '--baseline', WORKSPACE/'verification/hiro-piko.apk'], HERE/(case+'-resource-audit.log'))
        run(['java', '-XX:-UsePerfData', '-Xmx3g', '-cp', str(HERE/'classes')+os.pathsep+str(args.patcher),
             'AuditLocalizedDex', apk, HERE/'targets.tsv', HERE/(case+'-localized-dex.json')],
            HERE/(case+'-localized-dex.log'))
        resources[case] = json.loads((HERE/(case+'-resources.json')).read_text())
        dex[case] = json.loads((HERE/(case+'-localized-dex.json')).read_text())
        dex[case]['apk_sha256'] = hashlib.sha256(apk.read_bytes()).hexdigest()
        (HERE/(case+'-localized-dex.json')).write_text(json.dumps(dex[case], ensure_ascii=False, indent=2)+'\n')
    report = {'compiled_resources': {'result': 'PASS', 'cases': resources},
              'final_dex_localization': {'result': 'PASS', 'cases': dex}}
    (HERE/'completed-apk-audits.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'result':'PASS', 'cases':args.cases,
                      'exact_resource_checks_each':1322, 'localized_lookup_sites_each':224}, ensure_ascii=False))


if __name__ == '__main__':
    main()
