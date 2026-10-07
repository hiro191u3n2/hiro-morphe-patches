package android.content;
import java.util.HashMap;
import java.util.Map;
public class Context {
    public static final String ACTIVITY_SERVICE = "activity";
    private final String packageName;
    private final Map<String, Object> services = new HashMap<String, Object>();
    public Context(String name) { packageName = name; }
    public String getPackageName() { return packageName; }
    public Object getSystemService(String name) { return services.get(name); }
    public Context getApplicationContext() { return this; }
    public void setService(String name, Object value) { services.put(name, value); }
}
