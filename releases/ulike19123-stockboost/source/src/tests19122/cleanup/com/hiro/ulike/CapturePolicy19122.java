package com.hiro.ulike;
/** Deterministic policy input, not a substitute implementation of the worker. */
public final class CapturePolicy19122 {
    static volatile int workers=2;
    static volatile boolean cleanup=true;
    public static int cpuWorkerLimit(){return workers;}
    public static boolean cleanupEnabled(){return cleanup;}
}
