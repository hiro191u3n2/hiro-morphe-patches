#!/usr/bin/env python3
"""Negative-input tests for publication gate parsing; no release evidence mocked."""
from pathlib import Path
import argparse, importlib.util, json, sys, zipfile

def native_negative_controls(source,raw):
    import native_identity1995 as identity
    count=0;fingerprint=identity.current_fingerprint(source)
    report=identity.verify(raw,fingerprint);count+=1
    identity.verify_native_sources(source);count+=1
    assert report['whole_file_inverse_sha256']==identity.BASE_SHA256
    assert report['all_other_native_bytes_identical'] and report['machine_code_and_shader_literals_identical'];count+=1
    sections=identity.sections(raw)
    ranges={r['section']:r for r in report['allowed_metadata_ranges']}
    fp=ranges['.rodata']['offset'];build_id=ranges['.note.gnu.build-id']['offset']
    def rejected(value,expected=fingerprint):
        nonlocal count
        try:identity.verify(value,expected)
        except (AssertionError,ValueError):count+=1
        else:raise AssertionError('Non-metadata GPU native mutation was accepted')
    positions=[0,18,40,sections['.text'][4],sections['.text'][4]+100,
        sections['.text'][4]+sections['.text'][5]-1,sections['.plt'][4],
        sections['.rodata'][4],fp-1,fp,fp+63,fp+64,
        sections['.dynsym'][4],sections['.dynamic'][4],sections['.data'][4],
        sections['.comment'][4],sections['.note.gnu.build-id'][4],build_id-4,
        sections['.shstrtab'][4]+1]
    for position in positions:
        changed=bytearray(raw);changed[position]^=1;rejected(bytes(changed))
    rejected(raw+b'\0');rejected(raw[:-1]);rejected(raw,'0'*64);rejected(raw,'bad')
    # A second matching fingerprint is not accepted, even outside .rodata.
    duplicate=bytearray(raw);location=sections['.text'][4]+128
    duplicate[location:location+64]=fingerprint.encode();rejected(bytes(duplicate))
    # Build identity itself is intentionally metadata, never executable code.
    metadata=bytearray(raw);metadata[build_id]^=1
    allowed=identity.verify(bytes(metadata),fingerprint)
    assert allowed['all_other_native_bytes_identical'];count+=1
    return count

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source))
    spec=importlib.util.spec_from_file_location('release_validation1995',source/'validate1995.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
    assertions=0
    def rejected(call):
        nonlocal assertions
        try:call()
        except (AssertionError,RuntimeError,ValueError,zipfile.BadZipFile):assertions+=1
        else:raise AssertionError('Malformed release evidence accepted')
    for name in ('../secret.apk','/root/file','a/../secret','a\\b','a//b','a/',''):
        rejected(lambda name=name:gate.checked_name(name))
    gate.checked_name('src/tests1995/corpus.java');assertions+=1
    children={name:{'status':'passed','assertions':2,'physical_android_tested':False} for name in gate.REQUIRED_HOST_GROUPS}
    children['finish_recovery95'].update(resident_policy_trace_verified=True)
    children['release_gate'].update(native_identity_only_inverse_verified=True,native_identity_negative_assertions=1)
    good={'status':'passed','assertions':2*len(children),'physical_android_tested':False,'tests':children}
    gate.positive_host(good);assertions+=1
    for update in ({'status':'pending'},{'assertions':True},{'assertions':0},{'assertions':3},{'physical_android_tested':True},
            {'device_speedup_verified':True},{'tests':{**children,'release_gate':{**children['release_gate'],'native_identity_only_inverse_verified':False}}},{'tests':{**children,'release_gate':{**children['release_gate'],'native_identity_negative_assertions':0}}},{'tests':{**children,'finish_recovery95':{**children['finish_recovery95'],'resident_policy_trace_verified':False}}},{'tests':{}},{'tests':{**children,'chain':{'status':'failed','assertions':2}}},
            {'assertions':2*(len(children)-1),'tests':{k:v for k,v in children.items() if k!='finish_recovery95'}},
            {'assertions':2*(len(children)+1),'tests':{**children,'stale':{'status':'passed','assertions':2}}}):
        rejected(lambda update=update:gate.positive_host({**good,**update}))
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
    with zipfile.ZipFile(source.parent/'dist/ULike_HQ_Texture_Online_v1.9.95.mpp') as artifact:
        native_assertions=native_negative_controls(source,artifact.read('ulike1960/runtime/libulike_gpu1960.so'))
    assertions+=native_assertions
    result={'status':'passed','assertions':assertions,'native_identity_negative_assertions':native_assertions,'native_identity_only_inverse_verified':True,'physical_android_tested':False,'device_speedup_verified':False,
        'release_gate_negative_inputs_verified':True,'actual_release_validation_in_this_unit_test':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
