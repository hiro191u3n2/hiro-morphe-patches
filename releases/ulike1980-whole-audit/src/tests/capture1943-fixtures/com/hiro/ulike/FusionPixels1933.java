package com.hiro.ulike;
/** Scripted CPU boundary. The real fusion kernel has a separate pixel test. */
public final class FusionPixels1933 {
    static final long CAPTURE_WINDOW_NANOS=6000000000L;
    public static final class Analysis {
        static int retained;boolean pending,closed;
        synchronized void remember(){if(!closed&&!pending){pending=true;retained++;}}
        public synchronized void clear(){if(pending){pending=false;retained--;}}
        public synchronized void close(){closed=true;clear();}
        // Compile-only API compatibility for the retained historical camera suite.
        // The real lifecycle and pixels are exercised in the independent1948 suites.
        public void prepareFrame(byte[] data,int w,int h,int iso,long time,long exposure){}
        public void preparePair(byte[] r,byte[] c,int w,int h,int ri,int ci,long rt,long ct,long re,long ce){}
    }
    public static Motion next=new Motion();public static int fused,calls,lastFrames;public static long[] times;
    public static boolean throwFuse;
    public static int chosenReference;public static boolean wrongTimestamp,cancelResult;
    public static volatile boolean blockProbe,probeEntered,probeInterrupted;
    public static final class Motion {public float score,globalShiftPixels,movingFraction,confidence=1,darkness,noiseSigma;public boolean reliable=true,duplicate;}
    public static final class Result {public byte[] nv21;public int referenceIndex,acceptedCount;public long referenceTimestamp;public boolean cancelled;}
    // Scripted decision boundary; the production rule is exercised separately
    // by host_shutter_moment1943.py using the actual pixel fusion implementation.
    static boolean withinShutterMotion(Motion motion,int w,int h) {
        return motion!=null&&motion.reliable&&!motion.duplicate&&motion.score<=.45f
            &&motion.movingFraction<=.14f&&motion.globalShiftPixels<=Math.max(3f,Math.min(w,h)*.0015f);
    }
    public static Motion probeMotion(byte[] a,byte[] b,int w,int h,int iso,long exposure,Analysis analysis,long t1,long t2,int iso2,long e2){Motion motion=probeMotion(a,b,w,h,iso,exposure);analysis.remember();return motion;}
    public static Result fuse(byte[][] frames,int w,int h,long[] timestamps,long[] e,int[] iso,int level,boolean night,int workers,Analysis analysis){analysis.clear();return fuse(frames,w,h,timestamps,e,iso,level,night,workers);}
    public static Motion probeMotion(byte[] a,byte[] b,int w,int h,int iso,long exposure){
        if(a==b)throw new AssertionError("same frame reused");
        if(blockProbe){probeEntered=true;try{Thread.sleep(2000);}catch(InterruptedException e){probeInterrupted=true;Thread.currentThread().interrupt();}}
        return next;
    }
    public static Result fuse(byte[][] frames,int w,int h,long[] timestamps,long[] e,int[] iso,int level,boolean night,int workers){
        calls++;lastFrames=frames.length;times=timestamps;for(int i=1;i<frames.length;i++){if(frames[i]==frames[0]||timestamps[i]<=timestamps[0])throw new AssertionError("duplicate frame");}
        if(throwFuse)throw new IllegalStateException("scripted fusion failure");Result r=new Result();
        r.referenceIndex=chosenReference;r.nv21=frames[chosenReference];r.referenceTimestamp=timestamps[chosenReference]+(wrongTimestamp?1:0);r.cancelled=cancelResult;
        r.acceptedCount=frames.length;if(frames.length>1)fused++;return r;
    }
}
