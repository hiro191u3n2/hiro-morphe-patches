#!/usr/bin/env python3
"""Publish single-image NR5-NR8 on exact .56/.189 baseline while retaining other apps.

The independent --expected manifest pins all six artifacts and the exact QA
contract. Read-only --local-only and --preflight-only never write to GitHub.
Both feed branches advance atomically to direct descendants of the leased HEADs.
The checkout, its normal index, and all unrelated repository paths are preserved.
"""
from __future__ import annotations
import argparse
import datetime as dt
import hashlib
import base64
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import time
import urllib.error
import urllib.request
import tempfile
import zipfile

REPO = "hiro191u3n2/hiro-morphe-patches"
VERSION, PREVIOUS_APP = "1.9.57", "1.9.56"
PREVIOUS, BUNDLE = "1.0.189", "1.0.190"
TAG, RAW_BRANCH = "ulike-v1.9.57", "release/ulike1957-190"
SINGLE = "ULike_HQ_Texture_Online_v1.9.57.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.190.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.189.mpp"
BASE_SHA256 = "012ea089bcc82a5ac660d1e880af5304b71dcb722cf559f857956f4df1fc3073"
BASE_BYTES = 17632413
BASE_URL = f"https://raw.githubusercontent.com/{REPO}/514b048bf98299b6afc37dc1e173b3d2e96029f9/downloads/{BASE_NAME}"
SINGLE_BASE_NAME = "ULike_HQ_Texture_Online_v1.9.56.mpp"
SINGLE_BASE_SHA256 = "337dd9366c86c1c9988e5f690873aa1b8369fd48172737c1f644352fd2e1cb42"
SINGLE_BASE_BYTES = 985328
SINGLE_BASE_URL = f"https://github.com/{REPO}/releases/download/ulike-v1.9.56/{SINGLE_BASE_NAME}"
QA_NAME = "QA_ULike_v1.9.57.json"
SOURCE_ZIP = "ULike_v1.9.57_sources_and_QA.zip"
RECEIPT_NAME = "publication_ULike_v1.9.57.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/ulike1957-strong-single-noise"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
MF, RUNTIME = "META-INF/MANIFEST.MF", "ulike/runtime.dex"
ULIKE_LOADER = "app/hiro/ulike/patches/UlikeHqMaxPatch.class"
NATIVE_INSTALLER = "app/hiro/ulike/patches/IntegrationPayload186.class"
NR_ENTRY = "ulike1957/runtime/libulike_strong1957.so"
ALLOWED_ADDED = {NR_ENTRY}
ALLOWED_CHANGED = {MF, RUNTIME, "classes.dex", ULIKE_LOADER, NATIVE_INSTALLER}
PRODUCTION = ["QualityPipeline1932", "StrongNoise1957", "ProcessingTiming1947"]
SELECTED = ["NR5", "NR6", "NR7", "NR8"]
TOOLCHAIN_SHA256 = {
    "morphe.jar": "82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c",
    "android.jar": "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b",
    "d8.jar": "305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5",
}
DIAGNOSTIC_STAGES = ["fusion", "noise", "correction", "compression", "save"]
HOST_FLAGS = ["strong_single_image_algorithm_executed", "strong_final_save_pipeline_executed",
    "chroma_on_off_nr_applied", "legacy_primary_and_residual_disabled_in_new_save_path",
    "captured_noise_off_options_preserved", "rotation_and_resize_dimensions_preserved",
    "source_bitmap_immutable", "prepared_detail_duplicate_suppressed",
    "completion_capture_identity_preserved", "copy_oom_failure_rollback_preserved",
    "concurrent_shot_options_isolated", "immutable_streaming_halos_equal_whole_image",
    "private_nr_write_failure_discarded", "f16_wide_colour_gainmap_outside_integer_nr",
    "final_sharpen_flat_noise_non_amplification_checked", "fresh_production_c_jni_executed",
    "fresh_production_c_jni_pipeline_executed", "native_java_pixel_equivalence",
    "forced_native_unavailable_java_fallback_executed",
    "strong_stage_partial_write_failure_discarded", "strong_stage_partial_write_oom_discarded", "strong_model_memory_admission_checked", "alpha_preservation_checked"]
