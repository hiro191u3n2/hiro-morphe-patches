package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;

/** Shipped omission behavior, using actual GSD and no regional-helper class. */
public final class RegionalDisabled19115Test {
 static int assertions,scenarios;
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
 static Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=GpuStrongDefault1990.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
 static void reset()throws Exception{
  scenarios++;Thread.interrupted();GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuCertificates1990.restoredKeys.clear();GpuCertificates1990.interruptExact1995=GpuCertificates1990.failFast1995=false;
  GpuStrong1960.fault=GpuStrong1960.submits=0;GpuStrong1960.slowCandidate=GpuStrong1960.baselineDelay1995=false;
  GpuNoise1960.supported=GpuNoise1960.warmed=GpuNoise1960.warmed6=GpuNoise1960.warmed7=true;GpuNoise1960.supportCalls=GpuNoise1960.optionalPrepareCalls=0;
  GpuQualification1961.optionalAllowed1995=GpuQualification1961.acceptQueue=true;GpuQualification1961.scheduled=GpuQualification1961.regionalReservations=0;GpuQualification1961.exactRejected=false;GpuQualification1961.pending=null;GpuQualification1961.lastKey=null;ProcessingTiming1947.epoch=10;
  for(Class<?> owner:new Class<?>[]{GpuStrongDefault1990.Adaptive1994.class,GpuStrongDefault1990.Adaptive1998.class})for(String name:new String[]{"requested","attempted"}){Field f=owner.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
  Field f=GpuStrongDefault1990.Adaptive1998.class.getDeclaredField("completedEpoch");f.setAccessible(true);f.setLong(null,Long.MAX_VALUE);
  GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);
 }
 static String exact(int p){return p==7?GpuStrongDefault1990.Adaptive1994.EXACT:GpuStrongDefault1990.Adaptive1998.EXACT;}
 static String fast(int p){return p==7?GpuStrongDefault1990.Adaptive1994.FAST:GpuStrongDefault1990.Adaptive1998.FAST;}
 static void proof(int p,boolean quality,boolean speed,boolean namespaced){if(quality)GpuCertificates1990.passed(exact(p),70,1,1,1);if(speed)GpuCertificates1990.passed(fast(p)+(namespaced?":regional115":""),70,1,1,1);}
 static void noRegional(){check(GpuNoise1960.optionalPrepareCalls==0,"absent helper never prepares optional GPU programs");check(GpuQualification1961.regionalReservations==0,"absent helper never reserves regional heavy work");}
 static void oldKeys(){for(String key:GpuCertificates1990.restoredKeys)check(!key.endsWith(":regional115"),"missing helper preserves original FAST namespace");}
 static void runQueued(GpuQualification1961.Cancellation c){GpuQualification1961.Probe p=GpuQualification1961.pending;check(p!=null,"concrete legacy job queued");try{p.run(c);}finally{p.close();}}
 static void shapes(int expected){for(int[] wh:new int[][]{{3060,4080},{4080,3060},{65,65},{3060,4079}})check(GpuStrongDefault1990.profile1994(wh[0],wh[1])==expected,"legacy profile selected in every geometry "+expected);check(GpuStrongDefault1990.profile1994()==expected,"legacy profile API preserved "+expected);}
 public static void main(String[] args)throws Exception{
  boolean absent=false;try{Class.forName("com.hiro.ulike.GpuRegionalDefault19115");}catch(ClassNotFoundException expected){absent=true;}check(absent,"distributed optional helper is absent from JVM classpath");
  reset();check(!GpuStrongDefault1990.regionalFaster19115(),"missing helper readiness is false");
  for(String name:new String[]{"faster","pending","unknown"})check(Boolean.FALSE.equals(call("regionalState19115",new Class<?>[]{String.class},name)),"missing optional state returns false");
  call("requestRegional19115",new Class<?>[0]);check(GpuQualification1961.scheduled==0,"missing request hook is a no-op");noRegional();
  check("test-original".equals(call("fastKey19115",new Class<?>[]{String.class},"test-original")),"missing helper preserves original FAST key exactly");
  shapes(4);check(GpuNoise1960.supportCalls==0,"scalar default selection never compiles a shader");
  int[] u=GpuStrongDefault1990.fixture(0).u.clone();check(GpuStrongDefault1990.accepts1994(u,4),"ordinary standard output remains accepted");u[23]=1;check(!GpuStrongDefault1990.accepts1994(u,4),"regional output marker cannot commit without helper");
  check(Boolean.FALSE.equals(call("regionalAccepts19115",new Class<?>[]{int[].class},(Object)u)),"missing acceptance hook is false");oldKeys();noRegional();
  for(int p:new int[]{6,7})for(int mode=0;mode<4;mode++){
   reset();proof(p,mode!=1,mode!=0,mode==3);shapes(mode==2?p:4);oldKeys();noRegional();check(GpuNoise1960.supportCalls==0,"cached profile lookup is scalar");
  }
  for(int p:new int[]{6,7}){
   reset();proof(p,true,true,false);if(p==7)GpuNoise1960.warmed7=false;else GpuNoise1960.warmed6=false;
   shapes(4);GpuQualification1961.optionalAllowed1995=false;GpuStrongDefault1990.request1994();
   check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains(p==7?"layout1994":"stripe1998"),"cached adaptive keeps its original preparation owner");runQueued(GO);shapes(p);
   check(GpuNoise1960.supportCalls==1&&GpuStrong1960.submits==0,"cached old shader preparation performs no heavy benchmark");noRegional();oldKeys();
   ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1,"certified legacy route does not queue competing work");noRegional();
  }
  reset();GpuQualification1961.optionalAllowed1995=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==0,"no optional heavy trial before publication");noRegional();
  reset();GpuStrongDefault1990.request1994();GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("stripe1998"),"missing helper queues one legacy profile6 first");noRegional();
  boolean cancelled=false;try{runQueued(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&!GpuStrongDefault1990.Adaptive1998.pending19115(),"cancelled legacy trial releases ownership");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2&&GpuQualification1961.lastKey.contains("stripe1998"),"cancelled profile6 rearms profile6");
  GpuNoise1960.supported=false;runQueued(GO);GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2,"same capture cannot start profile7 after profile6");
  ProcessingTiming1947.epoch=9;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==2,"older capture cannot start profile7");
  ProcessingTiming1947.epoch=11;GpuStrongDefault1990.request1994();GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==3&&GpuQualification1961.lastKey.contains("layout1994"),"next capture starts one legacy profile7 trial");runQueued(GO);noRegional();
  ProcessingTiming1947.epoch=12;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==3,"completed profile7 is not retried each capture");shapes(4);noRegional();oldKeys();
  reset();GpuCertificates1990.reject(exact(6),"restored failure");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("layout1994"),"restored profile6 rejection retains profile7 scheduling");GpuQualification1961.pending.close();noRegional();
  reset();proof(6,true,true,false);proof(7,true,true,false);shapes(7);GpuNoise1960.warmed7=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("layout1994"),"both legacy certificates retain profile7 preparation priority");runQueued(GO);shapes(7);oldKeys();noRegional();
  reset();GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(0);Method timed=GpuStrongDefault1990.Adaptive1994.class.getDeclaredMethod("timed",GpuStrongDefault1990.Fixture.class,Integer.TYPE,GpuQualification1961.Cancellation.class);timed.setAccessible(true);Object read=timed.invoke(null,f,4,GO);
  check(read!=null&&GpuStrong1960.submits==1&&GpuStrong1960.lastProfile==4,"missing helper retains old complete-API timing reference");Field nanos=read.getClass().getDeclaredField("nanos");nanos.setAccessible(true);check(nanos.getLong(read)>0,"old reference returns measured complete API duration");noRegional();oldKeys();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"distributed_helper_absent_jvm\":true,\"standard_routes_preserved\":true,\"optional_gpu_prepare_calls\":"+GpuNoise1960.optionalPrepareCalls+",\"regional_heavy_reservations\":"+GpuQualification1961.regionalReservations+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
