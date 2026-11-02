#!/usr/bin/env python3
"""Guard the variable evidence schema and CI report boundaries before publishing."""
import copy
from pathlib import Path
import unittest
from unittest.mock import patch
import publish19123 as p
import ci19123 as ci


class EvidenceGuards(unittest.TestCase):
    def evidence(self, count=17, negatives=0):
        return {'status': 'passed', 'assertions': count, 'negative_controls': negatives,
                'source_sha256': {'Actual.java': 'a' * 64}, 'physical_android_tested': False}

    def test_variable_counts_and_known_evidence_forms(self):
        for count in (17, 245, 1001):
            for raw, normalized in ((2, 2), ({'failure_cleanup': True}, 1),
                    ([{'name': 'old_path', 'status': 'rejected'}], 1),
                    ([{'mutation': 'polling', 'detected': True}], 1)):
                with self.subTest(count=count, form=raw):
                    evidence = self.evidence(count, raw)
                    summary = p.host_summary(evidence)
                    self.assertEqual(summary['assertions'], count)
                    self.assertEqual(summary['negative_controls'], normalized)
                    self.assertEqual(summary['negative_control_evidence'], raw)
                    self.assertEqual(summary['source_sha256'], evidence['source_sha256'])

    def test_failed_controls_and_false_device_claims_are_rejected(self):
        mutations = [
            {'status': 'failed'}, {'assertions': True}, {'assertions': 0},
            {'negative_controls': -1}, {'negative_controls': {'cleanup': False}},
            {'negative_controls': [{'status': 'accepted'}]},
            {'negative_controls': [{'detected': False}]},
            {'physical_android_tested': True}, {'device_speedup_verified': True},
        ]
        for change in mutations:
            with self.subTest(change=change), self.assertRaises(RuntimeError):
                p.host_summary({**self.evidence(), **change})

    def test_named_suites_cannot_disappear_or_duplicate(self):
        self.assertEqual(set(p.parse_host_specs(['one=/tmp/one.json', 'two=/tmp/two.json'])), {'one', 'two'})
        for values in ([], ['one=a', 'one=b'], ['=foo'], ['../../outside=foo']):
            with self.subTest(values=values), self.assertRaises(RuntimeError):
                p.parse_host_specs(values)

    def test_report_paths_cannot_escape_ci_work(self):
        variables = {'work': '/tmp/ulike19123-ci-tests'}
        valid = {'name': {'command': ['python3', 'unused.py'], 'report': '{work}/host/result.json'}}
        self.assertEqual(ci.suite_paths(valid, variables)['name']['report'],
                         Path('/tmp/ulike19123-ci-tests/host/result.json'))
        for report in ('{work}/../source/result.json', '/tmp/elsewhere/result.json'):
            invalid = copy.deepcopy(valid); invalid['name']['report'] = report
            with self.subTest(report=report), self.assertRaises(RuntimeError):
                ci.suite_paths(invalid, variables)


