package com.hiro.ulike;

import android.content.Context;

/** Production startup coordinator, observable peer calls, no GPU performance claim. */
public final class QuietStartup19120Test {
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        String scenario=args[0];boolean cached=scenario.startsWith("cached"),ui=scenario.equals("ui");
        GpuStrongDefault1990.passed=cached;GpuModelDefault1990.passed=cached;GpuFinishDefault1990.passed=cached;
        GpuStrongDefault1990.Adaptive1994.saved=scenario.equals("cached_adaptive");
        GpuNoise1960.available=!scenario.equals("unavailable");GpuNoise1960.busy=ui;
        GpuStartup1990.request();check(GpuStartup1990.state()==0,"uninitialized request is inert");
        Context context=new Context();
        if(scenario.equals("rejected")){
            GpuCertificates1990.initialize(context);GpuCertificates1990.reject(GpuStrongDefault1990.SUITE,"prior pixel mismatch");
        }
        CameraSession1965.track(new Object(),context);
        if(ui){
            android.os.Looper.ui=true;long started=System.nanoTime();
            check(!GpuStartup1990.awaitPreparation(2000)&&!GpuStartup1990.awaitCoreReady(2000),"UI never waits on shared GPU ownership");
            check(System.nanoTime()-started<500000000L,"UI startup wait is nonblocking");
            android.os.Looper.ui=false;GpuNoise1960.busy=false;
        }
        for(int i=0;i<20;i++)GpuStartup1990.initialize(context);
        boolean ready=GpuStartup1990.awaitPreparation(2000);GpuStartup1990.Snapshot end=GpuStartup1990.snapshot();
        check(end.state!=0&&end.state!=1&&end.startedNanos>0&&end.elapsedNanos>0,"startup reaches a finite terminal state without new proof");
        check(GpuStrongDefault1990.calls==0&&GpuModelDefault1990.calls==0&&GpuFinishDefault1990.calls==0&&GpuFinishDefault1990.wideCalls==0,"startup performs zero corpus comparisons even on cache miss");
        check(GpuStrongDefault1990.optional==0&&GpuModelDefault1990.optional==0&&GpuFinishDefault1990.optional==0&&GpuQualification1961.preFirstCalls==0,"startup does not arm optional or pre-first-shot validation");
        check(!GpuQualification1961.paused&&GpuQualification1961.pauses==1&&GpuQualification1961.resumes==1,"temporary startup ownership is released once");
        if(cached){
            check(ready&&end.domainReady&&end.modelDomainReady&&end.finishDomainReady,"independent cached domains restore readiness");
            check(GpuNoise1960.calls==(scenario.equals("cached_adaptive")?16:15),"only cached-program families are prepared");
        }else{
            check(!ready&&!end.domainReady&&!end.modelDomainReady&&!end.finishDomainReady,"cold or unavailable startup cannot invent GPU admission");
            check(GpuNoise1960.calls==0&&end.preparedPrograms==0,"no standard program sweep without saved evidence");
        }
        if(scenario.equals("unavailable"))check(end.state==GpuStartup1990.UNAVAILABLE,"hardware unavailable remains fail closed");
        else check(GpuStrongDefault1990.restores==1&&GpuModelDefault1990.restores==1&&GpuFinishDefault1990.restores==1,"cached-only restoration runs once per independent domain");
        if(scenario.equals("rejected"))check(GpuCertificates1990.rejected(GpuStrongDefault1990.SUITE),"existing rejection survives startup");
        int programs=GpuNoise1960.calls;GpuStartup1990.request();GpuStartup1990.initialize(context);
        check(programs==GpuNoise1960.calls&&GpuQualification1961.pauses==1,"repeat initialize does not duplicate preparation");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenario\":\""+scenario+"\"}");
    }
}
