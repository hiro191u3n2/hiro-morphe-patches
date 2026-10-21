#!/usr/bin/env python3
"""Package public patch sources and existing build evidence deterministically."""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent.parent
BUNDLE = 'Hiro_Morphe_Patches_v1.0.270.mpp'
QA = 'QA_YahooTransit_v1.2.5.json'
SOURCE = 'YahooTransit_v1.2.5_sources_and_QA.zip'


def source_files():
    result = {}
    for folder in ('src', 'publication'):
        for path in sorted((ROOT / folder).rglob('*')):
            if (path.is_file() and '__pycache__' not in path.parts
                    and path.suffix in ('.py', '.java', '.xml', '.json', '.txt', '.md', '.yml')):
                result[path.relative_to(ROOT).as_posix()] = path.read_bytes()
    if (ROOT / 'README.md').is_file():
        result['README.md'] = (ROOT / 'README.md').read_bytes()
    workflow = ROOT.parent.parent / '.github/workflows/yahootransit125-publish.yml'
    if workflow.is_file():
        if 'publication/workflow.yml' in result:
            assert result['publication/workflow.yml'] == workflow.read_bytes(), 'Workflow copy differs'
        result['publication/workflow.yml'] = workflow.read_bytes()
    return result


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--dist', type=Path, required=True)
    a = p.parse_args()
    assert all((a.dist / n).is_file() for n in (BUNDLE, QA))
    qa = json.loads((a.dist / QA).read_bytes())
    assert (qa.get('schema') == 'yahootransit125-qa-v1' and qa.get('result') == 'PASS'
            and qa.get('patch_version') == '1.2.5' and qa.get('bundle_version') == '1.0.270'
            and qa.get('baseline_version') == '1.0.269'
            and qa.get('bundle_sha256') == hashlib.sha256((a.dist / BUNDLE).read_bytes()).hexdigest()), \
            'Public package requires completed QA bound to the final bundle270 bytes'
    notes = (ROOT / 'publication/RELEASE_NOTES.txt').read_bytes()
    (a.dist / 'RELEASE_NOTES.txt').write_bytes(notes)
    entries = {**source_files(), QA: (a.dist / QA).read_bytes(), 'RELEASE_NOTES.txt': notes}
    with zipfile.ZipFile(a.dist / SOURCE, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name, raw in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 21, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            z.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)
    names = (BUNDLE, QA, SOURCE, 'RELEASE_NOTES.txt')
    (a.dist / 'SHA256SUMS.txt').write_text(''.join(
        hashlib.sha256((a.dist / n).read_bytes()).hexdigest() + '  ' + n + '\n' for n in names))
    print('PASS deterministic public source and QA package')


if __name__ == '__main__':
    main()
