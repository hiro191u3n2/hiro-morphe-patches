package com.hiro.ulike;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;

/** GX1/2/4/7/8 exact NR9--NR13 GPU candidates. No photograph is retained by
 * the bounded admission cache. A stage owns a private session until endStage;
 * immutable evidence/maps stay on GPU while the strip source/output are reused.
 * Background qualification preserves the original CPU output. A new full
 * foreground key commits private GPU output only after two CPU comparisons.
 * Both full ARGB and sparse NR13 confidence must match twice consecutively.
 * Full-resolution Strong prefers the verified GPU even when it is slower;
 * preparation modes retain the upload/dispatch/read/close-inclusive 5% margin.
 * Tiled and generic full dispatch have separate quality certificates and
 * negatives; a rejected tiled path cannot bypass the generic comparison.
 */
public final class GpuStrong1960 {
    private GpuStrong1960() {}
    interface Oracle { boolean run(int[] destination,int[] confidence); }
    private static final int[] BINDINGS={0,1,2,3,4,5,6,7};
    // GX19: two disjoint mutable banks share only immutable model2/4/5/6.
    // Order:source,out,policy,confidence,mask,grid,sigma,dummy,expected,diff.
    private static final int[][] BANKS={{0,1,3,7,8,9,10,11,12,13},{14,15,16,17,18,19,20,21,22,23}};
    private static final int LIMIT=64;
    private static final LinkedHashMap<String,Gate> GATES=new LinkedHashMap<String,Gate>(LIMIT,.75f,true);
    private static volatile Stage active;
    private static final long PREFERRED_WAIT_NS=15000000000L;
    private static final int MAX_PREFERRED_WAITERS=4;
    private static final String[] REASONS={"awaiting_quality","quality_rejected","shader_unsupported","memory_budget",
        "session_unavailable","bank_busy","upload_failed","execution_failed","readback_failed","policy_mismatch","cancelled","unknown",
        "cached_legacy_negative","cached_argb_negative","cached_confidence_negative","cached_policy_negative","cache_limit_negative",
        "new_argb_mismatch","new_confidence_mismatch","policy_failure","submit_failed","output_shape_failed","native_failure_unknown",
        "cold_proof_budget","proof_in_flight","cold_bank_busy","proof_already_attempted","deferred_quality1978","cpu_faster1978"};
    private static final String[] TRIALS1971={"argb_mismatch","confidence_mismatch","policy_failure","submit_failed",
        "readback_failed","output_shape_failed","native_failure_unknown","upload_failed","execution_failed"};
    private static final class Gate {int consecutive,variant,badVariants1973,unsupportedVariants1973;long cpuBaseline,gpuBaseline;boolean rejected,accepted;}
    private static final int COLD_LIMIT1973=2;
    private static final long COLD_ADMISSION_NS1973=1000000000L;
    private static final Object COLD_LOCK1973=new Object();
    private static final HashSet<String> FLIGHTS1973=new HashSet<String>();
    // Weak keys and scalar-only values cannot retain a photograph or model.
    private static final WeakHashMap<StrongNoise1958.Model,ColdState1973> DETACHED1973=new WeakHashMap<StrongNoise1958.Model,ColdState1973>();
    private static int activeCold1973;
    private static final class ColdState1973 {long start;int attempts,pending;final HashSet<String> attempted=new HashSet<String>();}
    private static final class Flight1973 {
        final String base,failure;final ColdState1973 state;boolean committed;
        Flight1973(String base,String failure,ColdState1973 state){this.base=base;this.failure=failure;this.state=state;}
    }
    /** v1.9.74: reserve a bounded tentative owner first. A bank or memory
     * refusal did no GPU proof and must not consume this capture's two proofs. */
    private static Flight1973 admit1973(Stage stage,StrongNoise1958.Model model,String base){
        synchronized(COLD_LOCK1973){
            if(FLIGHTS1973.contains(base))return new Flight1973(null,"proof_in_flight",null);
            ColdState1973 state=stage==null?DETACHED1973.get(model):stage.cold1973;
            if(state==null){state=new ColdState1973();DETACHED1973.put(model,state);
                while(DETACHED1973.size()>LIMIT){java.util.Iterator<StrongNoise1958.Model> oldest=DETACHED1973.keySet().iterator();oldest.next();oldest.remove();}}
            if(state.attempted.contains(base))return new Flight1973(null,"proof_already_attempted",null);
            long now=System.nanoTime();
            if(state.attempts+state.pending>=COLD_LIMIT1973||state.start!=0&&now-state.start>COLD_ADMISSION_NS1973)return new Flight1973(null,"cold_proof_budget",null);
            if(activeCold1973>=COLD_LIMIT1973)return new Flight1973(null,"cold_bank_busy",null);
            Flight1973 token=new Flight1973(base,null,state);
            try{FLIGHTS1973.add(base);}
            catch(RuntimeException failure){FLIGHTS1973.remove(base);throw failure;}
            catch(Error failure){FLIGHTS1973.remove(base);throw failure;}
            state.pending++;activeCold1973++;return token;
        }
    }
    /** Count only a real submitted proof. Pending plus committed attempts are
     * bounded together, so admission refunds cannot create extra live proofs. */
    private static void commit1974(Flight1973 flight){
        if(flight==null||flight.base==null)return;
        synchronized(COLD_LOCK1973){
            if(flight.committed)return;
            try{flight.state.attempted.add(flight.base);}
            catch(RuntimeException failure){flight.state.attempted.remove(flight.base);throw failure;}
            catch(Error failure){flight.state.attempted.remove(flight.base);throw failure;}
            flight.state.pending--;flight.state.attempts++;flight.committed=true;
            if(flight.state.start==0)flight.state.start=System.nanoTime();
        }
    }
    private static void release1973(Flight1973 flight){
        if(flight!=null&&flight.base!=null)synchronized(COLD_LOCK1973){
            if(FLIGHTS1973.remove(flight.base)){if(!flight.committed)flight.state.pending--;activeCold1973--;COLD_LOCK1973.notifyAll();}
        }
    }
    /** Let the four production workers reuse a proof that is nearly complete.
     * A stalled peer adds at most 350 ms and never causes a duplicate proof. */
    private static void awaitProof1974(String base){
        long deadline=System.nanoTime()+350000000L;
        synchronized(COLD_LOCK1973){
            while(FLIGHTS1973.contains(base)){
                check();long remaining=deadline-System.nanoTime();if(remaining<=0)return;
                try{COLD_LOCK1973.wait(remaining/1000000L,(int)(remaining%1000000L));}
                catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException("GPU proof reuse wait interrupted");}
            }
        }
    }
    private static final class Sample {
        final String key;final long gpu,cpu;final int pixels;final boolean exact,observation;
        Sample(String key,long gpu,long cpu,int pixels,boolean exact,boolean observation){this.key=key;this.gpu=gpu;this.cpu=cpu;this.pixels=pixels;this.exact=exact;this.observation=observation;}
    }
    private static final class Stage {
        final StrongNoise1958.Model model;GpuNoise1960.Session session;
        final ProcessingTiming1947.Trace trace;
        final ColdState1973 cold1973=new ColdState1973();
        long cpuWork1973,gpuWork1973,waitWork1973;
        long firstArgbCount1973=-1,firstConfidenceCount1973=-1,firstConfidenceMax1973=-1;int firstArgbMax1973=-1;
        int gpuStrips,cpuStrips,verificationCpu;
        final int[] reasons=new int[REASONS.length],trials1971=new int[TRIALS1971.length];
        Failure1971 firstFailure1971;String unavailableReason1971;int unavailableCode1971;
        final ArrayDeque<Long> preferredWaiters=new ArrayDeque<Long>();
        long preferredSequence;
        final ArrayList<Sample> samples=new ArrayList<Sample>();
        final boolean[] banks=new boolean[2];
        boolean resident1976;int residentRows1976;
        int workers1978=1;
        final boolean cpuOracle1976=GpuResident1976.cpuOracle(),benchmark1976=GpuResident1976.benchmarking();
        final GpuQualification1961.Cancellation cancellation1976=GpuResident1976.cancellation();
        // Private arrays never alias caller outputs. Two sets preserve trial1
        // while trial2 CPU/GPU work overlaps; later strips reuse the capacities.
        final Readback1975[][] readbacks1975=new Readback1975[2][2];
        final long[] bankStart=new long[2],bankDuration=new long[2];
        boolean waiter,initialFailureReported1971;
        long setup,setupBegin,setupEnd,pixels;boolean closed,unavailable,failed;
        Stage(StrongNoise1958.Model model){this.model=model;trace=ProcessingTiming1947.current();}
    }
    private static boolean interrupted(){return Thread.currentThread().isInterrupted();}
    private static void check(){Stage stage=active;if(interrupted()||stage!=null&&stage.cancellation1976!=null&&stage.cancellation1976.cancelled())throw new CancellationException("GPU strong NR interrupted");}
    private static boolean accepted(String key){synchronized(GATES){Gate g=GATES.get(key);return g!=null&&g.accepted&&!g.rejected;}}
    private static int choice(String key){synchronized(GATES){Gate g=GATES.get(key);return g==null?0:g.variant;}}
    private static boolean rejected(String key){return !GpuQualification1961.maySchedule(key);}
    private static void record(String key,boolean exact,long gpu,long cpu){
        if(!exact){fail(key);return;}
        if(cpu<=0||gpu<=0||gpu>cpu-cpu/20){slow(key);return;}
        synchronized(GATES){
            Gate gate=GATES.get(key);if(gate==null){gate=new Gate();GATES.put(key,gate);}
            if(!GpuQualification1961.exactRejected(key)){gate.cpuBaseline=gate.cpuBaseline==0?cpu:Math.min(gate.cpuBaseline,cpu);gate.gpuBaseline=Math.max(gate.gpuBaseline,gpu);if(++gate.consecutive>=2)gate.accepted=true;}
            while(GATES.size()>LIMIT)GATES.remove(GATES.keySet().iterator().next());
        }
    }
    private static void observe(String key,long gpu){
        boolean invalid=false;
        synchronized(GATES){Gate gate=GATES.get(key);if(gate!=null&&gate.accepted&&!gate.rejected){
            if(gate.cpuBaseline<=0||gpu>gate.cpuBaseline-gate.cpuBaseline/20)invalid=true;
            else gate.gpuBaseline=Math.max(gate.gpuBaseline,gpu);}}
        if(invalid)slow(key);
    }
    private static void slow(String key){
        synchronized(GATES){Gate gate=GATES.get(key);if(gate!=null){gate.accepted=false;gate.consecutive=0;gate.cpuBaseline=0;gate.gpuBaseline=0;}}
        GpuQualification1961.rejectSpeed(key);
    }
    private static void fail(String key){
        synchronized(GATES){Gate gate=GATES.get(key);if(gate!=null){gate.rejected=true;gate.accepted=false;gate.consecutive=0;}}
        GpuQualification1961.rejectExact(key);
    }
    private static void restore(String key){
        if(GpuQualification1961.exactRejected(key))return;
        GpuQualification1961.Record saved=GpuQualification1961.restore(key);if(saved==null)return;
        synchronized(GATES){Gate gate=GATES.get(key);if(gate==null){gate=new Gate();GATES.put(key,gate);}
            gate.rejected=false;gate.accepted=true;gate.consecutive=2;gate.cpuBaseline=saved.cpuNanos;gate.gpuBaseline=saved.gpuNanos;gate.variant=saved.variant;
            while(GATES.size()>LIMIT)GATES.remove(GATES.keySet().iterator().next());}
    }
    private static String key(int[] u){
        // All branches, alpha/bounds handling and representative-cell alignment
        // belong to the key. No source pixels, mask or model evidence are stored.
        return "strong-gx1964-parallel-policy-bank-v1:"+u[10]+":"+u[0]+":"+u[1]+":"+(u[3]-u[2])+":"+
               u[4]+":"+u[5]+":"+(u[2]&7)+":"+(u[6]&63)+":"+u[7]+":"+u[8]+":"+u[9]+":"+u[11]+":"+u[12]+":"+u[18]+":"+u[19];
    }
    /** Called once immediately before starting this model's strip tasks. */
    public static void beginStage(StrongNoise1958.Model model){
        if(model==null||interrupted())return;
        synchronized(GpuStrong1960.class){
            if(active==null)active=new Stage(model);
        }
    }
    /** Exact production worker count, calculated after the caller's unchanged
     * memory admission. Unknown counts never activate a cohort hint. */
    public static void configureWorkers1978(StrongNoise1958.Model model,int workers){
        Stage stage=active;if(stage==null||stage.model!=model)return;
        synchronized(stage){if(!stage.closed&&workers>0&&workers<=4)stage.workers1978=workers;}
    }
    /** Called under stage's monitor only after an eligible non-rejected key.
     * A CPU-only stage never opens EGL or transfers a model. */
    private static boolean initialize(Stage stage,long rangePeak){
        if(stage.closed||stage.unavailable)return false;if(stage.session!=null)return true;
        // GX20: a qualified pyramid/evidence builder may hand over the exact
        // resident model. Ownership is consumed once; no maps are reuploaded.
        GpuAnalysis1961.Resident carried=StrongNoise1958.takeResident1961(stage.model);
        if(carried!=null){
            if(!GpuNoise1960.workspaceFits(rangePeak)){carried.close();stage.unavailable=true;return false;}
            stage.setupBegin=System.nanoTime();stage.session=carried.session;
            stage.setupEnd=System.nanoTime();stage.setup=carried.setupNanos+stage.setupEnd-stage.setupBegin;return true;
        }
        float[] evidence=StrongNoise1958.gpuEvidence1960(stage.model);int[][] maps=StrongNoise1958.gpuMaps1960(stage.model);
        long bytes=4L*(evidence.length+(long)maps[0].length+maps[1].length+maps[2].length);
        if(maps[0].length==0||maps[1].length==0||maps[2].length==0||bytes>GpuNoise1960.MAX_BYTES||
           !GpuNoise1960.workspaceFits(bytes+rangePeak)){stage.unavailable=true;return false;}
        stage.setupBegin=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
        if(session==null){stage.unavailable=true;return false;}
        boolean success=false;
        try{
            if(!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2]))return false;
            check();stage.session=session;stage.setupEnd=System.nanoTime();stage.setup=stage.setupEnd-stage.setupBegin;success=true;return true;
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return false;}catch(LinkageError unavailable){return false;}catch(OutOfMemoryError unavailable){return false;}
        finally{if(!success){stage.unavailable=true;stage.failed=true;session.close();}}
    }
    /** Full-route admission uses the capacities that will actually be uploaded.
     * The unchanged preparation/background routes keep their original gate. */
    private static boolean initialize1971(Stage stage){
        if(stage.closed||stage.unavailable)return false;if(stage.session!=null)return true;
        GpuAnalysis1961.Resident carried=StrongNoise1958.takeResident1961(stage.model);
        if(carried!=null){stage.setupBegin=System.nanoTime();stage.session=carried.session;
            stage.setupEnd=System.nanoTime();stage.setup=carried.setupNanos+stage.setupEnd-stage.setupBegin;return true;}
        float[] evidence=StrongNoise1958.gpuEvidence1960(stage.model);int[][] maps=StrongNoise1958.gpuMaps1960(stage.model);
        if(evidence==null||maps==null||maps.length!=3||maps[0].length==0||maps[1].length==0||maps[2].length==0){
            stage.unavailable=true;stage.unavailableReason1971="memory_budget";return false;}
        stage.setupBegin=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
        if(session==null){stage.unavailable=true;stage.unavailableReason1971="session_unavailable";return false;}
        boolean success=false;GpuNoise1960.Lease1971 lease=null;
        try{
            lease=session.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
            if(lease==null){stage.unavailableReason1971="memory_budget";return false;}
            if(!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2])){
                stage.unavailableReason1971=nativeReason1971(session,"upload_failed");stage.unavailableCode1971=session.failureCode1971();return false;}
            check();stage.session=session;stage.setupEnd=System.nanoTime();stage.setup=stage.setupEnd-stage.setupBegin;success=true;return true;
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){stage.unavailableReason1971="upload_failed";return false;}
        catch(LinkageError unavailable){stage.unavailableReason1971="upload_failed";return false;}
        catch(OutOfMemoryError unavailable){stage.unavailableReason1971="memory_budget";return false;}
        finally{try{if(lease!=null)lease.close();}finally{if(!success){stage.unavailable=true;stage.failed=true;session.close();}}}
    }
    static GpuNoise1960.Lease1971 reserveRange1971(GpuNoise1960.Session session,int[] source,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int[] bank){
        return reserveRange1974(session,source,policy,u,descriptor,bank,1);
    }
    /** Idle proof includes a complete private readback of the canonical policy.
     * This additional oracle storage is admitted before any native command. */
    static GpuNoise1960.Lease1971 reserveProof1978(GpuNoise1960.Session session,int[] source,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int[] bank){
        long count=(long)u[0]*(u[3]-u[2]),cf=u[12]!=0?(long)((u[0]+3)/4)*((u[3]-u[2]+3)/4):0;
        return reserveRange1975(session,source,policy,u,descriptor,bank,4L*(count+cf+1+(policy==null?1:policy.length))+320L);
    }
    static String verifyPolicy1978(GpuNoise1960.Session session,int slot,int[] policy){
        int[] actual=session.readInts(slot,policy==null?1:policy.length);
        if(actual==null)return "readback_failed";
        return policy==null?(actual.length==1&&actual[0]==0?null:"policy_failure"):Arrays.equals(actual,policy)?null:"policy_failure";
    }
    private static GpuNoise1960.Lease1971 reserveRange1974(GpuNoise1960.Session session,int[] source,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int[] bank,int resultCopies){
        long count=(long)u[0]*(u[3]-u[2]),cf=u[12]!=0?(long)((u[0]+3)/4)*((u[3]-u[2]+3)/4):0;
        return reserveRange1975(session,source,policy,u,descriptor,bank,4L*resultCopies*(count+cf+1)+256L);
    }
    private static GpuNoise1960.Lease1971 reserveRange1975(GpuNoise1960.Session session,int[] source,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int[] bank,long readbackBytes){
        long count=(long)u[0]*(u[3]-u[2]),confidence=u[12]!=0?(long)((u[0]+3)/4)*((u[3]-u[2]+3)/4):1;
        int[] slots;long[] target;
        if(descriptor==null){slots=new int[]{bank[0],bank[1],bank[2],bank[3],bank[9]};
            target=new long[]{4L*source.length,4L*count,policy==null?4:4L*policy.length,4L*confidence,4};}
        else{slots=bank.clone();target=new long[]{4L*source.length,4L*count,8L*count,4L*confidence,4L*descriptor.masks.length,
                4L*descriptor.grid.length,4L*count,4,4L*policy.length,4};}
        return session.reserveCapacity1971(slots,target,readbackBytes);
    }
    private static String nativeReason1971(GpuNoise1960.Session session,String fallback){
        int code=session.failureCode1971();
        return code==1?"memory_budget":code==2?"upload_failed":code==3?"execution_failed":
            code==4||code==5?"readback_failed":code==6?"native_failure_unknown":fallback;
    }
    /** GX36 one waiting strip at most; a bank lease is never queued without a
     * measured complete-output certificate. Wait only when the measured GPU
     * duration plus predicted nearest-bank release fits inside the measured
     * CPU wall duration with the unchanged five-percent margin. Timeout,
     * invalidated proof, failure and interruption all release the waiter slot. */
    private static int claimBank(Stage stage,String key,long requestStart) {
        boolean waiting=false;
        try {
            for(;;) {
                check();if(stage.closed||stage.unavailable||stage.failed)return -1;
                long cpu,gpu;
                synchronized(GATES){Gate gate=GATES.get(key);
                    if(gate==null||!gate.accepted||gate.rejected||gate.cpuBaseline<=0||gate.gpuBaseline<=0)return -1;
                    cpu=gate.cpuBaseline;gpu=gate.gpuBaseline;
                }
                long now=System.nanoTime(),budget=cpu-cpu/20-gpu-(now-requestStart);
                // A timeout and bank release may race while this waiter is
                // reacquiring the stage monitor. Recheck elapsed wall time
                // before taking the newly free bank, even after notification.
                if(waiting&&budget<=0)return -1;
                for(int i=0;i<stage.banks.length;i++)if(!stage.banks[i]) {
                    stage.banks[i]=true;stage.bankStart[i]=now;stage.bankDuration[i]=gpu;return i;
                }
                if(!waiting&&stage.waiter)return -1;
                if(budget<=0)return -1;
                long remaining=Long.MAX_VALUE;
                for(int i=0;i<stage.banks.length;i++) {
                    if(stage.bankStart[i]<=0||stage.bankDuration[i]<=0)return -1;
                    remaining=Math.min(remaining,Math.max(0L,stage.bankDuration[i]-(now-stage.bankStart[i])));
                }
                if(remaining>budget)return -1;
                if(!waiting){stage.waiter=true;waiting=true;}
                try{stage.wait(budget/1000000L,(int)(budget%1000000L));}
                catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException("GX36 GPU bank wait interrupted");}
            }
        }finally{if(waiting){stage.waiter=false;stage.notifyAll();}}
    }
    /** Worker tasks must all join before this call. End closes/frees GPU images
     * and accounts the actual stage setup/close cost before admitting a key. */
    public static void endStage(StrongNoise1958.Model model){
        Stage stage;
        synchronized(GpuStrong1960.class){stage=active;if(stage==null||stage.model!=model){StrongNoise1958.discardResident1961(model);return;}active=null;}
        synchronized(stage){
            if(stage.closed)return;stage.closed=true;try{if(stage.session==null){
                GpuAnalysis1961.Resident unused=StrongNoise1958.takeResident1961(model);if(unused!=null)unused.close();stage.samples.clear();return;
            }
            long start=System.nanoTime();stage.session.close();stage.session=null;
            long overhead=stage.setup+System.nanoTime()-start;
            for(Sample sample:stage.samples){
                long share=stage.pixels==0?overhead:(long)((double)overhead*sample.pixels/stage.pixels);
                if(sample.observation)observe(sample.key,sample.gpu+share);
                else record(sample.key,sample.exact,sample.gpu+share,sample.cpu);
            }
            stage.samples.clear();
            }finally{
                if(!stage.cpuOracle1976&&!stage.benchmark1976) {
                ProcessingTiming1947.noiseBackend(stage.trace,stage.gpuStrips,stage.cpuStrips);
                ProcessingTiming1947.strongGpuVerification1970(stage.trace,stage.verificationCpu);
                ProcessingTiming1947.strongGpuWork1973(stage.trace,stage.cpuWork1973,stage.gpuWork1973,stage.waitWork1973,0);
                for(int i=0;i<TRIALS1971.length;i++)if(stage.trials1971[i]>0)
                    ProcessingTiming1947.strongGpuTrial1971(stage.trace,TRIALS1971[i],stage.trials1971[i]);
                Failure1971 failure=stage.firstFailure1971;
                if(failure!=null){
                    ProcessingTiming1947.strongGpuMismatch1973(stage.trace,stage.firstArgbCount1973,stage.firstArgbMax1973,stage.firstConfidenceCount1973,stage.firstConfidenceMax1973);
                    ProcessingTiming1947.strongGpuFailure1971(stage.trace,failure.kind,failure.program,failure.layout,
                        failure.width,failure.rows,failure.x,failure.y,failure.delta);
                    try{CameraTrace1965.event("strong_gpu_failure1971",0,"trace="+(stage.trace==null?0:stage.trace.id)+" kind="+failure.kind+
                        " p="+failure.program+" l="+failure.layout+" w="+failure.width+" h="+failure.rows+
                        " x="+failure.x+" y="+failure.y+" d="+failure.delta+" n="+failure.nativeCode+
                        " ay="+failure.firstY+" b="+failure.begin+" sr="+failure.sourceRows);}catch(Throwable optional){}
                }
                for(int i=0;i<REASONS.length;i++)if(stage.reasons[i]>0)
                    ProcessingTiming1947.strongGpuReason1970(stage.trace,REASONS[i],stage.reasons[i]);
                logs1971(stage);
                }
            }
        }
    }
    /** Trace events use the fixed REASONS r0..r26 and TRIALS1971 t0..t8
     * order above. Numeric groups preserve every count within the160char sink. */
    private static void logs1971(Stage stage){
        long trace=stage.trace==null?0:stage.trace.id;
        try{CameraTrace1965.event("strong_gpu_counts1971",0,"trace="+trace+" gpu="+stage.gpuStrips+" cpu="+stage.cpuStrips+" verify="+stage.verificationCpu);}catch(Throwable optional){}
        for(int base=0;base<REASONS.length;base+=10){
            StringBuilder fields=new StringBuilder("trace="+trace+" base="+base);boolean any=false;
            for(int i=base;i<Math.min(REASONS.length,base+10);i++)if(stage.reasons[i]>0){any=true;fields.append(" r").append(i).append('=').append(stage.reasons[i]);}
            if(any)try{CameraTrace1965.event("strong_gpu_reasons1971",0,fields.toString());}catch(Throwable optional){}
        }
        StringBuilder fields=new StringBuilder("trace="+trace);boolean any=false;
        for(int i=0;i<TRIALS1971.length;i++)if(stage.trials1971[i]>0){any=true;fields.append(" t").append(i).append('=').append(stage.trials1971[i]);}
        if(any)try{CameraTrace1965.event("strong_gpu_trials1971",0,fields.toString());}catch(Throwable optional){}
        try{CameraTrace1965.event("strong_gpu_work1973",0,"trace="+trace+" cpu_ns="+stage.cpuWork1973+" gpu_ns="+stage.gpuWork1973+" wait_ns="+stage.waitWork1973);}catch(Throwable optional){}
        if(stage.firstFailure1971!=null)try{CameraTrace1965.event("strong_gpu_scan1973",0,"trace="+trace+" ac="+stage.firstArgbCount1973+" am="+stage.firstArgbMax1973+" cc="+stage.firstConfidenceCount1973+" cm="+stage.firstConfidenceMax1973);}catch(Throwable optional){}
    }
    static boolean prepare(int[] source,int[] destination,int width,int height,int noise,
            boolean shadows,float[] evidence,int mode,Oracle oracle){
        if(interrupted()||source==null||destination==null||width<1||height<1||
           source.length<(long)width*height||destination.length<(long)width*height||
           evidence==null||evidence.length<16||mode<0||mode>2)return false;
        int[] u=new int[32];u[0]=width;u[1]=height;u[3]=height;u[5]=height;u[7]=height;u[8]=noise;u[9]=shadows?1:0;u[10]=mode;
        String key=key(u);restore(key);
        if(!accepted(key)||GpuQualification1961.background()){
            boolean ok=oracle.run(destination,null);check();
            if(ok&&!rejected(key))schedulePrepare(key,source,evidence,u);return ok;
        }
        if(active!=null||!GpuNoise1960.supports(GpuNoise1960.strongProgram(u[10],choice(key))))return false;
        if(!GpuNoise1960.workspaceFits(8L*source.length+8L*width*height+4L*evidence.length+1048576L))return false;
        long start=System.nanoTime();Result candidate=ephemeral(source,evidence,null,null,u,null,choice(key));
        long gpu=System.nanoTime()-start;check();
        if(candidate==UNAVAILABLE)return false;if(candidate==null){fail(key);return false;}
        observe(key,gpu);System.arraycopy(candidate.pixels,0,destination,0,width*height);return true;
    }
    static boolean process(int[] source,int[] destination,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            StrongNoise1958.Model model,int[] policy,int[] confidence,
            GpuPolicy1960.Protection gpuProtection,Oracle oracle){
        // Record one completed GPU commit or one selected CPU route per strip.
        // A cancellation throws before the counter and cannot claim completion.
        int result=processChoice1967(source,destination,width,rows,begin,end,validBegin,validEnd,
            originY,noise,shadows,model,policy,confidence,gpuProtection,oracle);
        Stage stage=active;
        if(stage!=null&&stage.model==model&&noise>0&&end>begin&&!interrupted())synchronized(stage){
            if(!stage.closed){if(result==1)stage.gpuStrips++;else stage.cpuStrips++;}
        }
        return result>=0;
    }
    private static void reason1970(Stage stage,int reason){
        if(reason<0||reason>=REASONS.length)return;
        if(stage==null){ProcessingTiming1947.strongGpuReason1970(ProcessingTiming1947.current(),REASONS[reason],1);return;}
        synchronized(stage){if(!stage.closed&&stage.reasons[reason]<65535)stage.reasons[reason]++;}
    }
    private static void verification1970(Stage stage){
        if(stage==null){ProcessingTiming1947.strongGpuVerification1970(ProcessingTiming1947.current(),1);return;}
        synchronized(stage){if(!stage.closed&&stage.verificationCpu<65535)stage.verificationCpu++;}
    }
    private static long add1973(long a,long b){return b<=0?a:a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
    private static void work1973(Stage stage,int kind,long elapsed){
        if(stage==null||elapsed<0)return;synchronized(stage){if(stage.closed)return;
            if(kind==0)stage.cpuWork1973=add1973(stage.cpuWork1973,elapsed);
            else if(kind==1)stage.gpuWork1973=add1973(stage.gpuWork1973,elapsed);
            else stage.waitWork1973=add1973(stage.waitWork1973,elapsed);}
    }
    private static boolean oracle1973(Stage stage,Oracle oracle,int[] destination,int[] confidence){
        check();long start=System.nanoTime();try{return oracle.run(destination,confidence);}finally{work1973(stage,0,System.nanoTime()-start);}
    }
    private static int cpu1973(Stage stage,String reason,Oracle oracle,int[] destination,int[] confidence){
        reason1971(stage,reason);boolean ok=oracle1973(stage,oracle,destination,confidence);check();return ok?0:-1;
    }
    private static int cpu1970(Stage stage,int reason,Oracle oracle,int[] destination,int[] confidence){
        reason1970(stage,reason);boolean ok=oracle1973(stage,oracle,destination,confidence);check();return ok?0:-1;
    }
    /** FIFO waiting is bounded by the four production workers and an absolute
     * deadline. A verified GPU is not discarded merely because CPU is faster. */
    private static int claimPreferred1970(Stage stage,long requestStart){
        if(stage.closed||stage.unavailable||stage.failed||stage.preferredWaiters.size()>=MAX_PREFERRED_WAITERS)return -1;
        Long turn=Long.valueOf(++stage.preferredSequence);stage.preferredWaiters.addLast(turn);
        try{
            for(;;){
                check();if(stage.closed||stage.unavailable||stage.failed)return -1;
                long now=System.nanoTime(),remaining=PREFERRED_WAIT_NS-(now-requestStart);
                if(remaining<=0)return -1;
                if(stage.preferredWaiters.peekFirst()==turn)for(int bank=0;bank<stage.banks.length;bank++)if(!stage.banks[bank]){
                    stage.preferredWaiters.removeFirst();stage.banks[bank]=true;stage.bankStart[bank]=now;stage.bankDuration[bank]=0;return bank;
                }
                try{stage.wait(Math.min(remaining,50000000L)/1000000L,(int)(Math.min(remaining,50000000L)%1000000L));}
                catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new CancellationException("GPU preferred bank wait interrupted");}
            }
        }finally{stage.preferredWaiters.remove(turn);stage.notifyAll();}
    }
    private static boolean usable1970(Stage stage,int bank,StrongNoise1958.Model model){
        return stage==null||!stage.closed&&!stage.unavailable&&!stage.failed&&stage.model==model&&bank>=0&&stage.banks[bank];
    }
    private static String genericKey1971(int[] u){
        return "strong-gx1971-generic-policy-bank-v1:"+key(u).substring("strong-gx1964-parallel-policy-bank-v1:".length());
    }
    private static int program1971(boolean generic,int variant){
        return generic?GpuNoise1960.variant(GpuNoise1960.STRONG,variant):GpuNoise1960.strongProgram(3,variant);
    }
    private static boolean supported1971(boolean generic){
        for(int variant=0;variant<3;variant++)if(GpuNoise1960.supports(program1971(generic,variant)))return true;
        return false;
    }
    private static void reason1971(Stage stage,String code){
        if(code==null)code="unknown";
        for(int i=0;i<REASONS.length;i++)if(REASONS[i].equals(code)){reason1970(stage,i);return;}
        reason1970(stage,11);
    }
    private static final class Failure1971 {
        final String kind;final int program,layout,width,rows,x,y,nativeCode,begin,sourceRows;final long delta,firstY;
        Failure1971(String kind,int program,int layout,int width,int rows,int x,int y,long delta,int nativeCode,int[] u){
            this.kind=kind;this.program=program;this.layout=layout;this.width=width;this.rows=rows;this.x=x;this.y=y;this.delta=delta;this.nativeCode=nativeCode;begin=u[2];sourceRows=u[1];firstY=(long)u[6]+u[2];
        }
    }
    private static void trial1971(Stage stage,String kind,int program,int layout,int[] u,int x,int y,long delta){
        trial1971(stage,kind,program,layout,u,x,y,delta,0);
    }
    private static void trial1971(Stage stage,String kind,int program,int layout,int[] u,int x,int y,long delta,int nativeCode){
        int which=-1;for(int i=0;i<TRIALS1971.length;i++)if(TRIALS1971[i].equals(kind)){which=i;break;}
        if(which<0)return;
        if(stage==null){
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.current();
            ProcessingTiming1947.strongGpuTrial1971(trace,kind,1);
            ProcessingTiming1947.strongGpuFailure1971(trace,kind,program,layout,u[0],u[3]-u[2],x,y,delta);return;
        }
        synchronized(stage){if(stage.closed)return;if(stage.trials1971[which]<65535)stage.trials1971[which]++;
            if(stage.firstFailure1971==null)stage.firstFailure1971=new Failure1971(kind,program,layout,u[0],u[3]-u[2],x,y,delta,nativeCode,u);}
    }
    private static void fail1971(String key,String cause){
        synchronized(GATES){Gate gate=GATES.get(key);if(gate!=null){gate.rejected=true;gate.accepted=false;gate.consecutive=0;}}
        GpuQualification1961.rejectExact1971(key,cause);
    }
    private static long argbDelta1971(int a,int b){
        long delta=0;for(int shift=0;shift<=24;shift+=8)delta=Math.max(delta,Math.abs(((a>>>shift)&255)-((b>>>shift)&255)));return delta;
    }
    private static String mismatch1973(Stage stage,Result candidate,int[] destination,int[] confidence,int[] u,int variant,int profile){
        int count=u[0]*(u[3]-u[2]),at=u[2]*u[0],first=-1,firstCf=-1,max=0;long bad=0,badCf=u[12]!=0?0:-1,maxCf=u[12]!=0?0:-1;
        for(int i=0;i<count;i++)if(candidate.pixels[i]!=destination[at+i]){if(first<0)first=i;bad++;max=Math.max(max,(int)argbDelta1971(candidate.pixels[i],destination[at+i]));}
        if(u[12]!=0){int n=((u[0]+3)/4)*((u[3]-u[2]+3)/4);
            for(int i=0;i<n;i++)if(candidate.confidence[i]!=confidence[i]){if(firstCf<0)firstCf=i;badCf++;maxCf=Math.max(maxCf,Math.abs((long)candidate.confidence[i]-confidence[i]));}}
        if(bad==0&&(u[12]==0||badCf==0))return null;
        String kind=bad>0?"argb_mismatch":"confidence_mismatch";int cols=(u[0]+3)/4;
        int x=bad>0?first%u[0]:Math.min(u[0]-1,(firstCf%cols)*4+1);
        int y=bad>0?first/u[0]:Math.min(u[3]-u[2]-1,(firstCf/cols)*4+1);
        long delta=bad>0?argbDelta1971(candidate.pixels[first],destination[at+first]):Math.abs((long)candidate.confidence[firstCf]-confidence[firstCf]);
        if(stage!=null)synchronized(stage){boolean capture=stage.firstFailure1971==null;
            trial1971(stage,kind,program1973(profile,variant),variant,u,x,y,delta);
            if(capture){stage.firstArgbCount1973=bad;stage.firstArgbMax1973=max;stage.firstConfidenceCount1973=badCf;stage.firstConfidenceMax1973=maxCf;}}
        else trial1971(null,kind,program1973(profile,variant),variant,u,x,y,delta);
        return kind;
    }
    private static boolean stableReference1973(Result first,int[] destination,int[] confidence,int[] u){
        int count=u[0]*(u[3]-u[2]),at=u[2]*u[0];for(int i=0;i<count;i++)if(first.pixels[i]!=destination[at+i])return false;
        if(u[12]!=0){int n=((u[0]+3)/4)*((u[3]-u[2]+3)/4);for(int i=0;i<n;i++)if(first.confidence[i]!=confidence[i])return false;}return true;
    }
    static final class Read1971 {
        final Result result;final String failure;final int nativeCode;
        Read1971(Result result,String failure){this(result,failure,0);}
        Read1971(Result result,String failure,int nativeCode){this.result=result;this.failure=failure;this.nativeCode=nativeCode;}
    }
    private static Read1971 nativeRead1971(GpuNoise1960.Session session,String fallback){
        return new Read1971(null,nativeReason1971(session,fallback),session.failureCode1971());
    }
    static Read1971 read1971(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank){
        int[][] values;
        try{values=session.collect(ticket,readSlots(u,bank),readCounts(u));}
        catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return nativeRead1971(session,"readback_failed");}
        catch(LinkageError unavailable){return nativeRead1971(session,"readback_failed");}
        catch(OutOfMemoryError unavailable){return new Read1971(null,"memory_budget");}
        if(values==null)return nativeRead1971(session,"native_failure_unknown");
        return result1971(values,u);
    }
    private static Read1971 result1971(int[][] values,int[] u){
        if(values==null)return new Read1971(null,"native_failure_unknown");
        int count=u[0]*(u[3]-u[2]),cf=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):0;
        if(values.length!=(cf!=0?3:2)||values[values.length-1]==null||values[values.length-1].length!=1||
            values[0]==null||values[0].length!=count||cf!=0&&(values[1]==null||values[1].length!=cf))return new Read1971(null,"output_shape_failed");
        if(values[values.length-1][0]!=0)return new Read1971(null,"policy_failure");
        return new Read1971(new Result(values[0],cf!=0?values[1]:null),null);
    }
    /** Buffers are mutable only under the owning bank. A failed collection is
     * never committed; the next valid kernel overwrites every logical output. */
    private static final class Readback1975 {
        final int[][] values;final Result result;
        Readback1975(int[] counts,Readback1975 previous){
            values=new int[counts.length][];
            for(int i=0;i<counts.length;i++)values[i]=previous!=null&&previous.values.length==counts.length&&previous.values[i].length==counts[i]?previous.values[i]:new int[counts[i]];
            result=new Result(values[0],counts.length==3?values[1]:null);
        }
    }
    private static long growth1975(int[] counts,Readback1975 previous){
        long bytes=0;for(int i=0;i<counts.length;i++)if(previous==null||previous.values.length!=counts.length||previous.values[i].length!=counts[i])bytes+=4L*counts[i];return bytes;
    }
    private static Read1971 read1975(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank,Readback1975 target){
        try{
            int[] counts=readCounts(u);boolean complete=session.collectManyInto(ticket,readSlots(u,bank),counts,target.values,new int[counts.length]);
            if(!complete)return nativeRead1971(session,"readback_failed");
            if(target.values[target.values.length-1][0]!=0)return new Read1971(null,"policy_failure");
            return new Read1971(target.result,null);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return nativeRead1971(session,"readback_failed");}
        catch(LinkageError unavailable){return nativeRead1971(session,"readback_failed");}
        catch(OutOfMemoryError unavailable){return new Read1971(null,"memory_budget");}
    }
    static Read1971 readCohort1978(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank,int[][] targets){
        if(!session.collectManyInto(ticket,readSlots(u,bank),readCounts(u),targets,new int[targets.length]))return nativeRead1971(session,"readback_failed");
        if(targets[targets.length-1][0]!=0)return new Read1971(null,"policy_failure");
        return new Read1971(new Result(targets[0],u[12]==0?null:targets[1]),null);
    }
    /** Certified resident pixels never cross JNI. Confidence and policy result
     * remain CPU-visible for the unchanged protection-mask owner. */
    private static Read1971 readResident1976(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank,Readback1975 target) {
        int last=target.values.length-1;
        int[] slots=u[12]!=0?new int[]{bank[3],bank[9]}:new int[]{bank[9]};
        int[][] values=u[12]!=0?new int[][]{target.values[1],target.values[last]}:new int[][]{target.values[last]};
        int[] counts=new int[values.length];for(int i=0;i<counts.length;i++)counts[i]=values[i].length;
        if(!session.collectManyInto(ticket,slots,counts,values,new int[counts.length]))return nativeRead1971(session,"readback_failed");
        if(target.values[last][0]!=0)return new Read1971(null,"policy_failure");
        return new Read1971(new Result(null,u[12]!=0?target.values[1]:null),null);
    }
    private static final ThreadLocal<int[]> RESIDENT_ROW1976=new ThreadLocal<int[]>();
    static boolean cpuOracleStage1976(StrongNoise1958.Model model) {
        Stage stage=active;return stage!=null&&stage.model==model&&stage.cpuOracle1976;
    }
    static boolean enableResident1976(StrongNoise1958.Model model,int width,int height) {
        Stage stage=active;if(stage==null||stage.model!=model)return false;
        synchronized(stage) {
            if(!initialize1971(stage))return false;
            GpuNoise1960.Lease1971 lease=stage.session.reserveCapacity1971(new int[]{24},new long[]{4L*width*height},0);
            if(lease==null)return false;
            try {if(!lease.revalidate1971()||!stage.session.allocate(24,4L*width*height))return false;stage.resident1976=true;return true;}
            finally {lease.close();}
        }
    }
    /** Worker calls exactly once after processRange; a CPU/cold fallback is
     * copied directly from its worker-owned output without touching Bitmap. */
    static void commitResident1976(StrongNoise1958.Model model,int[] output,int offset,int width,int first,int rows) {
        int[] committed=RESIDENT_ROW1976.get();RESIDENT_ROW1976.remove();
        Stage stage=active;if(stage==null||stage.model!=model)throw new IllegalStateException("resident stage ownership");
        synchronized(stage) {
            check();
            if(!stage.resident1976||stage.failed||stage.session==null)throw new IllegalStateException("resident stage unavailable");
            if(committed!=null&&committed[0]==first&&committed[1]==rows)return;
            if(!stage.session.uploadRange1976(24,first*width,output,offset,width*rows)){stage.failed=true;throw new IllegalStateException("resident CPU row upload");}
            stage.residentRows1976+=rows;
        }
    }
    static GpuNoise1960.Session takeResident1976(StrongNoise1958.Model model,int height) {
        Stage stage=active;if(stage==null||stage.model!=model)return null;
        synchronized(stage) {
            if(!stage.resident1976||stage.failed||stage.residentRows1976!=height||stage.banks[0]||stage.banks[1])return null;
            GpuNoise1960.Session session=stage.session;stage.session=null;return session;
        }
    }
    // .78 retains profiles0..3 and every old key. Direct policy and power-of-two
    // arithmetic are separate proof namespaces, never inherited certificates.
    static final int PROFILES1978=8;
    static String profileKey1973(int[] u,int profile){
        if(profile<0||profile>=PROFILES1978)throw new IllegalArgumentException("strong profile");
        String suffix=key(u).substring("strong-gx1964-parallel-policy-bank-v1:".length());
        if(profile==4)return "strong-gx1978-policy-direct-ieee-v1:"+suffix;
        if(profile==5)return "strong-gx1978-policy-direct-tile8-v1:"+suffix;
        if(profile==6)return "strong-gx1978-policy-direct-pow2-v1:"+suffix;
        if(profile==7)return "strong-gx1978-policy-direct-pow2-tile8-v1:"+suffix;
        return profile==3?"strong-gx1976-ieee-tile8-policy-bank-v1:"+suffix:profile==0?"strong-gx1973-ieee-div-policy-bank-v1:"+suffix:profile==1?key(u):genericKey1971(u);
    }
    static boolean directPolicy1978(int profile){return profile>=4&&profile<PROFILES1978;}
    static int program1973(int profile,int variant){
        if(profile<0||profile>=PROFILES1978||variant<0||variant>2)return -1;
        return profile==7?48+variant:profile==6?45+variant:profile==3||profile==5?42+variant:profile==0||profile==4?39+variant:program1971(profile==2,variant);
    }
    private static int untested1973(String key){
        synchronized(GATES){Gate gate=GATES.get(key);int mask=gate==null?0:gate.badVariants1973|gate.unsupportedVariants1973;
            for(int variant=0;variant<3;variant++)if((mask&(1<<variant))==0)return variant;return -1;}
    }
    private static void variant1973(String key,int variant,String mismatch){
        boolean reject=false;synchronized(GATES){Gate gate=GATES.get(key);if(gate==null){gate=new Gate();GATES.put(key,gate);}
            if(mismatch==null)gate.unsupportedVariants1973|=1<<variant;else gate.badVariants1973|=1<<variant;
            reject=gate.badVariants1973==7;while(GATES.size()>LIMIT)GATES.remove(GATES.keySet().iterator().next());}
        if(reject)fail1971(key,mismatch);
    }
    private static int freeBank1973(Stage stage){
        check();if(stage.closed||stage.failed||stage.unavailable)return -1;
        for(int i=0;i<stage.banks.length;i++)if(!stage.banks[i]){stage.banks[i]=true;stage.bankStart[i]=System.nanoTime();return i;}return -1;
    }
    private static int processChoice1967(int[] source,int[] destination,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            StrongNoise1958.Model model,int[] policy,int[] confidence,GpuPolicy1960.Protection protection,Oracle oracle){
        RESIDENT_ROW1976.remove();
        if(model==null||noise<=0||end<=begin||interrupted())return -1;
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=validBegin;u[5]=validEnd;u[6]=originY;
        u[7]=model.height;u[8]=noise;u[9]=shadows?1:0;u[10]=3;u[11]=policy==null?0:1;u[12]=confidence==null?0:1;
        if(protection!=null&&protection.plan!=null){u[18]=protection.plan.beautyQ8;u[19]=protection.plan.shadowBudgetQ8;}
        Stage stage=active;if(stage!=null&&stage.model!=model)return cpu1970(null,4,oracle,destination,confidence);
        if(stage!=null&&stage.cpuOracle1976)return cpu1970(stage,4,oracle,destination,confidence);
        if(GpuQualification1961.background()&&!(stage!=null&&stage.benchmark1976)){boolean ok=oracle.run(destination,confidence);check();if(ok&&!rejected(key(u)))scheduleFull(key(u),source,policy,u,model,protection);return ok?0:-1;}
        long warm=System.nanoTime();boolean environment;
        try{environment=GpuNoise1960.warmEnvironment1973();}
        catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){environment=false;}
        catch(LinkageError unavailable){environment=false;}
        catch(OutOfMemoryError unavailable){environment=false;}
        finally{work1973(stage,1,System.nanoTime()-warm);}
        check();if(!environment)return cpu1973(stage,"shader_unsupported",oracle,destination,confidence);
        int profile=-1,variant=-1;String selectedKey=null;boolean certified=false;
        GpuStrongTuning1975.Choice preferred1975=GpuStrongTuning1975.select(u);
        if(preferred1975==null)preferred1975=GpuStrongTuning1975.selectCpu1978(u,stage==null?1:stage.workers1978);
        if(preferred1975!=null){restore(preferred1975.key);if(!GpuQualification1961.exactRejected(preferred1975.key)&&accepted(preferred1975.key)){
            profile=preferred1975.profile;variant=preferred1975.variant;selectedKey=preferred1975.key;certified=true;}}
        // New profiles require the selected tuning aggregate as well as their
        // exact child proof; a slower passing candidate alone is not admission.
        if(!certified)for(int p=0;p<4;p++){String k=profileKey1973(u,p);restore(k);if(!GpuQualification1961.exactRejected(k)&&accepted(k)){profile=p;variant=choice(k);selectedKey=k;certified=true;break;}}
        // .78 never creates a new GPU proof while a capture is being saved.
        // The existing CPU oracle finishes this strip; immutable inputs may
        // enter the bounded idle queue only while their owner still holds them.
        if(!certified){
            ProcessingTiming1947.strongRoute1978(stage==null?ProcessingTiming1947.current():stage.trace,2);
            try{
                int result=cpu1973(stage,"deferred_quality1978",oracle,destination,confidence);
                if(result==0&&(stage==null||!stage.cpuOracle1976&&!stage.benchmark1976))
                    GpuStrongTuning1975.schedule1978(source,policy,u,model,protection,stage==null?1:stage.workers1978);
                return result;
            }catch(CancellationException cancelled){throw cancelled;}
            catch(RuntimeException unavailable){return -1;}
            catch(LinkageError unavailable){return -1;}
            catch(OutOfMemoryError unavailable){return -1;}
        }
        Flight1973 flight=null;int bank=-1;boolean cpuReady=false,gpuCommitted=false;GpuNoise1960.Lease1971 lease=null;
        long minCpu=Long.MAX_VALUE,maxGpu=0;int count=width*(end-begin),cf=confidence==null?0:((width+3)/4)*((end-begin+3)/4);
        try{
            if(!certified){flight=admit1973(stage,model,key(u));if(flight.failure!=null){
                String admissionReason=flight.failure;flight=null;
                if("proof_in_flight".equals(admissionReason)){
                    long wait=System.nanoTime();try{awaitProof1974(key(u));}finally{work1973(stage,2,System.nanoTime()-wait);}
                    for(int p=0;p<3;p++){String k=profileKey1973(u,p);restore(k);
                        if(!GpuQualification1961.exactRejected(k)&&accepted(k)){profile=p;variant=choice(k);selectedKey=k;certified=true;break;}}
                }
                if(!certified)return cpu1973(stage,admissionReason,oracle,destination,confidence);
            }}
            String admissionFailure=null;boolean arrivedCertified=certified;
            // A resident output has an additional CPU-to-slot24 upload cost and
            // cannot borrow a nonresident cohort's timing proof.
            boolean mixed1978=stage!=null&&!stage.resident1976&&GpuStrongRouting1978.accepted(u,profile,variant,stage.workers1978);
            if(stage!=null)synchronized(stage){
                if(mixed1978&&!stage.closed&&!stage.failed&&!stage.unavailable&&stage.banks[0]&&stage.banks[1])admissionFailure="cpu_faster1978";
                else{long wait=System.nanoTime();bank=certified?claimPreferred1970(stage,wait):freeBank1973(stage);
                    if(certified)work1973(stage,2,System.nanoTime()-wait);
                    if(bank<0)admissionFailure=stage.unavailableReason1971!=null?stage.unavailableReason1971:certified?"bank_busy":"cold_bank_busy";}
            }
            if(admissionFailure!=null){
                if("cpu_faster1978".equals(admissionFailure))ProcessingTiming1947.strongRoute1978(stage.trace,1);
                return cpu1973(stage,admissionFailure,oracle,destination,confidence);
            }
            // A waiting certified caller must refresh both positive and negative
            // state before doing any CPU proof or allocating a model.
            if(GpuQualification1961.exactRejected(selectedKey))return cpu1973(stage,GpuQualification1961.exactFailure1971(selectedKey),oracle,destination,confidence);
            restore(selectedKey);certified=accepted(selectedKey);
            if(arrivedCertified&&!certified)return cpu1973(stage,"awaiting_quality",oracle,destination,confidence);
            if(certified)variant=choice(selectedKey);
            long support=System.nanoTime();boolean supported=GpuNoise1960.supports(program1973(profile,variant));work1973(stage,1,System.nanoTime()-support);
            if(!supported){if(!certified)variant1973(selectedKey,variant,null);return cpu1973(stage,"shader_unsupported",oracle,destination,confidence);}
            if(GpuQualification1961.exactRejected(selectedKey))return cpu1973(stage,GpuQualification1961.exactFailure1971(selectedKey),oracle,destination,confidence);
            // Admission precedes the costly CPU reference: an unavailable GPU
            // now incurs only the caller's ordinary CPU fallback, never a proof.
            GpuPolicy1960.PolicyData descriptor=directPolicy1978(profile)||protection==null?null:protection.data(width,end-begin,originY+begin);
            if(stage!=null)synchronized(stage){long setup=System.nanoTime();boolean ready=initialize1971(stage);work1973(stage,1,System.nanoTime()-setup);
                if(!ready){if(stage.unavailableCode1971>0&&!stage.initialFailureReported1971){stage.initialFailureReported1971=true;
                        trial1971(stage,"memory_budget".equals(stage.unavailableReason1971)?"native_failure_unknown":stage.unavailableReason1971,program1973(profile,variant),variant,u,-1,-1,-1,stage.unavailableCode1971);}
                    reason1971(stage,stage.unavailableReason1971);return cpuReady?0:-1;}
                int[] counts=readCounts(u);long growth=256L;
                for(int trial=0;trial<(certified?1:2);trial++)growth+=growth1975(counts,stage.readbacks1975[bank][trial]);
                lease=reserveRange1975(stage.session,source,policy,u,descriptor,BANKS[bank],growth);
                if(lease==null){reason1971(stage,"memory_budget");return -1;}
                for(int trial=0;trial<(certified?1:2);trial++){
                    Readback1975 previous=stage.readbacks1975[bank][trial];long bytes=growth1975(counts,previous);
                    if(bytes>0||previous==null){stage.readbacks1975[bank][trial]=new Readback1975(counts,previous);lease.consumeJava1974(bytes);}
                }
            }
            Result first=null,candidate=null;int trials=certified?1:2;
            for(int trial=0;trial<trials;trial++){
                boolean overlap=stage!=null&&!certified&&lease.tryReserveScratch1975(StrongNoise1958.workspaceBytes(width,end-begin));
                GpuNoise1960.Ticket pending1975=null;
                try{
                    // If the concurrent peak cannot fit, preserve .74's serial
                    // verification route; GPU admission itself remains valid.
                    if(!certified&&!overlap){
                        cpuReady=false;long start=System.nanoTime();if(!oracle1973(stage,oracle,destination,confidence))return -1;
                        minCpu=Math.min(minCpu,Math.max(1L,System.nanoTime()-start));cpuReady=true;verification1970(stage);check();
                        if(trial==1&&!stableReference1973(first,destination,confidence,u)){reason1971(stage,"quality_rejected");return 0;}
                    }
                    check();if(GpuQualification1961.exactRejected(selectedKey)){reason1971(stage,GpuQualification1961.exactFailure1971(selectedKey));return cpuReady?0:-1;}
                    long request=System.nanoTime();Read1971 read;
                    try{
                        if(stage!=null){
                            synchronized(stage){check();if(!usable1970(stage,bank,model)){reason1971(stage,"execution_failed");return cpuReady?0:-1;}
                                if(!lease.revalidate1971()){reason1971(stage,"memory_budget");return cpuReady?0:-1;}
                                commit1974(flight);
                                pending1975=stage.session.submit(trial==0?commands1973(source,policy,u,descriptor,BANKS[bank],variant,profile):commandsRepeat1975(u,descriptor,BANKS[bank],variant,profile),bank);
                            }
                            if(pending1975==null)read=nativeRead1971(stage.session,"submit_failed");
                            else{
                                if(overlap){
                                    // The original Oracle stays on its owning
                                    // worker. Source/policy uploads have already
                                    // completed; only GPU computation overlaps.
                                    cpuReady=false;long start=System.nanoTime();
                                    if(!oracle1973(stage,oracle,destination,confidence)){synchronized(stage){stage.failed=true;}return -1;}
                                    minCpu=Math.min(minCpu,Math.max(1L,System.nanoTime()-start));cpuReady=true;verification1970(stage);check();
                                    lease.releaseScratch1975();overlap=false;
                                    if(trial==1&&!stableReference1973(first,destination,confidence,u)){synchronized(stage){stage.failed=true;}reason1971(stage,"quality_rejected");return 0;}
                                }
                                read=certified&&stage.resident1976?readResident1976(stage.session,pending1975,u,BANKS[bank],stage.readbacks1975[bank][trial]):read1975(stage.session,pending1975,u,BANKS[bank],stage.readbacks1975[bank][trial]);
                                if(read.failure==null)pending1975=null;
                            }
                        }else{commit1974(flight);read=ephemeral1973(source,StrongNoise1958.gpuEvidence1960(model),StrongNoise1958.gpuMaps1960(model),policy,u,descriptor,variant,profile);}
                    }finally{work1973(stage,1,System.nanoTime()-request);}
                    check();candidate=read.result;
                    if(read.failure!=null){trial1971(stage,"memory_budget".equals(read.failure)?"native_failure_unknown":read.failure,program1973(profile,variant),variant,u,-1,-1,-1,read.nativeCode);
                        if("policy_failure".equals(read.failure))fail1971(selectedKey,"policy_failure");
                        if(stage!=null)synchronized(stage){stage.failed=true;stage.unavailableReason1971=read.failure;}
                        reason1971(stage,read.failure);return cpuReady?0:-1;}
                    maxGpu=Math.max(maxGpu,Math.max(1L,System.nanoTime()-request));
                    if(!certified){String mismatch=mismatch1973(stage,candidate,destination,confidence,u,variant,profile);
                        if(mismatch!=null){variant1973(selectedKey,variant,mismatch);reason1971(stage,"argb_mismatch".equals(mismatch)?"new_argb_mismatch":"new_confidence_mismatch");return 0;}}
                    if(trial==0)first=candidate;
                }finally{
                    // An oracle exception/false/cancellation can leave a real
                    // GPU ticket live. Quarantine the stage before relinquishing
                    // any bank; closeNative drains or quarantines its fences.
                    if(stage!=null&&pending1975!=null)synchronized(stage){stage.failed=true;stage.unavailableReason1971="execution_failed";}
                    if(overlap)lease.releaseScratch1975();
                }
            }
            check();
            if(stage!=null)synchronized(stage){
                if(!usable1970(stage,bank,model)){reason1971(stage,"execution_failed");return cpuReady?0:-1;}
                if(GpuQualification1961.exactRejected(selectedKey)){reason1971(stage,GpuQualification1961.exactFailure1971(selectedKey));return cpuReady?0:-1;}
                check();
                if(stage.resident1976) {
                    if(!stage.session.copy1976(BANKS[bank][1],24,0,(originY+begin)*width,count)){stage.failed=true;throw new IllegalStateException("resident strong copy");}
                    stage.residentRows1976+=end-begin;RESIDENT_ROW1976.set(new int[]{originY+begin,end-begin});
                } else System.arraycopy(candidate.pixels,0,destination,begin*width,count);
                if(cf>0)System.arraycopy(candidate.confidence,0,confidence,0,cf);gpuCommitted=true;
                if(!certified){check();GpuQualification1961.qualifiedStrongPreferred1970(selectedKey,minCpu,maxGpu,variant);restore(selectedKey);}
            }else{if(GpuQualification1961.exactRejected(selectedKey)){reason1971(stage,GpuQualification1961.exactFailure1971(selectedKey));return cpuReady?0:-1;}
                check();System.arraycopy(candidate.pixels,0,destination,begin*width,count);if(cf>0)System.arraycopy(candidate.confidence,0,confidence,0,cf);gpuCommitted=true;
                if(!certified){check();GpuQualification1961.qualifiedStrongPreferred1970(selectedKey,minCpu,maxGpu,variant);restore(selectedKey);}}
            return 1;
        }catch(CancellationException cancelled){reason1970(stage,10);throw cancelled;}
        catch(RuntimeException unavailable){if(gpuCommitted)return 1;reason1971(stage,"execution_failed");return cpuReady?0:-1;}
        catch(LinkageError unavailable){if(gpuCommitted)return 1;reason1971(stage,"execution_failed");return cpuReady?0:-1;}
        catch(OutOfMemoryError unavailable){if(gpuCommitted)return 1;reason1970(stage,3);return cpuReady?0:-1;}
        finally{try{try{if(lease!=null)lease.close();}finally{try{if(stage!=null&&bank>=0)synchronized(stage){stage.banks[bank]=false;stage.bankStart[bank]=0;stage.notifyAll();}}finally{release1973(flight);}}}finally{if(stage==null||!stage.cpuOracle1976&&!stage.benchmark1976){GpuStrongTuning1975.schedule1978(source,policy,u,model,protection,stage==null?1:stage.workers1978);GpuStrongLayout1976.schedule(source,policy,u,model,protection);}}}
    }
    static final class Result {
        final int[] pixels,confidence;
        Result(int[] pixels,int[] confidence){this.pixels=pixels;this.confidence=confidence;}
    }
    private static boolean canSchedule(){return !GpuQualification1961.background()&&!interrupted();}
    private static void schedulePrepare(String key,int[] source,float[] evidence,int[] u){
        if(!canSchedule()||!GpuQualification1961.maySchedule(key))return;long count=(long)u[0]*u[1],bytes=4L*count+256;
        if(count>Integer.MAX_VALUE||bytes>96L*1024*1024||!GpuQualification1961.canQueue(key,bytes)||!GpuNoise1960.workspaceFits(bytes+12L*count))return;
        try{GpuQualification1961.schedule(key,bytes,new Proof(key,Arrays.copyOf(source,(int)count),null,Arrays.copyOf(evidence,16),u.clone(),null,null));}
        catch(RuntimeException unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static void scheduleFull(String key,int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.Protection protection){
        if(!canSchedule()||!GpuQualification1961.maySchedule(key))return;long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
        int[][] maps=StrongNoise1958.gpuMaps1960(model);float[] evidence=StrongNoise1958.gpuEvidence1960(model);
        // Retention includes model-owned original evidence, the copied uniforms
        // and bounded object/array headers, plus the complete private policy.
        long bytes=4L*(count+(policy==null?0:core*2)+maps[0].length+(long)maps[1].length+maps[2].length+evidence.length)+1024L;
        if(protection!=null){
            long gridCells=1;
            if(protection.plan!=null&&protection.plan.localNoise!=null)
                gridCells=Math.max(1L,(long)protection.plan.localNoise.columns*protection.plan.localNoise.rows);
            bytes+=8L*core+256L+4L*gridCells;
        }
        if(count>Integer.MAX_VALUE||core>Integer.MAX_VALUE/2||bytes>96L*1024*1024||!GpuQualification1961.canQueue(key,bytes)||!GpuNoise1960.workspaceFits(bytes+12L*count))return;
        try{
            GpuPolicy1960.PolicyData descriptor=protection==null?null:protection.data(u[0],u[3]-u[2],u[6]+u[2]);
            GpuQualification1961.schedule(key,bytes,new Proof(key,Arrays.copyOf(source,(int)count),policy==null?null:Arrays.copyOf(policy,(int)core*2),null,u.clone(),model,descriptor));
        }catch(RuntimeException unavailable){}catch(OutOfMemoryError unavailable){}
    }
    /** GX22/GX23: private idle-only complete-output proofs for every supported
     * 32/64/128 layout. Source and policy are copied before foreground return;
     * model maps/evidence are immutable and retained only for this one job.
     * No foreground Workspace, pooled input or Oracle closure is reused. */
    private static final class Proof implements GpuQualification1961.Probe {
        final String key;int[] source,policy,u;float[] evidence;
        StrongNoise1958.Model model;GpuPolicy1960.PolicyData descriptor;
        Proof(String key,int[] source,int[] policy,float[] evidence,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor){
            this.key=key;this.source=source;this.policy=policy;this.evidence=evidence;this.u=u;this.model=model;this.descriptor=descriptor;
        }
        private boolean cpu(int[] destination,int[] confidence){
            if(u[10]!=3)return StrongNoise1958.gpuPrepareOracleSnapshot1961(source,destination,u[0],u[1],u[8],u[9]!=0,evidence,u[10]);
            return StrongNoise1958.gpuOracleSnapshot1961(source,destination,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,model,policy,confidence);
        }
        private long parallelCpu(final int[] destination,final int[] confidence,
                GpuQualification1961.Cancellation cancellation) {
            // Preparation already calls prepareScaleCpu's actual bounded
            // production parallel scheduler. Full-resolution strip snapshots
            // are partitioned on NR13's four-row representative-cell boundary.
            if(u[10]!=3){long start=System.nanoTime();if(!cpu(destination,confidence))return 0;return System.nanoTime()-start;}
            return GpuQualification1961.parallelRows1964(u[2],u[3],4,cancellation,
                new GpuQualification1961.RowTask1964(){public boolean run(int first,int last){
                    int[] partPolicy=policy==null?null:Arrays.copyOfRange(policy,(first-u[2])*u[0]*2,(last-u[2])*u[0]*2);
                    int cols=(u[0]+3)/4;
                    int[] partConfidence=confidence==null?null:new int[cols*((last-first+3)/4)];
                    boolean result=StrongNoise1958.gpuOracleSnapshot1961(source,destination,u[0],u[1],first,last,u[4],u[5],u[6],u[8],u[9]!=0,model,partPolicy,partConfidence);
                    if(result&&partConfidence!=null)System.arraycopy(partConfidence,0,confidence,cols*((first-u[2])/4),partConfidence.length);
                    return result;
                }});
        }
        public void run(GpuQualification1961.Cancellation cancellation){
            if(cancellation.cancelled()||source==null)return;
            boolean[] supported=new boolean[3];boolean any=false;
            for(int choice=0;choice<3;choice++){if(cancellation.cancelled())return;supported[choice]=GpuNoise1960.supports(GpuNoise1960.strongProgram(u[10],choice));any|=supported[choice];}
            if(!any||cancellation.cancelled())return;
            int core=u[0]*(u[3]-u[2]),cfCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):0;
            float[] bins=model==null?evidence:StrongNoise1958.gpuEvidence1960(model);int[][] maps=model==null?null:StrongNoise1958.gpuMaps1960(model);
            long nativeModel=4L*bins.length;if(maps!=null)for(int[] map:maps)nativeModel+=4L*map.length;
            if(!GpuNoise1960.workspaceFits(nativeModel+24L*source.length+24L*core))return;
            int[][] expected=new int[2][],confidence=new int[2][];long[] cpuNanos=new long[2];
            for(int trial=0;trial<2;trial++){
                if(cancellation.cancelled())return;expected[trial]=new int[source.length];confidence[trial]=cfCount==0?null:new int[cfCount];
                long start=System.nanoTime();if(!cpu(expected[trial],confidence[trial]))return;
                long serial=System.nanoTime()-start;
                if(u[10]==3) {
                    int[] parallel=new int[source.length],parallelConfidence=cfCount==0?null:new int[cfCount];
                    long measured=parallelCpu(parallel,parallelConfidence,cancellation);
                    if(measured<=0)return;
                    // Split CPU ranges must themselves reproduce the original
                    // complete output before their timing can qualify any GPU.
                    for(int i=0;i<core;i++)if(parallel[u[2]*u[0]+i]!=expected[trial][u[2]*u[0]+i])return;
                    if(cfCount!=0&&!Arrays.equals(parallelConfidence,confidence[trial]))return;
                    cpuNanos[trial]=Math.min(serial,measured);
                } else cpuNanos[trial]=serial;
                if(cancellation.cancelled())return;
            }
            boolean pixelFailure=false;int best=-1;long bestGpu=Long.MAX_VALUE,bestCpu=Math.min(cpuNanos[0],cpuNanos[1]);
            for(int choice=0;choice<3;choice++)if(supported[choice]){
                if(u[10]!=3){
                    // Preparation's live owner is ephemeral per call, so both
                    // proofs must retain its complete open/upload/close cost.
                    boolean wins=true;long worst=0;
                    for(int trial=0;trial<2;trial++){
                        if(cancellation.cancelled())return;
                        int[] committed=new int[core];
                        int[] committedConfidence=cfCount==0?null:new int[cfCount];
                        long start=System.nanoTime();
                        Result actual=ephemeral(source,bins,maps,policy,u,descriptor,choice);
                        if(actual==UNAVAILABLE)return;
                        if(actual==null||actual.pixels==null||actual.pixels.length!=core||
                                cfCount!=0&&(actual.confidence==null||actual.confidence.length!=cfCount)){
                            pixelFailure=true;wins=false;break;
                        }
                        System.arraycopy(actual.pixels,0,committed,0,core);
                        if(cfCount!=0)System.arraycopy(actual.confidence,0,committedConfidence,0,cfCount);
                        long gpu=System.nanoTime()-start;
                        if(cancellation.cancelled())return;
                        for(int i=0;i<core;i++)if(committed[i]!=expected[trial][u[2]*u[0]+i]){
                            pixelFailure=true;wins=false;break;
                        }
                        if(wins&&cfCount!=0&&!Arrays.equals(committedConfidence,confidence[trial])){
                            pixelFailure=true;wins=false;
                        }
                        if(gpu<=0||cpuNanos[trial]<=0||gpu>cpuNanos[trial]-cpuNanos[trial]/20)wins=false;
                        worst=Math.max(worst,gpu);if(!wins)break;
                    }
                    if(wins&&worst<=bestCpu-bestCpu/20&&worst<bestGpu){best=choice;bestGpu=worst;}
                    continue;
                }
                // .67: match the live Stage's ownership. The same immutable
                // model is uploaded once and both complete trials reuse its
                // session. Charge setup and close across only the pixels that
                // were actually measured, never an assumed full photograph.
                long additional=8L*source.length+24L*core+4L*bins.length+1048576L;
                if(maps!=null)for(int[] map:maps)additional+=4L*map.length;
                if(descriptor!=null)additional+=16L*descriptor.count+4L*descriptor.grid.length;
                if(!GpuNoise1960.workspaceFits(additional))return;
                boolean wins=true;int completedTrials=0;long overhead=0;
                long[] times=new long[2];
                long opened=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
                if(session==null)return;
                try {
                    boolean uploaded=session.upload(2,bins);
                    for(int k=0;k<3&&uploaded;k++)uploaded=session.upload(4+k,maps==null?new int[]{0}:maps[k]);
                    overhead=System.nanoTime()-opened;
                    if(!uploaded){pixelFailure=true;wins=false;}
                    else for(int trial=0;trial<2;trial++){
                        if(cancellation.cancelled())return;
                        int[] committed=new int[core];
                        int[] committedConfidence=cfCount==0?null:new int[cfCount];
                        long start=System.nanoTime();
                        Result actual=execute(session,source,policy,u,descriptor,choice);
                        if(actual==null||actual.pixels==null||actual.pixels.length!=core||
                                cfCount!=0&&(actual.confidence==null||actual.confidence.length!=cfCount)){
                            pixelFailure=true;wins=false;break;
                        }
                        // Include the foreground's full final output commit.
                        System.arraycopy(actual.pixels,0,committed,0,core);
                        if(cfCount!=0)System.arraycopy(actual.confidence,0,committedConfidence,0,cfCount);
                        times[trial]=System.nanoTime()-start;completedTrials++;
                        if(cancellation.cancelled())return;
                        for(int i=0;i<core;i++)if(committed[i]!=expected[trial][u[2]*u[0]+i]){
                            pixelFailure=true;wins=false;break;
                        }
                        if(wins&&cfCount!=0&&!Arrays.equals(committedConfidence,confidence[trial])){
                            pixelFailure=true;wins=false;
                        }
                        if(!wins)break;
                    }
                } finally {
                    long closing=System.nanoTime();
                    try{session.close();}finally{overhead+=System.nanoTime()-closing;}
                }
                if(cancellation.cancelled())return;
                long worst=0;
                if(completedTrials!=2)wins=false;
                if(wins){
                    // Identical to endStage's measured pixel-share formula.
                    long measuredPixels=2L*core;
                    long share=(long)((double)overhead*core/measuredPixels);
                    for(int trial=0;trial<2;trial++){
                        long gpu=times[trial]+share;
                        if(gpu<=0||cpuNanos[trial]<=0)wins=false;
                        worst=Math.max(worst,gpu);
                    }
                }
                if(wins&&worst<bestGpu){best=choice;bestGpu=worst;}
            }
            if(cancellation.cancelled())return;
            if(best<0){if(pixelFailure)fail(key);else slow(key);return;}
            if(u[10]==3)GpuQualification1961.qualifiedStrongPreferred1970(key,bestCpu,bestGpu,best);
            else GpuQualification1961.qualified(key,bestCpu,bestGpu,best);
            if(!cancellation.cancelled())restore(key);
        }
        public void close(){source=null;policy=null;u=null;evidence=null;model=null;descriptor=null;}
    }
    private static final Result UNAVAILABLE=new Result(null,null);
    private static Result ephemeral(int[] source,float[] evidence,int[][] maps,int[] policy,
            int[] u,GpuPolicy1960.PolicyData gpuPolicy,int variant){
        long additional=8L*source.length+24L*u[0]*(u[3]-u[2])+4L*evidence.length+1048576L;
        if(maps!=null)for(int[] map:maps)additional+=4L*map.length;
        if(gpuPolicy!=null)additional+=16L*gpuPolicy.count+4L*gpuPolicy.grid.length;
        if(!GpuNoise1960.workspaceFits(additional))return UNAVAILABLE;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try{
            if(!session.upload(2,evidence))return null;
            for(int k=0;k<3;k++)if(!session.upload(4+k,maps==null?new int[]{0}:maps[k]))return null;
            return execute(session,source,policy,u,gpuPolicy,variant);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
        finally{session.close();}
    }
    static Read1971 ephemeral1973(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int variant,int profile){
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return new Read1971(null,"session_unavailable");
        GpuNoise1960.Lease1971 modelLease=null,rangeLease=null;
        try{
            modelLease=session.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
            if(modelLease==null)return new Read1971(null,"memory_budget");
            if(!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2]))return nativeRead1971(session,"upload_failed");
            modelLease.close();modelLease=null;rangeLease=reserveRange1971(session,source,policy,u,descriptor,BANKS[0]);
            if(rangeLease==null)return new Read1971(null,"memory_budget");
            check();if(!rangeLease.revalidate1971())return new Read1971(null,"memory_budget");
            GpuNoise1960.Ticket ticket=session.submit(commands1973(source,policy,u,descriptor,BANKS[0],variant,profile),0);
            return ticket==null?nativeRead1971(session,"submit_failed"):read1971(session,ticket,u,BANKS[0]);
        }finally{try{if(rangeLease!=null)rangeLease.close();}finally{try{if(modelLease!=null)modelLease.close();}finally{session.close();}}}
    }
    private static Read1971 ephemeral1971(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,GpuPolicy1960.PolicyData descriptor,int variant,boolean generic){
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return new Read1971(null,"session_unavailable");
        GpuNoise1960.Lease1971 modelLease=null,rangeLease=null;
        try{
            modelLease=session.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
            if(modelLease==null)return new Read1971(null,"memory_budget");
            if(!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2]))return nativeRead1971(session,"upload_failed");
            modelLease.close();modelLease=null;rangeLease=reserveRange1971(session,source,policy,u,descriptor,BANKS[0]);
            if(rangeLease==null)return new Read1971(null,"memory_budget");
            check();if(!rangeLease.revalidate1971())return new Read1971(null,"memory_budget");
            GpuNoise1960.Ticket ticket=session.submit(commands1971(source,policy,u,descriptor,BANKS[0],variant,generic),0);
            return ticket==null?nativeRead1971(session,"submit_failed"):read1971(session,ticket,u,BANKS[0]);
        }finally{if(rangeLease!=null)rangeLease.close();if(modelLease!=null)modelLease.close();session.close();}
    }
    /** Trial2 owns the same bank and immutable inputs. Re-execute policy and
     * strong arithmetic completely, without re-uploading source/masks/grid. */
    static GpuNoise1960.Batch commandsRepeat1975(int[] u,GpuPolicy1960.PolicyData gpuPolicy,int[] bank,int variant,int profile){
        if(directPolicy1978(profile))gpuPolicy=null;
        int count=u[0]*(u[3]-u[2]);GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
        batch.upload(bank[9],new int[]{0});
        if(gpuPolicy!=null){
            batch.dispatch(GpuNoise1960.ANALYSIS,new int[]{bank[7],bank[7],bank[6],bank[2],bank[5],bank[4]},gpuPolicy.u,gpuPolicy.f,count);
            int[] compare=new int[32];compare[0]=count*2;
            batch.dispatch(GpuNoise1960.COMPARE1961,new int[]{bank[8],bank[2],bank[9]},compare,null,count*2);
        }
        return batch.dispatch(program1973(profile,variant),new int[]{bank[0],bank[1],2,bank[2],4,5,6,bank[3]},u,null,count);
    }
    static GpuNoise1960.Batch commands1973(int[] source,int[] policy,int[] u,
            GpuPolicy1960.PolicyData gpuPolicy,int[] bank,int variant,int profile){
        if(directPolicy1978(profile))gpuPolicy=null;
        if(profile==1)return commands(source,policy,u,gpuPolicy,bank,variant);
        if(profile==2)return commands1971(source,policy,u,gpuPolicy,bank,variant,true);
        int count=u[0]*(u[3]-u[2]);int confidenceCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):1;
        GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
        batch.upload(bank[0],source).allocate(bank[1],4L*count).allocate(bank[3],4L*confidenceCount).upload(bank[9],new int[]{0});
        if(gpuPolicy==null)batch.upload(bank[2],policy==null?new int[]{0}:policy);
        else{
            // GX17: exact integer policy comparison remains entirely on GPU.
            // Only its one-word failure flag accompanies the final pixels.
            batch.upload(bank[4],gpuPolicy.masks).upload(bank[5],gpuPolicy.grid)
                .allocate(bank[2],8L*count).allocate(bank[6],4L*count).allocate(bank[7],4)
                .dispatch(GpuNoise1960.ANALYSIS,new int[]{bank[7],bank[7],bank[6],bank[2],bank[5],bank[4]},gpuPolicy.u,gpuPolicy.f,count)
                .upload(bank[8],policy);
            int[] compare=new int[32];compare[0]=count*2;
            batch.dispatch(GpuNoise1960.COMPARE1961,new int[]{bank[8],bank[2],bank[9]},compare,null,count*2);
        }
        return batch.dispatch(program1973(profile,variant),
            new int[]{bank[0],bank[1],2,bank[2],4,5,6,bank[3]},u,null,count);
    }
    private static GpuNoise1960.Batch commands1971(int[] source,int[] policy,int[] u,
            GpuPolicy1960.PolicyData gpuPolicy,int[] bank,int variant,boolean generic){
        if(!generic)return commands(source,policy,u,gpuPolicy,bank,variant);
        int count=u[0]*(u[3]-u[2]);int confidenceCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):1;
        GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
        batch.upload(bank[0],source).allocate(bank[1],4L*count).allocate(bank[3],4L*confidenceCount).upload(bank[9],new int[]{0});
        if(gpuPolicy==null)batch.upload(bank[2],policy==null?new int[]{0}:policy);
        else{
            // GX17: exact integer policy comparison remains entirely on GPU.
            // Only its one-word failure flag accompanies the final pixels.
            batch.upload(bank[4],gpuPolicy.masks).upload(bank[5],gpuPolicy.grid)
                .allocate(bank[2],8L*count).allocate(bank[6],4L*count).allocate(bank[7],4)
                .dispatch(GpuNoise1960.ANALYSIS,new int[]{bank[7],bank[7],bank[6],bank[2],bank[5],bank[4]},gpuPolicy.u,gpuPolicy.f,count)
                .upload(bank[8],policy);
            int[] compare=new int[32];compare[0]=count*2;
            batch.dispatch(GpuNoise1960.COMPARE1961,new int[]{bank[8],bank[2],bank[9]},compare,null,count*2);
        }
        return batch.dispatch(program1971(true,variant),
            new int[]{bank[0],bank[1],2,bank[2],4,5,6,bank[3]},u,null,count);
    }
    private static GpuNoise1960.Batch commands(int[] source,int[] policy,int[] u,
            GpuPolicy1960.PolicyData gpuPolicy,int[] bank,int variant){
        int count=u[0]*(u[3]-u[2]);int confidenceCount=u[12]!=0?((u[0]+3)/4)*((u[3]-u[2]+3)/4):1;
        GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
        batch.upload(bank[0],source).allocate(bank[1],4L*count).allocate(bank[3],4L*confidenceCount).upload(bank[9],new int[]{0});
        if(gpuPolicy==null)batch.upload(bank[2],policy==null?new int[]{0}:policy);
        else{
            // GX17: exact integer policy comparison remains entirely on GPU.
            // Only its one-word failure flag accompanies the final pixels.
            batch.upload(bank[4],gpuPolicy.masks).upload(bank[5],gpuPolicy.grid)
                .allocate(bank[2],8L*count).allocate(bank[6],4L*count).allocate(bank[7],4)
                .dispatch(GpuNoise1960.ANALYSIS,new int[]{bank[7],bank[7],bank[6],bank[2],bank[5],bank[4]},gpuPolicy.u,gpuPolicy.f,count)
                .upload(bank[8],policy);
            int[] compare=new int[32];compare[0]=count*2;
            batch.dispatch(GpuNoise1960.COMPARE1961,new int[]{bank[8],bank[2],bank[9]},compare,null,count*2);
        }
        return batch.dispatch(GpuNoise1960.strongProgram(u[10],variant),
            new int[]{bank[0],bank[1],2,bank[2],4,5,6,bank[3]},u,null,count);
    }
    private static int[] readSlots(int[] u,int[] bank){return u[12]!=0?new int[]{bank[1],bank[3],bank[9]}:new int[]{bank[1],bank[9]};}
    private static int[] readCounts(int[] u){int count=u[0]*(u[3]-u[2]);return u[12]!=0?new int[]{count,((u[0]+3)/4)*((u[3]-u[2]+3)/4),1}:new int[]{count,1};}
    private static Result result(int[][] values,int[] u){
        if(values==null||values.length!=(u[12]!=0?3:2)||values[values.length-1]==null||values[values.length-1].length!=1||values[values.length-1][0]!=0)return null;
        return new Result(values[0],u[12]!=0?values[1]:null);
    }
    private static Result collect(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int[] u,int[] bank){
        try{return result(session.collect(ticket,readSlots(u,bank),readCounts(u)),u);}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
    }
    private static Result execute(GpuNoise1960.Session session,int[] source,int[] policy,
            int[] u,GpuPolicy1960.PolicyData gpuPolicy,int variant){
        try{
            return result(session.execute(commands(source,policy,u,gpuPolicy,BANKS[0],variant),readSlots(u,BANKS[0]),readCounts(u)),u);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){return null;}catch(LinkageError unavailable){return null;}catch(OutOfMemoryError unavailable){return null;}
    }
}

