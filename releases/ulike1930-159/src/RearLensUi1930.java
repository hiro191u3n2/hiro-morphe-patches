package com.hiro.ulike;

/** Read the camera screen's current selection before exposing rear-lens controls.
 * Camera2 route information can outlive a native front/rear switch, so only an
 * explicit rear selection permits the existing visibility and click rules.
 * This bridge does not cache, persist, or modify camera or UI state.
 */
public final class RearLensUi1930 {
    private RearLensUi1930() {}

    public static boolean rearSelected() {
        try {
            i.f.l.n.q.y.m owner = i.f.l.n.q.y.m.a;
            if (owner == null) return false;
            // Native owner.g() reads this same session and scene, but logs on
            // every call. Use its public read-only lookup path without logging.
            i.f.l.j session = i.f.l.n.q.y.m.b;
            String scene = i.f.l.n.q.y.m.c;
            if (session == null || scene == null) return false;
            i.f.l.u.g cameraState = session.g(scene);
            if (cameraState == null) return false;
            i.f.l.u.j state = cameraState.k();
            if (state == null) return false;
            i.f.l.u.p<?> property = state.x();
            if (property == null) return false;
            return Boolean.FALSE.equals(property.a());
        } catch (RuntimeException unavailableState) {
            return false;
        } catch (LinkageError unavailableBridge) {
            return false;
        }
    }
}
