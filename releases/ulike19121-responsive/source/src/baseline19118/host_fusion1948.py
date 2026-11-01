#!/usr/bin/env python3
"""Compare optimized temporal fusion against the immutable public 1.9.47 kernel.

Real shared worker implementation; synthetic owned full NV21 captures only.
Every result byte, reference field, motion bit, and source input is checked.
No Android device performance or camera verification is inferred from this test.
"""
from pathlib import Path
import hashlib,json,os,shutil,subprocess

BASELINE_ORIGINAL_SHA256='b8726da9e4fd1ad6a377c6c645875ac94b22576bca6195a876b7e106a958ad1f'

def java_tool(name):
    for variable in ('ULIKE_JDK_HOME','JAVA_HOME'):
        value=os.environ.get(variable)
        if value and (Path(value)/'bin'/name).is_file():return [str(Path(value)/'bin'/name)]
    found=shutil.which(name)
    if found:return [found]
    if name=='javac' and shutil.which('java'):return [shutil.which('java'),'com.sun.tools.javac.Main']
    raise RuntimeError(name+' not available')

def test(root,work):
    root=Path(root);work=Path(work)/'fusion1948';work.mkdir(parents=True,exist_ok=True)
    baseline=(root/'host_fusion_baseline1947.java.txt').read_text()
    original=baseline.replace('BaselineFusion1933','FusionPixels1933')
    if hashlib.sha256(original.encode()).hexdigest()!=BASELINE_ORIGINAL_SHA256:
        raise RuntimeError('Public baseline SHA256 mismatch')
    (work/'BaselineFusion1933.java').write_text(baseline)
    (work/'FusionEquivalence1948.java').write_text((root/'host_fusion_suite1948.java.txt').read_text())
    classes=work/'classes';classes.mkdir(exist_ok=True)
    sources=[root/'FusionPixels1933.java',root/'GpuInteger1949.java',root/'SpeedWorkers1935.java',work/'BaselineFusion1933.java',work/'FusionEquivalence1948.java']
    subprocess.run(java_tool('javac')+['-encoding','UTF-8','-source','8','-target','8','-d',str(classes)]+list(map(str,sources)),check=True,capture_output=True,text=True)
    run=subprocess.run(java_tool('java')+['-Xmx768m','-cp',str(classes),'com.hiro.ulike.FusionEquivalence1948'],check=True,capture_output=True,text=True,timeout=300)
    result=json.loads(run.stdout.strip())
    if result.get('status')!='passed' or result.get('assertions',0)<=0 or result.get('actual_fusion_results',0)<15:
        raise RuntimeError('Executed exact fusion assertions missing')
    result.update(baseline_public_version='1.9.47',baseline_commit='97dc6fa94680beadc94e135f01042a82b8eedf30',
        baseline_sha256=BASELINE_ORIGINAL_SHA256,full_resolution_checked='4080x3060',
        native_denoise_changed=False,gpu_execution_claimed=False,performance_measured_on_device=False)
    (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
    return result

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--root',type=Path,default=Path(__file__).resolve().parent);p.add_argument('--work',type=Path,required=True)
    a=p.parse_args();print(json.dumps(test(a.root,a.work),ensure_ascii=False))
