package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/** Real Resident producer, proof comparison and copy budget with controlled peers. */
public final class ResidentTicket1984Test {
    static int assertions;
    static void check(boolean value,String why) {
        assertions++;
        if(!value)throw new AssertionError(why);
    }
    static void reset()throws Exception {
        ResidentDiagnostics1982Test.reset();
        GpuQualification1961.resetTicket1984();
        GpuQualification1961.declineReason1983=13;
        GpuQualification1961.scheduleReason1983=12;
        GpuQualification1961.scheduleCalls1983=0;
        GpuQualification1961.afterDecision1983=GpuQualification1961.afterSchedule1983=null;
        GpuQualification1961.exact=GpuQualification1961.slow=GpuQualification1961.admissions=GpuQualification1961.checks=0;
        GpuQualification1961.cancelCheck=-1;
        ProcessingTiming1947.queueReason1983=-1;
        QualityPipeline1932.cpu=QualityPipeline1932.normal=QualityPipeline1932.resident=0;
        QualityPipeline1932.normalCompare=QualityPipeline1932.residentCompare=QualityPipeline1932.calls=QualityPipeline1932.dependency=0;
        QualityPipeline1932.cancelAt=-1;
        QualityPipeline1932.mismatch=QualityPipeline1932.slow=QualityPipeline1932.dependencyUnavailable=QualityPipeline1932.changingBaseline=false;
        QualityPipeline1932.lastHalf=null;QualityPipeline1932.snapshot=QualityPipeline1932.oracleLive=null;
        QualityPipeline1932.order.clear();
    }
    static Bitmap image(){return ResidentDiagnostics1982Test.image();}
    static Bitmap invoke(Bitmap b){return ResidentDiagnostics1982Test.invoke(b);}
    static long bytes(Bitmap b){return ResidentProof1978.retainedBytes(b,ResidentDiagnostics1982Test.plan(),ResidentDiagnostics1982Test.plan());}
    static void noCopyLock()throws Exception {
        Field copying=GpuSnapshotBudget1981.class.getDeclaredField("copying");copying.setAccessible(true);
        check(!copying.getBoolean(null),"actual foreground copy token is released");
    }
    static void clean(Bitmap b,int releases)throws Exception {
        check(!b.isRecycled()&&b.recycleCalls1984==0,"caller bitmap lifetime is unchanged");
        check(GpuQualification1961.queued==null&&GpuQualification1961.reserved1984==0,"no queued or reserved owner leaks");
        check(GpuQualification1961.held==0&&GpuQualification1961.releases1984==releases,"exact retained byte ownership is released once");
        check(GpuQualification1961.lastReservation1984==null||!GpuQualification1961.lastReservation1984.owns,"ticket no longer owns bytes");
        noCopyLock();
    }
    static void threadState() {
        check(!GpuResident1976.benchmarking()&&!GpuResident1976.cpuOracle()&&GpuResident1976.cancellation()==null,
              "Resident benchmark, oracle and cancellation thread state is cleared");
    }
    static Bitmap soleClone(int created) {
        check(Bitmap.all.size()==created+1,"one detached bitmap was allocated");
        return Bitmap.all.get(created);
    }
    static void oneRecycle(Bitmap clone) {
        check(clone.isRecycled()&&clone.recycleCalls1984==1,"snapshot recycle is called exactly once");
    }
    static void beforeCopyReservation()throws Exception {
        reset();final Bitmap source=image();final long retained=bytes(source);final int before=Bitmap.copies;
        Bitmap.beforeCopy1984=new Runnable(){public void run(){
            if(!GpuQualification1961.background()) {
                GpuQualification1961.events1984.add("copy");
                check(GpuQualification1961.reservationCalls1984==1&&GpuQualification1961.reserved1984==1,
                      "one ticket reserves a job slot before Bitmap.copy");
                check(GpuQualification1961.held==retained&&GpuQualification1961.lastReservation1984.owns,
                      "the complete known retention is held before Bitmap.copy");
            }
        }};
        int created=Bitmap.all.size();check(invoke(source)==null,"uncertified foreground stays on CPU");
        Bitmap clone=soleClone(created);
        check(Bitmap.copies==before+1&&clone!=source&&!clone.isRecycled(),"one immutable snapshot survives successful enqueue");
        check(GpuQualification1961.events1984.equals(Arrays.asList("reserve","copy","commit")),"real producer orders reserve, copy and commit");
        check(GpuQualification1961.queueCalls==1&&GpuQualification1961.scheduleCalls1983==0&&GpuQualification1961.commitCalls1984==1,
              "producer never performs a second admission lookup or legacy schedule");
        check(GpuQualification1961.held==retained&&GpuQualification1961.reserved1984==0&&GpuQualification1961.releases1984==0,
              "commit transfers retained ownership without releasing then reacquiring");
        check(ProcessingTiming1947.reason==2&&ProcessingTiming1947.queueReason1983==1,"accepted ticket records scheduled result");
        check(ProcessingTiming1947.queueHeld1983==retained&&ProcessingTiming1947.queueRequested1983==retained,
              "saved queue snapshot retains the actual reserved byte amount");
        noCopyLock();GpuQualification1961.drop();oneRecycle(clone);clean(source,1);
        check(GpuQualification1961.probeCloses1984==1,"queued probe owns the single snapshot release");

        reset();Bitmap denied=image();long required=bytes(denied);GpuQualification1961.limit1984=required-1;
        beforeDenied(denied);
        // Refusal before begin does not spend this photo's copy allowance.
        GpuQualification1961.limit1984=96L*1024*1024;
        int copies=Bitmap.copies;check(invoke(denied)==null&&GpuQualification1961.queued!=null,"same-photo retry can reserve after a refusal without copying");
        check(Bitmap.copies==copies+1,"only the accepted retry makes a copy");GpuQualification1961.drop();clean(denied,1);

        reset();denied=image();denied.slack=100*1024*1024;copies=Bitmap.copies;
        invoke(denied);check(ProcessingTiming1947.reason==5&&Bitmap.copies==copies&&GpuQualification1961.reservationCalls1984==0,
              "known retention over 96 MiB is rejected before reservation and copy");clean(denied,0);
    }
    static void beforeDenied(Bitmap source)throws Exception {
        int copies=Bitmap.copies,created=Bitmap.created;
        invoke(source);
        check(ProcessingTiming1947.reason==6&&ProcessingTiming1947.queueReason1983==16,"reservation budget refusal is recorded from its result");
        check(Bitmap.copies==copies&&Bitmap.created==created&&GpuQualification1961.commitCalls1984==0,
              "insufficient ticket bytes cause zero copies and zero commits");
        clean(source,0);
    }
    static void cancellationsAndCopies()throws Exception {
        for(int moment=0;moment<3;moment++) {
            reset();Bitmap source=image();int created=Bitmap.all.size(),copies=Bitmap.copies;
            Runnable cancel=new Runnable(){public void run(){ProcessingTiming1947.epoch++;}};
            if(moment==0)GpuQualification1961.afterReserve1984=cancel;
            else if(moment==1)Bitmap.afterCopy=cancel;
            else GpuQualification1961.beforeCommit1984=cancel;
            invoke(source);
            check(Bitmap.copies==copies+(moment==0?0:1),"epoch cancellation takes no stale pre-copy image");
            check(ProcessingTiming1947.reason==(moment==2?6:11),"cancellation preserves producer-versus-commit terminal classification");
            if(moment==2)check(ProcessingTiming1947.queueReason1983==6,"commit cancellation is captured as the returned interrupted reason");
            if(moment!=0)oneRecycle(soleClone(created));
            clean(source,1);
            check(GpuQualification1961.probeCloses1984==(moment==2?1:0),"only transferred ownership is closed by the commit peer");
        }
        for(int fault=1;fault<=3;fault++) {
            reset();Bitmap source=image();int created=Bitmap.created,copies=Bitmap.copies;
            Bitmap.copyFault=fault;invoke(source);
            check(Bitmap.created==created&&Bitmap.copies==copies+1,"null, OOM or cancelled copy does not return a detached bitmap");
            check(ProcessingTiming1947.reason==(fault==3?11:12),"copy failure classification retained");
            clean(source,1);
            Bitmap.copyFault=0;copies=Bitmap.copies;invoke(source);
            check(ProcessingTiming1947.reason==10&&Bitmap.copies==copies,"failed allocation still spends this capture's bounded copy attempt");
        }
        reset();Bitmap source=image();source.copySlack=4096;int created=Bitmap.all.size();invoke(source);
        oneRecycle(soleClone(created));
        check(ProcessingTiming1947.reason==5&&GpuQualification1961.commitCalls1984==0,
              "actual clone allocation larger than the reserved amount is never committed");clean(source,1);
        reset();source=image();Bitmap.afterCopy=new Runnable(){public void run(){Thread.currentThread().interrupt();}};
        created=Bitmap.all.size();invoke(source);oneRecycle(soleClone(created));
        check(Thread.currentThread().isInterrupted()&&ProcessingTiming1947.reason==11,"copy-stage interrupt remains set for the caller");
        Thread.interrupted();clean(source,1);
    }
    static void commitRaces()throws Exception {
        for(final int reason:new int[]{8,11}) {
            reset();Bitmap source=image();int created=Bitmap.all.size();
            GpuQualification1961.beforeCommit1984=new Runnable(){public void run(){
                String key=GpuQualification1961.lastReservation1984.reservedKey;
                if(reason==8)GpuQualification1961.negatives.add(key);
                else GpuQualification1961.records.put(key,new GpuQualification1961.Record(100,1));
            }};
            GpuQualification1961.afterCommit1984=new Runnable(){public void run(){
                GpuQualification1961.negatives.clear();GpuQualification1961.records.clear();
                GpuQualification1961.scheduleReason1983=17;
            }};
            invoke(source);oneRecycle(soleClone(created));clean(source,1);
            check(GpuQualification1961.probeCloses1984==1&&GpuQualification1961.lastReservation1984.effectiveReleases==1,
                  "certification race closes the passed probe and held bytes once");
            check(ProcessingTiming1947.reason==6&&ProcessingTiming1947.queueReason1983==reason,
                  "saved certification refusal uses the returned decision despite later live changes");
            int saved=ProcessingTiming1947.queueReason1983;long held=ProcessingTiming1947.queueHeld1983;
            GpuQualification1961.outcome1984("later_result");GpuQualification1961.progress1984("later_phase",1,3);
            check(ProcessingTiming1947.queueReason1983==saved&&ProcessingTiming1947.queueHeld1983==held,
                  "later diagnostic activity cannot mutate the saved failure label or held bytes");
        }
    }
    static void exceptionOwnership()throws Exception {
        for(int where=0;where<3;where++)for(int fault=1;fault<=5;fault++) {
            reset();Bitmap source=image();int created=Bitmap.all.size(),copies=Bitmap.copies;
            if(where==0)GpuQualification1961.reserveFault1984=fault;
            else if(where==1){GpuQualification1961.currentFaultAt1984=2;GpuQualification1961.currentFault1984=fault;}
            else GpuQualification1961.commitFault1984=fault;
            boolean fatal=false;
            try{invoke(source);}catch(AssertionError expected){fatal=true;}
            check(fatal==(fault==5),"fatal errors propagate while handled queue failures retain fallback");
            check(ProcessingTiming1947.reason==(fault==4?11:13),"exception terminal classification retained");
            check(Bitmap.copies==copies+(where==0?0:1),"failed reserve allocates nothing; later failures have only one copy");
            if(where!=0)oneRecycle(soleClone(created));
            clean(source,where==0?0:1);
        }
        for(int fault:new int[]{6,7,8,9,10}) {
            reset();Bitmap source=image();int created=Bitmap.all.size();GpuQualification1961.commitFault1984=fault;
            boolean fatal=false;try{invoke(source);}catch(AssertionError expected){fatal=true;}
            check(fatal==(fault==10),"post-close fatal exception remains observable");
            oneRecycle(soleClone(created));clean(source,1);
            check(GpuQualification1961.probeCloses1984==1,"exception after rejected ownership close never closes snapshot twice");
        }
        reset();Bitmap source=image();invoke(source);check(GpuQualification1961.queued!=null,"next capture remains schedulable after all failure cases");
        GpuQualification1961.drop();clean(source,1);
    }
    static void proofRun(boolean diagnosticFailure)throws Exception {
        reset();Bitmap source=image();int created=Bitmap.all.size();
        if(diagnosticFailure)GpuQualification1961.diagnosticFault1984=2;
        invoke(source);source.pixels[0]=0xff654321;
        check(GpuQualification1961.queued!=null,"proof job is owned by the queue");GpuQualification1961.run();
        check(QualityPipeline1932.cpu==2&&QualityPipeline1932.normalCompare==2&&QualityPipeline1932.residentCompare==2,
              "two independent CPU references and both complete candidate comparisons remain required");
        check(QualityPipeline1932.normal==2&&QualityPipeline1932.resident==2,
              "both foreground-shaped timings execute in each trial");
        List<String> trial=Arrays.asList("cpu","ordinary_compare","resident_compare","ordinary_time","resident_time",
                                         "cpu","ordinary_compare","resident_compare","ordinary_time","resident_time");
        check(QualityPipeline1932.order.equals(trial),"CPU proof, full comparisons and real timing order is unchanged");
        check(GpuQualification1961.admissions==1&&GpuQualification1961.exact==0&&GpuQualification1961.slow==0,
              "only complete exact comparisons plus an actual timing win certify the candidate");
        check(GpuQualification1961.cpuTime1984>GpuQualification1961.gpuTime1984&&GpuQualification1961.gpuTime1984>0,
              "timing diagnostics use measured completed CPU and candidate intervals");
        check(GpuQualification1961.phases1984.contains("resident_compare:2:2")&&GpuQualification1961.phases1984.contains("resident_speed:2:2"),
              "progress reaches both completed full trials");
        check(GpuQualification1961.diagnosticCalls1984>0,"real Resident called its optional diagnostic wrappers");
        for(int i=created;i<Bitmap.all.size();i++)oneRecycle(Bitmap.all.get(i));
        check(source.pixels[0]==0xff654321,"queued proof never overwrites subsequent foreground source data");
        clean(source,1);threadState();
        int copies=Bitmap.copies,reservations=GpuQualification1961.reservationCalls1984;
        Bitmap result=invoke(source);
        check(result!=null&&ProcessingTiming1947.reason==1,"new exact proof enables the existing foreground resident route");
        check(Bitmap.copies==copies&&GpuQualification1961.reservationCalls1984==reservations,
              "certified foreground does not create another qualification snapshot or ticket");
        result.recycle();threadState();
    }
    static void rejectedProofs()throws Exception {
        for(int mode=0;mode<7;mode++) {
            reset();Bitmap source=image();int created=Bitmap.all.size();
            if(mode==0)QualityPipeline1932.mismatch=true;
            if(mode==1)QualityPipeline1932.unavailable=true;
            if(mode==2)QualityPipeline1932.changingBaseline=true;
            if(mode==3)QualityPipeline1932.slow=true;
            if(mode==4)QualityPipeline1932.cancelAt=1;
            if(mode==5){GpuChain1961.baseline=null;QualityPipeline1932.dependencyUnavailable=true;}
            if(mode==6)source.pixels[source.pixels.length-1]=0x00123456;
            invoke(source);GpuQualification1961.run();
            check(GpuQualification1961.admissions==0,"incomplete, mismatched, unstable, slow or cancelled output never certifies");
            check(GpuQualification1961.exact==(mode==0?1:0),"only measured pixel mismatch persists exact rejection");
            check(GpuQualification1961.slow==((mode==3||mode==5)?1:0),"speed/unavailable dependency stays separate from exact quality rejection");
            String[] outcomes={"quality_mismatch","comparison_incomplete","reference_unstable","speed_condition",null,"baseline_unavailable","unsupported"};
            if(outcomes[mode]!=null)check(outcomes[mode].equals(GpuQualification1961.outcome1984),"specific actual attempt outcome is preserved");
            for(int i=created;i<Bitmap.all.size();i++)oneRecycle(Bitmap.all.get(i));
            clean(source,1);threadState();
        }
        reset();Bitmap source=image();GpuChain1961.baseline=null;invoke(source);GpuQualification1961.run();
        check(QualityPipeline1932.dependency==1&&GpuQualification1961.admissions==1,"missing ordinary baseline is established before complete resident proof");
        check(GpuQualification1961.phases1984.contains("resident_baseline:0:1")&&GpuQualification1961.phases1984.contains("resident_baseline:1:1"),
              "dependency progress records actual before and after states");clean(source,1);threadState();
        reset();source=image();invoke(source);GpuNoise1960.fits=false;GpuQualification1961.run();
        check("memory_budget".equals(GpuQualification1961.outcome1984)&&QualityPipeline1932.calls==0,
              "half reconstruction memory refusal runs no pixel work and records the real reason");clean(source,1);threadState();
    }
    static void reference83()throws Exception {
        reset();final Bitmap source=image();final long[] heldAtCopy={-1};
        Bitmap.beforeCopy1984=new Runnable(){public void run(){heldAtCopy[0]=GpuQualification1961.held;}};
        GpuQualification1961.declineAt=2;GpuQualification1961.declineReason1983=16;
        int created=Bitmap.all.size(),copies=Bitmap.copies;invoke(source);
        check(GpuQualification1961.reservationCalls1984==0&&GpuQualification1961.commitCalls1984==0,
              "published .83 does not reserve qualification bytes through a ticket");
        check(heldAtCopy[0]==0&&Bitmap.copies==copies+1,"published .83 copies before holding the queue retention");
        check(GpuQualification1961.queueCalls==2&&ProcessingTiming1947.queueReason1983==16,
              "published .83 reaches the second admission refusal only after copying");
        oneRecycle(soleClone(created));
        check(!source.isRecycled()&&source.recycleCalls1984==0,"historical rejection still preserves the caller image");
        noCopyLock();
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0&&args[0].equals("published83")) {
            reference83();
            System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"published83_unreserved_copy_reproduced1984\":true}");
            return;
        }
        beforeCopyReservation();cancellationsAndCopies();commitRaces();exceptionOwnership();
        proofRun(false);proofRun(true);rejectedProofs();GpuQualification1961.drop();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"resident_reservation_before_copy1984\":true,\"resident_ticket_cleanup1984\":true,\"resident_commit_races1984\":true,\"resident_attempt_diagnostics_noninterference1984\":true,\"resident_retained_proof_gates1984\":true}");
    }
}
