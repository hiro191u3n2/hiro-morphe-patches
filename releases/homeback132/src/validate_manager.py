#!/usr/bin/env python3
"""Recreate the reported mixed-source failure with the actual Manager JVM host.

The unmodified host and framework resources come from the official Manager APK.
Each case starts a fresh host process and passes genuine Manager selection keys.
Private APKs remain in --work. The proof describes explicit test selections; the
user's unknown set of 68 selected patches/options is not inferred from its count.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from build import SINGLE, BUNDLE, BASE_BYTES, BASE_SHA

PATCH = '戻る1回で終了・履歴削除'
NAVIGATION = 'Customize navigation bar'
EXPECTED_FAILURE = 'Instagram 452 external profile native body changed: LX/003Q;->A04'
CASE_IDS = ('old311-navigation', 'standalone-navigation',
            'aggregate-default-baseline', 'aggregate-default')
INPUT = {
    'package': 'com.instagram.android', 'version_name': '452.1.0.53.64',
    'version_code': 385811006, 'bytes': 143237227,
    'sha256': '22f20062d16997e26009e73b0425974efef8ac69c76dd8912ee8a091213c39e2',
}
HOST = {'bytes': 19926006,
        'sha256': '4ffa8ccabcc475a18781fcc8642c721eed35ede7d43f967c4f0bd0f51a964a29'}
MANAGER_APK = {'bytes': 41222610,
               'sha256': '3df9064c5648e0ea0a2d5df4394616bc2320c7bd950ab530dde3d93be6906300'}
MANAGER_COMMIT = '9fc6d7254c15181b3b6920a65d77dcadc73cb47e'
PATCHER_COMMIT = 'bd7bca01f438687c34ad4a5d9d8e6e6f20f0a2d1'
PIKO = {
    'version': '3.11.0-dev.3', 'bytes': 8529170,
    'sha256': 'a7658d042676d750ff68b782ff02e65d077c011d6a6f09031c05b39e80fcc7a3',
    'source_commit': '04987953674f98b3f302dc7a8da7492ae96b5267',
    'source_url': 'https://github.com/crimera/piko/tree/04987953674f98b3f302dc7a8da7492ae96b5267',
    'artifact_url': 'https://github.com/crimera/piko/releases/download/v3.11.0-dev.3/patches-3.11.0-dev.3.mpp',
}
DEVICE_CONFIG = {'supportedAbis': ['arm64-v8a'], 'localeTags': ['ja-JP'], 'densityDpi': 480}


def require(value, why):
    if not value:
        raise RuntimeError(why)


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def identity(path):
    return {'bytes': Path(path).stat().st_size, 'sha256': sha(path)}


def pinned(path, expected, label):
    require(identity(path) == {key: expected[key] for key in ('bytes', 'sha256')},
            'Use the exact pinned ' + label)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + '\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'piko', 'base', 'host', 'manager-apk', 'work', 'proof'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--dist', type=Path,
                        help='Final paired-build MPP directory, required for the complete matrix')
    parser.add_argument('--java', default='java')
    parser.add_argument('--javac', default='javac')
    parser.add_argument('--only', action='append', choices=CASE_IDS,
                        help='Diagnostic partial run; its proof has complete=false')
    args = parser.parse_args()
    for name, value in vars(args).items():
        if isinstance(value, Path):
            setattr(args, name, value.resolve())
    for name in ('java', 'javac'):
        executable = shutil.which(getattr(args, name))
        require(executable, 'Java 21 executable unavailable: ' + name)
        setattr(args, name, str(Path(executable).resolve()))
    case_ids = args.only or CASE_IDS
    require(len(case_ids) == len(set(case_ids)), 'Duplicate requested case')
    complete = tuple(case_ids) == CASE_IDS
    needs_final = any(value in case_ids for value in ('standalone-navigation', 'aggregate-default'))
    require(not needs_final or args.dist, '--dist is required for final-artifact cases')
    pinned(args.apk, INPUT, 'original Instagram APK')
    pinned(args.host, HOST, 'Manager JVM host')
    pinned(args.manager_apk, MANAGER_APK, 'Manager APK')
    pinned(args.piko, PIKO, 'Piko MPP')
    pinned(args.base, {'bytes': BASE_BYTES, 'sha256': BASE_SHA}, 'published Hiro 1.0.311 MPP')
    require(not args.work.exists(), 'Use a fresh Manager work directory')
    require(not args.proof.exists(), 'Refusing to overwrite an existing Manager proof')
    args.work.mkdir(parents=True)
    artifacts = ({name: identity(args.dist/name) for name in (SINGLE, BUNDLE)}
                 if args.dist else {})
    helper = args.work/'helper'
    helper.mkdir()

    def run(command, log, timeout=1800):
        with log.open('w') as stream:
            result = subprocess.run(list(map(str, command)), stdout=stream,
                                    stderr=subprocess.STDOUT, timeout=timeout)
        require(result.returncode == 0,
                'Process failed: ' + log.name + '\n' + log.read_text()[-12000:])
        return result.returncode

    helper_source = ROOT/'validation/Manager237Selections.java'
    run([args.javac, '-encoding', 'UTF-8', '-cp', args.host, '-d', helper, helper_source],
        args.work/'helper-compile.log', 180)
    classpath = os.pathsep.join(map(str, (helper, args.host)))
    manifests = {}

    def metadata(label, mpp):
        log = args.work/(label+'-selections.json')
        run([args.java, '-Xmx512m', '-cp', classpath, 'Manager237Selections', mpp,
             INPUT['package'], INPUT['version_name']], log, 180)
        manifest = json.loads(log.read_text())
        require(manifest['schema'] == 'manager237-selection-manifest-v1'
                and manifest['package'] == INPUT['package']
                and manifest['version'] == INPUT['version_name'], 'Bad selection manifest')
        rows = manifest['patches']
        require(len({row['key'] for row in rows}) == len(rows), 'Duplicate Manager selection key')
        manifests[label] = {'artifact': Path(mpp).name, **identity(mpp), **manifest}
        return rows

    piko = metadata('piko', args.piko)
    old = metadata('hiro311', args.base)
    standalone = metadata('standalone', args.dist/SINGLE) if args.dist else None
    aggregate = metadata('aggregate', args.dist/BUNDLE) if args.dist else old

    def defaults(rows):
        return [row for row in rows if row['default'] and row['version_compatible']]

    def named(rows, name):
        matches = [row for row in rows if row['name'] == name and row['version_compatible']]
        require(len(matches) == 1, 'Expected exactly one compatible patch: ' + name)
        return matches

    piko_defaults = defaults(piko)
    hiro_defaults = defaults(aggregate)
    require(len(piko) == 68 and len(piko_defaults) == 64, 'Piko selection metadata changed')
    require([row['name'] for row in hiro_defaults] == [PATCH],
            'Hiro Instagram default selection changed')
    cases = []

    def bundle(uid, mpp, rows):
        return {'uid': uid, 'patchesFile': str(mpp),
                'patches': [row['key'] for row in rows], 'options': {}}

    def selection(mpp, rows):
        return {'artifact': Path(mpp).name, **identity(mpp),
                'selected_patch_names': [row['name'] for row in rows],
                'selected_patch_keys': [row['key'] for row in rows],
                'option_overrides': {},
                'option_defaults': {row['key']: row['option_defaults'] for row in rows}}

    def apply(case_id, requested, expected_failure=False):
        print('RUN Manager237 ' + case_id, flush=True)
        folder = args.work/case_id
        folder.mkdir()
        output = folder/'patched.apk'
        request_path, commands, events_path = (folder/name for name in
                                               ('request.json', 'commands.jsonl', 'events.jsonl'))
        selected_bundles = [selection(mpp, rows) for mpp, rows in requested if rows]
        selected_names = [name for selected in selected_bundles
                          for name in selected['selected_patch_names']]
        require(len(selected_names) == len(set(selected_names)), 'Ambiguous selected patch names')
        request = {
            'packageName': INPUT['package'], 'inputFile': str(args.apk),
            'outputFile': str(output), 'mergedApkFile': str(folder/'merged.apk'),
            'temporaryDir': str(folder/'temp'), 'workspaceDir': str(folder/'workspace'),
            'bundles': [bundle(i+1, mpp, rows) for i, (mpp, rows) in enumerate(requested) if rows],
            'stripUnusedNativeLibs': False, 'device': DEVICE_CONFIG,
        }
        write_json(request_path, request)
        commands.write_text(json.dumps({'type': 'run', 'requestFile': str(request_path)}) + '\n')
        code = run([args.java, '-Xmx5g', '-cp', os.pathsep.join(map(str, (args.host, args.manager_apk))),
                    'app.morphe.manager.patcher.host.HostMainKt', commands, events_path],
                   folder/'process.log')
        events = [json.loads(line) for line in events_path.read_text().splitlines() if line.strip()]
        finished = [event for event in events if event.get('type') == 'finished']
        require(len(finished) == 1 and events[-1] == finished[0], 'Host did not finish exactly once')
        applied = [event['name'] for event in events if event.get('type') == 'patch']
        failed = [event for event in events if event.get('type') == 'patch_failed']
        counts = [event['count'] for event in events if event.get('type') == 'patches_done']
        failure = finished[0].get('failure')
        if expected_failure:
            require(applied == [NAVIGATION], 'Navigation did not precede the old OneBack failure')
            require(len(failed) == 1 and failed[0]['name'] == PATCH
                    and EXPECTED_FAILURE in failed[0]['stackTrace'], 'Wrong old OneBack failure')
            require(failure and EXPECTED_FAILURE in failure.get('stackTrace', '')
                    and failure.get('outOfMemory') is False, 'Expected contract failure missing')
            require(not counts and not output.exists(), 'Rejected patch unexpectedly completed')
        else:
            require(failure is None and not failed, 'Patch failure: ' + json.dumps(failure, ensure_ascii=False))
            require(len(applied) == len(selected_names) and set(applied) == set(selected_names),
                    'Host applied a different patch selection')
            require(counts == [len(selected_names)], 'Missing/inconsistent patches_done event')
            require(output.is_file() and zipfile.is_zipfile(output), 'Host output APK absent')
            with zipfile.ZipFile(output) as archive:
                require('AndroidManifest.xml' in archive.namelist() and 'classes.dex' in archive.namelist(),
                        'Incomplete output APK')
        row = {
            'id': case_id, 'result': 'PASS', 'expected_failure': expected_failure,
            'input_sha256': INPUT['sha256'], 'selected_count': len(selected_names),
            'selected_bundles': selected_bundles, 'applied_patches': applied,
            'failed_patches': failed, 'process_exit_code': code,
            'finished_failure': failure, 'patches_done_counts': counts,
            'events_sha256': sha(events_path), 'events_count': len(events),
            'request_sha256': sha(request_path),
        }
        if expected_failure:
            row['expected_failure_message'] = EXPECTED_FAILURE
        else:
            row['output_apk_sha256'] = sha(output)
            row['output_apk_bytes'] = output.stat().st_size
        cases.append(row)
        # Keep private final APKs and event evidence, discard reproducible intermediates.
        for temporary in ('temp', 'workspace'):
            shutil.rmtree(folder/temporary, ignore_errors=True)
        print('PASS Manager237 ' + case_id + ' selected=' + str(len(selected_names)), flush=True)

    for case_id in case_ids:
        if case_id == 'old311-navigation':
            apply(case_id, [(args.piko, named(piko, NAVIGATION)), (args.base, named(old, PATCH))], True)
        elif case_id == 'standalone-navigation':
            apply(case_id, [(args.piko, named(piko, NAVIGATION)),
                            (args.dist/SINGLE, named(standalone, PATCH))])
        elif case_id == 'aggregate-default-baseline':
            other_hiro = [row for row in hiro_defaults if row['name'] != PATCH]
            apply(case_id, [(args.piko, piko_defaults),
                            (args.dist/BUNDLE if args.dist else args.base, other_hiro)])
        elif case_id == 'aggregate-default':
            apply(case_id, [(args.piko, piko_defaults), (args.dist/BUNDLE, hiro_defaults)])
    if args.dist:
        require(all(identity(args.dist/name) == row for name, row in artifacts.items()),
                'Final MPP changed during Manager verification')
    for path, expected, label in ((args.host, HOST, 'host'), (args.manager_apk, MANAGER_APK, 'Manager'),
                                   (args.piko, PIKO, 'Piko'), (args.apk, INPUT, 'original APK')):
        pinned(path, expected, label)
    proof = {
        'schema': 'homeback132-manager-compat-v1', 'result': 'PASS', 'complete': complete,
        'physical_android_tested': False, 'input': INPUT,
        'manager': {'version': '2.3.0-dev.7', 'apk': MANAGER_APK, 'host': HOST,
                    'source_commit': MANAGER_COMMIT,
                    'source_url': 'https://github.com/MorpheApp/morphe-manager/tree/' + MANAGER_COMMIT,
                    'artifact_url': 'https://github.com/MorpheApp/morphe-manager/releases/tag/v2.3.0-dev.7',
                    'host_unmodified': True, 'frameworks_from_original_apk': True,
                    'entry_point': 'app.morphe.manager.patcher.host.HostMainKt'},
        'patcher': {'version': '2.1.0-dev.4', 'embedded_in_manager_host': True,
                    'source_commit': PATCHER_COMMIT,
                    'source_url': 'https://github.com/MorpheApp/morphe-patcher/tree/' + PATCHER_COMMIT,
                    'manager_dependency_url': 'https://github.com/MorpheApp/morphe-manager/blob/'
                        + MANAGER_COMMIT + '/gradle/libs.versions.toml'},
        'piko': PIKO, 'baseline_hiro': {'version': '1.0.311', 'bytes': BASE_BYTES, 'sha256': BASE_SHA},
        'artifacts': artifacts, 'cases': cases, 'selection_manifests': manifests,
        'selection_helper_sha256': sha(helper_source), 'validation_script_sha256': sha(Path(__file__)),
        'device_config': DEVICE_CONFIG,
        'limitations': [
            'These explicit default selections do not reconstruct the user\'s unknown 68 patch selections/options.',
            'The host ran on Java 21/Linux; no physical Samsung/Android 17 execution was performed.',
            'The supplied ABI/locale/density are a host test configuration, not measured device properties.',
        ],
    }
    write_json(args.proof, proof)
    print('PASS Manager237 matrix complete=' + str(complete).lower(), flush=True)


if __name__ == '__main__':
    main()
