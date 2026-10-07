package com.hiro.ulike;
/** Scripted CPU boundary. The real fusion kernel has a separate pixel test. */
public final class FusionPixels1933 {
    public static Motion next=new Motion();public static int fused,calls,lastFrames;public static long[] times;
    public static boolean throwFuse;
    public static volatile boolean blockProbe,probeEntered,probeInterrupted;
    public static final class Motion {public float score,globalShiftPixels,movingFraction,confidence=1,darkness,noiseSigma;public boolean reliable=true,duplicate;}
    public static final class Result {public byte[] nv21;public int referenceIndex,acceptedCount;public boolean cancelled;}
    public static Motion probeMotion(byte[] a,byte[] b,int w,int h,int iso,long exposure){
        if(a==b)throw new AssertionError("same frame reused");
        if(blockProbe){probeEntered=true;try{Thread.sleep(2000);}catch(InterruptedException e){probeInterrupted=true;Thread.currentThread().interrupt();}}
        return next;
    }
    public static Result fuse(byte[][] frames,int w,int h,long[] timestamps,long[] e,int[] iso,int level,boolean night,int workers){
        calls++;lastFrames=frames.length;times=timestamps;for(int i=1;i<frames.length;i++){if(frames[i]==frames[0]||timestamps[i]<=timestamps[0])throw new AssertionError("duplicate frame");}
        if(throwFuse)throw new IllegalStateException("scripted fusion failure");Result r=new Result();r.nv21=frames[0];r.acceptedCount=frames.length;if(frames.length>1)fused++;return r;
    }
}
