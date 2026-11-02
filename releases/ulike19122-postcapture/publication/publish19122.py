#!/usr/bin/env python3
"""Publish the frozen ULike .122 and bundle .328; retain atomic main/dev feed transactions.

Private original APKs, photographs, diagnostic logs and toolchain archives are never
publication assets. The device speed/quality scope remains explicitly unverified.
"""
from __future__ import annotations
import argparse
import datetime as dt
import hashlib
import base64
import io
import sys
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


REPO="hiro191u3n2/hiro-morphe-patches"
VERSION,BUNDLE,PREVIOUS_APP,PREVIOUS="1.9.122","1.0.328","1.9.121","1.0.327"
TAG="ulike-v1.9.122"
PAGE=f"https://github.com/{REPO}/releases/tag/{TAG}"
RAW_BRANCH="ulike19122-postcapture-downloads"
RELEASE_ROOT="releases/ulike19122-postcapture"
WORKFLOW=".github/workflows/ulike19122-postcapture-publish.yml"
CONFIG="publication/manifest19122.json"
RELEASE_DATE="2026-11-02"
POLICY_PATH="releases/ULike_ACTIVE.json"
SINGLE="ULike_HQ_Texture_Online_v1.9.122.mpp"
COMBINED="Hiro_Morphe_Patches_v1.0.328.mpp"
QA_NAME="QA_ULike_v1.9.122.json"
SOURCE_ZIP="ULike_v1.9.122_sources_and_QA.zip"
RECEIPT_NAME="publication_ULike_v1.9.122.json"
ASSETS=(SINGLE,COMBINED,QA_NAME,SOURCE_ZIP,"RELEASE_NOTES.txt","SHA256SUMS.txt")
BASE_SHA256="08fdc138369a972d7807ef684c021411183eb0d398297ca241d41ca5bddca8a7"
BASE_URL="https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/df736c1adf7dcfd215bbefdc056064e27cb830f4/downloads/Hiro_Morphe_Patches_v1.0.327.mpp"
SINGLE_BASE_SHA256="cfa0963fb2ec3e5207cff34b3a0d953a300ad5f69a64413e6e3b16d7e6b31fca"
INVENTORY=re.compile(r"(?m)^(ULike：)v(1\.9\.121)(（5\.6\.2／740）)\r?$")
HEADING = re.compile(r"^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)", re.I)
SCOPE = re.compile(r"^\* \*\*(.+?):\*\*")
EXPERIMENTAL_ONLY = re.compile(r"^Add(?:ed)?\s+experimental\s+support\s+for\b", re.I)


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
                "User-Agent": "Hiro-ULike19101-Publication",
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
    # Git refs are authoritative immediately after atomic push; REST refs may lag.
    result = git(Path.cwd(), "ls-remote", "--refs", f"https://github.com/{REPO}.git", "refs/heads/" + branch)
    rows = [line.split() for line in result.splitlines() if line.strip()]
    require(len(rows) == 1 and rows[0][1] == "refs/heads/" + branch
            and re.fullmatch(r"[0-9a-f]{40}", rows[0][0]), "Missing exact Git branch ref: " + branch)
    return rows[0][0]


def content(path, ref):
    checked_path(path)
    require(re.fullmatch(r"[a-f0-9]{40}",ref) is not None,"Content must use an exact commit SHA")
    endpoint=f"repos/{REPO}/contents/{path}?ref={ref}"
    row = api(endpoint)
    require(row.get("type")=="file" and row.get("path")==path and
            re.fullmatch(r"[a-f0-9]{40}",row.get("sha","")) is not None,
            "Repository content identity differs")
    if row.get("encoding")=="base64":
        raw=base64.b64decode(row["content"])
    else:
        require(row.get("encoding")=="none" and type(row.get("size")) is int and
                row["size"]>1024*1024,"Unexpected repository file encoding")
        raw=subprocess.check_output(["gh","api",endpoint,"-H","Accept: application/vnd.github.raw+json"])
    require(type(row.get("size")) is int and len(raw)==row["size"],"Repository content size differs")
    blob=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
    require(blob==row["sha"],"Repository content differs from exact-commit Git blob")
    return raw


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
    with tempfile.TemporaryDirectory(prefix="ulike19101-index-") as temp:
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
    # Each proposed commit has exactly the observed old HEAD as its sole parent.
    # Normal fast-forward-only push rejects an intervening update to either ref.
    git(repo, "push", "--atomic", f"https://github.com/{REPO}.git",
        f"{prepared['main']['head']}:refs/heads/main", f"{prepared['dev']['head']}:refs/heads/dev")
    # Retry only known old-to-new visibility; any third SHA is a concurrent write.
    for attempt in range(8):
        actual = {branch: head(branch) for branch in prepared}
        require(all(actual[b] in (states[b]['head'],prepared[b]['head']) for b in actual),
                'Unexpected concurrent branch HEAD after atomic push')
        if all(actual[b] == prepared[b]['head'] for b in actual):
            break
        require(attempt < 7, 'Atomic feed refs did not become visible within 14 seconds')
        time.sleep(2)
    return prepared

