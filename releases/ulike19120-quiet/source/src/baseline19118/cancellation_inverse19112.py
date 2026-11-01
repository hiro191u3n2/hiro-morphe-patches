#!/usr/bin/env python3
"""Exact, fail-closed v112 optional-checkpoint removal for historical proofs.

The complete original v111 class is pinned. Only the reviewed v112 class hash
is eligible for this inverse; arithmetic, gates or checkpoints changed elsewhere
are rejected rather than silently replaced by the reference.
"""
from pathlib import Path
import hashlib
BASE_SHA='96a7e2c1972387494e89bc5766d7f398814b029dc990aa8534d0d8486b7c4d44'
CURRENT_SHA='9c1990fd1dad5e37d2d5d121a3c42ea17384d4470929dd44789fb04a66334ef9'
ROOT=Path(__file__).resolve().parent
def restore_text(text,source=None):
    from source_inverse19115 import restore as restore115
    text=restore115(Path(source or ROOT),'GpuStrongDefault1990.java',text.encode()).decode()
    return _restore114_text(text,source)

def _restore114_text(text,source=None):
    """Historical cancellation guard, after separately validated .115 restoration."""
    if hashlib.sha256(text.encode()).hexdigest()!=CURRENT_SHA:raise AssertionError('Strong cancellation112 source drift')
    reference=(Path(source or ROOT)/'tests19112/cancellation/reference111/GpuStrongDefault1990.java').read_text()
    if hashlib.sha256(reference.encode()).hexdigest()!=BASE_SHA:raise AssertionError('Strong cancellation112 v111 reference drift')
    return reference
