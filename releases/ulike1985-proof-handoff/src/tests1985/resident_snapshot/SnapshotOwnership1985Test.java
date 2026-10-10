package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Real copy admission with controlled captures; no bitmap allocation or GPU. */
public final class SnapshotOwnership1985Test {
    static final long M=1024L*1024;
    static int assertions;
    static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static void capture(){ProcessingTiming1947.epoch++;}
    static GpuSnapshotBudget1981.Copy claim(long bytes,int family){return GpuSnapshotBudget1981.tryCopy(bytes,family);}

    static void unstartedPreviousCapture(final boolean baseline)throws Exception {
        capture();
        final CountDownLatch claimed=new CountDownLatch(1),resume=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        final boolean[] began={true};
        Thread oldThread=new Thread(new Runnable(){public void run(){
            GpuSnapshotBudget1981.Copy old=null;
            try {
                old=claim(M,0);if(old==null)throw new AssertionError("old scalar claim was refused");
                claimed.countDown();
                if(!resume.await(5,TimeUnit.SECONDS))throw new AssertionError("old copy fixture barrier timed out");
                began[0]=old.begin();
            }catch(Throwable error){failure.set(error);}
            finally{if(old!=null){old.close();old.close();}}
        }},"snapshot85-old-unstarted");
        oldThread.start();
        GpuSnapshotBudget1981.Copy fresh=null;
        try {
            check(claimed.await(5,TimeUnit.SECONDS),"old thread only reserved a scalar copy token");
            capture();
            long started=System.nanoTime();fresh=claim(M,0);
            check(System.nanoTime()-started<1000000000L,"new capture admission does not wait for the paused old caller");
            if(baseline){
                check(fresh==null,"published .84 unstarted stale owner blocks the new capture");
            }else{
                check(fresh!=null,"current capture revokes only the stale unstarted copy owner");
                check(fresh.begin(),"new owner begins while the obsolete caller remains paused");
            }
        }finally{
            resume.countDown();oldThread.join(5000);
        }
        check(!oldThread.isAlive()&&failure.get()==null,"old caller exits without an exception");
        check(!began[0],"obsolete token cannot begin a copy after capture changes");
        if(!baseline){
            check(fresh.current(),"late close from old token cannot invalidate the replacement owner");
            check(claim(1,0)==null,"late old close cannot permit overlapping real copies");
            fresh.close();fresh.close();
        }
        GpuSnapshotBudget1981.Copy next=claim(M,0);
        check(next!=null&&next.begin(),"copy budget progresses after the actual owner closes");
        next.close();
    }

    static void startedPreviousCapture(){
        capture();GpuSnapshotBudget1981.Copy old=claim(M,0);
        check(old!=null&&old.begin(),"old capture has actually begun a proof allocation");
        capture();check(!old.current(),"new capture invalidates old proof publication");
        check(claim(M,0)==null,"a started old allocation stays exclusive until its owner closes");
        check(!old.begin(),"started token cannot allocate a second time");
        old.close();GpuSnapshotBudget1981.Copy fresh=claim(M,0);
        check(fresh!=null&&fresh.begin(),"new capture proceeds after old allocation ownership ends");
        old.close();check(claim(M,0)==null,"repeated old close cannot release the replacement allocation");
        fresh.close();
    }

