package com.hiro.ulike.hdr.stillanalysis;

public final class NativeLifetimeBoundaryTest {
    private static int checks;
    public static final class Invoker {public long handle;public long getHandler(){return handle;}}
    private interface Action{void run();}
    private static void rejects(Action a){checks++;try{a.run();throw new AssertionError("not rejected");}catch(IllegalStateException expected){}}
    public static void main(String[] args){
        Object recorder=new Object();Invoker original=new Invoker();
        // Injected helper must preserve old behavior without even installing the ledger.
        NativeLifetimeHooks.beforeInit(null);NativeLifetimeHooks.returned(-1);NativeLifetimeHooks.failed(null);NativeLifetimeHooks.beforeUninit(null);checks++;
        rejects(()->NativeLifetimeBoundary.beforeInit(original));
        RecorderAdmission.installed(RecorderAdmission.HOOK_CONTRACT);
        RecorderAdmission.constructed(RecorderAdmission.beforeConstruction(),recorder);
        NativeLifetimeBoundary.beforeInit(original);
        rejects(()->NativeLifetimeBoundary.beforeInit(original));
        original.handle=7;NativeLifetimeBoundary.returned(0);checks++;
        rejects(()->NativeLifetimeBoundary.beforeInit(original));
        RecorderAdmission.Session s=RecorderAdmission.acquire(recorder,original);
        NativeLifetimeBoundary.beforeUninit(original);original.handle=0;
        // SDK clears Java handle before nativeUninitBeautyPlay returns.
        rejects(()->RecorderAdmission.analysis(s));
        rejects(()->NativeLifetimeBoundary.beforeUninit(original));
        NativeLifetimeBoundary.returned(0);RecorderAdmission.analysis(s);checks++;
        Invoker own=new Invoker();NativeLifetimeBoundary.beforeInit(own);own.handle=9;NativeLifetimeBoundary.returned(0);
        rejects(()->RecorderAdmission.restore(s));
        NativeLifetimeBoundary.beforeUninit(own);own.handle=0;NativeLifetimeBoundary.returned(0);RecorderAdmission.restore(s);
        NativeLifetimeBoundary.beforeInit(original);original.handle=11;NativeLifetimeBoundary.returned(0);RecorderAdmission.complete(s);checks++;
        rejects(()->NativeLifetimeBoundary.returned(0));
        NativeLifetimeBoundary.beforeUninit(original);
        NativeLifetimeBoundary.failed(new Object()); // unrelated callback cannot clear this call
        rejects(()->RecorderAdmission.acquire(recorder,original));
        NativeLifetimeBoundary.failed(original);
        rejects(()->RecorderAdmission.acquire(recorder,original));
        // Repeated exception cleanup is harmless, and the original throw is preserved by the DEX hook.
        NativeLifetimeBoundary.failed(original);checks++;
        System.out.println("PASS native lifetime boundary "+checks+" checks; hook installation=false");
    }
}
