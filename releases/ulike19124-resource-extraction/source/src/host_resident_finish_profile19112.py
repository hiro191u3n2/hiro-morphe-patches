#!/usr/bin/env python3
"""Foreground row-profile race regression using the actual admission producer.

Controlled peers model a readiness change during preflight. A second compiled
copy removes only the reviewed guard and must positively reproduce execution
of 128 rows under the restored 64-row whole proof. No pixel/native claim.
"""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess

ROOT=Path(__file__).resolve().parent
PRODUCER_SHA256='c3aaef4eb23a532bc0af2377266a4951fe39779533fce1d0d9cfb615f2618635'
GUARD='''                if(preflight.finishRows19112!=rowsFromKey19112(base)||
                        !routesFromKey19102(base).equals(deployedRoutes19102())){terminal1982=13;return null;}
'''
CURRENT_GUARD=GUARD.replace('deployedRoutes19102()','deployedRoutes19115(source.getWidth(),source.getHeight())')
COMMIT='queueDecision1983=reservation1984.commit(new GpuQualification1961.Probe(){'
CPU_CALL='oracle=QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,null,rotation,width,height,frozenOutput,false,true,identity);'
CPU_OLD='''                                try {'''+CPU_CALL+'''}
                                finally {CPU.remove();}'''
CPU_CURRENT='''                                residentTiming19111("beginCpu19115",trial,0,0);
                                try {
                                    '''+CPU_CALL+'''
                                    residentTiming19111("stopCpu19115",oracle==null?2:1,0,0);
                                } finally {residentTiming19111("publishCpu19115",0,0,0);CPU.remove();}'''

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def once(text,old,new):
    assert text.count(old)==1,'Foreground profile adapter anchor changed: '+old
    return text.replace(old,new,1)
def producer_structure19115(current,historical):
    start='    static Bitmap finish1978('
    old=historical[historical.index(start):historical.index(COMMIT)]
    new=current[current.index(start):current.index(COMMIT)]
    shape='deployedRoutes19115(source.getWidth(),source.getHeight())'
    assert new.count(shape)==2,'Foreground shape guard sites changed'
    assert new.replace(shape,'deployedRoutes19102()')==old,'Foreground admission/copy sequence changed outside exact shape guards'
    assert current.count(CURRENT_GUARD)==1 and historical.count(GUARD)==1,'Foreground profile guard missing'
    key_start='    private static String key(';key_end='    private static void note('
    key=current[current.index(key_start):current.index(key_end)]
    assert key.count(shape)==1 and key.replace(shape,'deployedRoutes19102()')==historical[historical.index(key_start):historical.index(key_end)],'Shape-aware proof key changed beyond deployed identity'
    copy='snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return null;'
    assert current.count(copy)==historical.count(copy)==1,'Foreground snapshot copy count differs'
    loop='for(int trial=0;trial<2;trial++) {'
    assert current.count(loop)==historical.count(loop)==1 and current.count(CPU_CALL)==historical.count(CPU_CALL)==1,'Existing two-trial CPU reference call changed'
    assert current.count(CPU_CURRENT)==1 and historical.count(CPU_OLD)==1,'CPU observer no longer wraps only existing reference work'
    assert current.index(COMMIT)<current.index(loop)<current.index(CPU_CURRENT),'CPU reference left queued two-trial scope'
