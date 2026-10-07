package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.*;
public final class PipelineSpeed1935Test {
 static int assertions,scenarios;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 static int[] pixels(int w,int h){int[] p=new int[w*h];Random r=new Random(1935);for(int i=0;i<p.length;i++){int x=i%w,y=i/w;int g=40+r.nextInt(90);p[i]=0xff000000|((g+(x/8%2)*15)<<16)|(g<<8)|(g+(y/5%2)*12);}return p;}
 static void normalize(PhotoDetail.Settings settings,boolean enabled,int turn,int ow,int oh,boolean expectFace,boolean expectNoise) {
  int w=192,h=144;int[] p=pixels(w,h);Bitmap live=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true),old=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);
  ShotContext1932.Snapshot shot=new ShotContext1932.Snapshot(800,30000000L,ShotContext1932.LENS_FRONT,true,.6f);
  ShotContext1932.SHOTS.put(live,shot);ShotContext1932.SHOTS.put(old,shot);
  PhotoDetail.REQUEST.set(settings);SaveQuality2.SIZE.set(new int[]{ow,oh});ChromaPipeline177.enabled=enabled;
  FaceRegions1934.calls=0;int before=ChromaPipeline177.applies;
  QualityPixels1932.Plan outer=QualityPixels1932.plan(null,0,0,0,0,0,0,false,false,1);
  HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(outer);
  Bitmap now=QualityPipeline1932.normalize(live,turn,false);
  check(FaceRegions1934.calls==(expectFace?1:0),"face-analysis consumer pruning");
  int liveReads=live.reads;
  check(HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").get()==outer,"caller plan restored");
  HostAudit1932.local("CURRENT").remove();
  check(ChromaPipeline177.applies==before+1,"chroma call retained with filters OFF");
  check(ShotContext1932.forBitmap(now)==shot,"exact shot metadata copied");
  Bitmap prior=ReferencePipeline1934.normalize(old,turn,false);
  check(Arrays.equals(prior.snapshot(),now.snapshot()),"full normalized output equals frozen1934");
  check(Arrays.equals(live.snapshot(),p)&&Arrays.equals(old.snapshot(),p),"source retained unchanged");
  if(!expectNoise&&turn==0&&ow==w&&oh==h)check(liveReads<old.reads,"OFF skips spatial pixel reads");
  check(QualityPipeline1932.applyDetail(now,live,settings)==now,"completion prevents double filters");
  check(SaveQuality2.FALLBACK.get()==0,"normal paths do not silently fall back");
  if(now!=live)now.recycle();if(prior!=old)prior.recycle();live.recycle();old.recycle();scenarios++;
 }
 static void fallbackResize(){
  int w=193,h=145;final int[] p=pixels(w,h);for(int i=0;i<p.length;i+=7)p[i]&=0x7fffffff;
  for(int turn:new int[]{0,90,180,270})for(int[] size:new int[][]{{31,23},{111,97},{320,241},{193,145}}){
   Bitmap in=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);Bitmap a=QualityPipeline1932.resample(in,turn,size[0],size[1]),b=ReferencePipeline1934.resample(in,turn,size[0],size[1]);
   check(Arrays.equals(a.snapshot(),b.snapshot()),"fallback resize bit exact");
   if(a!=in)check(a.writes==(size[1]+15)/16,"fallback writes batched16rows");
   if(a!=in)a.recycle();if(b!=in)b.recycle();in.recycle();scenarios++;
  }
 }
 static void adaptiveCache(){
  final int w=400,h=300,ow=47,oh=35;final int[] p=pixels(w,h),live=new int[ow*oh],old=new int[ow*oh];final int[] calls=new int[2];
  QualityPixels1932.resize(new QualityPixels1932.RowSource(){public void readRow(int row,int[] out){calls[0]++;System.arraycopy(p,row*w,out,0,w);}},w,h,new QualityPixels1932.RowSink(){public void writeRow(int row,int[] out){System.arraycopy(out,0,live,row*ow,ow);}},ow,oh);
  ReferencePixels1934.resize(new ReferencePixels1934.RowSource(){public void readRow(int row,int[] out){calls[1]++;System.arraycopy(p,row*w,out,0,w);}},w,h,new ReferencePixels1934.RowSink(){public void writeRow(int row,int[] out){System.arraycopy(out,0,old,row*ow,ow);}},ow,oh);
  check(Arrays.equals(live,old),"adaptive cache preserves accumulation order and rounding");check(calls[0]<calls[1],"adaptive cache removes repeated horizontal rows");scenarios++;
 }
 static void concurrentFinish()throws Exception {
  final int w=112,h=389;final Bitmap[] live=new Bitmap[2],old=new Bitmap[2];final QualityPixels1932.Plan[] plans=new QualityPixels1932.Plan[2];
  for(int k=0;k<2;k++){int[] p=pixels(w,h);if(k==1)for(int i=0;i<p.length;i++)p[i]^=0x00030303;
   live[k]=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);old[k]=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);
   plans[k]=QualityPixels1932.plan(QualityPixels1932.estimate(p,w,h),400,20000000L,1,.4f,0,3,true,true,1);
   ReferencePipeline1934.finishInPlace(old[k],plans[k],true,true);
  }
  final java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<Throwable>();Thread[] ts=new Thread[2];
  for(int k=0;k<2;k++){final int shot=k;ts[k]=new Thread(new Runnable(){public void run(){try{QualityPipeline1932.finishInPlace(live[shot],plans[shot],true,true);}catch(Throwable e){error.set(e);}}});ts[k].start();}
  for(Thread t:ts){t.join(15000);check(!t.isAlive(),"parallel photo finishing drains");}
  check(error.get()==null,"parallel photo finishing succeeds");for(int k=0;k<2;k++){check(Arrays.equals(live[k].snapshot(),old[k].snapshot()),"parallel photos retain frozen pixels and halo sequencing");live[k].recycle();old[k].recycle();}scenarios++;
 }
 static void failure(){
  PhotoDetail.REQUEST.set(new PhotoDetail.Settings(false,0,false,0));ChromaPipeline177.enabled=true;SaveQuality2.SIZE.set(new int[]{192,144});
  Bitmap b=Bitmap.from(192,144,pixels(192,144),Bitmap.Config.ARGB_8888,true);Bitmap.failNextCopyWrite=true;
  int before=SaveQuality2.FALLBACK.get();Bitmap out=QualityPipeline1932.normalize(b,0,false);
  check(SaveQuality2.FALLBACK.get()==before+1,"failed save preparation uses legacy fallback");check(!b.isRecycled(),"fallback keeps input");
  check(HostAudit1932.local("CURRENT").get()==null,"failure restores current plan");check(Bitmap.writesAfterRecycle.get()==0,"no worker after recycle");
  out.recycle();b.recycle();scenarios++;
 }
 public static void main(String[] args)throws Exception{
  for(boolean color:new boolean[]{false,true}){
   normalize(new PhotoDetail.Settings(false,3,false,4),color,0,192,144,false,false);
   normalize(new PhotoDetail.Settings(true,0,true,0),color,90,144,192,false,false);
   normalize(new PhotoDetail.Settings(false,0,true,3,false,true,true),color,0,192,144,false,true);
   normalize(new PhotoDetail.Settings(false,0,true,3,true,true,true),color,0,192,144,true,true);
   normalize(new PhotoDetail.Settings(true,3,false,0),color,0,192,144,true,true);
   normalize(new PhotoDetail.Settings(true,3,true,3,true,true,true),color,0,192,144,true,true);
   normalize(new PhotoDetail.Settings(true,3,true,3,false,false,false),color,0,192,144,true,true);
   normalize(new PhotoDetail.Settings(false,0,false,0),color,270,96,128,false,false);
  }
  fallbackResize();adaptiveCache();concurrentFinish();failure();SpeedWorkers1935.trim();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"physical_device_verified\":false}");
 }
}
