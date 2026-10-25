package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.Field;
public final class Startup1990Test {
    static int assertions;
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static void certificates(){
        Context context=new Context();GpuCertificates1990.initialize(context);
        check(GpuCertificates1990.restore("strong-suite") == null,"empty storage supplies no proof");
        check(!GpuCertificates1990.passed("strong-suite",0,100,1,2),"empty corpus rejected");
        check(!GpuCertificates1990.passed("strong-suite",8,0,1,2),"uncompared corpus rejected");
        check(GpuCertificates1990.passed("strong-suite",8,10000,300,400),"quality evidence not conflated with tiny-suite speed");
        GpuCertificates1990.Evidence e=GpuCertificates1990.restore("strong-suite");
        check(e!=null&&e.cases==8&&e.pixels==10000&&e.cpuNanos==300&&e.gpuNanos==400,"finite counts and timings restore exactly");
        check(GpuCertificates1990.restore("other-suite")==null,"different corpus revision requires own evidence");
        GpuQualification1961.environment="changed-driver";
        check(GpuCertificates1990.restore("strong-suite")==null,"changed OS/kernel/driver identity invalidates proof");
        GpuQualification1961.environment="device-os-app-kernel-driver-A";
        check(GpuCertificates1990.restore("strong-suite")!=null,"unchanged environment retains own proof");
        Thread.currentThread().interrupt();
        try{check(!GpuCertificates1990.passed("interrupted",8,100,1,1),"interrupted proof does not persist");}
        finally{Thread.interrupted();}
        check(GpuCertificates1990.restore("interrupted")==null,"cancellation has no certificate");
        GpuCertificates1990.reject("strong-suite","confidence mismatch");
        check(GpuCertificates1990.rejected("strong-suite"),"actual mismatch sticky");
        check(GpuCertificates1990.restore("strong-suite")==null,"mismatch removes positive evidence");
        check(!GpuCertificates1990.passed("strong-suite",80,100000,1,1),"later easy corpus cannot overwrite known mismatch");
        check(GpuCertificates1990.passed("corrected-suite-v2",80,100000,1,1),"versioned corrected algorithm may obtain new evidence");
        check(GpuCertificates1990.rejected("strong-suite"),"old rejected suite not erased by new version");
        for(String key:context.prefs.data.keySet().toArray(new String[0]))if(key.startsWith("pass-"))context.prefs.data.put(key,"corrupt");
        check(GpuCertificates1990.restore("corrected-suite-v2")==null,"corrupt proof cannot admit GPU");
    }
    static void startup(String scenario)throws Exception{
        if(scenario.equals("core")||scenario.equals("startup_cancel")||scenario.equals("ui"))GpuNoise1960.holdAux=true;
        if(scenario.equals("retry"))GpuStrongDefault1990.failFirst=true;
        if(scenario.equals("reject"))GpuStrongDefault1990.reject=true;
        boolean cached=scenario.startsWith("cached_");
        if(cached){GpuStrongDefault1990.passed=true;GpuModelDefault1990.passed=true;GpuFinishDefault1990.passed=true;GpuNoise1960.busy=true;}

        if(scenario.equals("partial"))GpuStrongDefault1990.result=false;
        if(scenario.equals("unavailable"))GpuNoise1960.available=false;
        if(scenario.equals("unsupported"))GpuNoise1960.unsupported=true;
        check(GpuStartup1990.snapshot().state==0,"starts uninitialized");
        GpuStartup1990.request();check(GpuStartup1990.state()==0,"request before application initialization is inert");
        Context context=new Context();
        if(scenario.equals("camera")){
            CameraSession1965.track(new Object(),context);
            check(GpuQualification1961.initCalls==1&&CameraTrace1965.initCalls==1,"camera track initializes GPU identity before startup");
            check(GpuStartup1990.startedNanos()>0&&GpuStartup1990.startedNanos()<=CameraTrace1965.firstEvent,"GPU preparation begins during capture registration before preview/shutter events");
        }else GpuStartup1990.initialize(context);
        GpuStartup1990.Snapshot initial=GpuStartup1990.snapshot();
        check(initial.startedNanos>0,"startup timing starts before any capture exists");
        for(int i=0;i<20;i++)GpuStartup1990.initialize(context);
        check(GpuQualification1961.preFirstCalls==1&&GpuQualification1961.preFirstAt>0&&
                GpuQualification1961.preFirstAt<=initial.startedNanos,
                "one pre-first quiet window begins at application initialization, before GPU startup");
        if(scenario.equals("startup_cancel")){
            check(GpuQualification1961.preFirstEnabled,"optional synthetic proof offered before first shutter");
            GpuQualification1961.captureChanged();
            check(!GpuQualification1961.preFirstEnabled,"shutter closes the unfinished proof window during startup");
            GpuNoise1960.aux.countDown();
        }
        if(scenario.equals("core")){
            check(!GpuStartup1990.awaitCoreReady(30),"core wait cannot pass while auxiliary setup may own the shared GPU");
            GpuStartup1990.Snapshot core=GpuStartup1990.snapshot();
            check(!core.domainReady&&core.state==1&&core.readyNanos==0&&core.preparedPrograms==1,"validated Strong alone is not published before startup shared-session fence");
            GpuNoise1960.aux.countDown();
            check(GpuStartup1990.awaitCoreReady(1000),"readiness published after all shared startup GPU work finishes");
        }
        if(cached){
            check(!GpuStartup1990.awaitCoreReady(20),"restored certificates cannot skip a still-owned shared GPU token");
            if(scenario.equals("cached_timeout")){
                Field started=GpuStartup1990.class.getDeclaredField("started");started.setAccessible(true);started.setLong(null,System.nanoTime()-31000000000L);
            }else GpuNoise1960.busy=false;
        }
        if(scenario.equals("ui")){
            android.os.Looper.ui=true;
            long uiBegin=System.nanoTime();
            check(!GpuStartup1990.awaitCoreReady(2000),"main looper cannot wait at downstream Strong/model entry");
            check(!GpuStartup1990.awaitPreparation(2000),"main looper cannot wait at public preparation entry");
            check(System.nanoTime()-uiBegin<500000000L,"main looper returns while optional startup worker remains withheld");
            android.os.Looper.ui=false;GpuNoise1960.aux.countDown();
        }
        boolean ready=GpuStartup1990.awaitPreparation(2000);
        GpuStartup1990.Snapshot finished=GpuStartup1990.snapshot();
        check(finished.elapsedNanos>0&&(scenario.equals("cached_timeout")||finished.startedNanos==initial.startedNanos),"single preparation interval preserved");
        if(scenario.equals("unavailable")){
            check(!ready&&finished.state==4&&!finished.domainReady,"unavailable hardware remains fail closed");
            check(GpuStrongDefault1990.calls==0&&GpuNoise1960.calls==0,"no corpus dispatch without actual environment");
        }else{
            boolean expected=!scenario.equals("partial")&&!scenario.equals("reject")&&!scenario.equals("cached_timeout");
            check(ready==expected&&finished.state==(expected?2:3)&&finished.domainReady==expected,"compilation not mistaken for quality admission");
            check(finished.preparedPrograms==(scenario.equals("unsupported")?15:16)&&finished.failedPrograms==(scenario.equals("unsupported")?1:0),"all standard programs have explicit supported result");
            check(GpuNoise1960.calls==16&&GpuStrongDefault1990.calls==(cached?0:(scenario.equals("partial")||scenario.equals("retry"))?2:1)&&GpuFinish1953.calls==1,"repeated init cannot duplicate setup or corpus");
            check((finished.readyNanos>0)==expected,"ready timestamp only recorded after finite corpus succeeds");
        }
        check(!GpuQualification1961.paused&&GpuQualification1961.pauses==1&&GpuQualification1961.resumes==1,"optional queue pause always released exactly once");
        check(GpuQualification1961.preFirstCalls==1,"startup completion and repeated initialization never reset the quiet timer");
        if(scenario.equals("startup_cancel"))check(GpuStrongDefault1990.requestCalls==1&&
                !GpuStrongDefault1990.preFirstAtRequest,"late startup cannot reopen optional tile8 after a shutter");
        if(scenario.equals("reject"))check(GpuCertificates1990.rejected(GpuStrongDefault1990.SUITE)&&GpuStrongDefault1990.calls==1,"actual mismatch permanently blocks retry within startup");
        if(!scenario.equals("unavailable"))check(finished.modelDomainReady&&finished.finishDomainReady&&GpuModelDefault1990.calls==(cached?0:1)&&GpuFinishDefault1990.calls==(cached?0:1),"model and finishing corpus statuses remain independent from Strong result");
        if(cached&&!scenario.equals("cached_timeout")){
            check(GpuFinishDefault1990.wideCalls==1&&GpuFinishDefault1990.wideReady1993(),"restored32 cannot skip independent wide recovery");
            check(finished.finishWideStatus1995.equals("128-ready"),"startup snapshot captures actual wide readiness");
            GpuFinishDefault1990.wide=false;
            check(GpuStartup1990.snapshot().finishWideStatus1995.equals("128-ready"),"historical startup snapshot does not rewrite itself from current wide state");
        }
        if(scenario.equals("cached_timeout"))check(GpuFinishDefault1990.wideCalls==0&&!GpuFinishDefault1990.wideReady1993(),"expired single startup deadline cannot dispatch wide recovery or revoke32");
        int calls=GpuNoise1960.calls;GpuStartup1990.request();check(GpuNoise1960.calls==calls,"completed preparation is idempotent");
        if(scenario.equals("core")){
            long historical=finished.readyNanos;GpuStartup1990.invalidateDomain();
            GpuStartup1990.Snapshot revoked=GpuStartup1990.snapshot();
            check(!revoked.domainReady&&revoked.state==3&&revoked.readyNanos==historical,"revocation clears current readiness and preserves real startup time");
            check(!GpuStartup1990.awaitCoreReady(0),"revoked route not readmitted by wait");
        }

        for(Field field:GpuStartup1990.class.getDeclaredFields())check(!Context.class.isAssignableFrom(field.getType()),"scheduler retains no Activity/context");
    }
    public static void main(String[] args)throws Exception{
        if(args[0].equals("certificates"))certificates();else startup(args[0]);
        System.out.println("{\"scenario\":\""+args[0]+"\",\"assertions\":"+assertions+",\"status\":\"passed\"}");
    }
}
