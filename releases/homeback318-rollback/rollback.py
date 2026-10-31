#!/usr/bin/env python3
"""Restore the exact requested318 distribution without rebuilding any patches."""
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
HELPER = ROOT / 'releases/homeback136/publication/publish.py'
assert hashlib.sha256(HELPER.read_bytes()).hexdigest() == 'd63591534b7c62c61f45c0b13ef4bba2851851887894b441cd6c8de8bd7c82a3'
spec = importlib.util.spec_from_file_location('publication', HELPER)
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)
REVIEWED_MAIN = '44a1abc7ac74f9880ac12d246c848ba2a6d23cef'
REVIEWED_DEV = 'afc1372a83114583c11ae03127602454a6411b3c'
PRIOR_MAIN = '16a5eb430aec2d3f14ab1717ac41c0e395a2335a'
PRIOR_DEV = 'e2c1a7e70a6f489371817d1669048443fb7f8458'
URL318 = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/9cb5e46a2df3d9eb3a4ad8700f0be22b4a694ecd/downloads/Hiro_Morphe_Patches_v1.0.318.mpp'
SHA318 = '170367733645b3ae3391a5d58f9426d7c43c01e02b6091b8c5cfc6948d1309af'
URL319 = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/96290380b0e265351dc2e47572c23dbce644a667/downloads/Hiro_Morphe_Patches_v1.0.319.mpp'


def main():
    p.require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Run via reviewed GitHub Actions')
    source = os.environ['GITHUB_SHA']
    p.require(p.git(ROOT, 'rev-parse', 'HEAD') == source, 'Wrong checkout')
    p.require(p.git(ROOT, 'show', '-s', '--format=%P', source) == REVIEWED_MAIN, 'Main lease differs')
    states = {b: p.snapshot(b) for b in ('main', 'dev')}
    p.require(states['main']['head'] == source and states['dev']['head'] == REVIEWED_DEV, 'Branches advanced')
    p.require(states['main']['feed'] == states['dev']['feed'], 'Feeds differ')
    for state in states.values():
        p.require(state['feed']['version'] == '1.0.319' and state['feed']['download_url'] == URL319, 'Unexpected active bundle')
    prior = p.content('patches-bundle.json', PRIOR_MAIN)
    p.require(prior == p.content('patches-bundle.json', PRIOR_DEV), 'Previous feeds differ')
    p.require(hashlib.sha1(b'blob ' + str(len(prior)).encode() + b'\0' + prior).hexdigest() == 'e276111958d6f95891cbef1bf9a3d279884c005f', 'Previous feed blob differs')
    feed = json.loads(prior)
    p.require(feed['version'] == '1.0.318' and feed['download_url'] == URL318 and feed['page_url'].endswith('/oneback-v1.1.27'), 'Previous feed identity differs')
    raw = p.fetch(URL318)
    p.require(len(raw) == 48032024 and p.sha(raw) == SHA318, 'Public318 bytes differ')
    previous_release = p.api(f'repos/{p.REPO}/releases/tags/oneback-v1.1.27')
    p.require(previous_release['draft'] is False, 'Previous release is not public')
    asset = [a for a in previous_release['assets'] if a['name'] == 'Hiro_Morphe_Patches_v1.0.318.mpp']
    p.require(len(asset) == 1 and asset[0]['size'] == 48032024 and asset[0]['digest'] == 'sha256:' + SHA318, 'Previous release asset differs')
    historical = {tag: p.api(f'repos/{p.REPO}/releases/tags/{tag}')
                  for tag in ('oneback-v1.1.28', 'oneback-v1.1.29')}
    for tag, release in historical.items():
        p.require(release['tag_name'] == tag and release['draft'] is False, 'Historical release identity differs')
    when = dt.datetime.now(dt.timezone.utc).isoformat()
    notice = ('# 指定バージョン318へ戻しました（2026-10-31）\n\n'
              'ユーザー指定により、Managerのmain/dev配布先を'
              'Hiro Morphe Patches v1.0.318 / OneBack v1.1.27へ戻しました。'
              '公開済み318と同一のMPPです。新しい修正は含みません。\n\n'
              '319または320で作成したInstagramは、318で元APKを再パッチしてインストールしてください。'
              'Managerに319または320が残る場合は318のMPPを直接読み込んでください。\n\n')
    receipt = {'schema': 'oneback318-restore-v1', 'requested_action': 'restore_requested_version',
               'created_at': when, 'source_commit': source, 'from_bundle': '1.0.319',
               'to_bundle': '1.0.318', 'to_oneback': '1.1.27', 'download_url': URL318,
               'bytes': len(raw), 'sha256': SHA318, 'rebuilt': False,
               'restored_feed_blob': 'e276111958d6f95891cbef1bf9a3d279884c005f',
               'reviewed_main': REVIEWED_MAIN, 'reviewed_dev': REVIEWED_DEV,
               'user_request': 'v1.0.318（OneBack v1.1.27）へ戻す', 'physical_android_tested': False}
    files = {b: {'patches-bundle.json': prior,
                 'CHANGELOG.md': notice.encode() + s['log'],
                 'releases/homeback318-rollback/receipt.json': p.json_bytes(receipt)}
             for b, s in states.items()}
    p.check_heads(states)
    active = p.commit_feeds(ROOT, states, files, 'revert: restore exact bundle318 requested by user')
    for b, state in active.items():
        p.require(p.content('patches-bundle.json', state['head']) == prior, 'Restored feed differs')
        p.require(p.content(p.POLICY_PATH, state['head']) == states[b]['policy'], 'Unrelated policy changed')
        p.require(p.content('releases/homeback318-rollback/receipt.json', state['head']) == p.json_bytes(receipt), 'Receipt differs')
        for attempt in range(10):
            public = p.fetch(f'https://raw.githubusercontent.com/{p.REPO}/{b}/patches-bundle.json?verify={p.time.time_ns()}')
            if public == prior:
                break
            p.check_heads(active)
            p.time.sleep(3)
        else:
            raise RuntimeError('Feed propagation not confirmed: ' + b)
    p.require(p.sha(p.fetch(URL318)) == SHA318, 'Final318 bytes differ')
    prefix = ('**現在のManager配布：ユーザー指定により318 / OneBack v1.1.27へ戻しました。**\n\n'
              '[318はこちら](https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/oneback-v1.1.27)。'
              '318で元APKを再パッチ・インストールしてください。以下は過去の公開記録です。\n\n---\n\n')
    for tag, release in historical.items():
        edited = p.api(f"repos/{p.REPO}/releases/{release['id']}",
                       {'body': prefix + release['body'], 'make_latest': 'false'}, 'PATCH')
        p.require(edited['body'].startswith(prefix) and edited['name'] == release['name'], 'Distribution notice differs')
        p.require([(a['id'], a['digest']) for a in edited['assets']] == [(a['id'], a['digest']) for a in release['assets']], 'Historical assets changed')
    p.check_heads(active)
    print(json.dumps({'status': 'rollback318_verified', 'feeds_updated_atomically': True,
                      'commits': {b: s['head'] for b, s in active.items()},
                      'sha256': SHA318, 'historical_releases_link_current318': True}, indent=2), flush=True)



if __name__ == '__main__':
    main()
