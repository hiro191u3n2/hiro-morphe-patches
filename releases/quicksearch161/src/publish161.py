#!/usr/bin/env python3
"""Publish independently checked QuickSearch MPPs and atomically advance both feeds.

No app APK is needed or uploaded. --local-only performs no network or Git calls;
--preflight-only reads remote state without writing it. Existing worktrees and
indexes are preserved by private-index commits with explicit parent/head leases.
"""
from __future__ import annotations

import argparse
import base64
import datetime as dt
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

REPO = "hiro191u3n2/hiro-morphe-patches"
VERSION, PREVIOUS_APP, PREVIOUS, BUNDLE = "1.0.1", "1.0.0", "1.0.160", "1.0.161"
TAG = "quicksearch-v1.0.1-bundle-v1.0.161"
RAW_BRANCH = "release/quicksearch161"
SINGLE = "QuickSearch_NoRecents_v1.0.1.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.161.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.160.mpp"
BASE_SHA256 = "7e3a1f531cf127f9c8c16cb8c12548ce9437b84adac82ea0d9bb2244e7333b5a"
BASE_BYTES = 17292368
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"0e15c2c64dfb9fcffbdc4f4738c587af5a5c9b18/downloads/{BASE_NAME}")
QA_NAME = "QA_QuickSearch_v1.0.1.json"
SOURCE_ZIP = "QuickSearch_v1.0.1_sources_and_QA.zip"
RECEIPT_NAME = "publication_QuickSearch_v1.0.1.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/quicksearch161"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
SOURCE = Path(__file__).resolve().parent
MF = "META-INF/MANIFEST.MF"
PATCH_PREFIX = "app/hiro/quicksearch/patches/"
EXTENSION = "extensions/quicksearch_recents.mpe"
APP = "簡単検索くん"
SUMMARY = "×ボタンとナビゲーションの戻る操作による終了・履歴削除を修正。対象0.4.5（36）。実機未確認。"
QA_REQUIRED = {
    "device_tested": False,
    "original_apk_apply_tested": True,
    "non_quicksearch_resources_unchanged": True,
    "non_quicksearch_loader_classes_unchanged": True,
    "standalone_and_bundle_quicksearch_resources_identical": True,
}
OTHER_APPS = ("ULike", "Microsoft SwiftKey Beta", "SwiftKey Beta", "Berry Browser", "Instagram",
              "X", "Trip.com", "TikTok", "Yahoo!乗換案内", "Amazonショッピング", "Hanull Reader",
              "Uber Eats", "BrightnessClick", "明るさタッチ")
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
INVENTORY = re.compile(r"(?m)^(簡単検索くん[：:])v1\.0\.0(（0\.4\.5／36）)\r?$")


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()


def checked_path(name):
    path = PurePosixPath(name)
    require(name and not path.is_absolute() and ".." not in path.parts and "\\" not in name,
            "Unsafe archive/repository path: " + name)
    return path


def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), "Duplicate archive entries")
        for name in names:
            checked_path(name)
        require(z.testzip() is None, "Corrupt archive")
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


def load_expected(path):
    data = json.loads(path.read_text())
    require(isinstance(data, dict) and set(data) == {SINGLE, COMBINED}, "Expected exactly two reviewed MPP hashes")
    for name, row in data.items():
        require(isinstance(row, dict) and type(row.get("bytes")) is int and row["bytes"] > 0,
                "Missing reviewed byte count: " + name)
        require(isinstance(row.get("sha256"), str) and re.fullmatch(r"[0-9a-f]{64}", row["sha256"]),
                "Missing reviewed SHA-256: " + name)
    return data


def is_quicksearch(name):
    return (name.startswith(PATCH_PREFIX) and name.endswith(".class")) or name == EXTENSION


