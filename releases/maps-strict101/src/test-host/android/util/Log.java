package android.util;

import java.util.ArrayList;
import java.util.List;

/** Host-only log capture. Never package this class into the Android extension. */
public final class Log {
    public static final List<String> messages = new ArrayList<String>();
    public static synchronized int w(String tag, String message) {
        messages.add(tag + ": " + message);
        return 0;
    }
}
