package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;

/** H21/H22/H27/H31: CPU output remains authoritative until background full-pixel
 * proof qualifies the complete GPU route and an exact, transfer-inclusive
 * dispatch choice. Production probes retain frozen input/reference leases
 * and release each private candidate after its complete operation drains. */
public final class WholeRoute1953 {
    private WholeRoute1953() { }
    private static final Engine ENGINE=new Engine(new Clock());
    static boolean run(int[] key,Work work,Object trace) { return ENGINE.run(key,work,trace); }
    public static void saved(Object trace,boolean success) { ENGINE.saved(trace,success); }
    public static void wake() { ENGINE.wake(); }
    public static void foregroundStarted() { ENGINE.foregroundStarted(); }
    public static long retainedBytes() { return ENGINE.retainedBytes; }
    public static void initialize(Context context) {
        if(context==null)return;
        try { ENGINE.history=new PreferencesHistory(context); }
        catch(RuntimeException unavailable) { }
        catch(LinkageError unavailable) { }
        catch(OutOfMemoryError unavailable) { }
    }

    interface Work extends WholeRoute1952.Work {
        /** Reserve all native snapshots, candidate and peak GPU workspace
         * before allocating anything. Root capture admission reads this budget. */
        default long probeBytes() { return 0; }
        /** Called before cpu(); snapshot failures must release partial copies. */
        default Probe snapshotProbe() { return null; }
        default boolean dispatchSelection() { return false; }
        default void setDispatchRows(int rows) { }
    }
    interface Cancellation { boolean cancelled(); }
    interface Probe {
        /** Called after the actual CPU stage; own its exact full reference. */
        boolean captureReference();
        long bytes();
        /** Return only after every submitted GPU operation has drained. The
         * timer includes fresh candidate creation, policy, transfer and wait. */
        boolean gpu(Cancellation cancellation);
        boolean equal(Cancellation cancellation);
        /** This queries capture + all preparation/encoding/codec ownership.
         * The engine always invokes it outside its own monitor. */
        boolean idle();
        default boolean timingReliable() { return true; }
        /** Only production probes opt into three full-route choices. */
        default boolean dispatchSelection() { return false; }
        default void setDispatchRows(int rows) { }
        /** Drain and release only the previous private candidate; retain input/reference. */
        default void discardCandidate() { }
        void discard();
    }
    static class Clock { long now() { return System.nanoTime(); } }
    interface History {
        /** Null/empty means no verified complete runtime fingerprint exists. */
        String environment();
        long restore(int[] key,String environment);
        void qualified(int[] key,String environment,long cpuBaseline);
        void rejected(int[] key,String environment);
        /** Legacy/custom providers cannot certify any dispatch selection. */
        default Qualification restoreQualified(int[] key,String environment) {
            long baseline=restore(key,environment);
            return baseline>0?new Qualification(baseline,-1):null;
        }
        default void qualified(int[] key,String environment,long cpuBaseline,int dispatchRows) {
            qualified(key,environment,cpuBaseline);
        }
    }
    /** -1 describes the scalar route only, never an admitted dispatch choice. */
    static final class Qualification {
        final long cpuBaseline;
        final int dispatchRows;
        Qualification(long cpuBaseline,int dispatchRows) {
            if(cpuBaseline<=0||(dispatchRows!=-1&&!validDispatch(dispatchRows)))
                throw new IllegalArgumentException("route qualification");
            this.cpuBaseline=cpuBaseline;this.dispatchRows=dispatchRows;
        }
        boolean dispatchVerified() { return validDispatch(dispatchRows); }
    }
    private static boolean validDispatch(int rows) { return rows==32||rows==64||rows==0; }
    private static final History NONE=new History() {
        public String environment() { return null; }
        public long restore(int[] key,String environment) { return 0; }
        public void qualified(int[] key,String environment,long cpuBaseline) { }
        public void rejected(int[] key,String environment) { }
    };

