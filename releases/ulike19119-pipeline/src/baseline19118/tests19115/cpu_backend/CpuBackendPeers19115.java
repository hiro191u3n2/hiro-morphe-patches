package com.hiro.ulike;
/** Actual StrongNoise is compiled against controlled non-numerical peers. */
final class SpeedWorkers1935 {
 static int floatFault,integerBorrows,floatBorrows;
 interface ScratchMemory {long retainedBytes();void trim();}
 static int maxWorkers(){return 1;}static int availableWorkers1944(){return 1;}static long nativeRetainedBytes1956(){return 0;}
 static void installScratchMemory1959(ScratchMemory memory){}
 static int[] borrowInts(int count){integerBorrows++;return new int[count];}
 static float[] borrowFloats(int count){floatBorrows++;if(floatFault==1)throw new IllegalStateException("controlled Java workspace failure");if(floatFault==2)Thread.currentThread().interrupt();return new float[count];}
 static void release(Object value){}static void run(Runnable[] tasks){for(Runnable r:tasks)r.run();}
}
final class GpuStrong1960 {
 interface Oracle {boolean run(int[] destination,int[] confidence);}
 static final class Route1981 {boolean rawPolicy(){return false;}}
 static final Object TOKEN=new Object();static int beginCalls,endCalls,observerFault,routeCalls,processCalls;static int[] reported;
 static boolean observed=true,gpuReturn,oracleMode;
 public static Object beginCpuStrip19115(StrongNoise1958.Model model){beginCalls++;if(observerFault==1)throw new AssertionError("optional begin observer failure");return observed?TOKEN:null;}
 public static void endCpuStrip19115(Object owner,StrongNoise1958.Model model,int result,int nativeFalse,int linkFailure){endCalls++;if(owner!=TOKEN)throw new AssertionError("wrong controlled observer token");reported=new int[]{result,nativeFalse,linkFailure};if(observerFault==2)throw new AssertionError("optional end observer failure");}
 static Route1981 route1981(int w,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,StrongNoise1958.Model model,boolean policy,boolean confidence,GpuPolicy1960.Protection protection){routeCalls++;return null;}
 static boolean process1981(int[] input,int[] output,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,StrongNoise1958.Model model,int[] policy,int[] confidence,GpuPolicy1960.Protection protection,Route1981 route,Oracle oracle){processCalls++;return oracleMode?oracle.run(output,confidence):gpuReturn;}
}
final class GpuPolicy1960 {
 static class Protection implements StrongNoise1958.Protection {
  public int budgetQ8(int x,int y){return 256;}public int detailQ8(int x,int y){return 0;}
  Protection freezePolicy1981(int width,int rows,int origin,int[] policy,boolean raw){return this;}
 }
}
final class GpuAnalysis1961 {
 static final class Resident {void close(){}}
 interface RegionsCpu {float[] compute(StrongNoise1958.Patches source);}
 static boolean regionsQualified1996(int width,int height){return false;}
 static StrongNoise1958.Model residentIfQualified1996(int[] half,int w,int h,int n,boolean shadows,float[] e,float[] runtime,int[] selected,Runnable overlap){return null;}
 static void scheduleResident(int[] half,int w,int h,int n,boolean shadows,float[] e,float[] runtime,StrongNoise1958.Model m,long nanos){}
 static float[] regions1982(StrongNoise1958.Patches source,int width,int height,float[] evidence,RegionsCpu cpu,int[] selected){return cpu.compute(source);}
}
final class CpuExact1978 {static boolean background(){return false;}}
final class CpuModel1978 {
 static void evidence(int[] source,int width,int height,float[] evidence,int offset){throw new AssertionError("model preparation outside backend branch proof");}
 static float[] regions(StrongNoise1958.Patches source,int width,int height,float[] evidence){throw new AssertionError("region preparation outside backend branch proof");}
}
final class ProcessingTiming1947 {static final class Trace {}}
final class PipelineDetail1988 {
 static final int CPU=0,GPU=1,CPU_FIXED=2,MODEL_MAPS=3,MODEL_REGIONS=4,MODEL_SOURCE=5,UNKNOWN=6,UNKNOWN_BACKEND=7;
 static ProcessingTiming1947.Trace current(){return null;}
 static void record(ProcessingTiming1947.Trace t,int stage,int backend,int reason,int count,long nanos){}
}
