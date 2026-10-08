#!/usr/bin/env python3
"""Same-pixel whole-image oracle, bounded overlapping CPU work and adaptive-profile guards."""
from pathlib import Path
import argparse,json,os,shutil,subprocess

def test(root,work,android=None):
 root=Path(root);out=Path(work)/'host-scheduling1944';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
 compiler=[javac] if javac else [java,'com.sun.tools.javac.Main']
 names=('SpeedWorkers1935','Scheduling1944','QualityPixels1932','PolicyCache1945','QualityPipeline1932','QualityShadow1932','SpatialNoise1934','LongMoire1934','NoiseCache1944','NativeSpeed1944')
 sources=list((root/'tests/pipeline1942-fixtures').rglob('*.java'))+[root/(n+'.java') for n in names]+[root/'tests/Scheduling1944Test.java']
 sources += list((root/'tests/timing1947-fixtures').rglob('*.java'))+[root/'ProcessingTiming1947.java']
 result=subprocess.run(compiler+['-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=120)
 (out/'compile.log').write_text(result.stdout+result.stderr)
 if result.returncode:raise RuntimeError(result.stderr[-8000:])
 result=subprocess.run([java,'-XX:ActiveProcessorCount=4','-cp',str(classes),'com.hiro.ulike.Scheduling1944Test'],capture_output=True,text=True,timeout=180)
 (out/'test.log').write_text(result.stdout+result.stderr)
 if result.returncode:raise RuntimeError(result.stderr[-8000:])
 report=json.loads(result.stdout);report['fixture_scope']='Production strip/final pixels and CPU scheduling execute; Android Bitmap/camera/primary NR are modeled host fixtures. Native optional acceleration is not loaded.'
 (out/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out)))
