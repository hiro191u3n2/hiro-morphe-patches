#!/usr/bin/env python3
"""Publish reviewed ULike1.9.40 while retaining every other patch in the latest pinned bundle.

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
VERSION, PREVIOUS_APP = "1.9.40", "1.9.39"
PREVIOUS, BUNDLE = "1.0.172", "1.0.173"
TAG, RAW_BRANCH = "ulike-v1.9.40", "release/ulike1940-173"
SINGLE = "ULike_HQ_Texture_Online_v1.9.40.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.173.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.172.mpp"
BASE_SHA256 = "10e87a841dc73db1a3dbd34262f1a156cebc75929df0d741a30b536dbd5ffd23"
BASE_BYTES = 17460390
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"76b16be695c32f2e7a1fc938f4496184cf2c738e/downloads/{BASE_NAME}")
SINGLE_BASE_NAME = "ULike_HQ_Texture_Online_v1.9.39.mpp"
SINGLE_BASE_SHA256 = "7ed0d10dc1730d6211fa215d663edaf0d81f977cc29dd6cdc97f8368f7afc142"
SINGLE_BASE_BYTES = 813317
SINGLE_BASE_URL = f"https://raw.githubusercontent.com/{REPO}/76b16be695c32f2e7a1fc938f4496184cf2c738e/downloads/{SINGLE_BASE_NAME}"
QA_NAME = "QA_ULike_v1.9.40.json"
SOURCE_ZIP = "ULike_v1.9.40_sources_and_QA.zip"
RECEIPT_NAME = "publication_ULike_v1.9.40.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/ulike1940-shutter"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
MF = "META-INF/MANIFEST.MF"
RUNTIME = "ulike/runtime.dex"
ULIKE_LOADER = "app/hiro/ulike/patches/UlikeHqMaxPatch.class"
NATIVE_ENTRY = "ulike1935/runtime/libulike_speed1935.so"
NATIVE_INSTALLER = "app/hiro/ulike/patches/IntegrationPayload186.class"
ALLOWED_ADDED = set()
ALLOWED_CHANGED = {MF, RUNTIME, "classes.dex", ULIKE_LOADER,
                   "ulike/methods.dex", "ulike/methods.tsv"}
REQUIRED_CHANGED = {MF, RUNTIME}
NATIVE_PAYLOADS = ("ulike/methods.dex", "ulike/methods.tsv")
TOOLCHAIN_SHA256 = {
    "morphe.jar": "82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c",
    "android.jar": "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b",
    "d8.jar": "305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5",
}
REQUIRED_QA = {
    "schema": "ulike1940-shutter-v1",
    "ulike_version": VERSION,
    "bundle_version": BUNDLE,
    "selected_candidates": ["black_double_tap_disabled", "shutter_feedback", "rapid_manual_capture"],
    "approved_lineage": "1.8.8",
    "device_tested": False,
    "device_quality_verified": False,
    "non_ulike_resources_byte_identical": True,
    "non_ulike_loader_classes_unchanged": True,
    "camera_startup_recovery_byte_identical": True,
    "front_lens_fixes_byte_identical": True,
    "viewport_layout_byte_identical": True,
    "image_pipeline_byte_identical": False,
    "quality_pixel_math_byte_identical": True,
    "manual_capture_safety_checks_preserved": True,
    "speed_and_save_helpers_byte_identical": False,
    "native_library_byte_identical": True,
    "native_installer_byte_identical": True,
    "standalone_and_bundle_ulike_resources_identical": True,
    "host_quality_passed": True,
    "production_source_consistency_verified": True,
    "host_quality_result.status": "passed",
    "host_quality_result.suites.gesture_feedback.status": "passed",
    "host_quality_result.suites.rapid_manual_capture.status": "passed",
    "jdk_identity.runtime_version": "21.0.8+9-LTS",
    "jdk_identity.vendor": "Eclipse Adoptium",
    "jdk_identity.javac_version": "21.0.8",
}

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
INVENTORY = re.compile(r"(?m)^(ULike：)v(1\.9\.39)(（5\.6\.2／740）)\r?$")
SUMMARY = "黒い部分のダブルタップ切替を無効化し、撮影表示を見やすく変更。画質を維持して連続手動撮影の待機を修正。実機速度と画質は未測定。"


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
                "User-Agent": "Hiro-ULike1940-Publication",
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
    with tempfile.TemporaryDirectory(prefix="ulike1940-index-") as temp:
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
    require(old["version"] == PREVIOUS and old["download_url"] == BASE_URL,
            "Another bundle or baseline source is active")
    require(dt.datetime.fromisoformat(created) > dt.datetime.fromisoformat(old["created_at"]), "created_at must advance")
    require(len(list(INVENTORY.finditer(old["description"]))) == 1, "Missing or ambiguous current ULike inventory")
    previous_description = INVENTORY.sub(lambda m: m[1] + "v" + VERSION + m[3], old["description"], count=1)
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": notes.strip() + "\n\n" + previous_description}
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Release notes contain a competing version heading")
    changelog = f"# {BUNDLE} ({created[:10]})\n\n* **ULike:** v{VERSION}：{SUMMARY}\n\n" + notes.strip() + "\n\n" + state["log"]
    require(has_changes_for(changelog, PREVIOUS, ["ULike"]), "Missing ULike update scope")
    require(has_changes_for(changelog, "1.0.172", ["ULike"]), "Prior ULike installation must receive a repatch update")
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
    """Pin the current reviewed bundle; fail closed if it advances before publish."""
    global PREVIOUS, BUNDLE, COMBINED, BASE_NAME, BASE_SHA256, BASE_BYTES, BASE_URL
    global RAW_BRANCH, ASSETS, REQUIRED_QA
    require(expected.get("ulike_version") == VERSION and expected.get("baseline_ulike_version") == PREVIOUS_APP,
            "ULike version or lineage base differs")
    require(expected.get("baseline_ulike_sha256") == SINGLE_BASE_SHA256,
            "Approved ULike1.9.39 checksum differs")
    require(expected.get("baseline_ulike_bytes") == SINGLE_BASE_BYTES and expected.get("baseline_ulike_url") == SINGLE_BASE_URL,
            "Approved ULike1.9.39 source differs")
    previous, bundle = expected.get("baseline_bundle_version"), expected.get("bundle_version")
    require(isinstance(previous, str) and isinstance(bundle, str), "Bundle versions are missing")
    pv, nv = version_tuple(previous), version_tuple(bundle)
    require(pv >= (1, 0, 171) and nv[:2] == pv[:2] and nv[2] == pv[2] + 1,
            "Bundle must advance exactly once from the reviewed current baseline")
    name = "Hiro_Morphe_Patches_v" + previous + ".mpp"
    digest, size, url = [expected.get("baseline_bundle_" + key) for key in ("sha256", "bytes", "url")]
    require(isinstance(digest, str) and re.fullmatch(r"[0-9a-f]{64}", digest), "Unfilled baseline digest")
    require(type(size) is int and size > 0, "Unfilled baseline size")
    require(isinstance(url, str) and re.fullmatch(
        r"https://raw\.githubusercontent\.com/hiro191u3n2/hiro-morphe-patches/[0-9a-f]{40}/downloads/" + re.escape(name), url),
        "Baseline must be an immutable current repository MPP URL")
    PREVIOUS, BUNDLE = previous, bundle
    COMBINED = "Hiro_Morphe_Patches_v" + BUNDLE + ".mpp"
    BASE_NAME, BASE_SHA256, BASE_BYTES, BASE_URL = name, digest, size, url
    RAW_BRANCH = "release/ulike1940-" + str(nv[2])
    ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
    REQUIRED_QA = {**REQUIRED_QA, "bundle_version": BUNDLE}
    require(expected.get("toolchain_sha256") == TOOLCHAIN_SHA256, "Reviewed toolchain differs")
    require(expected.get("jdk_runtime_version") == "21.0.8+9-LTS"
            and expected.get("jdk_vendor") == "Eclipse Adoptium", "Reviewed JDK identity differs")


def load_expected(path):
    expected = json.loads(path.read_text())
    configure(expected)
    require(expected.get("schema") == "ulike1940-publication-v1", "Wrong publication manifest schema")
    for key, value in {"ulike_version": VERSION, "bundle_version": BUNDLE,
                       "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
                       "baseline_ulike_version": PREVIOUS_APP, "baseline_ulike_sha256": SINGLE_BASE_SHA256}.items():
        require(expected.get(key) == value, "Wrong reviewed publication value: " + key)
    require(expected.get("change_plan_reviewed") is True and expected.get("qa_contract_reviewed") is True,
            "Review the exact entry delta and QA contract before publishing")
    require(expected.get("android_device_tested") is False, "Galaxy execution is unverified")
    require(expected.get("original_apk_apply_tested") is True, "Both original APK applications must be verified")
    require(set(expected.get("artifacts", {})) == set(ASSETS), "Pin exactly the six release assets")
    for name, row in expected["artifacts"].items():
        require(type(row.get("bytes")) is int and row["bytes"] > 0, "Unfilled asset size: " + name)
        require(isinstance(row.get("sha256"), str) and re.fullmatch(r"[0-9a-f]{64}", row["sha256"]) is not None
                and row["sha256"] != "0" * 64,
                "Unfilled asset checksum: " + name)
    for kind in ("bundle", "standalone"):
        changed = entry_plan(expected, "allowed_changed_" + kind + "_entries")
        require(REQUIRED_CHANGED <= changed <= ALLOWED_CHANGED, "Change plan exceeds the reviewed manual-shutter/save edit")
        require(entry_plan(expected, "allowed_added_" + kind + "_entries") == ALLOWED_ADDED, "No unreviewed archive entries may be added by this shutter-response fix")
    contract = expected.get("qa_required_values")
    require(isinstance(contract, dict), "Expected QA contract is absent")
    for key, value in REQUIRED_QA.items():
        require(type(contract.get(key)) is type(value) and contract[key] == value, "Missing semantic QA requirement: " + key)
    require(type(contract.get("host_quality_result.assertions")) is int
            and contract["host_quality_result.assertions"] > 0, "Pin the real positive host assertion count")
    require(contract.get("original_apk_apply_tested") is expected["original_apk_apply_tested"], "APK validation scope differs")
    for key in ("changed_runtime_methods", "changed_native_methods", "new_helper_classes",
                "new_runtime_aliases", "new_native_methods"):
        rows = contract.get(key)
        require(isinstance(rows, list) and all(isinstance(row, str) and row.startswith("L")
                and ";" in row and not re.search(r"TODO|PLACEHOLDER|TBD", row) for row in rows),
                "Missing reviewed DEX change inventory: " + key)
        require(len(rows) == len(set(rows)), "Duplicate reviewed DEX change inventory: " + key)
    require(expected.get("production_helper_roots") == ["AsyncSave1935", "CaptureMemory1940", "MemoryBudget1940", "SaveQueue1935", "ShutterFeedback1940"] and contract.get("replaced_helper_roots") == expected["production_helper_roots"], "Only reviewed shutter/save helper families may be replaced")
    for key in ("native_methods_byte_identical", "native_methods_tsv_byte_identical", "camera_lifecycle_byte_identical"):
        require(type(contract.get(key)) is bool, "Native payload preservation must be explicit: " + key)
    source_names = expected.get("required_source_paths")
    require(isinstance(source_names, list) and source_names and len(source_names) == len(set(source_names)),
            "Missing reviewed source inventory")
    for name in source_names:
        require(isinstance(name, str) and name.startswith("src/"), "Missing source path")
        checked_path(name)
        require(not re.search(r"TODO|PLACEHOLDER|TBD", name), "Unfilled required source path")
    require({"src/build1940.py", "src/validate1940.py", "src/finalize1940.py"} <= set(source_names), "Reproducible build sources required")
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
    require(other(old) == other(new), "Non-ULike resources changed from the exact baseline bundle")
    require(all(is_ulike(name) or name in (MF, "classes.dex") for name in single), "Standalone has another application's files")
    qa = json.loads(payloads[QA_NAME])
    for key, value in expected["qa_required_values"].items():
        actual = lookup_qa(qa, key)
        require(type(actual) is type(value) and actual == value, "QA assertion failed: " + key)
    native_unchanged = old["ulike/methods.dex"] == new["ulike/methods.dex"]
    inventory_unchanged = old["ulike/methods.tsv"] == new["ulike/methods.tsv"]
    require(new[NATIVE_ENTRY] == old[NATIVE_ENTRY] and new[NATIVE_INSTALLER] == old[NATIVE_INSTALLER], "Inherited native kernel and installer must remain byte identical")
    require(sha(new[NATIVE_ENTRY]) == expected["native_library_sha256"] and len(new[NATIVE_ENTRY]) == expected["native_library_bytes"], "Pinned native kernel differs")
    require(qa["native_methods_byte_identical"] is native_unchanged,
            "Native payload preservation claim does not match actual bytes")
    require(qa["native_methods_tsv_byte_identical"] is inventory_unchanged,
            "Native inventory preservation claim does not match actual bytes")
    require(native_unchanged or qa["changed_native_methods"], "Changed native payload requires exact method inventory")
    for name in NATIVE_PAYLOADS:
        require(name in new and name in single, "Missing native ULike payload: " + name)
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
    require(sources and set(expected["required_source_paths"]) <= sources.keys(), "Missing reviewed implementation source")
    source_hashes = qa.get("source_sha256")
    require(isinstance(source_hashes, dict) and set(expected["required_source_paths"]) <= source_hashes.keys(),
            "Missing reviewed source checksums")
    for name, digest in source_hashes.items():
        checked_path(name)
        require(name in sources and sha(sources[name]) == digest, "Source package differs from QA: " + name)
    require(not any(PurePosixPath(name).suffix.casefold() in (".apk", ".apks", ".aab", ".jks", ".keystore") for name in sources), "Source archive contains application binary or signing key")
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(all(value in notes for value in (VERSION, PREVIOUS_APP, BUNDLE, "シャッター", "画質", "未測定", "再適用")),
            "Release notes omit scope, prior version repatch, or device uncertainty")
    require(not re.search(r"\b(?:TODO|PLACEHOLDER|TBD)\b|レビュー後に確定", notes), "Unfilled release notes")
    report = {"bundle_changed_entries": sorted(changed), "bundle_added_entries": sorted(added),
              "standalone_changed_entries": sorted(single_changed), "standalone_added_entries": sorted(single_added),
              "non_ulike_resources_unchanged": len(other(new)), "ulike_resources_identical": len(own(new)),
              "native_methods_byte_identical": native_unchanged, "native_methods_tsv_byte_identical": inventory_unchanged,
              "baseline_other_app_resources_byte_identical": True,
              "qa_assertions_verified": list(expected["qa_required_values"])}
    return payloads, notes, report


def validate_policy(raw):
    policy = json.loads(raw)
    require(policy.get("ulike_version") == PREVIOUS_APP, "Active ULike advanced")
    require(policy.get("source_sha256") == SINGLE_BASE_SHA256, "Active ULike source bytes differ")
    require(policy.get("approved_lineage_version") == "1.8.8" and "1.9.17" in policy.get("withdrawn_versions", []), "Approved ULike lineage differs")
    return policy


def new_policy(old, expected):
    qa = expected["qa_required_values"]
    return {**old, "ulike_version": VERSION, "bundle_version": BUNDLE, "source_filename": SINGLE,
            "source_sha256": expected["artifacts"][SINGLE]["sha256"], "source_release": TAG, "active_release": TAG,
            "base_ulike_version": PREVIOUS_APP, "base_bundle_version": PREVIOUS, "base_source_sha256": SINGLE_BASE_SHA256,
            "reason": "Improve manual shutter admission and isolate shot/save ownership while retaining exact startup recovery, viewport geometry, lens visibility, image-quality calculations, native kernels and other apps.",
            "future_ulike_base": "Use ULike1.9.40 from approved1.8.8 lineage. Preserve manual-shutter admission backpressure, independent shot/save ownership, exact-pixel speed improvements, best-frame fusion, landmark-aware quality masks, local noise/chroma improvements, precise YUV conversion and combined image transform, plus same-exposure burst capture, front/rear preview recovery, camera-facing memory and UI gating. Do not restore withdrawn1.9.17/10bit or removed diagnostics.",
            "black_tap_native_switch": False, "black_bar_gesture_byte_identical": False, "black_tap_disabled": True, "shutter_feedback_device_tested": False, "release_status": "shutter1940_device_unverified", "android_device_tested": False,
            "q4_scope": "Saved-bitmap detected eye geometry with conservative face zones; precise lip contours and semantic hair segmentation are not implemented.",
            "q11_scope": "Exact YUV plane layout/sample preservation; no guessed RGB matrix/range or new bit-depth claim.",
            "q13_scope": "Direct pixel permutation for eligible quarter-turn/integer-crop cases; keep existing one-pass resampling and native beauty mesh.",
            "device_quality_verified": False, "selected_candidates": ["S2", "S3", "S4", "S5", "S6", "S7", "S8", "S16", "T1"],
            "current_fix_scope": ["black_double_tap_disabled", "shutter_feedback", "rapid_manual_capture"],
            "front_async_acceptance_waits_for_frame": True, "front_no_frame_preview_stop_max": 1,
            "front_committed_resume_attempts_max": 2, "front_retry_admission_deadline_ms": 5000,
            "front_input_readiness_timeout_ms": 15000, "front_retry_admission_deadline_origin": "first accepted native start",
            "front_completion_scope": "A committed native-handler preview stop/resume may complete after the admission deadline; no extra stop is admitted.",
            "original_apk_apply_tested": expected["original_apk_apply_tested"], "ci_rebuild_matches_local_tested_artifacts": True,
            "native_methods_byte_identical": qa["native_methods_byte_identical"],
            "native_methods_tsv_byte_identical": qa["native_methods_tsv_byte_identical"],
            "camera_lifecycle_byte_identical": qa["camera_lifecycle_byte_identical"],
            "camera_startup_recovery_byte_identical": qa["camera_startup_recovery_byte_identical"],
            "all_existing_runtime_unchanged": False,
            "image_pipeline_byte_identical": qa["image_pipeline_byte_identical"], "quality_pixel_math_byte_identical": qa["quality_pixel_math_byte_identical"], "manual_capture_safety_checks_preserved": qa["manual_capture_safety_checks_preserved"], "speed_and_save_helpers_byte_identical": qa["speed_and_save_helpers_byte_identical"], "front_lens_fixes_byte_identical": qa["front_lens_fixes_byte_identical"], "shutter_response_verified_on_device": False, "baseline_other_apps_preserved": True}


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


def publication(args):
    expected = load_expected(args.expected)
    baseline = args.baseline.read_bytes() if args.baseline else fetch(BASE_URL)
    standalone_baseline = args.standalone_baseline.read_bytes() if args.standalone_baseline else fetch(SINGLE_BASE_URL)
    payloads, notes, report = validate_local(args.dist, expected, baseline, standalone_baseline)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **report}, ensure_ascii=False, indent=2))
        return
    states = {branch: snapshot(branch) for branch in ("main", "dev")}
    check_heads(states)
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
    receipt = {"schema": "ulike1940-publication-receipt-v1", "ulike_version": VERSION, "bundle_version": BUNDLE,
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
            "name": f"ULike v{VERSION} 黒帯ダブルタップ無効・撮影表示・画質維持連続撮影 / Hiro Morphe v{BUNDLE}",
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
            f"release: exact ULike1940 and bundle{BUNDLE} immutable MPP downloads")
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
            f"release: ULike1940 manual shutter response and bundle{BUNDLE} Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(json.loads(content(POLICY_PATH, state["head"])) == expected_policies[branch], "Remote ULike policy mismatch")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?ulike1940={state['head']}")) == feed, "Public branch feed mismatch")
            require(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?ulike1940={state['head']}").decode() == changelog, "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_verified": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("published_and_verified", published=True, release_url=PAGE, manager_main_dev_updated_atomically=True,
            manager_ulike_update_eligible=True, previous_ulike1939_repatch_eligible=True,
            other_apps_false_updates=False, baseline_other_apps_preserved=True, fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds_atomically(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified ULike1940 publication receipt")
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
