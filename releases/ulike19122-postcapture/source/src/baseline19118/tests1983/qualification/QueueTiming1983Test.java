package com.hiro.ulike;

import java.lang.reflect.*;

/** Actual photo recording/rendering; live qualification is a controlled peer. */
public final class QueueTiming1983Test {
    static final long M=1024L*1024;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception{Timing1982Test.reset();}
    static String render(ProcessingTiming1947.Trace trace)throws Exception{return Timing1982Test.render(trace);}
    static void record(ProcessingTiming1947.Trace trace,int reason,long remain,int retries,int queued,int running,long held,long request){
        ProcessingTiming1947.residentExit1983(trace,47*M,reason==1?2:6,reason,remain,retries,queued,running,held,request);
    }
    static void labelsAndReadOnly()throws Exception {
        reset();String[] labels={"検証予約の事前条件成立","保存後の検証を予約","検証条件を識別できず","検証画像の保持量が不正",
            "この候補だけで検証保持上限96 MiBを超過","保存後の検証内からの再予約を停止","処理スレッドの中断",
            "端末・GPU環境の認定情報を取得できず","同じ条件の画質不一致記録あり","画質不一致以外の失敗後の再試行間隔待ち",
            "画質不一致以外の失敗の再試行上限","同じ条件の認定が先に成立","同じ条件の検証を実行中","同じ条件の検証を予約済み",
            "強ノイズ検証用の予約枠を確保","検証ジョブ数の上限8件","検証画像の合計保持上限96 MiBまでの空き不足",
            "検証ジョブ数8件と合計保持96 MiBの両方が不足","検証処理が未生成","検証予約処理を開始できず","検証予約処理のメモリ確保失敗"};
        for(int i=0;i<labels.length;i++){
            ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
            record(trace,i,1234000000L,2,4,1,80*M,47*M);
            String text=render(trace);check(text.contains("連結検証の判定: "+labels[i]),"distinct scalar reason "+i);
            check(trace.strongGpuStrips==0&&trace.strongCpuStrips==0&&trace.correctionRoute1976==-1,"admission telemetry cannot fabricate image adoption");
            check(text.contains("予約済み 4件 / 実行中 1件"),"actual decision queue counts remain separate from cache history");
        }
        check(GpuQualification1961.statusCalls==0,"recording and rendering never query live qualification or queue");
        for(Field f:ProcessingTiming1947.Trace.class.getDeclaredFields())if(f.getName().endsWith("1983"))
            check(f.getType().isPrimitive(),"new photo fields cannot retain image or configuration object: "+f.getName());
    }
    static void immutableSavedDecision()throws Exception {
        reset();Timing1982Test.MemoryPreferences preferences=new Timing1982Test.MemoryPreferences();
        ProcessingTiming1947.init(new Timing1982Test.MemoryContext(preferences));
        ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(trace,4,12);
        ProcessingTiming1947.resident1978(trace,3060,4080,4284,5712,0,false,3121200,47*M,0);
        record(trace,9,1234000000L,1,4,1,80*M,47*M);
        GpuQualification1961.status="CPU/GPU再判定（現在）: 保存時だけの状態";
        ProcessingTiming1947.finish(trace,true);String saved=ProcessingTiming1947.summary();int saves=preferences.saves;
        check(saved.contains("判定時の残り 2秒")&&saved.contains("再試行実施済み 1回"),"remaining interval rounds up without pretending a positive wait is zero");
        check(saved.contains("判定時の検証保持: 使用 80.0 MiB / 上限96 MiB / この候補 47.0 MiB"),"memory evidence is scalar and explicitly observed at decision time");
        check(saved.contains("GPU 4区間 / CPU 12区間")&&saved.contains("3060×4080 → 4284×5712"),"existing per-photo evidence remains intact");
        check(saved.equals(preferences.getString("summary","")),"exact admission detail persisted with completed photo");
        GpuQualification1961.status="CPU/GPU再判定（現在）: 後から変わった状態";
        record(trace,1,0,0,7,0,47*M,47*M);ProcessingTiming1947.residentExit1982(trace,1,1);
        check(saved.equals(ProcessingTiming1947.summary())&&saves==preferences.saves,"late qualification cannot rewrite or repersist photo decision");
        check(trace.residentQueueReason1983==9&&trace.residentRetryRemainingNanos1983==1234000000L,"late callback preserves exact original scalar decision");
        int calls=GpuQualification1961.statusCalls;
        for(int i=0;i<5;i++)check(saved.equals(ProcessingTiming1947.summary()),"saved summary is stable across subsequent reads");
        check(calls==GpuQualification1961.statusCalls,"saved rendering performs no live scheduler inspection");
        String live=ProcessingTiming1947.liveSummary1979();
        check(live.startsWith(saved)&&live.contains("表示時点のCPU/GPU再判定（現在）: 後から変わった状態"),"live cache suffix cannot replace historical cause");
        check(saved.equals(preferences.getString("summary","")),"opening live status cannot persist later queue as photo evidence");
        reset();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(preferences));
        check(saved.equals(ProcessingTiming1947.summary()),"restart preserves diagnostic decision without environment or source image");
    }
    static void invalidAndUnknownScalars()throws Exception {
        reset();ProcessingTiming1947.Trace trace=ProcessingTiming1947.begin(new Object());record(trace,13,0,0,4,1,80*M,47*M);
        long[][] invalid={{-2,0,0,4,1,80*M,47*M},{21,0,0,4,1,80*M,47*M},{13,-1,0,4,1,80*M,47*M},
            {13,0,-1,4,1,80*M,47*M},{13,0,0,-2,1,80*M,47*M},{13,0,0,9,1,80*M,47*M},
            {13,0,0,4,-2,80*M,47*M},{13,0,0,4,2,80*M,47*M},{13,0,0,4,1,-2,47*M},{13,0,0,4,1,80*M,-2}};
        for(long[] value:invalid){
            record(trace,(int)value[0],value[1],(int)value[2],(int)value[3],(int)value[4],value[5],value[6]);
            check(trace.residentQueueReason1983==13&&trace.residentQueuedJobs1983==4&&trace.residentRunningJobs1983==1&&
                trace.residentQueueRetainedBytes1983==80*M&&trace.residentQueueRequestedBytes1983==47*M&&trace.residentRetryRemainingNanos1983==0,
                "invalid observer payload cannot partly overwrite a real decision");
        }
        record(trace,19,0,0,-1,-1,-1,-1);String unknown=render(trace);
        check(unknown.contains("検証予約処理を開始できず")&&!unknown.contains("予約済み 0件")&&!unknown.contains("判定時の検証保持:"),"exception fallback does not invent empty queue or zero retention");
        ProcessingTiming1947.resident1978(trace,10,10,20,20,0,false,25,0,0);
        check(!render(trace).contains("連結検証の判定:"),"fresh route observation removes previous attempt detail");
        record(trace,12,0,0,0,1,47*M,47*M);ProcessingTiming1947.residentExit1982(trace,0,8);
        check(!render(trace).contains("連結検証の判定:")&&render(trace).contains("GPU連結: GPUセッション使用中"),"legacy final exit cannot inherit stale queue cause");
    }
    static void explicitOwner()throws Exception {
        reset();ProcessingTiming1947.Trace owner=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.Trace other=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.enter(other);
        record(owner,14,0,0,7,0,12*M,47*M);
        check(render(owner).contains("強ノイズ検証用の予約枠を確保"),"captured owner receives exact queue cause");
        check(other.residentQueueReason1983== -1&&!ProcessingTiming1947.summary().contains("強ノイズ検証用の予約枠を確保"),"unrelated current/newest photo cannot receive old admission result");
    }
    public static void main(String[] args)throws Exception {
        labelsAndReadOnly();immutableSavedDecision();invalidAndUnknownScalars();explicitOwner();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"resident_queue_labels1983\":true,\"resident_saved_decision_immutable1983\":true,\"resident_diagnostics_read_only1983\":true}");
    }
}
