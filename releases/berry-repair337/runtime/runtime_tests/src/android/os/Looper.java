package android.os;
public class Looper {
    private static final Looper MAIN = new Looper();
    public static boolean background;
    public static Looper myLooper() { return background ? null : MAIN; }
    public static Looper getMainLooper() { return MAIN; }
}
