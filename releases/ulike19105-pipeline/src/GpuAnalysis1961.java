package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** GX11/GX20: original sampled evidence and a resident coarse model graph.
 * Foreground unknown configurations use the original CPU algorithm. Detached,
 * idle-only proofs compare every value twice before a GPU configuration is used.
 * No photograph, detector result or pooled foreground workspace is retained. */
public final class GpuAnalysis1961 {
    private GpuAnalysis1961() {}
    private static boolean rejected(String key){return !GpuQualification1961.maySchedule(key);}
    private static void reject(String key){GpuQualification1961.rejectExact(key);}
    private static void slow(String key){GpuQualification1961.rejectSpeed(key);}
    private static void cancelled(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU analysis cancelled");}
    private static boolean win(long cpu,long gpu){return cpu>0&&gpu>0&&gpu<=cpu-cpu/20;}
    private static final class Deferred extends RuntimeException {
        final String reason;
        private Deferred(){this("comparison_incomplete");}
        Deferred(String reason){this.reason=reason;}
    }
    /** A source/CPU failure is not a failed GPU candidate and must not launch
     * another GPU variant. The original exception is restored at admission. */
    private static final class RuntimePreparationFailure1996 extends RuntimeException {
        final Throwable original;
        RuntimePreparationFailure1996(Throwable failure){super(failure);original=failure;}
        void rethrow(){if(original instanceof Error)throw (Error)original;throw (RuntimeException)original;}
    }
    private static int ceil2(int x){return x/2+(x&1);}
    private static float[] floats(int[] bits){float[] result=new float[bits.length];for(int i=0;i<bits.length;i++)result[i]=Float.intBitsToFloat(bits[i]);return result;}
    private static boolean equal(float[] a,float[] b){if(a==null||b==null||a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(Float.floatToRawIntBits(a[i])!=Float.floatToRawIntBits(b[i]))return false;return true;}
    private static ProcessingTiming1947.Trace trace1988(){
        try{return PipelineDetail1988.current();}catch(Throwable optional){return null;}
    }
    private static void record1988(ProcessingTiming1947.Trace trace,int phase,int backend,int reason,long nanos){
        if(trace!=null)try{PipelineDetail1988.record(trace,phase,backend,reason,1,nanos);}catch(Throwable optional){}
    }
    private static void copy1988(ProcessingTiming1947.Trace trace,int phase,int outcome,long bytes,long nanos){
        if(trace!=null)try{PipelineDetail1988.copy(trace,phase,outcome,bytes,nanos);}catch(Throwable optional){}
    }
    /** The existing event remains intact. No permission/provider is consulted
     * for this additional scalar observation of the decision already returned. */
    private static void copy1989(ProcessingTiming1947.Trace trace,int phase,int outcome,long legacyBytes,
            long requestedBytes,long acquiredPixelBytes,long nanos,GpuQualification1961.QueueDecision1983 decision){
        copy1988(trace,phase,outcome,legacyBytes,nanos);
        if(trace!=null)try{PipelineDetail1988.copyState1989(trace,phase,outcome,requestedBytes,acquiredPixelBytes,nanos,
            decision==null?-1:decision.reason,decision==null?-1:decision.retryRemainingNanos,decision==null?-1:decision.retries,
            decision==null?-1:decision.queuedJobs,decision==null?-1:decision.runningJobs,decision==null?-1:decision.retainedBytes);
        }catch(Throwable optional){}
    }
    private static long pixelBytes1989(Snapshot snapshot){
        try{return snapshot==null||snapshot.pixels==null?-1:4L*snapshot.pixels.length;}catch(Throwable optional){return -1;}
    }
    private static long pixelBytes1989(Capture1987 capture){
        try{return capture==null||!capture.valid||capture.cursor<0||capture.cursor>capture.shape.count?
            -1:4L*capture.cursor*capture.shape.pw*capture.shape.ph;}catch(Throwable optional){return -1;}
    }
    private static void reason1988(int[] selected,int reason){
        if(selected!=null&&selected.length>1)selected[1]=reason;
    }
    private static final class Snapshot {
        int[] pixels;
        final int[] xs,ys;final int pw,ph,count;final long gatherNanos;
        Snapshot(int[] p,int[] x,int[] y,int w,int h,long ns){pixels=p;xs=x;ys=y;pw=w;ph=h;count=x.length;gatherNanos=ns;}
        Reader reader(){return new Reader(this);}
        long bytes(){return 4L*(pixels.length+xs.length+ys.length)+256;}
        void close(){pixels=null;}
    }
    private static final class Reader implements SpatialNoise1934.Patches,StrongNoise1958.Patches {
        final Snapshot snapshot;int cursor;
        Reader(Snapshot s){snapshot=s;}
        public void read(int[] dst,int x,int y,int w,int h){
            if(cursor>=snapshot.count||w!=snapshot.pw||h!=snapshot.ph||x!=snapshot.xs[cursor]||y!=snapshot.ys[cursor])throw new IllegalArgumentException("GPU detached patch traversal");
            System.arraycopy(snapshot.pixels,cursor*w*h,dst,0,w*h);cursor++;
        }
    }
    /** The CPU traversal is the only read of a cold source. A reserved optional
     * snapshot observes those same patches; it never asks a caller to read them
     * again and never reaches the idle job with the source owner attached. */
    private static final class Capture1987 implements SpatialNoise1934.Patches,StrongNoise1958.Patches,AutoCloseable {
        final Object source;final boolean regions;final Snapshot shape;
        final GpuQualification1961.Reservation1984 reservation;
        int cursor;boolean valid=true;long cpuStarted,overheadNanos,gatherNanos;
        Capture1987(Object source,boolean regions,Snapshot shape,GpuQualification1961.Reservation1984 reservation){
            this.source=source;this.regions=regions;this.shape=shape;this.reservation=reservation;gatherNanos=shape.gatherNanos;
        }
        public void read(int[] dst,int x,int y,int w,int h){
            long started=System.nanoTime();
            if(regions)((StrongNoise1958.Patches)source).read(dst,x,y,w,h);
            else ((SpatialNoise1934.Patches)source).read(dst,x,y,w,h);
            long copied=System.nanoTime();
            if(valid){
                if(cursor>=shape.count||w!=shape.pw||h!=shape.ph||x!=shape.xs[cursor]||y!=shape.ys[cursor])valid=false;
                else System.arraycopy(dst,0,shape.pixels,cursor*w*h,w*h);
            }
            // The first CPU prelude includes its patch allocation. Charging it
            // to the GPU is conservative: a standalone GPU gather needs that
            // temporary buffer too. CPU arithmetic between reads is excluded.
            if(cursor==0&&cpuStarted!=0)gatherNanos+=Math.max(0L,started-cpuStarted);
            cursor++;
            long finished=System.nanoTime();gatherNanos+=finished-started;overheadNanos+=finished-copied;
        }
        long cpuNanos(long elapsed){return Math.max(1L,elapsed-overheadNanos);}
        Snapshot finish(){
            if(!valid||cursor!=shape.count||!reservation.current())return null;
            return new Snapshot(shape.pixels,shape.xs,shape.ys,shape.pw,shape.ph,gatherNanos);
        }
        public void close(){try{shape.close();}finally{reservation.close();}}
    }
    /** Reserve the complete retained closure before its first allocation. Any
     * denial leaves the original CPU call unchanged and spends no retry. */
    private static Capture1987 capture1987(Object source,int width,int height,boolean regions,String key){
        GpuQualification1961.Reservation1984 reservation=null;
        ProcessingTiming1947.Trace detail1988=trace1988();
        int phase1988=regions?PipelineDetail1988.MODEL_REGIONS:PipelineDetail1988.AUX_SPATIAL;
        long bytes1988=-1,started1988=-1;
        try{
            GpuQualification1961.QueueDecision1983 permission=GpuQualification1961.permissionSnapshot1996(key,-1);
            if(!permission.accepted){
                copy1989(detail1988,phase1988,PipelineDetail1988.SKIPPED,-1,-1,0,-1,permission);return null;
            }
            int skipReason=-1;
            if(GpuQualification1961.background())skipReason=GpuQualification1961.QueueDecision1983.BACKGROUND;
            else if(GpuNoise1960.sessionBusy())skipReason=GpuQualification1961.QueueDecision1983.SESSION_BUSY1996;
            else if(!GpuNoise1960.available())skipReason=GpuQualification1961.QueueDecision1983.ENVIRONMENT_UNAVAILABLE;
            if(skipReason>=0){
                GpuQualification1961.QueueDecision1983 skipped=new GpuQualification1961.QueueDecision1983(
                    skipReason,0,0,-1,-1,-1,-1);
                copy1989(detail1988,phase1988,PipelineDetail1988.SKIPPED,-1,-1,0,-1,skipped);return null;
            }
            int pw=Math.min(regions?32:64,width),ph=Math.min(regions?32:64,height);
            int nx=regions?(width+63)/64:Math.max(1,Math.min(13,(width+127)/128));
            int ny=regions?(height+63)/64:Math.max(1,Math.min(13,(height+127)/128));
            long count=(long)nx*ny,length=count*pw*ph;
            // SpatialNoise also retains its bounded coordinate axes. Regional
            // evidence retains exactly 16 source values and three per region.
            long reference=regions?4L*(16+3L*count):4L*(count+5)+12L*Math.min(32768L,(long)width+height);
            long retained=4L*(length+2L*count)+reference+1024L;
            bytes1988=retained;
            if(count<1||count>Integer.MAX_VALUE||length>Integer.MAX_VALUE||retained>96L*1024*1024||!GpuNoise1960.workspaceFits(retained+65536L)){
                int reason=retained>96L*1024*1024||count<1||count>Integer.MAX_VALUE||length>Integer.MAX_VALUE?
                    GpuQualification1961.QueueDecision1983.REQUEST_TOO_LARGE:GpuQualification1961.QueueDecision1983.SCHEDULER_MEMORY;
                copy1989(detail1988,phase1988,PipelineDetail1988.SKIPPED,retained,retained,0,-1,
                    new GpuQualification1961.QueueDecision1983(reason,0,0,-1,-1,-1,retained));return null;
            }
            reservation=GpuQualification1961.reserve1985(key,retained);
            if(!reservation.accepted()){copy1989(detail1988,phase1988,PipelineDetail1988.QUEUE_DECLINED,retained,retained,0,-1,reservation.decision);return null;}
            if(!reservation.begin1985()){copy1989(detail1988,phase1988,PipelineDetail1988.INTERRUPTED,retained,retained,0,-1,reservation.decision);return null;}
            copy1989(detail1988,phase1988,PipelineDetail1988.RESERVED,retained,retained,0,-1,reservation.decision);
            long started=System.nanoTime();
            started1988=started;
            int[] all=new int[(int)length],xs=new int[(int)count],ys=new int[(int)count];
            for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++){
                int at=gy*nx+gx;
                xs[at]=regions?Math.max(0,Math.min(width-pw,gx*64+32-pw/2)):nx==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(nx-1));
                ys[at]=regions?Math.max(0,Math.min(height-ph,gy*64+32-ph/2)):ny==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(ny-1));
            }
            Snapshot shape=new Snapshot(all,xs,ys,pw,ph,System.nanoTime()-started);
            Capture1987 capture=new Capture1987(source,regions,shape,reservation);reservation=null;return capture;
        }catch(RuntimeException unavailable){copy1989(detail1988,phase1988,PipelineDetail1988.INCOMPLETE,bytes1988,bytes1988,0,started1988<0?-1:System.nanoTime()-started1988,null);}
        catch(LinkageError unavailable){copy1989(detail1988,phase1988,PipelineDetail1988.INCOMPLETE,bytes1988,bytes1988,0,started1988<0?-1:System.nanoTime()-started1988,null);}
        catch(OutOfMemoryError unavailable){copy1989(detail1988,phase1988,PipelineDetail1988.ALLOCATION_FAILED,bytes1988,bytes1988,0,started1988<0?-1:System.nanoTime()-started1988,null);}
        finally{if(reservation!=null)reservation.close();}
        return null;
    }
    private static Snapshot spatialSnapshot(SpatialNoise1934.Patches source,int width,int height){
        long start=System.nanoTime();int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128));
        int count=nx*ny;long length=(long)count*pw*ph;if(length>Integer.MAX_VALUE||!GpuNoise1960.workspaceFits(length*4+32768))throw new Deferred("memory_budget");
        int[] all=new int[(int)length],patch=new int[pw*ph],xs=new int[count],ys=new int[count];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++){
            cancelled();int at=gy*nx+gx;xs[at]=nx==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(nx-1));ys[at]=ny==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(ny-1));
            source.read(patch,xs[at],ys[at],pw,ph);System.arraycopy(patch,0,all,at*pw*ph,pw*ph);
        }
        return new Snapshot(all,xs,ys,pw,ph,System.nanoTime()-start);
    }
    static SpatialNoise1934 spatial(SpatialNoise1934.Patches source,final int width,final int height){
        ProcessingTiming1947.Trace detail1988=trace1988();
        long started1988=detail1988==null?0:System.nanoTime();
        int backend1988=PipelineDetail1988.CPU,why1988=PipelineDetail1988.UNKNOWN;
        boolean returned1988=false;
        try{
        if(source==null||width<1||height<1)throw new IllegalArgumentException("noise map source");
        final String key="spatial1961:v2:"+width+":"+height;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        why1988=proof==null?PipelineDetail1988.PROOF_MISSING:PipelineDetail1988.UNKNOWN;
        boolean blocked1988=false,busy1988=false;
        if(proof!=null&&!(blocked1988=rejected(key))&&!(busy1988=GpuNoise1960.sessionBusy())){
            Snapshot s=null;
            try{
            try{long start=System.nanoTime();s=spatialSnapshot(source,width,height);SpatialNoise1934 candidate=spatialCandidate1961(s.pixels,width,height,proof.variant);long elapsed=System.nanoTime()-start;cancelled();if(candidate!=null){if(!win(proof.cpuNanos,elapsed))GpuQualification1961.rejectMeasuredSpeed1992(key,"領域・空間ノイズ解析",proof.cpuNanos,elapsed);backend1988=PipelineDetail1988.GPU;returned1988=true;return candidate;}why1988=PipelineDetail1988.CANDIDATE_UNAVAILABLE;GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");}
            catch(Deferred unavailable){why1988=PipelineDetail1988.UNAVAILABLE;}catch(CancellationException stop){throw stop;}
            catch(RuntimeException failure){why1988=PipelineDetail1988.CANDIDATE_FAILURE;GpuQualification1961.rejectNonSpeed1988(key,"exception");}
            catch(LinkageError failure){why1988=PipelineDetail1988.CANDIDATE_FAILURE;GpuQualification1961.rejectNonSpeed1988(key,"unsupported");}
            catch(OutOfMemoryError unavailable){why1988=PipelineDetail1988.MEMORY_LIMIT;}
            // A completed gather remains the exact input if execution is
            // unavailable. Its private patches outlive the CPU fallback.
            if(s!=null){SpatialNoise1934 result=SpatialNoise1934.probeCpu1961(s.reader(),width,height);
                copy1989(detail1988,PipelineDetail1988.AUX_SPATIAL,PipelineDetail1988.REPLAYED,s.bytes(),-1,pixelBytes1989(s),-1,null);
                returned1988=true;return result;}
            }finally{if(s!=null)s.close();}
        }
        if(blocked1988)why1988=PipelineDetail1988.QUALIFICATION_BLOCKED;
        else if(busy1988)why1988=PipelineDetail1988.SESSION_BUSY;
        Capture1987 capture=capture1987(source,width,height,false,key);Snapshot detached=null;
        try{
            long start=System.nanoTime();if(capture!=null)capture.cpuStarted=start;
            final SpatialNoise1934 reference=SpatialNoise1934.probeCpu1961(capture==null?source:capture,width,height);
            final long originalCpu=capture==null?System.nanoTime()-start:capture.cpuNanos(System.nanoTime()-start);
            if(capture==null){returned1988=true;return reference;}
            try{
            detached=capture.finish();if(detached==null){copy1989(detail1988,PipelineDetail1988.AUX_SPATIAL,PipelineDetail1988.INCOMPLETE,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,pixelBytes1989(capture),-1,null);returned1988=true;return reference;}
            final Snapshot s=detached;
            long acquired1989=pixelBytes1989(s);
            copy1989(detail1988,PipelineDetail1988.AUX_SPATIAL,PipelineDetail1988.CAPTURED,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,acquired1989,s.gatherNanos,null);
            GpuQualification1961.Probe pending=new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){
                    try {
                    if(c.cancelled())return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961)){if(!c.cancelled()&&!GpuNoise1960.sessionBusy())GpuQualification1961.rejectNonSpeed1988(key,"unsupported");return;}
                    long minCpu=Long.MAX_VALUE,maxGpu=0;
                    for(int pair=0;pair<2;pair++){
                        if(c.cancelled())return;
                        long cpuStart=System.nanoTime();SpatialNoise1934 oracle=SpatialNoise1934.probeCpu1961(s.reader(),width,height);long cpu=Math.min(originalCpu,System.nanoTime()-cpuStart+s.gatherNanos);
                        long start=System.nanoTime();SpatialNoise1934 candidate=spatialCandidate1961(s.pixels,width,height,0);long gpu=System.nanoTime()-start+s.gatherNanos;
                        if(c.cancelled())return;
                        if(candidate==null){GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return;}
                        if(!SpatialNoise1934.same1961(reference,oracle)||!SpatialNoise1934.same1961(oracle,candidate)){reject(key);return;}
                        if(!win(cpu,gpu)){GpuQualification1961.rejectMeasuredSpeed1992(key,"解析・モデル候補",cpu,gpu);return;}
                        minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
                    }
                    if(!c.cancelled()){if(win(minCpu,maxGpu))GpuQualification1961.qualified(key,minCpu,maxGpu,0);else GpuQualification1961.rejectMeasuredSpeed1992(key,"解析・モデル候補",minCpu,maxGpu);}
                    }catch(Deferred unavailable){GpuQualification1961.outcome1984(unavailable.reason);}catch(CancellationException stop){throw stop;}
                    catch(RuntimeException failure){if(!c.cancelled()){GpuQualification1961.exception1993(failure);GpuQualification1961.rejectNonSpeed1988(key,"exception");}}
                    catch(LinkageError failure){if(!c.cancelled())GpuQualification1961.rejectNonSpeed1988(key,"unsupported");}
                    catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
                }
                public void close(){s.close();}
            };
            detached=null;GpuQualification1961.QueueDecision1983 queued1988=capture.reservation.commit(pending);
            copy1989(detail1988,PipelineDetail1988.AUX_SPATIAL,queued1988.accepted?PipelineDetail1988.QUEUED:
                PipelineDetail1988.QUEUE_DECLINED,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,acquired1989,-1,queued1988);
            }catch(CancellationException stop){throw stop;}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
            returned1988=true;return reference;
        }finally{try{if(detached!=null)detached.close();}finally{if(capture!=null)capture.close();}}
        }finally{if(returned1988&&detail1988!=null)record1988(detail1988,PipelineDetail1988.AUX_SPATIAL,backend1988,
            backend1988==PipelineDetail1988.GPU?PipelineDetail1988.UNKNOWN:why1988,System.nanoTime()-started1988);}
    }
    /** Direct host QA entry: exact production command graph; no admission bypass
     * is used by the photograph route. Patch pixels must be in original order. */
    static SpatialNoise1934 spatialCandidate1961(int[] patches,int width,int height,int variant){
        int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128)),count=nx*ny;
        if(pw<2||ph<2)return null;
        int per=(pw-1)*(ph-1),samples=count*per,chunks=(samples+255)/256,hist=2051*(count+1),values=5*(count+1)+count+5;
        long bytes=4L*(patches.length+2L*samples+chunks+hist+values)+65536;
        if(patches.length!=count*pw*ph)throw new Deferred();
        if(!GpuNoise1960.workspaceFits(bytes+4L*values))throw new Deferred("memory_budget");
        if(GpuNoise1960.sessionBusy())throw new Deferred();
        int shader=GpuNoise1960.variant(GpuNoise1960.ANALYSIS1961,variant);if(!GpuNoise1960.supports(shader))return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new Deferred();
        try{
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,patches).allocate(1,4L*samples).allocate(2,4L*values).allocate(3,4L*hist).allocate(4,4).allocate(5,4L*samples).allocate(6,4L*chunks).allocate(7,4);
            int[] u=new int[32];u[1]=pw;u[2]=ph;u[3]=count;u[4]=per;u[5]=samples;u[6]=chunks;u[7]=hist;int[] bindings={0,1,2,3,4,5,6,7};float[] f=new float[32];
            u[0]=6;b.dispatch(shader,bindings,u,f,hist);u[0]=0;b.dispatch(shader,bindings,u,f,samples);u[0]=1;b.dispatch(shader,bindings,u,f,chunks);u[0]=2;b.dispatch(shader,bindings,u,f,1);u[0]=3;b.dispatch(shader,bindings,u,f,samples);u[0]=4;b.dispatch(shader,bindings,u,f,count+1);u[0]=5;b.dispatch(shader,bindings,u,f,1);
            int[][] result=session.execute(b,new int[]{2},new int[]{values});cancelled();if(result==null)return null;
            float[] a=floats(result[0]),sigma=Arrays.copyOfRange(a,5*(count+1),5*(count+1)+count);int at=5*(count+1)+count;
            return SpatialNoise1934.fromGpu1961(width,height,nx,ny,sigma,new QualityPixels1932.NoiseStats(a[at],a[at+1],a[at+2],a[at+3],result[0][at+4]));
        }finally{session.close();}
    }

    /** GX24: sampled post-geometry analysis keeps the full ARGB image in its
     * caller's GPU session. Only the bounded map/statistics are read back for
     * the exact Java finishing policy and CPU double-sampled masks. Slots 8-16
     * are reserved by this helper; imageSlot must be outside that range. Whole
     * chain qualification, rather than a stage-only proof, owns admission. */
    static SpatialNoise1934 residentSpatial1962(GpuNoise1960.Session session,
            int imageSlot,int width,int height) {
        if(session==null||imageSlot>=8&&imageSlot<=16||width<2||height<2)return null;
        int pw=Math.min(64,width),ph=Math.min(64,height),nx=Math.max(1,Math.min(13,(width+127)/128)),ny=Math.max(1,Math.min(13,(height+127)/128)),count=nx*ny;
        int per=(pw-1)*(ph-1),samples=count*per,chunks=(samples+255)/256,hist=2051*(count+1),values=5*(count+1)+count+5;
        long bytes=4L*(count*pw*ph+2L*samples+chunks+hist+values+2L*count)+65536;
        if(!GpuNoise1960.workspaceFits(bytes+4L*values))throw new Deferred("memory_budget");
        int[] coordinates=new int[count*2];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++) {
            int at=(gy*nx+gx)*2;
            coordinates[at]=nx==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(nx-1));
            coordinates[at+1]=ny==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(ny-1));
        }
        int[] u=new int[32];u[0]=20;u[1]=pw;u[2]=ph;u[3]=count;u[4]=per;u[5]=samples;u[6]=chunks;u[7]=hist;u[8]=width;
        int shader=GpuNoise1960.ANALYSIS1961;float[] f=new float[32];
        GpuNoise1960.Batch b=new GpuNoise1960.Batch().allocate(8,4L*count*pw*ph)
            .allocate(9,4L*samples).allocate(10,4L*values).allocate(11,4L*hist)
            .allocate(12,4).allocate(13,4L*samples).allocate(14,4L*chunks).allocate(15,4).uploadDirect(16,coordinates);
        b.dispatch(shader,new int[]{imageSlot,8,10,11,12,13,14,16},u,f,count*pw*ph);
        int[] bindings={8,9,10,11,12,13,14,15};
        u[0]=6;b.dispatch(shader,bindings,u,f,hist);u[0]=0;b.dispatch(shader,bindings,u,f,samples);u[0]=1;b.dispatch(shader,bindings,u,f,chunks);u[0]=2;b.dispatch(shader,bindings,u,f,1);u[0]=3;b.dispatch(shader,bindings,u,f,samples);u[0]=4;b.dispatch(shader,bindings,u,f,count+1);u[0]=5;b.dispatch(shader,bindings,u,f,1);
        int[] result=new int[values];if(!session.executeInto(b,10,values,result,0))return null;cancelled();
        float[] a=floats(result),sigma=Arrays.copyOfRange(a,5*(count+1),5*(count+1)+count);int at=5*(count+1)+count;
        return SpatialNoise1934.fromGpu1961(width,height,nx,ny,sigma,new QualityPixels1932.NoiseStats(a[at],a[at+1],a[at+2],a[at+3],result[at+4]));
    }

    interface RegionsCpu {float[] compute(StrongNoise1958.Patches source);}
    private static Snapshot regionSnapshot(StrongNoise1958.Patches source,int width,int height){
        long start=System.nanoTime();int pw=Math.min(32,width),ph=Math.min(32,height),nx=(width+63)/64,ny=(height+63)/64,count=nx*ny;
        long length=(long)count*pw*ph;if(length>Integer.MAX_VALUE||!GpuNoise1960.workspaceFits(length*4+32768))throw new Deferred("memory_budget");
        int[] all=new int[(int)length],patch=new int[pw*ph],xs=new int[count],ys=new int[count];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++){
            cancelled();int at=gy*nx+gx;xs[at]=Math.max(0,Math.min(width-pw,gx*64+32-pw/2));ys[at]=Math.max(0,Math.min(height-ph,gy*64+32-ph/2));
            source.read(patch,xs[at],ys[at],pw,ph);System.arraycopy(patch,0,all,at*pw*ph,pw*ph);
        }
        return new Snapshot(all,xs,ys,pw,ph,System.nanoTime()-start);
    }
    static float[] regions(StrongNoise1958.Patches source,final int width,final int height,final float[] evidence,final RegionsCpu cpu){
        return regions1982(source,width,height,evidence,cpu,null);
    }
    /** Optional caller-owned scalar; the returned CPU reference is also success. */
    static float[] regions1982(StrongNoise1958.Patches source,final int width,final int height,final float[] evidence,final RegionsCpu cpu,int[] selected1982){
        ProcessingTiming1947.Trace detail1988=trace1988();
        final String key="regions1961:v2:"+width+":"+height;
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        reason1988(selected1982,proof==null?PipelineDetail1988.PROOF_MISSING:PipelineDetail1988.UNKNOWN);
        boolean blocked1988=false,busy1988=false;
        if(proof!=null&&!(blocked1988=rejected(key))&&!(busy1988=GpuNoise1960.sessionBusy())){
            Snapshot s=null;
            try{
            try{long start=System.nanoTime();s=regionSnapshot(source,width,height);float[] result=regionsCandidate1961(s.pixels,width,height,evidence,proof.variant);long elapsed=System.nanoTime()-start;cancelled();if(result!=null){if(!win(proof.cpuNanos,elapsed))GpuQualification1961.rejectMeasuredSpeed1992(key,"領域・空間ノイズ解析",proof.cpuNanos,elapsed);if(selected1982!=null)selected1982[0]=1;return result;}reason1988(selected1982,PipelineDetail1988.CANDIDATE_UNAVAILABLE);GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");}
            catch(Deferred unavailable){reason1988(selected1982,PipelineDetail1988.UNAVAILABLE);}catch(CancellationException stop){throw stop;}
            catch(RuntimeException failure){reason1988(selected1982,PipelineDetail1988.CANDIDATE_FAILURE);GpuQualification1961.rejectNonSpeed1988(key,"exception");}
            catch(LinkageError failure){reason1988(selected1982,PipelineDetail1988.CANDIDATE_FAILURE);GpuQualification1961.rejectNonSpeed1988(key,"unsupported");}
            catch(OutOfMemoryError unavailable){reason1988(selected1982,PipelineDetail1988.MEMORY_LIMIT);}
            if(s!=null){if(selected1982!=null)selected1982[0]=0;
                float[] result=cpu.compute(s.reader());copy1989(detail1988,PipelineDetail1988.MODEL_REGIONS,PipelineDetail1988.REPLAYED,s.bytes(),-1,pixelBytes1989(s),-1,null);return result;}
            }finally{if(s!=null)s.close();}
        }
        if(blocked1988)reason1988(selected1982,PipelineDetail1988.QUALIFICATION_BLOCKED);
        else if(busy1988)reason1988(selected1982,PipelineDetail1988.SESSION_BUSY);
        Capture1987 capture=capture1987(source,width,height,true,key);Snapshot detached=null;
        try{
            long start=System.nanoTime();if(capture!=null)capture.cpuStarted=start;
            final float[] reference=cpu.compute(capture==null?source:capture);
            final long originalCpu=capture==null?System.nanoTime()-start:capture.cpuNanos(System.nanoTime()-start);
            if(selected1982!=null)selected1982[0]=0;
            if(capture==null||reference==null||reference.length!=3L*capture.shape.count){
                if(capture!=null)copy1989(detail1988,PipelineDetail1988.MODEL_REGIONS,PipelineDetail1988.INCOMPLETE,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,pixelBytes1989(capture),-1,null);
                return reference;
            }
            try{
            detached=capture.finish();if(detached==null){copy1989(detail1988,PipelineDetail1988.MODEL_REGIONS,PipelineDetail1988.INCOMPLETE,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,pixelBytes1989(capture),-1,null);return reference;}
            final Snapshot s=detached;final float[] ev=Arrays.copyOf(evidence,16);
            long acquired1989=pixelBytes1989(s);
            copy1989(detail1988,PipelineDetail1988.MODEL_REGIONS,PipelineDetail1988.CAPTURED,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,acquired1989,s.gatherNanos,null);
            GpuQualification1961.Probe pending=new GpuQualification1961.Probe(){
                public void run(GpuQualification1961.Cancellation c){
                    try {
                    if(c.cancelled())return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS1961)){if(!c.cancelled()&&!GpuNoise1960.sessionBusy())GpuQualification1961.rejectNonSpeed1988(key,"unsupported");return;}
                    long minCpu=Long.MAX_VALUE,maxGpu=0;
                    for(int pair=0;pair<2;pair++){
                        if(c.cancelled())return;long cpuStart=System.nanoTime();float[] oracle=cpu.compute(s.reader());long cpuTime=Math.min(originalCpu,System.nanoTime()-cpuStart+s.gatherNanos);long start=System.nanoTime();float[] candidate=regionsCandidate1961(s.pixels,width,height,ev,0);long gpu=System.nanoTime()-start+s.gatherNanos;
                        if(c.cancelled())return;if(candidate==null){GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return;}if(!equal(reference,oracle)||!equal(oracle,candidate)){reject(key);return;}if(!win(cpuTime,gpu)){GpuQualification1961.rejectMeasuredSpeed1992(key,"領域ノイズ解析",cpuTime,gpu);return;}minCpu=Math.min(minCpu,cpuTime);maxGpu=Math.max(maxGpu,gpu);
                    }
                    if(!c.cancelled()){if(win(minCpu,maxGpu))GpuQualification1961.qualified(key,minCpu,maxGpu,0);else GpuQualification1961.rejectMeasuredSpeed1992(key,"解析・モデル候補",minCpu,maxGpu);}
                    }catch(Deferred unavailable){GpuQualification1961.outcome1984(unavailable.reason);}catch(CancellationException stop){throw stop;}
                    catch(RuntimeException failure){if(!c.cancelled()){GpuQualification1961.exception1993(failure);GpuQualification1961.rejectNonSpeed1988(key,"exception");}}
                    catch(LinkageError failure){if(!c.cancelled())GpuQualification1961.rejectNonSpeed1988(key,"unsupported");}
                    catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
                }
                public void close(){s.close();}
            };
            detached=null;GpuQualification1961.QueueDecision1983 queued1988=capture.reservation.commit(pending);
            copy1989(detail1988,PipelineDetail1988.MODEL_REGIONS,queued1988.accepted?PipelineDetail1988.QUEUED:
                PipelineDetail1988.QUEUE_DECLINED,capture.reservation.decision.requestedBytes,capture.reservation.decision.requestedBytes,acquired1989,-1,queued1988);
            }catch(CancellationException stop){throw stop;}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
            return reference;
        }finally{try{if(detached!=null)detached.close();}finally{if(capture!=null)capture.close();}}
    }
    static float[] regionsCandidate1961(int[] patches,int width,int height,float[] ev,int variant){
        int pw=Math.min(32,width),ph=Math.min(32,height),regions=((width+63)/64)*((height+63)/64),values=3*regions;
        long bytes=4L*(patches.length+2064L*regions+values)+65536;
        if(patches.length!=(long)regions*pw*ph)throw new Deferred();
        if(!GpuNoise1960.workspaceFits(bytes+4L*values))throw new Deferred("memory_budget");
        if(GpuNoise1960.sessionBusy())throw new Deferred();
        int shader=GpuNoise1960.variant(GpuNoise1960.ANALYSIS1961,variant);if(!GpuNoise1960.supports(shader))return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new Deferred();
        try{
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,patches).allocate(1,4).allocate(2,4L*values).allocate(3,4L*2064*regions).upload(4,Arrays.copyOf(ev,16)).allocate(5,4).allocate(6,4).allocate(7,4);
            int[] u=new int[32];u[0]=10;u[1]=pw;u[2]=ph;u[3]=regions;b.dispatch(shader,new int[]{0,1,2,3,4,5,6,7},u,new float[32],regions);
            int[][] result=session.execute(b,new int[]{2},new int[]{values});cancelled();return result==null?null:floats(result[0]);
        }finally{session.close();}
    }

    /** The accepted model stage transfers this single owner to its first full
     * Strong stage. Closing an unused lease releases all resident coarse maps. */
    static final class Resident implements AutoCloseable {
        final GpuNoise1960.Session session;final long setupNanos;final int variant1994;boolean closed;
        Resident(GpuNoise1960.Session s,long ns){this(s,ns,-1);}
        Resident(GpuNoise1960.Session s,long ns,int variant){session=s;setupNanos=ns;variant1994=variant;}
        public synchronized void close(){if(!closed){closed=true;session.close();}}
    }
    static String residentKey(int width,int height,int noise,boolean shadows){return "resident1961:v2:"+width+":"+height+":"+noise+":"+shadows;}
    static String residentExactKey1991(int width,int height,int noise,boolean shadows){
        return "resident1991:exactprep51-div-v2:"+width+":"+height+":"+noise+":"+shadows;
    }
    static StrongNoise1958.Model residentIfQualified(int[] h,int width,int height,int noise,boolean shadows,float[] evidence,float[] runtime){
        return residentIfQualified1988(h,width,height,noise,shadows,evidence,runtime,null);
    }
    static StrongNoise1958.Model residentIfQualified1988(int[] h,int width,int height,int noise,boolean shadows,float[] evidence,float[] runtime,int[] selected1988){
        return residentIfQualified1996(h,width,height,noise,shadows,evidence,runtime,selected1988,null);
    }
    /** Observe the existing regional GPU admission before opening a model
     * session, so parallel CPU work cannot displace an already admitted path. */
    static boolean regionsQualified1996(int width,int height){
        String key="regions1961:v2:"+width+":"+height;
        return (!GpuQualification1961.background()||GpuResident1976.deployedProbe19102())&&GpuQualification1961.restore(key)!=null&&
            !rejected(key)&&!GpuNoise1960.sessionBusy();
    }
    static StrongNoise1958.Model residentIfQualified1996(int[] h,int width,int height,int noise,boolean shadows,
            float[] evidence,float[] runtime,int[] selected1988,Runnable runtimeReady1996){
        boolean exact=GpuModelDefault1990.owns(h,width,height,noise,evidence,runtime);
        String key=exact?residentExactKey1991(width,height,noise,shadows):residentKey(width,height,noise,shadows);
        GpuQualification1961.Record proof=GpuQualification1961.restore(key);
        boolean standard=false;
        final boolean deployedProbe=GpuResident1976.deployedProbe19102();
        if((!GpuQualification1961.background()||deployedProbe)&&exact){
            // Model preparation precedes Strong.beginStage. The startup worker
            // must have released every shared GPU session before this owner.
            boolean released=deployedProbe?GpuStartup1990.modelDomainReady19102():
                GpuStartup1990.awaitModelReady1991(30000L);cancelled();
            standard=released&&GpuModelDefault1990.accepts(h,width,height,noise,shadows,evidence,runtime);
            if(standard&&!deployedProbe)GpuModelDefault1990.requestOptional1994();
        }
        // A finite-corpus mismatch is never bypassed with a shape certificate.
        // Transient startup failure may instead earn its own exact shape proof.
        boolean blocked=exact&&GpuCertificates1990.rejected(GpuModelDefault1990.SUITE)||
            (standard?GpuQualification1961.exactRejected(key):rejected(key));
        if(blocked){reason1988(selected1988,PipelineDetail1988.QUALIFICATION_BLOCKED);return null;}
        if(!standard&&proof==null){reason1988(selected1988,PipelineDetail1988.PROOF_MISSING);return null;}
        if(GpuQualification1961.background()&&!deployedProbe){reason1988(selected1988,PipelineDetail1988.BACKGROUND);return null;}
        if(GpuNoise1960.sessionBusy()){reason1988(selected1988,PipelineDetail1988.SESSION_BUSY);return null;}
        StrongNoise1958.Model model=null;boolean publish=false;
        try{
            model=runtimeReady1996==null?(standard?residentStandard1994(h,width,height,noise,shadows,evidence,runtime):
                exact?residentCandidateExact1990(h,width,height,noise,shadows,evidence,runtime,proof.variant):
                residentCandidate1961(h,width,height,noise,shadows,evidence,runtime,proof.variant)):
                standard?residentStandard1996(h,width,height,noise,shadows,evidence,runtime,runtimeReady1996):
                residentCandidate1996(h,width,height,noise,shadows,evidence,runtime,proof.variant,exact,runtimeReady1996);
            cancelled();if(model==null){reason1988(selected1988,PipelineDetail1988.CANDIDATE_UNAVAILABLE);GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return null;}
            if(exact&&(!GpuModelDefault1990.owns(h,width,height,noise,evidence,runtime)||
                    GpuCertificates1990.rejected(GpuModelDefault1990.SUITE)||GpuQualification1961.exactRejected(key))||
                    standard&&!GpuModelDefault1990.accepts(h,width,height,noise,shadows,evidence,runtime)){
                reason1988(selected1988,PipelineDetail1988.QUALIFICATION_BLOCKED);return null;
            }
            Resident lease=StrongNoise1958.takeResident1961(model);
            if(lease==null){reason1988(selected1988,PipelineDetail1988.CANDIDATE_UNAVAILABLE);return null;}
            if(!standard&&!win(proof.cpuNanos,lease.setupNanos))GpuQualification1961.rejectMeasuredSpeed1992(key,"多段モデル準備",proof.cpuNanos,lease.setupNanos);
            StrongNoise1958.attachResident1961(model,lease);
            ProcessingTiming1947.Trace selectedTrace=trace1988();
            if(selectedTrace!=null)try{ProcessingTiming1947.modelVariant1994(selectedTrace,lease.variant1994);}catch(Throwable optional){}
            publish=true;return model;
        }catch(Deferred unavailable){reason1988(selected1988,PipelineDetail1988.UNAVAILABLE);return null;}catch(CancellationException stop){throw stop;}
        catch(RuntimePreparationFailure1996 failed){failed.rethrow();throw failed;}
        catch(RuntimeException failure){reason1988(selected1988,PipelineDetail1988.CANDIDATE_FAILURE);GpuQualification1961.rejectNonSpeed1988(key,"exception");return null;}
        catch(LinkageError failure){reason1988(selected1988,PipelineDetail1988.CANDIDATE_FAILURE);GpuQualification1961.rejectNonSpeed1988(key,"unsupported");return null;}
        catch(OutOfMemoryError unavailable){reason1988(selected1988,PipelineDetail1988.MEMORY_LIMIT);return null;}
        finally{if(!publish&&model!=null)StrongNoise1958.discardResident1961(model);}
    }
    /** An optional measured layout may fail independently. Its private session
     * is closed by the candidate owner before the unchanged standard retries.
     * Cancellation is never converted into extra foreground GPU work. */
    private static StrongNoise1958.Model residentStandard1994(int[] h,int width,int height,int noise,
            boolean shadows,float[] evidence,float[] runtime){
        int variant=GpuModelDefault1990.variant1994();
        if(variant!=0){
            StrongNoise1958.Model candidate=null;boolean publish=false;
            try{
                candidate=residentCandidateExact1990(h,width,height,noise,shadows,evidence,runtime,variant);cancelled();
                if(candidate!=null&&GpuModelDefault1990.admitted1994(variant)){publish=true;return candidate;}
            }catch(CancellationException stop){throw stop;}
            catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            finally{if(candidate!=null&&!publish)StrongNoise1958.discardResident1961(candidate);}
            GpuModelDefault1990.unavailable1994(variant);
        }
        cancelled();return residentCandidateExact1990(h,width,height,noise,shadows,evidence,runtime,0);
    }
    private static StrongNoise1958.Model residentStandard1996(int[] h,int width,int height,int noise,
            boolean shadows,float[] evidence,float[] runtime,Runnable runtimeReady){
        int variant=GpuModelDefault1990.variant1994();
        if(variant!=0){
            StrongNoise1958.Model candidate=null;boolean publish=false;
            try{
                candidate=residentCandidate1996(h,width,height,noise,shadows,evidence,runtime,variant,true,runtimeReady);cancelled();
                if(candidate!=null&&GpuModelDefault1990.admitted1994(variant)){publish=true;return candidate;}
            }catch(CancellationException stop){throw stop;}
            catch(RuntimePreparationFailure1996 failed){throw failed;}
            catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){}
            finally{if(candidate!=null&&!publish)StrongNoise1958.discardResident1961(candidate);}
            GpuModelDefault1990.unavailable1994(variant);
        }
        cancelled();return residentCandidate1996(h,width,height,noise,shadows,evidence,runtime,0,true,runtimeReady);
    }
    static void scheduleResident(int[] halfSource,final int width,final int height,final int noise,final boolean shadows,float[] evidence,float[] runtime,final StrongNoise1958.Model reference,final long originalCpu){
        boolean exact=GpuModelDefault1990.owns(halfSource,width,height,noise,evidence,runtime);
        if(exact&&(GpuModelDefault1990.accepts(halfSource,width,height,noise,shadows,evidence,runtime)||
                GpuCertificates1990.rejected(GpuModelDefault1990.SUITE)))return;
        final String key=exact?residentExactKey1991(width,height,noise,shadows):residentKey(width,height,noise,shadows);
        if(rejected(key)||GpuQualification1961.restore(key)!=null||GpuQualification1961.background()||GpuNoise1960.sessionBusy()||!GpuNoise1960.available())return;
        long bytes=4L*halfSource.length+4L*(16+runtime.length)+reference.residentBytes();
        if(!GpuQualification1961.canQueue(key,bytes)||bytes>96L*1024*1024||!GpuNoise1960.workspaceFits(4L*halfSource.length+4L*(16+runtime.length)))return;
        // Reserve before cloning. All model snapshots share the existing whole
        // image rotation so a failed startup does not duplicate a finish proof.
        GpuSnapshotBudget1981.offerWhole(GpuSnapshotBudget1981.RESIDENT);
        GpuSnapshotBudget1981.Copy copy=GpuSnapshotBudget1981.tryCopy(bytes,GpuSnapshotBudget1981.RESIDENT);
        if(copy==null)return;
        GpuQualification1961.Reservation1984 reservation=null;ResidentProbe probe=null;
        try{
            reservation=GpuQualification1961.reserve1985(key,bytes);
            if(!reservation.accepted()||!reservation.begin1985()||!copy.begin())return;
            probe=new ResidentProbe(key,halfSource.clone(),width,height,noise,shadows,Arrays.copyOf(evidence,16),runtime.clone(),reference,originalCpu,exact);
            if(!copy.current()||!reservation.current())return;
            reservation.commit(probe);probe=null;
        }catch(CancellationException stop){throw stop;}catch(RuntimeException unavailable){}catch(LinkageError unavailable){}catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
        finally{try{if(probe!=null)probe.close();}finally{try{if(reservation!=null)reservation.close();}finally{copy.close();}}}
    }
    private static final class ResidentProbe implements GpuQualification1961.Probe {
        final String key;final int width,height,noise;final boolean shadows,exact;final long originalCpu;
        int[] h;float[] ev,rt;StrongNoise1958.Model reference;
        ResidentProbe(String key,int[] h,int width,int height,int noise,boolean shadows,float[] ev,float[] rt,StrongNoise1958.Model reference,long cpu){
            this(key,h,width,height,noise,shadows,ev,rt,reference,cpu,false);
        }
        ResidentProbe(String key,int[] h,int width,int height,int noise,boolean shadows,float[] ev,float[] rt,StrongNoise1958.Model reference,long cpu,boolean exact){this.key=key;this.h=h;this.width=width;this.height=height;this.noise=noise;this.shadows=shadows;this.ev=ev;this.rt=rt;this.reference=reference;originalCpu=cpu;this.exact=exact;}
        private boolean stopped(GpuQualification1961.Cancellation c){return c.cancelled()||
            exact&&GpuCertificates1990.rejected(GpuModelDefault1990.SUITE);}
        public void run(GpuQualification1961.Cancellation c){
            try {
            if(stopped(c))return;if(!GpuNoise1960.supports(GpuNoise1960.ANALYSIS)){GpuQualification1961.outcome1984("unsupported");return;}
            for(int mode=0;mode<3;mode++)if(!GpuNoise1960.supports(exact?GpuNoise1960.strongExactPrepare1990(mode,0):GpuNoise1960.strongProgram(mode,0))){GpuQualification1961.outcome1984("unsupported");return;}
            long minCpu=Long.MAX_VALUE,maxGpu=0;
            for(int pair=0;pair<2;pair++){
                if(stopped(c))return;
                long cpuStart=System.nanoTime();StrongNoise1958.Model oracle=StrongNoise1958.preparedSnapshotCpu1961(h,width,height,noise,shadows,ev,rt);long cpu=Math.min(originalCpu,System.nanoTime()-cpuStart);
                long start=System.nanoTime();StrongNoise1958.Model candidate=null;long gpu;
                try{candidate=exact?residentCandidateExact1990(h,width,height,noise,shadows,ev,rt,0):residentCandidate1961(h,width,height,noise,shadows,ev,rt,0);}
                finally{if(candidate!=null)StrongNoise1958.discardResident1961(candidate);gpu=System.nanoTime()-start;}
                if(stopped(c))return;
                if(candidate==null){GpuQualification1961.rejectNonSpeed1988(key,"execution_unavailable");return;}
                if(!StrongNoise1958.sameModel1961(reference,oracle)||!StrongNoise1958.sameModel1961(oracle,candidate)){reject(key);return;}if(!win(cpu,gpu)){GpuQualification1961.rejectMeasuredSpeed1992(key,"解析・モデル候補",cpu,gpu);return;}minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
            }
            if(!stopped(c)){if(win(minCpu,maxGpu))GpuQualification1961.qualified(key,minCpu,maxGpu,0);else GpuQualification1961.rejectMeasuredSpeed1992(key,"解析・モデル候補",minCpu,maxGpu);}
            }catch(Deferred unavailable){GpuQualification1961.outcome1984(unavailable.reason);}catch(CancellationException stop){throw stop;}
            catch(RuntimeException failure){if(!stopped(c)){GpuQualification1961.exception1993(failure);GpuQualification1961.rejectNonSpeed1988(key,"exception");}}
            catch(LinkageError failure){if(!stopped(c))GpuQualification1961.rejectNonSpeed1988(key,"unsupported");}
            catch(OutOfMemoryError unavailable){GpuQualification1961.outcome1984("memory_budget");}
        }
        public void close(){h=null;ev=null;rt=null;reference=null;}
    }
    /** GX20: one upload, two exact average2 stages, unchanged Haar and D8 bins,
     * three original preparation modes, one fence. The private maps are read
     * only for the CPU fallback oracle; their SSBOs remain resident for Strong. */
    static StrongNoise1958.Model residentCandidate1961(int[] halfSource,int width,int height,int noise,boolean shadows,float[] sourceEvidence,float[] runtime,int variant){
        return residentCandidate1990(halfSource,width,height,noise,shadows,sourceEvidence,runtime,variant,false);
    }
    /** Exact division preparation programs have distinct native IDs and never
     * reinterpret a legacy graph certificate or clear its rejection history. */
    static StrongNoise1958.Model residentCandidateExact1990(int[] halfSource,int width,int height,int noise,boolean shadows,float[] sourceEvidence,float[] runtime,int variant){
        return residentCandidate1990(halfSource,width,height,noise,shadows,sourceEvidence,runtime,variant,true);
    }
    private static StrongNoise1958.Model residentCandidate1990(int[] halfSource,int width,int height,int noise,boolean shadows,float[] sourceEvidence,float[] runtime,int variant,boolean exact){
        return residentCandidate1996(halfSource,width,height,noise,shadows,sourceEvidence,runtime,variant,exact,null);
    }
    private static StrongNoise1958.Model residentCandidate1996(int[] halfSource,int width,int height,int noise,boolean shadows,
            float[] sourceEvidence,float[] runtime,int variant,boolean exact,Runnable runtimeReady1996){
        long start=System.nanoTime();int hw=ceil2(width),hh=ceil2(height),qw=ceil2(hw),qh=ceil2(hh),ew=ceil2(qw),eh=ceil2(qh);
        int hn=hw*hh,qn=qw*qh,en=ew*eh;long maps=(long)hn+qn+en;
        long bytes=8L*maps+4L*(64+runtime.length)+65536;
        if(halfSource.length!=hn)throw new Deferred();
        if(!GpuNoise1960.workspaceFits(bytes+4L*maps+4L*(64+runtime.length)))throw new Deferred("memory_budget");
        if(GpuNoise1960.sessionBusy())throw new Deferred();
        int analysis=GpuNoise1960.variant(GpuNoise1960.ANALYSIS,variant);
        if(!GpuNoise1960.supports(analysis))return null;
        for(int mode=0;mode<3;mode++)if(!GpuNoise1960.supports(exact?GpuNoise1960.strongExactPrepare1990(mode,variant):GpuNoise1960.strongProgram(mode,variant)))return null;
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)throw new Deferred();boolean transferred=false;
        try{
            float[] initial=new float[64];System.arraycopy(sourceEvidence,0,initial,0,16);int[] emptyHistogram=new int[2064];float[] f=new float[32];
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,halfSource).allocate(14,4L*qn).allocate(15,4L*en).allocate(4,4L*hn).allocate(5,4L*qn).allocate(6,4L*en).upload(17,initial).upload(18,GpuPolicy1960.strongBasis8()).allocate(19,4);
            int[] pyramid=GpuPolicy1960.pyramidUniforms(hw,hh);b.dispatch(analysis,new int[]{0,14,17,19,18,19},pyramid,f,qn);
            pyramid=GpuPolicy1960.pyramidUniforms(qw,qh);b.dispatch(analysis,new int[]{14,15,17,19,18,19},pyramid,f,en);
            int[] ws={hw,qw,ew},hs={hh,qh,eh},slots={0,14,15},counts={hn,qn,en};
            for(int level=0;level<3;level++){
                int w=ws[level],h=hs[level],offset=16+level*16;int[] bindings={slots[level],19,17,16,18,19};
                b.upload(16,emptyHistogram);
                if(w>1&&h>1){int step=Math.max(1,(int)Math.sqrt((long)w*h/24000.0));int[] lag=GpuPolicy1960.lagUniforms(w,h,step,1,0,0,w,h,0);b.dispatch(analysis,bindings,lag,f,GpuPolicy1960.lagInvocations(lag));}
                int[] median=new int[32];median[0]=3;median[10]=offset;b.dispatch(analysis,bindings,median,f,16);
                if(w>=8&&h>=8){b.upload(16,emptyHistogram);int[] spectral=GpuPolicy1960.spectralUniforms(w,h,0);b.dispatch(analysis,bindings,spectral,f,spectral[3]*spectral[4]);median[11]=1;b.dispatch(analysis,bindings,median,f,16);}
                int[] u=new int[32];u[0]=w;u[1]=h;u[3]=h;u[5]=h;u[7]=h;u[8]=noise;u[9]=shadows?1:0;u[10]=level;u[20]=offset;
                b.dispatch(exact?GpuNoise1960.strongExactPrepare1990(level,variant):GpuNoise1960.strongProgram(level,variant),new int[]{slots[level],4+level,17,19,4,5,6,19},u,f,counts[level]);
            }
            int[][] result;
            if(runtimeReady1996==null){
                // Startup/proof callers already own complete runtime metadata.
                b.upload(2,runtime);
                result=session.execute(b,new int[]{17,4,5,6},new int[]{64,hn,qn,en});
            }else{
                long submitNanos=-1,regionNanos=-1,collectNanos=-1;boolean submitted=false,joined=false;
                ProcessingTiming1947.Trace trace=trace1988();
                try{
                    // The GPU map graph reads slots 0/14/15/17/18, never 2.
                    // One foreground caller owns both the ticket and CPU input;
                    // no executor, second GPU context or cross-thread join.
                    long submittedAt=System.nanoTime();GpuNoise1960.Ticket ticket=session.submit(b,0);
                    submitNanos=System.nanoTime()-submittedAt;cancelled();if(ticket==null)return null;
                    submitted=true;long regionsAt=System.nanoTime();
                    try{runtimeReady1996.run();}
                    catch(CancellationException stop){throw stop;}
                    catch(RuntimeException failure){throw new RuntimePreparationFailure1996(failure);}
                    catch(Error failure){throw new RuntimePreparationFailure1996(failure);}
                    finally{regionNanos=System.nanoTime()-regionsAt;}
                    cancelled();
                    // This disjoint upload is ordered on the same EGL owner.
                    // It must succeed before the lease can reach Strong.
                    if(!session.upload(2,runtime)){cancelled();return null;}
                    long collectAt=System.nanoTime();
                    try{result=session.collect(ticket,new int[]{17,4,5,6},new int[]{64,hn,qn,en});}
                    finally{collectNanos=System.nanoTime()-collectAt;}
                    cancelled();joined=result!=null;
                }finally{
                    if(trace!=null)try{PipelineDetail1988.modelOverlap1996(trace,regionNanos,submitNanos,
                        collectNanos,submitted,joined);}catch(Throwable optional){}
                }
            }
            cancelled();if(result==null)return null;
            StrongNoise1958.Model model=StrongNoise1958.model1961(width,height,result[1],result[2],result[3],floats(result[0]),runtime);
            StrongNoise1958.attachResident1961(model,new Resident(session,System.nanoTime()-start,variant));transferred=true;return model;
        }finally{if(!transferred)session.close();}
    }
}

