package android.os;
public final class Looper {
    private static final Looper MAIN = new Looper();
    private static final ThreadLocal<Boolean> UI = new ThreadLocal<Boolean>();
    public static Looper getMainLooper() { return MAIN; }
    public static Looper myLooper() { return Boolean.TRUE.equals(UI.get()) ? MAIN : null; }
    public static void setMainThread(boolean main) { UI.set(main); }
}
