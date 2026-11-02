package com.hiro.ulike;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Random;

/** Compiled against the pinned v1.9.50 sources, not the edited implementation. */
public final class MoireOracle1951 {
    private MoireOracle1951() {}
    private static QualityPixels1932.Plan makePlan(final int mode,int phase,int level,
            final int width,final int rows,final int[] source) {
        int choice=(mode+phase+level)%8;
        float sigma=choice==0?0f:choice==1?.22f:choice==2?1.7f:choice==3?6.3f:2.8f;
        float scale=choice==4?.5f:choice==5?2f:choice==6?.999995f:1f;
        QualityPixels1932.Plan plan=QualityPixels1932.plan(
            new QualityPixels1932.NoiseStats(sigma,sigma*.7f,128f,.1f,144),
            choice==3?3200:100,choice==5?60000000L:9991324L,
            choice==5?QualityPixels1932.LENS_TELE:QualityPixels1932.LENS_WIDE,
            (mode%5)*.25f,4,level,(mode&1)!=0,false,scale).withHaloSuppression((phase&1)==0);
        if(choice>=4)plan=plan.withFaceRegions(new QualityPixels1932.RegionMask() {
            public int skinQ8(int x,int y) {
                int value=x*19+y*7+mode;
                // Include custom non-Q8 masks and Java signed-int-wrap semantics.
                return mode==12?Integer.MAX_VALUE:mode==13?-513:value&511;
            }
            public int detailQ8(int x,int y){return (x+y*3)&255;}
        });
        if(choice==6 || choice==7) {
            SpatialNoise1934 noise=SpatialNoise1934.probe(new SpatialNoise1934.Patches() {
                public void read(int[] pixels,int x,int y,int w,int h) {
                    for(int row=0;row<h;row++)System.arraycopy(source,(y+row)*width+x,pixels,row*w,w);
                }
            },width,rows);
            plan=choice==6?plan.withLocalNoise(noise,4):plan.withOutputNoise(noise);
        }
        return plan;
    }
    private static void policy(QualityPixels1932.Plan plan,int x,int y,int[] values,int at) {
        float sigma=plan.localNoise!=null && (plan.noiseMapAtOutput || Math.abs(plan.outputScale-1f)<.00001f)
            ?plan.localSigmaAt(x,y):plan.sourceSigma;
        if(!plan.noiseMapAtOutput && plan.outputScale<1f)sigma*=(float)Math.sqrt(plan.outputScale);
        values[at]=3+Math.round(sigma*.65f);
        values[at+1]=Math.max(3,Math.round(sigma*.75f));
        values[at+2]=Math.max(plan.sharpFloorQ8,Math.round((1f+sigma*1.8f)*256f));
        int skin=plan.texturePriority && plan.faceRegions!=null?plan.skinAt(x,y):0;
        values[at+3]=256-(skin*(20+(plan.beautyQ8>>3))>>8);
    }
    private static void write(DataOutputStream file,int width,int rows,int first,int last,
            int[] src,QualityPixels1932.Plan plan,boolean moire,boolean sharp,int originY) throws Exception {
        int[] reference=new int[src.length];Arrays.fill(reference,0x13579bdf);
        QualityPixels1932.finishStripAt(src,reference,width,rows,first,last,plan,moire,sharp,originY);
        boolean active=sharp && plan!=null && plan.sharpGainQ8>0;
        int[] settings=new int[Math.max(1,(last-first)*width)*4];
        if(active)for(int y=first;y<last;y++)for(int x=0;x<width;x++)
            policy(plan,x,y+originY,settings,((y-first)*width+x)*4);
        file.writeInt(width);file.writeInt(rows);file.writeInt(first);file.writeInt(last);
        file.writeInt(moire?1:0);file.writeInt(active?1:0);
        file.writeInt(active?plan.sharpGainQ8:0);file.writeInt(active?plan.sharpFloorQ8:0);
        file.writeInt(active?plan.sharpLimit:0);file.writeInt(active && plan.texturePriority?1:0);
        file.writeInt(active && plan.haloSuppression?1:0);file.writeInt(originY);
        for(int pixel:src)file.writeInt(pixel);
        for(int pixel:reference)file.writeInt(pixel);
        for(int value:settings)file.writeInt(value);
    }
    public static void main(String[] args) throws Exception {
        Random random=new Random(0x195135516L);
        try(DataOutputStream file=new DataOutputStream(new FileOutputStream(args[0]))) {
            for(int mode=0;mode<14;mode++) {
                int width=mode==0?7:mode==1?40:mode==2?65:mode==3?91:119;
                int rows=mode==0?9:mode==1?52:mode==2?66:mode==3?93:103;
                final int[] src=new int[width*rows];
                for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
                    int red,green,blue,randomValue=random.nextInt(256);
                    switch(mode%7) {
                    case 0: red=randomValue;green=random.nextInt(256);blue=random.nextInt(256);break;
                    case 1: green=121;red=green+((x&1)==0?24:-24);blue=green-((x&1)==0?24:-24);break;
                    case 2: green=129;red=green+((y&3)<2?26:-26);blue=green-((y&3)<2?26:-26);break;
                    case 3: green=122;red=green+(int)Math.round(19*Math.sin(2*Math.PI*x/16));blue=green-(red-green);break;
                    case 4: green=148;red=green+(int)Math.round(22*Math.sin(2*Math.PI*y/12));blue=green-(red-green);break;
                    case 5: green=128;red=green+((x+y)%8<4?14:-14);blue=green+(x%3-1)*4;break;
                    default: green=red=blue=127+random.nextInt(5);break;
                    }
                    int alpha=random.nextInt(67)==0?random.nextInt(255):255;
                    src[y*width+x]=(alpha<<24)|(red<<16)|(green<<8)|blue;
                }
                for(int phase=0;phase<3;phase++) {
                    int first=phase==0?0:phase==1?Math.min(5,rows):Math.min(32,rows);
                    int last=phase==0?rows:phase==1?Math.max(first,rows-6):Math.min(rows,63);
                    for(int level=0;level<=4;level++)for(int moire=0;moire<2;moire++) {
                        QualityPixels1932.Plan plan=makePlan(mode,phase,level,width,rows,src);
                        write(file,width,rows,first,last,src,plan,moire==1,true,phase==0?0:phase==1?71:-23);
                    }
                }
            }
        }
    }
}
