#!/usr/bin/env python3
from pathlib import Path
import subprocess,sys,json,hashlib
root=Path(__file__).resolve().parent
project=root.parent.parent
source=project/'src/runtime/UiOptions.java'
ecj=Path(sys.argv[1]) if len(sys.argv)>1 else project/'tools/ecj.jar'
out=root/'build';out.mkdir(parents=True,exist_ok=True)
sources=[source,root/'UiOptionsTest.java',*sorted((root/'stubs').rglob('*.java'))]
subprocess.run(['java','-XX:-UsePerfData','-jar',str(ecj),'-source','8','-target','8','-encoding','UTF-8','-d',str(out),*map(str,sources)],check=True)
r=subprocess.run(['java','-XX:-UsePerfData','-cp',str(out),'app.hiro.newxui.UiOptionsTest'],text=True,capture_output=True,check=True)
print(r.stdout,end='')
report={'result':'PASS','tested_real_source':'src/runtime/UiOptions.java','source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'host_test':r.stdout.strip(),'four_defaults_on':True,'chat_bars_views_require_restart':True,'refresh_requires_restart':False,'independent_toggle_combinations':16,'persistent_storage_tested':False,'android_settings_ui_tested':False,'fake_registry_not_for_packaging':True}
(root/'QA.json').write_text(json.dumps(report,indent=2)+'\n')
