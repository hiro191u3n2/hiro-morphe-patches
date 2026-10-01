#!/usr/bin/env python3
"""Verify actual pinned Purity2 weights without emitting original model data."""
import argparse
import json
from pathlib import Path

from purity_replay import load_pinned_purity
from verify_real_model import verify_plan


def verify(model,effect_library,bytenn_library):
    plan,metadata=load_pinned_purity(model,effect_library,bytenn_library)
    return verify_plan(plan,metadata,"tt_goodlike","Purity2")


if __name__=="__main__":
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("model",type=Path)
    p.add_argument("effect_library",type=Path)
    p.add_argument("bytenn_library",type=Path)
    p.add_argument("--report",type=Path)
    args=p.parse_args()
    result=verify(args.model,args.effect_library,args.bytenn_library)
    encoded=json.dumps(result,indent=2,allow_nan=False)+"\n"
    if args.report: args.report.write_text(encoded)
    else: print(encoded,end="")
