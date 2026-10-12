package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

/** Bounded, exact proof storage. A queued job owns one immutable source image;
 * its optional half image is reconstructed only while the idle job executes.
 * Final GPU rows are compared directly with the independent CPU Bitmap, so a
 * second full-size candidate Bitmap is never needed merely for comparison. */
final class ResidentProof1978 {
    static final int UNAVAILABLE=0,EXACT=1,MISMATCH=-1;
    final Bitmap expected;
    final int width,height;
    final GpuQualification1961.Cancellation cancellation;
    private final int[] reference;
    private final boolean[] visited;
    private int rowsCompared;
    private long pixelsCompared;
    private boolean mismatch;

    ResidentProof1978(Bitmap expected,GpuQualification1961.Cancellation cancellation) {
        if(expected==null||expected.isRecycled()||expected.getWidth()<1||expected.getHeight()<1)
            throw new IllegalArgumentException("resident comparison image");
        this.expected=expected;this.width=expected.getWidth();this.height=expected.getHeight();
        this.cancellation=cancellation;
        reference=new int[Math.multiplyExact(width,Math.min(32,height))];
        visited=new boolean[height];
    }
    void check() {
        if(Thread.currentThread().isInterrupted()||cancellation!=null&&cancellation.cancelled())
            throw new java.util.concurrent.CancellationException("resident whole-image proof cancelled");
    }
    boolean rows(int first,int count,int[] pixels,int offset) {
        check();
        if(first<0||count<=0||first>height-count||pixels==null||offset<0||
                (long)offset+(long)width*count>pixels.length)
            throw new IllegalArgumentException("resident comparison rows");
        for(int row=first;row<first+count;row++)if(visited[row])
            throw new IllegalStateException("resident comparison row written twice");
        for(int at=0;at<count;) {
            check();int chunk=Math.min(32,count-at);
            expected.getPixels(reference,0,width,0,first+at,width,chunk);
            int length=width*chunk,start=offset+at*width;
            for(int i=0;i<length;i++) {
                if((i&4095)==0)check();
                pixelsCompared++;
                if(reference[i]!=pixels[start+i]){mismatch=true;return false;}
            }
            for(int row=first+at;row<first+at+chunk;row++)visited[row]=true;
            rowsCompared+=chunk;at+=chunk;
        }
        return true;
    }
    int result() {check();return mismatch?MISMATCH:rowsCompared==height?EXACT:UNAVAILABLE;}
    long pixelsCompared(){return pixelsCompared;}
    long workspaceBytes(){return 4L*reference.length+visited.length+256L;}

    static boolean identity(Bitmap source,int rotation,int width,int height) {
        return source!=null&&!source.isRecycled()&&rotation==0&&source.getWidth()==width&&source.getHeight()==height;
    }
    static long retainedBytes(Bitmap source,QualityPixels1932.Plan plan,QualityPixels1932.Plan output) {
        if(source==null||source.getWidth()<1||source.getHeight()<1||plan==null||output==null)
            return Long.MAX_VALUE;
        long pixels=(long)source.getWidth()*source.getHeight();
        if(pixels>Integer.MAX_VALUE)return Long.MAX_VALUE;
        IdentityHashMap<Object,Boolean> counted=new IdentityHashMap<Object,Boolean>();
        long bitmap=bitmapBytes(source);if(bitmap==Long.MAX_VALUE)return bitmap;
        return bitmap+maskBytes(plan.faceRegions,counted)+maskBytes(output.faceRegions,counted)+
            noiseBytes(plan.localNoise,counted)+noiseBytes(output.localNoise,counted)+65536L;
    }
    /** Bitmap row alignment and a larger reused allocation are real retained
     * memory. Both the preflight source and the new immutable copy are measured;
     * unavailable accounting declines qualification instead of assuming 4WH. */
    static long bitmapBytes(Bitmap bitmap) {
        try {
            long stride=((Number)Bitmap.class.getMethod("getRowBytes").invoke(bitmap)).longValue();
            long allocation=((Number)Bitmap.class.getMethod("getAllocationByteCount").invoke(bitmap)).longValue();
            long minimum=4L*bitmap.getWidth(),rows=stride*bitmap.getHeight();
            if(stride<minimum||rows<minimum*bitmap.getHeight()||allocation<rows)return Long.MAX_VALUE;
            return allocation;
        } catch(ReflectiveOperationException unavailable){return Long.MAX_VALUE;}
          catch(RuntimeException unavailable){return Long.MAX_VALUE;}
    }
    private static long noiseBytes(SpatialNoise1934 noise,IdentityHashMap<Object,Boolean> counted) {
        if(noise==null||counted.put(noise,Boolean.TRUE)!=null)return 0;
        return 512L+4L*noise.columns*noise.rows+12L*(Math.min(32768,noise.width)+Math.min(32768,noise.height));
    }
    private static long maskBytes(QualityPixels1932.RegionMask mask,IdentityHashMap<Object,Boolean> counted) {
        if(mask==null||counted.put(mask,Boolean.TRUE)!=null)return 0;
        if(!(mask instanceof FaceRegions1934.Mask))return Long.MAX_VALUE/8;
        // Resized immutable mask wrappers share the same two byte planes. Count
        // every physical plane once; reflection failure keeps the old safe bound.
        try {
            Field skin=mask.getClass().getDeclaredField("skin"),detail=mask.getClass().getDeclaredField("detail");
            skin.setAccessible(true);detail.setAccessible(true);
            Object a=skin.get(mask),b=detail.get(mask);
            if(a!=null&&!(a instanceof byte[])||b!=null&&!(b instanceof byte[]))
                return ((FaceRegions1934.Mask)mask).retainedBytes1976();
            long bytes=192;
            if(a!=null&&counted.put(a,Boolean.TRUE)==null)bytes+=32L+((byte[])a).length;
            if(b!=null&&counted.put(b,Boolean.TRUE)==null)bytes+=32L+((byte[])b).length;
            return bytes;
        } catch(ReflectiveOperationException unavailable) {
            return ((FaceRegions1934.Mask)mask).retainedBytes1976();
        } catch(RuntimeException unavailable) {
            return ((FaceRegions1934.Mask)mask).retainedBytes1976();
        }
    }
    static int[] half(Bitmap source,GpuQualification1961.Cancellation cancellation) {
        int width=source.getWidth(),height=source.getHeight(),columns=width/2+(width&1),rows=height/2+(height&1);
        int[] half=new int[Math.multiplyExact(columns,rows)];
        int[] scan=new int[Math.multiplyExact(width,Math.min(2,height))];
        for(int y=0;y<rows;y++) {
            if(Thread.currentThread().isInterrupted()||cancellation!=null&&cancellation.cancelled())
                throw new java.util.concurrent.CancellationException("resident half reconstruction cancelled");
            int count=Math.min(2,height-y*2);source.getPixels(scan,0,width,0,y*2,width,count);
            for(int x=0;x<columns;x++) {
                int r=0,g=0,b=0,n=0;boolean opaque=true;
                for(int yy=0;yy<count;yy++)for(int xx=x*2;xx<Math.min(width,x*2+2);xx++) {
                    int p=scan[yy*width+xx];opaque&=(p>>>24)==255;
                    r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;
                }
                half[y*columns+x]=(opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
            }
        }
        return half;
    }
}