class PublicationBoundaries(unittest.TestCase):
    def state(self):
        return {'manifest': {'version': p.PREVIOUS, 'download_url': p.BASE_URL,
                'created_at': '2026-11-01T00:00:00',
                'description': 'Existing introduction\nULike：v1.9.122（5.6.2／740）\nOther：v7.8.9\n'},
                'log': '# [1.0.332](https://example.invalid/old) (2026-11-01)\n\n'
                       '* **ULike:** Previous fix.\n\n'
                       '# [1.0.331](https://example.invalid/older) (2026-10-31)\n\n'
                       '* **Other:** Preserve earlier app history.\n'}

    def test_metadata_preserves_history_and_other_app_detection(self):
        state = self.state()
        original = copy.deepcopy(state)
        feed, history = p.metadata(state, 'https://example.invalid/new',
                                   '2026-11-03T00:00:00', 'Reviewed new notes')
        self.assertEqual(state, original)
        self.assertTrue(history.endswith(original['log']))
        self.assertTrue(feed['description'].endswith(
            original['manifest']['description'].replace('v1.9.122', 'v1.9.123')))
        self.assertEqual(feed['version'], '1.0.333')
        self.assertTrue(p.has_changes_for(history, '1.0.332', ['ULike']))
        self.assertFalse(p.has_changes_for(history, '1.0.333', ['ULike']))
        self.assertFalse(p.has_changes_for(history, '1.0.332', ['Other']))

    def test_advanced_feed_or_missing_inventory_rejected(self):
        for field, value in (('version', '1.0.333'), ('download_url', 'https://example.invalid/changed'),
                             ('description', 'ULike：v1.9.120（5.6.2／740）\n')):
            state = self.state(); state['manifest'][field] = value
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                p.metadata(state, 'https://example.invalid/new', '2026-11-03T00:00:00', 'notes')

    def test_head_race_rejected_before_writes(self):
        states = {name: {'branch': name, 'head': name + '-old'} for name in ('main', 'dev')}
        for changed in ('main', 'dev'):
            with self.subTest(branch=changed), patch.object(p, 'head',
                    side_effect=lambda branch: branch + ('-new' if branch == changed else '-old')):
                with self.assertRaises(RuntimeError):
                    p.check_heads(states)
        with patch.object(p, 'head', side_effect=lambda branch: branch + '-old'):
            p.check_heads(states)

    def test_feed_commits_use_one_atomic_push_for_both_heads(self):
        states = {b: {'branch': b, 'head': b + '-old'} for b in ('main', 'dev')}
        updates = {b: {'patches-bundle.json': b'new'} for b in states}
        def prepared(repo, state, files, message):
            self.assertEqual(files, updates[state['branch']])
            return {**state, 'head': state['branch'] + '-new'}
        with patch.object(p, 'check_heads') as heads, patch.object(p, 'prepare_commit', side_effect=prepared), \
             patch.object(p, 'git', return_value='') as git, patch.object(p, 'head', side_effect=lambda b: b + '-new'):
            p.commit_feeds_atomically(Path('/unused'), states, updates, 'reviewed')
            self.assertEqual(heads.call_count, 2)
            pushes = [call.args for call in git.call_args_list if 'push' in call.args]
            self.assertEqual(len(pushes), 1)
            self.assertIn('--atomic', pushes[0])
            self.assertIn('main-new:refs/heads/main', pushes[0])
            self.assertIn('dev-new:refs/heads/dev', pushes[0])
        with patch.object(p, 'check_heads', side_effect=[None, RuntimeError('Concurrent update')]), \
             patch.object(p, 'prepare_commit', side_effect=prepared), patch.object(p, 'git', return_value='') as git:
            with self.assertRaises(RuntimeError):
                p.commit_feeds_atomically(Path('/unused'), states, updates, 'reviewed')
            self.assertFalse(any('push' in call.args for call in git.call_args_list))
        with self.assertRaises(RuntimeError):
            p.commit_feeds_atomically(Path('/unused'), {'main': states['main']}, updates, 'reviewed')

    def test_applied_boost_proof_cannot_be_missing_or_bound_to_another_apk(self):
        report = {'status': 'passed', 'mpp': {'sha256': 'a' * 64}, 'runtime_version': p.VERSION,
                  'package_name': 'com.gorgeous.liteinternational', 'original_apk_apply_tested': True,
                  'full_dex_compilation_tested': True, 'physical_android_tested': False,
                  'signing_or_installation_tested': False, 'expected_runtime_contracts': 6400,
                  'all_apk_linkage_count': 1, 'output_apk': {'sha256': 'b' * 64},
                  'stock_boost_applied': {'status': 'passed', 'apk_sha256': 'b' * 64,
                    'one_callsite_suppressed': True, 'vendor_boost_manager_preserved': True,
                    'actual_shutter_inverse_matches_122': True, 'original_input_contract_preserved': True}}
        p.verify_application(report, 'a' * 64, 'test')
        for field, value in [('status', 'failed'), ('apk_sha256', 'c' * 64),
                             ('one_callsite_suppressed', False), ('vendor_boost_manager_preserved', False),
                             ('actual_shutter_inverse_matches_122', False), ('original_input_contract_preserved', False)]:
            bad = copy.deepcopy(report); bad['stock_boost_applied'][field] = value
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                p.verify_application(bad, 'a' * 64, 'test')
        bad = copy.deepcopy(report); del bad['stock_boost_applied']
        with self.assertRaises(RuntimeError):
            p.verify_application(bad, 'a' * 64, 'test')

    def static_assets(self):
        old = {'ulike/runtime.dex': b'old runtime',
               'app/hiro/ulike/patches/UlikeHqMaxPatch.class': b'old metadata',
               'app/hiro/ulike/patches/IntegrationPayload186.class': b'unchanged installer',
               'ulike/methods.dex': b'stock methods', 'ulike/methods.tsv': (
                   'Li/o/a/b1/a/g/y;->X(IZ)Z\t' + 'a' * 64 + '\t' + 'b' * 64 + '\n').encode()}
        for index in range(14):
            old['ulike/runtime/native-' + str(index) + '.so'] = b'\x7fELF' + bytes([index])
        return old

    def test_native_and_installer_contracts_checked_from_bytes(self):
        before = self.static_assets(); after = dict(before)
        after['ulike/runtime.dex'] = b'new runtime'
        after['ulike/methods.dex'] = b'new stock methods'
        after['ulike/methods.tsv'] = before['ulike/methods.tsv'].replace(b'b' * 64, b'c' * 64)
        after['app/hiro/ulike/patches/UlikeHqMaxPatch.class'] = b'new metadata'
        p.verify_static_ulike_assets(before, after)
        for name in before:
            if name in ('ulike/runtime.dex', 'ulike/methods.dex', 'ulike/methods.tsv', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'):
                continue
            mutation = dict(after); mutation[name] += b'mutated'
            with self.subTest(path=name), self.assertRaises(RuntimeError):
                p.verify_static_ulike_assets(before, mutation)
        mutation = dict(after); mutation['ulike/runtime/new.so'] = b'\x7fELFnew'
        with self.assertRaises(RuntimeError):
            p.verify_static_ulike_assets(before, mutation)
        mutation = dict(after); mutation.pop('ulike/methods.tsv')
        with self.assertRaises(RuntimeError):
            p.verify_static_ulike_assets(before, mutation)

    def test_stock_table_only_target_output_hash_may_change(self):
        before = self.static_assets()['ulike/methods.tsv']
        good = before.replace(b'b' * 64, b'c' * 64)
        p.verify_stock_table(before, good)
        bad_cases = [before, good.replace(b'a' * 64, b'd' * 64),
                     good.replace(b'X(IZ)Z', b'Y(IZ)Z'), good + good,
                     good.replace(b'c' * 64, b'bad'), b'']
        for bad in bad_cases:
            with self.subTest(bad=bad), self.assertRaises(RuntimeError):
                p.verify_stock_table(before, bad)
        second = b'Lsafe/Class;->same()V\t' + b'd' * 64 + b'\t' + b'e' * 64 + b'\n'
        p.verify_stock_table(before + second, good + second)
        with self.assertRaises(RuntimeError):
            p.verify_stock_table(before + second, good + second.replace(b'e' * 64, b'f' * 64))


if __name__ == '__main__':
    unittest.main()
