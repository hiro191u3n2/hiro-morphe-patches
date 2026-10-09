#!/usr/bin/env python3
"""Actual original GLES shot policy vs frozen published Java policy, bit exact."""
from pathlib import Path
import ctypes as C
import hashlib,json,os,struct,subprocess,time

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'plan1965';work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME','/usr/lib/jvm/java-17-openjdk-amd64'))
    reference=root/'tests1964/published1963-reference/QualityPixels1932.java'
    tracked=[reference,root/'native1960/plan1965_soft.comp',root/'native1960/soft64_1965.glsl']
    pins={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    stubs=work/'stubs';stubs.mkdir(exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
    sources={
      'PolicyCache1945.java':'final class PolicyCache1945 {QualityPixels1932.Plan owner;float sigmaAt(int x,int y){return 0;}int skinQ8(int x,int y){return 0;}int detailQ8(int x,int y){return 0;}}',
      'SpatialNoise1934.java':'final class SpatialNoise1934 {float sigmaAt(int x,int y){return 0;}int budgetQ8(int x,int y){return 256;}}',
      'SpeedWorkers1935.java':'final class SpeedWorkers1935 {static int[] borrowInts(int n){return new int[n];}static float[] borrowFloats(int n){return new float[n];}static void release(Object a){}static void trim(){}}',
      'NativeMoire1951.java':'final class NativeMoire1951 {static boolean run(int[] a,int[] b,int w,int h,int lo,int hi,QualityPixels1932.Plan p,boolean m,boolean s,int y){return false;}}',
      'LongMoire1934.java':'final class LongMoire1934 {static int correct(int[] a,int i,int w,int p){return p;}}',
      'QualityShadow1932.java':'final class QualityShadow1932 {static final int RESIDUAL_RADIUS=3;static int textureQ8(int[] a,int w,int h,int x,int y,int lo,int hi,float s,int r){return 0;}}'}
    for name,body in sources.items():(stubs/name).write_text('package com.hiro.ulike;\n'+body+'\n')
    def run(command,name):
        p=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=360)
        (work/name).write_text(p.stdout+p.stderr)
        if p.returncode:raise RuntimeError(name+': '+p.stdout[-5000:]+p.stderr[-5000:])
        return p.stdout
    run([jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-d',classes,reference,
        *sorted(stubs.glob('*.java')),root/'tests1965/PlanOracle1965.java'],'oracle-compile.log')
    oracle=work/'oracle.bin';generated=json.loads(run([jdk/'bin/java','-cp',classes,
        'com.hiro.ulike.PlanOracle1965',oracle],'oracle-run.log'))
    assert generated['records']==20000
    source=(root/'native1960/plan1965_soft.comp').read_text().replace('/* SOFT64_1965_LIBRARY */',
        (root/'native1960/soft64_1965.glsl').read_text());(work/'plan1965_soft.comp').write_text(source)
    driver=(root/'native1949/host_gpu1949.py').read_text().replace("['fusion_sums1949','pack_rgb1949','residual1949']","['plan1965_soft']")
    ns={'__name__':'plan1965_gpu_test'};exec(compile(driver,'host_gpu1949.py','exec'),ns);gpu=ns['SoftwareGpu'](work)
    gpu.gl('glUniform1iv',None,[C.c_int,C.c_int,C.POINTER(C.c_int)])
    gpu.gl('glUniform1fv',None,[C.c_int,C.c_int,C.POINTER(C.c_float)])
    blob=oracle.read_bytes();magic,count=struct.unpack_from('>ii',blob);assert magic==196547
    offset=8;assert len(blob)==8+count*45*4
    fn=gpu.functions;program=gpu.programs[0];fn['glUseProgram'](program)
    ul=fn['glGetUniformLocation'](program,b'u[0]');fl=fn['glGetUniformLocation'](program,b'f[0]')
    gpu.upload(0,None,36);started=time.monotonic();errors=[]
    try:
        for case in range(count):
            u=struct.unpack_from('>32i',blob,offset);offset+=128
            fbits=struct.unpack_from('>4I',blob,offset);offset+=16
            f=list(struct.unpack('>4f',struct.pack('>4I',*fbits)))+[0.0]*28
            expected=list(struct.unpack_from('>9i',blob,offset));offset+=36
            fn['glUniform1iv'](ul,32,(C.c_int*32)(*u));fn['glUniform1fv'](fl,32,(C.c_float*32)(*f))
            fn['glDispatchCompute'](1,1,1);gpu.fence();got=gpu.read(0,9)
            if got!=expected:
                errors.append({'case':case,'u':u[:8],'fbits':fbits,'got':got,'expected':expected})
                if len(errors)>=10:break
        assert not errors,json.dumps(errors)
        assert pins=={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked},'Production changed during test; rerun'
        report={'schema':'ulike-gpu-shot-plan1965-v1','status':'passed','assertions':count*9,'cases':count,
            'original_gles_required_math_executed':True,'exact_policy_words':True,'shader_header_adapted':False,
            'software_binary64':True,'renderer':gpu.renderer,'version':gpu.version,
            'seconds':round(time.monotonic()-started,3),'physical_android_tested':False,
            'oracle_sha256':hashlib.sha256(blob).hexdigest(),'sources':pins}
        (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
    finally:gpu.close()

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',default=Path(__file__).resolve().parent.parent)
    p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args()
    print(json.dumps(test(a.root,a.work,a.jdk),indent=2))
