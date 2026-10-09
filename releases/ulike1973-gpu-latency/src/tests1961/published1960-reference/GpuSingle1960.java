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
        if(end<=begin||noise==0||Thread.currentThread().isInterrupted()||!supported())return false;
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
            if(gate.rejected||gate.busy)return false;
            gate.busy=true;
        }
        boolean completed=false,temporary=false;
        try {
            long start=System.nanoTime();
            int[] candidate=run(input,width,rows,begin,end,validBegin,validEnd,originY,
                noise,shadows,model,protection,(int)firstBy,(int)blockCols,(int)blockRows,(int)blocks,(int)count,scratchBytes);
            long gpuNanos=System.nanoTime()-start;
            if(candidate==UNAVAILABLE){temporary=true;return false;}
            if(candidate==null){reject(gate);return false;}
            interrupted();
            if(gate.admitted) {
                System.arraycopy(candidate,0,output,begin*width,(int)count);
                synchronized(GATES){if(gpuNanos>gate.cpuNanos*1.05)gate.rejected=true;}
                completed=true;return true;
            }
            start=System.nanoTime();
            SingleNoise1955.processCpuRange(input,output,width,rows,begin,end,validBegin,
                validEnd,originY,noise,shadows,model,protection);
            /* Strip CPU work can run in parallel, while this EGL owner queues
             * all GPU sessions. Admission uses that conservative throughput
             * bound; the GPU timer already includes owner-queue waits. */
            long cpuNanos=(System.nanoTime()-start)/Math.max(1,SpeedWorkers1935.maxWorkers());
            interrupted();boolean exact=true;
            for(int i=0;i<candidate.length;i++)if(candidate[i]!=output[begin*width+i]){exact=false;break;}
            synchronized(GATES) {
                if(!exact||cpuNanos<=0||gpuNanos>cpuNanos*.95)gate.rejected=true;
                else {gate.cpuNanos=gate.proofs==0?cpuNanos:Math.min(gate.cpuNanos,cpuNanos);if(++gate.proofs>=2)gate.admitted=true;}
            }
            /* Qualification always publishes the original CPU output. */
            completed=true;return true;
        } catch(CancellationException cancelled){throw cancelled;}
          catch(RuntimeException failed){reject(gate);return false;}
          catch(LinkageError failed){reject(gate);return false;}
          catch(OutOfMemoryError failed){reject(gate);return false;}
        finally {synchronized(GATES){gate.busy=false;if(!completed&&!temporary&&!gate.rejected)gate.rejected=true;}}
    }
    private static void reject(Gate gate){synchronized(GATES){gate.rejected=true;}}
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("GPU single NR cancelled before commit");}
    private static int[] run(int[] input,int width,int rows,int begin,int end,int validBegin,int validEnd,
            int originY,int noise,boolean shadows,SingleNoise1955.Model model,
            SingleNoise1955.Protection protection,int firstBy,int blockCols,int blockRows,
            int blocks,int count,long scratchBytes) {
        GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return UNAVAILABLE;
        try {
            if(!session.upload(0,input)||!session.allocate(1,count*4L)||!session.upload(2,model.gpuData1960())||
                    !session.allocate(4,scratchBytes))return null;
            if(protection instanceof GpuPolicy1960.Protection&&GpuNoise1960.supports(GpuNoise1960.ANALYSIS)) {
                GpuPolicy1960.PolicyData data=((GpuPolicy1960.Protection)protection).data(
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
