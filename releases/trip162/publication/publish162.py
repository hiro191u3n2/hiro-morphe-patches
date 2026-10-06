#!/usr/bin/env python3
"""Publish the reviewed Trip.com 1.10.15 MPPs and atomically advance both feeds.

--local-only performs no network or Git operations. --preflight-only reads remote
state without changing it. Binary release work runs in the established GitHub
Actions environment; only authored source and QA evidence are committed first.
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
VERSION, PREVIOUS_APP, PREVIOUS, BUNDLE = "1.10.15", "1.10.14", "1.0.161", "1.0.162"
TAG = "tripcom-v1.10.15"
RAW_BRANCH = "release/trip162"
SINGLE = "Tripcom_Quiet_v1.10.15.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.162.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.161.mpp"
BASE_SHA256 = "92d1a60ed74859eca0f9d24413167d4b792031fbd50c6a1495a608d4043b2d03"
BASE_BYTES = 17310850
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"c5014ec59c1948732e570990f88a0866db0616c7/downloads/{BASE_NAME}")
SINGLE_BASE_NAME = "Tripcom_Quiet_v1.10.14.mpp"
SINGLE_BASE_SHA256 = "e99d82b7d5250662c82c3a50463ac9cbd551d56db299c8837c9056cfcdaef3e4"
SINGLE_BASE_BYTES = 376932
SINGLE_BASE_URL = f"https://github.com/{REPO}/releases/download/tripcom-v1.10.14/{SINGLE_BASE_NAME}"
QA_NAME = "QA_Tripcom_v1.10.15.json"
SOURCE_ZIP = "Tripcom_v1.10.15_sources_and_QA.zip"
RECEIPT_NAME = "publication_Tripcom_v1.10.15.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/trip162"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
PUBLICATION = Path(__file__).resolve().parent
SOURCE_ROOT = PUBLICATION.parent
MF = "META-INF/MANIFEST.MF"
PATCH_PREFIX = "app/hiro/tripcom/patches/"
SCHEDULE_ARCHIVE = "replacements/rn_xtaro_ibu_schedule-431156475-30041599.7z"
EXISTING_TRIP_RESOURCES = frozenset({
    "extensions/extension.mpe",
    "extensions/search_landing_guard.mpe",
    "extensions/myplan_preparation_guard.mpe",
    SCHEDULE_ARCHIVE,
})
APP = "Trip.com"
QA_REQUIRED = {
    "android_device_tested": False,
    "original_apk_apply_tested": True,
    "non_trip_resources_unchanged": True,
    "non_trip_loader_classes_unchanged": True,
    "standalone_and_bundle_trip_resources_identical": True,
    "js_regression_passed": True,
    "native_hook_host_tests_passed": True,
    "native_hook_original_apk_verified": True,
}
OTHER_APPS = ("ULike", "Microsoft SwiftKey Beta", "SwiftKey Beta", "Berry Browser", "Instagram",
              "X", "TikTok", "Yahoo!乗換案内", "Amazonショッピング", "Hanull Reader",
              "Uber Eats", "BrightnessClick", "明るさタッチ", "簡単検索くん")
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
INVENTORY_OLD = "Trip.com：v1.10.14（8.54.2）"
INVENTORY_NEW = "Trip.com：v1.10.15（8.54.2）"
REQUIRED_SOURCES = (
    "src/build.py", "src/reviewed-qa.json", "publication/publish162.py", "publication/manifest.json",
    "publication/RELEASE_NOTES.txt", "publication/trip162-publish.yml",
)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()


def checked_path(name):
    path = PurePosixPath(name)
    require(name and not path.is_absolute() and ".." not in path.parts and "\\" not in name
            and str(path) == name and not name.endswith("/"),
            "Unsafe or noncanonical archive/repository path: " + name)
    return path


def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names = z.namelist()
        require(len(names) == len(set(names)), "Duplicate archive entries")
        for name in names:
            checked_path(name.rstrip("/"))
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


def digest_row(row, label):
    require(isinstance(row, dict) and type(row.get("bytes")) is int and row["bytes"] > 0,
            "Missing reviewed byte count: " + label)
    require(isinstance(row.get("sha256"), str) and re.fullmatch(r"[0-9a-f]{64}", row["sha256"]),
            "Missing reviewed SHA-256: " + label)


def load_config(path):
    cfg = json.loads(path.read_text())
    require(cfg.get("schema") == "trip162-publication-v1" and cfg.get("review_status") == "reviewed",
            "Publication manifest is pending review or has another schema")
    require((cfg.get("repo"), cfg.get("trip_version"), cfg.get("bundle_version")) == (REPO, VERSION, BUNDLE),
            "Publication identity differs")
    require(cfg.get("baseline_bundle") == {"version": PREVIOUS, "file": BASE_NAME,
            "bytes": BASE_BYTES, "sha256": BASE_SHA256, "url": BASE_URL}, "Pinned bundle baseline differs")
    require(cfg.get("baseline_standalone") == {"version": PREVIOUS_APP, "file": SINGLE_BASE_NAME,
            "bytes": SINGLE_BASE_BYTES, "sha256": SINGLE_BASE_SHA256, "url": SINGLE_BASE_URL},
            "Pinned standalone baseline differs")
    expected = cfg.get("expected_mpp")
    require(isinstance(expected, dict) and set(expected) == {SINGLE, COMBINED},
            "Expected exactly the two independently reviewed MPP hashes")
    for name, row in expected.items():
        digest_row(row, name)
    require(isinstance(cfg.get("expected_qa_sha256"), str)
            and re.fullmatch(r"[0-9a-f]{64}", cfg["expected_qa_sha256"]), "Missing independently reviewed QA hash")
    extensions = cfg.get("new_trip_extensions")
    require(isinstance(extensions, list) and len(extensions) == len(set(extensions)),
            "Missing or duplicate new Trip extension allowlist")
    for name in extensions:
        checked_path(name)
        require(re.fullmatch(r"extensions/[a-z0-9_]+\.mpe", name) is not None
                and name not in EXISTING_TRIP_RESOURCES, "Invalid new Trip extension: " + name)
    for key in ("release_title", "summary"):
        require(isinstance(cfg.get(key), str) and cfg[key].strip() and "\n" not in cfg[key]
                and not re.search(r"\b(?:TODO|PLACEHOLDER|TBD)\b", cfg[key]), "Missing reviewed release text: " + key)
    return cfg


def is_trip(name, cfg):
    return ((name.startswith(PATCH_PREFIX) and name.endswith(".class"))
            or name in EXISTING_TRIP_RESOURCES or name in cfg["new_trip_extensions"])


def validate_entry_delta(old, new, single, old_single, cfg):
    """Allow only explicitly scoped Trip resources plus the loader and manifest."""
    removed, added = set(old) - set(new), set(new) - set(old)
    changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
    old_own = {name: raw for name, raw in old_single.items() if name not in (MF, "classes.dex")}
    require(old_own and all(is_trip(name, cfg) for name in old_own), "Unexpected baseline Trip resources")
    require(set(EXISTING_TRIP_RESOURCES).issubset(old_own), "Incomplete baseline Trip source")
    require(all(old.get(name) == raw for name, raw in old_own.items()),
            "Current bundle Trip resources differ from the independently pinned standalone baseline")
    require({name: raw for name, raw in old.items() if is_trip(name, cfg)} == old_own,
            "Current bundle contains unexpected Trip resources or a proposed extension already exists")
    own = {name: raw for name, raw in single.items() if name not in (MF, "classes.dex")}
    require(own and all(is_trip(name, cfg) for name in own), "Standalone contains an unrelated resource")
    require(set(EXISTING_TRIP_RESOURCES).issubset(own), "Existing Trip extension or replacement asset was removed")
    require(set(cfg["new_trip_extensions"]).issubset(own), "Declared new Trip extension is missing")
    require(any(name.startswith(PATCH_PREFIX) for name in own), "Missing Trip JVM patch loader")
    for name, raw in own.items():
        if name.endswith(".class"):
            require(raw.startswith(b"\xca\xfe\xba\xbe"), "Invalid JVM patch class: " + name)
        elif name.endswith(".mpe"):
            require(raw.startswith(b"dex\n"), "Invalid runtime DEX: " + name)
    require(own[SCHEDULE_ARCHIVE].startswith(b"7z\xbc\xaf\x27\x1c"), "Invalid schedule 7z replacement")
    require(own[SCHEDULE_ARCHIVE] != old_own[SCHEDULE_ARCHIVE], "Schedule UI replacement was not updated")
    require(removed.issubset(old_own), "An unrelated bundle entry was removed")
    require(changed.issubset(set(old_own) | {MF, "classes.dex"}), "An unrelated bundle entry changed")
    require(added.issubset(own), "An unrelated resource was added to the bundle")
    require({MF, SCHEDULE_ARCHIVE}.issubset(changed), "Missing version or schedule UI update")
    require({name: raw for name, raw in new.items() if is_trip(name, cfg)} == own,
            "Standalone/bundle Trip resources differ or obsolete Trip resources remain")
    other = lambda entries: {name: raw for name, raw in entries.items()
                             if name not in (MF, "classes.dex") and not is_trip(name, cfg)}
    require(other(old) == other(new), "Non-Trip resources changed, including QuickSearch/ULike/SwiftKey")
    return {"bundle_changed_entries": sorted(changed), "bundle_added_entries": sorted(added),
            "bundle_removed_entries": sorted(removed),
            "existing_non_trip_resource_entries_byte_identical": len(other(old)),
            "standalone_and_bundle_trip_resources_identical": True}


def validate_sources(raw, cfg_path):
    entries = archive(raw)
    allowed = {".java", ".kt", ".py", ".json", ".txt", ".md", ".tsv", ".log", ".yaml", ".yml",
               ".js", ".cjs", ".mjs", ".template", ".patch", ".sha256", ".b64", ".html", ".xml"}
    require(entries and all(PurePosixPath(name).suffix.lower() in allowed for name in entries),
            "Source archive contains an app binary, key, image, compiled tool or unsupported file")
    for name, data in entries.items():
        data.decode("utf-8")
        require(b"\x00" not in data and re.search(rb"-----BEGIN (?:[A-Z ]+ )?PRIVATE KEY-----", data) is None,
                "Source archive contains a binary or private key: " + name)
    source_inputs = {"src/" + path.relative_to(SOURCE_ROOT / "src").as_posix()
                     for path in (SOURCE_ROOT / "src").rglob("*")
                     if path.is_file() and path.suffix.lower() in allowed - {".log"}
                     and "__pycache__" not in path.parts}
    require(source_inputs, "Reviewed source checkout is missing")
    for relative in sorted(set(REQUIRED_SOURCES) | source_inputs):
        matches = [data for name, data in entries.items() if name == relative or name.endswith("/" + relative)]
        require(len(matches) == 1, "Missing or ambiguous archived source: " + relative)
        local = cfg_path if relative == "publication/manifest.json" else SOURCE_ROOT / relative
        require(local.is_file() and matches[0] == local.read_bytes(),
                "Archived source differs from reviewed checkout: " + relative)
    require(any(name.endswith((".java", ".kt")) for name in entries), "Missing authored native patch/runtime source")
    require(any(name.endswith((".js", ".cjs", ".mjs")) for name in entries), "Missing authored JS patch or regression checks")
    return len(entries)


def validate_local(args):
    cfg = load_config(args.config)
    baseline, previous_single = args.base.read_bytes(), args.single_base.read_bytes()
    require(len(baseline) == BASE_BYTES and sha(baseline) == BASE_SHA256, "Wrong pinned baseline161 MPP bytes")
    require(len(previous_single) == SINGLE_BASE_BYTES and sha(previous_single) == SINGLE_BASE_SHA256,
            "Wrong pinned Trip1.10.14 standalone bytes")
    payloads = {name: (args.dist / name).read_bytes() for name in ASSETS}
    for name, row in cfg["expected_mpp"].items():
        require(len(payloads[name]) == row["bytes"] and sha(payloads[name]) == row["sha256"],
                "CI output differs from independently reviewed MPP: " + name)
    old, new, single, old_single = map(archive, (baseline, payloads[COMBINED], payloads[SINGLE], previous_single))
    for items, version, name in ((old, PREVIOUS, "Hiro Morphe Patches"),
                                 (new, BUNDLE, "Hiro Morphe Patches"),
                                 (single, VERSION, "Trip.com Quiet"),
                                 (old_single, PREVIOUS_APP, "Trip.com Quiet")):
        fields = headers(items[MF])
        require(fields.get("Version") == version and fields.get("Name") == name, "MPP identity/version differs")
        require(items.get("classes.dex", b"").startswith(b"dex\n"), "Invalid patch-loader DEX")
    report = validate_entry_delta(old, new, single, old_single, cfg)
    require(sha(payloads[QA_NAME]) == cfg["expected_qa_sha256"], "QA differs from independent desktop review")
    require(payloads[QA_NAME] == (SOURCE_ROOT / "src" / "reviewed-qa.json").read_bytes(),
            "Published QA differs from the static independently reviewed desktop evidence")
    qa = json.loads(payloads[QA_NAME])
    require(qa.get("schema") == "trip162-qa-v1" and qa.get("result") == "PASS"
            and qa.get("blocking_findings") == [], "QA is incomplete or has blocking findings")
    for key, value in QA_REQUIRED.items():
        require(qa.get(key) is value, "Missing or failing QA assertion: " + key)
    require(qa.get("artifacts") == cfg["expected_mpp"], "QA and independently reviewed MPP hashes differ")
    require(qa.get("supported_package") == "ctrip.english" and qa.get("supported_version") == "8.54.2",
            "QA identifies another target application/version")
    require(qa.get("bundle_version") == BUNDLE and qa.get("trip_version") == VERSION,
            "QA identifies different patch versions")
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
    require(set(checksums) == set(ASSETS) - {"SHA256SUMS.txt"}, "Checksums must cover the other five assets")
    source_count = validate_sources(payloads[SOURCE_ZIP], args.config)
    notes = payloads["RELEASE_NOTES.txt"].decode()
    require(notes == (PUBLICATION / "RELEASE_NOTES.txt").read_text(), "Release notes differ from reviewed source")
    require(all(value in notes for value in (APP, VERSION, BUNDLE, "8.54.2", "未確認")),
            "Release notes omit the target, versions, or device limitation")
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Notes contain a competing changelog heading")
    for line in notes.splitlines():
        match = SCOPE.match(line.strip())
        require(match is None or match[1] == APP, "Release notes contain an unrelated application update scope")
    report.update(baseline_bundle_version=PREVIOUS, baseline_bundle_sha256=BASE_SHA256,
                  baseline_trip_version=PREVIOUS_APP, baseline_trip_sha256=SINGLE_BASE_SHA256,
                  qa_assertions_verified=list(QA_REQUIRED), source_files_checked=source_count,
                  qa_sha256=sha(payloads[QA_NAME]), android_device_tested=False,
                  original_apk_apply_tested=True)
    assets = {name: {"bytes": len(raw), "sha256": sha(raw)} for name, raw in payloads.items()}
    return cfg, payloads, notes, assets, report


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
                "User-Agent": "Hiro-Trip162-Publication",
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
    require(result.returncode == 0, "Git command failed: " + " ".join(map(str, args)) + "\n"
            + result.stderr.decode(errors="replace"))
    return result.stdout.decode().strip()


def prepare_commit(repo, state, files, message):
    env = {**os.environ, "GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_COMMITTER_NAME": "github-actions[bot]",
           "GIT_AUTHOR_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com",
           "GIT_COMMITTER_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com"}
    with tempfile.TemporaryDirectory(prefix="trip162-index-") as temporary:
        env["GIT_INDEX_FILE"] = str(Path(temporary) / "index")
        git(repo, "read-tree", state["head"], env=env)
        for path, raw in files.items():
            checked_path(path)
            blob = git(repo, "hash-object", "-w", "--stdin", input=raw, env=env)
            git(repo, "update-index", "--add", "--cacheinfo", f"100644,{blob},{path}", env=env)
        tree = git(repo, "write-tree", env=env)
        commit = git(repo, "commit-tree", tree, "-p", state["head"], "-F", "-",
                     input=(message + "\n").encode(), env=env)
        require(git(repo, "show", "-s", "--format=%P", commit) == state["head"],
                "Prepared commit is not a direct descendant")
        changes = set(git(repo, "diff-tree", "--no-commit-id", "--name-only", "-r", commit).splitlines())
        require(changes and changes.issubset(files), "Prepared commit changes unrelated paths")
        return {**state, "head": commit, "tree": tree}


def commit_feeds(repo, states, updates, message):
    require(set(states) == set(updates) == {"main", "dev"}, "Both Manager branches are required")
    check_heads(states)
    git(repo, "fetch", "--no-tags", f"https://github.com/{REPO}.git", states["main"]["head"], states["dev"]["head"])
    prepared = {branch: prepare_commit(repo, state, updates[branch], message) for branch, state in states.items()}
    check_heads(states)
    # Every prepared commit has its leased HEAD as sole parent. The explicit
    # leases reject races; --atomic prevents one Manager feed advancing alone.
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
            if not EXPERIMENTAL_ONLY.match(body) and (scope.casefold() == app.casefold()
                    or scope.casefold().startswith(app.casefold() + " - ")):
                return True
    return False


def utc_date(value):
    parsed = dt.datetime.fromisoformat(value)
    return parsed.astimezone(dt.timezone.utc).replace(tzinfo=None) if parsed.tzinfo else parsed


def metadata(state, cfg, url, created, notes):
    old = state["manifest"]
    require(old["version"] == PREVIOUS and old["download_url"] == BASE_URL,
            "Active bundle is no longer the reviewed baseline161")
    require(utc_date(created) > utc_date(old["created_at"]), "created_at must advance")
    lines = old["description"].splitlines(keepends=True)
    require(sum(line.rstrip("\r\n") == INVENTORY_OLD for line in lines) == 1,
            "Missing or ambiguous exact active Trip1.10.14 inventory line")
    previous_description = "".join(INVENTORY_NEW + line[len(INVENTORY_OLD):]
                                   if line.rstrip("\r\n") == INVENTORY_OLD else line for line in lines)
    description = notes.strip() + "\n\n" + previous_description
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": description}
    changelog = (f"# {BUNDLE} ({created[:10]})\n\n* **{APP}:** v{VERSION}：{cfg['summary']}\n\n"
                 + notes.strip() + "\n\n" + state["log"])
    require(has_changes_for(changelog, PREVIOUS, APP), "Missing Trip.com Manager update scope")
    require(not has_changes_for(changelog, BUNDLE, APP), "Freshly patched Trip.com would remain outdated")
    require(not any(has_changes_for(changelog, PREVIOUS, app) for app in OTHER_APPS),
            "False update scope for an unchanged application")
    entry = parsed_entries(changelog)[0]
    require(entry["version"] == BUNDLE and {scope for scope, _ in entry["bullets"]} == {APP},
            "Unexpected top-entry update scope")
    require(feed["description"].endswith(previous_description) and changelog.endswith(state["log"]),
            "Existing description or changelog history was lost")
    inverse = "".join(INVENTORY_OLD + line[len(INVENTORY_NEW):]
                      if line.rstrip("\r\n") == INVENTORY_NEW else line
                      for line in previous_description.splitlines(keepends=True))
    require(inverse == old["description"], "Unrelated inventory/description changed")
    return feed, changelog


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
    cfg, payloads, notes, assets, report = validate_local(args)
    if args.local_only:
        print(json.dumps({"status": "local_artifacts_verified", **report}, ensure_ascii=False, indent=2))
        return
    states = {branch: snapshot(branch) for branch in ("main", "dev")}
    check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow was triggered")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    require(sha(fetch(BASE_URL)) == BASE_SHA256, "Public baseline161 bytes differ")
    require(sha(fetch(SINGLE_BASE_URL)) == SINGLE_BASE_SHA256, "Public Trip1.10.14 baseline bytes differ")
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()
    for state in states.values():
        metadata(state, cfg, "https://example.invalid/preflight-only", created, notes)
    require(api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/tags/{TAG}", absent=True) is None, "Tag already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None,
            "Distribution branch exists; inspect before retry")
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes",
                          "heads": {branch: state["head"] for branch, state in states.items()},
                          "bundle_version": BUNDLE, "trip_version": VERSION, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "trip162-publication-receipt-v1", "trip_version": VERSION,
               "baseline_trip_version": PREVIOUS_APP, "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "source_commit": os.environ.get("GITHUB_SHA") or git(args.repo, "rev-parse", "HEAD"),
               "publication_manifest_sha256": sha(args.config.read_bytes()),
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
            "name": cfg["release_title"], "body": notes, "draft": True, "prerelease": True, "make_latest": "false"})
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
            "release: exact Trip11015 and bundle162 immutable MPP downloads")
        # An empty lease requires the dedicated distribution branch to be absent.
        git(args.repo, "push", f"--force-with-lease=refs/heads/{RAW_BRANCH}:", f"https://github.com/{REPO}.git",
            f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH) == raw_state["head"], "Distribution branch moved")
        urls = {name: f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{name}"
                for name in (SINGLE, COMBINED)}
        for name, url in urls.items():
            require(fetch(url) == payloads[name], "Immutable raw download differs: " + name)
        checkpoint("immutable_downloads_verified", raw_commit=raw_state["head"], download_urls=urls)
        check_heads(states)
        new_metadata, updates = {}, {}
        for branch, state in states.items():
            feed, changelog = metadata(state, cfg, urls[COMBINED], created, notes)
            new_metadata[branch] = (feed, changelog)
            updates[branch] = {"patches-bundle.json": json_bytes(feed), "CHANGELOG.md": changelog.encode()}
            if branch == "main":
                for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                    updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds(args.repo, states, updates, "release: Trip schedule UI and cache fix and bundle162 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "Existing ULike active policy changed")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?trip162={state['head']}")) == feed,
                    "Public branch feed mismatch")
            require(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?trip162={state['head']}").decode() == changelog,
                    "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_unchanged": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], assets, True)
        checkpoint("published_and_verified", published=True, release_url=PAGE,
                   manager_main_dev_updated_atomically=True, manager_trip_scope_verified=True,
                   other_apps_false_updates=False, existing_quicksearch_ulike_and_swiftkey_preserved=True,
                   fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified Trip162 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0],
                    "Feed changed while saving receipt")
        subprocess.run(["gh", "release", "upload", TAG, "--repo", REPO, str(receipt_path)], check=True)
        verify_assets(release["id"], {**assets, RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, Trip scope, other app retention and receipt", flush=True)
    except BaseException as error:
        checkpoint("failed_requires_inspection", error=type(error).__name__ + ": " + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--base", type=Path, required=True)
    parser.add_argument("--single-base", "--standalone-base", dest="single_base", type=Path, required=True)
    parser.add_argument("--config", type=Path, default=PUBLICATION / "manifest.json")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true")
    mode.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("dist", "repo", "base", "single_base", "config"):
        setattr(args, key, getattr(args, key).resolve())
    publication(args)


if __name__ == "__main__":
    main()
