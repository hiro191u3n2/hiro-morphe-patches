package app.hiro.newxui;

import java.text.NumberFormat;
import java.util.Locale;

/** Formats only the raw view-count value supplied by the scoped native hook. */
public final class ViewCountFormat {
    private ViewCountFormat() {}

    /** Keep the native text when disabled or when the count is absent/invalid. */
    public static String format(Long rawCount, String original) {
        if (rawCount == null || rawCount.longValue() < 0 || !UiOptions.fullViewCounts()) {
            return original;
        }
        // Create per call: NumberFormat is mutable and not thread-safe, and Locale may change.
        // Select its primitive-long overload; converting through double would lose large counts.
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(rawCount.longValue());
    }
}
