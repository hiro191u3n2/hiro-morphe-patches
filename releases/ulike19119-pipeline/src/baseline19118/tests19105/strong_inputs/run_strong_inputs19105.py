#!/usr/bin/env python3
"""Real diagnostic orchestrator + real stage replay state, scripted kernel peers."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess
BITMAP='''package android.graphics; public final class Bitmap {public int[] pixels;public int reads;private final int w,h;public Bitmap(int width,int height){w=width;h=height;pixels=new int[w*h];}public int getWidth(){return w;}public int getHeight(){return h;}public void getPixels(int[] out,int offset,int stride,int x,int y,int width,int height){reads++;for(int row=0;row<height;row++)System.arraycopy(pixels,(y+row)*w+x,out,offset+row*stride,width);}}'''
def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 fixture=source/'tests19105/strong_inputs';files=[source/'StrongInputs19105.java',source/'ResidentStages19104.java',fixture/'StrongInputPeers19105.java',fixture/'StrongInputs19105Test.java'];tracked=files+[Path(__file__).resolve()];pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 bitmap=work/'fixtures/android/graphics/Bitmap.java';bitmap.parent.mkdir(parents=True,exist_ok=True);bitmap.write_text(BITMAP)
 def run(name,args):
  result=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=90);(work/(name+'.log')).write_text(result.stdout)
  if result.returncode:raise AssertionError(name+'\n'+result.stdout)
  return result.stdout
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*files,bitmap])
 output=run('run',[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.StrongInputs19105Test'])
 result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Production diagnostic changed during execution')
 result.update(source_sha256=pins,fixture_classes_in_runtime=False,physical_android_tested=False,device_speedup_verified=False)
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
