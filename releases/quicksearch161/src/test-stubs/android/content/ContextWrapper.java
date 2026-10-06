package android.content;
import android.content.res.Resources;
public class ContextWrapper extends Context {
    private final Context base;
    public ContextWrapper(Context base) { this.base = base; }
    public Context getBaseContext() { return base; }
    @Override public String getPackageName() { return base.getPackageName(); }
    @Override public Resources getResources() { return base.getResources(); }
    @Override public Object getSystemService(String name) { return base.getSystemService(name); }
}
