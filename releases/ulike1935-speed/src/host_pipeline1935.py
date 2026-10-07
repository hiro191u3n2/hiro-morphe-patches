#!/usr/bin/env python3
"""S6/S16 pool lifecycle and frozen1934 normalize/resize output comparison."""
from pathlib import Path
import argparse,json,subprocess

def test(root,work):
 root=Path(root);work=Path(work);out=work/'host-pipeline1935';classes=out/'classes';classes.mkdir(parents=True,exist_ok=True)
 sources=list((root/'tests/pipeline1935-fixtures').rglob('*.java'))+[root/n for n in ('SpeedWorkers1935.java','QualityPixels1932.java','QualityPipeline1932.java','QualityShadow1932.java','SpatialNoise1934.java','LongMoire1934.java','tests/reference/ReferencePipeline1934.java','tests/reference/ReferencePixels1934.java','tests/PipelineSpeed1935Test.java','tests/SpeedWorkers1935Test.java')]
 built=subprocess.run(['javac','-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],capture_output=True,text=True,timeout=120)
 (out/'compile.log').write_text(built.stdout+built.stderr)
 if built.returncode:raise RuntimeError(built.stderr[-8000:])
 results={}
 for name in ['SpeedWorkers1935Test','PipelineSpeed1935Test']:
  run=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.'+name],capture_output=True,text=True,timeout=120)
  (out/(name+'.log')).write_text(run.stdout+run.stderr)
  if run.returncode:raise RuntimeError(run.stderr[-8000:])
  results[name]=json.loads(run.stdout)
 result={'status':'passed','assertions':sum(r['assertions'] for r in results.values()),'scenarios':sum(r['scenarios'] for r in results.values()),'tests':results,'physical_device_verified':False}
 (out/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out)))
