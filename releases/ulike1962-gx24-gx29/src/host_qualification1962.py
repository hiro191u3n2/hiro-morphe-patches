#!/usr/bin/env python3
"""Exercise actual GX29 cooldown, persistence and cancellation decisions."""
from pathlib import Path
import json,os,re,subprocess

def helper_paths(root,work,jdk):
    # Compile the unchanged production source closure, then replace only its
    # transport ABI with controlled responses for hard/temporary fault branches.
    # Full production shader execution is independently covered by host_gpu1962.
    from host_gpu1962 import all_sources
    classes=work/'helper-classes';classes.mkdir(parents=True,exist_ok=True)
    sources=[p for p in all_sources(root) if p.name!='FastResize1933.java']+[root/'FastResize1933.java']
    overlay=[root/'tests1962/helper-fault-fixtures/com/hiro/ulike/GpuNoise1960.java',root/'tests1962/Gx29HelperFault1962Test.java']+[root/(name+'.java') for name in ('GpuPolicy1960','GpuSingle1960','GpuProtection1961','GpuGeometry1960')]
    for name,command in [
        ('helper-base-compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources]),
        ('helper-overlay-compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-cp',classes,'-d',classes,*overlay]),
        ('helper-run',[jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',classes,'com.hiro.ulike.Gx29HelperFault1962Test'])]:
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=90);(work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(str(work/(name+'.log'))+'\n'+result.stdout[-3000:]+result.stderr[-9000:])
    found=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not found:raise AssertionError('No actual production helper classification report')
    return json.loads(found.group(1))

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'host-qualification1962';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);sources=[root/'GpuQualification1961.java',root/'tests1961/Qualification1961Test.java',root/'tests1962/Gx29Qualification1962Test.java']+sorted((root/'tests1961/qualification-fixtures').rglob('*.java'))
    for name,command in [('compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources]),('run',[jdk/'bin/java','-cp',classes,'com.hiro.ulike.Gx29Qualification1962Test'])]:
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=60);(work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(str(work/(name+'.log'))+'\n'+result.stdout[-3000:]+result.stderr[-9000:])
    found=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not found:raise AssertionError('No actual GX29 retry-policy test report')
    report=json.loads(found.group(1));report['fixtures']='Production qualification scheduler and store; controlled elapsed cooldown clock, capture epoch, save queue and GL identity. Production helper probes use controlled GPU transport; shader equivalence is verified separately. No physical Android speed claim.'
    report['helper_paths']=helper_paths(root,work,jdk);report['assertions']+=report['helper_paths']['assertions']
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
