package android.content;
public class Intent {
    private ComponentName component;
    private String packageName;
    public Intent setComponent(ComponentName value) { component = value; return this; }
    public ComponentName getComponent() { return component; }
    public Intent setPackage(String value) { packageName = value; return this; }
    public String getPackage() { return packageName; }
}
