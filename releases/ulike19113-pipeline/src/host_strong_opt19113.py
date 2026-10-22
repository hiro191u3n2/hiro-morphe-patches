#!/usr/bin/env python3
"""Execute .112 and .113 Strong shaders with identical complete GPU inputs.

Only software GLES timing is reported. No device-speed or admission claim.
The independently byte-pinned .112 shader is never generated from .113.
"""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, statistics, subprocess, sys, time
import numpy as np

ROOT=Path(__file__).resolve().parent

def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    sys.path.insert(0,str(source))
    from strong_delta19113 import BASELINE_SHA256,extend_shader19113,restore_shader19113
    oldpath=source/'tests19113/strong/reference112/strong1960.comp'
    newpath=source/'native1960/strong1960.comp'
    baseline,candidate=oldpath.read_text(),newpath.read_text()
    assert hashlib.sha256(oldpath.read_bytes()).hexdigest()==BASELINE_SHA256
    assert extend_shader19113(baseline)==candidate
    assert restore_shader19113(candidate,True)==baseline
    # Prove that every standard/tiled/legacy numerical program still compiles
    # the exact old statements. Only optional profile6 is allowed to differ.
    import re
    def preprocess(text,mode,local,tile,ieee,pow2):
        text=re.sub(r'^#(?:version|extension)[^\n]*\n','',text,flags=re.M)
        argv=['cc','-E','-P','-x','c','-DGX_STRONG_MODE='+str(mode),'-DGX_LOCAL_SIZE='+str(local),
              '-DGX_TILE_WIDTH='+str(tile),'-DGX_IEEE_DIV73='+str(ieee),'-DGX_POW2_EXACT78='+str(pow2),'-']
        result=subprocess.run(argv,input=text,text=True,capture_output=True,check=True)
        return result.stdout
    unchanged_programs=0
    for mode in (-1,0,1,2,3):
        for local in (32,64,128):
            for tile in (0,8):
                for ieee in (0,1):
                    for pow2 in (0,1):
                        if mode==3 and tile==0 and ieee==1 and pow2==1:continue
                        assert preprocess(baseline,mode,local,tile,ieee,pow2)==preprocess(candidate,mode,local,tile,ieee,pow2),'unrelated program changed'
                        unchanged_programs+=1
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
          [oldpath,newpath,source/'strong_delta19113.py',source/'host_strong_opt19113.py',source/'tests1996/shader/guard1996.py']}
    spec=importlib.util.spec_from_file_location('strong113_gl',source/'tests1996/shader/guard1996.py')
    gl=importlib.util.module_from_spec(spec);spec.loader.exec_module(gl)
    c=gl.c; assertions=0;reads=0;pixels=0;negatives=0

    def check(value,message):
        nonlocal assertions
        assertions+=1
        if not value:raise AssertionError(message)

    def program(text,local,tile,pow2,trace=False):
        defines=f'\n#define GX_LOCAL_SIZE {local}\n#define GX_STRONG_MODE 3\n#define GX_TILE_WIDTH {tile}\n#define GX_IEEE_DIV73 1\n#define GX_POW2_EXACT78 {pow2}\n'
        if trace:defines+='#define GX_TRACE19106 1\n'
        p,size=gl.program(text.replace('#version 310 es','#version 310 es'+defines,1).encode())
        check(size==local,'compiled local size');return p

    def execute(prog,local,tile,arrays,u,stripe_groups=0):
        nonlocal reads,pixels
        gl.use(prog);loc=gl.location(prog,b'u');check(loc>=0,'uniform')
        for slot,array in enumerate(arrays):
            gl.bind(0x90d2,gl.buffers[slot]);gl.data(0x90d2,array.nbytes,array.ctypes.data,0x88e8);gl.base(0x90d2,slot,gl.buffers[slot])
        rows=int(u[3]-u[2]);groups=((int(u[0])+7)//8)*((rows+local//8-1)//(local//8)) if tile else (int(u[0])*rows+local-1)//local
        step=stripe_groups or groups
        for start in range(0,groups,step):
            params=u.copy();params[31]=start*local
            gl.uniform(loc,32,params.ctypes.data_as(c.POINTER(gl.I)))
            gl.dispatch(min(step,groups-start),1,1);gl.barrier(0x2000)
        gl.finish();check(gl.error()==0,'GL error')
        for slot in (0,2,3,4,5,6):
            check(np.array_equal(gl.read(slot,arrays[slot]),arrays[slot]),'immutable buffer '+str(slot))
        result=(gl.read(1,arrays[1]),gl.read(7,arrays[7]));reads+=1;pixels+=len(result[0]);return result

    def equal(expected,actual,name):
        for slot,(a,b) in enumerate(zip(expected,actual)):
            check(np.array_equal(a,b),name+' output '+str(slot)+' differs at '+str(np.flatnonzero(a!=b)[:8]))

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
    programs={};cases=[]
    for local in (32,64,128):
        for tile,pow2 in ((0,0),(0,1),(8,1)):
            label=f'local{local}/tile{tile}/pow2{pow2}'
            pair=[program(text,local,tile,pow2) for text in (baseline,candidate)]
            programs[(local,tile,pow2)]=pair
            for name,(arrays,u) in fixtures:
                before=execute(pair[0],local,tile,arrays,u)
                after=execute(pair[1],local,tile,arrays,u)
                equal(before,after,label+'/'+name)
                if not u[12]:check(np.array_equal(after[1],arrays[7]),'confidence-disabled preservation')
                # Split groups exercise u[31] and partial trailing invocations.
                if name.startswith('65x65'):equal(before,execute(pair[1],local,tile,arrays,u,3),label+'/split')
            cases.append(label)
            print('PASS '+label,flush=True)

    # Trace output compares every observed intermediate scalar and presence word,
    # including the local weighting loops whose reuse changed in this release.
    trace_pair=[program(text,64,0,1,True) for text in (baseline,candidate)]
    for name,(arrays,u) in fixtures[2:]:
        traced=list(arrays);traced[7]=np.zeros(4096,np.int32);params=u.copy();params[12]=0
        before=execute(trace_pair[0],64,0,traced,params)
        after=execute(trace_pair[1],64,0,traced,params)
        equal(before,after,'trace/'+name)
        check(np.count_nonzero(after[1][2048:])>0,'trace presence')

    arrays,u=fixtures[-1][1];before=execute(programs[(64,0,0)][0],64,0,arrays,u)
    # This numerical mutant is actually compiled and executed. A comparison
    # test which ignores a pixel difference must therefore fail this control.
    needle='confidenceOut);\n#endif\n\n    int ay='
    mutant=candidate.replace(needle,'confidenceOut)^1u;\n#endif\n\n    int ay=',1)
    check(mutant!=candidate,'numerical mutant anchor')
    changed=execute(program(mutant,64,0,0),64,0,arrays,u)
    check(not np.array_equal(before[0],changed[0]),'actual compiled pixel mutant rejected');negatives+=1
    for corrupted in [candidate.replace('.12*lumaRatioSquared19100','.13*lumaRatioSquared19100',1),candidate.replace('if(z>5.0)continue;','if(z>5.1)continue;',1)]:
        try:
            check(restore_shader19113(corrupted,True)==baseline,'mutated exact delta')
        except AssertionError:negatives+=1
        else:raise AssertionError('unreviewed arithmetic delta accepted')

    timing=[]
    for kind in ('smooth','grain','texture'):
        arrays,u=fixture(512,256,kind);pair=programs[(64,0,1)]
        expected=execute(pair[0],64,0,arrays,u);equal(expected,execute(pair[1],64,0,arrays,u),'warmup')
        samples=[[],[]]
        for trial in range(8):
            for i in ((0,1) if trial%2==0 else (1,0)):
                begin=time.perf_counter();actual=execute(pair[i],64,0,arrays,u)
                samples[i].append((time.perf_counter()-begin)*1000);equal(expected,actual,'timed/'+kind)
        medians=[statistics.median(v) for v in samples]
        timing.append(dict(kind=kind,width=512,height=256,baseline112_ms=medians[0],candidate113_ms=medians[1],ratio=medians[1]/medians[0],samples_ms=samples))
    check(pins=={name:hashlib.sha256((source/name).read_bytes()).hexdigest() for name in pins},'inputs unchanged during proof')
    result=dict(status='passed',assertions=assertions,negative_controls=negatives,actual_gles=True,
        baseline_shader_sha256=BASELINE_SHA256,candidate_shader_sha256=hashlib.sha256(newpath.read_bytes()).hexdigest(),
        configurations=cases,fixtures=len(fixtures),reads=reads,compared_pixels=pixels,whole_output_differences=0,
        confidence_differences=0,trace_intermediate_differences=0,immutable_inputs_preserved=True,
        split_dispatch_tested=True,full_luma_ratio_shared_exactly=True,per_pixel_repeated_divisions_removed=25,
        optimized_profile=6,unchanged_standard_profile=4,unchanged_tiled_profile=7,adaptive_gpu_speed_gate_percent=10,
        unchanged_preprocessed_programs=unchanged_programs,
        software_gles_timing=timing,renderer=gl.getstr(0x1f01).decode(),source_sha256=pins,
        physical_android_tested=False,device_speedup_verified=False,speed_gate=False)
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True)
    a=p.parse_args();print(json.dumps(test(a.source,a.work),indent=2))
