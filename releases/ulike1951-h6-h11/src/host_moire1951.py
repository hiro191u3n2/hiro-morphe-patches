#!/usr/bin/env python3
"""Execute the actual C moire kernel against the original Java pixel oracle."""
from pathlib import Path
import argparse,ctypes,json,os,shutil,struct,subprocess
SOURCE_NAMES=('SpeedWorkers1935.java','QualityPixels1932.java','NativeMoire1951.java',
              'PolicyCache1945.java','QualityShadow1932.java','NoiseCache1944.java',
              'NativeSpeed1944.java','GpuInteger1949.java','NativeSpeed1935.java',
              'SpatialNoise1934.java','LongMoire1934.java','tests/MoireOracle1951.java',
              'tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')
def test(root,work):
    root,work=Path(root),Path(work)/'host-moire1951'
    work.mkdir(parents=True,exist_ok=True)
    classes=work/'classes';classes.mkdir(exist_ok=True)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    cmd=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
    built=subprocess.run(cmd+['-source','8','-target','8','-Xlint:-options','-d',str(classes),
                           *map(str,(root/name for name in SOURCE_NAMES))],
                         text=True,capture_output=True,timeout=120)
    (work/'compile.log').write_text(built.stdout+built.stderr)
    if built.returncode:raise RuntimeError(built.stderr[-10000:])
    fixtures=work/'oracle.bin'
    subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.MoireOracle1951',str(fixtures)],
                   check=True,timeout=120)
    library=work/'libmoire1951_host.so'
    cbuild=subprocess.run(['cc','-std=c11','-O3','-shared','-fPIC','-DMOIRE1951_HOST',
                           '-Wall','-Wextra','-Werror',str(root/'native1951/moire1951.c'),
                           '-o',str(library)],text=True,capture_output=True,timeout=120)
    (work/'cc.log').write_text(cbuild.stdout+cbuild.stderr)
    if cbuild.returncode:raise RuntimeError(cbuild.stderr[-10000:])
    entry=ctypes.CDLL(str(library)).moire1951_host
    entry.argtypes=[ctypes.POINTER(ctypes.c_uint32),ctypes.POINTER(ctypes.c_uint32),
                    ctypes.c_int,ctypes.c_int,ctypes.c_int,ctypes.c_int]
    entry.restype=None
    data=memoryview(fixtures.read_bytes());position=0;cases=pixels=corrections=0
    while position<len(data):
        width,rows,first,last=struct.unpack_from('>4I',data,position);position+=16
        n=width*rows
        source=struct.unpack_from('>'+str(n)+'I',data,position);position+=4*n
        expected=struct.unpack_from('>'+str(n)+'I',data,position);position+=4*n
        NativeArray=ctypes.c_uint32*n
        src=NativeArray(*source)
        actual=NativeArray(*([0x13579bdf]*n))
        entry(src,actual,width,rows,first,last)
        for i,(a,e) in enumerate(zip(actual,expected)):
            if a!=e:
                raise AssertionError('C vs Java moire case=%d index=%d x=%d y=%d actual=%08x expected=%08x'
                                     %(cases,i,i%width,i//width,a,e))
        corrections+=sum(1 for i in range(first*width,last*width) if expected[i]!=source[i])
        pixels+=n;cases+=1
    if cases<30 or pixels<100000 or corrections<1:raise AssertionError('Insufficient moire/alpha/boundary cases')
    result={'status':'passed','cases':cases,'pixels_compared':pixels,
            'corrected_pixels':corrections,'host_C_to_original_Java_pixel_exact':True,
            'native_arm64_tested':False,'physical_device_tested':False}
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--out',required=True)
    args=parser.parse_args()
    print(json.dumps(test(Path(__file__).resolve().parent,args.out)))
