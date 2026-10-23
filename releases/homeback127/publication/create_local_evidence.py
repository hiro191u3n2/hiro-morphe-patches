#!/usr/bin/env python3
"""Bind completed private-APK results to the exact local release; never performs a build or remote write."""
from pathlib import Path
import argparse, hashlib, json

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--dist', type=Path, required=True)
a=p.parse_args()
root=Path(__file__).resolve().parent.parent
pub=root/'publication';ev=root/'evidence/instagram452-release'
sha=lambda data:hashlib.sha256(data).hexdigest()
read=lambda path:json.loads(path.read_text())
qa_raw=(a.dist/'QA_HomeBack_v1.1.17.json').read_bytes();qa=json.loads(qa_raw)
cfg=read(pub/'manifest-template.json');summary=read(ev/'fresh452-summary.json')
signing=read(ev/'signing-alignment.json');piko=read(ev/'piko-compatibility.json')
assert summary['result']=='PASS' and piko['result']=='PASS'
assert summary['version']=='452.1.0.53.64'
assert summary['input_sha256']==signing['input_apk_sha256']==cfg['reviewed_input_apk_sha256']
assert summary['release_qa_sha256']==sha(qa_raw)
for name,digest in summary['evidence_files'].items():
 assert sha((ev/name).read_bytes())==digest, name
assert summary['single_bundle_all_nonsignature_entry_bytes_identical'] is True
assert summary['resource_table_values_numeric_references_configs_preserved'] is True
assert summary['gate_coverage']['exception_routes_match_native_continuation'] is True
assert summary['gate_coverage']['unapproved_branch_handler_entries']==0
assert summary['gate_coverage']['rejected_negative_mutations']>=2
assert summary['executable_counts']['appClasses']==213950
assert summary['executable_counts']['appMethods']==867113
assert all(row['success'] is True and not row['failed_patches'] for row in piko['runs'])
assert {row['selection'] for row in piko['runs']}=={'default','all-nonclone'}
assert piko['hiro_mpp_sha256']==qa['artifacts']['Hiro_Morphe_Patches_v1.0.275.mpp']['sha256']
outputs={}
for name,artifact in qa['artifacts'].items():
 raw=(a.dist/name).read_bytes()
 assert artifact=={'bytes':len(raw),'sha256':sha(raw)}
 row=dict(signing['outputs'][name])
 assert row['result']=='PASS' and row['mpp_sha256']==artifact['sha256']
 assert row['signature_verified'] is True and row['alignment_verified'] is True
 prefix='single' if name.startswith('OneBack_') else 'bundle'
 assert row['output_apk_sha256']==summary[prefix+'_signed_apk_sha256']
 assert row['unsigned_output_apk_sha256']==summary[prefix+'_patched_sha256']
 row.update(native_back_boundaries_verified=True,runtime_classes_match=True,full_semantic_audit=True,noncode_preservation_audited=True,
  resource_preservation_scope='Numeric resource IDs, values, configurations and noncode payload bytes verified; resource entry names may change during rebuild. See fresh452-summary.json for counts and dynamic lookup limitation.')
 outputs[name]=row
records={}
for folder in ['runtime452','instagram452-release']:
 for path in sorted((root/'evidence'/folder).rglob('*')):
  if path.is_file() and path.suffix in ['.txt','.json','.md']:
   records[path.relative_to(root).as_posix()]=sha(path.read_bytes())
evidence={
 'schema':'homeback127-local-apk-validation-v1','result':'PASS','package_name':'com.instagram.android',
 'input_version':'452.1.0.53.64','input_apk_sha256':cfg['reviewed_input_apk_sha256'],
 'input_already_patched':False,'physical_android_tested':False,
 'input_apkm_sha256':piko['input_apkm_sha256'],
 'input_preparation':'Original supplied arm64-v8a APKM locally merged before OneBack application; original unpatched app code. APK and split bytes are not published.',
 'patcher_version':'2.0.0','desktop_version':'1.19.0',
 'artifacts':qa['artifacts'],'runtime_sha256':qa['runtime_sha256'],'outputs':outputs,
 'fresh_executable_audit':summary['executable_counts'],'gate_coverage':summary['gate_coverage'],
 'resource_preservation':{
   'numeric_values_references_configs_preserved':True,'resource_table_byte_identical':False,
   'entry_name_changes':summary['resource_entry_name_changes'],
   'noncode_byte_exact_count':summary['noncode_byte_exact_count'],
   'limits':summary['limitations']
 },
 'piko_coapplication':{
  'release':piko['piko_release'],'mpp_sha256':piko['piko_mpp_sha256'],
  'exact_user_64_patch_selection_known':False,
  'runs':[{'selection':r['selection'],'piko_patch_count':r['piko_patch_count'],'hiro_patch_count':r['hiro_patch_count'],'total_applied':r['total_applied'],'success':r['success'],'failed_patches':r['failed_patches']} for r in piko['runs']],
  'evidence':'evidence/instagram452-release/piko-compatibility.json',
  'scope':'Default and all compatible non-Clone patch sets with default options; not every possible option combination. Only broad non-Clone output received the independent full executable audit. Piko outputs were unsigned; both fresh standalone and aggregate OneBack outputs were separately signed and verified.'
 },
 'evidence_sha256':records,
 'limitations':['No physical Android installation, app screen operation or task-card verification.',
  'Signed APK tests used an ephemeral local key; compatibility with the installed app signing identity is not asserted.',
  'Resource recompilation renamed114 existing drawable entries and named576 unnamed strings; original name-based dynamic resource lookup equivalence is not asserted.',
  'The unknown user64-patch selection was not recreated exactly; broad and default Piko cases passed.']
}
(pub/'local-apk-validation.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
print('PASS local APK evidence bound to final QA,2MPPs,2signed outputs and '+str(len(records))+' text records')
