#!/usr/bin/env python3
"""Negative-input tests for publication gate parsing; no release evidence mocked."""
from pathlib import Path
import argparse, importlib.util, json, sys, zipfile

def native_negative_controls(source,raw,work):
    import native_delta1996 as delta
    count=0;fingerprint=delta.current_fingerprint(source)
    build=json.loads((source.parent/'dist/build1996.json').read_text())
    report=build['gpu_native']['identity_delta']
    delta.verify(raw,fingerprint,report,source);count+=1
    delta.verify_native_sources(source);count+=1
    assert report['inverse_rebuild_sha256']==delta.BASE_SHA256 and report['same_ndk_inverse_rebuild_verified'];count+=1
    sections=delta.sections(raw)
    def rejected(value,proof=report,expected=fingerprint):
        nonlocal count
        try:delta.verify(value,expected,proof,source)
        except (AssertionError,ValueError):count+=1
        else:raise AssertionError('Changed native or altered reviewed proof accepted')
    positions=[0,18,40]+[row[4] for name,row in sections.items() if row[1]!=8 and row[5]>0 and name not in ('',)]
    for position in positions:
        changed=bytearray(raw);changed[position]^=1;rejected(bytes(changed))
    rejected(raw+b'\0');rejected(raw[:-1]);rejected(raw,expected='0'*64);rejected(raw,expected='bad')
    for update in ({'current_sha256':'0'*64},{'current_bytes':len(raw)-1},{'baseline_sha256':'0'*64},
          {'inverse_rebuild_sha256':'0'*64},{'same_ndk_inverse_rebuild_verified':False},
          {'generated_header_delta':['strong1960_source']},{'changed_native_sources':[]},
          {'all_other_native_shader_and_cpu_sources_identical_to_1995':False},
          {'compute_manifest_sha256':'0'*64},{'native_source_sha256':{}}):
        rejected(raw,{**report,**update})
    # Mutate detached source copies, never the live production tree being built.
    copied=work/'negative-sources';copied.mkdir()
    proof_sources=list(report['native_source_sha256'])+[delta.REFERENCE,'native_delta1996_baseline.json','native1960/compute_dependencies1990.json']
    for name in proof_sources:
        target=copied/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes((source/name).read_bytes())
    delta.verify_native_sources(copied);count+=1
    for name in ('native1960/engine1960.c','native1960/build_native1960.py','native1955/single_noise1955.c',
            'native1960/finish1961.comp',delta.SHADER,delta.REFERENCE,'native1960/compute_dependencies1990.json'):
        path=copied/name;old=path.read_bytes();path.write_bytes(old+b'\n')
        try:delta.verify_native_sources(copied)
        except (AssertionError,ValueError):count+=1
        else:raise AssertionError('Unreviewed source mutation accepted: '+name)
        finally:path.write_bytes(old)
    return count

