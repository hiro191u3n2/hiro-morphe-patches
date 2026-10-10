package com.hiro.ulike;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class PipelineDetail1988Test {
    static final long MS=1000000L;
    static int assertions;
    static final ArrayList<String> cases=new ArrayList<String>();
    interface Case {void run()throws Exception;}
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void run(String name,Case work)throws Exception{
        Timing1982Test.reset();PipelineProviders1988.reset();work.run();cases.add(name);System.out.println("PASS "+name);
    }
    static ProcessingTiming1947.Trace trace(){return ProcessingTiming1947.begin(new Object());}
    static String render(ProcessingTiming1947.Trace owner)throws Exception{return Timing1982Test.render(owner);}
    static int at(int phase,int backend){return (phase*3+backend+1)*3;}
    static int copyAt(int phase,int outcome){return (phase*11+outcome)*4;}
    static long count(ProcessingTiming1947.Trace t,int phase,int backend){return t.pipelineWork1988[at(phase,backend)];}
    static long copies(ProcessingTiming1947.Trace t,int phase,int outcome){return t.pipelineCopies1988[copyAt(phase,outcome)];}
    static long[] state(ProcessingTiming1947.Trace t){
        long[] all=new long[t.pipelineWork1988.length+t.pipelineReasons1988.length+t.pipelineCopies1988.length+1];
        int offset=0;for(long[] values:new long[][]{t.pipelineWork1988,t.pipelineReasons1988,t.pipelineCopies1988}){
            System.arraycopy(values,0,all,offset,values.length);offset+=values.length;
        }all[offset]=t.pipelineObserved1988?1:0;return all;
    }
    static void record(ProcessingTiming1947.Trace t,int phase,int backend,int reason,int units,long nanos){PipelineDetail1988.record(t,phase,backend,reason,units,nanos);}
    static void explicitOwners()throws Exception{
        Object first=new Object(),second=new Object(),bound=new Object();
        ProcessingTiming1947.Trace a=ProcessingTiming1947.begin(first),b=ProcessingTiming1947.begin(second);
        check(PipelineDetail1988.current()==null,"latest photo cannot substitute for an explicit scope");
        check(PipelineDetail1988.owner(new Object())==null,"unknown identity without a scope remains unobserved");
        ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(b);
        try{
            check(PipelineDetail1988.current()==b,"current captures the entered owner");
            check(PipelineDetail1988.owner(first)==a,"bound identity takes priority over another entered scope");
            ProcessingTiming1947.transfer(first,bound);
            check(PipelineDetail1988.owner(bound)==a,"transformed input preserves its exact owner");
            record(a,PipelineDetail1988.MODEL_SOURCE,0,PipelineDetail1988.CPU_FIXED,1,4*MS);
            PipelineDetail1988.copy(a,PipelineDetail1988.AUX_SPATIAL,PipelineDetail1988.RESERVED,4096,-1);
            check(count(a,2,0)==1&&count(b,2,0)==0,"record does not recapture the worker's unrelated scope");
            check(copies(a,8,1)==1&&copies(b,8,1)==0,"snapshot observation follows its explicit owner");
        }finally{ProcessingTiming1947.restore(scope);}
        long[] before=state(a);record(null,0,0,0,1,1);PipelineDetail1988.copy(null,0,1,1,1);
        check(Arrays.equals(before,state(a)),"null owner cannot update any latest trace");
    }
    static void selectedAndUnknown()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        record(t,PipelineDetail1988.FRONT_STRIP,0,PipelineDetail1988.UNKNOWN,17,3303*MS);
        record(t,PipelineDetail1988.MODEL_MAPS,0,PipelineDetail1988.PROOF_MISSING,1,300*MS);
        record(t,PipelineDetail1988.MODEL_REGIONS,1,PipelineDetail1988.UNKNOWN,1,40*MS);
        record(t,PipelineDetail1988.AUX_SPATIAL,-1,PipelineDetail1988.UNKNOWN,1,-1);
        String text=render(t);
        check(text.contains("前段・画素処理: CPU 17回 3303 ms"),"selected work records accepted units and cumulative time");
        check(text.contains("CPU選択理由: 理由未観測 17回"),"unknown reason is explicit rather than an inferred GPU rejection");
        check(text.contains("モデル・領域推定: GPU 1回 40 ms"),"actual GPU adoption is independently reported");
        check(text.contains("補助・空間ノイズ解析: 経路未観測 1回 時間未測定"),"unobserved backend and elapsed remain unknown");
        check(!text.contains("補正・モアレ: "),"absence does not invent an executed or skipped correction phase");
        check(text.contains("記録のない処理は未観測"),"omitted phases are explained without a zero-execution claim");
        check(t.backendCounts1982[0]==0&&t.backendCounts1982[3]==0&&t.strongGpuStrips==0,"new observations do not alter historical adopted-output counters");
        for(int phase=0;phase<11;phase++)record(t,phase,0,0,1,0);
        check(render(t).contains("補正・モアレとくっきり一括: CPU 1回 0 ms"),"joined correction has its own phase and measured zero remains zero");
    }
    static void reasonsAndValidation()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        for(int reason=0;reason<12;reason++)record(t,0,0,reason,1,MS);
        for(int reason=0;reason<12;reason++)check(t.pipelineReasons1988[reason]==1,"exact bounded reason counter "+reason);
        String text=render(t);
        check(text.contains("検証の許可条件未成立（詳細未観測） 1回"),"maySchedule refusal does not masquerade as an exact mismatch or cooldown");
        check(!text.contains("画質不一致記録により不採用"),"generic reason does not fabricate a quality failure");
        long[] before=state(t);
        for(int phase:new int[]{-1,11,Integer.MAX_VALUE}){record(t,phase,0,0,1,1);PipelineDetail1988.copy(t,phase,1,1,1);}
        for(int backend:new int[]{-2,2,Integer.MIN_VALUE})record(t,0,backend,0,1,1);
        for(int reason:new int[]{-1,12,Integer.MAX_VALUE})record(t,0,0,reason,1,1);
        for(int units:new int[]{0,-1,Integer.MIN_VALUE})record(t,0,0,0,units,1);
        for(int outcome:new int[]{0,-1,11,Integer.MAX_VALUE})PipelineDetail1988.copy(t,0,outcome,1,1);
        record(t,0,0,0,1,-2);record(t,0,1,2,1,1);record(t,0,-1,2,1,1);
        PipelineDetail1988.copy(t,0,1,-2,1);PipelineDetail1988.copy(t,0,1,1,-2);
        check(Arrays.equals(before,state(t)),"invalid IDs, ambiguous GPU reasons and invalid scalars are ignored atomically");
    }
    static void snapshotEvents()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        for(int outcome=1;outcome<=10;outcome++)PipelineDetail1988.copy(t,8,outcome,1048576,outcome*MS);
        String text=render(t);
        for(int outcome=1;outcome<=10;outcome++)check(copies(t,8,outcome)==1,"bounded independent snapshot outcome "+outcome);
        check(text.contains("補助・空間ノイズ解析: 出力採用は未観測"),"captured or queued input never claims a committed backend");
        check(text.contains("取得完了 1回（2 ms、記録量 1.0 MiB）"),"captured input reports its measured quantity and work");
        check(text.contains("検証登録見送り 1回")&&text.contains("不要な補助準備を省略 1回"),"reservation refusal and .87 preparation avoidance remain distinct events");
        check(text.contains("同じ取得済み入力を再利用 1回"),"replay is not reported as a second capture");
        check(text.contains("各段階は同じ入力を数える場合があり"),"event stages are not presented as disjoint images");
        check(count(t,8,0)==0&&count(t,8,1)==0&&t.strongGpuStrips==0,"snapshot success has no output-adoption side effect");
    }
    static void elapsedAndBounds()throws Exception{
        ProcessingTiming1947.Trace t=trace();
        record(t,1,0,0,1,-1);record(t,1,0,0,1,0);
        PipelineDetail1988.copy(t,8,2,-1,-1);PipelineDetail1988.copy(t,8,2,0,0);
        String text=render(t);
        check(text.contains("CPU 2回 0 ms（一部未測定）"),"known zero and an untimed selection are distinguishable");
        check(text.contains("記録量 0.0 MiB・一部未観測"),"known zero bytes do not erase an unobserved quantity");
        int index=at(1,0);t.pipelineWork1988[index]=Long.MAX_VALUE-1;t.pipelineReasons1988[12]=Long.MAX_VALUE-1;
        record(t,1,0,0,Integer.MAX_VALUE,Long.MAX_VALUE);record(t,1,0,0,1,1);
        check(count(t,1,0)==Long.MAX_VALUE&&t.pipelineReasons1988[12]==Long.MAX_VALUE,"unit and reason totals saturate without wraparound");
        check(t.pipelineWork1988[index+1]==Long.MAX_VALUE,"work duration saturates without wraparound");
        int cp=copyAt(8,2);t.pipelineCopies1988[cp]=Long.MAX_VALUE;
        PipelineDetail1988.copy(t,8,2,Long.MAX_VALUE,Long.MAX_VALUE);PipelineDetail1988.copy(t,8,2,1,1);
        check(t.pipelineCopies1988[cp]==Long.MAX_VALUE&&t.pipelineCopies1988[cp+1]==Long.MAX_VALUE&&t.pipelineCopies1988[cp+2]==Long.MAX_VALUE,"copy count, duration and bytes all saturate");
        check(t.pipelineWork1988.length==99&&t.pipelineReasons1988.length==132&&t.pipelineCopies1988.length==484,"all per-owner primitive storage has a fixed finite bound");
        for(Field field:PipelineDetail1988.class.getDeclaredFields())check(Modifier.isStatic(field.getModifiers())&&Modifier.isFinal(field.getModifiers())&&field.getType()==int.class,"facade retains constants only: "+field.getName());
    }
    static void backgroundAndFaults()throws Exception{
        ProcessingTiming1947.Trace t=trace();ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(t);
        try{
            for(int provider=0;provider<4;provider++){
                long[] before=state(t);PipelineProviders1988.mask=1<<provider;
                record(t,0,0,0,1,1);PipelineDetail1988.copy(t,0,1,1,1);
                check(PipelineDetail1988.foreground(t)==null&&PipelineDetail1988.current()==null&&PipelineDetail1988.owner(new Object())==null,"background provider excludes captured/current owners: "+provider);
                check(Arrays.equals(before,state(t)),"background work cannot change the foreground trace: "+provider);
                PipelineProviders1988.mask=0;
                for(int failure=1;failure<=4;failure++){
                    PipelineProviders1988.failingProvider=provider;PipelineProviders1988.failure=failure;
                    int calls=PipelineProviders1988.calls;
                    record(t,0,0,0,1,1);PipelineDetail1988.copy(t,0,1,1,1);
                    check(PipelineDetail1988.current()==null&&PipelineDetail1988.foreground(t)==null&&PipelineDetail1988.owner(new Object())==null,"broken provider fails closed without propagation: "+provider+":"+failure);
                    check(PipelineProviders1988.calls>calls&&Arrays.equals(before,state(t)),"failure injection reaches actual optional provider without recording: "+provider+":"+failure);
                }
                PipelineProviders1988.failingProvider=-1;PipelineProviders1988.failure=0;
            }
            record(t,2,0,1,1,MS);check(count(t,2,0)==1,"foreground recording resumes after optional provider recovery");
        }finally{ProcessingTiming1947.restore(scope);PipelineProviders1988.reset();}
    }
    static void malformedOptionalStorage()throws Exception{
        ProcessingTiming1947.Trace t=trace();ProcessingTiming1947.noiseBackend(t,11,5);
        record(t,1,0,0,1,MS);
        for(String name:new String[]{"pipelineWork1988","pipelineCopies1988"}){
            Field field=ProcessingTiming1947.Trace.class.getDeclaredField(name);field.setAccessible(true);Object saved=field.get(t);
            try{
                field.set(t,null);record(t,1,0,0,1,MS);PipelineDetail1988.copy(t,1,2,1024,MS);
                check(render(t).contains("GPU 11区間 / CPU 5区間"),"broken optional storage preserves historical summary: "+name);
            }finally{field.set(t,saved);}
        }
        record(t,2,0,1,1,MS);check(count(t,2,0)==1,"a damaged optional observation does not poison later valid observations");
    }
    static void concurrentOwners()throws Exception{
        final ProcessingTiming1947.Trace a=trace(),b=trace();final int workers=8,loops=400;
        final CountDownLatch start=new CountDownLatch(1);final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] threads=new Thread[workers];
        for(int i=0;i<workers;i++){final int id=i;threads[i]=new Thread(new Runnable(){public void run(){
            ProcessingTiming1947.Trace owner=(id&1)==0?a:b,other=owner==a?b:a;ProcessingTiming1947.Scope scope=ProcessingTiming1947.enter(other);
            try{start.await();for(int n=0;n<loops;n++){
                record(owner,PipelineDetail1988.MODEL_REGIONS,0,PipelineDetail1988.PROOF_MISSING,1,MS);
                PipelineDetail1988.copy(owner,PipelineDetail1988.MODEL_REGIONS,PipelineDetail1988.QUEUE_DECLINED,4096,2*MS);
            }}catch(Throwable ex){failure.compareAndSet(null,ex);}finally{ProcessingTiming1947.restore(scope);}
        }});threads[i].start();}
        start.countDown();for(Thread thread:threads){thread.join(10000);check(!thread.isAlive(),"parallel diagnostics terminate without deadlock");}
        if(failure.get()!=null)throw new AssertionError(failure.get());
        for(ProcessingTiming1947.Trace owner:new ProcessingTiming1947.Trace[]{a,b}){
            check(count(owner,3,0)==workers/2*loops,"parallel counts belong to the explicit owner");
            check(owner.pipelineWork1988[at(3,0)+1]==workers/2*loops*MS,"parallel work sum is exact");
            check(owner.pipelineReasons1988[3*12+2]==workers/2*loops,"parallel reason counts are exact");
            check(copies(owner,3,4)==workers/2*loops&&owner.pipelineCopies1988[copyAt(3,4)+2]==workers/2*loops*4096L,"parallel snapshot events preserve counts and bytes");
        }
    }
    static void terminalAndPersistence()throws Exception{
        for(boolean success:new boolean[]{true,false}){
            Timing1982Test.reset();Timing1982Test.MemoryPreferences prefs=new Timing1982Test.MemoryPreferences();
            ProcessingTiming1947.init(new Timing1982Test.MemoryContext(prefs));ProcessingTiming1947.Trace t=trace();
            record(t,4,0,2,1,727*MS);PipelineDetail1988.copy(t,8,3,1048576,2*MS);ProcessingTiming1947.finish(t,success);
            check(t.state==(success?1:2),"real terminal success/failure state is recorded");
            String text=render(t);long[] before=state(t);long updated=t.updatedMillis;
            for(int i=0;i<100;i++){record(t,5,1,0,1,10*MS);PipelineDetail1988.copy(t,8,2,2048,10*MS);}
            ProcessingTiming1947.pipelineDetail1988(t,1,0,0,1,1);ProcessingTiming1947.pipelineCopy1988(t,1,1,1,1);
            check(Arrays.equals(before,state(t))&&text.equals(render(t))&&t.updatedMillis==updated,"both facade and sink reject all terminal updates");
            check(prefs.saves==1&&prefs.getString("summary","").equals(text),"completed scalar snapshot persists exactly once");
            Timing1982Test.reset();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(prefs));
            check(ProcessingTiming1947.summary().equals(text),"new detail survives storage restore without live recapture");
            check(GpuQualification1961.statusCalls==0,"restoring stored details performs no live provider lookup");
        }
        Timing1982Test.reset();Timing1982Test.MemoryPreferences old=new Timing1982Test.MemoryPreferences();
        old.values.put("version","1.9.87");old.values.put("summary","old details");old.values.put("id",1L);
        ProcessingTiming1947.init(new Timing1982Test.MemoryContext(old));
        check(ProcessingTiming1947.summary().startsWith("ULike v1.9.88 / 新しい撮影の計測待ち"),"old-version stored data cannot claim new instrumentation");
    }
    static void terminalRace()throws Exception{
        final ProcessingTiming1947.Trace t=trace();final CountDownLatch entered=new CountDownLatch(1),late=new CountDownLatch(1);
        Thread thread=new Thread(new Runnable(){public void run(){record(t,1,0,0,1,MS);entered.countDown();try{late.await();}catch(InterruptedException e){throw new AssertionError(e);}record(t,1,0,0,1,MS);PipelineDetail1988.copy(t,1,2,1024,MS);}});
        thread.start();entered.await();ProcessingTiming1947.finish(t,true);long[] before=state(t);late.countDown();thread.join(10000);
        check(!thread.isAlive()&&count(t,1,0)==1&&Arrays.equals(before,state(t)),"late worker cannot modify an already published owner");
    }
    static void noHotPublication()throws Exception{
        final Timing1982Test.CountingGroup1982 group=new Timing1982Test.CountingGroup1982();final Timing1982Test.MemoryPreferences prefs=new Timing1982Test.MemoryPreferences();
        android.preference.PreferenceActivity activity=new android.preference.PreferenceActivity(){
            public android.preference.PreferenceGroup getPreferenceScreen(){return group;}
            public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
        };
        ProcessingTiming1947.install(activity);ProcessingTiming1947.Trace t=trace();int summaries=group.summaries,status=GpuQualification1961.statusCalls;
        t.updatedMillis=123;Timing1982Test.field("lastViewRefresh").setLong(null,0);
        for(int i=0;i<100;i++){record(t,1,0,0,1,MS);PipelineDetail1988.copy(t,8,4,-1,-1);}
        check(group.summaries==summaries&&prefs.saves==0&&t.updatedMillis==123,"operation recording allocates no UI work and performs no publication or storage write");
        check(GpuQualification1961.statusCalls==status,"operation recording never asks the public qualification summary provider");
        ProcessingTiming1947.finish(t,true);
        check(group.summaries>summaries&&prefs.saves==1&&prefs.getString("summary","").contains("CPU 100回 100 ms"),"the existing terminal publish delivers every deferred observation");
    }
    static void workWallMeaning()throws Exception{
        ProcessingTiming1947.Trace t=trace();ProcessingTiming1947.detailWork1976(t,2,712*MS);
        record(t,4,0,0,5,1141*MS);record(t,4,1,0,11,827*MS);PipelineDetail1988.copy(t,4,2,100,20*MS);
        String text=render(t);
        check(text.contains("強ノイズ 712 ms")&&text.contains("CPU 5回 1141 ms / GPU 11回 827 ms"),"work sums larger than wall remain separate quantities");
        check(text.contains("工程時間は実経過、ここは作業累計")&&text.contains("各行は加算できません"),"nested and parallel work cannot be mistaken for elapsed wall duration");
        check(t.noiseStrongNanos1976==712*MS,"new work and snapshot events cannot rewrite the coarse wall timer");
    }
    public static void main(String[] args)throws Exception{
        run("explicit_trace_identity_and_current_scope",()->explicitOwners());
        run("selected_backends_and_unknown_semantics",()->selectedAndUnknown());
        run("bounded_reasons_and_invalid_inputs",()->reasonsAndValidation());
        run("snapshot_outcomes_are_not_gpu_adoption",()->snapshotEvents());
        run("unmeasured_zero_and_scalar_saturation",()->elapsedAndBounds());
        run("all_background_providers_and_optional_failures",()->backgroundAndFaults());
        run("damaged_optional_storage_preserves_legacy_summary",()->malformedOptionalStorage());
        run("parallel_multiple_owner_exact_totals",()->concurrentOwners());
        run("success_failure_immutable_persistence_and_restore",()->terminalAndPersistence());
        run("late_worker_terminal_race",()->terminalRace());
        run("no_operation_ui_storage_or_summary_provider",()->noHotPublication());
        run("parallel_work_and_wall_are_distinct",()->workWallMeaning());
        StringBuilder names=new StringBuilder("[");for(String name:cases){if(names.length()>1)names.append(',');names.append('"').append(name).append('"');}names.append(']');
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"tests\":"+names+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
