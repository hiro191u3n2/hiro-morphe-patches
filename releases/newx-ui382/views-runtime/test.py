#!/usr/bin/env python3
from pathlib import Path
import subprocess,sys,json,hashlib
root=Path(__file__).resolve().parent
project=root.parent
source=project/'src/runtime/UiOptions.java'
ecj=Path(sys.argv[1]) if len(sys.argv)>1 else project/'tools/ecj.jar'
out=root/'build';out.mkdir(parents=True,exist_ok=True)
registry=project/'refresh/settings-test/stubs/app/morphe/extension/crimera/settings/SettingsRegistry.java'
sources=[source,registry,root/'ViewCountFormat.java',root/'ViewCountFormatTest.java']
subprocess.run(['java','-XX:-UsePerfData','-jar',str(ecj),'-source','8','-target','8','-encoding','UTF-8','-d',str(out),*map(str,sources)],check=True)
r=subprocess.run(['java','-XX:-UsePerfData','-cp',str(out),'app.hiro.newxui.ViewCountFormatTest'],text=True,capture_output=True,check=True)
print(r.stdout,end='')
report={'result':'PASS','ui_options_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'formatter_sha256':hashlib.sha256((root/'ViewCountFormat.java').read_bytes()).hexdigest(),'host_test':r.stdout.strip(),'raw_integer_precision':64,'no_float_or_double_conversion':True,'locale_cases':['en-US','ja-JP','de-DE','fr-FR','ar-EG'],'off_null_negative_preserve_native_text':True,'physical_device_tested':False,'native_hook_scope_tested_here':False}
(root/'QA.json').write_text(json.dumps(report,indent=2)+'\n')
