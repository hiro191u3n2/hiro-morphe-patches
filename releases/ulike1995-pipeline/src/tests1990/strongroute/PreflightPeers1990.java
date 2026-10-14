package com.hiro.ulike;
import java.util.Arrays;

/** State-machine transport only. Numerical shader tests run separately. */
final class StrongNoise1958 {
    static final int HALO=18;
    static final class Model {int width,height;int[][] maps;float[] evidence;}
    static Model model1961(int w,int h,int[] a,int[] b,int[] c,float[] evidence,float[] runtime){Model m=new Model();m.width=w;m.height=h;m.maps=new int[][]{a,b,c};m.evidence=runtime;return m;}
    static boolean gpuOracleSnapshot1961(int[] input,int[] output,int width,int rows,int begin,int end,int vb,int ve,int origin,int noise,boolean shadows,Model model,int[] policy,int[] confidence){for(int i=width*begin;i<width*end;i++)output[i]=input[i]^0x00010203;if(confidence!=null)Arrays.fill(confidence,7);return true;}
    static float[] gpuEvidence1960(Model m){return m.evidence;}
    static int[][] gpuMaps1960(Model m){return m.maps;}
}
final class GpuStrong1960 {
    static int fault,submits;
    static int program1973(int profile,int variant){return 39;}
    static String profileKey1973(int[] u,int profile){return profile+":"+u[0]+":"+u[1];}
    static final class Result {int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
    static final class Read1971 {Result result;String failure;Read1971(Result r,String f){result=r;failure=f;}}
    static Read1971 ephemeral1973(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,Object descriptor,int variant,int profile){
        submits++;if(fault==3)return new Read1971(null,"memory_budget");int count=u[0]*(u[3]-u[2]);int[] out=new int[count];
        for(int i=0;i<count;i++)out[i]=source[u[0]*u[2]+i]^0x00010203;
        int[] cf=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];if(cf!=null)Arrays.fill(cf,7);
        if(fault==1)out[0]^=1;if(fault==2&&cf!=null)cf[0]^=1;
        return new Read1971(new Result(out,cf),null);
    }
}
final class GpuNoise1960 {static boolean prepared1994(int program){return true;}static boolean supports(int program){return true;}}
final class GpuQualification1961 {static boolean optionalAllowed1995=true;static boolean optionalBenchmarksAllowed1995(){return optionalAllowed1995;}static String lastOutcome;static void outcome1984(String reason){lastOutcome=reason;}static void exception1993(Throwable failure){}interface Cancellation {boolean cancelled();}interface Probe{void run(Cancellation c);void close();}static boolean schedule(String key,long bytes,Probe probe){probe.close();return false;}static boolean exactRejected;static boolean exactRejected(String key){return exactRejected;}}
final class GpuCertificates1990 {
    static boolean rejected;static Evidence evidence;static int writes;
    static final class Evidence {int cases;long pixels,cpuNanos,gpuNanos;Evidence(int c,long p,long cpu,long gpu){cases=c;pixels=p;cpuNanos=cpu;gpuNanos=gpu;}}
    static Evidence restore(String suite){return rejected?null:evidence;}
    static boolean rejected(String suite){return rejected;}
    static void reject(String suite,String reason){rejected=true;evidence=null;}
    static boolean passed(String suite,int c,long p,long cpu,long gpu){if(rejected)return false;writes++;evidence=new Evidence(c,p,cpu,gpu);return true;}
}
final class CameraTrace1965 {static void event(String event,long id,String detail){}}

final class GpuStartup1990 {static boolean invalidated;static void invalidateDomain(){invalidated=true;}}
