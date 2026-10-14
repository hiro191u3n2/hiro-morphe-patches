package com.hiro.ulike;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;
/** Actual private strip I/O, explicit trace ownership and front scalar replay. */
public final class FrontIo1996Test {
 static int assertions;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static Object make(Class<?> c,Class<?>[] t,Object...a)throws Exception{Constructor<?> v=c.getDeclaredConstructor(t);v.setAccessible(true);return v.newInstance(a);}
 static Method method(Class<?> c,String n,Class<?>...t)throws Exception{Method m=c.getDeclaredMethod(n,t);m.setAccessible(true);return m;}
 public static void main(String[] args)throws Exception {
  final int w=19,h=19;final int[] source=new int[w*h];for(int i=0;i<source.length;i++)source[i]=0xff000000|((i*97177)&0xffffff);
  final Bitmap bitmap=Bitmap.from(w,h,source,Bitmap.Config.ARGB_8888,true);
  SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] p,int x,int y,int rw,int rh){bitmap.getPixels(p,0,rw,x,y,rw,rh);}},w,h);
  ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(bitmap);
  Class<?> st=Class.forName("com.hiro.ulike.QualityPipeline1932$Strips"),wt=Class.forName("com.hiro.ulike.QualityPipeline1932$Worker"),ht=Class.forName("com.hiro.ulike.QualityPipeline1932$CommittedHalf1959");
  Object strips=make(st,new Class<?>[]{Bitmap.class,int.class,int.class},bitmap,8,7);
  Object half=make(ht,new Class<?>[]{int.class,int.class},w,h);field(st,"committedHalf").set(strips,half);
  Object worker=make(wt,new Class<?>[]{st,QualityPixels1932.Plan.class,SingleNoise1955.Model.class,int.class,boolean.class},strips,null,model,0,false);
  try {
   check(field(wt,"singleTrace1982").get(worker)==owner,"worker captures exact bitmap owner");
   ((Runnable)worker).run();check(field(st,"failure").get(strips)==null,"real worker succeeds");
   check(Arrays.equals(bitmap.snapshot(),source),"observational I/O leaves no-noise image exact");
   check(owner.frontObserved1996==3,"one completed work snapshot perstrip, exhausted read excluded");
   for(int part:new int[]{0,1,3,5,7,8,9,10})check(owner.frontWork1996[part*2]==3,"boundary onceperstrip "+part);
   check(owner.frontWork1996[2*2]==0&&owner.frontWork1996[4*2]==0&&owner.frontWork1996[11*2]==0,"unexecuted mask native proof are absent");
   check(owner.frontRoutes1996[7]==3&&owner.frontRoutes1996[3]==3&&owner.frontRoutes1996[8]==3,"clean CPU fallback cause and completion");
   check(owner.backendCounts1982[0]==3&&owner.backendCounts1982[1]==0,"detail observation cannot alter selected backend count");
   int[] out=(int[])method(ht,"result").invoke(half);int[] oracle=FrontHalf1994Test.oracle(bitmap.snapshot(),w,h);
   check(Arrays.equals(out,oracle),"carried half unchanged after observed writes");
   long saved=owner.frontObserved1996;ProcessingTiming1947.finish(owner,true);
   SingleNoise1955.Workspace ws=(SingleNoise1955.Workspace)field(wt,"singleWorkspace").get(worker);
   PipelineDetail1988.front1996(owner,ws.frontWork1996,ws.frontRoutes1996);
   check(owner.frontObserved1996==saved,"late observation cannot mutate terminal owner");
  } finally {
   method(ht,"close").invoke(half);SpeedWorkers1935.release((int[])field(st,"preceding").get(strips));
   SpeedWorkers1935.release((int[])field(wt,"input").get(worker));SpeedWorkers1935.release((int[])field(wt,"output").get(worker));
   ((SingleNoise1955.Workspace)field(wt,"singleWorkspace").get(worker)).close();bitmap.recycle();SpeedWorkers1935.trim();
  }
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"controlled_bitmap_boundary\":true,\"physical_android_tested\":false}");
 }
}
