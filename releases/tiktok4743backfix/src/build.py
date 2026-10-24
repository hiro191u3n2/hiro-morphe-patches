#!/usr/bin/env python3
"""Build authored TikTok 47.4.3 patches with native Back regression checks.

The deterministic build validates loader, runtime preservation and host regressions.
Real original-APK application and native call-site checks run separately with
validate_apk.py; neither stage asserts physical Android device operation.
"""
from pathlib import Path
import argparse, json, os, re, subprocess, sys
ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from bundle_tools import *

VERSION, BUNDLE_VERSION, BASE_VERSION = '4.2.0', '1.0.282', '1.0.281'
BASE_SHA = '22020c9ecfa22aaaf5ec089e30f266f1f77bf9384d129105459bde23cc4d4dab'
SINGLE = 'TikTok_47.4.3_Hiro_v4.2.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.282.mpp'
QA = 'QA_TikTok_47.4.3_v4.2.0.json'
MF = 'META-INF/MANIFEST.MF'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'morphe22.jar': 'fe4bc92815bb14976150e4ec83e4985932b46d1d47edec75d37604481572e864',
    'morphe200.jar': 'deb5d931f3899b20a119637473d338c52585f0c561b5d8b2856b916fe90163b5',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}
OLD_PREFIXES = ('app/hiro/oneback/patches/', 'app/hiro/tiktok4753/patches/', 'app/hiro/tiktokjapanese/patches/')
NEW_PREFIX = 'app/hiro/tiktok4753/patches/'
JAPANESE_PREFIX = 'app/hiro/tiktokjapanese/patches/'
ONEBACK_EXT = 'extensions/oneback_exit.mpe'
NATIVE_EXT = 'extensions/hiro_tiktok4753.mpe'
JAPANESE_EXT = 'extensions/hiro_tiktok_japanese.mpe'

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base','tools','work','dist'):
        parser.add_argument('--'+name, type=Path, required=True)
    args = parser.parse_args()
    for key in vars(args):
        setattr(args, key, getattr(args, key).resolve())
    require(sha(args.base) == BASE_SHA, 'Reviewed baseline281 changed')
    for name, digest in TOOLS.items():
        require(sha(args.tools/name) == digest, 'Pinned tool changed: '+name)
    require(not args.work.exists(), 'Use a fresh build directory')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'Use an empty dist directory')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)
    sources = source_ids(ROOT)
    def run(command, label, timeout=300):
        print('RUN '+label, flush=True)
        with (args.work/label).open('w') as log:
            result = subprocess.run(list(map(str,command)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        output = (args.work/label).read_text()
        require(result.returncode == 0, 'Failed '+label+'\n'+output[-10000:])
        return output.strip()
    require('Temurin-21.0.8+9' in run(['java','--version'],'java.txt'), 'Pinned JDK required')
    require(run(['javac','-version'],'javac.txt') == 'javac 21.0.8', 'Pinned javac required')
    for name in ('patch','oneback-runtime','native-runtime','oneback-host','oneback-host4743','oneback-host4753','native-host','audit','patch-dex','oneback-dex','native-dex'):
        (args.work/name).mkdir()
    patch_sources = sorted((ROOT/'oneback/patch').glob('*.java')) + sorted((ROOT/'legacy/patch').glob('*.java')) + sorted((ROOT/'japanese/patch').glob('*.java'))
    require(patch_sources and (ROOT/'legacy/patch/TikTok4753LegacyPatches.java').exists(), 'Native implementations missing')
    run(['javac','--release','8','-encoding','UTF-8','-cp',args.tools/'morphe.jar','-d',args.work/'patch',*patch_sources], 'patch-javac.log')
    for area, destination in (('oneback','oneback-runtime'),('legacy','native-runtime')):
        runtime_sources = sorted((ROOT/area/'runtime').glob('*.java'))
        require(runtime_sources, 'Runtime sources missing: '+area)
        run(['javac','--release','8','-encoding','UTF-8','-cp',args.tools/'android.jar','-d',args.work/destination,*runtime_sources], destination+'-javac.log')
    hostcp = os.pathsep.join(map(str,[args.work/'oneback-host', args.work/'oneback-runtime',args.tools/'android.jar']))
    modern_names = {p.stem for p in (ROOT/'oneback/test-runtime').glob('Back*.java')
                    if p.stem.startswith(('Back4743', 'Back4753')) and 'public static void main' in p.read_text()}
    classic_tests = [p for p in sorted((ROOT/'oneback/test-runtime').glob('*.java')) if p.stem not in modern_names]
    run(['javac','--release','8','-encoding','UTF-8','-cp',hostcp,'-d',args.work/'oneback-host',
         *sorted((ROOT/'oneback/test-stubs').rglob('*.java')), *classic_tests], 'oneback-host-javac.log')
    host = {}
    for source in sorted((ROOT/'oneback/test-runtime').glob('*.java')):
        if source.stem in modern_names or 'public static void main' not in source.read_text():
            continue
        output = run(['java','-ea','-cp',hostcp,'app.hiro.oneback.runtime.'+source.stem], source.stem+'.txt')
        require('PASS' in output or 'SUCCESS:' in output or re.search(r'REGRESSION: [0-9]+ cases, 0 failed',output), 'Missing host success marker')
        host[source.stem] = output.splitlines()[-1]
    for abi in ('4743', '4753'):
        folder = args.work/('oneback-host'+abi)
        names = sorted(name for name in modern_names if name.startswith('Back'+abi))
        moderncp = os.pathsep.join(map(str,[folder,args.work/'oneback-host',args.work/'oneback-runtime',args.tools/'android.jar']))
        run(['javac','--release','8','-encoding','UTF-8','-cp',moderncp,'-d',folder,
             *sorted((ROOT/('oneback/test-stubs'+abi)).rglob('*.java')),*[ROOT/'oneback/test-runtime'/(name+'.java') for name in names]], 'oneback-host'+abi+'-javac.log')
        for name in names:
            modern = run(['java','-ea','-cp',moderncp,'app.hiro.oneback.runtime.'+name],name+'.txt')
            require(modern.splitlines()[-1].startswith('PASS '+name), 'Native ABI regression did not pass: '+abi)
            host[name] = modern.splitlines()[-1]
    require({'Back117TikTokTest', *modern_names} <= host.keys(), 'TikTok legacy/new host regression missing')
    auditcp = os.pathsep.join(map(str,[args.work/'audit',args.work/'patch',args.tools/'morphe.jar']))
    run(['javac','--release','8','-encoding','UTF-8','-cp',auditcp,'-d',args.work/'audit',*sorted((ROOT/'test').glob('*.java'))], 'audit-javac.log')
    run(['javac','--release','8','-encoding','UTF-8','-cp',auditcp,'-d',args.work/'audit',
         ROOT/'legacy/test/LegacyPrivacyTest.java'], 'privacy-host-javac.log')
    run(['javac','--release','8','-encoding','UTF-8','-cp',auditcp,'-d',args.work/'audit',
         ROOT/'japanese/test/TikTokTargetAdmissionTest.java'], 'admission-host-javac.log')
    legacy_tests = {}
    legacy_tests['privacy'] = run(['java','-ea','-cp',auditcp,'app.hiro.tiktok4753.patches.LegacyPrivacyTest'],'privacy-host.txt')
    require(legacy_tests['privacy'].startswith('PASS'), 'Privacy policy regression failed')
    admission = []
    for sdk in ('morphe.jar','morphe22.jar','morphe200.jar'):
        admissioncp = os.pathsep.join(map(str,[args.work/'audit',args.work/'patch',args.tools/sdk]))
        result = run(['java','-ea','-cp',admissioncp,'TikTokTargetAdmissionTest'],sdk+'.admission.txt')
        require(result.startswith('PASS TikTokTargetAdmissionTest checks='), 'Version admission failed '+sdk)
        admission.append({'sdk':sdk,'result':result})
    run(['javac','--release','8','-encoding','UTF-8','-d',args.work/'native-host',
         *sorted((ROOT/'legacy/test-stubs').rglob('*.java')),
         *sorted((ROOT/'legacy/runtime').glob('*.java')),
         ROOT/'legacy/test/LegacyHooksHostTest.java'], 'native-host-javac.log')
    legacy_tests['ui_runtime'] = run(['java','-ea','-cp',args.work/'native-host','LegacyHooksHostTest'],'native-host.txt')
    require(legacy_tests['ui_runtime'].startswith('PASS LegacyHooksHostTest'), 'Native UI lifecycle regression failed')
    for kind, output, api in (('patch','patch-dex','26'),('oneback-runtime','oneback-dex','29'),('native-runtime','native-dex','26')):
        write_zip(args.work/(kind+'.jar'), class_files(args.work/kind))
        run(['java','-cp',args.tools/'d8.jar','com.android.tools.r8.D8','--release','--min-api',api,'--lib',args.tools/'android.jar',
             '--classpath',args.tools/('morphe.jar' if kind=='patch' else 'android.jar'),'--output',args.work/output,args.work/(kind+'.jar')], kind+'-d8.log')
        require({p.name for p in (args.work/output).iterdir()} == {'classes.dex'}, 'Unexpected multidex: '+kind)
    base = archive(args.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline manifest differs')
    (args.work/'baseline-loader.dex').write_bytes(base['classes.dex'])
    (args.work/'baseline-oneback.dex').write_bytes(base[ONEBACK_EXT])
    merge = run(['java','-ea','-cp',auditcp,'BundleAudit','merge',args.work/'baseline-loader.dex',args.work/'patch-dex/classes.dex',
                 args.work/'combined.dex',args.work/'single.dex'], 'loader-preservation.txt')
    runtime = run(['java','-ea','-cp',auditcp,'BundleAudit','runtime',args.work/'baseline-oneback.dex',
                   args.work/'oneback-dex/classes.dex',args.work/'verified-oneback.dex'], 'runtime-preservation.txt')
    fresh = {**class_files(args.work/'patch'), ONEBACK_EXT:(args.work/'verified-oneback.dex').read_bytes(),
             NATIVE_EXT:(args.work/'native-dex/classes.dex').read_bytes()}
    carried = {name:raw for name,raw in base.items() if name.startswith('META-INF/hiro-tiktok-japanese/') or name == JAPANESE_EXT
               or re.fullmatch(r'upgrades/oneback-runtime[0-9]+\.dex',name)}
    require(any(name.startswith(JAPANESE_PREFIX) for name in fresh), 'Restricted Japanese loader absent')
    require(JAPANESE_EXT in carried, 'Unchanged Japanese dictionary runtime absent')
    for name in ('LICENSE','NOTICE'):
        fresh['META-INF/hiro-tiktok4743backfix/'+name] = (ROOT.parent/name).read_bytes()
    inputs = json.loads((ROOT/'validation/apk-inputs.json').read_text())
    require(set(inputs) == {'47.4.3'}, 'Only the supported 47.4.3 APK identity is allowed')
    fresh['META-INF/hiro-tiktok4743backfix/ORIGIN.txt'] = (
        'Hiro TikTok 47.4.3 v4.2.0 / OneBack v1.1.22; 47.5.3 support removed\n'
        + ''.join('Original TikTok '+version+' global APK SHA256: '+row['sha256']+'\n' for version,row in sorted(inputs.items())) +
        'Source: https://github.com/hiro191u3n2/hiro-morphe-patches/tree/tiktok-v4.2.0/releases/tiktok4743backfix\n'
        'Japanese loader: this release src/japanese; unchanged dictionary source: releases/tiktok-japanese100 at efb5937ddea4fe29abdfe33027cfc0e5d397ae93\n'
        'Physical Android device execution is not claimed.\n').encode()
    retained = {name:raw for name,raw in base.items() if name not in (MF,'classes.dex',ONEBACK_EXT,NATIVE_EXT)
                and not name.startswith(OLD_PREFIXES)}
    combined = {**retained, **fresh, 'classes.dex':(args.work/'combined.dex').read_bytes()}
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-10-24T00:00:00',
        Description='TikTok 47.4.3 native Back fix; 47.5.3 support removed from all authored TikTok entries. Preserve other applications.')
    combined[MF] = manifest(fields)
    own_fields = dict(fields)
    own_fields.update(Name='Hiro TikTok 47.4.3', Version=VERSION, Author='Hiro contributors',
                      Source='https://github.com/hiro191u3n2/hiro-morphe-patches',
                      Description='Eight authored TikTok entries: six native features, OneBack, and opt-in Japanese settings.',
                      License='GPL-3.0-only with inherited Section 7 notices', **{'Patcher-Version':'2.0.0'})
    single = {**carried, **fresh, MF:manifest(own_fields),'classes.dex':(args.work/'single.dex').read_bytes()}
    require(all(combined[name] == raw for name,raw in retained.items()), 'Unrelated aggregate entries changed')
    metadata = []
    for name, entries in ((SINGLE,single),(BUNDLE,combined)):
        write_zip(args.dist/name, entries)
        require(archive(args.dist/name) == entries, 'Output archive differs')
        for sdk in ('morphe.jar','morphe22.jar','morphe200.jar'):
            result = run(['java','-ea','-cp',os.pathsep.join(map(str,[args.work/'audit',args.tools/sdk])),
                          'MetadataAudit4753',args.dist/name], name+'.'+sdk+'.metadata.txt')
            metadata.append({'artifact':name,'sdk':sdk,'result':result})
    require(source_ids(ROOT) == sources, 'Sources changed during build')
    evidence = {
        'schema':'tiktok4743-backfix-build-v1', 'patch_version':VERSION, 'oneback_version':'1.1.22',
        'bundle_version':BUNDLE_VERSION, 'baseline_version':BASE_VERSION, 'baseline_sha256':BASE_SHA,
        'supported_versions':['47.4.3'], 'removed_versions':['47.5.3'],
        'physical_android_tested':False, 'original_apk_validation':'separate signed original-APK application report',
        'sources':sources,'toolchain_sha256':TOOLS,'host':host,'legacy_tests':legacy_tests,'loader_preservation':merge,
        'oneback_runtime_preservation':runtime,'preserved_unrelated_entries':len(retained),
        'japanese_runtime_byte_identical':True, 'japanese_loader_version_restricted':True,'metadata':metadata,'target_admission':admission,
        'runtime_sha256':{'oneback':sha(args.work/'verified-oneback.dex'),'native':sha(args.work/'native-dex/classes.dex'),
                          'japanese':sha_bytes(base[JAPANESE_EXT])},
        'artifacts':{name:{'bytes':(args.dist/name).stat().st_size,'sha256':sha(args.dist/name)} for name in (SINGLE,BUNDLE)},
    }
    (args.dist/QA).write_bytes(json_bytes(evidence))
    print('PASS BUILD '+json.dumps(evidence['artifacts']), flush=True)

if __name__ == '__main__':
    main()
