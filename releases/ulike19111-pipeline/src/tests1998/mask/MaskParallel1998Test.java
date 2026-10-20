package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;
public final class MaskParallel1998Test {
 static long checks,pixels;static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static Object get(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 static void pixels(GpuFinishPolicy1992.Context c,GpuFinishPolicy1992.Fixture f,int first,int last)throws Exception{
  int[] masks=(int[])get(c,"masks");int at=0;
  for(int y=first;y<last;y++)for(int x=0;x<f.width;x++,at++){
   int skin=f.plan.texturePriority&&f.plan.faceRegions!=null?f.plan.skinAt(x,y):0;
   int smooth=f.plan.smoothedRegions==null?0:f.plan.smoothedRegions.smoothingQ8(x,y);
   check(masks[at]==(skin|(smooth<<16)),"parallel cache produces unchanged packed mask after writers join");pixels++;
  }
 }
 static void run(int id)throws Exception{
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(id,1025,257);
  GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"real JNI session open");GpuFinishPolicy1992.Context c=null;
  try{
   c=GpuFinishPolicy1992.open(session,f.plan,f.width,f.height,128);check(c!=null,"real lease admits cache context");
   GpuFinishPolicy1992.RowSampler1998[] cache=(GpuFinishPolicy1992.RowSampler1998[])get(c,"skinRows");check(cache!=null,"0/180 degrees use cache in real parallel pack");
   for(int first:new int[]{0,128,256,0}){
    int last=Math.min(first+128,f.height);GpuFinishPolicy1992.Band b=c.prepare(first,last,(first/128)&1);
    try{pixels(c,f,first,last);boolean blocked=false;try{c.prepare(0,128,0);}catch(IllegalArgumentException expected){blocked=true;}check(blocked,"open band exclusively owns pack and cache writers");}finally{b.close();}
   }
   // The published private descriptor remains executable, but has no worker
   // identity. Its compatibility bridge must not touch any writable cache.
   long builds=0;for(GpuFinishPolicy1992.RowSampler1998 row:cache)builds+=row.rowsBuilt;
   java.lang.reflect.Method legacy=c.getClass().getDeclaredMethod("pack",int.class,int.class,int.class);legacy.setAccessible(true);legacy.invoke(c,0,128,0);pixels(c,f,0,128);
   long after=0;for(GpuFinishPolicy1992.RowSampler1998 row:cache)after+=row.rowsBuilt;check(after==builds,"published private pack bridge uses uncached original path");
   // Every worker has already filled different source rows. Retire keys before
   // introducing an invalid index, then ensure failure joins all pack tasks.
   for(GpuFinishPolicy1992.RowSampler1998 row:cache){row.keyA=row.keyB=-1;}
   int original=cache[0].source.x.lo[0];cache[0].source.x.lo[0]=Integer.MAX_VALUE;boolean failed=false;
   try{c.prepare(0,128,0);}catch(IndexOutOfBoundsException expected){failed=true;}finally{cache[0].source.x.lo[0]=original;}
   check(failed,"injected worker fill error reaches caller");
   GpuFinishPolicy1992.Band b=c.prepare(0,128,0);try{pixels(c,f,0,128);}finally{b.close();}
   Thread.currentThread().interrupt();boolean cancelled=false;try{c.prepare(0,128,0);}catch(CancellationException expected){cancelled=true;}finally{check(Thread.currentThread().isInterrupted(),"prepare cancellation preserves caller interrupt");Thread.interrupted();}check(cancelled,"cancelled prepare publishes no band");
   b=c.prepare(128,256,1);try{pixels(c,f,128,256);}finally{b.close();}
  }finally{if(c!=null)c.close();session.close();}
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"all cache budget and JNI session retired");
 }
 public static void main(String[] args)throws Exception{run(4);run(10);System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"parallel_compared_pixels\":"+pixels+",\"worker_failure_join_verified\":true,\"cancel_reuse_verified\":true,\"actual_jni\":true,\"physical_android_tested\":false}");}
}
