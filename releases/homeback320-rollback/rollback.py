#!/usr/bin/env python3
"""Restore the exact previous319 distribution without rebuilding any patches."""
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
REVIEWED_MAIN = 'f62c8203787264c3094b23b623a4ad0a98ba278d'
REVIEWED_DEV = 'af488b0fe13b5bb06f0cd84975e876574fb8d43a'
PRIOR_MAIN = '14f00b372c33bdd2d17fa618d87b7976dd593b8f'
PRIOR_DEV = '76d42b3d90a41d7a22d4d0a5b08506a4f6bef773'
URL319 = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/96290380b0e265351dc2e47572c23dbce644a667/downloads/Hiro_Morphe_Patches_v1.0.319.mpp'
SHA319 = 'cd0f961cc549ee3da5e6222fd55dc46f80e760c05409318dde4903bdb5e17564'
URL320 = 'https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/d3c6a4f0a551b860e5113345a6a31ff1665ec2ae/downloads/Hiro_Morphe_Patches_v1.0.320.mpp'


def main():
    p.require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Run via reviewed GitHub Actions')
    source = os.environ['GITHUB_SHA']
    p.require(p.git(ROOT, 'rev-parse', 'HEAD') == source, 'Wrong checkout')
    p.require(p.git(ROOT, 'show', '-s', '--format=%P', source) == REVIEWED_MAIN, 'Main lease differs')
    states = {b: p.snapshot(b) for b in ('main', 'dev')}
    p.require(states['main']['head'] == source and states['dev']['head'] == REVIEWED_DEV, 'Branches advanced')
    p.require(states['main']['feed'] == states['dev']['feed'], 'Feeds differ')
    for state in states.values():
        p.require(state['feed']['version'] == '1.0.320' and state['feed']['download_url'] == URL320, 'Unexpected active bundle')
    prior = p.content('patches-bundle.json', PRIOR_MAIN)
    p.require(prior == p.content('patches-bundle.json', PRIOR_DEV), 'Previous feeds differ')
    p.require(hashlib.sha1(b'blob ' + str(len(prior)).encode() + b'\0' + prior).hexdigest() == '59808963d030a902de51539ca7a763e3eba9bac3', 'Previous feed blob differs')
    feed = json.loads(prior)
    p.require(feed['version'] == '1.0.319' and feed['download_url'] == URL319 and feed['page_url'].endswith('/oneback-v1.1.28'), 'Previous feed identity differs')
    raw = p.fetch(URL319)
    p.require(len(raw) == 48051087 and p.sha(raw) == SHA319, 'Public319 bytes differ')
    previous_release = p.api(f'repos/{p.REPO}/releases/tags/oneback-v1.1.28')
    p.require(previous_release['draft'] is False, 'Previous release is not public')
    asset = [a for a in previous_release['assets'] if a['name'] == 'Hiro_Morphe_Patches_v1.0.319.mpp']
    p.require(len(asset) == 1 and asset[0]['size'] == 48051087 and asset[0]['digest'] == 'sha256:' + SHA319, 'Previous release asset differs')
    withdrawn = p.api(f'repos/{p.REPO}/releases/tags/oneback-v1.1.29')
    p.require(withdrawn['id'] == 429729868 and withdrawn['draft'] is False, 'Unexpected320 release')
    when = dt.datetime.now(dt.timezone.utc).isoformat()
    notice = ('# 320を取り下げ、319へ戻しました（2026-10-31）\n\n'
              'ユーザーから挙動の悪化が報告されたため、Managerのmain/dev配布先を直前の'
              'Hiro Morphe Patches v1.0.319 / OneBack v1.1.28へ戻しました。'
              '公開済み319と同一のMPPです。初回と2回目の動作差を修正したという意味ではありません。\n\n'
              '320で作成したInstagramは、319で元APKを再パッチしてインストールしてください。'
              'Managerに320が残る場合は319のMPPを直接読み込んでください。\n\n')
    receipt = {'schema': 'oneback320-rollback-v1', 'requested_action': 'restore_previous_version',
               'created_at': when, 'source_commit': source, 'from_bundle': '1.0.320',
               'to_bundle': '1.0.319', 'to_oneback': '1.1.28', 'download_url': URL319,
               'bytes': len(raw), 'sha256': SHA319, 'rebuilt': False,
               'restored_feed_blob': '59808963d030a902de51539ca7a763e3eba9bac3',
               'reviewed_main': REVIEWED_MAIN, 'reviewed_dev': REVIEWED_DEV,
               'user_report': '320で挙動が悪化したため一つ前へ戻す', 'physical_android_tested': False}
    files = {b: {'patches-bundle.json': prior,
                 'CHANGELOG.md': notice.encode() + s['log'],
                 'releases/homeback320-rollback/receipt.json': p.json_bytes(receipt)}
             for b, s in states.items()}
    p.check_heads(states)
    active = p.commit_feeds(ROOT, states, files, 'revert: restore exact bundle319 after user-reported320 regression')
    for b, state in active.items():
        p.require(p.content('patches-bundle.json', state['head']) == prior, 'Restored feed differs')
        p.require(p.content(p.POLICY_PATH, state['head']) == states[b]['policy'], 'Unrelated policy changed')
        p.require(p.content('releases/homeback320-rollback/receipt.json', state['head']) == p.json_bytes(receipt), 'Receipt differs')
        for attempt in range(10):
            public = p.fetch(f'https://raw.githubusercontent.com/{p.REPO}/{b}/patches-bundle.json?verify={p.time.time_ns()}')
            if public == prior:
                break
            p.check_heads(active)
            p.time.sleep(3)
        else:
            raise RuntimeError('Feed propagation not confirmed: ' + b)
    p.require(p.sha(p.fetch(URL319)) == SHA319, 'Final319 bytes differ')
    prefix = ('**取り下げ済み：挙動悪化の報告により、配布を直前の319へ戻しました。**\n\n'
              '[319 / OneBack v1.1.28はこちら](https://github.com/hiro191u3n2/hiro-morphe-patches/releases/tag/oneback-v1.1.28)。'
              '319で元APKを再パッチ・インストールしてください。\n\n---\n\n')
    edited = p.api(f"repos/{p.REPO}/releases/{withdrawn['id']}",
                   {'name': '[Withdrawn / 取り下げ] OneBack v1.1.29 diagnostic / bundle320',
                    'body': prefix + withdrawn['body'], 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
    p.require(edited['body'].startswith(prefix) and edited['name'].startswith('[Withdrawn'), 'Withdrawal notice differs')
    p.require([(a['id'], a['digest']) for a in edited['assets']] == [(a['id'], a['digest']) for a in withdrawn['assets']], 'Historical assets changed')
    p.check_heads(active)
    print(json.dumps({'status': 'rollback319_verified', 'feeds_updated_atomically': True,
                      'commits': {b: s['head'] for b, s in active.items()},
                      'sha256': SHA319, 'release320_marked_withdrawn': True}, indent=2), flush=True)


if __name__ == '__main__':
    main()
