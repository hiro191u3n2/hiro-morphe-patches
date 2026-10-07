package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inspector.WindowInspector;
import android.window.FakeDispatcher;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Contract tests with deterministic Window/Activity task doubles; these are not device tests. */
public final class OneBackExitHostTest {
    private static final String INSTAGRAM = "com.instagram.android";
    private static final String TWITTER = "com.twitter.android";
    private static final String TRIP = "ctrip.english";
    private static int checks;
    private static int cases;

    private interface Case { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(int wanted, int actual, String message) {
        check(wanted == actual, message + ": expected " + wanted + ", got " + actual);
    }

    private static void run(String name, Case test) throws Exception {
        Build.VERSION.SDK_INT = 36;
        WindowInspector.clear();
        test.run();
        Handler.drain();
        equal(0, Handler.queuedCount(), "no permanent scan loop");
        cases++;
        System.out.println("PASS " + name);
    }

    private static final class Fixture implements AutoCloseable {
        final Application application;
        final ActivityManager manager = new ActivityManager();
        final List<Activity> activities = new ArrayList<Activity>();
        final List<Dialog> dialogs = new ArrayList<Dialog>();

        Fixture(String packageName) {
            application = new Application(packageName);
            application.setService(Context.ACTIVITY_SERVICE, manager);
        }

        Activity fresh(int id, boolean root) {
            Activity activity = new Activity(application, id, root);
            activities.add(activity);
            return activity;
        }

        Activity screen(int id, boolean root) {
            Activity activity = fresh(id, root);
            OneBackExit.install(activity);
            activity.getWindow().getDecorView();
            application.created(activity);
            activity.getWindow().attach();
            application.resumed(activity);
            activity.getWindow().getDecorView().focus(true);
            Handler.drain();
            return activity;
        }

        Activity lifecycleScreen(int id, boolean root) {
            Activity activity = fresh(id, root);
            activity.getWindow().getDecorView();
            application.created(activity);
            activity.getWindow().attach();
            application.resumed(activity);
            Handler.drain();
            return activity;
        }

        Dialog dialog(Context context) {
            Dialog dialog = new Dialog(context);
            dialogs.add(dialog);
            return dialog;
        }

        ActivityManager.AppTask task(int id, String packageName, Activity... activities) {
            ActivityManager.RecentTaskInfo info = new ActivityManager.RecentTaskInfo();
            info.taskId = id; info.id = id; info.persistentId = id;
            info.baseIntent = new Intent().setComponent(new ComponentName(packageName, packageName + ".Main"));
            info.baseActivity = new ComponentName(packageName, packageName + ".Main");
            ActivityManager.AppTask task = new ActivityManager.AppTask(info);
            task.activities.addAll(Arrays.asList(activities));
            manager.tasks.add(task);
            return task;
        }

        ActivityManager.AppTask ownTask(int id, Activity... activities) {
            return task(id, application.getPackageName(), activities);
        }

        @Override public void close() {
            for (Activity activity : activities) {
                OneBackExit.onDestroy(activity);
                application.destroyed(activity);
                if (activity.getWindow().peekDecorView() != null) activity.getWindow().peekDecorView().detach();
            }
            for (Dialog dialog : dialogs) dialog.dismiss();
            Handler.drain();
            WindowInspector.clear();
        }
    }

