#!/usr/bin/env python3
"""Build patch-time persistent ad shields and preserve the active integrated bundle.

The shipped Yahoo patch is Android DEX only. Verify source and exact shipped DEX,
registered execution under two production SDKs, and unmodified Data Binding logic.
Frozen local evidence covers real archived APK resource encode/redecode on the same
DEX. Those archived APKs lack original app code: no original-app or device claim.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BUNDLE_VERSION, BASE_VERSION = '1.2.3', '1.0.268', '1.0.267'
BASE_SHA = '01d5fb87e2550fad55f7acc81a2733377a8ab6a7d03503f95bfafe43cf353327'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'morphe2.jar': '67479a00dba2de387e5115b73960267325e544cedf44a709416cdfb70c82e7f2',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
    'dex-tools-v2.4.zip': 'ee7c45eb3c1d2474a6145d8d447e651a736a22d9664b6d3d3be5a5a817dda23a',
    'databinding-runtime-8.13.2.aar': '5d5d11738bd4cb27a4075ab7ea2e30d1e3255afe95cb32410a7cfbc6c3241086',
    'databinding-common-8.13.2.jar': '66cab82639dac0f6c2433464c093b074d608c4bb887ec38a9b8bc4ac98126732',
    'viewbinding-8.13.2.aar': 'b9a3ae329638ab623dc1daff2ac07489e0e4dbcf90d77ce2d8759f767cd7a135',
    'lifecycle-common-2.6.1.jar': 'f34831b6c71cd844e1d35d1be49d5e79447c5ab856346531b1e8676fda7374b1',
    'lifecycle-livedata-core-2.6.1.aar': '2256780a3cff4a1e57fbb3d442557c17dc363ab8af105bcaf5261d8e2d5db949',
    'collection-1.0.0.jar': '9c8d117b5c2bc120a1cdfeb857e05b495b16c36013570372a708f7827e3ac9f9',
}
MF = 'META-INF/MANIFEST.MF'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.268.mpp'
QA_NAME = 'QA_YahooTransit_v1.2.3.json'


def require(value, why):
    if not value:
        raise RuntimeError(why)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt archive')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate archive entries')
        return {name: z.read(name) for name in z.namelist() if not name.endswith('/')}


def extract_tools(path, destination):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt dex2jar archive')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate dex2jar entries')
        for entry in z.infolist():
            name = PurePosixPath(entry.filename)
            require(not name.is_absolute() and '..' not in name.parts and '\\' not in entry.filename
                    and name.parts and name.parts[0] == 'dex-tools-v2.4'
                    and (entry.external_attr >> 16) & 0o170000 != 0o120000,
                    'Unsafe dex2jar entry: ' + entry.filename)
            target = destination.joinpath(*name.parts)
            if entry.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(entry))


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [line.decode().split(': ', 1) for line in rows]
    require(all(len(p) == 2 for p in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest')
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    raw = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(raw) == fields, 'Manifest roundtrip differs')
    return raw


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda n: (n != MF, n)):
            info = zipfile.ZipInfo(name, (2026, 10, 21, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode()


def real_resource_evidence(dex_sha):
    evidence = json.loads((ROOT / 'evidence/real-resource-validation.json').read_bytes())
    require(evidence['helper_source_sha256'] == sha(ROOT / 'NearMeLayout.java')
            and evidence['test_source_sha256'] == sha(ROOT / 'tests/RealResourceRoundtripTest.java'),
            'Local real-resource evidence does not match final helper/test source')
    expected_apks = {
        '8.4.1': 'ca127e289281c5dece5ef6a746788944c42d852a1da815e46c04f43d53e21c09',
        '8.5.0': 'c4968b35e133853e7d24d44140dbe9bceb92a21a19a1a42f42365914396bd2ee',
    }
    sdks = {TOOLS['morphe.jar'], TOOLS['morphe2.jar']}
    runs = evidence['runs']
    require(len(runs) == 4 and {(r['inputVersionName'], r['sdkSha256']) for r in runs}
            == {(v, s) for v in expected_apks for s in sdks}, 'Incomplete real-resource SDK/version matrix')
    for run in runs:
        require(run['status'] == 'PASS' and run['shippedDexSha256'] == dex_sha
                and run['inputApkSha256'] == expected_apks[run['inputVersionName']]
                and run['evidenceIdentity'] == 'shippedDexSha256'
                and run['originalViewIdentityTopologyPreserved'] is True
                and run['normalSearchContentAndParentsPreserved'] is True
                and run['idempotent'] is True and run['binaryRoundtripStructureEqual'] is True
                and run['layoutCount'] == 570 and run['homeProtectedSlotCount'] == 2
                and run['hiddenWrapperCount'] >= 31
                and run['inputApkDexEntries'] == 0 and run['deviceTested'] is False
                and run['pristineOriginalApkAvailable'] is False,
                'Local real-resource result is incomplete or for a different DEX')
    return evidence


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base', 'tools', 'work', 'dist'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name in ('base', 'tools', 'work', 'dist'):
        setattr(args, name, getattr(args, name).resolve())
    require(sha(args.base) == BASE_SHA, 'Active bundle267 baseline differs')
    for name, digest in TOOLS.items():
        require(sha(args.tools / name) == digest, 'Pinned tool mismatch: ' + name)
    require(not args.work.exists(), 'Use a fresh work directory')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'Use an empty output directory')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)

    def run(command, name, timeout=300):
        print('RUN ' + name, flush=True)
        with (args.work / name).open('w') as output:
            result = subprocess.run(list(map(str, command)), stdout=output, stderr=subprocess.STDOUT, timeout=timeout)
        text = (args.work / name).read_text()
        require(result.returncode == 0, 'Failed ' + name + '\n' + text[-10000:])
        return text.strip()

    require('Temurin-21.0.8+9' in run(['java', '--version'], 'java.txt'), 'Pinned JDK21.0.8+9 required')
    require(run(['javac', '-version'], 'javac.txt') == 'javac 21.0.8', 'Pinned javac required')
    sources = {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
               if x.is_file() and x.suffix in ('.java', '.py', '.json')}
    for name in ('classes', 'tests', 'dex'):
        (args.work / name).mkdir()
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar',
         '-d', args.work / 'classes', ROOT / 'NearMeLayout.java'], 'helper-javac.log')
    cp = os.pathsep.join(map(str, (args.work / 'tests', args.work / 'classes', args.tools / 'morphe.jar')))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', args.work / 'tests',
         *sorted((ROOT / 'tests').glob('*.java'))], 'test-javac.log')
    host = run(['java', '-ea', '-cp', cp, 'app.hiro.yahootransit.patches.NearMeLayoutTest',
                args.work / 'fixtures'], 'host-tests.txt')
    require(host.splitlines()[-1].startswith('PASS NearMeLayoutTest:'), 'Host tests incomplete')
    ads = run(['java', '-ea', '-cp', cp, 'app.hiro.yahootransit.patches.AdSlotsTest',
               args.work / 'ad-fixtures'], 'ad-regression-tests.txt')
    require(ads.splitlines()[-1].startswith('PASS AdSlotsTest:'), 'Ad regression tests incomplete')
    binding = run(['java', '-ea', '-cp', cp, 'app.hiro.yahootransit.patches.BindingExecutionTest',
                   args.work / 'binding-fixtures', args.tools, args.tools / 'android.jar'], 'binding-tests.txt')
    require(binding.splitlines()[-1].startswith('PASS BindingExecutionTest:'), 'Actual binding-runtime tests incomplete')
    helper_entries = {p.relative_to(args.work / 'classes').as_posix(): p.read_bytes()
                      for p in (args.work / 'classes').rglob('*.class')}
    require(set(helper_entries) == {'app/hiro/yahootransit/patches/NearMeLayout.class'},
            'Unexpected additional patch classes')
    write_zip(args.work / 'helper.jar', helper_entries)
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', args.tools / 'morphe.jar',
         '--output', args.work / 'dex', args.work / 'helper.jar'], 'd8.log')
    require({p.name for p in (args.work / 'dex').iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    baseline = archive(args.base)
    fields = headers(baseline[MF])
    require(fields['Version'] == BASE_VERSION, 'Baseline manifest version differs')
    (args.work / 'base.dex').write_bytes(baseline['classes.dex'])
    preservation = run(['java', '-ea', '-cp', cp, 'LoaderAudit', args.work / 'base.dex',
                        args.work / 'dex/classes.dex', args.work / 'merged.dex'], 'dex-preservation.txt')
    require(preservation.startswith('PASS LoaderAudit:'), 'DEX preservation incomplete')
    extract_tools(args.tools / 'dex-tools-v2.4.zip', args.work / 'dex2jar-tools')
    conversion = run(['java', '-cp', args.work / 'dex2jar-tools/dex-tools-v2.4/lib/*',
                      'com.googlecode.dex2jar.tools.Dex2jarCmd', '-f', '-o', args.work / 'shipped-jvm-all.jar',
                      args.work / 'merged.dex'], 'shipped-dex-conversion.log')
    require('Exception' not in conversion and 'Error' not in conversion, 'Shipped DEX conversion failed')
    prefixes = ('app/hiro/yahootransit/', 'jp/hiro/patches/yahootransit/')
    bridge = {name: raw for name, raw in archive(args.work / 'shipped-jvm-all.jar').items()
              if name.endswith('.class') and name.startswith(prefixes)}
    require(len(bridge) == 29 and 'app/hiro/yahootransit/patches/NearMeLayout.class' in bridge
            and 'jp/hiro/patches/yahootransit/YahooTransitAdPatch.class' in bridge,
            'Unexpected shipped Yahoo JVM class inventory')
    write_zip(args.work / 'shipped-yahoo-test.jar', bridge)
    shipped_cp = os.pathsep.join(map(str, (args.work / 'tests', args.work / 'shipped-yahoo-test.jar',
                                         args.tools / 'morphe.jar')))
    shipped_host = run(['java', '-ea', '-cp', shipped_cp, 'app.hiro.yahootransit.patches.NearMeLayoutTest',
                        args.work / 'shipped-fixtures'], 'shipped-host-tests.txt')
    require(shipped_host.splitlines()[-1] == host.splitlines()[-1], 'Shipped DEX NearMe results differ from source')
    shipped_ads = run(['java', '-ea', '-cp', shipped_cp, 'app.hiro.yahootransit.patches.AdSlotsTest',
                       args.work / 'shipped-ad-fixtures'], 'shipped-ad-regression-tests.txt')
    require(shipped_ads.splitlines()[-1] == ads.splitlines()[-1], 'Shipped DEX ad results differ from source')
    shipped_binding = run(['java', '-ea', '-cp', shipped_cp, 'app.hiro.yahootransit.patches.BindingExecutionTest',
                           args.work / 'shipped-binding-fixtures', args.tools, args.tools / 'android.jar'],
                          'shipped-binding-tests.txt')
    require(shipped_binding.splitlines()[-1] == binding.splitlines()[-1], 'Shipped DEX binding results differ from source')
    binding_evidence = json.loads((args.work / 'binding-fixtures/binding-execution-provenance.json').read_bytes())
    require(binding_evidence == json.loads((args.work / 'shipped-binding-fixtures/binding-execution-provenance.json').read_bytes()),
            'Source and shipped DEX binding runtime provenance differs')
    full_patch = run(['java', '-ea', '-cp', shipped_cp, 'app.hiro.yahootransit.patches.FullPatchExecutionTest',
                      args.work / 'full-patch-fixtures'], 'shipped-full-patch-tests.txt')
    require(full_patch.splitlines()[-1].startswith('PASS FullPatchExecutionTest:'),
            'Shipped DEX full patch execution incomplete')
    combined = dict(baseline)
    combined['classes.dex'] = (args.work / 'merged.dex').read_bytes()
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-10-21T00:00:00',
                  Description='Yahoo Transit v1.2.3: fix skipped ad shields on Data Binding screens; preserve IDs and include/merge boundaries. Verify shipped DEX, two SDKs, archived resource roundtrips and official binding logic. Physical device not tested.')
    combined[MF] = manifest(fields)
    require(set(combined) == set(baseline), 'Archive entry inventory differs')
    preserved = {k: v for k, v in baseline.items() if k not in (MF, 'classes.dex')}
    require(all(combined[k] == v for k, v in preserved.items()), 'Existing archive resource changed')
    write_zip(args.dist / BUNDLE, combined)
    require(archive(args.dist / BUNDLE) == combined, 'MPP roundtrip differs')
    # Full test-only bundle uses the exact shipped DEX and corresponding converted
    # Yahoo classes. Never distribute this bridge or place source helper classes
    # on the registered-execution test classpath.
    write_zip(args.work / 'shipped-loader-test.mpp', {**combined, **bridge})
    registered = {}
    for sdk in ('morphe.jar', 'morphe2.jar'):
        registered_cp = os.pathsep.join(map(str, (args.work / 'tests', args.tools / sdk)))
        result = run(['java', '-ea', '-cp', registered_cp,
                      'app.hiro.yahootransit.patches.RegisteredPatchExecutionTest',
                      args.work / 'shipped-loader-test.mpp', args.work / ('registered-' + sdk)],
                     'registered-' + sdk + '.txt')
        require(result.splitlines()[-1].startswith('PASS RegisteredPatchExecutionTest:'),
                'Registered patch execution incomplete: ' + sdk)
        registered[sdk] = result.splitlines()[-1]
    require(len(set(registered.values())) == 1, 'SDK registered execution results differ')
    local_resources = real_resource_evidence(sha(args.work / 'merged.dex'))
    require(sources == {str(x.relative_to(ROOT)): sha(x) for x in sorted(ROOT.rglob('*'))
                        if x.is_file() and x.suffix in ('.java', '.py', '.json')}, 'Sources changed during build')
    qa = {
        'schema': 'yahootransit123-qa-v1', 'result': 'PASS', 'patch_version': VERSION,
        'bundle_version': BUNDLE_VERSION, 'baseline_version': BASE_VERSION,
        'baseline_sha256': BASE_SHA, 'bundle_sha256': sha(args.dist / BUNDLE),
        'physical_android_tested': False, 'production_morphe_sdk_compile': True,
        'original_apk_available': False, 'original_apk_applied': False,
        'host_tests': host.splitlines()[-1], 'ad_regression_tests': ads.splitlines()[-1], 'dex_preservation': preservation,
        'shipped_dex_tests': {
            'method': 'Exact merged DEX converted with pinned dex2jar v2.4; test classpath excludes source classes.',
            'dex_sha256': sha(args.work / 'merged.dex'), 'yahoo_jvm_classes': len(bridge),
            'host_tests': shipped_host.splitlines()[-1], 'ad_regression_tests': shipped_ads.splitlines()[-1],
            'full_patch_execution': full_patch.splitlines()[-1], 'source_results_match': True,
            'registered_execution_by_sdk': registered,
            'binding_runtime_test': shipped_binding.splitlines()[-1],
        },
        'binding_runtime_evidence': binding_evidence,
        'local_real_resource_evidence': local_resources,
        'preserved_archive_entries': len(preserved), 'changed_archive_entries': [MF, 'classes.dex'],
        'shipped_helper_class': 'Lapp/hiro/yahootransit/patches/NearMeLayout;',
        'scope': 'Ad IDs, ad includes and concrete ad View names across layout configurations, exact Yahoo SDK AdViewGroup, confirmed fare PR banner and search-result NearMe. Preserve normal Data Binding lookup and include/merge boundaries. No startup hooks or network changes.',
        'limitations': [
            'No physical Android display verification.',
            'Available 8.4.1/8.5.0 APK archives were previously modified and contain no DEX; original application code and complete original APK patching are unverified.',
            'Local archived-resource evidence is pinned to the exact shipped DEX and final test source. CI verifies its identity; private APK resource roundtrips are not repeated in CI.',
            'Actual official Data Binding lookup runs on host View stubs with test ID maps, not the application generated binding classes or Android inflation/rendering.',
            'Version8.6.0 is a generated regression fixture, not a claim about a current installed version.',
            'Generated fixture tests use production Morphe ResourcePatchContext/Document with a test-only directory ResourceCoder. Local archived-resource tests use the production resource coder.',
        ],
        'toolchain_sha256': TOOLS, 'sources': sources,
        'artifacts': {BUNDLE: {'bytes': (args.dist / BUNDLE).stat().st_size, 'sha256': sha(args.dist / BUNDLE)}},
    }
    (args.dist / QA_NAME).write_bytes(json_bytes(qa))
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()
