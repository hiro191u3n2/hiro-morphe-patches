#!/usr/bin/env python3
"""Actual camera ZIP exporter with explicit scalar GPU-log delivery fixtures."""
import argparse,json,pathlib,re,subprocess
def test(root,work,jdk,ndk=None):
 root=pathlib.Path(root).resolve();work=pathlib.Path(work).resolve();jdk=pathlib.Path(jdk).resolve();work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 sources=[root/'CameraTrace1965.java',root/'tests1965/GpuTraceZip1966Test.java',*sorted((root/'tests1965/trace-fixtures').rglob('*.java'))]
 compile=subprocess.run([str(jdk/'bin/javac'),'-encoding','UTF-8','-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(work/'compile.log').write_text(compile.stdout)
 if compile.returncode:raise RuntimeError(compile.stdout)
 run=subprocess.run([str(jdk/'bin/java'),'-Xmx128m','-cp',str(classes),'com.hiro.ulike.GpuTraceZip1966Test',str(work/'data')],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(work/'run.log').write_text(run.stdout)
 if run.returncode:raise RuntimeError(run.stdout)
 report=json.loads(re.search(r'^RESULT (\{.*\})$',run.stdout,re.M).group(1));assert report['status']=='passed' and report['boundedGpuDiagnosticsIncluded'] and report['frozenCameraSnapshotPreserved'];(work/'gpu-trace1966.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--root',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1]);p.add_argument('--work',type=pathlib.Path,required=True);p.add_argument('--jdk',type=pathlib.Path,required=True);a=p.parse_args();print(json.dumps(test(a.root,a.work,a.jdk),indent=2))
