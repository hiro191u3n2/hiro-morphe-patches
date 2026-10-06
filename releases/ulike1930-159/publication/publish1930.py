#!/usr/bin/env python3
"""Publish reviewed ULike 1.9.30 / bundle 1.0.159 without regressing SwiftKey183.

The independent --expected manifest pins all six artifacts and the exact QA
contract. Read-only --local-only and --preflight-only never write to GitHub.
Both feed branches advance atomically to direct descendants of the leased HEADs.
The checkout, its normal index, and all unrelated repository paths are preserved.
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
import subprocess
import sys
import tempfile
import zipfile

REPO = "hiro191u3n2/hiro-morphe-patches"
VERSION, PREVIOUS_APP, PREVIOUS, BUNDLE = "1.9.30", "1.9.29", "1.0.158", "1.0.159"
TAG, RAW_BRANCH = "ulike-v1.9.30", "release/ulike1930-159"
SINGLE = "ULike_HQ_Texture_Online_v1.9.30.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.159.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.158.mpp"
BASE_SHA256 = "4d0dca101976cfdd14b48949ec7f828c11b9c0dcf2e0c01e99a379dd6898aded"
BASE_BYTES = 17281055
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"fec446ac275d92a7b366ca83a3b63d5ad6dddf0e/downloads/{BASE_NAME}")
SINGLE_BASE_NAME = "ULike_HQ_Texture_Online_v1.9.29.mpp"
SINGLE_BASE_SHA256 = "2faae3114d5663cfb924f544b7c114f910caadc5b517eb575ea38b1d89311f6c"
SINGLE_BASE_BYTES = 705976
SINGLE_BASE_URL = f"https://raw.githubusercontent.com/{REPO}/906600c1098fdd412ce8b22018929850e2543d14/downloads/{SINGLE_BASE_NAME}"
QA_NAME = "QA_ULike_v1.9.30.json"
SOURCE_ZIP = "ULike_v1.9.30_sources_and_QA.zip"
RECEIPT_NAME = "publication_ULike_v1.9.30.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/ulike1930-159"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
MF = "META-INF/MANIFEST.MF"
RUNTIME = "ulike/runtime.dex"
ULIKE_LOADER = "app/hiro/ulike/patches/UlikeHqMaxPatch.class"
ALLOWED_CHANGED = {MF, RUNTIME, "classes.dex", ULIKE_LOADER}
REQUIRED_CHANGED = {MF, RUNTIME}
IMMUTABLE_NATIVE = ("ulike/methods.dex", "ulike/methods.tsv")
REQUIRED_QA = {
    "schema": "ulike1930-rear-lens-ui-v1",
    "ulike_version": VERSION,
    "bundle_version": BUNDLE,
    "changed_runtime_methods": [
        "Lcom/hiro/ulike/OpticalZoom;->select(I)V",
        "Lcom/hiro/ulike/OpticalZoom;->show()Z",
        "Lcom/hiro/ulike/OpticalZoomUi$Bar;->onPreDraw()Z",
    ],
    "new_helper_classes": ["Lcom/hiro/ulike/RearLensUi1930;"],
    "camera_lifecycle_byte_identical": True,
    "black_bar_gesture_byte_identical": True,
    "device_tested": False,
    "device_fix_confirmed": False,
    "native_methods_byte_identical": True,
    "native_methods_tsv_byte_identical": True,
    "non_ulike_resources_byte_identical": True,
    "non_ulike_loader_classes_unchanged": True,
    "image_pipeline_byte_identical": True,
    "rear_lens_ui_predraw_guard": True,
    "rear_lens_ui_selection_guard": True,
    "standalone_and_bundle_ulike_resources_identical": True,
}
OTHER_APPS = ("Microsoft SwiftKey Beta", "SwiftKey Beta", "Berry Browser", "Instagram", "X", "Trip.com", "TikTok",
              "Yahoo!乗換案内", "Amazonショッピング", "Hanull Reader", "Uber Eats", "BrightnessClick")
HELPER = "releases/ulike-rollback188-142/rollback142.py"
HELPER_BLOB_SHA = "e7771a4868d7a798217304af6d6099bcded5569e"
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
INVENTORY = re.compile(r"(?m)^(ULike：)v(1\.9\.29)(（5\.6\.2／740）)\r?$")
SUMMARY = "インカメラ時の倍率・接写ボタン残留を修正。次の描画前の非表示と残留クリック拒否。既存画質・他アプリを保持。実機未確認。"


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

def lookup_qa(data, dotted_path):
    for component in dotted_path.split("."):
        require(isinstance(data, dict) and component in data, "Missing QA field: " + dotted_path)
        data = data[component]
    return data

def load_helper(repo):
    path = repo / HELPER
    raw = path.read_bytes()
    git_sha = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    require(git_sha == HELPER_BLOB_SHA, "Pinned publication helper changed")
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location("ulike1930_publication_helper", path)
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
    with tempfile.TemporaryDirectory(prefix="ulike1930-index-") as temp:
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


def is_ulike(name):
    return name.startswith("app/hiro/ulike/patches/") or re.match(r"^ulike(?:\d+)?/", name) is not None


def metadata(state, url, created, notes):
    old = state["manifest"]
    require(old["version"] == PREVIOUS, "Another bundle is active")
    require(dt.datetime.fromisoformat(created) > dt.datetime.fromisoformat(old["created_at"]), "created_at must advance")
    require(len(list(INVENTORY.finditer(old["description"]))) == 1, "Missing or ambiguous current ULike inventory")
    previous_description = INVENTORY.sub(lambda m: m[1] + "v" + VERSION + m[3], old["description"], count=1)
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": notes.strip() + "\n\n" + previous_description}
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Release notes contain a competing version heading")
    changelog = f"# {BUNDLE} ({created[:10]})\n\n* **ULike:** v{VERSION}：{SUMMARY}\n\n" + notes.strip() + "\n\n" + state["log"]
    require(has_changes_for(changelog, PREVIOUS, ["ULike"]), "Missing ULike update scope")
    require(not has_changes_for(changelog, BUNDLE, ["ULike"]), "Freshly patched ULike would still be outdated")
    require(not any(has_changes_for(changelog, PREVIOUS, [app]) for app in OTHER_APPS), "False update scope for another app")
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


def load_expected(path):
    expected = json.loads(path.read_text())
    require(expected.get("schema") == "ulike1930-publication-v1", "Wrong publication manifest schema")
    for key, value in {"ulike_version": VERSION, "bundle_version": BUNDLE,
                       "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
                       "baseline_ulike_version": PREVIOUS_APP, "baseline_ulike_sha256": SINGLE_BASE_SHA256}.items():
        require(expected.get(key) == value, "Wrong reviewed publication value: " + key)
    require(expected.get("change_plan_reviewed") is True and expected.get("qa_contract_reviewed") is True,
            "Review the exact entry delta and QA contract before publishing")
    require(expected.get("android_device_tested") is False, "Galaxy execution is unverified")
    require(type(expected.get("original_apk_apply_tested")) is bool, "Original APK validation scope must be explicit")
    require(set(expected.get("artifacts", {})) == set(ASSETS), "Pin exactly the six release assets")
    for name, row in expected["artifacts"].items():
        require(type(row.get("bytes")) is int and row["bytes"] > 0, "Unfilled asset size: " + name)
        require(re.fullmatch(r"[0-9a-f]{64}", row.get("sha256", "")) is not None and row["sha256"] != "0" * 64,
                "Unfilled asset checksum: " + name)
    for kind in ("bundle", "standalone"):
        changed = entry_plan(expected, "allowed_changed_" + kind + "_entries")
        require(REQUIRED_CHANGED <= changed <= ALLOWED_CHANGED, "Change plan exceeds the ULike UI-only edit")
        require(not entry_plan(expected, "allowed_added_" + kind + "_entries"), "No new top-level MPP entries are expected")
    contract = expected.get("qa_required_values")
    require(isinstance(contract, dict), "Expected QA contract is absent")
    for key, value in REQUIRED_QA.items():
        require(type(contract.get(key)) is type(value) and contract[key] == value, "Missing semantic QA requirement: " + key)
    require(contract.get("original_apk_apply_tested") is expected["original_apk_apply_tested"], "APK validation scope differs")
    return expected


def validate_entry_delta(old, new, expected, kind):
    removed, added = set(old) - set(new), set(new) - set(old)
    changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
    require(not removed, kind + " entries were removed")
    require(changed == entry_plan(expected, "allowed_changed_" + kind + "_entries"), "Unexpected " + kind + " changed entries: " + repr(sorted(changed)))
    require(added == entry_plan(expected, "allowed_added_" + kind + "_entries"), "Unexpected " + kind + " added entries")
    if ULIKE_LOADER in changed:
        require(new[ULIKE_LOADER].startswith(b"\xca\xfe\xba\xbe"), "Invalid ULike JVM loader")
    return changed, added


def validate_local(dist, expected, baseline, standalone_baseline):
    require(len(baseline) == BASE_BYTES and sha(baseline) == BASE_SHA256, "Wrong bundle baseline bytes")
    require(len(standalone_baseline) == SINGLE_BASE_BYTES and sha(standalone_baseline) == SINGLE_BASE_SHA256, "Wrong ULike baseline bytes")
    payloads = {}
    for name, row in expected["artifacts"].items():
        raw = (dist / name).read_bytes()
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Artifact differs from reviewed bytes: " + name)
        payloads[name] = raw
    old, new = archive(baseline), archive(payloads[COMBINED])
    old_single, single = archive(standalone_baseline), archive(payloads[SINGLE])
    for items, version in ((old, PREVIOUS), (old_single, PREVIOUS_APP), (new, BUNDLE), (single, VERSION)):
        require(headers(items[MF])["Version"] == version, "MPP manifest version mismatch")
        require(items.get("classes.dex", b"").startswith(b"dex\n"), "Invalid patch-loader DEX")
        require(items.get(RUNTIME, b"").startswith(b"dex\n"), "Invalid ULike runtime DEX")
    require(headers(new[MF]).get("Name") == headers(old[MF]).get("Name"), "Bundle source name changed")
    require(headers(single[MF]).get("Name") == headers(old_single[MF]).get("Name"), "ULike source name changed")
    changed, added = validate_entry_delta(old, new, expected, "bundle")
    single_changed, single_added = validate_entry_delta(old_single, single, expected, "standalone")
    own = lambda items: {name: raw for name, raw in items.items() if is_ulike(name)}
    other = lambda items: {name: raw for name, raw in items.items() if not is_ulike(name) and name not in (MF, "classes.dex")}
    require(own(old) == own(old_single), "Bundle and standalone ULike baselines differ")
    require(own(new) == own(single), "Bundle and standalone ULike payloads differ")
    require(other(old) == other(new), "Non-ULike resources changed, including SwiftKey183")
    require(all(is_ulike(name) or name in (MF, "classes.dex") for name in single), "Standalone has another application's files")
    for name in IMMUTABLE_NATIVE:
        require(old[name] == new[name] == old_single[name] == single[name], "Native ULike methods changed: " + name)
    qa = json.loads(payloads[QA_NAME])
    for key, value in expected["qa_required_values"].items():
        actual = lookup_qa(qa, key)
        require(type(actual) is type(value) and actual == value, "QA assertion failed: " + key)
    for name in (SINGLE, COMBINED):
        row = qa.get("artifacts", {}).get(name, {})
        require(row.get("sha256") == sha(payloads[name]) and row.get("bytes") == len(payloads[name]), "QA MPP fingerprint mismatch: " + name)
    checksums = {}
    for line in payloads["SHA256SUMS.txt"].decode().splitlines():
        if not line.strip():
            continue
        match = re.fullmatch(r"([0-9a-f]{64}) [ *](.+)", line)
        require(match is not None, "Malformed SHA256SUMS line")
        digest, name = match.groups()
        require(name in payloads and name != "SHA256SUMS.txt" and name not in checksums, "Unknown or duplicate checksum entry")
        require(sha(payloads[name]) == digest, "SHA256SUMS mismatch: " + name)
        checksums[name] = digest
    require(set(checksums) == set(ASSETS) - {"SHA256SUMS.txt"}, "Checksums must cover all five other assets")
    sources = archive(payloads[SOURCE_ZIP])
    require(sources and any(name.endswith("RearLensUi1930.java") for name in sources), "Missing actual UI helper source")
    require(any(name.endswith("build1930.py") for name in sources), "Missing reproducible build source")
    require(not any(PurePosixPath(name).suffix.casefold() in (".apk", ".apks", ".aab", ".jks", ".keystore") for name in sources), "Source archive contains application binary or signing key")
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(all(value in notes for value in (VERSION, BUNDLE, "インカメラ", "描画", "未確認")), "Release notes omit scope or device uncertainty")
    report = {"bundle_changed_entries": sorted(changed), "bundle_added_entries": sorted(added),
              "standalone_changed_entries": sorted(single_changed), "standalone_added_entries": sorted(single_added),
              "non_ulike_resources_unchanged": len(other(new)), "ulike_resources_identical": len(own(new)),
              "native_methods_and_inventory_byte_identical": True, "swiftkey183_resources_byte_identical": True,
              "qa_assertions_verified": list(expected["qa_required_values"])}
    return payloads, notes, report


def validate_policy(raw):
    policy = json.loads(raw)
    require(policy.get("ulike_version") == PREVIOUS_APP, "Active ULike advanced")
    require(policy.get("source_sha256") == SINGLE_BASE_SHA256, "Active ULike source bytes differ")
    require(policy.get("approved_lineage_version") == "1.8.8" and "1.9.17" in policy.get("withdrawn_versions", []), "Approved ULike lineage differs")
    return policy


def new_policy(old, expected):
    return {**old, "ulike_version": VERSION, "bundle_version": BUNDLE, "source_filename": SINGLE,
            "source_sha256": expected["artifacts"][SINGLE]["sha256"], "source_release": TAG, "active_release": TAG,
            "base_ulike_version": PREVIOUS_APP, "base_bundle_version": PREVIOUS, "base_source_sha256": SINGLE_BASE_SHA256,
            "reason": "Hide rear-only zoom/macro UI before drawing whenever the live camera is front-facing or unavailable; reject stale lens clicks. Preserve native method payloads, image pipeline, previous fixes and SwiftKey183.",
            "future_ulike_base": "Use ULike1.9.30 from approved1.8.8 lineage; retain rear-only UI gating, prior lifecycle/facing memory and image processing fixes. Do not restore withdrawn1.9.17/10bit or removed diagnostics.",
            "release_status": "hotfix_device_unverified", "android_device_tested": False,
            "original_apk_apply_tested": expected["original_apk_apply_tested"], "ci_rebuild_matches_local_tested_artifacts": True,
            "rear_lens_ui_predraw_guard": True, "rear_lens_ui_selection_guard": True,
            "native_methods_byte_identical": True, "image_pipeline_byte_identical": True,
            "swiftkey183_preserved": True, "rear_lens_ui_device_fix_confirmed": False}


def publication(args):
    expected = load_expected(args.expected)
    helper = load_helper(args.repo)
    baseline = args.baseline.read_bytes() if args.baseline else helper.fetch(BASE_URL)
    standalone_baseline = args.standalone_baseline.read_bytes() if args.standalone_baseline else helper.fetch(SINGLE_BASE_URL)
    payloads, notes, report = validate_local(args.dist, expected, baseline, standalone_baseline)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **report}, ensure_ascii=False, indent=2))
        return
    states = {branch: helper.snapshot(branch) for branch in ("main", "dev")}
    helper.check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    require(all(state["manifest"]["version"] == PREVIOUS for state in states.values()), "Active bundle has advanced")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow checkout")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    require(sha(helper.fetch(states["main"]["manifest"]["download_url"])) == BASE_SHA256, "Active bundle bytes differ")
    policies = {branch: validate_policy(helper.content(POLICY_PATH, state["head"])) for branch, state in states.items()}
    created = utc_created(states)
    for state in states.values():
        metadata(state, "https://example.invalid/preflight-only", created, notes)
    require(helper.api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(helper.api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None, "Distribution branch exists; inspect before retry")
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes", "heads": {b: s["head"] for b, s in states.items()}, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "ulike1930-publication-receipt-v1", "ulike_version": VERSION, "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "baseline_ulike_version": PREVIOUS_APP, "baseline_ulike_sha256": SINGLE_BASE_SHA256,
               "assets": expected["artifacts"], "local_validation": report, "android_device_tested": False,
               "original_apk_apply_tested": expected["original_apk_apply_tested"], "manager_parser_source": MANAGER_SOURCE,
               "manager_device_update_badge_observed": False, "operations": []}

    def checkpoint(status, **changes):
        receipt.update(status=status, **changes)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)

    checkpoint("preflight_passed")
    try:
        helper.check_heads(states)
        release = helper.api(f"repos/{REPO}/releases", {"tag_name": TAG, "target_commitish": states["main"]["head"],
            "name": "ULike v1.9.30 インカメラの倍率ボタン非表示 / Hiro Morphe v1.0.159",
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
            "release: exact ULike1930 and bundle159 immutable MPP downloads")
        immutable_urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE, COMBINED)}
        for name, url in immutable_urls.items():
            require(helper.fetch(url) == payloads[name], "Immutable download mismatch: " + name)
        checkpoint("immutable_downloads_verified", raw_commit=raw_state["head"], download_urls=immutable_urls)
        helper.check_heads(states)
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
        active = commit_feeds_atomically(helper, args.repo, states, updates,
            "release: ULike1930 front-camera lens UI fix and bundle159 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(helper.content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(helper.content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(json.loads(helper.content(POLICY_PATH, state["head"])) == expected_policies[branch], "Remote ULike policy mismatch")
            require(helper.fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(helper.fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?ulike1930={state['head']}")) == feed, "Public branch feed mismatch")
            require(helper.fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?ulike1930={state['head']}").decode() == changelog, "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_verified": True, "manager_download_verified": True})
        helper.check_heads(active)
        helper.verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("published_and_verified", published=True, release_url=PAGE, manager_main_dev_updated_atomically=True,
            manager_ulike_update_eligible=True, other_apps_false_updates=False, swiftkey183_preserved=True, fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds_atomically(helper, args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified ULike1930 publication receipt")
        for branch, state in saved.items():
            require(helper.content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(json.loads(helper.content(POLICY_PATH, state["head"])) == expected_policies[branch], "ULike policy changed while saving receipt")
        helper.run(["gh", "release", "upload", TAG, "--repo", REPO, receipt_path])
        published_assets = {**expected["artifacts"], RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}
        helper.verify_assets(release["id"], published_assets, True)
        helper.check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, ULike scope, SwiftKey183 retention and saved receipt")
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
