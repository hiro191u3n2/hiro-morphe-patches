#!/usr/bin/env python3
"""Package the reviewed Berry sources and bound validation evidence."""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent.parent
SINGLE = 'Berry_Browser_v1.3.0.mpp'
BUNDLE = 'Hiro_Morphe_Patches_v1.0.271.mpp'
QA = 'QA_Berry_Browser_v1.3.0.json'
NOTES = 'README_Berry_Browser_v1.3.0.txt'
SOURCE = 'Berry_Browser_v1.3.0_sources.zip'
BASE_SHA = '05064b69f2965146de6461d7cbcf25318e61f17a2f099b25d6c15c2a205c821e'


def source_files():
    result = {}
    for folder in ('src', 'publication'):
        for path in sorted((ROOT / folder).rglob('*')):
            if (path.is_file() and '__pycache__' not in path.parts
                    and path.suffix in ('.py', '.java', '.xml', '.json', '.txt', '.md', '.yml')):
                result[path.relative_to(ROOT).as_posix()] = path.read_bytes()
    if (ROOT / 'README.md').is_file():
        result['README.md'] = (ROOT / 'README.md').read_bytes()
    workflow = ROOT.parent.parent / '.github/workflows/berry-messagequeue130-publish.yml'
    if workflow.is_file():
        if 'publication/workflow.yml' in result:
            assert result['publication/workflow.yml'] == workflow.read_bytes(), 'Workflow copy differs'
        result['publication/workflow.yml'] = workflow.read_bytes()
    return result


def validate_qa(dist):
    qa = json.loads((dist / QA).read_bytes())
    assert (qa.get('schema') == 'berry-messagequeue130-qa-v1'
            and qa.get('result') == 'PASS'
            and qa.get('patch_version') == '1.3.0'
            and qa.get('bundle_version') == '1.0.271'
            and qa.get('baseline_version') == '1.0.270'
            and qa.get('baseline_sha256') == BASE_SHA
            and qa.get('physical_android_tested') is False
            and qa.get('bundle_sha256') == hashlib.sha256((dist / BUNDLE).read_bytes()).hexdigest()
            and qa.get('standalone_sha256') == hashlib.sha256((dist / SINGLE).read_bytes()).hexdigest()
            and qa.get('real_apk_evidence_sha256') == hashlib.sha256(
                (ROOT / 'publication/real-apk-evidence.json').read_bytes()).hexdigest()), \
            'Completed QA must bind the exact MPPs and local private-APK evidence'
    return qa


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dist', type=Path, required=True)
    args = parser.parse_args()
    validate_qa(args.dist)
    notes = (ROOT / 'publication' / NOTES).read_bytes()
    (args.dist / NOTES).write_bytes(notes)
    entries = {**source_files(), QA: (args.dist / QA).read_bytes(), NOTES: notes}
    with zipfile.ZipFile(args.dist / SOURCE, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for name, raw in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (2026, 10, 22, 0, 0, 0))
            info.external_attr = 0o100644 << 16
            archive.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)
    names = (SINGLE, BUNDLE, QA, SOURCE, NOTES)
    (args.dist / 'SHA256SUMS.txt').write_text(''.join(
        hashlib.sha256((args.dist / name).read_bytes()).hexdigest() + '  ' + name + '\n'
        for name in names))
    print('PASS deterministic Berry source and QA package')


if __name__ == '__main__':
    main()
