#!/usr/bin/env python3
"""Execute current GX23 service against explicit capture/storage fault fixtures."""
from pathlib import Path
import json,os,re,subprocess

def test(root,work,jdk=None,ndk=None):
    root=Path(root).resolve();work=Path(work).resolve()/'host-qualification1961';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);sources=[root/'GpuQualification1961.java',root/'tests1961/Qualification1961Test.java']+sorted((root/'tests1961/qualification-fixtures').rglob('*.java'))
    for name,command in [('compile',[jdk/'bin/javac','-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',classes,*sources]),('run',[jdk/'bin/java','-cp',classes,'com.hiro.ulike.Qualification1961Test'])]:
        result=subprocess.run(list(map(str,command)),capture_output=True,text=True,timeout=60);(work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(str(work/(name+'.log'))+'\n'+result.stdout[-3000:]+result.stderr[-9000:])
    found=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not found:raise AssertionError('No actual service test report')
    report=json.loads(found.group(1));report['fixtures']='Conforming staged SharedPreferences, controlled capture epoch/queue/GL readiness. No physical Android performance claim.'
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
