package com.hiro.ulike;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

/** Real production shader6 against the unchanged finishing-policy owner and a
 * byte-frozen v1.9.60 face-raster source, never approximate/sample mask equality. */
public final class Protection1961Test {
    private static long assertions,values;
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    private static void equal(int[] expected,int[] actual,String label) {
        check(actual!=null&&actual.length==expected.length,label+" length");
        for(int i=0;i<expected.length;i++){values++;check(actual[i]==expected[i],label+" index="+i+" expected="+expected[i]+" actual="+actual[i]);}
    }
    private static Object field(Object source,String name)throws Exception{Field f=source.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(source);}
    private static int[] pixels(int w,int h,int kind,int seed) {
        Random random=new Random(seed);int[] source=new int[w*h];
        for(int i=0;i<source.length;i++) {
            int r,g,b;
            if(kind==0){r=147+random.nextInt(5);g=112+random.nextInt(5);b=91+random.nextInt(5);}
            else if(kind==1){r=random.nextInt(256);g=random.nextInt(256);b=random.nextInt(256);}
            else {r=g=b=27+i%9;}
            source[i]=0xff000000|(r<<16)|(g<<8)|b;
            if(kind==1&&i%17==0)source[i]&=0xffffff;
        }
        return source;
    }
    private static void faces() {
        for(int kind=0;kind<3;kind++)for(int phase=0;phase<4;phase++) {
            int w=193,h=177;int[] source=pixels(w,h,kind,196100+phase*3+kind);
            FaceRegions1934Pixels.Anchor[] anchors=new FaceRegions1934Pixels.Anchor[]{
                new FaceRegions1934Pixels.Anchor(76.3f,72.1f,31.7f,phase==0?0:phase==1?25:-24.9f,phase==2?20:0,.60f),
                new FaceRegions1934Pixels.Anchor(151.6f,68.9f,22.3f,-7.3f,0,.91f),
                phase==3?new FaceRegions1934Pixels.Anchor(79f,72f,30f,0,0,.8f):null};
            ReferenceFacePixels1960.Anchor[] referenceAnchors=new ReferenceFacePixels1960.Anchor[anchors.length];
            for(int i=0;i<anchors.length;i++)if(anchors[i]!=null){FaceRegions1934Pixels.Anchor a=anchors[i];referenceAnchors[i]=new ReferenceFacePixels1960.Anchor(a.x,a.y,a.eyes,a.roll,a.yaw,a.confidence);}
            ReferenceFacePixels1960.Raster reference=ReferenceFacePixels1960.build(source,w,h,referenceAnchors);
            int[][] geometry=FaceRegions1934Pixels.geometry1961(w,h,anchors);
            check(geometry.length>0,"accepted faces");int[] joined=new int[source.length*geometry.length];
            for(int i=0;i<geometry.length;i++)System.arraycopy(geometry[i],0,joined,i*source.length,source.length);
            int[] u=new int[32];u[1]=w;u[2]=h;u[3]=geometry.length;u[4]=source.length;
            int[] actual=GpuProtection1961.testCandidate(new int[][]{source,new int[]{0},joined,new int[]{0}},null,u,null,source.length*2);
            int[] expected=new int[source.length*2];for(int i=0;i<source.length;i++){expected[i*2]=reference.skin[i]&255;expected[i*2+1]=reference.detail[i]&255;}
            equal(expected,actual,"frozen face kind"+kind+" phase"+phase);
        }
    }
    private static void smoothing() {
        Random random=new Random(196113);
        for(int w:new int[]{1,3,4,5,17,65})for(int owned:new int[]{1,3,4,7,12})for(int phase=0;phase<3;phase++) {
            int begin=4,end=begin+owned,stripRows=end+2,origin=4,fullHeight=end+origin;
            int columns=(w+3)/4,cells=columns*((owned+3)/4);
            int[] source=pixels(w,stripRows,0,w+owned),policy=new int[w*owned*2],confidence=new int[cells];
            for(int i=0;i<policy.length;i++)policy[i]=phase==0?(i%2==0?256:0):random.nextInt(257);
            for(int i=0;i<cells;i++)confidence[i]=random.nextInt(257);
            if(phase==2)for(int i=0;i<source.length;i+=7)source[i]&=0xffffff;
            int[] u=new int[32];u[0]=1;u[1]=w;u[2]=stripRows;u[3]=begin;u[4]=end;u[5]=origin;u[6]=fullHeight;u[7]=columns;u[8]=cells;
            int[] actual=GpuProtection1961.testCandidate(new int[][]{source,policy,confidence,new int[]{0}},null,u,null,cells);
            equal(GpuProtection1961.smoothReference(source,w,stripRows,begin,end,origin,fullHeight,policy,confidence),actual,"smooth "+w+"x"+owned+" phase"+phase);
        }
    }
    private static void finishing()throws Exception {
        for(int phase=0;phase<8;phase++) {
            final int w=71,h=83,first=3,last=39,origin=17;
            final int p=phase;final int[] source=pixels(w,h,phase%2,196171+phase);
            SpatialNoise1934 noise=SpatialNoise1934.probe(new SpatialNoise1934.Patches(){public void read(int[] target,int x,int y,int width,int height){for(int r=0;r<height;r++)System.arraycopy(source,(y+r)*w+x,target,r*width,width);}},w,h);
            QualityPixels1932.Plan plan=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(.2f+phase*1.21f,.8f,123f,.1f,96),700,10000000L,QualityPixels1932.LENS_WIDE,.7f,3,4,true,true,phase%3==0?.63f:1f)
                .withOutputNoise(noise).withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){return p==7?((x&1)==0?Integer.MAX_VALUE:Integer.MIN_VALUE):(x*17+y*7)%257;}public int detailQ8(int x,int y){return 0;}})
                .withSmoothedRegions(new QualityPixels1932.SmoothMask(){public int smoothingQ8(int x,int y){return (x*31+y*19)%257;}});
            int count=w*(last-first),u[]=new int[32];float[] f=new float[32];int[] masks=new int[count*2],expected=new int[count*4];
            u[0]=2;u[1]=w;u[2]=last-first;u[3]=first+origin;u[4]=noise.columns;u[5]=noise.rows;u[6]=1;u[7]=plan.sharpFloorQ8;
            u[8]=Math.max(2,Math.round(Math.min(32f,plan.sourceSigma)*.85f));u[9]=Math.round((2f+Math.min(32f,plan.sourceSigma)*2.4f)*256f);u[10]=20+(plan.beautyQ8>>3);
            f[0]=plan.sourceSigma;f[1]=1;f[2]=(Float)field(noise,"left");f[3]=(Float)field(noise,"top");f[4]=(Float)field(noise,"stepX");f[5]=(Float)field(noise,"stepY");
            for(int y=first;y<last;y++)for(int x=0;x<w;x++){int i=(y-first)*w+x;masks[i*2]=plan.skinAt(x,y+origin);masks[i*2+1]=plan.smoothedRegions.smoothingQ8(x,y+origin);NativeMoire1951.preparePolicy(plan,x,y+origin,expected,i*4);}
            int[] actual=GpuProtection1961.testCandidate(new int[][]{new int[]{0},new int[]{0},new int[]{0},masks},((float[])field(noise,"sigma")).clone(),u,f,count*4);
            equal(expected,actual,"finish phase"+phase);
            // Unknown masks preserve the original call contract in the production wrapper.
            check(GpuProtection1961.finishBand(plan,w,h,first,last,origin)==null,"custom mask declines");
        }
    }
    public static void main(String[] args)throws Exception {
        check(GpuNoise1960.supports(6),"production GLES protection shader");faces();smoothing();finishing();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"values\":"+values+",\"shader\":6,\"faceOracle\":\"frozen-v1.9.60\"}");
    }
}
