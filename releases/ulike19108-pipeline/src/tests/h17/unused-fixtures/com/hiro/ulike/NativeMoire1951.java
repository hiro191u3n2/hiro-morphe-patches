package com.hiro.ulike;
/** Unused-policy shortcut must never execute the expensive float/mask policy. */
public final class NativeMoire1951 {
    public static int calls;
    static void preparePolicy(QualityPixels1932.Plan plan,int x,int y,int[] policy,int at) {
        calls++;throw new AssertionError("unused sharpening evaluated per-pixel policy");
    }
}
