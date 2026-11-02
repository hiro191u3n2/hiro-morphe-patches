package android.os;
public final class SystemClock {
    private static final ThreadLocal<Long> NOW=new ThreadLocal<Long>();
    public static long uptimeNanos() { Long v=NOW.get();return v==null?1000000000L:v.longValue(); }
    public static long uptimeMillis() { return uptimeNanos()/1000000L; }
    public static void setNanos(long value) { NOW.set(Long.valueOf(value)); }
    public static void advance(long value) { setNanos(uptimeNanos()+value); }
}
