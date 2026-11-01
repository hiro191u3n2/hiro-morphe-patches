package com.hiro.ulike;

/** Host-only competing physical-memory owner; the GPU ledger and native
 * allocations themselves are the unmodified production implementations. */
final class WholeRoute1953 {
    static volatile long retained;
    static volatile boolean fail,oom;
    static long retainedBytes(){
        if(fail)throw new IllegalStateException("injected other-owner budget failure");
        if(oom)throw new OutOfMemoryError("injected other-owner budget failure");
        return retained;
    }
}
final class GpuFinish1953 {static long retainedBytes(){return 0;}}
