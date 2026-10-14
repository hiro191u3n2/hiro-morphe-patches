package com.hiro.ulike;
import java.util.*;
import java.util.concurrent.*;
/** Controlled CPU and transport fixtures; no Android/device speed claim. */
final class GpuQualification1961 {
    interface Cancellation { boolean cancelled(); }
    interface Probe { void run(Cancellation c); void close(); }
    interface RowTask1964 { boolean run(int begin,int end); }
    static final class Record { final long cpuNanos,gpuNanos;final int variant;Record(long c,long g,int v){cpuNanos=c;gpuNanos=g;variant=v;} }
    static final Map<String,Record> records=new HashMap<String,Record>();
    static final Set<String> exact=new HashSet<String>(),speed=new HashSet<String>();
    static long lastQueueBytes;
    static void reset(){records.clear();exact.clear();speed.clear();lastQueueBytes=0;}
    static Record restore(String key){return records.get(key);}
    static boolean exactRejected(String key){return exact.contains(key);}
    static boolean maySchedule(String key){return !exact.contains(key);}
    static boolean background(){return false;}static boolean cancelled(){return false;}
    static long retainedBytes(){return 0;}
    static boolean canQueue(String key,long bytes){lastQueueBytes=bytes;return bytes>0&&!exactRejected(key);}
    static boolean schedule(String key,long bytes,Probe p){p.close();return false;}
    static void rejectExact(String key){exact.add(key);records.remove(key);}
    static void rejectSpeed(String key){speed.add(key);records.remove(key);}
    static void qualified(String key,long cpu,long gpu,int variant){if(cpu>0&&gpu>0&&gpu<=cpu-cpu/20)records.put(key,new Record(cpu,gpu,variant));}
    static long parallelRows1964(final int begin,final int end,int unit,final Cancellation c,final RowTask1964 task){
        ExecutorService pool=Executors.newFixedThreadPool(4);long start=System.nanoTime();
        List<Future<Boolean>> jobs=new ArrayList<Future<Boolean>>();
        try{
            int count=(end-begin+unit-1)/unit;
            for(int worker=0;worker<4;worker++){final int b=begin+count*worker/4*unit,e=Math.min(end,begin+count*(worker+1)/4*unit);
                if(e>b)jobs.add(pool.submit(new Callable<Boolean>(){public Boolean call(){if(c.cancelled())throw new CancellationException();return task.run(b,e);}}));}
            for(Future<Boolean> job:jobs)if(!job.get())return 0;
            if(c.cancelled())throw new CancellationException();return System.nanoTime()-start;
        }catch(ExecutionException e){throw new RuntimeException(e.getCause());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}
        finally{pool.shutdownNow();}
    }
}
final class GpuNoise1960 {
    static final int STRONG=0,ANALYSIS=2,COMPARE1961=7;
    static final long MAX_BYTES=512L*1024*1024;
    static int opens,closes,modelUploads,executes;
    static long uploadDelay,closeDelay;
    static boolean badSecondPixel,badConfidence,failSecondExecute,failModelUpload;
    static void reset(){opens=closes=modelUploads=executes=0;uploadDelay=closeDelay=0;badSecondPixel=badConfidence=failSecondExecute=failModelUpload=false;}
    static void pause(long nanos){long end=System.nanoTime()+nanos;while(System.nanoTime()<end)java.util.concurrent.locks.LockSupport.parkNanos(Math.min(1000000L,end-System.nanoTime()));}
    static boolean supports(int shader){return true;}static boolean available(){return true;}static boolean workspaceFits(long bytes){return true;}
    static boolean sessionBusy(){return false;}static String fingerprint(){return "proof-fixture";}
    static int strongProgram(int mode,int choice){return STRONG;}
    static Session open(){opens++;return new Session();}
    static final class Batch {int[] u;Batch upload(int slot,int[] x){return this;}Batch allocate(int slot,long bytes){return this;}Batch dispatch(int shader,int[] bindings,int[] uniforms,float[] f,int count){if(shader==STRONG)u=uniforms.clone();return this;}}
    static final class Ticket {final Batch b;Ticket(Batch b){this.b=b;}}
    static final class Session {
        int runs;
        boolean upload(int slot,int[] a){modelUploads++;pause(uploadDelay);return !failModelUpload;}
        boolean upload(int slot,float[] a){modelUploads++;pause(uploadDelay);return !failModelUpload;}
        Ticket submit(Batch b,int bank){return new Ticket(b);}
        int[][] collect(Ticket t,int[] slots,int[] counts){return execute(t.b,slots,counts);}
        int[][] execute(Batch b,int[] slots,int[] counts){
            runs++;executes++;if(failSecondExecute&&runs==2)return null;
            int[][] out=new int[counts.length][];out[0]=new int[counts[0]];Arrays.fill(out[0],0xffaabbcc);
            if(badSecondPixel&&runs==2)out[0][counts[0]-1]^=1;
            if(counts.length==3){out[1]=new int[counts[1]];Arrays.fill(out[1],37);if(badConfidence)out[1][out[1].length-1]++;out[2]=new int[]{0};}else out[1]=new int[]{0};
            return out;
        }
        void close(){pause(closeDelay);closes++;}
    }
}
final class StrongNoise1958 {
    static final class Model {int height=32;}
    interface Patches {void read(int[] data,int x,int y,int width,int height);}
    static final long CPU_DELAY=50000000L;
    static float[] gpuEvidence1960(Model m){return new float[16];}
    static int[][] gpuMaps1960(Model m){return new int[][]{new int[1000],new int[250],new int[63]};}
    static GpuAnalysis1961.Resident takeResident1961(Model m){return null;}static void discardResident1961(Model m){}
    static boolean gpuPrepareOracleSnapshot1961(int[] src,int[] out,int w,int h,int noise,boolean shadows,float[] ev,int mode){GpuNoise1960.pause(CPU_DELAY);Arrays.fill(out,0xffaabbcc);return true;}
    static boolean gpuOracleSnapshot1961(int[] src,int[] out,int w,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,Model m,int[] policy,int[] confidence){GpuNoise1960.pause(CPU_DELAY);Arrays.fill(out,begin*w,end*w,0xffaabbcc);if(confidence!=null)Arrays.fill(confidence,37);return true;}
}
final class GpuAnalysis1961 {
    static final class Resident {final GpuNoise1960.Session session;final long setupNanos;Resident(GpuNoise1960.Session s,long n){session=s;setupNanos=n;}void close(){session.close();}}
}
final class GpuPolicy1960 {
    static final class LocalNoise {int columns=1,rows=1;}
    static final class Plan {int beautyQ8,shadowBudgetQ8;LocalNoise localNoise;}
    static final class PolicyData {int count;int[] grid=new int[0],masks=new int[0],u=new int[32];float[] f=new float[32];}
    static final class Protection {Plan plan;PolicyData data(int width,int rows,int y){return new PolicyData();}}
}
final class ProcessingTiming1947 {
    static final class Trace {}
    static final Trace trace=new Trace();static int backendCalls,lastGpu,lastCpu;
    static Trace current(){return trace;}
    static void noiseBackend(Trace t,int gpu,int cpu){if(t!=trace)throw new AssertionError("capture trace lost");backendCalls++;lastGpu=gpu;lastCpu=cpu;}
}

