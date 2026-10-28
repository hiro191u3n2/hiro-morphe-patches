#!/usr/bin/env python3
"""Reject altered copies of real evidence without inventing success records."""
import argparse, copy, json
from pathlib import Path
import publish as pub

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--dist',type=Path,required=True);a=p.parse_args()
    qa=json.loads((a.dist/pub.QA_NAME).read_text())
    pub.validate_qa(qa)
    qa_changes=[
        ('missing snapshot-member reports',lambda q:q.pop('mutable_members',None)),
        ('missing actual Manager serialization report',lambda q:q['mutable_members'].pop('manager237-host.jar')),
        ('nearby SDK serialization report substituted',lambda q:q['mutable_members'].__setitem__(
            'manager238-host.jar',q['mutable_members'].pop('manager237-host.jar'))),
        ('failed serialized-member regression',lambda q:q['mutable_members'].__setitem__(
            'manager237-host.jar','FAIL Instagram132MutableContextAudit snapshot-members: missing added bridge')),
        ('different mutable-context audit mode',lambda q:q['mutable_members'].__setitem__(
            'manager237-host.jar','PASS Instagram132MutableContextAudit: original body checks')),
    ]
    for name,change in qa_changes:
        q=copy.deepcopy(qa);change(q)
        try:pub.validate_qa(q)
        except RuntimeError:continue
        raise AssertionError('Publication incorrectly accepted '+name)
    proof=json.loads((pub.SOURCE_ROOT/'src/validation/real-apk.json').read_text());artifacts=pub.artifacts_at(a.dist)
    pub.validate_real_apk(proof,artifacts)
    changes=[
        ('prior schema',lambda q:q.__setitem__('schema','homeback131-real-apk-v1')),
        ('wrong original version',lambda q:q['input'].__setitem__('version_name','447.0')),
        ('wrong original bytes',lambda q:q['input'].__setitem__('sha256','0'*64)),
        ('unsupported device claim',lambda q:q.__setitem__('physical_android_tested',True)),
        ('different MPP',lambda q:q['cases'][0].__setitem__('artifact_sha256','0'*64)),
        ('missing original contract',lambda q:q.pop('original_profile_contract',None)),
        ('wrong baseline resource artifact',lambda q:q['baseline_resource_reference'].__setitem__('artifact_sha256','0'*64)),
        ('wrong baseline resource input',lambda q:q['baseline_resource_reference'].__setitem__('input_sha256','0'*64)),
        ('wrong baseline resource patcher',lambda q:q['baseline_resource_reference'].__setitem__('patcher_sdk_sha256','0'*64)),
        ('different case input',lambda q:q['cases'][0].__setitem__('input_sha256','0'*64)),
        ('different patcher',lambda q:q['cases'][0].__setitem__('patcher_sdk_sha256','0'*64)),
        ('failed patch',lambda q:q['cases'][0].__setitem__('failed_patches',['failure'])),
        ('failed pipeline',lambda q:q['cases'][0].__setitem__('steps_all_success',False)),
        ('missing profile output audit',lambda q:q['cases'][0].__setitem__('profile_link_audit','')),
        ('original audit used as output',lambda q:q['cases'][0].__setitem__('profile_link_audit','PASS Instagram130ProfileLinkAudit original')),
        ('missing native preservation audit',lambda q:q['cases'][0].pop('existing_native_audit',None)),
        ('missing manifest audit',lambda q:q['cases'][0].pop('manifest_audit',None)),
        ('changed native libraries',lambda q:q['cases'][0]['resources'].__setitem__('native_libraries_byte_identical',False)),
        ('missing preserved resources',lambda q:q['cases'][0]['resources'].__setitem__('byte_identical_entries',0)),
        ('missing resource-table audit',lambda q:q['cases'][0]['resources'].pop('resource_table',None)),
        ('new resource semantics',lambda q:q['cases'][0]['resources']['resource_table'].__setitem__('baseline_semantics_identical',False)),
        ('changed resource values',lambda q:q['cases'][0]['resources']['resource_table'].__setitem__('resource_ids_and_values_preserved',False)),
        ('wrong baseline resource table',lambda q:q['cases'][0]['resources']['resource_table'].__setitem__('baseline_sha256','0'*64)),
        ('wrong original resource table',lambda q:q['cases'][0]['resources']['resource_table'].__setitem__('original_sha256','0'*64)),
        ('missing output hash',lambda q:q['cases'][0].__setitem__('output_apk_sha256','')),
        ('missing selected patch',lambda q:q['cases'][0].__setitem__('selected_patches',[])),
        ('wrong selected patch',lambda q:q['cases'][0].__setitem__('selected_patches',['Different patch'])),
        ('missing applied patch',lambda q:q['cases'][0].__setitem__('applied_patches',[])),
        ('missing final-MPP case',lambda q:q.__setitem__('cases',q['cases'][:1])),
        ('duplicate case',lambda q:q.__setitem__('cases',[q['cases'][0],q['cases'][0]])),
        ('missing mutable-context regression',lambda q:q.__setitem__('original_profile_contract',
            '\n'.join(line for line in q['original_profile_contract'].splitlines()
                      if not line.startswith('PASS Instagram132MutableContextAudit:')))),
        ('missing actual Manager matrix',lambda q:q.pop('manager_compatibility',None)),
        ('diagnostic partial Manager matrix',lambda q:q['manager_compatibility'].__setitem__('complete',False)),
        ('Manager physical-device claim',lambda q:q['manager_compatibility'].__setitem__('physical_android_tested',True)),
        ('different embedded Manager host',lambda q:q['manager_compatibility']['manager']['host'].__setitem__('sha256','0'*64)),
        ('modified Manager host',lambda q:q['manager_compatibility']['manager'].__setitem__('host_unmodified',False)),
        ('different Manager source',lambda q:q['manager_compatibility']['manager'].__setitem__('source_commit','0'*40)),
        ('nearby Patcher substituted',lambda q:q['manager_compatibility']['patcher'].__setitem__('version','2.1.0-dev.3')),
        ('different Piko artifact',lambda q:q['manager_compatibility']['piko'].__setitem__('sha256','0'*64)),
        ('different old Hiro reproduction',lambda q:q['manager_compatibility']['baseline_hiro'].__setitem__('sha256','0'*64)),
        ('Manager evidence for another final bundle',lambda q:q['manager_compatibility']['artifacts'][pub.COMBINED].__setitem__('sha256','0'*64)),
        ('different selection helper',lambda q:q['manager_compatibility'].__setitem__('selection_helper_sha256','0'*64)),
        ('different Manager harness',lambda q:q['manager_compatibility'].__setitem__('validation_script_sha256','0'*64)),
        ('missing explicit test limitations',lambda q:q['manager_compatibility'].__setitem__('limitations',[])),
        ('missing final selection manifest',lambda q:q['manager_compatibility']['selection_manifests'].pop('aggregate')),
        ('changed actual Piko defaults',lambda q:q['manager_compatibility']['selection_manifests']['piko']['patches'][0].__setitem__(
            'default',not q['manager_compatibility']['selection_manifests']['piko']['patches'][0]['default'])),
        ('missing Manager case',lambda q:q['manager_compatibility']['cases'].pop()),
        ('reordered Manager cases',lambda q:q['manager_compatibility']['cases'].reverse()),
        ('duplicate Manager case',lambda q:q['manager_compatibility']['cases'].__setitem__(1,q['manager_compatibility']['cases'][0])),
        ('old rejection without navigation',lambda q:q['manager_compatibility']['cases'][0].__setitem__('applied_patches',[])),
        ('old rejection without failed patch event',lambda q:q['manager_compatibility']['cases'][0].__setitem__('failed_patches',[])),
        ('wrong reproduced native failure',lambda q:q['manager_compatibility']['cases'][0].__setitem__('expected_failure_message','Different failure')),
        ('old rejection incorrectly completed',lambda q:q['manager_compatibility']['cases'][0].__setitem__('patches_done_counts',[2])),
        ('old rejection with generated APK',lambda q:q['manager_compatibility']['cases'][0].__setitem__('output_apk_sha256','a'*64)),
        ('Manager failure treated as success',lambda q:q['manager_compatibility']['cases'][1].__setitem__('finished_failure',{'outOfMemory':True})),
        ('Manager zero exit without final event',lambda q:q['manager_compatibility']['cases'][1].pop('finished_failure')),
        ('Manager patch failure ignored',lambda q:q['manager_compatibility']['cases'][1].__setitem__('failed_patches',[{'name':pub.PATCH_NAME}])),
        ('Manager selected patch not applied',lambda q:q['manager_compatibility']['cases'][1].__setitem__('applied_patches',[pub.NAVIGATION])),
        ('wrong Manager completed patch count',lambda q:q['manager_compatibility']['cases'][1].__setitem__('patches_done_counts',[1])),
        ('Manager host process failed',lambda q:q['manager_compatibility']['cases'][1].__setitem__('process_exit_code',1)),
        ('missing Manager output identity',lambda q:q['manager_compatibility']['cases'][1].__setitem__('output_apk_sha256','')),
        ('missing Manager event identity',lambda q:q['manager_compatibility']['cases'][1].__setitem__('events_sha256','0'*64)),
        ('missing Manager request identity',lambda q:q['manager_compatibility']['cases'][1].__setitem__('request_sha256','0'*64)),
        ('guessed Manager selection key',lambda q:q['manager_compatibility']['cases'][1]['selected_bundles'][0].__setitem__('selected_patch_keys',['guessed-key'])),
        ('unrecorded Manager option override',lambda q:q['manager_compatibility']['cases'][1]['selected_bundles'][0].__setitem__('option_overrides',{'changed':True})),
        ('reversed Manager bundle order',lambda q:q['manager_compatibility']['cases'][1]['selected_bundles'].reverse()),
        ('Piko baseline includes OneBack',lambda q:q['manager_compatibility']['cases'][2]['selected_bundles'].append(q['manager_compatibility']['cases'][3]['selected_bundles'][1])),
        ('final Piko combination omits OneBack',lambda q:q['manager_compatibility']['cases'][3]['selected_bundles'].pop()),
        ('unverified user 68 selection claim',lambda q:q['manager_compatibility']['cases'][3].__setitem__('selected_count',68)),
        ('missing Manager preservation proof',lambda q:q.pop('manager_preservation',None)),
        ('wrong Manager preservation input',lambda q:q['manager_preservation'].__setitem__('input_sha256','0'*64)),
        ('wrong Manager preservation output',lambda q:q['manager_preservation'].__setitem__('output_sha256','0'*64)),
        ('wrong Manager baseline case',lambda q:q['manager_preservation'].__setitem__('baseline_case','old311-navigation')),
        ('missing Manager profile hooks audit',lambda q:q['manager_preservation'].pop('profile_link_audit')),
        ('missing Manager whole-APK audit',lambda q:q['manager_preservation'].pop('existing_native_audit')),
        ('stock manifest audit used for Piko',lambda q:q['manager_preservation'].__setitem__('manifest_audit','PASS InstagramManifestAudit: stock')),
        ('changed Piko native libraries',lambda q:q['manager_preservation']['resources'].__setitem__('native_libraries_byte_identical',False)),
        ('missing Piko resource entry proof',lambda q:q['manager_preservation']['resources'].__setitem__('byte_identical_entries',0)),
        ('stock resource audit used for Piko',lambda q:q['manager_preservation']['resources']['resource_table'].__setitem__('audit','PASS InstagramResourcesAudit: stock')),
        ('changed Piko resource values',lambda q:q['manager_preservation']['resources']['resource_table'].__setitem__('resource_ids_and_values_preserved',False)),
        ('changed Piko resource semantics',lambda q:q['manager_preservation']['resources']['resource_table'].__setitem__('baseline_semantics_identical',False)),
        ('wrong Piko baseline table',lambda q:q['manager_preservation']['resources']['resource_table'].__setitem__('baseline_sha256','0'*64)),
    ]
    for name,change in changes:
        q=copy.deepcopy(proof);change(q)
        try:pub.validate_real_apk(q,artifacts)
        except RuntimeError:continue
        raise AssertionError('Publication incorrectly accepted '+name)
    print('PASS publication evidence rejection checks='+str(len(qa_changes)+len(changes))
          +'; build='+str(len(qa_changes))+'; real-APK='+str(len(changes))
          +'; actual evidence copies, no fabricated success proof')

if __name__=='__main__':main()
