package com.hiro.ulike;
/** Test-only policy values; production policy freezing is covered separately. */
public final class CapturePolicy19122 {
    static volatile int banks=1;
    static volatile long gap=2000000L;
    public static int gpuBanks(){return banks;}
    public static long gpuSubmissionGapNanos(){return gap;}
}
