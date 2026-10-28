#!/usr/bin/env python3
"""Publish reviewed dictionary repair bytes; never overwrite an advanced feed.

Two independent builds, reviewed source/artifact pins, exact archive deltas,
dictionary coverage, native ABI, and two real Morphe loaders gate publication.
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
VERSION, PREVIOUS, BUNDLE = '1.8.24', '1.0.312', '1.0.313'
SINGLE_PREVIOUS = '1.8.23'
TAG, RAW_BRANCH = 'swiftkey-v1.8.24', 'release/swiftkey1824-313'
SINGLE = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.24.mpp'
COMBINED = 'Hiro_Morphe_Patches_v1.0.313.mpp'
BASE_NAME = 'Hiro_Morphe_Patches_v1.0.312.mpp'
BASE_SHA256 = '5381f9b8038c847f244275b3e2c90a09869c235cc5492c8b59aa832570ad5a3e'
BASE_URL = f'https://raw.githubusercontent.com/{REPO}/845082966b599f03e66d592b5a4ed066f0a9f94f/downloads/{BASE_NAME}'
SINGLE_BASE_NAME = 'SwiftKeyBeta_v2_SamsungEmoji_v1.8.23.mpp'
SINGLE_BASE_SHA256 = '714d66b2cc5367346927fd8a766577640d4d1c3bc61829671f888ee13cc6bbdc'
SINGLE_BASE_URL = f'https://raw.githubusercontent.com/{REPO}/72a7fdd39a0ce9d4f4531c3509e90fab6300fe41/downloads/{SINGLE_BASE_NAME}'
BASE_RUNTIME_SHA256 = 'dea2051af47cd8e2106437dbf7b3bc4f9ac2e6d496a334462700d7d2584e405a'
REFERENCE_APK_SHA256 = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'
QA_NAME = 'QA_SwiftKey_v1.8.24.json'
SOURCE_ZIP = 'SwiftKeyBeta_v1.8.24_sources_and_QA.zip'
RECEIPT_NAME = 'publication_SwiftKey_v1.8.24.json'
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, 'RELEASE_NOTES.txt', 'SHA256SUMS.txt')
RELEASE_ROOT = 'releases/swiftkey1824-313'
PAGE = f'https://github.com/{REPO}/releases/tag/{TAG}'
MF, EXTENSION = 'META-INF/MANIFEST.MF', 'extensions/swiftkey_japanese.mpe'
REQUIRED_CHANGED = {MF, EXTENSION}
JVM_PREFIX = 'app/hiro/swiftkey/patches/'
JVM_CLASSES = frozenset(JVM_PREFIX + name + '.class' for name in (
    'JapaneseSamsungEmojiPatch', 'MethodContract', 'InputLearningPatch',
    'DictionarySupplementPatch', 'KeySwapPatch', 'PasteProtectionPatch',
    'JapaneseGlidePatch', 'DeleteProtectionPatch', 'CandidateScrollPatch',
    'CandidateToolbarPatch', 'PunctuationPatch', 'JapaneseStartPatch', 'EmojiFooterPatch'))
CHANGED_CLASSES = ['hiro/swiftkey/KatakanaEnglishCandidates.smali', 'hiro/swiftkey/WordCandidates.smali']
CHANGED_METHODS = [
    'Lhiro/swiftkey/KatakanaEnglishCandidates;->isRomaji(Ljava/lang/String;)Z',
    'Lhiro/swiftkey/WordCandidates;->addWords(Ljava/util/LinkedHashSet;[Ljava/lang/String;Ljava/util/Set;)V',
    'Lhiro/swiftkey/WordCandidates;->isRomaji(Ljava/lang/String;)Z']
REQUIRED_QA = {
    'status': 'DICTIONARY_LOOKUP_HOST_AND_ABI_VERIFIED_DEVICE_UNVERIFIED',
    'swiftkey_version': VERSION, 'bundle_version': BUNDLE,
    'baseline_swiftkey_version': SINGLE_PREVIOUS, 'baseline_bundle_version': PREVIOUS,
    'target_package': 'com.touchtype.swiftkey.beta', 'target_version': '9.13.16.4',
    'target_version_code': 1236271168, 'device_tested': False,
    'original_apk_apply_tested': False, 'all_dictionary_resources_preserved': True,
    'other_apps_byte_identical': True, 'loader_entries_byte_identical': True,
    'standalone_and_bundle_runtime_equal': True, 'runtime_before_classes': 91,
    'runtime_after_classes': 91, 'runtime_changed_classes': CHANGED_CLASSES,
    'runtime_changed_methods': CHANGED_METHODS, 'host_tests_passed': True,
    'native_abi_verified': True, 'two_loader_sdks_agree': True,
    'host_report.status': 'PASSED', 'host_report.negative_control_verified': True,
    'host_report.compiled_word_baseline_parity': True,
    'host_report.english_regression.whole_long_mark_examples_verified': True,
    'host_report.word_coverage.all_exact_entries_reachable': True,
    'abi_report.replayed_in_ci': False,
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
INVENTORY = re.compile(r'(?m)^((?:Microsoft )?SwiftKey Beta：)v(1\.8\.23)(（9\.13\.16\.4(?:／1236271168)?）)\r?$')
SUMMARY = '長音「ー」を含む読みの追加英語・日本語候補が出ない問題と、日本語の完全一致候補が12件で切られる問題を修正。辞書・学習・既存操作を保持。実機未確認。'


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
        'schema': 'swiftkey1824-publication-v1', 'swiftkey_version': VERSION,
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
              'Only the metadata and SwiftKey runtime may change: ' + kind)
        exact(data.get('allowed_added_' + kind + '_entries'), [], 'No archive entries may be added: ' + kind)
    for key, value in REQUIRED_QA.items():
        exact(data.get('qa_required_values', {}).get(key), value, 'Missing reviewed QA requirement: ' + key)
    return data


def verify_source_files(source_dir, expected):
    pins = expected.get('source_files')
    required = {'build1824.py', 'publish1824.py', 'publisher_tests.py', 'workflow.yml', 'tests/AuditDictionaryAbi.java'}
    require(isinstance(pins, dict) and required.issubset(pins), 'Incomplete reviewed source pins')
    root = source_dir.resolve()
    for name, row in pins.items():
        checked_path(name)
        path = root / name
        require(path.is_file() and not path.is_symlink() and path.resolve().is_relative_to(root), 'Unsafe/missing source: ' + name)
        raw = path.read_bytes()
        require(row.get('bytes') == len(raw) and row.get('sha256') == sha(raw), 'Reviewed source changed: ' + name)
    for path in root.rglob('*'):
        if path.is_file() and path.suffix in {'.java', '.py', '.smali', '.json', '.yml', '.txt', '.md', '.bin', '.b64'}:
            require(path.relative_to(root).as_posix() in pins, 'Unpinned build/source input: ' + str(path.relative_to(root)))
    return len(pins)


def lookup_qa(data, dotted_path):
    for key in dotted_path.split('.'):
        require(isinstance(data, dict) and key in data, 'Missing QA value: ' + dotted_path)
        data = data[key]
    return data


def validate_archive_delta(old, new, old_version, new_version, kind):
    require(set(old) == set(new), kind + ' archive entry inventory changed')
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
    require(old['classes.dex'] == new['classes.dex'], kind + ' Android patch loader changed')
    return sorted(changed)


def validate_evidence(qa, expected, runtime_sha):
    for path, value in expected['qa_required_values'].items():
        exact(lookup_qa(qa, path), value, 'QA assertion failed: ' + path)
    for path, value in REQUIRED_QA.items():
        exact(lookup_qa(qa, path), value, 'Hardcoded QA contract failed: ' + path)
    host, abi, loaders = qa.get('host_report', {}), qa.get('abi_report', {}), qa.get('loader_reports', [])
    exact(host.get('runtime_sha256'), runtime_sha, 'Host tests target different runtime')
    require(isinstance(host.get('word_coverage'), dict) and host['word_coverage'], 'Missing exhaustive word coverage report')
    require(isinstance(host.get('english_regression'), dict) and host['english_regression'], 'Missing English regression report')
    exact(abi.get('runtime_sha256'), runtime_sha, 'ABI gate targets different runtime')
    report = abi.get('report', {})
    for name, value in {'status': 'PASSED', 'extension_sha256': runtime_sha,
                        'apk_sha256': REFERENCE_APK_SHA256, 'unresolved': [], 'inaccessible': [],
                        'incompatible_opcodes': [], 'device_tested': False, 'original_apk_apply_tested': False}.items():
        exact(report.get(name), value, 'Native ABI assertion failed: ' + name)
    required_families = ('WordCandidates', 'KatakanaEnglishCandidates', 'EmojiCandidates', 'FrequencyLearning')
    audited = report.get('audited_classes', [])
    require(all('Lhiro/swiftkey/' + n + ';' in audited for n in required_families), 'Native ABI coverage omits dictionary families')
    for key in ('platform_members_checked', 'platform_inherited_members_resolved', 'each_caller_access_checked', 'interface_callbacks_checked'):
        exact(report.get(key), True, 'Native ABI checker omitted: ' + key)
    exact(abi.get('verifier_source_sha256'), expected['source_files']['tests/AuditDictionaryAbi.java']['sha256'], 'ABI verifier source changed')
    require(isinstance(loaders, list) and len(loaders) == 2 and loaders[0] == loaders[1], 'Both Morphe loaders must agree')
    for i, loader in enumerate(loaders):
        for key, value in {'status': 'PASSED', 'selected_extension_stream_verified': True,
                           'selected_extension_sha256': runtime_sha, 'baseline_extension_sha256': BASE_RUNTIME_SHA256,
                           'baseline_updated_dependency_graph_equal': True,
                           'standalone_combined_dependency_graph_equal': True,
                           'clean_apk_apply_tested': False, 'device_tested': False}.items():
            exact(loader.get(key), value, 'Morphe loader gate failed: ' + str(i) + ':' + key)
    return {'host_runtime_bound': True, 'native_abi_runtime_and_source_bound': True,
            'two_loader_sdks_bound': True, 'native_abi_replayed_in_ci': False}


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
        require({n for n in package if n.startswith(JVM_PREFIX) and n.endswith('.class')} == JVM_CLASSES, kind + ' SwiftKey loader inventory differs')
    for name in JVM_CLASSES:
        require(old[name] == old_single[name] == new[name] == single[name], 'SwiftKey JVM loader changed: ' + name)
    require(old[EXTENSION] == old_single[EXTENSION] and sha(old[EXTENSION]) == BASE_RUNTIME_SHA256, 'Wrong baseline runtime')
    require(new[EXTENSION] == single[EXTENSION], 'Standalone and aggregate runtime differ')
    resources = {n: v for n, v in old.items() if n.startswith('swiftkey/')}
    require(resources and all({n: v for n, v in p.items() if n.startswith('swiftkey/')} == resources for p in (old_single, new, single)),
            'A dictionary/resource was changed or differs across packages')
    runtime_sha = sha(new[EXTENSION])
    qa = json.loads(payloads[QA_NAME])
    evidence = validate_evidence(qa, expected, runtime_sha)
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
    require(all(token in notes for token in (VERSION, BUNDLE, '日本語', '英語', '辞書', '長音', '12', '未確認')), 'Incomplete release notes')
    return payloads, notes, {'two_independent_builds_byte_identical': True,
        'integrated_changed_entries': changed, 'standalone_changed_entries': single_changed,
        'integrated_added_entries': [], 'standalone_added_entries': [],
        'integrated_unchanged_entries': len(old) - len(changed),
        'standalone_unchanged_entries': len(old_single) - len(single_changed),
        'all_non_target_archive_entries_byte_identical': True,
        'all_dictionaries_byte_identical': True, 'all_loaders_byte_identical': True,
        'runtime_sha256': runtime_sha, **evidence}


def load_helper(repo):
    path = repo / HELPER
    raw = path.read_bytes()
    git_sha = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    require(git_sha == HELPER_BLOB_SHA, "Pinned publication helper changed")
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location("swiftkey1824_publication_helper", path)
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
    with tempfile.TemporaryDirectory(prefix="swiftkey1824-index-") as temp:
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
                   f"?swiftkey1824={head}&attempt={attempt}")
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
    receipt = {"schema": "swiftkey1824-publication-receipt-v1", "swiftkey_version": VERSION, "bundle_version": BUNDLE,
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
            "name": "SwiftKey Beta v1.8.24 日本語・英語の追加辞書候補修正 / Hiro Morphe v1.0.313",
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
        raw_state = helper.commit_files({**states["main"], "branch": RAW_BRANCH},
                                        {"downloads/" + name: payloads[name] for name in (SINGLE, COMBINED)},
                                        "release: exact SwiftKey1824 and bundle313 immutable MPP downloads")
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
                                         "release: SwiftKey1824 dictionary long-mark lookup and exact candidate coverage repair; bundle313 Manager update")
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
                                        "docs: verified SwiftKey1824 publication receipt")
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
    parser.add_argument("--baseline", type=Path, help="Local exact 1.0.312 MPP; avoids baseline download")
    parser.add_argument("--standalone-baseline", type=Path, help="Local exact SwiftKey 1.8.23 MPP")
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
