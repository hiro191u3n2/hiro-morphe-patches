package com.hiro.ulike;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/** Executes current helpers against renamed, byte-pinned published .53 sources.
 * Production FaceRegions1934.Mask is compiled unmodified; Android detector stubs
 * throw if invoked so no detector behavior can accidentally satisfy this test. */
public final class PolicySpeed1954Test {
    private static long h24,h26,maps,coordinateValues,policyCases,policyPixels,unknownCases;
    private static void need24(boolean ok,String message){h24++;if(!ok)throw new AssertionError(message);}
    private static void need26(boolean ok,String message){h26++;if(!ok)throw new AssertionError(message);}
    private static QualityPixels1932.NoiseStats stats(){return new QualityPixels1932.NoiseStats(2.77f,4.01f,128f,.12f,200);}
    private static <T> T map(Class<T> type,int w,int h,int columns,int rows,float left,float top,
            float sx,float sy,float[] values)throws Exception {
        Constructor<T> c=type.getDeclaredConstructor(int.class,int.class,int.class,int.class,
            float.class,float.class,float.class,float.class,float[].class,QualityPixels1932.NoiseStats.class);
        c.setAccessible(true);return c.newInstance(w,h,columns,rows,left,top,sx,sy,values.clone(),stats());
    }
    private static boolean sameFloat(float a,float b) {
        return Float.floatToRawIntBits(a)==Float.floatToRawIntBits(b) || Float.isNaN(a)&&Float.isNaN(b);
    }
    private static void coordinates()throws Exception {
        float[] starts={-19.25f,0f,31.5f,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY};
        float[] steps={33.25f,.125f,0f,-0f,Float.NaN,Float.POSITIVE_INFINITY};
        float[] samples={-2.25f,-1f,0f,-0f,1f,7.5f,13f,52f,100f,100.5f,103f,
            Float.NaN,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY};
        for(int columns=1;columns<=13;columns++)for(int rows=1;rows<=13;rows++)for(int mode=0;mode<6;mode++) {
            float[] values=new float[columns*rows];
            for(int i=0;i<values.length;i++)values[i]=(i%17)*.417f+(i%3)*1.17f;
            if(mode==5)for(int i=0;i<values.length;i+=3)values[i]=i%2==0?Float.NaN:Float.MAX_VALUE;
            SpatialNoise1934 actual=map(SpatialNoise1934.class,129,101,columns,rows,starts[mode],
                starts[(mode+2)%6],steps[mode],steps[(mode+3)%6],values);
            SpatialNoiseReference1953 reference=map(SpatialNoiseReference1953.class,129,101,columns,rows,
                starts[mode],starts[(mode+2)%6],steps[mode],steps[(mode+3)%6],values);
            Field xAxis=SpatialNoise1934.class.getDeclaredField("xAxis");xAxis.setAccessible(true);
            Field yAxis=SpatialNoise1934.class.getDeclaredField("yAxis");yAxis.setAccessible(true);
            need24(xAxis.get(actual)!=null && yAxis.get(actual)!=null,"bounded coordinate caches present");
            for(float y:samples)for(int x=-3;x<=132;x++) {
                need24(sameFloat(actual.sigmaAt(x,y),reference.sigmaAt(x,y)),"integer-coordinate exact float");
                need24(actual.budgetQ8(x,y)==reference.budgetQ8(x,y),"integer-coordinate exact rounded budget");
                coordinateValues++;
            }
            for(float x:samples)for(float y:samples) {
                need24(sameFloat(actual.sigmaAt(x,y),reference.sigmaAt(x,y)),"fractional/nonfinite coordinate fallback");
                coordinateValues++;
            }
            maps++;
        }
        // Real-width integer coordinates, end clamps, and row origins outside the map.
        float[] values={.1f,.41f,1.9f,2.37f,4.21f,8.77f,15.19f,19.27f,31.83f};
        SpatialNoise1934 actual=map(SpatialNoise1934.class,4080,3060,3,3,31.5f,31.5f,2008f,1498f,values);
        SpatialNoiseReference1953 reference=map(SpatialNoiseReference1953.class,4080,3060,3,3,31.5f,31.5f,2008f,1498f,values);
        for(int y:new int[]{-999,-1,0,1,31,32,1498,2000,3059,3060,9999})for(int x=-1;x<=4080;x++) {
            need24(sameFloat(actual.sigmaAt(x,y),reference.sigmaAt(x,y)),"4080-wide map exact");coordinateValues++;
        }
        actual=map(SpatialNoise1934.class,32768,1,3,3,0,0,17.5f,19.75f,values);
        Field field=SpatialNoise1934.class.getDeclaredField("xAxis");field.setAccessible(true);
        need24(field.get(actual)==null,"optional cache dimension budget bounded");
        reference=map(SpatialNoiseReference1953.class,32768,1,3,3,0,0,17.5f,19.75f,values);
        for(int x=0;x<32768;x+=17)need24(sameFloat(actual.sigmaAt(x,0),reference.sigmaAt(x,0)),"uncached large map exact");
    }
    private static final class CustomMask implements QualityPixels1932.RegionMask {
        final int mode;int calls;long sequence;
        CustomMask(int mode){this.mode=mode;}
        public int skinQ8(int x,int y) {
            calls++;sequence=sequence*31+x;sequence=sequence*31+y;
            if(mode==1)return -1000000;
            if(mode==2)return x%3==0?Integer.MIN_VALUE:x%3==1?1000000:-65536;
            if(mode==3)return (calls*997)^(x*1103515245+y*1234567);
            if(mode==4)return calls%13==0?1000000:(calls*17+x+y)&255;
            return (x*23+y*11)&511;
        }
        public int detailQ8(int x,int y){throw new AssertionError("unused detail mask read");}
    }
    private static QualityPixels1932.Plan plan(int noise,int sharp,boolean texture,float scale,int mode) {
        return QualityPixels1932.plan(stats(),100<<Math.min(mode,6),10000000L,mode%4,.73f,
            noise,sharp,texture,true,scale);
    }
    private static int decode(int mode,int[] words,int at,int k) {
        return mode==FinishPolicy1953.CONSTANT4?words[k]:mode==FinishPolicy1953.RAW4?words[at*4+k]:
            (words[at*2+k/2]>>>((k&1)*16))&65535;
    }
    private static void compare(QualityPixels1932.Plan actualPlan,QualityPixels1932.Plan referencePlan,
            int width,int rows,int first,int last,int origin,CustomMask actualMask,CustomMask referenceMask) {
        FinishPolicy1953.Band a=FinishPolicy1953.prepare(actualPlan,width,rows,first,last,origin);
        FinishPolicyReference1953.Band e=FinishPolicyReference1953.prepare(referencePlan,width,rows,first,last,origin);
        try {
            need26(a!=null&&e!=null,"valid policies");need26(a.mode==e.mode,"published representation mode");
            need26(a.rawFallback==e.rawFallback && a.pixels==e.pixels,"published policy metadata");
            for(int at=0;at<a.pixels;at++)for(int k=0;k<4;k++)
                need26(decode(a.mode,a.words,at,k)==decode(e.mode,e.words,at,k),"published exact policy integer");
            if(actualMask!=null) {
                need26(actualMask.calls==referenceMask.calls,"unknown mask original call count");
                need26(actualMask.sequence==referenceMask.sequence,"unknown mask original coordinate order");
                unknownCases++;
            }
            policyCases++;policyPixels+=a.pixels;
        } finally {if(a!=null)a.close();if(e!=null)e.close();}
    }
    private static void policies()throws Exception {
        SpatialNoise1934 local=map(SpatialNoise1934.class,512,384,3,3,-19.25f,-13.5f,41.75f,33.25f,
            new float[]{.1f,.41f,1.9f,2.37f,4.21f,8.77f,15.19f,19.27f,31.83f});
        SpatialNoise1934 extremes=map(SpatialNoise1934.class,512,384,3,3,0f,0f,8.75f,6.25f,
            new float[]{Float.MAX_VALUE,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,
            -Float.MAX_VALUE,1f,-2f,256f,4096f});
        byte[] skin=new byte[35],detail=new byte[35];
        for(int i=0;i<skin.length;i++)skin[i]=(byte)(i*71);
        FaceRegions1934.Mask known=FaceRegions1934.uprightRaster(97,79,7,5,skin,detail,0);
        int id=0;
        for(int noise=0;noise<=4;noise++)for(int sharp=0;sharp<=4;sharp++)for(int texture=0;texture<2;texture++)
        for(float scale:new float[]{.125f,.999998f,1f,1.000002f,2.7f})for(int mask=0;mask<8;mask++) {
            int current=id++;QualityPixels1932.Plan a=plan(noise,sharp,texture!=0,scale,current%8),e=a;
            if(current%4==1)a=e=a.withLocalNoise(local,noise);
            if(current%4==2)a=e=a.withOutputNoise(local);
            if(current%4==3)a=e=a.withOutputNoise(extremes);
            CustomMask am=null,em=null;
            if(mask==1)a=e=a.withFaceRegions(known);
            else if(mask==2)a=e=a.withFaceRegions(FaceRegions1934.empty(97,79));
            else if(mask>=3) {
                am=new CustomMask(mask-3);em=new CustomMask(mask-3);
                a=a.withFaceRegions(am);e=e.withFaceRegions(em);
            }
            compare(a,e,31,27,2,23,current%2==0?-14:19,am,em);
        }
        // Widths and late-row boundaries used by the real photo producer.
        for(int width:new int[]{1,2,127,4080})for(int origin:new int[]{-41,0,219}) {
            QualityPixels1932.Plan a=plan(4,4,true,1f,3).withOutputNoise(extremes).withFaceRegions(known);
            compare(a,a,width,37,3,35,origin,null,null);
        }
    }
    public static void main(String[] args)throws Exception {
        coordinates();policies();
        System.out.println("{\"status\":\"passed\",\"h24_assertions\":"+h24+",\"h26_assertions\":"+h26+
            ",\"maps\":"+maps+",\"coordinate_values\":"+coordinateValues+",\"policy_cases\":"+policyCases+
            ",\"policy_pixels\":"+policyPixels+",\"unknown_mask_cases\":"+unknownCases+"}");
    }
}
