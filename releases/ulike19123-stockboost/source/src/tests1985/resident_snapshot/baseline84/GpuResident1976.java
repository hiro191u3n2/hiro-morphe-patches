package com.hiro.ulike;

import android.graphics.Bitmap;

/** Whole Strong-to-finish admission. One immutable queued source is sufficient;
 * half-resolution input is reconstructed after save. A missing ordinary finish
 * proof is resolved first in the same idle job. Every candidate still requires
 * two complete CPU comparisons and a real existing-route timing advantage. */
final class GpuResident1976 {
    private GpuResident1976() {}
    private static final ThreadLocal<Boolean> BENCHMARK=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> CPU=new ThreadLocal<Boolean>();
    private static final ThreadLocal<GpuQualification1961.Cancellation> CANCELLATION=new ThreadLocal<GpuQualification1961.Cancellation>();
    static GpuQualification1961.Cancellation cancellation(){return CANCELLATION.get();}
    static boolean benchmarking(){return Boolean.TRUE.equals(BENCHMARK.get());}
    static boolean cpuOracle(){return Boolean.TRUE.equals(CPU.get());}
    private static String key(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,
            int rotation,int width,int height,QualityPixels1932.Plan output,boolean identity) {
        return "strong-resident1978:3:v1:"+GpuNoise1960.fingerprint()+":"+source.getWidth()+":"+source.getHeight()+":"+
            level+":"+shadows+":"+rotation+":"+width+":"+height+":"+plan.beautyQ8+":"+plan.shadowBudgetQ8+":"+
            output.sharpGainQ8+":"+output.sharpFloorQ8+":"+output.sharpLimit+":"+output.texturePriority+":"+output.haloSuppression+":"+
            Float.floatToRawIntBits(output.sourceSigma)+":"+Float.floatToRawIntBits(output.outputScale)+":"+
            (output.faceRegions!=null)+":"+(output.localNoise!=null)+":"+identity;
    }
    private static void note(ProcessingTiming1947.Trace trace,Bitmap source,int width,int height,int rotation,
            boolean identity,int halfLength,long retained,int reason) {
        try {if(source!=null&&!source.isRecycled())ProcessingTiming1947.resident1978(trace,source.getWidth(),source.getHeight(),width,height,
            rotation,identity,halfLength,retained,reason);}catch(Throwable optional){}
    }
    static Bitmap finish(final Bitmap source,final QualityPixels1932.Plan plan,final int level,final boolean shadows,
            final int[] half,final int rotation,final int width,final int height,final QualityPixels1932.Plan output,
            ProcessingTiming1947.Trace trace) {
        // The existing API includes reduce-first images whose remaining geometry
        // is a no-op, but whose evidence must still be refreshed after moire.
        return finish1978(source,plan,level,shadows,half,rotation,width,height,output,false,trace);
    }
    static Bitmap finish1978(final Bitmap source,final QualityPixels1932.Plan plan,final int level,final boolean shadows,
            final int[] half,final int rotation,final int width,final int height,final QualityPixels1932.Plan output,
            final boolean identity,ProcessingTiming1947.Trace trace) {
        final int halfLength=half==null?0:half.length;
        note(trace,source,width,height,rotation,identity,halfLength,0,0);
        int terminal1982=13;long retained1982=0;
        GpuQualification1961.QueueDecision1983 queueDecision1983=null;
        try {
        if(source==null||source.isRecycled()||source.getConfig()!=Bitmap.Config.ARGB_8888||level<=0||output==null||
                identity&&!ResidentProof1978.identity(source,rotation,width,height)||
                !detachedPlan1976(plan)||!detachedPlan1976(output)||
                !GpuChain1961.eligibleResident1976(source,rotation,width,height,output)||output.sharpGainQ8<=0) {
            terminal1982=3;return null;
        }
        // Preserve the original short-circuit order and every capability query.
        if(GpuQualification1961.background()){terminal1982=9;return null;}
        if(GpuNoise1960.sessionBusy()){terminal1982=8;return null;}
        if(!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||!identity&&!GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                !GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961)){terminal1982=3;return null;}
        final QualityPixels1932.Plan frozenPlan=plan,frozenOutput=output;
        final String base=key(source,frozenPlan,level,shadows,rotation,width,height,frozenOutput,identity);
        final String initialBaseline=GpuChain1961.residentBaseline1978(source,rotation,width,height,frozenOutput,identity);
        final String qualifiedKey=initialBaseline==null?null:base+":"+initialBaseline;
        if(qualifiedKey!=null&&GpuQualification1961.exactRejected(qualifiedKey)) {
            terminal1982=4;return null;
        }
        GpuQualification1961.Record proof=qualifiedKey==null?null:GpuQualification1961.restore(qualifiedKey);
        if(proof!=null) {
            final long started=System.nanoTime();
            GpuChain1961.Preflight1981 preflight=GpuChain1961.preflightResident1981(source,rotation,width,height,halfLength);
            if(preflight==null){terminal1982=5;return null;}
            preflight.deadline1981=started+Math.min(Long.MAX_VALUE/4,proof.cpuNanos-proof.cpuNanos/20);
            try {
                Bitmap result=QualityPipeline1932.strongFinish1978(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false,identity);
                if(result==null){terminal1982=14;return null;}
                if(!win(proof.cpuNanos,System.nanoTime()-started))GpuQualification1961.rejectSpeed(qualifiedKey);
                ProcessingTiming1947.detailRoute1976(trace,4);
                terminal1982=1;return result;
            } catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
            catch(RuntimeException unavailable){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("resident interrupted");return null;}
            catch(LinkageError unavailable){return null;}
            catch(OutOfMemoryError unavailable){return null;}
            finally{preflight.close();}
        }
        GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.RESIDENT);
        final String scheduledKey=qualifiedKey==null?base+":finish-dependency":qualifiedKey;
        final long retained=ResidentProof1978.retainedBytes(source,plan,output);retained1982=retained;
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained)) {
            terminal1982=5;return null;
        }
        GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryCopy(retained,GpuSnapshotBudget1981.RESIDENT);
        if(copy==null){terminal1982=Thread.currentThread().isInterrupted()?11:10;return null;}
        Bitmap snapshot=null;
        GpuQualification1961.Reservation1984 reservation1984=null;
        try {
            // Reserve both a job slot and the real retained bytes before the
            // snapshot is allocated. The ticket owns that reservation through
            // cancellation until the copying thread releases it.
            reservation1984=GpuQualification1961.reserve1984(scheduledKey,retained);
            GpuQualification1961.QueueDecision1983 admission=reservation1984.decision;
            if(!admission.accepted) {
                queueDecision1983=admission;terminal1982=6;return null;
            }
            if(!reservation1984.current()||!copy.begin()){terminal1982=11;return null;}
            terminal1982=12;
            snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return null;
            terminal1982=13;
            final Bitmap owned=snapshot;
            final long snapshotRetained=ResidentProof1978.retainedBytes(owned,plan,output);retained1982=snapshotRetained;
            if(snapshotRetained>retained||snapshotRetained>96L*1024*1024) {
                terminal1982=5;return null;
            }
            if(!copy.current()||!reservation1984.current()){terminal1982=11;return null;}
            queueDecision1983=reservation1984.commit(new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    CANCELLATION.set(cancellation);
                    try {
                        if(cancellation.cancelled())return;
                        if(!GpuChain1961.opaqueResident1976(owned)){outcome1984("unsupported");return;}
                        long halfBytes=4L*((owned.getWidth()+1L)/2)*((owned.getHeight()+1L)/2)+8L*owned.getWidth();
                        if(!GpuNoise1960.workspaceFits(halfBytes)){outcome1984("memory_budget");return;}
                        final int[] frozenHalf=ResidentProof1978.half(owned,cancellation);
                        String baselineKey=initialBaseline;
                        if(baselineKey==null) {
                            progress1984("resident_baseline",0,1);
                            CPU.set(Boolean.TRUE);
                            try {QualityPipeline1932.qualifyResidentBaseline1978(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,identity,cancellation);}
                            finally {CPU.remove();}
                            if(cancellation.cancelled())return;
                            baselineKey=GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity);
                            if(baselineKey==null){GpuQualification1961.rejectSpeed(scheduledKey);outcome1984("baseline_unavailable");return;}
                            progress1984("resident_baseline",1,1);
                        }
                        final String resultKey=base+":"+baselineKey;
                        if(GpuQualification1961.exactRejected(resultKey)){outcome1984("quality_rejected");return;}
                        long baseline=Long.MAX_VALUE,candidateWorst=0;
                        for(int trial=0;trial<2;trial++) {
                            if(cancellation.cancelled())return;
                            if(!baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))){outcome1984("reference_unstable");return;}
                            progress1984("resident_compare",trial,2);
                            Bitmap oracle=null;
                            try {
                                CPU.set(Boolean.TRUE);
                                try {oracle=QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,null,rotation,width,height,frozenOutput,false,true,identity);}
                                finally {CPU.remove();}
                                if(cancellation.cancelled())return;
                                if(oracle==null){outcome1984("baseline_unavailable");return;}
                                for(int candidate=0;candidate<2;candidate++) {
                                    BENCHMARK.set(Boolean.TRUE);int comparison;
                                    try {comparison=QualityPipeline1932.compareStrongFinish1978(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,candidate==1,identity,oracle,cancellation);}
                                    finally {BENCHMARK.remove();}
                                    if(cancellation.cancelled())return;
                                    if(comparison==ResidentProof1978.MISMATCH){GpuQualification1961.rejectExact(resultKey);outcome1984("quality_mismatch");return;}
                                    if(comparison!=ResidentProof1978.EXACT){outcome1984("comparison_incomplete");return;}
                                }
                            } finally {CPU.remove();BENCHMARK.remove();if(oracle!=null)oracle.recycle();}
                            progress1984("resident_compare",trial+1,2);
                            // Real foreground-shaped timing includes destination
                            // allocation/Bitmap writes. No CPU reference is held
                            // during this phase and proof-only row reads are absent.
                            progress1984("resident_speed",trial,2);
                            for(int candidate=0;candidate<2;candidate++) {
                                if(cancellation.cancelled())return;
                                Bitmap result=null;long elapsed;
                                Bitmap ordinaryInput=candidate==0?owned.copy(Bitmap.Config.ARGB_8888,true):null;
                                if(candidate==0&&ordinaryInput==null){outcome1984("baseline_unavailable");return;}
                                BENCHMARK.set(Boolean.TRUE);long started=System.nanoTime();
                                try {
                                    result=candidate==0?QualityPipeline1932.strongFinishOwned1978(ordinaryInput,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,false,identity)
                                        :QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,true,false,identity);
                                    elapsed=System.nanoTime()-started;
                                    if(cancellation.cancelled())return;
                                    if(result==null){outcome1984(candidate==0?"baseline_unavailable":"execution_unavailable");return;}
                                    if(candidate==0)baseline=Math.min(baseline,elapsed);else candidateWorst=Math.max(candidateWorst,elapsed);
                                } finally {BENCHMARK.remove();if(result!=null)result.recycle();if(ordinaryInput!=null&&!ordinaryInput.isRecycled())ordinaryInput.recycle();}
                            }
                            progress1984("resident_speed",trial+1,2);
                        }
                        if(cancellation.cancelled())return;
                        timings1984(baseline,candidateWorst);
                        if(win(baseline,candidateWorst))GpuQualification1961.qualified(resultKey,baseline,candidateWorst,0);
                        else {GpuQualification1961.rejectSpeed(resultKey);outcome1984("speed_condition");}
                    } finally {CPU.remove();BENCHMARK.remove();CANCELLATION.remove();}
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });snapshot=null;
            terminal1982=queueDecision1983.accepted?2:6;
        } catch(java.util.concurrent.CancellationException cancelled){terminal1982=11;}
          catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally {
            try{if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
            finally{try{copy.close();}finally{if(reservation1984!=null)reservation1984.close();}}
        }
        return null;
        } catch(java.util.concurrent.CancellationException cancelled){terminal1982=11;throw cancelled;}
          catch(RuntimeException failure){terminal1982=13;throw failure;}
          catch(LinkageError failure){terminal1982=13;throw failure;}
          catch(OutOfMemoryError failure){terminal1982=13;throw failure;}
        finally {
            try {
                // This is the outcome already used above, not a second queue
                // lookup that could evict work or observe a later capture.
                if(queueDecision1983==null)PipelineDiagnostics1982.residentExit(trace,Math.max(0L,retained1982),terminal1982);
                else ProcessingTiming1947.residentExit1983(trace,Math.max(0L,retained1982),terminal1982,
                    queueDecision1983.reason,queueDecision1983.retryRemainingNanos,queueDecision1983.retries,
                    queueDecision1983.queuedJobs,queueDecision1983.runningJobs,
                    queueDecision1983.retainedBytes,queueDecision1983.requestedBytes);
            }
            catch(Throwable optional){}
        }
    }
    /** Optional scalar evidence cannot change pixel admission or ownership. */
    private static void progress1984(String phase,int completed,int total) {
        try{GpuQualification1961.progress1984(phase,completed,total);}catch(Throwable optional){}
    }
    private static void outcome1984(String reason) {
        try{GpuQualification1961.outcome1984(reason);}catch(Throwable optional){}
    }
    private static void timings1984(long cpu,long gpu) {
        try{GpuQualification1961.timings1984(cpu,gpu);}catch(Throwable optional){}
    }
    private static boolean detachedPlan1976(QualityPixels1932.Plan plan) {
        if(plan==null||plan.smoothedRegions!=null)return false;
        if(plan.faceRegions!=null&&(plan.faceRegions.getClass().getClassLoader()!=GpuResident1976.class.getClassLoader()||
                !plan.faceRegions.getClass().getName().equals("com.hiro.ulike.FaceRegions1934$Mask")))return false;
        try {
            java.lang.reflect.Field cache=QualityPixels1932.Plan.class.getDeclaredField("policyCache");
            cache.setAccessible(true);return cache.get(plan)==null;
        } catch(ReflectiveOperationException unavailable){return false;}
          catch(RuntimeException unavailable){return false;}
    }
    private static boolean win(long baseline,long candidate){return baseline>0&&candidate>0&&candidate<=baseline-baseline/20;}
}
