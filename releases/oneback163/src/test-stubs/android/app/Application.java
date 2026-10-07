package android.app;
import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
public class Application extends Context {
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle saved);
        void onActivityStarted(Activity activity);
        void onActivityResumed(Activity activity);
        void onActivityPaused(Activity activity);
        void onActivityStopped(Activity activity);
        void onActivitySaveInstanceState(Activity activity, Bundle saved);
        void onActivityDestroyed(Activity activity);
        default void onActivityPostCreated(Activity activity, Bundle saved) { }
        default void onActivityPostResumed(Activity activity) { }
    }
    private final List<ActivityLifecycleCallbacks> callbacks = new ArrayList<ActivityLifecycleCallbacks>();
    public int registrations;
    public Application(String packageName) { super(packageName); }
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        registrations++; callbacks.add(callback);
    }
    private List<ActivityLifecycleCallbacks> copy() { return new ArrayList<ActivityLifecycleCallbacks>(callbacks); }
    public void created(Activity activity) {
        for (ActivityLifecycleCallbacks callback : copy()) callback.onActivityCreated(activity, null);
        for (ActivityLifecycleCallbacks callback : copy()) callback.onActivityPostCreated(activity, null);
    }
    public void resumed(Activity activity) {
        for (ActivityLifecycleCallbacks callback : copy()) callback.onActivityResumed(activity);
        for (ActivityLifecycleCallbacks callback : copy()) callback.onActivityPostResumed(activity);
    }
    public void paused(Activity activity) {
        for (ActivityLifecycleCallbacks callback : copy()) callback.onActivityPaused(activity);
    }
    public void destroyed(Activity activity) {
        activity.destroyed = true;
        for (ActivityLifecycleCallbacks callback : copy()) callback.onActivityDestroyed(activity);
    }
}
