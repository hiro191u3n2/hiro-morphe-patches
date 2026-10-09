#!/usr/bin/env python3
"""Run true-fp64 Single1960 on desktop Mesa against frozen Java1959 pixels.

Android GLES may not expose a true fp64 shader extension. This test does not
certify the Android route: only the dialect header is adapted to desktop GLSL;
the exact production computation body, buffers, order and precision are kept.
The independent Java pixel fixture is created by host_gpu1960.py.
"""
from pathlib import Path
import ctypes as C
import hashlib
import json
import struct
import time


def test(root, work, oracle_file):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'desktop-single-fp64-1960'
    work.mkdir(parents=True, exist_ok=True)
    oracle_file = Path(oracle_file).resolve()
    p=(root/'native1949/host_gpu1949.py').read_text().replace('(0x30A0)','(0x30A2)').replace('0x3040,0x40','0x3040,0x8').replace('ctxattrs=(C.c_int*3)(0x3098,3,0x3038)','ctxattrs=(C.c_int*7)(0x3098,4,0x30FB,3,0x30FD,1,0x3038)').replace("if 'OpenGL ES 3.' not in self.version:","if '4.' not in self.version:").replace("['fusion_sums1949','pack_rgb1949','residual1949']","['single1960']")

    s=(root/'native1960/single1960.comp').read_text().replace('#version 310 es','#version 430').replace('#extension GL_EXT_shader_explicit_arithmetic_types_float64 : require','#extension GL_ARB_gpu_shader_fp64 : require').replace('#extension GL_EXT_gpu_shader5 : require','').replace('#define D float64_t','#define D double');(work/'single1960.comp').write_text(s)
    ns={'__name__':'single_test'};exec(compile(p,'host_gpu1949.py','exec'),ns);g=ns['SoftwareGpu'](work)
    g.gl('glUniform1iv',None,[C.c_int,C.c_int,C.POINTER(C.c_int)])
    blob=oracle_file.read_bytes();offset=0

    def integer():
        nonlocal offset
        v=struct.unpack_from('>i',blob,offset)[0];offset+=4;return v

    def utf():
        nonlocal offset
        n=struct.unpack_from('>H',blob,offset)[0];offset+=2;v=blob[offset:offset+n].decode();offset+=n;return v

    def words():
        nonlocal offset
        n=integer();v=list(struct.unpack_from('>'+str(n)+'i',blob,offset));offset+=n*4;return v
    assert integer()==1960001
    fn=g.functions
    program=g.programs[0];fn['glUseProgram'](program);location=fn['glGetUniformLocation'](program,b'u[0]')
    count=0;pixels=0;errors=[];start=time.time()
    while True:
        name=utf()
        if not name:break
        shader=integer();u=words();invocations=integer();n=integer();buffers=[words() for _ in range(n)];expected=words();confidence=words()
        if shader!=1:continue
        for slot,b in enumerate(buffers):g.upload(slot,(C.c_int32*len(b))(*b))
        u[0]=0;u[31]=0
        fn['glUniform1iv'](location,32,(C.c_int*32)(*u));fn['glDispatchCompute']((u[17]*u[18]+63)//64,1,1);fn['glMemoryBarrier'](0x2000)
        u[0]=1;fn['glUniform1iv'](location,32,(C.c_int*32)(*u));fn['glDispatchCompute']((invocations+63)//64,1,1);g.fence();got=g.read(1,invocations)
        mismatches=[(i,a,b) for i,(a,b) in enumerate(zip(got,expected)) if a!=b]
        # Entire output is checked; keep routine output concise.
        if mismatches:errors.append({'name':name,'count':len(mismatches),'first':mismatches[:3]})
        count+=1;pixels+=invocations
    report={'cases':count,'pixels':pixels,'errors':errors,'renderer':g.renderer,'version':g.version,'seconds':time.time()-start,'reference':'frozen original Java v1.9.59','shader_body':'unmodified production single1960.comp; desktop dialect header only changed','mobile_fp64_confirmed':False,'physical_android_tested':False,'header_only_adaptation':True,'double_precision_preserved':True,'production_shader_sha256':hashlib.sha256((root/'native1960/single1960.comp').read_bytes()).hexdigest(),'frozen_oracle_sha256':hashlib.sha256(oracle_file.read_bytes()).hexdigest()}
    (work/'host-single-fp64-1960.json').write_text(json.dumps(report,indent=2)+'\n');g.close();assert count>=16 and pixels>=6168;assert not errors, str(errors);return report

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--work', required=True)
    parser.add_argument('--oracle', required=True)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.oracle)))
