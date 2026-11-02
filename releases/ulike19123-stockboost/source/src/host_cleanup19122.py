#!/usr/bin/env python3
"""Actual weighted CPU scheduler + post-save cleanup lifecycle against scalar
queue/GPU/native fixtures. Includes policy reset and next-capture races, not
Android memory reclamation, frame timing or performance claims.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
ROOT=Path(__file__).resolve().parent

def test(source,work,jdk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
    fixture=source/'tests19122/cleanup/com/hiro/ulike'
    production=[source/'SpeedWorkers1935.java',source/'PostSaveCleanup19122.java']
    queue_fixture=source/'tests19122/queue/com/hiro/ulike'
    tracked=[*production,source/'SaveQueue1935.java',source/'host_cleanup19122.py',*sorted(fixture.glob('*.java')),*sorted(queue_fixture.glob('*.java'))]
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    def run(label,args,success=True):
        result=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=40)
        (work/(label+'.log')).write_text(result.stdout+result.stderr)
        if success and result.returncode:raise AssertionError(label+'\n'+(result.stdout+result.stderr)[-10000:])
        if not success and result.returncode==0:raise AssertionError('Mutation unexpectedly passed: '+label)
        return result
    def compile_to(target,inputs,label):
        target.mkdir(parents=True,exist_ok=True)
        run(label,[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',target,*inputs,*sorted(fixture.glob('*.java'))])
    classes=work/'classes';compile_to(classes,production,'compile')
    results=[]
    for cores in (1,3,4):
        result=run('run-'+str(cores),[jdk/'bin/java','-ea','-XX:ActiveProcessorCount='+str(cores),'-cp',classes,'com.hiro.ulike.Cleanup19122Test'])
        result=json.loads(result.stdout.strip().splitlines()[-1]);result['host_processor_count']=cores;results.append(result)
    common_fixtures=[p for p in sorted(fixture.glob('*.java')) if p.name not in ('SaveQueue1935.java','Cleanup19122Test.java')]
    queue_inputs=[*production,source/'SaveQueue1935.java',*common_fixtures,*sorted(queue_fixture.glob('*.java'))]
    def queue_compile(target,inputs,label):
        target.mkdir(parents=True,exist_ok=True)
        run(label,[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',target,*inputs])
    queue_classes=work/'queue-classes';queue_compile(queue_classes,queue_inputs,'queue-compile')
    result=run('queue-run',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',queue_classes,'com.hiro.ulike.QueueCleanup19122Test'])
    result=json.loads(result.stdout.strip().splitlines()[-1]);result['test']='actual_save_queue';results.append(result)
    mutations={
        'worker_cap_not_enforced':('SpeedWorkers1935.java','return CPU_PERMITS19122/workerLimit19122();','return 1;'),
        'cleanup_revokes_cpu_lease':('SpeedWorkers1935.java','!CPU.tryAcquire(CPU_PERMITS19122)','!true'),
        'skip_comparison_not_frozen':('PostSaveCleanup19122.java','if(!enabled){result[0]=4;return;}','if(!enabled19122()){result[0]=4;return;}'),
        'single_workspace_revoked':('SpeedWorkers1935.java','if(scratchBytes(SINGLE_SCRATCH1978)!=0)return result;','/* MUTATION removed active Single guard */')}
    controls={}
    for name,(filename,before,after) in mutations.items():
        folder=work/name;folder.mkdir(exist_ok=True)
        raw=(source/filename).read_text()
        if raw.count(before)!=1:raise AssertionError('Mutation source mismatch '+name)
        changed=folder/filename;changed.write_text(raw.replace(before,after))
        inputs=[changed if p.name==filename else p for p in production]
        compile_to(folder/'classes',inputs,name+'-compile')
        result=run(name,[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',folder/'classes','com.hiro.ulike.Cleanup19122Test'],False)
        if 'AssertionError' not in result.stderr:raise AssertionError('Mutation failed outside assertion '+name)
        controls[name]=True
    release_original=(source/'SaveQueue1935.java').read_text()
    release_token='// This runs after releasing jobs and still respects terminal UI ownership.\n        notifyIdle19122();'
    if release_original.count(release_token)!=1:raise AssertionError('Release-hook mutation mismatch')
    release_path=work/'missing-release-event'/'SaveQueue1935.java';release_path.parent.mkdir(exist_ok=True)
    release_path.write_text(release_original.replace(release_token,'/* MUTATION omitted failed-preparation release event */'))
    release_inputs=[release_path if p.name=='SaveQueue1935.java' else p for p in queue_inputs]
    queue_compile(work/'missing-release-classes',release_inputs,'release-mutation-compile')
    mutation=run('release-mutation',[jdk/'bin/java','-ea','-XX:ActiveProcessorCount=4','-cp',work/'missing-release-classes','com.hiro.ulike.QueueCleanup19122Test'],False)
    if 'AssertionError' not in mutation.stderr:raise AssertionError('Release mutation failed outside assertion')
    controls['failed_preparation_idle_event_missing']=True
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Sources changed during checks')
    result=dict(status='passed',assertions=sum(r['assertions'] for r in results),tests=results,
        source_sha256=pins,negative_controls=controls,physical_android_tested=False,device_speedup_verified=False,
        fixture_scope='Real SpeedWorkers scheduler/array pool and PostSaveCleanup coordinator. Queue/shot status, GPU owner and native scratch capacities use deterministic fixtures; GPU owner is separately tested. Actual SaveQueue reserve/release/drain/finishStages exercises both terminal-before-release and terminal-after-release ordering. Java reference drops are not physical memory-reclamation measurements.')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',default=ROOT);p.add_argument('--work',required=True);p.add_argument('--jdk')
    args=p.parse_args();print(json.dumps(test(args.source,args.work,args.jdk)))
