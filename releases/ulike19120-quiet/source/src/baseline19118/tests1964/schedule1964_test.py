#!/usr/bin/env python3
"""Actual production GX30 CPU worker proof and GX36 bank paths; timed transport."""
from pathlib import Path
import json, os, re, subprocess

def test(root,work,jdk=None,ndk=None):
    from host_gpu1962 import all_sources
    root=Path(root).resolve();work=Path(work).resolve()/'gx30-gx36-schedule1964';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME'])
    base=list(all_sources(root))
    overlay=[root/'GpuStrong1960.java',root/'GpuQualification1961.java',root/'SpeedWorkers1935.java',root/'tests1961/Qualification1961Test.java',root/'tests1964/Gx30Gx36Schedule1964Test.java']
    overlay+=sorted((root/'tests1961/qualification-fixtures').rglob('*.java'))
    overlay=[p for p in overlay if p.name!='GpuNoise1960.java']+[root/'tests1964/schedule-fixtures/com/hiro/ulike/GpuNoise1960.java']
    for name,cmd in [('base',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*base]),('overlay',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-cp',classes,'-d',classes,*overlay]),('run',[jdk/'bin/java','-XX:ActiveProcessorCount=4','-Djava.library.path='+str(work/'unavailable'),'-cp',classes,'com.hiro.ulike.Gx30Gx36Schedule1964Test'])]:
        result=subprocess.run(list(map(str,cmd)),capture_output=True,text=True,timeout=120);(work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(str(work/(name+'.log'))+'\n'+result.stdout[-4000:]+result.stderr[-9000:])
    found=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not found:raise AssertionError('Actual GX30/GX36 production-path report absent')
    report=json.loads(found.group(1));(work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
