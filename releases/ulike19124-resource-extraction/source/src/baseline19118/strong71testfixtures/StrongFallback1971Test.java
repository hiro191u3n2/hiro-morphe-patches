package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
public final class StrongFallback1971Test {
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
        int cpuCalls,loseBudgetAt;boolean oracleSecondChanges,interruptAfterFirst,throwSecondAfterPartial,assertColdOracle;GpuPolicy1960.Protection protection;
        Job(StrongNoise1958.Model m,int marker){this(m,marker,0,0);}
        Job(StrongNoise1958.Model m,int marker,int tail,int confidenceTail){model=m;this.marker=marker;input=new int[1024+tail];output=new int[1024+tail];confidence=new int[64+confidenceTail];for(int i=0;i<input.length;i++)input[i]=0xff000000|((i*7919+marker)&0xffffff);input[0]=0xff100000|marker;Arrays.fill(output,SENTINEL);Arrays.fill(confidence,-991);}
        final GpuStrong1960.Oracle oracle=new GpuStrong1960.Oracle(){public boolean run(int[] destination,int[] conf){
            cpuCalls++;if(assertColdOracle&&cpuCalls==1){check(GpuNoise1960.opens==0&&GpuNoise1960.modelUploads==0,"first unknown CPU oracle precedes EGL/model uploads");check(GpuNoise1960.reservationCalls==0,"first unknown CPU oracle precedes GPU capacity reservations");}boolean ok=StrongNoise1958.gpuOracleSnapshot1961(input,destination,32,32,0,32,0,32,0,4,true,model,null,conf);
            if(oracleSecondChanges&&cpuCalls>=2)destination[1023]^=0x00008000;
            if(throwSecondAfterPartial&&cpuCalls==2){destination[1023]^=0x00002000;throw new IllegalStateException("CPU oracle failed after a partial destination write");}
            if(loseBudgetAt==cpuCalls)GpuNoise1960.fits=false;
            if(interruptAfterFirst&&cpuCalls==1)Thread.currentThread().interrupt();return ok;
        }};
        boolean raw(){return GpuStrong1960.process(input,output,32,32,0,32,0,32,0,4,true,model,protection==null?null:new int[2048],confidence,protection,oracle);}
        boolean complete(){boolean ok=raw();if(!ok){if(Thread.currentThread().isInterrupted())throw new CancellationException();return oracle.run(output,confidence);}return true;}
        void exactOutput(String label){int[] expected=new int[output.length];Arrays.fill(expected,SENTINEL);for(int i=0;i<1024;i++)expected[i]=StrongNoise1958.pixel(input[i]);if(oracleSecondChanges&&cpuCalls>=2)expected[1023]^=0x00008000;check(Arrays.equals(output,expected),label+" full ARGB including final word and unchanged capacity tail");int[] expectedConfidence=new int[confidence.length];Arrays.fill(expectedConfidence,-991);Arrays.fill(expectedConfidence,0,64,37);check(Arrays.equals(confidence,expectedConfidence),label+" complete confidence including final cell and unchanged capacity tail");}
        void untouched(String label){int[] expected=new int[output.length];Arrays.fill(expected,SENTINEL);check(Arrays.equals(output,expected),label+" output not committed");int[] expectedConfidence=new int[confidence.length];Arrays.fill(expectedConfidence,-991);check(Arrays.equals(confidence,expectedConfidence),label+" confidence not committed");}
    }
    static int[] uniforms(int mode){int[] u=new int[32];u[0]=32;u[1]=32;u[3]=32;u[5]=32;u[7]=32;u[8]=4;u[9]=1;u[10]=mode;u[12]=mode==3?1:0;return u;}
    static String key(int mode)throws Exception{Method m=GpuStrong1960.class.getDeclaredMethod("key",int[].class);m.setAccessible(true);return (String)m.invoke(null,(Object)uniforms(mode));}
    static String genericKey()throws Exception{Method m=GpuStrong1960.class.getDeclaredMethod("genericKey1971",int[].class);m.setAccessible(true);return (String)m.invoke(null,(Object)uniforms(3));}
    static void close(StrongNoise1958.Model model){GpuStrong1960.endStage(model);}
    static int reasons(){int n=0;for(int count:ProcessingTiming1947.reasons.values())n+=count;return n;}
    static void certified(int mode)throws Exception{GpuQualification1961.qualifiedStrongPreferred1970(key(mode),1000000L,20000000L,0);GpuQualification1961.preferredCalls=0;}
    static void firstAndRepeatedSlow()throws Exception{
        reset();String key=key(3);check(key.equals("strong-gx1964-parallel-policy-bank-v1:3:32:32:32:0:32:0:0:32:4:1:0:1:0:0"),"historical exact key unchanged");
        // Historical speed rejection must not block the new foreground quality proof.
        GpuQualification1961.rejectSpeed(key);GpuNoise1960.gpuDelay=8000000L;
        StrongNoise1958.Model model=new StrongNoise1958.Model();Job first=new Job(model,1);first.assertColdOracle=true;GpuStrong1960.beginStage(model);
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
            if(kind<2)check(GpuNoise1960.executes==12&&job.cpuCalls==12,"each supported tiled and generic variant receives two consecutive complete trials "+kind);
            check(GpuQualification1961.restore(key(3))==null&&GpuQualification1961.exactRejected(key(3)),"mismatch never certifies tiled "+kind);
            check(GpuQualification1961.restore(genericKey())==null&&GpuQualification1961.exactRejected(genericKey()),"mismatch never certifies generic "+kind);
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
            if(kind<=2)check(GpuNoise1960.executes==0&&job.cpuCalls==1,"unavailable capability/memory avoids GPU trials and preserves one CPU result "+kind);
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
        reset();GpuQualification1961.rejectExact(genericKey());GpuNoise1960.rejectOnSupports=key(3);model=new StrongNoise1958.Model();job=new Job(model,151);GpuStrong1960.beginStage(model);
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
    static int trials(String kind){Integer n=ProcessingTiming1947.trials.get(kind);return n==null?0:n;}
    static void genericAfterLegacyNegative()throws Exception{
        reset();String old=key(3),fresh=genericKey();GpuQualification1961.rejectExact(old);GpuNoise1960.gpuDelay=8000000L;
        check(!fresh.equals(old)&&fresh.equals("strong-gx1971-generic-policy-bank-v1:3:32:32:32:0:32:0:0:32:4:1:0:1:0:0"),"generic math route has independent complete historical dimensions");
        for(int photo=0;photo<3;photo++){
            ProcessingTiming1947.reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,200+photo,23,7);job.assertColdOracle=photo==0;int before=GpuNoise1960.executes;
            GpuStrong1960.beginStage(model);try{check(job.complete(),"generic photo "+photo+" completes");job.exactOutput("generic photo "+photo);}finally{close(model);}
            check(job.cpuCalls==(photo==0?2:0),"generic photo "+photo+" CPU oracle is required only for first proof");
            check(GpuNoise1960.executes-before==(photo==0?2:1),"generic photo "+photo+" executes only its independent proof or accepted use");
            check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0&&reasons()==0,"generic photo "+photo+" commits GPU despite legacy rejection and slow GPU");
            check(ProcessingTiming1947.verification==(photo==0?2:0),"generic photo "+photo+" verification is distinct from CPU route");
            check(GpuQualification1961.exactRejected(old)&&GpuQualification1961.restore(old)==null,"generic photo "+photo+" preserves historical negative");
            check(GpuQualification1961.restore(fresh)!=null&&!GpuQualification1961.exactRejected(fresh),"generic photo "+photo+" retains independent exact certificate");
            Field gates=GpuStrong1960.class.getDeclaredField("GATES");gates.setAccessible(true);((Map<?,?>)gates.get(null)).clear();
        }
        check(GpuQualification1961.preferredCalls==1,"three generic photos restore one saved certificate");
        check(GpuNoise1960.submittedPrograms.equals(Arrays.asList(0,0,0,0)),"generic proof and repeats dispatch linear Strong program only");
        check(GpuNoise1960.liveRangeLeases==0&&GpuNoise1960.reservedJava==0,"generic completion releases every reservation");
    }
    static void tiledMismatchThenGeneric()throws Exception{
        reset();GpuNoise1960.badSecondPixel=true;GpuNoise1960.badGenericMask=0;StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,210,31,9);
        GpuStrong1960.beginStage(model);try{check(job.complete(),"all tiled mismatches recover with generic");job.exactOutput("generic recovery");}finally{close(model);}
        check(job.cpuCalls==8&&GpuNoise1960.executes==8,"three tiled second-trial failures precede two independent generic exact trials");
        check(GpuNoise1960.submittedPrograms.equals(Arrays.asList(30,30,34,34,38,38,0,0)),"recovery trials cover all tiled variants before generic proof");
        check(GpuQualification1961.exactRejected(key(3))&&"cached_argb_negative".equals(GpuQualification1961.exactFailure1971(key(3))),"tiled ARGB rejection stores bounded cause");
        check(GpuQualification1961.restore(genericKey())!=null&&GpuQualification1961.preferredCalls==1,"only exact generic route certifies");
        check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0&&reasons()==0,"failed trials do not invent a selected CPU strip");
        check(trials("argb_mismatch")==3&&ProcessingTiming1947.verification==8,"failed pixel trials have independent counts");
        check("argb_mismatch".equals(ProcessingTiming1947.firstFailure)&&ProcessingTiming1947.firstProgram==30&&ProcessingTiming1947.firstLayout==0&&ProcessingTiming1947.firstX==31&&ProcessingTiming1947.firstY==31&&ProcessingTiming1947.firstDelta==1,"first bounded failure identifies exact last pixel and channel delta");
    }
    static void genericAlternativeVariant()throws Exception{
        reset();GpuQualification1961.rejectExact(key(3));GpuNoise1960.badSecondPixel=true;GpuNoise1960.badGenericMask=1;StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,215);GpuStrong1960.beginStage(model);
        try{check(job.complete(),"generic alternative layout completes");job.exactOutput("generic alternative");check(job.cpuCalls==4&&GpuNoise1960.executes==4,"generic alternative requires two complete own proofs after first layout mismatch");
            GpuQualification1961.Record saved=GpuQualification1961.restore(genericKey());check(saved!=null&&saved.variant==1,"generic alternative certifies its actual chosen layout");
            Job next=new Job(model,216);check(next.complete(),"accepted generic alternative reused");next.exactOutput("generic alternative repeat");check(next.cpuCalls==0,"accepted generic alternative invokes no CPU oracle");
        }finally{close(model);}
        check(GpuNoise1960.submittedPrograms.equals(Arrays.asList(0,0,9,9,9)),"generic alternative dispatches correct program family and selected repeat");
        check(ProcessingTiming1947.lastGpu==2&&ProcessingTiming1947.lastCpu==0&&reasons()==0&&trials("argb_mismatch")==1,"generic alternative separates failed trial from both completed GPU strips");
    }
    static void genericSecondTrialRejected()throws Exception{
        for(int confidence=0;confidence<2;confidence++){
            reset();GpuQualification1961.rejectExact(key(3));GpuNoise1960.unsupportedGenericMask=6;
            if(confidence==0)GpuNoise1960.badSecondPixel=true;else GpuNoise1960.badSecondConfidence=true;
            StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,220+confidence);GpuStrong1960.beginStage(model);
            try{check(job.complete(),"generic second trial mismatch retains CPU "+confidence);job.exactOutput("generic rejection latest CPU "+confidence);}finally{close(model);}
            check(job.cpuCalls==2&&GpuNoise1960.executes==2,"generic mismatch sees second full candidate before rejecting "+confidence);
            check(GpuQualification1961.exactRejected(key(3))&&GpuQualification1961.exactRejected(genericKey())&&GpuQualification1961.restore(genericKey())==null&&GpuQualification1961.preferredCalls==0,"generic rejection preserves both negatives without certificate "+confidence);
            String kind=confidence==0?"argb_mismatch":"confidence_mismatch";
            check(trials(kind)==1&&kind.equals(ProcessingTiming1947.firstFailure),"generic rejection diagnoses matching data class "+confidence);
            check(ProcessingTiming1947.firstProgram==0&&ProcessingTiming1947.firstLayout==0&&ProcessingTiming1947.firstX==(confidence==0?31:29)&&ProcessingTiming1947.firstY==(confidence==0?31:29)&&ProcessingTiming1947.firstDelta==1,"generic first failure is bounded and correctly located "+confidence);
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1&&reasons()==1,"generic rejection counts one selected CPU reason "+confidence);
            int before=GpuNoise1960.executes;ProcessingTiming1947.reset();model=new StrongNoise1958.Model();Job next=new Job(model,230+confidence);GpuStrong1960.beginStage(model);try{check(next.complete(),"cached generic rejection retains CPU");next.exactOutput("cached generic CPU");}finally{close(model);}
            check(next.cpuCalls==1&&GpuNoise1960.executes==before&&ProcessingTiming1947.verification==0,"cached generic rejection never replays rejected data route");
            check(reasons()==1&&ProcessingTiming1947.reasons.containsKey(confidence==0?"cached_argb_negative":"cached_confidence_negative")&&ProcessingTiming1947.trials.isEmpty(),"cached failure has one CPU reason and no new failed trial");
        }
    }
    static void classifiedNativePolicyAndShape()throws Exception{
        String[] kinds={"native_failure_unknown","policy_failure","output_shape_failed","readback_failed","submit_failed","upload_failed","execution_failed","readback_failed","memory_budget"};
        for(int kind=0;kind<kinds.length;kind++){
            reset();GpuQualification1961.rejectExact(key(3));StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,240+kind);
            if(kind==0)GpuNoise1960.failFirstExecute=true;if(kind==1)GpuNoise1960.policyFlag=1;if(kind==2)GpuNoise1960.badOutputShape=true;if(kind==3)GpuNoise1960.throwCollect=true;if(kind==4)GpuNoise1960.failSubmit=true;
            if(kind>=5){GpuNoise1960.failFirstExecute=true;GpuNoise1960.nativeFailureCode=new int[]{2,3,5,1}[kind-5];}
            GpuStrong1960.beginStage(model);try{check(job.complete(),"classified failure completes CPU "+kind);job.exactOutput("classified failure CPU "+kind);}finally{close(model);}
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1&&reasons()==1&&ProcessingTiming1947.reasons.containsKey(kinds[kind]),"one final CPU reason is classified independently "+kind);
            check(GpuQualification1961.restore(genericKey())==null&&GpuQualification1961.preferredCalls==0,"transport/policy failure never certifies "+kind);
            int expected=kind==1?3:1;check(job.cpuCalls==expected&&ProcessingTiming1947.verification==expected,"only completed CPU proof trials count "+kind);
            String trialKind=kind==8?"native_failure_unknown":kinds[kind];
            check(trials(trialKind)==expected&&trialKind.equals(ProcessingTiming1947.firstFailure),"per-trial failure count and bounded first native detail "+kind);
            check(ProcessingTiming1947.firstX==-1&&ProcessingTiming1947.firstY==-1&&ProcessingTiming1947.firstDelta==-1,"non-image failure contains no fabricated pixel location "+kind);
            check(GpuNoise1960.liveRangeLeases==0&&GpuNoise1960.reservedJava==0&&GpuNoise1960.opens==GpuNoise1960.closes,"classified failure frees native/session reservations "+kind);
            if(kind==1)check(GpuQualification1961.exactRejected(genericKey()),"policy proof failure persists its own negative");else check(!GpuQualification1961.exactRejected(genericKey()),"native transport failure does not become pixel certificate rejection "+kind);
            if(kind==8){boolean nativeOne=false;for(String event:CameraTrace1965.events)nativeOne|=event.contains("strong_gpu_failure1971")&&event.contains(" n=1");check(nativeOne,"native allocation code is retained in bounded trace detail");}
        }
    }
    static void descriptorAndConfidenceOffReservation()throws Exception{
        reset();GpuNoise1960.Session session=GpuNoise1960.open();Method reserve=GpuStrong1960.class.getDeclaredMethod("reserveRange1971",GpuNoise1960.Session.class,int[].class,int[].class,int[].class,GpuPolicy1960.PolicyData.class,int[].class);reserve.setAccessible(true);
        int[] bank={14,15,16,17,18,19,20,21,22,23},source=new int[1061],policy=new int[2061],u=uniforms(3);GpuPolicy1960.PolicyData descriptor=new GpuPolicy1960.PolicyData();descriptor.count=1024;descriptor.masks=new int[31];descriptor.grid=new int[17];
        GpuNoise1960.Lease1971 lease=(GpuNoise1960.Lease1971)reserve.invoke(null,session,source,policy,u,descriptor,bank);
        check(lease!=null,"descriptor can reserve its actual pooled capacities");GpuNoise1960.Reservation reservation=GpuNoise1960.reservations.get(0);
        check(Arrays.equals(reservation.slots,bank)&&Arrays.equals(reservation.target,new long[]{4244,4096,8192,256,124,68,4096,4,8244,4}),"descriptor reservation accounts actual mask/grid/pooled expected-policy capacity in bank1");
        check(reservation.javaBytes==8968,"descriptor shares exact two-readback Java allowance");lease.close();u[12]=0;
        lease=(GpuNoise1960.Lease1971)reserve.invoke(null,session,source,null,u,null,bank);check(lease!=null,"confidence-off reservation succeeds");reservation=GpuNoise1960.reservations.get(1);
        check(Arrays.equals(reservation.slots,new int[]{14,15,16,17,23})&&Arrays.equals(reservation.target,new long[]{4244,4096,4,4,4}),"confidence-off retains only one native dummy confidence word");
        check(reservation.javaBytes==8L*(1024+1)+256,"confidence-off Java budget excludes dummy confidence array");lease.close();session.close();check(GpuNoise1960.reservedJava==0&&GpuNoise1960.liveRangeLeases==0,"direct descriptor/dummy reservations release fully");
    }
    static void budgetRevalidatedBeforeEachSubmit()throws Exception{
        for(int trial=1;trial<=2;trial++){
            reset();GpuQualification1961.rejectExact(key(3));StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,280+trial);job.loseBudgetAt=trial;GpuStrong1960.beginStage(model);
            try{check(job.complete(),"lost runtime budget preserves completed CPU reference trial "+trial);job.exactOutput("runtime budget latest CPU "+trial);}finally{close(model);}
            check(job.cpuCalls==trial&&ProcessingTiming1947.verification==trial,"runtime budget retains only completed proof references "+trial);
            check(GpuNoise1960.executes==trial-1&&GpuNoise1960.submits==trial-1,"runtime budget is checked before every attempted native submission "+trial);
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1&&reasons()==1&&ProcessingTiming1947.reasons.containsKey("memory_budget"),"runtime budget reports one selected CPU route "+trial);
            check(GpuQualification1961.restore(genericKey())==null&&GpuQualification1961.preferredCalls==0&&!GpuQualification1961.exactRejected(genericKey()),"runtime budget never certifies or poisons generic quality key "+trial);
            check(GpuNoise1960.liveRangeLeases==0&&GpuNoise1960.reservedJava==0,"runtime budget failure releases range reservation "+trial);
        }
    }
    static void boundedCompleteDiagnosticLogs()throws Exception{
        reset();StrongNoise1958.Model model=new StrongNoise1958.Model();GpuStrong1960.beginStage(model);Field active=GpuStrong1960.class.getDeclaredField("active");active.setAccessible(true);Object stage=active.get(null);Class<?> owner=stage.getClass();
        Field reasons=owner.getDeclaredField("reasons"),trials=owner.getDeclaredField("trials1971");reasons.setAccessible(true);trials.setAccessible(true);int[] reasonCounts=(int[])reasons.get(stage),trialCounts=(int[])trials.get(stage);check(reasonCounts.length==23&&trialCounts.length==9,"fixed diagnostic index schema remains complete");Arrays.fill(reasonCounts,65535);Arrays.fill(trialCounts,65535);
        for(String name:new String[]{"gpuStrips","cpuStrips","verificationCpu"}){Field value=owner.getDeclaredField(name);value.setAccessible(true);value.setInt(stage,65535);}
        Class<?> failureType=Class.forName("com.hiro.ulike.GpuStrong1960$Failure1971");Constructor<?> constructor=failureType.getDeclaredConstructors()[0];constructor.setAccessible(true);int[] alignment=new int[32];alignment[1]=alignment[2]=Integer.MAX_VALUE;Object failure=constructor.newInstance("native_failure_unknown",38,2,65535,65535,65535,65535,4294967295L,6,alignment);Arrays.fill(alignment,0);Field first=owner.getDeclaredField("firstFailure1971");first.setAccessible(true);first.set(stage,failure);close(model);
        String trace="trace="+Long.MAX_VALUE;boolean[] seenReasons=new boolean[23],seenTrials=new boolean[9];int groups=0,countEvents=0,failureEvents=0,trialEvents=0;
        for(String event:CameraTrace1965.events){String[] pieces=event.split(" ",3);String phase=pieces[0],fields=pieces[2];check(fields.length()<=160,"diagnostic event fields survive 160-character sink: "+phase);check(fields.contains(trace),"diagnostic event preserves maximum trace identifier: "+phase);
            if(phase.equals("strong_gpu_counts1971")){countEvents++;check(fields.equals(trace+" gpu=65535 cpu=65535 verify=65535"),"counts event contains complete route and verification totals");}
            else if(phase.equals("strong_gpu_failure1971")){failureEvents++;check(fields.contains("kind=native_failure_unknown")&&fields.contains("p=38")&&fields.contains("l=2")&&fields.contains("w=65535")&&fields.contains("h=65535")&&fields.contains("x=65535")&&fields.contains("y=65535")&&fields.contains("d=4294967295")&&fields.contains(" n=6 ")&&fields.contains(" ay=2147483647 ")&&fields.contains(" b=2147483647 ")&&fields.endsWith("sr=2147483647"),"first failure event retains every bounded detail and copied alignment primitives without clipping");}
            else if(phase.equals("strong_gpu_reasons1971")){groups++;check(fields.contains(" base=0 ")||fields.contains(" base=12 "),"reason group identifies its fixed numeric base");for(String field:fields.split(" "))if(field.matches("r[0-9]+=65535")){int index=Integer.parseInt(field.substring(1,field.indexOf('=')));check(index<23&&!seenReasons[index],"reason count appears once as a whole field "+index);seenReasons[index]=true;}}
            else if(phase.equals("strong_gpu_trials1971")){trialEvents++;for(String field:fields.split(" "))if(field.matches("t[0-9]+=65535")){int index=Integer.parseInt(field.substring(1,field.indexOf('=')));check(index<9&&!seenTrials[index],"trial count appears once as a whole field "+index);seenTrials[index]=true;}}
            else check(false,"unexpected diagnostic event "+phase);
        }
        for(int i=0;i<seenReasons.length;i++)check(seenReasons[i],"reason "+i+" survives numeric-group serialization");for(int i=0;i<seenTrials.length;i++)check(seenTrials[i],"trial "+i+" survives numeric-group serialization");
        check(groups==2&&countEvents==1&&failureEvents==1&&trialEvents==1,"complete diagnostics use five bounded events");
        check(ProcessingTiming1947.lastGpu==65535&&ProcessingTiming1947.lastCpu==65535&&ProcessingTiming1947.verification==65535&&ProcessingTiming1947.reasons.size()==23&&ProcessingTiming1947.trials.size()==9,"numeric trace serialization also retains named settings telemetry");
    }
    static void reservationTargetsAndBoundary()throws Exception{
        reset();GpuQualification1961.rejectExact(key(3));StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,260,37,11);GpuStrong1960.beginStage(model);
        try{check(job.complete(),"pooled generic candidate completes");job.exactOutput("pooled generic reservation");}finally{close(model);}
        check(GpuNoise1960.reservations.size()==2,"one model and one leased range reservation covers all proof trials");
        GpuNoise1960.Reservation modelLease=GpuNoise1960.reservations.get(0),range=GpuNoise1960.reservations.get(1);
        check(Arrays.equals(modelLease.slots,new int[]{2,4,5,6})&&Arrays.equals(modelLease.target,new long[]{64,4000,1000,252})&&modelLease.javaBytes==0,"actual immutable model capacities are reserved once");
        check(Arrays.equals(range.slots,new int[]{0,1,3,7,13})&&Arrays.equals(range.target,new long[]{4244,4096,4,256,4}),"range reserves pooled source capacity and only logical output/confidence capacities");
        check(range.javaBytes==8L*(1024+64+1)+256,"both live readbacks plus headers are reserved exactly once");
        check(GpuNoise1960.reservationCalls==2&&GpuNoise1960.maxRangeLeases==1,"two GPU proof trials share one exclusive range lease");
        // For this fixture, model capacities 5316 + range capacities 8604 + private readbacks 8968 = 22888.
        long exact=22888;
        for(int below=0;below<2;below++){
            reset();GpuQualification1961.rejectExact(key(3));GpuNoise1960.reservationLimit=exact-below;model=new StrongNoise1958.Model();job=new Job(model,270+below,37,11);GpuStrong1960.beginStage(model);
            try{check(job.complete(),"reservation boundary always preserves complete output");job.exactOutput("reservation boundary");}finally{close(model);}
            check(ProcessingTiming1947.lastGpu==(below==0?1:0)&&ProcessingTiming1947.lastCpu==(below==0?0:1),"reservation predicate includes own complete range and succeeds exactly at boundary");
            check(job.cpuCalls==(below==0?2:1)&&ProcessingTiming1947.verification==(below==0?2:1),"reservation rejection preserves first cold CPU reference before any GPU trial");
            check(GpuNoise1960.executes==(below==0?2:0)&&GpuNoise1960.reservedJava==0,"reservation boundary retains no leaked readback reservation");
            check(reasons()==below&&(below==0||ProcessingTiming1947.reasons.containsKey("memory_budget")),"reservation rejection adds exactly one memory CPU reason");
        }
    }
    public static void main(String[] args)throws Exception{
        firstAndRepeatedSlow();threeSeparatePhotosRestoreSlowCertificate();mismatches();alternativeVariantsAndCapacity();failures();fourWorkersFifo();cancellation();negativeBeforeCommit();oracleFailureAndRefreshedNegative();preparationPreserved();genericAfterLegacyNegative();tiledMismatchThenGeneric();genericAlternativeVariant();genericSecondTrialRejected();classifiedNativePolicyAndShape();reservationTargetsAndBoundary();descriptorAndConfidenceOffReservation();budgetRevalidatedBeforeEachSubmit();boundedCompleteDiagnosticLogs();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"first_foreground_complete_trials\":2,\"foreground_quality_checked\":true,\"slow_gpu_preference_verified\":true,\"gpu_bank_fifo_verified\":true,\"cpu_fallback_safety_preserved\":true,\"preparation_five_percent_gate_preserved\":true,\"historical_exact_key_preserved\":true,\"generic_independent_certificate_verified\":true,\"generic_legacy_negative_recovery_verified\":true,\"trial_and_selected_cpu_counts_distinct\":true,\"pooled_range_reservation_targets_verified\":true,\"runtime_reservation_revalidated\":true,\"cold_cpu_oracle_precedes_gpu_model_allocation\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}
