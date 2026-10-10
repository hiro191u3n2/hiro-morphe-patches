package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Exercise production queue, cache and scalar ledger with controlled gates.
 * Certificate creation here is a state-machine fixture, not pixel evidence. */
public final class QualificationHandoff1985Test {
    static final long M=1024L*1024,MS=1000000L;
    static int assertions;
    static final String[] PREFIX={"strong-gx1973-ieee-div-policy-bank-v1:","strong-gx1964-parallel-policy-bank-v1:",
        "strong-gx1971-generic-policy-bank-v1:","strong-gx1976-ieee-tile8-policy-bank-v1:",
        "strong-gx1978-policy-direct-ieee-v1:","strong-gx1978-policy-direct-tile8-v1:",
        "strong-gx1978-policy-direct-pow2-v1:","strong-gx1978-policy-direct-pow2-tile8-v1:"};
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    interface Task {void run(GpuQualification1961.Cancellation cancellation)throws Exception;}
    static final class Probe implements GpuQualification1961.Probe {
        final AtomicInteger runs=new AtomicInteger(),closes=new AtomicInteger();
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();final Task task;
        Probe(Task task){this.task=task;}
        public void run(GpuQualification1961.Cancellation c){runs.incrementAndGet();try{if(task!=null)task.run(c);}catch(Throwable error){failure.set(error);throw new AssertionError(error);}}
        public void close(){closes.incrementAndGet();}
    }
    static void reset()throws Exception{QualificationProgress1984Test.reset();}
    static void quiet()throws Exception{QualificationProgress1984Test.quiet();}
    static Field field(String name)throws Exception{return QualificationProgress1984Test.field(name);}
    static String group(String shape){return QualificationProgress1984Test.group(shape);}
    static String cpu(String shape){return QualificationProgress1984Test.cpu(shape);}
    static String child(String shape,int profile){return PREFIX[profile]+"3:"+shape;}
    static void waitEnd(Probe probe)throws Exception{
        QualificationProgress1984Test.await(()->probe.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"new diagnostic job ended");
        check(probe.runs.get()==1,"new diagnostic probe runs exactly once");
        if(probe.failure.get()!=null)throw new AssertionError(probe.failure.get());
    }
    static String execute(String shape,Task task)throws Exception{
        Probe probe=new Probe(task);check(GpuQualification1961.scheduleStrong1978(group(shape),cpu(shape),1,probe),"Strong observation job admitted");
        quiet();waitEnd(probe);return GpuQualification1961.attemptSummary1985();
    }
    static void write(String shape,int profile,int variant){
        GpuQualification1961.qualifiedStrongProfile1975(group(shape),child(shape,profile),100,80,variant);
        GpuQualification1961.qualifiedStrongPreferred1970(group(shape),100,80,profile*3+variant);
    }
    static void select(String shape,int profile,int variant,boolean lost){GpuQualification1961.selection1985(child(shape,profile),profile,variant,lost);}
    static List<String> keys(String field)throws Exception{synchronized(field("LOCK").get(null)){return new ArrayList<String>(((Map<String,?>)field(field).get(null)).keySet());}}
    static void recordAndSelectionAreDifferent()throws Exception{
        reset();String text=execute("writes-unchecked",c->{write("writes-unchecked",0,0);GpuQualification1961.progress1984("strong_profile",8,8);});
        check(text.contains("認定書込 2回")&&text.contains("終了時採用候補: 未確認"),"writes alone cannot imply a selected end route");
        check(text.contains("確認一巡")&&text.contains("全候補の合格数ではなく"),"complete scan is explicitly distinct from success count");
        reset();text=execute("child-only",c->{GpuQualification1961.qualifiedStrongProfile1975(group("child-only"),child("child-only",4),100,80,0);select("child-only",4,0,false);});
        check(text.contains("終了時採用候補: 不成立"),"exact direct child alone is never an aggregate choice");
        check(GpuQualification1961.restore(child("child-only",4))!=null&&GpuQualification1961.restore(group("child-only"))==null,"diagnostic did not manufacture an aggregate");
        reset();text=execute("variant-mismatch",c->{write("variant-mismatch",0,1);select("variant-mismatch",0,0,false);});
        check(text.contains("終了時採用候補: 不成立"),"variant mismatch cannot be displayed as usable");
        reset();text=execute("false-call",c->{GpuQualification1961.outcome1984("strong_qualified");select("false-call",0,0,false);});
        check(text.contains("終了時採用候補: 不成立")&&text.contains("認定書込 0回")&&!text.contains("認定書込あり・検証終了"),"caller success and child strings do not create a proof");
    }
    static void authoritativeTerminalAndHistory()throws Exception{
        reset();final String shape="pre-cohort";CountDownLatch ready=new CountDownLatch(1),finish=new CountDownLatch(1);
        Probe probe=new Probe(c->{write(shape,0,0);select(shape,0,0,false);ready.countDown();finish.await(4,TimeUnit.SECONDS);GpuQualification1961.rejectExact(child(shape,0));});
        check(GpuQualification1961.scheduleStrong1978(group(shape),cpu(shape),1,probe),"pre-cohort job admitted");quiet();
        check(ready.await(4,TimeUnit.SECONDS),"pre-cohort selection reached");
        String active=GpuQualification1961.attemptSummary1985();
        check(active.contains("検証中")&&active.contains("終了時に再確認")&&!active.contains("終了時採用候補: 成立"),"in-progress selection cannot be reported as a finished fact");
        check(!CameraTrace1965.events.toString().contains("selection_at_end=available"),"rolling trace also waits for actual worker finish");
        finish.countDown();waitEnd(probe);String text=GpuQualification1961.attemptSummary1985();
        check(text.contains("終了時採用候補: 失効"),"final callback omission cannot hide exact rejection during cohort");
        check(CameraTrace1965.events.toString().contains("selection_at_end=invalidated"),"end invalidation reaches bounded rolling trace");
        reset();text=execute("aggregate-removed",c->{write("aggregate-removed",0,0);select("aggregate-removed",0,0,false);GpuQualification1961.invalidateStrongTuning1975(group("aggregate-removed"),0);});
        check(text.contains("終了時採用候補: 失効")&&GpuQualification1961.restore(child("aggregate-removed",0))!=null,"aggregate disappearance is enough to lose the earlier selection");
        reset();text=execute("replacement",c->{write("replacement",0,0);select("replacement",0,0,false);GpuQualification1961.rejectExact(child("replacement",0));write("replacement",2,1);select("replacement",2,1,true);});
        check(text.contains("終了時採用候補: 成立（候補 2・方式 1）")&&text.contains("前候補の失効後に選び直し"),"valid already-certified replacement is distinguished from invalid original");
        String frozen=text;GpuQualification1961.rejectExact(child("replacement",2));
        check(frozen.equals(GpuQualification1961.attemptSummary1985()),"past end facts do not claim or track current cache validity");
        check(frozen.contains("現在のキャッシュ状態を保証しません"),"historical selection scope is explicit");
        reset();final String prior="preexisting";probe=new Probe(c->select(prior,0,0,false));
        check(GpuQualification1961.scheduleStrong1978(group(prior),cpu(prior),1,probe),"job can precede an independent proof arrival");
        GpuQualification1961.qualifiedStrongPreferred1970(child(prior,0),100,80,0);
        GpuQualification1961.qualifiedStrongPreferred1970(group(prior),100,80,0);
        quiet();waitEnd(probe);text=GpuQualification1961.attemptSummary1985();
        check(text.contains("認定書込 0回")&&text.contains("終了時採用候補: 成立"),"usable existing proof is independent of this attempt's write count");
        reset();text=execute("cpu-aggregate",c->{GpuQualification1961.qualifiedStrongProfile1975(group("cpu-aggregate"),child("cpu-aggregate",5),100,80,2);
            GpuQualification1961.qualified(cpu("cpu-aggregate"),100,95,17);select("cpu-aggregate",5,2,false);});
        check(text.contains("終了時採用候補: 成立（候補 5・方式 2）"),"worker-bound valid CPU aggregate supports the direct child");
        reset();text=execute("cpu-too-slow",c->{GpuQualification1961.qualifiedStrongProfile1975(group("cpu-too-slow"),child("cpu-too-slow",5),100,80,2);
            GpuQualification1961.qualified(cpu("cpu-too-slow"),100,96,17);select("cpu-too-slow",5,2,false);});
        check(text.contains("終了時採用候補: 不成立"),"diagnostic cannot bypass existing five-percent aggregate gate");
    }
    static final class CountingMemory extends Context.Memory {
        int reads,edits;
        public synchronized String getString(String key,String fallback){reads++;return super.getString(key,fallback);}
        public synchronized Map<String,?> getAll(){reads++;return super.getAll();}
        public SharedPreferences.Editor edit(){edits++;return super.edit();}
    }
    static final class CountingContext extends Context {
        final CountingMemory memory=new CountingMemory();
        public SharedPreferences getSharedPreferences(String name,int mode){return memory;}
    }
    static void observationAndComparisonNoninterference()throws Exception{
        reset();final CountingContext context=new CountingContext();GpuQualification1961.initialize(context);
        String text=execute("observation-private-shape",c->{
            write("observation-private-shape",0,0);GpuQualification1961.qualified("independent-record",100,80,0);
            GpuQualification1961.rejectExact("independent-exact");GpuQualification1961.rejectSpeed("independent-speed");
            List<String> records=keys("RECORDS"),failures=keys("FAILURES");int reads=context.memory.reads,edits=context.memory.edits;
            for(int i=0;i<5;i++){select("observation-private-shape",0,0,false);GpuQualification1961.attemptSummary1985();}
            check(records.equals(keys("RECORDS"))&&failures.equals(keys("FAILURES")),"observation does not reorder either access-ordered cache");
            check(context.memory.reads==reads&&context.memory.edits==edits,"observation does not load or edit persisted certificates");
            GpuQualification1961.timings1984(900*MS,200*MS);GpuQualification1961.comparison1985("strong_gpu_legacy",1,250*MS,200*MS);
            GpuQualification1961.comparison1985("private-arbitrary-ref",1,1,1);GpuQualification1961.comparison1985("strong_cpu_parallel",0,1,1);
            GpuQualification1961.comparison1985("strong_cpu_parallel",5,1,1);GpuQualification1961.comparison1985("strong_cpu_parallel",4,-1,1);
            GpuQualification1961.selection1985(child("wrong-condition",0),0,0,true);
            GpuQualification1961.selection1985(child("observation-private-shape",0),7,0,true);
            GpuQualification1961.selection1985(null,0,0,true);
        });
        check(text.contains("Strong検証区間の直近比較: 既存GPU 250ms → 候補GPU 200ms")&&!text.contains("900ms"),"legacy GPU speed comparison uses its actual baseline instead of unrelated CPU oracle time");
        check(text.contains("終了時採用候補: 成立")&&!text.contains("前候補の失効後"),"malformed observation cannot alter a valid selection snapshot");
        check(text.contains("写真全体の処理時間や保存済みGPU採用区間数とは別"),"verification timing scope excludes photograph elapsed time");
        check(!text.contains("observation-private-shape")&&!CameraTrace1965.events.toString().contains("observation-private-shape")&&!CameraTrace1965.events.toString().contains("private-arbitrary-ref"),"display and persistent events retain no condition or arbitrary label");
        reset();text=execute("parallel-scope",c->GpuQualification1961.comparison1985("strong_cpu_parallel",4,350*MS,300*MS));
        check(text.contains("並列CPU 4並列 350ms → 候補GPU 300ms"),"actual worker count and CPU comparison wall time are explicit");
        reset();CameraTrace1965.fail=true;text=execute("failing-sink",c->{write("failing-sink",0,0);select("failing-sink",0,0,false);GpuQualification1961.comparison1985("strong_gpu_legacy",1,100,80);});
        check(text.contains("終了時採用候補: 成立")&&GpuQualification1961.restore(child("failing-sink",0))!=null,"diagnostic sink failure cannot alter valid proof or ownership");
        for(Class<?> type:GpuQualification1961.class.getDeclaredClasses())if(type.getSimpleName().equals("Attempt1984"))
            for(Field f:type.getDeclaredFields())check(f.getType().isPrimitive(),"history contains scalars only: "+f.getName());
    }
    static GpuQualification1961.Reservation1984 shared(String shape,long exclusive,Object owner,long common){
        return GpuQualification1961.reserveStrongShared1985(group(shape),cpu(shape),exclusive,owner,common);
    }
    static void prebeginAndMaterializedOwnership()throws Exception{
        reset();GpuQualification1961.Reservation1984 noBegin=GpuQualification1961.reserve1985("strong-resident1978:3:no-begin",20*M);Probe probe=new Probe(null);
        check(noBegin.accepted()&&noBegin.commit(probe).reason==21,"new ticket cannot commit before explicit allocation start");
        check(probe.closes.get()==1&&probe.runs.get()==0&&GpuQualification1961.retainedBytes()==0,"unbegun commit refusal releases exactly once and does not execute");noBegin.close();
        check(!noBegin.begin1985(),"closed ticket cannot begin");
        reset();GpuQualification1961.Reservation1984 begun=GpuQualification1961.reserve1985("strong-resident1978:3:begun",80*M);
        check(begun.accepted()&&begun.begin1985()&&!begun.begin1985(),"copy begins once after real budget reservation");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(!begun.current()&&GpuQualification1961.retainedBytes()==80*M,"begun copy remains charged across capture cancellation");
        GpuQualification1961.Reservation1984 tooLarge=GpuQualification1961.reserve1985("strong-resident1978:3:new-too-large",17*M);
        check(!tooLarge.accepted()&&GpuQualification1961.retainedBytes()==80*M,"new capture cannot overbook materialized old owner beyond 96 MiB");
        probe=new Probe(null);check(begun.commit(probe).reason==6&&probe.closes.get()==1,"cancelled begun copy cannot be queued into next epoch");
        begun.close();check(GpuQualification1961.retainedBytes()==0,"cancelled begun commit and finally release only once");
        reset();Object model=new Object();GpuQualification1961.Reservation1984 old=shared("shared-old",10*M,model,60*M),active=shared("shared-active",10*M,model,60*M);
        check(old.accepted()&&active.accepted()&&active.begin1985()&&GpuQualification1961.retainedBytes()==80*M,"unstarted and started tickets share immutable model charge");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(GpuQualification1961.retainedBytes()==70*M&&!old.begin1985(),"only unstarted owner's exclusive bytes released while started model remains");
        GpuQualification1961.Reservation1984 next=shared("shared-new",26*M,model,60*M);
        check(next.accepted()&&GpuQualification1961.retainedBytes()==96*M,"new snapshot can reuse same model without double counting");
        old.close();check(GpuQualification1961.retainedBytes()==96*M&&next.current(),"late revoked owner close cannot remove new shared owner");
        active.close();check(GpuQualification1961.retainedBytes()==86*M,"started cancellation releases only its own exclusive bytes");next.close();
        check(GpuQualification1961.retainedBytes()==0,"last shared owner releases model exactly once");
        reset();GpuQualification1961.Reservation1984 exact=GpuQualification1961.reserve1985("strong-resident1978:3:exact-arrival",10*M);
        check(exact.begin1985(),"copy began before external exact failure");GpuQualification1961.rejectExact("strong-resident1978:3:exact-arrival");probe=new Probe(null);
        check(exact.commit(probe).reason==8&&probe.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"exact rejection remains authoritative at commit after begin");
        exact.close();check(!GpuQualification1961.maySchedule("strong-resident1978:3:exact-arrival"),"diagnostic and lifecycle repair preserve permanent exact rejection");
        reset();GpuQualification1961.Reservation1984 ticket=GpuQualification1961.reserve1985("strong-resident1978:3:oom-after-begin",10*M);
        try{check(ticket.begin1985(),"allocation-failure path owns materialized budget");throw new OutOfMemoryError("controlled detached allocation failure");}
        catch(OutOfMemoryError expected){GpuQualification1961.captureChanged();check(GpuQualification1961.retainedBytes()==10*M,"allocation failure does not release begun owner before finally");}
        finally{ticket.close();}check(GpuQualification1961.retainedBytes()==0,"allocation failure finally releases ticket");
    }
    static void cancellationAtEnd()throws Exception{
        reset();final String shape="cancelled-final";CountDownLatch entered=new CountDownLatch(1),released=new CountDownLatch(1);
        Probe probe=new Probe(c->{write(shape,0,0);select(shape,0,0,false);entered.countDown();for(;;)try{released.await();break;}catch(InterruptedException expected){}});
        check(GpuQualification1961.scheduleStrong1978(group(shape),cpu(shape),1,probe),"cancelled selected job admitted");quiet();
        check(entered.await(4,TimeUnit.SECONDS),"job selected before cancellation");GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(GpuQualification1961.retainedBytes()==1&&probe.closes.get()==0,"active cancellation does not release worker buffers prematurely");
        released.countDown();waitEnd(probe);String text=GpuQualification1961.attemptSummary1985();
        check(text.contains("終了時採用候補: 取消のため未確認")&&!text.contains("終了時採用候補: 成立"),"cancelled attempt cannot publish old selected availability");
    }
    static void rollingRecordCap()throws Exception{
        reset();execute("max-scalars",c->{
            Object job=((ThreadLocal<?>)field("CURRENT").get(null)).get();Field a=job.getClass().getDeclaredField("attempt1984");a.setAccessible(true);Object attempt=a.get(job);
            Field sequence=attempt.getClass().getDeclaredField("sequence");sequence.setAccessible(true);sequence.setLong(attempt,Long.MAX_VALUE);
            for(String name:new String[]{"certificates","exactFailures","speedFailures"}){Field f=attempt.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(attempt,65535);}
            GpuQualification1961.timings1984(Long.MAX_VALUE,Long.MAX_VALUE);
            GpuQualification1961.comparison1985("strong_cpu_parallel",4,Long.MAX_VALUE,Long.MAX_VALUE);
            String longestPhase="",longestOutcome="";
            for(String value:(String[])field("PHASE_CODES1984").get(null))if(value.length()>longestPhase.length())longestPhase=value;
            for(String value:(String[])field("OUT_CODES1984").get(null))if(value.length()>longestOutcome.length())longestOutcome=value;
            GpuQualification1961.progress1984(longestPhase,192,192);GpuQualification1961.outcome1984(longestOutcome);
            GpuQualification1961.selection1985(null,-1,-1,true);
        });
        execute("max-sequence",c->{GpuQualification1961.comparison1985("strong_gpu_legacy",1,Long.MAX_VALUE,Long.MAX_VALUE);GpuQualification1961.selection1985(null,-1,-1,true);});
        boolean selection=false,comparison=false,measure=false;
        for(String event:CameraTrace1965.events){int split=event.indexOf(' ',event.indexOf(' ')+1);check(split>=0&&event.length()-split-1<=160,"each emitted scalar payload fits unchanged CameraTrace 160-character cap");
            selection|=event.startsWith("gpu_proof85_selection ");comparison|=event.startsWith("gpu_proof85_compare ");measure|=event.startsWith("gpu_proof85_measure ");}
        check(selection&&comparison&&measure,"bounded end selection, actual comparison and old measurements use independent keyed records");
        check(CameraTrace1965.events.toString().contains("id=9223372036854775807"),"payload cap includes maximum-width sequence together with all bounded counters and long times");
    }
    public static void main(String[] args)throws Exception{
        recordAndSelectionAreDifferent();authoritativeTerminalAndHistory();observationAndComparisonNoninterference();prebeginAndMaterializedOwnership();cancellationAtEnd();rollingRecordCap();reset();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"qualification_terminal_selection1985\":true,\"qualification_progress_write_distinction1985\":true,\"qualification_comparison_scope1985\":true,\"qualification_prebegin_revocation1985\":true,\"qualification_materialized_owner_lifetime1985\":true,\"qualification_diagnostic_noninterference1985\":true,\"qualification_rolling_payload_bound1985\":true}");
    }
}
