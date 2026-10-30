package android.view;

/** Explicit host input, not an Android device's density or configuration. */
public final class ViewConfiguration {
    public static int scaledTouchSlop;
    public static int getFailure, valueFailure;
    public static void reset() { scaledTouchSlop=0; getFailure=valueFailure=0; }
    public static ViewConfiguration get(android.content.Context ignored) {
        fail(getFailure);
        return new ViewConfiguration();
    }
    public int getScaledTouchSlop() { fail(valueFailure); return scaledTouchSlop; }
    private static void fail(int kind) {
        if(kind==1)throw new IllegalStateException("PRIVATE_SLOP_FAILURE");
        if(kind==2)throw new NoSuchMethodError("PRIVATE_SLOP_FAILURE");
    }
}

