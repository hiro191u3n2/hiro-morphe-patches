package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual scheduler decisions, ownership and races with controlled idle gates. */
public final class QueueDecision1983Test {
    static final long M=1024L*1024;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    interface Ready {boolean get();}
    static void await(Ready value,String why)throws Exception {
        long until=System.nanoTime()+4000000000L;
        while(!value.get()){if(System.nanoTime()>until)throw new AssertionError("timeout: "+why);Thread.sleep(2);}
        check(true,why);
    }
    static Field field(String name)throws Exception {
        Field value=GpuQualification1961.class.getDeclaredField(name);value.setAccessible(true);return value;
    }
    static Object failure(String key)throws Exception {
        Method environment=GpuQualification1961.class.getDeclaredMethod("environment");environment.setAccessible(true);
        Method failure=GpuQualification1961.class.getDeclaredMethod("failure",String.class,String.class);failure.setAccessible(true);
        synchronized(field("LOCK").get(null)){return failure.invoke(null,key,(String)environment.invoke(null));}
    }
    static void retry(String key,int count,long after)throws Exception {
        synchronized(field("LOCK").get(null)){
            Object failure=failure(key);check(failure!=null,"existing failure to age");
            Field retries=failure.getClass().getDeclaredField("retries");retries.setAccessible(true);retries.setInt(failure,count);
            Field retryAfter=failure.getClass().getDeclaredField("retryAfter");retryAfter.setAccessible(true);retryAfter.setLong(failure,after);
        }
    }
    static void reset()throws Exception {
        Thread.interrupted();SaveQueue1935.idle=false;GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        await(()->GpuQualification1961.retainedBytes()==0,"prior snapshots released");
        synchronized(field("LOCK").get(null)){
            ((Map<?,?>)field("RECORDS").get(null)).clear();((Map<?,?>)field("FAILURES").get(null)).clear();
            field("lastLegacy1981").setBoolean(null,false);
        }
        GpuNoise1960.busy=false;GpuNoise1960.fingerprintFault1983=0;
        GpuQualification1961.initialize(new Context());WholeRoute1953.retained=0;
    }
    static class Probe implements GpuQualification1961.Probe {
        final AtomicInteger runs=new AtomicInteger(),closes=new AtomicInteger();
        public void run(GpuQualification1961.Cancellation cancellation){runs.incrementAndGet();}
        public void close(){closes.incrementAndGet();}
    }
    static GpuQualification1961.QueueDecision1983 reason(String key,long bytes,int expected) {
        GpuQualification1961.QueueDecision1983 value=GpuQualification1961.canQueue1983(key,bytes);
        check(value.reason==expected,"reason "+expected+" observed "+value.reason);
        check(value.accepted==(expected==0||expected==1),"decision boolean and reason agree");return value;
    }
    static void invalidAndEnvironment()throws Exception {
        reset();reason(null,1,2);reason("",1,2);reason("zero",0,3);reason("negative",-1,3);reason("oversized",97*M,4);
        String base=(String)field("base").get(null);field("base").set(null,"");
        try{reason("environment",1,7);check(!GpuQualification1961.maySchedule("environment"),"missing environment still disallows old predicate");}
        finally{field("base").set(null,base);}
        Thread.currentThread().interrupt();try{reason("interrupted",1,6);check(Thread.currentThread().isInterrupted(),"classification preserves cancellation signal");}finally{Thread.interrupted();}
        GpuQualification1961.QueueDecision1983 absent=GpuQualification1961.schedule1983("no-probe",1,null);
        check(!absent.accepted&&absent.reason==18,"null probe is not scheduled");
        for(Field f:GpuQualification1961.QueueDecision1983.class.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers()))
            check(f.getType().isPrimitive()&&Modifier.isFinal(f.getModifiers()),"decision retains only final scalar fields: "+f.getName());
    }
    static void queuedRaceAndSnapshot()throws Exception {
        reset();GpuQualification1961.QueueDecision1983 ready=reason("same",11,0);
        check(ready.queuedJobs==0&&ready.retainedBytes==0,"preflight does not claim a reservation");
        Probe first=new Probe();GpuQualification1961.QueueDecision1983 admitted=GpuQualification1961.schedule1983("same",11,first);
        check(admitted.accepted&&admitted.reason==1&&admitted.queuedJobs==1&&admitted.retainedBytes==11,"accepted snapshot includes new ownership");
        GpuQualification1961.QueueDecision1983 duplicate=reason("same",11,13);
        check(duplicate.queuedJobs==1&&duplicate.runningJobs==0&&duplicate.retainedBytes==11,"same-key reservation observed atomically");
        Probe raced=new Probe();GpuQualification1961.QueueDecision1983 finalDecision=GpuQualification1961.schedule1983("same",11,raced);
        check(!finalDecision.accepted&&finalDecision.reason==13,"final schedule reports race after successful preflight");
        check(raced.closes.get()==1&&raced.runs.get()==0,"racing declined probe closed exactly once");
        for(int i=0;i<10;i++){GpuQualification1961.status1967();check(finalDecision.reason==13,"reading result never reevaluates queue");}
        check(first.closes.get()==0&&GpuQualification1961.retainedBytes()==11,"diagnostic reads do not reclaim reserved image");
        GpuQualification1961.captureChanged();
        check(first.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"fresh capture releases queued owner");
        check(finalDecision.queuedJobs==1&&finalDecision.retainedBytes==11&&ready.queuedJobs==0,"later live changes do not rewrite prior decisions");
        reason("same",11,0);
    }
    static void qualificationAndRetry()throws Exception {
        reset();reason("qualified-race",1,0);GpuQualification1961.qualified("qualified-race",100,95,0);
        reason("qualified-race",1,11);Probe qualified=new Probe();
        check(GpuQualification1961.schedule1983("qualified-race",1,qualified).reason==11&&qualified.closes.get()==1,"new certificate defeats a stale preflight without requeue");
        check(GpuQualification1961.maySchedule("qualified-race")&&GpuQualification1961.restore("qualified-race")!=null,"foreground certificate remains eligible");
        GpuQualification1961.rejectExact("exact");reason("exact",1,8);
        check(!GpuQualification1961.canQueue("exact",1)&&!GpuQualification1961.maySchedule("exact"),"exact rejection preserved through boolean APIs");
        GpuQualification1961.rejectSpeed("speed");GpuQualification1961.QueueDecision1983 cooldown=reason("speed",1,9);
        check(cooldown.retryRemainingNanos>0&&cooldown.retryRemainingNanos<=30000000000L&&cooldown.retries==0,"remaining cooldown belongs to rejecting decision");
        retry("speed",0,System.nanoTime()-1);reason("speed",1,0);
        check(cooldown.reason==9&&cooldown.retryRemainingNanos>0,"old cooldown result never becomes an eligible result");
        retry("speed",3,System.nanoTime()-1);GpuQualification1961.QueueDecision1983 limit=reason("speed",1,10);
        check(limit.retries==3&&limit.retryRemainingNanos==0,"exhausted retry is distinct from elapsed cooldown");
        String preferred="strong-gx1978-policy-direct-ieee-v1:3:test";
        GpuQualification1961.rejectSpeed(preferred);retry(preferred,3,System.nanoTime()+30000000000L);reason(preferred,1,0);
        GpuQualification1961.rejectExact(preferred);reason(preferred,1,8);
        check(!GpuQualification1961.maySchedule(preferred),"preferred policy cannot bypass exact rejection");
    }
    static void capacities()throws Exception {
        reset();for(int i=0;i<7;i++)check(GpuQualification1961.schedule("early-"+i,1,new Probe()),"early slot "+i);
        GpuQualification1961.QueueDecision1983 reserved=reason("strong-resident:3:reserved",1,14);
        check(reserved.queuedJobs==7&&reserved.retainedBytes==7,"dedicated Strong reservation distinguished from full eight jobs");
        check(GpuQualification1961.schedule("strong-test:3:eighth",1,new Probe()),"Strong still takes reserved slot");
        reset();for(int i=0;i<8;i++)check(GpuQualification1961.schedule("strong-test:3:job-"+i,1,new Probe()),"all Strong slot "+i);
        reason("strong-test:3:ninth",1,15);
        reset();check(GpuQualification1961.schedule("strong-test:3:large",60*M,new Probe()),"large Strong owner");
        check(GpuQualification1961.schedule("strong-test:3:tail",36*M,new Probe()),"exact 96 MiB combined bound");
        GpuQualification1961.QueueDecision1983 memory=reason("strong-test:3:overflow",1,16);
        check(memory.queuedJobs==2&&memory.retainedBytes==96*M&&memory.requestedBytes==1,"byte exhaustion is not job exhaustion");
        reset();for(int i=0;i<8;i++)check(GpuQualification1961.schedule("strong-test:3:both-"+i,12*M,new Probe()),"combined bound slot "+i);
        reason("strong-test:3:both-over",1,17);
        reset();Probe large=new Probe(),small=new Probe();
        check(GpuQualification1961.schedule("resident-large",60*M,large)&&GpuQualification1961.schedule("resident-small",20*M,small),"reclaim candidates admitted");
        GpuQualification1961.QueueDecision1983 reclaimed=reason("strong-test:3:reclaim",64*M,0);
        check(large.closes.get()==1&&small.closes.get()==0,"original minimal family-priority reclamation preserved");
        check(reclaimed.retainedBytes==20*M&&reclaimed.queuedJobs==1,"decision snapshot taken after its own allowed reclamation");
    }
    static void runningAndBackground()throws Exception {
        reset();final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        final AtomicReference<GpuQualification1961.QueueDecision1983> nested=new AtomicReference<GpuQualification1961.QueueDecision1983>();
        final Probe probe=new Probe(){public void run(GpuQualification1961.Cancellation cancellation){
            runs.incrementAndGet();nested.set(GpuQualification1961.canQueue1983("nested",1));entered.countDown();
            for(;;)try{release.await();break;}catch(InterruptedException expected){}
        }};
        check(GpuQualification1961.schedule("active",23,probe),"blocking probe admitted");
        SaveQueue1935.idle=true;field("lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
        check(entered.await(4,TimeUnit.SECONDS),"blocking probe began");
        try {
            GpuQualification1961.QueueDecision1983 running=reason("active",23,12);
            check(running.runningJobs==1&&running.queuedJobs==0&&running.retainedBytes==23,"active same key distinguished from queued duplicate");
            check(nested.get()!=null&&nested.get().reason==5&&!nested.get().accepted,"nested proof blocked without recursive scheduling");
            GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
            check(probe.closes.get()==0&&GpuQualification1961.retainedBytes()==23,"active ownership retained through cancellation until worker exits");
        } finally{release.countDown();SaveQueue1935.idle=false;}
        await(()->probe.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"active owner released once");
    }
    static void unavailableOwnership()throws Exception {
        reset();for(int fault=1;fault<=2;fault++){
            Probe p=new Probe();GpuNoise1960.fingerprintFault1983=fault;
            GpuQualification1961.QueueDecision1983 d;
            try{d=GpuQualification1961.schedule1983("unavailable",1,p);}finally{GpuNoise1960.fingerprintFault1983=0;}
            check(!d.accepted&&d.reason==(fault==1?19:20),"scheduler exception retains distinct terminal cause");
            check(d.queuedJobs== -1&&d.retainedBytes== -1,"exception fallback reports unobserved scalars honestly");
            check(p.closes.get()==1&&p.runs.get()==0&&GpuQualification1961.retainedBytes()==0,"failed scheduling retains original close ownership");
        }
    }
    public static void main(String[] args)throws Exception {
        invalidAndEnvironment();queuedRaceAndSnapshot();qualificationAndRetry();capacities();runningAndBackground();unavailableOwnership();reset();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"qualification_atomic_reasons1983\":true,\"qualification_queue_ownership1983\":true,\"qualification_scalar_snapshots1983\":true}");
    }
}
