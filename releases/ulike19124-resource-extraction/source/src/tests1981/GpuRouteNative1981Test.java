package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** Current Strong/tuner/queue/worker/JNI closure, frozen published CPU pixels. */
public final class GpuRouteNative1981Test {
    static int assertions;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static boolean waiting(Object stage,Class<?> type)throws Exception{synchronized(stage){return field(type,"waiter").getBoolean(stage);}}
    static void end(StrongNoise1958.Model model)throws Exception{
        try{GpuStrong1960.endStage(model);}finally{Thread.interrupted();SaveQueue1935.release();NativeSpeed1979Test.clearFault();WholeRoute1953.retained=0;}
        check(GpuNoise1960.reservedBytes1971()==0&&!GpuNoise1960.sessionBusy()&&SpeedWorkers1935.cpuIdle1944(),"actual stage, workers and GPU leases drained");
    }
    static void drain()throws Exception{
        field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
        long limit=System.nanoTime()+90000000000L;
        while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>limit)throw new AssertionError("actual idle tuner did not complete");Thread.sleep(5);}
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"idle JNI work drains every session and lease");
    }
    static void pixels(Bootstrap1979Test.Job job){
        check(job.failure==null&&job.ok,"production Strong facade completed");
        check(Arrays.equals(job.d.want,Arrays.copyOfRange(job.out,job.d.u[2]*job.d.u[0],job.d.u[3]*job.d.u[0])),"every saved ARGB equals frozen .59 CPU");
        check(Arrays.equals(job.d.confidence,Arrays.copyOf(job.cf,job.d.confidence.length)),"every NR13 confidence equals frozen .59 CPU");
        for(int i=job.d.confidence.length;i<job.cf.length;i++)check(job.cf[i]==0x2468ace0,"pooled confidence tail untouched");
    }
    static GpuStrong1960.Route1981 route(Bootstrap1979Test.Data d,StrongNoise1958.Model model){
        int[] u=d.u;return GpuStrong1960.route1981(u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,model,true,true,null);
    }
    static void forecast()throws Exception{
        check(!GpuNoise1960.planFits1981(null,0)&&!GpuNoise1960.planFits1981(new long[0],0),"no empty native capacity forecast");
        check(!GpuNoise1960.planFits1981(new long[]{0},0)&&!GpuNoise1960.planFits1981(new long[]{-1},0),"invalid per-slot sizes cannot reach admission");
        check(!GpuNoise1960.planFits1981(new long[]{Long.MAX_VALUE},0)&&!GpuNoise1960.planFits1981(new long[]{4},-1),"forecast integer/domain overflow is denied");
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"actual forecast session opens");
        try{
            long[] capacities=session.capacity1976();check(capacities!=null&&capacities[26]>0,"real native per-buffer limit is obtained");
            check(!GpuNoise1960.planFits1981(new long[]{capacities[26]+1},0),"forecast honors the actual device storage limit");
            check(GpuNoise1960.planFits1981(new long[]{4,1024,65536},1048576),"cached limit permits a small fit without another native session");
            check(GpuNoise1960.sessionBusy()&&session.capacity1976()!=null,"scalar forecast preserves the live session token");
            GpuNoise1960.Lease1971 future=session.reserveCapacity1971(new int[]{24,1,3,8,17,18,5},new long[]{20480,20480,65536,65536,16384,16384,4},1048576);
            check(future!=null&&future.revalidate1971(),"real native lease accepts the finish forecast");
            GpuNoise1960.Lease1971 a=null,b=null;
            try{
                a=session.reserveCapacity1971(new int[]{0,1,3},new long[]{1024,1024,2048},4096);
                b=session.reserveCapacity1971(new int[]{14,15,16},new long[]{1024,1024,2048},4096);
                check(a!=null&&b!=null&&a.revalidate1971()&&b.revalidate1971(),"finish forecast and both Strong banks coexist within the real three-lease ceiling");
                check(session.reserveCapacity1971(new int[]{23},new long[]{4},0)==null,"fourth concurrent native lease remains forbidden");
            }finally{if(b!=null)b.close();if(a!=null)a.close();future.close();}
            check(GpuNoise1960.reservedBytes1971()==0,"actual three-lease forecast releases all debt");
        }finally{session.close();}
    }
    static void coldIdleHot(Bootstrap1979Test.Data d)throws Exception{
        Bootstrap1979Test.next();StrongNoise1958.Model model=d.model();Bootstrap1979Test.begin(model);
        int before=NativeSpeed1979Test.dispatches();Bootstrap1979Test.Job first=new Bootstrap1979Test.Job(d,model,"normal");
        try{
            SpeedWorkers1935.run(new Runnable[]{first});pixels(first);
            check(first.calls==1,"cold capture computes ordinary CPU exactly once");
            check(NativeSpeed1979Test.dispatches()==before,"cold capture performs no GPU proof dispatch");
            check(GpuQualification1961.retainedBytes()>0,"cold capture queues an owned bounded idle proof");
            check(StrongNoise1958.oracleCalls.get()==0,"queued CPU verification stays off the active save");
        }finally{end(model);}
        drain();check(NativeSpeed1979Test.dispatches()>before,"idle job executed real production GLSL");
        check(StrongNoise1958.oracleCalls.get()>=2,"idle admission computed two independent CPU references");
        int accepted=0;for(int p=0;p<4;p++){GpuQualification1961.Record proof=GpuQualification1961.restore(GpuStrong1960.profileKey1973(d.u,p));if(proof!=null){accepted++;System.out.println("proved profile="+p+" cpu="+proof.cpuNanos+" gpu="+proof.gpuNanos);}}
        check(accepted>0,"idle legacy exact2 progress is persisted without a foreground bootstrap");
        Bootstrap1979Test.caches();check(route(d,d.model()).profile>=0,"signed persisted child restores a usable route after RAM eviction");
        Bootstrap1979Test.next();model=d.model();Bootstrap1979Test.begin(model);Bootstrap1979Test.Job hot=new Bootstrap1979Test.Job(d,model,"normal");before=NativeSpeed1979Test.dispatches();
        try{
            SpeedWorkers1935.run(new Runnable[]{hot});pixels(hot);check(hot.calls==0,"hot accepted route repeats no CPU oracle");
            check(NativeSpeed1979Test.dispatches()>before,"hot route used actual native GPU output");
        }finally{end(model);}
        drain();
    }
    static void waitAndSpeed(Bootstrap1979Test.Data d)throws Exception{
        Bootstrap1979Test.next();StrongNoise1958.Model model=d.model();Bootstrap1979Test.begin(model);
        Object stage=field(GpuStrong1960.class,"active").get(null);Class<?> type=stage.getClass();
        GpuStrong1960.Route1981 selected=route(d,model);check(selected!=null&&selected.profile>=0,"actual previously proved route is available");
        String key=selected.key;int variant=selected.variant;
        // Exactness was proved above. Controlled timing metadata tests only the
        // dispatch policy; no new/unverified pixel route is authorized here.
        for(int p=0;p<8;p++)if(p!=selected.profile)GpuQualification1961.rejectExact1971(GpuStrong1960.profileKey1973(d.u,p),"argb_mismatch");
        GpuQualification1961.qualifiedStrongPreferred1970(key,100000000L,200000000L,variant);
        check(route(d,model).profile<0,"a slower certified GPU takes the unchanged CPU route");
        int before=NativeSpeed1979Test.dispatches();Bootstrap1979Test.Job cpu=new Bootstrap1979Test.Job(d,model,"normal");
        SpeedWorkers1935.run(new Runnable[]{cpu});pixels(cpu);check(cpu.calls==1&&NativeSpeed1979Test.dispatches()==before,"slower GPU creates no extra dispatch or oracle");
        GpuQualification1961.qualifiedStrongPreferred1970(key,100000000L,80000000L,variant);
        Method claim=GpuStrong1960.class.getDeclaredMethod("claimBank1981",type,String.class,int.class,long.class);claim.setAccessible(true);
        boolean[] banks=(boolean[])field(type,"banks").get(stage);long[] starts=(long[])field(type,"bankStart").get(stage),durations=(long[])field(type,"bankDuration").get(stage);
        try{
            synchronized(stage){Arrays.fill(banks,true);Arrays.fill(starts,System.nanoTime());Arrays.fill(durations,200000000L);long started=System.nanoTime();
                check(((Integer)claim.invoke(null,stage,key,variant,started))==-1,"predicted wait plus transfers exceeds CPU budget");
                check(System.nanoTime()-started<500000000L,"known slow queue never uses the old 15-second preference wait");
                Arrays.fill(starts,System.nanoTime()-300000000L);Arrays.fill(durations,1000000L);
                check(((Integer)claim.invoke(null,stage,key,variant,System.nanoTime()))==-1,"overdue banks are not treated as immediately available");
            }
            GpuQualification1961.qualifiedStrongPreferred1970(key,1000000000L,10000000L,variant);
            synchronized(stage){Arrays.fill(starts,System.nanoTime());Arrays.fill(durations,200000000L);
                check(((Integer)claim.invoke(null,stage,key,variant,System.nanoTime()))==-1,"normal wait has a 50 ms hard ceiling even for long CPU certificates");}
            final Object owner=stage;final boolean[] acquired={false};final Throwable[] failure={null};
            synchronized(stage){Arrays.fill(starts,System.nanoTime());Arrays.fill(durations,45000000L);}
            Thread waiter=new Thread(new Runnable(){public void run(){try{synchronized(owner){acquired[0]=((Integer)claim.invoke(null,owner,key,variant,System.nanoTime()))>=0;}}catch(Throwable e){failure[0]=e;}}});
            waiter.start();long limit=System.nanoTime()+2000000000L;
            while(!waiting(stage,type)){if(!waiter.isAlive()||System.nanoTime()>limit)throw new AssertionError("bounded waiter did not start");Thread.sleep(1);}
            synchronized(stage){banks[0]=false;stage.notifyAll();}waiter.join(2000);
            check(!waiter.isAlive()&&failure[0]==null&&acquired[0],"one bounded waiter consumes a real notified bank release");
            check(!waiting(stage,type),"successful wait releases its single waiter slot");
            synchronized(stage){Arrays.fill(banks,true);Arrays.fill(starts,System.nanoTime());Arrays.fill(durations,45000000L);}
            Thread cancelled=new Thread(new Runnable(){public void run(){try{synchronized(owner){claim.invoke(null,owner,key,variant,System.nanoTime());}}catch(Throwable e){failure[0]=e;}}});
            failure[0]=null;cancelled.start();limit=System.nanoTime()+2000000000L;
            while(!waiting(stage,type)){if(!cancelled.isAlive()||System.nanoTime()>limit)throw new AssertionError("cancellable waiter did not start");Thread.sleep(1);}
            cancelled.interrupt();cancelled.join(2000);
            Throwable stopped=failure[0] instanceof InvocationTargetException?((InvocationTargetException)failure[0]).getCause():failure[0];
            check(!cancelled.isAlive()&&stopped instanceof CancellationException,"wait cancellation propagates before any image commit");
            check(!waiting(stage,type),"cancelled wait frees its ownership slot");
            GpuQualification1961.rejectExact1971(key,"policy_failure");Bootstrap1979Test.caches();
            check(route(d,model).profile<0,"durable negative cannot be bypassed by an old route ticket");
        }finally{synchronized(stage){Arrays.fill(banks,false);stage.notifyAll();}end(model);GpuQualification1961.captureChanged();drain();}
    }
    static void uncommittedFailures(Bootstrap1979Test.Data d)throws Exception{
        for(int fault=0;fault<2;fault++){
            Bootstrap1979Test.next();StrongNoise1958.Model model=d.model();Bootstrap1979Test.begin(model);
            GpuStrong1960.Route1981 admitted=route(d,model);check(admitted!=null&&admitted.profile>=0,"fault test uses a real previously proved fast route");
            Bootstrap1979Test.Job job=new Bootstrap1979Test.Job(d,model,"normal");
            try{
                if(fault==0){NativeSpeed1979Test.failRead();SpeedWorkers1935.run(new Runnable[]{job});check(!job.ok&&job.failure==null,"readback failure delegates unchanged CPU repair to caller");}
                else{
                    int before=NativeSpeed1979Test.dispatches();NativeSpeed1979Test.delay(60);Thread worker=new Thread(job);worker.start();long until=System.nanoTime()+5000000000L;
                    while(NativeSpeed1979Test.dispatches()==before){if(!worker.isAlive()||System.nanoTime()>until)throw new AssertionError("cancellable real GPU dispatch did not start");Thread.sleep(1);}
                    worker.interrupt();worker.join(5000);check(!worker.isAlive()&&job.failure instanceof CancellationException,"cancellation after native submission propagates");
                }
                check(job.calls==0,"hot failure never recomputes a qualification CPU oracle");
                for(int p:job.out)check(p==0x13579bdf,"failed GPU ARGB remains private until complete commit");
                for(int p:job.cf)check(p==0x2468ace0,"failed GPU confidence remains private until complete commit");
                check(!GpuQualification1961.exactRejected(admitted.key),"transport or cancellation cannot forge an exact mismatch");
            }finally{end(model);GpuQualification1961.captureChanged();drain();}
        }
    }
    public static void main(String[] args)throws Exception{
        check(GpuNoise1960.available()&&GpuNoise1960.warmEnvironment1973(),"actual JNI context loaded");
        GpuQualification1961.initialize(new Context());Bootstrap1979Test.Data d=Bootstrap1979Test.load(args[0]).cut(20,36);
        forecast();coldIdleHot(d);uncommittedFailures(d);waitAndSpeed(d);
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"final ownership drained");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cold_idle_persist_hot1981_actual_jni\":true,\"wait_transfer_inclusive1981_verified\":true,\"resident_forecast_actual_capacity1981_verified\":true,\"hot_failure_cancel_private1981_verified\":true,\"complete_argb_confidence_exact\":true,\"physical_android_tested\":false}");
    }
}
