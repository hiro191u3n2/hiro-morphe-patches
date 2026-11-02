package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Exercises real whole-route admission with explicitly mocked owned images.
 * These tests check lifecycle/selection; shader equality is tested separately.
 */
public final class WholeRoute1952Test {
    private static int assertions,scenarios;
    private static void req(boolean condition,String reason) {
        assertions++;if(!condition)throw new AssertionError(reason);
    }
    private static final class Timer extends WholeRoute1952.Clock {
        private long time;
        synchronized long now() { return time; }
        synchronized void spend(long nanos) { time+=nanos; }
    }
    private static int pixel(int value) {
        int r=(value>>>16)&255,g=(value>>>8)&255,b=value&255;
        return (value&0xff000000)|((r*3+g)/4<<16)|((g*7+b)/8<<8)|((b*7+r)/8);
    }
    private static int[] photograph(int seed) {
        int[] source=new int[37*19];long state=seed;
        for(int i=0;i<source.length;i++) {
            state=state*1664525+1013904223;source[i]=(int)state;
        }
        return source;
    }
    private static final class Photo implements WholeRoute1952.Work {
        final Timer clock;final int[] before,original;
        int[] candidate,published;
        int gpuCalls,cpuCalls,equalCalls,publicationCalls,discardCalls;
        long preparation=10,kernel=5,contextWait=10,readback=5,cpuTime=100;
        boolean bad,miss,reliable=true,cpuFalse,cpuThrow,discardThrow,publishThrow;
        int gpuThrow,equalThrow;
        CountDownLatch entered,release;
        String order="";
        Photo(Timer clock,int seed) {
            this.clock=clock;before=photograph(seed);original=before.clone();published=original;
        }
        public boolean gpu() {
            gpuCalls++;order+="G";candidate=original.clone();clock.spend(preparation);
            if(entered!=null) {
                entered.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS))throw new AssertionError("GPU test blocked"); }
                catch(InterruptedException stopped) { Thread.currentThread().interrupt();throw new IllegalStateException(stopped); }
            }
            clock.spend(contextWait);
            for(int i=0;i<candidate.length;i++)candidate[i]=pixel(candidate[i]);
            clock.spend(kernel);if(bad)candidate[candidate.length-1]^=1;
            throwScript(gpuThrow);clock.spend(readback);return !miss;
        }
        public boolean cpu() {
            cpuCalls++;order+="C";
            if(cpuThrow)throw new IllegalStateException("original CPU failure");
            for(int i=0;i<original.length;i++)original[i]=pixel(original[i]);
            clock.spend(cpuTime);return !cpuFalse;
        }
        public boolean equal() {
            equalCalls++;order+="E";throwScript(equalThrow);
            return Arrays.equals(candidate,original);
        }
        public void publishGpu() {
            publicationCalls++;order+="P";
            if(publishThrow)throw new IllegalStateException("candidate publication failed");
            published=candidate;
        }
        public void discardGpu() {
            discardCalls++;order+="D";
            if(published!=candidate)candidate=null;
            if(discardThrow)throw new IllegalStateException("candidate cleanup failed");
        }
        public boolean timingReliable() { return reliable; }
        void correct() {
            int[] expected=before.clone();for(int i=0;i<expected.length;i++)expected[i]=pixel(expected[i]);
            req(Arrays.equals(expected,published),"exact current photograph output");
        }
        void cpuResult() {
            correct();req(published==original,"reference photograph retained during probation/failure");
        }
    }
    private static void throwScript(int kind) {
        if(kind==1)throw new OutOfMemoryError("scripted candidate allocation failure");
        if(kind==2)throw new UnsatisfiedLinkError("scripted GPU ABI failure");
        if(kind==3)throw new IllegalStateException("scripted GPU runtime failure");
    }
    private static int[] key(int setting) { return new int[]{37,19,1,1,setting,0,19,4}; }
    private static void train(WholeRoute1952.Engine gate,Timer clock,int[] key) {
        for(int i=0;i<3;i++) {
            Photo photo=new Photo(clock,41+i);if(i==0)photo.preparation=1000;
            req(gate.run(key,photo),"whole route probation succeeded");photo.cpuResult();
            req(photo.cpuCalls==1&&photo.gpuCalls==1&&photo.equalCalls==1,"full paired probation");
            req(photo.order.equals("GCED"),"candidate before untouched reference; final comparison then cleanup");
        }
    }
    private static void admissionAndCurrentInputs() {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        train(gate,clock,key(3));
        for(int i=0;i<5;i++) {
            Photo photo=new Photo(clock,900+i);
            req(gate.run(key(3),photo),"admitted route success");photo.correct();
            req(photo.gpuCalls==1&&photo.cpuCalls==0&&photo.equalCalls==0,"no full CPU duplicate after admission");
            req(photo.order.equals("GPD"),"owned candidate published then released safely");
            req(Arrays.equals(photo.original,photo.before),"GPU route leaves immutable original pixels available for fallback");
            req(photo.candidate==photo.published,"successful cleanup preserves published image");
        }
        Photo changed=new Photo(clock,1200);
        req(gate.run(key(4),changed),"changed captured setting probation");changed.cpuResult();
        req(changed.cpuCalls==1,"changed complete settings key is independent");
    }
    private static void wholeCostSelection(int expensivePart) {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        Photo warm=new Photo(clock,1);req(gate.run(key(2),warm),"timing cold result");warm.cpuResult();
        Photo slow=new Photo(clock,2);
        if(expensivePart==0)slow.preparation=105;
        if(expensivePart==1)slow.contextWait=105;
        if(expensivePart==2)slow.readback=105;
        req(gate.run(key(2),slow),"slower whole route keeps reference");slow.cpuResult();
        req(slow.kernel==5,"a fast kernel alone does not admit slow route");
        Photo next=new Photo(clock,3);req(gate.run(key(2),next),"slow route original fallback");next.cpuResult();
        req(next.gpuCalls==0&&next.discardCalls==0,"rejected whole-route candidate is not retried");
    }
    private static void mismatch(int atProbe) {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        for(int i=0;i<=atProbe;i++) {
            Photo photo=new Photo(clock,9+i);photo.bad=i==atProbe;
            req(gate.run(key(5),photo),"mismatch reference succeeds");photo.cpuResult();
            req(photo.publicationCalls==0,"mismatching candidate never published");
        }
        Photo next=new Photo(clock,52);req(gate.run(key(5),next),"mismatch CPU fallback");next.cpuResult();
        req(next.gpuCalls==0,"mismatch disables route shape");
    }
    private static void candidateFailures(int failure,boolean admitted) {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        if(admitted)train(gate,clock,key(7));
        Photo photo=new Photo(clock,601);photo.gpuThrow=failure;
        req(gate.run(key(7),photo),"candidate failure runs unchanged original CPU");photo.cpuResult();
        req(photo.discardCalls==1&&photo.candidate==null,"failed partial candidate is released");
        Photo next=new Photo(clock,602);req(gate.run(key(7),next),"disabled route CPU");next.cpuResult();
        req(next.gpuCalls==0,"no failed candidate retry storm");
        Photo other=new Photo(clock,603);req(gate.run(key(8),other),"another route still works");other.cpuResult();
        req(other.gpuCalls==(failure==2?0:1),"link failure globally disables; memory/runtime failures disable shape");
    }
    private static void returnedFailureAndPublication() {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        Photo miss=new Photo(clock,71);miss.miss=true;
        req(gate.run(key(1),miss),"false native result uses CPU");miss.cpuResult();
        req(miss.equalCalls==0&&miss.discardCalls==1,"invalid native candidate unconditionally released");
        train(gate,clock,key(2));Photo publication=new Photo(clock,72);publication.publishThrow=true;
        req(gate.run(key(2),publication),"ownership publication failure restores CPU");publication.cpuResult();
        req(publication.discardCalls==1&&publication.candidate==null,"unpublished complete candidate released");
    }
    private static void comparisonAndCleanupFailures() {
        for(int kind=1;kind<=3;kind++) {
            scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
            Photo photo=new Photo(clock,81+kind);photo.equalThrow=kind;
            req(gate.run(key(4),photo),"failed exact comparison retains reference");photo.cpuResult();
            req(photo.publicationCalls==0&&photo.candidate==null,"comparison failure no candidate publication");
            Photo next=new Photo(clock,95);req(gate.run(key(4),next),"comparison failure disables");
            req(next.gpuCalls==0,"failed equality never admits");
        }
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        Photo cleanup=new Photo(clock,91);cleanup.discardThrow=true;
        req(gate.run(key(4),cleanup),"cleanup cannot replace successful CPU output");cleanup.cpuResult();
        Photo next=new Photo(clock,92);req(gate.run(key(4),next),"cleanup failed lease returned");
        req(next.gpuCalls==0,"cleanup error disables uncertain candidate");
    }
    private static void originalFailure() {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        Photo photo=new Photo(clock,11);photo.cpuFalse=true;
        req(!gate.run(key(1),photo),"original CPU failure is not hidden by candidate");
        req(photo.publicationCalls==0&&photo.discardCalls==1,"failed reference can't admit candidate");
        Photo thrown=new Photo(clock,12);thrown.cpuThrow=true;boolean propagated=false;
        try { gate.run(key(2),thrown); }
        catch(IllegalStateException expected) { propagated=expected.getMessage().equals("original CPU failure"); }
        req(propagated&&thrown.discardCalls==1,"original CPU exception propagates after candidate cleanup");
        Photo next=new Photo(clock,13);req(gate.run(key(2),next),"failed reference route original next time");
        req(next.gpuCalls==0,"reference exception prevents admission");
    }
    private static void unstableAndZeroTiming() {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        for(int i=0;i<4;i++) {
            Photo photo=new Photo(clock,41+i);photo.reliable=false;
            req(gate.run(key(1),photo),"unstable load exact CPU result");photo.cpuResult();
        }
        for(int i=0;i<2;i++) {
            Photo photo=new Photo(clock,50+i);req(gate.run(key(1),photo),"stable complete-route probes");photo.cpuResult();
        }
        Photo admitted=new Photo(clock,52);req(gate.run(key(1),admitted),"two stable complete wins admit");
        req(admitted.cpuCalls==0,"unstable comparisons didn't count as speed wins");
        Photo warm=new Photo(clock,55);req(gate.run(key(2),warm),"zero timing warmup");
        Photo zero=new Photo(clock,56);zero.preparation=zero.kernel=zero.contextWait=zero.readback=zero.cpuTime=0;
        req(gate.run(key(2),zero),"zero measured time retains CPU");zero.cpuResult();
        Photo next=new Photo(clock,57);req(gate.run(key(2),next),"zero duration rejects timing claim");
        req(next.gpuCalls==0,"zero time never admits faster route");
    }
    private static void admittedSlowdown() {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        train(gate,clock,key(6));
        Photo unstable=new Photo(clock,700);unstable.preparation=600;unstable.reliable=false;
        req(gate.run(key(6),unstable),"changing contention still publishes completed exact GPU image");
        unstable.correct();req(unstable.cpuCalls==0,"no duplicate on unreliable GPU timing");
        Photo normal=new Photo(clock,701);req(gate.run(key(6),normal),"unreliable slow interval didn't reject route");
        normal.correct();req(normal.cpuCalls==0,"admitted route retained after unreliable interval");
        Photo below=new Photo(clock,702);below.preparation=89; // total109 vs CPU100
        req(gate.run(key(6),below),"less than ten percent slowdown tolerated");below.correct();
        Photo boundary=new Photo(clock,703);boundary.preparation=90; // total110
        req(gate.run(key(6),boundary),"thermal/load threshold publishes current GPU result");boundary.correct();
        req(boundary.cpuCalls==0,"current slowed photograph has no complete CPU duplicate");
        Photo next=new Photo(clock,704);req(gate.run(key(6),next),"following photograph returns to complete CPU route");
        next.cpuResult();req(next.gpuCalls==0,"reliable complete slowdown rejects subsequent GPU dispatch");
    }
    private static void keyOwnershipAndBounds() {
        scenarios++;Timer clock=new Timer();WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        int[] mutable=key(2);Photo warm=new Photo(clock,6);req(gate.run(mutable,warm),"original settings warmup");
        mutable[4]=9;
        for(int i=0;i<2;i++) { Photo photo=new Photo(clock,7+i);req(gate.run(key(2),photo),"immutable admission key copy");photo.cpuResult(); }
        Photo admitted=new Photo(clock,9);req(gate.run(key(2),admitted),"caller mutation didn't corrupt cached key");
        req(admitted.cpuCalls==0,"copied original key retains two wins");
        int[][] invalid={null,new int[0],new int[65]};
        for(int[] key:invalid) {
            Photo photo=new Photo(clock,10);req(gate.run(key,photo),"invalid cache key uses original");photo.cpuResult();
            req(photo.gpuCalls==0,"invalid key no optional GPU allocation");
        }
        for(int i=20;i<50;i++) { Photo photo=new Photo(clock,i);req(gate.run(key(i),photo),"bounded shape table cycles");photo.cpuResult(); }
        Photo evicted=new Photo(clock,13);req(gate.run(key(2),evicted),"evicted shape retrains");
        req(evicted.cpuCalls==1&&evicted.equalCalls==1,"evicted timings never reuse old image or unstaged GPU output");
    }
    private static void contention() throws Exception {
        scenarios++;final Timer clock=new Timer();final WholeRoute1952.Engine gate=new WholeRoute1952.Engine(clock);
        final Photo first=new Photo(clock,111);first.entered=new CountDownLatch(1);first.release=new CountDownLatch(1);
        final Throwable[] failure=new Throwable[1];
        Thread worker=new Thread(new Runnable() { public void run() {
            try { if(!gate.run(key(5),first))throw new AssertionError("blocked probe failed"); }
            catch(Throwable thrown) { failure[0]=thrown; }
        }});
        worker.start();req(first.entered.await(5,TimeUnit.SECONDS),"first route lease begins");
        Photo competing=new Photo(clock,112);req(gate.run(key(5),competing),"competing photograph immediately uses CPU");
        competing.cpuResult();req(competing.gpuCalls==0,"same-shape GPU comparison never overlaps");
        // Pressure the bounded table while its oldest entry has a live lease.
        for(int i=0;i<30;i++) { Photo different=new Photo(clock,200+i);req(gate.run(key(30+i),different),"other settings during live lease"); }
        Photo same=new Photo(clock,113);req(gate.run(key(5),same),"leased key remains registered after cache pressure");
        req(same.gpuCalls==0,"in-flight shape never evicted or independently reprobed");
        first.release.countDown();worker.join(5000);req(!worker.isAlive(),"probe finishes without deadlock");
        if(failure[0]!=null)throw new AssertionError(failure[0]);first.cpuResult();
    }
    public static void main(String[] args)throws Exception {
        admissionAndCurrentInputs();for(int i=0;i<3;i++)wholeCostSelection(i);
        mismatch(0);mismatch(2);
        for(int i=1;i<=3;i++) { candidateFailures(i,false);candidateFailures(i,true); }
        returnedFailureAndPublication();comparisonAndCleanupFailures();originalFailure();
        unstableAndZeroTiming();admittedSlowdown();keyOwnershipAndBounds();contention();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+"}");
    }
}
