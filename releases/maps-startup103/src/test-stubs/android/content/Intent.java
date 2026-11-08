package android.content;

import android.net.Uri;
import java.util.HashSet;
import java.util.Set;

public class Intent {
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000;
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000;
    private String action;
    private final Set<String> categories = new HashSet<String>();
    private Uri data;
    private String type;
    private Intent selector;
    private ComponentName component;
    private int flags;
    public Intent() { }
    public Intent(String action) { this.action = action; }
    public int getFlags() { return flags; }
    public Intent setFlags(int flags) { this.flags = flags; return this; }
    public Intent addFlags(int flags) { this.flags |= flags; return this; }
    public String getAction() { return action; }
    public Intent setAction(String action) { this.action = action; return this; }
    public boolean hasCategory(String category) { return categories.contains(category); }
    public Intent addCategory(String category) { categories.add(category); return this; }
    public Uri getData() { return data; }
    public Intent setData(Uri data) { this.data = data; return this; }
    public String getType() { return type; }
    public Intent setType(String type) { this.type = type; return this; }
    public Intent getSelector() { return selector; }
    public void setSelector(Intent selector) { this.selector = selector; }
    public ComponentName getComponent() { return component; }
    public Intent setComponent(ComponentName component) { this.component = component; return this; }
}
