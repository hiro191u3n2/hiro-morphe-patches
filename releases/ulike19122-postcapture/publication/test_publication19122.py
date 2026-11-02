#!/usr/bin/env python3
"""Guard the variable evidence schema and CI report boundaries before publishing."""
import copy
from pathlib import Path
import unittest
from unittest.mock import patch
import publish19122 as p
import ci19122 as ci


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
        variables = {'work': '/tmp/ulike19122-ci-tests'}
        valid = {'name': {'command': ['python3', 'unused.py'], 'report': '{work}/host/result.json'}}
        self.assertEqual(ci.suite_paths(valid, variables)['name']['report'],
                         Path('/tmp/ulike19122-ci-tests/host/result.json'))
        for report in ('{work}/../source/result.json', '/tmp/elsewhere/result.json'):
            invalid = copy.deepcopy(valid); invalid['name']['report'] = report
            with self.subTest(report=report), self.assertRaises(RuntimeError):
                ci.suite_paths(invalid, variables)


class PublicationBoundaries(unittest.TestCase):
    def state(self):
        return {'manifest': {'version': p.PREVIOUS, 'download_url': p.BASE_URL,
                'created_at': '2026-11-01T00:00:00',
                'description': 'Existing introduction\nULike：v1.9.121（5.6.2／740）\nOther：v7.8.9\n'},
                'log': '# [1.0.327](https://example.invalid/old) (2026-11-01)\n\n'
                       '* **ULike:** Previous fix.\n\n'
                       '# [1.0.326](https://example.invalid/older) (2026-10-31)\n\n'
                       '* **Other:** Preserve earlier app history.\n'}

    def test_metadata_preserves_history_and_other_app_detection(self):
        state = self.state()
        original = copy.deepcopy(state)
        feed, history = p.metadata(state, 'https://example.invalid/new',
                                   '2026-11-02T00:00:00', 'Reviewed new notes')
        self.assertEqual(state, original)
        self.assertTrue(history.endswith(original['log']))
        self.assertTrue(feed['description'].endswith(
            original['manifest']['description'].replace('v1.9.121', 'v1.9.122')))
        self.assertEqual(feed['version'], '1.0.328')
        self.assertTrue(p.has_changes_for(history, '1.0.327', ['ULike']))
        self.assertFalse(p.has_changes_for(history, '1.0.328', ['ULike']))
        self.assertFalse(p.has_changes_for(history, '1.0.327', ['Other']))

    def test_advanced_feed_or_missing_inventory_rejected(self):
        for field, value in (('version', '1.0.328'), ('download_url', 'https://example.invalid/changed'),
                             ('description', 'ULike：v1.9.120（5.6.2／740）\n')):
            state = self.state(); state['manifest'][field] = value
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                p.metadata(state, 'https://example.invalid/new', '2026-11-02T00:00:00', 'notes')

    def test_head_race_rejected_before_writes(self):
        states = {name: {'branch': name, 'head': name + '-old'} for name in ('main', 'dev')}
        for changed in ('main', 'dev'):
            with self.subTest(branch=changed), patch.object(p, 'head',
                    side_effect=lambda branch: branch + ('-new' if branch == changed else '-old')):
                with self.assertRaises(RuntimeError):
                    p.check_heads(states)
        with patch.object(p, 'head', side_effect=lambda branch: branch + '-old'):
            p.check_heads(states)

    def static_assets(self):
        old = {'ulike/runtime.dex': b'old runtime',
               'app/hiro/ulike/patches/UlikeHqMaxPatch.class': b'old metadata',
               'app/hiro/ulike/patches/IntegrationPayload186.class': b'unchanged installer',
               'ulike/methods.dex': b'stock methods', 'ulike/methods.tsv': b'stock table'}
        for index in range(14):
            old['ulike/runtime/native-' + str(index) + '.so'] = b'\x7fELF' + bytes([index])
        return old

    def test_native_and_installer_contracts_checked_from_bytes(self):
        before = self.static_assets(); after = dict(before)
        after['ulike/runtime.dex'] = b'new runtime'
        after['app/hiro/ulike/patches/UlikeHqMaxPatch.class'] = b'new metadata'
        p.verify_static_ulike_assets(before, after)
        for name in before:
            if name in ('ulike/runtime.dex', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'):
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


if __name__ == '__main__':
    unittest.main()
