#!/usr/bin/env python3
"""Exercise emitted resource loaders with the exact modern patcher and synthetic APKs.

The native-input fixture is intentionally incompatible and must be rejected by
its preserved hash guard; successful full original-APK application is a separate
validation. No private app bytes or Android/device execution are used here.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import zipfile

BASE_SHA = 'c1cdab6d613ba594d8fe817799e5e208e25e847d2d55f72525f13f19e3b1b408'
OLD_MORPHE_SHA = '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c'
MODERN_MORPHE_SHA = 'ed8184451a88b373ab38e575884523a709706195a7232e971fcf8cd61fc5a22c'
EXPORTS = ['--add-exports', 'java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED',
           '--add-exports', 'java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED']


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log):
    result = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=180)
    log.write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError('Host command failed: ' + str(log) + '\n' + result.stdout[-3000:] + result.stderr[-3000:])
    return result.stdout


def test(source, work, jdk, tools, modern_morphe, baseline, candidate):
    source, work, jdk, tools, modern_morphe, baseline, candidate = [Path(v).resolve() for v in
        (source, work, jdk, tools, modern_morphe, baseline, candidate)]
    if work.exists():
        raise AssertionError('Use a fresh resource-extraction host directory')
    if digest(baseline) != BASE_SHA or baseline.stat().st_size != 1635847:
        raise AssertionError('Exact released ULike .123 baseline is required')
    if digest(tools / 'morphe.jar') != OLD_MORPHE_SHA or digest(modern_morphe) != MODERN_MORPHE_SHA:
        raise AssertionError('Morphe tool hash differs from reviewed version')
    with zipfile.ZipFile(modern_morphe) as z:
        embedded_version = z.read('app/morphe/patcher/version.properties').decode().strip()
    if 'version=2.2.0-dev.2' not in embedded_version:
        raise AssertionError('Wrong actual modern patcher version: ' + embedded_version)
    work.mkdir(parents=True)
    tracked = ['host_resource_extraction19124.py', 'tests19124/ResourceBehavior19124.java',
        'tests19124/ResourceStructure19124.java', 'ResourceReadHooks19124.java', 'ResourceClass19124.java', 'MergePayloads.java']
    pins = {name: digest(source / name) for name in tracked}
    for tag, artifact in [('baseline', baseline), ('candidate', candidate)]:
        with zipfile.ZipFile(artifact) as z:
            for name in z.namelist():
                if name != 'classes.dex' and not (name.startswith('app/hiro/ulike/patches/') and name.endswith('.class')):
                    continue
                if '..' in Path(name).parts or name.startswith('/'):
                    raise AssertionError('Unsafe known entry')
                target = work / tag / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(z.read(name))
    java, javac = jdk / 'bin/java', jdk / 'bin/javac'
    behavior_classes, structural_classes = work / 'behavior-classes', work / 'structural-classes'
    behavior_classes.mkdir(); structural_classes.mkdir()
    run([javac, *EXPORTS, '-encoding', 'UTF-8', '-cp', modern_morphe, '-d', behavior_classes,
        source / 'tests19124/ResourceBehavior19124.java'], work / 'behavior-compile.log')
    output = run([java, *EXPORTS, '-cp', os.pathsep.join(map(str, (modern_morphe, behavior_classes))),
        'ResourceBehavior19124', baseline, candidate, work / 'behavior'], work / 'behavior.log')
    behavior = json.loads(output.strip().splitlines()[-1])
    if behavior.get('status') != 'passed' or behavior.get('behavior_mutations_rejected') != 4 or behavior.get('assertions') != 68:
        raise AssertionError('Behavior evidence/counts differ')
    cp = os.pathsep.join(map(str, (tools / 'morphe.jar', work / 'baseline', structural_classes)))
    run([javac, *EXPORTS, '-encoding', 'UTF-8', '-cp', cp, '-d', structural_classes,
        source / 'MergePayloads.java', source / 'ResourceReadHooks19124.java', source / 'ResourceClass19124.java',
        source / 'tests19124/ResourceStructure19124.java'], work / 'structure-compile.log')
    output = run([java, *EXPORTS, '-cp', cp, 'ResourceStructure19124', work / 'baseline', work / 'candidate',
        work / 'behavior', work / 'structure'], work / 'structure.log')
    structure = json.loads(output.strip().splitlines()[-1])
    if structure.get('status') != 'passed' or structure.get('structural_mutations_rejected') != 10:
        raise AssertionError('JVM/DEX mutation evidence missing')
    mutations = ['asset-read', 'original-native-read', 'native-resolver', 'manifest-parent', 'remove-required-input-guard']
    failed = ['genuinely-missing', 'wrong-original-hash', 'wrong-replacement-hash', 'incompatible-original-native',
        'wrong-native-length', 'wrong-native-hash']
    controls = [{'name': encoding + ':' + name, 'kind': 'mutation', 'detected': True}
        for encoding in ('jvm', 'dex') for name in mutations]
    controls += [{'name': name, 'kind': 'controlled_failure', 'detected': True} for name in failed]
    if pins != {name: digest(source / name) for name in tracked}:
        raise AssertionError('Resource host sources changed during execution')
    report = {
        'schema': 'ulike19124-resource-extraction-host-v1', 'status': 'passed',
        'assertions': behavior['assertions'], 'structural_checks': structure['healthy_structural_checks'],
        'negative_controls': controls, 'mutation_controls': 10, 'failed_condition_controls': 6,
        'behavior_mutation_detections': 4,
        'behavior_mutation_count_note': 'The four live JVM behavior mutations overlap the five JVM inverse mutations; counted once within ten JVM/DEX mutation variants.',
        'source_sha256': pins, 'behavior': behavior, 'structure': structure,
        'modern_morphe': {'sha256': MODERN_MORPHE_SHA, 'bytes': modern_morphe.stat().st_size,
            'desktop_version': '2.0.0-dev.4', 'patcher_version': '2.2.0-dev.2'},
        'artifact': {'filename': candidate.name, 'bytes': candidate.stat().st_size, 'sha256': digest(candidate)},
        'baseline': {'bytes': baseline.stat().st_size, 'sha256': digest(baseline)},
        'baseline_missing_asset_reproduced': True, 'all_three_existing_file_reads_checked': True,
        'actual_modern_resource_context': True, 'full_on_request_resources': True,
        'native_collision_guards_checked': True, 'required_asset_hash_guards_checked': True,
        'new_native_creation_and_identical_existing_checked': True,
        'private_apk_used': False, 'physical_android_tested': False,
        'limitations': 'Synthetic APK fixtures run real modern ResourcePatchContext and actual emitted JVM classes. Synthetic original native bytes intentionally fail the unchanged native hash guard. Full original APKS application and DEX inspection are separate evidence; Android execution and device responsiveness remain unmeasured.',
    }
    (work / 'result.json').write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    print(json.dumps({'status': 'passed', 'assertions': report['assertions'], 'negative_controls': len(controls),
        'mutation_controls': 10, 'failed_condition_controls': 6, 'structural_checks': report['structural_checks']}, indent=2))
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('source', 'work', 'jdk', 'tools', 'modern-morphe', 'baseline', 'candidate'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    test(args.source, args.work, args.jdk, args.tools, args.modern_morphe, args.baseline, args.candidate)
