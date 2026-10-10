package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
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
            SCHEDULER_UNAVAILABLE=19,SCHEDULER_MEMORY=20,COPY_NOT_STARTED1985=21;
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
    // .84 reservations own a slot and the actual copy budget before any caller
    // allocates a detached image. A cancelled copier retains that ownership
    // until its finally block closes the ticket; no live input is cleared.
    private static final ArrayDeque<Reservation1984> RESERVED1984=new ArrayDeque<Reservation1984>(MAX_JOBS+1);
    private static final IdentityHashMap<Object,Shared1984> SHARED1984=new IdentityHashMap<Object,Shared1984>(MAX_JOBS+1);
    private static final class Shared1984 {final long bytes;int references;Shared1984(long bytes){this.bytes=bytes;}}
    private static int lastPrimary1984;
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
        final Object sharedOwner1984;final long sharedBytes1984;final int priority1984;
        Attempt1984 attempt1984;
        // The worker alone may hold a candidate key for its terminal recheck.
        // It never enters the retained scalar attempt ledger.
        String selectionKey1985;int selectionProfile1985= -1,selectionVariant1985= -1;
        boolean selectionReported1985,selectionSeen1985,selectionInvalidated1985;
        volatile boolean cancelled,running;boolean failureCounted;
        Job(String key,long bytes,Probe probe,String environment,boolean retry){this(key,bytes,probe,environment,retry,null,false);}
        Job(String key,long bytes,Probe probe,String environment,boolean retry,String companion,boolean companionRetry){this(key,bytes,probe,environment,retry,companion,companionRetry,null,0,0,null);}
        Job(String key,long bytes,Probe probe,String environment,boolean retry,String companion,boolean companionRetry,
                Object sharedOwner,long sharedBytes,int priority,Attempt1984 attempt){
            this(key,bytes,probe,environment,retry,companion,companionRetry,sharedOwner,sharedBytes,priority,attempt,ProcessingTiming1947.captureEpoch1953());
        }
        Job(String key,long bytes,Probe probe,String environment,boolean retry,String companion,boolean companionRetry,
                Object sharedOwner,long sharedBytes,int priority,Attempt1984 attempt,long sourceEpoch){
            this.key=key;this.bytes=bytes;this.probe=probe;this.environment=environment;speedRetry=retry;
            speedKey1978=companion;companionRetry1978=companionRetry;fullStrong=fullStrongKey(key);
            epoch=sourceEpoch;sharedOwner1984=sharedOwner;
            sharedBytes1984=sharedBytes;priority1984=priority;attempt1984=attempt;
        }
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
            for(Iterator<Reservation1984> it=RESERVED1984.iterator();it.hasNext();){
                Reservation1984 reservation=it.next();reservation.cancelled=true;
                finishAttempt1984(reservation.attempt,OUT_CANCELLED1984);
                // Only callers of the new API promise that begin1985 precedes
                // their first image allocation. Legacy tickets remain held.
                if(reservation.revokeBeforeCopy1985())it.remove();
            }
            if(activeJob!=null){activeJob.cancelled=true;if(worker!=null)worker.interrupt();}
            LOCK.notifyAll();
        }
        for(Job job:release){close(job.probe);synchronized(LOCK){finishAttempt1984(job.attempt1984,OUT_CANCELLED1984);releaseLocked1984(job.bytes,job.sharedOwner1984);LOCK.notifyAll();}}
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
        return new QueueDecision1983(reason,Math.max(0L,remaining),retries,queuedCount1984(),
            activeJob==null?0:1,retained,bytes);
    }
    private static int queuedCount1984(){return QUEUED.size()+RESERVED1984.size();}
    private static void releaseLocked1984(long bytes,Object owner){
        retained-=bytes;
        if(owner!=null){Shared1984 shared=SHARED1984.get(owner);
            if(shared!=null&&--shared.references==0){retained-=shared.bytes;SHARED1984.remove(owner);}}
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
        boolean count=queuedCount1984()+(activeJob==null?0:1)>=MAX_JOBS;
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
        for(Reservation1984 reservation:RESERVED1984)if(key.equals(reservation.key))return decisionLocked1983(QueueDecision1983.SAME_QUEUED,bytes);
        boolean strong=legacyStrong1981(key);int jobs=queuedCount1984()+(activeJob==null?0:1),early=0;
        long expendable=0;
        boolean preferred=strong!=lastLegacy1981;
        for(Job job:QUEUED){if(!legacyStrong1981(job.key))early++;if(preferred&&job.priority1984==0&&legacyStrong1981(job.key)!=strong)expendable+=job.bytes;}
        for(Reservation1984 reservation:RESERVED1984)if(!legacyStrong1981(reservation.key))early++;
        if(activeJob!=null&&!legacyStrong1981(activeJob.key))early++;
        // Keep one slot available for a late full Strong snapshot. Earlier
        // resident-model proofs may use the complete byte budget; they are not
        // permanently excluded merely because their maps exceed 32 MiB.
        if(!strong&&early>=MAX_JOBS-1)return decisionLocked1983(QueueDecision1983.RESERVED_SLOT,bytes);
        if(retained>MAX_RETAINED-bytes||jobs>=MAX_JOBS){
            if(!preferred||retained-expendable>MAX_RETAINED-bytes)
                return decisionLocked1983(capacityReasonLocked1983(bytes),bytes);
            int evictable=0;for(Job job:QUEUED)if(job.priority1984==0&&legacyStrong1981(job.key)!=strong)evictable++;
            if(jobs-evictable>=MAX_JOBS)return decisionLocked1983(capacityReasonLocked1983(bytes),bytes);
            while(retained>MAX_RETAINED-bytes||queuedCount1984()+(activeJob==null?0:1)>=MAX_JOBS){
                Job retire=null;
                for(Job job:QUEUED)if(job.priority1984==0&&legacyStrong1981(job.key)!=strong&&(retire==null||job.bytes>retire.bytes))retire=job;
                if(retire==null)return decisionLocked1983(capacityReasonLocked1983(bytes),bytes);
                QUEUED.remove(retire);retire.cancelled=true;
                close(retire.probe); // Snapshot ownership ends before byte budget is released.
                finishAttempt1984(retire.attempt1984,OUT_RETIRED1984);releaseLocked1984(retire.bytes,retire.sharedOwner1984);LOCK.notifyAll();
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
    /** .84: reserve real capacity before cloning. The returned ticket owns no
     * pixels itself; its budget covers the caller's not-yet-materialized copy.
     * A successful ticket must be closed in finally, including after commit. */
    public static Reservation1984 reserve1984(String key,long bytes){
        return reserveOwned1984(key,null,bytes,null,0);
    }
    /** An unstarted .85 ticket may be revoked by captureChanged(). The caller
     * must obtain begin1985() immediately before its first snapshot allocation. */
    public static Reservation1984 reserve1985(String key,long bytes){
        return reserveOwned1985(key,null,bytes,null,0,true);
    }
    /** The immutable model is identified by object identity, never equals().
     * Companion speed eligibility is checked by the tuner immediately before
     * that comparison, independently of the main exact-profile recovery. */
    public static Reservation1984 reserveStrongShared1984(String key,String companion,long exclusiveBytes,
            Object immutableOwner,long sharedBytes){
        if(!cpuTuningChild1978(key,companion))return declinedReservation1984(QueueDecision1983.INVALID_KEY,exclusiveBytes);
        return reserveOwned1984(key,companion,exclusiveBytes,immutableOwner,sharedBytes);
    }
    public static Reservation1984 reserveStrongShared1985(String key,String companion,long exclusiveBytes,
            Object immutableOwner,long sharedBytes){
        if(!cpuTuningChild1978(key,companion))return declinedReservation1984(QueueDecision1983.INVALID_KEY,exclusiveBytes);
        return reserveOwned1985(key,companion,exclusiveBytes,immutableOwner,sharedBytes,true);
    }
    public static final class Reservation1984 implements AutoCloseable {
        public final QueueDecision1983 decision;
        private String key,environment,companion;private Object owner;
        private final long bytes,sharedBytes,epoch;private final int priority;
        private final boolean revocableBeforeCopy1985;
        private boolean begun1985;
        private boolean held,cancelled;private Attempt1984 attempt;
        private Reservation1984(QueueDecision1983 decision){
            this.decision=decision;bytes=sharedBytes=epoch=0;priority=0;revocableBeforeCopy1985=false;
        }
        private Reservation1984(QueueDecision1983 decision,String key,String environment,String companion,
                long bytes,Object owner,long sharedBytes,int priority){
            this(decision,key,environment,companion,bytes,owner,sharedBytes,priority,false);
        }
        private Reservation1984(QueueDecision1983 decision,String key,String environment,String companion,
                long bytes,Object owner,long sharedBytes,int priority,boolean revocable){
            this.decision=decision;this.key=key;this.environment=environment;this.companion=companion;
            this.bytes=bytes;this.owner=owner;this.sharedBytes=sharedBytes;this.priority=priority;
            epoch=ProcessingTiming1947.captureEpoch1953();revocableBeforeCopy1985=revocable;
        }
        public boolean accepted(){return decision.accepted;}
        public boolean current(){synchronized(LOCK){return held&&!cancelled&&!Thread.currentThread().isInterrupted()&&
            epoch==ProcessingTiming1947.captureEpoch1953();}}
        public boolean begin1985(){synchronized(LOCK){
            if(begun1985||!current())return false;
            begun1985=true;return true;
        }}
        /** Probe ownership transfers even when the ticket was cancelled or a
         * certificate arrived during allocation. Capacity transfers to exactly
         * one queued job without releasing and reacquiring its shared model. */
        public QueueDecision1983 commit(Probe probe){
            boolean accepted=false;QueueDecision1983 result=UNAVAILABLE1983;
            try {synchronized(LOCK){
                if(probe==null)result=decisionLocked1983(QueueDecision1983.NO_PROBE,decision.requestedBytes);
                else if(!current())result=decisionLocked1983(QueueDecision1983.INTERRUPTED,decision.requestedBytes);
                else if(revocableBeforeCopy1985&&!begun1985)result=decisionLocked1983(QueueDecision1983.COPY_NOT_STARTED1985,decision.requestedBytes);
                else if(!environment.equals(environment()))result=decisionLocked1983(QueueDecision1983.ENVIRONMENT_UNAVAILABLE,decision.requestedBytes);
                else {
                    QueueDecision1983 permission=permissionLocked1983(key,environment,decision.requestedBytes);
                    if(permission!=null)result=permission;
                    else if(restore(key)!=null)result=decisionLocked1983(QueueDecision1983.ALREADY_QUALIFIED,decision.requestedBytes);
                    else {
                        startWorker1984();
                        Failure failure=failure(key,environment),secondary=companion==null?null:failure(companion,environment);
                        Job job=new Job(key,bytes,probe,environment,failure!=null,companion,secondary!=null&&!secondary.exact,
                            owner,sharedBytes,priority,attempt,epoch);
                        QueueDecision1983 scheduled=new QueueDecision1983(QueueDecision1983.SCHEDULED,0,0,
                            queuedCount1984(),activeJob==null?0:1,retained,decision.requestedBytes);
                        QUEUED.addLast(job);RESERVED1984.remove(this);held=false;accepted=true;result=scheduled;
                        progressAttempt1984(attempt,1,0,0);clear1984();LOCK.notifyAll();
                    }
                }
            }}catch(RuntimeException unavailable){result=UNAVAILABLE1983;}
              catch(OutOfMemoryError unavailable){result=MEMORY1983;}
            if(!accepted){if(probe!=null)GpuQualification1961.close(probe);close();}
            return result;
        }
        private void clear1984(){key=environment=companion=null;owner=null;attempt=null;}
        public void close(){synchronized(LOCK){
            if(!held)return;
            RESERVED1984.remove(this);held=false;
            finishAttempt1984(attempt,cancelled||Thread.currentThread().isInterrupted()||epoch!=ProcessingTiming1947.captureEpoch1953()?OUT_CANCELLED1984:OUT_INCOMPLETE1984);
            releaseLocked1984(bytes,owner);clear1984();LOCK.notifyAll();
        }}
        // Called under LOCK after marking this capture cancelled. Keep new
        // private-field access inside the ticket so existing compiler bridges
        // retain their prior descriptors and targets.
        boolean revokeBeforeCopy1985(){
            if(!held||!revocableBeforeCopy1985||begun1985)return false;
            held=false;releaseLocked1984(bytes,owner);clear1984();return true;
        }
    }
    private static final Reservation1984 RESERVATION_UNAVAILABLE1984=new Reservation1984(UNAVAILABLE1983);
    private static final Reservation1984 RESERVATION_MEMORY1984=new Reservation1984(MEMORY1983);
    private static Reservation1984 declinedReservation1984(int reason,long bytes){
        try{synchronized(LOCK){return new Reservation1984(decisionLocked1983(reason,bytes));}}
        catch(RuntimeException unavailable){return RESERVATION_UNAVAILABLE1984;}
        catch(OutOfMemoryError unavailable){return RESERVATION_MEMORY1984;}
    }
    private static int priority1984(String key){
        if(legacyStrong1981(key))return 2;
        if(key!=null&&(key.startsWith("strong-resident")||key.indexOf("|finish-chain1976:v2|")>=0||key.indexOf("|finish-chain1962:v1|")>=0))return 1;
        return 0;
    }
    private static int rank1984(int priority){return priority==0?0:priority==(lastPrimary1984==2?1:2)?2:1;}
    private static boolean contains1984(Job[] jobs,int count,Job value){for(int i=0;i<count;i++)if(jobs[i]==value)return true;return false;}
    /** Simulate the exact last-owner release. Incoming shared ownership keeps
     * the same model charged even if all its old queued references retire. */
    private static long projected1984(Job[] removed,int count,long bytes,Object owner,long sharedBytes){
        long projected=retained+bytes+(owner!=null&&!SHARED1984.containsKey(owner)?sharedBytes:0);
        for(int i=0;i<count;i++)projected-=removed[i].bytes;
        for(Map.Entry<Object,Shared1984> entry:SHARED1984.entrySet())if(entry.getKey()!=owner){
            int references=0;for(int i=0;i<count;i++)if(removed[i].sharedOwner1984==entry.getKey())references++;
            if(references==entry.getValue().references)projected-=entry.getValue().bytes;
        }
        return projected;
    }
    private static Reservation1984 reserveOwned1984(String key,String companion,long bytes,Object owner,long sharedBytes){
        return reserveOwned1985(key,companion,bytes,owner,sharedBytes,false);
    }
    private static Reservation1984 reserveOwned1985(String key,String companion,long bytes,Object owner,long sharedBytes,boolean revocable){
        try {synchronized(LOCK){
            if(key==null||key.length()==0)return new Reservation1984(decisionLocked1983(QueueDecision1983.INVALID_KEY,bytes));
            if(bytes<0||sharedBytes<0||owner==null&&sharedBytes!=0||bytes==0&&sharedBytes==0)
                return new Reservation1984(decisionLocked1983(QueueDecision1983.INVALID_BYTES,bytes));
            if(bytes>MAX_RETAINED||sharedBytes>MAX_RETAINED-bytes)
                return new Reservation1984(decisionLocked1983(QueueDecision1983.REQUEST_TOO_LARGE,bytes));
            if(background())return new Reservation1984(decisionLocked1983(QueueDecision1983.BACKGROUND,bytes));
            if(Thread.currentThread().isInterrupted())return new Reservation1984(decisionLocked1983(QueueDecision1983.INTERRUPTED,bytes));
            Shared1984 shared=owner==null?null:SHARED1984.get(owner);
            if(shared!=null&&shared.bytes!=sharedBytes)return new Reservation1984(decisionLocked1983(QueueDecision1983.INVALID_BYTES,bytes));
            long requested=bytes+(owner!=null&&shared==null?sharedBytes:0);
            String environment=environment();QueueDecision1983 permission=permissionLocked1983(key,environment,requested);
            if(permission!=null)return new Reservation1984(permission);
            if(restore(key)!=null)return new Reservation1984(decisionLocked1983(QueueDecision1983.ALREADY_QUALIFIED,requested));
            if(activeJob!=null&&key.equals(activeJob.key))return new Reservation1984(decisionLocked1983(QueueDecision1983.SAME_RUNNING,requested));
            for(Job job:QUEUED)if(key.equals(job.key))return new Reservation1984(decisionLocked1983(QueueDecision1983.SAME_QUEUED,requested));
            for(Reservation1984 reservation:RESERVED1984)if(key.equals(reservation.key))return new Reservation1984(decisionLocked1983(QueueDecision1983.SAME_QUEUED,requested));
            int priority=priority1984(key),rank=rank1984(priority),jobs=queuedCount1984()+(activeJob==null?0:1);
            if(priority==0&&jobs>=MAX_JOBS-1)return new Reservation1984(decisionLocked1983(QueueDecision1983.RESERVED_SLOT,requested));
            Job[] removed=new Job[MAX_JOBS];int count=0;
            long projected=projected1984(removed,count,bytes,owner,sharedBytes);
            while(projected>MAX_RETAINED||jobs-count>=MAX_JOBS){
                Job victim=null;int victimRank=Integer.MAX_VALUE;
                for(Job candidate:QUEUED){
                    int candidateRank=rank1984(candidate.priority1984);
                    if(candidateRank>=rank||contains1984(removed,count,candidate))continue;
                    if(victim==null||candidateRank<victimRank||candidateRank==victimRank&&candidate.bytes>victim.bytes){victim=candidate;victimRank=candidateRank;}
                }
                if(victim==null){boolean memory=projected>MAX_RETAINED,countLimit=jobs-count>=MAX_JOBS;
                    return new Reservation1984(decisionLocked1983(memory?(countLimit?QueueDecision1983.JOB_AND_RETAINED_LIMIT:QueueDecision1983.RETAINED_LIMIT):QueueDecision1983.JOB_LIMIT,requested));}
                removed[count++]=victim;projected=projected1984(removed,count,bytes,owner,sharedBytes);
            }
            // Allocate all ownership records before destroying any old copy.
            // Containers are pre-sized for the eight-job bound. The final
            // transition below needs no image or replacement-copy allocation.
            if(owner!=null&&shared==null)shared=new Shared1984(sharedBytes);
            QueueDecision1983 decision=new QueueDecision1983(QueueDecision1983.READY,0,0,jobs-count+1-(activeJob==null?0:1),
                activeJob==null?0:1,projected,requested);
            Reservation1984 reservation=!revocable?
                new Reservation1984(decision,key,environment,companion,bytes,owner,sharedBytes,priority):
                new Reservation1984(decision,key,environment,companion,bytes,owner,sharedBytes,priority,true);
            for(int i=0;i<count;i++){
                Job victim=removed[i];QUEUED.remove(victim);victim.cancelled=true;close(victim.probe);
                finishAttempt1984(victim.attempt1984,OUT_RETIRED1984);releaseLocked1984(victim.bytes,victim.sharedOwner1984);
            }
            if(owner!=null){Shared1984 current=SHARED1984.get(owner);
                if(current==null){shared.references=0;SHARED1984.put(owner,shared);retained+=shared.bytes;current=shared;}
                current.references++;
            }
            retained+=bytes;reservation.held=true;RESERVED1984.addLast(reservation);
            reservation.attempt=startAttempt1984(key,reservation.epoch,0);LOCK.notifyAll();return reservation;
        }}catch(RuntimeException unavailable){return RESERVATION_UNAVAILABLE1984;}
          catch(OutOfMemoryError unavailable){return RESERVATION_MEMORY1984;}
    }
    private static void startWorker1984(){
        if(worker==null||!worker.isAlive()){
            Thread made=new Thread(new Runnable(){public void run(){loop();}},"Hiro-ULike-GX-proof");
            made.setDaemon(true);made.setPriority(Thread.MIN_PRIORITY);made.start();worker=made;
        }
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
                    startWorker1984();
                    String environment=environment();Failure failure=failure(key,environment);
                    Failure secondary=companion==null?null:failure(companion,environment);
                    Job job=new Job(key,bytes,probe,environment,failure!=null,companion,secondary!=null&&!secondary.exact);
                    // Allocate the scalar result before ownership changes. An
                    // optional diagnostic allocation cannot strand a queued probe.
                    QueueDecision1983 scheduled=new QueueDecision1983(QueueDecision1983.SCHEDULED,0,0,
                        queuedCount1984()+1,activeJob==null?0:1,retained+bytes,bytes);
                    QUEUED.addLast(job);retained+=bytes;accepted=true;decision=scheduled;
                    job.attempt1984=startAttempt1984(key,job.epoch,1);LOCK.notifyAll();
                }
            }
        } catch(RuntimeException unavailable) {decision=UNAVAILABLE1983;}
          catch(OutOfMemoryError unavailable) {decision=MEMORY1983;}
        if(!accepted)close(probe);return decision;
    }
    // .84 telemetry is deliberately separate from RECORDS and FAILURES. Every
    // retained attempt contains scalars only; source keys, model owners and
    // driver identities stay in live ownership objects and never enter it.
    private static final int OUT_QUALIFIED1984=1,OUT_PARTIAL1984=2,OUT_UNRECORDED1984=3,
        OUT_CANCELLED1984=4,OUT_RETIRED1984=5,OUT_MEMORY1984=6,OUT_UNSUPPORTED1984=7,
        OUT_TRANSPORT1984=8,OUT_REFERENCE1984=9,OUT_BASELINE1984=10,OUT_SPEED1984=11,
        OUT_EXACT1984=12,OUT_CACHED1984=13,OUT_INCOMPLETE1984=14,OUT_EXCEPTION1984=15;
    private static final String[] PHASE_CODES1984={"reserved","queued","running","strong_cpu_reference",
        "strong_profile","strong_speed_compare","resident_baseline","resident_compare","resident_speed","finish_compare","finish_speed"};
    private static final String[] PHASE_LABELS1984={"コピー用予算を予約","検証実行待ち","検証開始","Strong CPU基準",
        "Strong候補の比較","Strong速度比較","連結の前提認定","連結の画質比較","連結の速度比較","仕上げの画質比較","仕上げの速度比較"};
    private static final String[] OUT_CODES1984={"pending","qualified","partial","no_result","cancelled","priority_retired",
        "memory_budget","unsupported","transport_failure","reference_unstable","baseline_unavailable","speed_condition",
        "quality_mismatch","quality_rejected","comparison_incomplete","exception"};
    private static final String[] OUT_LABELS1984={"未終了","認定記録を作成","一部の認定記録を作成・続きは未完了","認定未成立・終了理由の詳細なし",
        "撮影変更・中断で取消","優先検証の予算確保で取消","検証用メモリ不足","対応するGPU候補なし","GPU実行・取得を完了できず",
        "CPU基準の結果が不安定","前提となる認定が未成立","速度条件を満たさず","今回の比較が不一致","同じ条件の不一致記録あり",
        "比較完了前に終了","検証中の例外で終了"};
    private static final String[] FAMILY_LABELS1984={"補助","Strong","連結","仕上げ"};
    private static final String[] PROFILE_PREFIXES1985={"strong-gx1973-ieee-div-policy-bank-v1:",
        "strong-gx1964-parallel-policy-bank-v1:","strong-gx1971-generic-policy-bank-v1:","strong-gx1976-ieee-tile8-policy-bank-v1:",
        "strong-gx1978-policy-direct-ieee-v1:","strong-gx1978-policy-direct-tile8-v1:",
        "strong-gx1978-policy-direct-pow2-v1:","strong-gx1978-policy-direct-pow2-tile8-v1:"};
    private static final String[] SELECTION_CODES1985={"unchecked","available","none","invalidated","cancelled"};
    private static final String[] SELECTION_LABELS1985={"未確認","成立","不成立","失効","取消のため未確認"};
    private static final String[] REFERENCE_CODES1985={"unspecified","strong_cpu_parallel","strong_gpu_legacy","resident_chain_cpu","finish_cpu"};
    private static final String[] REFERENCE_LABELS1985={"検証CPU基準","並列CPU","既存GPU","連結CPU基準","仕上げCPU基準"};
    private static final String[] FINISH_REASONS1986={"確認中","画質比較が一致","今回の全画素比較が不一致",
        "実行・結果取得を完了できず","検証用メモリ不足","速度条件を満たさず","撮影変更・中断で取消",
        "比較条件が成立","保存済み画質不一致により対象外","別候補を選択","CPU基準の結果が不安定"};
    private static final String[] FINISH_CODES1986={"checking","exact","mismatch","execution_unavailable","memory_budget",
        "speed_condition","cancelled","conditions_met","exact_rejected","other_selected","reference_unstable"};
    private static final String[] FINISH_REFERENCES1986={"未選択","CPU","CPUと旧GPUの両方"};
    private static final String[] FINISH_CANDIDATES1986={"旧GPU仕上げ","新GPU仕上げ32行","新GPU仕上げ64行","新GPU仕上げ128行"};
    private static final String[] FINISH_PHASES1988={"未試行","CPU基準","全画素比較","実出力の速度計測"};
    private static final String[] FINISH_STAGES1988={"未開始","CPU入力コピー","CPU出力作成","CPU出力確認","CPU比較用メモリ",
        "CPU基準の安定性確認","入力・経路の前提確認","ジオメトリ準備","追加作業メモリ","入力画素の取得",
        "GPUセッション開始","GPU画像容量の予約","入力転送・初期GPU処理","ジオメトリ作業メモリ","ジオメトリ容量の予約",
        "ジオメトリGPU処理","残差解析","出力画像の確保","仕上げポリシー準備","仕上げバンク容量の予約",
        "GPUバンクへの送信","GPU結果の取得","全画素比較","出力画素の保存","完了","予約の再確認",
        "比較用バッファ確保","旧GPU仕上げ実行・結果取得"};
    private static final String[] FINISH_FAULTS1988={"なし","実行時例外","リンク例外","配列・画像確保失敗","取消"};
    private static final class Attempt1984 {
        final long sequence,epoch;final int family;
        int phase,completed,total,outcome,suggested,certificates,exactFailures,speedFailures;
        long cpuNanos,gpuNanos;boolean finished;
        int selection1985,selectionProfile1985= -1,selectionVariant1985= -1,reference1985,referenceWorkers1985;
        long referenceNanos1985,candidateNanos1985;boolean priorInvalidated1985;
        // Four bounded candidate observations. Packed fields and individual
        // scalars keep the retained ledger free of keys, images and arrays.
        int finishSeen1986,finishReasons1986,finishExact1986,finishSpeed1986,finishReferences1986;
        long finishCpu01986,finishLegacy01986,finishGpu01986,finishCpu11986,finishLegacy11986,finishGpu11986,
            finishCpu21986,finishLegacy21986,finishGpu21986,finishCpu31986,finishLegacy31986,finishGpu31986;
        // Four bounded observations only. No key, exception, image or owner is retained.
        int finishStages1988,finishPhases1988,finishFaults1988,finishActiveSlot1988= -1;
        int nonSpeedFailures1988,nonSpeedOutcome1988;
        Attempt1984(long sequence,long epoch,int family,int phase){this.sequence=sequence;this.epoch=epoch;this.family=family;this.phase=phase;}
    }
    private static final Attempt1984[] ATTEMPTS1984=new Attempt1984[16];
    private static int attemptCursor1984,attemptCount1984;private static long attemptSequence1984;
    private static volatile boolean traceLookedUp1984;private static volatile java.lang.reflect.Method traceEvent1984;
    private static int family1984(String key){
        if(legacyStrong1981(key))return 1;
        if(key!=null&&key.startsWith("strong-resident"))return 2;
        return priority1984(key)==1?3:0;
    }
    private static Attempt1984 startAttempt1984(String key,long epoch,int phase){
        try{
            Attempt1984 value=new Attempt1984(++attemptSequence1984,epoch,family1984(key),phase);
            ATTEMPTS1984[attemptCursor1984]=value;attemptCursor1984=(attemptCursor1984+1)%ATTEMPTS1984.length;
            if(attemptCount1984<ATTEMPTS1984.length)attemptCount1984++;emitAttempt1984(value);return value;
        }catch(Throwable optional){return null;}
    }
    private static void emitAttempt1984(Attempt1984 value){
        if(value==null)return;
        try{
            if(!traceLookedUp1984){
                try{traceEvent1984=Class.forName("com.hiro.ulike.CameraTrace1965").getMethod("event",String.class,long.class,String.class);}
                finally{traceLookedUp1984=true;}
            }
            java.lang.reflect.Method event=traceEvent1984;if(event==null)return;
            // CameraTrace bounds each fields value to 160 characters. Keep
            // independently readable scalar records within that existing cap.
            event.invoke(null,"gpu_proof85",Long.valueOf(value.epoch),"id="+value.sequence+" family="+value.family+
                " phase="+PHASE_CODES1984[value.phase]+" done="+value.completed+" total="+value.total+
                " end="+(value.outcome==OUT_QUALIFIED1984?"records_written":OUT_CODES1984[value.outcome])+" record_writes="+value.certificates+
                " exact="+value.exactFailures+" speed="+value.speedFailures);
            if(value.finished&&value.family==1)event.invoke(null,"gpu_proof85_selection",Long.valueOf(value.epoch),
                "id="+value.sequence+" selection_at_end="+SELECTION_CODES1985[value.selection1985]+" profile="+value.selectionProfile1985+
                " variant="+value.selectionVariant1985+" prior_invalidated="+value.priorInvalidated1985);
            if(value.reference1985>0)event.invoke(null,"gpu_proof85_compare",Long.valueOf(value.epoch),
                "id="+value.sequence+" reference="+REFERENCE_CODES1985[value.reference1985]+" workers="+value.referenceWorkers1985+
                " reference_ns="+value.referenceNanos1985+" candidate_ns="+value.candidateNanos1985);
            else if(value.cpuNanos>0&&value.gpuNanos>0)event.invoke(null,"gpu_proof85_measure",Long.valueOf(value.epoch),
                "id="+value.sequence+" verification_cpu_ns="+value.cpuNanos+" verification_gpu_ns="+value.gpuNanos);
        }catch(Throwable optional){}
    }
    private static void progressAttempt1984(Attempt1984 value,int phase,int completed,int total){
        try{if(value==null||value.finished)return;
            if(value.phase==phase&&value.completed==completed&&value.total==total)return;
            value.phase=phase;value.completed=completed;value.total=total;emitAttempt1984(value);
        }catch(Throwable optional){}
    }
    /** Unknown labels and malformed counts are ignored; telemetry cannot alter
     * retry policy, queue admission, cancellation, pixels or ownership. */
    public static void progress1984(String phase,int completed,int total){
        try{
            if(completed<0||total<0||completed>total||total>192)return;
            int selected=-1;for(int i=0;i<PHASE_CODES1984.length;i++)if(PHASE_CODES1984[i].equals(phase)){selected=i;break;}
            if(selected<0)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){if(activeJob==job)progressAttempt1984(job.attempt1984,selected,completed,total);}
        }catch(Throwable optional){}
    }
    public static void timings1984(long cpuNanos,long gpuNanos){
        try{if(cpuNanos<0||gpuNanos<0)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                value.cpuNanos=cpuNanos;value.gpuNanos=gpuNanos;emitAttempt1984(value);}
        }catch(Throwable optional){}
    }
    /** Actual comparison input, not photograph elapsed time. The old CPU
     * reference measurements remain separately available to older callers. */
    public static void comparison1985(String reference,int workers,long baselineNanos,long candidateNanos){
        try{
            int selected=-1;for(int i=1;i<REFERENCE_CODES1985.length;i++)if(REFERENCE_CODES1985[i].equals(reference)){selected=i;break;}
            if(selected<0||workers<0||workers>4||baselineNanos<=0||candidateNanos<=0)return;
            if(selected==1&&workers<1)return;
            Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                value.reference1985=selected;value.referenceWorkers1985=workers;
                value.referenceNanos1985=baselineNanos;value.candidateNanos1985=candidateNanos;emitAttempt1984(value);}
        }catch(Throwable optional){}
    }
    /** Observe an already selected route. This never writes a certificate or
     * authorizes a route. Keys live only on the active job until its final
     * cache-only recheck; the retained result contains primitive values only. */
    public static void selection1985(String childKey,int profile,int variant,boolean priorInvalidated){
        try{
            Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){
                if(activeJob!=job||job.attempt1984==null||job.attempt1984.finished||!tuningKey1975(job.key))return;
                if(childKey==null){if(profile!= -1||variant!= -1)return;}
                else if(profile<0||profile>=PROFILE_PREFIXES1985.length||variant<0||variant>2||
                        !tuningChild1975(job.key,childKey)||!childKey.startsWith(PROFILE_PREFIXES1985[profile]))return;
                job.selectionReported1985=true;job.selectionKey1985=childKey;
                job.selectionProfile1985=profile;job.selectionVariant1985=variant;
                boolean available=selectionPresent1985(job);
                job.selectionInvalidated1985|=priorInvalidated||job.selectionSeen1985&&!available;
                job.selectionSeen1985|=available;
            }
        }catch(Throwable optional){}
    }
    /** Iterate instead of get(): observing a terminal snapshot must not move
     * either access-ordered LRU, load preferences or retire any certificate. */
    private static Record peekRecord1985(String key,String environment){
        if(key==null)return null;String name=recordKey(key,environment);
        for(Map.Entry<String,Failure> entry:FAILURES.entrySet())if(name.equals(entry.getKey())&&entry.getValue().exact)return null;
        for(Map.Entry<String,Record> entry:RECORDS.entrySet())if(name.equals(entry.getKey()))return entry.getValue();
        return null;
    }
    private static boolean aggregatePresent1985(String key,String environment,int choice){
        Record value=peekRecord1985(key,environment);
        return value!=null&&value.variant==choice&&value.cpuNanos>0&&value.gpuNanos>0&&choice<=tuningChoiceLimit1976(key);
    }
    private static boolean selectionPresent1985(Job job){
        if(job.selectionKey1985==null||job.cancelled()||!job.environment.equals(environment()))return false;
        Record child=peekRecord1985(job.selectionKey1985,job.environment);
        if(child==null||child.variant!=job.selectionVariant1985||child.cpuNanos<=0||child.gpuNanos<=0)return false;
        int choice=job.selectionProfile1985*3+job.selectionVariant1985;
        if(aggregatePresent1985(job.key,job.environment,choice))return true;
        String suffix=job.key.substring(job.key.indexOf(':')+1);
        if(aggregatePresent1985(TUNING_PREFIX1976+suffix,job.environment,choice)||
                aggregatePresent1985(TUNING_PREFIX1975+suffix,job.environment,choice))return true;
        return job.selectionProfile1985>=4&&job.speedKey1978!=null&&cpuTuningChild1978(job.key,job.speedKey1978)&&
            aggregatePresent1985(job.speedKey1978,job.environment,choice);
    }
    private static void selectionAtEnd1985(Job job,int terminal){
        try{
            Attempt1984 value=job.attempt1984;if(value==null||value.finished)return;
            if(terminal==OUT_CANCELLED1984||job.cancelled()){value.selection1985=4;return;}
            if(!job.selectionReported1985)return;
            boolean available=selectionPresent1985(job);
            value.selection1985=available?1:job.selectionSeen1985||job.selectionInvalidated1985?3:2;
            value.priorInvalidated1985=job.selectionInvalidated1985||job.selectionSeen1985&&!available;
            if(available){value.selectionProfile1985=job.selectionProfile1985;value.selectionVariant1985=job.selectionVariant1985;}
        }catch(Throwable optional){
            if(job.attempt1984!=null){job.attempt1984.selection1985=0;job.attempt1984.selectionProfile1985= -1;job.attempt1984.selectionVariant1985= -1;}
        }finally{job.selectionKey1985=null;}
    }
    public static void outcome1984(String reason){
        try{
            if("strong_qualified".equals(reason))reason="qualified";
            if("strong_partial".equals(reason))reason="partial";
            if("cpu_speed_cooldown".equals(reason))reason="speed_condition";
            if("execution_unavailable".equals(reason))reason="transport_failure";
            int selected=-1;for(int i=1;i<OUT_CODES1984.length;i++)if(OUT_CODES1984[i].equals(reason)){selected=i;break;}
            if(selected<0)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob==job&&value!=null&&!value.finished)value.suggested=selected;}
        }catch(Throwable optional){}
    }
    private static void certificateAttempt1984(Job job,boolean exact,boolean speed){
        try{if(job==null||job.attempt1984==null)return;Attempt1984 value=job.attempt1984;if(value.finished)return;
            if(exact){if(value.exactFailures<65535)value.exactFailures++;}
            else if(speed){if(value.speedFailures<65535)value.speedFailures++;}
            else if(value.certificates<65535)value.certificates++;
            emitAttempt1984(value);
        }catch(Throwable optional){}
    }
    private static void finishAttempt1984(Attempt1984 value,int forced){
        try{if(value==null||value.finished)return;
            int outcome=forced!=0?forced:value.suggested;
            if(outcome==OUT_QUALIFIED1984&&value.certificates==0)outcome=OUT_UNRECORDED1984;
            if(outcome==OUT_PARTIAL1984&&value.certificates==0)outcome=OUT_INCOMPLETE1984;
            if(outcome==0)outcome=value.certificates>0?OUT_QUALIFIED1984:value.nonSpeedFailures1988>0?
                (value.speedFailures>0||value.exactFailures>0&&value.nonSpeedOutcome1988!=OUT_EXACT1984?
                    OUT_INCOMPLETE1984:value.nonSpeedOutcome1988):value.exactFailures>0?OUT_EXACT1984:
                value.speedFailures>0?OUT_SPEED1984:OUT_UNRECORDED1984;
            value.outcome=outcome;value.finished=true;emitAttempt1984(value);
        }catch(Throwable optional){}
    }
    public static String attemptSummary1984(){
        try{synchronized(LOCK){
            StringBuilder text=new StringBuilder("保存後の検証履歴（現在・最大16件）");
            if(attemptCount1984==0)return text.append(": まだ記録なし").toString();
            for(int n=0;n<attemptCount1984;n++){
                Attempt1984 value=ATTEMPTS1984[(attemptCursor1984-attemptCount1984+n+ATTEMPTS1984.length)%ATTEMPTS1984.length];
                if(value==null)continue;
                text.append('\n').append('#').append(value.sequence).append(' ').append(FAMILY_LABELS1984[value.family]).append(": ")
                    .append(value.finished?OUT_LABELS1984[value.outcome]:PHASE_LABELS1984[value.phase]);
                if(value.total>0)text.append(" / ").append(PHASE_LABELS1984[value.phase]).append(' ').append(value.completed).append('/').append(value.total);
                if(value.certificates>0)text.append(" / 認定記録 ").append(value.certificates).append("件");
                if(value.cpuNanos>0&&value.gpuNanos>0)text.append(" / 比較 CPU ").append(value.cpuNanos/1000000L)
                    .append("ms・GPU ").append(value.gpuNanos/1000000L).append("ms");
            }
            return text.append("\n※検証履歴は写真のGPU採用区間数・保存時間とは別の記録です。").toString();
        }}catch(Throwable optional){return "保存後の検証履歴: 読み出せませんでした";}
    }
    /** End-of-attempt facts stay fixed even when a later job replaces a proof.
     * Reading this text never looks up a live condition or selects a candidate. */
    public static String attemptSummary1985(){
        try{synchronized(LOCK){
            StringBuilder text=new StringBuilder("保存後の検証履歴（終了時点の記録・最大16件）");
            if(attemptCount1984==0)return text.append(": まだ記録なし").toString();
            for(int n=0;n<attemptCount1984;n++){
                Attempt1984 value=ATTEMPTS1984[(attemptCursor1984-attemptCount1984+n+ATTEMPTS1984.length)%ATTEMPTS1984.length];
                if(value==null)continue;
                text.append('\n').append('#').append(value.sequence).append(' ').append(FAMILY_LABELS1984[value.family]).append(": ");
                if(!value.finished)text.append("検証中");
                else if(value.outcome==OUT_QUALIFIED1984)text.append("認定書込あり・検証終了");
                else text.append(OUT_LABELS1984[value.outcome]);
                if(value.total>0){
                    text.append(" / ").append(value.phase==4?"候補確認":PHASE_LABELS1984[value.phase]).append(' ')
                        .append(value.completed).append('/').append(value.total);
                    if(value.phase==4&&value.completed==value.total)text.append("（確認一巡）");
                }
                text.append(" / 認定書込 ").append(value.certificates).append("回");
                if(value.family==1){
                    text.append(" / 終了時採用候補: ").append(value.finished?SELECTION_LABELS1985[value.selection1985]:"終了時に再確認");
                    if(value.selection1985==1)text.append("（候補 ").append(value.selectionProfile1985).append("・方式 ").append(value.selectionVariant1985).append('）');
                    if(value.priorInvalidated1985&&value.selection1985==1)text.append("・前候補の失効後に選び直し");
                }
                if(value.reference1985>0){
                    text.append(" / ").append(value.reference1985<=2?"Strong検証区間の直近比較: ":"候補工程の直近検証比較: ")
                        .append(REFERENCE_LABELS1985[value.reference1985]);
                    if(value.reference1985==1)text.append(' ').append(value.referenceWorkers1985).append("並列");
                    text.append(' ').append(value.referenceNanos1985/1000000L).append("ms → 候補GPU ")
                        .append(value.candidateNanos1985/1000000L).append("ms");
                }else if(value.cpuNanos>0&&value.gpuNanos>0)text.append(" / 検証用の計測: CPU基準 ")
                    .append(value.cpuNanos/1000000L).append("ms・候補GPU ").append(value.gpuNanos/1000000L).append("ms（比較対象の詳細未記録）");
            }
            return text.append("\n※確認一巡は全候補の合格数ではなく、認定書込は同じ記録の更新も含む延べ回数です。採用候補は各検証の終了時点で、現在のキャッシュ状態を保証しません。比較時間は検証対象の区間・工程であり、写真全体の処理時間や保存済みGPU採用区間数とは別です。").toString();
        }}catch(Throwable optional){return "保存後の検証履歴: 読み出せませんでした";}
    }
    /** Observe a finish dependency without replacing its enclosing resident
     * phase. candidate -1 is legacy, 0..2 are the 32/64/128-row candidates.
     * reference 1 requires CPU; 2 requires both CPU and legacy GPU timings.
     * reason 7 means measured conditions met, never a claimed certificate write.
     * Call only outside timed pixel loops, including before early returns. */
    public static void finishCandidate1986(int candidate,int reason,int exactTrials,int speedTrials,int reference,
            long cpuNanos,long legacyNanos,long candidateNanos){
        try{
            if(candidate< -1||candidate>2||reason<0||reason>=FINISH_CODES1986.length||exactTrials<0||exactTrials>2||
                    speedTrials<0||speedTrials>2||reference<0||reference>=FINISH_REFERENCES1986.length||
                    cpuNanos<0||legacyNanos<0||candidateNanos<0)return;
            if(reason==7&&(exactTrials!=2||speedTrials!=2||reference==0||cpuNanos<=0||candidateNanos<=0||
                    candidateNanos>cpuNanos-cpuNanos/20||reference==2&&(legacyNanos<=0||candidateNanos>legacyNanos-legacyNanos/20)))return;
            Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){
                Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                int slot=candidate+1,bit=1<<slot,wide=slot*4,narrow=slot*2;
                if((value.finishSeen1986&bit)!=0&&((value.finishReasons1986>>>wide)&15)==reason&&
                        ((value.finishExact1986>>>narrow)&3)==exactTrials&&((value.finishSpeed1986>>>narrow)&3)==speedTrials&&
                        ((value.finishReferences1986>>>narrow)&3)==reference&&finishTime1986(value,slot,0)==cpuNanos&&
                        finishTime1986(value,slot,1)==legacyNanos&&finishTime1986(value,slot,2)==candidateNanos)return;
                value.finishSeen1986|=bit;
                value.finishReasons1986=(value.finishReasons1986&~(15<<wide))|(reason<<wide);
                value.finishExact1986=(value.finishExact1986&~(3<<narrow))|(exactTrials<<narrow);
                value.finishSpeed1986=(value.finishSpeed1986&~(3<<narrow))|(speedTrials<<narrow);
                value.finishReferences1986=(value.finishReferences1986&~(3<<narrow))|(reference<<narrow);
                switch(slot){
                    case 0:value.finishCpu01986=cpuNanos;value.finishLegacy01986=legacyNanos;value.finishGpu01986=candidateNanos;break;
                    case 1:value.finishCpu11986=cpuNanos;value.finishLegacy11986=legacyNanos;value.finishGpu11986=candidateNanos;break;
                    case 2:value.finishCpu21986=cpuNanos;value.finishLegacy21986=legacyNanos;value.finishGpu21986=candidateNanos;break;
                    case 3:value.finishCpu31986=cpuNanos;value.finishLegacy31986=legacyNanos;value.finishGpu31986=candidateNanos;break;
                }
                emitFinish1986(value,candidate,reason,exactTrials,speedTrials,reference,cpuNanos,legacyNanos,candidateNanos);
            }
        }catch(Throwable optional){}
    }
    private static long finishTime1986(Attempt1984 value,int slot,int part){
        switch(slot){
            case 0:return part==0?value.finishCpu01986:part==1?value.finishLegacy01986:value.finishGpu01986;
            case 1:return part==0?value.finishCpu11986:part==1?value.finishLegacy11986:value.finishGpu11986;
            case 2:return part==0?value.finishCpu21986:part==1?value.finishLegacy21986:value.finishGpu21986;
            default:return part==0?value.finishCpu31986:part==1?value.finishLegacy31986:value.finishGpu31986;
        }
    }
    private static void emitFinish1986(Attempt1984 value,int candidate,int reason,int exactTrials,int speedTrials,int reference,
            long cpuNanos,long legacyNanos,long candidateNanos){
        try{
            java.lang.reflect.Method event=traceEvent1984;if(event==null)return;
            event.invoke(null,"gpu_finish86",Long.valueOf(value.epoch),"id="+value.sequence+" candidate="+candidate+
                " cause="+FINISH_CODES1986[reason]+" exact="+exactTrials+" speed="+speedTrials+" reference="+reference);
            if(cpuNanos>0||legacyNanos>0||candidateNanos>0)event.invoke(null,"gpu_finish86_times",Long.valueOf(value.epoch),
                "id="+value.sequence+" candidate="+candidate+" reference="+reference+
                " cpu_ns="+cpuNanos+" legacy_ns="+legacyNanos+" candidate_ns="+candidateNanos);
            int slot=candidate+1;
            event.invoke(null,"gpu_finish88_stage",Long.valueOf(value.epoch),"id="+value.sequence+" candidate="+candidate+
                " phase="+((value.finishPhases1988>>>(slot*2))&3)+" stage="+((value.finishStages1988>>>(slot*5))&31)+
                " fault="+((value.finishFaults1988>>>(slot*3))&7));
        }catch(Throwable optional){}
    }
    /** The familiar attempt ledger is unchanged. Detailed finish evidence is
     * limited to the latest observed dependency, so four candidates cannot
     * multiply every historical row into an unbounded view. */
    public static String attemptSummary1986(){
        try{synchronized(LOCK){
            String prior=attemptSummary1985();Attempt1984 latest=null;
            for(int n=0;n<attemptCount1984;n++){
                Attempt1984 value=ATTEMPTS1984[(attemptCursor1984-1-n+ATTEMPTS1984.length)%ATTEMPTS1984.length];
                if(value!=null&&value.finishSeen1986!=0){latest=value;break;}
            }
            if(latest==null)return prior;
            StringBuilder text=new StringBuilder(prior).append("\n\n仕上げ前提の直近検証 #").append(latest.sequence)
                .append(latest.finished?"（終了時の記録）":"（検証中・途中の記録）");
            for(int slot=0;slot<4;slot++)if((latest.finishSeen1986&(1<<slot))!=0){
                int reason=(latest.finishReasons1986>>>(slot*4))&15,reference=(latest.finishReferences1986>>>(slot*2))&3;
                text.append('\n').append(FINISH_CANDIDATES1986[slot]).append(": ").append(FINISH_REASONS1986[reason])
                    .append(" / 全画素一致 ").append((latest.finishExact1986>>>(slot*2))&3).append("/2")
                    .append(" / 速度計測 ").append((latest.finishSpeed1986>>>(slot*2))&3).append("/2")
                    .append(" / 比較基準: ").append(FINISH_REFERENCES1986[reference])
                    .append(" / 段階: ").append(FINISH_PHASES1988[(latest.finishPhases1988>>>(slot*2))&3])
                    .append(" / 最終工程: ").append(FINISH_STAGES1988[(latest.finishStages1988>>>(slot*5))&31]);
                int fault=(latest.finishFaults1988>>>(slot*3))&7;
                if(fault!=0)text.append(" / 例外区分: ").append(FINISH_FAULTS1988[fault]);
                long cpu=finishTime1986(latest,slot,0),legacy=finishTime1986(latest,slot,1),gpu=finishTime1986(latest,slot,2);
                if(cpu>0||legacy>0||gpu>0){
                    text.append(" / CPU ");finishMilliseconds1986(text,cpu);
                    text.append("・旧GPU ");finishMilliseconds1986(text,legacy);
                    text.append("・候補GPU ");finishMilliseconds1986(text,gpu);
                }
            }
            return text.append("\n※前提の候補別確認です。比較条件の成立と実際の認定書込は別です。時間はこの検証でのCPU最速・旧GPU最速・候補GPU最遅で、写真全体の処理時間ではありません。").toString();
        }}catch(Throwable optional){return attemptSummary1985();}
    }
    private static void finishMilliseconds1986(StringBuilder text,long nanos){
        if(nanos<=0)text.append("未取得");else if(nanos<1000000L)text.append("<1ms");else text.append(nanos/1000000L).append("ms");
    }
    private static Job nextJob1984(){
        Job selected=null;int rank=-1;
        for(Job job:QUEUED)if(job.priority1984>0){int value=rank1984(job.priority1984);if(value>rank){selected=job;rank=value;}}
        if(selected!=null)return selected;
        selected=QUEUED.peekFirst();for(Job job:QUEUED)if(legacyStrong1981(job.key)!=lastLegacy1981){selected=job;break;}
        return selected;
    }
    private static void loop() {
        for(;;) {
            Job job;
            synchronized(LOCK) {
                for(;;) {
                    job=nextJob1984();
                    // FIFO within each group; the first quiet opportunity goes
                    // to legacy Strong, then the other group gets its turn.
                    // The scalar turn survives cancellation; images do not.
                    if(job!=null&&(job.cancelled||job.epoch!=ProcessingTiming1947.captureEpoch1953()))break;
                    if(job!=null&&System.nanoTime()-lastCapture>=QUIET&&SaveQueue1935.idle1953()&&
                            !GpuNoise1960.sessionBusy()&&WholeRoute1953.retainedBytes()==0)break;
                    try{LOCK.wait(250);}catch(InterruptedException ignored){}
                }
                QUEUED.remove(job);activeJob=job;job.running=true;lastLegacy1981=legacyStrong1981(job.key);
                if(job.priority1984>0)lastPrimary1984=job.priority1984;
                progressAttempt1984(job.attempt1984,2,0,0);
            }
            int terminal=0;
            try {
                if(!job.cancelled()){CURRENT.set(job);job.probe.run(job);}
            } catch(java.util.concurrent.CancellationException cancelled){terminal=OUT_CANCELLED1984;}
              catch(RuntimeException unavailable){terminal=OUT_EXCEPTION1984;}
              catch(LinkageError unavailable){terminal=OUT_EXCEPTION1984;}
              catch(OutOfMemoryError unavailable){terminal=OUT_MEMORY1984;}
              catch(AssertionError unavailable){terminal=OUT_EXCEPTION1984;}
            finally {
                CURRENT.remove();close(job.probe);
                synchronized(LOCK){int ended=job.cancelled()?OUT_CANCELLED1984:terminal;
                    selectionAtEnd1985(job,ended);finishAttempt1984(job.attempt1984,ended);
                    if(activeJob==job)activeJob=null;releaseLocked1984(job.bytes,job.sharedOwner1984);LOCK.notifyAll();}
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
            return "CPU/GPU再判定（現在）: 予約済み"+queuedCount1984()+"件 / 実行中"+(activeJob==null?0:1)+"件\n"+
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
            certificateAttempt1984(job,false,false);
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
        rejectReason1988(key,exact,cause,exact?OUT_EXACT1984:OUT_SPEED1984);
    }
    /** Finish failures retain the existing nonexact retry policy without
     * inventing a measured speed failure or persisting a pixel rejection. */
    public static void rejectNonSpeed1988(String key,String reason) {
        int outcome="execution_unavailable".equals(reason)?OUT_TRANSPORT1984:
            "memory_budget".equals(reason)?OUT_MEMORY1984:"reference_unstable".equals(reason)?OUT_REFERENCE1984:
            "quality_mismatch".equals(reason)?OUT_EXACT1984:"quality_rejected".equals(reason)?OUT_CACHED1984:
            "unsupported".equals(reason)?OUT_UNSUPPORTED1984:"comparison_incomplete".equals(reason)?OUT_INCOMPLETE1984:0;
        if(outcome!=0)rejectReason1988(key,false,null,outcome);
    }
    static void rejectReason1988(String key,boolean exact,String cause,int outcome) {
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
                if(exact||outcome==OUT_SPEED1984)certificateAttempt1984(job,exact,!exact);
                else nonSpeedAttempt1988(job,outcome);
                try{if(edit!=null)edit.apply();}catch(RuntimeException unavailable){}
            }
        }
    }
    static void nonSpeedAttempt1988(Job job,int outcome){
        try{
            if(job==null||job.attempt1984==null)return;Attempt1984 value=job.attempt1984;if(value.finished)return;
            if(value.nonSpeedFailures1988<65535)value.nonSpeedFailures1988++;
            value.nonSpeedOutcome1988=value.nonSpeedOutcome1988==0||value.nonSpeedOutcome1988==outcome?outcome:OUT_INCOMPLETE1984;
            java.lang.reflect.Method event=traceEvent1984;
            if(event!=null)event.invoke(null,"gpu_proof88_failure",Long.valueOf(value.epoch),"id="+value.sequence+
                " non_speed="+value.nonSpeedFailures1988+" cause="+OUT_CODES1984[value.nonSpeedOutcome1988]);
        }catch(Throwable optional){}
    }
    /** CPU observations update only candidates which still need a trial.
     * Stage writes are scalar-only and never emit events inside timed loops. */
    public static void finishCpu1988(int stage,int mask){
        try{
            if(stage<1||stage>5||mask<0||mask>15)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                value.finishActiveSlot1988= -1;
                for(int slot=0;slot<4;slot++)if((mask&(1<<slot))!=0){
                    value.finishPhases1988=(value.finishPhases1988&~(3<<(slot*2)))|(1<<(slot*2));
                    value.finishStages1988=(value.finishStages1988&~(31<<(slot*5)))|(stage<<(slot*5));
                    value.finishFaults1988&=~(7<<(slot*3));
                }
            }
        }catch(Throwable optional){}
    }
    public static void finishBegin1988(int candidate,int phase){
        try{
            if(candidate< -1||candidate>2||phase<2||phase>3)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                int slot=candidate+1;value.finishActiveSlot1988=slot;
                value.finishPhases1988=(value.finishPhases1988&~(3<<(slot*2)))|(phase<<(slot*2));
                value.finishStages1988=(value.finishStages1988&~(31<<(slot*5)))|(6<<(slot*5));
                value.finishFaults1988&=~(7<<(slot*3));
            }
        }catch(Throwable optional){}
    }
    public static void finishStage1988(int stage){
        try{
            if(stage<0||stage>=FINISH_STAGES1988.length)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                int slot=value.finishActiveSlot1988;if(slot<0||slot>3)return;
                value.finishStages1988=(value.finishStages1988&~(31<<(slot*5)))|(stage<<(slot*5));
            }
        }catch(Throwable optional){}
    }
    public static void finishFault1988(int fault,int mask){
        try{
            if(fault<0||fault>=FINISH_FAULTS1988.length||mask<0||mask>15)return;Job job=CURRENT.get();if(job==null)return;
            synchronized(LOCK){Attempt1984 value=job.attempt1984;if(activeJob!=job||value==null||value.finished)return;
                if(mask==0){int slot=value.finishActiveSlot1988;if(slot<0||slot>3)return;mask=1<<slot;}
                for(int slot=0;slot<4;slot++)if((mask&(1<<slot))!=0)
                    value.finishFaults1988=(value.finishFaults1988&~(7<<(slot*3)))|(fault<<(slot*3));
            }
        }catch(Throwable optional){}
    }
    public static void finishEnd1988(){
        try{Job job=CURRENT.get();if(job==null)return;synchronized(LOCK){Attempt1984 value=job.attempt1984;
            if(activeJob==job&&value!=null&&!value.finished)value.finishActiveSlot1988= -1;}}
        catch(Throwable optional){}
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

