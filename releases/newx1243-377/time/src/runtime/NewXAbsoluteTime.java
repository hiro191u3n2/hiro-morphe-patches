package app.hiro.newx.runtime;

import android.content.Context;
import android.content.res.Resources;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import kotlin.time.Instant;

/** Only called by verified post byline, post detail and post accessibility sites. */
public final class NewXAbsoluteTime {
    private NewXAbsoluteTime() {}
    private static final ThreadLocal<SimpleDateFormat> FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US);
        }
    };

    public static String absoluteOrNull(long millis) {
        if (millis <= 0L || millis > 253402214399999L) return null;
        try {
            SimpleDateFormat format = FORMAT.get();
            format.setTimeZone(TimeZone.getDefault());
            return format.format(new Date(millis));
        } catch (RuntimeException failure) { return null; }
    }

    public static String fromInstant(Instant value) {
        if (value == null) return null;
        // Verified in 12.43: seconds * 1000 + nanoseconds / 1000000.
        return absoluteOrNull(value.e());
    }

    public static String post(Instant value, Resources resources, com.x.clock.c clock) {
        String result = fromInstant(value);
        return result != null ? result : com.x.android.utils.i2.i(value, resources, clock);
    }
    public static String detail(Context context, Instant value, kotlinx.datetime.d0 zone) {
        String result = fromInstant(value);
        return result != null ? result : com.x.android.utils.i2.g(context, value, zone);
    }
    public static String accessible(Instant value, Resources resources, com.x.clock.c clock, boolean flag) {
        String result = fromInstant(value);
        return result != null ? result : com.x.android.utils.i2.d(value, resources, clock, flag);
    }
}
