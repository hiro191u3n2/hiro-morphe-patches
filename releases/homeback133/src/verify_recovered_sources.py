#!/usr/bin/env python3
"""Recompile corresponding sources and compare them to the preserved 317 binary.

This is a recovery verification, not a replacement build or recreated prior QA.
Runtime comparisons ignore only DEX line/local debug records; bytecode, registers,
exception handlers, annotations and every other class definition remain checked.
The saved release binary is never changed by this program.
"""
import argparse
import hashlib
import json
import os
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SAVED_SHA = '4f190bc54c91effbfa778b62bf913e2b6509f82c8e8b4f1b99b5a59ccae3a466'
BASE_SHA = 'd45359b07529e3c1bcbebfe795befeeae8ebe53e5794725c2c06750e996e06ea'
TOOLS = {
    'morphe.jar': '82a0df2ff881d83d5ca8b4f9a6ce196bd4ac3b87ff147fe37845c296b436806c',
    'android.jar': '4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b',
    'd8.jar': '305622ad00535684534eb8f742cbf5e628a9abc09d8ea4d39d1babb95bf0cee5',
}

def require(ok, message):
    if not ok:
        raise RuntimeError(message)

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def sources():
    files = [ROOT / 'verify_recovered_sources.py', ROOT / 'publication-audit/RecoveredSourceAudit.java']
    files += list((ROOT / 'runtime').glob('*.java')) + list((ROOT / 'patch').glob('*.java'))
    return {p.relative_to(ROOT).as_posix(): sha(p) for p in sorted(files)}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('mpp', 'base', 'tools', 'work'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    args = argparse.Namespace(**{key: value.resolve() for key, value in vars(args).items()})
    require(sha(args.mpp) == SAVED_SHA and args.mpp.stat().st_size == 48001679, 'Saved317 identity differs')
    require(sha(args.base) == BASE_SHA and args.base.stat().st_size == 47985964, 'Baseline316 identity differs')
    for name, value in TOOLS.items():
        require(sha(args.tools / name) == value, 'Pinned tool differs: ' + name)
    require(not args.work.exists(), 'Use a new recovery verification work directory')
    args.work.mkdir(parents=True)
    source_hashes = sources()
    logs = {}
    def run(command, label):
        result = subprocess.run(list(map(str, command)), stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, timeout=300)
        (args.work / (label + '.log')).write_text(result.stdout)
        logs[label] = result.stdout.strip()
        require(result.returncode == 0, label + ' failed\n' + result.stdout[-12000:])
        return result.stdout
    require('Temurin-21.0.8+9' in run(['java', '--version'], 'java'), 'Use pinned Temurin21.0.8+9')
    require(run(['javac', '-version'], 'javac').strip() == 'javac 21.0.8', 'Use pinned javac21.0.8')
    for name in ('runtime', 'runtime-dex', 'patch', 'patch-dex', 'audit'):
        (args.work / name).mkdir()
    for kind, dependency, minimum in (('runtime', 'android.jar', '29'), ('patch', 'morphe.jar', '26')):
        run(['javac', '--release', '8', '-encoding', 'UTF-8', '-cp', args.tools / dependency,
             '-d', args.work / kind, *sorted((ROOT / kind).glob('*.java'))], kind + '-javac')
        with zipfile.ZipFile(args.work / (kind + '.jar'), 'w') as z:
            for p in sorted((args.work / kind).rglob('*.class')):
                z.writestr(p.relative_to(args.work / kind).as_posix(), p.read_bytes())
        run(['java', '-cp', args.tools / 'd8.jar', 'com.android.tools.r8.D8', '--release',
             '--min-api', minimum, '--lib', args.tools / 'android.jar', '--classpath', args.tools / dependency,
             '--output', args.work / (kind + '-dex'), args.work / (kind + '.jar')], kind + '-d8')
        require({p.name for p in (args.work / (kind + '-dex')).iterdir()} == {'classes.dex'}, 'Unexpected multidex')
    with zipfile.ZipFile(args.mpp) as z:
        compiled_classes = {p.relative_to(args.work / 'patch').as_posix(): p.read_bytes()
                            for p in (args.work / 'patch').rglob('*.class')}
        saved_classes = {n: z.read(n) for n in z.namelist() if n.startswith('app/hiro/oneback/patches/')}
        require(len(compiled_classes) == 18 and compiled_classes == saved_classes,
                'Recovered JVM loader classes do not match the preserved release bytes')
        (args.work / 'saved-runtime.dex').write_bytes(z.read('extensions/oneback_exit.mpe'))
        (args.work / 'saved-loader.dex').write_bytes(z.read('classes.dex'))
    with zipfile.ZipFile(args.base) as z:
        (args.work / 'base-runtime.dex').write_bytes(z.read('extensions/oneback_exit.mpe'))
        (args.work / 'base-loader.dex').write_bytes(z.read('classes.dex'))
    cp = os.pathsep.join(map(str, [args.work / 'audit', args.tools / 'morphe.jar']))
    run(['javac', '-encoding', 'UTF-8', '-cp', cp, '-d', args.work / 'audit',
         ROOT / 'publication-audit/RecoveredSourceAudit.java'], 'audit-javac')
    audit = run(['java', '-ea', '-cp', cp, 'RecoveredSourceAudit', args.work / 'saved-runtime.dex',
                 args.work / 'runtime-dex/classes.dex', args.work / 'base-runtime.dex',
                 args.work / 'saved-loader.dex', args.work / 'patch-dex/classes.dex',
                 args.work / 'base-loader.dex'], 'source-and-scope-audit')
    require(audit.count('PASS RecoveredSourceAudit') == 3, 'Expected audit completion markers missing')
    require(sources() == source_hashes, 'Source changed during verification')
    report = {
        'schema': 'homeback133-recovered-source-verification-v1',
        'bundle_version': '1.0.317', 'saved_binary_sha256': SAVED_SHA,
        'baseline_version': '1.0.316', 'baseline_sha256': BASE_SHA,
        'source_sha256': source_hashes, 'tool_sha256': TOOLS,
        'loader_jvm_classes_byte_identical': 18,
        'runtime_class_inventory': 93, 'runtime_byte_exact_preserved_from_baseline': 91,
        'runtime_changed_from_baseline': ['InstagramDispatcherBack', 'InstagramExternalProfile'],
        'runtime_comparison_ignores_only_dex_line_and_local_debug_records': True,
        'compiled_loader_dex_class_definitions_equal_saved': True,
        'unrelated_loader_class_definitions_preserved': 282,
        'audit': audit.strip().splitlines(),
        'logs_sha256': {p.name: sha(p) for p in sorted(args.work.glob('*.log'))},
        'physical_android_tested': False, 'complete': True,
    }
    (args.work / 'source-verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(audit, end='')
    print('PASS recovered source verification: saved317 binary unchanged')

if __name__ == '__main__':
    main()
