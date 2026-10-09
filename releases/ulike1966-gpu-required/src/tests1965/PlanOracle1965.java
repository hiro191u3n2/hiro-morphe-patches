package com.hiro.ulike;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Random;
/** Test-only frozen published per-shot policy oracle, isolated JVM. */
public final class PlanOracle1965 {
    private static final float[] SPECIAL={Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,
        -0f,0f,Float.MIN_VALUE,-Float.MIN_VALUE,-100f,.00001f,.0019531249f,.001953125f,
        .01f,Math.nextDown(.01f),Math.nextUp(.01f),.5f,Math.nextDown(.5f),Math.nextUp(.5f),
        .8f,Math.nextDown(.8f),Math.nextUp(.8f),1f,Math.nextDown(1f),Math.nextUp(1f),
        3.8f,Math.nextDown(3.8f),Math.nextUp(3.8f),12f,32f,64f,Math.nextUp(64f),96f,255f};
    private static void record(DataOutputStream out,boolean present,float sy,float sc,int samples,
            int iso,long exposure,int lens,float beauty,int noise,int sharp,float scale)throws Exception {
        QualityPixels1932.NoiseStats stats=present?new QualityPixels1932.NoiseStats(sy,sc,128f,0f,samples):null;
        QualityPixels1932.Plan p=QualityPixels1932.plan(stats,iso,exposure,lens,beauty,noise,sharp,
            true,true,scale).withLocalNoise(null,noise);
        int[] u=new int[32];u[0]=present?1:0;u[1]=iso;u[2]=(int)exposure;u[3]=(int)(exposure>>>32);
        u[4]=lens;u[5]=noise;u[6]=sharp;u[7]=samples;
        for(int value:u)out.writeInt(value);
        for(float value:new float[]{sy,sc,beauty,scale})out.writeInt(Float.floatToRawIntBits(value));
        for(int value:new int[]{p.noiseLevel,p.sharpLevel,p.shadowBudgetQ8,p.beautyQ8,p.sharpGainQ8,
            p.sharpFloorQ8,p.sharpLimit,Float.floatToRawIntBits(p.sourceSigma),Float.floatToRawIntBits(p.outputScale)})out.writeInt(value);
    }
    public static void main(String[] args)throws Exception {
        int count=20000;Random random=new Random(196547);
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[0]))) {
            out.writeInt(196547);out.writeInt(count);
            for(int i=0;i<count;i++) {
                float sy=i<SPECIAL.length*4?SPECIAL[i%SPECIAL.length]:random.nextFloat()*50f-5f;
                float sc=i<SPECIAL.length*4?SPECIAL[(i*7)%SPECIAL.length]:random.nextFloat()*110f-10f;
                float scale=i<4096?SPECIAL[(i*13)%SPECIAL.length]:.005f+random.nextFloat()*70f;
                float beauty=i<4096?SPECIAL[(i*19)%SPECIAL.length]:random.nextFloat()*1.2f-.1f;
                int[] sampleOptions={Integer.MIN_VALUE,-1,0,1,47,48,49,Integer.MAX_VALUE};
                int[] isoOptions={Integer.MIN_VALUE,-1,0,1,99,100,101,3200,Integer.MAX_VALUE};
                long[] exposureOptions={Long.MIN_VALUE,-1,0,24999999,25000000,25000001,49999999,
                    50000000,50000001,0xffffffffL,0x100000000L,Long.MAX_VALUE};
                record(out,i%19!=0,sy,sc,sampleOptions[i%sampleOptions.length],isoOptions[i%isoOptions.length],
                    exposureOptions[i%exposureOptions.length],i%9-2,beauty,i%11-3,i%11-3,scale);
            }
        }
        System.out.println("{\"status\":\"passed\",\"records\":"+count+"}");
    }
}