REQUIRED_QA = {
    "schema": "ulike1957-strong-single-noise-v1", "ulike_version": VERSION, "bundle_version": BUNDLE,
    "baseline_ulike_version": PREVIOUS_APP, "baseline_bundle_version": PREVIOUS,
    "selected_candidates": SELECTED, "approved_lineage": "ULike1.8.8",
    "input_sha256": {SINGLE_BASE_NAME: SINGLE_BASE_SHA256, BASE_NAME: BASE_SHA256, **TOOLCHAIN_SHA256},
    "host_quality_passed": True, "host_quality_result.status": "passed",
    "host_quality_result.selected": SELECTED, "host_quality_result.pixel_equivalence_to_baseline": False,
    "host_quality_result.physical_android_tested": False,
    "host_quality_result.device_speedup_verified": False,
    "host_quality_result.device_quality_improvement_verified": False,
    "host_quality_result.native_parity_pixel_differences": 0,
    "host_quality_result.host_native_build.fresh_build": True,
    "host_quality_result.host_native_build.cached_native_binary_used": False,
    "host_pixel_equivalence_to_baseline": False, "intentional_quality_algorithm_change": True,
    "capture_fusion_enabled": False, "capture_begin_image_false_return_verified": True,
    "capture_class_bytecode_identical": True,
    "preserved_helper_roots": ['AsyncSave1935', 'CodecDrain1945', 'CorePixels1950', 'QualityShadow1932', 'SaveQueue1935', 'SingleNoise1955', 'SpeedWorkers1935', 'WholeRoute1953'],
    "h28_h33_inherited_helpers_bytecode_identical": True, "nr1_nr4_helper_bytecode_identical": True,
    "save_publication_hooks_preserved": True, "production_source_consistency_verified": True, "unrelated_runtime_classes_bytecode_identical": True,
    "non_ulike_loader_classes_unchanged": True, "non_ulike_resources_byte_identical": True,
    "standalone_and_bundle_ulike_resources_identical": True,
    "inherited_native_libraries_byte_identical": True, "native_methods_byte_identical": True,
    "native_methods_tsv_byte_identical": True, "native_installer_existing_rows_preserved": True,
    "native_installer_preserved_rows": 9, "native_installer_total_rows": 10, "native_installer_inverse_verified": True,
    "resolution_and_save_format_preserved": True, "save_format_and_codec_configuration_preserved": True,
    "stage_timing_algorithm_preserved": True, "timing_version_only_change": True,
    "diagnostic_stage_names": DIAGNOSTIC_STAGES, "original_apk_apply_tested": False,
    "original_split_merge_tested": False, "ci_android_apply_tested": False,
    "device_tested": False, "device_quality_verified": False, "device_save_speed_measured": False,
    "jdk_identity.runtime_version": "21.0.8+9-LTS", "jdk_identity.vendor": "Eclipse Adoptium",
    "jdk_identity.javac_version": "21.0.8",
}
REQUIRED_QA.update({"host_quality_result." + key: True for key in HOST_FLAGS})
NUMERICAL_FLAGS = ('nr5_actual_image_pyramid_executed', 'nr5_correlated_coarse_noise_reduced', 'nr6_flat_dark_noise_reduced', 'nr6_edge_texture_protection_checked', 'nr7_wide_chroma_noise_reduced', 'nr7_colour_boundary_protection_checked', 'nr8_same_image_nlm_executed', 'nr8_bounded_search_checked')
REQUIRED_QA.update({"host_quality_result.reports." + report + "." + key: True
    for report in ("strong_single_image_algorithm", "strong_single_image_native") for key in NUMERICAL_FLAGS})
OTHER_APPS = ("Microsoft SwiftKey Beta", "Microsoft SwiftKey Beta Keyboard", "SwiftKey Beta",
              "Berry Browser", "Instagram", "X", "Twitter", "Trip.com", "TikTok",
              "Yahoo!乗換案内", "Y!乗換案内", "Amazonショッピング", "Amazon Shopping", "Hanull Reader",
              "Uber Eats", "BrightnessClick", "明るさタッチ", "簡単検索くん", "LINE")
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
INVENTORY = re.compile(r"(?m)^(ULike：)v(1\.9\.56)(（5\.6\.2／740）)\r?$")
SUMMARY = "NR5～NR8を統合。合成を使わず、1枚の画像ピラミッドで粗いノイズとムラを低減し、平坦な暗部を強く処理。色ムラを広い範囲で抑え、同じ写真内の似た小領域を使う範囲限定NLMを、ノイズが測定された平坦な暗部へ追加。NR1～NR4とH28～H33を基準に、保存解像度・美顔・補正・圧縮品質を保持。元APKSへの今回の適用とGalaxy実機の画質・速度は未確認。"

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()

def checked_path(name):
    p = PurePosixPath(name)
    require(name and not p.is_absolute() and ".." not in p.parts and "\\" not in name
            and str(p) == name and not name.endswith("/"),
            "Unsafe archive/repository path: " + name)
    return p

def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), "Duplicate archive paths")
        for name in names:
            checked_path(name.rstrip("/"))
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

def lookup_qa(data, dotted_path):
    for component in dotted_path.split("."):
        require(isinstance(data, dict) and component in data, "Missing QA field: " + dotted_path)
        data = data[component]
    return data

def api(path, data=None, method=None, absent=False):
    command = ["gh", "api", path]
    if method:
        command += ["--method", method]
    if data is not None:
        command += ["--input", "-"]
    result = subprocess.run(command, input=None if data is None else json.dumps(data),
                            text=True, capture_output=True, check=False)
    if result.returncode:
        if absent and data is None and "(HTTP 404)" in result.stderr:
            return None
        raise RuntimeError("GitHub API failure: " + result.stderr.strip())
    return json.loads(result.stdout) if result.stdout.strip() else None


def fetch(url):
    for attempt in range(4):
        try:
            request = urllib.request.Request(url, headers={
                "User-Agent": "Hiro-ULike1957-Publication",
                "Accept-Encoding": "identity", "Cache-Control": "no-cache",
            })
            with urllib.request.urlopen(request, timeout=45) as response:
                require(response.status == 200, "Public download did not return HTTP200")
                return response.read()
        except (OSError, urllib.error.URLError) as error:
            if attempt == 3 or isinstance(error, urllib.error.HTTPError) and error.code not in (404, 429, 500, 502, 503, 504):
                raise
            time.sleep(2 ** attempt)


