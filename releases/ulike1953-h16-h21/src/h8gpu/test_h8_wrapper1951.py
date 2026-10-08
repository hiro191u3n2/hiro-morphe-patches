#!/usr/bin/env python3
"""Execute the actual pinned filter/stage/packing/sharpening DEX with H8 wiring."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,zipfile

BASELINE_SHA='ca872a0b6df5f1ca5a949eac0f23d6cba97c7ce662ec7257f4445ba39302a490'

def run(cmd,log):
    p=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,timeout=240)
    log.write_text(p.stdout+p.stderr)
    if p.returncode:raise RuntimeError(p.stdout[-2000:]+p.stderr[-7000:])
    return p.stdout

def test(source,work,baseline):
    source,work,baseline=Path(source),Path(work)/'h8-wrapper1951',Path(baseline)
    work.mkdir(parents=True,exist_ok=True)
    if hashlib.sha256(baseline.read_bytes()).hexdigest()!=BASELINE_SHA:raise RuntimeError('Pinned H8 production baseline differs')
    with zipfile.ZipFile(baseline) as z:runtime=z.read('ulike/runtime.dex')
    dex=work/'baseline-runtime.dex';dex.write_bytes(runtime)
    compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
    tool=os.environ.get('ULIKE_MORPHE_JAR')
    if not compiler or not tool:raise RuntimeError('H8 wrapper needs the reviewed JDK compiler and Morphe jar')
    javac=Path(compiler)
    java=Path(os.environ.get('ULIKE_JAVA') or shutil.which('java') or str(javac.with_name('java')))
    jdk=Path(os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME') or javac.resolve().parent.parent)
    tools=Path(tool)
    cc=os.environ.get('CC') or shutil.which('cc')
    lib=work/'libulike_core1950.so'
    run([cc,'-std=c11','-O2','-shared','-fPIC','-Wall','-Wextra','-Werror','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),source/'h8gpu/wrapper_endpoint1951.c','-o',lib],work/'native-compile.log')
    shutil.copyfile(lib,work/'libulike_h8gpu1951.so')
    classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=[source/'CorePixels1950.java',source/'h8gpu/H8DexOracle1951.java',source/'h8gpu/H8Wrapper1951Test.java',source/'tests/core1950/com/hiro/ulike/DetailPixels.java',source/'tests/core1950/com/hiro/ulike/SpeedWorkers1935.java',source/'h8gpu/fixtures/com/hiro/ulike/DetailSerial186.java']
    run([javac,'-source','8','-target','8','-Xlint:-options','-cp',tools,'-d',classes,*sources],work/'compile.log')
    result=json.loads(run([java,'-Xcheck:jni','-Djava.library.path='+str(work),'-cp',str(classes)+os.pathsep+str(tools),'com.hiro.ulike.H8Wrapper1951Test',dex],work/'test.log'))
    for key in ('stage_parameter_order_verified','sharpening_halo_pixel_exact','production_filter_dex_oracle_executed','stage_0_1_suppressed','stage_2_3_preserved'):
        if result.get(key) is not True:raise RuntimeError('Missing H8 full-filter evidence '+key)
    # Mutation controls ensure this suite rejects both defects it protects:
    # wrong phase position repeats CPU denoise, while missing sharpening halos
    # changes actual final pixels even with the GPU arithmetic kept exact.
    current=(source/'CorePixels1950.java').read_text()
    phase='public static void stage(DetailSerial186.Context context,int phase,int first,int last)'
    forward='DetailSerial186.stageBeforeH8(context,phase,first,last);'
    if current.count(phase)!=1 or current.count(forward)!=1:raise RuntimeError('Reviewed stage control source differs')
    old_phase=current.replace(phase,'public static void stage(DetailSerial186.Context context,int first,int last,int phase)').replace(forward,'DetailSerial186.stageBeforeH8(context,first,last,phase);')
    first='int denoiseFirst=sharp>0?Math.max(0,first-4):first;'
    last='int denoiseLast=sharp>0?(int)Math.min((long)rows,last+4):(int)last;'
    if current.count(first)!=1 or current.count(last)!=1:raise RuntimeError('Reviewed halo control source differs')
    old_halo=current.replace(first,'int denoiseFirst=first;').replace(last,'int denoiseLast=(int)last;')
    controls={}
    for name,content,marker in (('old_stage_order',old_phase,'phase argument first: CPU denoise suppressed'),('old_sharpening_halo',old_halo,'production filter differs')):
        folder=work/name;folder.mkdir(exist_ok=True)
        core=folder/'CorePixels1950.java';core.write_text(content)
        negative_sources=[core]
        if name=='old_sharpening_halo':
            wrapper=(source/'h8gpu/H8Wrapper1951Test.java').read_text()
            wrapper='\n'.join(line for line in wrapper.splitlines() if 'production sharpening top halo' not in line and 'production sharpening bottom halo' not in line)+'\n'
            fixture=folder/'H8Wrapper1951Test.java';fixture.write_text(wrapper);negative_sources.append(fixture)
        negative_classes=folder/'classes';negative_classes.mkdir(exist_ok=True)
        run([javac,'-source','8','-target','8','-Xlint:-options','-cp',str(classes)+os.pathsep+str(tools),'-d',negative_classes,*negative_sources],folder/'compile.log')
        p=subprocess.run([str(java),'-Xcheck:jni','-Djava.library.path='+str(work),'-cp',str(negative_classes)+os.pathsep+str(classes)+os.pathsep+str(tools),'com.hiro.ulike.H8Wrapper1951Test',str(dex)],capture_output=True,text=True,timeout=240)
        (folder/'test.log').write_text(p.stdout+p.stderr)
        if p.returncode==0 or marker not in p.stderr:raise RuntimeError('H8 negative control failed: '+name+'\n'+p.stdout+p.stderr)
        controls[name]={'rejected':True,'expected_failure':marker}
    result['negative_controls']=controls
    result['assertions']+=len(controls)
    result.update(baseline_sha256=BASELINE_SHA,production_runtime_sha256=hashlib.sha256(runtime).hexdigest(),fixture_scope='Actual pinned production DEX filter, Context, stage, packing and sharpening execute in host VM. GPU JNI endpoint is an exact DEX bilateral standin; actual GLES arithmetic is checked separately by Mesa tests.')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
