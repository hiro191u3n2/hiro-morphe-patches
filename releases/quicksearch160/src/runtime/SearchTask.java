package app.hiro.quicksearch.runtime;

import android.app.Activity;
import android.content.Intent;

/** Runtime helpers used only by the two verified MainActivity call sites. */
public final class SearchTask {
    private SearchTask() {}

    /** Called inside the application's existing exit-after-search ON branch. */
    public static void finish(Activity activity) {
        if (activity.isTaskRoot()) {
            activity.finishAndRemoveTask();
        } else {
            // A caller can host the search activity (e.g. SEARCH_LONG_PRESS).
            // End this activity without deleting the caller's task.
            activity.finish();
        }
    }

    /** Keep the external result alive when this search task is about to be removed. */
    public static void startExternal(Activity activity, Intent intent) {
        if (activity.getSharedPreferences(activity.getPackageName() + "_preferences", 0)
                .getBoolean("exit", false)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        activity.startActivity(intent);
    }
}
