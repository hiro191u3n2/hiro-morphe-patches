package com.hiro.ulike;
public final class GpuNoise1960 {
    static volatile int calls;
    static volatile boolean race,busy;
    static long retained=16L*1024*1024;
    public static long[] trimIdle19122(long threshold,long generation){
        calls++;
        if(race){race=false;PostSaveCleanup19122.captureStarted19122(999);}
        if(busy||!PostSaveCleanup19122.isCurrentIdle19122(generation))return new long[]{0,retained,retained};
        long before=retained;
        if(retained<=threshold)return new long[]{1,before,before};
        retained=0;return new long[]{2,before,retained};
    }
}
