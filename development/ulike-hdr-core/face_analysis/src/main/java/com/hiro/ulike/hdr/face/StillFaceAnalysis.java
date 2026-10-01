package com.hiro.ulike.hdr.face;

import com.hiro.ulike.hdr.input.HdrFrame;
import java.nio.ByteBuffer;

/** Same-still analysis ownership and geometry. Does not supply a face detector. */
public final class StillFaceAnalysis {
    private StillFaceAnalysis() {}

    /** Explicit analysis-only colour mapping; the HDR samples are never replaced. */
    public interface ProxyRenderer {
        String policyName();
        /** Packed full-range, encoded SDR RGB8 for this exact source pixel. */
        int rgb8(HdrFrame ownedSource, int sourceX, int sourceY);
    }

    /**
     * Must synchronously analyse only the submitted bytes and return copied
     * landmarks in this image's pixel-centre coordinates, ordered as SDK106.
     * A cached preview callback is not an implementation of this interface.
     * Backend licensing, model loading and error checking remain mandatory.
     */
    public interface Detector {
        Face[] detect(Image image) throws Exception;
    }

    public static final class Face {
        private final float[] xy;
        public final float score;
        public Face(float[] xy, float score) {
            if (xy == null || xy.length != 212 || !Float.isFinite(score) || score < 0 || score > 1)
                throw new IllegalArgumentException("Expected 106 ordered points and finite score");
            this.xy = xy.clone(); this.score = score;
            for (float value : this.xy) if (!Float.isFinite(value))
                throw new IllegalArgumentException("Nonfinite landmark");
        }
        public float[] points() { return xy.clone(); }
    }

    public static final class Image {
        public final int width, height, sourceWidth, sourceHeight, rotationClockwise;
        public final boolean mirroredAfterRotation;
        public final String proxyPolicy;
        public final long sensorTimestampNs, frameNumber, shot;
        private final byte[] pixels;
        private final HdrFrame source;
        private final double scaleX, scaleY;
        private Image(HdrFrame source, int width, int height, int rotation, boolean mirrored,
                      String policy, byte[] pixels, int orientedWidth, int orientedHeight) {
            this.source = source; this.sourceWidth = source.width; this.sourceHeight = source.height;
            this.width = width; this.height = height; this.rotationClockwise = rotation;
            this.mirroredAfterRotation = mirrored; this.proxyPolicy = policy; this.pixels = pixels;
            this.sensorTimestampNs = source.timestampNs; this.frameNumber = source.frameNumber;
            this.shot = source.shot;
            this.scaleX = orientedWidth / (double) width; this.scaleY = orientedHeight / (double) height;
        }
        public ByteBuffer rgb8() { return ByteBuffer.wrap(pixels).asReadOnlyBuffer(); }
        /** Explicit resize-centre, optional mirror, then inverse rotation. */
        public double[] sourcePoint(double x, double y) {
            if (!Double.isFinite(x) || !Double.isFinite(y))
                throw new IllegalArgumentException("Nonfinite coordinate");
            double ox = (x + 0.5) * scaleX - 0.5, oy = (y + 0.5) * scaleY - 0.5;
            int ow = (rotationClockwise % 180 == 0) ? sourceWidth : sourceHeight;
            if (mirroredAfterRotation) ox = ow - 1.0 - ox;
            switch (rotationClockwise) {
                case 0: return new double[]{ox, oy};
                case 90: return new double[]{oy, sourceHeight - 1.0 - ox};
                case 180: return new double[]{sourceWidth - 1.0 - ox, sourceHeight - 1.0 - oy};
                case 270: return new double[]{sourceWidth - 1.0 - oy, ox};
                default: throw new AssertionError();
            }
        }
    }

    public static final class Result {
        private final HdrFrame source;
        private final Face[] sourceFaces;
        public final Image analysisImage;
        private Result(Image image, Face[] faces) {
            source = image.source; analysisImage = image; sourceFaces = faces;
        }
        /** Object ownership, not a guess from matching timestamps or face IDs. */
        public Face[] requireSource(HdrFrame exactSource) {
            if (exactSource != source) throw new IllegalArgumentException("Landmarks belong to a different still");
            return sourceFaces.clone();
        }
    }

    public static Image prepare(HdrFrame source, int rotationClockwise, boolean mirror,
                                int maximumSide, ProxyRenderer renderer) {
        if (source == null || renderer == null) throw new NullPointerException();
        if (rotationClockwise != 0 && rotationClockwise != 90 && rotationClockwise != 180 && rotationClockwise != 270)
            throw new IllegalArgumentException("Rotation must be explicit quarter turns");
        if (maximumSide < 128 || maximumSide > 4096) throw new IllegalArgumentException("Invalid analysis bound");
        String policy = renderer.policyName();
        if (policy == null || policy.trim().isEmpty() || policy.length() > 256)
            throw new IllegalArgumentException("Analysis colour policy must be explicit");
        int ow = rotationClockwise % 180 == 0 ? source.width : source.height;
        int oh = rotationClockwise % 180 == 0 ? source.height : source.width;
        double factor = Math.min(1.0, maximumSide / (double)Math.max(ow, oh));
        int w = Math.max(1, (int)Math.floor(ow * factor));
        int h = Math.max(1, (int)Math.floor(oh * factor));
        byte[] pixels = new byte[Math.multiplyExact(Math.multiplyExact(w, h), 3)];
        Image image = new Image(source, w, h, rotationClockwise, mirror, policy, pixels, ow, oh);
        // Nearest source centre is deliberate for the analysis proxy only.
        // No resizing is applied to the separately owned HDR photograph.
        for (int y = 0, p = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double[] xy = image.sourcePoint(x, y);
            int sx = Math.max(0, Math.min(source.width - 1, (int)Math.floor(xy[0] + 0.5)));
            int sy = Math.max(0, Math.min(source.height - 1, (int)Math.floor(xy[1] + 0.5)));
            int rgb = renderer.rgb8(source, sx, sy);
            if ((rgb & 0xff000000) != 0) throw new IllegalArgumentException("Renderer must return RGB24");
            pixels[p++] = (byte)(rgb >> 16); pixels[p++] = (byte)(rgb >> 8); pixels[p++] = (byte)rgb;
        }
        return image;
    }

    public static Result analyse(Image image, Detector backend) throws Exception {
        if (image == null || backend == null) throw new NullPointerException();
        Face[] detected = backend.detect(image);
        if (detected == null || detected.length > 10) throw new IllegalArgumentException("Invalid face result");
        Face[] output = new Face[detected.length];
        for (int f = 0; f < detected.length; f++) {
            Face face = detected[f];
            if (face == null) throw new IllegalArgumentException("Null face");
            float[] mapped = new float[212];
            for (int p = 0; p < 106; p++) {
                double[] xy = image.sourcePoint(face.xy[p * 2], face.xy[p * 2 + 1]);
                mapped[p * 2] = (float)xy[0]; mapped[p * 2 + 1] = (float)xy[1];
            }
            output[f] = new Face(mapped, face.score);
        }
        return new Result(image, output);
    }
}
