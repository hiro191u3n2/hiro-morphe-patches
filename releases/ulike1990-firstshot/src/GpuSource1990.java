package com.hiro.ulike;

/** Exact bounded source transfer. Native destination remains a complete image,
 * but neither Java nor native upload staging needs a second complete image.
 * This does not tile computation or change any kernel neighbourhood/order. */
final class GpuSource1990 {
    static final int ROWS=32;
    interface Source { void read(int[] pixels,int first,int rows); }
    interface Sink { boolean write(int firstPixel,int[] pixels,int count); }
    interface Check { void check(); }
    private GpuSource1990() {}
    static int capacity(int width,int height) {
        if(width<1||height<1||(long)width*height>Integer.MAX_VALUE)
            throw new IllegalArgumentException("GPU source dimensions");
        return Math.multiplyExact(width,Math.min(ROWS,height));
    }
    static long workspaceBytes(int width,int height) {
        long bytes=4L*capacity(width,height);return bytes+((bytes+4095L)&~4095L);
    }
    static boolean upload(int width,int height,Source source,Sink sink,Check cancellation) {
        final int[] pixels=new int[capacity(width,height)];
        for(int first=0;first<height;) {
            cancellation.check();
            final int rows=Math.min(ROWS,height-first),count=width*rows;
            source.read(pixels,first,rows);
            // hasAlpha() is not a pixel proof. Inspect every active word,
            // including the final partial band, before adopting any output.
            for(int i=0;i<count;i++) {
                if((i&4095)==0)cancellation.check();
                if((pixels[i]>>>24)!=255)return false;
            }
            cancellation.check();
            if(!sink.write(first*width,pixels,count))return false;
            first+=rows;
        }
        cancellation.check();return true;
    }
}
