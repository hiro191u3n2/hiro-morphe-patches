package com.hiro.ulike;

/** Pure capture policy. Every exposure change is applied only to supplementary
 * still requests; the repeating preview request is never changed. */
public final class CapturePolicy1933 {
    private CapturePolicy1933() { }

    public static final long MEMORY_RESERVE = 72L * 1024 * 1024;
    public static final long MAX_EXPOSURE_NS = 66666666L;
    public static final long MOVING_EXPOSURE_NS = 10000000L;
    public static final long MIN_EXPOSURE_NS = 1000000L;

    public static final class Decision {
        public final long exposureNanos;
        public final int iso;
        public final int frames;
        public final boolean changedExposure;
        public Decision(long exposure, int sensitivity, int count, boolean changed) {
            exposureNanos = exposure; iso = sensitivity;
            frames = count; changedExposure = changed;
        }
    }

    /** Full-resolution buffers, a fusion output and conversion/workspace reserve.
     * Refusing a burst leaves the already-working single-frame path intact. */
    public static int frameLimit(int width, int height, long available) {
        long pixels = (long) width * height;
        if (width < 2 || height < 2 || (width & 1) != 0 || (height & 1) != 0
                || pixels < 4000000L || pixels > 32000000L) return 0;
        long bytes = pixels + pixels / 2;
        if (available <= MEMORY_RESERVE) return 0;
        int count = (int) Math.min(4, (available - MEMORY_RESERVE) / bytes - 2);
        return count >= 2 ? count : 0;
    }

    public static boolean night(int iso, long exposureNanos, float darkness) {
        return iso >= 500 || exposureNanos >= 20000000L
                || darkness > 0.58f;
    }

    /** Unknown motion never earns a longer exposure. The exposure/ISO product
     * stays within 3% of the reference so the transition does not change brightness. */
    public static Decision choose(long exposure, int iso, long minExposure,
            long maxExposure, int minIso, int maxIso, int maxFrames,
            boolean manual, boolean night, boolean reliable,
            float motion, float movingFraction, float shift, long interval) {
        int limit = Math.max(2, Math.min(4, maxFrames));
        int count = night ? limit : Math.min(3, limit);
        if (!valid(exposure, iso, minExposure, maxExposure, minIso, maxIso))
            return new Decision(exposure, iso, 2, false);
        if (!night) return new Decision(exposure, iso, count, false);
        boolean moving = !reliable || motion >= 0.24f || movingFraction >= 0.12f;
        boolean still = reliable && motion < 0.065f && movingFraction < 0.025f;
        // Global camera displacement over the measured interval also limits how
        // long a future frame may expose, even when registration can align it.
        double speed = interval > 0 && shift >= 0
                ? shift / (interval / 1000000000.0) : Double.POSITIVE_INFINITY;
        if (moving) count = 2;
        else if (!still) count = Math.min(3, count);
        if (!manual) return new Decision(exposure, iso, count, false);

        long wanted = exposure;
        if (moving) wanted = Math.min(exposure, MOVING_EXPOSURE_NS);
        else if (still) {
            wanted = Math.min(MAX_EXPOSURE_NS, saturatedTwice(exposure));
            if (speed > 0 && speed < Double.POSITIVE_INFINITY)
                wanted = Math.min(wanted, (long) (1250000000.0 / speed));
            wanted = Math.max(exposure, wanted);
        }
        wanted = Math.max(minExposure, Math.max(MIN_EXPOSURE_NS,
                Math.min(maxExposure, wanted)));
        // Do not double the ISO without limit simply to satisfy a short shutter.
        int allowedIso = Math.min(maxIso, Math.max(iso, (int)Math.min(12800L, (long)iso * 4)));
        double product = (double) exposure * iso;
        long sensitivity = Math.round(product / wanted);
        if (sensitivity < minIso || sensitivity > allowedIso) {
            sensitivity = Math.max(minIso, Math.min(allowedIso, sensitivity));
            wanted = Math.round(product / sensitivity);
        }
        // A sensor may already expose below our preferred 1 ms floor. Motion
        // and indeterminate probes must never make that real exposure longer.
        if (!still && wanted > exposure)
            return new Decision(exposure, iso, count, false);
        if (wanted < minExposure || wanted > maxExposure || wanted <= 0
                || sensitivity < minIso || sensitivity > maxIso)
            return new Decision(exposure, iso, count, false);
        double ratio = (double) wanted * sensitivity / product;
        boolean changed = wanted != exposure && Math.abs(ratio - 1.0) <= 0.03
                && (wanted >= exposure * 1.18 || wanted <= exposure / 1.18);
        return changed ? new Decision(wanted, (int) sensitivity, count, true)
                : new Decision(exposure, iso, count, false);
    }

    public static boolean compatible(long referenceTimestamp, long timestamp,
            long referenceExposure, long exposure, int referenceIso, int iso) {
        if (referenceTimestamp <= 0 || timestamp <= referenceTimestamp
                || timestamp - referenceTimestamp > 6000000000L
                || referenceExposure <= 0 || exposure <= 0
                || referenceIso <= 0 || iso <= 0) return false;
        return Math.abs((double) exposure / referenceExposure - 1.0) <= 0.035
                && Math.abs((double) iso / referenceIso - 1.0) <= 0.035;
    }

    private static boolean valid(long e, int i, long minE, long maxE, int minI, int maxI) {
        return e > 0 && i > 0 && minE > 0 && maxE >= minE
                && minI > 0 && maxI >= minI && e >= minE && e <= maxE
                && i >= minI && i <= maxI;
    }

    private static long saturatedTwice(long value) {
        return value > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : value * 2;
    }
}
