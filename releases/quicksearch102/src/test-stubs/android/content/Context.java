package android.content;
import android.content.res.Resources;
import java.util.HashMap;
import java.util.Map;
public class Context {
    public static final String ACTIVITY_SERVICE = "activity";
    public static final String INPUT_METHOD_SERVICE = "input_method";
    public static final int MODE_PRIVATE = 0;
    public String packageName = "jp.ddo.sugihiro.quicksearch";
    public final Map<String,Object> services = new HashMap<>();
    public final Map<String,Boolean> preferences = new HashMap<>();
    private final Resources resources = new Resources();
    public String getPackageName() { return packageName; }
    public Resources getResources() { return resources; }
    public Object getSystemService(String name) { return services.get(name); }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return (key, value) -> preferences.containsKey(key) ? preferences.get(key) : value;
    }
}
