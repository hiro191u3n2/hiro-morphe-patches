package android.content;
public class ComponentName {
    private final String pkg, cls;
    public ComponentName(String pkg, String cls) { this.pkg = pkg; this.cls = cls; }
    public String getPackageName() { return pkg; }
    public String getClassName() { return cls; }
}
