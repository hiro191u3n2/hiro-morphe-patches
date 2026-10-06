#!/usr/bin/env python3
"""Publish exact SwiftKey 1.8.2 MPPs and advance both Manager feeds with leases.

Expected hashes are supplied independently in --expected; this file never invents
them from the output being approved. --local-only and --preflight-only do not
write to GitHub. The source checkout/index/worktree is not changed by publishing.
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
VERSION, PREVIOUS, BUNDLE = "1.8.2", "1.0.156", "1.0.157"
TAG, RAW_BRANCH = "swiftkey-v1.8.2", "release/swiftkey182-157"
SINGLE = "SwiftKeyBeta_v2_SamsungEmoji_v1.8.2.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.157.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.156.mpp"
BASE_SHA256 = "40c885d205d0a087192be599263a13858ff5c48fec935835791bfc93a364de88"
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"906600c1098fdd412ce8b22018929850e2543d14/downloads/{BASE_NAME}")
SINGLE_BASE_NAME = "SwiftKeyBeta_v2_SamsungEmoji_v1.8.1.mpp"
SINGLE_BASE_SHA256 = "077ad014caa5825a690fa2aad9d4d743ecc36267ae86491563e12ffade9ee8e0"
SINGLE_BASE_URL = "https://hiro-morphe-patches.otoha10.chatgpt.site/" + SINGLE_BASE_NAME
QA_NAME = "QA_SwiftKey_v1.8.2.json"
SOURCE_ZIP = "SwiftKeyBeta_v1.8.2_sources_and_QA.zip"
RECEIPT_NAME = "publication_SwiftKey_v1.8.2.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/swiftkey182-157"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
EXTENSION = "extensions/swiftkey_japanese.mpe"
MF = "META-INF/MANIFEST.MF"
ALLOWED_CHANGED = {MF, EXTENSION}
APP_NAMES = ("Microsoft SwiftKey Beta", "SwiftKey Beta")
OTHER_APPS = ("ULike", "Berry Browser", "Instagram", "X", "Trip.com", "TikTok",
              "Yahoo!乗換案内", "Amazonショッピング", "Hanull Reader", "Uber Eats", "BrightnessClick")
HELPER = "releases/ulike-rollback188-142/rollback142.py"
HELPER_BLOB_SHA = "e7771a4868d7a798217304af6d6099bcded5569e"
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
INVENTORY = re.compile(r"(?m)^((?:Microsoft )?SwiftKey Beta：)v(1\.8\.[01])(（9\.13\.16\.4(?:／1236271168)?）)\r?$")
SUMMARY = "日本語入力で既存の英単語へカーソルを戻した時の自動選択を抑止。Pokémonなどéを含むラテン文字にも対応。実機未確認。"


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
    require(old["version"] == PREVIOUS, "Another bundle is active")
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
              f"* **SwiftKey Beta:** v{VERSION}：日本語入力中のカーソル移動でラテン語句を再変換対象へ戻す漏れを修正。既存辞書・キー配置・候補学習を保持。\n\n"
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


def load_expected(path):
    data = json.loads(path.read_text())
    require(data.get("schema") == "swiftkey182-publication-v1", "Wrong expected manifest schema")
    require(data.get("swiftkey_version") == VERSION and data.get("bundle_version") == BUNDLE,
            "Expected manifest targets another release")
    require(data.get("baseline_bundle_version") == PREVIOUS and data.get("baseline_bundle_sha256") == BASE_SHA256,
            "Expected manifest has a different baseline")
    require(data.get("baseline_swiftkey_version") == "1.8.1" and data.get("baseline_swiftkey_sha256") == SINGLE_BASE_SHA256,
            "Expected manifest has a different standalone baseline")
    require(set(data.get("artifacts", {})) == set(ASSETS), "Expected manifest must pin all six release assets")
    for name, row in data["artifacts"].items():
        require(isinstance(row.get("bytes"), int) and row["bytes"] > 0, "Unfilled artifact size: " + name)
        require(re.fullmatch(r"[0-9a-f]{64}", row.get("sha256", "")) is not None
                and row["sha256"] != "0" * 64, "Unfilled artifact hash: " + name)
    require(set(data.get("allowed_changed_bundle_entries", [])) == ALLOWED_CHANGED,
            "Only the SwiftKey extension and MPP manifest may change")
    require(data.get("android_device_tested") is False and data.get("original_apk_apply_tested") is False,
            "This publication must retain the documented unverified device/original-APK scope")
    require(isinstance(data.get("qa_required_values"), dict) and data["qa_required_values"],
            "Expected QA assertions must be specified")
    return data


def lookup_qa(data, dotted_path):
    for component in dotted_path.split("."):
        require(isinstance(data, dict) and component in data, "Missing QA field: " + dotted_path)
        data = data[component]
    return data


def validate_local(dist, expected, baseline, standalone_baseline):
    require(sha(baseline) == BASE_SHA256, "Wrong baseline MPP bytes")
    require(sha(standalone_baseline) == SINGLE_BASE_SHA256, "Wrong standalone baseline bytes")
    payloads = {}
    for name, row in expected["artifacts"].items():
        raw = (dist / name).read_bytes()
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Artifact differs from reviewed bytes: " + name)
        payloads[name] = raw
    old, new, single = archive(baseline), archive(payloads[COMBINED]), archive(payloads[SINGLE])
    old_single = archive(standalone_baseline)
    require(headers(old[MF])["Version"] == PREVIOUS, "Wrong baseline manifest")
    require(headers(old_single[MF])["Version"] == "1.8.1", "Wrong standalone baseline manifest")
    require(headers(new[MF])["Version"] == BUNDLE, "Wrong integrated MPP version")
    require(headers(single[MF])["Version"] == VERSION, "Wrong standalone MPP version")
    require(set(old) == set(new), "Integrated ZIP entries were added or removed")
    changed = {name for name in old if old[name] != new[name]}
    require(changed == ALLOWED_CHANGED, "Unexpected integrated entry changes: " + repr(sorted(changed)))
    require(old["classes.dex"] == new["classes.dex"], "Integrated patch-loader classes.dex changed")
    require(set(old_single) == set(single), "Standalone ZIP entries were added or removed")
    single_changed = {name for name in old_single if old_single[name] != single[name]}
    require(single_changed == ALLOWED_CHANGED, "Unexpected standalone entry changes: " + repr(sorted(single_changed)))
    require(old_single["classes.dex"] == single["classes.dex"], "Standalone patch-loader classes.dex changed")
    swiftkey = {name: raw for name, raw in new.items() if name == EXTENSION or name.startswith("swiftkey/")}
    require({name: raw for name, raw in single.items() if name == EXTENSION or name.startswith("swiftkey/")} == swiftkey,
            "Standalone and integrated SwiftKey resource bytes differ")
    require({name: raw for name, raw in old_single.items() if name == EXTENSION or name.startswith("swiftkey/")}
            == {name: raw for name, raw in old.items() if name == EXTENSION or name.startswith("swiftkey/")},
            "Standalone and integrated baselines contain different SwiftKey resource bytes")
    require(single.get("classes.dex", b"").startswith(b"dex\n"), "Missing standalone patch-loader DEX")
    require(new[EXTENSION].startswith(b"dex\n"), "Missing SwiftKey runtime DEX")
    require(headers(new[MF]).get("Name") == headers(old[MF]).get("Name"), "Integrated source name changed")
    qa = json.loads(payloads[QA_NAME])
    for path, expected_value in expected["qa_required_values"].items():
        actual = lookup_qa(qa, path)
        require(type(actual) is type(expected_value) and actual == expected_value, "QA assertion failed: " + path)
    checksum_rows = {}
    for line in payloads["SHA256SUMS.txt"].decode().splitlines():
        if not line.strip():
            continue
        match = re.fullmatch(r"([0-9a-f]{64}) [ *](.+)", line)
        require(match is not None, "Malformed SHA256SUMS line")
        digest, name = match.groups()
        require(name in payloads and name != "SHA256SUMS.txt" and name not in checksum_rows,
                "Unknown or duplicate SHA256SUMS entry")
        require(sha(payloads[name]) == digest, "SHA256SUMS mismatch: " + name)
        checksum_rows[name] = digest
    require({SINGLE, COMBINED}.issubset(checksum_rows), "SHA256SUMS must include both MPPs")
    sources = archive(payloads[SOURCE_ZIP])
    require(sources, "Empty source archive")
    require(not any(PurePosixPath(n).suffix.casefold() in (".apk", ".apks", ".aab", ".jks", ".keystore") for n in sources),
            "Source archive contains an app binary or signing key")
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(VERSION in notes and BUNDLE in notes and "Pokémon" in notes and "未確認" in notes,
            "Release notes lack the target versions, reported example, or device uncertainty")
    return payloads, notes, {
        "integrated_changed_entries": sorted(changed),
        "integrated_unchanged_entries": len(old) - len(changed),
        "integrated_classes_dex_unchanged": True,
        "standalone_changed_entries": sorted(single_changed),
        "standalone_unchanged_entries": len(single) - len(single_changed),
        "standalone_classes_dex_and_jvm_classes_unchanged": True,
        "standalone_and_integrated_swiftkey_resources_identical": True,
        "swiftkey_resource_entries": len(swiftkey),
        "qa_assertions_verified": list(expected["qa_required_values"]),
    }


def load_helper(repo):
    path = repo / HELPER
    raw = path.read_bytes()
    git_sha = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
    require(git_sha == HELPER_BLOB_SHA, "Pinned publication helper changed")
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location("swiftkey182_publication_helper", path)
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
    with tempfile.TemporaryDirectory(prefix="swiftkey182-index-") as temp:
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


def publication(args):
    expected = load_expected(args.expected)
    helper = load_helper(args.repo)
    baseline = args.baseline.read_bytes() if args.baseline else helper.fetch(BASE_URL)
    standalone_baseline = args.standalone_baseline.read_bytes() if args.standalone_baseline else helper.fetch(SINGLE_BASE_URL)
    payloads, notes, local_report = validate_local(args.dist, expected, baseline, standalone_baseline)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **local_report}, ensure_ascii=False, indent=2))
        return
    states = {branch: helper.snapshot(branch) for branch in ("main", "dev")}
    helper.check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
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
    receipt = {"schema": "swiftkey182-publication-receipt-v1", "swiftkey_version": VERSION, "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "baseline_swiftkey_version": "1.8.1", "baseline_swiftkey_sha256": SINGLE_BASE_SHA256,
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
            "name": "SwiftKey Beta v1.8.2 英単語の自動選択修正 / Hiro Morphe v1.0.157",
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
                                        "release: exact SwiftKey182 and bundle157 immutable MPP downloads")
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
                                         "release: SwiftKey182 Latin cursor protection and bundle157 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(helper.content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(helper.content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(helper.content("releases/ULike_ACTIVE.json", state["head"]) == policies[branch], "ULike policy changed")
            require(helper.fetch(feed["download_url"]) == payloads[COMBINED], "Manager download mismatch")
            public_url = f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?swiftkey182={state['head']}"
            require(json.loads(helper.fetch(public_url)) == feed, "Public branch feed mismatch")
            public_log = f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?swiftkey182={state['head']}"
            require(helper.fetch(public_log).decode() == changelog, "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                                           "changelog_verified": True, "ulike_policy_byte_identical": True,
                                           "manager_download_verified": True})
        helper.check_heads(active)
        helper.verify_assets(release["id"], expected["artifacts"], True)
        checkpoint("published_and_verified", published=True, release_url=PAGE,
                   manager_main_dev_updated_atomically=True, manager_update_eligible_for_both_app_names=True,
                   manager_device_update_badge_observed=False, other_apps_false_updates=False,
                   fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        receipt_updates = {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active}
        saved = commit_feeds_atomically(helper, args.repo, active, receipt_updates,
                                        "docs: verified SwiftKey182 publication receipt")
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
    parser.add_argument("--repo", required=True, type=Path)
    parser.add_argument("--expected", required=True, type=Path)
    parser.add_argument("--baseline", type=Path, help="Local exact 1.0.156 MPP; avoids baseline download")
    parser.add_argument("--standalone-baseline", type=Path, help="Local exact SwiftKey 1.8.1 MPP")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true", help="Verify local reviewed outputs only")
    mode.add_argument("--preflight-only", action="store_true", help="Also verify current GitHub state, without writes")
    args = parser.parse_args()
    for key in ("dist", "repo", "expected", "baseline", "standalone_baseline"):
        if getattr(args, key) is not None:
            setattr(args, key, getattr(args, key).resolve())
    require(not args.local_only or (args.baseline is not None and args.standalone_baseline is not None),
            "--local-only requires --baseline and --standalone-baseline")
    publication(args)


if __name__ == "__main__":
    main()
