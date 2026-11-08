#!/usr/bin/env python3
"""Build and verify the Maps bicycle-marker repair from immutable 369.

The release build requires the real native guards, behavior tests, and APK audit.
No Android UI execution is implied by successful host/DEX verification.
"""
from collections import Counter
from datetime import datetime
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
VERSION, BASE_VERSION, BUNDLE_VERSION = '1.0.1', '1.0.369', '1.0.370'
BASE_SHA = '6914c6dd5878e4c3dfffeb6bc284ec73f0e1f9915310ef9b1da19eab4f1d01ad'
PREVIOUS_FOUR_SHA = 'b14644953b807eae4ed8b026aa6445b3806e9acab766e0d623f47e72af66de27'
OLD_SINGLE_SHA = '83f6d9e7d04887dced6f28c7536646db50b65db84f84b1a9098c133f81eaa5f9'
OLD_FIVE_SHA = 'accb87a4b02fe48248580cff50ac1b73e7f00016e0b9848ed1ea94eb09928aed'
APK_SHA = '7de0b555219eca65d40f751e64381e55cbd817a0c393fbd363a5d89feb17c063'
REPORTED_SHA = '238b2ed13f530e223f090625c30753d2c01bcb24cbfb9986059f23e14f290d71'
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-2.2.jar': '855dd2d6fa5ed997a5884a44f8807c9b5bc95e69b5ebd58dfd7e8b897c86e2d7',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
}
MF = 'META-INF/MANIFEST.MF'
EXT = 'extensions/maps_hide_bicycle_marks.mpe'
OLD_RUNTIME = 'contracts/maps_hide_bicycle_marks_v1.0.0.dex'
PREFIX = 'app/hiro/mapsbikes/patches/'
NAME = '地図上の自転車マークを非表示'
EXISTING_NAMES = [
    '起動時に現在地を表示', '検索欄の下のカテゴリ行を非表示',
    '戻るで終了して履歴から削除', '店名検索を厳密にする',
]
PACKAGES = ('org.ungoogled.android.apps.maps.microg', 'com.google.android.apps.maps')
SINGLE = 'Maps_Hide_Bicycle_Marks_v1.0.1.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.370.mpp'
QA = 'QA_Maps_Hide_Bicycle_Marks_v1.0.1.json'


def require(ok, why):
    if not ok:
        raise RuntimeError(why)


def stream_sha(stream):
    result = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b''):
        result.update(chunk)
    return result.hexdigest()


def sha(path):
    with Path(path).open('rb') as source:
        return stream_sha(source)


def archive(path):
    with zipfile.ZipFile(path) as source:
        require(source.testzip() is None, 'Corrupt ZIP: ' + str(path))
        require(len(source.namelist()) == len(set(source.namelist())), 'Duplicate ZIP entry')
        return {name: source.read(name) for name in source.namelist() if not name.endswith('/')}


def entry_hashes(path):
    with zipfile.ZipFile(path) as source:
        require(len(source.namelist()) == len(set(source.namelist())), 'Duplicate APK entry')
        result = {}
        for entry in source.infolist():
            if not entry.is_dir():
                with source.open(entry) as content:
                    result[entry.filename] = (entry.file_size, stream_sha(content))
        return result


def headers(raw):
    rows = []
    for line in raw.replace(b'\r\n', b'\n').split(b'\n'):
        if line.startswith(b' '):
            require(bool(rows), 'Invalid manifest continuation')
            rows[-1] += line[1:]
        elif line:
            rows.append(line)
    pairs = [row.decode().split(': ', 1) for row in rows]
    require(all(len(pair) == 2 for pair in pairs), 'Invalid manifest header')
    require(len(pairs) == len(dict(pairs)), 'Duplicate manifest header')
    return dict(pairs)


def manifest(fields):
    lines = []
    for key, value in fields.items():
        require(isinstance(value, str) and not any(c in value for c in '\r\n\0'), 'Invalid manifest value')
        raw, prefix = (key + ': ' + value).encode(), b''
        while len(prefix) + len(raw) > 72:
            cut = 72 - len(prefix)
            while raw[cut] & 192 == 128:
                cut -= 1
            lines.append(prefix + raw[:cut])
            raw, prefix = raw[cut:], b' '
        lines.append(prefix + raw)
    result = b'\r\n'.join(lines) + b'\r\n\r\n'
    require(headers(result) == fields, 'Manifest round trip differs')
    return result


