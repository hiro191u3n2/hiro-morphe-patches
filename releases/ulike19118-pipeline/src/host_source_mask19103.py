#!/usr/bin/env python3
"""Exercise the exact production immutable source-mask cache against frozen code.

Only Android detector/bitmap integration and the RegionMask type are test peers;
the original and new sampling math are the real production classes.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

def test(source,work,jdk=None,ndk=None):
 source,work,jdk=map(lambda p:Path(p).resolve(),(source,work,jdk or os.environ['ULIKE_JDK_HOME']));work.mkdir(parents=True,exist_ok=True)
 fixtures=work/'fixtures';classes=work/'classes';fixtures.mkdir(exist_ok=True);classes.mkdir(exist_ok=True)
 fixture_text={
  'QualityPixels1932.java':'package com.hiro.ulike; public final class QualityPixels1932 { public interface RegionMask {int skinQ8(int x,int y);int detailQ8(int x,int y);} }',
  'GpuProtection1961.java':'package com.hiro.ulike; final class GpuProtection1961 { static boolean available(){throw new AssertionError("detector not used");} static boolean mayTryFace(int w,int h,int n){throw new AssertionError("detector not used");} static int[] faceRaster(int[] p,int w,int h,int[][] g){throw new AssertionError("detector not used");} }',
  'FaceRowsReference19103.java':(source/'tests1976/cpu-mask75-reference/FaceRegions1934.java').read_text().replace('FaceRegions1934','FaceRowsReference19103').replace('FaceRowsReference19103Pixels','FaceRegions1934Pixels'),
 }
 for name,body in fixture_text.items():(fixtures/name).write_text(body)
 production=[source/'FaceResult1981.java',source/'quality-dependencies/com/hiro/ulike/FaceRegions1934.java',source/'quality-dependencies/com/hiro/ulike/FaceRegions1934Pixels.java',source/'PairedRegions1976.java']
 testfile=source/'tests19103/SourceMaskRows19103Test.java'
 inputs=production+[testfile]+list(fixtures.glob('*.java'))+list((source/'tests/h24h26/graphics').rglob('*.java'))
 for label,args in [('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*inputs]),('run',[jdk/'bin/java','-ea','-Xmx512m','-XX:ActiveProcessorCount=4','-cp',classes,'com.hiro.ulike.SourceMaskRows19103Test'])]:
  p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=180);(work/(label+'.log')).write_text(p.stdout+p.stderr)
  if p.returncode:raise AssertionError(label+' failed:\n'+p.stdout+p.stderr)
 result=json.loads(p.stdout.strip().splitlines()[-1]);result['source_sha256']={str(path.relative_to(source)):hashlib.sha256(path.read_bytes()).hexdigest() for path in production+[testfile,source/'host_source_mask19103.py']}
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for key in ('source','work','jdk'):p.add_argument('--'+key,required=True)
 a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
