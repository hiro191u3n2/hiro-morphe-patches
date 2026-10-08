package android.content;
public interface SharedPreferences {
 String getString(String k,String fallback);long getLong(String k,long fallback);Editor edit();
 interface Editor {Editor putString(String k,String v);Editor putLong(String k,long v);void apply();}
}
