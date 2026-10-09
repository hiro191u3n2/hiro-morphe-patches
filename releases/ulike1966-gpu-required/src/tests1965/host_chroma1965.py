#!/usr/bin/env python3
"""Unmodified GLES Chroma vs retained .64 production DEX instruction oracle."""
from pathlib import Path
import ctypes as C, hashlib, json, os, struct, subprocess, time

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'chroma1965';work.mkdir(parents=True,exist_ok=True)
    toolchain=Path(os.environ.get('ULIKE_TOOLCHAIN1965',str(root.parents[3]/'toolchain1965')))
    jdk=Path(jdk or toolchain/'jdk-21.0.8+9');dex=toolchain/'runtime1964.dex';jar=toolchain/'tools/morphe.jar'
    assert dex.is_file() and jar.is_file(), 'Published .64 DEX and dexlib2 required'
    files=[root/'native1960/chroma1965_soft.comp',root/'native1960/soft64_1965.glsl',
           root/'tests1965/ChromaDexOracle1965.java',root/'tests1965/ChromaOracle1965.java',dex]
    pins={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
    def run(command,name):
        p=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=360)
        (work/name).write_text(p.stdout+p.stderr)
        if p.returncode:raise RuntimeError(name+': '+p.stdout[-3000:]+p.stderr[-6000:])
        return p.stdout
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','-encoding','UTF-8','-cp',jar,'-d',classes,files[2],files[3]],'oracle-compile.log')
    oracle=work/'oracle.bin'
    generated=json.loads(run([jdk/'bin/java','-cp',str(classes)+os.pathsep+str(jar),
                              'com.hiro.ulike.ChromaOracle1965',dex,oracle],'oracle-run.log'))
    assert generated['actual_dex_oracle'] and generated['changed']>0
    library=files[1].read_text();shader=files[0].read_text()
    assert shader.count('/* SOFT64_1965_LIBRARY */')==1 and shader.startswith('#version 310 es')
    (work/'chroma1965_soft.comp').write_text(shader.replace('/* SOFT64_1965_LIBRARY */',library))
    driver=(root/'native1949/host_gpu1949.py').read_text().replace(
        "['fusion_sums1949','pack_rgb1949','residual1949']", "['chroma1965_soft']")
    ns={'__name__':'actual_gles_chroma1965'};exec(compile(driver,'host_gpu1949.py','exec'),ns)
    gpu=ns['SoftwareGpu'](work);gpu.gl('glUniform1iv',None,[C.c_int,C.c_int,C.POINTER(C.c_int)])
    gpu.gl('glUniform1fv',None,[C.c_int,C.c_int,C.POINTER(C.c_float)])
    fn=gpu.functions;data=oracle.read_bytes();offset=0
    def integer():
        nonlocal offset
        v=struct.unpack_from('>i',data,offset)[0];offset+=4;return v
    def utf():
        nonlocal offset
        n=struct.unpack_from('>H',data,offset)[0];offset+=2;v=data[offset:offset+n].decode();offset+=n;return v
    def words():
        nonlocal offset
        n=integer();v=list(struct.unpack_from('>'+str(n)+'i',data,offset));offset+=n*4;return v
    def array(v):return (C.c_int32*len(v))(*v)
    assert integer()==1965002
    errors=[];records=[];pixels=changed=0;started=time.monotonic()
    try:
        fn['glUseProgram'](gpu.programs[0])
        ui=fn['glGetUniformLocation'](gpu.programs[0],b'u[0]');fi=fn['glGetUniformLocation'](gpu.programs[0],b'f[0]')
        fn['glUniform1fv'](fi,32,(C.c_float*32)(*([0.]*32)))
        while True:
            name=utf()
            if not name:break
            w,h,begin,end,radius,expected_changed=[integer() for _ in range(6)]
            source,wanted=words(),words();count=w*(end-begin)
            gpu.upload(0,array(source));gpu.upload(1,None,max(1,count)*36);gpu.upload(2,None,max(1,count)*4)
            u=[0]*32;u[1:6]=[w,h,begin,end,radius]
            if count:
                fn['glUniform1iv'](ui,32,(C.c_int32*32)(*u));fn['glDispatchCompute']((count+63)//64,1,1);fn['glMemoryBarrier'](0x2000)
                u[0]=1;fn['glUniform1iv'](ui,32,(C.c_int32*32)(*u));fn['glDispatchCompute']((count+63)//64,1,1);gpu.fence()
                candidate=gpu.read(2,count)
            else:candidate=[]
            band=wanted[begin*w:end*w];input_band=source[begin*w:end*w]
            mismatch=[(i,a,b) for i,(a,b) in enumerate(zip(candidate,band)) if a!=b]
            if mismatch:errors.append({'case':name,'mismatches':len(mismatch),'first':mismatch[:8]})
            actual_changed=sum(a!=b for a,b in zip(candidate,input_band))
            if actual_changed!=expected_changed:errors.append({'case':name,'changed_expected':expected_changed,'changed_actual':actual_changed})
            records.append({'name':name,'width':w,'height':h,'begin':begin,'end':end,'radius':radius,'pixels':count,'changed_pixels':actual_changed})
            pixels+=count;changed+=actual_changed
        assert offset==len(data) and len(records)==generated['fixtures']
        assert pixels==generated['pixels'] and changed>0
        assert pins=={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}, 'Sources changed while testing; rerun required'
        report={'schema':'ulike-original-chroma-gpu1965-v1','status':'passed' if not errors else 'failed',
                'assertions':pixels+len(records),
                'cases':len(records),'exact_output_pixels':pixels,'actually_corrected_pixels':changed,'errors':errors,
                'records':records,'actual_published_dex_oracle':True,'published_runtime_dex_sha256':pins[str(dex)],
                'oracle_sha256':hashlib.sha256(data).hexdigest(),'original_gles_required_math_executed':True,
                'host_shader_body_unchanged':True,'desktop_header_only_adaptation':False,
                'software_ieee_binary64_preserved':True,'hardware_fp64_required':False,'live_cpu_qualification_used':False,
                'physical_android_tested':False,'renderer':gpu.renderer,'gles_version':gpu.version,
                'seconds':round(time.monotonic()-started,3),'sources':pins}
        (work/'result.json').write_text(json.dumps(report,indent=2)+'\n')
        assert not errors,str(errors)
        return report
    finally:gpu.close()

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',default=Path(__file__).resolve().parents[1]);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args()
    print(json.dumps(test(a.root,a.work,a.jdk),indent=2))
