#!/usr/bin/env python3
"""Audit noncode preservation for the real 1.1.14 -> 1.1.15 CLI upgrade.

DEX layout can differ (the extension merger emits a separate DEX); all executable
code is independently checked by ActualXAudit against the original APK and MPP.
"""
import argparse
import json
import re
from pathlib import Path
import resource_audit as audit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('aapt2', 'original', 'fresh', 'prior', 'upgraded', 'output'):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    audit.require(audit.file_sha(args.aapt2) == audit.AAPT2_SHA256, 'Unreviewed aapt2')
    audit.require(audit.file_sha(args.original) == audit.INPUT_SHA256, 'Unreviewed original X APK')
    audit.require(audit.file_sha(args.prior) == '558b030fe5bfacb9e3d4e60ca454d1b936742f9a86e2692da44a668cf084ca75', 'Unreviewed actual 1.1.14 input')
    entries = {name: audit.apk_entries(getattr(args, name)) for name in ('original','fresh','prior','upgraded')}
    immutable = {name: {key: val for key, val in data.items() if not audit.mutable_entry(key)} for name,data in entries.items()}
    for name in ('fresh', 'prior', 'upgraded'):
        audit.require(immutable[name] == immutable['original'], 'Original noncode entries changed: '+name)
    fresh_manifest = audit.dump(args.aapt2, args.fresh, 'xmltree')
    upgrade_manifest = audit.dump(args.aapt2, args.upgraded, 'xmltree')
    audit.require(audit.manifest_model(fresh_manifest) == audit.manifest_model(upgrade_manifest), 'Fresh/upgrade decoded manifest differs')
    manifest = audit.compare_manifest(audit.dump(args.aapt2,args.original,'xmltree'),upgrade_manifest)
    resource = audit.compare_resources(audit.dump(args.aapt2,args.original,'resources'),audit.dump(args.aapt2,args.upgraded,'resources'))
    dex = {name: sorted(key for key in data if re.fullmatch(r'classes(?:[2-9][0-9]*|1[0-9]+)?\.dex', key)) for name,data in entries.items()}
    report = {
        'schema':'homeback125-real124-upgrade-resource-audit-v1','result':'PASS',
        'physical_android_tested':False,
        'original_sha256':audit.file_sha(args.original),
        'prior124_sha256':audit.file_sha(args.prior),
        'fresh125_sha256':audit.file_sha(args.fresh),
        'upgraded125_sha256':audit.file_sha(args.upgraded),
        'byte_exact_original_noncode_entries':len(immutable['original']),
        'fresh_upgrade_decoded_manifest_equal':True,
        'resource_table':resource,
        'manifest':{key: val for key,val in manifest.items() if key!='changes'},
        'dex_entry_counts':{name:len(data) for name,data in dex.items()},
        'dex_layout_differences':'Fresh and upgrade DEX entry counts may differ; all original app code and current runtime are checked independently in both outputs by ActualXAudit.'
    }
    args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__ == '__main__':
    main()