def validate_entry_delta(old, new, single):
    """Replace only QuickSearch resources; preserve every other ZIP payload."""
    removed = set(old) - set(new)
    changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
    added = set(new) - set(old)
    old_own = {name: raw for name, raw in old.items() if is_quicksearch(name)}
    new_own = {name: raw for name, raw in new.items() if is_quicksearch(name)}
    own = {name: raw for name, raw in single.items() if name not in (MF, "classes.dex")}
    require(EXTENSION in old_own and any(name.startswith(PATCH_PREFIX) for name in old_own),
            "Baseline160 is missing its existing QuickSearch patch")
    require(own and all(is_quicksearch(name) for name in own), "Standalone contains unexpected application resources")
    require(EXTENSION in own and own[EXTENSION].startswith(b"dex\n"), "Missing QuickSearch runtime extension")
    require(any(name.startswith(PATCH_PREFIX) for name in own), "Missing JVM patch loader")
    for name, raw in own.items():
        if name.endswith(".class"):
            require(raw.startswith(b"\xca\xfe\xba\xbe"), "Invalid JVM patch class: " + name)
    require(removed.issubset(old_own), "An unrelated application's MPP entry was removed")
    require(changed.issubset(set(old_own) | {MF, "classes.dex"}), "An unrelated application's MPP entry changed")
    require(added.issubset(own), "Unexpected new application resources in the bundle")
    require({MF, "classes.dex", EXTENSION}.issubset(changed), "Expected QuickSearch runtime/loader/version changes are missing")
    require(any(name.startswith(PATCH_PREFIX) for name in changed | added), "The updated QuickSearch JVM loader is missing")
    # Exact equality also rejects obsolete QuickSearch classes left only in the
    # integrated bundle when the new standalone no longer contains them.
    require(new_own == own, "Standalone and bundle QuickSearch resources differ or obsolete resources remain")
    other = lambda entries: {name: raw for name, raw in entries.items()
                             if name not in (MF, "classes.dex") and not is_quicksearch(name)}
    old_other, new_other = other(old), other(new)
    require(old_other == new_other, "Non-QuickSearch resources changed, including ULike or SwiftKey")
    return removed, changed, added, len(old_other)


