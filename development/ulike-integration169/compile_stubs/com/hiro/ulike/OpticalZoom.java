package com.hiro.ulike;
import android.hardware.camera2.*;
import android.os.Handler;
import java.util.List;
/** Compile-only signatures. Never packaged: implementation is the hash-pinned released runtime. */
public final class OpticalZoom {
 public static CaptureRequest build(CaptureRequest.Builder b){throw new AssertionError("compile-only");}
 public static int capture(CameraCaptureSession s,CaptureRequest r,CameraCaptureSession.CaptureCallback c,Handler h)throws CameraAccessException{throw new AssertionError("compile-only");}
 public static int burst(CameraCaptureSession s,List<CaptureRequest> r,CameraCaptureSession.CaptureCallback c,Handler h)throws CameraAccessException{throw new AssertionError("compile-only");}
}
