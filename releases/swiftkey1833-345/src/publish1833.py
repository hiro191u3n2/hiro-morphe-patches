#!/usr/bin/env python3
"""Publish reviewed context-sensitive conversion bytes; never overwrite an advanced feed.

Two independent builds, reviewed source/artifact pins, exact archive deltas,
Space-conversion host coverage, runtime preservation, and two real Morphe loaders gate publication.
The original APK and handset have not been tested. Remote mutation is disabled
by --local-only and --preflight-only; all public writes occur only after checks.
"""
from __future__ import annotations
import argparse
import datetime as dt
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import struct
import subprocess
import sys
import tempfile
import time
import zipfile
sys.dont_write_bytecode = True

REPO = 'hiro191u3n2/hiro-morphe-patches'
VERSION, PREVIOUS, BUNDLE = '1.8.33', '1.0.344', '1.0.345'
SINGLE_PREVIOUS = '1.8.32'
TAG, RAW_BRANCH = 'swiftkey-v1.8.33', 'release/swiftkey1833-345'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.33.mpp'
COMBINED = 'Hiro_Morphe_Patches_v1.0.345.mpp'
BASE_NAME = 'Hiro_Morphe_Patches_v1.0.344.mpp'
BASE_SHA256 = '6853b42cb05394db077140f4664b65feb35ca3b80cb3e75fa815613db152cdcb'
BASE_URL = f'https://raw.githubusercontent.com/{REPO}/c7cc4f193d27e20f2feeaa65ed6ef741ad59ec64/downloads/{BASE_NAME}'
SINGLE_BASE_NAME = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.32.mpp'
SINGLE_BASE_SHA256 = '03fca4b37efe73c24529d1db7422fbf68d9867603be2aff1228af728be03cc67'
SINGLE_BASE_URL = f'https://raw.githubusercontent.com/{REPO}/c7cc4f193d27e20f2feeaa65ed6ef741ad59ec64/downloads/{SINGLE_BASE_NAME}'
BASE_RUNTIME_SHA256 = '377de86d964dea1f66515dae59b098dbdeda0cb0e5b469cd215e7c2e6aad0834'
BASE_QA_SHA256 = '3bcc65ae4ea0be96b9aecc61a2780e4d07cb1e1d44062211a86871ac724238fc'
RETAINED_LOADER_REPORT_SHA256 = '549064b0b96056c0c68fc28c40c8e2f603f8d032a9c863c75244dc4b1a76334a'
RETAINED_NATIVE_REPORT_SHA256 = '6c6405102a0ca4ecc2834107eb3fe8429dbb8aef0fe546b57106f731535c5c49'
REFERENCE_APK_SHA256 = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
QA_NAME = 'QA_SwiftKey_v1.8.33.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.33_sources_and_QA.zip'
RECEIPT_NAME = 'publication_SwiftKey_v1.8.33.json'
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')
RELEASE_ROOT = 'releases/swiftkey1833-345'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
MF, EXTENSION = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
EMOJI = 'swiftkey/emoji_ja.tsv'
LOADER_CLASS = 'app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
SPACE_LOADER_CLASS = 'app/hiro/swiftkey/patches/SpaceConversionPatch.class'
SPACE_LOADER_TYPE = 'Lapp/hiro/swiftkey/patches/SpaceConversionPatch;'
RUNTIME_CHANGED_CLASSES = ['Lhiro/swiftkey/FrequencyLearning;', 'Lhiro/swiftkey/SpaceConversion;']
RANK_METHOD = 'Lhiro/swiftkey/FrequencyLearning;->rank(Ljava/util/List;Ll40/c;Z)Ljava/util/List;'
REQUIRED_CHANGED = {MF, EXTENSION}
SHIFT_CLASS = 'app/hiro/swiftkey/patches/ShiftLockPatch.class'
REQUIRED_ADDED = set()
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = frozenset(JVM_PREFIX + name + '.class' for name in (
    'JapaneseSamsungEmojiPatch', 'MethodContract', 'InputLearningPatch',
    'DictionarySupplementPatch', 'KeySwapPatch', 'PasteProtectionPatch',
    'JapaneseGlidePatch', 'DeleteProtectionPatch', 'CandidateScrollPatch', 'ShiftLockPatch',
    'CandidateToolbarPatch', 'PunctuationPatch', 'JapaneseStartPatch', 'EmojiFooterPatch', 'SpaceConversionPatch'))
REQUIRED_QA = {
    'status': 'CONTEXT_SELECTION_HOST_VERIFIED_DEVICE_UNVERIFIED',
    'swiftkey_version': VERSION, 'bundle_version': BUNDLE,
    'baseline_swiftkey_version': SINGLE_PREVIOUS, 'baseline_bundle_version': PREVIOUS,
    'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4',
    'target_version_code': 1236271168, 'device_tested': False,
    'original_apk_apply_tested': False, 'other_apps_byte_identical': True,
    'unrelated_loader_entries_byte_identical': True, 'standalone_and_bundle_runtime_equal': True,
    'host_tests_passed': True, 'runtime_byte_identical': False, 'two_loader_sdks_agree': True,
    'all_dictionary_resources_preserved': True, 'shift_helper_byte_identical': True,
    'all_loaders_byte_identical': True, 'ranking_runtime_preserved': False,
}


APP_NAMES = ('Microsoft SwiftKey Beta', 'SwiftKey Beta')
OTHER_APPS = ('ULike', 'Berry Browser', 'Instagram', 'X', 'Trip.com', 'TikTok',
              'Yahoo!乗換案内', 'Amazonショッピング', 'Hanull Reader', 'Uber Eats', 'BrightnessClick')
HELPER = 'releases/ulike-rollback188-142/rollback142.py'
HELPER_BLOB_SHA = 'e7771a4868d7a798217304af6d6099bcded5569e'
MANAGER_SOURCE = ('https://github.com/MorpheApp/morphe-manager/blob/'
                  '0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/'
                  'app/src/main/java/app/morphe/manager/util/ChangelogParser.kt')
