package android.content;
public interface SharedPreferences {
 java.util.Map<String,?> getAll();String getString(String key,String fallback);Editor edit();
 interface Editor {Editor remove(String key);Editor putString(String key,String value);void apply();}
}
