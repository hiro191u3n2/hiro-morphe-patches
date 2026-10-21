package android.os;
import java.util.HashMap;
import java.util.Set;
public class Bundle {
 private final HashMap<String,Object> values=new HashMap<String,Object>();
 public void putString(String key,String value) { values.put(key,value); }
 public void putBoolean(String key,boolean value) { values.put(key,Boolean.valueOf(value)); }
 public boolean containsKey(String key) { return values.containsKey(key); }
 public boolean isEmpty() { return values.isEmpty(); }
 public Set<String> keySet() { return values.keySet(); }
 public Object get(String key) { return values.get(key); }
}
