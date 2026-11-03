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
spec = importlib.util.spec_from_file_location('p', ROOT / 'publish1832.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)


class FeedTests(unittest.TestCase):
    def setUp(self):
        self.state = {'manifest': {'version': p.PREVIOUS, 'download_url': p.BASE_URL,
            'created_at': '2026-10-28T16:28:08', 'description': 'History\nSwiftKey Beta：v1.8.31（9.13.16.4／1236271168）\nULike：v1.9.19（5.6.2／740）\n',
            'page_url': 'old-page', 'signature_download_url': '', 'custom': 'preserved'},
            'log': '# 1.0.343 (2026-10-28)\n\n* **SwiftKey Beta:** previous\n\n# 1.0.313 (2026-10-28)\n\n* **ULike:** historical\n'}
        self.notes = 'SwiftKey Beta v1.8.32 / Hiro Morphe Patches v1.0.344\n英語上フリック修正。Shift大文字維持・絵文字候補保持。実機未確認。'
    def metadata(self):
        return p.metadata(self.state, 'https://example.invalid/new.mpp', '2026-10-29T00:00:00', self.notes)
    def test_update_scope_and_history(self):
        old = copy.deepcopy(self.state)
        feed, log = self.metadata()
        self.assertEqual(self.state, old)
        self.assertEqual(feed['custom'], 'preserved')
        self.assertEqual(feed['version'], p.BUNDLE)
        self.assertEqual(feed['description'], self.notes + '\n\n' + old['manifest']['description'].replace('Beta：v1.8.31', 'Beta：v1.8.32'))
        self.assertTrue(log.endswith(old['log']))
        for name in p.APP_NAMES:
            self.assertTrue(p.has_changes_for(log, p.PREVIOUS, [name]))
            self.assertFalse(p.has_changes_for(log, p.BUNDLE, [name]))
        for name in p.OTHER_APPS:
            self.assertFalse(p.has_changes_for(log, p.PREVIOUS, [name]))
    def test_advanced_feed_rejected(self):
        self.state['manifest']['version'] = '1.0.344'
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
        self.state['manifest']['description'] = 'Microsoft SwiftKey Beta：v1.8.31（9.13.16.4）'
        feed, log = self.metadata()
        self.assertIn('Microsoft SwiftKey Beta：v1.8.32（9.13.16.4）', feed['description'])


class ContractTests(unittest.TestCase):
    def test_bool_is_not_integer(self):
        with self.assertRaises(RuntimeError): p.exact(True, 1, 'type confusion')
    def test_path_traversal_rejected(self):
        for name in ('../x', '/x', 'a/../../b', 'a\\b'):
            with self.subTest(name=name), self.assertRaises(RuntimeError): p.checked_path(name)
    def test_missing_evidence_rejected(self):
        with self.assertRaises(RuntimeError): p.lookup_qa({}, 'host_report.status')
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
        dex = bytearray(112); dex[:4] = b'dex\n'; struct.pack_into('<I', dex, 96, 92)
        self.old = {p.MF: b'Version: 1.8.31\nName: SwiftKey\n', p.EXTENSION: bytes(dex),
                    p.EMOJI: b'old emoji', 'classes.dex': bytes(dex), p.LOADER_CLASS: b'old JVM',
                    'swiftkey/words_ja.tsv': b'unchanged expanded dictionary', 'other-app': b'untouched', p.SHIFT_CLASS: b'existing Shift helper',
                    p.JVM_PREFIX+'SpaceConversionPatch.class': b'existing space helper'}
        self.new = {**self.old, p.MF: b'Version: 1.8.32\nName: SwiftKey\n',
                    p.EXTENSION: bytes(dex) + b'Space fix', 'classes.dex': bytes(dex) + b'Loader fix',
                    p.SPACE_LOADER_CLASS: b'new space helper'}
    def validate(self):
        return p.validate_archive_delta(self.old, self.new, '1.8.31', '1.8.32', 'standalone')
    def test_exact_update_scope(self):
        self.assertEqual(self.validate(), sorted(p.REQUIRED_CHANGED))
    def test_other_app_edit_rejected(self):
        self.new['other-app'] = b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_unrelated_main_loader_edit_rejected(self):
        self.new[p.LOADER_CLASS] += b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_android_loader_update_rejected(self):
        self.new['classes.dex'] = self.old['classes.dex']
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_space_helper_update_rejected(self):
        self.new[p.SPACE_LOADER_CLASS] = self.old[p.SPACE_LOADER_CLASS]
        with self.assertRaises(RuntimeError): self.validate()
    def test_emoji_dictionary_edit_rejected(self):
        self.new[p.EMOJI] += b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_shift_helper_edit_rejected(self):
        self.new[p.SHIFT_CLASS] += b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_dictionary_edit_rejected(self):
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
        self.new[p.MF] = b'Version: 1.8.32\nName: Changed\n'
        with self.assertRaises(RuntimeError): self.validate()


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.runtime = 'f' * 64
        self.qa = {}
        for path, value in p.REQUIRED_QA.items():
            parts = path.split('.')
            obj = self.qa
            for key in parts[:-1]: obj = obj.setdefault(key, {})
            obj[parts[-1]] = value
        self.qa['native_route_report'] = {'status': 'PASSED', 'apk_sha256': p.REFERENCE_APK_SHA256,
            'existing_hook_is_on_space_route': True, 'baseline_failure_reproduced_by_this_test': False,
            'device_tested': False, 'full_apk_runtime_execution': False}
        self.qa['loader_build_report'] = {'status': 'PASSED',
            'unrelated_android_classes_preserved': True,
            'unrelated_jvm_classes_preserved': True,
            'changed_android_classes': [p.SPACE_LOADER_TYPE], 'loader_classes_added': 0,
            'loader_classes_replaced': 1, 'jvm_main_and_bridges_byte_exact': True,
            'android_main_and_bridges_canonical_preserved': True, 'android_serialization_canonical_preservation': True,
            'native_report': {'status': 'PASSED', 'separator_getters_rewritten': 7,
                'separator_methods_changed': 6, 'original_fallback_preserved': True,
                'native_methods_changed': 7, 'native_instructions_added': 4,
                'inverse_native_method_restoration': True, 'all_target_drift_rejected_before_mutation': True}}
        native = self.qa['loader_build_report']['native_report']
        native.update(reference_apk_sha256=p.REFERENCE_APK_SHA256,
            old_loader_separator_getters_rewritten=0, old_and_new_sdk_apply_executed=True,
            native_fixture_dex_serialized=True, candidate_getter_bodies_unchanged=True,
            android_runtime_executed=False, device_tested=False, original_apk_apply_tested=False)
        sdk_native = {k: v for k, v in native.items() if k != 'reference_apk_sha256'}
        native['morphe_runtime_tests'] = {name: copy.deepcopy(sdk_native) for name in ('morphe.jar', 'morphe-current.jar')}
        native['morphe_runtime_sha256'] = {
            'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
            'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573'}
        self.qa['native_space_report'] = copy.deepcopy(self.qa['loader_build_report']['native_report'])
        self.qa['runtime_sha256'] = self.runtime
        self.qa['host_report'] = {'status': 'PASSED', 'runtime_sha256': self.runtime}
        self.qa['runtime_report'] = {'status': 'PASSED', 'runtime_sha256': self.runtime,
            'baseline_runtime_sha256': p.BASE_RUNTIME_SHA256, 'all_other_classes_preserved': True,
            'changed_classes': ['Lhiro/swiftkey/SpaceConversion;'], 'added_classes': [], 'host_tests_passed': True}
        loader = {'status': 'PASSED', 'selected_extension_stream_verified': True,
            'selected_extension_sha256': self.runtime, 'baseline_extension_sha256': p.BASE_RUNTIME_SHA256,
            'baseline_updated_dependency_graph_equal': True,
            'standalone_combined_dependency_graph_equal': True,
            'clean_apk_apply_tested': False, 'device_tested': False}
        self.qa['loader_reports'] = [loader, copy.deepcopy(loader)]
        self.qa['runtime_report'].update(baseline_java_matches_published_dex=True,
            native_abi_verified=True,
            native_abi_report={'status': 'PASSED', 'extension_sha256': self.runtime,
                'apk_sha256': p.REFERENCE_APK_SHA256, 'unresolved': [], 'inaccessible': [],
                'incompatible_opcodes': []},
            host_report={'status': 'PASSED', 'assertions': 354, 'candidate_mutation_removed': True,
                'identity_scoped_separator_reads': True, 'immutable_native_cases': 8, 'baseline_regressions': {'status': 'PASSED',
                'baseline_failures_reproduced': 6, 'failure_scope': 'immutable_native_prediction_separator',
                'device_trace_reproduced': False}})
        self.qa['runtime_report']['host_report']['native_raw_evidence'] = {
            'status': 'PASSED', 'reference_apk_sha256': p.REFERENCE_APK_SHA256,
            'native_state_and_request_share_marker_raw': True,
            'native_raw_append_has_no_script_filter': True,
            'native_romaji_call_has_no_all_latin_precondition': True,
            'native_correction_text_can_be_reused_for_editing': True,
            'android_code_executed': False, 'device_tested': False, 'user_video_raw_input_observed': False}
        self.qa['runtime_report']['host_report']['native_separator_evidence'] = {
            'status': 'PASSED', 'reference_apk_sha256': p.REFERENCE_APK_SHA256, 'fragment_count': 29,
            'native_fluency_candidate_setter_always_throws': True,
            'native_prediction_factory_produces_immutable_candidates': True,
            'native_precommit_null_separator_calls_setter': True,
            'commit_separator_sites': 7, 'commit_separator_methods': 6,
            'native_visitor_unwraps_candidates': True,
            'native_context_guard_checks_K_against_request_i': True,
            'wrapper_chain_public_field_count': 9, 'email_wrapper_has_public_accessor': True,
            'android_code_executed': False, 'device_tested': False, 'user_device_candidate_type_observed': False}
    def validate(self):
        return p.validate_evidence(self.qa, {'qa_required_values': p.REQUIRED_QA}, self.runtime, 'e' * 64)
    def test_matching_runtime_evidence_accepted(self):
        self.assertTrue(self.validate()['host_runtime_bound'])
    def test_route_must_not_claim_baseline_failure(self):
        self.qa['native_route_report']['baseline_failure_reproduced_by_this_test'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_route_evidence_rejected(self):
        del self.qa['native_route_report']
        with self.assertRaises(RuntimeError): self.validate()
    def test_native_separator_scope_must_not_claim_device_trace(self):
        self.qa['runtime_report']['host_report']['baseline_regressions']['device_trace_reproduced'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_native_separator_regression_count_must_match(self):
        self.qa['runtime_report']['host_report']['baseline_regressions']['baseline_failures_reproduced'] = 0
        with self.assertRaises(RuntimeError): self.validate()
    def test_wrong_regression_scope_rejected(self):
        self.qa['runtime_report']['host_report']['baseline_regressions']['failure_scope'] = 'valid_exact_native_raw_requests'
        with self.assertRaises(RuntimeError): self.validate()
    def test_unrelated_android_loader_change_rejected(self):
        self.qa['loader_build_report']['changed_android_classes'].append('Lapp/hiro/swiftkey/patches/ShiftLockPatch;')
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_native_separator_hook_rejected(self):
        self.qa['loader_build_report']['native_report']['separator_getters_rewritten'] = 6
        with self.assertRaises(RuntimeError): self.validate()
    def test_unpreserved_native_fallback_rejected(self):
        self.qa['loader_build_report']['native_report']['original_fallback_preserved'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_native_space_probe_must_match_loader_build(self):
        self.qa['native_space_report']['separator_getters_rewritten'] = 999
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_separator_source_evidence_rejected(self):
        del self.qa['runtime_report']['host_report']['native_separator_evidence']
        with self.assertRaises(RuntimeError): self.validate()
    def test_device_candidate_type_must_not_be_claimed(self):
        self.qa['runtime_report']['host_report']['native_separator_evidence']['user_device_candidate_type_observed'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_incomplete_separator_callsites_rejected(self):
        self.qa['runtime_report']['host_report']['native_separator_evidence']['commit_separator_sites'] = 6
        with self.assertRaises(RuntimeError): self.validate()
    def test_unscoped_separator_reads_rejected(self):
        self.qa['runtime_report']['host_report']['identity_scoped_separator_reads'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_candidate_mutation_must_be_removed(self):
        self.qa['runtime_report']['host_report']['candidate_mutation_removed'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_all_immutable_native_cases_required(self):
        self.qa['runtime_report']['host_report']['immutable_native_cases'] = 7
        with self.assertRaises(RuntimeError): self.validate()
    def test_both_native_sdk_probes_required(self):
        for target in ('loader_build_report',):
            del self.qa[target]['native_report']['morphe_runtime_tests']['morphe-current.jar']
        self.qa['native_space_report'] = copy.deepcopy(self.qa['loader_build_report']['native_report'])
        with self.assertRaises(RuntimeError): self.validate()
    def test_disagreeing_native_sdk_probe_rejected(self):
        self.qa['loader_build_report']['native_report']['morphe_runtime_tests']['morphe-current.jar']['separator_getters_rewritten'] = 6
        self.qa['native_space_report'] = copy.deepcopy(self.qa['loader_build_report']['native_report'])
        with self.assertRaises(RuntimeError): self.validate()
    def test_host_test_for_wrong_runtime_rejected(self):
        self.qa['host_report']['runtime_sha256'] = 'a' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_runtime_host_failure_rejected(self):
        self.qa['runtime_report']['host_tests_passed'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_unrelated_runtime_class_rejected(self):
        self.qa['runtime_report']['changed_classes'].append('Lhiro/swiftkey/EmojiCandidates;')
        with self.assertRaises(RuntimeError): self.validate()
    def test_similar_class_prefix_rejected(self):
        self.qa['runtime_report']['changed_classes'] = ['Lhiro/swiftkey/FlickSymbolsUnrelated;']
        with self.assertRaises(RuntimeError): self.validate()
    def test_unverified_space_suite_rejected(self):
        self.qa['runtime_report']['host_report']['status'] = 'UNVERIFIED'
        with self.assertRaises(RuntimeError): self.validate()
    def test_native_abi_mismatch_rejected(self):
        self.qa['runtime_report']['native_abi_report']['extension_sha256'] = '0' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_new_runtime_class_rejected(self):
        self.qa['runtime_report']['added_classes'] = ['Lhiro/swiftkey/Unexpected;']
        with self.assertRaises(RuntimeError): self.validate()
    def test_ranking_change_rejected(self):
        self.qa['runtime_report']['changed_classes'].append('Lhiro/swiftkey/WordCandidates;')
        with self.assertRaises(RuntimeError): self.validate()
    def test_unpreserved_runtime_rejected(self):
        self.qa['runtime_report']['all_other_classes_preserved'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_loader_selected_wrong_runtime_rejected(self):
        for loader in self.qa['loader_reports']: loader['selected_extension_sha256'] = 'a' * 64
        with self.assertRaises(RuntimeError): self.validate()


class GitTransportTests(unittest.TestCase):
    def test_remote_git_heads_match_exact_leases(self):
        states = {b: {'branch': b, 'head': h * 40} for b, h in [('main', 'a'), ('dev', 'b')]}
        raw = 'b' * 40 + '\trefs/heads/dev\n' + 'a' * 40 + '\trefs/heads/main\n'
        with patch.object(p, 'git', return_value=raw) as call:
            p.check_heads_git(Path('.'), states)
        self.assertIn('ls-remote', call.call_args.args)
    def test_concurrent_remote_update_rejected(self):
        states = {'main': {'branch': 'main', 'head': 'a' * 40}}
        with patch.object(p, 'git', return_value='b' * 40 + '\trefs/heads/main\n'):
            with self.assertRaises(RuntimeError): p.check_heads_git(Path('.'), states)
    def test_missing_remote_branch_rejected(self):
        states = {'main': {'branch': 'main', 'head': 'a' * 40}}
        with patch.object(p, 'git', return_value=''):
            with self.assertRaises(RuntimeError): p.check_heads_git(Path('.'), states)
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

