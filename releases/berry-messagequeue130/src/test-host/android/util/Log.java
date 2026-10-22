package android.util;
import java.util.ArrayList;
import java.util.List;
public final class Log {
    public static final List<String> lines = new ArrayList<String>();
    public static boolean throwOnLog;
    public static int i(String tag, String message) {
        if (throwOnLog) { throw new IllegalStateException("logger unavailable"); }
        lines.add(tag + ": " + message);
        return 0;
    }
}
