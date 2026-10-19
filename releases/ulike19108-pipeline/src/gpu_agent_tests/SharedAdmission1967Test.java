package com.hiro.ulike;
import java.lang.reflect.*;
public final class SharedAdmission1967Test {
    static int assertions;
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static GpuQualification1961.Probe proof(String key)throws Exception{
        return proof(key,3);
    }
    static GpuQualification1961.Probe proof(String key,int mode)throws Exception{
        int[] u=new int[32];u[0]=32;u[1]=32;u[3]=32;u[5]=32;u[7]=32;u[8]=4;u[9]=1;u[10]=3;u[12]=1;
        u[10]=mode;u[12]=mode==3?1:0;
        Class<?> type=Class.forName("com.hiro.ulike.GpuStrong1960$Proof");Constructor<?> ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
        return (GpuQualification1961.Probe)ctor.newInstance(key,new int[1024],null,mode==3?null:new float[16],u,mode==3?new StrongNoise1958.Model():null,null);
    }
    static void reset(){GpuNoise1960.reset();GpuQualification1961.reset();}
    static void run(String key,GpuQualification1961.Cancellation c)throws Exception{GpuQualification1961.Probe p=proof(key);try{p.run(c);}finally{p.close();}}
    static void preparationCosts()throws Exception{
        reset();GpuQualification1961.Probe fast=proof("preparation-fast",0);try{fast.run(LIVE);}finally{fast.close();}
        check(GpuQualification1961.restore("preparation-fast")!=null,"complete ephemeral preparation can qualify");
        check(GpuNoise1960.opens==6&&GpuNoise1960.closes==6&&GpuNoise1960.executes==6,"each preparation trial opens and closes a separate session");
        check(GpuNoise1960.modelUploads==24,"preparation retains full per-trial model upload cost");
        reset();GpuNoise1960.uploadDelay=13000000L;GpuQualification1961.Probe slow=proof("preparation-slow",0);try{slow.run(LIVE);}finally{slow.close();}
        check(GpuQualification1961.restore("preparation-slow")==null&&!GpuQualification1961.exactRejected("preparation-slow"),"preparation overhead is never improperly halved for admission");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"rejected ephemeral preparation releases sessions");
    }
    static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void backendCounts()throws Exception{
        reset();ProcessingTiming1947.backendCalls=0;
        final StrongNoise1958.Model model=new StrongNoise1958.Model();
        final int[] input=new int[1024],output=new int[1024],confidence=new int[64];
        GpuStrong1960.beginStage(model);
        check(GpuStrong1960.process(input,output,32,32,0,32,0,32,0,4,true,model,null,confidence,null,new GpuStrong1960.Oracle(){
            public boolean run(int[] p,int[] c){return StrongNoise1958.gpuOracleSnapshot1961(input,p,32,32,0,32,0,32,0,4,true,model,null,c);}
        }),"unqualified foreground uses complete CPU oracle");
        int[] u=new int[32];u[0]=32;u[1]=32;u[3]=32;u[5]=32;u[7]=32;u[8]=4;u[9]=1;u[10]=3;u[12]=1;
        Method method=GpuStrong1960.class.getDeclaredMethod("key",int[].class);method.setAccessible(true);
        String key=(String)method.invoke(null,(Object)u);
        check(key.startsWith("strong-gx1964-parallel-policy-bank-v1:"),"existing safe GPU qualification key preserved");
        GpuQualification1961.qualified(key,500000000L,1000000L,0);
        check(GpuStrong1960.process(input,output,32,32,0,32,0,32,0,4,true,model,null,confidence,null,new GpuStrong1960.Oracle(){
            public boolean run(int[] p,int[] c){throw new AssertionError("admitted GPU called CPU");}
        }),"qualified foreground completes actual GPU transport path");
        GpuStrong1960.endStage(model);
        check(ProcessingTiming1947.backendCalls==1&&ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==1,"capture receives one accurate mixed GPU and CPU strip count");
        GpuStrong1960.endStage(model);
        check(ProcessingTiming1947.backendCalls==1,"duplicate stage close cannot emit another backend record");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"mixed foreground stage releases GPU session");
    }
    static void snapshotBounds()throws Exception{
        reset();int[] u=new int[32];u[0]=32;u[1]=32;u[3]=32;
        Method prep=GpuStrong1960.class.getDeclaredMethod("schedulePrepare",String.class,int[].class,float[].class,int[].class);prep.setAccessible(true);
        prep.invoke(null,"prepare-byte-bound",new int[1024],new float[16],u);
        check(GpuQualification1961.lastQueueBytes==4L*1024+256,"preparation queue includes copied source evidence and all uniforms");
        GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection();protection.plan=new GpuPolicy1960.Plan();protection.plan.localNoise=new GpuPolicy1960.LocalNoise();protection.plan.localNoise.columns=7;protection.plan.localNoise.rows=9;
        Method full=GpuStrong1960.class.getDeclaredMethod("scheduleFull",String.class,int[].class,int[].class,int[].class,StrongNoise1958.Model.class,GpuPolicy1960.Protection.class);full.setAccessible(true);
        full.invoke(null,"full-byte-bound",new int[1024],new int[2048],u,new StrongNoise1958.Model(),protection);
        long expected=4L*(1024+2048+1000+250+63+16)+1024+8L*1024+256+4L*7*9;
        check(GpuQualification1961.lastQueueBytes==expected,"full queue includes model original evidence policy grid uniforms and masks");
    }
    public static void main(String[] args)throws Exception{
        reset();GpuNoise1960.uploadDelay=13000000L;run("shared-cost",LIVE);
        GpuQualification1961.Record r=GpuQualification1961.restore("shared-cost");
        check(r!=null,"two exact complete trials admit shared uploads that serial per-trial transfer would reject");
        check(r.gpuNanos<=r.cpuNanos-r.cpuNanos/20,"retains actual five-percent gain requirement");
        check(GpuNoise1960.opens==3&&GpuNoise1960.closes==3,"each workgroup variant closes its one reused session");
        check(GpuNoise1960.modelUploads==12,"model uploaded exactly once per variant, not once per trial");
        check(GpuNoise1960.executes==6,"all three variants produce two complete outputs");
        reset();GpuNoise1960.badSecondPixel=true;run("wrong-second-pixel",LIVE);
        check(GpuQualification1961.restore("wrong-second-pixel")==null&&GpuQualification1961.exactRejected("wrong-second-pixel"),"second full-output trial pixel mismatch remains permanent rejection");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"wrong-pixel path releases every session");
        reset();GpuNoise1960.badConfidence=true;run("wrong-confidence",LIVE);
        check(GpuQualification1961.restore("wrong-confidence")==null&&GpuQualification1961.exactRejected("wrong-confidence"),"NR13 confidence mismatch remains rejection");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"wrong-confidence path releases sessions");
        reset();GpuNoise1960.failSecondExecute=true;run("failed-read",LIVE);
        check(GpuQualification1961.restore("failed-read")==null,"incomplete second transfer cannot qualify");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"read failure releases sessions");
        reset();GpuNoise1960.failModelUpload=true;run("failed-upload",LIVE);
        check(GpuQualification1961.restore("failed-upload")==null&&GpuNoise1960.executes==0,"failed setup cannot execute or qualify");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"upload failure releases sessions");
        reset();run("cancelled",new GpuQualification1961.Cancellation(){public boolean cancelled(){return GpuNoise1960.executes>0;}});
        check(GpuQualification1961.restore("cancelled")==null,"cancelled proof cannot publish");
        check(GpuNoise1960.opens==1&&GpuNoise1960.closes==1,"capture cancellation closes current shared session");
        reset();GpuNoise1960.closeDelay=120000000L;run("slow-close",LIVE);
        check(GpuQualification1961.restore("slow-close")==null&&!GpuQualification1961.exactRejected("slow-close"),"shared session close remains included and slow route keeps CPU");
        check(GpuNoise1960.opens==GpuNoise1960.closes,"slow route releases all sessions");
        backendCounts();
        preparationCosts();
        snapshotBounds();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"shared_setup_actual_trials\":true,\"complete_output_trials\":2,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}