def validate_local(dist, expected_path, baseline):
    expected = load_expected(expected_path)
    require(len(baseline) == BASE_BYTES and sha(baseline) == BASE_SHA256, "Wrong baseline160 MPP bytes")
    payloads = {name: (dist / name).read_bytes() for name in ASSETS}
    for name, row in expected.items():
        require(len(payloads[name]) == row["bytes"] and sha(payloads[name]) == row["sha256"],
                "CI output differs from independently reviewed MPP: " + name)
    old, new, single = archive(baseline), archive(payloads[COMBINED]), archive(payloads[SINGLE])
    for items, version in ((old, PREVIOUS), (new, BUNDLE), (single, VERSION)):
        require(headers(items[MF]).get("Version") == version, "MPP manifest version mismatch")
        require(items.get("classes.dex", b"").startswith(b"dex\n"), "Invalid patch-loader DEX")
    require(headers(new[MF]).get("Name") == headers(old[MF]).get("Name") == "Hiro Morphe Patches",
            "Existing bundle identity changed")
    require(headers(single[MF]).get("Name") == "QuickSearch No Recents", "Existing standalone source identity changed")
    removed, changed, added, preserved_resources = validate_entry_delta(old, new, single)
    qa = json.loads(payloads[QA_NAME])
    for key, value in QA_REQUIRED.items():
        require(qa.get(key) is value, "Missing or failing QA assertion: " + key)
    require(qa.get("artifacts") == expected, "QA and independently reviewed MPP hashes differ")
    require(qa.get("supported_package") == "jp.ddo.sugihiro.quicksearch" and
            qa.get("supported_version") == "0.4.5" and qa.get("supported_version_code") == 36,
            "QA identifies another target application or version")
    local_qa = json.loads((SOURCE / "local-qa.json").read_text())
    require(local_qa.get("artifacts") == expected and local_qa.get("original_apk_apply_tested") is True
            and local_qa.get("device_tested") is False, "Original-APK evidence differs from the tested MPPs")
    require(qa.get("original_apk_evidence") == local_qa, "Published QA differs from recorded original-APK validation")
    checksums = {}
    for line in payloads["SHA256SUMS.txt"].decode().splitlines():
        if not line.strip():
            continue
        match = re.fullmatch(r"([0-9a-f]{64}) [ *](.+)", line)
        require(match is not None, "Malformed SHA256SUMS line")
        digest, name = match.groups()
        require(name in payloads and name != "SHA256SUMS.txt" and name not in checksums,
                "Unknown or duplicate checksum entry")
        require(sha(payloads[name]) == digest, "Checksum mismatch: " + name)
        checksums[name] = digest
    require(set(checksums) == set(ASSETS) - {"SHA256SUMS.txt"}, "Checksums must cover all five other assets")
    sources = archive(payloads[SOURCE_ZIP])
    allowed = {".java", ".kt", ".py", ".json", ".txt", ".md", ".tsv", ".log", ".yaml", ".yml"}
    require(sources and all(PurePosixPath(name).suffix.lower() in allowed for name in sources),
            "Source archive contains an app binary, key, image, or unexpected file type")
    for name in ("build.py", "publish161.py", "expected-mpp.json", "local-qa.json"):
        entries = [raw for path, raw in sources.items() if PurePosixPath(path).name == name]
        require(len(entries) == 1, "Missing or ambiguous archived source: " + name)
        original = expected_path if name == "expected-mpp.json" else SOURCE / name
        require(entries[0] == original.read_bytes(), "Archived source differs from reviewed checkout: " + name)
    require(any(name.endswith(".java") for name in sources), "Missing actual Java patch/runtime source")
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(notes == (SOURCE / "RELEASE_NOTES.txt").read_text(), "Release notes differ from reviewed source")
    require(all(value in notes for value in (APP, VERSION, BUNDLE, "0.4.5", "未確認")),
            "Release notes omit the target, versions, or device limitation")
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Notes contain a competing changelog version heading")
    for line in notes.splitlines():
        match = SCOPE.match(line.strip())
        require(match is None or match[1] == APP, "Release notes contain another application's update scope")
    report = {
        "baseline_bundle_version": PREVIOUS,
        "baseline_bundle_sha256": BASE_SHA256,
        "bundle_changed_entries": sorted(changed),
        "bundle_added_entries": sorted(added),
        "bundle_removed_entries": sorted(removed),
        "existing_non_quicksearch_resource_entries_byte_identical": preserved_resources,
        "quicksearch_payloads_identical": True,
        "qa_assertions_verified": list(QA_REQUIRED),
        "original_apk_apply_evidence_archived": True,
        "android_device_tested": False,
    }
    assets = {name: {"bytes": len(raw), "sha256": sha(raw)} for name, raw in payloads.items()}
    return payloads, notes, assets, report


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
                "User-Agent": "Hiro-QuickSearch161-Publication",
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
    env = {**os.environ, "GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_COMMITTER_NAME": "github-actions[bot]",
           "GIT_AUTHOR_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com",
           "GIT_COMMITTER_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com"}
    with tempfile.TemporaryDirectory(prefix="quicksearch161-index-") as temporary:
        env["GIT_INDEX_FILE"] = str(Path(temporary) / "index")
        git(repo, "read-tree", state["head"], env=env)
        for path, raw in files.items():
            checked_path(path)
            blob = git(repo, "hash-object", "-w", "--stdin", input=raw, env=env)
            git(repo, "update-index", "--add", "--cacheinfo", f"100644,{blob},{path}", env=env)
        tree = git(repo, "write-tree", env=env)
        commit = git(repo, "commit-tree", tree, "-p", state["head"], "-F", "-", input=(message + "\n").encode(), env=env)
        require(git(repo, "show", "-s", "--format=%P", commit) == state["head"], "Prepared commit is not a direct descendant")
        changes = set(git(repo, "diff-tree", "--no-commit-id", "--name-only", "-r", commit).splitlines())
        require(changes and changes.issubset(files), "Prepared commit changes unrelated paths")
        return {**state, "head": commit, "tree": tree}


