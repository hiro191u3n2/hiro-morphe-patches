#!/usr/bin/env python3
"""Offline checks for the inspected recovery's race and identity boundaries."""
import copy
import importlib.util
from pathlib import Path
import unittest
from unittest import mock

spec = importlib.util.spec_from_file_location('resume', Path(__file__).with_name('resume_publication.py'))
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)


class RecoveryTests(unittest.TestCase):
    def test_prior_build_evidence_and_artifact_pins(self):
        run = {'id': r.RUN_ID, 'head_sha': r.SOURCE_COMMIT, 'head_branch': 'main',
               'event': 'push', 'status': 'completed', 'conclusion': 'failure'}
        job = {'id': r.JOB_ID, 'run_id': r.RUN_ID, 'head_sha': r.SOURCE_COMMIT, 'conclusion': 'failure',
               'steps': [{'number': k, 'name': v, 'conclusion': 'success'} for k, v in r.SUCCESS_STEPS.items()] +
               [{'number': 9, 'name': 'Publish verified MPPs and atomically advance both Manager feeds', 'conclusion': 'failure'}]}
        artifact = {'id': r.ARTIFACT_ID, 'name': r.ARTIFACT_NAME, 'expired': False,
                    'size_in_bytes': r.ARTIFACT_BYTES, 'digest': 'sha256:' + r.ARTIFACT_SHA256,
                    'workflow_run': {'id': r.RUN_ID, 'head_sha': r.SOURCE_COMMIT}}
        r.validate_prior_run(run, job, artifact)
        for group, key, value in [(run, 'head_sha', '0' * 40), (run, 'event', 'pull_request'),
                                  (artifact, 'digest', 'sha256:' + '0' * 64), (artifact, 'expired', True)]:
            original = group[key]
            group[key] = value
            with self.assertRaises(RuntimeError):
                r.validate_prior_run(run, job, artifact)
            group[key] = original
        for step in job['steps'][:4]:
            step['conclusion'] = 'failure'
            with self.assertRaises(RuntimeError):
                r.validate_prior_run(run, job, artifact)
            step['conclusion'] = 'success'

    def test_existing_release_identity(self):
        release = {'id': r.RELEASE_ID, 'tag_name': r.p.TAG, 'target_commitish': r.SOURCE_COMMIT,
                   'draft': False, 'prerelease': True}
        r.validate_release(release)
        for key, value in [('id', 1), ('draft', True), ('prerelease', False), ('target_commitish', 'main')]:
            bad = {**release, key: value}
            with self.assertRaises(RuntimeError):
                r.validate_release(bad)

    def test_advanced_branches_and_baselines_rejected(self):
        feed = {'version': r.p.PREVIOUS, 'download_url': r.p.BASE_URL}
        states = {'main': {'head': 'new-review', 'manifest': dict(feed)},
                  'dev': {'head': r.DEV_COMMIT, 'manifest': dict(feed)}}
        raw = {'branch': r.p.RAW_BRANCH, 'head': r.SOURCE_COMMIT}
        r.validate_states(states, raw, 'new-review')
        for branch in ('main', 'dev'):
            bad = copy.deepcopy(states)
            bad[branch]['head'] = 'concurrent'
            with self.assertRaises(RuntimeError):
                r.validate_states(bad, raw, 'new-review')
        for key, value in [('version', '1.0.315'), ('download_url', 'https://example.invalid/other')]:
            bad = copy.deepcopy(states)
            for state in bad.values():
                state['manifest'][key] = value
            with self.assertRaises(RuntimeError):
                r.validate_states(bad, raw, 'new-review')
        with self.assertRaises(RuntimeError):
            r.validate_states(states, {**raw, 'head': 'concurrent'}, 'new-review')

    def test_raw_transfer_uses_git_pack_with_exact_lease(self):
        helper = mock.Mock()
        initial = {'branch': r.p.RAW_BRANCH, 'head': r.SOURCE_COMMIT}
        prepared = {**initial, 'head': 'a' * 40}
        payloads = {r.p.SINGLE: b'standalone', r.p.COMBINED: b'bundle'}
        with mock.patch.object(r.p, 'prepare_commit', return_value=prepared) as prepare, \
                mock.patch.object(r.p, 'git', return_value='') as git:
            result = r.commit_raw(helper, Path('/unused'), initial, payloads)
        self.assertEqual(result, prepared)
        self.assertEqual(prepare.call_args.args[2], {'downloads/' + k: v for k, v in payloads.items()})
        pushes = [call.args for call in git.call_args_list if call.args[1] == 'push']
        self.assertEqual(len(pushes), 1)
        self.assertIn('--force-with-lease=refs/heads/' + r.p.RAW_BRANCH + ':' + r.SOURCE_COMMIT, pushes[0])
        self.assertIn(prepared['head'] + ':refs/heads/' + r.p.RAW_BRANCH, pushes[0])
        helper.api.assert_not_called()
        helper.commit_files.assert_not_called()
        self.assertEqual(helper.check_heads.call_count, 3)
        with self.assertRaises(RuntimeError):
            r.commit_raw(helper, Path('/unused'), {**initial, 'head': 'concurrent'}, payloads)


if __name__ == '__main__':
    unittest.main()
