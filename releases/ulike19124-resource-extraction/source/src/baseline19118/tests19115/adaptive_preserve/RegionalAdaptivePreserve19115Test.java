package com.hiro.ulike;
import java.lang.reflect.Field;

/** Both actual selection owners; GPU/certificate transport remains controlled. */
public final class RegionalAdaptivePreserve19115Test {
 static int assertions,scenarios;
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static void reset()throws Exception {
  scenarios++;Thread.interrupted();GpuCertificates1990.proofs.clear();GpuCertificates1990.rejects.clear();GpuCertificates1990.interruptExact1995=GpuCertificates1990.failFast1995=false;GpuCertificates1990.restoreCalls=GpuCertificates1990.regionalWrites=0;
  GpuNoise1960.supported=GpuNoise1960.warmed=GpuNoise1960.warmed6=GpuNoise1960.warmed7=true;GpuNoise1960.supportCalls=GpuNoise1960.regionalPrepares=0;GpuNoise1960.regionalPrepared=true;
  GpuQualification1961.optionalAllowed1995=GpuQualification1961.acceptQueue=true;GpuQualification1961.exactRejected=false;GpuQualification1961.pending=null;GpuQualification1961.scheduled=GpuQualification1961.injectLegacyProfile=0;GpuQualification1961.lastKey=GpuQualification1961.lastOutcome=null;GpuQualification1961.clearLegacyAtFinal=false;
  GpuStrong1960.submits=GpuStrong1960.fault=0;GpuStrong1960.slowCandidate=GpuStrong1960.baselineDelay1995=false;GpuStartup1990.invalidated=false;ProcessingTiming1947.epoch=2;
  for(Class<?> owner:new Class<?>[]{GpuRegionalDefault19115.class,GpuStrongDefault1990.Adaptive1994.class,GpuStrongDefault1990.Adaptive1998.class})for(String name:new String[]{"requested","attempted"}){Field f=owner.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
  Field epoch=GpuStrongDefault1990.Adaptive1998.class.getDeclaredField("completedEpoch");epoch.setAccessible(true);epoch.setLong(null,Long.MAX_VALUE);
  GpuCertificates1990.passed(GpuStrongDefault1990.SUITE,68,1,1,1);
 }
 static void regional(){GpuCertificates1990.passed(GpuRegionalDefault19115.EXACT,70,1,1,1);GpuCertificates1990.passed(GpuRegionalDefault19115.FAST,70,1,1,1);GpuCertificates1990.regionalWrites=0;}
 static String exact(int profile){return profile==7?GpuStrongDefault1990.Adaptive1994.EXACT:GpuStrongDefault1990.Adaptive1998.EXACT;}
 static String fast(int profile){return profile==7?GpuStrongDefault1990.Adaptive1994.FAST:GpuStrongDefault1990.Adaptive1998.FAST;}
 static void legacy(int profile,boolean quality,boolean speed,boolean namespaced){if(quality)GpuCertificates1990.passed(exact(profile),70,1,1,1);if(speed)GpuCertificates1990.passed(fast(profile)+(namespaced?":regional115":""),70,1,1,1);}
 static void prepared(int profile,boolean state){if(profile==7)GpuNoise1960.warmed7=state;else GpuNoise1960.warmed6=state;}
 static void legacyCompleted(boolean value){try{for(Class<?> owner:new Class<?>[]{GpuStrongDefault1990.Adaptive1994.class,GpuStrongDefault1990.Adaptive1998.class}){Field f=owner.getDeclaredField("attempted");f.setAccessible(true);f.setBoolean(null,value);}}catch(Exception failure){throw new AssertionError(failure);}}
 static void cancelQueued(){GpuQualification1961.Probe p=GpuQualification1961.pending;check(p!=null,"cancel has a concrete queued owner");boolean cancelled=false;try{p.run(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(java.util.concurrent.CancellationException expected){cancelled=true;}finally{p.close();GpuQualification1961.pending=null;}check(cancelled,"capture cancellation exits probe before proof completion");}
 static void runQueued(){GpuQualification1961.Probe p=GpuQualification1961.pending;check(p!=null,"concrete queued owner");try{p.run(GO);}finally{p.close();GpuQualification1961.pending=null;}}
 static void bounded(){check(GpuCertificates1990.restoreCalls<100,"certificate queries remain bounded without recursion");}
 public static void main(String[] args)throws Exception {
  for(int profile:new int[]{6,7})for(boolean ready:new boolean[]{false,true}){
   reset();regional();legacy(profile,true,true,false);prepared(profile,ready);
   check(GpuRegionalDefault19115.legacyAdaptiveCached19115(),"old exact and speed pair blocks independently of preparation");check(!GpuRegionalDefault19115.faster(),"regional cannot replace old independently faster adaptive route");
   GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==0&&!GpuRegionalDefault19115.pending()&&GpuNoise1960.regionalPrepares==0,"old adaptive pair prevents regional queue reservation");
   bounded();GpuStrongDefault1990.request1994();
   check(!GpuRegionalDefault19115.pending()&&GpuNoise1960.regionalPrepares==0,"standard requester leaves regional preparation inactive");
   if(ready){check(GpuQualification1961.scheduled==0,"prepared old adaptive needs no extra job");check(GpuStrongDefault1990.profile1994(3060,4080)==profile,"prepared old adaptive remains selected");}
   else {check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains(profile==7?"layout1994":"stripe1998"),"unprepared old adaptive owns the sole preparation job");runQueued();check(GpuNoise1960.regionalPrepares==0&&GpuNoise1960.supportCalls==1,"only the cached adaptive shader is prepared");check(GpuStrongDefault1990.profile1994(3060,4080)==profile,"cached adaptive preparation restores intended route");}
   check(GpuStrongDefault1990.profile1994(4080,3060)==profile&&GpuStrongDefault1990.profile1994(65,65)==profile,"both tested orientations and old shapes preserve adaptive route");bounded();
  }
  for(int profile:new int[]{6,7})for(int partial=0;partial<3;partial++){
   reset();regional();legacy(profile,partial!=1,partial!=0,partial==2);prepared(profile,false);
   check(!GpuRegionalDefault19115.legacyAdaptiveCached19115(),"partial or new-namespace proof is not an old adaptive pair");check(GpuRegionalDefault19115.faster(),"partial or new-namespace proof leaves regional eligible");bounded();
   GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==0,"already ready regional requires no reservation");
  }
  for(int profile:new int[]{6,7}){
   reset();legacyCompleted(true);legacy(profile,true,true,true);GpuNoise1960.regionalPrepared=false;GpuRegionalDefault19115.request();check(GpuRegionalDefault19115.pending()&&GpuQualification1961.scheduled==1,"new namespace alone permits optional regional preparation");GpuQualification1961.pending.close();
   reset();regional();legacy(profile,true,true,false);GpuCertificates1990.reject(exact(profile),"old candidate quality rejection");check(!GpuRegionalDefault19115.legacyAdaptiveCached19115()&&GpuRegionalDefault19115.faster(),"rejected adaptive quality cannot veto valid regional path");
   reset();regional();legacy(profile,true,true,false);GpuNoise1960.regionalPrepared=false;check(!GpuRegionalDefault19115.qualify(GO,false),"direct cached regional preparation refuses legacy adaptive owner");check(GpuNoise1960.regionalPrepares==0&&GpuStrong1960.submits==0&&GpuCertificates1990.regionalWrites==0,"legacy owner checked before compilation and proof writes");
   reset();legacyCompleted(true);GpuNoise1960.regionalPrepared=false;GpuRegionalDefault19115.request();check(GpuRegionalDefault19115.pending(),"regional request owns pending before certificate race");legacy(profile,true,true,false);runQueued();
   check(!GpuRegionalDefault19115.pending(),"certificate race closes regional pending owner");check(GpuNoise1960.regionalPrepares==0&&GpuStrong1960.submits==0&&GpuCertificates1990.regionalWrites==0,"queued-to-running legacy certificate race does not compile or certify regional");
   GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1,"prepared restored legacy after canceled regional needs no new work");check(GpuStrongDefault1990.profile1994(3060,4080)==profile,"late legacy certificate still selects adaptive route");
  }
  reset();regional();legacy(6,true,true,false);legacy(7,true,true,false);GpuNoise1960.warmed6=GpuNoise1960.warmed7=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("layout1994"),"both cached old routes preserve original profile7 preparation priority");runQueued();check(GpuStrongDefault1990.profile1994(3060,4080)==7,"profile7 priority preserved after preparation");bounded();
  reset();regional();legacy(6,true,true,false);legacy(7,true,true,true);check(!GpuRegionalDefault19115.faster()&&GpuStrongDefault1990.profile1994(3060,4080)==6,"new-namespace7 cannot invalidate existing old6 speed evidence");bounded();
  reset();regional();legacy(7,true,true,false);GpuNoise1960.warmed7=false;GpuQualification1961.optionalAllowed1995=false;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&!GpuRegionalDefault19115.pending()&&GpuQualification1961.lastKey.contains("layout1994"),"existing cached preparation semantics retained before optional benchmarks");runQueued();check(GpuNoise1960.regionalPrepares==0,"optional regional never compiles before publication");
  for(int profile:new int[]{6,7}){
   reset();legacyCompleted(true);GpuQualification1961.injectLegacyProfile=profile;boolean qualified=GpuRegionalDefault19115.qualify(GO);check(GpuCertificates1990.regionalWrites==0,"final-boundary race cannot persist a mismatched baseline proof");check(!qualified,"legacy proof arriving at final boundary vetoes regional qualification");check(GpuRegionalDefault19115.legacyAdaptiveCached19115()&&!GpuRegionalDefault19115.faster(),"late old adaptive remains authoritative");bounded();
  }
  // Actual helper + actual GSD scheduling boundary. Controlled GPU failures
  // complete the old candidates without claiming a speed or pixel result.
  reset();GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==0&&!GpuRegionalDefault19115.pending(),"unfinished legacy candidates refuse regional heavy reservation");
  check(!GpuRegionalDefault19115.qualify(GO),"unfinished legacy candidates refuse direct regional heavy qualification");check(GpuNoise1960.regionalPrepares==0&&GpuStrong1960.submits==0,"unfinished legacy boundary precedes optional GPU preparation");
  reset();GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==1&&GpuQualification1961.lastKey.contains("stripe1998")&&!GpuRegionalDefault19115.pending(),"cold first publication preserves stripe6 opportunity");
  GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==1,"queued adaptive prevents a second heavy owner");
  for(int capture=0;capture<3;capture++){cancelQueued();check(!GpuStrongDefault1990.Adaptive1998.completed19115()&&!GpuStrongDefault1990.regionalHeavyAllowed19115(),"cancelled legacy attempt is not a terminal candidate");ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.lastKey.contains("stripe1998")&&!GpuRegionalDefault19115.pending(),"repeated captures retry6 without regional starvation");}
  GpuNoise1960.supported=false;runQueued();int after6=GpuQualification1961.scheduled;check(!GpuStrongDefault1990.regionalHeavyAllowed19115(),"one completed legacy candidate cannot unlock regional heavy");
  GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==after6,"completed6 keeps existing next-capture barrier for7");
  ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==after6+1&&GpuQualification1961.lastKey.contains("layout1994"),"next publication preserves tile7 opportunity");
  cancelQueued();ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.lastKey.contains("layout1994")&&!GpuRegionalDefault19115.pending(),"cancelled7 retries7 before regional");runQueued();int after7=GpuQualification1961.scheduled;
  check(GpuStrongDefault1990.regionalHeavyAllowed19115()&&GpuQualification1961.pending==null&&after7==after6+2,"both completed candidates do not chain another queue job");
  ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==after7+1&&GpuQualification1961.lastKey.contains("regional19115")&&GpuRegionalDefault19115.pending(),"later publication may request regional only after both legacy completions");
  cancelQueued();ProcessingTiming1947.epoch++;GpuStrongDefault1990.request1994();check(GpuQualification1961.scheduled==after7+2&&GpuQualification1961.lastKey.contains("regional19115")&&GpuStrongDefault1990.regionalHeavyAllowed19115(),"regional cancellation rearms without postponing unfinished legacy work");GpuQualification1961.pending.close();GpuQualification1961.pending=null;
  reset();GpuCertificates1990.reject(exact(6),"sticky old6 mismatch");GpuStrongDefault1990.request1994();check(GpuQualification1961.lastKey.contains("layout1994")&&!GpuRegionalDefault19115.pending(),"permanent6 rejection preserves direct7 opportunity");GpuQualification1961.pending.close();GpuQualification1961.pending=null;
  reset();GpuCertificates1990.reject(exact(6),"sticky old6 mismatch");GpuCertificates1990.reject(exact(7),"sticky old7 mismatch");GpuStrongDefault1990.request1994();check(GpuStrongDefault1990.regionalHeavyAllowed19115()&&GpuRegionalDefault19115.pending()&&GpuQualification1961.lastKey.contains("regional19115"),"both permanent rejections permit regional without repeated old proofs");GpuQualification1961.pending.close();GpuQualification1961.pending=null;
  reset();regional();GpuNoise1960.regionalPrepared=false;GpuStrongDefault1990.request1994();check(GpuRegionalDefault19115.pending()&&!GpuStrongDefault1990.regionalHeavyAllowed19115(),"cached regional preparation is separate from new heavy qualification");runQueued();check(GpuNoise1960.regionalPrepares==1&&GpuStrong1960.submits==0&&GpuQualification1961.scheduled==1,"cached regional preparation cannot silently launch heavy proof or chained job");
  reset();GpuStrongDefault1990.request1994();regional();GpuNoise1960.regionalPrepared=false;GpuRegionalDefault19115.request();check(GpuQualification1961.scheduled==1&&!GpuRegionalDefault19115.pending(),"even cached regional preparation cannot coqueue an active adaptive owner");GpuQualification1961.pending.close();GpuQualification1961.pending=null;
  reset();legacyCompleted(true);GpuRegionalDefault19115.request();legacyCompleted(false);runQueued();check(GpuNoise1960.regionalPrepares==0&&GpuStrong1960.submits==0&&GpuCertificates1990.regionalWrites==0&&!GpuRegionalDefault19115.pending(),"queued-to-running terminal loss cannot compile or certify regional");
  reset();legacyCompleted(true);GpuQualification1961.clearLegacyAtFinal=true;check(!GpuRegionalDefault19115.qualify(GO),"final terminal loss vetoes regional heavy qualification");check(GpuCertificates1990.regionalWrites==0,"final terminal loss cannot persist regional proof");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"actual_selection_owners\":true,\"controlled_gpu_peers\":true,\"physical_android_tested\":false}");
 }
}
