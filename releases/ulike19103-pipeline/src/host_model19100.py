#!/usr/bin/env python3
"""Retain full CPU/model equality; compare .99/current shader model graphs exactly.

The benchmark holds the Java graph, native engine and every other shader fixed.
Only strong1960.comp is replaced with the frozen .99 source. Preparation shares
localWork with Strong: its identical rounded luma quotient/square is now reused.
The exported raw maps/evidence are compared byte-for-byte, not only by hashes.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR']);native=source/'native1960'
    harness=source/'tests19100/model/ModelPreparation19100Bench.java';reference=source/'tests19100/strong/reference99/strong1960.comp'
    tracked=[source/'GpuAnalysis1961.java',source/'StrongNoise1958.java',harness,reference,source/'host_model19100.py',source/'host_strong_math19100.py',native/'engine1960.c']+list(native.glob('*.comp'))
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    strict=module('model100_strict',source/'host_strong_math19100.py')
    if sha(reference)!=strict.REFERENCE99 or (native/'strong1960.comp').read_text()!=strict.extend19100(reference.read_text()):raise AssertionError('Model shader exceeds reviewed exact scalar sharing delta')
    base=module('model100_base',source/'host_model_math1990.py');retained=base.test(source,work/'model',jdk,ndk)
    cp=str(work/'model/classes')+os.pathsep+str(android);env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1')
    commands=[]
    def run(command,name):
        command=list(map(str,command));commands.append({'name':name,'argv':command})
        r=subprocess.run(command,capture_output=True,text=True,env=env,timeout=420);output=r.stdout+r.stderr;(work/(name+'.log')).write_text(output)
        if r.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise AssertionError(name+'\n'+output[-12000:])
        return output
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',work/'model/classes',harness],'compile-preparation')
    builder=module('model100_builder',native/'build_native1960.py');results={}
    for tag in ('before','after'):
        out=work/tag;out.mkdir(exist_ok=True)
        # Same source snapshot; .99 differs only in the strong shader. The
        # benchmark fingerprint is explicitly distinct from production proof.
        header='/* Host comparison snapshot; never used for certification. */\n'
        for name in builder.NAMES:
            shader=reference.read_text() if tag=='before' and name=='strong1960' else (native/(name+'.comp')).read_text()
            header+='static const char '+name+'_source[]=\n'+''.join(json.dumps(line+'\n')+'\n' for line in shader.splitlines())+';\n'
        header+='#define GPU1960_SOURCE_SHA256 "bench19100-'+hashlib.sha256(header.encode()).hexdigest()+'"\n'
        (out/'shader_sources1960.h').write_text(header)
        run(['cc','-std=c11','-O3','-shared','-fPIC','-DANDROID','-fno-fast-math','-ffp-contract=off','-Wall','-Wextra','-Werror','-Wno-misleading-indentation','-I'+str(work/'model/headers'),'-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(out),native/'engine1960.c','-l:libEGL.so.1','-l:libGL.so.1','-o',out/'libulike_gpu1960.so'],tag+'-compile')
        output=run([jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx512m','-Djava.library.path='+str(out)+os.pathsep+str(work/'model/cpu-native'),'-cp',cp,'com.hiro.ulike.ModelPreparation19100Bench',out/'maps-evidence.bin'],tag+'-model-bench')
        result=json.loads(output.split('RESULT ',1)[1]);results[tag]=result
        if result.get('status')!='passed' or len(result.get('model_benchmarks',[]))!=6 or not result.get('actual_jni'):raise AssertionError('Incomplete model graph benchmark')
    before=(work/'before/maps-evidence.bin').read_bytes();after=(work/'after/maps-evidence.bin').read_bytes()
    if not before or before!=after:raise AssertionError('Old/new complete model map/evidence bits differ')
    ratios=[]
    for old,new in zip(results['before']['model_benchmarks'],results['after']['model_benchmarks']):
        if any(old[k]!=new[k] for k in ('width','height','variant','output_hash')):raise AssertionError('Old/new model case correspondence differs')
        ratios.append(dict(width=old['width'],height=old['height'],variant=old['variant'],before_median_ns=old['median_ns'],after_median_ns=new['median_ns'],after_over_before=new['median_ns']/old['median_ns']))
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Model sources changed during proof')
    result=dict(status='passed',assertions=retained['assertions']+sum(r['assertions'] for r in results.values())+7,tests=[retained,results],whole_model_bitwise_equal=True,model_preparation_old_new_bitwise_equal=True,model_local_luma_ratio_shared_exactly=True,model_original_fp32_order_preserved=True,actual_jni=True,physical_android_tested=False,device_speedup_verified=False,compared_model_output_bytes=len(before),model_output_sha256=hashlib.sha256(before).hexdigest(),host_model_benchmarks=ratios,benchmark_comparison='frozen .99 Strong shader vs current; Java graph, native engine, other shaders fixed',source_sha256=pins)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);p.add_argument('--ndk',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),ensure_ascii=False))
