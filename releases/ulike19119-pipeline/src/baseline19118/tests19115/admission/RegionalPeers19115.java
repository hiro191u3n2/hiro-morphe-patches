package com.hiro.ulike;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

/** Controlled peers for the REAL admission helper. This is not a GPU test. */
final class GpuNoise1960 {
    static boolean prepared,supported=true;
    static int prepareCalls,readinessCalls;
    static boolean regionalReady19115(){readinessCalls++;return prepared;}
    static boolean prepareRegional19115(){prepareCalls++;if(supported)prepared=true;return supported;}
}
final class GpuQualification1961 {
    interface Cancellation {boolean cancelled();}
    interface Probe {void run(Cancellation cancellation);void close();}
    static boolean allowed=true,queueAccept=true,rejected,throwSchedule,oomSchedule,underHelperLock;
    static int scheduled,exceptions;static String outcome,key;static Probe queued;
    static boolean optionalBenchmarksAllowed1995(){return allowed;}
    static boolean exactRejected(String value){return rejected;}
    static void outcome1984(String value){outcome=value;}
    static void exception1993(Throwable failure){exceptions++;}
    static void progress1984(String phase,int done,int total){}
    static boolean schedule(String value,long bytes,Probe probe){
        underHelperLock|=Thread.holdsLock(GpuRegionalDefault19115.class);scheduled++;key=value;
        if(throwSchedule)throw new IllegalStateException("controlled queue refusal");
        if(oomSchedule)throw new OutOfMemoryError("controlled queue allocation refusal");
        if(queueAccept){queued=probe;return true;}probe.close();return false;
    }
}
final class GpuCertificates1990 {
    static final Map<String,Evidence> proofs=new HashMap<String,Evidence>();
    static final Set<String> rejects=new HashSet<String>();
    static String environment="device-A",failWrite;static boolean interruptExact;static int writes;
    static final class Evidence {final int cases;final long pixels,cpuNanos,gpuNanos;Evidence(int c,long p,long cpu,long gpu){cases=c;pixels=p;cpuNanos=cpu;gpuNanos=gpu;}}
    static String full(String key){return environment+":"+key;}
    static Evidence restore(String key){return rejected(key)?null:proofs.get(full(key));}
    static boolean rejected(String key){return rejects.contains(full(key));}
    static void reject(String key,String detail){rejects.add(full(key));proofs.remove(full(key));}
    static boolean passed(String key,int cases,long pixels,long cpu,long gpu){
        if(rejected(key)||key.equals(failWrite))return false;
        writes++;proofs.put(full(key),new Evidence(cases,pixels,cpu,gpu));
        if(interruptExact&&key.equals(GpuRegionalDefault19115.EXACT))Thread.currentThread().interrupt();
        return true;
    }
}
final class StrongNoise1958 {
    static final int HALO=18;
    static boolean oracleSuccess=true,oracleThrow,oracleOom;static int oracles;
    static final class Model {final int width,height;Model(int w,int h){width=w;height=h;}}
    static float[] gpuEvidence1960(Model model){return new float[]{1f};}
    static int[][] gpuMaps1960(Model model){return new int[][]{new int[]{1},new int[]{1},new int[]{1}};}
    static boolean gpuOracleSnapshot1961(int[] source,int[] output,int width,int rows,int begin,int end,int validBegin,int validEnd,int origin,int noise,boolean shadows,Model model,int[] policy,int[] confidence){
        oracles++;if(oracleThrow)throw new IllegalStateException("controlled oracle failure");if(oracleOom)throw new OutOfMemoryError("controlled oracle memory");
        if(!oracleSuccess)return false;
        for(int i=width*begin;i<width*end;i++)output[i]=source[i]^0x00010203;
        if(confidence!=null)Arrays.fill(confidence,7);return true;
    }
}
final class GpuStrongDefault1990 {
    static final int PROFILE=4,VARIANT=0,CASES=68;static final String SUITE="real-standard-independent-proof";
    // This isolated helper fixture starts after both legacy candidates have
    // finished. The actual GSD scheduling/terminal boundary is exercised by
    // host_regional_adaptive19115 with the real selection owners.
    static boolean adaptivePending19115(){return false;}
    static boolean regionalHeavyAllowed19115(){return true;}
    static boolean ready(){return GpuCertificates1990.restore(SUITE)!=null;}
    static boolean owns(int[] u){return u!=null&&u.length>=20&&u[10]==3&&u[0]>0&&u[7]>0&&u[2]>=u[4]&&u[3]<=u[5]&&u[3]>u[2]&&u[5]<=u[1];}
    static boolean accepts(int[] u){return owns(u)&&ready()&&!GpuQualification1961.exactRejected(GpuStrong1960.profileKey1973(u,PROFILE));}
    static final class Fixture {final int[] source,policy,u;final StrongNoise1958.Model model;Fixture(int[] s,int[] p,int[] u,StrongNoise1958.Model m){source=s;policy=p;this.u=u;model=m;}}
    static Fixture fixture19112(int id,GpuQualification1961.Cancellation c){check(c);if(id<0||id>=68)throw new IllegalArgumentException();return fixture(17,40,4,36,17,80,id);}
    static Fixture fixture(int width,int rows,int begin,int end,int mw,int mh,int id){
        int[] u=new int[32];u[0]=width;u[1]=rows;u[2]=begin;u[3]=end;u[4]=0;u[5]=rows;u[6]=0;u[7]=mh;u[8]=4;u[9]=1;u[10]=3;u[11]=1;u[12]=1;
        int[] source=new int[width*rows];Arrays.fill(source,0xff808080|id);return new Fixture(source,new int[width*(end-begin)*2],u,new StrongNoise1958.Model(mw,mh));
    }
    static void check(GpuQualification1961.Cancellation c){if(Thread.currentThread().isInterrupted()||c!=null&&c.cancelled())throw new CancellationException("controlled capture cancellation");}
    static String difference19112(int[] actual,int[] actualConfidence,int[] expected,int[] confidence,int offset,int count,GpuQualification1961.Cancellation c){
        check(c);if(actual==null||actual.length!=count)return "output_shape";
        for(int i=0;i<count;i++)if(actual[i]!=expected[offset+i])return "pixel";
        if(!Arrays.equals(actualConfidence,confidence))return "confidence";check(c);return null;
    }
    static final class Adaptive1994 {
        static final String EXACT="strong1994-tile8-pow2-68cases-256rows-exact-v1";
        static final String FAST=EXACT+":both-widths-two-trials-10pct";
        static Fixture benchmark19112(int id,GpuQualification1961.Cancellation c){check(c);if(id!=64&&id!=65)throw new IllegalArgumentException();int width=id==64?3060:4080,height=id==64?4080:3060;return fixture(width,292,18,274,width,height,id);}
    }
    static final class Adaptive1998 {
        static final String EXACT="strong1998-stripe-pow2-68cases-256rows-exact-v1";
        static final String FAST=EXACT+":both-widths-two-trials-10pct";
    }
}
final class GpuStrong1960 {
    static int fault,candidateCalls,baselineCalls,requiredCalls,slowWidth;
    static boolean returnFallback,cancelTransport,transportThrow,transportOom;
    static final StringBuilder order=new StringBuilder();
    static String profileKey1973(int[] u,int profile){return profile+":"+u[0]+":"+u[7];}
    static final class Result {final int[] pixels,confidence;Result(int[] p,int[] c){pixels=p;confidence=c;}}
    static final class Read1971 {final Result result;final String failure;final boolean regional19115;Read1971(Result r,String f,boolean regional){result=r;failure=f;regional19115=regional;}}
    static Read1971 ephemeral1973(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,Object descriptor,int variant,int profile){return run(source,u,false);}
    static Read1971 ephemeralRegional19115(int[] source,float[] evidence,int[][] maps,int[] policy,int[] u,Object descriptor,int variant,int profile,boolean required){if(required)requiredCalls++;return run(source,u,true);}
    static Read1971 run(int[] source,int[] u,boolean regional){
        if(regional)candidateCalls++;else baselineCalls++;
        if(cancelTransport)throw new CancellationException("controlled transport cancellation");
        if(transportThrow)throw new IllegalStateException("controlled transport exception");
        if(transportOom)throw new OutOfMemoryError("controlled transport memory");
        if(fault==3)return new Read1971(null,"memory_budget",false);
        if(fault==4)return new Read1971(null,"transfer",false);
        if(fault==8)return null;
        int count=u[0]*(u[3]-u[2]);int[] out=new int[count];for(int i=0;i<count;i++)out[i]=source[u[0]*u[2]+i]^0x00010203;
        int[] confidence=u[12]==0?null:new int[((u[0]+3)/4)*((u[3]-u[2]+3)/4)];if(confidence!=null)Arrays.fill(confidence,7);
        if(fault==1&&regional||fault==5&&!regional)out[0]^=1;
        if(fault==2&&regional&&confidence!=null)confidence[0]^=1;
        if(fault==6&&regional)out=Arrays.copyOf(out,Math.max(0,out.length-1));
        if(fault==7&&regional)confidence=null;
        if(u[3]-u[2]==256){order.append(u[0]).append(regional?'C':'B').append(',');delay(!regional?110:slowWidth==u[0]?150:0);}
        return new Read1971(new Result(out,confidence),null,regional&&!returnFallback);
    }
    static void delay(long ms){if(ms==0)return;try{Thread.sleep(ms);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new CancellationException();}}
}
final class GpuStartup1990 {static boolean invalidated;static void invalidateDomain(){invalidated=true;}}
final class CameraTrace1965 {static void event(String event,long id,String detail){}}
final class ProcessingTiming1947 {static long epoch=2;static long captureEpoch1953(){return epoch;}}
