#!/usr/bin/env python3
"""Prove dev.1 Japanese-only APK preserves unrelated class contents.

Canonicalizes classes independently of APK/DEX ordering, then normalizes only
const-string/jumbo opcode and corresponding builder-adjusted branch/debug offsets.
All remaining class metadata, code, fields and annotations must match exactly.
"""
import argparse
import hashlib
import json
import os
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORKSPACE = HERE.parent.parent


def run(args, log):
    with log.open('w') as output:
        p = subprocess.run([str(x) for x in args], stdout=output, stderr=subprocess.STDOUT)
    print(log.read_text(), end='', flush=True)
    p.check_returncode()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('case', default='dev1', nargs='?')
    ap.add_argument('--baseline', type=Path, default=WORKSPACE/'verification/hiro-piko.apk')
    ap.add_argument('--patcher', type=Path, default=WORKSPACE/'tools/morphe.jar')
    ap.add_argument('--ecj', type=Path, default=Path('/workspace/scratch/80dcfb9aa48e/tools/ecj.jar'))
    args = ap.parse_args()
    apk = HERE/(args.case+'.apk')
    cp = str(HERE/'classes')+os.pathsep+str(args.patcher)
    run(['java','-XX:-UsePerfData','-jar',args.ecj,'-17','-nowarn','-encoding','UTF-8','-cp',args.patcher,
         '-d',HERE/'classes',HERE/'CompareApk.java',HERE/'NormalizeClassDiff.java',HERE/'ReviewNewXApk.java'],
        HERE/(args.case+'-compile-preservation.log'))
    raw_report = HERE/(args.case+'-raw-class-diff.json')
    run(['java','-XX:-UsePerfData','-Xmx4g','-cp',cp,'CompareApk',args.baseline,apk,raw_report],
        HERE/(args.case+'-raw-class-diff.log'))
    raw = json.loads(raw_report.read_text())
    changed_file = HERE/(args.case+'-changed-classes.txt')
    changed_file.write_text('\n'.join(raw['changed_classes'])+'\n')
    normalized_report = HERE/(args.case+'-normalized-class-diff.json')
    run(['java','-XX:-UsePerfData','-Xmx4g','-cp',cp,'NormalizeClassDiff',args.baseline,apk,changed_file,normalized_report],
        HERE/(args.case+'-normalized-class-diff.log'))
    normalized = json.loads(normalized_report.read_text())
    targets = json.loads((HERE.parent/'translations/hardcoded-ja.json').read_text())
    allowed = {t['class'] for t in targets}
    actual = set(normalized['normalized_changed_classes'])
    extra, missing = sorted(actual-allowed), sorted(allowed-actual)
    failures = []
    for field in ('added_classes','removed_classes'):
        if raw[field]:
            failures.append(field+': '+str(len(raw[field])))
    if normalized['errors']:
        failures.append('Class normalization errors')
    if extra:
        failures.append('Unexpected changed classes: '+str(len(extra)))
    if missing:
        failures.append('Expected localized classes not changed: '+str(len(missing)))
    run(['java','-XX:-UsePerfData','-Xmx3g','-cp',cp,'ReviewNewXApk',apk], HERE/(args.case+'-newx-linkage.log'))
    result = {'result':'PASS' if not failures else 'FAIL', 'apk':str(apk.resolve()),
              'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
              'baseline':str(args.baseline.resolve()),'baseline_sha256':hashlib.sha256(args.baseline.read_bytes()).hexdigest(),
              'before_classes':raw['before_classes'],'after_classes':raw['after_classes'],
              'exact_identical_classes':raw['identical_classes'],
              'jumbo_normalized_identical_classes':normalized['normalized_identical_count'],
              'localized_changed_classes':sorted(actual), 'unexpected_changed_classes':extra,
              'missing_localized_changes':missing, 'added_classes':raw['added_classes'], 'removed_classes':raw['removed_classes'],
              'newx_linkage_log':(HERE/(args.case+'-newx-linkage.log')).read_text().strip(),
              'failures':failures,
              'scope':'All APK DEX class definitions, including Hiro NewX runtime and native X hooks; only the 30 specified Piko extension classes may differ after jumbo normalization.',
              'limitations':['Requires same Piko version, selected feature set and application package as baseline.',
                             'Static artifact equivalence/linkage; no device UI execution.']}
    report = HERE/(args.case+'-preservation.json')
    report.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:result[k] for k in ('result','before_classes','after_classes','exact_identical_classes',
                                         'jumbo_normalized_identical_classes','failures')}))
    raise SystemExit(bool(failures))


if __name__ == '__main__':
    main()
