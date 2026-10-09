package com.hiro.ulike;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;

/** GX1/2/4/7/8 exact NR9--NR13 GPU candidates. No photograph is retained by
 * the bounded admission cache. A stage owns a private session until endStage;
 * immutable evidence/maps stay on GPU while the strip source/output are reused.
 * Background qualification preserves the original CPU output. A new full
 * foreground key commits private GPU output only after two CPU comparisons.
 * Both full ARGB and sparse NR13 confidence must match twice consecutively.
 * Full-resolution Strong prefers the verified GPU even when it is slower;
 * preparation modes retain the upload/dispatch/read/close-inclusive 5% margin.
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
        "session_unavailable","bank_busy","upload_failed","execution_failed","readback_failed","policy_mismatch","cancelled","unknown"};
    private static final class Gate {int consecutive,variant;long cpuBaseline,gpuBaseline;boolean rejected,accepted;}
    private static final class Sample {
        final String key;final long gpu,cpu;final int pixels;final boolean exact,observation;
        Sample(String key,long gpu,long cpu,int pixels,boolean exact,boolean observation){this.key=key;this.gpu=gpu;this.cpu=cpu;this.pixels=pixels;this.exact=exact;this.observation=observation;}
    }
    private static final class Stage {
        final StrongNoise1958.Model model;GpuNoise1960.Session session;
        final ProcessingTiming1947.Trace trace;
        int gpuStrips,cpuStrips,verificationCpu;
        final int[] reasons=new int[REASONS.length];
        final ArrayDeque<Long> preferredWaiters=new ArrayDeque<Long>();
        long preferredSequence;
        final ArrayList<Sample> samples=new ArrayList<Sample>();
        final boolean[] banks=new boolean[2];
        final long[] bankStart=new long[2],bankDuration=new long[2];
        boolean waiter;
        long setup,setupBegin,setupEnd,pixels;boolean closed,unavailable,failed;
        Stage(StrongNoise1958.Model model){this.model=model;trace=ProcessingTiming1947.current();}
    }
    private static boolean interrupted(){return Thread.currentThread().isInterrupted();}
    private static void check(){if(interrupted())throw new CancellationException("GPU strong NR interrupted");}
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
        if(accepted(key)||GpuQualification1961.exactRejected(key))return;
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
                ProcessingTiming1947.noiseBackend(stage.trace,stage.gpuStrips,stage.cpuStrips);
                ProcessingTiming1947.strongGpuVerification1970(stage.trace,stage.verificationCpu);
                StringBuilder fields=new StringBuilder("gpu="+stage.gpuStrips+" cpu="+stage.cpuStrips+" verify_cpu="+stage.verificationCpu);
                for(int i=0;i<REASONS.length;i++)if(stage.reasons[i]>0){
                    ProcessingTiming1947.strongGpuReason1970(stage.trace,REASONS[i],stage.reasons[i]);
                    fields.append(' ').append(REASONS[i]).append('=').append(stage.reasons[i]);
                }
                try{CameraTrace1965.event("strong_gpu_preferred1970",0,fields.toString());}catch(Throwable optional){}
            }
        }
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
    private static int cpu1970(Stage stage,int reason,Oracle oracle,int[] destination,int[] confidence){
        reason1970(stage,reason);boolean ok=oracle.run(destination,confidence);check();return ok?0:-1;
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
    private static int processChoice1967(int[] source,int[] destination,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            StrongNoise1958.Model model,int[] policy,int[] confidence,
            GpuPolicy1960.Protection gpuProtection,Oracle oracle){
        if(model==null||noise<=0||end<=begin||interrupted())return -1;
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=validBegin;u[5]=validEnd;
        u[6]=originY;u[7]=model.height;u[8]=noise;u[9]=shadows?1:0;u[10]=3;u[11]=policy==null?0:1;u[12]=confidence==null?0:1;
        if(gpuProtection!=null&&gpuProtection.plan!=null){u[18]=gpuProtection.plan.beautyQ8;u[19]=gpuProtection.plan.shadowBudgetQ8;}
        String key=key(u);Stage stage=active;
        if(stage!=null&&stage.model!=model)return cpu1970(null,4,oracle,destination,confidence);
        restore(key);
        if(GpuQualification1961.background()){
            boolean ok=oracle.run(destination,confidence);check();
            if(ok&&!rejected(key))scheduleFull(key,source,policy,u,model,gpuProtection);return ok?0:-1;
        }
        if(GpuQualification1961.exactRejected(key))return cpu1970(stage,1,oracle,destination,confidence);
        boolean certified=accepted(key);int variant=certified?choice(key):-1;
        if(certified){if(!GpuNoise1960.supports(GpuNoise1960.strongProgram(3,variant)))return cpu1970(stage,2,oracle,destination,confidence);}
        else for(int layout=0;layout<3;layout++)if(GpuNoise1960.supports(GpuNoise1960.strongProgram(3,layout))){variant=layout;break;}
        // supports refreshes the native fingerprint. A persisted exact negative
        // must be checked again before any model upload or GPU verification.
        if(GpuQualification1961.exactRejected(key))return cpu1970(stage,1,oracle,destination,confidence);
        restore(key);certified=accepted(key);
        if(certified){variant=choice(key);if(!GpuNoise1960.supports(GpuNoise1960.strongProgram(3,variant)))variant=-1;}
        if(variant<0)return cpu1970(stage,2,oracle,destination,confidence);
        int count=width*(end-begin),confidenceCount=confidence==null?0:((width+3)/4)*((end-begin+3)/4);
        long extra=8L*source.length+24L*count+1048576L;
        if(gpuProtection!=null)extra+=16L*count;
        if(!GpuNoise1960.workspaceFits(extra))return cpu1970(stage,3,oracle,destination,confidence);
        boolean cpuReady=false,gpuCommitted=false;int bank=-1;long minCpu=Long.MAX_VALUE,maxGpu=0;
        try{
            if(!certified){
                long cpuStart=System.nanoTime();if(!oracle.run(destination,confidence))return -1;
                minCpu=Math.max(1L,System.nanoTime()-cpuStart);cpuReady=true;verification1970(stage);check();
            }
            GpuPolicy1960.PolicyData gpuPolicy=gpuProtection==null?null:gpuProtection.data(width,end-begin,originY+begin);
            long requestStart=System.nanoTime();
            if(stage!=null)synchronized(stage){
                if(!initialize(stage,extra)){
                    reason1970(stage,stage.failed?6:4);return cpuReady?0:-1;
                }
                bank=claimPreferred1970(stage,requestStart);
                if(bank<0){reason1970(stage,stage.failed?7:5);return cpuReady?0:-1;}
            }
            Result candidate=null;int firstVariant=variant,selected=-1;boolean qualityFailure=false;
            long firstCpu=minCpu;
            for(int candidateVariant=firstVariant;candidateVariant<(certified?firstVariant+1:3);candidateVariant++){
                if(!certified&&!GpuNoise1960.supports(GpuNoise1960.strongProgram(3,candidateVariant)))continue;
                minCpu=Long.MAX_VALUE;maxGpu=0;boolean matches=true;int trials=certified?1:2;
                for(int trial=0;trial<trials;trial++){
                    if(!certified){
                        if(candidateVariant==firstVariant&&trial==0)minCpu=firstCpu;
                        else{
                            cpuReady=false;
                            long cpuStart=System.nanoTime();if(!oracle.run(destination,confidence))return -1;
                            minCpu=Math.min(minCpu,Math.max(1L,System.nanoTime()-cpuStart));cpuReady=true;verification1970(stage);check();
                        }
                    }
                    long gpuStart=candidateVariant==firstVariant&&trial==0?requestStart:System.nanoTime();
                    if(stage!=null){
                        GpuNoise1960.Ticket ticket;
                        synchronized(stage){
                            check();if(!usable1970(stage,bank,model)){reason1970(stage,7);return cpuReady?0:-1;}
                            ticket=stage.session.submit(commands(source,policy,u,gpuPolicy,BANKS[bank],candidateVariant),bank);
                        }
                        candidate=ticket==null?null:collect(stage.session,ticket,u,BANKS[bank]);
                    }else candidate=ephemeral(source,StrongNoise1958.gpuEvidence1960(model),StrongNoise1958.gpuMaps1960(model),policy,u,gpuPolicy,candidateVariant);
                    check();
                    if(candidate==UNAVAILABLE){reason1970(stage,4);return cpuReady?0:-1;}
                    if(candidate==null||candidate.pixels==null||candidate.pixels.length!=count||
                            confidenceCount>0&&(candidate.confidence==null||candidate.confidence.length!=confidenceCount)){
                        if(stage!=null)synchronized(stage){stage.failed=true;}
                        reason1970(stage,7);return cpuReady?0:-1;
                    }
                    maxGpu=Math.max(maxGpu,Math.max(1L,System.nanoTime()-gpuStart));
                    if(!certified){
                        boolean exact=true;
                        for(int i=0;i<count;i++)if(candidate.pixels[i]!=destination[begin*width+i]){exact=false;break;}
                        if(exact)for(int i=0;i<confidenceCount;i++)if(candidate.confidence[i]!=confidence[i]){exact=false;break;}
                        if(!exact){qualityFailure=true;matches=false;break;}
                    }
                }
                if(matches){selected=candidateVariant;break;}
            }
            if(selected<0){if(qualityFailure){fail(key);reason1970(stage,1);}else reason1970(stage,2);return cpuReady?0:-1;}
            variant=selected;
            check();
            // Private readback ownership lasts through comparison and commit.
            // A failed sibling bank or a closed model cannot publish a proof.
            if(stage!=null)synchronized(stage){
                if(!usable1970(stage,bank,model)){reason1970(stage,7);return cpuReady?0:-1;}
                if(GpuQualification1961.exactRejected(key)){reason1970(stage,1);return cpuReady?0:-1;}
                check();long commit=System.nanoTime();
                System.arraycopy(candidate.pixels,0,destination,begin*width,count);
                if(confidenceCount>0)System.arraycopy(candidate.confidence,0,confidence,0,confidenceCount);
                gpuCommitted=true;
                maxGpu+=Math.max(0L,System.nanoTime()-commit);
                if(!certified){check();GpuQualification1961.qualifiedStrongPreferred1970(key,minCpu,maxGpu,variant);restore(key);}
            }else{
                if(GpuQualification1961.exactRejected(key)){reason1970(stage,1);return cpuReady?0:-1;}
                check();
                long commit=System.nanoTime();System.arraycopy(candidate.pixels,0,destination,begin*width,count);
                if(confidenceCount>0)System.arraycopy(candidate.confidence,0,confidence,0,confidenceCount);
                gpuCommitted=true;
                maxGpu+=Math.max(0L,System.nanoTime()-commit);
                if(!certified){check();GpuQualification1961.qualifiedStrongPreferred1970(key,minCpu,maxGpu,variant);restore(key);}
            }
            return 1;
        }catch(CancellationException cancelled){reason1970(stage,10);throw cancelled;}
        catch(RuntimeException unavailable){if(gpuCommitted)return 1;reason1970(stage,7);return cpuReady?0:-1;}
        catch(LinkageError unavailable){if(gpuCommitted)return 1;reason1970(stage,7);return cpuReady?0:-1;}
        catch(OutOfMemoryError unavailable){if(gpuCommitted)return 1;reason1970(stage,3);return cpuReady?0:-1;}
        finally{if(stage!=null&&bank>=0)synchronized(stage){stage.banks[bank]=false;stage.bankStart[bank]=0;stage.bankDuration[bank]=0;stage.notifyAll();}}
    }
    private static final class Result {
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

