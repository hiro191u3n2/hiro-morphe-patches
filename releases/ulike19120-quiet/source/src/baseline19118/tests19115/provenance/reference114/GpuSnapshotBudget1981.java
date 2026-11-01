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
    private static Copy owner1985;
    private static void refresh(){
        long now=ProcessingTiming1947.captureEpoch1953();if(now==epoch)return;
        // A selected family may be unable to queue because its independent
        // memory/retry gate remains closed. Spend the turn, not the copy, so
        // that such a family cannot hold the other stages behind it forever.
        preferred=next(seen,preferred==0?lastWhole:preferred,1);
        stripPreferred=next(stripSeen,stripPreferred==0?stripLast:stripPreferred,8);
        // A tentative token from an obsolete capture cannot begin an allocation.
        // Revoke that scalar owner without waiting for its paused caller. A
        // started copy still excludes every new copy until its own close().
        if(owner1985!=null&&owner1985.revokeUnstarted1985()){
            owner1985=null;copying=false;
        }
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
        if(admissionReason1985(bytes,family)!=Decision1985.READY)return null;
        Copy result=new Copy(epoch,bytes,family);owner1985=result;copying=true;return result;
    }
    /** The same admission plus its immutable scalar explanation. Legacy strip
     * callers keep the allocation-free refusal path through tryCopy(). */
    static synchronized Decision1985 tryCopy1985(long bytes,int family){
        int reason=admissionReason1985(bytes,family);
        Copy copy=reason==Decision1985.READY?new Copy(epoch,bytes,family):null;
        int selected=family==0||family>=STRONG_TUNING?stripPreferred:preferred;
        Decision1985 result=new Decision1985(reason,selected,owner1985==null?-1:owner1985.family1985(),
            owner1985!=null&&owner1985.started1985(),epoch,copy);
        // Optional explanation allocation must succeed before ownership changes.
        if(copy!=null){owner1985=copy;copying=true;}
        return result;
    }
    private static int admissionReason1985(long bytes,int family){
        if(bytes<=0)return Decision1985.INVALID_REQUEST;
        if(bytes>MAX_COPY)return Decision1985.BYTE_LIMIT;
        if(Thread.currentThread().isInterrupted())return Decision1985.INTERRUPTED;
        refresh();
        boolean strip=family==0||family==STRONG_TUNING||family==SINGLE_STAGE||family==RESIDUAL_STAGE;
        if(strip){
            if(family!=0){stripSeen|=family;if(stripPreferred!=0&&stripPreferred!=family)return Decision1985.STRIP_TURN;}
            if(stripBytes!=0&&(bytes>STRIP_COPY||stripBytes>STRIP_COPY-bytes))return Decision1985.STRIP_BUDGET;
        }else{
            if(family!=RESIDENT&&family!=FINISH&&family!=MOIRE_GEOMETRY)return Decision1985.INVALID_REQUEST;
            seen|=family;
            if(wholeUsed)return Decision1985.WHOLE_USED;
            if(preferred!=0&&preferred!=family)return Decision1985.WHOLE_TURN;
        }
        if(copying)return owner1985!=null&&owner1985.started1985()?Decision1985.COPY_ACTIVE:Decision1985.COPY_RESERVED;
        return Decision1985.READY;
    }
    // Called under this class's monitor. Package-local bridges preserve the
    // existing private-field synthetic accessors for retained DEX closures.
    static boolean owns1985(Copy copy){return owner1985==copy;}
    static boolean releaseOwner1985(Copy copy){
        if(owner1985!=copy)return false;owner1985=null;return true;
    }
    static final class Decision1985 {
        static final int READY=0,INVALID_REQUEST=1,BYTE_LIMIT=2,INTERRUPTED=3,WHOLE_USED=4,
            WHOLE_TURN=5,STRIP_BUDGET=6,STRIP_TURN=7,COPY_ACTIVE=8,COPY_RESERVED=9;
        final int reason,preferredFamily,activeFamily;
        final boolean activeStarted;final long copyEpoch;final Copy copy;
        Decision1985(int reason,int preferred,int active,boolean started,long epoch,Copy copy){
            this.reason=reason;preferredFamily=preferred;activeFamily=active;activeStarted=started;copyEpoch=epoch;this.copy=copy;
        }
    }
    static final class Copy implements AutoCloseable {
        private final long ownerEpoch,bytes;private final int family;
        private boolean started,closed;
        Copy(long epoch,long bytes,int family){ownerEpoch=epoch;this.bytes=bytes;this.family=family;}
        /** Call after queue/workspace checks and immediately before allocating
         * or copying. A failed/OOM attempt still consumes this capture's budget
         * so many worker strips cannot repeatedly allocate the same large copy. */
        boolean begin(){synchronized(GpuSnapshotBudget1981.class){
            if(closed||started||!owns1985(this)||ownerEpoch!=ProcessingTiming1947.captureEpoch1953()||Thread.currentThread().isInterrupted())return false;
            started=true;
            if(family==0||family>=STRONG_TUNING){stripBytes+=bytes;if(family!=0)stripLast=family;}
            else{wholeUsed=true;lastWhole=family;}
            return true;
        }}
        boolean current(){synchronized(GpuSnapshotBudget1981.class){
            return !closed&&owns1985(this)&&ownerEpoch==ProcessingTiming1947.captureEpoch1953()&&!Thread.currentThread().isInterrupted();
        }}
        boolean started1985(){return started;}
        int family1985(){return family;}
        boolean revokeUnstarted1985(){if(started)return false;closed=true;return true;}
        public void close(){synchronized(GpuSnapshotBudget1981.class){
            if(closed)return;closed=true;
            if(releaseOwner1985(this))copying=false;
        }}
    }
}