    private static void allowlistAndInstall() {
        for (String name : new String[] { "com.ss.android.ugc.trill", "com.zhiliaoapp.musically",
                INSTAGRAM, TWITTER, TRIP }) {
            try (Fixture f = new Fixture(name)) {
                Activity activity = f.screen(1, true);
                OneBackExit.install(activity);
                OneBackExit.onResume(activity);
                Handler.drain();
                equal(1, f.application.registrations, "one lifecycle per Application: " + name);
                equal(1, activity.getWindow().dispatcher().count(), "one callback per root: " + name);
                equal(0, f.manager.queries, "install does not remove/exclude tasks");
                equal(0, activity.removeCalls, "install does not finish Activity");
            }
        }
        try (Fixture f = new Fixture("com.example.unrelated")) {
            Activity activity = f.screen(2, true);
            check(!OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)),
                    "foreign package keys are delegated");
            OneBackExit.back(activity);
            equal(0, activity.removeCalls, "foreign package Activity preserved");
            equal(0, activity.getWindow().dispatcher().count(), "foreign callbacks untouched");
            equal(0, f.application.registrations, "foreign lifecycle untouched");
        }
    }

    private static void earlyCreate() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.fresh(10, true);
            OneBackExit.install(activity);
            equal(0, activity.getWindow().decorCreations, "onCreate entry does not force decor creation");
            activity.getWindow().getDecorView();
            f.application.created(activity);
            activity.getWindow().attach();
            f.application.resumed(activity);
            Handler.drain();
            equal(1, activity.getWindow().dispatcher().count(), "post-lifecycle binds newly created decor");
        }
    }

    private static void allTasks() {
        try (Fixture f = new Fixture(INSTAGRAM); Fixture other = new Fixture(TWITTER)) {
            Activity root = f.screen(20, true);
            Activity detail = f.lifecycleScreen(20, false);
            Activity second = f.lifecycleScreen(21, true);
            Activity separatePackage = other.screen(25, true);
            ActivityManager.AppTask taskA = f.ownTask(20, root, detail);
            ActivityManager.AppTask taskB = f.ownTask(21, second);
            ActivityManager.AppTask dormant = f.ownTask(22);
            dormant.info.baseActivity = null;
            ActivityManager.AppTask foreign = f.task(24, "com.android.chrome");
            ActivityManager.AppTask otherTask = other.ownTask(25, separatePackage);
            OneBackExit.back(detail);
            equal(1, taskA.removeCalls, "deep page removes its whole task");
            equal(1, taskB.removeCalls, "other same-package task removed");
            equal(1, dormant.removeCalls, "recent-only task without Activity removed");
            equal(0, foreign.removeCalls, "foreign task rejected");
            equal(0, foreign.excludeCalls, "foreign task not hidden");
            equal(0, otherTask.removeCalls, "another allowlisted package is not part of this exit");
            equal(0, taskA.excludeCalls, "successful removal does not need persistent exclusion");
            check(root.finishing && detail.finishing && second.finishing, "all own Activity instances finished");
        }
    }

    private static void rootAndEmbeddedFallback() {
        try (Fixture f = new Fixture(TRIP)) {
            Activity root = f.screen(30, true);
            Activity detail = f.lifecycleScreen(30, false);
            Activity embedded = f.lifecycleScreen(31, false);
            f.application.setService(Context.ACTIVITY_SERVICE, null);
            OneBackExit.back(detail);
            equal(1, root.removeCalls, "root fallback removes owned task when task service unavailable");
            equal(0, detail.finishCalls, "root removal covers detail in same task");
            equal(1, embedded.finishCalls, "embedded own Activity finishes separately");
            equal(0, embedded.removeCalls, "embedded Activity does not remove host task");
        }
    }

    private static void foreignTaskGuard() {
        try (Fixture f = new Fixture(TRIP)) {
            Activity embedded = f.screen(40, false);
            ActivityManager.AppTask host = f.task(40, "com.android.chrome");
            OneBackExit.back(embedded);
            equal(0, host.removeCalls, "host AppTask preserved");
            equal(0, host.excludeCalls, "host recents preserved");
            equal(1, embedded.finishCalls, "only target Activity closed");
        }
        try (Fixture f = new Fixture(TRIP)) {
            Activity root = f.screen(41, true);
            ActivityManager.AppTask host = f.task(41, "com.android.chrome");
            OneBackExit.back(root);
            equal(0, root.removeCalls, "explicit foreign task identity blocks root-wide fallback");
            equal(1, root.finishCalls, "root Activity still has local finish fallback");
            equal(0, host.removeCalls, "foreign task unchanged");
        }
    }

    private static void vanishedAndFailedTasks() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity first = f.screen(50, true);
            Activity second = f.lifecycleScreen(51, true);
            ActivityManager.AppTask vanished = f.ownTask(49);
            vanished.failInfo = true;
            ActivityManager.AppTask failed = f.ownTask(50, first);
            failed.failRemove = true;
            ActivityManager.AppTask good = f.ownTask(51, second);
            OneBackExit.back(first);
            equal(1, failed.removeCalls, "failing own task attempted");
            equal(1, failed.excludeCalls, "confirmed-own failed entry excluded only during exit");
            check(failed.excluded, "failed own entry does not remain visible in recents");
            equal(1, first.removeCalls, "failed task gets Activity root fallback");
            equal(1, good.removeCalls, "failures do not abort subsequent tasks");
        }
        try (Fixture f = new Fixture(TWITTER)) {
            Activity first = f.screen(52, true);
            Activity second = f.lifecycleScreen(53, true);
            f.manager.failList = true;
            first.failRemove = true;
            OneBackExit.back(first);
            equal(1, first.finishCalls, "Activity removal error falls back to Activity finish");
            equal(1, second.removeCalls, "one broken Activity does not block next fallback");
        }
    }

    private static void taskIdentity() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity root = f.screen(60, true);
            f.ownTask(60, root);
            ActivityManager.AppTask baseOnly = f.ownTask(61);
            baseOnly.info.baseIntent = null;
            ActivityManager.AppTask explicitPackage = f.ownTask(62);
            explicitPackage.info.baseIntent = new Intent().setPackage(INSTAGRAM);
            ActivityManager.AppTask conflict = f.task(63, "com.example.external");
            conflict.info.baseActivity = new ComponentName(INSTAGRAM, INSTAGRAM + ".Misleading");
            ActivityManager.AppTask unknown = f.ownTask(64);
            unknown.info.baseIntent = null; unknown.info.baseActivity = null;
            OneBackExit.back(root);
            equal(1, baseOnly.removeCalls, "API29 baseActivity fallback identifies own task");
            equal(1, explicitPackage.removeCalls, "explicit base Intent package identifies own task");
            equal(0, conflict.removeCalls, "base Intent identity wins over misleading baseActivity");
            equal(0, unknown.removeCalls, "unknown package identity is not removed");
        }
    }

    private static void legacyKeys() {
        try (Fixture f = new Fixture(TWITTER)) {
            Activity activity = f.screen(70, true);
            ActivityManager.AppTask task = f.ownTask(70, activity);
            check(!OneBackExit.key(activity, null), "null event delegated");
            check(!OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)),
                    "non-BACK key delegated");
            check(OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)), "BACK down consumed");
            check(OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)), "BACK repeat consumed");
            check(OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_BACK)), "BACK multiple consumed");
            equal(0, task.removeCalls, "down/repeat/multiple do not exit");
            check(OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, true)), "cancelled release consumed");
            equal(0, task.removeCalls, "cancelled release does not exit");
            check(OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)), "BACK up consumed");
            equal(1, task.removeCalls, "one committed release exits");
            OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            equal(1, task.removeCalls, "duplicate completion does not exit twice");
        }
    }

    private static void viewAndDialogKeys() {
        try (Fixture f = new Fixture(TRIP)) {
            Activity activity = f.screen(80, true);
            ActivityManager.AppTask task = f.ownTask(80, activity);
            View input = new View(new ContextWrapper(new ContextWrapper(activity)));
            check(OneBackExit.keyFromView(input, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)), "pre-IME down consumed");
            equal(0, task.removeCalls, "pre-IME down waits for release");
            OneBackExit.keyFromView(input, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            equal(1, task.removeCalls, "wrapped input Context resolves Activity");
        }
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.screen(81, true);
            ActivityManager.AppTask task = f.ownTask(81, activity);
            Dialog dialog = f.dialog(new ContextWrapper(activity));
            dialog.show(); OneBackExit.dialogShown(dialog);
            check(!OneBackExit.dialogKey(dialog, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)), "dialog non-BACK delegated");
            check(OneBackExit.dialogKey(dialog, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, true)), "dialog cancel consumed");
            equal(0, task.removeCalls, "dialog cancel does not exit");
            OneBackExit.dialogBack(dialog);
            equal(1, task.removeCalls, "direct Dialog.onBackPressed replacement exits");
        }
        try (Fixture f = new Fixture(TWITTER)) {
            Activity activity = f.screen(82, true);
            ActivityManager.AppTask task = f.ownTask(82, activity);
            Dialog dialog = f.dialog(activity);
            check(OneBackExit.dialogKey(dialog, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)), "dialog BACK committed");
            equal(1, task.removeCalls, "legacy dialog key closes app");
        }
    }

    private static void overlayPrecedence() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.screen(90, true);
            ActivityManager.AppTask task = f.ownTask(90, activity);
            final int[] internalBack = { 0 };
            FakeDispatcher dispatcher = activity.getWindow().dispatcher();
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> internalBack[0]++); // AndroidX/WebView/RN navigation bridge.
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> internalBack[0]++); // IME callback registered later than app navigation.
            dispatcher.invokeBack();
            equal(1, task.removeCalls, "overlay runs ahead of IME and internal navigation");
            equal(0, internalBack[0], "original back paths do not run after exit");
        }
    }

    private static void samePriorityRearm() {
        try (Fixture f = new Fixture(TRIP)) {
            Activity activity = f.screen(100, true);
            ActivityManager.AppTask task = f.ownTask(100, activity);
            final int[] competitorCalls = { 0 };
            OnBackInvokedCallback competitor = () -> competitorCalls[0]++;
            FakeDispatcher dispatcher = activity.getWindow().dispatcher();
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, competitor);
            check(dispatcher.top() == competitor, "test begins with later equal-priority competitor on top");
            f.application.resumed(activity);
            Handler.drain();
            equal(2, dispatcher.count(), "rearm retains one runtime callback and original callback");
            check(dispatcher.top() != competitor, "post-resume moves existing runtime callback to top");
            dispatcher.invokeBack();
            equal(1, task.removeCalls, "rearmed callback exits");
            equal(0, competitorCalls[0], "equal-priority app callback bypassed");
        }
    }

    private static void lifecycleCoverage() {
        try (Fixture f = new Fixture(TWITTER)) {
            Activity entry = f.screen(110, true);
            Activity later = f.lifecycleScreen(111, true);
            ActivityManager.AppTask taskA = f.ownTask(110, entry);
            ActivityManager.AppTask taskB = f.ownTask(111, later);
            equal(1, later.getWindow().dispatcher().count(), "later Activity bound through lifecycle alone");
            later.getWindow().dispatcher().invokeBack();
            equal(1, taskA.removeCalls, "later Activity exit includes older task");
            equal(1, taskB.removeCalls, "later Activity exit includes itself");
        }
    }

    private static void discoverDialog() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.screen(120, true);
            ActivityManager.AppTask task = f.ownTask(120, activity);
            Dialog dialog = f.dialog(new ContextWrapper(activity));
            dialog.show();
            // Simulate a framework/reflection show() call with no direct dialogShown hook.
            activity.getWindow().getDecorView().focus(false);
            Handler.drain();
            equal(1, dialog.getWindow().dispatcher().count(), "focus scan discovers separate Dialog window");
            dialog.getWindow().dispatcher().invokeBack();
            equal(1, task.removeCalls, "discovered Dialog closes entire app");
        }
    }

    private static void nestedDialog() {
        try (Fixture f = new Fixture(TRIP)) {
            Activity activity = f.screen(130, true);
            ActivityManager.AppTask task = f.ownTask(130, activity);
            Dialog first = f.dialog(activity);
            first.show(); OneBackExit.dialogShown(first); Handler.drain();
            Dialog second = f.dialog(f.application);
            second.show();
            first.getWindow().getDecorView().focus(false);
            Handler.drain();
            equal(1, second.getWindow().dispatcher().count(), "nested app-context Dialog resolves active owner");
            second.getWindow().dispatcher().invokeBack();
            equal(1, task.removeCalls, "nested Dialog exits on first back");
        }
    }

    private static void detachAndDestroy() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.screen(140, true);
            ActivityManager.AppTask task = f.ownTask(140, activity);
            Dialog dialog = f.dialog(activity);
            dialog.show(); OneBackExit.dialogShown(dialog); Handler.drain();
            FakeDispatcher old = dialog.getWindow().dispatcher();
            View root = dialog.getWindow().getDecorView();
            dialog.dismiss();
            equal(0, old.count(), "detached dialog unregisters callback");
            FakeDispatcher replacement = new FakeDispatcher();
            dialog.getWindow().replaceDispatcher(replacement);
            dialog.show();
            Handler.drain();
            equal(1, replacement.count(), "reattached root binds a replacement dispatcher");
            equal(1, root.attachListenerCount(), "reattach does not duplicate attach listener");
            equal(1, root.getViewTreeObserver().listenerCount(), "reattach does not duplicate focus listener");
            OnBackInvokedCallback stale = replacement.top();
            ViewTreeObserver observer = root.getViewTreeObserver();
            OneBackExit.onDestroy(activity);
            equal(0, replacement.count(), "owner destroy unregisters dialog");
            equal(0, activity.getWindow().dispatcher().count(), "owner destroy unregisters Activity");
            equal(0, root.attachListenerCount(), "owner destroy removes attach listener");
            equal(0, observer.listenerCount(), "owner destroy removes focus listener");
            stale.onBackInvoked();
            equal(0, task.removeCalls, "queued stale callback cannot exit a destroyed owner");
        }
    }

    private static void reopen() {
        try (Fixture f = new Fixture(TWITTER)) {
            Activity original = f.screen(150, true);
            ActivityManager.AppTask first = f.ownTask(150, original);
            original.getWindow().dispatcher().invokeBack();
            f.application.destroyed(original);
            f.manager.tasks.remove(first);
            Activity reopened = f.lifecycleScreen(151, true);
            ActivityManager.AppTask next = f.ownTask(151, reopened);
            equal(0, next.removeCalls, "new launch is not killed by old closing state");
            equal(1, reopened.getWindow().dispatcher().count(), "reopened Activity gets fresh callback");
            reopened.getWindow().dispatcher().invokeBack();
            equal(1, next.removeCalls, "reopened app can exit again");
        }
    }

    private static void workerDispatch() throws Exception {
        try (Fixture f = new Fixture(TRIP)) {
            final Activity activity = f.screen(160, true);
            ActivityManager.AppTask task = f.ownTask(160, activity);
            Thread worker = new Thread(() -> OneBackExit.back(activity));
            worker.start(); worker.join();
            equal(0, task.removeCalls, "worker request queues task work onto main");
            equal(1, Handler.queuedCount(), "exactly one exit action queued");
            Handler.drain();
            equal(1, task.removeCalls, "main executes queued exit");
        }
    }

    private static void reentrantExit() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity first = f.screen(170, true);
            final Activity second = f.lifecycleScreen(171, true);
            ActivityManager.AppTask taskA = f.ownTask(170, first);
            ActivityManager.AppTask taskB = f.ownTask(171, second);
            taskA.onRemove = () -> OneBackExit.back(second);
            OneBackExit.back(first);
            equal(1, taskA.removeCalls, "reentrant callback does not re-remove first task");
            equal(1, taskB.removeCalls, "reentrant callback does not double-remove later task");
        }
    }

    private static void legacyApiGuards() {
        Build.VERSION.SDK_INT = 28;
        try (Fixture f = new Fixture(TRIP)) {
            Activity activity = f.screen(180, true);
            ActivityManager.AppTask task = f.ownTask(180, activity);
            task.info.taskId = -500;
            equal(0, activity.getWindow().dispatcher().count(), "API28 does not register API33 callback");
            equal(0, WindowInspector.queries, "API28 does not call API29 WindowInspector");
            OneBackExit.key(activity, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            equal(1, task.removeCalls, "legacy task ID path still removes app task");
            equal(0, activity.removeCalls, "legacy task ID correctly prevents duplicate root removal");
        }
        Build.VERSION.SDK_INT = 32;
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.screen(181, true);
            ActivityManager.AppTask task = f.ownTask(181, activity);
            equal(0, activity.getWindow().dispatcher().count(), "API32 does not touch API33 dispatcher");
            View input = new View(activity);
            OneBackExit.keyFromView(input, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            equal(1, task.removeCalls, "legacy pre-IME entry commits exit");
        }
    }

    private static void rejectedExitCanRetry() {
        try (Fixture f = new Fixture(INSTAGRAM)) {
            Activity activity = f.screen(190, true);
            f.manager.failList = true;
            activity.failRemove = true;
            activity.failFinish = true;
            OneBackExit.back(activity);
            equal(1, activity.removeCalls, "initial failed removal attempted");
            equal(1, activity.finishCalls, "initial failed fallback attempted");
            check(!activity.finishing, "test retains a visible Activity after both API failures");
            activity.failRemove = false;
            activity.failFinish = false;
            activity.getWindow().dispatcher().invokeBack();
            equal(2, activity.removeCalls, "another back can retry after the service recovers");
            check(activity.finishing, "retry finishes the Activity");
        }
    }

    public static void main(String[] args) throws Exception {
        Looper.getMainLooper();
        run("allowlist and idempotent install", OneBackExitHostTest::allowlistAndInstall);
        run("onCreate does not force decor", OneBackExitHostTest::earlyCreate);
        run("all own tasks including dormant recents", OneBackExitHostTest::allTasks);
        run("root and embedded Activity fallbacks", OneBackExitHostTest::rootAndEmbeddedFallback);
        run("foreign task preservation", OneBackExitHostTest::foreignTaskGuard);
        run("vanished tasks and per-task failure isolation", OneBackExitHostTest::vanishedAndFailedTasks);
        run("task package identity validation", OneBackExitHostTest::taskIdentity);
        run("legacy key phases and cancellation", OneBackExitHostTest::legacyKeys);
        run("pre-IME and Dialog entry points", OneBackExitHostTest::viewAndDialogKeys);
        run("overlay ahead of IME and AndroidX", OneBackExitHostTest::overlayPrecedence);
        run("equal-priority callback rearm", OneBackExitHostTest::samePriorityRearm);
        run("lifecycle covers later Activities", OneBackExitHostTest::lifecycleCoverage);
        run("unhooked Dialog focus discovery", OneBackExitHostTest::discoverDialog);
        run("nested app-context Dialog", OneBackExitHostTest::nestedDialog);
        run("detach, reattach, destroy and stale callback", OneBackExitHostTest::detachAndDestroy);
        run("relaunch after exit in same process", OneBackExitHostTest::reopen);
        run("worker request runs on main", OneBackExitHostTest::workerDispatch);
        run("reentrant exit is idempotent", OneBackExitHostTest::reentrantExit);
        run("API28 and API32 guards", OneBackExitHostTest::legacyApiGuards);
        run("fully rejected exit can retry", OneBackExitHostTest::rejectedExitCanRetry);
        System.out.println("SUCCESS: " + cases + " cases, " + checks + " assertions");
    }
}
