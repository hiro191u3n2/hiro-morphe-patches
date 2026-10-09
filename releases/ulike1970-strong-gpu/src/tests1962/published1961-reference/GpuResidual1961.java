package com.hiro.ulike;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** GX13: mobile FP32 finish of the existing NR1--4 float residuals.
 * The binary64 transform/filter/safeguard stays in the original native1955
 * arithmetic. Admission measures that preparation plus all copies/transfers,
 * ordered GPU accumulation, reconstruction, readback and context close.
 * Qualification always returns CPU output; no image is kept by this cache.
 */
final class GpuResidual1961 {
    private GpuResidual1961() {}
    private static final int BATCH_ROWS=64,LIMIT=64;
    private static final int[] UNAVAILABLE=new int[0];
    private static volatile int capability;
    private static final LinkedHashMap<String,Gate> GATES=new LinkedHashMap<String,Gate>(LIMIT,.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<String,Gate> entry){return size()>LIMIT;}
    };
    private static final class Gate {boolean busy,rejected,admitted;int proofs;long cpuNanos;}
    private static void check(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU residual NR cancelled before commit");}
    private static boolean supported() {
        if(Thread.currentThread().isInterrupted())return false;
        if(capability==0)synchronized(GpuResidual1961.class) {
            if(capability==0)capability=GpuNoise1960.supports(GpuNoise1960.RESIDUAL1961)?1:-1;
        }
        return capability>0;
    }
    static long workspaceBytes(int width,int rows) {
        if(!supported())return 0;
        long blocks=((width+3L)/4+1)*((Math.min(BATCH_ROWS,rows)+17L)/4);
        return blocks*992L*3L+(long)width*rows*40L+(long)width*14L*8L+1048576L;
    }
    static boolean process(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        if(end<=begin||noise<=0||Thread.currentThread().isInterrupted()||!supported()||GpuNoise1960.sessionBusy())return false;
        long count=(long)width*(end-begin);
        long peak=workspaceBytes(width,end-begin)+8L*input.length;
        if(count>Integer.MAX_VALUE/2||peak>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(peak))return false;
        String key=GpuNoise1960.fingerprint()+"|residual1961|"+width+","+rows+","+begin+","+end+","+
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
            if(gate.rejected||gate.busy)return false;gate.busy=true;
        }
        boolean completed=false,temporary=false;
        try {
            GpuQualification1961.Record restored=GpuQualification1961.restore(key);
            if(restored!=null&&restored.variant==0){gate.admitted=true;gate.cpuNanos=restored.cpuNanos;}
            if(!gate.admitted) {
                /* Capture foreground remains the original CPU route. Only
                 * immutable copies are handed to idle background qualification. */
                SingleNoise1955.processCpuRange(input,output,width,rows,begin,end,validBegin,
                    validEnd,originY,noise,shadows,model,protection);
                completed=true;check();
                if(!GpuQualification1961.background()&&GpuQualification1961.retainedBytes()==0&&
                        GpuNoise1960.workspaceFits(12L*input.length+count*24L+1048576L)) {
                    ResidualProbe probe=new ResidualProbe(key,gate,input,output,width,rows,begin,end,
                        validBegin,validEnd,originY,noise,shadows,model,protection);
                    GpuQualification1961.schedule(key,probe.bytes(),probe);
                }
                return true;
            }
            long start=System.nanoTime();
            int[] candidate=run(input,width,rows,begin,end,validBegin,validEnd,originY,noise,shadows,model,protection,(int)count,null);
            long gpuNanos=System.nanoTime()-start;
            if(candidate==UNAVAILABLE){temporary=true;return false;}
            if(candidate==null){reject(key,gate);return false;}
            check();start=System.nanoTime();System.arraycopy(candidate,0,output,begin*width,(int)count);
            gpuNanos+=System.nanoTime()-start;
            if(gpuNanos>gate.cpuNanos*1.05)reject(key,gate);
            completed=true;return true;
        } catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException failed){if(!completed)reject(key,gate);return completed;}
          catch(LinkageError failed){if(!completed)reject(key,gate);return completed;}
          catch(OutOfMemoryError failed){temporary=true;return completed;}
        finally {synchronized(GATES){gate.busy=false;if(!completed&&!temporary&&!gate.rejected)gate.rejected=true;}}
    }
    private static void reject(String key,Gate gate) {
        synchronized(GATES){gate.rejected=true;gate.admitted=false;}
        GpuQualification1961.reject(key);
    }
    private static void check(GpuQualification1961.Cancellation cancellation) {
        check();if(cancellation!=null&&cancellation.cancelled())throw new CancellationException("GPU residual proof cancelled");
    }
    /** Worker policy caches are leases. A proof keeps only copied Q8 values,
     * descriptor arrays and immutable Single model; it never holds the Plan. */
    private static final class FrozenProtection implements SingleNoise1955.Protection {
        final int width,firstY;final int[] policy;
        final GpuPolicy1960.PolicyData data;final long preparationNanos;
        FrozenProtection(int width,int begin,int end,int originY,SingleNoise1955.Protection original) {
            this.width=width;firstY=originY+begin;policy=new int[width*(end-begin)*2];
            long start=System.nanoTime();
            for(int y=begin;y<end;y++)for(int x=0;x<width;x++) {
                int at=((y-begin)*width+x)*2;
                policy[at]=Math.max(0,Math.min(256,original.budgetQ8(x,y+originY)));
                policy[at+1]=Math.max(0,Math.min(256,original.detailQ8(x,y+originY)));
            }
            long cpuPreparation=System.nanoTime()-start;
            if(original instanceof GpuPolicy1960.Protection&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS)) {
                start=System.nanoTime();data=((GpuPolicy1960.Protection)original).data(width,end-begin,firstY);
                preparationNanos=System.nanoTime()-start;
            } else {data=null;preparationNanos=cpuPreparation;}
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
        public void run(GpuQualification1961.Cancellation cancellation) {
            long minCpu=Long.MAX_VALUE,maxGpu=0;
            for(int trial=0;trial<2;trial++) {
                check(cancellation);
                long peak=workspaceBytes(width,end-begin)+12L*input.length;
                if(!GpuNoise1960.workspaceFits(peak))return;
                int[] oracle=new int[input.length];
                long started=System.nanoTime();
                SingleNoise1955.processCpuRange(input,oracle,width,rows,begin,end,validBegin,validEnd,
                    originY,noise,shadows,model,protection);
                long cpu=(System.nanoTime()-started)/Math.max(1,SpeedWorkers1935.maxWorkers());
                check(cancellation);started=System.nanoTime();
                int[] candidate=GpuResidual1961.run(input,width,rows,begin,end,validBegin,validEnd,
                    originY,noise,shadows,model,protection,count,cancellation);
                if(candidate==UNAVAILABLE)return;
                if(candidate==null){reject(key,gate);return;}
                /* Include the full final output copy and source-mask preparation
                 * that the actual foreground route will require. */
                candidate=candidate.clone();
                long gpu=System.nanoTime()-started+(protection==null?0:protection.preparationNanos);
                check(cancellation);
                boolean exact=true;
                for(int i=0;i<count;i++)if(reference[i]!=oracle[begin*width+i]||candidate[i]!=reference[i]){exact=false;break;}
                if(!exact||cpu<=0||gpu>cpu-cpu/20){reject(key,gate);return;}
                minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
            }
            check(cancellation);
            if(maxGpu<=minCpu-minCpu/20)GpuQualification1961.qualified(key,minCpu,maxGpu,0);
        }
        public void close(){input=null;reference=null;model=null;protection=null;}
    }
    private static int[] run(int[] input,int width,int rows,int begin,int end,int validBegin,int validEnd,
            int originY,int noise,boolean shadows,SingleNoise1955.Model model,
            SingleNoise1955.Protection protection,int count,GpuQualification1961.Cancellation cancellation) {
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try {
            GpuNoise1960.Batch initial=session.newBatch().upload(0,input);
            GpuPolicy1960.PolicyData frozen=protection instanceof FrozenProtection?((FrozenProtection)protection).data:null;
            if(frozen!=null||protection instanceof GpuPolicy1960.Protection&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS)) {
                GpuPolicy1960.PolicyData data=frozen!=null?frozen:((GpuPolicy1960.Protection)protection).data(width,end-begin,originY+begin);
                initial.upload(7,data.masks).upload(6,data.grid).allocate(3,count*8L).allocate(5,count*4L).allocate(8,4L)
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
                initial.upload(3,policy);
            }
            check(cancellation);if(!session.run(initial))return null;
            int[] candidate=new int[count];
            for(int start=begin;start<end;start+=BATCH_ROWS) {
                check(cancellation);int finish=Math.min(end,start+BATCH_ROWS),batchCount=width*(finish-start);
                float[] records=SingleResidual1961.prepare(input,width,rows,start,finish,validBegin,validEnd,
                    originY,noise,shadows,model);
                check(cancellation);if(records==null)return null;
                int[] u=new int[32];u[1]=width;u[2]=model.height;u[4]=start;u[5]=finish;u[6]=originY;
                u[15]=protection==null?0:1;u[16]=Math.floorDiv(originY+start-7,4)*4;
                u[17]=(width+3)/4+1;u[18]=(originY+finish-u[16]+3)/4;u[20]=begin;
                if((long)u[17]*u[18]*248!=records.length)return null;
                GpuNoise1960.Batch batch=session.newBatch().upload(2,records).allocate(1,batchCount*4L)
                    .dispatch(GpuNoise1960.RESIDUAL1961,new int[]{0,1,2,3},u,null,batchCount);
                int[][] out=session.execute(batch,new int[]{1},new int[]{batchCount});
                if(out==null||out.length!=1||out[0]==null||out[0].length!=batchCount)return null;
                System.arraycopy(out[0],0,candidate,(start-begin)*width,batchCount);
            }
            check(cancellation);return candidate;
        } finally {session.close();}
    }
}
