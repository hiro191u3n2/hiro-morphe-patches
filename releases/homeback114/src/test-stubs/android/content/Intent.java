package android.content;
public class Intent {
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
