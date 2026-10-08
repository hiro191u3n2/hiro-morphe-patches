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
    words=struct.unpack('<'+'H'*(len(f[1])//2),f[1])
    # The production order is Context, phase, first, last. Pin all four
    # callsites, including the constants assigned to the phase register.
    stage_sites=((133,0x109d),(137,0xac0d),(164,0x1043),(170,0x1203))
    for at,args in stage_sites:
        assert words[at:at+3]==(0x4071,stage_id,args),(at,words[at:at+3])
    assert words[113]==0x8901 and words[53]==0x0812  # v9 = v8 = phase 0
    assert words[136]==0x1012 and words[163]==0x2412 and words[167]==0x3012
    # Sharpening adds four rows to both sides of the vertical denoise/packing
    # interval before adding another three rows for horizontal support.
    assert words[22]==0x4012  # v0 = 4
    assert words[69:76]==(0x0f3d,4,0x0101,0x0228,0x8101,0x0191,0x010c)
    assert words[82:87]==(0x0f3d,3,0x0228,0x8001,0x60b0)
    assert words[103]==0x5a01 and words[110]==0x7c01  # clipped last/first
    assert words[121:123]==(0x07d8,0xfd0c)  # first - 3
    assert words[127:129]==(0x05d8,0x030a)  # last + 3
    run_code=codes[(run,'run')][1]
    run_words=struct.unpack('<'+'H'*(len(run_code)//2),run_code)
    assert codes[(run,'run')][0][:2]==(21,4)
    assert run_words[2:5]==(0x0102,18,0x0139)  # first incoming int is phase
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
    assert match and int(match.group(1))>=640,completed.stdout+completed.stderr
    count=int(match.group(1))
    return {'status':'passed','assertions':count+3,'production_filter_sha256':hashlib.sha256(f[1]).hexdigest(),
        'production_stage_sha256':hashlib.sha256(s[1]).hexdigest(),
        'production_context_run_sha256':hashlib.sha256(run_code).hexdigest(),
        'pinned_filter_stage_invocations':4,'production_context_bilateral_calls':2,
        'production_stage_calls_context_run_synchronously':True,
        'stage_parameter_order_verified':True,
        'production_sharpening_halo_rows':4,
        'software_gpu_two_pass_exact_cases':count,'software_gpu':completed.stdout.strip(),
        'partial_stripe_halo_supported':True,'device_tested':False}

if __name__=='__main__':
    if len(sys.argv)!=2:raise SystemExit('usage: test_h8_integration.py BASELINE.mpp')
    print(json.dumps(test(Path(__file__).parent.parent,Path(sys.argv[1])),indent=2))
