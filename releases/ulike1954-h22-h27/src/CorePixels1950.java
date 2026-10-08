package com.hiro.ulike;

/** H1/H7: the actual source-domain six-neighbour bilateral kernel, retaining the
 * original integer ranges, rounding, tap order, opacity and texture/shadow rules.
 * The existing stage scheduler, sharpening, masks and output depth are untouched.
 */
public final class CorePixels1950 {
    private static final int[] LUMA = ranges(new int[]{1,12,18,26,34});
    private static final int[] COLOUR = ranges(new int[]{1,18,28,38,48});
    private static final boolean LOADED = load();
    private static volatile int verified;
    private static final boolean GPU_LOADED = loadGpu();
    private static volatile int gpuVerified;
    // Stage 0 and 1 are suppressed only while this thread executes the exact
    // preserved filter after a successful two-pass GPU denoise. Stage 2 and 3
    // (packing and sharpening) always execute their original DEX bodies.
    private static final ThreadLocal<Boolean> GPU_PAIR = new ThreadLocal<Boolean>();
    private static volatile int gpuAdmission; // 0 sampling, 1 fast, -1 slow
    private static volatile long cpuFilterNanos;
    private static volatile long cpuFilterPixels;
    private static volatile long gpuAdmissionKey;
    private CorePixels1950() { }
    private static boolean load() {
        try { System.loadLibrary("ulike_core1950"); return nativeAbi()==1950; }
        catch(LinkageError absent) { return false; }
        catch(SecurityException absent) { return false; }
    }
    private static boolean loadGpu() {
        try { System.loadLibrary("ulike_h8gpu1951"); return bilateralPairGpuAbi()==1951; }
        catch(LinkageError absent) { return false; }
        catch(SecurityException absent) { return false; }
    }
    private static int[] ranges(int[] sigma) {
        int[] table=new int[5*256];
        for(int level=1;level<=4;level++)for(int i=0;i<256;i++)
            table[level*256+i]=(int)Math.round(StrictMath.exp(-(double)i*i/(2*sigma[level]*sigma[level]))*256.0);
        return table;
    }
    private static boolean ready() {
        if(!LOADED)return false;
        if(verified!=0)return verified>0;
        synchronized(CorePixels1950.class) {
            if(verified==0) {
                try { verified=selfCheck()?1:-1; }
                catch(Throwable unavailable) { verified=-1; }
            }
            return verified>0;
        }
    }
    /** Called from DetailSerial186's private bilateral boundary. A failed native
     * admission returns to the preserved original method, including exceptions. */
    public static void bilateral(int[] guide,int[] values,int[] output,int width,int rows,
            int noise,boolean horizontal,boolean texture,boolean shadows,int begin,int end) {
        if(ready() && run(guide,values,output,width,rows,noise,horizontal,texture,shadows,begin,end))return;
        DetailSerial186.bilateralBefore1950(guide,values,output,width,rows,noise,
            horizontal,texture,shadows,begin,end);
    }
    /** The production filter boundary is preserved as filterBeforeH8. GPU
     * admission is limited to the positive-noise, fully sized two-pass case;
     * malformed arguments follow the production exception path unchanged. */
    public static void filter(DetailPixels.Work work,int width,int rows,int first,int count,
            int noise,int sharp,boolean texture,boolean halos,boolean shadows) {
        long pixels=(long)width*count;
        if(width>0 && rows>0 && noise>0 && noise<=4) {
            long key=((((((long)width*31+noise)*31+sharp)*2+(texture?1:0))*2+
                    (halos?1:0))*2+(shadows?1:0));
            if(gpuAdmissionKey!=key) synchronized(CorePixels1950.class) {
                if(gpuAdmissionKey!=key) {
                    cpuFilterNanos=0;cpuFilterPixels=0;gpuAdmission=0;gpuAdmissionKey=key;
                }
            }
        }
        if(GPU_LOADED && gpuAdmission==0 && cpuFilterNanos==0 &&
                width>0 && rows>0 && first>=0 && count>0 && (long)first+count<=rows &&
                pixels>=131072 && noise>0) {
            if(!ready())gpuAdmission=-1;
            long baseline=System.nanoTime();
            DetailSerial186.filterBeforeH8(work,width,rows,first,count,noise,sharp,
                    texture,halos,shadows);
            cpuFilterNanos=System.nanoTime()-baseline;
            cpuFilterPixels=pixels;
            if(gpuAdmission==0 && !gpuReady())gpuAdmission=-1;
            return;
        }
        long started=System.nanoTime();
        if(!gpuPair(work,width,rows,first,(long)first+count,noise,sharp,texture,shadows)) {
            long cpuStarted=System.nanoTime();
            DetailSerial186.filterBeforeH8(work,width,rows,first,count,noise,sharp,
                    texture,halos,shadows);
            if(GPU_LOADED && noise>0 && gpuAdmission==0 && pixels>=131072) {
                cpuFilterNanos=System.nanoTime()-cpuStarted;
                cpuFilterPixels=pixels;
            }
            return;
        }
        Boolean previous=GPU_PAIR.get();GPU_PAIR.set(Boolean.TRUE);
        try { DetailSerial186.filterBeforeH8(work,width,rows,first,count,noise,sharp,
                texture,halos,shadows); }
        finally { if(previous==null)GPU_PAIR.remove();else GPU_PAIR.set(previous); }
        long cpu=cpuFilterNanos;
        if(gpuAdmission==0 && cpu>0 && cpuFilterPixels>0 && pixels>=131072) {
            // Both times include the same production packing and sharpening.
            long elapsed=System.nanoTime()-started;
            gpuAdmission=elapsed*10*cpuFilterPixels<cpu*8*pixels?1:-1;
        }
    }
    /** Called by the original filter's stage invocations through H8Hooks. */
    public static void stage(DetailSerial186.Context context,int phase,int first,int last) {
        if((phase==0 || phase==1) && Boolean.TRUE.equals(GPU_PAIR.get()))return;
        DetailSerial186.stageBeforeH8(context,phase,first,last);
    }
    private static boolean gpuPair(DetailPixels.Work work,int width,int rows,int first,long last,
            int noise,int sharp,boolean texture,boolean shadows) {
        if(!GPU_LOADED || !ready() || gpuAdmission<0 || work==null ||
                work.source==null || work.denoised==null || work.horizontal==null ||
                work.output==null || width<1 || rows<1 || noise<1 || noise>4 ||
                first<0 || last>rows || first>=last ||
                (long)width*rows>Integer.MAX_VALUE ||
                work.source.length<(long)width*rows ||
                work.denoised.length<(long)width*rows ||
                work.horizontal.length<(long)width*rows ||
                work.output.length<(long)width*rows)return false;
        if(!gpuReady())return false;
        // The preserved filter denoises and packs four additional rows before
        // sharpening. The two-pass GPU endpoint must produce that same vertical
        // interval; its own horizontal support adds a further three rows.
        int denoiseFirst=sharp>0?Math.max(0,first-4):first;
        int denoiseLast=sharp>0?(int)Math.min((long)rows,last+4):(int)last;
        boolean successful;
        try { successful=bilateralPairGpuNative(work.source,work.denoised,work.horizontal,width,rows,
                noise,texture,shadows,denoiseFirst,denoiseLast,LUMA,COLOUR); }
        catch(LinkageError badNative) { gpuVerified=-1;return false; }
        catch(OutOfMemoryError unavailable) { return false; }
        if(!successful){gpuAdmission=-1;return false;}
        return true;
    }
    private static boolean gpuReady() {
        if(gpuVerified!=0)return gpuVerified>0;
        synchronized(CorePixels1950.class) {
            if(gpuVerified==0) {
                try { gpuVerified=gpuSelfCheck()?1:-1; }
                catch(Throwable failed) {gpuVerified=-1;}
            }
            return gpuVerified>0;
        }
    }
    private static boolean gpuSelfCheck() {
        int width=17,rows=13,n=width*rows;
        int[] guide=new int[n],between=new int[n],expected=new int[n],actual=new int[n],halos=new int[n];
        for(int i=0;i<n;i++){
            guide[i]=0xff000000|((i*23+71)&255)<<16|((i*41+13)&255)<<8|((i*11+53)&255);
            if(i%29==0)guide[i]&=0x7fffffff;
        }
        for(int level=1;level<=4;level++)for(int flags=0;flags<4;flags++)for(int region=0;region<3;region++){
            java.util.Arrays.fill(between,0x13579bdf);
            java.util.Arrays.fill(expected,0x13579bdf);
            java.util.Arrays.fill(actual,0x13579bdf);
            java.util.Arrays.fill(halos,0x13579bdf);
            boolean texture=(flags&1)!=0,shadows=(flags&2)!=0;
            int first=region==0?0:region==1?2:rows-4;
            int last=region==0?rows:region==1?rows-2:rows;
            int outerFirst=Math.max(0,first-3),outerLast=Math.min(rows,last+3);
            if(!run(guide,guide,between,width,rows,level,true,texture,shadows,outerFirst,outerLast) ||
                    !run(guide,between,expected,width,rows,level,false,texture,shadows,first,last) ||
                    !bilateralPairGpuNative(guide,actual,halos,width,rows,level,texture,shadows,
                            first,last,LUMA,COLOUR) || !java.util.Arrays.equals(expected,actual))return false;
            for(int row=outerFirst;row<outerLast;row++)if(row<first||row>=last)
                for(int x=0;x<width;x++)if(halos[row*width+x]!=between[row*width+x])return false;
        }
        return true;
    }
    static boolean run(int[] guide,int[] values,int[] output,int width,int rows,
            int noise,boolean horizontal,boolean texture,boolean shadows,int begin,int end) {
        if(!LOADED || guide==null || values==null || output==null || guide==output || values==output ||
                width<1 || rows<1 || noise<1 || noise>4 || begin<0 || end<begin || end>rows ||
                (long)width*rows>guide.length || (long)width*rows>values.length ||
                (long)width*rows>output.length)return false;
        if(begin==end)return true;
        int maxRows=Math.min(rows,70);
        int[] decoded=null,valuePacked=null;
        try {
            decoded=SpeedWorkers1935.borrowInts(width*maxRows);
            valuePacked=SpeedWorkers1935.borrowInts(width*maxRows);
            // Limit pinned critical time to 64 output rows. Each tile retains its
            // original three-row source halo; no filtered pixels feed another tile.
            for(int first=begin;first<end;) {
                int last=end-first>64?first+64:end;
                if(!bilateralNative(guide,values,output,width,rows,noise,horizontal,texture,
                        shadows,first,last,LUMA,COLOUR,decoded,valuePacked))return false;
                first=last;
            }
            return true;
        } catch(OutOfMemoryError unavailable) { return false; }
          catch(LinkageError unavailable) { verified=-1;return false; }
          finally { SpeedWorkers1935.release(valuePacked); SpeedWorkers1935.release(decoded); }
    }
    private static boolean selfCheck() {
        final int width=17,rows=13,n=width*rows;
        int[] guide=new int[n],values=new int[n],expected=new int[n],actual=new int[n];
        for(int i=0;i<n;i++) {
            int x=i%width,y=i/width,k=(x*17+y*29+x*y*3)%31;
            guide[i]=0xff000000|((31+k+x*4)&255)<<16|((26+k+y*5)&255)<<8|((22+k+x*2)&255);
            values[i]=0xff000000|((i*31+11)&255)<<16|((i*19+37)&255)<<8|((i*7+91)&255);
            if(i%43==0)guide[i]&=0x7fffffff;
        }
        for(int level=1;level<=4;level++)for(int flags=0;flags<8;flags++) {
            java.util.Arrays.fill(expected,0x13579bdf);java.util.Arrays.fill(actual,0x13579bdf);
            boolean horizontal=(flags&1)!=0,texture=(flags&2)!=0,shadows=(flags&4)!=0;
            DetailSerial186.bilateralBefore1950(guide,values,expected,width,rows,level,horizontal,texture,shadows,1,rows-1);
            if(!run(guide,values,actual,width,rows,level,horizontal,texture,shadows,1,rows-1) || !java.util.Arrays.equals(expected,actual))return false;
        }
        return true;
    }
    private static native int nativeAbi();
    private static native boolean bilateralNative(int[] guide,int[] values,int[] output,
        int width,int rows,int noise,boolean horizontal,boolean texture,boolean shadows,
        int begin,int end,int[] luma,int[] colour,int[] decoded,int[] valuePacked);
    private static native int bilateralPairGpuAbi();
    private static native boolean bilateralPairGpuNative(int[] guide,int[] output,int[] horizontal,
        int width,int rows,int noise,boolean texture,boolean shadows,int begin,int end,
        int[] luma,int[] colour);
}
