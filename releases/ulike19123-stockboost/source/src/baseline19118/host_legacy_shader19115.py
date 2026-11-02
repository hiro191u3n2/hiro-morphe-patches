#!/usr/bin/env python3
"""Preprocess all 37 published Strong programs and compare complete tokens.

This checks preservation of arithmetic/topology, not device timing.
"""
from pathlib import Path
import argparse, hashlib, json, re, subprocess

ROOT=Path(__file__).resolve().parent
BASELINE_SHA256='fd2c6845d4df9cfef42bd20d563ed37362bb43bf6138f84674d7a6d1c00cd051'

def definitions(program):
    trace=program==60
    if program in (0,9,18):mode=-1;variant=program//9;tiled=False;ieee=0;pow2=0
    elif 27<=program<39:mode=(program-27)%4;variant=(program-27)//4;tiled=True;ieee=0;pow2=0
    elif program<42:mode=3;variant=program-39;tiled=False;ieee=1;pow2=0
    elif program<45:mode=3;variant=program-42;tiled=True;ieee=1;pow2=0
    elif program<48:mode=3;variant=program-45;tiled=False;ieee=1;pow2=1
    elif program<51:mode=3;variant=program-48;tiled=True;ieee=1;pow2=1
    elif program<60:mode=(program-51)%3;variant=(program-51)//3;tiled=True;ieee=1;pow2=1
    else:mode=3;variant=0;tiled=False;ieee=1;pow2=0
    return dict(GX_STRONG_MODE=mode,GX_LOCAL_SIZE=(64,32,128)[variant],
                GX_TILE_WIDTH=8 if tiled else 0,GX_IEEE_DIV73=ieee,
                GX_POW2_EXACT78=pow2,GX_TRACE19106=int(trace))

TOKEN=re.compile(r'[A-Za-z_]\w*|0[xX][0-9A-Fa-f]+[uU]?|(?:\d+\.\d*|\.\d+|\d+)(?:[eE][+-]?\d+)?[fFuU]?|<<=|>>=|\+\+|--|&&|\|\||==|!=|<=|>=|<<|>>|\+=|-=|\*=|/=|%=|&=|\|=|\^=|[^\s]')
def preprocess(text,defines):
    body=re.sub(r'^#(?:version|extension)[^\n]*\n','',text,flags=re.M)
    cmd=['cc','-E','-P','-x','c',*[f'-D{k}={v}' for k,v in defines.items()],'-']
    result=subprocess.run(cmd,input=body,text=True,capture_output=True,check=True).stdout
    return TOKEN.findall(result)

def test(source,work=None,jdk=None,ndk=None):
    source=Path(source).resolve();old=source/'tests19115/provenance/reference114/strong1960.comp';new=source/'native1960/strong1960.comp'
    assert hashlib.sha256(old.read_bytes()).hexdigest()==BASELINE_SHA256,'frozen114 shader changed'
    baseline=old.read_text();candidate=new.read_text();programs=[0,9,18,*range(27,61)]
    for p in programs:
        assert preprocess(baseline,definitions(p))==preprocess(candidate,definitions(p)),f'legacy program {p} changed'
    result={'status':'passed','assertions':len(programs),'program_ids':programs,
            'baseline_sha256':hashlib.sha256(old.read_bytes()).hexdigest(),
            'candidate_sha256':hashlib.sha256(new.read_bytes()).hexdigest(),
            'legacy_tokens_identical':True,'physical_android_tested':False,'device_speedup_verified':False,
            'speed_equivalence_claimed':False}
    if work:
        work=Path(work);work.mkdir(parents=True,exist_ok=True)
        (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT));p.add_argument('--work');a=p.parse_args();print(json.dumps(test(a.source,a.work)))
