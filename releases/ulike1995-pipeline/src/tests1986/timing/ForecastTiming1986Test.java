package com.hiro.ulike;

import java.lang.reflect.Field;

/** Render the actual observed forecast scalars without recomputing the routing
 * decision. Later qualification and forecasts cannot rewrite saved metrics. */
public final class ForecastTiming1986Test {
    static final long MS=1000000L;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void reset()throws Exception{
        QueueTiming1985Test.reset();GpuQualification1961.attempts1986=null;
        GpuQualification1961.attemptCalls1986=0;GpuQualification1961.throwAttempts1986=false;
    }
    static String render(ProcessingTiming1947.Trace trace)throws Exception{return Timing1982Test.render(trace);}
    static void sameLookupAndSavedPhoto()throws Exception{
        reset();Timing1982Test.MemoryPreferences preferences=new Timing1982Test.MemoryPreferences();
        ProcessingTiming1947.init(new Timing1982Test.MemoryContext(preferences));
        ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());
        check(!render(photo).contains("GPU所要時間の直近予測"),"absence of a real forecast is explicitly absent");
        ProcessingTiming1947.noiseBackend(photo,4,12);
        ProcessingTiming1947.strongGpuWork1973(photo,2327*MS,2604*MS,90*MS,0);
        ProcessingTiming1947.recordInterval(photo,ProcessingTiming1947.NOISE,0,1486*MS);
        int saves=preferences.saves,status=GpuQualification1961.statusCalls,attempts=GpuQualification1961.attemptCalls1986;
        long updated=photo.updatedMillis;
        ProcessingTiming1947.strongForecast1986(photo,4,3,6000*MS,100*MS,300*MS,350*MS);
        check(preferences.saves==saves&&GpuQualification1961.statusCalls==status&&GpuQualification1961.attemptCalls1986==attempts&&photo.updatedMillis==updated,"hot observer does not publish, persist, query other helpers or change the photo timestamp");
        String text=render(photo);
        check(text.contains("冷却終了後も実測を保持")&&text.contains("有効観測 3撮影")&&text.contains("最も古い有効観測 6000ms前"),"display captures sample count and oldest valid observation from the same lookup");
        check(text.contains("認定値 100ms")&&text.contains("保持実測の最大値 300ms")&&text.contains("この照会の予測 350ms"),"certified, observed max and final same-photo floor are distinct actual inputs");
        check(text.contains("撮影間の実測は2撮影から使用")&&text.contains("写真全体の実測時間ではありません"),"forecast is neither one-sample adoption nor a photograph timing measurement");
        check(photo.strongGpuStrips==4&&photo.strongCpuStrips==12&&photo.strongCpuOracleNanos==2327*MS&&photo.strongGpuRequestNanos==2604*MS&&photo.strongCpuVerifications==0,"new observer does not change GPU counts, cumulative work or CPU comparisons");
        ProcessingTiming1947.finish(photo,true);String saved=ProcessingTiming1947.summary();int writes=preferences.saves;
        check(saved.contains("この照会の予測 350ms")&&saved.equals(preferences.getString("summary","")),"ordinary photo save stores the last in-photo forecast snapshot");
        ProcessingTiming1947.strongForecast1986(photo,5,0,-1,1,0,1);
        GpuQualification1961.attempts1986="保存後の検証履歴\n仕上げ前提の直近検証 #8（終了時の記録）\n旧GPU仕上げ: 保存済み画質不一致により対象外\n新GPU仕上げ32行: 速度条件を満たさず / 比較基準: CPU / CPU 1000ms・旧GPU 未取得・候補GPU 960ms";
        String live=ProcessingTiming1947.liveSummary1979();
        check(live.startsWith(saved)&&live.contains("旧GPU仕上げ: 保存済み画質不一致により対象外")&&live.contains("候補GPU 960ms"),"later finish reason and times appear only after the exact saved photo text");
        check(GpuQualification1961.attemptCalls1986==1&&GpuQualification1961.attemptCalls1985==0&&GpuQualification1961.attemptCalls1984==0,"new provider is used directly when it supplies the new record");
        check(saved.equals(ProcessingTiming1947.summary())&&saved.equals(preferences.getString("summary",""))&&writes==preferences.saves,"late forecasts and opening live detail cannot persist a new photo result");
        check(photo.strongForecastState1986==4&&photo.strongForecastChosen1986==350*MS&&photo.strongGpuStrips==4&&photo.strongCpuStrips==12,"completed photo rejects late diagnostic mutation");
        reset();ProcessingTiming1947.init(new Timing1982Test.MemoryContext(preferences));
        check(saved.equals(ProcessingTiming1947.summary())&&GpuQualification1961.attemptCalls1986==0,"restart restores frozen photo and forecast without later proof inquiry");
    }
    static void branchLabelsAndUnobservedTimes()throws Exception{
        reset();String[] labels={"直近観測なし","観測が2撮影未満","保持した実測を使用","短い冷却中・実測は保持","冷却終了後も実測を保持","観測期限切れ・認定値で再測定"};
        int[] samples={0,1,2,2,3,0};
        for(int state=0;state<labels.length;state++){
            ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());
            long age=samples[state]==0?-1:200*MS,observed=samples[state]==0?0:300*MS,chosen=samples[state]<2?100*MS:300*MS;
            ProcessingTiming1947.strongForecast1986(photo,state,samples[state],age,100*MS,observed,chosen);String text=render(photo);
            check(text.contains("GPU所要時間の直近予測: "+labels[state]),"distinct bounded state label for actual lookup: "+state);
            check(text.contains("有効観測 "+samples[state]+"撮影")&&text.contains("この照会の予測 "+chosen/MS+"ms"),"observation count and selected time stay paired");
            if(samples[state]==0)check(text.contains("保持実測の最大値 未取得")&&!text.contains("最も古い有効観測"),"unobserved/expired time is not invented as zero-ms evidence");
            if(samples[state]==1)check(text.contains("保持実測の最大値 300ms")&&text.contains("この照会の予測 100ms"),"one observation can be shown without falsely claiming it supplied the forecast");
            check(photo.strongGpuStrips==0&&photo.strongCpuStrips==0&&photo.strongCpuVerifications==0,"state diagnosis never substitutes for actual GPU adoption");
        }
        ProcessingTiming1947.Trace tiny=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.strongForecast1986(tiny,2,2,0,1,2,2);
        check(render(tiny).contains("認定値 <1ms")&&render(tiny).contains("この照会の予測 <1ms"),"positive submillisecond forecast remains distinct from missing data");
        ProcessingTiming1947.strongForecast1986(tiny,4,3,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE);
        check(render(tiny).contains("最も古い有効観測 9223372036854ms前")&&tiny.strongForecastChosen1986==Long.MAX_VALUE,"maximum scalar ages and costs do not overflow rendering");
        check(GpuQualification1961.statusCalls==0&&GpuQualification1961.attemptCalls1986==0,"rendering stored scalar forecasts never performs a second decision lookup");
    }
    static void malformedInactiveAndExplicitOwner()throws Exception{
        reset();ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());
        ProcessingTiming1947.strongForecast1986(photo,3,2,400*MS,100*MS,200*MS,200*MS);
        long[][] invalid={{-1,2,1,100,200,200},{6,2,1,100,200,200},{3,-1,1,100,200,200},{3,4,1,100,200,200},
            {3,2,-2,100,200,200},{3,2,1,0,200,200},{3,2,1,-1,200,200},{3,2,1,100,-1,200},{3,2,1,100,200,0},{3,2,1,100,200,-1}};
        for(long[] v:invalid){ProcessingTiming1947.strongForecast1986(photo,(int)v[0],(int)v[1],v[2],v[3],v[4],v[5]);
            check(photo.strongForecastState1986==3&&photo.strongForecastSamples1986==2&&photo.strongForecastAge1986==400*MS&&photo.strongForecastCertified1986==100*MS&&photo.strongForecastObserved1986==200*MS&&photo.strongForecastChosen1986==200*MS,"malformed observation rejected atomically");}
        ProcessingTiming1947.strongForecast1986(null,0,0,-1,1,0,1);
        ProcessingTiming1947.Trace other=ProcessingTiming1947.begin(new Object());ProcessingTiming1947.enter(other);
        ProcessingTiming1947.strongForecast1986(photo,4,3,900*MS,100*MS,200*MS,250*MS);
        check(photo.strongForecastState1986==4&&photo.strongForecastChosen1986==250*MS&&other.strongForecastState1986== -1,"explicit original photo owns observation despite different current worker trace");
        check(!ProcessingTiming1947.summary().contains("GPU所要時間の直近予測"),"newer unrelated photo does not inherit delayed forecast");
        ProcessingTiming1947.finish(photo,false);String failed=render(photo);ProcessingTiming1947.strongForecast1986(photo,0,0,-1,1,0,1);
        check(failed.equals(render(photo))&&photo.state==2&&photo.strongForecastState1986==4,"failed terminal trace is as immutable as successful terminal trace");
        for(Field field:ProcessingTiming1947.Trace.class.getDeclaredFields())if(field.getName().endsWith("1986"))
            check(field.getType().isPrimitive(),"forecast history retains no condition strings, arrays or owner objects: "+field.getName());
    }
    static void optionalFailureAndBoundedDetail()throws Exception{
        reset();ProcessingTiming1947.Trace photo=ProcessingTiming1947.begin(new Object());
        GpuQualification1961.throwStatus=true;GpuQualification1961.throwAttempts1986=true;
        ProcessingTiming1947.strongForecast1986(photo,2,2,100*MS,100*MS,200*MS,200*MS);
        check(photo.strongForecastState1986==2&&photo.strongForecastChosen1986==200*MS&&GpuQualification1961.statusCalls==0&&GpuQualification1961.attemptCalls1986==0,"hot observer cannot be affected by unrelated broken optional diagnostics");
        GpuQualification1961.throwStatus=false;ProcessingTiming1947.finish(photo,true);String saved=ProcessingTiming1947.summary();
        GpuQualification1961.status="CPU/GPU再判定（現在）: preserved queue";String live=ProcessingTiming1947.liveSummary1979();
        check(live.startsWith(saved)&&live.contains("preserved queue")&&GpuQualification1961.attemptCalls1986==1&&GpuQualification1961.attemptCalls1985==0,"LinkageError in new summary provider preserves saved photo and live queue without pretending old data is new");
        GpuQualification1961.throwAttempts1986=false;StringBuilder huge=new StringBuilder();for(int i=0;i<6100;i++)huge.append('x');huge.append("UNBOUNDED_EXCESS");
        GpuQualification1961.attempts1986=huge.toString();live=ProcessingTiming1947.liveSummary1979();
        check(live.startsWith(saved)&&!live.contains("UNBOUNDED_EXCESS"),"new detailed provider retains existing 6000-character bound");
        GpuQualification1961.attempts1986="仕上げ前提の直近検証 #1（検証中・途中の記録）";
        check(ProcessingTiming1947.liveSummary1979().contains("（検証中・途中の記録）"),"live view preserves provisional wording instead of declaring completion");
        check(saved.equals(ProcessingTiming1947.summary()),"all provider failures and later progress preserve frozen photo summary");
    }
    public static void main(String[] args)throws Exception{
        sameLookupAndSavedPhoto();branchLabelsAndUnobservedTimes();malformedInactiveAndExplicitOwner();optionalFailureAndBoundedDetail();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"timing_forecast_same_lookup_scalars1986\":true,\"timing_forecast_scope_and_expiry1986\":true,\"timing_forecast_saved_owner_immutable1986\":true,\"timing_finish_live_detail_isolation1986\":true,\"timing_new_provider_failure_noninterference1986\":true,\"timing_forecast_scalar_only1986\":true}");
    }
}
