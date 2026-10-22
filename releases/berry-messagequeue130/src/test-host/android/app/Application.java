package android.app;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Application extends Context {
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle state);
        void onActivityStarted(Activity activity);
        void onActivityResumed(Activity activity);
        void onActivityPaused(Activity activity);
        void onActivityStopped(Activity activity);
        void onActivitySaveInstanceState(Activity activity, Bundle state);
        void onActivityDestroyed(Activity activity);
    }
    public static class MemoryPreferences implements SharedPreferences {
        public final Map<String, Boolean> values = new HashMap<String, Boolean>();
        public int writes;
        public boolean throwOnApply;
        public boolean getBoolean(String key, boolean fallback) {
            return values.containsKey(key) ? values.get(key) : fallback;
        }
        public Editor edit() {
            return new Editor() {
                private final Map<String, Boolean> pending = new HashMap<String, Boolean>();
                public Editor putBoolean(String key, boolean value) {
                    pending.put(key, value);
                    return this;
                }
                public void apply() {
                    if (throwOnApply) { throw new IllegalStateException("storage unavailable"); }
                    values.putAll(pending);
                    writes++;
                }
            };
        }
    }
    public final List<ActivityLifecycleCallbacks> callbacks = new ArrayList<ActivityLifecycleCallbacks>();
    public MemoryPreferences preferences;
    public int permission = PackageManager.PERMISSION_DENIED;
    public int preferencesReads;
    public boolean throwOnPreferences;
    public boolean throwOnRegister;
    public boolean throwOnUnregister;
    public Application() { this(new MemoryPreferences()); }
    public Application(MemoryPreferences preferences) { this.preferences = preferences; }
    @Override public SharedPreferences getSharedPreferences(String name, int mode) {
        preferencesReads++;
        if (throwOnPreferences) { throw new IllegalStateException("storage unavailable"); }
        return preferences;
    }
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        if (throwOnRegister) { throw new IllegalStateException("registration unavailable"); }
        callbacks.add(callback);
    }
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        if (throwOnUnregister) { throw new IllegalStateException("unregister unavailable"); }
        callbacks.remove(callback);
    }
    public void resume(Activity activity) {
        for (ActivityLifecycleCallbacks callback : new ArrayList<ActivityLifecycleCallbacks>(callbacks)) {
            callback.onActivityResumed(activity);
        }
    }
    public void pause(Activity activity) {
        for (ActivityLifecycleCallbacks callback : new ArrayList<ActivityLifecycleCallbacks>(callbacks)) {
            callback.onActivityPaused(activity);
        }
    }
    public void destroy(Activity activity) {
        activity.destroyed = true;
        for (ActivityLifecycleCallbacks callback : new ArrayList<ActivityLifecycleCallbacks>(callbacks)) {
            callback.onActivityDestroyed(activity);
        }
    }
}
