package android.content;
import java.util.HashMap;
public class Context {
 public static final int MODE_PRIVATE=0;
 public boolean broken;
 public final HashMap<String,Prefs> stores=new HashMap<String,Prefs>();
 public Object getSystemService(String key){return null;}
 public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String k,int mode){if(broken)throw new IllegalStateException("disk");Prefs p=stores.get(k);if(p==null){p=new Prefs();stores.put(k,p);}return p;}
 public static final class Prefs implements SharedPreferences,SharedPreferences.Editor {
  public final HashMap<String,Object> values=new HashMap<String,Object>();public boolean broken;
  public String getString(String k,String f){Object v=values.get(k);return v instanceof String?(String)v:f;}
  public long getLong(String k,long f){Object v=values.get(k);return v instanceof Long?(Long)v:f;}
  public Editor edit(){if(broken)throw new IllegalStateException("edit");return this;}
  public Editor putString(String k,String v){values.put(k,v);return this;}
  public Editor putLong(String k,long v){values.put(k,Long.valueOf(v));return this;}
  public void apply(){if(broken)throw new IllegalStateException("write");}
 }
}
