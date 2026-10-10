package com.hiro.ulike;

/** Conservative admission by allocation domain. Live system headroom and live
 * Java headroom are independent constraints; neither substitutes for the other.
 * Unknown platform accounting retains the published conservative scalar gate. */
final class SaveMemory1981 {
    static final int COPY=0,NEXT_CAPTURE=1,ANALYSIS=2;
    static final long RESERVE=72L*1024*1024,MAX_PIXELS=32000000L;
    static final Snapshot DENIED=new Snapshot(0,0,true,true);
    static final class Snapshot {
        final long heap,system;
        final boolean knownSystem,lowMemory;
        Snapshot(long heap,long system,boolean knownSystem,boolean lowMemory){this.heap=heap;this.system=system;this.knownSystem=knownSystem;this.lowMemory=lowMemory;}
    }
    static final class Plan {
        final int width,height,outputWidth,outputHeight;
        final long sourcePixels,outputPixels,pixels,inputBytes,modelBytes,workerBytes;
        Plan(int width,int height,int outputWidth,int outputHeight,long inputBytes,long modelBytes,long workerBytes) {
            this.width=width;this.height=height;this.outputWidth=outputWidth;this.outputHeight=outputHeight;
            sourcePixels=(long)width*height;outputPixels=(long)outputWidth*outputHeight;
            pixels=Math.max(sourcePixels,outputPixels);this.inputBytes=inputBytes;this.modelBytes=modelBytes;this.workerBytes=workerBytes;
        }
        boolean valid(){return width>0&&height>0&&outputWidth>0&&outputHeight>0&&sourcePixels>0&&outputPixels>0&&pixels<=MAX_PIXELS&&inputBytes>0&&modelBytes>=0&&workerBytes>=0;}
    }
    private SaveMemory1981(){}
    static Snapshot unknown(long heap){try{return new Snapshot(heap,0,false,false);}catch(OutOfMemoryError unavailable){return DENIED;}}
    static long add(long left,long right){return left<0||right<0||left>Long.MAX_VALUE-right?Long.MAX_VALUE:left+right;}
    static long multiply(long count,long bytes){return count<0||bytes<0||(bytes!=0&&count>Long.MAX_VALUE/bytes)?Long.MAX_VALUE:count*bytes;}
    static long nv21(long pixels){return add(pixels,(pixels+1)/2);}
    static long managedNeed(Plan plan,int phase,long uncertainProof,long analysis) {
        // Two bounded NV21 arrays cover packing and the SDK handoff of ONE
        // capture. There is no four-frame fusion allocation in this release.
        long capture=phase==NEXT_CAPTURE?multiply(nv21(plan.sourcePixels),2):0;
        // modelBytes is StrongNoise1958.modelMemoryBytes at the actual working
        // geometry. Worker bytes include the unchanged halo/core minimum and
        // every configured worker, conservatively charged to the Java domain.
        long work=add(plan.modelBytes,plan.workerBytes);
        return add(RESERVE,add(add(capture,work),add(uncertainProof,analysis)));
    }
    static long systemNeed(Plan plan,int phase,long queuedNative,long uncertainProof,long nativeScratch,long analysis) {
        // A prospective SDK render and its independent handoff may both be
        // RGBA_F16 (8 B/pixel). Never infer the next photo's depth from the last
        // ARGB result. COPY already has its SDK source in observed live memory.
        long capture=phase==NEXT_CAPTURE?multiply(plan.sourcePixels,16):phase==COPY?plan.inputBytes:0;
        // Pristine rollback, a mutable working image and a geometry destination
        // coexist at the heaviest software boundary. queuedNative is future
        // growth beyond each verified live input/final pair; retain two extra
        // ARGB working destinations without deducting resident images twice.
        long working=multiply(plan.pixels,8);
        long managed=managedNeed(plan,phase,uncertainProof,analysis);
        return add(managed,add(add(queuedNative,capture),add(working,add(uncertainProof,nativeScratch))));
    }
    static boolean allows(Snapshot snapshot,Plan plan,int phase,long queuedNative,long proof,long nativeScratch,long analysis) {
        if(snapshot==null||plan==null||!plan.valid()||snapshot.lowMemory||snapshot.heap<0
                ||queuedNative<0||proof<0||nativeScratch<0||analysis<0||(phase!=COPY&&phase!=NEXT_CAPTURE&&phase!=ANALYSIS))return false;
        if(!snapshot.knownSystem)return false;
        long managed=managedNeed(plan,phase,proof,analysis);
        long system=systemNeed(plan,phase,queuedNative,proof,nativeScratch,analysis);
        return managed!=Long.MAX_VALUE&&system!=Long.MAX_VALUE&&snapshot.heap>=managed&&snapshot.system>=system;
    }
}
