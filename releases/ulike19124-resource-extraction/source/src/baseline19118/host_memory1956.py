#!/usr/bin/env python3
"""Execute real Java pool/native scratch accounting and trim lock-order checks."""
from pathlib import Path
import hashlib,json,os,re,subprocess
ROOT=Path(__file__).resolve().parent
def run(work,inputs=None,tools=None):
    work=Path(work)/'host-memory1956';work.mkdir(parents=True,exist_ok=True)
    jdk=os.environ.get('ULIKE_JDK_HOME')
    def tool(name):return str(Path(jdk)/'bin'/name) if jdk else name
    sources=[ROOT/'SpeedWorkers1935.java',ROOT/'tests/ScratchMemory1956Test.java']
    subprocess.run([tool('javac'),'-encoding','UTF-8','-d',str(work),*map(str,sources)],check=True,capture_output=True,text=True)
    result=subprocess.run([tool('java'),'-cp',str(work),'com.hiro.ulike.ScratchMemory1956Test'],check=True,capture_output=True,text=True,timeout=20)
    (work/'execution.log').write_text(result.stdout+result.stderr)
    match=re.search(r'PASS ScratchMemory1956 assertions=(\d+)',result.stdout)
    if not match:raise RuntimeError('Missing executed memory/ownership result')
    report={'status':'passed','assertions':int(match.group(1)),'source_sha256':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'native_budget_accounted':True,'trim_outside_java_pool_lock':True,'physical_android_tested':False}
    (work/'result.json').write_text(json.dumps(report,indent=2)+'\n')
    return report
if __name__=='__main__':
    import sys
    print(json.dumps(run(Path(sys.argv[1])),indent=2))
