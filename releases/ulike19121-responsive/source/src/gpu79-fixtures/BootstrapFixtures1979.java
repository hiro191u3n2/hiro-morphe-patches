package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Controlled transport and CPU fixtures. No real shader/device speed claim. */
final class StrongNoise1958 {
    static final class Model {int height=32;float[] evidence;int[][] maps;int[] frozenPixels,frozenConfidence;}
    interface Patches {void read(int[] data,int x,int y,int width,int height);}
    static volatile long cpuDelay=1000000L;
    static long scratchHint1975=1048576L; static long workspaceBytes(int width,int rows){return scratchHint1975;}
    static final AtomicInteger oracleCalls=new AtomicInteger(),cpuActive=new AtomicInteger(),cpuPeak=new AtomicInteger();static volatile boolean contendCpu;
    static int pixel(int value){return value^0x00010203;}
    static float[] gpuEvidence1960(Model m){return m.evidence;}
    static int[][] gpuMaps1960(Model m){return m.maps;}
    static GpuAnalysis1961.Resident takeResident1961(Model m){return null;}static void discardResident1961(Model m){}
    static boolean gpuPrepareOracleSnapshot1961(int[] src,int[] out,int w,int h,int noise,boolean shadows,float[] ev,int mode){oracleCalls.incrementAndGet();for(int i=0;i<w*h;i++)out[i]=pixel(src[i]);return true;}
    static boolean gpuOracleSnapshot1961(int[] src,int[] out,int w,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,Model m,int[] policy,int[] confidence){
        oracleCalls.incrementAndGet();int active=cpuActive.incrementAndGet();cpuPeak.accumulateAndGet(active,Math::max);
        try {if(contendCpu){Thread.sleep(10);Thread.sleep(cpuActive.get()>=3?180:35);}if(m.frozenPixels!=null)System.arraycopy(m.frozenPixels,0,out,begin*w,m.frozenPixels.length);else for(int i=begin*w;i<end*w;i++)out[i]=pixel(src[i]);if(confidence!=null){if(m.frozenConfidence!=null)System.arraycopy(m.frozenConfidence,0,confidence,0,m.frozenConfidence.length);else Arrays.fill(confidence,0,((w+3)/4)*((end-begin+3)/4),37);}return true;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}finally{cpuActive.decrementAndGet();}
    }
}
final class GpuAnalysis1961 {static final class Resident {final GpuNoise1960.Session session;final long setupNanos;Resident(GpuNoise1960.Session s,long n){session=s;setupNanos=n;}void close(){session.close();}}}
final class GpuPolicy1960 {
    static final class LocalNoise {int columns=1,rows=1;}
    static final class Plan {int beautyQ8,shadowBudgetQ8;LocalNoise localNoise;}
    static final class PolicyData {int count;int[] grid=new int[0],masks=new int[0],u=new int[32];float[] f=new float[32];}
    static final class Protection {Plan plan;PolicyData descriptor;int dataCalls;PolicyData data(int width,int rows,int y){dataCalls++;return descriptor;}}
}
final class ProcessingTiming1947 {
    static final class Trace {final long captureId=1970,id=1971;}
    static volatile long epoch=1;static long captureEpoch1953(){return epoch;}static final Trace trace=new Trace();static int routeMode;static int backendCalls,lastGpu,lastCpu,verification;static synchronized void strongRoute1978(Trace t,int mode){if(mode==1||routeMode==0)routeMode=mode;}
    static final Map<String,Integer> reasons=new HashMap<String,Integer>();
    static final Map<String,Integer> trials=new HashMap<String,Integer>();
    static String firstFailure;static int firstProgram,firstLayout,firstX,firstY,firstWidth,firstRows;static long firstDelta;
    static void reset(){routeMode=0;backendCalls=lastGpu=lastCpu=verification=0;reasons.clear();trials.clear();firstFailure=null;}
    static Trace current(){return trace;}
    static synchronized void noiseBackend(Trace t,int gpu,int cpu){if(t!=trace)throw new AssertionError("capture trace lost");backendCalls++;lastGpu=gpu;lastCpu=cpu;}
    static synchronized void strongGpuReason1970(Trace t,String reason,int count){if(t!=trace)throw new AssertionError("reason trace lost");Integer old=reasons.get(reason);reasons.put(reason,(old==null?0:old)+count);}
    static synchronized void strongGpuVerification1970(Trace t,int count){if(t!=trace)throw new AssertionError("verification trace lost");verification+=count;}
    static synchronized void strongGpuTrial1971(Trace t,String kind,int count){if(t!=trace)throw new AssertionError("trial trace lost");Integer old=trials.get(kind);trials.put(kind,(old==null?0:old)+count);}
    static synchronized void strongGpuWork1973(Trace t,long cpu,long gpu,long wait,int reuse){}
    static synchronized void strongGpuMismatch1973(Trace t,long argb,int argbMax,long confidence,long confidenceMax){}
    static synchronized void strongGpuFailure1971(Trace t,String kind,int program,int layout,int width,int rows,int x,int y,long delta){if(t!=trace)throw new AssertionError("failure trace lost");if(firstFailure==null){firstFailure=kind;firstProgram=program;firstLayout=layout;firstWidth=width;firstRows=rows;firstX=x;firstY=y;firstDelta=delta;}}
}
final class CameraTrace1965 {static final List<String> events=Collections.synchronizedList(new ArrayList<String>());static void event(String phase,long epoch,String fields){events.add(phase+" "+epoch+" "+fields);}static void event(String phase,Object owner,long epoch,String fields){event(phase,epoch,fields);}}

final class WholeRoute1953{static void wake(){}static volatile long retained;static long retainedBytes(){return retained;}}
final class GpuFinish1953{static long retainedBytes(){return 0;}}


/** Orchestration only: tuning has its separate production-helper tests. */
final class GpuStrongTuning1975 {
 static final class Choice {final int profile,variant;final String key;Choice(int p,int v,String k){profile=p;variant=v;key=k;}}
 static Choice selectCpu1978(int[] u,int workers){return null;}static void schedule1978(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.Protection protection,int workers){snapshots++;}static volatile Choice chosen;static int snapshots;static Choice select(int[] u){return chosen;}
 static void schedule(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.Protection protection){snapshots++;}
}

final class GpuResident1976 {static volatile boolean benchmark;static boolean benchmarking(){return benchmark;}static boolean cpuOracle(){return false;}static GpuQualification1961.Cancellation cancellation(){return null;}}
final class GpuStrongLayout1976 {static void schedule(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.Protection protection){}}

final class GpuStrongRouting1978 {static boolean accepted(int[] u,int profile,int variant,int workers){return false;}}
final class NativeSpeed1979Test {static native int dispatches();static native void delay(int ms);static native void failRead();static native void clearFault();}
