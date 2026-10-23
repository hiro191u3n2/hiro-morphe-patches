#!/usr/bin/env python3
"""Package authored sources, exact old runtime upgrade references and validation records."""
from pathlib import Path
import argparse, hashlib, importlib.util, shutil

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
parser.add_argument('--legacy', type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location('base_util', args.legacy / 'build.py')
util = importlib.util.module_from_spec(spec)
spec.loader.exec_module(util)
references = {'src/assets/upgrades/oneback-runtime120.dex', 'src/assets/upgrades/oneback-runtime121.dex', 'src/assets/upgrades/oneback-runtime122.dex', 'src/assets/upgrades/oneback-runtime123.dex', 'src/assets/upgrades/oneback-runtime124.dex', 'src/assets/upgrades/oneback-runtime125.dex'}
entries = {}
EVIDENCE_DIRS = {'runtime452', 'instagram452-release'}
for folder in ('src', 'publication', 'evidence'):
    for path in sorted((root / folder).rglob('*')):
        name = path.relative_to(root).as_posix()
        relative = path.relative_to(root)
        if folder == 'evidence' and (len(relative.parts) < 3 or relative.parts[1] not in EVIDENCE_DIRS):
            continue
        if path.is_file() and '__pycache__' not in path.parts and (
                path.suffix in ('.java', '.py', '.json', '.txt', '.md', '.yml', '.sh') or name in references):
            entries[name] = path.read_bytes()
for name in references:
    assert name in entries, 'Missing exact runtime reference: ' + name
if (root / 'README.md').is_file():
    entries['README.md'] = (root / 'README.md').read_bytes()
for name in ('build.py', 'test-dex/DexAudit.java', 'test-dex/Fixtures.java'):
    entries['legacy/' + name] = (args.legacy / name).read_bytes()
qa = 'QA_HomeBack_v1.1.17.json'
entries[qa] = (args.dist / qa).read_bytes()
source_zip = 'HomeBack_v1.1.17_sources_and_QA.zip'
util.write_zip(args.dist / source_zip, entries)
shutil.copyfile(root / 'publication/RELEASE_NOTES.txt', args.dist / 'RELEASE_NOTES.txt')
names = ('OneBack_Exit_NoRecents_v1.1.17.mpp', 'Hiro_Morphe_Patches_v1.0.275.mpp', source_zip, qa, 'RELEASE_NOTES.txt')
(args.dist / 'SHA256SUMS.txt').write_text(''.join(hashlib.sha256((args.dist / name).read_bytes()).hexdigest() + '  ' + name + '\n' for name in names))
print('PASS packaged exact source, QA and six release assets; no user APK included')
