#!/usr/bin/env python3
"""Offline negative tests for publication gates; never contacts GitHub."""
from __future__ import annotations
import copy
import importlib.util
import io
import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest import mock
import zipfile
sys.dont_write_bytecode = True
SPEC = importlib.util.spec_from_file_location('publisher1824_under_test', Path(__file__).with_name('publish1824.py'))
p = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(p)


def put(data, dotted, value):
    parts = dotted.split('.')
    for part in parts[:-1]:
        data = data.setdefault(part, {})
    data[parts[-1]] = copy.deepcopy(value)


def expected_manifest():
    return {'schema': 'swiftkey1824-publication-v1', 'swiftkey_version': p.VERSION,
            'bundle_version': p.BUNDLE, 'baseline_bundle_version': p.PREVIOUS,
            'baseline_bundle_sha256': p.BASE_SHA256, 'baseline_swiftkey_version': p.SINGLE_PREVIOUS,
            'baseline_swiftkey_sha256': p.SINGLE_BASE_SHA256, 'change_plan_reviewed': True,
            'qa_contract_reviewed': True, 'android_device_tested': False, 'original_apk_apply_tested': False,
            'artifacts': {n: {'bytes': 1, 'sha256': '1' * 64} for n in p.ASSETS},
            'allowed_changed_bundle_entries': sorted(p.REQUIRED_CHANGED),
            'allowed_added_bundle_entries': [], 'allowed_changed_standalone_entries': sorted(p.REQUIRED_CHANGED),
            'allowed_added_standalone_entries': [], 'qa_required_values': copy.deepcopy(p.REQUIRED_QA),
            'source_files': {'tests/AuditDictionaryAbi.java': {'bytes': 1, 'sha256': '3' * 64}}}


def good_evidence(runtime='2' * 64):
    data = {}
    for key, value in p.REQUIRED_QA.items():
        put(data, key, value)
    data['host_report']['runtime_sha256'] = runtime
    data['abi_report'].update(runtime_sha256=runtime, verifier_source_sha256='3' * 64, report={
        'status': 'PASSED', 'extension_sha256': runtime, 'apk_sha256': p.REFERENCE_APK_SHA256,
        'unresolved': [], 'inaccessible': [], 'incompatible_opcodes': [], 'device_tested': False,
        'original_apk_apply_tested': False, 'audited_classes': ['Lhiro/swiftkey/' + n + ';' for n in
            ('WordCandidates', 'KatakanaEnglishCandidates', 'EmojiCandidates', 'FrequencyLearning')],
        'platform_members_checked': True, 'platform_inherited_members_resolved': True,
        'each_caller_access_checked': True, 'interface_callbacks_checked': True})
    row = {'status': 'PASSED', 'selected_extension_stream_verified': True,
           'selected_extension_sha256': runtime, 'baseline_extension_sha256': p.BASE_RUNTIME_SHA256,
           'baseline_updated_dependency_graph_equal': True, 'standalone_combined_dependency_graph_equal': True,
           'clean_apk_apply_tested': False, 'device_tested': False}
    data['loader_reports'] = [copy.deepcopy(row), copy.deepcopy(row)]
    return data


def runtime_bytes(marker):
    result = bytearray(b'dex\n035\0' + b'\0' * 120)
    result[8] = marker
    struct.pack_into('<I', result, 96, 91)
    return bytes(result)


def package(version, marker):
    return {p.MF: ('Manifest-Version: 1.0\r\nName: Hiro test\r\nVersion: ' + version +
                    '\r\nTimestamp: fixed\r\nDescription: dictionary fix\r\n\r\n').encode(),
            p.EXTENSION: runtime_bytes(marker), 'classes.dex': b'dex\n035\0loader',
            'swiftkey/words_ja.tsv': 'あ\t亜\n'.encode(), 'other/resource': b'preserve',
            **{n: b'\xca\xfe\xba\xbe' + n.encode() for n in p.JVM_CLASSES}}


def zipped(entries):
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w') as z:
        for name, raw in sorted(entries.items()):
            z.writestr(zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0)), raw)
    return output.getvalue()


