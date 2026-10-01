package com.hiro.ulike.composer;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Independent protocol/canonical-identity tests. All receipts below are
 * deliberately synthetic and confer no production replay authorization. */
public final class ComposerReplayIndependentTest {
    static int checks;
    static final String INIT=new String(new char[64]).replace('\0','e');
    static final String STYLE="7307549491547083266";
    interface Action{void run()throws Exception;}
    static void check(boolean b){checks++;if(!b)throw new AssertionError();}
    static void rejects(Action a)throws Exception{checks++;try{a.run();throw new AssertionError("accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
    static ComposerJournal journal(){ComposerJournal j=new ComposerJournal(new Object());j.beforeNativeInit(INIT);j.nativeInitResult(0,101);return j;}
    static void append(ComposerJournal j,ComposerCommand c){j.result(j.begin(c),0);}
    static ComposerJournal.Snapshot shot(ComposerJournal j){return j.snapshot(new Object(),11,STYLE);}
    static class Target implements ReplayPlan.ReplayTarget {
        final Object id=new Object();int calls,aborts;ComposerJournal mutate;Error shared;
        public Object identity(){return id;}
        public void requireFresh(String fingerprint){check(INIT.equals(fingerprint));}
        public int apply(ComposerCommand c){calls++;if(shared!=null)throw shared;return 0;}
        public ReplayPlan.BarrierReceipt awaitSetupBarrier(ReplayPlan.BarrierRequest r)throws Exception {
            if(mutate!=null){Thread t=new Thread(()->append(mutate,ComposerCommand.mode(77,88)));t.start();t.join(1000);check(!t.isAlive());}
            return new ReplayPlan.BarrierReceipt(id,r.shotIdentity,r.shotEpoch,r.nonce,r.settingsSha256,r.mappedCommandsSha256);
        }
        public void abort(){aborts++;if(shared!=null)throw shared;}
    }
    static void unicode()throws Exception {
        for(int value=0xd800;value<=0xdfff;value++) {
            final String lone="/model/"+(char)value,key="k"+(char)value;
            rejects(()->ComposerCommand.resource(lone));
            rejects(()->ComposerCommand.update("/model",key,1));
        }
        for(int value=0x10000;value<=0x10ffff;value+=997) {
            String s="/model/"+new String(Character.toChars(value));
            check(ComposerCommand.resource(s).paths()[0].equals(s));
        }
        ComposerJournal a=journal(),b=journal();
        append(a,ComposerCommand.resource("/model/\ud800\udc00"));append(b,ComposerCommand.resource("/model/\ud800\udc01"));
        check(!shot(a).sha256.equals(shot(b).sha256));
    }
    static void threads()throws Exception {
        ComposerJournal j=journal();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        AtomicReference<Throwable> error=new AtomicReference<>();
        Thread writer=new Thread(()->{try{ComposerJournal.Ticket ticket=j.begin(ComposerCommand.resource("/r"));entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new AssertionError();j.result(ticket,0);}catch(Throwable t){error.set(t);}});
        writer.start();check(entered.await(1,TimeUnit.SECONDS));rejects(()->shot(j));release.countDown();writer.join(2000);check(!writer.isAlive()&&error.get()==null);check(shot(j).commands.size()==1);
        ComposerJournal.Snapshot s=shot(j);Target t=new Target();t.mutate=j;
        rejects(()->ReplayPlan.authorize(s,x->x.requireCurrent()).execute(t,(path,inline)->path));
        check(t.calls==1&&t.aborts==1);
    }
    static void cleanup()throws Exception {
        ComposerJournal j=journal();append(j,ComposerCommand.mode(1,0));Target t=new Target();Error primary=new AssertionError("same original failure");t.shared=primary;
        try{ReplayPlan.authorize(shot(j),x->x.requireCurrent()).execute(t,(path,inline)->path);throw new AssertionError("success");}
        catch(Error observed){check(observed==primary);check(t.aborts==1);}
        rejects(()->ReplayPlan.authorize(shot(j),ReplayPlan.UNAVAILABLE));
    }
    static void ownership()throws Exception {
        ComposerJournal j=journal();String[] paths={"/r"},tags={"tag"};
        append(j,ComposerCommand.nodes(ComposerCommand.Kind.APPEND_TAG,paths,null,tags));
        ComposerJournal.Snapshot s=shot(j);byte[] before=s.canonicalBytes();
        paths[0]="/evil";tags[0]="other";s.commands.get(0).paths()[0]="/changed";byte[] attempt=s.canonicalBytes();Arrays.fill(attempt,(byte)0);
        check(Arrays.equals(before,s.canonicalBytes()));check(s.commands.get(0).paths()[0].equals("/r"));
    }
    public static void main(String[] args)throws Exception{unicode();threads();cleanup();ownership();System.out.println("PASS "+checks+" independent composer identity/concurrency/cleanup checks; native replay=false");}
}
