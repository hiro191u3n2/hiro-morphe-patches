package com.hiro.ulike.composer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class NativeInitCallbackTest {
    private static int checks;
    private static final String STYLE="7306041792770609665";
    private static final class Recorder {volatile long handle;}
    private interface Work {void run()throws Exception;}
    private static void check(boolean condition){checks++;if(!condition)throw new AssertionError("check "+checks);}
    private static void rejects(Work work)throws Exception{try{work.run();throw new AssertionError("unexpected receipt");}catch(IllegalStateException expected){checks++;}}
    private static NativeComposerBoundary boundary(){return new NativeComposerBoundary(r->((Recorder)r).handle);}
    private static void enter(NativeComposerBoundary b,Recorder r){b.beforeInit(r,4080,3060,"/work",1,0,"/model",0,false,false,false);}
    private static void finish(NativeComposerBoundary b,Recorder r){r.handle=77;b.returned(0);}
    public static void main(String[] args)throws Exception {
        for(boolean early:new boolean[]{false,true})for(int status:new int[]{0,1,Integer.MAX_VALUE}) {
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();enter(b,r);r.handle=77;
            rejects(()->b.initializationReceipt(r));
            if(early)b.initCallback(r,status);b.returned(0);if(!early)b.initCallback(r,status);
            NativeComposerBoundary.InitializationReceipt receipt=b.initializationReceipt(r);
            check(receipt.recorderIdentity==r && receipt.nativeHandler==77 && receipt.callbackStatus==status);
            check(!receipt.nativeQueueBarrierProven && !receipt.listenerCompletionProven && !receipt.restorationProven);
            check(receipt.initConfigurationSha256.equals(b.snapshot(r,new Object(),1,STYLE).initConfigurationSha256));receipt.requireCurrent();checks++;
            b.initCallback(r,status);rejects(receipt::requireCurrent);rejects(()->b.snapshot(r,new Object(),2,STYLE));
        }
        for(boolean early:new boolean[]{false,true})for(int status:new int[]{-1,-105,Integer.MIN_VALUE}) {
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();enter(b,r);r.handle=77;
            if(early)b.initCallback(r,status);b.returned(0);if(!early)b.initCallback(r,status);
            rejects(()->b.initializationReceipt(r));rejects(()->b.snapshot(r,new Object(),1,STYLE));
        }
        NativeComposerBoundary unknown=boundary();Recorder un=new Recorder();un.handle=77;unknown.initCallback(un,0);
        rejects(()->unknown.initializationReceipt(un));rejects(()->unknown.snapshot(un,new Object(),1,STYLE));
        NativeComposerBoundary missing=boundary();Recorder mr=new Recorder();enter(missing,mr);finish(missing,mr);
        rejects(()->missing.initializationReceipt(mr));check(missing.snapshot(mr,new Object(),1,STYLE)!=null); // request-only transcript remains distinct
        for(boolean zero:new boolean[]{false,true}) {
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();enter(b,r);finish(b,r);r.handle=zero?0:88;b.initCallback(r,0);
            rejects(()->b.initializationReceipt(r));
        }
        NativeComposerBoundary earlyMismatch=boundary();Recorder em=new Recorder();enter(earlyMismatch,em);em.handle=66;earlyMismatch.initCallback(em,0);finish(earlyMismatch,em);
        rejects(()->earlyMismatch.initializationReceipt(em));
        NativeComposerBoundary changed=boundary();Recorder cr=new Recorder();enter(changed,cr);finish(changed,cr);changed.initCallback(cr,0);
        NativeComposerBoundary.InitializationReceipt current=changed.initializationReceipt(cr);cr.handle=91;rejects(current::requireCurrent);cr.handle=77;rejects(current::requireCurrent);
        NativeComposerBoundary torn=boundary();Recorder tr=new Recorder();enter(torn,tr);finish(torn,tr);torn.initCallback(tr,0);
        NativeComposerBoundary.InitializationReceipt original=torn.initializationReceipt(tr);torn.beforeUninit(tr);rejects(original::requireCurrent);tr.handle=0;torn.returned(0);
        enter(torn,tr);finish(torn,tr);torn.initCallback(tr,0);rejects(()->torn.initializationReceipt(tr));
        // Unsupported and malformed init attempts also retain tombstones; otherwise an old callback could cross generations.
        for(boolean unsupported:new boolean[]{true,false}) {
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();
            if(unsupported)b.beforeUnsupportedInit(r);else b.beforeInit(r,1,1,null,0,0,"/model",0,false,false,false);
            r.handle=77;b.returned(0);b.beforeUninit(r);r.handle=0;b.returned(0);enter(b,r);finish(b,r);b.initCallback(r,0);
            rejects(()->b.initializationReceipt(r));
        }
        NativeComposerBoundary threw=boundary();Recorder er=new Recorder();enter(threw,er);er.handle=77;threw.failed(er);threw.initCallback(er,0);rejects(()->threw.initializationReceipt(er));
        for(int n=0;n<100;n++) {
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();enter(b,r);r.handle=77;
            CountDownLatch go=new CountDownLatch(1);Throwable[] error={null};
            Thread callback=new Thread(()->{try{if(!go.await(2,TimeUnit.SECONDS))throw new AssertionError("timeout");b.initCallback(r,0);}catch(Throwable failure){error[0]=failure;}});
            callback.start();go.countDown();b.returned(0);callback.join(2000);
            check(!callback.isAlive() && error[0]==null);b.initializationReceipt(r).requireCurrent();checks++;
        }
        NativeComposerBoundary bounded=boundary();
        for(int i=0;i<32;i++){Recorder r=new Recorder();enter(bounded,r);finish(bounded,r);bounded.beforeUninit(r);r.handle=0;bounded.returned(0);}
        rejects(()->enter(bounded,new Recorder()));NativeComposerHooks.initCallback(null,-105);check(true);
        System.out.println("PASS "+checks+" init callback correlation checks; no native queue/restoration receipt");
    }
}
