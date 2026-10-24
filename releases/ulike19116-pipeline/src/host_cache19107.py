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
LEGACY_INGRESS_OWNERS19115=('regionalEvidence19107','initialize','initialize1971','initializeResident19110','ephemeral','ephemeral1973','trace19106','ephemeral1971')
def java_method19115(text,name):
    declarations=list(re.finditer(r'(?m)^    (?:private |public |protected )?static [^\n({;]+\b'+re.escape(name)+r'\(',text))
    assert len(declarations)==1,'Ambiguous ingress owner: '+name
    start=declarations[0].start();opening=text.index('{',declarations[0].end());depth=1;end=opening+1
    while depth:
        depth+=(text[end]=='{')-(text[end]=='}');end+=1
    return text[start:end]
def legacy_ingress_structure19115(current,historical):
    assert historical.count('regionalEvidence19107(')==8,'Published114 ingress scope changed'
    for name in LEGACY_INGRESS_OWNERS19115:
        old=java_method19115(historical,name);new=java_method19115(current,name)
        assert old.count('regionalEvidence19107(')==1 and new==old,'Legacy ingress owner changed: '+name
def candidate_ingress_structure19115(text):
    candidate=java_method19115(text,'ephemeralRegional19115')
    assert text.count('regionalEvidence19107(')==9 and candidate.count('regionalEvidence19107(')==1,'Current nine-site ingress ownership changed'
    # Exact entry prefix binds the checked clone/geometry and early fallback.
    # This independent structural check runs directly on each mutated source,
    # without the complete-source inverse masking a missing/moved domain guard.
    prefix='''
        if(!GpuNoise1960.regionalReady19115()||profile<4||profile>7||variant<0||variant>2||descriptor!=null||
           original==null||original.length!=32||original[10]!=3||original[3]<=original[2]||original[3]-original[2]>256){
            return requiredPrepass?new Read1971(null,"regional_preparation_unavailable"):
                ephemeral1973(source,evidence,maps,policy,original,descriptor,variant,profile);
        }
        int[] u=original.clone();markRegional19115(u);
        if(!regionalEvidence19107(evidence,u[0],u[7]))return new Read1971(null,"regional_input_unsupported19107");
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return new Read1971(null,"session_unavailable");
        GpuNoise1960.Lease1971 modelLease=null,rangeLease=null;'''
    assert candidate[candidate.index('{')+1:].startswith(prefix),'Regional candidate ingress prefix/guard changed'
    assert candidate.count('GpuNoise1960.open()')==1 and candidate.count('session.upload(2,evidence)')==1,'Regional candidate session/upload ownership changed'
    assert candidate.index('regionalEvidence19107(')<candidate.index('GpuNoise1960.open()')<candidate.index('session.upload(2,evidence)'),'Regional candidate uploads unchecked evidence'