    static void sameCaptureAndBounds(){
        capture();GpuSnapshotBudget1981.Copy held=claim(16*M,0);
        check(held!=null&&held.current(),"tentative token is current before its allocation starts");
        check(claim(1,0)==null,"another caller cannot bypass a same-capture tentative owner");
        held.close();GpuSnapshotBudget1981.Copy first=claim(16*M,0);
        check(first!=null&&first.begin(),"unstarted refusal refunds this capture's copy allowance");
        check(!first.begin(),"one token spends its allowance only once");first.close();
        GpuSnapshotBudget1981.Copy second=claim(16*M,0);
        check(second!=null&&second.begin(),"two bounded strips can use exactly 32 MiB");second.close();
        check(claim(1,0)==null,"a third strip cannot exceed the unchanged 32 MiB cap");
        capture();GpuSnapshotBudget1981.Copy large=claim(96*M,0);
        check(large!=null&&large.begin(),"one complete oversized strip can fit the unchanged 96 MiB cap");large.close();
        check(claim(1,0)==null,"one oversized strip cannot turn into several copies");
        capture();check(claim(96*M+1,0)==null,"over 96 MiB is rejected before any allocation starts");
        check(claim(0,0)==null&&claim(-1,0)==null,"invalid byte counts cannot acquire a token");
        Thread.currentThread().interrupt();check(claim(M,0)==null,"interrupted capture cannot acquire a token");
        check(Thread.currentThread().isInterrupted(),"admission preserves the interrupt flag");Thread.interrupted();
        GpuSnapshotBudget1981.Copy cancelled=claim(M,0);check(cancelled!=null,"token is available after cancellation is cleared");
        Thread.currentThread().interrupt();check(!cancelled.begin(),"interruption before copy prevents allocating");
        cancelled.close();check(Thread.currentThread().isInterrupted(),"closing a cancelled token preserves interruption");Thread.interrupted();
    }

    static void roundRobin(){
        for(int[] order:new int[][]{{1,2,4},{4,2,1},{2,4,1}}){
            int[] counts=new int[3];
            for(int shot=0;shot<9;shot++){
                capture();for(int family:order)GpuSnapshotBudget1981.offerWhole(family);
                int copies=0;
                for(int family:order){GpuSnapshotBudget1981.Copy copy=claim(80*M,family);
                    if(copy!=null)try{check(copy.begin(),"selected whole family begins");copies++;counts[family==1?0:family==2?1:2]++;}finally{copy.close();}
                }
                check(copies==1,"one capture permits exactly one eligible whole-image copy");
            }
            check(Arrays.equals(counts,new int[]{3,3,3}),"Resident, finish and moire rotate regardless of their call order");
        }
        int resident=0,moire=0;
        for(int shot=0;shot<6;shot++){
            capture();for(int family:new int[]{1,2,4})GpuSnapshotBudget1981.offerWhole(family);
            for(int family:new int[]{1,4}){GpuSnapshotBudget1981.Copy copy=claim(80*M,family);
                if(copy!=null)try{check(copy.begin(),"an available whole family makes progress");if(family==1)resident++;else moire++;}finally{copy.close();}}
        }
        check(resident==2&&moire==2,"permanently resource-denied finish spends its turn without starving Resident");
        int streak=0,largest=0;
        for(int shot=0;shot<36;shot++){
            capture();GpuSnapshotBudget1981.offerWhole(1);
            if(shot%2==0)GpuSnapshotBudget1981.offerWhole(2);
            if(shot%3!=0)GpuSnapshotBudget1981.offerWhole(4);
            boolean copied=false;
            for(int family:new int[]{4,2,1}){
                if(family==2&&shot%2!=0||family==4&&shot%3==0)continue;
                GpuSnapshotBudget1981.Copy copy=claim(80*M,family);
                if(copy!=null)try{check(copy.begin(),"changing optional family remains within a single copy");if(family==1)copied=true;}finally{copy.close();}
            }
            streak=copied?0:streak+1;largest=Math.max(largest,streak);
        }
        check(largest<=2,"continuously offered Resident gets its turn within three fresh captures when other gates are open");
    }

    public static void main(String[] args)throws Exception {
        boolean baseline=args.length>0&&args[0].equals("published84");
        unstartedPreviousCapture(baseline);startedPreviousCapture();sameCaptureAndBounds();roundRobin();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"snapshot_unstarted_stale_owner1985\":"+(!baseline)+
            ",\"published84_stale_unstarted_copy_reproduced1985\":"+baseline+
            ",\"snapshot_started_owner_exclusion1985\":true,\"snapshot_copy_caps1985_preserved\":true,\"snapshot_whole_family_fairness1985\":true}");
    }
}
