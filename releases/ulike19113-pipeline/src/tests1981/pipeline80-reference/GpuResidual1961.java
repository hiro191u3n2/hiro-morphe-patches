package com.hiro.ulike;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** GX13/GX25: mobile FP32 finish of the existing NR1--4 float residuals.
 * The binary64 transform/filter/safeguard stays in the original native1955
 * arithmetic. Admission measures that preparation plus all copies/transfers,
 * ordered GPU accumulation, reconstruction, readback and context close.
 * Qualification always returns CPU output; no image is kept by this cache.
 */
final class GpuResidual1961 {
    private GpuResidual1961() {}
    private static final int BATCH_ROWS=64,LIMIT=64;
    private static final int[] UNAVAILABLE=new int[0];
    private static final LinkedHashMap<String,Gate> GATES=new LinkedHashMap<String,Gate>(LIMIT,.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<String,Gate> entry){return size()>LIMIT;}
    };
    private static final class Gate {boolean busy,admitted;long cpuNanos;}
    private static void check(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU residual NR cancelled before commit");}
    private static boolean supported() {
        if(Thread.currentThread().isInterrupted())return false;
        return GpuNoise1960.supports(GpuNoise1960.RESIDUAL1961);
    }
    static long workspaceBytes(int width,int rows) {
        if(!supported())return 0;
        long blocks=((width+3L)/4+1)*((Math.min(BATCH_ROWS,rows)+17L)/4);
        // Two GPU banks, two Java-owned native-order record banks, bounded
        // exact source/model tail cache and source-array JNI acquisition peak.
        return blocks*992L*4L+(long)width*rows*40L+(long)width*14L*8L+
            SingleResidual1961.Preparation.cacheBytes(width,13*13*3+88)+1048576L;
    }
    static boolean process(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        if(end<=begin||noise<=0||Thread.currentThread().isInterrupted()||!supported()||GpuNoise1960.sessionBusy())return false;
        long count=(long)width*(end-begin);
        long peak=workspaceBytes(width,end-begin)+8L*input.length;
        if(count>Integer.MAX_VALUE/2||peak>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(peak))return false;
        String key=GpuNoise1960.fingerprint()+"|residual1964-cache-buffer|"+width+","+rows+","+begin+","+end+","+
            validBegin+","+validEnd+","+originY+","+noise+","+shadows+","+model.height+","+
            model.columns+","+model.rows+","+model.gpuPatchWidth1960()+","+model.gpuPatchHeight1960()+","+
            (protection==null?0:protection instanceof GpuPolicy1960.Protection?2:1);
        if(protection instanceof GpuPolicy1960.Protection) {
            GpuPolicy1960.Protection p=(GpuPolicy1960.Protection)protection;
            key+="|"+p.strong+","+(p.plan==null?256:p.plan.shadowBudgetQ8)+","+(p.plan==null?0:p.plan.beautyQ8);
        }
        Gate gate;
        synchronized(GATES) {
            gate=GATES.get(key);if(gate==null){gate=new Gate();GATES.put(key,gate);}
            if(GpuQualification1961.exactRejected(key)||gate.busy)return false;gate.busy=true;
        }
        boolean completed=false;
        try {
            GpuQualification1961.Record restored=GpuQualification1961.restore(key);
            if(restored!=null&&restored.variant==0){gate.admitted=true;gate.cpuNanos=restored.cpuNanos;}
            if(!gate.admitted) {
                /* Capture foreground remains the original CPU route. Only
                 * immutable copies are handed to idle background qualification. */
                SingleNoise1955.processCpuRange(input,output,width,rows,begin,end,validBegin,
                    validEnd,originY,noise,shadows,model,protection);
                completed=true;check();
                if(GpuQualification1961.maySchedule(key)&&!GpuQualification1961.background()&&
                        GpuNoise1960.workspaceFits(12L*input.length+count*24L+1048576L)) {
                    long proofBase=proofBytesEstimate1967(input.length,count,model,protection);
                    if(!GpuQualification1961.canQueue(key,proofBase))return true;
                    ResidualProbe probe=new ResidualProbe(key,gate,input,output,width,rows,begin,end,
                        validBegin,validEnd,originY,noise,shadows,model,protection);
                    GpuQualification1961.schedule(key,probe.bytes(),probe);
                }
                return true;
            }
            long start=System.nanoTime();
            int[] candidate=run(input,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,(int)count,null);
            long gpuNanos=System.nanoTime()-start;
            if(candidate==UNAVAILABLE)return false;
            // Failed execution is retryable. A null result has never passed
            // through the ARGB equality test and cannot be an exact rejection.
            if(candidate==null){slow(key,gate);return false;}
            check();start=System.nanoTime();System.arraycopy(candidate,0,output,begin*width,(int)count);
            gpuNanos+=System.nanoTime()-start;
            if(gpuNanos>gate.cpuNanos-gate.cpuNanos/20)slow(key,gate);
            completed=true;return true;
        } catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException failed){if(!completed)slow(key,gate);return completed;}
          catch(LinkageError failed){if(!completed)slow(key,gate);return completed;}
          catch(OutOfMemoryError failed){return completed;}
        finally {synchronized(GATES){gate.busy=false;}}
    }
    private static void slow(String key,Gate gate) {
        synchronized(GATES){gate.admitted=false;gate.cpuNanos=0;}
        GpuQualification1961.rejectSpeed(key);
    }
    private static void reject(String key,Gate gate) {
        synchronized(GATES){gate.admitted=false;}
        GpuQualification1961.reject(key);
    }
    private static void check(GpuQualification1961.Cancellation cancellation) {
        check();if(cancellation!=null&&cancellation.cancelled())throw new CancellationException("GPU residual proof cancelled");
    }
    /** Worker policy caches are leases. A proof keeps only copied Q8 values,
     * descriptor arrays and immutable Single model; it never holds the Plan. */
    /** Account all copied policy/mask/grid arrays before allocating a probe.
     * The final schedule() check remains authoritative under concurrent callers. */
    private static long proofBytesEstimate1967(int inputCount,long core,SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        long bytes=4L*(inputCount+core+model.gpuData1960().length)+65536L;
        if(protection!=null)bytes+=8L*core;
        if(protection instanceof GpuPolicy1960.Protection){
            QualityPixels1932.Plan plan=((GpuPolicy1960.Protection)protection).plan;
            SpatialNoise1934 map=plan==null?null:plan.localNoise;
            long grid=map==null?1:(long)map.columns*map.rows;
            bytes+=8L*core+256+4L*Math.max(1L,grid);
        }
        return bytes;
    }
    private static final class FrozenProtection implements SingleNoise1955.Protection {
        final int width,firstY;final int[] policy;
        final GpuPolicy1960.PolicyData data;final long preparationNanos;
        FrozenProtection(int width,int begin,int end,int originY,SingleNoise1955.Protection original) {
            this.width=width;firstY=originY+begin;policy=new int[width*(end-begin)*2];
            long start=System.nanoTime();
            if(original instanceof GpuPolicy1960.Protection) {
                boolean snapshot=GpuNoise1960.supports(GpuNoise1960.ANALYSIS);
                GpuPolicy1960.Protection frozen=((GpuPolicy1960.Protection)original).freezePolicy1964(width,end-begin,firstY,policy,snapshot);
                data=snapshot?frozen.data(width,end-begin,firstY):null;
            } else {
                for(int y=begin;y<end;y++)for(int x=0;x<width;x++) {
                    int at=((y-begin)*width+x)*2;
                    policy[at]=Math.max(0,Math.min(256,original.budgetQ8(x,y+originY)));
                    policy[at+1]=Math.max(0,Math.min(256,original.detailQ8(x,y+originY)));
                }
                data=null;
            }
            preparationNanos=System.nanoTime()-start;
        }
        public int budgetQ8(int x,int y){return policy[((y-firstY)*width+x)*2];}
        public int detailQ8(int x,int y){return policy[((y-firstY)*width+x)*2+1];}
        long bytes(){return 4L*policy.length+(data==null?0:4L*(data.masks.length+data.grid.length+data.u.length+data.f.length));}
    }
    private static final class ResidualProbe implements GpuQualification1961.Probe {
        final String key;final Gate gate;final int width,rows,begin,end,validBegin,validEnd,originY,noise,count;
        final boolean shadows;int[] input,reference;SingleNoise1955.Model model;FrozenProtection protection;
        ResidualProbe(String key,Gate gate,int[] original,int[] output,int width,int rows,int begin,int end,
                int lo,int hi,int origin,int noise,boolean shadows,SingleNoise1955.Model model,
                SingleNoise1955.Protection protection) {
            this.key=key;this.gate=gate;this.width=width;this.rows=rows;this.begin=begin;this.end=end;
            validBegin=lo;validEnd=hi;originY=origin;this.noise=noise;this.shadows=shadows;this.model=model;
            count=width*(end-begin);input=original.clone();reference=new int[count];
            System.arraycopy(output,begin*width,reference,0,count);
            this.protection=protection==null?null:new FrozenProtection(width,begin,end,origin,protection);
        }
        long bytes(){return 4L*(input.length+reference.length+model.gpuData1960().length)+(protection==null?0:protection.bytes())+65536L;}
        /** GX30 compare measured CPU routes, including actual pool admission
         * and joining. First validate the original serial oracle, then reuse
         * its private output for the complete disjoint parallel run. The frozen
         * policy remains indexed by the full strip's absolute coordinates. */
        private long cpu1964(final int[] oracle,final GpuQualification1961.Cancellation cancellation) {
            check(cancellation);long started=System.nanoTime();
            SingleNoise1955.processCpuRange(input,oracle,width,rows,begin,end,validBegin,validEnd,
                originY,noise,shadows,model,protection);
            long serial=System.nanoTime()-started;check(cancellation);
            for(int i=0;i<count;i++)if(reference[i]!=oracle[begin*width+i]){reject(key,gate);return 0;}
            long parallel=GpuQualification1961.parallelRows1964(begin,end,4,cancellation,new GpuQualification1961.RowTask1964(){
                public boolean run(int first,int last) {
                    SingleNoise1955.processCpuRange(input,oracle,width,rows,first,last,validBegin,validEnd,
                        originY,noise,shadows,model,protection);
                    return true;
                }
            });
            check(cancellation);
            for(int i=0;i<count;i++)if(reference[i]!=oracle[begin*width+i]){reject(key,gate);return 0;}
            return parallel<=0?0:Math.min(serial,parallel);
        }
        public void run(GpuQualification1961.Cancellation cancellation) {
            try {
            long minCpu=Long.MAX_VALUE,maxGpu=0;
            for(int trial=0;trial<2;trial++) {
                check(cancellation);
                long peak=workspaceBytes(width,end-begin)+12L*input.length;
                if(!GpuNoise1960.workspaceFits(peak))return;
                int[] oracle=new int[input.length];
                long cpu=cpu1964(oracle,cancellation);if(cpu<=0)return;
                check(cancellation);long started=System.nanoTime();
                int[] candidate=GpuResidual1961.run(input,width,rows,begin,end,validBegin,validEnd,
                    originY,noise,shadows,model,protection,count,cancellation);
                if(candidate==UNAVAILABLE)return;
                check(cancellation);
                if(candidate==null){slow(key,gate);return;}
                /* Include the full final output copy and source-mask preparation
                 * that the actual foreground route will require. */
                candidate=candidate.clone();
                long gpu=System.nanoTime()-started+(protection==null?0:protection.preparationNanos);
                check(cancellation);
                boolean exact=true;
                for(int i=0;i<count;i++)if(reference[i]!=oracle[begin*width+i]||candidate[i]!=reference[i]){exact=false;break;}
                if(!exact){reject(key,gate);return;}
                if(cpu<=0||gpu<=0||gpu>cpu-cpu/20){slow(key,gate);return;}
                minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
            }
            check(cancellation);
            if(maxGpu<=minCpu-minCpu/20)GpuQualification1961.qualified(key,minCpu,maxGpu,0);
            else slow(key,gate);
            }catch(CancellationException stop){throw stop;}
            catch(RuntimeException failure){if(!cancellation.cancelled())slow(key,gate);}
            catch(LinkageError failure){if(!cancellation.cancelled())slow(key,gate);}
            catch(OutOfMemoryError unavailable){}
        }
        public void close(){input=null;reference=null;model=null;protection=null;}
    }
    private static int[] run(final int[] input,final int width,final int rows,final int begin,final int end,final int validBegin,final int validEnd,
            final int originY,final int noise,final boolean shadows,final SingleNoise1955.Model model,
            final SingleNoise1955.Protection protection,int count,final GpuQualification1961.Cancellation cancellation) {
        final GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try {
            GpuNoise1960.Batch initial=session.newBatch().uploadDirect(0,input);
            GpuPolicy1960.PolicyData frozen=protection instanceof FrozenProtection?((FrozenProtection)protection).data:null;
            if(frozen!=null||protection instanceof GpuPolicy1960.Protection&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS)) {
                GpuPolicy1960.PolicyData data=frozen!=null?frozen:((GpuPolicy1960.Protection)protection).data(width,end-begin,originY+begin);
                initial.uploadDirect(7,data.masks).uploadDirect(6,data.grid).allocate(3,count*8L).allocate(5,count*4L).allocate(8,4L)
                    .dispatch(GpuNoise1960.ANALYSIS,new int[]{8,8,5,3,6,7},data.u,data.f,count);
            } else {
                int[] policy=protection==null?new int[]{0}:protection instanceof FrozenProtection?
                    ((FrozenProtection)protection).policy:new int[count*2];
                if(protection!=null&&!(protection instanceof FrozenProtection))for(int y=begin;y<end;y++) {
                    check();for(int x=0;x<width;x++) {
                        int at=((y-begin)*width+x)*2;
                        policy[at]=Math.max(0,Math.min(256,protection.budgetQ8(x,y+originY)));
                        policy[at+1]=Math.max(0,Math.min(256,protection.detailQ8(x,y+originY)));
                    }
                }
                initial.uploadDirect(3,policy);
            }
            check(cancellation);if(!session.run(initial))return null;
            final SingleResidual1961.Preparation preparation=new SingleResidual1961.Preparation(input,
                width,rows,validBegin,validEnd,originY,noise,shadows,model);
            try {
            return ResidualOverlap1962.runDirect(width,begin,end,BATCH_ROWS,new ResidualOverlap1962.DirectDriver(){
                public ByteBuffer prepare(int start,int finish){
                    check(cancellation);return preparation.prepare(start,finish);
                }
                public Object submit(ByteBuffer records,int bank,int start,int finish){
                    check(cancellation);int batchCount=width*(finish-start);
                    int[] u=new int[32];u[1]=width;u[2]=model.height;u[4]=start;u[5]=finish;u[6]=originY;
                    u[15]=protection==null?0:1;u[16]=Math.floorDiv(originY+start-7,4)*4;
                    u[17]=(width+3)/4+1;u[18]=(originY+finish-u[16]+3)/4;u[20]=begin;
                    if((long)u[17]*u[18]*248*4!=records.remaining())return null;
                    int outputSlot=bank==0?1:14,recordSlot=bank==0?2:15;
                    GpuNoise1960.Batch batch=session.newBatch().uploadDirect(recordSlot,records).allocate(outputSlot,batchCount*4L)
                        .dispatch(GpuNoise1960.RESIDUAL1961,new int[]{0,outputSlot,recordSlot,3},u,null,batchCount);
                    // Native submit snapshots record bytes before return. The
                    // worker may prepare the next band without aliasing them.
                    return session.submit(batch,bank);
                }
                public boolean collect(Object ticket,int bank,int batchCount,int[] target,int offset){
                    return session.collectInto((GpuNoise1960.Ticket)ticket,bank==0?1:14,batchCount,target,offset);
                }
            },new ResidualOverlap1962.Guard(){public void check(){GpuResidual1961.check(cancellation);}});
            } finally {preparation.close();}
        } finally {session.close();}
    }
}

