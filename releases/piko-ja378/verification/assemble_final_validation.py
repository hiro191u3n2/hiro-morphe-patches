#!/usr/bin/env python3
"""Bind completed actual APK/resource/DEX evidence to the delivered MPP hashes."""
import hashlib
import json
import re
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
WORKSPACE = ROOT.parent
AAPT = Path('/workspace/scratch/80dcfb9aa48e/inputs/build-tools37/android-37.0/aapt2')
CASES = [('dev1', '3.12.0-dev.1', 50), ('dev2', '3.12.0-dev.2', 50),
         ('dev2-clone', '3.12.0-dev.2', 51), ('standalone', '3.12.0-dev.2', 2)]


def read(path):
    return json.loads(path.read_text())


def sha(path):
    with path.open('rb') as f:
        return hashlib.file_digest(f, 'sha256').hexdigest()


def main():
    build = read(ROOT/'dist/build-evidence.json')
    component = read(ROOT/'patch/QA.json')
    assert build['result'] == component['result'] == 'PASS'
    for name, record in build['artifacts'].items():
        p = ROOT/'dist'/name
        assert p.stat().st_size == record['bytes'] and sha(p) == record['sha256'], 'Artifact changed: '+name
    assert sha(ROOT/'patch/component.mpp') == component['component_sha256'] == build['component_sha256']
    resources, dex, applications = {}, {}, {}
    for name, version, count in CASES:
        apk = HERE/(name+'.apk')
        digest = sha(apk)
        cli = read(HERE/(name+'-result.json'))
        assert cli['success'] and not cli['failedPatches']
        assert len(cli['appliedPatches']) == count
        assert all(step['success'] for step in cli['stepResults'])
        assert {step['step'] for step in cli['stepResults']} >= {'PATCHING', 'REBUILDING'}
        resource = read(HERE/(name+'-resources.json'))
        code = read(HERE/(name+'-localized-dex.json'))
        for report in (resource, code):
            assert report['result'] == 'PASS' and not report['failures']
            assert report['apk_sha256'] == digest, 'Audit bound to a different APK: '+name
        assert len(resource['checks']) == 1322 and all(c['matches'] for c in resource['checks'])
        assert code['actual_lookup_sites'] == code['expected_lookup_sites'] == code['exact_target_rows'] == 224
        assert code['target_classes'] == 30
        badging = subprocess.run([str(AAPT), 'dump', 'badging', str(apk)], check=True,
                                 text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout
        match = re.search(r"^package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging, re.M)
        assert match, 'Compiled manifest identity unavailable: '+name
        package, version_code, version_name = match.groups()
        assert version_name == '12.43.0-prod.01'
        assert (package != 'com.twitter.android') if name == 'dev2-clone' else (package == 'com.twitter.android')
        artifact = 'Piko_NewX_Japanese_v1.0.0.mpp' if name == 'standalone' else 'Hiro_Morphe_Patches_v1.0.378.mpp'
        sources = [{'file':artifact,'sha256':build['artifacts'][artifact]['sha256']},
                   {'file':'piko-'+version+'.mpp','sha256':sha(WORKSPACE/'inputs'/('piko-'+version+'.mpp'))}]
        if name == 'dev1':
            sources.reverse()
        applications[name] = {'result':'PASS','patch_count':count,'piko_version':version,
                              'apk_sha256':digest,'apk_bytes':apk.stat().st_size,
                              'manifest_package':package,'manifest_version_name':version_name,
                              'manifest_version_code':version_code,'ordered_patch_sources':sources,
                              'unsigned':True,'cli_result':cli}
        resources[name], dex[name] = resource, code
    preservation = read(HERE/'dev1-preservation.json')
    assert preservation['result'] == 'PASS' and not preservation['failures']
    assert preservation['apk_sha256'] == applications['dev1']['apk_sha256']
    sources = [HERE/n for n in ('audit_resources.py','AuditLocalizedDex.java','run_completed_apk_audits.py',
                               'audit_preservation.py','CompareApk.java','NormalizeClassDiff.java',
                               'ReviewNewXApk.java','assemble_final_validation.py')]
    final = {'schema':'hiro-piko-newx-japanese-final-validation-v1','result':'PASS',
             'target':{'package':'com.twitter.android','version':'12.43.0-prod.01'},
             'device_tested':False,'physical_device_tested':False,'emulator_tested':False,
             'artifacts':build['artifacts'],'build':build,'component_qa':component,
             'application_tests':applications,'compiled_resources':{'result':'PASS','cases':resources},
             'final_dex_localization':{'result':'PASS','cases':dex},
             'apk_class_preservation':preservation,
             'verification_source_sha256':{p.name:sha(p) for p in sources},
             'limitations':['No physical Android device or emulator test; checks verify packaged artifacts statically.',
                            'Language change requires process restart for cached static translation labels.',
                            'Default English values are preserved; intentionally untranslated API names, internal identifiers and data-dependent Image/Video labels remain.',
                            'The separate Piko patch-source names/descriptions shown by Manager are outside this APK translation.']}
    path = HERE/'final-validation.json'
    path.write_text(json.dumps(final, ensure_ascii=False, indent=2)+'\n')
    (HERE/'completed-apk-audits.json').write_text(json.dumps({
        'compiled_resources': final['compiled_resources'],
        'final_dex_localization': final['final_dex_localization'],
    }, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'result':final['result'],'cases':list(applications),
                      'exact_resource_checks_per_case':1322,'localized_lookups_per_case':224,
                      'allowed_changed_classes':len(preservation['localized_changed_classes']),
                      'final_validation_sha256':sha(path)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
