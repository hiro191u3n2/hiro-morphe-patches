package app.morphe.extension.crimera.settings;
/** Compile-only signature stub, never packaged. */
public final class SettingsRegistry {
    public static void registerCategory(String id, String title, String summary, String icon, int order) {}
    public static void registerToggle(String parent, String id, String title, String summary, int order) {}
    public static void configureToggle(String id, boolean value, boolean restart) {}
    public static boolean getBooleanOrDefault(String id, boolean value) { return value; }
}
