#!/usr/bin/env python3
"""Exact complete-source inverse for the reviewed .115 delta over published114.

This is a provenance guard, not numerical equivalence or device-speed evidence.
A mutated current file cannot be silently normalized into its old counterpart.
"""
from pathlib import Path
import argparse, hashlib, json, re
import source_inverse19117
import source_inverse19118
ROOT=Path(__file__).resolve().parent
MANIFEST='source_inverse19115_manifest.json'
MANIFEST_SHA256='535bcfe44ac8420d2f61f5d558003ad949af6a809e9370343706931a80b1eef4'
OVERLAY116='source_inverse19116_overlay.json'
OVERLAY116_SHA256='c39eb936507c54d3fb27346a7a075bd08aaeaa9bf3ac0450396b06dc0d83b814'
OVERLAY116_FILES=('GpuQualification1961.java','GpuResident1976.java','ProcessingTiming1947.java','ResidentTiming19111.java','VerificationJournal19101.java')
REFERENCE='tests19115/provenance/reference114'
REVIEWED=('GpuNoise1960.java','GpuStrong1960.java','GpuStrongDefault1990.java','GpuResident1976.java','ProcessingTiming1947.java','GpuQualification1961.java','StrongNoise1958.java','ResidentTiming19111.java','VerificationJournal19101.java','native1955/single_noise1955.c','native1958/neon1959.h','native1960/engine1960.c','native1960/strong1960.comp','native1960/compute_dependencies1990.json')
def sha(b):return hashlib.sha256(b).hexdigest()
def read_manifest(source=ROOT):
    raw=(Path(source)/MANIFEST).read_bytes()
    assert sha(raw)==MANIFEST_SHA256,'Pinned complete .115 inverse manifest differs'
    d=json.loads(raw);assert d['schema']=='ulike19115-complete-source-inverse-v1'
    assert tuple(d['reviewed_files'])==REVIEWED and set(d['files'])==set(REVIEWED)
    return d

def read_overlay116(source=ROOT, previous=None, raw=None):
    previous=previous or read_manifest(source)
    raw=(Path(source)/OVERLAY116).read_bytes() if raw is None else raw
    assert type(raw) is bytes and sha(raw)==OVERLAY116_SHA256,'Pinned .116 overlay differs'
    overlay=json.loads(raw)
    assert overlay['schema']=='ulike19116-reviewed-overlay-to-115-v1' and overlay['previous_release']=='ulike-v1.9.115'
    assert tuple(overlay['reviewed_files'])==OVERLAY116_FILES and set(overlay['files'])==set(OVERLAY116_FILES)
    for name,entry in overlay['files'].items():
        assert set(entry)=={'previous_sha256','current_sha256','changes'}
        assert entry['previous_sha256']==previous['files'][name]['current_sha256']
        assert type(entry['changes']) is list and entry['changes']
        assert all(set(change)=={'current','previous'} and type(change['current']) is str and
                   type(change['previous']) is str and change['current'] for change in entry['changes'])
    return overlay

def undo_overlay116(name,current,entry):
    assert type(current) is bytes and sha(current)==entry['current_sha256'],'Source differs outside complete reviewed .116 overlay: '+name
    for change in entry['changes']:
        now=change['current'].encode();old=change['previous'].encode()
        assert current.count(now)==1,'Ambiguous .116 reverse hunk: '+name
        current=current.replace(now,old,1)
    assert sha(current)==entry['previous_sha256'],'Incomplete .116 reverse hunk: '+name
    return current

def restore(source,name,raw=None):
    source=Path(source);d=read_manifest(source)
    assert name in d['files'],'Unreviewed .115 inverse request: '+name
    p=d['files'][name];reference=(source/REFERENCE/name).read_bytes()
    assert sha(reference)==p['baseline_sha256'],'Frozen published114 source differs: '+name
    current=(source/name).read_bytes() if raw is None else raw
    assert type(current) is bytes,'Source must be raw bytes: '+name
    if sha(current) not in (p['current_sha256'],p['baseline_sha256']):
        if name in source_inverse19118.REVIEWED:
            current=source_inverse19118.restore_to117(source,name,current)
        if name in source_inverse19117.REVIEWED:
            current=source_inverse19117.restore_to116(source,name,current)
        if sha(current) not in (p['current_sha256'],p['baseline_sha256']):
            assert name in OVERLAY116_FILES,'Source differs outside complete reviewed .115 diff: '+name
            overlay=read_overlay116(source,d)
            current=undo_overlay116(name,current,overlay['files'][name])
            assert sha(current)==p['current_sha256'],'Reviewed .116 overlay does not restore .115: '+name
    return reference

def restore_shader19115(text):
    return restore(ROOT,'native1960/strong1960.comp',text.encode()).decode()

def test(source=ROOT,work=None,jdk=None,ndk=None):
    source=Path(source);d=read_manifest(source);overlay=read_overlay116(source,d);count=0
    for name in REVIEWED:
        raw=(source/name).read_bytes();old=(source/REFERENCE/name).read_bytes()
        assert restore(source,name,raw)==old;count+=1
        try:restore(source,name,raw+b'\n')
        except AssertionError:count+=1
        else:raise AssertionError('Altered complete source accepted: '+name)
    overlay_rejected=0
    for name in OVERLAY116_FILES:
        raw=(source/name).read_bytes();entry=overlay['files'][name]
        if name in source_inverse19117.REVIEWED:
            raw=source_inverse19117.restore_to116(source,name,raw)
        assert sha(undo_overlay116(name,raw,entry))==d['files'][name]['current_sha256'];count+=1
        for change in entry['changes']:
            now=change['current'].encode();altered=raw.replace(now,bytes((now[0]^1,))+now[1:],1)
            try:restore(source,name,altered)
            except AssertionError:count+=1;overlay_rejected+=1
            else:raise AssertionError('Partial .116 overlay accepted: '+name)
    try:read_overlay116(source,d,(source/OVERLAY116).read_bytes()+b'\n')
    except AssertionError:count+=1;overlay_rejected+=1
    else:raise AssertionError('Altered .116 overlay accepted')
    return {'schema':'ulike19115-source-inverse-proof-v1','status':'passed','assertions':count,'manifest_sha256':MANIFEST_SHA256,'overlay116_sha256':OVERLAY116_SHA256,'complete_source_inverses_verified':True,'mutations_rejected':len(REVIEWED),'overlay116_mutations_rejected':overlay_rejected,'physical_android_tested':False,'device_speedup_verified':False}

def freeze(source=ROOT):
    source=Path(source);files={}
    for name in REVIEWED:
        old=(source/REFERENCE/name).read_bytes();new=(source/name).read_bytes()
        assert old!=new,'Reviewed .115 delta missing: '+name
        files[name]={'baseline_sha256':sha(old),'current_sha256':sha(new)}
    value={'schema':'ulike19115-complete-source-inverse-v1','baseline_release':'ulike-v1.9.114','reviewed_files':list(REVIEWED),'files':files}
    raw=(json.dumps(value,sort_keys=True,indent=2)+'\n').encode();(source/MANIFEST).write_bytes(raw)
    path=source/'source_inverse19115.py';text=path.read_text()
    text,n=re.subn(r"(?m)^MANIFEST_SHA256='[^']*'$","MANIFEST_SHA256='"+sha(raw)+"'",text);assert n==1;path.write_text(text)
    return sha(raw)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--freeze',action='store_true');a=p.parse_args()
    print(freeze() if a.freeze else json.dumps(test()))
