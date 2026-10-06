package android.content;

/** Minimal host-test contract; never included in the Android extension. */
public interface SharedPreferences {
    boolean getBoolean(String key, boolean defaultValue);
}
