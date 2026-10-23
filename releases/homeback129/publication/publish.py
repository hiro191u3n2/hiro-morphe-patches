#!/usr/bin/env python3
"""Publish locally SDK-tested and twice CI-rebuilt Back MPPs with source/byte gates and atomic feed updates.
No real-device or third-party APK validation is implied by synthetic fixtures.
"""
from __future__ import annotations
import argparse,base64,datetime as dt,hashlib,io,json,os,re,subprocess,struct,sys,tempfile,time,urllib.error,urllib.request,zipfile
from pathlib import Path, PurePosixPath
REPO='hiro191u3n2/hiro-morphe-patches'
VERSION,PREVIOUS,BUNDLE='1.1.19','1.0.277','1.0.278'
TAG='oneback-v1.1.19';RAW_BRANCH='release/homeback129'
SINGLE='OneBack_Exit_NoRecents_v1.1.19.mpp';COMBINED='Hiro_Morphe_Patches_v1.0.278.mpp'
BASE_NAME='Hiro_Morphe_Patches_v1.0.277.mpp'
BASE_SHA256='797e102352dfb46fc38f0ffb4a8d65066a843079d5089be8f390306ac3c4281a'
BASE_URL='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/0cac9cc15a075444cd6a462b41b479dab1d0a6aa/downloads/Hiro_Morphe_Patches_v1.0.277.mpp'
QA_NAME='QA_HomeBack_v1.1.19.json';SOURCE_ZIP='HomeBack_v1.1.19_sources_and_QA.zip'
RECEIPT_NAME='publication_HomeBack_v1.1.19.json'
ASSETS=(SINGLE,COMBINED,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
RELEASE_ROOT='releases/homeback129';POLICY_PATH='releases/ULike_ACTIVE.json'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'
PUBLICATION=Path(__file__).resolve().parent;SOURCE_ROOT=PUBLICATION.parent
sys.path.insert(0,str(PUBLICATION))
MF='META-INF/MANIFEST.MF';PATCH_PREFIX='app/hiro/oneback/patches/';RUNTIME_EXTENSION='extensions/oneback_exit.mpe'
UPGRADE_PINS={'upgrades/oneback-runtime120.dex':'d614e7ba8501af6ee65b3b237cf75034d3035a46c3556ef23365363b25955fc2','upgrades/oneback-runtime121.dex':'52e1817fdaa16591cb5eb7b0383aaa07f578a4a6799dad1cf5ef6e99d9a817d5','upgrades/oneback-runtime122.dex':'e23a94da3f45accb98d52f3ed3ebbbdf7e4238b7fb6a30cb950199794c1c012b','upgrades/oneback-runtime123.dex':'cea6c6519bd9c6c70776cddd3e6a26f28709a77d0fed7a8335a7baad7be28a0e','upgrades/oneback-runtime124.dex':'559d89a6e7c3008cfb14b1e2c5e46285cf3a48365725d87524195a466c02b07f','upgrades/oneback-runtime125.dex':'f6ba029c8d68954d42e76a8fbda8ae8f9d7ce1b22be29e37719636b7254d8b5f','upgrades/oneback-runtime127.dex':'818800fec2272345ac6ff60d5fc86ef769b83746aa89544638f7a6ad44a0231c'}
UPGRADE_RESOURCES=tuple(UPGRADE_PINS)
BINARY_FIXTURES=tuple('src/assets/'+name for name in UPGRADE_RESOURCES)
LOCAL_SCOPE_EVIDENCE='local-scope-validation.json'
EVIDENCE_DIRS={'app-scope129'}
TOOLS={'morphe200.jar':'deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5','morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','morphe22.jar':'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
REGRESSION_MINIMUMS={
    'twitter119':('Back119TwitterTest',35,104),
    'twitter124':('Back124TwitterTest',24,147),
    'twitter125key':('Back125TwitterKeyTest',19,126),
    'twitter125system':('Back125TwitterSystemTest',43,321),
    'twitter125terminal':('Back125TwitterTerminalTest',24,234),
    'instagram119':('Back119InstagramTest',15,51),
    # Consumed native guards return before the dedicated Reels bridge. Old
    # tests fabricated a second bridge call after that return; v123 replaces
    # those impossible calls with explicit native guard/call-count scenarios.
    'instagram120main':('Back120MainDispatchTest',16,76),
    'instagram120':('Back120InstagramTest',25,92),
    'instagram121layers':('Back121InstagramActivityLayersTest',34,60),
    'instagram121scope':('Back121InstagramScopeTest',9,34),
    'instagram121current':('Back121InstagramCurrentTest',23,78),
    # Three unknown-root cases now assert native preservation instead of
    # task removal plus repeated-remove state; all39 scenarios are retained.
    'instagram121dispatch':('Back121InstagramDispatchTest',39,199),
    'instagram122dispatcher':('Back122InstagramDispatcherTest',22,119),
    'instagram123tab':('Back123InstagramTabBackTest',14,64),
    'instagram123native':('Back123NativeReelsTest',10,56),
    'instagram126profile':('Back126InstagramProfileTest',22,206),
    'instagram127compat':('Back127Instagram452Test',18,160),
}
APP_SCOPES=('TikTok',)
OTHER_APPS=('X','Twitter','Instagram','Trip.com','ULike','Microsoft SwiftKey Beta','SwiftKey Beta','Berry Browser','Yahoo!乗換案内','Amazon Shopping','BrightnessClick','LINE','Fossify Gallery','Aurora Store')
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


def validate_qa(qa, cfg=None):
    require(qa.get('schema') == 'homeback129-qa-v1', 'Wrong QA schema')
    require((qa['patch_version'], qa['bundle_version'], qa['baseline_version']) ==
            (VERSION, BUNDLE, PREVIOUS), 'QA version identity differs')
    require(qa['baseline_sha256'] == BASE_SHA256, 'QA baseline identity differs')
    require(qa['physical_android_tested'] is False, 'Host build must not claim Android device execution')
    require('PASS selftest:' in qa['dex_structure'], 'DEX structural tests incomplete')
    tests = qa['host']
    expected_tests = {p.stem for p in (SOURCE_ROOT / 'src/test-runtime').glob('*.java')
                      if 'public static void main' in p.read_text()}
    require(set(tests) == expected_tests and 'Back126InstagramProfileTest' in tests and 'Back127Instagram452Test' in tests and 'Back128InstagramProfileTest' in tests,
            'Host test inventory missing or new regression test absent')
    for name, summary in tests.items():
        require(summary.startswith(('PASS ', 'SUCCESS:', 'REGRESSION'))
                and ('failed' not in summary or summary.endswith(' 0 failed')),
                'Failed/incomplete host test: ' + name)
    for _, (name, minimum_cases, minimum_assertions) in REGRESSION_MINIMUMS.items():
        match = re.fullmatch(r'PASS '+re.escape(name)+r': (\d+) cases, (\d+) assertions', tests.get(name, ''))
        require(match is not None and int(match[1]) >= minimum_cases and int(match[2]) >= minimum_assertions,
                'Previous regression coverage lost: ' + name)
    host = re.fullmatch(r'SUCCESS: (\d+) cases, (\d+) assertions', tests['HomeBackHostTest'])
    require(host is not None, 'Host tests incomplete')
    host_counts = {'cases': int(host[1]), 'assertions': int(host[2])}
    require(host_counts['cases'] >= 315 and host_counts['assertions'] >= 1476,
            'Existing host coverage lost')
    packages = {'com.twitter.android', 'com.instagram.android', 'com.zhiliaoapp.musically',
                'com.ss.android.ugc.trill', 'ctrip.english'}
    wanted = {(bundle, package, sdk) for bundle in (SINGLE, COMBINED) for package in packages
              for sdk in ('morphe.jar','morphe22.jar','morphe200.jar')}
    require(len(qa['synthetic']) == 30 and
            {(row['bundle'], row['package'], row['sdk']) for row in qa['synthetic'] if row['result'] == 'PASS'} == wanted,
            'Synthetic full-patcher application coverage incomplete')
    non_instagram = packages - {'com.instagram.android'}
    require(all(row.get('collision_fixture') is True for row in qa['synthetic']
                if row['package'] in non_instagram),
            'Non-Instagram full-patcher cases must exercise obfuscated-name collisions')
    scope = qa.get('app_scope', {})
    require(scope.get('result') == 'PASS'
            and isinstance(scope.get('summary'), str) and scope['summary'].startswith('PASS ')
            and set(scope.get('packages', [])) == non_instagram
            and scope.get('instagram_malformed_rejected') is True
            and scope.get('colliding_classes_unchanged') is True,
            'Package scope regression evidence incomplete')
    repro = scope.get('old_repro', {})
    require(repro.get('result') == 'REPRODUCED'
            and repro.get('error') == 'Instagram 452 Reels Back method missing',
            'Published prior-patch failure was not reproduced')
    require(qa.get('runtime_preserved') is True
            and qa.get('baseline_runtime_sha256') == qa.get('runtime_sha256'),
            'Patcher-only fix must preserve the published runtime exactly')
    require(set(qa['artifacts']) == {SINGLE, COMBINED}, 'QA artifact inventory differs')
    expected_metadata = {(bundle, sdk) for bundle in (SINGLE, COMBINED) for sdk in ('morphe.jar','morphe22.jar','morphe200.jar')}
    require(len(qa.get('metadata', [])) == 6 and {(row['bundle'], row['sdk']) for row in qa['metadata'] if row.get('all_versions') is True and row.get('executable_from_mpp') is True} == expected_metadata, 'Official loader metadata/execution origin coverage incomplete')
    source = SOURCE_ROOT / 'src'
    require(qa['sources'] == {p.relative_to(source).as_posix(): sha(p.read_bytes())
            for p in sorted(source.rglob('*')) if p.is_file() and p.suffix in ('.java', '.py', '.dex')},
            'Sources differ from the tested build')
    require(qa['production_android_sdk_compile'] is True and qa['toolchain_sha256'] == TOOLS,
            'Production SDK compile gate or pinned tools differ')
    require(re.fullmatch(r'[0-9a-f]{64}', qa['runtime_sha256']) is not None,
            'Runtime DEX identity missing')
    if cfg is not None:
        require(qa['artifacts'] == cfg['expected_mpp'] and qa['host'] == cfg['local_expected_host'],
                'QA identities/results differ from the local freeze')
    return host_counts


def validate_scope_evidence(qa, cfg=None):
    """Bind this release's synthetic collision proof to frozen MPP and QA bytes.

    This is intentionally not a third-party APK or Android execution record.
    The supplied TikTok 47.4.3 error is reproduced with obfuscated-name collision
    fixtures and the prior published patch; runtime bytes remain unchanged.
    """
    raw = (PUBLICATION / LOCAL_SCOPE_EVIDENCE).read_bytes()
    evidence = json.loads(raw)
    require(evidence.get('schema') == 'homeback129-local-scope-validation-v1'
            and evidence.get('result') == 'PASS'
            and evidence.get('original_apk_apply_tested') is False
            and evidence.get('physical_android_tested') is False,
            'Scope evidence absent, wrong release, or falsely claims actual APK/device testing')
    require(evidence.get('reported_package') == 'com.zhiliaoapp.musically'
            and evidence.get('reported_version') == '47.4.3',
            'Reported issue identity differs')
    require(evidence.get('artifacts') == qa['artifacts']
            and evidence.get('runtime_sha256') == qa['runtime_sha256']
            and evidence.get('runtime_preserved') is True
            and evidence.get('baseline_runtime_sha256') == qa['baseline_runtime_sha256']
            and evidence.get('app_scope') == qa['app_scope']
            and evidence.get('synthetic') == qa['synthetic'],
            'Scope evidence not bound to this release artifacts, runtime and executed fixtures')
    require(evidence.get('qa_sha256') == sha(json_bytes(qa)), 'Scope evidence identifies a different QA record')
    require(evidence.get('limitations') and any('47.4.3' in text for text in evidence['limitations']),
            'Actual TikTok APK limitation missing')
    if cfg is not None:
        require(cfg.get('local_scope_evidence_sha256') == sha(raw), 'Scope evidence changed after freeze')
    return evidence


def validate_local(args):
    cfg = json.loads(args.config.read_text())
    require(cfg.get('freeze_state') == 'frozen', 'Local build identities have not been frozen')
    require(cfg['schema'] == 'homeback129-publication-v1' and cfg['patch_version'] == VERSION
            and cfg['bundle_version'] == BUNDLE, 'Publication identity mismatch')
    require(sha(args.base.read_bytes()) == BASE_SHA256 and cfg['baseline_sha256'] == BASE_SHA256
            and cfg['baseline_url'] == BASE_URL, 'Unreviewed baseline')
    payloads = {name: (args.dist / name).read_bytes() for name in ASSETS}
    require(set(cfg['expected_mpp']) == {SINGLE, COMBINED}, 'MPP inventory differs')
    for name, row in cfg['expected_mpp'].items():
        require(row == {'bytes': len(payloads[name]), 'sha256': sha(payloads[name])},
                'Rebuilt MPP differs from local frozen build: ' + name)
    require(sha(payloads[QA_NAME]) == cfg['expected_qa_sha256'],
            'Rebuilt QA differs from local frozen build')
    qa = json.loads(payloads[QA_NAME])
    host_counts = validate_qa(qa, cfg)
    scope_evidence = validate_scope_evidence(qa, cfg)
    old, new, single = map(archive, (args.base.read_bytes(), payloads[COMBINED], payloads[SINGLE]))
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
    own=lambda name:name.startswith(PATCH_PREFIX) or name==RUNTIME_EXTENSION or name in UPGRADE_RESOURCES
    # Owned reference assets are excluded from the unrelated-entry count, as in build.py.
    # Independently retain every previously published reference byte-for-byte.
    retained={name:data for name,data in old.items() if name not in (MF,'classes.dex') and not own(name)}
    require(all(new.get(name)==raw for name,raw in old.items() if name in UPGRADE_RESOURCES),
            'Previously published upgrade reference changed/removed')
    require(retained and all(new.get(name)==data for name,data in retained.items()),'Unrelated resource changed/removed')
    require(all(name in old or own(name) for name in new),'Unrelated resource added')
    require(all(new.get(name)==data for name,data in single.items() if own(name)),'Standalone/integrated OneBack differs')
    require(all(own(name) or name in (MF,'classes.dex') for name in single),'Unexpected standalone entry')
    require(len(retained)==qa['preserved_unrelated_entries'],'Resource inventory mismatch')
    src={str(p.relative_to(SOURCE_ROOT/'src')):sha(p.read_bytes()) for p in sorted((SOURCE_ROOT/'src').rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.dex')}
    require(src==qa['sources'],'Authored sources differ from tested sources')

    require(old[RUNTIME_EXTENSION] == new[RUNTIME_EXTENSION] == single[RUNTIME_EXTENSION],
            'Patcher-only fix changed the published runtime bytes')
    require(sha(single[RUNTIME_EXTENSION]) == qa['runtime_sha256'] == qa['baseline_runtime_sha256'],
            'QA runtime/baseline digest differs')
    for name in UPGRADE_RESOURCES:
        require(single.get(name) == (SOURCE_ROOT / 'src/assets' / name).read_bytes()
                and sha(single[name]) == UPGRADE_PINS[name],
                'Pinned upgrade resource changed: ' + name)
    entries = archive(payloads[SOURCE_ZIP])
    for name in BINARY_FIXTURES:
        require(entries.get(name) == (SOURCE_ROOT / name).read_bytes(), 'Missing/changed pinned upgrade reference')
    for name, data in entries.items():
        parts = PurePosixPath(name).parts
        require(parts[0] != 'evidence' or (len(parts) >= 3 and parts[1] in EVIDENCE_DIRS), 'Unreviewed historical evidence in source archive: ' + name)
        if name in BINARY_FIXTURES:
            continue
        require(PurePosixPath(name).suffix in ('.java', '.py', '.json', '.txt', '.md', '.yml', '.sh'),
                'Unexpected binary in source archive: ' + name)
        data.decode('utf-8')
        require(b'\x00' not in data, 'Binary source entry')
    for folder in ('src', 'publication', 'evidence'):
        for path in (SOURCE_ROOT / folder).rglob('*'):
            relative = path.relative_to(SOURCE_ROOT)
            if folder == 'evidence' and (len(relative.parts) < 3 or relative.parts[1] not in EVIDENCE_DIRS):
                continue
            if path.is_file() and (path.suffix in ('.java', '.py', '.json', '.txt', '.md', '.yml', '.sh') or path.relative_to(SOURCE_ROOT).as_posix() in BINARY_FIXTURES) and '__pycache__' not in path.parts:
                require(entries.get(path.relative_to(SOURCE_ROOT).as_posix()) == path.read_bytes(),
                        'Source archive missing/changed file: ' + str(path))
    require(entries.get('README.md') == (SOURCE_ROOT / 'README.md').read_bytes(), 'Archived README differs')
    require(entries.get(QA_NAME) == payloads[QA_NAME], 'Archived QA differs')
    notes = payloads['RELEASE_NOTES.txt'].decode()
    require(notes == (PUBLICATION / 'RELEASE_NOTES.txt').read_text(), 'Notes changed')
    require('未確認' in notes and not any(HEADING.match(line) for line in notes.splitlines()),
            'Notes missing limitations or contain conflicting Manager headings')
    asset_checksums = {}
    for line in payloads['SHA256SUMS.txt'].decode().splitlines():
        digest, name = line.split('  ', 1)
        require(name in payloads and name not in asset_checksums and sha(payloads[name]) == digest,
                'Asset checksum differs: ' + name)
        asset_checksums[name] = digest
    require(set(asset_checksums) == set(ASSETS) - {'SHA256SUMS.txt'}, 'Checksum inventory differs')
    report = {
        'baseline_bundle_version': PREVIOUS, 'baseline_bundle_sha256': BASE_SHA256,
        'android_device_tested': False, 'original_apk_apply_tested': False,
        'unrelated_resource_entries_byte_identical': len(retained),
        'unrelated_loader_classes_unchanged': len(retained_types),
        'two_fresh_ci_builds_required': True,
        'two_fresh_ci_builds_mpp_qa_byte_identical': cfg.get('ci_build_pair_verified') is True,
        'local_sdk_build': True, 'local_host_tested': True,
        'validation_coverage': {'host_cases': host_counts['cases'], 'host_assertions': host_counts['assertions'],
                                'host_suites': qa['host'], 'synthetic_apk_apply_cases': len(qa['synthetic']),
                                'local_real_apk_apply_cases': 0, 'app_scope': qa['app_scope'], 'runtime_preserved': True},
        'local_synthetic_scope_validation': scope_evidence, 'qa_sha256': sha(payloads[QA_NAME])}
    assets = {name: {'bytes': len(raw), 'sha256': sha(raw)} for name, raw in payloads.items()}
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
                "User-Agent": "Hiro-HomeBack129-Publication",
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
    with tempfile.TemporaryDirectory(prefix="homeback129-index-") as temporary:
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
            "Active bundle is no longer the reviewed baseline277")
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
        parents=[line.split(' ',1)[1] for line in git(args.repo,'cat-file','-p','HEAD').splitlines() if line.startswith('parent ')]
        require(parents==[cfg['reviewed_main_head']], 'Source commit must directly descend from the reviewed remote main head')
        require(states['dev']['head']==cfg['reviewed_dev_head'], 'dev changed after source/baseline review')
    require(sha(fetch(BASE_URL)) == BASE_SHA256, "Public baseline277 bytes differ")
    created = dt.datetime.now(dt.timezone.utc).replace(tzinfo=None, microsecond=0).isoformat()
    for state in states.values():
        metadata(state, cfg, "https://example.invalid/preflight-only", created, notes)
    require(api(f"repos/{REPO}/releases/tags/{TAG}", absent=True) is None, "Release already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/tags/{TAG}", absent=True) is None, "Tag already exists; inspect before retry")
    require(api(f"repos/{REPO}/git/ref/heads/{RAW_BRANCH}", absent=True) is None,
            "Distribution branch exists; inspect before retry")
    for state in states.values():
        require(api(f"repos/{REPO}/contents/downloads/{COMBINED}?ref={state['head']}",absent=True) is None,
                'Proposed bundle filename is already present on a Manager branch')
    recent_releases=api(f"repos/{REPO}/releases?per_page=100")
    require(not any(asset.get('name')==COMBINED for release in recent_releases for asset in release.get('assets',[])),
            'Proposed bundle version is already used by a recent release asset')
    if args.preflight_only:
        print(json.dumps({"status": "preflight_passed_no_remote_writes",
                          "heads": {branch: state["head"] for branch, state in states.items()},
                          "bundle_version": BUNDLE, "patch_version": VERSION, **report}, ensure_ascii=False, indent=2))
        return
    receipt_path = args.dist / RECEIPT_NAME
    require(not receipt_path.exists(), "Prior publication receipt exists; inspect before retry")
    receipt = {"schema": "homeback129-publication-receipt-v1", "patch_version": VERSION,
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
            "release: exact HomeBack129 and bundle278 immutable MPP downloads")
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
            # Mirror only this reviewed release's source/evidence on both feed branches.
            for path in [p for folder in ('src','publication','evidence') for p in (SOURCE_ROOT/folder).rglob('*')]+[SOURCE_ROOT/'README.md']:
                relative=path.relative_to(SOURCE_ROOT).as_posix()
                if path.is_file() and (path.suffix in (".java", ".py", ".json", ".txt", ".md", ".yml") or relative in BINARY_FIXTURES) and "__pycache__" not in path.parts:
                    updates[branch][RELEASE_ROOT + "/" + relative] = path.read_bytes()
            for name in (QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds(args.repo, states, updates, "release: OneBack exit and recents removal and bundle278 Manager update")
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
                public_feed=json.loads(fetch(prefix+"/patches-bundle.json?homeback129="+nonce))
                public_log=fetch(prefix+"/CHANGELOG.md?homeback129="+nonce).decode()
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
                   other_apps_false_updates=False, all_unrelated_patch_resources_preserved=True,
                   fixed_chatgpt_site_updated=False)
        receipt_bytes = receipt_path.read_bytes()
        saved = commit_feeds(args.repo, active,
            {branch: {RELEASE_ROOT + "/" + RECEIPT_NAME: receipt_bytes} for branch in active},
            "docs: verified HomeBack129 publication receipt")
        for branch, state in saved.items():
            require(content(RELEASE_ROOT + "/" + RECEIPT_NAME, state["head"]) == receipt_bytes, "Receipt commit mismatch")
            require(content(POLICY_PATH, state["head"]) == states[branch]["ulike_policy"], "ULike policy changed while saving receipt")
            require(json.loads(content("patches-bundle.json", state["head"])) == new_metadata[branch][0],
                    "Feed changed while saving receipt")
        verify_assets(release["id"], assets, True)
        check_heads(saved)
        print("PASS exact Release and immutable MPP bytes, atomic main/dev feeds, TikTok scope, all unrelated patches and receipt", flush=True)
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
