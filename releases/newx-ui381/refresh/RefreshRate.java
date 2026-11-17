package app.hiro.newxui;

import android.app.Activity;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.util.WeakHashMap;

/** Window-scoped 120 Hz preference. The OS still owns the final display rate. */
public final class RefreshRate {
    private static final float TARGET_HZ = 120.0f;
    private static final float TOLERANCE_HZ = 0.6f;
    private static final WeakHashMap<Window, Saved> SAVED = new WeakHashMap<Window, Saved>();

    private static final class Saved {
        int originalMode;
        float originalRate;
        int appliedMode;
        float appliedRate;
        Saved(WindowManager.LayoutParams p) {
            originalMode = p.preferredDisplayModeId;
            originalRate = p.preferredRefreshRate;
        }
    }

    private RefreshRate() {}

    /** Invoke on the UI thread after the native MainActivity.onResume body. */
    public static synchronized void bind(Activity activity) {
        if (activity == null) return;
        try {
            Window window = activity.getWindow();
            if (window == null) return;
            WindowManager.LayoutParams current = window.getAttributes();
            if (current == null) return;
            if (!UiOptions.force120Hz()) {
                restore(window, current);
                return;
            }
            View decor = window.getDecorView();
            Display display = decor == null ? null : decor.getDisplay();
            if (display == null) display = activity.getWindowManager().getDefaultDisplay();
            Display.Mode mode = chooseMode(display);
            if (mode == null) {
                // A moved/resized window may no longer be on a compatible display.
                restore(window, current);
                return;
            }
            Saved saved = SAVED.get(window);
            if (saved == null) {
                saved = new Saved(current);
                SAVED.put(window, saved);
            } else {
                // Native changes since the last application become the new restoration baseline.
                if (current.preferredDisplayModeId != saved.appliedMode)
                    saved.originalMode = current.preferredDisplayModeId;
                if (Float.compare(current.preferredRefreshRate, saved.appliedRate) != 0)
                    saved.originalRate = current.preferredRefreshRate;
            }
            saved.appliedMode = mode.getModeId();
            saved.appliedRate = mode.getRefreshRate();
            update(window, current, saved.appliedMode, saved.appliedRate);
        } catch (RuntimeException ignored) {
            // Losing a window/display during an Activity transition must not prevent X resuming.
        } catch (LinkageError ignored) {
            // An OEM API mismatch should leave the native window behavior intact.
        }
    }

    /** Never change resolution or select a non-120 Hz fallback. */
    static Display.Mode chooseMode(Display display) {
        if (display == null) return null;
        Display.Mode current = display.getMode();
        Display.Mode[] modes = display.getSupportedModes();
        if (current == null || modes == null) return null;
        Display.Mode best = null;
        float distance = Float.POSITIVE_INFINITY;
        for (Display.Mode candidate : modes) {
            if (candidate == null || candidate.getModeId() <= 0
                    || candidate.getPhysicalWidth() != current.getPhysicalWidth()
                    || candidate.getPhysicalHeight() != current.getPhysicalHeight()) continue;
            float delta = Math.abs(candidate.getRefreshRate() - TARGET_HZ);
            if (delta > TOLERANCE_HZ || Float.isNaN(delta)) continue;
            if (best == null || delta < distance
                    || (Float.compare(delta, distance) == 0
                        && candidate.getModeId() == current.getModeId())) {
                best = candidate;
                distance = delta;
            }
        }
        return best;
    }

    private static void restore(Window window, WindowManager.LayoutParams current) {
        Saved saved = SAVED.remove(window);
        if (saved == null) return;
        // Restore only values still owned by this extension. Preserve later native writes.
        int mode = current.preferredDisplayModeId == saved.appliedMode
                ? saved.originalMode : current.preferredDisplayModeId;
        float rate = Float.compare(current.preferredRefreshRate, saved.appliedRate) == 0
                ? saved.originalRate : current.preferredRefreshRate;
        update(window, current, mode, rate);
    }

    private static void update(Window window, WindowManager.LayoutParams current,
            int mode, float rate) {
        if (current.preferredDisplayModeId == mode
                && Float.compare(current.preferredRefreshRate, rate) == 0) return;
        WindowManager.LayoutParams next = new WindowManager.LayoutParams();
        next.copyFrom(current);
        next.preferredDisplayModeId = mode;
        next.preferredRefreshRate = rate;
        window.setAttributes(next);
    }
}