class ManifestAndScopeTests(unittest.TestCase):
    def test_manifest_accepts_complete_contract(self):
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / 'manifest.json'; f.write_text(json.dumps(expected_manifest()))
            self.assertEqual(p.load_expected(f)['bundle_version'], p.BUNDLE)

    def test_manifest_rejects_missing_or_weakened_guards(self):
        changes = [lambda x: x.update(baseline_bundle_sha256='0' * 64),
                   lambda x: x.update(change_plan_reviewed=False),
                   lambda x: x.update(allowed_added_bundle_entries=['swiftkey/new.tsv']),
                   lambda x: x['qa_required_values'].pop('host_report.negative_control_verified'),
                   lambda x: x['qa_required_values'].update(device_tested=0),
                   lambda x: x['artifacts'].pop(p.QA_NAME)]
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / 'manifest.json'
            for change in changes:
                value = expected_manifest(); change(value); f.write_text(json.dumps(value))
                with self.subTest(change=change), self.assertRaises(RuntimeError): p.load_expected(f)

    def test_scope_and_unrelated_metadata_preserved(self):
        old = {'version': p.PREVIOUS, 'download_url': p.BASE_URL, 'created_at': '2026-10-28T00:00:00',
               'description': 'SwiftKey Beta：v1.8.23（9.13.16.4／1236271168）\nULike：v1.9.99\n',
               'unrelated_future_key': {'keep': [1, 2]}, 'signature_download_url': ''}
        state = {'manifest': old, 'log': '# 1.0.312 (2026-10-28)\n\n* **Instagram:** old\n'}
        feed, log = p.metadata(state, 'https://example.invalid/fixed.mpp', '2026-10-28T00:01:00', 'Notes')
        self.assertEqual(feed['unrelated_future_key'], old['unrelated_future_key'])
        self.assertIn('ULike：v1.9.99', feed['description'])
        self.assertIn('SwiftKey Beta：v1.8.24', feed['description'])
        self.assertTrue(log.endswith(state['log']))
        for app in p.APP_NAMES: self.assertTrue(p.has_changes_for(log, p.PREVIOUS, [app]))
        for app in p.OTHER_APPS: self.assertFalse(p.has_changes_for(log, p.PREVIOUS, [app]))
        self.assertFalse(p.has_changes_for(log, p.BUNDLE, list(p.APP_NAMES)))
        newer = copy.deepcopy(state); newer['manifest']['version'] = '1.0.314'
        with self.assertRaises(RuntimeError): p.metadata(newer, 'url', '2026-10-28T00:01:00', 'Notes')
        wrong = copy.deepcopy(state); wrong['manifest']['download_url'] = 'changed'
        with self.assertRaises(RuntimeError): p.metadata(wrong, 'url', '2026-10-28T00:01:00', 'Notes')

    def test_source_pin_rejects_extra_compiled_source(self):
        names = ['build1824.py', 'publish1824.py', 'publisher_tests.py', 'workflow.yml', 'tests/AuditDictionaryAbi.java']
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); pins = {}
            for name in names:
                f = root / name; f.parent.mkdir(parents=True, exist_ok=True); f.write_bytes(b'x')
                pins[name] = {'bytes': 1, 'sha256': p.sha(b'x')}
            expected = {'source_files': pins}
            self.assertEqual(p.verify_source_files(root, expected), 5)
            (root / 'injected.java').write_text('class Injected {}')
            with self.assertRaises(RuntimeError): p.verify_source_files(root, expected)


class EvidenceTests(unittest.TestCase):
    def test_complete_evidence(self):
        self.assertTrue(p.validate_evidence(good_evidence(), expected_manifest(), '2' * 64)['two_loader_sdks_bound'])

    def test_runtime_hash_and_native_errors_are_rejected(self):
        for path, value in [('host_report.runtime_sha256', '4' * 64),
                            ('host_report.word_coverage.all_exact_entries_reachable', False),
                            ('host_report.negative_control_verified', False),
                            ('abi_report.runtime_sha256', '4' * 64),
                            ('abi_report.verifier_source_sha256', '4' * 64),
                            ('abi_report.report.unresolved', ['missing method']),
                            ('abi_report.report.inaccessible', ['private method']),
                            ('abi_report.report.incompatible_opcodes', ['wrong opcode']),
                            ('abi_report.report.audited_classes', []),
                            ('abi_report.report.device_tested', True),
                            ('abi_report.replayed_in_ci', True)]:
            data = good_evidence(); put(data, path, value)
            with self.subTest(path=path), self.assertRaises(RuntimeError):
                p.validate_evidence(data, expected_manifest(), '2' * 64)

    def test_two_real_loader_results_required(self):
        for mode in ('one', 'different', 'wrong-bytes'):
            data = good_evidence()
            if mode == 'one': data['loader_reports'].pop()
            elif mode == 'different': data['loader_reports'][1]['status'] = 'FAILED'
            else:
                for row in data['loader_reports']: row['selected_extension_sha256'] = '4' * 64
            with self.subTest(mode=mode), self.assertRaises(RuntimeError):
                p.validate_evidence(data, expected_manifest(), '2' * 64)


