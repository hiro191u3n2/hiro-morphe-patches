#!/usr/bin/env python3
"""Recover only the receipt after Back125's verified feed update hit CDN lag.
Uses the original real CI artifact; never rebuilds or replaces assets or feeds.
"""
from pathlib import Path
from types import SimpleNamespace
import hashlib, importlib.util, json, os, re, time

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL_RUN = 39233695544
ORIGINAL_JOB = 117997529717
ARTIFACT_ID = 12177854393
ARTIFACT_SHA = 'c8f624cadf3cb398c533237d54e9c669303bd3c84a2f3de132fabcf091aa79e9'
ORIGINAL_SOURCE = '988a07e9bccf90feba838e70179c7f639a27e6b1'
PUBLISHED = {'main': 'b2ca5db8dd4841d12ff26f9727346f796e9d365e',
             'dev': '9dee6ad919ce3a05d85a58ee00611d99a3d2458a'}
BEFORE = {'main': ORIGINAL_SOURCE, 'dev': '7e55ffd0a2791eca8e7013b6b8976315378706b9'}
RAW_COMMIT = '2f13e76d9a2087b1ee7efa081656981300520d1f'
RECEIPT_SHA = '6e40b834605b80bfe39b1150cb6df6c593f12fd9cc93bbd4c615ffeb656770ee'
LOG_SHA = '236bf3b7580b4ca2ab7985c29fa9ae62342ec8b6ee2160e8d2843cd68a00e213'
ERROR = 'RuntimeError: Public branch metadata did not propagate: main'
RECOVERY_FILES = {'.github/workflows/homeback125-reverify.yml', 'tools/homeback125_reverify.py'}
publisher = ROOT / 'releases/homeback125/publication/publish.py'
assert hashlib.sha256(publisher.read_bytes()).hexdigest() == '19aba18aa50edce38c7a219057ebc4788197ff7dba5e4409bdf683defad2194f', 'Published verifier changed'
spec = importlib.util.spec_from_file_location('homeback125_publish', publisher)
p = importlib.util.module_from_spec(spec); spec.loader.exec_module(p)
p.require(os.environ.get('GITHUB_ACTIONS') == 'true', 'Recovery requires a genuine GitHub Actions run')
recovery_run = os.environ.get('GITHUB_RUN_ID', '')
recovery_source = os.environ.get('GITHUB_SHA', '')
p.require(recovery_run.isdigit() and int(recovery_run) != ORIGINAL_RUN
          and re.fullmatch(r'[0-9a-f]{40}', recovery_source), 'Recovery run/source missing')
p.require((p.VERSION, p.BUNDLE, p.PREVIOUS) == ('1.1.15', '1.0.259', '1.0.258'), 'Wrong release verifier')
run = p.api(f'repos/{p.REPO}/actions/runs/{ORIGINAL_RUN}')
p.require(run['head_sha'] == ORIGINAL_SOURCE and run['head_branch'] == 'main'
          and run['path'] == '.github/workflows/homeback125-publish.yml'
          and run['status'] == 'completed' and run['conclusion'] == 'failure'
          and run['run_attempt'] == 1, 'Unexpected original CI run')
job = p.api(f'repos/{p.REPO}/actions/jobs/{ORIGINAL_JOB}')
p.require(job['run_id'] == ORIGINAL_RUN and job['conclusion'] == 'failure', 'Unexpected original job')
steps = {s['number']: s for s in job['steps']}
for number, name in {6: 'First fresh SDK build with host DEX and synthetic full-patcher gates',
                     7: 'Second fresh SDK build and repeat every gate',
                     8: 'Require exact local and two clean CI build bytes then package'}.items():
    p.require(steps[number]['name'] == name and steps[number]['conclusion'] == 'success', 'Original build gate failed')
p.require(steps[9]['name'] == 'Publish verified release and atomically update main and dev'
          and steps[9]['conclusion'] == 'failure' and steps[10]['conclusion'] == 'success', 'Unexpected failed step/artifact upload')
