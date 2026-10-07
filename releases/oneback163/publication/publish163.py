#!/usr/bin/env python3
"""Publish the reviewed OneBack 1.0.0 MPPs and atomically advance both feeds.

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
VERSION, PREVIOUS, BUNDLE = "1.0.0", "1.0.162", "1.0.163"
TAG = "oneback-v1.0.0"
RAW_BRANCH = "release/oneback163"
SINGLE = "OneBack_Exit_NoRecents_v1.0.0.mpp"
COMBINED = "Hiro_Morphe_Patches_v1.0.163.mpp"
BASE_NAME = "Hiro_Morphe_Patches_v1.0.162.mpp"
BASE_SHA256 = "17184ecbfe63b35871afff33a833a8b2b87d00d1bef8fcc22ce2997bcd297da5"
BASE_BYTES = 17322782
BASE_URL = (f"https://raw.githubusercontent.com/{REPO}/"
            f"50524e48dcc4851f4486e5b17b28010ec4cc4c19/downloads/{BASE_NAME}")
QA_NAME = "QA_OneBack_v1.0.0.json"
SOURCE_ZIP = "OneBack_v1.0.0_sources_and_QA.zip"
RECEIPT_NAME = "publication_OneBack_v1.0.0.json"
ASSETS = (SINGLE, COMBINED, QA_NAME, SOURCE_ZIP, "RELEASE_NOTES.txt", "SHA256SUMS.txt")
RELEASE_ROOT = "releases/oneback163"
POLICY_PATH = "releases/ULike_ACTIVE.json"
PAGE = f"https://github.com/{REPO}/releases/tag/{TAG}"
PUBLICATION = Path(__file__).resolve().parent
SOURCE_ROOT = PUBLICATION.parent
MF = "META-INF/MANIFEST.MF"
PATCH_PREFIX = "app/hiro/oneback/patches/"
RUNTIME_EXTENSION = "extensions/oneback_exit.mpe"
SINGLE_MANIFEST_NAME = "OneBack Exit No Recents"
APP_SCOPES = ("TikTok", "Instagram", "X", "Twitter", "Trip.com")
PACKAGE_APPS = {
    "com.ss.android.ugc.trill": "TikTok",
    "com.zhiliaoapp.musically": "TikTok",
    "com.instagram.android": "Instagram",
    "com.twitter.android": "X",
    "ctrip.english": "Trip.com",
}
QA_REQUIRED = {
    "android_device_tested": False,
    "existing_resource_entries_unchanged": True,
    "existing_loader_classes_unchanged": True,
    "standalone_and_bundle_new_entries_identical": True,
}
OTHER_APPS = ("ULike", "Microsoft SwiftKey Beta", "Microsoft SwiftKey Beta Keyboard",
              "SwiftKey Beta", "Berry Browser", "Yahoo!乗換案内", "Y!乗換案内",
              "Amazonショッピング", "Amazon Shopping", "Hanull Reader", "Uber Eats",
              "BrightnessClick", "明るさタッチ", "簡単検索くん", "LINE")
MANAGER_SOURCE = ("https://github.com/MorpheApp/morphe-manager/blob/"
                  "0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/"
                  "app/src/main/java/app/morphe/manager/util/ChangelogParser.kt")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)
REQUIRED_SOURCES = (
    "src/build.py", "src/reviewed-qa.json", "publication/publish163.py", "publication/manifest.json",
    "publication/RELEASE_NOTES.txt", "publication/oneback163-publish.yml",
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
    require(cfg.get("schema") == "oneback163-publication-v1" and cfg.get("review_status") == "reviewed",
            "Publication manifest is pending review or has another schema")
    require((cfg.get("repo"), cfg.get("patch_version"), cfg.get("bundle_version")) == (REPO, VERSION, BUNDLE),
            "Publication identity differs")
    require(cfg.get("baseline_bundle") == {"version": PREVIOUS, "file": BASE_NAME,
            "bytes": BASE_BYTES, "sha256": BASE_SHA256, "url": BASE_URL}, "Pinned bundle baseline differs")
    expected = cfg.get("expected_mpp")
    require(isinstance(expected, dict) and set(expected) == {SINGLE, COMBINED},
            "Expected exactly the two independently reviewed MPP hashes")
    for name, row in expected.items():
        digest_row(row, name)
    require(isinstance(cfg.get("expected_qa_sha256"), str)
            and re.fullmatch(r"[0-9a-f]{64}", cfg["expected_qa_sha256"]), "Missing independently reviewed QA hash")
    prefix = cfg.get("patch_class_prefix")
    require(prefix == PATCH_PREFIX,
            "Missing or invalid new-patch class prefix")
    additions = cfg.get("new_entries")
    require(isinstance(additions, dict) and additions, "Missing reviewed new-entry allowlist")
    for name, row in additions.items():
        checked_path(name)
        require((name.startswith(prefix) and name.endswith(".class"))
                or name == RUNTIME_EXTENSION,
                "Entry outside the new common patch: " + name)
        digest_row(row, name)
    require(any(name.startswith(prefix) for name in additions), "Missing new JVM patch class")
    require(RUNTIME_EXTENSION in additions, "Missing new runtime extension")
    packages = cfg.get("supported_packages")
    require(isinstance(packages, list) and len(packages) == len(set(packages))
            and set(packages) == set(PACKAGE_APPS)
            and {PACKAGE_APPS[name] for name in packages} == {"TikTok", "Instagram", "X", "Trip.com"},
            "Common patch does not target exactly the requested four applications")
    require(cfg.get("patch_default_enabled") is True, "New common patch must be selected by default")
    require(cfg.get("single_manifest_name") == SINGLE_MANIFEST_NAME, "Standalone identity differs")
    for key in ("single_manifest_name", "release_title", "summary"):
        require(isinstance(cfg.get(key), str) and cfg[key].strip() and "\n" not in cfg[key]
                and not re.search(r"\b(?:TODO|PLACEHOLDER|TBD)\b", cfg[key]), "Missing reviewed release text: " + key)
    extra_qa = cfg.get("required_qa_assertions", {})
    require(isinstance(extra_qa, dict) and all(type(v) is bool for v in extra_qa.values()),
            "Additional reviewed QA assertions must be booleans")
    require(not any(key in QA_REQUIRED and value is not QA_REQUIRED[key] for key, value in extra_qa.items()),
            "Additional QA assertions may not weaken fixed preservation checks")
    return cfg


def validate_entry_delta(old, new, single, cfg):
    """Preserve every old resource and JVM patch; allow only a new common patch."""
    removed, added = set(old) - set(new), set(new) - set(old)
    changed = {name for name in old.keys() & new.keys() if old[name] != new[name]}
    own = {name: raw for name, raw in single.items() if name not in (MF, "classes.dex")}
    require(not removed, "An existing bundle entry was removed")
    require(changed == {MF, "classes.dex"}, "An existing bundle resource changed or loader/version was not updated")
    require(set(own) == set(cfg["new_entries"]) == added, "Reviewed new patch entry inventory differs")
    require(not (set(own) & set(old)), "New patch collides with an existing bundle entry")
    require(own and all(new.get(name) == raw for name, raw in own.items()),
            "Standalone and bundle common-patch resources differ")
    for name, raw in own.items():
        row = cfg["new_entries"][name]
        require(len(raw) == row["bytes"] and sha(raw) == row["sha256"], "New entry differs from review: " + name)
        if name.endswith(".class"):
            require(raw.startswith(b"\xca\xfe\xba\xbe"), "Invalid JVM patch class: " + name)
        elif name.endswith(".mpe"):
            require(raw.startswith(b"dex\n"), "Invalid runtime DEX: " + name)
    existing = {name: raw for name, raw in old.items() if name not in (MF, "classes.dex")}
    require(existing and all(new.get(name) == raw for name, raw in existing.items()),
            "Existing patches or extension resources changed")
    return {"bundle_changed_entries": sorted(changed), "bundle_added_entries": sorted(added),
            "bundle_removed_entries": [], "existing_resource_entries_byte_identical": len(existing),
            "standalone_and_bundle_new_entries_identical": True}


def validate_sources(raw, cfg_path):
    entries = archive(raw)
    allowed = {".java", ".kt", ".py", ".json", ".txt", ".md", ".tsv", ".log", ".yaml", ".yml",
               ".js", ".cjs", ".mjs", ".template", ".patch", ".sha256", ".b64", ".html", ".xml", ".sh"}
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
    return len(entries)


def validate_local(args):
    cfg = load_config(args.config)
    baseline = args.base.read_bytes()
    require(len(baseline) == BASE_BYTES and sha(baseline) == BASE_SHA256, "Wrong pinned baseline162 MPP bytes")
    payloads = {name: (args.dist / name).read_bytes() for name in ASSETS}
    for name, row in cfg["expected_mpp"].items():
        require(len(payloads[name]) == row["bytes"] and sha(payloads[name]) == row["sha256"],
                "CI output differs from independently reviewed MPP: " + name)
    old, new, single = map(archive, (baseline, payloads[COMBINED], payloads[SINGLE]))
    for items, version, name in ((old, PREVIOUS, "Hiro Morphe Patches"),
                                 (new, BUNDLE, "Hiro Morphe Patches"),
                                 (single, VERSION, cfg["single_manifest_name"])):
        fields = headers(items[MF])
        require(fields.get("Version") == version and fields.get("Name") == name, "MPP identity/version differs")
        require(items.get("classes.dex", b"").startswith(b"dex\n"), "Invalid patch-loader DEX")
    report = validate_entry_delta(old, new, single, cfg)
    require(sha(payloads[QA_NAME]) == cfg["expected_qa_sha256"], "QA differs from independent desktop review")
    require(payloads[QA_NAME] == (SOURCE_ROOT / "src" / "reviewed-qa.json").read_bytes(),
            "Published QA differs from the static independently reviewed desktop evidence")
    qa = json.loads(payloads[QA_NAME])
    require(qa.get("schema") == "oneback163-qa-v1" and qa.get("result") == "PASS"
            and qa.get("blocking_findings") == [], "QA is incomplete or has blocking findings")
    required_qa = {**QA_REQUIRED, **cfg.get("required_qa_assertions", {})}
    for key, value in required_qa.items():
        require(qa.get(key) is value, "Missing or failing QA assertion: " + key)
    require(qa.get("artifacts") == cfg["expected_mpp"], "QA and independently reviewed MPP hashes differ")
    require(qa.get("supported_packages") == cfg["supported_packages"], "QA identifies another target application set")
    require(qa.get("bundle_version") == BUNDLE and qa.get("patch_version") == VERSION,
            "QA identifies different patch versions")
    require(qa.get("patch_default_enabled") is True, "QA did not verify the default patch selection")
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
    require(all(value in notes for value in ("TikTok", "Instagram", "X", "Trip.com", VERSION, BUNDLE, "未確認")),
            "Release notes omit the targets, versions, or device limitation")
    require(not any(HEADING.match(line) for line in notes.splitlines()), "Notes contain a competing changelog heading")
    for line in notes.splitlines():
        match = SCOPE.match(line.strip())
        require(match is None or match[1] in APP_SCOPES, "Release notes contain an unrelated application update scope")
    require(type(qa.get("original_apk_apply_tested")) is bool,
            "QA must explicitly report whether original APK application was tested")
    report.update(baseline_bundle_version=PREVIOUS, baseline_bundle_sha256=BASE_SHA256,
                  qa_assertions_verified=list(required_qa), source_files_checked=source_count,
                  qa_sha256=sha(payloads[QA_NAME]), android_device_tested=False,
                  original_apk_apply_tested=qa["original_apk_apply_tested"],
                  validation_coverage=qa.get("validation_coverage", {}),
                  supported_packages=qa["supported_packages"], patch_default_enabled=True)
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
                "User-Agent": "Hiro-OneBack163-Publication",
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
    with tempfile.TemporaryDirectory(prefix="oneback163-index-") as temporary:
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
            "Active bundle is no longer the reviewed baseline162")
    require(utc_date(created) > utc_date(old["created_at"]), "created_at must advance")
    previous_description = old["description"]
    description = notes.strip() + "\n\n" + previous_description
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": description}
    bullets = "\n".join(f"* **{app}:** 共通パッチ v{VERSION}：{cfg['summary']}" for app in APP_SCOPES)
    changelog = (f"# {BUNDLE} ({created[:10]})\n\n" + bullets + "\n\n"
                 + notes.strip() + "\n\n" + state["log"])
    for app in APP_SCOPES:
        require(has_changes_for(changelog, PREVIOUS, app), "Missing Manager update scope: " + app)
        require(not has_changes_for(changelog, BUNDLE, app), "Freshly patched application would remain outdated: " + app)
    historical_scopes = {scope for entry in parsed_entries(state["log"]) for scope, _ in entry["bullets"]}
    unchanged_apps = set(OTHER_APPS) | (historical_scopes - set(APP_SCOPES))
    # Sub-scopes of the four changed apps legitimately match those app names.
    unchanged_apps = {app for app in unchanged_apps if not any(
        app.casefold() == target.casefold() or app.casefold().startswith(target.casefold() + " - ")
        for target in APP_SCOPES)}
    require(not any(has_changes_for(changelog, PREVIOUS, app) for app in unchanged_apps),
            "False update scope for an unchanged application")
    entry = parsed_entries(changelog)[0]
    require(entry["version"] == BUNDLE and {scope for scope, _ in entry["bullets"]} == set(APP_SCOPES),
            "Unexpected top-entry update scope")
    require(feed["description"] == notes.strip() + "\n\n" + old["description"]
            and changelog.endswith(state["log"]), "Existing inventory, description or changelog history was changed")
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
    require(sha(fetch(BASE_URL)) == BASE_SHA256, "Public baseline162 bytes differ")
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
                          "bundle_version": BUNDLE, "patch_version": VERSION, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "oneback163-publication-receipt-v1", "patch_version": VERSION,
               "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "source_commit": os.environ.get("GITHUB_SHA") or git(args.repo, "rev-parse", "HEAD"),
               "publication_manifest_sha256": sha(args.config.read_bytes()),
               "assets": assets, "local_validation": report, "android_device_tested": False,
               "original_apk_apply_tested": report["original_apk_apply_tested"], "manager_parser_source": MANAGER_SOURCE,
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
            "release: exact OneBack100 and bundle163 immutable MPP downloads")
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
        active = commit_feeds(args.repo, states, updates, "release: OneBack exit and recents removal and bundle163 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "Existing ULike active policy changed")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            require(json.loads(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?oneback163={state['head']}")) == feed,
                    "Public branch feed mismatch")
            require(fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/CHANGELOG.md?oneback163={state['head']}").decode() == changelog,
                    "Public changelog mismatch")
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_unchanged": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], assets, True)
        checkpoint("published_and_verified", published=True, release_url=PAGE,
                   manager_main_dev_updated_atomically=True, manager_app_scopes_verified=list(APP_SCOPES),
                   other_apps_false_updates=False, all_existing_patch_resources_preserved=True,
                   fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified OneBack163 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0],
                    "Feed changed while saving receipt")
        subprocess.run(["gh", "release", "upload", TAG, "--repo", REPO, str(receipt_path)], check=True)
        verify_assets(release["id"], {**assets, RECEIPT_NAME: {"bytes": len(receipt_bytes), "sha256": sha(receipt_bytes)}}, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, four-app scopes, all existing patches and receipt", flush=True)
    except BaseException as error:
        checkpoint("failed_requires_inspection", error=type(error).__name__ + ": " + str(error))
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dist", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--base", type=Path, required=True)
    parser.add_argument("--config", type=Path, default=PUBLICATION / "manifest.json")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true")
    mode.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("dist", "repo", "base", "config"):
        setattr(args, key, getattr(args, key).resolve())
    publication(args)


if __name__ == "__main__":
    main()
