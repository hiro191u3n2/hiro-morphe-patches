package com.hiro.ulike;
/** Adapter ABI only. Real pool integration is independently verified by its host suite. */
public final class SpeedWorkers1935 {
    public interface Hints {
        Runnable worker(Runnable loop);
        Object begin(long target);
        void complete(Object token,boolean success);
    }
    public static volatile Hints hints;
    public static void installHints(Hints hooks) { hints=hooks; }
}
