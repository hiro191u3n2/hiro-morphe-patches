package com.hiro.ulike;

/** H1: the actual source-domain six-neighbour bilateral kernel, retaining the
 * original integer ranges, rounding, tap order, opacity and texture/shadow rules.
 * The existing stage scheduler, sharpening, masks and output depth are untouched.
 */
public final class CorePixels1950 {
    private static final int[] LUMA = ranges(new int[]{1,12,18,26,34});
    private static final int[] COLOUR = ranges(new int[]{1,18,28,38,48});
    private static final boolean LOADED = load();
    private static volatile int verified;
    private CorePixels1950() { }
    private static boolean load() {
        try { System.loadLibrary("ulike_core1950"); return nativeAbi()==1950; }
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
    static boolean run(int[] guide,int[] values,int[] output,int width,int rows,
            int noise,boolean horizontal,boolean texture,boolean shadows,int begin,int end) {
        if(!LOADED || guide==null || values==null || output==null || guide==output || values==output ||
                width<1 || rows<1 || noise<1 || noise>4 || begin<0 || end<begin || end>rows ||
                (long)width*rows>guide.length || (long)width*rows>values.length ||
                (long)width*rows>output.length)return false;
        if(begin==end)return true;
        int maxRows=Math.min(rows,70);
        int[] decoded=null,valueY=null;
        try {
            decoded=SpeedWorkers1935.borrowInts(width*maxRows);
            if(!horizontal)valueY=SpeedWorkers1935.borrowInts(width*maxRows);
            // Limit pinned critical time to 64 output rows. Each tile retains its
            // original three-row source halo; no filtered pixels feed another tile.
            for(int first=begin;first<end;) {
                int last=end-first>64?first+64:end;
                if(!bilateralNative(guide,values,output,width,rows,noise,horizontal,texture,
                        shadows,first,last,LUMA,COLOUR,decoded,valueY))return false;
                first=last;
            }
            return true;
        } catch(OutOfMemoryError unavailable) { return false; }
          catch(LinkageError unavailable) { verified=-1;return false; }
          finally { SpeedWorkers1935.release(valueY); SpeedWorkers1935.release(decoded); }
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
        int begin,int end,int[] luma,int[] colour,int[] decoded,int[] valueY);
}
