package com.hiro.ulike;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Deterministic two-bank ownership/lifecycle test. Real GLES pixel equivalence
 * remains covered by the unchanged SingleResidual1961Test fixtures. */
public final class ResidualOverlap1962Test {
    private static int assertions;
    private static void check(boolean okay,String message){assertions++;if(!okay)throw new AssertionError(message);}
    private static final ResidualOverlap1962.Guard GUARD=new ResidualOverlap1962.Guard(){public void check(){}};
    private static final class Ticket {
        final int begin,bank;final int[] snapshot;
        Ticket(int b,int k,int[] a){begin=b;bank=k;snapshot=a;}
    }
    private static class Driver implements ResidualOverlap1962.Driver {
        final int width=19,begin=3,end=134;
        final int[] source=new int[width*(end+2)],protection=new int[source.length];
        final Ticket[] pending=new Ticket[2];final List<String> calls=new ArrayList<String>();
        final AtomicInteger preparing=new AtomicInteger();
        final Thread owner=Thread.currentThread();
        int activeMax,submissions,collections,workerPreparations;String fault="";
        Driver(){for(int i=0;i<source.length;i++){source[i]=i*3+7;protection[i]=i%257;}}
        public float[] prepare(int lo,int hi){
            int live=preparing.incrementAndGet();activeMax=Math.max(activeMax,live);
            try {
                if(lo!=begin){check(Thread.currentThread()!=owner,"next band prepared by capped worker");workerPreparations++;
                    check(pending[0]!=null||pending[1]!=null,"CPU next band overlaps submitted GPU current");}
                if(lo!=begin&&fault.equals("prepare"))throw new IllegalStateException("injected preparation failure");
                if(lo!=begin&&fault.equals("nullPrepare"))return null;
                float[] data=new float[width*(hi-lo)];
                for(int i=0;i<data.length;i++)data[i]=source[lo*width+i]+protection[lo*width+i];
                return data;
            } finally {preparing.decrementAndGet();}
        }
        public Object submit(float[] records,int bank,int lo,int hi){
            check(Thread.currentThread()==owner,"GPU submission stays on controller");
            check(pending[bank]==null,"bank reused only after collection");
            check(bank==(submissions%2),"banks alternate");
            if(submissions>0)check(pending[bank^1]!=null,"next bank submits before current collection");
            if(lo!=begin&&fault.equals("submit"))return null;
            int[] snapshot=new int[records.length];for(int i=0;i<snapshot.length;i++)snapshot[i]=(int)records[i];
            Arrays.fill(records,-999); // Reuse after backend snapshots cannot affect GPU input.
            Ticket t=new Ticket(lo,bank,snapshot);pending[bank]=t;submissions++;
            calls.add("submit:"+lo);return t;
        }
        public boolean collect(Object object,int bank,int count,int[] target,int offset){
            Ticket t=(Ticket)object;check(pending[bank]==t&&t.bank==bank,"collect owns exact bank ticket");
            check(t.begin==begin+collections*64,"readback append order");
            check(offset==(t.begin-begin)*width,"direct readback appends at original row offset");
            if(fault.equals("collect"))return false;
            pending[bank]=null;collections++;calls.add("collect:"+t.begin);
            if(fault.equals("lateRead")){System.arraycopy(t.snapshot,0,target,offset,count-1);return false;}
            System.arraycopy(t.snapshot,0,target,offset,count);return true;
        }
        int[] reference(){int[] out=new int[width*(end-begin)];for(int i=0;i<out.length;i++)out[i]=source[begin*width+i]+protection[begin*width+i];return out;}
        void close(){check(preparing.get()==0,"CPU reader quiesced before session/source release");pending[0]=null;pending[1]=null;}
    }
    private static int[] run(Driver d){try{return ResidualOverlap1962.run(d.width,d.begin,d.end,64,d,GUARD);}finally{d.close();}}
    private static void exactAndFailures(){
        Driver d=new Driver();int[] before=d.source.clone(),policy=d.protection.clone();
        int[] out=run(d);check(Arrays.equals(out,d.reference()),"every pixel equals sequential source/protection baseline");
        check(Arrays.equals(before,d.source)&&Arrays.equals(policy,d.protection),"source and protection remain immutable");
        check(d.submissions==3&&d.collections==3&&d.workerPreparations==2,"three bands including short last band");
        check(d.activeMax==1,"one preparation worker maximum");
        check(d.calls.equals(Arrays.asList("submit:3","submit:67","collect:3","submit:131","collect:67","collect:131")),"two-ticket bounded ordered schedule");
        for(String fault:new String[]{"nullPrepare","submit","collect","lateRead"}){
            Driver failed=new Driver();failed.fault=fault;
            check(run(failed)==null,"failed "+fault+" discards entire private candidate");
            check(failed.pending[0]==null&&failed.pending[1]==null,"failed "+fault+" releases backend tickets");
        }
        Driver failed=new Driver();failed.fault="prepare";boolean thrown=false;
        try{run(failed);}catch(IllegalStateException expected){thrown=expected.getMessage().contains("injected");}
        check(thrown,"preparation exception propagates after quiescence");
        check(failed.pending[0]==null&&failed.pending[1]==null,"exception drains caller-owned session");
        check(Arrays.equals(run(new Driver()),new Driver().reference()),"worker remains usable after failed preparation");
    }
    private static void await(CountDownLatch latch,String label)throws Exception{check(latch.await(5,TimeUnit.SECONDS),label);}
    private static void cancellationQuiescence()throws Exception {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),stopping=new CountDownLatch(1),finished=new CountDownLatch(1);
        final AtomicBoolean cancelled=new AtomicBoolean(),interruptPreserved=new AtomicBoolean();
        final AtomicInteger sourceReaders=new AtomicInteger();
        final Throwable[] failure=new Throwable[1];
        final Thread controller=new Thread(new Runnable(){public void run(){
            final Thread own=Thread.currentThread();
            ResidualOverlap1962.Driver d=new ResidualOverlap1962.Driver(){
                public float[] prepare(int lo,int hi){
                    sourceReaders.incrementAndGet();
                    try {
                        if(Thread.currentThread()!=own){
                            entered.countDown();
                            for(;;)try{release.await();break;}catch(InterruptedException ignored){stopping.countDown();}
                        }
                        return new float[(hi-lo)*2];
                    }finally{sourceReaders.decrementAndGet();}
                }
                public Object submit(float[] a,int bank,int lo,int hi){return new Ticket(lo,bank,new int[a.length]);}
                public boolean collect(Object t,int bank,int count,int[] target,int offset){System.arraycopy(((Ticket)t).snapshot,0,target,offset,count);return true;}
            };
            try{ResidualOverlap1962.run(2,0,131,64,d,GUARD);failure[0]=new AssertionError("cancelled route returned a candidate");}
            catch(CancellationException expected){cancelled.set(true);interruptPreserved.set(Thread.currentThread().isInterrupted());}
            catch(Throwable unexpected){failure[0]=unexpected;}
            finally{finished.countDown();}
        }},"residual-cancellation-controller");
        controller.start();await(entered,"next preparation actually borrowing source");
        controller.interrupt();await(stopping,"cancelled controller interrupts preparation lease");
        check(sourceReaders.get()==1,"native-like uninterruptible reader remains active until actual finish");
        check(finished.getCount()==1,"controller cannot release source before worker quiescence");
        release.countDown();await(finished,"cancelled controller finishes after worker");controller.join(5000);
        check(failure[0]==null,"no unexpected cancellation exception: "+failure[0]);
        check(cancelled.get()&&interruptPreserved.get(),"cancel throws and preserves caller interrupt");
        check(sourceReaders.get()==0,"no borrowed source reader survives cancellation");
        check(Arrays.equals(run(new Driver()),new Driver().reference()),"cancel interrupt does not contaminate next worker task");
    }
    public static void main(String[] args)throws Exception {
        exactAndFailures();cancellationQuiescence();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"bounded_gpu_banks\":2,\"preparation_workers\":1,\"ordered_output_exact\":true,\"source_quiescence_verified\":true,\"physical_android_tested\":false}");
    }
}
