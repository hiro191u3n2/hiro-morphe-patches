package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** GX23/GX37: bounded, private, idle-only qualification jobs. Certificates contain
 * timings and configuration only. A new capture cancels old proofs before any
 * certificate is committed; candidates never reach the user's saved image. */
public final class GpuQualification1961 {
    private GpuQualification1961() {}
    public interface Cancellation {boolean cancelled();}
    public interface Probe {void run(Cancellation cancellation);void close();}
    /** GX30 detached CPU ranges must really run on the bounded production
     * worker pool. The returned wall time includes task admission and joining;
     * it is never a serial duration divided by the configured worker count. */
    public interface RowTask1964 {boolean run(int begin,int end);}
    public static long parallelRows1964(final int begin,final int end,int alignment,
            final Cancellation cancellation,final RowTask1964 operation) {
        if(begin<0||end<=begin||alignment<1||operation==null||cancellation==null)
            throw new IllegalArgumentException("GX30 CPU parallel range");
        if(cancellation.cancelled())throw new java.util.concurrent.CancellationException("GX30 CPU proof cancelled");
        final int units=(end-begin+alignment-1)/alignment;
        final int workers=Math.min(units,Math.min(SpeedWorkers1935.maxWorkers(),SpeedWorkers1935.availableWorkers1944()));
        Runnable[] tasks=new Runnable[workers];final boolean[] completed=new boolean[workers];
        for(int i=0;i<workers;i++) {
            final int index=i,first=begin+(int)((long)units*i/workers)*alignment;
            final int last=i+1==workers?end:begin+(int)((long)units*(i+1)/workers)*alignment;
            tasks[i]=new Runnable(){public void run(){
                if(cancellation.cancelled())return;
                completed[index]=operation.run(first,last);
            }};
        }
        long start=System.nanoTime();SpeedWorkers1935.run(tasks);long elapsed=System.nanoTime()-start;
        if(cancellation.cancelled())throw new java.util.concurrent.CancellationException("GX30 CPU proof cancelled after join");
        for(boolean value:completed)if(!value)return 0;
        return elapsed;
    }
    public static final class Record {
        public final long cpuNanos,gpuNanos;
        public final int variant;
        Record(long cpu,long gpu,int choice){cpuNanos=cpu;gpuNanos=gpu;variant=choice;}
    }
    /** One actual admission decision, captured under LOCK. These scalars are
     * safe to retain with a photograph: no key, environment, probe or image is
     * exposed. READY is a preflight result; only SCHEDULED owns a queued job. */
    public static final class QueueDecision1983 {
        public static final int READY=0,SCHEDULED=1,INVALID_KEY=2,INVALID_BYTES=3,
            REQUEST_TOO_LARGE=4,BACKGROUND=5,INTERRUPTED=6,ENVIRONMENT_UNAVAILABLE=7,
            EXACT_REJECTED=8,RETRY_COOLDOWN=9,RETRY_LIMIT=10,ALREADY_QUALIFIED=11,
            SAME_RUNNING=12,SAME_QUEUED=13,RESERVED_SLOT=14,JOB_LIMIT=15,
            RETAINED_LIMIT=16,JOB_AND_RETAINED_LIMIT=17,NO_PROBE=18,
            SCHEDULER_UNAVAILABLE=19,SCHEDULER_MEMORY=20;
        public final boolean accepted;
        public final int reason,retries,queuedJobs,runningJobs;
        public final long retryRemainingNanos,retainedBytes,requestedBytes;
        QueueDecision1983(int why,long remaining,int attempts,int queued,int running,long held,long requested) {
            accepted=why==READY||why==SCHEDULED;reason=why;retryRemainingNanos=remaining;
            retries=attempts;queuedJobs=queued;runningJobs=running;retainedBytes=held;requestedBytes=requested;
        }
    }
    // No allocation or second admission attempt is needed after an allocation
    // failure. Unknown queue scalars are explicitly -1, never invented zeros.
    private static final QueueDecision1983 UNAVAILABLE1983=new QueueDecision1983(
        QueueDecision1983.SCHEDULER_UNAVAILABLE,0,0,-1,-1,-1,-1);
    private static final QueueDecision1983 MEMORY1983=new QueueDecision1983(
        QueueDecision1983.SCHEDULER_MEMORY,0,0,-1,-1,-1,-1);
    private static final Object LOCK=new Object();
    private static final Object PERSIST_LOCK=new Object();
    private static final int LIMIT=64;
    private static final long MAX_RETAINED=96L*1024*1024,QUIET=2000000000L;
    private static final long RETRY_COOLDOWN=30000000000L;
    private static final int MAX_SPEED_RETRIES=3;
    // GX30 changes the timing oracle. Previously persisted serial/worker-count
    // estimates cannot qualify the newly measured parallel route.
    private static final String SCHEMA="gx1964-full-output-parallel-2wins5-v1";
    // Keep the environment and legacy exact-rejection signatures compatible.
    // Old reject-all markers did not retain the failed key at capacity. Their
    // removal is not a proof: every old success must pass the existing complete
    // output checks again before this new positive-certificate epoch accepts it.
    private static final String PROOF_SCHEMA1977=SCHEMA+"-per-key-recovery-v1";
    private static final String STRONG_PREFERRED_SCHEMA=PROOF_SCHEMA1977+"-strong-exact2-preferred-v1";
    private static final String EXACT_SCHEMA1977="gx1977-exact-per-key-v1";
    private static boolean strongPreferred1970(String key) {
        return key!=null&&(key.startsWith("strong-gx1964-parallel-policy-bank-v1:3:")||
            key.startsWith("strong-gx1971-generic-policy-bank-v1:3:")||
            key.startsWith("strong-gx1973-ieee-div-policy-bank-v1:3:")||key.startsWith("strong-gx1976-ieee-tile8-policy-bank-v1:3:")||
            key.startsWith("strong-gx1978-policy-direct-ieee-v1:3:")||key.startsWith("strong-gx1978-policy-direct-tile8-v1:3:")||
            key.startsWith("strong-gx1978-policy-direct-pow2-v1:3:")||key.startsWith("strong-gx1978-policy-direct-pow2-tile8-v1:3:")||tuningKey1975(key));
    }
    private static final String TUNING_PREFIX1975="strong-gx1975-tuning-policy-bank-v1:";
    private static final String TUNING_PREFIX1976="strong-gx1976-tuning-policy-bank-v1:";
    private static final String TUNING_PREFIX1978="strong-gx1978-tuning-policy-bank-v1:";
    private static final String CPU_TUNING_PREFIX1978="strong-gx1978-cpu-tuning-policy-bank-v1:";
    private static boolean cpuTuningKey1978(String key){return key!=null&&key.startsWith(CPU_TUNING_PREFIX1978+"3:");}
    private static boolean cpuTuningChild1978(String group,String key){
        if(group==null||!group.startsWith(TUNING_PREFIX1978+"3:")||!cpuTuningKey1978(key))return false;
        String prefix=CPU_TUNING_PREFIX1978+group.substring(TUNING_PREFIX1978.length())+":";
        return key.length()==prefix.length()+1&&key.startsWith(prefix)&&key.charAt(prefix.length())>='1'&&key.charAt(prefix.length())<='4';
    }
    private static boolean tuningKey1975(String key){return key!=null&&(key.startsWith(TUNING_PREFIX1975+"3:")||key.startsWith(TUNING_PREFIX1976+"3:")||key.startsWith(TUNING_PREFIX1978+"3:"));}
    private static int tuningChoiceLimit1976(String key){return cpuTuningKey1978(key)||key!=null&&key.startsWith(TUNING_PREFIX1978+"3:")?23:key!=null&&key.startsWith(TUNING_PREFIX1976+"3:")?11:tuningKey1975(key)?8:2;}
    /** The aggregate stores only profile*3+workgroup and timings. Every child
     * certificate must describe exactly the same scalar geometry/quality key. */
    private static boolean tuningChild1975(String group,String key){
        if(!tuningKey1975(group)||key==null)return false;
        boolean newest=group.startsWith(TUNING_PREFIX1978),expanded=newest||group.startsWith(TUNING_PREFIX1976);
        String suffix=group.substring((newest?TUNING_PREFIX1978:expanded?TUNING_PREFIX1976:TUNING_PREFIX1975).length());
        return key.equals("strong-gx1973-ieee-div-policy-bank-v1:"+suffix)||
            key.equals("strong-gx1964-parallel-policy-bank-v1:"+suffix)||
            key.equals("strong-gx1971-generic-policy-bank-v1:"+suffix)||
            expanded&&key.equals("strong-gx1976-ieee-tile8-policy-bank-v1:"+suffix)||
            newest&&(key.equals("strong-gx1978-policy-direct-ieee-v1:"+suffix)||key.equals("strong-gx1978-policy-direct-tile8-v1:"+suffix)||
                key.equals("strong-gx1978-policy-direct-pow2-v1:"+suffix)||key.equals("strong-gx1978-policy-direct-pow2-tile8-v1:"+suffix));
    }
    private static final LinkedHashMap<String,Failure> FAILURES=new LinkedHashMap<String,Failure>(LIMIT,.75f,true);
    private static final class Failure {
        boolean exact;int retries;long retryAfter;String cause="cached_legacy_negative";
        // Diagnostic metadata only. Nonexact failures are created exclusively
        // by rejectReason under LOCK; persisted failures restored here are exact.
        boolean immediateRetry1982;
        Failure(boolean exact){this.exact=exact;}
    }
    private static final LinkedHashMap<String,Record> RECORDS=new LinkedHashMap<String,Record>(LIMIT,.75f,true);
    private static final ThreadLocal<Job> CURRENT=new ThreadLocal<Job>();
    private static volatile SharedPreferences preferences;
    private static volatile String base="";
    private static final int MAX_JOBS=8;
    private static final ArrayDeque<Job> QUEUED=new ArrayDeque<Job>();
    private static Job activeJob;
    // Alternate completed admission opportunities, including across a capture
    // cancellation. A long resident proof cannot repeatedly starve the legacy
    // bootstrap, and repeated Strong snapshots cannot starve whole finishing.
    private static boolean lastLegacy1981;
    private static long retained;
    private static Thread worker;
    private static long lastCapture=System.nanoTime();
    private static final class Job implements Cancellation {
        final String key,environment;final long epoch,bytes;final Probe probe;final boolean speedRetry,fullStrong;
        final String speedKey1978;final boolean companionRetry1978;boolean companionCounted1978;
        volatile boolean cancelled,running;boolean failureCounted;
        Job(String key,long bytes,Probe probe,String environment,boolean retry){this(key,bytes,probe,environment,retry,null,false);}
        Job(String key,long bytes,Probe probe,String environment,boolean retry,String companion,boolean companionRetry){this.key=key;this.bytes=bytes;this.probe=probe;this.environment=environment;speedRetry=retry;speedKey1978=companion;companionRetry1978=companionRetry;fullStrong=fullStrongKey(key);epoch=ProcessingTiming1947.captureEpoch1953();}
        public boolean cancelled(){return cancelled||Thread.currentThread().isInterrupted()||
            epoch!=ProcessingTiming1947.captureEpoch1953()||!SaveQueue1935.idle1953();}
    }
    public static void initialize(Context context) {
        if(context==null)return;
        try {
            Context app=context.getApplicationContext();if(app==null)app=context;
            PackageInfo info=app.getPackageManager().getPackageInfo(app.getPackageName(),0);
            long code=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
            String identity=Build.FINGERPRINT+"|"+Build.VERSION.SDK_INT+"|"+app.getPackageName()+"|"+info.versionName+":"+code+"|"+SCHEMA;
            if(Build.FINGERPRINT==null||Build.FINGERPRINT.length()==0||"unknown".equals(Build.FINGERPRINT))return;
            synchronized(LOCK){base=identity;preferences=app.getSharedPreferences("ulike_gx1961_proofs",Context.MODE_PRIVATE);}
        } catch(Exception unavailable) { }
    }
    public static boolean background(){return CURRENT.get()!=null;}
    public static boolean cancelled(){Job job=CURRENT.get();return job!=null&&job.cancelled();}
    public static long retainedBytes(){synchronized(LOCK){return retained;}}
    /** Cancel every detached snapshot from the previous capture. The active
     * probe keeps its ownership until run() exits; no array is cleared under a
     * running oracle. Queued probes are detached first and then closed once. */
    public static void captureChanged() {
        ArrayList<Job> release=new ArrayList<Job>();
        synchronized(LOCK){
            lastCapture=System.nanoTime();
            while(!QUEUED.isEmpty()){
                Job job=QUEUED.removeFirst();job.cancelled=true;release.add(job);
            }
            if(activeJob!=null){activeJob.cancelled=true;if(worker!=null)worker.interrupt();}
            LOCK.notifyAll();
        }
        for(Job job:release){close(job.probe);synchronized(LOCK){retained-=job.bytes;LOCK.notifyAll();}}
    }
    public static void wake(){synchronized(LOCK){LOCK.notifyAll();}}
    /** Admission for another detached proof is separate from maySchedule(),
     * whose accepted-key semantics also permit the foreground GPU route. A late
     * full Strong request may retire queued lower-priority snapshots here,
     * before its caller clones another image. The active oracle is never retired.
     * Retired jobs can only retry from a fresh future capture's snapshot. */
    public static boolean canQueue(String key,long bytes) {
        return canQueue1983(key,bytes).accepted;
    }
    /** The same possibly reclaiming preflight as canQueue, not a live status
     * query. Callers must use this returned outcome instead of querying again
     * to explain a preceding boolean decision. */
    public static QueueDecision1983 canQueue1983(String key,long bytes) {
        synchronized(LOCK){return canQueueLocked1983(key,bytes);}
    }
    private static boolean fullStrongKey(String key) {
        if(key==null||!key.startsWith("strong-"))return false;
        int mode=key.indexOf(':');return mode>=0&&key.startsWith(":3:",mode);
    }
    private static boolean legacyStrong1981(String key){
        return fullStrongKey(key)&&!key.startsWith("strong-resident")&&!key.startsWith("strong-layout");
    }
    private static QueueDecision1983 decisionLocked1983(int reason,long bytes) {
        return decisionLocked1983(reason,bytes,0,0);
    }
    private static QueueDecision1983 decisionLocked1983(int reason,long bytes,long remaining,int retries) {
        return new QueueDecision1983(reason,Math.max(0L,remaining),retries,QUEUED.size(),
            activeJob==null?0:1,retained,bytes);
    }
    /** null means the existing retry predicate allows admission. A failure's
     * reason and remaining cooldown are computed from the very same observation
     * used to reject it, while its mutation lock is still held. */
    private static QueueDecision1983 permissionLocked1983(String key,String environment,long bytes) {
        if(key==null||key.length()==0)return decisionLocked1983(QueueDecision1983.INVALID_KEY,bytes);
        if(environment==null)return decisionLocked1983(QueueDecision1983.ENVIRONMENT_UNAVAILABLE,bytes);
        String name=recordKey(key,environment);
        if(RECORDS.containsKey(name)&&!FAILURES.containsKey(name))return null;
        Failure failure=failure(key,environment);
        if(failure==null)return null;
        if(failure.exact)return decisionLocked1983(QueueDecision1983.EXACT_REJECTED,bytes);
        if(strongPreferred1970(key))return null;
        if(failure.retries>=MAX_SPEED_RETRIES)
            return decisionLocked1983(QueueDecision1983.RETRY_LIMIT,bytes,0,failure.retries);
        long elapsed=System.nanoTime()-failure.retryAfter;
        if(elapsed<0)return decisionLocked1983(QueueDecision1983.RETRY_COOLDOWN,bytes,
            elapsed==Long.MIN_VALUE?Long.MAX_VALUE:-elapsed,failure.retries);
        return null;
    }
    private static int capacityReasonLocked1983(long bytes) {
        boolean memory=retained>MAX_RETAINED-bytes;
        boolean count=QUEUED.size()+(activeJob==null?0:1)>=MAX_JOBS;
        return memory?(count?QueueDecision1983.JOB_AND_RETAINED_LIMIT:QueueDecision1983.RETAINED_LIMIT)
            :QueueDecision1983.JOB_LIMIT;
    }
    /** Retain the historical private DEX signature for existing closures. */
    private static boolean canQueueLocked(String key,long bytes) {
        return canQueueLocked1983(key,bytes).accepted;
    }
    private static QueueDecision1983 canQueueLocked1983(String key,long bytes) {
        if(key==null||key.length()==0)return decisionLocked1983(QueueDecision1983.INVALID_KEY,bytes);
        if(bytes<=0)return decisionLocked1983(QueueDecision1983.INVALID_BYTES,bytes);
        if(bytes>MAX_RETAINED)return decisionLocked1983(QueueDecision1983.REQUEST_TOO_LARGE,bytes);
        if(background())return decisionLocked1983(QueueDecision1983.BACKGROUND,bytes);
        if(Thread.currentThread().isInterrupted())return decisionLocked1983(QueueDecision1983.INTERRUPTED,bytes);
        QueueDecision1983 permission=permissionLocked1983(key,environment(),bytes);
        if(permission!=null)return permission;
        if(restore(key)!=null)return decisionLocked1983(QueueDecision1983.ALREADY_QUALIFIED,bytes);
        if(activeJob!=null&&key.equals(activeJob.key))return decisionLocked1983(QueueDecision1983.SAME_RUNNING,bytes);
        for(Job job:QUEUED)if(key.equals(job.key))return decisionLocked1983(QueueDecision1983.SAME_QUEUED,bytes);
        boolean strong=legacyStrong1981(key);int jobs=QUEUED.size()+(activeJob==null?0:1),early=0;
        long expendable=0;
        boolean preferred=strong!=lastLegacy1981;
        for(Job job:QUEUED){if(!legacyStrong1981(job.key))early++;if(preferred&&legacyStrong1981(job.key)!=strong)expendable+=job.bytes;}
        if(activeJob!=null&&!legacyStrong1981(activeJob.key))early++;
        // Keep one slot available for a late full Strong snapshot. Earlier
        // resident-model proofs may use the complete byte budget; they are not
        // permanently excluded merely because their maps exceed 32 MiB.
        if(!strong&&early>=MAX_JOBS-1)return decisionLocked1983(QueueDecision1983.RESERVED_SLOT,bytes);
        if(retained>MAX_RETAINED-bytes||jobs>=MAX_JOBS){
            if(!preferred||retained-expendable>MAX_RETAINED-bytes)
                return decisionLocked1983(capacityReasonLocked1983(bytes),bytes);
            int evictable=0;for(Job job:QUEUED)if(legacyStrong1981(job.key)!=strong)evictable++;
            if(jobs-evictable>=MAX_JOBS)return decisionLocked1983(capacityReasonLocked1983(bytes),bytes);
            while(retained>MAX_RETAINED-bytes||QUEUED.size()+(activeJob==null?0:1)>=MAX_JOBS){
                Job retire=null;
                for(Job job:QUEUED)if(legacyStrong1981(job.key)!=strong&&(retire==null||job.bytes>retire.bytes))retire=job;
                if(retire==null)return decisionLocked1983(capacityReasonLocked1983(bytes),bytes);
                QUEUED.remove(retire);retire.cancelled=true;
                close(retire.probe); // Snapshot ownership ends before byte budget is released.
                retained-=retire.bytes;LOCK.notifyAll();
            }
        }
        return decisionLocked1983(QueueDecision1983.READY,bytes);
    }
    /** Ownership of probe is transferred even if scheduling is declined. */
    public static boolean schedule(String key,long bytes,Probe probe) {
        return scheduleOwned1978(key,null,bytes,probe);
    }
    /** Ownership is transferred exactly as for schedule. The return value is
     * the final locked admission result, including races after preflight. */
    public static QueueDecision1983 schedule1983(String key,long bytes,Probe probe) {
        return scheduleOwned1983(key,null,bytes,probe);
    }
    /** The tuner can own one worker-bound CPU fallback speed trial. It is
     * separate from the preferred aggregate, so a failed fallback cannot write
     * a fake aggregate failure or evade the existing three-retry limit. */
    static boolean scheduleStrong1978(String key,String cpuKey,long bytes,Probe probe){
        if(!cpuTuningChild1978(key,cpuKey)){close(probe);return false;}
        return scheduleOwned1978(key,cpuKey,bytes,probe);
    }
    private static boolean scheduleOwned1978(String key,String companion,long bytes,Probe probe){
        return scheduleOwned1983(key,companion,bytes,probe).accepted;
    }
    private static QueueDecision1983 scheduleOwned1983(String key,String companion,long bytes,Probe probe){
        if(probe==null){synchronized(LOCK){return decisionLocked1983(QueueDecision1983.NO_PROBE,bytes);}}
        boolean accepted=false;QueueDecision1983 decision=UNAVAILABLE1983;
        try {
            synchronized(LOCK) {
                QueueDecision1983 secondaryPermission=companion==null?null:permissionLocked1983(companion,environment(),bytes);
                if(secondaryPermission!=null)decision=secondaryPermission;
                else if(companion!=null&&restore(companion)!=null)decision=decisionLocked1983(QueueDecision1983.ALREADY_QUALIFIED,bytes);
                else decision=canQueueLocked1983(key,bytes);
                if(decision.accepted) {
                    if(worker==null||!worker.isAlive()) {
                        Thread made=new Thread(new Runnable(){public void run(){loop();}},"Hiro-ULike-GX-proof");
                        made.setDaemon(true);made.setPriority(Thread.MIN_PRIORITY);made.start();worker=made;
                    }
                    String environment=environment();Failure failure=failure(key,environment);
                    Failure secondary=companion==null?null:failure(companion,environment);
                    Job job=new Job(key,bytes,probe,environment,failure!=null,companion,secondary!=null&&!secondary.exact);
                    // Allocate the scalar result before ownership changes. An
                    // optional diagnostic allocation cannot strand a queued probe.
                    QueueDecision1983 scheduled=new QueueDecision1983(QueueDecision1983.SCHEDULED,0,0,
                        QUEUED.size()+1,activeJob==null?0:1,retained+bytes,bytes);
                    QUEUED.addLast(job);retained+=bytes;accepted=true;decision=scheduled;LOCK.notifyAll();
                }
            }
        } catch(RuntimeException unavailable) {decision=UNAVAILABLE1983;}
          catch(OutOfMemoryError unavailable) {decision=MEMORY1983;}
        if(!accepted)close(probe);return decision;
    }
    private static void loop() {
        for(;;) {
            Job job;
            synchronized(LOCK) {
                for(;;) {
                    job=QUEUED.peekFirst();
                    // FIFO within each group; the first quiet opportunity goes
                    // to legacy Strong, then the other group gets its turn.
                    // The scalar turn survives cancellation; images do not.
                    for(Job queued:QUEUED)if(legacyStrong1981(queued.key)!=lastLegacy1981){job=queued;break;}
                    if(job!=null&&(job.cancelled||job.epoch!=ProcessingTiming1947.captureEpoch1953()))break;
                    if(job!=null&&System.nanoTime()-lastCapture>=QUIET&&SaveQueue1935.idle1953()&&
                            !GpuNoise1960.sessionBusy()&&WholeRoute1953.retainedBytes()==0)break;
                    try{LOCK.wait(250);}catch(InterruptedException ignored){}
                }
                QUEUED.remove(job);activeJob=job;job.running=true;lastLegacy1981=legacyStrong1981(job.key);
            }
            try {
                if(!job.cancelled()){CURRENT.set(job);job.probe.run(job);}
            } catch(RuntimeException unavailable) { } catch(LinkageError unavailable) { } catch(OutOfMemoryError unavailable) { } catch(AssertionError unavailable) { }
            finally {
                CURRENT.remove();close(job.probe);
                synchronized(LOCK){if(activeJob==job)activeJob=null;retained-=job.bytes;LOCK.notifyAll();}
                Thread.interrupted();
            }
        }
    }
    /** Current known cache/queue state only: no image, GPU query or timing-lock
     * lookup. Counts are deliberately not described as this photo's route. */
    public static String status1967() {
        synchronized(LOCK){
            int eligible=0,cooldown=0,exhausted=0,exact=0;long now=System.nanoTime();
            for(Failure failure:FAILURES.values()){
                if(failure.exact)exact++;
                else if(failure.immediateRetry1982)eligible++;
                else if(failure.retries>=MAX_SPEED_RETRIES)exhausted++;
                else if(now-failure.retryAfter>=0)eligible++;
                else cooldown++;
            }
            return "CPU/GPU再判定（現在）: 予約済み"+QUEUED.size()+"件 / 実行中"+(activeJob==null?0:1)+"件\n"+
                "直近照会キャッシュ: 確認済み記録"+RECORDS.size()+"件 / 画質不一致の拒否"+exact+"件\n"+
                "画質不一致以外の履歴（速度・利用失敗など）: 再試行の時間条件内"+eligible+
                "件 / 再試行間隔待ち"+cooldown+"件 / 再試行上限"+exhausted+"件\n"+
                "※履歴は予約件数・この写真の採用数・保存証明の総数ではありません。";
        }
    }
    private static void close(Probe probe){try{probe.close();}catch(RuntimeException ignored){}catch(LinkageError ignored){}catch(OutOfMemoryError ignored){}catch(AssertionError ignored){}}
    private static String environment() {
        String nativeIdentity=GpuNoise1960.fingerprint();
        return base.length()==0||nativeIdentity==null||nativeIdentity.length()==0?null:digest(base+"|"+nativeIdentity);
    }
    private static String recordKey(String key,String environment){return "proof-"+digest(environment+"|"+key);}
    public static Record restore(String key) {
        String environment=environment();if(environment==null||key==null)return null;
        String name=recordKey(key,environment);
        synchronized(LOCK) {
            Record cached=RECORDS.get(name);if(cached!=null)return cached;
            Failure failure=failure(key,environment);
            if(failure!=null&&(failure.exact||!strongPreferred1970(key)))return null;
            SharedPreferences p=preferences;if(p==null)return null;
            try {
                String raw=p.getString(name,"");String[] fields=raw.split(":",-1);
                if(fields.length!=6)return null;
                boolean preferred=STRONG_PREFERRED_SCHEMA.equals(fields[0])&&strongPreferred1970(key);
                if(!preferred&&!PROOF_SCHEMA1977.equals(fields[0]))return null;
                long cpu=Long.parseLong(fields[1]),gpu=Long.parseLong(fields[2]),stamp=Long.parseLong(fields[4]);int variant=Integer.parseInt(fields[3]);
                String core=fields[0]+":"+fields[1]+":"+fields[2]+":"+fields[3]+":"+fields[4];
                if(cpu<=0||gpu<=0||(!preferred&&gpu>cpu-cpu/20)||variant<0||variant>tuningChoiceLimit1976(key)||stamp<=0||
                        !digest(environment+"|"+key+"|"+core).equals(fields[5]))return null;
                Record result=new Record(cpu,gpu,variant);RECORDS.put(name,result);trim();return result;
            } catch(RuntimeException malformed){return null;}
        }
    }
    /** Caller must have proved every output value in two full trials with the
     * same variant. The service also enforces the inclusive 5% timing margin. */
    public static void qualified(String key,long cpu,long gpu,int variant) {
        synchronized(PERSIST_LOCK){persistQualified(key,cpu,gpu,variant,false,null);}
    }
    /** Only a full Strong caller that proved every ARGB and confidence value
     * twice with one variant may publish this GPU-preferred certificate. A
     * previous speed failure supplies no proof and can only permit rechecking. */
    public static void qualifiedStrongPreferred1970(String key,long cpu,long gpu,int variant) {
        if(!strongPreferred1970(key))return;
        synchronized(PERSIST_LOCK){persistQualified(key,cpu,gpu,variant,true,null);}
    }
    /** An idle tuning job may certify a child profile only after its own two
     * complete exact trials. Foreground callers cannot use this authorization. */
    public static void qualifiedStrongProfile1975(String group,String key,long cpu,long gpu,int variant) {
        Job job=CURRENT.get();
        if(group==null||job==null||!group.equals(job.key)||job.cancelled()||!tuningChild1975(group,key)||variant<0||variant>2)return;
        synchronized(PERSIST_LOCK){persistQualified(key,cpu,gpu,variant,true,group);}
    }
    /** Retire only a stale aggregate choice, never its child proof or any exact
     * rejection. Bounded LRU eviction can otherwise strand the choice forever. */
    public static void invalidateStrongTuning1975(String group,int expectedChoice) {
        if(!tuningKey1975(group)&&!cpuTuningKey1978(group))return;
        synchronized(PERSIST_LOCK){synchronized(LOCK){
            Record record=restore(group);if(record==null||record.variant!=expectedChoice)return;
            String environment=environment();if(environment==null)return;
            String name=recordKey(group,environment);RECORDS.remove(name);
            SharedPreferences p=preferences;
            try{if(p!=null)p.edit().remove(name).apply();}catch(RuntimeException unavailable){}
        }}
    }
    private static void persistQualified(String key,long cpu,long gpu,int variant,boolean preferred,String tuningGroup) {
        if(key==null||(preferred&&Thread.currentThread().isInterrupted())||cpu<=0||gpu<=0||
                (!preferred&&gpu>cpu-cpu/20)||variant<0||variant>tuningChoiceLimit1976(key))return;
        String environment=environment();if(environment==null)return;
        String name=recordKey(key,environment);SharedPreferences p=preferences;
        SharedPreferences.Editor edit=null;
        if(p!=null)try {
                edit=p.edit();Map<String,?> all=p.getAll();
                int count=0;String oldest=null;long oldestTime=Long.MAX_VALUE;
                for(Map.Entry<String,?> entry:all.entrySet())if(entry.getKey().startsWith("proof-")) {
                    count++;if(name.equals(entry.getKey()))continue;
                    String[] fields=String.valueOf(entry.getValue()).split(":",-1);long stamp=0;
                    try{if(fields.length==6)stamp=Long.parseLong(fields[4]);}catch(RuntimeException malformed){}
                    if(stamp<oldestTime){oldestTime=stamp;oldest=entry.getKey();}
                }
                if(count>=LIMIT&&!all.containsKey(name)&&oldest!=null)edit.remove(oldest);
                String core=(preferred?STRONG_PREFERRED_SCHEMA:PROOF_SCHEMA1977)+":"+cpu+":"+gpu+":"+variant+":"+System.currentTimeMillis();
                edit.putString(name,core+":"+digest(environment+"|"+key+"|"+core));
        } catch(RuntimeException unavailable) {edit=null;}
        synchronized(LOCK) {
            if(preferred&&Thread.currentThread().isInterrupted())return;
            Job job=CURRENT.get();if(job!=null&&(activeJob!=job||job.cancelled()||!environment.equals(job.environment)||
                    (preferred&&!key.equals(job.key)&&!(tuningGroup!=null&&tuningGroup.equals(job.key)&&tuningChild1975(tuningGroup,key)))))return;
            if(!environment.equals(environment()))return;
            Failure failure=failure(key,environment);if(failure!=null&&failure.exact)return;
            FAILURES.remove(name);
            RECORDS.put(name,new Record(cpu,gpu,variant));trim();
            try{if(edit!=null)edit.apply();}catch(RuntimeException unavailable){}
        }
    }
    /** GX29: speed failure is temporary. Reproof is eligible only after 30s,
     * runs in the existing idle worker, and has three unsuccessful retries at
     * most per key/environment. Cancellation never spends a retry. No picture
     * is retained across cooldown; the next foreground CPU result can supply
     * a new immutable proof snapshot once this predicate becomes true. Full
     * Strong's preferred policy can recheck speed-only failures immediately;
     * their history never supplies a two-exact-trial certificate. */
    public static boolean maySchedule(String key) {
        if(key==null||key.length()==0)return false;
        String environment=environment();if(environment==null)return false;
        synchronized(LOCK){return permissionLocked1983(key,environment,0)==null;}
    }
    public static boolean exactRejected(String key) {
        String environment=environment();synchronized(LOCK){Failure failure=failure(key,environment);return failure!=null&&failure.exact;}
    }
    /** Classification only: this never changes a rejection or permits GPU. */
    public static String exactFailure1971(String key) {
        String environment=environment();synchronized(LOCK){Failure failure=failure(key,environment);
            return failure!=null&&failure.exact?failure.cause:null;}
    }
    private static boolean typedCause1971(String cause) {
        return "argb_mismatch".equals(cause)||"confidence_mismatch".equals(cause)||"policy_failure".equals(cause);
    }
    private static String cachedCause1971(String cause) {
        return "argb_mismatch".equals(cause)?"cached_argb_negative":
            "confidence_mismatch".equals(cause)?"cached_confidence_negative":
            "policy_failure".equals(cause)?"cached_policy_negative":"cached_legacy_negative";
    }
    /** Exact failures are safety evidence, not an evictable result cache. The
     * key omits source pixels, so forgetting a mismatch and certifying a later
     * easy image could wrongly re-enable a known-bad configuration. Persist only
     * real per-key failures, with an explicit environment/family namespace; never
     * derive a rejection from how many other keys have failed. LIMIT bounds the
     * hot RAM cache only. Unknown-key lookups never extend this journal. */
    private static String rejectionKey1977(String key,String environment) {
        int separator=key.indexOf(':');String family=separator<0?key:key.substring(0,separator);
        return "reject1977-"+environment+"-"+digest(family)+"-"+digest(key);
    }
    private static String journalCause1977(SharedPreferences p,String key,String environment) {
        String[] fields=p.getString(rejectionKey1977(key,environment),"").split(":",-1);
        return fields.length==3&&EXACT_SCHEMA1977.equals(fields[0])&&
            ("exact".equals(fields[1])||typedCause1971(fields[1]))&&
            digest(environment+"|"+key+"|exact1977|"+fields[1]).equals(fields[2])?fields[1]:null;
    }
    private static Failure failure(String key,String environment) {
        if(key==null||environment==null)return null;
        String name=recordKey(key,environment);Failure known=FAILURES.get(name);if(known!=null)return known;
        SharedPreferences p=preferences;if(p==null)return null;
        try {
            String cause=null;
            try{cause=journalCause1977(p,key,environment);}catch(RuntimeException malformedJournal){}
            String raw="";
            try{raw=p.getString("reject-"+digest(environment+"|"+key),"");}catch(RuntimeException malformedLegacy){}
            boolean direct=raw.equals(SCHEMA+":"+digest(environment+"|"+key+"|exact"));
            // A legacy capacity marker is not evidence about this key. The
            // positive epoch above forces a fresh proof for such unknown keys;
            // signed legacy individual failures remain authoritative.
            if(cause==null&&!direct)return null;
            Failure exact=new Failure(true);
            if(cause!=null)exact.cause=cachedCause1971(cause);
            if(!typedCause1971(cause)&&direct)try {
                String[] fields=p.getString("failure-cause-"+digest(environment+"|"+key),"").split(":",-1);
                if(fields.length==3&&SCHEMA.equals(fields[0])&&typedCause1971(fields[1])&&
                        digest(environment+"|"+key+"|cause|"+fields[1]).equals(fields[2]))exact.cause=cachedCause1971(fields[1]);
            } catch(RuntimeException unavailableCause) { /* Rejection remains authoritative. */ }
            FAILURES.put(name,exact);trimFailures();return exact;
        } catch(RuntimeException unavailable){return null;}
    }
    /** A failed output comparison is never reconsidered for this app, OS,
     * driver and shader fingerprint. Existing callers of reject remain exact. */
    public static void reject(String key){rejectExact(key);}
    public static void rejectExact(String key){rejectReason(key,true,null);}
    public static void rejectExact1971(String key,String cause){rejectReason(key,true,typedCause1971(cause)?cause:null);}
    public static void rejectSpeed(String key){rejectReason(key,false,null);}
    private static void rejectReason(String key,boolean exact,String cause) {
        if(key==null||Thread.currentThread().isInterrupted())return;
        synchronized(PERSIST_LOCK) {
            String environment=environment();if(environment==null)return;
            String name=recordKey(key,environment);SharedPreferences p=preferences;
            SharedPreferences.Editor edit=null;
            try {
                if(p!=null){edit=p.edit().remove(name);if(exact){
                    String detail=cause;
                    if(detail==null)try{detail=journalCause1977(p,key,environment);}catch(RuntimeException malformedJournal){}
                    if(detail==null)detail="exact";
                    edit.putString(rejectionKey1977(key,environment),EXACT_SCHEMA1977+":"+detail+":"+
                        digest(environment+"|"+key+"|exact1977|"+detail));
                }}
            } catch(RuntimeException unavailable){edit=null;}
            synchronized(LOCK) {
                Job job=CURRENT.get();if(job!=null&&(activeJob!=job||job.cancelled()||!environment.equals(job.environment)))return;
                if(!environment.equals(environment()))return;
                Failure failure=failure(key,environment);
                if(failure==null){failure=new Failure(exact);FAILURES.put(name,failure);}
                if(exact){failure.exact=true;if(cause!=null)failure.cause=cachedCause1971(cause);}
                else if(!failure.exact){
                    failure.immediateRetry1982=strongPreferred1970(key);
                    if(job!=null&&job.speedRetry&&job.key.equals(key)&&!job.failureCounted){failure.retries++;job.failureCounted=true;}
                    if(job!=null&&job.companionRetry1978&&key.equals(job.speedKey1978)&&!job.companionCounted1978){failure.retries++;job.companionCounted1978=true;}
                    failure.retryAfter=System.nanoTime()+RETRY_COOLDOWN;
                }
                RECORDS.remove(name);trimFailures();
                try{if(edit!=null)edit.apply();}catch(RuntimeException unavailable){}
            }
        }
    }
    private static void trimFailures(){while(FAILURES.size()>LIMIT)FAILURES.remove(FAILURES.keySet().iterator().next());}
    private static void trim(){while(RECORDS.size()>LIMIT)RECORDS.remove(RECORDS.keySet().iterator().next());}
    private static String digest(String value) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out=new StringBuilder(bytes.length*2);char[] hex="0123456789abcdef".toCharArray();
            for(byte b:bytes){out.append(hex[(b>>>4)&15]);out.append(hex[b&15]);}return out.toString();
        } catch(Exception unavailable){throw new IllegalStateException("GX proof fingerprint",unavailable);}
    }
}

