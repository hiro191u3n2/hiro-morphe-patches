#!/usr/bin/env python3
"""Build both executable MPP forms and verify the shipped bytes.

Private APK validation writes hash-bound evidence only. CI rebuilds the same
artifacts, executes synthetic APK pipelines and binds that local evidence.
No Android device behavior or timing claim follows from these host tests.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_SHA = '83ffc3416c99cef5d0464a147633f3d064a8b1856282715031176d2bb3cdbf7a'
TOOLS = {n: r['sha256'] for n, r in json.loads((ROOT / 'tool-sources.json').read_bytes()).items()}
MF = 'META-INF/MANIFEST.MF'
SINGLE = 'Berry_Browser_v1.3.1.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.272.mpp'
QA = 'QA_Berry_Browser_v1.3.1.json'
QUEUE = 'ロックフリーMessageQueueを有効化'
OLD = ('プレビュー作成を後回しにして表示中タブを優先', '共有先ブラウザの表示を個別に切替',
       'ダブルタップから指を離さず片手ズーム')
PREFIXES = ('app/hiro/berry/', 'app/hiro/berryshare/', 'app/hiro/berryzoom/', 'app/hiro/berryqueue/')
EXTENSIONS = ('extensions/berry_foreground_tabs.mpe', 'extensions/berry_browser_visibility.mpe',
              'extensions/berry_onehand_zoom.mpe')
NEW_EXT = 'extensions/berry_messagequeue.mpe'


def require(ok, reason):
    if not ok:
        raise RuntimeError(reason)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + '\n').encode()


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None and len(z.namelist()) == len(set(z.namelist())), 'Invalid archive')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name, raw in sorted(entries.items(), key=lambda row: (row[0] != MF, row[0])):
            info = zipfile.ZipInfo(name, (2026, 10, 22, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [row.decode().split(': ', 1) for row in rows]
    require(all(len(p) == 2 for p in pairs) and len(dict(pairs)) == len(pairs), 'Invalid manifest')
    return dict(pairs)


def manifest(fields):
    rows = []
    for name, value in fields.items():
        require(not any(c in value for c in '\r\n\0'), 'Invalid header')
        raw, prefix = (name + ': ' + value).encode(), b''
        while len(raw) + len(prefix) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            rows.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        rows.append(prefix + raw)
    result = b'\r\n'.join(rows) + b'\r\n\r\n'
    require(headers(result) == fields, 'Header roundtrip differs')
    return result


def class_files(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for n in ('base', 'tools', 'work', 'dist'):
        parser.add_argument('--' + n, type=Path, required=True)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--apk', type=Path)
    mode.add_argument('--without-real-apk', action='store_true')
    parser.add_argument('--regression-apk', type=Path)
    parser.add_argument('--variant-apk', type=Path, action='append', default=[])
    args = parser.parse_args()
    for n in ('base', 'tools', 'work', 'dist'):
        setattr(args, n, getattr(args, n).resolve())
    require(sha(args.base) == BASE_SHA, 'Published v271 baseline differs')
    for name, digest in TOOLS.items():
        require(sha(args.tools / name) == digest, 'Pinned tool differs: ' + name)
    require(not args.work.exists(), 'Use a fresh work directory')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'Use a fresh output directory')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)

    def run(cmd, name, timeout=600):
        print('RUN ' + name, flush=True)
        with (args.work / name).open('w') as log:
            done = subprocess.run(list(map(str, cmd)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        text = (args.work / name).read_text()
        require(done.returncode == 0, 'Failed ' + name + '\n' + text[-16000:])
        return text.strip()

    require('Temurin-21.0.8+9' in run(['java', '--version'], 'java.txt'), 'Pinned JDK21.0.8+9 required')
    require(run(['javac', '-version'], 'javac.txt') == 'javac 21.0.8', 'Pinned javac required')
    baseline = archive(args.base)
    fields = headers(baseline[MF])
    require(fields['Version'] == '1.0.271', 'Baseline version differs')
    require(all(n in baseline for n in EXTENSIONS), 'Existing Berry extensions missing')
    require(NEW_EXT in baseline, 'Existing MessageQueue extension missing')
    require(sum(n.endswith('.class') and n.startswith(PREFIXES) for n in baseline) == 32,
            'Baseline Berry JVM inventory differs')
    for folder in ('patch', 'patch-dex', 'build-tools', 'tests', 'manager-tests'):
        (args.work / folder).mkdir()
    write_zip(args.work / 'baseline-berry-jvm.jar',
              {n: raw for n, raw in baseline.items() if n.endswith('.class') and n.startswith(PREFIXES)})
    compile_cp = os.pathsep.join(map(str, (args.tools / 'morphe.jar', args.work / 'baseline-berry-jvm.jar')))
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', compile_cp,
         '-d', args.work / 'patch', *sorted((ROOT / 'patch').rglob('*.java'))], 'patch-javac.log')
    write_zip(args.work / 'patch.jar', class_files(args.work / 'patch'))
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', args.tools / 'morphe.jar',
         '--classpath', args.work / 'baseline-berry-jvm.jar',
         '--output', args.work / 'patch-dex', args.work / 'patch.jar'], 'patch-d8.log')
    require({p.name for p in (args.work / 'patch-dex').iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    (args.work / 'baseline.dex').write_bytes(baseline['classes.dex'])
    run(['javac', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar', '-d', args.work / 'build-tools',
         *sorted((ROOT / 'build-tools').glob('*.java'))], 'build-tools-javac.log')
    auditcp = os.pathsep.join(map(str, (args.work / 'build-tools', args.tools / 'morphe.jar')))
    preservation = run(['java', '-ea', '-cp', auditcp, 'BerryBundleDex', args.work / 'baseline.dex',
                        args.work / 'patch-dex/classes.dex', args.work / 'combined.dex', args.work / 'berry.dex'], 'dex-preservation.txt')
    require('PASS' in preservation, 'Unrelated patch DEX preservation incomplete')
    with zipfile.ZipFile(args.tools / 'dex-tools-v2.4.zip') as z:
        for entry in z.infolist():
            path = PurePosixPath(entry.filename)
            require(not path.is_absolute() and '..' not in path.parts and '\\' not in entry.filename
                    and path.parts and path.parts[0] == 'dex-tools-v2.4'
                    and (entry.external_attr >> 16) & 0o170000 != 0o120000, 'Unsafe tool archive')
            out = args.work / 'dex2jar' / path
            if entry.is_dir():
                out.mkdir(parents=True, exist_ok=True)
            else:
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_bytes(z.read(entry))
    conversion = run(['java', '-cp', args.work / 'dex2jar/dex-tools-v2.4/lib/*',
                      'com.googlecode.dex2jar.tools.Dex2jarCmd', '-f', '-o', args.work / 'berry-jvm.jar',
                      args.work / 'berry.dex'], 'dex2jar.log')
    require('Exception' not in conversion and 'Error' not in conversion, 'JVM conversion failed')
    jvm = {n: raw for n, raw in archive(args.work / 'berry-jvm.jar').items() if n.endswith('.class')}
    require(len(jvm) >= 13 and all(n.startswith(PREFIXES) for n in jvm), 'Invalid Berry JVM inventory')
    def retained_jvm(name):
        return name.endswith('.class') and (name.startswith('app/hiro/berryqueue/')
            or name.startswith(PREFIXES[:3]) and name.rsplit('/', 1)[-1] in ('MethodContract.class', 'OriginalContracts.class'))
    # The unchanged queue and canonical fingerprint utilities retain their reviewed JVM bytes too.
    kept_jvm = {n: raw for n, raw in baseline.items() if retained_jvm(n)}
    require(len(kept_jvm) == 10 and set(kept_jvm) <= set(jvm), 'Retained JVM inventory differs')
    jvm.update(kept_jvm)
    extra = jvm
    fields.update(Version='1.0.272', Timestamp='2026-10-22T00:00:00',
                  Description='Berry Browser v1.3.1: validated structural compatibility for3.83.74 and3.83.93.1 across foreground tabs, browser chooser and one-hand zoom; all-version Manager display; existing MessageQueue implementation retained; physical device not tested.')
    retained = {n: raw for n, raw in baseline.items() if not (n.endswith('.class') and n.startswith(PREFIXES[:3]))}
    combined = {**retained, **extra, MF: manifest(fields), 'classes.dex': (args.work / 'combined.dex').read_bytes()}
    sf = dict(fields)
    sf.update(Name='Berry Browser', Version='1.3.1')
    single = {**extra, **{n: baseline[n] for n in (*EXTENSIONS, NEW_EXT)}, MF: manifest(sf), 'classes.dex': (args.work / 'berry.dex').read_bytes()}
    require(all(combined.get(n) == raw for n, raw in baseline.items()
                if n not in (MF, 'classes.dex') and not (n.endswith('.class') and n.startswith(PREFIXES[:3]))),
            'Unrelated baseline entry changed')
    require(all(n.endswith('.class') and n.startswith(PREFIXES[:3])
                for n in set(combined).symmetric_difference(baseline)), 'Unexpected bundle entry additions/removals')
    require(all(combined[n] == baseline[n] for n in (*EXTENSIONS, NEW_EXT)), 'Existing runtime extension changed')
    for name, entries in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(args.dist / name, entries)
        require(archive(args.dist / name) == entries, 'MPP archive roundtrip differs')
    tests_cp = os.pathsep.join(map(str, (args.dist / SINGLE, args.tools / 'morphe.jar')))
    run(['javac', '-encoding', 'UTF-8', '-cp', tests_cp, '-d', args.work / 'tests',
         *sorted((ROOT / 'tests').glob('*.java'))], 'manifest-tests-javac.log')
    manifest_result = run(['java', '-ea', '-cp', os.pathsep.join((str(args.work / 'tests'), tests_cp)),
                           'ManifestContractTest'], 'manifest-contracts.txt')
    require(manifest_result.startswith('PASS ManifestContractTest:'), 'Manifest contracts incomplete')
    compat_result = run(['java', '-ea', '-cp', os.pathsep.join((str(args.work / 'tests'), tests_cp)),
                         'BerryCompatibilityTest'], 'compatibility-contracts.txt')
    require(compat_result.splitlines()[-1].startswith('PASS BerryCompatibilityTest:'), 'Compatibility contracts incomplete')
    metadata = {}
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe22.jar', 'manager-dev16-host.jar'):
            cp = os.pathsep.join(map(str, (args.work / 'build-tools', args.tools / sdk)))
            result = run(['java', '-ea', '-cp', cp, 'BerryMetadataAudit', args.dist / name, '4'], name + '-' + sdk + '-metadata.txt')
            require(result.splitlines()[-1].startswith('PASS BerryMetadataAudit:'), 'Shipped metadata/JVM audit incomplete')
            metadata[name + ':' + sdk] = result.splitlines()[-1]
    manager_host = args.tools / 'manager-dev16-host.jar'
    run(['javac', '-encoding', 'UTF-8', '-cp', manager_host, '-d', args.work / 'manager-tests',
         *sorted((ROOT / 'manager-tests').glob('*.java'))], 'manager-tests-javac.log')
    managercp = os.pathsep.join(map(str, (args.work / 'manager-tests', manager_host)))
    run(['java', '-ea', '-cp', managercp, 'BerryManagerTest', args.base, args.dist / BUNDLE,
         manager_host, args.work / 'manager-selection.json'], 'manager-selection.txt')
    manager_result = json.loads((args.work / 'manager-selection.json').read_bytes())

    # A small independently generated APK: no app code/assets are published.
    fixture = args.work / 'fixture'
    (fixture / 'res/values').mkdir(parents=True)
    (fixture / 'classes').mkdir()
    (fixture / 'dex').mkdir()
    (fixture / 'FixtureApplication.java').write_text('package jp.ejimax.berrybrowser; public final class FixtureApplication extends android.app.Application {}\n')
    (fixture / 'res/values/strings.xml').write_text('<resources><string name="app_name">Synthetic Berry fixture</string><item type="id" name="fixture_id"/></resources>\n')
    (fixture / 'AndroidManifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="jp.ejimax.berrybrowser" android:versionCode="999999" android:versionName="99.9">'
        '<uses-sdk android:minSdkVersion="32" android:targetSdkVersion="35"/>'
        '<uses-permission android:name="android.permission.INTERNET"/>'
        '<application android:name="jp.ejimax.berrybrowser.FixtureApplication" android:label="@string/app_name" android:allowBackup="false"/></manifest>\n')
    run(['javac', '--release', '8', '-cp', args.tools / 'android.jar', '-d', fixture / 'classes', fixture / 'FixtureApplication.java'], 'fixture-javac.log')
    write_zip(fixture / 'classes.jar', class_files(fixture / 'classes'))
    run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--min-api', '32', '--lib', args.tools / 'android.jar',
         '--output', fixture / 'dex', fixture / 'classes.jar'], 'fixture-d8.log')
    run([args.tools / 'aapt2', 'compile', '--dir', fixture / 'res', '-o', fixture / 'res.zip'], 'fixture-aapt-compile.log')
    run([args.tools / 'aapt2', 'link', '-I', args.tools / 'android.jar', '--manifest', fixture / 'AndroidManifest.xml',
         '-o', fixture / 'input.apk', fixture / 'res.zip'], 'fixture-aapt-link.log')
    with zipfile.ZipFile(fixture / 'input.apk', 'a') as z:
        z.writestr('classes.dex', (fixture / 'dex/classes.dex').read_bytes())

    def apply(apk, name, sdk, selection, label):
        out = args.work / 'applications' / label
        out.mkdir(parents=True)
        cmd = ['java', '-Xmx1536m', '-jar', args.tools / sdk, 'patch', apk, '-p', args.dist / name,
               '--exclusive', '--unsigned', '--bytecode-mode', 'FULL', '-o', out / 'patched.apk',
               '-t', out / 'temporary', '-r', out / 'patch-report.json']
        for selected in selection:
            cmd += ['-e', selected]
        log = run(cmd, label + '.apply.log')
        report = json.loads((out / 'patch-report.json').read_bytes())
        selected = [p if isinstance(p, str) else p.get('name') for p in report.get('appliedPatches', [])]
        require(not report.get('failedPatches') and set(selected) == set(selection), 'Shipped patch did not execute: ' + label)
        steps = report.get('patchingSteps', report.get('stepResults', []))
        require(steps and all(s.get('success') is True for s in steps), 'Patching/rebuild pipeline incomplete: ' + label)
        input_badging = run([args.tools / 'aapt2', 'dump', 'badging', apk], label + '.input-badging.txt')
        input_min_sdk = re.search(r"^minSdkVersion:'([0-9]+)'", input_badging, re.M)
        badging = run([args.tools / 'aapt2', 'dump', 'badging', out / 'patched.apk'], label + '.badging.txt')
        output_min_sdk = re.search(r"^minSdkVersion:'([0-9]+)'", badging, re.M)
        require(input_min_sdk and output_min_sdk and input_min_sdk[1] == output_min_sdk[1],
                'Encoded APK changed minimum SDK: ' + label)
        target = re.search(r"targetSdkVersion:'([0-9]+)'", badging)
        require(target and int(target[1]) >= 37, 'Encoded APK target must be37+')
        require("name='android.permission.ACCESS_LOCAL_NETWORK'" in badging, 'Encoded APK lacks LAN permission')
        xml = run([args.tools / 'aapt2', 'dump', 'xmltree', out / 'patched.apk', '--file', 'AndroidManifest.xml'], label + '.manifest.txt')
        provider_blocks = []
        lines = xml.splitlines()
        for index, line in enumerate(lines):
            if not re.match(r'^\s*E: provider(?:\s|$)', line):
                continue
            depth = len(line) - len(line.lstrip())
            end = index + 1
            while end < len(lines) and (not lines[end].strip()
                    or len(lines[end]) - len(lines[end].lstrip()) > depth):
                end += 1
            block = '\n'.join(lines[index:end])
            if '="app.hiro.berry.runtime.BerryQueueInit"' in block:
                provider_blocks.append(block)
        require(len(provider_blocks) == 1
                and '="jp.ejimax.berrybrowser.hiro.messagequeue"' in provider_blocks[0],
                'Encoded APK lacks unique startup diagnostic provider')
        for attribute in ('exported', 'grantUriPermissions'):
            require(re.search(r':'+ attribute + r'\(0x[0-9a-f]+\)=false(?:\s|$)', provider_blocks[0]),
                    'Encoded diagnostic provider must set ' + attribute + '=false')
        run(['java', '-Xmx768m', '-ea', '-cp', auditcp, 'AppDexAudit', 'all' if len(selection) > 1 else 'queue',
             apk, out / 'patched.apk', out / 'dex-audit.json'], label + '.dex-audit.txt')
        audit = json.loads((out / 'dex-audit.json').read_bytes())
        return {'sdk': sdk, 'artifact': name, 'selected': sorted(selection), 'result': 'PASS',
                'encoded_min_sdk': int(output_min_sdk[1]), 'encoded_target_sdk': int(target[1]),
                'diagnostic_provider_exported': False, 'diagnostic_provider_grant_uri_permissions': False,
                'dex_audit': audit}

    synthetic = []
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe22.jar'):
            synthetic.append(apply(fixture / 'input.apk', name, sdk, [QUEUE], 'synthetic-' + name + '-' + sdk))
    evidence_path = ROOT.parent / 'publication/real-apk-evidence.json'
    if args.apk:
        require(args.regression_apk is not None, 'Both current and regression APKs are required')
        inputs = [
            {'path': args.regression_apk.resolve(), 'version_name': '3.83.74', 'version_code': '383740',
             'input_apk_sha256': '4a4da9029d1d71edf376b2d1edf3636e3fba946b49d1fafa5787d20252c8ad5b',
             'provenance': 'Previously user-provided base APK'},
            {'path': args.apk.resolve(), 'version_name': '3.83.93.1', 'version_code': '383931',
             'input_apk_sha256': '63d6f38c50c1cdca7a6c0bfed7abffa0183817aa33741b5bcb3bd287ef3342ac',
             'provenance': 'Public APKMirror API32 variant; original signer verified'},
        ]
        compat_real_result = run(['java', '-Xmx1536m', '-ea', '-cp', os.pathsep.join((str(args.work / 'tests'), tests_cp)),
                                  'BerryCompatibilityTest', *(row['path'] for row in inputs)], 'compatibility-real-contracts.txt')
        require(compat_real_result.splitlines()[-1].startswith('PASS BerryCompatibilityTest:'), 'Real compatibility guards incomplete')
        actual = []
        for row in inputs:
            apk = row['path']
            require(sha(apk) == row['input_apk_sha256'], 'Reviewed Berry input differs: ' + row['version_name'])
            for name in (SINGLE, BUNDLE):
                for sdk in ('morphe.jar', 'morphe22.jar'):
                    label = 'real-' + row['version_name'] + '-' + name + '-' + sdk + '-all'
                    result = apply(apk, name, sdk, [*OLD, QUEUE], label)
                    result['version_name'] = row['version_name']
                    result['input_apk_sha256'] = row['input_apk_sha256']
                    actual.append(result)
        variant_hashes = {
            'fe0586a46ab1fa8b7386078b093f42585bc1383ef6c385fedfa73d07aa78a2b6': 29,
            '0e604165f45511130265d0480f78cd42616400fa4ee990c9fc58df8bbbdbb5b0': 26,
        }
        require(len(args.variant_apk) == 2 and {sha(v) for v in args.variant_apk} == set(variant_hashes),
                'Both additional reviewed 3.83.93.1 variants required')
        variant_inputs, variant_runs = [], []
        for apk in sorted(args.variant_apk, key=sha):
            digest = sha(apk)
            item = {'version_name': '3.83.93.1', 'version_code': '383931', 'input_apk_sha256': digest,
                    'input_min_sdk': variant_hashes[digest], 'provenance': 'Original-signed public APKMirror variant'}
            variant_inputs.append(item)
            result = apply(apk.resolve(), BUNDLE, 'morphe22.jar', [*OLD, QUEUE], 'variant-api' + str(variant_hashes[digest]))
            result.update(version_name='3.83.93.1', input_apk_sha256=digest)
            variant_runs.append(result)
        evidence = {'schema': 'berry-compat131-apk-evidence-v1', 'result': 'PASS',
                    'package_name': 'jp.ejimax.berrybrowser',
                    'inputs': [{k: v for k, v in row.items() if k != 'path'} for row in inputs],
                    'production_check_tests': compat_real_result,
                    'standalone_sha256': sha(args.dist / SINGLE), 'bundle_sha256': sha(args.dist / BUNDLE),
                    'physical_android_tested': False, 'actual_queue_selection_verified': False,
                    'performance_measured': False, 'runs': actual,
                    'additional_variant_inputs': variant_inputs, 'additional_variant_runs': variant_runs,
                    'scope': 'Saved3.83.74 and original-signed public3.83.93.1 API32 base APKs. Split install/runtime and Android17 device not tested.'}
        evidence_path.write_bytes(json_bytes(evidence))
    require(evidence_path.is_file(), 'Bound APK evidence required')
    evidence = json.loads(evidence_path.read_bytes())
    require(evidence.get('result') == 'PASS' and evidence.get('standalone_sha256') == sha(args.dist / SINGLE)
            and evidence.get('bundle_sha256') == sha(args.dist / BUNDLE)
            and evidence.get('physical_android_tested') is False, 'Bound APK evidence is stale or incomplete')
    # Synthetic output archives can contain builder timestamps; retain semantic audits, not output file hashes.
    for row in synthetic:
        for key in ('input_sha256', 'output_sha256', 'input_apk_sha256', 'output_apk_sha256'):
            row['dex_audit'].pop(key, None)
    qa = {'schema': 'berry-compat131-qa-v1', 'result': 'PASS', 'patch_version': '1.3.1',
          'bundle_version': '1.0.272', 'baseline_version': '1.0.271', 'baseline_sha256': BASE_SHA,
          'standalone_sha256': sha(args.dist / SINGLE), 'bundle_sha256': sha(args.dist / BUNDLE),
          'physical_android_tested': False, 'real_apk_evidence_sha256': sha(evidence_path),
          'tools': TOOLS, 'existing_runtime_extension_bytes_preserved': True,
          'manifest_contracts': manifest_result, 'compatibility_contracts': compat_result, 'dex_preservation': preservation,
          'jvm_class_count': len(jvm), 'metadata': metadata, 'manager_selection': manager_result,
          'synthetic_apk_tests': synthetic, 'real_apk_runs': len(evidence['runs']), 'additional_variant_runs': len(evidence['additional_variant_runs']),
          'actual_queue_selection_verified': False, 'performance_measured': False,
          'notes': 'Existing3 providers validate exact method contracts and typed field chains for original3.83.74/3.83.93.1; unknown structures fail before changes. Queue provider/extension and prior3 extension bytes unchanged. No device performance claim.'}
    (args.dist / QA).write_bytes(json_bytes(qa))
    print('PASS Berry v1.3.1 bundle272: distributed JVM/DEX, all-version metadata, official Manager dev16 selection, synthetic and two-version APK validation', flush=True)


if __name__ == '__main__':
    main()
