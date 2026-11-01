#!/usr/bin/env python3
"""Freeze the reviewed .120/.326 publication without any remote write."""
from pathlib import Path
import argparse
import json
import re
import shutil
import publish19120 as p


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('repo', 'dist', 'baseline', 'source', 'bundle-report', 'combined-application'):
        parser.add_argument('--' + name, type=Path, required=True)
    for name in ('build-report', 'host-report', 'cached-report'):
        parser.add_argument('--' + name, type=Path)
    parser.add_argument('--reviewed-main', required=True)
    parser.add_argument('--reviewed-dev', required=True)
    parser.add_argument('--authorized', action='store_true')
    args = parser.parse_args()
    args.repo, args.dist, args.source = args.repo.resolve(), args.dist.resolve(), args.source.resolve()
    p.require(args.authorized, 'This release requires the user publication instruction')
    p.require(all(re.fullmatch('[a-f0-9]{40}', value) for value in (args.reviewed_main, args.reviewed_dev)), 'Exact reviewed commits required')
    p.require(p.git(args.repo, 'rev-parse', 'HEAD') == args.reviewed_main, 'Freeze must use the reviewed main checkout')
    args.build_report = args.build_report or args.source / 'evidence/build19120.json'
    args.host_report = args.host_report or args.source / 'evidence/quiet-runtime19120.json'
    args.cached_report = args.cached_report or args.source / 'evidence/cached-restore19120.json'
    original_qa, build, quiet, cached, bundle, application = p.inspect_inputs(args)
    states = {}
    for branch, commit in (('main', args.reviewed_main), ('dev', args.reviewed_dev)):
        def raw(name):
            return p.fetch(f'https://raw.githubusercontent.com/{p.REPO}/{commit}/{name}')
        feed = json.loads(raw('patches-bundle.json'))
        policy = raw(p.POLICY_PATH)
        p.require(feed['version'] == p.PREVIOUS and feed['download_url'] == p.BASE_URL, 'Reviewed feed is not .325')
        old_policy = json.loads(policy)
        p.require(old_policy['ulike_version'] == p.PREVIOUS_APP and old_policy['source_sha256'] == p.SINGLE_BASE_SHA256,
                  'Reviewed ULike policy is not .119')
        states[branch] = {'branch': branch, 'head': commit, 'manifest': feed, 'ulike_policy': policy,
                          'log': raw('CHANGELOG.md').decode()}
    p.require(states['main']['manifest'] == states['dev']['manifest'] and
              states['main']['ulike_policy'] == states['dev']['ulike_policy'], 'Main/dev feeds or policies differ')
    artifact_mpps = {name: p.file_row(args.dist / name) for name in (p.SINGLE, p.COMBINED)}
    qa = {**original_qa, 'publication': {
        'schema': 'ulike19120-published-bundle-qa-v1', 'ulike_version': p.VERSION,
        'bundle_version': p.BUNDLE, 'baseline_bundle_version': p.PREVIOUS,
        'baseline_bundle_sha256': p.BASE_SHA256, 'artifacts': artifact_mpps,
        'bundle_report': bundle, 'combined_application': application,
        'single_original_apks_apply_tested': True, 'combined_original_apks_apply_tested': True,
        'device_tested': False, 'device_performance_measured': False,
        'bundle_report_phase_note': 'Bundle-only proof precedes the separate original-APKS application report.'}}
    (args.dist / p.QA_NAME).write_bytes(p.json_bytes(qa))
    notes = ('ULike v1.9.120 / Hiro Morphe Patches v1.0.326\n'
        '統合版1.0.325からULikeだけを更新。ほかのアプリのパッチ・資産を保持しています。\n'
        'Morphe ManagerでHiro Morphe Patchesを1.0.326へ更新し、元のULike 5.6.2 (740)に適用してください。\n'
        '単独版と統合版はどちらか一方を使用します。\n\n' +
        (args.source / 'RELEASE_NOTES_v1.9.120.txt').read_text())
    (args.dist / 'RELEASE_NOTES.txt').write_text(notes)
    sums = ''.join(p.sha((args.dist / n).read_bytes()) + '  ' + n + '\n'
                   for n in sorted(p.ASSETS) if n != 'SHA256SUMS.txt')
    (args.dist / 'SHA256SUMS.txt').write_text(sums)
    frozen = args.repo / p.RELEASE_ROOT / 'publication/frozen'
    frozen.mkdir(parents=True, exist_ok=True)
    for name in (p.QA_NAME, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'):
        shutil.copyfile(args.dist / name, frozen / name)
    shutil.copyfile(args.bundle_report, frozen / 'bundle-report19120.json')
    shutil.copyfile(args.combined_application, frozen / 'combined-application19120.json')
    cfg = {'schema': 'ulike19120-publication-v1', 'ready': True,
        'publication_authorized_by_user': True, 'version': p.VERSION, 'bundle_version': p.BUNDLE,
        'previous_bundle': p.PREVIOUS, 'reviewed_main': args.reviewed_main, 'reviewed_dev': args.reviewed_dev,
        'reviewed_policy_sha256': p.sha(states['main']['ulike_policy']),
        'artifacts': {n: p.file_row(args.dist / n) for n in p.ASSETS},
        'bundle_report_sha256': p.file_row(args.bundle_report)['sha256'],
        'combined_application_sha256': p.file_row(args.combined_application)['sha256'],
        'source_files': p.source_files(args.repo)}
    args.manifest = args.repo / p.RELEASE_ROOT / p.CONFIG
    args.manifest.write_bytes(p.json_bytes(cfg))
    p.verify(args)
    created = p.utc_created(states)
    for state in states.values():
        p.metadata(state, 'https://example.invalid/freeze-only', created, notes)
    print(json.dumps({'status': 'frozen_no_remote_writes', 'manifest': str(args.manifest),
                      'artifacts': cfg['artifacts']}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
