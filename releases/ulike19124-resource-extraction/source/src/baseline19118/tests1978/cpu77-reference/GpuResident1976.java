package com.hiro.ulike;

import android.graphics.Bitmap;

/** Optional complete Strong-to-finish route. The immutable source is never
 * modified, and no resident image is published before the final private Bitmap
 * is complete. Two idle whole-image CPU comparisons plus an existing-route
 * timing comparison are required before foreground admission. */
final class GpuResident1976 {
    private GpuResident1976() {}
    private static final ThreadLocal<Boolean> BENCHMARK=new ThreadLocal<Boolean>();
    private static final ThreadLocal<Boolean> CPU=new ThreadLocal<Boolean>();
    private static final ThreadLocal<GpuQualification1961.Cancellation> CANCELLATION=new ThreadLocal<GpuQualification1961.Cancellation>();
    static GpuQualification1961.Cancellation cancellation(){return CANCELLATION.get();}
    static boolean benchmarking(){return Boolean.TRUE.equals(BENCHMARK.get());}
    static boolean cpuOracle(){return Boolean.TRUE.equals(CPU.get());}
    private static String key(Bitmap source,QualityPixels1932.Plan plan,int level,boolean shadows,
            int rotation,int width,int height,QualityPixels1932.Plan output) {
        return "strong-resident1976:3:v1:"+GpuNoise1960.fingerprint()+":"+source.getWidth()+":"+source.getHeight()+":"+
            level+":"+shadows+":"+rotation+":"+width+":"+height+":"+plan.beautyQ8+":"+plan.shadowBudgetQ8+":"+
            output.sharpGainQ8+":"+output.sharpFloorQ8+":"+output.sharpLimit+":"+output.texturePriority+":"+output.haloSuppression+":"+
            Float.floatToRawIntBits(output.sourceSigma)+":"+Float.floatToRawIntBits(output.outputScale)+":"+
            (output.faceRegions!=null)+":"+(output.localNoise!=null);
    }
    static Bitmap finish(final Bitmap source,final QualityPixels1932.Plan plan,final int level,final boolean shadows,
            final int[] half,final int rotation,final int width,final int height,final QualityPixels1932.Plan output,
            ProcessingTiming1947.Trace trace) {
        if(source==null||source.isRecycled()||source.getConfig()!=Bitmap.Config.ARGB_8888||level<=0||output==null||
                !detachedPlan1976(plan)||!detachedPlan1976(output)||
                !GpuChain1961.eligibleResident1976(source,rotation,width,height,output)||
                faceBytes1976(plan.faceRegions)>=Long.MAX_VALUE/4||output.sharpGainQ8<=0||GpuQualification1961.background()||GpuNoise1960.sessionBusy()||
                !GpuNoise1960.supports(GpuNoise1960.FINISH1961)||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                !GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961))return null;
        // Only already detached immutable plans enter qualification. Never
        // silently remove a caller's active cache or smoothing policy.
        final QualityPixels1932.Plan frozenPlan=plan;
        final QualityPixels1932.Plan frozenOutput=output;
        // Defer before snapshot allocation, leaving the foreground finish
        // proof queue available until its actual existing route has qualified.
        final String baselineKey=GpuChain1961.residentBaseline1976(source,rotation,width,height,frozenOutput);
        if(baselineKey==null)return null;
        final String key=key(source,frozenPlan,level,shadows,rotation,width,height,frozenOutput)+":"+baselineKey;
        if(GpuQualification1961.exactRejected(key))return null;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null) {
            try {
                long started=System.nanoTime();Bitmap result=QualityPipeline1932.strongFinish1976(source,frozenPlan,level,shadows,half,rotation,width,height,frozenOutput,true,false);
                if(result==null)return null;
                if(!win(proof.cpuNanos,System.nanoTime()-started))GpuQualification1961.rejectSpeed(key);
                ProcessingTiming1947.detailRoute1976(trace,4);return result;
            } catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
            catch(RuntimeException unavailable){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("resident interrupted");return null;}
            catch(LinkageError unavailable){return null;}
            catch(OutOfMemoryError unavailable){return null;}
        }
        long retained=4L*source.getWidth()*source.getHeight()+(half==null?0:4L*half.length)+
            faceBytes1976(plan.faceRegions)+faceBytes1976(output.faceRegions)+noiseBytes1976(plan.localNoise)+noiseBytes1976(output.localNoise)+65536L;
        if(!GpuQualification1961.canQueue(key,retained)||!GpuNoise1960.workspaceFits(retained))return null;
        Bitmap snapshot=null;
        try {
            snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return null;
            final Bitmap owned=snapshot;final int[] frozenHalf=half==null?null:half.clone();
            GpuQualification1961.schedule(key,retained,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    CANCELLATION.set(cancellation);
                    try {
                    if(!GpuChain1961.opaqueResident1976(owned))return;
                    long baseline=Long.MAX_VALUE,candidateWorst=0;
                    for(int trial=0;trial<2;trial++) {
                        if(cancellation.cancelled()||!baselineKey.equals(GpuChain1961.residentBaseline1976(owned,rotation,width,height,frozenOutput)))return;
                        Bitmap oracle=null,reference=null,candidate=null;
                        try {
                            CPU.set(Boolean.TRUE);
                            try {oracle=QualityPipeline1932.strongFinish1976(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,false,true);}
                            finally {CPU.remove();}
                            if(cancellation.cancelled())return;
                            // The foreground already owns a mutable image.
                            // This independent proof copy is outside its timer.
                            Bitmap ordinaryInput=owned.copy(Bitmap.Config.ARGB_8888,true);
                            if(ordinaryInput==null)return;
                            BENCHMARK.set(Boolean.TRUE);
                            long started=System.nanoTime();
                            try {reference=QualityPipeline1932.strongFinishOwned1976(ordinaryInput,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,false);}
                            finally {BENCHMARK.remove();}
                            long ordinary=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            if(reference==null)return;
                            if(!GpuGeometry1960.equal1961(oracle,reference)){GpuQualification1961.rejectExact(key);return;}
                            reference.recycle();reference=null;
                            BENCHMARK.set(Boolean.TRUE);started=System.nanoTime();
                            try {candidate=QualityPipeline1932.strongFinish1976(owned,frozenPlan,level,shadows,frozenHalf,rotation,width,height,frozenOutput,true,false);}
                            finally {BENCHMARK.remove();}
                            long resident=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            if(candidate==null)return;
                            if(!GpuGeometry1960.equal1961(oracle,candidate)){GpuQualification1961.rejectExact(key);return;}
                            baseline=Math.min(baseline,ordinary);candidateWorst=Math.max(candidateWorst,resident);
                        } finally {
                            CPU.remove();BENCHMARK.remove();
                            if(oracle!=null)oracle.recycle();if(reference!=null)reference.recycle();if(candidate!=null)candidate.recycle();
                        }
                    }
                    if(cancellation.cancelled())return;
                    if(win(baseline,candidateWorst))GpuQualification1961.qualified(key,baseline,candidateWorst,0);
                    else GpuQualification1961.rejectSpeed(key);
                    } finally {CANCELLATION.remove();}
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally {if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
        return null;
    }
    private static boolean detachedPlan1976(QualityPixels1932.Plan plan) {
        if(plan==null||plan.smoothedRegions!=null)return false;
        try {
            java.lang.reflect.Field cache=QualityPixels1932.Plan.class.getDeclaredField("policyCache");
            cache.setAccessible(true);return cache.get(plan)==null;
        } catch(ReflectiveOperationException unavailable){return false;}
          catch(RuntimeException unavailable){return false;}
    }
    private static long noiseBytes1976(SpatialNoise1934 noise) {
        return noise==null?0:512L+4L*noise.columns*noise.rows+12L*(Math.min(32768,noise.width)+Math.min(32768,noise.height));
    }
    private static long faceBytes1976(QualityPixels1932.RegionMask mask) {
        if(mask==null)return 0;
        return mask instanceof FaceRegions1934.Mask?((FaceRegions1934.Mask)mask).retainedBytes1976():Long.MAX_VALUE/4;
    }
    private static boolean win(long baseline,long candidate){return baseline>0&&candidate>0&&candidate<=baseline-baseline/20;}
}
