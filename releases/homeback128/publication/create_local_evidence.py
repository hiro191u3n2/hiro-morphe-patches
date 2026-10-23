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
qa_raw=(a.dist/'QA_HomeBack_v1.1.18.json').read_bytes();qa=json.loads(qa_raw)
cfg=read(pub/'manifest-template.json');summary=read(ev/'fresh452-summary.json')
signing=read(ev/'signing-alignment.json')
upgrade=read(ev/'upgrade127-summary.json')
assert upgrade['result']=='PASS' and upgrade['physical_android_tested'] is False
assert upgrade['input_version']=='452.1.0.53.64'
assert upgrade['input_apk_sha256']==cfg['reviewed_upgrade_input_apk_sha256']
assert upgrade['prior_runtime_sha256']=='818800fec2272345ac6ff60d5fc86ef769b83746aa89544638f7a6ad44a0231c'
assert upgrade['mpp_sha256']==qa['artifacts'][upgrade['mpp_name']]['sha256']
assert upgrade['runtime_sha256']==qa['runtime_sha256']
assert upgrade['native_back_boundaries_verified'] is True
assert upgrade['runtime_classes_match'] is True
assert upgrade['unrelated_app_code_unchanged'] is True
assert summary['result']=='PASS'
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
assert summary['executable_counts']['appClasses']>0
assert summary['executable_counts']['appMethods']>0
assert summary.get('physical_android_execution',False) is False
assert summary['audited_to_signed_nonsignature_entry_bytes_identical'] is True
outputs={}
for name,artifact in qa['artifacts'].items():
 raw=(a.dist/name).read_bytes()
 assert artifact=={'bytes':len(raw),'sha256':sha(raw)}
 row=dict(signing['outputs'][name])
 assert row['result']=='PASS' and row['mpp_sha256']==artifact['sha256']
 assert row['signature_verified'] is True and row['alignment_verified'] is True
 prefix='single' if name.startswith('OneBack_') else 'bundle'
 assert summary[prefix+'_mpp_sha256']==artifact['sha256']
 assert row['output_apk_sha256']==summary[prefix+'_signed_apk_sha256']
 assert row['unsigned_output_apk_sha256']==summary[prefix+'_patched_sha256']
 row.update(native_back_boundaries_verified=True,runtime_classes_match=True,full_semantic_audit=True,noncode_preservation_audited=True,
  resource_preservation_scope='Numeric resource IDs, values, configurations and noncode payload bytes verified; resource entry names may change during rebuild. See fresh452-summary.json for counts and dynamic lookup limitation.')
 outputs[name]=row
records={}
for folder in ['runtime128','instagram452-release']:
 for path in sorted((root/'evidence'/folder).rglob('*')):
  if path.is_file() and path.suffix in ['.txt','.json','.md']:
   records[path.relative_to(root).as_posix()]=sha(path.read_bytes())
evidence={
 'schema':'homeback128-local-apk-validation-v1','result':'PASS','package_name':'com.instagram.android',
 'input_version':'452.1.0.53.64','input_apk_sha256':cfg['reviewed_input_apk_sha256'],
 'input_already_patched':False,'physical_android_tested':False,
 'input_apkm_sha256':cfg['reviewed_input_apkm_sha256'],
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
 'upgrade127_validation':upgrade,
 'piko_coapplication_this_release':False,
 'piko_existing_apk_upgrade_validation':read(ev/'upgrade127-piko-summary.json'),
 'evidence_sha256':records,
 'limitations':['No physical Android installation, app screen operation or task-card verification.',
  'Signed APK tests used an ephemeral local key; compatibility with the installed app signing identity is not asserted.',
  'Resource entry-name changes are counted in fresh452-summary.json; original name-based dynamic resource lookup equivalence is not asserted.',
  'Only current OneBack was applied to a previously-built Piko3.11.0-dev.1 all-nonclone65+OneBack127 APK. The update preserves existing app/Piko code and adds explicit enableOnBackInvokedCallback=true to seven Piko-added activities under the existing manifest policy; all other manifest content and 24,530 other noncode entries are unchanged. Piko patches were not newly coapplied, and the unknown user64 selection was not recreated. Upgrade outputs were unsigned; fresh standalone/aggregate outputs were separately signed and verified.']
}
(pub/'local-apk-validation.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
print('PASS current-release APK evidence bound to final QA,2MPPs,2signed outputs and '+str(len(records))+' fresh text records')
