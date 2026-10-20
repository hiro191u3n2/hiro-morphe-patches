package com.hiro.ulike;
import java.util.Arrays;
import static com.hiro.ulike.Memory1989Test.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

/** Current foreground policy plus real unchanged JNI scratch and GLES owners. */
public final class ForegroundMemory1997Test {
    static long[] last;
    static boolean refusedThenRetry(long bytes,boolean observe) {
        check(!GpuNoise1960.workspaceFits(bytes),"controlled initial foreground refusal");
        last=observe?new long[33]:null;
        return FinishMemory1989.recheckForeground1997(bytes,last);
    }
    static void foreground(){reset();GpuQualification1961.inBackground=false;}
    static void recovered1997(){
        for(boolean observe:new boolean[]{false,true}) {
            foreground();prime(16*(int)M);pressureAfterNative(56*M);
            check(refusedThenRetry(32*M,observe),"foreground recovers idle native capacity under unchanged budget");
            check(h8.trims==1&&strong.trims==1&&single.trims==0,"one native-only trim, no Single trim");
            check(GpuQualification1961.reports==0,"foreground does not write background attempt report");
            if(observe){check(last[6]==0&&last[2]==0&&last[4]==1&&last[5]==2&&last[8]==3,"foreground failure and retry recorded separately");exactObservation(last,9);exactObservation(last,21);}
        }
        foreground();prime(8*(int)M);pressureAfterNative(24*M);
        check(!refusedThenRetry(32*M,true),"insufficient reclaimed bytes cannot admit request");
        check(last[5]==2&&last[4]==0&&h8.trims==1&&strong.trims==1,"one unsuccessful same-limit recheck only");
        for(long n:new long[]{-1,512*M+1,Long.MAX_VALUE}) {
            foreground();prime(4096);check(!refusedThenRetry(n,true),"fixed capacity limits unchanged");
            check(last[3]==6&&last[5]==1&&h8.trims==0&&strong.trims==0,"invalid request never trims");
        }
        for(int mode=0;mode<2;mode++) {
            foreground();prime(4096);WholeRoute1953.bytes=Long.MAX_VALUE;
            if(mode==0)GpuQualification1961.cancelled=true;else Thread.currentThread().interrupt();
            try{check(!refusedThenRetry(1,true),"cancelled foreground remains refused");check(last[3]==4&&last[5]==1&&h8.trims==0,"cancel does not trim or recheck");}
            finally{Thread.interrupted();}
        }
        foreground();prime(4096);WholeRoute1953.bytes=Long.MAX_VALUE;
        boolean permit=SpeedWorkers1935.enterLegacy();try {
            check(!refusedThenRetry(1,true),"CPU work prevents optional trim");
            check(last[3]==2&&last[5]==1&&h8.trims==0&&strong.trims==0,"busy CPU skips retry");
        }finally{SpeedWorkers1935.leaveLegacy(permit);}
        foreground();prime(16*(int)M);pressureAfterNative(56*M);h8.fault=1;
        check(refusedThenRetry(32*M,true),"independent successful owner can relieve partial trim failure");
        check(last[3]==7&&h8.retainedBytes()==16*M&&strong.retainedBytes()==0,"failed owner retained and failure observed");
        foreground();single.bytes=32*M;pressureAfterNative(48*M);
        check(!refusedThenRetry(32*M,true)&&single.bytes==32*M&&single.trims==0,"live Single ownership remains charged");
    }
    static void gpuAndNative1997(boolean busyNative)throws Exception {
        foreground();int bytes=16*(int)M;long a=0,b=0;
        if(busyNative){a=MemoryNative1989.h8Acquire(bytes,89);b=MemoryNative1989.strongAcquire(bytes,113);check(a!=0&&b!=0,"live native handles acquired");}
        else prime(bytes);
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"real native GL session opens");
        GpuNoise1960.Lease1971 lease=null;
        try {
            lease=session.reserveCapacity1971(new int[]{0},new long[]{16},128);check(lease!=null,"GPU buffer and Java readback debt reserved");
            check(session.upload(0,new int[]{19,23,29,31}),"live GPU sentinel uploaded");
            long held=GpuNoise1960.reservedBytes1971(),nativeBytes=GpuNoise1960.retainedBytes();
            pressureAfterNative(56*M);
            check(refusedThenRetry(32*M,true)==!busyNative,"active GL alone permits CPU trim; busy native still refuses");
            check(h8.trims==1&&strong.trims==1&&last[5]==2,"one CPU trim and exact live-owner recheck");
            check(GpuNoise1960.reservedBytes1971()==held&&GpuNoise1960.retainedBytes()==nativeBytes,"actual GL buffers and readback debt unchanged");
            check(h8.retainedBytes()==(busyNative?bytes:0)&&strong.retainedBytes()==(busyNative?bytes:0),"busy native allocations stay charged until original release");
            WholeRoute1953.bytes=0;
            check(Arrays.equals(session.readInts(0,4),new int[]{19,23,29,31}),"uploaded GPU pixels survive optional CPU trim exactly");
        }finally {
            WholeRoute1953.bytes=0;
            if(busyNative){check(MemoryNative1989.h8Release(a,bytes,89)&&MemoryNative1989.strongRelease(b,bytes,113),"busy CPU sentinel contents survive raced trim");}
            if(lease!=null)lease.close();session.close();
        }
        check(h8.retainedBytes()==0&&strong.retainedBytes()==0,"deferred frees happen at original lease release");
        check(((List<?>)field(GpuNoise1960.class,"leases1971").get(null)).isEmpty()&&GpuNoise1960.reservedBytes1971()==0,"GL owners close and release all reserved debt");
    }
    public static void main(String[] args)throws Exception {
        try {
            recovered1997();gpuAndNative1997(false);gpuAndNative1997(true);
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"foreground_same_budget_retry_verified\":true,\"active_gpu_ownership_preserved\":true,\"busy_native_deferred_release_verified\":true}");
        }finally{reset();((ExecutorService)field(GpuNoise1960.class,"OWNER").get(null)).shutdownNow();((ExecutorService)field(SpeedWorkers1935.class,"EXECUTOR").get(null)).shutdownNow();}
    }
}
