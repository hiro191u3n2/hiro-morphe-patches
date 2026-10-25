package com.hiro.ulike;

/** Pixel-independent scheduling only. No user filter strength or image arithmetic. */
public final class Scheduling1944 {
    private Scheduling1944() {}
    private static final int[] CANDIDATES = {1, 2, 3, 4};
    private static final long MIN_PIXELS = 262144L;
    private static final long MIN_NANOS = 2000000L;
    private static final Profile[] PROFILES = {new Profile(), new Profile(), new Profile(), new Profile()};

    /** Larger strips amortize repeated halo reads without reducing halo reach.
     * Keep at least two strips per chosen worker so uneven texture can rebalance.
     * Bound each input/output pair to 12 MiB, and never exhaust processing reserve. */
    public static int coreRows(int width,int height,int halo,int workers,long budget) {
        if(width<=0 || height<=0 || halo<0 || workers<1)return 128;
        int core=128;
        for(int candidate=256;candidate<=512;candidate*=2) {
            long rows=Math.min((long)height,(long)candidate+2L*halo);
            long pair=(long)width*rows*8L;
            if(height<(long)candidate*workers*2L || pair>12L*1024*1024 ||
                    pair*workers+(long)width*halo*4L>budget)break;
            core=candidate;
        }
        return core;
    }

    /** Tune only independent final-image passes. Initial behavior keeps the existing
     * <=4 cap. Explore 1/2/4 sparsely after warm-up; a lower count wins only after
     * three substantial uncontended measurements and >=12% normalized improvement.
     * Low-memory/short passes and nested calls never train the profile. */
    public static int workers(int stage,long pixels,int tiles,int maximum) {
        int cap=Math.max(1,Math.min(SpeedWorkers1935.maxWorkers(),Math.min(tiles,maximum)));
        if(stage<0 || stage>=PROFILES.length || pixels<MIN_PIXELS || cap<2)return cap;
        Profile p=PROFILES[stage];
        synchronized(p) {
            if(cap<SpeedWorkers1935.maxWorkers())return cap;
            int chosen=Math.min(cap,p.preferred);
            // A probe runs a real pass once; no image is processed a second time.
            if(p.observations>=4 && (++p.turns & 15)==0) {
                for(int i=0;i<CANDIDATES.length;i++)
                    if(CANDIDATES[i]<=cap && (CANDIDATES[i]!=3 || cap==3) && p.samples[i]<3)return CANDIDATES[i];
            }
            return Math.max(1,chosen);
        }
    }
    public static void measured(int stage,int workers,long pixels,long elapsed,boolean uncontended) {
        if(stage<0 || stage>=PROFILES.length || pixels<MIN_PIXELS || elapsed<MIN_NANOS || !uncontended)return;
        int index=index(workers);if(index<0)return;
        Profile p=PROFILES[stage];
        synchronized(p) {
            // The first pass can include runtime compilation/cold caches.
            if(p.observations++==0)return;
            double value=(double)elapsed/pixels;
            p.cost[index]=p.samples[index]==0?value:p.cost[index]*.75+value*.25;
            p.samples[index]++;
            int old=index(p.preferred),best=old;
            if(old<0 || p.samples[old]<3)return;
            for(int i=0;i<CANDIDATES.length;i++)
                if(CANDIDATES[i]<=SpeedWorkers1935.maxWorkers() &&
                        (CANDIDATES[i]!=3 || SpeedWorkers1935.maxWorkers()==3) && p.samples[i]>=3 &&
                        p.cost[i]<p.cost[best]*.88)best=i;
            p.preferred=CANDIDATES[best];
        }
    }
    private static int index(int workers) {for(int i=0;i<CANDIDATES.length;i++)if(CANDIDATES[i]==workers)return i;return -1;}
    private static final class Profile {
        final int[] samples=new int[CANDIDATES.length];final double[] cost=new double[CANDIDATES.length];
        int preferred=SpeedWorkers1935.maxWorkers(),observations,turns;
    }

    /** Optional recovery polls, only. Required camera/renderer callbacks still run.
     * Delaying one owned retry coalesces superseded busy polls while preserving the
     * original ticket deadline and all foreground/epoch/surface checks. */
    public static long optionalRetryDelay(boolean busy,long normal,long now,long deadline) {
        if(!busy || deadline<=now)return normal;
        return Math.min(Math.max(normal,600L),Math.max(1L,deadline-now));
    }
}
