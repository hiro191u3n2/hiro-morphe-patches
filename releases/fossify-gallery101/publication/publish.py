#!/usr/bin/env python3
"""Publish the verified portable Fossify Gallery patch and preserve both Manager feeds."""
from __future__ import annotations
import argparse,base64,datetime as dt,hashlib,io,json,os,re,subprocess,struct,sys,tempfile,time,urllib.error,urllib.request,zipfile
from pathlib import Path, PurePosixPath
REPO='hiro191u3n2/hiro-morphe-patches'
VERSION,PREVIOUS,BUNDLE='1.0.1','1.0.257','1.0.258'
TAG='fossify-gallery-v1.0.1';RAW_BRANCH='release/fossify-gallery101'
SINGLE='Fossify_Gallery_NoRecents_v1.0.1.mpp';COMBINED='Hiro_Morphe_Patches_v1.0.258.mpp'
BASE_SHA256='69143101c1700a144e5a447948f4e7f6a68dab0653fa77b7fae9688da4bcadd7'
BASE_URL='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/a150fe5e5f937534e64e32fab6756db93101e76e/downloads/Hiro_Morphe_Patches_v1.0.257.mpp'
QA_NAME='QA_Fossify_Gallery_v1.0.1.json';SOURCE_ZIP='Fossify_Gallery_v1.0.1_sources_and_QA.zip'
RECEIPT_NAME='publication_Fossify_Gallery_v1.0.1.json'
ASSETS=(SINGLE,COMBINED,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
RELEASE_ROOT='releases/fossify-gallery101';POLICY_PATH='releases/ULike_ACTIVE.json'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'
PUBLICATION=Path(__file__).resolve().parent;SOURCE_ROOT=PUBLICATION.parent
MF='META-INF/MANIFEST.MF';PATCH_PREFIX='app/hiro/fossifygallery/patches/';RUNTIME_EXTENSION='extensions/fossify_gallery_exit.mpe';REFERENCE_ASSET='fossify-gallery/original-back.dex'
TOOLS={'morphe2.jar':'67479a00dba2de387e5115b73960267325e544cedf44a709416cdfb70c82e7f2','morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
APP_SCOPES=('Fossify Gallery','Gallery')
MANAGER_PARSER_SOURCES=('https://github.com/MorpheApp/morphe-manager/blob/0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/app/src/main/java/app/morphe/manager/util/ChangelogParser.kt','https://github.com/MorpheApp/morphe-manager/blob/da8545c92c94b83e23adf7b573b46b282fa2b062/app/src/main/java/app/morphe/manager/util/ChangelogParser.kt')
HEADING=re.compile(r'^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)',re.I)
SCOPE=re.compile(r'^\* \*\*(.+?):\*\*');EXPERIMENTAL_ONLY=re.compile(r'^Add(?:ed)?\s+experimental\s+support\s+for\b',re.I)
SOURCE_SUFFIXES={'.java','.py','.json','.txt','.md','.yml'}


def reviewed_files():
    files={}
    for folder in ('src','publication'):
        for path in sorted((SOURCE_ROOT/folder).rglob('*')):
            if path.is_file() and path.suffix in SOURCE_SUFFIXES and '__pycache__' not in path.parts:
                name=path.relative_to(SOURCE_ROOT).as_posix()
                if name not in ('publication/manifest.json','publication/manifest-template.json'):
                    files[name]=path.read_bytes()
    files['README.md']=(SOURCE_ROOT/'README.md').read_bytes()
    return files


def validate_qa(qa, cfg):
    require(qa.get('schema')=='fossify-gallery101-qa-v1','Wrong QA schema')
    require((qa['patch_version'],qa['bundle_version'],qa['baseline_version'])==(VERSION,BUNDLE,PREVIOUS),'QA version differs')
    require(qa['baseline_sha256']==BASE_SHA256,'QA baseline differs')
    require(qa['production_android_sdk_compile'] is True and qa['physical_android_tested'] is False,'Compilation/device identity differs')
    require(qa['toolchain_sha256']==TOOLS,'Toolchain differs')
    require('PASS' in qa['host_tests'] and 'PASS' in qa['loader_preservation'],'Host/loader preservation audit incomplete')
    require(qa['official_apk_sha256']==cfg['official_apk_sha256'],'Official input APK differs')
    require(len(qa['apk_application'])==2 and {(r['bundle'],r['package']) for r in qa['apk_application'] if r['result']=='PASS'}=={(name,'org.fossify.gallery') for name in (SINGLE,COMBINED)},'Both official APK applications required')
    require(all('PASS' in row['audit_summary'] for row in qa['apk_application']),'Official APK semantic audit incomplete')
    compatible=qa.get('compatibility_patcher2_apply',[])
    require(len(compatible)==2 and {(row['bundle'],row['package']) for row in compatible if row['result']=='PASS'}=={(name,'org.fossify.gallery') for name in (SINGLE,COMBINED)},'Both Patcher2 official APK applications required')
    require(all('PASS' in row['audit_summary'] and row.get('patcher_version')=='2.0.0-dev.12' and row.get('desktop_version')=='1.19.0-dev.4' for row in compatible),'Patcher2 application audit or version identity incomplete')
    require(qa.get('patcher2_writer_equivalent_to_user_dev13') is True,'Reviewed Patcher2 dev13 writer-equivalence assertion absent')
    require(set(qa.get('compatibility_tests',{}))=={'stable','patcher2'} and all(result.startswith('PASS ') for result in qa['compatibility_tests'].values()),'Both fixed-code compatibility test suites required')
    require(re.fullmatch(r'[0-9a-f]{64}',qa.get('original_back_reference_sha256','')) is not None,'Original callback reference digest absent')
    require(qa.get('single_bundle_apk_entry_contents_equal') is True,'Standalone/combined APK entry comparison incomplete')
    require(qa.get('official_apk_non_dex_resources_unchanged') is True,'Official APK manifest/resources/assets/native preservation incomplete')
    counts={row.get('preserved_original_resources') for row in qa['apk_application']+compatible}
    require(len(counts)==1 and all(isinstance(count,int) and count>0 for count in counts),'Official APK resource preservation count missing or inconsistent')
    require(set(qa['artifacts'])=={SINGLE,COMBINED},'MPP inventory differs')
    source=SOURCE_ROOT/'src'
    require(qa['sources']=={p.relative_to(source).as_posix():sha(p.read_bytes()) for p in sorted(source.rglob('*')) if p.is_file() and p.suffix in ('.java','.py')},'Tested sources differ')
    require(re.fullmatch(r'[0-9a-f]{64}',qa['runtime_sha256']) is not None,'Runtime digest absent')
    require(qa.get('gallery_runtime_unchanged') is True,'Original Gallery exit behavior/runtime preservation incomplete')


def validate_local(args):
    cfg=json.loads(args.config.read_text())
    require(cfg.get('schema')=='fossify-gallery101-publication-v1' and cfg.get('freeze_state')=='frozen','Local build not frozen')
    require((cfg['patch_version'],cfg['bundle_version'],cfg['baseline_version'])==(VERSION,BUNDLE,PREVIOUS),'Publication version differs')
    require(cfg['baseline_url']==BASE_URL and cfg['baseline_sha256']==BASE_SHA256 and sha(args.base.read_bytes())==BASE_SHA256,'Baseline changed')
    require(cfg['source_sha256']=={name:sha(raw) for name,raw in reviewed_files().items()},'Reviewed source/publication files changed after freeze')
    payloads={name:(args.dist/name).read_bytes() for name in ASSETS}
    require(cfg['expected_mpp']=={name:{'bytes':len(payloads[name]),'sha256':sha(payloads[name])} for name in (SINGLE,COMBINED)},'MPP differs from local freeze')
    require(sha(payloads[QA_NAME])==cfg['expected_qa_sha256'],'QA differs from local freeze')
    qa=json.loads(payloads[QA_NAME]);validate_qa(qa,cfg)
    require(qa['artifacts']==cfg['expected_mpp'],'QA artifact identity differs')
    old,new,single=map(archive,(args.base.read_bytes(),payloads[COMBINED],payloads[SINGLE]))
    for data,version in ((old,PREVIOUS),(new,BUNDLE),(single,VERSION)):
        require(headers(data[MF])['Version']==version and data['classes.dex'].startswith(b'dex\n'),'MPP identity invalid')
    old_types,new_types,single_types=map(dex_class_types,(old['classes.dex'],new['classes.dex'],single['classes.dex']))
    old_own={t for t in old_types if t.startswith('L'+PATCH_PREFIX)}
    retained_types=old_types-old_own
    require(old_own and retained_types,'Expected baseline Gallery and unrelated loaders absent')
    require(new_types==retained_types|single_types and retained_types.isdisjoint(single_types),'Replacement loader inventory differs')
    require(single_types and all(t.startswith('L'+PATCH_PREFIX) for t in single_types),'Unexpected standalone loader')
    own=lambda name:name.startswith(PATCH_PREFIX) or name in (RUNTIME_EXTENSION,REFERENCE_ASSET)
    retained={name:raw for name,raw in old.items() if name not in (MF,'classes.dex',REFERENCE_ASSET) and not name.startswith(PATCH_PREFIX)}
    require(all(new.get(name)==raw for name,raw in retained.items()),'Unrelated old bundle entry changed or removed')
    require(all(name in old or own(name) for name in new),'Unexpected new entry')
    require(all(own(name) or name in (MF,'classes.dex') for name in single),'Unexpected standalone entry')
    require({name:raw for name,raw in new.items() if own(name)}=={name:raw for name,raw in single.items() if own(name)},'Standalone/integrated Gallery resource inventories differ')
    require(len(retained)==qa['preserved_unrelated_entries'],'Preserved resource count differs')
    require(sha(single[RUNTIME_EXTENSION])==qa['runtime_sha256'],'Runtime digest differs')
    require(single[RUNTIME_EXTENSION]==old[RUNTIME_EXTENSION],'Existing Gallery runtime changed')
    require(sha(single[REFERENCE_ASSET])==qa['original_back_reference_sha256'],'Original callback reference digest differs')
    require(dex_class_types(single[REFERENCE_ASSET]),'Original callback reference DEX invalid')
    entries=archive(payloads[SOURCE_ZIP])
    expected={**reviewed_files(),'publication/manifest-template.json':(PUBLICATION/'manifest-template.json').read_bytes(),'publication/manifest.json':args.config.read_bytes(),QA_NAME:payloads[QA_NAME]}
    require(entries==expected,'Source archive differs from exact reviewed source and QA')
    notes=payloads['RELEASE_NOTES.txt'].decode()
    require(notes==(PUBLICATION/'RELEASE_NOTES.txt').read_text(),'Notes changed')
    require('未確認' in notes and not any(HEADING.match(line) for line in notes.splitlines()),'Release note identity invalid')
    sums={}
    for line in payloads['SHA256SUMS.txt'].decode().splitlines():
        digest,name=line.split('  ',1)
        require(name in payloads and name not in sums and sha(payloads[name])==digest,'Checksum mismatch: '+name);sums[name]=digest
    require(set(sums)==set(ASSETS)-{'SHA256SUMS.txt'},'Checksum inventory differs')
    assets={name:{'bytes':len(raw),'sha256':sha(raw)} for name,raw in payloads.items()}
    report={'baseline_bundle_version':PREVIOUS,'baseline_bundle_sha256':BASE_SHA256,'android_device_tested':False,'official_apk_sha256':cfg['official_apk_sha256'],'official_apk_apply_tested':True,'official_apk_apply_cases':4,'compatibility_patcher2_apply':qa['compatibility_patcher2_apply'],'original_back_reference_sha256':qa['original_back_reference_sha256'],'host_tests':qa['host_tests'],'loader_preservation':qa['loader_preservation'],'unrelated_loader_classes_preserved':len(retained_types),'unrelated_resource_entries_byte_identical':len(retained),'two_fresh_ci_builds_mpp_qa_byte_identical':cfg.get('ci_build_pair_verified') is True,'qa_sha256':sha(payloads[QA_NAME])}
    return cfg,payloads,notes,assets,report

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
                "User-Agent": "Hiro-FossifyGallery101-Publication",
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
    with tempfile.TemporaryDirectory(prefix="fossify-gallery101-index-") as temporary:
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



def metadata(state,cfg,url,created,notes):
    old=state['manifest']
    require(old['version']==PREVIOUS and old['download_url']==BASE_URL,'Manager baseline advanced')
    require(utc_date(created)>utc_date(old['created_at']),'created_at must advance')
    feed={**old,'version':BUNDLE,'created_at':created,'download_url':url,'page_url':PAGE,'signature_download_url':'','description':notes.strip()+'\n\n'+old['description']}
    bullets='\n'.join(f"* **{scope}:** {cfg['summary']}" for scope in APP_SCOPES)
    changelog=f"# {BUNDLE} ({created[:10]})\n\n"+bullets+'\n\n'+notes.strip()+'\n\n'+state['log']
    for scope in APP_SCOPES:
        require(has_changes_for(changelog,PREVIOUS,scope),'Missing Manager update alias: '+scope)
        require(not has_changes_for(changelog,BUNDLE,scope),'Freshly patched app would remain outdated: '+scope)
    unchanged={scope for entry in parsed_entries(state['log']) for scope,_ in entry['bullets']}-set(APP_SCOPES)
    require(not any(has_changes_for(changelog,PREVIOUS,app) for app in unchanged),'False update scope for unchanged app')
    require(parsed_entries(changelog)[0]['bullets']==[(scope,cfg['summary']) for scope in APP_SCOPES],'Unexpected new update scope')
    require(changelog.endswith(state['log']) and feed['description'].endswith(old['description']),'History altered')
    return feed,changelog


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
    cfg,payloads,notes,assets,report=validate_local(args)
    if args.local_only:
        print(json.dumps({'status':'local_artifacts_verified',**report},ensure_ascii=False,indent=2));return
    require(cfg.get('ci_build_pair_verified') is True and os.environ.get('GITHUB_ACTIONS')=='true'
            and cfg.get('ci_run_id')==os.environ.get('GITHUB_RUN_ID') and cfg.get('ci_source_commit')==os.environ.get('GITHUB_SHA'),
            'Publication requires this workflow two-build verification')
    states={branch:snapshot(branch) for branch in ('main','dev')};check_heads(states)
    require(states['main']['manifest']==states['dev']['manifest'],'main/dev feeds differ')
    require(states['main']['head']==os.environ['GITHUB_SHA']==git(args.repo,'rev-parse','HEAD'),'Source main moved')
    parents=[line.split(' ',1)[1] for line in git(args.repo,'cat-file','-p','HEAD').splitlines() if line.startswith('parent ')]
    require(parents==[cfg['reviewed_main_head']] and states['dev']['head']==cfg['reviewed_dev_head'],'Source/baseline review head changed')
    require(sha(fetch(BASE_URL))==BASE_SHA256,'Public baseline bytes differ')
    created=dt.datetime.now(dt.timezone.utc).replace(tzinfo=None,microsecond=0).isoformat()
    for state in states.values():metadata(state,cfg,'https://example.invalid/preflight-only',created,notes)
    require(api(f'repos/{REPO}/releases/tags/{TAG}',absent=True) is None,'Release exists; inspect before retry')
    require(api(f'repos/{REPO}/git/ref/tags/{TAG}',absent=True) is None,'Tag exists; inspect before retry')
    require(api(f'repos/{REPO}/git/ref/heads/{RAW_BRANCH}',absent=True) is None,'Distribution branch exists; inspect before retry')
    for state in states.values():
        require(api(f"repos/{REPO}/contents/downloads/{COMBINED}?ref={state['head']}",absent=True) is None,'Bundle filename already exists')
    releases=api(f'repos/{REPO}/releases?per_page=100')
    require(not any(a.get('name')==COMBINED for r in releases for a in r.get('assets',[])),'Bundle version already used')
    if args.preflight_only:
        print(json.dumps({'status':'preflight_passed_no_remote_writes','heads':{b:s['head'] for b,s in states.items()},**report},ensure_ascii=False,indent=2));return
    receipt_path=args.dist/RECEIPT_NAME
    require(not receipt_path.exists(),'Prior receipt exists; inspect before retry')
    receipt={'schema':'fossify-gallery101-publication-receipt-v1','patch_version':VERSION,'bundle_version':BUNDLE,
             'baseline_bundle_version':PREVIOUS,'baseline_bundle_sha256':BASE_SHA256,'source_commit':os.environ['GITHUB_SHA'],
             'publication_manifest_sha256':sha(args.config.read_bytes()),'assets':assets,'validation':report,'android_device_tested':False,'manager_parser_sources':list(MANAGER_PARSER_SOURCES),'operations':[]}
    def checkpoint(status,**changes):
        receipt.update(status=status,**changes);receipt_path.write_bytes(json_bytes(receipt));print(status,flush=True)
    checkpoint('preflight_passed')
    try:
        check_heads(states)
        release=api(f'repos/{REPO}/releases',{'tag_name':TAG,'target_commitish':states['main']['head'],'name':cfg['release_title'],'body':notes,'draft':True,'prerelease':True,'make_latest':'false'})
        checkpoint('draft_created',release_id=release['id'])
        for name in ASSETS:subprocess.run(['gh','release','upload',TAG,'--repo',REPO,str(args.dist/name)],check=True)
        verify_assets(release['id'],assets,False);checkpoint('draft_assets_verified');check_heads(states)
        api(f"repos/{REPO}/releases/{release['id']}",{'draft':False,'prerelease':True,'make_latest':'false'},'PATCH')
        verify_assets(release['id'],assets,True);checkpoint('release_bytes_verified');check_heads(states)
        git(args.repo,'fetch','--no-tags',f'https://github.com/{REPO}.git',states['main']['head'])
        raw_state=prepare_commit(args.repo,{**states['main'],'branch':RAW_BRANCH},{'downloads/'+n:payloads[n] for n in (SINGLE,COMBINED)},'release: immutable Fossify Gallery101 and bundle258 MPPs')
        git(args.repo,'push',f'--force-with-lease=refs/heads/{RAW_BRANCH}:',f'https://github.com/{REPO}.git',f"{raw_state['head']}:refs/heads/{RAW_BRANCH}")
        require(head(RAW_BRANCH)==raw_state['head'],'Distribution branch moved')
        urls={n:f"https://raw.githubusercontent.com/{REPO}/{raw_state['head']}/downloads/{n}" for n in (SINGLE,COMBINED)}
        for name,url in urls.items():require(fetch(url)==payloads[name],'Immutable raw bytes differ: '+name)
        checkpoint('immutable_downloads_verified',raw_commit=raw_state['head'],download_urls=urls);check_heads(states)
        new_metadata,updates={},{}
        mirrored={f'{RELEASE_ROOT}/{name}':raw for name,raw in reviewed_files().items()}
        for name in ('manifest-template.json','manifest.json'):mirrored[f'{RELEASE_ROOT}/publication/{name}']=(PUBLICATION/name).read_bytes()
        for name in (QA_NAME,'RELEASE_NOTES.txt','SHA256SUMS.txt'):mirrored[f'{RELEASE_ROOT}/{name}']=payloads[name]
        for branch,state in states.items():
            feed,changelog=metadata(state,cfg,urls[COMBINED],created,notes);new_metadata[branch]=(feed,changelog)
            updates[branch]={'patches-bundle.json':json_bytes(feed),'CHANGELOG.md':changelog.encode(),**mirrored}
        active=commit_feeds(args.repo,states,updates,'release: Fossify Gallery patcher portability v1.0.1 and bundle258')
        for branch,state in active.items():
            feed,changelog=new_metadata[branch]
            require(json.loads(content('patches-bundle.json',state['head']))==feed,'Remote feed differs')
            require(content('CHANGELOG.md',state['head']).decode()==changelog,'Remote changelog differs')
            require(content(POLICY_PATH,state['head'])==states[branch]['ulike_policy'],'ULike active policy changed')
            require(fetch(feed['download_url'])==payloads[COMBINED],'Manager MPP bytes differ')
            for attempt in range(12):
                prefix=f'https://raw.githubusercontent.com/{REPO}/{branch}';nonce=str(time.time_ns())
                public_feed=json.loads(fetch(prefix+'/patches-bundle.json?fossify101='+nonce))
                public_log=fetch(prefix+'/CHANGELOG.md?fossify101='+nonce).decode()
                if public_feed==feed and public_log==changelog:break
                check_heads(active)
                if attempt<11:time.sleep(3)
            else:raise RuntimeError('Public metadata propagation incomplete: '+branch)
            receipt['operations'].append({'branch':branch,'commit':state['head'],'feed_verified':True,'changelog_verified':True,'ulike_policy_unchanged':True,'manager_download_verified':True})
        check_heads(active);verify_assets(release['id'],assets,True)
        checkpoint('published_and_verified',published=True,release_url=PAGE,manager_main_dev_updated_atomically=True,manager_app_scopes_verified=list(APP_SCOPES),all_non_gallery_bundle_classes_and_resources_preserved=True)
        saved=commit_feeds(args.repo,active,{branch:{f'{RELEASE_ROOT}/{RECEIPT_NAME}':receipt_path.read_bytes()} for branch in active},'docs: verified Fossify Gallery publication receipt')
        for branch,state in saved.items():
            require(content(f'{RELEASE_ROOT}/{RECEIPT_NAME}',state['head'])==receipt_path.read_bytes(),'Saved receipt differs')
            require(content(POLICY_PATH,state['head'])==states[branch]['ulike_policy'],'ULike policy changed saving receipt')
            require(json.loads(content('patches-bundle.json',state['head']))==new_metadata[branch][0],'Feed changed saving receipt')
        check_heads(saved);verify_assets(release['id'],assets,True)
        print('PASS public release, immutable downloads, atomic main/dev Manager feeds, Fossify-only update scope and preserved baseline',flush=True)
    except BaseException as error:
        checkpoint('failed_requires_inspection',error=type(error).__name__+': '+str(error));raise


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--dist',type=Path,required=True);parser.add_argument('--repo',type=Path,required=True);parser.add_argument('--base',type=Path,required=True)
    parser.add_argument('--config',type=Path,default=PUBLICATION/'manifest.json')
    mode=parser.add_mutually_exclusive_group();mode.add_argument('--local-only',action='store_true');mode.add_argument('--preflight-only',action='store_true')
    args=parser.parse_args()
    for key in ('dist','repo','base','config'):setattr(args,key,getattr(args,key).resolve())
    publication(args)

if __name__=='__main__':main()
