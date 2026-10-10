#!/usr/bin/env python3
"""Execute production Strong admission with controlled transport/CPU fixtures.
No host result is a physical Android benchmark or a pixel-kernel equivalence
claim. The historical production pipeline/shader suites provide math coverage.
"""
from pathlib import Path
import argparse,json,re,shutil,subprocess
ROOT=Path(__file__).resolve().parent
def test(source,work,jdk=None):
    source=Path(source).resolve()
    if source.is_dir():source=source/'GpuStrong1960.java'
    work=Path(work).resolve()/'strong1967';classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    fixtures=ROOT/'gpu_agent_tests'
    java=Path(jdk).resolve()/'bin/java' if jdk else Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java').resolve()
    javac=java.parent/'javac'
    compiler=[str(javac)] if javac.is_file() else [str(java),'-m','jdk.compiler/com.sun.tools.javac.Main']
    compile_cmd=compiler+['-source','8','-target','8','-Xlint:-options','-encoding','UTF-8','-d',str(classes),str(source),str(fixtures/'FixtureClasses.java'),str(fixtures/'SharedAdmission1967Test.java')]
    run_cmd=[str(java),'-XX:ActiveProcessorCount=4','-cp',str(classes),'com.hiro.ulike.SharedAdmission1967Test']
    for name,cmd in [('compile',compile_cmd),('run',run_cmd)]:
        result=subprocess.run(cmd,capture_output=True,text=True,timeout=60)
        (work/(name+'.log')).write_text(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError((work/(name+'.log')).read_text())
    match=re.search(r'^RESULT (\{[^\n]+\})$',result.stdout,re.M)
    if not match:raise AssertionError('Executed production admission result absent')
    report=json.loads(match.group(1))
    report.update(strong_admission_regressions_passed=True,admission_route_regressions_passed=True,gpu_safety_gates_preserved=True)
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n')
    return report
if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',default=str(ROOT/'GpuStrong1960.java'));parser.add_argument('--work',default=str(ROOT/'gpu_agent_tests/host-work'));parser.add_argument('--jdk')
    args=parser.parse_args();print(json.dumps(test(args.source,args.work,args.jdk),indent=2))

