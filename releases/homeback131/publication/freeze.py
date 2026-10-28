#!/usr/bin/env python3
"""Freeze two actual identical local builds and their original-APK evidence."""
import argparse, json, re
from pathlib import Path
import publish as pub

def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('first','second','base','repo'): p.add_argument('--'+name,type=Path,required=True)
    for name in ('reviewed-main','reviewed-dev'): p.add_argument('--'+name,required=True)
    a = p.parse_args()
    for value in (a.reviewed_main,a.reviewed_dev): pub.require(re.fullmatch(r'[0-9a-f]{40}',value) is not None, 'Exact reviewed heads required')
    qa,real,artifacts = pub.validate_build(a.first,a.base)
    qa2,real2,artifacts2 = pub.validate_build(a.second,a.base)
    pub.require(qa == qa2 and real == real2 and artifacts == artifacts2, 'Independent builds differ')
    for name in (pub.SINGLE,pub.COMBINED,pub.QA_NAME):
        pub.require((a.first/name).read_bytes() == (a.second/name).read_bytes(), 'Paired artifact differs: '+name)
    pub.require((a.repo/'.github/workflows/homeback131-publish.yml').read_bytes() == (pub.HERE/'workflow.yml').read_bytes(), 'Workflow mirror differs')
    config = {'schema':'homeback131-frozen-v1','version':pub.VERSION,'bundle_version':pub.BUNDLE,
        'baseline_version':pub.PREVIOUS,'baseline_bytes':pub.build.BASE_BYTES,'baseline_sha256':pub.BASE_SHA256,'baseline_url':pub.BASE_URL,
        'reviewed_main_head':a.reviewed_main,'reviewed_dev_head':a.reviewed_dev,'two_local_builds_identical':True,
        'artifacts':artifacts,'qa_sha256':pub.sha(qa),'real_apk_sha256':pub.sha(real),
        'physical_android_tested':False,'source_sha256':pub.source_hashes()}
    (pub.HERE/'expected.json').write_bytes(pub.json_bytes(config))
    print('PASS frozen exact clean-build pair and original-APK identities; no remote writes')

if __name__ == '__main__': main()
