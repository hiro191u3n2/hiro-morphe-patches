#!/usr/bin/env python3
"""Guard the variable evidence schema and CI report boundaries before publishing."""
import copy
import hashlib
import json
import tempfile
import zipfile
from pathlib import Path
import unittest
from unittest.mock import patch
import publish19125 as p
import ci19125 as ci
import source_delta19125 as delta


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
        variables = {'work': '/tmp/ulike19125-ci-tests'}
        valid = {'name': {'command': ['python3', 'unused.py'], 'report': '{work}/host/result.json'}}
        self.assertEqual(ci.suite_paths(valid, variables)['name']['report'],
                         Path('/tmp/ulike19125-ci-tests/host/result.json'))
        for report in ('{work}/../source/result.json', '/tmp/elsewhere/result.json'):
            invalid = copy.deepcopy(valid); invalid['name']['report'] = report
            with self.subTest(report=report), self.assertRaises(RuntimeError):
                ci.suite_paths(invalid, variables)


class PublicationBoundaries(unittest.TestCase):
    def state(self):
        return {'manifest': {'version': p.PREVIOUS, 'download_url': p.BASE_URL,
                'created_at': '2026-11-01T00:00:00',
                'description': 'Existing introduction\nULike：v1.9.124（5.6.2／740）\nOther：v7.8.9\n'},
                'log': '# [1.0.337](https://example.invalid/old) (2026-11-01)\n\n'
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
        new_entry = history[:-len(original['log'])]
        self.assertIn('HDプレビュー早期解除', new_entry)
        self.assertNotIn('素材が未展開のため適用に失敗', new_entry)
        self.assertTrue(feed['description'].endswith(
            original['manifest']['description'].replace('v1.9.124', 'v1.9.125')))
        self.assertEqual(feed['version'], '1.0.338')
        self.assertTrue(p.has_changes_for(history, '1.0.337', ['ULike']))
        self.assertFalse(p.has_changes_for(history, '1.0.338', ['ULike']))
        self.assertFalse(p.has_changes_for(history, '1.0.337', ['Other']))

    def test_advanced_feed_or_missing_inventory_rejected(self):
        for field, value in (('version', '1.0.338'), ('download_url', 'https://example.invalid/changed'),
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
        report.update(resource_extraction={'status': 'passed', 'decode_mode': 'on_request', 'target_patcher_version': '2.2.0-dev.2',
                      'embedded_patcher_version': '2.2.0-dev.2', 'original_asset_count': 217, 'unchanged_original_asset_count': 214,
                      'asset_inventory_preserved': True, 'original_asset_hash_guards_preserved': True,
                      'patched_assets': {n: {'before_sha256': 'a'*64, 'after_sha256': 'b'*64, 'bytes': 10}
                        for n in ('assets/81001.zip', 'assets/81002.zip', 'assets/90000.zip')}},
                      validator_morphe_jar={'sha256': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c', 'embedded_patcher_version': '1.14.0'},
                      morphe_jar={'sha256': 'ed8184451a88b373ab38e575884523a709706195a7232e971fcf8cd61fc5a22c'})
        p.verify_application(report, 'a' * 64, 'test')
        for field, value in [('status', 'failed'), ('apk_sha256', 'c' * 64),
                             ('one_callsite_suppressed', False), ('vendor_boost_manager_preserved', False),
                             ('actual_shutter_inverse_matches_122', False), ('original_input_contract_preserved', False)]:
            bad = copy.deepcopy(report); bad['stock_boost_applied'][field] = value
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                p.verify_application(bad, 'a' * 64, 'test')
        for field, value in [('decode_mode', 'legacy_full'), ('target_patcher_version', '1.8.0'),
                             ('embedded_patcher_version', '1.14.0'), ('original_asset_count', 216),
                             ('unchanged_original_asset_count', 213), ('asset_inventory_preserved', False),
                             ('original_asset_hash_guards_preserved', False), ('patched_assets', {})]:
            bad = copy.deepcopy(report); bad['resource_extraction'][field] = value
            with self.subTest(resource_field=field), self.assertRaises(RuntimeError):
                p.verify_application(bad, 'a' * 64, 'test')
        bad = copy.deepcopy(report); del bad['stock_boost_applied']
        with self.assertRaises(RuntimeError):
            p.verify_application(bad, 'a' * 64, 'test')

    def test_reproduction_requires_actual_new_combined_failure_and_existing_asset(self):
        report = {'status': 'expected_failure_reproduced', 'physical_android_tested': False,
            'input': {'input_sha256': '73c6d3a3008b9975645f63238f07dc9c1960ad982c70f141ee4b5dfe60f7a293',
                'base_apk_assets': {'assets/81001.zip': {'sha256': 'f2517f8556bdf241caaa6bc3d9838cc3d306ad842b1b094ba93e9e33843eb125'}}},
            'old_desktop': {'embedded_patcher': '1.14.0'},
            'new_desktop': {'embedded_patcher': '2.2.0-dev.2', 'sha256': 'ed8184451a88b373ab38e575884523a709706195a7232e971fcf8cd61fc5a22c'},
            'combined': {'sha256': '8305a6cdfd3d4bec658cc6c41e0e595e4b906a2e276631677a5a7a39ea414db2',
                'target_patcher': '2.2.0-dev.2', 'decode_mode': 'on_request', 'patch_result': 'failed',
                'failure': '対象素材がありません: assets/81001.zip'},
            'standalone': {'sha256': 'c1cdab6d613ba594d8fe817799e5e208e25e847d2d55f72525f13f19e3b1b408', 'target_patcher': '1.8.0',
                'decode_mode': 'legacy_all_resources', 'patch_result': 'success'}}
        p.verify_reproduction(report)
        for section, field, value in [('new_desktop', 'embedded_patcher', '1.14.0'),
                 ('combined', 'target_patcher', '1.8.0'), ('combined', 'decode_mode', 'legacy_all_resources'),
                 ('combined', 'patch_result', 'success'), ('combined', 'failure', 'different'),
                 ('standalone', 'patch_result', 'failed'), ('input', 'base_apk_assets', {})]:
            bad = copy.deepcopy(report); bad[section][field] = value
            with self.subTest(section=section, field=field), self.assertRaises(RuntimeError):
                p.verify_reproduction(bad)

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

    def test_stock_table_must_remain_byte_identical(self):
        before = self.static_assets()['ulike/methods.tsv']
        p.verify_stock_table(before, before)
        bad_cases = [before.replace(b'b' * 64, b'c' * 64), before.replace(b'a' * 64, b'd' * 64),
                     before.replace(b'X(IZ)Z', b'Y(IZ)Z'), before + before, b'']
        for bad in bad_cases:
            with self.subTest(bad=bad), self.assertRaises(RuntimeError):
                p.verify_stock_table(before, bad)

    def test_resource_inverse_and_manifest_lookup_required(self):
        proof = {'status': 'passed', 'read_requests_enabled': 3, 'manifest_parent_lookup_preserved': True,
                 'dex_inverse_verified': True, 'guard_instructions_preserved': True, 'jvm_inverse_verified': True}
        p.verify_resource_proof(proof)
        for field in proof:
            bad = dict(proof); del bad[field]
            with self.subTest(field=field), self.assertRaises(RuntimeError):
                p.verify_resource_proof(bad)
        dex_only = dict(proof); del dex_only['jvm_inverse_verified']
        p.verify_resource_proof(dex_only, jvm=False)


class DeltaReconstruction(unittest.TestCase):
    def fixture(self, directory):
        root = Path(directory)
        before = {'keep.txt': b'keep', 'change.txt': b'before', 'remove.txt': b'remove'}
        manifest = {'version': '1.9.124', 'files': {n: delta.row(raw) for n, raw in before.items()}}
        before['manifest.json'] = json.dumps(manifest).encode()
        base = root / 'base.zip'
        with zipfile.ZipFile(base, 'w') as archive:
            for name, raw in before.items():
                archive.writestr(name, raw)
        source = root / 'source'; source.mkdir()
        final = {'keep.txt': b'keep', 'change.txt': b'after', 'new.txt': b'new'}
        manifest = {'version': '1.9.125', 'files': {n: delta.row(raw) for n, raw in final.items()}}
        final['manifest.json'] = json.dumps(manifest).encode()
        for name, raw in final.items():
            (source / name).write_bytes(raw)
        return root, source, base, final

    def test_exact_base_plus_delta_reconstructs_full_tree(self):
        with tempfile.TemporaryDirectory() as directory:
            root, source, base, expected = self.fixture(directory)
            with patch.object(delta, 'BASE_SHA', hashlib.sha256(base.read_bytes()).hexdigest()), \
                 patch.object(delta, 'BASE_BYTES', base.stat().st_size):
                cfg = delta.create(source, root / 'release', base)
                self.assertEqual(cfg['removed'], ['remove.txt'])
                delta.hydrate(root / 'release', root / 'reconstructed', base)
                self.assertEqual(delta.local_entries(root / 'reconstructed'), expected)
                self.assertNotIn('keep.txt', cfg['overlay'])

    def test_corrupted_base_overlay_and_final_closure_are_rejected(self):
        mutations = ('base', 'overlay', 'unexpected-overlay', 'final-pin', 'unsafe-delete', 'unknown-base')
        for mutation in mutations:
            with self.subTest(mutation=mutation), tempfile.TemporaryDirectory() as directory:
                root, source, base, expected = self.fixture(directory)
                with patch.object(delta, 'BASE_SHA', hashlib.sha256(base.read_bytes()).hexdigest()), \
                     patch.object(delta, 'BASE_BYTES', base.stat().st_size):
                    release = root / 'release'; cfg = delta.create(source, release, base)
                    if mutation == 'base':
                        base.write_bytes(base.read_bytes() + b'changed')
                    elif mutation == 'overlay':
                        (release / delta.OVERLAY / 'new.txt').write_bytes(b'changed')
                    elif mutation == 'unexpected-overlay':
                        (release / delta.OVERLAY / 'extra.txt').write_bytes(b'extra')
                    elif mutation == 'final-pin':
                        cfg['final_files']['keep.txt']['sha256'] = '0' * 64
                    elif mutation == 'unsafe-delete':
                        cfg['removed'].append('../outside')
                    else:
                        cfg['base']['url'] = 'https://example.invalid/unreviewed.zip'
                    (release / delta.CONFIG).write_text(json.dumps(cfg))
                    with self.assertRaises(RuntimeError):
                        delta.hydrate(release, root / 'reconstructed', base)
                    self.assertFalse((root / 'reconstructed').exists())


class BuildPreservation(unittest.TestCase):
    def report(self):
        flags = ('deterministic_dex_verified', 'deterministic_archive_verified',
                 'native_assets_byte_identical', 'stock_methods_and_contracts_byte_identical',
                 'stock_method_original_contracts_preserved', 'stock_boost_suppression_inherited',
                 'asset_contracts_byte_identical', 'resource_extraction_fix_inherited',
                 'numerical_helpers_outside_reviewed_scope_byte_identical', 'installer_byte_identical',
                 'native_installer_dex_semantics_identical', 'helper_references_verified')
        inventory = ('unrelated_runtime_classes_identical', 'existing_method_abi_preserved',
                     'existing_anonymous_identity_preserved', 'native_declarations_unchanged',
                     'executable_loader_and_installer_preserved', 'timing_camera_hooks_preserved',
                     'stock_methods_and_contracts_byte_identical', 'resource_extraction_loader_preserved')
        return {'schema': 'ulike19125-build-evidence-v1', 'version': p.VERSION,
                'baseline': {'sha256': p.SINGLE_BASE_SHA256}, 'native_payload_count': 14,
                'resource_delta': ['META-INF/MANIFEST.MF', 'classes.dex', 'ulike/runtime.dex',
                                   'app/hiro/ulike/patches/UlikeHqMaxPatch.class'],
                'inventory': {name: True for name in inventory}, **{name: True for name in flags}}

    def test_current_proofs_required_and_every_false_preservation_rejected(self):
        report = self.report(); p.verify_build_preservation(report)
        for name, value in report.items():
            if value is True:
                bad = copy.deepcopy(report); bad[name] = False
                with self.subTest(flag=name), self.assertRaises(RuntimeError):
                    p.verify_build_preservation(bad)
        for name in report['inventory']:
            bad = copy.deepcopy(report); bad['inventory'][name] = False
            with self.subTest(inventory=name), self.assertRaises(RuntimeError):
                p.verify_build_preservation(bad)
        for name, value in [('schema', 'historical'), ('version', '1.9.124'), ('native_payload_count', 13),
                            ('resource_delta', report['resource_delta'] + ['ulike/methods.dex'])]:
            bad = copy.deepcopy(report); bad[name] = value
            with self.subTest(field=name), self.assertRaises(RuntimeError):
                p.verify_build_preservation(bad)


if __name__ == '__main__':
    unittest.main()
