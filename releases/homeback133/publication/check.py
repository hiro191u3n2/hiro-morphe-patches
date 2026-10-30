#!/usr/bin/env python3
"""Verify the preserved release and frozen, freshly executed recovery evidence."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
SAVED_SHA = '4f190bc54c91effbfa778b62bf913e2b6509f82c8e8b4f1b99b5a59ccae3a466'
BASE_SHA = 'd45359b07529e3c1bcbebfe795befeeae8ebe53e5794725c2c06750e996e06ea'

def require(value, message):
    if not value:
        raise RuntimeError(message)

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def safe(root, name):
    path = root / name
    require(not Path(name).is_absolute() and '..' not in Path(name).parts
            and not path.is_symlink(), 'Unsafe evidence path')
    return path

def manifest():
    files = {}
    for directory in (REPO/'releases/homeback132', ROOT):
        for path in sorted(directory.rglob('*')):
            if not path.is_file() or '__pycache__' in path.parts or path.suffix in ('.pyc', '.class'):
                continue
            if path == ROOT/'verification/verified.json':
                continue
            # Generated publication receipts/assets are not inputs to this gate.
            if path.parent == ROOT and (path.name.startswith('publication_') or path.suffix in ('.json', '.txt', '.zip')):
                continue
            require(not path.is_symlink(), 'Symlink in source/evidence tree')
            files[path.relative_to(REPO).as_posix()] = sha(path)
    workflow = REPO/'.github/workflows/homeback133-publish.yml'
    files[workflow.relative_to(REPO).as_posix()] = sha(workflow)
    return files

def verify_evidence():
    source = json.loads((ROOT/'verification/source/source-verification.json').read_text())
    require(source['schema'] == 'homeback133-recovered-source-verification-v1'
            and source['complete'] is True and source['physical_android_tested'] is False,
            'Source verification is incomplete')
    for key, value in {
        'saved_binary_sha256': SAVED_SHA, 'baseline_sha256': BASE_SHA,
        'loader_jvm_classes_byte_identical': 18, 'runtime_class_inventory': 93,
        'runtime_byte_exact_preserved_from_baseline': 91,
        'runtime_changed_from_baseline': ['InstagramDispatcherBack', 'InstagramExternalProfile'],
        'runtime_comparison_ignores_only_dex_line_and_local_debug_records': True,
        'compiled_loader_dex_class_definitions_equal_saved': True,
        'unrelated_loader_class_definitions_preserved': 282,
    }.items():
        require(source.get(key) == value, 'Source verification differs: '+key)
    require(len(source['audit']) == 3 and all(row.startswith('PASS RecoveredSourceAudit ') for row in source['audit']),
            'Canonical DEX audit did not pass')
    for name, digest in source['source_sha256'].items():
        require(sha(safe(ROOT/'src', name)) == digest, 'Source changed after compilation: '+name)
    for name, digest in source['logs_sha256'].items():
        require(sha(safe(ROOT/'verification/source', name)) == digest, 'Source audit log differs: '+name)
    host = json.loads((ROOT/'verification/host-report.json').read_text())
    require(host['schema'] == 'homeback133-recovered-host-v1' and host['result'] == 'PASS'
            and host['physical_android_tested'] is False and host['suite_count'] == 31
            and len(host['suites']) == 31, 'Host suite verification incomplete')
    require(host['retained_bundle'] == {'bytes': 48001679, 'sha256': SAVED_SHA}, 'Host artifact differs')
    require(all(row['exit_code'] == 0 for row in host['suites'].values()), 'Host suite failed')
    require(host['suites']['Back133InstagramDispatcherProfileTest']['summary'].startswith('PASS Back133InstagramDispatcherProfileTest'),
            'Dispatcher regression absent')
    require(host['baseline_negative_control']['exit_code'] == 1
            and host['baseline_negative_control']['failed_count'] > 0,
            'Old-runtime negative control absent')
    for name, digest in host['source_sha256'].items():
        require(sha(safe(ROOT, name)) == digest, 'Host source changed after execution: '+name)
    require(sha(ROOT/'verification/host-report.log') == host['transcript_sha256'], 'Host transcript differs')
    return source, host

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundle', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    require(args.bundle.stat().st_size == 48001679 and sha(args.bundle) == SAVED_SHA,
            'Previously verified saved317 bytes differ')
    with zipfile.ZipFile(args.bundle) as archive:
        require(len(archive.namelist()) == len(set(archive.namelist())) and archive.testzip() is None,
                'Bundle ZIP corruption or duplicate entries')
    report = json.loads(args.report.read_text())
    require(report['schema'] == 'homeback133-preserved-artifact-recovery-v1'
            and report['complete'] is True and report['physical_android_tested'] is False,
            'Frozen recovery record incomplete')
    require(report['artifact'] == {'bytes': 48001679, 'sha256': SAVED_SHA}, 'Frozen artifact differs')
    require(report['original_detailed_qa_reports_recovered'] is False
            and report['original_apk_apply_repeated_in_this_publication'] is False,
            'Recovery evidence must distinguish previous APK testing')
    require(manifest() == report['source_and_evidence_sha256'], 'Frozen source/evidence inventory differs')
    verify_evidence()
    require((REPO/'.github/workflows/homeback133-publish.yml').read_bytes()
            == (ROOT/'publication/workflow.yml').read_bytes(), 'Workflow mirror differs')
    require(report['zip_scope'] == {
        'removed': [], 'added': ['META-INF/hiro-homeback133/LICENSE', 'META-INF/hiro-homeback133/NOTICE'],
        'changed': ['META-INF/MANIFEST.MF', 'classes.dex', 'extensions/oneback_exit.mpe'],
        'unchanged_entries': 266, 'baseline_sha256': BASE_SHA,
    }, 'Frozen ZIP preservation scope differs')
    print('PASS preserved bundle317 recovery gate: exact saved binary, compiled corresponding sources, 31 host suites, scoped preservation')

if __name__ == '__main__':
    main()
