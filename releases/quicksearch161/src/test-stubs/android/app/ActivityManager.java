package android.app;
import java.util.ArrayList;
import java.util.List;
public class ActivityManager {
    public final List<AppTask> tasks = new ArrayList<>();
    public RuntimeException readFailure;
    public List<AppTask> getAppTasks() {if(readFailure!=null)throw readFailure;return tasks;}
    public static class RecentTaskInfo extends TaskInfo {public int id,persistentId;}
    public static class AppTask {
        public final RecentTaskInfo info=new RecentTaskInfo();
        public int removals;
        public RuntimeException infoFailure,removeFailure;
        public RecentTaskInfo getTaskInfo(){if(infoFailure!=null)throw infoFailure;return info;}
        public void finishAndRemoveTask(){if(removeFailure!=null)throw removeFailure;removals++;}
    }
}
