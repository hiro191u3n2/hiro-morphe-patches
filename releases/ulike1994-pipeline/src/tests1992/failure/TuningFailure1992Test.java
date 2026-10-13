package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;

/** Actual tuner and qualification queue; controlled parallel comparison samples. */
public final class TuningFailure1992Test {
    static int assertions;
    static void check(boolean pass,String why){assertions++;if(!pass)throw new AssertionError(why);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static Object invoke(String n,Class<?>[] t,Object...args)throws Exception{Method m=GpuQualification1961.class.getDeclaredMethod(n,t);m.setAccessible(true);return m.invoke(null,args);}
    static Object failure(String key)throws Exception{
        String env=(String)invoke("environment",new Class<?>[0]);
        String name=(String)invoke("recordKey",new Class<?>[]{String.class,String.class},key,env);
        return ((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).get(name);
    }
    static void scenario(int tag,long cpu,long gpu,int outcome,String label)throws Exception{
        SaveQueue1935.idle=false;ProcessingTiming1947.epoch++;GpuQualification1961.captureChanged();
        int[] u=new int[32];u[0]=u[1]=u[3]=u[5]=u[7]=8;u[6]=tag;u[8]=4;u[9]=u[11]=u[12]=1;u[10]=3;u[18]=tag;
        String selected=GpuStrong1960.profileKey1973(u,4);
        String first=GpuStrong1960.profileKey1973(u,0);
        String group="strong-gx1978-tuning-policy-bank-v1:"+first.substring(first.indexOf(':')+1);
        String cpuKey=group.replace("strong-gx1978-tuning-","strong-gx1978-cpu-tuning-")+":2";
        for(int p=0;p<8;p++)if(p!=4)GpuQualification1961.rejectExact1971(GpuStrong1960.profileKey1973(u,p),"argb_mismatch");
        GpuQualification1961.qualifiedStrongPreferred1970(selected,1000,1500,0);
        check(GpuQualification1961.restore(selected)!=null,"independent exact direct child seed exists");
        GpuStrongRouting1978.comparison1992=new long[]{cpu,gpu};
        int calls=GpuStrongRouting1978.cpuCalls1984;
        GpuStrongTuning1975.schedule1978(new int[64],new int[128],u,new StrongNoise1958.Model(),new GpuPolicy1960.Protection(),2);
        check(GpuQualification1961.retainedBytes()>0,"real tuner proof was queued");
        SaveQueue1935.idle=true;field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();
        long deadline=System.nanoTime()+5000000000L;
        while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>deadline)throw new AssertionError("tuner did not drain");Thread.sleep(5);}
        check(GpuStrongRouting1978.cpuCalls1984==calls+1,"completed exact child reached one real CPU comparison checkpoint");
        Object f=failure(cpuKey);check(f!=null,"CPU group stores classified failure");
        check(field(f.getClass(),"outcome1991").getInt(f)==outcome,"positive/unknown samples have distinct classifications");
        check(!GpuQualification1961.exactRejected(cpuKey),"speed and incomplete samples never become exact failure");
        check(GpuQualification1961.restore(selected)!=null&&!GpuQualification1961.exactRejected(selected),"CPU timing failure preserves independently exact child");
        check(GpuStrongTuning1975.selectCpu1978(u,2)==null,"failure does not certify aggregate speed");
        if(outcome==11){
            check(field(f.getClass(),"measuredCpu1992").getLong(f)==cpu&&field(f.getClass(),"measuredGpu1992").getLong(f)==gpu,"reported speed uses observed comparison pair");
            check("strong_cpu_parallel_4".equals(field(f.getClass(),"measuredCandidate1992").get(f)),"reported comparison names correct child");
        }else check(field(f.getClass(),"measuredCpu1992").getLong(f)==0&&field(f.getClass(),"measuredGpu1992").getLong(f)==0,"incomplete pair has no measured-speed record");
        String[] lines=GpuQualification1961.attemptSummary1984().split("\n");String last=lines[lines.length-2];
        check(last.contains(label),"terminal display matches classification: "+last);
        if(outcome!=11)check(!last.contains("速度条件を満たさず"),"unknown timing never displays measured speed failure");
        check(GpuNoise1960.sessions.get()==0,"tuner leaves no active session");
    }
    public static void main(String[] args)throws Exception{
        GpuQualification1961.initialize(new Context());
        scenario(201,100,200,11,"速度条件を満たさず");
        scenario(202,0,200,14,"比較完了前に終了");
        scenario(203,100,0,14,"比較完了前に終了");
        scenario(204,-1,200,14,"比較完了前に終了");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"comparison_cases\":4}");
    }
}