    static final class Engine {
        private static final int LIMIT=24,MAX_KEY=64;
        private final Clock clock;
        private final State[] states=new State[LIMIT];
        volatile History history;
        volatile long retainedBytes;
        private int victim;
        private long generation,notification;
        private Pending pending;
        private Thread worker;
        private volatile boolean unavailable;
        Engine(Clock clock) { this(clock,NONE); }
        Engine(Clock clock,History history) {
            if(clock==null)throw new NullPointerException("route clock");
            this.clock=clock;this.history=history==null?NONE:history;
        }
        boolean run(int[] key,Work work,Object trace) {
            if(work==null)throw new NullPointerException("route work");
            if(unavailable||key==null||key.length==0||key.length>MAX_KEY)return work.cpu();
            State state;
            try { state=claim(key.clone(),environment()); }
            catch(OutOfMemoryError noState) { return work.cpu(); }
            if(state==null)return work.cpu();
            if(state.admitted) {
                boolean selection;
                try { selection=work.dispatchSelection(); }
                catch(RuntimeException optionalFailure) { state.release();return work.cpu(); }
                catch(LinkageError optionalFailure) { state.release();return work.cpu(); }
                catch(OutOfMemoryError optionalFailure) { state.release();return work.cpu(); }
                if(!selection||state.dispatchVerified)return foreground(state,work);
                state.requireDispatchProof();
            }
            Pending reservation=null;
            try {
                long bytes=work.probeBytes();
                if(trace!=null&&bytes>0)synchronized(this) {
                    if(pending==null&&!unavailable) {
                        reservation=new Pending(state,trace,generation,bytes);
                        pending=reservation;retainedBytes=bytes;
                    }
                }
                if(reservation==null)return work.cpu();
                try {
                    Probe probe=work.snapshotProbe();
                    reservation.probe=probe;
                    if(probe==null||probe.bytes()<=0||probe.bytes()>reservation.bytes)reservation.cancelled=true;
                } catch(RuntimeException noSnapshot) { reservation.cancelled=true; }
                  catch(LinkageError noSnapshot) { reservation.cancelled=true; }
                  catch(OutOfMemoryError noSnapshot) { reservation.cancelled=true; }
                boolean before=reliable(work);
                long started=clock.now();
                boolean success=work.cpu();
                reservation.cpuNanos=clock.now()-started;
                reservation.cpuReliable=before&&reliable(work);
                if(!success)reservation.cancelled=true;
                if(success&&!reservation.cancelled) {
                    try {
                        reservation.reference=reservation.probe.captureReference();
                        if(!reservation.reference)reservation.cancelled=true;
                    }
                    catch(RuntimeException noReference) { reservation.cancelled=true; }
                    catch(LinkageError noReference) { reservation.cancelled=true; }
                    catch(OutOfMemoryError noReference) { reservation.cancelled=true; }
                }
                return success;
            } finally {
                if(reservation==null)state.release();
                else {
                    if(reservation.cpuNanos<=0)reservation.cpuReliable=false;
                    // Original CPU exceptions cannot leave a probe with a
                    // missing reference waiting forever for a successful save.
                    if(!reservation.referenceReady())reservation.cancelled=true;
                    synchronized(this) {
                        reservation.preparing=false;notification++;notifyAll();
                    }
                    startWorker();
                }
            }
        }
        private boolean foreground(State state,Work work) {
            boolean complete=false;
            try {
                long started=clock.now();boolean gpu=false;
                try { work.setDispatchRows(state.dispatchRows);gpu=work.gpu(); }
                catch(OutOfMemoryError failed) { reject(state); }
                catch(LinkageError failed) { unavailable=true;reject(state); }
                catch(RuntimeException failed) { reject(state); }
                long gpuNanos=clock.now()-started;
                if(gpu) {
                    if(reliable(work)&&state.slower(gpuNanos))reject(state);
                    try { work.publishGpu();complete=true;return true; }
                    catch(OutOfMemoryError failed) { reject(state); }
                    catch(LinkageError failed) { unavailable=true;reject(state); }
                    catch(RuntimeException failed) { reject(state); }
                } else reject(state);
                complete=work.cpu();return complete;
            } finally {
                if(!complete)reject(state);
                try { work.discardGpu(); }
                catch(OutOfMemoryError failed) { reject(state); }
                catch(LinkageError failed) { unavailable=true;reject(state); }
                catch(RuntimeException failed) { reject(state); }
                finally { state.release(); }
            }
        }
        void saved(Object trace,boolean success) {
            synchronized(this) {
                if(pending!=null&&pending.trace==trace) {
                    if(success)pending.saved=true;else pending.cancelled=true;
                    notification++;notifyAll();
                }
            }
        }
        void wake() { synchronized(this) { notification++;notifyAll(); } }
        void foregroundStarted() {
            Pending abandoned=null;
            synchronized(this) {
                generation++;
                if(pending!=null) {
                    pending.cancelled=true;
                    if(pending.running&&worker!=null)worker.interrupt();
                    else if(!pending.preparing&&!pending.releasing) {
                        // Readiness immediately consults the optional native
                        // budget. A queued proof must surrender it before this
                        // call returns, rather than wait for a low-priority
                        // thread and accidentally reject the user's shutter.
                        abandoned=pending;abandoned.releasing=true;
                    }
                }
                notification++;notifyAll();
            }
            if(abandoned!=null)discardOwned(abandoned);
        }
        private void startWorker() {
            synchronized(this) {
                if(worker!=null)return;
                try {
                    Thread made=new Thread(new Runnable() { public void run() { drain(); } },"ULike-proof1953");
                    made.setDaemon(true);made.setPriority(Thread.MIN_PRIORITY);
                    made.start();worker=made;return;
                } catch(RuntimeException noWorker) { }
                  catch(OutOfMemoryError noWorker) { }
            }
            // No GPU was submitted if worker creation failed. The foreground
            // CPU result remains valid; release this independent calibration.
            Pending abandoned;
            synchronized(this) { abandoned=pending;if(abandoned!=null)abandoned.cancelled=true; }
            if(abandoned!=null)release(abandoned);
        }
        private void drain() {
            for(;;) {
                Pending job;long observedNotification;
                synchronized(this) {
                    while(pending==null||pending.preparing||pending.releasing||(!pending.cancelled&&!pending.saved)) {
                        try { wait(); } catch(InterruptedException wake) { }
                    }
                    job=pending;observedNotification=notification;
                }
                Thread.interrupted();
                if(job.cancelled) { release(job);continue; }
                boolean idle=idle(job);
                synchronized(this) {
                    if(pending!=job||job.preparing||job.releasing||job.cancelled||job.generation!=generation)continue;
                    if(!idle) {
                        if(observedNotification==notification)try { wait(); }catch(InterruptedException wake) { }
                        continue;
                    }
                    job.running=true;
                }
                background(job);
            }
        }
        private void background(Pending job) {
            try {
                if(job.cancelled()||!idle(job))return;
                boolean selection=job.probe.dispatchSelection();
                long gpuNanos=Long.MAX_VALUE;int dispatchRows=32;
                boolean equal=false,reliable=job.cpuReliable;
                int provedDispatchMask=0,exactDispatchMask=0;
                if(selection) {
                    // Each timing includes candidate allocation, Java policy, owner
                    // queueing, transfer and final readback. No native-only shortcut.
                    int[] choices={32,64,0};
                    long baseline=Long.MAX_VALUE;boolean hasTimed=false;
                    for(int mode:choices) {
                        if(job.cancelled()||!idle(job))return;
                        job.probe.setDispatchRows(mode);
                        boolean completed=false,exact=false,valid=false;
                        long elapsed;
                        try {
                            long started=clock.now();completed=job.probe.gpu(job);
                            elapsed=clock.now()-started;
                            if(job.cancelled()||!idle(job))return;
                            exact=completed&&job.probe.equal(job);
                            if(job.cancelled()||!idle(job))return;
                            valid=job.probe.timingReliable();
                        } finally { job.probe.discardCandidate(); }
                        if(exact) {
                            equal=true;exactDispatchMask|=mode==32?1:mode==64?2:4;
                        }
                        if(exact&&valid&&elapsed>0) {
                            provedDispatchMask|=mode==32?1:mode==64?2:4;
                            if(mode==32)baseline=elapsed;
                            // Retain 32 unless batching saves at least five percent;
                            // between batched choices keep the shorter complete route.
                            if(!hasTimed||(elapsed<gpuNanos&&
                                    (dispatchRows!=32||elapsed<=baseline-baseline/20))) {
                                gpuNanos=elapsed;dispatchRows=mode;
                            }
                            hasTimed=true;
                        }
                    }
                    reliable=reliable&&hasTimed;
                } else {
                    long started=clock.now();boolean gpu=job.probe.gpu(job);
                    gpuNanos=clock.now()-started;
                    if(job.cancelled()||!idle(job))return;
                    if(!gpu) { reject(job.state);return; }
                    equal=job.probe.equal(job);
                    if(job.cancelled()||!idle(job))return;
                    reliable=reliable&&job.probe.timingReliable();
                }
                String now=environment();
                synchronized(this) {
                    // Capture can start between a pixel comparison and this
                    // commit. It must atomically cancel all state/history wins
                    // from the old generation before either is published.
                    if(pending!=job||job.cancelled||job.generation!=generation)return;
                    if(job.state.environment!=null&&!same(job.state.environment,now))return;
                    boolean restored=false;
                    if(job.state.environment==null&&now!=null) {
                        job.state.environment=now;
                        // The first real background dispatch may establish a
                        // previously unknown driver fingerprint. Restore only
                        // after this same photograph has a complete exact proof
                        // and a trustworthy fresh CPU baseline. Cold context
                        // creation cannot invalidate a verified steady-state
                        // history; the next foreground GPU call is timed again.
                        try {
                            Qualification prior=history.restoreQualified(job.state.key,now);
                            int storedMode=prior==null?-1:prior.dispatchRows;
                            int storedBit=storedMode==32?1:storedMode==64?2:storedMode==0?4:0;
                            if(prior!=null&&selection&&(storedBit==0||(exactDispatchMask&storedBit)==0)) {
                                // Correctness failure invalidates the old certificate
                                // even if CPU/GPU timings are unreliable or zero, or
                                // every other dispatch also failed pixel comparison.
                                // Timing reliability gates admission, never rejection
                                // of a mode disproved by this complete fresh probe.
                                history.rejected(job.state.key,now);prior=null;
                            }
                            if(equal&&reliable&&job.cpuNanos>0&&prior!=null
                                    &&(!selection||(provedDispatchMask&storedBit)!=0)) {
                                job.state.restore(new Qualification(job.cpuNanos,storedMode));restored=true;
                            }
                        }catch(RuntimeException failedHistory) { }
                         catch(LinkageError failedHistory) { }
                         catch(OutOfMemoryError failedHistory) { }
                    }
                    if(!restored&&selection&&equal&&reliable)job.state.selectedDispatch(dispatchRows);
                    if(!restored&&!job.state.probe(equal,gpuNanos,job.cpuNanos,reliable))reject(job.state);
                    else if(job.state.admitted&&now!=null) {
                        try { history.qualified(job.state.key,now,job.state.cpuBaseline,
                                job.state.dispatchVerified?job.state.dispatchRows:-1); }
                        catch(RuntimeException failedHistory) { }
                        catch(LinkageError failedHistory) { }
                        catch(OutOfMemoryError failedHistory) { }
                    }
                }
            } catch(OutOfMemoryError failed) { if(!job.cancelled())reject(job.state); }
              catch(LinkageError failed) { if(!job.cancelled()){unavailable=true;reject(job.state);} }
              catch(RuntimeException failed) { if(!job.cancelled())reject(job.state); }
            finally { release(job); }
        }
        private boolean idle(Pending job) {
            try { return job.probe!=null&&job.probe.idle(); }
            catch(RuntimeException failed) { job.cancelled=true;return false; }
            catch(LinkageError failed) { job.cancelled=true;return false; }
            catch(OutOfMemoryError failed) { job.cancelled=true;return false; }
        }
        private void release(Pending job) {
            synchronized(this) {
                if(pending!=job||job.releasing)return;
                job.releasing=true;
            }
            discardOwned(job);
        }
        private void discardOwned(Pending job) {
            try { if(job.probe!=null)job.probe.discard(); }
            catch(RuntimeException failed) { }
            catch(LinkageError failed) { }
            catch(OutOfMemoryError failed) { }
            finally { synchronized(this) {
                if(pending==job) { pending=null;retainedBytes=0;job.state.release();notification++;notifyAll(); }
            } }
        }
        private synchronized State claim(int[] key,String environment) {
            for(State state:states)if(state!=null&&Arrays.equals(state.key,key)&&same(state.environment,environment))
                return state.claim()?state:null;
            int slot=-1;
            for(int i=0;i<LIMIT;i++)if(states[i]==null) { slot=i;break; }
            if(slot<0)for(int i=0;i<LIMIT;i++) {
                int at=(victim+i)%LIMIT;if(!states[at].busy()){slot=at;victim=(at+1)%LIMIT;break;}
            }
            if(slot<0)return null;
            Qualification qualification=null;
            if(environment!=null)try { qualification=history.restoreQualified(key,environment); }
            catch(RuntimeException failedHistory) { }
            catch(LinkageError failedHistory) { }
            catch(OutOfMemoryError failedHistory) { }
            State state=new State(key,environment,qualification);state.claim();states[slot]=state;return state;
        }
        private String environment() {
            try { String value=history.environment();return value==null||value.length()==0?null:value; }
            catch(RuntimeException failed) { return null; }
            catch(LinkageError failed) { return null; }
            catch(OutOfMemoryError failed) { return null; }
        }
        private void reject(State state) {
            state.disable();
            if(state.environment!=null)try { history.rejected(state.key,state.environment); }
            catch(RuntimeException failedHistory) { }
            catch(LinkageError failedHistory) { }
            catch(OutOfMemoryError failedHistory) { }
        }
        private static boolean reliable(Work work) {
            try { return work.timingReliable(); }
            catch(RuntimeException failed) { return false; }
            catch(LinkageError failed) { return false; }
            catch(OutOfMemoryError failed) { return false; }
        }
    }
    private static boolean same(String a,String b) { return a==null?b==null:a.equals(b); }
    private static final class Pending implements Cancellation {
        final State state;final Object trace;final long generation,bytes;
        volatile Probe probe;
        volatile boolean preparing=true,cancelled,saved,running,reference,releasing;
        volatile boolean cpuReliable;
        long cpuNanos;
        Pending(State state,Object trace,long generation,long bytes) {
            this.state=state;this.trace=trace;this.generation=generation;this.bytes=bytes;
        }
        boolean referenceReady() { return reference; }
        public boolean cancelled() { return cancelled||Thread.currentThread().isInterrupted(); }
    }
    private static final class State {
        final int[] key;volatile String environment;
        private boolean inFlight,disabled;
        volatile boolean admitted,dispatchVerified;
        volatile int dispatchRows=32;
        private int probes,wins;
        long cpuBaseline;
        State(int[] key,String environment,Qualification restored) {
            this.key=key;this.environment=environment;
            restore(restored);
        }
        synchronized boolean claim() { if(disabled||inFlight)return false;inFlight=true;return true; }
        synchronized boolean busy() { return inFlight; }
        synchronized void release() { inFlight=false; }
        synchronized void disable() { disabled=true; }
        synchronized void restore(Qualification qualification) {
            if(disabled||qualification==null)return;
            admitted=true;cpuBaseline=qualification.cpuBaseline;probes=3;wins=2;
            dispatchVerified=qualification.dispatchVerified();
            dispatchRows=dispatchVerified?qualification.dispatchRows:32;
        }
        synchronized void requireDispatchProof() {
            admitted=false;probes=0;wins=0;dispatchVerified=false;dispatchRows=32;
        }
        synchronized void selectedDispatch(int rows) {
            if(rows!=32&&rows!=64&&rows!=0)throw new IllegalArgumentException("dispatch rows");
            if(dispatchVerified&&dispatchRows!=rows)wins=0;
            dispatchRows=rows;dispatchVerified=true;
        }
        synchronized boolean probe(boolean equal,long gpu,long cpu,boolean reliable) {
            if(disabled)return false;
            if(!equal)return false;
            if(probes++==0)return true;
            if(!reliable)return true;
            if(gpu<=0||cpu<=0||gpu>=cpu||gpu>cpu-cpu/20)return false;
            cpuBaseline=cpu;if(++wins>=2)admitted=true;return true;
        }
        synchronized boolean slower(long gpu) {
            return cpuBaseline>0&&gpu>=cpuBaseline
                &&gpu-cpuBaseline>=cpuBaseline/10+(cpuBaseline%10==0?0:1);
        }
    }

