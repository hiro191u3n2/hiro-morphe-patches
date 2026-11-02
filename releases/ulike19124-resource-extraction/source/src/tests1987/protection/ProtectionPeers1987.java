package com.hiro.ulike;

import java.util.IdentityHashMap;

/** Explicit host adapters: GPU transport, qualification state and array owner.
 * Pixel plans, policy arithmetic, compact encoding and immutable masks remain
 * production classes. No adapter changes the policy under test. */
final class ProtectionPeers1987 {
    static int assertions;
    static int generation;
    static void reset() {
        generation++;
        GpuQualification1961.record=null;
        GpuQualification1961.failRestore=false;
        GpuQualification1961.allowed=true;
        GpuQualification1961.inBackground=false;
        GpuQualification1961.held=0;
        GpuQualification1961.queued=0;
        GpuQualification1961.probe=null;
        GpuNoise1960.memory=true;
        GpuNoise1960.busy=false;
        GpuNoise1960.enabled=true;
        GpuNoise1960.output=null;
        GpuNoise1960.executions=0;
        GpuNoise1960.executeOom=false;
        GpuNoise1960.budgetCalls=0;
        GpuNoise1960.interruptBudget=0;
        SpeedWorkers1935.borrowed=0;
        SpeedWorkers1935.failBorrow=0;
        if(!SpeedWorkers1935.owned.isEmpty())throw new AssertionError("Leaked array lease");
        Thread.interrupted();
    }
    static void check(boolean value,String message) {
        assertions++;
        if(!value)throw new AssertionError(message);
    }
}

final class GpuQualification1961 {
    static final class Record {
        final long cpuNanos,gpuNanos; final int variant;
        Record(long cpu,long gpu,int v){cpuNanos=cpu;gpuNanos=gpu;variant=v;}
    }
    interface Cancellation { boolean cancelled(); }
    interface Probe { void run(Cancellation cancellation); void close(); }
    static Record record;
    static boolean allowed=true,inBackground,failRestore;
    static long held;
    static int queued;
    static Probe probe;
    static boolean maySchedule(String key){return allowed;}
    static boolean exactRejected(String key){return !allowed;}
    static Record restore(String key){
        if(failRestore&&key.startsWith("protection1962:"))throw new IllegalStateException("Injected unavailable history");
        return key.startsWith("protection1962:")?record:null;
    }
    static boolean background(){return inBackground;}
    static long retainedBytes(){return held;}
    static boolean canQueue(String key,long bytes){return allowed&&!inBackground&&held==0;}
    static boolean schedule(String key,long bytes,Probe value) {
        if(!canQueue(key,bytes)){value.close();return false;}
        if(!key.startsWith("protection1962:")){value.close();return false;}
        queued++;probe=value;return true;
    }
    static void rejectSpeed(String key){}
    static void rejectExact(String key){}
    static void qualified(String key,long cpu,long gpu,int variant){}
}

final class GpuNoise1960 {
    static final long MAX_BYTES=256L*1024*1024;
    static boolean memory=true,busy,enabled=true,executeOom;
    static int[] output;
    static int executions,budgetCalls,interruptBudget;
    static String fingerprint(){return "protection-host-"+ProtectionPeers1987.generation;}
    static boolean sessionBusy(){return busy;}
    static boolean supports(int shader){return enabled;}
    static boolean workspaceFits(long bytes) {
        budgetCalls++;
        if(interruptBudget>0&&budgetCalls==interruptBudget)Thread.currentThread().interrupt();
        return memory;
    }
    static Session open(){return new Session();}
    static final class Batch {
        Batch upload(int slot,int[] values){return this;}
        Batch upload(int slot,float[] values){return this;}
        Batch allocate(int slot,long bytes){return this;}
        Batch dispatch(int shader,int[] slots,int[] u,float[] f,int count){return this;}
    }
    static final class Session {
        int[][] execute(Batch batch,int[] slots,int[] counts) {
            executions++;
            if(executeOom)throw new OutOfMemoryError("Injected GPU output allocation");
            if(output==null||output.length!=counts[0])throw new AssertionError("Unexpected output descriptor");
            return new int[][]{output.clone()};
        }
        void close(){}
    }
}

final class SpeedWorkers1935 {
    static final IdentityHashMap<int[],Boolean> owned=new IdentityHashMap<int[],Boolean>();
    static int borrowed,failBorrow;
    static int[] borrowInts(int length) {
        if(++borrowed==failBorrow)throw new OutOfMemoryError("Injected Java policy allocation");
        int[] value=new int[length];owned.put(value,Boolean.TRUE);return value;
    }
    static void release(int[] value){if(value!=null)owned.remove(value);}
    static void trim(){}
    static int maxWorkers(){return 1;}
    static int availableWorkers1944(){return 1;}
    static long nativeRetainedBytes1956(){return 0;}
    static void run(Runnable[] work){for(Runnable item:work)item.run();}
}
