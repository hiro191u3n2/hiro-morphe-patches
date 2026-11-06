package com.google.android.maps;

import android.app.Activity;
import android.view.View;
import app.hiro.mapsstartup.LocationHost;

/** A fake native bridge contract, not a fake Android execution claim. */
public final class MapsActivity extends Activity implements LocationHost {
    public static final int MY_LOCATION_BUTTON = 0x7f0b06b0;
    public final View locationButton = new View();
    public boolean nativeFixReady;
    public boolean nativeTracking;
    public boolean navigationMode;
    public int bridgeCalls;
    public int recenterRequests;
    public Throwable bridgeFailure;
    public MapsActivity() { putView(MY_LOCATION_BUTTON, locationButton); }
    @Override public int hiroTryCurrentLocation() {
        bridgeCalls++;
        if (bridgeFailure != null) {
            if (bridgeFailure instanceof Error) throw (Error) bridgeFailure;
            throw (RuntimeException) bridgeFailure;
        }
        if (navigationMode) return -1;
        if (!nativeFixReady) return 0;
        if (!nativeTracking) {
            nativeTracking = true;
            recenterRequests++;
        }
        return 1;
    }
}
