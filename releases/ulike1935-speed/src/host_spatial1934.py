#!/usr/bin/env python3
"""Run shipped Q4/Q6/Q7 pixel primitives in a host JVM (not a camera/device test)."""
from pathlib import Path
import argparse, json, subprocess

def test(root,work):
    root=Path(root);work=Path(work);classes=work/'spatial-classes';classes.mkdir(parents=True,exist_ok=True)
    source=[root/name for name in ('SpeedWorkers1935.java','QualityPixels1932.java','SpatialNoise1934.java','LongMoire1934.java','tests/SpatialQuality1934Test.java')]
    subprocess.run(['javac','-source','8','-target','8','-Xlint:-options','-d',str(classes),*map(str,source)],check=True)
    result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.SpatialQuality1934Test'],check=True,capture_output=True,text=True)
    data=json.loads(result.stdout);(work/'spatial1934.json').write_text(json.dumps(data,indent=2)+'\n');return data

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args();print(json.dumps(test(Path(__file__).resolve().parent,args.out)))
if __name__=='__main__':main()
