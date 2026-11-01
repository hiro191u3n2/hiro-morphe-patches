package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;

/** Exercises the real standard/adaptive owner, with controlled peer state. */
public final class StrongReference19115Test {
 static int assertions;
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static void reset()throws Exception{
  GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuCertificates1990.failFast1995=GpuCertificates1990.interruptExact1995=false;
  GpuNoise1960.supported=GpuNoise1960.warmed=GpuNoise1960.warmed6=GpuNoise1960.warmed7=true;GpuNoise1960.supportCalls=0;
  GpuStrong1960.submits=GpuStrong1960.fault=0;GpuStrong1960.slowCandidate=GpuStrong1960.baselineDelay1995=false;
  GpuQualification1961.exactRejected=false;GpuQualification1961.optionalAllowed1995=true;GpuQualification1961.acceptQueue=true;GpuQualification1961.scheduled=0;GpuQualification1961.pending=null;GpuQualification1961.scheduledUnderAdaptiveLock1995=false;
  GpuRegionalDefault19115.active=GpuRegionalDefault19115.pendingState=GpuRegionalDefault19115.requestCreatesPending=GpuRegionalDefault19115.bridgeNull=GpuRegionalDefault19115.bridgeCancel=false;GpuRegionalDefault19115.currentCertificate=true;GpuRegionalDefault19115.requests=GpuRegionalDefault19115.bridgeCalls=0;
  for(Class<?> c:new Class<?>[]{GpuStrongDefault1990.Adaptive1994.class,GpuStrongDefault1990.Adaptive1998.class})for(String name:new String[]{"requested","attempted"}){Field f=c.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
  Field epoch=GpuStrongDefault1990.Adaptive1998.class.getDeclaredField("completedEpoch");epoch.setAccessible(true);epoch.setLong(null,Long.MAX_VALUE);ProcessingTiming1947.epoch=2;
  GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);
 }
 static void proof(int profile,boolean regional,boolean exact){
  String e=profile==6?GpuStrongDefault1990.Adaptive1998.EXACT:GpuStrongDefault1990.Adaptive1994.EXACT;
  String f=profile==6?GpuStrongDefault1990.Adaptive1998.FAST:GpuStrongDefault1990.Adaptive1994.FAST;
  if(exact)GpuCertificates1990.passed(e,70,1,1,1);GpuCertificates1990.passed(f+(regional?":regional115":""),70,1,1,1);
 }
 static Object timed(int profile)throws Exception{
  Method m=GpuStrongDefault1990.Adaptive1994.class.getDeclaredMethod("timed",GpuStrongDefault1990.Fixture.class,Integer.TYPE,GpuQualification1961.Cancellation.class);m.setAccessible(true);
  try{return m.invoke(null,GpuStrongDefault1990.fixture(0),profile,GO);}catch(InvocationTargetException failure){if(failure.getCause() instanceof CancellationException)throw (CancellationException)failure.getCause();throw failure;}
 }
 static long nanos(Object read)throws Exception{Field f=read.getClass().getDeclaredField("nanos");f.setAccessible(true);return f.getLong(read);}
 public static void main(String[] args)throws Exception{
  reset();check(GpuStrongDefault1990.profile1994(3060,4080)==4,"unproven adaptive defaults to standard");proof(6,false,true);
  check(GpuStrongDefault1990.profile1994(3060,4080)==6&&GpuStrongDefault1990.profile1994(4080,3060)==6,"old baseline proof accepts both before regional adoption");
  GpuRegionalDefault19115.active=true;check(GpuStrongDefault1990.profile1994(3060,4080)==4&&GpuStrongDefault1990.profile1994(4080,3060)==4,"old speed proof cannot certify new baseline");
  check(GpuStrongDefault1990.profile1994(65,65)==6&&GpuStrongDefault1990.profile1994(3060,3060)==6,"outside tested shapes preserve old speed proofs");
  proof(6,true,true);check(GpuStrongDefault1990.profile1994(3060,4080)==6&&GpuStrongDefault1990.profile1994(4080,3060)==6,"separate regional speed proof selects6");
  proof(7,false,true);check(GpuStrongDefault1990.profile1994(3060,4080)==6&&GpuStrongDefault1990.profile1994(65,65)==7,"profile7 old proof only applies to old baseline shapes");
  proof(7,true,true);check(GpuStrongDefault1990.profile1994(3060,4080)==7&&GpuStrongDefault1990.profile1994(4080,3060)==7,"independently certified profile7 priority retained");
  GpuNoise1960.warmed7=false;check(GpuStrongDefault1990.profile1994(3060,4080)==6&&GpuStrongDefault1990.profile1994(65,65)==6,"unprepared7 cannot select");GpuNoise1960.warmed6=false;check(GpuStrongDefault1990.profile1994(3060,4080)==4&&GpuStrongDefault1990.profile1994(65,65)==4,"unprepared optional routes retain standard");check(GpuNoise1960.supportCalls==0,"foreground profile lookup never compiles");
  reset();GpuRegionalDefault19115.active=true;proof(6,true,false);check(GpuStrongDefault1990.profile1994(3060,4080)==4,"regional speed proof still requires independent exact proof");
  reset();int[] marked=new int[32];marked[0]=3060;marked[1]=292;marked[2]=18;marked[3]=274;marked[4]=0;marked[5]=292;marked[6]=1374;marked[7]=4080;marked[8]=4;marked[9]=1;marked[10]=3;marked[11]=1;marked[12]=1;marked[23]=1;
  GpuRegionalDefault19115.active=true;check(GpuStrongDefault1990.accepts1994(marked,4),"current regional route accepted with exact current proof");
  GpuRegionalDefault19115.active=false;check(!GpuStrongDefault1990.accepts1994(marked,4),"stage-like regional marker must recheck live helper state");
  GpuRegionalDefault19115.active=true;GpuRegionalDefault19115.currentCertificate=false;check(!GpuStrongDefault1990.accepts1994(marked,4),"stage-like regional marker must recheck current certificate");
  marked[23]=0;check(GpuStrongDefault1990.accepts1994(marked,4),"old route marker remains independently accepted");
  marked[23]=1;GpuRegionalDefault19115.currentCertificate=true;proof(6,true,true);proof(7,true,true);
  check(!GpuStrongDefault1990.accepts1994(marked,6)&&!GpuStrongDefault1990.accepts1994(marked,7),"regional output marker cannot certify another adaptive profile");
  marked[23]=2;check(!GpuStrongDefault1990.accepts1994(marked,4),"unknown regional marker rejected");marked[23]=-1;check(!GpuStrongDefault1990.accepts1994(marked,4),"negative regional marker rejected");
  reset();GpuRegionalDefault19115.requestCreatesPending=true;GpuStrongDefault1990.request1994();check(GpuRegionalDefault19115.requests==1&&GpuQualification1961.scheduled==0,"regional pending prevents adaptive coqueue");GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==0,"existing pending prevents later adaptive coqueue");
  GpuRegionalDefault19115.pendingState=GpuRegionalDefault19115.requestCreatesPending=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("stripe1998"),"completed regional owner permits old adaptive request");check(!GpuQualification1961.scheduledUnderAdaptiveLock1995,"adaptive queue owner retains lock order");
  reset();GpuRegionalDefault19115.active=true;Object active=timed(4);check(active!=null&&GpuRegionalDefault19115.bridgeCalls==1&&GpuStrong1960.submits==0,"active regional profile4 uses explicit measured bridge");check(nanos(active)==GpuRegionalDefault19115.completeApiNanos,"bridge uses returned complete API time");
  Object adaptive=timed(6);check(adaptive!=null&&GpuRegionalDefault19115.bridgeCalls==1&&GpuStrong1960.submits==1,"adaptive candidate itself keeps own transport");
  GpuRegionalDefault19115.active=false;Object inactive=timed(4);check(inactive!=null&&GpuRegionalDefault19115.bridgeCalls==1&&GpuStrong1960.submits==2,"inactive baseline keeps old complete transport");
  GpuRegionalDefault19115.active=true;GpuRegionalDefault19115.bridgeNull=true;check(timed(4)==null&&GpuStrong1960.submits==2,"failed regional reference cannot silently time old baseline");
  GpuRegionalDefault19115.bridgeNull=false;GpuRegionalDefault19115.bridgeCancel=true;boolean canceled=false;try{timed(4);}catch(CancellationException expected){canceled=true;}check(canceled,"bridge cancellation propagates unchanged");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");
 }
}
