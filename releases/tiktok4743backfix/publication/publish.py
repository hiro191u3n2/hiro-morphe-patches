#!/usr/bin/env python3
"""Publish the TikTok 47.4.3 native Back fix and verified 47.5.3 rejection after exact byte gates.
The two fresh CI builds must reproduce the MPPs actually tested on the original
APK. Neither these checks nor the release claims physical Android operation.
"""
from __future__ import annotations
import argparse,base64,datetime as dt,hashlib,importlib.util,io,json,os,re,subprocess,struct,sys,tempfile,time,urllib.error,urllib.request,zipfile
from pathlib import Path, PurePosixPath
REPO='hiro191u3n2/hiro-morphe-patches'
VERSION,PREVIOUS,BUNDLE='4.2.0','1.0.281','1.0.282'
TAG='tiktok-v4.2.0';RAW_BRANCH='release/tiktok4743backfix'
SINGLE='TikTok_47.4.3_Hiro_v4.2.0.mpp';COMBINED='Hiro_Morphe_Patches_v1.0.282.mpp'
BASE_NAME='Hiro_Morphe_Patches_v1.0.281.mpp'
BASE_SHA256='22020c9ecfa22aaaf5ec089e30f266f1f77bf9384d129105459bde23cc4d4dab'
BASE_URL='https://raw.githubusercontent.com/hiro191u3n2/hiro-morphe-patches/736cca92f1c0825dd0f0ef3a22d6a495f024fbe8/downloads/Hiro_Morphe_Patches_v1.0.281.mpp'
UPSTREAM_SHA256='45dc0c2244f8cf6973de18542d2d9a0cc25512f5b1de52438c685b82edb7c4cd'
UPSTREAM_URL='https://github.com/icysymmetra/tiktok-patches-for-morphe/releases/download/v0.9.0-dev.1/patches-0.9.0-dev.1.mpp'
ORIGINAL_APK_SHA256='af7cbe2b00d33ff1339730f6abb8ed7e387a83b27ff2b16748b8f41e6fc87e0b'
ORIGINAL_4753_APK_SHA256='6670ca3f93bfea379658cad3c27f05d3a380b23a0d82569cce684b7453e8ff3e'
ORIGINAL_SIGNER_SHA256='9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba'
QA_NAME='QA_TikTok_47.4.3_v4.2.0.json'
REAL_QA_NAME='QA_TikTok_47.4.3_real_APK.json'
SOURCE_ZIP='TikTok_47.4.3_Hiro_v4.2.0_sources_and_QA.zip'
RECEIPT_NAME='publication_TikTok_47.4.3_v4.2.0.json'
ASSETS=(SINGLE,COMBINED,QA_NAME,REAL_QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
RELEASE_ROOT='releases/tiktok4743backfix';POLICY_PATH='releases/ULike_ACTIVE.json'
PAGE=f'https://github.com/{REPO}/releases/tag/{TAG}'
PUBLICATION=Path(__file__).resolve().parent;SOURCE_ROOT=PUBLICATION.parent
INPUTS=json.loads((SOURCE_ROOT/'src/validation/apk-inputs.json').read_text())
UNSUPPORTED_INPUTS=json.loads((SOURCE_ROOT/'src/validation/unsupported-apk-inputs.json').read_text())
MF='META-INF/MANIFEST.MF'
OLD_PREFIXES=('app/hiro/oneback/patches/','app/hiro/tiktok4753/patches/','app/hiro/tiktokjapanese/patches/')
NEW_PREFIXES=OLD_PREFIXES
JAPANESE_PREFIX='app/hiro/tiktokjapanese/patches/'
ONEBACK_EXT='extensions/oneback_exit.mpe'
NATIVE_EXT='extensions/hiro_tiktok4753.mpe'
JAPANESE_EXT='extensions/hiro_tiktok_japanese.mpe'
NOTICE_PREFIX='META-INF/hiro-tiktok4743backfix/'
JAPANESE_SOURCE_COMMIT='efb5937ddea4fe29abdfe33027cfc0e5d397ae93'
JAPANESE_SOURCE_PATH='releases/tiktok-japanese100'
LEGACY_FILES=('build.py','test-dex/Fixtures.java')
TOOLS={'morphe200.jar':'deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5','morphe.jar':'82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c','morphe22.jar':'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864','android.jar':'4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b','d8.jar':'305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5'}
APP_SCOPES=('TikTok',)
OTHER_APPS=('X','Twitter','Instagram','Trip.com','ULike','Microsoft SwiftKey Beta','SwiftKey Beta','Berry Browser','Yahoo!乗換案内','Amazon Shopping','BrightnessClick','LINE','Fossify Gallery','Aurora Store')
MANAGER_SOURCE='https://github.com/MorpheApp/morphe-manager/blob/0ed521a8fddd1b8d72c70ac2faa0c9658ba7a3ff/app/src/main/java/app/morphe/manager/util/ChangelogParser.kt'
HEADING=re.compile(r'^#{1,3}\s+(?:\S+\s+)?(?:\[([^]]+)]\([^)]*\)|([^\s\[(]+))\s+\((\d{4}-\d{2}-\d{2})\)',re.I)
SCOPE=re.compile(r'^\* \*\*(.+?):\*\*');EXPERIMENTAL_ONLY=re.compile(r'^Add(?:ed)?\s+experimental\s+support\s+for\b',re.I)
NATIVE_PATCHES={
 'TikTok 怪しい通信だけ遮断（TikTok 47.4.3）',
 'フィード左下の文字・再投稿欄を非表示（TikTok 47.4.3）',
 'フィード下部のCapCut・エフェクト誘導枠を完全遮断（TikTok 47.4.3）',
 '動画のシークバーを常時表示（TikTok 47.4.3）',
 'シーク中の動画プレビューサムネイルを表示（TikTok 47.4.3）',
 'フォロー中のストーリーズ件数を非表示（TikTok 47.4.3）'
}
ONEBACK_NAME='戻る1回で終了・履歴削除';JAPANESE_NAME='TikTok設定を日本語化'

def require(condition,message):
    if not condition: raise RuntimeError(message)

def sha(raw): return hashlib.sha256(raw).hexdigest()

def json_bytes(value): return (json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode()

def checked_path(name):
    path=PurePosixPath(name)
    require(name and not path.is_absolute() and '..' not in path.parts and '\\' not in name
            and str(path)==name and not name.endswith('/'),'Unsafe repository/archive path: '+name)
    return path

def archive(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        names=z.namelist();require(len(names)==len(set(names)),'Duplicate archive entries')
        for name in names: checked_path(name.rstrip('/'))
        require(z.testzip() is None,'Corrupt archive')
        return {name:z.read(name) for name in names if not name.endswith('/')}

def headers(raw):
    rows=[]
    for line in raw.replace(b'\r\n',b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows),'Manifest continuation without header');rows[-1]+=line[1:]
        elif line: rows.append(line)
    pairs=[line.decode().split(': ',1) for line in rows]
    require(all(len(p)==2 for p in pairs) and len(pairs)==len(dict(pairs)),'Malformed/duplicate manifest header')
    return dict(pairs)

def dex_class_types(raw):
    """Independent inventory; the build's Java audit also compares complete class semantics."""
    require(raw.startswith(b'dex\n') and len(raw)>=112,'Invalid DEX')
    def u32(offset):
        require(0<=offset<=len(raw)-4,'DEX table outside file');return struct.unpack_from('<I',raw,offset)[0]
    ns,so,nt,to,nc,co=u32(56),u32(60),u32(64),u32(68),u32(96),u32(100)
    require(so+ns*4<=len(raw) and to+nt*4<=len(raw) and co+nc*32<=len(raw),'Truncated DEX tables')
    def string(index):
        require(index<ns,'DEX string index outside table');offset=u32(so+index*4)
        for _ in range(5):
            require(offset<len(raw),'Truncated DEX string');value=raw[offset];offset+=1
            if value<128: break
        else: raise RuntimeError('Invalid DEX string length')
        end=raw.find(b'\x00',offset);require(end>=0,'Unterminated DEX string')
        return raw[offset:end].decode('utf-8')
    types=[string(u32(to+i*4)) for i in range(nt)];found=[]
    for i in range(nc):
        index=u32(co+i*32);require(index<nt,'DEX class type outside table');found.append(types[index])
    require(len(found)==len(set(found)),'Duplicate DEX class definitions');return set(found)

def current_source_files():
    # Share the build's explicit source inventory. Historical copied evidence is
    # never included merely because a local workspace still contains it.
    spec=importlib.util.spec_from_file_location('hiro_tiktok4743_backfix_source_inventory',SOURCE_ROOT/'src/bundle_tools.py')
    helpers=importlib.util.module_from_spec(spec);spec.loader.exec_module(helpers)
    return helpers.source_files(SOURCE_ROOT/'src')

def source_inventory():
    return {name:sha(path.read_bytes()) for name,path in current_source_files().items()}

def checked_text_source(path,name):
    checked_path(name);require(path.is_file() and not path.is_symlink(),'Missing/linked source '+name)
    raw=path.read_bytes();raw.decode('utf-8')
    require(b'\x00' not in raw and path.suffix not in ('.apk','.apkm','.mpp','.mpe','.dex','.class','.jar','.zip','.pyc'),
            'Binary file in text source archive: '+name)
    return path

def release_sources():
    files={'src/'+name:checked_text_source(path,'src/'+name) for name,path in current_source_files().items()}
    for path in sorted((SOURCE_ROOT/'publication').rglob('*')):
        if path.is_file() and '__pycache__' not in path.parts:
            name=path.relative_to(SOURCE_ROOT).as_posix()
            # Old v4.1.0 integration evidence belongs to its immutable release,
            # even if a local source copy still contains that historical folder.
            if name.startswith('publication/metra-all144/'):
                continue
            files[name]=checked_text_source(path,name)
    for name in ('README.md','LICENSE','NOTICE'): files[name]=checked_text_source(SOURCE_ROOT/name,name)
    return files

def inherited_sources(repo):
    """Carry the original corresponding source for the unchanged Japanese dictionary runtime; the restricted loader is in src/japanese."""
    files={};root=repo/JAPANESE_SOURCE_PATH
    for p in sorted((root/'src').rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts:
            name='inherited/japanese/'+p.relative_to(root).as_posix();files[name]=checked_text_source(p,name)
    require(files,'Corresponding Japanese source missing')
    for name in ('README.md','LICENSE','NOTICE'):
        dest='inherited/japanese/'+name;files[dest]=checked_text_source(root/name,dest)
    for name in LEGACY_FILES:
        dest='inherited/japanese/legacy/'+name;files[dest]=checked_text_source(repo/'releases/oneback163/src'/name,dest)
    return files

def inherited_source_ids(repo): return {n:sha(p.read_bytes()) for n,p in inherited_sources(repo).items()}

def artifact_identity(value):
    return isinstance(value,dict) and set(value)=={'bytes','sha256'} and type(value['bytes']) is int and value['bytes']>0 and re.fullmatch('[0-9a-f]{64}',value['sha256']) is not None

def runtime_audit(value):
    match=re.fullmatch(r'PASS OneBack runtime: preserved=(\d+); changed=\[([^]]*)\]; added=\[([^]]*)\]',value or '')
    require(match is not None,'Missing runtime preservation proof')
    def types(raw): return set(raw.split(', ')) if raw else set()
    changed,added=types(match[2]),types(match[3])
    allowed={
        'Lapp/hiro/oneback/runtime/TikTokHome;','Lapp/hiro/oneback/runtime/TikTokHome$Route;',
        'Lapp/hiro/oneback/runtime/TikTokBack;','Lapp/hiro/oneback/runtime/TikTokBack$Session;',
        'Lapp/hiro/oneback/runtime/TikTokBack$1;','Lapp/hiro/oneback/runtime/TikTokBack$2;',
        'Lapp/hiro/oneback/runtime/TikTokScreen;','Lapp/hiro/oneback/runtime/TikTokScreen$Snapshot;',
        'Lapp/hiro/oneback/runtime/OneBackExit;',
        'Lapp/hiro/oneback/runtime/HomeScreen;'}
    require(changed and changed <= allowed and int(match[1])>0,'OneBack changed beyond the reviewed TikTok integration')
    require(all(t.startswith('Lapp/hiro/oneback/runtime/TikTok') and t.endswith(';') for t in added),'Unrelated OneBack runtime added')
    return {'preserved':int(match[1]),'changed':sorted(changed),'added':sorted(added)}

def validate_qa(qa,cfg=None):
    require(qa.get('schema')=='tiktok4743-backfix-build-v1','Wrong build QA schema')
    require((qa.get('patch_version'),qa.get('bundle_version'),qa.get('baseline_version'))==(VERSION,BUNDLE,PREVIOUS),'QA release version differs')
    require(qa.get('oneback_version')=='1.1.22' and qa.get('baseline_sha256')==BASE_SHA256,'OneBack/baseline identity differs')
    require(qa.get('physical_android_tested') is False,'Build must not claim Android device execution')
    host=qa.get('host',{})
    require(isinstance(host,dict) and len(host)>=27,'Prior and new OneBack host tests incomplete')
    for name in ('Back117TikTokTest','Back4743TikTokTest','Back4743NativeBackTest','Back4753RejectedTest',
                 'HomeBackHostTest','Back128InstagramProfileTest','Back125TwitterSystemTest'):
        require(name in host,'Missing host regression '+name)
    for name,result in host.items():
        require(isinstance(result,str) and (result.startswith('PASS ') or result.startswith('SUCCESS:')
            or re.fullmatch(r'REGRESSION.*: \d+ cases, 0 failed',result) is not None),'Missing host success result '+name)
    require(host['Back4743NativeBackTest'].startswith('PASS Back4743NativeBackTest: native-guarded single Back; label-independent;'),
            'Native Back regression missing; API-name-only checks are insufficient')
    require(host['Back4753RejectedTest']=='PASS Back4753RejectedTest: retired 47.5.3 ABI is not recognized',
            'Retired 47.5.3 runtime rejection missing')
    require(qa.get('supported_versions')==['47.4.3'] and qa.get('removed_versions')==['47.5.3'],
            'TikTok support must be 47.4.3 with 47.5.3 removed')
    legacy_tests=qa.get('legacy_tests',{})
    for name,prefix in (('privacy','PASS LegacyPrivacyTest assertions='),('ui_runtime','PASS LegacyHooksHostTest checks=')):
        require(isinstance(legacy_tests,dict) and isinstance(legacy_tests.get(name),str)
                and legacy_tests[name].startswith(prefix), 'Native host regression missing: '+name)
    require(re.fullmatch(r'PASS loader merge: preserved=\d+; replaced=\d+; new=\d+; standalone=\d+; japanese=\d+',qa.get('loader_preservation','')) is not None,'Loader semantic preservation proof absent')
    preservation=runtime_audit(qa.get('oneback_runtime_preservation'))
    require(qa.get('japanese_runtime_byte_identical') is True and qa.get('japanese_loader_version_restricted') is True,
            'Unchanged dictionary and restricted Japanese loader proofs are required')
    require(type(qa.get('preserved_unrelated_entries')) is int and qa['preserved_unrelated_entries']>0,'Resource preservation proof missing')
    sdk_names={'morphe.jar','morphe22.jar','morphe200.jar'}
    admission=qa.get('target_admission',[])
    require(len(admission)==3 and {r.get('sdk') for r in admission}==sdk_names,'Official SDK admission matrix incomplete')
    for row in admission:
        require(re.fullmatch(r'PASS TikTokTargetAdmissionTest checks=\d+; 47\.4\.3 only; all seven native/Japanese entries and dependencies; forced 47\.5\.3 rejected before mutation',row.get('result','')) is not None,
                'Production admission/dependency regression missing: '+str(row.get('sdk')))
    metadata=qa.get('metadata',[])
    expected={(a,s) for a in (SINGLE,COMBINED) for s in sdk_names}
    marker='PASS official loader: native=6; OneBack=1; Japanese=1; 47.4.3 accepted and 47.5.3 rejected by all entries and dependencies; all actions shipped'
    require(len(metadata)==6 and {(r.get('artifact'),r.get('sdk')) for r in metadata if r.get('result')==marker}==expected,'Official loader exact version/dependency matrix incomplete')
    require(set(qa.get('artifacts',{}))=={SINGLE,COMBINED} and all(artifact_identity(x) for x in qa['artifacts'].values()),'Malformed MPP identities')
    require(qa.get('sources')==source_inventory(),'Sources differ from tested build')
    require(qa.get('toolchain_sha256')==TOOLS,'Pinned tools differ')
    runtime=qa.get('runtime_sha256',{})
    require(set(runtime)=={'oneback','native','japanese'} and all(re.fullmatch('[0-9a-f]{64}',h) for h in runtime.values()),'Runtime identities incomplete')
    if cfg is not None:
        require(qa['artifacts']==cfg['expected_mpp']==cfg['local_expected_mpp'] and qa['host']==cfg['local_expected_host'],'QA differs from the two-build local freeze')
    return {'oneback_host_suites':len(host),'native_back_regression':True,'removed_4753_runtime_rejected':True,
            'official_loader_cases':6,'production_admission_sdk_cases':3,'runtime_preservation':preservation}

def validate_input_identities():
    expected_supported={'47.4.3':{'package':'com.zhiliaoapp.musically','version_name':'47.4.3',
        'version_code':2024704030,'bytes':474921201,'sha256':ORIGINAL_APK_SHA256,
        'signature_verified':True,'signer_certificate_sha256':ORIGINAL_SIGNER_SHA256}}
    expected_removed={'47.5.3':{'package':'com.zhiliaoapp.musically','version_name':'47.5.3',
        'version_code':2024705030,'bytes':476733983,'sha256':ORIGINAL_4753_APK_SHA256,
        'signature_verified':True,'signer_certificate_sha256':ORIGINAL_SIGNER_SHA256}}
    require(INPUTS==expected_supported and UNSUPPORTED_INPUTS==expected_removed,'Signed input identity or support boundary changed')

def validate_rejection(proof,artifacts):
    validate_input_identities()
    require(proof.get('schema')=='tiktok4743-backfix-rejection-v1','Missing real-APK rejection schema')
    require(proof.get('input')==UNSUPPORTED_INPUTS['47.5.3'],'Rejection uses the wrong original APK')
    require(proof.get('physical_android_tested') is False and proof.get('patcher_sdk_sha256')==TOOLS['morphe200.jar'],
            'Rejection environment or device claim differs')
    wanted={'4753_standalone_rejected':SINGLE,'4753_aggregate_rejected':COMBINED}
    cases=proof.get('cases',[])
    require(len(cases)==2 and {c.get('id') for c in cases}==set(wanted),'Both MPPs must reject the actual 47.5.3 input')
    selection=NATIVE_PATCHES|{ONEBACK_NAME,JAPANESE_NAME}
    guard=re.compile(r'requires the original global TikTok 47\.4\.3|47\.4\.3専用|unsupported TikTok 47\.5\.3')
    for case in cases:
        name=wanted[case['id']]
        require(case.get('version')=='47.5.3' and case.get('input_sha256')==ORIGINAL_4753_APK_SHA256,'Wrong rejected APK version/hash')
        require(case.get('artifact')==name and case.get('artifact_sha256')==artifacts[name]['sha256'],'Rejection is for different MPP bytes')
        require(case.get('forced_selection') is True and case.get('result')=='REJECTED','Metadata-only rejection cannot prove production guards')
        for field in ('selected_patches','rejected_patches'):
            values=case.get(field,[])
            require(len(values)==8 and set(values)==selection,'Every authored entry must reject 47.5.3: '+field)
        require(case.get('applied_patches')==[] and case.get('output_apk_created') is False,'Rejected version produced a patched APK')
        require(case.get('selection_mode')=='individual_fail_fast','Every authored entry must be tested separately without continue-on-error')
        require(type(case.get('process_exit_code')) is int and 0<case['process_exit_code']<=255
                and case.get('reported_top_level_success') is False,'Missing failed rejection process result')
        messages=case.get('failure_messages',[])
        require(isinstance(messages,list) and messages and all(isinstance(m,str) and guard.search(m) for m in messages),
                'Rejection must be caused by explicit version admission, not an unrelated crash')
        attempts=case.get('attempts',[])
        require(len(attempts)==8 and {a.get('patch') for a in attempts}==selection,'A removed-version feature was not individually exercised')
        require(messages==[a.get('failure_message') for a in attempts],'Failure causes do not describe individual patch attempts')
        for attempt in attempts:
            require(type(attempt.get('process_exit_code')) is int and attempt['process_exit_code']==case['process_exit_code']
                    and attempt.get('reported_top_level_success') is False,'An individual attempt did not fail explicitly')
            require(attempt.get('applied_patches')==[] and attempt.get('rejected_patches')==[attempt['patch']]
                    and attempt.get('output_apk_created') is False,'An individual removed-version attempt applied patches or emitted an APK')
        evidence=case.get('evidence_directory','')
        require(evidence.startswith('validation/') and len(checked_path(evidence).parts)>1,'Missing rejection evidence location')
    return {'original_apk_rejection_tested':True,'version':'47.5.3','input_sha256':ORIGINAL_4753_APK_SHA256,
            'forced_selection':True,'all_eight_authored_entries_rejected':True,'cases':list(wanted)}

def validate_integration(proof,artifacts):
    require(proof.get('schema')=='tiktok4743-backfix-integration-v1','Missing upstream integration proof')
    require(proof.get('input_sha256')==ORIGINAL_APK_SHA256 and proof.get('upstream_sha256')==UPSTREAM_SHA256,
            'Upstream integration input differs from signed 47.4.3 / Metra 0.9.0-dev.1')
    require(proof.get('artifact')==COMBINED and proof.get('artifact_sha256')==artifacts[COMBINED]['sha256'],
            'Upstream integration used different release MPP bytes')
    catalog_raw=(SOURCE_ROOT/'src/validation/integration/upstream-selected.json').read_bytes()
    require(sha(catalog_raw)=='8f4e7d6590faab0f6c8c264cbc911bae401d03524212811c186b3d89de41819a',
            'Reviewed complete upstream patch catalog changed')
    catalog=json.loads(catalog_raw)
    require(len(catalog)==136 and len(set(catalog))==136 and 'Settings' in catalog,'Incomplete upstream catalog')
    authored={'TikTok 怪しい通信だけ遮断（TikTok 47.4.3）',ONEBACK_NAME,JAPANESE_NAME}
    wanted=set(catalog)|authored
    require(len(wanted)==139,'Unexpected upstream/authored name overlap')
    for field in ('selected_patches','applied_patches'):
        values=proof.get(field,[])
        require(len(values)==139 and set(values)==wanted,'Upstream 136 plus selected Hiro three mismatch: '+field)
    require(proof.get('failed_patches')==[] and proof.get('steps_all_success') is True and proof.get('result')=='PASS',
            'Partial or failed upstream integration')
    require(type(proof.get('output_apk_bytes')) is int and proof['output_apk_bytes']>10_000_000
            and re.fullmatch('[0-9a-f]{64}',proof.get('output_apk_sha256','')) is not None
            and proof['output_apk_sha256']!=ORIGINAL_APK_SHA256,'Missing rebuilt integration APK identity')
    audits=proof.get('audits',{})
    require(isinstance(audits.get('oneback'),str) and audits['oneback'].startswith('PASS TikTok4753ContractAudit output:')
            and '\n' not in audits['oneback'],'Upstream integration native Back output audit missing')
    require(audits.get('settings_japanese')=='NATIVE_SETTINGS_HOOK_AUDIT_PASSED','Upstream integration Japanese Settings hooks missing')
    require(proof.get('max_heap')=='3g' and proof.get('force_compatibility') is False
            and proof.get('default_options_used') is True,'Integration execution conditions changed')
    require(proof.get('physical_android_tested') is False and proof.get('user_exact_selection_reproduced') is False,
            'Integration cannot claim physical operation or the unknown upstream 120 selection')
    require(proof.get('evidence_directory')=='validation/integration/139','Integration evidence location differs')
    return {'selected_count':139,'upstream_count':136,'authored_count':3,'input_sha256':ORIGINAL_APK_SHA256,
            'scope':'pipeline and emitted OneBack/Settings/Japanese hooks','physical_android_tested':False,
            'user_exact_selection_reproduced':False,'max_heap':'3g','force_compatibility':False}

def validate_real_apk(proof,artifacts,cfg=None):
    require(proof.get('schema')=='tiktok4743-backfix-real-apk-v1','Wrong real-APK proof schema')
    validate_input_identities()
    require(proof.get('inputs')==INPUTS and proof.get('unsupported_inputs')==UNSUPPORTED_INPUTS,
            'Unverified, swapped or incorrectly supported original APK identities')
    require(proof.get('physical_android_tested') is False,'No physical Android execution may be claimed')
    require(proof.get('patcher_sdk_sha256')==TOOLS['morphe200.jar'],'Original-APK patcher SDK differs')
    preflight=proof.get('original_preflight',{})
    require(set(preflight)=={'47.4.3'} and preflight['47.4.3'].get('native','').startswith('PASS LegacyNativeAudit original')
            and preflight['47.4.3'].get('oneback','').startswith('PASS TikTok4753ContractAudit original:'),'Original native preflight evidence missing')
    wanted={'4743_'+kind:artifact for kind,artifact in (('standalone_native',SINGLE),('aggregate_settings_ja',COMBINED))}
    cases=proof.get('cases',[])
    require(len(cases)==2 and {c.get('id') for c in cases}==set(wanted),'Both complete supported-version configurations are required')
    for case in cases:
        version,name='47.4.3',wanted[case['id']]
        require(case.get('version')==version and case.get('input_sha256')==ORIGINAL_APK_SHA256,'Case uses wrong version or input APK')
        settings=case['id'].endswith('aggregate_settings_ja')
        selection=NATIVE_PATCHES|{ONEBACK_NAME}
        if settings:selection|={JAPANESE_NAME,'Settings'}
        require(case.get('artifact')==name and case.get('artifact_sha256')==artifacts[name]['sha256'],'Real-APK proof is for different MPP bytes: '+name)
        require(case.get('result')=='PASS' and case.get('steps_all_success') is True and case.get('failed_patches')==[], 'A partial/failed pipeline cannot be published: '+case['id'])
        for field in ('selected_patches','applied_patches'):
            values=case.get(field,[])
            require(len(values)==len(selection) and set(values)==selection,'Missing/extra '+field+' in '+case['id'])
        require(type(case.get('output_apk_bytes')) is int and case['output_apk_bytes']>10_000_000,'Missing real output APK size')
        require(re.fullmatch('[0-9a-f]{64}',case.get('output_apk_sha256','')) is not None and case['output_apk_sha256']!=ORIGINAL_APK_SHA256,'Missing/unchanged output APK identity')
        audits=case.get('audits',{})
        for key in ('native','oneback','integrity'):
            result=audits.get(key)
            require(isinstance(result,str) and result.startswith('PASS ') and '\n' not in result,'Missing successful output audit '+key+' in '+case['id'])
        require(audits['native'].startswith('PASS LegacyNativeAudit output'),'Native emitted hooks/gates proof is missing')
        require(audits['oneback'].startswith('PASS TikTok4753ContractAudit output:'),'OneBack emitted native-hook proof is missing')
        require(audits['integrity'].startswith('PASS APK inventory:'),'Output class inventory proof is missing')
        require(audits.get('privacy')=='PRIVACY_APK_AUDIT_PASSED','Complete privacy native/manifest/DEX audit is missing')
        if settings:
            require(case.get('upstream_version')=='0.9.0-dev.1' and case.get('upstream_sha256')==UPSTREAM_SHA256,'Settings dependency differs from tested upstream')
            require(audits.get('settings_japanese')=='NATIVE_SETTINGS_HOOK_AUDIT_PASSED','Japanese settings native dispatch proof missing')
    rejection=validate_rejection(proof.get('rejection',{}),artifacts)
    integration=validate_integration(proof.get('integration',{}),artifacts)
    limitations=proof.get('limitations',[])
    require(isinstance(limitations,list) and len(limitations)>=2 and all(isinstance(x,str) and x.strip() for x in limitations),'Real-APK proof limitations missing')
    if cfg is not None:
        raw=(SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
        require(sha(raw)==cfg['local_expected_real_apk_sha256']==cfg['expected_real_apk_sha256'],'Real-APK proof changed after freeze')
    return {'original_apk_apply_tested':True,'input_sha256':{v:r['sha256'] for v,r in INPUTS.items()},'cases':[c['id'] for c in cases],
            'exact_tested_mpp_identity_required':True,'supported_versions':['47.4.3'],'removed_versions':['47.5.3'],
            'unsupported_apk_rejection':rejection,'upstream_integration':integration}


def expected_source_archive(repo,qa_raw,real_raw):
    values={name:path.read_bytes() for name,path in release_sources().items()}
    values.update({name:path.read_bytes() for name,path in inherited_sources(repo).items()})
    values['inherited/japanese/ORIGIN.txt']=(f'Original corresponding source for the unchanged Japanese dictionary runtime. The version-restricted loader is in this release src/japanese.\nRepository: https://github.com/{REPO}\nCommit: {JAPANESE_SOURCE_COMMIT}\nPath: {JAPANESE_SOURCE_PATH}\n').encode()
    values[QA_NAME]=qa_raw;values[REAL_QA_NAME]=real_raw
    return values

def validate_local(args):
    cfg=json.loads(args.config.read_text())
    require(cfg.get('schema')=='tiktok4743-backfix-publication-v1' and (cfg.get('patch_version'),cfg.get('bundle_version'),cfg.get('baseline_version'))==(VERSION,BUNDLE,PREVIOUS),'Publication identity mismatch')
    require(cfg.get('supported_versions')==['47.4.3'] and cfg.get('removed_versions')==['47.5.3'],'Publication support scope differs')
    require(cfg.get('freeze_state')=='frozen' and cfg.get('local_build_pair_verified') is True,'Two distinct fresh local builds have not been frozen')
    require(cfg.get('original_apk_apply_tested') is True and cfg.get('physical_android_tested') is False,
            'Publication must declare actual-APK application and untested physical Android status')
    require(cfg.get('original_apk_rejection_tested') is True and cfg.get('integration_139_verified') is True,
            'Publication must include actual removed-version rejection and upstream integration')
    require(sha(args.base.read_bytes())==BASE_SHA256==cfg.get('baseline_sha256') and cfg.get('baseline_url')==BASE_URL,'Unreviewed baseline')
    require(sha(args.upstream.read_bytes())==UPSTREAM_SHA256==cfg.get('upstream_sha256') and cfg.get('upstream_url')==UPSTREAM_URL,'Unreviewed Metra Settings dependency')
    require(inherited_source_ids(args.repo)==cfg['inherited_source_sha256'] and cfg['japanese_source_commit']==JAPANESE_SOURCE_COMMIT,'Corresponding Japanese source differs from freeze')
    payloads={name:(args.dist/name).read_bytes() for name in ASSETS}
    require(set(cfg['expected_mpp'])=={SINGLE,COMBINED},'MPP inventory differs')
    for name,row in cfg['expected_mpp'].items():
        require(row=={'bytes':len(payloads[name]),'sha256':sha(payloads[name])},'MPP differs from frozen tested bytes: '+name)
    require(sha(payloads[QA_NAME])==cfg['expected_qa_sha256']==cfg['local_expected_qa_sha256'],'Build QA changed')
    qa=json.loads(payloads[QA_NAME]);coverage=validate_qa(qa,cfg)
    real_raw=(SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
    require(real_raw==payloads[REAL_QA_NAME],'Published real-APK QA differs from tested source evidence')
    real=validate_real_apk(json.loads(real_raw),qa['artifacts'],cfg)
    old,new,single=map(archive,(args.base.read_bytes(),payloads[COMBINED],payloads[SINGLE]))
    for data,version in ((old,PREVIOUS),(new,BUNDLE),(single,VERSION)):
        require(headers(data[MF])['Version']==version and data['classes.dex'].startswith(b'dex\n'),'MPP manifest/loader identity differs')
    old_types,new_types,single_types=map(dex_class_types,(old['classes.dex'],new['classes.dex'],single['classes.dex']))
    replaced=lambda t:t.startswith(tuple('L'+p for p in OLD_PREFIXES))
    fresh_type=lambda t:t.startswith(tuple('L'+p for p in NEW_PREFIXES))
    ja_types={t for t in single_types if t.startswith('L'+JAPANESE_PREFIX)}
    kept_types={t for t in old_types if not replaced(t)};fresh_types=single_types
    require(ja_types and fresh_types and all(fresh_type(t) for t in fresh_types),'Standalone loader has unexpected namespaces')
    require(new_types==kept_types|fresh_types and not kept_types.intersection(fresh_types),'Aggregate loader replacement changed unrelated definitions')
    proof=re.fullmatch(r'PASS loader merge: preserved=(\d+); replaced=(\d+); new=(\d+); standalone=(\d+); japanese=(\d+)',qa['loader_preservation'])
    require(tuple(map(int,proof.groups()))==(len(kept_types),len(old_types-kept_types),len(fresh_types),len(single_types),len(ja_types)),'Loader semantic proof does not describe shipped DEX')
    retained={n:r for n,r in old.items() if n not in (MF,'classes.dex',ONEBACK_EXT,NATIVE_EXT) and not n.startswith(OLD_PREFIXES)}
    require(len(retained)==qa['preserved_unrelated_entries'] and all(new.get(n)==r for n,r in retained.items()),'Existing unrelated resource or classfile changed')
    own=lambda n:n.startswith(NEW_PREFIXES) or n in (ONEBACK_EXT,NATIVE_EXT) or n.startswith(NOTICE_PREFIX)
    require(all(n in old or own(n) for n in new),'Unexpected aggregate entry')
    carry=lambda n:n.startswith('META-INF/hiro-tiktok-japanese/') or n==JAPANESE_EXT or re.fullmatch(r'upgrades/oneback-runtime[0-9]+\.dex',n) is not None
    carried={n:r for n,r in old.items() if carry(n)}
    require(all(single.get(n)==r for n,r in carried.items()),'Standalone omitted or changed inherited Japanese/upgrade assets')
    require(all(own(n) or n in carried or n in (MF,'classes.dex') for n in single),'Unexpected standalone asset')
    require(all(new.get(n)==r for n,r in single.items() if own(n)),'Standalone/aggregate authored bytes differ')
    for key,name in (('oneback',ONEBACK_EXT),('native',NATIVE_EXT),('japanese',JAPANESE_EXT)):
        require(single[name]==new[name] and sha(single[name])==qa['runtime_sha256'][key],'Runtime bytes differ: '+key)
    before_runtime,after_runtime=map(dex_class_types,(old[ONEBACK_EXT],new[ONEBACK_EXT]))
    runtime_report=runtime_audit(qa['oneback_runtime_preservation'])
    require(before_runtime <= after_runtime and after_runtime-before_runtime==set(runtime_report['added']),
            'OneBack runtime added/removed classes differ from the preservation audit')
    require(set(runtime_report['changed']) <= before_runtime
            and runtime_report['preserved']+len(runtime_report['changed'])==len(before_runtime),
            'OneBack preserved class count does not describe shipped DEX')
    require(dex_class_types(single[NATIVE_EXT]) and all(t.startswith('Lapp/hiro/tiktok4753/runtime/') for t in dex_class_types(single[NATIVE_EXT])),'Native helper namespace differs')
    require(single[JAPANESE_EXT]==old[JAPANESE_EXT],'Japanese runtime was modified')
    for name in ('LICENSE','NOTICE'): require(single.get(NOTICE_PREFIX+name)==(SOURCE_ROOT/name).read_bytes(),'License/NOTICE changed')
    require(set(n for n in single if n.startswith(NOTICE_PREFIX))=={NOTICE_PREFIX+n for n in ('LICENSE','NOTICE','ORIGIN.txt')},'Authored notices incomplete')
    require(single[NOTICE_PREFIX+'ORIGIN.txt'].strip(),'Empty origin notice')
    require(archive(payloads[SOURCE_ZIP])==expected_source_archive(args.repo,payloads[QA_NAME],real_raw),'Corresponding source archive incomplete or changed')
    notes=payloads['RELEASE_NOTES.txt'].decode();require(notes==(PUBLICATION/'RELEASE_NOTES.txt').read_text(),'Release notes changed')
    require(all(s in notes for s in ('47.4.3','47.5.3','対応対象から削除','実APK','未確認','戻る','Settings','TikTok設定を日本語化'))
            and not any(HEADING.match(line) for line in notes.splitlines()),'Notes lack native Back verification, removed support, limitations or exact Settings guidance')
    checksums={}
    for line in payloads['SHA256SUMS.txt'].decode().splitlines():
        digest,name=line.split('  ',1);require(name in payloads and name not in checksums and sha(payloads[name])==digest,'Asset checksum differs: '+name);checksums[name]=digest
    require(set(checksums)==set(ASSETS)-{'SHA256SUMS.txt'},'Checksum inventory differs')
    report={'baseline_bundle_version':PREVIOUS,'baseline_bundle_sha256':BASE_SHA256,
        'upstream_version':'0.9.0-dev.1','upstream_sha256':UPSTREAM_SHA256,'android_device_tested':False,
        'original_apk_apply_tested':True,'original_apk_rejection_tested':True,
        'supported_versions':['47.4.3'],'removed_versions':['47.5.3'],
        'original_apk_sha256':{v:r['sha256'] for v,r in INPUTS.items()},
        'unrelated_resource_entries_byte_identical':len(retained),'unrelated_loader_classes_unchanged':len(kept_types),
        'oneback_runtime_classes_unchanged':runtime_report['preserved'],
        'oneback_changed_runtime_classes':runtime_report['changed'],'oneback_added_runtime_classes':runtime_report['added'],
        'japanese_runtime_byte_identical':True,'japanese_loader_version_restricted':True,
        'two_fresh_local_builds_mpp_qa_byte_identical':True,'two_fresh_ci_builds_required':True,
        'two_fresh_ci_builds_mpp_qa_byte_identical':cfg.get('ci_build_pair_verified') is True,
        'local_sdk_build':True,'local_host_tested':True,'validation_coverage':coverage,'real_apk_validation':real,
        'qa_sha256':sha(payloads[QA_NAME]),'real_apk_qa_sha256':sha(real_raw)}
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
                "User-Agent": "Hiro-TikTok4743-Publication",
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
    with tempfile.TemporaryDirectory(prefix="tiktok4743-index-") as temporary:
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
            "Active bundle is no longer the reviewed baseline281")
    require(utc_date(created) > utc_date(old["created_at"]), "created_at must advance")
    previous_description = old["description"]
    description = notes.strip() + "\n\n" + previous_description
    feed = {**old, "version": BUNDLE, "created_at": created, "download_url": url,
            "page_url": PAGE, "signature_download_url": "", "description": description}
    bullets = "\n".join(f"* **{app}:** 47.4.3戻る修正・47.5.3対応削除 v{VERSION}：{cfg['summary']}" for app in APP_SCOPES)
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
    require(sha(fetch(BASE_URL)) == BASE_SHA256, "Public baseline281 bytes differ")
    require(sha(fetch(UPSTREAM_URL)) == UPSTREAM_SHA256, "Public Metra dependency bytes differ")
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
    receipt = {"schema": "tiktok4743-backfix-publication-receipt-v1", "patch_version": VERSION,
               "bundle_version": BUNDLE,
               "baseline_bundle_version": PREVIOUS, "baseline_bundle_sha256": BASE_SHA256,
               "upstream_sha256": UPSTREAM_SHA256, "upstream_url": UPSTREAM_URL,
               "source_commit": os.environ.get("GITHUB_SHA") or git(args.repo, "rev-parse", "HEAD"),
               "publication_manifest_sha256": sha(args.config.read_bytes()),
               "assets": assets, "local_validation": report, "android_device_tested": False,
               "original_apk_apply_tested": report["original_apk_apply_tested"], "manager_parser_source": MANAGER_SOURCE,
               "original_apk_rejection_tested": True, "supported_versions": ["47.4.3"], "removed_versions": ["47.5.3"],
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
            "release: exact TikTok 47.4.3 v4.2.0 and bundle282 immutable MPP downloads")
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
            # Mirror only this reviewed release's complete authored source and notices.
            for relative, path in release_sources().items():
                updates[branch][RELEASE_ROOT + "/" + relative] = path.read_bytes()
            for name in (QA_NAME, REAL_QA_NAME, "RELEASE_NOTES.txt", "SHA256SUMS.txt"):
                updates[branch][RELEASE_ROOT + "/" + name] = payloads[name]
        active = commit_feeds(args.repo, states, updates, "release: TikTok 47.4.3 native Back fix, 47.5.3 rejection and bundle282 Manager update")
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
                public_feed=json.loads(fetch(prefix+"/patches-bundle.json?tiktok4743="+nonce))
                public_log=fetch(prefix+"/CHANGELOG.md?tiktok4743="+nonce).decode()
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
            "docs: verified TikTok 47.4.3 v4.2.0 publication receipt")
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
    parser.add_argument("--upstream", type=Path, required=True)
    parser.add_argument("--config", type=Path, default=PUBLICATION / "manifest.json")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--local-only", action="store_true")
    mode.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("dist", "repo", "base", "upstream", "config"):
        setattr(args, key, getattr(args, key).resolve())
    publication(args)



if __name__ == '__main__': main()
