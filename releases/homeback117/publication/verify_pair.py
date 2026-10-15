from pathlib import Path
import hashlib,json,os,sys
first,second=map(Path,sys.argv[1:3])
names=['OneBack_Exit_NoRecents_v1.1.7.mpp','Hiro_Morphe_Patches_v1.0.243.mpp','QA_HomeBack_v1.1.7.json']
for name in names:
 assert (first/name).read_bytes()==(second/name).read_bytes(),'Fresh builds differ: '+name
root=Path(__file__).resolve().parent
cfg=json.loads((root/'manifest-template.json').read_text())
qa=json.loads((first/names[-1]).read_text())
assert qa['host']==cfg['local_expected_host']
assert qa['host_counts']['cases']>=259 and qa['host_counts']['assertions']>=1260
assert qa['provided_apk_static_audit_evidence'] and qa['provided_apk_inputs_already_patched'] is True
assert qa['physical_android_tested'] is False and qa['third_party_apk_tested'] is False
assert len(qa['synthetic'])==10 and all(x['result']=='PASS' for x in qa['synthetic'])
assert cfg['local_expected_mpp']==qa['artifacts'],'CI MPP differs from reviewed local SDK build'
assert cfg['local_expected_qa_sha256']==hashlib.sha256((first/names[-1]).read_bytes()).hexdigest(),'CI QA differs from reviewed local SDK build'
cfg['expected_mpp']=qa['artifacts']
cfg['expected_qa_sha256']=hashlib.sha256((second/names[-1]).read_bytes()).hexdigest()
cfg['ci_build_pair_verified']=os.environ.get('GITHUB_ACTIONS')=='true' and bool(os.environ.get('GITHUB_RUN_ID')) and bool(os.environ.get('GITHUB_SHA'))
cfg['ci_run_id']=os.environ.get('GITHUB_RUN_ID') if cfg['ci_build_pair_verified'] else None
cfg['ci_source_commit']=os.environ.get('GITHUB_SHA') if cfg['ci_build_pair_verified'] else None
(root/'manifest.json').write_text(json.dumps(cfg,ensure_ascii=False,indent=2)+'\n')
print('PASS two fresh SDK builds MPP and QA byte-identical; '+qa['host'])
