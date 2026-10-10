package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class PipelineDetail1989Test {
    static final long MIB=1048576L,MS=1000000L;
    static int assertions;
    static final ArrayList<String> cases=new ArrayList<String>();
    interface Case {void run()throws Exception;}
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void run(String name,Case test)throws Exception{
        Timing1982Test.reset();PipelineProviders1988.reset();test.run();cases.add(name);System.out.println("PASS "+name);
    }
    static ProcessingTiming1947.Trace trace(){return ProcessingTiming1947.begin(new Object());}
    static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
    static int copyAt(int phase,int outcome){return (phase*11+outcome)*4;}
    static long[] state(ProcessingTiming1947.Trace t){
        long[][] arrays={t.pipelineWork1988,t.pipelineReasons1988,t.pipelineCopies1988,t.pipelinePriorReasons1989,
            t.pipelineCopyQuantities1989,t.pipelineQueueCounts1989,t.pipelineQueueLast1989};
        int size=1;for(long[] array:arrays)size+=array.length;
        long[] result=new long[size];int offset=0;
        for(long[] array:arrays){System.arraycopy(array,0,result,offset,array.length);offset+=array.length;}
        result[offset]=t.pipelineObserved1988?1:0;return result;
    }
    static void copy(ProcessingTiming1947.Trace t,int phase,int outcome,long requested,long acquired,
            int reason,long retry,int retries,int queued,int running,long retained){
        PipelineDetail1988.copy(t,phase,outcome,requested,MS);
        PipelineDetail1988.copyState1989(t,phase,outcome,requested,acquired,MS,reason,retry,retries,queued,running,retained);
    }
    static void reasons()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        String[] labels={"候補画質不一致の記録","CPU比較経路がGPUを見送り","経路枠が使用中","共用GPU枠を取得できず",
            "GPU環境識別なし","処理幅のGPU認定なし","入力条件対象外","処理中断を観測"};
        for(int reason=12;reason<20;reason++)PipelineDetail1988.record(t,1,0,reason,1,MS);
        for(int reason=12;reason<20;reason++){
            check(t.pipelineReasons1988[20+reason]==1,"new actual CPU reason is bounded and counted: "+reason);
            check(render(t).contains(labels[reason-12]+" 1回"),"each new CPU cause has its exact non-speculative label");
        }
        PipelineDetail1988.priorReason1989(t,1,PipelineDetail1988.STAGE_LEASE,3);
        check(t.pipelinePriorReasons1989[20+15]==3,"primary candidate handoff is retained separately");
        check(t.pipelineWork1988[(1*3+1)*3]==8&&t.pipelineWork1988[(1*3+2)*3]==0,"handoff never adds output adoption");
        check(render(t).contains("GPU候補の切替理由: 共用GPU枠を取得できず 3回"),"prior cause is not the final CPU decision");
        check(!render(t).contains("速度判定でCPU優先"),"combined CPU policy never invents a measured speed verdict");
        long[] before=state(t);PipelineDetail1988.priorReason1989(t,1,0,3);
        check(Arrays.equals(before,state(t)),"unknown prior cause is omitted without a fabricated decision");
    }
    static void quantities()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        copy(t,3,4,12*MIB,0,16,0,0,7,1,88*MIB);
        String text=render(t);
        check(text.contains("検証登録見送り 1回（1 ms、予約要求量 12.0 MiB、取得済み画素量 0.0 MiB）"),"preallocation refusal says requested12 and acquired0");
        check(!text.contains("記録量 12.0 MiB"),"typed quantity replaces ambiguous legacy bytes for the same event");
        check(t.pipelineCopies1988[copyAt(3,4)]==1&&t.pipelineCopyQuantities1989[copyAt(3,4)]==1,"new quantity sink never double-counts the old event");
        copy(t,3,2,12*MIB,4*MIB,-1,-1,-1,-1,-1,-1);
        text=render(t);
        check(text.contains("取得完了 1回（1 ms、予約要求量 12.0 MiB、取得済み画素量 4.0 MiB）"),"held closure and materialized pixel payload remain separate");
        check(text.contains("モデル・領域推定: 出力採用は未観測"),"successful capture is not GPU adoption");
        check(t.strongGpuStrips==0&&t.pipelineWork1988[(3*3+2)*3]==0,"copy diagnostics leave all adoption counters untouched");
        copy(t,3,7,12*MIB,2*MIB,-1,-1,-1,-1,-1,-1);
        check(render(t).contains("取得済み画素量 2.0 MiB"),"partial acquisition survives later allocation refusal");
    }
    static void queue()throws Exception{
        ProcessingTiming1947.Trace t=trace();int status=GpuQualification1961.statusCalls;
        copy(t,3,4,12*MIB,0,9,29*1000*MS,2,6,1,70*MIB);
        long[] captured=t.pipelineQueueLast1989.clone();
        copy(t,3,2,12*MIB,4*MIB,-1,-1,-1,-1,-1,-1);
        check(Arrays.equals(captured,t.pipelineQueueLast1989),"an event with no actual queue decision cannot invent a new one");
        String text=render(t);
        check(text.contains("画質不一致以外の失敗後の再試行間隔待ち 1回"),"actual queue refusal enum is retained");
        check(text.contains("再試行まで 29000 ms")&&text.contains("再試行数 2"),"retry metadata belongs to that exact admission");
        check(text.contains("予約 6件")&&text.contains("実行中 1件")&&text.contains("判断時保持量 70.0 MiB"),"queue and owner bytes use captured scalar data");
        for(int i=0;i<30;i++)render(t);
        check(GpuQualification1961.statusCalls==status,"rendering reads no live queue provider");
        copy(t,3,3,12*MIB,4*MIB,1,0,0,1,0,12*MIB);
        check(t.pipelineQueueCounts1989[3*22+9]==1&&t.pipelineQueueCounts1989[3*22+1]==1,"earlier refusal and later successful admission are distinct observations");
        check(t.pipelineQueueLast1989[30+1]==1&&t.pipelineQueueLast1989[30+8]==4*MIB,"last actual decision advances only on a new recorded decision");
    }
    static void bounds()throws Exception{
        ProcessingTiming1947.Trace t=trace();long[] before=state(t);
        for(int phase:new int[]{-1,11,Integer.MAX_VALUE}){
            ProcessingTiming1947.pipelinePriorReason1989(t,phase,1,1);
            ProcessingTiming1947.pipelineCopyState1989(t,phase,1,1,1,1,1,1,1,1,1,1);
        }
        for(int reason:new int[]{-1,0,20,Integer.MAX_VALUE})ProcessingTiming1947.pipelinePriorReason1989(t,1,reason,1);
        for(int reason:new int[]{-2,22,Integer.MAX_VALUE})ProcessingTiming1947.pipelineCopyState1989(t,1,1,1,1,1,reason,1,1,1,1,1);
        ProcessingTiming1947.pipelineCopyState1989(t,1,1,-2,1,1,1,1,1,1,1,1);
        ProcessingTiming1947.pipelineCopyState1989(t,1,1,1,-2,1,1,1,1,1,1,1);
        ProcessingTiming1947.pipelineCopyState1989(t,1,1,1,1,-2,1,1,1,1,1,1);
        ProcessingTiming1947.pipelineCopyState1989(t,1,1,1,1,1,1,-2,1,1,1,1);
        check(Arrays.equals(before,state(t)),"invalid fields are rejected atomically before any scalar update");
        int prior=20+1;t.pipelinePriorReasons1989[prior]=Long.MAX_VALUE-1;
        ProcessingTiming1947.pipelinePriorReason1989(t,1,1,Integer.MAX_VALUE);
        check(t.pipelinePriorReasons1989[prior]==Long.MAX_VALUE,"prior count saturates safely");
        int at=copyAt(3,4);t.pipelineCopyQuantities1989[at]=Long.MAX_VALUE-1;
        t.pipelineCopyQuantities1989[at+1]=Long.MAX_VALUE-1;t.pipelineCopyQuantities1989[at+2]=Long.MAX_VALUE-1;
        ProcessingTiming1947.pipelineCopyState1989(t,3,4,Long.MAX_VALUE,Long.MAX_VALUE,0,-1,-1,-1,-1,-1,-1);
        check(t.pipelineCopyQuantities1989[at]==Long.MAX_VALUE&&t.pipelineCopyQuantities1989[at+1]==Long.MAX_VALUE&&t.pipelineCopyQuantities1989[at+2]==Long.MAX_VALUE,"count and independent quantities saturate");
        ProcessingTiming1947.Trace unknown=trace();copy(unknown,3,4,-1,-1,-1,-1,-1,-1,-1,-1);
        check(render(unknown).contains("予約要求量 未観測、取得済み画素量 未観測"),"unknown is not represented as observed zero");
        check(unknown.pipelinePriorReasons1989.length==220&&unknown.pipelineCopyQuantities1989.length==484&&unknown.pipelineQueueCounts1989.length==242&&unknown.pipelineQueueLast1989.length==110,"all new retained arrays have fixed finite bounds");
    }
    static void isolation()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        for(int provider=0;provider<4;provider++){
            long[] before=state(t);PipelineProviders1988.mask=1<<provider;
            PipelineDetail1988.priorReason1989(t,1,2,1);copy(t,3,4,12*MIB,0,16,0,0,7,1,88*MIB);
            check(Arrays.equals(before,state(t)),"new observers exclude each real background provider");
            PipelineProviders1988.mask=0;
            for(int fault=1;fault<=4;fault++){
                PipelineProviders1988.failingProvider=provider;PipelineProviders1988.failure=fault;
                PipelineDetail1988.priorReason1989(t,1,2,1);copy(t,3,4,12*MIB,0,16,0,0,7,1,88*MIB);
                check(Arrays.equals(before,state(t)),"runtime/linkage/OOM/assertion diagnostic failure cannot mutate capture data");
            }
            PipelineProviders1988.reset();
        }
        ProcessingTiming1947.noiseBackend(t,11,5);
        for(String fieldName:new String[]{"pipelinePriorReasons1989","pipelineCopyQuantities1989","pipelineQueueCounts1989","pipelineQueueLast1989"}){
            Field field=ProcessingTiming1947.Trace.class.getDeclaredField(fieldName);field.setAccessible(true);Object saved=field.get(t);
            try{field.set(t,null);PipelineDetail1988.priorReason1989(t,1,2,1);copy(t,3,4,12*MIB,0,16,0,0,7,1,88*MIB);
                check(render(t).contains("GPU 11区間 / CPU 5区間"),"broken optional storage preserves the original processing summary");
            }finally{field.set(t,saved);}
        }
        for(boolean success:new boolean[]{true,false}){
            Timing1982Test.reset();Timing1982Test.MemoryPreferences prefs=new Timing1982Test.MemoryPreferences();
            ProcessingTiming1947.init(new Timing1982Test.MemoryContext(prefs));t=trace();
            PipelineDetail1988.priorReason1989(t,1,2,1);copy(t,3,4,12*MIB,0,16,0,0,7,1,88*MIB);
            ProcessingTiming1947.finish(t,success);long[] before=state(t);String text=render(t);
            PipelineDetail1988.priorReason1989(t,1,2,1);copy(t,3,4,12*MIB,4*MIB,1,0,0,0,0,12*MIB);
            ProcessingTiming1947.pipelinePriorReason1989(t,1,2,1);ProcessingTiming1947.pipelineCopyState1989(t,3,4,1,1,1,1,1,1,1,1,1);
            check(Arrays.equals(before,state(t))&&text.equals(render(t)),"both direct sinks and facade reject terminal updates");
            check(prefs.saves==1&&prefs.getString("summary","").equals(text),"new reason and capacity text persist at the existing terminal publish");
            Timing1982Test.reset();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(prefs));
            check(ProcessingTiming1947.summary().equals(text),"saved decision snapshot restores without current providers");
        }
    }
    static void identity()throws Exception{
        Object first=new Object(),second=new Object();ProcessingTiming1947.begin(first);
        check(ProcessingTiming1947.captureId1989(ProcessingTiming1947.captureEpoch1953())==0,"unassociated begin cannot borrow the previous shot");
        ProcessingTiming1947.Trace firstTrace=ProcessingTiming1947.associate(first,701);long firstEpoch=ProcessingTiming1947.captureEpoch1953();
        check(ProcessingTiming1947.captureId1989(firstEpoch)==firstTrace.id,"exact association binds the displayed trace ID to its epoch");
        ProcessingTiming1947.begin(second);check(ProcessingTiming1947.captureId1989(firstEpoch)==0,"overlapping new begin invalidates old fallback");
        final ProcessingTiming1947.Trace secondTrace=ProcessingTiming1947.associate(second,702);long secondEpoch=ProcessingTiming1947.captureEpoch1953();
        check(secondEpoch!=firstEpoch&&ProcessingTiming1947.captureId1989(firstEpoch)==0&&ProcessingTiming1947.captureId1989(secondEpoch)==secondTrace.id,"new epoch cannot relabel an old background owner");
        ProcessingTiming1947.associate(first,701);
        check(ProcessingTiming1947.captureId1989(secondEpoch)==secondTrace.id,"re-associating an older existing trace does not steal current epoch identity");
        Object lock=Timing1982Test.field("LOCK").get(null);final CountDownLatch done=new CountDownLatch(1);final long epoch=secondEpoch;
        final AtomicReference<Throwable> fault=new AtomicReference<Throwable>();
        Thread lookup=new Thread(()->{try{if(ProcessingTiming1947.captureId1989(epoch)!=secondTrace.id)throw new AssertionError("identity changed");}catch(Throwable e){fault.set(e);}finally{done.countDown();}});
        synchronized(lock){lookup.start();check(done.await(2,TimeUnit.SECONDS),"qualification holding its monitor never waits on timing monitor");}
        lookup.join(5000);if(fault.get()!=null)throw new AssertionError(fault.get());
        check(!lookup.isAlive()&&ProcessingTiming1947.captureId1989(-1)==0,"identity lookup terminates and unknown epoch remains unknown");
    }
    static void parallel()throws Exception{
        final Timing1982Test.MemoryPreferences prefs=new Timing1982Test.MemoryPreferences();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(prefs));
        final ProcessingTiming1947.Trace a=trace(),b=trace();final int loops=120,workers=4;
        final CountDownLatch start=new CountDownLatch(1);final AtomicReference<Throwable> fault=new AtomicReference<Throwable>();
        Thread[] threads=new Thread[workers];int status=GpuQualification1961.statusCalls;long updated=a.updatedMillis;
        for(int i=0;i<workers;i++){final ProcessingTiming1947.Trace owner=(i&1)==0?a:b;
            threads[i]=new Thread(()->{try{start.await();for(int n=0;n<loops;n++){
                PipelineDetail1988.priorReason1989(owner,1,15,1);copy(owner,3,4,12*MIB,0,16,0,0,7,1,88*MIB);
            }}catch(Throwable e){fault.compareAndSet(null,e);}});threads[i].start();}
        start.countDown();for(Thread thread:threads){thread.join(10000);check(!thread.isAlive(),"parallel observers terminate");}
        if(fault.get()!=null)throw new AssertionError(fault.get());
        for(ProcessingTiming1947.Trace owner:new ProcessingTiming1947.Trace[]{a,b}){
            check(owner.pipelinePriorReasons1989[20+15]==workers/2*loops,"prior reasons stay with each explicit owner");
            check(owner.pipelineCopyQuantities1989[copyAt(3,4)]==workers/2*loops,"copy detail count is atomic per owner");
            check(owner.pipelineCopyQuantities1989[copyAt(3,4)+1]==workers/2*loops*12*MIB,"requested bytes are exact under contention");
            check(owner.pipelineQueueCounts1989[3*22+16]==workers/2*loops,"queue snapshots are counted exactly per owner");
        }
        check(prefs.saves==0&&a.updatedMillis==updated&&GpuQualification1961.statusCalls==status,"hot observations neither publish nor query queue status");
    }
    public static void main(String[] args)throws Exception{
        run("expanded_actual_cpu_reasons_and_prior_handoff",()->reasons());
        run("requested_closure_and_acquired_pixels_are_distinct",()->quantities());
        run("queue_decisions_are_snapshots_not_requeries",()->queue());
        run("new_scalar_bounds_saturation_and_unknowns",()->bounds());
        run("new_observers_background_terminal_and_failure_isolation",()->isolation());
        run("capture_identity_epoch_overlap_and_no_reverse_lock",()->identity());
        run("parallel_owner_counts_and_existing_publication_only",()->parallel());
        StringBuilder names=new StringBuilder("[");for(String name:cases){if(names.length()>1)names.append(',');names.append('"').append(name).append('"');}names.append(']');
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"tests\":"+names+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
