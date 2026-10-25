package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;

/** Optional transport faults are injected into a copied entry only; the
 * fallback's CPU model, GLES JNI graph, maps and session ownership are real. */
public final class ModelFallback1994Test {
 static int assertions;
 static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  int w=65,h=67;int[] half=new int[33*34];for(int i=0;i<half.length;i++)half[i]=0xff303030+((i*17)&0x1f1f1f);
  float[] e=new float[16];java.util.Arrays.fill(e,3.7f);float[] rt=new float[16+3*2*2];System.arraycopy(e,0,rt,0,16);
  StrongNoise1958.Model cpu=StrongNoise1958.preparedSnapshotCpu1961(half,w,h,4,true,e,rt);
  Method call=GpuAnalysis1961.class.getDeclaredMethod("residentStandard1994",int[].class,int.class,int.class,int.class,boolean.class,float[].class,float[].class);call.setAccessible(true);
  for(int fault=0;fault<=7;fault++){
   GpuAnalysis1961.fault1994=fault;GpuAnalysis1961.tried1994="";GpuModelDefault1990.revoked=false;GpuModelDefault1990.disabled=false;
   StrongNoise1958.Model model=null;
   try{
    model=(StrongNoise1958.Model)call.invoke(null,half,w,h,4,true,e,rt);
    check(fault!=5&&fault!=6,"cancellation propagated");
    check(model!=null&&StrongNoise1958.sameModel1961(cpu,model),"selected/fallback complete pixels fault="+fault);
    GpuAnalysis1961.Resident owner=StrongNoise1958.takeResident1961(model);
    check(owner!=null&&owner.variant1994==(fault==0?1:0),"real executed variant retained fault="+fault);StrongNoise1958.attachResident1961(model,owner);
    check(GpuAnalysis1961.tried1994.equals(fault==0?"1":"10"),"one optional attempt then standard only fault="+fault);
    check(fault==0||GpuModelDefault1990.disabled,"optional failure disabled for current process");
   }catch(InvocationTargetException wrapped){
    check((fault==5||fault==6)&&wrapped.getCause() instanceof CancellationException,"only controlled cancellation escapes");
    check(GpuAnalysis1961.tried1994.equals("1"),"cancellation performs no fallback dispatch");
   }finally{Thread.interrupted();if(model!=null)StrongNoise1958.discardResident1961(model);}
   check(!GpuNoise1960.sessionBusy(),"all exceptional and returned resident owners closed fault="+fault);
  }
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_jni\":true,\"injected_optional_faults\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
 }
}
class GpuModelDefault1990 {
 static final String SUITE="model-test";static boolean revoked,disabled;
 static int variant1994(){return 1;}static boolean admitted1994(int v){return !revoked;}
 static void unavailable1994(int v){disabled=true;}static void requestOptional1994(){}
 static boolean owns(int[] h,int w,int z,int n,float[] e,float[] r){return false;}
 static boolean accepts(int[] h,int w,int z,int n,boolean s,float[] e,float[] r){return false;}
 static boolean ready(){return true;}static String summary(){return "test";}
}
