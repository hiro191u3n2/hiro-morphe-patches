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
BASE_SHA = '05064b69f2965146de6461d7cbcf25318e61f17a2f099b25d6c15c2a205c821e'
TOOLS = {n: r['sha256'] for n, r in json.loads((ROOT / 'tool-sources.json').read_bytes()).items()}
MF = 'META-INF/MANIFEST.MF'
SINGLE = 'Berry_Browser_v1.3.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.271.mpp'
QA = 'QA_Berry_Browser_v1.3.0.json'
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
    args = parser.parse_args()
    for n in ('base', 'tools', 'work', 'dist'):
        setattr(args, n, getattr(args, n).resolve())
    require(sha(args.base) == BASE_SHA, 'Published v270 baseline differs')
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
    require(fields['Version'] == '1.0.270', 'Baseline version differs')
    require(all(n in baseline for n in EXTENSIONS), 'Existing Berry extensions missing')
    require(not any(n.endswith('.class') and n.startswith(PREFIXES) for n in baseline), 'Baseline Berry JVM inventory advanced')
    for folder in ('runtime', 'patch', 'runtime-dex', 'patch-dex', 'build-tools', 'tests', 'manager-tests'):
        (args.work / folder).mkdir()
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
         '-d', args.work / 'runtime', *sorted((ROOT / 'runtime').rglob('*.java'))], 'runtime-javac.log')
    run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar',
         '-d', args.work / 'patch', *sorted((ROOT / 'patch').glob('*.java'))], 'patch-javac.log')
    host_result = run(['python3', ROOT / 'test-host/run_tests.py'], 'host-runtime-tests.txt')
    require('PASS' in host_result, 'Host runtime tests incomplete')
    for kind, min_api in (('runtime', '23'), ('patch', '26')):
        write_zip(args.work / (kind + '.jar'), class_files(args.work / kind))
        run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', min_api,
             '--lib', args.tools / 'android.jar', '--classpath', args.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', args.work / (kind + '-dex'), args.work / (kind + '.jar')], kind + '-d8.log')
        require({p.name for p in (args.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
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
    require(len(jvm) > 27 and all(n.startswith(PREFIXES) for n in jvm), 'Invalid Berry JVM inventory')
    extra = {**jvm, NEW_EXT: (args.work / 'runtime-dex/classes.dex').read_bytes()}
    fields.update(Version='1.0.271', Timestamp='2026-10-22T00:00:00',
                  Description='Berry Browser v1.3.0: Android17 OS lock-free MessageQueue targetSDK37 opt-in, public runtime diagnosis and LAN permission; all-version Manager metadata with existing guards retained; physical device not tested.')
    combined = {**baseline, **extra, MF: manifest(fields), 'classes.dex': (args.work / 'combined.dex').read_bytes()}
    sf = dict(fields)
    sf.update(Name='Berry Browser', Version='1.3.0')
    single = {**extra, **{n: baseline[n] for n in EXTENSIONS}, MF: manifest(sf), 'classes.dex': (args.work / 'berry.dex').read_bytes()}
    require(all(combined[n] == raw for n, raw in baseline.items() if n not in (MF, 'classes.dex')), 'Unrelated baseline entry changed')
    require(set(combined) - set(baseline) == set(extra), 'Unexpected bundle additions')
    for name, entries in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(args.dist / name, entries)
        require(archive(args.dist / name) == entries, 'MPP archive roundtrip differs')
    tests_cp = os.pathsep.join(map(str, (args.dist / SINGLE, args.tools / 'morphe.jar')))
    run(['javac', '-encoding', 'UTF-8', '-cp', tests_cp, '-d', args.work / 'tests',
         *sorted((ROOT / 'tests').glob('*.java'))], 'manifest-tests-javac.log')
    manifest_result = run(['java', '-ea', '-cp', os.pathsep.join((str(args.work / 'tests'), tests_cp)),
                           'ManifestContractTest'], 'manifest-contracts.txt')
    require(manifest_result.startswith('PASS ManifestContractTest:'), 'Manifest contracts incomplete')
    metadata = {}
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe22.jar', 'manager-dev15-host.jar'):
            cp = os.pathsep.join(map(str, (args.work / 'build-tools', args.tools / sdk)))
            result = run(['java', '-ea', '-cp', cp, 'BerryMetadataAudit', args.dist / name, '4'], name + '-' + sdk + '-metadata.txt')
            require(result.splitlines()[-1].startswith('PASS BerryMetadataAudit:'), 'Shipped metadata/JVM audit incomplete')
            metadata[name + ':' + sdk] = result.splitlines()[-1]
    manager_host = args.tools / 'manager-dev15-host.jar'
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
        badging = run([args.tools / 'aapt2', 'dump', 'badging', out / 'patched.apk'], label + '.badging.txt')
        target = re.search(r"targetSdkVersion:'([0-9]+)'", badging)
        require(target and int(target[1]) >= 37, 'Encoded APK target must be37+')
        require("name='android.permission.ACCESS_LOCAL_NETWORK'" in badging, 'Encoded APK lacks LAN permission')
        xml = run([args.tools / 'aapt2', 'dump', 'xmltree', out / 'patched.apk', '--file', 'AndroidManifest.xml'], label + '.manifest.txt')
        require('app.hiro.berry.runtime.BerryQueueInit' in xml and 'jp.ejimax.berrybrowser.hiro.messagequeue' in xml,
                'Encoded APK lacks startup diagnostic provider')
        run(['java', '-Xmx768m', '-ea', '-cp', auditcp, 'AppDexAudit', 'all' if len(selection) > 1 else 'queue',
             apk, out / 'patched.apk', out / 'dex-audit.json'], label + '.dex-audit.txt')
        audit = json.loads((out / 'dex-audit.json').read_bytes())
        return {'sdk': sdk, 'artifact': name, 'selected': sorted(selection), 'result': 'PASS',
                'encoded_target_sdk': int(target[1]), 'dex_audit': audit}

    synthetic = []
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe22.jar'):
            synthetic.append(apply(fixture / 'input.apk', name, sdk, [QUEUE], 'synthetic-' + name + '-' + sdk))
    evidence_path = ROOT.parent / 'publication/real-apk-evidence.json'
    if args.apk:
        apk = args.apk.resolve()
        require(sha(apk) == '4a4da9029d1d71edf376b2d1edf3636e3fba946b49d1fafa5787d20252c8ad5b', 'Reviewed saved Berry input differs')
        actual = []
        for name in (SINGLE, BUNDLE):
            for sdk in ('morphe.jar', 'morphe22.jar'):
                for selected in ([QUEUE], [*OLD, QUEUE]):
                    label = 'real-' + name + '-' + sdk + ('-all' if len(selected) > 1 else '-queue')
                    actual.append(apply(apk, name, sdk, selected, label))
        evidence = {'schema': 'berry-messagequeue130-private-apk-evidence-v1', 'result': 'PASS',
                    'package_name': 'jp.ejimax.berrybrowser', 'version_name': '3.83.74', 'version_code': '383740',
                    'input_apk_sha256': sha(apk),
                    'input_apks_sha256': '71e52ebe4ab0ad493e5c058cbf7bc361f9300652223c9abe44a7b3ae7e19b1b9',
                    'input_target_sdk': 37, 'input_min_sdk': 32,
                    'standalone_sha256': sha(args.dist / SINGLE), 'bundle_sha256': sha(args.dist / BUNDLE),
                    'physical_android_tested': False, 'actual_queue_selection_verified': False,
                    'performance_measured': False, 'runs': actual,
                    'scope': 'Saved user-provided base APK. Split install/runtime and Android17 device not tested.'}
        evidence_path.write_bytes(json_bytes(evidence))
    require(evidence_path.is_file(), 'Private APK evidence required')
    evidence = json.loads(evidence_path.read_bytes())
    require(evidence.get('result') == 'PASS' and evidence.get('standalone_sha256') == sha(args.dist / SINGLE)
            and evidence.get('bundle_sha256') == sha(args.dist / BUNDLE)
            and evidence.get('physical_android_tested') is False, 'Private APK evidence is stale or incomplete')
    # Synthetic output archives can contain builder timestamps; retain semantic audits, not output file hashes.
    for row in synthetic:
        for key in ('input_sha256', 'output_sha256', 'input_apk_sha256', 'output_apk_sha256'):
            row['dex_audit'].pop(key, None)
    qa = {'schema': 'berry-messagequeue130-qa-v1', 'result': 'PASS', 'patch_version': '1.3.0',
          'bundle_version': '1.0.271', 'baseline_version': '1.0.270', 'baseline_sha256': BASE_SHA,
          'standalone_sha256': sha(args.dist / SINGLE), 'bundle_sha256': sha(args.dist / BUNDLE),
          'physical_android_tested': False, 'real_apk_evidence_sha256': sha(evidence_path),
          'tools': TOOLS, 'host_runtime_tests': next(line for line in host_result.splitlines() if line.startswith('PASS')),
          'manifest_contracts': manifest_result, 'dex_preservation': preservation,
          'jvm_class_count': len(jvm), 'metadata': metadata, 'manager_selection': manager_result,
          'synthetic_apk_tests': synthetic, 'private_apk_runs': len(evidence['runs']),
          'actual_queue_selection_verified': False, 'performance_measured': False,
          'notes': 'Saved3.83.74 already targets37; all-version display does not bypass existing3 runtime guards; no Android16 backport.'}
    (args.dist / QA).write_bytes(json_bytes(qa))
    print('PASS Berry v1.3.0 bundle271: distributed JVM/DEX, all-version metadata, official Manager selection, synthetic and bound saved-APK validation', flush=True)


if __name__ == '__main__':
    main()
