package com.hiro.ulike.hdr.color;

import com.hiro.ulike.hdr.face.StillFaceAnalysis;
import com.hiro.ulike.hdr.input.HdrFrame;
import hiro.ulike.beauty.BeautyImageEngine;

/** Explicit replacement SDR rendition for analysis/model input; never an HDR beauty output. */
public final class SdrRendition implements BeautyImageEngine.SourceRgbFloat, StillFaceAnalysis.ProxyRenderer {
    public static final String POLICY = "scene-reinhard-luma-srgb-gamutclip-v1";
    private final P010SceneSource source;
    public final double exposureScale;
    private final ThreadLocal<double[]> scratch = new ThreadLocal<double[]>() {
        @Override protected double[] initialValue() { return new double[3]; }
    };

    /** Exposure is explicit relative scene exposure; this is not Samsung/ULike's tone mapper. */
    public SdrRendition(P010SceneSource source, double exposureScale) {
        if (source == null) throw new NullPointerException("source");
        if (!Double.isFinite(exposureScale) || exposureScale < 1e-6 || exposureScale > 1e6)
            throw new IllegalArgumentException("explicit exposure scale 1e-6..1e6 required");
        this.source = source; this.exposureScale = exposureScale;
    }

    public P010SceneSource hdrSource() { return source; }
    @Override public int width() { return source.width(); }
    @Override public int height() { return source.height(); }
    @Override public Object frameIdentity() { return source.frameIdentity(); }
    @Override public String policyName() {
        return POLICY + ";exposure=" + Double.toHexString(exposureScale)
                + ";chroma=" + source.horizontal + "/" + source.vertical
                + ";evidence=" + source.chromaLocationEvidence;
    }

    /** No 8-bit intermediate: continuous encoded sRGB into the FP32 neural component. */
    @Override public void readPixel(int x, int y, float[] dst) {
        if (dst == null || dst.length < 3) throw new IllegalArgumentException("RGB destination required");
        double[] rgb = render(x, y);
        for (int c = 0; c < 3; c++) dst[c] = (float) rgb[c];
    }

    /** Only the separate face-analysis proxy is quantized to RGB8. */
    @Override public int rgb8(HdrFrame exactSource, int x, int y) {
        if (exactSource != source.frameIdentity())
            throw new IllegalArgumentException("renderer belongs to a different still");
        double[] rgb = render(x, y);
        return ((int) Math.floor(rgb[0] * 255 + .5) << 16)
                | ((int) Math.floor(rgb[1] * 255 + .5) << 8)
                | (int) Math.floor(rgb[2] * 255 + .5);
    }

    private double[] render(int x, int y) {
        double[] rgb = scratch.get(); source.readPixel(x, y, rgb);
        double lum = .2627 * rgb[0] + .678 * rgb[1] + .0593 * rgb[2];
        double scale = exposureScale / (1 + Math.max(0, lum) * exposureScale);
        double r = rgb[0] * scale, g = rgb[1] * scale, b = rgb[2] * scale;
        // W3C CSS Color 4 rational RGB-to-XYZ matrices: inverse(sRGB) * BT.2020.
        rgb[0] = encode(1.6604910021084345*r - .5876411387885495*g - .07284986331988488*b);
        rgb[1] = encode(-.12455047452159074*r + 1.1328998971259603*g - .008349422604369477*b);
        rgb[2] = encode(-.018150763354905303*r - .10057889800800739*g + 1.1187296613629127*b);
        return rgb;
    }

    private static double encode(double linear) {
        // Declared gamut clipping occurs on this SDR rendition only, never on the HDR source.
        double x = Math.max(0, Math.min(1, linear));
        return x <= .0031308 ? 12.92*x : 1.055*Math.pow(x, 1/2.4)-.055;
    }
}