HEADING = re.compile(r'^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)', re.I)
SCOPE = re.compile(r'^\* \*\*(.+?):\*\*')
EXPERIMENTAL_ONLY = re.compile(r'^Add(?:ed)?\s+experimental\s+support\s+for\b', re.I)
INVENTORY = re.compile(r'(?m)^((?:Microsoft )?SwiftKey Beta：)v(1\.8\.32)(（9\.13\.16\.4(?:／1236271168)?）)\r?$')
SUMMARY = '標準予測の文脈順位を保ち、学習候補は未掲載の語だけ後ろへ追加します。スペースで標準のかな候補も選べるようにし、候補本体を変更しない確定処理・辞書・上フリック・Shift大文字維持・絵文字を保持。実機未確認。'


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()


def checked_path(name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and ".." not in p.parts and "\\" not in name,
            "Unsafe archive/repository path: " + name)
    return p


def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), "Duplicate archive paths")
        for name in names:
            checked_path(name)
        require(z.testzip() is None, "Corrupt ZIP")
        return {name: z.read(name) for name in names if not name.endswith("/")}


def headers(raw):
    rows = []
    for line in raw.replace(b"\r\n", b"\n").split(b"\n"):
        if line.startswith(b" "):
            require(bool(rows), "Manifest continuation without a header")
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(": ", 1) for line in rows]
    require(all(len(pair) == 2 for pair in pairs), "Malformed MPP manifest")
    require(len(pairs) == len(dict(pairs)), "Duplicate MPP manifest key")
    return dict(pairs)


def version_tuple(value):
    require(re.fullmatch(r"v?\d+\.\d+\.\d+", value) is not None,
            "Expected stable three-part bundle version")
    return tuple(map(int, value.removeprefix("v").split(".")))


def parsed_entries(markdown):
    """Only the heading/scope subset used by this stable release is needed."""
    entries, current = [], None
    for line in markdown.splitlines():
        m = HEADING.match(line)
        if m:
            current = {"version": m[1] or m[2], "date": m[3], "bullets": []}
            entries.append(current)
        elif current is not None:
            s = SCOPE.match(line.strip())
            if s:
                body = line.strip()[s.end():].strip()
                current["bullets"].append((s[1], body))
    return entries


def has_changes_for(markdown, installed, app_names):
    """Mirror Manager's scope matching for the stable versions used here."""
    entries = parsed_entries(markdown)
    old_date = next((e["date"] for e in entries if e["version"].removeprefix("v") == installed), None)
    for entry in entries:
        if not re.fullmatch(r"v?\d+\.\d+\.\d+", entry["version"]):
            continue
        if version_tuple(entry["version"]) <= version_tuple(installed):
            continue
        if old_date and entry["date"] < old_date:
            continue
        for scope, body in entry["bullets"]:
            if EXPERIMENTAL_ONLY.match(body):
                continue
            for app in app_names:
                if scope.casefold() == app.casefold() or scope.casefold().startswith(app.casefold() + " - "):
                    return True
    return False


def metadata(state, url, created, notes):
    old = state["manifest"]
    require(old["version"] == PREVIOUS and old["download_url"] == BASE_URL, "Another bundle is active")
    require(dt.datetime.fromisoformat(created) > dt.datetime.fromisoformat(old["created_at"]),
            "created_at must advance")
    matches = list(INVENTORY.finditer(old["description"]))
    require(len(matches) == 1, "Missing or ambiguous current SwiftKey inventory")
    previous_description = INVENTORY.sub(lambda m: m[1] + "v" + VERSION + m[3], old["description"], count=1)
    description = notes.strip() + "\n\n" + previous_description
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": description}
    prefix = (f"# {BUNDLE} ({created[:10]})\n\n"
              f"* **Microsoft SwiftKey Beta:** v{VERSION}：{SUMMARY}\n"
              f"* **SwiftKey Beta:** v{VERSION}：{SUMMARY}\n\n"
              + notes.strip() + "\n\n")
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Release notes contain a competing version heading")
    changelog = prefix + state["log"]
    for app in APP_NAMES:
        require(has_changes_for(changelog, PREVIOUS, [app]), "Missing update scope for " + app)
        require(not has_changes_for(changelog, BUNDLE, [app]), "Freshly patched SwiftKey would still be marked outdated")
    require(not any(has_changes_for(changelog, PREVIOUS, [app]) for app in OTHER_APPS),
            "False update scope for another app")
    new_entry = parsed_entries(changelog)[0]
    require(new_entry["version"] == BUNDLE and {scope for scope, _ in new_entry["bullets"]} == set(APP_NAMES),
            "New changelog entry has unrelated or missing app scopes")
    return feed, changelog



def exact(actual, expected, message):
    require(type(actual) is type(expected) and actual == expected, message)


def load_expected(path):
    data = json.loads(path.read_text())
    for name, value in {
        'schema': 'swiftkey1833-publication-v1', 'swiftkey_version': VERSION,
        'bundle_version': BUNDLE, 'baseline_bundle_version': PREVIOUS,
        'baseline_bundle_sha256': BASE_SHA256, 'baseline_swiftkey_version': SINGLE_PREVIOUS,
        'baseline_swiftkey_sha256': SINGLE_BASE_SHA256, 'change_plan_reviewed': True,
        'qa_contract_reviewed': True, 'android_device_tested': False,
        'original_apk_apply_tested': False,
    }.items():
        exact(data.get(name), value, 'Expected manifest contract differs: ' + name)
    require(set(data.get('artifacts', {})) == set(ASSETS), 'Manifest must pin all six release assets')
    for name, row in data['artifacts'].items():
        require(type(row.get('bytes')) is int and row['bytes'] > 0, 'Invalid artifact size: ' + name)
        require(re.fullmatch(r'[0-9a-f]{64}', row.get('sha256', '')) is not None
                and row['sha256'] != '0' * 64, 'Invalid artifact digest: ' + name)
    for kind in ('bundle', 'standalone'):
        exact(sorted(data.get('allowed_changed_' + kind + '_entries', [])), sorted(REQUIRED_CHANGED),
              'Only manifest and context-selection runtime may change: ' + kind)
        exact(sorted(data.get('allowed_added_' + kind + '_entries', [])), sorted(REQUIRED_ADDED), 'No loader entry may be added: ' + kind)
    for key, value in REQUIRED_QA.items():
        exact(data.get('qa_required_values', {}).get(key), value, 'Missing reviewed QA requirement: ' + key)
    return data


