#!/usr/bin/env python3
"""Compare exact pinned quality output before/after timing instrumentation."""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
ROOT=Path(__file__).resolve().parent
REFERENCE_SHA256='adcc6d5c982acdcd3d1e111d3f00fd1a1201811b40c45dd2dcb360582e0fa975'
def test(root,work,android=None):
 root=Path(root);out=Path(work)/'host-quality1947';out.mkdir(parents=True,exist_ok=True)
 baseline=root/'quality1947-reference/QualityPipeline1932.java'
 if hashlib.sha256(baseline.read_bytes()).hexdigest()!=REFERENCE_SHA256:raise RuntimeError('Unreviewed quality baseline')
 javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
 compiler=[javac] if javac else [java,'com.sun.tools.javac.Main']
 common=list((root/'tests/pipeline1942-fixtures').rglob('*.java'))+list((root/'tests/timing1947-fixtures').rglob('*.java'))+[root/n for n in ('SpeedWorkers1935.java','Scheduling1944.java','NoiseCache1944.java','NativeSpeed1944.java','GpuInteger1949.java','QualityPixels1932.java','PolicyCache1945.java','QualityShadow1932.java','SpatialNoise1934.java','LongMoire1934.java','ProcessingTiming1947.java','tests/PipelineQuality1942Test.java','tests/QualityGolden1947Test.java')]
 reports={}
 for name,pipeline in [('baseline',baseline),('instrumented',root/'QualityPipeline1932.java')]:
  target=out/name;target.mkdir(exist_ok=True)
  built=subprocess.run(compiler+['-source','8','-target','8','-Xlint:-options','-d',str(target),*map(str,common+[pipeline])],capture_output=True,text=True,timeout=120)
  (out/(name+'-compile.log')).write_text(built.stdout+built.stderr)
  if built.returncode:raise RuntimeError(built.stderr[-8000:])
  run=subprocess.run([java,'-XX:ActiveProcessorCount=4','-cp',str(target),'com.hiro.ulike.QualityGolden1947Test',name],capture_output=True,text=True,timeout=180)
  (out/(name+'-test.log')).write_text(run.stdout+run.stderr)
  if run.returncode:raise RuntimeError(run.stderr[-8000:])
  reports[name]=json.loads(run.stdout)
 if reports['baseline']['output_sha256']!=reports['instrumented']['output_sha256']:raise RuntimeError('Timing instrumentation changed image pixels')
 result={'status':'passed','baseline':reports['baseline'],'instrumented':reports['instrumented'],'same_output_pixels':True,'baseline_source_sha256':REFERENCE_SHA256,'fixture_scope':'Actual quality pipeline and timing core execute; Android Bitmap, primary noise transform and camera metadata are host fixtures.'}
 (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(ROOT,a.out)))
