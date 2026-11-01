#!/usr/bin/env python3
"""Restore pinned *published118* inputs for immutable historical host unit tests.

This does not build any native payload, create119 evidence, or change a baseline
source file. The original QA file supplies the historical optimization-provenance
objects expected by host19101_release_gate.py; its bytes are never rewritten.
"""
from pathlib import Path
import argparse
import hashlib
import json
import subprocess
import sys
import zipfile

MANIFEST_SHA256 = '6ee3a313a762c3b9d7000221712bffbea6d66b7a1ccce55b8f7ecfdaab9fea35'
QA_SHA256 = '8f5c278738cfb79fff820df094fe6bfcf4896a297d518d87e825ecb2d877148a'
SINGLE_NAME = 'ULike_HQ_Texture_Online_v1.9.118.mpp'
SINGLE_SHA256 = '536d022c6a60766461b64951e3f695a2ffb0613d34d41e76ffe81e8a99c30189'
SINGLE_BYTES = 1528249


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def need(condition, message):
    if not condition:
        raise AssertionError(message)


def immutable_file(path, expected_hash, expected_bytes=None):
    path = Path(path)
    need(path.is_file() and not path.is_symlink(), 'Missing/nonregular published118 input: ' + str(path))
    raw = path.read_bytes()
    need(digest(raw) == expected_hash, 'Published118 input hash differs: ' + str(path))
    need(expected_bytes is None or len(raw) == expected_bytes, 'Published118 input size differs: ' + str(path))
    return raw


def restore(source118, single118):
    """Return input provenance after verifying and restoring old118 dist inputs."""
    source = Path(source118).resolve()
    release = source.parent
    manifest_raw = immutable_file(release / 'manifest.json', MANIFEST_SHA256)
    qa_raw = immutable_file(release / 'QA_ULike_v1.9.118.json', QA_SHA256)
    single_raw = immutable_file(single118, SINGLE_SHA256, SINGLE_BYTES)
    manifest, qa = json.loads(manifest_raw), json.loads(qa_raw)
    need(manifest['reviewed_source_sha256'] == qa['source_sha256'], 'Published118 QA/source manifest differ')
    for name, expected in manifest['reviewed_source_sha256'].items():
        path = source / name
        need(path.resolve().is_relative_to(source), 'Baseline source path escapes root')
        immutable_file(path, expected)
    need(manifest['reviewed_mpp_artifacts'][SINGLE_NAME] == {'sha256': SINGLE_SHA256, 'bytes': SINGLE_BYTES},
         'Published118 manifest artifact pin differs')
    with zipfile.ZipFile(single118) as artifact:
        for kind, name in [('gpu_native', 'ulike1960/runtime/libulike_gpu1960.so'),
                           ('nr_native', 'ulike1955/runtime/libulike_nr1955.so'),
                           ('smooth_native', 'ulike1958/runtime/libulike_smooth1958.so')]:
            raw = artifact.read(name)
            need(digest(raw) == qa[kind]['sha256'] and len(raw) == qa[kind]['bytes'],
                 'Published118 QA does not identify payload: ' + name)
    # Isolated interpreter is essential: current119 build/provenance modules may
    # already be imported by the caller and must not shadow baseline118 modules.
    code = '''from pathlib import Path
import json,sys,zipfile
source=Path(sys.argv[1]);sys.path.insert(0,str(source))
import native_provenance19115,native_smooth_provenance19115
qa=json.loads((source.parent/'QA_ULike_v1.9.118.json').read_text())
with zipfile.ZipFile(sys.argv[2]) as artifact:
 gpu=artifact.read('ulike1960/runtime/libulike_gpu1960.so')
 nr=artifact.read('ulike1955/runtime/libulike_nr1955.so')
 smooth=artifact.read('ulike1958/runtime/libulike_smooth1958.so')
native_provenance19115.verify(source,gpu,nr,qa['gpu_native']['optimization_provenance'])
native_smooth_provenance19115.verify(source,smooth,qa['smooth_native']['optimization_provenance'])
print('PUBLISHED118_PROVENANCE_VERIFIED')
'''
    done = subprocess.run([sys.executable, '-c', code, str(source), str(Path(single118).resolve())],
                          capture_output=True, text=True, timeout=120)
    need(done.returncode == 0 and done.stdout.strip() == 'PUBLISHED118_PROVENANCE_VERIFIED',
         'Published118 complete native provenance failed: ' + (done.stdout + done.stderr)[-6000:])
    dist = release / 'dist'
    need(not dist.is_symlink(), 'Baseline dist must not be a symlink')
    dist.mkdir(exist_ok=True)
    for path, raw in [(dist / 'build19101.json', qa_raw), (dist / SINGLE_NAME, single_raw)]:
        need(not path.is_symlink(), 'Baseline input target must not be a symlink')
        if path.exists():
            need(path.is_file() and path.read_bytes() == raw, 'Refusing to replace different baseline input: ' + str(path))
        else:
            path.write_bytes(raw)
    return {'schema': 'ulike118-published-baseline-test-input-recovery-v1',
            'status': 'passed', 'scope': 'published118 inputs for immutable historical tests only',
            'baseline_manifest_sha256': MANIFEST_SHA256, 'original_qa_sha256': QA_SHA256,
            'single_sha256': SINGLE_SHA256, 'single_bytes': SINGLE_BYTES,
            'source_files_verified': len(manifest['reviewed_source_sha256']),
            'original_native_provenance_verified': True,
            'new_native_build_claimed': False, 'current119_numerical_evidence': False,
            'baseline_source_changed': False, 'physical_android_tested': False}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source118', type=Path, required=True)
    parser.add_argument('--single118', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(restore(args.source118, args.single118), sort_keys=True, indent=2))
