#!/usr/bin/env python3
"""Publish locally SDK-tested and twice CI-rebuilt Back MPPs with source/byte gates and atomic feed updates.
No real-device or third-party APK validation is implied by synthetic fixtures.
"""
from __future__ import annotations
import argparse,base64,datetime as dt,hashlib,io,json,os,re,subprocess,struct,tempfile,time,urllib.error,urllib.request,zipfile
from pathlib import Path, PurePosixPath
REPO='hiro191u3n2/hiro-morphe-patches'
VERSION,PREVIOUS,BUNDLE='1.1.7','1.0.242','1.0.243'
TAG='oneback-v1.1.7';RAW_BRANCH='release/homeback117'
SINGLE='OneBack_Exit_NoRecents_v1.1.7.mpp';COMBINED='Hiro_Morphe_Patches_v1.0.243.mpp'
BASE_NAME='Hiro_Morphe_Patches_v1.0.242.mpp'
BASE_SHA256='c6ceb2b9c00385940c3a89349fa6f978bfc2f0bf9b84de5b17dc4e91b98e21b8'
BASE_URL='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/03272431ddd282542ebbf57e1c6e2d50584425e1/downloads/Hiro_Morphe_Patches_v1.0.242.mpp'
QA_NAME='QA_HomeBack_v1.1.7.json';SOURCE_ZIP='HomeBack_v1.1.7_sources_and_QA.zip'
RECEIPT_NAME='publication_HomeBack_v1.1.7.json'
ASSETS=(SINGLE,COMBINED,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
RELEASE_ROOT='releases/homeback117';POLICY_PATH='releases/ULike_ACTIVE.json'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'
PUBLICATION=Path(__file__).resolve().parent;SOURCE_ROOT=PUBLICATION.parent
MF='META-INF/MANIFEST.MF';PATCH_PREFIX='app/hiro/oneback/patches/';RUNTIME_EXTENSION='extensions/oneback_exit.mpe'
APP_SCOPES=('TikTok','Instagram')
OTHER_APPS=('Trip.com','X','Twitter','ULike','Microsoft SwiftKey Beta','SwiftKey Beta','Berry Browser','Yahoo!乗換案内','Amazon Shopping','BrightnessClick','LINE')
MANAGER_SOURCE='https://github.com/MorpheApp/morphe-manager/blob/0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/app/src/main/java/app/morphe/manager/util/ChangelogParser.kt'
HEADING=re.compile(r'^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)',re.I)
SCOPE=re.compile(r'^\* \*\*(.+?):\*\*');EXPERIMENTAL_ONLY=re.compile(r'^Add(?:ed)?\s+experimental\s+support\s+for\b',re.I)

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




def dex_class_types(raw):
    """Independent descriptor inventory; semantic bytecode preservation is checked by HomeBackAudit."""
    require(raw.startswith(b'dex\n') and len(raw)>=112, 'Invalid DEX inventory input')
    def u32(offset):
        require(0<=offset<=len(raw)-4, 'DEX table outside file')
        return struct.unpack_from('<I',raw,offset)[0]
    strings_size,strings_offset=u32(56),u32(60)
    types_size,types_offset=u32(64),u32(68)
    classes_size,classes_offset=u32(96),u32(100)
    require(strings_offset+strings_size*4<=len(raw) and types_offset+types_size*4<=len(raw)
            and classes_offset+classes_size*32<=len(raw), 'DEX inventory table truncated')
    def string(index):
        require(index<strings_size, 'DEX string index outside table')
        offset=u32(strings_offset+index*4)
        for _ in range(5):
            require(offset<len(raw), 'DEX string length truncated')
            value=raw[offset];offset+=1
            if value<128:break
        else:raise RuntimeError('DEX string length malformed')
        end=raw.find(b'\x00',offset);require(end>=0, 'DEX string unterminated')
        return raw[offset:end].decode('utf-8')
    types=[string(u32(types_offset+i*4)) for i in range(types_size)]
    result=[]
    for i in range(classes_size):
        index=u32(classes_offset+i*32);require(index<types_size, 'DEX class type outside table')
        result.append(types[index])
    require(len(set(result))==len(result), 'Duplicate DEX class definition')
    return set(result)


def validate_local(args):
    cfg=json.loads(args.config.read_text())
    require(cfg['schema']=='homeback117-publication-v1' and cfg['patch_version']==VERSION and cfg['bundle_version']==BUNDLE,'Publication identity mismatch')
    require(sha(args.base.read_bytes())==BASE_SHA256 and cfg['baseline_sha256']==BASE_SHA256 and cfg['baseline_url']==BASE_URL,'Unreviewed baseline')
    payloads={name:(args.dist/name).read_bytes() for name in ASSETS}
    require(set(cfg['expected_mpp'])=={SINGLE,COMBINED},'MPP inventory mismatch')
    for name,row in cfg['expected_mpp'].items():
        require(len(payloads[name])==row['bytes'] and sha(payloads[name])==row['sha256'],'Rebuilt MPP differs from independent fresh SDK build: '+name)
    require(sha(payloads[QA_NAME])==cfg['expected_qa_sha256'],'Rebuilt QA differs from independent fresh SDK build')
    qa=json.loads(payloads[QA_NAME])
    require(qa['schema']=='homeback117-qa-v1' and qa['physical_android_tested'] is False and qa['third_party_apk_tested'] is False,'Validation limitations missing')
    require(qa['host']==cfg['local_expected_host'] and 'PASS selftest:' in qa['dex_structure'],'Runtime/structure tests incomplete')
    host_counts=qa['host_counts']
    require(host_counts['cases']>=259 and host_counts['assertions']>=1260,'Existing host coverage lost')
    evidence={str(p.relative_to(SOURCE_ROOT)):{'sha256':sha(p.read_bytes()),'report':json.loads(p.read_text())} for p in sorted((SOURCE_ROOT/'evidence').rglob('*.json')) if p.is_file()}
    require(evidence and evidence==qa['provided_apk_static_audit_evidence'] and qa['provided_apk_inputs_already_patched'] is True,'Provided patched APK audit evidence changed/missing')
    packages={'com.twitter.android','com.instagram.android','com.zhiliaoapp.musically','com.ss.android.ugc.trill','ctrip.english'}
    wanted={(bundle,pkg) for bundle in (SINGLE,COMBINED) for pkg in packages}
    require(len(qa['synthetic'])==10 and {(r['bundle'],r['package']) for r in qa['synthetic'] if r['result']=='PASS'}==wanted,'Synthetic apply coverage incomplete')
    require(set(qa['discovery'])=={SINGLE,COMBINED} and all(set(v)==packages and all(ok is True for ok in v.values()) for v in qa['discovery'].values()),'Discovery coverage incomplete')
    require(qa['artifacts']==cfg['expected_mpp'],'QA artifact identities differ')
    old,new,single=map(archive,(args.base.read_bytes(),payloads[COMBINED],payloads[SINGLE]))
    summaries=[row['report'] for row in evidence.values() if row['report'].get('schema')=='homeback117-provided-apk-audit-v1']
    require(len(summaries)==1, 'Exactly one explicit provided-APK audit summary is required')
    audit=summaries[0]
    require(audit.get('inputs_already_patched') is True and audit.get('original_apk_apply_tested') is False
            and audit.get('physical_android_tested') is False and audit.get('input_apks_modified') is False
            and audit.get('result')=='PASS', 'APK static-audit limitations/result invalid')
    require(audit.get('replacement_runtime_sha256')==sha(single[RUNTIME_EXTENSION]),
            'Provided-APK ABI audit was not run against the distributed runtime')
    inputs=audit.get('inputs',[])
    require(len(inputs)==3 and len({row.get('sha256') for row in inputs})==3
            and 'com.instagram.android' in {row.get('package') for row in inputs}
            and bool({'com.ss.android.ugc.trill','com.zhiliaoapp.musically'} & {row.get('package') for row in inputs}),
            'Provided APK input coverage incomplete')
    old_runtime_count=len(dex_class_types(old[RUNTIME_EXTENSION]))
    for row in inputs:
        name='evidence/'+str(checked_path(row['report']))
        require(name in evidence, 'Referenced APK audit report missing')
        detail=evidence[name]['report']
        require(detail.get('schema')=='provided-patched-apk-hook-audit-v1' and detail.get('result')=='PASS'
                and detail.get('input_apk')==row['filename'] and detail.get('input_sha256')==row['sha256']
                and detail.get('package')==row['package'] and detail.get('version')==row['version']
                and detail.get('physical_android_tested') is False and detail.get('clean_original_apk_tested') is False
                and detail.get('input_apk_modified') is False, 'Provided APK report identity or limitations differ')
        require(detail.get('matches_known_v116_runtime') is True and detail.get('old_extension_sha256')==sha(old[RUNTIME_EXTENSION])
                and detail.get('existing_runtime_classes')==old_runtime_count and detail.get('known_v116_runtime_classes')==old_runtime_count
                and detail.get('replacement_runtime_sha256')==sha(single[RUNTIME_EXTENSION])
                and detail.get('replacement_abi_valid_for_all_existing_hooks') is True
                and isinstance(detail.get('old_public_methods_preserved'),int) and detail['old_public_methods_preserved']>0
                and not detail.get('replacement_abi_errors') and detail.get('duplicate_class_definitions')==[],
                'Provided APK identity, unique classes or existing-hook ABI preservation not established')
    audit_sources={str(p.relative_to(SOURCE_ROOT)):sha(p.read_bytes()) for p in sorted((SOURCE_ROOT/'evidence').rglob('*'))
                   if p.is_file() and p.suffix in ('.java','.py')}
    require(audit_sources and audit_sources==cfg['local_expected_audit_sources'], 'APK audit source identities changed since local freeze')
    for data,version in ((old,PREVIOUS),(new,BUNDLE),(single,VERSION)):
        require(headers(data[MF])['Version']==version and data['classes.dex'].startswith(b'dex\n'),'MPP identity/DEX invalid')
    old_types,new_types,single_types=map(dex_class_types,(old['classes.dex'],new['classes.dex'],single['classes.dex']))
    own_type=lambda name:name.startswith('L'+PATCH_PREFIX)
    old_own={name for name in old_types if own_type(name)}
    retained_types=old_types-old_own
    require(old_own and retained_types and {name for name in new_types if not own_type(name)}==retained_types,
            'Unrelated loader descriptor inventory changed')
    require(single_types and all(own_type(name) for name in single_types)
            and {name for name in new_types if own_type(name)}==single_types, 'Standalone/integrated loader inventory differs')
    proof=re.search(r'PASS merge: preserved=(\d+), replaced=(\d+), replacement=(\d+), assertions=(\d+)',qa['loader_preservation'])
    require(proof is not None and tuple(map(int,proof.groups()[:3]))==(len(retained_types),len(old_own),len(single_types))
            and int(proof[4])>len(retained_types), 'Loader semantic preservation proof and actual DEX counts differ')
    own=lambda name:name.startswith(PATCH_PREFIX) or name==RUNTIME_EXTENSION
    retained={name:data for name,data in old.items() if name not in (MF,'classes.dex') and not own(name)}
    require(retained and all(new.get(name)==data for name,data in retained.items()),'Unrelated resource changed/removed')
    require(all(name in old or own(name) for name in new),'Unrelated resource added')
    require(all(new.get(name)==data for name,data in single.items() if own(name)),'Standalone/integrated OneBack differs')
    require(all(own(name) or name in (MF,'classes.dex') for name in single),'Unexpected standalone entry')
    require(len(retained)==qa['preserved_unrelated_entries'],'Resource inventory mismatch')
    src={str(p.relative_to(SOURCE_ROOT/'src')):sha(p.read_bytes()) for p in sorted((SOURCE_ROOT/'src').rglob('*')) if p.is_file() and p.suffix in ('.java','.py')}
    require(src==qa['sources'],'Authored sources differ from tested sources')
    entries=archive(payloads[SOURCE_ZIP])
    for name,data in entries.items():
        require(PurePosixPath(name).suffix in ('.java','.py','.json','.txt','.md','.yml'),'Unexpected binary in source archive')
        data.decode('utf-8');require(b'\x00' not in data,'Binary source entry')
    for p in list((SOURCE_ROOT/'src').rglob('*'))+list((SOURCE_ROOT/'publication').rglob('*'))+list((SOURCE_ROOT/'evidence').rglob('*'))+[SOURCE_ROOT/'README.md']:
        if p.is_file() and p.suffix in ('.java','.py','.json','.txt','.md','.yml') and '__pycache__' not in p.parts:
            require(entries.get(str(p.relative_to(SOURCE_ROOT)))==p.read_bytes(),'Source archive missing/changed file: '+str(p))
    require(entries.get('QA_HomeBack_v1.1.7.json')==payloads[QA_NAME],'Archived QA mismatch')
    notes=payloads['RELEASE_NOTES.txt'].decode();require(notes==(PUBLICATION/'RELEASE_NOTES.txt').read_text(),'Notes changed')
    require('未確認' in notes and not any(HEADING.match(line) for line in notes.splitlines()),'Notes missing limitations or contains conflicting heading')
    checks={}
    for line in payloads['SHA256SUMS.txt'].decode().splitlines():
        digest,name=line.split('  ',1);require(name in payloads and name not in checks and sha(payloads[name])==digest,'Asset checksum mismatch');checks[name]=digest
    require(set(checks)==set(ASSETS)-{'SHA256SUMS.txt'},'Asset checksum inventory mismatch')
    report={'baseline_bundle_version':PREVIOUS,'baseline_bundle_sha256':BASE_SHA256,'original_apk_apply_tested':False,'android_device_tested':False,'unrelated_resource_entries_byte_identical':len(retained),'unrelated_loader_classes_unchanged':len(retained_types),'trip_com_behavior_unchanged':True,'trip_com_home_only_proactive_exit':True,'trip_com_native_terminal_back_cleanup':True,'instagram_reels_exit':True,'instagram_comments_collapse_first':True,'instagram_same_gesture_exit_protected':True,'tiktok_native_child_back_retained':True,'tiktok_messages_profile_exit':False,'tiktok_messages_profile_home_navigation':True,'tiktok_all_nonhome_protected_navigation':True,'tiktok_foreground_home_positive_evidence':True,'tiktok_unknown_navigation_failure_does_not_exit':True,'tiktok_predictive_start_capture':True,'trip_bounded_async_back_and_stop_cleanup':True,'two_fresh_ci_builds_required':True,'two_fresh_ci_builds_mpp_qa_byte_identical':cfg.get('ci_build_pair_verified') is True,'local_sdk_build':True,'local_host_tested':True,'provided_apk_inputs_already_patched':True,'provided_apk_static_audit_evidence':evidence,'validation_coverage':{'host_cases':host_counts['cases'],'host_assertions':host_counts['assertions'],'synthetic_apk_apply_cases':10},'qa_sha256':sha(payloads[QA_NAME])}
    assets={name:{'bytes':len(raw),'sha256':sha(raw)} for name,raw in payloads.items()}
    return cfg,payloads,notes,assets,report


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
                "User-Agent": "Hiro-HomeBack117-Publication",
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
    checked_path(path)
    require(re.fullmatch(r"[0-9a-f]{40}", ref) is not None, "Read requires immutable commit")
    row = api(f"repos/{REPO}/contents/{path}?ref={ref}")
    require(row.get("type") == "file" and re.fullmatch(r"[0-9a-f]{40}", row.get("sha", "")) is not None, "Not a Git file")
    if row.get("encoding") == "base64":
        raw = base64.b64decode(row["content"])
    elif row.get("encoding") == "none":
        # GitHub Contents omits inline bytes above its size limit.
        raw = fetch(f"https://raw.githubusercontent.com/{REPO}/{ref}/{path}")
    else:
        raise RuntimeError("Unsupported repository file encoding")
    require(len(raw) == row["size"], "Immutable repository file size mismatch")
    object_id = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\x00" + raw).hexdigest()
    require(object_id == row["sha"], "Immutable repository Git blob mismatch")
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
    require(result.returncode == 0, "Git command failed: " + " ".join(map(str, args)) + "\n"
            + result.stderr.decode(errors="replace"))
    return result.stdout.decode().strip()



def prepare_commit(repo, state, files, message):
    env = {**os.environ, "GIT_AUTHOR_NAME": "github-actions[bot]", "GIT_COMMITTER_NAME": "github-actions[bot]",
           "GIT_AUTHOR_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com",
           "GIT_COMMITTER_EMAIL": "41898282+github-actions[bot]@users.noreply.github.com"}
    with tempfile.TemporaryDirectory(prefix="homeback117-index-") as temporary:
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
            "Active bundle is no longer the reviewed baseline242")
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
    # Sub-scopes of the changed app legitimately match those app names.
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
    require(cfg.get('ci_build_pair_verified') is True and os.environ.get('GITHUB_ACTIONS')=='true'
            and cfg.get('ci_run_id')==os.environ.get('GITHUB_RUN_ID')
            and cfg.get('ci_source_commit')==os.environ.get('GITHUB_SHA'),
            'Publication requires the current GitHub workflow two-build verification')
    states = {branch: snapshot(branch) for branch in ("main", "dev")}
    check_heads(states)
    require(states["main"]["manifest"] == states["dev"]["manifest"], "main/dev baseline feeds differ")
    if os.environ.get("GITHUB_SHA"):
        require(states["main"]["head"] == os.environ["GITHUB_SHA"], "main moved after this workflow was triggered")
        require(git(args.repo, "rev-parse", "HEAD") == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    require(sha(fetch(BASE_URL)) == BASE_SHA256, "Public baseline242 bytes differ")
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
    receipt = {"schema": "homeback117-publication-receipt-v1", "patch_version": VERSION,
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
            "release: exact HomeBack117 and bundle243 immutable MPP downloads")
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
                for path in [p for folder in ('src','publication','evidence') for p in (SOURCE_ROOT/folder).rglob('*')]+[SOURCE_ROOT/'README.md']:
                    if path.is_file() and path.suffix in (".java", ".py", ".json", ".txt", ".md", ".yml") and "__pycache__" not in path.parts:
                        updates[branch][RELEASE_ROOT + "/" + path.relative_to(SOURCE_ROOT).as_posix()] = path.read_bytes()
                for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                    updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds(args.repo, states, updates, "release: OneBack exit and recents removal and bundle243 Manager update")
        for branch, state in active.items():
            feed, changelog = new_metadata[branch]
            require(json.loads(content("patches-bundle.json", state["head"])) == feed, "Remote feed mismatch")
            require(content("CHANGELOG.md", state["head"]).decode() == changelog, "Remote changelog mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "Existing ULike active policy changed")
            require(fetch(feed["download_url"]) == payloads[COMBINED], "Manager MPP bytes mismatch")
            # Public branch URLs may briefly lag a successful Git commit. Re-read only;
            # never rewrite the feed merely to make a propagation check pass.
            for attempt in range(12):
                nonce=str(time.time_ns())
                prefix=f"https://raw.githubusercontent.com/{REPO}/{branch}"
                public_feed=json.loads(fetch(prefix+"/patches-bundle.json?homeback117="+nonce))
                public_log=fetch(prefix+"/CHANGELOG.md?homeback117="+nonce).decode()
                if public_feed==feed and public_log==changelog:break
                check_heads(active)
                if attempt<11:time.sleep(3)
            else:raise RuntimeError("Public branch metadata did not propagate: "+branch)
            receipt["operations"].append({"branch": branch, "commit": state["head"], "feed_verified": True,
                "changelog_verified": True, "ulike_policy_unchanged": True, "manager_download_verified": True})
        check_heads(active)
        verify_assets(release["id"], assets, True)
        checkpoint("published_and_verified", published=True, release_url=PAGE,
                   manager_main_dev_updated_atomically=True, manager_app_scopes_verified=list(APP_SCOPES),
                   other_apps_false_updates=False, all_unrelated_patch_resources_preserved=True, trip_com_behavior_unchanged=True, trip_com_home_only_proactive_exit=True, trip_com_native_terminal_back_cleanup=True, instagram_reels_exit=True, instagram_comments_collapse_first=True, instagram_same_gesture_exit_protected=True, tiktok_native_child_back_retained=True, tiktok_messages_profile_exit=False, tiktok_messages_profile_home_navigation=True, trip_bounded_async_back_and_stop_cleanup=True,
                   fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified HomeBack117 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0],
                    "Feed changed while saving receipt")
        verify_assets(release["id"], assets, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, TikTok / Instagram scopes, all unrelated patches and receipt", flush=True)
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



if __name__ == '__main__': main()
