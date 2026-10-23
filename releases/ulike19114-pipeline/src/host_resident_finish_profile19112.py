#!/usr/bin/env python3
"""Foreground row-profile race regression using the actual admission producer.

Controlled peers model a readiness change during preflight. A second compiled
copy removes only the reviewed guard and must positively reproduce execution
of 128 rows under the restored 64-row whole proof. No pixel/native claim.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

ROOT=Path(__file__).resolve().parent
PRODUCER_SHA256='c3aaef4eb23a532bc0af2377266a4951fe39779533fce1d0d9cfb615f2618635'
GUARD='''                if(preflight.finishRows19112!=rowsFromKey19112(base)||
                        !routesFromKey19102(base).equals(deployedRoutes19102())){terminal1982=13;return null;}
'''

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def test(source,work,jdk=None,ndk=None):
    source=Path(source).resolve();work=Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']).resolve()
    producer=source/'GpuResident1976.java'
    if sha(producer)!=PRODUCER_SHA256:raise AssertionError('Reviewed foreground producer hash changed')
    fixture=source/'tests19112/finish_profile'
    peers=fixture/'ProfilePeers19112.java';harness=fixture/'ResidentFinishProfile19112Test.java'
    bitmap=source/'tests1982/pipeline/android/graphics/Bitmap.java'
    tracked=[producer,peers,harness,bitmap,source/'host_resident_finish_profile19112.py']
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
    if raw.count(GUARD)!=1:raise AssertionError('Exactly one reviewed foreground guard required')
    faulty=work/'faulty-source/GpuResident1976.java';faulty.parent.mkdir(exist_ok=True)
    faulty.write_text(raw.replace(GUARD,'',1))
    if faulty.read_text().replace('                Bitmap result=QualityPipeline1932.strongFinish1978(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false,identity);',GUARD+'                Bitmap result=QualityPipeline1932.strongFinish1978(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false,identity);',1)!=raw:
        raise AssertionError('Fault copy changed more than the foreground guard')
    fault_classes=work/'faulty';fault_classes.mkdir(exist_ok=True)
    run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',fault_classes,faulty,peers,harness,bitmap],'compile-faulty-guard-removal')
    fault=json.loads(run([jdk/'bin/java','-ea','-Xmx128m','-cp',fault_classes,'com.hiro.ulike.ResidentFinishProfile19112Test','faulty'],'run-faulty-positive-reproducer').split('RESULT ',1)[1])
    if fault.get('status')!='passed' or fault.get('cases')!=1 or fault.get('assertions')!=9 or fault.get('faulty_guard_removed') is not True:
        raise AssertionError('Removed guard did not positively reproduce unsafe 128-row execution')
    if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Profile regression source changed during execution')
    result=dict(status='passed',assertions=current['assertions']+fault['assertions'],cases=6,current_cases=5,faulty_cases=1,
        foreground_profile_toctou_rejected=True,faulty_guard_removal_reproduces_128=True,
        stable_profiles_preserved=True,preflight_closed_on_all_paths=True,
        actual_foreground_producer_executed=True,controlled_peer_fixture=True,
        native_tested=False,physical_android_tested=False,device_speedup_verified=False,
        fault_scope='Only the two-line foreground profile guard removed from an exact pinned producer copy',
        current=current,faulty=fault,source_sha256=pins,faulty_producer_sha256=sha(faulty))
    (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n')
    (work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=ROOT);parser.add_argument('--work',type=Path,required=True);parser.add_argument('--jdk',type=Path);parser.add_argument('--ndk',type=Path)
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk,args.ndk),indent=2))
