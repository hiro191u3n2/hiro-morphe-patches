package com.hiro.ulike;
public final class SaveMemory1981 {
    static final int ANALYSIS=1;
    static final class Snapshot{}
    static final class Plan {
        final int width,height,outputWidth,outputHeight;
        final long inputBytes,modelBytes,workerBytes,pixels;
        Plan(int w,int h,int ow,int oh,long input,long model,long worker){width=w;height=h;outputWidth=ow;outputHeight=oh;inputBytes=input;modelBytes=model;workerBytes=worker;pixels=(long)w*h;}
        boolean valid(){return true;}
    }
    static long add(long a,long b){return a+b;}
    static boolean allows(Snapshot memory,Plan plan,int phase,long queued,long proof,long nativeBytes,long analysis){return true;}
}
