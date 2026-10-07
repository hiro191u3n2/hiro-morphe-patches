#!/usr/bin/env python3
"""Production scheduler, local NR mix and shadow cleanup with host collaborators."""
from pathlib import Path
import argparse,json,subprocess

def test(root,work):
 root=Path(root);work=Path(work);classes=work/'scheduler-classes';classes.mkdir(parents=True,exist_ok=True)
 sources=list((root/'tests/scheduler-fixtures').rglob('*.java'))+[root/n for n in ('SpeedWorkers1935.java','QualityPixels1932.java','QualityPipeline1932.java','QualityShadow1932.java','SpatialNoise1934.java','LongMoire1934.java','tests/SchedulerQuality1934Test.java')]
 subprocess.run(['javac','-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,sources)],check=True)
 result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.SchedulerQuality1934Test'],capture_output=True,text=True,check=True)
 data=json.loads(result.stdout);(work/'scheduler1934.json').write_text(json.dumps(data,indent=2)+'\n');return data

def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,a.out)))
if __name__=='__main__':main()