def head(branch):
    return api(f"repos/{REPO}/git/ref/heads/{branch}")["object"]["sha"]


def content(path, ref):
    row = api(f"repos/{REPO}/contents/{path}?ref={ref}")
    require(row.get("encoding") == "base64", "Unexpected repository file encoding")
    return base64.b64decode(row["content"])


def snapshot(branch):
    current = head(branch)
    commit = api(f"repos/{REPO}/git/commits/{current}")
    return {"branch": branch, "head": current, "tree": commit["tree"]["sha"],
            "manifest": json.loads(content("patches-bundle.json", current)),
            "log": content("CHANGELOG.md", current).decode(),
            "ulike_policy": content(POLICY_PATH, current)}


def check_heads(states):
    for state in states.values():
        require(head(state["branch"]) == state["head"], "Concurrent update: " + state["branch"])


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
    with tempfile.TemporaryDirectory(prefix="ulike1957-index-") as temp:
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

def commit_feeds_atomically(repo, states, updates, message):
    require(set(states) == {"main", "dev"} and set(updates) == set(states), "Both feed branches are required")
    check_heads(states)
    git(repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"], states["dev"]["head"])
    prepared = {branch: prepare_commit(repo, state, updates[branch], message) for branch, state in states.items()}
    check_heads(states)
    # Each proposed commit has exactly the leased old HEAD as its sole parent.
    # The lease adds race protection; it does not permit rewriting old history.
    git(repo, "push", "--atomic",
        f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}",
        f"https://github.com/{REPO}.git",
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared)
    return prepared

def utc_created(states):
    now = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0)
    require(all(now > dt.datetime.fromisoformat(s["manifest"]["created_at"]) for s in states.values()),
            "Current UTC time must follow both old feed timestamps")
    return now.isoformat()


def is_ulike(name):
    return name.startswith("app/hiro/ulike/patches/") or re.match(r"^ulike(?:\d+)?/", name) is not None


def metadata(state, url, created, notes):
    old = state["manifest"]
    require("元APKSへの今回の適用は未実施" in notes,
            "Manager feed must disclose that this release was not applied to the original APKS")
    require(old["version"] == PREVIOUS and old["download_url"] == BASE_URL,
            "Another bundle or baseline source is active")
    require(dt.datetime.fromisoformat(created) > dt.datetime.fromisoformat(old["created_at"]), "created_at must advance")
    require(len(list(INVENTORY.finditer(old["description"]))) == 1, "Missing or ambiguous current ULike inventory")
    previous_description = INVENTORY.sub(lambda m: m[1] + "v" + VERSION + m[3], old["description"], count=1)
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": notes.strip() + "\n\n" + previous_description}
    require(feed["description"].startswith(notes.strip()) and
            "元APKSへの今回の適用は未実施" in feed["description"].split("\n\n" + previous_description)[0],
            "Current feed validation scope was lost")
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Release notes contain a competing version heading")
    changelog = f"# {BUNDLE} ({created[:10]})\n\n* **ULike:** v{VERSION}：{SUMMARY}\n\n" + notes.strip() + "\n\n" + state["log"]
    require(has_changes_for(changelog, PREVIOUS, ["ULike"]), "Missing ULike update scope")
    for installed in tuple("1.0."+str(v) for v in range(171,190)):
        require(has_changes_for(changelog, installed, ["ULike"]), "Prior ULike installation must receive a repatch update: " + installed)
    require(not has_changes_for(changelog, BUNDLE, ["ULike"]), "Freshly patched ULike would still be outdated")
    historical_scopes = {scope for entry in parsed_entries(state["log"]) for scope, _ in entry["bullets"]}
    other_apps = {app for app in set(OTHER_APPS) | historical_scopes
                  if app.casefold() != "ulike" and not app.casefold().startswith("ulike - ")}
    require(not any(has_changes_for(changelog, PREVIOUS, [app]) for app in other_apps), "False update scope for another app")
    entry = parsed_entries(changelog)[0]
    require(entry["version"] == BUNDLE and {scope for scope, _ in entry["bullets"]} == {"ULike"}, "Missing or unrelated app scope")
    require(changelog.endswith(state["log"]), "Prior change history was lost")
    # The exact old description is retained except for its one ULike inventory row.
    require(feed["description"].endswith(previous_description), "Prior description was lost")
    return feed, changelog


def entry_plan(expected, key):
    value = expected.get(key)
    require(isinstance(value, list) and all(isinstance(x, str) for x in value), "Missing entry plan: " + key)
    require(len(value) == len(set(value)), "Duplicate entry plan: " + key)
    for name in value:
        checked_path(name)
    return set(value)


def configure(expected):
    pins = {"ulike_version": VERSION, "bundle_version": BUNDLE,
            "baseline_ulike_version": PREVIOUS_APP, "baseline_bundle_version": PREVIOUS,
            "baseline_ulike_sha256": SINGLE_BASE_SHA256, "baseline_bundle_sha256": BASE_SHA256,
            "baseline_ulike_bytes": SINGLE_BASE_BYTES, "baseline_bundle_bytes": BASE_BYTES,
            "baseline_ulike_url": SINGLE_BASE_URL, "baseline_bundle_url": BASE_URL,
            "selected_candidates": SELECTED, "production_helper_roots": PRODUCTION,
            "toolchain_sha256": TOOLCHAIN_SHA256, "ndk_revision": "27.2.12479018",
            "jdk_runtime_version": "21.0.8+9-LTS", "jdk_vendor": "Eclipse Adoptium",
            "android_device_tested": False, "original_apk_apply_tested": False}
    for key, value in pins.items():
        require(type(expected.get(key)) is type(value) and expected[key] == value,
                "Reviewed declaration differs: " + key)


