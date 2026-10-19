package com.hiro.ulike;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
final class ProcessingTiming1947 {static volatile long epoch=1;static long captureEpoch1953(){return epoch;}}
final class SaveQueue1935 {static volatile boolean idle;static boolean idle1953(){return idle;}}
final class WholeRoute1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}static void run(Runnable[] a){for(Runnable r:a)r.run();}}
final class QualityPixels1932 {static class Plan {Local localNoise;}static class Local {int columns=1,rows=1;}}
final class GpuPolicy1960 {
    static final class PolicyData {final int[] masks,u;final float[] grid,f;final int count;PolicyData(int[] m,int[] u,float[] g,float[] f,int c){masks=m;this.u=u;grid=g;this.f=f;count=c;}}
    static final class Protection {QualityPixels1932.Plan plan;PolicyData original=new PolicyData(new int[]{33},new int[]{44},new float[]{55},new float[]{66},64);PolicyData data(int w,int r,int y){return original;}}
        static PolicyData detached1975(PolicyData data) {
        return data==null?null:new PolicyData(data.masks.clone(),data.u.clone(),data.grid.clone(),data.f.clone(),data.count);
    }
}
final class StrongNoise1958 {
    static AtomicInteger cpuRuns=new AtomicInteger();static volatile boolean unstable;
    static class Model {long residentBytes(){return 1024;}int height=8;}
    static float[] gpuEvidence1960(Model m){return new float[16];}static int[][] gpuMaps1960(Model m){return new int[][]{{1},{2},{3}};}
    static boolean gpuOracleSnapshot1961(int[] s,int[] d,int w,int r,int b,int e,int vb,int ve,int oy,int n,boolean shadow,Model model,int[] p,int[] cf){int turn=cpuRuns.incrementAndGet();for(int i=b*w;i<e*w;i++)d[i]=s[i]^(p==null?0:p[0]);if(unstable&&turn%2==0)d[b*w]^=1;if(cf!=null)Arrays.fill(cf,23);return true;}
}
final class GpuNoise1960 {
    static AtomicInteger sessions=new AtomicInteger(),closed=new AtomicInteger(),reads=new AtomicInteger(),uploads=new AtomicInteger(),repeats=new AtomicInteger();
    static volatile int newMode,blockProfile=-1,blockVariant=-1,policyFailureProfile=-1,policyFailureVariant=-1;static volatile boolean oldUnavailable,rejectMemory,failLease,stallOld;static AtomicIntegerArray readsByProfile=new AtomicIntegerArray(8);static volatile CountDownLatch blockStarted,blockRelease;static volatile boolean blocked,policyFailure,failRead;
    static String fingerprint(){return "tuning-host-driver-v1";}static boolean sessionBusy(){return sessions.get()!=0;}
    static boolean workspaceFits(long b){return !rejectMemory&&b<512L*1024*1024;}static boolean supports(int p){return true;}
    static class Lease1971 {boolean revalidate1971(){return true;}void close(){}}
    static class Ticket {Batch batch;Ticket(Batch b){batch=b;}}
    static class Batch {int[] source,policy,u;GpuPolicy1960.PolicyData descriptor;int profile,variant;boolean repeat;Batch(int[] s,int[] p,int[] u,GpuPolicy1960.PolicyData d,int v,int f,boolean repeat){source=s;policy=p;this.u=u;descriptor=d;variant=v;profile=f;this.repeat=repeat;}}
    static class Session {Batch resident;boolean done;Lease1971 reserveCapacity1971(int[] b,long[] sizes,long j){return new Lease1971();}boolean upload(int i,int[] v){uploads.incrementAndGet();return true;}boolean upload(int i,float[] v){uploads.incrementAndGet();return true;}
        Ticket submit(Batch b,int bank){if(b.profile==7&&newMode==6)try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}if(b.repeat){repeats.incrementAndGet();b.source=resident.source;b.policy=resident.policy;}else resident=b;return new Ticket(b);}int[] readInts(int slot,int count){int[] p=resident.policy==null?new int[]{0}:resident.policy.clone();if(newMode==4&&resident.profile==7)p[0]^=1;return p;}void close(){if(!done){done=true;sessions.decrementAndGet();closed.incrementAndGet();}}
    }
    static Session open(){sessions.incrementAndGet();return new Session();}
}
final class GpuStrong1960 {
    static final int PROFILES1978=8;
    private static String key(int[] u){
        // All branches, alpha/bounds handling and representative-cell alignment
        // belong to the key. No source pixels, mask or model evidence are stored.
        return "strong-gx1964-parallel-policy-bank-v1:"+u[10]+":"+u[0]+":"+u[1]+":"+(u[3]-u[2])+":"+
               u[4]+":"+u[5]+":"+(u[2]&7)+":"+(u[6]&63)+":"+u[7]+":"+u[8]+":"+u[9]+":"+u[11]+":"+u[12]+":"+u[18]+":"+u[19];
    }
    private static String genericKey1971(int[] u){
        return "strong-gx1971-generic-policy-bank-v1:"+key(u).substring("strong-gx1964-parallel-policy-bank-v1:".length());
    }
    static String profileKey1973(int[] u,int profile){
        if(profile<0||profile>=PROFILES1978)throw new IllegalArgumentException("strong profile");
        String suffix=key(u).substring("strong-gx1964-parallel-policy-bank-v1:".length());
        if(profile==4)return "strong-gx1978-policy-direct-ieee-v1:"+suffix;
        if(profile==5)return "strong-gx1978-policy-direct-tile8-v1:"+suffix;
        if(profile==6)return "strong-gx1978-policy-direct-pow2-v1:"+suffix;
        if(profile==7)return "strong-gx1978-policy-direct-pow2-tile8-v1:"+suffix;
        return profile==3?"strong-gx1976-ieee-tile8-policy-bank-v1:"+suffix:profile==0?"strong-gx1973-ieee-div-policy-bank-v1:"+suffix:profile==1?key(u):genericKey1971(u);
    }
    static boolean directPolicy1978(int profile){return profile>=4&&profile<PROFILES1978;}
    static int program1973(int p,int v){return p*3+v;}
    static class Result {final int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
    static class Read1971 {final Result result;final String failure;Read1971(Result r,String f){result=r;failure=f;}}
    static GpuNoise1960.Lease1971 reserveRange1971(GpuNoise1960.Session s,int[] src,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank){return GpuNoise1960.failLease?null:new GpuNoise1960.Lease1971();}
    static GpuNoise1960.Lease1971 reserveProof1978(GpuNoise1960.Session s,int[] src,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank){return reserveRange1971(s,src,p,u,d,bank);}
    static String verifyPolicy1978(GpuNoise1960.Session session,int slot,int[] policy){
        int[] actual=session.readInts(slot,policy==null?1:policy.length);
        if(actual==null)return "readback_failed";
        return policy==null?(actual.length==1&&actual[0]==0?null:"policy_failure"):Arrays.equals(actual,policy)?null:"policy_failure";
    }
    static GpuNoise1960.Batch commands1973(int[] s,int[] p,int[] u,GpuPolicy1960.PolicyData d,int[] bank,int v,int f){return new GpuNoise1960.Batch(s,p,u,d,v,f,false);}
    static GpuNoise1960.Batch commandsRepeat1975(int[] u,GpuPolicy1960.PolicyData d,int[] bank,int v,int f){return new GpuNoise1960.Batch(null,null,u,d,v,f,true);}
    static Read1971 read1971(GpuNoise1960.Session session,GpuNoise1960.Ticket t,int[] u,int[] bank){
        GpuNoise1960.reads.incrementAndGet();GpuNoise1960.Batch b=t.batch;GpuNoise1960.readsByProfile.incrementAndGet(b.profile);
        if(GpuNoise1960.blocked&&(GpuNoise1960.blockProfile<0||GpuNoise1960.blockProfile==b.profile)&&(GpuNoise1960.blockVariant<0||GpuNoise1960.blockVariant==b.variant)){GpuNoise1960.blockStarted.countDown();try{GpuNoise1960.blockRelease.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}}
        if(GpuNoise1960.failRead||GpuNoise1960.oldUnavailable&&b.profile<4)return new Read1971(null,"readback_failed");
        // A controlled pause proves that another exact legacy profile may be
        // the correct measured winner. No production clock or gate is changed.
        try{if(GpuNoise1960.stallOld&&b.profile==2)Thread.sleep(35);Thread.sleep(b.profile==7?(GpuNoise1960.newMode==0?25:GpuNoise1960.newMode==7&&GpuNoise1960.readsByProfile.get(7)>6?40:b.variant==2?1:20):b.profile==2&&b.variant==2?8:20);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}
        if(GpuNoise1960.policyFailure||(GpuNoise1960.policyFailureProfile==b.profile&&GpuNoise1960.policyFailureVariant==b.variant)||b.descriptor!=null&&(b.descriptor.masks[0]!=33||b.descriptor.u[0]!=44||b.descriptor.grid[0]!=55||b.descriptor.f[0]!=66))return new Read1971(null,"policy_failure");
        int core=u[0]*(u[3]-u[2]),offset=u[0]*u[2];int[] out=new int[core],cf=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];
        for(int i=0;i<core;i++)out[i]=b.source[offset+i]^(b.policy==null?0:b.policy[0]);if(cf!=null)Arrays.fill(cf,23);
        if(b.profile==7){if(GpuNoise1960.newMode==2||GpuNoise1960.newMode==5&&b.repeat)out[core-1]^=1;if(GpuNoise1960.newMode==3&&cf!=null)cf[cf.length-1]^=1;}if(b.profile==0){if(b.variant==1&&cf!=null)cf[0]++;else out[core-1]^=1;}
        return new Read1971(new Result(out,cf),null);
    }
}

final class GpuStrongRouting1978 {static int calls;static boolean slowerCpu;static long[] compareCpu1978(int[] s,int[] p,int[] u,StrongNoise1958.Model m,GpuPolicy1960.PolicyData d,int f,int v,int w,GpuQualification1961.Cancellation c){return slowerCpu?new long[]{100,200}:new long[]{200,100};}static void prove(int[] source,int[] policy,int[] u,StrongNoise1958.Model m,GpuPolicy1960.PolicyData d,int p,int v,GpuQualification1961.Cancellation c){calls++;}}
