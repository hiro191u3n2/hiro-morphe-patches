package com.hiro.ulike;
import android.content.Context;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Real queue and real SpeedWorkers; cancellation joins children before close. */
public final class Cancellation1997Test {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
    static void drain()throws Exception{long until=System.nanoTime()+3000000000L;while(GpuQualification1961.retainedBytes()!=0){if(System.nanoTime()>until)throw new AssertionError("drain timeout");Thread.sleep(2);}}
    static void begin()throws Exception{SaveQueue1935.idle=true;field(GpuQualification1961.class,"lastCapture").setLong(null,System.nanoTime()-3000000000L);GpuQualification1961.wake();}
    static void reset()throws Exception {
        SaveQueue1935.idle=false;GpuQualification1961.captureChanged();drain();ProcessingTiming1947.epoch++;
        synchronized(field(GpuQualification1961.class,"LOCK").get(null)) {
            Arrays.fill((Object[])field(GpuQualification1961.class,"ATTEMPTS1984").get(null),null);
            field(GpuQualification1961.class,"attemptCount1984").setInt(null,0);field(GpuQualification1961.class,"attemptCursor1984").setInt(null,0);
        }
    }
    public static void main(String[] args)throws Exception {
        GpuQualification1961.initialize(new Context());
        try {
            reset();final CountDownLatch started=new CountDownLatch(1);final AtomicInteger live=new AtomicInteger(),closed=new AtomicInteger();final AtomicReference<RuntimeException> actual=new AtomicReference<RuntimeException>();
            check(GpuQualification1961.schedule("actual-interrupt1997",1,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){
                    try{SpeedWorkers1935.run(new Runnable[]{()->{live.incrementAndGet();started.countDown();try{new CountDownLatch(1).await();}catch(InterruptedException expected){Thread.currentThread().interrupt();}finally{live.decrementAndGet();}}});}
                    catch(RuntimeException expected){actual.set(expected);throw expected;}
                }
                public void close(){check(live.get()==0,"children joined before input close");closed.incrementAndGet();}
            }),"actual interrupt probe admitted");
            begin();check(started.await(3,TimeUnit.SECONDS),"real worker entered");SaveQueue1935.idle=false;GpuQualification1961.captureChanged();drain();
            check(actual.get()!=null&&actual.get().getClass()==IllegalStateException.class&&"speed interrupted".equals(actual.get().getMessage()),"actual SpeedWorkers cancellation sentinel observed");
            String summary=GpuQualification1961.attemptSummary1985();check(summary.contains("撮影変更・中断で取消")&&!summary.contains("IllegalStateException"),"expected sentinel displayed as cancellation without false exception detail");
            check(closed.get()==1&&GpuQualification1961.retainedBytes()==0&&SpeedWorkers1935.cpuIdle1944(),"cancellation releases exactly one owner and all worker permits");
            // An unrelated same-message failure during cancellation must remain visible.
            reset();check(GpuQualification1961.schedule("real-fault1997",1,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){Thread.currentThread().interrupt();throw new IllegalStateException("speed interrupted");}
                public void close(){}
            }),"unrelated fault admitted");begin();drain();
            summary=GpuQualification1961.attemptSummary1985();check(summary.contains("IllegalStateException")&&summary.contains("Cancellation1997Test"),"same message outside SpeedWorkers retains actual fault diagnostic");
            // Same real worker location without cancellation is still a failure.
            reset();check(GpuQualification1961.schedule("uncancelled-sentinel1997",1,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){IllegalStateException failure=new IllegalStateException("speed interrupted");failure.setStackTrace(new StackTraceElement[]{new StackTraceElement("com.hiro.ulike.SpeedWorkers1935","run","SpeedWorkers1935.java",206)});throw failure;}
                public void close(){}
            }),"uncancelled control admitted");begin();drain();
            check(GpuQualification1961.attemptSummary1985().contains("IllegalStateException"),"uncancelled failure is not silently reclassified");
            System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"cancelled_speed_workers_classified\":true,\"actual_worker_join_and_owner_release\":true}");
        }finally{reset();((ExecutorService)field(SpeedWorkers1935.class,"EXECUTOR").get(null)).shutdownNow();}
    }
}