    /** H31 Android persistence: qualified CPU baseline and exact dispatch mode
     * are bound to one proof schema, full runtime fingerprint and route key.
     * No images, pixel masks, capture contents or binaries are serialized. */
    private static final class PreferencesHistory implements History {
        private final SharedPreferences preferences;
        private final String base;
        PreferencesHistory(Context context) {
            Context app=context.getApplicationContext();if(app==null)app=context;
            String packageName=app.getPackageName();
            String version;
            try {
                PackageInfo info=app.getPackageManager().getPackageInfo(packageName,0);
                long code=Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;
                version=info.versionName+":"+code;
            } catch(Exception unknownVersion) { throw new IllegalStateException("route app version unavailable",unknownVersion); }
            base=Build.FINGERPRINT+"|"+Build.VERSION.SDK_INT+"|"+Arrays.toString(Build.SUPPORTED_ABIS)
                +"|"+packageName+"|"+version+"|route1956-h31-proof-v1";
            preferences=app.getSharedPreferences("ulike_route1953",Context.MODE_PRIVATE);
        }
        public String environment() {
            String nativeEnvironment=GpuFinish1953.fingerprint();
            if(nativeEnvironment==null||nativeEnvironment.length()==0||Build.FINGERPRINT==null
                    ||Build.FINGERPRINT.length()==0||"unknown".equals(Build.FINGERPRINT))return null;
            return digest(base+"|"+nativeEnvironment);
        }
        private static final String SCHEMA="1956:1";
        private static final String PROOF="fullpixels-cold1-samemodewins2-margin5";
        public long restore(int[] key,String environment) {
            Qualification record=restoreQualified(key,environment);
            return record==null?0:record.cpuBaseline;
        }
        public Qualification restoreQualified(int[] key,String environment) {
            if(environment==null||!same(environment,environment())
                    ||!environment.equals(preferences.getString("environment","")))return null;
            String record=preferences.getString(recordKey(key,environment),"");
            String[] fields=record.split(":",-1);
            // The former CPU-baseline-only record lacks a dispatch proof and
            // is deliberately not migrated into the current proof schema.
            if(fields.length!=7||!"1956".equals(fields[0])||!"1".equals(fields[1])
                    ||!PROOF.equals(fields[5]))return null;
            try {
                long value=Long.parseLong(fields[2]),stamp=Long.parseLong(fields[3]);
                int rows=Integer.parseInt(fields[4]);
                if(value<=0||stamp<=0||(rows!=-1&&!validDispatch(rows)))return null;
                String core=SCHEMA+":"+value+":"+stamp+":"+rows+":"+PROOF;
                String check=digest(environment+"|"+Arrays.toString(key)+"|"+core);
                return check.equals(fields[6])?new Qualification(value,rows):null;
            }
            catch(NumberFormatException malformed) { return null; }
        }
        public void qualified(int[] key,String environment,long cpuBaseline) {
            qualified(key,environment,cpuBaseline,-1);
        }
        public void qualified(int[] key,String environment,long cpuBaseline,int dispatchRows) {
            if(environment==null||cpuBaseline<=0||(dispatchRows!=-1&&!validDispatch(dispatchRows))
                    ||!same(environment,environment()))return;
            SharedPreferences.Editor edit=preferences.edit();
            if(!environment.equals(preferences.getString("environment","")))edit.clear();
            String record=recordKey(key,environment);
            Map<String,?> stored=preferences.getAll();int count=0;
            String oldest=null;long oldestTime=Long.MAX_VALUE;
            for(Map.Entry<String,?> entry:stored.entrySet())if(entry.getKey().startsWith("route-")) {
                count++;
                if(entry.getKey().equals(record))continue;
                try {
                    String[] values=String.valueOf(entry.getValue()).split(":",-1);
                    long timestamp=values.length==7?Long.parseLong(values[3]):0;
                    if(timestamp<oldestTime){oldestTime=timestamp;oldest=entry.getKey();}
                }catch(RuntimeException malformed){oldest=entry.getKey();oldestTime=0;}
            }
            if(count>=24&&!stored.containsKey(record)&&oldest!=null)edit.remove(oldest);
            edit.putString("environment",environment);
            String core=SCHEMA+":"+cpuBaseline+":"+System.currentTimeMillis()+":"+dispatchRows+":"+PROOF;
            edit.putString(record,core+":"+digest(environment+"|"+Arrays.toString(key)+"|"+core));edit.apply();
        }
        public void rejected(int[] key,String environment) {
            if(environment!=null)preferences.edit().remove(recordKey(key,environment)).apply();
        }
        private static String recordKey(int[] key,String environment) { return "route-"+digest(environment+"|"+Arrays.toString(key)); }
        private static String digest(String value) {
            try {
                byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
                char[] out=new char[hash.length*2],hex="0123456789abcdef".toCharArray();
                for(int i=0;i<hash.length;i++){out[i*2]=hex[(hash[i]>>>4)&15];out[i*2+1]=hex[hash[i]&15];}
                return new String(out);
            }catch(Exception unavailable){throw new IllegalStateException("route fingerprint unavailable",unavailable);}
        }
    }
}
