package android.content;
import java.util.HashMap;
import java.util.Map;
public class Intent {
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    private int flags;
    private String action;
    private final Map<String,String> extras = new HashMap<>();
    private ComponentName component;
    public Intent() {}
    public Intent(String action) { this.action = action; }
    public Intent addFlags(int flags) { this.flags |= flags; return this; }
    public Intent setFlags(int flags) { this.flags = flags; return this; }
    public int getFlags() { return flags; }
    public String getAction() { return action; }
    public Intent putExtra(String key, String value) { extras.put(key,value); return this; }
    public String getStringExtra(String key) { return extras.get(key); }
    public Intent setComponent(ComponentName component) { this.component = component; return this; }
    public ComponentName getComponent() { return component; }
}
