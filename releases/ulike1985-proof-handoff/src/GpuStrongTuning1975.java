package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** v1.9.75: private idle qualification plus fastest exact GPU selection. The
 * queue owns copies of mutable inputs; only scalar certificates survive. GPU
 * preference does not permit an unverified faster kernel to reach a photo. */
public final class GpuStrongTuning1975 {
    private GpuStrongTuning1975() {}
    private static final Object SNAPSHOT_LOCK=new Object();
    private static final int[] BANK={0,1,3,7,8,9,10,11,12,13};
    private static final String PREFIX="strong-gx1978-tuning-policy-bank-v1:";
    private static final String PREVIOUS_PREFIX1976="strong-gx1976-tuning-policy-bank-v1:";
    private static final String PREVIOUS_PREFIX1975="strong-gx1975-tuning-policy-bank-v1:";
    private static final String CPU_PREFIX1978="strong-gx1978-cpu-tuning-policy-bank-v1:";
    // Telemetry is optional even if an older/malformed diagnostic provider is
    // linked. Neither recording failure nor a missing method changes pixels,
    // scheduling, certificate persistence or private-snapshot ownership.
    private static void progress1984(String phase,int completed,int total){
        try{GpuQualification1961.progress1984(phase,completed,total);}catch(Throwable optional){}
    }
    private static void timings1984(long cpu,long gpu){
        try{GpuQualification1961.timings1984(cpu,gpu);}catch(Throwable optional){}
    }
    private static void outcome1984(String reason){
        try{GpuQualification1961.outcome1984(reason);}catch(Throwable optional){}
    }
    public static final class Choice {
        public final int profile,variant;public final String key;
        Choice(int profile,int variant,String key){this.profile=profile;this.variant=variant;this.key=key;}
    }
    private static String group(int[] u){
        String profile=GpuStrong1960.profileKey1973(u,0);
        return PREFIX+profile.substring(profile.indexOf(':')+1);
    }
    private static String cpuGroup1978(int[] u,int workers){return CPU_PREFIX1978+group(u).substring(PREFIX.length())+":"+workers;}
    /** A previously unavailable legacy GPU cannot strand a corrected direct
     * profile. Its separate selection requires actual parallel CPU speed proof
     * and is restored only for the same production worker count. */
    public static Choice selectCpu1978(int[] u,int workers){
        if(u==null||u.length<20||workers<1||workers>4)return null;
        String name=cpuGroup1978(u,workers);GpuQualification1961.Record winner=GpuQualification1961.restore(name);
        if(winner==null||winner.variant<12||winner.variant>23)return null;
        int profile=winner.variant/3,variant=winner.variant%3;String key=GpuStrong1960.profileKey1973(u,profile);
        GpuQualification1961.Record child=GpuQualification1961.exactRejected(key)?null:GpuQualification1961.restore(key);
        if(child!=null&&child.variant==variant)return new Choice(profile,variant,key);
        GpuQualification1961.invalidateStrongTuning1975(name,winner.variant);return null;
    }
    /** A persisted aggregate is usable only while its exact child certificate
     * still exists with the selected variant and has no exact rejection. */
    public static Choice select(int[] u) {
        try{return selectChecked1975(u);}
        catch(CancellationException cancelled){Thread.currentThread().interrupt();return null;}
        catch(RuntimeException unavailable){return null;}
        catch(LinkageError unavailable){return null;}
        catch(OutOfMemoryError unavailable){return null;}
    }
    private static Choice selectChecked1975(int[] u) {
        if(u==null||u.length<20||u[10]!=3)return null;
        String selectedGroup=group(u);GpuQualification1961.Record winner=GpuQualification1961.restore(selectedGroup);
        String suffix=selectedGroup.substring(PREFIX.length());
        if(winner==null){selectedGroup=PREVIOUS_PREFIX1976+suffix;winner=GpuQualification1961.restore(selectedGroup);}
        if(winner==null){selectedGroup=PREVIOUS_PREFIX1975+suffix;winner=GpuQualification1961.restore(selectedGroup);}
        int limit=selectedGroup.startsWith(PREFIX)?23:selectedGroup.startsWith(PREVIOUS_PREFIX1976)?11:8;
        if(winner==null||winner.variant<0||winner.variant>limit)return null;
        int profile=winner.variant/3,variant=winner.variant%3;
        String key=GpuStrong1960.profileKey1973(u,profile);
        GpuQualification1961.Record proof=GpuQualification1961.exactRejected(key)?null:GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant==variant)return new Choice(profile,variant,key);
        GpuQualification1961.invalidateStrongTuning1975(selectedGroup,winner.variant);return null;
    }
    /** Called while the foreground still owns its strip source. A background
     * oracle never recursively schedules, and duplicate geometry never clones. */
    public static void schedule(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,
            GpuPolicy1960.Protection protection) {
        schedule1978(source,policy,u,model,protection,SpeedWorkers1935.maxWorkers());
    }
    static void schedule1978(int[] source,int[] policy,int[] u,StrongNoise1958.Model model,
            GpuPolicy1960.Protection protection,int productionWorkers) {
        if(GpuQualification1961.background()||Thread.currentThread().isInterrupted()||source==null||
                model==null||u==null||u.length<20||u[10]!=3)return;
        try {synchronized(SNAPSHOT_LOCK){
            int workers=Math.max(1,Math.min(4,productionWorkers));
            if(selectCpu1978(u,workers)!=null)return;
            long count=(long)u[0]*u[1],core=(long)u[0]*(u[3]-u[2]);
            if(count<=0||count>Integer.MAX_VALUE||core<=0||core>Integer.MAX_VALUE/2||
                    source.length<count||policy!=null&&policy.length<core*2)return;
            select(u);String key=group(u);boolean eligible=false,missing=false,legacy=false;
            for(int p=0;p<GpuStrong1960.PROFILES1978;p++){
                String child=GpuStrong1960.profileKey1973(u,p);
                if(!GpuQualification1961.exactRejected(child)){
                    eligible=true;GpuQualification1961.Record saved=GpuQualification1961.restore(child);
                    if(saved==null)missing=true;else if(p<4)legacy=true;
                }
            }
            if(!eligible||!GpuQualification1961.maySchedule(key)||GpuQualification1961.restore(key)!=null)return;
            // A failed parallel CPU comparison controls only that comparison.
            // Missing exact profiles may still make progress during its cooling
            // period. Once every profile is known, do not copy/re-run two CPU
            // references merely to rediscover the companion's same cooldown.
            if(!missing&&!legacy&&!GpuQualification1961.maySchedule(cpuGroup1978(u,workers)))return;
            GpuSnapshotBudget1981.offerStrip1981(GpuSnapshotBudget1981.STRONG_TUNING);
            // Include original model evidence, all retained arrays and generous
            // fixed object/header overhead. Model maps/evidence are immutable.
            long bytes=4L*(count+(policy==null?0:core*2))+model.residentBytes()+4096L;
            if(protection!=null){
                long grid=1;
                if(protection.plan!=null&&protection.plan.localNoise!=null)
                    grid=Math.max(1L,(long)protection.plan.localNoise.columns*protection.plan.localNoise.rows);
                bytes+=8L*core+4L*grid+1024L;
            }
            long shared=model.residentBytes(),exclusive=bytes-shared;
            if(shared<0||exclusive<0||bytes>96L*1024*1024)return;
            GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryStripCopy1981(exclusive,GpuSnapshotBudget1981.STRONG_TUNING);
            if(copy==null)return;
            GpuQualification1961.Reservation1984 reservation=null;
            try{
                // The immutable model is already charged in the live Java heap;
                // only new private arrays are additional physical allocations.
                if(!GpuNoise1960.workspaceFits(exclusive+16L*count+24L*core))return;
                reservation=GpuQualification1961.reserveStrongShared1985(key,cpuGroup1978(u,workers),exclusive,model,shared);
                if(!reservation.accepted()||!copy.begin()||!reservation.begin1985())return;
                GpuPolicy1960.PolicyData descriptor=protection==null?null:
                    GpuPolicy1960.detached1975(protection.data(u[0],u[3]-u[2],u[6]+u[2]));
                Proof proof=new Proof(key,Arrays.copyOf(source,(int)count),
                    policy==null?null:Arrays.copyOf(policy,(int)core*2),u.clone(),model,descriptor,workers);
                if(copy.current()&&reservation.current())reservation.commit(proof);
                else proof.close();
            }finally{try{if(reservation!=null)reservation.close();}finally{copy.close();}}
        }}catch(CancellationException cancelled){Thread.currentThread().interrupt();}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
    }
    private static final class Proof implements GpuQualification1961.Probe {
        private static void comparison1985(String reference,int workers,long baseline,long candidate){
            try{GpuQualification1961.comparison1985(reference,workers,baseline,candidate);}catch(Throwable optional){}
        }
        private static void selection1985(Choice selected,boolean priorInvalidated){
            try{GpuQualification1961.selection1985(selected==null?null:selected.key,
                selected==null?-1:selected.profile,selected==null?-1:selected.variant,priorInvalidated);}catch(Throwable optional){}
        }
        final String key;final int workers1978;int[] source,policy,u;StrongNoise1958.Model model;GpuPolicy1960.PolicyData descriptor;
        Proof(String key,int[] source,int[] policy,int[] u,StrongNoise1958.Model model,GpuPolicy1960.PolicyData descriptor,int workers){
            this.key=key;this.source=source;this.policy=policy;this.u=u;this.model=model;this.descriptor=descriptor;workers1978=workers;
        }
        private boolean cpu(int[] output,int[] confidence){
            return StrongNoise1958.gpuOracleSnapshot1961(source,output,u[0],u[1],u[2],u[3],u[4],u[5],u[6],u[8],u[9]!=0,model,policy,confidence);
        }
        private String difference(int[] actual,int[] actualConfidence,int[] expected,int[] confidence,int core,int offset){
            if(actual==null||actual.length!=core)return "output_shape_failed";
            for(int i=0;i<core;i++)if(actual[i]!=expected[offset+i])return "argb_mismatch";
            if(confidence!=null&&(actualConfidence==null||!Arrays.equals(actualConfidence,confidence)))return "confidence_mismatch";
            return null;
        }
        private static final class Measured {
            final int[] pixels,confidence;final String failure;final long nanos;
            Measured(int[] p,int[] c,String f,long n){pixels=p;confidence=c;failure=f;nanos=n;}
        }
        /** Same resident immutable model and strip bank as production. A second
         * exact trial recomputes policy and pixels without uploading them again. */
        private Measured measure(GpuNoise1960.Session session,int profile,int variant,boolean repeat,int core,int cf,
                GpuQualification1961.Cancellation cancellation) {
            long start=System.nanoTime(),elapsed=0;GpuNoise1960.Lease1971 lease=null;
            GpuPolicy1960.PolicyData activeDescriptor=GpuStrong1960.directPolicy1978(profile)?null:descriptor;
            int[] committed=null,committedConfidence=null;String failure=null;
            try {
                // Materialize final-commit storage before admission so Runtime's
                // used heap includes it. The held lease reserves the additional
                // private readback; no unreserved copy is allocated after it.
                committed=new int[core];committedConfidence=cf==0?null:new int[cf];
                lease=GpuStrong1960.reserveProof1978(session,source,policy,u,activeDescriptor,BANK);
                if(lease==null||!lease.revalidate1971())failure="memory_budget";
                else {
                    if(cancellation.cancelled())return new Measured(null,null,"cancelled",0);
                    GpuNoise1960.Ticket ticket=session.submit(repeat?
                        GpuStrong1960.commandsRepeat1975(u,activeDescriptor,BANK,variant,profile):
                        GpuStrong1960.commands1973(source,policy,u,activeDescriptor,BANK,variant,profile),0);
                    GpuStrong1960.Read1971 read=ticket==null?null:GpuStrong1960.read1971(session,ticket,u,BANK);
                    if(cancellation.cancelled())return new Measured(null,null,"cancelled",0);
                    if(read==null||read.failure!=null||read.result==null)failure=read==null?"submit_failed":read.failure==null?"readback_failed":read.failure;
                    else {
                        GpuStrong1960.Result result=read.result;
                        if(result.pixels==null||result.pixels.length!=core||cf!=0&&(result.confidence==null||result.confidence.length!=cf))failure="output_shape_failed";
                        else {
                            System.arraycopy(result.pixels,0,committed,0,core);
                            if(cf!=0)System.arraycopy(result.confidence,0,committedConfidence,0,cf);
                            elapsed=Math.max(1L,System.nanoTime()-start);
                            // Protection is fully compared during every proof,
                            // including direct-policy routes. Proof-only readback
                            // is outside the production-equivalent timer.
                            failure=GpuStrong1960.verifyPolicy1978(session,BANK[2],policy);
                        }
                    }
                }
            }finally{if(lease!=null)lease.close();}
            return new Measured(committed,committedConfidence,failure,elapsed==0?Math.max(1L,System.nanoTime()-start):elapsed);
        }
        /** Confirm a proposed .78 winner against the best retained profile in
         * balanced order. Both use a complete fresh production upload graph;
         * optional proof-only policy readbacks do not bias either timer. */
        private Measured complete1978(int profile,int variant,int core,int cf,GpuQualification1961.Cancellation cancellation){
            long opened=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
            if(session==null)return new Measured(null,null,"session_unavailable",0);
            long setup=0,closing;Measured measured;
            try{
                float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[][] maps=StrongNoise1958.gpuMaps1960(model);
                GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
                if(lease==null)return new Measured(null,null,"memory_budget",0);
                try{if(!lease.revalidate1971()||!session.upload(2,evidence)||!session.upload(4,maps[0])||!session.upload(5,maps[1])||!session.upload(6,maps[2]))return new Measured(null,null,"upload_failed",0);}
                finally{lease.close();}
                setup=System.nanoTime()-opened;
                measured=measure(session,profile,variant,false,core,cf,cancellation);
            }finally{closing=System.nanoTime();session.close();closing=System.nanoTime()-closing;}
            return new Measured(measured.pixels,measured.confidence,measured.failure,measured.nanos+setup+closing);
        }
        private static final class Progress1984 {
            boolean exact,cpuCooling,cpuSlow,transport,mismatch;
        }
        private boolean current1985(Choice choice){
            if(choice==null||GpuQualification1961.exactRejected(choice.key))return false;
            GpuQualification1961.Record saved=GpuQualification1961.restore(choice.key);
            return saved!=null&&saved.variant==choice.variant&&saved.cpuNanos>0&&saved.gpuNanos>0;
        }
        /** A later full comparison may reject the previously fastest legacy
         * child. Always choose from authoritative records, not a loop-local
         * profile captured before that rejection. New direct profiles still
         * require their separate complete speed proof. */
        private Choice legacy1985(){
            Choice selected=null;long best=Long.MAX_VALUE;
            for(int profile=0;profile<4;profile++){
                String child=GpuStrong1960.profileKey1973(u,profile);
                GpuQualification1961.Record saved=GpuQualification1961.exactRejected(child)?null:GpuQualification1961.restore(child);
                if(saved!=null&&saved.variant>=0&&saved.variant<=2&&saved.cpuNanos>0&&saved.gpuNanos>0&&saved.gpuNanos<best){
                    selected=new Choice(profile,saved.variant,child);best=saved.gpuNanos;
                }
            }
            return selected;
        }
        private Choice completed1985(){
            Choice selected=select(u);
            if(selected==null){
                Choice legacy=legacy1985();
                if(legacy!=null){
                    GpuQualification1961.Record saved=GpuQualification1961.restore(legacy.key);
                    if(saved!=null&&saved.variant==legacy.variant)
                        GpuQualification1961.qualifiedStrongPreferred1970(key,saved.cpuNanos,saved.gpuNanos,legacy.profile*3+legacy.variant);
                    selected=select(u);
                }
            }
            return selected==null?selectCpu1978(u,workers1978):selected;
        }
        /** A complete profile need not wait for every later profile. This
         * checkpoint performs the same two AB/BA full-upload comparisons (or
         * the same real parallel CPU proof when there is no legacy GPU) before
         * publishing an existing aggregate key. A child alone never selects a
         * new direct-policy foreground route. */
        /** Preserve the published private DEX signature. Old loop-local
         * reference parameters never substitute for current certificates. */
        private void checkpoint1984(int profile,GpuQualification1961.Record child,
                int legacyProfile,int legacyVariant,int core,int cf,int[][] expected,int[][] confidence,long minCpu,
                GpuQualification1961.Cancellation cancellation,Progress1984 progress){
            checkpoint1985(profile,child,core,cf,expected,confidence,minCpu,cancellation,progress);
        }
        private void checkpoint1985(int profile,GpuQualification1961.Record child,
                int core,int cf,int[][] expected,int[][] confidence,long minCpu,
                GpuQualification1961.Cancellation cancellation,Progress1984 progress){
            if(child==null||child.variant<0||child.variant>2)return;
            String exactKey=GpuStrong1960.profileKey1973(u,profile);
            Choice direct=new Choice(profile,child.variant,exactKey);
            // At most four legacy references can fail. Each replacement gets
            // fresh complete AB/BA trials; once none remains, the unchanged
            // real parallel CPU comparison is the only speed admission.
            for(int reference=0;reference<=4;reference++){
                if(cancellation.cancelled()||!current1985(direct))return;
                progress1984("strong_speed_compare",0,2);
                Choice legacy=legacy1985();
                if(legacy==null){cpuCheckpoint1985(profile,child,exactKey,cpuGroup1978(u,workers1978),cancellation,progress);return;}
                long baseline=Long.MAX_VALUE,candidate=0;boolean retired=false;
                comparison:for(int trial=0;trial<2;trial++)for(int turn=0;turn<2;turn++){
                    if(cancellation.cancelled())return;
                    boolean newer=(trial+turn)%2!=0;
                    Choice selected=newer?direct:legacy;
                    if(!current1985(selected)){
                        if(newer)return;retired=true;break comparison;
                    }
                    Measured measured=complete1978(selected.profile,selected.variant,core,cf,cancellation);
                    if(cancellation.cancelled())return;
                    String mismatch=measured.failure==null?difference(measured.pixels,measured.confidence,expected[trial],confidence[trial],core,u[0]*u[2]):measured.failure;
                    if(mismatch!=null){
                        if("argb_mismatch".equals(mismatch)||"confidence_mismatch".equals(mismatch)||"policy_failure".equals(mismatch)){
                            progress.mismatch=true;GpuQualification1961.rejectExact1971(selected.key,mismatch);
                            if(!newer){retired=true;break comparison;}
                        }else progress.transport=true;
                        return;
                    }
                    if(newer)candidate=Math.max(candidate,measured.nanos);else baseline=Math.min(baseline,measured.nanos);
                    if(turn==1)progress1984("strong_speed_compare",trial+1,2);
                }
                if(retired)continue;
                // Preserve the old CPU-reference display while recording the
                // actual old-GPU baseline separately with its correct label.
                timings1984(minCpu,candidate);
                comparison1985("strong_gpu_legacy",1,baseline,candidate);
                if(candidate<=0||candidate>baseline-baseline/20||cancellation.cancelled()||!current1985(direct))return;
                Choice existing=select(u);
                GpuQualification1961.Record prior=existing==null?null:GpuQualification1961.restore(key);
                // Keep the fastest completely proved direct aggregate. A later
                // profile's unsuccessful optimization cannot retire useful progress.
                if(prior!=null&&existing.profile>=4&&prior.gpuNanos<=candidate)return;
                GpuQualification1961.qualifiedStrongProfile1975(key,exactKey,minCpu,candidate,child.variant);
                if(!cancellation.cancelled()&&current1985(direct))
                    GpuQualification1961.qualifiedStrongPreferred1970(key,minCpu,candidate,profile*3+child.variant);
                return;
            }
        }
        private void cpuCheckpoint1985(int profile,GpuQualification1961.Record child,String exactKey,String cpuName,
                GpuQualification1961.Cancellation cancellation,Progress1984 progress){
            Choice existing=selectCpu1978(u,workers1978);
            // Cooling/capped CPU timing controls only this comparison, including
            // when its last legacy GPU reference was rejected moments earlier.
            if(existing==null&&!GpuQualification1961.maySchedule(cpuName)){
                progress.cpuCooling=true;return;
            }
            long[] parallel=GpuStrongRouting1978.compareCpu1978(source,policy,u,model,descriptor,profile,child.variant,workers1978,cancellation);
            if(cancellation.cancelled())return;
            if(parallel==null){
                if(GpuQualification1961.exactRejected(exactKey))progress.mismatch=true;
                else progress.transport=true;
                return;
            }
            progress1984("strong_speed_compare",2,2);
            timings1984(parallel[0],parallel[1]);
            comparison1985("strong_cpu_parallel",workers1978,parallel[0],parallel[1]);
            if(parallel[0]>0&&parallel[1]>0&&parallel[1]<=parallel[0]-parallel[0]/20){
                GpuQualification1961.Record prior=existing==null?null:GpuQualification1961.restore(cpuName);
                GpuQualification1961.Record priorChild=existing==null?null:GpuQualification1961.restore(existing.key);
                if(prior==null||parallel[1]<prior.gpuNanos||parallel[1]==prior.gpuNanos&&priorChild!=null&&child.gpuNanos<priorChild.gpuNanos)
                    GpuQualification1961.qualified(cpuName,parallel[0],parallel[1],profile*3+child.variant);
            }else{
                // A slower early profile must not put a later, potentially
                // useful profile into cooldown in this same idle job. Spend
                // one speed retry only after all eligible profiles finish.
                progress.cpuSlow=true;
            }
        }
        public void run(GpuQualification1961.Cancellation cancellation){
            if(cancellation.cancelled()||source==null)return;
            int core=u[0]*(u[3]-u[2]),offset=u[0]*u[2];
            int cf=u[12]==0?0:((u[0]+3)/4)*((u[3]-u[2]+3)/4);
            // Peak room for two full CPU references, private returned GPU data
            // and its measured final commit is checked before allocating them.
            if(!GpuNoise1960.workspaceFits(8L*source.length+40L*core+8L*cf+1048576L)){
                outcome1984("memory_budget");return;
            }
            int[][] expected=new int[2][],confidence=new int[2][];long minCpu=Long.MAX_VALUE;
            for(int trial=0;trial<2;trial++){
                if(cancellation.cancelled())return;
                progress1984("strong_cpu_reference",trial,2);
                expected[trial]=new int[source.length];confidence[trial]=cf==0?null:new int[cf];
                long start=System.nanoTime();if(!cpu(expected[trial],confidence[trial])){
                    outcome1984("reference_unstable");return;
                }
                minCpu=Math.min(minCpu,Math.max(1L,System.nanoTime()-start));
                if(cancellation.cancelled())return;
            }
            progress1984("strong_cpu_reference",2,2);
            for(int i=0;i<core;i++)if(expected[0][offset+i]!=expected[1][offset+i]){
                outcome1984("reference_unstable");return;
            }
            if(cf!=0&&!Arrays.equals(confidence[0],confidence[1])){
                outcome1984("reference_unstable");return;
            }
            Progress1984 progress=new Progress1984();
            float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[][] maps=StrongNoise1958.gpuMaps1960(model);
            for(int profile=0;profile<GpuStrong1960.PROFILES1978;profile++){
                progress1984("strong_profile",profile,8);
                String child=GpuStrong1960.profileKey1973(u,profile);
                if(cancellation.cancelled())return;
                if(GpuQualification1961.exactRejected(child))continue;
                int profileBest=-1,bad=0;long profileTime=Long.MAX_VALUE;String cause=null;
                GpuQualification1961.Record old=GpuQualification1961.restore(child);
                // A signed child already represents two complete exact trials.
                // Resume at the missing speed/aggregate dependency instead of
                // repeating every successful profile after capture cancellation.
                // Reuse is condition- and variant-bound; no timestamp or global
                // "GPU works" flag substitutes for an authoritative child.
                boolean resumed=old!=null&&old.variant>=0&&old.variant<=2&&old.cpuNanos>0&&old.gpuNanos>0;
                if(resumed){profileBest=old.variant;profileTime=old.gpuNanos;}
                for(int variant=0;!resumed&&variant<3;variant++){
                    if(cancellation.cancelled())return;
                    if(!GpuNoise1960.supports(GpuStrong1960.program1973(profile,variant)))continue;
                    boolean exact=true;long worst=0,overhead=0;int completed=0;
                    long opened=System.nanoTime();GpuNoise1960.Session session=GpuNoise1960.open();
                    if(session==null){progress.transport=true;continue;}
                    try {
                        GpuNoise1960.Lease1971 modelLease=session.reserveCapacity1971(new int[]{2,4,5,6},
                            new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
                        boolean uploaded=false;
                        try {if(modelLease!=null)uploaded=session.upload(2,evidence)&&session.upload(4,maps[0])&&session.upload(5,maps[1])&&session.upload(6,maps[2]);}
                        finally{if(modelLease!=null)modelLease.close();}
                        overhead=System.nanoTime()-opened;
                        if(!uploaded)exact=false;
                        else for(int trial=0;trial<2;trial++){
                            if(cancellation.cancelled())return;
                            Measured measured=measure(session,profile,variant,trial==1,core,cf,cancellation);
                            if(cancellation.cancelled())return;
                            if(measured.failure!=null){
                                exact=false;
                                if("policy_failure".equals(measured.failure)){
                                    progress.mismatch=true;
                                    GpuQualification1961.rejectExact1971(child,"policy_failure");cause="policy_failure";
                                }else progress.transport=true;
                                break;
                            }
                            String mismatch=difference(measured.pixels,measured.confidence,expected[trial],confidence[trial],core,offset);
                            if(mismatch!=null){
                                exact=false;bad++;cause=mismatch;progress.mismatch=true;
                                // A failed reproof of the currently accepted
                                // variant invalidates that certificate.
                                if(old!=null&&old.variant==variant)GpuQualification1961.rejectExact1971(child,mismatch);
                                break;
                            }
                            completed++;worst=Math.max(worst,measured.nanos);
                        }
                    } finally {
                        long closing=System.nanoTime();
                        try{session.close();}finally{overhead+=System.nanoTime()-closing;}
                    }
                    if(completed!=2)exact=false;
                    if(exact)worst+=overhead/2;
                    if(cancellation.cancelled())return;
                    if(GpuQualification1961.exactRejected(child))break;
                    // Keep a completed legacy exact2 proof immediately. A new
                    // capture may cancel the later 24-candidate optimization;
                    // that does not erase already verified useful progress.
                    // Existing rejection rules are deliberately unchanged: a
                    // later policy failure can still invalidate this profile.
                    if(profile<4&&old==null&&exact&&worst>0&&GpuQualification1961.restore(child)==null)
                        GpuQualification1961.qualifiedStrongProfile1975(key,child,minCpu,worst,variant);
                    if(exact&&worst>0&&worst<profileTime){profileTime=worst;profileBest=variant;}
                }
                if(cancellation.cancelled())return;
                if(bad==3)GpuQualification1961.rejectExact1971(child,cause);
                if(profileBest>=0&&!GpuQualification1961.exactRejected(child)){
                    if(!resumed)GpuQualification1961.qualifiedStrongProfile1975(key,child,minCpu,profileTime,profileBest);
                    GpuQualification1961.Record saved=GpuQualification1961.restore(child);
                    if(saved!=null&&saved.variant==profileBest){
                        progress.exact=true;
                        if(profile>=4)checkpoint1985(profile,saved,core,cf,expected,confidence,minCpu,cancellation,progress);
                    }
                }
            }
            if(cancellation.cancelled())return;
            progress1984("strong_profile",8,8);
            Choice selected=completed1985();
            selection1985(selected,false);
            if(selected!=null){
                if(!cancellation.cancelled())GpuStrongRouting1978.prove(source,policy,u,model,descriptor,selected.profile,selected.variant,cancellation);
                // Optional mixed routing can discover a real later mismatch.
                // Re-read before reporting completion, and preserve only an
                // independently completed alternative if one still exists.
                boolean invalidated=!current1985(selected);
                if(invalidated&&GpuQualification1961.exactRejected(selected.key))progress.mismatch=true;
                selected=completed1985();selection1985(selected,invalidated);
            }
            if(selected!=null)outcome1984("strong_qualified");
            else{
                if(progress.cpuSlow&&!progress.cpuCooling)GpuQualification1961.rejectSpeed(cpuGroup1978(u,workers1978));
                outcome1984(progress.cpuCooling?"cpu_speed_cooldown":progress.cpuSlow?"speed_condition":progress.mismatch?"quality_mismatch":progress.exact?"strong_partial":progress.transport?"transport_failure":"unsupported");
            }
        }
        public void close(){source=null;policy=null;u=null;model=null;descriptor=null;}
    }
}
