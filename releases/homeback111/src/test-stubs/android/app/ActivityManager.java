package android.app;
import android.content.ComponentName;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;
public class ActivityManager {
    public static class RecentTaskInfo {
        public Intent baseIntent;
        public ComponentName baseActivity;
        public int taskId;
        public int id;
        public int persistentId;
    }
    public static class AppTask {
        public final RecentTaskInfo info;
        public final List<Activity> activities = new ArrayList<Activity>();
        public boolean failInfo;
        public boolean failRemove;
        public boolean failExclude;
        public int removeCalls;
        public int excludeCalls;
        public boolean excluded;
        public Runnable onRemove;
        public AppTask(RecentTaskInfo value) { info = value; }
        public RecentTaskInfo getTaskInfo() {
            if (failInfo) throw new IllegalArgumentException("task disappeared");
            return info;
        }
        public void finishAndRemoveTask() {
            removeCalls++;
            if (failRemove) throw new IllegalStateException("task removal failed");
            for (Activity activity : activities) activity.finishing = true;
            if (onRemove != null) onRemove.run();
        }
        public void setExcludeFromRecents(boolean value) {
            excludeCalls++;
            if (failExclude) throw new IllegalStateException("exclude failed");
            excluded = value;
        }
    }
    public final List<AppTask> tasks = new ArrayList<AppTask>();
    public boolean failList;
    public int queries;
    public List<AppTask> getAppTasks() {
        queries++;
        if (failList) throw new IllegalStateException("service unavailable");
        return tasks;
    }
}