def write_zip(path, data):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as output:
        for name in sorted(data, key=lambda item: (item != MF, item != 'classes.dex', item)):
            entry = zipfile.ZipInfo(name, (2026, 11, 9, 0, 0, 0))
            entry.external_attr = 0o100644 << 16
            output.writestr(entry, data[name], compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)


def classes(folder):
    return {p.relative_to(folder).as_posix(): p.read_bytes() for p in sorted(folder.rglob('*.class'))}


def sources():
    return {str(p.relative_to(ROOT)): sha(p) for p in sorted(ROOT.rglob('*'))
            if p.is_file() and p.suffix in ('.java', '.py') and '__pycache__' not in p.parts}


def listing_blocks(listing):
    blocks = re.split(r'(?m)^(?:INFO: )?Index: [0-9]+\s*\n', listing)[1:]
    require(blocks and all(block.startswith('Name: ') for block in blocks), 'Unrecognized patch listing')
    return Counter(block.strip() for block in blocks)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('base', 'tools', 'work', 'dist', 'apk', 'reported-apk', 'java-home', 'old-bikes-single-apk', 'old-bikes-apk', 'legend'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--reference-dir', type=Path,
                        help='Optional verified baseline cache containing patched.apk, morphe.json, provenance.json')
    parser.add_argument('--timestamp', required=True, help='JST release timestamp, YYYY-MM-DDTHH:MM:SS')
    args = parser.parse_args()
    for key in ('base', 'tools', 'work', 'dist', 'apk', 'reported_apk', 'java_home', 'old_bikes_single_apk', 'old_bikes_apk', 'legend'):
        setattr(args, key, getattr(args, key).resolve())
    if args.reference_dir:
        args.reference_dir = args.reference_dir.resolve()
    datetime.strptime(args.timestamp, '%Y-%m-%dT%H:%M:%S')
    require(sha(args.base) == BASE_SHA and sha(args.apk) == APK_SHA and sha(args.reported_apk) == REPORTED_SHA
            and sha(args.old_bikes_single_apk) == OLD_SINGLE_SHA and sha(args.old_bikes_apk) == OLD_FIVE_SHA
            and sha(args.legend) == 'c63aaba03ff7c812c6644ad489f75812c62465e87c63a36d09cccd7d839dfe2d',
            'A pinned input differs')
    for name, digest in TOOLS.items():
        require(sha(args.tools / name) == digest, 'Pinned tool differs: ' + name)
    require(not args.work.exists(), 'Fresh build work directory required')
    require(not args.dist.exists() or not any(args.dist.iterdir()), 'Empty dist required')
    for required in ('patch/MapsHideBicycleMarksPatch.java', 'test/MapsBikesAudit.java',
                     'test/BundleAudit.java', 'runtime/BicycleMarkerFilter.java'):
        require((ROOT / required).is_file(), 'Required production/audit source missing: ' + required)
    args.work.mkdir(parents=True)
    args.dist.mkdir(parents=True, exist_ok=True)
    java, javac = args.java_home / 'bin/java', args.java_home / 'bin/javac'
    source_hashes = sources()

    def run(command, name, timeout=900):
        print('RUN ' + name, flush=True)
        with (args.work / name).open('w') as log:
            result = subprocess.run(list(map(str, command)), stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
        output = (args.work / name).read_text()
        require(result.returncode == 0, 'Failed ' + name + '\n' + output[-18000:])
        return output.strip()

    for name in ('patch', 'runtime', 'patch-dex', 'runtime-dex', 'test', 'host', 'applications'):
        (args.work / name).mkdir()
    base = archive(args.base)
    require(headers(base[MF])['Version'] == BASE_VERSION, 'Wrong published baseline version')
    require(EXT in base and any(n.startswith(PREFIX) for n in base),
            'Published bicycle patch is missing from baseline369')
    (args.work / 'patch/contracts').mkdir()
    (args.work / 'patch' / OLD_RUNTIME).write_bytes(base[EXT])
    runtime_sources = sorted((ROOT / 'runtime').rglob('*.java'))
    patch_sources = sorted((ROOT / 'patch').rglob('*.java'))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
         '-d', args.work / 'runtime', *runtime_sources], 'runtime-javac.log')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'morphe.jar',
         '-d', args.work / 'patch', *patch_sources], 'patch-javac.log')
    compile_cp = os.pathsep.join(map(str, (args.work / 'patch', args.tools / 'morphe.jar')))
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', compile_cp,
         '-d', args.work / 'test', *sorted((ROOT / 'test').rglob('*.java'))], 'audit-javac.log')
    host_sources = sorted((ROOT / 'test-host').rglob('*.java'))
    require(host_sources, 'Runtime behavior tests missing')
    run([javac, '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / 'android.jar',
         '-d', args.work / 'host', *runtime_sources, *host_sources], 'host-javac.log')
    host = run([java, '-ea', '-cp', args.work / 'host', 'app.hiro.mapsbikes.BicycleMarkerFilterTest'], 'runtime-tests.txt')
    require('PASS' in host, 'Incomplete runtime verification')
    legend_tests = run([java, '-ea', '-cp', args.work / 'host', 'app.hiro.mapsbikes.BicycleLegendFilterTest', args.legend], 'legend-tests.txt')
    require('PASS' in legend_tests, 'Incomplete real legend verification')
    guard_results = []

    def guards(label, apk):
        for sdk in ('morphe.jar', 'morphe-2.2.jar'):
            cp = os.pathsep.join(map(str, (args.work / 'test', args.work / 'patch', args.tools / sdk)))
            result = run([java, '-Xmx2g', '-ea', '-cp', cp, 'MapsBikesAudit', 'guards', apk], sdk + '.' + label + '.guards.txt')
            require('PASS' in result, 'Incomplete native contract verification')
            guard_results.append({'sdk': sdk, 'input': label, 'sha256': sha(apk), 'result': result})

    guards('v285', args.apk)
    guards('v287', args.reported_apk)
    for kind, api in (('runtime', '23'), ('patch', '26')):
        write_zip(args.work / (kind + '.jar'), classes(args.work / kind))
        run([java, '-cp', args.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', api,
             '--lib', args.tools / 'android.jar', '--classpath', args.tools / ('android.jar' if kind == 'runtime' else 'morphe.jar'),
             '--output', args.work / (kind + '-dex'), args.work / (kind + '.jar')], kind + '-d8.log')
    (args.work / 'base.dex').write_bytes(base['classes.dex'])
    audit_cp = os.pathsep.join(map(str, (args.work / 'test', args.work / 'patch', args.tools / 'morphe-2.2.jar')))
    merge = run([java, '-Xmx2g', '-ea', '-cp', audit_cp, 'BundleAudit',
                 args.work / 'base.dex', args.work / 'patch-dex/classes.dex', args.work / 'merged.dex'], 'loader-preservation.txt')
    added = {**classes(args.work / 'patch'), EXT: (args.work / 'runtime-dex/classes.dex').read_bytes(), OLD_RUNTIME: base[EXT]}
    (args.work / 'patch/extensions').mkdir()
    (args.work / 'patch' / EXT).write_bytes(added[EXT])
    require(all(n in (EXT, OLD_RUNTIME) or n.startswith(PREFIX) and n.endswith('.class') for n in added), 'Unexpected new MPP payload')
    require(all(n in (EXT,) or n.startswith(PREFIX) for n in set(added) & set(base)), 'Repair collides with an unrelated payload')
    preserved = {name: value for name, value in base.items() if name not in (MF, 'classes.dex', EXT) and not name.startswith(PREFIX)}
    fields = headers(base[MF])
    fields.update(Version=BUNDLE_VERSION, Timestamp=args.timestamp,
                  Description='Maps bicycle markers v1.0.1: repair missing asset-name and map rendering paths; upgrade exact v1.0.0 installations. Only bicycle provider/runtime replaced from bundle369; all unrelated providers and payloads preserved. Android UI not verified.')
    combined = {**preserved, **added, 'classes.dex': (args.work / 'merged.dex').read_bytes(), MF: manifest(fields)}
    single = {**added, 'classes.dex': (args.work / 'patch-dex/classes.dex').read_bytes(),
              MF: manifest({**fields, 'Name': 'Maps Hide Bicycle Marks', 'Version': VERSION})}
    require(all(combined[name] == value for name, value in preserved.items()), 'Existing bundle payload changed')
    for name, data in ((SINGLE, single), (BUNDLE, combined)):
        write_zip(args.dist / name, data)
        require(archive(args.dist / name) == data, 'Written artifact differs from verified entries')
    selection = []
    for name in (SINGLE, BUNDLE):
        for sdk in ('morphe.jar', 'morphe-2.2.jar'):
            for package in PACKAGES:
                listing = run([java, '-jar', args.tools / sdk, 'list-patches', '--patches', args.dist / name,
                               '-f', package, '-p', '-v'], name + '.' + sdk + '.' + package + '.listing.txt')
                require(listing.count('Name: ' + NAME + '\n') == 1, 'Bicycle option not selectable exactly once')
                if name == BUNDLE:
                    require(all(listing.count('Name: ' + n + '\n') == 1 for n in EXISTING_NAMES), 'Existing Maps options missing')
                selection.append({'artifact': name, 'sdk': sdk, 'package': package, 'matches': 1})
    provider_results = []
    for sdk in ('morphe.jar', 'morphe-2.2.jar'):
        before = run([java, '-jar', args.tools / sdk, 'list-patches', '--patches', args.base, '-p', '-v'], sdk + '.baseline-all-providers.txt')
        after = run([java, '-jar', args.tools / sdk, 'list-patches', '--patches', args.dist / BUNDLE, '-p', '-v'], sdk + '.current-all-providers.txt')
        old, new = listing_blocks(before), listing_blocks(after)
        old_bike = Counter({block: count for block, count in old.items() if block.startswith('Name: ' + NAME + '\n')})
        new_bike = Counter({block: count for block, count in new.items() if block.startswith('Name: ' + NAME + '\n')})
        require(sum(old_bike.values()) == 1 and sum(new_bike.values()) == 1
                and old - old_bike == new - new_bike,
                'Unrelated provider metadata changed or bicycle provider duplicated')
        provider_results.append({'sdk': sdk, 'existing_providers_preserved': sum(old.values()) - 1, 'replaced_providers': 1})

    applications = []

    def apply(label, artifact, input_apk, names, reference_apk=None, audit=True):
        output = args.work / 'applications' / label
        output.mkdir()
        target, report = output / 'patched.apk', output / 'morphe.json'
        command = [java, '-Xmx3g', '-jar', args.tools / 'morphe-2.2.jar', 'patch', input_apk,
                   '-p', artifact, '--exclusive', '--unsigned', '--bytecode-mode', 'STRIP_FAST',
                   '-o', target, '-t', output / 'tmp', '-r', report]
        for name in names:
            command += ['-e', name]
        run(command, label + '.apply.log')
        result = json.loads(report.read_text())
        applied = [item['name'] if isinstance(item, dict) else item for item in result.get('appliedPatches', [])]
        require(target.exists() and result.get('success') is True and not result.get('failedPatches')
                and Counter(applied) == Counter(names), 'APK patch selection/application failed')
        steps = result.get('stepResults', [])
        require(steps and all(step.get('success') is True for step in steps)
                and {'PATCHING', 'REBUILDING'}.issubset({step.get('step') for step in steps}),
                'APK patch/rebuild steps were not successful')
        audit_result = None
        if audit:
            audit_result = run([java, '-Xmx3g', '-ea', '-cp', audit_cp, 'MapsBikesAudit', 'audit',
                                reference_apk or input_apk, target, args.work / 'runtime-dex/classes.dex'], label + '.audit.txt')
            require('PASS' in audit_result, 'Incomplete APK preservation audit')
        applications.append({'label': label, 'artifact': Path(artifact).name, 'artifact_sha256': sha(artifact),
                             'input_sha256': sha(input_apk), 'output_sha256': sha(target), 'patches': names,
                             'steps': steps, 'audit': audit_result, 'result': 'PASS'})
        return target

    standalone = apply('standalone-v285', args.dist / SINGLE, args.apk, [NAME])
    integrated = apply('integrated-v285', args.dist / BUNDLE, args.apk, [NAME])
    require(entry_hashes(standalone) == entry_hashes(integrated), 'Standalone and integrated APK contents differ')
    apply('reported-v287', args.dist / BUNDLE, args.reported_apk, [NAME])
    if args.reference_dir:
        current = args.reference_dir / 'patched.apk'
        baseline_application = json.loads((args.reference_dir / 'provenance.json').read_text())
        baseline_report_path = args.reference_dir / 'morphe.json'
        baseline_report = json.loads(baseline_report_path.read_text())
        cached_names = [item['name'] if isinstance(item, dict) else item
                        for item in baseline_report.get('appliedPatches', [])]
        cached_steps = baseline_report.get('stepResults', [])
        require(baseline_application.get('schema') == 'maps-bikes100-baseline-cache-v1'
                and baseline_application.get('label') == 'reference368-existing-four'
                and baseline_application.get('artifact_sha256') == PREVIOUS_FOUR_SHA
                and baseline_application.get('input_sha256') == APK_SHA
                and baseline_application.get('output_sha256') == sha(current)
                and baseline_application.get('report_sha256') == sha(baseline_report_path)
                and baseline_application.get('result') == 'PASS'
                and Counter(baseline_application.get('patches', [])) == Counter(EXISTING_NAMES),
                'Cached baseline provenance or artifact hash differs')
        require(baseline_report.get('success') is True and not baseline_report.get('failedPatches')
                and Counter(cached_names) == Counter(EXISTING_NAMES)
                and cached_steps and all(step.get('success') is True for step in cached_steps)
                and {'PATCHING', 'REBUILDING'}.issubset({step.get('step') for step in cached_steps})
                and baseline_application.get('steps') == cached_steps,
                'Cached baseline patch/rebuild result differs')
        (args.work / 'baseline-reference.json').write_text(json.dumps(baseline_application, ensure_ascii=False, indent=2) + '\n')
    else:
        current = apply('reference369-existing-four', args.base, args.apk, EXISTING_NAMES, audit=False)
        baseline_application = applications.pop()
    guards('current368-existing-four', current)
    additive = apply('additive-to-existing-four', args.dist / BUNDLE, current, [NAME])
    all_five = apply('all-five-maps', args.dist / BUNDLE, args.apk, EXISTING_NAMES + [NAME], reference_apk=current)
    coexistence = run([java, '-Xmx3g', '-ea', '-cp', audit_cp, 'MapsBikesAudit', 'compare', additive, all_five], 'additive-vs-all-five.txt')
    require('PASS' in coexistence, 'Incomplete all-five coexistence verification')
    all_five_zip_identical = entry_hashes(additive) == entry_hashes(all_five)
    upgrade_guard_results = []
    for label, old_apk in (('old-single', args.old_bikes_single_apk), ('old-all-five', args.old_bikes_apk)):
        result = run([java, '-Xmx3g', '-ea', '-cp', audit_cp, 'MapsBikesAudit', 'upgrade-guards', old_apk], label + '.upgrade-guards.txt')
        require('PASS' in result, 'Old-installation guard test incomplete')
        upgrade_guard_results.append({'input': label, 'sha256': sha(old_apk), 'result': result})
    upgraded_single = apply('upgrade-old-single', args.dist / BUNDLE, args.old_bikes_single_apk, [NAME])
    upgraded_five = apply('upgrade-old-all-five', args.dist / BUNDLE, args.old_bikes_apk, [NAME])
    upgrade_equivalence = []
    for label, old_output, fresh_output in (('single', upgraded_single, standalone), ('all-five', upgraded_five, all_five)):
        result = run([java, '-Xmx3g', '-ea', '-cp', audit_cp, 'MapsBikesAudit', 'compare', old_output, fresh_output], 'upgrade-' + label + '-equivalence.txt')
        require('PASS' in result, 'Upgrade and fresh APK differ')
        upgrade_equivalence.append({'case': label, 'result': result})
    rejection = run([java, '-Xmx3g', '-ea', '-cp', audit_cp, 'MapsBikesAudit', 'reject-current', standalone], 'reapply-rejection.txt')
    require('PASS' in rejection, 'Current version reapplication not rejected')
    require(sources() == source_hashes, 'Source changed during build')
    qa = {
        'schema': 'maps-bikes101-qa-v1', 'patch_version': VERSION, 'bundle_version': BUNDLE_VERSION,
        'baseline_version': BASE_VERSION, 'baseline_sha256': BASE_SHA, 'runtime_extension': EXT,
        'release_timestamp': args.timestamp, 'release_timezone': 'Asia/Tokyo',
        'production_android_sdk_compile': True, 'runtime_android_tested': False,
        'physical_android_tested': False, 'android_emulator_tested': False,
        'renderer_scope': 'SharedLabeler and legacy point labels; selective legend filtering for native rendering', 'all_rendering_backends_verified': False,
        'runtime_limitation': 'Actual APK transformations, host regression tests, new native ingress guards and old-installation upgrades verified. No Android UI execution was available; on-device rendering and performance remain unverified.',
        'source_sha256': source_hashes, 'toolchain_sha256': TOOLS, 'host_tests': host, 'legend_tests': legend_tests, 'legend_sha256': sha(args.legend),
        'guard_results': guard_results, 'upgrade_guard_results': upgrade_guard_results, 'upgrade_equivalence': upgrade_equivalence,
        'reapply_rejection': rejection, 'verified_previous_runtime_resource': {'path': OLD_RUNTIME, 'sha256': hashlib.sha256(base[EXT]).hexdigest()},
        'selection_results': selection, 'provider_preservation': provider_results,
        'apk_application': applications, 'baseline_application': baseline_application,
        'baseline_reference_reused': bool(args.reference_dir), 'loader_preservation': merge,
        'native_scope': {
            'owners': ['Lbkbv;', 'Lbkex;', 'Lbkup;', 'Lcom/google/android/libraries/geo/mapcore/geoxp/jni/NativeMapFactory;', 'Lcom/google/android/libraries/geo/mapcore/geoxp/jni/providers/MapCoreConfigProvider;'],
            'shared_label_method': 'Lbkbv;->b(Lcift;Lbkeo;ILjava/util/List;Ljava/util/List;Lbkdc;Lbjiv;)Lbkba;',
            'shared_label_added_instructions': 5, 'shared_label_register_count': 35,
            'runtime_filter': 'Lapp/hiro/mapsbikes/BicycleMarkerFilter;->shouldHide(Ljava/lang/Object;)Z',
            'original_instructions_preserved_except_cache_namespace': True,
            'original_try_blocks_preserved': True,
            'legacy_filter': 'Lapp/hiro/mapsbikes/BicycleMarkerFilter;->shouldHideLegacy(Ljava/lang/Object;Ljava/lang/Object;)Z',
            'legend_filter': 'Lapp/hiro/mapsbikes/BicycleLegendFilter;->filter([B)[B',
            'cache_directory': 'geoxp_cache_hiro_bikes101',
            'old_online_cache_and_secure_offline_paths_preserved': True,
            'unrelated_native_classes_and_resources_preserved': True,
        },
        'preserved_unrelated_entries': len(preserved), 'unrelated_mpp_payloads_byte_identical': True,
        'single_bundle_apk_contents_equal': True, 'all_five_additive_apk_contents_equal': True,
        'all_five_additive_zip_entries_identical': all_five_zip_identical,
        'additive_all_five_equivalence': coexistence,
        'user_report': 'Bicycle icons still visible after v1.0.0, confirmed by screenshot 1000011992.png; repair missed rendering and asset-name paths.',
        'artifacts': {name: {'bytes': (args.dist / name).stat().st_size, 'sha256': sha(args.dist / name)} for name in (SINGLE, BUNDLE)},
    }
    (args.dist / QA).write_text(json.dumps(qa, ensure_ascii=False, indent=2) + '\n')
    print('PASS BUILD ' + json.dumps(qa['artifacts']), flush=True)


if __name__ == '__main__':
    main()
