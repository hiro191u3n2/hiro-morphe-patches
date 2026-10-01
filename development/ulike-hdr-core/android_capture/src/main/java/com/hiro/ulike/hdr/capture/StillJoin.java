package com.hiro.ulike.hdr.capture;

/** One in-flight still, serialized by its caller. Takes exclusive image ownership at entry. */
public final class StillJoin<I extends AutoCloseable, R> implements AutoCloseable {
    private Object request;
    private I image;
    private R result;
    private long imageTime, resultTime;
    private boolean closed;

    public static final class Pair<I extends AutoCloseable, R> implements AutoCloseable {
        public final Object request;
        public final R result;
        public final long timestampNs;
        private I image;
        Pair(Object request, I image, R result, long timestampNs) {
            this.request = request; this.image = image; this.result = result;
            this.timestampNs = timestampNs;
        }
        public I image() {
            if (image == null) throw new IllegalStateException("pair image closed");
            return image;
        }
        @Override public void close() throws Exception {
            I old = image; image = null;
            if (old != null) old.close();
        }
    }

    public void begin(Object submittedRequest) {
        if (closed || request != null || submittedRequest == null)
            throw new IllegalStateException("open, idle join and exact submitted request required");
        request = submittedRequest;
    }

    /** Unsolicited/late images are closed. Duplicate or invalid images abort the active shot. */
    public Pair<I, R> image(I ownedImage, long timestampNs) throws Exception {
        if (ownedImage == null) throw new NullPointerException("image");
        if (closed || request == null) { ownedImage.close(); return null; }
        if (image != null || timestampNs <= 0) {
            try { ownedImage.close(); } finally { cancel(); }
            throw new IllegalStateException("duplicate image or invalid timestamp");
        }
        image = ownedImage; imageTime = timestampNs;
        return ready();
    }

    /** Results for any other request identity are ignored, never paired with current pixels. */
    public Pair<I, R> result(Object submittedRequest, R totalResult, long timestampNs)
            throws Exception {
        if (closed || request == null || request != submittedRequest) return null;
        if (result != null || totalResult == null || timestampNs <= 0) {
            cancel();
            throw new IllegalStateException("duplicate or invalid total result");
        }
        result = totalResult; resultTime = timestampNs;
        return ready();
    }

    private Pair<I, R> ready() throws Exception {
        if (image == null || result == null) return null;
        if (imageTime != resultTime) {
            cancel();
            throw new IllegalStateException("image/result sensor timestamps differ");
        }
        Pair<I, R> pair = new Pair<I, R>(request, image, result, imageTime);
        image = null; result = null; request = null; imageTime = resultTime = 0;
        return pair;
    }

    public boolean active() { return request != null; }
    /** Timeout/lens-switch/session-close: forget request before closing the retained image. */
    public void cancel() throws Exception {
        I old = image;
        request = null; image = null; result = null; imageTime = resultTime = 0;
        if (old != null) old.close();
    }
    @Override public void close() throws Exception { closed = true; cancel(); }
}
