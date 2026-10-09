#!/usr/bin/env python3
"""Real unmodified GLES3.1 shader soft binary64 vs host binary64, bit for bit.

The library is inserted exactly as production's builder; no dialect changes,
native FP64 extension, CPU-derived shader outputs or reference-assisted route.
"""
from pathlib import Path
import ctypes as C, hashlib, json, math, random, struct, time, argparse
ROOT=Path(__file__).resolve().parents[1]

def bits(x):return struct.unpack('<Q',struct.pack('<d',x))[0]
def value(x):return struct.unpack('<d',struct.pack('<Q',x))[0]
def fbits(x):
    try:return struct.unpack('<I',struct.pack('<f',x))[0]
    except OverflowError:return 0x7f800000 if x>0 else 0xff800000
def fvalue(x):return struct.unpack('<f',struct.pack('<I',x))[0]
def words(x):return [x&0xffffffff,x>>32]
def expected(op,a,b):
    av,bv=value(a),value(b)
    if op==0:return bits(av+bv)
    if op==1:return bits(av-bv)
    if op==2:return bits(av*bv)
    if op==3:
        if math.isnan(av) or math.isnan(bv) or (av==0 and bv==0) or (math.isinf(av) and math.isinf(bv)):return bits(math.nan)
        if bv==0:return bits(math.copysign(math.inf,av)*math.copysign(1.,bv))
        return bits(av/bv)
    if op==4:return bits(math.sqrt(av)) if av>=0 else bits(math.nan)
    if op in [5,6]:
        if not math.isfinite(av):return a
        if av==0:return a
        v=math.floor(av) if op==5 else math.trunc(av)
        return bits(float(v)) if v else bits(math.copysign(0.,av))
    if op==7:return bits(fvalue(a&0xffffffff))
    if op==8:return fbits(av)
    if op==9:return bits(float(C.c_int32(a&0xffffffff).value))
    if op==10:return max(-2147483648,min(2147483647,math.trunc(av)))&0xffffffff
    if op==11:return int(av==bv)|(int(av<bv)<<1)|(int(av<=bv)<<2)|(int(av>bv)<<3)|(int(av>=bv)<<4)
    if op in [12,13]:
        if math.isnan(av) or math.isnan(bv):return bits(math.nan)
        if av==0 and bv==0:return (a|b) if op==12 else (a&b)
        if op==12:return a if av<bv else b
        return b if av<bv else a
    if op==14:return bits(float(a&0xffffffff))
    if op==15:return a&0x7fffffffffffffff
    if op==16:return a^0x8000000000000000
    if op==17:return int(math.isfinite(av))|(int(av==0)<<1)
    raise ValueError(op)

