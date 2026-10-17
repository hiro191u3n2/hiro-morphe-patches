#!/usr/bin/env python3
"""Stable summary of read-only real APK/extension ABI evidence (no device claim)."""
from pathlib import Path
import json

root = Path(__file__).resolve().parent
reports = sorted(root.glob('*_input_audit.json'))
if len(reports) != 3:
    raise SystemExit('Expected all three supplied APK reports')
inputs = []
replacement_digests = set()
for report_path in reports:
    report = json.loads(report_path.read_text())
    if report['result'] != 'PASS' or not report.get('replacement_abi_valid_for_all_existing_hooks'):
        raise SystemExit('Final replacement ABI audit required: ' + report_path.name)
    replacement_digests.add(report['replacement_runtime_sha256'])
    inputs.append({
        'filename': report['input_apk'],
        'package': report['package'],
        'version': report['version'],
        'version_code': report['version_code'],
        'sha256': report['input_sha256'],
        'report': report_path.name,
        'findings': {
            'old_runtime_matches_single_v1_1_6': report['matches_known_v116_runtime'],
            'old_runtime_classes': report['existing_runtime_classes'],
            'duplicate_classes': len(report['duplicate_class_definitions']),
            'class_count': report['total_classes'],
            'existing_external_runtime_callsites': report['external_runtime_callsites'],
            'existing_hooked_methods': report['external_hooked_methods'],
            'existing_unique_runtime_entrypoints': len(report['runtime_entrypoints_referenced_by_apk']),
            'all_existing_runtime_hook_abis_preserved': report['replacement_abi_valid_for_all_existing_hooks'],
            'replacement_abi_errors': report['replacement_abi_errors'],
            'old_public_methods_preserved': report['old_public_methods_preserved'],
        },
    })
if len(replacement_digests) != 1:
    raise SystemExit('Mixed replacement runtimes')
summary = {
    'schema': 'homeback119-provided-apk-audit-v1',
    'inputs_already_patched': True,
    'original_apk_apply_tested': False,
    'physical_android_tested': False,
    'input_apks_modified': False,
    'replacement_runtime_sha256': replacement_digests.pop(),
    'inputs': inputs,
    'result': 'PASS',
    'scope': 'Static read-only audit of all DEX files in all three supplied, already patched APKs; known v1.1.6 extension identity and every existing external extension hook descriptor/access/static ABI checked against compiled v1.1.10. No clean original APK application, APK reconstruction, signing, installation or Android UI test.',
}
dest = root / 'provided_apk_audit_summary.json'
dest.write_text(json.dumps(summary, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
print('PASS', dest)
