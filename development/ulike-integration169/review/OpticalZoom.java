package com.hiro.ulike;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import java.util.List;

/** Test-only call recorder; never copied into the candidate runtime or APK. */
public final class OpticalZoom {
    public static int builds,captures,bursts;
    public static Object[] last;
    public static RuntimeException failure;
    public static CaptureRequest build(CaptureRequest.Builder b) {
        builds++;last=new Object[]{b};if(failure!=null)throw failure;return null;
    }
    public static int capture(CameraCaptureSession s,CaptureRequest r,CameraCaptureSession.CaptureCallback c,Handler h)throws CameraAccessException {
        captures++;last=new Object[]{s,r,c,h};if(failure!=null)throw failure;return 1729;
    }
    public static int burst(CameraCaptureSession s,List<CaptureRequest> r,CameraCaptureSession.CaptureCallback c,Handler h)throws CameraAccessException {
        bursts++;last=new Object[]{s,r,c,h};if(failure!=null)throw failure;return 1730;
    }
}
