package com.hiro.ulike;
import android.view.Surface;
import android.view.SurfaceView;
/** Host-only output-probe hook spy; no PixelCopy/native rendering is modeled. */
public final class PreviewOutput1965 {
 public static int observations,cancellations;
 public static long lastEpoch;
 public static void observe(Object recorder,SurfaceView view,Surface surface,long epoch){observations++;lastEpoch=epoch;}
 public static void cancelled(Object recorder){cancellations++;}
}