def ingress_source_proof19115(source):
    inverse=module('cache_source_inverse19115',source/'source_inverse19115.py')
    current=(source/'GpuStrong1960.java').read_text()
    historical=inverse.restore(source,'GpuStrong1960.java',current.encode()).decode()
    legacy_ingress_structure19115(current,historical);candidate_ingress_structure19115(current)
    assert 'static boolean regionalReady19115(){return regionalPrepared19115;}' in (source/'GpuNoise1960.java').read_text(),'Regional readiness is no longer a cached scalar read'
    legacy_mutations=0
    for name in LEGACY_INGRESS_OWNERS19115:
        old=java_method19115(current,name);changed=old.replace('regionalEvidence19107(','regionalEvidenceRemoved19115(',1)
        mutated=once(current,old,changed)
        try:legacy_ingress_structure19115(mutated,historical)
        except AssertionError:legacy_mutations+=1
        else:raise AssertionError('Legacy ingress owner mutation accepted: '+name)
    candidate=java_method19115(current,'ephemeralRegional19115')
    guard='if(!regionalEvidence19107(evidence,u[0],u[7]))return new Read1971(null,"regional_input_unsupported19107");'
    opening='GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return new Read1971(null,"session_unavailable");'
    mutations=[candidate.replace(guard,'',1),candidate.replace(guard,guard.replace('if(!','if('),1),
        candidate.replace(guard,guard.replace('u[0],u[7]','u[7],u[0]'),1),
        candidate.replace(guard,'',1).replace(opening,opening+'\n        '+guard,1),
        candidate.replace(guard,'GpuNoise1960.open();\n        '+guard,1)]
    rejected=0
    for changed in mutations:
        try:candidate_ingress_structure19115(once(current,candidate,changed))
        except AssertionError:rejected+=1
        else:raise AssertionError('Regional candidate ingress structural mutation accepted')
    return dict(legacy_ingress_sites_preserved19115=True,regional_candidate_ingress_guard_verified19115=True,
        legacy_ingress_sites19115=8,regional_candidate_ingress_sites19115=1,
        legacy_ingress_negative_controls19115=legacy_mutations,regional_candidate_ingress_negative_controls19115=rejected,
        regional_candidate_ingress_scope19115='research_wrapper_cached_readiness_before_session_open_upload',
        complete_source_inverse_manifest_sha256=inverse.MANIFEST_SHA256,
        legacy_ingress_method_sha25619115={name:hashlib.sha256(java_method19115(current,name).encode()).hexdigest() for name in LEGACY_INGRESS_OWNERS19115})
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('JAVA_HOME') or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR'])
    fixtures=source/'tests19107/cache';candidate_fixture=source/'tests19115/cache/RegionalCandidateIngress19115Test.java';commands=[]
    tracked=[source/n for n in ('GpuStrong1960.java','GpuAnalysis1961.java','GpuStrongTuning1975.java','GpuStrongRouting1978.java','GpuStrongLayout1976.java','GpuNoise1960.java','GpuQualification1961.java','GpuCertificates1990.java','GpuStrongDefault1990.java','GpuModelDefault1990.java','GpuFinishDefault1990.java','GpuResident1976.java','GpuChain1961.java','StrongNoise1958.java','host_cache19107.py','native1960/engine1960.c','native1960/compute_dependencies1990.json','native1960/build_native1960.py','host_qualification1981.py')]+sorted(fixtures.glob('*.java'))
    tracked += [candidate_fixture,source/'source_inverse19115.py',source/'source_inverse19115_manifest.json',source/'tests19115/provenance/reference114/GpuStrong1960.java']
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    source_proof=ingress_source_proof19115(source)
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
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',full,*inputs,fixtures/'RegionalIngress19107Test.java',candidate_fixture],'ingress-compile')
    ingress=json.loads(run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',str(full)+os.pathsep+str(android),'com.hiro.ulike.RegionalIngress19107Test'],'ingress-run').strip().splitlines()[-1])
    candidate_ingress=json.loads(run([jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-Xmx512m','-cp',str(full)+os.pathsep+str(android),'com.hiro.ulike.RegionalCandidateIngress19115Test'],'candidate-ingress-run').strip().splitlines()[-1])
    # Call-site ownership: carry/upload paths must keep the domain check before
    # their sole regional upload; protected model-map globals remain unchanged.
    callsites={}
    for name in ('GpuStrong1960.java','GpuAnalysis1961.java','GpuStrongTuning1975.java','GpuStrongRouting1978.java','GpuStrongLayout1976.java'):
        text=(source/name).read_text();count=text.count('regionalEvidence19107(');callsites[name]=count
        # The independently checked .115 research wrapper adds exactly one site;
        # every published114 ingress owner's complete method remains identical.
        required={'GpuStrong1960.java':9,'GpuAnalysis1961.java':2,'GpuStrongTuning1975.java':2,'GpuStrongRouting1978.java':1,'GpuStrongLayout1976.java':1}[name]
        if count!=required:raise AssertionError('Unexpected regional ingress coverage '+name+': '+str(count))
    if cache['proof_families']!=8 or cache['corpus_families']!=4 or ingress['generated_models']!=84 or ingress['invalid_binary32_classes']!=9:raise AssertionError('Cache/ingress coverage incomplete')
    if candidate_ingress!={'status':'passed','assertions':112,'invalid_binary32_classes':9,'readiness_required_combinations':4,'invalid_cases':36}:raise AssertionError('Current regional candidate ingress coverage incomplete')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Cache/ingress source changed during test')
    result=dict(status='passed',assertions=cache['assertions']+ingress['assertions']+10+112+13,cache=cache,ingress=ingress,candidate_ingress19115=candidate_ingress,
        cache_fingerprint_isolation_verified=True,all_cache_families_covered=True,regional_evidence_domain_verified=True,
        invalid_ingress_rejected_before_gpu=True,generated_regional_evidence_accepted=True,
        native_fingerprint_old=OLD_FINGERPRINT106,native_fingerprint_new=fingerprint,
        native_fingerprint_dependency_sha256=native_pins,ingress_callsites=callsites,source_sha256=pins,
        physical_android_tested=False,device_speedup_verified=False,
        scope='Real proof/corpus caches with controlled device identity; actual runtime domain guards and generated CPU/startup evidence. No physical-device speed or equality claim.')
    result.update(source_proof)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
