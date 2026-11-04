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
spec = importlib.util.spec_from_file_location('p', ROOT / 'publish1833.py')
p = importlib.util.module_from_spec(spec)
spec.loader.exec_module(p)


class FeedTests(unittest.TestCase):
    def setUp(self):
        self.state = {'manifest': {'version': p.PREVIOUS, 'download_url': p.BASE_URL,
            'created_at': '2026-10-28T16:28:08', 'description': 'History\nSwiftKey Beta：v1.8.32（9.13.16.4／1236271168）\nULike：v1.9.19（5.6.2／740）\n',
            'page_url': 'old-page', 'signature_download_url': '', 'custom': 'preserved'},
            'log': '# 1.0.344 (2026-10-28)\n\n* **SwiftKey Beta:** previous\n\n# 1.0.313 (2026-10-28)\n\n* **ULike:** historical\n'}
        self.notes = 'SwiftKey Beta v1.8.33 / Hiro Morphe Patches v1.0.345\n英語上フリック修正。Shift大文字維持・絵文字候補保持。実機未確認。'
    def metadata(self):
        return p.metadata(self.state, 'https://example.invalid/new.mpp', '2026-10-29T00:00:00', self.notes)
    def test_update_scope_and_history(self):
        old = copy.deepcopy(self.state)
        feed, log = self.metadata()
        self.assertEqual(self.state, old)
        self.assertEqual(feed['custom'], 'preserved')
        self.assertEqual(feed['version'], p.BUNDLE)
        self.assertEqual(feed['description'], self.notes + '\n\n' + old['manifest']['description'].replace('Beta：v1.8.32', 'Beta：v1.8.33'))
        self.assertTrue(log.endswith(old['log']))
        for name in p.APP_NAMES:
            self.assertTrue(p.has_changes_for(log, p.PREVIOUS, [name]))
            self.assertFalse(p.has_changes_for(log, p.BUNDLE, [name]))
        for name in p.OTHER_APPS:
            self.assertFalse(p.has_changes_for(log, p.PREVIOUS, [name]))
    def test_advanced_feed_rejected(self):
        self.state['manifest']['version'] = '1.0.345'
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
        self.state['manifest']['description'] = 'Microsoft SwiftKey Beta：v1.8.32（9.13.16.4）'
        feed, log = self.metadata()
        self.assertIn('Microsoft SwiftKey Beta：v1.8.33（9.13.16.4）', feed['description'])


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
        self.old = {p.MF: b'Version: 1.8.32\nName: SwiftKey\n', p.EXTENSION: bytes(dex),
                    p.EMOJI: b'old emoji', 'classes.dex': bytes(dex), p.LOADER_CLASS: b'old JVM',
                    'swiftkey/words_ja.tsv': b'unchanged expanded dictionary', 'other-app': b'untouched', p.SHIFT_CLASS: b'existing Shift helper',
                    p.JVM_PREFIX+'SpaceConversionPatch.class': b'existing space helper'}
        self.new = {**self.old, p.MF: b'Version: 1.8.33\nName: SwiftKey\n',
                    p.EXTENSION: bytes(dex) + b'Context selection fix'}
    def validate(self):
        return p.validate_archive_delta(self.old, self.new, '1.8.32', '1.8.33', 'standalone')
    def test_exact_update_scope(self):
        self.assertEqual(self.validate(), sorted(p.REQUIRED_CHANGED))
    def test_other_app_edit_rejected(self):
        self.new['other-app'] = b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_unrelated_main_loader_edit_rejected(self):
        self.new[p.LOADER_CLASS] += b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_android_loader_change_rejected(self):
        self.new['classes.dex'] = self.old['classes.dex'] + b'changed'
        with self.assertRaises(RuntimeError): self.validate()
    def test_space_helper_change_rejected(self):
        self.new[p.SPACE_LOADER_CLASS] = self.old[p.SPACE_LOADER_CLASS] + b'changed'
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
        self.new[p.MF] = b'Version: 1.8.33\nName: Changed\n'
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
        self.qa['retained_separator_loader_evidence'] = json.loads((ROOT / 'loader/retained1832.json').read_text())
        self.retained = self.qa['retained_separator_loader_evidence']
        self.qa['loader_preservation_report'] = {
            'status': 'PASSED', 'baseline_swiftkey_version': p.SINGLE_PREVIOUS,
            'all_jvm_loader_bytes_preserved': True, 'all_android_loader_bytes_preserved': True,
            'loader_compiled': False}
        self.qa['runtime_sha256'] = self.runtime
        self.qa['host_report'] = {'status': 'PASSED', 'runtime_sha256': self.runtime}
        self.qa['runtime_report'] = {'status': 'PASSED', 'runtime_sha256': self.runtime,
            'baseline_runtime_sha256': p.BASE_RUNTIME_SHA256, 'all_other_classes_preserved': True,
            'changed_classes': list(p.RUNTIME_CHANGED_CLASSES), 'added_classes': [], 'host_tests_passed': True}
        self.qa['runtime_report']['merge'] = {
            'status': 'PASSED', 'extension_sha256': self.runtime,
            'baseline_extension_sha256': p.BASE_RUNTIME_SHA256,
            'changed_existing_classes': list(p.RUNTIME_CHANGED_CLASSES),
            'added_classes': [], 'runtime_before_classes': 92, 'runtime_after_classes': 92,
            'rank_method_only_replacement': True, 'rank_method_descriptor': p.RANK_METHOD,
            'frequency_nested_classes_preserved': True, 'frequency_other_members_preserved': True,
            'inverse_rank_restores_original_class': True, 'compiled_rank_body_preserved': True, 'rank_overlay_sha256': 'c' * 64}
        self.qa['runtime_report']['ranking_report'] = {
            'status': 'PASSED', 'baseline_runtime_sha256': p.BASE_RUNTIME_SHA256,
            'baseline_rank_matches_published': True, 'baseline_java_matches_published_rank': True,
            'bridge_java_matches_published_rank': True,
            'baseline_rank_sha256': 'a059975c9295141ff8f7b8000fdb81cf3ee3246cdd2a5077605fe4d1166bb585',
            'fixture_count': 7, 'baseline_context_order_regressions': 6, 'fixed_context_order_regressions': 0,
            'preserves_current_order_then_appends_unseen_history': True,
            'blocked_and_revoked_session_boundaries_tested': True,
            'native_preferences_are_modeled_inputs': True,
            'store_and_cleanup_are_models_bound_to_published_method_fixtures': True,
            'merge_must_preserve_all_other_methods_and_classes': True,
            'android_device_tested': False, 'fixed_dex_sha256': 'c' * 64, 'fixed_rank_sha256': 'd' * 64,
            'fixture_manifest_sha256': p.sha((ROOT / 'ranking/fixtures/manifest.json').read_bytes()),
            'baseline_host': {'status': 'PASSED', 'mode': 'baseline', 'context_order_regressions': 6, 'assertions': 54,
                'native_preferences_are_modeled_inputs': True, 'device_tested': False},
            'fixed_host': {'status': 'PASSED', 'mode': 'fixed', 'context_order_regressions': 0, 'assertions': 61,
                'native_preferences_are_modeled_inputs': True, 'device_tested': False}}
        loader = {'status': 'PASSED', 'selected_extension_stream_verified': True,
            'selected_extension_sha256': self.runtime, 'baseline_extension_sha256': p.BASE_RUNTIME_SHA256,
            'baseline_updated_dependency_graph_equal': True,
            'standalone_combined_dependency_graph_equal': True,
            'clean_apk_apply_tested': False, 'device_tested': False}
        self.qa['loader_reports'] = [loader, copy.deepcopy(loader)]
        self.qa['runtime_report'].update(baseline_java_matches_published_dex=True,
            baseline_java_scope=['SpaceConversion entire class', 'FrequencyLearning.rank method'],
            native_abi_verified=True,
            native_abi_report={'status': 'PASSED', 'extension_sha256': self.runtime,
                'apk_sha256': p.REFERENCE_APK_SHA256, 'audited_classes': list(p.RUNTIME_CHANGED_CLASSES),
                'audited_methods': [p.RANK_METHOD], 'unresolved': [], 'inaccessible': [],
                'incompatible_opcodes': []},
            host_report={'status': 'PASSED', 'assertions': 354, 'candidate_mutation_removed': True,
                'identity_scoped_separator_reads': True, 'immutable_native_cases': 8, 'baseline_regressions': {'status': 'PASSED',
                'assertions': 20, 'baseline_failures_reproduced': 4, 'scope': 'kana_prediction_skipped_by_kanji_filter',
                'language_model_executed': False}})
        self.qa['runtime_report']['host_report']['context_selection'] = {
            'status': 'PASSED', 'assertions': 152, 'original_objects_preserved': True,
            'synthetic_candidates_excluded': True, 'language_model_executed': False, 'device_tested': False}
        cache_case = {'status': 'PASSED', 'checks': 13, 'actual_production_space_handler': True,
            'actual_production_word_supplement': True, 'cache_hit_visible_candidate_case_verified': True,
            'native_cache_branch_translated': True, 'visible_kanji': '愛', 'device_tested': False}
        self.qa['runtime_report']['host_report']['cache_delivery_preservation'] = {
            'native_fixture_sha256': '62b9518ae9a14472198f1fcf708bdbab8e72c717781f6865c6ce3d3d9f9bc7b5',
            'dictionary_row_sha256': 'd3f53de2165130bc5e533d73f77a315aeadc220e3e71430d37748ad738e07b9e',
            'native_branch_translated_not_android_execution': True,
            'baseline': dict(cache_case, mode='baseline'), 'fixed': dict(cache_case, mode='fixed')}
        self.source_files = {'runtime/src/hiro/swiftkey/SpaceConversion.java': {'sha256': 'a' * 64},
            'ranking/fixtures/manifest.json': {'sha256': p.sha((ROOT / 'ranking/fixtures/manifest.json').read_bytes())}}
        metadata_dir = ROOT / 'runtime/tests/native_metadata_review'
        fixture_sha = {path.name: p.sha(path.read_bytes()) for path in metadata_dir.glob('*.smali')}
        for name, digest in fixture_sha.items():
            self.source_files['runtime/tests/native_metadata_review/' + name] = {'sha256': digest}
        self.qa['runtime_report']['native_metadata_evidence'] = {
            'status': 'PASSED', 'reference_apk_sha256': p.REFERENCE_APK_SHA256,
            'fixture_count': 13, 'fixture_sha256': fixture_sha, 'runtime_source_sha256': 'a' * 64,
            'prefix_flag_requires_separate_exclusion': True,
            'default_metadata_is_not_contextual_proof': True,
            'k0_is_native_true_verbatim_wrapper': True, 'g0_forwards_native_metadata_flags': True,
            'runtime_source_bound_not_executed_by_this_checker': True,
            'decoded_reference_rechecked': False, 'device_tested': False, 'original_apk_apply_tested': False,
            'native_truth_table': [{'prefix': prefix, 'verbatim': verbatim, 'native_D': prefix,
                'native_Q': not prefix and verbatim} for prefix in (False, True) for verbatim in (False, True)]}
        self.qa['source_sha256'] = {name: row['sha256'] for name, row in self.source_files.items()}
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
        return p.validate_evidence(self.qa, {'qa_required_values': p.REQUIRED_QA, 'source_files': self.source_files}, self.runtime, 'e' * 64)
    def test_matching_runtime_evidence_accepted(self):
        self.assertTrue(self.validate()['host_runtime_bound'])
    def test_qa_source_map_must_match_manifest(self):
        self.qa['source_sha256']['runtime/src/hiro/swiftkey/SpaceConversion.java'] = 'b' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_abi_must_cover_frequency_class_and_rank_method(self):
        for field, value in (('audited_classes', ['Lhiro/swiftkey/SpaceConversion;']), ('audited_methods', [])):
            with self.subTest(field=field):
                self.setUp()
                self.qa['runtime_report']['native_abi_report'][field] = value
                with self.assertRaises(RuntimeError): self.validate()
    def test_wrong_rank_graft_method_rejected(self):
        self.qa['runtime_report']['merge']['rank_method_descriptor'] = 'Lhiro/swiftkey/FrequencyLearning;->record()V'
        with self.assertRaises(RuntimeError): self.validate()
    def test_baseline_proof_scope_must_include_rank(self):
        self.qa['runtime_report']['baseline_java_scope'] = ['SpaceConversion entire class']
        with self.assertRaises(RuntimeError): self.validate()
    def test_metadata_wrong_source_rejected(self):
        self.qa['runtime_report']['native_metadata_evidence']['runtime_source_sha256'] = 'b' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_metadata_fixture_drift_rejected(self):
        hashes = self.qa['runtime_report']['native_metadata_evidence']['fixture_sha256']
        hashes[next(iter(hashes))] = 'b' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_metadata_false_verbatim_truth_table_rejected(self):
        self.qa['runtime_report']['native_metadata_evidence']['native_truth_table'][3]['native_Q'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_default_metadata_cannot_prove_context(self):
        self.qa['runtime_report']['native_metadata_evidence']['default_metadata_is_not_contextual_proof'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_context_selection_suite_required(self):
        del self.qa['runtime_report']['host_report']['context_selection']
        with self.assertRaises(RuntimeError): self.validate()
    def test_synthetic_context_selection_rejected(self):
        self.qa['runtime_report']['host_report']['context_selection']['synthetic_candidates_excluded'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_context_selection_must_not_claim_engine_execution(self):
        self.qa['runtime_report']['host_report']['context_selection']['language_model_executed'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_cache_visible_dictionary_preservation_required(self):
        self.qa['runtime_report']['host_report']['cache_delivery_preservation']['fixed']['cache_hit_visible_candidate_case_verified'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_ranking_regression_suite_assertions_required(self):
        for mode in ('baseline_host', 'fixed_host'):
            with self.subTest(mode=mode):
                self.setUp()
                self.qa['runtime_report']['ranking_report'][mode]['assertions'] = 0
                with self.assertRaises(RuntimeError): self.validate()
    def test_ranking_donor_must_match_merge(self):
        self.qa['runtime_report']['merge']['rank_overlay_sha256'] = 'b' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_ranking_native_preferences_must_be_modeled(self):
        self.qa['runtime_report']['ranking_report']['native_preferences_are_modeled_inputs'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_ranking_fixture_manifest_must_match_source(self):
        self.qa['runtime_report']['ranking_report']['fixture_manifest_sha256'] = 'b' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_new_ranking_order_failure_rejected(self):
        self.qa['runtime_report']['ranking_report']['fixed_host']['context_order_regressions'] = 1
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_session_boundaries_rejected(self):
        self.qa['runtime_report']['ranking_report']['blocked_and_revoked_session_boundaries_tested'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_unverified_ranking_baseline_rejected(self):
        self.qa['runtime_report']['ranking_report']['baseline_rank_matches_published'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_rank_graft_guards_required(self):
        for field in ('rank_method_only_replacement', 'frequency_other_members_preserved',
                      'inverse_rank_restores_original_class', 'compiled_rank_body_preserved', 'frequency_nested_classes_preserved'):
            with self.subTest(field=field):
                self.setUp()
                self.qa['runtime_report']['merge'][field] = False
                with self.assertRaises(RuntimeError): self.validate()
    def test_graft_for_wrong_runtime_rejected(self):
        self.qa['runtime_report']['merge']['extension_sha256'] = '0' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_frequency_nested_class_change_rejected(self):
        self.qa['runtime_report']['changed_classes'].append('Lhiro/swiftkey/FrequencyLearning$Store;')
        with self.assertRaises(RuntimeError): self.validate()
    def test_loader_compile_claim_rejected(self):
        self.qa['loader_preservation_report']['loader_compiled'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_unpreserved_current_loaders_rejected(self):
        for field in ('all_jvm_loader_bytes_preserved', 'all_android_loader_bytes_preserved'):
            with self.subTest(field=field):
                self.setUp()
                self.qa['loader_preservation_report'][field] = False
                with self.assertRaises(RuntimeError): self.validate()
    def test_retained_loader_evidence_version_rejected(self):
        self.retained['source_swiftkey_version'] = p.VERSION
        with self.assertRaises(RuntimeError): self.validate()
    def test_retained_loader_source_qa_digest_rejected(self):
        self.retained['source_qa_sha256'] = '0' * 64
        with self.assertRaises(RuntimeError): self.validate()
    def test_self_consistent_modified_retained_reports_rejected(self):
        self.retained['loader_build_report']['native_report']['assertions'] += 1
        self.retained['native_space_report'] = copy.deepcopy(self.retained['loader_build_report']['native_report'])
        with self.assertRaises(RuntimeError): self.validate()
    def test_route_must_not_claim_baseline_failure(self):
        self.qa['native_route_report']['baseline_failure_reproduced_by_this_test'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_route_evidence_rejected(self):
        del self.qa['native_route_report']
        with self.assertRaises(RuntimeError): self.validate()
    def test_context_baseline_must_not_claim_engine_execution(self):
        self.qa['runtime_report']['host_report']['baseline_regressions']['language_model_executed'] = True
        with self.assertRaises(RuntimeError): self.validate()
    def test_context_baseline_regression_count_must_match(self):
        self.qa['runtime_report']['host_report']['baseline_regressions']['baseline_failures_reproduced'] = 0
        with self.assertRaises(RuntimeError): self.validate()
    def test_wrong_regression_scope_rejected(self):
        self.qa['runtime_report']['host_report']['baseline_regressions']['scope'] = 'immutable_native_prediction_separator'
        with self.assertRaises(RuntimeError): self.validate()
    def test_unrelated_android_loader_change_rejected(self):
        self.retained['loader_build_report']['changed_android_classes'].append('Lapp/hiro/swiftkey/patches/ShiftLockPatch;')
        with self.assertRaises(RuntimeError): self.validate()
    def test_missing_native_separator_hook_rejected(self):
        self.retained['loader_build_report']['native_report']['separator_getters_rewritten'] = 6
        with self.assertRaises(RuntimeError): self.validate()
    def test_unpreserved_native_fallback_rejected(self):
        self.retained['loader_build_report']['native_report']['original_fallback_preserved'] = False
        with self.assertRaises(RuntimeError): self.validate()
    def test_native_space_probe_must_match_loader_build(self):
        self.retained['native_space_report']['separator_getters_rewritten'] = 999
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
        del self.retained['loader_build_report']['native_report']['morphe_runtime_tests']['morphe-current.jar']
        self.retained['native_space_report'] = copy.deepcopy(self.retained['loader_build_report']['native_report'])
        with self.assertRaises(RuntimeError): self.validate()
    def test_disagreeing_native_sdk_probe_rejected(self):
        self.retained['loader_build_report']['native_report']['morphe_runtime_tests']['morphe-current.jar']['separator_getters_rewritten'] = 6
        self.retained['native_space_report'] = copy.deepcopy(self.retained['loader_build_report']['native_report'])
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

