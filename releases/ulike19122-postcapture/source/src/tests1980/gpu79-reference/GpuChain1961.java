package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.os.Build;
import java.util.LinkedHashMap;

/** GX20: source-domain moire correction feeds exact geometry on the same GPU
 * session. The intermediate ARGB image is never downloaded or re-uploaded.
 * Subsequent output-noise measurement and output-only sharpening keep their
 * established order. Full private CPU/GPU proof gates this optional route. */
final class GpuChain1961 {
    private GpuChain1961() {}
    private static final LinkedHashMap<String,Boolean> REJECTED=new LinkedHashMap<String,Boolean>();
    static Bitmap moireGeometry(final Bitmap source,final int rotation,final int width,final int height) {
        if(!eligible(source,rotation,width,height)||GpuNoise1960.sessionBusy()||
                GpuQualification1961.background()||!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||
                !GpuNoise1960.supports(GpuNoise1960.GEOMETRY))return null;
        final String key=GpuNoise1960.fingerprint()+"|moire-geometry|"+source.getWidth()+","+source.getHeight()+","+rotation+","+width+","+height;
        synchronized(REJECTED){if(REJECTED.containsKey(key))return null;}
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null) {
            try {
                long start=System.nanoTime();Bitmap result=run(source,rotation,width,height);long elapsed=System.nanoTime()-start;
                if(result==null){reject(key);return null;}
                if(elapsed>proof.cpuNanos-proof.cpuNanos/20)GpuQualification1961.rejectSpeed(key);
                return result;
            } catch(DeferredChain unavailable){return null;}
              catch(RuntimeException unavailable){reject(key);return null;}
              catch(LinkageError unavailable){reject(key);return null;}
              catch(OutOfMemoryError unavailable){return null;}
        }
        if(!GpuQualification1961.maySchedule(key)||GpuQualification1961.retainedBytes()!=0)return null;
        long retained=4L*source.getWidth()*source.getHeight();
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained*3+8L*width*height))return null;
        Bitmap snapshot=null;
        try {
            snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return null;
            final Bitmap owned=snapshot;
            GpuQualification1961.schedule(key,retained,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    long cpu=Long.MAX_VALUE,gpu=0;
                    for(int trial=0;trial<2;trial++) {
                        if(cancellation.cancelled())return;
                        Bitmap expected=null,candidate=null;
                        try {
                            long started=System.nanoTime();expected=cpu(owned,rotation,width,height);long cpuTime=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            started=System.nanoTime();candidate=GpuChain1961.run(owned,rotation,width,height);long gpuTime=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            if(candidate==null||!GpuGeometry1960.equal1961(expected,candidate)){reject(key);return;}
                            if(cpuTime<=0||gpuTime<=0||gpuTime>cpuTime-cpuTime/20){GpuQualification1961.rejectSpeed(key);return;}
                            cpu=Math.min(cpu,cpuTime);gpu=Math.max(gpu,gpuTime);
                        } finally {if(expected!=null&&expected!=owned)expected.recycle();if(candidate!=null&&candidate!=owned)candidate.recycle();}
                    }
                    if(!cancellation.cancelled()&&gpu<=cpu-cpu/20)GpuQualification1961.qualified(key,cpu,gpu,0);
                    else if(!cancellation.cancelled())GpuQualification1961.rejectSpeed(key);
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });
            snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally{if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
        return null;
    }
    /** GX24 whole compatible post-denoise slice: source moire, geometry,
     * optional output evidence, exact CPU mask/policy, output-only sharp. Full
     * ARGB intermediates remain resident until the final sharp tile readback.
     * The SDK/denoise owner and CPU double masks are not replaced by this route. */
    static Bitmap finish(final Bitmap source,final int rotation,final int width,final int height,
            final QualityPixels1932.Plan outputPlan,final boolean refreshNoise) {
        return finish(source,rotation,width,height,outputPlan,refreshNoise,null);
    }
    static Bitmap finish(final Bitmap source,final int rotation,final int width,final int height,
            final QualityPixels1932.Plan outputPlan,final boolean refreshNoise,
            final ProcessingTiming1947.Trace trace) {
        if(!eligibleFinish(source,rotation,width,height,outputPlan)||GpuNoise1960.sessionBusy()||
                GpuQualification1961.background()||!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||
                !GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                refreshNoise&&!GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961))return null;
        final String key=finishKey1976(source,rotation,width,height,outputPlan,refreshNoise);
        if(GpuQualification1961.exactRejected(key))return legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2) {
            try {
                long started=System.nanoTime();Bitmap result=runFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace,32<<proof.variant);long elapsed=System.nanoTime()-started;
                if(result==null)return null;
                if(!win(proof.cpuNanos,elapsed))GpuQualification1961.rejectSpeed(key);
                ProcessingTiming1947.detailRoute1976(trace,1+proof.variant);return result;
            } catch(DeferredChain unavailable){return null;}
              catch(java.util.concurrent.CancellationException interrupted){throw interrupted;}
              catch(RuntimeException failure){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("finish interrupted");return null;}
              catch(LinkageError failure){return null;}
              catch(OutOfMemoryError unavailable){return null;}
        }
        if(!GpuQualification1961.maySchedule(key)||GpuQualification1961.retainedBytes()!=0)return legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
        long retained=4L*source.getWidth()*source.getHeight()+
            (outputPlan.faceRegions==null?0:((FaceRegions1934.Mask)outputPlan.faceRegions).retainedBytes1976())+
            (outputPlan.smoothedRegions==null?0:((long)width+3)/4*((height+3L)/4))+
            (outputPlan.localNoise==null?0:512L+4L*outputPlan.localNoise.columns*outputPlan.localNoise.rows+
                12L*(Math.min(32768,outputPlan.localNoise.width)+Math.min(32768,outputPlan.localNoise.height)));
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained))return legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
        Bitmap snapshot=null;
        try {
            snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null||!opaqueSnapshot1962(snapshot))return null;
            final Bitmap owned=snapshot;
            GpuQualification1961.schedule(key,retained,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    qualifyFinish1978(owned,rotation,width,height,outputPlan,refreshNoise,cancellation);
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally {if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
        return legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
    }
    /** One idle source can first establish the ordinary finish dependency and
     * then prove Strong-to-finish. All complete comparisons precede admission.
     * Comparison runs do not allocate a full GPU destination Bitmap. Timings
     * use separate real output-producing runs after the CPU reference is freed,
     * so the smaller proof workspace cannot manufacture a speed advantage. */
    static void qualifyFinish1978(Bitmap owned,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,
            GpuQualification1961.Cancellation cancellation) {
        String key=finishKey1976(owned,rotation,width,height,outputPlan,refreshNoise);
        if(GpuQualification1961.exactRejected(key)||cancellation.cancelled())return;
        long cpu=Long.MAX_VALUE,legacyFastest=Long.MAX_VALUE,legacyWorst=0;
        boolean legacyExact=true;long[] gpu=new long[3];boolean[] exact={true,true,true};
        for(int trial=0;trial<2;trial++) {
            if(cancellation.cancelled())return;
            Bitmap expected=null,oracleInput=null;
            try {
                oracleInput=owned.copy(Bitmap.Config.ARGB_8888,true);
                if(oracleInput==null)return;
                long started=System.nanoTime();expected=cpuFinishOwned1962(oracleInput,rotation,width,height,outputPlan,refreshNoise);oracleInput=null;
                cpu=Math.min(cpu,System.nanoTime()-started);
                if(expected==null||cancellation.cancelled())return;
                int result=compareFinish1978(owned,rotation,width,height,outputPlan,refreshNoise,expected,32,false,null,false,cancellation);
                if(result!=ResidentProof1978.EXACT) {
                    legacyExact=false;
                    if(result==ResidentProof1978.MISMATCH)GpuQualification1961.rejectExact(legacyKey1976(key));
                }
                for(int variant=0;variant<3;variant++) {
                    if(cancellation.cancelled())return;if(!exact[variant])continue;
                    result=compareFinish1978(owned,rotation,width,height,outputPlan,refreshNoise,expected,32<<variant,true,null,false,cancellation);
                    if(result!=ResidentProof1978.EXACT)exact[variant]=false;
                }
            } finally {if(oracleInput!=null&&!oracleInput.isRecycled())oracleInput.recycle();if(expected!=null&&expected!=owned)expected.recycle();}
            if(cancellation.cancelled())return;
            if(legacyExact) {
                Bitmap candidate=null;
                try {
                    long started=System.nanoTime();candidate=runLegacy1976(owned,rotation,width,height,outputPlan,refreshNoise,null);
                    long elapsed=System.nanoTime()-started;
                    if(candidate==null)legacyExact=false;
                    else {legacyFastest=Math.min(legacyFastest,elapsed);legacyWorst=Math.max(legacyWorst,elapsed);}
                } catch(DeferredChain unavailable){legacyExact=false;}
                finally {if(candidate!=null&&candidate!=owned)candidate.recycle();}
            }
            for(int variant=0;variant<3;variant++) {
                if(cancellation.cancelled())return;if(!exact[variant])continue;
                Bitmap candidate=null;
                try {
                    long started=System.nanoTime();candidate=runFinish1976(owned,rotation,width,height,outputPlan,refreshNoise,null,32<<variant);
                    long elapsed=System.nanoTime()-started;
                    if(candidate==null)exact[variant]=false;else gpu[variant]=Math.max(gpu[variant],elapsed);
                } catch(DeferredChain unavailable){exact[variant]=false;}
                finally {if(candidate!=null&&candidate!=owned)candidate.recycle();}
            }
        }
        if(cancellation.cancelled())return;
        int best=chooseFinish1976(cpu,legacyFastest,gpu,exact,legacyExact);
        if(best>=0)GpuQualification1961.qualified(key,Math.min(cpu,legacyFastest),gpu[best],best);
        else {
            if(legacyExact&&win(cpu,legacyWorst))GpuQualification1961.qualified(legacyKey1976(key),cpu,legacyWorst,0);
            else if(legacyExact)GpuQualification1961.rejectSpeed(legacyKey1976(key));
            GpuQualification1961.rejectSpeed(key);
        }
    }
    private static String legacyKey1976(String key){return key.replace("|finish-chain1976:v2|","|finish-chain1962:v1|");}
    private static Bitmap legacyFinish1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        String key=legacyKey1976(finishKey1976(source,rotation,width,height,plan,refresh));
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof==null||proof.variant!=0||GpuQualification1961.exactRejected(key))return null;
        try {
            long started=System.nanoTime();Bitmap result=runLegacy1976(source,rotation,width,height,plan,refresh,trace);
            if(result!=null){if(!win(proof.cpuNanos,System.nanoTime()-started))GpuQualification1961.rejectSpeed(key);ProcessingTiming1947.detailRoute1976(trace,1);}
            return result;
        } catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
          catch(RuntimeException unavailable){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("legacy finish interrupted");return null;}
          catch(LinkageError unavailable){return null;}
          catch(OutOfMemoryError unavailable){return null;}
    }
    private static String finishKey1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan outputPlan,boolean refreshNoise) {
        return GpuNoise1960.fingerprint()+"|finish-chain1976:v2|"+source.getWidth()+","+source.getHeight()+","+rotation+","+width+","+height+","+refreshNoise+","+
            outputPlan.sharpGainQ8+","+outputPlan.sharpFloorQ8+","+outputPlan.sharpLimit+","+outputPlan.texturePriority+","+outputPlan.haloSuppression+","+
            Float.floatToRawIntBits(outputPlan.sourceSigma)+","+Float.floatToRawIntBits(outputPlan.outputScale)+","+
            (outputPlan.faceRegions!=null)+","+(outputPlan.smoothedRegions!=null)+","+(outputPlan.localNoise!=null);
    }
    /** Benchmark the already deployed route, including full input transfer.
     * It may use only an existing finish certificate, never a new candidate. */
    static Bitmap benchmarkFinish1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan) {
        return benchmarkFinish1978(source,rotation,width,height,plan,true);
    }
    static Bitmap benchmarkFinish1978(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh) {
        String key=finishKey1976(source,rotation,width,height,plan,refresh);
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2&&!GpuQualification1961.exactRejected(key)) {
            Bitmap value=runFinish1976(source,rotation,width,height,plan,refresh,null,32<<proof.variant);
            if(value!=null)return value;
        }
        String legacyKey=legacyKey1976(key);proof=GpuQualification1961.restore(legacyKey);
        if(proof!=null&&proof.variant==0&&!GpuQualification1961.exactRejected(legacyKey))return runLegacy1976(source,rotation,width,height,plan,refresh,null);
        return null;
    }
    static int compareBenchmark1978(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,Bitmap expected,
            GpuQualification1961.Cancellation cancellation) {
        String key=finishKey1976(source,rotation,width,height,plan,refresh);
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2&&!GpuQualification1961.exactRejected(key))
            return compareFinish1978(source,rotation,width,height,plan,refresh,expected,32<<proof.variant,true,null,false,cancellation);
        key=legacyKey1976(key);proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant==0&&!GpuQualification1961.exactRejected(key))
            return compareFinish1978(source,rotation,width,height,plan,refresh,expected,32,false,null,false,cancellation);
        return ResidentProof1978.UNAVAILABLE;
    }
    private static final class ResidentMaskKey1976 implements QualityPixels1932.SmoothMask {
        public int smoothingQ8(int x,int y){return 0;}
    }
    private static final QualityPixels1932.SmoothMask RESIDENT_MASK_KEY1976=new ResidentMaskKey1976();
    /** Only mask presence belongs to this key; no mask value or pixel is used
     * before the actual Strong mask has finished. This enforces the exact
     * deployed whole-finish route as the resident benchmark's speed baseline. */
    static String residentBaseline1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan) {
        return residentBaseline1978(source,rotation,width,height,plan,false);
    }
    static String residentBaseline1978(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean identity) {
        String key=finishKey1976(source,rotation,width,height,plan.withSmoothedRegions(RESIDENT_MASK_KEY1976),!identity);
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2&&!GpuQualification1961.exactRejected(key))return "pipeline:"+proof.variant+":"+proof.cpuNanos+":"+proof.gpuNanos;
        key=legacyKey1976(key);proof=GpuQualification1961.restore(key);
        if(proof==null||proof.variant!=0||GpuQualification1961.exactRejected(key))return null;
        return "legacy:"+proof.cpuNanos+":"+proof.gpuNanos;
    }
    static int chooseFinish1976(long cpu,long legacyFastest,long[] gpu,boolean[] exact,boolean legacyExact) {
        if(!legacyExact||gpu==null||exact==null||gpu.length!=3||exact.length!=3)return -1;
        int best=-1;
        for(int variant=0;variant<3;variant++)if(exact[variant]&&win(cpu,gpu[variant])&&win(legacyFastest,gpu[variant])&&(best<0||gpu[variant]<gpu[best]))best=variant;
        return best;
    }
    private static boolean win(long cpu,long gpu){return cpu>0&&gpu>0&&gpu<=cpu-cpu/20;}
    /** Only the private idle snapshot is examined before scheduling. Unsupported
     * alpha does not become a permanent pixel-mismatch fact for opaque photos.
     * The admitted foreground runner still inspects every source pixel. */
    private static boolean opaqueSnapshot1962(Bitmap snapshot) {
        int width=snapshot.getWidth(),height=snapshot.getHeight();int[] row=new int[width];
        for(int y=0;y<height;y++) {
            if(Thread.currentThread().isInterrupted())return false;
            snapshot.getPixels(row,0,width,0,y,width,1);
            for(int pixel:row)if((pixel>>>24)!=255)return false;
        }
        return true;
    }
    private static boolean known(Object mask,String name) {
        return mask==null||mask.getClass().getClassLoader()==GpuChain1961.class.getClassLoader()&&mask.getClass().getName().equals(name);
    }
    static boolean eligibleResident1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan) {
        return eligibleFinish(source,rotation,width,height,plan);
    }
    static boolean opaqueResident1976(Bitmap source){return opaqueSnapshot1962(source);}
    private static boolean eligibleFinish(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan) {
        if(source==null||source.isRecycled()||source.getConfig()!=Bitmap.Config.ARGB_8888||width<2||height<2||plan==null||plan.sharpGainQ8<=0||
                rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270||
                (long)source.getWidth()*source.getHeight()>Integer.MAX_VALUE||(long)width*height>Integer.MAX_VALUE)return false;
        ColorSpace color=source.getColorSpace();
        if(color!=null&&!color.isSrgb()||Build.VERSION.SDK_INT>=34&&source.hasGainmap())return false;
        // Stateful/custom masks are deliberately outside detached qualification.
        if(!known(plan.faceRegions,"com.hiro.ulike.FaceRegions1934$Mask"))return false;
        return plan.smoothedRegions==null||known(plan.smoothedRegions,"com.hiro.ulike.QualityPipeline1932$SmoothRegions1958")||
            known(plan.smoothedRegions,"com.hiro.ulike.QualityPipeline1932$SmoothRegions1958$1");
    }
    /** Unchanged CPU algorithms are the whole-slice oracle, including every
     * intermediate rounding, geometry, evidence refresh and final output pixel. */
    static Bitmap cpuFinish(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise) {
        Bitmap owned=source.copy(Bitmap.Config.ARGB_8888,true);
        if(owned==null)throw new OutOfMemoryError("finish oracle copy");
        return cpuFinishOwned1962(owned,rotation,width,height,outputPlan,refreshNoise);
    }
    /** Ownership transfers here. The qualification timer surrounds precisely
     * these ordinary in-place/native-strip, geometry and evidence operations. */
    static Bitmap cpuFinishOwned1962(Bitmap corrected,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise) {
        Bitmap geometry=null;boolean done=false;
        try {
            // Use the existing native-CPU/strip scheduler as the speed baseline,
            // rather than an artificially slow serial Java pixel loop.
            QualityPipeline1932.finishInPlace(corrected,null,true,false);
            geometry=FastResize1933.resampleCpu1960(corrected,rotation,width,height);
            QualityPixels1932.Plan finalPlan=outputPlan;
            if(refreshNoise) {
                final Bitmap image=geometry;
                finalPlan=outputPlan.withOutputNoise(SpatialNoise1934.probeCpu1961(new SpatialNoise1934.Patches(){
                    public void read(int[] pixels,int x,int y,int w,int h){image.getPixels(pixels,0,w,x,y,w,h);}
                },width,height));
            }
            QualityPipeline1932.finishInPlace(geometry,finalPlan,false,true);done=true;return geometry;
        } finally {if(corrected!=geometry)corrected.recycle();if(!done&&geometry!=null)geometry.recycle();}
    }
    /** Host QA entry executes the real production session command graph. */
    static Bitmap runFinish(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise) {
        return runFinish(source,rotation,width,height,outputPlan,refreshNoise,null);
    }
    private static Bitmap runFinish(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,ProcessingTiming1947.Trace trace) {
        return runFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace,32);
    }
    static Bitmap runFinish1976(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,ProcessingTiming1947.Trace trace,int tileRows) {
        if(tileRows!=32&&tileRows!=64&&tileRows!=128)throw new IllegalArgumentException("finish tile rows");
        Timing1962 timing=new Timing1962(trace);
        try {return runFinishStages(source,rotation,width,height,outputPlan,refreshNoise,timing,tileRows,null,true);}
        finally {timing.close();}
    }
    static Bitmap runLegacy1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        Timing1962 timing=new Timing1962(trace);
        try {return runFinishStages(source,rotation,width,height,plan,refresh,timing,32,null,false);}
        finally {timing.close();}
    }
    static Bitmap runResident1976(Bitmap source,GpuNoise1960.Session session,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        if(session==null)return null;Timing1962 timing=new Timing1962(trace);
        try {return runFinishStages(source,rotation,width,height,plan,refresh,timing,64,session,true);}
        finally {timing.close();session.close();}
    }
    static Bitmap runResident1978(Bitmap source,GpuNoise1960.Session session,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean identity,ProcessingTiming1947.Trace trace) {
        if(session==null)return null;Timing1962 timing=new Timing1962(trace);
        try {return runFinishStages(source,rotation,width,height,plan,!identity,timing,64,session,true,null,identity);}
        finally {timing.close();session.close();}
    }
    static int compareFinish1978(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,Bitmap expected,int tileRows,boolean pipeline,
            GpuNoise1960.Session carried,boolean identityNoiseBefore,
            GpuQualification1961.Cancellation cancellation) {
        Timing1962 timing=new Timing1962(null);
        try {
            if(expected==null||expected.isRecycled()||expected.getWidth()!=width||expected.getHeight()!=height)return ResidentProof1978.UNAVAILABLE;
            ResidentProof1978 comparison=new ResidentProof1978(expected,cancellation);
            runFinishStages(source,rotation,width,height,plan,refresh,timing,tileRows,carried,pipeline,comparison,identityNoiseBefore);
            return comparison.result();
        } catch(DeferredChain unavailable){return ResidentProof1978.UNAVAILABLE;}
        finally {timing.close();if(carried!=null)carried.close();}
    }
    /** Separate intervals preserve the original evidence/finishing categories.
     * Private qualification runs pass no capture trace. */
    private static final class Timing1962 {
        final ProcessingTiming1947.Trace trace;ProcessingTiming1947.Token active;
        Timing1962(ProcessingTiming1947.Trace trace){this.trace=trace;stage(ProcessingTiming1947.CORRECTION);}
        void stage(int stage){ProcessingTiming1947.end(active);active=ProcessingTiming1947.beginStage(trace,stage);}
        void close(){ProcessingTiming1947.end(active);active=null;}
    }
    private static Bitmap runFinishStages(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,Timing1962 timing,int tileRows,GpuNoise1960.Session carried,boolean pipeline) {
        return runFinishStages(source,rotation,width,height,outputPlan,refreshNoise,timing,tileRows,carried,pipeline,null,false);
    }
    private static Bitmap runFinishStages(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,Timing1962 timing,int tileRows,
            GpuNoise1960.Session carried,boolean pipeline,ResidentProof1978 comparison,boolean identityNoiseBefore) {
        int sw=source.getWidth(),sh=source.getHeight(),n=Math.multiplyExact(sw,sh),out=Math.multiplyExact(width,height);
        boolean identity=rotation==0&&sw==width&&sh==height;
        if(identityNoiseBefore&&(!identity||carried==null||refreshNoise))throw new IllegalArgumentException("resident identity evidence position");
        GpuPolicy1960.GeometryData data=identity?null:GpuPolicy1960.geometry(sw,sh,rotation,width,height);
        long extra=(carried==null?12L:4L)*n+(comparison==null?8L:4L)*out+48L*width*tileRows+16L*1024*1024;
        if(comparison!=null)extra+=comparison.workspaceBytes();
        if(data!=null)extra+=4L*(data.tables.length+data.weights.length);
        if(!GpuNoise1960.workspaceFits(extra))throw new DeferredChain();
        int[] pixels=null;
        if(carried==null) {pixels=new int[n];source.getPixels(pixels,0,sw,0,0,sw,sh);for(int p:pixels)if((p>>>24)!=255)return null;}
        GpuNoise1960.Session session=carried==null?GpuNoise1960.open():carried;if(session==null)throw new DeferredChain();
        Bitmap result=null;boolean done=false;GpuNoise1960.Lease1971 imageLease=null;
        try {
            // Geometry executes bounded bands, so reserving its complete
            // rotated image would incorrectly exclude large otherwise-safe
            // photographs. Per-band capacity is reserved before each dispatch.
            int[] slots=data==null?new int[]{carried==null?0:24,1,3,5}:new int[]{carried==null?0:24,1,2,3,5,6,7};
            long[] capacities=data==null?new long[]{4L*n,4L*n,4,4}:new long[]{4L*n,4L*n,4L*out,4,4,4L*data.weights.length,4L*data.tables.length};
            imageLease=session.reserveCapacity1971(slots,capacities,0);
            if(imageLease==null||!imageLease.revalidate1971())throw new DeferredChain();
            int[] finish=new int[32];finish[1]=sw;finish[2]=sh;finish[5]=sh;finish[8]=1;
            int sourceSlot=carried==null?0:24;
            QualityPixels1932.Plan finalPlan=outputPlan;
            if(identityNoiseBefore) {
                // The unchanged identity CPU route measures residual noise after
                // Strong and before moire. Never move that observation past moire.
                timing.stage(ProcessingTiming1947.NOISE);
                try {
                    SpatialNoise1934 evidence=GpuAnalysis1961.residentSpatial1962(session,sourceSlot,sw,sh);
                    if(evidence==null)return null;finalPlan=outputPlan.withOutputNoise(evidence);
                } finally {timing.stage(ProcessingTiming1947.CORRECTION);}
            }
            GpuNoise1960.Batch b=new GpuNoise1960.Batch();if(pixels!=null)b.uploadDirect(0,pixels);
            b.allocate(1,4L*n).allocate(3,4).allocate(5,4)
                .dispatch(GpuNoise1960.FINISH1961,new int[]{sourceSlot,sourceSlot,1,3},finish,null,n);
            if(!identity)b.allocate(2,4L*out).uploadDirect(6,data.weights).uploadDirect(7,data.tables);
            if(!session.run(b))return null;
            checkCancelled();
            int imageSlot=identity?1:2;
            if(!identity) {
                int[] base=data.uniforms(0),bindings={1,2,4,5,6,7};
                for(int first=0;first<height;) {
                    checkCancelled();int rows=Math.min(32,height-first),lo,hi;
                    for(;;) {
                        lo=Integer.MAX_VALUE;hi=-1;
                        if(data.exactCrop){lo=first+base[7];hi=lo+rows;}
                        else {for(int y=first;y<first+rows;y++)for(int t=data.tables[base[10]+y];t<data.tables[base[10]+y+1];t++)
                            {int row=data.tables[base[11]+t];lo=Math.min(lo,row);hi=Math.max(hi,row);}hi++;}
                        long intermediate=data.exactCrop?4:12L*width*(hi-lo);
                        if(lo<0||hi<=lo||hi>base[14])return null;
                        if(intermediate<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(intermediate+65536L))break;
                        if(rows==1)throw new DeferredChain();rows=Math.max(1,rows/2);
                    }
                    int[] u=base.clone();u[15]=first;u[16]=first+rows;u[17]=lo;u[18]=hi-lo;u[20]=0;u[21]=1;
                    b=new GpuNoise1960.Batch().allocate(4,data.exactCrop?4:12L*width*(hi-lo));
                    if(data.exactCrop)b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*rows);
                    else {u[0]=1;b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*(hi-lo));u[0]=2;b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*rows);}
                    GpuNoise1960.Lease1971 geometryLease=session.reserveCapacity1971(new int[]{4},new long[]{data.exactCrop?4:12L*width*(hi-lo)},0);
                    if(geometryLease==null)throw new DeferredChain();
                    try {if(!geometryLease.revalidate1971()||!session.run(b))return null;}
                    finally {geometryLease.close();}
                    first+=rows;
                }
            }
            if(refreshNoise) {
                timing.stage(ProcessingTiming1947.NOISE);
                try {
                    SpatialNoise1934 evidence=GpuAnalysis1961.residentSpatial1962(session,imageSlot,width,height);
                    if(evidence==null)return null;finalPlan=outputPlan.withOutputNoise(evidence);
                } finally {timing.stage(ProcessingTiming1947.CORRECTION);}
            }
            result=comparison==null?Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888):null;
            imageLease.close();imageLease=null;
            if(!(pipeline?finishBands1978(session,imageSlot,width,height,finalPlan,result,tileRows,comparison):finishBandsLegacy1978(session,imageSlot,width,height,finalPlan,result,comparison)))return null;
            if(comparison!=null){done=true;return source;}
            result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());done=true;return result;
        } finally {if(imageLease!=null)imageLease.close();session.close();if(!done&&result!=null)result.recycle();}
    }
    /** Preserved .75 single-bank 32-row finish schedule. This is both the
     * real speed baseline and the fallback when pipelining is not faster. */
    private static boolean finishBandsLegacy1976(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result) {
        return finishBandsLegacy1978(session,imageSlot,width,height,plan,result,null);
    }
    private static boolean finishBandsLegacy1978(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result,ResidentProof1978 comparison) {
        int[] readPixels=new int[Math.multiplyExact(width,Math.min(32,height))];
        for(int first=0;first<height;first+=32) {
            checkCancelled();int last=Math.min(height,first+32),count=width*(last-first);
            FinishPolicy1953.Band policy=FinishPolicy1953.prepareParallel1978(plan,width,height,first,last,0);
            if(policy==null)return false;
            try {
                int[] u=new int[32];u[1]=width;u[2]=height;u[4]=first;u[5]=last;u[6]=first;u[7]=first;
                u[9]=1;u[10]=plan.sharpGainQ8;u[11]=plan.sharpFloorQ8;u[12]=plan.sharpLimit;
                u[13]=plan.texturePriority?1:0;u[14]=plan.haloSuppression?1:0;u[16]=policy.mode;
                GpuNoise1960.Batch batch=new GpuNoise1960.Batch().uploadDirect(3,policy.words).allocate(17,4L*count)
                    .dispatch(GpuNoise1960.FINISH1961,new int[]{imageSlot,imageSlot,17,3},u,null,count);
                boolean read=session.executeInto(batch,17,count,readPixels,0);checkCancelled();if(!read)return false;
                if(!finishRows1978(result,comparison,readPixels,width,first,last-first))return false;
            } finally {policy.close();}
        }
        return true;
    }
    /** The two output/policy banks are disjoint until collect completes. CPU
     * policy preparation and the next submission overlap the preceding GPU
     * tile. Only private result Bitmap rows are written before whole success. */
    private static boolean finishBands1976(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result,int tileRows) {
        return finishBands1978(session,imageSlot,width,height,plan,result,tileRows,null);
    }
    private static boolean finishBands1978(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result,int tileRows,ResidentProof1978 comparison) {
        final int capacity=Math.multiplyExact(width,Math.min(tileRows,height));
        final int[] policySlots={3,8},outputSlots={17,18};
        GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{3,8,17,18},
            new long[]{16L*capacity,16L*capacity,4L*capacity,4L*capacity},8L*capacity+512L);
        if(lease==null)throw new DeferredChain();
        try {
            int[][] read={new int[capacity],new int[capacity]};lease.consumeJava1974(8L*capacity);
            GpuNoise1960.Ticket[] pending=new GpuNoise1960.Ticket[2];
            int[] firsts=new int[2],counts=new int[2];int sequence=0;
            for(int first=0;first<height;first+=tileRows,sequence++) {
                checkCancelled();int bank=sequence&1;
                if(pending[bank]!=null) {
                    if(!collectBand1978(session,pending[bank],outputSlots[bank],counts[bank],read[bank],result,width,firsts[bank],comparison))return false;
                    pending[bank]=null;
                }
                int last=Math.min(height,first+tileRows),count=width*(last-first);
                FinishPolicy1953.Band policy=FinishPolicy1953.prepareParallel1978(plan,width,height,first,last,0);
                if(policy==null)return false;
                try {
                    if(4L*policy.words.length>16L*capacity||!lease.revalidate1971())throw new DeferredChain();
                    int[] u=new int[32];u[1]=width;u[2]=height;u[4]=first;u[5]=last;u[6]=first;u[7]=first;
                    u[9]=1;u[10]=plan.sharpGainQ8;u[11]=plan.sharpFloorQ8;u[12]=plan.sharpLimit;
                    u[13]=plan.texturePriority?1:0;u[14]=plan.haloSuppression?1:0;u[16]=policy.mode;
                    GpuNoise1960.Batch batch=new GpuNoise1960.Batch().uploadDirect(policySlots[bank],policy.words).allocate(outputSlots[bank],4L*count)
                        .dispatch(GpuNoise1960.FINISH1961,new int[]{imageSlot,imageSlot,outputSlots[bank],policySlots[bank]},u,null,count);
                    pending[bank]=session.submit(batch,bank);if(pending[bank]==null)return false;
                    firsts[bank]=first;counts[bank]=count;
                } finally {policy.close();}
            }
            // Bank order need not be row order: each private output row has one owner.
            for(int bank=0;bank<2;bank++)if(pending[bank]!=null&&
                !collectBand1978(session,pending[bank],outputSlots[bank],counts[bank],read[bank],result,width,firsts[bank],comparison))return false;
            return true;
        } finally {lease.close();}
    }
    private static boolean collectBand1976(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int slot,int count,
            int[] pixels,Bitmap result,int width,int first) {
        return collectBand1978(session,ticket,slot,count,pixels,result,width,first,null);
    }
    private static boolean collectBand1978(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int slot,int count,
            int[] pixels,Bitmap result,int width,int first,ResidentProof1978 comparison) {
        if(!session.collectInto(ticket,slot,count,pixels,0))return false;
        checkCancelled();return finishRows1978(result,comparison,pixels,width,first,count/width);
    }
    private static boolean finishRows1978(Bitmap result,ResidentProof1978 comparison,int[] pixels,int width,int first,int rows) {
        if(comparison!=null)return comparison.rows(first,rows,pixels,0);
        result.setPixels(pixels,0,width,0,first,width,rows);return true;
    }
    private static void checkCancelled(){GpuQualification1961.Cancellation resident=GpuResident1976.cancellation();if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled()||resident!=null&&resident.cancelled())throw new java.util.concurrent.CancellationException("finish chain cancelled");}
    private static final class DeferredChain extends RuntimeException {}
    private static void reject(String key){GpuQualification1961.reject(key);synchronized(REJECTED){REJECTED.put(key,Boolean.TRUE);while(REJECTED.size()>24)REJECTED.remove(REJECTED.keySet().iterator().next());}}
    private static boolean eligible(Bitmap source,int rotation,int width,int height) {
        if(source==null||source.isRecycled()||source.getConfig()!=Bitmap.Config.ARGB_8888||width<=0||height<=0||
                rotation!=0&&rotation!=90&&rotation!=180&&rotation!=270||
                rotation==0&&source.getWidth()==width&&source.getHeight()==height)return false;
        ColorSpace color=source.getColorSpace();return (color==null||color.isSrgb())&&
            !(Build.VERSION.SDK_INT>=34&&source.hasGainmap())&&
            (long)source.getWidth()*source.getHeight()<=Integer.MAX_VALUE&&(long)width*height<=Integer.MAX_VALUE;
    }
    static Bitmap cpu(Bitmap source,int rotation,int width,int height) {
        int sw=source.getWidth(),sh=source.getHeight();int[] input=new int[Math.multiplyExact(sw,sh)],output=new int[input.length];
        source.getPixels(input,0,sw,0,0,sw,sh);
        QualityPixels1932.finishStripAtBefore1951(input,output,sw,sh,0,sh,null,true,false,0);
        Bitmap corrected=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);
        try {corrected.setPixels(output,0,sw,0,0,sw,sh);corrected.setDensity(source.getDensity());corrected.setHasAlpha(source.hasAlpha());
            return FastResize1933.resampleCpu1960(corrected,rotation,width,height);
        } finally {corrected.recycle();}
    }
    static Bitmap run(Bitmap source,int rotation,int width,int height) {
        GpuPolicy1960.GeometryData data=GpuPolicy1960.geometry(source.getWidth(),source.getHeight(),rotation,width,height);
        int sw=source.getWidth(),sh=source.getHeight(),n=Math.multiplyExact(sw,sh),out=Math.multiplyExact(width,height);
        long extra=16L*n+4L*out+4L*(data.tables.length+data.weights.length)+
            16L*width*64+65536L;
        if(!GpuNoise1960.workspaceFits(extra))throw new DeferredChain();
        int[] pixels=new int[n];source.getPixels(pixels,0,sw,0,0,sw,sh);
        for(int p:pixels)if((p>>>24)!=255)return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredChain();
        try {
            int[] finish=new int[32];finish[1]=sw;finish[2]=sh;finish[5]=sh;finish[8]=1;
            GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(0,pixels).allocate(1,4L*n)
                .allocate(3,4).allocate(5,4)
                .upload(6,data.weights).upload(7,data.tables)
                .dispatch(GpuNoise1960.FINISH1961,new int[]{0,0,1,3},finish,null,n);
            if(!session.run(batch))return null;
            int[] geometryBindings={1,2,4,5,6,7};
            Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);boolean done=false;
            try {
                int[] base=data.uniforms(0);
                for(int first=0;first<height;) {
                    if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled())return null;
                    int rows=Math.min(32,height-first),lo,hi;
                    for(;;) {
                        lo=Integer.MAX_VALUE;hi=-1;
                        if(data.exactCrop){lo=first+base[7];hi=lo+rows;}
                        else {
                            for(int y=first;y<first+rows;y++)for(int t=data.tables[base[10]+y];t<data.tables[base[10]+y+1];t++)
                                {int row=data.tables[base[11]+t];lo=Math.min(lo,row);hi=Math.max(hi,row);}
                            hi++;
                        }
                        long intermediate=data.exactCrop?4:12L*width*(hi-lo);
                        if(lo<0||hi<=lo||hi>base[14])return null;
                        if(intermediate<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(intermediate+12L*width*rows+65536L))break;
                        if(rows==1)throw new DeferredChain();rows=Math.max(1,rows/2);
                    }
                    int count=Math.multiplyExact(width,rows);int[] u=base.clone();
                    u[15]=first;u[16]=first+rows;u[17]=lo;u[18]=hi-lo;u[20]=1;u[21]=1;
                    batch=new GpuNoise1960.Batch().allocate(2,4L*count).allocate(4,data.exactCrop?4:12L*width*(hi-lo));
                    if(data.exactCrop)batch.dispatch(GpuNoise1960.GEOMETRY,geometryBindings,u,null,count);
                    else {
                        u[0]=1;batch.dispatch(GpuNoise1960.GEOMETRY,geometryBindings,u,null,Math.multiplyExact(width,hi-lo));
                        u[0]=2;batch.dispatch(GpuNoise1960.GEOMETRY,geometryBindings,u,null,count);
                    }
                    int[][] read=session.execute(batch,new int[]{2},new int[]{count});if(read==null||Thread.currentThread().isInterrupted())return null;
                    result.setPixels(read[0],0,width,0,first,width,rows);first+=rows;
                }
                result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());done=true;return result;
            }
            finally{if(!done)result.recycle();}
        } finally {session.close();}
    }
}