def commit_feeds(repo, states, updates, message):
    require(set(states) == set(updates) == {"main", "dev"}, "Both Manager branches are required")
    check_heads(states)
    git(repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"], states["dev"]["head"])
    prepared = {branch: prepare_commit(repo, state, updates[branch], message) for branch, state in states.items()}
    check_heads(states)
    # The leases only reject races. Each new commit has the leased HEAD as its
    # sole parent, so this cannot replace or discard existing branch history.
    git(repo, "push", "--atomic",
        f"--force-with-lease=refs/heads/main:{states['main']['head']}",
        f"--force-with-lease=refs/heads/dev:{states['dev']['head']}",
        f"https://github.com/{REPO}.git",
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    check_heads(prepared)
    return prepared


def version_tuple(value):
    require(re.fullmatch(r"v?\d+\.\d+\.\d+", value) is not None, "Expected stable three-part bundle version")
    return tuple(map(int, value.removeprefix("v").split(".")))


def parsed_entries(markdown):
    entries, current = [], None
    for line in markdown.splitlines():
        match = HEADING.match(line)
        if match:
            current = {"version": match[1] or match[2], "date": match[3], "bullets": []}
            entries.append(current)
        elif current is not None:
            scope = SCOPE.match(line.strip())
            if scope:
                current["bullets"].append((scope[1], line.strip()[scope.end():].strip()))
    return entries


def has_changes_for(markdown, installed, app):
    entries = parsed_entries(markdown)
    old_date = next((entry["date"] for entry in entries if entry["version"].removeprefix("v") == installed), None)
    for entry in entries:
        if not re.fullmatch(r"v?\d+\.\d+\.\d+", entry["version"]):
            continue
        if version_tuple(entry["version"]) <= version_tuple(installed) or old_date and entry["date"] < old_date:
            continue
        for scope, body in entry["bullets"]:
            if not EXPERIMENTAL_ONLY.match(body) and (scope.casefold() == app.casefold() or scope.casefold().startswith(app.casefold() + " - ")):
                return True
    return False


def utc_date(value):
    parsed = dt.datetime.fromisoformat(value)
    return parsed.astimezone(dt.timezone.utc).replace(tzinfo=None) if parsed.tzinfo else parsed


def metadata(state, url, created, notes):
    old = state["manifest"]
    require(old["version"] == PREVIOUS and old["download_url"] == BASE_URL, "Active bundle is no longer the reviewed baseline160")
    require(utc_date(created) > utc_date(old["created_at"]), "created_at must advance")
    inventory = list(INVENTORY.finditer(old["description"]))
    require(len(inventory) == 1 and inventory[0].start() == 0,
            "Missing or ambiguous existing QuickSearch1.0.0 inventory at the start of the baseline description")
    previous_description = INVENTORY.sub(lambda match: match[1] + "v" + VERSION + match[2], old["description"], count=1)
    # Keep the complete previous description, changing only its active app
    # inventory row. Previous release headings remain historical information.
    description = notes.strip() + "\n\n" + previous_description
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": description}
    changelog = f"# {BUNDLE} ({created[:10]})\n\n* **{APP}:** v{VERSION}：{SUMMARY}\n\n" + notes.strip() + "\n\n" + state["log"]
    require(has_changes_for(changelog, PREVIOUS, APP), "Missing QuickSearch update scope")
    require(not has_changes_for(changelog, BUNDLE, APP), "Freshly patched QuickSearch would still be outdated")
    require(not any(has_changes_for(changelog, PREVIOUS, app) for app in OTHER_APPS), "False update scope for an unchanged application")
    entry = parsed_entries(changelog)[0]
    require(entry["version"] == BUNDLE and {scope for scope, _ in entry["bullets"]} == {APP}, "Unexpected top-entry update scope")
    require(feed["description"].endswith(previous_description) and changelog.endswith(state["log"]), "Existing description/history was lost")
    return feed, changelog


def verify_assets(release_id, expected, public):
    release = api(f"repos/{REPO}/releases/{release_id}")
    require(release["tag_name"] == TAG and release["draft"] is not public, "Release identity or draft status differs")
    assets = {asset["name"]: asset for asset in release["assets"]}
    require(set(assets) == set(expected) and len(assets) == len(release["assets"]), "Release asset inventory differs")
    for name, row in expected.items():
        asset = assets[name]
        require(asset["size"] == row["bytes"], "Release asset size differs: " + name)
        if asset.get("digest"):
            require(asset["digest"] == "sha256:" + row["sha256"], "GitHub asset digest differs: " + name)
        raw = fetch(asset["browser_download_url"]) if public else subprocess.check_output(
            ["gh", "api", f"repos/{REPO}/releases/assets/{asset['id']}", "-H", "Accept: application/octet-stream"])
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "Downloaded release bytes differ: " + name)


def publication(args):
    payloads, notes, assets, report = validate_local(args.dist, args.expected, args.base.read_bytes())
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **report}, ensure_ascii=False, indent=2))
        return
    states = {branch: snapshot(branch) for branch in ("main", "dev")}
    check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow was triggered")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    require(sha(fetch(BASE_URL)) == BASE_SHA256, "Public baseline160 bytes differ")
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()
    for state in states.values():
        metadata(state, "https://example.invalid/preflight-only", created, notes)
    require(api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/tags/{TAG}", absent=True) is None, "Tag already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None, "Distribution branch exists; inspect before retry")
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes", "heads": {branch: state["head"] for branch, state in states.items()}, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "quicksearch161-publication-receipt-v1", "quicksearch_version": VERSION,
               "baseline_quicksearch_version": PREVIOUS_APP,
               "bundle_version": BUNDLE, "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "assets": assets, "local_validation": report, "android_device_tested": False,
               "original_apk_apply_tested": True, "manager_parser_source": MANAGER_SOURCE,
               "manager_device_update_badge_observed": False, "operations": []}

    def checkpoint(status, **changes):
        receipt.update(status=status, **changes)
        receipt_path.write_bytes(json_bytes(receipt))
        print(status, flush=True)

    checkpoint("preflight_passed")
    try:
        check_heads(states)
        release = api(f"repos/{REPO}/releases", {"tag_name": TAG, "target_commitish": states["main"]["head"],
            "name": "簡単検索くん v1.0.1 ×ボタン・戻る操作の終了修正 / Hiro Morphe v1.0.161",
            "body": notes, "draft": True, "prerelease": True, "make_latest": "false"})
        checkpoint("draft_created", release_id=release["id"])
        for name in ASSETS:
            subprocess.run(["gh", "release", "upload", TAG, "--repo", REPO, str(args.dist / name)], check=True)
        verify_assets(release["id"], assets, False)
        checkpoint("draft_assets_verified")
        check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}", {"draft": False, "prerelease": True, "make_latest": "false"}, "PATCH")
        verify_assets(release["id"], assets, True)
        checkpoint("release_bytes_verified")
        check_heads(states)
        git(args.repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"])
        raw_state = prepare_commit(args.repo, {**states["main"], "branch": RAW_BRANCH},
            {"downloads/" + name: payloads[name] for name in (SINGLE, COMBINED)},
            "release: exact QuickSearch101 and bundle161 immutable MPP downloads")
        # An empty lease means the distribution branch must still be absent.
        git(args.repo, "push", f"--force-with-lease=refs/heads/{RAW_BRANCH}:", f"https://github.com/{REPO}.git",
            f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH) == raw_state["head"], "Distribution branch moved")
        urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}" for name in (SINGLE, COMBINED)}
        for name, url in urls.items():
            require(fetch(url) == payloads[name], "Immutable raw download differs: " + name)
        checkpoint("immutable_downloads_verified", raw_commit=raw_state["head"], download_urls=urls)
        check_heads(states)
        new_metadata, updates = {}, {}
        for branch, state in states.items():
            feed, changelog = metadata(state, urls[COMBINED], created, notes)
            new_metadata[branch] = (feed, changelog)
            updates[branch] = {"patches-bundle.json": json_bytes(feed), "CHANGELOG.md": changelog.encode()}
            if branch == "main":
                for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                    updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds(args.repo, states, updates,
            "release: QuickSearch no-recents patch and bundle161 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "Existing ULike active policy changed")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?quicksearch161={state['head']}")) == feed, "Public branch feed mismatch")
            require(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?quicksearch161={state['head']}").decode() == changelog, "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_unchanged": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], assets, True)
        checkpoint("published_and_verified", published=True, release_url=PAGE, manager_main_dev_updated_atomically=True,
            manager_quicksearch_scope_verified=True, other_apps_false_updates=False,
            existing_ulike_and_swiftkey_preserved=True, fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified QuickSearch161 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0], "Feed changed while saving receipt")
        subprocess.run(["gh", "release", "upload", TAG, "--repo", REPO, str(receipt_path)], check=True)
        verify_assets(release["id"], {**assets, RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, QuickSearch scope, existing app retention and receipt", flush=True)
    except BaseException as error:
        checkpoint("failed_requires_inspection", error=type(error).__name__ + ": " + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--base", type=Path, required=True)
    parser.add_argument("--expected", type=Path, default=SOURCE / "expected-mpp.json")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true")
    mode.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("dist", "repo", "base", "expected"):
        setattr(args, key, getattr(args, key).resolve())
    publication(args)


if __name__ == "__main__":
    main()
