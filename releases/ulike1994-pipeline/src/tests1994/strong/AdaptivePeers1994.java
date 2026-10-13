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
    static int fault,submits;static boolean slowCandidate;
    static int program1973(int profile,int variant){return profile==7?48:39;}
    static String profileKey1973(int[] u,int profile){return profile+":"+u[0]+":"+u[1];}
    static final class Result {int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
    static final class Read1971 {Result result;String failure;Read1971(Result r,String f){result=r;failure=f;}}
    static Read1971 ephemeral1973(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,Object descriptor,int variant,int profile){
        submits++;if(fault==4)throw new OutOfMemoryError("controlled candidate refusal");if(slowCandidate&&profile==7&&u[3]-u[2]==256)try{Thread.sleep(30);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.util.concurrent.CancellationException();}if(fault==3)return new Read1971(null,"memory_budget");int count=u[0]*(u[3]-u[2]);int[] out=new int[count];
        for(int i=0;i<count;i++)out[i]=source[u[0]*u[2]+i]^0x00010203;
        int[] cf=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];if(cf!=null)Arrays.fill(cf,7);
        if(fault==1||fault==5&&profile==4)out[0]^=1;if(fault==2&&cf!=null)cf[0]^=1;
        return new Read1971(new Result(out,cf),null);
    }
}
final class GpuNoise1960 {static boolean supported=true,warmed=true;static int supportCalls;static boolean prepared1994(int program){return warmed;}static boolean supports(int program){supportCalls++;if(supported)warmed=true;return supported;}}
final class GpuQualification1961 {static String lastOutcome;static void outcome1984(String reason){lastOutcome=reason;}static void exception1993(Throwable failure){}interface Cancellation {boolean cancelled();}interface Probe{void run(Cancellation c);void close();}static boolean acceptQueue;static int scheduled;static Probe pending;static boolean schedule(String key,long bytes,Probe probe){scheduled++;if(acceptQueue){pending=probe;return true;}probe.close();return false;}static boolean exactRejected;static boolean exactRejected(String key){return exactRejected;}}
final class GpuCertificates1990 {
 static final java.util.Map<String,Evidence> proofs=new java.util.HashMap<String,Evidence>();static final java.util.Set<String> rejects=new java.util.HashSet<String>();
 static final class Evidence {int cases;long pixels,cpuNanos,gpuNanos;Evidence(int c,long p,long cpu,long gpu){cases=c;pixels=p;cpuNanos=cpu;gpuNanos=gpu;}}
 static Evidence restore(String suite){return rejects.contains(suite)?null:proofs.get(suite);}
 static boolean rejected(String suite){return rejects.contains(suite);}
 static void reject(String suite,String reason){rejects.add(suite);proofs.remove(suite);}
 static boolean passed(String suite,int c,long p,long cpu,long gpu){if(rejected(suite))return false;proofs.put(suite,new Evidence(c,p,cpu,gpu));return true;}
}
final class CameraTrace1965 {static void event(String event,long id,String detail){}}

final class GpuStartup1990 {static boolean invalidated;static void invalidateDomain(){invalidated=true;}}
