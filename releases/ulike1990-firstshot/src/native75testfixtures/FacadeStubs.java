package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Controlled transport and CPU fixtures. No real shader/device speed claim. */
final class GpuQualification1961 {
    interface Cancellation { boolean cancelled(); }
    interface Probe { void run(Cancellation c); void close(); }
    interface RowTask1964 { boolean run(int begin,int end); }
    static final class Record {final long cpuNanos,gpuNanos;final int variant;Record(long c,long g,int v){cpuNanos=c;gpuNanos=g;variant=v;}}
    static final Map<String,Record> records=Collections.synchronizedMap(new HashMap<String,Record>());
    static final Set<String> exact=Collections.synchronizedSet(new HashSet<String>()),speed=Collections.synchronizedSet(new HashSet<String>());
    static final Map<String,String> exactCauses=Collections.synchronizedMap(new HashMap<String,String>());
    static volatile int preferredCalls,ordinaryCalls,schedules;
    static volatile boolean busyBackground,cancel,throwPreferred;
    static long lastQueueBytes;
    static void reset(){records.clear();exact.clear();exactCauses.clear();speed.clear();preferredCalls=ordinaryCalls=schedules=0;busyBackground=cancel=throwPreferred=false;lastQueueBytes=0;}
    static Record restore(String key){return records.get(key);}
    static boolean exactRejected(String key){return exact.contains(key);}
    static boolean maySchedule(String key){return !exact.contains(key);}
    static boolean background(){return busyBackground;}static boolean cancelled(){return cancel;}
    static long retainedBytes(){return 0;}
    static boolean canQueue(String key,long bytes){lastQueueBytes=bytes;return bytes>0&&!exactRejected(key);}
    static boolean schedule(String key,long bytes,Probe p){schedules++;p.close();return false;}
    static void rejectExact(String key){exact.add(key);records.remove(key);}
    static void rejectExact1971(String key,String cause){rejectExact(key);exactCauses.put(key,cause);}
    static String exactFailure1971(String key){String cause=exactCauses.get(key);return cause==null?"cached_legacy_negative":"argb_mismatch".equals(cause)?"cached_argb_negative":"confidence_mismatch".equals(cause)?"cached_confidence_negative":"policy_failure".equals(cause)?"cached_policy_negative":cause;}
    static void rejectSpeed(String key){speed.add(key);records.remove(key);}
    static void qualified(String key,long cpu,long gpu,int variant){ordinaryCalls++;if(cpu>0&&gpu>0&&gpu<=cpu-cpu/20&&!exactRejected(key))records.put(key,new Record(cpu,gpu,variant));}
    static void qualifiedStrongPreferred1970(String key,long cpu,long gpu,int variant){preferredCalls++;if(throwPreferred)throw new IllegalStateException("optional certificate storage unavailable");if(cpu>0&&gpu>0&&!exactRejected(key)){speed.remove(key);records.put(key,new Record(cpu,gpu,variant));}}
    static long parallelRows1964(final int begin,final int end,int unit,final Cancellation c,final RowTask1964 task){
        ExecutorService pool=Executors.newFixedThreadPool(4);long start=System.nanoTime();List<Future<Boolean>> jobs=new ArrayList<Future<Boolean>>();
        try{int count=(end-begin+unit-1)/unit;for(int worker=0;worker<4;worker++){final int b=begin+count*worker/4*unit,e=Math.min(end,begin+count*(worker+1)/4*unit);if(e>b)jobs.add(pool.submit(new Callable<Boolean>(){public Boolean call(){if(c.cancelled())throw new CancellationException();return task.run(b,e);}}));}for(Future<Boolean> job:jobs)if(!job.get())return 0;if(c.cancelled())throw new CancellationException();return System.nanoTime()-start;}
        catch(ExecutionException e){throw new RuntimeException(e.getCause());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}finally{pool.shutdownNow();}
    }
}
final class StrongNoise1958 {
    static final class Model {int height=32;float[] evidence;int[][] maps;}
    interface Patches {void read(int[] data,int x,int y,int width,int height);}
    static volatile long cpuDelay=1000000L;
    static long scratchHint1975=1048576L; static long workspaceBytes(int width,int rows){return scratchHint1975;}
    static final AtomicInteger oracleCalls=new AtomicInteger();
    static int pixel(int value){return value^0x00010203;}
    static float[] gpuEvidence1960(Model m){return m.evidence;}
    static int[][] gpuMaps1960(Model m){return m.maps;}
    static GpuAnalysis1961.Resident takeResident1961(Model m){return null;}static void discardResident1961(Model m){}
    static boolean gpuPrepareOracleSnapshot1961(int[] src,int[] out,int w,int h,int noise,boolean shadows,float[] ev,int mode){oracleCalls.incrementAndGet();for(int i=0;i<w*h;i++)out[i]=pixel(src[i]);return true;}
    static boolean gpuOracleSnapshot1961(int[] src,int[] out,int w,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,Model m,int[] policy,int[] confidence){oracleCalls.incrementAndGet();for(int i=begin*w;i<end*w;i++)out[i]=pixel(src[i]);if(confidence!=null)Arrays.fill(confidence,0,((w+3)/4)*((end-begin+3)/4),37);return true;}
}
final class GpuAnalysis1961 {static final class Resident {final GpuNoise1960.Session session;final long setupNanos;Resident(GpuNoise1960.Session s,long n){session=s;setupNanos=n;}void close(){session.close();}}}
final class GpuPolicy1960 {
    static final class LocalNoise {int columns=1,rows=1;}
    static final class Plan {int beautyQ8,shadowBudgetQ8;LocalNoise localNoise;}
    static final class PolicyData {int count;int[] grid=new int[0],masks=new int[0],u=new int[32];float[] f=new float[32];}
    static final class Protection {Plan plan;PolicyData descriptor;PolicyData data(int width,int rows,int y){return descriptor;}}
}
final class ProcessingTiming1947 {
    static final class Trace {final long captureId=1970,id=1971;}
    static final Trace trace=new Trace();static int backendCalls,lastGpu,lastCpu,verification;
    static final Map<String,Integer> reasons=new HashMap<String,Integer>();
    static final Map<String,Integer> trials=new HashMap<String,Integer>();
    static String firstFailure;static int firstProgram,firstLayout,firstX,firstY,firstWidth,firstRows;static long firstDelta;
    static void reset(){backendCalls=lastGpu=lastCpu=verification=0;reasons.clear();trials.clear();firstFailure=null;}
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

final class WholeRoute1953{static volatile long retained;static long retainedBytes(){return retained;}}
final class GpuFinish1953{static long retainedBytes(){return 0;}}
final class SpeedWorkers1935{static long nativeRetainedBytes1956(){return 0;}static int maxWorkers(){return 4;}}

/** Orchestration only: tuning has its separate production-helper tests. */
final class GpuStrongTuning1975 {
 static final class Choice {final int profile,variant;final String key;Choice(int p,int v,String k){profile=p;variant=v;key=k;}}
 static Choice select(int[] u){return null;}
 static void schedule(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.Protection protection){}
}
