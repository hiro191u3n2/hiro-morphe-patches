package android.util;
public class Log {
    public static int warnings;
    public static int w(String tag, String message, Throwable error) { warnings++; return 0; }
}
