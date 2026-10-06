package com.hiro.ulike;

import android.app.Activity;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Capture-only exit guard. The retired StyleStill4 diagnostics are NOT work items. */
public final class ExitBusy1921 {
    private static volatile boolean warned;
    private ExitBusy1921() {}

    public static boolean route(BackExit185 controller, Activity activity) {
        // A second Back cancels only a pending exit, never an actual save or recording.
        if (controller != null && Looper.myLooper() == Looper.getMainLooper()
                && BackExit185.cancelExit1921(controller)) return true;
        return BackRoute1920.consume(controller, activity);
    }

    private static Object value(String type, String field) throws ReflectiveOperationException {
        Field f = Class.forName("com.hiro.ulike." + type).getDeclaredField(field);
        f.setAccessible(true);
        return f.get(null);
    }

    public static boolean captureBusy(boolean closed) {
        try {
            if (!closed && ((AtomicInteger) value("OpticalZoom", "inFlight")).get() > 0)
                return true;
            if (!closed && SystemClock.uptimeMillis()
                    < ((Number) value("OpticalZoom", "stillUntil")).longValue()) return true;
            Map<?, ?> states = (Map<?, ?>) value("CaptureYuv", "states");
            Object[] snapshot;
            synchronized (states) { snapshot = states.values().toArray(); }
            for (Object state : snapshot) {
                if (state == null) continue;
                synchronized (state) {
                    for (String name : new String[]{"delivering", "selected"}) {
                        Field f = state.getClass().getDeclaredField(name);
                        f.setAccessible(true);
                        if (f.getBoolean(state)) return true;
                    }
                    for (String name : new String[]{"pending", "captureCallback"}) {
                        Field f = state.getClass().getDeclaredField(name);
                        f.setAccessible(true);
                        if (f.get(state) != null) return true;
                    }
                }
            }
            // These are the actual lens handoff/delivery queues, not diagnostic maps.
            Object lock = value("LensRelease163", "LOCK");
            synchronized (lock) {
                for (String name : new String[]{"callbacks", "nativeCallbacks", "deliveries", "stops"}) {
                    if (!((Map<?, ?>) value("LensRelease163", name)).isEmpty()) return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            // Unknown state must not authorize killing a writer. ExitFlow bounds/cancels the wait.
            if (!warned) {
                warned = true;
                Log.w("ULikeExit", "Cannot verify active capture; preserving work", error);
            }
            return true;
        }
    }
}
