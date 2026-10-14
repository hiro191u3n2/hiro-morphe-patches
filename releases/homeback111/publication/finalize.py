#!/usr/bin/env python3
"""Accept only identical independently built outputs and the locally tested sources."""
from pathlib import Path
import argparse,hashlib,json
root=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser();p.add_argument('--first',type=Path,required=True);p.add_argument('--second',type=Path,required=True);p.add_argument('--work',type=Path,required=True);a=p.parse_args()
sha=lambda data:hashlib.sha256(data).hexdigest()
qa_name='QA_HomeBack_v1.1.1.json'
qa=json.loads((a.first/qa_name).read_text())
assert sha(json.dumps(qa['sources'],sort_keys=True,separators=(',',':')).encode())=='3e9a714f4382579f18dcc82ba0413accd7501c67a8eacf5bf6e845d456c1285f', 'CI source inventory differs from locally reviewed sources'
assert qa['host']=='SUCCESS: 60 cases, 238 assertions'
assert sha((a.work/'host-tests.txt').read_bytes())=='af79b879ef1f45f6d9cf73b9acbbe234cdee5aba9235bcd73ea58b0908944563', 'Host output differs from local run'
names=['OneBack_Exit_NoRecents_v1.1.1.mpp','Hiro_Morphe_Patches_v1.0.236.mpp',qa_name]
for name in names:
 assert (a.first/name).read_bytes()==(a.second/name).read_bytes(), 'Repeated build differs: '+name
cfg_path=root/'publication/manifest.json';cfg=json.loads(cfg_path.read_text())
assert cfg['patch_version']=='1.1.1' and cfg['bundle_version']=='1.0.236'
cfg['expected_mpp']=qa['artifacts'];cfg['expected_qa_sha256']=sha((a.first/qa_name).read_bytes())
cfg['validation_basis']='locally tested sources and twice byte-identical CI builds; no local SDK/DEX build or device test claimed'
cfg_path.write_text(json.dumps(cfg,ensure_ascii=False,indent=2)+'\n')
print('PASS two independent MPP and QA builds byte-identical; all source hashes and local host results matched')
