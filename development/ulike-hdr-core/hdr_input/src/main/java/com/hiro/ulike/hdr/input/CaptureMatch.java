package com.hiro.ulike.hdr.input;

/** Identity and same-frame checks; no clock tolerance, nearest frame or stale-result fallback. */
public final class CaptureMatch {
    private CaptureMatch() {}
    public enum TimestampConvention { SENSOR_START_OF_EXPOSURE, UNKNOWN }

    /** Capture these identities at submission, not when a delayed callback is delivered. */
    public static final class Context {
        public final Object owner, session, reader, request, callback;
        public final long shot;
        public final String cameraId, physicalId;

        public Context(Object owner, Object session, Object reader, Object request,
                       Object callback, long shot, String cameraId, String physicalId) {
            if (owner == null || session == null || reader == null || request == null
                    || callback == null || shot < 0 || cameraId == null || cameraId.isEmpty()
                    || (physicalId != null && physicalId.isEmpty())) {
                throw new IllegalArgumentException("complete capture identities required");
            }
            this.owner = owner; this.session = session; this.reader = reader;
            this.request = request; this.callback = callback; this.shot = shot;
            this.cameraId = cameraId; this.physicalId = physicalId;
        }
    }

    /** Reader/session provenance, captured when this output reader is installed. */
    public static final class Source {
        public final Object owner, session, reader;
        public final String cameraId, physicalId;
        public final TimestampConvention timestampConvention;
        public Source(Object owner, Object session, Object reader, String cameraId, String physicalId,
                      TimestampConvention timestampConvention) {
            this.owner = owner; this.session = session; this.reader = reader;
            this.cameraId = cameraId; this.physicalId = physicalId;
            this.timestampConvention = timestampConvention;
        }
    }

    /** Supplies the live selection. The caller must serialize lifecycle changes with use/commit. */
    public interface Current { Context snapshot(); }

    /** Metadata from the total result, or its explicitly selected physical result. */
    public static final class Result {
        public final Context submitted;
        public final Object actualRequest;
        public final long sensorTimestampNs, frameNumber;
        public final String timestampPhysicalId, activePhysicalId;
        public final Long exposureTimeNs;
        public final Integer sensitivityIso;

        public Result(Context submitted, Object actualRequest, long sensorTimestampNs,
                      long frameNumber, String timestampPhysicalId, String activePhysicalId,
                      Long exposureTimeNs, Integer sensitivityIso) {
            this.submitted = submitted; this.actualRequest = actualRequest;
            this.sensorTimestampNs = sensorTimestampNs; this.frameNumber = frameNumber;
            this.timestampPhysicalId = timestampPhysicalId; this.activePhysicalId = activePhysicalId;
            this.exposureTimeNs = exposureTimeNs; this.sensitivityIso = sensitivityIso;
        }
    }

    public static void requireCurrent(Context expected, Context actual) throws InputRejected {
        if (expected == null || actual == null || expected.owner != actual.owner
                || expected.session != actual.session || expected.reader != actual.reader
                || expected.request != actual.request || expected.callback != actual.callback
                || expected.shot != actual.shot || !same(expected.cameraId, actual.cameraId)
                || !same(expected.physicalId, actual.physicalId)) {
            throw new InputRejected(InputRejected.Reason.OWNERSHIP, "capture selection changed or is incomplete");
        }
    }

    public static void requireFrame(Context expected, Source source, Result result,
                                    long imageTimestampNs) throws InputRejected {
        if (expected == null || source == null || source.owner != expected.owner
                || source.session != expected.session || source.reader != expected.reader
                || !same(source.cameraId, expected.cameraId)
                || !same(source.physicalId, expected.physicalId)) {
            throw new InputRejected(InputRejected.Reason.OWNERSHIP, "image reader/session/route mismatch");
        }
        if (result == null) throw new InputRejected(InputRejected.Reason.RESULT, "total result required");
        if (source.timestampConvention != TimestampConvention.SENSOR_START_OF_EXPOSURE)
            throw new InputRejected(InputRejected.Reason.TIMESTAMP, "sensor/start-of-exposure timestamp contract required");
        requireCurrent(expected, result.submitted);
        if (result.actualRequest != expected.request || result.frameNumber < 0
                || !same(result.timestampPhysicalId, expected.physicalId)) {
            throw new InputRejected(InputRejected.Reason.RESULT, "result request or timestamp provenance mismatch");
        }
        if (imageTimestampNs <= 0 || result.sensorTimestampNs <= 0
                || imageTimestampNs != result.sensorTimestampNs) {
            throw new InputRejected(InputRejected.Reason.TIMESTAMP, "image and result timestamps must match exactly");
        }
    }

    private static boolean same(String a, String b) { return a == null ? b == null : a.equals(b); }
}
