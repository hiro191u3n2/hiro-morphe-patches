package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual production proof/CPU worker and foreground process path with a
 * controlled transport, independent serial strong-NR expected pixels. */
public final class Gx30Gx36Schedule1964Test {
    static int assertions;
    static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static int[] source;static StrongNoise1958.Model model;static int[] expected,mask;
    static final int W=48,H=64;
    static void prepare()throws Exception{
        field(GpuQualification1961.class,"base").set(null,"gx30-gx36-production-test");
        source=new int[W*H];Random random=new Random(1964);
        for(int i=0;i<source.length;i++){int y=100+random.nextInt(31);source[i]=0xff000000|y<<16|(y+random.nextInt(5)-2)<<8|y+random.nextInt(5)-2;}
        model=StrongNoise1958.prepareJava(new StrongNoise1958.Patches(){public void read(int[] p,int x,int y,int w,int h){for(int r=0;r<h;r++)System.arraycopy(source,(y+r)*W+x,p,r*w,w);}},W,H,4,true);
        expected=new int[source.length];mask=new int[((W+3)/4)*((H+3)/4)];
        check(StrongNoise1958.gpuOracleSnapshot1961(source,expected,W,H,0,H,0,H,0,4,true,model,null,mask),"independent production serial strong oracle completes");
    }
    static void parallelExecution()throws Exception{
        final Set<String> threads=Collections.synchronizedSet(new HashSet<String>());
        final int[] out=new int[64];final CountDownLatch concurrent=new CountDownLatch(4);
        long measured=GpuQualification1961.parallelRows1964(0,64,4,LIVE,new GpuQualification1961.RowTask1964(){public boolean run(int begin,int end){
            threads.add(Thread.currentThread().getName());concurrent.countDown();
            try{if(!concurrent.await(3,TimeUnit.SECONDS))throw new AssertionError("actual CPU workers did not overlap");}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
            GpuNoise1960.pause(10000000L);Arrays.fill(out,begin,end,7);return true;
        }});
        check(measured>=10000000L&&threads.size()==4,"actual measured wall duration uses four real production workers");
        for(int value:out)check(value==7,"all detached CPU rows completed exactly once");
        long failed=GpuQualification1961.parallelRows1964(0,64,4,LIVE,new GpuQualification1961.RowTask1964(){public boolean run(int b,int e){return false;}});
        check(failed==0,"incomplete CPU work cannot be a timing oracle");
        final AtomicBoolean cancelled=new AtomicBoolean();
        boolean caught=false;try{GpuQualification1961.parallelRows1964(0,64,4,new GpuQualification1961.Cancellation(){public boolean cancelled(){return cancelled.get();}},new GpuQualification1961.RowTask1964(){public boolean run(int b,int e){cancelled.set(true);return true;}});}catch(CancellationException valid){caught=true;}
        check(caught&&SpeedWorkers1935.cpuIdle1944(),"cancelled parallel proof joins every real worker and cannot certify");
    }
    static int[] uniforms(){int[] u=new int[32];u[0]=W;u[1]=H;u[3]=H;u[5]=H;u[7]=H;u[8]=4;u[9]=1;u[10]=3;u[12]=1;return u;}
    static GpuQualification1961.Probe proof(String key)throws Exception{
        Class<?> c=Class.forName("com.hiro.ulike.GpuStrong1960$Proof");Constructor<?> made=c.getDeclaredConstructors()[0];made.setAccessible(true);
        return (GpuQualification1961.Probe)made.newInstance(key,source.clone(),null,null,uniforms(),model,null);
    }
    static void proofs()throws Exception{
        GpuNoise1960.reset(expected,mask);GpuQualification1961.Probe fast=proof("gx30-fast");
        try{fast.run(LIVE);}finally{fast.close();}
        GpuQualification1961.Record record=GpuQualification1961.restore("gx30-fast");
        check(record!=null&&record.gpuNanos<=record.cpuNanos-record.cpuNanos/20,"real parallel complete-output CPU proof admits controlled faster transport");
        check(GpuNoise1960.opens==GpuNoise1960.closes&&GpuNoise1960.opens>=2,"both exact trials include session open and close");
        GpuNoise1960.reset(expected,mask);GpuNoise1960.closeDelay=200000000L;GpuQualification1961.Probe close=proof("gx30-slow-close");try{close.run(LIVE);}finally{close.close();}
        check(GpuQualification1961.restore("gx30-slow-close")==null&&!GpuQualification1961.exactRejected("gx30-slow-close"),"close-inclusive slow GPU is rejected as speed only");
        GpuNoise1960.reset(expected,mask);GpuNoise1960.uploadDelay=100000000L;GpuQualification1961.Probe upload=proof("gx30-slow-upload");try{upload.run(LIVE);}finally{upload.close();}
        check(GpuQualification1961.restore("gx30-slow-upload")==null&&!GpuQualification1961.exactRejected("gx30-slow-upload"),"upload and read transfer-inclusive slow GPU rejected");
        GpuNoise1960.reset(expected,mask);GpuNoise1960.corrupt=true;GpuQualification1961.Probe bad=proof("gx30-wrong-pixel");try{bad.run(LIVE);}finally{bad.close();}
        check(GpuQualification1961.restore("gx30-wrong-pixel")==null&&GpuQualification1961.exactRejected("gx30-wrong-pixel"),"complete pixel mismatch permanently rejects candidate");
        GpuQualification1961.qualified("gx30-too-close",100000,95001,0);check(GpuQualification1961.restore("gx30-too-close")==null,"strictly less than five percent gain cannot admit");
        GpuQualification1961.qualified("gx30-tie",100000,100000,0);check(GpuQualification1961.restore("gx30-tie")==null,"tie cannot admit");
        GpuQualification1961.qualified("gx30-boundary",100000,95000,0);check(GpuQualification1961.restore("gx30-boundary")!=null,"exact five percent boundary remains valid");
    }
    static String processKey()throws Exception{Method key=GpuStrong1960.class.getDeclaredMethod("key",int[].class);key.setAccessible(true);return (String)key.invoke(null,(Object)uniforms());}
    static Object stage()throws Exception{return field(GpuStrong1960.class,"active").get(null);}
    static void banks(Object s,long duration)throws Exception{boolean[] busy=(boolean[])field(s.getClass(),"banks").get(s);long[] starts=(long[])field(s.getClass(),"bankStart").get(s),times=(long[])field(s.getClass(),"bankDuration").get(s);Arrays.fill(busy,true);Arrays.fill(starts,System.nanoTime());Arrays.fill(times,duration);}
    static boolean process(final int[] out){return GpuStrong1960.process(source,out,W,H,0,H,0,H,0,4,true,model,null,new int[mask.length],null,new GpuStrong1960.Oracle(){public boolean run(int[] p,int[] c){throw new AssertionError("certified path must use bank admission or return CPU fallback");}});}
    static void clearStage(Object s)throws Exception{synchronized(s){Arrays.fill((boolean[])field(s.getClass(),"banks").get(s),false);s.notifyAll();}GpuStrong1960.endStage(model);}
    static void foregroundBankChoice()throws Exception{
        final String key=processKey();GpuQualification1961.qualified(key,500000000L,30000000L,0);GpuNoise1960.reset(expected,mask);GpuStrong1960.beginStage(model);final Object s=stage();banks(s,40000000L);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();final AtomicBoolean result=new AtomicBoolean();final int[] out=new int[source.length];
        Thread waiting=new Thread(new Runnable(){public void run(){try{result.set(process(out));}catch(Throwable e){failure.set(e);}}},"foreground-bank-test");waiting.start();
        long deadline=System.nanoTime()+2000000000L;while(System.nanoTime()<deadline&&!field(s.getClass(),"waiter").getBoolean(s))Thread.yield();
        check(field(s.getClass(),"waiter").getBoolean(s),"measured faster nearest bank admits one bounded waiting strip");
        check(!process(new int[source.length]),"second waiting strip immediately keeps CPU fallback");
        synchronized(s){((boolean[])field(s.getClass(),"banks").get(s))[0]=false;s.notifyAll();}
        waiting.join(3000);check(!waiting.isAlive()&&failure.get()==null&&result.get(),"bank release resumes actual production foreground GPU process");
        check(Arrays.equals(out,expected)&&!field(s.getClass(),"waiter").getBoolean(s),"bounded bank reuse preserves all exact output pixels and releases waiter");
        clearStage(s);check(GpuNoise1960.opens==GpuNoise1960.closes,"photograph stage closes exactly one private GPU session");
        GpuNoise1960.reset(expected,mask);GpuStrong1960.beginStage(model);Object slow=stage();banks(slow,2000000000L);
        long start=System.nanoTime();check(!process(new int[source.length]),"measured slower GPU bank backlog immediately selects CPU fallback");check(System.nanoTime()-start<500000000L,"fallback does not wait for overloaded GPU");clearStage(slow);
        GpuNoise1960.reset(expected,mask);GpuStrong1960.beginStage(model);final Object cancel=stage();banks(cancel,40000000L);
        final AtomicReference<Throwable> cancelled=new AtomicReference<Throwable>();Thread t=new Thread(new Runnable(){public void run(){try{process(new int[source.length]);}catch(Throwable e){cancelled.set(e);}}});t.start();
        deadline=System.nanoTime()+2000000000L;while(System.nanoTime()<deadline&&!field(cancel.getClass(),"waiter").getBoolean(cancel))Thread.yield();t.interrupt();t.join(3000);
        check(!t.isAlive()&&cancelled.get() instanceof CancellationException&&!field(cancel.getClass(),"waiter").getBoolean(cancel),"foreground interruption frees GPU waiter immediately before commit");clearStage(cancel);
        GpuNoise1960.reset(expected,mask);GpuStrong1960.beginStage(model);final Object failed=stage();banks(failed,40000000L);
        final AtomicReference<Throwable> waitFailure=new AtomicReference<Throwable>();final AtomicBoolean waitFallback=new AtomicBoolean();
        Thread f=new Thread(new Runnable(){public void run(){try{waitFallback.set(!process(new int[source.length]));}catch(Throwable e){waitFailure.set(e);}}});f.start();
        deadline=System.nanoTime()+2000000000L;while(System.nanoTime()<deadline&&!field(failed.getClass(),"waiter").getBoolean(failed))Thread.yield();
        synchronized(failed){field(failed.getClass(),"failed").setBoolean(failed,true);failed.notifyAll();}f.join(3000);
        check(!f.isAlive()&&waitFailure.get()==null&&waitFallback.get()&&!field(failed.getClass(),"waiter").getBoolean(failed),"a failed photograph session wakes waiting strip immediately to CPU fallback");clearStage(failed);
        // Hold the real stage monitor past the measured wait deadline, then
        // release a bank before letting the timed-out waiter reacquire it.
        // Previously the free-bank branch ran first and incorrectly dispatched.
        GpuNoise1960.reset(expected,mask);GpuStrong1960.beginStage(model);final Object expired=stage();banks(expired,40000000L);
        final AtomicReference<Throwable> expiredFailure=new AtomicReference<Throwable>();final AtomicBoolean expiredFallback=new AtomicBoolean();
        Thread race=new Thread(new Runnable(){public void run(){try{expiredFallback.set(!process(new int[source.length]));}catch(Throwable e){expiredFailure.set(e);}}},"expired-bank-release-race");race.start();
        deadline=System.nanoTime()+2000000000L;while(System.nanoTime()<deadline&&!field(expired.getClass(),"waiter").getBoolean(expired))Thread.yield();
        check(field(expired.getClass(),"waiter").getBoolean(expired),"timeout-release race enters actual production bounded wait");
        synchronized(expired){GpuNoise1960.pause(600000000L);((boolean[])field(expired.getClass(),"banks").get(expired))[0]=false;expired.notifyAll();}
        race.join(3000);
        check(!race.isAlive()&&expiredFailure.get()==null&&expiredFallback.get(),"expired waiter falls back despite a newly free notified GPU bank");
        check(GpuNoise1960.submits==0&&!field(expired.getClass(),"waiter").getBoolean(expired),"timeout-release race cannot dispatch or retain waiter after CPU95 deadline");clearStage(expired);
        GpuNoise1960.reset(expected,mask);GpuNoise1960.unavailable=true;GpuStrong1960.beginStage(model);check(!process(new int[source.length]),"failed GPU initialization preserves immediate CPU fallback");GpuStrong1960.endStage(model);
    }
    public static void main(String[] args)throws Exception{prepare();parallelExecution();proofs();foregroundBankChoice();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_production_parallel_cpu\":true,\"actual_production_foreground_bank_path\":true,\"transport\":\"controlled-timing-and-exact-oracle-response\",\"max_gpu_bank_waiters\":1,\"complete_output_trials\":2,\"physical_android_tested\":false,\"device_speedup_verified\":false}");}
}