def producer_source_proof19115(source):
    spec=importlib.util.spec_from_file_location('profile_source_inverse19115',source/'source_inverse19115.py');inverse=importlib.util.module_from_spec(spec);spec.loader.exec_module(inverse)
    current=(source/'GpuResident1976.java').read_text();historical=inverse.restore(source,'GpuResident1976.java',current.encode()).decode()
    assert hashlib.sha256(historical.encode()).hexdigest()==PRODUCER_SHA256,'Reviewed historical foreground producer hash changed'
    producer_structure19115(current,historical)
    mutations=[once(current,CURRENT_GUARD,''),once(current,CURRENT_GUARD,CURRENT_GUARD.replace('!=rowsFromKey19112','==rowsFromKey19112')),
        once(current,'snapshot=source.copy(Bitmap.Config.ARGB_8888,false);','source.copy(Bitmap.Config.ARGB_8888,false);snapshot=source.copy(Bitmap.Config.ARGB_8888,false);'),
        once(current,'for(int trial=0;trial<2;trial++) {','for(int trial=0;trial<3;trial++) {'),
        once(current,CPU_CALL,CPU_CALL.replace('false,true,identity','false,false,identity'))]
    rejected=0
    for changed in mutations:
        try:producer_structure19115(changed,historical)
        except AssertionError:rejected+=1
        else:raise AssertionError('Foreground producer structure mutation accepted')
    return dict(historical_producer_pin_preserved19115=True,current_foreground_copy_sequence_preserved19115=True,
        existing_two_cpu_reference_trials_preserved19115=True,producer_structure_negative_controls19115=rejected,
        historical_producer_sha25619115=PRODUCER_SHA256,complete_source_inverse_manifest_sha256=inverse.MANIFEST_SHA256)

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    producer=source/'GpuResident1976.java'
    source_proof=producer_source_proof19115(source)
    fixture=source/'tests19112/finish_profile'
    peers=fixture/'ProfilePeers19112.java';harness=fixture/'ResidentFinishProfile19112Test.java'
    bitmap=source/'tests1982/pipeline/android/graphics/Bitmap.java'
    shape_harness=source/'tests19115/finish_profile/ResidentShapeProfile19115Test.java'
    tracked=[producer,peers,harness,bitmap,shape_harness,source/'host_resident_finish_profile19112.py',source/'source_inverse19115.py',source/'source_inverse19115_manifest.json',source/'tests19115/provenance/reference114/GpuResident1976.java']
    pins={str(p.relative_to(source)):sha(p) for p in tracked}
    commands=[]
    def run(argv,name):
        argv=list(map(str,argv));commands.append(dict(name=name,argv=argv))
        result=subprocess.run(argv,capture_output=True,text=True,timeout=90)
        output=result.stdout+result.stderr;(work/(name+'.log')).write_text(output)
        if result.returncode:raise AssertionError(name+'\n'+output)
        return output
    classes=work/'current';classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,producer,peers,harness,bitmap],'compile-current')
    current=json.loads(run([jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.ResidentFinishProfile19112Test'],'run-current').split('RESULT ',1)[1])
    if current.get('status')!='passed' or current.get('cases')!=5 or current.get('assertions')!=45 or current.get('faulty_guard_removed') is not False:
        raise AssertionError('Current foreground profile regression incomplete')
    raw=producer.read_text()
    if raw.count(CURRENT_GUARD)!=1:raise AssertionError('Exactly one reviewed foreground guard required')
    faulty=work/'faulty-source/GpuResident1976.java';faulty.parent.mkdir(exist_ok=True)
    faulty.write_text(raw.replace(CURRENT_GUARD,'',1))
    if faulty.read_text().replace('                Bitmap result=QualityPipeline1932.strongFinish1978(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false,identity);',CURRENT_GUARD+'                Bitmap result=QualityPipeline1932.strongFinish1978(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false,identity);',1)!=raw:
        raise AssertionError('Fault copy changed more than the foreground guard')
    fault_classes=work/'faulty';fault_classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',fault_classes,faulty,peers,harness,bitmap],'compile-faulty-guard-removal')
    fault=json.loads(run([jdk/'bin/java','-ea','-Xmx128m','-cp',fault_classes,'com.hiro.ulike.ResidentFinishProfile19112Test','faulty'],'run-faulty-positive-reproducer').split('RESULT ',1)[1])
    if fault.get('status')!='passed' or fault.get('cases')!=1 or fault.get('assertions')!=9 or fault.get('faulty_guard_removed') is not True:
        raise AssertionError('Removed guard did not positively reproduce unsafe 128-row execution')
    # Preserve both old immutable fixtures. A separately generated peer adds
    # only the real .115 optional shape overload, with a deterministic change
    # during preflight; the actual current producer remains unmodified.
    shape_peers=work/'shape-peers/ProfilePeers19112.java';shape_peers.parent.mkdir(exist_ok=True)
    shape_text=once(peers.read_text(),'final class GpuStrongDefault1990 {static int profile1994(){return 4;}}','''final class GpuStrongDefault1990 {
    static int shapeProfile,nextShapeProfile,shapeCalls;
    static int profile1994(){return 99;}
    static int profile1994(int width,int height){if(width!=5||height!=7)throw new AssertionError("caller source geometry changed");shapeCalls++;return shapeProfile;}
    static boolean regionalFaster19115(){return false;}
}''')
    shape_text=once(shape_text,'preferred=afterPreferred;active=new Preflight1981(planned);return active;','preferred=afterPreferred;GpuStrongDefault1990.shapeProfile=GpuStrongDefault1990.nextShapeProfile;active=new Preflight1981(planned);return active;')
    shape_peers.write_text(shape_text)
    shape_results=[]
    for label,body,args in [('current',producer,[]),('faulty',faulty,['faulty'])]:
        shape_classes=work/('shape-'+label);shape_classes.mkdir(exist_ok=True)
        run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',shape_classes,body,shape_peers,harness,shape_harness,bitmap],'compile-shape-'+label)
        shape_results.append(json.loads(run([jdk/'bin/java','-ea','-Xmx128m','-cp',shape_classes,'com.hiro.ulike.ResidentShapeProfile19115Test',*args],'run-shape-'+label).split('RESULT ',1)[1]))
    shape_current,shape_fault=shape_results
    if shape_current!={'status':'passed','assertions':44,'cases':4,'faulty_guard_removed':False} or shape_fault!={'status':'passed','assertions':11,'cases':1,'faulty_guard_removed':True}:raise AssertionError('Shape-aware foreground profile regression incomplete')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Profile regression source changed during execution')
    result=dict(status='passed',assertions=current['assertions']+fault['assertions']+55+5,cases=6,current_cases=5,faulty_cases=1,
        foreground_profile_toctou_rejected=True,faulty_guard_removal_reproduces_128=True,
        stable_profiles_preserved=True,preflight_closed_on_all_paths=True,
        actual_foreground_producer_executed=True,controlled_peer_fixture=True,
        native_tested=False,physical_android_tested=False,device_speedup_verified=False,
        fault_scope='Only the two-line foreground profile guard removed from an exact pinned producer copy',
        current=current,faulty=fault,source_sha256=pins,faulty_producer_sha256=sha(faulty),
        current_shape_profile_toctou_rejected19115=True,faulty_shape_guard_removal_reproduced19115=True,
        shape_current19115=shape_current,shape_faulty19115=shape_fault,generated_shape_peers_sha25619115=sha(shape_peers))
    result.update(source_proof)
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=ROOT);parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path)
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
