#!/usr/bin/env python3
from pathlib import Path
import subprocess,sys,json,hashlib
root=Path(__file__).resolve().parent
ecj=Path(sys.argv[1]) if len(sys.argv)>1 else root.parent/'tools/ecj.jar'
out=root/'build/test-classes';out.mkdir(parents=True,exist_ok=True)
sources=[root/'RefreshRate.java',*sorted((root/'stubs').rglob('*.java')),*sorted((root/'test-stubs').rglob('*.java')),*sorted((root/'tests').rglob('*.java'))]
subprocess.run(['java','-XX:-UsePerfData','-jar',str(ecj),'-source','8','-target','8','-encoding','UTF-8','-d',str(out),*map(str,sources)],check=True)
r=subprocess.run(['java','-XX:-UsePerfData','-cp',str(out),'app.hiro.newxui.RefreshRateTest'],text=True,capture_output=True,check=True)
print(r.stdout,end='')
report={'result':'PASS','host_runtime_test':r.stdout.strip(),'runtime_sha256':hashlib.sha256((root/'RefreshRate.java').read_bytes()).hexdigest(),'public_android_apis_only':True,'minimum_api':26,'device_tested':False,'emulator_tested':False,'system_refresh_guaranteed':False}
(root/'QA.json').write_text(json.dumps(report,indent=2)+'\n')
