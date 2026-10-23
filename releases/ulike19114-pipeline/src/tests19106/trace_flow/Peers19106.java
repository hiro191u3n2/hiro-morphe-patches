package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

final class TraceFixture19106 {
    static int javaCalls,gpuCalls,checks,cancelAt,reports;
    static boolean fits,mutate,throwGpu;
    static int[] javaTrace,gpuTrace,gpuOutput;
    static long[] reported;
    static void reset(){javaCalls=gpuCalls=checks=cancelAt=reports=0;fits=true;mutate=throwGpu=false;reported=null;
        javaTrace=new int[4096];gpuTrace=new int[4096];for(int i=0;i<=93;i++){javaTrace[2048+i]=gpuTrace[2048+i]=1;}
        javaTrace[93]=0xff123456;gpuTrace[93]=0xff123455;gpuOutput=new int[8];Arrays.fill(gpuOutput,0xff123456);gpuOutput[3]=0xff123455;
    }
}
final class StrongNoise1958 {
    static final class Model {}
    static long workspaceBytes(int w,int r){return 1000;}
    static int[][] gpuMaps1960(Model m){return new int[][]{new int[]{1},new int[]{2},new int[]{3}};}
    static float[] gpuEvidence1960(Model m){return new float[]{1};}
    static int[] diagnosticTrace19106(int[] input,int[] u,Model m,int[] policy,int x){TraceFixture19106.javaCalls++;return TraceFixture19106.javaTrace;}
}
final class GpuStrong1960 {
    static final class Trace19106 {final int[] pixels,words;Trace19106(int[] p,int[] w){pixels=p;words=w;}}
    static Trace19106 trace19106(int[] input,float[] e,int[][] m,int[] p,int[] u,int x){
        TraceFixture19106.gpuCalls++;if(TraceFixture19106.throwGpu)throw new IllegalStateException("fixture GPU");
        if(TraceFixture19106.mutate)input[0]^=1;
        return new Trace19106(TraceFixture19106.gpuOutput,TraceFixture19106.gpuTrace);
    }
}
final class GpuNoise1960 {static boolean workspaceFits(long n){return TraceFixture19106.fits;}}
final class StrongInputs19105 {
    static byte[] signature(int[] input,int[] policy,int[] u,int[][] maps,float[] evidence){
        int h=Arrays.hashCode(input)^Arrays.hashCode(policy)^Arrays.hashCode(u)^Arrays.deepHashCode(maps)^Arrays.hashCode(evidence);
        return new byte[]{(byte)h,(byte)(h>>>8),(byte)(h>>>16),(byte)(h>>>24)};
    }
}
final class ResidentStages19104 {
    static void checkFixed19105(){if(++TraceFixture19106.checks==TraceFixture19106.cancelAt)throw new CancellationException("fixture cancel");}
    static void traceReport19106(long[] v){TraceFixture19106.reports++;TraceFixture19106.reported=v.clone();}
    // Published fixed-summary shape guard. Actual DEX mutation tests additionally
    // check both production caller/module gates against the real validator.
    static boolean validFixed19105(long[] v){
        if(v==null||v.length!=14)return false;for(long x:v)if(x<0)return false;
        if(v[0]>4||v[1]>Integer.MAX_VALUE||v[2]>5||v[6]>v[13]||v[7]>0xffffffffL||v[8]>0xffffffffL||v[9]>0xffffffffL||
                v[10]>2||v[11]>2||v[12]>v[13]||v[13]>8192||v[3]>v[12]||v[4]>v[12]||v[5]>v[12])return false;
        if(v[0]==0){for(long x:v)if(x!=0)return false;}else if(v[1]==0||v[13]==0)return false;
        if(v[0]==1&&(v[2]==0||v[12]!=v[13]||v[10]==0||v[11]==0))return false;
        if(v[6]>v[12]||(v[3]+v[4]+v[5]>0)!=(v[6]>0))return false;
        return v[6]==0?v[7]==0&&v[8]==0&&v[9]==0:(v[7]!=v[8]||v[7]!=v[9]);
    }
}