class ArchiveAndPublicationTests(unittest.TestCase):
    def test_only_reviewed_archive_entries_change(self):
        before, after = package(p.PREVIOUS, 1), package(p.BUNDLE, 2)
        self.assertEqual(p.validate_archive_delta(before, after, p.PREVIOUS, p.BUNDLE, 'bundle'), sorted(p.REQUIRED_CHANGED))
        for entry in ['other/resource', 'swiftkey/words_ja.tsv', 'classes.dex', next(iter(p.JVM_CLASSES))]:
            altered = copy.deepcopy(after); altered[entry] += b'bad'
            with self.subTest(entry=entry), self.assertRaises(RuntimeError):
                p.validate_archive_delta(before, altered, p.PREVIOUS, p.BUNDLE, 'bundle')
        for mode in ('remove', 'add'):
            altered = copy.deepcopy(after)
            if mode == 'remove': altered.pop('other/resource')
            else: altered['extra'] = b'bad'
            with self.subTest(mode=mode), self.assertRaises(RuntimeError):
                p.validate_archive_delta(before, altered, p.PREVIOUS, p.BUNDLE, 'bundle')

    def test_archive_duplicate_and_path_traversal_rejected(self):
        with self.assertRaises(RuntimeError): p.archive(zipped({'../escape': b'no'}))
        b = io.BytesIO()
        with zipfile.ZipFile(b, 'w') as z:
            z.writestr('dup', b'a')
            with __import__('warnings').catch_warnings():
                __import__('warnings').simplefilter('ignore'); z.writestr('dup', b'b')
        with self.assertRaises(RuntimeError): p.archive(b.getvalue())

    def test_two_builds_must_differ_in_location_and_match_bytes(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); one, two = root / 'one', root / 'two'; one.mkdir(); two.mkdir()
            before, before_single = zipped(package(p.PREVIOUS, 1)), zipped(package(p.SINGLE_PREVIOUS, 1))
            expected = expected_manifest()
            for name in p.ASSETS:
                (one / name).write_bytes(b'a'); (two / name).write_bytes(b'b')
                expected['artifacts'][name] = {'bytes': 1, 'sha256': p.sha(b'a')}
            with mock.patch.multiple(p, BASE_SHA256=p.sha(before), SINGLE_BASE_SHA256=p.sha(before_single)):
                with self.assertRaisesRegex(RuntimeError, 'distinct'):
                    p.validate_local(one, one, expected, before, before_single)
                with self.assertRaisesRegex(RuntimeError, 'Independent build differs'):
                    p.validate_local(one, two, expected, before, before_single)

    def test_atomic_feed_update_uses_both_expected_head_leases(self):
        states = {branch: {'head': digit * 40, 'branch': branch} for branch, digit in [('main', 'a'), ('dev', 'b')]}
        updates = {b: {'patches-bundle.json': b'{}'} for b in states}
        helper = mock.Mock()
        def prepare(repo, state, files, message): return {**state, 'head': ('c' if state['branch'] == 'main' else 'd') * 40}
        with mock.patch.object(p, 'git', return_value='') as git, mock.patch.object(p, 'prepare_commit', side_effect=prepare):
            out = p.commit_feeds_atomically(helper, Path('/unused'), states, updates, 'message')
        push = [call.args for call in git.call_args_list if 'push' in call.args][0]
        self.assertIn('--atomic', push)
        self.assertIn('--force-with-lease=refs/heads/main:' + 'a' * 40, push)
        self.assertIn('--force-with-lease=refs/heads/dev:' + 'b' * 40, push)
        self.assertIn('c' * 40 + ':refs/heads/main', push)
        self.assertIn('d' * 40 + ':refs/heads/dev', push)
        self.assertEqual(helper.check_heads.call_count, 3)
        self.assertEqual(set(out), {'main', 'dev'})

    def test_missing_branch_aborts_before_git(self):
        with mock.patch.object(p, 'git') as git, self.assertRaises(RuntimeError):
            p.commit_feeds_atomically(mock.Mock(), Path('/unused'), {'main': {}}, {'main': {}}, 'message')
        git.assert_not_called()


if __name__ == '__main__':
    unittest.main(verbosity=2)
