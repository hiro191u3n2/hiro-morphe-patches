package app.hiro.newxui;

import java.text.NumberFormat;
import java.util.Locale;

/** Formats only the raw view-count value supplied by the scoped native hook. */
public final class ViewCountFormat {
    private static final ThreadLocal<FormatterState> FORMAT = new ThreadLocal<FormatterState>();

    private static final class FormatterState {
        final Locale locale;
        final NumberFormat number;

        FormatterState(Locale locale) {
            this.locale = locale;
            number = NumberFormat.getIntegerInstance(locale);
        }
    }

    private ViewCountFormat() {}

    /** Null asks the native bridge to produce its original label instead. */
    public static String formatOrNull(Long rawCount) {
        if (rawCount == null) return null;
        long count = rawCount.longValue();
        if (count < 0 || !UiOptions.fullViewCounts()) return null;

        Locale locale = Locale.getDefault();
        FormatterState state = FORMAT.get();
        if (state == null || !state.locale.equals(locale)) {
            // NumberFormat is mutable. Each thread keeps only its current-locale instance.
            state = new FormatterState(locale);
            FORMAT.set(state);
        }
        // The primitive-long overload preserves values beyond double's integer precision.
        return state.number.format(count);
    }
}
