package com.hiro.ulike.hdr.faceprobe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Thread-safe observations only. A passing ledger does not certify coordinates. */
public final class ProbeLedger {
    private final int width, height;
    private final List<String> events = new ArrayList<>();
    private boolean submitted, render, closed;
    private Integer initCode;
    private int faceCallbacks, earlyFaceCallbacks;
    private String failure;
    private float[][] points;
    private float[] scores;
    private double[] orientationError;
    private StockSdkIdentity sdkIdentity;

    public ProbeLedger(int width, int height) {
        if (width < 1 || height < 1 || (long)width * height > 4194304)
            throw new IllegalArgumentException("Bounded analysis image required");
        this.width = width; this.height = height;
    }
    private void event(String event) {
        if (events.size() < 64) events.add(event);
        else if (failure == null) failure = "Too many callbacks";
        notifyAll();
    }
    public synchronized void verifiedSdk(StockSdkIdentity identity) {
        if (closed || submitted || initCode != null || sdkIdentity != null || identity == null)
            throw new IllegalStateException("SDK identity must be recorded once before initialization");
        sdkIdentity = identity;
        event("sdk=" + identity.variant + ":" + identity.installedSha256);
    }
    public synchronized void initialized(int code) {
        if (closed) return;
        if (initCode != null) fail("Repeated initialization callback");
        initCode = code;
        event("init=" + code);
        if (code != 0) fail("Native initialization failed: " + code);
    }
    public synchronized void submit() {
        if (closed || submitted || initCode == null || initCode != 0 || failure != null)
            throw new IllegalStateException("Not ready for exactly one still");
        submitted = true; event("submit");
    }
    public synchronized void face(float[][] xy, float[] confidence) {
        if (closed) return;
        if (!submitted) { earlyFaceCallbacks++; event("face_before_submit"); return; }
        faceCallbacks++;
        if (faceCallbacks != 1) { fail("More than one face callback for one still"); return; }
        if (xy == null || confidence == null || xy.length != confidence.length || xy.length > 10) {
            fail("Invalid face array"); return;
        }
        float[][] copy = new float[xy.length][];
        for (int f = 0; f < xy.length; f++) {
            if (xy[f] == null || xy[f].length != 212 || !Float.isFinite(confidence[f]) ||
                    confidence[f] < 0 || confidence[f] > 1) { fail("Invalid 106-point face"); return; }
            copy[f] = xy[f].clone();
            for (float v : copy[f]) if (!Float.isFinite(v)) { fail("Nonfinite landmark"); return; }
        }
        points = copy; scores = confidence.clone(); event("face_count=" + xy.length);
    }
    public synchronized void rendered(int w, int h, int pixelCount) {
        if (closed) return;
        if (!submitted || render || w != width || h != height || pixelCount != width * height) {
            fail("Render completion geometry/count mismatch"); return;
        }
        render = true; event("render=" + w + "x" + h);
    }
    public synchronized void orientationEvidence(double[] errors) {
        if (closed) return;
        if (!submitted || errors == null || errors.length != 8 || orientationError != null) {
            fail("Invalid or repeated orientation evidence"); return;
        }
        for (double value : errors) if (!Double.isNaN(value) && (!Double.isFinite(value) || value < 0 || value > 255)) {
            fail("Invalid orientation error"); return;
        }
        orientationError = errors.clone();
    }
    public synchronized void status(int type, int code) {
        if (closed) return;
        event("picture_status=" + type + ":" + code);
        if (code < 0) fail("Native picture error: " + code);
    }
    public synchronized void fail(String reason) {
        if (closed) return;
        if (failure == null) failure = reason;
        event("failure=" + reason);
    }
    /** Timeout only stops waiting. The caller must still join native workers. */
    public synchronized boolean await(boolean initialization, long millis) throws InterruptedException {
        if (millis < 1 || millis > 30000) throw new IllegalArgumentException("Invalid callback deadline");
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
        while (!closed && failure == null && !(initialization ? initCode != null : render && points != null)) {
            long left = end - System.nanoTime();
            if (left <= 0) { fail(initialization ? "Initialization callback timeout" : "Still callback timeout"); break; }
            TimeUnit.NANOSECONDS.timedWait(this, left);
        }
        return failure == null && !closed && (initialization ? initCode != null && initCode == 0 : render && points != null);
    }
    /** Must only be called after unregister/stop/uninit have returned. */
    public synchronized Snapshot finish(boolean teardownReturned) {
        if (closed) throw new IllegalStateException("Already finished");
        if (!teardownReturned) fail("Native teardown did not complete");
        if (!render || points == null || faceCallbacks != 1) fail("Incomplete still observations");
        closed = true; notifyAll();
        return new Snapshot(width, height, failure, events, points, scores, earlyFaceCallbacks, orientationError, sdkIdentity);
    }
    public static final class Snapshot {
        public final int width, height, earlyFaceCallbacks;
        public final String failure;
        public final boolean callbacksObserved;
        public final String sdkLibrarySha256, sdkLibraryVariant;
        /** Always false: even successful native callbacks do not prove geometry. */
        public final boolean sourceCoordinateContractVerified = false;
        private final List<String> events;
        private final float[][] points;
        private final float[] scores;
        private final double[] orientationError;
        private Snapshot(int w, int h, String error, List<String> log, float[][] xy, float[] score, int early, double[] orientation, StockSdkIdentity sdk) {
            width = w; height = h; failure = error; callbacksObserved = error == null;
            events = new ArrayList<>(log); points = copy(xy); scores = score == null ? new float[0] : score.clone();
            earlyFaceCallbacks = early;
            orientationError = orientation == null ? new double[0] : orientation.clone();
            sdkLibrarySha256 = sdk == null ? null : sdk.installedSha256;
            sdkLibraryVariant = sdk == null ? null : sdk.variant;
        }
        public List<String> events() { return new ArrayList<>(events); }
        public float[][] rawPoints() { return copy(points); }
        public float[] rawScores() { return scores.clone(); }
        public double[] renderOrientationRgbMae() { return orientationError.clone(); }
        private static float[][] copy(float[][] data) {
            if (data == null) return new float[0][];
            float[][] out = new float[data.length][];
            for (int i = 0; i < data.length; i++) out[i] = data[i].clone();
            return out;
        }
    }
}
