#!/usr/bin/env python3
"""Prove finite RNE division, execute actual private JNI, and preserve old GLSL.

The hardware divisor is only a quotient guess; deliberate guess perturbations
exercise both integer correction and the exact long-division fallback. Android
hardware and the cause of the reported Galaxy mismatch remain unverified.
"""
from pathlib import Path
import hashlib,importlib.util,json,os,re,shutil,subprocess

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    local=Path(__file__).parent;fixtures=local/'precision73-fixtures'
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve();ndk=Path(ndk or os.environ['ULIKE_NDK_HOME']).resolve()
    if '27.2.12479018' not in (ndk/'source.properties').read_text():raise AssertionError('Pinned NDK required')
    baseline=source if (source/'native1960/build_native1960.py').is_file() else local.parent/'baseline72/src'
    def run(cmd,name,env=None,timeout=180):
        p=subprocess.run(list(map(str,cmd)),text=True,capture_output=True,env=env,timeout=timeout)
        (work/(name+'.log')).write_text(p.stdout+p.stderr)
        if p.returncode or re.search(r'WARNING in native method|FATAL ERROR in native method',p.stdout+p.stderr):raise RuntimeError(name+' failed or JNI checker warning\n'+p.stdout[-6000:]+p.stderr[-6000:])
        return p.stdout
    inputs=[source/'GpuNoise1960.java',source/'native1960/engine1960.c',source/'native1960/strong1960.comp'];before={str(p.relative_to(source)):sha(p) for p in inputs}
    helper=fixtures/'ieee_div1973.glsl';helperText=helper.read_text()
    if helperText not in (source/'native1960/strong1960.comp').read_text():raise AssertionError('Proof helper is not verbatim production helper')
    reports={};legacy=fixtures/'strong1960_72.comp'
    def preprocess(path,mode,tile,size):
        content=path.read_text();content='\n'.join(x for x in content.splitlines() if not x.startswith(('#version','#extension')))
        f=work/'preprocess.comp';f.write_text(content)
        output=run(['cc','-E','-P','-x','c','-DGX_IEEE_DIV73=0','-DGX_STRONG_MODE='+str(mode),'-DGX_TILE_WIDTH='+str(tile),'-DGX_LOCAL_SIZE='+str(size),f],'legacy-preprocess')
        return re.sub(r'\s+','',output)
    for size in (64,32,128):
        for mode in (-1,0,1,2,3):
            tile=0 if mode==-1 else 8
            if preprocess(legacy,mode,tile,size)!=preprocess(source/'native1960/strong1960.comp',mode,tile,size):raise AssertionError('Legacy Strong shader changed')
    reports['legacy_glsl_tokens']={'status':'passed','assertions':15,'sha256':sha(legacy)}
    cpu=work/'cpu';cpu.mkdir(exist_ok=True)
    translated=re.sub(r'\b(uint|int|float)\(',lambda m:'('+m.group(1)+')(',helperText)
    (cpu/'ieee_div1973_c.h').write_text('#include <stdbool.h>\n'+translated)
    for arch,compiler,prefix in [('x86','cc',[]),('aarch64','aarch64-linux-gnu-gcc',['qemu-aarch64'])]:
        if not shutil.which(compiler) or prefix and not shutil.which(prefix[0]):raise RuntimeError('Required ARM64 proof tool missing: '+compiler)
        for perturb in (0,-8,8,100):
            binary=cpu/(arch+str(perturb));name='cpu-'+arch+'-'+str(perturb)
            flags=['-std=c11','-O2','-fno-fast-math','-ffp-contract=off','-frounding-math','-Wall','-Wextra','-Werror','-I'+str(cpu),'-DGX_DIV_GUESS_PERTURB73='+str(perturb)]
            if arch=='aarch64':flags+=['-static']
            run([compiler,*flags,fixtures/'div_cpu1973.c','-lm','-o',binary],name+'-compile')
            reports[name]=json.loads(run([*prefix,binary],name).strip().splitlines()[-1])
    overlay=work/'source';native=overlay/'native1960';shutil.copytree(baseline/'native1960',native,dirs_exist_ok=True)
    shutil.copy2(source/'native1960/engine1960.c',native/'engine1960.c');shutil.copy2(source/'native1960/strong1960.comp',native/'strong1960.comp');shutil.copy2(source/'GpuNoise1960.java',overlay/'GpuNoise1960.java')
    # Raw uint operands/results avoid incidental subnormal flushing by SSBO float
    # loads. The helper is unchanged; only its hardware guess is perturbed.
    probe='''#version 310 es
#extension GL_EXT_gpu_shader5 : require
precision highp float;precision highp int;
#ifndef GX_LOCAL_SIZE
#define GX_LOCAL_SIZE 64
#endif
layout(local_size_x=GX_LOCAL_SIZE) in;
layout(std430,binding=0) readonly buffer A { uint pairs[]; };
layout(std430,binding=1) writeonly buffer B { uint result[]; };
uniform int u[32];uniform float f[32];
#define GX_DIV_GUESS_PERTURB73 u[1]
'''+helperText+'''
void main(){int at=int(gl_GlobalInvocationID.x)+u[31];if(at>=u[0])return;result[at]=ieeeDivBits1973(pairs[at*2],pairs[at*2+1]);}
'''
    (native/'analysis1960.comp').write_text(probe)
    spec=importlib.util.spec_from_file_location('builder1973',native/'build_native1960.py');builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder);builder.shader_header(native,work)
    headers=work/'headers';sysroot=ndk/'toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include'
    for name in ('EGL','GLES3','KHR'):shutil.copytree(sysroot/name,headers/name,dirs_exist_ok=True)
    includes=['-I'+str(headers),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(work),'-I'+str(native)]
    library=work/'libulike_gpu1960.so'
    run(['cc','-std=c11','-O3','-shared','-fPIC','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-DANDROID',*includes,fixtures/'div_gpu1973.c','-Wl,--no-undefined','-l:libEGL.so.1','-l:libGL.so.1','-lm','-o',library],'gpu-native-compile')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,source/'GpuNoise1960.java',fixtures/'DivisionGpu1973Test.java'],'gpu-java-compile')
    environment=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    reports['actual_gpu_bits_and_warm_context']=json.loads(run([jdk/'bin/java','-ea','-Xcheck:jni','-Xmx1024m','-Djava.library.path='+str(work),'-cp',classes,'com.hiro.ulike.DivisionGpu1973Test'],'gpu-divide',env=environment).strip().splitlines()[-1])
    if before!={str(p.relative_to(source)):sha(p) for p in inputs}:raise AssertionError('Production sources changed during proof')
    result={'status':'passed','assertions':sum(x.get('assertions',0)+x.get('cases',0) for x in reports.values()),'tests':reports,'exact_division_regressions_passed':True,'legacy_program_semantics_preserved':True,'gpu_safety_gates_preserved':True,'physical_android_tested':False,'galaxy_mismatch_cause_proven':False,'finite_binary32_rounding':'nearest ties-even','nan_payload_contract':'not portable; quiet NaN class only','hardware_divide_is_untrusted_guess':True,'actual_production_java_and_jni_executed':True,'production_source_sha256':before,'runner_source_sha256':sha(__file__),'fixture_source_sha256':{p.name:sha(p) for p in fixtures.iterdir() if p.is_file()},'host_native_sha256':sha(library),'ndk_revision':'27.2.12479018'}
    (work/'precision-host-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
