#!/usr/bin/env python3
"""Alternating same-process real GLES regional lookup comparison, frozen .99.

This measures upload, complete Strong dispatch, readback and immutable-input
checks on software GLES only; it is never an Android speed certificate.
"""
from pathlib import Path
import argparse,importlib.util,inspect,json

def main(source,work):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 spec=importlib.util.spec_from_file_location('guard19100',source/'tests1996/shader/guard1996.py');guard=importlib.util.module_from_spec(spec);spec.loader.exec_module(guard)
 original=inspect.getsource(guard.benchmark)
 # Retain the original exact transport, alternating timing and all-output tests;
 # focus this new comparison on full Strong. Preparation is covered in the
 # inherited 1016-record matrix and .96 boundary proof.
 body=original.replace('for mode in (0,1,3):','for mode in (3,):')
 if body==original:raise AssertionError('full-kernel benchmark scope seam changed')
 scope=dict(vars(guard));exec(body,scope)
 result=scope['benchmark'](dict(published95=(source/'tests19100/strong/reference99/strong1960.comp').read_text(),candidate96=(source/'native1960/strong1960.comp').read_text()))
 for case in result['cases']:
  for key in ('median_ms','samples_ms'):
   case[key]={'published99':case[key]['published95'],'candidate19100':case[key]['candidate96']}
 result.update(status='passed',renderer=guard.getstr(0x1f01).decode(),baseline='frozen published .99',candidate='current .100',actual_gles=True,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();main(a.source,a.work)
