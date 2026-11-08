package app.hiro.mapsstartup;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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
    private static MapsActivity create(Intent intent) { return create(intent, null); }
    private static MapsActivity create(Intent intent, Bundle savedState) {
        MapsActivity a = new MapsActivity(); a.setIntent(intent); StartupLocation.onCreate(a, savedState);
        // The supplied APK's Lnce.onCreate(null) invokes onNewIntent(getIntent())
        // virtually at 0xA52, dispatching to Lnce before onStart/onResume.
        // MapsStartupColdLaunchAudit binds this order to the pinned native body.
        if (savedState == null) StartupLocation.onNewIntent(a, a.getIntent());
        return a;
    }
    private static void foreground(MapsActivity a) { StartupLocation.onStart(a); StartupLocation.onResume(a); }
    private static MapsActivity launch(Intent intent) { MapsActivity a = create(intent); foreground(a); return a; }
    private static void background(MapsActivity a) { StartupLocation.onPause(a); StartupLocation.onStop(a); }
    private static void tap(Activity a) {
        StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_DOWN));
        StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_UP));
    }
    private static void finishWithFix(MapsActivity a) { a.nativeFixReady = true; Handler.advanceBy(2500L); }
    private static void assertComplete(MapsActivity a, int count) {
        equal(count, a.recenterRequests, "Expected native command count");
        equal(0, Handler.queuedCallbacks(), "Confirmed opening has no pending work");
    }

    public static void main(String[] args) {
        scenario("native cold-launch replay settles one second and dispatch requires two acknowledgements", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true;
            Handler.runUntil(999L); equal(0, a.bridgeCalls, "No early native query or command");
            Handler.runUntil(1000L); equal(1, a.recenterRequests, "One command after settle");
            check(Handler.queuedCallbacks() > 0, "Dispatch is not completion");
            Handler.runUntil(1250L); check(Handler.queuedCallbacks() > 0, "One confirmation is insufficient");
            Handler.runUntil(1500L); assertComplete(a, 1);
            equal(2, a.readOnlyQueries, "Both acknowledgement polls are read-only");
        });
        scenario("native cold-launch repeated initial MAIN keeps one bounded request session", () -> {
            MapsActivity a = create(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            for (int repeat = 0; repeat < 4; repeat++) {
                Handler.advanceBy(250L); StartupLocation.onNewIntent(a, launcher()); tap(a);
                equal(0, a.bridgeCalls, "Initial intent replay must not read or move before first resume");
                equal(0, Handler.queuedCallbacks(), "Initial intent replay cannot schedule early work");
            }
            foreground(a);
            Handler.runUntil(1999L); equal(0, a.recenterRequests, "First resume still starts the full settle interval");
            Handler.runUntil(2000L); equal(1, a.recenterRequests, "Initial replays share one first request");
            Handler.runUntil(3499L); equal(1, a.recenterRequests, "Initial replays cannot advance recovery");
            Handler.runUntil(3500L); equal(2, a.recenterRequests, "Only the one existing recovery is available");
            Handler.runUntil(61000L); assertComplete(a, 2);
            check(a.requestTimes.get(0).longValue() == 2000L && a.requestTimes.get(1).longValue() == 3500L,
                "Replay does not change movement timing or duplicate the retry budget");
        });
        scenario("native cold-launch replay cannot renew the original sixty-second deadline", () -> {
            MapsActivity a = create(launcher()); Handler.runUntil(59000L);
            StartupLocation.onNewIntent(a, launcher()); a.nativeFixReady = true; foreground(a);
            Handler.runUntil(62000L);
            equal(0, a.bridgeCalls, "Initial replay cannot extend the deadline beyond the initial creation");
            assertComplete(a, 0);
        });
        scenario("malformed initial redelivery cancels and cannot be revived by MAIN", () -> {
            Intent broken = new Intent(Intent.ACTION_MAIN) {
                @Override public String getAction() { throw new IllegalArgumentException("unreadable initial intent"); }
            };
            MapsActivity a = create(launcher()); StartupLocation.onNewIntent(a, broken);
            StartupLocation.onNewIntent(a, launcher()); tap(a); foreground(a); finishWithFix(a);
            equal(0, a.bridgeCalls, "Unreadable initial redelivery must end the pending action safely");
            assertComplete(a, 0);
        });
        scenario("immediate and repeated startup taps preserve the original first request", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true;
            tap(a);
            Handler.runUntil(500L); tap(a);
            Handler.runUntil(999L); tap(a);
            equal(0, a.bridgeCalls, "Touches before a request do not call the native bridge");
            equal(0, a.recenterRequests, "Taps cannot issue an early movement");
            Handler.runUntil(1000L);
            equal(1, a.recenterRequests, "Startup taps must not cancel or defer the first request");
            check(a.requestTimes.get(0).longValue() == 1000L, "Taps cannot restart the initial settle delay");
            Handler.runUntil(1500L); assertComplete(a, 1);
        });
        scenario("taps while a fix is delayed preserve pending startup", () -> {
            MapsActivity a = launch(launcher());
            Handler.runUntil(2000L); int calls = a.bridgeCalls; tap(a);
            equal(calls, a.bridgeCalls, "A pre-request tap does not query the native graph");
            Handler.runUntil(5000L); tap(a);
            equal(0, a.recenterRequests, "No movement can occur without a fix");
            check(Handler.queuedCallbacks() > 0, "Taps leave the location wait pending");
            a.nativeFixReady = true;
            Handler.runUntil(5750L); assertComplete(a, 1);
            check(a.requestTimes.get(0).longValue() == 5250L, "The next scheduled poll uses the delayed fix");
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
        scenario("touch before arrival preserves the original recovery time", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); int reads = a.readOnlyQueries; tap(a);
            equal(reads + 1, a.readOnlyQueries, "A pending dispatched move is checked read-only on touch");
            equal(1, a.recenterRequests, "Touch does not issue a recovery itself");
            check(Handler.queuedCallbacks() > 0, "A movement request is not evidence of arrival");
            Handler.runUntil(2499L); tap(a);
            equal(1, a.recenterRequests, "Touch cannot move recovery ahead of its delay");
            a.autoAcknowledge = true;
            Handler.runUntil(2500L); equal(2, a.recenterRequests, "Recovery remains due at the original time");
            Handler.runUntil(3000L); assertComplete(a, 2);
        });
        scenario("arrival after a pending tap completes without an extra request", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); tap(a);
            a.cameraAcknowledged = true;
            Handler.runUntil(1250L); check(Handler.queuedCallbacks() > 0, "First scheduled acknowledgement stays pending");
            Handler.runUntil(1500L); assertComplete(a, 1);
        });
        for (final long touchAt : new long[] {1000L, 1250L}) {
            scenario("manual gesture after actual arrival is not pulled back at " + touchAt + "ms", () -> {
                MapsActivity a = launch(launcher()); a.nativeFixReady = true;
                Handler.runUntil(touchAt); equal(1, a.recenterRequests, "Only the startup move has been issued");
                int reads = a.readOnlyQueries; tap(a);
                equal(reads + 1, a.readOnlyQueries, "Touch observes native arrival without another move");
                assertComplete(a, 1);
                // The real hook runs before Maps handles the gesture. Simulate that
                // gesture moving the camera only after the arrival observation.
                a.cameraAcknowledged = false; a.nativeTracking = false;
                int calls = a.bridgeCalls;
                Handler.advanceBy(5000L); tap(a);
                StartupLocation.onPause(a); StartupLocation.onResume(a);
                Handler.advanceBy(60000L);
                equal(calls, a.bridgeCalls, "Arrival ends all queries before subsequent manual camera changes");
                assertComplete(a, 1);
            });
        }
        scenario("repeated pending taps cannot replenish recovery budget or extend the deadline", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L);
            for (long time = 1100L; time < 60000L; time += 1000L) {
                Handler.runUntil(time); int requests = a.recenterRequests; tap(a);
                equal(requests, a.recenterRequests, "Every tap is observational, never a camera request");
                check(a.recenterRequests <= 2, "Taps cannot replenish the two-command budget");
            }
            Handler.runUntil(59999L); tap(a);
            equal(2, a.recenterRequests, "Only initial and recovery requests are allowed despite repeated input");
            check(a.requestTimes.get(0).longValue() == 1000L
                && a.requestTimes.get(1).longValue() == 2500L, "Input does not postpone or accelerate request timing");
            Handler.runUntil(60000L); assertComplete(a, 2);
            int calls = a.bridgeCalls; tap(a); a.cameraAcknowledged = true;
            Handler.advanceBy(5000L);
            equal(calls, a.bridgeCalls, "Expired startup cannot restart on another tap or a late fix");
            assertComplete(a, 2);
        });
        scenario("taps before a delayed fix cannot extend the sixty-second wait", () -> {
            MapsActivity a = launch(launcher());
            for (long time = 0L; time <= 55000L; time += 5000L) {
                Handler.runUntil(time); tap(a);
            }
            Handler.runUntil(59999L); tap(a); a.nativeFixReady = true;
            Handler.runUntil(60000L); assertComplete(a, 0);
            int calls = a.bridgeCalls; tap(a); Handler.advanceBy(2000L);
            equal(calls, a.bridgeCalls, "Late input does not rearm an expired location wait");
        });
        scenario("touch after the deadline cannot query before an overdue callback runs", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); int calls = a.bridgeCalls;
            // Simulate the main thread being busy past the timeout before the
            // queued poll gets a turn, with input handled first on resumption.
            SystemClock.setElapsedRealtime(60000L); tap(a);
            equal(calls, a.bridgeCalls, "An expired touch must not query the native camera");
            assertComplete(a, 1);
            Handler.runUntil(61000L); assertComplete(a, 1);
        });
        scenario("touch while paused cannot query native state or discard the pending move", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); StartupLocation.onPause(a);
            int calls = a.bridgeCalls; tap(a);
            equal(calls, a.bridgeCalls, "No touch query while the activity is paused");
            a.autoAcknowledge = true; StartupLocation.onResume(a);
            Handler.runUntil(4000L); assertComplete(a, 2);
        });
        scenario("touch without focus preserves pending work without native queries", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); a.windowFocus = false;
            int calls = a.bridgeCalls; tap(a);
            equal(calls, a.bridgeCalls, "No native touch query behind another window");
            a.windowFocus = true; a.autoAcknowledge = true;
            Handler.runUntil(3000L); assertComplete(a, 2);
        });
        for (final String state : new String[] {"finishing", "destroyed"}) {
            scenario("touch cannot query an unhealthy activity: " + state, () -> {
                MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
                Handler.runUntil(1000L);
                if ("finishing".equals(state)) a.finishing = true; else a.destroyed = true;
                int calls = a.bridgeCalls; tap(a);
                equal(calls, a.bridgeCalls, "No touch query after activity termination begins");
                Handler.runUntil(2000L); assertComplete(a, 1);
            });
        }
        scenario("touch observes navigation before the next poll and leaves it terminal", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); a.navigationMode = true;
            int reads = a.readOnlyQueries; tap(a);
            equal(reads + 1, a.readOnlyQueries, "Navigation is observed with a read-only query");
            assertComplete(a, 1);
            int calls = a.bridgeCalls; a.navigationMode = false; tap(a);
            StartupLocation.onPause(a); StartupLocation.onResume(a); Handler.advanceBy(5000L);
            equal(calls, a.bridgeCalls, "Leaving navigation cannot revive the cancelled opening");
            assertComplete(a, 1);
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
            tap(a); StartupLocation.onPause(a); StartupLocation.onResume(a); tap(a); Handler.advanceBy(5000L);
            equal(1, a.recenterRequests, "Transient lifecycle cannot move map again");
            equal(calls, a.bridgeCalls, "Completed session is not queried again");
        });
        scenario("pre-fix pause and resume can still complete", () -> {
            MapsActivity a = launch(launcher()); Handler.advanceBy(1500L);
            StartupLocation.onPause(a); int calls = a.bridgeCalls; a.nativeFixReady = true;
            Handler.advanceBy(3000L); equal(calls, a.bridgeCalls, "No callbacks while paused");
            StartupLocation.onResume(a); Handler.advanceBy(2000L); assertComplete(a, 1);
        });
        scenario("retained task from Recents preserves the user map after Home", () -> {
            MapsActivity a = launch(launcher()); finishWithFix(a);
            a.cameraAcknowledged = false; a.nativeTracking = false;
            int calls = a.bridgeCalls;
            background(a); Handler.advanceBy(70000L); foreground(a); tap(a);
            Handler.advanceBy(65000L);
            equal(1, a.recenterRequests, "Retained task must not request current location again");
            equal(calls, a.bridgeCalls, "Retained map must not be queried by startup work");
            assertComplete(a, 1);
        });
        for (final boolean intentBeforeStart : new boolean[] {true, false}) {
            scenario("retained task icon MAIN does not rearm: intent before start=" + intentBeforeStart, () -> {
                MapsActivity a = launch(launcher()); finishWithFix(a);
                a.cameraAcknowledged = false; a.nativeTracking = false;
                int calls = a.bridgeCalls;
                background(a); Handler.advanceBy(3000L);
                if (intentBeforeStart) StartupLocation.onNewIntent(a, launcher());
                StartupLocation.onStart(a);
                if (!intentBeforeStart) StartupLocation.onNewIntent(a, launcher());
                StartupLocation.onResume(a); tap(a); Handler.advanceBy(65000L);
                equal(calls, a.bridgeCalls, "Launcher reuse cannot schedule a new query or movement");
                assertComplete(a, 1);
            });
        }
        scenario("MAIN on an existing paused activity cannot rearm completed startup", () -> {
            MapsActivity a = launch(launcher()); finishWithFix(a);
            a.cameraAcknowledged = false; a.nativeTracking = false;
            int calls = a.bridgeCalls;
            StartupLocation.onPause(a); StartupLocation.onNewIntent(a, launcher());
            StartupLocation.onResume(a); Handler.advanceBy(65000L);
            equal(calls, a.bridgeCalls, "New launcher intent cannot reopen a completed startup action");
            assertComplete(a, 1);
        });
        scenario("retained external entry stays excluded when reopened through its icon", () -> {
            MapsActivity a = launch(external()); background(a);
            StartupLocation.onNewIntent(a, launcher()); a.nativeFixReady = true;
            foreground(a); tap(a); Handler.advanceBy(65000L);
            equal(0, a.bridgeCalls, "An existing destination task cannot become a fresh startup");
            assertComplete(a, 0);
        });
        scenario("rapid MAIN intents on an existing activity cancel pending startup", () -> {
            MapsActivity a = launch(launcher()); Handler.runUntil(500L); StartupLocation.onPause(a);
            StartupLocation.onNewIntent(a, launcher()); StartupLocation.onNewIntent(a, launcher());
            StartupLocation.onResume(a); tap(a); finishWithFix(a);
            equal(0, a.bridgeCalls, "Repeated launcher intents cannot reset the initial wait or budget");
            assertComplete(a, 0);
        });
        scenario("MAIN before arrival cancels recovery on the existing activity", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); int calls = a.bridgeCalls;
            StartupLocation.onPause(a); StartupLocation.onNewIntent(a, launcher());
            StartupLocation.onResume(a); tap(a); a.autoAcknowledge = true; Handler.advanceBy(65000L);
            equal(calls, a.bridgeCalls, "New launcher intent cannot recover the earlier move");
            assertComplete(a, 1);
        });
        for (final String stage : new String[] {"before settle", "waiting for fix", "first request",
                "first acknowledgement", "both requests"}) {
            scenario("Home ends pending startup permanently: " + stage, () -> {
                MapsActivity a = launch(launcher());
                a.nativeFixReady = !"waiting for fix".equals(stage);
                a.autoAcknowledge = "first acknowledgement".equals(stage);
                long stopAt = "before settle".equals(stage) ? 500L
                    : "waiting for fix".equals(stage) ? 2000L
                    : "first request".equals(stage) ? 1000L
                    : "first acknowledgement".equals(stage) ? 1250L : 2500L;
                Handler.runUntil(stopAt);
                int count = a.recenterRequests; int calls = a.bridgeCalls;
                check(Handler.queuedCallbacks() > 0, "The startup attempt is still pending at Home");
                background(a); equal(0, Handler.queuedCallbacks(), "Home removes all pending callbacks");
                a.nativeFixReady = true; a.autoAcknowledge = true;
                a.cameraAcknowledged = false; a.nativeTracking = false;
                tap(a); Handler.advanceBy(3000L); foreground(a); tap(a); Handler.advanceBy(65000L);
                equal(calls, a.bridgeCalls, "Returning from Home cannot resume a deferred startup query");
                assertComplete(a, count);
                StartupLocation.onPause(a); StartupLocation.onNewIntent(a, launcher()); StartupLocation.onResume(a);
                Handler.advanceBy(2500L); equal(calls, a.bridgeCalls, "A later icon intent cannot revive cancelled work");
                assertComplete(a, count);
            });
        }
        scenario("stop before first resume prevents a deferred initial request", () -> {
            MapsActivity a = create(launcher()); StartupLocation.onStart(a); StartupLocation.onStop(a);
            a.nativeFixReady = true; Handler.advanceBy(2000L);
            StartupLocation.onNewIntent(a, launcher()); foreground(a); tap(a); Handler.advanceBy(65000L);
            equal(0, a.bridgeCalls, "Even pre-resume MAIN cannot revive an unseen stopped opening");
            assertComplete(a, 0);
        });
        scenario("repeated Home and icon reuse never replenish a completed opening", () -> {
            MapsActivity a = launch(launcher()); finishWithFix(a); int calls = a.bridgeCalls;
            a.cameraAcknowledged = false; a.nativeTracking = false;
            for (int round = 0; round < 5; round++) {
                background(a); Handler.advanceBy(5000L);
                if ((round & 1) == 0) StartupLocation.onNewIntent(a, launcher());
                foreground(a); tap(a); Handler.advanceBy(70000L);
                equal(calls, a.bridgeCalls, "Repeated task return cannot start a new sixty-second window");
                assertComplete(a, 1);
            }
        });
        scenario("saved-state process restoration does not start current-location work", () -> {
            MapsActivity a = create(launcher(), new Bundle());
            // Native Maps may discard a stale Bundle and replay its current intent;
            // an excluded restored session must still not be armed by that replay.
            StartupLocation.onNewIntent(a, launcher()); foreground(a); a.nativeFixReady = true;
            tap(a); Handler.advanceBy(65000L); equal(0, a.bridgeCalls, "Restored state is preserved");
            background(a); StartupLocation.onNewIntent(a, launcher()); foreground(a); Handler.advanceBy(2500L);
            equal(0, a.bridgeCalls, "Reopening restored state stays excluded"); assertComplete(a, 0);
        });
        for (final boolean wasComplete : new boolean[] {false, true}) {
            scenario("saved-state recreation preserves the map: previous complete=" + wasComplete, () -> {
                MapsActivity old = launch(launcher());
                if (wasComplete) finishWithFix(old); else Handler.runUntil(500L);
                int calls = old.bridgeCalls; int count = old.recenterRequests;
                background(old); StartupLocation.onDestroy(old); old.destroyed = true;
                MapsActivity restored = create(launcher(), new Bundle()); foreground(restored);
                restored.nativeFixReady = true; tap(restored); Handler.advanceBy(65000L);
                equal(calls, old.bridgeCalls, "Recreation cannot leave old callbacks active");
                equal(count, old.recenterRequests, "The destroyed activity does not move again");
                equal(0, restored.bridgeCalls, "Recreated map cannot consume a new startup request");
                assertComplete(restored, 0);
            });
        }
        for (final int flags : new int[] {Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY}) {
            scenario("history restoration without saved state preserves the map: flags=" + flags, () -> {
                MapsActivity a = launch(launcher().setFlags(flags)); a.nativeFixReady = true;
                tap(a); Handler.advanceBy(65000L);
                equal(0, a.bridgeCalls, "History origin cannot be mistaken for a new launcher creation");
                background(a); StartupLocation.onNewIntent(a, launcher()); foreground(a); Handler.advanceBy(2500L);
                equal(0, a.bridgeCalls, "Subsequent MAIN cannot revive history startup work"); assertComplete(a, 0);
            });
        }
        scenario("ordinary launcher flags still allow a fresh creation", () -> {
            MapsActivity a = launch(launcher().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            tap(a); finishWithFix(a); assertComplete(a, 1);
        });
        for (final boolean wasComplete : new boolean[] {false, true}) {
            scenario("real exit then fresh launch gets its own one-shot request: previous complete=" + wasComplete, () -> {
                MapsActivity old = launch(launcher());
                if (wasComplete) finishWithFix(old); else Handler.runUntil(500L);
                int calls = old.bridgeCalls; int count = old.recenterRequests;
                old.finishing = true; background(old); StartupLocation.onDestroy(old); old.destroyed = true;
                MapsActivity fresh = launch(launcher().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                fresh.nativeFixReady = true; tap(fresh);
                Handler.advanceBy(999L); equal(0, fresh.bridgeCalls, "Fresh opening still settles before moving");
                Handler.advanceBy(1501L);
                equal(calls, old.bridgeCalls, "Exited activity cannot receive new callbacks");
                equal(count, old.recenterRequests, "Exited activity remains unchanged");
                assertComplete(fresh, 1);
            });
        }
        Intent selector = launcher(); selector.setSelector(new Intent(Intent.ACTION_VIEW));
        final Intent[] excluded = {external(), launcher().setData(Uri.parse("geo:0,0?q=example")), selector,
            launcher().setComponent(new ComponentName(PACKAGE, "com.google.android.maps.PlacesActivity")),
            launcher().setType("application/example"), new Intent(Intent.ACTION_MAIN),
            new Intent().addCategory(Intent.CATEGORY_LAUNCHER), null};
        final String[] names = {"VIEW", "data", "selector", "alias", "MIME", "no launcher category", "no action", "null"};
        for (int index = 0; index < excluded.length; index++) {
            final Intent intent = excluded[index]; final String name = names[index];
            scenario("excluded cold entry stays excluded: " + name, () -> {
                MapsActivity a = launch(intent); tap(a); finishWithFix(a); background(a); foreground(a); tap(a); Handler.advanceBy(2500L);
                equal(0, a.bridgeCalls, "Destination entry cannot call recenter bridge"); assertComplete(a, 0);
            });
            scenario("initial destination redelivery cancels before first resume: " + name, () -> {
                MapsActivity a = create(launcher()); StartupLocation.onNewIntent(a, intent);
                StartupLocation.onNewIntent(a, launcher()); tap(a); foreground(a); finishWithFix(a);
                equal(0, a.bridgeCalls, "Initial destination wins and later MAIN cannot revive startup");
                assertComplete(a, 0);
            });
            scenario("new destination cancels pending recovery: " + name, () -> {
                MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
                Handler.runUntil(1000L); tap(a); StartupLocation.onNewIntent(a, intent); int calls = a.bridgeCalls;
                tap(a); Handler.advanceBy(3000L); background(a); foreground(a); tap(a); Handler.advanceBy(3000L);
                equal(calls, a.bridgeCalls, "New destination cancels old confirmation/recovery"); assertComplete(a, 1);
            });
        }
        scenario("implicit normal launcher remains accepted", () -> {
            MapsActivity a = launch(launcher().setComponent(null)); finishWithFix(a); assertComplete(a, 1);
        });
        scenario("touch before first resume does not discard the initial opening", () -> {
            MapsActivity a = create(launcher()); tap(a); a.nativeFixReady = true;
            Handler.advanceBy(2000L); equal(0, a.bridgeCalls, "Pre-resume input does not access native state");
            foreground(a); Handler.advanceBy(2000L); assertComplete(a, 1);
        });
        scenario("null and non-down motion do not query or cancel pending arrival", () -> {
            MapsActivity a = launch(launcher()); a.nativeFixReady = true; a.autoAcknowledge = false;
            Handler.runUntil(1000L); int calls = a.bridgeCalls; StartupLocation.onTouch(a, null);
            StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_MOVE)); StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_UP));
            StartupLocation.onTouch(a, new MotionEvent(MotionEvent.ACTION_CANCEL));
            equal(calls, a.bridgeCalls, "Only the beginning of a touch can observe arrival");
            a.cameraAcknowledged = true; Handler.runUntil(1500L); assertComplete(a, 1);
        });
        scenario("actual navigation is a terminal exclusion", () -> {
            MapsActivity a = launch(launcher()); tap(a); a.navigationMode = true; a.nativeFixReady = true;
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
            int calls = a.bridgeCalls; tap(a);
            StartupLocation.onResume(a); StartupLocation.onNewIntent(a, launcher()); Handler.advanceBy(61000L); assertComplete(a, 1);
            equal(calls, a.bridgeCalls, "Destroyed sessions cannot be queried by touch or lifecycle calls");
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
            tap(a); a.nativeFixReady = true; b.nativeFixReady = true; b.autoAcknowledge = false;
            Handler.runUntil(1000L); tap(a);
            equal(1, a.recenterRequests, "Touched startup reaches its location");
            equal(1, b.recenterRequests, "Other activity also issues its own startup request");
            a.cameraAcknowledged = false; a.nativeTracking = false; b.autoAcknowledge = true;
            Handler.runUntil(3000L);
            equal(1, a.recenterRequests, "Completed touched activity is not pulled back");
            equal(2, b.recenterRequests, "Other activity retains its independent recovery");
            equal(0, Handler.queuedCallbacks(), "Both activity sessions complete independently");
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
            + "; native cold-launch callback replay, retained-task preservation, early-touch persistence and bounded recovery verified; Android UI execution not performed.");
        if (args.length > 0 && "--list".equals(args[0])) for (String name : PASSED) System.out.println("PASS " + name);
    }
}
