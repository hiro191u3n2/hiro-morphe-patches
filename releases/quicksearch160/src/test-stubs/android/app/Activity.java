package android.app;

import android.content.Context;
import android.content.Intent;

/** Minimal host-test contract; never included in the Android extension. */
public class Activity extends Context {
    public boolean isTaskRoot() { throw new UnsupportedOperationException(); }
    public void finish() { throw new UnsupportedOperationException(); }
    public void finishAndRemoveTask() { throw new UnsupportedOperationException(); }
    public void startActivity(Intent intent) { throw new UnsupportedOperationException(); }
}
