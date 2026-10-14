package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.Arrays;

/** Geometry peers expose typed calls, while the other suites run the real service. */
final class PipelineDetail1988 {
    static final int UNKNOWN=0,BACKGROUND=1,INPUT_INELIGIBLE=2,SESSION_BUSY=3,UNAVAILABLE=4,
        ENVIRONMENT_MISSING=5,EXACT_REJECTED=6,ROUTE_BUSY=7,CANDIDATE_UNAVAILABLE=8,
        PROOF_MISSING=9,CANDIDATE_FAILURE=10,MEMORY_LIMIT=11;
}
final class GpuQualification1961 {
    interface Cancellation{boolean cancelled();}interface Probe{void run(Cancellation c);void close();}
    static final class Record {long cpuNanos,gpuNanos;Record(long c,long g){cpuNanos=c;gpuNanos=g;}}
    static boolean admitted,cancelled,exact;static Record record;static Probe pending;static String reason,candidate;
    static long measuredCpu,measuredGpu;static int qualified;
    static boolean background(){return false;}static boolean cancelled(){return cancelled;}
    static boolean exactRejected(String key){return exact;}static Record restore(String key){return admitted?record:null;}
    static boolean maySchedule(String key){return true;}static long retainedBytes(){return 0;}
    static void reject(String key){reason="quality_mismatch";exact=true;admitted=false;}
    static void rejectNonSpeed1988(String key,String value){reason=value;admitted=false;}
    static void rejectMeasuredSpeed1992(String key,String name,long cpu,long gpu){
        candidate=name;measuredCpu=cpu;measuredGpu=gpu;admitted=false;
        reason=cpu>0&&gpu>0?"speed_condition":"comparison_incomplete";
    }
    static void qualified(String key,long cpu,long gpu,int variant){qualified++;admitted=true;record=new Record(cpu,gpu);}
    static void schedule(String key,long bytes,Probe probe){if(pending!=null)throw new AssertionError("snapshot owner duplicated");pending=probe;}
}
final class GpuNoise1960 {
    static final int GEOMETRY=12;static final long MAX_BYTES=512L*1024*1024;
    static int fault,serial,opened,closed;static boolean fits=true;
    static boolean sessionBusy(){return false;}static boolean supports(int n){return true;}
    static String fingerprint(){return "geometry-controlled-"+serial;}static boolean workspaceFits(long n){return fits;}
    static Session open(){if(fault==7)return null;opened++;return new Session();}
    static final class Batch {Batch upload(int s,int[] v){return this;}Batch upload(int s,float[] v){return this;}
        Batch allocate(int s,long n){return this;}Batch dispatch(int p,int[] b,int[] u,float[] f,int c){return this;}}
    static final class Session {
        boolean run(Batch b){return true;}
        int[][] execute(Batch b,int[] slots,int[] counts){
            if(fault==1)return null;if(fault==2)throw new IllegalStateException("controlled transport exception");
            if(fault==3)throw new OutOfMemoryError("controlled memory failure");if(fault==4)throw new LinkageError("controlled linkage failure");
            if(fault==6)try{Thread.sleep(20);}catch(InterruptedException e){throw new AssertionError(e);}
            int[] out=new int[counts[0]];Arrays.fill(out,0xff234567);if(fault==5)out[out.length-1]^=1;
            return new int[][]{out};
        }
        void close(){closed++;}
    }
}
final class GpuPolicy1960 {
    static final class GeometryData {
        boolean exactCrop=true;int horizontalInvocations=16,outputInvocations=12;int[] tables=new int[32];float[] weights=new float[32];
        long workspaceBytes(){return 256;}int[] uniforms(int mode){int[] u=new int[32];u[14]=4;return u;}
    }
    static GeometryData geometry(int sw,int sh,int r,int w,int h){return new GeometryData();}
}
final class FastResize1933 {
    static Bitmap output(int w,int h){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] p=new int[w*h];Arrays.fill(p,0xff234567);b.setPixels(p,0,w,0,0,w,h);return b;}
    static Bitmap resampleCpu1960(Bitmap b,int r,int w,int h){return output(w,h);}
}
