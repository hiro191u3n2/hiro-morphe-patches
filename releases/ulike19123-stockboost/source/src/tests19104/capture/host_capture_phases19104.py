#!/usr/bin/env python3
from pathlib import Path
import argparse,importlib.util,json,subprocess

def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();jdk=Path(jdk).resolve();work.mkdir(parents=True,exist_ok=True)
 path=source/'tests19103/capture/host_capture19103.py';spec=importlib.util.spec_from_file_location('capture104_retained',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 base=module.test(source,work/'retained',jdk,ndk);classes=work/'retained/retained/classes'
 built=subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-cp',str(classes),'-d',str(classes),str(source/'tests19104/capture/CapturePhases19104Test.java')],text=True,capture_output=True)
 (work/'compile.log').write_text(built.stdout+built.stderr)
 if built.returncode:raise RuntimeError(built.stderr)
 run=subprocess.run([str(jdk/'bin/java'),'-ea','-cp',str(classes),'com.hiro.ulike.CapturePhases19104Test'],text=True,capture_output=True,timeout=60);(work/'test.log').write_text(run.stdout+run.stderr)
 if run.returncode:raise RuntimeError(run.stdout+run.stderr)
 result=json.loads(run.stdout.split('RESULT ',1)[1].strip());result['new_assertions']=result['assertions'];result['retained_assertions']=base['assertions'];result['assertions']+=base['assertions'];result['physical_android_tested']=False;result['device_speedup_verified']=False
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
