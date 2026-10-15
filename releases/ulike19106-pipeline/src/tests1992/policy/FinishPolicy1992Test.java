package com.hiro.ulike;
import android.graphics.Bitmap;
import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
public final class FinishPolicy1992Test {
 static int assertions;static long pixels;static boolean residentPolicyObserved;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void certificateStore()throws Exception {
  final HashMap<String,String> values=new HashMap<String,String>();final Object[] editor=new Object[1];
  editor[0]=Proxy.newProxyInstance(FinishPolicy1992Test.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("putString"))values.put((String)a[0],(String)a[1]);else if(n.equals("remove"))values.remove(a[0]);else if(n.equals("clear"))values.clear();else if(n.equals("commit"))return true;else if(n.equals("apply"))return null;return editor[0];}});
  Object preferences=Proxy.newProxyInstance(FinishPolicy1992Test.class.getClassLoader(),new Class[]{SharedPreferences.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("edit"))return editor[0];if(n.equals("getString"))return values.containsKey(a[0])?values.get(a[0]):a[1];if(n.equals("contains"))return values.containsKey(a[0]);if(n.equals("getAll"))return values;return null;}});
  Field f=GpuCertificates1990.class.getDeclaredField("preferences");f.setAccessible(true);f.set(null,preferences);f=GpuQualification1961.class.getDeclaredField("base");f.setAccessible(true);f.set(null,"host-policy1992-production-native");
 }
 static void ownership()throws Exception {
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(1);GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"ownership session");GpuFinishPolicy1992.Context context=null;GpuNoise1960.Lease1971 lease=null;
  try {
   final int[] calls={0};QualityPixels1932.Plan custom=f.plan.withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){calls[0]++;return 256;}public int detailQ8(int x,int y){return 0;}});
   long retained=GpuNoise1960.retainedBytes();check(GpuFinishPolicy1992.open(session,custom,f.width,f.height,7)==null&&calls[0]==0&&GpuNoise1960.retainedBytes()==retained,"unknown mask declines before callbacks or native commands");
   context=GpuFinishPolicy1992.open(session,f.plan,f.width,f.height,7);check(context!=null,"owned descriptor");lease=session.reserveCapacity1983(new int[]{3},new long[]{16L*f.width*7},new long[]{0},16L*f.width*7);check(lease!=null,"owned output budget");
   GpuFinishPolicy1992.Band band=context.prepare(0,7,0);check(band.uploadBytes==4L*f.width*7&&band.mode==FinishPolicy1953.RAW4,"packed masks transfer one int per pixel");boolean blocked=false;try{context.prepare(7,14,1);}catch(IllegalArgumentException expected){blocked=true;}check(blocked,"CPU staging cannot refill before submit owner closes");
   try{Native1960Test.setFault(2);check(session.execute(band.append(new GpuNoise1960.Batch(),3),new int[]{3},new int[]{4*f.width*7})==null,"injected native upload/read failure publishes no policy");}finally{Native1960Test.setFault(0);band.close();}
  }finally{if(lease!=null)lease.close();if(context!=null)context.close();session.close();}
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.retainedBytes()==0,"fault drains and releases all resident policy storage");
  session=GpuNoise1960.open();try{context=GpuFinishPolicy1992.open(session,f.plan,f.width,f.height,7);Thread.currentThread().interrupt();boolean stopped=false;try{context.prepare(0,7,0);}catch(CancellationException expected){stopped=true;}check(stopped&&Thread.currentThread().isInterrupted(),"band cancellation retains interruption");}finally{Thread.interrupted();if(context!=null)context.close();session.close();}
  check(!GpuNoise1960.sessionBusy(),"cancelled policy releases session");GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==0,"cancelled policy capacity is reclaimable after drain");
 }
 static void axes()throws Exception {
  // Independently evaluate the unchanged production callbacks. Small domains
  // are exhaustive; full photograph dimensions exercise random/boundary
  // coordinates without retaining a photograph-sized policy buffer.
  long checked=0;
  for(int id=0;id<GpuFinishPolicy1992.CASES;id++)for(int large=0;large<2;large++) {
   int width=large==0?257:(id&1)==0?4284:5712,height=large==0?263:(id&1)==0?5712:4284;
   GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(id,width,height);
   GpuFinishPolicy1992.Sampler skin=GpuFinishPolicy1992.sampler(f.plan.faceRegions,width,height,false);
   GpuFinishPolicy1992.Sampler confidence=GpuFinishPolicy1992.sampler(f.plan.smoothedRegions,width,height,true);
   check(f.plan.faceRegions==null||skin!=null,"face axis descriptor id="+id);
   check(f.plan.smoothedRegions==null||confidence!=null,"smooth axis descriptor id="+id);
   long random=0x1993abcL+id;int count=large==0?width*height:65536;
   for(int i=0;i<count;i++) {
    random=(random*1664525L+1013904223L)&0xffffffffL;
    int x=large==0?i%width:i<width?i:(int)(random%width);
    random=(random*1664525L+1013904223L)&0xffffffffL;
    int y=large==0?i/width:i<width?0:(int)(random%height);
    if(skin!=null){assertions++;if(skin.sample(x,y)!=f.plan.faceRegions.skinQ8(x,y))throw new AssertionError("skin exact id="+id+" x="+x+" y="+y);}
    if(confidence!=null){assertions++;if(confidence.sample(x,y)!=f.plan.smoothedRegions.smoothingQ8(x,y))throw new AssertionError("smooth exact id="+id+" x="+x+" y="+y);}
    checked++;
   }
  }
  check(checked>3000000,"large and exact small coordinate corpus");
 }
 static void parallelBands()throws Exception {
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(1,1025,257);
  Bitmap source=Bitmap.createBitmap(35,39,Bitmap.Config.ARGB_8888);int[] input=new int[35*39];
  for(int i=0;i<input.length;i++)input[i]=0xff000000|((i*1973)&0xffffff);
  source.setPixels(input,0,35,0,0,35,39);Bitmap cpu=null,gpu=null;
  try{
   cpu=GpuChain1961.cpuFinish(source,90,f.width,f.height,f.plan,false);
   ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace(1993,System.currentTimeMillis(),System.nanoTime());
   gpu=GpuChain1961.runFinish1976(source,90,f.width,f.height,f.plan,false,trace,128);
   check(gpu!=null&&GpuFinishDefault1990.difference(cpu,gpu)==null,"parallel128 mask packing exact across bank reuse and partial final row");
   int mask=ProcessingTiming1947.FINISH_CPU_MASK_PACK1992*4;
   check(trace.finishWork1992[mask]==3&&trace.finishWork1992[ProcessingTiming1947.FINISH_CPU_POLICY1992*4]==0,"large128 graph uses resident policy across three bands");
   check(trace.correctionRows1994==128&&trace.geometryBands1994==3&&trace.geometryMinRows1994==1&&trace.geometryMaxRows1994==128&&trace.geometryApiNanos1994>0,"geometry and finishing both execute observed128-row bands plus partial last row");
   pixels+=(long)f.width*f.height;
  }finally{if(cpu!=null)cpu.recycle();if(gpu!=null)gpu.recycle();source.recycle();}
  check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"parallel128 writers joined and resources retired");
 }
 public static void main(String[] args)throws Exception {
  axes();
  certificateStore();check(!GpuFinishPolicy1992.ready(),"unverified policy not admitted");
  check(GpuFinishPolicy1992.verify(GO),GpuFinishPolicy1992.verificationFailure());
  check(!GpuNoise1960.sessionBusy(),"policy corpus released session");
  GpuCertificates1990.Evidence proof=GpuCertificates1990.restore(GpuFinishPolicy1992.SUITE);check(proof!=null&&proof.cases==48&&proof.pixels>0&&proof.cpuNanos>0&&proof.gpuNanos>0,"independent measured complete policy proof");
  boolean cancelled=false;try{GpuFinishPolicy1992.verify(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&!GpuNoise1960.sessionBusy(),"cancelled corpus releases owner");
  ownership();
  parallelBands();
  for(int id=0;id<GpuFinishPolicy1992.CASES;id++) {
   GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(id);
   int sw=35,sh=39,rotation=(id&3)*90;int[] data=new int[sw*sh];
   for(int i=0;i<data.length;i++){int r=(i*37+id*19)&255,g=(i*11+id*47)&255,b=(i*71+id*3)&255;data[i]=0xff000000|(r<<16)|(g<<8)|b;}
   Bitmap source=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);source.setPixels(data,0,sw,0,0,sw,sh);
   Bitmap cpu=GpuChain1961.cpuFinish(source,rotation,f.width,f.height,f.plan,false);
   check(cpu!=null,"CPU output "+id);int[] expected=new int[f.width*f.height];cpu.getPixels(expected,0,f.width,0,0,f.width,f.height);
   for(int tile:new int[]{32,64,128}) {
    ProcessingTiming1947.Trace trace=id==1&&tile==32?new ProcessingTiming1947.Trace(1992,System.currentTimeMillis(),System.nanoTime()):null;
    Bitmap gpu=GpuChain1961.runFinish1976(source,rotation,f.width,f.height,f.plan,false,trace,tile);
    try{check(gpu!=null,"GPU output "+id+" tile="+tile);int[] actual=new int[expected.length];gpu.getPixels(actual,0,f.width,0,0,f.width,f.height);check(Arrays.equals(expected,actual),"full output exact "+id+" tile="+tile);pixels+=actual.length;}
    finally{if(gpu!=null)gpu.recycle();}
    if(trace!=null){
     int bands=(f.height+tile-1)/tile,mask=ProcessingTiming1947.FINISH_CPU_MASK_PACK1992*4,metadata=ProcessingTiming1947.FINISH_METADATA_UPLOAD1992*4,submit=ProcessingTiming1947.FINISH_TRANSFER_SUBMIT1992*4;
     check(trace.finishWork1992[mask]==bands&&trace.finishWork1992[mask+2]==4L*f.width*Math.min(tile,f.height)*bands,"actual finish graph uses packed-mask resident policy in every band");
     check(trace.finishWork1992[ProcessingTiming1947.FINISH_CPU_POLICY1992*4]==0,"observed full finish does not fall back to CPU policy builder");
     check(trace.finishWork1992[metadata]==1&&trace.finishWork1992[metadata+2]>0&&trace.finishWork1992[submit]==bands,"actual metadata upload and fused policy/pixel submissions recorded");
     residentPolicyObserved=true;
    }
    check(!GpuNoise1960.sessionBusy(),"full output releases session");
   }
   int[] unchanged=new int[data.length];source.getPixels(unchanged,0,sw,0,0,sw,sh);check(Arrays.equals(data,unchanged),"source immutable");if(id==0){data[0]&=0x7fffffff;source.setPixels(data,0,sw,0,0,sw,sh);check(GpuChain1961.runFinish1976(source,rotation,f.width,f.height,f.plan,false,null,32)==null,"nonopaque source remains outside GPU domain");}cpu.recycle();source.recycle();
  }
  check(residentPolicyObserved,"resident policy graph adoption observed");
  System.out.println("ENV "+GpuNoise1960.fingerprint());
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"policy_cases\":"+GpuFinishPolicy1992.CASES+",\"finish_cases\":"+(GpuFinishPolicy1992.CASES*3)+",\"compared_pixels\":"+pixels+",\"resident_policy_trace_verified\":"+residentPolicyObserved+",\"actual_jni\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
