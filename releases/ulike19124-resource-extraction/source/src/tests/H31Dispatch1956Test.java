package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import static com.hiro.ulike.WholeRoute1953Test.*;

/** Runs the actual Engine and PreferencesHistory; Android storage/driver are
 * mocks. No native GPU or physical-device performance claim is made. */
public final class H31Dispatch1956Test {
 static int scenarios;
 static final String GPU="vendor|renderer|GLESdriverA|nativeABI19531|sourcesHashA";
 static final class App extends Context {
  final MemoryPreferences prefs;
  String pkg="com.host.ulike",version="1.9.56";int code=1956;
  App(){this(new MemoryPreferences());}App(MemoryPreferences prefs){this.prefs=prefs;}
  public String getPackageName(){return pkg;}
  public PackageManager getPackageManager(){return new PackageManager(){public PackageInfo getPackageInfo(String name,int flags){PackageInfo p=new PackageInfo();p.versionName=version;p.versionCode=code;return p;}};}
  public SharedPreferences getSharedPreferences(String name,int mode){req(name.equals("ulike_route1953")&&mode==MODE_PRIVATE,"private Android preference store");return prefs;}
 }
 static WholeRoute1953.History history(Context app)throws Exception {
  Constructor<?> constructor=Class.forName("com.hiro.ulike.WholeRoute1953$PreferencesHistory").getDeclaredConstructor(Context.class);
  constructor.setAccessible(true);return (WholeRoute1953.History)constructor.newInstance(app);
 }
 static String digest(String value)throws Exception {
  byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder result=new StringBuilder();
  for(byte b:bytes)result.append(String.format("%02x",b&255));return result.toString();
 }
 static String recordName(int[] key,String env)throws Exception{return "route-"+digest(env+"|"+Arrays.toString(key));}
 static String checked(int[] key,String env,String core)throws Exception{return core+":"+digest(env+"|"+Arrays.toString(key)+"|"+core);}
 static long[] durations(int mode){return mode==32?new long[]{20,40,50}:mode==64?new long[]{50,20,40}:new long[]{50,40,20};}
 static WholeRoute1953.Engine qualify(App app,Timer timer,int[] key,int mode)throws Exception {
  WholeRoute1953.History h=history(app);WholeRoute1953.Engine e=new WholeRoute1953.Engine(timer,h);
  for(int i=0;i<3;i++){
   Photo photo=new Photo(timer,3200+i);photo.chooseDispatch=true;photo.modeTimes=durations(mode);Object trace=new Object();
   req(e.run(key,photo,trace),"qualification CPU result");photo.returned=true;photo.correct();
   req(photo.cpu==1&&photo.gpu==0,"qualification never publishes unproved GPU");complete(e,photo,trace);
   req(photo.proof.gpu==3&&photo.proof.equal==3&&photo.proof.candidateDiscards==3,"three drained full-pixel dispatch proofs");
   WholeRoute1953.Qualification restored=h.restoreQualified(key,h.environment());
   req(i<2?restored==null:restored!=null&&restored.dispatchRows==mode,
    "only cold equality plus two same-mode speed wins can persist a dispatch");
  }
  return e;
 }
 static Photo use(WholeRoute1953.Engine e,Timer t,int[] key)throws Exception {
  Photo p=new Photo(t,4801);p.chooseDispatch=true;
  req(e.run(key,p,new Object()),"restored foreground result");p.returned=true;p.correct();return p;
 }
 static void roundtripEveryMode()throws Exception {
  for(int mode:new int[]{32,64,0}){
   scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer timer=new Timer();int[] shape=key(550+mode);
   qualify(app,timer,shape,mode);WholeRoute1953.History h=history(app);String env=h.environment();
   WholeRoute1953.Qualification q=h.restoreQualified(shape,env);
   req(q!=null&&q.cpuBaseline==100&&q.dispatchVerified()&&q.dispatchRows==mode,"atomic qualification retains CPU baseline and certified dispatch");
   // Simulated process recreation retains serialized preference strings only.
   MemoryPreferences reloaded=new MemoryPreferences();reloaded.values.putAll(app.prefs.values);
   App nextApp=new App(reloaded);WholeRoute1953.Engine restart=new WholeRoute1953.Engine(timer,history(nextApp));
   Photo resumed=use(restart,timer,shape);
   req(resumed.gpu==1&&resumed.cpu==0&&resumed.snapshots==0&&resumed.dispatchRows==mode,
    "recreated engine restores the previously certified exact mode without repeat probation");
   Photo unrelated=use(restart,timer,key(990+mode));
   req(unrelated.cpu==1&&unrelated.gpu==0,"changed geometry/settings key cannot reuse admission");restart.foregroundStarted();released(restart);
  }
 }
 static void malformedAndLegacy()throws Exception {
  scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer t=new Timer();int[] shape=key(601);qualify(app,t,shape,64);
  WholeRoute1953.History h=history(app);String env=h.environment(),name=recordName(shape,env),valid=app.prefs.values.get(name);
  String[] f=valid.split(":");
  String[] invalid={"",valid.substring(0,valid.length()-1),valid+":trailing",
   checked(shape,env,"1953:3:100:1000"),
   checked(shape,env,"1956:2:100:1000:64:"+f[5]),
   checked(shape,env,"1956:1:100:1000:64:timing-only"),
   checked(shape,env,"1956:1:100:1000:16:"+f[5]),
   checked(shape,env,"1956:1:0:1000:64:"+f[5]),
   checked(shape,env,"1956:1:100:0:64:"+f[5]),
   checked(shape,env,"1956:1:9223372036854775808:1000:64:"+f[5]),
   valid.replace(":64:",":32:")};
  for(String broken:invalid){
   app.prefs.values.put(name,broken);req(h.restoreQualified(shape,env)==null&&h.restore(shape,env)==0,"legacy, incomplete, corrupt or unproved records never restore");
   WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,history(app));Photo p=use(e,t,shape);
   req(p.cpu==1&&p.gpu==0,"invalid proof record falls back to original CPU");e.foregroundStarted();released(e);
  }
  app.prefs.values.put(name,valid);req(h.restoreQualified(shape,env).dispatchRows==64,"valid unchanged certified record remains usable");
  // A valid scalar certificate carries no dispatch-selection proof.
  h.qualified(shape,env,100);WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,history(app));Photo p=use(e,t,shape);
  req(p.cpu==1&&p.gpu==0,"scalar-only qualification cannot certify a dispatch choice");e.foregroundStarted();released(e);
 }
 static void fingerprintChanges()throws Exception {
  scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer t=new Timer();int[] shape=key(602);qualify(app,t,shape,0);
  WholeRoute1953.History old=history(app);String original=old.environment();
  String[] changed={"","vendorB|renderer|GLESdriverA|nativeABI19531|sourcesHashA","vendor|rendererB|GLESdriverA|nativeABI19531|sourcesHashA","vendor|renderer|GLESdriverB|nativeABI19531|sourcesHashA","vendor|renderer|GLESdriverA|nativeABI19532|sourcesHashA","vendor|renderer|GLESdriverA|nativeABI19531|sourcesHashB"};
  for(String driver:changed){GpuFinish1953.value=driver;WholeRoute1953.History h=history(app);req(h.restoreQualified(shape,h.environment())==null,"driver/vendor/renderer/native ABI/source changes invalidate selection");req(old.restoreQualified(shape,original)==null,"stale supplied fingerprint cannot bypass current runtime check");}
  GpuFinish1953.value=GPU;
  String oldOs=Build.FINGERPRINT;Build.FINGERPRINT="host-os-changed";req(history(app).restoreQualified(shape,history(app).environment())==null,"OS change invalidates");Build.FINGERPRINT=oldOs;
  int oldSdk=Build.VERSION.SDK_INT;Build.VERSION.SDK_INT=oldSdk-1;req(history(app).restoreQualified(shape,history(app).environment())==null,"SDK change invalidates");Build.VERSION.SDK_INT=oldSdk;
  String[] oldAbis=Build.SUPPORTED_ABIS;Build.SUPPORTED_ABIS=new String[]{"x86_64"};req(history(app).restoreQualified(shape,history(app).environment())==null,"ABI change invalidates");Build.SUPPORTED_ABIS=oldAbis;
  app.version="changed";req(history(app).restoreQualified(shape,history(app).environment())==null,"app version name invalidates");app.version="1.9.56";
  app.code++;req(history(app).restoreQualified(shape,history(app).environment())==null,"app version code invalidates");app.code--;
  app.pkg="different.package";req(history(app).restoreQualified(shape,history(app).environment())==null,"app package invalidates");app.pkg="com.host.ulike";
  req(history(app).restoreQualified(shape,original).dispatchRows==0,"restoring identical complete runtime recovers original certificate");
 }
 static void coldFingerprintProof()throws Exception {
  for(int trial=0;trial<3;trial++){
   scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer t=new Timer();int[] shape=key(620+trial);qualify(app,t,shape,64);
   GpuFinish1953.value="";WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,history(app));Photo cold=new Photo(t,6001);cold.chooseDispatch=true;
   // Cold startup cost and another mode winning cannot rewrite a saved choice.
   cold.modeTimes=new long[]{2000,1500,1000};if(trial==1)cold.modeBad[1]=true;Object trace=new Object();
   req(e.run(shape,cold,trace),"unknown runtime begins with original CPU");cold.returned=true;cold.correct();req(cold.cpu==1&&cold.gpu==0,"unknown runtime never directly restores");
   cold.proof.establishEnvironment=GPU;if(trial==2)cold.proof.reliable=false;complete(e,cold,trace);
   Photo resumed=use(e,t,shape);
   if(trial==0)req(resumed.gpu==1&&resumed.cpu==0&&resumed.dispatchRows==64,"current exact stored-mode proof restores historical choice after fingerprint appears");
   else {req(resumed.cpu==1&&resumed.gpu==0,"stored mode must have fresh exact and reliable proof after unknown environment");
    if(trial==1)req(history(app).restoreQualified(shape,history(app).environment())==null,"fresh mismatch cannot leave historical dispatch eligible next boot");
    e.foregroundStarted();released(e);}
  }
 }
 static void coldMismatchInvalidatesWithoutTiming()throws Exception {
  for(int trial=0;trial<5;trial++){
   scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer t=new Timer();int[] shape=key(680+trial);
   qualify(app,t,shape,64);GpuFinish1953.value="";
   WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,history(app));Photo cold=new Photo(t,7600+trial);
   cold.chooseDispatch=true;cold.modeBad[1]=true;
   if(trial==0||trial==2)cold.cpuReliable=false;
   if(trial==3)cold.modeTimes=new long[]{0,0,0};
   if(trial==4)Arrays.fill(cold.modeBad,true);
   Object trace=new Object();req(e.run(shape,cold,trace),"unreliable cold proof keeps exact CPU photo");
   cold.returned=true;cold.correct();
   cold.proof.establishEnvironment=GPU;
   if(trial==1||trial==2||trial==4)cold.proof.reliable=false;
   complete(e,cold,trace);
   req(cold.proof.gpu==3&&cold.proof.equal==3,"fresh pixel proof finishes for all choices independently of timing");
   WholeRoute1953.History h=history(app);
   req(h.restoreQualified(shape,h.environment())==null,
    "fresh stored-mode mismatch invalidates certificate despite unreliable CPU/GPU timing, zero duration or all-mode mismatch");
   WholeRoute1953.Engine restart=new WholeRoute1953.Engine(t,h);Photo next=use(restart,t,shape);
   req(next.cpu==1&&next.gpu==0,"disproved historical mode cannot restore after next process restart");
   restart.foregroundStarted();released(restart);
  }
 }
 static void restoredFailureAndCancellation()throws Exception {
  for(int failure=0;failure<2;failure++){
   scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer t=new Timer();int[] shape=key(640+failure);qualify(app,t,shape,64);
   WholeRoute1953.History h=history(app);WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,h);
   Photo bad=new Photo(t,7001);bad.chooseDispatch=true;bad.gpuFail=failure==0;if(failure==1)bad.gpuNanos=110;
   req(e.run(shape,bad,new Object()),"restored route failure/slowdown preserves complete image");bad.returned=true;bad.correct();
   req(bad.gpu==1&&(failure==0?bad.cpu==1:bad.cpu==0),"GPU failure falls back once; slow exact output avoids CPU duplication");
   req(h.restoreQualified(shape,h.environment())==null,"rejection removes persistent admission");
   WholeRoute1953.Engine restart=new WholeRoute1953.Engine(t,history(app));Photo p=use(restart,t,shape);
   req(p.cpu==1&&p.gpu==0,"rejected record cannot reappear on process restart");restart.foregroundStarted();released(restart);
  }
  scenarios++;GpuFinish1953.value=GPU;App app=new App();Timer t=new Timer();WholeRoute1953.History h=history(app);WholeRoute1953.Engine e=new WholeRoute1953.Engine(t,h);int[] shape=key(650);
  for(int i=0;i<3;i++){
   Photo p=new Photo(t,7200+i);p.chooseDispatch=true;Object trace=new Object();req(e.run(shape,p,trace),"cancellable original CPU result");p.returned=true;
   if(i==2){p.proof.engine=e;p.proof.cancelAtEqual=true;}complete(e,p,trace);
  }
  req(h.restoreQualified(shape,h.environment())==null,"capture cancellation before final winning proof cannot persist admission");
  WholeRoute1953.Engine restart=new WholeRoute1953.Engine(t,history(app));Photo p=use(restart,t,shape);req(p.cpu==1&&p.gpu==0,"cancelled proof cannot restore after restart");restart.foregroundStarted();released(restart);
 }
 public static void main(String[] args)throws Exception {
  roundtripEveryMode();malformedAndLegacy();fingerprintChanges();coldFingerprintProof();coldMismatchInvalidatesWithoutTiming();restoredFailureAndCancellation();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions.get()+",\"scenarios\":"+scenarios+"}");
 }
}