def utc_created(states):
    now = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0)
    require(all(now > dt.datetime.fromisoformat(s["manifest"]["created_at"]) for s in states.values()),
            "Current UTC time must follow both old feed timestamps")
    return now.isoformat()


def is_ulike(name):
    return name.startswith("app/hiro/ulike/patches/") or re.match(r"^ulike(?:\d+)?/", name) is not None


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


def file_row(path):
    raw = path.read_bytes()
    return {"bytes": len(raw), "sha256": sha(raw)}


def source_files(repo):
    """Freeze only the new release and its single workflow, including staged pins."""
    root = repo / RELEASE_ROOT
    require(root.is_dir(), "New release root missing")
    result = {}
    for path in sorted(root.rglob('*')):
        if not path.is_file() or '__pycache__' in path.parts or path.suffix == '.pyc':
            continue
        relative = path.relative_to(root).as_posix()
        if relative in (CONFIG, RECEIPT_NAME):
            continue
        require(not path.is_symlink(), "Source symlink forbidden")
        require(path.suffix.lower() not in ('.apk', '.apks', '.apkm'), "Private application in release sources")
        result[path.relative_to(repo).as_posix()] = file_row(path)
    path = repo / WORKFLOW
    require(path.is_file(), "Publication workflow missing")
    result[WORKFLOW] = file_row(path)
    return result


def read_json(path):
    return json.loads(path.read_text())


def verify_source_hashes(report, key, root):
    rows = report.get(key)
    require(type(rows) is dict and rows, "Missing tested/compiled source hashes: " + key)
    for name, digest in rows.items():
        checked_path(name)
        require(sha((root / name).read_bytes()) == digest, "Source changed: " + name)


def verify_application(report, digest, name):
    require(report.get('status') == 'passed' and report.get('mpp', {}).get('sha256') == digest,
            name + " original APKS proof targets other bytes")
    require(report.get('runtime_version') == VERSION and report.get('package_name') == 'com.gorgeous.liteinternational',
            name + " applied identity differs")
    require(report.get('original_apk_apply_tested') is True and report.get('full_dex_compilation_tested') is True,
            name + " actual full rebuild proof missing")
    require(report.get('physical_android_tested') is False and report.get('signing_or_installation_tested') is False,
            name + " unexpected device/install claim")
    require(report.get('expected_runtime_contracts', 0) >= 6400 and report.get('all_apk_linkage_count', 0) > 0,
            name + " exact DEX/linkage verification missing")


def parse_host_specs(values):
    result = {}
    for value in values or []:
        name, separator, location = value.partition('=')
        require(separator and re.fullmatch(r'[a-z][a-z0-9_-]*', name) and location, 'Expected --host-report NAME=PATH')
        require(name not in result, 'Duplicate host suite: ' + name)
        result[name] = Path(location)
    require(result, 'At least one explicit host suite is required')
    return result


def host_summary(report):
    require(report.get('status') in ('passed', 'PASS'), 'Host suite did not pass')
    count = report.get('assertions')
    require(type(count) is int and count > 0, 'Host assertion count missing')
    negative = report.get('negative_controls', 0)
    if isinstance(negative, list):
        require(all(isinstance(row, dict) and (row.get('detected') is True or
                    row.get('status') in ('rejected', 'expected_failure')) for row in negative), 'Host negative control not detected')
        negative = len(negative)
    elif isinstance(negative, dict):
        require(all(type(name) is str and value is True for name, value in negative.items()), 'Host negative control did not pass')
        negative = len(negative)
    require(type(negative) is int and negative >= 0, 'Invalid negative-control count')
    require(report.get('physical_android_tested', False) is False and
            report.get('device_speedup_verified', False) is False, 'Host report claims device validation')
    return {'status': report['status'], 'assertions': count, 'negative_controls': negative,
            'negative_control_evidence': report.get('negative_controls', 0),
            'source_sha256': report.get('source_sha256')}


