package android.content;
public interface SharedPreferences {
    boolean getBoolean(String key,boolean defaultValue);
    Editor edit();
    interface Editor { Editor putBoolean(String key,boolean value);boolean commit(); }
}
