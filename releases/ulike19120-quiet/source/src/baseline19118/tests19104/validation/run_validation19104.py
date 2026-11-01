#!/usr/bin/env python3
"""Execute real bounded stage diagnostic; GPU/Android peers enforce call contracts only."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess
BITMAP='''package android.graphics; public final class Bitmap {public int[] pixels;private final int w,h;public Bitmap(int width,int height){w=width;h=height;pixels=new int[w*h];}public int getWidth(){return w;}public int getHeight(){return h;}public void getPixels(int[] out,int offset,int stride,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(pixels,(y+row)*w+x,out,offset+row*stride,width);}}'''
def test(source,work,jdk=None,ndk=None):
 source=Path(source).resolve();work=Path(work).resolve();jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);work.mkdir(parents=True,exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
 fixture=source/'tests19104/validation';production=source/'ResidentStages19104.java';files=[production,fixture/'StagePeers19104.java',fixture/'StageDiagnostic19104Test.java'];tracked=files+[Path(__file__).resolve()];pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
 bitmap=work/'fixtures/android/graphics/Bitmap.java';bitmap.parent.mkdir(parents=True,exist_ok=True);bitmap.write_text(BITMAP)
 def run(name,args):
  result=subprocess.run(list(map(str,args)),text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60);(work/(name+'.log')).write_text(result.stdout)
  if result.returncode:raise AssertionError(name+'\n'+result.stdout)
  return result.stdout
 run('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*files,bitmap])
 output=run('run',[jdk/'bin/java','-ea','-Xmx128m','-cp',classes,'com.hiro.ulike.StageDiagnostic19104Test'])
 result=json.loads(next(line[7:] for line in output.splitlines() if line.startswith('RESULT ')))
 if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Production stage diagnostic changed during execution')
 retained_path=source/'tests19103/validation/run_validation19103.py';spec=importlib.util.spec_from_file_location('retained_validation19103',retained_path);retained_module=importlib.util.module_from_spec(spec);spec.loader.exec_module(retained_module)
 retained=retained_module.test(source,work/'retained103',jdk,ndk)
 required=('first_difference_scalar_evidence','reference_candidate_incomplete_distinct','negative_admission_preserved','mismatch_survives_optional_replay_cancel','journal_v1_v2_v3_compatible')
 if retained.get('status')!='passed' or not all(retained.get(key) is True for key in required):raise AssertionError('Retained exact comparison and journal proofs not established')
 result.update(stage_assertions=result['assertions'],retained_assertions=retained['assertions'],assertions=result['assertions']+retained['assertions'],retained_validation=retained,original_mismatch_preserved=True,journal_v1_v2_v3_compatible=True)
 result.update(source_sha256=pins,fixture_classes_in_runtime=False,gpu_crop_scope='Executed mode0 dispatch/readback contract with scripted GPU; physical GPU not claimed')
 (work/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))
