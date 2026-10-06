package android.content;

/** Minimal host-test contract; never included in the Android extension. */
public class Context {
    public String getPackageName() { throw new UnsupportedOperationException(); }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        throw new UnsupportedOperationException();
    }
}
