package com.hiro.ulike.hdr.encoder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

/** Owned 10-bit YCbCr 4:2:0 codes. This class performs no color conversion. */
public final class P010Frame {
    public enum Encoding { BT2020_NCL_HLG_FULL, BT2020_NCL_HLG_LIMITED }
    public final int width, height;
    public final Encoding encoding;
    public final String captureId, geometryId, processingId;
    private final short[][] samples;

    public P010Frame(int width, int height, Encoding encoding, String captureId,
                     String geometryId, String processingId,
                     ShortBuffer y, ShortBuffer cb, ShortBuffer cr) {
        if (width < 2 || height < 2 || (width & 1) != 0 || (height & 1) != 0
                || (long) width * height > 32_000_000)
            throw new IllegalArgumentException("even dimensions up to 32MP required");
        this.width = width; this.height = height;
        this.encoding = Objects.requireNonNull(encoding, "encoding");
        this.captureId = id(captureId); this.geometryId = id(geometryId);
        this.processingId = id(processingId);
        samples = new short[][] { copy(y, width * height), copy(cb, width * height / 4),
                                  copy(cr, width * height / 4) };
    }
    private static String id(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("provenance ID required");
        return value;
    }
    private static short[] copy(ShortBuffer source, int size) {
        if (source == null || source.remaining() != size) throw new IllegalArgumentException("plane sample count");
        short[] out = new short[size]; source.duplicate().get(out);
        for (short code : out) if (code < 0 || code > 1023) throw new IllegalArgumentException("not a 10-bit code");
        return out;
    }
    public ShortBuffer samples(int component) {
        if (component < 0 || component > 2) throw new IllegalArgumentException("component");
        return ShortBuffer.wrap(samples[component]).asReadOnlyBuffer();
    }

    /** Plane view positions/limits are honored; input buffer positions are never changed. */
    public static final class Plane {
        final ByteBuffer data;
        final int rowStride, pixelStride;
        public Plane(ByteBuffer data, int rowStride, int pixelStride) {
            this.data = Objects.requireNonNull(data, "data").duplicate();
            this.rowStride = rowStride; this.pixelStride = pixelStride;
        }
        void validate(int width, int height, int requiredPixelStride) {
            long rowBytes = (long) (width - 1) * pixelStride + 2;
            long physicalRowBytes = (long) width * requiredPixelStride;
            long end = (long) data.position() + (height - 1L) * rowStride + rowBytes;
            if (data.isReadOnly() || pixelStride != requiredPixelStride || rowStride < physicalRowBytes
                    || rowStride <= 0 || (rowStride & 1) != 0 || end > data.limit())
                throw new IllegalArgumentException("invalid writable P010 plane layout");
        }
    }
    /** P010 is little endian, 10 useful high bits, six zero low bits. No 8-bit intermediary. */
    public void copyTo(Plane y, Plane cb, Plane cr) {
        Plane[] planes = { Objects.requireNonNull(y), Objects.requireNonNull(cb), Objects.requireNonNull(cr) };
        if (cb.rowStride != cr.rowStride)
            throw new IllegalArgumentException("P010 chroma row strides must match");
        // Validate EVERY plane before mutating ANY buffer.
        for (int n = 0; n < 3; n++)
            planes[n].validate(n == 0 ? width : width / 2, n == 0 ? height : height / 2, n == 0 ? 2 : 4);
        for (int n = 0; n < 3; n++) {
            Plane p = planes[n]; int w = n == 0 ? width : width / 2, h = n == 0 ? height : height / 2;
            int start = p.data.position();
            for (int row = 0; row < h; row++) for (int col = 0; col < w; col++) {
                int code = samples[n][row * w + col] << 6;
                int offset = start + row * p.rowStride + col * p.pixelStride;
                p.data.put(offset, (byte) code); p.data.put(offset + 1, (byte) (code >>> 8));
            }
        }
    }
}
