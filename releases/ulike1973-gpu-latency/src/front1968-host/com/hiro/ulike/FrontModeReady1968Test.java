package com.hiro.ulike;

import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.SystemClock;
import i.s.a.w.q;
import java.lang.ref.WeakReference;

/** Incomplete native mode ownership must never be mistaken for a started preview. */
public final class FrontModeReady1968Test {
    static int checks;
    static void ck(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    static void reset() {
        FrontTest1936.reset(false, false);
        FrontTest1936.ready();
    }
    static void startWithMissingMode() {
        FrontTest1936.camera2.M = null;
        ck(!FrontPreview1936.before(FrontTest1936.capture), "opened device without its native mode is withheld");
        ck(FrontTest1936.begin() == -100, "incomplete mode keeps native not-ready return");
        FrontTest1936.tick();
        ck(FrontTest1936.capture.accepted == 0 && FrontTest1936.capture.starts == 1,
                "no provider/start replay enters the missing-mode native failure path");
    }
    static void delayedMode() {
        reset();
        startWithMissingMode();
        FrontTest1936.camera2.M = FrontTest1936.mode;
        FrontTest1936.settle();
        ck(FrontTest1936.capture.accepted == 1 && FrontTest1936.capture.starts == 2,
                "independent mode preparation completes the same bounded pending request");
        ck(q.INSTANCE.stops == 0 && FrontTest1936.capture.newSurfaceRequests == 0 && Handler.queued() == 0,
                "delayed mode requires no extra stop, texture renewal or observation work");

        reset();
        q.INSTANCE.mCurrentCameraState = 3;
        startWithMissingMode();
        FrontTest1936.camera2.M = FrontTest1936.mode;
        FrontTest1936.settle();
        ck(FrontTest1936.capture.accepted == 1 && FrontTest1936.capture.starts == 2,
                "server preview state alone cannot fabricate success for a missing mode");
    }
    static void incompleteOwnership() {
        for (int fault = 0; fault < 4; fault++) {
            reset();
            Object previous;
            if (fault == 0) { previous=FrontTest1936.mode.g; FrontTest1936.mode.g=new i.s.a.w.g(); }
            else if (fault == 1) { previous=FrontTest1936.mode.h; FrontTest1936.mode.h=new com.ss.android.ttvecamera.TECameraSettings(); }
            else if (fault == 2) { previous=FrontTest1936.mode.j; FrontTest1936.mode.j=new CameraDevice(); }
            else { previous=FrontTest1936.mode.a; FrontTest1936.mode.a=null; }
            ck(FrontTest1936.begin() == -100, "incomplete/foreign mode withheld " + fault);
            FrontTest1936.tick();
            ck(FrontTest1936.capture.accepted == 0 && FrontTest1936.capture.starts == 1,
                    "mode identities are checked on queued replay " + fault);
            if (fault == 0) FrontTest1936.mode.g=(i.s.a.w.g)previous;
            else if (fault == 1) FrontTest1936.mode.h=(com.ss.android.ttvecamera.TECameraSettings)previous;
            else if (fault == 2) FrontTest1936.mode.j=(CameraDevice)previous;
            else FrontTest1936.mode.a=(android.hardware.camera2.CameraCharacteristics)previous;
            FrontTest1936.settle();
            ck(FrontTest1936.capture.accepted == 1 && FrontTest1936.capture.starts == 2,
                    "only fully owned prepared mode can complete request " + fault);
        }
    }
    static void boundedAndCancelled() {
        reset();
        startWithMissingMode();
        Handler.until(SystemClock.now + 12000L);
        FrontTest1936.camera2.M = FrontTest1936.mode;
        FrontTest1936.settle();
        ck(FrontTest1936.capture.accepted == 1, "late independent mode setup within original input deadline completes");

        reset();
        startWithMissingMode();
        FrontTest1936.settle();
        FrontTest1936.camera2.M = FrontTest1936.mode;
        FrontPreview1936.pipelines(FrontTest1936.capture);
        FrontTest1936.settle();
        ck(FrontTest1936.capture.starts == 1 && FrontTest1936.capture.accepted == 0 && Handler.queued() == 0,
                "mode availability never replenishes an expired input request");

        for (int stale = 0; stale < 6; stale++) {
            reset();
            startWithMissingMode();
            if (stale == 0) FrontPreview1936.cancel(FrontTest1936.capture);
            else if (stale == 1) ManualLens170.f.put("epoch", 8L);
            else if (stale == 2) ManualLens170.f.put("foreground", false);
            else if (stale == 3) q.INSTANCE.mCameraSettings = new com.ss.android.ttvecamera.TECameraSettings();
            else if (stale == 4) ManualLens170.f.put("capture", new WeakReference<Object>(new com.ss.android.vesdk.VECameraCapture()));
            else FrontTest1936.camera2.K = new CameraDevice();
            FrontTest1936.camera2.M = FrontTest1936.mode;
            FrontTest1936.settle();
            ck(FrontTest1936.capture.starts == 1 && FrontTest1936.capture.accepted == 0 && Handler.queued() == 0,
                    "late mode cannot revive stale/cancelled request " + stale);
        }
    }
    static void retainedLegacyPath() {
        FrontTest1936.reset(false, true);
        FrontTest1936.ready();
        ck(FrontTest1936.begin() == 0, "Camera1 needs no Camera2 mode");
        FrontTest1936.settle();
        ck(FrontTest1936.capture.accepted == 1 && FrontTest1936.capture.starts == 1
                && q.INSTANCE.stops == 0 && Handler.queued() == 0, "ordinary Camera1 lifecycle retained");
    }
    public static void main(String[] args) {
        delayedMode(); incompleteOwnership(); boundedAndCancelled(); retainedLegacyPath();
        System.out.println("PASS1968 front native mode assertions=" + checks);
    }
}
