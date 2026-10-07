package com.hiro.ulike;import java.util.*;
public final class SchedulerQuality1934Test {
 static int assertions;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static int gray(int n){return 0xff000000|(n<<16)|(n<<8)|n;}
 static QualityPixels1932.Plan plan(final int[] p,final int w,final int h,int noise){
  SpatialNoise1934 local=SpatialNoise1934.probe(new SpatialNoise1934.Patches(){public void read(int[] out,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(p,(y+row)*w+x,out,row*width,width);}},w,h);
  return QualityPixels1932.plan(local.global,400,10000000L,2,0,noise,0,true,true,1).withLocalNoise(local,noise);
 }
 static int[] run(int[] p,int w,int h,int core,int workers,QualityPixels1932.Plan plan)throws Exception {
  HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(plan);
  final ChromaPipeline186.State s=new ChromaPipeline186.State(p,w,h,core,plan.noiseLevel);
  HostAudit1932.local("CURRENT").remove();
  final java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
  Thread[] ts=new Thread[workers];for(int i=0;i<workers;i++)ts[i]=new Thread(new Runnable(){public void run(){try {
   HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(plan);
   HostAudit1932.<Integer>local("ROW_ORIGIN").set(928);
   QualityPipeline1932.run(s,new ChromaPipeline186.Buffer(s));
   if(HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").get()!=plan||HostAudit1932.<Integer>local("ROW_ORIGIN").get()!=928)throw new AssertionError("context restore");
  }catch(Throwable t){failure.compareAndSet(null,t);}}});
  for(Thread t:ts)t.start();for(Thread t:ts)t.join();check(failure.get()==null,"worker context restores");check(!s.failed,"workers succeed");return s.result;
 }
 public static void main(String[] args)throws Exception {
  int w=384,h=288;int[] p=new int[w*h];Random random=new Random(3934);
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)p[y*w+x]=gray((int)Math.round(100+(y<h/2?0:10)*random.nextGaussian()));
  int[] original=p.clone();QualityPixels1932.Plan plan=plan(p,w,h,3);int[] single=run(p,w,h,h,1,plan),tiled=run(p,w,h,37,4,plan);
  check(Arrays.equals(single,tiled),"scheduler absolute origins and shadow halos produce identical output");
  for(int y=20;y<60;y++)for(int x=20;x<w-20;x++)check(tiled[y*w+x]==Chroma186.corrected(p[y*w+x]),"clean NR budget must not restore original colour after correction");
  int[] off=run(p,w,h,43,3,plan(p,w,h,0));for(int i=0;i<p.length;i++)check(off[i]==Chroma186.corrected(p[i]),"NR OFF bypasses all added smoothing");
  check(Arrays.equals(p,original),"source immutable");System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_device_verified\":false}");
 }
}
