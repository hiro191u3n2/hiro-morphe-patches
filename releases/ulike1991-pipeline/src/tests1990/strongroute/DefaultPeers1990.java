package com.hiro.ulike;

/** Explicit mock startup evidence; proves dispatch/ownership only. */
final class GpuStrongDefault1990 {
    static final int PROFILE=4,VARIANT=0;
    static boolean ready,owned;
    static boolean ready(){return ready;}
    static boolean owns(int[] u){return owned;}
    static boolean accepts(int[] u){return owned&&ready&&!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,PROFILE));}
}
final class GpuStartup1990 {static int waits;static boolean awaitCoreReady(long milliseconds){waits++;return GpuStrongDefault1990.ready;}}
