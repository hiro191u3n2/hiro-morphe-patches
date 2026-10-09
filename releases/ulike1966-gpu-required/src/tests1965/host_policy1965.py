"""Actual host GPU policy outputs against isolated frozen CPU oracle."""
from pathlib import Path
import ctypes as C,hashlib,json,os,struct,subprocess,time

def test(root,work,jdk=None,ndk=None):
    os.environ['LP_DEBUG']='noopt'  # Only the CPU LLVM backend optimizer; production GLES math unchanged.
    root=Path(root).resolve();work=Path(work).resolve()/'host-policy1965';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);classes=work/'classes';classes.mkdir(exist_ok=True)
    refs=root/'tests1965/policy-reference'
    files=[*refs.glob('*.java'),root/'tests1965/Policy1965Oracle.java']
    subprocess.run([str(jdk/'bin/javac'),'-d',str(classes),*map(str,files)],check=True,capture_output=True)
    oracle=work/'oracle.bin';subprocess.run([str(jdk/'bin/java'),'-cp',str(classes),'com.hiro.ulike.Policy1965Oracle',str(oracle)],check=True,capture_output=True)
    host=(root/'native1949/host_gpu1949.py').read_text().replace("['fusion_sums1949','pack_rgb1949','residual1949']","['policy1965_soft0','policy1965_soft1','policy1965_soft2','policy1965_soft3','policy1965_soft4','policy1965_soft5']").replace(']*5',']*7').replace('(C.c_uint*5)','(C.c_uint*7)').replace("['glGenBuffers'](5,","['glGenBuffers'](7,").replace("['glDeleteBuffers'](5,","['glDeleteBuffers'](7,").replace('2000000000','60000000000')
    shader=(root/'native1960/policy1965_soft.comp').read_text().replace('/* SOFT64_1965_LIBRARY */',(root/'native1960/soft64_1965.glsl').read_text())
    for mode in range(6):
        (work/('policy1965_soft'+str(mode)+'.comp')).write_text(shader.replace('#version 310 es','#version 310 es\n#define POLICY_MODE1965 '+str(mode),1))
    ns={'__name__':'policy1965_gpu'};exec(compile(host,'host_gpu1949.py','exec'),ns);gpu=ns['SoftwareGpu'](work)
    gpu.gl('glUniform1iv',None,[C.c_int,C.c_int,C.POINTER(C.c_int)])
    fn=gpu.functions;program=gpu.programs[0];fn['glUseProgram'](program);loc=fn['glGetUniformLocation'](program,b'u[0]')
    blob=oracle.read_bytes();offset=0
    def integer():
        nonlocal offset
        v=struct.unpack_from('>i',blob,offset)[0];offset+=4;return v
    def utf():
        nonlocal offset
        n=struct.unpack_from('>H',blob,offset)[0];offset+=2;s=blob[offset:offset+n].decode();offset+=n;return s
    def words():
        nonlocal offset
        n=integer();v=list(struct.unpack_from('>'+str(n)+'i',blob,offset));offset+=n*4;return v
    assert integer()==1965001
    cases=values=0;errors=[];start=time.time()
    while True:
        name=utf()
        if not name:break
        u,m,src,raster,grid,smooth,expected,sigma=[words() for _ in range(8)]
        buffers=[src,[0]*len(expected),m,raster,grid,smooth,[0]*max(1,u[30])]
        for slot,data in enumerate(buffers):gpu.upload(slot,(C.c_int32*len(data))(*data))
        if u[0]==0:
            pu=u[:];pu[0]=4;program=gpu.programs[4];fn['glUseProgram'](program);ploc=fn['glGetUniformLocation'](program,b'u[0]');fn['glUniform1iv'](ploc,32,(C.c_int*32)(*pu));fn['glDispatchCompute']((u[29]+63)//64,1,1);fn['glMemoryBarrier'](0x2000)
        if u[0] in (1,2):
            pu=u[:];pu[0]=5;program=gpu.programs[5];fn['glUseProgram'](program);ploc=fn['glGetUniformLocation'](program,b'u[0]');fn['glUniform1iv'](ploc,32,(C.c_int*32)(*pu));fn['glDispatchCompute'](1,1,1);fn['glMemoryBarrier'](0x2000)
        program=gpu.programs[u[0]];fn['glUseProgram'](program);loc=fn['glGetUniformLocation'](program,b'u[0]');fn['glUniform1iv'](loc,32,(C.c_int*32)(*u));fn['glDispatchCompute']((u[30]+63)//64,1,1);gpu.fence();actual=gpu.read(1,len(expected))
        mismatches=[(i,a,b) for i,(a,b) in enumerate(zip(actual,expected)) if a!=b]
        if sigma:
            got=gpu.read(6,len(sigma));mismatches += [(i,a,b) for i,(a,b) in enumerate(zip(got,sigma)) if a!=b]
        if mismatches:errors.append({'name':name,'count':len(mismatches),'first':mismatches[:3]})
        cases+=1;values+=len(expected)+len(sigma)
    gpu.close()
    report={'status':'passed' if not errors else 'failed','assertions':values,'cases':cases,'errors':errors,'renderer':gpu.renderer,'version':gpu.version,'seconds':time.time()-start,'gpu_shader_execution_on_host':True,'physical_android_tested':False,'production_fp64_gles_confirmed':False,'header_only_adaptation':False,'original_gles_required_math_executed':True,'software_binary64_gpu':True,'host_llvm_optimization_disabled':True,'production_shader_sha256':hashlib.sha256((root/'native1960/policy1965_soft.comp').read_bytes()).hexdigest(),'frozen_reference_sha256':{f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in refs.glob('*.java')}}
    report['software_binary64_library_sha256']=hashlib.sha256((root/'native1960/soft64_1965.glsl').read_bytes()).hexdigest()
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');assert cases==77;assert not errors,str(errors);return report
