package com.google.android.maps;

import android.app.Activity;
import android.os.SystemClock;
import android.view.View;
import app.hiro.mapsstartup.LocationHost;
import java.util.ArrayList;
import java.util.List;

/** A fake native bridge contract, not a fake Android execution claim. */
public final class MapsActivity extends Activity implements LocationHost {
    public static final int MY_LOCATION_BUTTON = 0x7f0b06b0;
    public final View locationButton = new View();
    public boolean nativeFixReady;
    public boolean nativeGraphReady = true;
    public boolean nativeModeReady = true;
    public boolean nativeTracking;
    public boolean cameraAcknowledged;
    public boolean autoAcknowledge = true;
    public boolean navigationMode;
    public int bridgeCalls;
    public int recenterRequests;
    public int readOnlyQueries;
    public final List<Long> requestTimes = new ArrayList<Long>();
    public final List<Boolean> issueArguments = new ArrayList<Boolean>();
    public Throwable bridgeFailure;
    public MapsActivity() { putView(MY_LOCATION_BUTTON, locationButton); }
    @Override public int hiroTryCurrentLocation(boolean issue) {
        bridgeCalls++;
        issueArguments.add(Boolean.valueOf(issue));
        if (!issue) readOnlyQueries++;
        if (bridgeFailure != null) {
            if (bridgeFailure instanceof Error) throw (Error) bridgeFailure;
            throw (RuntimeException) bridgeFailure;
        }
        if (!nativeGraphReady || !nativeModeReady) return 0;
        if (navigationMode) return -1;
        if (!nativeFixReady) return 0;
        if (cameraAcknowledged) return 1;
        if (!issue) return 0;
        nativeTracking = true;
        recenterRequests++;
        requestTimes.add(Long.valueOf(SystemClock.elapsedRealtime()));
        if (autoAcknowledge) cameraAcknowledged = true;
        return 2;
    }
}