def verify_source_files(source_dir, expected):
    pins = expected.get('source_files')
    required = {'build1833.py', 'publish1833.py', 'publisher_tests.py', 'workflow.yml'}
    require(isinstance(pins, dict) and required.issubset(pins), 'Incomplete reviewed source pins')
    root = source_dir.resolve()
    for name, row in pins.items():
        checked_path(name)
        path = root / name
        require(path.is_file() and not path.is_symlink() and path.resolve().is_relative_to(root), 'Unsafe/missing source: ' + name)
        raw = path.read_bytes()
        require(row.get('bytes') == len(raw) and row.get('sha256') == sha(raw), 'Reviewed source changed: ' + name)
    for path in root.rglob('*'):
        if path.is_file() and '__pycache__' not in path.relative_to(root).parts:
            require(path.relative_to(root).as_posix() in pins, 'Unpinned build/source input: ' + str(path.relative_to(root)))
    return len(pins)


def lookup_qa(data, dotted_path):
    for key in dotted_path.split('.'):
        require(isinstance(data, dict) and key in data, 'Missing QA value: ' + dotted_path)
        data = data[key]
    return data


def validate_archive_delta(old, new, old_version, new_version, kind):
    require(set(new) == set(old) | REQUIRED_ADDED, kind + ' archive entry inventory changed')
    changed = {name for name in old if old[name] != new[name]}
    require(changed == REQUIRED_CHANGED, kind + ' unexpected changed entries: ' + repr(sorted(changed)))
    before, after = headers(old[MF]), headers(new[MF])
    require(before.get('Version') == old_version and after.get('Version') == new_version, kind + ' wrong versions')
    require(set(before) == set(after), kind + ' manifest keys changed')
    require({k: v for k, v in before.items() if k not in {'Version', 'Timestamp', 'Description'}}
            == {k: v for k, v in after.items() if k not in {'Version', 'Timestamp', 'Description'}},
            kind + ' unrelated manifest field changed')
    require(new.get('classes.dex', b'').startswith(b'dex\n') and new[EXTENSION].startswith(b'dex\n'), kind + ' missing DEX')
    require(struct.unpack_from('<I', new[EXTENSION], 96)[0] == 92, kind + ' runtime conversion helper missing')
    return sorted(changed)


