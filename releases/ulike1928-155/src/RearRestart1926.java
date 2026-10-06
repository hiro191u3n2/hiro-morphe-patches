package com.hiro.ulike;

import android.content.Context;
import android.util.Log;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

/** Rear-camera cold/warm startup ordering repair. Does not alter facing, output size,
 * image processing, zoom selection or permissions. No device result is implied.
 */
public final class RearRestart1926 {
    private RearRestart1926() {}

    /** Invoked at the successful end of native init, never before its settings exist. */
    public static void initialized(Object capture) {
        if (capture == null) return;
        try {
            Object context = OpticalZoom.field(capture, "d");
            if (!(context instanceof Context) || OpticalZoom.field(capture, "a") == null
                    || OpticalZoom.field(capture, "c") == null) return;
            OpticalZoom.track(capture, (Context) context);
        } catch (ReflectiveOperationException error) {
            Log.w("ULikeRear1926", "Camera initialization ownership unavailable", error);
        } catch (RuntimeException error) {
            Log.w("ULikeRear1926", "Camera initialization tracking failed", error);
        } catch (LinkageError error) {
            Log.w("ULikeRear1926", "Camera initialization bridge unavailable", error);
        }
    }

    /** Called after native mode preparation, before CameraManager.openCamera.
     * Fixed physical rear routes wait for their real preview surfaces instead of
     * opening a deferred, surface-less session and later racing finalization.
     * Ordinary native session construction remains responsible for actual outputs.
     */
    public static void prepared(Object mode, int result) {
        if (mode == null || result != 0) return;
        try {
            synchronized (ManualLens170.get("LOCK")) {
                OpticalZoom.Route route = (OpticalZoom.Route)
                        ((Map<?, ?>) ManualLens170.get("modes")).get(mode);
                if (route == null || route != ManualLens170.get("active")
                        || route.epoch != ManualLens170.number("epoch")
                        || !ManualLens170.yes("foreground") || route.failed
                        || !route.rear || route.nominal <= 0 || !route.physical()) return;
                Object settings = OpticalZoom.field(mode, "h");
                if (settings == null) return;
                Field deferred = settings.getClass().getField("u0");
                int cameraMode = settings.getClass().getField("M").getInt(settings);
                if ((cameraMode == 0 || cameraMode == 1) && deferred.getBoolean(settings)) {
                    deferred.setBoolean(settings, false);
                    Log.i("ULikeRear1926", "Fixed rear startup waits for concrete preview surfaces");
                }
            }
        } catch (ReflectiveOperationException error) {
            Log.w("ULikeRear1926", "Native surface-start policy unavailable", error);
        } catch (RuntimeException error) {
            Log.w("ULikeRear1926", "Native surface-start policy unchanged", error);
        } catch (LinkageError error) {
            Log.w("ULikeRear1926", "Native surface-start policy unsupported", error);
        }
    }

    /** Recovery readiness is the live camera window, not a painted/visible shade
     * or the optional lens bar. Keep ManualLens170.ready for manual lens taps.
     * Background/recording/save guards remain in the original recovery callers.
     */
    public static boolean recoveryWindowReady() {
        try {
            if (!ManualLens170.yes("foreground")) return false;
            Object reference = ManualLens170.get("capture");
            if (!(reference instanceof WeakReference)
                    || ((WeakReference<?>) reference).get() == null) return false;
            View shade = PreviewLayout1922.gestureView1925();
            if (shade == null || !shade.isAttachedToWindow()
                    || shade.getWindowVisibility() != View.VISIBLE
                    || shade.getWidth() <= 0 || shade.getHeight() <= 0) return false;
            View root = shade.getRootView();
            return root != null && root.isShown() && root.hasWindowFocus()
                    && root.getWindowVisibility() == View.VISIBLE;
        } catch (ReflectiveOperationException error) {
            return false;
        } catch (RuntimeException error) {
            return false;
        } catch (LinkageError error) {
            return false;
        }
    }
}
