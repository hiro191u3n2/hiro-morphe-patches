from pathlib import Path
import hashlib,json
R=Path(__file__).resolve().parent;W=R.parent

def identity(p):
 with p.open('rb') as f:return {'bytes':p.stat().st_size,'sha256':hashlib.file_digest(f,'sha256').hexdigest()}
base=json.loads((R/'piko64-baseline/proof.json').read_text())
assert base['success'] and identity(R/'piko64-baseline/patched.apk')==base['output']
proof_path=W/'dist134-first/QA_HomeBack_v1.1.27.json';build=json.loads(proof_path.read_text());src=W/'repo134/releases/homeback134/src'
for name,sha in build['sources'].items():assert hashlib.sha256((src/name).read_bytes()).hexdigest()==sha, name
mpp=W/'dist134-first/Hiro_Morphe_Patches_v1.0.318.mpp'
assert identity(mpp)=={'bytes':48032024,'sha256':'170367733645b3ae3391a5d58f9426d7c43c01e02b6091b8c5cfc6948d1309af'}
cases=[]
for label in ['piko64-hiro318-final','hiro318-piko64-final']:
 p=R/label/'preservation-proof.json';d=json.loads(p.read_text());assert d['result']=='PASS' and d['mpp']==identity(mpp) and identity(R/label/'patched.apk')==d['output']
 cases.append({'case':label,'requested_source_bundle_order':d['order'],'manager_apply_selected':d['manager_apply']['selected_count'],'manager_patches_done':d['manager_apply']['patches_done_counts'],'manager_finished':d['manager_apply']['finished'],'manager_patch_failures':d['manager_apply']['patch_failures'],'output':d['output'],'evidence_file':str(p.relative_to(W)),'evidence_identity':identity(p),'audits':{k:v['summary'] for k,v in d['audits'].items()},'unchanged_zip_entries':d['unchanged_zip_entries'],'native_libraries_byte_identical':True,'inputs_hash_unchanged_after_each_audit':True})
result={'schema':'homeback134-piko-final-summary-v1','result':'PASS','physical_android_tested':False,'mpp':identity(mpp),'piko':identity(R/'piko-3.11.0-dev.3.mpp'),'original_apk':base['input'],'manager':base['manager'],'host':base['host'],'piko_baseline':{'output':base['output'],'selection_count':base['selected_count'],'patches_done_counts':base['patches_done_counts'],'failure':base['failure'],'proof':identity(R/'piko64-baseline/proof.json')},'source_freeze':{'matching_files':len(build['sources']),'build_proof':identity(proof_path)},'cases':cases,'source_order_outputs_byte_identical':cases[0]['output']==cases[1]['output'],'limitations':['Manager request source-bundle order was reversed; Manager actual patch event sequence is sorted identically in both cases.','Official Piko default 64 selections tested, not an unknown custom 68-selection configuration.','No physical handset execution; this proves application, emitted native hooks/runtime, and preservation only.']}
assert result['source_order_outputs_byte_identical']
(R/'piko-final-summary.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
print(json.dumps({'result':'PASS','summary':str(R/'piko-final-summary.json'),'sha256':identity(R/'piko-final-summary.json')['sha256'],'output':cases[0]['output']}))
