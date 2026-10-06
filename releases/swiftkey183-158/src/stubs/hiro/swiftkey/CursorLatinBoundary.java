package hiro.swiftkey;
/** Compile/test-only observation point; never included in the production DEX. */
public final class CursorLatinBoundary {
    public static int calls;
    public static ec0.x lastOwner;
    public static ec0.n lastContext;
    public static boolean throwRuntime;
    public static boolean throwLinkage;
    public static void reset() {
        calls = 0;
        lastOwner = null;
        lastContext = null;
        throwRuntime = false;
        throwLinkage = false;
    }
    static void afterSelection(ec0.x owner, ec0.n context) {
        calls++;
        lastOwner = owner;
        lastContext = context;
        if (throwRuntime) throw new IllegalStateException("test downstream failure");
        if (throwLinkage) throw new NoClassDefFoundError("test downstream linkage");
    }
}
