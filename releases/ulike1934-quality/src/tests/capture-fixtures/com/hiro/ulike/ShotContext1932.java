package com.hiro.ulike;
import android.hardware.camera2.*;
public final class ShotContext1932 {
    public static int calls;public static long timestamp;public static int iso;public static boolean allow=true;
    public static boolean receivedValues1933(Object owner,Object callback,long t,int w,int h,CaptureResult r){if(!allow)return false;calls++;timestamp=t;iso=r.get(CaptureResult.SENSOR_SENSITIVITY);return true;}
}
