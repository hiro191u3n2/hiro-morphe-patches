#!/usr/bin/env python3
"""Freeze reviewed .123/.333 inputs without remote writes or invented test counts."""
from pathlib import Path
import argparse
import json
import re
import shutil
import publish19123 as p


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('repo', 'dist', 'baseline', 'source', 'bundle-report', 'combined-application'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--build-report', type=Path)
    parser.add_argument('--notes', type=Path)
    parser.add_argument('--host-report', action='append', default=[], metavar='NAME=PATH')
    parser.add_argument('--reviewed-main', required=True)
    parser.add_argument('--reviewed-dev', required=True)
    parser.add_argument('--authorized', action='store_true')
    args = parser.parse_args()
    args.repo, args.dist, args.source = args.repo.resolve(), args.dist.resolve(), args.source.resolve()
    p.require(args.authorized, 'User publication instruction required')
    p.require(all(re.fullmatch('[a-f0-9]{40}', value) for value in (args.reviewed_main, args.reviewed_dev)), 'Exact reviewed commits required')
    p.require(p.git(args.repo, 'rev-parse', 'HEAD') == args.reviewed_main, 'Freeze must use reviewed main checkout')
    args.build_report = args.build_report or args.source / 'evidence/build19123.json'
    args.host_reports = p.parse_host_specs(args.host_report)
    reports = p.inspect_inputs(args)
    suites = p.read_json(args.repo / p.RELEASE_ROOT / 'publication/ci-suites19123.json')
    p.require(set(suites) == set(reports['hosts']), 'CI suite plan differs from reviewed host evidence')
    states = {}
    for branch, commit in (('main', args.reviewed_main), ('dev', args.reviewed_dev)):
        def raw(name):
            return p.fetch(f'https://raw.githubusercontent.com/{p.REPO}/{commit}/{name}')
        feed = json.loads(raw('patches-bundle.json'))
        policy = raw(p.POLICY_PATH)
        p.require(feed['version'] == p.PREVIOUS and feed['download_url'] == p.BASE_URL, 'Reviewed feed is not .332')
        old_policy = json.loads(policy)
        p.require(old_policy['ulike_version'] == p.PREVIOUS_APP and old_policy['source_sha256'] == p.SINGLE_BASE_SHA256,
                  'Reviewed ULike policy is not .122')
        states[branch] = {'branch': branch, 'head': commit, 'manifest': feed, 'ulike_policy': policy,
                          'log': raw('CHANGELOG.md').decode()}
    p.require(states['main']['manifest'] == states['dev']['manifest'] and
              states['main']['ulike_policy'] == states['dev']['ulike_policy'], 'Main/dev feeds or policies differ')
    artifact_mpps = {name: p.file_row(args.dist / name) for name in (p.SINGLE, p.COMBINED)}
    qa = {**reports['original_qa'], 'publication': {
        'schema': 'ulike19123-published-bundle-qa-v1', 'ulike_version': p.VERSION,
        'bundle_version': p.BUNDLE, 'baseline_bundle_version': p.PREVIOUS,
        'baseline_bundle_sha256': p.BASE_SHA256, 'artifacts': artifact_mpps,
        'bundle_report': reports['bundle'], 'combined_application': reports['application'],
        'apk_reflection_abi': reports['abi'],
        'host_suites': reports['hosts'],
        'single_original_apks_apply_tested': True, 'combined_original_apks_apply_tested': True,
        'device_tested': False, 'device_performance_measured': False,
        'bundle_report_phase_note': 'Bundle-only proof precedes separate original-APKS application.'}}
    (args.dist / p.QA_NAME).write_bytes(p.json_bytes(qa))
    notes_path = args.notes or args.source / 'RELEASE_NOTES_v1.9.123.txt'
    notes_raw = notes_path.read_bytes()
    notes = notes_raw.decode()
    p.require('1.9.123' in notes and '1.0.333' in notes and '実機' in notes and ('未測定' in notes or '未確認' in notes),
              'Root-prepared notes must identify both versions and device limitation')
    (args.dist / 'RELEASE_NOTES.txt').write_bytes(notes_raw)
    sums = ''.join(p.sha((args.dist / n).read_bytes()) + '  ' + n + '\n'
                   for n in sorted(p.ASSETS) if n != 'SHA256SUMS.txt')
    (args.dist / 'SHA256SUMS.txt').write_text(sums)
    frozen = args.repo / p.RELEASE_ROOT / 'publication/frozen'
    (frozen / 'host').mkdir(parents=True, exist_ok=True)
    for name in (p.QA_NAME, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt'):
        shutil.copyfile(args.dist / name, frozen / name)
    shutil.copyfile(args.bundle_report, frozen / 'bundle-report19123.json')
    shutil.copyfile(args.combined_application, frozen / 'combined-application19123.json')
    for name, path in args.host_reports.items():
        shutil.copyfile(path, frozen / 'host' / (name + '.json'))
    cfg = {'schema': 'ulike19123-publication-v1', 'ready': True,
        'publication_authorized_by_user': True, 'version': p.VERSION, 'bundle_version': p.BUNDLE,
        'previous_bundle': p.PREVIOUS, 'reviewed_main': args.reviewed_main, 'reviewed_dev': args.reviewed_dev,
        'reviewed_policy_sha256': p.sha(states['main']['ulike_policy']),
        'artifacts': {n: p.file_row(args.dist / n) for n in p.ASSETS},
        'bundle_report_sha256': p.file_row(args.bundle_report)['sha256'],
        'combined_application_sha256': p.file_row(args.combined_application)['sha256'],
        'host_suites': reports['hosts'], 'source_files': p.source_files(args.repo)}
    args.manifest = args.repo / p.RELEASE_ROOT / p.CONFIG
    args.manifest.write_bytes(p.json_bytes(cfg))
    p.verify(args)
    created = p.utc_created(states)
    for state in states.values():
        p.metadata(state, 'https://example.invalid/freeze-only', created, notes)
    print(json.dumps({'status': 'frozen_no_remote_writes', 'manifest': str(args.manifest),
                      'artifacts': cfg['artifacts'], 'host_assertions': qa['host_assertions'],
                      'host_negative_controls': qa['host_negative_controls']}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
