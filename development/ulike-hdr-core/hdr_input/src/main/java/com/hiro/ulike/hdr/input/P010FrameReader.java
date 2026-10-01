package com.hiro.ulike.hdr.input;

import java.nio.ByteBuffer;

/** Android-independent, bounded copy of Android's three logical P010 planes. */
public final class P010FrameReader {
    public static final int FORMAT_P010 = 54;
    private P010FrameReader() {}

    public static final class Plane {
        private final ByteBuffer data;
        public final int rowStride, pixelStride;
        /** The buffer's current position denotes this logical plane's first sample. */
        public Plane(ByteBuffer data, int rowStride, int pixelStride) {
            this.data = data == null ? null : data.asReadOnlyBuffer();
            this.rowStride = rowStride; this.pixelStride = pixelStride;
        }
    }

    public static final class Input {
        public final int format, width, height, cropLeft, cropTop, cropRight, cropBottom;
        public final long timestampNs;
        public final HdrFrame.Encoding encoding;
        public final CaptureMatch.Source source;
        private final Plane[] planes;
        public Input(int format, int width, int height, int cropLeft, int cropTop,
                     int cropRight, int cropBottom, long timestampNs, HdrFrame.Encoding encoding,
                     CaptureMatch.Source source, Plane[] planes) {
            this.format = format; this.width = width; this.height = height;
            this.cropLeft = cropLeft; this.cropTop = cropTop;
            this.cropRight = cropRight; this.cropBottom = cropBottom;
            this.timestampNs = timestampNs; this.encoding = encoding; this.source = source;
            this.planes = planes == null ? null : planes.clone();
        }
    }

    /** Required caller contract: exact negotiated dimensions/color and a per-frame payload cap. */
    public static final class Request {
        public final int width, height;
        public final HdrFrame.Encoding encoding;
        public final long maxAllocationBytes;
        public Request(int width, int height, HdrFrame.Encoding encoding, long maxAllocationBytes) {
            if (width <= 0 || height <= 0 || (width & 1) != 0 || (height & 1) != 0
                    || encoding == null || maxAllocationBytes <= 0) {
                throw new IllegalArgumentException("even dimensions, explicit color/range and allocation cap required");
            }
            this.width = width; this.height = height; this.encoding = encoding;
            this.maxAllocationBytes = maxAllocationBytes;
        }
    }

    /** Borrows input buffers synchronously. Does not close them or mutate their position/limit. */
    public static HdrFrame copy(Input input, Request request, CaptureMatch.Context capture,
                                CaptureMatch.Result result, CaptureMatch.Current current)
            throws InputRejected {
        if (input == null || request == null || current == null)
            throw reject(InputRejected.Reason.ARGUMENT, "input, request and live selection required");
        CaptureMatch.requireCurrent(capture, current.snapshot());
        CaptureMatch.requireFrame(capture, input.source, result, input.timestampNs);
        if (input.format != FORMAT_P010) throw reject(InputRejected.Reason.FORMAT, "P010 input required");
        if (input.width != request.width || input.height != request.height)
            throw reject(InputRejected.Reason.DIMENSIONS, "input differs from negotiated native size");
        if (input.cropLeft != 0 || input.cropTop != 0 || input.cropRight != input.width
                || input.cropBottom != input.height)
            throw reject(InputRejected.Reason.CROP, "only uncropped full-frame P010 is supported");
        if (input.encoding == null || input.encoding != request.encoding)
            throw reject(InputRejected.Reason.COLOR, "unknown or unexpected transfer/gamut/range");
        if (input.planes == null || input.planes.length != 3)
            throw reject(InputRejected.Reason.FORMAT, "three logical Y/Cb/Cr planes required");

        long pixels = (long) input.width * input.height;
        // Guard multiplication and Java array limits before allocation or int address arithmetic.
        if (pixels > Integer.MAX_VALUE - 8L || pixels > Long.MAX_VALUE / 3
                || pixels * 3 > request.maxAllocationBytes)
            throw reject(InputRejected.Reason.ALLOCATION_LIMIT, "10-bit plane payload exceeds caller cap");
        validate(input.planes[0], input.width, input.height, 2);
        validate(input.planes[1], input.width / 2, input.height / 2, 4);
        validate(input.planes[2], input.width / 2, input.height / 2, 4);
        if (input.planes[1].rowStride != input.planes[2].rowStride)
            throw reject(InputRejected.Reason.STRIDE, "chroma row strides differ");

        short[][] out = new short[][] { new short[(int) pixels],
                new short[(int) (pixels / 4)], new short[(int) (pixels / 4)] };
        unpack(input.planes[0], input.width, input.height, out[0]);
        unpack(input.planes[1], input.width / 2, input.height / 2, out[1]);
        unpack(input.planes[2], input.width / 2, input.height / 2, out[2]);
        // A close/switch during a long copy must not produce an eligible completed frame.
        CaptureMatch.requireCurrent(capture, current.snapshot());
        return new HdrFrame(input.width, input.height, input.encoding, capture, result, out, pixels * 3);
    }

    private static void validate(Plane p, int width, int height, int expectedPixelStride)
            throws InputRejected {
        if (p == null || p.data == null) throw reject(InputRejected.Reason.BUFFER_BOUNDS, "missing plane");
        long rowBytes = ((long) width - 1) * expectedPixelStride + 2;
        // Chroma views are separate but the physical row contains complete Cb/Cr pairs.
        long physicalRowBytes = (long) width * expectedPixelStride;
        if (p.pixelStride != expectedPixelStride || p.rowStride <= 0 || (p.rowStride & 1) != 0
                || p.rowStride < physicalRowBytes)
            throw reject(InputRejected.Reason.STRIDE, "unsupported or overlapping P010 rows");
        long end = (long) p.data.position() + (long) (height - 1) * p.rowStride + rowBytes;
        if (end > p.data.limit())
            throw reject(InputRejected.Reason.BUFFER_BOUNDS, "plane lacks the final addressed sample");
    }

    private static void unpack(Plane p, int width, int height, short[] out) throws InputRejected {
        int n = 0, base = p.data.position();
        for (int y = 0; y < height; y++) {
            int row = (int) ((long) base + (long) y * p.rowStride);
            for (int x = 0; x < width; x++) {
                int i = row + x * p.pixelStride;
                // Explicit LE reads; ByteBuffer's byte order is not evidence of producer order.
                int word = (p.data.get(i) & 255) | ((p.data.get(i + 1) & 255) << 8);
                if ((word & 63) != 0)
                    throw reject(InputRejected.Reason.SAMPLE_ENCODING, "P010 low six bits are not zero");
                out[n++] = (short) (word >>> 6);
            }
        }
    }
    private static InputRejected reject(InputRejected.Reason reason, String message) {
        return new InputRejected(reason, message);
    }
}
