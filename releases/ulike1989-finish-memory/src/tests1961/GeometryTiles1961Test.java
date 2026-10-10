package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Method;

/** Executes production JNI, tiled geometry and resident moire->geometry rather
 * than an emulated GPU. Original CPU pixel algorithms are independently pinned
 * by the main1961 harness. Each output pixel is compared, including tile seams. */
public final class GeometryTiles1961Test {
    private static long assertions,pixels;
    private static int cases,chains;
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static Bitmap input(int width,int height,int pattern) {
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        int[] row=new int[width];
        for(int y=0;y<height;y++) {
            for(int x=0;x<width;x++) {
                int g=pattern==0?(x*37+y*19+x*y*3)&255:130;
                int r=pattern==0?(x*11+y*43)&255:g+((x&3)<2?22:-22);
                int b=pattern==0?(x*67+y*7)&255:g-((x&3)<2?22:-22);
                row[x]=0xff000000|r<<16|g<<8|b;
            }
            bitmap.setPixels(row,0,width,0,y,width,1);
        }
        bitmap.setDensity(333);bitmap.setHasAlpha(true);return bitmap;
    }
    private static void equal(Bitmap expected,Bitmap actual,String message) {
        check(actual!=null,message+" absent candidate");
        check(expected.getWidth()==actual.getWidth()&&expected.getHeight()==actual.getHeight(),message+" dimensions");
        check(expected.getDensity()==actual.getDensity(),message+" density");
        check(expected.hasAlpha()==actual.hasAlpha(),message+" alpha metadata");
        int width=expected.getWidth(),height=expected.getHeight();int[] a=new int[width],b=new int[width];
        for(int y=0;y<height;y++) {
            expected.getPixels(a,0,width,0,y,width,1);actual.getPixels(b,0,width,0,y,width,1);
            for(int x=0;x<width;x++){assertions++;if(a[x]!=b[x])throw new AssertionError(message+" pixel "+x+","+y);pixels++;}
        }
    }
    public static void main(String[] args)throws Exception {
        check(GpuNoise1960.supports(GpuNoise1960.GEOMETRY),"actual geometry program");
        check(GpuNoise1960.supports(GpuNoise1960.FINISH1961),"actual finish program");
        Method tiled=GpuGeometry1960.class.getDeclaredMethod("gpuTiled",Bitmap.class,int.class,int.class,int.class,GpuPolicy1960.GeometryData.class);tiled.setAccessible(true);
        int[][] fixtures={{93,95,133,143},{97,101,47,49},{99,103,87,71},{65,67,1,1},
            {1,73,5,111},{71,1,103,7},{32,35,32,35},{67,65,65,67}};
        for(int[] size:fixtures)for(int rotation:new int[]{0,90,180,270}) {
            if(rotation==0&&size[0]==size[2]&&size[1]==size[3])continue;
            Bitmap source=input(size[0],size[1],0),expected=null,actual=null;
            try {
                expected=FastResize1933.resampleCpu1960(source,rotation,size[2],size[3]);
                actual=(Bitmap)tiled.invoke(null,source,rotation,size[2],size[3],GpuPolicy1960.geometry(size[0],size[1],rotation,size[2],size[3]));
                equal(expected,actual,"tilecase"+cases+" rotation"+rotation);cases++;
            } finally {if(expected!=null&&expected!=source)expected.recycle();if(actual!=null&&actual!=source)actual.recycle();source.recycle();}
        }
        for(int pattern=0;pattern<2;pattern++)for(int rotation:new int[]{0,90,180,270}) {
            Bitmap source=input(95,97,pattern),expected=null,actual=null;
            try {
                expected=GpuChain1961.cpu(source,rotation,131,139);
                actual=GpuChain1961.run(source,rotation,131,139);
                equal(expected,actual,"residentchain"+chains);chains++;
            } finally {if(expected!=null)expected.recycle();if(actual!=null)actual.recycle();source.recycle();}
        }
        // A complete24.5MP output proves production-sized tile coordinates and
        // seams. This is an exact-output test, not a phone speed measurement.
        if(args.length>0&&"large".equals(args[0])) {
            Bitmap source=input(4080,3060,1),expected=null,actual=null;
            try {
                expected=FastResize1933.resampleCpu1960(source,0,5712,4284);
                actual=(Bitmap)tiled.invoke(null,source,0,5712,4284,GpuPolicy1960.geometry(4080,3060,0,5712,4284));
                equal(expected,actual,"full24.5MP");cases++;
            } finally {if(expected!=null)expected.recycle();if(actual!=null)actual.recycle();source.recycle();}
            source=input(4080,3060,1);expected=null;actual=null;
            try {
                expected=GpuChain1961.cpu(source,0,5712,4284);
                actual=GpuChain1961.run(source,0,5712,4284);
                equal(expected,actual,"full24.5MPresidentchain");chains++;
            } finally {if(expected!=null)expected.recycle();if(actual!=null)actual.recycle();source.recycle();}
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"exactPixels\":"+pixels+",\"geometryCases\":"+cases+",\"residentChainCases\":"+chains+",\"physicalAndroidTested\":false}");
    }
}
