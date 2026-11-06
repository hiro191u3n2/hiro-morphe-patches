package android.app;

import android.content.Intent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Activity {
    public static final List<Activity> INSTANCES = new ArrayList<Activity>();
    private Intent intent;
    private final Map<Integer, View> views = new HashMap<Integer, View>();
    public boolean finishing;
    public boolean destroyed;
    public boolean windowFocus = true;
    public String throwingMethod;
    public Throwable failure = new IllegalStateException("Injected activity failure");
    public int systemServiceRequests;
    public int permissionRequests;
    public Activity() { INSTANCES.add(this); }
    private void check(String method) {
        if (method.equals(throwingMethod)) {
            if (failure instanceof Error) throw (Error) failure;
            throw (RuntimeException) failure;
        }
    }
    public Intent getIntent() { check("getIntent"); return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }
    public boolean isFinishing() { check("isFinishing"); return finishing; }
    public boolean isDestroyed() { check("isDestroyed"); return destroyed; }
    public boolean hasWindowFocus() { check("hasWindowFocus"); return windowFocus; }
    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(int id) { check("findViewById"); return (T) views.get(id); }
    public void putView(int id, View view) {
        if (view == null) views.remove(id); else views.put(id, view);
    }
    public Object getSystemService(String name) {
        systemServiceRequests++;
        throw new AssertionError("Startup recenter must use native Maps state, not location services");
    }
    public void requestPermissions(String[] permissions, int requestCode) {
        permissionRequests++;
        throw new AssertionError("Startup recenter must not request permissions");
    }
}
