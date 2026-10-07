#!/usr/bin/env python3
"""Pixel equality: published v1943 reference vs production cached/native v1944.
Optional-workspace allocation faults are injected only in a generated test worker.
The production worker and all pixel-processing classes remain unchanged.
"""
from pathlib import Path
import argparse,json,subprocess,shutil,os

def test(root,work):
 root=Path(root);work=Path(work);out=work/'host-speed1944-noise';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 command=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
 runtime=os.environ.get('ULIKE_JAVA') or (str(Path(compiler).with_name('java')) if compiler else 'java')
 names=('QualityPixels1932.java','QualityShadow1932.java','NoiseCache1944.java','NativeSpeed1944.java','NativeSpeed1935.java','SpatialNoise1934.java','LongMoire1934.java','cache1944-reference/QualityShadowReference1943.java','tests/NoiseCache1944Test.java','tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')
 # Fault hooks are compiled in the test-only generated source, never production dex.
 fixture=out/'fault-fixture';fixture.mkdir(parents=True,exist_ok=True)
 worker=(root/'SpeedWorkers1935.java').read_text().replace('public final class SpeedWorkers1935 {','public final class SpeedWorkers1935 {\n    private static final AtomicInteger TEST_BORROWS=new AtomicInteger();')
 worker=worker.replace('public static int[] borrowInts(int length) {','public static int[] borrowInts(int length) {\n        String fault=System.getProperty("ulike.test.borrowFault", "none");\n        if("always".equals(fault) || ("once".equals(fault) && TEST_BORROWS.getAndIncrement()==0))throw new OutOfMemoryError("test optional workspace");')
 worker_path=fixture/'SpeedWorkers1935.java';worker_path.write_text(worker)
 built=subprocess.run(command+['-source','8','-target','8','-Xlint:-options','-d',str(classes),str(worker_path),*[str(root/name) for name in names]],capture_output=True,text=True,timeout=120)
 (out/'compile.log').write_text(built.stdout+built.stderr)
 if built.returncode:raise RuntimeError(built.stderr[-8000:])
 variants=[('java-cache',[]),('java-uncached-oom',['-Dulike.test.borrowFault=always'])]
 native=os.environ.get('ULIKE_HOST_NATIVE')
 if native:
  lib=['-Djava.library.path='+native]
  variants.extend([('native',lib),('native-workspace-oom',lib+['-Dulike.test.borrowFault=once']),('native-uncached-oom',lib+['-Dulike.test.borrowFault=always'])])
 results={}
 for label,extra in variants:
  run=subprocess.run([runtime,*extra,'-cp',str(classes),'com.hiro.ulike.NoiseCache1944Test'],capture_output=True,text=True,timeout=180)
  (out/(label+'.log')).write_text(run.stdout+run.stderr)
  if run.returncode:raise RuntimeError(label+': '+run.stderr[-8000:])
  results[label]=json.loads(run.stdout)
 result={'suite':'noise-cache1944','equal':all(v['equal'] for v in results.values()),'variants':results}
 (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out)))
