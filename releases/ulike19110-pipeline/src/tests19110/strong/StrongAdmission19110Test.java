package com.hiro.ulike;

import static com.hiro.ulike.Memory1989Test.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/** Real production Strong admission, capacity accounting and JNI/GLES owners.
 * Oracle/legacy/candidate/timing order repeats admission, not image equivalence.
 * Android scheduling and device speed are deliberately outside this proof. */
public final class StrongAdmission19110Test {
    static int cases,rounds;
    static final int[] SENTINEL={0xff123456,0x12345678,0x76543210,0xffabcdef};
    static void start(){reset();GpuQualification1961.strongReports=0;GpuQualification1961.strongLast=null;cases++;}
    static StrongNoise1958.Model model(int size)throws Exception{
        Constructor<StrongNoise1958.Model> c=StrongNoise1958.Model.class.getDeclaredConstructor(int.class,int.class,int[].class,int[].class,int[].class,float[].class,int.class);c.setAccessible(true);
        int[] h=new int[(size/2)*(size/2)],q=new int[(size/4)*(size/4)],e=new int[(size/8)*(size/8)];h[0]=SENTINEL[0];q[0]=SENTINEL[1];e[0]=SENTINEL[2];
        return c.newInstance(size,size,h,q,e,new float[48],0);
    }
    static final class Owned implements AutoCloseable {
        final StrongNoise1958.Model model;final Object stage;final Class<?> type;GpuChain1961.Preflight1981 plan;
        Owned(int size,boolean carried)throws Exception{
            model=model(size);type=Class.forName("com.hiro.ulike.GpuStrong1960$Stage");Constructor<?> c=type.getDeclaredConstructor(StrongNoise1958.Model.class,boolean.class);c.setAccessible(true);stage=c.newInstance(model,false);field(GpuStrong1960.class,"active").set(null,stage);
            if(carried){GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"live GPU owner opens");check(s.upload(0,SENTINEL),"real source sentinel uploads");field(type,"session").set(stage,s);}
        }
        GpuNoise1960.Session session()throws Exception{return (GpuNoise1960.Session)field(type,"session").get(stage);}
        void preflight(long capacity,long javaBytes)throws Exception{
            plan=new GpuChain1961.Preflight1981(null,0,model.width,model.height,128,null,new int[]{23},new long[]{capacity},new long[]{0},javaBytes);
            ((ThreadLocal<GpuChain1961.Preflight1981>)field(GpuChain1961.class,"PREFLIGHT1981").get(null)).set(plan);
        }
        boolean enable(){return GpuStrong1960.enableResident1976(model,model.width,model.height);}
        void intact()throws Exception{WholeRoute1953.bytes=0;check(Arrays.equals(session().readInts(0,4),SENTINEL),"live GPU source survives admission byte for byte");}
        public void close(){WholeRoute1953.bytes=0;if(plan!=null)plan.close();GpuStrong1960.endStage(model);check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"original owners close every session and reservation");}
    }
    static long[] report(){long[] r=GpuQualification1961.strongLast;check(r!=null&&r.length==18&&r[0]==1,"fixed optional eighteen-scalar report");return r;}
    static void refusal(Owned own,String expected){try{own.enable();throw new AssertionError("admission unexpectedly succeeded: "+expected);}catch(RuntimeException fail){check(expected.equals(GpuStrong1960.residentAdmissionReason19110(fail)),"typed reason "+expected+" observed: "+fail);}}
    static void checkRecovery(long[] r,int stage){check(r[1]==stage&&r[2]==0&&r[3]==1&&r[4]==1&&r[5]==1&&r[17]==1,"one physical-budget rejection, one trim, successful same-limit retry");check(r[7]>r[8]&&r[9]<r[10],"real original refusal and reclaimed capacity reported");check(h8.trims==1&&strong.trims==1&&single.trims==0,"only two idle CPU owners trimmed once");check(r[13]==32*M&&r[14]==0,"actual JNI allocations reclaimed");}
    static void repeatedTrials()throws Exception{
        for(int trial=1;trial<=2;trial++){
            start();prime(16*(int)M); // CPU oracle leaves real idle native workspaces.
            try(Owned legacy=new Owned(1024,false)){
                Method init=GpuStrong1960.class.getDeclaredMethod("initialize1971",legacy.type);init.setAccessible(true);
                check(Boolean.TRUE.equals(init.invoke(null,legacy.stage)),"existing production model admission completes before candidate");
                check(legacy.session().readInts(4,1)[0]==SENTINEL[0],"legacy model upload remains exact");
            }
            for(int phase=0;phase<2;phase++){
                // Oracle/existing/timing CPU phases may retain workspace again.
                prime(16*(int)M);GpuQualification1961.strongReports=0;GpuQualification1961.strongLast=null;h8.trims=strong.trims=0;
                try(Owned own=new Owned(1024,true)){
                    GpuNoise1960.Lease1971 live=own.session().reserveCapacity1971(new int[]{22},new long[]{4096},M);
                    check(live!=null,"independent live reservation precedes Strong admission");
                    try{pressureAfterNative(35*M);check(own.enable(),"UNUSED_NATIVE_CACHE_BLOCKS_STRONG_RESIDENT_ADMISSION trial "+trial);checkRecovery(report(),5);own.intact();check(GpuNoise1960.reservedBytes1971()>=M,"independent readback debt preserved");check(own.session().uploadRange1976(24,0,SENTINEL,0,4),"output slot remains writable");check(Arrays.equals(own.session().readInts(24,4),SENTINEL),"real output pixels survive recovered slot allocation");}
                    finally{live.close();}
                }
            }
            rounds++;
        }
    }
    static void modelAndForecast()throws Exception{
        start();prime(16*(int)M);try(Owned own=new Owned(1024,false)){
            pressureAfterNative(33*M);check(own.enable(),"initial model-capacity refusal recovers");checkRecovery(report(),2);WholeRoute1953.bytes=0;check(own.session().readInts(4,1)[0]==SENTINEL[0],"model upload exact after retry");
        }
        start();prime(16*(int)M);try(Owned own=new Owned(1024,true)){
            own.preflight(16*M,2*M);pressureAfterNative(40*M);check(own.enable(),"whole-chain forecast failure recovers without closing carried owner");checkRecovery(report(),4);check(own.plan.session==own.session()&&own.plan.lease!=null,"original forecast ownership binds exactly once");own.intact();
        }
    }
    static void limitsAndPressure()throws Exception{
        start();prime(4096);try(Owned own=new Owned(1024,true)){WholeRoute1953.bytes=headroom();refusal(own,"memory_budget");long[] r=report();check(r[4]==1&&r[17]==0&&h8.trims==1&&strong.trims==1,"unrecoverable memory receives one attempt only");own.intact();}
        start();prime(4096);GpuQualification1961.inBackground=false;try(Owned own=new Owned(1024,true)){WholeRoute1953.bytes=headroom();refusal(own,"memory_budget");check(report()[5]==5&&report()[4]==0&&h8.trims==0&&strong.trims==0,"foreground does not reclaim CPU storage");own.intact();}
        start();prime(4096);try(Owned own=new Owned(1024,true)){WholeRoute1953.bytes=headroom();boolean permit=SpeedWorkers1935.enterLegacy();try{refusal(own,"memory_budget");check(report()[5]==2&&report()[4]==0&&h8.trims==0&&strong.trims==0,"active CPU work prevents trim and retry");}finally{SpeedWorkers1935.leaveLegacy(permit);}own.intact();}
        start();prime(4096);try(Owned own=new Owned(64,true)){
            GpuNoise1960.Lease1971[] leases=new GpuNoise1960.Lease1971[3];try{for(int i=0;i<3;i++){leases[i]=own.session().reserveCapacity1971(new int[]{20+i},new long[]{4096},0);check(leases[i]!=null,"three legitimate original leases admitted");}refusal(own,"lease_limit");check(report()[3]==5&&report()[4]==0&&h8.trims==0&&strong.trims==0,"lease ceiling never masquerades as physical-budget failure");own.intact();}finally{for(GpuNoise1960.Lease1971 l:leases)if(l!=null)l.close();}
        }
        start();try(Owned own=new Owned(64,true)){try{GpuStrong1960.enableResident1976(own.model,0,64);throw new AssertionError("bad dimensions accepted");}catch(RuntimeException e){check("input_invalid".equals(GpuStrong1960.residentAdmissionReason19110(e)),"invalid input has exact type");}check(h8.trims==0&&strong.trims==0,"invalid input does not trim");}
        start();try(Owned own=new Owned(64,false)){GpuNoise1960.Session other=GpuNoise1960.open();try{refusal(own,"session_unavailable");check(h8.trims==0&&strong.trims==0,"another owner is never evicted");}finally{other.close();}}
        start();try(Owned own=new Owned(64,true)){own.preflight(4096,0);check(own.plan.bind(own.session()),"prebound plan belongs to original owner");refusal(own,"forecast_unavailable");check(report()[3]==8&&report()[2]==12&&h8.trims==0&&strong.trims==0,"unmeasured forecast rejection cannot authorize memory repair");own.intact();}
        start();prime(4096);try(Owned own=new Owned(64,true)){h8.queryFault=1;refusal(own,"capacity_unavailable");check(report()[2]==6&&report()[4]==0&&h8.trims==0&&strong.trims==0,"unknown native owner bytes cannot authorize memory relief");h8.queryFault=0;own.intact();}
        check(GpuStrong1960.residentAdmissionReason19110(new IllegalStateException("memory_budget"))==null,"exception text cannot forge memory recovery type");
    }
    static void activeNativeAndCancellation()throws Exception{
        start();int bytes=16*(int)M;long a=MemoryNative1989.h8Acquire(bytes,89),b=MemoryNative1989.strongAcquire(bytes,113);check(a!=0&&b!=0,"real active native leases held");
        try(Owned own=new Owned(1024,true)){pressureAfterNative(35*M);refusal(own,"memory_budget");check(h8.retainedBytes()==bytes&&strong.retainedBytes()==bytes,"raced native owners retain all live bytes");own.intact();}
        finally{check(MemoryNative1989.h8Release(a,bytes,89)&&MemoryNative1989.strongRelease(b,bytes,113),"live CPU sentinel bytes remain exact across trim request");}
        check(h8.retainedBytes()==0&&strong.retainedBytes()==0,"deferred trim happens only at original owner release");
        for(int mode=0;mode<3;mode++){
            start();prime(16*(int)M);try(Owned own=new Owned(1024,true)){
                pressureAfterNative(35*M);if(mode==0)GpuQualification1961.cancelled=true;else if(mode==1)Thread.currentThread().interrupt();else SpeedWorkers1935.installScratchMemory1956(new SpeedWorkers1935.ScratchMemory(){public long retainedBytes(){return h8.retainedBytes();}public void trim(){h8.trim();GpuQualification1961.cancelled=true;}});
                try{own.enable();throw new AssertionError("cancelled admission succeeded");}catch(CancellationException expected){check(GpuStrong1960.residentAdmissionReason19110(expected)==null,"cancellation keeps original exception identity");if(mode<2)check(h8.trims==0&&strong.trims==0,"entry cancellation has no trim");else check(report()[5]==4&&report()[4]==0&&report()[17]==0,"cancellation after trim is observed before any retry");}
                finally{Thread.interrupted();GpuQualification1961.cancelled=false;}
                own.intact();
            }
        }
    }
    static void optionalReports()throws Exception{
        start();prime(16*(int)M);try(Owned own=new Owned(1024,true)){pressureAfterNative(35*M);GpuQualification1961.fault=1;check(own.enable(),"hostile optional observer cannot change completed admission");check(h8.trims==1&&strong.trims==1,"observer fault cannot repeat trim");own.intact();}
        start();prime(4096);try(Owned own=new Owned(64,true)){check(own.enable(),"normal already-fitting admission succeeds");check(h8.trims==0&&strong.trims==0&&GpuQualification1961.strongReports==0,"normal fitting path skips recovery and report allocation");own.intact();}
    }
    static void actualCapacityReasons()throws Exception{
        start();try(Owned own=new Owned(64,true)){
            GpuNoise1960.Session s=own.session();check(s.reserveCapacity1971(new int[]{99},new long[]{16},0)==null&&GpuNoise1960.capacityFailure19110()[0]==3,"invalid native slot reports input refusal");
            GpuNoise1960.Lease1971 l=s.reserveCapacity1971(new int[]{20},new long[]{4096},M);check(l!=null&&GpuNoise1960.capacityFailure19110()==null,"success clears prior capacity reason");
            try{
                WholeRoute1953.bytes=headroom();check(!l.revalidate1971()&&GpuNoise1960.capacityFailure19110()[0]==1,"actual existing lease recheck identifies physical budget");
                long[] first=GpuNoise1960.capacityFailure19110();first[0]=0;check(GpuNoise1960.capacityFailure19110()[0]==1,"report getter cannot mutate retry authorization");
                final CountDownLatch ready=new CountDownLatch(1),done=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
                Thread t=new Thread(new Runnable(){public void run(){try{check(!l.revalidate1971(),"worker observes its own memory refusal");ready.countDown();done.await();check(GpuNoise1960.capacityFailure19110()[0]==1,"another caller cannot replace this caller's failure reason");}catch(Throwable x){failure.set(x);ready.countDown();}}});t.start();ready.await();WholeRoute1953.bytes=0;check(l.revalidate1971()&&GpuNoise1960.capacityFailure19110()==null,"same lease accepts independently in later caller");done.countDown();t.join();if(failure.get()!=null)throw new AssertionError(failure.get());
                own.intact();
            }finally{l.close();}
            check(!l.revalidate1971()&&GpuNoise1960.capacityFailure19110()[0]==7,"closed lease refusal is session ownership, never memory");
            check(GpuNoise1960.MAX_BYTES==512*M,"native fixed ceiling unchanged");
        }
    }
    public static void main(String[] args)throws Exception{
        try{repeatedTrials();modelAndForecast();limitsAndPressure();activeNativeAndCancellation();optionalReports();actualCapacityReasons();
            check(rounds==2,"two independent verification rounds complete");check(((List<?>)field(GpuNoise1960.class,"leases1971").get(null)).isEmpty(),"no final live GPU debt remains");
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"rounds\":"+rounds+",\"actual_strong_java_executed\":true,\"actual_memory_java_executed\":true,\"actual_native_pools_executed\":true,\"actual_gpu_lease_executed\":true,\"pixel_equivalence_tested\":false,\"physical_android_tested\":false,\"strong_admission_same_budget_retry19110\":true,\"strong_admission_typed_refusal19110\":true,\"strong_admission_live_ownership19110\":true,\"strong_admission_cancellation19110\":true,\"strong_admission_repeat_trial19110\":true,\"strong_admission_actual_jni19110\":true,\"strong_admission_optional_report19110\":true}");
        }finally{reset();((ExecutorService)field(GpuNoise1960.class,"OWNER").get(null)).shutdownNow();((ExecutorService)field(SpeedWorkers1935.class,"EXECUTOR").get(null)).shutdownNow();}
    }
}
