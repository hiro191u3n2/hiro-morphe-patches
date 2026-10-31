package android.content;
import java.util.HashMap;
import java.util.Map;
public class Context {
    public static final int MODE_PRIVATE=0;
    private final Map<String,SharedPreferences> preferences=new HashMap<String,SharedPreferences>();
    public SharedPreferences getSharedPreferences(String name,int mode){
        SharedPreferences value=preferences.get(name);
        if(value==null){value=new SharedPreferences(){
            final Map<String,Boolean> values=new HashMap<String,Boolean>();
            public boolean getBoolean(String key,boolean fallback){return values.containsKey(key)?values.get(key):fallback;}
            public Editor edit(){return new Editor(){final Map<String,Boolean> changes=new HashMap<String,Boolean>();
                public Editor putBoolean(String key,boolean v){changes.put(key,v);return this;}
                public boolean commit(){values.putAll(changes);return true;}};}
        };preferences.put(name,value);}return value;
    }
    public static final String POWER_SERVICE="power",KEYGUARD_SERVICE="keyguard";
    public static final String ACTIVITY_SERVICE = "activity";
    private final String packageName;
    private final Map<String, Object> services = new HashMap<String, Object>();
    public Context(String name) { packageName = name; }
    public String getPackageName() { return packageName; }
    public Object getSystemService(String name) { return services.get(name); }
    public Context getApplicationContext() { return this; }
    public void setService(String name, Object value) { services.put(name, value); }
}
