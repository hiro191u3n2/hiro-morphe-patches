package com.hiro.ulike;
import android.graphics.Bitmap;
import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;
/** Actual current shader execution, independently frozen whole-slice CPU pixels. */
public final class Chain1976Test {
 static long assertions,pixels;static int cases,variants,legacyCases;
 static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
 static void equal(int[] expected,Bitmap actual,String label){check(actual!=null,label+" absent");int[] p=Chain1962Oracle.pixels(actual);check(p.length==expected.length,label+" shape");for(int i=0;i<p.length;i++){assertions++;pixels++;if(p[i]!=expected[i])throw new AssertionError(label+" at "+i+" expected "+Integer.toHexString(expected[i])+" got "+Integer.toHexString(p[i]));}check(actual.getDensity()==333&&actual.hasAlpha(),label+" metadata");}
 static GpuNoise1960.Session carried(Bitmap source){
  int[] pixels=Chain1962Oracle.pixels(source);GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"resident input session");
  GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{0,24},new long[]{4L*pixels.length,4L*pixels.length},0);check(lease!=null,"resident input and source lease");
  try{check(lease.revalidate1971()&&session.upload(0,pixels)&&session.allocate(24,4L*pixels.length),"materialize resident input");check(session.copy1976(0,24,0,0,pixels.length),"actual GPU copy into slot24");check(Arrays.equals(pixels,session.readInts(0,pixels.length)),"copy does not mutate source SSBO");}finally{lease.close();}
  return session;
 }
 static Bitmap candidate(Bitmap source,int rotation,int w,int h,QualityPixels1932.Plan plan,boolean refresh,int variant){
  return variant==3?GpuChain1961.runResident1976(source,carried(source),rotation,w,h,plan,refresh,null):GpuChain1961.runFinish1976(source,rotation,w,h,plan,refresh,null,32<<variant);
 }
 static void pixels(String file)throws Exception {
  try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){
   check(in.readInt()==19624,"frozen oracle header");int records=in.readInt();
   for(int record=0;record<records;record++){
    int sw=in.readInt(),sh=in.readInt(),w=in.readInt(),h=in.readInt(),rotation=in.readInt(),pattern=in.readInt(),mask=in.readInt();boolean refresh=in.readBoolean();int[] expected=new int[in.readInt()];for(int i=0;i<expected.length;i++)expected[i]=in.readInt();
    Bitmap source=Chain1962Oracle.image(sw,sh,pattern);int[] pristine=Chain1962Oracle.pixels(source);
    try{QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,w,h,mask);for(int v=0;v<4;v++){
     Bitmap actual=null;try{actual=candidate(source,rotation,w,h,plan,refresh,v);equal(expected,actual,"record "+record+" variant "+v);check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"source bitmap untouched");check(!GpuNoise1960.sessionBusy(),"consumed session closed");check(GpuNoise1960.reservedBytes1971()==0,"all pipeline leases retired");variants++;}finally{if(actual!=null)actual.recycle();}
    }
    int representative=record%48;
    if(representative==0||representative==12||representative==24||representative==47){
     Bitmap legacy=null;try{legacy=GpuChain1961.runLegacy1976(source,rotation,w,h,plan,refresh,null);equal(expected,legacy,"legacy sync record "+record);check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"legacy source unchanged");check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"legacy owners retired");legacyCases++;}finally{if(legacy!=null)legacy.recycle();}
    }
    cases++;}finally{source.recycle();}
   }
  }
 }
 static void failure(){for(int variant=0;variant<4;variant++){
  Bitmap source=Chain1962Oracle.image(95,197,2);int[] pristine=Chain1962Oracle.pixels(source);QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,131,239,0);
  GpuNoise1960.Session session=variant==3?carried(source):null;Bitmap result=null;
  try{Native1960Test.setFault(4);result=variant==3?GpuChain1961.runResident1976(source,session,90,131,239,plan,false,null):GpuChain1961.runFinish1976(source,90,131,239,plan,false,null,32<<variant);check(result==null,"late second unmap never publishes partial bitmap");}
  finally{Native1960Test.setFault(0);if(result!=null)result.recycle();if(session!=null)session.close();}
  check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"fault preserves source");check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"fault drains/releases banks and leases");
  result=GpuChain1961.runFinish1976(source,90,131,239,plan,false,null,64);check(result!=null,"subsequent actual GPU recovers");result.recycle();source.recycle();
 }}
 static void cancel(){for(int variant=0;variant<4;variant++){
  Bitmap source=Chain1962Oracle.image(95,257,2);int[] pristine=Chain1962Oracle.pixels(source);final int[] calls={0};final int trigger=95*(variant==3?64:32<<variant)+1;
  QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,95,257,0).withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){if(++calls[0]==trigger)Thread.currentThread().interrupt();return 0;}public int detailQ8(int x,int y){return 0;}});
  Bitmap result=null;boolean cancelled=false;try{result=candidate(source,0,95,257,plan,false,variant);}catch(RuntimeException expected){if(!Thread.currentThread().isInterrupted())throw expected;cancelled=true;}finally{check(Thread.currentThread().isInterrupted(),"cancel preserves interrupt");Thread.interrupted();}
  check(cancelled||result==null,"cancel cannot publish partial bitmap");check(result==null,"private interrupted result withheld");check(calls[0]>=trigger,"cancellation reached policy while earlier tile exists");check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"cancel source unchanged");check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"cancel drains both pending banks");
  Bitmap recovery=GpuChain1961.runFinish1976(source,0,95,257,Chain1962Oracle.plan(source,95,257,0),false,null,64);check(recovery!=null,"GPU reuse after cancel");recovery.recycle();source.recycle();
 }}

 static void certifiedCancel()throws Exception {
  Bitmap source=Chain1962Oracle.image(95,257,2);int[] pristine=Chain1962Oracle.pixels(source);
  QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,95,257,0).withFaceRegions(new FaceRegions1934.Mask());
  java.lang.reflect.Method keyMethod=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);keyMethod.setAccessible(true);
  String key=(String)keyMethod.invoke(null,source,0,95,257,plan,false);android.os.Build.FINGERPRINT="chain1976-test-device-identity";GpuQualification1961.initialize(new android.content.Context());
  // Seed only scalar control state to enter the already-qualified foreground
  // branch; pixel equivalence of this production runner is covered separately.
  GpuQualification1961.qualified(key,1000000000000L,1L,0);check(GpuQualification1961.restore(key)!=null,"certified foreground state seeded");
  FaceRegions1934.interruptSamples1976=95*32+1;Bitmap result=null;boolean interrupted=false;
  try{result=GpuChain1961.finish(source,0,95,257,plan,false);}catch(RuntimeException expected){if(!Thread.currentThread().isInterrupted())throw expected;interrupted=true;}
  finally{check(Thread.currentThread().isInterrupted(),"certified policy cancellation preserves interrupt");Thread.interrupted();FaceRegions1934.interruptSamples1976=0;}
  check(interrupted,"certified cancellation propagates");check(result==null,"certified cancelled partial bitmap withheld");check(!GpuQualification1961.exactRejected(key),"interruption never persists a pixel mismatch");check(GpuQualification1961.restore(key)!=null,"exact certificate survives cancellation");
  check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"certified cancellation source unchanged");check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"certified cancellation drains and releases");
  Bitmap recovered=GpuChain1961.finish(source,0,95,257,plan,false);check(recovered!=null,"same certificate GPU route recovers");recovered.recycle();source.recycle();
 }

 static void choiceAndLegacy()throws Exception {
  boolean[] all={true,true,true};
  check(GpuChain1961.chooseFinish1976(1000,800,new long[]{850,900,950},all,true)==-1,"CPU faster alone cannot adopt slower-than-existing pipeline");
  check(GpuChain1961.chooseFinish1976(1000,800,new long[]{770,700,900},all,true)==1,"fastest exact candidate beats CPU and existing GPU");
  check(GpuChain1961.chooseFinish1976(1000,800,new long[]{760,700,780},new boolean[]{true,false,true},true)==0,"mismatch excluded and exact5percentthreshold accepted");
  check(GpuChain1961.chooseFinish1976(1000,800,new long[]{761,0,0},all,true)==-1,"below5percentGPU improvement rejected");
  check(GpuChain1961.chooseFinish1976(1000,800,new long[]{1,1,1},all,false)==-1,"missing exact legacy baseline never certifies pipeline");
  check(GpuChain1961.chooseFinish1976(0,800,new long[]{1,1,1},all,true)==-1&&GpuChain1961.chooseFinish1976(1000,0,new long[]{1,1,1},all,true)==-1,"nonpositivebaseline timing cannot adopt");
  Bitmap source=Chain1962Oracle.image(95,97,2),expected=null,result=null;int[] pristine=Chain1962Oracle.pixels(source);QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,131,139,0);
  android.os.Build.FINGERPRINT="chain1976-legacy-test-device";GpuQualification1961.initialize(new android.content.Context());
  java.lang.reflect.Method keyMethod=GpuChain1961.class.getDeclaredMethod("finishKey1976",Bitmap.class,int.class,int.class,int.class,QualityPixels1932.Plan.class,boolean.class);keyMethod.setAccessible(true);
  String key=(String)keyMethod.invoke(null,source,90,131,139,plan,true),legacy=key.replace("|finish-chain1976:v2|","|finish-chain1962:v1|");
  GpuQualification1961.qualified(legacy,1000000000000L,1L,0);GpuQualification1961.rejectSpeed(key);check(GpuQualification1961.restore(legacy)!=null&&!GpuQualification1961.maySchedule(key),"old certificate survives new pipeline speedcooldown");
  try {expected=Chain1962Oracle.cpu(source,90,131,139,plan,true);result=GpuChain1961.finish(source,90,131,139,plan,true);equal(Chain1962Oracle.pixels(expected),result,"actual legacy fallback during new pipeline cooldown");check(GpuQualification1961.restore(key)==null&&GpuQualification1961.restore(legacy)!=null,"fallback did not invent a new pipeline certificate");check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"legacy fallback source preserved");check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"legacy fallback drains owners");}
  finally{if(expected!=null)expected.recycle();if(result!=null)result.recycle();source.recycle();}
 }
 static void timeout(){Bitmap source=Chain1962Oracle.image(95,197,2);int[] pristine=Chain1962Oracle.pixels(source);QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,95,197,0);GpuNoise1960.Session session=carried(source);Native1960Test.setFault(1);Bitmap result=GpuChain1961.runResident1976(source,session,0,95,197,plan,false,null);check(result==null,"resident unknown fence withholds private frame");check(Arrays.equals(pristine,Chain1962Oracle.pixels(source)),"quarantine source unchanged");check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"quarantine releases Java owner");check(GpuNoise1960.retainedBytes()>0&&GpuNoise1960.open()==null,"uncertain native allocations remain quarantined");source.recycle();}
 static void unsupported(){Bitmap source=Chain1962Oracle.image(95,97,0);QualityPixels1932.Plan plan=Chain1962Oracle.plan(source,95,97,0);source.setGainmapForTest(true);check(!GpuChain1961.eligibleResident1976(source,0,95,97,plan)&&GpuChain1961.finish(source,0,95,97,plan,false)==null,"HDR refused before route admission");source.setGainmapForTest(false);source.setColorSpaceForTest(new android.graphics.ColorSpace(false));check(!GpuChain1961.eligibleResident1976(source,0,95,97,plan)&&GpuChain1961.finish(source,0,95,97,plan,false)==null,"wide color refused before route admission");source.setColorSpaceForTest(new android.graphics.ColorSpace(true));source.setPixels(new int[]{0x00000000},0,1,0,0,1,1);check(!GpuChain1961.opaqueResident1976(source),"alpha resident snapshot screened");for(int v=0;v<3;v++)check(candidate(source,0,95,97,plan,false,v)==null,"alpha rejected by every finish runner");check(!GpuNoise1960.sessionBusy(),"unsupported input leaves no session");source.recycle();}
 public static void main(String[] args)throws Exception{String mode=args[1];check(GpuNoise1960.supports(GpuNoise1960.FINISH1961)&&GpuNoise1960.supports(GpuNoise1960.GEOMETRY)&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961),"actual production programs");if(mode.equals("pixels"))pixels(args[0]);else if(mode.equals("failure"))failure();else if(mode.equals("cancel"))cancel();else if(mode.equals("certified_cancel"))certifiedCancel();else if(mode.equals("choice"))choiceAndLegacy();else if(mode.equals("timeout"))timeout();else unsupported();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"variants\":"+variants+",\"legacyCases\":"+legacyCases+",\"exactPixels\":"+pixels+"}");}
}
