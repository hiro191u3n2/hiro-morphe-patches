package app.hiro.berry.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Host logic tests: Android framework and real-device queue activation are not simulated. */
public final class BerryQueueRuntimeTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) { throw new AssertionError(message); }
    }

    private static void reset(int sdk) {
        Build.VERSION.SDK_INT = sdk;
        Handler.queue.clear();
        Handler.rejectPosts = false;
        Looper.MAIN.lines.clear();
        Looper.MAIN.lines.add("Looper main");
        Looper.MAIN.lines.add(" (MessageQueue is using DeliQueue implementation)");
        Looper.MAIN.lines.add(" Message 0: obj=https://private.example/?token=PRIVATE_TOKEN");
        Looper.MAIN.dumpCalls = 0;
        Looper.MAIN.throwOnDump = false;
        Log.lines.clear();
        Log.throwOnLog = false;
    }

    private static BerryQueueInit install(Context context) {
        BerryQueueInit provider = new BerryQueueInit();
        provider.attachTestContext(context);
        check(provider.onCreate(), "Provider startup must succeed");
        return provider;
    }

    private static void assertNoPrivateLog() {
        for (String line : Log.lines) {
            check(!line.contains("PRIVATE_TOKEN") && !line.contains("private.example")
                    && !line.contains("private dump") && !line.contains("private request"),
                    "Queued data or exception data leaked into logs");
        }
    }

    private static void olderAndroidIsUntouched() {
        reset(36);
        Application app = new Application();
        install(app);
        Activity activity = new Activity(app);
        app.resume(activity);
        Handler.drain();
        check(app.callbacks.isEmpty(), "Android 16 must not register callbacks");
        check(app.preferencesReads == 0 && app.preferences.writes == 0,
                "Android 16 must not touch preference state");
        check(activity.requests == 0 && Looper.MAIN.dumpCalls == 0 && Log.lines.isEmpty(),
                "Android 16 must not request or diagnose");
    }

    private static void permissionIsDeferredAndNeverRepeatedAfterDenial() {
        reset(37);
        Application app = new Application();
        install(app);
        Activity activity = new Activity(app);
        app.resume(activity);
        app.resume(activity);
        check(activity.requests == 0, "No permission request inside resume callback");
        check(app.preferences.writes == 0, "No attempt flag before actual request");
        check(Handler.queue.size() == 1, "Duplicate resumes must coalesce");
        Handler.drain();
        check(activity.requests == 1, "One permission attempt expected");
        check(activity.requestedPermissions.length == 1
                        && "android.permission.ACCESS_LOCAL_NETWORK".equals(activity.requestedPermissions[0]),
                "Only the required local-network permission may be requested");
        check(activity.requestCode > 0 && activity.requestCode <= 65535, "Valid request code");
        check(activity.writesAtRequest == 1, "Attempt must be recorded before showing dialog");
        check(app.callbacks.isEmpty(), "Callbacks must detach after request");
        app.resume(activity);
        Handler.drain();
        check(activity.requests == 1 && Looper.MAIN.dumpCalls == 1,
                "Denial must not lead to repeat work");
        assertNoPrivateLog();

        // A new process uses the persisted state left by the denied request.
        Application restarted = new Application(app.preferences);
        install(restarted);
        Activity restartedActivity = new Activity(restarted);
        restarted.resume(restartedActivity);
        Handler.drain();
        check(restartedActivity.requests == 0 && restarted.preferences.writes == 1,
                "A restart must respect a previous attempt/denial");
        check(restarted.callbacks.isEmpty(), "No permanent lifecycle work after previous attempt");
    }

    private static void existingGrantDoesNotAskOrMarkAttempt() {
        reset(37);
        Application app = new Application();
        app.permission = PackageManager.PERMISSION_GRANTED;
        install(app);
        Activity activity = new Activity(app);
        app.resume(activity);
        Handler.drain();
        check(activity.requests == 0 && app.preferences.writes == 0,
                "Already granted permission must not be requested or marked attempted");
        check(app.callbacks.isEmpty() && Looper.MAIN.dumpCalls == 1,
                "Grant still permits the one queue diagnostic");
    }

    private static void unavailableActivityDoesNotConsumeAttempt() {
        reset(37);
        Application app = new Application();
        install(app);
        Activity stale = new Activity(app);
        app.resume(stale);
        app.pause(stale);
        app.destroy(stale);
        Handler.drain();
        check(stale.requests == 0 && app.preferences.writes == 0 && Looper.MAIN.dumpCalls == 0,
                "Destroyed/paused activity must not consume or diagnose an attempt");
        Activity active = new Activity(app);
        app.resume(active);
        Handler.drain();
        check(active.requests == 1 && app.preferences.writes == 1,
                "Next valid activity must still receive the request");
    }

    private static void newestResumedActivityReceivesQueuedRequest() {
        reset(37);
        Application app = new Application();
        install(app);
        Activity old = new Activity(app);
        Activity next = new Activity(app);
        app.resume(old);
        app.pause(old);
        app.resume(next);
        app.destroy(old);
        Handler.drain();
        check(old.requests == 0 && next.requests == 1,
                "A pending post must use the current resumed Activity");
    }

    private static void finishingAndForeignActivitiesAreIgnored() {
        reset(37);
        Application app = new Application();
        install(app);
        Activity finishing = new Activity(app);
        finishing.finishing = true;
        Activity foreign = new Activity(new Application());
        app.resume(finishing);
        app.resume(foreign);
        Handler.drain();
        check(finishing.requests == 0 && foreign.requests == 0 && app.preferences.writes == 0,
                "Transient/foreign activities must not consume attempt");
        Activity becomesFinishing = new Activity(app);
        app.resume(becomesFinishing);
        becomesFinishing.finishing = true;
        Handler.drain();
        check(becomesFinishing.requests == 0 && app.preferences.writes == 0,
                "Readiness must be rechecked after posting");
    }

    private static void failingRequestDoesNotCrashOrRepeat() {
        reset(37);
        Application app = new Application();
        install(app);
        Activity activity = new Activity(app);
        activity.throwOnRequest = true;
        app.throwOnUnregister = true;
        app.resume(activity);
        Handler.drain();
        app.resume(activity);
        Handler.drain();
        check(activity.requests == 1 && app.preferences.writes == 1,
                "Request exception and unregister failure must not cause a retry loop");
        check(Looper.MAIN.dumpCalls == 1, "Failing request must not repeat queue diagnostic");
        assertNoPrivateLog();
    }

    private static void missingPersistenceNeverPrompts() {
        for (boolean readFailure : new boolean[] {true, false}) {
            reset(37);
            Application app = new Application();
            app.throwOnPreferences = readFailure;
            app.preferences.throwOnApply = !readFailure;
            install(app);
            Activity activity = new Activity(app);
            app.resume(activity);
            Handler.drain();
            check(activity.requests == 0 && app.preferences.writes == 0,
                    "No unrecorded request if preference read/write fails");
            check(app.callbacks.isEmpty(), "Failed persistence must not leave repeated callbacks");
        }
    }

    private static void queueClassificationUsesOnlyExactPublicMarkers() {
        String[][] samples = {
            {" (MessageQueue is using DeliQueue implementation)", "DeliQueue (lock-free)"},
            {" (MessageQueue is using Legacy implementation)", "Legacy"},
            {" Message 0: obj=(MessageQueue is using DeliQueue implementation)", "unknown"},
            {" DeliQueue custom unknown marker", "unknown"}
        };
        for (String[] sample : samples) {
            reset(37);
            Looper.MAIN.lines.clear();
            Looper.MAIN.lines.add(sample[0]);
            Looper.MAIN.lines.add("PRIVATE_TOKEN https://private.example/");
            Application app = new Application();
            app.permission = PackageManager.PERMISSION_GRANTED;
            install(app);
            app.resume(new Activity(app));
            Handler.drain();
            check(Log.lines.size() == 1 && Log.lines.get(0).contains(sample[1]),
                    "Only an exact implementation marker may select the log classification");
            assertNoPrivateLog();
        }
        reset(37);
        Looper.MAIN.throwOnDump = true;
        Application app = new Application();
        install(app);
        Activity activity = new Activity(app);
        app.resume(activity);
        Handler.drain();
        check(Log.lines.size() == 1 && Log.lines.get(0).contains("unknown"),
                "A failed dump must not be mistaken for proof of activation");
        check(activity.requests == 1, "A failed optional dump must not break permission flow");
        assertNoPrivateLog();
    }

    private static void optionalInfrastructureFailureIsBenign() {
        reset(37);
        install(null);
        install(new Context() {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                throw new AssertionError("Non-Application context must not access preferences");
            }
        });
        Application failedRegistration = new Application();
        failedRegistration.throwOnRegister = true;
        install(failedRegistration);
        check(failedRegistration.callbacks.isEmpty(), "Registration failure must be harmless");

        Application app = new Application();
        install(app);
        Activity activity = new Activity(app);
        Handler.rejectPosts = true;
        app.resume(activity);
        check(activity.requests == 0 && app.preferences.writes == 0, "Rejected post consumes no attempt");
        Handler.rejectPosts = false;
        Log.throwOnLog = true;
        app.resume(activity);
        Handler.drain();
        check(activity.requests == 1, "Optional log failure must not crash or prevent request");
    }

    public static void main(String[] args) {
        olderAndroidIsUntouched();
        permissionIsDeferredAndNeverRepeatedAfterDenial();
        existingGrantDoesNotAskOrMarkAttempt();
        unavailableActivityDoesNotConsumeAttempt();
        newestResumedActivityReceivesQueuedRequest();
        finishingAndForeignActivitiesAreIgnored();
        failingRequestDoesNotCrashOrRepeat();
        missingPersistenceNeverPrompts();
        queueClassificationUsesOnlyExactPublicMarkers();
        optionalInfrastructureFailureIsBenign();
        System.out.println("PASS Berry runtime host behavior: " + checks + " checks");
    }
}
