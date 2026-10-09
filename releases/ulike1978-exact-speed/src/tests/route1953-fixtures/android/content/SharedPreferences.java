package android.content;
import java.util.Map;
public interface SharedPreferences {
 String getString(String key,String fallback);
 Map<String,?> getAll();
 Editor edit();
 interface Editor { Editor clear();Editor remove(String key);Editor putString(String key,String value);void apply(); }
}
