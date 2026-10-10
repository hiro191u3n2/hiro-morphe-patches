package com.hiro.ulike;

import java.util.Arrays;

/** Exact source-neighbourhood moire and final sharpening. Java prepares only
 * immutable float/mask policy; the native kernel performs the pixel arithmetic,
 * repeating-texture probe and hue-preserving final delta in one row pass. */
public final class NativeMoire1951 {
    private static final boolean LOADED=load();
    private static volatile int verified;
    private static volatile boolean cacheAvailable1976=true;
    private NativeMoire1951() {}

    private static boolean load() {
        try {
            System.loadLibrary("ulike_moire1951");
            return nativeAbi()==19512;
        } catch(LinkageError unavailable) { return false; }
          catch(SecurityException unavailable) { return false; }
    }
    static boolean run(int[] src,int[] dst,int width,int rows,int first,int last) {
        return run(src,dst,width,rows,first,last,null,true,false,0);
    }
    static boolean run(int[] src,int[] dst,int width,int rows,int first,int last,
            QualityPixels1932.Plan plan,boolean moire,boolean sharp,int originY) {
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
        try { return runUnchecked(src,dst,width,rows,first,last,plan,moire,sharp,originY); }
        catch(LinkageError unavailable) { verified=-1; return false; }
        catch(OutOfMemoryError optionalWorkspace) { SpeedWorkers1935.trim();return false; }
    }

