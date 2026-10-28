package android.os;

/** In-memory values used by the native-link argument fixtures. */
public class Bundle {
    private final java.util.Map<String,Object> values=new java.util.LinkedHashMap<String,Object>();
    public Bundle() {}
    public Bundle(Bundle other) { values.putAll(other.values); }
    public Object get(String key) { return values.get(key); }
    public String getString(String key) { Object value=get(key);return value instanceof String?(String)value:null; }
    public String getString(String key,String fallback) { String value=getString(key);return value==null?fallback:value; }
    public void putString(String key,String value) { values.put(key,value); }
    public void putBoolean(String key,boolean value) { values.put(key,value); }
    public void putInt(String key,int value) { values.put(key,value); }
    public int getInt(String key,int fallback) { Object value=get(key);return value instanceof Integer?(Integer)value:fallback; }
    public boolean getBoolean(String key) { return Boolean.TRUE.equals(get(key)); }
    public boolean containsKey(String key) { return values.containsKey(key); }
    public void remove(String key) { values.remove(key); }
    public java.util.Set<String> keySet() { return values.keySet(); }
    /** Test-only object insertion; production reads the actual Bundle API. */
    public void fixtureObject(String key,Object value) { values.put(key,value); }
}
