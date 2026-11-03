#!/usr/bin/env python3
"""Replace the space helper in the pinned 1.8.31 JVM and Android loaders."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
MAIN = 'app/hiro/swiftkey/patches/JapaneseSamsungEmojiPatch.class'
HELPER = 'app/hiro/swiftkey/patches/SpaceConversionPatch.class'
PINS = {
    'single': '642bc5e1338021d472e064919b6459d87b97e9595635190d315d8e22e06fd719',
    'bundle': '217c4d2b7b68a00cff4fea5ad5efcc86a9117e96448e122051c4c7e8b48841ca',
    'main': '35584d5052ad59b6b7951eeda8e1aa3201e10b43b259f2fcc028534f11bfda39',
    'helper': '2126c4f32be0f73e1aab41a9150468680a45364c1b988a84b2b4398af858eaee',
    'single_dex': '64696b80f270d7e1ee41dc717cb85a74fc23cd79ba15d4f9cfe6e69c4398eed9',
    'bundle_dex': '77a626fec73cc808ed29011cdc3b54736b03ac73e6d6174a2d9542c8bb688c0d',
}
TOOLS = {
    'morphe.jar': 'b98ca01f1eaac8ea66a8c44a0555a08a9d200e8847dcc200723e5574b8d67c58',
    'morphe-current.jar': 'a51472252cd63b2eac635dbb5a154ff918bfc15268da89679feb3d760230c573',
    'r8.jar': 'badba8e0fd96dc9f41f53a0686f20fc08f1172a9816461fec13b6635b7c883fa',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
}
APK_SHA = 'e9f400945e7012148eda86b6039c905566312fb1a0cf39d0360f0ccccb0124f7'


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def run(cmd, log):
    with log.open('w') as out:
        result = subprocess.run(list(map(str, cmd)), stdout=out, stderr=subprocess.STDOUT)
    require(result.returncode == 0, 'Failed; inspect ' + str(log) + '\n' + log.read_text()[-5000:])


def put(z, name, raw):
    info = zipfile.ZipInfo(name, (2026, 11, 3, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    z.writestr(info, raw)


def classpath(*paths):
    return os.pathsep.join(map(str, paths))


def main():
    parser = argparse.ArgumentParser()
    for name in ('single', 'bundle', 'tools', 'work', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    for name in ('single', 'bundle', 'tools', 'work', 'output'):
        setattr(args, name, getattr(args, name).resolve())
    require(not args.work.exists(), 'Fresh work directory required')
    args.work.mkdir(parents=True)
    args.output.mkdir(parents=True, exist_ok=True)
    for name, expected in TOOLS.items():
        require(sha((args.tools / name).read_bytes()) == expected, 'Wrong tool ' + name)
    inputs = {}
    for kind in ('single', 'bundle'):
        path = getattr(args, kind)
        require(sha(path.read_bytes()) == PINS[kind], 'Wrong baseline ' + kind)
        with zipfile.ZipFile(path) as z:
            require(len(z.namelist()) == len(set(z.namelist())), 'Duplicate baseline archive entries')
            require(sha(z.read(MAIN)) == PINS['main'], 'Unexpected JVM main baseline')
            require(sha(z.read(HELPER)) == PINS['helper'], 'Unexpected JVM helper baseline')
            require(sha(z.read('classes.dex')) == PINS[kind + '_dex'], 'Unexpected Android baseline')
            inputs[kind] = {'mpp_sha256': PINS[kind], 'android_sha256': PINS[kind + '_dex'],
                            'jvm_sha256': PINS['main'], 'helper_jvm_sha256': PINS['helper']}
    baseline = args.work / 'baseline'
    baseline.mkdir()
    with zipfile.ZipFile(args.single) as z:
        for name in z.namelist():
            if name.endswith('.class'):
                target = baseline / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(name))
    classes, helper, helperdex = [args.work / name for name in ('classes', 'helper', 'helper-dex')]
    for path in (classes, helper, helperdex):
        path.mkdir()
    cp = classpath(args.tools / 'morphe.jar', baseline)
    run(['java', 'com.sun.tools.javac.Main', '--release', '8', '-g:none', '-cp', cp, '-d', helper,
         ROOT / 'app/hiro/swiftkey/patches/SpaceConversionPatch.java'], args.work / 'compile-helper.log')
    require([str(p.relative_to(helper)) for p in helper.rglob('*.class')] == [HELPER],
            'Helper must compile to exactly one class')
    run(['java', 'com.sun.tools.javac.Main', '-cp', classpath(helper, args.tools / 'morphe.jar', baseline,
         args.tools / 'r8.jar'), '-d', classes, ROOT / 'UpdateSpaceLoader.java',
         ROOT / 'app/hiro/swiftkey/patches/NativeSpaceProbe.java'], args.work / 'compile-tools.log')
    with zipfile.ZipFile(args.work / 'helper.jar', 'w') as z:
        put(z, HELPER, (helper / HELPER).read_bytes())
    with zipfile.ZipFile(args.work / 'baseline.jar', 'w') as z:
        for path in sorted(baseline.rglob('*.class')):
            put(z, str(path.relative_to(baseline)), path.read_bytes())
    run(['java', '-cp', args.tools / 'r8.jar', 'com.android.tools.r8.D8', '--release', '--min-api', '26',
         '--lib', args.tools / 'android.jar', '--classpath', args.tools / 'morphe.jar',
         '--classpath', args.work / 'baseline.jar', '--output', helperdex, args.work / 'helper.jar'],
        args.work / 'd8-helper.log')
    java = os.environ.get('MORPHE_JAVA')
    if java is None:
        bundled = sorted(args.tools.glob('jdk-21*/bin/java'))
        java = str(bundled[0]) if bundled else 'java'
    runtimecp = classpath(classes, helper, args.tools / 'morphe.jar', baseline, args.tools / 'r8.jar')
    jvm = args.output / 'jvm'
    jvm.mkdir(exist_ok=True)
    run([java, '-cp', runtimecp, 'UpdateSpaceLoader', 'jvm', args.single, helper, jvm],
        args.work / 'replace-jvm.log')
    (args.output / 'SpaceConversionPatch.class').write_bytes((jvm / HELPER).read_bytes())
    for kind in ('single', 'bundle'):
        run([java, '-cp', runtimecp, 'UpdateSpaceLoader', 'dex', getattr(args, kind),
             helperdex / 'classes.dex', args.output / (kind + '-classes.dex')],
            args.work / (kind + '-replace-dex.log'))
    native_runs = {}
    for runtime in ('morphe.jar', 'morphe-current.jar'):
        target = args.work / ('native-tests-' + runtime.removesuffix('.jar'))
        probe_cp = classpath(classes, helper, args.tools / runtime, baseline, args.tools / 'r8.jar')
        run([java, '-cp', probe_cp, 'app.hiro.swiftkey.patches.NativeSpaceProbe',
             ROOT / 'tests/fixtures', target, baseline / HELPER], args.work / (runtime + '-native-tests.log'))
        data = json.loads((target / 'native-space-report.json').read_text())
        require(data['status'] == 'PASSED', 'Native tests failed for ' + runtime)
        native_runs[runtime] = data
    native = dict(native_runs['morphe.jar'])
    native['morphe_runtime_tests'] = native_runs
    native['morphe_runtime_sha256'] = {name: TOOLS[name] for name in native_runs}
    native['reference_apk_sha256'] = APK_SHA
    native['fixtures_sha256'] = {
        str(path.relative_to(ROOT / 'tests/fixtures')): sha(path.read_bytes())
        for path in sorted((ROOT / 'tests/fixtures').rglob('*.smali'))}
    native['fixture_provenance'] = ('Exact selected native methods from the preserved modified SwiftKey Beta '
                                    '9.13.16.4 reference APK; not an untouched APK or a device run.')
    (args.output / 'native-space-report.json').write_text(json.dumps(native, indent=2, sort_keys=True) + '\n')
    result = {
        'status': 'PASSED',
        'single_android_sha256': sha((args.output / 'single-classes.dex').read_bytes()),
        'bundle_android_sha256': sha((args.output / 'bundle-classes.dex').read_bytes()),
        'jvm_sha256': sha((jvm / MAIN).read_bytes()),
        'helper_jvm_sha256': sha((jvm / HELPER).read_bytes()),
        'replaced_loader_helper': HELPER,
        'loader_classes_added': 0,
        'loader_classes_replaced': 1,
        'jvm_main_and_bridges_byte_exact': True,
        'android_main_and_bridges_canonical_preserved': True,
        'unrelated_android_classes_preserved': True,
        'unrelated_jvm_classes_preserved': True,
        'changed_android_classes': ['Lapp/hiro/swiftkey/patches/SpaceConversionPatch;'],
        'jvm_output_changes_only_helper': True,
        'android_serialization_canonical_preservation': True,
        'native_report': native,
        'input_pins': inputs,
        'tool_pins': TOOLS,
        'device_tested': False,
        'original_apk_apply_tested': False,
    }
    (args.output / 'loader-report.json').write_text(json.dumps(result, indent=2, sort_keys=True) + '\n')
    print(json.dumps(result))


if __name__ == '__main__':
    main()
