package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutorService;

/** Executed current Java plus actual native pool and GPU lease ownership.
 * Competing-owner bytes are a deliberate admission-pressure seam, not RAM
 * measurement or a simulation of the new repair/control flow. */
public final class Memory1989Test {
    static final long M=1024L*1024;
    static int assertions;
    static final Scratch h8=new Scratch(false),strong=new Scratch(true);
    static final Single single=new Single();
    static final class Scratch implements SpeedWorkers1935.ScratchMemory {
        final boolean smooth;int trims,fault,queryFault;
        Scratch(boolean value){smooth=value;}
        public long retainedBytes(){
            if(queryFault==1)throw new IllegalStateException("native owner observation");
            if(queryFault==2)throw new LinkageError("native owner observation");
            if(queryFault==3)throw new OutOfMemoryError("native owner observation");
            if(queryFault==4)throw new AssertionError("native owner observation");
            if(queryFault==5)return -19;
            return smooth?MemoryNative1989.strongBytes():MemoryNative1989.h8Bytes();
        }
        public void trim(){
            trims++;
            if(fault==1)throw new IllegalStateException("native owner trim");
            if(fault==2)throw new LinkageError("native owner trim");
            if(fault==3)throw new OutOfMemoryError("native owner trim");
            if(fault==4)throw new AssertionError("native owner trim");
            if(smooth)MemoryNative1989.strongTrim();else MemoryNative1989.h8Trim();
        }
    }
    static final class Single implements SpeedWorkers1935.ScratchMemory {
        long bytes;int trims;
        public long retainedBytes(){return bytes;}
        public void trim(){trims++;throw new AssertionError("active Single owner must not be trimmed");}
    }
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static long headroom(){Runtime r=Runtime.getRuntime();return r.maxMemory()-(r.totalMemory()-r.freeMemory());}
    static void pressureAfterNative(long desired){
        WholeRoute1953.bytes=Math.max(0,headroom()-64*M-GpuNoise1960.retainedBytes()-GpuFinish1953.bytes-
            GpuNoise1960.reservedBytes1971()-desired);
        WholeRoute1953.queries=0;WholeRoute1953.faultAt=0;
    }
    static void reset(){
        Thread.interrupted();WholeRoute1953.bytes=0;WholeRoute1953.queries=0;WholeRoute1953.faultAt=0;
        GpuFinish1953.bytes=0;GpuQualification1961.inBackground=true;GpuQualification1961.cancelled=false;
        GpuQualification1961.reports=0;GpuQualification1961.last=null;GpuQualification1961.fault=0;
        h8.trims=strong.trims=single.trims=0;h8.fault=strong.fault=0;h8.queryFault=strong.queryFault=0;single.bytes=0;
        MemoryNative1989.h8Trim();MemoryNative1989.strongTrim();GpuNoise1960.trimIdle();
        SpeedWorkers1935.installScratchMemory1956(h8);SpeedWorkers1935.installScratchMemory1959(strong);
        SpeedWorkers1935.installScratchMemory1978(single);
    }
    static void prime(int bytes){
        long a=MemoryNative1989.h8Acquire(bytes,41),b=MemoryNative1989.strongAcquire(bytes,67);
        check(a!=0&&b!=0,"actual native scratch leases allocate");
        check(MemoryNative1989.h8Release(a,bytes,41)&&MemoryNative1989.strongRelease(b,bytes,67),"native scratch sentinels survive ordinary release");
        check(h8.retainedBytes()==bytes&&strong.retainedBytes()==bytes,"both actual native idle pools retain requested capacity");
    }
    static long[] report(){long[] r=GpuQualification1961.last;check(r!=null&&r.length==33,"one fixed 33-scalar report");return r;}
    static void exactObservation(long[] r,int at){
        check(r[at]==Runtime.getRuntime().maxMemory(),"maximum heap is observed, not invented physical RAM");
        check(r[at+1]>=0&&r[at+2]==r[at]-r[at+1],"observed heap headroom preserves scalar identity");
        check(r[at+6]==r[at+7]+r[at+8]+r[at+9],"observed native owner sum counts independent owners once");
        long free=r[at+2];for(int i:new int[]{3,4,5,6})free=Math.max(0,free-r[at+i]);
        free=Math.max(0,free-64*M);free=Math.max(0,free-r[at+10]);
        check(r[at+11]==free,"observation retains exact 64 MiB reserve and all owners");
    }
    static void recovered(boolean baseline){
        reset();prime(16*(int)M);pressureAfterNative(56*M);
        boolean okay=baseline?GpuNoise1960.workspaceFits(32*M):FinishMemory1989.fits(32*M);
        check(okay,"UNUSED_NATIVE_CACHE_PREVENTS_FINISH");
        check(h8.trims==1&&strong.trims==1&&single.trims==0,"only two reclaimable native owners receive one request");
        check(h8.retainedBytes()==0&&strong.retainedBytes()==0,"actual native idle allocations are reclaimed");
        long[] r=report();check(r[1]==32*M&&r[2]==0&&r[4]==1&&r[5]==2&&r[3]==1,"one rejection then one unchanged-budget recovery");
        check(r[8]==3&&r[9+11]<r[1]&&r[21+11]>=r[1],"before and after observations independently explain this controlled recovery");
        exactObservation(r,9);exactObservation(r,21);
    }
    static void admittedAndForeground(){
        reset();prime(8*(int)M);
        check(FinishMemory1989.fits(4*M),"already-admitted background returns immediately");
        check(h8.trims==0&&strong.trims==0&&GpuQualification1961.reports==0,"admitted call has no trim or report allocation");
        check(WholeRoute1953.queries==1,"admitted call performs only original budget query");
        check(h8.retainedBytes()==8*M&&strong.retainedBytes()==8*M,"admitted idle caches remain intact");
        pressureAfterNative(24*M);GpuQualification1961.inBackground=false;
        check(!FinishMemory1989.fits(32*M),"foreground refusal remains the exact original refusal");
        check(h8.trims==0&&strong.trims==0&&GpuQualification1961.reports==0&&WholeRoute1953.queries==1,"foreground policy and query count unchanged");
    }
    static void rejectedAndLimits(){
        reset();prime(8*(int)M);pressureAfterNative(24*M);
        check(!FinishMemory1989.fits(32*M),"unrecoverable pressure stays refused");
        long[] r=report();check(r[4]==0&&r[5]==2&&h8.trims==1&&strong.trims==1,"failed relief has exactly one retry, no loop");
        check(WholeRoute1953.queries==4,"only initial/retry and two explicitly diagnostic owner observations");
        exactObservation(r,21);
        for(long requested:new long[]{-1,512*M+1,Long.MAX_VALUE}){
            reset();prime(4096);
            check(!FinishMemory1989.fits(requested),"invalid/fixed-ceiling request remains refused");
            r=report();check(r[3]==6&&r[5]==1&&h8.trims==0&&strong.trims==0,"invalid request cannot trigger reclaim or retry");
        }
        reset();single.bytes=32*M;pressureAfterNative(48*M);
        check(!FinishMemory1989.fits(32*M),"live Single ownership cannot be reclaimed to manufacture admission");
        r=report();check(single.bytes==32*M&&single.trims==0&&r[9+9]==32*M&&r[21+9]==32*M,"active Single capacity preserved before and after");
        check(GpuNoise1960.MAX_BYTES==512*M,"native capacity ceiling is still 512 MiB");
    }
    static void nativeActiveSafety(){
        reset();int bytes=16*(int)M;
        long a=MemoryNative1989.h8Acquire(bytes,89),b=MemoryNative1989.strongAcquire(bytes,113);
        check(a!=0&&b!=0,"actual same-thread native leases are active");
        try {
            pressureAfterNative(48*M);
            check(!FinishMemory1989.fits(32*M),"busy native memory stays charged through relief");
            check(h8.trims==1&&strong.trims==1&&h8.retainedBytes()==bytes&&strong.retainedBytes()==bytes,"native trim defers freeing busy allocations");
        } finally {
            check(MemoryNative1989.h8Release(a,bytes,89)&&MemoryNative1989.strongRelease(b,bytes,113),"all live native sentinel bytes survive a raced trim");
        }
        check(h8.retainedBytes()==0&&strong.retainedBytes()==0,"deferred trim occurs only at original native release");
        reset();prime(4096);WholeRoute1953.bytes=Long.MAX_VALUE;
        boolean permit=SpeedWorkers1935.enterLegacy();
        try {
            check(!FinishMemory1989.fits(32*M),"active CPU work prevents optional relief");
            long[] r=report();check(r[3]==2&&r[5]==1&&h8.trims==0&&strong.trims==0,"CPU busy predicate skips every native trim and retry");
        } finally {SpeedWorkers1935.leaveLegacy(permit);}
    }
    static void gpuActiveSafety()throws Exception {
        reset();prime(4096);
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"actual native GPU session opens");
        GpuNoise1960.Lease1971 lease=null;
        try {
            lease=session.reserveCapacity1971(new int[]{0},new long[]{16},128);check(lease!=null,"actual GPU capacity and readback debt are leased");
            check(session.upload(0,new int[]{19,23,29,31}),"actual leased native source is uploaded");
            long held=GpuNoise1960.reservedBytes1971(),nativeBytes=GpuNoise1960.retainedBytes();
            WholeRoute1953.bytes=Long.MAX_VALUE;
            check(!FinishMemory1989.fits(32*M),"active GPU session cannot trigger idle-native repair");
            long[] r=report();check(r[3]==3&&r[5]==1&&h8.trims==0&&strong.trims==0,"active GPU guard avoids trim and retry");
            check(GpuNoise1960.reservedBytes1971()==held&&GpuNoise1960.retainedBytes()==nativeBytes,"refusal preserves actual native capacity and every outstanding debt");
            WholeRoute1953.bytes=0;int[] read=session.readInts(0,4);
            check(java.util.Arrays.equals(read,new int[]{19,23,29,31}),"active GPU source survives refused repair and reads back exactly");
        } finally {WholeRoute1953.bytes=0;if(lease!=null)lease.close();session.close();}
        check(((List<?>)field(GpuNoise1960.class,"leases1971").get(null)).isEmpty()&&GpuNoise1960.reservedBytes1971()==0,"original GPU owners close and release all scalar leases");
    }
    static void cancellation(){
        for(int mode=0;mode<2;mode++) {
            reset();prime(4096);WholeRoute1953.bytes=Long.MAX_VALUE;
            if(mode==0)GpuQualification1961.cancelled=true;else Thread.currentThread().interrupt();
            try {
                check(!FinishMemory1989.fits(32*M),"cancelled background cannot reclaim or retry");
                long[] r=report();check(r[3]==4&&r[5]==1&&h8.trims==0&&strong.trims==0,"live cancellation predicate is honored");
                if(mode==1)check(Thread.currentThread().isInterrupted(),"optional relief preserves interruption status");
            } finally {Thread.interrupted();}
        }
    }
    static void optionalFailures(){
        for(int fault=1;fault<=4;fault++) {
            reset();prime(16*(int)M);pressureAfterNative(56*M);GpuQualification1961.fault=fault;
            check(FinishMemory1989.fits(32*M),"throwing optional report cannot disable actual recovery "+fault);
            check(h8.trims==1&&strong.trims==1&&h8.retainedBytes()==0&&strong.retainedBytes()==0,"report failure leaves completed native relief intact");
        }
        reset();prime(16*(int)M);pressureAfterNative(56*M);WholeRoute1953.faultAt=2;
        check(FinishMemory1989.fits(32*M),"OOM in pre-trim observation cannot affect actual admission");
        long[] r=report();check(r[8]==2&&r[4]==1,"failed observation is marked unobserved while later snapshot survives");
        for(int fault=1;fault<=4;fault++){
            reset();prime(16*(int)M);pressureAfterNative(56*M);h8.fault=fault;
            check(FinishMemory1989.fits(32*M),"one broken trim owner cannot stop other idle native relief");
            r=report();check(r[3]==7&&r[5]==2&&strong.retainedBytes()==0&&h8.retainedBytes()==16*M,"partial trim failure is reported without freeing the failed owner");
        }
        reset();check(!GpuNoise1960.snapshotMemory1989(null,0)&&!GpuNoise1960.snapshotMemory1989(new long[11],0),"null or short optional memory target is rejected safely");
        check(!GpuNoise1960.snapshotMemory1989(new long[12],-1)&&!GpuNoise1960.snapshotMemory1989(new long[12],1),"invalid snapshot offset cannot write beyond fixed scalars");
        for(int fault=1;fault<=5;fault++){
            reset();h8.queryFault=fault;long[] observation=new long[12];java.util.Arrays.fill(observation,-1);
            check(!GpuNoise1960.snapshotMemory1989(observation,0),"unknown native query invalidates only observation "+fault);
            for(long value:observation)check(value!=Long.MAX_VALUE,"unknown sentinel is never presented as actual exabytes");
            if(fault!=4){
                check(observation[7]==-1&&observation[6]==-1&&observation[11]==-1,"unknown native owner/total/available stay explicitly unmeasured");
                check(SpeedWorkers1935.nativeRetainedBytes1956()==Long.MAX_VALUE,"original admission retains its fail-closed saturation");
            }
        }
        reset();WholeRoute1953.bytes=Long.MAX_VALUE;long[] observation=new long[12];
        check(!GpuNoise1960.snapshotMemory1989(observation,0)&&observation[4]==-1&&observation[11]==-1,"other-owner unknown sentinel is also unmeasured");
    }
    static void boundedAndJavaPool()throws Exception {
        reset();int[] kept=SpeedWorkers1935.borrowInts(4096);SpeedWorkers1935.release(kept);
        long pooled=SpeedWorkers1935.retainedBytes();check(pooled>=16384,"actual Java array pool holds an idle array");
        for(int index=0;index<16;index++){
            WholeRoute1953.bytes=Long.MAX_VALUE;
            check(!FinishMemory1989.fits(1),"bounded repeated refusals remain refusals");
            long[] r=report();check(r.length==33&&r[5]==2,"each event is fixed size and at most two checks");
            for(long value:r)check(value>=-1,"all observed fields are finite long scalars or explicit unknown");
        }
        check(SpeedWorkers1935.retainedBytes()==pooled,"native-only relief never churns the Java pool");
        WholeRoute1953.bytes=0;check(SpeedWorkers1935.borrowInts(4096)==kept,"the same idle Java capacity is still reusable");
        for(Field f:FinishMemory1989.class.getDeclaredFields())check(f.getType().isPrimitive(),"helper cannot retain reports/images/context globally");
    }
    public static void main(String[] args)throws Exception {
        try {
            if(args.length>0&&args[0].equals("published88")){recovered(true);return;}
            recovered(false);admittedAndForeground();rejectedAndLimits();nativeActiveSafety();gpuActiveSafety();cancellation();optionalFailures();boundedAndJavaPool();
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_memory_java_executed\":true,\"actual_native_pools_executed\":true,\"actual_gpu_lease_executed\":true,\"physical_android_tested\":false}");
        } finally {
            reset();((ExecutorService)field(GpuNoise1960.class,"OWNER").get(null)).shutdownNow();
            ((ExecutorService)field(SpeedWorkers1935.class,"EXECUTOR").get(null)).shutdownNow();
        }
    }
}
