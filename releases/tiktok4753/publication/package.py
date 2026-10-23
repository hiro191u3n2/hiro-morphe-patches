#!/usr/bin/env python3
"""Package corresponding source, exact build/real-APK proofs, notices and seven assets; no APK."""
from pathlib import Path
import argparse, importlib.util, json, shutil, zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist', type=Path, required=True)
parser.add_argument('--repo', type=Path, required=True)
args = parser.parse_args(); root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('publication', root/'publish.py')
pub = importlib.util.module_from_spec(spec); spec.loader.exec_module(pub)
qa_raw = (args.dist/pub.QA_NAME).read_bytes(); qa = json.loads(qa_raw)
pub.validate_qa(qa)
real_raw = (pub.SOURCE_ROOT/'src/validation/real-apk.json').read_bytes()
pub.validate_real_apk(json.loads(real_raw), qa['artifacts'])
for name, row in qa['artifacts'].items():
    raw = (args.dist/name).read_bytes()
    pub.require(row == {'bytes': len(raw), 'sha256': pub.sha(raw)}, 'Artifact identity differs: '+name)
(args.dist/pub.REAL_QA_NAME).write_bytes(real_raw)
entries = pub.expected_source_archive(args.repo.resolve(), qa_raw, real_raw)
for name, raw in entries.items():
    pub.checked_path(name); raw.decode('utf-8'); pub.require(b'\x00' not in raw, 'Binary source entry: '+name)
with zipfile.ZipFile(args.dist/pub.SOURCE_ZIP, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
    for name, raw in sorted(entries.items()):
        info = zipfile.ZipInfo(name, (2020,1,1,0,0,0)); info.compress_type = zipfile.ZIP_DEFLATED
        info.create_system = 3; info.external_attr = 0o100644 << 16
        archive.writestr(info, raw, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
shutil.copyfile(root/'RELEASE_NOTES.txt', args.dist/'RELEASE_NOTES.txt')
names = [name for name in pub.ASSETS if name != 'SHA256SUMS.txt']
(args.dist/'SHA256SUMS.txt').write_text(''.join(pub.sha((args.dist/name).read_bytes())+'  '+name+'\n' for name in names))
print('PASS packaged complete current/inherited source, licenses, exact build and real-APK proofs; seven assets; no APK')
