#!/usr/bin/env python3
"""Run actual fused C/JNI moire+sharpen against pinned v1.9.50 Java pixels."""
from pathlib import Path
import argparse,ctypes,json,os,re,shutil,struct,subprocess
SOURCE_NAMES=('SpeedWorkers1935.java','QualityPixels1932.java','NativeMoire1951.java',
              'PolicyCache1945.java','QualityShadow1932.java','NoiseCache1944.java',
              'NativeSpeed1944.java','GpuInteger1949.java','NativeSpeed1935.java',
              'SpatialNoise1934.java','LongMoire1934.java',
              'tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')
def compile_java(source,classes,tests):
    classes.mkdir(parents=True,exist_ok=True)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    cmd=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    paths=[source/name for name in SOURCE_NAMES if (source/name).is_file()]
    p=subprocess.run(cmd+['-source','8','-target','8','-Xlint:-options','-d',str(classes),
                         *map(str,paths+tests)],text=True,capture_output=True,timeout=120)
    (classes.parent/(classes.name+'-compile.log')).write_text(p.stdout+p.stderr)
    if p.returncode:raise RuntimeError(p.stderr[-10000:])
def test(root,work):
    root,work=Path(root),Path(work)/'host-moire1951'
    work.mkdir(parents=True,exist_ok=True)
    from host_common1953 import pregpu1950_root
    baseline=pregpu1950_root(root)
    if not (baseline/'QualityPixels1932.java').is_file():raise RuntimeError('Pinned v1.9.50 oracle sources absent')
    classes=work/'baseline-classes'
    compile_java(baseline,classes,[root/'tests/MoireOracle1951.java'])
    fixtures=work/'oracle.bin'
    subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.MoireOracle1951',str(fixtures)],
                   check=True,timeout=120)
    library=work/'libmoire1951_host.so'
    cbuild=subprocess.run(['cc','-std=c11','-O3','-shared','-fPIC','-DMOIRE1951_HOST',
                           '-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror',
                           str(root/'native1951/moire1951.c'),'-o',str(library)],
                         text=True,capture_output=True,timeout=120)
    (work/'cc.log').write_text(cbuild.stdout+cbuild.stderr)
    if cbuild.returncode:raise RuntimeError(cbuild.stderr[-10000:])
    entry=ctypes.CDLL(str(library)).finish1951_host
    entry.argtypes=[ctypes.POINTER(ctypes.c_uint32),ctypes.POINTER(ctypes.c_uint32),
                    ctypes.POINTER(ctypes.c_int32),*([ctypes.c_int]*12)]
    entry.restype=None
    data=memoryview(fixtures.read_bytes());position=0;cases=pixels=corrections=sharp_cases=combined_cases=0
    sharpen_changes=combined_changes=0
    while position<len(data):
        (width,rows,first,last,moire,sharp,gain,floor,limit,texture,halo,origin)=struct.unpack_from('>12i',data,position)
        position+=48;n=width*rows;count=max(1,(last-first)*width)*4
        source=struct.unpack_from('>'+str(n)+'I',data,position);position+=4*n
        expected=struct.unpack_from('>'+str(n)+'I',data,position);position+=4*n
        policy=struct.unpack_from('>'+str(count)+'i',data,position);position+=4*count
        NativeArray=ctypes.c_uint32*n;PolicyArray=ctypes.c_int32*count
        src=NativeArray(*source);actual=NativeArray(*([0x13579bdf]*n));settings=PolicyArray(*policy)
        # Same 16-row production batches and compact per-batch policy offsets.
        start=first
        while start<last:
            end=min(last,(start+16)&~15)
            if end<=start:end=min(last,start+16)
            policy_at=ctypes.cast(ctypes.byref(settings,(start-first)*width*16),ctypes.POINTER(ctypes.c_int32))
            entry(src,actual,policy_at,1,width,rows,start,end,moire,sharp,gain,floor,limit,texture,halo)
            start=end
        for i,(a,e) in enumerate(zip(actual,expected)):
            if a!=e:raise AssertionError('C vs pinned v1.9.50 case=%d i=%d x=%d y=%d moire=%d sharp=%d actual=%08x expected=%08x'
                                        %(cases,i,i%width,i//width,moire,sharp,a,e))
        if tuple(src)!=source:raise AssertionError('C changed source image')
        changed=sum(1 for i in range(first*width,last*width) if expected[i]!=source[i])
        corrections+=changed;pixels+=n;cases+=1
        if sharp:
            sharp_cases+=1
            if not moire:sharpen_changes+=changed
            else:combined_cases+=1;combined_changes+=changed
    if cases<400 or pixels<1000000 or sharpen_changes<1 or combined_changes<1:
        raise AssertionError('Insufficient native moire/sharpen/alpha/mask/strip cases')
    current=work/'current-classes'
    compile_java(root,current,[root/'tests/NativeSharp1951.java'])
    java_info=subprocess.run(['java','-XshowSettings:properties','-version'],capture_output=True,text=True,check=True)
    match=re.search(r'^\s*java.home\s*=\s*(.+)$',java_info.stderr,re.M)
    java_home=Path(match.group(1)) if match else None
    include=Path(os.environ.get('ULIKE_JNI_INCLUDE',str(Path(os.environ.get('JAVA_HOME',str(java_home)))/'include')))
    if not (include/'jni.h').is_file():raise RuntimeError('JNI headers unavailable; set ULIKE_JNI_INCLUDE to a full JDK include directory')
    jni=work/'libulike_moire1951.so'
    p=subprocess.run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off',
                      '-Wall','-Wextra','-Werror','-I'+str(include),
                      '-I'+str(include/'linux'),str(root/'native1951/moire1951.c'),'-o',str(jni)],
                     capture_output=True,text=True,timeout=120)
    (work/'jni-build.log').write_text(p.stdout+p.stderr)
    if p.returncode:raise RuntimeError(p.stderr[-10000:])
    exercised=subprocess.run(['java','-Djava.library.path='+str(work),'-cp',str(current),
                              'com.hiro.ulike.NativeSharp1951'],capture_output=True,text=True,timeout=120)
    (work/'jni-run.log').write_text(exercised.stdout+exercised.stderr)
    if exercised.returncode or 'JNI_PASS' not in exercised.stdout:
        raise AssertionError('Actual native JNI path failed: '+exercised.stdout+exercised.stderr)
    result={'status':'passed','cases':cases,'pixels_compared':pixels,'corrected_pixels':corrections,
            'host_C_to_original_Java_pixel_exact':True,'pinned_java_oracle_version':'1.9.50',
            'native_sharpen_c_executed':True,'native_sharpen_pixel_exact':True,
            'combined_moire_sharpen_pixel_exact':True,'native_sharpen_cases':sharp_cases,
            'combined_moire_sharpen_cases':combined_cases,'native_sharpen_changed_pixels':sharpen_changes,
            'combined_changed_pixels':combined_changes,'actual_host_jni_executed':True,
            'host_jni_result':exercised.stdout.strip(),'native_arm64_tested':False,
            'physical_device_tested':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',required=True)
    args=parser.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,args.out)))
