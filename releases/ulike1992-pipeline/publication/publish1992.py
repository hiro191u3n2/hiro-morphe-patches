#!/usr/bin/env python3
"""Publish only a fresh validated .92 build; retain atomic main/dev feed transactions.

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
VERSION,BUNDLE,PREVIOUS_APP,PREVIOUS="1.9.92","1.0.225","1.9.91","1.0.224"
TAG="ulike-v1.9.92"
PAGE=f"https://github.com/{REPO}/releases/tag/{TAG}"
RAW_BRANCH="ulike1992-pipeline-downloads"
RELEASE_ROOT="releases/ulike1992-pipeline"
POLICY_PATH="releases/ULike_ACTIVE.json"
SINGLE="ULike_HQ_Texture_Online_v1.9.92.mpp"
COMBINED="Hiro_Morphe_Patches_v1.0.225.mpp"
QA_NAME="QA_ULike_v1.9.92.json"
SOURCE_ZIP="ULike_v1.9.92_sources_and_QA.zip"
RECEIPT_NAME="publication_ULike_v1.9.92.json"
ASSETS=(SINGLE,COMBINED,QA_NAME,SOURCE_ZIP,"RELEASE_NOTES.txt","SHA256SUMS.txt")
BASE_SHA256="fe2ed0dc25502ae3be39adae02e3cb72053d43636a72c245d830038afd100c24"
SINGLE_BASE_SHA256="5b9327b05498c1e688aa5d584f601b617ccee2fda091b45d97de221d43053fef"
INVENTORY=re.compile(r"(?m)^(ULike：)v(1\.9\.91)(（5\.6\.2／740）)\r?$")
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
                "User-Agent": "Hiro-ULike1992-Publication",
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
    with tempfile.TemporaryDirectory(prefix="ulike1992-index-") as temp:
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



def local(args):
    validation=json.loads(args.validation.read_text())
    require(validation.get("schema")=="ulike1992-release-validation-v1" and validation.get("status")=="passed","Fresh release validation required")
    require(validation.get("host_tests_passed") is True and validation.get("original_apk_apply_tested") is True,"Fresh host and exact original-APKS application required")
    for key in ("device_tested","device_speed_measured","device_quality_verified"):
        require(validation.get(key) is False,"Untested Android claim: "+key)
    payloads={name:(args.dist/name).read_bytes() for name in ASSETS}
    expected={name:{"sha256":sha(raw),"bytes":len(raw)} for name,raw in payloads.items()}
    require(validation.get("release_artifacts")==expected,"Validated release assets changed")
    qa=json.loads(payloads[QA_NAME]);require(qa.get("ulike_version")==VERSION and qa.get("bundle_version")==BUNDLE,"QA version mismatch")
    require(qa.get("artifacts")=={name:expected[name] for name in (SINGLE,COMBINED)},"QA MPP identity mismatch")
    with zipfile.ZipFile(io.BytesIO(payloads[SOURCE_ZIP])) as z:
        require(z.testzip() is None,"Source archive CRC failed")
        for name in z.namelist():
            checked_path(name)
            require(not name.endswith((".apk",".apks")) and not any(part in ("tools","input","work") for part in PurePosixPath(name).parts),"Private/tool artifact in source archive")
    notes=payloads['RELEASE_NOTES.txt'].decode()
    require('実機' in notes and '未' in notes,"Device verification limitation missing")
    return validation,qa,payloads,expected,notes

def metadata(state,url,created,notes):
    old=state['manifest'];require(old['version']==PREVIOUS,"Active bundle advanced")
    require(len(list(INVENTORY.finditer(old['description'])))==1,"Exact ULike inventory missing")
    description=INVENTORY.sub(lambda m:m[1]+'v'+VERSION+m[3],old['description'],count=1)
    feed={**old,'version':BUNDLE,'created_at':created,'download_url':url,'page_url':PAGE,'signature_download_url':'','description':notes.strip()+'\n\n'+description}
    changelog=f"# {BUNDLE} ({created[:10]})\n\n* **ULike:** v{VERSION}：仕上げ経路・GPU補正方針・失敗分類・撮影計測を更新。2,000ms以下は実機未測定。\n\n"+notes.strip()+'\n\n'+state['log']
    require(changelog.endswith(state['log']) and feed['description'].endswith(description),'Previous history lost')
    require(has_changes_for(changelog,PREVIOUS,['ULike']) and not has_changes_for(changelog,BUNDLE,['ULike']),'Manager update eligibility wrong')
    scopes={scope for entry in parsed_entries(state['log']) for scope,_ in entry['bullets']}
    require(not any(has_changes_for(changelog,PREVIOUS,[app]) for app in scopes if app.casefold()!='ulike' and not app.casefold().startswith('ulike - ')),'Other app falsely marked updated')
    require({scope for scope,_ in parsed_entries(changelog)[0]['bullets']}=={'ULike'},'Release scope widened')
    return feed,changelog

def new_policy(raw,qa,expected):
    old=json.loads(raw)
    require(old.get('ulike_version')==PREVIOUS_APP and old.get('source_sha256')==SINGLE_BASE_SHA256,'Active ULike baseline advanced')
    require(old.get('approved_lineage_version')=='1.8.8' and '1.9.17' in old.get('withdrawn_versions',[]),'Approved lineage differs')
    # Preserve configuration/history but do not promote historical validation booleans.
    historical={key:value for key,value in old.items() if key.endswith(('_verified','_passed','_regressions_passed','_byte_identical','_bytecode_identical')) or key in ('audit_validation','gx30_gx36_validation','strong_gpu_host_evidence','gpu_native_build')}
    policy={key:value for key,value in old.items() if key not in historical}
    policy.update(ulike_version=VERSION,bundle_version=BUNDLE,source_filename=SINGLE,source_sha256=expected[SINGLE]['sha256'],source_release=TAG,active_release=TAG,
        base_ulike_version=PREVIOUS_APP,base_bundle_version=PREVIOUS,base_source_sha256=SINGLE_BASE_SHA256,
        inherited_release_evidence1992={'ulike_version':PREVIOUS_APP,'source_sha256':SINGLE_BASE_SHA256,'historical_only':True,'evidence':historical},
        release_status='pipeline92_host_original_apks_verified_device_unverified',current_fix_scope=qa.get('selected_candidates',[]),
        selected_candidates=qa.get('selected_candidates',[]),current_validation=qa,
        gpu_native_library_sha256=qa['gpu_native']['sha256'],gpu_native_library_bytes=qa['gpu_native']['bytes'],
        original_apk_apply_tested=True,android_device_tested=False,device_quality_verified=False,device_save_speed_measured=False,
        gpu_execution_on_physical_android=False,samsung_gpu_failure_root_cause_confirmed=False,
        capture_fusion_enabled=False,single_image_only=True,
        reason='Complete-route finishing validation, in-session GPU policy, typed failure causes, capture/save metrics and runtime release-version consistency. See exact current QA for executed scope and unresolved physical-device validation.',
        future_ulike_base='Use .92/.225 source and exact artifact hashes. Preserve complete quality checks, actual GPU-output telemetry, bounded memory, camera/save contracts and other apps. Do not infer device speed or numerical equivalence from host-only or cached results.')
    return policy

def publication(args):
    validation,qa,payloads,expected,notes=local(args)
    if args.local_only:print(json.dumps({'status':'local_artifacts_verified','assets':expected},indent=2));return
    states={branch:snapshot(branch) for branch in ('main','dev')};check_heads(states)
    require(states['main']['manifest']==states['dev']['manifest'],'Feed branches differ')
    require(all(s['manifest']['version']==PREVIOUS for s in states.values()),'Bundle has advanced')
    require(sha(fetch(states['main']['manifest']['download_url']))==BASE_SHA256,'Active bundle bytes differ')
    created=utc_created(states)
    policies={b:new_policy(s['ulike_policy'],qa,expected) for b,s in states.items()}
    for s in states.values():metadata(s,'https://example.invalid/preflight-only',created,notes)
    for path in (f'releases/tags/{TAG}',f'git/ref/tags/{TAG}',f'git/ref/heads/{RAW_BRANCH}'):
        require(api(f'repos/{REPO}/'+path,absent=True) is None,'Existing release/tag/distribution branch; inspect before retry')
    if args.preflight_only:print(json.dumps({'status':'preflight_passed_no_remote_writes','heads':{b:s['head'] for b,s in states.items()}},indent=2));return
    receipt_path=args.dist/RECEIPT_NAME;require(not receipt_path.exists(),'Prior receipt must be inspected')
    receipt={'schema':'ulike1992-publication-receipt-v1','ulike_version':VERSION,'bundle_version':BUNDLE,'assets':expected,'device_tested':False,'operations':[]}
    def checkpoint(status,**fields):
        receipt.update(status=status,**fields);receipt_path.write_bytes(json_bytes(receipt));print(status,flush=True)
    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release=api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':states['main']['head'],'name':f'ULike v{VERSION} / Hiro Morphe v{BUNDLE}','body':notes,'draft':True,'prerelease':True,'make_latest':'false'})
        checkpoint('draft_created',release_id=release['id'])
        for name in ASSETS:subprocess.run(['gh','release','upload',TAG,'--repo',REPO,str(args.dist/name)],check=True)
        verify_assets(release['id'],expected,False);checkpoint('draft_assets_verified');check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}",{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
        verify_assets(release['id'],expected,True);checkpoint('release_bytes_verified');check_heads(states)
        git(args.repo,'fetch','--no-tags',f'https://github.com/{REPO}.git',states['main']['head'])
        raw=prepare_commit(args.repo,{**states['main'],'branch':RAW_BRANCH},{'downloads/'+n:payloads[n] for n in (SINGLE,COMBINED)},'release: immutable ULike1992 and bundle225 packages')
        git(args.repo,'push',f'--force-with-lease=refs/heads/{RAW_BRANCH}:',f'https://github.com/{REPO}.git',f"{raw['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH)==raw['head'],'Distribution branch changed')
        urls={n:f"https://raw.githubusercontent.com/{REPO}/{raw['head']}/downloads/{n}" for n in (SINGLE,COMBINED)}
        for n,url in urls.items():require(fetch(url)==payloads[n],'Immutable download differs '+n)
        checkpoint('immutable_downloads_verified',download_urls=urls,raw_commit=raw['head'])
        updates={};metadata_out={}
        for b,s in states.items():
            feed,log=metadata(s,urls[COMBINED],created,notes);metadata_out[b]=(feed,log)
            updates[b]={'patches-bundle.json':json_bytes(feed),'CHANGELOG.md':log.encode(),POLICY_PATH:json_bytes(policies[b])}
            if b=='main':
                for name in (QA_NAME,'RELEASE_NOTES.txt','SHA256SUMS.txt'):updates[b][RELEASE_ROOT+'/'+name]=payloads[name]
        active=commit_feeds_atomically(args.repo,states,updates,'release: ULike1992 capture pipeline and bundle225 Manager feeds')
        for b,s in active.items():
            feed,log=metadata_out[b]
            require(json.loads(content('patches-bundle.json',s['head']))==feed,'Feed bytes differ')
            require(content('CHANGELOG.md',s['head']).decode()==log,'Changelog differs')
            require(json.loads(content(POLICY_PATH,s['head']))==policies[b],'Policy differs')
            require(fetch(feed['download_url'])==payloads[COMBINED],'Manager package differs')
            receipt['operations'].append({'branch':b,'commit':s['head'],'feed_verified':True,'download_verified':True})
        check_heads(active);verify_assets(release['id'],expected,True)
        checkpoint('published_and_verified',release_url=PAGE,published=True,manager_main_dev_updated_atomically=True)
        raw_receipt=receipt_path.read_bytes()
        saved=commit_feeds_atomically(args.repo,active,{b:{RELEASE_ROOT+'/'+RECEIPT_NAME:raw_receipt} for b in active},'docs: verified ULike1992 publication receipt')
        for b,s in saved.items():require(content(RELEASE_ROOT+'/'+RECEIPT_NAME,s['head'])==raw_receipt,'Receipt differs')
        subprocess.run(['gh','release','upload',TAG,'--repo',REPO,str(receipt_path)],check=True)
        verify_assets(release['id'],{**expected,RECEIPT_NAME:{'sha256':sha(raw_receipt),'bytes':len(raw_receipt)}},True);check_heads(saved)
    except BaseException as error:
        checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error));raise

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('dist','repo','validation'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--local-only',action='store_true');p.add_argument('--preflight-only',action='store_true');a=p.parse_args();publication(a)
