package com.hiro.ulike;
import android.content.Context;
import java.io.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Production qualification/storage, save ownership, bounded worker pool,
 * Strong facade, JNI and GLSL. Only Android/model holders and CPU pixels are
 * fixtures; the pixels are generated independently by frozen published Java. */
public final class Bootstrap1979Test {
    static int assertions;static final AtomicInteger oracleCalls=new AtomicInteger();
    static void check(boolean value,String text){assertions++;if(!value)throw new AssertionError(text);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();return a;}
    static final class Data {
        int[] u,want,confidence;int[][] input;
        StrongNoise1958.Model model(){StrongNoise1958.Model m=new StrongNoise1958.Model();m.height=u[7];m.maps=new int[][]{input[4],input[5],input[6]};m.evidence=new float[input[2].length];for(int i=0;i<m.evidence.length;i++)m.evidence[i]=Float.intBitsToFloat(input[2][i]);m.frozenPixels=want;m.frozenConfidence=confidence;return m;}
        Data cut(int begin,int end){Data d=new Data();d.u=u.clone();d.input=input.clone();int first=begin-18,last=end+18,width=u[0],cols=(width+3)/4;d.u[1]=last-first;d.u[2]=18;d.u[3]=18+end-begin;d.u[4]=0;d.u[5]=last-first;d.u[6]=first;d.input[0]=Arrays.copyOfRange(input[0],first*width,last*width);d.input[3]=Arrays.copyOfRange(input[3],begin*width*2,end*width*2);d.want=Arrays.copyOfRange(want,begin*width,end*width);d.confidence=Arrays.copyOfRange(confidence,begin/4*cols,end/4*cols);return d;}
    }
    static Data load(String path)throws Exception{
        try(DataInputStream in=new DataInputStream(new FileInputStream(path))){check(in.readInt()==1960001,"frozen oracle format");for(;;){String name=in.readUTF();if(name.isEmpty())throw new AssertionError("representative complete Strong oracle missing");int shader=in.readInt();int[] u=ints(in);in.readInt();int n=in.readInt();int[][] input=new int[n][];for(int i=0;i<n;i++)input[i]=ints(in);int[] out=ints(in),cf=ints(in);if(shader==0&&u[10]==3&&u[1]>=96&&u[2]==0&&u[3]==u[1]&&u[8]>0&&cf.length>0){Data d=new Data();d.u=u;d.input=input;d.want=out;d.confidence=cf;return d;}}}
    }
    static void caches()throws Exception{
        ((Map<?,?>)field(GpuStrong1960.class,"GATES").get(null)).clear();
        synchronized(field(GpuQualification1961.class,"LOCK").get(null)){((Map<?,?>)field(GpuQualification1961.class,"RECORDS").get(null)).clear();((Map<?,?>)field(GpuQualification1961.class,"FAILURES").get(null)).clear();}
    }
    static void next()throws Exception{GpuQualification1961.captureChanged();ProcessingTiming1947.epoch++;ProcessingTiming1947.reset();caches();check(SaveQueue1935.idle1953(),"previous actual save owner drained");}
    static void begin(StrongNoise1958.Model model)throws Exception{
        SaveQueue1935.reserve();check(!SaveQueue1935.idle1953(),"actual save ownership blocks optional idle work");GpuStrong1960.beginStage(model);
        try{GpuStrong1960.class.getDeclaredMethod("configureWorkers1978",StrongNoise1958.Model.class,int.class).invoke(null,model,4);}catch(NoSuchMethodException legacy77){}
    }
    static void end(StrongNoise1958.Model model)throws Exception{
        try{GpuStrong1960.endStage(model);}finally{Thread.interrupted();SaveQueue1935.release();NativeSpeed1979Test.clearFault();WholeRoute1953.retained=0;}
        check(GpuNoise1960.reservedBytes1971()==0&&!GpuNoise1960.sessionBusy()&&SpeedWorkers1935.cpuIdle1944(),"native sessions, leases and actual bounded workers drained");
        check(field(GpuStrong1960.class,"activeCold1973").getInt(null)==0&&((Set<?>)field(GpuStrong1960.class,"FLIGHTS1973").get(null)).isEmpty(),"singleflight ownership fully released");
    }
    static final class Job implements Runnable {
        final Data d;final StrongNoise1958.Model model;final String mode;final int[] out,cf;int calls;boolean ok;Throwable failure;
        Job(Data d,StrongNoise1958.Model m,String mode){this.d=d;model=m;this.mode=mode;out=new int[d.input[0].length];cf=new int[d.confidence.length+9];Arrays.fill(out,0x13579bdf);Arrays.fill(cf,0x2468ace0);}
        public void run(){try{ok=GpuStrong1960.process(d.input[0],out,d.u[0],d.u[1],d.u[2],d.u[3],d.u[4],d.u[5],d.u[6],d.u[8],d.u[9]!=0,model,d.input[3],cf,null,(p,c)->{
            oracleCalls.incrementAndGet();calls++;if("false".equals(mode))return false;if("throw".equals(mode))throw new IllegalStateException("controlled CPU reference failure");
            System.arraycopy(d.want,0,p,d.u[2]*d.u[0],d.want.length);System.arraycopy(d.confidence,0,c,0,d.confidence.length);
            if("argb".equals(mode))p[d.u[3]*d.u[0]-1]^=1;if("confidence".equals(mode))c[d.confidence.length-1]^=1;
            if("late-negative".equals(mode)&&calls==2)GpuQualification1961.rejectExact1971(GpuStrong1960.profileKey1973(d.u,0),"argb_mismatch");
            if("interrupt".equals(mode))Thread.currentThread().interrupt();return true;
        });}catch(Throwable t){failure=t;}}
        void pixels(){check(Arrays.equals(d.want,Arrays.copyOfRange(out,d.u[2]*d.u[0],d.u[3]*d.u[0])),"complete ARGB equals frozen published CPU");check(Arrays.equals(d.confidence,Arrays.copyOf(cf,d.confidence.length)),"complete confidence equals frozen published CPU");for(int i=d.confidence.length;i<cf.length;i++)check(cf[i]==0x2468ace0,"pooled confidence tail remains private");}
    }
    static int attempts()throws Exception{Object stage=field(GpuStrong1960.class,"active").get(null);Object cold=field(stage.getClass(),"cold1973").get(stage);return field(cold.getClass(),"attempts").getInt(cold);}
    static void cold(Data d,int revision)throws Exception{
        String key=GpuStrong1960.profileKey1973(d.u,0);boolean bootstrap=revision!=78;check(GpuQualification1961.restore(key)==null,"no seed certificate before first real capture");
        for(int capture=0;capture<2;capture++){
            next();StrongNoise1958.Model model=d.model();Job job=new Job(d,model,"normal");begin(model);
            try{SpeedWorkers1935.run(new Runnable[]{job});check(job.failure==null&&job.ok,"cold capture completes through production facade");job.pixels();check(job.calls==(bootstrap?(capture==0?2:0):1),"exact .77/.78/.79 CPU-reference behavior");check(attempts()==(bootstrap&&capture==0?1:0),"only initial legacy bootstrap consumes an admission");}
            finally{end(model);}
            check(ProcessingTiming1947.lastGpu==(bootstrap?1:0)&&ProcessingTiming1947.lastCpu==(bootstrap?0:1),"selected native output differs from .78 all-CPU regression");check(ProcessingTiming1947.verification==(bootstrap&&capture==0?2:0),"exact2 only once, no reference on restored capture");
            caches();check((GpuQualification1961.restore(key)!=null)==bootstrap,"actual signed preferences restore completed proof after clearing both RAM caches");
        }
    }
    static void budget(Data full)throws Exception{
        next();StrongNoise1958.Model model=full.model();Data[] strips={full.cut(20,36),full.cut(36,52),full.cut(52,68)};begin(model);int before=NativeSpeed1979Test.dispatches();
        try{int proven=0;for(Data d:strips){Job job=new Job(d,model,"normal");SpeedWorkers1935.run(new Runnable[]{job});check(job.failure==null&&job.ok,"each distinct production strip completes");job.pixels();if(job.calls==2)proven++;else check(job.calls==1,"non-admitted strip computes CPU only once");}check(proven>=1&&proven<=2&&attempts()==proven,"capture admits at most two exact2 proofs, never sixteen");check(NativeSpeed1979Test.dispatches()-before==2*proven,"only admitted proofs submit two real native trials");}
        finally{end(model);}
        check(ProcessingTiming1947.verification>=2&&ProcessingTiming1947.verification<=4,"bounded actual comparison work");
    }
    static void followers(Data d)throws Exception{
        next();StrongNoise1958.Model model=d.model();Job[] jobs=new Job[4];for(int i=0;i<4;i++)jobs[i]=new Job(d,model,"normal");begin(model);NativeSpeed1979Test.delay(35);int before=NativeSpeed1979Test.dispatches();
        try{SpeedWorkers1935.run(jobs);int owners=0;for(Job job:jobs){check(job.failure==null&&job.ok,"same-key actual worker completes");job.pixels();if(job.calls==2)owners++;else check(job.calls==0||job.calls==1,"follower uses certificate or single ordinary fallback");}check(owners==1&&attempts()==1,"four actual production workers share one same-key proof");check(NativeSpeed1979Test.dispatches()-before<=5,"followers cannot duplicate two-trial GPU proof");}
        finally{end(model);}
        check(ProcessingTiming1947.verification==2,"same-key comparison count stays exactly two");
    }
    static void benchmark(Data d)throws Exception{
        next();StrongNoise1958.Model model=d.model();String key=GpuStrong1960.profileKey1973(d.u,0);check(GpuQualification1961.restore(key)==null,"resident benchmark starts without seeded legacy proof");
        GpuResident1976.benchmark=true;begin(model);Job job=new Job(d,model,"normal");int before=NativeSpeed1979Test.dispatches();
        try{SpeedWorkers1935.run(new Runnable[]{job});check(job.failure==null&&job.ok&&job.calls==1,"unknown resident benchmark keeps original one-pass CPU route");job.pixels();check(attempts()==0&&NativeSpeed1979Test.dispatches()==before,"idle whole-chain comparison spends no foreground admission or native proof");}
        finally{try{end(model);}finally{GpuResident1976.benchmark=false;}}
        caches();check(GpuQualification1961.restore(key)==null,"unknown resident benchmark does not fabricate legacy child proof");
    }
    static void failure(Data d,String mode)throws Exception{
        next();StrongNoise1958.Model model=d.model();String key=GpuStrong1960.profileKey1973(d.u,0);Job job=new Job(d,model,mode);begin(model);
        try{
            if("memory".equals(mode))WholeRoute1953.retained=Long.MAX_VALUE;if("readback".equals(mode))NativeSpeed1979Test.failRead();
            if("legacy-rejected".equals(mode))for(int p=0;p<3;p++)GpuQualification1961.rejectExact1971(GpuStrong1960.profileKey1973(d.u,p),"policy_failure");
            SpeedWorkers1935.run(new Runnable[]{job});
            if("interrupt".equals(mode))check(job.failure instanceof CancellationException&&!job.ok,"cold reference cancellation propagates with pending native work");
            else if("false".equals(mode)||"throw".equals(mode)||"memory".equals(mode))check(job.failure==null&&!job.ok,"unfinished cold result requests original caller repair");
            else{check(job.failure==null&&job.ok,"valid CPU fallback remains available");if(!"argb".equals(mode)&&!"confidence".equals(mode))job.pixels();}
        }finally{end(model);}
        check(ProcessingTiming1947.lastGpu==0,"failed or deferred candidate never becomes saved GPU output");caches();check(GpuQualification1961.restore(key)==null,"no incomplete/failed trial is persisted as proof");
        if("late-negative".equals(mode)||"legacy-rejected".equals(mode))check(GpuQualification1961.exactRejected(key),"actual exact rejection remains signed and durable");
        else check(!GpuQualification1961.exactRejected(key),"transport, cancellation and one alternative mismatch do not forge global negative");
        if("memory".equals(mode))check(job.calls==0&&ProcessingTiming1947.verification==0,"memory refusal precedes any CPU proof");
        if("legacy-rejected".equals(mode))check(job.calls==1&&ProcessingTiming1947.verification==0&&GpuQualification1961.restore(GpuStrong1960.profileKey1973(d.u,2))==null,"all legacy negatives are honored; new routes still await idle proof");
    }
    public static void main(String[] args)throws Exception{
        check(GpuNoise1960.available()&&GpuNoise1960.warmEnvironment1973()&&GpuNoise1960.supports(39),"real JNI context and exact legacy program");GpuQualification1961.initialize(new Context());Data full=load(args[0]),d=full.cut(20,36);String mode=args[1];int revision=Integer.parseInt(args[2]);
        if("cold".equals(mode))cold(d,revision);else if("budget".equals(mode))budget(full);else if("followers".equals(mode))followers(d);else if("benchmark".equals(mode))benchmark(d);else failure(d,mode);
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"revision\":"+revision+",\"oracle_calls\":"+oracleCalls.get()+",\"actual_signed_preferences\":true,\"actual_workers\":true,\"actual_jni\":true,\"physical_android_tested\":false}");
    }
}
