package com.hiro.ulike;
import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.security.MessageDigest;

/** Actual production dispatcher, corpus, CPU oracle, JNI and shaders. The
 * in-memory Android preference adapter stores only measured test evidence. */
public final class StrongAdaptive1994Test {
 static int assertions;static long pixels;
 static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void certificateStore()throws Exception{
  final HashMap<String,String> values=new HashMap<String,String>();final Object[] editor=new Object[1];
  editor[0]=Proxy.newProxyInstance(StrongAdaptive1994Test.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("putString"))values.put((String)a[0],(String)a[1]);else if(n.equals("remove"))values.remove(a[0]);else if(n.equals("clear"))values.clear();else if(n.equals("commit"))return true;else if(n.equals("apply"))return null;return editor[0];}});
  Object prefs=Proxy.newProxyInstance(StrongAdaptive1994Test.class.getClassLoader(),new Class[]{SharedPreferences.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("edit"))return editor[0];if(n.equals("getString"))return values.containsKey(a[0])?values.get(a[0]):a[1];if(n.equals("contains"))return values.containsKey(a[0]);if(n.equals("getAll"))return values;return null;}});
  Field f=GpuCertificates1990.class.getDeclaredField("preferences");f.setAccessible(true);f.set(null,prefs);f=GpuQualification1961.class.getDeclaredField("base");f.setAccessible(true);f.set(null,"actual-host-adaptive1994");
 }
 static void oracleDigest(boolean nativeCpu)throws Exception{
  check(StrongNoise1958.nativeAvailable()==nativeCpu,"requested CPU backend actually available");
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  for(int id=64;id<66;id++){
   GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.Adaptive1994.benchmark(id);check(GpuStrongDefault1990.owns(f.u),"full256-row benchmark production geometry");
   check(f.u[3]-f.u[2]==256&&f.u[0]==(id==64?3060:4080),"representative benchmark dimensions");
   int[] expected=new int[f.source.length],confidence=new int[((f.u[0]+3)/4)*64];
   check(StrongNoise1958.gpuOracleSnapshot1961(f.source,expected,f.u[0],f.u[1],f.u[2],f.u[3],f.u[4],f.u[5],f.u[6],f.u[8],f.u[9]!=0,f.model,f.policy,confidence),"benchmark CPU oracle");
   if(nativeCpu)StartupOracle1990.verifyNative(f,expected,confidence);
   for(int[] values:new int[][]{expected,confidence})for(int v:values){digest.update((byte)v);digest.update((byte)(v>>>8));digest.update((byte)(v>>>16));digest.update((byte)(v>>>24));}
   pixels+=(long)f.u[0]*256;
  }
  StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format("%02x",b&255));
  System.out.println("ORACLE "+hex+" pixels="+pixels);
 }
 public static void main(String[] args)throws Exception{
  if(args.length>0){oracleDigest(args[0].equals("native"));return;}
  certificateStore();check(GpuStrongDefault1990.profile1994()==4,"no unmeasured candidate adoption");
  check(GpuStrongDefault1990.qualify(GO),GpuStrongDefault1990.summary());
  check(GpuStrongDefault1990.ready()&&GpuStrongDefault1990.profile1994()==4,"standard readiness independent of optional candidate");
  boolean cancelled=false;try{GpuStrongDefault1990.Adaptive1994.qualify(new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){cancelled=true;}
  check(cancelled&&GpuStrongDefault1990.ready()&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)==null,"cancelled candidate neither revokes base nor creates proof");
  check(GpuStrongDefault1990.Adaptive1994.measuredWin(100,90)&&!GpuStrongDefault1990.Adaptive1994.measuredWin(100,91)&&!GpuStrongDefault1990.Adaptive1994.measuredWin(0,1)&&!GpuStrongDefault1990.Adaptive1994.measuredWin(100,0),"inclusive10percent positive timing gate");
  boolean faster=GpuStrongDefault1990.Adaptive1994.qualify(GO);
  GpuCertificates1990.Evidence exact=GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT),speed=GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST);
  check(exact!=null&&exact.cases==70&&exact.pixels>1800000&&exact.cpuNanos>0&&exact.gpuNanos>0,"actual70-case CPU/profile7 exact corpus persisted");pixels=exact.pixels;
  check(faster==(speed!=null)&&GpuStrongDefault1990.profile1994()==(faster?7:4),"selection follows measured host speed condition without asserting device speed");
  check(GpuStrongDefault1990.ready()&&!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"optional proof drains ownership and preserves standard");
  GpuCertificates1990.reject(GpuStrongDefault1990.Adaptive1994.FAST,"controlled speed eligibility revocation after actual proof");
  check(GpuStrongDefault1990.ready()&&GpuStrongDefault1990.profile1994()==4&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null,"quality proof alone never enables optional layout");
  GpuCertificates1990.reject(GpuStrongDefault1990.Adaptive1994.EXACT,"controlled candidate revocation after actual proof");
  check(GpuStrongDefault1990.ready()&&GpuStrongDefault1990.profile1994()==4&&!GpuStrongDefault1990.Adaptive1994.qualify(GO),"candidate rejection keeps independently proven profile4");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":true,\"optional_cases\":70,\"compared_pixels\":"+pixels+",\"host_speed_condition\":"+faster+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
