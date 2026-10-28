package android.content;
public class Intent {
    public static final String ACTION_MAIN="android.intent.action.MAIN",ACTION_VIEW="android.intent.action.VIEW",CATEGORY_LAUNCHER="android.intent.category.LAUNCHER";
    private String action;private android.net.Uri data;
    private final java.util.Map<String,String> extras=new java.util.HashMap<String,String>();
    public Intent putExtra(String key,String value){extras.put(key,value);return this;}
    public String getStringExtra(String key){return extras.get(key);}
    public Intent(Intent other){action=other.action;data=other.data;extras.putAll(other.extras);categories.addAll(other.categories);flags=other.flags;component=other.component;packageName=other.packageName;}
    public Intent removeExtra(String key){extras.remove(key);return this;}
    public android.os.Bundle getExtras(){android.os.Bundle result=new android.os.Bundle();for(java.util.Map.Entry<String,String> e:extras.entrySet())result.putString(e.getKey(),e.getValue());return result;}
    public Intent putExtras(android.os.Bundle b){for(String k:b.keySet())if(b.get(k) instanceof String)putExtra(k,b.getString(k));return this;}
    private final java.util.Set<String> categories=new java.util.HashSet<String>();
    public Intent(String value){action=value;}
    public Intent setAction(String value){action=value;return this;}public String getAction(){return action;}
    public Intent setData(android.net.Uri value){data=value;return this;}public android.net.Uri getData(){return data;}
    public Intent addCategory(String value){categories.add(value);return this;}public boolean hasCategory(String value){return categories.contains(value);}
    public static final int FLAG_ACTIVITY_CLEAR_TOP=0x04000000,FLAG_ACTIVITY_SINGLE_TOP=0x20000000;
    public int flags; public Intent(){} public Intent(Context c,Class<?> k){setComponent(new ComponentName(c.getPackageName(),k.getName()));}
    public Intent addFlags(int f){flags|=f;return this;}
    private ComponentName component;
    private String packageName;
    public Intent setComponent(ComponentName value) { component = value; return this; }
    public ComponentName getComponent() { return component; }
    public Intent setPackage(String value) { packageName = value; return this; }
    public String getPackage() { return packageName; }
}
