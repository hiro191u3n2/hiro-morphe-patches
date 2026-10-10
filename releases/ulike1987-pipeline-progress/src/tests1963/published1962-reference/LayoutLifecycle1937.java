package com.hiro.ulike;

import com.bytedance.corecamera.ui.view.CameraShadeView;
import android.os.Handler;
import java.lang.ref.WeakReference;

/** Reject delayed UI work whose original camera view or selected ratio no longer owns it.
 * All entry points run on the native main/UI thread. Current geometry is intentionally
 * read at delivery: a still-selected ratio must adapt to the latest measured container.
 */
public final class LayoutLifecycle1937 {
    private LayoutLifecycle1937() {}
    private static boolean ratio(CameraShadeView view, int value, boolean round) {
        if (view == null || view.v == null) return false;
        String expected;
        switch (value) {
            case 1: expected = "RADIO_3_4"; break;
            case 2: expected = round ? "RADIO_ROUND" : "RADIO_1_1"; break;
            case 3: expected = "RADIO_9_16"; break;
            default: expected = "RADIO_FULL";
        }
        return expected.equals(view.v.name());
    }
    // Native delayed ratio delivery intentionally consumes the latest measured margins.
    // Bind it to attachment ownership, not geometry generation: first layout/insets
    // must not suppress the native TextureView AND focus-overlay margin updates.
    // The delayed animation-completion callback is stricter and remains transition-bound.
    private static boolean post(Handler handler, final Runnable task, long delay, CameraShadeView owner, final boolean transition) {
        final WeakReference<CameraShadeView> view = new WeakReference<>(owner);
        final long generation = transition ? PreviewLayout1922.generation(owner) : PreviewLayout1922.ownershipGeneration(owner);
        return handler.postDelayed(new Runnable() {
            public void run() {
                CameraShadeView current = view.get();
                boolean valid = transition ? PreviewLayout1922.current(current, generation)
                        : PreviewLayout1922.current(current) && PreviewLayout1922.ownershipGeneration(current) == generation;
                if (valid) task.run();
            }
        }, delay);
    }
    public static boolean postCamera(Handler handler, Runnable task, long delay) {
        if (!(task instanceof i.o.a.b1.a.d.a)) return handler.postDelayed(task, delay);
        i.o.a.b1.a.d.b owner = ((i.o.a.b1.a.d.a) task).c;
        return post(handler, task, delay, owner == null ? null : owner.b, false);
    }
    public static boolean postPreview(Handler handler, Runnable task, long delay) {
        if (!(task instanceof i.o.a.m.j.b)) return handler.postDelayed(task, delay);
        i.o.a.m.j.f owner = ((i.o.a.m.j.b) task).c;
        return post(handler, task, delay, owner == null ? null : owner.a, false);
    }
    public static boolean postSettled(Handler handler, Runnable task, long delay) {
        if (!(task instanceof i.f.l.v.b.b)) return handler.postDelayed(task, delay);
        return post(handler, task, delay, ((i.f.l.v.b.b) task).c, true);
    }
    /** CameraBgController's 200 ms callback carries an earlier ratio and grid id. */
    public static boolean camera(i.o.a.b1.a.d.b controller, int value, int grid) {
        return controller != null && controller.c == grid
                && PreviewLayout1922.current(controller.b)
                && ratio(controller.b, value, grid == 5);
    }
    /** PreviewController's delayed callback also has an explicit current ratio field. */
    public static boolean preview(i.o.a.m.j.f controller, int value) {
        return controller != null && controller.d == value
                && PreviewLayout1922.current(controller.a)
                && ratio(controller.a, value, false);
    }
    /** The native end callback's extra 50 ms delivery cannot target a detached/old view
     * or publish the next transition before it settles. Its existing body remains native. */
    public static boolean settled(CameraShadeView view) {
        return PreviewLayout1922.current(view) && view.w == null;
    }
}
