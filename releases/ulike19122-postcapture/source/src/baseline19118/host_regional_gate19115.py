#!/usr/bin/env python3
"""Exercise the real optional-regional admission helper with controlled peers.

These are policy/lifecycle tests; peer delays select deterministic branches and
are never numerical shader proofs or estimates of physical Android save time.
"""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, time

ROOT=Path(__file__).resolve().parent
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
MUTATIONS=[
 ('tail_speed_scope','u[3]-u[2]==256','u[3]-u[2]<=256','short tail not speed certified'),
 ('geometry_speed_scope','return width==3060&&height==4080||width==4080&&height==3060;','return width==3060||width==4080;','unmeasured geometry refused'),
 ('both_certificates','GpuCertificates1990.restore(EXACT)!=null&&GpuCertificates1990.restore(FAST)!=null','GpuCertificates1990.restore(EXACT)!=null','quality alone cannot select'),
 ('prepared_program','&&GpuNoise1960.regionalReady19115()','', 'restored certificates still need program preparation'),
 ('standard_independent','return GpuStrongDefault1990.ready()&&!legacyAdaptiveCached19115()&&GpuNoise1960.regionalReady19115()&&cached();','return !legacyAdaptiveCached19115()&&GpuNoise1960.regionalReady19115()&&cached();','standard proof remains independently required'),
 ('shape_rejection','&&GpuStrongDefault1990.accepts(u)','', 'per-shape standard rejection remains authoritative'),
 ('early_request','if(!GpuQualification1961.optionalBenchmarksAllowed1995()||!GpuStrongDefault1990.ready()||','if(!GpuStrongDefault1990.ready()||','no optional queue/compile before publication'),
 ('actual_route','||regional&&!actual.regional19115','', 'unreported old-route fallback cannot certify candidate'),
 ('required_route','f.policy,f.u,null,0,4,true)','f.policy,f.u,null,0,4,false)','every candidate call requires actual new route'),
 ('both_widths','speed&=measuredWin(best,worst);','speed=measuredWin(best,worst);','either measured orientation can veto speed'),
 ('five_percent','candidateWorst<=baselineBest-required','candidateWorst<=baselineBest','invalid or insufficient measured improvement refused'),
 ('two_trials','trial<2;trial++','trial<1;trial++','every candidate call requires actual new route'),
 ('pending_close','public void close(){synchronized(GpuRegionalDefault19115.class){requested=false;}}','public void close(){synchronized(GpuRegionalDefault19115.class){requested=true;}}','queued cancellation close clears pending'),
 ('baseline_invalidation','if(!regional)GpuStartup1990.invalidateDomain();','if(regional)GpuStartup1990.invalidateDomain();','new candidate failure preserves standard'),
]
def test(source,work,jdk=None,ndk=None):
    source,work=Path(source).resolve(),Path(work).resolve()
    if work.exists():raise AssertionError('Fresh regional admission work directory required')
    work.mkdir(parents=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    tracked=[source/'GpuRegionalDefault19115.java',source/'host_regional_gate19115.py',source/'tests19115/admission/RegionalPeers19115.java',source/'tests19115/admission/RegionalAdmission19115Test.java']
    pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[];started=time.monotonic()
    def run(argv,name,expect_failure=None):
        argv=list(map(str,argv));commands.append({'name':name,'argv':argv})
        p=subprocess.run(argv,text=True,capture_output=True,timeout=180);output=p.stdout+p.stderr
        (work/(name+'.log')).write_text(output)
        if expect_failure:
            if not p.returncode or 'java.lang.AssertionError' not in output or expect_failure not in output:
                raise AssertionError('Mutation did not fail at intended contract '+name+':\n'+output[-8000:])
        elif p.returncode:raise AssertionError(name+' failed:\n'+output[-8000:])
        return output
    def compile_and_run(helper,folder,name,expected=None):
        folder.mkdir(parents=True)
        run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',folder,helper,*tracked[2:]],name+'-compile')
        return run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xmx768m','-cp',folder,'com.hiro.ulike.RegionalAdmission19115Test'],name,expected)
    result=json.loads(compile_and_run(tracked[0],work/'classes','real-helper'))
    if result.get('status')!='passed' or result.get('assertions',0)<100 or result.get('scenarios',0)<30:raise AssertionError('Incomplete admission proof')
    original=tracked[0].read_text();mutations=[]
    for name,old,new,expected in MUTATIONS:
        if original.count(old)!=1:raise AssertionError('Ambiguous or missing mutation anchor '+name)
        folder=work/'mutations'/name;folder.mkdir(parents=True)
        helper=folder/'GpuRegionalDefault19115.java';helper.write_text(original.replace(old,new,1))
        compile_and_run(helper,folder/'classes','mutation-'+name,expected)
        mutations.append({'name':name,'rejected':True,'expected_assertion':expected})
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Admission proof inputs changed while executing')
    result.update(assertions=result['assertions']+len(mutations),mutation_controls=mutations,mutation_count=len(mutations),source_sha256=pins,host_elapsed_seconds=round(time.monotonic()-started,3),scope='Real helper with controlled certificate, queue, oracle and transport peers; actual scheduling/JNI/GLES and device timing are separate proofs.',physical_android_tested=False,device_speedup_verified=False)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
