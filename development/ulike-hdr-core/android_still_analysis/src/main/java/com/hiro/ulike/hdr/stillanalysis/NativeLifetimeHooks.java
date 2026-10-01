package com.hiro.ulike.hdr.stillanalysis;

/** Partial diagnostic patch target. Compile-time disabled; there is deliberately
 * no setter, preference, reflection-controlled switch or installed() call.
 * Full recorder/backend coverage and restoration are not implemented here.
 */
public final class NativeLifetimeHooks {
    private static final boolean ENABLED=false;
    private NativeLifetimeHooks() {}
    public static void beforeInit(Object invoker){if(ENABLED)NativeLifetimeBoundary.beforeInit(invoker);}
    public static void beforeUninit(Object invoker){if(ENABLED)NativeLifetimeBoundary.beforeUninit(invoker);}
    public static void returned(int status){if(ENABLED)NativeLifetimeBoundary.returned(status);}
    public static void failed(Object invoker){
        if(ENABLED)try{NativeLifetimeBoundary.failed(invoker);}
        catch(Throwable cleanupFailure){
            // This is the original method's exceptional path. Preserve its
            // original throwable even if bookkeeping itself fails.
            try{RecorderAdmission.failed(null);}catch(Throwable ignored){}
        }
    }
}
