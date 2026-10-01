package com.hiro.ulike.hdr.faceprobe;

/** Explicit application allocation policy, never an advertised SDK/device capability.
 * Known payload excludes app baseline, native/GPU storage, models and allocator overhead.
 */
public final class AnalysisCapacity {
    public static final int LEGACY_MAX_PIXELS=4194304;
    public static final int NATIVE_CANDIDATE_MAX_PIXELS=12484800;
    public final int maxPixels,maxDimension;
    public final long maxKnownPayloadBytes,maxDiagnosticBytes;
    public final boolean comparePhotographicOrientation;
    private AnalysisCapacity(int pixels,int dimension,long payload,long diagnostic,boolean orientation) {
        maxPixels=pixels;maxDimension=dimension;maxKnownPayloadBytes=payload;maxDiagnosticBytes=diagnostic;
        comparePhotographicOrientation=orientation;
    }
    public static AnalysisCapacity legacyDiagnostic() {
        return new AnalysisCapacity(LEGACY_MAX_PIXELS,Integer.MAX_VALUE,Long.MAX_VALUE,4L*LEGACY_MAX_PIXELS,true);
    }
    public static AnalysisCapacity nativeSize4080x3060Candidate() {
        // 15P = P010 + one Bitmap + SDK callback + one owned diagnostic. One transfer row is additional.
        return new AnalysisCapacity(NATIVE_CANDIDATE_MAX_PIXELS,4080,192L*1024*1024,4L*NATIVE_CANDIDATE_MAX_PIXELS,false);
    }
    public long requireGrid(int width,int height) {
        long count=(long)width*height;
        if(width<1 || height<1 || width>maxDimension || height>maxDimension || count>maxPixels)
            throw new IllegalArgumentException("Analysis grid exceeds explicit application policy; no resize");
        return count;
    }
    public long requireTransferredPayload(int width,int height) {
        long count=requireGrid(width,height);
        long bytes=(comparePhotographicOrientation?19L:15L)*count+4L*width;
        if(bytes>maxKnownPayloadBytes || 4L*count>maxDiagnosticBytes)
            throw new IllegalArgumentException("Known analysis payload exceeds explicit policy");
        return bytes;
    }
    public void requireDiagnostic(int width,int height) {
        if(4L*requireGrid(width,height)>maxDiagnosticBytes)throw new IllegalArgumentException("Diagnostic allocation budget");
    }
}