def verify_static_ulike_assets(base, single):
    """Compare unchanged pixels/installers directly, independently of build claims."""
    old = {n: raw for n, raw in base.items() if is_ulike(n)}
    new = {n: raw for n, raw in single.items() if is_ulike(n)}
    require(set(old) == set(new), 'ULike asset inventory changed')
    permitted = {'ulike/runtime.dex', 'app/hiro/ulike/patches/UlikeHqMaxPatch.class'}
    require(permitted.issubset(old), 'Expected ULike runtime/loader missing')
    require(all(new[n] == raw for n, raw in old.items() if n not in permitted),
            'Static ULike native/installer/contracts asset changed')
    native_old = {n: raw for n, raw in old.items() if raw.startswith(b'\x7fELF')}
    native_new = {n: raw for n, raw in new.items() if raw.startswith(b'\x7fELF')}
    require(len(native_old) == 14 and native_old == native_new, 'Fourteen native payloads not preserved')
    for n in ('ulike/methods.dex', 'ulike/methods.tsv'):
        require(n in old and new[n] == old[n], 'Original method contracts changed: ' + n)


def inspect_inputs(args, artifact_pins=None):
    """Bind the prepared release, every host suite, and both original APK proofs."""
    base_raw = args.baseline.read_bytes()
    require(sha(base_raw) == BASE_SHA256, 'Combined baseline must be active .327')
    single_raw = (args.dist / SINGLE).read_bytes()
    single_hash = sha(single_raw)
    source_raw = (args.dist / SOURCE_ZIP).read_bytes()
    if artifact_pins is not None:
        for name, raw in ((SINGLE, single_raw), (SOURCE_ZIP, source_raw)):
            require(artifact_pins[name] == {'bytes': len(raw), 'sha256': sha(raw)}, 'Frozen artifact changed: ' + name)
    source_zip = archive(source_raw)
    require(not any(n.lower().endswith(('.apk', '.apks', '.apkm')) for n in source_zip), 'Private application in source archive')
    source_manifest = json.loads(source_zip['manifest.json'])['files']
    require(set(source_zip) == set(source_manifest) | {'manifest.json'}, 'Source inventory differs')
    for name, metadata in source_manifest.items():
        raw = source_zip[name]
        require(metadata == {'bytes': len(raw), 'sha256': sha(raw)}, 'Source archive entry changed: ' + name)
        require((args.source / name).read_bytes() == raw, 'Extracted source differs: ' + name)
    original_qa = json.loads(source_zip[QA_NAME])
    require(original_qa['version'] == VERSION and original_qa['artifact'] ==
            {'name': SINGLE, 'bytes': len(single_raw), 'sha256': single_hash}, 'Prepared local QA differs')
    require(original_qa['automatic_qualification_enabled'] is False and
            original_qa['unqualified_gpu_admission_enabled'] is False, 'Quiet/admission policy differs')
    build = read_json(args.build_report)
    require(build['artifact']['sha256'] == single_hash and build['native_payload_count'] == 14,
            'Rebuilt .122 identity or native count differs')
    require(build.get('deterministic_dex_verified') is True and build.get('deterministic_archive_verified') is True
            and build.get('native_assets_byte_identical') is True and build.get('helper_references_verified') is True,
            'Build preservation/reproducibility evidence missing')
    verify_source_hashes(build, 'compiled_input_sha256', args.source / 'src')
    hosts = {}
    for name, path in sorted(args.host_reports.items()):
        report = read_json(path)
        summary = host_summary(report)
        verify_source_hashes(report, 'source_sha256', args.source / 'src')
        hosts[name] = summary
    require(hosts, 'No host evidence provided')
    require(original_qa['host_assertions'] == sum(row['assertions'] for row in hosts.values()), 'Frozen QA host assertion total differs')
    require(original_qa['host_negative_controls'] == sum(row['negative_controls'] for row in hosts.values()), 'Frozen QA negative-control total differs')
    combined_raw = (args.dist / COMBINED).read_bytes()
    combined_hash = sha(combined_raw)
    report = read_json(args.bundle_report)
    require(report.get('schema') == 'ulike19122-bundle-v1' and report.get('status') == 'passed', 'Bundle proof missing')
    require(report.get('baseline_sha256') == BASE_SHA256 and report.get('standalone_sha256') == single_hash
            and report.get('combined_sha256') == combined_hash, 'Bundle proof identity differs')
    require(report.get('unrelated_entries_preserved') is True and report.get('unrelated_loader_classes_preserved') is True,
            'Other-app preservation not proven')
    base, single, combined = map(archive, (base_raw, single_raw, combined_raw))
    verify_static_ulike_assets(base, single)
    for entries, version in ((base, PREVIOUS), (single, VERSION), (combined, BUNDLE)):
        require(headers(entries['META-INF/MANIFEST.MF']).get('Version') == version, 'MPP version differs')
        require(entries['classes.dex'].startswith(b'dex\n'), 'MPP loader missing')
    special = {'META-INF/MANIFEST.MF', 'classes.dex'}
    require(all(is_ulike(n) or n in special for n in single), 'Unexpected standalone contents')
    preserved = {n: raw for n, raw in base.items() if n not in special and not is_ulike(n)}
    own = {n: raw for n, raw in single.items() if is_ulike(n)}
    require(own and all(combined.get(n) == raw for n, raw in preserved.items()), 'Unrelated combined asset changed')
    require({n: raw for n, raw in combined.items() if is_ulike(n)} == own, 'Standalone and combined ULike differ')
    require(set(combined) == set(preserved) | set(own) | special, 'Unexpected combined inventory')
    application = json.loads(source_zip['evidence/application19122.json'])
    verify_application(application, single_hash, 'Standalone')
    abi = json.loads(source_zip['evidence/native-abi19122.json'])
    require(abi.get('status') == 'passed' and abi.get('mpp_sha256') == single_hash and
            abi.get('apk_sha256') == application['output_apk']['sha256'], 'Reflection ABI proof targets another APK/MPP')
    require(abi.get('exact_reflection_chain_verified') is True and abi.get('display_only_endpoint_verified') is True
            and abi.get('physical_android_tested') is False, 'Guarded preview reflection ABI not proven')
    require(type(abi.get('assertions')) is int and abi['assertions'] > 0, 'Reflection ABI checks missing')
    verify_source_hashes(abi, 'source_sha256', args.source / 'src')
    require(original_qa.get('preview_original_apk_abi') == abi, 'Local QA reflection ABI evidence differs')
    combined_application = read_json(args.combined_application)
    verify_application(combined_application, combined_hash, 'Combined')
    require(application['original_input'] == combined_application['original_input'], 'Original input differs across patch forms')
    return {'original_qa': original_qa, 'build': build, 'hosts': hosts, 'bundle': report,
            'application': combined_application, 'abi': abi}


