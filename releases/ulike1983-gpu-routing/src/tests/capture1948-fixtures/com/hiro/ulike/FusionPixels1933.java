package com.hiro.ulike;
import java.util.*;
/** Scripted preprocessing/CPU boundary. Pixel equivalence is tested using the real kernel. */
public final class FusionPixels1933 {
    static final long CAPTURE_WINDOW_NANOS=6000000000L;
    public static int prepareFrameCalls,preparePairCalls,preparationRunning,maxPreparationRunning;
    public static int framePrepsExited,pairPrepsExited,identityChecks;
    public static volatile long blockFrameTimestamp,blockPairCandidateTimestamp;
    public static volatile boolean preparationEntered,preparationInterrupted,releasePreparation;
    public static volatile boolean permitCooperativeInterrupt=true;
    public static final List<PreparedFrame> preparedFrames=Collections.synchronizedList(new ArrayList<PreparedFrame>());
    public static final List<PreparedPair> preparedPairs=Collections.synchronizedList(new ArrayList<PreparedPair>());
    public static final class PreparedFrame {
        public final byte[] bytes;public final int w,h,iso,hash;public final long time,exposure;
        PreparedFrame(byte[] b,int width,int height,int i,long t,long e){bytes=b;w=width;h=height;iso=i;time=t;exposure=e;hash=Arrays.hashCode(b);}
        void immutable(){if(Arrays.hashCode(bytes)!=hash)throw new AssertionError("prepared NV21 modified before renderer");identityChecks++;}
        boolean matches(byte[] b,int width,int height,int i,long t,long e){return bytes==b&&w==width&&h==height&&iso==i&&time==t&&exposure==e;}
    }
    public static final class PreparedPair {
        public final PreparedFrame reference,candidate;
        PreparedPair(PreparedFrame a,PreparedFrame b){reference=a;candidate=b;}
    }
    public static synchronized void resetPreparation(){
        assertIdle("scenario reset");prepareFrameCalls=preparePairCalls=framePrepsExited=pairPrepsExited=identityChecks=0;
        maxPreparationRunning=0;preparedFrames.clear();preparedPairs.clear();
        blockFrameTimestamp=blockPairCandidateTimestamp=0;preparationEntered=preparationInterrupted=releasePreparation=false;
        permitCooperativeInterrupt=true;
    }
    public static synchronized void assertIdle(String why){if(preparationRunning!=0)throw new AssertionError(why+" while preparation still running");}
    private static synchronized void entered(boolean pair){if(pair)preparePairCalls++;else prepareFrameCalls++;preparationRunning++;maxPreparationRunning=Math.max(maxPreparationRunning,preparationRunning);}
    private static synchronized void exited(boolean pair){preparationRunning--;if(pair)pairPrepsExited++;else framePrepsExited++;}
    private static void block(long time,boolean pair){
        if(time!=(pair?blockPairCandidateTimestamp:blockFrameTimestamp))return;
        preparationEntered=true;boolean interrupted=false;
        long deadline=System.nanoTime()+10000000000L;
        while(!releasePreparation){
            if(System.nanoTime()>deadline)throw new AssertionError("preparation release timeout");
            try{Thread.sleep(1);}catch(InterruptedException e){preparationInterrupted=true;interrupted=true;if(permitCooperativeInterrupt)break;}
        }
        if(interrupted)Thread.currentThread().interrupt();
    }
    public static final class Analysis {
        static int retained;boolean pending,closed;
        final List<PreparedFrame> frames=new ArrayList<PreparedFrame>();
        synchronized void remember(){if(!closed&&!pending){pending=true;retained++;}}
        public synchronized void clear(){if(pending){pending=false;retained--;}frames.clear();}
        public synchronized void close(){assertIdle("Analysis.close");closed=true;clear();}
        public void prepareFrame(byte[] bytes,int w,int h,int iso,long timestamp,long exposure){
            if(bytes==null||bytes.length!=w*h*3/2||timestamp<=0||exposure<=0||iso<=0)throw new AssertionError("preparation received incomplete actual frame metadata");
            synchronized(this){if(closed)return;for(PreparedFrame f:frames)if(f.matches(bytes,w,h,iso,timestamp,exposure)){f.immutable();return;}}
            entered(false);try{
                PreparedFrame frame=new PreparedFrame(bytes,w,h,iso,timestamp,exposure);
                block(timestamp,false);frame.immutable();
                synchronized(this){if(!closed){remember();frames.add(frame);preparedFrames.add(frame);}}
            }finally{exited(false);}
        }
        synchronized PreparedFrame lookup(byte[] b,int w,int h,int iso,long time,long exposure){
            for(PreparedFrame frame:frames)if(frame.matches(b,w,h,iso,time,exposure)){frame.immutable();return frame;}
            synchronized(preparedFrames){for(PreparedFrame frame:preparedFrames)if(frame.matches(b,w,h,iso,time,exposure)){frame.immutable();return frame;}}
            throw new AssertionError("pair/fuse lacked exact immutable frame preprocessing identity");
        }
        public void preparePair(byte[] ref,byte[] cand,int w,int h,int refIso,int candIso,long refTime,long candTime,long refExposure,long candExposure){
            entered(true);try{
                PreparedFrame a=lookup(ref,w,h,refIso,refTime,refExposure),b=lookup(cand,w,h,candIso,candTime,candExposure);
                if(a.bytes==b.bytes||a.time>=b.time)throw new AssertionError("pair uses duplicate/non-shutter metadata");
                block(candTime,true);a.immutable();b.immutable();
                synchronized(this){if(!closed){remember();preparedPairs.add(new PreparedPair(a,b));}}
            }finally{exited(true);}
        }
    }
    public static Motion next=new Motion();public static int fused,calls,lastFrames;public static long[] times;
    public static boolean throwFuse;
    public static int chosenReference;public static boolean wrongTimestamp,cancelResult;
    public static volatile boolean blockProbe,probeEntered,probeInterrupted;
    public static final class Motion {public float score,globalShiftPixels,movingFraction,confidence=1,darkness,noiseSigma;public boolean reliable=true,duplicate;}
    public static final class Result {public byte[] nv21;public int referenceIndex,acceptedCount;public long referenceTimestamp;public boolean cancelled;}
    static boolean withinShutterMotion(Motion motion,int w,int h) {
        return motion!=null&&motion.reliable&&!motion.duplicate&&motion.score<=.45f
            &&motion.movingFraction<=.14f&&motion.globalShiftPixels<=Math.max(3f,Math.min(w,h)*.0015f);
    }
    public static Motion probeMotion(byte[] a,byte[] b,int w,int h,int iso,long exposure,Analysis analysis,long t1,long t2,int iso2,long e2){
        assertIdle("probe");analysis.lookup(a,w,h,iso,t1,exposure);analysis.lookup(b,w,h,iso2,t2,e2);
        Motion motion=probeMotion(a,b,w,h,iso,exposure);analysis.remember();return motion;
    }
    public static Result fuse(byte[][] frames,int w,int h,long[] timestamps,long[] e,int[] iso,int level,boolean night,int workers,Analysis analysis){
        assertIdle("fuse");
        for(int i=0;i<frames.length;i++)analysis.lookup(frames[i],w,h,iso[i],timestamps[i],e[i]);
        analysis.clear();return fuse(frames,w,h,timestamps,e,iso,level,night,workers);
    }
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
