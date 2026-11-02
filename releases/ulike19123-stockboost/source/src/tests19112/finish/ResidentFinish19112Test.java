package com.hiro.ulike;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Real JNI/GLES pixels and real session/lease ownership. Fault hooks only
 * change the existing budget observation or inject capture interruption. */
public final class ResidentFinish19112Test {
 static int assertions,cases,failAtFinish;static long pixels;static boolean refuseBudget;
 static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
 static void finishBoundary(){if(failAtFinish==1)refuseBudget=true;else if(failAtFinish==2)Thread.currentThread().interrupt();}
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static int[] pixels(Bitmap b){int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return p;}
 static Bitmap source(){Bitmap b=Bitmap.createBitmap(65,67,Bitmap.Config.ARGB_8888);int[] p=new int[65*67];for(int i=0;i<p.length;i++)p[i]=0xff000000|((i*1973+i/65*531)&0xffffff);b.setPixels(p,0,65,0,0,65,67);return b;}
 static GpuNoise1960.Session carried(GpuChain1961.Preflight1981 plan,Bitmap b){
  GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"session acquired");
  try{check(plan.bind(s),"live source plan bound");check(s.upload(24,pixels(b)),"real carried Strong pixels uploaded");
   check(s.allocate(0,262144),"real dead model bank present until finish");return s;
  }catch(Throwable t){s.close();throw t;}
 }
 static void retired(){GpuNoise1960.trimIdle();check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0&&GpuNoise1960.retainedBytes()==0,"all private storage and debt retired");}
 static String key(Bitmap b,QualityPixels1932.Plan p)throws Exception{
  Method m=GpuResident1976.class.getDeclaredMethod("key",Bitmap.class,QualityPixels1932.Plan.class,int.class,boolean.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);m.setAccessible(true);
  return (String)m.invoke(null,b,p,4,true,0,257,257,p,false);
 }
 static void exact(int rotation,int height,int rows)throws Exception{
  cases++;Bitmap b=source(),expected=null,actual=null;int[] original=pixels(b);GpuChain1961.Preflight1981 plan=null;GpuNoise1960.Session session=null;
  GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(rotation/90,257,height);
  try{
   expected=GpuChain1961.cpuFinish(b,rotation,257,height,f.plan,false);check(expected!=null,"independent CPU result");
   plan=GpuChain1961.preflightResident19112(b,rotation,257,height,0,true);check(plan!=null&&plan.finishRows19112==rows&&plan.geometryRows==rows&&plan.javaPeak==0,"comparison forecasts selected rows without future phase debt");
   session=carried(plan,b);int comparison=GpuChain1961.compareResidentFinish19108(b,rotation,257,height,f.plan,false,expected,GpuChain1961.residentRows19112(),true,session,false,GO);session=null;
   check(comparison==ResidentProof1978.EXACT,"all resident final pixels exact at boundary height="+height+" turn="+rotation);pixels+=(long)257*height;plan.close();plan=null;retired();
   plan=GpuChain1961.preflightResident1981(b,rotation,257,height,0);check(plan!=null&&plan.finishRows19112==rows&&plan.geometryRows==rows,"foreground and speed use same requested schedule");
   session=carried(plan,b);ProcessingTiming1947.Trace trace=new ProcessingTiming1947.Trace(19112,System.currentTimeMillis(),System.nanoTime());
   actual=GpuChain1961.runResident1976(b,session,rotation,257,height,f.plan,false,trace);session=null;
   check(actual!=null&&Arrays.equals(pixels(expected),pixels(actual)),"complete carried output equals CPU");pixels+=(long)257*height;
   check(trace.correctionRows1994==rows&&trace.geometryBands1994==(height+rows-1)/rows,"executed geometry and finish row counts match profile");
   check(trace.finishWork1992[ProcessingTiming1947.FINISH_READBACK1992*4]==(height+rows-1)/rows,"actual finish readback count matches128/64 including tail");
   check(Arrays.equals(original,pixels(b)),"caller source unchanged through comparison and output");
  }finally{if(session!=null)session.close();if(plan!=null)plan.close();if(actual!=null)actual.recycle();if(expected!=null)expected.recycle();b.recycle();}retired();
 }
 static void debtAndFailures()throws Exception{
  Bitmap b=source();int[] original=pixels(b);GpuFinishPolicy1992.Fixture f=GpuFinishPolicy1992.fixture(1,257,257);
  GpuChain1961.Preflight1981 plan=GpuChain1961.preflightResident1981(b,0,257,257,0);check(plan!=null&&plan.javaPeak==0,"future finish capacity not charged during Strong");
  GpuNoise1960.Session s=GpuNoise1960.open();GpuNoise1960.Lease1971 unrelated=null;
  try{unrelated=s.reserveCapacity1983(new int[]{23},new long[]{131072},new long[]{0},4096);check(unrelated!=null,"independent live owner lease");long debt=GpuNoise1960.reservedBytes1971();
   check(plan.bind(s)&&GpuNoise1960.reservedBytes1971()>debt,"source reservation joins rather than overwrites other debt");
   check(!plan.bind(s)&&!plan.bind(null),"cannot rebind or replace a live plan owner");plan.finishAdmission(s);check(GpuNoise1960.reservedBytes1971()==debt&&unrelated.revalidate1971(),"finish admission releases only its own lease");
  }finally{if(unrelated!=null)unrelated.close();s.close();plan.close();}retired();
  refuseBudget=true;try{check(GpuChain1961.preflightResident1981(b,0,257,257,0)==null,"low actual budget refuses selected profile before Strong");}finally{refuseBudget=false;}
  for(int fault=1;fault<=2;fault++){
   plan=GpuChain1961.preflightResident1981(b,0,257,257,0);check(plan!=null,"preflight succeeds before later capacity/capture change");s=carried(plan,b);failAtFinish=fault;boolean stopped=false;
   try{GpuChain1961.runResident1976(b,s,0,257,257,f.plan,false,null);}
   catch(CancellationException expected){check(fault==2&&Thread.currentThread().isInterrupted(),"capture cancellation preserved");stopped=true;}
   catch(RuntimeException expected){check(fault==1&&GpuChain1961.memoryRefusal19108(expected),"later finish capacity loss is typed memory refusal");stopped=true;}
   finally{Thread.interrupted();failAtFinish=0;refuseBudget=false;s.close();plan.close();}
   check(stopped&&Arrays.equals(original,pixels(b)),"failure discards private route and preserves exact fallback input");retired();
   Bitmap fallback=GpuChain1961.cpuFinish(b,0,257,257,f.plan,false),again=GpuChain1961.cpuFinish(b,0,257,257,f.plan,false);
   check(fallback!=null&&again!=null&&Arrays.equals(pixels(fallback),pixels(again)),"existing CPU fallback remains reproducible after abandoned carried route");fallback.recycle();again.recycle();
  }
  plan=GpuChain1961.preflightResident1981(b,0,257,257,0);s=carried(plan,b);Bitmap other=b.copy(Bitmap.Config.ARGB_8888,false);boolean mismatch=false;
  try{GpuChain1961.runResident1976(other,s,0,257,257,f.plan,false,null);}catch(RuntimeException expected){mismatch=true;}
  finally{s.close();plan.close();other.recycle();}check(mismatch&&Arrays.equals(original,pixels(b)),"preflight cannot transfer to different source owner");retired();b.recycle();
 }
 public static void main(String[] args)throws Exception{
  FinishPolicy1992Test.certificateStore();check(GpuChain1961.preferredResidentRows19112()==64,"uncertified128 never selected");
  Bitmap b=source();QualityPixels1932.Plan p=GpuFinishPolicy1992.fixture(1,257,257).plan;String narrow=key(b,p);check(narrow.contains("resident-finish=64"),"whole key binds64profile");exact(0,129,64);
  check(GpuFinishDefault1990.qualify(GO)&&GpuFinishDefault1990.wideReady1993(),"actual startup corpus certifies128 independently");
  check(GpuChain1961.preferredResidentRows19112()==128,"certified128 is candidate schedule");String wide=key(b,p);check(wide.contains("resident-finish=128")&&!wide.equals(narrow),"whole64proof cannot restore as128proof");
  check(GpuQualification1961.restore(wide)==null,"startup128 does not grant whole resident certificate");
  Method win=GpuResident1976.class.getDeclaredMethod("win",long.class,long.class);win.setAccessible(true);check((Boolean)win.invoke(null,100L,95L)&&!(Boolean)win.invoke(null,100L,96L),"unchanged95percent speed gate");
  for(int rotation:new int[]{0,90,180,270})for(int height:new int[]{127,128,129,257})exact(rotation,height,128);
  debtAndFailures();
  GpuChain1961.Preflight1981 plan=GpuChain1961.preflightResident1981(b,0,257,257,0);GpuNoise1960.Session s=carried(plan,b);
  FinishPolicy1992Test.certificateStore();boolean stale=false;try{GpuChain1961.runResident1976(b,s,0,257,257,p,false,null);}catch(RuntimeException expected){stale=true;}finally{s.close();plan.close();}
  check(stale&&GpuChain1961.preferredResidentRows19112()==64,"128 readiness loss cannot silently use64 under old profile");retired();b.recycle();
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"compared_pixels\":"+pixels+",\"actual_jni\":true,\"resident128_exact_pixels\":true,\"resident_profile_key_bound\":true,\"resident_phase_budget_verified\":true,\"resident_failure_ownership_verified\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
