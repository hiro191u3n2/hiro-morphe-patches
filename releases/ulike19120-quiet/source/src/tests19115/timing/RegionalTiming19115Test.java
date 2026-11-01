package com.hiro.ulike;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Counts belong to one foreground photograph and only committed outputs. */
public final class RegionalTiming19115Test {
    static int assertions;
    static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
    static ProcessingTiming1947.Trace trace(long id){return new ProcessingTiming1947.Trace(id,1000,1000000000L);}
    static String render(ProcessingTiming1947.Trace t)throws Exception{
        Method m=ProcessingTiming1947.class.getDeclaredMethod("render",ProcessingTiming1947.Trace.class);
        m.setAccessible(true);return (String)m.invoke(null,t);
    }
    public static void main(String[] args)throws Exception{
        ProcessingTiming1947.Trace a=trace(1),b=trace(2);
        check(render(a).contains("強ノイズ新方式の実採用: 未観測"),"unknown is not invented zero");
        ProcessingTiming1947.strongRegional19115(b,0,0);
        check(render(b).contains("強ノイズ新方式の実採用: 0区間 / 0画素"),"observed fallback is explicit zero");
        ProcessingTiming1947.strongRegional19115(a,-1,100);
        ProcessingTiming1947.strongRegional19115(a,1,-1);
        ProcessingTiming1947.strongRegional19115(a,0,100);
        ProcessingTiming1947.strongRegional19115(a,3,2);
        check(!a.strongRegionalObserved19115,"invalid observations cannot manufacture adoption");
        ProcessingTiming1947.strongRegional19115(a,2,1566720);
        ProcessingTiming1947.strongRegional19115(a,1,783360);
        check(a.strongRegionalStrips19115==3&&a.strongRegionalPixels19115==2350080,"committed counts accumulate");
        check(b.strongRegionalStrips19115==0&&b.strongRegionalPixels19115==0,"another photo is unaffected");
        ProcessingTiming1947.finish(a,true);
        ProcessingTiming1947.strongRegional19115(a,7,9999999);
        check(a.strongRegionalStrips19115==3&&a.strongRegionalPixels19115==2350080,"terminal capture cannot be changed by late worker");
        ProcessingTiming1947.Trace limit=trace(3);
        limit.strongRegionalStrips19115=Long.MAX_VALUE-1;limit.strongRegionalPixels19115=Long.MAX_VALUE-1;
        ProcessingTiming1947.strongRegional19115(limit,4,4000);
        check(limit.strongRegionalStrips19115==Long.MAX_VALUE&&limit.strongRegionalPixels19115==Long.MAX_VALUE,"overflow saturates");
        final ProcessingTiming1947.Trace parallel=trace(4);
        final CountDownLatch ready=new CountDownLatch(8),start=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[8];
        for(int i=0;i<workers.length;i++){workers[i]=new Thread(new Runnable(){public void run(){
            ready.countDown();try{start.await();for(int n=0;n<256;n++)ProcessingTiming1947.strongRegional19115(parallel,1,3060);}
            catch(Throwable t){failure.compareAndSet(null,t);}
        }});workers[i].start();}
        ready.await();start.countDown();for(Thread worker:workers)worker.join();
        check(failure.get()==null&&parallel.strongRegionalStrips19115==2048&&parallel.strongRegionalPixels19115==6266880,"parallel completions are not lost");
        parallel.strongGpuStrips=16;
        String output=render(parallel);
        check(output.startsWith("ULike v1.9.116 / 撮影 #4")&&output.contains("\n強ノイズ処理: GPU 16区間 / CPU 0区間"),"existing capture header and actual GPU count remain authoritative");
        ProcessingTiming1947.strongRegional19115(null,1,1);
        ProcessingTiming1947.Trace done=trace(5);ProcessingTiming1947.finish(done,false);
        ProcessingTiming1947.strongRegional19115(done,0,0);
        check(!done.strongRegionalObserved19115,"failed capture cannot acquire a later observation");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_regional_output_counts_verified\":true}");
    }
}
