#!/usr/bin/env python3
"""Build and verify additive Maps startup-location MPPs from pinned inputs.

Host behavior and real APK structure checks do not imply Android UI execution.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.4', '1.0.365', '1.0.366'
BASE_SHA = '335e2da04909308848defcba74d438c576dc3db9087c55cf6bd410468c62e1e1'
PRIOR_RUNTIME_SOURCE_SHA = '72186fe264b31cb5403489312516674a4bede23e79b28682e2c73e6ea3ca456f'
LEGACY_RUNTIME_SHA = '8bb1a90aca616a1831eb7bbbf2bfb18ce4eb1f4d0bba6b061b4a1a5fbb20fb9d'
SUPPLIED_APK_SHA = '7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063'
REPORTED_APK_SHA = '238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-2.2.jar': '855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}
PREFIX = 'app/hiro/mapsstartup/patches/'
EXT = 'extensions/maps_startup_location.mpe'
MF = 'META-INF/MANIFEST.MF'
PATCH_NAME = '起動時に現在地を表示'
CATEGORY_NAME = '検索欄の下のカテゴリ行を非表示'
BACK_NAME = '戻るで終了して履歴から削除'
PACKAGES = ('org.ungoogled.android.apps.maps.microg', 'com.google.android.apps.maps')
APP_VERSION = '26.36.04.973607363'
SINGLE = 'Maps_Startup_Location_v1.0.4.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.366.mpp'
QA_NAME = 'QA_Maps_Startup_Location_v1.0.4.json'


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt ZIP')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP entry')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [row.decode().split(': ', 1) for row in rows]
    require(all(len(pair) == 2 for pair in pairs) and len(pairs) == len(dict(pairs)), 'Invalid manifest')
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(isinstance(value, str) and not any(c in value for c in '\r\n\0'), 'Invalid manifest field')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    value = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(value) == fields, 'Manifest round trip differs')
    return value


def write_zip(path, data):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(data, key=lambda n: (n != MF, n)):
            info = zipfile.ZipInfo(name, (2026, 11, 8, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, data[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def classes(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}


def sources():
    return {str(p.relative_to(ROOT)): sha(p) for p in sorted(ROOT.rglob('*'))
            if p.is_file() and p.suffix in ('.py', '.java') and '__pycache__' not in p.parts}


def entry_hashes(path, resources_only=False):
    with zipfile.ZipFile(path) as z:
        return {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist()
                if not n.endswith('/') and (not resources_only or (
                    not re.fullmatch(r'classes(?:[0-9]+)?\.dex', n)
                    and not (n.startswith('META-INF/') and
                             (n == MF or n.upper().endswith(('.SF', '.RSA', '.DSA', '.EC'))))))}


def main():
    parser = argparse.ArgumentParser()
    for name in ('base', 'tools', 'work', 'dist', 'apk', 'reported-apk', 'java-home', 'legacy-runtime'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--timestamp', required=True, help='JST release timestamp, YYYY-MM-DDTHH:MM:SS')
    args = parser.parse_args()
    for name in ('base', 'tools', 'work', 'dist', 'apk', 'reported_apk', 'java_home', 'legacy_runtime'):
        setattr(args, name, getattr(args, name).resolve())
    require(re.fullmatch(r'2026-11-0[89]T\d\d:\d\d:\d\d', args.timestamp), 'Unexpected release timestamp')
    java, javac = args.java_home / 'bin/java', args.java_home / 'bin/javac'
    require(sha(args.base) == BASE_SHA, 'Baseline bundle differs')
    require(sha(args.apk) == SUPPLIED_APK_SHA, 'Supplied microG Maps APK differs')
    require(sha(args.reported_apk) == REPORTED_APK_SHA, 'Reported non-centering APK differs')
    require(sha(args.legacy_runtime) == LEGACY_RUNTIME_SHA, 'Legacy regression runtime differs')
    require(sha(ROOT / 'test-prior/StartupLocation.java') == PRIOR_RUNTIME_SOURCE_SHA,
            'Prior runtime is not the exact published v1.0.3 source')
    for name, digest in TOOLS.items():
        require(sha(args.tools / name) == digest, 'Pinned tool mismatch: ' + name)
    require(not args.work.exists(), 'Fresh build directory required')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'Empty output directory required')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)

    def run(command, log_name, timeout=900):
        print('RUN ' + log_name, flush=True)
        with (args.work / log_name).open('w') as out:
            result = subprocess.run(list(map(str, command)), stdout=out, stderr=subprocess.STDOUT, timeout=timeout)
        output = (args.work / log_name).read_text()
        require(result.returncode == 0, 'Failed ' + log_name + '\n' + output[-16000:])
        return output.strip()

    compiler = run([javac, '-version'], 'javac.txt')
    require(compiler in ('javac 17.0.20', 'javac 21.0.13', 'javac 21.0.9'), 'Pinned javac required')
    java_runtime = run([java, '-version'], 'java.txt')
    source_hashes = sources()
    for name in ('runtime', 'runtime-dex', 'patch', 'patch-dex', 'test', 'host', 'host-prior', 'applications'):
        (args.work / name).mkdir()
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
         '-d', args.work / 'runtime', *sorted((ROOT / 'runtime').glob('*.java'))], 'runtime-javac.log')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar',
         '-d', args.work / 'patch', *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    auditcp = os.pathsep.join(map(str, (args.work / 'test', args.work / 'patch', args.tools / 'morphe-2.2.jar')))
    # 2.2 retains the old getter for binary compatibility but marks it synthetic,
    # so Java sources compile against the matching public legacy API.
    audit_compile_cp = os.pathsep.join(map(str, (args.work / 'test', args.work / 'patch', args.tools / 'morphe.jar')))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', audit_compile_cp,
         '-d', args.work / 'test', *sorted((ROOT / 'test').glob('*.java'))], 'audit-javac.log')
    native_sources = sorted((ROOT / 'test-native').rglob('*.java'))
    require(native_sources, 'Emitted native bridge execution tests missing')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', audit_compile_cp,
         '-d', args.work / 'test', *native_sources], 'native-vm-javac.log')
    coexist_sources = sorted((ROOT / 'test-coexist').glob('*.java'))
    require(coexist_sources, 'Existing Maps back-exit audit missing')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', audit_compile_cp + os.pathsep + str(args.base),
         '-d', args.work / 'test', *coexist_sources], 'coexist-javac.log')
    host_sources = [*sorted((ROOT / 'test-stubs').rglob('*.java')),
                    *sorted((ROOT / 'test-host').rglob('*.java')),
                    *sorted((ROOT / 'runtime').glob('*.java'))]
    require(any('test-host' in str(p) for p in host_sources), 'Host behavioral tests missing')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-d', args.work / 'host', *host_sources], 'host-javac.log')
    host = run([java, '-ea', '-cp', args.work / 'host', 'app.hiro.mapsstartup.StartupLocationHostTest'], 'host-tests.txt')
    require('PASS' in host, 'Host behavior tests incomplete')
    prior_sources = [*sorted((ROOT / 'test-stubs').rglob('*.java')),
                     *sorted((ROOT / 'test-host').rglob('*.java')),
                     ROOT / 'runtime/LocationHost.java', ROOT / 'test-prior/StartupLocation.java']
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-d', args.work / 'host-prior', *prior_sources],
        'prior-host-javac.log')
    prior = subprocess.run([str(java), '-ea', '-cp', str(args.work / 'host-prior'),
                            'app.hiro.mapsstartup.StartupLocationHostTest'],
                           stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=30)
    (args.work / 'prior-host-regression.txt').write_text(prior.stdout)
    require(prior.returncode != 0 and 'AssertionError' in prior.stdout
            and 'native cold-launch' in prior.stdout.lower(), 'Prior native cold-launch regression was not reproduced')
    guards, native_bridge_tests, native_cold_launch_structure = [], [], []
    for sdk in ('morphe.jar', 'morphe-2.2.jar'):
        cp = os.pathsep.join(map(str, (args.work / 'test', args.work / 'patch', args.tools / sdk)))
        guards.append(run([java, '-Xmx3g', '-ea', '-cp', cp, 'MapsStartupAudit', 'guards', args.apk], sdk + '.guards.txt'))
        native_bridge_tests.append(run([java, '-Xmx3g', '-ea', '-cp', cp, 'MapsStartupBridgeVmTest',
                                        args.reported_apk], sdk + '.native-bridge.txt'))
        for label, input_apk in (('supplied-v285', args.apk), ('reported-v287', args.reported_apk)):
            proof = run([java, '-Xmx3g', '-ea', '-cp', cp, 'MapsStartupColdLaunchAudit', input_apk],
                        sdk + '.' + label + '.cold-launch-structure.txt')
            require('PASS native cold-launch structure' in proof, 'Native cold-launch structural proof incomplete')
            native_cold_launch_structure.append({'sdk': sdk, 'input': label, 'input_sha256': sha(input_apk),
                                                'result': 'PASS', 'audit': proof,
                                                'android_runtime_executed': False})
    for kind, api in (('runtime', '23'), ('patch', '26')):
        write_zip(args.work / (kind + '.jar'), classes(args.work / kind))
        run([java, '-cp', args.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', api,
             '--lib', args.tools / 'android.jar', '--classpath', args.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', args.work / (kind + '-dex'), args.work / (kind + '.jar')], kind + '-d8.log')
        require({p.name for p in (args.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')

    base = archive(args.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Baseline version differs')
    require(any(n.startswith(PREFIX) for n in base) and EXT in base, 'Expected startup v1.0.3 provider missing')
    require('app/hiro/maps/patches/MapsCategoryRowPatch.class' in base, 'Previous category patch missing')
    require('app/hiro/mapsback/patches/MapsBackExitPatch.class' in base, 'Previous back-exit patch missing')
    (args.work / 'base.dex').write_bytes(base['classes.dex'])
    (args.work / 'legacy-runtime.dex').write_bytes(args.legacy_runtime.read_bytes())
    preservation = run([java, '-Xmx3g', '-ea', '-cp', auditcp, 'MapsStartupAudit', 'merge',
                        args.work / 'base.dex', args.work / 'patch-dex/classes.dex', args.work / 'merged.dex'], 'loader-preservation.txt')
    require('PASS' in preservation, 'Loader preservation check incomplete')
    additions = {**classes(args.work / 'patch'), EXT: (args.work / 'runtime-dex/classes.dex').read_bytes()}
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp=args.timestamp,
                  Description='Maps cold-launch fix: the internal MAIN delivery during onCreate keeps the pending initial recenter until first resume. Preserve retained-task map position, early-tap handling, the 60s deadline, bounded recovery, routes, back-exit/category options and unrelated patches. Android UI execution unverified.')
    retained = {n: value for n, value in base.items() if not n.startswith(PREFIX) and n != EXT}
    combined = {**retained, **additions, 'classes.dex': (args.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    single = {**additions, 'classes.dex': (args.work / 'patch-dex/classes.dex').read_bytes(),
              MF: manifest({**fields, 'Name': 'Maps Startup Location', 'Version': VERSION})}
    preserved = {n: value for n, value in retained.items() if n not in (MF, 'classes.dex')}
    require(all(combined[n] == value for n, value in preserved.items()), 'Existing bundle entry changed')
    for name, data in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(args.dist / name, data)
        require(archive(args.dist / name) == data, 'Archive round trip differs')

    selection = []
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe-2.2.jar'):
            for package in PACKAGES:
                listing = run([java, '-jar', args.tools / sdk, 'list-patches', '--patches', args.dist / name,
                               '-f', package, '-p', '-v'], name + '.' + sdk + '.' + package + '.listing.txt')
                require(listing.count('Name: ' + PATCH_NAME + '\n') == 1, 'New patch not discoverable exactly once')
                if name == BUNDLE:
                    require(listing.count('Name: ' + CATEGORY_NAME + '\n') == 1, 'Existing Maps category patch lost')
                    require(listing.count('Name: ' + BACK_NAME + '\n') == 1, 'Existing Maps back-exit patch lost')
                selection.append({'artifact': name, 'loader': sdk, 'package': package, 'matches': 1})

    original_resources = entry_hashes(args.apk, resources_only=True)
    applications, outputs = [], {}

    def apply(name, names, label, input_apk=None):
        out = args.work / 'applications' / label
        out.mkdir()
        apk, report = out / 'patched.apk', out / 'morphe.json'
        source_apk = input_apk or args.apk
        command = [java, '-Xmx4g', '-jar', args.tools / 'morphe-2.2.jar', 'patch', source_apk,
                   '-p', args.dist / name, '--exclusive', '--unsigned', '--bytecode-mode', 'STRIP_FAST',
                   '-o', apk, '-t', out / 'tmp', '-r', report]
        for patch in names:
            command += ['-e', patch]
        run(command, label + '.apply.log')
        result = json.loads(report.read_text())
        applied = [item['name'] if isinstance(item, dict) else item for item in result.get('appliedPatches', [])]
        steps = result.get('stepResults', result.get('patchingSteps', []))
        require(apk.exists() and result.get('success', True) is True and not result.get('failedPatches')
                and set(applied) == set(names) and len(applied) == len(names)
                and result.get('packageName') == PACKAGES[0] and result.get('packageVersion') == APP_VERSION
                and steps and all(step.get('success') is True for step in steps), 'Actual APK application failed')
        require(entry_hashes(apk, resources_only=True) == entry_hashes(source_apk, resources_only=True),
                'APK resources/manifest/native code changed')
        return apk, result

    for name, label in ((SINGLE, 'standalone'), (BUNDLE, 'integrated')):
        apk, report = apply(name, [PATCH_NAME], label)
        audit = run([java, '-Xmx4g', '-ea', '-cp', auditcp, 'MapsStartupAudit', 'audit',
                     args.apk, apk, args.work / 'runtime-dex/classes.dex'], label + '.audit.txt')
        require('PASS' in audit, 'Actual APK class audit incomplete')
        outputs[label] = apk
        applications.append({'bundle': name, 'sdk': 'morphe-2.2.jar', 'sdk_version': '2.2.0',
                             'bytecode_mode': 'STRIP_FAST', 'package': PACKAGES[0], 'version': APP_VERSION,
                             'result': 'PASS', 'audit_summary': audit.splitlines()[-1],
                             'preserved_original_resources': len(original_resources)})
    require(entry_hashes(outputs['standalone']) == entry_hashes(outputs['integrated']), 'Standalone/integrated APK contents differ')

    # Co-selection is a practical Manager regression: both native Maps patches
    # must coexist with the previous category row transformation unchanged.
    both_apk, both_report = apply(BUNDLE, [PATCH_NAME, CATEGORY_NAME], 'with-category')
    both_audit = run([java, '-Xmx4g', '-ea', '-cp', auditcp, 'MapsStartupAudit', 'audit-category',
                      args.apk, both_apk, args.work / 'runtime-dex/classes.dex'], 'with-category.audit.txt')
    require('PASS' in both_audit, 'Category coexistence audit incomplete')
    triple_apk, triple_report = apply(BUNDLE, [PATCH_NAME, CATEGORY_NAME, BACK_NAME], 'all-three-maps')
    back_audit = run([java, '-Xmx4g', '-ea', '-cp', auditcp + os.pathsep + str(args.base),
                      'MapsBackAudit', 'audit', both_apk, triple_apk], 'all-three-maps.audit.txt')
    require('PASS' in back_audit, 'Back-exit coexistence audit incomplete')

    # The user-provided failing APK includes upstream v2.8.7 plus our v1.0.0.
    # Strip only the verified legacy hooks/runtime to form a private regression
    # input; retain its upstream modifications and existing category suppression.
    normalized_apk = args.work / 'reported-native-v287.apk'
    normalization = run([java, '-Xmx4g', '-ea', '-cp', auditcp, 'MapsStartupAudit', 'normalize-legacy',
                         args.reported_apk, normalized_apk, args.work / 'legacy-runtime.dex'],
                        'reported-v287.normalize.txt')
    require('PASS' in normalization, 'Reported APK legacy normalization incomplete')
    reported_guards = run([java, '-Xmx3g', '-ea', '-cp', auditcp, 'MapsStartupAudit', 'guards', normalized_apk],
                          'reported-v287.guards.txt')
    reported_out, reported_report = apply(BUNDLE, [PATCH_NAME], 'reported-v287', normalized_apk)
    reported_audit = run([java, '-Xmx4g', '-ea', '-cp', auditcp, 'MapsStartupAudit', 'audit',
                          normalized_apk, reported_out, args.work / 'runtime-dex/classes.dex'],
                         'reported-v287.audit.txt')
    require('PASS' in reported_audit, 'Reported APK new patch application audit incomplete')
    require(sources() == source_hashes, 'Sources changed during build')
    qa = {'schema': 'maps-startup104-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
          'release_timestamp': args.timestamp, 'release_timezone': 'Asia/Tokyo',
          'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA,
          'production_android_sdk_compile': True, 'physical_android_tested': False,
          'runtime_android_tested': False, 'android_emulator_tested': False,
          'runtime_limitation': 'No connected Android device or usable emulator was available in this session. Production lifecycle coordinator host tests, native bridge instruction tests and actual APK patch application are verified separately from Android UI behavior.',
          'original_stock_apk_tested': False, 'runtime_extension': EXT, 'apk_bytecode_mode': 'STRIP_FAST',
          'compiler': compiler, 'java_runtime': java_runtime,
          'toolchain_sha256': TOOLS, 'sources': source_hashes,
          'cold_launch_regression': {'prior_version': '1.0.3',
                                     'prior_source_sha256': PRIOR_RUNTIME_SOURCE_SHA,
                                     'failure_marker': 'native cold-launch', 'prior_reproduced': True,
                                     'prior_failure': prior.stdout, 'fixed_host_suite_passed': True},
          'artifacts': {n: {'bytes': (args.dist / n).stat().st_size, 'sha256': sha(args.dist / n)} for n in (SINGLE, BUNDLE)},
          'loader_preservation': preservation, 'preserved_unrelated_entries': len(preserved),
          'host_tests': host, 'guard_tests': '\n'.join(guards), 'selection_results': selection,
          'native_bridge_execution_tests': native_bridge_tests,
          'native_cold_launch_structure': native_cold_launch_structure,
          'reported_apk_regression': {'input_sha256': REPORTED_APK_SHA,
                                      'upstream_patch_version': '2.8.7',
                                      'input_contains_legacy_startup_patch': True,
                                      'normalization': normalization,
                                      'guard_tests': reported_guards,
                                      'new_patch_application': 'PASS',
                                      'audit_summary': reported_audit,
                                      'physical_android_ui_tested': False},
          'apk_application': applications, 'category_coexistence_audit': both_audit,
          'back_exit_coexistence_audit': back_audit,
          'three_patch_coexistence': {'patches': [PATCH_NAME, CATEGORY_NAME, BACK_NAME],
                                      'result': 'PASS', 'audit': back_audit},
          'supplied_apk_sha256': SUPPLIED_APK_SHA, 'supplied_apk_non_dex_resources_unchanged': True,
          'single_bundle_apk_entry_contents_equal': True}
    (args.dist / QA_NAME).write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()