artifact = p.api(f'repos/{p.REPO}/actions/artifacts/{ARTIFACT_ID}')
p.require(artifact['name'] == 'homeback125-bundle259-verification' and not artifact['expired']
          and artifact['digest'] == 'sha256:' + ARTIFACT_SHA
          and artifact['workflow_run']['id'] == ORIGINAL_RUN
          and artifact['workflow_run']['head_sha'] == ORIGINAL_SOURCE, 'Unreviewed CI artifact')
recovery = p.api(f'repos/{p.REPO}/actions/runs/{recovery_run}')
p.require(recovery['head_sha'] == recovery_source and recovery['path'] == '.github/workflows/homeback125-reverify.yml'
          and recovery['event'] == 'push' and recovery['head_branch'] == 'main', 'Recovery is not this workflow')
change = p.api(f'repos/{p.REPO}/commits/{recovery_source}')
p.require([row['sha'] for row in change['parents']] == [PUBLISHED['main']]
          and {row['filename'] for row in change['files']} == RECOVERY_FILES, 'Recovery source changes unrelated files')
p.require(p.git(ROOT, 'rev-parse', 'HEAD') == recovery_source, 'Checkout differs from real recovery source')

staged = ROOT / '.ci-homeback125-reverify'
first, second = staged / 'dist-a', staged / 'dist-b'
receipt_path = first / p.RECEIPT_NAME
receipt_raw = receipt_path.read_bytes()
p.require(p.sha(receipt_raw) == RECEIPT_SHA and p.sha((staged / 'publication.log').read_bytes()) == LOG_SHA,
          'Original failure evidence changed')
receipt = json.loads(receipt_raw)
p.require(receipt['status'] == 'failed_requires_inspection' and receipt['error'] == ERROR
          and receipt['operations'] == [] and receipt['source_commit'] == ORIGINAL_SOURCE
          and receipt['release_id'] == 417138805 and receipt['raw_commit'] == RAW_COMMIT, 'Unexpected recovery stage')
base = staged / 'baseline258.mpp'; base.write_bytes(p.fetch(p.BASE_URL))
args = SimpleNamespace(config=ROOT / 'releases/homeback125/publication/manifest.json', base=base, dist=first)
cfg, payloads, notes, assets, report = p.validate_local(args)
p.require(cfg['ci_build_pair_verified'] is True and cfg['ci_run_id'] == str(ORIGINAL_RUN)
          and cfg['ci_source_commit'] == ORIGINAL_SOURCE
          and p.sha(args.config.read_bytes()) == receipt['publication_manifest_sha256'], 'Original CI manifest differs')
p.require(receipt['assets'] == assets and receipt['local_validation'] == report, 'Original publication evidence differs')
for name in (p.SINGLE, p.COMBINED, p.QA_NAME):
    p.require((first / name).read_bytes() == (second / name).read_bytes(), 'Two original CI builds differ: ' + name)
p.verify_assets(receipt['release_id'], assets, True)
p.require(p.head(p.RAW_BRANCH) == RAW_COMMIT, 'Immutable distribution branch moved')
expected_urls = {name: f'https://raw.githubusercontent.com/{p.REPO}/{RAW_COMMIT}/downloads/{name}' for name in (p.SINGLE, p.COMBINED)}
p.require(receipt['download_urls'] == expected_urls, 'Immutable URLs differ')
for name, url in expected_urls.items():
    p.require(p.fetch(url) == payloads[name], 'Immutable MPP differs: ' + name)
preflight = json.loads((staged / 'preflight.log').read_text())
p.require(preflight['status'] == 'preflight_passed_no_remote_writes' and preflight['heads'] == BEFORE,
          'Original preflight heads differ')
