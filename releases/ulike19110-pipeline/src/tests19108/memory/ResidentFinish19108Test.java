package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.*;
import java.util.concurrent.CancellationException;
import static com.hiro.ulike.FinishMemory1992Test.*;

/** Actual Finish graph/admission with explicitly mocked transport. This proves
 * call scope, typed failure and cleanup, not GPU pixels or device latency. */
public final class ResidentFinish19108Test {
    static ThreadLocal<Object> current;
    @SuppressWarnings("unchecked") static void background()throws Exception {
        SaveQueue1935.idle=true;
        Class<?> cls=Class.forName("com.hiro.ulike.GpuQualification1961$Job");
        Constructor<?> constructor=cls.getDeclaredConstructor(String.class,long.class,GpuQualification1961.Probe.class,String.class,boolean.class);
        constructor.setAccessible(true);
        Object job=constructor.newInstance("resident108-controlled",0L,new GpuQualification1961.Probe(){public void run(GpuQualification1961.Cancellation c){}public void close(){}},"test",false);
        Field field=GpuQualification1961.class.getDeclaredField("CURRENT");field.setAccessible(true);current=(ThreadLocal<Object>)field.get(null);current.set(job);
    }
    static void prepare(int mode)throws Exception {FinishStageControl1988.reset(0,0);FinishMemoryControl1989.reset(mode,0);GpuNoise1960.busy=true;background();}
    static void clear(){current.remove();GpuNoise1960.busy=false;Thread.interrupted();}
    static void classify()throws Exception {
        Class<?> type=Class.forName("com.hiro.ulike.GpuChain1961$DeferredChain");Constructor<?> c=type.getDeclaredConstructor(int.class);c.setAccessible(true);
        RuntimeException memory=(RuntimeException)c.newInstance(4),execution=(RuntimeException)c.newInstance(3);
        check(GpuChain1961.memoryRefusal19108(memory),"typed reason4 classified");
        check(!GpuChain1961.memoryRefusal19108(execution),"typed execution refusal not memory");
        check(!GpuChain1961.memoryRefusal19108(new RuntimeException(memory)),"wrapped memory cause not inferred");
        check(!GpuChain1961.memoryRefusal19108(new RuntimeException("DeferredChain reason4")),"exception name/message not inferred");
        check(!GpuChain1961.memoryRefusal19108(new CancellationException("cancel")),"cancellation remains cancellation");
        check(!GpuChain1961.memoryRefusal19108(null),"null not memory");
    }
    static void compareRefusal(boolean modern)throws Exception {
        prepare(1);Bitmap source=image(80,80),expected=image(37,67);GpuNoise1960.Session carried=GpuNoise1960.open();
        for(int i=0;i<25;i++)carried.allocate(i,1024);
        try {
            if(modern){
                try{GpuChain1961.compareResidentFinish19108(source,0,37,67,plan(),false,expected,64,true,carried,false,()->false);throw new AssertionError("typed resident memory refusal lost");}
                catch(RuntimeException failure){check(GpuChain1961.memoryRefusal19108(failure),"resident full comparison preserves explicit memory refusal");}
            }else check(GpuChain1961.compareFinish1978(source,0,37,67,plan(),false,expected,64,true,carried,false,()->false)==ResidentProof1978.UNAVAILABLE,"historical comparison retains three-result API");
            check(carried.closed,"comparison closes actual carried graph owner before refusal delivery");
            check(FinishMemoryControl1989.trims==1&&FinishMemoryControl1989.checks==2,"actual carried comparison takes exactly one optional recovery");
            for(int i=0;i<24;i++)check(FinishStageControl1988.events.contains("release:"+i),"old Strong slot released before admission");
            check(!FinishStageControl1988.events.contains("release:24"),"memory refusal does not retire active image slot24");
        }finally{carried.close();source.recycle();expected.recycle();clear();}clean();
    }
    static void releaseBarrier()throws Exception {
        prepare(1);FinishStageControl1988.releaseFailure=1;Bitmap source=image(80,80),expected=image(37,67);GpuNoise1960.Session carried=GpuNoise1960.open();
        try {
            check(GpuChain1961.compareResidentFinish19108(source,0,37,67,plan(),false,expected,64,true,carried,false,()->false)==ResidentProof1978.UNAVAILABLE,"unsuccessful slot release stays execution unavailable");
            check(FinishMemoryControl1989.trims==0&&FinishMemoryControl1989.checks==0,"release failure never reaches scoped admission or trim");
        }finally {carried.close();source.recycle();expected.recycle();clear();}clean();
    }
    static void ordinaryBackground()throws Exception {
        prepare(1);Bitmap source=image(80,80),expected=image(37,67);
        try {
            check(GpuChain1961.compareFinish1978(source,0,37,67,plan(),false,expected,64,true,null,false,()->false)==ResidentProof1978.UNAVAILABLE,"ordinary background keeps unavailable result");
            check(FinishMemoryControl1989.trims==0&&FinishMemoryControl1989.checks==1,"ordinary background still refuses another active GPU without trimming");
        }finally {source.recycle();expected.recycle();clear();}clean();
    }
    static void recovered()throws Exception {
        prepare(2);Bitmap source=image(80,80),expected=image(37,67);GpuNoise1960.Session carried=GpuNoise1960.open();
        try {
            check(GpuChain1961.compareResidentFinish19108(source,0,37,67,plan(),false,expected,64,true,carried,false,()->false)==ResidentProof1978.EXACT,"successful same-budget recovery reaches unchanged complete comparator");
            check(FinishMemoryControl1989.trims==1&&FinishMemoryControl1989.checks==2,"comparison does not loop recovery");
        }finally {carried.close();source.recycle();expected.recycle();clear();}clean();
    }
    public static void main(String[] args)throws Exception {
        classify();compareRefusal(true);compareRefusal(false);releaseBarrier();ordinaryBackground();recovered();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"resident_memory_typed_refusal19108\":true,\"resident_memory_carried_scope19108\":true,\"actual_finish_graph\":true,\"native_transport_mocked\":true,\"physical_android_tested\":false}");
    }
}
