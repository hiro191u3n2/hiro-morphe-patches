package com.hiro.ulike;

import android.graphics.Bitmap;
import java.util.ArrayList;

/** Explicit host transport/policy and memory-refusal peers. The real current
 * Finish graph, memory helper, qualification and queue execute. No native GPU
 * pixels or actual native memory release are asserted by these peers. */
final class FinishStageControl1988 {
    static int failStage,fault,opens,activeSessions,activeLeases,activePolicies,activeTickets,submitted,collected;
    static void reset(int stage,int kind){failStage=stage;fault=kind;opens=activeSessions=activeLeases=activePolicies=activeTickets=submitted=collected=0;}
    static boolean stop(int stage){
        if(failStage!=stage)return false;
        if(fault==1)throw new IllegalStateException("private fixture fault is not retained");
        if(fault==2)throw new LinkageError("private fixture linkage is not retained");
        if(fault==3)throw new OutOfMemoryError("private fixture allocation is not retained");
        if(fault==4)throw new java.util.concurrent.CancellationException("private fixture cancellation is not retained");
        return true;
    }
    static void fill(int[] pixels,int count,int first,int width){for(int i=0;i<count;i++)pixels[i]=FinishControl1986.pixel(i%width,first+i/width);
        if(failStage==22&&count>0)pixels[count-1]^=1;}
}
final class ProcessingTiming1947 {
    static volatile long epoch=1;static long captureEpoch1953(){return epoch;}static long capture1989=1791670635624L;static long captureId1989(long expected){return expected==epoch?capture1989:0;}
    static final int CORRECTION=2,NOISE=3;
    static final class Trace{}static final class Token{}
    static Token beginStage(Trace trace,int stage){return null;}static void end(Token token){}
}
final class GpuNoise1960 {
    static volatile boolean busy;static final int FINISH1961=8,GEOMETRY=3;static final long MAX_BYTES=512L*1024*1024;
    static String fingerprint(){return "unchanged-pixel-shader-v1";}static boolean sessionBusy(){return busy;}
    static boolean workspaceFits(long bytes){
        if(bytes==65540L&&FinishStageControl1988.stop(13))return false;
        if(bytes>16L*1024*1024){
            FinishMemoryControl1989.checks++;
            if(FinishMemoryControl1989.mode==3)busy=true;
            if(FinishMemoryControl1989.mode==4)Thread.currentThread().interrupt();
            if(FinishMemoryControl1989.mode==2||FinishMemoryControl1989.mode==6)
                return FinishMemoryControl1989.checks%2==0;
            if(FinishMemoryControl1989.mode!=0||FinishStageControl1988.stop(8))return false;
        }
        return bytes>=0&&bytes<=MAX_BYTES;
    }
    static boolean snapshotMemory1989(long[] values,int offset){
        FinishMemoryControl1989.samples++;
        long m=1024L*1024, nativeBytes=(FinishMemoryControl1989.checks%2==0?100:250)*m;
        long[] sample={512*m,200*m,312*m,0,0,0,nativeBytes,nativeBytes,0,0,0,Math.max(0,312*m-nativeBytes-64*m)};
        System.arraycopy(sample,0,values,offset,12);return true;
    }
    static Session open(){if(FinishStageControl1988.stop(10))return null;return new Session();}
    static final class Batch {
        int kind;int[] u;
        Batch allocate(int slot,long bytes){return this;}Batch uploadDirect(int slot,int[] words){return this;}Batch uploadDirect(int slot,float[] words){return this;}
        Batch dispatch(int kind,int[] bindings,int[] u,float[] f,int count){this.kind=kind;this.u=u.clone();return this;}
    }
    static final class Lease1971 implements AutoCloseable {
        final int stage;boolean closed;Lease1971(int stage){this.stage=stage;FinishStageControl1988.activeLeases++;}
        boolean revalidate1971(){return !FinishStageControl1988.stop(stage==19?25:stage);}
        boolean consumeJava1974(long bytes){return true;}
        public void close(){if(!closed){closed=true;FinishStageControl1988.activeLeases--;}}
    }
    static final class Ticket {final Batch batch;boolean done;Ticket(Batch batch){this.batch=batch;FinishStageControl1988.activeTickets++;}}
    static final class Session implements AutoCloseable {
        final int ordinal;boolean closed,pipeline;final ArrayList<Ticket> tickets=new ArrayList<Ticket>();
        Session(){ordinal=++FinishStageControl1988.opens;FinishStageControl1988.activeSessions++;}
        Lease1971 reserveCapacity1983(int[] slots,long[] bytes,long[] uploads,long javaBytes){
            int stage=slots.length==1?14:slots[0]==3?19:11;
            if(stage==19)pipeline=true;if(FinishStageControl1988.stop(stage))return null;return new Lease1971(stage);
        }
        boolean run(Batch b){return !FinishStageControl1988.stop(b.kind==GEOMETRY?15:12);}
        Ticket submit(Batch b,int bank){if(FinishStageControl1988.stop(20))return null;Ticket t=new Ticket(b);tickets.add(t);FinishStageControl1988.submitted++;return t;}
        boolean collectInto(Ticket ticket,int slot,int count,int[] out,int offset){
            if(FinishStageControl1988.stop(21))return false;
            if(ticket.done)throw new AssertionError("ticket collected twice");ticket.done=true;FinishStageControl1988.activeTickets--;FinishStageControl1988.collected++;
            FinishStageControl1988.fill(out,count,ticket.batch.u[4],ticket.batch.u[1]);return true;
        }
        boolean executeInto(Batch b,int slot,int count,int[] out,int offset){
            if(FinishStageControl1988.stop(27))return false;FinishStageControl1988.fill(out,count,b.u[4],b.u[1]);return true;
        }
        public void close(){if(closed)return;closed=true;FinishStageControl1988.activeSessions--;
            for(Ticket t:tickets)if(!t.done){t.done=true;FinishStageControl1988.activeTickets--;}
            FinishControl1986.clock+=pipeline?700:800;
        }
    }
}
final class GpuPolicy1960 {
    static final class GeometryData {
        final int[] tables=new int[1];final float[] weights=new float[1];final int[] u=new int[32];final boolean exactCrop=true;
        GeometryData(int height){u[14]=height;}int[] uniforms(int mode){return u.clone();}
    }
    static GeometryData geometry(int sw,int sh,int rotation,int width,int height){
        if(FinishStageControl1988.stop(7))throw new IllegalStateException("controlled geometry unavailable");return new GeometryData(height);
    }
}
final class GpuAnalysis1961 {
    static SpatialNoise1934 residentSpatial1962(GpuNoise1960.Session session,int slot,int width,int height){
        if(FinishStageControl1988.stop(16))return null;
        if(session.ordinal>=5){if(FinishStageControl1988.failStage==17)Bitmap.failCreateOnce=true;if(FinishStageControl1988.failStage==23)Bitmap.failNextCreatedWrite=true;}
        return SpatialNoise1934.fromGpu1961(width,height,1,1,new float[]{0},new QualityPixels1932.NoiseStats(0,0,0,0,0));
    }
}
final class FinishPolicy1953 {
    static final class Band implements AutoCloseable {
        final int[] words;final int mode=0;boolean closed;
        Band(int count){words=new int[4*count];FinishStageControl1988.activePolicies++;}
        public void close(){if(!closed){closed=true;FinishStageControl1988.activePolicies--;}}
    }
    static Band prepareParallel1978(QualityPixels1932.Plan plan,int width,int height,int first,int last,int workers){
        if(FinishStageControl1988.stop(18))return null;return new Band(width*(last-first));
    }
}

final class FinishMemoryControl1989 {
    static int mode,checks,trims,samples;static long trimCost;
    static void reset(int value,long cost){mode=value;checks=trims=samples=0;trimCost=cost;}
}
final class SpeedWorkers1935 {
    static int maxWorkers(){return 2;}static int availableWorkers1944(){return 2;}
    static void run(Runnable[] tasks){for(Runnable task:tasks)task.run();}
    static int trimNativeIdle1989(){
        FinishMemoryControl1989.trims++;
        if(FinishMemoryControl1989.mode==5)return 0;
        if(FinishMemoryControl1989.mode==6)throw new LinkageError("controlled optional native trim failure");
        FinishControl1986.clock+=FinishMemoryControl1989.trimCost;return 1;
    }
}
