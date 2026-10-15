from pathlib import Path
import hashlib,json,sys
first,second=map(Path,sys.argv[1:3])
names=['OneBack_Exit_NoRecents_v1.1.5.mpp','Hiro_Morphe_Patches_v1.0.240.mpp','QA_HomeBack_v1.1.5.json']
for name in names:
 assert (first/name).read_bytes()==(second/name).read_bytes(),'Fresh builds differ: '+name
root=Path(__file__).resolve().parent
cfg=json.loads((root/'manifest-template.json').read_text())
qa=json.loads((first/names[-1]).read_text())
assert qa['host']=='SUCCESS: 229 cases, 1140 assertions'
assert qa['physical_android_tested'] is False and qa['third_party_apk_tested'] is False
assert len(qa['synthetic'])==10 and all(x['result']=='PASS' for x in qa['synthetic'])
assert cfg['local_expected_mpp']==qa['artifacts'],'CI MPP differs from reviewed local SDK build'
assert cfg['local_expected_qa_sha256']==hashlib.sha256((first/names[-1]).read_bytes()).hexdigest(),'CI QA differs from reviewed local SDK build'
cfg['expected_mpp']=qa['artifacts']
cfg['expected_qa_sha256']=hashlib.sha256((second/names[-1]).read_bytes()).hexdigest()
(root/'manifest.json').write_text(json.dumps(cfg,ensure_ascii=False,indent=2)+'\n')
print('PASS two fresh SDK builds MPP and QA byte-identical; '+qa['host'])
