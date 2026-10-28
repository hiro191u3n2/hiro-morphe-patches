#!/usr/bin/env python3
"""Mutate copies of actual evidence to check publication rejects incomplete/wrong results."""
import argparse, copy, json
from pathlib import Path
import publish as pub

def main():
    p = argparse.ArgumentParser(description=__doc__); p.add_argument('--dist',type=Path,required=True); a=p.parse_args()
    proof=json.loads((pub.SOURCE_ROOT/'src/validation/real-apk.json').read_text()); artifacts=pub.artifacts_at(a.dist)
    pub.validate_real_apk(proof,artifacts)
    changes=[
        ('old release evidence schema',lambda q:q.__setitem__('schema','tiktok-link-direct-real-apk-v1')),
        ('wrong version',lambda q:q['input'].__setitem__('version_name','47.5.3')),
        ('wrong original bytes',lambda q:q['input'].__setitem__('sha256','0'*64)),
        ('signature not verified',lambda q:q['input'].__setitem__('signature_verified',False)),
        ('signature output absent',lambda q:q.pop('original_signature',None)),
        ('wrong signer output',lambda q:q.__setitem__('original_signature','PASS OriginalIdentity verified=true signer_certificate_sha256='+'0'*64)),
        ('unsupported device claim',lambda q:q.__setitem__('physical_android_tested',True)),
        ('different MPP',lambda q:q['cases'][0].__setitem__('artifact_sha256','0'*64)),
        ('failed patch',lambda q:q['cases'][0].__setitem__('failed_patches',['failure'])),
        ('failed pipeline',lambda q:q['cases'][0].__setitem__('steps_all_success',False)),
        ('missing output audit',lambda q:q['cases'][0].__setitem__('direct_link_audit','')),
        ('unrelated output audit',lambda q:q['cases'][0].__setitem__('direct_link_audit','PASS DifferentAudit')),
        ('original audit used as output',lambda q:q['cases'][0].__setitem__('direct_link_audit','PASS LinkBranchAudit original: original input only')),
        ('missing target-first routing audit',lambda q:q['cases'][0].pop('fast_route_audit',None)),
        ('unrelated routing audit',lambda q:q['cases'][0].__setitem__('fast_route_audit','PASS DifferentAudit output:')),
        ('original routing audit used as output',lambda q:q['cases'][0].__setitem__('fast_route_audit','PASS LinkFastRouteAudit original: original input only')),
        ('missing output hash',lambda q:q['cases'][0].__setitem__('output_apk_sha256','')),
        ('missing selected patches',lambda q:q['cases'][0].__setitem__('selected_patches',[])),
        ('missing aggregate or single',lambda q:q.__setitem__('cases',q['cases'][:1])),
    ]
    for name,change in changes:
        mutated=copy.deepcopy(proof);change(mutated)
        try:pub.validate_real_apk(mutated,artifacts)
        except RuntimeError:continue
        raise AssertionError('Publication incorrectly accepted '+name)
    mutated=copy.deepcopy(proof)
    for case in mutated['cases']:
        case['selected_patches']=['A different patch']
        case['applied_patches']=['A different patch']
    try:pub.validate_real_apk(mutated,artifacts)
    except RuntimeError:pass
    else:raise AssertionError('Publication accepted success without the new direct-link patch')
    print('PASS publication evidence rejection checks='+str(len(changes)+1)+'; tested actual evidence copies, no fabricated success proof')

if __name__ == '__main__':main()