def load_expected(path):
    expected = json.loads(path.read_text())
    configure(expected)
    require(expected.get("schema") == "ulike1957-publication-v1", "Wrong publication manifest schema")
    require(expected.get("change_plan_reviewed") is True and expected.get("qa_contract_reviewed") is True,
            "Missing reviewed change and QA declaration")
    require(set(expected.get("artifacts", {})) == set(ASSETS), "Pin exactly six release assets")
    for name, row in expected["artifacts"].items():
        require(type(row.get("bytes")) is int and row["bytes"] > 0
                and re.fullmatch(r"[0-9a-f]{64}", row.get("sha256", "")) is not None
                and row["sha256"] != "0" * 64, "Unfilled artifact pin: " + name)
    for kind in ("bundle", "standalone"):
        require(entry_plan(expected, "allowed_changed_" + kind + "_entries") == ALLOWED_CHANGED,
                "Unexpected reviewed resource edit")
        require(entry_plan(expected, "allowed_added_" + kind + "_entries") == ALLOWED_ADDED,
                "Exactly one new optional native NR resource is required")
    contract = expected.get("qa_required_values")
    require(isinstance(contract, dict), "Missing semantic QA contract")
    for key, value in REQUIRED_QA.items():
        require(type(contract.get(key)) is type(value) and contract[key] == value,
                "Missing required semantic QA value: " + key)
    require(type(contract.get("host_quality_result.assertions")) is int
            and contract["host_quality_result.assertions"] > 0, "Pin positive executed host assertions")
    require(contract.get("replaced_helper_roots") == sorted(PRODUCTION), "Wrong production helper roots")
    for key in ("changed_runtime_methods", "changed_native_methods", "new_helper_classes",
                "new_runtime_aliases", "new_native_methods", "new_jni_methods", "new_runtime_methods", "removed_runtime_methods"):
        rows = contract.get(key)
        require(isinstance(rows, list) and len(rows) == len(set(rows))
                and all(isinstance(row, str) and row.startswith("L") and ";" in row for row in rows),
                "Missing exact DEX inventory: " + key)
    require(bool(contract["changed_runtime_methods"]) and bool(contract["new_helper_classes"]),
            "Actual changed and new implementation inventory is required")
    paths = expected.get("required_source_paths")
    require(isinstance(paths, list) and paths and len(paths) == len(set(paths)), "Missing source inventory")
    for name in paths:
        checked_path(name)
        require(name.startswith("src/"), "Source inventory outside src")
    required = {"src/" + root + ".java" for root in PRODUCTION} | {
        "src/build1957.py", "src/validate1957.py", "src/finalize1957.py", "src/host_nr1957.py", "src/host_nr1955.py",
        "src/Transform1957.java", "src/PatchClass1957.java", "src/PatchLoader1957.java",
        "src/VerifyHelperReferences.java", "src/SingleNoise1955.java", "src/tests/StrongNoise1957Test.java", "src/tests/PipelineStrongNoise1957Test.java", "src/native1957/build_native1957.py"}
    require(required <= set(paths), "Incomplete reproducible NR build sources")
    native = expected.get("nr_native_payload", {})
    require(type(native.get("bytes")) is int and native["bytes"] > 0
            and re.fullmatch(r"[0-9a-f]{64}", native.get("sha256", "")) is not None,
            "Pin new optional native payload")
    inherited = expected.get("inherited_native_payloads", {})
    require(isinstance(inherited, dict) and len(inherited) == 9, "Pin all nine inherited native payloads")
    return expected


def validate_entry_delta(old, new, expected, kind):
    removed, added = set(old) - set(new), set(new) - set(old)
    changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
    require(not removed and changed == entry_plan(expected, "allowed_changed_" + kind + "_entries")
            and added == entry_plan(expected, "allowed_added_" + kind + "_entries"),
            "Unexpected " + kind + " resource delta")
    return changed, added


