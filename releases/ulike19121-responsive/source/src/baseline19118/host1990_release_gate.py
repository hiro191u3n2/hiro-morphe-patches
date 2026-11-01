#!/usr/bin/env python3
"""Negative-input tests for publication gate parsing; no release evidence mocked."""
from pathlib import Path
import argparse, importlib.util, json, sys, zipfile

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);sys.path.insert(0,str(source))
    spec=importlib.util.spec_from_file_location('release_validation1990',source/'validate1990.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
    assertions=0
    def rejected(call):
        nonlocal assertions
        try:call()
        except (AssertionError,RuntimeError,ValueError,zipfile.BadZipFile):assertions+=1
        else:raise AssertionError('Malformed release evidence accepted')
    for name in ('../secret.apk','/root/file','a/../secret','a\\b','a//b','a/',''):
        rejected(lambda name=name:gate.checked_name(name))
    gate.checked_name('src/tests1990/corpus.java');assertions+=1
    good={'status':'passed','assertions':2,'physical_android_tested':False,'tests':{'a':{'status':'passed','assertions':2,'physical_android_tested':False}}}
    gate.positive_host(good);assertions+=1
    for update in ({'status':'pending'},{'assertions':True},{'assertions':0},{'assertions':3},{'physical_android_tested':True},
            {'device_speedup_verified':True},{'tests':{}},{'tests':{'a':{'status':'failed','assertions':2}}}):
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
    result={'status':'passed','assertions':assertions,'physical_android_tested':False,'device_speedup_verified':False,
        'release_gate_negative_inputs_verified':True,'actual_release_validation_in_this_unit_test':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
