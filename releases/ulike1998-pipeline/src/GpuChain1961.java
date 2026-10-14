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
    private static final ThreadLocal<Preflight1981> PREFLIGHT1981=new ThreadLocal<Preflight1981>();
    /** A capture-local plan for the already certified Strong-to-finish route.
     * Geometry is prepared once before Strong. The eventual Strong session
     * reserves the finish capacities before starting any NR strip, and the CPU
     * producer subtracts that outstanding debt from its own worker allowance.
     * No source pixel or quality proof is changed by admission. */
    static final class Preflight1981 implements AutoCloseable {
        final Bitmap source;final int rotation,width,height,geometryRows;
        final GpuPolicy1960.GeometryData geometry;final int[] slots;final long[] capacities,uploadBytes1983;
        final long javaPeak;GpuNoise1960.Lease1971 lease;GpuNoise1960.Session session;
        long deadline1981;
        private boolean closed;
        Preflight1981(Bitmap source,int rotation,int width,int height,int rows,GpuPolicy1960.GeometryData data,
                int[] slots,long[] capacities,long javaPeak){
            this(source,rotation,width,height,rows,data,slots,capacities,capacities,javaPeak);
        }
        Preflight1981(Bitmap source,int rotation,int width,int height,int rows,GpuPolicy1960.GeometryData data,
                int[] slots,long[] capacities,long[] uploadBytes,long javaPeak){
            this.source=source;this.rotation=rotation;this.width=width;this.height=height;geometryRows=rows;
            geometry=data;this.slots=slots;this.capacities=capacities;uploadBytes1983=uploadBytes;this.javaPeak=javaPeak;
        }
        boolean matches(Bitmap input,int turn,int w,int h){return !closed&&source==input&&rotation==turn&&width==w&&height==h;}
        boolean bind(GpuNoise1960.Session value){
            if(closed||value==null||session!=null)return false;
            GpuNoise1960.Lease1971 admitted=value.reserveCapacity1983(slots,capacities,uploadBytes1983,javaPeak);
            if(admitted==null)return false;
            boolean bound=false;
            try{
                if(!admitted.revalidate1971())return false;
                lease=admitted;session=value;bound=true;return true;
            }finally{if(!bound)admitted.close();}
        }
        void finishAdmission(GpuNoise1960.Session value){
            if(session!=value)throw new DeferredChain();
            // Strong workers have joined. Transfer the forecast back to the
            // unchanged finish image/band leases, which immediately revalidate
            // actual capacities and heap use before their first dispatch.
            if(lease!=null){lease.close();lease=null;}
        }
        public void close(){
            if(closed)return;closed=true;
            try{if(lease!=null){lease.close();lease=null;}}
            finally{session=null;if(PREFLIGHT1981.get()==this)PREFLIGHT1981.remove();}
        }
    }
    static Preflight1981 preflightResident1981(Bitmap source,int rotation,int width,int height,int halfLength){
        if(source==null||source.isRecycled()||PREFLIGHT1981.get()!=null||halfLength<0)return null;
        int sw=source.getWidth(),sh=source.getHeight();long n=(long)sw*sh,out=(long)width*height;
        if(sw<1||sh<1||width<2||height<2||n>Integer.MAX_VALUE||out>Integer.MAX_VALUE)return null;
        try{
            boolean identity=rotation==0&&sw==width&&sh==height;
            GpuPolicy1960.GeometryData data=identity?null:GpuPolicy1960.geometry(sw,sh,rotation,width,height);
            long model=StrongNoise1958.modelMemoryBytes(sw,sh)-4L*halfLength;
            long smooth=((long)sw+3)/4*((sh+3L)/4);
            // A joined Strong image owns only slot24. Finish later frees its
            // source after moire and moire after geometry, so one scalar debt
            // reserves the largest future phase instead of all phase buffers.
            for(int rows=32;rows>=1;rows/=2){
                long intermediate=data==null||data.exactCrop?4:geometryPeak1981(data,width,height,rows);
                long tableBytes=data==null?0:4L*(data.tables.length+data.weights.length);
                long peak=finishPeak1991(n,out,width,64,0,tableBytes,intermediate,data!=null,true);
                if(model<0||!GpuNoise1960.workspaceFits(Math.max(model+smooth,peak)))continue;
                int[] slots=new int[]{24};long[] capacities=new long[]{4L*n},uploads=new long[]{4L*n};
                // This lease is future capacity debt, not an allocated Java
                // image. Source24 is charged separately and never twice.
                long javaPeak=Math.max(0,peak-4L*n);
                if(!GpuNoise1960.planFits1983(capacities,uploads,javaPeak))continue;
                Preflight1981 result=new Preflight1981(source,rotation,width,height,rows,data,slots,capacities,uploads,javaPeak);
                PREFLIGHT1981.set(result);return result;
            }
        }catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
        catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        return null;
    }
    private static long geometryPeak1981(GpuPolicy1960.GeometryData data,int width,int height,int rows){
        int[] u=data.u;long peak=4;
        for(int first=0;first<height;first+=rows){
            checkCancelled();
            int lo=Integer.MAX_VALUE,hi=-1;
            for(int y=first;y<Math.min(height,first+rows);y++)for(int tap=data.tables[u[10]+y];tap<data.tables[u[10]+y+1];tap++){
                int row=data.tables[u[11]+tap];lo=Math.min(lo,row);hi=Math.max(hi,row);
            }
            if(lo<0||hi<lo||hi>=u[14])throw new DeferredChain();
            peak=Math.max(peak,12L*width*(hi-lo+1L));
        }
        return peak;
    }
    static boolean admitResident1981(GpuNoise1960.Session session){
        Preflight1981 plan=PREFLIGHT1981.get();return plan==null||plan.bind(session);
    }
    static long residentDeadline1981(){Preflight1981 plan=PREFLIGHT1981.get();return plan==null?0:plan.deadline1981;}
    static Bitmap moireGeometry(final Bitmap source,final int rotation,final int width,final int height) {
        return moireGeometry1982(source,rotation,width,height,null);
    }
    static Bitmap moireGeometry1982(final Bitmap source,final int rotation,final int width,final int height,ProcessingTiming1947.Trace trace1982) {
        if(!eligible(source,rotation,width,height)||GpuNoise1960.sessionBusy()||
                GpuQualification1961.background()||!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||
                !GpuNoise1960.supports(GpuNoise1960.GEOMETRY))return null;
        final String key=GpuNoise1960.fingerprint()+"|moire-geometry|"+source.getWidth()+","+source.getHeight()+","+rotation+","+width+","+height;
        synchronized(REJECTED){if(REJECTED.containsKey(key))return null;}
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null) {
            try {
                long start=System.nanoTime();Bitmap result=run(source,rotation,width,height);long elapsed=System.nanoTime()-start;
                if(result==null){GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return null;}
                if(elapsed>proof.cpuNanos-proof.cpuNanos/20)GpuQualification1961.rejectMeasuredSpeed1992(key,"moire_geometry",proof.cpuNanos,elapsed);
                if(trace1982!=null)try{PipelineDiagnostics1982.selected(trace1982,2,1,1,elapsed);}catch(Throwable optional){}
                return result;
            } catch(DeferredChain unavailable){return null;}
              catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
              catch(RuntimeException unavailable){GpuQualification1961.rejectNonSpeed1988(key,"exception");return null;}
              catch(LinkageError unavailable){GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return null;}
              catch(OutOfMemoryError unavailable){return null;}
        }
        GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.MOIRE_GEOMETRY);
        if(!GpuQualification1961.maySchedule(key))return null;
        long retained=4L*source.getWidth()*source.getHeight();
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained*3+8L*width*height))return null;
        GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryCopy(retained,GpuSnapshotBudget1981.MOIRE_GEOMETRY);
        if(copy==null)return null;
        Bitmap snapshot=null;
        try {
            if(!GpuQualification1961.canQueue(key,retained))return null;
            if(!copy.begin())return null;
            snapshot=source.copy(Bitmap.Config.ARGB_8888,false);if(snapshot==null)return null;
            final Bitmap owned=snapshot;
            if(!copy.current())return null;
            GpuQualification1961.schedule(key,retained,new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    try {
                    long cpu=Long.MAX_VALUE,gpu=0;
                    for(int trial=0;trial<2;trial++) {
                        if(cancellation.cancelled())return;
                        Bitmap expected=null,candidate=null;
                        try {
                            long started=System.nanoTime();expected=cpu(owned,rotation,width,height);long cpuTime=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            started=System.nanoTime();candidate=GpuChain1961.run(owned,rotation,width,height);long gpuTime=System.nanoTime()-started;
                            if(cancellation.cancelled())return;
                            if(candidate==null){GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return;}
                            if(!GpuGeometry1960.equal1961(expected,candidate)){reject(key);return;}
                            if(cpuTime<=0||gpuTime<=0||gpuTime>cpuTime-cpuTime/20){GpuQualification1961.rejectMeasuredSpeed1992(key,"moire_geometry",cpuTime,gpuTime);return;}
                            cpu=Math.min(cpu,cpuTime);gpu=Math.max(gpu,gpuTime);
                        } finally {if(expected!=null&&expected!=owned)expected.recycle();if(candidate!=null&&candidate!=owned)candidate.recycle();}
                    }
                    if(!cancellation.cancelled()&&gpu<=cpu-cpu/20)GpuQualification1961.qualified(key,cpu,gpu,0);
                    else if(!cancellation.cancelled())GpuQualification1961.rejectMeasuredSpeed1992(key,"moire_geometry",cpu,gpu);
                    }catch(DeferredChain unavailable){}
                    catch(java.util.concurrent.CancellationException stop){throw stop;}
                    catch(RuntimeException failure){if(!cancellation.cancelled())GpuQualification1961.rejectNonSpeed1988(key,"exception");}
                    catch(LinkageError failure){if(!cancellation.cancelled())GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");}
                    catch(OutOfMemoryError unavailable){}
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });
            snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally{try{if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}finally{copy.close();}}
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
    private static final ThreadLocal<Boolean> FINISH_PRESSURE1997=new ThreadLocal<Boolean>();
    static Bitmap finish(final Bitmap source,final int rotation,final int width,final int height,
            final QualityPixels1932.Plan outputPlan,final boolean refreshNoise,
            final ProcessingTiming1947.Trace trace) {
        Boolean previous=FINISH_PRESSURE1997.get();
        FINISH_PRESSURE1997.set(Boolean.FALSE);
        try{return finishOwned1997(source,rotation,width,height,outputPlan,refreshNoise,trace);}
        finally{if(previous==null)FINISH_PRESSURE1997.remove();else FINISH_PRESSURE1997.set(previous);}
    }
    private static Bitmap finishOwned1997(final Bitmap source,final int rotation,final int width,final int height,
            final QualityPixels1932.Plan outputPlan,final boolean refreshNoise,
            final ProcessingTiming1947.Trace trace) {
        if(!eligibleFinish(source,rotation,width,height,outputPlan)||GpuNoise1960.sessionBusy()||
                GpuQualification1961.background()||!GpuNoise1960.supports(GpuNoise1960.FINISH1961)||
                !GpuNoise1960.supports(GpuNoise1960.GEOMETRY)||
                refreshNoise&&!GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961))return null;
        ProcessingTiming1947.Trace selected1982=null;
        try{selected1982=PipelineDiagnostics1982.foreground(trace);}catch(Throwable optional){}
        final String key=finishKey1976(source,rotation,width,height,outputPlan,refreshNoise);
        if(GpuQualification1961.exactRejected(key))return legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
        GpuQualification1961.Record cpuChoice1992=GpuQualification1961.restore(cpuChoiceKey1992(key));
        if(cpuChoice1992!=null) {
            Timing1962 cpuTiming1992=new Timing1962(trace);
            long started1992=System.nanoTime();
            try {
                Bitmap input1992=source.copy(Bitmap.Config.ARGB_8888,true);
                if(input1992==null)return null;
                Bitmap result1992=cpuFinishTimed1992(input1992,rotation,width,height,outputPlan,refreshNoise,cpuTiming1992);
                if(result1992!=null) {
                    ProcessingTiming1947.detailRoute1976(trace,0);
                    try{PipelineDiagnostics1982.selected(selected1982,2,0,1,System.nanoTime()-started1992);}catch(Throwable optional){}
                }
                return result1992;
            } finally {cpuTiming1992.close();}
        }
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2) {
            try {
                finishSelection1995(trace,5,32<<proof.variant);
                long started=System.nanoTime();Bitmap result=runFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace,32<<proof.variant);long elapsed=System.nanoTime()-started;
                if(result==null)return null;
                if(!win(proof.cpuNanos,elapsed))GpuQualification1961.rejectMeasuredThreshold1992(key,"finish","certified_reference",proof.cpuNanos,elapsed,proof.cpuNanos-proof.cpuNanos/20);
                ProcessingTiming1947.detailRoute1976(trace,1+proof.variant);
                try{PipelineDiagnostics1982.selected(selected1982,2,1,1,elapsed);}catch(Throwable optional){}
                return result;
            } catch(DeferredChain unavailable){return null;}
              catch(java.util.concurrent.CancellationException interrupted){throw interrupted;}
              catch(RuntimeException failure){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("finish interrupted");return null;}
              catch(LinkageError failure){return null;}
              catch(OutOfMemoryError unavailable){return null;}
        }
        // A measured older schedule remains a valid tuned winner. Do not
        // hide that photo-specific speed evidence behind the corpus fallback.
        Bitmap measuredLegacy1992=legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
        if(measuredLegacy1992!=null)return measuredLegacy1992;
        // A device/corpus certificate is a distinct standard-route admission;
        // it never invents a photograph proof or a per-photo timing baseline.
        // Keep any genuinely measured faster tile variant above this fallback.
        long standardStarted1990=System.nanoTime();
        Bitmap standard1990=GpuFinishDefault1990.run(source,rotation,width,height,outputPlan,refreshNoise,trace,key);
        if(standard1990!=null) {
            ProcessingTiming1947.detailRoute1976(trace,1);
            try{PipelineDiagnostics1982.selected(selected1982,2,1,1,System.nanoTime()-standardStarted1990);}catch(Throwable optional){}
            scheduleFinish1992(source,rotation,width,height,outputPlan,refreshNoise,trace,key);
            return standard1990;
        }
        scheduleFinish1992(source,rotation,width,height,outputPlan,refreshNoise,trace,key);
        return legacyFinish1976(source,rotation,width,height,outputPlan,refreshNoise,trace);
    }
    /** One owned immutable snapshot only; comparison work starts after save. */
    private static void scheduleFinish1992(final Bitmap source,final int rotation,final int width,final int height,
            final QualityPixels1932.Plan outputPlan,final boolean refreshNoise,
            final ProcessingTiming1947.Trace trace,final String key) {
        // Preserve photograph qualification on ordinary calls. After mandatory
        // Finish needed memory relief, do not immediately consume the recovered
        // headroom again with a second full-image validation copy.
        if(Boolean.TRUE.equals(FINISH_PRESSURE1997.get())) {
            try{ProcessingTiming1947.finishValidationSkipped1997(trace,1);}catch(Throwable optional){}
            return;
        }
        GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.FINISH);
        if(!GpuQualification1961.maySchedule(key))return;
        long retained=4L*source.getWidth()*source.getHeight()+
            (outputPlan.faceRegions==null?0:((FaceRegions1934.Mask)outputPlan.faceRegions).retainedBytes1976())+
            (outputPlan.smoothedRegions==null?0:((long)width+3)/4*((height+3L)/4))+
            (outputPlan.localNoise==null?0:512L+4L*outputPlan.localNoise.columns*outputPlan.localNoise.rows+
                12L*(Math.min(32768,outputPlan.localNoise.width)+Math.min(32768,outputPlan.localNoise.height)));
        if(retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained))return;
        GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryCopy(retained,GpuSnapshotBudget1981.FINISH);
        if(copy==null)return;
        Bitmap snapshot=null;
        GpuQualification1961.Reservation1984 reservation1987=null;
        try {
            // The Finish family is a primary proof, just like the Resident
            // dependency that consumes it. Hold its real slot and bytes before
            // copying, then transfer that ownership without a second admission.
            reservation1987=GpuQualification1961.reserve1985(key,retained);
            if(!reservation1987.accepted()||!reservation1987.current()||!copy.begin()||!reservation1987.begin1985())
                return;
            long copyStarted1992=System.nanoTime();
            try {snapshot=source.copy(Bitmap.Config.ARGB_8888,false);}
            finally {finishWork1992(trace,ProcessingTiming1947.FINISH_VALIDATION_COPY1997,copyStarted1992,4L*source.getWidth()*source.getHeight());}
            if(snapshot==null)return;
            final Bitmap owned=snapshot;
            if(!copy.current()||!reservation1987.current())return;
            reservation1987.commit(new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation cancellation) {
                    if(cancellation.cancelled()||!opaqueSnapshot1962(owned))return;
                    qualifyFinish1978(owned,rotation,width,height,outputPlan,refreshNoise,cancellation);
                }
                public void close(){if(!owned.isRecycled())owned.recycle();}
            });snapshot=null;
        } catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
        finally {try{if(snapshot!=null&&!snapshot.isRecycled())snapshot.recycle();}
            finally{try{copy.close();}finally{if(reservation1987!=null)reservation1987.close();}}}
    }
    /** One idle source can first establish the ordinary finish dependency and
     * then prove Strong-to-finish. All complete comparisons precede admission.
     * Comparison runs do not allocate a full GPU destination Bitmap. Timings
     * use separate real output-producing runs after the CPU reference is freed,
     * so the smaller proof workspace cannot manufacture a speed advantage. */
    static void qualifyFinish1978(Bitmap owned,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,
            GpuQualification1961.Cancellation cancellation) {
        finishShape1989(owned,rotation,width,height);
        String key=finishKey1976(owned,rotation,width,height,outputPlan,refreshNoise);
        if(GpuQualification1961.exactRejected(key)) {
            for(int variant=0;variant<3;variant++)finishNote1986(variant,8,0,0,0,0,0,0);
            return;
        }
        final String policyIdentity1992=GpuFinishDefault1990.activeSuite1992();
        String legacyKey=legacyKey1976(key);
        long cpu=Long.MAX_VALUE,legacyFastest=Long.MAX_VALUE,legacyWorst=0,cpuChoiceWorst1992=0;
        long[] fastest1992={Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE};
        long[] gpu=new long[3];int[] exactTrials=new int[4],speedTrials=new int[4],reason=new int[4];
        if(GpuQualification1961.exactRejected(legacyKey))reason[0]=8;
        byte[] cpuReference=null;int reference=0;boolean cancelled1988=false,selected1988=false;
        try {
            for(int trial=0;trial<2;trial++) {
                finishCancelled1986(cancellation);
                // A second CPU image cannot prove a candidate which already
                // stopped. Keep the normal classification and cleanup below,
                // and retain both complete trials if even the legacy survives.
                if(trial>0&&finishPendingMask1988(reason)==0){finishOracle1989(false);break;}
                Bitmap expected=null,oracleInput=null;
                try {
                    finishCpu1988(1,reason);
                    long copyStarted1992=System.nanoTime();
                    oracleInput=owned.copy(Bitmap.Config.ARGB_8888,true);
                    long copyNanos1992=System.nanoTime()-copyStarted1992;
                    if(oracleInput==null){finishStop1986(reason,3);return;}
                    finishCpu1988(2,reason);
                    finishOracle1989(true);
                    long started=System.nanoTime();expected=cpuFinishOwned1962(oracleInput,rotation,width,height,outputPlan,refreshNoise);oracleInput=null;
                    long elapsed=System.nanoTime()-started;
                    finishCpu1988(3,reason);
                    finishCancelled1986(cancellation);
                    if(expected==null||expected.getWidth()!=width||expected.getHeight()!=height||elapsed<=0){finishStop1986(reason,10);return;}
                    cpu=Math.min(cpu,elapsed);
                    cpuChoiceWorst1992=Math.max(cpuChoiceWorst1992,elapsed+copyNanos1992);
                    // This bounded stability guard is outside the speed timer.
                    // It supplements, never replaces, either full pixel proof.
                    finishCpu1988(4,reason);
                    if(!GpuNoise1960.workspaceFits(16L*1024)){finishStop1986(reason,4);return;}
                    finishCpu1988(5,reason);
                    byte[] current=finishReference1986(expected,cancellation);
                    if(trial==0)cpuReference=current;
                    else if(!java.util.Arrays.equals(cpuReference,current)){finishStop1986(reason,10);return;}
                    for(int index=0;index<4;index++) {
                        finishCancelled1986(cancellation);if(reason[index]>1)continue;
                        int variant=index-1,result;
                        try {
                            finishBegin1988(variant,2);
                            result=compareFinish1988(owned,rotation,width,height,outputPlan,refreshNoise,expected,
                                index==0?32:32<<variant,index!=0,null,false,cancellation);
                            finishCancelled1986(cancellation);
                            if(result==ResidentProof1978.EXACT){exactTrials[index]++;reason[index]=1;finishStage1988(24);}
                            else if(result==ResidentProof1978.MISMATCH){reason[index]=2;if(index==0)GpuQualification1961.rejectExact(legacyKey);}
                            else if(result==FINISH_MEMORY1988)reason[index]=4;
                            else reason[index]=3;
                        } catch(java.util.concurrent.CancellationException cancelled){finishFault1988(4,0);throw cancelled;}
                          catch(OutOfMemoryError unavailable){finishFault1988(3,0);reason[index]=4;}
                          catch(RuntimeException unavailable){finishFault1988(1,0);finishCancelled1986(cancellation);reason[index]=3;}
                          catch(LinkageError unavailable){finishFault1988(2,0);reason[index]=3;}
                        finally {finishEnd1988();}
                        finishNote1986(variant,reason[index],exactTrials[index],speedTrials[index],0,cpu,legacyFastest,index==0?legacyWorst:gpu[variant]);
                    }
                } finally {if(oracleInput!=null&&!oracleInput.isRecycled())oracleInput.recycle();if(expected!=null&&expected!=owned)expected.recycle();}
                for(int index=0;index<4;index++) {
                    finishCancelled1986(cancellation);if(reason[index]!=1||exactTrials[index]!=trial+1)continue;
                    int variant=index-1;Bitmap candidate=null;
                    try {
                        finishBegin1988(variant,3);
                        // Input upload, workspace preparation, readback and the
                        // real destination Bitmap remain inside this timer.
                        long started=System.nanoTime();
                        candidate=index==0?runLegacy1976(owned,rotation,width,height,outputPlan,refreshNoise,null)
                            :runFinish1976(owned,rotation,width,height,outputPlan,refreshNoise,null,32<<variant);
                        long elapsed=System.nanoTime()-started;
                        finishCancelled1986(cancellation);
                        if(candidate==null||candidate.getWidth()!=width||candidate.getHeight()!=height||elapsed<=0)reason[index]=3;
                        else {finishStage1988(24);speedTrials[index]++;fastest1992[index]=Math.min(fastest1992[index],elapsed);if(index==0){legacyFastest=Math.min(legacyFastest,elapsed);legacyWorst=Math.max(legacyWorst,elapsed);}
                            else gpu[variant]=Math.max(gpu[variant],elapsed);}
                    } catch(java.util.concurrent.CancellationException cancelled){finishFault1988(4,0);throw cancelled;}
                      catch(OutOfMemoryError unavailable){finishFault1988(3,0);reason[index]=4;}
                      catch(DeferredChain unavailable){reason[index]=unavailable.reason1988;}
                      catch(RuntimeException unavailable){finishFault1988(1,0);finishCancelled1986(cancellation);reason[index]=3;}
                      catch(LinkageError unavailable){finishFault1988(2,0);reason[index]=3;}
                    finally {try{if(candidate!=null&&candidate!=owned)candidate.recycle();}finally{finishEnd1988();}}
                    finishNote1986(variant,reason[index],exactTrials[index],speedTrials[index],0,cpu,legacyFastest,index==0?legacyWorst:gpu[variant]);
                }
            }
            finishCancelled1986(cancellation);
            if(!policyIdentity1992.equals(GpuFinishDefault1990.activeSuite1992())){finishStop1986(reason,10);return;}
            if(reason[0]==1&&GpuQualification1961.exactRejected(legacyKey))reason[0]=8;
            boolean legacyExact=reason[0]==1&&exactTrials[0]==2&&speedTrials[0]==2;
            boolean[] exact=new boolean[3];for(int variant=0;variant<3;variant++)
                exact[variant]=reason[variant+1]==1&&exactTrials[variant+1]==2&&speedTrials[variant+1]==2;
            // CPU preference is a distinct route record. Its baseline/candidate
            // durations are actual GPU-fastest / CPU-worst complete calls; no
            // component GPU speed certificate or fictitious CPU time is made.
            if(cpuChoice1992(cpuChoiceWorst1992,fastest1992,exactTrials,speedTrials,reason)) {
                long fastest=Math.min(fastest1992[1],Math.min(fastest1992[2],fastest1992[3]));
                if(legacyExact)fastest=Math.min(fastest,fastest1992[0]);
                GpuQualification1961.qualified(cpuChoiceKey1992(key),fastest,cpuChoiceWorst1992,0);
            }
            reference=legacyExact?2:1;
            int best=chooseFinish1986(cpu,legacyFastest,gpu,exact,legacyExact);
            for(int variant=0;variant<3;variant++)if(exact[variant])reason[variant+1]=variant==best?7:
                win(cpu,gpu[variant])&&(!legacyExact||win(legacyFastest,gpu[variant]))?9:5;
            if(legacyExact)reason[0]=win(cpu,legacyWorst)?7:5;
            if(best>=0){selected1988=true;GpuQualification1961.qualified(key,legacyExact?Math.min(cpu,legacyFastest):cpu,gpu[best],best);}
            else {
                if(legacyExact&&win(cpu,legacyWorst))GpuQualification1961.qualified(legacyKey,cpu,legacyWorst,0);
                else if(legacyExact)GpuQualification1961.rejectMeasuredSpeed1992(legacyKey,"legacy_finish",cpu,legacyWorst);
            }
        } catch(java.util.concurrent.CancellationException cancelled){cancelled1988=true;finishFault1988(4,finishPendingMask1988(reason));finishStop1986(reason,6);throw cancelled;}
          catch(OutOfMemoryError unavailable){finishFault1988(3,finishPendingMask1988(reason));finishStop1986(reason,4);}
          catch(RuntimeException unavailable){
            try {finishCancelled1986(cancellation);}
            catch(java.util.concurrent.CancellationException cancelled){cancelled1988=true;finishFault1988(4,finishPendingMask1988(reason));finishStop1986(reason,6);throw cancelled;}
            finishFault1988(1,finishPendingMask1988(reason));
            finishStop1986(reason,3);
          }
          catch(LinkageError unavailable){finishFault1988(2,finishPendingMask1988(reason));finishStop1986(reason,3);}
        finally {
            try {
                // Transport, capacity and reference failures have the same
                // bounded retry policy, but cannot claim an unmeasured speed.
                if(!cancelled1988&&!selected1988&&!Thread.currentThread().isInterrupted()&&!cancellation.cancelled())
                    finishReject1992(key,reason,exactTrials,speedTrials,cpu,gpu,
                        exactTrials[0]==2&&speedTrials[0]==2&&(reason[0]==5||reason[0]==7)?legacyFastest:Long.MAX_VALUE);
            } finally {
                finishEnd1988();
                for(int index=0;index<4;index++)finishNote1986(index-1,reason[index],exactTrials[index],speedTrials[index],
                    index==0&&reference==2?1:reference,cpu,legacyFastest,index==0?legacyWorst:gpu[index-1]);
            }
        }
    }
    /** Only a missing/unusable old GPU reference selects the actual CPU route.
     * A valid old reference keeps the entire established dual-baseline gate. */
    static int chooseFinish1986(long cpu,long legacyFastest,long[] gpu,boolean[] exact,boolean legacyExact) {
        if(legacyExact)return chooseFinish1976(cpu,legacyFastest,gpu,exact,true);
        if(gpu==null||exact==null||gpu.length!=3||exact.length!=3)return -1;
        int best=-1;
        for(int variant=0;variant<3;variant++)if(exact[variant]&&win(cpu,gpu[variant])&&(best<0||gpu[variant]<gpu[best]))best=variant;
        return best;
    }
    private static void finishCancelled1986(GpuQualification1961.Cancellation cancellation) {
        checkCancelled();if(cancellation.cancelled())throw new java.util.concurrent.CancellationException("finish proof cancelled");
    }
    private static void finishStop1986(int[] reason,int value){for(int i=0;i<reason.length;i++)if(reason[i]<=1)reason[i]=value;}
    private static byte[] finishReference1986(Bitmap image,GpuQualification1961.Cancellation cancellation) {
        final java.security.MessageDigest digest;
        try {digest=java.security.MessageDigest.getInstance("SHA-256");}
        catch(java.security.NoSuchAlgorithmException unavailable){throw new IllegalStateException("finish reference unavailable",unavailable);}
        int[] pixels=new int[1024];byte[] bytes=new byte[4096];
        int width=image.getWidth(),height=image.getHeight();
        for(int y=0;y<height;y++)for(int x=0;x<width;x+=pixels.length) {
            finishCancelled1986(cancellation);int count=Math.min(pixels.length,width-x);
            image.getPixels(pixels,0,count,x,y,count,1);
            for(int i=0;i<count;i++){int p=pixels[i],at=i*4;bytes[at]=(byte)(p>>>24);bytes[at+1]=(byte)(p>>>16);bytes[at+2]=(byte)(p>>>8);bytes[at+3]=(byte)p;}
            digest.update(bytes,0,count*4);
        }
        finishCancelled1986(cancellation);return digest.digest();
    }
    private static void finishNote1986(int candidate,int reason,int exactTrials,int speedTrials,int reference,
            long cpu,long legacy,long gpu) {
        try {GpuQualification1961.finishCandidate1986(candidate,reason,exactTrials,speedTrials,reference,
            cpu<=0||cpu==Long.MAX_VALUE?0:cpu,legacy<=0||legacy==Long.MAX_VALUE?0:legacy,gpu<=0||gpu==Long.MAX_VALUE?0:gpu);}
        catch(Throwable optional){}
    }
    private static String legacyKey1976(String key){return key.replace("|finish-chain1976:v2|","|finish-chain1962:v1|");}
    private static Bitmap legacyFinish1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        ProcessingTiming1947.Trace selected1982=null;
        try{selected1982=PipelineDiagnostics1982.foreground(trace);}catch(Throwable optional){}
        String key=legacyKey1976(finishKey1976(source,rotation,width,height,plan,refresh));
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof==null||proof.variant!=0||GpuQualification1961.exactRejected(key))return null;
        try {
            long started=System.nanoTime();Bitmap result=runLegacy1976(source,rotation,width,height,plan,refresh,trace);
            if(result!=null){long elapsed1982=System.nanoTime()-started;if(!win(proof.cpuNanos,elapsed1982))GpuQualification1961.rejectMeasuredSpeed1992(key,"legacy_finish",proof.cpuNanos,elapsed1982);ProcessingTiming1947.detailRoute1976(trace,1);
                try{PipelineDiagnostics1982.selected(selected1982,2,1,1,elapsed1982);}catch(Throwable optional){}}
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
            (outputPlan.faceRegions!=null)+","+(outputPlan.smoothedRegions!=null)+","+(outputPlan.localNoise!=null)+",policy1992:"+GpuFinishPolicy1992.ready();
    }
    /** Benchmark the already deployed route, including full input transfer.
     * It may use only an existing finish certificate, never a new candidate. */
    static Bitmap benchmarkFinish1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan) {
        return benchmarkFinish1978(source,rotation,width,height,plan,true);
    }
    static Bitmap benchmarkFinish1978(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh) {
        String key=finishKey1976(source,rotation,width,height,plan,refresh);
        if(!GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(cpuChoiceKey1992(key))!=null)
            return cpuFinish(source,rotation,width,height,plan,refresh);
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2&&!GpuQualification1961.exactRejected(key)) {
            // A failed deployed route makes this trial incomplete. Measuring
            // a fallback under the old route identity would fabricate a whole
            // baseline and disagree with foreground/row-comparison behavior.
            return runFinish1976(source,rotation,width,height,plan,refresh,null,32<<proof.variant);
        }
        String legacyKey=legacyKey1976(key);proof=GpuQualification1961.restore(legacyKey);
        if(proof!=null&&proof.variant==0&&!GpuQualification1961.exactRejected(legacyKey))return runLegacy1976(source,rotation,width,height,plan,refresh,null);
        if(GpuFinishDefault1990.admissible1992(source,rotation,width,height,plan,key))
            return GpuFinishDefault1990.run(source,rotation,width,height,plan,refresh,null,key);
        return null;
    }
    static int compareBenchmark1978(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,Bitmap expected,
            GpuQualification1961.Cancellation cancellation) {
        String key=finishKey1976(source,rotation,width,height,plan,refresh);
        if(!GpuQualification1961.exactRejected(key)&&GpuQualification1961.restore(cpuChoiceKey1992(key))!=null) {
            if(!GpuNoise1960.workspaceFits(12L*source.getWidth()*source.getHeight()+12L*width*height))return ResidentProof1978.UNAVAILABLE;
            Bitmap actual=null;
            try {
                finishCancelled1986(cancellation);actual=cpuFinish(source,rotation,width,height,plan,refresh);
                finishCancelled1986(cancellation);
                return actual==null?ResidentProof1978.UNAVAILABLE:
                    GpuFinishDefault1990.difference(expected,actual)==null?ResidentProof1978.EXACT:ResidentProof1978.MISMATCH;
            } finally {if(actual!=null&&actual!=source)actual.recycle();}
        }
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2&&!GpuQualification1961.exactRejected(key))
            return compareFinish1978(source,rotation,width,height,plan,refresh,expected,32<<proof.variant,true,null,false,cancellation);
        String legacyKey=legacyKey1976(key);proof=GpuQualification1961.restore(legacyKey);
        if(proof!=null&&proof.variant==0&&!GpuQualification1961.exactRejected(legacyKey))
            return compareFinish1978(source,rotation,width,height,plan,refresh,expected,32,false,null,false,cancellation);
        if(GpuFinishDefault1990.admissible1992(source,rotation,width,height,plan,key))
            return compareFinish1978(source,rotation,width,height,plan,refresh,expected,32,true,null,false,cancellation);
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
        if(!GpuQualification1961.exactRejected(key)) {
            GpuQualification1961.Record cpu=GpuQualification1961.restore(cpuChoiceKey1992(key));
            if(cpu!=null)return "cpu1992:"+cpu.cpuNanos+":"+cpu.gpuNanos;
        }
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        if(proof!=null&&proof.variant>=0&&proof.variant<=2&&!GpuQualification1961.exactRejected(key))return "pipeline:"+proof.variant+":"+proof.cpuNanos+":"+proof.gpuNanos;
        // Corpus evidence admits the implementation domain, not photo speed.
        // Whole-route qualification below still compares every output pixel
        // twice and actual deployed baseline/candidate complete-call timings.
        String legacyKey=legacyKey1976(key);proof=GpuQualification1961.restore(legacyKey);
        if(proof!=null&&proof.variant==0&&!GpuQualification1961.exactRejected(legacyKey))return "legacy:"+proof.cpuNanos+":"+proof.gpuNanos;
        if(GpuFinishDefault1990.admissible1992(source,rotation,width,height,plan,key))return "standard:"+GpuFinishDefault1990.activeSuite1992();
        return null;
    }
    static int chooseFinish1976(long cpu,long legacyFastest,long[] gpu,boolean[] exact,boolean legacyExact) {
        if(!legacyExact||gpu==null||exact==null||gpu.length!=3||exact.length!=3)return -1;
        int best=-1;
        for(int variant=0;variant<3;variant++)if(exact[variant]&&win(cpu,gpu[variant])&&win(legacyFastest,gpu[variant])&&(best<0||gpu[variant]<gpu[best]))best=variant;
        return best;
    }
    static String cpuChoiceKey1992(String finishKey){return finishKey+"|cpu-choice1992:v1";}
    static boolean cpuChoice1992(long cpuWorst,long[] fastest,int[] exact,int[] speed,int[] reason) {
        if(cpuWorst<=0||fastest==null||exact==null||speed==null||reason==null||fastest.length!=4||exact.length!=4||speed.length!=4||reason.length!=4)return false;
        for(int i=1;i<4;i++)if(reason[i]!=1||exact[i]!=2||speed[i]!=2||fastest[i]==Long.MAX_VALUE||!win(fastest[i],cpuWorst))return false;
        return reason[0]!=1||exact[0]!=2||speed[0]!=2||win(fastest[0],cpuWorst);
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
        return cpuFinishTimed1992(corrected,rotation,width,height,outputPlan,refreshNoise,null);
    }
    private static Bitmap cpuFinishTimed1992(Bitmap corrected,int rotation,int width,int height,
            QualityPixels1932.Plan outputPlan,boolean refreshNoise,Timing1962 timing) {
        Bitmap geometry=null;boolean done=false;
        try {
            // Use the existing native-CPU/strip scheduler as the speed baseline,
            // rather than an artificially slow serial Java pixel loop.
            QualityPipeline1932.finishInPlace(corrected,null,true,false);
            geometry=FastResize1933.resampleCpu1960(corrected,rotation,width,height);
            QualityPixels1932.Plan finalPlan=outputPlan;
            if(refreshNoise) {
                final Bitmap image=geometry;
                if(timing!=null)timing.stage(ProcessingTiming1947.NOISE);
                try {finalPlan=outputPlan.withOutputNoise(SpatialNoise1934.probeCpu1961(new SpatialNoise1934.Patches(){
                    public void read(int[] pixels,int x,int y,int w,int h){image.getPixels(pixels,0,w,x,y,w,h);}
                },width,height));}
                finally {if(timing!=null)timing.stage(ProcessingTiming1947.CORRECTION);}
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
        try {Bitmap result=runFinishStages(source,rotation,width,height,outputPlan,refreshNoise,timing,tileRows,null,true);
            if(result!=null)detailRows1994(trace,tileRows);else finishSelection1995(trace,7,tileRows);return result;}
        catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
        catch(DeferredChain unavailable){if(unavailable.reason1988!=4)finishSelection1995(trace,7,tileRows);throw unavailable;}
        catch(RuntimeException unavailable){finishSelection1995(trace,7,tileRows);throw unavailable;}
        catch(LinkageError unavailable){finishSelection1995(trace,7,tileRows);throw unavailable;}
        catch(OutOfMemoryError unavailable){finishSelection1995(trace,7,tileRows);throw unavailable;}
        finally {timing.close();}
    }
    /** The standard startup corpus independently proves both schedules. Larger
     * bands cut Java/JNI/fence round trips; an actual memory refusal retains the
     * original bounded 32-row schedule, without relaxing any capacity check. */
    static Bitmap runDefault1993(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        if(!GpuFinishDefault1990.wideReady1993()){
            finishSelection1995(trace,GpuCertificates1990.rejected(GpuFinishDefault1990.wideSuite1993())?3:2,32);
            return runFinish1976(source,rotation,width,height,plan,refresh,trace,32);
        }
        finishSelection1995(trace,1,128);
        try{return runFinish1976(source,rotation,width,height,plan,refresh,trace,128);}
        catch(DeferredChain refused){
            if(refused.reason1988!=4)throw refused;
            finishSelection1995(trace,4,32);
            return runFinish1976(source,rotation,width,height,plan,refresh,trace,32);
        }
    }
    static Bitmap runLegacy1976(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        Timing1962 timing=new Timing1962(trace);
        finishSelection1995(trace,6,32);
        try {Bitmap result=runFinishStages(source,rotation,width,height,plan,refresh,timing,32,null,false);
            if(result!=null)detailRows1994(trace,32);return result;}
        finally {timing.close();}
    }
    static Bitmap runResident1976(Bitmap source,GpuNoise1960.Session session,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,ProcessingTiming1947.Trace trace) {
        if(session==null)return null;Timing1962 timing=new Timing1962(trace);
        ProcessingTiming1947.Trace selected1982=null;
        try{selected1982=PipelineDiagnostics1982.foreground(trace);}catch(Throwable optional){}
        long started1982=selected1982==null?0:System.nanoTime();Bitmap result;
        try {result=runFinishStages(source,rotation,width,height,plan,refresh,timing,64,session,true);}
        finally {timing.close();session.close();}
        if(result!=null){detailRows1994(trace,64);if(selected1982!=null)try{PipelineDiagnostics1982.selected(selected1982,2,1,1,System.nanoTime()-started1982);}catch(Throwable optional){}}
        return result;
    }
    static Bitmap runResident1978(Bitmap source,GpuNoise1960.Session session,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean identity,ProcessingTiming1947.Trace trace) {
        if(session==null)return null;Timing1962 timing=new Timing1962(trace);
        ProcessingTiming1947.Trace selected1982=null;
        try{selected1982=PipelineDiagnostics1982.foreground(trace);}catch(Throwable optional){}
        long started1982=selected1982==null?0:System.nanoTime();Bitmap result;
        try {result=runFinishStages(source,rotation,width,height,plan,!identity,timing,64,session,true,null,identity);}
        finally {timing.close();session.close();}
        if(result!=null){detailRows1994(trace,64);if(selected1982!=null)try{PipelineDiagnostics1982.selected(selected1982,2,1,1,System.nanoTime()-started1982);}catch(Throwable optional){}}
        return result;
    }
    static int compareFinish1978(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,Bitmap expected,int tileRows,boolean pipeline,
            GpuNoise1960.Session carried,boolean identityNoiseBefore,
            GpuQualification1961.Cancellation cancellation) {
        int result=compareFinish1988(source,rotation,width,height,plan,refresh,expected,tileRows,pipeline,carried,identityNoiseBefore,cancellation);
        return result==FINISH_MEMORY1988?ResidentProof1978.UNAVAILABLE:result;
    }
    /** Qualification alone distinguishes an explicit workspace refusal. Other
     * callers retain the established three-result ResidentProof contract. */
    static int compareFinish1988(Bitmap source,int rotation,int width,int height,
            QualityPixels1932.Plan plan,boolean refresh,Bitmap expected,int tileRows,boolean pipeline,
            GpuNoise1960.Session carried,boolean identityNoiseBefore,
            GpuQualification1961.Cancellation cancellation) {
        Timing1962 timing=new Timing1962(null);
        try {
            finishStage1988(6);
            if(expected==null||expected.isRecycled()||expected.getWidth()!=width||expected.getHeight()!=height)return ResidentProof1978.UNAVAILABLE;
            finishStage1988(26);
            ResidentProof1978 comparison=new ResidentProof1978(expected,cancellation);
            runFinishStages(source,rotation,width,height,plan,refresh,timing,tileRows,carried,pipeline,comparison,identityNoiseBefore);
            return comparison.result();
        } catch(DeferredChain unavailable){return unavailable.reason1988==4?FINISH_MEMORY1988:ResidentProof1978.UNAVAILABLE;}
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
        finishStage1988(6);
        int sw=source.getWidth(),sh=source.getHeight(),n=Math.multiplyExact(sw,sh),out=Math.multiplyExact(width,height);
        boolean identity=rotation==0&&sw==width&&sh==height;
        if(identityNoiseBefore&&(!identity||carried==null||refreshNoise))throw new IllegalArgumentException("resident identity evidence position");
        Preflight1981 planned=PREFLIGHT1981.get();
        if(planned!=null&&carried!=null&&comparison==null){
            if(!planned.matches(source,rotation,width,height))throw new DeferredChain();
            planned.finishAdmission(carried);
        }else planned=null;
        if(carried!=null){
            checkCancelled();
            // Strong has joined every producer and transferred only slot24.
            // Model and strip banks are dead; retain no former large capacity
            // in a slot subsequently reused for a small Finish lookup buffer.
            if(!carried.releaseSlots1991(new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23}))
                return null;
        }
        finishStage1988(7);
        GpuPolicy1960.GeometryData data=identity?null:planned==null?GpuPolicy1960.geometry(sw,sh,rotation,width,height):planned.geometry;
        long uploadBytes1990=carried==null?4L*GpuSource1990.capacity(sw,sh):0;
        // Source, moire and final Bitmap do not coexist: each producer is
        // fenced and released at its last use below. Charge the maximum real
        // phase, not the sum of two full images with disjoint lifetimes.
        long tables=data==null?0:4L*(data.tables.length+data.weights.length);
        long geometryPeak=data==null?0:data.exactCrop?4:geometryPeak1981(data,width,height,planned==null?tileRows:planned.geometryRows);
        long extra=finishPeak1991(n,out,width,tileRows,carried==null?GpuSource1990.workspaceBytes(sw,sh):0,
            tables,geometryPeak,data!=null,comparison==null);
        // A transferred slot24 is already included in freeBudget's native
        // retained bytes. Subtract only its logical size (not rounded capacity)
        // from the phase peak to avoid charging the existing owner twice.
        if(carried!=null)extra=Math.max(0,extra-4L*n);
        if(comparison!=null)extra+=comparison.workspaceBytes();
        finishStage1988(8);
        boolean fits1997;
        if(GpuQualification1961.background())fits1997=FinishMemory1989.fits(extra);
        else {
            fits1997=GpuNoise1960.workspaceFits(extra);
            if(!fits1997) {
                if(FINISH_PRESSURE1997.get()!=null)FINISH_PRESSURE1997.set(Boolean.TRUE);
                long[] memory1997=null;
                try{memory1997=new long[FinishMemory1989.COUNT];}catch(OutOfMemoryError optional){}
                fits1997=FinishMemory1989.recheckForeground1997(extra,memory1997);
                try{ProcessingTiming1947.finishMemory1997(timing==null?null:timing.trace,memory1997);}catch(Throwable optional){}
            }
        }
        if(!fits1997)throw new DeferredChain(4);
        finishStage1988(9);
        finishStage1988(10);
        GpuNoise1960.Session session=carried==null?GpuNoise1960.open():carried;if(session==null)throw new DeferredChain();
        Bitmap result=null;boolean done=false;GpuNoise1960.Lease1971 imageLease=null;
        try {
            // Geometry executes bounded bands, so reserving its complete
            // rotated image would incorrectly exclude large otherwise-safe
            // photographs. Per-band capacity is reserved before each dispatch.
            int[] slots=new int[]{carried==null?0:24,1,3,5};
            long[] capacities=new long[]{4L*n,4L*n,4,4};
            long[] uploads=new long[]{uploadBytes1990,0,0,0};
            finishStage1988(11);
            imageLease=session.reserveCapacity1983(slots,capacities,uploads,uploadBytes1990);
            if(imageLease==null||!imageLease.revalidate1971())throw new DeferredChain();
            int[] finish=new int[32];finish[1]=sw;finish[2]=sh;finish[5]=sh;finish[8]=1;
            int sourceSlot=carried==null?0:24;
            QualityPixels1932.Plan finalPlan=outputPlan;
            if(identityNoiseBefore) {
                // The unchanged identity CPU route measures residual noise after
                // Strong and before moire. Never move that observation past moire.
                timing.stage(ProcessingTiming1947.NOISE);
                try {
                    finishStage1988(16);
                    SpatialNoise1934 evidence=GpuAnalysis1961.residentSpatial1962(session,sourceSlot,sw,sh);
                    if(evidence==null)return null;finalPlan=outputPlan.withOutputNoise(evidence);
                } finally {timing.stage(ProcessingTiming1947.CORRECTION);}
            }
            finishStage1988(12);
            if(carried==null) {
                long started1992=System.nanoTime();
                try {if(!uploadSource1990(source,session,0))return null;}
                finally {finishWork1992(timing.trace,ProcessingTiming1947.FINISH_SOURCE_UPLOAD1992,started1992,4L*n);}
            }
            GpuNoise1960.Batch b=new GpuNoise1960.Batch();
            b.allocate(1,4L*n).allocate(3,4).allocate(5,4)
                .dispatch(GpuNoise1960.FINISH1961,new int[]{sourceSlot,sourceSlot,1,3},finish,null,n);
            if(!session.run(b))return null;
            checkCancelled();
            imageLease.close();imageLease=null;
            // Moire no longer reads sourceSlot. Native release fences this
            // completed dispatch before deleting storage and reconciling debt.
            if(!session.releaseSlots1991(new int[]{sourceSlot}))return null;
            int imageSlot=identity?1:2;
            if(!identity) {
                imageLease=session.reserveCapacity1983(new int[]{1,2,3,5,6,7},
                    new long[]{4L*n,4L*out,4,4,4L*data.weights.length,4L*data.tables.length},
                    new long[]{0,0,0,0,4L*data.weights.length,4L*data.tables.length},0);
                if(imageLease==null||!imageLease.revalidate1971())throw new DeferredChain();
                if(!session.run(new GpuNoise1960.Batch().allocate(2,4L*out)
                    .uploadDirect(6,data.weights).uploadDirect(7,data.tables)))return null;
                int[] base=data.uniforms(0),bindings={1,2,4,5,6,7};
                for(int first=0;first<height;) {
                    finishStage1988(13);
                    checkCancelled();int rows=Math.min(planned==null?tileRows:planned.geometryRows,height-first),lo,hi;
                    for(;;) {
                        lo=Integer.MAX_VALUE;hi=-1;
                        if(data.exactCrop){lo=first+base[7];hi=lo+rows;}
                        else {for(int y=first;y<first+rows;y++)for(int t=data.tables[base[10]+y];t<data.tables[base[10]+y+1];t++)
                            {int row=data.tables[base[11]+t];lo=Math.min(lo,row);hi=Math.max(hi,row);}hi++;}
                        long intermediate=data.exactCrop?4:12L*width*(hi-lo);
                        if(lo<0||hi<=lo||hi>base[14])return null;
                        if(intermediate<=GpuNoise1960.MAX_BYTES&&GpuNoise1960.workspaceFits(intermediate+65536L))break;
                        if(rows==1)throw new DeferredChain(4);rows=Math.max(1,rows/2);
                    }
                    int[] u=base.clone();u[15]=first;u[16]=first+rows;u[17]=lo;u[18]=hi-lo;u[20]=0;u[21]=1;
                    b=new GpuNoise1960.Batch().allocate(4,data.exactCrop?4:12L*width*(hi-lo));
                    if(data.exactCrop)b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*rows);
                    else {u[0]=1;b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*(hi-lo));u[0]=2;b.dispatch(GpuNoise1960.GEOMETRY,bindings,u,null,width*rows);}
                    finishStage1988(14);
                    GpuNoise1960.Lease1971 geometryLease=session.reserveCapacity1983(new int[]{4},new long[]{data.exactCrop?4:12L*width*(hi-lo)},new long[]{0},0);
                    if(geometryLease==null)throw new DeferredChain();
                    try {if(!geometryLease.revalidate1971())return null;finishStage1988(15);
                        long geometryStarted1994=System.nanoTime();if(!session.run(b))return null;
                        if(timing.trace!=null)try{ProcessingTiming1947.geometryBand1994(timing.trace,rows,System.nanoTime()-geometryStarted1994);}catch(Throwable optional){}
                    }
                    finally {geometryLease.close();}
                    first+=rows;
                }
                imageLease.close();imageLease=null;
                // Geometry and its lookup buffers have no consumers after the
                // last output row; only imageSlot survives into output policy.
                if(!session.releaseSlots1991(new int[]{1,4,6,7}))return null;
            }
            if(refreshNoise) {
                timing.stage(ProcessingTiming1947.NOISE);
                try {
                    finishStage1988(16);
                    SpatialNoise1934 evidence=GpuAnalysis1961.residentSpatial1962(session,imageSlot,width,height);
                    if(evidence==null)return null;finalPlan=outputPlan.withOutputNoise(evidence);
                } finally {timing.stage(ProcessingTiming1947.CORRECTION);}
            }
            finishStage1988(17);
            long prepareStarted1992=System.nanoTime();
            try {result=comparison==null?Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888):null;}
            finally {finishWork1992(timing.trace,ProcessingTiming1947.FINISH_PREPARATION1992,prepareStarted1992,comparison==null?4L*out:0);}
            if(imageLease!=null){imageLease.close();imageLease=null;}
            if(!(pipeline?finishBands1992(session,imageSlot,width,height,finalPlan,result,tileRows,comparison,timing.trace):finishBandsLegacy1978(session,imageSlot,width,height,finalPlan,result,comparison)))return null;
            if(comparison!=null){done=true;return source;}
            result.setDensity(source.getDensity());result.setHasAlpha(source.hasAlpha());done=true;return result;
        } finally {if(imageLease!=null)imageLease.close();session.close();if(!done&&result!=null)result.recycle();}
    }
    /** Upper bound of simultaneous additions after source/moire last-use
     * release. Native session leases still revalidate actual allocation debt;
     * this forecast never enlarges MAX_BYTES or the 64 MiB process reserve. */
    static long finishPeak1991(long sourcePixels,long outputPixels,int width,int tileRows,
            long sourceStaging,long tableBytes,long geometryBytes,boolean geometry,boolean destination) {
        if(sourcePixels<1||outputPixels<1||width<1||tileRows<1||sourceStaging<0||tableBytes<0||geometryBytes<0)
            throw new IllegalArgumentException("finish peak shape");
        long moire=8L*sourcePixels+sourceStaging;
        long resample=geometry?4L*sourcePixels+4L*outputPixels+geometryBytes+2L*tableBytes:0;
        long finalImage=(destination?8L:4L)*outputPixels+60L*width*tileRows;
        return Math.max(moire,Math.max(resample,finalImage))+tableBytes+16L*1024*1024;
    }
    /** Preserved .75 single-bank 32-row finish schedule. This is both the
     * real speed baseline and the fallback when pipelining is not faster. */
    private static boolean finishBandsLegacy1976(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result) {
        return finishBandsLegacy1978(session,imageSlot,width,height,plan,result,null);
    }
    private static boolean finishBandsLegacy1978(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result,ResidentProof1978 comparison) {
        finishStage1988(26);
        int[] readPixels=new int[Math.multiplyExact(width,Math.min(32,height))];
        for(int first=0;first<height;first+=32) {
            checkCancelled();int last=Math.min(height,first+32),count=width*(last-first);
            finishStage1988(18);
            FinishPolicy1953.Band policy=FinishPolicy1953.prepareParallel1978(plan,width,height,first,last,0);
            if(policy==null)return false;
            try {
                int[] u=new int[32];u[1]=width;u[2]=height;u[4]=first;u[5]=last;u[6]=first;u[7]=first;
                u[9]=1;u[10]=plan.sharpGainQ8;u[11]=plan.sharpFloorQ8;u[12]=plan.sharpLimit;
                u[13]=plan.texturePriority?1:0;u[14]=plan.haloSuppression?1:0;u[16]=policy.mode;
                finishStage1988(27);
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
        return finishBands1992(session,imageSlot,width,height,plan,result,tileRows,comparison,null);
    }
    private static boolean finishBands1992(GpuNoise1960.Session session,int imageSlot,int width,int height,
            QualityPixels1932.Plan plan,Bitmap result,int tileRows,ResidentProof1978 comparison,ProcessingTiming1947.Trace trace) {
        final int capacity=Math.multiplyExact(width,Math.min(tileRows,height));
        final int[] policySlots={3,8},outputSlots={17,18};
        GpuNoise1960.Lease1971 lease=null;
        GpuFinishPolicy1992.Context gpuPolicy1992=null;
        try {
            long openStarted1992=System.nanoTime();
            try {if(GpuFinishPolicy1992.ready())gpuPolicy1992=GpuFinishPolicy1992.open(session,plan,width,height,tileRows);}
            finally {finishWork1992(trace,ProcessingTiming1947.FINISH_METADATA_UPLOAD1992,openStarted1992,gpuPolicy1992==null?0:gpuPolicy1992.metadataUploadBytes);}
            finishStage1988(19);
            // Resident policy words are generated in their final native banks;
            // only CPU fallback needs Java-to-native policy upload capacity.
            long policyUpload1992=gpuPolicy1992==null?16L*capacity:0;
            lease=session.reserveCapacity1983(new int[]{3,8,17,18},
                new long[]{16L*capacity,16L*capacity,4L*capacity,4L*capacity},
                new long[]{policyUpload1992,policyUpload1992,0,0},8L*capacity+512L);
            if(lease==null)throw new DeferredChain();
            finishStage1988(26);
            int[][] read={new int[capacity],new int[capacity]};lease.consumeJava1974(8L*capacity);
            GpuNoise1960.Ticket[] pending=new GpuNoise1960.Ticket[2];
            int[] firsts=new int[2],counts=new int[2];int sequence=0;
            for(int first=0;first<height;first+=tileRows,sequence++) {
                checkCancelled();int bank=sequence&1;
                if(pending[bank]!=null) {
                    if(!collectBand1992(session,pending[bank],outputSlots[bank],counts[bank],read[bank],result,width,firsts[bank],comparison,trace))return false;
                    pending[bank]=null;
                }
                int last=Math.min(height,first+tileRows),count=width*(last-first);
                finishStage1988(18);
                FinishPolicy1953.Band policy=null;GpuFinishPolicy1992.Band residentPolicy1992=null;
                try {
                    if(gpuPolicy1992!=null) {
                        residentPolicy1992=gpuPolicy1992.prepare(first,last,bank);
                        try {ProcessingTiming1947.finishWork1992(trace,ProcessingTiming1947.FINISH_CPU_MASK_PACK1992,residentPolicy1992.cpuNanos,residentPolicy1992.uploadBytes);}catch(Throwable optional){}
                    } else {
                        long policyStarted1992=System.nanoTime();
                        try {policy=FinishPolicy1953.prepareParallel1978(plan,width,height,first,last,0);}
                        finally {finishWork1992(trace,ProcessingTiming1947.FINISH_CPU_POLICY1992,policyStarted1992,policy==null?0:4L*policy.words.length);}
                        if(policy==null)return false;
                    }
                    finishStage1988(25);
                    if(policy!=null&&4L*policy.words.length>16L*capacity||!lease.revalidate1971())throw new DeferredChain();
                    int[] u=new int[32];u[1]=width;u[2]=height;u[4]=first;u[5]=last;u[6]=first;u[7]=first;
                    u[9]=1;u[10]=plan.sharpGainQ8;u[11]=plan.sharpFloorQ8;u[12]=plan.sharpLimit;
                    u[13]=plan.texturePriority?1:0;u[14]=plan.haloSuppression?1:0;u[16]=residentPolicy1992==null?policy.mode:residentPolicy1992.mode;
                    finishStage1988(20);
                    long submitStarted1992=System.nanoTime();
                    long uploaded1992=residentPolicy1992==null?4L*policy.words.length:residentPolicy1992.uploadBytes;
                    try {
                        GpuNoise1960.Batch batch=new GpuNoise1960.Batch();
                        if(residentPolicy1992==null)batch.uploadDirect(policySlots[bank],policy.words);
                        else {
                            residentPolicy1992.append(batch,policySlots[bank]);
                            // The combined submission/fence span below is the
                            // observable API time; no GPU kernel timer exists.
                            try {ProcessingTiming1947.finishWork1992(trace,ProcessingTiming1947.FINISH_GPU_POLICY1992,-1,16L*count);}catch(Throwable optional){}
                        }
                        batch.allocate(outputSlots[bank],4L*count)
                            .dispatch(GpuNoise1960.FINISH1961,new int[]{imageSlot,imageSlot,outputSlots[bank],policySlots[bank]},u,null,count);
                        pending[bank]=session.submit(batch,bank);if(pending[bank]==null)return false;
                    } finally {finishWork1992(trace,ProcessingTiming1947.FINISH_TRANSFER_SUBMIT1992,submitStarted1992,uploaded1992);}
                    firsts[bank]=first;counts[bank]=count;
                } finally {if(policy!=null)policy.close();if(residentPolicy1992!=null)residentPolicy1992.close();}
            }
            // Bank order need not be row order: each private output row has one owner.
            for(int bank=0;bank<2;bank++)if(pending[bank]!=null&&
                !collectBand1992(session,pending[bank],outputSlots[bank],counts[bank],read[bank],result,width,firsts[bank],comparison,trace))return false;
            return true;
        } finally {
            // Pending native commands retain their actual allocations until
            // Session.close drains/quarantines them; closing a capacity lease
            // never frees buffers or pretends that pending tickets completed.
            if(gpuPolicy1992!=null)gpuPolicy1992.close();if(lease!=null)lease.close();
        }
    }
    private static boolean collectBand1976(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int slot,int count,
            int[] pixels,Bitmap result,int width,int first) {
        return collectBand1978(session,ticket,slot,count,pixels,result,width,first,null);
    }
    private static boolean collectBand1978(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int slot,int count,
            int[] pixels,Bitmap result,int width,int first,ResidentProof1978 comparison) {
        return collectBand1992(session,ticket,slot,count,pixels,result,width,first,comparison,null);
    }
    private static boolean collectBand1992(GpuNoise1960.Session session,GpuNoise1960.Ticket ticket,int slot,int count,
            int[] pixels,Bitmap result,int width,int first,ResidentProof1978 comparison,ProcessingTiming1947.Trace trace) {
        finishStage1988(21);
        long started1992=System.nanoTime();
        try {if(!session.collectInto(ticket,slot,count,pixels,0))return false;}
        finally {finishWork1992(trace,ProcessingTiming1947.FINISH_READBACK1992,started1992,4L*count);}
        checkCancelled();started1992=System.nanoTime();
        try {return finishRows1978(result,comparison,pixels,width,first,count/width);}
        finally {if(comparison==null)finishWork1992(trace,ProcessingTiming1947.FINISH_BITMAP_WRITE1992,started1992,4L*count);}
    }
    private static boolean finishRows1978(Bitmap result,ResidentProof1978 comparison,int[] pixels,int width,int first,int rows) {
        finishStage1988(comparison!=null?22:23);
        if(comparison!=null)return comparison.rows(first,rows,pixels,0);
        result.setPixels(pixels,0,width,0,first,width,rows);return true;
    }
    private static void finishWork1992(ProcessingTiming1947.Trace trace,int kind,long started,long bytes) {
        if(trace==null)return;
        try {ProcessingTiming1947.finishWork1992(trace,kind,System.nanoTime()-started,bytes);}catch(Throwable optional){}
    }
    private static void finishSelection1995(ProcessingTiming1947.Trace trace,int reason,int rows){
        if(trace!=null)try{ProcessingTiming1947.finishSelection1995(trace,reason,rows);}catch(Throwable optional){}
    }
    private static void detailRows1994(ProcessingTiming1947.Trace trace,int rows){
        if(trace!=null)try{ProcessingTiming1947.detailRows1994(trace,rows);}catch(Throwable optional){}
    }
    private static void checkCancelled(){GpuQualification1961.Cancellation resident=GpuResident1976.cancellation();if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled()||resident!=null&&resident.cancelled())throw new java.util.concurrent.CancellationException("finish chain cancelled");}
    private static final class DeferredChain extends RuntimeException {
        final int reason1988;
        private DeferredChain(){reason1988=3;}
        DeferredChain(int reason){reason1988=reason==4?4:3;}
    }
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
        long extra=8L*n+GpuSource1990.workspaceBytes(sw,sh)+4L*out+4L*(data.tables.length+data.weights.length)+
            16L*width*64+65536L;
        if(!GpuNoise1960.workspaceFits(extra))throw new DeferredChain();

        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new DeferredChain();
        try {
            int[] finish=new int[32];finish[1]=sw;finish[2]=sh;finish[5]=sh;finish[8]=1;
            if(!uploadSource1990(source,session,0))return null;
            GpuNoise1960.Batch batch=new GpuNoise1960.Batch().allocate(1,4L*n)
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
    /** One full resident SSBO, bounded Java/native staging. Partial uploads
     * are private to this session and discarded on alpha/fault/cancellation. */
    private static boolean uploadSource1990(final Bitmap source,final GpuNoise1960.Session session,final int slot) {
        final int width=source.getWidth(),height=source.getHeight();
        if(!session.allocate(slot,4L*width*height))return false;
        return GpuSource1990.upload(width,height,new GpuSource1990.Source(){
            public void read(int[] pixels,int first,int rows){source.getPixels(pixels,0,width,0,first,width,rows);}
        },new GpuSource1990.Sink(){
            public boolean write(int firstPixel,int[] pixels,int count){return session.uploadRange1976(slot,firstPixel,pixels,0,count);}
        },new GpuSource1990.Check(){public void check(){checkCancelled();}});
    }
    static final int FINISH_MEMORY1988= -2;
    /** This classification consumes only execution facts, never optional
     * telemetry. A missing output/time cannot be a measured speed failure. */
    static void finishReject1988(String key,int[] reason,int[] exact,int[] speed,long cpu,long[] gpu){
        finishReject1992(key,reason,exact,speed,cpu,gpu,Long.MAX_VALUE);
    }
    private static void finishReject1992(String key,int[] reason,int[] exact,int[] speed,long cpu,long[] gpu,long legacy){
        boolean slow=false,mismatch=false,cached=false;int failure=0;
        for(int index=1;index<4;index++){
            int value=reason[index];
            if(value==5&&exact[index]==2&&speed[index]==2&&cpu>0&&cpu!=Long.MAX_VALUE&&gpu[index-1]>0)slow=true;
            else if(value==2)mismatch=true;
            else if(value==8)cached=true;
            else {
                int cause=value==3?3:value==4?4:value==10?10:11;
                // An unstable whole CPU oracle invalidates every speed claim.
                if(cause==10||failure==10)failure=10;
                else failure=failure==0||failure==cause?cause:11;
            }
        }
        if(failure==0&&slow&&!mismatch&&!cached){
            long best=Math.min(gpu[0],Math.min(gpu[1],gpu[2]));
            if(legacy>0&&legacy<Long.MAX_VALUE) {
                long reference=Math.min(cpu,legacy);
                GpuQualification1961.rejectMeasuredThreshold1992(key,"finish_best",legacy<cpu?"legacy_finish":"CPU",reference,best,reference-reference/20);
            } else if(!win(cpu,best))GpuQualification1961.rejectMeasuredSpeed1992(key,"finish_best",cpu,best);
            else GpuQualification1961.rejectNonSpeed1988(key,"comparison_incomplete");
            return;
        }
        String cause=failure==10?"reference_unstable":failure==0?(slow?"comparison_incomplete":mismatch?"quality_mismatch":cached?"quality_rejected":"comparison_incomplete"):
            slow||mismatch||cached?"comparison_incomplete":failure==3?"execution_unavailable":failure==4?"memory_budget":"comparison_incomplete";
        GpuQualification1961.rejectNonSpeed1988(key,cause);
    }
    static int finishPendingMask1988(int[] reason){int mask=0;for(int i=0;i<4;i++)if(reason[i]<=1)mask|=1<<i;return mask;}
    static void finishCpu1988(int stage,int[] reason){try{GpuQualification1961.finishCpu1988(stage,finishPendingMask1988(reason));}catch(Throwable optional){}}
    static void finishBegin1988(int candidate,int phase){try{GpuQualification1961.finishBegin1988(candidate,phase);}catch(Throwable optional){}}
    static void finishStage1988(int stage){try{GpuQualification1961.finishStage1988(stage);}catch(Throwable optional){}}
    static void finishFault1988(int fault,int mask){try{GpuQualification1961.finishFault1988(fault,mask);}catch(Throwable optional){}}
    static void finishEnd1988(){try{GpuQualification1961.finishEnd1988();}catch(Throwable optional){}}
    static void finishShape1989(Bitmap source,int rotation,int width,int height){
        try{GpuQualification1961.finishShape1989(source.getWidth(),source.getHeight(),width,height,rotation);}catch(Throwable optional){}
    }
    static void finishOracle1989(boolean executed){try{GpuQualification1961.finishOracle1989(executed);}catch(Throwable optional){}}
}
