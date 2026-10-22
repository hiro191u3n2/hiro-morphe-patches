#!/usr/bin/env python3
"""Execute real proof-cache isolation and Strong regional-domain ingress guards."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, re, subprocess, sys
ROOT=Path(__file__).resolve().parent
OLD_FINGERPRINT106='5ab857f6118a9e0b146faf2b118a21a8b2ad17187483baa231fec3f91fc9fbf1'
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def once(body,old,new):
    if body.count(old)!=1:raise AssertionError('Cache fixture anchor changed: '+old)
    return body.replace(old,new)
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR'])
    fixtures=source/'tests19107/cache';commands=[]
    tracked=[source/n for n in ('GpuStrong1960.java','GpuAnalysis1961.java','GpuStrongTuning1975.java','GpuStrongRouting1978.java','GpuStrongLayout1976.java','GpuNoise1960.java','GpuQualification1961.java','GpuCertificates1990.java','GpuStrongDefault1990.java','GpuModelDefault1990.java','GpuFinishDefault1990.java','GpuResident1976.java','GpuChain1961.java','StrongNoise1958.java','host_cache19107.py','native1960/engine1960.c','native1960/compute_dependencies1990.json','native1960/build_native1960.py','host_qualification1981.py')]+sorted(fixtures.glob('*.java'))
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    def run(argv,name):
        argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));p=subprocess.run(argv,text=True,capture_output=True,timeout=300)
        out=p.stdout+p.stderr;(work/(name+'.log')).write_text(out)
        if p.returncode:raise RuntimeError(name+'\n'+out[-14000:])
        return out
    # The identity consumed by the native engine is generated from actual current
    # compute files; no ad-hoc version string substitutes for shader dependency pins.
    native=module('cache_native19107',source/'native1960/build_native1960.py')
    fingerprint,native_pins=native.compute_fingerprint1990(source/'native1960')
    if fingerprint==OLD_FINGERPRINT106:raise AssertionError('Changed arithmetic retained old fingerprint')
    generated=work/'native-header';generated.mkdir(exist_ok=True);header=native.shader_header(source/'native1960',generated)
    if '#define GPU1960_SOURCE_SHA256 "'+fingerprint+'"' not in header.read_text():raise AssertionError('Native header identity mismatch')
    engine=(source/'native1960/engine1960.c').read_text()
    if 'source=%s' not in engine or 'GPU1960_SOURCE_SHA256' not in engine:raise AssertionError('Native environment omits source identity')
    holders=module('cache_android19107',source/'host_qualification1981.py');files=[]
    for name,body in holders.FIXTURES.items():
        if name.endswith('QueueHost1967.java'):continue
        if name.endswith('QueueFixtures1967.java'):
            body=once(body,'static long captureEpoch1953(){return epoch;}','static long captureEpoch1953(){return epoch;}static long captureId1989(long epoch){return epoch;}')
            body=once(body,'static String fingerprint(){return "unchanged-pixel-shader-v1";}','static String identity="";static String fingerprint(){return identity;}')
        p=work/'cache-fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(p)
    classes=work/'cache-classes';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*files,source/'GpuQualification1961.java',source/'GpuCertificates1990.java',fixtures/'CacheIsolation19107Test.java'],'cache-compile')
    cache=json.loads(run([jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.CacheIsolation19107Test',OLD_FINGERPRINT106,fingerprint],'cache-run').strip().splitlines()[-1])
    # Compile the actual runtime closure. The ingress tests call real full-route
    # wrappers and verify invalid input cannot initialize JNI or open a session.
    sys.path.insert(0,str(source));builder=module('cache_compile19107',source/'build1992.py')
    full=work/'ingress-classes';full.mkdir(exist_ok=True);inputs=list(dict.fromkeys(builder.compile_inputs([]).values()))
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',full,*inputs,fixtures/'RegionalIngress19107Test.java'],'ingress-compile')
    ingress=json.loads(run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',str(full)+os.pathsep+str(android),'com.hiro.ulike.RegionalIngress19107Test'],'ingress-run').strip().splitlines()[-1])
    # Call-site ownership: carry/upload paths must keep the domain check before
    # their sole regional upload; protected model-map globals remain unchanged.
    callsites={}
    for name in ('GpuStrong1960.java','GpuAnalysis1961.java','GpuStrongTuning1975.java','GpuStrongRouting1978.java','GpuStrongLayout1976.java'):
        text=(source/name).read_text();count=text.count('regionalEvidence19107(');callsites[name]=count
        # .110 retains all seven existing occurrences (including the helper
        # declaration) and adds the resident-only initializer's upload guard.
        required={'GpuStrong1960.java':8,'GpuAnalysis1961.java':2,'GpuStrongTuning1975.java':2,'GpuStrongRouting1978.java':1,'GpuStrongLayout1976.java':1}[name]
        if count!=required:raise AssertionError('Unexpected regional ingress coverage '+name+': '+str(count))
    if cache['proof_families']!=8 or cache['corpus_families']!=4 or ingress['generated_models']!=84 or ingress['invalid_binary32_classes']!=9:raise AssertionError('Cache/ingress coverage incomplete')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Cache/ingress source changed during test')
    result=dict(status='passed',assertions=cache['assertions']+ingress['assertions']+10,cache=cache,ingress=ingress,
        cache_fingerprint_isolation_verified=True,all_cache_families_covered=True,regional_evidence_domain_verified=True,
        invalid_ingress_rejected_before_gpu=True,generated_regional_evidence_accepted=True,
        native_fingerprint_old=OLD_FINGERPRINT106,native_fingerprint_new=fingerprint,
        native_fingerprint_dependency_sha256=native_pins,ingress_callsites=callsites,source_sha256=pins,
        physical_android_tested=False,device_speedup_verified=False,
        scope='Real proof/corpus caches with controlled device identity; actual runtime domain guards and generated CPU/startup evidence. No physical-device speed or equality claim.')
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