def validate_evidence(qa, expected, runtime_sha, dictionary_sha):
    for path, value in expected['qa_required_values'].items():
        exact(lookup_qa(qa, path), value, 'QA assertion failed: ' + path)
    for path, value in REQUIRED_QA.items():
        exact(lookup_qa(qa, path), value, 'Hardcoded QA contract failed: ' + path)
    exact(qa.get('source_sha256'), {name: row['sha256'] for name, row in expected.get('source_files', {}).items()},
          'QA build source map differs from reviewed source pins')
    exact(qa.get('runtime_sha256'), runtime_sha, 'Evidence targets different runtime')
    host = qa.get('host_report', {})
    exact(host.get('status'), 'PASSED', 'Host regression suite did not pass')
    exact(host.get('runtime_sha256'), runtime_sha, 'Host report targets different runtime')
    runtime = qa.get('runtime_report', {})
    for key, value in {'status': 'PASSED', 'runtime_sha256': runtime_sha,
                       'baseline_runtime_sha256': BASE_RUNTIME_SHA256,
                       'all_other_classes_preserved': True}.items():
        exact(runtime.get(key), value, 'Runtime preservation failed: ' + key)
    exact(runtime.get('changed_classes'), RUNTIME_CHANGED_CLASSES,
          'Only SpaceConversion and the FrequencyLearning.rank donor class may change')
    merge = runtime.get('merge', {})
    for key, value in {'status': 'PASSED', 'extension_sha256': runtime_sha,
                       'baseline_extension_sha256': BASE_RUNTIME_SHA256,
                       'changed_existing_classes': RUNTIME_CHANGED_CLASSES,
                       'added_classes': [], 'runtime_before_classes': 92, 'runtime_after_classes': 92,
                       'rank_method_only_replacement': True, 'rank_method_descriptor': RANK_METHOD,
                       'frequency_other_members_preserved': True, 'frequency_nested_classes_preserved': True,
                       'inverse_rank_restores_original_class': True,
                       'compiled_rank_body_preserved': True}.items():
        exact(merge.get(key), value, 'Runtime method graft evidence differs: ' + key)
    ranking = runtime.get('ranking_report', {})
    for key, value in {'status': 'PASSED', 'baseline_runtime_sha256': BASE_RUNTIME_SHA256,
                       'baseline_rank_matches_published': True, 'baseline_java_matches_published_rank': True,
                       'bridge_java_matches_published_rank': True,
                       'baseline_rank_sha256': 'a059975c9295141ff8f7b8000fdb81cf3ee3246cdd2a5077605fe4d1166bb585',
                       'fixture_count': 7, 'baseline_context_order_regressions': 6,
                       'fixed_context_order_regressions': 0,
                       'preserves_current_order_then_appends_unseen_history': True,
                       'blocked_and_revoked_session_boundaries_tested': True,
                       'native_preferences_are_modeled_inputs': True,
                       'store_and_cleanup_are_models_bound_to_published_method_fixtures': True,
                       'merge_must_preserve_all_other_methods_and_classes': True,
                       'android_device_tested': False}.items():
        exact(ranking.get(key), value, 'Ranking verification differs: ' + key)
    for field in ('fixed_rank_sha256', 'fixed_dex_sha256'):
        value = ranking.get(field)
        require(isinstance(value, str) and re.fullmatch(r'[0-9a-f]{64}', value) is not None
                and value != '0' * 64, 'Invalid ranking output hash: ' + field)
    require(ranking['fixed_rank_sha256'] != ranking['baseline_rank_sha256'], 'Ranking method update missing')
    exact(merge.get('rank_overlay_sha256'), ranking['fixed_dex_sha256'], 'Ranking tests target a different graft donor')
    ranking_fixture_pin = expected.get('source_files', {}).get('ranking/fixtures/manifest.json', {}).get('sha256')
    require(isinstance(ranking_fixture_pin, str) and re.fullmatch(r'[0-9a-f]{64}', ranking_fixture_pin) is not None,
            'Reviewed ranking fixture manifest pin missing')
    exact(ranking.get('fixture_manifest_sha256'), ranking_fixture_pin, 'Ranking fixture manifest differs from reviewed source')
    for key, mode, regressions, assertions in (('baseline_host', 'baseline', 6, 54), ('fixed_host', 'fixed', 0, 61)):
        for field, value in {'status': 'PASSED', 'mode': mode, 'context_order_regressions': regressions, 'assertions': assertions,
                             'native_preferences_are_modeled_inputs': True, 'device_tested': False}.items():
            exact(ranking.get(key, {}).get(field), value, 'Ranking host report differs: ' + key + '/' + field)
    exact(runtime.get('added_classes'), [], 'Runtime class inventory must remain unchanged')
    exact(runtime.get('host_tests_passed'), True, 'Runtime host tests did not pass')
    exact(runtime.get('baseline_java_matches_published_dex'), True, 'Restored baseline source differs')
    exact(runtime.get('baseline_java_scope'), ['SpaceConversion entire class', 'FrequencyLearning.rank method'],
          'Restored baseline proof has the wrong scope')
    exact(runtime.get('native_abi_verified'), True, 'Native runtime ABI not verified')
    abi = runtime.get('native_abi_report', {})
    for key, value in {'status': 'PASSED', 'extension_sha256': runtime_sha,
                       'apk_sha256': REFERENCE_APK_SHA256, 'audited_classes': RUNTIME_CHANGED_CLASSES, 'unresolved': [],
                       'inaccessible': [], 'incompatible_opcodes': []}.items():
        exact(abi.get(key), value, 'Runtime ABI evidence differs: ' + key)
    audited_methods = abi.get('audited_methods', [])
    require(isinstance(audited_methods, list) and RANK_METHOD in audited_methods, 'Native ABI does not cover FrequencyLearning.rank')
    exact(runtime.get('host_report', {}).get('status'), 'PASSED',
          'Missing space behavior suite')
    for key, value in {'assertions': 354, 'candidate_mutation_removed': True,
                       'identity_scoped_separator_reads': True, 'immutable_native_cases': 8}.items():
        exact(runtime.get('host_report', {}).get(key), value, 'Native separator behavior suite differs: ' + key)
    context = runtime.get('host_report', {}).get('context_selection', {})
    for key, value in {'status': 'PASSED', 'assertions': 152, 'original_objects_preserved': True,
                       'synthetic_candidates_excluded': True, 'language_model_executed': False,
                       'device_tested': False}.items():
        exact(context.get(key), value, 'Context selection behavior evidence differs: ' + key)
    regression = runtime.get('host_report', {}).get('baseline_regressions', {})
    for key, value in {'status': 'PASSED', 'assertions': 20,
                       'scope': 'kana_prediction_skipped_by_kanji_filter',
                       'baseline_failures_reproduced': 4, 'language_model_executed': False}.items():
        exact(regression.get(key), value, 'Context selection baseline regression evidence differs: ' + key)
    cache = runtime.get('host_report', {}).get('cache_delivery_preservation', {})
    for key, value in {
            'native_fixture_sha256': '62b9518ae9a14472198f1fcf708bdbab8e72c717781f6865c6ce3d3d9f9bc7b5',
            'dictionary_row_sha256': 'd3f53de2165130bc5e533d73f77a315aeadc220e3e71430d37748ad738e07b9e',
            'native_branch_translated_not_android_execution': True}.items():
        exact(cache.get(key), value, 'Cache preservation evidence differs: ' + key)
    for mode in ('baseline', 'fixed'):
        for key, value in {'status': 'PASSED', 'mode': mode, 'checks': 13,
                           'actual_production_space_handler': True,
                           'actual_production_word_supplement': True,
                           'cache_hit_visible_candidate_case_verified': True,
                           'native_cache_branch_translated': True, 'visible_kanji': '愛',
                           'device_tested': False}.items():
            exact(cache.get(mode, {}).get(key), value, 'Cache delivery regression differs: ' + mode + '/' + key)

    raw_evidence = runtime.get('host_report', {}).get('native_raw_evidence', {})
    for key, value in {'status': 'PASSED', 'reference_apk_sha256': REFERENCE_APK_SHA256,
                       'native_state_and_request_share_marker_raw': True,
                       'native_raw_append_has_no_script_filter': True,
                       'native_romaji_call_has_no_all_latin_precondition': True,
                       'native_correction_text_can_be_reused_for_editing': True,
                       'android_code_executed': False, 'device_tested': False,
                       'user_video_raw_input_observed': False}.items():
        exact(raw_evidence.get(key), value, 'Native raw source evidence differs: ' + key)
    separator_evidence = runtime.get('host_report', {}).get('native_separator_evidence', {})
    for key, value in {'status': 'PASSED', 'reference_apk_sha256': REFERENCE_APK_SHA256,
                       'fragment_count': 29,
                       'native_fluency_candidate_setter_always_throws': True,
                       'native_prediction_factory_produces_immutable_candidates': True,
                       'native_precommit_null_separator_calls_setter': True,
                       'commit_separator_sites': 7, 'commit_separator_methods': 6,
                       'native_visitor_unwraps_candidates': True,
                       'native_context_guard_checks_K_against_request_i': True,
                       'wrapper_chain_public_field_count': 9, 'email_wrapper_has_public_accessor': True,
                       'android_code_executed': False, 'device_tested': False,
                       'user_device_candidate_type_observed': False}.items():
        exact(separator_evidence.get(key), value, 'Native separator source evidence differs: ' + key)
    metadata = runtime.get('native_metadata_evidence', {})
    for key, value in {'status': 'PASSED', 'reference_apk_sha256': REFERENCE_APK_SHA256,
                       'fixture_count': 13, 'prefix_flag_requires_separate_exclusion': True,
                       'default_metadata_is_not_contextual_proof': True,
                       'k0_is_native_true_verbatim_wrapper': True,
                       'g0_forwards_native_metadata_flags': True,
                       'runtime_source_bound_not_executed_by_this_checker': True,
                       'decoded_reference_rechecked': False,
                       'device_tested': False, 'original_apk_apply_tested': False}.items():
        exact(metadata.get(key), value, 'Native metadata evidence differs: ' + key)
    source_pins = expected.get('source_files', {})
    source_hash = source_pins.get('runtime/src/hiro/swiftkey/SpaceConversion.java', {}).get('sha256')
    require(isinstance(source_hash, str) and re.fullmatch(r'[0-9a-f]{64}', source_hash) is not None,
            'Native metadata requires the reviewed production SpaceConversion source pin')
    exact(metadata.get('runtime_source_sha256'), source_hash, 'Native metadata targets a different runtime source')
    metadata_prefix = 'runtime/tests/native_metadata_review/'
    fixture_pins = {name[len(metadata_prefix):]: pin['sha256'] for name, pin in source_pins.items()
                    if name.startswith(metadata_prefix) and name.endswith('.smali')}
    exact(len(fixture_pins), 13, 'Reviewed native metadata fixtures missing')
    exact(metadata.get('fixture_sha256'), fixture_pins, 'Native metadata evidence differs from reviewed fixtures')
    truth_table = [{'prefix': prefix, 'verbatim': verbatim, 'native_D': prefix,
                    'native_Q': not prefix and verbatim}
                   for prefix in (False, True) for verbatim in (False, True)]
    exact(metadata.get('native_truth_table'), truth_table, 'Native metadata truth table changed')
    route = qa.get('native_route_report', {})
    for key, value in {'status': 'PASSED', 'apk_sha256': REFERENCE_APK_SHA256,
                       'existing_hook_is_on_space_route': True,
                       'baseline_failure_reproduced_by_this_test': False,
                       'device_tested': False, 'full_apk_runtime_execution': False}.items():
        exact(route.get(key), value, 'Native route evidence differs: ' + key)
    preservation = qa.get('loader_preservation_report', {})
    for key, value in {'status': 'PASSED', 'baseline_swiftkey_version': SINGLE_PREVIOUS,
                       'all_jvm_loader_bytes_preserved': True,
                       'all_android_loader_bytes_preserved': True, 'loader_compiled': False}.items():
        exact(preservation.get(key), value, 'Current loader preservation evidence differs: ' + key)
    retained = qa.get('retained_separator_loader_evidence', {})
    for key, value in {'source_swiftkey_version': SINGLE_PREVIOUS, 'source_bundle_version': PREVIOUS,
                       'source_qa_sha256': BASE_QA_SHA256}.items():
        exact(retained.get(key), value, 'Historical separator loader provenance differs: ' + key)
    build = retained.get('loader_build_report', {})
    retained_native = retained.get('native_space_report', {})
    for name, report, digest in (('loader', build, RETAINED_LOADER_REPORT_SHA256),
                                 ('native', retained_native, RETAINED_NATIVE_REPORT_SHA256)):
        canonical = json.dumps(report, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()
        exact(sha(canonical), digest, 'Retained 1.8.32 ' + name + ' evidence changed')
    for key, value in {'status': 'PASSED', 'unrelated_android_classes_preserved': True,
                       'unrelated_jvm_classes_preserved': True,
                       'changed_android_classes': [SPACE_LOADER_TYPE], 'loader_classes_added': 0,
                       'loader_classes_replaced': 1, 'jvm_main_and_bridges_byte_exact': True,
                       'android_main_and_bridges_canonical_preserved': True,
                       'android_serialization_canonical_preservation': True}.items():
        exact(build.get(key), value, 'Loader preservation contract failed: ' + key)
    native = build.get('native_report', {})
    exact(retained_native, native, 'Retained native space probe differs from retained loader build report')
    for key, value in {'status': 'PASSED', 'separator_getters_rewritten': 7,
                       'separator_methods_changed': 6,
                       'original_fallback_preserved': True, 'native_methods_changed': 7,
                       'native_instructions_added': 4, 'inverse_native_method_restoration': True,
                       'all_target_drift_rejected_before_mutation': True,
                       'reference_apk_sha256': REFERENCE_APK_SHA256,
                       'old_loader_separator_getters_rewritten': 0,
                       'old_and_new_sdk_apply_executed': True, 'native_fixture_dex_serialized': True,
                       'candidate_getter_bodies_unchanged': True, 'android_runtime_executed': False,
                       'device_tested': False, 'original_apk_apply_tested': False}.items():
        exact(native.get(key), value, 'Native separator hook evidence differs: ' + key)
    native_runs = native.get('morphe_runtime_tests', {})
    exact(sorted(native_runs), ['morphe-current.jar', 'morphe.jar'], 'Both native hook SDK probes required')
    exact(native_runs['morphe.jar'], native_runs['morphe-current.jar'], 'Native hook SDK probes disagree')
    for key, value in native_runs['morphe.jar'].items():
        exact(native.get(key), value, 'Merged native hook report differs from actual SDK probe: ' + key)
    exact(native.get('morphe_runtime_sha256'), {
        'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
        'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573'},
        'Native hook probe SDK pins differ')
    loaders = qa.get('loader_reports', [])
    require(isinstance(loaders, list) and len(loaders) == 2 and loaders[0] == loaders[1], 'Both real Morphe loaders must agree')
    for loader in loaders:
        for key, value in {'status': 'PASSED', 'selected_extension_stream_verified': True,
                           'selected_extension_sha256': runtime_sha, 'baseline_extension_sha256': BASE_RUNTIME_SHA256,
                           'baseline_updated_dependency_graph_equal': True,
                           'standalone_combined_dependency_graph_equal': True,
                           'clean_apk_apply_tested': False, 'device_tested': False}.items():
            exact(loader.get(key), value, 'Morphe loader gate failed: ' + key)
    return {'host_runtime_bound': True, 'runtime_and_loader_preservation_bound': True, 'two_loader_sdks_bound': True}


def validate_local(dist, second_dist, expected, baseline, standalone_baseline):
    require(sha(baseline) == BASE_SHA256 and sha(standalone_baseline) == SINGLE_BASE_SHA256, 'Baseline MPP digest differs')
    require(dist.resolve() != second_dist.resolve(), 'Two distinct build output directories are required')
    payloads = {}
    for name, row in expected['artifacts'].items():
        raw = (dist / name).read_bytes()
        require(len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Artifact differs from reviewed bytes: ' + name)
        require((second_dist / name).read_bytes() == raw, 'Independent build differs: ' + name)
        payloads[name] = raw
    old, old_single = archive(baseline), archive(standalone_baseline)
    new, single = archive(payloads[COMBINED]), archive(payloads[SINGLE])
    changed = validate_archive_delta(old, new, PREVIOUS, BUNDLE, 'bundle')
    single_changed = validate_archive_delta(old_single, single, SINGLE_PREVIOUS, VERSION, 'standalone')
    for kind, package in (('old single', old_single), ('old bundle', old), ('new single', single), ('new bundle', new)):
        expected_classes = JVM_CLASSES | (REQUIRED_ADDED if kind.startswith('new ') else set())
        require({n for n in package if n.startswith(JVM_PREFIX) and n.endswith('.class')} == expected_classes, kind + ' SwiftKey loader inventory differs')
    for name in JVM_CLASSES:
        require(old[name] == old_single[name] and new[name] == single[name], 'Standalone/bundle JVM loader differs: ' + name)
        require(old[name] == new[name], 'SwiftKey JVM loader changed: ' + name)
    require(old['classes.dex'] == new['classes.dex'], 'Combined Android loader changed')
    require(old_single['classes.dex'] == single['classes.dex'], 'Standalone Android loader changed')
    require(new[SHIFT_CLASS] == single[SHIFT_CLASS], 'Standalone/bundle Shift helper differs')
    require(old[EXTENSION] == old_single[EXTENSION] and sha(old[EXTENSION]) == BASE_RUNTIME_SHA256, 'Wrong baseline runtime')
    require(new[EXTENSION] == single[EXTENSION] and new[EXTENSION] != old[EXTENSION], 'Runtime update missing or standalone differs')
    dictionary_names = {name for name in old if name.startswith('swiftkey/')}
    require(dictionary_names == {name for name in old_single if name.startswith('swiftkey/')}, 'Baseline dictionary inventories differ')
    for name in dictionary_names:
        require(old[name] == old_single[name] and new[name] == single[name], 'Standalone and combined dictionary differ: ' + name)
        require(old[name] == new[name], 'Dictionary resource changed: ' + name)
    dictionary_report = {'sha256': sha(new[EMOJI]), 'byte_identical': True}
    runtime_sha = sha(new[EXTENSION])
    qa = json.loads(payloads[QA_NAME])
    for evidence_name, loader in (
            ('current preservation', qa.get('loader_preservation_report', {})),
            ('retained 1.8.32', qa.get('retained_separator_loader_evidence', {}).get('loader_build_report', {}))):
        exact(loader.get('helper_jvm_sha256'), sha(new[SPACE_LOADER_CLASS]), evidence_name + ' JVM helper differs from final MPP')
        exact(loader.get('single_android_sha256'), sha(single['classes.dex']), evidence_name + ' standalone Android loader differs from final MPP')
        exact(loader.get('bundle_android_sha256'), sha(new['classes.dex']), evidence_name + ' combined Android loader differs from final MPP')
    resources = qa.get('final_resource_reports', [])
    require(isinstance(resources, list) and len(resources) == 4, 'Four final package resource probes required')
    for index, (kind, sdk, name) in enumerate((('single', 'morphe.jar', SINGLE), ('single', 'morphe-current.jar', SINGLE), ('bundle', 'morphe.jar', COMBINED), ('bundle', 'morphe-current.jar', COMBINED))):
        row = resources[index]
        exact(row.get('kind'), kind, 'Final resource probe kind differs')
        exact(row.get('sdk'), sdk, 'Final resource probe SDK differs')
        for key, value in {'status': 'PASSED', 'real_loader_resource_open_read_hash_verified': True,
                           'all_eight_resource_hashes_verified': True, 'exact_192mib_accepted': True,
                           'over_192mib_rejected': True, 'mpp_sha256': sha(payloads[name]), 'device_tested': False}.items():
            exact(row.get(key), value, 'Final resource probe differs: ' + str(index) + ' / ' + key)
    probe_payloads = [{k: v for k, v in row.items() if k not in ('kind', 'sdk')} for row in resources]
    require(probe_payloads[0] == probe_payloads[1] and probe_payloads[2] == probe_payloads[3], 'SDK resource probes disagree')
    evidence = validate_evidence(qa, expected, runtime_sha, sha(new[EMOJI]))
    checksum_rows = {}
    for line in payloads['SHA256SUMS.txt'].decode().splitlines():
        if not line.strip(): continue
        match = re.fullmatch(r'([0-9a-f]{64}) [ *](.+)', line)
        require(match is not None, 'Malformed SHA256SUMS row')
        digest, name = match.groups()
        require(name in payloads and name != 'SHA256SUMS.txt' and name not in checksum_rows, 'Invalid checksum asset')
        require(sha(payloads[name]) == digest, 'Checksum mismatch: ' + name)
        checksum_rows[name] = digest
    require(set(checksum_rows) == set(ASSETS) - {'SHA256SUMS.txt'}, 'Checksums must cover all other assets')
    sources = archive(payloads[SOURCE_ZIP])
    require(set(sources) == {'src/' + name for name in expected['source_files']} | {QA_NAME}, 'Source archive inventory differs')
    require(sources[QA_NAME] == payloads[QA_NAME], 'Source archive QA differs')
    for name, row in expected['source_files'].items():
        raw = sources.get('src/' + name)
        require(raw is not None and len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Source archive differs: ' + name)
    require(not any(PurePosixPath(n).suffix.casefold() in {'.apk', '.apks', '.apkm', '.aab', '.jks', '.keystore'} for n in sources),
            'Source archive contains application or signing-key bytes')
    notes = payloads['RELEASE_NOTES.txt'].decode()
    require(all(token in notes for token in (VERSION, BUNDLE, '候補', 'スペース', '上フリック', 'Shift', '絵文字', '未確認')), 'Incomplete release notes')
    return payloads, notes, {'two_independent_builds_byte_identical': True,
        'integrated_changed_entries': changed, 'standalone_changed_entries': single_changed,
        'integrated_added_entries': sorted(REQUIRED_ADDED), 'standalone_added_entries': sorted(REQUIRED_ADDED),
        'integrated_unchanged_entries': len(old) - len(changed),
        'standalone_unchanged_entries': len(old_single) - len(single_changed),
        'all_non_target_archive_entries_byte_identical': True,
        'existing_emoji_pairs_and_order_preserved': True, 'all_dictionary_resources_preserved': True, 'emoji_dictionary': dictionary_report, 'unrelated_jvm_loaders_byte_identical': True,
        'runtime_sha256': runtime_sha, 'all_loaders_byte_identical': True, **evidence}


def load_helper(repo):
    path = repo / HELPER
    raw = path.read_bytes()
    git_sha = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    require(git_sha == HELPER_BLOB_SHA, "Pinned publication helper changed")
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location("swiftkey1833_publication_helper", path)
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    require(helper.REPO == REPO, "Wrong helper repository")
    return helper


def git(repo, *args, input=None, env=None):
    command = ["git", "-C", str(repo), "-c", "credential.helper=",
               "-c", "credential.helper=!gh auth git-credential", *map(str, args)]
    result = subprocess.run(command, input=input, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            env=env, check=False)
    require(result.returncode == 0, "Git command failed: " + " ".join(map(str, args)) + "\n" + result.stderr.decode(errors="replace"))
    return result.stdout.decode().strip()


def check_heads_git(repo, states):
    require(states and all(isinstance(s.get('branch'), str) and s.get('head') for s in states.values()), 'Invalid branch lease state')
    expected = {'refs/heads/' + s['branch']: s['head'] for s in states.values()}
    require(len(expected) == len(states), 'Duplicate branch lease')
    raw = git(repo, 'ls-remote', '--heads', f'https://github.com/{REPO}.git', *sorted(expected))
    actual = {}
    for line in raw.splitlines():
        parts = line.split()
        require(len(parts) == 2 and re.fullmatch(r'[0-9a-f]{40}', parts[0]) and parts[1] not in actual, 'Malformed remote refs')
        actual[parts[1]] = parts[0]
    require(actual == expected, 'Concurrent branch update; inspect before retrying: ' + repr(actual))


def prepare_commit(repo, state, files, message):
    """Use a private index; preserve the checkout, its index, and every other path."""
    env = {**os.environ, "GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_COMMITTER_NAME": "github-actions[bot]",
           "GIT_AUTHOR_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com",
           "GIT_COMMITTER_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com"}
    with tempfile.TemporaryDirectory(prefix="swiftkey1833-index-") as temp:
        env["GIT_INDEX_FILE"] = str(Path(temp) / "index")
        git(repo, "read-tree", state["head"], env=env)
        for path, raw in files.items():
            checked_path(path)
            blob = git(repo, "hash-object", "-w", "--stdin", input=raw, env=env)
            git(repo, "update-index", "--add", "--cacheinfo", f"100644,{blob},{path}", env=env)
        tree = git(repo, "write-tree", env=env)
        commit = git(repo, "commit-tree", tree, "-p", state["head"], "-F", "-", input=(message + "\n").encode(), env=env)
        require(git(repo, "show", "-s", "--format=%P", commit) == state["head"], "New commit is not a direct descendant")
        actual_changes = set(git(repo, "diff-tree", "--no-commit-id", "--name-only", "-r", commit).splitlines())
        require(actual_changes and actual_changes.issubset(files), "Prepared commit changes unrelated files")
        return {**state, "head": commit, "tree": tree}


def commit_feeds_atomically(helper, repo, states, updates, message):
    require(set(states) == {"main", "dev"} and set(updates) == set(states), "Both feed branches are required")
    helper.check_heads(states)
    git(repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"], states["dev"]["head"])
    prepared = {branch: prepare_commit(repo, state, updates[branch], message) for branch, state in states.items()}
    helper.check_heads(states)
    # Each proposed commit has exactly the leased old HEAD as its sole parent.
    # The lease adds race protection; it does not permit rewriting old history.
    git(repo, "push", "--atomic",
        f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}",
        f"https://github.com/{REPO}.git",
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    helper.check_heads(prepared)
    return prepared


def utc_created(states):
    now = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0)
    require(all(now > dt.datetime.fromisoformat(s["manifest"]["created_at"]) for s in states.values()),
            "Current UTC time must follow both old feed timestamps")
    return now.isoformat()


def verify_public_branch(helper, states, branch, feed, changelog, attempts=8):
    """Allow bounded raw-branch propagation delay without relaxing byte checks."""
    require(isinstance(attempts, int) and 1 <= attempts <= 8, "Invalid public verification retry bound")
    head = states[branch]["head"]
    targets = {"patches-bundle.json": json_bytes(feed), "CHANGELOG.md": changelog.encode()}
    for attempt in range(1, attempts + 1):
        helper.check_heads(states)
        pending = []
        for path, expected in targets.items():
            url = (f"https://raw.githubusercontent.com/{REPO}/{branch}/{path}"
                   f"?swiftkey1833={head}&attempt={attempt}")
            if helper.fetch(url) != expected:
                pending.append(path)
        if not pending:
            helper.check_heads(states)
            return attempt
        print(f"Public {branch} propagation pending ({attempt}/{attempts}): {', '.join(pending)}", flush=True)
        require(attempt < attempts, "Public branch metadata did not converge: " + branch)
        time.sleep(6)


def publication(args):
    expected = load_expected(args.expected)
    verify_source_files(args.source_dir, expected)
    helper = load_helper(args.repo) if not args.local_only else None
    if helper is not None:
        # Git receive-pack and ls-remote share authoritative refs; avoid stale
        # REST branch-head reads immediately after a successful atomic push.
        helper.check_heads = lambda states: check_heads_git(args.repo, states)
    if not args.local_only:
        require(os.environ.get('GITHUB_REPOSITORY') == REPO, 'Publication requires the authorized repository Actions runner')
    baseline = args.baseline.read_bytes() if args.baseline else helper.fetch(BASE_URL)
    standalone_baseline = args.standalone_baseline.read_bytes() if args.standalone_baseline else helper.fetch(SINGLE_BASE_URL)
    payloads, notes, local_report = validate_local(args.dist, args.second_dist, expected, baseline, standalone_baseline)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **local_report}, ensure_ascii=False, indent=2))
        return
    states = {branch: helper.snapshot(branch) for branch in ("main", "dev")}
    helper.check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    require(all(s["manifest"].get("download_url") == BASE_URL for s in states.values()), "Active baseline URL changed")
    require(all(s["manifest"]["version"] == PREVIOUS for s in states.values()), "Active bundle has advanced")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow checkout")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source commit")
    require(sha(helper.fetch(states["main"]["manifest"]["download_url"])) == BASE_SHA256, "Active baseline bytes differ")
    policies = {branch: helper.content("releases/ULike_ACTIVE.json", state["head"]) for branch, state in states.items()}
    created = utc_created(states)
    for state in states.values():
        metadata(state, "https://example.invalid/preflight-only", created, notes)
    require(helper.api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(helper.api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None, "Distribution branch already exists; inspect before retry")
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes", "heads": {b:s["head"] for b,s in states.items()}, **local_report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "swiftkey1833-publication-receipt-v1", "swiftkey_version": VERSION, "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "baseline_swiftkey_version": SINGLE_PREVIOUS, "baseline_swiftkey_sha256": SINGLE_BASE_SHA256,
               "assets": expected["artifacts"], "local_validation": local_report,
               "android_device_tested": False, "original_apk_apply_tested": False,
               "manager_parser_source": MANAGER_SOURCE, "manager_app_names": list(APP_NAMES), "operations": []}

    def checkpoint(status, **changes):
        receipt.update(status=status, **changes)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)

    checkpoint("preflight_passed")
    try:
        helper.check_heads(states)
        release = helper.api(f"repos/{REPO}/releases", {
            "tag_name": TAG, "target_commitish": states["main"]["head"],
            "name": "SwiftKey Beta v1.8.33 文脈を優先する候補選択 / Hiro Morphe v1.0.345",
            "body": notes, "draft": True, "prerelease": True, "make_latest": "false"})
        checkpoint("draft_created", release_id=release["id"])
        for name in ASSETS:
            helper.run(["gh", "release", "upload", TAG, "--repo", REPO, args.dist / name])
        helper.verify_assets(release["id"], expected["artifacts"], False)
        checkpoint("draft_assets_verified")
        helper.check_heads(states)
        helper.api(f"repos/{REPO}/releases/{release['id']}", {"draft": False, "prerelease": True, "make_latest": "false"}, "PATCH")
        helper.verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("release_bytes_verified")
        helper.check_heads(states)
        helper.api(f"repos/{REPO}/git/refs", {"ref": "refs/heads/" + RAW_BRANCH, "sha": states["main"]["head"]})
        raw_state = {**states['main'], 'branch': RAW_BRANCH}
        helper.check_heads({'raw': raw_state})
        # Large MPPs use Git pack transfer; GitHub's JSON/base64 blobs API rejects them.
        prepared = prepare_commit(args.repo, raw_state,
            {'downloads/' + name: payloads[name] for name in (SINGLE, COMBINED)},
            'release: exact SwiftKey1833 and bundle345 immutable MPP downloads')
        helper.check_heads({'raw': raw_state})
        git(args.repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:{raw_state["head"]}',
            f'https://github.com/{REPO}.git', f'{prepared["head"]}:refs/heads/{RAW_BRANCH}')
        helper.check_heads({'raw': prepared})
        raw_state = prepared
        immutable_urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE, COMBINED)}
        for name, url in immutable_urls.items():
            require(helper.fetch(url) == payloads[name], "Immutable download mismatch: " + name)
        checkpoint("immutable_downloads_verified", raw_commit=raw_state["head"], download_urls=immutable_urls)
        helper.check_heads(states)
        new_metadata, updates = {}, {}
        for branch, state in states.items():
            feed, changelog = metadata(state, immutable_urls[COMBINED], created, notes)
            new_metadata[branch] = (feed, changelog)
            updates[branch] = {"patches-bundle.json": json_bytes(feed), "CHANGELOG.md": changelog.encode()}
            if branch == "main":
                for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                    updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds_atomically(helper, args.repo, states, updates,
                                         "release: SwiftKey1833 contextual candidate selection; bundle345 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(helper.content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(helper.content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(helper.content("releases/ULike_ACTIVE.json", state["head"]) == policies[branch], "ULike policy changed")
            require(helper.fetch(feed["download_url"]) == payloads[COMBINED], "Manager download mismatch")
            public_attempts = verify_public_branch(helper, active, branch, feed, changelog)
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                                           "changelog_verified": True, "ulike_policy_byte_identical": True,
                                           "manager_download_verified": True,
                                           "public_verification_attempts": public_attempts})
        helper.check_heads(active)
        helper.verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("published_and_verified", published=True, release_url=PAGE,
                   manager_main_dev_updated_atomically=True, manager_update_eligible_for_both_app_names=True,
                   manager_device_update_badge_observed=False, other_apps_false_updates=False,
                   fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        receipt_updates = {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active}
        saved = commit_feeds_atomically(helper, args.repo, active, receipt_updates,
                                        "docs: verified SwiftKey1833 publication receipt")
        for branch, state in saved.items():
            require(helper.content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(helper.content("releases/ULike_ACTIVE.json", state["head"]) == policies[branch], "ULike policy changed while saving receipt")
        helper.run(["gh", "release", "upload", TAG, "--repo", REPO, receipt_path])
        published_assets = {**expected["artifacts"], RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}
        helper.verify_assets(release["id"], published_assets, True)
        helper.check_heads(saved)
        print("PASS public Release and immutable MPP bytes, both Manager feeds, aliases, unchanged ULike policy and saved receipt", flush=True)
    except BaseException as error:
        checkpoint("failed_requires_inspection", error=type(error).__name__ + ": " + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", required=True, type=Path)
    parser.add_argument("--second-dist", required=True, type=Path, help="Independent reproducible rebuild output")
    parser.add_argument("--repo", required=True, type=Path)
    parser.add_argument("--expected", required=True, type=Path)
    parser.add_argument("--source-dir", type=Path, default=Path(__file__).resolve().parent,
                        help="Source tree whose files are pinned in the reviewed manifest")
    parser.add_argument("--baseline", type=Path, help="Local exact 1.0.344 MPP; avoids baseline download")
    parser.add_argument("--standalone-baseline", type=Path, help="Local exact SwiftKey 1.8.32 MPP")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true", help="Verify local reviewed outputs only")
    mode.add_argument("--preflight-only", action="store_true", help="Also verify current GitHub state, without writes")
    args = parser.parse_args()
    for key in ("dist", "second_dist", "repo", "expected", "baseline", "standalone_baseline", "source_dir"):
        if getattr(args, key) is not None:
            setattr(args, key, getattr(args, key).resolve())
    require(not args.local_only or (args.baseline is not None and args.standalone_baseline is not None),
            "--local-only requires --baseline and --standalone-baseline")
    publication(args)


if __name__ == "__main__":
    main()

