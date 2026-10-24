#!/usr/bin/env python3
"""Warmed production Stage/JNI timings on software GLES, with load observations.

Reuses a completed real-JNI correctness build only after checking all of that
build's recorded source hashes. This never writes real qualification records.
"""
from pathlib import Path
import argparse,hashlib,json,os,statistics,subprocess,time

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def load_observation():
    info={'monotonic':time.monotonic(),'load_average':list(os.getloadavg())}
    for name in ('cpu.max','cpu.stat','memory.current'):
        path=Path('/sys/fs/cgroup')/name
        if path.is_file():info[name]=path.read_text().strip()
    try:
        p=subprocess.run(['ps','-eo','pid,pcpu,comm','--sort=-pcpu'],capture_output=True,text=True,check=True)
        info['top_process_cpu']=p.stdout.splitlines()[:9]
    except Exception as e:info['process_observation_error']=type(e).__name__
    return info

def test(source,work,jdk=None,ndk=None,proof=None):
    source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    proof=Path(proof or os.environ['ULIKE_STAGE115_PROOF']).resolve()
    evidence=json.loads((proof/'result.json').read_text());assert evidence['status']=='passed' and evidence['actual_jni']
    for name,value in evidence['source_sha256'].items():
        assert sha(source/name)==value,'prebuilt correctness source changed: '+name
    pins={name:sha(source/name) for name in evidence['source_sha256']}
    harness=source/'tests19115/stage/RegionalStageTiming19115Test.java'
    pins[str(harness.relative_to(source))]=sha(harness);pins[Path(__file__).name]=sha(__file__)
    artifacts=[p for root in (proof/'classes',proof/'native') for p in root.rglob('*') if p.is_file()]
    artifact_pins={str(p.relative_to(proof)):sha(p) for p in artifacts}
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']);jar=Path(os.environ['ULIKE_ANDROID_JAR'])
    classes=work/'classes';classes.mkdir(exist_ok=True)
    cp=str(proof/'classes')+os.pathsep+str(jar)
    compile_cmd=[str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-cp',cp,'-d',str(classes),str(harness)]
    c=subprocess.run(compile_cmd,capture_output=True,text=True);(work/'compile.log').write_text(c.stdout+c.stderr)
    if c.returncode:raise AssertionError((work/'compile.log').read_text())
    command=[str(jdk/'bin/java'),'-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx1200m',
             '-Djava.library.path='+str(proof/'native'),'-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.RegionalStageTiming19115Test']
    env=dict(os.environ,EGL_PLATFORM='surfaceless',LIBGL_ALWAYS_SOFTWARE='1',LP_NUM_THREADS='4')
    observations=[load_observation()];samples=[];summary=None;started=time.monotonic()
    print('Running actual Stage/JNI full-frame pairs; software GLES only.',flush=True)
    with (work/'timing.log').open('w') as log:
        process=subprocess.Popen(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1,env=env)
        for line in process.stdout:
            log.write(line);log.flush();print(line.rstrip(),flush=True)
            if line.startswith('FRAME_READY ') or line.startswith('SAMPLE '):observations.append(load_observation())
            if line.startswith('SAMPLE '):samples.append(json.loads(line[7:]))
            if line.startswith('RESULT '):summary=json.loads(line[7:])
        code=process.wait(timeout=900)
    if code or summary is None:raise AssertionError('Timing harness failed; see '+str(work/'timing.log'))
    assert len(samples)==16 and summary['status']=='passed'
    shapes=[]
    for w,h in ((3060,4080),(4080,3060)):
        measured=[x for x in samples if x['width']==w and x['height']==h and not x['warmup']]
        old=[x['elapsed_ns'] for x in measured if not x['candidate']];new=[x['elapsed_ns'] for x in measured if x['candidate']]
        assert len(old)==len(new)==3
        shapes.append({'width':w,'height':h,'standard_ns':old,'candidate_ns':new,
                       'standard_median_ns':statistics.median(old),'candidate_median_ns':statistics.median(new),
                       'candidate_median_change_percent':100*(statistics.median(new)/statistics.median(old)-1),
                       'standard_best_ns':min(old),'candidate_worst_ns':max(new)})
    assert pins=={name:sha(source/name) for name in pins},'timing source changed during measurement'
    assert artifact_pins=={name:sha(proof/name) for name in artifact_pins},'prebuilt JNI/classes changed during measurement'
    result=dict(summary,shapes=shapes,samples=samples,source_sha256=pins,prebuilt_artifact_sha256=artifact_pins,
                actual_stage_four_workers=True,actual_bank_scheduler=True,cpu_fallback_forbidden=True,
                elapsed_seconds=round(time.monotonic()-started,3),host_load_observations=observations,
                quiet_host_verified=False,device_speedup_verified=False,save_2000ms_verified=False,
                scope='Synthetic warmed Strong Stage only, actual production four-worker pool/JNI/software GLES; not physical Galaxy timing.',
                timing_includes=['Stage construction','output/workspace allocation','model upload','strip/policy upload','real two-bank waiting','prepass and Strong dispatch','direct readback','owned output commit copies','endStage and session close'],
                timing_excludes=['synthetic source/model/policy construction','startup/candidate compilation and quality qualification','output equality comparisons','Android Bitmap I/O','model generation','camera/beauty','correction','codec','publication'],
                certificate_scope='Test-only in-memory EXACT/FAST records control which production route runs; these are not speed certifications.',
                background_load_scope='Host load snapshots are retained; unrelated workloads were observed, so this report does not claim a quiet or isolated host.')
    (work/'commands.json').write_text(json.dumps([compile_cmd,command],indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');p.add_argument('--proof',required=True);a=p.parse_args()
    r=test(a.source,a.work,a.jdk,a.ndk,a.proof);print(json.dumps({k:r[k] for k in ('status','shapes','device_speedup_verified','save_2000ms_verified')}))
