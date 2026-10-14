package com.hiro.ulike;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Controlled comparator boundary tests routing only, never pixels or speed.
 * Certificates and input metadata use actual production classes. */
public final class ChainRouting1999Test {
 static int checks;static boolean controlled=true;static int wideResult=1,narrowResult=1;
 static final List<Integer> rows=new ArrayList<Integer>();
 static final GpuQualification1961.Cancellation GO=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static int comparison(int tile){rows.add(tile);return tile==128?wideResult:narrowResult;}
 static void set(Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
 static void store()throws Exception {
  final Map<String,String> values=new HashMap<String,String>();final Object[] editor=new Object[1];
  editor[0]=Proxy.newProxyInstance(ChainRouting1999Test.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("putString"))values.put((String)a[0],(String)a[1]);else if(n.equals("remove"))values.remove(a[0]);else if(n.equals("clear"))values.clear();else if(n.equals("commit"))return true;else if(n.equals("apply"))return null;return editor[0];}});
  Object preferences=Proxy.newProxyInstance(ChainRouting1999Test.class.getClassLoader(),new Class[]{SharedPreferences.class},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){String n=m.getName();if(n.equals("edit"))return editor[0];if(n.equals("getString"))return values.containsKey(a[0])?values.get(a[0]):a[1];if(n.equals("contains"))return values.containsKey(a[0]);if(n.equals("getAll"))return values;return null;}});
  set(GpuCertificates1990.class,"preferences",preferences);set(GpuQualification1961.class,"base","controlled-chain1999");set(GpuNoise1960.class,"environment","controlled-comparator-no-native");
 }
 static int compare(GpuFinishDefault1990.Fixture f){return GpuChain1961.compareBenchmark1978(f.source,f.rotation,f.width,f.height,f.plan,f.refresh,f.source,GO);}
 static String key(GpuFinishDefault1990.Fixture f){return GpuChain1961.residentBaseline1978(f.source,f.rotation,f.width,f.height,f.plan,false);}
 public static void main(String[] args)throws Exception {
  store();GpuFinishDefault1990.Fixture f=GpuFinishDefault1990.fixture(0);
  try {
   check(GpuCertificates1990.passed(GpuFinishDefault1990.activeSuite1992(),1,1,1,1),"controlled narrow certificate installed");
   check(!GpuFinishDefault1990.wideReady1993(),"wide needs independent certificate");
   String narrowKey=key(f);check(narrowKey!=null&&narrowKey.endsWith(":32"),"baseline identifies deployed32");
   check(compare(f)==ResidentProof1978.EXACT&&rows.equals(Arrays.asList(32)),"baseline full-pixel comparator uses32 when32 deployed");rows.clear();
   check(GpuCertificates1990.passed(GpuFinishDefault1990.wideSuite1993(),1,1,1,1),"controlled wide certificate installed");
   String wideKey=key(f);check(wideKey!=null&&wideKey.endsWith(":128-or-memory32")&&!wideKey.equals(narrowKey),"wide selection changes resident baseline key");
   check(compare(f)==ResidentProof1978.EXACT&&rows.equals(Arrays.asList(128)),"actual compareBenchmark dispatch now matches128 baseline");rows.clear();
   wideResult=GpuChain1961.FINISH_MEMORY1988;
   check(compare(f)==ResidentProof1978.EXACT&&rows.equals(Arrays.asList(128,32)),"only explicit wide memory refusal retries complete32 compare");rows.clear();
   narrowResult=GpuChain1961.FINISH_MEMORY1988;
   check(compare(f)==ResidentProof1978.UNAVAILABLE&&rows.equals(Arrays.asList(128,32)),"both memory refusals remain unavailable");rows.clear();narrowResult=1;
   wideResult=ResidentProof1978.MISMATCH;
   check(compare(f)==ResidentProof1978.MISMATCH&&rows.equals(Arrays.asList(128)),"wide mismatch cannot be laundered through32 fallback");rows.clear();
   wideResult=ResidentProof1978.UNAVAILABLE;
   check(compare(f)==ResidentProof1978.UNAVAILABLE&&rows.equals(Arrays.asList(128)),"wide transport failure cannot be laundered through32 fallback");rows.clear();
   boolean cancelled=false;try{GpuChain1961.compareDefault1999(f.source,f.rotation,f.width,f.height,f.plan,f.refresh,f.source,new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException expected){cancelled=true;}
   check(cancelled&&rows.isEmpty(),"capture cancellation wins before any controlled dispatch");
   GpuCertificates1990.reject(GpuFinishDefault1990.wideSuite1993(),"fixture exact mismatch");wideResult=1;
   check(key(f).equals(narrowKey)&&compare(f)==ResidentProof1978.EXACT&&rows.equals(Arrays.asList(32)),"wide rejection returns distinct narrow key and32 route");
   check(GpuQualification1961.restore(wideKey)==null&&GpuQualification1961.restore(narrowKey)==null,"routing fixture fabricated no photograph certificate");
  } finally {f.source.recycle();}
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"controlled_comparison_boundary\":true,\"resident_baseline_schedule_matches\":true,\"resident_baseline_key_binds_wide_state\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
