package com.hiro.ulike.hdr.input;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;

/** Small deterministic host suite; deliberately uses padded and aliased chroma buffers. */
public final class P010FrameReaderTest {
    private static int checks;
    private static final HdrFrame.Encoding FULL = HdrFrame.Encoding.BT2020_NCL_HLG_FULL;
    private static final HdrFrame.Encoding LIMITED = HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED;
    private static final Object OWNER = new Object(), SESSION = new Object(), READER = new Object();
    private static final Object REQUEST = new Object(), CALLBACK = new Object();
    private static final CaptureMatch.Context CONTEXT = context(OWNER, SESSION, READER, REQUEST, CALLBACK, 7, "0", null);
    private static final CaptureMatch.Source SOURCE = source(CONTEXT, CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE);
    private static final CaptureMatch.Result RESULT = result(CONTEXT, REQUEST, 1234, 88, null);
    private static final P010FrameReader.Request SPEC = new P010FrameReader.Request(4, 4, FULL, 48);
    private static final int[] Y = {0, 1, 2, 3, 64, 65, 511, 512, 513, 514, 939, 940, 941, 1021, 1022, 1023};
    private static final int[] CB = {3, 500, 501, 960}, CR = {7, 511, 512, 1023};
    private interface Checked { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        losslessAndOwned(); colorCropAndBudget(); planeValidation(); exactCapturePairing(); staleDuringCopy();
        System.out.println("P010FrameReaderTest PASS (" + checks + " assertions)");
    }

    private static void losslessAndOwned() throws Exception {
        Fixture f = new Fixture();
        HdrFrame frame = copy(f.input(), SPEC, CONTEXT, RESULT, () -> CONTEXT);
        samples(frame.samples(HdrFrame.Component.Y), Y);
        samples(frame.samples(HdrFrame.Component.CB), CB);
        samples(frame.samples(HdrFrame.Component.CR), CR);
        check(frame.width == 4 && frame.height == 4 && frame.copiedSampleBytes == 48, "native size and exact payload");
        check(frame.timestampNs == 1234 && frame.frameNumber == 88 && frame.exposureTimeNs == 10000L
                && frame.sensitivityIso == 100, "same-result metadata retained");
        check(f.y.position() == 2 && f.y.limit() == 46 && f.uv.position() == 0, "source cursors unchanged");
        f.y.put(2, (byte) 0xff);
        check(frame.samples(HdrFrame.Component.Y).get(0) == 0, "output does not alias Image buffers");
        try { frame.samples(HdrFrame.Component.Y).put(0, (short) 8); throw new AssertionError("mutable output"); }
        catch (ReadOnlyBufferException expected) { checks++; }
        P010FrameReader.Input limited = f.with(54, 4, 4, 0, 0, 4, 4, 1234, LIMITED, SOURCE, new Fixture().planes);
        HdrFrame limitedFrame = copy(limited, new P010FrameReader.Request(4, 4, LIMITED, 48), CONTEXT, RESULT, () -> CONTEXT);
        check(limitedFrame.encoding == LIMITED && limitedFrame.samples(HdrFrame.Component.Y).get(15) == 1023,
                "limited range preserved without clamping headroom");
    }

