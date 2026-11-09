#!/usr/bin/env python3
"""Build QuickSearch 1.0.2 and bundle371 from the pinned published bundle370.

Original APKs and unsigned local test outputs are never release artifacts.
Host decisions and APK transformations do not establish Android UI behavior.
"""
import argparse
from collections import Counter
from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_SHA = 'e15c1b809ea88ba917ec7d18880bbf5dbaf603e2402694cd2cf0a0685fc77894'
APK_SHA = '234168150a529a256cadae7666f7ec86f795c276714682ad6f11a346084c8f46'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-2.2.jar': '855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}
MF = 'META-INF/MANIFEST.MF'
PREFIX = 'app/hiro/quicksearch/patches/'
EXT = 'extensions/quicksearch_recents.mpe'
NAME = '検索後の終了時に履歴へ残さない'
PACKAGE = 'jp.ddo.sugihiro.quicksearch'
SINGLE = 'QuickSearch_NoRecents_v1.0.2.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.371.mpp'
QA = 'QA_QuickSearch_v1.0.2.json'


def require(value, why):
    if not value:
        raise RuntimeError(why)


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def archive(path):
    with zipfile.ZipFile(path) as z:
        require(z.testzip() is None, 'Corrupt ZIP')
        require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate ZIP entry')
        require(all(not Path(n).is_absolute() and '..' not in Path(n).parts for n in z.namelist()), 'Unsafe ZIP path')
        return {n: z.read(n) for n in z.namelist() if not n.endswith('/')}


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    return dict(row.decode().split(': ', 1) for row in rows)


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
    result = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(result) == fields, 'Manifest roundtrip differs')
    return result