def verify(args):
    cfg = read_json(args.manifest)
    require(cfg.get('schema') == 'ulike19122-publication-v1' and cfg.get('ready') is True, 'Publication not frozen')
    require(cfg.get('publication_authorized_by_user') is True, 'Explicit user publication instruction required')
    require((cfg['version'], cfg['bundle_version'], cfg['previous_bundle']) == (VERSION, BUNDLE, PREVIOUS), 'Manifest versions differ')
    require(cfg['source_files'] == source_files(args.repo), 'Reviewed publication code/payload changed')
    reports = inspect_inputs(args, cfg['artifacts'])
    require(cfg['host_suites'] == reports['hosts'], 'Fresh host suites/counts/tested sources differ from frozen QA')
    require(cfg['bundle_report_sha256'] == file_row(args.bundle_report)['sha256'] and
            cfg['combined_application_sha256'] == file_row(args.combined_application)['sha256'], 'Local combined evidence changed')
    payloads = {n: (args.dist / n).read_bytes() for n in ASSETS}
    expected = {n: {'bytes': len(v), 'sha256': sha(v)} for n, v in payloads.items()}
    require(expected == cfg['artifacts'], 'Frozen release artifacts changed')
    qa = json.loads(payloads[QA_NAME])
    require(qa['publication']['artifacts'] == {n: expected[n] for n in (SINGLE, COMBINED)}, 'Publication QA MPP identity differs')
    require(qa['publication']['bundle_report'] == reports['bundle'] and qa['publication']['combined_application'] == reports['application'],
            'Publication QA evidence differs')
    require(qa['publication']['host_suites'] == reports['hosts'], 'Publication QA host evidence differs')
    require(qa['publication']['apk_reflection_abi'] == reports['abi'], 'Publication QA reflection ABI differs')
    notes = payloads['RELEASE_NOTES.txt'].decode()
    require('実機' in notes and ('未確認' in notes or '未測定' in notes), 'Device limitation missing')
    sums = ''.join(expected[n]['sha256'] + '  ' + n + '\n' for n in sorted(payloads) if n != 'SHA256SUMS.txt').encode()
    require(payloads['SHA256SUMS.txt'] == sums, 'Asset checksums differ')
    return cfg, qa, payloads, expected, notes


