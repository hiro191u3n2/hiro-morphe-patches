package android.content;
/** Host-only fixture, never packaged into Android code. */
public final class Context { public ClassLoader getClassLoader(){return getClass().getClassLoader();} }
