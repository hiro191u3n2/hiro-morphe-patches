#!/usr/bin/env python3
"""Run production FaceRegions/FaceResult/PhotoAnalysis against controlled Android.

The immutable .103 source is a negative control: successful pre-analysis causes
two real detector-owner calls because its completion hooks were lost. The fixed
source must retain identical masks and failure behavior while calling it once.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess

BASE_SHA='bd33d625aa8a83ca28038eca25a673a979bb23b2eb429d54821c451e4d84f9fa'
FIXTURES={
'android/graphics/Bitmap.java':'''package android.graphics; public final class Bitmap {
 public enum Config{ARGB_8888,RGB_565,HARDWARE,RGBA_F16} public int width,height,generation=1; public int[] pixels; public boolean recycled; public Config config=Config.ARGB_8888; public static boolean failCreate;
 public Bitmap(int w,int h){width=w;height=h;pixels=new int[w*h];} public static Bitmap createBitmap(int w,int h,Config c){if(failCreate)throw new OutOfMemoryError("controlled allocation");Bitmap b=new Bitmap(w,h);b.config=c;return b;}
 public int getWidth(){return width;}public int getHeight(){return height;}public int getGenerationId(){return generation;}public int getDensity(){return 160;}public boolean hasAlpha(){return false;}public boolean isPremultiplied(){return true;}public Config getConfig(){return config;}public ColorSpace getColorSpace(){return null;}public boolean hasGainmap(){return false;}public boolean isRecycled(){return recycled;}public void recycle(){recycled=true;}
 public void getPixels(int[] out,int off,int stride,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,out,off+row*stride,w);}}
''',
'android/graphics/ColorSpace.java':'package android.graphics;public class ColorSpace {public boolean isSrgb(){return true;}}',
'android/graphics/Canvas.java':'package android.graphics;public final class Canvas {final Bitmap to;public static boolean fail;public Canvas(Bitmap b){to=b;}public void drawBitmap(Bitmap b,Matrix m,Paint p){if(fail)throw new IllegalStateException("controlled canvas");for(int y=0;y<to.height;y++)for(int x=0;x<to.width;x++)to.pixels[y*to.width+x]=b.pixels[Math.min(b.height-1,y)*b.width+Math.min(b.width-1,x)];}}',
'android/graphics/Matrix.java':'package android.graphics;public final class Matrix {public void setValues(float[] values){}}',
'android/graphics/Paint.java':'package android.graphics;public final class Paint {public static final int FILTER_BITMAP_FLAG=2;public Paint(int flags){}}',
'android/graphics/PointF.java':'package android.graphics;public final class PointF {public float x,y;}',
'android/media/FaceDetector.java':'''package android.media;import android.graphics.*;import java.util.concurrent.atomic.AtomicInteger;public final class FaceDetector {
 public static final ThreadLocal<Integer> mode=new ThreadLocal<Integer>(){protected Integer initialValue(){return 0;}};public static final AtomicInteger calls=new AtomicInteger();public FaceDetector(int w,int h,int n){}
 public int findFaces(Bitmap b,Face[] faces){calls.incrementAndGet();int m=mode.get();if(m==4)throw new IllegalStateException("controlled detector");if(m==5)throw new LinkageError("controlled linkage");if(m==2)return -1;if(m==3)return 9;if(m==0)return 0;faces[0]=new Face(m==6);return 1;}
 public static final class Face {public static final int EULER_Z=2,EULER_Y=1;final boolean invalid;Face(boolean b){invalid=b;}public void getMidPoint(PointF p){p.x=32;p.y=28;}public float eyesDistance(){return 20;}public float pose(int key){return 0;}public float confidence(){return invalid?0:.95f;}}}
''',
'android/os/Build.java':'package android.os;public class Build {public static class VERSION{public static int SDK_INT=34;}}',
'com/hiro/ulike/FacePeers19104.java':'''package com.hiro.ulike;import android.graphics.Bitmap;
final class QualityPixels1932 {interface RegionMask{int skinQ8(int x,int y);int detailQ8(int x,int y);}}
final class GpuProtection1961 {static boolean available(){return false;}static boolean mayTryFace(int w,int h,int n){return false;}static int[] faceRaster(int[] p,int w,int h,int[][] g){throw new AssertionError();}}
final class PhotoDetail {static class Settings {final boolean noiseOn=true,sharpOn=true,texturePriority=true;final int noiseLevel=4,sharpLevel=4;}}
final class ShotContext1932 {static class Snapshot{}}
final class SaveMemory1981 {static class Plan {final int outputWidth,outputHeight;Plan(int w,int h){outputWidth=w;outputHeight=h;}boolean valid(){return true;}}}
final class ProcessingTiming1947 {static class Trace{}static class Scope{}static class Token{}static final int CORRECTION=1,NOISE=2;static Scope enter(Trace t){return null;}static void restore(Scope s){}static Token beginStage(Trace t,int k){return null;}static void end(Token t){}static Trace traceFor(Bitmap b){return null;}}
final class SpatialNoise1934 {interface Patches{void read(int[] p,int x,int y,int w,int h);}static SpatialNoise1934 probe(Patches p,int w,int h){throw new AssertionError("noise not part of face test");}}
final class AsyncSave1935 {static final ThreadLocal<PhotoAnalysis1981.Context> context=new ThreadLocal<PhotoAnalysis1981.Context>();static PhotoAnalysis1981.Context photoContext1981(Bitmap b){return context.get();}static boolean analysisMemoryAllows1981(long b){return true;}}
final class SaveQueue1935 {static boolean analysisWaiting1981(Runnable r){return true;}}
final class SpeedWorkers1935 {static boolean cpuIdle1944(){return true;}static boolean tryEnterAnalysis1981(){return true;}static void leaveAnalysis1981(boolean value){}}
'''
}

def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True);jdk=Path(jdk or os.environ['JAVA_HOME'])
 sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
 baseline=source/'tests19104/face/reference103/FaceRegions1934.java';current=source/'quality-dependencies/com/hiro/ulike/FaceRegions1934.java'
 if sha(baseline)!=BASE_SHA:raise AssertionError('Frozen .103 FaceRegions source changed')
 production=[source/'FaceResult1981.java',source/'PhotoAnalysis1981.java',source/'PairedRegions1976.java',source/'quality-dependencies/com/hiro/ulike/FaceRegions1934Pixels.java']
 harness=source/'tests19104/face/FaceReuse19104Test.java';tracked=production+[baseline,current,harness,Path(__file__).resolve()];pins={str(p.relative_to(source)):sha(p) for p in tracked}
 fixtures=[]
 for name,body in FIXTURES.items():
  p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);fixtures.append(p)
 results={}
 for tag,face,fixed in [('baseline103',baseline,False),('current104',current,True)]:
  classes=work/tag;classes.mkdir(exist_ok=True)
  for label,args in [('compile',[jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*production,face,harness,*fixtures]),('run',[jdk/'bin/java','-ea','-Xmx256m','-cp',classes,'com.hiro.ulike.FaceReuse19104Test',str(fixed).lower()])]:
   p=subprocess.run(list(map(str,args)),capture_output=True,text=True,timeout=120);output=p.stdout+p.stderr;(work/(tag+'-'+label+'.log')).write_text(output)
   if p.returncode:raise AssertionError(tag+' '+label+'\n'+output[-12000:])
  results[tag]=json.loads(output.split('RESULT ',1)[1])
 old,new=results['baseline103'],results['current104']
 if old['mask_sha256']!=new['mask_sha256'] or old['avoided_detector_calls']!=0 or new['avoided_detector_calls']!=2:raise AssertionError('Face masks changed or detector work not removed')
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Face proof source changed')
 result={'status':'passed','assertions':sum(r['assertions'] for r in results.values()),'tests':results,'baseline_double_detection_reproduced':True,'successful_preanalysis_reused':True,'same_mask_outputs':True,'failure_fallback_preserved':True,'identity_guards_preserved':True,'thread_local_completion_verified':True,'physical_android_tested':False,'device_speedup_verified':False,'source_sha256':pins}
 (work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser()
 for name in ('source','work'):p.add_argument('--'+name,required=True)
 p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
