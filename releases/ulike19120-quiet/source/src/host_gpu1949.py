#!/usr/bin/env python3
"""Exercise real Java gate against an explicitly mocked GPU backend."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess

def java_tool(name):
    for variable in ('ULIKE_JDK_HOME','JAVA_HOME'):
        value=os.environ.get(variable)
        if value and (Path(value)/'bin'/name).is_file():return [str(Path(value)/'bin'/name)]
    found=shutil.which(name)
    if found:return [found]
    if name=='javac' and shutil.which('java'):return [shutil.which('java'),'com.sun.tools.javac.Main']
    raise RuntimeError(name+' not available')

def test(root,work):
    root=Path(root);work=Path(work)/'gpu1949';work.mkdir(parents=True,exist_ok=True)
    classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=[root/'GpuInteger1949.java',root/'tests/GpuAdmission1949Test.java']
    subprocess.run(java_tool('javac')+['-encoding','UTF-8','-source','8','-target','8','-d',str(classes)]+list(map(str,sources)),check=True,capture_output=True,text=True)
    run=subprocess.run(java_tool('java')+['-Xmx768m','-cp',str(classes),'com.hiro.ulike.GpuAdmission1949Test'],check=True,capture_output=True,text=True,timeout=120)
    result=json.loads(run.stdout.strip())
    if result.get('status')!='passed' or result.get('assertions',0)<1000 or result.get('scenarios',0)<15:
        raise RuntimeError('Executed GPU admission assertions missing')
    result.update(test_adapter=True,physical_android_tested=False,gpu_execution_claimed=False,
        real_native_runtime_tested=False,device_speed_measured=False,transfer_inclusive_timing_gate=True,
        cold_equality_probes=1,timed_equality_speed_probes=2,reassessment_interval=64,
        sources={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources})
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    return result

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parent);p.add_argument('--work',type=Path,required=True)
    a=p.parse_args();print(json.dumps(test(a.root,a.work),ensure_ascii=False))
