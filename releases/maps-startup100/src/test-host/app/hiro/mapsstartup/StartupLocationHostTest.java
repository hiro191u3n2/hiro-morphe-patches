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

/** Executes the production runtime against deterministic Android API stand-ins. */
public final class StartupLocationHostTest {
    private static final String ACTIVITY = "com.google.android.maps.MapsActivity";
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
        Handler.reset();
        Activity.INSTANCES.clear();
        View.INSTANCES.clear();
        try {
            body.run();
            for (Activity activity : Activity.INSTANCES) {
                equal(0, activity.systemServiceRequests, "No independent service/location access");
                equal(0, activity.permissionRequests, "No automatic permission requests");
            }
            for (View view : View.INSTANCES) {
                equal(0, view.syntheticClickCalls, "No synthetic current-location button clicks");
            }
            scenarios++;
            PASSED.add(name);
        } catch (Throwable failure) {
            throw new AssertionError("Host scenario failed: " + name, failure);
        } finally {
            for (Activity activity : Activity.INSTANCES) StartupLocation.onDestroy(activity);
            Handler.reset();
            Activity.INSTANCES.clear();
            View.INSTANCES.clear();
        }
    }
    private static Intent launcher() {
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(new ComponentName(PACKAGE, ACTIVITY));
    }
    private static Intent external() {
        return new Intent(Intent.ACTION_VIEW).setData(Uri.parse("geo:0,0?q=example"));
    }
    private static MapsActivity create(Intent intent) {
        MapsActivity activity = new MapsActivity();
        activity.setIntent(intent);
        StartupLocation.onCreate(activity, null);
        return activity;
    }
    private static void foreground(MapsActivity activity) {
        StartupLocation.onStart(activity);
        StartupLocation.onResume(activity);
    }
    private static MapsActivity launch(Intent intent) {
        MapsActivity activity = create(intent);
        foreground(activity);
        return activity;
    }
    private static void background(MapsActivity activity) {
        StartupLocation.onPause(activity);
        StartupLocation.onStop(activity);
    }
    private static void readyAndDrain(MapsActivity activity) {
        activity.nativeFixReady = true;
        Handler.advanceBy(1000L);
    }

    public static void main(String[] args) {
        scenario("cold launcher waits for native fix, then requests once", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                equal(0, activity.bridgeCalls, "No synchronous native call during creation");
                Handler.advanceBy(2000L);
                equal(0, activity.recenterRequests, "No request before native fix");
                check(activity.bridgeCalls > 0, "Native readiness was queried");
                readyAndDrain(activity);
                equal(1, activity.recenterRequests, "One request when a fix becomes ready");
                int calls = activity.bridgeCalls;
                Handler.advanceBy(120000L);
                equal(calls, activity.bridgeCalls, "No native calls after success");
                equal(1, activity.recenterRequests, "No later repeat");
                equal(0, Handler.queuedCallbacks(), "Successful session has no queued callback");
            }
        });
        scenario("already tracking is consumed without another request", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                activity.nativeFixReady = true;
                activity.nativeTracking = true;
                Handler.advanceBy(1000L);
                equal(1, activity.bridgeCalls, "Already-centered result is terminal");
                equal(0, activity.recenterRequests, "Already-centered map is not moved again");
                activity.nativeTracking = false;
                Handler.advanceBy(60000L);
                equal(0, activity.recenterRequests, "Later manual pan remains respected");
                equal(0, Handler.queuedCallbacks(), "No retry after already-centered result");
            }
        });
        scenario("pause and resume do not rearm a consumed opening", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                readyAndDrain(activity);
                activity.nativeTracking = false;
                StartupLocation.onPause(activity);
                Handler.advanceBy(1000L);
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(1, activity.recenterRequests, "Transient pause does not recenter again");
                equal(1, activity.bridgeCalls, "Consumed opening is not queried again");
            }
        });
        scenario("pause suspends a pending action and resume can finish it", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                Handler.advanceBy(1000L);
                StartupLocation.onPause(activity);
                int calls = activity.bridgeCalls;
                activity.nativeFixReady = true;
                Handler.advanceBy(3000L);
                equal(calls, activity.bridgeCalls, "No callbacks while paused");
                equal(0, activity.recenterRequests, "No movement while paused");
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(1, activity.recenterRequests, "Pending startup finishes after resume");
            }
        });
        scenario("full stop and start rearm a normal foreground opening", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                readyAndDrain(activity);
                activity.nativeTracking = false;
                background(activity);
                Handler.advanceBy(5000L);
                equal(1, activity.recenterRequests, "No movement in background");
                foreground(activity);
                Handler.advanceBy(1000L);
                equal(2, activity.recenterRequests, "One new request for new foreground opening");
                Handler.advanceBy(5000L);
                equal(2, activity.recenterRequests, "New opening also remains one-shot");
            }
        });
        scenario("new foreground opening gets a fresh timeout", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                Handler.advanceBy(1000L);
                background(activity);
                Handler.advanceBy(70000L);
                activity.nativeFixReady = true;
                foreground(activity);
                Handler.advanceBy(1000L);
                equal(1, activity.recenterRequests, "Expired old opening does not prevent a fresh opening");
            }
        });

        final Intent selectorLaunch = launcher();
        selectorLaunch.setSelector(new Intent(Intent.ACTION_VIEW));
        final Intent[] excluded = new Intent[] {
            external(),
            launcher().setData(Uri.parse("geo:0,0?q=example")),
            selectorLaunch,
            launcher().setComponent(new ComponentName(PACKAGE, "com.google.android.maps.PlacesActivity")),
            launcher().setType("application/example"),
            new Intent(Intent.ACTION_MAIN),
            new Intent().addCategory(Intent.CATEGORY_LAUNCHER),
            null
        };
        final String[] excludedNames = new String[] {
            "ACTION_VIEW", "data URI", "selector", "activity alias", "MIME type",
            "missing launcher category", "missing action", "null intent"
        };
        for (int index = 0; index < excluded.length; index++) {
            final Intent intent = excluded[index];
            final String name = excludedNames[index];
            scenario("excluded cold launch: " + name, new Runnable() {
                @Override public void run() {
                    MapsActivity activity = launch(intent);
                    readyAndDrain(activity);
                    background(activity);
                    foreground(activity);
                    Handler.advanceBy(1000L);
                    equal(0, activity.bridgeCalls, "Excluded launch never reaches native recenter bridge");
                    equal(0, activity.recenterRequests, "Excluded destination remains untouched");
                    equal(0, Handler.queuedCallbacks(), "Excluded launch has no pending callback");
                }
            });
            scenario("new intent cancels pending launcher: " + name, new Runnable() {
                @Override public void run() {
                    MapsActivity activity = launch(launcher());
                    Handler.advanceBy(1000L);
                    int calls = activity.bridgeCalls;
                    StartupLocation.onNewIntent(activity, intent);
                    activity.nativeFixReady = true;
                    Handler.advanceBy(2000L);
                    background(activity);
                    foreground(activity);
                    Handler.advanceBy(1000L);
                    equal(calls, activity.bridgeCalls, "New excluded intent cancels native polling");
                    equal(0, activity.recenterRequests, "No old request overrides the new destination");
                    equal(0, Handler.queuedCallbacks(), "Cancelled destination leaves no callback");
                }
            });
        }
        scenario("implicit launcher component is accepted", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher().setComponent(null));
                readyAndDrain(activity);
                equal(1, activity.recenterRequests, "Supported implicit launcher request can recenter");
            }
        });
        scenario("warm launcher intent starts exactly one new request", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                readyAndDrain(activity);
                activity.nativeTracking = false;
                StartupLocation.onNewIntent(activity, launcher());
                Handler.advanceBy(1000L);
                equal(2, activity.recenterRequests, "Warm icon reopening creates one new request");
                StartupLocation.onResume(activity);
                Handler.advanceBy(5000L);
                equal(2, activity.recenterRequests, "Resume after new intent cannot duplicate request");
            }
        });
        scenario("warm launcher from external entry waits for resume", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(external());
                StartupLocation.onPause(activity);
                StartupLocation.onNewIntent(activity, launcher());
                activity.nativeFixReady = true;
                Handler.advanceBy(1000L);
                equal(0, activity.bridgeCalls, "Warm intent does not act while paused");
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(1, activity.recenterRequests, "The explicit new intent controls eligibility");
            }
        });
        scenario("rapid warm launcher intents do not leave parallel callbacks", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                StartupLocation.onNewIntent(activity, launcher());
                StartupLocation.onNewIntent(activity, launcher());
                readyAndDrain(activity);
                equal(1, activity.recenterRequests, "Only the active opening moves the map");
                equal(1, activity.bridgeCalls, "Replaced requests are not also executed");
            }
        });

        scenario("user touch cancels a pending startup request", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                Handler.advanceBy(1000L);
                int calls = activity.bridgeCalls;
                StartupLocation.onTouch(activity, new MotionEvent(MotionEvent.ACTION_DOWN));
                readyAndDrain(activity);
                StartupLocation.onPause(activity);
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(calls, activity.bridgeCalls, "Manual interaction stops native polling");
                equal(0, activity.recenterRequests, "No movement after user interaction");
                equal(0, Handler.queuedCallbacks(), "User cancellation removes queued work");
            }
        });
        scenario("touch cancellation permits a later explicit new opening", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                StartupLocation.onTouch(activity, new MotionEvent(MotionEvent.ACTION_DOWN));
                StartupLocation.onNewIntent(activity, launcher());
                readyAndDrain(activity);
                equal(1, activity.recenterRequests, "A fresh launch can act after earlier cancellation");
            }
        });
        scenario("non-down events and null events do not cancel startup", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                StartupLocation.onTouch(activity, null);
                StartupLocation.onTouch(activity, new MotionEvent(MotionEvent.ACTION_MOVE));
                StartupLocation.onTouch(activity, new MotionEvent(MotionEvent.ACTION_UP));
                readyAndDrain(activity);
                equal(1, activity.recenterRequests, "Only a fresh user down gesture cancels startup");
            }
        });
        scenario("native route or navigation mode is terminal for this opening", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                activity.navigationMode = true;
                activity.nativeFixReady = true;
                Handler.advanceBy(1000L);
                equal(1, activity.bridgeCalls, "Navigation exclusion is checked once");
                equal(0, activity.recenterRequests, "Route camera is left unchanged");
                activity.navigationMode = false;
                StartupLocation.onPause(activity);
                StartupLocation.onResume(activity);
                Handler.advanceBy(60000L);
                equal(1, activity.bridgeCalls, "Native -1 does not poll again on transient resume");
                equal(0, activity.recenterRequests, "Leaving route mode does not revive old startup action");
            }
        });

        final String[] readiness = new String[] { "null button", "hidden button", "unattached button", "disabled button", "window without focus" };
        for (final String gate : readiness) {
            scenario("UI readiness waits: " + gate, new Runnable() {
                @Override public void run() {
                    MapsActivity activity = launch(launcher());
                    activity.nativeFixReady = true;
                    if ("null button".equals(gate)) activity.putView(MapsActivity.MY_LOCATION_BUTTON, null);
                    if ("hidden button".equals(gate)) activity.locationButton.shown = false;
                    if ("unattached button".equals(gate)) activity.locationButton.windowToken = null;
                    if ("disabled button".equals(gate)) activity.locationButton.enabled = false;
                    if ("window without focus".equals(gate)) activity.windowFocus = false;
                    Handler.advanceBy(1500L);
                    equal(0, activity.bridgeCalls, "Unready UI must not invoke native camera action");
                    equal(0, activity.recenterRequests, "Unready UI cannot move map");
                    activity.putView(MapsActivity.MY_LOCATION_BUTTON, activity.locationButton);
                    activity.locationButton.shown = true;
                    activity.locationButton.enabled = true;
                    activity.locationButton.windowToken = new Object();
                    activity.windowFocus = true;
                    Handler.advanceBy(1000L);
                    equal(1, activity.recenterRequests, "Ready UI allows one pending action");
                    equal(0, Handler.queuedCallbacks(), "Success removes UI wait");
                }
            });
        }
        scenario("native fix arriving at sixty-second deadline is not used", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                Handler.runUntil(59999L);
                equal(0, activity.recenterRequests, "No request without native fix");
                activity.nativeFixReady = true;
                Handler.runUntil(65000L);
                equal(0, activity.recenterRequests, "Deadline prevents a late unsolicited map jump");
                equal(0, Handler.queuedCallbacks(), "Timed-out opening has no retries");
                StartupLocation.onPause(activity);
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(0, activity.recenterRequests, "Transient resume cannot revive timed-out action");
            }
        });
        scenario("long initial pause does not extend the same opening deadline", new Runnable() {
            @Override public void run() {
                MapsActivity activity = create(launcher());
                StartupLocation.onStart(activity);
                Handler.advanceBy(61000L);
                activity.nativeFixReady = true;
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(0, activity.recenterRequests, "Opening deadline remains bounded without focus/resume");
                equal(0, Handler.queuedCallbacks(), "Expired deferred startup terminates");
            }
        });
        scenario("destroy cancels queued work and removes lifecycle state", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                StartupLocation.onDestroy(activity);
                activity.nativeFixReady = true;
                StartupLocation.onResume(activity);
                StartupLocation.onStart(activity);
                StartupLocation.onNewIntent(activity, launcher());
                Handler.advanceBy(120000L);
                equal(0, activity.bridgeCalls, "Destroyed session cannot call native code");
                equal(0, activity.recenterRequests, "Destroyed activity is not recentered");
                equal(0, Handler.queuedCallbacks(), "Destroyed activity leaves no callback");
            }
        });
        scenario("finishing or destroyed Activity never receives native action", new Runnable() {
            @Override public void run() {
                MapsActivity finishing = launch(launcher());
                MapsActivity destroyed = launch(launcher());
                finishing.finishing = true;
                destroyed.destroyed = true;
                readyAndDrain(finishing);
                readyAndDrain(destroyed);
                equal(0, finishing.bridgeCalls, "Finishing activity is ignored");
                equal(0, destroyed.bridgeCalls, "Destroyed activity is ignored");
                equal(0, Handler.queuedCallbacks(), "Invalid activity states terminate");
            }
        });
        scenario("recreation replacement cannot leave the old request alive", new Runnable() {
            @Override public void run() {
                MapsActivity activity = launch(launcher());
                activity.setIntent(external());
                StartupLocation.onCreate(activity, new Bundle());
                foreground(activity);
                readyAndDrain(activity);
                equal(0, activity.bridgeCalls, "Superseded creation cannot recenter over external entry");
                equal(0, Handler.queuedCallbacks(), "Superseded callbacks are removed");
            }
        });
        scenario("two activity sessions remain independent", new Runnable() {
            @Override public void run() {
                MapsActivity cancelled = launch(launcher());
                MapsActivity active = launch(launcher());
                StartupLocation.onTouch(cancelled, new MotionEvent(MotionEvent.ACTION_DOWN));
                cancelled.nativeFixReady = true;
                active.nativeFixReady = true;
                Handler.advanceBy(1000L);
                equal(0, cancelled.recenterRequests, "Cancellation affects only its owning activity");
                equal(1, active.recenterRequests, "Other session still receives its single action");
            }
        });
        scenario("unsupported and missing activities are harmless", new Runnable() {
            @Override public void run() {
                Activity foreign = new Activity();
                foreign.setIntent(launcher());
                StartupLocation.onCreate(foreign, null);
                StartupLocation.onStart(foreign);
                StartupLocation.onResume(foreign);
                StartupLocation.onNewIntent(foreign, launcher());
                StartupLocation.onTouch(foreign, new MotionEvent(MotionEvent.ACTION_DOWN));
                StartupLocation.onPause(foreign);
                StartupLocation.onStop(foreign);
                StartupLocation.onDestroy(foreign);
                StartupLocation.onCreate(null, null);
                StartupLocation.onStart(null);
                StartupLocation.onResume(null);
                StartupLocation.onNewIntent(null, launcher());
                StartupLocation.onTouch(null, null);
                StartupLocation.onPause(null);
                StartupLocation.onStop(null);
                StartupLocation.onDestroy(null);
                Handler.advanceBy(1000L);
                equal(0, Handler.queuedCallbacks(), "Unsupported callbacks are no-ops");
            }
        });

        final String[] activityFaults = new String[] { "isFinishing", "isDestroyed", "hasWindowFocus", "findViewById" };
        for (final String fault : activityFaults) {
            scenario("activity API failure is terminal and cannot escape: " + fault, new Runnable() {
                @Override public void run() {
                    MapsActivity activity = launch(launcher());
                    activity.nativeFixReady = true;
                    activity.throwingMethod = fault;
                    Handler.advanceBy(1000L);
                    equal(0, activity.recenterRequests, "Failure does not trigger map movement");
                    activity.throwingMethod = null;
                    StartupLocation.onPause(activity);
                    StartupLocation.onResume(activity);
                    Handler.advanceBy(1000L);
                    equal(0, activity.recenterRequests, "Failure does not repeatedly retry through transient resume");
                    equal(0, Handler.queuedCallbacks(), "Failed runtime leaves no callback");
                }
            });
        }
        final String[] viewFaults = new String[] { "isShown", "isEnabled", "getWindowToken" };
        for (final String fault : viewFaults) {
            scenario("view API failure cannot escape: " + fault, new Runnable() {
                @Override public void run() {
                    MapsActivity activity = launch(launcher());
                    activity.nativeFixReady = true;
                    activity.locationButton.throwingMethod = fault;
                    Handler.advanceBy(1000L);
                    equal(0, activity.bridgeCalls, "Failed view readiness blocks native calls");
                    equal(0, Handler.queuedCallbacks(), "Failed view readiness terminates opening");
                }
            });
        }
        final Throwable[] bridgeFaults = new Throwable[] {
            new IllegalStateException("Injected native failure"),
            new LinkageError("Injected linkage failure")
        };
        for (final Throwable fault : bridgeFaults) {
            scenario("native bridge failure cannot escape: " + fault.getClass().getSimpleName(), new Runnable() {
                @Override public void run() {
                    MapsActivity activity = launch(launcher());
                    activity.nativeFixReady = true;
                    activity.bridgeFailure = fault;
                    Handler.advanceBy(1000L);
                    equal(1, activity.bridgeCalls, "Faulting bridge is called only once");
                    equal(0, activity.recenterRequests, "Faulting bridge does not produce a successful request");
                    activity.bridgeFailure = null;
                    StartupLocation.onPause(activity);
                    StartupLocation.onResume(activity);
                    Handler.advanceBy(1000L);
                    equal(1, activity.bridgeCalls, "Native failure is terminal for the opening");
                    equal(0, Handler.queuedCallbacks(), "Native failure has no pending work");
                }
            });
        }
        scenario("intent and creation failures cannot crash application lifecycle", new Runnable() {
            @Override public void run() {
                MapsActivity brokenGetter = new MapsActivity();
                brokenGetter.throwingMethod = "getIntent";
                StartupLocation.onCreate(brokenGetter, null);
                foreground(brokenGetter);
                Intent brokenIntent = new Intent(Intent.ACTION_MAIN) {
                    @Override public String getAction() { throw new IllegalArgumentException("Injected intent failure"); }
                };
                MapsActivity brokenAction = launch(brokenIntent);
                MapsActivity cancelled = launch(launcher());
                StartupLocation.onNewIntent(cancelled, brokenIntent);
                brokenGetter.nativeFixReady = true;
                brokenAction.nativeFixReady = true;
                cancelled.nativeFixReady = true;
                Handler.advanceBy(1000L);
                equal(0, brokenGetter.bridgeCalls, "Broken intent getter cannot start native action");
                equal(0, brokenAction.bridgeCalls, "Malformed intent cannot start native action");
                equal(0, cancelled.bridgeCalls, "Malformed new intent cancels old pending request safely");
                equal(0, Handler.queuedCallbacks(), "Malformed intents leave no callback");
            }
        });
        scenario("scheduler and motion-event failures cannot escape", new Runnable() {
            @Override public void run() {
                MapsActivity activity = create(launcher());
                StartupLocation.onStart(activity);
                Handler.nextPostFailure = new IllegalStateException("Injected scheduler failure");
                StartupLocation.onResume(activity);
                Handler.advanceBy(1000L);
                equal(0, activity.bridgeCalls, "Scheduler failure cannot synchronously invoke native action");
                MotionEvent brokenEvent = new MotionEvent(MotionEvent.ACTION_DOWN) {
                    @Override public int getActionMasked() { throw new IllegalStateException("Injected motion failure"); }
                };
                StartupLocation.onTouch(activity, brokenEvent);
                StartupLocation.onDestroy(activity);
                equal(0, Handler.queuedCallbacks(), "Destruction clears session after callback failures");
            }
        });

        System.out.println("PASS StartupLocation host lifecycle: scenarios=" + scenarios
            + " assertions=" + assertions
            + "; production runtime compiled with deterministic Android stubs; Android UI execution not performed.");
        if (args.length > 0 && "--list".equals(args[0])) {
            for (String name : PASSED) System.out.println("PASS " + name);
        }
    }
}
