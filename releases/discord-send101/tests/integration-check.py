#!/usr/bin/env python3
"""Compare equivalent Venus-only and Venus+Hiro patcher outputs, without running Android."""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

parser = argparse.ArgumentParser(description=__doc__)
for name in ('baseline', 'candidate', 'baseline-result', 'result', 'out'):
    parser.add_argument('--' + name, type=Path, required=True)
args = parser.parse_args()
name = '送信ボタンの表示消失を修正'
bundle = 'assets/index.android.bundle'
sha = lambda data: hashlib.sha256(data).hexdigest()
left = json.loads(args.baseline_result.read_text())
right = json.loads(args.result.read_text())
assert left['success'] and right['success'], 'A patcher invocation failed'
assert len(right['appliedPatches']) == len(set(right['appliedPatches'])), 'Duplicate patches'
assert set(right['appliedPatches']) == set(left['appliedPatches']) | {name}, 'Selection mismatch'
assert len(right['appliedPatches']) == len(left['appliedPatches']) + 1
with zipfile.ZipFile(args.baseline) as a, zipfile.ZipFile(args.candidate) as b:
    assert a.testzip() is None and b.testzip() is None
    assert len(a.namelist()) == len(set(a.namelist()))
    assert len(b.namelist()) == len(set(b.namelist()))
    assert set(a.namelist()) == set(b.namelist()), 'APK entry inventory differs'
    changed, unchanged = [], []
    for entry in a.namelist():
        if a.read(entry) == b.read(entry):
            unchanged.append(entry)
        else:
            changed.append(entry)
    assert changed == [bundle], 'Unexpected changes: ' + repr(changed)
    before, after = a.read(bundle), b.read(bundle)
    assert len(before) == len(after)
    assert hashlib.sha1(before[:-20]).digest() == before[-20:]
    assert hashlib.sha1(after[:-20]).digest() == after[-20:]
    expected = bytearray(before)
    for offset, old, new in [(36285345, 'b1e500000008', 'afe500000000'),
                             (36286539, 'b06010', 'ae6000')]:
        old, new = bytes.fromhex(old), bytes.fromhex(new)
        assert before[offset:offset + len(old)] == old
        expected[offset:offset + len(new)] = new
    expected[-20:] = hashlib.sha1(expected[:-20]).digest()
    assert after == expected, 'Bundle changes differ from the verified two branches'
    report = {
        'schema': 'discord-send101-integration-v1', 'result': 'PASS',
        'baseline_applied_patches': left['appliedPatches'],
        'candidate_applied_patches': right['appliedPatches'],
        'baseline_apk_sha256': sha(args.baseline.read_bytes()),
        'candidate_apk_sha256': sha(args.candidate.read_bytes()),
        'baseline_bundle_sha256': sha(before), 'candidate_bundle_sha256': sha(after),
        'bundle_size': len(after), 'entry_inventory_identical': True,
        'unchanged_entry_count': len(unchanged), 'changed_entries': changed,
        'all_dex_and_venus_assets_identical': True,
        'changed_body_byte_offsets': [36285345, 36285350, 36286539, 36286541],
        'both_sha1_footers_valid': True, 'android_runtime_tested': False,
    }
args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({key: report[key] for key in ('result', 'unchanged_entry_count', 'changed_entries',
      'baseline_bundle_sha256', 'candidate_bundle_sha256')}, ensure_ascii=False))