def validate_local(dist, expected, baseline, standalone_baseline, repo):
    require(len(baseline) == BASE_BYTES and sha(baseline) == BASE_SHA256, "Wrong bundle baseline")
    require(len(standalone_baseline) == SINGLE_BASE_BYTES and sha(standalone_baseline) == SINGLE_BASE_SHA256,
            "Wrong standalone baseline")
    payloads = {}
    for name, row in expected["artifacts"].items():
        raw = (dist / name).read_bytes()
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Artifact differs from reviewed bytes: " + name)
        payloads[name] = raw
    old, new = archive(baseline), archive(payloads[COMBINED])
    old_single, single = archive(standalone_baseline), archive(payloads[SINGLE])
    for items, version in ((old, PREVIOUS), (old_single, PREVIOUS_APP), (new, BUNDLE), (single, VERSION)):
        require(headers(items[MF])["Version"] == version, "MPP version mismatch")
        require(items.get("classes.dex", b"").startswith(b"dex\n")
                and items.get(RUNTIME, b"").startswith(b"dex\n"), "Invalid DEX payload")
    require(headers(new[MF]).get("Name") == headers(old[MF]).get("Name")
            and headers(single[MF]).get("Name") == headers(old_single[MF]).get("Name"), "MPP source name changed")
    changed, added = validate_entry_delta(old, new, expected, "bundle")
    single_changed, single_added = validate_entry_delta(old_single, single, expected, "standalone")
    own = lambda items: {name: raw for name, raw in items.items() if is_ulike(name)}
    other = lambda items: {name: raw for name, raw in items.items() if not is_ulike(name) and name not in (MF, "classes.dex")}
    require(own(old) == own(old_single) and own(new) == own(single), "Standalone/bundle ULike payload mismatch")
    require(other(old) == other(new), "Non-ULike resource changed")
    require(all(is_ulike(name) or name in (MF, "classes.dex") for name in single), "Foreign standalone app payload")
    qa = json.loads(payloads[QA_NAME])
    for key, value in expected["qa_required_values"].items():
        actual = lookup_qa(qa, key)
        require(type(actual) is type(value) and actual == value, "QA assertion failed: " + key)
    host = qa["host_quality_result"]
    reports = host.get("reports", {})
    require({"existing_single_image_algorithm", "strong_single_image_algorithm", "strong_final_save_pipeline", "strong_single_image_native", "strong_final_save_pipeline_native"}
            == reports.keys(), "Missing executed NR Java/JNI suites")
    require(type(host.get("native_parity_cases")) is int and host["native_parity_cases"] >= 5, "At least five native parity cases required")
    for name, report in reports.items():
        require((report.get("status") == "passed" or report.get("passed") is True)
                and type(report.get("assertions")) is int and report["assertions"] > 0,
                "Unexecuted or failed host report: " + name)
    require(sum(row["assertions"] for row in reports.values()) == host["assertions"], "Host assertion total differs")
    require(sha(json.dumps(host, sort_keys=True).encode()) == qa["host_quality_result_sha256"], "Host result digest differs")
    inherited = {name: {"sha256": sha(raw), "bytes": len(raw)} for name, raw in old_single.items()
                 if name.endswith(".so") or name == "ulike186/runtime/0000.bin"}
    require(inherited == expected["inherited_native_payloads"] == qa["inherited_native_payloads"]
            and all(single[name] == old_single[name] for name in inherited), "Inherited native bytes changed")
    require(all(single[name] == old_single[name] for name in ("ulike/methods.dex", "ulike/methods.tsv")),
            "Legacy native methods changed")
    native = single[NR_ENTRY]
    require(native[:6] == b"\x7fELF\x02\x01" and native[18:20] == b"\xb7\x00", "NR payload is not ELF64 AArch64")
    require(expected["nr_native_payload"] == {"sha256": sha(native), "bytes": len(native)}
            and qa["nr_native_library_sha256"] == sha(native) and qa["nr_native_library_bytes"] == len(native),
            "NR native fingerprint differs")
    require(qa["nr_native_build"]["ndk_revision"] == expected["ndk_revision"], "NDK provenance differs")
    for name in (SINGLE, COMBINED):
        require(qa["artifacts"].get(name) == {"sha256": sha(payloads[name]), "bytes": len(payloads[name])},
                "QA MPP fingerprint differs")
    sums = {}
    for line in payloads["SHA256SUMS.txt"].decode().splitlines():
        match = re.fullmatch(r"([0-9a-f]{64}) [ *](.+)", line)
        require(match is not None, "Malformed checksum line")
        digest, name = match.groups()
        require(name in payloads and name != "SHA256SUMS.txt" and name not in sums and sha(payloads[name]) == digest,
                "Unknown, duplicate or wrong checksum entry")
        sums[name] = digest
    require(set(sums) == set(ASSETS) - {"SHA256SUMS.txt"}, "Checksums omit an asset")
    sources = archive(payloads[SOURCE_ZIP])
    require(set(expected["required_source_paths"]) <= sources.keys(), "Source archive incomplete")
    evidence_bytes = sources.get("evidence/validation.json", b"")
    desktop = json.loads(evidence_bytes)
    require(desktop.get("schema") == "ulike1957-desktop-validation-v1"
            and desktop.get("status") == "passed" and desktop.get("artifacts") == qa["artifacts"]
            and desktop.get("bundle_version") == BUNDLE and desktop.get("original_apk_apply_tested") is False
            and desktop.get("device_tested") is False and desktop.get("device_quality_verified") is False
            and qa.get("desktop_evidence_sha256") == sha(evidence_bytes), "Packaged desktop evidence differs")
    source_hashes = qa.get("source_sha256", {})
    require(set(expected["required_source_paths"]) <= source_hashes.keys(), "Missing source hash inventory")
    for name, digest in source_hashes.items():
        checked_path(name)
        require(name in sources and sha(sources[name]) == digest, "Packaged source checksum differs: " + name)
        if name.startswith(("src/", "publication/")):
            path = repo / RELEASE_ROOT / name
            require(path.is_file() and path.read_bytes() == sources[name], "Source archive differs from checkout: " + name)
    require(not any(PurePosixPath(name).suffix.casefold() in
                    (".apk", ".apks", ".aab", ".jks", ".keystore", ".pem", ".p12", ".pfx", ".png", ".jpg", ".jpeg", ".heic", ".heif")
                    for name in sources), "Private app, key or photo included in source archive")
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(all(value in notes for value in (VERSION, PREVIOUS_APP, BUNDLE, *SELECTED, "合成", "ノイズ",
                "補正", "圧縮", "保存", "元APKSへの今回の適用は未実施", "実機", "未確認", "再適用")),
            "Release notes omit scope or validation limitation")
    require(not re.search(r"\b(?:TODO|PLACEHOLDER|TBD)\b|レビュー後に確定", notes), "Unfilled release notes")
    return payloads, notes, {"bundle_changed_entries": sorted(changed), "bundle_added_entries": sorted(added),
            "standalone_changed_entries": sorted(single_changed), "standalone_added_entries": sorted(single_added),
            "non_ulike_resources_unchanged": len(other(new)), "ulike_resources_identical": len(own(new)),
            "native_methods_byte_identical": True, "native_methods_tsv_byte_identical": True,
            "baseline_other_app_resources_byte_identical": True,
            "qa_assertions_verified": list(expected["qa_required_values"])}
