package com.hiro.ulike;

import android.content.SharedPreferences;
import android.util.Log;

/** Remember the user's front/rear selection without opening or switching a camera.
 * Uses the existing native preference, not an unrelated zoom-slot preference.
 * Only this small setting is committed synchronously on selection/exit; image
 * writers, session startup and preview-frame processing are not changed.
 */
public final class FacingMemory1928 {
    private static final String KEY = "last_use_front_camera";
    private static boolean needsFlush;
    private FacingMemory1928() {}

    /** Preserve the original startup choice until a valid saved selection exists.
     * The special copyright-camera override is outside this native getter.
     */
    public static synchronized boolean initial(boolean fallback) {
        try {
            SharedPreferences prefs = i.f.j0.f.a.b;
            return prefs != null && prefs.contains(KEY)
                    ? prefs.getBoolean(KEY, fallback) : fallback;
        } catch (RuntimeException error) {
            Log.w("ULikeFacing1928", "Saved camera selection unreadable; native default retained", error);
        } catch (LinkageError error) {
            Log.w("ULikeFacing1928", "Native preferences unavailable; native default retained", error);
        }
        return fallback;
    }

    /** Native selection setter. Also creates the preference when front==the
     * old default, which the original change-only setter could skip forever.
     */
    public static synchronized void remember(boolean front) {
        try {
            i.f.j0.d.d.b = Boolean.valueOf(front);
            SharedPreferences prefs = i.f.j0.f.a.b;
            if (prefs == null) { needsFlush = true; return; }
            boolean unchanged = false;
            try { unchanged = prefs.contains(KEY) && prefs.getBoolean(KEY, !front) == front; }
            catch (ClassCastException corruptSetting) { /* Replace only this key. */ }
            if (!unchanged || needsFlush) {
                needsFlush = true;
                if (prefs.edit().putBoolean(KEY, front).commit()) needsFlush = false;
                else Log.w("ULikeFacing1928", "Camera selection commit failed; retry on next selection/exit");
            }
        } catch (RuntimeException error) {
            needsFlush = true;
            Log.w("ULikeFacing1928", "Camera selection could not be persisted", error);
        } catch (LinkageError error) {
            needsFlush = true;
            Log.w("ULikeFacing1928", "Native camera selection storage unavailable", error);
        }
    }

    /** Snapshot the real UI selection before closing camera screens. Missing
     * state during teardown is UNKNOWN, not rear: never replace front with false.
     */
    public static void rememberCurrent() {
        try {
            i.f.l.n.q.y.m owner = i.f.l.n.q.y.m.a;
            if (owner == null) return;
            i.f.l.u.j state = owner.g();
            if (state == null) return;
            i.f.l.u.p<?> value = state.x();
            if (value == null) return;
            Object selected = value.a();
            if (selected instanceof Boolean) remember(((Boolean) selected).booleanValue());
        } catch (RuntimeException error) {
            Log.w("ULikeFacing1928", "Camera state unavailable; previous selection retained", error);
        } catch (LinkageError error) {
            Log.w("ULikeFacing1928", "Camera state bridge unavailable; previous selection retained", error);
        }
    }
}
