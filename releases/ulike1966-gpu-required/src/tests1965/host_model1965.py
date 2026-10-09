#!/usr/bin/env python3
"""Execute exact GPU noise model against independently compiled frozen Java.

The hardware FP64 shader must reject an unsupported driver. Positive coverage
executes the portable binary64 model and transform on original GLES 3.1 source
against the frozen CPU oracle. Software Mesa results do not establish physical
Android performance or camera behavior.
"""
from pathlib import Path
import ctypes as C
import hashlib
import json
import os
import struct
import subprocess
import time


def test(root, work, jdk=None, ndk=None):
    root = Path(root).resolve()
    work = Path(work).resolve() / 'model1965'
    work.mkdir(parents=True, exist_ok=True)
    tracked = [root/'native1960'/name for name in ('model1965.comp','single1960.comp',
                'model1965_soft.comp','single1965_soft.comp','soft64_1965.glsl')]
    pins = {str(path.relative_to(root)):hashlib.sha256(path.read_bytes()).hexdigest() for path in tracked}
    jdk = Path(jdk or os.environ.get('ULIKE_JDK_HOME', '/usr/lib/jvm/java-17-openjdk-amd64'))
    reference = root / 'tests1964/published1963-reference/SingleNoise1955.java'
    before = hashlib.sha256(reference.read_bytes()).hexdigest()
    stubs = work / 'stubs'
    classes = work / 'classes'
    stubs.mkdir(exist_ok=True)
    classes.mkdir(exist_ok=True)
    stub = stubs / 'GpuSingle1960.java'
    stub.write_text('''package com.hiro.ulike;
final class GpuSingle1960 {
 static long workspaceBytes(int width,int rows){return 0;}
 static boolean process(int[] a,int[] b,int w,int r,int begin,int end,int lo,int hi,int y,int n,boolean s,SingleNoise1955.Model m,SingleNoise1955.Protection p){return false;}
}
''')
    def run(command, name):
        p = subprocess.run(list(map(str, command)), capture_output=True, text=True, timeout=360)
        (work / name).write_text(p.stdout + p.stderr)
        if p.returncode:
            raise RuntimeError(name + ': ' + p.stdout[-5000:] + p.stderr[-5000:])
        return p.stdout
    run([jdk / 'bin/javac', '-encoding', 'UTF-8', '-source', '8', '-target', '8', '-Xlint:-options',
         '-d', classes, reference, stub, root / 'tests1965/ModelOracle1965.java',
         root / 'tests1965/ModelRoundingOracle1965.java'], 'oracle-compile.log')
    oracle = work / 'oracle.bin'
    generated = json.loads(run([jdk / 'bin/java', '-Djava.library.path=' + str(work / 'unavailable'),
                               '-cp', classes, 'com.hiro.ulike.ModelOracle1965', oracle], 'oracle-run.log'))
    assert generated['records'] == 50 and not generated['referenceNativeEnabled']
    assert before == hashlib.sha256(reference.read_bytes()).hexdigest(), 'Frozen oracle changed'
    rounding_file=work/'rounding.bin'
    rounding_oracle=json.loads(run([jdk/'bin/java','-cp',classes,'com.hiro.ulike.ModelRoundingOracle1965',
        rounding_file],'rounding-oracle.log'))
    assert rounding_oracle['records']==43012 and rounding_oracle['naiveMismatches']>0

    driver = (root / 'native1949/host_gpu1949.py').read_text()
    driver = driver.replace('self.capacities=[0]*5', 'self.capacities=[0]*6')
    driver = driver.replace('(C.c_uint*5)()', '(C.c_uint*6)()')
    driver = driver.replace("['glGenBuffers'](5,self.buffer_array)", "['glGenBuffers'](6,self.buffer_array)")
    driver = driver.replace("['glDeleteBuffers'](5,self.buffer_array)", "['glDeleteBuffers'](6,self.buffer_array)")
    driver = driver.replace("['fusion_sums1949','pack_rgb1949','residual1949']", "[]")
    namespace = {'__name__': 'gpu_model_test'}
    exec(compile(driver, 'host_gpu1949.py', 'exec'), namespace)
    # Verify rejection is produced by compiling the unchanged production GLES
    # shader, not by replacing its compute body with a host-only result.
    gles = namespace['SoftwareGpu'](root / 'native1960')
    gles_report = {'renderer': gles.renderer, 'version': gles.version}
    try:
        try:
            program = gles.compile('model1965')
            gles.functions['glDeleteProgram'](program)
            gles_report['fp64_shader_supported'] = True
        except RuntimeError as error:
            message = str(error)
            assert 'GL_EXT_shader_explicit_arithmetic_types_float64' in message, message
            gles_report.update(fp64_shader_supported=False, unsupported_precision_rejected=True,
                               compiler_error=message)
    finally:
        gles.close()

    # Positive coverage uses the exact portable production GLES kernels and
    # library expansion also used by the native shader builder. No dialect
    # header adaptation, FP32 coefficient substitute or CPU preparation.
    required = driver.replace('self.programs=[self.compile(name) for name in []]',
                              "self.programs=[self.compile(name) for name in ['model1965_soft','single1965_soft','model1965_round_test']]")
    library = (root / 'native1960/soft64_1965.glsl').read_text()
    for name in ('model1965_soft', 'single1965_soft'):
        source = (root / 'native1960' / (name + '.comp')).read_text()
        assert '/* SOFT64_1965_LIBRARY */' in source
        source = source.replace('/* SOFT64_1965_LIBRARY */', library)
        (work / (name + '.comp')).write_text(source)
    # Test entry calls the unchanged production rounding function with focused
    # binary64 words. The normal model and Single bodies above also execute
    # unchanged for all 50 image fixtures; this is additional unit coverage.
    focused=(work/'model1965_soft.comp').read_text().replace('void main() {','void originalModelMain() {',1)
    focused+='''\nvoid main(){int id=u[31]+int(gl_GlobalInvocationID.x);if(id>=u[29])return;
model[id]=intBitsToFloat(roundPositive(uvec2(pixels[id*2],pixels[id*2+1])));}\n'''
    (work/'model1965_round_test.comp').write_text(focused)
    namespace = {'__name__': 'gpu_model_required_gles_test'}
    exec(compile(required, 'host_gpu1949.py', 'exec'), namespace)
    gpu = namespace['SoftwareGpu'](work)
    gpu.gl('glUniform1iv', None, [C.c_int, C.c_int, C.POINTER(C.c_int)])
    fn = gpu.functions
    data = oracle.read_bytes()
    offset = 0
    def integer():
        nonlocal offset
        value = struct.unpack_from('>i', data, offset)[0]
        offset += 4
        return value
    def utf():
        nonlocal offset
        length = struct.unpack_from('>H', data, offset)[0]
        offset += 2
        value = data[offset:offset+length].decode()
        offset += length
        return value
    def words():
        nonlocal offset
        count = integer()
        value = list(struct.unpack_from('>' + str(count) + 'i', data, offset))
        offset += count * 4
        return value
    def array(values):
        return (C.c_int32 * len(values))(*values)
    def dispatch(program, u, count):
        fn['glUseProgram'](gpu.programs[program])
        location = fn['glGetUniformLocation'](gpu.programs[program], b'u[0]')
        fn['glUniform1iv'](location, 32, (C.c_int * 32)(*u))
        fn['glDispatchCompute']((count + 63) // 64, 1, 1)
        fn['glMemoryBarrier'](0x2000)
    assert integer() == 1965001
    started = time.monotonic()
    cases = pixels = assertions = 0
    errors = []
    try:
        while True:
            name = utf()
            if not name:
                break
            width,height,columns,rows,pw,ph = [integer() for _ in range(6)]
            samples,expected = words(),words()
            noise,shadows = integer(),integer()
            source,expected_pixels = words(),words()
            cells,blocks = columns*rows,(pw//8)*(ph//8)
            model_count = cells*3+88
            gpu.upload(0,array(samples))
            gpu.upload(1,None,cells*2048*4)
            gpu.upload(2,None,72*1024*4)
            gpu.upload(3,None,max(1,cells*blocks)*8)
            gpu.upload(4,None,max(1,cells*blocks)*4)
            gpu.upload(5,None,(model_count+3)*4)
            u = [0]*32
            u[1:9] = [width,height,columns,rows,pw,ph,pw//8,ph//8]
            dispatch(0,u,max(cells*2048,72*1024,cells*blocks))
            if blocks:
                u[0]=1
                dispatch(0,u,cells*blocks)
            u[0]=2
            dispatch(0,u,cells)
            u[0]=3
            dispatch(0,u,1)
            gpu.fence()
            got = gpu.read(5,model_count+3)
            mismatch = [(i,a,b) for i,(a,b) in enumerate(zip(got,expected)) if a!=b]
            if mismatch:
                errors.append({'case': name, 'stage': 'model', 'count': len(mismatch), 'first': mismatch[:4]})
            assertions += model_count+3
            if source:
                first_by = ((-7)//4)*4
                block_columns = (width+3)//4+1
                block_rows = (height-first_by+3)//4
                gpu.upload(0,array(source))
                gpu.upload(1,None,len(source)*4)
                gpu.upload(2,array(got[:model_count]))
                gpu.upload(3,array([0]))
                gpu.upload(4,None,block_columns*block_rows*992)
                u=[0]*32
                u[1:19]=[width,height,height,0,height,0,0,height,columns,rows,pw,ph,noise,shadows,0,first_by,block_columns,block_rows]
                dispatch(1,u,block_columns*block_rows)
                u[0]=1
                dispatch(1,u,len(source))
                gpu.fence()
                candidate=gpu.read(1,len(source))
                mismatch=[(i,a,b) for i,(a,b) in enumerate(zip(candidate,expected_pixels)) if a!=b]
                if mismatch:
                    errors.append({'case':name,'stage':'pixels','count':len(mismatch),'first':mismatch[:4]})
                pixels+=len(source)
                assertions+=len(source)
            cases+=1
        assert offset==len(data)
        assert cases==50 and pixels>20000
        assert not errors,str(errors)
        rounding_data=rounding_file.read_bytes()
        magic,rounding_count=struct.unpack_from('>ii',rounding_data)
        assert magic==196548 and rounding_count==43012
        raw=[];round_expected=[]
        for case in range(rounding_count):
            lo,hi,expected=struct.unpack_from('>iii',rounding_data,8+case*12)
            raw.extend((lo,hi));round_expected.append(expected)
        gpu.upload(0,array(raw));gpu.upload(5,None,rounding_count*4)
        u=[0]*32;u[29]=rounding_count
        dispatch(2,u,rounding_count);gpu.fence();round_got=gpu.read(5,rounding_count)
        round_errors=[(case,got,expected) for case,(got,expected) in enumerate(zip(round_got,round_expected)) if got!=expected]
        assert not round_errors,str(round_errors[:10])
        assertions+=rounding_count
        assert pins == {str(path.relative_to(root)):hashlib.sha256(path.read_bytes()).hexdigest() for path in tracked}, 'Production shader changed during execution; rerun required'
        report={'schema':'ulike-gpu-noise-model1965-v1','status':'passed','assertions':assertions,
                'cases':cases,'exact_output_pixels':pixels,'exact_model_float_words':True,
                'binary64_rounding_cases':rounding_count,'naive_rounding_regressions':rounding_oracle['naiveMismatches'],
                'gles':gles_report,'required_renderer':gpu.renderer,'required_gles_version':gpu.version,
                'seconds':round(time.monotonic()-started,3),'host_shader_body_unchanged':True,
                'desktop_header_only_adaptation':False,'original_gles_required_math_executed':True,
                'software_ieee_binary64_preserved':True,'hardware_fp64_required':False,
                'live_cpu_qualification_used':False,'physical_android_tested':False,
                'frozen_reference_sha256':before,'oracle_sha256':hashlib.sha256(data).hexdigest(),
                'sources':pins}
        (work/'result.json').write_text(json.dumps(report,indent=2)+'\n')
        return report
    finally:
        gpu.close()


if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser()
    parser.add_argument('--root',default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--work',required=True)
    parser.add_argument('--jdk')
    args=parser.parse_args()
    print(json.dumps(test(args.root,args.work,args.jdk),indent=2))
