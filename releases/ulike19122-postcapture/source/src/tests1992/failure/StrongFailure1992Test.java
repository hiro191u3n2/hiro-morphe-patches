package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;

/** Real Proof and qualification classes; synthetic pixel transport only. */
public final class StrongFailure1992Test {
    static int assertions;
    static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void check(boolean pass,String why){assertions++;if(!pass)throw new AssertionError(why);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static Object invoke(Class<?> c,Object o,String n,Class<?>[] types,Object...args)throws Exception{Method m=c.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);}
    static Object failure(String key)throws Exception{
        String env=(String)invoke(GpuQualification1961.class,null,"environment",new Class<?>[0]);
        String name=(String)invoke(GpuQualification1961.class,null,"recordKey",new Class<?>[]{String.class,String.class},key,env);
        return ((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).get(name);
    }
    static void classified(String key,int outcome,boolean exact)throws Exception{
        Object f=failure(key);check(f!=null,"failure exists: "+key);
        check(exact?field(f.getClass(),"exact").getBoolean(f):field(f.getClass(),"outcome1991").getInt(f)==outcome,"typed failure "+key+" expected "+outcome);
        check(GpuQualification1961.exactRejected(key)==exact,"only actual disagreement is durable exact rejection: "+key);
        check(GpuQualification1961.restore(key)==null,"failed candidate is not admitted: "+key);
        if(outcome!=11)check(field(f.getClass(),"measuredCpu1992").getLong(f)==0&&field(f.getClass(),"measuredGpu1992").getLong(f)==0,"no fabricated measured speed: "+key);
        else check(field(f.getClass(),"measuredCpu1992").getLong(f)>0&&field(f.getClass(),"measuredGpu1992").getLong(f)>0,"speed classification retains actual durations: "+key);
    }
    static void proof(String key,int mode,int fault,int delay)throws Exception{
        GpuNoise1960.fault1992=fault;GpuNoise1960.delay1992=delay;
        int[] u=new int[32];u[0]=8;u[1]=u[3]=u[5]=8;u[7]=64;u[8]=4;u[10]=mode;u[12]=mode==3?1:0;
        int[] source=new int[64];for(int i=0;i<source.length;i++)source[i]=0xff000000+i;
        Class<?> c=Class.forName("com.hiro.ulike.GpuStrong1960$Proof");Constructor<?> ctor=c.getDeclaredConstructors()[0];ctor.setAccessible(true);
        GpuQualification1961.Probe p=(GpuQualification1961.Probe)ctor.newInstance(key,source,new int[128],new float[16],u,new StrongNoise1958.Model(),null);
        try{p.run(LIVE);}finally{p.close();}
        check(GpuNoise1960.active==0&&GpuNoise1960.leases==0&&GpuNoise1960.opens==GpuNoise1960.closes,"all proof sessions close: "+key);
    }
    public static void main(String[] ignored)throws Exception{
        GpuQualification1961.initialize(new Context());
        for(int mode:new int[]{0,3})for(int fault:new int[]{1,2,3,4,8}){
            String key="strong-transport-"+mode+"-"+fault;proof(key,mode,fault,0);classified(key,8,false);
        }
        proof("strong-confidence-missing",3,6,0);classified("strong-confidence-missing",8,false);
        for(int mode:new int[]{0,3}){String key="strong-pixel-"+mode;proof(key,mode,5,0);classified(key,12,true);}
        proof("strong-confidence-different",3,7,0);classified("strong-confidence-different",12,true);
        proof("strong-prepare-slow",0,0,20);classified("strong-prepare-slow",11,false);
        String preferred="strong-gx1973-ieee-div-policy-bank-v1:3:preferred92";
        proof(preferred,3,0,20);GpuQualification1961.Record r=GpuQualification1961.restore(preferred);
        check(r!=null,"u[10]==3 retains StrongPreferred admission despite slower exact GPU");
        check(r.gpuNanos>r.cpuNanos,"controlled slower GPU sample really occurred");
        check(!GpuQualification1961.exactRejected(preferred)&&failure(preferred)==null,"StrongPreferred does not invent speed or exact rejection");
        for(long[] sample:new long[][]{{0,100},{100,0},{-1,100},{100,100}}){
            String key="strong-record-"+sample[0]+"-"+sample[1];
            invoke(GpuStrong1960.class,null,"record",new Class<?>[]{String.class,boolean.class,long.class,long.class},key,true,sample[1],sample[0]);
            classified(key,sample[0]>0&&sample[1]>0?11:14,false);
        }
        GpuQualification1961.rejectMeasuredSpeed1992("strong-exact-threshold","controlled",100,95);
        check(failure("strong-exact-threshold")==null,"inclusive five percent boundary remains accepted");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"proof_modes\":2,\"transport_faults\":11}");
    }
}
