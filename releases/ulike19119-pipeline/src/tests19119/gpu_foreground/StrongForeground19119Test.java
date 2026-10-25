package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Real foreground begin/process/end, native CPU and both actual JNI engines.
 * Test-only in-memory certificates bypass device timing admission to exercise
 * the selected route. They are not published, and provide no speed evidence. */
public final class StrongForeground19119Test {
    static int assertions,cases;static long pixels;
    static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    static void set(Class<?> owner,String name,Object value)throws Exception{Field f=owner.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static Object active()throws Exception{Field f=GpuStrong1960.class.getDeclaredField("active");f.setAccessible(true);return f.get(null);}
    static void seed(String suite){check(GpuCertificates1990.passed(suite,70,100000,1000000,500000),"test-only certificate adapter "+suite);}
    static void one(int id,boolean optimized)throws Exception{
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(id);
        int[] wantCf=StrongTransport1995Test.confidence(f),want=StrongTransport1995Test.expected(f,wantCf),original=f.source.clone();
        // Exercise the actual foreground handoff: old model maps already own
        // the baseline context. Candidate must release that owner before it
        // can reserve/open its independent context and upload the same maps.
        if(optimized){
            GpuNoise1960.Session carried=GpuNoise1960.open();check(carried!=null,"actual baseline carried session");
            float[] evidence=StrongNoise1958.gpuEvidence1960(f.model);int[][] maps=StrongNoise1958.gpuMaps1960(f.model);
            GpuNoise1960.Lease1971 lease=carried.reserveCapacity1971(new int[]{2,4,5,6},new long[]{4L*evidence.length,4L*maps[0].length,4L*maps[1].length,4L*maps[2].length},0);
            check(lease!=null,"carried model lease");
            try{check(carried.run(new GpuNoise1960.Batch().upload(2,evidence).upload(4,maps[0]).upload(5,maps[1]).upload(6,maps[2])),"real old model upload");}finally{lease.close();}
            StrongNoise1958.attachResident1961(f.model,new GpuAnalysis1961.Resident(carried,1L));
        }
        GpuStrong1960.beginStage(f.model);Object stage=active();check(stage!=null,"real beginStage active");
        GpuStrong1960.configureWorkers1978(f.model,2);
        try{
            for(int repeat=0;repeat<2;repeat++){
                int[] actual=StrongTransport1995Test.output(f),cf=StrongTransport1995Test.confidence(f);if(cf!=null)Arrays.fill(cf,0x12345678);
                check(StrongTransport1995Test.run(f,actual,cf),"actual process1981 returns GPU");
                check(Arrays.equals(want,actual),"all ARGB + halo + tail exact width "+f.u[0]);
                check(Arrays.equals(wantCf,cf),"confidence exact");check(Arrays.equals(original,f.source),"source unchanged");
                check(((Boolean)StrongTransport1995Test.field(stage,"optimized19119"))==optimized,"selected backend identity");
                GpuNoise1960.Session session=(GpuNoise1960.Session)StrongTransport1995Test.field(stage,"session");
                check(session!=null&&session.optimized19119()==optimized,"actual native session engine");cases++;pixels+=(long)f.u[0]*(f.u[3]-f.u[2]);
            }
            check(((Integer)StrongTransport1995Test.field(stage,"directStrips1995"))==2,"two actual direct commits");
            check(((Integer)StrongTransport1995Test.field(stage,"optimizedStrips19119"))==(optimized?2:0),"only candidate commits counted");
            check(((Long)StrongTransport1995Test.field(stage,"optimizedPixels19119"))==(optimized?2L*f.u[0]*(f.u[3]-f.u[2]):0L),"candidate pixel count");
        }finally{GpuStrong1960.endStage(f.model);}
        check(active()==null,"real endStage clears active");check(!GpuNoise1960.sessionBusy(),"both native session owners released");
        check(GpuNoise1960.reservedBytes1971()==0,"all leases released");check(GpuStrongDefault1990.Adaptive1994.faster(),"baseline certificate remains usable");
    }
    public static void main(String[] args)throws Exception{
        StrongTransport1995Test.store();check(StrongNoise1958.nativeAvailable(),"real CPU JNI");
        check(GpuStrongDefault1990.qualify(StrongTransport1995Test.GO),"real production baseline startup corpus");
        check(GpuNoise1960.supports(GpuStrong1960.program1973(7,0)),"actual baseline tile8 shader");
        seed(GpuStrongDefault1990.Adaptive1994.EXACT);seed(GpuStrongDefault1990.Adaptive1994.FAST);
        // Readiness merely exposes the already completed real standard startup
        // corpus to beginStage, without launching Android application startup.
        set(GpuStartup1990.class,"coreReady",true);set(GpuStartup1990.class,"sharedReleased1991",true);
        check(GpuNoiseOptimized19119.prepare(),"actual candidate shader prepare");
        check(!GpuStrongOptimized19119.ready(3060,4080),"old proof cannot authorize candidate");
        one(64,false);Map<String,String> baseline=new HashMap<String,String>(StrongTransport1995Test.preferences);
        set(GpuStrongOptimized19119.class,"exactKey","host19119-route-only-exact");set(GpuStrongOptimized19119.class,"fastKey","host19119-route-only-speed");
        seed("host19119-route-only-exact");seed("host19119-route-only-speed");
        check(GpuStrongOptimized19119.ready(3060,4080),"test-only candidate admission ready");
        one(64,true);one(65,true);
        StrongTransport1995Test.preferences.clear();StrongTransport1995Test.preferences.putAll(baseline);
        check(!GpuStrongOptimized19119.ready(3060,4080),"removed independent proof immediately refuses candidate");one(65,false);
        GpuNoise1960.trimIdle();check(GpuNoise1960.reservedBytes1971()==0,"trim remains lease free");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"direct_cases\":"+cases+",\"candidate_cases\":4,\"compared_pixels\":"+pixels+",\"actual_foreground_begin_process_end\":true,\"carried_baseline_transition_exercised\":true,\"actual_jni_gles\":true,\"actual_native_cpu\":true,\"test_only_certificate_preseed\":true,\"negative_controls\":2,\"device_speedup_verified\":false,\"physical_android_tested\":false}");
    }
}
