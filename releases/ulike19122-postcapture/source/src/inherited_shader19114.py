"""Compose exact .114/.113 inverses for inherited historical source guards.

This module never feeds a production build or the numerical execution path.
Every caller compares the restored text/hash with its independent frozen source.
The current shader remains the shader compiled by JNI/GLES tests.
"""
from source_inverse19115 import restore_shader19115
from strong_delta19113 import BASELINE_SHA256, restore_shader19113 as _restore113
from strong_delta19114 import restore_shader19114


def restore_shader19113(text, required=False):
    # The complete reviewed .115 source is restored before older inverses.
    if '19115' in text:
        text = restore_shader19115(text)
    # Partial/altered .114 blocks must not silently become a historical input.
    if '19114' in text:
        text = restore_shader19114(text, required=True)
    return _restore113(text, required=required)
