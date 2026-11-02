package com.hiro.ulike;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** GX1/GX3/GX7/GX8: the exact double-precision single-image NR candidate.
 * Real fp64 capability is checked before model/mask/workspace allocation.
 * Pixel- and speed-qualified keys retain counters only, never photograph data.
 */
final class GpuSingle1960 {
    private GpuSingle1960() {}
    private static final int LIMIT=24;
    private static final int[] UNAVAILABLE=new int[0];
    private static volatile int capability;
    private static final LinkedHashMap<String,Gate> GATES=new LinkedHashMap<String,Gate>(LIMIT,.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<String,Gate> entry){return size()>LIMIT;}
    };
    private static final class Gate {boolean busy,admitted,rejected;int proofs;long cpuNanos;}

    private static boolean supported() {
        if(Thread.currentThread().isInterrupted())return false;
        if(capability==0)synchronized(GpuSingle1960.class) {
            if(capability==0)capability=GpuNoise1960.supports(GpuNoise1960.SINGLE)?1:-1;
        }
        return capability>0;
    }
    /** Reservation is added only once this driver has actually proved fp64. */
    static long workspaceBytes(int width,int coreRows) {
        if(capability!=1&&!supported())return 0;
        long blocks=((width+3L)/4+1)*((coreRows+14L+3)/4);
        return blocks*992L+(long)width*(coreRows+14L)*4L+(long)width*coreRows*28L+65536L;
    }
    static boolean process(int[] input,int[] output,int width,int rows,int begin,int end,
            int validBegin,int validEnd,int originY,int noise,boolean shadows,
            SingleNoise1955.Model model,SingleNoise1955.Protection protection) {
        if(end<=begin||noise==0||Thread.currentThread().isInterrupted())return false;
        if(!supported())return GpuResidual1961.process(input,output,width,rows,begin,end,validBegin,
            validEnd,originY,noise,shadows,model,protection);
        long count=(long)width*(end-begin);
        long blockCols=(width+3L)/4+1;
        long firstBy=(long)Math.floorDiv(originY+begin-7,4)*4;
        long blockRows=((long)originY+end-firstBy+3)/4;
        long blocks=blockCols*blockRows;
        long scratchBytes=blocks*992L;
        /* Native input upload may hold a JNI snapshot; mask descriptors and the
         * private readback coexist with their GPU allocations during proof. */
        long total=scratchBytes+8L*input.length+count*44L+4L*model.gpuData1960().length+1048576L;
        if(count>Integer.MAX_VALUE/2||blocks>Integer.MAX_VALUE||scratchBytes>GpuNoise1960.MAX_BYTES||
                total>GpuNoise1960.MAX_BYTES||!GpuNoise1960.workspaceFits(total))return false;
        String key=GpuNoise1960.fingerprint()+"|single|"+width+","+rows+","+model.height+","+
            begin+","+end+","+validBegin+","+validEnd+","+originY+","+noise+","+shadows+","+
            model.columns+","+model.rows+","+model.gpuPatchWidth1960()+","+model.gpuPatchHeight1960()+","+
            (protection==null?0:protection instanceof GpuPolicy1960.Protection?2:1);
        if(protection instanceof GpuPolicy1960.Protection) {
            GpuPolicy1960.Protection p=(GpuPolicy1960.Protection)protection;
            key+="|"+p.strong+","+(p.plan==null?256:p.plan.shadowBudgetQ8)+","+(p.plan==null?0:p.plan.beautyQ8);
        }
        Gate gate;
        synchronized(GATES) {
            gate=GATES.get(key);
            if(gate==null){gate=new Gate();GATES.put(key,gate);}
            if(GpuQualification1961.exactRejected(key)||gate.busy)return false;
            gate.busy=true;
        }
        boolean completed=false;
        try {
            GpuQualification1961.Record saved=GpuQualification1961.restore(key);
            if(saved!=null){gate.admitted=true;gate.proofs=2;gate.cpuNanos=saved.cpuNanos;}
            if(!gate.admitted) {
                SingleNoise1955.processCpuRange(input,output,width,rows,begin,end,validBegin,
                    validEnd,originY,noise,shadows,model,protection);
                completed=true;interrupted();
                if(GpuQualification1961.maySchedule(key)&&!GpuQualification1961.background()&&
                        
                        (protection==null||protection instanceof GpuPolicy1960.Protection)&&
                        GpuNoise1960.workspaceFits(total+8L*input.length)) {
                    long proofBase=proofBytesEstimate1967(input.length,count,model,protection);
                    if(!GpuQualification1961.canQueue(key,proofBase))return true;
                    SingleProbe probe=new SingleProbe(key,input,output,width,rows,begin,end,
                        validBegin,validEnd,originY,noise,shadows,model,protection,(int)firstBy,
                        (int)blockCols,(int)blockRows,(int)blocks,(int)count,scratchBytes);
                    GpuQualification1961.schedule(key,probe.bytes(),probe);
                }
                return true;
            }
            long start=System.nanoTime();
            int[] candidate=run(input,width,rows,begin,end,validBegin,validEnd,originY,
                noise,shadows,model,protection,(int)firstBy,(int)blockCols,(int)blockRows,(int)blocks,(int)count,scratchBytes);
            if(candidate==UNAVAILABLE)return false;
            if(candidate==null){reject(key,gate);return false;}
            interrupted();System.arraycopy(candidate,0,output,begin*width,(int)count);completed=true;
            if(System.nanoTime()-start>gate.cpuNanos-gate.cpuNanos/20)slow(key,gate);
            return true;
        } catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException failed){if(!completed&&gate.admitted)reject(key,gate);return completed;}
          catch(LinkageError failed){if(!completed&&gate.admitted)reject(key,gate);return completed;}
          catch(OutOfMemoryError failed){return completed;}
        finally {synchronized(GATES){gate.busy=false;}}
    }
    private static void reject(String key,Gate gate){synchronized(GATES){gate.rejected=true;gate.admitted=false;}GpuQualification1961.rejectExact(key);}
    private static void slow(String key,Gate gate){synchronized(GATES){gate.admitted=false;gate.proofs=0;gate.cpuNanos=0;}GpuQualification1961.rejectSpeed(key);}
    private static void interrupted(){if(Thread.currentThread().isInterrupted()||GpuQualification1961.cancelled())throw new CancellationException("GPU single NR cancelled before commit");}
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
        final int width,firstY;final int[] policy;final GpuPolicy1960.PolicyData data;final long preparationNanos;
        FrozenProtection(int width,int begin,int end,int originY,SingleNoise1955.Protection original) {
            this.width=width;firstY=begin+originY;policy=new int[width*(end-begin)*2];long start=System.nanoTime();
            if(original instanceof GpuPolicy1960.Protection) {
                boolean snapshot=GpuNoise1960.supports(GpuNoise1960.ANALYSIS);
                GpuPolicy1960.Protection frozen=((GpuPolicy1960.Protection)original).freezePolicy1964(width,end-begin,firstY,policy,snapshot);
                data=snapshot?frozen.data(width,end-begin,firstY):null;
            } else {
                for(int y=begin;y<end;y++)for(int x=0;x<width;x++) {
                    int at=((y-begin)*width+x)*2;policy[at]=Math.max(0,Math.min(256,original.budgetQ8(x,y+originY)));
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
    private static final class SingleProbe implements GpuQualification1961.Probe {
        final String key;final int width,rows,begin,end,validBegin,validEnd,originY,noise,firstBy,blockCols,blockRows,blocks,count;
        final boolean shadows;final long scratchBytes;int[] input,reference;SingleNoise1955.Model model;FrozenProtection protection;
        SingleProbe(String key,int[] input,int[] output,int width,int rows,int begin,int end,int lo,int hi,
                int origin,int noise,boolean shadows,SingleNoise1955.Model model,SingleNoise1955.Protection protection,
                int firstBy,int cols,int blockRows,int blocks,int count,long scratchBytes) {
            this.key=key;this.width=width;this.rows=rows;this.begin=begin;this.end=end;validBegin=lo;validEnd=hi;
            originY=origin;this.noise=noise;this.shadows=shadows;this.model=model;this.firstBy=firstBy;blockCols=cols;
            this.blockRows=blockRows;this.blocks=blocks;this.count=count;this.scratchBytes=scratchBytes;
            this.input=input.clone();reference=new int[count];System.arraycopy(output,begin*width,reference,0,count);
            this.protection=protection==null?null:new FrozenProtection(width,begin,end,origin,protection);
        }
        long bytes(){return 4L*(input.length+reference.length+model.gpuData1960().length)+(protection==null?0:protection.bytes())+65536L;}
        public void run(GpuQualification1961.Cancellation cancellation) {
            try {
            long minCpu=Long.MAX_VALUE,maxGpu=0;
            for(int trial=0;trial<2;trial++) {
                if(cancellation.cancelled())return;interrupted();int[] oracle=new int[input.length];
                long start=System.nanoTime();SingleNoise1955.processCpuRange(input,oracle,width,rows,begin,end,
                    validBegin,validEnd,originY,noise,shadows,model,protection);
                long serialCpu=System.nanoTime()-start;
                for(int i=0;i<count;i++)if(reference[i]!=oracle[begin*width+i]){GpuQualification1961.rejectExact(key);return;}
                // The serial oracle is validated before its private output is
                // reused by disjoint parallel workers. Retain no extra frame.
                final int[] parallel=oracle;
                long cpu=GpuQualification1961.parallelRows1964(begin,end,4,cancellation,new GpuQualification1961.RowTask1964(){
                    public boolean run(int first,int last) {
                        SingleNoise1955.processCpuRange(input,parallel,width,rows,first,last,
                            validBegin,validEnd,originY,noise,shadows,model,protection);
                        return true;
                    }
                });
                if(cpu>0)cpu=Math.min(cpu,serialCpu);
                if(cancellation.cancelled())return;start=System.nanoTime();
                int[] candidate=GpuSingle1960.run(input,width,rows,begin,end,validBegin,validEnd,originY,
                    noise,shadows,model,protection,firstBy,blockCols,blockRows,blocks,count,scratchBytes);
                if(candidate==UNAVAILABLE)return;if(cancellation.cancelled())return;
                if(candidate==null){GpuQualification1961.rejectExact(key);return;}
                candidate=candidate.clone();long gpu=System.nanoTime()-start+(protection==null?0:protection.preparationNanos);
                if(cancellation.cancelled())return;
                for(int i=0;i<count;i++)if(parallel[begin*width+i]!=reference[i]||candidate[i]!=reference[i]){GpuQualification1961.rejectExact(key);return;}
                if(cpu<=0||gpu<=0||gpu>cpu-cpu/20){GpuQualification1961.rejectSpeed(key);return;}
                minCpu=Math.min(minCpu,cpu);maxGpu=Math.max(maxGpu,gpu);
            }
            if(!cancellation.cancelled()){if(maxGpu<=minCpu-minCpu/20)GpuQualification1961.qualified(key,minCpu,maxGpu,0);else GpuQualification1961.rejectSpeed(key);}
            } catch(CancellationException cancelled){throw cancelled;}
              catch(RuntimeException failure){if(!cancellation.cancelled())GpuQualification1961.rejectExact(key);}
              catch(LinkageError failure){if(!cancellation.cancelled())GpuQualification1961.rejectExact(key);}
              catch(OutOfMemoryError unavailable) { }
        }
        public void close(){input=null;reference=null;model=null;protection=null;}
    }
    private static int[] run(int[] input,int width,int rows,int begin,int end,int validBegin,int validEnd,
            int originY,int noise,boolean shadows,SingleNoise1955.Model model,
            SingleNoise1955.Protection protection,int firstBy,int blockCols,int blockRows,
            int blocks,int count,long scratchBytes) {
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try {
            if(!session.upload(0,input)||!session.allocate(1,count*4L)||!session.upload(2,model.gpuData1960())||
                    !session.allocate(4,scratchBytes))return null;
            GpuPolicy1960.PolicyData frozen=protection instanceof FrozenProtection?((FrozenProtection)protection).data:null;
            if(frozen!=null||protection instanceof GpuPolicy1960.Protection&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS)) {
                GpuPolicy1960.PolicyData data=frozen!=null?frozen:((GpuPolicy1960.Protection)protection).data(
                    width,end-begin,originY+begin);
                if(!GpuPolicy1960.uploadPolicy(session,data,3,7,6,5,8))return null;
            } else {
                int[] policy=protection==null?new int[]{0}:new int[count*2];
                if(protection!=null)for(int y=begin;y<end;y++) {
                    interrupted();
                    for(int x=0;x<width;x++) {
                        int at=((y-begin)*width+x)*2;
                        policy[at]=Math.max(0,Math.min(256,protection.budgetQ8(x,originY+y)));
                        policy[at+1]=Math.max(0,Math.min(256,protection.detailQ8(x,originY+y)));
                    }
                }
                if(!session.upload(3,policy))return null;
            }
            int[] u=new int[32];u[0]=0;u[1]=width;u[2]=model.height;u[3]=rows;u[4]=begin;u[5]=end;
            u[6]=originY;u[7]=validBegin;u[8]=validEnd;u[9]=model.columns;u[10]=model.rows;
            u[11]=model.gpuPatchWidth1960();u[12]=model.gpuPatchHeight1960();u[13]=noise;
            u[14]=shadows?1:0;u[15]=protection==null?0:1;u[16]=firstBy;u[17]=blockCols;u[18]=blockRows;
            int[] bindings={0,1,2,3,4};
            if(!session.dispatch(GpuNoise1960.SINGLE,bindings,u,null,blocks))return null;
            u[0]=1;if(!session.dispatch(GpuNoise1960.SINGLE,bindings,u,null,count))return null;
            return session.readInts(1,count);
        } finally {session.close();}
    }
}

