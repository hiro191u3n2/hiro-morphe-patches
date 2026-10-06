package android.app;
import android.content.ComponentName;
import android.content.Intent;
public class TaskInfo {
    public int taskId;
    public ComponentName baseActivity;
    public ComponentName topActivity;
    public Intent baseIntent;
    public int numActivities;
}
