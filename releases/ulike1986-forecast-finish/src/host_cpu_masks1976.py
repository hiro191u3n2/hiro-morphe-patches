#!/usr/bin/env python3
"""Execute exact CPU mask pairing and bounded native YUV-band reuse against frozen75."""
from pathlib import Path
import argparse,ctypes,hashlib,json,os,re,shutil,struct,subprocess,time,statistics

def run(args,log,timeout=180):
    p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=timeout)
    Path(log).write_text(p.stdout+p.stderr)
    if p.returncode:raise RuntimeError(str(log)+'\n'+p.stdout[-1500:]+p.stderr[-9000:])
    return p.stdout

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'cpu-masks1976';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parents[1])
    ref=root/'tests1976/cpu-mask75-reference';frozen={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in ref.iterdir() if p.is_file()}
    generated=work/'generated';generated.mkdir(exist_ok=True)
    old=[]
    for name,new in [('FaceRegions1934','FaceRegions1975Reference'),('GpuPolicy1960','GpuPolicy1975Reference')]:
        p=generated/(new+'.java');p.write_text((ref/(name+'.java')).read_text().replace(name+'Pixels',name+'PIXEL_SENTINEL').replace(name,new).replace(new+'PIXEL_SENTINEL',name+'Pixels'));old.append(p)
    from host_moire1951 import SOURCE_NAMES,compile_java
    classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=[root/n for n in SOURCE_NAMES]+old+list((root/'tests/h24h26/graphics').rglob('*.java'))+[
        root/'GpuPolicy1960.java',root/'PairedRegions1976.java',root/'CpuFinishCache1976.java',root/'quality-dependencies/com/hiro/ulike/FaceRegions1934.java',
        root/'quality-dependencies/com/hiro/ulike/FaceRegions1934Pixels.java',root/'tests1976/CpuMasks1976Test.java',
        root/'tests1976/CpuMaskStubs.java',root/'tests1976/NativeSharp1976.java',root/'tests1976/NativeCache1976Test.java']
    run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources],work/'compile.log')
    result=run([jdk/'bin/java','-cp',classes,'com.hiro.ulike.CpuMasks1976Test'],work/'mask-run.log')
    match=re.search(r'RESULT (\{[^\n]+\})',result);assert match
    mask=json.loads(match[1]);assert mask['assertions']>100000
    library=work/'libulike_moire1951.so'
    flags=['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror']
    run(flags+['-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),root/'native1951/moire1951.c','-o',library],work/'jni-compile.log')
    cache_checked=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.NativeCache1976Test'],work/'cache-jni-checked.log')
    if 'RESULT' not in cache_checked or 'WARNING in native method' in cache_checked:raise AssertionError('cache control JNI failed')
    cache_controls=json.loads(re.search(r'RESULT (\{[^\n]+\})',cache_checked)[1])
    exercised=run([jdk/'bin/java','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.NativeSharp1976'],work/'jni-run.log')
    if 'JNI_PASS' not in exercised:raise AssertionError('actual JNI failed')
    if 'WARNING in native method' in exercised or 'FATAL ERROR' in exercised:raise AssertionError('JNI validation warning')
    checked=run([jdk/'bin/java','-Xcheck:jni','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.NativeSharp1976','serial-check'],work/'jni-checked.log')
    if 'JNI_PASS' not in checked or 'WARNING in native method' in checked or 'FATAL ERROR' in checked:raise AssertionError('checked JNI validation failed')
    # Independently execute the frozen75 native kernel and candidate cache kernel.
    oracle=work/'libfrozen75.so';candidate=work/'libcandidate76.so'
    run(flags+['-DMOIRE1951_HOST',ref/'moire1951.c','-o',oracle],work/'frozen75-compile.log')
    run(flags+['-DMOIRE1951_HOST',root/'native1951/moire1951.c','-o',candidate],work/'candidate76-compile.log')
    oldfn=ctypes.CDLL(str(oracle)).finish1951_host;newfn=ctypes.CDLL(str(candidate)).finish1976_host
    baseargs=[ctypes.POINTER(ctypes.c_uint32),ctypes.POINTER(ctypes.c_uint32),ctypes.POINTER(ctypes.c_int32),*([ctypes.c_int]*12)]
    oldfn.argtypes=baseargs;newfn.argtypes=baseargs+[ctypes.POINTER(ctypes.c_int32)];oldfn.restype=newfn.restype=None
    # Original .50 Java fixture generator still supplies independent integer policy and pixels.
    from host_common1953 import pregpu1950_root
    baseline=pregpu1950_root(root);baselineclasses=work/'baseline-classes';compile_java(baseline,baselineclasses,[root/'tests/MoireOracle1951.java'])
    fixtures=work/'oracle.bin';run([jdk/'bin/java','-cp',baselineclasses,'com.hiro.ulike.MoireOracle1951',fixtures],work/'fixture.log')
    blob=memoryview(fixtures.read_bytes());at=0;cases=pixels=changed=0
    while at<len(blob):
        width,rows,first,last,moire,sharp,gain,floor,limit,texture,halo,origin=struct.unpack_from('>12i',blob,at);at+=48;n=width*rows;count=max(1,(last-first)*width)*4
        source=struct.unpack_from('>'+str(n)+'I',blob,at);at+=n*4
        expected=struct.unpack_from('>'+str(n)+'I',blob,at);at+=n*4
        policy=struct.unpack_from('>'+str(count)+'i',blob,at);at+=count*4
        Src=ctypes.c_uint32*n;Pol=ctypes.c_int32*count;Scratch=ctypes.c_int32*(2+width*min(rows,16+(64 if moire else 8)))
        src=Src(*source);before=Src(*([0x13579bdf]*n));after=Src(*([0x13579bdf]*n));pol=Pol(*policy);scratch=Scratch();scratch[0]=-1
        start=first
        while start<last:
            end=min(last,(start+16)&~15)
            if end<=start:end=min(last,start+16)
            ptr=ctypes.cast(ctypes.byref(pol,(start-first)*width*16),ctypes.POINTER(ctypes.c_int32))
            args=[src,None,ptr,1,width,rows,start,end,moire,sharp,gain,floor,limit,texture,halo]
            args[1]=before;oldfn(*args);args[1]=after;newfn(*args,scratch);start=end
        if tuple(before)!=expected:raise AssertionError('frozen75 original Java discrepancy '+str(cases))
        if tuple(after)!=expected:raise AssertionError('cache pixel mismatch '+str(cases))
        if tuple(src)!=source:raise AssertionError('cache mutates source')
        changed+=sum(a!=source[i] for i,a in enumerate(after) if first*width<=i<last*width)
        cases+=1;pixels+=n
    if cases<400 or changed<1000:raise AssertionError('insufficient native exercise')
    # Whole repeated correction timing, all transfers here are ordinary host C arrays.
    width,rows=1024,256;n=width*rows;Src=ctypes.c_uint32*n
    src=Src(*(0xff000000|((120+(25 if x%4<2 else -25))<<16)|(120<<8)|(120-(25 if x%4<2 else -25)) for y in range(rows) for x in range(width)))
    dst=Src();pol=(ctypes.c_int32*4)(12,4,256,128);scratch=(ctypes.c_int32*(2+width*80))();times={'frozen75':[],'candidate76':[]}
    for trial in range(8):
        for label,fn in [('candidate76',newfn),('frozen75',oldfn)] if trial%2 else [('frozen75',oldfn),('candidate76',newfn)]:
            scratch[0]=-1;scratch[1]=0;start=time.perf_counter_ns()
            for first in range(0,rows,16):
                args=[src,dst,pol,0,width,rows,first,min(rows,first+16),1,1,128,256,16,1,1]
                if fn is newfn:args.append(scratch)
                fn(*args)
            if trial>=2:times[label].append(time.perf_counter_ns()-start)
    benchmark={k:round(statistics.median(v)/1e6,3) for k,v in times.items()}
    for name,digest in frozen.items():
        if hashlib.sha256((ref/name).read_bytes()).hexdigest()!=digest:raise AssertionError('frozen75 oracle changed '+name)
    result={'status':'passed','assertions':mask['assertions']+pixels+cases*2+cache_controls['assertions'],'mask_report':mask,'cache_controls':cache_controls,'native_cases':cases,'native_pixels_compared':pixels,'native_corrected_pixels':changed,
        'foreground_cpu_finish_cache_and_fallback_verified':True,'published75_mask_exact':True,'published75_policy_exact':True,'published75_native_pixels_exact':True,'unknown_callback_order_unchanged':True,
        'source_immutable_and_alpha_edges_exact':True,'jni_concurrency_cancel_executed':True,'actual_host_jni_result':exercised.strip(),
        'host_c_benchmark_ms':benchmark,'host_c_speed_ratio':round(benchmark['frozen75']/benchmark['candidate76'],4),
        'fixed_max_cache_bytes_per_worker':2*1024*1024,'frozen75_sources':frozen,'physical_android_tested':False,'device_speedup_verified':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--root',default=str(Path(__file__).resolve().parent));p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk),indent=2))