def validate_policy(raw):
    policy = json.loads(raw)
    require(policy.get("ulike_version") == PREVIOUS_APP, "Active ULike advanced")
    require(policy.get("source_sha256") == SINGLE_BASE_SHA256, "Active ULike source bytes differ")
    require(policy.get("approved_lineage_version") == "1.8.8" and "1.9.17" in policy.get("withdrawn_versions", []), "Approved ULike lineage differs")
    return policy


def new_policy(old, expected):
    qa = expected["qa_required_values"]
    inherited_nr = expected["inherited_native_payloads"]["ulike1955/runtime/libulike_nr1955.so"]
    policy = {**old, "ulike_version": VERSION, "bundle_version": BUNDLE, "source_filename": SINGLE,
        "source_sha256": expected["artifacts"][SINGLE]["sha256"], "source_release": TAG, "active_release": TAG,
        "base_ulike_version": PREVIOUS_APP, "base_bundle_version": PREVIOUS, "base_source_sha256": SINGLE_BASE_SHA256,
        "reason": "NR5-NR8 add an actual image pyramid, stronger flat dark-area filtering, wide chroma smoothing and bounded quarter-scale same-image non-local means in measured-noise dark flat regions after the retained optimized NR1-NR4 stage. All nine inherited native libraries and unrelated app/runtime classes are unchanged. Host/source/package checks pass; original APKS and physical Galaxy quality/speed remain unverified.",
        "future_ulike_base": "Use ULike1.9.57 NR5-NR8 on exact1.9.56/1.0.189 from approved1.8.8 lineage. Preserve single-image capture and false-return beginImage admission, NR1-NR4, H28-H33 retained helpers/natives, beauty/color, dimensions, per-shot noise-off, compression settings, FIFO encoding/publication, camera recovery and codec ownership. Strong processing targets eligible sRGB ARGB8888/RGB565 saved images and preserves high-depth/wide-colour/gainmap fallback. Validate original APKS and physical Galaxy separately.",
        "selected_candidates": SELECTED, "current_fix_scope": SELECTED,
        "retained_baseline_candidates": {"ulike_version": PREVIOUS_APP,
            "candidates": list(dict.fromkeys(old.get("retained_baseline_candidates", {}).get("candidates", []) + old.get("selected_candidates", [])))},
        "release_status": "strong_single_image_nr_host_verified_original_apks_device_unverified",
        "capture_fusion_enabled": False, "single_image_only": True,
        "capture_begin_image_false_return_verified": True, "capture_class_bytecode_identical": True,
        "quality_algorithm_and_settings_preserved": False, "intentional_quality_algorithm_change": True,
        "nr1_nr4_preserved": True, "h28_h33_helpers_and_native_payloads_preserved": True,
        "host_pixel_equivalence_to_baseline": False, "host_optimized_kernel_pixel_equivalence_verified": False,
        "gpu_processing_added": False, "gpu_shader_execution_on_host": False,
        "gpu_execution_on_physical_android": False,
        "gpu_policy": "Existing GPU resources, dispatch certification, bounded scratch ownership, admission and CPU fallback are byte-preserved. NR5-NR8 add independent CPU/JNI single-image processing.",
        "android_device_tested": False, "device_quality_verified": False, "device_save_speed_measured": False,
        "original_apk_apply_tested": False, "ci_rebuild_matches_checked_out_source": True,
        "ci_rebuild_matches_local_tested_artifacts": False, "all_existing_runtime_unchanged": False,
        "baseline_other_apps_preserved": True, "native_installer_existing_row_count_preserved": False,
        "native_installer_existing_rows_preserved": True, "native_installer_preserved_rows": 9,
        "native_installer_updated_rows": 0, "native_installer_total_rows": 10,
        "nr_native_library_sha256": inherited_nr["sha256"], "nr_native_library_bytes": inherited_nr["bytes"],
        "strong_nr_native_library_sha256": expected["nr_native_payload"]["sha256"],
        "strong_nr_native_library_bytes": expected["nr_native_payload"]["bytes"],
        "diagnostic_stage_names": DIAGNOSTIC_STAGES, "diagnostic_overlapping_wall_clock": True,
        "diagnostic_measurement_scope": "Single-image noise/additional correction/compression/save intervals; fusion remains disabled. Encoder start-to-stop excludes construction/prewarm, and native standard beauty and capture wait are outside stage measurements. Overall elapsed starts at trace admission, not necessarily at the shutter button."}
    for key, value in qa.items():
        if "." not in key and (key.endswith("_byte_identical") or key.endswith("_preserved")
                              or key.startswith("diagnostic_") or key.startswith("legacy_report_")):
            policy[key] = value
    policy.update(native_library_byte_identical=True, inherited_native_libraries_byte_identical=True,
                  native_installer_byte_identical=False, stage_timing_payload_byte_identical=False,
                  capture_save_policies_byte_identical=True, quality_pixel_math_byte_identical=False,
                  image_pipeline_byte_identical=False, speed_and_save_helpers_byte_identical=True,
                  save_encoding_helpers_byte_identical=True)
    for key in ("black_tap_native_switch", "black_tap_disabled", "front_input_readiness_timeout_ms",
                "shutter_feedback_duration_ms", "shutter_feedback_device_tested"):
        require(policy.get(key) == old.get(key) and (key in policy) == (key in old),
                "Inherited camera/feedback policy changed: " + key)
    require(policy.get("shutter_feedback_duration_ms") == 320, "320ms visible feedback required")
    return policy
