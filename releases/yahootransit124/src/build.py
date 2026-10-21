#!/usr/bin/env python3
"""Package equivalent JVM and Android Yahoo patches and verify the actual Manager runtime.

The exact distributed MPP is used by every execution regression. No test bridge
or source helper is added to its runtime. Preserve Android DEX and all old JVM
classes byte for byte, then include the 29 equivalent Yahoo JVM classes needed by
Manager 2's JVM runtime. Real 8.5.6 application resources/device are unavailable.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BUNDLE_VERSION, BASE_VERSION = '1.2.4', '1.0.269', '1.0.268'
BASE_SHA = 'ba53dc71ff4a247549fd7e45ec8e7bca529e0819eda865383ad74f20c84eff32'
TOOLS = {name: row['sha256'] for name, row in json.loads((ROOT / 'tool-sources.json').read_bytes()).items()}

MF = 'META-INF/MANIFEST.MF'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.269.mpp'
QA_NAME = 'QA_YahooTransit_v1.2.4.json'


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
    require(sha(args.base) == BASE_SHA, 'Active bundle268 baseline differs')
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
        require(result.returncode == 0, 'Failed ' + name + '\n' + text[-12000:])
        return text.strip()

    def source_hashes():
        return {str(p.relative_to(ROOT)): sha(p) for p in sorted(ROOT.rglob('*'))
                if p.is_file() and p.suffix in ('.java', '.py', '.json', '.xml', '.txt')}

    require('Temurin-21.0.8+9' in run(['java', '--version'], 'java.txt'), 'Pinned JDK21.0.8+9 required')
    require(run(['javac', '-version'], 'javac.txt') == 'javac 21.0.8', 'Pinned javac required')
    sources = source_hashes()
    baseline = archive(args.base)
    fields = headers(baseline[MF])
    require(fields['Version'] == BASE_VERSION, 'Baseline manifest version differs')
    (args.work / 'baseline.dex').write_bytes(baseline['classes.dex'])
    extract_tools(args.tools / 'dex-tools-v2.4.zip', args.work / 'dex2jar-tools')
    conversion = run(['java', '-cp', args.work / 'dex2jar-tools/dex-tools-v2.4/lib/*',
                      'com.googlecode.dex2jar.tools.Dex2jarCmd', '-f', '-o', args.work / 'converted-jvm.jar',
                      args.work / 'baseline.dex'], 'dex-conversion.log')
    require('Exception' not in conversion and 'Error' not in conversion, 'DEX conversion failed')
    prefixes = ('app/hiro/yahootransit/', 'jp/hiro/patches/yahootransit/')
    yahoo = {name: raw for name, raw in archive(args.work / 'converted-jvm.jar').items()
             if name.endswith('.class') and name.startswith(prefixes)}
    require(len(yahoo) == 29 and not (set(yahoo) & set(baseline))
            and 'app/hiro/yahootransit/patches/NearMeLayout.class' in yahoo
            and 'jp/hiro/patches/yahootransit/YahooTransitAdPatch.class' in yahoo,
            'Yahoo JVM class inventory or collision differs')
    combined = {**baseline, **yahoo}
    fields.update(Version=BUNDLE_VERSION, Timestamp='2026-10-21T00:00:00',
                  Description='Yahoo Transit v1.2.4: include equivalent Yahoo JVM classes alongside unchanged Android DEX so Manager JVM runtime executes the selected ad patch. Exact release loading and scheduler regression tested; physical device not tested.')
    combined[MF] = manifest(fields)
    preserved = {name: raw for name, raw in baseline.items() if name != MF}
    require(all(combined[name] == raw for name, raw in preserved.items()), 'Existing bundle code/resource changed')
    require(combined['classes.dex'] == baseline['classes.dex'], 'Android DEX must remain byte-identical')
    artifact = args.dist / BUNDLE
    write_zip(artifact, combined)
    require(archive(artifact) == combined, 'MPP roundtrip differs')
    require(set(combined) - set(baseline) == set(yahoo), 'Unexpected archive additions')
    artifact_sha = sha(artifact)

    # Compile only test code. Runtime classes must come from the distributed MPP
    # itself. No source helper class, converted test JAR or augmented bridge exists.
    core_tests = args.work / 'core-tests'
    core_tests.mkdir()
    compile_cp = os.pathsep.join(map(str, (artifact, args.tools / 'morphe.jar')))
    run(['javac', '-encoding', 'UTF-8', '-cp', compile_cp, '-d', core_tests,
         *sorted((ROOT / 'tests').glob('*.java'))], 'core-test-javac.log')
    require(not (core_tests / 'app/hiro/yahootransit/patches/NearMeLayout.class').exists(),
            'Test classes must not contain a helper implementation')
    runtime_cp = os.pathsep.join(map(str, (core_tests, artifact, args.tools / 'morphe.jar')))
    results = {}
    for test in ('NearMeLayoutTest', 'AdSlotsTest', 'FullPatchExecutionTest'):
        result = run(['java', '-ea', '-cp', runtime_cp, 'app.hiro.yahootransit.patches.' + test,
                      args.work / ('fixtures-' + test)], test + '.txt')
        require(result.splitlines()[-1].startswith('PASS ' + test + ':'), 'Incomplete distributed MPP regression: ' + test)
        results[test] = result.splitlines()[-1]
    binding = run(['java', '-ea', '-cp', runtime_cp, 'app.hiro.yahootransit.patches.BindingExecutionTest',
                   args.work / 'binding-fixtures', args.tools, args.tools / 'android.jar'], 'binding-tests.txt')
    require(binding.splitlines()[-1].startswith('PASS BindingExecutionTest:'), 'Binding runtime tests incomplete')
    results['BindingExecutionTest'] = binding.splitlines()[-1]
    binding_evidence = json.loads((args.work / 'binding-fixtures/binding-execution-provenance.json').read_bytes())
    registered = {}
    for sdk in ('morphe.jar', 'morphe2.jar', 'morphe22.jar'):
        registered_cp = os.pathsep.join(map(str, (core_tests, args.tools / sdk)))
        result = run(['java', '-ea', '-cp', registered_cp,
                      'app.hiro.yahootransit.patches.RegisteredPatchExecutionTest', artifact,
                      args.work / ('registered-' + sdk)], 'registered-' + sdk + '.txt')
        require(result.splitlines()[-1].startswith('PASS RegisteredPatchExecutionTest:'), 'Registered execution incomplete')
        registered[sdk] = result.splitlines()[-1]
    require(len(set(registered.values())) == 1, 'Registered results differ across SDKs')

    manager_tests = args.work / 'manager-tests'
    manager_tests.mkdir()
    manager_host = args.tools / 'manager-dev15-host.jar'
    run(['javac', '-encoding', 'UTF-8', '-cp', manager_host, '-d', manager_tests,
         *sorted((ROOT / 'manager-tests').glob('*.java'))], 'manager-test-javac.log')
    manager_cp = os.pathsep.join(map(str, (manager_tests, manager_host)))
    selection = run(['java', '-ea', '-cp', manager_cp, 'ManagerJarSelectionTest',
                     args.base, artifact, manager_host, args.work / 'manager-selection.json'], 'manager-selection.txt')
    require(selection.splitlines()[-1].startswith('PASS ManagerJarSelectionTest:'), 'Actual Manager selection regression incomplete')
    # The fixture is generated from small, published text resources and inert
    # application classes. No archived app resources or input APK enter CI.
    run(['python3', ROOT / 'fixtures/create_scheduler_fixture.py', '--tools', args.tools,
         '--work', args.work / 'synthetic-fixture-build', '--output', args.work / 'synthetic-8.5.6.apk'],
        'synthetic-fixture-build.txt')
    # JvmInstallation adds these official APK framework resources to its host.
    # Keep the same resource bytes in a separate resource-only JAR for this test.
    scheduler_cp = os.pathsep.join((manager_cp, str(args.tools / 'manager-dev15-frameworks.jar')))
    scheduler = run(['java', '-ea', '-cp', scheduler_cp, 'app.hiro.yahootransit.patches.HostRuntimeRegressionTest', args.base, artifact,
                     args.work / 'synthetic-8.5.6.apk', args.work / 'scheduler',
                     args.work / 'manager-scheduler.json'], 'manager-scheduler.txt')
    require(scheduler.splitlines()[-1].startswith('PASS HostRuntimeRegressionTest:'), 'Actual scheduler regression incomplete')
    selection_evidence = json.loads((args.work / 'manager-selection.json').read_bytes())
    scheduler_evidence = json.loads((args.work / 'manager-scheduler.json').read_bytes())
    local_resources = real_resource_evidence(sha(args.work / 'baseline.dex'))
    require(sources == source_hashes(), 'Sources changed during build')
    require(sha(artifact) == artifact_sha, 'Artifact changed during verification')
    qa = {
        'schema': 'yahootransit124-qa-v1', 'result': 'PASS', 'patch_version': VERSION,
        'bundle_version': BUNDLE_VERSION, 'baseline_version': BASE_VERSION,
        'baseline_sha256': BASE_SHA, 'bundle_sha256': sha(artifact),
        'physical_android_tested': False, 'original_apk_8_5_6_available': False,
        'original_apk_8_5_6_applied': False,
        'android_dex_unchanged': True, 'android_dex_sha256': sha(args.work / 'baseline.dex'),
        'preserved_archive_entries': len(preserved),
        'preserved_jvm_classes': sum(name.endswith('.class') for name in baseline),
        'changed_existing_archive_entries': [MF],
        'added_yahoo_jvm_classes': {name: hashlib.sha256(raw).hexdigest() for name, raw in sorted(yahoo.items())},
        'distributed_mpp_tests': {
            'method': 'Exact final MPP. No source helper or augmented test bundle on the selection/scheduler classpath.',
            'resource_and_binding_regressions': results,
            'registered_execution_by_sdk': registered,
            'manager_selection': selection.splitlines()[-1],
            'manager_scheduler': scheduler.splitlines()[-1],
        },
        'manager_runtime_evidence': selection_evidence,
        'scheduler_execution_evidence': scheduler_evidence,
        'binding_runtime_evidence': binding_evidence,
        'local_real_resource_evidence': local_resources,
        'inherited_resource_evidence_context': 'Prior local 8.4.1/8.5.0 resource-only evidence remains bound to the unchanged Android DEX. It is not evidence of an original 8.5.6 APK or this Manager selection path.',
        'limitations': [
            'Actual Manager JVM code is exercised on the host; Android device/UI behavior is not tested.',
            'The full scheduler fixture is synthetic and merely declares version8.5.6. It is not an original Yahoo8.5.6 application.',
            'The historical resource archives contain no original DEX and were previously modified.',
            'Official Data Binding lookup uses host View stubs/test ID maps, not app-generated binding classes or Android rendering.',
        ],
        'toolchain_sha256': TOOLS, 'sources': sources,
        'artifacts': {BUNDLE: {'bytes': artifact.stat().st_size, 'sha256': sha(artifact)}},
    }
    (args.dist / QA_NAME).write_bytes(json_bytes(qa))
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()

