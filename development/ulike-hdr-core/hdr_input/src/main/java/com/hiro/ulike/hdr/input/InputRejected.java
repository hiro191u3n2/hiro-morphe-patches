package com.hiro.ulike.hdr.input;

/** A rejected input is not a request to retry through an 8-bit/JPEG path. */
public final class InputRejected extends Exception {
    private static final long serialVersionUID = 1L;
    public enum Reason {
        ARGUMENT, FORMAT, DIMENSIONS, CROP, COLOR, STRIDE, BUFFER_BOUNDS,
        SAMPLE_ENCODING, ALLOCATION_LIMIT, OWNERSHIP, RESULT, TIMESTAMP
    }
    public final Reason reason;

    public InputRejected(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }
}