def run(command):
    subprocess.run(list(map(str, command)), check=True)


def verify_assets(release_id, expected, public):
    release = api(f"repos/{REPO}/releases/{release_id}")
    require(release["tag_name"] == TAG and release["draft"] is not public and release["prerelease"] is True,
            "Release identity or visibility differs")
    assets = {asset["name"]: asset for asset in release["assets"]}
    require(set(assets) == set(expected) and len(assets) == len(release["assets"]), "Release asset inventory differs")
    for name, row in expected.items():
        asset = assets[name]
        require(asset["size"] == row["bytes"] and asset["state"] == "uploaded", "Release asset size/state differs: " + name)
        if asset.get("digest"):
            require(asset["digest"] == "sha256:" + row["sha256"], "GitHub asset digest differs: " + name)
        raw = fetch(asset["browser_download_url"]) if public else subprocess.check_output(
            ["gh", "api", f"repos/{REPO}/releases/assets/{asset['id']}", "-H", "Accept: application/octet-stream"])
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Downloaded release bytes differ: " + name)


def verify_completed(states, expected, payloads, args):
    """An exact completed retry only reads and verifies; partial runs fail closed."""
    if not all(state['manifest'].get('version') == BUNDLE for state in states.values()):
        return False
    require(states['main']['manifest'] == states['dev']['manifest'], 'Completed feed branches differ')
    receipts = {branch: content(RELEASE_ROOT + '/' + RECEIPT_NAME, state['head'])
                for branch, state in states.items()}
    require(receipts['main'] == receipts['dev'], 'Completed publication receipts differ')
    receipt = json.loads(receipts['main'])
    require(receipt.get('status') == 'published_and_verified' and receipt.get('published') is True
            and receipt.get('ulike_version') == VERSION and receipt.get('bundle_version') == BUNDLE
            and receipt.get('assets') == expected['artifacts']
            and receipt.get('publication_manifest_sha256') == sha(args.expected.read_bytes()),
            'Existing publication differs; inspect before retry')
    release = api(f'repos/{REPO}/releases/tags/{TAG}', absent=True)
    require(release is not None, 'Completed receipt has no matching Release')
    verify_assets(release['id'], {**expected['artifacts'], RECEIPT_NAME:
                  {'bytes': len(receipts['main']), 'sha256': sha(receipts['main'])}}, True)
    for state in states.values():
        feed = state['manifest']
        policy = json.loads(state['ulike_policy'])
        require(feed.get('page_url') == PAGE and feed.get('download_url') == receipt['download_urls'][COMBINED]
                and fetch(feed['download_url']) == payloads[COMBINED], 'Completed Manager download differs')
        require(policy.get('ulike_version') == VERSION and policy.get('bundle_version') == BUNDLE
                and policy.get('source_sha256') == expected['artifacts'][SINGLE]['sha256']
                and policy.get('capture_fusion_enabled') is False and policy.get('single_image_only') is True,
                'Completed active policy differs')
    check_heads(states)
    print(json.dumps({'status': 'already_published_and_verified_no_remote_writes',
                      'release_url': PAGE, 'heads': {branch: state['head'] for branch, state in states.items()}},
                     ensure_ascii=False, indent=2))
    return True


