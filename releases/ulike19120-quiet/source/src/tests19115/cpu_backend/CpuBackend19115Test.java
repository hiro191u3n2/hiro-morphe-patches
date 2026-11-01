package com.hiro.ulike;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Real processRange, Java fallback, and controlled JNI true/false/link returns. */
public final class CpuBackend19115Test {
 static int assertions,scenarios;static native void nativeMode(int mode);static native int nativeCalls(boolean shared);
 static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
 static final int[] INPUT=new int[64];static {for(int i=0;i<64;i++)INPUT[i]=0xff000000|((i*17)&255)<<16|((i*11)&255)<<8|((i*7)&255);}
 static StrongNoise1958.Model model(boolean active){float[] evidence=new float[48],runtime=new float[19];Arrays.fill(evidence,active?.5f:0);Arrays.fill(runtime,active?.5f:0);return StrongNoise1958.model1961(8,8,new int[16],new int[4],new int[1],evidence,runtime);}
 static void state(int value)throws Exception{Field f=StrongNoise1958.class.getDeclaredField("nativeState");f.setAccessible(true);f.setInt(null,value);}
 static void reset(int mode)throws Exception{
  scenarios++;Thread.interrupted();nativeMode(mode);state(0);GpuStrong1960.beginCalls=GpuStrong1960.endCalls=GpuStrong1960.observerFault=GpuStrong1960.routeCalls=GpuStrong1960.processCalls=0;GpuStrong1960.reported=null;GpuStrong1960.observed=true;GpuStrong1960.gpuReturn=GpuStrong1960.oracleMode=false;SpeedWorkers1935.floatFault=SpeedWorkers1935.integerBorrows=SpeedWorkers1935.floatBorrows=0;
 }
 static int[] run(StrongNoise1958.Model model,int noise,boolean shared,StrongNoise1958.Workspace workspace){
  int[] output=new int[64];Arrays.fill(output,0x12345678);
  if(shared)StrongNoise1958.processRange(INPUT,output,8,8,0,8,0,8,0,noise,true,model,null,workspace);
  else StrongNoise1958.processRange(INPUT,output,8,8,0,8,0,8,0,noise,true,model,null);
  return output;
 }
 static void report(int result,int failed,int link){check(Arrays.equals(GpuStrong1960.reported,new int[]{result,failed,link}),"observed backend classification "+result+"/"+failed+"/"+link);check(GpuStrong1960.beginCalls==1&&GpuStrong1960.endCalls==1,"one observer begin and finally report");}
 public static void main(String[] args)throws Exception{
  System.loadLibrary("ulike_smooth1958");StrongNoise1958.Model active=model(true);int[] original=INPUT.clone();
  if(args.length>0){reset(0);int[] output=run(active,4,false,null);check(Arrays.equals(output,INPUT)&&nativeCalls(false)==1,"missing observer preserves one actual native completion");check(GpuStrong1960.beginCalls==0&&GpuStrong1960.endCalls==0,"missing observer methods are ignored");System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"observer_absence_preserved\":true}");return;}
  reset(1);state(-1);GpuStrong1960.observed=false;int[] javaExpected=run(active,4,false,null);check(nativeCalls(false)==0,"independent Java reference executes with native disabled");
  for(boolean oracle:new boolean[]{false,true})for(boolean shared:new boolean[]{false,true})for(int mode=0;mode<=2;mode++){
   reset(mode);GpuStrong1960.oracleMode=oracle;StrongNoise1958.Workspace w=shared?new StrongNoise1958.Workspace(true):null;
   try{int[] output=run(active,4,shared,w);report(mode==0?1:2,mode==1?1:0,mode==2?1:0);
    check(Arrays.equals(output,mode==0?INPUT:javaExpected),"observation preserves selected backend output");check(nativeCalls(shared)==1&&nativeCalls(!shared)==0,"one real JNI native attempt on selected confidence overload");
    check(GpuStrong1960.routeCalls==1&&GpuStrong1960.processCalls==1,"observer adds no route or GPU adapter attempt");if(w!=null)check(w.cpuOwner19115==null,"workspace token cleared after completed call");
   }finally{if(w!=null)w.close();}
  }
  for(int mode=0;mode<2;mode++){
   reset(0);StrongNoise1958.Model m=mode==0?active:model(false);int[] output=run(m,mode==0?0:4,false,null);report(3,0,0);check(Arrays.equals(output,INPUT)&&nativeCalls(false)==0,"no-op completes without native work");
  }
  reset(0);GpuStrong1960.gpuReturn=true;run(active,4,false,null);report(4,0,0);check(nativeCalls(false)==0,"unclassified completion never invents native adoption");
  for(int fault:new int[]{1,2}){
   reset(1);SpeedWorkers1935.floatFault=fault;boolean failed=false;
   try{run(active,4,false,null);}catch(CancellationException expected){check(fault==2,"actual Java cancellation retained");failed=true;}catch(IllegalStateException expected){check(fault==1,"actual Java workspace failure retained");failed=true;}finally{Thread.interrupted();}
   check(failed,"Java fallback failure propagates");report(5,1,0);check(nativeCalls(false)==1,"incomplete Java fallback does not repeat native attempt");
  }
  reset(0);Thread.currentThread().interrupt();boolean canceled=false;try{run(active,4,false,null);}catch(CancellationException expected){canceled=true;}finally{Thread.interrupted();}check(canceled,"entry cancellation propagates");report(5,0,0);check(nativeCalls(false)==0,"entry cancellation never calls native");
  for(int fault:new int[]{1,2}){
   reset(0);GpuStrong1960.observerFault=fault;int[] output=run(active,4,false,null);check(Arrays.equals(output,INPUT)&&nativeCalls(false)==1,"observer exception cannot change native return/output");check(GpuStrong1960.endCalls==(fault==1?0:1),"failed begin never manufactures an owner");
  }
  reset(0);GpuStrong1960.observed=false;run(active,4,false,null);check(GpuStrong1960.endCalls==0&&nativeCalls(false)==1,"ordinary unobserved call retains native route");
  reset(0);StrongNoise1958.Workspace reused=new StrongNoise1958.Workspace(true);try{
   run(active,4,true,reused);report(1,0,0);check(reused.cpuOwner19115==null,"first reusable workspace owner cleared");
   reset(1);run(active,4,true,reused);report(2,1,0);check(reused.cpuOwner19115==null,"reused workspace owner cleared without stale counts");
  }finally{reused.close();}
  check(Arrays.equals(INPUT,original),"source pixels remain unchanged by all observer branches");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+",\"actual_native_return_executed\":true,\"controlled_jni_not_production_kernel\":true,\"native_false_and_link_fallback_observed\":true,\"actual_java_fallback_executed\":true,\"observer_failures_preserved\":true,\"physical_android_tested\":false}");
 }
}
