#!/usr/bin/env python3
"""Fail-closed inverse of the .118 scheduling delta to published .117 sources.

The frozen reference was checked against the exact public .117 source archive.
The numerical GPU implementation is separately pinned by its dependency graph;
this inverse does not claim device speed or infer a photograph comparison.
"""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parent
REFERENCE = 'tests19118/provenance/reference117'
MANIFEST = 'source_inverse19118_manifest.json'
MANIFEST_SHA256 = 'af9e53bf7ecc235177e77613d2ae19e24d35a4360cca18ffeffad0f521b2c3e7'
PUBLISHED117_ARCHIVE_SHA256 = '9ec1d61a2353ee81b2e63e5fc27edd68a5cdcb31fa699b7154983f11ad2f8e38'
REVIEWED = (
    'GpuStartup1990.java',
    'ProcessingTiming1947.java',
    'host_startup1990.py',
    'tests1990/startup/Startup1990Test.java',
)

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def manifest(source=ROOT, raw=None):
    source = Path(source)
    raw = (source / MANIFEST).read_bytes() if raw is None else raw
    assert type(raw) is bytes and sha(raw) == MANIFEST_SHA256, 'Pinned .118 inverse manifest differs'
    value = json.loads(raw)
    assert value['schema'] == 'ulike19118-complete-source-inverse-v1'
    assert value['previous_release'] == 'ulike-v1.9.117'
    assert value['published_archive_sha256'] == PUBLISHED117_ARCHIVE_SHA256
    assert tuple(value['reviewed_files']) == REVIEWED and set(value['files']) == set(REVIEWED)
    for entry in value['files'].values():
        assert set(entry) == {'previous_sha256', 'current_sha256'}
        assert entry['previous_sha256'] != entry['current_sha256']
    return value

def restore_to117(source, name, raw=None):
    source = Path(source)
    assert name in REVIEWED, 'Unreviewed .118 inverse request: ' + name
    entry = manifest(source)['files'][name]
    previous = (source / REFERENCE / name).read_bytes()
    assert sha(previous) == entry['previous_sha256'], 'Frozen published .117 source differs: ' + name
    current = (source / name).read_bytes() if raw is None else raw
    assert type(current) is bytes, 'Source must be raw bytes: ' + name
    assert sha(current) in (entry['current_sha256'], entry['previous_sha256']), 'Source differs outside reviewed .118 delta: ' + name
    return previous

def test(source=ROOT, work=None, jdk=None, ndk=None):
    source = Path(source)
    manifest(source)
    count = 0
    for name in REVIEWED:
        current = (source / name).read_bytes()
        previous = (source / REFERENCE / name).read_bytes()
        assert current != previous, 'Missing reviewed .118 delta: ' + name
        assert restore_to117(source, name, current) == previous
        assert restore_to117(source, name, previous) == previous
        count += 2
        for corrupted in (current + b'\n', b''):
            try:
                restore_to117(source, name, corrupted)
            except AssertionError:
                count += 1
            else:
                raise AssertionError('Altered .118 source accepted: ' + name)
    try:
        manifest(source, (source / MANIFEST).read_bytes() + b'\n')
    except AssertionError:
        count += 1
    else:
        raise AssertionError('Altered .118 manifest accepted')
    return dict(schema='ulike19118-source-inverse-proof-v1', status='passed',
                assertions=count, reviewed_files=list(REVIEWED),
                mutations_rejected=2 * len(REVIEWED) + 1,
                manifest_sha256=MANIFEST_SHA256,
                published_archive_sha256=PUBLISHED117_ARCHIVE_SHA256,
                exact_previous_sources_verified=True, physical_android_tested=False,
                device_speedup_verified=False)

if __name__ == '__main__':
    print(json.dumps(test(), sort_keys=True))
