#!/usr/bin/env python3
"""Compare actual APK payloads; DEX and Manifest have separate semantic audits."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import zipfile


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(chunk)
    return {'file': path.name, 'bytes': path.stat().st_size, 'sha256': h.hexdigest()}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('original', type=Path)
    p.add_argument('patched', type=Path)
    p.add_argument('report', type=Path)
    a = p.parse_args()
    with zipfile.ZipFile(a.original) as old, zipfile.ZipFile(a.patched) as new:
        old_names = [n for n in old.namelist() if not n.endswith('/')]
        new_names = [n for n in new.namelist() if not n.endswith('/')]
        assert len(old_names) == len(set(old_names)), 'duplicate input entry'
        assert len(new_names) == len(set(new_names)), 'duplicate output entry'
        before, after = set(old_names), set(new_names)
        changed, unchanged = [], []
        for name in sorted(before & after):
            (unchanged if old.read(name) == new.read(name) else changed).append(name)
        removed, added = sorted(before - after), sorted(after - before)
        dex = lambda name: re.fullmatch(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex', name) is not None
        unexpected = [n for n in changed if not dex(n) and n not in ('AndroidManifest.xml', 'resources.arsc')]
        unexpected += [n for n in removed + added if not dex(n)]
        native = [n for n in unchanged if n.startswith('lib/')]
    result = {
        'schema': 'oneback163-apk-entry-audit-v1',
        'result': 'FAIL' if unexpected else 'PASS',
        'blocking_findings': unexpected,
        'original': digest(a.original),
        'patched': digest(a.patched),
        'unchanged_entry_count': len(unchanged),
        'unchanged_native_library_count': len(native),
        'changed_entries': changed,
        'removed_entries': removed,
        'added_entries': added,
        'allowed_changes': 'Manifest, resource table and DEX layout only; DEX inventory/bodies and Manifest verified separately',
        'physical_device_tested': False,
    }
    a.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    if unexpected:
        raise SystemExit('FAIL unexpected APK entries: ' + ', '.join(unexpected))
    print('PASS APK entries:', a.patched.name, '; unchanged=', len(unchanged), '; native=', len(native))


if __name__ == '__main__':
    main()
