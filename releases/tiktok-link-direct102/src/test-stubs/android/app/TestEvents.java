package android.app;
/** Host-only observable platform calls, never bundled in the extension. */
public final class TestEvents {
    public static final java.util.List<String> values=new java.util.ArrayList<String>();
    public static void add(String s){values.add(s);}
    public static void clear(){values.clear();}
}
