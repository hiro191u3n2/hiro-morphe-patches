package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Production scalar observations around a real queued attempt. Timings are
 * controlled observations, never host evidence of GPU pixels or device speed. */
public final class QualificationFinish1986Test {
    static final long MS=1000000L;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception{QualificationHandoff1985Test.reset();}
    static Field field(String name)throws Exception{return QualificationHandoff1985Test.field(name);}
    static String key(String shape){return "strong-resident1978:3:"+shape;}
    static String execute(String shape,QualificationHandoff1985Test.Task task)throws Exception{
        QualificationHandoff1985Test.Probe probe=new QualificationHandoff1985Test.Probe(task);
        check(GpuQualification1961.schedule(key(shape),1,probe),"resident observation admitted");
        QualificationHandoff1985Test.quiet();QualificationHandoff1985Test.waitEnd(probe);
        check(probe.closes.get()==1&&GpuQualification1961.retainedBytes()==0,"observation job releases ownership exactly once");
        return GpuQualification1961.attemptSummary1986();
    }
    static String row(String summary,String label){for(String line:summary.split("\n"))if(line.startsWith(label+":"))return line;return "";}
    static int eventCount(){synchronized(CameraTrace1965.events){return CameraTrace1965.events.size();}}
    static void subreasonsAndParentPhase()throws Exception{
        reset();String text=execute("private-finish-scope",c->{
            GpuQualification1961.progress1984("resident_baseline",0,1);
            String old=GpuQualification1961.attemptSummary1985();
            GpuQualification1961.finishCandidate1986(-1,8,0,0,1,1000*MS,0,0);
            GpuQualification1961.finishCandidate1986(0,5,2,2,1,1000*MS,0,960*MS);
            GpuQualification1961.finishCandidate1986(1,2,1,0,1,1000*MS,0,0);
            GpuQualification1961.finishCandidate1986(2,3,0,0,1,1000*MS,0,0);
            check(old.equals(GpuQualification1961.attemptSummary1985()),"finish observations do not replace enclosing phase or existing ledger");
            String active=GpuQualification1961.attemptSummary1986();
            check(active.contains("連結の前提認定 0/1")&&active.contains("（検証中・途中の記録）")&&!active.contains("（終了時の記録）"),"active dependency remains explicitly provisional and parent stays 0/1");
            int events=eventCount();GpuQualification1961.finishCandidate1986(2,3,0,0,1,1000*MS,0,0);
            check(events==eventCount(),"identical observation does not duplicate rolling records");
            GpuQualification1961.outcome1984("baseline_unavailable");
        });
        check(text.contains("前提となる認定が未成立")&&text.contains("認定書込 0回")&&text.contains("（終了時の記録）"),"parent terminal outcome and real write count remain separate");
        check(row(text,"旧GPU仕上げ").contains("保存済み画質不一致により対象外")&&row(text,"旧GPU仕上げ").contains("全画素一致 0/2"),"cached legacy rejection is not a newly completed comparison");
        check(row(text,"新GPU仕上げ32行").contains("速度条件を満たさず")&&row(text,"新GPU仕上げ32行").contains("全画素一致 2/2 / 速度計測 2/2"),"two exact trials do not imply acceptable speed");
        check(row(text,"新GPU仕上げ32行").contains("CPU 1000ms・旧GPU 未取得・候補GPU 960ms"),"actual CPU reference and candidate time survive with unmeasured legacy explicit");
        check(row(text,"新GPU仕上げ64行").contains("今回の全画素比較が不一致")&&row(text,"新GPU仕上げ64行").contains("全画素一致 1/2"),"second mismatch preserves only the completed successful trial count");
        check(row(text,"新GPU仕上げ128行").contains("実行・結果取得を完了できず"),"unavailable execution differs from speed and actual quality rejection");
        check(text.contains("CPU最速・旧GPU最速・候補GPU最遅")&&text.contains("写真全体の処理時間ではありません"),"min/max verification timing scope is explicit");
        check(!text.contains("private-finish-scope")&&!CameraTrace1965.events.toString().contains("private-finish-scope"),"condition keys never enter retained observations or events");
        String frozen=text;GpuQualification1961.finishCandidate1986(0,7,2,2,1,1000*MS,0,900*MS);
        GpuQualification1961.rejectExact("later-unrelated");
        check(frozen.equals(GpuQualification1961.attemptSummary1986()),"inactive late observation and later cache changes cannot rewrite an ended dependency");
    }
    static void everyReasonAndProofDistinction()throws Exception{
        reset();final String[] reasons={"確認中","画質比較が一致","今回の全画素比較が不一致","実行・結果取得を完了できず",
            "検証用メモリ不足","速度条件を満たさず","撮影変更・中断で取消","比較条件が成立",
            "保存済み画質不一致により対象外","別候補を選択","CPU基準の結果が不安定"};
        String text=execute("reasons",c->{
            for(int reason=0;reason<reasons.length;reason++){
                GpuQualification1961.finishCandidate1986(0,reason,2,2,1,1000*MS,0,900*MS);
                check(row(GpuQualification1961.attemptSummary1986(),"新GPU仕上げ32行").contains(": "+reasons[reason]+" /"),"stable finish subreason has distinct truthful label: "+reason);
            }
            GpuQualification1961.finishCandidate1986(1,7,2,2,2,1000*MS,800*MS,700*MS);
            GpuQualification1961.outcome1984("qualified");
        });
        check(row(text,"新GPU仕上げ64行").contains("比較基準: CPUと旧GPUの両方")&&row(text,"新GPU仕上げ64行").contains("CPU 1000ms・旧GPU 800ms・候補GPU 700ms"),"legacy gate uses both actual references, not an unrelated CPU duration");
        check(text.contains("認定書込 0回")&&!text.contains("認定書込あり・検証終了")&&GpuQualification1961.restore(key("reasons"))==null,"met comparison conditions and a caller success label cannot manufacture a certificate");
        reset();text=execute("actual-write",c->{
            GpuQualification1961.finishCandidate1986(0,7,2,2,1,1000*MS,0,950*MS);
            GpuQualification1961.qualified(key("actual-write"),1000*MS,950*MS,0);
        });
        check(text.contains("認定書込 1回")&&text.contains("認定書込あり・検証終了")&&GpuQualification1961.restore(key("actual-write"))!=null,"only actual existing qualified path increments proof writes, including exact 5% boundary");
        check(text.contains("比較条件の成立と実際の認定書込は別"),"display explicitly separates condition observation from certificate creation");
    }
    static void malformedAndNoActiveOwner()throws Exception{
        reset();String empty=GpuQualification1961.attemptSummary1986();
        GpuQualification1961.finishCandidate1986(0,2,1,0,1,1,0,1);
        check(empty.equals(GpuQualification1961.attemptSummary1986())&&empty.equals(GpuQualification1961.attemptSummary1985()),"no worker means no new attempt or invented detail");
        execute("malformed",c->{
            GpuQualification1961.finishCandidate1986(0,1,1,0,1,1000*MS,0,0);
            String stable=GpuQualification1961.attemptSummary1986();int events=eventCount();
            long[][] invalid={{-2,1,1,0,1,1,0,0},{3,1,1,0,1,1,0,0},{0,-1,1,0,1,1,0,0},{0,11,1,0,1,1,0,0},
                {0,1,-1,0,1,1,0,0},{0,1,3,0,1,1,0,0},{0,1,1,-1,1,1,0,0},{0,1,1,3,1,1,0,0},
                {0,1,1,0,-1,1,0,0},{0,1,1,0,3,1,0,0},{0,1,1,0,1,-1,0,0},{0,1,1,0,1,1,-1,0},{0,1,1,0,1,1,0,-1},
                {0,7,1,2,1,100,0,90},{0,7,2,1,1,100,0,90},{0,7,2,2,0,100,0,90},{0,7,2,2,1,0,0,90},
                {0,7,2,2,1,100,0,0},{0,7,2,2,1,100,0,96},{0,7,2,2,2,100,0,90},{0,7,2,2,2,100,80,77}};
            for(long[] v:invalid){GpuQualification1961.finishCandidate1986((int)v[0],(int)v[1],(int)v[2],(int)v[3],(int)v[4],v[5],v[6],v[7]);
                check(stable.equals(GpuQualification1961.attemptSummary1986())&&events==eventCount(),"invalid observation makes no partial state or event update");}
        });
    }
    static void cancellationAndOtherThread()throws Exception{
        reset();CountDownLatch ready=new CountDownLatch(1),release=new CountDownLatch(1);
        QualificationHandoff1985Test.Probe probe=new QualificationHandoff1985Test.Probe(c->{
            GpuQualification1961.progress1984("resident_baseline",0,1);GpuQualification1961.finishCandidate1986(0,1,1,0,1,1000*MS,0,0);
            ready.countDown();for(;;)try{release.await();break;}catch(InterruptedException expected){}
            GpuQualification1961.finishCandidate1986(0,6,1,0,1,1000*MS,0,0);
            GpuQualification1961.qualified(key("cancelled"),100,80,0);
        });
        check(GpuQualification1961.schedule(key("cancelled"),1,probe),"cancel test admitted");QualificationHandoff1985Test.quiet();
        check(ready.await(4,TimeUnit.SECONDS),"active comparison reached controlled boundary");
        String active=GpuQualification1961.attemptSummary1986();GpuQualification1961.finishCandidate1986(0,7,2,2,1,100,0,80);
        check(active.equals(GpuQualification1961.attemptSummary1986()),"unrelated thread cannot update active worker's observation");
        GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;
        check(GpuQualification1961.retainedBytes()==1&&probe.closes.get()==0,"cancellation still protects in-flight buffers");
        release.countDown();QualificationHandoff1985Test.waitEnd(probe);String text=GpuQualification1961.attemptSummary1986();
        check(row(text,"新GPU仕上げ32行").contains("撮影変更・中断で取消")&&text.contains("認定書込 0回"),"last cancellation observation is captured before worker finally without authorizing cancelled writes");
        check(GpuQualification1961.restore(key("cancelled"))==null,"cancelled diagnostic worker cannot write a certificate");
    }
    static void isolatedSinkCacheAndScalarHistory()throws Exception{
        reset();final QualificationHandoff1985Test.CountingContext context=new QualificationHandoff1985Test.CountingContext();
        GpuQualification1961.initialize(context);
        String text=execute("fault-sink",c->{
            GpuQualification1961.qualified("independent-a",100,80,0);GpuQualification1961.qualified("independent-b",100,80,0);
            GpuQualification1961.rejectExact("independent-exact");GpuQualification1961.rejectSpeed("independent-speed");
            List<String> records=QualificationHandoff1985Test.keys("RECORDS"),failures=QualificationHandoff1985Test.keys("FAILURES");
            int reads=context.memory.reads,edits=context.memory.edits;
            CameraTrace1965.fail=true;
            GpuQualification1961.finishCandidate1986(-1,4,0,0,0,0,0,0);
            GpuQualification1961.finishCandidate1986(0,7,2,2,1,100,0,80);
            for(int i=0;i<4;i++)GpuQualification1961.attemptSummary1986();
            check(records.equals(QualificationHandoff1985Test.keys("RECORDS"))&&failures.equals(QualificationHandoff1985Test.keys("FAILURES")),"rendering and broken trace sink never touch access-ordered proof or failure caches");
            check(reads==context.memory.reads&&edits==context.memory.edits,"optional observations never load or edit proof preferences");
            GpuQualification1961.qualified(key("fault-sink"),100,80,0);
        });
        check(text.contains("認定書込 3回")&&text.contains("比較条件が成立")&&GpuQualification1961.restore(key("fault-sink"))!=null,"throwing trace sink cannot interfere with certificate creation or observation state");
        for(Class<?> type:GpuQualification1961.class.getDeclaredClasses())if(type.getSimpleName().equals("Attempt1984"))
            for(Field f:type.getDeclaredFields())check(f.getType().isPrimitive(),"bounded history retains only scalars: "+f.getName());
        CameraTrace1965.fail=false;
    }
    static void eventWidthAndBoundedRecentDetail()throws Exception{
        reset();execute("event-cap",c->{
            Object job=((ThreadLocal<?>)field("CURRENT").get(null)).get();Field a=job.getClass().getDeclaredField("attempt1984");a.setAccessible(true);Object attempt=a.get(job);
            Field sequence=attempt.getClass().getDeclaredField("sequence");sequence.setAccessible(true);sequence.setLong(attempt,Long.MAX_VALUE);
            for(int candidate=-1;candidate<3;candidate++)for(int reason=0;reason<=10;reason++)if(reason!=7)
                GpuQualification1961.finishCandidate1986(candidate,reason,2,2,2,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE);
            GpuQualification1961.finishCandidate1986(2,7,2,2,2,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE-Long.MAX_VALUE/20);
        });
        boolean cause=false,times=false;
        for(String event:CameraTrace1965.events)if(event.startsWith("gpu_finish86")){
            int split=event.indexOf(' ',event.indexOf(' ')+1);check(split>=0&&event.length()-split-1<=160,"every new payload remains within original 160-character field cap with maximum longs");
            cause|=event.startsWith("gpu_finish86 ");times|=event.startsWith("gpu_finish86_times ");
        }
        check(cause&&times&&CameraTrace1965.events.toString().contains("id=9223372036854775807"),"fixed cause and separate scalar time records both survive maximum sequence");
        reset();for(int n=0;n<18;n++){final int index=n;execute("bounded-"+n,c->{
            for(int candidate=-1;candidate<3;candidate++)GpuQualification1961.finishCandidate1986(candidate,5,2,2,2,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE-index);
        });}
        String text=GpuQualification1961.attemptSummary1986();int detail=text.indexOf("仕上げ前提の直近検証");
        check(field("attemptCount1984").getInt(null)==16&&detail>=0&&text.indexOf("仕上げ前提の直近検証",detail+1)<0,"sixteen scalar attempts render only one detailed dependency");
        check(text.contains("仕上げ前提の直近検証 #18")&&!text.contains("\n#1 ")&&text.length()<6000,"latest dependency remains visible inside the existing live-display limit");
        check(text.contains("旧GPU仕上げ")&&text.contains("新GPU仕上げ128行"),"bounded latest view retains all four candidate outcomes");
    }
    public static void main(String[] args)throws Exception{
        subreasonsAndParentPhase();everyReasonAndProofDistinction();malformedAndNoActiveOwner();cancellationAndOtherThread();
        isolatedSinkCacheAndScalarHistory();eventWidthAndBoundedRecentDetail();reset();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"qualification_finish_subreasons1986\":true,\"qualification_finish_parent_phase_preserved1986\":true,\"qualification_finish_reference_and_trials1986\":true,\"qualification_finish_proof_write_separation1986\":true,\"qualification_finish_diagnostic_noninterference1986\":true,\"qualification_finish_cancelled_snapshot1986\":true,\"qualification_finish_scalar_payload_bound1986\":true}");
    }
}
