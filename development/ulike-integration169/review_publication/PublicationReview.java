package com.hiro.ulike.integration169;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Independent Java latch tests, not an Android provider execution. */
public final class PublicationReview {
    static int checks;
    static final String URI="content://media/external_primary/images/media/901";
    static final PublicationOutcome169.UnconfirmedPhoto PHOTO=new PublicationOutcome169.UnconfirmedPhoto("capture-901",URI,4096,
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Action{void run();}
    static void bad(Action action){try{action.run();throw new AssertionError("stale/foreign claim accepted");}catch(IllegalStateException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        for(int result=0;result<3;result++){
            final int resolution=result;PublicationOutcome169 outcome=new PublicationOutcome169();
            CountDownLatch providerEntered=new CountDownLatch(1),providerReturn=new CountDownLatch(1);
            AtomicReference<Throwable> error=new AtomicReference<>();AtomicReference<PublicationOutcome169.SaveAttempt> acquired=new AtomicReference<>();
            Thread saver=new Thread(()->{
                try{
                    PublicationOutcome169.SaveAttempt attempt=outcome.beginSave();acquired.set(attempt);providerEntered.countDown();
                    if(!providerReturn.await(3,TimeUnit.SECONDS))throw new AssertionError("provider gate timeout");
                    if(resolution==0)outcome.committed(attempt,URI);else if(resolution==1)outcome.uncertain(attempt,PHOTO);else outcome.failedSave(attempt);
                }catch(Throwable failure){error.set(failure);}
            });
            saver.start();check(providerEntered.await(3,TimeUnit.SECONDS),"save entered");
            PublicationOutcome169.Snapshot inFlight=outcome.snapshot();
            check(inFlight.kind==PublicationOutcome169.Kind.PUBLISHING&&inFlight.committedUri==null&&inFlight.unconfirmed==null,"in-flight has no fabricated outcome");
            for(int i=0;i<128;i++)check(!outcome.failBeforePublication(),"external camera/UI failure cannot preempt in-flight publication");
            PublicationOutcome169 foreign=new PublicationOutcome169();PublicationOutcome169.SaveAttempt alien=foreign.beginSave();
            bad(()->outcome.committed(alien,URI));bad(()->outcome.uncertain(alien,PHOTO));bad(()->outcome.failedSave(alien));bad(outcome::beginSave);
            providerReturn.countDown();saver.join(3000);check(!saver.isAlive()&&error.get()==null,"authority resolves actual result");
            PublicationOutcome169.Kind expected=result==0?PublicationOutcome169.Kind.COMMITTED:result==1?PublicationOutcome169.Kind.PUBLICATION_UNCERTAIN:PublicationOutcome169.Kind.FAILED_BEFORE_PUBLICATION;
            PublicationOutcome169.Snapshot finished=outcome.snapshot();check(finished.kind==expected,"terminal outcome correct");
            check((finished.committedUri!=null)==(result==0),"only confirmed result has saved URI");check((finished.unconfirmed!=null)==(result==1),"only uncertain result has recovery identity");
            check(inFlight.kind==PublicationOutcome169.Kind.PUBLISHING,"prior snapshot immutable");
            check(!outcome.failBeforePublication(),"late failure cannot rewrite terminal outcome");
            bad(()->outcome.committed(acquired.get(),URI));bad(()->outcome.uncertain(acquired.get(),PHOTO));bad(()->outcome.failedSave(acquired.get()));
        }
        // Admission vs cancellation races: either failure prevents admission or the admitted save owns completion.
        for(int i=0;i<100;i++){
            PublicationOutcome169 outcome=new PublicationOutcome169();CountDownLatch start=new CountDownLatch(1);
            AtomicReference<PublicationOutcome169.SaveAttempt> claim=new AtomicReference<>();AtomicInteger failed=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();
            Thread save=new Thread(()->{try{start.await();claim.set(outcome.beginSave());}catch(IllegalStateException expected){}catch(Throwable t){error.set(t);}});
            Thread failure=new Thread(()->{try{start.await();if(outcome.failBeforePublication())failed.incrementAndGet();}catch(Throwable t){error.set(t);}});
            save.start();failure.start();start.countDown();save.join(3000);failure.join(3000);
            check(!save.isAlive()&&!failure.isAlive()&&error.get()==null,"race completed");
            check((claim.get()!=null?1:0)+failed.get()==1,"one admission winner");
            if(claim.get()!=null){check(outcome.snapshot().kind==PublicationOutcome169.Kind.PUBLISHING,"admission wins before failure");outcome.committed(claim.get(),URI);}
            else bad(outcome::beginSave);
            check(outcome.snapshot().kind==(claim.get()!=null?PublicationOutcome169.Kind.COMMITTED:PublicationOutcome169.Kind.FAILED_BEFORE_PUBLICATION),"race terminal remains truthful");
        }
        System.out.println("{\"checks\":"+checks+",\"blocked_completion_outcomes\":3,\"external_failures_during_save\":384,\"admission_races\":100,\"android_provider_execution\":false}");
    }
}
