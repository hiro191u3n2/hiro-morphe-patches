#!/usr/bin/env python3
"""Strict X APK resource/manifest audit with independently reviewed rebuild deltas.

The input and aapt2 are pinned. Only DEX, root signature metadata, the manifest,
and resources.arsc are excluded from the byte-exact entry comparison. Manifest
changes are limited to enableOnBackInvokedCallback=true on application/activity.
Resource-table normalization is accepted only when explicitly pinned below from
this X input/output pair; no Instagram exceptions are inherited.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path
from difflib import SequenceMatcher

INPUT_SHA256 = 'f6792d27f18ecb7e37fddcd1f67cf6f65f4d667fd3a8941e09c7ac266058e1ae'
AAPT2_SHA256 = 'd1096e11aba9c974644369ee3c50d239acac3f3428ffa928e5b9c14dfb7a57de'
EXPECTED_BASELINE_RESOURCES = 34523
EXPECTED_BASELINE_PUBLIC = 8428
EXPECTED_ACTIVITIES = 396
EXPECTED_ALIASES = 138
# Reviewed against the actual v1.1.14 X rebuild: no decoded resource change,
# including PUBLIC visibility. The input already contains other Morphe patches.
EXPECTED_PUBLIC_ADDED_COUNT = 0
EXPECTED_PUBLIC_ADDED_SHA256 = '4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945'
EXPECTED_OTHER_RESOURCE_DELTAS = []
BACK_KEY = 'http://schemas.android.com/apk/res/android:enableOnBackInvokedCallback(0x0101066c)'
NAME_KEY = 'http://schemas.android.com/apk/res/android:name(0x01010003)'


def sha(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def file_sha(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        while chunk := stream.read(1024*1024):
            digest.update(chunk)
    return digest.hexdigest()


def require(condition: bool, message: str, details=None):
    if not condition:
        suffix = '' if details is None else ': ' + json.dumps(details, ensure_ascii=False)
        raise AssertionError(message + suffix)


def dump(aapt2: Path, apk: Path, mode: str) -> list[str]:
    command = [str(aapt2), 'dump', mode, str(apk)]
    if mode == 'xmltree':
        command.extend(['--file', 'AndroidManifest.xml'])
    return subprocess.check_output(command, text=True).splitlines()


def apk_entries(path: Path) -> dict[str, tuple[int, str]]:
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)), 'Duplicate APK entries')
        entries = {}
        for info in archive.infolist():
            digest = hashlib.sha256()
            size = 0
            with archive.open(info) as entry:
                while True:
                    chunk = entry.read(1024*1024)
                    if not chunk:
                        break
                    size += len(chunk)
                    digest.update(chunk)
            require(size == info.file_size, 'APK entry size differs', info.filename)
            entries[info.filename] = (size, digest.hexdigest())
        return entries


def signature_metadata(name: str) -> bool:
    # Nested META-INF/versions/.../OSGI-INF/MANIFEST.MF is program data.
    return name == 'META-INF/MANIFEST.MF' or re.fullmatch(
        r'META-INF/[^/]+\.(?:SF|RSA|DSA|EC)', name, flags=re.IGNORECASE) is not None


def mutable_entry(name: str) -> bool:
    return (re.fullmatch(r'classes(?:[2-9][0-9]*|1[0-9]+)?\.dex', name) is not None
            or signature_metadata(name) or name in ('AndroidManifest.xml', 'resources.arsc'))


def compare_entries(before: dict[str, tuple[int, str]], single: dict[str, tuple[int, str]],
                    bundle: dict[str, tuple[int, str]]):
    require(set(single) == set(bundle), 'Single/bundle entry inventories differ',
            {'single_only': sorted(set(single)-set(bundle)), 'bundle_only': sorted(set(bundle)-set(single))})
    unequal = [name for name in single if single[name] != bundle[name]]
    require(not unequal, 'Single/bundle entry bytes differ', unequal)
    immutable_before = {name for name in before if not mutable_entry(name)}
    immutable_after = {name for name in single if not mutable_entry(name)}
    require(immutable_before == immutable_after, 'Noncode entry inventory changed',
            {'removed': sorted(immutable_before-immutable_after), 'added': sorted(immutable_after-immutable_before)})
    unequal = [name for name in sorted(immutable_before) if before[name] != single[name]]
    require(not unequal, 'Noncode entry bytes changed', unequal)
    return {
        'all_single_bundle_zip_entry_bytes_equal': True,
        'byte_exact_noncode_entries': len(immutable_before),
        'byte_exact_non_signature_meta_inf_entries': sum(name.startswith('META-INF/') for name in immutable_before),
        'removed_signature_metadata': sorted(name for name in set(before)-set(single) if signature_metadata(name)),
        'added_signature_metadata': sorted(name for name in set(single)-set(before) if signature_metadata(name)),
    }


def manifest_model(lines: list[str]):
    """Retain every decoded line except allowed flags and XML source line numbers."""
    normalized = [re.sub(r' \(line=\d+\)', '', line) for line in lines]
    retained = []
    nodes = []
    stack = []
    for line in normalized:
        match = re.fullmatch(r'(\s*)E: ([^\s]+)', line)
        if match:
            indent = len(match[1])
            while stack and stack[-1]['indent'] >= indent:
                stack.pop()
            parent = stack[-1] if stack else None
            node = {'index': len(nodes), 'parent': parent['index'] if parent else None,
                    'indent': indent, 'tag': match[2], 'name': None, 'back': None}
            nodes.append(node)
            stack.append(node)
        elif line.lstrip().startswith('A: '):
            require(bool(stack), 'Manifest attribute has no owning element', line)
            node = stack[-1]
            value = line.strip()[3:]
            if value.startswith(NAME_KEY+'='):
                node['name'] = value.split('=', 1)[1]
            if value.startswith(BACK_KEY+'=') and node['tag'] in ('application', 'activity'):
                require(node['back'] is None, 'Duplicate predictive-Back flag', node['index'])
                node['back'] = value.split('=', 1)[1]
                continue
        retained.append(line)
    return retained, nodes


def compare_manifest(before: list[str], after: list[str]):
    old_lines, old_nodes = manifest_model(before)
    new_lines, new_nodes = manifest_model(after)
    if old_lines != new_lines:
        differences = [{'kind': tag, 'before': old_lines[i:j], 'after': new_lines[k:l]}
                       for tag, i, j, k, l in SequenceMatcher(a=old_lines, b=new_lines, autojunk=False).get_opcodes()
                       if tag != 'equal']
        require(False, 'Manifest changed outside the predictive-Back flag', differences[:20])
    require(len(old_nodes) == len(new_nodes), 'Manifest element inventory changed')
    old_targets = [node for node in old_nodes if node['tag'] in ('application', 'activity')]
    new_targets = [node for node in new_nodes if node['tag'] in ('application', 'activity')]
    require(sum(node['tag'] == 'application' for node in old_nodes) == 1, 'Expected one application')
    require(sum(node['tag'] == 'activity' for node in old_nodes) == EXPECTED_ACTIVITIES, 'X activity inventory differs')
    require(sum(node['tag'] == 'activity-alias' for node in old_nodes) == EXPECTED_ALIASES, 'X activity-alias inventory differs')
    require(len(old_targets) == len(new_targets) == EXPECTED_ACTIVITIES+1, 'Predictive-Back target inventory differs')
    require([node['back'] for node in old_targets].count('true') == 1
            and all(node['back'] in (None, 'true') for node in old_targets), 'Baseline Back flags differ')
    changes = []
    for old, new in zip(old_targets, new_targets):
        require({key: value for key, value in old.items() if key != 'back'} ==
                {key: value for key, value in new.items() if key != 'back'}, 'Manifest target identity changed')
        require(new['back'] == 'true', 'Output application/activity does not enable predictive Back', new['name'])
        if old['back'] != new['back']:
            changes.append({'element': old['tag'], 'name': old['name'], 'before': old['back'], 'after': new['back']})
    return {'result': 'PASS', 'all_other_flags_values_elements_and_order_unchanged': True,
            'application_count': 1, 'activity_count': EXPECTED_ACTIVITIES,
            'activity_alias_count_unchanged': EXPECTED_ALIASES,
            'input_explicit_true_count': 1, 'output_explicit_true_count': len(new_targets),
            'added_true_count': len(changes), 'changes': changes}


def resource_observation(before: list[str], after: list[str]):
    require(sum(line.startswith('    resource ') for line in before) == EXPECTED_BASELINE_RESOURCES,
            'X resource inventory differs')
    require(sum(line.startswith('    resource ') and line.endswith(' PUBLIC') for line in before) == EXPECTED_BASELINE_PUBLIC,
            'X baseline PUBLIC inventory differs')
    require(len(before) == len(after), 'Resource dump line inventory differs',
            {'before_lines': len(before), 'after_lines': len(after)})
    public = []
    deltas = []
    current = None
    for old, new in zip(before, after):
        if old.startswith('    resource '):
            current = old.strip().split()[2]
        if old == new:
            continue
        if old.startswith('    resource ') and new == old+' PUBLIC':
            public.append({'resource': current, 'before': old, 'after': new})
        else:
            deltas.append({'resource': current, 'before': old, 'after': new})
    public_raw = json.dumps(public, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode()
    return {'aapt_public_flags_added': len(public), 'public_added_sha256': sha(public_raw),
            'other_deltas': deltas, 'baseline_resources': EXPECTED_BASELINE_RESOURCES,
            'baseline_public_resources': EXPECTED_BASELINE_PUBLIC}


def compare_resources(before: list[str], after: list[str]):
    observed = resource_observation(before, after)
    require(EXPECTED_PUBLIC_ADDED_COUNT is not None and EXPECTED_PUBLIC_ADDED_SHA256 is not None
            and EXPECTED_OTHER_RESOURCE_DELTAS is not None,
            'Actual X resource normalization has not yet been reviewed', observed)
    require(observed['aapt_public_flags_added'] == EXPECTED_PUBLIC_ADDED_COUNT
            and observed['public_added_sha256'] == EXPECTED_PUBLIC_ADDED_SHA256
            and observed['other_deltas'] == EXPECTED_OTHER_RESOURCE_DELTAS,
            'Unreviewed X resource normalization', observed)
    return {**observed, 'result': 'PASS', 'all_other_decoded_values_equal': True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('aapt2', type=Path)
    parser.add_argument('before', type=Path)
    parser.add_argument('single', type=Path)
    parser.add_argument('bundle', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    require(file_sha(args.aapt2) == AAPT2_SHA256, 'Unreviewed aapt2 tool')
    require(file_sha(args.before) == INPUT_SHA256, 'Unreviewed X input APK')
    entries = [apk_entries(path) for path in (args.before, args.single, args.bundle)]
    entry_result = compare_entries(*entries)
    manifest = compare_manifest(dump(args.aapt2, args.before, 'xmltree'), dump(args.aapt2, args.single, 'xmltree'))
    resource_table = compare_resources(dump(args.aapt2, args.before, 'resources'), dump(args.aapt2, args.single, 'resources'))
    report = {'schema': 'homeback124-x-resource-audit-v1', 'result': 'PASS',
              'package_name': 'com.twitter.android', 'physical_android_tested': False,
              'input_sha256': INPUT_SHA256, 'single_output_sha256': file_sha(args.single),
              'bundle_output_sha256': file_sha(args.bundle), 'aapt2_sha256': AAPT2_SHA256,
              **entry_result, 'resource_table': resource_table, 'manifest': manifest}
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({key: value for key, value in report.items() if key != 'manifest'}, ensure_ascii=False, indent=2))
    print('PASS manifest: application and all 396 activities enable predictive Back; all other values and elements retained')


if __name__ == '__main__':
    main()
