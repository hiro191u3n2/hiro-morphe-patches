import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import app.hiro.quicksearch.runtime.SearchTask;
import java.util.Objects;

/** Host behavior checks, not Android task-manager or device tests. */
public final class SearchTaskTest {
    private static int assertions;
    private static void same(Object expected, Object actual, String reason) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(reason + ": expected " + expected + ", got " + actual);
        }
        assertions++;
    }

    private static final class FakePreferences implements SharedPreferences {
        Boolean exit;
        int reads;
        String key;
        boolean defaultValue;
        @Override public boolean getBoolean(String key, boolean defaultValue) {
            reads++;
            this.key = key;
            this.defaultValue = defaultValue;
            return exit == null ? defaultValue : exit.booleanValue();
        }
    }

    private static final class FakeActivity extends Activity {
        final FakePreferences preferences = new FakePreferences();
        String packageName = "jp.ddo.sugihiro.quicksearch";
        boolean root;
        int rootReads, finishes, removals, starts, preferenceReads;
        String preferenceName;
        int preferenceMode;
        Intent startedIntent;
        int startedFlags;
        RuntimeException launchFailure;

        @Override public String getPackageName() { return packageName; }
        @Override public SharedPreferences getSharedPreferences(String name, int mode) {
            preferenceReads++;
            preferenceName = name;
            preferenceMode = mode;
            return preferences;
        }
        @Override public boolean isTaskRoot() { rootReads++; return root; }
        @Override public void finish() { finishes++; }
        @Override public void finishAndRemoveTask() { removals++; }
        @Override public void startActivity(Intent intent) {
            starts++;
            startedIntent = intent;
            startedFlags = intent.getFlags();
            if (launchFailure != null) throw launchFailure;
        }
    }

    private static void external(Boolean exit, int existingFlags) {
        FakeActivity a = new FakeActivity();
        a.preferences.exit = exit;
        a.packageName = "fixture.current.package";
        Intent intent = new Intent("android.intent.action.VIEW").setFlags(existingFlags)
                .putExtra("unchanged-query", "日本語 test & Pokémon");
        SearchTask.startExternal(a, intent);
        int expectedFlags = Boolean.TRUE.equals(exit)
                ? existingFlags | Intent.FLAG_ACTIVITY_NEW_TASK : existingFlags;
        same(1, a.starts, "launch exactly once");
        same(intent, a.startedIntent, "original intent identity");
        same(expectedFlags, a.startedFlags, "flags at launch");
        same(expectedFlags, intent.getFlags(), "only NEW_TASK may be added");
        same("android.intent.action.VIEW", intent.getAction(), "action preserved");
        same("日本語 test & Pokémon", intent.getStringExtra("unchanged-query"), "payload preserved");
        same("fixture.current.package_preferences", a.preferenceName, "preference file follows context");
        same(0, a.preferenceMode, "private preference file");
        same(1, a.preferenceReads, "one preference open");
        same(1, a.preferences.reads, "one exit read");
        same("exit", a.preferences.key, "original exit key");
        same(false, a.preferences.defaultValue, "original exit default");
        same(0, a.finishes + a.removals, "launch does not finish caller prematurely");
    }

    public static void main(String[] args) {
        // Includes absent preference and an already present NEW_TASK bit.
        for (Boolean exit : new Boolean[]{null, false, true}) {
            for (int flags : new int[]{0, 0x08000001, 0x14000000}) external(exit, flags);
        }
        for (boolean root : new boolean[]{false, true}) {
            FakeActivity a = new FakeActivity();
            a.root = root;
            SearchTask.finish(a);
            same(root ? 1 : 0, a.removals, "remove only own root task");
            same(root ? 0 : 1, a.finishes, "preserve enclosing task when non-root");
            same(1, a.rootReads, "root state read once");
            same(0, a.starts + a.preferenceReads, "finish has no launch or preference side effects");
        }
        FakeActivity failing = new FakeActivity();
        failing.preferences.exit = true;
        failing.launchFailure = new IllegalStateException("fixture: no activity handles search");
        RuntimeException observed = null;
        try { SearchTask.startExternal(failing, new Intent()); }
        catch (RuntimeException ex) { observed = ex; }
        same(failing.launchFailure, observed, "original launch exception reaches existing app catch block");
        same(1, failing.starts, "failed launch is not retried");
        same(0, failing.finishes + failing.removals, "failure does not independently finish caller");
        System.out.println("PASS SearchTask host assertions=" + assertions
                + "; exit ON/OFF/absent, flags, root/non-root, exception propagation. Android device behavior NOT tested.");
    }
}