    private static void colorCropAndBudget() throws Exception {
        Fixture f = new Fixture();
        reject(InputRejected.Reason.FORMAT, () -> normal(f.with(35, 4, 4, 0, 0, 4, 4, 1234, FULL, SOURCE, f.planes)));
        reject(InputRejected.Reason.DIMENSIONS, () -> normal(f.with(54, 8, 4, 0, 0, 8, 4, 1234, FULL, SOURCE, f.planes)));
        reject(InputRejected.Reason.CROP, () -> normal(f.with(54, 4, 4, 2, 0, 4, 4, 1234, FULL, SOURCE, f.planes)));
        reject(InputRejected.Reason.COLOR, () -> normal(f.with(54, 4, 4, 0, 0, 4, 4, 1234, null, SOURCE, f.planes)));
        reject(InputRejected.Reason.COLOR, () -> normal(f.with(54, 4, 4, 0, 0, 4, 4, 1234, LIMITED, SOURCE, f.planes)));
        reject(InputRejected.Reason.ALLOCATION_LIMIT, () -> copy(f.input(), new P010FrameReader.Request(4, 4, FULL, 47), CONTEXT, RESULT, () -> CONTEXT));
        P010FrameReader.Request huge = new P010FrameReader.Request(2147483646, 2147483646, FULL, Long.MAX_VALUE);
        reject(InputRejected.Reason.ALLOCATION_LIMIT, () -> copy(f.with(54, huge.width, huge.height, 0, 0,
                huge.width, huge.height, 1234, FULL, SOURCE, f.planes), huge, CONTEXT, RESULT, () -> CONTEXT));
        try { new P010FrameReader.Request(3, 4, FULL, 100); throw new AssertionError("odd dimensions accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }

    private static void planeValidation() throws Exception {
        Fixture f = new Fixture();
        P010FrameReader.Plane[] bad = f.planes.clone();
        bad[0] = new P010FrameReader.Plane(f.y, 12, 4);
        reject(InputRejected.Reason.STRIDE, () -> normal(f.withPlanes(bad)));
        P010FrameReader.Plane[] overlap = f.planes.clone();
        overlap[0] = new P010FrameReader.Plane(f.y, 6, 2);
        reject(InputRejected.Reason.STRIDE, () -> normal(f.withPlanes(overlap)));
        P010FrameReader.Plane[] partialPair = f.planes.clone();
        partialPair[1] = new P010FrameReader.Plane(f.uv, 6, 4);
        partialPair[2] = new P010FrameReader.Plane(f.uv, 6, 4);
        reject(InputRejected.Reason.STRIDE, () -> normal(f.withPlanes(partialPair)));
        P010FrameReader.Plane[] shortBuffer = f.planes.clone();
        ByteBuffer truncated = f.y.duplicate(); truncated.limit(truncated.limit() - 1);
        shortBuffer[0] = new P010FrameReader.Plane(truncated, 12, 2);
        reject(InputRejected.Reason.BUFFER_BOUNDS, () -> normal(f.withPlanes(shortBuffer)));
        P010FrameReader.Plane[] chromaMismatch = f.planes.clone();
        chromaMismatch[2] = new P010FrameReader.Plane(ByteBuffer.allocate(30), 14, 4);
        reject(InputRejected.Reason.STRIDE, () -> normal(f.withPlanes(chromaMismatch)));
        reject(InputRejected.Reason.FORMAT, () -> normal(f.withPlanes(new P010FrameReader.Plane[] { f.planes[0], f.planes[1] })));
        f.y.put(2, (byte) 1);
        reject(InputRejected.Reason.SAMPLE_ENCODING, () -> normal(f.input()));
    }

    private static void exactCapturePairing() throws Exception {
        Fixture f = new Fixture();
        reject(InputRejected.Reason.TIMESTAMP, () -> normal(f.with(54, 4, 4, 0, 0, 4, 4, 1235, FULL, SOURCE, f.planes)));
        reject(InputRejected.Reason.TIMESTAMP, () -> normal(f.with(54, 4, 4, 0, 0, 4, 4, 1234, FULL,
                source(CONTEXT, CaptureMatch.TimestampConvention.UNKNOWN), f.planes)));
        CaptureMatch.Context[] changed = {
            context(new Object(), SESSION, READER, REQUEST, CALLBACK, 7, "0", null),
            context(OWNER, new Object(), READER, REQUEST, CALLBACK, 7, "0", null),
            context(OWNER, SESSION, new Object(), REQUEST, CALLBACK, 7, "0", null),
            context(OWNER, SESSION, READER, new Object(), CALLBACK, 7, "0", null),
            context(OWNER, SESSION, READER, REQUEST, new Object(), 7, "0", null),
            context(OWNER, SESSION, READER, REQUEST, CALLBACK, 8, "0", null),
            context(OWNER, SESSION, READER, REQUEST, CALLBACK, 7, "1", null),
            context(OWNER, SESSION, READER, REQUEST, CALLBACK, 7, "0", "5")
        };
        for (CaptureMatch.Context other : changed) {
            reject(InputRejected.Reason.OWNERSHIP, () -> copy(f.input(), SPEC, CONTEXT, RESULT, () -> other));
            reject(InputRejected.Reason.OWNERSHIP, () -> copy(f.input(), SPEC, CONTEXT, result(other, REQUEST, 1234, 88, null), () -> CONTEXT));
        }
        reject(InputRejected.Reason.RESULT, () -> copy(f.input(), SPEC, CONTEXT, result(CONTEXT, new Object(), 1234, 88, null), () -> CONTEXT));
        reject(InputRejected.Reason.RESULT, () -> copy(f.input(), SPEC, CONTEXT, result(CONTEXT, REQUEST, 1234, -1, null), () -> CONTEXT));
        reject(InputRejected.Reason.RESULT, () -> copy(f.input(), SPEC, CONTEXT, null, () -> CONTEXT));
        CaptureMatch.Context physical = changed[7];
        P010FrameReader.Input physicalInput = f.with(54, 4, 4, 0, 0, 4, 4, 1234, FULL,
                source(physical, CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE), f.planes);
        reject(InputRejected.Reason.RESULT, () -> copy(physicalInput, SPEC, physical, result(physical, REQUEST, 1234, 88, null), () -> physical));
        HdrFrame ok = copy(physicalInput, SPEC, physical, result(physical, REQUEST, 1234, 88, "5"), () -> physical);
        check("5".equals(ok.physicalId), "physical result provenance preserved");
    }

    private static void staleDuringCopy() throws Exception {
        final int[] snapshots = {0};
        CaptureMatch.Context next = context(OWNER, SESSION, READER, REQUEST, CALLBACK, 8, "0", null);
        reject(InputRejected.Reason.OWNERSHIP, () -> copy(new Fixture().input(), SPEC, CONTEXT, RESULT,
                () -> ++snapshots[0] == 1 ? CONTEXT : next));
        check(snapshots[0] == 2, "selection rechecked after copying");
    }

    private static HdrFrame normal(P010FrameReader.Input i) throws Exception { return copy(i, SPEC, CONTEXT, RESULT, () -> CONTEXT); }
    private static HdrFrame copy(P010FrameReader.Input i, P010FrameReader.Request r, CaptureMatch.Context c,
                                  CaptureMatch.Result result, CaptureMatch.Current current) throws InputRejected {
        return P010FrameReader.copy(i, r, c, result, current);
    }
    private static CaptureMatch.Context context(Object o, Object s, Object r, Object q, Object cb, long n, String id, String p) {
        return new CaptureMatch.Context(o, s, r, q, cb, n, id, p);
    }
    private static CaptureMatch.Source source(CaptureMatch.Context c, CaptureMatch.TimestampConvention t) {
        return new CaptureMatch.Source(c.owner, c.session, c.reader, c.cameraId, c.physicalId, t);
    }
    private static CaptureMatch.Result result(CaptureMatch.Context c, Object request, long ts, long frame, String p) {
        return new CaptureMatch.Result(c, request, ts, frame, p, "5", 10000L, 100);
    }
    private static void samples(ShortBuffer actual, int[] expected) {
        check(actual.remaining() == expected.length, "sample count");
        for (int i = 0; i < expected.length; i++) check(actual.get(i) == expected[i], "10-bit sample " + i);
    }
    private static void check(boolean ok, String detail) { if (!ok) throw new AssertionError(detail); checks++; }
    private static void reject(InputRejected.Reason expected, Checked action) throws Exception {
        try { action.run(); throw new AssertionError("accepted " + expected); }
        catch (InputRejected e) { check(e.reason == expected, "expected " + expected + ", got " + e.reason); }
    }
    private static void word(ByteBuffer b, int offset, int value) {
        int w = value << 6; b.put(offset, (byte) w); b.put(offset + 1, (byte) (w >>> 8));
    }
    private static final class Fixture {
        final ByteBuffer y = ByteBuffer.allocate(46), uv = ByteBuffer.allocate(20);
        final P010FrameReader.Plane[] planes;
        Fixture() {
            for (int i = 0; i < y.capacity(); i++) y.put(i, (byte) 0x7f);
            for (int i = 0; i < 16; i++) word(y, 2 + (i / 4) * 12 + (i % 4) * 2, Y[i]);
            y.position(2);
            for (int i = 0; i < 4; i++) {
                int at = (i / 2) * 12 + (i % 2) * 4;
                word(uv, at, CB[i]); word(uv, at + 2, CR[i]);
            }
            ByteBuffer cb = uv.duplicate(); cb.limit(18);
            ByteBuffer cr = uv.duplicate(); cr.position(2); cr = cr.slice();
            planes = new P010FrameReader.Plane[] { new P010FrameReader.Plane(y, 12, 2),
                new P010FrameReader.Plane(cb, 12, 4), new P010FrameReader.Plane(cr, 12, 4) };
        }
        P010FrameReader.Input input() { return withPlanes(planes); }
        P010FrameReader.Input withPlanes(P010FrameReader.Plane[] p) { return with(54, 4, 4, 0, 0, 4, 4, 1234, FULL, SOURCE, p); }
        P010FrameReader.Input with(int f, int w, int h, int l, int t, int r, int b, long ts,
                                  HdrFrame.Encoding e, CaptureMatch.Source s, P010FrameReader.Plane[] p) {
            return new P010FrameReader.Input(f, w, h, l, t, r, b, ts, e, s, p);
        }
    }
}
