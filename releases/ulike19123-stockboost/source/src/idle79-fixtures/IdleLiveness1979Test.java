package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Execute actual asynchronous schedulers with owned scalar image fixtures.
 * No manually invoked Engine.background(), fabricated queue or physical GPU. */
public final class IdleLiveness1979Test {
    static final AtomicInteger assertions = new AtomicInteger();
    static final Map<String,Integer> sections = new LinkedHashMap<String,Integer>();
    static volatile boolean captureBusy;
    static int next = 1;
    interface Condition { boolean get() throws Exception; }
    static void check(boolean value,String why) {
        assertions.incrementAndGet();
        if(!value)throw new AssertionError(why);
    }
    static void waitFor(Condition condition,String why) throws Exception {
        long deadline=System.nanoTime()+3000000000L;
        while(!condition.get()) {
            if(System.nanoTime()>=deadline)throw new AssertionError("timeout: "+why);
            Thread.sleep(5);
        }
        check(true,why);
    }
    static void await(CountDownLatch latch,String why) {
        boolean interrupted=false;
        try {
            for(;;)try {
                if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("timeout: "+why);
                return;
            }catch(InterruptedException cancelled){interrupted=true;}
        }finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    static Field field(Class<?> type,String name) throws Exception {
        Field value=type.getDeclaredField(name);value.setAccessible(true);return value;
    }
    static boolean saveWorkersIdle() {
        int found=0;
        for(Thread thread:Thread.getAllStackTraces().keySet())if(thread.getName().startsWith("ULike-save1944-")) {
            found++;if(thread.getState()!=Thread.State.WAITING)return false;
        }
        return found>0;
    }
    static void nextCapture() throws Exception {
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        WholeRoute1953.foregroundStarted();
        waitFor(()->WholeRoute1953.retainedBytes()==0&&GpuQualification1961.retainedBytes()==0,"old owners fully released");
        captureBusy=false;GpuNoise1960.busy=false;
        check(SaveQueue1935.idle1953()&&SpeedWorkers1935.cpuIdle1944(),"real save and CPU workers drained");
    }
    static void elapsedQuietInterval() throws Exception {
        // Only elapsed time is controlled; actual production idle conditions,
        // queue ownership and asynchronous loop are evaluated unchanged.
        field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);
    }
    static void section(String name,int before){sections.put(name,assertions.get()-before);}
    static int value(int pixel){return Integer.rotateLeft(pixel^0x47a53c19,7)+0x163a05;}
    static final class Work implements WholeRoute1953.Work {
        final int[] key,input=new int[1025],output=new int[1025];
        final AtomicInteger cpuCalls=new AtomicInteger(),snapshots=new AtomicInteger();
        final boolean mismatch;
        volatile Probe proof;
        final CountDownLatch gpuEntered,gpuDrain;
        Work(int id,boolean mismatch,boolean blockGpu) {
            key=new int[]{1979,id,1025,1};this.mismatch=mismatch;
            gpuEntered=blockGpu?new CountDownLatch(1):null;gpuDrain=blockGpu?new CountDownLatch(1):null;
            for(int i=0;i<input.length;i++)input[i]=0xff010203+i*65537;
        }
        public long probeBytes(){return 16384;}
        public WholeRoute1953.Probe snapshotProbe(){snapshots.incrementAndGet();return proof=new Probe(this);}
        public boolean cpu(){cpuCalls.incrementAndGet();for(int i=0;i<input.length;i++)output[i]=value(input[i]);return true;}
        public boolean gpu(){throw new AssertionError("unqualified foreground must remain CPU");}
        public boolean equal(){throw new AssertionError("foreground cannot perform background proof");}
        public void publishGpu(){throw new AssertionError("background candidate cannot publish");}
        public void discardGpu(){ }
        void exact(){for(int i=0;i<input.length;i++)check(output[i]==value(input[i]),"foreground output unchanged at "+i);}
    }
    static final class Probe implements WholeRoute1953.Probe {
        final Work work;final int[] input;
        int[] reference,candidate;
        final AtomicInteger idleReads=new AtomicInteger(),gpuCalls=new AtomicInteger(),compared=new AtomicInteger(),discards=new AtomicInteger();
        volatile boolean discarded;
        Probe(Work work){this.work=work;input=work.input.clone();}
        public boolean captureReference(){reference=work.output.clone();return true;}
        public long bytes(){return work.probeBytes();}
        public boolean idle(){idleReads.incrementAndGet();return SaveQueue1935.idle1953()&&!captureBusy&&SpeedWorkers1935.cpuIdle1944();}
        public boolean gpu(WholeRoute1953.Cancellation cancellation) {
            check(!discarded&&reference!=null,"owned CPU reference survives until GPU run");
            gpuCalls.incrementAndGet();candidate=new int[input.length];
            for(int i=0;i<input.length;i++)candidate[i]=value(input[i]);
            if(work.mismatch)candidate[candidate.length-1]^=1;
            if(work.gpuEntered!=null){work.gpuEntered.countDown();await(work.gpuDrain,"controlled GPU drain");}
            return !cancellation.cancelled();
        }
        public boolean equal(WholeRoute1953.Cancellation cancellation) {
            for(int i=0;i<input.length;i++){
                if(cancellation.cancelled())return false;
                compared.incrementAndGet();if(reference[i]!=candidate[i])return false;
            }
            return true;
        }
        public void discard(){check(!discarded,"snapshot discarded exactly once");discards.incrementAndGet();candidate=null;reference=null;discarded=true;}
    }
    static final class Downstream implements GpuQualification1961.Probe {
        final String key;final AtomicInteger runs=new AtomicInteger(),closes=new AtomicInteger();
        Downstream(int id){key="idle-lifecycle1979:"+id;}
        public void run(GpuQualification1961.Cancellation cancellation) {
            check(!cancellation.cancelled(),"downstream job has live capture generation");
            check(WholeRoute1953.retainedBytes()==0,"downstream cannot overlap retained WholeRoute proof");
            check(SaveQueue1935.idle1953()&&!GpuNoise1960.sessionBusy(),"downstream obeys actual save/session idle");
            runs.incrementAndGet();GpuQualification1961.qualified(key,100,80,0);
        }
        public void close(){check(closes.incrementAndGet()==1,"downstream ownership closed once");}
    }
    static final class Save implements Runnable {
        final Object trace=new Object();final Work work;final Downstream downstream;
        final CountDownLatch prepared=new CountDownLatch(1),finish=new CountDownLatch(1);
        final int saveResult;volatile Throwable failure;
        Save(Work work,int saveResult){this.work=work;this.saveResult=saveResult;downstream=new Downstream(next++);}
        public void run(){
            try {
                check(WholeRoute1953.run(work.key,work,trace),"real foreground CPU completes once");
                check(GpuQualification1961.schedule(downstream.key,32,downstream),"real GX queue accepts owned downstream snapshot");
                prepared.countDown();await(finish,"save completion permission");
                SaveQueue1935.terminalTurn1956(this);
                WholeRoute1953.saved(saveResult==0?new Object():trace,saveResult>=0);
            }catch(Throwable problem){failure=problem;prepared.countDown();}
            finally{SaveQueue1935.release();}
        }
        void releaseSave() throws Exception {
            finish.countDown();waitFor(()->SaveQueue1935.idle1953(),"actual SaveQueue publication/preparation ownership ends");
            waitFor(()->saveWorkersIdle(),"final SaveQueue wake completed before dynamic transition");
            if(failure!=null)throw new AssertionError("save fixture failed",failure);
            check(work.cpuCalls.get()==1,"foreground CPU executes exactly once");
        }
    }
    static Save prepare(int id,int result,boolean bad,boolean blockGpu) throws Exception {
        Save job=new Save(new Work(id,bad,blockGpu),result);
        SaveQueue1935.reserve();SaveQueue1935.submit(job,1025,8200);
        await(job.prepared,"foreground and reference snapshot prepared");
        if(job.failure!=null)throw new AssertionError("preparation fixture failed",job.failure);
        check(WholeRoute1953.retainedBytes()==16384&&GpuQualification1961.retainedBytes()==32,"independent retained owners accounted");
        Thread.sleep(30);check(job.work.proof.gpuCalls.get()==0&&job.downstream.runs.get()==0,"unsaved photograph runs neither proof");
        return job;
    }
    static void finalWakeWhileBusy(Save job) throws Exception {
        int before=job.work.proof.idleReads.get();WholeRoute1953.wake();
        waitFor(()->job.work.proof.idleReads.get()>before,"last wake observes busy dynamic gate");
        Object engine=field(WholeRoute1953.class,"ENGINE").get(null);
        final Thread worker=(Thread)field(WholeRoute1953.Engine.class,"worker").get(engine);
        waitFor(()->worker.getState()==Thread.State.WAITING||worker.getState()==Thread.State.TIMED_WAITING,
                "real Engine is waiting after the last notification");
        check(job.work.proof.gpuCalls.get()==0&&job.downstream.runs.get()==0,"busy WholeRoute blocks itself and downstream safely");
    }
    static void finished(Save job,String reason) throws Exception {
        waitFor(()->WholeRoute1953.retainedBytes()==0,reason);
        waitFor(()->GpuQualification1961.retainedBytes()==0&&job.downstream.closes.get()==1,"downstream real queue drains after upstream release");
        check(job.work.proof.discards.get()==1&&job.downstream.runs.get()==1,"both independent owners complete exactly once");
        check(GpuQualification1961.restore(job.downstream.key)!=null,"actual GX service commits only completed downstream result");
        job.work.exact();
    }
    static void cpuTransition() throws Exception {
        int start=assertions.get();nextCapture();Save job=prepare(1,1,false,false);
        CountDownLatch cpuStarted=new CountDownLatch(1),cpuRelease=new CountDownLatch(1);
        Thread caller=new Thread(()->SpeedWorkers1935.run(new Runnable[]{()->{cpuStarted.countDown();await(cpuRelease,"CPU idle transition");}}),"idle79-real-CPU-group");
        caller.start();await(cpuStarted,"real CPU worker became busy");
        elapsedQuietInterval();job.releaseSave();finalWakeWhileBusy(job);
        cpuRelease.countDown();caller.join(3000);check(!caller.isAlive()&&SpeedWorkers1935.cpuIdle1944(),"real CPU group finishes without WholeRoute notification");
        finished(job,"idle_transition_without_notification: CPU completion must release retained WholeRoute");
        check(job.work.proof.gpuCalls.get()==1&&job.work.proof.compared.get()==1025,"every final pixel compared after CPU becomes idle");
        section("real_cpu_completion_without_notification",start);
    }
    static void captureTransitionAndSession() throws Exception {
        int start=assertions.get();nextCapture();captureBusy=true;Save job=prepare(2,1,false,false);
        elapsedQuietInterval();job.releaseSave();finalWakeWhileBusy(job);
        GpuNoise1960.busy=true;captureBusy=false;
        waitFor(()->WholeRoute1953.retainedBytes()==0,"metadata idle transition needs no further wake");
        Thread.sleep(300);check(job.downstream.runs.get()==0&&GpuQualification1961.retainedBytes()==32,"GPU session still blocks downstream after WholeRoute release");
        GpuNoise1960.busy=false;finished(job,"upstream remains drained");
        section("capture_idle_and_session_release_without_notification",start);
    }
    static void wrongAndFailedSave() throws Exception {
        int start=assertions.get();nextCapture();Save job=prepare(3,0,false,false);
        elapsedQuietInterval();job.releaseSave();WholeRoute1953.wake();Thread.sleep(600);
        check(job.work.proof.gpuCalls.get()==0&&job.downstream.runs.get()==0&&WholeRoute1953.retainedBytes()==16384,"periodic recheck never admits an unrelated save trace");
        WholeRoute1953.saved(job.trace,false);finished(job,"failed exact save releases upstream without executing GPU");
        check(job.work.proof.gpuCalls.get()==0&&job.work.proof.compared.get()==0,"failed save performs no GPU comparison");
        section("unrelated_and_failed_save_preserved",start);
    }
    static void queuedCancellation() throws Exception {
        int start=assertions.get();nextCapture();captureBusy=true;Save job=prepare(4,1,false,false);
        elapsedQuietInterval();job.releaseSave();finalWakeWhileBusy(job);
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;WholeRoute1953.foregroundStarted();
        check(WholeRoute1953.retainedBytes()==0&&job.work.proof.discards.get()==1,"new capture returns only after queued WholeRoute budget released");
        waitFor(()->GpuQualification1961.retainedBytes()==0,"queued downstream cancellation closes its owner");
        captureBusy=false;Thread.sleep(300);
        check(job.work.proof.gpuCalls.get()==0&&job.downstream.runs.get()==0&&job.downstream.closes.get()==1,"cancelled old generation never starts after dynamic idle opens");
        check(GpuQualification1961.restore(job.downstream.key)==null,"cancelled generation cannot publish certificate");job.work.exact();
        section("queued_cancel_before_idle_transition",start);
    }
    static void activeCancellation() throws Exception {
        int start=assertions.get();nextCapture();Save job=prepare(5,1,false,true);
        elapsedQuietInterval();job.releaseSave();await(job.work.gpuEntered,"actual background loop enters candidate");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;WholeRoute1953.foregroundStarted();
        check(WholeRoute1953.retainedBytes()==16384&&!job.work.proof.discarded,"active candidate retains images until real drain");
        check(job.downstream.closes.get()==1&&job.downstream.runs.get()==0,"downstream queued job cancelled independently");
        job.work.gpuDrain.countDown();waitFor(()->WholeRoute1953.retainedBytes()==0,"cancelled in-flight candidate drains before releasing source");
        check(job.work.proof.discards.get()==1&&job.work.proof.compared.get()==0,"cancelled candidate cannot count as exact proof");job.work.exact();
        Save retry=prepare(5,1,false,false);elapsedQuietInterval();retry.releaseSave();finished(retry,"cancel does not permanently reject valid shape");
        section("active_cancel_drain_and_retry",start);
    }
    static void exactRejection() throws Exception {
        int start=assertions.get();nextCapture();Save job=prepare(6,1,true,false);
        elapsedQuietInterval();job.releaseSave();finished(job,"mismatching candidate releases ownership safely");
        check(job.work.proof.compared.get()==1025,"last final pixel mismatch is observed");
        Work refused=new Work(6,false,false);check(WholeRoute1953.run(refused.key,refused,new Object()),"disproved route still completes original CPU");
        check(refused.cpuCalls.get()==1&&refused.snapshots.get()==0,"exact rejection remains authoritative after liveness repair");refused.exact();
        section("full_image_mismatch_keeps_CPU_authority",start);
    }
    static final class Memory implements SharedPreferences {
        final Map<String,String> values=new HashMap<String,String>();
        public synchronized String getString(String key,String fallback){String value=values.get(key);return value==null?fallback:value;}
        public synchronized Map<String,?> getAll(){return new HashMap<String,String>(values);}
        public Editor edit(){return new Editor(){boolean clear;final Map<String,String> edits=new HashMap<String,String>();
            public Editor clear(){clear=true;return this;}public Editor remove(String key){edits.put(key,null);return this;}
            public Editor putString(String key,String value){edits.put(key,value);return this;}
            public void apply(){synchronized(Memory.this){if(clear)values.clear();for(Map.Entry<String,String> e:edits.entrySet())if(e.getValue()==null)values.remove(e.getKey());else values.put(e.getKey(),e.getValue());}}
        };}
    }
    static final class App extends Context {
        final Memory memory=new Memory();public String getPackageName(){return "com.host.idle79";}
        public PackageManager getPackageManager(){return new PackageManager();}
        public SharedPreferences getSharedPreferences(String name,int mode){return memory;}
    }
    public static void main(String[] args) throws Exception {
        GpuQualification1961.initialize(new App());
        cpuTransition();captureTransitionAndSession();wrongAndFailedSave();queuedCancellation();activeCancellation();exactRejection();nextCapture();
        StringBuilder groups=new StringBuilder();for(Map.Entry<String,Integer> e:sections.entrySet()){
            if(groups.length()>0)groups.append(',');groups.append('"').append(e.getKey()).append("\":").append(e.getValue());
        }
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions.get()+",\"scenarios\":"+sections.size()+",\"sections\":{"+groups+"}}");
    }
}
