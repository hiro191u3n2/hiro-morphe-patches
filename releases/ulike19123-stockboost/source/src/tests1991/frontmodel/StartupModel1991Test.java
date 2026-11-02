package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
import java.util.Map;

/** Controlled initialization with real startup fencing and certificate store. */
public final class StartupModel1991Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static void cache()throws Exception {
  Context context=new Context();GpuCertificates1990.initialize(context);
  check(GpuCertificates1990.passed("cache",32,1000,100,200),"real finite evidence stored");
  Object first=GpuCertificates1990.restore("cache");
  check(first!=null&&first==GpuCertificates1990.restore("cache"),"unchanged raw evidence reuses parsed scalar evidence");
  for(int i=0;i<30;i++){GpuCertificates1990.passed("suite-"+i,1,1,1,1);GpuCertificates1990.restore("suite-"+i);}
  Field f=GpuCertificates1990.class.getDeclaredField("CACHE");f.setAccessible(true);
  check(((Map<?,?>)f.get(null)).size()<=16,"bounded cache stores at most 16 suites");
  check(GpuCertificates1990.restore("cache")!=null,"evicted cache restores actual saved proof");
  GpuCertificates1990.reject("cache","observed bit difference");
  check(GpuCertificates1990.restore("cache")==null&&GpuCertificates1990.rejected("cache"),"rejection immediately invalidates cached pass");
  check(!GpuCertificates1990.passed("cache",100,100,1,1),"mismatch cannot be overwritten");
  check(GpuCertificates1990.passed("fresh",1,1,1,1),"independent corrected suite allowed");
  check(GpuCertificates1990.restore("fresh")!=null,"fresh proof loaded");
  for(String key:context.prefs.data.keySet().toArray(new String[0]))if(key.startsWith("pass-"))context.prefs.data.put(key,"corrupt");
  check(GpuCertificates1990.restore("fresh")==null,"raw corruption detected despite positive parse cache");
  String before=GpuQualification1961.environment;GpuQualification1961.environment="other-device";
  check(!GpuCertificates1990.rejected("cache")&&GpuCertificates1990.restore("cache")==null,"environment change does not leak pass or mismatch");
  GpuQualification1961.environment=before;check(GpuCertificates1990.rejected("cache"),"returning device restores sticky rejection");
 }
 static void startup(boolean hold)throws Exception {
  Context context=new Context();GpuStrongDefault1990.result=false;GpuNoise1960.holdAux=hold;
  GpuStartup1990.initialize(context);
  if(hold){
   check(!GpuStartup1990.awaitModelReady1991(20),"model dispatch cannot precede shared startup release");
   GpuNoise1960.aux.countDown();
  }
  GpuStartup1990.awaitPreparation(2000);
  check(!GpuStartup1990.domainReady(),"controlled Strong failure remains unavailable");
  check(GpuStartup1990.snapshot().modelDomainReady,"independent model exact suite completed");
  check(GpuStartup1990.awaitModelReady1991(0),"model corpus success independent from Strong failure after release");
  check(GpuStartup1990.detail().contains("model ready"),"startup detail exposes model state");
  GpuStartup1990.invalidateModelDomain();check(!GpuStartup1990.awaitModelReady1991(0),"model revocation immediate");
 }
 public static void main(String[] args)throws Exception {
  if(args[0].equals("cache"))cache();else startup(args[0].equals("busy"));
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