def repository_content_controls(source):
    import base64,hashlib
    from unittest.mock import patch
    path=source.parent/'publication/publish1996.py'
    spec=importlib.util.spec_from_file_location('release_content1996',path);pub=importlib.util.module_from_spec(spec);spec.loader.exec_module(pub)
    ref='a'*40;name='releases/ULike_ACTIVE.json';endpoint=f'repos/{pub.REPO}/contents/{name}?ref={ref}';count=0
    def row(raw,encoding):
        return dict(type='file',path=name,size=len(raw),sha=hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest(),
            encoding=encoding,content=base64.b64encode(raw).decode() if encoding=='base64' else '')
    small=b'{"test":"small"}\n';large=b'x'*(1024*1024+7)
    with patch.object(pub,'api',return_value=row(small,'base64')) as api,patch.object(pub.subprocess,'check_output') as rawget:
        assert pub.content(name,ref)==small and api.call_args.args==(endpoint,) and rawget.call_count==0;count+=1
    with patch.object(pub,'api',return_value=row(large,'none')) as api,patch.object(pub.subprocess,'check_output',return_value=large) as rawget:
        assert pub.content(name,ref)==large and api.call_args.args==(endpoint,);count+=1
        assert rawget.call_args.args==(['gh','api',endpoint,'-H','Accept: application/vnd.github.raw+json'],);count+=1
    for meta,raw,reference in [
        ({**row(large,'none'),'sha':'0'*40},large,ref),
        ({**row(large,'none'),'size':len(large)+1},large,ref),
        (row(large,'none'),large[:-1],ref),
        ({**row(large,'none'),'path':'another.json'},large,ref),
        ({**row(large,'none'),'type':'dir'},large,ref),
        (row(small,'none'),small,ref),
        (row(large,'none'),large,'main'),
        ({**row(small,'base64'),'sha':'0'*40},small,ref),
        ({**row(small,'base64'),'size':0},small,ref),
        ({**row(large,'none'),'encoding':'unknown'},large,ref)]:
        with patch.object(pub,'api',return_value=meta),patch.object(pub.subprocess,'check_output',return_value=raw):
            try:pub.content(name,reference)
            except (AssertionError,RuntimeError,ValueError):count+=1
            else:raise AssertionError('Changed or unbound repository content accepted')
    return count

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source))
    spec=importlib.util.spec_from_file_location('release_validation1996',source/'validate1996.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
    assertions=0
    def rejected(call):
        nonlocal assertions
        try:call()
        except (AssertionError,RuntimeError,ValueError,zipfile.BadZipFile):assertions+=1
        else:raise AssertionError('Malformed release evidence accepted')
    for name in ('../secret.apk','/root/file','a/../secret','a\\b','a//b','a/',''):
        rejected(lambda name=name:gate.checked_name(name))
    gate.checked_name('src/tests1996/corpus.java');assertions+=1
    children={name:{'status':'passed','assertions':2,'physical_android_tested':False} for name in gate.REQUIRED_HOST_GROUPS}
    children['finish_recovery95'].update(resident_policy_trace_verified=True)
    children['release_gate'].update(native_reviewed_shader_inverse_verified=True,native_delta_negative_assertions=1,large_content_exact_commit_verified=True)
    children['gpu_math'].update(shader96=True,only_guarded_partial_predicates_changed=True,published95_records=1016,
        published95_reads=18240,published95_argb_differences=0,published95_confidence_differences=0,legacy_preprocessor_programs=60,
        focused_gles={'actual_gles_execution':True,'cutoff_proof':{'unsafe_rejections':0,'final_contribution_differences':0}})
    children['cpu_copy96'].update(**{k:True for k in ('foreground_cpu_copy_disabled','existing_certificates_and_original_fallback_verified',
        'explicit_owned_verifiers_exact_speed_cancel_memory_verified','reused_result_alias_regression_verified',
        'actual_colour_jni_outputs_compared','gpu_queue_independent')})
    children['model_parallel96'].update(**{k:True for k in ('whole_model_bitwise_equal', 'actual_source_regions_overlapped', 'resident_runtime_upload_bitwise_equal', 'model_ticket_joined_before_handoff', 'callback_once_and_fallback_verified', 'existing_region_gpu_route_preserved', 'invalid_completed_runtime_rejected')})
    children['front_timing96'].update(controlled_native_transport=True,actual_jvm_loading=True)
    good={'status':'passed','assertions':2*len(children),'physical_android_tested':False,'tests':children}
    gate.positive_host(good);assertions+=1
    for update in ({'status':'pending'},{'assertions':True},{'assertions':0},{'assertions':3},{'physical_android_tested':True},
            {'device_speedup_verified':True},{'tests':{**children,'release_gate':{**children['release_gate'],'native_reviewed_shader_inverse_verified':False}}},{'tests':{**children,'release_gate':{**children['release_gate'],'native_delta_negative_assertions':0}}},{'tests':{**children,'finish_recovery95':{**children['finish_recovery95'],'resident_policy_trace_verified':False}}},{'tests':{}},{'tests':{**children,'chain':{'status':'failed','assertions':2}}},
            {'assertions':2*(len(children)-1),'tests':{k:v for k,v in children.items() if k!='finish_recovery95'}},
            {'assertions':2*(len(children)+1),'tests':{**children,'stale':{'status':'passed','assertions':2}}}):
        rejected(lambda update=update:gate.positive_host({**good,**update}))
    for group,key in [('gpu_math','shader96'),('gpu_math','only_guarded_partial_predicates_changed'),
            ('gpu_math','published95_records'),('gpu_math','published95_reads'),('gpu_math','legacy_preprocessor_programs')]+[
            ('cpu_copy96',key) for key in ('foreground_cpu_copy_disabled','existing_certificates_and_original_fallback_verified',
            'explicit_owned_verifiers_exact_speed_cancel_memory_verified','reused_result_alias_regression_verified',
            'actual_colour_jni_outputs_compared','gpu_queue_independent')]:
        changed={**children,group:{**children[group],key:None}}
        rejected(lambda changed=changed:gate.positive_host({**good,'tests':changed}))
    for group,key in [('model_parallel96',k) for k in ('whole_model_bitwise_equal', 'actual_source_regions_overlapped', 'resident_runtime_upload_bitwise_equal', 'model_ticket_joined_before_handoff', 'callback_once_and_fallback_verified', 'existing_region_gpu_route_preserved', 'invalid_completed_runtime_rejected')]+[('front_timing96','controlled_native_transport'),('front_timing96','actual_jvm_loading')]:
        changed={**children,group:{**children[group],key:False}}
        rejected(lambda changed=changed:gate.positive_host({**good,'tests':changed}))
    rejected(lambda:gate.positive_host({**good,'tests':{**children,'release_gate':{**children['release_gate'],'large_content_exact_commit_verified':False}}}))
    bad=work/'unsafe.zip'
    with zipfile.ZipFile(bad,'w') as archive:archive.writestr('../secret.apk',b'private')
    rejected(lambda:gate.checked_archive(bad))
    symlink=work/'symlink.zip'
    with zipfile.ZipFile(symlink,'w') as archive:
        item=zipfile.ZipInfo('safe');item.create_system=3;item.external_attr=(0o120777<<16);archive.writestr(item,b'../private')
    rejected(lambda:gate.checked_archive(symlink))
    goodzip=work/'good.zip'
    with zipfile.ZipFile(goodzip,'w') as archive:archive.writestr('src/code.java',b'class A {}')
    assert gate.checked_archive(goodzip)=={'src/code.java':b'class A {}'};assertions+=1
    with zipfile.ZipFile(source.parent/'dist/ULike_HQ_Texture_Online_v1.9.96.mpp') as artifact:
        native_assertions=native_negative_controls(source,artifact.read('ulike1960/runtime/libulike_gpu1960.so'),work)
    content_assertions=repository_content_controls(source)
    assertions+=native_assertions+content_assertions
    result={'status':'passed','assertions':assertions,'native_delta_negative_assertions':native_assertions,'native_reviewed_shader_inverse_verified':True,'large_content_exact_commit_verified':True,'repository_content_assertions':content_assertions,'physical_android_tested':False,'device_speedup_verified':False,
        'release_gate_negative_inputs_verified':True,'actual_release_validation_in_this_unit_test':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
