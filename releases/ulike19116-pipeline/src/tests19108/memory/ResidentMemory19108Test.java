package com.hiro.ulike;

import static com.hiro.ulike.Memory1989Test.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;

/** Production admission, actual JNI pools and real GLES slot24/reservation
 * ownership under controlled competing-owner pressure. No device speed claim. */
public final class ResidentMemory19108Test {
    static final int[] SENTINEL={0xff123456,0x12345678,0x76543210,0xffabcdef};
    static final class Owned implements AutoCloseable {
        final GpuNoise1960.Session session;
        final GpuNoise1960.Lease1971 reservation;
        final long bytes,debt;
        Owned() {
            session=GpuNoise1960.open();check(session!=null,"actual carried GLES owner opens");
            check(session.upload(0,new int[]{91,93,97,99}),"obsolete Strong slot allocated");
            check(session.upload(24,SENTINEL),"actual carried slot24 uploaded");
            int[] retired=new int[24];for(int i=0;i<24;i++)retired[i]=i;
            check(session.releaseSlots1991(retired),"actual post-join retirement preserves slot24");
            reservation=session.reserveCapacity1971(new int[]{24},new long[]{16},12*M);
            check(reservation!=null,"outstanding readback debt remains independently charged");
            bytes=GpuNoise1960.retainedBytes();debt=GpuNoise1960.reservedBytes1971();
            check(bytes>0&&debt>=12*M,"actual retained storage and readback reservation observed");
        }
        void intact() {
            check(GpuNoise1960.sessionBusy(),"own live GPU session remains active");
            check(GpuNoise1960.retainedBytes()==bytes&&GpuNoise1960.reservedBytes1971()==debt,"capacity and every reservation byte unchanged");
            WholeRoute1953.bytes=0;
            check(Arrays.equals(session.readInts(24,4),SENTINEL),"actual carried GPU pixels preserved bit for bit");
        }
        public void close(){WholeRoute1953.bytes=0;reservation.close();session.close();}
    }
    static void start(){reset();GpuQualification1961.residentReports=0;GpuQualification1961.residentLast=null;}
    static long[] residentReport(){long[] r=GpuQualification1961.residentLast;check(r!=null&&r.length==33,"fixed resident scalar record emitted");check(r[0]==1&&r[6]==1,"resident schema1 and background flag preserved");return r;}
    static void recoveredAndGuard()throws Exception {
        start();prime(16*(int)M);
        try(Owned own=new Owned()) {
            pressureAfterNative(56*M);
            check(!FinishMemory1989.fits(32*M),"old generic background route refuses own active GPU");
            check(report()[3]==3&&h8.trims==0&&strong.trims==0,"generic background GPU guard remains unchanged");
            check(FinishMemory1989.fitsResident19108(32*M),"scoped carried route recovers idle native memory under same budget");
            long[] r=residentReport();
            check(r[1]==32*M&&r[2]==0&&r[3]==1&&r[4]==1&&r[5]==2&&r[7]>=0,"one initial refusal, one CPU-only trim, one retry");
            check(r[8]==3&&r[9+11]<32*M&&r[21+11]>=32*M,"controlled before/after pressure explains recovery");
            exactObservation(r,9);exactObservation(r,21);
            check(h8.trims==1&&strong.trims==1&&single.trims==0,"each reclaimable CPU owner requested once; Single untouched");
            check(h8.retainedBytes()==0&&strong.retainedBytes()==0,"real native idle caches reclaimed");own.intact();
        }
        check(((List<?>)field(GpuNoise1960.class,"leases1971").get(null)).isEmpty()&&GpuNoise1960.reservedBytes1971()==0,"original final owners release all GPU reservations");
    }
    static void busyCpu() {
        start();prime(16*(int)M);
        try(Owned own=new Owned()) {
            pressureAfterNative(56*M);boolean permit=SpeedWorkers1935.enterLegacy();
            try {
                check(!FinishMemory1989.fitsResident19108(32*M),"CPU worker permit prevents resident relief");
                long[] r=residentReport();check(r[3]==2&&r[5]==1&&h8.trims==0&&strong.trims==0,"busy CPU does not trim or retry");
                check(h8.retainedBytes()==16*M&&strong.retainedBytes()==16*M,"busy CPU retained memory remains charged");
            }finally{SpeedWorkers1935.leaveLegacy(permit);}own.intact();
        }
    }
    static void racedNativeLease() {
        start();int bytes=16*(int)M;
        long a=MemoryNative1989.h8Acquire(bytes,89),b=MemoryNative1989.strongAcquire(bytes,113);
        check(a!=0&&b!=0,"actual native leases held across attempted relief");
        try(Owned own=new Owned()) {
            pressureAfterNative(56*M);
            check(!FinishMemory1989.fitsResident19108(32*M),"a raced busy native owner cannot manufacture free memory");
            long[] r=residentReport();check(r[3]==1&&r[5]==2&&r[4]==0,"trim request still requires authoritative unchanged admission");
            check(h8.trims==1&&strong.trims==1&&h8.retainedBytes()==bytes&&strong.retainedBytes()==bytes,"busy native pools defer freeing capacity");own.intact();
        }finally {check(MemoryNative1989.h8Release(a,bytes,89)&&MemoryNative1989.strongRelease(b,bytes,113),"all actual busy native sentinel bytes survive");}
        check(h8.retainedBytes()==0&&strong.retainedBytes()==0,"deferred native trim occurs only at original owner release");
    }
    static void failedPressureAndLimits() {
        start();prime(8*(int)M);
        try(Owned own=new Owned()) {
            pressureAfterNative(24*M);
            check(!FinishMemory1989.fitsResident19108(32*M),"insufficient idle capacity remains refused");
            long[] r=residentReport();check(r[3]==1&&r[4]==0&&r[5]==2,"one unsuccessful same-limit retry, no loop");own.intact();
        }
        for(long extra:new long[]{-1,512*M+1,Long.MAX_VALUE}) {
            start();prime(4096);try(Owned own=new Owned()) {
                check(!FinishMemory1989.fitsResident19108(extra),"invalid or over-ceiling request refused");
                long[] r=residentReport();check(r[3]==6&&r[5]==1&&h8.trims==0&&strong.trims==0,"unchanged fixed limit prevents trim/retry");own.intact();
            }
        }
        start();single.bytes=32*M;try(Owned own=new Owned()) {
            pressureAfterNative(48*M);check(!FinishMemory1989.fitsResident19108(32*M),"Single native owner stays charged");
            long[] r=residentReport();check(single.bytes==32*M&&single.trims==0&&r[9+9]==32*M&&r[21+9]==32*M,"Single is excluded from trim before and after");own.intact();
        }
        check(GpuNoise1960.MAX_BYTES==512*M,"original 512 MiB limit unchanged");
    }
    static void fastAndForeground() {
        start();prime(4096);try(Owned own=new Owned()) {
            check(FinishMemory1989.fitsResident19108(4*M),"already admitted carried request stays on fast path");
            check(h8.trims==0&&strong.trims==0&&GpuQualification1961.residentReports==0,"admitted request has no trim or report allocation");
            pressureAfterNative(24*M);GpuQualification1961.inBackground=false;
            check(!FinishMemory1989.fitsResident19108(32*M),"misrouted foreground uses original nonrelieving fits contract");
            check(h8.trims==0&&strong.trims==0&&GpuQualification1961.residentReports==0,"foreground does not enter resident background recovery");own.intact();
        }
    }
    static void cancellation() {
        for(int mode=0;mode<2;mode++) {
            start();prime(16*(int)M);try(Owned own=new Owned()) {
                pressureAfterNative(56*M);if(mode==0)GpuQualification1961.cancelled=true;else Thread.currentThread().interrupt();
                try {FinishMemory1989.fitsResident19108(32*M);throw new AssertionError("resident cancellation swallowed");}
                catch(CancellationException expected){check(h8.trims==0&&strong.trims==0,"initial cancellation preserves CPU native owners");}
                finally {if(mode==1)check(Thread.currentThread().isInterrupted(),"interruption flag survives");Thread.interrupted();GpuQualification1961.cancelled=false;}own.intact();
            }
        }
        start();prime(16*(int)M);try(Owned own=new Owned()) {
            SpeedWorkers1935.installScratchMemory1956(new SpeedWorkers1935.ScratchMemory(){
                public long retainedBytes(){return h8.retainedBytes();}
                public void trim(){h8.trim();GpuQualification1961.cancelled=true;}
            });pressureAfterNative(56*M);
            try {FinishMemory1989.fitsResident19108(32*M);throw new AssertionError("mid-trim cancellation swallowed");}
            catch(CancellationException expected){long[] r=residentReport();check(r[3]==4&&r[5]==1,"cancellation during optional relief stops before admission retry");}
            finally {GpuQualification1961.cancelled=false;}own.intact();
        }
    }
    static void optionalFailures() {
        start();prime(16*(int)M);try(Owned own=new Owned()) {
            pressureAfterNative(56*M);GpuQualification1961.fault=1;
            check(FinishMemory1989.fitsResident19108(32*M),"observer failure cannot change successful actual recovery");
            check(GpuQualification1961.residentReports==1&&h8.trims==1&&strong.trims==1,"report attempted after exactly one trim");own.intact();
        }
        start();prime(16*(int)M);try(Owned own=new Owned()) {
            pressureAfterNative(56*M);h8.fault=1;
            check(FinishMemory1989.fitsResident19108(32*M),"one optional trim failure does not stop independent owner relief");
            long[] r=residentReport();check(r[3]==7&&r[5]==2&&h8.retainedBytes()==16*M&&strong.retainedBytes()==0,"failed owner remains charged and reported");own.intact();
        }
        start();prime(16*(int)M);try(Owned own=new Owned()) {
            pressureAfterNative(56*M);WholeRoute1953.faultAt=2;
            check(FinishMemory1989.fitsResident19108(32*M),"optional snapshot failure does not supply or block admission");
            check(h8.trims==1&&strong.trims==1,"snapshot failure still trims once");own.intact();
        }
    }
    public static void main(String[] args)throws Exception {
        try {
            recoveredAndGuard();busyCpu();racedNativeLease();failedPressureAndLimits();fastAndForeground();cancellation();optionalFailures();
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"resident_memory_same_budget_retry19108\":true,\"resident_memory_active_gpu_preserved19108\":true,\"resident_memory_busy_cpu_preserved19108\":true,\"resident_memory_cancellation_preserved19108\":true,\"resident_memory_original_paths_preserved19108\":true,\"resident_memory_actual_jni19108\":true,\"resident_memory_bounded_report19108\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
        }finally {reset();((ExecutorService)field(GpuNoise1960.class,"OWNER").get(null)).shutdownNow();((ExecutorService)field(SpeedWorkers1935.class,"EXECUTOR").get(null)).shutdownNow();}
    }
}
