package review;

import com.hiro.ulike.integration169.ProcessingSequence169;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Independent host review; no native binding, Android UI or photograph publication. */
public final class SequenceReview {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static class Work implements ProcessingSequence169.Work {
        final ArrayList<String> calls=new ArrayList<>();String fail="";Throwable closeFailure;String uri="content://synthetic/committed";
        public void validate()throws Exception{stage("validate");}public void analyse()throws Exception{stage("analyse");}
        public void bind()throws Exception{stage("bind");}public void render()throws Exception{stage("render");}
        public String publish()throws Exception{stage("publish");return uri;}
        void stage(String stage)throws IOException{calls.add(stage);if(stage.equals(fail))throw new IOException(stage);}
        public void close()throws Exception{calls.add("close");if(closeFailure instanceof Error)throw (Error)closeFailure;if(closeFailure!=null)throw (Exception)closeFailure;}
    }
    public static void main(String[] args)throws Exception{
        ProcessingSequence169 sequence=new ProcessingSequence169();Object identity=new Object();
        Work success=new Work();ProcessingSequence169.Result result=sequence.execute(identity,success);
        check(result.committedPhoto.equals(success.uri)&&result.cleanupFailure==null,"successful owned URI");
        check(success.calls.equals(Arrays.asList("validate","analyse","bind","render","publish","close")),"exact stage order");
        check(!sequence.cancel(identity),"no cancellation of completed identity");
        for(String stage:Arrays.asList("validate","analyse","bind","render","publish")){
            Work failure=new Work();failure.fail=stage;failure.closeFailure=new IOException("cleanup");boolean original=false;
            try{sequence.execute(new Object(),failure);}catch(IOException e){original=e.getMessage().equals(stage)&&e.getSuppressed().length==1;}
            check(original,"original failure and cleanup retained at "+stage);check(Collections.frequency(failure.calls,"close")==1,"exactly one cleanup at "+stage);
            check(failure.calls.indexOf(stage)==failure.calls.size()-2,"no subsequent stage after "+stage);
            check(sequence.execute(new Object(),new Work()).committedPhoto!=null,"owner reusable after "+stage);
        }
        Work cleanup=new Work();cleanup.closeFailure=new IOException("post-commit cleanup");result=sequence.execute(new Object(),cleanup);
        check(result.committedPhoto.equals(cleanup.uri)&&result.cleanupFailure==cleanup.closeFailure,"post-commit cleanup cannot erase photo URI");
        for(String uri:Arrays.asList(null,"")){
            Work missing=new Work();missing.uri=uri;boolean rejected=false;try{sequence.execute(new Object(),missing);}catch(IllegalStateException e){rejected=true;}
            check(rejected&&missing.calls.get(missing.calls.size()-1).equals("close"),"missing committed URI fails and cleans");
        }
        Work error=new Work(){public void bind(){calls.add("bind");throw new AssertionError("primary error");}};error.closeFailure=new AssertionError("cleanup error");
        boolean preserved=false;try{sequence.execute(new Object(),error);}catch(AssertionError e){preserved=e.getMessage().equals("primary error")&&e.getSuppressed().length==1;}
        check(preserved,"primary Error preserved over cleanup Error");check(sequence.execute(new Object(),new Work()).committedPhoto!=null,"owner released after Error");

        CountDownLatch analysing=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Throwable> thrown=new AtomicReference<>();AtomicBoolean cleanupSawInterrupt=new AtomicBoolean(true),finalInterrupt=new AtomicBoolean(false);
        Work cancellable=new Work(){public void analyse()throws Exception{calls.add("analyse");analysing.countDown();release.await();}
            public void close(){calls.add("close");cleanupSawInterrupt.set(Thread.currentThread().isInterrupted());}};
        Object active=new Object();Thread worker=new Thread(()->{try{sequence.execute(active,cancellable);}catch(Throwable e){thrown.set(e);}finally{finalInterrupt.set(Thread.currentThread().isInterrupted());}});
        worker.start();check(analysing.await(5,TimeUnit.SECONDS),"worker reached analysis");
        check(!sequence.cancel(new Object())&&!sequence.cancel(null),"foreign identity cannot cancel active capture");
        Work rejected=new Work();boolean busy=false;try{sequence.execute(new Object(),rejected);}catch(IllegalStateException e){busy=true;}
        check(busy&&rejected.calls.isEmpty(),"second capture not accepted or prematurely closed");
        check(sequence.cancel(active),"exact identity cancellation accepted");worker.join(5000);check(!worker.isAlive(),"cancelled worker terminates");
        check(thrown.get() instanceof InterruptedException,"interrupt propagated from native wait stand-in");
        check(cancellable.calls.equals(Arrays.asList("validate","analyse","close")),"cancelled capture never binds/renders/publishes");
        check(!cleanupSawInterrupt.get(),"cleanup runs with interrupt cleared");
        // InterruptedException consumed the interrupt; restoration applies only to a still-set flag.
        check(!finalInterrupt.get(),"no spurious interrupt recreated after consumed InterruptedException");

        AtomicBoolean acceptedAfterCommit=new AtomicBoolean();Object commitRace=new Object();
        Work publishedThenCancelled=new Work(){public String publish(){calls.add("publish");acceptedAfterCommit.set(sequence.cancel(commitRace));return uri;}
            public void close(){calls.add("close");check(!Thread.currentThread().isInterrupted(),"post-commit cleanup clears interrupt");}};
        try{result=sequence.execute(commitRace,publishedThenCancelled);check(result.committedPhoto.equals(publishedThenCancelled.uri),"successful commit wins late cancellation");check(acceptedAfterCommit.get()&&Thread.currentThread().isInterrupted(),"late interrupt preserved after cleanup");}finally{Thread.interrupted();}
        AtomicBoolean cancelledDuringClose=new AtomicBoolean(true);Object closeIdentity=new Object();
        Work closeRace=new Work(){public void close(){calls.add("close");cancelledDuringClose.set(sequence.cancel(closeIdentity));}};
        sequence.execute(closeIdentity,closeRace);check(!cancelledDuringClose.get(),"cleanup cannot be interrupted by owned cancel");
        System.out.println("{\"checks\":"+checks+",\"android_execution\":false,\"real_photo_publication\":false}");
    }
}
