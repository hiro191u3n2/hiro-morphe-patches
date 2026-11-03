#!/usr/bin/env python3
"""Verify the archived native space routing evidence; this is not a device test.

The original hook already covers this route. These checks must not be presented
as an old-version failure/new-version success regression.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent / 'native_space_route'

def check(condition, message):
    if not condition:
        raise AssertionError(message)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--decoded', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'manifest.json').read_text())
    data = {}
    for item in manifest['methods']:
        body = (ROOT / item['fixture']).read_text()
        check(hashlib.sha256(body.encode()).hexdigest() == item['sha256'],
              'Fixture changed: ' + item['fixture'])
        if args.decoded:
            source = (args.decoded / 'smali_classes2' /
                      (item['owner'][1:-1] + '.smali')).read_text()
            methods = [m.group() + '\n' for m in re.finditer(
                r'^\.method[^\n]*\n.*?^\.end method$', source, re.M | re.S)
                if m.group().splitlines()[0].endswith(' ' + item['signature'])]
            check(methods == [body], 'Decoded native method differs: ' + item['fixture'])
        data[item['fixture']] = body

    edges = {
        'xd0_d1__onTouchEvent.smali': ['Lxd0/d1;->f(Llh0/a;Landroid/view/MotionEvent;)Z'],
        'xd0_f0__f.smali': ['Lef0/d;->l(ILfc0/f;Lkj0/f;)V'],
        'ef0_d__l.smali': ['Ltj0/e;->G(Lfc0/f;Lef0/p;I)V', 'Ltj0/e;->Z(Lef0/p;I)V'],
        'tj0_e__Z.smali': ['Ltj0/e;->F(Lef0/p;I)V'],
        'tj0_e__F.smali': ['Lgf0/g;->c(Lef0/p;)Lgf0/g;'],
        'gf0_a__c.smali': ['Lfc0/n2;->c(Lef0/p;)V'],
        'fc0_n2__c.smali': ['Loc0/n;->c(Lef0/p;)V'],
        'oc0_m__c.smali': ['Lhc0/a;->c(Lef0/p;)V', 'Loc0/n;->c(Lef0/p;)V'],
        'oc0_b__c.smali': ['Lhc0/a;->l(Lef0/p;)V'],
        'hc0_a__l.smali': ['Lhc0/a;->j(Lef0/p;Ljava/util/Collection;)V'],
        'hc0_a__j.smali': ['Lic0/c0;->a(Lef0/p;)V'],
        'ic0_e__a.smali': ['Lic0/a;->b(Llh0/a;)V'],
        'fc0_d1__b.smali': ['Lwb0/t0;->P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V'],
        'ic0_t__b.smali': ['Lwb0/t0;->P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V'],
        'fc0_s1__n.smali': ['Lfc0/k1;->i(Lhc0/b;Lcm0/d;Lfc0/g2;Lfc0/m1;Lxc0/h;Z)V'],
        'hc0_b__i.smali': ['Lic0/d;->a(Lic0/a;)Lic0/e;', 'Lhc0/b;->d:Ljava/util/List;'],
    }
    for fixture, references in edges.items():
        for reference in references:
            check(reference in data[fixture], 'Missing route edge ' + fixture + ': ' + reference)

    for fixture, register in [('fc0_k1__e.smali', 'v5'),
                              ('fc0_k1__i.smali', 'v9'),
                              ('fc0_k1__n.smali', 'v1')]:
        body = data[fixture]
        check('const-string ' + register + ', " "' in body,
              'Space binding input literal changed: ' + fixture)
        check(re.search(r'invoke-direct \{[^}]+, ' + register + ', ' + register +
                        r', [^}]+\}, Lfc0/d1;-><init>', body),
              'Space binding no longer passes the same literal for both input strings')
    for register in ['v5', 'v6']:
        check('const-string ' + register + ', " "' in data['ic0_t__b.smali'],
              'Cycle-reset space input changed')
    check('iget-object v3, p0, Lfc0/d1;->b:Ljava/lang/String;' in data['fc0_d1__b.smali']
          and 'iget-object v4, p0, Lfc0/d1;->c:Ljava/lang/String;' in data['fc0_d1__b.smali']
          and 'invoke-interface/range {v1 .. v7}, Lwb0/t0;->P' in data['fc0_d1__b.smali'],
          'Native space action argument routing changed')

    # The route methods themselves contain no thread transfer. This does not
    # assert the behavior of every transitively invoked method or every layout.
    async_call = re.compile(r'invoke-[^\n]*(?:Ljava/util/concurrent/(?:Executor|ExecutorService);->(?:execute|submit)|Landroid/os/Handler;->(?:post|send)|Ljava/lang/Thread;->start\(|Ltm0/[^;]+;->)')
    for fixture in edges:
        check(not async_call.search(data[fixture]), 'Thread hop in route method: ' + fixture)

    report = {
        'status': 'PASSED',
        'apk_sha256': manifest['apk_sha256'],
        'native_method_fixtures_checked': len(data),
        'native_call_edges_checked': sum(map(len, edges.values())),
        'space_arguments': ['U+0020', 'U+0020'],
        'existing_hook_is_on_space_route': True,
        'ordinary_touch_route_has_direct_view_callback_entry': True,
        'explicit_thread_hop_found_in_route_methods': False,
        'baseline_failure_reproduced_by_this_test': False,
        'device_tested': False,
        'full_apk_runtime_execution': False,
    }
    rendered = json.dumps(report, indent=2, sort_keys=True) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered)
    print(rendered, end='')

if __name__ == '__main__':
    main()
