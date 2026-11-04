#!/usr/bin/env python3
"""Verify archived native metadata evidence; this is not an Android device test."""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
APK_SHA = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'


def require(value, reason):
    if not value:
        raise AssertionError(reason)


def native_boolean(body, prefix, verbatim):
    """Execute the actual small cb0.p D/Q fixture instruction subset."""
    lines = [line.strip() for line in body.splitlines() if line.strip()
             and not line.lstrip().startswith('.')]
    labels = {line: index for index, line in enumerate(lines) if line.startswith(':')}
    registers = {'p0': {'prediction': {'isPrefix': prefix, 'isVerbatim': verbatim}}}
    result = None
    pc = 0
    for _ in range(64):
        require(pc < len(lines), 'Native boolean fell off end')
        line = lines[pc]
        pc += 1
        if line.startswith(':') or line.startswith('check-cast '):
            continue
        if line.startswith('iget-object '):
            match = re.fullmatch(r'iget-object (\w+), (\w+), Lcb0/p;->a:Ljava/lang/Object;', line)
            require(match is not None, 'Unexpected native metadata field read')
            registers[match[1]] = registers[match[2]]['prediction']
        elif line.startswith('invoke-virtual '):
            match = re.fullmatch(r'invoke-virtual \{(\w+)\}, Lcom/microsoft/fluency/Prediction;->(isPrefix|isVerbatim)\(\)Z', line)
            require(match is not None, 'Unexpected native Prediction call')
            result = registers[match[1]][match[2]]
        elif line.startswith('move-result '):
            registers[line.split()[1]] = result
        elif line.startswith('const/4 '):
            match = re.fullmatch(r'const/4 (\w+), (0x[01])', line)
            require(match is not None, 'Unexpected native boolean constant')
            registers[match[1]] = int(match[2], 16)
        elif line.startswith(('if-eqz ', 'if-nez ')):
            opcode, reg, label = line.replace(',', '').split()
            is_zero = not registers[reg]
            if is_zero == (opcode == 'if-eqz'):
                pc = labels[label]
        elif line.startswith('return '):
            return bool(registers[line.split()[1]])
        else:
            raise AssertionError('Unsupported native boolean instruction: ' + line)
    raise AssertionError('Native boolean did not terminate')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path)
    parser.add_argument('--native-decoded', type=Path)
    parser.add_argument('--source', type=Path)
    args = parser.parse_args()
    evidence = json.loads((ROOT / 'evidence.json').read_text())
    require(evidence['native_reference_apk_sha256'] == APK_SHA, 'Reference APK changed')
    bodies, hashes = {}, {}
    for record in evidence['methods']:
        name = record['fixture']
        require(Path(name).name == name, 'Unexpected fixture path')
        raw = (ROOT / name).read_bytes()
        digest = hashlib.sha256(raw).hexdigest()
        require(digest == record['method_sha256'], 'Native method hash changed: ' + name)
        body = raw.decode()
        require(body.splitlines()[0] == record['method_header'], 'Native signature changed: ' + name)
        if args.native_decoded:
            owner = record['owner'][1:-1]
            originals = list(args.native_decoded.glob('smali*/' + owner + '.smali'))
            require(len(originals) == 1, 'Native class missing or duplicated: ' + owner)
            original = originals[0].read_bytes()
            require(hashlib.sha256(original).hexdigest() == record['source_class_sha256'],
                    'Native source class hash changed: ' + owner)
            require(raw in original, 'Native method does not occur verbatim in decoded reference: ' + name)
        bodies[name] = body
        hashes[name] = digest
    require(len(bodies) == 13, 'Native evidence inventory changed')
    truth_table = []
    for prefix in (False, True):
        for verbatim in (False, True):
            is_prefix = native_boolean(bodies['cb0_p__D.smali'], prefix, verbatim)
            is_literal = native_boolean(bodies['cb0_p__Q.smali'], prefix, verbatim)
            require(is_prefix == prefix, 'D no longer maps to isPrefix')
            require(is_literal == (not prefix and verbatim), 'Q no longer maps to non-prefix verbatim')
            truth_table.append({'prefix': prefix, 'verbatim': verbatim,
                                'native_D': is_prefix, 'native_Q': is_literal})
    require('new-instance p2, Lcb0/p;' in bodies['m40_c0__init.smali']
            and 'invoke-direct {p2, p1}, Lcb0/p;-><init>(Ljava/lang/Object;)V' in bodies['m40_c0__init.smali'],
            'Native candidate metadata provenance changed')
    require('iget-object v0, p0, Lm40/c0;->d:Lcb0/p;' in bodies['m40_c0__sourceMetadata.smali'],
            'Native metadata accessor changed')
    require('sget-object v0, Ln40/b;->a:Ln40/b;' in bodies['m40_h0__sourceMetadata.smali']
            and 'const/4 v0, 0x0' in bodies['n40_b__Q.smali'], 'Default metadata semantics changed')
    require('"IS_TRUE_VERBATIM"' in bodies['mb0_p0__toString.smali']
            and 'Lm40/r;->g:Lm40/f;' in bodies['mb0_p0__apply.smali'], 'Native true-verbatim predicate changed')
    compact = lambda value: '\n'.join(x.strip() for x in value.splitlines() if x.strip())
    require('new-instance v0, Lm40/f;\nconst/4 v1, 0x1\ninvoke-direct {v0, v1}, Lm40/f;-><init>(I)V\nsput-object v0, Lm40/r;->g:Lm40/f;'
            in compact(bodies['m40_r__clinit.smali']), 'True-verbatim visitor mode changed')
    wrapper = compact(bodies['m40_f__m.smali'])
    require(':pswitch_0\nsget-object p1, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;\nreturn-object p1' in wrapper
            and '.packed-switch 0x1\n:pswitch_0' in wrapper, 'k0 no longer marks true-verbatim')
    for method in ('Q', 'D'):
        require('Ln40/a;->' + method + '()Z' in bodies['ah0_x0__' + method + '.smali'],
                'g0 metadata flag forwarding changed')
    require('Lah0/x0;' in bodies['m40_g0__init.smali'], 'g0 metadata wrapper changed')
    report = {'status': 'PASSED', 'reference_apk_sha256': APK_SHA,
              'fixture_count': len(bodies), 'fixture_sha256': hashes,
              'native_truth_table': truth_table,
              'prefix_flag_requires_separate_exclusion': True,
              'default_metadata_is_not_contextual_proof': True,
              'k0_is_native_true_verbatim_wrapper': True,
              'g0_forwards_native_metadata_flags': True,
              'device_tested': False, 'original_apk_apply_tested': False}
    if args.source:
        require(args.source.is_file(), 'Runtime source missing')
        report['runtime_source_sha256'] = hashlib.sha256(args.source.read_bytes()).hexdigest()
        report['runtime_source_bound_not_executed_by_this_checker'] = True
    report['decoded_reference_rechecked'] = args.native_decoded is not None
    serialized = json.dumps(report, sort_keys=True, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(serialized)
    print(serialized, end='')


if __name__ == '__main__':
    main()
