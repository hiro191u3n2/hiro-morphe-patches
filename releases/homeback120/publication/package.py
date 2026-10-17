#!/usr/bin/env python3
from pathlib import Path
import argparse,hashlib,importlib.util,shutil
p=argparse.ArgumentParser();p.add_argument('--dist',type=Path,required=True);p.add_argument('--legacy',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parent.parent
s=importlib.util.spec_from_file_location('base_util',a.legacy/'build.py');u=importlib.util.module_from_spec(s);s.loader.exec_module(u)
BINARY_FIXTURES=('evidence/layout_clips_comment_composer.xml.bin', 'evidence/layout_clips_viewer_fragment.xml.bin', 'evidence/layout_clips_viewer_fragment_two_pane_comments.xml.bin', 'evidence/layout_clips_viewer_refresh_container.xml.bin')
entries={str(p.relative_to(root)):p.read_bytes() for folder in [root/'src',root/'publication',root/'evidence'] for p in sorted(folder.rglob('*')) if p.is_file() and p.suffix in ('.java','.py','.json','.txt','.md','.yml') and '__pycache__' not in p.parts}
for name in BINARY_FIXTURES:entries[name]=(root/name).read_bytes()
for n in ['README.md']:
 if (root/n).exists():entries[n]=(root/n).read_bytes()
for n in ['build.py','test-dex/DexAudit.java','test-dex/Fixtures.java']:entries['legacy/'+n]=(a.legacy/n).read_bytes()
entries['QA_HomeBack_v1.1.10.json']=(a.dist/'QA_HomeBack_v1.1.10.json').read_bytes()
u.write_zip(a.dist/'HomeBack_v1.1.10_sources_and_QA.zip',entries)
shutil.copyfile(root/'publication/RELEASE_NOTES.txt',a.dist/'RELEASE_NOTES.txt')
names=['OneBack_Exit_NoRecents_v1.1.10.mpp','Hiro_Morphe_Patches_v1.0.249.mpp','HomeBack_v1.1.10_sources_and_QA.zip','QA_HomeBack_v1.1.10.json','RELEASE_NOTES.txt']
(a.dist/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256((a.dist/n).read_bytes()).hexdigest()+'  '+n+'\n' for n in names))
print('PASS packaged source, QA and six release assets')
