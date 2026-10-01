package com.hiro.ulike.hdr.capture;

import java.util.Set;

/** No automatic SDR/JPEG, upscale, maximum-sensor-mode or hidden-camera fallback. */
public final class CapturePolicy {
    private CapturePolicy() {}
    public static void dimensions(int width, int height, long maxOwnedBytes, int[][] advertised) {
        if (width <= 0 || height <= 0 || (width & 1) != 0 || (height & 1) != 0)
            throw new IllegalArgumentException("positive even dimensions required");
        long pixels = (long) width * height;
        if (pixels > Integer.MAX_VALUE - 8L || maxOwnedBytes <= 0 || pixels * 3 > maxOwnedBytes)
            throw new IllegalArgumentException("owned P010 sample payload exceeds budget");
        boolean found = false;
        if (advertised != null) for (int[] s : advertised)
            if (s != null && s.length == 2 && s[0] == width && s[1] == height) found = true;
        if (!found) throw new IllegalArgumentException("requested size is not advertised for normal P010");
    }
    public static void range(long requiredProfile, Set<Long> profiles, Set<Long> sameRequest) {
        if (profiles == null || !profiles.contains(requiredProfile))
            throw new IllegalArgumentException("required HDR profile is not advertised");
        // Empty constraint set means no constraints. This module targets exactly one output.
        if (sameRequest == null || (!sameRequest.isEmpty() && !sameRequest.contains(requiredProfile)))
            throw new IllegalArgumentException("single-output HDR profile constraint rejected");
    }
    public static void rotation(int quarterTurnsClockwise) {
        if (quarterTurnsClockwise < 0 || quarterTurnsClockwise > 3)
            throw new IllegalArgumentException("explicit display orientation quarter-turns required");
    }
}
