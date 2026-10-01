package com.hiro.ulike.hdr.faceprobe;

/** Exact-grid ownership boundary for optional diagnostic SDK render callbacks. */
public final class OwnedRenderPixels {
    private OwnedRenderPixels() {}
    public static int[] copy(int[] sdkPixels,int width,int height,int expectedWidth,int expectedHeight) {
        if(width<1 || height<1 || width!=expectedWidth || height!=expectedHeight || (long)width*height>4194304 ||
                sdkPixels==null || sdkPixels.length!=(long)width*height)throw new IllegalArgumentException("Invalid diagnostic pixel grid");
        return sdkPixels.clone();
    }
}
