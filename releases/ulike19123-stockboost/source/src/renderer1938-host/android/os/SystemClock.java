package android.os;
public final class SystemClock {
    public static long now = 100L;
    public static long uptimeMillis() { return now; }
    public static long elapsedRealtime() { return now; }
}
