package app.hiro.twitter.runtime;

import android.content.res.Resources;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Local display only. Accepts the original post's epoch milliseconds, never an age or post ID. */
public final class AbsoluteTime {
    private AbsoluteTime() {}
    private static final ThreadLocal<SimpleDateFormat> FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
        }
    };

    // null asks the wrapper to retain X's original behavior for absent/invalid data.
    public static String absoluteOrNull(long epochMillis) {
        if (epochMillis <= 0L || epochMillis > 253402214399999L) return null;
        try {
            SimpleDateFormat formatter = FORMAT.get();
            // No global formatter and no stale zone after the user travels or changes settings.
            formatter.setTimeZone(TimeZone.getDefault());
            return formatter.format(new Date(epochMillis));
        } catch (RuntimeException failure) {
            return null;
        }
    }

    public static String post(long epochMillis, Resources resources) {
        String text = absoluteOrNull(epochMillis);
        return text != null ? text : com.twitter.util.datetime.d.j(epochMillis, resources);
    }
    public static String accessiblePost(long epochMillis, Resources resources) {
        String text = absoluteOrNull(epochMillis);
        return text != null ? text : com.twitter.util.datetime.d.i(epochMillis, resources);
    }
    public static String detail(Format original, Object value) {
        if (original instanceof DateFormat) {
            long epochMillis = value instanceof Date ? ((Date) value).getTime()
                    : value instanceof Long ? ((Long) value).longValue() : 0L;
            String text = absoluteOrNull(epochMillis);
            if (text != null) return text;
        }
        return original.format(value);
    }
    public static String accessibleDetail(DateFormat original, Date value) {
        if (value != null) {
            String text = absoluteOrNull(value.getTime());
            if (text != null) return text;
        }
        return original.format(value);
    }
}
