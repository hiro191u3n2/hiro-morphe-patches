#!/usr/bin/env python3
"""Guard the variable evidence schema and CI report boundaries before publishing."""
import copy
from pathlib import Path
import unittest
import publish19121 as p
import ci19121 as ci


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
        variables = {'work': '/tmp/ulike19121-ci-tests'}
        valid = {'name': {'command': ['python3', 'unused.py'], 'report': '{work}/host/result.json'}}
        self.assertEqual(ci.suite_paths(valid, variables)['name']['report'],
                         Path('/tmp/ulike19121-ci-tests/host/result.json'))
        for report in ('{work}/../source/result.json', '/tmp/elsewhere/result.json'):
            invalid = copy.deepcopy(valid); invalid['name']['report'] = report
            with self.subTest(report=report), self.assertRaises(RuntimeError):
                ci.suite_paths(invalid, variables)


if __name__ == '__main__':
    unittest.main()
