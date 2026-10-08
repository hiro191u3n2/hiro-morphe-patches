package com.hiro.ulike;

import java.util.Arrays;

/**
 * Exact ARM64 implementation of the existing period-2/4 and long-wave chroma
 * correction. The source is read only, and the final sharp pass still reads
 * the original processed neighbourhood and receives the corrected pixel.
 */
public final class NativeMoire1951 {
    private static final boolean LOADED=load();
    private static volatile int verified;
    private NativeMoire1951() {}

    private static boolean load() {
        try {
            System.loadLibrary("ulike_moire1951");
            return nativeAbi()==1951;
        } catch(LinkageError unavailable) { return false; }
          catch(SecurityException unavailable) { return false; }
    }
    static boolean run(int[] src,int[] dst,int width,int rows,int first,int last) {
        if(!LOADED || src==null || dst==null || src==dst ||
            width<=0 || rows<=0 || first<0 || last<first || last>rows ||
            (long)width*rows>src.length || (long)width*rows>dst.length)return false;
        if(verified==0) {
            synchronized(NativeMoire1951.class) {
                if(verified==0) {
                    try { verified=selfCheck()?1:-1; }
                    catch(Throwable unavailable) { verified=-1; }
                }
            }
        }
        if(verified<0)return false;
        try { return runUnchecked(src,dst,width,rows,first,last); }
        catch(LinkageError unavailable) { verified=-1; return false; }
    }
    private static boolean runUnchecked(int[] src,int[] dst,int width,int rows,int first,int last) {
        int start=first;
        while(start<last) {
            if((start&15)==0 && Thread.currentThread().isInterrupted())
                throw new IllegalStateException("quality interrupted");
            int end=Math.min(last,(start+16)&~15);
            // Preserve Java's interruption check at exactly every 16th row,
            // even if the caller starts inside an unaligned strip.
            if(end<=start)end=Math.min(last,start+16);
            if(!moireStripNative(src,dst,width,rows,start,end))return false;
            start=end;
        }
        return true;
    }
    private static boolean selfCheck() {
        final int width=91,rows=93,n=width*rows;
        int[] src=new int[n],expected=new int[n],actual=new int[n];
        for(int mode=0;mode<7;mode++) {
            for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
                int v=(x*11+y*7+x*y*3)&255;
                int r,g,b;
                if(mode==0) {r=v;g=(v*3+13)&255;b=(v*5+37)&255;}
                else if(mode==1) {r=g=b=132;}
                else if(mode==2) {g=148;r=g+((x&1)==0?25:-25);b=g-((x&1)==0?25:-25);}
                else if(mode==3) {g=140;r=g+((y&3)<2?22:-22);b=g-((y&3)<2?22:-22);}
                else if(mode==4) {g=125;r=g+(int)Math.round(19*Math.sin(2*Math.PI*x/16));b=g-(r-g);}
                else if(mode==5) {g=128;r=g+((x+y)%12<6?13:-13);b=g+(x%3-1)*5;}
                else {g=130;r=220;b=35;}
                int a=(((x*3+y*17)%101)==0)?127:255;
                src[y*width+x]=(a<<24)|(r<<16)|(g<<8)|b;
            }
            for(int phase=0;phase<3;phase++) {
                int first=phase==0?0:phase==1?9:33;
                int last=phase==0?rows:phase==1?rows-7:79;
                Arrays.fill(expected,0x13579bdf);
                Arrays.fill(actual,0x13579bdf);
                QualityPixels1932.finishStripAtBefore1951(src,expected,width,rows,first,last,null,true,false,0);
                if(!runUnchecked(src,actual,width,rows,first,last) || !Arrays.equals(expected,actual))
                    return false;
            }
        }
        return true;
    }
    static boolean testNative(int[] src,int[] dst,int width,int rows,int first,int last) {
        return run(src,dst,width,rows,first,last);
    }
    private static native int nativeAbi();
    private static native boolean moireStripNative(int[] src,int[] dst,int width,int rows,int first,int last);
}
