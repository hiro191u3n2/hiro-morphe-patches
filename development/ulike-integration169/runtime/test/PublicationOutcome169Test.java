package com.hiro.ulike.integration169;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

/** Publication uncertainty must not become success, unsaved failure, or automatic retry. */
public final class PublicationOutcome169Test {
    private static int checks;
    private static void ok(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    private static final String URI="content://media/external/images/media/123";
    private static final String HASH="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static PublicationOutcome169.UnconfirmedPhoto photo(){return new PublicationOutcome169.UnconfirmedPhoto("transaction-123",URI,100,HASH);}
    private interface Action{void run();}
    private static void rejects(Action action){try{action.run();throw new AssertionError("accepted invalid transition");}catch(IllegalArgumentException|IllegalStateException|NullPointerException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        PublicationOutcome169 committed=new PublicationOutcome169();
        PublicationOutcome169.Snapshot initial=committed.snapshot();
        ok(initial.kind==PublicationOutcome169.Kind.PROCESSING && initial.committedUri==null && initial.unconfirmed==null);
        PublicationOutcome169.SaveAttempt committedAttempt=committed.beginSave();committed.committed(committedAttempt,URI);ok(committed.snapshot().kind==PublicationOutcome169.Kind.COMMITTED);
        ok(URI.equals(committed.snapshot().committedUri) && committed.snapshot().unconfirmed==null);
        ok(!committed.failBeforePublication());rejects(()->committed.uncertain(committedAttempt,photo()));rejects(()->committed.committed(committedAttempt,URI));
        ok(initial.kind==PublicationOutcome169.Kind.PROCESSING);
        PublicationOutcome169 uncertain=new PublicationOutcome169();PublicationOutcome169.UnconfirmedPhoto recovery=photo();
        PublicationOutcome169.SaveAttempt uncertainAttempt=uncertain.beginSave();uncertain.uncertain(uncertainAttempt,recovery);ok(uncertain.snapshot().kind==PublicationOutcome169.Kind.PUBLICATION_UNCERTAIN);
        ok(uncertain.snapshot().unconfirmed==recovery && uncertain.snapshot().committedUri==null);
        ok(!uncertain.failBeforePublication());rejects(()->uncertain.committed(uncertainAttempt,URI));rejects(()->uncertain.uncertain(uncertainAttempt,photo()));
        PublicationOutcome169 failed=new PublicationOutcome169();ok(failed.failBeforePublication());ok(!failed.failBeforePublication());
        rejects(()->failed.committed(committedAttempt,URI));rejects(()->failed.uncertain(committedAttempt,photo()));
        for(String invalid:new String[]{null,"","a\nb","a\u007fb"}){
            rejects(()->new PublicationOutcome169.UnconfirmedPhoto(invalid,URI,1,HASH));
            rejects(()->new PublicationOutcome169.UnconfirmedPhoto("id",invalid,1,HASH));
            rejects(()->{PublicationOutcome169 state=new PublicationOutcome169();state.committed(state.beginSave(),invalid);});
        }
        rejects(()->new PublicationOutcome169.UnconfirmedPhoto("id",URI,0,HASH));
        rejects(()->new PublicationOutcome169.UnconfirmedPhoto("id",URI,1,HASH.toUpperCase()));
        rejects(()->{PublicationOutcome169 state=new PublicationOutcome169();state.uncertain(state.beginSave(),null);});
        for(int i=0;i<100;i++){
            PublicationOutcome169 shared=new PublicationOutcome169();PublicationOutcome169.SaveAttempt claim=shared.beginSave();CountDownLatch begin=new CountDownLatch(1);
            AtomicInteger terminalWinners=new AtomicInteger();AtomicInteger unexpected=new AtomicInteger();
            Runnable[] actions={()->shared.committed(claim,URI),()->shared.uncertain(claim,photo()),()->shared.failedSave(claim)};
            Thread[] workers=new Thread[3];
            for(int j=0;j<3;j++){final Runnable action=actions[j];workers[j]=new Thread(()->{try{begin.await();action.run();terminalWinners.incrementAndGet();}catch(IllegalStateException expected){}catch(Exception error){unexpected.incrementAndGet();}});workers[j].start();}
            begin.countDown();for(Thread t:workers)t.join(3000);
            for(Thread t:workers)ok(!t.isAlive());
            ok(terminalWinners.get()==1 && unexpected.get()==0);
            PublicationOutcome169.Snapshot finalValue=shared.snapshot();ok(finalValue.kind!=PublicationOutcome169.Kind.PROCESSING);
            ok((finalValue.kind==PublicationOutcome169.Kind.COMMITTED)==(finalValue.committedUri!=null));
            ok((finalValue.kind==PublicationOutcome169.Kind.PUBLICATION_UNCERTAIN)==(finalValue.unconfirmed!=null));
        }
        for(int uncertainResult=0;uncertainResult<2;uncertainResult++){
            PublicationOutcome169 blocked=new PublicationOutcome169();CountDownLatch insideSave=new CountDownLatch(1),releaseSave=new CountDownLatch(1);
            AtomicReference<Throwable> failure=new AtomicReference<>();final boolean isUncertain=uncertainResult==1;
            Thread saver=new Thread(()->{
                try{
                    PublicationOutcome169.SaveAttempt claim=blocked.beginSave();insideSave.countDown();
                    if(!releaseSave.await(3,TimeUnit.SECONDS))throw new AssertionError("release timeout");
                    if(isUncertain)blocked.uncertain(claim,photo());else blocked.committed(claim,URI);
                }catch(Throwable error){failure.set(error);}
            });
            saver.start();ok(insideSave.await(3,TimeUnit.SECONDS));
            ok(blocked.snapshot().kind==PublicationOutcome169.Kind.PUBLISHING);
            for(int n=0;n<100;n++)ok(!blocked.failBeforePublication());
            rejects(()->blocked.beginSave());rejects(()->blocked.failedSave(committedAttempt));
            rejects(()->blocked.committed(committedAttempt,URI));rejects(()->blocked.uncertain(committedAttempt,photo()));
            releaseSave.countDown();saver.join(3000);ok(!saver.isAlive() && failure.get()==null);
            ok(blocked.snapshot().kind==(isUncertain?PublicationOutcome169.Kind.PUBLICATION_UNCERTAIN:PublicationOutcome169.Kind.COMMITTED));
        }
        System.out.println("publication-outcome-checks="+checks);
    }
}
