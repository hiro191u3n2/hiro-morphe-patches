#!/usr/bin/env python3
"""Package complete reviewed text source, notices and exact QA; no APK or binary source."""
from pathlib import Path
import argparse,importlib.util,json,shutil,zipfile
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--dist',type=Path,required=True)
parser.add_argument('--legacy',type=Path,required=True)
args=parser.parse_args();root=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('publication',root/'publish.py')
pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
qa_raw=(args.dist/pub.QA_NAME).read_bytes();pub.validate_qa(json.loads(qa_raw))
entries={name:path.read_bytes() for name,path in pub.release_sources().items()}
for name in pub.LEGACY_FILES:entries['legacy/'+name]=(args.legacy/name).read_bytes()
entries[pub.QA_NAME]=qa_raw
for name,raw in entries.items():
    pub.checked_path(name);raw.decode('utf-8');pub.require(b'\x00' not in raw,'Binary source entry: '+name)
with zipfile.ZipFile(args.dist/pub.SOURCE_ZIP,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for name,raw in sorted(entries.items()):
        info=zipfile.ZipInfo(name,(2020,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
        info.create_system=3;info.external_attr=0o100644<<16
        archive.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
shutil.copyfile(root/'RELEASE_NOTES.txt',args.dist/'RELEASE_NOTES.txt')
names=(pub.SINGLE,pub.COMBINED,pub.SOURCE_ZIP,pub.QA_NAME,'RELEASE_NOTES.txt')
(args.dist/'SHA256SUMS.txt').write_text(''.join(pub.sha((args.dist/name).read_bytes())+'  '+name+'\n' for name in names))
print('PASS packaged complete source, LICENSE/NOTICE, CSV, QA and six release assets; no APK included')
