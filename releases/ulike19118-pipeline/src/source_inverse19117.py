#!/usr/bin/env python3
"""Fail-closed inverse from reviewed .117 sources to frozen published .116.

This proves source identity only. It does not claim numerical equivalence,
hardware execution, or a saving in elapsed capture time.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import zipfile
import source_inverse19118

ROOT = Path(__file__).resolve().parent
MANIFEST = 'source_inverse19117_manifest.json'
MANIFEST_SHA256 = '66f02643ccef522c613936ffa2259fc73ddc0ba6ef142af74d93e66913b8bdc4'
REFERENCE = 'tests19117/provenance/reference116'
ARCHIVE_SHA256 = '38554f719d7714cb3dcd4dc324d6b94f1602627c8f8667b59e5c7021fadaf247'
REVIEWED = (
    'GpuAnalysis1961.java',
    'GpuNoise1960.java',
    'GpuQualification1961.java',
    'GpuStartup1990.java',
    'GpuStrong1960.java',
    'GpuStrongDefault1990.java',
    'ProcessingTiming1947.java',
    'native1960/engine1960.c',
)

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def manifest(source=ROOT, raw=None):
    raw = (Path(source) / MANIFEST).read_bytes() if raw is None else raw
    assert type(raw) is bytes
    assert sha(raw) == MANIFEST_SHA256, 'Pinned .117 source inverse manifest differs'
    value = json.loads(raw)
    assert value['schema'] == 'ulike19117-complete-source-inverse-v1'
    assert value['previous_release'] == 'ulike-v1.9.116'
    assert value['published_archive_sha256'] == ARCHIVE_SHA256
    assert tuple(value['reviewed_files']) == REVIEWED and set(value['files']) == set(REVIEWED)
    return value

def restore_to116(source, name, raw=None):
    source = Path(source)
    assert name in REVIEWED, 'Unreviewed .117 inverse request: ' + name
    entry = manifest(source)['files'][name]
    previous = (source / REFERENCE / name).read_bytes()
    assert sha(previous) == entry['previous_sha256'], 'Frozen published116 source differs: ' + name
    current = (source / name).read_bytes() if raw is None else raw
    assert type(current) is bytes, 'Source must be raw bytes: ' + name
    if sha(current) not in (entry['current_sha256'], entry['previous_sha256']) and name in source_inverse19118.REVIEWED:
        current = source_inverse19118.restore_to117(source, name, current)
    assert sha(current) in (entry['current_sha256'], entry['previous_sha256']), 'Source differs outside reviewed .117 delta: ' + name
    return previous

def test(source=ROOT, work=None, jdk=None, ndk=None):
    source = Path(source)
    manifest(source)
    count = 0
    for name in REVIEWED:
        current = (source / name).read_bytes()
        previous = (source / REFERENCE / name).read_bytes()
        assert current != previous, 'Missing reviewed .117 delta: ' + name
        assert restore_to116(source, name, current) == previous
        assert restore_to116(source, name, previous) == previous
        count += 2
        try:
            restore_to116(source, name, current + b'\n')
        except AssertionError:
            count += 1
        else:
            raise AssertionError('Altered .117 source accepted: ' + name)
    try:
        manifest(source, (source / MANIFEST).read_bytes() + b'\n')
    except AssertionError:
        count += 1
    else:
        raise AssertionError('Altered .117 manifest accepted')
    return dict(schema='ulike19117-source-inverse-proof-v1', status='passed',
                assertions=count, reviewed_files=list(REVIEWED),
                mutations_rejected=len(REVIEWED) + 1,
                manifest_sha256=MANIFEST_SHA256,
                published_archive_sha256=ARCHIVE_SHA256,
                exact_previous_sources_verified=True, physical_android_tested=False,
                device_speedup_verified=False)

def freeze(source=ROOT):
    source = Path(source)
    archive = source.parent.parent / 'ulike19116-published/ULike_v1.9.116_sources_and_QA.zip'
    assert sha(archive.read_bytes()) == ARCHIVE_SHA256, 'Wrong published .116 source archive'
    files = {}
    with zipfile.ZipFile(archive) as published:
        for name in REVIEWED:
            previous = (source / REFERENCE / name).read_bytes()
            assert previous == published.read('src/' + name), 'Reference not published .116 source: ' + name
            current = (source / name).read_bytes()
            assert current != previous, 'Missing .117 change: ' + name
            files[name] = dict(previous_sha256=sha(previous), current_sha256=sha(current))
    value = dict(schema='ulike19117-complete-source-inverse-v1',
                 previous_release='ulike-v1.9.116',
                 published_archive_sha256=ARCHIVE_SHA256,
                 reviewed_files=list(REVIEWED), files=files)
    raw = (json.dumps(value, sort_keys=True, indent=2) + '\n').encode()
    (source / MANIFEST).write_bytes(raw)
    path = source / 'source_inverse19117.py'
    text = path.read_text()
    text, changes = re.subn(r"(?m)^MANIFEST_SHA256 = '[^']*'$",
                            "MANIFEST_SHA256 = '" + sha(raw) + "'", text)
    assert changes == 1
    path.write_text(text)
    return sha(raw)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--freeze', action='store_true')
    args = parser.parse_args()
    print(freeze() if args.freeze else json.dumps(test()))
