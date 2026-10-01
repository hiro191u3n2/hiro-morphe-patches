package com.hiro.ulike.hdr.capture;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public final class CaptureBoundaryTest {
    private static int assertions;
    private static final class Buffer implements AutoCloseable {
        int closes;
        final boolean throwClose;
        Buffer() { this(false); }
        Buffer(boolean throwClose) { this.throwClose = throwClose; }
        @Override public void close() throws Exception { closes++; if (throwClose) throw new Exception("close failure"); }
    }
    private interface Check { void run() throws Exception; }
    private static void eq(boolean value) { assertions++; if (!value) throw new AssertionError(assertions); }
    private static void rejects(Check check) throws Exception {
        boolean rejected = false;
        try { check.run(); } catch (Exception expected) { rejected = true; }
        eq(rejected);
    }
    public static void main(String[] args) throws Exception {
        StillJoin<Buffer, Object> join = new StillJoin<Buffer, Object>();
        Object request = new String("request"), result = new Object();
        Buffer image = new Buffer();
        join.begin(request);
        eq(join.image(image, 7) == null);
        eq(join.result(new String("request"), result, 7) == null); // identity, not equality
        StillJoin.Pair<Buffer, Object> pair = join.result(request, result, 7);
        eq(pair.image() == image && pair.result == result && pair.request == request && pair.timestampNs == 7);
        eq(!join.active() && image.closes == 0);
        pair.close(); pair.close(); eq(image.closes == 1); rejects(pair::image);

        join.begin(request); image = new Buffer();
        eq(join.result(request, result, 21) == null);
        pair = join.image(image, 21); eq(pair.image() == image);
        pair.close(); eq(image.closes == 1);

        join.begin(request); final Buffer mismatch = new Buffer();
        join.image(mismatch, 11);
        rejects(() -> join.result(request, result, 12));
        eq(mismatch.closes == 1 && !join.active());
        Buffer late = new Buffer(); eq(join.image(late, 12) == null && late.closes == 1);

        join.begin(request); Buffer first = new Buffer(); Buffer duplicate = new Buffer();
        join.image(first, 15); rejects(() -> join.image(duplicate, 15));
        eq(first.closes == 1 && duplicate.closes == 1 && !join.active());
        join.begin(request); join.result(request, result, 13);
        rejects(() -> join.result(request, result, 13)); eq(!join.active());

        join.begin(request); Buffer timedOut = new Buffer(); join.image(timedOut, 10);
        rejects(() -> join.begin(new Object()));
        join.cancel(); join.cancel(); eq(timedOut.closes == 1);
        eq(join.result(request, result, 10) == null);
        join.begin(request); Buffer badTimestamp = new Buffer();
        rejects(() -> join.image(badTimestamp, 0)); eq(badTimestamp.closes == 1 && !join.active());
        join.begin(request); rejects(() -> join.result(request, result, -1)); eq(!join.active());

        join.begin(request); Buffer throwing = new Buffer(true); join.image(throwing, 15);
        rejects(join::cancel); eq(!join.active() && throwing.closes == 1);
        join.begin(request); Buffer closePending = new Buffer(); join.image(closePending, 17);
        join.close(); join.close(); eq(closePending.closes == 1);
        rejects(() -> join.begin(request));
        Buffer afterClose = new Buffer(); eq(join.image(afterClose, 22) == null && afterClose.closes == 1);

        int[][] nativeSizes = {{4080, 3060}, {1920, 1080}};
        CapturePolicy.dimensions(4080, 3060, 4080L * 3060 * 3, nativeSizes); eq(true);
        rejects(() -> CapturePolicy.dimensions(5712, 4284, 100000000, nativeSizes));
        rejects(() -> CapturePolicy.dimensions(4080, 3060, 4080L * 3060 * 3 - 1, nativeSizes));
        rejects(() -> CapturePolicy.dimensions(4079, 3060, 100000000, nativeSizes));
        rejects(() -> CapturePolicy.dimensions(4080, 3060, 100000000, null));
        rejects(() -> CapturePolicy.dimensions(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1, Long.MAX_VALUE, nativeSizes));
        CapturePolicy.range(2L, new HashSet<Long>(Arrays.asList(1L, 2L)), Collections.<Long>emptySet()); eq(true);
        CapturePolicy.range(2L, Collections.singleton(2L), Collections.singleton(2L)); eq(true);
        rejects(() -> CapturePolicy.range(2L, Collections.singleton(1L), Collections.<Long>emptySet()));
        rejects(() -> CapturePolicy.range(2L, Collections.singleton(2L), Collections.singleton(1L)));
        rejects(() -> CapturePolicy.range(2L, Collections.singleton(2L), null));
        for (int q = 0; q < 4; q++) { CapturePolicy.rotation(q); eq(true); }
        rejects(() -> CapturePolicy.rotation(-1)); rejects(() -> CapturePolicy.rotation(4));
        System.out.println("Capture boundary host assertions PASS: " + assertions);
    }
}
