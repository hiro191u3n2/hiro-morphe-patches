package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual unchanged production Java and JNI execute in software GLES. */
public final class Memory1971Test {
    static int assertions;
    static final long M=1024L*1024;
    static native void setFault(int code);
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static int leases()throws Exception {Field f=GpuNoise1960.class.getDeclaredField("leases1971");f.setAccessible(true);return ((List<?>)f.get(null)).size();}
    static GpuNoise1960.Session open(){GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"actual private native session opens");return s;}
    static long rounded(long bytes){return (bytes+4095)&~4095L;}
    static void logic()throws Exception {
        GpuNoise1960.Session s=open();long[] initial=s.capacity1971();
        check(initial!=null&&initial.length==27&&initial[25]>=80*M,"actual bounded capacity/maxStorage JNI snapshot");
        check(initial[24]==0&&initial[26]==0,"fresh SSBO and staging capacities start empty");
        check(s.failureCode1971()==0,"fresh fixed failure code is clear");
        check(s.reserveCapacity1971(new int[]{0},new long[]{0},0)==null,"zero target excluded");
        check(s.reserveCapacity1971(new int[]{24},new long[]{4},0)==null,"invalid slot excluded");
        check(s.reserveCapacity1971(new int[]{0,0},new long[]{4,8},0)==null,"duplicate slot target excluded");
        check(s.reserveCapacity1971(new int[]{0},new long[]{GpuNoise1960.MAX_BYTES+1},0)==null,"native maximum unchanged");
        check(s.reserveCapacity1971(new int[]{0,1,2,3},new long[]{128*M,128*M,128*M,128*M},0)==null,"combined target and temporary peak cannot exceed native 512 MiB limit");
        check(s.reserveCapacity1971(new int[]{0},new long[]{4},-1)==null,"negative Java peak excluded");
        check(GpuNoise1960.reservedBytes1971()==0&&leases()==0,"invalid requests retain no debt");
        GpuNoise1960.Lease1971 model=s.reserveCapacity1971(new int[]{2,4,5,6},new long[]{256,24*M,6*M,1536*1024},0);
        long modelBytes=rounded(256)+24*M+6*M+1536*1024;
        check(model!=null&&GpuNoise1960.reservedBytes1971()==modelBytes+24*M,"initial model includes largest upload staging peak");
        model.close();check(GpuNoise1960.reservedBytes1971()==0,"unused model debt released");
        GpuNoise1960.Lease1971 mixed=s.reserveCapacity1971(new int[]{0,1},new long[]{32*M,80*M},0);
        check(mixed!=null&&GpuNoise1960.reservedBytes1971()==224*M,"small persistent staging and large temporary upload coexist");
        mixed.close();
        int size=65537;long bytes=4L*size,cap=rounded(bytes),javaPeak=65536;
        GpuNoise1960.Lease1971 a=s.reserveCapacity1971(new int[]{0},new long[]{bytes},javaPeak);
        GpuNoise1960.Lease1971 b=s.reserveCapacity1971(new int[]{14},new long[]{bytes},javaPeak);
        check(a!=null&&b!=null,"two bank target reservations coexist");
        check(GpuNoise1960.reservedBytes1971()==3*cap+2*javaPeak,"shared staging reserved once and Java peaks sum");
        check(s.upload(0,new int[size]),"first materialized upload completes");
        long[] materialized=s.capacity1971();check(materialized[0]==cap&&materialized[24]==cap&&materialized[26]==cap,"native query reports actual rounded materialization");
        check(GpuNoise1960.retainedBytes()==2*cap&&GpuNoise1960.reservedBytes1971()==cap+2*javaPeak,"actual capacity growth replaces debt exactly");
        check(s.upload(14,new int[size]),"second bank materializes");
        check(GpuNoise1960.retainedBytes()==3*cap&&GpuNoise1960.reservedBytes1971()==2*javaPeak,"resident banks leave only private Java debt");
        check(a.revalidate1971()&&b.revalidate1971(),"held leases revalidate fresh actual memory");
        WholeRoute1953.retained=Long.MAX_VALUE;
        check(!a.revalidate1971()&&!GpuNoise1960.workspaceFits(1),"future owner growth refuses GPU admission without deleting ownership");
        WholeRoute1953.retained=0;check(a.revalidate1971(),"fresh memory recovery can revalidate existing reservation");
        a.close();a.close();b.close();check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"lease close is idempotent and releases all debt");
        GpuNoise1960.Lease1971 reuse=s.reserveCapacity1971(new int[]{0},new long[]{bytes},javaPeak);
        check(reuse!=null&&GpuNoise1960.reservedBytes1971()==javaPeak,"existing bank and staging capacity never double charged");
        WholeRoute1953.fail=true;check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"failed concurrent budget query declines only new lease");WholeRoute1953.fail=false;
        check(leases()==1&&GpuNoise1960.reservedBytes1971()==javaPeak&&reuse.revalidate1971(),"failed new admission preserves earlier owned readback debt");reuse.close();
        GpuNoise1960.Lease1971[] limits=new GpuNoise1960.Lease1971[3];
        for(int i=0;i<3;i++)limits[i]=s.reserveCapacity1971(new int[]{i+8},new long[]{4},0);
        check(limits[0]!=null&&limits[1]!=null&&limits[2]!=null,"bounded model and two-bank leases permitted");
        check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"fourth lease refused");
        for(GpuNoise1960.Lease1971 lease:limits)lease.close();
        ExecutorService callers=Executors.newFixedThreadPool(4);CountDownLatch attempt=new CountDownLatch(1);
        List<Future<GpuNoise1960.Lease1971>> futures=new ArrayList<Future<GpuNoise1960.Lease1971>>();
        for(int i=0;i<4;i++){final int slot=8+i;futures.add(callers.submit(()->{attempt.await();return s.reserveCapacity1971(new int[]{slot},new long[]{4},0);}));}
        attempt.countDown();int owned=0;List<GpuNoise1960.Lease1971> concurrent=new ArrayList<GpuNoise1960.Lease1971>();for(Future<GpuNoise1960.Lease1971> future:futures){GpuNoise1960.Lease1971 lease=future.get(3,TimeUnit.SECONDS);if(lease!=null){owned++;concurrent.add(lease);}}
        check(owned==3&&leases()==3,"four simultaneous callers cannot race bounded scalar admission");
        for(GpuNoise1960.Lease1971 lease:concurrent)lease.close();callers.shutdownNow();check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"concurrent caller ownership releases completely");
        WholeRoute1953.fail=true;check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"post-add physical-budget exception returns declined admission");WholeRoute1953.fail=false;
        check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"exception rolls back unreachable lease and debt");
        WholeRoute1953.oom=true;check(s.reserveCapacity1971(new int[]{20},new long[]{4},0)==null,"post-add physical-budget OOM declines admission");WholeRoute1953.oom=false;
        check(leases()==0&&GpuNoise1960.reservedBytes1971()==0,"OOM rolls back unreachable lease and debt");
        GpuNoise1960.Lease1971 retained=s.reserveCapacity1971(new int[]{20},new long[]{4},0);check(retained!=null,"later valid reservation recovers after failures");
        s.close();check(leases()==0&&GpuNoise1960.reservedBytes1971()==0&&!retained.revalidate1971(),"session close releases every scalar lease");
        check(s.capacity1971()==null,"closed session cannot expose a new owner's capacities");retained.close();
        GpuNoise1960.Session next=open();check(next.failureCode1971()==0&&s.failureCode1971()==0,"closed clear diagnostic stable across new session");next.close();
        check(GpuNoise1960.MAX_BYTES==512*M,"fixed native limit preserved");
    }
    static GpuNoise1960.Batch compareBatch(){int[] u=new int[32];u[0]=4;return new GpuNoise1960.Batch().upload(0,new int[]{1,2,3,4}).upload(1,new int[]{1,2,3,4}).upload(2,new int[]{0}).dispatch(GpuNoise1960.COMPARE1961,new int[]{0,1,2},u,null,4);}
    static void fault(int code)throws Exception {
        check(GpuNoise1960.supports(GpuNoise1960.COMPARE1961),"real comparison program supported");
        GpuNoise1960.Session s=open();GpuNoise1960.Lease1971 lease=s.reserveCapacity1971(new int[]{0,1,2},new long[]{16,16,4},128);check(lease!=null,"fault candidate has owned scalar reservation");
        if(code==4){check(s.upload(0,new int[]{1,2,3,4}),"readback fault input upload succeeds");setFault(code);check(s.readInts(0,4)==null,"actual readback fault returns no candidate");}
        else if(code==5){GpuNoise1960.Ticket ticket=s.submit(compareBatch(),0);check(ticket!=null,"fence test submits actual private ticket");setFault(code);check(s.collect(ticket,new int[]{2},new int[]{1})==null,"actual timeout cannot return output");}
        else {setFault(code);check(!s.run(compareBatch()),"actual wrapped native command fault returns failure");}
        check(s.failureCode1971()==code,"fixed failure code retains originating stage "+code);
        setFault(0);check(s.failureCode1971()==code,"later generic failure does not overwrite first native reason");
        s.close();check(GpuNoise1960.reservedBytes1971()==0&&leases()==0,"failure session releases Java and pending capacity debt");lease.close();check(s.failureCode1971()==code,"closed session retains first scalar reason");
        if(code==5){check(GpuNoise1960.retainedBytes()>0,"unknown completion native capacities remain quarantined and counted");check(GpuNoise1960.open()==null,"timeout cannot reopen quarantined GPU context");GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()>0,"idle trim cannot free unknown-completion allocations");}
        else {GpuNoise1960.Session next=open();check(next.failureCode1971()==0&&s.failureCode1971()==code,"new session reset does not erase previous session cause");next.close();}
    }
    public static void main(String[] args)throws Exception {if(args.length==0||"logic".equals(args[0]))logic();else fault(Integer.parseInt(args[0]));System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+"}");}
}

final class WholeRoute1953 {static volatile long retained;static volatile boolean fail,oom;static long retainedBytes(){if(fail)throw new IllegalStateException("budget fixture fault");if(oom)throw new OutOfMemoryError("budget fixture fault");return retained;}}
final class GpuFinish1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static long nativeRetainedBytes1956(){return 0;}}
