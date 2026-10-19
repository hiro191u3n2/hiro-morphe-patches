package app.hiro.fossifygallery.runtime;

import android.app.Activity;

/** Host boundary tests; these do not substitute for Android task-manager testing. */
public final class GalleryExitTest {
    private static int assertions;
    private static void check(boolean condition, String reason) {
        assertions++;
        if (!condition) throw new AssertionError(reason);
    }
    private static final class FakeActivity extends Activity {
        String packageName = "org.fossify.gallery";
        boolean root;
        int rootReads, removals;
        RuntimeException packageFailure, rootFailure, removalFailure;
        @Override public String getPackageName() {
            if (packageFailure != null) throw packageFailure;
            return packageName;
        }
        @Override public boolean isTaskRoot() {
            rootReads++;
            if (rootFailure != null) throw rootFailure;
            return root;
        }
        @Override public void finishAndRemoveTask() {
            removals++;
            if (removalFailure != null) throw removalFailure;
        }
    }
    public static void main(String[] args) {
        check(!GalleryExit.tryExit(null), "null preserves native fallback");
        for (boolean root : new boolean[]{false, true}) {
            FakeActivity activity = new FakeActivity();
            activity.root = root;
            check(GalleryExit.tryExit(activity) == root, "consume only own root-task back");
            check(activity.removals == (root ? 1 : 0), "nested folder/photo/caller task is never removed");
            check(activity.rootReads == 1, "read root status once");
        }
        FakeActivity unrelated = new FakeActivity();
        unrelated.packageName = "caller.application";
        unrelated.root = true;
        check(!GalleryExit.tryExit(unrelated), "unrelated package preserves fallback");
        check(unrelated.rootReads == 0 && unrelated.removals == 0, "unrelated task untouched");
        for (int stage = 0; stage < 3; stage++) {
            FakeActivity activity = new FakeActivity();
            activity.root = true;
            RuntimeException failure = new SecurityException("host removal failure");
            if (stage == 0) activity.packageFailure = failure;
            else if (stage == 1) activity.rootFailure = failure;
            else activity.removalFailure = failure;
            check(!GalleryExit.tryExit(activity), "platform failure returns to native fallback");
            check(activity.removals == (stage == 2 ? 1 : 0), "no additional removal attempts");
        }
        System.out.println("PASS GalleryExit host assertions=" + assertions
                + "; root/nested/caller/null/failure boundaries. Android device behavior NOT tested.");
    }
}
