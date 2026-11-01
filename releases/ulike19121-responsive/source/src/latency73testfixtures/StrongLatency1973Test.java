package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
/** Actual production Strong controls with deterministic transport and oracle gates. */
public final class StrongLatency1973Test {
    static int assertions;
    static final int SENTINEL=0x11335577;
    static final List<String> tests=new ArrayList<String>();
    interface Case {void run()throws Exception;}
    static synchronized void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static Field field(String name)throws Exception{Field f=GpuStrong1960.class.getDeclaredField(name);f.setAccessible(true);return f;}
    static void reset()throws Exception{
        GpuNoise1960.releaseAll();check(field("active").get(null)==null,"previous scenario released Strong stage");
        check(((Set<?>)field("FLIGHTS1973").get(null)).isEmpty()&&field("activeCold1973").getInt(null)==0,"previous scenario released every single-flight owner");
        for(String name:new String[]{"GATES","DETACHED1973"})((Map<?,?>)field(name).get(null)).clear();
        GpuNoise1960.reset();GpuQualification1961.reset();ProcessingTiming1947.reset();CameraTrace1965.events.clear();
        StrongNoise1958.oracleCalls.set(0);StrongNoise1958.cpuDelay=1000000L;
    }
    static void run(String name,Case c)throws Exception{reset();c.run();tests.add(name);System.out.println("PASS "+name);}
    static final class Job {
        final StrongNoise1958.Model model;final int[] source,output,confidence;
        int noise=4,origin,cpuCalls;boolean shadows=true,withConfidence=true,unstable,interruptFirst,throwSecond,assertFirstCold;
        GpuPolicy1960.Protection protection;
        Job(StrongNoise1958.Model m,int marker){model=m;source=new int[1024+17];output=new int[1024+17];confidence=new int[64+7];
            for(int i=0;i<source.length;i++)source[i]=0xff000000|((i*7919+marker)&0xffffff);source[0]=0xff100000|marker;
            Arrays.fill(output,SENTINEL);Arrays.fill(confidence,-991);}
        final GpuStrong1960.Oracle oracle=new GpuStrong1960.Oracle(){public boolean run(int[] out,int[] cf){
            try{Object stage=field("active").get(null);check(stage==null||!Thread.holdsLock(stage),"CPU oracle never runs while holding the Strong Stage monitor");}
            catch(Exception e){throw new AssertionError(e);}
            cpuCalls++;if(assertFirstCold&&cpuCalls==1)check(GpuNoise1960.opens==0&&GpuNoise1960.modelUploads==0&&GpuNoise1960.reservationCalls==0,
                "cold CPU reference precedes model transfer and allocation");
            boolean ok=StrongNoise1958.gpuOracleSnapshot1961(source,out,32,32,0,32,0,32,origin,noise,shadows,model,null,cf);
            if(unstable&&cpuCalls>=2)out[1023]^=0x00008000;
            if(throwSecond&&cpuCalls==2){out[1023]^=0x00002000;throw new IllegalStateException("partial CPU failure");}
            if(interruptFirst&&cpuCalls==1)Thread.currentThread().interrupt();return ok;
        }};
        boolean raw(){return GpuStrong1960.process(source,output,32,32,0,32,0,32,origin,noise,shadows,model,
            protection==null?null:new int[2048],withConfidence?confidence:null,protection,oracle);}
        boolean complete(){boolean result=raw();if(!result){if(Thread.currentThread().isInterrupted())throw new CancellationException();return oracle.run(output,confidence);}return true;}
        void exact(String label){int[] expected=new int[output.length];Arrays.fill(expected,SENTINEL);
            for(int i=0;i<1024;i++)expected[i]=StrongNoise1958.pixel(source[i]);if(unstable&&cpuCalls>=2)expected[1023]^=0x00008000;
            check(Arrays.equals(expected,output),label+" preserves all ARGB core and unused capacity tail");
            int[] cf=new int[confidence.length];Arrays.fill(cf,-991);if(withConfidence)Arrays.fill(cf,0,64,37);
            check(Arrays.equals(cf,confidence),label+" preserves logical confidence and unused tail");}
        int[] uniforms(){int[] u=new int[32];u[0]=u[1]=u[3]=u[5]=32;u[6]=origin;u[7]=model.height;u[8]=noise;u[9]=shadows?1:0;
            u[10]=3;u[11]=protection==null?0:1;u[12]=withConfidence?1:0;return u;}
    }
    static String key(Job job,int profile)throws Exception{Method m=GpuStrong1960.class.getDeclaredMethod("profileKey1973",int[].class,int.class);m.setAccessible(true);return (String)m.invoke(null,job.uniforms(),profile);}
    static void close(StrongNoise1958.Model model){GpuStrong1960.endStage(model);}
    static void certified(Job job,int profile,int variant)throws Exception{GpuQualification1961.qualifiedStrongPreferred1970(key(job,profile),1000000L,20000000L,variant);GpuQualification1961.preferredCalls=0;}
    static void clean()throws Exception{check(GpuNoise1960.opens==GpuNoise1960.closes&&GpuNoise1960.liveRangeLeases==0&&GpuNoise1960.reservedJava==0,
        "all GPU sessions and range leases released");check(((Set<?>)field("FLIGHTS1973").get(null)).isEmpty()&&field("activeCold1973").getInt(null)==0,
        "all single-flight slots released");}
    static int reason(String code){Integer n=ProcessingTiming1947.reasons.get(code);return n==null?0:n;}
    static void firstProofAndFastRestore()throws Exception{
        StrongNoise1958.cpuDelay=0L;GpuNoise1960.gpuDelay=20000000L;GpuNoise1960.warmDelay=2000000L;GpuNoise1960.supportDelay=1000000L;
        for(int photo=0;photo<3;photo++){
            ProcessingTiming1947.reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,10+photo);job.assertFirstCold=photo==0;
            int before=GpuNoise1960.executes;GpuStrong1960.beginStage(model);try{check(job.complete(),"photo completes");job.exact("photo "+photo);}finally{close(model);}
            check(job.cpuCalls==(photo==0?2:0)&&GpuNoise1960.executes-before==(photo==0?2:1),"new certificate needs two stable complete proofs, repeats need no CPU oracle");
            check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0,"only final output selection records GPU1 CPU0");
            check(ProcessingTiming1947.verification==(photo==0?2:0),"CPU proof calls remain independent of selected GPU output");
            check(ProcessingTiming1947.workCalls==1&&ProcessingTiming1947.gpuWork>=3000000L,"work includes warm environment, support and request scope");
            GpuQualification1961.Record saved=GpuQualification1961.restore(key(job,0));
            check(saved!=null&&saved.gpuNanos>saved.cpuNanos,"new profile preserves a measured GPU-slower-than-CPU preferred certificate before and after restart");
            ((Map<?,?>)field("GATES").get(null)).clear();
        }
        check(GpuQualification1961.preferredCalls==1,"three photos reuse one published exact certificate");
        check(GpuNoise1960.submittedPrograms.equals(Arrays.asList(39,39,39,39)),"new proof and repeats dispatch only the chosen exact program");clean();
    }
    static final class Worker implements Runnable {final Job job;final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();boolean done;
        Worker(Job j){job=j;}public void run(){try{done=job.complete();}catch(Throwable t){failure.set(t);}}}
    static void until(Callable<Boolean> state,String label)throws Exception{long end=System.nanoTime()+5000000000L;while(!state.call()){
        if(System.nanoTime()>=end)throw new AssertionError(label+" timed out");Thread.sleep(1);}check(true,label);}
    static void join(Thread t,String label)throws Exception{t.join(5000);check(!t.isAlive(),label);}
    static void coldSameKeySixteen()throws Exception{
        GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();final Worker[] workers=new Worker[16];Thread[] threads=new Thread[16];
        for(int i=0;i<16;i++){workers[i]=new Worker(new Job(model,100+i));threads[i]=new Thread(workers[i]);}
        GpuStrong1960.beginStage(model);try{
            threads[0].start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"one cold proof holds its first GPU candidate");
            for(int i=1;i<16;i++)threads[i].start();for(int i=1;i<16;i++)join(threads[i],"same-key follower returns without waiting for GPU proof");
            check(GpuNoise1960.submits==1,"sixteen same-key strips cannot launch duplicate cold GPU proofs");
            for(int i=1;i<16;i++){check(workers[i].failure.get()==null&&workers[i].done&&workers[i].job.cpuCalls==1,"same-key follower uses one ordinary CPU result");workers[i].job.exact("same-key follower");}
            GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();join(threads[0],"cold proof owner finishes");
            check(workers[0].failure.get()==null&&workers[0].done&&workers[0].job.cpuCalls==2,"only proof owner runs two CPU comparisons");workers[0].job.exact("proof owner");
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();for(Thread t:threads)if(t.isAlive())t.join(5000);close(model);}
        check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==15&&reason("proof_in_flight")==15,"single-flight fallback reasons match final CPU strip selections");
        check(ProcessingTiming1947.verification==2&&GpuNoise1960.executes==2,"same-key concurrency never amplifies verification work");clean();
    }
    static void coldDifferentKeysSixteen()throws Exception{
        GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();final Worker[] workers=new Worker[16];Thread[] threads=new Thread[16];
        for(int i=0;i<16;i++){Job job=new Job(model,200+i);job.noise=i+1;workers[i]=new Worker(job);threads[i]=new Thread(workers[i]);}
        GpuStrong1960.beginStage(model);try{
            threads[0].start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"first distinct proof holds bank");
            threads[1].start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==2;}},"second distinct proof holds bank");
            for(int i=2;i<16;i++)threads[i].start();for(int i=2;i<16;i++)join(threads[i],"distinct-key follower obeys cold-stage budget");
            check(GpuNoise1960.submits==2&&GpuNoise1960.maxInflight==2,"two private cold proof owners bound GPU work for sixteen distinct conditions");
            for(int i=2;i<16;i++){check(workers[i].failure.get()==null&&workers[i].done&&workers[i].job.cpuCalls==1,"budget fallback selects one complete CPU result");workers[i].job.exact("distinct-key fallback");}
            GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();join(threads[0],"first distinct proof finishes");join(threads[1],"second distinct proof finishes");
            for(int i=0;i<2;i++){check(workers[i].failure.get()==null&&workers[i].done&&workers[i].job.cpuCalls==2,"each admitted distinct proof completes two exact trials");workers[i].job.exact("distinct proof");}
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();for(Thread t:threads)if(t.isAlive())t.join(5000);close(model);}
        check(ProcessingTiming1947.lastGpu==2&&ProcessingTiming1947.lastCpu==14&&reason("cold_proof_budget")==14,"distinct-key budget has accurate selected-output counts");
        check(ProcessingTiming1947.verification==4&&GpuNoise1960.executes==4&&GpuQualification1961.preferredCalls==2,"cold-stage budget limits proof work to two owners");clean();
    }
    static void firstBadCandidateBudgetAndFullStats()throws Exception{
        GpuNoise1960.fullMismatch=true;StrongNoise1958.Model model=new StrongNoise1958.Model();GpuStrong1960.beginStage(model);
        try{for(int i=0;i<16;i++){Job job=new Job(model,300+i);job.noise=i+1;check(job.complete(),"bad candidate or budget preserves complete CPU output");job.exact("bad candidate CPU");check(job.cpuCalls==1,"first comparison failure does not launch another CPU reference");}}
        finally{close(model);}
        check(GpuNoise1960.executes==2&&GpuNoise1960.submits==2&&GpuQualification1961.preferredCalls==0,"first bad candidate consumes one attempt, not six-layout retries");
        check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==16&&reason("new_argb_mismatch")==2&&reason("cold_proof_budget")==14,"first failure and budget have one CPU reason per final strip");
        check(ProcessingTiming1947.firstX==17&&ProcessingTiming1947.firstY==0&&ProcessingTiming1947.firstDelta==1,"first scalar observation retains its own coordinate and one-channel difference");
        check(ProcessingTiming1947.mismatchCalls==1&&ProcessingTiming1947.argbCount==2&&ProcessingTiming1947.argbMax==17
            &&ProcessingTiming1947.confidenceCount==2&&ProcessingTiming1947.confidenceMax==5,"full ARGB and confidence scans are independent even after ARGB failure");
        check(GpuQualification1961.exact.isEmpty(),"a single bad layout is not a false permanent rejection of untested layouts");clean();
    }
    static void unstableSecondCpuAndPartialFailure()throws Exception{
        for(int failure=0;failure<2;failure++){
            if(failure>0)reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,400+failure);job.unstable=failure==0;job.throwSecond=failure==1;
            GpuStrong1960.beginStage(model);try{
                if(failure==0){check(job.complete(),"unstable second CPU retains latest complete CPU result");job.exact("unstable latest CPU");}
                else{check(!job.raw(),"partial second oracle failure declines GPU commit for caller repair");check(job.oracle.run(job.output,job.confidence),"caller can repair partial CPU write");job.exact("repaired CPU");}
                check(GpuNoise1960.executes==1&&GpuQualification1961.preferredCalls==0,"unstable or failed second CPU never submits or certifies a second GPU candidate");
            }finally{close(model);}
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"CPU instability never records a GPU output commit");clean();
        }
    }
    static void lateCertificateAndNegativeAfterBankWait()throws Exception{
        for(int negative=0;negative<3;negative++){
            if(negative>0)reset();GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();Job prototype=new Job(model,500);certified(prototype,0,0);
            final Worker a=new Worker(new Job(model,501)),b=new Worker(new Job(model,502)),waiting=new Worker(new Job(model,503));
            Thread ta=new Thread(a),tb=new Thread(b),tw=new Thread(waiting);GpuStrong1960.beginStage(model);try{
                ta.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"certified first bank");
                tb.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==2;}},"certified second bank");
                tw.start();until(new Callable<Boolean>(){public Boolean call(){return tw.getState()==Thread.State.WAITING||tw.getState()==Thread.State.TIMED_WAITING;}},"certified caller queues without CPU proof");
                ((Map<?,?>)field("GATES").get(null)).clear();
                if(negative==0)GpuQualification1961.qualifiedStrongPreferred1970(key(prototype,0),1000000L,20000000L,1);
                else if(negative==1)GpuQualification1961.rejectExact1971(key(prototype,0),"argb_mismatch");
                else GpuQualification1961.records.remove(key(prototype,0));
                GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();join(ta,"first certified worker");join(tb,"second certified worker");join(tw,"queued certified worker");
                check(waiting.failure.get()==null&&waiting.done,"queued worker refreshes changed qualification state");waiting.job.exact("queued output");
                if(negative==0){check(waiting.job.cpuCalls==0&&GpuNoise1960.submittedPrograms.contains(40),"late certificate refreshes actual layout without duplicate CPU verification");}
                else{check(waiting.job.cpuCalls==1&&GpuNoise1960.submits==2,"lost qualification prevents a private GPU submit after bank wait");}
                if(negative==2)check(ProcessingTiming1947.verification==0&&GpuQualification1961.preferredCalls==0
                    &&((Set<?>)field("FLIGHTS1973").get(null)).isEmpty(),"lost positive cannot bypass cold admission or launch a new CPU proof after waiting");
            }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();for(Thread t:new Thread[]{ta,tb,tw})if(t.isAlive())t.join(5000);close(model);}
            check(ProcessingTiming1947.waitWork>0,"bank waiting duration is measured separately from oracle and request work");clean();
        }
    }
    static void interruptionAndNativeFailureReleaseFlights()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,600);job.interruptFirst=true;GpuStrong1960.beginStage(model);
        try{boolean cancelled=false;try{job.raw();}catch(CancellationException expected){cancelled=true;}check(cancelled,"interrupted cold reference propagates cancellation");job.exact("completed reference before cancellation");}
        finally{Thread.interrupted();close(model);}
        check(GpuNoise1960.submits==0&&GpuQualification1961.preferredCalls==0,"interrupted oracle cannot publish proof or output");clean();
        reset();GpuNoise1960.failSubmit=true;GpuNoise1960.nativeFailureCode=4;model=new StrongNoise1958.Model();job=new Job(model,601);GpuStrong1960.beginStage(model);
        try{check(job.complete(),"native submit failure preserves reference CPU output");job.exact("native failure CPU");}finally{close(model);}
        check(GpuQualification1961.preferredCalls==0&&ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"native failure has no certificate or GPU commit");clean();
        reset();model=new StrongNoise1958.Model();job=new Job(model,602);GpuStrong1960.beginStage(model);try{check(job.complete(),"new stage after failed owner can qualify normally");job.exact("after failure proof");}finally{close(model);}
        check(ProcessingTiming1947.lastGpu==1&&job.cpuCalls==2,"failed single-flight owner never poisons later stage proof ownership");clean();
    }
    static void boundedAllReasonLogs()throws Exception{
        StrongNoise1958.Model model=new StrongNoise1958.Model();GpuStrong1960.beginStage(model);Object stage=field("active").get(null);
        Field reasons=stage.getClass().getDeclaredField("reasons"),trials=stage.getClass().getDeclaredField("trials1971");reasons.setAccessible(true);trials.setAccessible(true);
        int[] r=(int[])reasons.get(stage),t=(int[])trials.get(stage);check(r.length==27&&t.length==9,"production uses fixed bounded reason and trial slots");Arrays.fill(r,65535);Arrays.fill(t,65535);close(model);
        int observed=0;for(String event:CameraTrace1965.events){int at=event.indexOf(" 0 ");check(at>0,"trace scalar event has known framing");String fields=event.substring(at+3);
            check(fields.length()<=160,"worst-case integer reason groups fit actual CameraTrace160char field limit");if(event.startsWith("strong_gpu_reasons1971 "))for(String word:fields.split(" "))if(word.matches("r[0-9]+=65535"))observed++;}
        check(observed==27,"all27 maximum reason counts survive split diagnostic events without tail clipping");clean();
    }
    static void confidenceAbsentAndPolicyRejection()throws Exception{
        GpuNoise1960.fullMismatch=true;StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,700);job.withConfidence=false;
        GpuStrong1960.beginStage(model);try{check(job.complete(),"ARGB mismatch without confidence retains CPU");job.exact("confidence absent CPU");}finally{close(model);}
        check(ProcessingTiming1947.argbCount==2&&ProcessingTiming1947.argbMax==17&&ProcessingTiming1947.confidenceCount== -1
            &&ProcessingTiming1947.confidenceMax== -1,"disabled logical confidence remains unavailable rather than fabricated exact zeros");clean();
        reset();model=new StrongNoise1958.Model();job=new Job(model,701);job.protection=new GpuPolicy1960.Protection();job.protection.plan=new GpuPolicy1960.Plan();
        certified(job,0,0);GpuQualification1961.rejectExact1971(key(job,1),"policy_failure");GpuQualification1961.rejectExact1971(key(job,2),"policy_failure");
        GpuNoise1960.policyFlag=1;String rejected=key(job,0);
        for(int photo=0;photo<3;photo++){
            ProcessingTiming1947.reset();Job current=new Job(model,701+photo);current.protection=job.protection;GpuStrong1960.beginStage(model);
            try{check(current.complete(),"policy failure/cached rejection preserves CPU capture "+photo);current.exact("policy capture "+photo);}finally{close(model);}
            check(GpuQualification1961.exactRejected(rejected)&&GpuQualification1961.restore(rejected)==null,
                "common policy flag removes accepted route and preserves permanent policy cause");
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1,"policy flag never commits GPU pixels");
            if(photo>0)check(reason("cached_policy_negative")==1&&current.cpuCalls==1,
                "next capture reuses truthful cached policy rejection without repeated GPU verification");
        }
        check(GpuNoise1960.executes==1&&GpuNoise1960.submits==1,"three policy captures cannot repeatedly execute the same failed accepted route");clean();
    }
    static void collectorInterruptAndLeaseCloseCleanup()throws Exception{
        GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();final Worker worker=new Worker(new Job(model,800));Thread thread=new Thread(worker);
        GpuStrong1960.beginStage(model);try{
            thread.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"cold GPU collection is interruptible");
            thread.interrupt();join(thread,"interrupted cold GPU collector terminates");check(worker.failure.get() instanceof CancellationException,
                "GPU collection interruption propagates without consuming a completed output");
            check(((Set<?>)field("FLIGHTS1973").get(null)).isEmpty()&&field("activeCold1973").getInt(null)==0,
                "interrupted collector releases global flight immediately before stage close");
            GpuNoise1960.holdTickets=false;Job next=new Job(model,801);next.noise=3;check(next.complete(),"another key uses released bank and cold slot");next.exact("after collector interrupt");
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();if(thread.isAlive())thread.join(5000);close(model);}clean();
        reset();GpuNoise1960.throwLeaseClose=true;model=new StrongNoise1958.Model();Job job=new Job(model,802);GpuStrong1960.beginStage(model);
        try{
            boolean failed=false;try{job.raw();}catch(IllegalStateException expected){failed=true;}check(failed,"controlled range lease close error reaches caller");job.exact("private output before cleanup error");
            Object stage=field("active").get(null);Field banks=stage.getClass().getDeclaredField("banks");banks.setAccessible(true);
            for(boolean owned:(boolean[])banks.get(stage))check(!owned,"lease close error cannot prevent bank release");
            check(((Set<?>)field("FLIGHTS1973").get(null)).isEmpty()&&field("activeCold1973").getInt(null)==0,
                "lease close error cannot strand a global single-flight owner");
            GpuNoise1960.throwLeaseClose=false;Job next=new Job(model,803);check(next.complete(),"released bank remains usable after cleanup error");next.exact("after lease close error");
        }finally{GpuNoise1960.throwLeaseClose=false;close(model);}clean();
    }
    static void firstRuntimeFailureCannotBorrowAnotherScan()throws Exception{
        GpuNoise1960.holdTickets=true;GpuNoise1960.fullMismatch=true;StrongNoise1958.Model model=new StrongNoise1958.Model();
        final Worker first=new Worker(new Job(model,900)),second=new Worker(new Job(model,901));second.job.noise=3;
        Thread a=new Thread(first),b=new Thread(second);GpuStrong1960.beginStage(model);try{
            a.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"first runtime candidate submitted");
            b.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==2;}},"second independent candidate submitted");
            GpuNoise1960.failedCollectTickets.add(1);GpuNoise1960.releaseTicket(1);join(a,"first candidate runtime failure completes CPU fallback");
            GpuNoise1960.releaseTicket(2);join(b,"second candidate mismatch completes CPU fallback");
            check(first.failure.get()==null&&second.failure.get()==null&&first.done&&second.done,"both failed private candidates preserve complete CPU output");
            first.job.exact("first runtime CPU");second.job.exact("later mismatch CPU");
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();if(a.isAlive())a.join(5000);if(b.isAlive())b.join(5000);close(model);}
        check("readback_failed".equals(ProcessingTiming1947.firstFailure)&&ProcessingTiming1947.firstX== -1&&ProcessingTiming1947.firstDelta== -1,
            "earliest runtime failure remains the first candidate without fabricated pixel coordinates");
        check(ProcessingTiming1947.argbCount== -1&&ProcessingTiming1947.argbMax== -1&&ProcessingTiming1947.confidenceCount== -1
            &&ProcessingTiming1947.confidenceMax== -1,"full scan from a later candidate cannot attach to an earlier runtime failure object");
        check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==2,"runtime and comparison failures still count only final CPU outputs");clean();
    }
    static void detachedScalarStateAndGlobalSlotsAreBounded()throws Exception{
        Method admit=GpuStrong1960.class.getDeclaredMethod("admit1973",Class.forName("com.hiro.ulike.GpuStrong1960$Stage"),StrongNoise1958.Model.class,String.class);
        Method release=GpuStrong1960.class.getDeclaredMethod("release1973",Class.forName("com.hiro.ulike.GpuStrong1960$Flight1973"));admit.setAccessible(true);release.setAccessible(true);
        List<StrongNoise1958.Model> retained=new ArrayList<StrongNoise1958.Model>();
        for(int i=0;i<70;i++){StrongNoise1958.Model model=new StrongNoise1958.Model();retained.add(model);Object flight=admit.invoke(null,null,model,"bounded-key-"+i);release.invoke(null,flight);}
        check(((Map<?,?>)field("DETACHED1973").get(null)).size()<=64,"weak detached model registry remains bounded even while every model is alive");
        Object one=admit.invoke(null,null,new StrongNoise1958.Model(),"global-one"),two=admit.invoke(null,null,new StrongNoise1958.Model(),"global-two"),three=admit.invoke(null,null,new StrongNoise1958.Model(),"global-three");
        try{Field failure=three.getClass().getDeclaredField("failure");failure.setAccessible(true);
            check("cold_bank_busy".equals(failure.get(three))&&field("activeCold1973").getInt(null)==2,"global cold admission cannot exceed two detached owners");}
        finally{release.invoke(null,one);release.invoke(null,two);release.invoke(null,three);}clean();
    }
    static void elapsedAdmissionStillAllowsMandatorySecondProof()throws Exception{
        GpuNoise1960.holdTickets=true;StrongNoise1958.Model model=new StrongNoise1958.Model();final Worker owner=new Worker(new Job(model,1000));Thread thread=new Thread(owner);
        GpuStrong1960.beginStage(model);try{
            thread.start();until(new Callable<Boolean>(){public Boolean call(){return GpuNoise1960.submits==1;}},"already admitted proof holds its first candidate");
            Object stage=field("active").get(null);Field state=stage.getClass().getDeclaredField("cold1973");state.setAccessible(true);Object cold=state.get(stage);
            Field start=cold.getClass().getDeclaredField("start");start.setAccessible(true);start.setLong(cold,System.nanoTime()-field("COLD_ADMISSION_NS1973").getLong(null)-1000000L);
            Job late=new Job(model,1001);late.noise=3;check(late.complete(),"elapsed new admission completes one CPU route");late.exact("elapsed CPU fallback");
            check(late.cpuCalls==1&&GpuNoise1960.submits==1,"elapsed budget blocks new candidate rather than mandatory owner trial");
            GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();join(thread,"already admitted owner finishes its second proof after admission deadline");
            check(owner.failure.get()==null&&owner.done&&owner.job.cpuCalls==2&&GpuNoise1960.executes==2,
                "elapsed admission limit never truncates the two-exact-trial safety requirement");owner.job.exact("late completed certificate");
        }finally{GpuNoise1960.holdTickets=false;GpuNoise1960.releaseAll();if(thread.isAlive())thread.join(5000);close(model);}
        check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==1&&reason("cold_proof_budget")==1,
            "elapsed admission reasons stay separate from successfully completed GPU proof");clean();
    }
    static void detachedModelUsesAtMostTwoColdProofs()throws Exception{
        GpuNoise1960.badAlwaysPixel=true;StrongNoise1958.Model model=new StrongNoise1958.Model();
        for(int i=0;i<16;i++){Job job=new Job(model,1100+i);job.noise=i+1;check(job.complete(),"detached path retains completed CPU output");job.exact("detached output");
            check(job.cpuCalls==1,"detached failure or budget cannot duplicate a CPU reference");}
        check(GpuNoise1960.executes==2&&GpuNoise1960.submits==2&&GpuQualification1961.preferredCalls==0,
            "detached model is subject to the same two-attempt cold proof budget");
        check(reason("cold_proof_budget")==14&&((Map<?,?>)field("DETACHED1973").get(null)).size()==1,
            "detached fallback records scalar budget state per weak model without a Stage");clean();
    }
    static void memoryAndUnsupportedCapabilityKeepCpu()throws Exception{
        for(int fault=0;fault<3;fault++){
            if(fault>0)reset();if(fault==0)GpuNoise1960.fits=false;else if(fault==1)GpuNoise1960.reservationLimit=8000L;else GpuNoise1960.unsupportedExactMask=1;
            StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,1200+fault);GpuStrong1960.beginStage(model);
            try{check(job.complete(),"memory or shader refusal preserves complete CPU result");job.exact("capacity or capability fallback");}
            finally{close(model);}
            check(job.cpuCalls==1&&GpuNoise1960.executes==0&&GpuQualification1961.preferredCalls==0,
                "pre-dispatch capacity/capability failure never duplicates CPU reference or certifies GPU");
            check(ProcessingTiming1947.lastGpu==0&&ProcessingTiming1947.lastCpu==1&&reason(fault<2?"memory_budget":"shader_unsupported")==1,
                "capacity and shader capability have one truthful selected CPU reason");
            check(GpuQualification1961.exact.isEmpty(),"memory/shader refusal never becomes a false pixel inequality cache entry");clean();
            if(fault==2){model=new StrongNoise1958.Model();Job next=new Job(model,1203);GpuStrong1960.beginStage(model);
                try{check(next.complete(),"next supported exact layout may qualify on a later photo");next.exact("supported alternative layout");}finally{close(model);}
                GpuQualification1961.Record saved=GpuQualification1961.restore(key(next,0));check(saved!=null&&saved.variant==1&&next.cpuCalls==2,
                    "unsupported layout mask skips only that variant and still requires two own exact proofs");clean();}
        }
    }
    static void retainedCertificatesAndHistoricalNegativesStaySeparate()throws Exception{
        for(int profile=1;profile<=2;profile++){
            if(profile>1)reset();StrongNoise1958.Model model=new StrongNoise1958.Model();Job job=new Job(model,1300+profile);
            GpuQualification1961.rejectExact(key(job,0));if(profile==2)GpuQualification1961.rejectExact(key(job,1));certified(job,profile,0);
            GpuStrong1960.beginStage(model);try{check(job.complete(),"retained historical exact certificate remains usable");job.exact("retained profile");}finally{close(model);}
            check(job.cpuCalls==0&&GpuNoise1960.executes==1&&GpuQualification1961.preferredCalls==0,
                "new exact profile introduction never forces duplicate CPU validation of retained positive certificate");
            check(GpuNoise1960.submittedPrograms.equals(Arrays.asList(profile==1?30:0)),"retained certificate dispatches its original shader family");
            check(GpuQualification1961.exactRejected(key(job,0))&&GpuQualification1961.restore(key(job,0))==null,
                "retained positive certificate cannot clear another profile's historical negative");
            check(ProcessingTiming1947.lastGpu==1&&ProcessingTiming1947.lastCpu==0,"retained actual GPU commit retains honest per-photo output counts");clean();
        }
    }
    public static void main(String[] args)throws Exception{
        run("two_exact_proofs_then_three_photos_restore_without_cpu",new Case(){public void run()throws Exception{firstProofAndFastRestore();}});
        run("sixteen_same_keys_singleflight_without_follower_gpu_wait",new Case(){public void run()throws Exception{coldSameKeySixteen();}});
        run("sixteen_distinct_keys_obey_two_cold_owner_budget",new Case(){public void run()throws Exception{coldDifferentKeysSixteen();}});
        run("first_bad_candidate_stops_and_scans_argb_confidence_independently",new Case(){public void run()throws Exception{firstBadCandidateBudgetAndFullStats();}});
        run("unstable_second_cpu_and_partial_oracle_failure_never_certify",new Case(){public void run()throws Exception{unstableSecondCpuAndPartialFailure();}});
        run("bank_wait_refreshes_late_certificate_and_exact_negative",new Case(){public void run()throws Exception{lateCertificateAndNegativeAfterBankWait();}});
        run("interruption_and_native_failure_release_singleflight",new Case(){public void run()throws Exception{interruptionAndNativeFailureReleaseFlights();}});
        run("all27_reason_logs_fit_bounded_trace_events",new Case(){public void run()throws Exception{boundedAllReasonLogs();}});
        run("disabled_confidence_and_policy_negative_have_truthful_persisted_causes",new Case(){public void run()throws Exception{confidenceAbsentAndPolicyRejection();}});
        run("collector_interrupt_and_lease_close_error_release_banks_and_flights",new Case(){public void run()throws Exception{collectorInterruptAndLeaseCloseCleanup();}});
        run("first_runtime_failure_never_borrows_later_candidate_scan",new Case(){public void run()throws Exception{firstRuntimeFailureCannotBorrowAnotherScan();}});
        run("weak_detached_scalar_state_and_global_cold_slots_are_bounded",new Case(){public void run()throws Exception{detachedScalarStateAndGlobalSlotsAreBounded();}});
        run("elapsed_cold_admission_blocks_new_owner_without_truncating_two_exact_trials",new Case(){public void run()throws Exception{elapsedAdmissionStillAllowsMandatorySecondProof();}});
        run("detached_model_obeys_two_cold_proofs_for_sixteen_distinct_conditions",new Case(){public void run()throws Exception{detachedModelUsesAtMostTwoColdProofs();}});
        run("memory_and_unsupported_layout_refusal_preserve_cpu_without_false_quality_negative",new Case(){public void run()throws Exception{memoryAndUnsupportedCapabilityKeepCpu();}});
        run("historical_tiled_generic_certificates_and_other_profile_negatives_are_preserved",new Case(){public void run()throws Exception{retainedCertificatesAndHistoricalNegativesStaySeparate();}});
        StringBuilder names=new StringBuilder("[");for(String name:tests){if(names.length()>1)names.append(',');names.append('"').append(name).append('"');}names.append(']');
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"strong_latency_controls_regressions_passed\":true,\"strong_latency_regressions_passed\":true,\"latency_budget_regressions_passed\":true,\"cold_proof_bounds_passed\":true,\"strong_singleflight_regressions_passed\":true,\"strong_first_mismatch_identity_regressions_passed\":true,\"physical_android_tested\":false,\"device_speedup_verified\":false,\"tests\":"+names+"}");
    }
}
