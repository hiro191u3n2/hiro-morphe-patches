package com.hiro.ulike;

import java.lang.reflect.*;
import java.util.concurrent.*;

public final class ReflectionWarmup1981Test {
    static int assertions,initialized,constructed;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    public static final class MetadataTarget {static{initialized++;}public MetadataTarget(String value){constructed++;}private MetadataTarget(int value){}}
    public static void main(String[]args)throws Exception {
        String name="com.hiro.ulike.ReflectionWarmup1981Test$MetadataTarget";
        Class<?> type=ReflectionCache1945.metadataType1981(name);
        Class<?>[] parameters={String.class};Constructor<?> first=ReflectionCache1945.constructor1981(type,parameters);
        parameters[0]=Integer.class;
        check(initialized==0&&constructed==0,"metadata and public constructor lookup execute no initializer or constructor");
        check(first==ReflectionCache1945.constructor1981(type,new Class<?>[]{String.class}),"constructor metadata cache isolates caller parameter arrays");
        boolean hidden=false;try{ReflectionCache1945.constructor1981(type,new Class<?>[]{int.class});}catch(NoSuchMethodException expected){hidden=true;}
        check(hidden,"metadata cache retains public-only constructor semantics");
        check(ReflectionCache1945.type(name)==type&&initialized==1&&constructed==0,"actual type use still runs required class initialization");first.newInstance("same");check(constructed==1,"instances are created only on actual request");
        check(WarmProbe1981.initialized==0&&WarmProbe1981.constructed==0,"SDK controller not initialized before warmup");
        AsyncSave1935.warmupIdle1981();
        Field state=AsyncSave1935.class.getDeclaredField("warmState1981");state.setAccessible(true);
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(state.getInt(null)!=2&&System.nanoTime()<until)Thread.yield();
        check(state.getInt(null)==2,"idle metadata/preparation-thread warmup finishes");
        check(WarmProbe1981.initialized==0&&WarmProbe1981.constructed==0,"warmup never initializes or instantiates SDK save controller");
        Field prep=AsyncSave1935.class.getDeclaredField("PREP");prep.setAccessible(true);ThreadPoolExecutor executor=(ThreadPoolExecutor)prep.get(null);
        check(executor.getPoolSize()==1&&executor.getQueue().isEmpty(),"one empty handoff worker prestarted without dummy capture");
        AsyncSave1935.warmupIdle1981();check(executor.getPoolSize()==1,"repeated stable-preview observations do not add workers");
        boolean permit=SpeedWorkers1935.tryEnterAnalysis1981();check(permit,"idle analysis claims shared CPU permit without waiting");
        check(!SpeedWorkers1935.tryEnterAnalysis1981(),"nested analysis cannot claim a second thread permit");SpeedWorkers1935.leaveAnalysis1981(permit);check(SpeedWorkers1935.cpuIdle1944(),"analysis releases shared CPU permit");
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);
        Thread foreground=new Thread(()->{SpeedWorkers1935.run(new Runnable[]{()->{entered.countDown();try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}}});done.countDown();});foreground.start();check(entered.await(3,TimeUnit.SECONDS),"foreground shared worker active");
        check(!SpeedWorkers1935.tryEnterAnalysis1981(),"analysis immediately declines active foreground worker group");release.countDown();check(done.await(3,TimeUnit.SECONDS),"foreground job finishes normally");check(SpeedWorkers1935.cpuIdle1944(),"CPU permit/group accounting restored");
        System.out.println("PASS ReflectionWarmup1981 assertions="+assertions);
    }
}
