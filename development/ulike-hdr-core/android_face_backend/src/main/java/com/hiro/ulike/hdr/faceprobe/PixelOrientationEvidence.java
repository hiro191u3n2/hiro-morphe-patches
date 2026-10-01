package com.hiro.ulike.hdr.faceprobe;

/** Diagnostic image-grid comparisons. Never used to silently transform points. */
public final class PixelOrientationEvidence {
    private PixelOrientationEvidence() {}
    public static final String[] NAMES = {"identity", "clockwise90", "clockwise180", "clockwise270",
            "mirror_after0", "mirror_after90", "mirror_after180", "mirror_after270"};
    /** NaN means dimensions cannot match; other values are mean absolute RGB8 error. */
    public static double[] measure(int[] source, int sw, int sh, int[] result, int rw, int rh) {
        check(source,sw,sh); check(result,rw,rh);
        double[] error = new double[8];
        for (int transform=0;transform<8;transform++) {
            int rotation=transform%4;
            int ow=rotation%2==0?sw:sh, oh=rotation%2==0?sh:sw;
            if (rw!=ow || rh!=oh) { error[transform]=Double.NaN; continue; }
            long total=0;
            for (int y=0;y<rh;y++) for(int x=0;x<rw;x++) {
                int rx=transform>=4?rw-1-x:x, sx,sy;
                switch(rotation) {
                    case 0: sx=rx;sy=y;break;
                    case 1: sx=y;sy=sh-1-rx;break;
                    case 2: sx=sw-1-rx;sy=sh-1-y;break;
                    case 3: sx=sw-1-y;sy=rx;break;
                    default: throw new AssertionError();
                }
                int a=source[sy*sw+sx], b=result[y*rw+x];
                for(int shift=0;shift<24;shift+=8) total+=Math.abs(((a>>>shift)&255)-((b>>>shift)&255));
            }
            error[transform]=total/(3.0*rw*rh);
        }
        return error;
    }
    private static void check(int[] pixels,int w,int h) {
        if (pixels==null || w<1 || h<1 || (long)w*h>4194304 || (long)w*h!=pixels.length)
            throw new IllegalArgumentException("Invalid bounded pixel array");
    }
}
