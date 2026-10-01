package com.hiro.ulike.hdr.stillanalysis;

import java.util.concurrent.atomic.AtomicReference;
public final class RecorderAdmissionTest {
    private static int checks;
    private interface Work {void run()throws Exception;}
    private static void rejects(Work work)throws Exception {checks++;try{work.run();throw new AssertionError("Not rejected");}catch(IllegalStateException expected){}}
    private static void check(boolean value){checks++;if(!value)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        Object original=new Object(),originalNative=new Object();
        rejects(RecorderAdmission::beforeConstruction);
        RecorderAdmission.installed(RecorderAdmission.HOOK_CONTRACT);
        rejects(()->RecorderAdmission.installed(RecorderAdmission.HOOK_CONTRACT));
        RecorderAdmission.Ticket construction=RecorderAdmission.beforeConstruction();
        rejects(()->RecorderAdmission.acquire(original,originalNative));
        RecorderAdmission.constructed(construction,original);
        RecorderAdmission.Ticket initialized=RecorderAdmission.beforeNativeInit(originalNative);
        rejects(()->RecorderAdmission.beforeNativeInit(originalNative));
        rejects(()->RecorderAdmission.acquire(original,originalNative));
        RecorderAdmission.nativeInitialized(initialized,42,0);
        rejects(()->RecorderAdmission.beforeNativeInit(originalNative));
        Object second=new Object();RecorderAdmission.Ticket secondTicket=RecorderAdmission.beforeConstruction();RecorderAdmission.constructed(secondTicket,second);
        rejects(()->RecorderAdmission.acquire(original,originalNative));RecorderAdmission.destroyed(RecorderAdmission.beforeDestroy(second));
        RecorderAdmission.Session lease=RecorderAdmission.acquire(original,originalNative);
        rejects(RecorderAdmission::beforeConstruction);rejects(()->RecorderAdmission.beforeDestroy(original));
        rejects(()->RecorderAdmission.beforeApplicationLifecycle(original,"startPreviewAsync"));
        RecorderAdmission.beforeApplicationLifecycle(original,"stopPreviewAsync");checks++;
        rejects(()->RecorderAdmission.analysis(lease));
        RecorderAdmission.Ticket stop=RecorderAdmission.beforeNativeUninit(originalNative);
        rejects(()->RecorderAdmission.beforeNativeUninit(originalNative));rejects(()->RecorderAdmission.beforeNativeInit(originalNative));
        rejects(()->RecorderAdmission.analysis(lease));RecorderAdmission.nativeUninitialized(stop,0);
        RecorderAdmission.analysis(lease);RecorderAdmission.requireIdle(lease);checks++;
        rejects(()->RecorderAdmission.beforeApplicationLifecycle(original,"stopPreviewAsync"));
        AtomicReference<Throwable> failure=new AtomicReference<>();Thread other=new Thread(()->{try{RecorderAdmission.beforeNativeInit(new Object());}catch(Throwable e){failure.set(e);}});other.start();other.join();check(failure.get() instanceof IllegalStateException);
        Object own=new Object();RecorderAdmission.Ticket begin=RecorderAdmission.beforeNativeInit(own);
        rejects(()->RecorderAdmission.requireIdle(lease));RecorderAdmission.nativeInitialized(begin,84,0);RecorderAdmission.requireIdle(lease);checks++;
        rejects(()->RecorderAdmission.beforeNativeInit(new Object()));rejects(()->RecorderAdmission.restore(lease));
        RecorderAdmission.nativeUninitialized(RecorderAdmission.beforeNativeUninit(own),0);RecorderAdmission.restore(lease);
        rejects(()->RecorderAdmission.beforeNativeInit(own));rejects(()->RecorderAdmission.beforeApplicationLifecycle(second,"startPreviewAsync"));
        RecorderAdmission.beforeApplicationLifecycle(original,"startPreviewAsync");checks++;
        RecorderAdmission.nativeInitialized(RecorderAdmission.beforeNativeInit(originalNative),100,0);RecorderAdmission.complete(lease);checks++;
        // A later failed uninit must never advertise a clean idle lease.
        RecorderAdmission.Session failed=RecorderAdmission.acquire(original,originalNative);
        RecorderAdmission.Ticket bad=RecorderAdmission.beforeNativeUninit(originalNative);rejects(()->RecorderAdmission.nativeUninitialized(bad,-1));
        rejects(()->RecorderAdmission.analysis(failed));rejects(()->RecorderAdmission.acquire(original,originalNative));
        System.out.println("PASS recorder admission "+checks+" host checks; actual hook coverage and phone lifecycle=false");
    }
}
