#!/usr/bin/env python3
"""Run the actual bounded GPU prepass and dedicated Strong kernels.

Compare every ARGB pixel and confidence word against independently frozen114
programs; this software GLES proof does not establish phone speed.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,time
import numpy as np
ROOT=Path(__file__).resolve().parent

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    oldpath=source/'tests19115/provenance/reference114/strong1960.comp';newpath=source/'native1960/strong1960.comp'
    assert hashlib.sha256(oldpath.read_bytes()).hexdigest()=='fd2c6845d4df9cfef42bd20d563ed37362bb43bf6138f84674d7a6d1c00cd051'
    baseline=oldpath.read_text();candidate=newpath.read_text()
    spec=importlib.util.spec_from_file_location('regional115_gl',source/'tests1996/shader/guard1996.py')
    gl=importlib.util.module_from_spec(spec);spec.loader.exec_module(gl);c=gl.c
    delete=gl.gl('glDeleteBuffers',[gl.I,c.POINTER(gl.U)],None)
    assertions=0;compared_pixels=0;compared_confidence_words=0;cases=[]
    def check(ok,message):
        nonlocal assertions
        assertions+=1
        if not ok:raise AssertionError(message)
    def program(text,local,tile,pow2,kind=0):
        defines=f'\n#define GX_LOCAL_SIZE {local}\n#define GX_STRONG_MODE 3\n#define GX_TILE_WIDTH {tile}\n#define GX_IEEE_DIV73 1\n#define GX_POW2_EXACT78 {pow2}\n'
        if kind:defines+=f'#define GX_REGIONAL_PASS19115 {int(kind==1)}\n#define GX_REGIONAL_MAIN19115 {int(kind==2)}\n#define GX_DIV_FAST19115 1\n'
        p,size=gl.program(text.replace('#version 310 es','#version 310 es'+defines,1).encode());check(size==local,'local size');return p
    prepass=program(candidate,64,0,0,1)
    def execute(prog,local,tile,arrays,u,regional=False,stripe_groups=0):
        for slot,array in enumerate(arrays):
            gl.bind(0x90d2,gl.buffers[slot]);gl.data(0x90d2,array.nbytes,array.ctypes.data,0x88e8);gl.base(0x90d2,slot,gl.buffers[slot])
        rows=int(u[3]-u[2]);count=int(u[0])*rows;scratch=(gl.U*1)()
        def dispatch(p,width,tiled,parameters):
            gl.use(p);loc=gl.location(p,b'u');check(loc>=0,'uniform')
            groups=((int(u[0])+7)//8)*((rows+width//8-1)//(width//8)) if tiled else (count+width-1)//width
            step=stripe_groups or groups
            for start in range(0,groups,step):
                current=parameters.copy();current[31]=start*width
                gl.uniform(loc,32,current.ctypes.data_as(c.POINTER(gl.I)));gl.dispatch(min(step,groups-start),1,1);gl.barrier(0x2000)
        params=u.copy()
        try:
            if regional:
                gl.gen(1,scratch);gl.bind(0x90d2,scratch[0]);gl.data(0x90d2,count*12,None,0x88e8)
                gl.base(0x90d2,1,scratch[0]);dispatch(prepass,64,0,params)
                gl.base(0x90d2,1,gl.buffers[1]);gl.base(0x90d2,2,scratch[0]);params[21]=-1;params[22]=count*3
            dispatch(prog,local,tile,params);gl.finish();check(gl.error()==0,'GL dispatch error')
            for slot in (0,2,3,4,5,6):check(np.array_equal(gl.read(slot,arrays[slot]),arrays[slot]),'immutable '+str(slot))
            return gl.read(1,arrays[1]),gl.read(7,arrays[7])
        finally:
            if scratch[0]:delete(1,scratch)
    def equal(a,b,name):
        nonlocal compared_pixels,compared_confidence_words
        compared_pixels+=len(a[0]);compared_confidence_words+=len(a[1])
        for slot,(x,y) in enumerate(zip(a,b)):check(np.array_equal(x,y),name+' output '+str(slot)+' differs at '+str(np.flatnonzero(x!=y)[:8]))
    rng=np.random.default_rng(1911301)
    def fixture(w,h,kind,partial=False,capture=True):
        rgb=np.full((h,w,3),65,np.int32)
        if kind=='grain':rgb+=rng.integers(-28,29,rgb.shape)
        elif kind=='texture':rgb+=np.where(np.indices((h,w)).sum(axis=0)%4<2,90,-55)[:,:,None]
        elif kind=='random':rgb=rng.integers(0,256,(h,w,3),dtype=np.int32)
        image=(np.uint32(0xff000000)|(rgb[:,:,0].astype(np.uint32)<<16)|(rgb[:,:,1].astype(np.uint32)<<8)|rgb[:,:,2].astype(np.uint32)).ravel()
        if kind=='random':image[::7]&=np.uint32(0x7fffffff)
        first,last=(1,h-1) if partial and h>2 else (0,h);n=w*(last-first)
        u=np.zeros(32,np.int32);u[:13]=[w,h,first,last,0,h,0,h,4,1,3,1,int(capture)]
        u[30]=min(w-1,w//2)
        ev=rng.uniform(.05,18,16+3*((w+63)//64)*((h+63)//64)).astype(np.float32)
        ev[18::3]=rng.uniform(0,.7,len(ev[18::3])).astype(np.float32)
        policy=np.zeros(n*2,np.int32);policy[::2]=256
        if kind=='random':policy[::2]=rng.integers(0,257,n);policy[1::2]=rng.integers(0,257,n)
        maps=[];mw,mh=w,h
        for _ in range(3):
            mw=(mw+1)//2;mh=(mh+1)//2
            # Valid signed deltas and immutable luma, with independent channels.
            delta=rng.integers(-12,13,(mw*mh,3),dtype=np.int32)&255
            maps.append(np.uint32(65<<24)|(delta[:,0].astype(np.uint32)<<16)|(delta[:,1].astype(np.uint32)<<8)|delta[:,2].astype(np.uint32))
        cf=np.full(((w+3)//4)*((last-first+3)//4),0x1234abcd,np.int32)
        return [image,np.zeros(n,np.uint32),ev,policy,*maps,cf],u

    fixtures=[(str(w)+'x'+str(h)+'/'+kind,fixture(w,h,kind,partial,capture)) for w,h,kind,partial,capture in
        [(1,1,'smooth',False,True),(3,5,'random',True,True),(7,9,'grain',False,False),
         (31,33,'smooth',True,True),(65,65,'grain',False,True),(129,17,'texture',True,True),
         (67,19,'random',True,False),(128,64,'grain',False,True)]]
    # Production strips keep only a source halo, regional rows and map rows.
    # Exercise every nonzero base independently with asymmetric image data.
    arrays,u=fixture(65,65,'grain',False,True)
    u[2:4]=[9,51];u[6]=97;u[7]=227;u[13]=1;u[14]=3
    rows=int(u[3]-u[2]);n=int(u[0])*rows
    arrays[0]=arrays[0].reshape(65,65)[3:57].copy().ravel()
    arrays[1]=np.zeros(n,np.uint32)
    rw=(int(u[0])+63)//64;rh=(int(u[7])+63)//64
    ev=rng.uniform(.05,18,16+3*rw*rh).astype(np.float32)
    ev[18::3]=rng.uniform(0,.7,len(ev[18::3])).astype(np.float32)
    arrays[2]=np.concatenate([ev[:16],ev[16+3*rw:]])
    arrays[3]=np.zeros(n*2,np.int32);arrays[3][::2]=256
    mw,mh=int(u[0]),int(u[7])
    for m in range(3):
        mw=(mw+1)//2;mh=(mh+1)//2
        delta=rng.integers(-12,13,(mh,mw,3),dtype=np.int32)&255
        packed=np.uint32(65<<24)|(delta[:,:,0].astype(np.uint32)<<16)|(delta[:,:,1].astype(np.uint32)<<8)|delta[:,:,2].astype(np.uint32)
        first=int((int(u[6])+int(u[2])+.5)/(1<<(m+1))-.5)-1
        u[15+m]=first;arrays[4+m]=packed[first:].copy().ravel()
    arrays[7]=np.full(((int(u[0])+3)//4)*((rows+3)//4),0x1234abcd,np.int32)
    fixtures.append(('65x65/retained-source-regional-map-bases',(arrays,u)))
    for noise,shadows,has_policy in ((0,0,0),(1,0,0),(2,1,1),(3,0,1),(5,1,0)):
        arrays,u=fixture(35,17,'grain',True,True)
        u[8]=noise;u[9]=shadows;u[11]=has_policy
        fixtures.append((f'35x17/noise{noise}-shadows{shadows}-policy{has_policy}',(arrays,u)))
    # Exercise branch thresholds with the same pinned source/candidate bytes.
    for level in (np.nextafter(np.float32(.4),np.float32(0)),np.float32(.4),
                  np.nextafter(np.float32(.6),np.float32(0)),np.float32(.6)):
        arrays,u=fixture(17,13,'smooth',False,True)
        arrays[2][:16]=level;arrays[2][16::3]=level;arrays[2][17::3]=level;arrays[2][18::3]=0
        fixtures.append(('17x13/evidence'+str(float(level)),(arrays,u)))
    programs={}
    for local in (32,64,128):
        for tile,pow2 in ((0,0),(8,0),(0,1),(8,1)):
            pair=(program(baseline,local,tile,pow2),program(candidate,local,tile,pow2,2));programs[(local,tile,pow2)]=pair
            label=f'local{local}/tile{tile}/pow2{pow2}'
            for name,(arrays,u) in fixtures:
                before=execute(pair[0],local,tile,arrays,u)
                after=execute(pair[1],local,tile,arrays,u,True)
                equal(before,after,label+'/'+name)
                if not u[12]:check(np.array_equal(after[1],arrays[7]),'confidence-disabled preservation')
                if name.startswith('65x65'):equal(before,execute(pair[1],local,tile,arrays,u,True,3),label+'/split')
            cases.append(label);print('PASS '+label,flush=True)
    # Execute all16 full-resolution portrait bands and all12 landscape bands.
    # Partial final bands are compared numerically here, although production
    # speed admission deliberately keeps them on the published route.
    full=[]
    for w,h in ((3060,4080),(4080,3060)):
        original,base=fixture(w,h,'grain',False,True)
        baseline_parts=[];candidate_parts=[];start=time.monotonic();count=0
        pair=programs[(64,0,0)]
        for first in range(0,h,256):
            last=min(h,first+256);rows=last-first;arrays=list(original);u=base.copy();u[2:4]=[first,last]
            arrays[1]=np.zeros(w*rows,np.uint32);arrays[3]=np.zeros(w*rows*2,np.int32);arrays[3][::2]=256
            arrays[7]=np.full(((w+3)//4)*((rows+3)//4),0x1234abcd,np.int32)
            before=execute(pair[0],64,0,arrays,u);after=execute(pair[1],64,0,arrays,u,True)
            equal(before,after,f'full {w}x{h} rows {first}:{last}');count+=1
            baseline_parts.append(before[0]);candidate_parts.append(after[0])
        check(np.array_equal(np.concatenate(baseline_parts),np.concatenate(candidate_parts)),'complete frame')
        full.append({'width':w,'height':h,'bands':count,'pixels':w*h,'elapsed_seconds':round(time.monotonic()-start,3)})
        print(f'PASS full {w}x{h}',flush=True)
    # Deliberately corrupt a prepass component and prove output comparison
    # detects the resulting error instead of silently ignoring the new input.
    needle='dst[3*at]=floatBitsToUint(value.x);'
    mutant=candidate.replace(needle,'dst[3*at]=floatBitsToUint(value.x+10.0);',1);check(mutant!=candidate,'mutant applied')
    actual_prepass=prepass;prepass=program(mutant,64,0,0,1)
    arrays,u=fixture(128,64,'grain',False,True);pair=programs[(64,0,0)]
    before=execute(pair[0],64,0,arrays,u);after=execute(pair[1],64,0,arrays,u,True)
    corrupt=int(np.count_nonzero(before[0]!=after[0]))+int(np.count_nonzero(before[1]!=after[1]));check(corrupt>0,'regional mutant detected');prepass=actual_prepass
    result={'status':'passed','assertions':assertions+compared_pixels+compared_confidence_words,
            'structural_assertions':assertions,'compared_pixels':compared_pixels,
            'compared_confidence_words':compared_confidence_words,'programs':cases,
            'full_frames':full,'prepass_mutant_differences':corrupt,
            'candidate_sha256':hashlib.sha256(newpath.read_bytes()).hexdigest(),
            'baseline_sha256':hashlib.sha256(oldpath.read_bytes()).hexdigest(),
            'physical_android_tested':False,'device_speedup_verified':False,
            'transport':'Actual software GLES, prepass table stays on GPU; JNI lifecycle is tested separately.'}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT));p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk)))
