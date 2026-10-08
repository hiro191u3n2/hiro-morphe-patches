#!/usr/bin/env python3
"""Run production N1–N3 pixels against image-domain fixtures in a real JVM."""
from pathlib import Path
import argparse,json,subprocess,shutil,os

def test(root,work):
 root=Path(root);work=Path(work);out=work/'host-noise1941';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 compiler=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 command=[compiler] if compiler else ['java','com.sun.tools.javac.Main']
 sources=[root/name for name in ('SpeedWorkers1935.java','QualityPixels1932.java','PolicyCache1945.java','QualityShadow1932.java','NoiseCache1944.java','NativeSpeed1944.java','GpuInteger1949.java','NativeSpeed1935.java','SpatialNoise1934.java','LongMoire1934.java','tests/NoiseLegacy1938.java','tests/NoiseQuality1941Test.java','tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')]
 built=subprocess.run(command+['-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=120)
 (out/'compile.log').write_text(built.stdout+built.stderr)
 if built.returncode:raise RuntimeError(built.stderr[-8000:])
 runtime=os.environ.get('ULIKE_JAVA') or (str(Path(compiler).with_name('java')) if compiler else 'java')
 run=subprocess.run([runtime,'-cp',str(classes),'com.hiro.ulike.NoiseQuality1941Test'],capture_output=True,text=True,timeout=120)
 (out/'test.log').write_text(run.stdout+run.stderr)
 if run.returncode:raise RuntimeError(run.stderr[-8000:])
 result=json.loads(run.stdout);(out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out)))
