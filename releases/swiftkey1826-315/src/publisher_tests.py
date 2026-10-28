#!/usr/bin/env python3
"""Publication guards: feed scope, exact packages, source pins, and data preservation."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import subprocess
import struct
from unittest.mock import patch
import unittest

ROOT = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('p', ROOT / 'publish1826.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)


class FeedTests(unittest.TestCase):
    def setUp(self):
        self.state = {'manifest': {'version': p.PREVIOUS, 'download_url': p.BASE_URL,
            'created_at': '2026-10-28T16:28:08', 'description': 'History\nSwiftKey Beta：v1.8.25（9.13.16.4／1236271168）\nULike：v1.9.19（5.6.2／740）\n',
            'page_url': 'old-page', 'signature_download_url': '', 'custom': 'preserved'},
            'log': '# 1.0.314 (2026-10-28)\n\n* **SwiftKey Beta:** previous\n\n# 1.0.313 (2026-10-28)\n\n* **ULike:** historical\n'}
        self.notes = 'SwiftKey Beta v1.8.26 / Hiro Morphe Patches v1.0.315\nShift大文字維持、関連語の絵文字候補拡充。実機未確認。'
    def metadata(self):
        return p.metadata(self.state, 'https://example.invalid/new.mpp', '2026-10-29T00:00:00', self.notes)
    def test_update_scope_and_history(self):
        old = copy.deepcopy(self.state)
        feed, log = self.metadata()
        self.assertEqual(self.state, old)
        self.assertEqual(feed['custom'], 'preserved')
        self.assertEqual(feed['version'], p.BUNDLE)
        self.assertEqual(feed['description'], self.notes + '\n\n' + old['manifest']['description'].replace('Beta：v1.8.25', 'Beta：v1.8.26'))
        self.assertTrue(log.endswith(old['log']))
        for name in p.APP_NAMES:
            self.assertTrue(p.has_changes_for(log, p.PREVIOUS, [name]))
            self.assertFalse(p.has_changes_for(log, p.BUNDLE, [name]))
        for name in p.OTHER_APPS:
            self.assertFalse(p.has_changes_for(log, p.PREVIOUS, [name]))
    def test_advanced_feed_rejected(self):
        self.state['manifest']['version'] = '1.0.316'
        with self.assertRaises(RuntimeError): self.metadata()
    def test_changed_baseline_url_rejected(self):
        self.state['manifest']['download_url'] = 'https://example.invalid/other.mpp'
        with self.assertRaises(RuntimeError): self.metadata()
    def test_ambiguous_inventory_rejected(self):
        self.state['manifest']['description'] *= 2
        with self.assertRaises(RuntimeError): self.metadata()
    def test_wrong_inventory_rejected(self):
        self.state['manifest']['description'] = 'SwiftKey Beta：v1.8.24（9.13.16.4）'
        with self.assertRaises(RuntimeError): self.metadata()
    def test_timestamp_must_advance(self):
        self.state['manifest']['created_at'] = '2026-10-30T00:00:00'
        with self.assertRaises(RuntimeError): self.metadata()
    def test_competing_notes_heading_rejected(self):
        self.notes += '\n# 1.0.999 (2026-10-29)'
        with self.assertRaises(RuntimeError): self.metadata()
    def test_microsoft_alias_preserved(self):
        self.state['manifest']['description'] = 'Microsoft SwiftKey Beta：v1.8.25（9.13.16.4）'
        feed, log = self.metadata()
        self.assertIn('Microsoft SwiftKey Beta：v1.8.26（9.13.16.4）', feed['description'])


class DictionaryTests(unittest.TestCase):
    def setUp(self):
        self.old = 'ありがとう\t🙏\t😊\nねこ\t🐈\n'.encode()
        self.new = 'ありがとう\t🙏\t😊\t💐\nおやすみ\t😴\nねこ\t🐈\n'.encode()
    def test_additions_and_preservation(self):
        q = p.validate_dictionary(self.old, self.new)
        self.assertEqual((q['old_readings'], q['old_candidates'], q['readings'], q['candidates'], q['added_candidates']), (2, 3, 3, 5, 2))
        self.assertEqual(q['sha256'], p.sha(self.new))
    def reject(self, raw):
        with self.assertRaises(RuntimeError): p.validate_dictionary(self.old, raw.encode())
    def test_original_order_required(self):
        self.reject('ありがとう\t😊\t🙏\t💐\nねこ\t🐈\n')
    def test_original_key_required(self):
        self.reject('ありがとう\t🙏\t😊\t💐\n')
    def test_original_pair_required(self):
        self.reject('ありがとう\t🙏\t💐\nねこ\t🐈\n')
    def test_duplicate_pair_rejected(self):
        self.reject('ありがとう\t🙏\t😊\t😊\nねこ\t🐈\n')
    def test_duplicate_key_rejected(self):
        self.reject('ありがとう\t🙏\t😊\nありがとう\t💐\nねこ\t🐈\n')
    def test_control_character_rejected(self):
        self.reject('ありがとう\t🙏\t😊\t💐\r\nねこ\t🐈\n')
    def test_missing_candidate_rejected(self):
        self.reject('ありがとう\t🙏\t😊\t💐\nおやすみ\t\nねこ\t🐈\n')
    def test_expansion_required(self):
        with self.assertRaises(RuntimeError): p.validate_dictionary(self.old, self.old)


class ContractTests(unittest.TestCase):
    def test_bool_is_not_integer(self):
        with self.assertRaises(RuntimeError): p.exact(True, 1, 'type confusion')
    def test_path_traversal_rejected(self):
        for name in ('../x', '/x', 'a/../../b', 'a\\b'):
            with self.subTest(name=name), self.assertRaises(RuntimeError): p.checked_path(name)
    def test_missing_evidence_rejected(self):
        with self.assertRaises(RuntimeError): p.lookup_qa({}, 'host_report.shift.status')
    def test_source_pins_require_all_files(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            with self.assertRaises(RuntimeError): p.verify_source_files(root, {'source_files': {}})
    def test_manifest_pins_reject_missing_artifacts(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'manifest.json'
            path.write_text('{}')
            with self.assertRaises(RuntimeError): p.load_expected(path)


class ArchiveTests(unittest.TestCase):
    def setUp(self):
        dex = bytearray(112); dex[:4] = b'dex\n'; struct.pack_into('<I', dex, 96, 91)
        self.old = {p.MF: b'Version: 1.8.25\nName: SwiftKey\n', p.EXTENSION: bytes(dex),
                    p.EMOJI: b'old emoji', 'classes.dex': bytes(dex), p.LOADER_CLASS: b'old JVM',
                    'swiftkey/words_ja.tsv': b'unchanged expanded dictionary', 'other-app': b'untouched'}
        self.new = {**self.old, p.MF: b'Version: 1.8.26\nName: SwiftKey\n', p.EMOJI: b'new emoji',
                    'classes.dex': bytes(dex) + b'new loader', p.LOADER_CLASS: b'new JVM', p.SHIFT_CLASS: b'new helper'}
    def validate(self):
        return p.validate_archive_delta(self.old, self.new, '1.8.25', '1.8.26', 'standalone')
    def test_exact_update_scope(self):
        self.assertEqual(self.validate(), sorted(p.REQUIRED_CHANGED))
    def test_other_app_edit_rejected(self):
        self.new['other-app'] = b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_runtime_edit_rejected(self):
        self.new[p.EXTENSION] += b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_nonemoji_dictionary_edit_rejected(self):
        self.new['swiftkey/words_ja.tsv'] = b'old dictionary regression'
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_helper_rejected(self):
        del self.new[p.SHIFT_CLASS]
        with self.assertRaises(RuntimeError): self.validate()
    def test_extra_loader_rejected(self):
        self.new[p.JVM_PREFIX + 'Other.class'] = b'extra'
        with self.assertRaises(RuntimeError): self.validate()
    def test_removed_asset_rejected(self):
        del self.new['other-app']
        with self.assertRaises(RuntimeError): self.validate()
    def test_other_manifest_field_rejected(self):
        self.new[p.MF] = b'Version: 1.8.26\nName: Changed\n'
        with self.assertRaises(RuntimeError): self.validate()


class GitTransportTests(unittest.TestCase):
    def test_private_index_preserves_worktree_and_unrelated_paths(self):
        with tempfile.TemporaryDirectory() as d:
            repo = Path(d)
            def git(*args):
                return subprocess.check_output(['git', '-C', str(repo), *args], stderr=subprocess.PIPE).decode().strip()
            git('init', '-q')
            git('config', 'user.name', 'Publisher Test')
            git('config', 'user.email', 'publisher-test@example.invalid')
            (repo / 'keep.txt').write_text('original unrelated bytes')
            (repo / 'feed.json').write_text('old feed')
            git('add', '.')
            git('commit', '-qm', 'baseline')
            old = git('rev-parse', 'HEAD')
            (repo / 'keep.txt').write_text('user staged edit')
            git('add', 'keep.txt')
            before_index = (repo / '.git/index').read_bytes()
            state = {'head': old, 'branch': 'main'}
            prepared = p.prepare_commit(repo, state, {'feed.json': b'new feed'}, 'publication')
            self.assertEqual(git('rev-parse', 'HEAD'), old)
            self.assertEqual((repo / '.git/index').read_bytes(), before_index)
            self.assertEqual((repo / 'keep.txt').read_text(), 'user staged edit')
            self.assertEqual((repo / 'feed.json').read_text(), 'old feed')
            self.assertEqual(git('show', prepared['head'] + ':keep.txt'), 'original unrelated bytes')
            self.assertEqual(git('show', prepared['head'] + ':feed.json'), 'new feed')
            self.assertEqual(git('show', '-s', '--format=%P', prepared['head']), old)
    def test_both_feed_refs_push_atomically_with_leases(self):
        states = {branch: {'branch': branch, 'head': 'old-' + branch} for branch in ('main', 'dev')}
        calls = []
        class Helper:
            def check_heads(self, values):
                calls.append(('heads', values))
        def fake_git(repo, *args, **kwargs):
            calls.append(('git', args))
            return ''
        def fake_prepare(repo, state, updates, message):
            return {**state, 'head': 'new-' + state['branch']}
        with patch.object(p, 'git', side_effect=fake_git), patch.object(p, 'prepare_commit', side_effect=fake_prepare):
            result = p.commit_feeds_atomically(Helper(), Path('.'), states,
                {'main': {'feed': b'a'}, 'dev': {'feed': b'b'}}, 'publish')
        push = [args for kind, args in calls if kind == 'git' and args[0] == 'push']
        self.assertEqual(len(push), 1)
        self.assertIn('--atomic', push[0])
        for branch in states:
            self.assertIn('--force-with-lease=refs/heads/' + branch + ':old-' + branch, push[0])
            self.assertIn('new-' + branch + ':refs/heads/' + branch, push[0])
        self.assertEqual(set(result), {'main', 'dev'})
    def test_single_feed_mutation_rejected(self):
        with self.assertRaises(RuntimeError):
            p.commit_feeds_atomically(None, Path('.'), {'main': {}}, {'main': {}}, 'bad')


if __name__ == '__main__':
    unittest.main(verbosity=2)
