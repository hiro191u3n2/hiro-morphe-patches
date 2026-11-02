package com.hiro.ulike;

import android.graphics.Bitmap;
import android.view.MotionEvent;

public final class ShutterBinding1994Test {
    static int assertions;
    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    static ProcessingTiming1947.Trace trace(Object callback) {
        return ProcessingTiming1947.forKey(callback);
    }
    public static void main(String[] args) throws Exception {
        Object cb = new Object();
        ShutterTiming1994.enter(new MotionEvent(0));
        try { ShotContext1932.begin(null, cb); } finally { ShutterTiming1994.exit(); }
        ProcessingTiming1947.Trace first = trace(cb);
        check(first.shutterDownNanos1994 > 0 && first.shutterDownNanos1994 <= first.started,
                "exact native callback trace inherits current real press");
        Bitmap bitmap = new Bitmap(4, 4);
        ShotContext1932.bindDelivery(cb, new Object(), bitmap);
        check(trace(bitmap) == first && trace(bitmap).shutterDownNanos1994 > 0,
                "same press follows exact bitmap identity");
        long recorded = first.shutterDownNanos1994;
        ShotContext1932.begin(null, cb);
        check(trace(cb) != first && trace(cb).shutterDownNanos1994 == 0,
                "reused callback outside dispatch cannot borrow previous press");
        check(first.shutterDownNanos1994 == recorded, "previous bitmap trace remains unchanged");
        Object recorder = new Object(), recorderCb = new Object();
        ShutterTiming1994.enter(new MotionEvent(0));
        try {
            ShotContext1932.beginRecorder(recorder, recorderCb);
            ProcessingTiming1947.Trace accepted = trace(recorderCb);
            ShotContext1932.begin(null, recorderCb);
            check(trace(recorderCb) == accepted && accepted.shutterDownNanos1994 > 0,
                    "recorder and native phases retain same consumed press");
            Object extra = new Object(); ShotContext1932.begin(null, extra);
            check(trace(extra).shutterDownNanos1994 == 0, "second capture in one dispatch cannot duplicate UI press");
        } finally { ShutterTiming1994.exit(); }
        Object workerCb = new Object();
        ShutterTiming1994.enter(new MotionEvent(0));
        try {
            Thread worker = new Thread(() -> ShotContext1932.begin(null, workerCb));
            worker.start(); worker.join();
            check(trace(workerCb).shutterDownNanos1994 == 0, "async capture has no guessed UI correlation");
        } finally { ShutterTiming1994.exit(); }
        Object up = new Object();
        ShutterTiming1994.enter(new MotionEvent(1));
        try { ShotContext1932.begin(null, up); } finally { ShutterTiming1994.exit(); }
        check(trace(up).shutterDownNanos1994 == 0, "release-trigger capture is not mislabeled finger-down");
        Object delayed = new Object();
        ShotContext1932.begin(null, delayed);
        check(trace(delayed).shutterDownNanos1994 == 0, "delayed same-thread request remains unobserved");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":" + assertions
                + ",\"exact_ui_bitmap_binding_verified\":true}");
    }
}