    /** Preserve Java float evaluation/rounding exactly. C consumes only integers;
     * no fast-math, FMA, native interpolation or approximated mask is involved. */
    static void preparePolicy(QualityPixels1932.Plan plan,int x,int y,int[] policy,int at) {
        float sigma=plan.localNoise!=null && (plan.noiseMapAtOutput || Math.abs(plan.outputScale-1f)<.00001f)
            ?plan.localSigmaAt(x,y):plan.sourceSigma;
        if(!plan.noiseMapAtOutput && plan.outputScale<1f)sigma*=(float)Math.sqrt(plan.outputScale);
        policy[at]=plan.sharpTolerance1958(sigma,x,y);
        policy[at+1]=Math.max(3,Math.round(sigma*.75f));
        policy[at+2]=plan.sharpNoiseFloor1958(sigma,x,y);
        int skin=plan.texturePriority && plan.faceRegions!=null?plan.skinAt(x,y):0;
        // This calculation deliberately retains Java's signed-int wrap, even for
        // a custom RegionMask returning values outside the usual 0..256 range.
        policy[at+3]=256-(skin*(20+(plan.beautyQ8>>3))>>8);
    }
    private static boolean candidate(int[] src,int at,int width,int floor) {
        if((src[at]>>>24)!=255 || (src[at-1]>>>24)!=255 || (src[at+1]>>>24)!=255 ||
            (src[at-width]>>>24)!=255 || (src[at+width]>>>24)!=255 ||
            (src[at-width-1]>>>24)!=255 || (src[at-width+1]>>>24)!=255 ||
            (src[at+width-1]>>>24)!=255 || (src[at+width+1]>>>24)!=255)return false;
        int center=QualityPixels1932.luma(src[at]);
        int sides=QualityPixels1932.luma(src[at-1])+QualityPixels1932.luma(src[at+1])+
            QualityPixels1932.luma(src[at-width])+QualityPixels1932.luma(src[at+width]);
        int corners=QualityPixels1932.luma(src[at-width-1])+QualityPixels1932.luma(src[at-width+1])+
            QualityPixels1932.luma(src[at+width-1])+QualityPixels1932.luma(src[at+width+1]);
        return Math.abs((center*12-sides*2-corners)*16)>floor;
    }
    /** Optional heap-owned cache. Runtime heap accounting includes both leased
     * and pooled arrays; native scratch is subtracted before this admission.
     * At most 80 source rows / 2 MiB per worker, with the original kernel retained
     * when memory is tight or an older native library has no cache entry. */
    private static int[] borrowCache1976(int width,int rows,boolean moire,boolean sharp) {
        if(!cacheAvailable1976||(!moire&&!sharp))return null;
        long count=2+(long)width*Math.min(rows,16+(moire?64:8));
        if(count<2||count>Integer.MAX_VALUE||count*4+64>2L*1024*1024)return null;
        Runtime runtime=Runtime.getRuntime();
        long free=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
        long nativeBytes=SpeedWorkers1935.nativeRetainedBytes1956();
        if(nativeBytes>free||count*8+128>free-nativeBytes-64L*1024*1024)return null;
        try {
            // Query the current GPU/native/codec owners and pending reservations
            // without making this optional old native helper require their ABI.
            Class<?> owner=Class.forName("com.hiro.ulike.GpuNoise1960",false,NativeMoire1951.class.getClassLoader());
            java.lang.reflect.Method budget=owner.getDeclaredMethod("workspaceFits",long.class);
            budget.setAccessible(true);
            if(!Boolean.TRUE.equals(budget.invoke(null,Long.valueOf(count*8+128))))return null;
        } catch(ReflectiveOperationException unavailable){return null;}
          catch(LinkageError unavailable){return null;}
          catch(SecurityException unavailable){return null;}
        try {
            int[] cache=SpeedWorkers1935.borrowInts((int)count);
            cache[0]=-1;cache[1]=0;return cache;
        } catch(OutOfMemoryError optional) {SpeedWorkers1935.trim();return null;}
    }
    private static boolean strip1976(int[] src,int[] dst,int[] policy,boolean variable,
            int width,int rows,int first,int last,boolean moire,boolean sharp,
            int gain,int floor,int limit,boolean texture,boolean halo,int[] cache) {
        if(cache!=null&&cacheAvailable1976)try {
            if(finishStripCached1976(src,dst,policy,variable,width,rows,first,last,moire,sharp,
                    gain,floor,limit,texture,halo,cache))return true;
        } catch(UnsatisfiedLinkError oldNative) {cacheAvailable1976=false;}
        return finishStripNative(src,dst,policy,variable,width,rows,first,last,moire,sharp,
            gain,floor,limit,texture,halo);
    }
    private static boolean runUnchecked(int[] src,int[] dst,int width,int rows,int first,int last,
            QualityPixels1932.Plan plan,boolean moire,boolean requestedSharp,int originY) {
        boolean sharp=requestedSharp && plan!=null && plan.sharpGainQ8>0;
        boolean variable=sharp && ((plan.localNoise!=null &&
            (plan.noiseMapAtOutput || Math.abs(plan.outputScale-1f)<.00001f)) ||
            (plan.texturePriority && plan.faceRegions!=null) || plan.smoothedRegions!=null);
        int[] policy=null,cache=null;
        int gain=sharp?plan.sharpGainQ8:0,floor=sharp?plan.sharpFloorQ8:0,limit=sharp?plan.sharpLimit:0;
        boolean texture=sharp&&plan.texturePriority,halo=sharp&&plan.haloSuppression;
        String key=CpuFinishCache1976.key(width,rows,first,last,variable,moire,sharp,gain,floor,limit,texture,halo);
        boolean cached=CpuFinishCache1976.enabled(key);CpuFinishCache1976.Snapshot snapshot=null;
        try {
            if(cached)cache=borrowCache1976(width,rows,moire,sharp);
            else if(verified>0)snapshot=CpuFinishCache1976.capture(key,src,width,rows,first,last,variable,moire,sharp,gain,floor,limit,texture,halo);
            if(sharp) {
                long size=variable?(long)width*Math.min(16,last-first)*4:4;
                if(size>Integer.MAX_VALUE)return false;
                policy=SpeedWorkers1935.borrowInts((int)size);
                if(!variable)preparePolicy(plan,0,originY,policy,0);
            }
            int start=first;
            while(start<last) {
                if((start&15)==0 && Thread.currentThread().isInterrupted())
                    throw new IllegalStateException("quality interrupted");
                int end=Math.min(last,(start+16)&~15);
                if(end<=start)end=Math.min(last,start+16);
                if(variable)for(int row=start;row<end;row++) {
                    if(row==0 || row==rows-1)continue;
                    for(int x=1;x<width-1;x++) {
                        int at=row*width+x;
                        if(candidate(src,at,width,plan.sharpFloorQ8))
                            preparePolicy(plan,x,row+originY,policy,((row-start)*width+x)*4);
                    }
                }
                if(snapshot!=null)snapshot.record(start,end,policy);
                if(!strip1976(src,dst,policy,variable,width,rows,start,end,moire,sharp,
                        sharp?plan.sharpGainQ8:0,sharp?plan.sharpFloorQ8:0,
                        sharp?plan.sharpLimit:0,sharp && plan.texturePriority,
                        sharp && plan.haloSuppression,cache))return false;
                start=end;
            }
            if(snapshot!=null){snapshot.queue();snapshot=null;}
            return true;
        } finally {if(snapshot!=null)snapshot.close();SpeedWorkers1935.release(policy);SpeedWorkers1935.release(cache);}
    }
    /** Execute exactly the frozen JNI bands, including cache admission/release;
     * quality policy values have already been copied from the original call. */
    static boolean proof1976(CpuFinishCache1976.Snapshot proof,int[] output,boolean cached) {
        int[] cache=null;
        try {
            if(cached){cache=borrowCache1976(proof.width,proof.rows,proof.moire,proof.sharp);if(cache==null)return false;}
            for(int i=0;i<proof.recorded;i++) {
                if(Thread.currentThread().isInterrupted())return false;
                boolean okay=cached?finishStripCached1976(proof.source,output,proof.policies[i],proof.variable,
                    proof.width,proof.rows,proof.starts[i],proof.ends[i],proof.moire,proof.sharp,
                    proof.gain,proof.floor,proof.limit,proof.texture,proof.halo,cache):
                    finishStripNative(proof.source,output,proof.policies[i],proof.variable,
                    proof.width,proof.rows,proof.starts[i],proof.ends[i],proof.moire,proof.sharp,
                    proof.gain,proof.floor,proof.limit,proof.texture,proof.halo);
                if(!okay)return false;
            }
            return true;
        } finally {SpeedWorkers1935.release(cache);}
    }
    private static boolean selfCheck() {
        final int width=91,rows=93,n=width*rows;
        int[] src=new int[n],expected=new int[n],actual=new int[n];
        for(int mode=0;mode<7;mode++) {
            for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
                int value=(x*11+y*7+x*y*3)&255;
                int r,g,b;
                if(mode==0) {r=value;g=(value*3+13)&255;b=(value*5+37)&255;}
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
                QualityPixels1932.Plan plan=QualityPixels1932.plan(
                    new QualityPixels1932.NoiseStats(.4f+mode*.2f,.6f,128f,0f,96),
                    100,10000000L,QualityPixels1932.LENS_WIDE,.7f,2,phase+2,true,false,
                    phase==1?.8f:1f).withHaloSuppression(phase!=2);
                if(phase==2)plan=plan.withFaceRegions(new QualityPixels1932.RegionMask() {
                    public int skinQ8(int x,int y){return (x*7+y*13)&255;}
                    public int detailQ8(int x,int y){return 0;}
                });
                Arrays.fill(expected,0x13579bdf);Arrays.fill(actual,0x13579bdf);
                QualityPixels1932.finishStripAtBefore1951(src,expected,width,rows,first,last,plan,true,true,37);
                if(!runUnchecked(src,actual,width,rows,first,last,plan,true,true,37) || !Arrays.equals(expected,actual))return false;
            }
        }
        return true;
    }
    static boolean testNative(int[] src,int[] dst,int width,int rows,int first,int last) {
        return run(src,dst,width,rows,first,last);
    }
    static boolean testNativeFinish(int[] src,int[] dst,int width,int rows,int first,int last,
            QualityPixels1932.Plan plan,boolean moire,boolean sharp,int originY) {
        return run(src,dst,width,rows,first,last,plan,moire,sharp,originY);
    }
    private static native boolean finishStripCached1976(int[] src,int[] dst,int[] policy,boolean variable,
            int width,int rows,int first,int last,boolean moire,boolean sharp,
            int gain,int floor,int limit,boolean texturePriority,boolean haloSuppression,int[] cache);
    private static native int nativeAbi();
    private static native boolean finishStripNative(int[] src,int[] dst,int[] policy,boolean variable,
            int width,int rows,int first,int last,boolean moire,boolean sharp,
            int gain,int floor,int limit,boolean texturePriority,boolean haloSuppression);
}
