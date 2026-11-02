package com.hiro.ulike;

/** Replaces only the audited still-photo BoostManager.g call site.
 * No camera, bitmap, vendor boost API or receiver state is accessed. This is
 * a count of suppressed request sites, not proof of previous OS activation
 * or an improvement in device responsiveness. */
public final class CaptureBoost19123 {
    private CaptureBoost19123() {}
    private static long suppressed;

    /** The original receiver is passed to preserve the call-site register shape,
     * but is deliberately not retained, inspected or invoked. */
    public static synchronized void suppress(Object ignoredReceiver) {
        if (suppressed < Long.MAX_VALUE) suppressed++;
    }

    public static synchronized long suppressedCount() { return suppressed; }

    public static String summary19123() {
        return "標準撮影CPUブースト要求: 抑止 " + suppressedCount()
            + "回（この起動の撮影要求箇所の累計。撮影成功回数・OSへの適用・操作改善量ではありません）";
    }
}
