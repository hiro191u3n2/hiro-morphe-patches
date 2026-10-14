#!/usr/bin/env python3
"""Fail-closed publication gate for exact .95 artifacts and fresh host/APKS evidence.

This is a desktop/build gate. It cannot claim Android GPU speed, first-shot GPU
execution, or device quality from software GLES and original-APKS application.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, re, stat, zipfile
from build1995 import (ROOT, VERSION, BUNDLE_VERSION, SINGLE, BUNDLE, BASE_PINS,
    GPU_ENTRY, CHANGED, TOOL_PINS, source_pins, publication_pins, production_path,
    headers, dex_integrity, sha, require, json_bytes, runtime_source_version)
from finalize1995 import QA_NAME, SOURCE_ZIP, SELECTED
import application_evidence1995
import native_identity1995

ASSETS=(SINGLE,BUNDLE,QA_NAME,SOURCE_ZIP,'RELEASE_NOTES.txt','SHA256SUMS.txt')
FALSE_QA=('device_tested','physical_android_tested','device_speed_measured','device_quality_verified',
    'gpu_execution_on_physical_android','first_shot_gpu_verified_on_physical_android',
    'samsung_gpu_failure_root_cause_confirmed','all_processing_on_gpu','capture_fusion_enabled')
REQUIRED_HOST_GROUPS=frozenset(('logcopy95','shutter94','front94','queue95','runtime_version','fingerprint','release_gate','startup','strong_timing95',
    'timing','route_failure92','memory','gpu_math','strong_adaptive94','strong_transport95','model_layout94','native91',
    'finish_memory92','chain','finish_domain','pipeline_gate','failure92','finish_recovery95'))

def checked_name(name):
    path=PurePosixPath(name)
    require(isinstance(name,str) and bool(name) and not path.is_absolute() and '..' not in path.parts
        and '\\' not in name and str(path)==name and not name.endswith('/'),'Unsafe archive/source path: '+str(name))
    return path

def checked_archive(path):
    with zipfile.ZipFile(path) as archive:
        names=archive.namelist();require(len(names)==len(set(names)),'Duplicate ZIP member: '+str(path))
        for item in archive.infolist():
            checked_name(item.filename.rstrip('/'))
            require(not stat.S_ISLNK(item.external_attr>>16),'ZIP symlink: '+item.filename)
            require(not item.flag_bits&1,'Encrypted release member: '+item.filename)
        require(archive.testzip() is None,'ZIP CRC failure: '+str(path))
        return {item.filename:archive.read(item) for item in archive.infolist() if not item.is_dir()}

def pin_files(base,pins):
    require(isinstance(pins,dict) and bool(pins),'Empty source pin set')
    for name,digest in pins.items():
        checked_name(name);path=base/name
        require(re.fullmatch('[a-f0-9]{64}',digest or '') is not None,'Malformed source SHA: '+name)
        require(path.is_file() and not path.is_symlink() and sha(path.read_bytes())==digest,'Source bytes changed: '+name)

def positive_host(report):
    require(isinstance(report,dict) and report.get('status')=='passed','Current host suite did not pass')
    require(type(report.get('assertions')) is int and report['assertions']>0,'No executed host assertions')
    require(report.get('physical_android_tested') is False,'Host report claims physical Android')
    require(report.get('device_speedup_verified',False) is False,'Host report claims device speed')
    tests=report.get('tests');require(isinstance(tests,dict) and bool(tests),'Host groups absent')
    require(set(tests)==REQUIRED_HOST_GROUPS,'Required executed host group inventory differs')
    total=0
    for name,child in tests.items():
        require(isinstance(child,dict) and child.get('status')=='passed' and type(child.get('assertions')) is int
            and child['assertions']>0,'Incomplete host group: '+name)
        require(child.get('physical_android_tested',False) is False,'Host group claims Android execution: '+name)
        total+=child['assertions']
    require(total==report['assertions'],'Host assertion total differs from executed groups')
    require(tests['finish_recovery95'].get('resident_policy_trace_verified') is True,'Resident GPU finish policy trace missing')
    require(tests['release_gate'].get('native_identity_only_inverse_verified') is True and type(tests['release_gate'].get('native_identity_negative_assertions')) is int and tests['release_gate']['native_identity_negative_assertions']>0,'Native identity corruption controls missing')

def validate(args):
    root=args.root.resolve();dist=root/'dist';source=root/'src'
    require(source.resolve()==ROOT.resolve(),'Validator must run from this release source graph')
    require(runtime_source_version()==VERSION,'Current timing source version differs')
    declaration=json.loads((root/'manifest.json').read_text())
    build=json.loads((dist/'build1995.json').read_text())
    qa=json.loads((dist/QA_NAME).read_text());host=json.loads(args.host_report.read_text())
    require(declaration.get('schema')=='ulike1995-build-declaration-v1','Wrong build declaration')
    require(build.get('schema')=='ulike1995-build-evidence-v1','Wrong build evidence')
    require(qa.get('schema')=='ulike1995-pipeline-release-qa-v1' and
        qa.get('status')=='HOST_AND_ORIGINAL_APKS_VERIFIED_DEVICE_UNVERIFIED','Wrong final QA identity')
    for data in (declaration,build,qa):
        require(data.get('ulike_version')==VERSION and data.get('bundle_version')==BUNDLE_VERSION,'Release version drift')
        require(data.get('baseline')==BASE_PINS,'Published baseline pins changed')
    require(declaration.get('tool_sha256')==TOOL_PINS==build.get('tool_sha256')==qa.get('tool_sha256'),'Pinned build toolchain differs')
    require(qa.get('selected_candidates')==SELECTED,'Adopted candidate inventory changed')
    require(qa.get('host_tests_passed') is True and qa.get('original_apk_apply_tested') is True,'Final evidence gate incomplete')
    for key in FALSE_QA:require(qa.get(key) is False,'Unverified physical capability claimed: '+key)
    require(declaration.get('runtime_version')==build.get('runtime_version')==qa.get('runtime_version')==VERSION and declaration.get('timing_version_matches_release') is True and build.get('timing_version_matches_release') is True and qa.get('timing_version_matches_release') is True,'Timing runtime/package versions differ')
    require(build.get('structural_inventory',{}).get('runtime_version')==VERSION and build['structural_inventory'].get('timing_version_matches_release') is True,'Emitted DEX timing version missing')
    require(build.get('device_tested') is False and build.get('device_speed_measured') is False
        and build.get('device_quality_verified') is False,'Build claims physical verification')
    pins=source_pins();pubpins=publication_pins();pin_files(source,pins);pin_files(root/'publication',pubpins)
    require(declaration.get('reviewed_source_sha256')==pins==build.get('source_sha256')==qa.get('source_sha256'),
        'Reviewed/built/current source graph differs')
    require(declaration.get('reviewed_publication_sha256')==pubpins==build.get('publication_sha256')==qa.get('publication_sha256'),
        'Reviewed/built/current publisher graph differs')
    roots=declaration.get('production_helper_roots')
    require(isinstance(roots,list) and roots and roots==sorted(set(roots)),'Production root inventory invalid')
    compiled={production_path(name).relative_to(source).as_posix():sha(production_path(name).read_bytes()) for name in roots}
    require(build.get('production_helper_roots')==roots and qa.get('production_helper_roots')==roots and
        build.get('compiled_production_sha256')==compiled==qa.get('compiled_production_sha256'),'Compiled root source hashes differ')
    for key in ('runtime_helper_references_verified','deterministic_dex_verified','other_apps_preserved'):
        require(build.get(key) is True and qa.get(key) is True,'Build contract absent: '+key)
    require(host.get('schema')=='ulike1995-host-audit-v1' and host.get('source_stability_verified') is True,
        'Current stable host source graph was not executed')
    require(host.get('current_source_sha256')==pins and host.get('current_publication_sha256')==pubpins,
        'Host tests ran against different source/publication bytes')
    positive_host(host)
    require(qa.get('host_evidence')==host and qa.get('host_report_sha256')==sha(args.host_report.read_bytes()),'QA embeds different host evidence')
    artifacts={name:{'sha256':sha((dist/name).read_bytes()),'bytes':(dist/name).stat().st_size} for name in ASSETS}
    mpps={name:artifacts[name] for name in (SINGLE,BUNDLE)}
    require(mpps==build.get('artifacts')==qa.get('artifacts'),'Built/applied/published MPP bytes differ')
    if 'reviewed_mpp_artifacts' in declaration:require(declaration['reviewed_mpp_artifacts']==mpps,'CI rebuilt different MPP bytes')
    application=application_evidence1995.read(source,mpps,report_path=args.application_report)
    require(qa.get('original_application')==application,'QA uses different original APKS application')
    hook=build.get('stock_shutter_hook',{})
    require(hook==qa.get('stock_shutter_hook') and hook.get('added_method_count')==0 and hook.get('existing_methods_preserved') is True and hook.get('byte_identical_to_baseline') is True,'Inherited stock method byte identity missing')
    require(hook.get('target')=='Lcom/light/beauty/mc/preview/shutter/module/ShutterButton;->onTouchEvent(Landroid/view/MotionEvent;)Z' and hook.get('original_sha256')=='ba56099d88ea1dbdab32dab9302393ddc9f2aae0e5f9900574b2551f973b69c1','Shutter target/original contract differs')
    require({k:application['report']['stock_shutter_hook'].get(k) for k in ('target','original_sha256','patched_sha256')}=={k:hook.get(k) for k in ('target','original_sha256','patched_sha256')},'Applied shutter hook differs from built payload')

    packages={name:checked_archive(dist/name) for name in (SINGLE,BUNDLE)}
    for name,version in ((SINGLE,VERSION),(BUNDLE,BUNDLE_VERSION)):
        members=packages[name];require(headers(members['META-INF/MANIFEST.MF']).get('Version')==version,'Wrong MPP version: '+name)
        for dex in ('classes.dex','ulike/runtime.dex','ulike/methods.dex'):dex_integrity(members[dex],name+'/'+dex)
        require(sha(members[GPU_ENTRY])==build['gpu_native']['sha256'] and len(members[GPU_ENTRY])==build['gpu_native']['bytes'],'GPU payload differs from built JNI')
        identity=native_identity1995.verify(members[GPU_ENTRY],native_identity1995.current_fingerprint(source))
        identity.update(native_identity1995.verify_native_sources(source))
        require(build['gpu_native'].get('identity_delta')==identity==qa['gpu_native'].get('identity_delta'),'Exact native identity-only inverse proof differs')
        shutter_rows=[line.split('\t') for line in members['ulike/methods.tsv'].decode().splitlines() if line.split('\t')[0]==hook['target']]
        require(shutter_rows==[[hook['target'],hook['original_sha256'],hook['patched_sha256']]],'Published shutter TSV differs from built/applied proof')
        for inherited,digest in build.get('inherited_native_sha256',{}).items():
            require(inherited in members and sha(members[inherited])==digest,'Inherited native payload changed: '+inherited)
    own=lambda name:name.startswith('ulike') or name.startswith('app/hiro/ulike/')
    require({n:v for n,v in packages[SINGLE].items() if own(n)}=={n:v for n,v in packages[BUNDLE].items() if own(n)},'Standalone/bundle ULike resources differ')
    require(build.get('gpu_native',{}).get('build')=='rebuilt_from_current_sources','GPU payload provenance missing')
    require(build.get('resource_delta')=={'standalone':sorted(CHANGED),'bundle':sorted(CHANGED)},'Exact approved resource changes missing')
    require(len(build.get('inherited_native_sha256',{}))==11,'Inherited native payload inventory differs')
    require(build.get('resource_delta')==qa.get('resource_delta') and build.get('structural_inventory')==qa.get('structural_inventory'),'QA resource/type inventory drift')
    expected_sums=''.join(artifacts[name]['sha256']+'  '+name+'\n' for name in sorted(ASSETS[:-1]))
    require((dist/'SHA256SUMS.txt').read_text()==expected_sums,'SHA256SUMS incomplete or inconsistent')
    require(json.loads((dist/'release-artifacts1995.json').read_text())==artifacts,'Finalizer release inventory differs')
    archive=checked_archive(dist/SOURCE_ZIP)
    expected={'manifest.json':(root/'manifest.json').read_bytes(),QA_NAME:(dist/QA_NAME).read_bytes(),
        'RELEASE_NOTES.txt':(dist/'RELEASE_NOTES.txt').read_bytes(),'evidence/host-audit1995.json':args.host_report.read_bytes(),
        'evidence/original-application1995.json':args.application_report.read_bytes(),'evidence/build1995.json':(dist/'build1995.json').read_bytes()}
    expected.update({'src/'+name:(source/name).read_bytes() for name in pins})
    expected.update({'publication/'+name:(root/'publication'/name).read_bytes() for name in pubpins})
    require(set(archive)==set(expected),'Source ZIP has missing/unexpected/private members')
    for name,raw in archive.items():
        checked_name(name)
        require(PurePosixPath(name).suffix.lower() not in ('.apk','.apks','.apkm','.aab','.png','.jpg','.jpeg','.heic','.heif','.mp4'),
            'Private input or generated APK in source ZIP: '+name)
        require(raw==expected[name],'Source/evidence ZIP bytes differ: '+name)
    result={'schema':'ulike1995-release-validation-v1','status':'passed','ulike_version':VERSION,'bundle_version':BUNDLE_VERSION,
        'release_artifacts':artifacts,'host_tests_passed':True,'host_assertions':host['assertions'],
        'host_report_sha256':sha(args.host_report.read_bytes()),'original_apk_apply_tested':True,
        'original_application_report_sha256':application['report_sha256'],'source_consistency_verified':True,
        'archive_inventory_verified':True,'no_original_apks_or_private_images_distributed':True,
        'standalone_bundle_ulike_identical':True,'device_tested':False,'physical_android_tested':False,
        'device_speed_measured':False,'device_quality_verified':False,'first_shot_gpu_verified_on_physical_android':False}
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_bytes(json_bytes(result));return result
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('root','host-report','application-report','output'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();print(json.dumps(validate(args),ensure_ascii=False,indent=2))
