package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
public final class StrongPreferred1970Test {
    static int assertions;
    static final int SENTINEL=0x11335577;
    static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
    static void reset()throws Exception{
        GpuNoise1960.releaseAll();GpuNoise1960.reset();GpuQualification1961.reset();ProcessingTiming1947.reset();CameraTrace1965.events.clear();StrongNoise1958.oracleCalls.set(0);StrongNoise1958.cpuDelay=1000000L;
        Field gates=GpuStrong1960.class.getDeclaredField("GATES");gates.setAccessible(true);((Map<?,?>)gates.get(null)).clear();
        Field active=GpuStrong1960.class.getDeclaredField("active");active.setAccessible(true);check(active.get(null)==null,"previous test closed its Strong stage");
    }
    static final class Job {
        final int[] input,output,confidence;
        final StrongNoise1958.Model model;final int marker;
        int cpuCalls;boolean oracleSecondChanges,interruptAfterFirst,throwSecondAfterPartial;GpuPolicy1960.Protection protection;
        Job(StrongNoise1958.Model m,int marker){this(m,marker,0,0);}
        Job(StrongNoise1958.Model m,int marker,int tail,int confidenceTail){model=m;this.marker=marker;input=new int[1024+tail];output=new int[1024+tail];confidence=new int[64+confidenceTail];for(int i=0;i<input.length;i++)input[i]=0xff000000|((i*7919+marker)&0xffffff);input[0]=0xff100000|marker;Arrays.fill(output,SENTINEL);Arrays.fill(confidence,-991);}
        final GpuStrong1960.Oracle oracle=new GpuStrong1960.Oracle(){public boolean run(int[] destination,int[] conf){
            cpuCalls++;boolean ok=StrongNoise1958.gpuOracleSnapshot1961(input,destination,32,32,0,32,0,32,0,4,true,model,null,conf);
            if(oracleSecondChanges&&cpuCalls>=2)destination[1023]^=0x00008000;
            if(throwSecondAfterPartial&&cpuCalls==2){destination[1023]^=0x00002000;throw new IllegalStateException("CPU oracle failed after a partial destination write");}
            if(interruptAfterFirst&&cpuCalls==1)Thread.currentThread().interrupt();return ok;
        }};
        boolean raw(){return GpuStrong1960.process(input,output,32,32,0,32,0,32,0,4,true,model,null,confidence,protection,oracle);}
        boolean complete(){boolean ok=raw();if(!ok){if(Thread.currentThread().isInterrupted())throw new CancellationException();return oracle.run(output,confidence);}return true;}
        void exactOutput(String label){int[] expected=new int[output.length];Arrays.fill(expected,SENTINEL);for(int i=0;i<1024;i++)expected[i]=StrongNoise1958.pixel(input[i]);if(oracleSecondChanges&&cpuCalls>=2)expected[1023]^=0x00008000;check(Arrays.equals(output,expected),label+" full ARGB including final word and unchanged capacity tail");int[] expectedConfidence=new int[confidence.length];Arrays.fill(expectedConfidence,-991);Arrays.fill(expectedConfidence,0,64,37);check(Arrays.equals(confidence,expectedConfidence),label+" complete confidence including final cell and unchanged capacity tail");}
        void untouched(String label){int[] expected=new int[output.length];Arrays.fill(expected,SENTINEL);check(Arrays.equals(output,expected),label+" output not committed");int[] expectedConfidence=new int[confidence.length];Arrays.fill(expectedConfidence,-991);check(Arrays.equals(confidence,expectedConfidence),label+" confidence not committed");}
    }
    static int[] uniforms(int mode){int[] u=new int[32];u[0]=32;u[1]=32;u[3]=32;u[5]=32;u[7]=32;u[8]=4;u[9]=1;u[10]=mode;u[12]=mode==3?1:0;return u;}
    static String key(int mode)throws Exception{Method m=GpuStrong1960.class.getDeclaredMethod("key",int[].class);m.setAccessible(true);return (String)m.invoke(null,(Object)uniforms(mode));}
    static void close(StrongNoise1958.Model model){GpuStrong1960.endStage(model);}
    static int reasons(){int n=0;for(int count:ProcessingTiming1947.reasons.values())n+=count;return n;}
    static void certified(int mode)throws Exception{GpuQualification1961.qualifiedStrongPreferred1970(key(mode),1000000L,20000000L,0);GpuQualification1961.preferredCalls=0;}
    static void firstAndRepeatedSlow()throws Exception{
        reset();String key=key(3);check(key.equals("strong-gx1964-parallel-policy-bank-v1:3:32:32:32:0:32:0:0:32:4:1:0:1:0:0"),"historical exact key unchanged");
        // Historical speed rejection must not block the new foreground quality proof.
        GpuQualification1961.rejectSpeed(key);GpuNoise1960.gpuDelay=8000000L;
        StrongNoise1958.Model model=new StrongNoise1958.Model();Job first=new Job(model,1);GpuStrong1960.beginStage(model);
        try{check(first.complete(),"first new key completes foreground");first.exactOutput("first");
            check(first.cpuCalls==2,"first foreground runs the actual CPU oracle twice");
            check(GpuNoise1960.executes==2,"first foreground compares two complete GPU trials");
            check(GpuQualification1961.restore(key)!=null,"exact foreground gets an immediate certificate");
            check(GpuQualification1961.preferredCalls==1,"foreground uses Strong preference certificate API");
            check(!GpuQualification1961.exactRejected(key),"valid slow GPU has no quality rejection");
            Job second=new Job(model,2);check(second.complete(),"second same key completes");second.exactOutput("second");check(second.cpuCalls==0,"certified slow GPU calls no foreground CPU oracle");
            Job third=new Job(model,3);check(third.complete(),"third same key stays GPU");third.exactOutput("third");check(third.cpuCalls==0,"repeated GPU observation cannot reinstate speed gate");
        }finally{close(model);}
        check(ProcessingTiming1947.lastGpu==3&&ProcessingTiming1947.lastCpu==0,"only final GPU commits count as GPU strips");
        check(ProcessingTiming1947.verification==2,"CPU verification is counted separately from selected CPU strips");
        check(reasons()==0,"successful GPU strips add no CPU fallback reason");
        check(GpuQualification1961.restore(key)!=null&&!GpuQualification1961.speed.contains(key),"stage close retains slow preferred certificate");
        check(GpuNoise1960.opens==1&&GpuNoise1960.closes==1&&GpuNoise1960.modelUploads==4,"stage shares one session and model transfer");
    }
    static void threeSeparatePhotosRestoreSlowCertificate()throws Exception{
        reset();GpuNoise1960.gpuDelay=25000000L;String key=key(3);
        for(int photo=0;photo<3;photo++){
            ProcessingTiming1947.reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,170+photo);int beforeGpu=GpuNoise1960.executes;
            GpuStrong1960.beginStage(model);try{check(job.complete(),"separate photo "+photo+" completes");job.exactOutput("separate photo "+photo);}
            finally{close(model);}
            check(job.cpuCalls==(photo==0?2:0),"separate photo "+photo+" uses CPU oracle only for first photo qualification");
            check(GpuNoise1960.executes-beforeGpu==(photo==0?2:1),"separate photo "+photo+" performs only required GPU trials");
            check(ProcessingTiming1947.backendCalls==1&&ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0,"separate photo "+photo+" independently records GPU1 CPU0");
            check(ProcessingTiming1947.verification==(photo==0?2:0),"separate photo "+photo+" independently attributes verification CPU");
            GpuQualification1961.Record saved=GpuQualification1961.restore(key);check(saved!=null&&saved.gpuNanos>saved.cpuNanos,"slow preferred certificate survives separate photo "+photo+" stage close");
            check(!GpuQualification1961.speed.contains(key)&&reasons()==0,"separate photo "+photo+" close cannot reinstate speed fallback");
            Field gates=GpuStrong1960.class.getDeclaredField("GATES");gates.setAccessible(true);((Map<?,?>)gates.get(null)).clear();
        }
        check(GpuQualification1961.preferredCalls==1,"three separate photos restore the saved certificate without recertification");
        check(GpuNoise1960.opens==3&&GpuNoise1960.closes==3&&GpuNoise1960.modelUploads==12,"each separate photo owns and releases its own model session");
    }
    static void mismatches()throws Exception{
        for(int kind=0;kind<3;kind++){
            reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,20+kind);
            if(kind==0)GpuNoise1960.badSecondPixel=true;if(kind==1)GpuNoise1960.badSecondConfidence=true;if(kind==2)job.oracleSecondChanges=true;
            GpuStrong1960.beginStage(model);try{check(job.complete(),"mismatch preserves CPU completion "+kind);job.exactOutput("mismatch latest CPU "+kind);}finally{close(model);}
            check(GpuNoise1960.executes>=2,"second trial failure checked before variant rejection "+kind);
            if(kind<2)check(GpuNoise1960.executes==6&&job.cpuCalls==6,"each supported variant receives two consecutive complete trials "+kind);
            check(GpuQualification1961.restore(key(3))==null&&GpuQualification1961.exactRejected(key(3)),"mismatch never certifies "+kind);
            check(GpuQualification1961.preferredCalls==0,"mismatch never invokes certificate publication "+kind);
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"mismatch never counts a GPU commit "+kind);
            check(ProcessingTiming1947.verification==job.cpuCalls&&job.cpuCalls>=2,"all completed CPU verification runs counted "+kind);
            check(reasons()==1,"mismatch has one selected CPU reason "+kind);
            check(GpuNoise1960.opens==GpuNoise1960.closes,"mismatch releases session "+kind);
            int executes=GpuNoise1960.executes;StrongNoise1958.Model nextModel=new StrongNoise1958.Model();Job next=new Job(nextModel,30+kind);GpuStrong1960.beginStage(nextModel);
            try{check(next.complete(),"known quality rejection preserves CPU on next photo");next.exactOutput("known quality CPU");}finally{close(nextModel);}
            check(GpuNoise1960.executes==executes&&next.cpuCalls==1,"known exact rejection does not execute GPU again");
        }
    }
    static void alternativeVariantsAndCapacity()throws Exception{
        for(int kind=0;kind<2;kind++){
            reset();if(kind==0){GpuNoise1960.badSecondPixel=true;GpuNoise1960.badVariantMask=1;}else GpuNoise1960.unsupportedVariantMask=1;
            StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,110+kind,37,11);GpuStrong1960.beginStage(model);
            try{check(job.complete(),"alternative supported variant completes "+kind);job.exactOutput("alternative pooled output "+kind);
                GpuQualification1961.Record proof=GpuQualification1961.restore(key(3));check(proof!=null&&proof.variant==1,"next supported exact variant is certified "+kind);
                check(!GpuQualification1961.exactRejected(key(3)),"a single failed variant does not poison whole exact key "+kind);
                check(job.cpuCalls==(kind==0?4:2)&&GpuNoise1960.executes==job.cpuCalls,"successful variant still needs two own complete trials "+kind);
                Job next=new Job(model,120+kind);check(next.complete(),"alternative certified variant reused");next.exactOutput("alternative reuse");check(next.cpuCalls==0,"alternative certificate avoids repeated CPU oracle");
            }finally{close(model);}
            check(ProcessingTiming1947.lastGpu==2&&ProcessingTiming1947.lastCpu==0,"variant retry never falsely counts CPU output "+kind);check(GpuNoise1960.bankReuse==0,"variant trials retain private bank ownership "+kind);
        }
        reset();GpuQualification1961.throwPreferred=true;StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,130);GpuStrong1960.beginStage(model);
        try{check(job.complete(),"optional persistence failure preserves completed GPU output");job.exactOutput("optional persistence failure");}finally{close(model);}
        check(GpuQualification1961.restore(key(3))==null,"failed optional storage publishes no certificate");check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0,"optional persistence failure does not relabel committed GPU as CPU");check(reasons()==0,"optional persistence failure adds no false CPU reason");
    }
    static void failures()throws Exception{
        for(int kind=0;kind<7;kind++){
            reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,40+kind);
            if(kind==0)GpuNoise1960.available=false;if(kind==1)GpuNoise1960.supported=false;if(kind==2)GpuNoise1960.fits=false;
            if(kind==3)GpuNoise1960.failModelUpload=true;if(kind==4)GpuNoise1960.failSecondExecute=true;if(kind==5)GpuNoise1960.failSubmit=true;
            if(kind==6){GpuNoise1960.policyFlag=1;job.protection=new GpuPolicy1960.Protection();job.protection.plan=new GpuPolicy1960.Plan();}
            GpuStrong1960.beginStage(model);try{check(job.complete(),"failure preserves complete CPU result "+kind);job.exactOutput("failure CPU "+kind);}finally{close(model);}
            check(GpuQualification1961.restore(key(3))==null&&GpuQualification1961.preferredCalls==0,"failed GPU never certifies "+kind);
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"failed GPU never counts a commit "+kind);
            check(reasons()==1,"failed GPU contributes one CPU fallback reason "+kind);
            check(GpuNoise1960.opens==GpuNoise1960.closes,"failed GPU releases session "+kind);
            if(kind<=2)check(GpuNoise1960.executes==0&&GpuNoise1960.opens==0,"unavailable capability/memory avoids EGL work "+kind);
        }
    }
    static final class Worker implements Runnable {
        final Job job;final AtomicReference<Throwable> error=new AtomicReference<Throwable>();boolean complete;
        Worker(Job job){this.job=job;}public void run(){try{complete=job.complete();}catch(Throwable e){error.set(e);}}
    }
    static void until(Callable<Boolean> condition,String label)throws Exception{long end=System.nanoTime()+3000000000L;while(!condition.call()){if(System.nanoTime()>=end)throw new AssertionError(label+" timed out");Thread.sleep(1);}check(true,label);}
    static void threadWaiting(final Thread t,String label)throws Exception{until(new Callable<Boolean>(){public Boolean call(){Thread.State s=t.getState();return s==Thread.State.WAITING||s==Thread.State.TIMED_WAITING||s==Thread.State.TERMINATED;}},label);check(t.isAlive(),label+" remains queued");}
    static void fourWorkersFifo()throws Exception{
        reset();certified(3);GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();final Worker[] workers=new Worker[4];Thread[] threads=new Thread[4];
        for(int i=0;i<4;i++){workers[i]=new Worker(new Job(model,70+i));threads[i]=new Thread(workers[i],"strong70-worker-"+i);}
        GpuStrong1960.beginStage(model);
        try{
            threads[0].start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"first bank submitted");
            threads[1].start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==2;}},"second bank submitted");
            threads[2].start();threadWaiting(threads[2],"third worker queues for a bank");
            threads[3].start();threadWaiting(threads[3],"fourth worker also queues");
            check(GpuNoise1960.submits==2&&GpuNoise1960.inflight==2,"four workers share only two live mutable banks");
            GpuNoise1960.releaseTicket(1);until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==3;}},"oldest waiter gets first released bank");
            check(GpuNoise1960.submittedMarkers.get(2).intValue()==workers[2].job.input[0],"third worker precedes fourth under FIFO contention");
            GpuNoise1960.releaseTicket(2);until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==4;}},"remaining waiter gets second released bank");
            check(GpuNoise1960.submittedMarkers.get(3).intValue()==workers[3].job.input[0],"fourth worker is eventually submitted");
            GpuNoise1960.releaseAll();for(Thread t:threads){t.join(3000);check(!t.isAlive(),"parallel worker joined");}
            for(Worker worker:workers){check(worker.error.get()==null&&worker.complete,"parallel worker completed");worker.job.exactOutput("parallel");check(worker.job.cpuCalls==0,"bank contention never selects CPU for preferred route");}
        }finally{GpuNoise1960.releaseAll();for(Thread t:threads)if(t.isAlive())t.join(3000);close(model);}
        check(GpuNoise1960.maxInflight==2&&GpuNoise1960.bankReuse==0&&GpuNoise1960.inflight==0,"bank ownership and bounded inflight maintained");
        check(ProcessingTiming1947.lastGpu==4&&ProcessingTiming1947.lastCpu==0,"parallel final output counts all four GPU strips");
        check(GpuNoise1960.opens==1&&GpuNoise1960.closes==1,"parallel stage session released once");
    }
    static void cancellation()throws Exception{
        reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,90);GpuStrong1960.beginStage(model);
        try{Thread.currentThread().interrupt();boolean ok=job.raw();check(!ok,"already interrupted foreground declines work");check(GpuNoise1960.executes==0&&job.cpuCalls==0,"already interrupted path does no verification");job.untouched("already interrupted");}
        finally{Thread.interrupted();close(model);}
        reset();model=new StrongNoise1958.Model();job=new Job(model,91);job.interruptAfterFirst=true;GpuStrong1960.beginStage(model);
        try{boolean cancelled=false;try{job.raw();}catch(CancellationException expected){cancelled=true;}check(cancelled,"cancellation during verification propagates");job.exactOutput("last completed CPU reference preserved on cancellation");check(GpuNoise1960.executes==0,"cancelled first CPU verification never submits GPU work");check(GpuQualification1961.restore(key(3))==null,"cancelled verification never certifies");}
        finally{Thread.interrupted();close(model);}
        check(ProcessingTiming1947.lastGpu==0,"cancelled verification never records final GPU commit");
        reset();certified(3);GpuNoise1960.holdTickets=true;model=new StrongNoise1958.Model();Worker one=new Worker(new Job(model,92)),two=new Worker(new Job(model,93)),waiting=new Worker(new Job(model,94));Thread t1=new Thread(one),t2=new Thread(two),t3=new Thread(waiting);GpuStrong1960.beginStage(model);
        try{t1.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"cancel test first bank");t2.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==2;}},"cancel test second bank");t3.start();threadWaiting(t3,"cancellable preferred waiter");t3.interrupt();t3.join(3000);check(!t3.isAlive()&&waiting.error.get() instanceof CancellationException,"queued cancellation propagates without CPU selection");waiting.job.untouched("cancelled bank waiter");check(GpuNoise1960.submits==2,"cancelled waiter never submits GPU work");GpuNoise1960.releaseAll();t1.join(3000);t2.join(3000);}
        finally{GpuNoise1960.releaseAll();for(Thread t:new Thread[]{t1,t2,t3})if(t.isAlive())t.join(3000);close(model);}
        check(ProcessingTiming1947.lastGpu==2&&ProcessingTiming1947.lastCpu==0,"cancelled waiter does not claim a completed strip");check(GpuNoise1960.opens==GpuNoise1960.closes,"cancelled waiter releases stage ownership");
    }
    static void negativeBeforeCommit()throws Exception{
        reset();certified(3);GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();Worker worker=new Worker(new Job(model,140));Thread thread=new Thread(worker);GpuStrong1960.beginStage(model);
        try{thread.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"negative test has private GPU candidate in flight");GpuQualification1961.rejectExact(key(3));GpuNoise1960.releaseAll();thread.join(3000);check(!thread.isAlive()&&worker.error.get()==null&&worker.complete,"late exact negative preserves CPU completion");worker.job.exactOutput("negative before commit latest CPU");check(worker.job.cpuCalls==1,"late exact negative uses CPU rather than private GPU commit");}
        finally{GpuNoise1960.releaseAll();if(thread.isAlive())thread.join(3000);close(model);}
        check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"late negative never records GPU commit");check(GpuQualification1961.restore(key(3))==null,"late negative cannot be overwritten by a certificate");check(reasons()==1,"late negative has one selected CPU reason");
    }
    static void oracleFailureAndRefreshedNegative()throws Exception{
        reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,150);job.throwSecondAfterPartial=true;GpuStrong1960.beginStage(model);
        try{boolean ok=job.raw();check(!ok,"partial second CPU oracle failure returns false for caller repair");check(job.cpuCalls==2&&GpuNoise1960.executes==1,"failed second CPU reference never launches a second GPU trial");check(job.oracle.run(job.output,job.confidence),"caller can repair the complete output after false");job.exactOutput("caller repair after partial oracle failure");}finally{close(model);}
        check(GpuQualification1961.restore(key(3))==null&&GpuQualification1961.preferredCalls==0,"partial CPU oracle failure never certifies");check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"partial oracle failure never counts GPU commit");check(ProcessingTiming1947.verification==1,"only the completed CPU reference counts as verification");
        reset();GpuNoise1960.rejectOnSupports=key(3);model=new StrongNoise1958.Model();job=new Job(model,151);GpuStrong1960.beginStage(model);
        try{check(job.complete(),"fingerprint-refreshed negative keeps CPU completion");job.exactOutput("refreshed negative CPU");}finally{close(model);}
        check(job.cpuCalls==1&&GpuNoise1960.opens==0&&GpuNoise1960.executes==0,"negative loaded by supports blocks all model upload and GPU work");check(ProcessingTiming1947.verification==0,"refreshed exact negative avoids new verification");check(GpuQualification1961.restore(key(3))==null,"refreshed exact negative cannot be recertified");
    }
    static GpuQualification1961.Probe proof(String key,int mode)throws Exception{
        int[] u=uniforms(mode);Class<?> c=Class.forName("com.hiro.ulike.GpuStrong1960$Proof");Constructor<?> constructor=c.getDeclaredConstructors()[0];constructor.setAccessible(true);int[] source=new int[1024];Arrays.fill(source,0xff998877);
        return (GpuQualification1961.Probe)constructor.newInstance(key,source,null,new float[16],u,null,null);
    }
    static final GpuQualification1961.Cancellation LIVE=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    static void preparationPreserved()throws Exception{
        for(int mode=0;mode<3;mode++){
            reset();GpuNoise1960.gpuDelay=10000000L;GpuQualification1961.Probe p=proof("prep-slow-"+mode,mode);try{p.run(LIVE);}finally{p.close();}
            check(GpuQualification1961.restore("prep-slow-"+mode)==null&&GpuQualification1961.speed.contains("prep-slow-"+mode),"mode "+mode+" retains five-percent speed gate");check(!GpuQualification1961.exactRejected("prep-slow-"+mode),"slow preparation does not become quality rejection");check(GpuQualification1961.preferredCalls==0,"mode "+mode+" never uses foreground preference API");
            reset();StrongNoise1958.cpuDelay=12000000L;p=proof("prep-fast-"+mode,mode);try{p.run(LIVE);}finally{p.close();}
            GpuQualification1961.Record r=GpuQualification1961.restore("prep-fast-"+mode);check(r!=null&&r.gpuNanos<=r.cpuNanos-r.cpuNanos/20,"mode "+mode+" can still qualify a measured fast GPU");check(GpuQualification1961.ordinaryCalls==1&&GpuQualification1961.preferredCalls==0,"mode "+mode+" uses original qualification API");check(GpuNoise1960.opens==GpuNoise1960.closes,"preparation releases ephemeral sessions");
        }
    }
    public static void main(String[] args)throws Exception{
        firstAndRepeatedSlow();threeSeparatePhotosRestoreSlowCertificate();mismatches();alternativeVariantsAndCapacity();failures();fourWorkersFifo();cancellation();negativeBeforeCommit();oracleFailureAndRefreshedNegative();preparationPreserved();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"first_foreground_complete_trials\":2,\"foreground_quality_checked\":true,\"slow_gpu_preference_verified\":true,\"gpu_bank_fifo_verified\":true,\"cpu_fallback_safety_preserved\":true,\"preparation_five_percent_gate_preserved\":true,\"historical_exact_key_preserved\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
