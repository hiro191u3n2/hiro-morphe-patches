package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual CPU worker pool, immutable production masks and original policy
 * oracle. A task-entry barrier makes active cancellation deterministic. */
public final class CpuFinishOwnership1978Test {
    static long checks,words;
    static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    static QualityPixels1932.Plan plan(){return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(5.3f,7.4f,121f,.2f,100),200,10000000L,2,.83f,4,4,true,true,1f);}
    static QualityPixels1932.Plan known(int width,int rows)throws Exception {
        byte[] skin=new byte[99],detail=new byte[99];new Random(1978).nextBytes(skin);new Random(978).nextBytes(detail);
        Class<?> type=Class.forName("com.hiro.ulike.QualityPipeline1932$SmoothRegions1958");
        Constructor<?> ctor=type.getDeclaredConstructor(int.class,int.class);ctor.setAccessible(true);Object smooth=ctor.newInstance(width,rows);
        new Random(781).nextBytes((byte[])field(type,"confidence").get(smooth));
        return plan().withFaceRegions(FaceRegions1934.uprightRaster(width,rows,11,9,skin,detail,0)).withSmoothedRegions((QualityPixels1932.SmoothMask)smooth)
            .withOutputNoise(SpatialNoise1934.fromGpu1961(width,rows,3,2,new float[]{.1f,2.1f,4.8f,3.3f,7.5f,32f},new QualityPixels1932.NoiseStats(3.1f,4.2f,121f,.2f,100)));
    }
    static void same(FinishPolicy1953.Band a,FinishPolicy1953.Band b,String why) {
        check(a!=null&&b!=null&&a.mode==b.mode&&a.pixels==b.pixels&&a.rawFallback==b.rawFallback,why+" representation");
        check(Arrays.equals(a.words,b.words),why+" complete integer policy");words+=a.words.length;
    }
    static final class Unknown implements QualityPixels1932.RegionMask,QualityPixels1932.SmoothMask {
        int calls;long order;
        int next(int x,int y,int op){calls++;order=order*31+x*17L+y*7L+op;return calls%3==0?Integer.MAX_VALUE:x*1978-y*713+calls;}
        public int skinQ8(int x,int y){return next(x,y,1);}public int detailQ8(int x,int y){return next(x,y,2);}public int smoothingQ8(int x,int y){return next(x,y,3);}
    }
    static void fallbacks()throws Exception {
        Unknown left=new Unknown(),right=new Unknown();QualityPixels1932.Plan a=plan().withFaceRegions(left).withSmoothedRegions(left),b=plan().withFaceRegions(right).withSmoothedRegions(right);
        check(!CpuFinishPolicy1978.immutable(a),"unknown callbacks not parallelized or cached");
        try(FinishPolicy1953.Band old=FinishPolicy1953.prepareCpu1961(a,71,83,3,80,-7);FinishPolicy1953.Band fresh=CpuFinishPolicy1978.prepareCandidate(b,71,83,3,80,-7,4)) {
            same(old,fresh,"unknown fallback");check(left.calls==right.calls&&left.order==right.order,"unknown callback order and RAW4 replay retained");
        }
        QualityPixels1932.Plan p=known(71,83);check(CpuFinishPolicy1978.immutable(p),"known owned mask graph admitted");
        PolicyCache1945 lease=PolicyCache1945.borrow(p,71,0,83);check(lease!=null,"actual worker-bound policy lease");
        try {QualityPixels1932.Plan scoped=p.withPolicyCache(lease);check(!CpuFinishPolicy1978.immutable(scoped),"worker-bound lease cannot cross worker boundary");
            try(FinishPolicy1953.Band old=FinishPolicy1953.prepareCpu1961(scoped,71,83,3,80,0);FinishPolicy1953.Band fresh=CpuFinishPolicy1978.prepareCandidate(scoped,71,83,3,80,0,4)){same(old,fresh,"scoped original route");}
        }finally{lease.close();}
        try(FinishPolicy1953.Band old=FinishPolicy1953.prepareCpu1961(p,71,83,3,80,0)) {
            for(int workers:new int[]{1,2,4})try(FinishPolicy1953.Band fresh=CpuFinishPolicy1978.prepareCandidate(p,71,83,3,80,0,workers)){same(old,fresh,"all parallel partitions "+workers);}
        }
        Method retained=CpuFinishPolicy1978.class.getDeclaredMethod("retainedPlan",QualityPixels1932.Plan.class);retained.setAccessible(true);
        check((Long)retained.invoke(null,p)>512&&(Long)retained.invoke(null,p)<1024*1024,"known primitive mask graph accounted");
        check((Long)retained.invoke(null,a)==Long.MAX_VALUE,"unknown graph cannot be retained by probe");
    }
    static void cancellation()throws Exception {
        final QualityPixels1932.Plan p=known(71,83);final int length=71*77*4;
        SpeedWorkers1935.trim();final int[] owned=SpeedWorkers1935.borrowInts(length);Arrays.fill(owned,0x13579bdf);SpeedWorkers1935.release(owned);
        CountDownLatch entered=new CountDownLatch(4),release=new CountDownLatch(1);AtomicInteger completed=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<Throwable>();AtomicReference<FinishPolicy1953.Band> result=new AtomicReference<FinishPolicy1953.Band>();AtomicBoolean interrupted=new AtomicBoolean();
        SpeedWorkers1935.installHints(new SpeedWorkers1935.Hints(){
            public Runnable worker(Runnable loop){return loop;}
            public Object begin(long target){entered.countDown();try{release.await();}catch(InterruptedException cancelled){Thread.currentThread().interrupt();}return this;}
            public void complete(Object token,boolean success){completed.incrementAndGet();}
        });
        Thread caller=new Thread(()->{try{result.set(CpuFinishPolicy1978.prepareCandidate(p,71,83,3,80,0,4));}catch(Throwable t){error.set(t);}finally{interrupted.set(Thread.currentThread().isInterrupted());}});
        int[] other=null,recovered=null;
        try {
            caller.start();check(entered.await(10,TimeUnit.SECONDS),"all four actual policy workers active");
            other=SpeedWorkers1935.borrowInts(length);check(other!=owned,"active policy storage cannot be leased twice");
            caller.interrupt();caller.join(10000);check(!caller.isAlive(),"cancel joins every policy worker");
            check(error.get() instanceof IllegalStateException&&result.get()==null&&interrupted.get(),"cancel publishes no partial band and preserves interrupt");
            check(completed.get()==4&&SpeedWorkers1935.cpuIdle1944(),"all delegates and CPU permits drained before return");
            recovered=SpeedWorkers1935.borrowInts(length);check(recovered==owned,"private raw buffer returned after join");
            Arrays.fill(recovered,0x2468ace0);SpeedWorkers1935.installHints(null);
            SpeedWorkers1935.run(new Runnable[]{()->{},()->{},()->{},()->{}});
            for(int value:recovered)check(value==0x2468ace0,"no stale policy write after buffer return");
        }finally {
            release.countDown();caller.interrupt();caller.join(10000);SpeedWorkers1935.installHints(null);
            if(result.get()!=null)result.get().close();SpeedWorkers1935.release(other);SpeedWorkers1935.release(recovered);
        }
        Thread.currentThread().interrupt();boolean cancelled=false;
        try{CpuFinishPolicy1978.prepareCandidate(p,71,83,3,80,0,4);}catch(IllegalStateException yes){cancelled=true;}finally{Thread.interrupted();}
        check(cancelled&&SpeedWorkers1935.cpuIdle1944(),"already cancelled policy has no running tasks");
    }
    static void memory()throws Exception {
        QualityPixels1932.Plan p=known(71,83);String key=CpuFinishPolicy1978.key(p,71,83,3,80,0,4,false);
        Cpu77Differential1978.candidate=true;Cpu77Differential1978.force(key);
        Field held=field(GpuNoise1960.class,"retained"),busy=field(GpuNoise1960.class,"activeSession");long prior=held.getLong(null);boolean active=busy.getBoolean(null);
        AtomicInteger parallel=new AtomicInteger();SpeedWorkers1935.installHints(new SpeedWorkers1935.Hints(){public Runnable worker(Runnable r){return r;}public Object begin(long n){parallel.incrementAndGet();return this;}public void complete(Object t,boolean ok){}});
        try {held.setLong(null,Long.MAX_VALUE);busy.setBoolean(null,true);
            try(FinishPolicy1953.Band old=FinishPolicy1953.prepareCpu1961(p,71,83,3,80,0);FinishPolicy1953.Band fresh=CpuFinishPolicy1978.prepare(p,71,83,3,80,0)){same(old,fresh,"memory decline uses original");}
            check(parallel.get()==0,"no optional parallel raw allocation under physical pressure");
        }finally{held.setLong(null,prior);busy.setBoolean(null,active);SpeedWorkers1935.installHints(null);}
    }
    public static void main(String[] args)throws Exception {
        fallbacks();cancellation();memory();SpeedWorkers1935.trim();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+(checks+words)+",\"policy_words_compared\":"+words+",\"cpu78_policy_parallel_unknown_and_scoped_exact\":true,\"cpu78_policy_cancel_join_and_budget_verified\":true,\"cpu78_policy_immutable_snapshot_accounted\":true}");
    }
}
