package android.content;

import android.net.Uri;
import java.util.HashSet;
import java.util.Set;

public class Intent {
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    private String action;
    private final Set<String> categories = new HashSet<String>();
    private Uri data;
    private String type;
    private Intent selector;
    private ComponentName component;
    public Intent() { }
    public Intent(String action) { this.action = action; }
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
