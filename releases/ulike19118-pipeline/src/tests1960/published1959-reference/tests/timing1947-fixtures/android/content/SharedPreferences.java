package android.content;
public interface SharedPreferences {
 java.util.Map<String,?> getAll();
 String getString(String k,String fallback);long getLong(String k,long fallback);Editor edit();
 interface Editor {Editor clear();Editor remove(String key);Editor putString(String k,String v);Editor putLong(String k,long v);void apply();}
}
