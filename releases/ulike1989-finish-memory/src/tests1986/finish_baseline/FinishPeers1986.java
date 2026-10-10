package com.hiro.ulike;

/** Runtime-only peers, compiled after the real production closure. */
final class GpuNoise1960 {
    static String fingerprint(){return "finish1986-controlled-host";}
    static boolean workspaceFits(long bytes){return FinishControl1986.workspace&&bytes>=0&&bytes<=512L*1024*1024;}
}
final class GpuQualification1961 {
    interface Cancellation {boolean cancelled();}
    static final class Record {
        final long cpuNanos,gpuNanos;final int variant;
        Record(long cpu,long gpu,int variant){cpuNanos=cpu;gpuNanos=gpu;this.variant=variant;}
    }
    static boolean cancelled(){return FinishControl1986.cancelled();}
    static boolean exactRejected(String key){return FinishControl1986.exactRejections.contains(key);}
    static void rejectExact(String key){FinishControl1986.exactRejections.add(key);FinishControl1986.proofs.remove(key);}
    static void rejectSpeed(String key){FinishControl1986.speedRejections.add(key);FinishControl1986.proofs.remove(key);}
    static Record restore(String key){
        if(exactRejected(key))return null;
        FinishControl1986.Proof p=FinishControl1986.proofs.get(key);return p==null?null:new Record(p.cpu,p.gpu,p.variant);
    }
    static void qualified(String key,long cpu,long gpu,int variant){
        if(cancelled()||Thread.currentThread().isInterrupted()||exactRejected(key)||cpu<=0||gpu<=0||gpu>cpu-cpu/20||variant<0||variant>2)return;
        FinishControl1986.proofs.put(key,new FinishControl1986.Proof(cpu,gpu,variant));FinishControl1986.speedRejections.remove(key);
    }
    public static void finishCandidate1986(int candidate,int reason,int exact,int speed,int reference,long cpu,long legacy,long gpu){
        FinishControl1986.note(candidate,reason,exact,speed,reference,cpu,legacy,gpu);
    }
}
