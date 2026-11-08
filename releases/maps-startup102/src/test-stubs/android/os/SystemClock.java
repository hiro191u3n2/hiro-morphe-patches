package android.os;

public final class SystemClock {
    private static long elapsed;
    private SystemClock() { }
    public static long elapsedRealtime() { return elapsed; }
    public static void setElapsedRealtime(long value) {
        if (value < 0) throw new IllegalArgumentException("Negative virtual time");
        elapsed = value;
    }
}
