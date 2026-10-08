#!/usr/bin/env python3
"""Final-save N4 properties, using production pipeline and explicit Android/NR fixtures.

Checks meaningful invariants rather than equality with pre-change denoise pixels.
Does not claim a host fixture is an Android camera or encoder execution.
"""
from pathlib import Path
import argparse,json,os,shutil,subprocess

def test(root,work,android=None):
 root=Path(root);work=Path(work);out=work/'host-pipeline1942';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
 compiler=[javac] if javac else [java,'com.sun.tools.javac.Main']
 sources=list((root/'tests/pipeline1942-fixtures').rglob('*.java'))+[root/n for n in ('SpeedWorkers1935.java','Scheduling1944.java','NoiseCache1944.java','NativeSpeed1944.java','QualityPixels1932.java','PolicyCache1945.java','QualityPipeline1932.java','QualityShadow1932.java','SpatialNoise1934.java','LongMoire1934.java','tests/PipelineQuality1942Test.java','tests/PipelinePhoto1942Test.java')]
 built=subprocess.run(compiler+['-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=120)
 (out/'compile.log').write_text(built.stdout+built.stderr)
 if built.returncode:raise RuntimeError(built.stderr[-8000:])
 run=subprocess.run([java,'-cp',str(classes),'com.hiro.ulike.PipelineQuality1942Test'],capture_output=True,text=True,timeout=120)
 (out/'test.log').write_text(run.stdout+run.stderr)
 if run.returncode:raise RuntimeError(run.stderr[-8000:])
 result=json.loads(run.stdout);result['fixture_scope']='Android Bitmap, primary NR and colour transform are host fixtures; production final pipeline, residual NR, spatial map, resample and output sharp execute.'
 fixture_manifest=os.environ.get('ULIKE_PHOTO_FIXTURES')
 if fixture_manifest:
  manifest=json.loads(Path(fixture_manifest).read_text());arguments=[]
  for name in ('wall_central','wall_upper_right','wall_shadow_upper','wall_shadow_back'):
   region=manifest['regions'][name];arguments.extend([name,region['argb_file'],str(region['width']),str(region['height'])])
  photo=subprocess.run([java,'-cp',str(classes),'com.hiro.ulike.PipelinePhoto1942Test',*arguments],capture_output=True,text=True,timeout=180)
  (out/'photo-test.log').write_text(photo.stdout+photo.stderr)
  if photo.returncode:raise RuntimeError(photo.stderr[-8000:])
  result['private_photo_fixtures']=json.loads(photo.stdout)
  result['assertions']+=result['private_photo_fixtures']['assertions'];result['scenarios']+=result['private_photo_fixtures']['scenarios']
 (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out)))
