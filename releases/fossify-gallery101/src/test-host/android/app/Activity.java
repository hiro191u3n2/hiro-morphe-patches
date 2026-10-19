package android.app;

/** Minimal host contract, never packaged in the Android extension. */
public class Activity {
    public String getPackageName() { throw new UnsupportedOperationException(); }
    public boolean isTaskRoot() { throw new UnsupportedOperationException(); }
    public void finishAndRemoveTask() { throw new UnsupportedOperationException(); }
}
