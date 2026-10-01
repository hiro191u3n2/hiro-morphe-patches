package com.hiro.ulike.hdr.faceprobe;

/** Exact-grid ownership boundary for optional diagnostic SDK render callbacks. */
public final class OwnedRenderPixels {
    private OwnedRenderPixels() {}
    public static int[] copy(int[] sdkPixels,int width,int height,int expectedWidth,int expectedHeight) {
        return copy(sdkPixels,width,height,expectedWidth,expectedHeight,AnalysisCapacity.legacyDiagnostic());
    }
    public static int[] copy(int[] sdkPixels,int width,int height,int expectedWidth,int expectedHeight,AnalysisCapacity capacity) {
        if(capacity==null)throw new NullPointerException("capacity");capacity.requireDiagnostic(width,height);
        if(width!=expectedWidth || height!=expectedHeight ||
                sdkPixels==null || sdkPixels.length!=(long)width*height)throw new IllegalArgumentException("Invalid diagnostic pixel grid");
        return sdkPixels.clone();
    }
}
