package app.hiro.mapsstartup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import com.google.android.maps.MapsActivity;
import java.util.ArrayList;
import java.util.List;

/** Runs production lifecycle coordination. Native branch execution has a separate DEX test. */
public final class StartupLocationHostTest {
    private static final String MAIN = "com.google.android.maps.MapsActivity";
    private static final String PACKAGE = "org.ungoogled.android.apps.maps.microg";
    private static int scenarios;
    private static int assertions;
    private static final List<String> PASSED = new ArrayList<String>();
    private StartupLocationHostTest() { }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static void equal(int expected, int actual, String message) {
        check(expected == actual, message + ": expected " + expected + ", got " + actual);
    }
    private static void scenario(String name, Runnable body) {
        Handler.reset(); Activity.INSTANCES.clear(); View.INSTANCES.clear();
        try {
            body.run();
            for (Activity a : Activity.INSTANCES) {
                equal(0, a.systemServiceRequests, "No independent system/location service access");
                equal(0, a.permissionRequests, "No automatic permission requests");
                equal(0, a.viewLookupCalls, "Native readiness is not gated by an optional button view");
            }
            for (View view : View.INSTANCES) equal(0, view.syntheticClickCalls, "No synthetic clicks");
            scenarios++; PASSED.add(name);
        } catch (Throwable failure) {
            throw new AssertionError("Host scenario failed: " + name, failure);
        } finally {
            for (Activity a : Activity.INSTANCES) StartupLocation.onDestroy(a);
            Handler.reset(); Activity.INSTANCES.clear(); View.INSTANCES.clear();
        }
    }
    private static Intent launcher() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(new ComponentName(PACKAGE, MAIN));
    }
    private static Intent external() { return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("geo:0,0?q=example")); }
    private static MapsActivity create(Intent intent) {
        MapsActivity a = new MapsActivity(); a.setIntent(intent); StartupLocation.onCreate(a, null); return a;
    }
    private static void foreground(MapsActivity a) { StartupLocation.onStart(a); StartupLocation.onResume(a); }
    private static MapsActivity launch(Intent intent) { MapsActivity a = create(intent); foreground(a); return a; }
    private static void background(MapsActivity a) { StartupLocation.onPause(a); StartupLocation.onStop(a); }
    private static void finishWithFix(MapsActivity a) { a.nativeFixReady = true; Handler.advanceBy(2500L); }
    private static void assertComplete(MapsActivity a, int count) {
        equal(count, a.recenterRequests, "Expected native command count");
        equal(0, Handler.queuedCallbacks(), "Confirmed opening has no pending work");
    }

    public static void main(String[] args) {
        scenario("fresh opening settles one second and dispatch requires two acknowledgements", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true;
            Handler.runUntil(999L); equal(0, a.bridgeCalls, "No early native query or command");
            Handler.runUntil(1000L); equal(1, a.recenterRequests, "One command after settle");
            check(Handler.queuedCallbacks() > 0, "Dispatch is not completion");
            Handler.runUntil(1250L); check(Handler.queuedCallbacks() > 0, "One confirmation is insufficient");
            Handler.runUntil(1500L); assertComplete(a, 1);
            equal(2, a.readOnlyQueries, "Both acknowledgement polls are read-only");
        });
        scenario("startup settle starts after first resume", () -> {
            MapsActivity a = create(launcher()); a.nativeFixReady = true;
            Handler.advanceBy(5000L); foreground(a);
            Handler.advanceBy(999L); equal(0, a.bridgeCalls, "Slow creation does not consume resume settle");
            Handler.advanceBy(1001L); assertComplete(a, 1);
        });
        scenario("native NONE then MAP remains pending", () -> {
            MapsActivity a = launch(launcher()); a.nativeModeReady = false; a.nativeFixReady = true;
            Handler.advanceBy(2000L); equal(0, a.recenterRequests, "Transient mode does not issue");
            check(Handler.queuedCallbacks() > 0, "Transient mode is not a terminal exclusion");
            a.nativeModeReady = true; Handler.advanceBy(2000L); assertComplete(a, 1);
        });
        scenario("late controller graph and fix are awaited", () -> {
            MapsActivity a = launch(launcher()); a.nativeGraphReady = false;
            Handler.advanceBy(2000L); equal(0, a.recenterRequests, "No command without graph");
            a.nativeGraphReady = true; Handler.advanceBy(2000L); equal(0, a.recenterRequests, "No command without fix");
            finishWithFix(a); assertComplete(a, 1);
        });
        scenario("tracking mode alone is not camera acknowledgement", () -> {
            MapsActivity a = launch(launcher()); a.nativeTracking = true; a.cameraAcknowledged = false;
            finishWithFix(a); assertComplete(a, 1);
            check(a.cameraAcknowledged, "Far camera is corrected even when tracking was set");
        });
        scenario("already centered gets stable read-only confirmation", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.cameraAcknowledged = true;
            Handler.runUntil(1000L); equal(0, a.recenterRequests, "No redundant command");
            check(Handler.queuedCallbacks() > 0, "First confirmation remains pending");
            Handler.runUntil(1250L); assertComplete(a, 0);
            equal(1, a.readOnlyQueries, "Second confirmation cannot issue");
        });
        scenario("single transient confirmation cannot hide camera restoration", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.cameraAcknowledged = true;
            Handler.runUntil(1000L); a.cameraAcknowledged = false;
            Handler.runUntil(1250L); equal(0, a.recenterRequests, "Read-only check sees restored camera");
            Handler.runUntil(2250L); assertComplete(a, 1);
        });
        scenario("unacknowledged dispatch permits one recovery after 1500ms", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); equal(1, a.recenterRequests, "First command issued");
            Handler.runUntil(2499L); equal(1, a.recenterRequests, "No duplicate before acknowledgement interval");
            check(Handler.queuedCallbacks() > 0, "Unconfirmed dispatch stays pending");
            a.autoAcknowledge = true;
            Handler.runUntil(2500L); equal(2, a.recenterRequests, "One bounded recovery command");
            check(a.requestTimes.get(1).longValue() - a.requestTimes.get(0).longValue() >= 1500L, "Recovery spacing");
            Handler.runUntil(3000L); assertComplete(a, 2);
            Handler.advanceBy(60000L); equal(2, a.recenterRequests, "No commands after confirmed recovery");
        });
        scenario("no camera acknowledgement cannot cause unlimited commands", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(10000L); equal(2, a.recenterRequests, "Only initial plus one recovery command");
            check(Handler.queuedCallbacks() > 0, "Actual camera acknowledgement remains pending");
            Handler.runUntil(60000L); equal(2, a.recenterRequests, "No third command");
            equal(0, Handler.queuedCallbacks(), "Unacknowledged opening stops at deadline");
        });
        scenario("touch cancels recovery after command issue", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_DOWN));
            Handler.advanceBy(60000L); equal(1, a.recenterRequests, "No recovery after user interaction");
            equal(0, Handler.queuedCallbacks(), "Touch removes confirmation work");
        });
        scenario("pause suspends recovery and resume requires fresh readiness", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); StartupLocation.onPause(a); int calls = a.bridgeCalls;
            Handler.advanceBy(5000L); equal(calls, a.bridgeCalls, "No native action while paused");
            StartupLocation.onResume(a);
            Handler.advanceBy(999L); equal(calls, a.bridgeCalls, "Pending command settles after resume");
            Handler.advanceBy(1500L); equal(1, a.recenterRequests, "Paused time does not spend recovery delay");
            a.autoAcknowledge = true; Handler.advanceBy(1501L); assertComplete(a, 2);
        });
        scenario("focus loss suppresses recovery until focus is stable", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); a.windowFocus = false;
            Handler.advanceBy(5000L); equal(1, a.recenterRequests, "No command behind another window");
            a.windowFocus = true; a.autoAcknowledge = true;
            Handler.advanceBy(1000L); equal(1, a.recenterRequests, "No immediate recovery on focus return");
            Handler.advanceBy(1500L); assertComplete(a, 2);
        });
        scenario("pause and resume do not rearm completed opening", () -> {
            MapsActivity a = launch(launcher()); finishWithFix(a); int calls = a.bridgeCalls;
            a.cameraAcknowledged = false; a.nativeTracking = false;
            StartupLocation.onPause(a); StartupLocation.onResume(a); Handler.advanceBy(5000L);
            equal(1, a.recenterRequests, "Transient lifecycle cannot move map again");
            equal(calls, a.bridgeCalls, "Completed session is not queried again");
        });
        scenario("pre-fix pause and resume can still complete", () -> {
            MapsActivity a = launch(launcher()); Handler.advanceBy(1500L);
            StartupLocation.onPause(a); int calls = a.bridgeCalls; a.nativeFixReady = true;
            Handler.advanceBy(3000L); equal(calls, a.bridgeCalls, "No callbacks while paused");
            StartupLocation.onResume(a); Handler.advanceBy(2000L); assertComplete(a, 1);
        });
        scenario("full stop and start create a fresh opening", () -> {
            MapsActivity a = launch(launcher()); finishWithFix(a); a.cameraAcknowledged = false;
            background(a); Handler.advanceBy(70000L); foreground(a);
            Handler.advanceBy(999L); equal(1, a.recenterRequests, "Fresh opening settles again");
            Handler.advanceBy(1501L); assertComplete(a, 2);
        });
        scenario("warm MAIN rearms once without duplicate resume request", () -> {
            MapsActivity a = launch(launcher()); finishWithFix(a); a.cameraAcknowledged = false;
            StartupLocation.onNewIntent(a, launcher()); Handler.advanceBy(2500L); assertComplete(a, 2);
            StartupLocation.onResume(a); Handler.advanceBy(2500L); assertComplete(a, 2);
        });
        scenario("warm MAIN from external entry waits for resume", () -> {
            MapsActivity a = launch(external()); StartupLocation.onPause(a);
            StartupLocation.onNewIntent(a, launcher()); a.nativeFixReady = true;
            Handler.advanceBy(2000L); equal(0, a.bridgeCalls, "No action while paused");
            StartupLocation.onResume(a); Handler.advanceBy(2500L); assertComplete(a, 1);
        });
        scenario("rapid MAIN intents retain only current opening", () -> {
            MapsActivity a = launch(launcher()); StartupLocation.onNewIntent(a, launcher());
            StartupLocation.onNewIntent(a, launcher()); finishWithFix(a); assertComplete(a, 1);
        });
        Intent selector = launcher(); selector.setSelector(new Intent(Intent.ACTION_VIEW));
        final Intent[] excluded = {external(), launcher().setData(Uri.parse("geo:0,0?q=example")), selector,
            launcher().setComponent(new ComponentName(PACKAGE, "com.google.android.maps.PlacesActivity")),
            launcher().setType("application/example"), new Intent(Intent.ACTION_MAIN),
            new Intent().addCategory(Intent.CATEGORY_LAUNCHER), null};
        final String[] names = {"VIEW", "data", "selector", "alias", "MIME", "no launcher category", "no action", "null"};
        for (int index = 0; index < excluded.length; index++) {
            final Intent intent = excluded[index]; final String name = names[index];
            scenario("excluded cold entry stays excluded: " + name, () -> {
                MapsActivity a = launch(intent); finishWithFix(a); background(a); foreground(a); Handler.advanceBy(2500L);
                equal(0, a.bridgeCalls, "Destination entry cannot call recenter bridge"); assertComplete(a, 0);
            });
            scenario("new destination cancels pending recovery: " + name, () -> {
                MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
                Handler.runUntil(1000L); StartupLocation.onNewIntent(a, intent); int calls = a.bridgeCalls;
                Handler.advanceBy(3000L); background(a); foreground(a); Handler.advanceBy(3000L);
                equal(calls, a.bridgeCalls, "New destination cancels old confirmation/recovery"); assertComplete(a, 1);
            });
        }
        scenario("implicit normal launcher remains accepted", () -> {
            MapsActivity a = launch(launcher().setComponent(null)); finishWithFix(a); assertComplete(a, 1);
        });
        scenario("touch before fix cancels until a fresh explicit opening", () -> {
            MapsActivity a = launch(launcher()); StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_DOWN));
            finishWithFix(a); equal(0, a.recenterRequests, "No movement after manual interaction");
            StartupLocation.onNewIntent(a, launcher()); Handler.advanceBy(2500L); assertComplete(a, 1);
        });
        scenario("null and non-down motion do not spuriously cancel", () -> {
            MapsActivity a = launch(launcher()); StartupLocation.onTouch(a, null);
            StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_MOVE)); StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_UP));
            finishWithFix(a); assertComplete(a, 1);
        });
        scenario("actual navigation is a terminal exclusion", () -> {
            MapsActivity a = launch(launcher()); a.navigationMode = true; a.nativeFixReady = true;
            Handler.advanceBy(2000L); equal(1, a.bridgeCalls, "Route exclusion is terminal");
            a.navigationMode = false; StartupLocation.onPause(a); StartupLocation.onResume(a);
            Handler.advanceBy(3000L); assertComplete(a, 0);
        });
        for (final String state : new String[] {"missing", "hidden", "disabled", "unattached", "lookup throws"}) {
            scenario("optional button cannot gate native readiness: " + state, () -> {
                MapsActivity a = launch(launcher());
                if ("missing".equals(state)) a.putView(MapsActivity.MY_LOCATION_BUTTON, null);
                if ("hidden".equals(state)) a.locationButton.shown = false;
                if ("disabled".equals(state)) a.locationButton.enabled = false;
                if ("unattached".equals(state)) a.locationButton.windowToken = null;
                if ("lookup throws".equals(state)) a.throwingMethod = "findViewById";
                finishWithFix(a); assertComplete(a, 1);
            });
        }
        scenario("initial window focus waits without consuming startup", () -> {
            MapsActivity a = launch(launcher()); a.windowFocus = false; a.nativeFixReady = true;
            Handler.advanceBy(4000L); equal(0, a.bridgeCalls, "No native action without focus");
            a.windowFocus = true; Handler.advanceBy(2000L); assertComplete(a, 1);
        });
        scenario("fix at sixty-second deadline cannot jump map", () -> {
            MapsActivity a = launch(launcher()); Handler.runUntil(59999L); a.nativeFixReady = true;
            Handler.runUntil(61000L); assertComplete(a, 0);
            StartupLocation.onPause(a); StartupLocation.onResume(a); Handler.advanceBy(2000L); assertComplete(a, 0);
        });
        scenario("long initial pause cannot extend total deadline", () -> {
            MapsActivity a = create(launcher()); Handler.advanceBy(61000L); a.nativeFixReady = true;
            foreground(a); Handler.advanceBy(2000L); assertComplete(a, 0);
        });
        scenario("destroy removes confirmation and recovery", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); StartupLocation.onDestroy(a);
            StartupLocation.onResume(a); StartupLocation.onNewIntent(a, launcher()); Handler.advanceBy(61000L); assertComplete(a, 1);
        });
        scenario("finishing and destroyed activities cannot issue", () -> {
            MapsActivity a = launch(launcher()); MapsActivity b = launch(launcher());
            a.finishing = true; b.destroyed = true; a.nativeFixReady = true; b.nativeFixReady = true;
            Handler.advanceBy(2000L); equal(0, a.bridgeCalls, "Finishing ignored"); equal(0, b.bridgeCalls, "Destroyed ignored");
            equal(0, Handler.queuedCallbacks(), "No dead activity work");
        });
        scenario("recreation supersedes old callbacks", () -> {
            MapsActivity a = launch(launcher()); a.setIntent(external()); StartupLocation.onCreate(a, new Bundle());
            foreground(a); finishWithFix(a); assertComplete(a, 0);
        });
        scenario("activity sessions remain independent", () -> {
            MapsActivity a = launch(launcher()); MapsActivity b = launch(launcher());
            StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_DOWN)); a.nativeFixReady = true; b.nativeFixReady = true;
            Handler.advanceBy(2500L); equal(0, a.recenterRequests, "Cancellation stays local"); equal(1, b.recenterRequests, "Other session succeeds");
        });
        scenario("unsupported and null lifecycle inputs are harmless", () -> {
            Activity a = new Activity(); a.setIntent(launcher()); StartupLocation.onCreate(a, null);
            StartupLocation.onStart(a); StartupLocation.onResume(a); StartupLocation.onNewIntent(a, launcher());
            StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_DOWN)); StartupLocation.onPause(a);
            StartupLocation.onStop(a); StartupLocation.onDestroy(a); StartupLocation.onCreate(null, null);
            StartupLocation.onStart(null); StartupLocation.onResume(null); StartupLocation.onNewIntent(null, launcher());
            StartupLocation.onTouch(null, null); StartupLocation.onPause(null); StartupLocation.onStop(null); StartupLocation.onDestroy(null);
            Handler.advanceBy(2500L); equal(0, Handler.queuedCallbacks(), "No unsupported work");
        });
        for (final String fault : new String[] {"isFinishing", "isDestroyed", "hasWindowFocus"}) {
            scenario("activity failure cannot escape: " + fault, () -> {
                MapsActivity a = launch(launcher()); a.throwingMethod = fault; a.nativeFixReady = true;
                Handler.advanceBy(2000L); a.throwingMethod = null; StartupLocation.onPause(a); StartupLocation.onResume(a);
                Handler.advanceBy(2000L); assertComplete(a, 0);
            });
        }
        for (final Throwable fault : new Throwable[] {new IllegalStateException("native failure"), new LinkageError("linkage failure")}) {
            scenario("native exception remains terminal and safe: " + fault.getClass().getSimpleName(), () -> {
                MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.bridgeFailure = fault;
                Handler.advanceBy(2000L); equal(1, a.bridgeCalls, "Faulting native bridge is not looped");
                a.bridgeFailure = null; StartupLocation.onPause(a); StartupLocation.onResume(a); Handler.advanceBy(2000L); assertComplete(a, 0);
            });
        }
        scenario("malformed intent cannot preserve old pending request", () -> {
            Intent broken = new Intent(Intent.ACTION_MAIN) { @Override public String getAction() { throw new IllegalArgumentException("intent failure"); } };
            MapsActivity a = launch(broken); MapsActivity b = launch(launcher()); StartupLocation.onNewIntent(b, broken);
            a.nativeFixReady = true; b.nativeFixReady = true; Handler.advanceBy(2500L);
            equal(0, a.bridgeCalls, "Malformed initial intent does not arm"); equal(0, b.bridgeCalls, "Replacement cancels old request");
            equal(0, Handler.queuedCallbacks(), "No malformed work");
        });
        scenario("scheduler and touch decoding failures cannot escape", () -> {
            MapsActivity a = create(launcher()); StartupLocation.onStart(a);
            Handler.nextPostFailure = new IllegalStateException("scheduler failure"); StartupLocation.onResume(a);
            Handler.advanceBy(2000L); equal(0, a.bridgeCalls, "Failed scheduler did not invoke native action");
            MotionEvent broken = new MotionEvent(MotionEvent.ACTION_DOWN) { @Override public int getActionMasked() { throw new IllegalStateException("touch failure"); } };
            StartupLocation.onTouch(a, broken); StartupLocation.onDestroy(a); equal(0, Handler.queuedCallbacks(), "Safe cleanup");
        });
        System.out.println("PASS StartupLocation host lifecycle: scenarios=" + scenarios + " assertions=" + assertions
            + "; dispatch/acknowledgement separated, bounded recovery verified; Android UI execution not performed.");
        if (args.length > 0 && "--list".equals(args[0])) for (String name : PASSED) System.out.println("PASS " + name);
    }
}