def metadata(state, url, created, notes):
    old = state['manifest']
    require(old['version'] == PREVIOUS and old['download_url'] == BASE_URL, 'Active bundle advanced')
    require(len(list(INVENTORY.finditer(old['description']))) == 1, 'Exact ULike inventory missing')
    description = INVENTORY.sub(lambda m: m[1] + 'v' + VERSION + m[3], old['description'], count=1)
    feed = {**old, 'version': BUNDLE, 'created_at': created, 'download_url': url,
            'page_url': PAGE, 'signature_download_url': '', 'description': notes.strip() + '\n\n' + description}
    # Freeze the user-facing release date from this request's Japan date.
    # Manager keeps its existing increasing UTC created_at protocol separately.
    day = RELEASE_DATE
    changelog = (f'# [{BUNDLE}]({PAGE}) ({day})\n\n'
        '* **ULike:** v1.9.122：撮影・保存時の同時処理を調整し、保存後の未使用資源の整理と操作応答の計測を追加。通常撮影の画質設定と認定済み経路を維持。\n'
        '* **ULike:** ホスト動作検証と単独版・統合版の元APKS適用を確認。実機の軽快さは未確認。\n\n' + notes.strip() + '\n\n' + state['log'])
    require(changelog.endswith(state['log']) and feed['description'].endswith(description), 'Previous history lost')
    require(has_changes_for(changelog, PREVIOUS, ['ULike']) and not has_changes_for(changelog, BUNDLE, ['ULike']), 'Manager update detection differs')
    scopes = {s for e in parsed_entries(state['log']) for s, _ in e['bullets']}
    require(not any(has_changes_for(changelog, PREVIOUS, [s]) for s in scopes
                    if s.casefold() != 'ulike' and not s.casefold().startswith('ulike - ')), 'Other application falsely marked updated')
    return feed, changelog


def new_policy(state, qa, expected):
    old = json.loads(state['ulike_policy'])
    require(old.get('ulike_version') == PREVIOUS_APP and old.get('source_sha256') == SINGLE_BASE_SHA256,
            'Active ULike baseline changed')
    require(old.get('approved_lineage_version') == '1.8.8' and '1.9.17' in old.get('withdrawn_versions', []), 'Approved lineage differs')
    policy = dict(old)
    policy.update(ulike_version=VERSION, bundle_version=BUNDLE, source_filename=SINGLE,
        source_sha256=expected[SINGLE]['sha256'], source_release=TAG, active_release=TAG,
        base_ulike_version=PREVIOUS_APP, base_bundle_version=PREVIOUS, base_source_sha256=SINGLE_BASE_SHA256,
        historical_policy_reference={'commit': state['head'], 'path': POLICY_PATH, 'sha256': sha(state['ulike_policy'])},
        current_validation=qa, release_status='postcapture122_host_single_combined_apks_verified_device_unverified',
        current_fix_scope=qa.get('fix_scope', ['save_resource_admission', 'idle_resource_cleanup', 'post_save_responsiveness_observation', 'explicit_diagnostic_comparison']),
        selected_candidates=[], automatic_additional_qualification_enabled=False,
        experimental_gpu_auto_adoption=False, unqualified_gpu_admission_enabled=False,
        original_apk_apply_tested=True, combined_original_apk_apply_tested=True,
        android_device_tested=False, device_quality_verified=False, device_save_speed_measured=False,
        gpu_execution_on_physical_android=False, samsung_gpu_failure_root_cause_confirmed=False,
        native_installer_preserved_rows=14, native_installer_updated_rows=0,
        native_installer_added_rows=0, native_installer_total_rows=14,
        capture_fusion_enabled=False, single_image_only=True,
        reason='.122 limits concurrent save resource use, safely trims eligible idle caches, and records post-save responsiveness for explicit comparison. Normal image algorithms, output resolution and certified GPU admission remain. Automatic additional qualification remains disabled. Bundle .328 replaces only ULike in active .327. Physical Android responsiveness and the device-specific root cause remain unverified.',
        future_ulike_base='Use .122 source and exact standalone hash, but choose the then-current combined baseline from patches-bundle.json. Preserve disabled automatic qualification, exact/fast admission, CPU fallback, all fourteen native assets, capture/save contracts and all other applications. Do not infer device performance from host tests.')
    return policy


