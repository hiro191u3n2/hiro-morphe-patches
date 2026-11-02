package com.hiro.ulike;

import android.graphics.Bitmap;

/** Whole Strong-to-finish admission. One immutable queued source is sufficient;
 * half-resolution input is reconstructed after save. A missing ordinary finish
 * proof is resolved first in the same idle job. Every candidate still requires
 * two complete CPU comparisons and a real existing-route timing advantage. */
final class GpuResident1976 {
    private GpuResident1976() {}
    // resident timing19111 BEGIN: optional private benchmark observation only.
    static void residentTiming19111(String operation,long first,long second,long third) {
        try {
            if(!GpuQualification1961.background())return;
            Class.forName("com.hiro.ulike.ResidentTiming19111").getMethod("dispatch",String.class,long[].class)
                .invoke(null,operation,new long[]{first,second,third});
        } catch(Throwable optional) {}
    }
    // resident timing19111 END
    private static final ThreadLocal<Boolean> BENCHMARK=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> CPU=new ThreadLocal<Boolean>();
    private static final ThreadLocal<GpuQualification1961.Cancellation> CANCELLATION=new ThreadLocal<GpuQualification1961.Cancellation>();
    static GpuQualification1961.Cancellation cancellation(){return CANCELLATION.get();}
    static boolean benchmarking(){return Boolean.TRUE.equals(BENCHMARK.get());}
    static boolean cpuOracle(){return Boolean.TRUE.equals(CPU.get());}
    /** Permission is private to the owned whole-output benchmark, never all
     * background work. Its independent CPU oracle remains strictly CPU. */
    static boolean deployedProbe19102(){
        GpuQualification1961.Cancellation owner=CANCELLATION.get();
        return benchmarking()&&!cpuOracle()&&owner!=null&&!owner.cancelled();
    }
    private static String deployedRoutes19102(){
        return "strong="+(GpuStartup1990.coreDomainReady19102()?GpuStrongDefault1990.profile1994():-1)+
            ";model="+(GpuStartup1990.modelDomainReady19102()?GpuModelDefault1990.variant1994():-1)+
            ";resident-finish="+GpuChain1961.preferredResidentRows19112();
    }
    /** Shape-aware deployed identity. Retain the old ABI and old key text
     * when no regional prepass is actually selected. Optional lookup keeps
     * isolated historical fixtures independent of the new admission helper. */
    private static String deployedRoutes19115(int sourceWidth,int sourceHeight){
        int profile=GpuStartup1990.coreDomainReady19102()?profileForShape19115(sourceWidth,sourceHeight):-1;
        boolean regional=profile==4&&(sourceWidth==3060&&sourceHeight==4080||sourceWidth==4080&&sourceHeight==3060)&&regionalReady19115();
        return "strong="+profile+(regional?";regional=115":"")+
            ";model="+(GpuStartup1990.modelDomainReady19102()?GpuModelDefault1990.variant1994():-1)+
            ";resident-finish="+GpuChain1961.preferredResidentRows19112();
    }
    private static int profileForShape19115(int width,int height){
        try {java.lang.reflect.Method call=GpuStrongDefault1990.class.getDeclaredMethod("profile1994",Integer.TYPE,Integer.TYPE);
            call.setAccessible(true);return ((Integer)call.invoke(null,Integer.valueOf(width),Integer.valueOf(height))).intValue();}
        catch(Throwable unavailable){return GpuStrongDefault1990.profile1994();}
    }
    private static boolean regionalReady19115(){
        try {java.lang.reflect.Method call=GpuStrongDefault1990.class.getDeclaredMethod("regionalFaster19115");
            call.setAccessible(true);return Boolean.TRUE.equals(call.invoke(null));}
        catch(Throwable unavailable){return false;}
    }
    private static String routesFromKey19102(String key){
        String prefix="strong-resident1978:3:v2-deployed:";
        if(key==null||!key.startsWith(prefix))throw new IllegalArgumentException("resident route key");
        int end=key.indexOf(':',prefix.length());
        if(end<0)throw new IllegalArgumentException("resident route key boundary");
        return key.substring(prefix.length(),end);
    }
    private static int rowsFromKey19112(String key){
        String routes=routesFromKey19102(key);
        if(routes.endsWith(";resident-finish=128"))return 128;
        if(routes.endsWith(";resident-finish=64"))return 64;
        throw new IllegalArgumentException("resident row profile key");
    }
    private static String key(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,
            int rotation,int width,int height,QualityPixels1932.Plan output,boolean identity) {
        return "strong-resident1978:3:v2-deployed:"+deployedRoutes19115(source.getWidth(),source.getHeight())+":"+GpuNoise1960.fingerprint()+":"+source.getWidth()+":"+source.getHeight()+":"+
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
            if(!routesFromKey19102(base).equals(deployedRoutes19115(source.getWidth(),source.getHeight()))){terminal1982=13;return null;}
            final long started=System.nanoTime();
            GpuChain1961.Preflight1981 preflight=GpuChain1961.preflightResident1981(source,rotation,width,height,halfLength);
            if(preflight==null){terminal1982=5;return null;}
            preflight.deadline1981=started+Math.min(Long.MAX_VALUE/4,proof.cpuNanos-proof.cpuNanos/20);
            try {
                // Wide readiness can change while geometry is prepared. Never
                // use a newly preferred schedule under another whole proof.
                if(preflight.finishRows19112!=rowsFromKey19112(base)||
                        !routesFromKey19102(base).equals(deployedRoutes19115(source.getWidth(),source.getHeight()))){terminal1982=13;return null;}
                Bitmap result=QualityPipeline1932.strongFinish1978(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false,identity);
                if(result==null){terminal1982=14;return null;}
                long elapsed1992=System.nanoTime()-started;
                if(!win(proof.cpuNanos,elapsed1992))GpuQualification1961.rejectMeasuredThreshold1992(qualifiedKey,"resident_whole","deployed_whole",proof.cpuNanos,elapsed1992,proof.cpuNanos-proof.cpuNanos/20);
                ProcessingTiming1947.detailRoute1976(trace,4);
                terminal1982=1;return result;
            } catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
            catch(RuntimeException unavailable){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("resident interrupted");return null;}
            catch(LinkageError unavailable){return null;}
            catch(OutOfMemoryError unavailable){return null;}
            finally{preflight.close();}
        }
        // .91: independent startup proofs do not prove the connected route.
        // Keep the existing bounded one-copy budget and reservation below so
        // a complete resident candidate can earn its own whole-output proof.
        // The foreground still uses standard processing until that succeeds.
        GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.RESIDENT);
        final String scheduledKey=qualifiedKey==null?base+":finish-dependency":qualifiedKey;
        final long retained=ResidentProof1978.retainedBytes(source,plan,output);retained1982=retained;
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained)) {
            terminal1982=5;return null;
        }
        GpuSnapshotBudget1981.Decision1985 copyDecision1985=GpuSnapshotBudget1981.tryCopy1985(retained,GpuSnapshotBudget1981.RESIDENT);
        GpuSnapshotBudget1981.Copy copy=copyDecision1985.copy;
        try {ProcessingTiming1947.residentCopy1985(trace,copyDecision1985.reason,copyDecision1985.preferredFamily,
            copyDecision1985.activeFamily,copyDecision1985.activeStarted,copyDecision1985.copyEpoch);}
        catch(Throwable optional){}
        if(copy==null){terminal1982=Thread.currentThread().isInterrupted()?11:10;return null;}
        Bitmap snapshot=null;
        GpuQualification1961.Reservation1984 reservation1984=null;
        try {
            // Reserve both a job slot and the real retained bytes before the
            // snapshot is allocated. The ticket owns that reservation through
            // cancellation until the copying thread releases it.
            reservation1984=GpuQualification1961.reserve1985(scheduledKey,retained);
            GpuQualification1961.QueueDecision1983 admission=reservation1984.decision;
            if(!admission.accepted) {
                queueDecision1983=admission;terminal1982=6;return null;
            }
            if(!reservation1984.current()||!copy.begin()||!reservation1984.begin1985()){terminal1982=11;return null;}
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
                        // Read geometry from the immutable copy owned by this
                        // job. These are locals, preserving the shipped Probe
                        // constructor and permitting caller Bitmap recycling.
                        final int sourceWidth19115=owned.getWidth(),sourceHeight19115=owned.getHeight();
                        stage1999(1,0,false);
                        if(!GpuChain1961.opaqueResident1976(owned)){outcome1984("unsupported");return;}
                        stage1999(1,0,true);
                        long halfBytes=4L*((owned.getWidth()+1L)/2)*((owned.getHeight()+1L)/2)+8L*owned.getWidth();
                        if(!GpuNoise1960.workspaceFits(halfBytes)){outcome1984("memory_budget");return;}
                        stage1999(2,0,false);
                        final int[] frozenHalf=ResidentProof1978.half(owned,cancellation);
                        stage1999(2,0,true);
                        String baselineKey=initialBaseline;
                        if(baselineKey==null) {
                            progress1984("resident_baseline",0,1);
                            stage1999(3,0,false);
                            CPU.set(Boolean.TRUE);
                            try {QualityPipeline1932.qualifyResidentBaseline1978(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,identity,cancellation);}
                            finally {CPU.remove();}
                            if(cancellation.cancelled())return;
                            baselineKey=GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity);
                            if(baselineKey==null){GpuQualification1961.rejectNonSpeed1988(scheduledKey,"baseline_unavailable");outcome1984("baseline_unavailable");return;}
                            stage1999(3,0,true);
                            progress1984("resident_baseline",1,1);
                        }
                        final String resultKey=base+":"+baselineKey;
                        if(GpuQualification1961.exactRejected(resultKey)){
                            outcome1984("cached_resident_reference_mismatch".equals(GpuQualification1961.exactFailure1971(resultKey))?
                                "resident_reference_rejected":"quality_rejected");return;
                        }
                        // Decode the existing captured key rather than add or
                        // reorder fields in the shipped anonymous Probe ABI.
                        final String observedRoutes=routesFromKey19102(base);
                        if(!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))){
                            GpuQualification1961.rejectNonSpeed1988(scheduledKey,"reference_unstable");
                            outcome1984("reference_unstable");return;
                        }
                        long baseline=Long.MAX_VALUE,candidateWorst=0;
                        for(int trial=0;trial<2;trial++) {
                            if(cancellation.cancelled())return;
                            if(!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))||
                                    !baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))){outcome1984("reference_unstable");return;}
                            progress1984("resident_compare",trial,2);
                            Bitmap oracle=null;
                            try {
                                if(trial==0)stages19104("begin",cancellation);
                                stage1999(4,trial+1,false);
                                CPU.set(Boolean.TRUE);
                                residentTiming19111("beginCpu19115",trial,0,0);
                                try {
                                    oracle=QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,null,rotation,width,height,frozenOutput,false,true,identity);
                                    residentTiming19111("stopCpu19115",oracle==null?2:1,0,0);
                                } finally {residentTiming19111("publishCpu19115",0,0,0);CPU.remove();}
                                if(cancellation.cancelled())return;
                                if(oracle==null){outcome1984("baseline_unavailable");return;}
                                stage1999(4,trial+1,true);
                                for(int candidate=0;candidate<2;candidate++) {
                                    if(trial==0&&candidate==0)stages19104("baseline",null);
                                    stage1999(candidate==0?5:6,trial+1,false);
                                    BENCHMARK.set(Boolean.TRUE);int comparison=ResidentProof1978.UNAVAILABLE;boolean returned=false;
                                    ResidentProof1978.beginEvidence19103(oracle.getWidth(),oracle.getHeight());
                                    GpuChain1961.Preflight1981 prepared19112=null;
                                    try {
                                        if(candidate==1){
                                            prepared19112=GpuChain1961.preflightResident19112(owned,rotation,width,height,frozenHalf.length,true);
                                            if(prepared19112==null){returned=true;GpuQualification1961.rejectNonSpeed1988(scheduledKey,"execution_unavailable");outcome1984("execution_unavailable");return;}
                                            if(prepared19112.finishRows19112!=rowsFromKey19112(base)||!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))){returned=true;outcome1984("reference_unstable");return;}
                                        }
                                        comparison=QualityPipeline1932.compareStrongFinish1978(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,candidate==1,identity,oracle,cancellation);returned=true;
                                    }
                                    finally {if(prepared19112!=null)prepared19112.close();BENCHMARK.remove();
                                        ResidentProof1978.Evidence19103 evidence=ResidentProof1978.endEvidence19103();
                                        int status=cancellation.cancelled()||Thread.currentThread().isInterrupted()?4:
                                            !returned?5:evidence!=null&&evidence.shapeMismatch?6:comparison==ResidentProof1978.EXACT?1:comparison==ResidentProof1978.MISMATCH?2:3;
                                        comparisonEvidence19103(candidate==1,status,evidence);
                                    }
                                    if(cancellation.cancelled())return;
                                    if(!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))){
                                        outcome1984("reference_unstable");return;
                                    }
                                    boolean accepted=acceptComparison19103(resultKey,candidate==1,comparison);
                                    if(trial==0&&candidate==0){
                                        // The independent exact gate is already settled. Secondary
                                        // diagnostics cannot clear or replace its persistent failure.
                                        if(!accepted&&comparison==ResidentProof1978.MISMATCH&&Boolean.TRUE.equals(stages19104("needsReplay",null))){
                                            // Final evidence is detached. Release the full output before
                                            // creating a CPU replay; never retain two output Bitmaps.
                                            oracle.recycle();oracle=null;
                                            long replayImages=4L*owned.getWidth()*owned.getHeight()+4L*width*height;
                                            if(Boolean.TRUE.equals(stages19104("canReplay",Long.valueOf(replayImages)))){
                                            Bitmap replay=null;stages19104("replay",null);CPU.set(Boolean.TRUE);
                                            try{replay=QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,null,rotation,width,height,frozenOutput,false,true,identity);}
                                            catch(Throwable diagnosticOnly){if(!Boolean.TRUE.equals(stages19104("replayComplete",null)))stages19104("failed",null);}
                                            finally{CPU.remove();if(replay!=null&&!replay.isRecycled())replay.recycle();}
                                            }
                                        }
                                        stageReport19104(comparison);
                                    }
                                    if(!accepted)return;
                                    stage1999(candidate==0?5:6,trial+1,true);
                                }
                            } finally {stageReport19104(ResidentProof1978.UNAVAILABLE);CPU.remove();BENCHMARK.remove();if(oracle!=null)oracle.recycle();}
                            progress1984("resident_compare",trial+1,2);
                            // Real foreground-shaped timing includes destination
                            // allocation/Bitmap writes. No CPU reference is held
                            // during this phase and proof-only row reads are absent.
                            progress1984("resident_speed",trial,2);
                            for(int candidate=0;candidate<2;candidate++) {
                                if(cancellation.cancelled())return;
                                Bitmap result=null;long elapsed;
                                GpuChain1961.Preflight1981 prepared19112=null;
                                Bitmap ordinaryInput=candidate==0?owned.copy(Bitmap.Config.ARGB_8888,true):null;
                                if(candidate==0&&ordinaryInput==null){outcome1984("baseline_unavailable");return;}
                                stage1999(candidate==0?7:8,trial+1,false);
                                residentTiming19111("begin",candidate,trial,0); // resident timing19111
                                BENCHMARK.set(Boolean.TRUE);long started=System.nanoTime();
                                residentTiming19111("start",started,0,0); // resident timing19111
                                try {
                                    if(candidate==1){
                                        prepared19112=GpuChain1961.preflightResident19112(owned,rotation,width,height,frozenHalf.length,false);
                                        if(prepared19112==null){GpuQualification1961.rejectNonSpeed1988(scheduledKey,"execution_unavailable");outcome1984("execution_unavailable");return;}
                                        if(prepared19112.finishRows19112!=rowsFromKey19112(base)||!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))){outcome1984("reference_unstable");return;}
                                    }
                                    result=candidate==0?QualityPipeline1932.strongFinishOwned1978(ordinaryInput,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,false,identity)
                                        :QualityPipeline1932.strongFinish1978(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,true,false,identity);
                                    elapsed=System.nanoTime()-started;
                                    residentTiming19111("stop",started+elapsed,result==null?2:1,0); // resident timing19111
                                    if(cancellation.cancelled())return;
                                    if(!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))){
                                        outcome1984("reference_unstable");return;
                                    }
                                    if(result==null){outcome1984(candidate==0?"baseline_unavailable":"execution_unavailable");return;}
                                    if(candidate==0)baseline=Math.min(baseline,elapsed);else candidateWorst=Math.max(candidateWorst,elapsed);
                                    stage1999(candidate==0?7:8,trial+1,true);
                                } finally {
                                    residentTiming19111("publish",0,0,0); // resident timing19111
                                    if(prepared19112!=null)prepared19112.close();BENCHMARK.remove();if(result!=null)result.recycle();if(ordinaryInput!=null&&!ordinaryInput.isRecycled())ordinaryInput.recycle();}
                            }
                            progress1984("resident_speed",trial+1,2);
                        }
                        if(cancellation.cancelled())return;
                        stage1999(9,0,false);
                        if(!observedRoutes.equals(deployedRoutes19115(sourceWidth19115,sourceHeight19115))||
                                !baselineKey.equals(GpuChain1961.residentBaseline1978(owned,rotation,width,height,frozenOutput,identity))) {
                            GpuQualification1961.rejectNonSpeed1988(resultKey,"reference_unstable");outcome1984("reference_unstable");return;
                        }
                        // This proof compares fastest existing route with the
                        // slowest connected route. A different Strong worker
                        // plan changes most of that measured call. Keep the
                        // exact-pixel result, but retry speed on comparable
                        // executions instead of recording a speed rejection.
                        if(!GpuQualification1961.comparableResidentPlans19116()){
                            GpuQualification1961.rejectNonSpeed1988(resultKey,"reference_unstable");
                            outcome1984("reference_unstable");return;
                        }
                        timings1984(baseline,candidateWorst);
                        if(win(baseline,candidateWorst))GpuQualification1961.qualified(resultKey,baseline,candidateWorst,0);
                        else {GpuQualification1961.rejectMeasuredThreshold1992(resultKey,"resident_whole","deployed_whole",baseline,candidateWorst,baseline-baseline/20);outcome1984("speed_condition");}
                        stage1999(9,0,true);
                    } catch(RuntimeException failure) {
                        // runFinishStages and each comparison/timing owner have
                        // already closed their sessions and recycled outputs.
                        // A typed Strong admission refusal is not a pixel or
                        // measured-speed failure. The classifier accepts only
                        // the private producer sentinel, never its message or
                        // a wrapped cause. Unknown exceptions retain the queue's
                        // normal exception/cancellation handling.
                        String refusal19110=admissionRefusal19110(failure);
                        if(refusal19110==null)throw failure;
                        if(cancellation.cancelled()||Thread.currentThread().isInterrupted())
                            throw new java.util.concurrent.CancellationException("resident interrupted");
                        GpuQualification1961.rejectNonSpeed1988(scheduledKey,refusal19110);
                        outcome1984(refusal19110);
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
    /** A failed existing route prevents the connected proof from being trusted.
     * Keep the same persistent safety stop: keys omit source pixels, so a later
     * easy image must never clear an observed failed prerequisite. The cause
     * explicitly says the connected candidate itself has not been evaluated. */
    static boolean acceptComparison19103(String key,boolean candidate,int comparison){
        if(comparison==ResidentProof1978.EXACT)return true;
        if(comparison==ResidentProof1978.MISMATCH){
            if(candidate){GpuQualification1961.rejectExact(key);outcome1984("resident_candidate_mismatch");}
            else {GpuQualification1961.rejectExact1971(key,"resident_reference_mismatch");outcome1984("resident_reference_mismatch");}
        }else{GpuQualification1961.rejectNonSpeed1988(key,"comparison_incomplete");outcome1984("comparison_incomplete");}
        return false;
    }
    private static volatile java.lang.reflect.Method comparisonDetail19103;
    private static volatile boolean comparisonDetailLooked19103;
    private static void comparisonEvidence19103(boolean candidate,int status,ResidentProof1978.Evidence19103 e){
        try{
            if(e==null)return;
            if(!comparisonDetailLooked19103){try{comparisonDetail19103=GpuQualification1961.class.getMethod("residentComparison19103",long[].class);}
                finally{comparisonDetailLooked19103=true;}}
            if(comparisonDetail19103!=null)comparisonDetail19103.invoke(null,(Object)new long[]{candidate?2:1,status,e.compared,
                (long)e.width*e.height,e.x+1L,e.y+1L,e.expectedArgb&0xffffffffL,e.actualArgb&0xffffffffL,e.width,e.height});
        }catch(Throwable optional){}
    }
    /** Optional scalar evidence cannot change pixel admission or ownership. */
    /** Optional ABI bridge; the chain itself checks private type and reason.
     * A missing bridge cannot turn an unknown failure into a budget result. */
    private static boolean memoryRefusal19108(RuntimeException failure){
        try{return Boolean.TRUE.equals(GpuChain1961.class.getDeclaredMethod("memoryRefusal19108",RuntimeException.class).invoke(null,failure));}
        catch(Throwable unavailable){return false;}
    }
    /** Optional producer ABI bridge. Older/narrow runtime peers cannot invent
     * a typed Strong failure, and an unavailable bridge never relaxes a gate. */
    private static String admissionRefusal19110(RuntimeException failure){
        if(failure instanceof java.util.concurrent.CancellationException)return null;
        if(memoryRefusal19108(failure))return "memory_budget";
        Object reason;
        try {reason=Class.forName("com.hiro.ulike.GpuStrong1960")
                .getDeclaredMethod("residentAdmissionReason19110",RuntimeException.class).invoke(null,failure);}
        catch(Throwable unavailable){return null;}
        if("memory_budget".equals(reason))return "memory_budget";
        if("storage_limit".equals(reason)||"native_limit".equals(reason))return "unsupported";
        if("session_unavailable".equals(reason)||"input_invalid".equals(reason)||
                "capacity_unavailable".equals(reason)||"lease_limit".equals(reason)||
                "upload_failed".equals(reason)||"allocation_failed".equals(reason)||
                "stage_unavailable".equals(reason)||"forecast_unavailable".equals(reason))return "execution_unavailable";
        return null;
    }
    private static volatile java.lang.reflect.Method residentDetail1999;
    private static volatile boolean residentDetailLooked1999;
    private static void stage1999(int stage,int trial,boolean completed) {
        try {
            if(!residentDetailLooked1999){
                try {residentDetail1999=GpuQualification1961.class.getMethod("residentStage1999",int.class,int.class,boolean.class);}
                finally {residentDetailLooked1999=true;}
            }
            java.lang.reflect.Method detail=residentDetail1999;
            if(detail!=null)detail.invoke(null,Integer.valueOf(stage),Integer.valueOf(trial),Boolean.valueOf(completed));
        }catch(Throwable optional){}
    }
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
    /** Optional after-save diagnostics. Narrow host fixtures deliberately omit
     * the observer; absence cannot change the independent quality gate. */
    private static Object stages19104(String operation,Object argument){
        try{Class<?> type=Class.forName("com.hiro.ulike.ResidentStages19104");
            if("begin".equals(operation))return type.getMethod(operation,Object.class).invoke(null,argument);
            if("finish".equals(operation))return type.getMethod(operation,int.class).invoke(null,argument);
            if("canReplay".equals(operation))return type.getMethod(operation,long.class).invoke(null,argument);
            return type.getMethod(operation).invoke(null);
        }catch(Throwable diagnosticOnly){return null;}
    }
    private static void stageReport19104(int comparison){
        Object values=stages19104("finish",Integer.valueOf(comparison));if(!(values instanceof long[]))return;
        try{GpuQualification1961.class.getMethod("residentStages19104",long[].class).invoke(null,values);}catch(Throwable optional){}
    }

}