def publication(args):
    expected = load_expected(args.expected)
    baseline = args.baseline.read_bytes() if args.baseline else fetch(BASE_URL)
    standalone_baseline = args.standalone_baseline.read_bytes() if args.standalone_baseline else fetch(SINGLE_BASE_URL)
    payloads, notes, report = validate_local(args.dist, expected, baseline, standalone_baseline, args.repo)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **report}, ensure_ascii=False, indent=2))
        return
    states = {branch: snapshot(branch) for branch in ("main", "dev")}
    check_heads(states)
    if verify_completed(states, expected, payloads, args):
        return
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    require(all(state["manifest"]["version"] == PREVIOUS for state in states.values()), "Active bundle has advanced")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow checkout")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    require(sha(fetch(states["main"]["manifest"]["download_url"])) == BASE_SHA256, "Active bundle bytes differ")
    policies = {branch: validate_policy(content(POLICY_PATH, state["head"])) for branch, state in states.items()}
    created = utc_created(states)
    for state in states.values():
        metadata(state, "https://example.invalid/preflight-only", created, notes)
    require(api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/tags/{TAG}", absent=True) is None, "Release tag already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None, "Distribution branch exists; inspect before retry")
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes", "heads": {b: s["head"] for b, s in states.items()}, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "ulike1957-publication-receipt-v1", "ulike_version": VERSION, "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "baseline_ulike_version": PREVIOUS_APP, "baseline_ulike_sha256": SINGLE_BASE_SHA256,
               "source_commit": os.environ.get("GITHUB_SHA") or git(args.repo, "rev-parse", "HEAD"),
               "publication_manifest_sha256": sha(args.expected.read_bytes()),
               "assets": expected["artifacts"], "local_validation": report, "android_device_tested": False,
               "original_apk_apply_tested": expected["original_apk_apply_tested"], "manager_parser_source": MANAGER_SOURCE,
               "manager_device_update_badge_observed": False, "operations": []}

    def checkpoint(status, **changes):
        receipt.update(status=status, **changes)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)

    checkpoint("preflight_passed")
    try:
        check_heads(states)
        release = api(f"repos/{REPO}/releases", {"tag_name": TAG, "target_commitish": states["main"]["head"],
            "name": f"ULike v{VERSION} NR5～NR8 単写ノイズ低減 / Hiro Morphe v{BUNDLE}",
            "body": notes, "draft": True, "prerelease": True, "make_latest": "false"})
        checkpoint("draft_created", release_id=release["id"])
        for name in ASSETS:
            run(["gh", "release", "upload", TAG, "--repo", REPO, args.dist / name])
        verify_assets(release["id"], expected["artifacts"], False)
        checkpoint("draft_assets_verified")
        check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}", {"draft": False, "prerelease": True, "make_latest": "false"}, "PATCH")
        verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("release_bytes_verified")
        check_heads(states)
        git(args.repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"])
        raw_state = prepare_commit(args.repo, {**states["main"], "branch": RAW_BRANCH},
            {"downloads/" + name: payloads[name] for name in (SINGLE, COMBINED)},
            f"release: exact ULike1957 and bundle{BUNDLE} immutable MPP downloads")
        git(args.repo, "push", f"--force-with-lease=refs/heads/{RAW_BRANCH}:",
            f"https://github.com/{REPO}.git", f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH) == raw_state["head"], "Distribution branch moved")
        immutable_urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE, COMBINED)}
        for name, url in immutable_urls.items():
            require(fetch(url) == payloads[name], "Immutable download mismatch: " + name)
        checkpoint("immutable_downloads_verified", raw_commit=raw_state["head"], download_urls=immutable_urls)
        check_heads(states)
        new_metadata, updates, expected_policies = {}, {}, {}
        for branch, state in states.items():
            feed, changelog = metadata(state, immutable_urls[COMBINED], created, notes)
            new_metadata[branch] = (feed, changelog)
            policy = new_policy(policies[branch], expected)
            expected_policies[branch] = policy
            updates[branch] = {"patches-bundle.json": json_bytes(feed), "CHANGELOG.md": changelog.encode(), POLICY_PATH: json_bytes(policy)}
            if branch == "main":
                for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                    updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds_atomically(args.repo, states, updates,
            f"release: ULike1957 strong single-image NR5-NR8 noise reduction and bundle{BUNDLE} Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(json.loads(content(POLICY_PATH, state["head"])) == expected_policies[branch], "Remote ULike policy mismatch")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?ulike1957={state['head']}")) == feed, "Public branch feed mismatch")
            require(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?ulike1957={state['head']}").decode() == changelog, "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_verified": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("published_and_verified", published=True, release_url=PAGE, manager_main_dev_updated_atomically=True,
            manager_ulike_update_eligible=True, previous_ulike1956_repatch_eligible=True,
            other_apps_false_updates=False, baseline_other_apps_preserved=True, fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds_atomically(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified ULike1957 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(json.loads(content(POLICY_PATH, state["head"])) == expected_policies[branch], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0],
                    "Feed changed while saving receipt")
        run(["gh", "release", "upload", TAG, "--repo", REPO, receipt_path])
        published_assets = {**expected["artifacts"], RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}
        verify_assets(release["id"], published_assets, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, ULike scope, baseline bundle other-app retention and saved receipt")
    except BaseException as error:
        checkpoint("failed_requires_inspection", error=type(error).__name__ + ": " + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", required=True, type=Path)
    parser.add_argument("--repo", required=True, type=Path)
    parser.add_argument("--expected", required=True, type=Path)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--standalone-baseline", type=Path)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true")
    mode.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("dist", "repo", "expected", "baseline", "standalone_baseline"):
        if getattr(args, key) is not None:
            setattr(args, key, getattr(args, key).resolve())
    require(not args.local_only or (args.baseline is not None and args.standalone_baseline is not None), "--local-only requires both local baselines")
    publication(args)


if __name__ == "__main__":
    main()