def publication(args):
    cfg, qa, payloads, expected, notes = verify(args)
    if args.local_only:
        print(json.dumps({'status': 'local_artifacts_verified', 'artifacts': expected}, indent=2)); return
    states = {b: snapshot(b) for b in ('main', 'dev')}
    source_commit = git(args.repo, 'rev-parse', 'HEAD')
    require(states['main']['head'] == source_commit, 'Main advanced after this source checkout')
    require(git(args.repo, 'show', '-s', '--format=%P', source_commit) == cfg['reviewed_main'], 'Source commit is not the reviewed direct child')
    require(states['dev']['head'] == cfg['reviewed_dev'], 'Dev advanced before publication')
    workflow_commit = os.environ.get('GITHUB_SHA')
    require(not workflow_commit or workflow_commit == source_commit, 'Workflow SHA differs from checkout')
    changes = set(git(args.repo, 'diff-tree', '--no-commit-id', '--name-only', '-r', source_commit).splitlines())
    require(changes and all(n == WORKFLOW or n.startswith(RELEASE_ROOT + '/') for n in changes), 'Source commit changes unrelated paths')
    require(states['main']['manifest'] == states['dev']['manifest'], 'Main/dev feeds diverged')
    require(states['main']['ulike_policy'] == states['dev']['ulike_policy'], 'Main/dev ULike policy diverged')
    require(sha(states['main']['ulike_policy']) == cfg['reviewed_policy_sha256'], 'ULike policy changed')
    require(sha(fetch(states['main']['manifest']['download_url'])) == BASE_SHA256, 'Active combined bytes changed')
    created = utc_created(states)
    policies = {b: new_policy(states['main'], qa, expected) for b in states}
    for s in states.values():
        metadata(s, 'https://example.invalid/preflight-only', created, notes)
    for path in (f'releases/tags/{TAG}', f'git/ref/tags/{TAG}', f'git/ref/heads/{RAW_BRANCH}'):
        require(api(f'repos/{REPO}/' + path, absent=True) is None, 'Existing release/tag/branch; inspect instead of overwriting')
    require(not any(a['name'] == COMBINED for r in api(f'repos/{REPO}/releases?per_page=100') for a in r.get('assets', [])), 'Bundle version already used')
    check_heads(states)
    if args.preflight_only:
        print(json.dumps({'status': 'preflight_passed_no_remote_writes', 'heads': {b: s['head'] for b, s in states.items()}})); return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), 'Prior publication receipt must be inspected')
    receipt = {'schema': 'ulike19122-publication-receipt-v1', 'ulike_version': VERSION, 'bundle_version': BUNDLE,
        'source_commit': source_commit, 'assets': expected, 'device_tested': False, 'operations': []}
    def checkpoint(status, **fields):
        receipt.update(status=status, **fields); receipt_path.write_bytes(json_bytes(receipt)); print(status, flush=True)
    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release = api(f'repos/{REPO}/releases', {'tag_name': TAG, 'target_commitish': source_commit,
            'name': f'ULike v{VERSION} / Hiro Morphe Patches v{BUNDLE}', 'body': notes,
            'draft': True, 'prerelease': True, 'make_latest': 'false'})
        checkpoint('draft_created', release_id=release['id'])
        for name in ASSETS:
            subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(args.dist / name)], check=True)
        verify_assets(release['id'], expected, False); checkpoint('draft_assets_verified'); check_heads(states)
        git(args.repo, 'fetch', '--no-tags', f'https://github.com/{REPO}.git', source_commit)
        raw = prepare_commit(args.repo, {**states['main'], 'branch': RAW_BRANCH},
            {'downloads/' + n: payloads[n] for n in (SINGLE, COMBINED)}, 'release: immutable verified ULike122 and bundle328')
        # Empty expected ref means create-only: even an existing ancestor is
        # rejected. No existing branch can be rewritten by this operation.
        git(args.repo, 'push', f'--force-with-lease=refs/heads/{RAW_BRANCH}:', f'https://github.com/{REPO}.git', f"{raw['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH) == raw['head'], 'Immutable download branch differs')
        urls = {n: f"https://raw.githubusercontent.com/{REPO}/{raw['head']}/downloads/{n}" for n in (SINGLE, COMBINED)}
        for n, url in urls.items():
            require(fetch(url) == payloads[n], 'Immutable download differs: ' + n)
        checkpoint('immutable_downloads_verified', download_urls=urls, raw_commit=raw['head'])
        api(f"repos/{REPO}/releases/{release['id']}", {'draft': False, 'prerelease': True, 'make_latest': 'false'}, 'PATCH')
        verify_assets(release['id'], expected, True); checkpoint('release_bytes_verified'); check_heads(states)
        updates, wanted = {}, {}
        for b, s in states.items():
            feed, log = metadata(s, urls[COMBINED], created, notes)
            updates[b] = {'patches-bundle.json': json_bytes(feed), 'CHANGELOG.md': log.encode(), POLICY_PATH: json_bytes(policies[b])}
            wanted[b] = feed, log
        active = commit_feeds_atomically(args.repo, states, updates, 'release: ULike122 post-capture scheduling and bundle328 Manager feeds')
        for b, s in active.items():
            feed, log = wanted[b]
            require(json.loads(content('patches-bundle.json', s['head'])) == feed, 'Published feed differs')
            require(content('CHANGELOG.md', s['head']).decode() == log, 'Published history differs')
            require(json.loads(content(POLICY_PATH, s['head'])) == policies[b], 'Published ULike policy differs')
            require(fetch(feed['download_url']) == payloads[COMBINED], 'Manager package differs')
            receipt['operations'].append({'branch': b, 'commit': s['head'], 'feed_verified': True, 'download_verified': True})
        check_heads(active)
        checkpoint('published_and_verified', release_url=PAGE, published=True, manager_main_dev_updated_atomically=True)
        raw_receipt = receipt_path.read_bytes()
        saved = commit_feeds_atomically(args.repo, active,
            {b: {RELEASE_ROOT + '/' + RECEIPT_NAME: raw_receipt} for b in active}, 'docs: verified ULike122 publication receipt')
        for b, s in saved.items():
            require(content(RELEASE_ROOT + '/' + RECEIPT_NAME, s['head']) == raw_receipt, 'Publication receipt differs')
        subprocess.run(['gh', 'release', 'upload', TAG, '--repo', REPO, str(receipt_path)], check=True)
        verify_assets(release['id'], {**expected, RECEIPT_NAME: {'bytes': len(raw_receipt), 'sha256': sha(raw_receipt)}}, True)
        check_heads(saved)
    except BaseException as error:
        checkpoint('failed_requires_inspection', error=type(error).__name__ + ': ' + str(error)); raise


def arguments():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('repo', 'dist', 'baseline', 'source', 'bundle-report', 'combined-application'):
        parser.add_argument('--' + name, type=Path, required=True)
    for name in ('build-report', 'manifest'):
        parser.add_argument('--' + name, type=Path)
    parser.add_argument('--host-report', action='append', default=[], metavar='NAME=PATH')
    parser.add_argument('--local-only', action='store_true')
    parser.add_argument('--preflight-only', action='store_true')
    args = parser.parse_args()
    args.repo, args.dist, args.source = args.repo.resolve(), args.dist.resolve(), args.source.resolve()
    args.build_report = args.build_report or args.source / 'evidence/build19122.json'
    args.host_reports = parse_host_specs(args.host_report)
    args.manifest = args.manifest or args.repo / RELEASE_ROOT / CONFIG
    return args


if __name__ == '__main__':
    publication(arguments())
