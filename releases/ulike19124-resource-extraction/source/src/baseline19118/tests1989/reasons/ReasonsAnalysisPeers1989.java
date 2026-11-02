package com.hiro.ulike;

import java.util.HashMap;
import java.util.Map;

/** Controlled reservation and transport peers. The production analysis wrapper,
 * spatial estimator and regional estimator are compiled without substitution. */
final class GpuQualification1961 {
    interface Probe {void run(Cancellation cancellation);void close();}
    static final class Cancellation {boolean stopped;boolean cancelled(){return stopped;}}
    static final class Record {
        final long cpuNanos,gpuNanos;final int variant;
        Record(long cpu,long gpu,int variant){cpuNanos=cpu;gpuNanos=gpu;this.variant=variant;}
    }
    static final Map<String,Record> records=new HashMap<String,Record>();
    static Probe pending;static boolean background,current=true,allow=true,beginAllowed=true,throwReserve;
    static long retained;static int reservations,commits,closes,speedFailures,exactFailures,qualified;
    static int reserveReason=0,commitReason=1,permissionCalls;static boolean nullDecision,nullCommit;
    static long retryRemaining=1234567L,decisionHeld=9876543L;static int retryCount=2,queuedCount=3,runningCount=1;
    static final class QueueDecision1983 {
        final boolean accepted;final int reason,retries,queuedJobs,runningJobs;
        final long requestedBytes,retainedBytes,retryRemainingNanos;
        QueueDecision1983(int why,long bytes){reason=why;accepted=why==0||why==1;requestedBytes=bytes;
            retainedBytes=decisionHeld;retryRemainingNanos=retryRemaining;retries=retryCount;queuedJobs=queuedCount;runningJobs=runningCount;}
    }
    static final class Reservation1984 implements AutoCloseable {
        final QueueDecision1983 decision;
        final boolean accepted;final long bytes;boolean held,begun;
        Reservation1984(boolean accepted,long bytes){decision=nullDecision?null:new QueueDecision1983(reserveReason,bytes);this.accepted=accepted;this.bytes=bytes;held=accepted;if(held)retained+=bytes;}
        boolean accepted(){return accepted;}
        boolean current(){return held&&current&&!Thread.currentThread().isInterrupted();}
        boolean begin1985(){if(!beginAllowed||!current())return false;begun=true;return true;}
        QueueDecision1983 commit(Probe probe){commits++;if(!begun||!current()||commitReason!=1){probe.close();close();return nullCommit?null:new QueueDecision1983(commitReason,bytes);}pending=probe;held=false;return nullCommit?null:new QueueDecision1983(1,bytes);}
        public void close(){closes++;if(held){retained-=bytes;held=false;}}
    }
    static Reservation1984 reserve1985(String key,long bytes){reservations++;if(throwReserve)throw new OutOfMemoryError("fixture reservation allocation");return new Reservation1984(allow&&reserveReason==0,bytes);}
    static boolean maySchedule(String key){permissionCalls++;return allow;}
    static boolean background(){return background;}
    static boolean canQueue(String key,long bytes){permissionCalls++;return allow;}
    static boolean schedule(String key,long bytes,Probe probe){if(!allow){probe.close();return false;}pending=probe;retained+=bytes;return true;}
    static Record restore(String key){return records.get(key);}
    static void rejectExact(String key){exactFailures++;records.remove(key);}
    static void rejectSpeed(String key){speedFailures++;records.remove(key);}
    static void qualified(String key,long cpu,long gpu,int variant){
        if(cpu<=0||gpu<=0||gpu>cpu-cpu/20)throw new AssertionError("production speed gate bypassed");
        qualified++;records.put(key,new Record(cpu,gpu,variant));
    }
    static void reset(){clear();records.clear();background=false;current=true;allow=true;beginAllowed=true;throwReserve=false;reservations=commits=closes=speedFailures=exactFailures=qualified=permissionCalls=0;reserveReason=0;commitReason=1;nullDecision=nullCommit=false;retryRemaining=1234567L;decisionHeld=9876543L;retryCount=2;queuedCount=3;runningCount=1;}
    static void clear(){if(pending!=null){pending.close();pending=null;}retained=0;}
}

final class GpuNoise1960 {
    static final int ANALYSIS=2,ANALYSIS1961=4;
    static final long MAX_BYTES=512L*1024*1024;
    static boolean available=true,busy,budget=true,supported=true,throwBudget;
    static int opens,executions;static long executionDelay;static int[][] result;static Runnable duringExecute;
    static boolean available(){return available;}
    static boolean sessionBusy(){return busy;}
    static boolean workspaceFits(long bytes){if(throwBudget)throw new OutOfMemoryError("fixture optional allocation");return budget&&bytes<=MAX_BYTES;}
    static boolean supports(int shader){return supported;}
    static int variant(int shader,int variant){return shader+variant*9;}
    static int strongProgram(int mode,int variant){return 27+mode+variant*4;}
    static Session open(){opens++;return new Session();}
    static final class Session {
        int[][] execute(Batch batch,int[] slots,int[] lengths){executions++;if(duringExecute!=null)duringExecute.run();AnalysisCapture1987Test.pause(executionDelay);return result;}
        boolean executeInto(Batch batch,int slot,int length,int[] dst,int at){return false;}
        boolean run(Batch batch){return true;}
        void close(){}
    }
    static final class Batch {
        Batch upload(int slot,int[] values){return this;}
        Batch upload(int slot,float[] values){return this;}
        Batch uploadDirect(int slot,int[] values){return this;}
        Batch allocate(int slot,long bytes){return this;}
        Batch dispatch(int shader,int[] bindings,int[] u,float[] f,int invocations){return this;}
    }
    static void reset(){available=budget=supported=true;busy=throwBudget=false;opens=executions=0;executionDelay=0;result=null;duringExecute=null;}
}
