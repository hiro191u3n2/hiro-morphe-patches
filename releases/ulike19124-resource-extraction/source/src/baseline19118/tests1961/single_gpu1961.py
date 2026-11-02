#!/usr/bin/env python3
"""GX13 real GLES shader vs frozen1960 Java CPU, original fp64 C preparation.

Production residual1961.comp is executed unchanged on software GLES. JNI
preparation code is compiled with the same no-FMA/no-fast-math constraints.
No mobile speed/device capability claim is inferred from this host test.
"""
from pathlib import Path
import ctypes as C
import hashlib
import json
import os
import struct
import subprocess
import time


def run(command):
    p = subprocess.run([str(x) for x in command], text=True, capture_output=True)
    if p.returncode:
        raise RuntimeError(p.stdout + p.stderr)
    return p.stdout


def test(root, work, jdk):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'single-residual1961'
    work.mkdir(parents=True, exist_ok=True)
    jdk = Path(jdk).resolve()
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    frozen = root / 'tests1961/published1960-reference'
    compile_sources = [frozen / 'SingleNoise1955.java',
        root / 'tests1961/singlefixtures/com/hiro/ulike/GpuSingle1960.java',
        root / 'tests1961/SingleOracle1961.java']
    run([jdk / 'bin/javac', '-d', classes, *compile_sources])
    oracle = work / 'frozen1960-single.bin'
    run([jdk / 'bin/java', '-cp', classes, 'com.hiro.ulike.SingleOracle1961', oracle])
    c_source = work / 'export.c'
    c_source.write_text('#include "' + str(root / 'native1960/residual_blocks1961.c') + '"\n' + r'''
int single61_export(const uint32_t *input,const float *data,const int *u,float *out) {
    int cells=u[9]*u[10];Model m={0};Workspace w={0};
    m.width=u[1];m.height=u[2];m.columns=u[9];m.rows=u[10];
    m.luma=data;m.chroma=data+cells;m.mean=data+cells*2;
    m.brightness_y=data+cells*3;m.brightness_c=m.brightness_y+8;m.ratios=m.brightness_c+8;
    m.left=(u[11]-1)*.5f;m.top=(u[12]-1)*.5f;
    m.step_x=u[9]==1?1.f:(float)(u[1]-u[11])/(u[9]-1);
    m.step_y=u[10]==1?1.f:(float)(u[2]-u[12])/(u[10]-1);
    for(int row=0;row<u[18];row++)for(int col=0;col<u[17];col++) {
        int bx=col*4-4,by=u[16]+row*4;float *record=out+(row*u[17]+col)*248;
        if(!export_block1961((const jint *)input,u[1],u[7],u[8],u[6],&m,u[13],u[14],bx,by,8,u[4],u[5],&w,record))return 0;
        record[196]=0;
        if(by+4>u[4]+u[6]&&bx>=0&&!export_block1961((const jint *)input,u[1],u[7],u[8],u[6],&m,u[13],u[14],bx,by,4,u[4],u[5],&w,record+196))return 0;
    }return 1;
}
''')
    library = work / 'export.so'
    run(['cc','-std=c11','-O2','-shared','-fPIC','-fno-fast-math','-ffp-contract=off',
        '-Wall','-Wextra','-Werror','-I'+str(jdk / 'include'),'-I'+str(jdk / 'include/linux'),
        c_source,'-lm','-o',library])
    cpu = C.CDLL(str(library))
    cpu.single61_export.argtypes = [C.POINTER(C.c_uint32), C.POINTER(C.c_float), C.POINTER(C.c_int32), C.POINTER(C.c_float)]
    cpu.single61_export.restype = C.c_int
    # Reuse the known real surfaceless GLES driver harness, changing only the
    # requested program filename. Production shader itself is never rewritten.
    harness = (root / 'native1949/host_gpu1949.py').read_text()
    old = "['fusion_sums1949','pack_rgb1949','residual1949']"
    assert old in harness
    harness = harness.replace(old, "['residual1961']")
    ns = {'__name__': 'single_residual_test'}
    exec(compile(harness, 'host_gpu1949.py', 'exec'), ns)
    gpu = ns['SoftwareGpu'](root / 'native1960')
    gpu.gl('glUniform1iv', None, [C.c_int,C.c_int,C.POINTER(C.c_int)])
    blob = oracle.read_bytes()
    offset = 0
    def integer():
        nonlocal offset
        value = struct.unpack_from('>i', blob, offset)[0]
        offset += 4
        return value
    def utf():
        nonlocal offset
        n = struct.unpack_from('>H', blob, offset)[0]
        offset += 2
        value = blob[offset:offset+n].decode()
        offset += n
        return value
    def words():
        nonlocal offset
        n = integer()
        values = list(struct.unpack_from('>'+str(n)+'i', blob, offset))
        offset += n*4
        return values
    assert integer() == 19610013
    f = gpu.functions
    program = gpu.programs[0]
    f['glUseProgram'](program)
    location = f['glGetUniformLocation'](program,b'u[0]')
    cases = chunks = pixels = changed = enabled = 0
    errors = []
    started = time.monotonic()
    try:
        while True:
            name = utf()
            if not name:
                break
            u = words(); source = words(); model_bits = words(); policy = words(); expected = words()
            source_array = (C.c_uint32*len(source))(*source)
            model_bytes = b''.join(struct.pack('=i', v) for v in model_bits)
            model = (C.c_float*len(model_bits)).from_buffer_copy(model_bytes)
            gpu.upload(0,source_array)
            gpu.upload(3,(C.c_int32*len(policy))(*policy))
            obtained = []
            begin,end = u[4],u[5]
            for first in range(begin,end,64):
                finish = min(end,first+64)
                local = u.copy();local[4]=first;local[5]=finish
                local[16]=((local[6]+first-7)//4)*4
                local[17]=(local[1]+3)//4+1
                local[18]=(local[6]+finish-local[16]+3)//4
                n = local[17]*local[18]*248
                records = (C.c_float*n)()
                assert cpu.single61_export(source_array,model,(C.c_int32*32)(*local),records)
                enabled += sum(records[i*248] != 0 or records[i*248+196] != 0 for i in range(local[17]*local[18]))
                gpu.upload(2,records)
                count = local[1]*(finish-first)
                gpu.upload(1,None,count*4)
                f['glUniform1iv'](location,32,(C.c_int32*32)(*local))
                f['glDispatchCompute']((count+63)//64,1,1)
                gpu.fence()
                obtained += gpu.read(1,count)
                chunks += 1
            mismatches = [(i,a,b) for i,(a,b) in enumerate(zip(obtained,expected)) if a != b]
            if mismatches:
                errors.append({'case':name,'count':len(mismatches),'first':mismatches[:3]})
            assert list(source_array) == [v&0xffffffff for v in source], 'immutable source changed'
            changed += sum(a != b for a,b in zip(expected,source[begin*u[1]:end*u[1]]))
            pixels += len(expected);cases += 1
        # Exact rounding boundary: a float value+.5 would falsely round this
        # reconstruction up; the original1955 double promotion rounds down.
        for residual,wanted in [(0.499992,0xff7f7f7f),(0.500008,0xff808080),(-0.500008,0xff7e7e7e)]:
            u=[0]*32;u[1]=u[2]=u[5]=1;u[16]=-8;u[17]=2;u[18]=3
            records=(C.c_float*(2*3*248))();base=(2*2+1)*248+196
            records[base]=1;records[base+1]=1;records[base+2]=residual
            gpu.upload(0,(C.c_uint32*1)(0xff7f7f7f));gpu.upload(1,None,4);gpu.upload(2,records);gpu.upload(3,(C.c_int32*1)(0))
            f['glUniform1iv'](location,32,(C.c_int32*32)(*u));f['glDispatchCompute'](1,1,1);gpu.fence()
            got=gpu.read(1,1)[0]&0xffffffff
            assert got==wanted,(residual,hex(got),hex(wanted))
        report={'schema':'gx13-residual1961-real-gles-v1','status':'passed' if not errors else 'failed',
            'cases':cases,'chunks':chunks,'pixels':pixels,'changed_pixels':changed,'enabled_block_records':enabled,
            'rounding_boundary_cases':3,'errors':errors,'renderer':gpu.renderer,'gles_version':gpu.version,
            'elapsed_seconds':round(time.monotonic()-started,3),'production_shader_unchanged':True,
            'double_preparation_preserved':True,'original_cpu_reference':'frozen published1960 SingleNoise1955.java',
            'physical_android_tested':False,'speed_claim':False,
            'shader_sha256':hashlib.sha256((root/'native1960/residual1961.comp').read_bytes()).hexdigest(),
            'native_preparation_sha256':hashlib.sha256((root/'native1960/residual_blocks1961.c').read_bytes()).hexdigest(),
            'frozen_cpu_sha256':hashlib.sha256((frozen/'SingleNoise1955.java').read_bytes()).hexdigest()}
        (work/'single-residual1961.json').write_text(json.dumps(report,indent=2)+'\n')
        assert cases>=26 and chunks>cases and enabled>0 and changed>0
        assert not errors, errors
        return report
    finally:
        gpu.close()


if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser()
    p.add_argument('--root',default=Path(__file__).resolve().parent.parent)
    p.add_argument('--work',required=True)
    p.add_argument('--jdk',required=True)
    a=p.parse_args()
    print(json.dumps(test(a.root,a.work,a.jdk)))
