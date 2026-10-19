#!/usr/bin/env python3
"""Package exact reviewed text sources and final QA; omit all APKs and build tools."""
import argparse,importlib.util,zipfile
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--dist',type=Path,required=True);args=parser.parse_args()
root=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('publication',root/'publish.py');pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
entries=pub.reviewed_files()
for name in ('manifest-template.json','manifest.json'):entries['publication/'+name]=(root/name).read_bytes()
entries[pub.QA_NAME]=(args.dist/pub.QA_NAME).read_bytes()
with zipfile.ZipFile(args.dist/pub.SOURCE_ZIP,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for name,raw in sorted(entries.items()):
        info=zipfile.ZipInfo(name,date_time=(2026,10,19,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;archive.writestr(info,raw)
(args.dist/'RELEASE_NOTES.txt').write_bytes((root/'RELEASE_NOTES.txt').read_bytes())
(args.dist/'SHA256SUMS.txt').write_text(''.join(pub.sha((args.dist/name).read_bytes())+'  '+name+'\n' for name in pub.ASSETS if name!='SHA256SUMS.txt'))
print('PASS exact sources, QA and six release assets packaged; APKs excluded')
