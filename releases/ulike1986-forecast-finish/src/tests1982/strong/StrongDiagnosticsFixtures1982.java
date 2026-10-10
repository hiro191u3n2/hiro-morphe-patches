package com.hiro.ulike;

import java.util.*;
import java.util.concurrent.*;

/** Explicit deterministic transport holders. The production Strong dispatcher
 * and certificate service run unchanged. Synthetic XOR pixels test ownership
 * and route equality only; they do not certify a shader or Android performance. */
final class GpuNoise1960 {
    static final int STRONG=0,ANALYSIS=2,COMPARE1961=7;
    static final long MAX_BYTES=512L*1024*1024;
    static volatile boolean available=true,supported=true,busy;
    static int warms,fingerprints,opens,closes,submits,collects,uploads,leases,active;
    static void reset(){available=supported=true;busy=false;warms=fingerprints=opens=closes=submits=collects=uploads=leases=active=0;}
    static boolean warmEnvironment1973(){warms++;return available;}
    static String fingerprint(){fingerprints++;return "explicit-strong-diagnostic-transport1982";}
    static boolean supports(int program){return available&&supported;}
    static boolean available(){return available;}
    static boolean workspaceFits(long bytes){return bytes>=0&&bytes<=MAX_BYTES;}
    static boolean sessionBusy(){return busy||active!=0;}
    static int strongProgram(int mode,int variant){return 27+mode+variant*4;}
    static int variant(int program,int variant){return program+variant*9;}
    static Session open(){if(!available||busy)return null;opens++;active++;return new Session();}
    static final class Lease1971 {
        boolean closed;Lease1971(){leases++;}
        boolean revalidate1971(){if(closed)throw new AssertionError("closed lease used");return true;}
        boolean consumeJava1974(long bytes){return bytes>=0&&!closed;}
        void close(){if(closed)throw new AssertionError("lease closed twice");closed=true;leases--;}
    }
    static final class Batch {
        int[] source,u;int program;
        Batch upload(int slot,int[] values){if(slot==0||slot==14)source=values.clone();return this;}
        Batch upload(int slot,float[] values){return this;}
        Batch allocate(int slot,long bytes){return this;}
        Batch dispatch(int value,int[] bindings,int[] uniforms,float[] floats,int count){
            if(value!=ANALYSIS&&value!=COMPARE1961){program=value;u=uniforms.clone();}return this;
        }
    }
    static final class Ticket {final Batch batch;final int bank;Ticket(Batch b,int index){batch=b;bank=index;}}
    static final class Session {
        boolean closed;final Ticket[] pending=new Ticket[2];
        int failureCode1971(){return 0;}
        Lease1971 reserveCapacity1971(int[] slots,long[] targets,long javaBytes){
            if(closed||slots.length!=targets.length||javaBytes<0)throw new AssertionError("invalid lease");return new Lease1971();
        }
        boolean upload(int slot,int[] values){uploads++;return true;}
        boolean upload(int slot,float[] values){uploads++;return true;}
        boolean allocate(int slot,long bytes){return bytes>=0;}
        int[] readInts(int slot,int count){return new int[count];}
        Ticket submit(Batch batch,int bank){
            if(closed||bank<0||bank>1||pending[bank]!=null||leases==0)throw new AssertionError("invalid bank ownership");
            submits++;Ticket t=new Ticket(batch,bank);pending[bank]=t;return t;
        }
        int[][] execute(Batch batch,int[] slots,int[] counts){return values(batch,counts);}
        int[][] collect(Ticket ticket,int[] slots,int[] counts){
            if(pending[ticket.bank]!=ticket)throw new AssertionError("ticket owner lost");
            pending[ticket.bank]=null;collects++;return values(ticket.batch,counts);
        }
        boolean collectManyInto(Ticket ticket,int[] slots,int[] counts,int[][] targets,int[] offsets){
            int[][] values=collect(ticket,slots,counts);
            for(int i=0;i<targets.length;i++)System.arraycopy(values[i],0,targets[i],offsets[i],counts[i]);return true;
        }
        private int[][] values(Batch batch,int[] counts){
            if(batch.source==null||batch.u==null)throw new AssertionError("missing strip upload or command");
            int[][] values=new int[counts.length][];
            for(int i=0;i<counts.length;i++)values[i]=new int[counts[i]];
            for(int i=0;i<counts[0];i++)values[0][i]=StrongNoise1958.pixel(batch.source[batch.u[2]*batch.u[0]+i]);
            if(counts.length==3)Arrays.fill(values[1],37);return values;
        }
        boolean copy1976(int source,int destination,int sourceOffset,int destinationOffset,int count){throw new AssertionError("resident pixels outside diagnostic holder");}
        boolean uploadRange1976(int slot,int destination,int[] source,int offset,int count){throw new AssertionError("resident pixels outside diagnostic holder");}
        void close(){
            if(closed||leases!=0||pending[0]!=null||pending[1]!=null)throw new AssertionError("session closed with owned work");
            closed=true;closes++;active--;
        }
    }
}
final class StrongNoise1958 {
    static final class Model {int height=64;}
    static int pixel(int value){return value^0x00010203;}
    static float[] gpuEvidence1960(Model model){return new float[16];}
    static int[][] gpuMaps1960(Model model){return new int[][]{new int[16],new int[4],new int[1]};}
    static GpuAnalysis1961.Resident takeResident1961(Model model){return null;}
    static void discardResident1961(Model model){}
    static boolean gpuPrepareOracleSnapshot1961(int[] s,int[] d,int w,int h,int n,boolean shadows,float[] e,int mode){throw new AssertionError("background pixels are not part of this transport test");}
    static boolean gpuOracleSnapshot1961(int[] s,int[] d,int w,int rows,int begin,int end,int vb,int ve,int origin,int n,boolean shadows,Model m,int[] policy,int[] confidence){throw new AssertionError("background pixels are not part of this transport test");}
}
final class GpuAnalysis1961 {
    static final class Resident {final GpuNoise1960.Session session;final long setupNanos=0;Resident(GpuNoise1960.Session s){session=s;}void close(){session.close();}}
}
final class GpuPolicy1960 {
    static final class LocalNoise {int columns=1,rows=1;}
    static final class Plan {int beautyQ8,shadowBudgetQ8;LocalNoise localNoise;}
    static final class PolicyData {int count;int[] grid=new int[0],masks=new int[0],u=new int[32];float[] f=new float[32];}
    static final class Protection {Plan plan;PolicyData data(int width,int rows,int origin){throw new AssertionError("policy computation is outside diagnostics");}}
}
final class GpuResident1976 {
    static boolean benchmarking(){return false;}static boolean cpuOracle(){return false;}
    static GpuQualification1961.Cancellation cancellation(){return null;}
}
final class GpuChain1961 {
    static boolean admitResident1981(GpuNoise1960.Session session){return false;}
    static long residentDeadline1981(){return 0;}
}
final class GpuStrongTuning1975 {
    static final class Choice {final int profile,variant;Choice(int p,int v){profile=p;variant=v;}}
    static Choice tuned,parallel;static int schedules;
    static Choice select(int[] uniforms){return tuned;}
    static Choice selectCpu1978(int[] uniforms,int workers){return parallel;}
    static void schedule1978(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.Protection p,int workers){schedules++;}
    static void reset(){tuned=parallel=null;schedules=0;}
}
final class GpuStrongRouting1978 {
    static boolean mixed;
    static boolean accepted(int[] u,int profile,int variant,int workers){return mixed;}
}
final class GpuStrongLayout1976 {static void schedule(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.Protection d){}}
final class ProcessingTiming1947 {
    static final class Trace {final long id=1982,captureId=1982;}
    static final Trace trace=new Trace();static volatile long epoch=1;
    static final Map<String,Integer> reasons=new LinkedHashMap<String,Integer>();
    static int gpu,cpu,verification,routeMode,workCalls,lockCalls;
    static long cpuWork,gpuWork,bankWork,lockWork;
    static long captureEpoch1953(){return epoch;}
    static Trace current(){return trace;}
    static void reset(){reasons.clear();gpu=cpu=verification=routeMode=workCalls=lockCalls=0;cpuWork=gpuWork=bankWork=lockWork=0;}
    static void trace(Trace t){if(t!=trace)throw new AssertionError("trace owner changed");}
    static synchronized void noiseBackend(Trace t,int g,int c){trace(t);gpu+=g;cpu+=c;}
    static synchronized void strongGpuReason1970(Trace t,String value,int count){trace(t);Integer old=reasons.get(value);reasons.put(value,(old==null?0:old)+count);}
    static synchronized void strongGpuVerification1970(Trace t,int count){trace(t);verification+=count;}
    static synchronized void strongGpuWork1973(Trace t,long c,long g,long w,int reuse){trace(t);workCalls++;cpuWork+=c;gpuWork+=g;bankWork+=w;}
    static synchronized void strongGpuLockWait1982(Trace t,long nanos){trace(t);if(nanos<0)throw new AssertionError("negative lock wait");lockCalls++;lockWork+=nanos;}
    static synchronized void strongRoute1978(Trace t,int mode){trace(t);if(mode==1||routeMode==0)routeMode=mode;}
    static void strongGpuTrial1971(Trace t,String kind,int count){trace(t);}
    static void strongGpuMismatch1973(Trace t,long a,int am,long c,long cm){trace(t);}
    static void strongGpuFailure1971(Trace t,String kind,int program,int layout,int width,int rows,int x,int y,long delta){trace(t);}
}
final class CameraTrace1965 {
    static final List<String> events=new ArrayList<String>();
    static void event(String phase,long epoch,String fields){events.add(phase+" "+fields);}
}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static volatile long retained;static long retainedBytes(){return retained;}}
final class SpeedWorkers1935 {
    static int maxWorkers(){return 4;}static int availableWorkers1944(){return 4;}
    static void run(Runnable[] tasks){
        Thread[] threads=new Thread[tasks.length];for(int i=0;i<threads.length;i++){threads[i]=new Thread(tasks[i]);threads[i].start();}
        for(Thread thread:threads)try{thread.join();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException();}
    }
}
