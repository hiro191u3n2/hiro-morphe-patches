package com.hiro.ulike;

/** Scalar-only admission before a foreground proof copy. The queue's separate
 * 96 MiB retained-owner limit remains authoritative. Per capture, strip copies
 * spend at most 32 MiB (or one larger complete strip), and at most one complete
 * image is copied. Large images are not permanently excluded by a smaller cap.
 * Whole-image and NR strip families rotate across fresh captures; no old
 * Bitmap or source array is retained by this scalar budget. */
final class GpuSnapshotBudget1981 {
    private GpuSnapshotBudget1981() {}
    static final int RESIDENT=1,FINISH=2,MOIRE_GEOMETRY=4;
    static final int STRONG_TUNING=8,SINGLE_STAGE=16,RESIDUAL_STAGE=32;
    private static final long STRIP_COPY=32L*1024*1024,MAX_COPY=96L*1024*1024;
    private static long epoch=Long.MIN_VALUE,stripBytes;
    private static int seen,preferred,lastWhole,stripSeen,stripPreferred,stripLast;
    private static boolean wholeUsed,copying;
    private static void refresh(){
        long now=ProcessingTiming1947.captureEpoch1953();if(now==epoch)return;
        // A selected family may be unable to queue because its independent
        // memory/retry gate remains closed. Spend the turn, not the copy, so
        // that such a family cannot hold the other stages behind it forever.
        preferred=next(seen,preferred==0?lastWhole:preferred,1);
        stripPreferred=next(stripSeen,stripPreferred==0?stripLast:stripPreferred,8);
        seen=stripSeen=0;stripBytes=0;wholeUsed=false;epoch=now;
    }
    private static int next(int mask,int after,int first){
        int start=after==first?1:after==first*2?2:0;
        for(int n=0;n<3;n++){int value=first<<((start+n)%3);if((mask&value)!=0)return value;}
        return 0;
    }
    static synchronized void offerWhole(int family){
        if(family!=RESIDENT&&family!=FINISH&&family!=MOIRE_GEOMETRY)throw new IllegalArgumentException("snapshot family");
        refresh();seen|=family;
    }
    static synchronized void offerStrip1981(int family){
        if(family!=STRONG_TUNING&&family!=SINGLE_STAGE&&family!=RESIDUAL_STAGE)throw new IllegalArgumentException("strip snapshot family");
        refresh();stripSeen|=family;
    }
    static synchronized Copy tryStripCopy1981(long bytes,int family){
        if(family!=STRONG_TUNING&&family!=SINGLE_STAGE&&family!=RESIDUAL_STAGE)return null;
        return tryCopy(bytes,family);
    }
    static synchronized Copy tryCopy(long bytes,int family){
        if(bytes<=0||bytes>MAX_COPY||Thread.currentThread().isInterrupted())return null;
        refresh();
        boolean strip=family==0||family==STRONG_TUNING||family==SINGLE_STAGE||family==RESIDUAL_STAGE;
        if(strip){
            if(family!=0){stripSeen|=family;if(stripPreferred!=0&&stripPreferred!=family)return null;}
            if(stripBytes!=0&&(bytes>STRIP_COPY||stripBytes>STRIP_COPY-bytes))return null;
        }else{
            if(family!=RESIDENT&&family!=FINISH&&family!=MOIRE_GEOMETRY)return null;
            seen|=family;
            if(wholeUsed||preferred!=0&&preferred!=family)return null;
        }
        if(copying)return null;
        Copy result=new Copy(epoch,bytes,family);copying=true;return result;
    }
    static final class Copy implements AutoCloseable {
        private final long ownerEpoch,bytes;private final int family;
        private boolean started,closed;
        Copy(long epoch,long bytes,int family){ownerEpoch=epoch;this.bytes=bytes;this.family=family;}
        /** Call after queue/workspace checks and immediately before allocating
         * or copying. A failed/OOM attempt still consumes this capture's budget
         * so many worker strips cannot repeatedly allocate the same large copy. */
        boolean begin(){synchronized(GpuSnapshotBudget1981.class){
            if(closed||started||ownerEpoch!=ProcessingTiming1947.captureEpoch1953()||Thread.currentThread().isInterrupted())return false;
            started=true;
            if(family==0||family>=STRONG_TUNING){stripBytes+=bytes;if(family!=0)stripLast=family;}
            else{wholeUsed=true;lastWhole=family;}
            return true;
        }}
        boolean current(){return !closed&&ownerEpoch==ProcessingTiming1947.captureEpoch1953()&&!Thread.currentThread().isInterrupted();}
        public void close(){synchronized(GpuSnapshotBudget1981.class){
            if(closed)return;closed=true;copying=false;
        }}
    }
}