def write_zip(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name in sorted(entries, key=lambda n: (n != MF, n != 'classes.dex', n)):
            info = zipfile.ZipInfo(name, (2026, 11, 9, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            z.writestr(info, entries[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def classes(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}


def source_hashes():
    return {p.relative_to(ROOT).as_posix(): sha(p) for p in sorted(ROOT.rglob('*'))
            if p.is_file() and p.suffix in ('.java', '.py') and '__pycache__' not in p.parts}


def listing_blocks(value):
    parts = re.split(r'(?m)^(?:INFO: )?Index: [0-9]+\s*\n', value)[1:]
    require(parts and all(p.startswith('Name: ') for p in parts), 'Invalid patch listing')
    return Counter(p.strip() for p in parts)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base', 'apk', 'tools', 'java-home', 'work', 'dist'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--timestamp', required=True)
    args = parser.parse_args()
    for key in ('base', 'apk', 'tools', 'java_home', 'work', 'dist'):
        setattr(args, key, getattr(args, key).resolve())
    datetime.strptime(args.timestamp, '%Y-%m-%dT%H:%M:%S')
    require(sha(args.base) == BASE_SHA and sha(args.apk) == APK_SHA, 'Pinned baseline or original APK differs')
    for name, digest in TOOLS.items():
        require(sha(args.tools / name) == digest, 'Pinned tool differs: ' + name)
    require(not args.work.exists(), 'A fresh work directory is required')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'An empty dist directory is required')
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)
    java = [args.java_home / 'bin/java', '-XX:-UsePerfData']
    javac = [args.java_home / 'bin/javac', '-J-XX:-UsePerfData', '--release', '8', '-encoding', 'UTF-8']
    initial_sources = source_hashes()

    def run(command, name):
        print('RUN ' + name, flush=True)
        with (args.work / name).open('w') as log:
            result = subprocess.run(list(map(str, command)), stdout=log, stderr=subprocess.STDOUT, timeout=300)
        output = (args.work / name).read_text(errors='replace').strip()
        require(result.returncode == 0, 'Failed ' + name + '\n' + output[-12000:])
        return output

    for name in ('runtime', 'patch', 'host', 'audit', 'runtime-dex', 'patch-dex', 'applications'):
        (args.work / name).mkdir()
    base = archive(args.base)
    require(headers(base[MF])['Version'] == '1.0.370', 'Unexpected baseline version')
    require(EXT in base and any(n.startswith(PREFIX) for n in base), 'Baseline QuickSearch payload missing')
    run([*javac, '-cp', args.tools / 'android.jar', '-d', args.work / 'runtime',
         *sorted((ROOT / 'runtime').rglob('*.java'))], 'runtime-javac.log')
    run([*javac, '-cp', args.tools / 'morphe.jar', '-d', args.work / 'patch',
         *sorted((ROOT / 'patch').rglob('*.java'))], 'patch-javac.log')
    audit_compile_cp = os.pathsep.join(map(str, (args.work / 'patch', args.tools / 'morphe.jar')))
    run([*javac, '-cp', audit_compile_cp, '-d', args.work / 'audit',
         ROOT / 'test/DexAudit.java'], 'audit-javac.log')
    run([*javac, '-d', args.work / 'host', *sorted((ROOT / 'runtime').rglob('*.java')),
         *sorted((ROOT / 'test-stubs').rglob('*.java')), ROOT / 'test/SearchTaskTest.java'], 'host-javac.log')
    host = run([*java, '-ea', '-cp', args.work / 'host', 'SearchTaskTest'], 'host-tests.txt')
    guards = []
    for sdk in ('morphe.jar', 'morphe-2.2.jar'):
        cp = os.pathsep.join(map(str, (args.work / 'audit', args.work / 'patch', args.tools / sdk)))
        result = run([*java, '-Xmx2g', '-ea', '-cp', cp, 'DexAudit', 'original', args.apk], sdk + '.original-audit.txt')
        guards.append({'sdk': sdk, 'result': result})
    for kind, min_api in (('runtime', '29'), ('patch', '26')):
        write_zip(args.work / (kind + '.jar'), classes(args.work / kind))
        cp = args.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar')
        run([*java, '-cp', args.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', min_api,
             '--lib', args.tools / 'android.jar', '--classpath', cp, '--output', args.work / (kind + '-dex'),
             args.work / (kind + '.jar')], kind + '-d8.log')
        require({p.name for p in (args.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    (args.work / 'base.dex').write_bytes(base['classes.dex'])
    audit_cp = os.pathsep.join(map(str, (args.work / 'audit', args.work / 'patch', args.tools / 'morphe-2.2.jar')))
    merge = run([*java, '-Xmx2g', '-ea', '-cp', audit_cp, 'DexAudit', 'merge',
                 args.work / 'base.dex', args.work / 'patch-dex/classes.dex', args.work / 'merged.dex'], 'loader-preservation.txt')
    added = {**classes(args.work / 'patch'), EXT: (args.work / 'runtime-dex/classes.dex').read_bytes()}
    require(all(n == EXT or n.startswith(PREFIX) and n.endswith('.class') for n in added), 'Unexpected MPP payload')
    preserved = {n: raw for n, raw in base.items() if n not in (MF, 'classes.dex', EXT) and not n.startswith(PREFIX)}
    require(not set(added).intersection(preserved), 'Unrelated payload collision')
    fields = headers(base[MF])
    fields.update(Version='1.0.371', Timestamp=args.timestamp,
                  Description='QuickSearch v1.0.2: keep search open on outside touches; retain X/Back exit and support Patcher2.2. Other bundle370 payloads retained. Android UI untested.')
    combined = {**preserved, **added, 'classes.dex': (args.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    fields.update(Name='QuickSearch No Recents', Version='1.0.2',
                  Description='QuickSearch0.4.5(36): outside touches keep search open. X/Back/task cleanup/search settings retained. Patcher2.2 compatible. Android UI untested.')
    standalone = {**added, 'classes.dex': (args.work / 'patch-dex/classes.dex').read_bytes(), MF: manifest(fields)}
    require(all(combined[n] == raw for n, raw in preserved.items()), 'Unrelated MPP payload changed')
    for name, data in ((SINGLE, standalone), (BUNDLE, combined)):
        write_zip(args.dist / name, data)
        require(archive(args.dist / name) == data, 'Written MPP differs')
    listings = []
    for sdk in ('morphe.jar', 'morphe-2.2.jar'):
        old = run([*java, '-jar', args.tools / sdk, 'list-patches', '-p', '-v', '--patches', args.base], sdk + '.old-list.txt')
        new = run([*java, '-jar', args.tools / sdk, 'list-patches', '-p', '-v', '--patches', args.dist / BUNDLE], sdk + '.new-list.txt')
        old_blocks, new_blocks = listing_blocks(old), listing_blocks(new)
        own_old = Counter({k: v for k, v in old_blocks.items() if k.startswith('Name: ' + NAME + '\n')})
        own_new = Counter({k: v for k, v in new_blocks.items() if k.startswith('Name: ' + NAME + '\n')})
        require(sum(own_old.values()) == 1 and sum(own_new.values()) == 1
                and old_blocks - own_old == new_blocks - own_new, 'Unrelated patch metadata changed')
        single_list = run([*java, '-jar', args.tools / sdk, 'list-patches', '-p', '-v', '-f', PACKAGE,
                           '--patches', args.dist / SINGLE], sdk + '.single-list.txt')
        require(single_list.count('Name: ' + NAME + '\n') == 1 and '0.4.5' in single_list, 'Standalone selection failed')
        listings.append({'sdk': sdk, 'unrelated_providers_preserved': sum(old_blocks.values()) - 1, 'quicksearch_matches': 1})
    applications = []
    outputs = []
    input_entries = archive(args.apk)
    for label, artifact, sdk in (('single-modern', SINGLE, 'morphe-2.2.jar'),
                                  ('bundle-modern', BUNDLE, 'morphe-2.2.jar'),
                                  ('single-legacy', SINGLE, 'morphe.jar')):
        output = args.work / 'applications' / label
        output.mkdir()
        target, report = output / 'patched.apk', output / 'morphe.json'
        run([*java, '-Xmx2g', '-jar', args.tools / sdk, 'patch', args.apk, '-p', args.dist / artifact,
             '--exclusive', '-e', NAME, '--unsigned', '--bytecode-mode', 'STRIP_FAST',
             '-o', target, '-r', report, '-t', output / 'tmp'], label + '.apply.log')
        result = json.loads(report.read_text())
        selected = [item['name'] if isinstance(item, dict) else item for item in result.get('appliedPatches', [])]
        steps = result.get('stepResults', result.get('patchingSteps', []))
        # Desktop 1.17 reports per-step success without a top-level success key.
        # Desktop 2.0/Patcher2.2 has both. Require the documented shape for each.
        schema_success = (result.get('success') is True if sdk == 'morphe-2.2.jar'
                          else 'success' not in result and isinstance(result.get('patchingSteps'), list))
        require(target.exists() and schema_success and not result.get('failedPatches') and selected == [NAME]
                and result.get('packageName') == PACKAGE and result.get('packageVersion') == '0.4.5',
                'Patch did not execute successfully')
        require(steps and all(s.get('success') is True for s in steps)
                and {'PATCHING', 'REBUILDING'}.issubset({s.get('step') for s in steps}), 'Rebuild steps incomplete')
        cp = os.pathsep.join(map(str, (args.work / 'audit', args.work / 'patch', args.tools / sdk)))
        audit = run([*java, '-Xmx2g', '-ea', '-cp', cp, 'DexAudit', 'patched', args.apk, target], label + '.audit.txt')
        result_entries = archive(target)
        retained = {n: raw for n, raw in input_entries.items() if not re.fullmatch(r'classes(?:\d+)?\.dex', n) and not n.startswith('META-INF/')}
        require(all(result_entries.get(n) == raw for n, raw in retained.items()), 'Original non-DEX app resources changed')
        applications.append({'label': label, 'artifact': artifact, 'sdk': sdk, 'artifact_sha256': sha(args.dist / artifact),
                             'input_sha256': sha(args.apk), 'output_sha256': sha(target), 'steps': steps,
                             'original_non_dex_resources_preserved': len(retained), 'result': 'PASS', 'audit': audit})
        outputs.append(result_entries)
    require(outputs[0] == outputs[1], 'Standalone/integrated APK entries differ')
    require(source_hashes() == initial_sources, 'Source changed during build')
    qa = {'schema': 'quicksearch102-qa-v1', 'bundle_version': '1.0.371', 'patch_version': '1.0.2',
          'baseline_version': '1.0.370', 'baseline_sha256': BASE_SHA, 'target_package': PACKAGE,
          'target_version': '0.4.5', 'target_version_code': 36, 'original_apk_sha256': APK_SHA,
          'release_timestamp': args.timestamp, 'release_timezone': 'Asia/Tokyo', 'runtime_change': 'bindMain: dialog.setCanceledOnTouchOutside(false)',
          'host_test_result': host, 'original_apk_guards': guards, 'loader_preservation': merge,
          'unrelated_zip_entries_byte_identical': len(preserved), 'patch_listing': listings,
          'apk_application': applications, 'standalone_and_integrated_apk_entries_identical': True,
          'source_sha256': initial_sources, 'toolchain_sha256': TOOLS,
          'artifacts': {n: {'bytes': (args.dist / n).stat().st_size, 'sha256': sha(args.dist / n)} for n in (SINGLE, BUNDLE)},
          'physical_android_tested': False, 'android_emulator_tested': False, 'original_app_binary_distributed': False,
          'limitations': ['Host fakes and actual APK/DEX audits do not emulate Android WindowManager or Samsung IME.',
                          'Outside taps and Recents UI have not been tested on a physical Android device or emulator.']}
    (args.dist / QA).write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'status': 'PASS', 'artifacts': qa['artifacts'], 'host': host}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
