package com.hiro.ulike;
import android.hardware.camera2.*;import java.util.*;
public final class OpticalZoom {
    public static final Map<CameraDevice,Object> routes=new IdentityHashMap<CameraDevice,Object>();
    public static final class Route {public String physicalId="";public CameraCharacteristics lens;public boolean failed;}
    private static Object route(CameraDevice d){return routes.get(d);}
}