states = {branch: p.snapshot(branch) for branch in ('main', 'dev')}
p.require(states['main']['head'] == recovery_source and states['dev']['head'] == PUBLISHED['dev'], 'Concurrent branch update')
p.require(states['main']['manifest'] == states['dev']['manifest'], 'Manager feeds differ')
operations, retained = [], {}
for branch, state in states.items():
    original_commit = p.api(f'repos/{p.REPO}/git/commits/{PUBLISHED[branch]}')
    p.require([row['sha'] for row in original_commit['parents']] == [BEFORE[branch]], 'Published feed parent differs')
    baseline = {'manifest': json.loads(p.content('patches-bundle.json', BEFORE[branch])),
                'log': p.content('CHANGELOG.md', BEFORE[branch]).decode()}
    expected_feed, expected_log = p.metadata(baseline, cfg, expected_urls[p.COMBINED], state['manifest']['created_at'], notes)
    p.require(state['manifest'] == expected_feed and state['log'] == expected_log, 'Reviewed metadata differs: ' + branch)
    p.require(state['ulike_policy'] == p.content(p.POLICY_PATH, BEFORE[branch]), 'ULike policy changed: ' + branch)
    retained[branch] = {path: p.content(path, state['head']) for path in ('patches-bundle.json', 'CHANGELOG.md', p.POLICY_PATH)}
    for attempt in range(18):
        prefix = f'https://raw.githubusercontent.com/{p.REPO}/{branch}'
        nonce = str(time.time_ns())
        feed = json.loads(p.fetch(prefix + '/patches-bundle.json?homeback125_reverify=' + nonce))
        log = p.fetch(prefix + '/CHANGELOG.md?homeback125_reverify=' + nonce).decode()
        if feed == expected_feed and log == expected_log: break
        p.check_heads(states)
        print('Waiting for public metadata propagation', branch, attempt + 1, flush=True)
        if attempt < 17: time.sleep(5)
    else: raise RuntimeError('Public metadata still differs; no receipt written: ' + branch)
    p.require(p.fetch(expected_feed['download_url']) == payloads[p.COMBINED], 'Manager download differs: ' + branch)
    operations.append({'branch': branch, 'commit': state['head'], 'feed_verified': True, 'changelog_verified': True,
                       'ulike_policy_unchanged': True, 'manager_download_verified': True})
    print('PASS existing public and canonical metadata', branch, flush=True)
p.check_heads(states)
p.verify_assets(receipt['release_id'], assets, True)
receipt['recovered_from_error'] = receipt.pop('error')
receipt.update(status='published_and_verified', published=True, release_url=p.PAGE, operations=operations,
               manager_main_dev_updated_atomically=True, manager_app_scopes_verified=list(p.APP_SCOPES),
               other_apps_false_updates=False, all_unrelated_patch_resources_preserved=True,
               fixed_chatgpt_site_updated=False, initial_publication_run=ORIGINAL_RUN,
               initial_publication_job=ORIGINAL_JOB, initial_publication_conclusion='failure',
               initial_publication_status='failed_requires_inspection', initial_failure_receipt_sha256=RECEIPT_SHA,
               initial_publication_log_sha256=LOG_SHA, original_artifact_id=ARTIFACT_ID,
               original_artifact_sha256=ARTIFACT_SHA, verification_recovery_run=recovery_run,
               verification_recovery_source=recovery_source, public_metadata_reverified=True,
               assets_replaced_during_recovery=False, feeds_changed_during_recovery=False,
               patches_rebuilt_during_recovery=False)
raw = p.json_bytes(receipt); receipt_path.write_bytes(raw)
receipt_file = p.RELEASE_ROOT + '/' + p.RECEIPT_NAME
saved = p.commit_feeds(ROOT, states, {branch: {receipt_file: raw} for branch in states},
                       'docs: recover verified HomeBack125 receipt after public CDN propagation')
for branch, state in saved.items():
    p.require(p.content(receipt_file, state['head']) == raw, 'Saved receipt mismatch')
    for path, previous in retained[branch].items():
        p.require(p.content(path, state['head']) == previous, 'Receipt save changed ' + path)
p.check_heads(saved)
print('PASS original failed CI retained; existing release verified; only receipt saved atomically on main/dev', flush=True)
