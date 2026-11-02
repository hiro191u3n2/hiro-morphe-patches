package com.hiro.ulike;

import android.graphics.Bitmap;
import java.io.*;

/** Compiled and executed in a separate JVM against hash-pinned published .60
 * CPU sources. Current chain code is never available to this oracle. */
public final class Chain1962Oracle {
    static int[][] dimensions={{95,97,95,97},{95,97,131,139},{97,101,47,49},{99,103,87,71},{32,35,35,32},{513,389,521,411}};
    static Bitmap image(int width,int height,int pattern) {
        Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);int[] p=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int base=130,r,g,blue;
            if(pattern==0){r=(x*11+y*43)&255;g=(x*37+y*19+x*y*3)&255;blue=(x*67+y*7)&255;}
            else if(pattern==1){g=base;r=g+((x&3)<2?22:-22);blue=g-((x&3)<2?22:-22);}
            else {int grain=((x*7+y*11)%13)-6;g=base+grain;r=g+((x/4+y/4)&1)*7;blue=g-((x/4+y/4)&1)*7;}
            p[y*width+x]=0xff000000|r<<16|g<<8|blue;
        }
        b.setPixels(p,0,width,0,0,width,height);b.setDensity(333);b.setHasAlpha(true);return b;
    }
    static SpatialNoise1934 noise(final Bitmap image) {
        SpatialNoise1934.Patches source=new SpatialNoise1934.Patches(){public void read(int[] p,int x,int y,int w,int h){image.getPixels(p,0,w,x,y,w,h);}};
        try {
            java.lang.reflect.Method cpu=SpatialNoise1934.class.getDeclaredMethod("probeCpu1961",SpatialNoise1934.Patches.class,int.class,int.class);
            cpu.setAccessible(true);return (SpatialNoise1934)cpu.invoke(null,source,image.getWidth(),image.getHeight());
        } catch(NoSuchMethodException frozenCpu){return SpatialNoise1934.probe(source,image.getWidth(),image.getHeight());}
          catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    static QualityPixels1932.Plan plan(Bitmap source,int width,int height,int mask) {
        SpatialNoise1934 evidence=noise(source);
        QualityPixels1932.Plan p=QualityPixels1932.plan(evidence.global,800,16666666L,0,35,4,4,true,true,Math.max((float)width/source.getWidth(),(float)height/source.getHeight()))
            .withHaloSuppression(true).withOutputNoise(evidence);
        if(mask!=0) {
            p=p.withFaceRegions(new QualityPixels1932.RegionMask(){
                public int skinQ8(int x,int y){return ((x/8+y/7)%5)*57;}
                public int detailQ8(int x,int y){return (x*13+y*9)&255;}
            }).withSmoothedRegions(new QualityPixels1932.SmoothMask(){
                public int smoothingQ8(int x,int y){return ((x/4+y/4)&1)==0?240:17;}
            });
        }
        return p;
    }
    static Bitmap cpu(Bitmap source,int rotation,int width,int height,QualityPixels1932.Plan p,boolean refresh) {
        int sw=source.getWidth(),sh=source.getHeight();int[] in=new int[sw*sh],out=new int[in.length];source.getPixels(in,0,sw,0,0,sw,sh);
        QualityPixels1932.finishStripAtBefore1951(in,out,sw,sh,0,sh,null,true,false,0);
        Bitmap corrected=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);corrected.setPixels(out,0,sw,0,0,sw,sh);corrected.setDensity(source.getDensity());corrected.setHasAlpha(source.hasAlpha());
        Bitmap geometry=FastResize1933.resampleCpu1960(corrected,rotation,width,height);
        if(geometry!=corrected)corrected.recycle();
        if(refresh)p=p.withOutputNoise(noise(geometry));
        in=new int[width*height];out=new int[in.length];geometry.getPixels(in,0,width,0,0,width,height);
        QualityPixels1932.finishStripAtBefore1951(in,out,width,height,0,height,p,false,true,0);geometry.setPixels(out,0,width,0,0,width,height);return geometry;
    }
    static int[] pixels(Bitmap b){int w=b.getWidth(),h=b.getHeight();int[] p=new int[w*h];b.getPixels(p,0,w,0,0,w,h);return p;}
    public static void main(String[] args)throws Exception {
        int records=dimensions.length*4*3*2*2;
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])))) {
            out.writeInt(19624);out.writeInt(records);
            for(int[] d:dimensions)for(int rotation:new int[]{0,90,180,270})for(int pattern=0;pattern<3;pattern++)for(int mask=0;mask<2;mask++)for(boolean refresh:new boolean[]{false,true}) {
                Bitmap source=image(d[0],d[1],pattern),result=null;
                try {
                    result=cpu(source,rotation,d[2],d[3],plan(source,d[2],d[3],mask),refresh);
                    for(int n:d)out.writeInt(n);out.writeInt(rotation);out.writeInt(pattern);out.writeInt(mask);out.writeBoolean(refresh);
                    int[] p=pixels(result);out.writeInt(p.length);for(int pixel:p)out.writeInt(pixel);
                } finally {if(result!=null)result.recycle();source.recycle();}
            }
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+records+",\"records\":"+records+",\"frozenCpuOracle\":true}");
    }
}
