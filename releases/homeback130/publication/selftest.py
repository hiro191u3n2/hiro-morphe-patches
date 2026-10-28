#!/usr/bin/env python3
"""Reject altered copies of real evidence without inventing success records."""
import argparse, copy, json
from pathlib import Path
import publish as pub

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--dist',type=Path,required=True);a=p.parse_args()
    proof=json.loads((pub.SOURCE_ROOT/'src/validation/real-apk.json').read_text());artifacts=pub.artifacts_at(a.dist)
    pub.validate_real_apk(proof,artifacts)
    changes=[
        ('prior schema',lambda q:q.__setitem__('schema','homeback129-real-apk-v1')),
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
    ]
    for name,change in changes:
        q=copy.deepcopy(proof);change(q)
        try:pub.validate_real_apk(q,artifacts)
        except RuntimeError:continue
        raise AssertionError('Publication incorrectly accepted '+name)
    print('PASS publication evidence rejection checks='+str(len(changes))+'; actual evidence copies, no fabricated success proof')

if __name__=='__main__':main()
