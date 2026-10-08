"""Strict published .53 source view and current helper compilation closure.

Every historical source byte is pinned. Changed-family files have independent
original snapshots; unchanged files are accepted only at their .53 checksum.
The oracle view contains the historical tree without new .54 implementation.
"""
from pathlib import Path
import hashlib
import json
import re

PIN_MANIFEST_SHA256 = '25fbe7bfc04d551b78f89c0f8c50e6cc7a2204c169363a353967d0fb99e6c6d6'
BASELINE_MPP_SHA256 = '0169006ac7d86349ce6dd274b263332a7bd202845fe8bb28f8c947ae662ee982'


def source_pins(root):
    reference = Path(root) / 'tests/published1953-reference'
    data = (reference / 'pins.json').read_bytes()
    if hashlib.sha256(data).hexdigest() != PIN_MANIFEST_SHA256:
        raise AssertionError('Changed independent published .53 source pin manifest')
    pins = json.loads(data)
    if (pins.get('schema') != 'ulike-published1953-source-oracle-v1'
            or pins.get('baseline_version') != '1.9.53'
            or pins.get('baseline_mpp_sha256') != BASELINE_MPP_SHA256):
        raise AssertionError('Wrong independent published .53 source oracle identity')
    for name, expected in pins['snapshots'].items():
        if hashlib.sha256((reference / name).read_bytes()).hexdigest() != expected:
            raise AssertionError('Changed independent published .53 source snapshot: ' + name)
        if pins['historical_files'].get(name) != expected:
            raise AssertionError('Published .53 snapshot and historical file pins differ: ' + name)
    return pins


def inherited1953_root(root, work):
    root = Path(root).resolve()
    pins = source_pins(root)
    reference = root / 'tests/published1953-reference'
    view = Path(work).resolve() / 'inherited-source1953/releases/ulike1953-h16-h21/src'
    view.mkdir(parents=True, exist_ok=True)
    for name, expected in pins['historical_files'].items():
        source = reference / name if name in pins['snapshots'] else root / name
        data = source.read_bytes()
        if hashlib.sha256(data).hexdigest() != expected:
            raise AssertionError('Historical .53 source changed without its original snapshot: ' + name)
        target = view / name
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.is_symlink():
            target.unlink()
        target.write_bytes(data)
    return view


def sources1954(root, sources):
    """Include production .54 helper references without replacing their code."""
    from host_common1953 import sources1953
    root = Path(root)
    result = sources1953(root, sources)
    # H27 can introduce a production ownership helper while retaining the .53
    # roots. Follow only explicitly referenced root Java types with the .54
    # suffix; unrelated transformers and tests are never compiler inputs.
    while True:
        text = '\n'.join(Path(p).read_text() for p in result if Path(p).suffix == '.java')
        added = False
        for name in sorted(set(re.findall(r'\b([A-Za-z_$][A-Za-z0-9_$]*1954)\b', text))):
            path = root / (name + '.java')
            if path.is_file() and path not in result:
                result.append(path)
                added = True
        if not added:
            return result
