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
            key.startsWith("strong-gx1973-ieee-div-policy-bank-v1:3:")||key.startsWith("strong-gx1976-ieee-tile8-policy-bank-v1:3:")||tuningKey1975(key));
    }
    private static final String TUNING_PREFIX1975="strong-gx1975-tuning-policy-bank-v1:";
    private static final String TUNING_PREFIX1976="strong-gx1976-tuning-policy-bank-v1:";
    private static boolean tuningKey1975(String key){return key!=null&&(key.startsWith(TUNING_PREFIX1975+"3:")||key.startsWith(TUNING_PREFIX1976+"3:"));}
    private static int tuningChoiceLimit1976(String key){return key!=null&&key.startsWith(TUNING_PREFIX1976+"3:")?11:tuningKey1975(key)?8:2;}
    /** The aggregate stores only profile*3+workgroup and timings. Every child
     * certificate must describe exactly the same scalar geometry/quality key. */
    private static boolean tuningChild1975(String group,String key){
        if(!tuningKey1975(group)||key==null)return false;
        boolean expanded=group.startsWith(TUNING_PREFIX1976);
        String suffix=group.substring((expanded?TUNING_PREFIX1976:TUNING_PREFIX1975).length());
        return key.equals("strong-gx1973-ieee-div-policy-bank-v1:"+suffix)||
            key.equals("strong-gx1964-parallel-policy-bank-v1:"+suffix)||
            key.equals("strong-gx1971-generic-policy-bank-v1:"+suffix)||
            expanded&&key.equals("strong-gx1976-ieee-tile8-policy-bank-v1:"+suffix);
    }
    private static final LinkedHashMap<String,Failure> FAILURES=new LinkedHashMap<String,Failure>(LIMIT,.75f,true);
    private static final class Failure {boolean exact;int retries;long retryAfter;String cause="cached_legacy_negative";Failure(boolean exact){this.exact=exact;}}
    private static final LinkedHashMap<String,Record> RECORDS=new LinkedHashMap<String,Record>(LIMIT,.75f,true);
    private static final ThreadLocal<Job> CURRENT=new ThreadLocal<Job>();
    private static volatile SharedPreferences preferences;
    private static volatile String base="";
    private static final int MAX_JOBS=8;
    private static final ArrayDeque<Job> QUEUED=new ArrayDeque<Job>();
    private static Job activeJob;
    private static long retained;
    private static Thread worker;
    private static long lastCapture=System.nanoTime();
    private static final class Job implements Cancellation {
        final String key,environment;final long epoch,bytes;final Probe probe;final boolean speedRetry,fullStrong;
        volatile boolean cancelled,running;boolean failureCounted;
        Job(String key,long bytes,Probe probe,String environment,boolean retry){this.key=key;this.bytes=bytes;this.probe=probe;this.environment=environment;speedRetry=retry;fullStrong=fullStrongKey(key);epoch=ProcessingTiming1947.captureEpoch1953();}
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
        synchronized(LOCK){return canQueueLocked(key,bytes);}
    }
    private static boolean fullStrongKey(String key) {
        if(key==null||!key.startsWith("strong-"))return false;
        int mode=key.indexOf(':');return mode>=0&&key.startsWith(":3:",mode);
    }
    private static boolean canQueueLocked(String key,long bytes) {
        if(key==null||key.length()==0||bytes<=0||bytes>MAX_RETAINED||
                background()||Thread.currentThread().isInterrupted()||!maySchedule(key)||restore(key)!=null)return false;
        if(activeJob!=null&&key.equals(activeJob.key))return false;
        for(Job job:QUEUED)if(key.equals(job.key))return false;
        boolean strong=fullStrongKey(key);int jobs=QUEUED.size()+(activeJob==null?0:1),early=0;
        long expendable=0;
        for(Job job:QUEUED)if(!job.fullStrong){early++;expendable+=job.bytes;}
        if(activeJob!=null&&!activeJob.fullStrong)early++;
        // Keep one slot available for a late full Strong snapshot. Earlier
        // resident-model proofs may use the complete byte budget; they are not
        // permanently excluded merely because their maps exceed 32 MiB.
        if(!strong&&early>=MAX_JOBS-1)return false;
        if(retained>MAX_RETAINED-bytes||jobs>=MAX_JOBS){
            if(!strong||retained-expendable>MAX_RETAINED-bytes)return false;
            int evictable=0;for(Job job:QUEUED)if(!job.fullStrong)evictable++;
            if(jobs-evictable>=MAX_JOBS)return false;
            while(retained>MAX_RETAINED-bytes||QUEUED.size()+(activeJob==null?0:1)>=MAX_JOBS){
                Job retire=null;
                for(Job job:QUEUED)if(!job.fullStrong&&(retire==null||job.bytes>retire.bytes))retire=job;
                if(retire==null)return false;
                QUEUED.remove(retire);retire.cancelled=true;
                close(retire.probe); // Snapshot ownership ends before byte budget is released.
                retained-=retire.bytes;LOCK.notifyAll();
            }
        }
        return true;
    }
    /** Ownership of probe is transferred even if scheduling is declined. */
    public static boolean schedule(String key,long bytes,Probe probe) {
        if(probe==null)return false;
        boolean accepted=false;
        try {
            synchronized(LOCK) {
                if(canQueueLocked(key,bytes)) {
                    if(worker==null||!worker.isAlive()) {
                        Thread made=new Thread(new Runnable(){public void run(){loop();}},"Hiro-ULike-GX-proof");
                        made.setDaemon(true);made.setPriority(Thread.MIN_PRIORITY);made.start();worker=made;
                    }
                    String environment=environment();Failure failure=failure(key,environment);
                    Job job=new Job(key,bytes,probe,environment,failure!=null);
                    QUEUED.addLast(job);retained+=bytes;accepted=true;LOCK.notifyAll();
                }
            }
        } catch(RuntimeException unavailable) { } catch(OutOfMemoryError unavailable) { }
        if(!accepted)close(probe);return accepted;
    }
    private static void loop() {
        for(;;) {
            Job job;
            synchronized(LOCK) {
                for(;;) {
                    job=QUEUED.peekFirst();
                    // Preserve FIFO within full Strong keys, while prioritizing
                    // that expensive route ahead of earlier lightweight jobs.
                    for(Job queued:QUEUED)if(queued.fullStrong){job=queued;break;}
                    if(job!=null&&(job.cancelled||job.epoch!=ProcessingTiming1947.captureEpoch1953()))break;
                    if(job!=null&&System.nanoTime()-lastCapture>=QUIET&&SaveQueue1935.idle1953()&&
                            !GpuNoise1960.sessionBusy()&&WholeRoute1953.retainedBytes()==0)break;
                    try{LOCK.wait(250);}catch(InterruptedException ignored){}
                }
                QUEUED.remove(job);activeJob=job;job.running=true;
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
            int speed=0,exact=0;for(Failure failure:FAILURES.values())if(failure.exact)exact++;else speed++;
            return "CPU/GPU再判定（現在）: 待ち"+QUEUED.size()+"件 / 実行中"+(activeJob==null?0:1)+
                "件 / 採用済み（確認済み）"+RECORDS.size()+"件 / 速度条件待ち"+speed+"件 / 個別拒否（直近照会）"+exact+"件";
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
        if(!tuningKey1975(group))return;
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
        synchronized(LOCK) {
            String name=recordKey(key,environment);
            if(RECORDS.containsKey(name)&&!FAILURES.containsKey(name))return true;
            Failure failure=failure(key,environment);
            return failure==null||!failure.exact&&(strongPreferred1970(key)||
                failure.retries<MAX_SPEED_RETRIES&&System.nanoTime()-failure.retryAfter>=0);
        }
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
                    if(job!=null&&job.speedRetry&&job.key.equals(key)&&!job.failureCounted){failure.retries++;job.failureCounted=true;}
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

