package com.hiro.ulike;

import java.lang.reflect.Field;

/** Actual .85 photo diagnostics with later-changing optional proof data. */
public final class QueueTiming1985Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception{
        QueueTiming1984Test.reset();GpuQualification1961.attempts1985=null;
        GpuQualification1961.attemptCalls1985=0;GpuQualification1961.throwAttempts1985=false;
    }
    static String render(ProcessingTiming1947.Trace trace)throws Exception{return Timing1982Test.render(trace);}
    static void exactCopyDecisionAndSavedOwner()throws Exception{
        reset();Timing1982Test.MemoryPreferences preferences=new Timing1982Test.MemoryPreferences();
        ProcessingTiming1947.init(new Timing1982Test.MemoryContext(preferences));
        ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.noiseBackend(photo,0,16);ProcessingTiming1947.strongGpuVerification1970(photo,2);
        ProcessingTiming1947.resident1978(photo,3060,4080,4284,5712,0,false,3121200,47L*1024*1024,0);
        ProcessingTiming1947.residentCopy1985(photo,9,1,8,false,37);
        ProcessingTiming1947.residentExit1983(photo,47L*1024*1024,10,-1,0,0,-1,-1,-1,-1);
        ProcessingTiming1947.finish(photo,true);String saved=ProcessingTiming1947.summary();int writes=preferences.saves;
        check(saved.contains("連結検証のコピー枠判定: 別の検証画像がコピー開始前の予約中"),"same admission records the actual reserved-copy refusal");
        check(saved.contains("判定時の優先対象: Strongから連結")&&saved.contains("保持中: Strong候補（コピー開始前）")&&saved.contains("撮影世代 37"),"priority, holder phase and epoch come from the same immutable decision");
        check(photo.residentCopyReason1985==9&&photo.residentCopyActive1985==8&&!photo.residentCopyStarted1985&&photo.residentCopyEpoch1985==37,"saved copy scalars retain exact owner evidence");
        check(saved.contains("GPU 0区間 / CPU 16区間")&&photo.strongCpuVerifications==2,"copy admission cannot fabricate executed GPU output or CPU comparisons");
        GpuQualification1961.status="CPU/GPU再判定（現在）: 後続の状態";
        GpuQualification1961.attempts1985="保存後の検証履歴（終了時点の記録）\n#22 Strong: 候補確認 8/8（確認一巡） / 認定書込 9回 / 終了時採用候補: 失効\nStrong検証区間の直近比較: 既存GPU 250ms → 候補GPU 200ms";
        String live=ProcessingTiming1947.liveSummary1979();
        check(live.startsWith(saved)&&live.contains("終了時採用候補: 失効")&&live.contains("既存GPU 250ms"),"new end-state and actual reference are appended only to opened live view");
        check(GpuQualification1961.attemptCalls1985==1&&GpuQualification1961.attemptCalls1984==0,"live view requests the .85 provider without querying obsolete summary");
        check(saved.equals(preferences.getString("summary",""))&&preferences.saves==writes,"viewing later proof does not rewrite stored photograph");
        ProcessingTiming1947.residentCopy1985(photo,0,2,-1,false,99);
        ProcessingTiming1947.residentExit1983(photo,1,1,21,0,0,0,0,0,0);
        check(saved.equals(ProcessingTiming1947.summary())&&preferences.saves==writes&&photo.residentCopyEpoch1985==37,"completed photo rejects late admission and queue snapshots");
        check(photo.strongGpuStrips==0&&photo.strongCpuStrips==16&&photo.strongCpuVerifications==2,"later qualification does not change saved backend or verification counts");
        reset();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(preferences));
        check(saved.equals(ProcessingTiming1947.summary()),"restart restores original refusal and photo time without live proof lookup");
    }
    static void allReasonsFamiliesAndUnknowns()throws Exception{
        reset();String[] labels={"コピー用の枠を予約","コピー要求が不正","このコピーの容量上限を超過","処理スレッドの中断",
            "今回の全画像コピー枠は使用済み","別工程の全画像コピーを優先","今回の区間コピー容量の上限","別工程の区間コピーを優先",
            "別の検証画像をコピー開始済み","別の検証画像がコピー開始前の予約中"};
        for(int reason=0;reason<labels.length;reason++){
            ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());
            ProcessingTiming1947.residentCopy1985(photo,reason,8,1,reason==8,12);
            String text=render(photo);check(text.contains("連結検証のコピー枠判定: "+labels[reason]),"copy decision reason maps exactly: "+reason);
            check(photo.strongGpuStrips==0&&photo.strongCpuStrips==0&&photo.correctionRoute1976== -1,"copy reason does not set route or image counters");
            check(text.contains(reason==8?"保持中: Strongから連結（コピー開始済み）":"保持中: Strongから連結（コピー開始前）"),"holder start phase is explicit and independent of candidate reason");
        }
        int[] families={-1,0,1,2,4,8,16,32};String[] names={"なし","区間検証","Strongから連結","仕上げ","モアレ・形状","Strong候補","単独工程","残差工程"};
        for(int i=0;i<families.length;i++){
            ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.residentCopy1985(photo,0,families[i],families[i],false,0);
            String text=render(photo);check(text.contains("判定時の優先対象: "+(families[i]==0?"未選択":names[i]))&&text.contains("保持中: "+names[i]),"priority zero is unselected while active zero denotes strip owner: "+families[i]);
            if(families[i]<0)check(!text.contains("なし（コピー開始前）"),"unknown holder is not reported as a real reservation");
        }
        check(GpuQualification1961.statusCalls==0&&GpuQualification1961.attemptCalls1985==0,"record and render perform no later queue or proof inquiry");
        for(Field field:ProcessingTiming1947.Trace.class.getDeclaredFields())if(field.getName().endsWith("1985"))
            check(field.getType().isPrimitive(),"copy diagnosis retains no image, owner or configuration object: "+field.getName());
    }
    static void invalidAndExplicitOwner()throws Exception{
        reset();ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.residentCopy1985(photo,9,1,8,false,37);
        int[][] invalid={{-1,1,8},{10,1,8},{Integer.MAX_VALUE,1,8},{9,-2,8},{9,3,8},{9,64,8},{9,1,-2},{9,1,3},{9,1,64}};
        for(int[] value:invalid){ProcessingTiming1947.residentCopy1985(photo,value[0],value[1],value[2],true,99);
            check(photo.residentCopyReason1985==9&&photo.residentCopyPreferred1985==1&&photo.residentCopyActive1985==8&&!photo.residentCopyStarted1985&&photo.residentCopyEpoch1985==37,"invalid payload is rejected atomically");}
        ProcessingTiming1947.residentCopy1985(photo,0,1,-1,false,-1);ProcessingTiming1947.residentCopy1985(null,0,1,-1,false,0);
        check(photo.residentCopyEpoch1985==37&&photo.residentCopyReason1985==9,"invalid epoch and absent owner cannot change old observation");
        ProcessingTiming1947.Trace other=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.enter(other);
        ProcessingTiming1947.residentCopy1985(photo,8,2,1,true,38);
        check(render(photo).contains("保持中: Strongから連結（コピー開始済み）")&&photo.residentCopyEpoch1985==38,"explicit originating photo owns observation despite another current trace");
        check(other.residentCopyReason1985== -1&&!ProcessingTiming1947.summary().contains("コピー枠判定"),"newest unrelated photo cannot inherit delayed owner details");
        ProcessingTiming1947.residentExit1983(other,0,6,21,0,0,0,0,0,0);
        check(render(other).contains("連結検証の判定: 検証画像コピーの開始が未成立")&&other.residentQueueReason1983==21,"new begin-required refusal has an additive stable queue code");
        ProcessingTiming1947.residentExit1983(other,0,6,22,0,0,0,0,0,0);
        check(other.residentQueueReason1983==21,"first unknown queue code remains rejected");
    }
    static void optionalFailureAndBoundedProvider()throws Exception{
        reset();ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.finish(photo,true);
        String saved=ProcessingTiming1947.summary();GpuQualification1961.status="CPU/GPU再判定（現在）: kept-live-queue";
        GpuQualification1961.throwAttempts1985=true;String live=ProcessingTiming1947.liveSummary1979();
        check(live.startsWith(saved)&&live.contains("kept-live-queue")&&GpuQualification1961.attemptCalls1984==0,"optional missing new provider cannot remove saved photo or live queue");
        GpuQualification1961.throwAttempts1985=false;StringBuilder huge=new StringBuilder();for(int i=0;i<6100;i++)huge.append('x');huge.append("PRIVATE_EXCESS_TAIL");
        GpuQualification1961.attempts1985=huge.toString();live=ProcessingTiming1947.liveSummary1979();
        check(!live.contains("PRIVATE_EXCESS_TAIL")&&live.startsWith(saved),"new optional ledger keeps existing 6000-character bound");
        GpuQualification1961.attempts1985="検証中 / 終了時に再確認";
        check(ProcessingTiming1947.liveSummary1979().contains("検証中 / 終了時に再確認"),"active ledger remains visibly provisional");
        check(saved.equals(ProcessingTiming1947.summary()),"all optional provider failures and later progress leave stored photo unchanged");
    }
    public static void main(String[] args)throws Exception{
        exactCopyDecisionAndSavedOwner();allReasonsFamiliesAndUnknowns();invalidAndExplicitOwner();optionalFailureAndBoundedProvider();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"timing_terminal_ledger_isolation1985\":true,\"timing_copy_decision_snapshot1985\":true,\"timing_copy_fields_scalar_only1985\":true,\"timing_new_provider_failure_noninterference1985\":true,\"timing_copy_begin_reason1985\":true}");
    }
}
