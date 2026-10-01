#!/usr/bin/env python3
"""Strict Purity2 host tensor replay via the audited Espresso/ByteNN shared core.

The caller supplies original model and both matching libraries. No original data
or constants needed to decode that model are included in this module.
"""
from pathlib import Path

from baoman_replay import _plan,run_tensor
from graph_schema import parse_graph,UnsupportedGraph


def load_pinned_purity(model_path: Path, effect_library_path: Path, bytenn_library_path: Path):
    from legacy_graph_metadata import load_goodlike
    graph_bytes,weights,metadata=load_goodlike(model_path,effect_library_path,bytenn_library_path)
    plan=_plan(parse_graph(graph_bytes.decode("ascii")),weights)
    if plan.input_shape!=(1,3,256,256) or plan.layers[-1].shape!=(1,4,256,256) or plan.weight_count!=250004 or len(plan.layers)!=96:
        raise UnsupportedGraph("pinned Purity topology/weight aggregate mismatch")
    return plan,metadata
