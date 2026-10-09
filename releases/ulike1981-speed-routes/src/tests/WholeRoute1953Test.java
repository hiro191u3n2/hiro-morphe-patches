package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual gate/background/history Java, owned mock photos and mocked Android
 * preferences. Native execution and physical Android speed are not claimed. */
public final class WholeRoute1953Test {
 static final AtomicInteger assertions=new AtomicInteger();static int scenarios;
 static void req(boolean ok,String why){assertions.incrementAndGet();if(!ok)throw new AssertionError(why);}
 static final class Timer extends WholeRoute1953.Clock {long time;synchronized long now(){return time;}synchronized void add(long n){time+=n;}}
 static int[] image(int seed){int[] out=new int[193];long n=seed;for(int i=0;i<out.length;i++){n=n*1664525+1013904223;out[i]=(int)n;}return out;}
 static int pixel(int p){return (p&0xff000000)|(((p>>>16)&255)*7+((p>>>8)&255))/8<<16|((((p>>>8)&255)*7+(p&255))/8<<8)|((p&255)*7+((p>>>16)&255))/8;}
 static void filter(int[] p){for(int i=0;i<p.length;i++)p[i]=pixel(p[i]);}
 static int[] key(int setting){return new int[]{1953,193,1,setting,4,128};}
 static final class Photo implements WholeRoute1953.Work {
  final Timer timer;final int[] before,original;int[] published,candidate;
  int cpu,gpu,pub,snapshots,discard;volatile boolean returned;
  boolean cpuReliable=true,snapshotFail,referenceFail,cpuFalse,cpuThrow,gpuFail;
  long gpuNanos=30,cpuNanos=100;long budget=8192;Proof proof;
  boolean chooseDispatch;int dispatchRows=32,dispatchThrow;
  long[] modeTimes={50,35,20};boolean[] modeBad=new boolean[3];
  public boolean dispatchSelection(){
   if(dispatchThrow==1)throw new IllegalStateException("optional dispatch capability");
   if(dispatchThrow==2)throw new UnsatisfiedLinkError("optional dispatch capability");
   if(dispatchThrow==3)throw new OutOfMemoryError("optional dispatch capability");
   return chooseDispatch;
  }
  public void setDispatchRows(int rows){dispatchRows=rows;}
  Photo(Timer t,int seed){timer=t;before=image(seed);original=before.clone();published=original;}
  public long probeBytes(){return budget;}
  public WholeRoute1953.Probe snapshotProbe(){snapshots++;if(snapshotFail)throw new OutOfMemoryError("snapshot");timer.add(11);proof=new Proof(this);return proof;}
  public boolean cpu(){cpu++;if(cpuThrow)throw new IllegalStateException("original CPU failure");filter(original);timer.add(cpuNanos);return !cpuFalse;}
  public boolean gpu(){gpu++;candidate=original.clone();filter(candidate);timer.add(gpuNanos);return !gpuFail;}
  public boolean equal(){throw new AssertionError("foreground equality never needed");}
  public void publishGpu(){pub++;published=candidate;}
  public void discardGpu(){discard++;if(published!=candidate)candidate=null;}
  public boolean timingReliable(){return cpuReliable;}
  void correct(){int[] expected=before.clone();filter(expected);req(Arrays.equals(expected,published),"current photo exact pixels");}
 }
 static final class Proof implements WholeRoute1953.Probe {
  final Photo photo;final int[] input;int[] reference,candidate;
  volatile boolean idle=true,reliable=true,bad,miss,cancelAtEqual;volatile long time=30;
  volatile boolean released;int gpu,equal,discards;long bytes=8192;String establishEnvironment;
  int dispatchRows=32,candidateDiscards;
  public boolean dispatchSelection(){return photo.chooseDispatch;}
  public void setDispatchRows(int rows){dispatchRows=rows;}
  public void discardCandidate(){candidateDiscards++;candidate=null;}
  int gpuThrow;CountDownLatch entered,drain,comparisonEntered,comparisonDrain,idleEntered,idleDrain;
  WholeRoute1953.Engine engine;
  Proof(Photo photo){this.photo=photo;input=photo.original.clone();}
  public boolean captureReference(){if(photo.referenceFail)return false;reference=photo.original.clone();photo.timer.add(17);return true;}
  public long bytes(){return bytes;}
  public boolean gpu(WholeRoute1953.Cancellation cancel){
   req(photo.returned,"foreground CPU returned before background GPU starts");gpu++;
   req(reference!=null,"CPU reference snapshot completed before GPU");
   candidate=input.clone();filter(candidate);int index=dispatchRows==32?0:dispatchRows==64?1:2;
   if(bad||(photo.chooseDispatch&&photo.modeBad[index]))candidate[candidate.length-1]^=1;
   if(entered!=null)entered.countDown();
   if(drain!=null)awaitUninterruptibly(drain);
   photo.timer.add(photo.chooseDispatch?photo.modeTimes[index]:time);
   if(gpuThrow==1)throw new OutOfMemoryError("GPU candidate");
   if(gpuThrow==2)throw new UnsatisfiedLinkError("GPU link");
   if(gpuThrow==3)throw new IllegalStateException("GPU runtime");
   if(establishEnvironment!=null)GpuFinish1953.value=establishEnvironment;
   return !miss&&!cancel.cancelled();
  }
  public boolean equal(WholeRoute1953.Cancellation cancel){equal++;if(comparisonEntered!=null)comparisonEntered.countDown();if(comparisonDrain!=null)awaitUninterruptibly(comparisonDrain);if(cancelAtEqual)engine.foregroundStarted();return !cancel.cancelled()&&Arrays.equals(reference,candidate);}
  public boolean idle(){if(idleEntered!=null){idleEntered.countDown();awaitUninterruptibly(idleDrain);}return idle;}
  public boolean timingReliable(){return reliable;}
  public void discard(){discards++;candidate=null;reference=null;released=true;}
 }
 static void awaitUninterruptibly(CountDownLatch latch){boolean interrupted=false;for(;;)try{if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("test drain timed out");break;}catch(InterruptedException wake){interrupted=true;}if(interrupted)Thread.currentThread().interrupt();}
 static void released(WholeRoute1953.Engine engine)throws Exception{for(int i=0;i<400;i++){if(engine.retainedBytes==0)return;Thread.sleep(5);}throw new AssertionError("background ownership not released");}
 static Photo foreground(WholeRoute1953.Engine engine,Timer timer,int[] key,Object trace,int seed){Photo p=new Photo(timer,seed);req(engine.run(key,p,trace),"real foreground result");p.returned=true;p.correct();return p;}
 static void complete(WholeRoute1953.Engine engine,Photo p,Object trace)throws Exception{engine.saved(trace,true);engine.wake();released(engine);req(p.proof.released,"all background snapshots released");req(p.pub==0,"background candidate never published");p.correct();}
 static void train(WholeRoute1953.Engine e,Timer t,int[] key)throws Exception{for(int i=0;i<3;i++){Object trace=new Object();Photo p=foreground(e,t,key,trace,31+i);req(p.cpu==1&&p.gpu==0,"unadmitted real photo CPU exactly once");if(i==0)p.proof.time=1000;complete(e,p,trace);req(p.proof.gpu==1&&p.proof.equal==1,"whole background exact proof");}}
 static void immediateAndTraceIdle()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo p=foreground(e,t,key(1),trace,10);
  req(e.retainedBytes==p.budget,"full snapshot/native peak budget reserved");req(p.proof.gpu==0,"no synchronous foreground probation");
  e.saved(new Object(),true);Thread.sleep(20);req(p.proof.gpu==0,"unrelated saved trace cannot arm proof");
  p.proof.idle=false;e.saved(trace,true);e.wake();Thread.sleep(20);req(p.proof.gpu==0,"success waits for all capture/save/codec activity to drain");
  p.proof.idle=true;e.wake();released(e);req(p.proof.gpu==1&&p.proof.released,"idle successful trace runs proof");req(p.pub==0,"background never publishes");p.correct();
 }
 static void admissionAndSnapshots()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);train(e,t,key(2));
  for(int i=0;i<6;i++){Photo p=foreground(e,t,key(2),new Object(),900+i);req(p.gpu==1&&p.cpu==0&&p.snapshots==0,"qualified foreground GPU only, no snapshots/CPU duplicate");req(p.pub==1&&p.discard==1,"foreground owned GPU output publication");}
  Object trace=new Object();Photo p=foreground(e,t,key(3),trace,49);Arrays.fill(p.original,0xabcdef01);e.saved(trace,true);e.wake();released(e);
  req(p.proof.gpu==1&&p.proof.equal==1,"probe reads owned input/reference after original can change/recycle");req(p.proof.released,"owned snapshots release");
 }
 static void singlePendingAndBudget()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo first=foreground(e,t,key(4),trace,81);
  for(int i=0;i<8;i++){Photo p=foreground(e,t,key(5+i),new Object(),82+i);req(p.cpu==1&&p.snapshots==0&&p.gpu==0,"single globally pending probe budget");req(e.retainedBytes==first.budget,"no second reservation");}
  e.foregroundStarted();req(e.retainedBytes==0&&first.proof.released,"queued optional proof budget surrendered before capture readiness returns");released(e);req(first.proof.released&&first.proof.gpu==0,"cancel waiting trace safely releases without GPU");req(first.proof.discards==1,"queued proof discard exactly once");
  Photo fail=new Photo(t,101);fail.snapshotFail=true;req(e.run(key(4),fail,new Object()),"snapshot OOM leaves original CPU result");fail.returned=true;released(e);fail.correct();
  req(e.retainedBytes==0,"partial snapshot failure budget released");
  Photo retry=foreground(e,t,key(4),trace,102);complete(e,retry,trace);req(retry.proof.gpu==1,"snapshot cancellation doesn't permanently disable shape");
 }
 static void failedTraceAndReference()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo p=foreground(e,t,key(6),trace,16);e.saved(trace,false);released(e);
  req(p.proof.gpu==0&&p.proof.released,"failed saved photograph never GPU-validates");
  Photo badRef=new Photo(t,17);badRef.referenceFail=true;req(e.run(key(6),badRef,new Object()),"reference snapshot failure CPU success");badRef.returned=true;released(e);badRef.correct();
  Photo next=foreground(e,t,key(6),trace,18);complete(e,next,trace);req(next.proof.gpu==1,"failed save/snapshot not quality rejection");
 }
 static void cancellationDrainAndRetry()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo p=foreground(e,t,key(7),trace,70);p.proof.entered=new CountDownLatch(1);p.proof.drain=new CountDownLatch(1);
  e.saved(trace,true);e.wake();req(p.proof.entered.await(2,TimeUnit.SECONDS),"GPU proof begins only after success");long start=System.nanoTime();e.foregroundStarted();long elapsed=System.nanoTime()-start;
  req(elapsed<200000000L,"new capture cancellation never joins GPU");req(e.retainedBytes>0&&!p.proof.released,"in-flight snapshots remain owned until real GPU drain");
  Photo competing=foreground(e,t,key(8),new Object(),71);req(competing.snapshots==0,"no new background allocation while previous GPU drains");
  p.proof.drain.countDown();released(e);req(p.proof.released,"canceled GPU releases after drain");req(p.proof.equal==0,"canceled result never counts as equality/speed win");
  train(e,t,key(7));Photo admitted=foreground(e,t,key(7),new Object(),72);req(admitted.gpu==1&&admitted.cpu==0,"cancellation didn't disable valid route");
 }
 static void cancelBeforeCommit()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);for(int i=0;i<2;i++){Object trace=new Object();Photo p=foreground(e,t,key(9),trace,40+i);complete(e,p,trace);}
  Object trace=new Object();Photo p=foreground(e,t,key(9),trace,43);p.proof.cancelAtEqual=true;p.proof.engine=e;complete(e,p,trace);
  Photo next=foreground(e,t,key(9),new Object(),44);req(next.cpu==1&&next.gpu==0,"capture starting at comparison/commit prevents admission");e.foregroundStarted();released(e);
 }
 static void idleOutsideMonitor()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo p=foreground(e,t,key(10),trace,88);p.proof.idleEntered=new CountDownLatch(1);p.proof.idleDrain=new CountDownLatch(1);
  e.saved(trace,true);e.wake();req(p.proof.idleEntered.await(2,TimeUnit.SECONDS),"idle query entered");long start=System.nanoTime();e.foregroundStarted();
  req(System.nanoTime()-start<200000000L,"idle callback never holds Engine monitor against capture lock");p.proof.idleDrain.countDown();released(e);req(p.proof.gpu==0,"capture wins before GPU submission");
  req(p.proof.discards==1,"idle-query cancellation and worker cleanup do not double free snapshots");
 }
 static void mismatchAndSlower(boolean bad)throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo cold=foreground(e,t,key(11),trace,60);complete(e,cold,trace);
  trace=new Object();Photo p=foreground(e,t,key(11),trace,61);p.proof.bad=bad;if(!bad)p.proof.time=105;complete(e,p,trace);
  Photo next=foreground(e,t,key(11),new Object(),62);req(next.cpu==1&&next.snapshots==0,"mismatch or complete route slower than CPU disables shape");req(next.gpu==0,"wrong/slower background pixels never published");
 }
 static void failures(int kind)throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Object trace=new Object();Photo p=foreground(e,t,key(12),trace,63);p.proof.gpuThrow=kind;complete(e,p,trace);
  Photo next=foreground(e,t,key(12),new Object(),64);req(next.snapshots==0&&next.gpu==0,"GPU failure disables route without failing real saved image");
  Photo other=foreground(e,t,key(13),new Object(),65);req(other.snapshots==(kind==2?0:1),"link globally disables; OOM/runtime only shape");e.foregroundStarted();released(e);
 }
 static void unreliableAndSlowdown()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);
  for(int i=0;i<4;i++){Object trace=new Object();Photo p=new Photo(t,50+i);p.cpuReliable=false;req(e.run(key(14),p,trace),"CPU load unreliable result");p.returned=true;complete(e,p,trace);}
  for(int i=0;i<2;i++){Object trace=new Object();Photo p=foreground(e,t,key(14),trace,60+i);complete(e,p,trace);}
  Photo slow=foreground(e,t,key(14),new Object(),66);req(slow.cpu==0&&slow.gpu==1,"two reliable complete wins admit after cold");
  Photo thermal=new Photo(t,67);thermal.gpuNanos=110;req(e.run(key(14),thermal,new Object()),"thermal slowdown retains current exact GPU image");thermal.returned=true;thermal.correct();req(thermal.cpu==0,"slowdown doesn't duplicate current photo CPU");
  Photo next=foreground(e,t,key(14),new Object(),68);req(next.cpu==1&&next.gpu==0&&next.snapshots==0,"next photograph returns CPU after ten percent slowdown");
 }
 static void originalErrorsAndInvalidKeys()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);Photo p=new Photo(t,1);p.cpuThrow=true;boolean thrown=false;try{e.run(key(15),p,new Object());}catch(IllegalStateException original){thrown=original.getMessage().equals("original CPU failure");}req(thrown,"original CPU exception propagates");released(e);req(p.proof.released,"failed CPU reference frees snapshot");
  for(int[] invalid:new int[][]{null,new int[0],new int[65]}){Photo photo=foreground(e,t,invalid,new Object(),2);req(photo.snapshots==0&&photo.gpu==0,"invalid key CPU only");}
  Photo noTrace=foreground(e,t,key(15),null,3);req(noTrace.snapshots==0,"unattributed save trace never prepares proof");
 }
 static final class MemoryPreferences implements SharedPreferences {
  final Map<String,String> values=new HashMap<String,String>();
  public synchronized String getString(String key,String fallback){String value=values.get(key);return value==null?fallback:value;}
  public synchronized Map<String,?> getAll(){return new HashMap<String,String>(values);}
  public Editor edit(){return new Editor(){boolean clear;final Map<String,String> updates=new HashMap<String,String>();public Editor clear(){clear=true;return this;}public Editor remove(String key){updates.put(key,null);return this;}public Editor putString(String key,String value){updates.put(key,value);return this;}public void apply(){synchronized(MemoryPreferences.this){if(clear)values.clear();for(Map.Entry<String,String> entry:updates.entrySet()){if(entry.getValue()==null)values.remove(entry.getKey());else values.put(entry.getKey(),entry.getValue());}}}};}
 }
 static final class App extends Context {final MemoryPreferences prefs=new MemoryPreferences();public String getPackageName(){return "com.host.ulike";}public PackageManager getPackageManager(){return new PackageManager();}public SharedPreferences getSharedPreferences(String name,int mode){req(name.equals("ulike_route1953")&&mode==MODE_PRIVATE,"actual Android profile store contract");return prefs;}}
 static WholeRoute1953.History history(App app)throws Exception{Class<?> type=Class.forName("com.hiro.ulike.WholeRoute1953$PreferencesHistory");Constructor<?> constructor=type.getDeclaredConstructor(Context.class);constructor.setAccessible(true);return (WholeRoute1953.History)constructor.newInstance(app);}
 static void persistence()throws Exception{
  scenarios++;GpuFinish1953.value="vendor|renderer|GLESdriverA|nativeABI19531|sourcesHashA";App app=new App();WholeRoute1953.History h=history(app);String env=h.environment();req(env!=null&&env.length()==64,"strict complete environment fingerprint");req(h.restore(key(21),env)==0,"unqualified history absent");
  Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,h);train(e,t,key(21));req(h.restore(key(21),env)==100,"actual SharedPreferences stores only qualified CPU baseline");
  WholeRoute1953.Engine restored=new WholeRoute1953.Engine(t,history(app));Photo fast=foreground(restored,t,key(21),new Object(),550);req(fast.gpu==1&&fast.cpu==0,"same verified runtime qualified history avoids repeated probation");
  GpuFinish1953.value="";WholeRoute1953.Engine unknown=new WholeRoute1953.Engine(t,history(app));Photo uncertain=foreground(unknown,t,key(21),new Object(),551);req(uncertain.cpu==1&&uncertain.gpu==0,"unknown driver never restores admission");unknown.foregroundStarted();released(unknown);
  GpuFinish1953.value="";WholeRoute1953.Engine established=new WholeRoute1953.Engine(t,history(app));Object coldTrace=new Object();Photo cold=foreground(established,t,key(21),coldTrace,552);cold.proof.establishEnvironment="vendor|renderer|GLESdriverA|nativeABI19531|sourcesHashA";cold.proof.time=1000;complete(established,cold,coldTrace);Photo resumed=foreground(established,t,key(21),new Object(),553);req(resumed.gpu==1&&resumed.cpu==0,"newly verified matching driver restores history only after current exact cold proof");
  GpuFinish1953.value="vendor|renderer|GLESdriverB|nativeABI19531|sourcesHashA";WholeRoute1953.History different=history(app);req(different.restore(key(21),different.environment())==0,"driver change invalidates qualified profile");
  GpuFinish1953.value="vendor|renderer|GLESdriverA|nativeABI19531|sourcesHashB";different=history(app);req(different.restore(key(21),different.environment())==0,"native/source changes invalidate qualified profile");
  GpuFinish1953.value="vendor|renderer|GLESdriverA|nativeABI19531|sourcesHashA";Build.FINGERPRINT="host-device-os-B";different=history(app);req(different.restore(key(21),different.environment())==0,"OS fingerprint changes invalidate profile");Build.FINGERPRINT="host-device-os-A";
  req(h.restore(key(22),env)==0,"settings/geometry key never shares unrelated proof");
  for(String name:app.prefs.values.keySet())if(name.startsWith("route-"))app.prefs.values.put(name,"1953:3:100:not-a-time:corrupt");req(h.restore(key(21),env)==0,"corrupt history rejected");
  for(int i=0;i<40;i++)h.qualified(key(100+i),env,100+i);int count=0;for(String name:app.prefs.values.keySet())if(name.startsWith("route-"))count++;req(count<=24,"persisted histories bounded to twenty-four scalar records");
  for(Map.Entry<String,String> entry:app.prefs.values.entrySet()){req(!entry.getValue().contains(Arrays.toString(image(550))),"no pixels persisted");req(entry.getKey().equals("environment")||entry.getKey().matches("route-[0-9a-f]{64}"),"history only scalar key/environment digests");}
 }
 static void dispatchChoices()throws Exception{
  for(int caseIndex=0;caseIndex<5;caseIndex++){
   scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);
   long[][] durations={{50,55,60},{50,30,40},{50,35,20},{50,49,49},{50,35,20}};
   int[] expected={32,64,0,32,64};int[] setting=key(300+caseIndex);
   for(int n=0;n<3;n++){
    Object trace=new Object();Photo p=new Photo(t,800+caseIndex*10+n);p.chooseDispatch=true;p.modeTimes=durations[caseIndex].clone();
    if(caseIndex==4)p.modeBad[2]=true;
    req(e.run(setting,p,trace),"dispatch probation saves CPU result");p.returned=true;p.correct();
    req(p.cpu==1&&p.gpu==0,"dispatch selection stays after successful save");
    complete(e,p,trace);
    req(p.proof.gpu==3&&p.proof.equal==3,"all dispatch modes have full exact proof");
    req(p.proof.candidateDiscards==3,"private candidates drain/release between modes");
   }
   Photo fast=new Photo(t,999+caseIndex);fast.chooseDispatch=true;
   req(e.run(setting,fast,new Object()),"qualified selected dispatch produces image");fast.returned=true;fast.correct();
   req(fast.gpu==1&&fast.cpu==0&&fast.snapshots==0,"selected mode avoids foreground duplication");
   req(fast.dispatchRows==expected[caseIndex],"complete-route fastest exact mode retained; five-percent batching margin");
  }
 }
 static void dispatchHistoryAndCancellation()throws Exception{
  scenarios++;Timer t=new Timer();
  WholeRoute1953.History history=new WholeRoute1953.History(){public String environment(){return "verified-env";}public long restore(int[] k,String env){return 100;}public void qualified(int[] k,String env,long cpu){}public void rejected(int[] k,String env){}};
  WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,history);Photo p=new Photo(t,1200);p.chooseDispatch=true;Object trace=new Object();
  req(e.run(key(400),p,trace),"scalar history still yields valid CPU result");p.returned=true;p.correct();
  req(p.cpu==1&&p.gpu==0,"history cannot restore an unverified dispatch mode");
  p.proof.engine=e;p.proof.cancelAtEqual=true;complete(e,p,trace);
  req(p.proof.gpu==1&&p.proof.candidateDiscards==1,"new capture cancels remaining mode trials after current drain");
  Photo next=new Photo(t,1201);next.chooseDispatch=true;
  req(e.run(key(400),next,new Object()),"canceled mode trial leaves CPU usable");next.returned=true;next.correct();
  req(next.cpu==1&&next.gpu==0,"canceled proof cannot select/admit any mode");e.foregroundStarted();released(e);
 }
 static void dispatchUnreliableRetry()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);int[] setting=key(401);
  for(int n=0;n<3;n++){
   Object trace=new Object();Photo p=new Photo(t,1300+n);p.chooseDispatch=true;
   req(e.run(setting,p,trace),"unreliable dispatch trial saves CPU result");p.returned=true;p.correct();
   req(p.cpu==1&&p.gpu==0,"unreliable full-route timings never admit foreground GPU");
   p.proof.reliable=false;complete(e,p,trace);
   req(p.proof.gpu==3&&p.proof.equal==3&&p.proof.candidateDiscards==3,
    "all unreliable but exact modes are fully tested and drained");
  }
  // The earlier exact cold proof still counts, but two reliable complete-route
  // wins are required after competition ends. Unreliable proof must not disable
  // the shape and prevent these real saved photographs from retrying it.
  for(int n=0;n<2;n++){
   Object trace=new Object();Photo p=new Photo(t,1310+n);p.chooseDispatch=true;
   req(e.run(setting,p,trace),"competition-cleared dispatch trial retries");p.returned=true;p.correct();
   req(p.cpu==1&&p.gpu==0&&p.snapshots==1,"retry still runs foreground CPU exactly once");complete(e,p,trace);
   req(p.proof.gpu==3&&p.proof.equal==3&&p.proof.candidateDiscards==3,
    "reliable retry compares every dispatch candidate and releases ownership");
  }
  Photo fast=new Photo(t,1320);fast.chooseDispatch=true;
  req(e.run(setting,fast,new Object()),"two post-competition wins produce exact saved image");fast.returned=true;fast.correct();
  req(fast.gpu==1&&fast.cpu==0&&fast.snapshots==0&&fast.dispatchRows==0,
   "unreliable modes cannot reject route and stable reliable full-band mode qualifies");
 }
 static void dispatchChangingWinner()throws Exception{
  scenarios++;Timer t=new Timer();WholeRoute1953.Engine e=new WholeRoute1953.Engine(t);int[] setting=key(402);
  long[][] durations={{50,35,20},{50,35,20},{50,20,35},{50,35,20},{20,35,50},{20,35,50}};
  for(int n=0;n<durations.length;n++){
   Object trace=new Object();Photo p=new Photo(t,1400+n);p.chooseDispatch=true;p.modeTimes=durations[n].clone();
   req(e.run(setting,p,trace),"changing-winner dispatch trial saves original CPU photograph");p.returned=true;p.correct();
   req(p.cpu==1&&p.gpu==0&&p.snapshots==1,
    "wins from different exact fastest modes cannot combine into early admission");complete(e,p,trace);
   req(p.proof.gpu==3&&p.proof.equal==3&&p.proof.candidateDiscards==3,
    "changing winner keeps every proof candidate private and drained");
  }
  Photo fast=new Photo(t,1450);fast.chooseDispatch=true;
  req(e.run(setting,fast,new Object()),"stable final winner produces exact image");fast.returned=true;fast.correct();
  req(fast.gpu==1&&fast.cpu==0&&fast.snapshots==0&&fast.dispatchRows==32,
   "only two reliable wins for the same final dispatch mode qualify");
 }
 static void dispatchCapabilityFailures()throws Exception{
  for(int failure=1;failure<=3;failure++){
   scenarios++;Timer t=new Timer();
   WholeRoute1953.History history=new WholeRoute1953.History(){public String environment(){return "verified-dispatch-env";}public long restore(int[] k,String env){return 100;}public void qualified(int[] k,String env,long cpu){}public void rejected(int[] k,String env){}};
   WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,history);int[] setting=key(403+failure);
   Photo broken=new Photo(t,1500+failure);broken.chooseDispatch=true;broken.dispatchThrow=failure;
   req(e.run(setting,broken,new Object()),"optional dispatch capability failure preserves CPU save "+failure);
   broken.returned=true;broken.correct();
   req(broken.cpu==1&&broken.gpu==0&&broken.snapshots==0&&e.retainedBytes==0,
    "runtime/linkage/OOM capability failures release claimed route before CPU fallback "+failure);
   for(int n=0;n<3;n++){
    Object trace=new Object();Photo p=new Photo(t,1510+failure*10+n);p.chooseDispatch=true;
    req(e.run(setting,p,trace),"same route can claim after optional capability failure "+failure);p.returned=true;p.correct();
    req(p.cpu==1&&p.gpu==0&&p.snapshots==1,
     "capability exception never leaves route permanently in flight "+failure);complete(e,p,trace);
    req(p.proof.gpu==3&&p.proof.equal==3&&p.proof.candidateDiscards==3,
     "post-exception route still completes all private mode proofs "+failure);
   }
   Photo fast=new Photo(t,1590+failure);fast.chooseDispatch=true;
   req(e.run(setting,fast,new Object()),"post-exception qualified GPU image executes "+failure);fast.returned=true;fast.correct();
   req(fast.gpu==1&&fast.cpu==0&&fast.snapshots==0&&fast.dispatchRows==0,
    "optional capability exception cannot disable valid later dispatch admission "+failure);
  }
 }
 public static void main(String[] args)throws Exception{
  immediateAndTraceIdle();admissionAndSnapshots();singlePendingAndBudget();failedTraceAndReference();cancellationDrainAndRetry();cancelBeforeCommit();idleOutsideMonitor();mismatchAndSlower(true);mismatchAndSlower(false);for(int i=1;i<=3;i++)failures(i);unreliableAndSlowdown();originalErrorsAndInvalidKeys();persistence();int beforeH22=assertions.get();dispatchChoices();dispatchHistoryAndCancellation();dispatchUnreliableRetry();dispatchChangingWinner();dispatchCapabilityFailures();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions.get()+",\"scenarios\":"+scenarios+",\"h22_assertions\":"+(assertions.get()-beforeH22)+"}");
 }
}
