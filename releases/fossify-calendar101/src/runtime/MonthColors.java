package app.hiro.fossifycalendar.runtime;

import android.content.Context;
import android.os.Looper;
import android.graphics.Paint;
import android.view.View;
import java.lang.reflect.Method;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Color-only hooks for Fossify Calendar 1.12.1's monthly app view and RemoteViews widget. */
public final class MonthColors {
    static final int SATURDAY = 0xff2196f3;
    static final int HOLIDAY = 0xffff0000;
    private static final String MANAGED_HOLIDAY_PREFIX = "managed-holiday:";
    private static final ThreadLocal<Set<Long>> RENDER_CALENDARS = new ThreadLocal<Set<Long>>();
    private static volatile Set<Long> lastCalendars = Collections.emptySet();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> GETTERS =
            new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>>();

    private MonthColors() { }

    /** One metadata read per background render, never one database query per date. */
    public static void beginRender(Context context) {
        Set<Long> calendars = lastCalendars;
        try {
            // A no-events/preview callback can run on the UI thread. Room must not be queried there.
            if (Looper.myLooper() != Looper.getMainLooper()) {
                Class<?> extensions = Class.forName("q50", false, MonthColors.class.getClassLoader());
                Object dao = extensions.getMethod("f", Context.class).invoke(null, context);
                Object rows = dao.getClass().getMethod("c").invoke(dao);
                if (rows instanceof Iterable<?>) {
                    HashSet<Long> found = new HashSet<Long>();
                    for (Object calendar : (Iterable<?>) rows) {
                        if (calendar != null && isHolidayCalendar(calendar)) {
                            Object id = get(calendar, "getId");
                            if (id instanceof Number) found.add(((Number) id).longValue());
                        }
                    }
                    calendars = Collections.unmodifiableSet(found);
                    lastCalendars = calendars;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Missing metadata never prevents the widget rendering; managed holiday IDs still work.
        } catch (RuntimeException ignored) {
            // Covers unavailable/closed Room databases during startup or shutdown.
        }
        RENDER_CALENDARS.set(calendars);
    }

    /** ISO first-day setting (Monday=1), zero-based displayed column, existing ARGB color. */
    public static int weekdayColor(int firstDayOfWeek, int column, int original) {
        if (firstDayOfWeek < 1 || firstDayOfWeek > 7 || column < 0 || column > 6) return original;
        return (firstDayOfWeek - 1 + column) % 7 == 5 ? recolor(original, SATURDAY) : original;
    }

    public static int dateColor(Context context, Object day, int original) {
        if (day == null) return original;
        try {
            Object rawCode = get(day, "getCode");
            if (!(rawCode instanceof String)) return original;
            LocalDate date = parseCode((String) rawCode);
            if (date == null) return original;
            Object events = get(day, "getDayEvents");
            if (events instanceof Iterable<?>) {
                Set<Long> calendars = RENDER_CALENDARS.get();
                if (calendars == null) calendars = lastCalendars;
                for (Object event : (Iterable<?>) events) {
                    if (isHolidayEvent(event, calendars)) return recolor(original, HOLIDAY);
                }
            }
            return date.getDayOfWeek().getValue() == 6 ? recolor(original, SATURDAY) : original;
        } catch (ReflectiveOperationException ignored) {
            return original;
        } catch (RuntimeException ignored) {
            return original;
        }
    }

    /** Refresh the UI thread's immutable snapshot, without running a Room query there. */
    public static void appBeginRender(View view) {
        if (enabled(view)) beginRender(view.getContext());
    }

    /** The native header can reuse its base Paint, so clone instead of recoloring it in place. */
    public static Paint appWeekdayPaint(View view, int column, Paint original) {
        if (original == null || !enabled(view)) return original;
        try {
            Class<?> extensions = Class.forName("q50", false, MonthColors.class.getClassLoader());
            Object config = extensions.getMethod("h", Context.class).invoke(null, view.getContext());
            Object first = get(config, "h");
            if (!(first instanceof Number)) return original;
            int color = weekdayColor(((Number) first).intValue(), column, original.getColor());
            if (color == original.getColor()) return original;
            Paint copy = new Paint(original);
            copy.setColor(color);
            return copy;
        } catch (ReflectiveOperationException ignored) {
            return original;
        } catch (RuntimeException ignored) {
            return original;
        }
    }

    /** Invoked only for the date numeral, after native selection/today styling. */
    public static void appDatePaint(View view, Object day, Paint paint) {
        if (paint == null || !enabled(view)) return;
        int original = paint.getColor();
        if (isToday(day)) paint.setColor(recolor(original, 0xff000000));
        else paint.setColor(dateColor(view.getContext(), day, original));
    }

    /** Match the filled today marker to the same date color as the widget. */
    public static void appCirclePaint(View view, Object day, Paint paint) {
        if (paint != null && enabled(view))
            paint.setColor(dateColor(view.getContext(), day, paint.getColor()));
    }

    /** Retain today's filled marker when selected, so black text stays visible on a dark theme. */
    public static boolean drawSelection(View view, Object day, boolean selected) {
        return selected && (!enabled(view) || !isToday(day));
    }

    private static boolean isToday(Object day) {
        if (day == null) return false;
        try {
            return Boolean.TRUE.equals(get(day, "isToday"));
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean enabled(View view) {
        if (view == null) return false;
        try {
            // Exact public field in the verified MonthView: y = isPrintVersion.
            return !view.getClass().getField("y").getBoolean(view);
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static int recolor(int original, int color) {
        return (original & 0xff000000) | (color & 0x00ffffff);
    }

    private static LocalDate parseCode(String code) {
        if (code.length() != 8) return null;
        int value = 0;
        for (int i = 0; i < 8; i++) {
            char c = code.charAt(i);
            if (c < '0' || c > '9') return null;
            value = value * 10 + c - '0';
        }
        int year = value / 10000;
        if (year == 0) return null;
        try {
            return LocalDate.of(year, value / 100 % 100, value % 100);
        } catch (DateTimeException invalid) {
            return null;
        }
    }

    private static boolean isHolidayEvent(Object event, Set<Long> calendars) {
        if (event == null) return false;
        try {
            if (!Boolean.TRUE.equals(get(event, "getIsAllDay"))
                    || Boolean.TRUE.equals(get(event, "isTask"))
                    || Boolean.TRUE.equals(get(event, "isEventCanceled"))) return false;
            Object importId = get(event, "getImportId");
            if (importId instanceof String && ((String) importId).startsWith(MANAGED_HOLIDAY_PREFIX)) return true;
            Object id = get(event, "getCalendarId");
            return id instanceof Number && calendars.contains(((Number) id).longValue());
        } catch (ReflectiveOperationException ignored) {
            return false;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isHolidayCalendar(Object calendar) throws ReflectiveOperationException {
        Object type = get(calendar, "getType");
        if (type instanceof Number && ((Number) type).intValue() == 3) return true;
        // Synced Google holiday calendars do not carry Fossify's local HOLIDAY_EVENT=3 marker.
        // Match only complete recognized calendar names, never event titles or red event colors.
        return isHolidayName(get(calendar, "getTitle"))
                || isHolidayName(get(calendar, "getCaldavDisplayName"));
    }

    static boolean isHolidayName(Object name) {
        if (!(name instanceof String)) return false;
        String value = ((String) name).trim().toLowerCase(Locale.ROOT);
        return value.equals("祝日") || value.equals("日本の祝日")
                || value.equals("日本の祝祭日") || value.equals("国民の祝日")
                || value.equals("holidays") || value.equals("public holidays")
                || value.equals("japanese holidays") || value.equals("japan holidays")
                || value.equals("holidays in japan") || value.equals("japan public holidays")
                || value.equals("japanese public holidays");
    }

    private static Object get(Object receiver, String name) throws ReflectiveOperationException {
        Class<?> type = receiver.getClass();
        ConcurrentHashMap<String, Method> methods = GETTERS.get(type);
        if (methods == null) {
            ConcurrentHashMap<String, Method> created = new ConcurrentHashMap<String, Method>();
            ConcurrentHashMap<String, Method> prior = GETTERS.putIfAbsent(type, created);
            methods = prior == null ? created : prior;
        }
        Method method = methods.get(name);
        if (method == null) {
            Method found = type.getMethod(name);
            Method prior = methods.putIfAbsent(name, found);
            method = prior == null ? found : prior;
        }
        return method.invoke(receiver);
    }
}
