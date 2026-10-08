package com.hiro.ulike;

import android.content.Context;
import android.os.Build;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;

/** Actual production foreground/background wrappers and resident Stage adoption.
 * Controlled host queue/certificate identity; real JNI and real Mesa dispatches.
 * Timing results are deliberately not used as Android performance evidence. */
public final class AnalysisBackground1961Test {
    private static long assertions;
    private static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static Object field(Class<?> type,Object owner,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private static long[] facts()throws Exception{Method m=Class.forName("com.hiro.ulike.Batch1961Test").getDeclaredMethod("facts");m.setAccessible(true);return (long[])m.invoke(null);}
    private static void quiet()throws Exception{Field f=GpuQualification1961.class.getDeclaredField("lastCapture");f.setAccessible(true);Object lock=field(GpuQualification1961.class,null,"LOCK");synchronized(lock){f.setLong(null,System.nanoTime()-3000000000L);}GpuQualification1961.wake();}
    private static void drained()throws Exception{long until=System.nanoTime()+5000000000L;while(GpuQualification1961.retainedBytes()!=0&&System.nanoTime()<until)Thread.sleep(20);check(GpuQualification1961.retainedBytes()==0,"detached analysis snapshot released");}
    private static boolean rejected(String key)throws Exception{Map<?,?> m=(Map<?,?>)field(GpuAnalysis1961.class,null,"REJECTED");synchronized(m){return m.containsKey(key);}}
    public static void main(String[] args)throws Exception{
        check(GpuNoise1960.available()&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961),"actual analysis compute available");
        Build.FINGERPRINT="analysis-host-qualification-fixture";GpuQualification1961.initialize(new Context());
        SaveQueue1935.busy1953=true;
        int w=257,h=259;int[] image=Analysis1961Oracle.image(w,h,1);
        SpatialNoise1934 reference=SpatialNoise1934.probeCpu1961(Analysis1961Oracle.spatial(image,w),w,h);
        long before=facts()[3];SpatialNoise1934 output=SpatialNoise1934.probe(Analysis1961Oracle.spatial(image,w),w,h);
        check(SpatialNoise1934.same1961(reference,output),"unqualified foreground publishes original CPU output");
        check(facts()[3]==before,"unqualified foreground performs no compute dispatch");
        check(GpuQualification1961.retainedBytes()>0,"bounded immutable patch snapshot queued");
        Arrays.fill(image,0); // The caller's mutable source is no longer the proof input.
        SaveQueue1935.busy1953=false;quiet();drained();
        check(facts()[3]>before,"detached idle proof executed actual production shader graph");
        check(!GpuNoise1960.sessionBusy(),"background proof returns GPU ownership");
        String key="spatial1961:v2:"+w+":"+h;
        check(rejected(key)||GpuQualification1961.restore(key)!=null,"complete speed/exactness gate reaches a verdict");
        GpuNoise1960.trimIdle();

        int bw=129,bh=131;int[] busyImage=Analysis1961Oracle.image(bw,bh,2);String busyKey="spatial1961:v2:"+bw+":"+bh;
        GpuNoise1960.Session held=GpuNoise1960.open();check(held!=null,"independent active stage held");
        try{before=facts()[3];output=SpatialNoise1934.probe(Analysis1961Oracle.spatial(busyImage,bw),bw,bh);check(SpatialNoise1934.same1961(output,SpatialNoise1934.probeCpu1961(Analysis1961Oracle.spatial(busyImage,bw),bw,bh)),"busy foreground CPU fallback exact");check(facts()[3]==before&&GpuQualification1961.retainedBytes()==0,"busy stage skips qualification work");check(!rejected(busyKey),"temporary context collision does not poison key");}
        finally{held.close();}
        SaveQueue1935.busy1953=true;SpatialNoise1934.probe(Analysis1961Oracle.spatial(busyImage,bw),bw,bh);check(GpuQualification1961.retainedBytes()>0,"formerly busy key can queue a later proof");
        GpuQualification1961.captureChanged();drained();check(!rejected(busyKey),"cancelled private snapshot does not reject configuration");SaveQueue1935.busy1953=false;

        int rw=65,rh=67;int[] residentImage=Analysis1961Oracle.image(rw,rh,1);
        StrongNoise1958.Model cpu=StrongNoise1958.prepareJava(Analysis1961Oracle.strong(residentImage,rw),rw,rh,4,true);
        StrongNoise1958.Model resident=GpuAnalysis1961.residentCandidate1961(Analysis1961Oracle.half(residentImage,rw,rh),rw,rh,4,true,(float[])Analysis1961Oracle.field(cpu,"evidence"),(float[])Analysis1961Oracle.field(cpu,"runtimeEvidence"),0);
        check(StrongNoise1958.sameModel1961(cpu,resident),"resident maps/evidence/model are exact original CPU values");
        GpuAnalysis1961.Resident lease=StrongNoise1958.takeResident1961(resident);check(lease!=null&&lease.setupNanos>0,"actual model owns measured resident setup");
        StrongNoise1958.attachResident1961(resident,lease);StrongNoise1958.Model model=resident;
        try{
            GpuStrong1960.beginStage(model);Object stage=field(GpuStrong1960.class,null,"active");check(stage!=null,"actual Strong stage created");
            Method initialize=GpuStrong1960.class.getDeclaredMethod("initialize",stage.getClass(),long.class);initialize.setAccessible(true);before=facts()[3];
            check(((Boolean)initialize.invoke(null,stage,0L)).booleanValue(),"actual Strong initialization adopts resident owner");
            check(field(stage.getClass(),stage,"session")==lease.session&&StrongNoise1958.takeResident1961(model)==null,"session ownership transfers exactly once");
            check(((Long)field(stage.getClass(),stage,"setup")).longValue()>=lease.setupNanos,"model setup cost included in Strong stage timing");
            check(facts()[3]==before,"resident Stage adoption does not repeat preparation dispatches");
        }finally{GpuStrong1960.endStage(model);StrongNoise1958.discardResident1961(model);}
        GpuNoise1960.trimIdle();check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.retainedBytes()==0,"known-complete model/stage resources released");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actualBackgroundDispatches\":true,\"unqualifiedForegroundCpuOnly\":true,\"detachedMutableSource\":true,\"transientBusyRetry\":true,\"residentStageAdoption\":true,\"physicalAndroidTested\":false}");
    }
}
