#!/usr/bin/env python3
"""Verify pinned MPP/code/evidence plus freshly generated, non-deterministic CI logs.

Host logs intentionally retain timings and compiler paths. They are not app inputs.
Only their containing ZIP and its derived checksum list vary by verification run.
No executable, source, reviewed evidence, QA, or release-note pin is relaxed.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

QA = 'QA_ULike_v1.9.29.json'
NOTES = 'RELEASE_NOTES.txt'
ARCHIVE = 'ULike_v1.9.29_sources_and_QA.zip'
SUMS = 'SHA256SUMS.txt'
LOGS = (
    'host-tests1920.txt', 'host-tests1921.txt', 'host-tests1922.txt',
    'host-tests1923.txt', 'host-pipeline1924.txt', 'host-gesture1925.txt',
    'host-restart1926.txt', 'host-start1927.txt', 'host-facing1928.txt',
    'host-audit1929.txt', 'restart-contracts.txt', 'start1927-contracts.txt',
    'audit1929-contracts.txt', 'emitted-audit.tsv', 'references.log',
)
MARKERS = {
    'host-tests1920.txt': 'PASS total=71',
    'host-tests1921.txt': 'exit assertions=79',
    'host-tests1922.txt': 'HOST_LAYOUT_ASSERTIONS=134',
    'host-tests1923.txt': 'HOST_SHADOW_ASSERTIONS=223',
    'host-pipeline1924.txt': 'HOST_PIPELINE_ASSERTIONS=299',
    'host-gesture1925.txt': 'HOST_GESTURE_ASSERTIONS=66',
    'host-restart1926.txt': 'checks=132',
    'host-start1927.txt': 'checks=283',
    'host-facing1928.txt': 'PASS 73 facing persistence assertions',
    'host-audit1929.txt': 'PASS1929 shipped helper assertions=172',
}

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def digest(data):
    return hashlib.sha256(data).hexdigest()

def verify(manifest_path, dist, build):
    manifest = json.loads(manifest_path.read_text())
    require(set(manifest['expected_dist']) == {QA, NOTES, ARCHIVE, SUMS},
            'Unexpected manifest distribution files')
    require(set(manifest['expected_mpp']) == {
        'ULike_HQ_Texture_Online_v1.9.29.mpp', 'Hiro_Morphe_Patches_v1.0.156.mpp'
    }, 'Unexpected executable set')
    for name, pin in manifest['expected_mpp'].items():
        data = (dist / name).read_bytes()
        require(digest(data) == pin['sha256'] and len(data) == pin['bytes'],
                'Original-APK-validated MPP mismatch: ' + name)
    for name in (QA, NOTES):
        require(digest((dist / name).read_bytes()) == manifest['expected_dist'][name],
                'Desktop-pinned QA/release notes mismatch: ' + name)
    qa = json.loads((dist / QA).read_text())
    require(qa['status'] == 'REBUILD_MATCHES_DESKTOP_TESTED_MPP_DEVICE_UNVERIFIED',
            'QA has not been finalized')
    require(qa['host_tests']['total'] == 1532 and not qa['device_tested'],
            'Verification scope mismatch')
    source_pins = {'src/' + n: h for n, h in qa['source_sha256'].items()}
    evidence_pins = {'evidence/' + n: h for n, h in qa['desktop_evidence_sha256'].items()}
    require(len(source_pins) == manifest['source_files'] and
            len(evidence_pins) == manifest['evidence_files'], 'Source/evidence count mismatch')
    pins = dict(source_pins, **evidence_pins)
    current_files = (QA, NOTES, *LOGS)
    for name in LOGS:
        raw = (dist / name).read_bytes()
        require(raw == (build / name).read_bytes(), 'Not the current CI log: ' + name)
        if name in MARKERS:
            require(MARKERS[name] in raw.decode('utf-8'), 'Missing host evidence: ' + name)
    with zipfile.ZipFile(dist / ARCHIVE) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), 'Duplicate ZIP entries')
        require(set(names) == set(pins) | set(current_files), 'Unexpected ZIP entries')
        require(z.testzip() is None, 'ZIP CRC failure')
        for name, pin in pins.items():
            require(digest(z.read(name)) == pin, 'Reviewed ZIP content mismatch: ' + name)
        for name in current_files:
            require(z.read(name) == (dist / name).read_bytes(), 'ZIP output/log mismatch: ' + name)
    names = [*manifest['expected_mpp'], QA, NOTES, ARCHIVE]
    sums = ''.join(digest((dist / n).read_bytes()) + '  ' + n + '\n' for n in names)
    require((dist / SUMS).read_text() == sums, 'Checksum file does not cover exact current outputs')
    print('PASS exact pinned MPP, QA, notes, all 162 source and 20 reviewed evidence files; '
          '15 fresh CI logs match build and archive; checksum file covers complete current outputs.')
    print('INFO archive SHA256=' + digest((dist / ARCHIVE).read_bytes()) +
          '; timing/compiler-path-bearing logs are run-specific, not executable inputs; no device claim.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for flag in ('manifest', 'dist', 'build'):
        parser.add_argument('--' + flag, type=Path, required=True)
    args = parser.parse_args()
    verify(args.manifest, args.dist, args.build)
