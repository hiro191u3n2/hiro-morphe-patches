package android.view;

import java.util.ArrayList;
import java.util.List;

public class View {
    public static final List<View> INSTANCES = new ArrayList<View>();
    public boolean shown = true;
    public boolean enabled = true;
    public Object windowToken = new Object();
    public String throwingMethod;
    public Throwable failure = new IllegalStateException("Injected view failure");
    public int syntheticClickCalls;
    public View() { INSTANCES.add(this); }
    private void check(String method) {
        if (method.equals(throwingMethod)) {
            if (failure instanceof Error) throw (Error) failure;
            throw (RuntimeException) failure;
        }
    }
    public boolean isShown() { check("isShown"); return shown; }
    public boolean isEnabled() { check("isEnabled"); return enabled; }
    public Object getWindowToken() { check("getWindowToken"); return windowToken; }
    public boolean performClick() {
        syntheticClickCalls++;
        throw new AssertionError("Startup recenter must not synthesize a click");
    }
    public boolean callOnClick() {
        syntheticClickCalls++;
        throw new AssertionError("Startup recenter must not synthesize a click");
    }
}
