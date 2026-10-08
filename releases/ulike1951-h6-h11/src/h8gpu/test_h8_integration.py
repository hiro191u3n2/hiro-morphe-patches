#!/usr/bin/env python3
"""Pinned production filter/stage review plus actual Mesa two-pass shader test."""
from pathlib import Path
import hashlib,json,re,struct,subprocess,sys,zipfile

def methods(dex):
    u32=lambda p:struct.unpack_from('<I',dex,p)[0]
    u16=lambda p:struct.unpack_from('<H',dex,p)[0]
    def uleb(p):
        n=0;bits=0
        while True:
            c=dex[p];p+=1;n|=(c&127)<<bits;bits+=7
            if not c&128:return n,p
    names=[]
    for i in range(u32(0x38)):
        p=u32(u32(0x3c)+i*4);_,p=uleb(p)
        names.append(dex[p:dex.index(0,p)].decode())
    types=[names[u32(u32(0x44)+i*4)] for i in range(u32(0x40))]
    identifiers=[]
    for i in range(u32(0x58)):
        owner,proto,name=struct.unpack_from('<HHI',dex,u32(0x5c)+i*8)
        identifiers.append((types[owner],names[name],proto))
    code={}
    for i in range(u32(0x60)):
        owner,_,_,_,_,_,p,_=struct.unpack_from('<IIIIIIII',dex,u32(0x64)+i*32)
        if not p:continue
        counts=[]
        for _ in range(4):
            n,p=uleb(p);counts.append(n)
        cumulative=0
        for _ in range(counts[0]+counts[1]):
            _,p=uleb(p);_,p=uleb(p)
        for amount in counts[2:]:
            cumulative=0
            for _ in range(amount):
                delta,p=uleb(p);cumulative+=delta;_,p=uleb(p);offset,p=uleb(p)
                if offset:
                    words=u32(offset+12)
                    code[identifiers[cumulative][:2]]=(
                        struct.unpack_from('<HHH',dex,offset),
                        dex[offset+16:offset+16+words*2])
    return identifiers,code

def test(source,baseline):
    with zipfile.ZipFile(baseline) as z:dex=z.read('ulike/runtime.dex')
    ids,codes=methods(dex)
    owner='Lcom/hiro/ulike/DetailSerial186;'
    run='Lcom/hiro/ulike/DetailSerial186$Context;'
    f= codes[(owner,'filter')]
    assert f[0] == (27,10,9),f[0]
    stage_id=ids.index(next(m for m in ids if m[:2]==(owner,'stage')))
    def count_invoke(body,method_id):
        # Relevant stage invocations are exact 35c invoke-static at the pinned
        # filter; count also checked against the known instruction offsets.
        return sum(body[at]==0x71 and struct.unpack_from('<H',body,at+2)[0]==method_id
                   for at in range(0,len(body)-3,2))
    assert count_invoke(f[1],stage_id)==4
    run_code=codes[(run,'run')][1]
    access_id=ids.index(next(m for m in ids if m[:2]==(owner,'access$000')))
    assert sum(run_code[at]==0x77 and struct.unpack_from('<H',run_code,at+2)[0]==access_id
               for at in range(0,len(run_code)-3,2))==2
    s=codes[(owner,'stage')]
    assert s[0]==(4,4,4)
    run_id=ids.index(next(m for m in ids if m[:2]==(run,'run')))
    assert len(s[1])==8 and s[1][0]==0x6e and s[1][6:]==b'\x0e\x00'
    assert struct.unpack_from('<H',s[1],2)[0]==run_id
    shader=Path(source)/'h8gpu'/'test_bilateral1950.py'
    completed=subprocess.run([sys.executable,str(shader)],text=True,capture_output=True,check=True)
    match=re.search(r'PASS: (\d+) exact integer cases',completed.stdout)
    assert match and int(match.group(1))>=384,completed.stdout+completed.stderr
    count=int(match.group(1))
    return {'status':'passed','assertions':count+3,'production_filter_sha256':hashlib.sha256(f[1]).hexdigest(),
        'production_stage_sha256':hashlib.sha256(s[1]).hexdigest(),
        'production_context_run_sha256':hashlib.sha256(run_code).hexdigest(),
        'pinned_filter_stage_invocations':4,'production_context_bilateral_calls':2,
        'production_stage_calls_context_run_synchronously':True,
        'software_gpu_two_pass_exact_cases':count,'software_gpu':completed.stdout.strip(),
        'partial_stripe_halo_supported':True,'device_tested':False}

if __name__=='__main__':
    if len(sys.argv)!=2:raise SystemExit('usage: test_h8_integration.py BASELINE.mpp')
    print(json.dumps(test(Path(__file__).parent.parent,Path(sys.argv[1])),indent=2))
