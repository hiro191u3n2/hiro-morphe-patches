package app.hiro.newxui;

import java.text.NumberFormat;
import java.util.Locale;

/** Formats raw counts for secondary renderers that implement their own abbreviation. */
public final class ViewCountFormat {
    private ViewCountFormat() {}

    public static String groupIfEnabled(long count) {
        if (count < 1000 || !UiOptions.fullViewCounts()) return null;
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(count);
    }

    public static String groupIfEnabled(int count) {
        return groupIfEnabled((long) count);
    }
}
