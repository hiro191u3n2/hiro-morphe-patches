package android.content;
public class ContextWrapper extends Context {
    private final Context base;
    public ContextWrapper(Context base) { super(null); this.base = base; }
    public Context getBaseContext() { return base; }
    @Override public String getPackageName() { return base.getPackageName(); }
    @Override public Object getSystemService(String name) { return base.getSystemService(name); }
    @Override public Context getApplicationContext() { return base.getApplicationContext(); }
    @Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(name,mode);}
}
