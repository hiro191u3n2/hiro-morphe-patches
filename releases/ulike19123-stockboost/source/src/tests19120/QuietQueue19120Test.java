package com.hiro.ulike;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual production admission/persistence engine; only Android/GPU peers are fixtures. */
public final class QuietQueue19120Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Field field(String name)throws Exception{Field f=GpuQualification1961.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static final class Probe implements GpuQualification1961.Probe {
        final AtomicInteger run=new AtomicInteger(),close=new AtomicInteger();
        public void run(GpuQualification1961.Cancellation cancellation){run.incrementAndGet();}
        public void close(){close.incrementAndGet();}
    }
    static void assertClosed(Probe p){check(p.run.get()==0&&p.close.get()==1,"declined ownership closes exactly once without executing");}
    public static void main(String[] args)throws Exception{
        Context app=new Context();GpuQualification1961.initialize(app);
        SaveQueue1935.idle=true;GpuNoise1960.busy=false;WholeRoute1953.retained=0;
        field("lastCapture").setLong(null,System.nanoTime()-3000000000L);
        check(GpuQualification1961.maySchedule("new-candidate"),"legacy proof eligibility remains independent from automatic admission");
        check(!GpuQualification1961.canQueue("new-candidate",4096),"quiet startup must not admit an unqualified background job");
        check(GpuQualification1961.canQueue1983("new-candidate",4096).reason==25,"disabled admission has an explicit diagnostic reason");
        Probe p=new Probe();check(!GpuQualification1961.schedule("new-candidate",4096,p),"schedule remains declined after preflight");assertClosed(p);
        Probe q=new Probe();GpuQualification1961.QueueDecision1983 decision=GpuQualification1961.schedule1983("other-candidate",4096,q);
        check(!decision.accepted&&decision.reason==25,"structured scheduling uses the same disabled policy");assertClosed(q);
        for(int revision=84;revision<=85;revision++){
            GpuQualification1961.Reservation1984 r=revision==84?GpuQualification1961.reserve1984("resident-new",4096):GpuQualification1961.reserve1985("resident-new",4096);
            check(!r.accepted()&&r.decision.reason==25&&!r.current()&&!r.begin1985(),"reservation declines before any caller snapshot");
            Probe owned=new Probe();check(!r.commit(owned).accepted,"declined reservation cannot commit");r.close();r.close();assertClosed(owned);
        }
        check(GpuQualification1961.retainedBytes()==0,"no snapshots or reservations remain");
        check(field("worker").get(null)==null,"no automatic proof worker is created even when all idle conditions hold");
        // Simulate persisted evidence produced by a prior released version, not new runtime proof.
        GpuQualification1961.qualified("prior-certified",100,80,0);
        check(GpuQualification1961.restore("prior-certified")!=null,"prior complete speed proof still restores");
        check(GpuQualification1961.maySchedule("prior-certified"),"foreground certified route retains legacy eligibility");
        synchronized(field("LOCK").get(null)){((Map<?,?>)field("RECORDS").get(null)).clear();}
        check(GpuQualification1961.restore("prior-certified")!=null,"persistent certified route survives memory-cache eviction");
        GpuQualification1961.qualified("too-slow",100,96,0);
        check(GpuQualification1961.restore("too-slow")==null,"insufficient speed proof remains rejected");
        GpuQualification1961.rejectExact("pixel-mismatch");
        GpuQualification1961.qualified("pixel-mismatch",100,80,0);
        check(GpuQualification1961.exactRejected("pixel-mismatch")&&!GpuQualification1961.maySchedule("pixel-mismatch")&&GpuQualification1961.restore("pixel-mismatch")==null,"exact mismatch remains fail closed");
        GpuQualification1961.captureChanged();GpuQualification1961.wake();
        check(GpuQualification1961.retainedBytes()==0&&field("worker").get(null)==null,"capture and idle notifications do not start a proof worker");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"suite\":\"quiet_queue\"}");
    }
}