def test(work,samples=4000):
    work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    library=(ROOT/'native1960/soft64_1965.glsl').read_text()
    for name,source in [('soft64_arithmetic1965',ROOT/'tests1965/soft64_arithmetic1965.comp'),('soft64_probe1965',ROOT/'native1960/soft64_probe1965.comp')]:
        s=source.read_text();assert s.count('/* SOFT64_1965_LIBRARY */')==1
        (work/(name+'.comp')).write_text(s.replace('/* SOFT64_1965_LIBRARY */',library))
    harness=(ROOT/'native1949/host_gpu1949.py').read_text().replace("['fusion_sums1949','pack_rgb1949','residual1949']","['soft64_arithmetic1965','soft64_probe1965']")
    ns={'__name__':'soft64_test'};exec(compile(harness,'host_gpu1949.py','exec'),ns)
    started=time.monotonic();gpu=ns['SoftwareGpu'](work)
    gpu.gl('glUniform1iv',None,[C.c_int,C.c_int,C.POINTER(C.c_int)])
    cases=[];r=random.Random(19650064)
    def add(op,a,b=0):cases.append((op,a,b,expected(op,a,b)))
    boundaries=[0,0x8000000000000000,1,2,0x000fffffffffffff,0x0010000000000000,
                0x0010000000000001,0x3fefffffffffffff,0x3ff0000000000000,0x3ff0000000000001,
                0x3ff0000000000002,0x3ca0000000000000,0x7fefffffffffffff,0x7ff0000000000000,
                0xfff0000000000000,0x7ff8000000000000,0x8000000000000001,
                bits(-1.25),bits(-.1),bits(2.),bits(3.)]
    for op in [0,1,2,3,11,12,13]:
        for a in boundaries:
            for b in boundaries:add(op,a,b)
        for _ in range(samples):
            a=r.getrandbits(64);b=r.getrandbits(64)
            if ((a>>52)&2047)==2047:a&=~(2047<<52)
            if ((b>>52)&2047)==2047:b&=~(2047<<52)
            add(op,a,b)
    for op in [4,5,6,8,15,16,17]:
        for a in boundaries:add(op,a)
        for _ in range(samples):
            a=r.getrandbits(64)
            if ((a>>52)&2047)==2047:a&=~(2047<<52)
            if op==4:a&=0x7fffffffffffffff
            add(op,a)
    for _ in range(samples):
        raw=r.getrandbits(32)
        if raw&0x7f800000!=0x7f800000:add(7,raw)
        add(9,raw)
        add(14,raw)
        add(10,bits(r.uniform(-2147483648,2147483647)))
    for x in [-2147483648,-1,0,1,2147483647]:add(9,x&0xffffffff);add(10,bits(float(x)))
    for x in [0,1,0x7fffffff,0x80000000,0xffffffff]:add(14,x)
    for x in [0,0x80000000,1,0x80000001,0x007fffff,0x00800000,0x3f800000,
              0x3f800001,0x3f800002,0x7f7fffff,0x7f800000,0xff800000,0x7fc00000]:add(7,x)
    errors=[];tested=0;counts={};f=gpu.functions
    try:
        for chunk_start in range(0,len(cases),2048):
            chunk=cases[chunk_start:chunk_start+2048];inputs=[]
            for op,a,b,wanted in chunk:inputs.extend([op,*words(a),*words(b)])
            gpu.upload(0,(C.c_uint32*len(inputs))(*inputs));gpu.upload(1,None,len(chunk)*8)
            f['glUseProgram'](gpu.programs[0]);loc=f['glGetUniformLocation'](gpu.programs[0],b'u[0]')
            u=[0]*32;u[2]=len(chunk);f['glUniform1iv'](loc,32,(C.c_int32*32)(*u))
            f['glDispatchCompute']((len(chunk)+63)//64,1,1);gpu.fence();got=gpu.read(1,len(chunk)*2)
            for i,(op,a,b,wanted) in enumerate(chunk):
                actual=(got[2*i]&0xffffffff)|((got[2*i+1]&0xffffffff)<<32)
                counts[op]=counts.get(op,0)+1
                nan=op not in [8,10,11,17] and math.isnan(value(wanted))
                equal=math.isnan(fvalue(actual&0xffffffff)) if op==8 and math.isnan(fvalue(wanted)) else math.isnan(value(actual)) if nan else actual==wanted
                if not equal:errors.append({'op':op,'a':hex(a),'b':hex(b),'expected':hex(wanted),'actual':hex(actual)})
            tested+=len(chunk)
        f['glUseProgram'](gpu.programs[1]);u=[0]*32;u[1]=1
        gpu.upload(0,(C.c_uint32*1)(0));gpu.upload(1,(C.c_uint32*16)(*([0]*16)))
        loc=f['glGetUniformLocation'](gpu.programs[1],b'u[0]');f['glUniform1iv'](loc,32,(C.c_int32*32)(*u));f['glDispatchCompute'](1,1,1);gpu.fence()
        probe=[x&0xffffffff for x in gpu.read(1,16)]
        wanted_probe=[0,0x3ff00000,2,0x3ff00000,0,0x3ff00000,0x55555555,0x3fd55555,0x667f3bcd,0x3ff6a09e,0,0x00080000,0,0xc0000000,0xa0000000,0xbfb99999]
        if probe!=wanted_probe:errors.append({'probe':probe,'expected':wanted_probe})
        # Actual mode1 source opacity dispatch, including offset and tail guard.
        for alpha in [255,254]:
            gpu.upload(0,(C.c_uint32*3)(0xffffffff,0xffffffff,(alpha<<24)|0x123456));gpu.upload(1,(C.c_uint32*16)(*([0]*16)))
            u[0]=1;u[2]=3;f['glUniform1iv'](loc,32,(C.c_int32*32)(*u));f['glDispatchCompute'](1,1,1);gpu.fence();assert gpu.read(1,1)==[int(alpha!=255)]
        report={'status':'passed' if not errors else 'failed','cases':tested,'counts':counts,'errors':errors[:100],
                'error_count':len(errors),'library_sha256':hashlib.sha256(library.encode()).hexdigest(),
                'gles':gpu.version,'renderer':gpu.renderer,'shader_dialect_unmodified':True,'native_fp64_required':False,
                'physical_android_tested':False,'elapsed_seconds':round(time.monotonic()-started,3)}
        (work/'soft64_arithmetic1965.json').write_text(json.dumps(report,indent=2)+'\n')
        print(json.dumps(report,indent=2));assert not errors
        return report
    finally:gpu.close()

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--work',required=True);p.add_argument('--samples',type=int,default=4000);a=p.parse_args();test(a.work,a.samples)
