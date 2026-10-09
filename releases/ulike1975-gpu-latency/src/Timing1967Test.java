package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Regressions against the production timing helper, not a reimplementation. */
public final class Timing1967Test {
    static int assertions;
    static final ArrayList<String> tests=new ArrayList<String>();
    interface Case {void run() throws Exception;}
    static void check(boolean condition,String reason) {
        assertions++;if(!condition)throw new AssertionError(reason);
    }
    static Field field(String name) throws Exception {
        Field result=ProcessingTiming1947.class.getDeclaredField(name);result.setAccessible(true);return result;
    }
    static void reset() throws Exception {
        ProcessingTiming1947.enter(null);
        for(String name:new String[]{"TRACES","KEYS","VIEWS"})((ArrayList<?>)field(name).get(null)).clear();
        field("latest").set(null,null);field("persisted").set(null,null);field("preferences").set(null,null);
        field("loaded").setBoolean(null,false);field("displayedId").setLong(null,0);
        field("nextId").setLong(null,1000);field("captureEpoch1953").setLong(null,0);
        GpuQualification1961.status="GPU資格: 未確認";GpuQualification1961.statusCalls=0;GpuQualification1961.wakeStatus=null;
    }
    static String render(ProcessingTiming1947.Trace trace) throws Exception {
        Method render=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);
        render.setAccessible(true);return (String)render.invoke(null,trace);
    }
    static void run(String name,Case test) throws Exception {reset();test.run();tests.add(name);System.out.println("PASS "+name);}

    static void workerIdentity() throws Exception {
        final Object original=new Object(),transformed=new Object(),unrelated=new Object();
        final ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(original);
        final ProcessingTiming1947.Trace wrong=ProcessingTiming1947.begin(unrelated);
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread worker=new Thread(new Runnable(){public void run(){try{
            ProcessingTiming1947.enter(wrong);
            check(ProcessingTiming1947.current()==wrong,"fixture has wrong current worker trace");
            check(ProcessingTiming1947.traceFor(original)==owner,"bitmap identity beats unrelated worker scope");
            ProcessingTiming1947.transfer(original,transformed);
            check(ProcessingTiming1947.traceFor(transformed)==owner,"transformation retains exact owner");
            ProcessingTiming1947.noiseBackend(owner,2,3);
        }catch(Throwable failure){error.set(failure);}}});
        worker.start();worker.join(10000);check(!worker.isAlive(),"worker completed");
        if(error.get()!=null)throw new AssertionError(error.get());
        check(owner.strongGpuStrips==2&&owner.strongCpuStrips==3,"explicit owner receives backend counters");
        check(wrong.strongGpuStrips==0&&wrong.strongCpuStrips==0,"unrelated current worker receives no counts");
        check(render(owner).contains("強ノイズ処理: GPU 2区間 / CPU 3区間"),"counts render accurately");
        check(!ProcessingTiming1947.summary().contains("強ノイズ処理:"),"older owner cannot replace newer displayed shot");
    }
    static void concurrentCounts() throws Exception {
        final ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        final int count=8,iterations=250;
        final CountDownLatch ready=new CountDownLatch(count),start=new CountDownLatch(1);
        final AtomicReference<Throwable> error=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[count];
        for(int i=0;i<count;i++){workers[i]=new Thread(new Runnable(){public void run(){try{
            ready.countDown();start.await();for(int n=0;n<iterations;n++)ProcessingTiming1947.noiseBackend(trace,1,2);
        }catch(Throwable failure){error.set(failure);}}});workers[i].start();}
        ready.await();start.countDown();for(Thread worker:workers){worker.join(10000);check(!worker.isAlive(),"count worker terminated");}
        if(error.get()!=null)throw new AssertionError(error.get());
        check(trace.strongGpuStrips==count*iterations,"concurrent GPU increments are never lost");
        check(trace.strongCpuStrips==2*count*iterations,"concurrent CPU increments are never lost");
        check(ProcessingTiming1947.summary().contains("強ノイズ処理: GPU 2000区間 / CPU 4000区間"),"summary contains completed concurrent counts");
    }
    static void invalidInputs() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(null,1,1);ProcessingTiming1947.noiseBackend(trace,-1,3);
        ProcessingTiming1947.noiseBackend(trace,3,-1);ProcessingTiming1947.noiseBackend(trace,0,0);
        check(trace.strongGpuStrips==0&&trace.strongCpuStrips==0,"invalid deltas make no partial update");
        check(!ProcessingTiming1947.summary().contains("強ノイズ処理:"),"absence of execution remains unlabeled");
        check(!ProcessingTiming1947.summary().contains("保存時の"),"qualification is not invented before completion");
    }
    static void terminalSnapshot() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,3,5);
        GpuQualification1961.status="GPU資格: 認定済み（現在）";GpuQualification1961.wakeStatus="GPU資格: 次の撮影で検査中";
        ProcessingTiming1947.finish(trace,true);
        String saved=ProcessingTiming1947.summary();long updated=trace.updatedMillis;
        check(trace.state==1,"successful shot becomes terminal");
        check(saved.contains("保存時のGPU資格: 認定済み"),"qualification snapshots before wake changes global status");
        check(!saved.contains("（現在）"),"saved qualification does not contradict its historical snapshot label");
        check(GpuQualification1961.status.equals("GPU資格: 次の撮影で検査中"),"fixture changed live qualification after save");
        ProcessingTiming1947.noiseBackend(trace,17,19);ProcessingTiming1947.finish(trace,false);
        ProcessingTiming1947.note(trace,"late mutation");ProcessingTiming1947.settings(trace,"late settings");
        ProcessingTiming1947.output(trace,99,99,"late output");
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,0,999000000L);
        check(ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE)==null,"terminal stage cannot restart");
        check(trace.strongGpuStrips==3&&trace.strongCpuStrips==5,"late worker backend counts ignored");
        check(GpuQualification1961.statusCalls==1,"repeat finish does not resnapshot qualification");
        check(trace.updatedMillis==updated,"late operations preserve saved timestamp");
        check(saved.equals(ProcessingTiming1947.summary()),"terminal summary remains byte stable");
    }
    static void failedTerminal() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,0,7);GpuQualification1961.status="GPU資格: 保留";
        ProcessingTiming1947.finish(trace,false);String saved=ProcessingTiming1947.summary();
        check(trace.state==2&&saved.contains(" / エラー"),"failed shot becomes terminal error");
        check(saved.contains("GPU 0区間 / CPU 7区間"),"CPU-only execution shown explicitly");
        GpuQualification1961.status="GPU資格: 後から認定済み";
        ProcessingTiming1947.noiseBackend(trace,4,4);ProcessingTiming1947.finish(trace,true);
        check(saved.equals(ProcessingTiming1947.summary()),"failed result cannot be rewritten by late success");
    }
    static final class MemoryPreferences implements SharedPreferences {
        final Map<String,Object> values=new HashMap<String,Object>();int saves;
        public synchronized long getLong(String key,long fallback){Object value=values.get(key);return value instanceof Long?(Long)value:fallback;}
        public synchronized String getString(String key,String fallback){Object value=values.get(key);return value instanceof String?(String)value:fallback;}
        public Editor edit(){return new Editor(){final Map<String,Object> staged=new HashMap<String,Object>();
            public Editor putString(String key,String value){staged.put(key,value);return this;}
            public Editor putLong(String key,long value){staged.put(key,value);return this;}
            public void apply(){synchronized(MemoryPreferences.this){values.putAll(staged);saves++;}}
        };}
    }
    static final class MemoryContext extends Context {
        final MemoryPreferences preferences;MemoryContext(MemoryPreferences value){preferences=value;}
        public SharedPreferences getSharedPreferences(String name,int mode){return preferences;}
    }
    static void persistence() throws Exception {
        MemoryPreferences preferences=new MemoryPreferences();ProcessingTiming1947.init(new MemoryContext(preferences));
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,9,2);GpuQualification1961.status="GPU資格: 撮影時の状態";
        ProcessingTiming1947.finish(trace,true);String saved=ProcessingTiming1947.summary();
        check(preferences.saves==1,"result saved exactly once");
        check(saved.equals(preferences.getString("summary","")),"stored summary matches terminal result");
        check(preferences.getString("version","").equals(ProcessingTiming1947.VERSION),"storage version tracks real helper");
        ProcessingTiming1947.noiseBackend(trace,90,20);ProcessingTiming1947.finish(trace,false);
        check(preferences.saves==1,"late operations never overwrite persisted result");
        reset();GpuQualification1961.status="GPU資格: 再起動後の別状態";
        ProcessingTiming1947.init(new MemoryContext(preferences));
        check(saved.equals(ProcessingTiming1947.summary()),"restart restores exact saved backend and qualification snapshot");
        check(GpuQualification1961.statusCalls==0,"reading saved result never uses live qualification");
    }
    static void unionDuration() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());long ms=1000000L;
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,0,10*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,5*ms,20*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,7*ms,9*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,30*ms,40*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.NOISE,40*ms,50*ms);
        ProcessingTiming1947.recordInterval(trace,ProcessingTiming1947.CORRECTION,0,60*ms);
        ProcessingTiming1947.noiseBackend(trace,1,2);
        check(ProcessingTiming1947.summary().contains("ノイズ除去: 40 ms"),"overlap/nesting union counts once and excludes 10ms gap");
        check(ProcessingTiming1947.summary().contains("補正: 60 ms"),"different stages retain separate timing scopes");
        check(!ProcessingTiming1947.summary().contains("ノイズ除去: 54 ms"),"not additive worker interval duration");
    }
    static void liveTokens() throws Exception {
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Token outer=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
        ProcessingTiming1947.Token nested=ProcessingTiming1947.beginStage(trace,ProcessingTiming1947.NOISE);
        check(ProcessingTiming1947.summary().contains("ノイズ除去: 処理中"),"live overlapping stages report active state");
        ProcessingTiming1947.end(nested);ProcessingTiming1947.end(nested);
        check(ProcessingTiming1947.summary().contains("ノイズ除去: 処理中"),"ending nested token twice cannot consume outer token");
        ProcessingTiming1947.end(outer);
        check(!ProcessingTiming1947.summary().contains("ノイズ除去: 処理中"),"drained tokens finalize actual duration");
        check(!ProcessingTiming1947.summary().contains("ノイズ除去: 未計測"),"ended work remains measured");
    }
    public static void main(String[] args) throws Exception {
        run("explicit_owner_ignores_unrelated_worker_scope",new Case(){public void run()throws Exception{workerIdentity();}});
        run("concurrent_backend_counts_are_exact",new Case(){public void run()throws Exception{concurrentCounts();}});
        run("invalid_or_missing_backend_evidence_remains_unknown",new Case(){public void run()throws Exception{invalidInputs();}});
        run("completed_snapshot_ignores_late_workers_and_live_qualification",new Case(){public void run()throws Exception{terminalSnapshot();}});
        run("failed_snapshot_cannot_be_rewritten",new Case(){public void run()throws Exception{failedTerminal();}});
        run("restart_preserves_saved_backend_and_qualification",new Case(){public void run()throws Exception{persistence();}});
        run("stage_wall_time_is_union_not_worker_sum_or_first_to_last",new Case(){public void run()throws Exception{unionDuration();}});
        run("nested_tokens_remain_idempotent",new Case(){public void run()throws Exception{liveTokens();}});
        StringBuilder names=new StringBuilder("[");for(String name:tests){if(names.length()>1)names.append(',');names.append('"').append(name).append('"');}names.append(']');
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"backend_diagnostics_regressions_passed\":true,\"physical_android_tested\":false,\"tests\":"+names+"}");
    }
}
