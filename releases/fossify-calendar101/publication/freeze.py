#!/usr/bin/env python3
"""Freeze locally tested artifacts and reviewed source identities for CI."""
import argparse
import json
from pathlib import Path
import re
import publish as p


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', required=True, type=Path)
    parser.add_argument('--dist', required=True, type=Path)
    parser.add_argument('--reviewed-main', required=True)
    parser.add_argument('--reviewed-dev', required=True)
    args = parser.parse_args()
    for commit in (args.reviewed_main, args.reviewed_dev):
        p.require(re.fullmatch('[0-9a-f]{40}', commit), 'Invalid reviewed commit')
    qa_raw = (args.dist / p.QA).read_bytes()
    qa = json.loads(qa_raw)
    p.require(qa['supplied_apk_sha256'] and len(qa['apk_application']) == 2,
              'Freeze requires the local build tested against the supplied APK')
    artifacts = {n: {'bytes': (args.dist / n).stat().st_size,
                     'sha256': p.sha((args.dist / n).read_bytes())} for n in (p.SINGLE, p.BUNDLE)}
    p.require(qa['artifacts'] == artifacts, 'QA and MPP identities differ')
    verification = p.ROOT / 'verification'
    verification.mkdir(exist_ok=True)
    (verification / p.QA).write_bytes(qa_raw)
    cfg = {'schema': 'fossify-calendar101-publication-v1', 'ready': True,
           'baseline_url': p.BASE_URL, 'baseline_sha256': p.BASE_SHA,
           'reviewed_main': args.reviewed_main, 'reviewed_dev': args.reviewed_dev,
           'patch_prefix': 'app/hiro/fossifycalendar/patches/',
           'runtime_extension': 'extensions/fossify_calendar_colors.mpe',
           'supplied_apk_sha256': qa['supplied_apk_sha256'], 'artifacts': artifacts,
           'local_qa_sha256': p.sha(qa_raw),
           'source_sha256': {n: p.sha(v) for n, v in p.source_files(args.repo.resolve()).items()}}
    (p.HERE / 'manifest.json').write_bytes(p.encoded(cfg))
    print('PASS frozen ' + str(len(cfg['source_sha256'])) + ' source/evidence files and both MPPs')


if __name__ == '__main__':
    main()
