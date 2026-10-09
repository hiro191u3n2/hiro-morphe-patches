#!/usr/bin/env python3
"""Run actual Strong foreground latency controls with CPU/transport fixtures.
No result establishes physical Android performance or shader pixel equivalence.
"""
from pathlib import Path
import argparse,hashlib,json,re,shutil,subprocess
ROOT=Path(__file__).resolve().parent
def test(source,work,jdk=None):
    source=Path(source).resolve()
    if source.is_dir():source=source/'GpuStrong1960.java'
    work=Path(work).resolve()/'strong_latency1973';classes=work/'classes'
    if classes.exists():shutil.rmtree(classes)
    classes.mkdir(parents=True,exist_ok=True)
    fixtures=ROOT/'latency73testfixtures'
    java=Path(jdk).resolve()/'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac=java.parent/'javac'
    compiler=[str(javac)] if javac.is_file() else [str(java),'-m','jdk.compiler/com.sun.tools.javac.Main']
    commands=[('compile',compiler+['-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',str(classes),str(source),str(fixtures/'FixtureClasses.java'),str(fixtures/'StrongLatency1973Test.java')]),('run',[str(java),'-XX:ActiveProcessorCount=4','-cp',str(classes),'com.hiro.ulike.StrongLatency1973Test'])]
    for name,cmd in commands:
        result=subprocess.run(cmd,capture_output=True,text=True,timeout=60)
        log=work/(name+'.log');log.write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(log.read_text())
    match=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not match:raise AssertionError('Executed production Strong foreground result absent')
    report=json.loads(match.group(1));report['production_source_sha256']=hashlib.sha256(source.read_bytes()).hexdigest()
    report.update(strong_latency_controls_regressions_passed=True,gpu_safety_gates_preserved=True,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',default=str(ROOT.parent/'strong/GpuStrong1960.java'));p.add_argument('--work',default=str(ROOT/'host-work'));p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
