#!/usr/bin/env python3
"""Require two clean builds to match the reviewed local bytes and current evidence."""
import argparse, json, os
from pathlib import Path
import publish as pub

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('first',type=Path); p.add_argument('second',type=Path)
    p.add_argument('--base',type=Path,required=True); p.add_argument('--ci',action='store_true')
    a = p.parse_args(); cfg = json.loads((pub.HERE/'expected.json').read_text())
    pub.require(cfg.get('two_local_builds_identical') is True and cfg['source_sha256'] == pub.source_hashes(), 'Reviewed source changed')
    for dist in (a.first,a.second):
        qa,real,artifacts = pub.validate_build(dist,a.base)
        pub.require(artifacts == cfg['artifacts'] and pub.sha(qa) == cfg['qa_sha256'] and pub.sha(real) == cfg['real_apk_sha256'], 'Fresh build differs from reviewed identity')
    for name in (pub.SINGLE,pub.COMBINED,pub.QA_NAME): pub.require((a.first/name).read_bytes() == (a.second/name).read_bytes(), 'Fresh pair differs: '+name)
    if a.ci:
        pub.require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('GITHUB_RUN_ID') and os.environ.get('GITHUB_SHA'), 'Real CI identity required')
        record = {'run_id':os.environ['GITHUB_RUN_ID'],'source_commit':os.environ['GITHUB_SHA'],
                  'artifacts':cfg['artifacts'],'qa_sha256':cfg['qa_sha256']}
        (a.first/'ci-pair.json').write_bytes(pub.json_bytes(record))
    print('PASS exact paired build and reviewed local output identities')

if __name__ == '__main__': main()
