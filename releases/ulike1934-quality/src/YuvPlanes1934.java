package com.hiro.ulike;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import java.nio.ByteBuffer;

/** Canonical byte-exact YUV_420_888 -> NV21 packing, shared by single and burst.
 * This changes layout only: no guessed range/matrix, gamma or colour conversion.
 * Plane 1 is U and plane 2 is V even on planar/non-overlapping camera buffers.
 * Buffer positions, limits and Image ownership always stay with the caller.
 */
public final class YuvPlanes1934 {
    private YuvPlanes1934() { }

    /** Stock Q0 adapter: its existing false-result error handling remains active. */
    public static boolean copy(Image image, byte[] destination) {
        try { copyStrict(image,destination); return true; }
        catch (RuntimeException invalid) { return false; }
        catch (OutOfMemoryError unavailable) { return false; }
    }

    public static byte[] owned(Image image) {
        int size=required(image);
        byte[] result=new byte[size];
        copyStrict(image,result);
        return result;
    }

    static void copyStrict(Image image,byte[] destination) {
        int size=required(image),w=image.getWidth(),h=image.getHeight();
        if(destination==null || destination.length<size)
            throw new IllegalArgumentException("short NV21 destination");
        Image.Plane[] planes=image.getPlanes();
        if(planes==null || planes.length!=3)
            throw new IllegalArgumentException("three YUV planes required");
        // Validate every source before any destination bytes are changed.
        Plane y=new Plane(planes[0],w,h),u=new Plane(planes[1],w/2,h/2),v=new Plane(planes[2],w/2,h/2);
        y.write(w,h,destination,0,w,1);
        int luma=w*h;
        v.write(w/2,h/2,destination,luma,w,2);
        u.write(w/2,h/2,destination,luma+1,w,2);
    }

    private static int required(Image image) {
        if(image==null || image.getFormat()!=ImageFormat.YUV_420_888)
            throw new IllegalArgumentException("YUV_420_888 image required");
        int w=image.getWidth(),h=image.getHeight();
        if(w<2 || h<2 || (w&1)!=0 || (h&1)!=0)
            throw new IllegalArgumentException("even NV21 dimensions required");
        Rect crop=image.getCropRect();
        // Q0 and the native renderer retain Image width/height. Silently packing
        // a smaller/odd-offset crop would change geometry and chroma siting.
        if(crop!=null && (crop.left!=0 || crop.top!=0 || crop.right!=w || crop.bottom!=h))
            throw new IllegalArgumentException("full-frame NV21 geometry required");
        long pixels=(long)w*h,size=pixels+pixels/2;
        if(size>Integer.MAX_VALUE-8)throw new IllegalArgumentException("NV21 image too large");
        return (int)size;
    }

    private static final class Plane {
        final ByteBuffer bytes;
        final int start,rowStride,pixelStride;
        Plane(Image.Plane plane,int width,int height) {
            if(plane==null || plane.getBuffer()==null)
                throw new IllegalArgumentException("missing YUV plane");
            bytes=plane.getBuffer().duplicate();
            start=bytes.position();rowStride=plane.getRowStride();pixelStride=plane.getPixelStride();
            long span=(long)pixelStride*(width-1)+1;
            long end=(long)start+(long)rowStride*(height-1)+span;
            if(rowStride<=0 || pixelStride<=0 || rowStride<span || end>bytes.limit())
                throw new IllegalArgumentException("invalid/truncated YUV plane");
        }
        void write(int width,int height,byte[] output,int base,int stride,int step) {
            for(int row=0;row<height;row++) {
                int source=start+row*rowStride,target=base+row*stride;
                if(pixelStride==1 && step==1) {
                    bytes.position(source);bytes.get(output,target,width);
                } else {
                    for(int x=0;x<width;x++)output[target+x*step]=bytes.get(source+x*pixelStride);
                }
            }
        }
    }
}
