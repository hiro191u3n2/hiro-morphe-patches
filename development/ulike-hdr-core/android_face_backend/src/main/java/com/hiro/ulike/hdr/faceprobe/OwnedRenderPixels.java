package com.hiro.ulike.hdr.faceprobe;

/** Exact-grid ownership boundary for optional diagnostic SDK render callbacks. */
public final class OwnedRenderPixels {
    private OwnedRenderPixels() {}
    public interface Receiver {
        /** Returning normally accepts ownership. On throw, do not retain this array. */
        void image(int[] ownedPixels,int width,int height)throws Exception;
    }
    /** Wipe an undelivered owned clone without ever modifying the borrowed SDK array. */
    public static void copyAndDeliver(int[] sdkPixels,int width,int height,int expectedWidth,int expectedHeight,
            AnalysisCapacity capacity,Receiver receiver)throws Exception {
        if(receiver==null)throw new NullPointerException("receiver");
        int[] owned=copy(sdkPixels,width,height,expectedWidth,expectedHeight,capacity);
        boolean delivered=false;
        try {receiver.image(owned,width,height);delivered=true;}
        finally {if(!delivered)java.util.Arrays.fill(owned,0);}
    }
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
