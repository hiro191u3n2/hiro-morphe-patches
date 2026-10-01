package com.hiro.ulike.integration169;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Failure ordering, exact cancellation and commit linearization, without Android stubs. */
public final class ProcessingSequence169Test {
    static int checks;
    static void ok(boolean condition){checks++;if(!condition)throw new AssertionError("check "+checks);}
    static final String[] STAGES={"validate","analyse","bind","render","publish"};
    static class Fixture implements ProcessingSequence169.Work {
        final List<String> calls=new ArrayList<>();int fail=-1;boolean closeFails,error;
        void step(int i)throws Exception{calls.add(STAGES[i]);if(fail==i){if(error)throw new LinkageError(STAGES[i]);throw new IOException(STAGES[i]);}}
        public void validate()throws Exception{step(0);}public void analyse()throws Exception{step(1);}
        public void bind()throws Exception{step(2);}public void render()throws Exception{step(3);}
        public String publish()throws Exception{step(4);return "content://owned/committed/1";}
        public void close()throws Exception{calls.add("close");ok(!Thread.currentThread().isInterrupted());if(closeFails)throw new IOException("close");}
    }
    public static void main(String[] args)throws Exception {
        ProcessingSequence169 sequence=new ProcessingSequence169();Object identity=new Object();
        Fixture success=new Fixture();ProcessingSequence169.Result result=sequence.execute(identity,success);
        ok(result.committedPhoto.equals("content://owned/committed/1") && result.cleanupFailure==null);
        ok(success.calls.equals(Arrays.asList("validate","analyse","bind","render","publish","close")));
        for(int error=0;error<2;error++)for(int stage=0;stage<5;stage++)for(int cleanup=0;cleanup<2;cleanup++){
            Fixture failure=new Fixture();failure.fail=stage;failure.closeFails=cleanup==1;failure.error=error==1;
            try{sequence.execute(identity,failure);throw new AssertionError("accepted failure");}
            catch(Exception|LinkageError e){ok(e.getMessage().equals(STAGES[stage]));ok(e.getSuppressed().length==cleanup);}
            ok(failure.calls.size()==stage+2);ok(failure.calls.get(failure.calls.size()-1).equals("close"));
            // Each failure releases admission; a subsequent independent shot completes.
            ok(sequence.execute(new Object(),new Fixture()).committedPhoto!=null);
        }
        Fixture closeAfterCommit=new Fixture();closeAfterCommit.closeFails=true;
        result=sequence.execute(identity,closeAfterCommit);ok(result.committedPhoto!=null && result.cleanupFailure.getMessage().equals("close"));
        Fixture noUri=new Fixture(){@Override public String publish()throws Exception{super.publish();return null;}};
        try{sequence.execute(identity,noUri);throw new AssertionError("accepted missing URI");}catch(IllegalStateException e){ok(noUri.calls.contains("close"));}
        Thread.currentThread().interrupt();Fixture preCancelled=new Fixture();
        try{sequence.execute(identity,preCancelled);throw new AssertionError("accepted preinterrupt");}
        catch(InterruptedIOException e){ok(preCancelled.calls.equals(Arrays.asList("close")));ok(Thread.interrupted());}
        for(int cancelStage=0;cancelStage<4;cancelStage++){
            final int point=cancelStage;Fixture cancel=new Fixture(){@Override void step(int i)throws Exception{super.step(i);if(i==point)ok(sequence.cancel(identity));}};
            try{sequence.execute(identity,cancel);throw new AssertionError("accepted cancellation");}
            catch(InterruptedIOException e){ok(cancel.calls.size()==point+2);ok(!cancel.calls.contains("publish"));ok(Thread.interrupted());}
        }
        Fixture commitRace=new Fixture(){@Override public String publish()throws Exception{String uri=super.publish();ok(sequence.cancel(identity));return uri;}};
        result=sequence.execute(identity,commitRace);ok(result.committedPhoto!=null);ok(Thread.interrupted());
        Fixture closing=new Fixture(){@Override public void close()throws Exception{ok(!sequence.cancel(identity));super.close();}};
        sequence.execute(identity,closing);ok(!sequence.cancel(identity));ok(!sequence.cancel(null));
        for(int firstError=0;firstError<2;firstError++){
            final Throwable failure=firstError==0?new IOException("first"):new LinkageError("first");List<Integer> closed=new ArrayList<>();
            AutoCloseable first=()->{closed.add(1);if(failure instanceof Exception)throw (Exception)failure;throw (Error)failure;};
            AutoCloseable repeated=()->{closed.add(2);if(failure instanceof Exception)throw (Exception)failure;throw (Error)failure;};
            AutoCloseable second=()->{closed.add(3);throw new IOException("second");};
            try{ProcessingSequence169.closeOwned(first,null,repeated,second,()->closed.add(4));throw new AssertionError("accepted cleanupfailure");}
            catch(Exception|Error e){ok(e==failure);ok(e.getSuppressed().length==1);ok(e.getSuppressed()[0].getMessage().equals("second"));ok(closed.equals(Arrays.asList(1,2,3,4)));}
        }
        final IOException shared=new IOException("same-primary-and-cleanup");
        Fixture repeatedFailure=new Fixture(){@Override public void validate()throws Exception{throw shared;}@Override public void close()throws Exception{throw shared;}};
        try{sequence.execute(identity,repeatedFailure);throw new AssertionError("accepted repeated failure");}catch(IOException e){ok(e==shared);ok(e.getSuppressed().length==0);}
        for(int repeat=0;repeat<20;repeat++)concurrent();
        System.out.println("processing-sequence-checks="+checks);
    }
    static void concurrent()throws Exception {
        ProcessingSequence169 sequence=new ProcessingSequence169();Object first=new Object(),other=new Object();
        CountDownLatch entered=new CountDownLatch(1),blocked=new CountDownLatch(1);AtomicReference<Throwable> outcome=new AtomicReference<>();
        Fixture work=new Fixture(){@Override public void analyse()throws Exception{super.analyse();entered.countDown();blocked.await();}};
        Thread worker=new Thread(()->{try{sequence.execute(first,work);outcome.set(new AssertionError("uncancelled"));}catch(InterruptedException e){outcome.set(e);}catch(Throwable e){outcome.set(e);}});
        worker.start();ok(entered.await(5,TimeUnit.SECONDS));Fixture rejected=new Fixture();
        try{sequence.execute(other,rejected);throw new AssertionError("accepted overlapping shot");}catch(IllegalStateException expected){ok(rejected.calls.isEmpty());}
        ok(!sequence.cancel(other));ok(sequence.cancel(first));worker.join(5000);ok(!worker.isAlive());
        ok(outcome.get() instanceof InterruptedException);ok(work.calls.equals(Arrays.asList("validate","analyse","close")));
        ok(sequence.execute(other,new Fixture()).committedPhoto!=null);ok(!sequence.cancel(first));
    }
}
