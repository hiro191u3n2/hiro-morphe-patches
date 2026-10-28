#!/usr/bin/env python3
"""Publish reviewed sticky Shift and emoji expansion bytes; never overwrite an advanced feed.

Two independent builds, reviewed source/artifact pins, exact archive deltas,
Shift and emoji host coverage, native hook checks, and two real Morphe loaders gate publication.
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
VERSION, PREVIOUS, BUNDLE = '1.8.26', '1.0.314', '1.0.315'
SINGLE_PREVIOUS = '1.8.25'
TAG, RAW_BRANCH = 'swiftkey-v1.8.26', 'release/swiftkey1826-315'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.26.mpp'
COMBINED = 'Hiro_Morphe_Patches_v1.0.315.mpp'
BASE_NAME = 'Hiro_Morphe_Patches_v1.0.314.mpp'
BASE_SHA256 = 'cdc309a2b8a4b4f22cfec05a63d73d53089a6e0d9950ed6a835f457ca8541b5a'
BASE_URL = f'https://raw.githubusercontent.com/{REPO}/8a7eeea4821b7bd14f55aa32e1c8d8c36f0f3f91/downloads/{BASE_NAME}'
SINGLE_BASE_NAME = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.25.mpp'
SINGLE_BASE_SHA256 = 'b6cfb2362dffe42468c8ba795dabd801153b9ed10581b10ba4fab438518165ab'
SINGLE_BASE_URL = f'https://raw.githubusercontent.com/{REPO}/8a7eeea4821b7bd14f55aa32e1c8d8c36f0f3f91/downloads/{SINGLE_BASE_NAME}'
BASE_RUNTIME_SHA256 = '94e7af5056df941fc935ff8876663fe5003584ab0e38f8e71f9c8d195f907755'
REFERENCE_APK_SHA256 = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
QA_NAME = 'QA_SwiftKey_v1.8.26.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.26_sources_and_QA.zip'
RECEIPT_NAME = 'publication_SwiftKey_v1.8.26.json'
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')
RELEASE_ROOT = 'releases/swiftkey1826-315'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
MF, EXTENSION = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
EMOJI = 'swiftkey/emoji_ja.tsv'
LOADER_CLASS = 'app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
REQUIRED_CHANGED = {MF, EMOJI, 'classes.dex', LOADER_CLASS}
SHIFT_CLASS = 'app/hiro/swiftkey/patches/ShiftLockPatch.class'
REQUIRED_ADDED = {SHIFT_CLASS}
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = frozenset(JVM_PREFIX + name + '.class' for name in (
    'JapaneseSamsungEmojiPatch', 'MethodContract', 'InputLearningPatch',
    'DictionarySupplementPatch', 'KeySwapPatch', 'PasteProtectionPatch',
    'JapaneseGlidePatch', 'DeleteProtectionPatch', 'CandidateScrollPatch',
    'CandidateToolbarPatch', 'PunctuationPatch', 'JapaneseStartPatch', 'EmojiFooterPatch'))
REQUIRED_QA = {
    'status': 'SHIFT_EMOJI_HOST_AND_NATIVE_VERIFIED_DEVICE_UNVERIFIED',
    'swiftkey_version': VERSION, 'bundle_version': BUNDLE,
    'baseline_swiftkey_version': SINGLE_PREVIOUS, 'baseline_bundle_version': PREVIOUS,
    'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4',
    'target_version_code': 1236271168, 'device_tested': False,
    'original_apk_apply_tested': False, 'other_apps_byte_identical': True,
    'unrelated_loader_entries_byte_identical': True, 'loader_changes_verified': True, 'standalone_and_bundle_runtime_equal': True,
    'host_tests_passed': True, 'native_hook_verified': True, 'runtime_byte_identical': True, 'two_loader_sdks_agree': True,
    'all_non_emoji_dictionary_resources_preserved': True,
    'emoji_dictionary.old_pairs_preserved': True, 'emoji_dictionary.order_preserved': True,
    'emoji_dictionary.duplicates_absent': True,
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
INVENTORY = re.compile(r'(?m)^((?:Microsoft )?SwiftKey Beta：)v(1\.8\.25)(（9\.13\.16\.4(?:／1236271168)?）)\r?$')
SUMMARY = 'Shiftを再度押すまでローマ字大文字を維持し、関連語からの絵文字候補を拡充。既存辞書と操作を保持。実機未確認。'


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
        'schema': 'swiftkey1826-publication-v1', 'swiftkey_version': VERSION,
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
              'Only reviewed metadata, emoji dictionary and Shift/digest loaders may change: ' + kind)
        exact(sorted(data.get('allowed_added_' + kind + '_entries', [])), sorted(REQUIRED_ADDED), 'Only the Shift hook loader class may be added: ' + kind)
    for key, value in REQUIRED_QA.items():
        exact(data.get('qa_required_values', {}).get(key), value, 'Missing reviewed QA requirement: ' + key)
    return data


def verify_source_files(source_dir, expected):
    pins = expected.get('source_files')
    required = {'build1826.py', 'publish1826.py', 'publisher_tests.py', 'workflow.yml', 'shift/build_shift_loader.py', 'loader/PatchEmojiLoader.java', 'emoji/build_emoji.py'}
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
    require(set(new) == set(old) | REQUIRED_ADDED and not (set(old) & REQUIRED_ADDED), kind + ' archive entry inventory changed')
    changed = {name for name in old if old[name] != new[name]}
    require(changed == REQUIRED_CHANGED, kind + ' unexpected changed entries: ' + repr(sorted(changed)))
    before, after = headers(old[MF]), headers(new[MF])
    require(before.get('Version') == old_version and after.get('Version') == new_version, kind + ' wrong versions')
    require(set(before) == set(after), kind + ' manifest keys changed')
    require({k: v for k, v in before.items() if k not in {'Version', 'Timestamp', 'Description'}}
            == {k: v for k, v in after.items() if k not in {'Version', 'Timestamp', 'Description'}},
            kind + ' unrelated manifest field changed')
    require(new.get('classes.dex', b'').startswith(b'dex\n') and new[EXTENSION].startswith(b'dex\n'), kind + ' missing DEX')
    require(struct.unpack_from('<I', new[EXTENSION], 96)[0] == 91, kind + ' runtime class inventory differs')
    return sorted(changed)


def validate_evidence(qa, expected, runtime_sha, dictionary_sha):
    for path, value in expected['qa_required_values'].items():
        exact(lookup_qa(qa, path), value, 'QA assertion failed: ' + path)
    for path, value in REQUIRED_QA.items():
        exact(lookup_qa(qa, path), value, 'Hardcoded QA contract failed: ' + path)
    exact(qa.get('runtime_sha256'), runtime_sha, 'Host evidence targets different runtime')
    exact(qa.get('emoji_dictionary', {}).get('sha256'), dictionary_sha, 'Host evidence targets different dictionary')
    host = qa.get('host_report', {})
    exact(host.get('runtime_sha256'), runtime_sha, 'Host report targets different runtime')
    exact(host.get('dictionary_sha256'), dictionary_sha, 'Host report targets different dictionary')
    exact(host.get('status'), 'PASSED', 'Host regression suite did not pass')
    exact(host.get('shift', {}).get('status'), 'PASSED', 'Shift state regression did not pass')
    exact(host.get('emoji', {}).get('status'), 'PASSED', 'Emoji lookup regression did not pass')
    # Native hook checks and output hashes are bound below to the packaged JVM/DEX.
    exact(qa.get('native_hook_report', {}).get('status'), 'PASSED', 'Native Shift hook evidence missing')
    hook = qa['native_hook_report']
    for key, value in {'unrelated_android_classes_preserved': True, 'unrelated_jvm_classes_preserved': True,
                       'jvm_bridge_inverse_restoration': True, 'android_bridge_inverse_restoration': True,
                       'android_serialization_canonical_preservation': True, 'added_loader_helper': SHIFT_CLASS,
                       'device_tested': False, 'original_apk_apply_tested': False}.items():
        exact(hook.get(key), value, 'Shift loader preservation failed: ' + key)
    native = hook.get('native_report', {})
    for key, value in {'status': 'PASSED', 'reference_apk_sha256': REFERENCE_APK_SHA256,
                       'native_method': 'Lwb0/p1;->n(Llh0/a;)V',
                       'baseline_method_sha256': '098d23bd8c99e605fd1357ca13126c07532374afdb1bec6a5ca76be897e869b6',
                       'patched_method_sha256': '8e3997d695faed0411154a76f76eb17583a9844d03b3aabfa4a92ce685ed0ea2',
                       'native_instructions_replaced': 1, 'native_methods_changed': 1,
                       'inverse_native_method_restoration': True, 'all_target_drift_rejected_before_mutation': True,
                       'native_dex_interpreter_tests': True, 'auto_caps_and_hardware_methods_preserved': True,
                       'long_press_timing_unchanged': False, 'device_tested': False, 'original_apk_apply_tested': False}.items():
        exact(native.get(key), value, 'Native Shift contract failed: ' + key)
    require(type(native.get('assertions')) is int and native['assertions'] >= 76, 'Shift regression coverage incomplete')
    fixture_prefix = 'shift/tests/fixtures/'
    source_fixtures = {name.removeprefix(fixture_prefix): row['sha256'] for name, row in expected['source_files'].items()
                       if name.startswith(fixture_prefix) and name.endswith('.smali')}
    require(source_fixtures and native.get('fixtures_sha256') == source_fixtures, 'Native fixture evidence differs from pinned source')

    exact(qa.get('emoji_loader_report', {}).get('status'), 'PASSED', 'Emoji digest loader evidence missing')
    for key in ('unrelated_android_classes_preserved', 'unrelated_jvm_classes_preserved',
                'only_emoji_hash_string_changed', 'asset_capacity_unchanged', 'real_jvm_resource_loader_verified'):
        exact(qa['emoji_loader_report'].get(key), True, 'Emoji loader preservation failed: ' + key)
    exact(qa['emoji_loader_report'].get('emoji_sha256'), dictionary_sha, 'Emoji loader bound to wrong dictionary')
    for kind in ('single', 'bundle'):
        incoming = hook.get('input_pins', {}).get(kind, {})
        exact(incoming.get('android_sha256'), qa['emoji_loader_report'].get(kind + '_android_sha256'), 'Shift input DEX differs from verified emoji loader: ' + kind)
        exact(incoming.get('jvm_sha256'), qa['emoji_loader_report'].get('jvm_sha256'), 'Shift input JVM differs from verified emoji loader: ' + kind)

    loaders = qa.get('loader_reports', [])
    require(isinstance(loaders, list) and len(loaders) == 2 and loaders[0] == loaders[1], 'Both real Morphe loaders must agree')
    for loader in loaders:
        for key, value in {'status': 'PASSED', 'selected_extension_stream_verified': True,
                           'selected_extension_sha256': runtime_sha, 'baseline_extension_sha256': BASE_RUNTIME_SHA256,
                           'baseline_updated_dependency_graph_equal': True,
                           'standalone_combined_dependency_graph_equal': True,
                           'clean_apk_apply_tested': False, 'device_tested': False}.items():
            exact(loader.get(key), value, 'Morphe loader gate failed: ' + key)
    return {'host_runtime_and_dictionary_bound': True, 'two_loader_sdks_bound': True}


def dictionary_rows(raw):
    previous = None
    for number, line in enumerate(io.StringIO(raw.decode('utf-8')), 1):
        parts = line.rstrip('\n').split('\t')
        require(len(parts) >= 2 and all(parts), 'Malformed dictionary row: ' + str(number))
        reading, candidates = parts[0], tuple(parts[1:])
        require(previous is None or previous < reading, 'Dictionary readings repeat or are not sorted')
        require(len(candidates) == len(set(candidates)), 'Duplicate reading/candidate pair: ' + reading)
        require(not any('\r' in v or '\x00' in v for v in parts), 'Control character in dictionary')
        previous = reading
        yield reading, candidates


def validate_dictionary(old_raw, new_raw):
    old = dict(dictionary_rows(old_raw))
    old_readings = len(old)
    old_candidates = sum(map(len, old.values()))
    count = rows = maximum = 0
    for reading, candidates in dictionary_rows(new_raw):
        existing = old.pop(reading, ())
        require(candidates[:len(existing)] == existing, 'Existing candidate order/content changed: ' + reading)
        count += len(candidates)
        rows += 1
        maximum = max(maximum, len(candidates))
    require(not old, 'Existing dictionary readings removed')
    require(count > old_candidates and rows >= old_readings, 'Dictionary expansion is missing')
    return {'old_readings': old_readings, 'old_candidates': old_candidates,
            'readings': rows, 'candidates': count, 'added_candidates': count - old_candidates,
            'maximum_candidates_per_reading': maximum,
            'old_pairs_preserved': True, 'order_preserved': True, 'duplicates_absent': True,
            'sha256': sha(new_raw)}


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
        if name != LOADER_CLASS:
            require(old[name] == new[name], 'Unrelated SwiftKey JVM loader changed: ' + name)
    require(new[SHIFT_CLASS] == single[SHIFT_CLASS], 'Standalone/bundle Shift helper differs')
    require(old[EXTENSION] == old_single[EXTENSION] and sha(old[EXTENSION]) == BASE_RUNTIME_SHA256, 'Wrong baseline runtime')
    require(new[EXTENSION] == single[EXTENSION] == old[EXTENSION], 'Existing runtime bytes changed')
    dictionary_names = {name for name in old if name.startswith('swiftkey/')}
    require(dictionary_names == {name for name in old_single if name.startswith('swiftkey/')}, 'Baseline dictionary inventories differ')
    for name in dictionary_names:
        require(old[name] == old_single[name] and new[name] == single[name], 'Standalone and combined dictionary differ: ' + name)
        if name != EMOJI:
            require(old[name] == new[name], 'Non-emoji dictionary resource changed: ' + name)
    dictionary_report = validate_dictionary(old[EMOJI], new[EMOJI])
    runtime_sha = sha(new[EXTENSION])
    qa = json.loads(payloads[QA_NAME])
    # Final Shift component must bind its final JVM/Android outputs in the report.
    loader_proof = qa.get('native_hook_report', {})
    for key, raw in {'single_android_sha256': single['classes.dex'], 'bundle_android_sha256': new['classes.dex'],
                     'jvm_sha256': new[LOADER_CLASS], 'helper_jvm_sha256': new[SHIFT_CLASS]}.items():
        exact(loader_proof.get(key), sha(raw), 'Native loader evidence targets different package bytes: ' + key)
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
    for key, value in dictionary_report.items():
        exact(qa.get('emoji_dictionary', {}).get(key), value, 'Dictionary QA does not match packaged data: ' + key)
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
    for name, row in expected['source_files'].items():
        raw = sources.get('src/' + name)
        require(raw is not None and len(raw) == row['bytes'] and sha(raw) == row['sha256'], 'Source archive differs: ' + name)
    require(not any(PurePosixPath(n).suffix.casefold() in {'.apk', '.apks', '.apkm', '.aab', '.jks', '.keystore'} for n in sources),
            'Source archive contains application or signing-key bytes')
    notes = payloads['RELEASE_NOTES.txt'].decode()
    require(all(token in notes for token in (VERSION, BUNDLE, 'Shift', '大文字', '絵文字', '未確認')), 'Incomplete release notes')
    return payloads, notes, {'two_independent_builds_byte_identical': True,
        'integrated_changed_entries': changed, 'standalone_changed_entries': single_changed,
        'integrated_added_entries': sorted(REQUIRED_ADDED), 'standalone_added_entries': sorted(REQUIRED_ADDED),
        'integrated_unchanged_entries': len(old) - len(changed),
        'standalone_unchanged_entries': len(old_single) - len(single_changed),
        'all_non_target_archive_entries_byte_identical': True,
        'existing_emoji_pairs_and_order_preserved': True, 'all_non_emoji_dictionary_resources_preserved': True, 'emoji_dictionary': dictionary_report, 'unrelated_jvm_loaders_byte_identical': True,
        'runtime_sha256': runtime_sha, **evidence}


def load_helper(repo):
    path = repo / HELPER
    raw = path.read_bytes()
    git_sha = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    require(git_sha == HELPER_BLOB_SHA, "Pinned publication helper changed")
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location("swiftkey1826_publication_helper", path)
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


def prepare_commit(repo, state, files, message):
    """Use a private index; preserve the checkout, its index, and every other path."""
    env = {**os.environ, "GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_COMMITTER_NAME": "github-actions[bot]",
           "GIT_AUTHOR_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com",
           "GIT_COMMITTER_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com"}
    with tempfile.TemporaryDirectory(prefix="swiftkey1826-index-") as temp:
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
                   f"?swiftkey1826={head}&attempt={attempt}")
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
    receipt = {"schema": "swiftkey1826-publication-receipt-v1", "swiftkey_version": VERSION, "bundle_version": BUNDLE,
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
            "name": "SwiftKey Beta v1.8.26 Shift大文字維持・関連語の絵文字候補拡充 / Hiro Morphe v1.0.315",
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
            'release: exact SwiftKey1826 and bundle315 immutable MPP downloads')
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
                                         "release: SwiftKey1826 sticky Shift and related emoji expansion; bundle315 Manager update")
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
                                        "docs: verified SwiftKey1826 publication receipt")
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
    parser.add_argument("--baseline", type=Path, help="Local exact 1.0.314 MPP; avoids baseline download")
    parser.add_argument("--standalone-baseline", type=Path, help="Local exact SwiftKey 1.8.25 MPP")
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
