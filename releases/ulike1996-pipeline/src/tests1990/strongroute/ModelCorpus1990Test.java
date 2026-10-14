package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;

/** Real production CPU model and real production resident JNI graph. */
public final class ModelCorpus1990Test {
    static int assertions,cases;static long words;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    static String difference(StrongNoise1958.Model a,StrongNoise1958.Model b)throws Exception{
        for(String name:new String[]{"halfMap","quarterMap","eighthMap"}){
            int[] x=(int[])field(a,name),y=(int[])field(b,name);
            if(x.length!=y.length)return name+" lengths";
            for(int i=0;i<x.length;i++)if(x[i]!=y[i])return name+" index="+i+" cpu="+Integer.toHexString(x[i])+" gpu="+Integer.toHexString(y[i]);
        }
        for(String name:new String[]{"evidence","runtimeEvidence"}){
            float[] x=(float[])field(a,name),y=(float[])field(b,name);
            if(x.length!=y.length)return name+" lengths";
            for(int i=0;i<x.length;i++)if(Float.floatToRawIntBits(x[i])!=Float.floatToRawIntBits(y[i]))return name+" index="+i+" cpu="+Integer.toHexString(Float.floatToRawIntBits(x[i]))+" gpu="+Integer.toHexString(Float.floatToRawIntBits(y[i]));
        }
        return "model scalar";
    }
    public static void main(String[] args)throws Exception{
        boolean nativeCpu=StrongNoise1958.nativeAvailable();
        check(nativeCpu==(args.length>0&&"native".equals(args[0])),"requested CPU backend active");
        for(int id=0;id<GpuModelDefault1990.CASES;id++){
            GpuModelDefault1990.Fixture f=GpuModelDefault1990.fixture(id);
            check(GpuModelDefault1990.owns(f.half,f.width,f.height,f.noise,f.evidence,f.runtime),"fixture domain "+id);
            StrongNoise1958.Model cpu=StrongNoise1958.preparedSnapshotCpu1961(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime);
            for(int variant=0;variant<3;variant++){
                StrongNoise1958.Model gpu=GpuAnalysis1961.residentCandidateExact1990(f.half,f.width,f.height,f.noise,f.shadows,f.evidence,f.runtime,variant);
                try{
                    check(gpu!=null,"model available case="+id+" variant="+variant);
                    check(StrongNoise1958.sameModel1961(cpu,gpu),"exact model case="+id+" variant="+variant+" "+difference(cpu,gpu));
                    GpuAnalysis1961.Resident owner=StrongNoise1958.takeResident1961(gpu);
                    check(owner!=null&&!owner.closed&&owner.session!=null,"live resident model owner transfers once");
                    check(StrongNoise1958.takeResident1961(gpu)==null,"resident owner cannot be taken twice");
                    StrongNoise1958.attachResident1961(gpu,owner);
                    for(int[] map:StrongNoise1958.gpuMaps1960(gpu))words+=map.length;
                    words+=64+StrongNoise1958.gpuEvidence1960(gpu).length;cases++;
                    if(id==0&&variant==0){int[] map=StrongNoise1958.gpuMaps1960(gpu)[0];map[0]^=1;check(!StrongNoise1958.sameModel1961(cpu,gpu),"one wrong model bit rejected");}
                }finally{if(gpu!=null)StrongNoise1958.discardResident1961(gpu);}
                check(!GpuNoise1960.sessionBusy(),"resident session released after comparison");
            }
        }
        GpuModelDefault1990.Fixture f=GpuModelDefault1990.fixture(0);f.runtime[16]=Float.NaN;
        check(!GpuModelDefault1990.owns(f.half,f.width,f.height,f.noise,f.evidence,f.runtime),"nonfinite runtime evidence rejected");
        System.out.println("ENV "+GpuNoise1960.fingerprint());
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"model_cases\":"+cases+",\"compared_words\":"+words+",\"actual_cpu_jni\":"+nativeCpu+",\"actual_jni\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
